package com.moyun.system.flowable.common.enums;

/**
 * 流程意见类型
 *
 * @author Tony
 * @date 2021-04-03
 */
public enum FlowComment {

    /**
     * 正常
     */
    NORMAL("1", "正常"),

    /**
     * 退回
     */
    RETURN("2", "退回"),

    /**
     * 驳回
     */
    REJECT("3", "驳回"),

    /**
     * 委派
     */
    DELEGATE("4", "委派"),

    /**
     * 转办
     */
    TRANSFER("5", "转办"),

    /**
     * 终止
     */
    STOP("6", "终止");

    private final String code;

    private final String desc;

    FlowComment(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String getType() {
        return code;
    }

    /**
     * 退回（驳回）
     */
    public static final String REBACK = "2";

}