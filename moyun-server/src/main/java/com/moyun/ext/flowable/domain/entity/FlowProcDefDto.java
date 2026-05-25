package com.moyun.ext.flowable.domain.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程定义DTO
 *
 * @author ruoyi
 */
@Data
public class FlowProcDefDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String key;

    private Integer version;

    private String category;

    private String deploymentId;

    private String resourceName;

    private String diagramResourceName;

    private String description;

    private Boolean isSuspended;
}
