package com.moyun.ext.ai.enums;

/**
 * 分析报告状态枚举
 *
 * <p>对应数据库表 analysis_report.report_status 字段，标识报告的生命周期：
 * <ul>
 *   <li>{@link #DRAFT}      - 草稿：生成中或未完成</li>
 *   <li>{@link #COMPLETED}  - 已完成：可查看、导出</li>
 *   <li>{@link #ARCHIVED}   - 已归档：从默认列表隐藏，保留数据</li>
 * </ul>
 *
 * @author moyun
 */
public enum ReportStatus {

    DRAFT("draft", "草稿"),
    COMPLETED("completed", "已完成"),
    ARCHIVED("archived", "已归档");

    private final String code;
    private final String desc;

    ReportStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ReportStatus fromCode(String code) {
        if (code == null) {
            return DRAFT;
        }
        for (ReportStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }
}
