package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 面试岗位字典表 portal_interview_position
 * <p>
 * 驱动分类筛选与画像抽题。required_skills / hot_companies 为 JSON 字符串数组，
 * 与 portal_tag.name 对齐，便于推荐 SQL JOIN。
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_position")
public class PortalInterviewPosition extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 岗位编码（如 java_backend） */
    private String code;

    /** 岗位名称（如 Java后端工程师） */
    private String name;

    /** 所属行业 */
    private String industry;

    /** 岗位级别 junior/mid/senior */
    private String level;

    /** 必备技能 JSON 数组（如 ["Spring","MySQL"]，与 portal_tag.name 对齐） */
    private String requiredSkills;

    /** 热门公司 JSON 数组（如 ["阿里","腾讯"]） */
    private String hotCompanies;

    /** 岗位描述 */
    private String description;

    /** 排序 */
    private Integer sort;

    /** 状态 active/inactive */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
