package com.moyun.ext.ai.enums;

/**
 * 对话角色枚举
 *
 * <p>对应数据库表 conversation_message.role 字段，标识消息的发送方：
 * <ul>
 *   <li>{@link #USER}      - 用户消息</li>
 *   <li>{@link #ASSISTANT} - 助手（AI）回复</li>
 * </ul>
 *
 * <p>遵循 OpenAI Chat Completion API 的 role 约定，便于直接透传至 LLM。
 *
 * @author moyun
 */
public enum ConversationRole {

    USER("user", "用户"),
    ASSISTANT("assistant", "助手");

    private final String code;
    private final String desc;

    ConversationRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ConversationRole fromCode(String code) {
        if (code == null) {
            return USER;
        }
        for (ConversationRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return USER;
    }
}
