package com.moyun.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 话题观点发布数据传输对象
 *
 * <p>用于 PortalTopicController.createPost 接口参数接收，替代旧的 {@code Map<String, Object>}，
 * 通过 JSR-303 校验保证参数合法性。</p>
 *
 * @author moyun
 */
@Data
@Schema(description = "话题观点发布DTO")
public class TopicPostCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 观点内容（Markdown，最长 5000 字符）
     */
    @NotBlank(message = "观点内容不能为空")
    @Size(max = 5000, message = "观点内容长度不能超过5000个字符")
    @Schema(description = "观点内容（Markdown）", example = "我认为这个话题很有价值...")
    private String content;

    /**
     * 图片 URL 列表（最多 9 张），由 Service 层序列化为 JSON 字符串持久化
     */
    @Schema(description = "图片URL列表（最多9张）", example = "[\"https://example.com/1.jpg\"]")
    private List<String> images;

    /**
     * 父观点 ID（楼中楼回复时传入，NULL 为一级观点）
     */
    @Schema(description = "父观点ID（楼中楼回复时传入）", example = "1")
    private Long parentPostId;

    /**
     * 回复的目标用户 ID（楼中楼回复时传入）
     */
    @Schema(description = "回复的目标用户ID", example = "2")
    private Long replyToUserId;
}
