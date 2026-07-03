package com.moyun.ext.cms.service;

import com.moyun.common.exception.system.ServiceException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 代码沙箱执行器（任务 3.6 在线代码运行）
 * <p>
 * 简化实现：使用 {@link ProcessBuilder} 执行用户代码，不引入 Docker。
 * 安全限制：
 * <ul>
 *   <li>超时 5 秒强制 kill（destroyForcibly）</li>
 *   <li>stdout/stderr 输出截断至 1MB，避免内存耗尽</li>
 *   <li>命令以参数数组传入（ProcessBuilder 不经 shell），规避 shell 注入</li>
 *   <li>Java 临时编译执行于独立临时目录，执行后清理</li>
 *   <li>Python/JavaScript 直接通过解释器 -c/-e 执行</li>
 * </ul>
 * 注意：ProcessBuilder 无法像 Docker 那样强隔离文件/网络访问；生产环境如需强沙箱应迁移至容器方案。
 *
 * @author moyun
 */
@Service
public class CodeExecutorService {

    /** 执行超时（毫秒） */
    private static final long TIMEOUT_MS = 5_000L;

    /** 输出截断上限（1MB） */
    private static final int MAX_OUTPUT_BYTES = 1024 * 1024;

    /** 源代码长度上限（64KB，防止滥用） */
    private static final int MAX_CODE_LENGTH = 64 * 1024;

    /** 标准输入长度上限（64KB） */
    private static final int MAX_STDIN_LENGTH = 64 * 1024;

    /** 截断后追加的提示 */
    private static final String TRUNCATE_NOTICE = "\n...[输出超过 1MB，已截断]";

    /** 临时目录前缀 */
    private static final String TEMP_DIR_PREFIX = "code-run-";

    /**
     * 执行结果（值对象）
     */
    public static class ExecuteResult {
        private String output;
        private String errorMsg;
        private String status;
        private int runtimeMs;
        private Integer memKb;

        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public String getErrorMsg() { return errorMsg; }
        public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getRuntimeMs() { return runtimeMs; }
        public void setRuntimeMs(int runtimeMs) { this.runtimeMs = runtimeMs; }
        public Integer getMemKb() { return memKb; }
        public void setMemKb(Integer memKb) { this.memKb = memKb; }
    }

    /**
     * 执行用户代码。
     *
     * @param language 编程语言 java/python/javascript
     * @param code     源代码
     * @param stdin    标准输入（可空）
     * @param workTag  临时目录标识（建议传 userId），用于隔离不同用户的临时文件
     */
    public ExecuteResult execute(String language, String code, String stdin, String workTag) {
        if (language == null) {
            throw new ServiceException("编程语言不能为空");
        }
        if (code == null || code.isEmpty()) {
            throw new ServiceException("代码不能为空");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new ServiceException("代码长度超过上限（" + (MAX_CODE_LENGTH / 1024) + "KB）");
        }
        if (stdin != null && stdin.length() > MAX_STDIN_LENGTH) {
            throw new ServiceException("标准输入长度超过上限（" + (MAX_STDIN_LENGTH / 1024) + "KB）");
        }

        String lang = language.trim().toLowerCase();
        switch (lang) {
            case "java":
                return executeJava(code, stdin, workTag);
            case "python":
            case "python3":
                return executeScript("python3", "-c", code, stdin);
            case "javascript":
            case "js":
            case "node":
                return executeScript("node", "-e", code, stdin);
            default:
                throw new ServiceException("暂不支持的语言：" + language + "（支持 java/python/javascript）");
        }
    }

    // ========================================================================
    // Python / JavaScript：直接通过解释器 -c / -e 执行
    // ========================================================================
    private ExecuteResult executeScript(String interpreter, String flag, String code, String stdin) {
        return runProcess(new String[]{interpreter, flag, code}, null, null, stdin);
    }

