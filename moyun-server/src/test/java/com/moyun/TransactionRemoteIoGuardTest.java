package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：{@code @Transactional} 方法内**不得出现远程/磁盘 IO**
 *
 * <h3>为什么需要这条守卫</h3>
 * <p>事务一旦开启就占着 <b>DB 连接</b>（本项目 Druid 连接池容量有限）。把秒级的远程 IO 放进事务里，
 * 等于"用连接池额度去等网络"：慢模型/慢对象存储下并发几个请求就能把连接池打满，表现为
 * "接口莫名变慢 + 其它接口拿不到连接"。更隐蔽的是它<b>不报错、不留日志</b>——只有并发压测才暴露。</p>
 *
 * <p>本项目真实案例（v13.14 修复，均为"看起来只写了 DB"的方法）：</p>
 * <ul>
 *   <li>{@code KnowledgeBaseServiceImpl.uploadFileOnly}：整体 {@code @Transactional}，方法体内
 *       {@code minioService.uploadKnowledgeFile(...)}（对象存储上传）在事务里；</li>
 *   <li>{@code VoiceInterviewServiceImpl.start}：整体 {@code @Transactional}，方法体内 RAG 检索 +
 *       多次 LLM 往返（warmup/开场白）在事务里 —— 一次开面最慢的十几秒全程占着连接；</li>
 *   <li>{@code VoiceInterviewServiceImpl.requestHint}：整体 {@code @Transactional}，LLM 调用在事务里；</li>
 *   <li>{@code SysFileServiceImpl.uploadFile/uploadFileForPortal/uploadBytes/deleteFileById/
 *       deleteFileByIds/deleteFileByUrl}：MinIO 上传/删除与本地磁盘写入都在事务里。</li>
 * </ul>
 *
 * <p>修法统一为：<b>把事务边界收窄到"只包 DB 写"</b>——远程 IO 全部前置（或后置）到事务外，
 * 用 {@code TransactionTemplate} 精确框住 DB 写步骤，DB 写失败仍整体回滚（语义等价），
 * 远程 IO 失败的语义与旧实现保持一致（见各处方法注释）。</p>
 *
 * <h3>本守卫覆盖什么</h3>
 * <ul>
 *   <li><b>直接命中</b>：{@code @Transactional} 方法体内出现下列标记之一；</li>
 *   <li><b>间接命中</b>：{@code @Transactional} 方法调用了<b>同文件内</b>一个"体内含标记"的方法
 *       （私有 helper 是最常见形态，例如 {@code SysFileServiceImpl.uploadFile(...)} 把 MinIO 放在
 *       私有重载里、{@code deleteFileByIds} 自调用 {@code deleteFileById}）；</li>
 *   <li><b>类级 {@code @Transactional}</b>：类上标注等于"每个方法都在事务里"，含 IO 的方法无法豁免，
 *       一律要求显式说明。</li>
 * </ul>
 *
 * <h3>刻意排除（不是遗漏，是口径）</h3>
 * <ul>
 *   <li><b>DB 自身</b>（{@code mapper.}/{@code baseMapper.}/{@code ServiceImpl} 的 save/update）——那正是事务要保护的对象；</li>
 *   <li><b>Redis / 缓存</b>（{@code redisTemplate.}、{@code memoryProvider.} 等）——毫秒级往返，
 *       且多处与 DB 行存在性强一致（如开面时写滑窗，Redis 失败就该回滚"建会话"，否则会留下
 *       "有会话无记忆"的降级态），故不纳入本口径；</li>
 *   <li><b>纯 CPU</b>（MD5、分段、JSON 序列化）——不产生 IO 等待。</li>
 * </ul>
 *
 * <h3>不覆盖（已知边界，不要误以为已证明）</h3>
 * <p><b>跨类调用链</b>：{@code @Transactional} 方法调用<b>另一个类</b>的远程方法（如
 * {@code someService.callLlm()}）需要跨文件符号解析，本静态守卫不解析；这类形态由报告评审与
 * {@code TransactionRemoteIoRuntimeProbeTest} 的运行时探针兜底。</p>
 *
 * @author moyun
 */
class TransactionRemoteIoGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    /**
     * 远程/磁盘 IO 标记（子串匹配，大小写敏感）
     * <p>新增存储/HTTP/LLM 客户端时请同步补进这里，否则守卫会出现盲区。</p>
     */
    private static final List<String> REMOTE_IO_MARKERS = List.of(
            // 对象存储
            "minioUtils.", "minioService.", "MinioClient", "ossClient",
            // 本地磁盘写入/删除（存储侧 IO，与远程同等对待：不在事务里做）
            "transferTo(", "uploadToLocal(", "uploadBytesToLocal(", "deleteFileFromStorage(",
            "FileOutputStream", "Files.write", "Files.copy", "FileUtils.",
            // HTTP 客户端
            "restTemplate.", "RestTemplate", "WebClient", "OkHttpClient", "okHttpClient",
            "HttpRequest.", "HttpUtil.", "URLConnection", "httpClient.",
            // 邮件 / 短信
            "MailSender", "mailSender", "smsSender", "sendCode(",
            // LLM / RAG / 向量
            "agentClient.", "llmService.", "embeddingModel.", "ragRetrievalService.",
            "imageExtractionService.", "aiGatewayService.", "aiSceneJsonClient.", "vectorStore");

    /**
     * 白名单：key = {@code 相对路径#方法名}，value = 必须写清"为什么这个远程 IO 留在事务内是安全的"。
     * <p><b>当前为空</b>——v13.14 已把全项目命中项全部收窄到"事务只包 DB 写"。
     * 若将来确需豁免，请连同理由一起加，并接受"条目失效即测试失败"的自检。</p>
     */
    private static final Map<String, String> WHITELIST = Map.of();

    /** 控制流关键字：形如 {@code if (x) {} } 的块会被误当作"方法"，这里排除 */
    private static final Set<String> NON_METHOD_KEYWORDS = Set.of(
            "if", "for", "while", "switch", "catch", "synchronized", "try", "else", "do",
            "return", "new", "assert", "case", "throw", "super", "this");

    /** 形如 {@code ...) throws X { } 形态的"参数表结束 + 方法体开始" */
    private static final Pattern PAREN_THEN_BRACE = Pattern.compile(
            "\\)\\s*(?:throws[^{;]*?)?\\{", Pattern.DOTALL);

    /** 类级 {@code @Transactional}：注解后（可带其它注解/修饰符）直接声明类型 */
    private static final Pattern CLASS_LEVEL_TX = Pattern.compile(
            "@Transactional\\b(?:\\([^()]*\\))?\\s*(?:@\\w+(?:\\([^()]*\\))?\\s*)*"
                    + "(?:public|final|abstract|static|\\s)*\\b(?:class|interface|enum|record)\\b");

    @Test
    @DisplayName("全量源码：不存在 @Transactional 方法内做远程/磁盘 IO（含同文件私有 helper 间接调用）")
    void noRemoteIoInsideTransactions() throws IOException {
        List<Violation> all = new ArrayList<>();
        int files = 0;
        int methods = 0;
        int transactionalMethods = 0;
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                files++;
                int[] stats = scanStatistics(source);
                methods += stats[0];
                transactionalMethods += stats[1];
                all.addAll(scan(source, SOURCE_ROOT.relativize(file).toString().replace('\\', '/')));
            }
        }
        all.sort(Comparator.comparing((Violation v) -> v.file).thenComparingInt(v -> v.line));

        // 正向控制：否则解析器一旦失效，守卫会"零违规"地假绿
        System.out.println("[TransactionRemoteIoGuard] 扫描文件=" + files
                + "，识别方法=" + methods + "，其中 @Transactional 方法=" + transactionalMethods
                + "，违规=" + all.size());
        assertTrue(files > 100, "扫描器未覆盖到源码（文件数=" + files + "）——守卫等于空转");
        assertTrue(transactionalMethods >= 50,
                "@Transactional 方法识别数过低（" + transactionalMethods + "）——解析器可能已失效，守卫会假绿");

        List<Violation> unexplained = all.stream()
                .filter(v -> !WHITELIST.containsKey(v.key()))
                .toList();
        assertTrue(unexplained.isEmpty(),
                "事务内出现远程/磁盘 IO → 事务期间占着 DB 连接等网络，并发下会打满连接池。\n"
                        + "修法：远程 IO 前置/后置到事务外，用 TransactionTemplate 只框住 DB 写（v13.14 先例："
                        + "KnowledgeBaseServiceImpl#uploadFileOnly、VoiceInterviewServiceImpl#start、"
                        + "SysFileServiceImpl#uploadFile）：\n  "
                        + String.join("\n  ", unexplained.stream().map(Violation::describe).toList())
                        + (WHITELIST.isEmpty() ? "" : "\n当前白名单（须写清理由）：" + WHITELIST));

        List<String> stale = WHITELIST.keySet().stream()
                .filter(k -> all.stream().noneMatch(v -> v.key().equals(k)))
                .toList();
        assertTrue(stale.isEmpty(),
                "白名单存在已失效条目（该方法已不再违规或已被改名/删除），请删除对应豁免，"
                        + "避免白名单变成「看起来很安全」的噪音：\n  " + String.join("\n  ", stale));
    }

    @Test
    @DisplayName("扫描器自检：直接/间接命中、类级注解、注释与字符串不可误报、Redis 不在口径内")
    void scannerSelfCheck() {
        // 1. 直接命中：@Transactional 方法体内调对象存储
        assertEquals(1, scan("""
                class A {
                    @Transactional
                    public void save(Long id) {
                        String url = minioUtils.uploadFile(file);
                        mapper.insert(id);
                    }
                }
                """, "A.java").size(), "事务内直连对象存储必须命中");

        // 2. 间接命中：IO 放在私有 helper，@Transactional 方法调用它（真实案例 SysFileServiceImpl#uploadFile）
        List<Violation> indirect = scan("""
                class B {
                    @Transactional
                    public void upload(MultipartFile f) {
                        String url = doUpload(f);
                        mapper.insert(url);
                    }
                    private String doUpload(MultipartFile f) {
                        return minioUtils.uploadFile(f);
                    }
                }
                """, "B.java");
        assertEquals(1, indirect.size(), "事务方法调用含 IO 的私有 helper 必须命中（间接）");
        assertTrue(indirect.get(0).helpers().contains("doUpload"),
                "违规描述应指出间接调用的 helper 名，实际：" + indirect.get(0).describe());

        // 3. 自调用另一事务方法且该方法含 IO（真实案例 SysFileServiceImpl#deleteFileByIds）
        assertEquals(2, scan("""
                class C {
                    @Transactional
                    public void deleteAll(Long[] ids) {
                        for (Long id : ids) { deleteOne(id); }
                    }
                    @Transactional
                    public void deleteOne(Long id) {
                        minioUtils.removeFile("x");
                    }
                }
                """, "C.java").size(), "自调用含 IO 的事务方法：调用方（间接）与被调用方（直接）都应命中");

        // 4. 非事务方法里的 IO：不在口径内
        assertTrue(scan("""
                class D {
                    public void upload(MultipartFile f) {
                        String url = minioUtils.uploadFile(f);
                        transactionTemplate.executeWithoutResult(s -> mapper.insert(url));
                    }
                }
                """, "D.java").isEmpty(), "非事务方法内的 IO 不应报错");

        // 5. 事务方法只写 DB（mapper）：不应报错
        assertTrue(scan("""
                class E {
                    @Transactional(rollbackFor = Exception.class)
                    public void update(Entity e) {
                        mapper.updateById(e);
                        mapper.deleteById(e.getId());
                    }
                }
                """, "E.java").isEmpty(), "事务方法内的 DB 写是正常用法");

        // 6. 注释/javadoc 里的 @Transactional 与标记：必须被剥离（v13.14 修复时就踩过这个假阳性）
        assertTrue(scan("""
                class F {
                    /**
                     * 本方法不再整体 @Transactional —— MinIO 上传（minioService.uploadKnowledgeFile）
                     * 属远程 IO，必须在事务外。
                     */
                    public void upload(MultipartFile f) {
                        // minioUtils.uploadFile(f) 已在事务外执行
                        mapper.insert(url);
                    }
                }
                """, "F.java").isEmpty(), "注释内出现的注解名/标记不算违规");

        // 7. 字符串字面量里的标记：不算违规
        assertTrue(scan("""
                class G {
                    @Transactional
                    public void update(Long id) {
                        log.info("skip minioUtils.uploadFile for {}", id);
                        mapper.updateById(id);
                    }
                }
                """, "G.java").isEmpty(), "字符串字面量内的标记不算 IO 调用");

        // 8. Redis / 缓存：按口径排除（见类注释）
        assertTrue(scan("""
                class H {
                    @Transactional
                    public void start(Long id) {
                        mapper.insert(id);
                        memoryService.initFirstTurn(id);
                        redisTemplate.opsForValue().set("k", "v");
                    }
                }
                """, "H.java").isEmpty(), "Redis/缓存按既定口径排除在本守卫之外");

        // 9. 类级 @Transactional：一律要求显式说明
        assertEquals(1, scan("""
                @Transactional
                class I {
                    public void upload(MultipartFile f) { minioUtils.uploadFile(f); }
                }
                """, "I.java").size(), "类级 @Transactional 会让每个方法都进事务，必须显式处理");

        // 10. 多行签名 + throws：解析器必须能定位方法体
        assertEquals(1, scan("""
                class J {
                    @Transactional(rollbackFor = Exception.class)
                    public String upload(MultipartFile file,
                                         String businessType,
                                         String businessId) throws Exception {
                        return minioUtils.uploadFile(file);
                    }
                }
                """, "J.java").size(), "多行签名 / throws 不应让解析器漏掉方法体");

        // 11. 控制流块不应被误当成"方法"，进而造成间接误报
        assertTrue(scan("""
                class K {
                    @Transactional
                    public void run(boolean flag) {
                        if (flag) { log.info("x"); }
                        mapper.insert(flag);
                    }
                    private void helper() { minioUtils.uploadFile(f); }
                }
                """, "K.java").isEmpty(), "if 块不得被当作含 IO 的方法，也不得凭空产生间接命中");
    }

    @Test
    @DisplayName("白名单机制：未豁免项必报、已豁免项放行、失效条目必须暴露")
    void whitelistPartition() {
        Violation v = new Violation("X.java", 10, "upload", "直连 minioUtils.", List.of());
        assertEquals(List.of(v.describe()), partition(List.of(v), Map.of()).unexplained());
        assertEquals(List.of(), partition(List.of(v), Map.of("X.java#upload", "理由")).unexplained());
        assertEquals(List.of("Y.java#gone"),
                partition(List.of(v), Map.of("X.java#upload", "理由", "Y.java#gone", "理由")).stale());
    }

    // ==================== 扫描实现 ====================

    /** 违规项 */
    record Violation(String file, int line, String method, String reason, List<String> helpers) {

        String key() {
            return file + "#" + method;
        }

        String describe() {
            return file + ":" + line + "  " + method + "()  " + reason
                    + (helpers.isEmpty() ? "" : "（间接：本文件内调用 " + String.join("/", helpers) + "）");
        }
    }

    /** 白名单分区结果 */
    record Partition(List<String> unexplained, List<String> stale) {
    }

    /**
     * 正向控制用统计：[方法总数, @Transactional 方法数]
     * <p>守卫"零违规"可能是真干净，也可能是解析器失效；调用方据此断言识别量非零。</p>
     */
    static int[] scanStatistics(String source) {
        String stripped = stripCommentsAndStrings(source);
        List<Method> methods = scanMethods(stripped);
        int tx = (int) methods.stream().filter(m -> isTransactional(stripped, m)).count();
        return new int[]{methods.size(), tx};
    }

    /** 扫描单个源文件 */
    static List<Violation> scan(String source, String fileName) {
        String stripped = stripCommentsAndStrings(source);
        List<Violation> violations = new ArrayList<>();

        Matcher classLevel = CLASS_LEVEL_TX.matcher(stripped);
        while (classLevel.find()) {
            violations.add(new Violation(fileName, lineOf(stripped, classLevel.start()), "<class>",
                    "类级 @Transactional 会让类内每个方法都进事务，无法对「只写 DB」的方法豁免", List.of()));
        }

        List<Method> methods = scanMethods(stripped);
        // 同文件内"体内含 IO 标记"的方法名 → 用于间接命中
        Map<String, String> ioHelpers = new LinkedHashMap<>();
        for (Method m : methods) {
            String marker = firstMarker(m.body());
            if (marker != null) {
                ioHelpers.putIfAbsent(m.name(), marker);
            }
        }

        for (Method m : methods) {
            if (!isTransactional(stripped, m)) {
                continue;
            }
            String direct = firstMarker(m.body());
            List<String> indirect = new ArrayList<>();
            for (Map.Entry<String, String> helper : ioHelpers.entrySet()) {
                if (helper.getKey().equals(m.name())) {
                    continue;
                }
                if (callsMethod(m.body(), helper.getKey())) {
                    indirect.add(helper.getKey());
                }
            }
            if (direct != null) {
                violations.add(new Violation(fileName, m.line(), m.name(),
                        "方法体内直接出现远程/磁盘 IO 标记 [" + direct + "]", List.of()));
            }
            if (!indirect.isEmpty()) {
                violations.add(new Violation(fileName, m.line(), m.name(),
                        "调用了同文件内含 IO 的方法 → IO 会跑在本事务内", indirect));
            }
        }
        return violations;
    }

    /** 扫描全部方法（名 + 行号 + 方法体），用于定位"IO helper"与事务方法 */
    static List<Method> scanMethods(String stripped) {
        List<Method> result = new ArrayList<>();
        Matcher m = PAREN_THEN_BRACE.matcher(stripped);
        while (m.find()) {
            int brace = m.end() - 1;
            int closeParen = m.start();
            int openParen = matchBackwards(stripped, closeParen, '(', ')');
            if (openParen < 0) {
                continue;
            }
            int nameEnd = openParen - 1;
            while (nameEnd >= 0 && Character.isWhitespace(stripped.charAt(nameEnd))) {
                nameEnd--;
            }
            int nameStart = nameEnd;
            while (nameStart >= 0 && (Character.isJavaIdentifierPart(stripped.charAt(nameStart)))) {
                nameStart--;
            }
            if (nameStart == nameEnd) {
                continue;
            }
            String name = stripped.substring(nameStart + 1, nameEnd + 1);
            if (NON_METHOD_KEYWORDS.contains(name)) {
                continue;
            }
            int end = matchBraces(stripped, brace);
            if (end < 0) {
                continue;
            }
            result.add(new Method(name, lineOf(stripped, brace), brace, stripped.substring(brace, end + 1)));
        }
        return result;
    }

    /** 方法声明块内（从方法体前回溯到上一个 ; { } ）是否带 @Transactional */
    private static boolean isTransactional(String stripped, Method method) {
        int from = method.bodyStart();
        int i = from - 1;
        while (i >= 0) {
            char c = stripped.charAt(i);
            if (c == ';' || c == '{' || c == '}') {
                break;
            }
            i--;
        }
        String decl = stripped.substring(i + 1, from);
        return Pattern.compile("@Transactional\\b").matcher(decl).find();
    }

    private static String firstMarker(String body) {
        for (String marker : REMOTE_IO_MARKERS) {
            if (body.contains(marker)) {
                return marker;
            }
        }
        return null;
    }

    /** body 内是否出现 {@code name(} 形态的本地调用（{@code .name(} 的外部调用不算） */
    private static boolean callsMethod(String body, String name) {
        return Pattern.compile("(?<![\\w.])" + Pattern.quote(name) + "\\s*\\(").matcher(body).find();
    }

    static Partition partition(List<Violation> all, Map<String, String> whitelist) {
        List<String> unexplained = all.stream()
                .filter(v -> !whitelist.containsKey(v.key()))
                .map(Violation::describe)
                .toList();
        List<String> stale = whitelist.keySet().stream()
                .filter(k -> all.stream().noneMatch(v -> v.key().equals(k)))
                .sorted()
                .toList();
        return new Partition(unexplained, stale);
    }

    // ==================== 词法处理 ====================

    /**
     * 剥离注释与字符串内容（保留换行与字符偏移，便于报行号）
     * <p>注释剥离是硬需求：v13.14 的修复注释里就写了 {@code 不使用@Transactional} 与
     * {@code minioService.uploadKnowledgeFile}，不剥离会把"解释为什么不在事务里"的注释误判成违规。</p>
     */
    static String stripCommentsAndStrings(String src) {
        StringBuilder sb = new StringBuilder(src.length());
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            char c2 = i + 1 < n ? src.charAt(i + 1) : '\0';
            if (c == '/' && c2 == '/') {
                while (i < n && src.charAt(i) != '\n') {
                    sb.append(' ');
                    i++;
                }
            } else if (c == '/' && c2 == '*') {
                sb.append("  ");
                i += 2;
                while (i < n && !(src.charAt(i) == '*' && i + 1 < n && src.charAt(i + 1) == '/')) {
                    sb.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    sb.append("  ");
                    i += 2;
                }
            } else if (c == '"') {
                // 文本块 """..."""（Java 15+）：整体当作字符串处理，避免块内引号让词法状态错位
                boolean textBlock = c2 == '"' && i + 2 < n && src.charAt(i + 2) == '"';
                if (textBlock) {
                    sb.append("   ");
                    i += 3;
                    while (i < n && !(src.charAt(i) == '"' && i + 2 < n
                            && src.charAt(i + 1) == '"' && src.charAt(i + 2) == '"')) {
                        sb.append(src.charAt(i) == '\n' ? '\n' : ' ');
                        i++;
                    }
                    if (i < n) {
                        sb.append("   ");
                        i += 3;
                    }
                    continue;
                }
                sb.append(' ');
                i++;
                while (i < n && src.charAt(i) != '"') {
                    if (src.charAt(i) == '\\') {
                        sb.append("  ");
                        i += 2;
                    } else {
                        sb.append(src.charAt(i) == '\n' ? '\n' : ' ');
                        i++;
                    }
                }
                if (i < n) {
                    sb.append(' ');
                    i++;
                }
            } else if (c == '\'') {
                sb.append(' ');
                i++;
                while (i < n && src.charAt(i) != '\'') {
                    if (src.charAt(i) == '\\') {
                        sb.append("  ");
                        i += 2;
                    } else {
                        sb.append(' ');
                        i++;
                    }
                }
                if (i < n) {
                    sb.append(' ');
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int lineOf(String src, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /** 从 {@code closeIdx}（值为 {@code close}）向前匹配到配对的 {@code open} */
    private static int matchBackwards(String src, int closeIdx, char open, char close) {
        int depth = 0;
        for (int i = closeIdx; i >= 0; i--) {
            char c = src.charAt(i);
            if (c == close) {
                depth++;
            } else if (c == open) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** 从 {@code { 起始处向后匹配到配对的 } */
    private static int matchBraces(String src, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** 已定位的方法：名、声明行号、方法体起始偏移（指向 <code>{</code>）、方法体文本（含花括号） */
    record Method(String name, int line, int bodyStart, String body) {
    }
}
