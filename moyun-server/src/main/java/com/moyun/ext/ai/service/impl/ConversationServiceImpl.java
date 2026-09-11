package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.entity.Conversation;
import com.moyun.ext.ai.entity.ConversationMessage;
import com.moyun.ext.ai.enums.ConversationRole;
import com.moyun.ext.ai.mapper.ConversationMapper;
import com.moyun.ext.ai.mapper.ConversationMessageMapper;
import com.moyun.ext.ai.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话会话服务实现
 */
@Slf4j
@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {

    @Autowired
    private ConversationMessageMapper messageMapper;

    @Override
    public Conversation createConversation(Long agentId, String userId) {
        Conversation conversation = new Conversation();
        conversation.setAgentId(agentId);
        conversation.setUserId(userId);
        conversation.setTitle("新对话");
        conversation.setMessageCount(0);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());

        this.save(conversation);
        log.info("创建新会话: conversationId={}, agentId={}", conversation.getId(), agentId);

        return conversation;
    }

    @Override
    public List<Conversation> listByAgentId(Long agentId, String userId) {
        QueryWrapper<Conversation> wrapper = new QueryWrapper<>();
        wrapper.eq("agent_id", agentId);
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        wrapper.orderByDesc("update_time");

        return this.list(wrapper);
    }

    @Override
    @Transactional
    public void addMessage(Long conversationId, String role, String content, String referenceSources) {
        // 保存消息
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setReferenceSources(referenceSources);
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insert(message);

        // 更新会话的消息数量和更新时间
        Conversation conversation = this.getById(conversationId);
        if (conversation != null) {
            int oldMessageCount = conversation.getMessageCount();
            conversation.setMessageCount(oldMessageCount + 1);
            conversation.setUpdateTime(LocalDateTime.now());

            // 自动标题：仅当标题仍为默认值"新对话"时，才用最新用户消息更新标题；
            // 用户手动重命名过（updateTitle）后标题不再是默认值，不再覆盖用户的命名。
            if (ConversationRole.USER.getCode().equals(role) && !"你好".equals(content.trim())
                    && "新对话".equals(conversation.getTitle())) {
                String title = generateTitle(content);
                conversation.setTitle(title);
                log.info("更新会话标题为最新消息: conversationId={}, title={}", conversationId, title);
            }

            this.updateById(conversation);
        }

        log.debug("添加消息到会话: conversationId={}, role={}", conversationId, role);
    }

    @Override
    public List<ConversationMessage> getMessages(Long conversationId) {
        QueryWrapper<ConversationMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("conversation_id", conversationId);
        wrapper.orderByAsc("create_time");

        return messageMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId) {
        // 删除消息（外键级联删除）
        QueryWrapper<ConversationMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("conversation_id", conversationId);
        messageMapper.delete(wrapper);

        // 逻辑删除会话。
        // 注意：removeById(id) 走逻辑删除时不携带实体，MetaObjectHandler 的 updateFill
        // 不会触发，update_time 会被绑定为 null，而该列有非空约束导致 SQL 报错。
        // 故显式 UPDATE 同时设置 update_time 与 deleted。
        this.update(new LambdaUpdateWrapper<Conversation>()
                .set(Conversation::getUpdateTime, LocalDateTime.now())
                .set(Conversation::getDeleted, true)
                .eq(Conversation::getId, conversationId));

        log.info("删除会话: conversationId={}", conversationId);
    }

    @Override
    public void updateTitle(Long conversationId, String title) {
        Conversation conversation = this.getById(conversationId);
        if (conversation != null) {
            conversation.setTitle(title);
            this.updateById(conversation);
            log.info("更新会话标题: conversationId={}, title={}", conversationId, title);
        }
    }

    /**
     * 生成会话标题
     * 智能截取合适长度，保持语义完整
     */
    private String generateTitle(String content) {
        if (content == null || content.isEmpty()) {
            return "新对话";
        }

        // 去除首尾空格和换行
        String text = content.trim();

        // 如果内容很短，直接返回
        if (text.length() <= 15) {
            return text;
        }

        // 优先在标点符号处截断（15-30字符范围内）
        String[] delimiters = {"？", "?", "。", "！", "!", "，", ",", "；", ";"};
        for (int i = 15; i <= Math.min(30, text.length()); i++) {
            String currentChar = text.substring(i - 1, i);
            for (String delimiter : delimiters) {
                if (currentChar.equals(delimiter)) {
                    return text.substring(0, i);
                }
            }
        }

        // 如果没有合适的标点，在20字符处截断并加省略号
        if (text.length() > 20) {
            return text.substring(0, 20) + "...";
        }

        return text;
    }
}
