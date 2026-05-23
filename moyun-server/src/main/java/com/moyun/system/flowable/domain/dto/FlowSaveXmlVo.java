package com.moyun.system.flowable.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 流程定义传输对象
 *
 * @author Tony
 * @date 2021-04-03
 */
@Data
@Schema(description = "流程定义传输对象")
public class FlowSaveXmlVo {

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程分类")
    private String category;

    @Schema(description = "xml文件")
    private String xml;
}
