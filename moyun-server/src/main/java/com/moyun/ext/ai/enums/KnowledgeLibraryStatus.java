package com.moyun.ext.ai.enums;

/**
 * 知识库主表状态枚举
 *
 * <p>对应数据库表 knowledge_library.status 字段，标识知识库的可用性：
 * <ul>
 *   <li>{@link #ACTIVE}    - 正常：可被智能体引用并检索</li>
 *   <li>{@link #DISABLED} - 禁用：暂停检索，但保留数据和配置</li>
 *   <li>{@link #ARCHIVED}  - 归档：长期不用，从默认列表隐藏</li>
 * </ul>
 *
 * <p>与软删除（deleted 字段）的区别：
 * <ul>
 *   <li>status 是业务状态，可逆（DISABLED → ACTIVE）</li>
 *   <li>deleted 是删除标记，逻辑上不可逆（仅管理员可恢复）</li>
 * </ul>
 *
 * @author moyun
 */
public enum KnowledgeLibraryStatus {

    ACTIVE("active", "正常"),
    DISABLED("disabled", "禁用"),
    ARCHIVED("archived", "归档");

    private final String code;
    private final String desc;

    KnowledgeLibraryStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static KnowledgeLibraryStatus fromCode(String code) {
        if (code == null) {
            return ACTIVE;
        }
        for (KnowledgeLibraryStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return ACTIVE;
    }
}
