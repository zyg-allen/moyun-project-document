package com.moyun.portal.domain.entity;

import java.io.Serial;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

/**
 * 面试题目测试用例（v6.3 OJ 判题系统）
 * <p>
 * 算法题的判题依据：每条记录包含标准输入与期望输出，判题器依次运行用户代码并比对输出。
 * 样例用例（is_sample=1）会下发给前端展示，便于用户理解题意；非样例用例为隐藏判题用例。
 *
 * @author moyun
 */
@Data
@TableName("portal_interview_question_test_case")
public class PortalInterviewQuestionTestCase extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目ID */
    private Long questionId;

    /** 标准输入（多行文本，运行时通过 stdin 传入） */
    private String input;

    /** 期望输出（多行文本，运行时与 stdout 比对） */
    private String expectedOutput;

    /** 是否样例：1=样例（前端展示）/ 0=隐藏（仅判题使用） */
    private Integer isSample;

    /** 用例排序（按升序执行与展示） */
    private Integer orderNum;

    /** 用例说明（可选，前端样例展示时附带说明） */
    private String explanation;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // === BaseEntity 中不需要持久化的字段 ===
    @TableField(exist = false)
    private String createBy;

    @TableField(exist = false)
    private String updateBy;

    @TableField(exist = false)
    private String remark;
}
