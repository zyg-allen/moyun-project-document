package com.moyun.portal.judge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;

/**
 * 基于 Docker 沙箱的判题引擎（v8.0 生产环境）
 * <p>
 * 实现策略：
 * <ul>
 *   <li>每个提交在宿主临时目录准备源码（与 {@link ProcessJudgeEngine} 一致），随后挂载入容器；</li>
 *   <li>编译型语言在容器内完成编译，编译失败直接返回 CE；</li>
 *   <li>逐用例通过 {@code docker run --rm -i} 在隔离容器内执行，stdin 输入 / stdout 捕获；</li>
 *   <li>容器资源限制（CPU/内存/PID/网络/文件系统）通过 docker CLI flag 注入；</li>
 *   <li>容器退出后由 --rm 自动清理，宿主临时目录在 finally 中清理。</li>
 * </ul>
 *
 * <p>
 * <b>沙箱隔离</b>：默认禁用网络（--network=none）、只读根文件系统（--read-only）、
 * 限制 CPU 配额（--cpus）、内存上限（--memory）、PID 上限（--pids-limit）、丢弃全部 capability（--cap-drop=ALL）、
 * 禁用特权（--security-opt=no-new-privileges），并挂载 tmpfs 限制可写空间。
 * 生产环境需确保 docker CLI 已安装且后端服务账号具备 docker 客户端调用权限
 * （推荐与 docker daemon 同机或挂载只读 docker.sock）。
 *
 * <p>
 * <b>与 {@link ProcessJudgeEngine} 的关系</b>：通过配置
 * {@code moyun.judge.engine-type=docker} 切换为本实现，默认 dev 环境保持 process。
 *
 * @author moyun
 */
@Component
@ConditionalOnProperty(prefix = "moyun.judge", name = "engine-type", havingValue = "docker")
public class DockerJudgeEngine implements JudgeEngine {

    private static final Logger log = LoggerFactory.getLogger(DockerJudgeEngine.class);

    /** 输出最大捕获长度（防止用户代码输出爆量，与 ProcessJudgeEngine 一致） */
    private static final int MAX_OUTPUT_LEN = 1_000_000;

    /** 编译超时（秒） */
    private static final long COMPILE_TIMEOUT_SECONDS = 60L;

