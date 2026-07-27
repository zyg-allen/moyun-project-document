package com.moyun.agent.dto;

import com.moyun.agent.entity.Agent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体列表响应DTO
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentListResponse {

    /**
     * 智能体列表
     */
    private List<Agent> agents;

    /**
     * 总数量
     */
    private Integer total;
}
