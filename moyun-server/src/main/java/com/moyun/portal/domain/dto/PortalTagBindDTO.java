package com.moyun.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "绑定标签到实体DTO")
public class PortalTagBindDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "entityType不能为空")
    @Schema(description = "实体类型，如 interview_question, interview_experience, article, book 等")
    private String entityType;

    @NotNull(message = "entityId不能为空")
    @Schema(description = "实体ID")
    private Long entityId;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "标签名称列表（如果标签不存在则自动创建）")
    private List<String> tagNames;

    @Schema(description = "所属模块，如 interview/article/book/common")
    private String module;
}
