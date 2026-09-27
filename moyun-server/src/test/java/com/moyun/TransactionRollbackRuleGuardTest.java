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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：{@code @Transactional} 必须显式声明**回滚口径**，且注解不能被"静默失效"的写法吞掉
 *
 * <h3>为什么需要这条守卫</h3>
 * <p>Spring 的默认回滚规则是 <b>只对 {@code RuntimeException} / {@code Error} 回滚</b>。
 * 一个多步写方法若声明了受检异常（或内部抛出了受检异常），事务会**照常提交前半段写入**——
 * 不报错、不回滚，留下"半截数据"。真实案例（本批修复的 10 处）：</p>
 * <ul>
 *   <li>{@code AiSceneConfigVersionService.createWithSnapshot/updateWithSnapshot/rollback}：
 *       配置 + 版本快照两步写；</li>
 *   <li>{@code KnowledgeConfigServiceImpl.applyConfiguration/createDefaultConfig/updateConfig}：知识库配置多表写；</li>
 *   <li>{@code ConversationServiceImpl.addMessage/deleteConversation}、{@code ToolServiceImpl.bindToolsToAgent}
 *       （先删后插的绑定关系）、{@code ModelConfigServiceImpl.setDefault}（先清默认再置默认）。</li>
 * </ul>
 * <p>报告 §6.2 原文写"约 205 处缺 {@code rollbackFor}"，本批实测为
 * <b>211 个方法级 {@code @Transactional}，其中 201 已带、仅 10 处缺</b>——按语义单位复核过一次，
 * 与附录 O-1/W-1 的"计数订正"同族。</p>
 *
 * <h3>本守卫的四条规则</h3>
 * <ol>
 *   <li><b>必须声明回滚口径</b>：方法级 {@code @Transactional} 必须带 {@code rollbackFor}/{@code rollbackForClassName}，
 *       或显式 {@code readOnly = true}（真正只读的查询可以用后者）；</li>
 *   <li><b>禁止 {@code noRollbackFor}</b>：那等于主动关掉回滚；如需例外必须写进白名单说明理由；</li>
 *   <li><b>注解不得标在"代理看不见"的方法上</b>：{@code private}/{@code protected}/{@code static}/方法级
 *       {@code final} 上的 {@code @Transactional} 不生效（CGLIB/JDK 代理都拦不到），
 *       写了会让人误以为有事务——与 {@code AsyncSelfInvocationGuardTest} 同一类"静默失效"；</li>
 *   <li><b>禁止类级 {@code @Transactional}</b>：类级注解会把只读方法也拖进写事务，
 *       且与"事务边界只包 DB 写"的收窄口径冲突，必须逐方法显式声明。</li>
 * </ol>
 *
 * <p><b>复用 {@link TransactionRemoteIoGuardTest} 的词法与解析器</b>（同一套注释剥离 + 方法体定位），
 * 避免两套解析器对同一份源码给出不同结论。</p>
 *
 * @author moyun
 */
class TransactionRollbackRuleGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    /** 方法级 {@code @Transactional}：{@code \b} 保证不会把 {@code @TransactionalEventListener} 误算进来 */
    private static final Pattern TX_ANNOTATION = Pattern.compile("@Transactional\\b");

    /**
     * 白名单：key = {@code 相对路径#方法名}，value = 理由。
     * <p>当前为空——本批已把全仓 10 处缺口补齐，且不存在 {@code noRollbackFor} / 代理不可见注解 / 类级注解。</p>
     */
    private static final Map<String, String> WHITELIST = Map.of();

    /** 正向控制阈值：识别到的方法级 {@code @Transactional} 数不得低于此值（防解析器失效导致假绿） */
    private static final int MIN_TRANSACTIONAL_METHODS = 150;

    @Test
    @DisplayName("全量源码：每个 @Transactional 都声明回滚口径，且不出现代理不可见/类级/noRollbackFor 写法")
    void everyTransactionalDeclaresRollbackRule() throws IOException {
        List<Violation> violations = new ArrayList<>();
        int files = 0;
        int transactionalMethods = 0;
        int readOnlyMethods = 0;

        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                files++;
                String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
                ScanResult result = scan(source, relative);
                transactionalMethods += result.transactionalMethods();
                readOnlyMethods += result.readOnlyMethods();
                violations.addAll(result.violations().stream()
                        .filter(v -> !WHITELIST.containsKey(v.key()))
                        .toList());
            }
        }

        violations.sort(Comparator.comparing(Violation::toString));
        System.out.println("[TransactionRollbackRuleGuard] 扫描文件=" + files
                + "，方法级 @Transactional=" + transactionalMethods
                + "（其中 readOnly=" + readOnlyMethods + "），违规=" + violations.size());

        assertTrue(files > 100, "扫描器未覆盖到源码（文件数=" + files + "）");
        assertTrue(transactionalMethods >= MIN_TRANSACTIONAL_METHODS,
                "识别到的事务方法数过低（" + transactionalMethods + " < " + MIN_TRANSACTIONAL_METHODS
                        + "）——解析器可能已失效，守卫会假绿");
        assertTrue(violations.isEmpty(),
                "事务回滚口径缺陷（Spring 默认只对 RuntimeException/Error 回滚，受检异常会提交半截数据）：\n  "
                        + String.join("\n  ", violations.stream().map(Violation::toString).toList()));

        List<String> stale = WHITELIST.keySet().stream()
                .filter(k -> violations.stream().noneMatch(v -> v.key().equals(k)))
                .sorted()
                .toList();
        assertTrue(stale.isEmpty(), "白名单存在已失效条目，请删除：\n  " + String.join("\n  ", stale));
    }

    @Test
    @DisplayName("扫描器自检：缺回滚口径/私有方法/noRollbackFor/类级注解必须命中，正确写法不得误报")
    void scannerSelfCheck() {
        // 1. 缺 rollbackFor（本批修复前的真实形态）
        assertEquals(1, scan("""
                class A {
                    @Transactional
                    public void save(Entity e) { mapper.insert(e); }
                }
                """, "A.java").violations().size(), "缺回滚口径必须命中");

        // 2. 带 rollbackFor：不得误报
        assertEquals(0, scan("""
                class B {
                    @Transactional(rollbackFor = Exception.class)
                    public void save(Entity e) { mapper.insert(e); }
                }
                """, "B.java").violations().size(), "显式 rollbackFor 是正确写法");

        // 3. readOnly = true 视作已声明口径（只读事务无需回滚规则）
        assertEquals(0, scan("""
                class C {
                    @Transactional(readOnly = true)
                    public List<Entity> list() { return mapper.selectList(null); }
                }
                """, "C.java").violations().size(), "readOnly=true 可替代 rollbackFor");

        // 4. 其它注解属性形态（propagation）也要能被识别为"未声明口径"
        assertEquals(1, scan("""
                class D {
                    @Transactional(propagation = Propagation.REQUIRES_NEW)
                    public void save(Entity e) { mapper.insert(e); }
                }
                """, "D.java").violations().size(),
                "带其它属性的注解若缺 rollbackFor，同样必须命中");

        // 5. noRollbackFor 一律命中
        assertEquals(1, scan("""
                class E {
                    @Transactional(rollbackFor = Exception.class, noRollbackFor = ServiceException.class)
                    public void save(Entity e) { mapper.insert(e); }
                }
                """, "E.java").violations().size(), "noRollbackFor 等于主动关掉回滚，必须命中");

        // 6. 代理不可见的方法：private / static / final
        assertEquals(1, scan("""
                class F {
                    @Transactional(rollbackFor = Exception.class)
                    private void save(Entity e) { mapper.insert(e); }
                }
                """, "F.java").violations().size(), "private 方法上的 @Transactional 不生效，必须命中");
        assertEquals(1, scan("""
                class G {
                    @Transactional(rollbackFor = Exception.class)
                    static void save(Entity e) { mapper.insert(e); }
                }
                """, "G.java").violations().size(), "static 方法上的 @Transactional 不生效，必须命中");
        assertEquals(1, scan("""
                class H {
                    @Transactional(rollbackFor = Exception.class)
                    public final void save(Entity e) { mapper.insert(e); }
                }
                """, "H.java").violations().size(), "final 方法 CGLIB 无法代理，必须命中");

        // 6b. 例外：final 只出现在参数上时不得误报
        assertEquals(0, scan("""
                class H2 {
                    @Transactional(rollbackFor = Exception.class)
                    public void save(final Entity e) { mapper.insert(e); }
                }
                """, "H2.java").violations().size(), "参数上的 final 不是方法修饰符，不得误报");

        // 7. 类级注解
        assertEquals(1, scan("""
                @Transactional
                class I {
                    public void save(Entity e) { mapper.insert(e); }
                }
                """, "I.java").violations().size(), "类级 @Transactional 会把只读方法也拖进写事务，必须命中");

        // 8. @TransactionalEventListener 是另一个注解，不得误报
        assertEquals(0, scan("""
                class J {
                    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
                    public void onEvent(Event e) { log.info("x"); }
                }
                """, "J.java").violations().size(), "TransactionalEventListener 不是 @Transactional");

        // 9. 注释里提到的 @Transactional 不得误报（v13.14 就踩过这个假阳性）
        assertEquals(0, scan("""
                class K {
                    /**
                     * 本方法不再整体 @Transactional —— 远程 IO 已移出事务。
                     */
                    public void save(Entity e) { transactionTemplate.executeWithoutResult(s -> mapper.insert(e)); }
                }
                """, "K.java").violations().size(), "注释内的注解名不算声明");
    }

    @Test
    @DisplayName("白名单机制：未豁免必报、已豁免放行、失效条目暴露")
    void whitelistBehaviour() {
        List<Violation> all = scan("""
                class L {
                    @Transactional
                    public void save(Entity e) { mapper.insert(e); }
                }
                """, "L.java").violations();
        assertEquals(1, all.size());
        assertEquals(List.of(), partition(all, Map.of(all.get(0).key(), "理由")).unexplained());
        assertEquals(List.of("L.java#save"), partition(all, Map.of()).unexplainedKeys());
        assertEquals(List.of("M.java#gone"), partition(List.of(), Map.of("M.java#gone", "理由")).stale());
    }

    // ==================== 扫描实现 ====================

    /** 违规项 */
    record Violation(String file, int line, String method, String reason) {
        String key() {
            return file + "#" + method;
        }

        @Override
        public String toString() {
            return file + ":" + line + "  " + method + "()  " + reason;
        }
    }

    /** 分区结果 */
    record Partition(List<String> unexplained, List<String> stale) {
        List<String> unexplainedKeys() {
            return unexplained;
        }
    }

    /** 单文件扫描结果 */
    record ScanResult(List<Violation> violations, int transactionalMethods, int readOnlyMethods) {
    }

    static Partition partition(List<Violation> all, Map<String, String> whitelist) {
        List<String> unexplained = all.stream()
                .filter(v -> !whitelist.containsKey(v.key()))
                .map(Violation::key)
                .toList();
        List<String> stale = whitelist.keySet().stream()
                .filter(k -> all.stream().noneMatch(v -> v.key().equals(k)))
                .sorted()
                .toList();
        return new Partition(unexplained, stale);
    }

    /** 扫描单个源文件的文本（测试与主扫描共用） */
    static List<Violation> scanText(String source, String fileName) {
        return scan(source, fileName).violations();
    }

    private static ScanResult scan(String source, String fileName) {
        String stripped = TransactionRemoteIoGuardTest.stripCommentsAndStrings(source);
        List<TransactionRemoteIoGuardTest.Method> methods = TransactionRemoteIoGuardTest.scanMethods(stripped);
        List<Violation> violations = new ArrayList<>();
        int transactional = 0;
        int readOnly = 0;

        // 类级 @Transactional：注解后直接声明类型
        Matcher classLevel = Pattern.compile(
                "@Transactional\\b(?:\\([^()]*\\))?\\s*(?:@\\w+(?:\\([^()]*\\))?\\s*)*"
                        + "(?:public|final|abstract|static|\\s)*\\b(?:class|interface|enum|record)\\b")
                .matcher(stripped);
        while (classLevel.find()) {
            violations.add(new Violation(fileName, lineOf(stripped, classLevel.start()), "<class>",
                    "类级 @Transactional：会把只读方法也拖进写事务，须逐方法显式声明"));
        }

        for (TransactionRemoteIoGuardTest.Method method : methods) {
            String decl = declarationBefore(stripped, method.bodyStart());
            Matcher matcher = TX_ANNOTATION.matcher(decl);
            if (!matcher.find()) {
                continue;
            }
            transactional++;
            String annotation = annotationText(decl, matcher.start());
            boolean hasRollbackRule = annotation.contains("rollbackFor");
            boolean isReadOnly = annotation.matches("(?s).*readOnly\\s*=\\s*true.*");
            if (isReadOnly) {
                readOnly++;
            }
            if (!hasRollbackRule && !isReadOnly) {
                violations.add(new Violation(fileName, method.line(), method.name(),
                        "@Transactional 未声明回滚口径：Spring 默认只对 RuntimeException/Error 回滚，"
                                + "受检异常会提交半截数据 → 补 rollbackFor = Exception.class"));
            }
            if (annotation.contains("noRollbackFor")) {
                violations.add(new Violation(fileName, method.line(), method.name(),
                        "@Transactional 声明了 noRollbackFor：等于主动放弃回滚，需白名单说明理由"));
            }
            String signature = stripAnnotations(decl);
            int signatureParen = signature.indexOf('(');
            String modifiers = signatureParen < 0 ? signature : signature.substring(0, signatureParen);
            if (Pattern.compile("\\b(private|protected|static)\\b").matcher(modifiers).find()) {
                violations.add(new Violation(fileName, method.line(), method.name(),
                        "注解标在 private/protected/static 方法上：代理拦不到，事务不会生效（静默失效）"));
            } else if (Pattern.compile("\\bfinal\\b").matcher(modifiers).find()) {
                violations.add(new Violation(fileName, method.line(), method.name(),
                        "注解标在 final 方法上：CGLIB 无法代理，事务不会生效（静默失效）"));
            }
        }
        return new ScanResult(violations, transactional, readOnly);
    }

    /** 方法体之前、上一个 {@code ; { }} 之后的声明文本（含注解行） */
    private static String declarationBefore(String stripped, int bodyStart) {
        int i = bodyStart - 1;
        while (i >= 0) {
            char c = stripped.charAt(i);
            if (c == ';' || c == '{' || c == '}') {
                break;
            }
            i--;
        }
        return stripped.substring(i + 1, bodyStart);
    }

    /**
     * 去掉声明文本里的注解（含注解自身的括号参数），只留修饰符/返回类型/方法名/参数表
     * <p>必须去注解后再找"方法自己的 {@code (}"——否则会命中注解参数的括号，
     * 把 {@code private}/{@code static}/{@code final} 这些在注解**之后**的修饰符漏掉。</p>
     */
    private static String stripAnnotations(String decl) {
        StringBuilder sb = new StringBuilder(decl.length());
        int i = 0;
        while (i < decl.length()) {
            char c = decl.charAt(i);
            if (c == '@') {
                i++;
                while (i < decl.length() && Character.isJavaIdentifierPart(decl.charAt(i))) {
                    i++;
                }
                while (i < decl.length() && Character.isWhitespace(decl.charAt(i))) {
                    i++;
                }
                if (i < decl.length() && decl.charAt(i) == '(') {
                    int depth = 0;
                    while (i < decl.length()) {
                        char ch = decl.charAt(i);
                        if (ch == '(') {
                            depth++;
                        } else if (ch == ')') {
                            depth--;
                            if (depth == 0) {
                                i++;
                                break;
                            }
                        }
                        i++;
                    }
                }
                sb.append(' ');
                continue;
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /** 取出该注解自身的完整文本（含括号参数），用于判定属性 */
    private static String annotationText(String decl, int annotationStart) {
        int open = decl.indexOf('(', annotationStart);
        if (open < 0) {
            return decl.substring(annotationStart);
        }
        int depth = 0;
        for (int i = open; i < decl.length(); i++) {
            char c = decl.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return decl.substring(annotationStart, i + 1);
                }
            }
        }
        return decl.substring(annotationStart);
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
}
