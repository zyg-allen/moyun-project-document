package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：LLM 输出的 JSON 抠取 / 围栏剥离**只能有唯一实现**（{@code com.moyun.util.json.LlmJsonExtractor}）
 *
 * <h3>为什么需要这条守卫</h3>
 * <p>模型输出不是结构化协议：会带 markdown 围栏、前后夹说明文字、直接给数组、也可能被 max_tokens 截断。
 * 这段"抠 JSON"的逻辑一旦各写各的，就会各自漏掉一半场景。v13.19 收敛前全仓有 <b>4 处同族实现</b>：</p>
 * <ul>
 *   <li>{@code ext/cms/service/LlmJsonExtractor} —— **零调用方的死代码**（且只处理"围栏在首行"）；</li>
 *   <li>{@code AbstractAiSceneHandler.extractJson} —— 另有内联的"首个 {@code [} 到最后一个 {@code ]}"数组分支；</li>
 *   <li>{@code VoiceInterviewServiceImpl.extractJsonObject} —— 无围栏处理；</li>
 *   <li>{@code WorkflowGeneratorServiceImpl.extractJson} —— 什么都匹配不上时返回 null。</li>
 * </ul>
 * <p>另有 2 处内联"剥围栏取纯文本"（{@code AbstractAiSceneHandler.cleanLlmText}、
 * {@code PromptGeneratorServiceImpl.cleanResponse}）——一并收敛到 {@code stripCodeFence}。</p>
 *
 * <h3>规则</h3>
 * <ol>
 *   <li>业务代码中**不得**出现 {@code lastIndexOf('}')}/{@code lastIndexOf("}")} 这类"首括号到末括号"抠取；
 *   </li>
 *   <li>业务代码中**不得**出现 {@code ```}/{@code ```json} 围栏字符串字面量（应调 {@code stripCodeFence}）。</li>
 * </ol>
 * <p>唯一实现自身（{@code com/moyun/util/json/LlmJsonExtractor.java}）在扫描时排除；
 * 白名单为空——已知的 6 处（4 抠取 + 2 剥围栏）已全部收敛。</p>
 *
 * @author moyun
 */
class LlmJsonExtractionGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    /** 唯一实现的相对路径（扫描时排除） */
    private static final String CANONICAL = "com/moyun/util/json/LlmJsonExtractor.java";

    /** 手写"首括号到末括号"抠取 */
    private static final Pattern HAND_ROLLED_SLICE = Pattern.compile(
            "lastIndexOf\\s*\\(\\s*'}'\\s*\\)|lastIndexOf\\s*\\(\\s*\"}\"\\s*\\)");

    /** 手写 markdown 围栏处理（字符串字面量里出现三个反引号，可带 json 语言标注） */
    private static final Pattern HAND_ROLLED_FENCE = Pattern.compile("`{3}(json)?");

    /** "在同一行做处理"的调用（用于把提示词模板里的围栏与解析时的围栏剥离开） */
    private static final Pattern PROCESSING_CALL = Pattern.compile(
            "\\b(indexOf|lastIndexOf|startsWith|endsWith|replaceAll|replace|substring|split|contains)\\s*\\(");

    /** 白名单：{@code 相对路径} → 理由（当前为空） */
    private static final Map<String, String> WHITELIST = Map.of();

    @Test
    @DisplayName("全量源码：不存在第二处手写 JSON 抠取/围栏剥离（唯一实现在 util.json）")
    void onlyOneImplementation() throws IOException {
        List<String> violations = new ArrayList<>();
        int files = 0;
        boolean canonicalFound = false;

        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
                if (CANONICAL.equals(relative)) {
                    canonicalFound = true;
                    continue;
                }
                files++;
                // 只剥注释、**保留字符串字面量**：本守卫要抓的"围栏"恰好写在字符串里
                // （`TransactionRemoteIoGuardTest.stripCommentsAndStrings` 会把字符串内容抹掉，
                //  用它会让这条规则永远匹配不到——本守卫第一版就踩了这个坑，fixture 已改为走同一管线）
                String source = stripCommentsOnly(Files.readString(file, StandardCharsets.UTF_8));
                if (HAND_ROLLED_SLICE.matcher(source).find()) {
                    violations.add(relative + "：手写\"首个 { 到最后一个 }\"抠取 → 改用 LlmJsonExtractor.extract/extractNode");
                }
                // 围栏：只有"同一行还在做处理"才算违规——提示词模板里写 ```json 是**合法**用法
                // （告诉模型用围栏包裹），不能与"解析时剥围栏"混为一谈
                String[] lines = source.split("\\R", -1);
                for (int i = 0; i < lines.length; i++) {
                    if (HAND_ROLLED_FENCE.matcher(lines[i]).find() && PROCESSING_CALL.matcher(lines[i]).find()) {
                        violations.add(relative + ":" + (i + 1)
                                + "：手写 markdown 围栏处理 → 改用 LlmJsonExtractor.stripCodeFence");
                        break;
                    }
                }
            }
        }

        List<String> unexplained = violations.stream()
                .filter(v -> WHITELIST.keySet().stream().noneMatch(v::startsWith))
                .sorted(Comparator.naturalOrder())
                .toList();

        System.out.println("[LlmJsonExtractionGuard] 扫描文件=" + files
                + "，唯一实现存在=" + canonicalFound + "，违规=" + violations.size());
        assertTrue(canonicalFound, "未找到唯一实现 " + CANONICAL + "——守卫路径可能已失效");
        assertTrue(files > 100, "扫描器未覆盖到源码（文件数=" + files + "）");
        assertTrue(unexplained.isEmpty(),
                "LLM JSON 抠取/围栏剥离出现第二个实现（收敛前有 4 处同族 + 2 处剥围栏）：\n  "
                        + String.join("\n  ", unexplained));
    }

    @Test
    @DisplayName("扫描器自检：手写抠取/围栏必须命中，注释中的字样与正确用法不得误报")
    void scannerSelfCheck() {
        // 1. 手写"首括号到末括号"（收敛前的真实形态）——走与主扫描相同的管线
        assertTrue(hits("int end = text.lastIndexOf('}');"), "手写括号切片必须被识别");
        assertTrue(hits("int end = raw.lastIndexOf(\"}\");"), "双引号写法同样要识别");

        // 2. 手写围栏处理（字符串字面量里出现三个反引号）
        assertTrue(hits("int p = text.indexOf(\"```json\");"), "```json 必须被识别");
        assertTrue(hits("int p = text.lastIndexOf(\"```\");"), "``` 必须被识别");

        // 3. 注释中的字样不算（同一条管线里剥离注释）
        assertTrue(!hits("""
                class A {
                    // 历史实现：int end = text.lastIndexOf('}');
                    /** 也提过 ```json 围栏 */
                    void run() { }
                }
                """), "注释里的括号切片/围栏字样不得误报");

        // 4. 正确用法（调唯一实现）不得误报
        assertTrue(!hits("""
                class B {
                    void run(String raw) {
                        String body = LlmJsonExtractor.extract(raw);
                        String text = LlmJsonExtractor.stripCodeFence(raw);
                    }
                }
                """), "调用唯一实现是正确写法");

        // 5. 普通字符串字面量里的花括号（非围栏）不得误报
        assertTrue(!hits("String s = \"a{b}c\";"), "普通字符串不得误报");

        // 6. 提示词模板里的围栏是**合法**用法（告诉模型用围栏包裹），不得误报
        assertTrue(!hits("""
                class C {
                    String prompt() {
                        StringBuilder sb = new StringBuilder();
                        sb.append("请输出 ```json\\n");
                        sb.append("{\\"a\\":1}\\n```\\n");
                        return sb.toString();
                    }
                }
                """), "提示词模板里的围栏不是解析逻辑，不得误报");
        assertTrue(hits("sql = sql.replaceAll(\"```sql\\\\s*\", \"\");"),
                "解析时手写剥围栏必须命中（与提示词模板区分开）");
    }

    /** 与主扫描同管线：剥注释 → 括号切片命中，或"围栏 + 同行处理调用"命中 */
    private static boolean hits(String source) {
        String stripped = stripCommentsOnly(source);
        if (HAND_ROLLED_SLICE.matcher(stripped).find()) {
            return true;
        }
        for (String line : stripped.split("\\R", -1)) {
            if (HAND_ROLLED_FENCE.matcher(line).find() && PROCESSING_CALL.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 只剥注释（{@code //} 与 {@code /* *}{@code /}），**保留字符串字面量内容**
     *
     * <p>与 {@code TransactionRemoteIoGuardTest.stripCommentsAndStrings} 的差别正在这里：
     * 后者连字符串内容一起抹掉，会让"字符串里写死 ``` 围栏"这类目标漏检。</p>
     */
    static String stripCommentsOnly(String src) {
        StringBuilder sb = new StringBuilder(src.length());
        int i = 0;
        while (i < src.length()) {
            char c = src.charAt(i);
            char c2 = i + 1 < src.length() ? src.charAt(i + 1) : '\0';
            if (c == '/' && c2 == '/') {
                while (i < src.length() && src.charAt(i) != '\n') {
                    sb.append(' ');
                    i++;
                }
            } else if (c == '/' && c2 == '*') {
                sb.append("  ");
                i += 2;
                while (i < src.length() && !(src.charAt(i) == '*' && i + 1 < src.length() && src.charAt(i + 1) == '/')) {
                    sb.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < src.length()) {
                    sb.append("  ");
                    i += 2;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
