package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识图谱边：标签共现关系（3.5）
 *
 * @author moyun
 */
@Data
public class KnowledgeEdgeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 起点标签ID */
    private Long source;

    /** 终点标签ID */
    private Long target;

    /** 共现次数（共同出现的题目数） */
    private Integer weight;
}
