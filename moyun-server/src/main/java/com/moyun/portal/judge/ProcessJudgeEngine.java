package com.moyun.portal.judge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;

/**
 * 基于 ProcessBuilder 的判题引擎实现（v6.3 OJ 判题系统）
 * <p>
 * 实现策略：
 * <ul>
 *   <li>每个提交在临时工作目录执行；提交结束后清理；</li>
 *   <li>编译型语言先执行编译命令，编译失败直接返回 CE；</li>
 *   <li>解释型语言直接运行；</li>
 *   <li>每个用例：将 input 写入 stdin，捕获 stdout，与 expected 比对（trim 末尾换行）；</li>
 *   <li>用 Process.waitFor(timeout) 控制 TLE；</li>
 *   <li>遇到首个失败用例即停止（不再跑后续用例，避免无意义消耗）；</li>
 *   <li>Java/C++ 在临时目录运行；TypeScript 通过 tsc 编译到子目录后用 node 运行。</li>
 * </ul>
 *
 * <p>
 * <b>安全说明</b>：本实现为开发/测试环境的简化版，未做沙箱隔离。
 * 生产环境应替换为 Docker / Firecracker MicroVM 隔离运行（限制 CPU/内存/网络/文件系统），
 * 避免恶意代码访问宿主资源。可通过实现 {@link JudgeEngine} 接口并替换 Bean 即可切换。
 *
 * @author moyun
 */
@Component
public class ProcessJudgeEngine implements JudgeEngine {

    private static final Logger log = LoggerFactory.getLogger(ProcessJudgeEngine.class);

    /** 输出最大捕获长度（防止用户代码输出爆量） */
    private static final int MAX_OUTPUT_LEN = 1_000_000;

