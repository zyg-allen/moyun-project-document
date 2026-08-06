package com.moyun.ext.ai.enums;

/**
 * 知识库处理状态枚举
 */
public enum ProcessingStatus {

    /**
     * 待配置 - 文件已上传，等待用户配置
     */
    PENDING("pending", "待配置"),

    /**
     * 已配置 - 用户完成配置，等待处理
     */
    CONFIGURED("configured", "已配置"),

    /**
     * 处理中 - 正在分片和向量化
     */
    PROCESSING("processing", "处理中"),

    /**
     * 已完成 - 处理成功
     */
    COMPLETED("completed", "已完成"),

    /**
     * 失败 - 处理失败
     */
    FAILED("failed", "失败");

    private final String code;
    private final String desc;

    ProcessingStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ProcessingStatus fromCode(String code) {
        for (ProcessingStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
