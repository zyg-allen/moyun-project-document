package com.moyun.agent.dto;

import com.moyun.agent.vo.ConversationVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话列表响应DTO
 *
 * <p>包含会话列表数据</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationListResponse {

    /**
     * 会话列表
     */
    private List<ConversationVO> conversations;

    /**
     * 总数量
     */
    private Integer total;
}
