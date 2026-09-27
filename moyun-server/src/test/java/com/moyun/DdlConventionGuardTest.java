package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：DDL 约定（报告六 §6.5「数据库设计」的可执行门禁）
 *
 * <h3>守三件事</h3>
 * <ol>
 *   <li><b>金额列精度</b>：名字像金额的 {@code decimal} 列必须是 {@code decimal(18,2)}（元）。
 *       白名单：AI 计费列按 **token 单价**计价、需要 6 位小数（{@code cost} / {@code cost_yuan} /
 *       {@code input_price} / {@code output_price} / {@code total_cost}）。</li>
 *   <li><b>逻辑删除列名</b>：每表最多一个软删列且必须叫 {@code del_flag}（0=存在 / 2=删除，
 *       配 {@code BaseEntity} + 全局 {@code logic-delete-field=delFlag}）。
 *       白名单：AI 模块 11 张表用 {@code deleted}（{@code AiBaseEntity} 是**有意设计**：
 *       AI 表无 {@code create_by/remark/del_flag} 列，模块内统一 + {@code @TableLogic} 显式声明）。
 *       （话题 2 张表的 {@code is_deleted} 已于 v13.13 迁移完毕，登记随之移除——见附录 W。）</li>
 *   <li><b>collation 单一</b>：除 {@code ledger_ai_analysis_report}（{@code utf8mb4_bin}，
 *       数据指纹/JSON 快照需精确匹配，建表语句即声明）外，一律 {@code utf8mb4_0900_ai_ci}。</li>
 * </ol>
 *
 * <p>白名单同样受"清单卫生"约束：每条必须有理由，且**不得过期**（登记的表/列必须仍然存在、
 * 仍然处于该偏差状态）——防止"改完了但豁免没删"。</p>
 *
 * @author moyun
 * @see ModuleDependencyGuardTest 同族守卫（结构约束靠测试而非靠人记）
 */
class DdlConventionGuardTest {

    private static final Path DDL = Path.of("src/main/resources/init-sql/moyun-db-ddl.sql");

    /** 金额列名关键词（命中且类型为 decimal 时，精度必须是 18,2） */
    private static final String MONEY_KEYWORDS =
            "amount|balance|price|fee|income|expense|cost|money|salary|budget|principal|interest|total";

    /** 软删列候选（每表最多一个） */
    private static final List<String> LOGIC_DELETE_COLUMNS = List.of("del_flag", "deleted", "is_deleted");

    private static final Pattern CREATE_TABLE =
            Pattern.compile("(?is)CREATE TABLE[^(]*`([^`]+)`\\s*\\((.*?)\\n\\)\\s*ENGINE");

    /** 列定义行：`col` type ... */
    private static final Pattern COLUMN_DEF =
            Pattern.compile("(?im)^\\s*`(\\w+)`\\s+([a-zA-Z]+(?:\\(\\s*\\d+\\s*(?:,\\s*\\d+\\s*)?\\))?)");

    private static final Pattern COLLATE = Pattern.compile("(?i)COLLATE\\s+(utf8mb4_\\w+)");

    /** 允许的非 (18,2) 金额列 → 理由 */
    private static final Map<String, String> MONEY_ALLOWLIST = new LinkedHashMap<>();

    /** 允许的非 del_flag 软删列：表名 → [列名, 理由] */
    private static final Map<String, String[]> LOGIC_DELETE_ALLOWLIST = new LinkedHashMap<>();

    /** 允许的非 0900_ai_ci collation：表名 → [collation, 理由] */
    private static final Map<String, String[]> COLLATION_ALLOWLIST = new LinkedHashMap<>();

