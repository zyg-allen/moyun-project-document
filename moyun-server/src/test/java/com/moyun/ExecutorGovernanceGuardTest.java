package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：异步任务必须有<strong>受管执行器</strong>，裸线程/裸线程池只允许在白名单内出现
 *
 * <h3>为什么需要这条守卫（v13.5 的真实缺陷）</h3>
 * <p>{@code CompletableFuture.runAsync(task)} 不传执行器时，用的是
 * {@code ForkJoinPool.commonPool()}——<strong>全 JVM 共享、并行度只有 CPU-1</strong>，
 * 而且它同时服务于 {@code parallelStream()} 等所有公共并行任务。而本项目的异步点恰恰
 * 都是"长任务"（架构图对话 {@code latch.await(5, MINUTES)}、文档向量化、工作流流式执行），
 * 几个并发请求就能把公共池占满，症状是<strong>全站并行任务一起变慢/卡住</strong>，
 * 且日志里没有任何"线程池满"的痕迹。</p>
 *
 * <p>裸 {@code new Thread(...)} / {@code Executors.new*} 则是另一类问题：
 * 线程无命名（出问题无法定位）、队列无上限（可无限堆积）、无优雅停机
 * （应用关闭时任务被硬杀）、并发数完全不受控。</p>
 *
 * <p>因此本守卫对 {@code src/main/java} 全量源码做静态扫描，两条硬约束：</p>
 * <ol>
 *   <li>{@code CompletableFuture.runAsync/supplyAsync} <strong>必须显式传入执行器</strong>；</li>
 *   <li>{@code Executors.new*} / {@code new Thread(} <strong>只允许出现在白名单文件中</strong>，
 *       且白名单每条都必须写明"为什么不能池化"（白名单条目失效会被本测试抓出）。</li>
 * </ol>
 *
 * <p>命名规范：受管池统一定义在 {@code core.config.AsyncTaskConfig}（系统级）与
 * {@code ext.ai.config.AsyncConfig}（AI 模块专用），业务代码只做
 * {@code @Qualifier} 注入。</p>
 *
 * @author moyun
 * @see AsyncSelfInvocationGuardTest 同族守卫（{@code @Async} 禁止同类自调用）
 */
class ExecutorGovernanceGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    /** {@code CompletableFuture.runAsync(...)} / {@code supplyAsync(...)} 调用点 */
    private static final Pattern ASYNC_CALL = Pattern.compile(
            "CompletableFuture\\s*\\.\\s*(runAsync|supplyAsync)\\s*\\(");

    /** 裸线程 / 裸线程池创建点 */
    private static final Pattern RAW_THREAD = Pattern.compile(
            "\\bExecutors\\s*\\.\\s*new\\w+|\\bnew\\s+Thread\\s*\\(");

    /**
     * 裸线程白名单：文件相对路径 → 允许保留的理由（必须写明"为什么不能池化"）。
     *
     * <p>注意：白名单只放"池化会改变语义或引入更糟后果"的场景，
     * 不是"懒得改"的豁免清单——白名单里的条目同样必须自己实现生命周期收口。</p>
     */
    private static final Map<String, String> RAW_THREAD_ALLOWLIST = new LinkedHashMap<>();

    static {
        RAW_THREAD_ALLOWLIST.put(
                "com/moyun/portal/judge/JudgeAsyncWorker.java",
                "常驻 worker loop 模型：N 个线程各自 BLPOP 阻塞消费（N = judge.worker.concurrency），"
                        + "不是'提交任务'模型；已实现完整生命周期（@PostConstruct 启动 + "
                        + "@PreDestroy shutdown/awaitTermination/shutdownNow）。改为受管池会破坏"
                        + "'固定 worker 数 + 各自阻塞消费'的语义");
        RAW_THREAD_ALLOWLIST.put(
                "com/moyun/portal/handler/AsrStreamRelayHandler.java",
                "每个 ASR 会话一个'10 分钟安全上限'延时任务：单线程守护调度器、无跨请求状态、"
                        + "进程退出即失效（没有需要落盘的待办）");
        RAW_THREAD_ALLOWLIST.put(
                "com/moyun/core/redis/DistributedLockUtil.java",
                "分布式锁看门狗续期：单线程守护、按锁 token 动态注册/取消；进程退出后锁由 TTL 自然过期，"
                        + "无优雅停机诉求");
        RAW_THREAD_ALLOWLIST.put(
                "com/moyun/ext/cms/service/CodeExecutorService.java",
                "OJ 每次代码执行的 stdout/stderr 排水线程：必须与子进程生命周期绑定（进程退出即阻塞解除）。"
                        + "若改用共享池，一个'既不输出也不退出'的子进程读线程会永久占死池线程；"
                        + "此处线程数天然被'并发执行数'限制");
    }

    @Test
    @DisplayName("全量源码：CompletableFuture.runAsync/supplyAsync 必须显式传入执行器")
    void noExecutorLessAsyncInMainSources() throws IOException {
        List<String> violations = new ArrayList<>();
        forEachMainSource((file, source) -> violations.addAll(findExecutorLessAsync(source, file)));
        assertTrue(violations.isEmpty(),
                "禁止使用默认 ForkJoinPool.commonPool()（并行度=CPU-1，会被长任务占满，"
                        + "导致全站并行任务饿死）。请注入受管执行器作为第二个参数"
                        + "（受管池见 core.config.AsyncTaskConfig / ext.ai.config.AsyncConfig）：\n  "
                        + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("全量源码：裸线程/裸线程池只能出现在白名单文件内")
    void rawThreadCreationOnlyInAllowlist() throws IOException {
        List<String> violations = new ArrayList<>();
        forEachMainSource((file, source) -> {
            if (RAW_THREAD_ALLOWLIST.containsKey(file)) {
                return;
            }
            Matcher m = RAW_THREAD.matcher(source);
            while (m.find()) {
                violations.add(file + ":" + lineOf(source, m.start())
                        + " 裸线程/裸线程池创建：" + m.group().replaceAll("\\s+", " "));
            }
        });
        assertTrue(violations.isEmpty(),
                "禁止裸 new Thread/Executors.new*（无命名、队列无上限、无优雅停机、并发不受控）。"
                        + "请使用受管执行器；若确属'池化会改变语义'的场景，"
                        + "需在本测试的 RAW_THREAD_ALLOWLIST 中登记并写明理由：\n  "
                        + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("白名单自检：条目文件必须存在，且确实仍含裸线程创建（防止过期豁免）")
    void allowlistIsNotStale() throws IOException {
        for (Map.Entry<String, String> entry : RAW_THREAD_ALLOWLIST.entrySet()) {
            Path file = SOURCE_ROOT.resolve(entry.getKey());
            assertTrue(Files.exists(file), "白名单条目已失效（文件不存在），请删除：" + entry.getKey());
            String source = stripCommentsAndLiterals(Files.readString(file, StandardCharsets.UTF_8));
            assertTrue(RAW_THREAD.matcher(source).find(),
                    "白名单条目已失效（该文件已不再创建裸线程），请删除：" + entry.getKey());
            assertTrue(entry.getValue() != null && entry.getValue().length() > 20,
                    "白名单必须写明理由：" + entry.getKey());
        }
    }

    @Test
    @DisplayName("扫描器自检：无执行器识别 + 注释不误报 + 有执行器不报")
    void scannerDetectsExecutorLessAsyncOnly() {
        String noExecutor = """
                class A {
                    void run() {
                        CompletableFuture.runAsync(() -> doWork());
                    }
                }
                """;
        assertEquals(1, findExecutorLessAsync(stripCommentsAndLiterals(noExecutor), "A.java").size(),
                "单参数 runAsync 必须被识别为违规");

        String withExecutor = """
                class B {
                    void run() {
                        CompletableFuture.runAsync(() -> doWork(), executor);
                    }
                }
                """;
        assertTrue(findExecutorLessAsync(stripCommentsAndLiterals(withExecutor), "B.java").isEmpty(),
                "显式传入执行器不应报错");

        String multiLineArgs = """
                class C {
                    void run() {
                        CompletableFuture.runAsync(() -> {
                            doWork();
                        }, sseStreamExecutor);
                    }
                }
                """;
        assertTrue(findExecutorLessAsync(stripCommentsAndLiterals(multiLineArgs), "C.java").isEmpty(),
                "跨行的 lambda + 执行器（本项目最常见写法）不应误报");

        String comparisonInsideLambda = """
                class D {
                    void run() {
                        CompletableFuture.runAsync(() -> {
                            if (a < b) { doWork(); }
                        }, executor);
                    }
                }
                """;
        assertTrue(findExecutorLessAsync(stripCommentsAndLiterals(comparisonInsideLambda), "D.java").isEmpty(),
                "lambda 体内的比较运算符不能把参数切分搞错");

        String commentOnly = """
                class E {
                    // CompletableFuture.runAsync(() -> doWork());
                    /**
                     * 历史写法：CompletableFuture.runAsync(task)
                     */
                    void run() {
                        CompletableFuture.runAsync(() -> doWork(), executor);
                    }
                }
                """;
        assertTrue(findExecutorLessAsync(stripCommentsAndLiterals(commentOnly), "E.java").isEmpty(),
                "注释中的历史写法不算违规（守卫扫描前必须剥离注释/字符串）");

        String supplyAsync = """
                class F {
                    void run() {
                        CompletableFuture.supplyAsync(() -> load());
                    }
                }
                """;
        assertEquals(1, findExecutorLessAsync(stripCommentsAndLiterals(supplyAsync), "F.java").size(),
                "supplyAsync 同样必须指定执行器");
    }

    @Test
    @DisplayName("扫描器自检：裸线程识别 + 注释/字符串不误报")
    void scannerDetectsRawThreadCreationOnly() {
        assertTrue(RAW_THREAD.matcher(stripCommentsAndLiterals(
                "class A { void r() { new Thread(() -> go()).start(); } }")).find(),
                "new Thread 必须被识别");

        assertTrue(RAW_THREAD.matcher(stripCommentsAndLiterals(
                "class B { Object p = Executors.newFixedThreadPool(10); }")).find(),
                "Executors.new* 必须被识别");

        assertTrue(!RAW_THREAD.matcher(stripCommentsAndLiterals(
                "class C { /** Executors.newFixedThreadPool(10) 已删除 */ void r() { } }")).find(),
                "注释中的历史写法不算违规");

        assertTrue(!RAW_THREAD.matcher(stripCommentsAndLiterals(
                "class D { String s = \"new Thread(x)\"; void r() { } }")).find(),
                "字符串字面量中的文本不算违规");
    }

    // ==================== 扫描实现 ====================

    /** 遍历 {@code src/main/java} 全部 Java 源文件（已剔除注释与字符串字面量），回调处理 */
    private static void forEachMainSource(SourceConsumer consumer) throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = stripCommentsAndLiterals(Files.readString(file, StandardCharsets.UTF_8));
                consumer.accept(SOURCE_ROOT.relativize(file).toString().replace('\\', '/'), source);
            }
        }
    }

    /** 找出未指定执行器的 {@code runAsync}/{@code supplyAsync} 调用 */
    static List<String> findExecutorLessAsync(String source, String fileName) {
        List<String> violations = new ArrayList<>();
        Matcher m = ASYNC_CALL.matcher(source);
        while (m.find()) {
            int open = m.end() - 1;
            int close = matchParen(source, open);
            if (close < 0) {
                continue;
            }
            String args = source.substring(open + 1, close);
            if (countTopLevelArgs(args) < 2) {
                violations.add(fileName + ":" + lineOf(source, m.start())
                        + " CompletableFuture." + m.group(1) + "(...) 未指定执行器 → 默认落 commonPool");
            }
        }
        return violations;
    }

    /** 返回 {@code openIndex} 处 {@code (} 对应的 {@code )} 下标；不匹配返回 -1 */
    private static int matchParen(String s, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 统计参数个数（顶层逗号 + 1）。
     *
     * <p>只跟踪 {@code () [] {}} 三种括号：Java 中泛型实参里的逗号
     * （{@code new Foo<A, B>()}）必然出现在其他括号内，故无需跟踪尖括号——
     * 这样也避免了把 lambda 体内的比较运算符 {@code a < b} 误当泛型括号。</p>
     */
    private static int countTopLevelArgs(String args) {
        if (args.isBlank()) {
            return 0;
        }
        int depth = 0;
        int count = 1;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                count++;
            }
        }
        return count;
    }

    private static int lineOf(String source, int index) {
        int line = 1;
        for (int i = 0; i < index && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * 抹掉注释与字符串/字符/文本块字面量的内容（保留换行以维持行号）。
     *
     * <p>目的：避免"文档里写了历史写法"或"字符串里含 new Thread"造成误报。</p>
     */
    static String stripCommentsAndLiterals(String src) {
        StringBuilder out = new StringBuilder(src.length());
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                while (i < n && src.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                out.append("  ");
                i += 2;
                while (i < n && !(src.charAt(i) == '*' && i + 1 < n && src.charAt(i + 1) == '/')) {
                    out.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("  ");
                    i += 2;
                }
            } else if (c == '"' && i + 2 < n && src.charAt(i + 1) == '"' && src.charAt(i + 2) == '"') {
                // 文本块
                out.append("   ");
                i += 3;
                while (i < n && !(src.charAt(i) == '"' && i + 2 < n
                        && src.charAt(i + 1) == '"' && src.charAt(i + 2) == '"')) {
                    out.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("   ");
                    i += 3;
                }
            } else if (c == '"' || c == '\'') {
                char quote = c;
                out.append(' ');
                i++;
                while (i < n) {
                    char d = src.charAt(i);
                    if (d == '\\') {
                        out.append("  ");
                        i += 2;
                        continue;
                    }
                    if (d == quote) {
                        out.append(' ');
                        i++;
                        break;
                    }
                    out.append(d == '\n' ? '\n' : ' ');
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** 源文件消费回调 */
    @FunctionalInterface
    private interface SourceConsumer {
        void accept(String relativePath, String strippedSource);
    }
}
