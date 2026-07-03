package com.moyun.ext.cms.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 知识图谱节点（3.5）
 *
 * @author moyun
 */
@Data
public class KnowledgeNodeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 标签ID */
    private Long tagId;

    /** 标签名 */
    private String name;

    /** 关联题目数（节点大小依据） */
    private Integer questionCount;

    /** 该维度下题目总数（用户视角，未传 userId 时等于 questionCount） */
    private Integer total;

    /** 该维度下用户已通过题目数 */
    private Integer solved;

    /** 掌握度 0-100，未传 userId 时为 0 */
    private Integer mastery;
}
