package com.moyun.ext.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新会话标题请求DTO
 *
 * <p>用于修改会话的标题</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdateTitleRequest {

    /**
     * 新的会话标题
     */
    private String title;
}
