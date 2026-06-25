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
 * 举报记录表 portal_report
 * 用户举报违规内容（文章/评论/用户等）
 *
 * @author moyun
 */
@Data
@TableName("portal_report")
public class PortalReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 举报ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 举报类型：spam/inappropriate/infringement/fraud/other */
    @NotBlank(message = "举报类型不能为空")
    @Size(max = 32, message = "举报类型长度不能超过32")
    private String reportType;

    /** 举报目标URL（被举报内容的链接） */
    @Size(max = 500, message = "目标URL长度不能超过500")
    private String targetUrl;

    /** 举报描述（问题描述） */
    @NotBlank(message = "问题描述不能为空")
    @Size(max = 2000, message = "问题描述长度不能超过2000")
    private String description;

    /** 联系方式（可选） */
    private String contact;

    /** 图片证据（JSON 数组，最多3张） */
    private String images;

    /** 举报人用户ID（登录用户） */
    private Long userId;

    /** 举报人用户名（冗余，便于后台展示） */
    private String username;

    /** 举报人IP */
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
