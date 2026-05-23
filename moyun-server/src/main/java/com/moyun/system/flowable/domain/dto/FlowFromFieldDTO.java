package com.moyun.system.flowable.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程表单字段传输对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "流程表单字段传输对象")
public class FlowFromFieldDTO {

    @Schema(description = "字段名称")
    private String name;

    @Schema(description = "字段值")
    private String value;
}