    @Autowired
    private JudgeProperties properties;

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
            r.setErrorMessage("不支持的语言: " + language);
            return r;
        }

        String image = properties.getDocker().getImages().get(runtime.getLanguage());
        if (image == null || image.isBlank()) {
            JudgeResult r = JudgeResult.of(JudgeStatus.SYSTEM_ERROR);
            r.setErrorMessage("Docker 沙箱未配置语言镜像: " + runtime.getLanguage());
            return r;
        }

        Path hostWorkDir = null;
        try {
            hostWorkDir = Files.createTempDirectory("oj-docker-");
            Path srcPath = hostWorkDir.resolve("Solution." + runtime.getFileSuffix());
            String finalCode = runtime.isNeedsWrapper() ? wrapJavaCode(code) : code;
            Files.writeString(srcPath, finalCode, StandardCharsets.UTF_8);

            // 编译型语言先在容器内编译（产物同样落在挂载目录）
            Path outPath = null;
            Path outDir = null;
            if (runtime.isCompiled()) {
                if ("typescript".equals(runtime.getLanguage())) {
                    outDir = hostWorkDir.resolve("dist");
                    Files.createDirectories(outDir);
                    outPath = outDir.resolve("Solution.js");
                } else {
                    outPath = hostWorkDir.resolve("Solution.out");
                }
                String compileErr = compileInContainer(runtime, image, srcPath, outPath, outDir, hostWorkDir);
                if (compileErr != null) {
                    JudgeResult r = JudgeResult.of(JudgeStatus.COMPILE_ERROR);
                    r.setErrorMessage(compileErr);
                    r.setTotalCount(cases.size());
                    r.setPassedCount(0);
                    return r;
                }
            }

            return runCasesInContainer(runtime, srcPath, outPath, outDir, hostWorkDir, cases, timeoutMs);

        } catch (Exception e) {
            log.error("[OJ-Docker] 判题引擎异常 lang={} err={}", language, e.getMessage(), e);
            JudgeResult r = JudgeResult.of(JudgeStatus.SYSTEM_ERROR);
            r.setErrorMessage("Docker 判题机异常: " + e.getMessage());
            r.setTotalCount(cases == null ? 0 : cases.size());
            return r;
        } finally {
            if (hostWorkDir != null) {
                cleanupQuietly(hostWorkDir);
            }
        }
    }

    // ==================== 容器编译 ====================

    private String compileInContainer(LanguageRuntime runtime, String image,
                                       Path srcPath, Path outPath, Path outDir, Path hostWorkDir)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(runtime.getCompileCommand().size());
        for (String arg : runtime.getCompileCommand()) {
            switch (arg) {
                case "<SRC>": cmd.add(containerPath(srcPath, hostWorkDir)); break;
                case "<OUT>": cmd.add(containerPath(outPath, hostWorkDir)); break;
                case "<OUTDIR>": cmd.add(containerPath(outDir, hostWorkDir)); break;
                case "<WORKDIR>": cmd.add(properties.getDocker().getWorkDir()); break;
                default: cmd.add(arg);
            }
        }
        ProcessBuilder pb = buildDockerRunBase(hostWorkDir);
        pb.command().addAll(List.of(
                // 覆盖镜像默认 ENTRYPOINT/CMD，仅执行编译命令
                "--entrypoint", "",
                image, "sh", "-c", String.join(" ", quoteIfNeeded(cmd))
        ));
        pb.directory(hostWorkDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean finished = p.waitFor(COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            return "编译超时";
        }
        if (p.exitValue() != 0) {
            return truncate(output);
        }
        return null;
    }

    // ==================== 容器内逐用例运行 ====================

    private JudgeResult runCasesInContainer(LanguageRuntime runtime,
                                            Path srcPath, Path outPath, Path outDir, Path hostWorkDir,
                                            List<PortalInterviewQuestionTestCase> cases, long timeoutMs)
            throws IOException, InterruptedException {
        JudgeResult result = new JudgeResult();
        result.setTotalCount(cases.size());
        result.setPassedCount(0);
        result.setMaxRuntimeMs(0);
        List<CaseJudgeResult> caseResults = new ArrayList<>(cases.size());

        for (int i = 0; i < cases.size(); i++) {
            PortalInterviewQuestionTestCase tc = cases.get(i);
            CaseJudgeResult cr = runSingleCaseInContainer(runtime, srcPath, outPath, outDir,
                    hostWorkDir, tc, i + 1, timeoutMs);
            caseResults.add(cr);
            if (cr.getRuntime() != null && cr.getRuntime() > result.getMaxRuntimeMs()) {
                result.setMaxRuntimeMs(cr.getRuntime());
            }
            if (Boolean.TRUE.equals(cr.getPassed())) {
                result.setPassedCount(result.getPassedCount() + 1);
                continue;
            }
            // 首个失败用例即停止
            result.setFailedCaseId(tc.getId());
            result.setFailedCaseInput(tc.getInput());
            result.setFailedCaseExpected(tc.getExpectedOutput());
            result.setFailedCaseActual(cr.getActualOutput());
            result.setErrorMessage(cr.getErrorMessage());
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
        result.setStatus(JudgeStatus.ACCEPTED);
        result.setCaseResults(caseResults);
        return result;
    }

    private CaseJudgeResult runSingleCaseInContainer(LanguageRuntime runtime,
                                                      Path srcPath, Path outPath, Path outDir, Path hostWorkDir,
                                                      PortalInterviewQuestionTestCase tc, int caseIndex, long timeoutMs) {
        List<String> runCmd = new ArrayList<>(runtime.getRunCommand().size());
        for (String arg : runtime.getRunCommand()) {
            switch (arg) {
                case "<SRC>": runCmd.add(containerPath(srcPath, hostWorkDir)); break;
                case "<OUT>": runCmd.add(containerPath(outPath, hostWorkDir)); break;
                case "<OUTDIR>": runCmd.add(containerPath(outDir, hostWorkDir)); break;
                case "<WORKDIR>": runCmd.add(properties.getDocker().getWorkDir()); break;
                default: runCmd.add(arg);
            }
        }
        ProcessBuilder pb = buildDockerRunBase(hostWorkDir);
        // 必须保留 -i 以接收 stdin
        pb.command().add("-i");
        pb.command().add("--entrypoint");
        pb.command().add("");
        // buildDockerRunBase 仅装配资源限制与挂载，不追加镜像名；
        // 此处追加运行时镜像，由配置 moyun.judge.docker.images.<lang> 决定
        String image = properties.getDocker().getImages().get(runtime.getLanguage());
        pb.command().add(image);
        pb.command().add("sh");
        pb.command().add("-c");
        pb.command().add(String.join(" ", quoteIfNeeded(runCmd)));
        pb.directory(hostWorkDir.toFile());
        pb.redirectErrorStream(true);

        long start = System.currentTimeMillis();
        try {
            Process p = pb.start();
            if (tc.getInput() != null) {
                p.getOutputStream().write(tc.getInput().getBytes(StandardCharsets.UTF_8));
            }
            p.getOutputStream().close();
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

    // ==================== Docker 命令装配 ====================

    /**
     * 构造 {@code docker run} 基础命令（含资源限制、挂载、工作目录），不含镜像名与运行参数。
     * 调用方按需追加 --entrypoint / image / 命令。
     */
    private ProcessBuilder buildDockerRunBase(Path hostWorkDir) {
        JudgeProperties.Docker d = properties.getDocker();
        List<String> cmd = new ArrayList<>();
        cmd.add(d.getBinary());
        cmd.add("run");
        cmd.add("--rm");
        // 内存上限
        cmd.add("--memory=" + properties.getMemoryLimitMb() + "m");
        // 禁用 swap，防止用户代码使用 swap 突破内存限制
        cmd.add("--memory-swap=" + properties.getMemoryLimitMb() + "m");
        // CPU 限制
        if (d.getCpuQuota() > 0) {
            cmd.add("--cpu-period=" + d.getCpuPeriod());
            cmd.add("--cpu-quota=" + d.getCpuQuota());
        } else if (d.getCpus() > 0) {
            cmd.add("--cpus=" + d.getCpus());
        }
        // PID 上限
        if (d.getPidsLimit() >= 0) {
            cmd.add("--pids-limit=" + d.getPidsLimit());
        }
        // 网络
        if (d.isNetworkDisabled()) {
            cmd.add("--network=none");
        }
        // 文件系统
        if (d.isReadOnlyRoot()) {
            cmd.add("--read-only");
        }
        if (d.getTmpfs() != null && !d.getTmpfs().isBlank()) {
            for (String t : d.getTmpfs().split(",")) {
                cmd.add("--tmpfs=" + t.trim());
            }
        }
        // Capability 与特权
        if (d.isDropAllCaps()) {
            cmd.add("--cap-drop=ALL");
        }
        cmd.add("--security-opt=no-new-privileges");
        if (d.isSeccompUnconfined()) {
            cmd.add("--security-opt=seccomp=unconfined");
        }
        // 挂载宿主工作目录到容器内 workDir（rw：编译型语言产物需写回）
        cmd.add("-v");
        cmd.add(hostWorkDir.toAbsolutePath() + ":" + d.getWorkDir() + ":rw");
        cmd.add("-w");
        cmd.add(d.getWorkDir());
        return new ProcessBuilder(cmd);
    }

    /** 宿主路径 → 容器内路径（仅替换 hostWorkDir 前缀） */
    private String containerPath(Path hostPath, Path hostWorkDir) {
        if (hostPath == null) return null;
        Path rel = hostWorkDir.toAbsolutePath().relativize(hostPath.toAbsolutePath());
        return properties.getDocker().getWorkDir() + "/" + rel.toString().replace("\\", "/");
    }

    /**
     * 对包含空格或特殊字符的运行时参数进行简单引号包裹（用于 sh -c 拼接场景）。
     */
    private List<String> quoteIfNeeded(List<String> args) {
        List<String> out = new ArrayList<>(args.size());
        for (String a : args) {
            if (a == null || a.isEmpty()) {
                out.add("");
            } else if (a.indexOf(' ') >= 0 || a.indexOf('"') >= 0 || a.indexOf('\'') >= 0
                    || a.indexOf('$') >= 0 || a.indexOf('`') >= 0 || a.indexOf('\\') >= 0) {
                // 简单转义：用单引号包裹，内部单引号转义
                out.add("'" + a.replace("'", "'\"'\"'") + "'");
            } else {
                out.add(a);
            }
        }
        return out;
    }

    // ==================== 共享工具方法（与 ProcessJudgeEngine 行为对齐） ====================

    private boolean outputEquals(String actual, String expected) {
        if (actual == null) actual = "";
        if (expected == null) expected = "";
        return normalize(actual).equals(normalize(expected));
    }

    private String normalize(String s) {
        String unified = s.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = unified.split("\n", -1);
        List<String> trimmed = new ArrayList<>(lines.length);
        for (String line : lines) {
            trimmed.add(line.replaceAll("\\s+$", ""));
        }
        int end = trimmed.size();
        while (end > 0 && trimmed.get(end - 1).isEmpty()) end--;
        return String.join("\n", trimmed.subList(0, end));
    }

    private String wrapJavaCode(String code) {
        if (code == null) code = "";
        if (code.contains("class Solution") || code.contains("public class Solution")) {
            return code;
        }
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
            log.debug("[OJ-Docker] 清理临时目录失败: {}", e.getMessage());
        }
    }
}
