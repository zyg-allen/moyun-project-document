package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位模板对象 portal_job_template
 *
 * <p><b>全 portal 岗位配置的唯一来源</b>：岗位下拉、岗位 JD 回填、难度/题量建议、
 * 简历岗位匹配评分（requiredSkills）、用户画像必备技能，全部读本表。</p>
 *
 * @author moyun
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_job_template")
public class PortalJobTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称（如：Java后端工程师）；岗位下拉展示项、按名称反查的匹配键 */
    private String name;

    /** 岗位类别（技术/产品/运营/设计等） */
    private String category;

    /** 岗位编码（对齐 portal_voice_interview.position） */
    private String positionCode;

    /** 岗位编码（如 java_backend），按编码反查用 */
    private String code;

    /** 所属行业（如 互联网/金融/制造） */
    private String industry;

    /** 岗位级别 junior/mid/senior */
    private String level;

    /** 必备技能 JSON 数组（如 ["Spring","MySQL","Redis"]，与 portal_tag.name 对齐）
     *  驱动简历岗位匹配评分与画像必备技能 */
    private String requiredSkills;

    /** 热门公司 JSON 数组（如 ["阿里","腾讯","字节"]） */
    private String hotCompanies;

    /** 排序（升序） */
    private Integer sort;

    /** 模板描述 */
    private String description;

    /** 岗位 JD 原文（用于 LLM 关键词提取与出题上下文；准备页选中岗位后回填「岗位要求」） */
    private String jdText;

    /** 岗位关键词，逗号分隔（LLM 提取 + 人工维护；与 requiredSkills 的区别见开发规范） */
    private String keywords;

    /** 难度:easy,medium,hard（对齐 portal_interview_question.difficulty；准备页选中岗位后回填） */
    private String difficulty;

    /** 默认出题数量（准备页选中岗位后回填） */
    private Integer questionCount;

    /** 出题权重 JSON（job岗位核心/resume简历深挖/weak薄弱点/random随机兜底） */
    private String weights;

    /** 状态:active 启用/inactive 停用 */
    private String status;
}