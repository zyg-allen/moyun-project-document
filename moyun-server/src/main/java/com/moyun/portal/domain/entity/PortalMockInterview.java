package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

/**
 * 模拟面试会话 portal_mock_interview
 * <p>
 * 业务字段 create_time / update_time 复用 BaseEntity.createTime / updateTime（驼峰映射一致），
 * 其余 BaseEntity 公共字段 createBy/updateBy/remark 在本表不存在，需排除映射。
 *
 * @author moyun
 */
@Data
@TableName("portal_mock_interview")
public class PortalMockInterview extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 面试用户ID */
    private Long userId;

    /** 面试岗位 */
    private String position;

    /** 面试场景（对应题目分类） */
    private String scene;

    /** 状态 in_progress/finished */
    private String status;

    /** 题目总数 */
    private Integer totalQa;

    /** 面试总分（0-100，结束面试时计算） */
    private Integer score;

    /** AI 生成的面试总结 */
    private String summary;

    // createTime / updateTime 复用父类字段（对应 create_time / update_time 列），不重声明
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private String remark;
}
