package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

/**
 * 模拟面试问答 portal_mock_interview_qa
 * <p>
 * 业务字段 create_time 复用 BaseEntity.createTime，其余公共字段 createBy/updateBy/updateTime/remark 不存在，需排除。
 *
 * @author moyun
 */
@Data
@TableName("portal_mock_interview_qa")
public class PortalMockInterviewQA extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 面试会话ID */
    private Long interviewId;

    /** 关联题目ID（portal_interview_question.id） */
    private Long questionId;

    /** 题目序号（从 0 开始） */
    private Integer questionIdx;

    /** 面试问题（快照自题目标题） */
    private String question;

    /** 用户回答 */
    private String userAnswer;

    /** AI 反馈（规则化生成） */
    private String aiFeedback;

    /** 本题评分（0-100） */
    private Integer score;

    // createTime 复用父类字段（对应 create_time 列），不重声明
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private java.time.LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;
}
