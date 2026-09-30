package com.moyun.ext.cms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「统一 LLM 调用端口」架构守卫（v13.52 批次 4 二）。
 *
 * <p><b>为什么需要本守卫</b>：方案 V1.2 §4 的目标是「面试族 AI 调用 100% 走网关」，
 * 但"走网关"这件事**只靠代码评审维持**是脆弱的 —— 后人加一句
 * {@code agentClient.chat(...)} 就能绕过限流/成本熔断/版本锁/执行日志，
 * 且**编译与既有测试都不会报错**（静默退化）。本守卫把该约束钉死为可回归的断言。</p>
 *
 * <p><b>判定口径</b>：扫描 {@code com/moyun/ext/cms} 与 {@code com/moyun/portal}
 * 下的 Java 源码，**剔除注释行与块注释**后，若出现直连模型调用（{@code agentClient.chat(}）
 * 即判定失败 —— 所有 LLM 调用必须走以下两个网关入口之一：</p>
 * <ul>
 *   <li>{@code AiSceneJsonClient#executeForJson}（同步/JSON 任务型场景）</li>
 *   <li>{@code AiGatewayService#executeConversationStream}（会话流式）</li>
 * </ul>
 *
 * <p><b>允许的例外</b>：{@code InterviewAgentClient} 接口自身的定义与实现
 * （它只是网关能力的适配层，不是业务直连）。</p>
 *
 * @author laomao
 */
class LlmCallPortGuardTest {

    /** 扫描根目录（相对模块根） */
    private static final String[] SCAN_ROOTS = {
            "src/main/java/com/moyun/ext/cms",
            "src/main/java/com/moyun/portal"
    };

    /** 直连模型的调用形态（业务代码出现即违规） */
    private static final Pattern DIRECT_CHAT = Pattern.compile("\\bagentClient\\s*\\.\\s*chat\\s*\\(");

    /** 适配层自身文件（允许出现该方法名——它是在**定义**能力而非**调用**模型） */
    private static final List<String> ALLOWED_FILES = List.of(
            "InterviewAgentClient.java",
            "InterviewAgentClientImpl.java"
    );

    @Test
    @DisplayName("业务代码零直连：ext.cms / portal 下不得出现 agentClient.chat(（注释除外）")
    void noDirectModelChatInBusinessCode() throws IOException {
        List<String> violations = new ArrayList<>();
        for (String root : SCAN_ROOTS) {
            Path dir = Paths.get(root);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                for (Path p : walk.filter(Files::isRegularFile)
                        .filter(f -> f.toString().endsWith(".java"))
                        .toList()) {
                    String name = p.getFileName().toString();
                    if (ALLOWED_FILES.contains(name)) {
                        continue;
                    }
                    String code = stripComments(Files.readString(p, StandardCharsets.UTF_8));
                    Matcher m = DIRECT_CHAT.matcher(code);
                    while (m.find()) {
                        int line = lineOf(code, m.start());
                        violations.add(p + ":" + line + " 出现直连模型调用：" + m.group());
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(),
                "面试链 LLM 调用必须 100% 走网关（AiSceneJsonClient#executeForJson "
                        + "或 AiGatewayService#executeConversationStream），"
                        + "直连会绕过限流/成本熔断/版本锁/执行日志。违规点：\n"
                        + String.join("\n", violations));
    }

    @Test
    @DisplayName("网关入口存在性：统一端口的方法签名不得被改名/删除")
    void gatewayEntryPointsExist() throws IOException {
        Path jsonClient = Paths.get("src/main/java/com/moyun/ext/aigateway/support/AiSceneJsonClient.java");
        assertTrue(Files.exists(jsonClient), "AiSceneJsonClient 必须存在（同步/JSON 任务型场景的唯一入口）");
        String a = Files.readString(jsonClient, StandardCharsets.UTF_8);
        assertTrue(a.contains("executeForJson"),
                "AiSceneJsonClient#executeForJson 是任务型场景的唯一入口，不得改名");

        Path gateway = Paths.get("src/main/java/com/moyun/ext/aigateway/service/AiGatewayService.java");
        assertTrue(Files.exists(gateway), "AiGatewayService 必须存在");
        String b = Files.readString(gateway, StandardCharsets.UTF_8);
        assertTrue(b.contains("executeConversationStream"),
                "AiGatewayService#executeConversationStream 是会话流式的唯一入口，不得改名");
        // 会话流式必须经过治理三件套（限流 / 成本熔断 / 版本锁）——否则"走网关"名不副实
        assertTrue(b.contains("resolveSessionConfig"),
                "会话流式必须经 resolveSessionConfig（按首轮锁定版本读快照）");
        assertTrue(b.contains("rateLimiter"), "会话流式必须经限流");
        assertTrue(b.contains("tokenCostGuard"), "会话流式必须经成本熔断");
    }

    @Test
    @DisplayName("自我验证：stripComments 确实能识别注释中的调用（防止守卫被注释骗过）")
    void stripCommentsWorks() {
        String sample = "// 原实现 agentClient.chat(...) 直连模型\n"
                + "/* 块注释里的 agentClient.chat(a, b) */\n"
                + "String s = foo.bar();\n"
                + "real.agentClient.chat(x);\n";
        String stripped = stripComments(sample);
        assertTrue(stripped.contains("real.agentClient.chat(x);"),
                "真实调用必须保留（否则守卫会漏判）");
        assertFalse(stripped.contains("原实现 agentClient.chat"),
                "行注释中的调用必须被剔除（否则守卫会有误报）");
        assertFalse(stripped.contains("块注释里的"),
                "块注释内容必须被剔除");
    }

    // ========================================================================
    // 工具：剥离注释（行注释 + 块注释），保留字符串字面量内的内容不动
    // ========================================================================
    private static String stripComments(String src) {
        StringBuilder sb = new StringBuilder(src.length());
        boolean inLine = false;
        boolean inBlock = false;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            char next = i + 1 < src.length() ? src.charAt(i + 1) : '\0';

            if (inLine) {
                if (c == '\n') {
                    inLine = false;
                    sb.append(c);
                }
                continue;
            }
            if (inBlock) {
                if (c == '*' && next == '/') {
                    inBlock = false;
                    i++;
                } else if (c == '\n') {
                    sb.append(c); // 保留换行以维持行号
                }
                continue;
            }
            if (inString) {
                sb.append(c);
                if (c == '\\') {
                    if (next != '\0') {
                        sb.append(next);
                        i++;
                    }
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                sb.append(c);
                if (c == '\\') {
                    if (next != '\0') {
                        sb.append(next);
                        i++;
                    }
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }

            if (c == '/' && next == '/') {
                inLine = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inBlock = true;
                i++;
                continue;
            }
            if (c == '"') {
                inString = true;
                sb.append(c);
                continue;
            }
            if (c == '\'') {
                inChar = true;
                sb.append(c);
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** 字符偏移 → 1-based 行号 */
    private static int lineOf(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
