package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.common.ListResponse;
import com.moyun.ext.ai.dto.ConversationCreateRequest;
import com.moyun.ext.ai.dto.ConversationUpdateTitleRequest;
import com.moyun.ext.ai.entity.Conversation;
import com.moyun.ext.ai.entity.ConversationMessage;
import com.moyun.ext.ai.service.ConversationService;
import com.moyun.ext.ai.vo.ConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "对话会话管理")
@RestController
@RequestMapping("/cms/ai/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Operation(summary = "创建新会话", description = "为指定智能体创建新的对话会话")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermi('cms:ai:conversation:add')")
    public AjaxResult create(
            @RequestBody ConversationCreateRequest request) {
        try {
            Conversation conversation = conversationService.createConversation(
                request.getAgentId(),
                null
            );

            log.info("创建会话成功 - ID: {}, AgentID: {}", conversation.getId(), request.getAgentId());
            return AjaxResult.success("创建成功", conversation);

        } catch (Exception e) {
            log.error("创建会话失败 - AgentID: {}", request.getAgentId(), e);
            return AjaxResult.error("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取会话列表", description = "查询指定智能体的所有会话")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:conversation:list')")
    public AjaxResult list(
            @RequestParam Long agentId,
            @RequestParam(required = false) String userId) {
        try {
            List<Conversation> conversations = conversationService.listByAgentId(agentId, userId);

            List<ConversationVO> conversationVOList = conversations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

            log.debug("查询会话列表成功 - AgentID: {}, 数量: {}", agentId, conversations.size());
            return AjaxResult.success(new ListResponse<>(conversationVOList));

        } catch (Exception e) {
            log.error("获取会话列表失败 - AgentID: {}", agentId, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取会话历史消息", description = "获取指定会话的所有历史消息")
    @GetMapping("/{conversationId}/messages")
    @PreAuthorize("@ss.hasPermi('cms:ai:conversation:query')")
    public AjaxResult getMessages(
            @PathVariable Long conversationId) {
        try {
            List<ConversationMessage> messages = conversationService.getMessages(conversationId);

            log.debug("查询历史消息成功 - ConversationID: {}, 数量: {}", conversationId, messages.size());
            return AjaxResult.success(new ListResponse<>(messages));

        } catch (Exception e) {
            log.error("获取历史消息失败 - ConversationID: {}", conversationId, e);
            return AjaxResult.error("获取失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除会话", description = "删除指定会话及其历史消息")
    @DeleteMapping("/{conversationId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:conversation:remove')")
    public AjaxResult delete(@PathVariable Long conversationId) {
        try {
            conversationService.deleteConversation(conversationId);

            log.info("删除会话成功 - ID: {}", conversationId);
            return AjaxResult.success("删除成功");

        } catch (Exception e) {
            log.error("删除会话失败 - ID: {}", conversationId, e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新会话标题", description = "修改指定会话的标题")
    @PutMapping("/{conversationId}/title")
    @PreAuthorize("@ss.hasPermi('cms:ai:conversation:edit')")
    public AjaxResult updateTitle(
            @PathVariable Long conversationId,
            @RequestBody ConversationUpdateTitleRequest request) {
        try {
            conversationService.updateTitle(conversationId, request.getTitle());

            log.info("更新会话标题成功 - ID: {}, 新标题: {}", conversationId, request.getTitle());
            return AjaxResult.success("更新成功");

        } catch (Exception e) {
            log.error("更新标题失败 - ID: {}", conversationId, e);
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }

    private ConversationVO convertToVO(Conversation conversation) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setAgentId(conversation.getAgentId());
        vo.setAgentName(null);
        vo.setTitle(conversation.getTitle());
        vo.setMessageCount(conversation.getMessageCount());
        vo.setCreateTime(conversation.getCreateTime());
        vo.setUpdateTime(conversation.getUpdateTime());
        return vo;
    }
}
