package com.moyun.portal.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 私信发送 DTO
 *
 * @author moyun
 */
@Data
@Schema(description = "私信发送DTO")
public class MessageSendDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 接收者用户ID */
    @Schema(description = "接收者用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "接收者不能为空")
    private Long receiverId;

    /** 消息内容 */
    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容长度不能超过5000个字符")
    private String content;

    /** 消息类型 text/image/file */
    @Schema(description = "消息类型 text/image/file", example = "text")
    private String msgType;
}
