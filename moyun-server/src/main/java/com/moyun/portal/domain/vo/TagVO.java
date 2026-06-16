package com.moyun.portal.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 标签视图对象
 *
 * @author moyun
 */
@Data
@Schema(description = "标签VO")
public class TagVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签ID
     */
    @Schema(description = "标签ID", example = "1")
    private Long id;

    /**
     * 标签名称
     */
    @Schema(description = "标签名称", example = "Java")
    private String name;

    /**
     * 标签别名
     */
    @Schema(description = "标签别名", example = "java")
    private String slug;

    /**
     * 排序
     */
    @Schema(description = "排序", example = "1")
    private Integer sort;

    /**
     * 状态
     */
    @Schema(description = "状态", example = "0")
    private String status;

    /**
     * 所属模块
     */
    @Schema(description = "所属模块", example = "article")
    private String module;

    /**
     * 引用次数
     */
    @Schema(description = "引用次数", example = "10")
    private Long referenceCount;
}
