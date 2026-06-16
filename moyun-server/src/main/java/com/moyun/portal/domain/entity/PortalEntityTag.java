package com.moyun.portal.domain.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import com.moyun.core.base.BaseEntity;

@Data
@TableName("portal_entity_tag")
public class PortalEntityTag extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotNull(message = "标签ID不能为空")
    private Long tagId;

    @NotBlank(message = "实体类型不能为空")
    @Size(min = 0, max = 50, message = "实体类型长度不能超过50个字符")
    private String entityType;

    @NotNull(message = "实体ID不能为空")
    private Long entityId;

    private Integer sort;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public PortalEntityTag() {
    }

    public PortalEntityTag(Long id) {
        this.id = id;
    }
}
