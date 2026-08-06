package com.moyun.ext.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 对话历史实体类
 *
 * @author laomao
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("chat_history")
public class ChatHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户消息
     */
    private String userMessage;
    
    /**
     * 助手回复
     */
    private String assistantMessage;
    
    /**
     * Token消耗
     */
    private Integer tokensUsed;
    
    /**
     * 检索结果JSON
     */
    private String retrievalResults;
    
    /**
     * 检索命中数
     */
    private Integer retrievalCount;
    
    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
