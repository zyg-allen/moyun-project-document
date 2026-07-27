package com.moyun.agent.constant;

/**
 * Redis Key 常量类
 *
 * <p>统一管理项目中所有 Redis Key，便于维护和避免冲突</p>
 *
 * <h3>Key 命名规范：</h3>
 * <ul>
 *   <li>使用冒号分隔层级，如 module:submodule:id</li>
 *   <li>全部小写，单词间用冒号分隔</li>
 *   <li>动态部分使用占位符 {id} 表示</li>
 * </ul>
 *
 * @author laomao
 */
public final class RedisKeys {

    private RedisKeys() {
        // 私有构造函数，防止实例化
    }

    // ==================== 聊天记忆 ====================

    /**
     * 聊天记忆前缀
     * <p>完整Key: chat:memory:{conversationId}</p>
     * <p>类型: String</p>
     * <p>过期: 30天</p>
     */
    public static final String CHAT_MEMORY_PREFIX = "chat:memory:";

    /**
     * 获取聊天记忆Key
     */
    public static String chatMemory(Object conversationId) {
        return CHAT_MEMORY_PREFIX + conversationId;
    }

    // ==================== 知识库处理 ====================

    /**
     * 知识库处理进度前缀
     * <p>完整Key: knowledge:progress:{knowledgeId}</p>
     * <p>类型: Object (ProcessProgress)</p>
     * <p>过期: 24小时</p>
     */
    public static final String KNOWLEDGE_PROGRESS_PREFIX = "knowledge:progress:";

    /**
     * 知识库处理锁前缀
     * <p>完整Key: knowledge:lock:{knowledgeId}</p>
     * <p>类型: String</p>
     * <p>过期: 1小时</p>
     */
    public static final String KNOWLEDGE_LOCK_PREFIX = "knowledge:lock:";

    /**
     * 获取知识库进度Key
     */
    public static String knowledgeProgress(Long knowledgeId) {
        return KNOWLEDGE_PROGRESS_PREFIX + knowledgeId;
    }

    /**
     * 获取知识库锁Key
     */
    public static String knowledgeLock(Long knowledgeId) {
        return KNOWLEDGE_LOCK_PREFIX + knowledgeId;
    }

    // ==================== 验证码 ====================

    /**
     * 验证码前缀
     * <p>完整Key: captcha:{captchaKey}</p>
     * <p>类型: String</p>
     * <p>过期: 5分钟</p>
     */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 获取验证码Key
     */
    public static String captcha(String captchaKey) {
        return CAPTCHA_PREFIX + captchaKey;
    }

    // ==================== Token 使用统计 ====================

    /**
     * Token统计前缀
     */
    public static final String TOKEN_USAGE_PREFIX = "token:usage:";

    /**
     * Token日志队列（待写入数据库）
     * <p>完整Key: token:usage:logs</p>
     * <p>类型: List</p>
     */
    public static final String TOKEN_USAGE_LOGS = TOKEN_USAGE_PREFIX + "logs";

    /**
     * Token实时统计前缀
     * <p>完整Key: token:usage:realtime:{date}</p>
     * <p>类型: Hash</p>
     * <p>过期: 2天</p>
     */
    public static final String TOKEN_REALTIME_PREFIX = TOKEN_USAGE_PREFIX + "realtime:";

    /**
     * 获取Token实时统计Key（按日期）
     */
    public static String tokenRealtimeStats(String date) {
        return TOKEN_REALTIME_PREFIX + date;
    }

    /**
     * 获取Token按模型统计Key
     * <p>完整Key: token:usage:realtime:{date}:model:{modelName}</p>
     */
    public static String tokenRealtimeModel(String date, String modelName) {
        return TOKEN_REALTIME_PREFIX + date + ":model:" + modelName;
    }

    /**
     * 获取Token按类型统计Key
     * <p>完整Key: token:usage:realtime:{date}:type:{type}</p>
     */
    public static String tokenRealtimeType(String date, String type) {
        return TOKEN_REALTIME_PREFIX + date + ":type:" + type;
    }

    // ==================== 配置缓存 ====================

    /**
     * 模型配置缓存前缀
     * <p>完整Key: config:model:{id}</p>
     * <p>类型: String (JSON)</p>
     * <p>过期: 10分钟</p>
     */
    public static final String MODEL_CONFIG_PREFIX = "config:model:";

    /**
     * 默认模型配置缓存Key
     * <p>完整Key: config:model:default:{type}</p>
     */
    public static final String MODEL_CONFIG_DEFAULT_PREFIX = "config:model:default:";

    /**
     * 智能体缓存前缀
     * <p>完整Key: config:agent:{id}</p>
     * <p>类型: String (JSON)</p>
     * <p>过期: 5分钟</p>
     */
    public static final String AGENT_PREFIX = "config:agent:";

    /**
     * 获取模型配置缓存Key
     */
    public static String modelConfig(Long id) {
        return MODEL_CONFIG_PREFIX + id;
    }

    /**
     * 获取默认模型配置缓存Key
     */
    public static String modelConfigDefault(String type) {
        return MODEL_CONFIG_DEFAULT_PREFIX + type;
    }

    /**
     * 获取智能体缓存Key
     */
    public static String agent(Long id) {
        return AGENT_PREFIX + id;
    }

    /**
     * 模型配置缓存过期分钟数
     */
    public static final long MODEL_CONFIG_EXPIRE_MINUTES = 10;

    /**
     * 智能体缓存过期分钟数
     */
    public static final long AGENT_EXPIRE_MINUTES = 5;

    // ==================== 过期时间常量 ====================

    /**
     * 聊天记忆过期天数
     */
    public static final long CHAT_MEMORY_EXPIRE_DAYS = 30;

    /**
     * 知识库进度过期小时数
     */
    public static final long KNOWLEDGE_PROGRESS_EXPIRE_HOURS = 24;

    /**
     * 知识库锁过期秒数
     */
    public static final long KNOWLEDGE_LOCK_EXPIRE_SECONDS = 3600;

    /**
     * 验证码过期分钟数
     */
    public static final int CAPTCHA_EXPIRE_MINUTES = 5;

    /**
     * Token统计过期天数
     */
    public static final long TOKEN_STATS_EXPIRE_DAYS = 2;
}