    static {
        String aiBilling = "AI 计费列：按 token 单价计价，需 6 位小数（元/千token），宽化到 2 位会丢精度";
        MONEY_ALLOWLIST.put("ai_execute_log.cost_yuan", aiBilling);
        MONEY_ALLOWLIST.put("ai_model_config.input_price", aiBilling);
        MONEY_ALLOWLIST.put("ai_model_config.output_price", aiBilling);
        MONEY_ALLOWLIST.put("ai_token_usage_summary.total_cost", aiBilling);
        MONEY_ALLOWLIST.put("ai_token_usage_log.cost", aiBilling);

        String aiSoftDelete = "AiBaseEntity 有意设计：AI 表无 create_by/remark/del_flag 列，"
                + "模块内统一 deleted(0/1) + @TableLogic 显式声明覆盖全局配置";
        for (String table : List.of("ai_agent", "ai_agent_tool", "ai_conversation", "ai_datasource_config",
                "ai_domain_dictionary", "ai_knowledge_base", "ai_knowledge_library", "ai_model_config",
                "ai_provider", "ai_scene_config", "ai_workflow")) {
            LOGIC_DELETE_ALLOWLIST.put(table, new String[]{"deleted", aiSoftDelete});
        }
        // 话题 2 张表（portal_topic_post / portal_topic_comment）的 is_deleted 已于 v13.13 迁移为 del_flag，
        // 登记随之移除——这正是"债务清单"应有的闭环：改完即删登记，守卫会验证它确实不再偏差。

        COLLATION_ALLOWLIST.put("ledger_ai_analysis_report", new String[]{"utf8mb4_bin",
                "AI 财务分析月度快照：data_fingerprint 数据指纹与 JSON 需精确（区分大小写）匹配，建表语句即声明 COLLATE=utf8mb4_bin"});
    }

    @Test
    @DisplayName("金额列：名字像金额的 decimal 列必须是 decimal(18,2)（AI 计费列按理由白名单）")
    void moneyColumnsUseDecimal18_2() throws IOException {
        Map<String, List<ColumnDef>> tables = parseTables(readDdl());
        List<String> violations = new ArrayList<>();
        Set<String> seenAllowlisted = new LinkedHashSet<>();

        for (Map.Entry<String, List<ColumnDef>> table : tables.entrySet()) {
            for (ColumnDef column : table.getValue()) {
                if (!"decimal".equalsIgnoreCase(column.type())) {
                    continue;
                }
                if (!column.name().matches(".*(" + MONEY_KEYWORDS + ").*")) {
                    continue;
                }
                String key = table.getKey() + "." + column.name();
                if (column.precision() == 18 && column.scale() == 2) {
                    continue;
                }
                if (MONEY_ALLOWLIST.containsKey(key)) {
                    seenAllowlisted.add(key);
                    continue;
                }
                violations.add(key + " 是 decimal(" + column.precision() + "," + column.scale() + ")");
            }
        }
        assertTrue(violations.isEmpty(),
                "金额列必须统一 decimal(18,2)（元）；如确需其它精度（如按 token 计价的单价列），"
                        + "请加入 MONEY_ALLOWLIST 并写明理由：\n  " + String.join("\n  ", violations));

        // 清单卫生：白名单条目必须仍然存在且仍然是非 (18,2)
        List<String> stale = new ArrayList<>();
        for (String key : MONEY_ALLOWLIST.keySet()) {
            if (!seenAllowlisted.contains(key)) {
                stale.add(key + "（已不再是「非 18,2 金额列」或已不存在）");
            }
        }
        assertTrue(stale.isEmpty(), "金额白名单已过期，请删除登记：\n  " + String.join("\n  ", stale));
    }

