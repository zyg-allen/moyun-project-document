package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：{@code @Async} 方法**不得在同一个 Bean 内被自调用**
 *
 * <h3>为什么需要这条守卫</h3>
 * <p>{@code @Async} 靠 Spring AOP 代理生效。同类内部调用（{@code this.method()}）**绕过代理**，
 * 方法会退化为**同步执行**——不报错、不打日志，只是"悄悄变慢"，
 * 而文档/方法名仍在宣称它是异步的。真实案例：</p>
 * <ul>
 *   <li>{@code ToolRegistry.logToolCallAsync}（v13.4 前）：方法标了 {@code @Async}、
 *       类头与 {@code AsyncConfig}/{@code AsyncTaskConfig} 的注释都称"异步记录、不阻塞主流程"，
 *       但它被**同一个类**的 {@code executeTool} 直接调用 → 工具调用日志的 DB 插入
 *       一直跑在请求线程上（同步）；</li>
 *   <li>{@code AiExecuteLogService.record(String,...)}（v13.4 前）：10 参重载体内直接调 11 参重载，
 *       属同类自调用；该重载本身也无任何外部调用方（死代码）。</li>
 * </ul>
 *
 * <p>正确做法（本项目既有先例）：把 {@code @Async} 方法抽到**独立 Bean**
 * （参见 {@code AiTaskAsyncExecutor} 与 {@code ToolCallLogWriter}），由调用方注入后调用。</p>
 *
 * <p>本测试对 {@code src/main/java} 全量源码做静态扫描：每个 {@code @Async} 方法，
 * 其方法名在**本文件内**除声明外不得再出现调用（注释中的提及不算）。</p>
 *
 * @author moyun
 */
class AsyncSelfInvocationGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    /** 方法声明行：捕获方法名（修饰符/泛型/返回类型任意，只要后跟 方法名( ） */
    private static final Pattern METHOD_DECL = Pattern.compile(
            "^(?:public|protected|private|static|final|synchronized|abstract|default|\\s)*"
                    + "[\\w<>\\[\\],.\\s?]+\\s+(\\w+)\\s*\\(");

    /**
     * {@code @Async} 注解行：**必须同时覆盖 {@code @Async} 与 {@code @Async("executor")}** 两种写法。
     * <p>{@code \b} 保证不会把 {@code @AsyncExecution} 之类的前缀相同注解误判为 {@code @Async}。</p>
     */
    private static final Pattern ASYNC_ANNOTATION = Pattern.compile("^@Async\\b");

    @Test
    @DisplayName("全量源码：不存在 @Async 方法的同类自调用（自调用会让 @Async 静默失效）")
    void noAsyncSelfInvocationInMainSources() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                violations.addAll(scan(source, SOURCE_ROOT.relativize(file).toString()));
            }
        }
        assertTrue(violations.isEmpty(),
                "@Async 方法被同类自调用 → 代理不生效、实际同步执行。请把该方法抽到独立 Bean：\n  "
                        + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("扫描器自检：能识别自调用，且不误报注释提及/外部调用")
    void scannerDetectsSelfInvocationOnly() {
        String selfCall = """
                class A {
                    @Async
                    public void logAsync(String x) { }
                    public void run() {
                        logAsync("x");
                    }
                }
                """;
        assertEquals(1, scan(selfCall, "A.java").size(), "同类内调用应被识别为违规");

        String overloadSelfCall = """
                class B {
                    @Async
                    public void record(String a) {
                        record(a, null);
                    }
                    @Async
                    public void record(String a, Long b) { }
                }
                """;
        assertEquals(2, scan(overloadSelfCall, "B.java").size(), "两个重载各自的自调用都应被识别");

        String externalOnly = """
                class C {
                    @Async
                    public void logAsync(String x) { }
                }
                """;
        assertTrue(scan(externalOnly, "C.java").isEmpty(), "无自调用不应报错");

        String commentMentionOnly = """
                class D {
                    /**
                     * 供调用方使用：logAsync(...)
                     * 另见 // logAsync(x)
                     */
                    @Async
                    public void logAsync(String x) { }
                }
                """;
        assertTrue(scan(commentMentionOnly, "D.java").isEmpty(), "注释/javadoc 中的提及不算调用");

        // 带执行器名的写法同样必须被识别（真实案例：AiTaskAsyncExecutor#execute）
        String qualifiedAsyncSelfCall = """
                class E {
                    @Async("aiTaskExecutor")
                    public void execute(Long id) { }
                    public void submit() {
                        execute(1L);
                    }
                }
                """;
        assertEquals(1, scan(qualifiedAsyncSelfCall, "E.java").size(),
                "带执行器名的 @Async 写法必须同样纳入扫描");
    }

    // ==================== 扫描实现 ====================

    /**
     * 扫描单个源文件，返回违规描述
     *
     * @param source   源码文本
     * @param fileName 文件名（仅用于描述）
     */
    static List<String> scan(String source, String fileName) {
        List<String> violations = new ArrayList<>();
        String[] lines = source.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            if (!ASYNC_ANNOTATION.matcher(lines[i].trim()).find()) {
                continue;
            }
            String methodName = nextMethodName(lines, i + 1);
            if (methodName == null) {
                continue;
            }
            int calls = 0;
            for (int j = 0; j < lines.length; j++) {
                if (isCommentLine(lines[j])) {
                    continue;
                }
                if (containsCall(lines[j], methodName)) {
                    calls++;
                }
            }
            // 恰好 1 次 = 方法声明本身；>1 即存在本文件内的调用（自调用）
            if (calls > 1) {
                violations.add(fileName + ":" + (i + 1) + " @Async 方法 " + methodName
                        + "() 在本文件内被调用 " + (calls - 1) + " 次（自调用使 @Async 失效）");
            }
        }
        return violations;
    }

    /** 从注解之后的第一行方法声明中取方法名 */
    private static String nextMethodName(String[] lines, int from) {
        for (int i = from; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("*") || trimmed.startsWith("//")
                    || trimmed.startsWith("@") || trimmed.startsWith("/*")) {
                continue;
            }
            Matcher m = METHOD_DECL.matcher(line);
            if (m.find()) {
                return m.group(1);
            }
            if (trimmed.contains("(")) {
                return null; // 声明形态不认识，宁可不报也不误报
            }
        }
        return null;
    }

    private static boolean isCommentLine(String line) {
        String t = line.trim();
        return t.startsWith("*") || t.startsWith("//") || t.startsWith("/*");
    }

    /** 行内是否出现 {@code name(}（且不是 {@code .name(} 的外部调用写法） */
    private static boolean containsCall(String line, String methodName) {
        Pattern p = Pattern.compile("(?<![\\w.])" + Pattern.quote(methodName) + "\\s*\\(");
        return p.matcher(line).find();
    }
}
