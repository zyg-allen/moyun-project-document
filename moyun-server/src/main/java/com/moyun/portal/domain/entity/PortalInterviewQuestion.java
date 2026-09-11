package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.common.annotation.Excel;
import com.moyun.core.base.BaseEntity;

/**
 * 面试题目对象 portal_interview_question
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_question")
public class PortalInterviewQuestion extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    @Excel(name = "ID", cellType = Excel.ColumnType.NUMERIC, type = Excel.Type.EXPORT)
    private Long id;

    /**
     * 题目标题
     */
    @NotBlank(message = "题目标题不能为空")
    @Size(min = 0, max = 500, message = "题目标题长度不能超过500个字符")
    @Excel(name = "题目标题")
    private String title;

    /**
     * 题目描述
     */
    @Excel(name = "题目描述")
    private String description;

    /**
     * 难度:easy,medium,hard
     */
    @Size(min = 0, max = 20, message = "难度长度不能超过20个字符")
    @Excel(name = "难度", readConverterExp = "easy=简单,medium=中等,hard=困难")
    private String difficulty;

    /**
     * 所属岗位模板ID（portal_job_template.id，v11.x 智能出题）
     */
    private Long jobTemplateId;

    /**
     * 分类ID
     */
    @Excel(name = "分类ID", cellType = Excel.ColumnType.NUMERIC)
    private Long categoryId;

    /**
     * 标签，逗号分隔
     */
    @Size(min = 0, max = 500, message = "标签长度不能超过500个字符")
    @Excel(name = "标签")
    private String tags;

    /**
     * 公司，逗号分隔
     */
    @Size(min = 0, max = 500, message = "公司长度不能超过500个字符")
    @Excel(name = "公司")
    private String companies;

    /**
     * 通过率
     */
    @Excel(name = "通过率", cellType = Excel.ColumnType.NUMERIC)
    private BigDecimal acceptanceRate;

    /**
     * 提交次数
     */
    @Excel(name = "提交次数", cellType = Excel.ColumnType.NUMERIC)
    private Long submissionCount;

    /**
     * 点赞数
     */
    @Excel(name = "点赞数", cellType = Excel.ColumnType.NUMERIC)
    private Long likeCount;

    /**
     * 提示
     */
    @Excel(name = "提示")
    private String hint;

    /**
     * 参考答案
     */
    @Excel(name = "参考答案")
    private String solution;

    /**
     * 排序
     */
    @Excel(name = "排序", cellType = Excel.ColumnType.NUMERIC)
    private Integer sort;

    /**
     * 状态:draft 草稿/published 已发布/archived 已归档
     * 注意：历史注释为 active/inactive（启停语义），实际前端使用 draft/published/archived（内容生命周期语义），
     *       已修正注释与前端保持一致；存量数据兼容。
     */
    @Size(min = 0, max = 20, message = "状态长度不能超过20个字符")
    @Excel(name = "状态", readConverterExp = "draft=草稿,published=已发布,archived=已归档")
    private String status;

    // ============ 结构化字段（v6.3 题目结构化） ============

    /**
     * 题目类型：bagwen 八股 / algorithm 算法 / system_design 系统设计 / project 项目 / hr HR
     * 用于题库按类型筛选与练习区交互模式切换（如 algorithm 默认代码作答，bagwen 默认文本作答）
     */
    @Size(min = 0, max = 50, message = "题目类型长度不能超过50个字符")
    @Excel(name = "题目类型", readConverterExp = "bagwen=八股,algorithm=算法,system_design=系统设计,project=项目,hr=HR")
    private String questionType;

    /**
     * 考察点列表（JSON 数组字符串，如 ["TCP 三次握手","拥塞控制"]）
     * 详情页展示，便于面试者快速对齐面试官关注点
     */
    @Excel(name = "考察点")
    private String examinePoints;

    /**
     * 答题大纲（Markdown 结构化答题思路，如步骤/框架）
     */
    @Excel(name = "答题大纲")
    private String answerOutline;

    /**
     * 评分标准（JSON 数组字符串，每项含 dimension/weight/description，用于自评与精选笔记筛选）
     */
    @Excel(name = "评分标准")
    private String scoringCriteria;

    /**
     * 官方参考答案（Markdown），区别于旧字段 solution（纯文本/代码片段）
     * solution 保留用于代码题的参考代码；referenceAnswer 用于八股/设计/项目/HR 类题目的完整答案
     */
    @Excel(name = "官方参考答案")
    private String referenceAnswer;

    /**
     * 前置题目 ID，逗号分隔（用于学习路径推荐：未通过前置题则提示先做前置题）
     */
    @Size(min = 0, max = 500, message = "前置题目ID长度不能超过500个字符")
    @Excel(name = "前置题目ID")
    private String prerequisiteIds;

    // ============ 练习模式扩展字段（v10.6 题库重构·阶段2） ============

    /**
     * 练习模式：reading 展示阅读 / choice 选择题 / coding 编程题
     * 与 questionType 正交：questionType 描述题目内容分类，practiceMode 描述作答方式
     */
    @Size(min = 0, max = 20, message = "练习模式长度不能超过20个字符")
    @Excel(name = "练习模式", readConverterExp = "reading=展示阅读,choice=选择题,coding=编程题")
    private String practiceMode;

    /**
     * 选择题选项（JSON 数组字符串，如 [{"label":"A","text":"...","is_correct":false}]）
     * 仅 practice_mode=choice 时有值，做题页前端渲染选项
     */
    @Excel(name = "选择题选项")
    private String options;

    /**
     * 正确答案（选择题：选项 label 如 B；编程题：null，靠测试用例判定）
     */
    @Size(min = 0, max = 50, message = "正确答案长度不能超过50个字符")
    @Excel(name = "正确答案")
    private String correctAnswer;

    /**
     * 题目解析（做题后展示，区别于 referenceAnswer 参考答案）
     */
    @Excel(name = "题目解析")
    private String analysis;

    /**
     * 知识点标签，逗号分隔（用于错题本/学习路径聚类）
     */
    @Size(min = 0, max = 500, message = "知识点标签长度不能超过500个字符")
    @Excel(name = "知识点标签")
    private String knowledgeTags;

    public PortalInterviewQuestion() {
    }

    public PortalInterviewQuestion(Long id) {
        this.id = id;
    }
}
