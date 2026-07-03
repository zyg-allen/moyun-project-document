package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 刷题日历热力图单元（3.4）
 *
 * @author moyun
 */
@Data
public class LearnCalendarCellVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 日期，格式 yyyy-MM-dd */
    private String date;

    /** 当日提交数 */
    private Integer count;

    /** 当日通过数 */
    private Integer successCount;
}