    @Test
    @DisplayName("逻辑删除：每表最多一个软删列且必须叫 del_flag（AI/话题按理由白名单）")
    void logicDeleteColumnMustBeDelFlag() throws IOException {
        Map<String, List<ColumnDef>> tables = parseTables(readDdl());
        List<String> violations = new ArrayList<>();
        Set<String> seenAllowlisted = new LinkedHashSet<>();

        for (Map.Entry<String, List<ColumnDef>> table : tables.entrySet()) {
            List<String> found = new ArrayList<>();
            for (ColumnDef column : table.getValue()) {
                if (LOGIC_DELETE_COLUMNS.contains(column.name())) {
                    found.add(column.name());
                }
            }
            if (found.isEmpty()) {
                continue;
            }
            if (found.size() > 1) {
                violations.add(table.getKey() + " 同时存在多个软删列：" + found);
                continue;
            }
            String column = found.get(0);
            if ("del_flag".equals(column)) {
                continue;
            }
            String[] allowed = LOGIC_DELETE_ALLOWLIST.get(table.getKey());
            if (allowed != null && allowed[0].equals(column)) {
                seenAllowlisted.add(table.getKey() + "." + column);
                continue;
            }
            violations.add(table.getKey() + "." + column + "（应为 del_flag）");
        }
        assertTrue(violations.isEmpty(),
                "逻辑删除列必须统一 del_flag（0=存在 / 2=删除，配 BaseEntity + 全局 logic-delete-field）；"
                        + "如属有意保留的模块内约定，请加入 LOGIC_DELETE_ALLOWLIST 并写明理由：\n  "
                        + String.join("\n  ", violations));

        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : LOGIC_DELETE_ALLOWLIST.entrySet()) {
            if (!seenAllowlisted.contains(entry.getKey() + "." + entry.getValue()[0])) {
                stale.add(entry.getKey() + "." + entry.getValue()[0]);
            }
        }
        assertTrue(stale.isEmpty(), "逻辑删除白名单已过期（该表已合规或已不存在），请删除登记：\n  "
                + String.join("\n  ", stale));
    }

    @Test
    @DisplayName("collation：除登记的有意例外，一律 utf8mb4_0900_ai_ci")
    void collationMustBeSingle() throws IOException {
        Map<String, Set<String>> tableCollations = parseTableCollations(readDdl());
        List<String> violations = new ArrayList<>();
        Set<String> seenAllowlisted = new LinkedHashSet<>();

        for (Map.Entry<String, Set<String>> table : tableCollations.entrySet()) {
            for (String collation : table.getValue()) {
                if ("utf8mb4_0900_ai_ci".equals(collation)) {
                    continue;
                }
                String[] allowed = COLLATION_ALLOWLIST.get(table.getKey());
                if (allowed != null && allowed[0].equals(collation)) {
                    seenAllowlisted.add(table.getKey() + ":" + collation);
                    continue;
                }
                violations.add(table.getKey() + " → " + collation);
            }
        }
        assertTrue(violations.isEmpty(),
                "DDL 中只允许 utf8mb4_0900_ai_ci（全库统一口径）；确需区分大小写等特殊语义时"
                        + "请加入 COLLATION_ALLOWLIST 并写明理由：\n  " + String.join("\n  ", violations));

        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : COLLATION_ALLOWLIST.entrySet()) {
            if (!seenAllowlisted.contains(entry.getKey() + ":" + entry.getValue()[0])) {
                stale.add(entry.getKey() + ":" + entry.getValue()[0]);
            }
        }
        assertTrue(stale.isEmpty(), "collation 白名单已过期，请删除登记：\n  " + String.join("\n  ", stale));
    }

    @Test
    @DisplayName("白名单自检：每条必须有可读理由")
    void allowlistsHaveReasons() {
        MONEY_ALLOWLIST.forEach((key, reason) -> assertTrue(reason.length() > 15, "金额白名单缺理由：" + key));
        LOGIC_DELETE_ALLOWLIST.forEach((key, value) -> assertTrue(value[1].length() > 15, "软删白名单缺理由：" + key));
        COLLATION_ALLOWLIST.forEach((key, value) -> assertTrue(value[1].length() > 15, "collation 白名单缺理由：" + key));
    }

    @Test
    @DisplayName("状态默认值 must fail-closed：核验/审核类状态列不得默认放行；update_time 必须自动更新")
    void failOpenDefaultsAndTimestampAutoUpdate() throws IOException {
        Map<String, List<ColumnLine>> tables = parseColumnLines(readDdl());
        int columns = tables.values().stream().mapToInt(List::size).sum();
        List<String> violations = findFailOpenDefaults(tables);

        System.out.println("[DdlConventionGuard] fail-open 默认值检查：表=" + tables.size()
                + "，列=" + columns + "，违规=" + violations.size());
        assertTrue(tables.size() > 100 && columns > 1000,
                "解析器未覆盖到 DDL（表=" + tables.size() + "，列=" + columns + "）——检查会假绿");
        assertTrue(violations.isEmpty(),
                "状态默认值/时间戳语义缺陷（v13.18，报告 §6.4）：\n  " + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("解析器自检：列类型/精度、软删列识别、collation 提取")
    void parserWorksOnFixtures() {
        String fixture = """
                CREATE TABLE `demo_money` (
                  `id` bigint NOT NULL,
                  `price` decimal(10,2) NOT NULL COMMENT '价格',
                  `cost` decimal(12,6) DEFAULT NULL COMMENT '成本',
                  `name` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

                CREATE TABLE `demo_soft` (
                  `id` bigint NOT NULL,
                  `is_deleted` tinyint NOT NULL DEFAULT '0',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB;
                """;

        Map<String, List<ColumnDef>> tables = parseTables(fixture);
        assertEquals(2, tables.size());
        ColumnDef price = tables.get("demo_money").stream().filter(c -> c.name().equals("price")).findFirst().orElseThrow();
        assertEquals("decimal", price.type());
        assertEquals(10, price.precision());
        assertEquals(2, price.scale());

        assertTrue(tables.get("demo_soft").stream().anyMatch(c -> c.name().equals("is_deleted")),
                "软删列必须被识别");
        assertFalse(tables.get("demo_money").stream().anyMatch(c -> LOGIC_DELETE_COLUMNS.contains(c.name())),
                "无软删列的表不应误报");

        Map<String, Set<String>> collations = parseTableCollations(fixture);
        assertTrue(collations.get("demo_money").contains("utf8mb4_general_ci"), "列级 collation 必须被采集");
        assertFalse(collations.containsKey("demo_soft"), "无 collation 的表不应出现在结果里");
    }

    @Test
    @DisplayName("解析器自检（v13.18 新增）：fail-open 默认值与 update_time 自动更新判定")
    void failOpenDefaultParserWorksOnFixtures() {
        String fixture = """
                CREATE TABLE `demo_card` (
                  `id` bigint NOT NULL,
                  `verify_status` varchar(16) NOT NULL DEFAULT 'VERIFIED' COMMENT '核验状态',
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB;

                CREATE TABLE `demo_ok` (
                  `id` bigint NOT NULL,
                  `verify_status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT '核验状态',
                  `audit_status` varchar(16) NOT NULL DEFAULT 'auditing' COMMENT '审核状态（非放行值：不受规则命中）',
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                  `del_flag` char(1) NOT NULL DEFAULT '0',
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `uk_demo` (`id`),
                  KEY `idx_demo` (`del_flag`)
                ) ENGINE=InnoDB;
                """;

        Map<String, List<ColumnLine>> tables = parseColumnLines(fixture);
        assertEquals(2, tables.size());
        assertEquals(6, tables.get("demo_ok").size(), "demo_ok 应有 6 个列定义");
        assertTrue(tables.get("demo_ok").stream().noneMatch(c ->
                        c.name().matches("(?i)primary|unique|key|idx_demo|uk_demo")),
                "约束行（PRIMARY/UNIQUE/KEY）不得被当成列定义");

        List<String> violations = findFailOpenDefaults(tables);
        assertEquals(2, violations.size(), "期望命中 2 处：demo_card 的 fail-open 默认值 + update_time 缺 ON UPDATE");
        assertTrue(violations.get(0).contains("demo_card") || violations.get(1).contains("demo_card"));
        assertTrue(violations.stream().anyMatch(v -> v.contains("verify_status") && v.contains("VERIFIED")),
                "fail-open 默认值必须命中");
        assertTrue(violations.stream().anyMatch(v -> v.contains("update_time") && v.contains("ON UPDATE")),
                "缺 ON UPDATE 必须命中");
        assertTrue(violations.stream().noneMatch(v -> v.contains("demo_ok")),
                "合规写法（PENDING / 非放行 audit 值 / 带 ON UPDATE）不得误报");
    }

    // ==================== 解析实现 ====================

    /** 核验/审核类状态列：默认值不得是"放行"语义（fail-open 默认值 = 直插 SQL 可绕过核验） */
    private static final Pattern VERIFY_LIKE_COLUMN =
            Pattern.compile(".*(verify_status|audit_status|review_status|cert_status).*", Pattern.CASE_INSENSITIVE);

    /** 默认值中的"放行"取值 */
    private static final Set<String> FAIL_OPEN_DEFAULTS =
            Set.of("VERIFIED", "APPROVED", "PASSED", "SUCCESS", "TRUE", "1");

    /** 建表块内"列名 → 原始列定义行"（跳过 KEY/PRIMARY/UNIQUE/INDEX 等约束行） */
    private static final Pattern COLUMN_LINE =
            Pattern.compile("(?im)^\\s*`(\\w+)`\\s+([^\\r\\n]*)$");

    /**
     * 找出两类"默认值/语义缺陷"（v13.18）
     * <ol>
     *   <li>核验/审核类状态列默认值是放行值（如 {@code verify_status DEFAULT 'VERIFIED'}）——
     *       任何绕过业务核验的直插 SQL 都会拿到"已验证/已通过"；</li>
     *   <li>{@code update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP} 却缺
     *       {@code ON UPDATE CURRENT_TIMESTAMP}——看着像自动维护，实际更新不刷新。</li>
     * </ol>
     */
    static List<String> findFailOpenDefaults(Map<String, List<ColumnLine>> tables) {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, List<ColumnLine>> entry : tables.entrySet()) {
            for (ColumnLine column : entry.getValue()) {
                String upper = column.definition().toUpperCase();
                Matcher defaultValue = Pattern.compile("DEFAULT\\s+'?([A-Za-z0-9_]+)'?").matcher(column.definition());
                String value = defaultValue.find() ? defaultValue.group(1).toUpperCase() : null;
                if (VERIFY_LIKE_COLUMN.matcher(column.name()).matches()
                        && value != null && FAIL_OPEN_DEFAULTS.contains(value)) {
                    violations.add(entry.getKey() + "." + column.name()
                            + " 默认值 '" + value + "' 是放行语义（fail-open）→ 必须默认 'PENDING'/未通过");
                }
                if ("update_time".equalsIgnoreCase(column.name())
                        && upper.contains("NOT NULL") && upper.contains("CURRENT_TIMESTAMP")
                        && !upper.contains("ON UPDATE")) {
                    violations.add(entry.getKey() + ".update_time 有 DEFAULT CURRENT_TIMESTAMP 却缺 "
                            + "ON UPDATE CURRENT_TIMESTAMP（更新时不刷新，语义误导）");
                }
            }
        }
        return violations;
    }

    /** 建表块 → 每个表的原始列定义行 */
    static Map<String, List<ColumnLine>> parseColumnLines(String ddl) {
        Map<String, List<ColumnLine>> tables = new LinkedHashMap<>();
        Matcher tableMatcher = CREATE_TABLE.matcher(ddl);
        while (tableMatcher.find()) {
            String table = tableMatcher.group(1);
            List<ColumnLine> columns = new ArrayList<>();
            Matcher columnMatcher = COLUMN_LINE.matcher(tableMatcher.group(2));
            while (columnMatcher.find()) {
                columns.add(new ColumnLine(table, columnMatcher.group(1), columnMatcher.group(2)));
            }
            tables.put(table, columns);
        }
        return tables;
    }

    /** 建表块 → 列定义 */
    static Map<String, List<ColumnDef>> parseTables(String ddl) {
        Map<String, List<ColumnDef>> tables = new LinkedHashMap<>();
        Matcher tableMatcher = CREATE_TABLE.matcher(ddl);
        while (tableMatcher.find()) {
            String table = tableMatcher.group(1);
            List<ColumnDef> columns = new ArrayList<>();
            Matcher columnMatcher = COLUMN_DEF.matcher(tableMatcher.group(2));
            while (columnMatcher.find()) {
                String name = columnMatcher.group(1);
                String type = columnMatcher.group(2);
                int precision = -1;
                int scale = -1;
                Matcher args = Pattern.compile("\\(\\s*(\\d+)\\s*(?:,\\s*(\\d+)\\s*)?\\)").matcher(type);
                if (args.find()) {
                    precision = Integer.parseInt(args.group(1));
                    scale = args.group(2) == null ? -1 : Integer.parseInt(args.group(2));
                }
                String plainType = type.replaceAll("\\(.*", "");
                columns.add(new ColumnDef(name, plainType, precision, scale));
            }
            tables.put(table, columns);
        }
        return tables;
    }

    /** 建表块 → 该表出现的全部 collation（表级 + 列级） */
    static Map<String, Set<String>> parseTableCollations(String ddl) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        Matcher tableMatcher = CREATE_TABLE.matcher(ddl);
        while (tableMatcher.find()) {
            String table = tableMatcher.group(1);
            // 建表块 + 紧随其后的 ENGINE 行（表级 collation 在 ) ENGINE=... 这一行上）
            int tail = ddl.indexOf('\n', tableMatcher.end());
            String block = tableMatcher.group() + (tail > 0 ? ddl.substring(tableMatcher.end() - 6, Math.min(ddl.length(), tail)) : "");
            Set<String> collations = new LinkedHashSet<>();
            Matcher collateMatcher = COLLATE.matcher(block);
            while (collateMatcher.find()) {
                collations.add(collateMatcher.group(1));
            }
            if (!collations.isEmpty()) {
                result.put(table, collations);
            }
        }
        return result;
    }

    private static String readDdl() throws IOException {
        return Files.readString(DDL, StandardCharsets.UTF_8);
    }

    /** 列定义（类型 + 精度） */
    record ColumnDef(String name, String type, int precision, int scale) {
    }

    /** 列定义原始行（表名 + 列名 + 该行完整定义） */
    record ColumnLine(String table, String name, String definition) {
    }
}
