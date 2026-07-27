package com.moyun.portal.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.moyun.core.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专栏-文章关联
 *
 * @author moyun
 */
@Data
@TableName("portal_column_article")
public class PortalColumnArticle extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 专栏ID */
    private Long columnId;

    /** 文章ID */
    private Long articleId;

    /** 专栏内顺序 */
    private Integer sortOrder;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    // BaseEntity 公共字段对应列在 portal_column_article 表中不存在，排除 MyBatis-Plus 映射
    @TableField(exist = false)
    private String createBy;
    @TableField(exist = false)
    private LocalDateTime createTime;
    @TableField(exist = false)
    private String updateBy;
    @TableField(exist = false)
    private LocalDateTime updateTime;
    @TableField(exist = false)
    private String remark;

    // 覆盖 BaseEntity 的 delFlag：本表无 del_flag 列（迁移脚本排除），保持物理删除（toggle/流水语义）
    @TableField(exist = false)
    private String delFlag;
}
