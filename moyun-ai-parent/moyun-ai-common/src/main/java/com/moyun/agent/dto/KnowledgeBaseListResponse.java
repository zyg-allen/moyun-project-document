package com.moyun.agent.dto;

import com.moyun.agent.vo.KnowledgeBaseVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库列表响应DTO
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseListResponse {

    /**
     * 知识库列表
     */
    private List<KnowledgeBaseVO> knowledgeBases;

    /**
     * 总数量
     */
    private Integer total;
}
