package com.moyun.ext.cms.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 职位详情 VO（含公司信息）
 *
 * @author moyun
 */
@Data
public class JobVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long companyId;
    private String title;
    private String city;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String experience;
    private String education;
    private String description;
    private String requirement;
    private String status;

    /** 公司名称 */
    private String companyName;
    /** 公司Logo */
    private String companyLogo;
    /** 公司行业 */
    private String companyIndustry;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