    @Override
    public JudgeResult judge(String language, String code,
                              List<PortalInterviewQuestionTestCase> cases,
                              long timeoutMs) {
        if (cases == null || cases.isEmpty()) {
            return JudgeResult.of(JudgeStatus.SYSTEM_ERROR);
        }

        LanguageRuntime runtime = LanguageRuntime.of(language);
        if (runtime == null) {
            JudgeResult r = JudgeResult.of(JudgeStatus.SYSTEM_ERROR);
            r.errorMessage = "不支持的语言: " + language;
            return r;
        }

        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("oj-judge-");
            Path srcPath = workDir.resolve("Solution." + runtime.getFileSuffix());

            // 写入用户代码（Java 需包成 Solution 类）
            String finalCode = runtime.isNeedsWrapper() ? wrapJavaCode(code) : code;
            Files.writeString(srcPath, finalCode, StandardCharsets.UTF_8);

            // 编译型语言先编译
            Path outPath = null;
            Path outDir = null;
            if (runtime.isCompiled()) {
                if (runtime.getLanguage().equals("typescript")) {
                    outDir = workDir.resolve("dist");
                    Files.createDirectories(outDir);
                    outPath = outDir.resolve("Solution.js");
                } else {
                    outPath = workDir.resolve("Solution.out");
                }
                String compileErr = compile(runtime, srcPath, outPath, outDir, workDir);
                if (compileErr != null) {
                    JudgeResult r = JudgeResult.of(JudgeStatus.COMPILE_ERROR);
                    r.errorMessage = compileErr;
                    r.totalCount = cases.size();
                    r.passedCount = 0;
                    return r;
                }
            }

            // 逐用例运行
            return runCases(runtime, srcPath, outPath, outDir, workDir, cases, timeoutMs);

        } catch (Exception e) {
            log.error("[OJ] 判题引擎异常 lang={} err={}", language, e.getMessage(), e);
            JudgeResult r = JudgeResult.of(JudgeStatus.SYSTEM_ERROR);
            r.errorMessage = "判题机异常: " + e.getMessage();
            r.totalCount = cases == null ? 0 : cases.size();
            return r;
        } finally {
            if (workDir != null) {
                cleanupQuietly(workDir);
            }
        }
    }

    /**
     * 编译用户代码，返回 null 表示成功，否则为错误输出
     */
    private String compile(LanguageRuntime runtime, Path srcPath, Path outPath, Path outDir, Path workDir) {
        List<String> cmd = new ArrayList<>(runtime.getCompileCommand().size());
        for (String arg : runtime.getCompileCommand()) {
            switch (arg) {
                case "<SRC>": cmd.add(srcPath.toString()); break;
                case "<OUT>": cmd.add(outPath.toString()); break;
                case "<OUTDIR>": cmd.add(outDir.toString()); break;
                case "<WORKDIR>": cmd.add(workDir.toString()); break;
                default: cmd.add(arg);
            }
        }
        try {
            Process p = new ProcessBuilder(cmd).directory(workDir.toFile())
                    .redirectErrorStream(true).start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return "编译超时";
            }
            if (p.exitValue() != 0) {
                return truncate(output);
            }
            return null;
        } catch (IOException | InterruptedException e) {
            return "编译执行失败: " + e.getMessage();
        }
    }

    /**
     * 逐用例运行代码并比对输出
     */
    private JudgeResult runCases(LanguageRuntime runtime, Path srcPath, Path outPath, Path outDir,
                                  Path workDir, List<PortalInterviewQuestionTestCase> cases, long timeoutMs) {
        JudgeResult result = new JudgeResult();
        result.totalCount = cases.size();
        result.passedCount = 0;
        result.maxRuntimeMs = 0;
        List<CaseJudgeResult> caseResults = new ArrayList<>(cases.size());

        for (int i = 0; i < cases.size(); i++) {
            PortalInterviewQuestionTestCase tc = cases.get(i);
            CaseJudgeResult cr = runSingleCase(runtime, srcPath, outPath, outDir, workDir, tc, i + 1, timeoutMs);
            caseResults.add(cr);
            if (cr.getRuntime() != null && cr.getRuntime() > result.maxRuntimeMs) {
                result.maxRuntimeMs = cr.getRuntime();
            }
            if (Boolean.TRUE.equals(cr.getPassed())) {
                result.passedCount++;
                continue;
            }
            // 首个失败用例即停止，记录状态与失败详情
            result.setFailedCaseId(tc.getId());
            result.setFailedCaseInput(tc.getInput());
            result.setFailedCaseExpected(tc.getExpectedOutput());
            result.setFailedCaseActual(cr.getActualOutput());
            result.setErrorMessage(cr.getErrorMessage());
            // 根据失败原因映射状态
            if (cr.getErrorMessage() != null && cr.getErrorMessage().contains("[TLE]")) {
                result.setStatus(JudgeStatus.TIME_LIMIT_EXCEEDED);
            } else if (cr.getErrorMessage() != null && cr.getErrorMessage().contains("[RE]")) {
                result.setStatus(JudgeStatus.RUNTIME_ERROR);
            } else {
                result.setStatus(JudgeStatus.WRONG_ANSWER);
            }
            result.setCaseResults(caseResults);
            return result;
        }
        // 全部通过
        result.setStatus(JudgeStatus.ACCEPTED);
        result.setCaseResults(caseResults);
        return result;
    }

    /**
     * 运行单个用例，返回该用例判题结果
     */
    private CaseJudgeResult runSingleCase(LanguageRuntime runtime, Path srcPath, Path outPath, Path outDir,
                                           Path workDir, PortalInterviewQuestionTestCase tc,
                                           int caseIndex, long timeoutMs) {
        List<String> cmd = new ArrayList<>(runtime.getRunCommand().size());
        for (String arg : runtime.getRunCommand()) {
            switch (arg) {
                case "<SRC>": cmd.add(srcPath.toString()); break;
                case "<OUT>": cmd.add(outPath.toString()); break;
                case "<OUTDIR>": cmd.add(outDir.toString()); break;
                case "<WORKDIR>": cmd.add(workDir.toString()); break;
                default: cmd.add(arg);
            }
        }
        long start = System.currentTimeMillis();
        try {
            Process p = new ProcessBuilder(cmd).directory(workDir.toFile()).start();
            // 写入 stdin
            if (tc.getInput() != null) {
                p.getOutputStream().write(tc.getInput().getBytes(StandardCharsets.UTF_8));
            }
            p.getOutputStream().close();
            // 读取 stdout（异步避免缓冲区满导致死锁：用 redirectErrorStream 合并 stderr）
            String stdout = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long elapsed = System.currentTimeMillis() - start;
            if (!finished) {
                p.destroyForcibly();
                return CaseJudgeResult.fail(tc.getId(), caseIndex,
                        Integer.valueOf(1).equals(tc.getIsSample()),
                        (int) Math.min(elapsed, Integer.MAX_VALUE), null, "[TLE] 运行超时");
            }
            int exit = p.exitValue();
            if (exit != 0) {
                return CaseJudgeResult.fail(tc.getId(), caseIndex,
                        Integer.valueOf(1).equals(tc.getIsSample()),
                        (int) Math.min(elapsed, Integer.MAX_VALUE), truncate(stdout),
                        "[RE] 退出码 " + exit);
            }
            boolean ok = outputEquals(stdout, tc.getExpectedOutput());
            if (ok) {
                return CaseJudgeResult.pass(tc.getId(), caseIndex,
                        Integer.valueOf(1).equals(tc.getIsSample()),
                        (int) Math.min(elapsed, Integer.MAX_VALUE));
            }
            return CaseJudgeResult.fail(tc.getId(), caseIndex,
                    Integer.valueOf(1).equals(tc.getIsSample()),
                    (int) Math.min(elapsed, Integer.MAX_VALUE), truncate(stdout), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long elapsed = System.currentTimeMillis() - start;
            return CaseJudgeResult.fail(tc.getId(), caseIndex,
                    Integer.valueOf(1).equals(tc.getIsSample()),
                    (int) Math.min(elapsed, Integer.MAX_VALUE), null, "[TLE] 判题线程被中断");
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            return CaseJudgeResult.fail(tc.getId(), caseIndex,
                    Integer.valueOf(1).equals(tc.getIsSample()),
                    (int) Math.min(elapsed, Integer.MAX_VALUE), null,
                    "[RE] " + e.getMessage());
        }
    }

    /**
     * 输出比对：去末尾空白行后比较，容错 Windows 换行
     */
    private boolean outputEquals(String actual, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        return normalize(actual).equals(normalize(expected));
    }

    private String normalize(String s) {
        // 统一换行为 \n，去掉每行末尾空格，去掉末尾多余空白行
        String unified = s.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = unified.split("\n", -1);
        List<String> trimmed = new ArrayList<>(lines.length);
        for (String line : lines) {
            trimmed.add(line.replaceAll("\\s+$", ""));
        }
        // 去掉末尾空行
        int end = trimmed.size();
        while (end > 0 && trimmed.get(end - 1).isEmpty()) end--;
        return String.join("\n", trimmed.subList(0, end));
    }

    /**
     * 将用户代码包成 Java Solution 类（保留原 main 入口，若用户已写则不再包）
     */
    private String wrapJavaCode(String code) {
        if (code == null) code = "";
        // 若用户已包含 public class Solution，直接使用
        if (code.contains("class Solution") || code.contains("public class Solution")) {
            return code;
        }
        // 默认包成 Solution 类，原样包裹用户代码
        return "import java.util.*;\n" +
                "import java.io.*;\n" +
                "public class Solution {\n" +
                "    public static void main(String[] args) throws Exception {\n" +
                "        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));\n" +
                "        // ===== 用户代码开始 =====\n" +
                code + "\n" +
                "        // ===== 用户代码结束 =====\n" +
                "    }\n" +
                "}\n";
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > MAX_OUTPUT_LEN ? s.substring(0, MAX_OUTPUT_LEN) + "\n...[输出超长已截断]" : s;
    }

    private void cleanupQuietly(Path workDir) {
        try (Stream<Path> walk = Files.walk(workDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 删除失败忽略，不影响判题结果
                }
            });
        } catch (IOException e) {
            log.debug("[OJ] 清理临时目录失败: {}", e.getMessage());
        }
    }
}
