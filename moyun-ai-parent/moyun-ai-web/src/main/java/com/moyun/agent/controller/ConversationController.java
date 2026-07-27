package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.common.ListResponse;
import com.moyun.agent.dto.ConversationCreateRequest;
import com.moyun.agent.dto.ConversationUpdateTitleRequest;
import com.moyun.agent.entity.Conversation;
import com.moyun.agent.entity.ConversationMessage;
import com.moyun.agent.service.ConversationService;
import com.moyun.agent.vo.ConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话会话管理控制器
 *
 * <p>提供会话的创建、查询、删除等管理功能</p>
 *
 * @author laomao
 * @time 2025/11/23
 */
@Slf4j
@Tag(name = "对话会话管理")
@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * 创建新会话
     *
     * <p>为指定的智能体创建一个新的对话会话</p>
     *
     * @param request 创建请求，包含智能体ID
     * @return 创建的会话信息
     */
    @Operation(summary = "创建新会话", description = "为指定智能体创建新的对话会话")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Conversation>> create(
            @RequestBody ConversationCreateRequest request) {
        try {
            // 创建会话
            Conversation conversation = conversationService.createConversation(
                request.getAgentId(),
                null  // userId暂时不使用
            );

            log.info("创建会话成功 - ID: {}, AgentID: {}", conversation.getId(), request.getAgentId());
            return ResponseEntity.ok(ApiResponse.success("创建成功", conversation));

        } catch (Exception e) {
            log.error("创建会话失败 - AgentID: {}", request.getAgentId(), e);
            return ResponseEntity.ok(ApiResponse.error("创建失败: " + e.getMessage()));
        }
    }

    /**
     * 获取智能体的会话列表
     *
     * <p>查询指定智能体下的所有会话</p>
     *
     * @param agentId 智能体ID
     * @param userId 用户ID（可选）
     * @return 会话列表
     */
    @Operation(summary = "获取会话列表", description = "查询指定智能体的所有会话")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<ListResponse<ConversationVO>>> list(
            @RequestParam Long agentId,
            @RequestParam(required = false) String userId) {
        try {
            // 查询会话列表
            List<Conversation> conversations = conversationService.listByAgentId(agentId, userId);

            // 转换为VO
            List<ConversationVO> conversationVOList = conversations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

            log.debug("查询会话列表成功 - AgentID: {}, 数量: {}", agentId, conversations.size());
            return ResponseEntity.ok(ApiResponse.success(new ListResponse<>(conversationVOList)));

        } catch (Exception e) {
            log.error("获取会话列表失败 - AgentID: {}", agentId, e);
            return ResponseEntity.ok(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }

    /**
     * 获取会话的历史消息
     *
     * <p>获取指定会话的所有历史消息记录</p>
     *
     * @param conversationId 会话ID
     * @return 消息列表
     */
    @Operation(summary = "获取会话历史消息", description = "获取指定会话的所有历史消息")
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ListResponse<ConversationMessage>>> getMessages(
            @PathVariable Long conversationId) {
        try {
            // 查询历史消息
            List<ConversationMessage> messages = conversationService.getMessages(conversationId);

            log.debug("查询历史消息成功 - ConversationID: {}, 数量: {}", conversationId, messages.size());
            return ResponseEntity.ok(ApiResponse.success(new ListResponse<>(messages)));

        } catch (Exception e) {
            log.error("获取历史消息失败 - ConversationID: {}", conversationId, e);
            return ResponseEntity.ok(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }

    /**
     * 删除会话
     *
     * <p>删除指定的会话及其所有历史消息</p>
     *
     * @param conversationId 会话ID
     * @return 删除结果
     */
    @Operation(summary = "删除会话", description = "删除指定会话及其历史消息")
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long conversationId) {
        try {
            // 删除会话
            conversationService.deleteConversation(conversationId);

            log.info("删除会话成功 - ID: {}", conversationId);
            return ResponseEntity.ok(ApiResponse.success("删除成功"));

        } catch (Exception e) {
            log.error("删除会话失败 - ID: {}", conversationId, e);
            return ResponseEntity.ok(ApiResponse.error("删除失败: " + e.getMessage()));
        }
    }

    /**
     * 更新会话标题
     *
     * <p>修改指定会话的标题</p>
     *
     * @param conversationId 会话ID
     * @param request 更新请求，包含新标题
     * @return 更新结果
     */
    @Operation(summary = "更新会话标题", description = "修改指定会话的标题")
    @PutMapping("/{conversationId}/title")
    public ResponseEntity<ApiResponse<Void>> updateTitle(
            @PathVariable Long conversationId,
            @RequestBody ConversationUpdateTitleRequest request) {
        try {
            // 更新标题
            conversationService.updateTitle(conversationId, request.getTitle());

            log.info("更新会话标题成功 - ID: {}, 新标题: {}", conversationId, request.getTitle());
            return ResponseEntity.ok(ApiResponse.success("更新成功"));

        } catch (Exception e) {
            log.error("更新标题失败 - ID: {}", conversationId, e);
            return ResponseEntity.ok(ApiResponse.error("更新失败: " + e.getMessage()));
        }
    }

    /**
     * 转换Conversation实体为VO
     *
     * @param conversation 会话实体
     * @return 会话VO
     */
    private ConversationVO convertToVO(Conversation conversation) {
        ConversationVO vo = new ConversationVO();
        vo.setId(conversation.getId());
        vo.setAgentId(conversation.getAgentId());
        vo.setAgentName(null);  // 如果需要智能体名称，需要关联查询
        vo.setTitle(conversation.getTitle());
        vo.setMessageCount(conversation.getMessageCount());
        vo.setCreateTime(conversation.getCreateTime());
        vo.setUpdateTime(conversation.getUpdateTime());
        return vo;
    }
}
