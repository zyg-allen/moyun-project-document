package com.moyun.util.uuid;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 业务主键生成器（v5.9 P1）
 *
 * <p>用于替代自增 ID 作为表间关联的业务主键，避免父表 TRUNCATE 后自增 ID 重置导致子表关联错乱。</p>
 *
 * <h3>格式</h3>
 * <pre>{前缀}_{13位毫秒时间戳}_{6位Base62随机后缀}</pre>
 * <ul>
 *   <li>示例：{@code art_1751234567890_a3b2c1}</li>
 *   <li>长度：约 25-27 字符（含分隔符）</li>
 *   <li>建议字段类型：VARCHAR(32)，加唯一索引</li>
 * </ul>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li><b>半有序</b>：13 位毫秒时间戳单调递增，对 InnoDB 聚簇索引友好，避免页分裂。</li>
 *   <li><b>可读性</b>：前缀标识业务类型，便于日志排查与人工识别。</li>
 *   <li><b>唯一性</b>：时间戳 + 6 位 Base62（62^6 ≈ 568 亿组合）+ SecureRandom，单机每毫秒碰撞概率极低。</li>
 *   <li><b>无依赖</b>：纯 Java 实现，无需雪花算法的机器位配置，部署简单。</li>
 *   <li><b>不替代主键</b>：双轨过渡方案中保留自增 id 作为物理主键，business_id 作为业务关联键。</li>
 * </ul>
 *
 * <h3>前缀规范</h3>
 * <pre>
 * usr_  门户用户       art_  文章          cat_  分类
 * com_  评论           tag_  标签
 * sysu_ 后台用户（保留 sys 命名空间，避免与 usr_ 冲突）
 * </pre>
 *
 * @author moyun
 * @since v5.9 P1
 */
public final class BusinessIdGenerator {

    /** Base62 字符表，用于随机后缀，避免 URL/SQL 转义问题 */
    private static final char[] BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /** 随机后缀长度（6 位 ≈ 568 亿组合，单机每毫秒碰撞概率可忽略） */
    private static final int RANDOM_SUFFIX_LENGTH = 6;

    /** 线程安全的强随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    private BusinessIdGenerator() {
    }

    /**
     * 生成业务主键。
     *
     * @param prefix 业务前缀，如 {@code "art"}、{@code "usr"}，不允许为空
     * @return 业务主键字符串，如 {@code art_1751234567890_a3b2c1}
     */
    public static String generate(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            throw new IllegalArgumentException("业务主键前缀不能为空");
        }
        long timestamp = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        StringBuilder sb = new StringBuilder(prefix.length() + 1 + 13 + 1 + RANDOM_SUFFIX_LENGTH);
        sb.append(prefix).append('_').append(timestamp).append('_');
        for (int i = 0; i < RANDOM_SUFFIX_LENGTH; i++) {
            sb.append(BASE62[RANDOM.nextInt(BASE62.length)]);
        }
        return sb.toString();
    }

    // ===== 内置业务前缀常量，统一管理，避免散落各处 =====

    /** 门户用户前缀 */
    public static final String PREFIX_PORTAL_USER = "usr";
    /** 后台用户前缀（沿用 sys 命名空间，避免与 usr_ 冲突） */
    public static final String PREFIX_SYS_USER = "sysu";
    /** 文章前缀 */
    public static final String PREFIX_ARTICLE = "art";
    /** 分类前缀 */
    public static final String PREFIX_CATEGORY = "cat";
    /** 评论前缀 */
    public static final String PREFIX_COMMENT = "com";
    /** 标签前缀 */
    public static final String PREFIX_TAG = "tag";

    /** 生成门户用户业务主键 */
    public static String forPortalUser() {
        return generate(PREFIX_PORTAL_USER);
    }

    /** 生成后台用户业务主键 */
    public static String forSysUser() {
        return generate(PREFIX_SYS_USER);
    }

    /** 生成文章业务主键 */
    public static String forArticle() {
        return generate(PREFIX_ARTICLE);
    }

    /** 生成分类业务主键 */
    public static String forCategory() {
        return generate(PREFIX_CATEGORY);
    }

    /** 生成评论业务主键 */
    public static String forComment() {
        return generate(PREFIX_COMMENT);
    }

    /** 生成标签业务主键 */
    public static String forTag() {
        return generate(PREFIX_TAG);
    }
}
