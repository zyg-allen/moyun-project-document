package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 知识图谱聚合数据（3.5）
 *
 * @author moyun
 */
@Data
public class KnowledgeGraphVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点列表 */
    private List<KnowledgeNodeVO> nodes;

    /** 边列表 */
    private List<KnowledgeEdgeVO> edges;

    /** 当前查看掌握度的用户ID（未指定时为 null，表示全局标签云） */
    private Long userId;
}
