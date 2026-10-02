package com.moyun.ext.ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL 安全校验与结果集脱敏的守卫测试（清单 P2 修复固化）。
 *
 * <p>背景：此前存在三处"安全控制形同虚设"——</p>
 * <ol>
 *   <li>调用方用 1 参 {@code validateSql} ⇒ {@code allowedTables} 恒为 null，**表名白名单整条分支从未生效**；</li>
 *   <li>{@code isLegitimateUnion} 的 {@code beforeUnion.matches(".*\\w$")} 对任何查询恒真 ⇒ **UNION 检查从不拦截**；</li>
 *   <li>结果集脱敏被整段注释禁用。</li>
 * </ol>
 *
 * <p>本测试把修复后的行为**固定下来**：白名单必须生效、注入形态的 UNION 必须被拦、合法 UNION 必须放行、
 * 严格档脱敏必须"只打码可自证的 PII、不误伤业务名称"。</p>
 */
class SqlSecurityValidatorTest {

    @Test
    @DisplayName("表名白名单生效：未授权表被拒绝")
    void whitelistRejectsUnauthorizedTable() {
        Set<String> allowed = Set.of("portal_article", "portal_user");
        SqlSecurityValidator.ValidationResult r =
                SqlSecurityValidator.validate("SELECT id, title FROM portal_article", allowed);
        assertTrue(r.isValid(), "白名单内的表应放行");

        SqlSecurityValidator.ValidationResult bad =
                SqlSecurityValidator.validate("SELECT id, password FROM sys_user", allowed);
        assertFalse(bad.isValid(), "白名单外的表必须被拒绝");
        assertEquals("HIGH", bad.getRiskLevel());
        assertTrue(bad.getMessage().contains("sys_user"), "错误信息应指出具体表名，便于排查");
    }

    @Test
    @DisplayName("白名单生效：JOIN 中的未授权表同样被拒绝")
    void whitelistRejectsUnauthorizedJoinTable() {
        Set<String> allowed = Set.of("portal_article");
        SqlSecurityValidator.ValidationResult r = SqlSecurityValidator.validate(
                "SELECT a.id FROM portal_article a JOIN sys_user u ON u.id = a.author_id", allowed);
        assertFalse(r.isValid(), "JOIN 里的未授权表必须被拒绝");
    }

    @Test
    @DisplayName("UNION 检查不再形同虚设：注入形态被拦、合法合并查询放行")
    void unionCheckIsMeaningful() {
        // ① 单引号未配对（典型注入拼接）
        SqlSecurityValidator.ValidationResult unpaired = SqlSecurityValidator.validate(
                "SELECT id FROM portal_article WHERE title = 'abc UNION SELECT password FROM sys_user");
        assertFalse(unpaired.isValid(), "UNION 前单引号未配对必须被拦");

        // ② UNION 之前出现注释符（注入拼接手法）
        SqlSecurityValidator.ValidationResult commented = SqlSecurityValidator.validate(
                "SELECT id FROM portal_article WHERE id = 1 -- UNION SELECT password FROM sys_user");
        assertFalse(commented.isValid(), "含注释的 UNION 必须被拦");

        // ③ UNION 后面不是 SELECT
        SqlSecurityValidator.ValidationResult notSelect = SqlSecurityValidator.validate(
                "SELECT id FROM portal_article UNION DELETE FROM portal_article");
        assertFalse(notSelect.isValid(), "UNION 后非 SELECT 必须被拦");

        // ④ 合法：顶层 UNION SELECT（两表都在白名单内）
        Set<String> allowed = Set.of("portal_article", "portal_book");
        SqlSecurityValidator.ValidationResult ok = SqlSecurityValidator.validate(
                "SELECT id FROM portal_article UNION SELECT id FROM portal_book", allowed);
        assertTrue(ok.isValid(), "合法的 UNION SELECT 应放行（不能把正常功能一起拦掉）");
    }

    @Test
    @DisplayName("特征化：不传白名单时校验器不限制表 ⇒ 调用方必须显式传入（本次修复的根因）")
    void mustPassWhitelistFromCaller() {
        // 这条测试记录"为什么调用方那行代码重要"：
        // 1 参 validate(sql) 等价于"不限制表"，因此像 DataQueryServiceImpl 这类把 AI 生成的 SQL
        // 直接丢进校验的调用方，若不传白名单，等于**没有表访问控制**。
        String sqlOnAnyTable = "SELECT id, password FROM sys_user";
        assertTrue(SqlSecurityValidator.validate(sqlOnAnyTable).isValid(),
                "1 参重载不限制表（这是既有语义，不能悄悄改变）");
        assertFalse(SqlSecurityValidator.validate(sqlOnAnyTable, Set.of("portal_article")).isValid(),
                "显式传白名单后必须拒绝未授权表");
    }

    @Test
    @DisplayName("严格档脱敏：打码可自证的 PII，不误伤业务名称")
    void strictMaskingTargetsPiiOnly() {
        // 手机号：字段名与值都能自证 ⇒ 打码
        String phone = DataMaskingUtils.autoMaskStrict("mobile", "13812345678");
        assertNotEquals("13812345678", phone, "手机号必须被脱敏");
        assertTrue(phone.contains("*"), "脱敏结果应含掩码字符");

        // 邮箱：打码
        String email = DataMaskingUtils.autoMaskStrict("email", "someone@example.com");
        assertNotEquals("someone@example.com", email, "邮箱必须被脱敏");

        // 身份证：打码
        String idCard = DataMaskingUtils.autoMaskStrict("id_card", "110101199003071234");
        assertNotEquals("110101199003071234", idCard, "身份证号必须被脱敏");

        // 业务名称：**不得**被脱敏（原 autoMask 会因为字段含 name 而打码公司名，使分析结果失去意义）
        assertEquals("旭林知行科技有限公司",
                DataMaskingUtils.autoMaskStrict("company_name", "旭林知行科技有限公司"),
                "业务名称不应被严格档脱敏");
        assertEquals("高级工程师",
                DataMaskingUtils.autoMaskStrict("position_name", "高级工程师"),
                "职位名称不应被严格档脱敏");
        // 地址同理（不在严格档范围）
        assertEquals("北京市海淀区",
                DataMaskingUtils.autoMaskStrict("address", "北京市海淀区"),
                "地址不在严格档范围内（如需按列脱敏应由后台配置策略）");
    }
}
