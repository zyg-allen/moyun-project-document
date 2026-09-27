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
 * 结构守卫：{@code increment-sql/} 增量脚本必须**可重跑**（幂等），破坏性操作必须先备份
 *
 * <h3>为什么需要这条守卫</h3>
 * <p>增量脚本的读者是"线上已有库"，而线上库的状态**不可假设**：可能没跑过、跑过一次、甚至手工改过。
 * 因此每条 DDL 都必须先查状态再决定是否变更。真实缺陷（报告 §6.4，v13.18 修复）：</p>
 * <ul>
 *   <li>{@code 20260925-01} 的 {@code ALTER TABLE portal_user DROP KEY idx_email, ADD UNIQUE KEY uk_email(email)}
 *       **无前置判断**：库已迁移（{@code uk_email} 已建、{@code idx_email} 已删）时重跑直接
 *       {@code ERROR 1091 (Can't DROP 'idx_email')} —— 脚本二次执行即中断；</li>
 *   <li>同脚本的两处 {@code SET p.phone/email = NULL} 是**破坏性清洗**，
 *       却没有任何备份步骤，且"校验清洗结果"只写在注释里（不执行、不阻断）。</li>
 * </ul>
 *
 * <h3>两条规则</h3>
 * <ol>
 *   <li><b>{@code DROP KEY}/{@code DROP INDEX}/{@code ADD UNIQUE KEY}/{@code ADD COLUMN} 必须有前置判断</b>：
 *       文件里必须出现 {@code information_schema} 守卫（按 {@code TABLE_SCHEMA = DATABASE()} 查
 *       {@code COLUMNS}/{@code STATISTICS}）；</li>
 *   <li><b>破坏性清洗（{@code SET <col> = NULL}）必须同期建备份表</b>：
 *       文件里必须出现 {@code CREATE TABLE IF NOT EXISTS `..._bak_...` AS SELECT}。</li>
 * </ol>
 * <p>白名单按"脚本文件名 → 理由"登记，且带失效自检（脚本已修正/删除即需删登记）。</p>
 *
 * @author moyun
 */
class IncrementSqlIdempotencyGuardTest {

    private static final Path SQL_DIR = Path.of("src/main/resources/increment-sql");

    /**
     * 结构性 DDL：**重跑会报错**的那些（必须能重跑）
     * <p>刻意**不包含 {@code MODIFY COLUMN}**——它重跑只是重复应用同一份列定义，不报错（天然幂等）；
     * 而下面这些在"目标状态已达成"时重跑会直接失败并中断脚本：
     * {@code DROP KEY/INDEX} → 1091、{@code ADD COLUMN} → 1060、{@code ADD KEY/INDEX/UNIQUE} → 1061、
     * {@code DROP COLUMN}、{@code CHANGE COLUMN}（旧列名不存在 → 1054）。</p>
     */
    private static final Pattern STRUCTURAL_DDL = Pattern.compile(
            "(?i)\\b(DROP\\s+(KEY|INDEX|COLUMN)|ADD\\s+(UNIQUE\\s+)?(KEY|INDEX|COLUMN)|CREATE\\s+(UNIQUE\\s+)?INDEX|CHANGE\\s+COLUMN)\\b");

    /** 破坏性清洗 */
    private static final Pattern DESTRUCTIVE_CLEANUP = Pattern.compile(
            "(?i)\\bSET\\s+[\\w.]*`?\\w+`?\\s*=\\s*NULL\\b");

    /** 备份表创建 */
    private static final Pattern BACKUP_TABLE = Pattern.compile(
            "(?i)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`?\\w*_bak_\\w*`?\\s+AS\\s+SELECT");

    /** 前置判断（information_schema 守卫） */
    private static final Pattern GUARD = Pattern.compile("information_schema\\.(COLUMNS|STATISTICS|KEY_COLUMN_USAGE)");

    /** 白名单：脚本文件名 → 理由（当前为空：全部脚本已按规则改造） */
    private static final Map<String, String> WHITELIST = Map.of();

    @Test
    @DisplayName("全部增量脚本：结构性 DDL 有 information_schema 守卫；破坏性清洗有备份表")
    void incrementalScriptsAreRerunnable() throws IOException {
        List<String> violations = new ArrayList<>();
        int files = 0;
        int structuralFiles = 0;
        int cleanupFiles = 0;

        try (Stream<Path> walk = Files.list(SQL_DIR)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".sql")).toList()) {
                String name = file.getFileName().toString();
                String sql = stripSqlComments(Files.readString(file, StandardCharsets.UTF_8));
                files++;
                boolean guarded = GUARD.matcher(sql).find();
                boolean hasStructural = STRUCTURAL_DDL.matcher(sql).find();
                boolean hasCleanup = DESTRUCTIVE_CLEANUP.matcher(sql).find();
                boolean hasBackup = BACKUP_TABLE.matcher(sql).find();
                if (hasStructural) {
                    structuralFiles++;
                    if (!guarded) {
                        violations.add(name + "：含结构性 DDL 但没有 information_schema 前置判断"
                                + "（已有库重跑会报 1091/1060/1061 而中断）");
                    }
                }
                if (hasCleanup) {
                    cleanupFiles++;
                    if (!hasBackup) {
                        violations.add(name + "：含破坏性清洗 SET <col> = NULL 但没有备份表创建语句"
                                + "（CREATE TABLE IF NOT EXISTS `..._bak_...` AS SELECT）");
                    }
                }
            }
        }

        List<String> unexplained = violations.stream()
                .filter(v -> WHITELIST.keySet().stream().noneMatch(v::startsWith))
                .sorted(Comparator.naturalOrder())
                .toList();

        System.out.println("[IncrementSqlIdempotencyGuard] 扫描脚本=" + files
                + "（含结构性 DDL=" + structuralFiles + "，含破坏性清洗=" + cleanupFiles + "），违规=" + violations.size());
        assertTrue(files >= 5, "扫描到的增量脚本过少（" + files + "）——路径或解析可能失效，守卫会假绿");
        assertTrue(structuralFiles >= 3,
                "识别到的结构性 DDL 脚本过少（" + structuralFiles + "）——解析可能失效");
        assertTrue(unexplained.isEmpty(),
                "增量脚本不可重跑（报告 §6.4）：\n  " + String.join("\n  ", unexplained));
    }

    @Test
    @DisplayName("扫描器自检：无守卫的结构变更、无备份的破坏性清洗必须命中；正确写法不得误报")
    void scannerSelfCheck() {
        // 1. 无守卫的结构性 DDL（修复前的真实形态）
        assertEquals(1, check("ALTER TABLE portal_user DROP KEY idx_email, ADD UNIQUE KEY uk_email (email);")
                .size(), "无信息架构守卫的结构变更必须命中");

        // 2. 有守卫 + 预处理语句 → 合规
        assertEquals(0, check("""
                SET @has := (SELECT COUNT(*) FROM information_schema.STATISTICS
                             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='portal_user' AND INDEX_NAME='idx_email');
                SET @sql := IF(@has > 0, 'ALTER TABLE portal_user DROP KEY idx_email', 'DO 0');
                PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
                """).size(), "带守卫的结构变更应合规");

        // 3. 破坏性清洗无备份 → 命中
        assertEquals(1, check("UPDATE portal_user p SET p.phone = NULL WHERE p.id > 1;").size(),
                "无备份的破坏性清洗必须命中");

        // 4. 破坏性清洗 + 备份表 → 合规
        assertEquals(0, check("""
                CREATE TABLE IF NOT EXISTS `portal_user_bak_20260925` AS SELECT * FROM `portal_user`;
                UPDATE portal_user p SET p.phone = NULL WHERE p.id > 1;
                """).size(), "先建备份再做破坏性清洗应合规");

        // 5. 普通 UPDATE（非 NULL 清洗）/ 注释里的关键词 → 不报
        assertEquals(0, check("""
                -- 说明：历史上曾 SET p.phone = NULL，现已改为带备份
                UPDATE portal_user SET del_flag = '0' WHERE del_flag IS NULL;
                """).size(), "注释中的字样与普通 UPDATE 不得误报");

        // 6. ADD COLUMN 带守卫 → 合规（v13.13 话题脚本形态）
        assertEquals(0, check("""
                SET @has_old := (SELECT COUNT(*) FROM information_schema.COLUMNS
                                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'portal_topic_post' AND COLUMN_NAME = 'is_deleted');
                SET @sql := IF(@has_old = 1, 'ALTER TABLE `portal_topic_post` ADD COLUMN `del_flag` char(1) NOT NULL DEFAULT ''0''', 'DO 0');
                PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
                """).size(), "带守卫的 ADD COLUMN 应合规");
    }

    // ==================== 扫描实现 ====================

    /** 对单段 SQL 文本做两条规则检查（测试与主扫描共用） */
    static List<String> check(String sql) {
        String code = stripSqlComments(sql);
        List<String> violations = new ArrayList<>();
        boolean guarded = GUARD.matcher(code).find();
        if (STRUCTURAL_DDL.matcher(code).find() && !guarded) {
            violations.add("结构性 DDL 缺 information_schema 前置判断");
        }
        if (DESTRUCTIVE_CLEANUP.matcher(code).find() && !BACKUP_TABLE.matcher(code).find()) {
            violations.add("破坏性清洗缺备份表");
        }
        return violations;
    }

    /**
     * 剥离 SQL 注释（{@code --} 行注释与 {@code /* *}{@code /} 块注释），保留换行与格式
     * <p>必须剥离：脚本头部注释里常写"历史上 SET x = NULL""本脚本会 DROP KEY ..."这类说明，
     * 不剥离会把文档当成代码误报（本守卫第一版就踩了这个坑）。</p>
     */
    static String stripSqlComments(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            char c2 = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (c == '-' && c2 == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    sb.append(' ');
                    i++;
                }
            } else if (c == '/' && c2 == '*') {
                sb.append("  ");
                i += 2;
                while (i < sql.length() && !(sql.charAt(i) == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/')) {
                    sb.append(sql.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < sql.length()) {
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
