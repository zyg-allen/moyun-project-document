package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 意见反馈表 portal_feedback
 * 用户提交产品建议、Bug反馈、体验问题等
 *
 * @author moyun
 */
@Data
@TableName("portal_feedback")
public class PortalFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 反馈ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 反馈类型：suggestion=功能建议/bug=Bug反馈/experience=体验问题/other=其他 */
    @NotBlank(message = "反馈类型不能为空")
    @Size(max = 32, message = "反馈类型长度不能超过32")
    private String feedbackType;

    /** 反馈主题 */
    @Size(max = 200, message = "反馈主题长度不能超过200")
    private String subject;

    /** 反馈详细描述 */
    @NotBlank(message = "反馈描述不能为空")
    @Size(max = 2000, message = "反馈描述长度不能超过2000")
    private String description;

    /** 联系方式（可选） */
    private String contact;

    /** 反馈人用户ID（登录用户） */
    private Long userId;

    /** 反馈人用户名（冗余） */
    private String username;

    /** 反馈人IP */
    private String ip;

    /** 处理状态：pending=待处理/processing=处理中/resolved=已解决/rejected=已驳回 */
    private String status;

    /** 处理人（后台管理员用户名） */
    private String handler;

    /** 处理结果说明 */
    private String handleResult;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handleTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;
}
