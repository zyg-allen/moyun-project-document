package com.moyun.agent.dto;

import com.moyun.agent.entity.DocumentSegment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库分片列表响应DTO
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSegmentListResponse {

    /**
     * 分片列表
     */
    private List<DocumentSegment> segments;

    /**
     * 总数量
     */
    private Integer total;
}