    // ========================================================================
    // Java：临时编译执行
    // ========================================================================
    private ExecuteResult executeJava(String code, String stdin, String workTag) {
        Path tempDir = null;
        try {
            String tag = (workTag == null || workTag.isEmpty()) ? String.valueOf(System.nanoTime()) : workTag;
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX + tag + "-");
            Path sourceFile = Paths.get(tempDir.toString(), "Main.java");
            Files.writeString(sourceFile, code, StandardCharsets.UTF_8);

            // 1. 编译
            ExecuteResult compileResult = runProcess(
                    new String[]{"javac", "-encoding", "UTF-8", "Main.java"},
                    tempDir.toFile(), null, null);
            if (!"success".equals(compileResult.getStatus())) {
                // 编译失败：把编译错误作为 errorMsg 返回
                ExecuteResult r = new ExecuteResult();
                r.setStatus("failed");
                r.setErrorMsg(compileResult.getErrorMsg());
                r.setOutput("");
                r.setRuntimeMs(compileResult.getRuntimeMs());
                r.setMemKb(compileResult.getMemKb());
                return r;
            }

            // 2. 运行
            return runProcess(new String[]{"java", "-cp", tempDir.toString(), "Main"}, null, null, stdin);
        } catch (IOException e) {
            ExecuteResult r = new ExecuteResult();
            r.setStatus("failed");
            r.setErrorMsg("Java 编译环境异常：" + e.getMessage());
            r.setOutput("");
            return r;
        } finally {
            cleanup(tempDir);
        }
    }

    // ========================================================================
    // 通用进程执行：写 stdin，并发读 stdout/stderr，超时强制结束
    // ========================================================================
    private ExecuteResult runProcess(String[] command, java.io.File workDir, java.io.File dummy, String stdin) {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workDir != null) {
            pb.directory(workDir);
        }
        // 不合并 stderr 到 stdout，便于区分正常输出与错误信息
        pb.redirectErrorStream(false);

        ExecuteResult result = new ExecuteResult();
        long start = System.currentTimeMillis();
        Process process = null;
        try {
            process = pb.start();
        } catch (IOException e) {
            result.setStatus("failed");
            result.setErrorMsg("执行环境异常（可能未安装对应运行时）：" + e.getMessage()
                    + "（命令：" + safeCommandName(command) + "）");
            result.setOutput("");
            result.setRuntimeMs((int) (System.currentTimeMillis() - start));
            return result;
        }

        final Process p = process;
        // 并发读取 stdout / stderr，避免管道缓冲写满导致死锁
        StringBuilder outBuf = new StringBuilder();
        StringBuilder errBuf = new StringBuilder();
        Thread outReader = new Thread(() -> readStream(p.getInputStream(), outBuf));
        Thread errReader = new Thread(() -> readStream(p.getErrorStream(), errBuf));
        outReader.start();
        errReader.start();

        // 后台采样进程峰值内存（Linux /proc），非 Linux 时保持 null
        AtomicLong peakRssKb = new AtomicLong(0L);
        Thread memSampler = new Thread(() -> samplePeakRss(p, peakRssKb));
        memSampler.setDaemon(true);
        memSampler.start();

        // 写入 stdin
        if (stdin != null && !stdin.isEmpty()) {
            try (OutputStream os = p.getOutputStream()) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                // 进程可能已退出导致管道关闭，忽略
            }
        }

        boolean finished;
        try {
            finished = p.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }

        if (!finished) {
            // 超时：强制结束进程树
            p.destroyForcibly();
            try { p.waitFor(1, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            result.setStatus("timeout");
            result.setErrorMsg("执行超时（超过 " + TIMEOUT_MS + "ms 已被强制终止）");
        } else {
            int exit = p.exitValue();
            result.setStatus(exit == 0 ? "success" : "failed");
        }

        // 等待读取线程结束（带超时，避免它们卡死）
        try { outReader.join(2_000); } catch (InterruptedException ignored) { }
        try { errReader.join(2_000); } catch (InterruptedException ignored) { }
        memSampler.interrupt();

        result.setOutput(truncate(outBuf.toString()));
        // 仅在非 success 或有 stderr 时填 errorMsg
        if (errBuf.length() > 0) {
            result.setErrorMsg(truncate(errBuf.toString()));
        } else if (!"success".equals(result.getStatus()) && result.getErrorMsg() == null) {
            result.setErrorMsg("进程异常退出，退出码：" + (finished ? p.exitValue() : -1));
        }
        result.setRuntimeMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start));
        long peak = peakRssKb.get();
        result.setMemKb(peak > 0 ? (int) Math.min(peak, Integer.MAX_VALUE) : null);
        return result;
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    /** 读取输入流到 StringBuilder（带 1MB 截断） */
    private void readStream(InputStream is, StringBuilder buf) {
        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
            char[] cbuf = new char[4096];
            int n;
            while ((n = reader.read(cbuf)) != -1) {
                if (buf.length() + n > MAX_OUTPUT_BYTES) {
                    buf.append(cbuf, 0, Math.max(0, MAX_OUTPUT_BYTES - buf.length()));
                    buf.append(TRUNCATE_NOTICE);
                    break;
                }
                buf.append(cbuf, 0, n);
            }
        } catch (IOException e) {
            // 进程被销毁时通常抛 IOException，忽略
        }
    }

    /** 截断字符串至 1MB（双保险，readStream 已截断，这里防止外部拼装超长） */
    private String truncate(String s) {
        if (s == null) {
            return "";
        }
        if (s.length() > MAX_OUTPUT_BYTES) {
            return s.substring(0, MAX_OUTPUT_BYTES) + TRUNCATE_NOTICE;
        }
        return s;
    }

    /** 后台采样进程峰值 RSS（仅 Linux /proc 生效，非 Linux 直接返回不采样） */
    private void samplePeakRss(Process p, AtomicLong peakHolder) {
        long pid = p.pid();
        while (!Thread.currentThread().isInterrupted() && p.isAlive()) {
            long rss = readProcRssKb(pid);
            if (rss > peakHolder.get()) {
                peakHolder.set(rss);
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 进程结束前最后采样一次峰值
        long last = readProcRssKb(pid);
        if (last > peakHolder.get()) {
            peakHolder.set(last);
        }
    }

    /** 读取 /proc/{pid}/status 中的 VmRSS（KB），失败返回 0 */
    private long readProcRssKb(long pid) {
        Path status = Paths.get("/proc", String.valueOf(pid), "status");
        if (!Files.exists(status)) {
            return 0L;
        }
        try (BufferedReader r = Files.newBufferedReader(status, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("VmRSS:")) {
                    // 形如：VmRSS:      12345 kB
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        return Long.parseLong(parts[1]);
                    }
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            // 忽略读取失败
        }
        return 0L;
    }

    /** 清理临时目录及其内容 */
    private void cleanup(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try {
            try (var stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                }
            }
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // 清理失败不抛出，仅记录日志位置可由调用方观察
        }
    }

    /** 仅用于错误信息展示命令名（不含用户代码，避免泄露） */
    private String safeCommandName(String[] command) {
        return command == null || command.length == 0 ? "(empty)" : command[0];
    }
}
