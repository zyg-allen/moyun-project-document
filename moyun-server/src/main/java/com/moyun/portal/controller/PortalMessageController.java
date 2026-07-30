package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.MessageSessionVO;
import com.moyun.ext.cms.domain.vo.MessageVO;
import com.moyun.ext.cms.service.IMessageService;
import com.moyun.portal.domain.dto.MessageSendDTO;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 私信 Controller（门户端）
 * <p>
 * 提供会话列表、历史消息、发送消息、标记已读、未读数等接口，所有接口均需登录。
 *
 * @author moyun
 */
@Tag(name = "私信", description = "私信会话与消息接口")
@RestController
@RequestMapping("/portal/message")
public class PortalMessageController extends BaseController {

    @Autowired
    private IMessageService messageService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    /** 门户端发件人类型固定为 portal */
    private static final String SENDER_TYPE = "portal";

    /**
     * 我的会话列表
     */
    @Operation(summary = "我的会话列表", description = "分页查询当前用户参与的私信会话，按最后消息时间倒序")
    @GetMapping("/sessions")
    public AjaxResult listSessions(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<MessageSessionVO> page = messageService.listSessions(userId, SENDER_TYPE, query);
        return AjaxResult.success(page);
    }

    /**
     * 历史消息
     */
    @Operation(summary = "历史消息", description = "分页查询指定会话的历史消息（仅会话成员可查）")
    @GetMapping("/{sessionId}/history")
    public AjaxResult listHistory(@Parameter(description = "会话ID") @PathVariable("sessionId") Long sessionId, PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<MessageVO> page = messageService.listHistory(sessionId, userId, SENDER_TYPE, query);
        return AjaxResult.success(page);
    }

    /**
     * 发送消息
     */
    @Operation(summary = "发送消息", description = "向指定用户发送私信；receiverType 留空默认 portal，显式传 sys 可给管理员发")
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody MessageSendDTO dto) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        // 接收者类型默认 portal；显式传 sys 才给管理员发，避免误发
        String receiverType = (dto.getReceiverType() == null || dto.getReceiverType().isEmpty())
                ? "portal" : dto.getReceiverType();
        MessageVO vo = messageService.sendMessage(userId, SENDER_TYPE, dto.getReceiverId(), receiverType, dto.getContent(), dto.getMsgType());
        return AjaxResult.success(vo);
    }

    /**
     * 标记会话已读
     */
    @Operation(summary = "标记会话已读", description = "清零当前用户在该会话的未读数，并置接收消息为已读")
    @PutMapping("/session/{id}/read")
    public AjaxResult markSessionRead(@Parameter(description = "会话ID") @PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        boolean ok = messageService.markSessionRead(id, userId, SENDER_TYPE);
        return ok ? AjaxResult.success() : AjaxResult.error("会话不存在或无权操作");
    }

    /**
     * 总未读数
     */
    @Operation(summary = "总未读数", description = "查询当前用户的私信总未读数")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(messageService.getUnreadCount(userId, SENDER_TYPE));
    }

    /**
     * 按对方用户ID获取或创建会话
     * 用于"作者主页发起新私信"等场景：尚未有任何消息往来时仍能进入聊天页。幂等，已有会话直接返回。
     * peerType 留空默认 portal。
     */
    @Operation(summary = "按对方用户ID获取或创建会话", description = "用于作者主页发起新私信；幂等，已有会话直接返回")
    @GetMapping("/session/with/{userId}")
    public AjaxResult getOrCreateSessionWithUser(
            @Parameter(description = "对方用户ID") @PathVariable("userId") Long userId,
            @Parameter(description = "对方用户类型 portal/sys，留空默认 portal") @RequestParam(value = "peerType", required = false) String peerType) {
        Long currentUserId = currentUserId();
        if (currentUserId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (currentUserId.equals(userId) && (peerType == null || SENDER_TYPE.equals(peerType))) {
            return AjaxResult.error("不能给自己发私信");
        }
        String pt = (peerType == null || peerType.isEmpty()) ? SENDER_TYPE : peerType;
        MessageSessionVO vo = messageService.getOrCreateSessionVO(currentUserId, SENDER_TYPE, userId, pt);
        return AjaxResult.success(vo);
    }
}
