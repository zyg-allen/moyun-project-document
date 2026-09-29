package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简历解析配置对象 portal_resume_parse_config
 *
 * <p><b>规则解析的词表配置来源</b>（v13.38 简历解析重构）：把「章节标题词典 / 技能词域 /
 * 学历词 / 岗位词」从代码里挪到表里，支持后台「简历解析配置」页维护。</p>
 *
 * <p><b>与代码内默认词典的关系</b>：本表是配置权威来源；但规则引擎内置同内容默认词典兜底，
 * 保证新库/表为空时规则解析仍可开箱可用（配置缺失不会导致解析失败）。</p>
 *
 * <p>技能词域无需铺满：规则引擎会<b>自动聚合</b> {@code portal_job_template.required_skills}
 * （岗位必备技能）作为主词域，本表只补充「岗位技能之外的通识技能」。</p>
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_resume_parse_config")
public class PortalResumeParseConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置类型：section 章节词 / skill 技能词 / degree 学历词 / position 岗位词 */
    private String configType;

    /**
     * 配置键。
     * <p>configType=section 时为<b>目标大类</b>（edu/work/project/skill/self/intention/basic/other）；
     * 其余类型为词条本身。</p>
     */
    private String itemKey;

    /** 显示名称（后台列表展示，如「教育背景」） */
    private String itemName;

    /** 关键词（多个用英文逗号分隔；section 用于匹配章节标题，其余用于全文匹配） */
    private String keywords;

    /** 排序（升序） */
    private Integer sort;

    /** 状态：active 启用 / inactive 停用 */
    private String status;
}
