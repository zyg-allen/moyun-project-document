package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：**用户聚合表的增量写必须校验影响行数**（不得裸调用、不得赋值后不校验）
 *
 * <h3>为什么需要这条守卫</h3>
 * <p>用户聚合行（成长值 / 积分 / 统计计数）是**懒创建**的：先
 * {@code INSERT IGNORE INTO portal_user_growth|portal_user_stats (...) VALUES (userId)}，
 * 再 {@code UPDATE ... SET col = col + ? WHERE user_id = ?}。</p>
 * <p>危险组合：{@code INSERT IGNORE} 对**非重复键原因**的失败是**静默的**（MySQL 只发 warning），
 * 此时随后的增量 UPDATE 命中 0 行——若不校验返回值，就出现：</p>
 * <ul>
 *   <li><b>扣了没入账</b>：{@code PortalTipServiceImpl} 先原子扣打赏者积分，再给作者 {@code addPoints}；
 *       作者侧 0 行 → 积分凭空消失（而打赏订单已置 PAID）；</li>
 *   <li><b>流水写了、成长值没加</b>：{@code PortalGrowthServiceImpl.recordEventWithTarget} 先插成长流水再 {@code addGrowth}；</li>
 *   <li><b>计数永久偏差且无日志</b>：关注/粉丝数、创作字数、精选笔记数。</li>
 * </ul>
 * <p>本批（v13.16）已把这些点全部改为"校验影响行数，0 行即 fail-closed 抛错（由所在事务回滚）"，
 * 并顺手修掉了 {@code note_adopted}（精选笔记数）的三处写入冲突：它原是
 * {@code updateStats} 按**成长值增量**写入 + 控制器再显式 +1 → 重复采纳持续累加、
 * 取消精选只 −1（单调虚高）。现唯一写入源 = {@code IPortalGrowthService#updateNoteAdoptedCount(userId, ±1)}。</p>
 *
 * <h3>规则</h3>
 * <ol>
 *   <li>对**聚合表 mapper**（{@code PortalUserStatsMapper} / {@code PortalUserGrowthMapper} /
 *       {@code PortalUserBadgeMapper}）的增量写方法（mapper 接口里返回 {@code int} 且名字匹配
 *       {@code increment*|add*|deduct*}）：
 *       <ul>
 *         <li>**裸调用**（整条语句就是调用）→ 违规；</li>
 *         <li>赋值后**必须被校验**：同方法内该变量参与比较（{@code ==/!=/</>/<=/>=}）
 *             或作为 {@code require*}/{@code assert*} 辅助方法的入参；否则违规；</li>
 *         <li>作为 {@code require*}/{@code assert*} 的实参直接包裹 → 合规。</li>
 *       </ul></li>
 *   <li><b>不做范围扩张</b>：内容行上的计数（文章/话题/评论的浏览、点赞数，接收者 var 不是聚合表 mapper）
 *       不在本规则内——那些行由同请求内的内容记录保证存在，0 行=并发删除，属可忽略的最佳努力计数。</li>
 * </ol>
 *
 * <p>复用 {@link TransactionRemoteIoGuardTest} 的注释剥离与方法体解析（同一套词法，避免两套解析器结论不一致）。</p>
 *
 * @author moyun
 */
class AggregateIncrementGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    /** 懒创建聚合行的三个 mapper（简单类名） */
    private static final Set<String> AGGREGATE_MAPPERS = Set.of(
            "PortalUserStatsMapper", "PortalUserGrowthMapper", "PortalUserBadgeMapper");

    /** mapper 接口里"增量写且返回 int"的方法声明 */
    private static final Pattern INCREMENT_DECL = Pattern.compile(
            "(?:^|\\s)(?:int|Integer|long|Long)\\s+((?:increment|add|deduct|incr|increase)[A-Z]\\w*)\\s*\\(");

    /** 字段声明：private XxxMapper varName;（含 @Autowired 单行写法） */
    private static final Pattern MAPPER_FIELD = Pattern.compile(
            "(?:private|protected|public)\\s+(\\w*Mapper)\\s+(\\w+)\\s*;");

    /** ServiceImpl<XxxMapper, T> → baseMapper 的静态类型 */
    private static final Pattern SERVICE_IMPL = Pattern.compile("extends\\s+ServiceImpl<\\s*(\\w*Mapper)\\s*,");

    /** 白名单：key = {@code 相对路径:行号:方法名}，value = 理由（当前为空） */
    private static final Map<String, String> WHITELIST = Map.of();

    /** 正向控制：全仓聚合表增量写调用点（合规+违规）不得少于该数 */
    private static final int MIN_INCREMENT_CALL_SITES = 8;

    @Test
    @DisplayName("全量源码：聚合表增量写必须校验影响行数（0 行 = 静默丢积分/成长值/计数）")
    void aggregateIncrementsMustBeChecked() throws IOException {
        Map<String, Set<String>> mapperIncrements = collectAggregateIncrementMethods();
        List<String> violations = new ArrayList<>();
        int files = 0;
        int callSites = 0;

        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String relative = SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
                if (AGGREGATE_MAPPERS.stream().anyMatch(m -> relative.endsWith(m + ".java"))) {
                    continue; // mapper 接口本身
                }
                String source = Files.readString(file, StandardCharsets.UTF_8);
                files++;
                ScanOutcome outcome = scan(source, relative, mapperIncrements);
                callSites += outcome.callSites();
                violations.addAll(outcome.violations());
            }
        }

        violations.sort(Comparator.naturalOrder());
        // 白名单 key 形如 "path/File.java:123"（违规串以该前缀开头）
        List<String> unexplained = violations.stream()
                .filter(v -> WHITELIST.keySet().stream().noneMatch(v::startsWith))
                .toList();

        System.out.println("[AggregateIncrementGuard] 扫描文件=" + files
                + "，聚合表增量写调用点=" + callSites
                + "，违规=" + violations.size());
        assertTrue(files > 100, "扫描器未覆盖到源码（文件数=" + files + "）");
        assertTrue(callSites >= MIN_INCREMENT_CALL_SITES,
                "识别到的聚合表增量写调用点过少（" + callSites + " < " + MIN_INCREMENT_CALL_SITES
                        + "）——解析器可能失效，守卫会假绿");
        assertTrue(unexplained.isEmpty(),
                "聚合表增量写未校验影响行数（0 行 = 静默丢积分/成长值/计数）：\n  "
                        + String.join("\n  ", unexplained));

        List<String> stale = WHITELIST.keySet().stream()
                .filter(k -> violations.stream().noneMatch(k::equals))
                .sorted()
                .toList();
        assertTrue(stale.isEmpty(), "白名单存在已失效条目，请删除：\n  " + String.join("\n  ", stale));
    }

    @Test
    @DisplayName("扫描器自检：裸调用/赋值不校验必须命中，校验写法与内容计数不得误报")
    void scannerSelfCheck() {
        Map<String, Set<String>> increments = collectAggregateIncrementMethods();
        assertTrue(increments.getOrDefault("PortalUserStatsMapper", Set.of()).contains("addNoteAdopted"),
                "必须能从 mapper 接口解析出增量写方法名");

        // 1. 裸调用 → 违规
        assertEquals(1, scan("""
                class A {
                    private PortalUserStatsMapper statsMapper;
                    public void run(Long userId) {
                        statsMapper.addNoteAdopted(userId, 1);
                    }
                }
                """, "A.java", increments).violations().size(), "裸调增量写必须命中");

        // 2. 赋值 + 比较校验 → 合规
        assertEquals(0, scan("""
                class B {
                    private PortalUserGrowthMapper growthMapper;
                    public void run(Long userId, int delta) {
                        int rows = growthMapper.addGrowth(userId, delta);
                        if (rows == 0) { throw new ServiceException("x"); }
                    }
                }
                """, "B.java", increments).violations().size(), "赋值后校验应合规");

        // 3. 作为 require* 实参包裹 → 合规
        assertEquals(0, scan("""
                class C {
                    private PortalUserStatsMapper statsMapper;
                    public void run(Long userId, int delta) {
                        requireStatsUpdated(statsMapper.addArticleWordSum(userId, delta), "addArticleWordSum");
                    }
                    private void requireStatsUpdated(int rows, String scene) { }
                }
                """, "C.java", increments).violations().size(), "require* 包裹应合规");

        // 4. 赋值后不校验 → 违规（返回值被丢弃在日志里不算校验）
        assertEquals(1, scan("""
                class D {
                    private PortalUserGrowthMapper growthMapper;
                    public void run(Long userId, int delta) {
                        int rows = growthMapper.addPoints(userId, delta);
                        log.info("credited {}", rows);
                    }
                }
                """, "D.java", increments).violations().size(), "赋值但不校验必须命中");

        // 5. 内容行计数（接收者不是聚合表 mapper）→ 不报
        assertEquals(0, scan("""
                class E {
                    private PortalArticleMapper articleMapper;
                    public void run(Long id) { articleMapper.incrementViews(id, 1); }
                }
                """, "E.java", increments).violations().size(), "内容行计数不在本规则范围");

        // 6. 集合 addAll / 配置对象 addXxx → 不报
        assertEquals(0, scan("""
                class F {
                    private PortalUserStatsMapper statsMapper;
                    public void run(List<String> a, List<String> b) {
                        a.addAll(b);
                        config.addAllowedOriginPattern("*");
                    }
                }
                """, "F.java", increments).violations().size(), "非 mapper 的 addXxx 不得误报");

        // 7. baseMapper（ServiceImpl 泛型）也要能解析出所属 mapper；insertIfNotExists 不是增量写 → 不报
        assertEquals(0, scan("""
                class G extends ServiceImpl<PortalUserBadgeMapper, PortalUserBadge> {
                    public void run(Long userId, Long achievementId) {
                        baseMapper.insertIfNotExists(userId, achievementId);
                    }
                }
                """, "G.java", increments).violations().size(), "insertIfNotExists 不是增量写");
    }

    // ==================== 扫描实现 ====================

    /** 单文件扫描结果 */
    record ScanOutcome(List<String> violations, int callSites) {
    }

    /** 解析三个聚合 mapper 接口里"返回 int 的增量写方法" */
    static Map<String, Set<String>> collectAggregateIncrementMethods() {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                if (!AGGREGATE_MAPPERS.contains(name)) {
                    continue;
                }
                String stripped = TransactionRemoteIoGuardTest.stripCommentsAndStrings(
                        Files.readString(file, StandardCharsets.UTF_8));
                Set<String> methods = new LinkedHashSet<>();
                Matcher matcher = INCREMENT_DECL.matcher(stripped);
                while (matcher.find()) {
                    methods.add(matcher.group(1));
                }
                result.put(name, methods);
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取 mapper 接口失败", e);
        }
        return result;
    }

    /** 扫描单个源文件 */
    static ScanOutcome scan(String source, String fileName, Map<String, Set<String>> mapperIncrements) {
        String stripped = TransactionRemoteIoGuardTest.stripCommentsAndStrings(source);
        String[] lines = stripped.split("\\R", -1);

        // 文件内的 mapper 变量 → mapper 简单类名
        Map<String, String> receivers = new HashMap<>();
        Matcher field = MAPPER_FIELD.matcher(stripped);
        while (field.find()) {
            receivers.put(field.group(2), field.group(1));
        }
        Matcher serviceImpl = SERVICE_IMPL.matcher(stripped);
        if (serviceImpl.find()) {
            receivers.put("baseMapper", serviceImpl.group(1));
        }
        if (receivers.isEmpty()) {
            return new ScanOutcome(List.of(), 0);
        }

        Pattern call = Pattern.compile("\\b(" + String.join("|", receivers.keySet())
                + ")\\.(\\w+)\\s*\\(");
        List<String> violations = new ArrayList<>();
        int callSites = 0;

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = call.matcher(lines[i]);
            while (matcher.find()) {
                String receiver = matcher.group(1);
                String method = matcher.group(2);
                String mapper = receivers.get(receiver);
                if (!AGGREGATE_MAPPERS.contains(mapper)
                        || !mapperIncrements.getOrDefault(mapper, Set.of()).contains(method)) {
                    continue;
                }
                callSites++;
                String trimmed = lines[i].trim();
                String prefix = lines[i].substring(0, matcher.start()).trim();
                if (prefix.isEmpty() && trimmed.endsWith(";")) {
                    violations.add(fileName + ":" + (i + 1) + "  " + trimmed
                            + "   ← 裸调增量写：影响行数被丢弃，0 行时静默丢数据");
                    continue;
                }
                if (prefix.contains("require") || prefix.contains("assert")) {
                    continue; // 由 require* / assert* 包裹校验
                }
                Matcher assigned = Pattern.compile("(\\w+)\\s*=\\s*" + Pattern.quote(receiver)
                        + "\\." + Pattern.quote(method) + "\\s*\\(").matcher(lines[i]);
                if (assigned.find()) {
                    String var = assigned.group(1);
                    String enclosing = enclosingBody(lines, i);
                    if (!isChecked(enclosing, var)) {
                        violations.add(fileName + ":" + (i + 1) + "  " + trimmed
                                + "   ← 赋值后未校验 " + var + "：0 行时静默丢数据");
                    }
                }
            }
        }
        return new ScanOutcome(violations, callSites);
    }

    /** 变量是否被校验：比较运算，或作为 require* / assert* 的实参 */
    private static boolean isChecked(String body, String var) {
        if (Pattern.compile("\\b" + Pattern.quote(var) + "\\b\\s*(==|!=|<=|>=|<|>)").matcher(body).find()) {
            return true;
        }
        return Pattern.compile("(?:require|assert)\\w*\\s*\\([^;)]*\\b" + Pattern.quote(var) + "\\b")
                .matcher(body).find();
    }

    /** 取该行所在方法的文本（找不到时退化为整文件，宁可少报不误报） */
    private static String enclosingBody(String[] lines, int lineIndex) {
        int start = lineIndex;
        while (start > 0 && !lines[start - 1].trim().endsWith("{") && !lines[start - 1].contains("{")) {
            start--;
        }
        int end = lineIndex;
        while (end < lines.length - 1 && !lines[end + 1].trim().startsWith("}")) {
            end++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end && i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString();
    }
}
