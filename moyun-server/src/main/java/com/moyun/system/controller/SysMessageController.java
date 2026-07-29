package com.moyun.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.domain.vo.MessageSessionVO;
import com.moyun.ext.cms.domain.vo.MessageVO;
import com.moyun.ext.cms.service.IMessageService;
import com.moyun.portal.domain.dto.MessageSendDTO;
import com.moyun.util.security.SecurityUtils;

/**
 * 私信 Controller（后台管理端）
 *
 * <p>管理员接收门户用户私信、回复私信的入口，与门户端 {@link com.moyun.portal.controller.PortalMessageController}
 * 共用同一套 Service/数据模型，通过 user_type='sys' 区分管理员体系。</p>
 *
 * <p>归属调整：原 {@code com.moyun.ext.cms.controller.SysMessageController} 已迁移至此
 * （与 {@link SysNotificationController} 同级），私信是全局系统级能力，不应归属 CMS 业务模块。
 * Service/VO 暂留在 ext.cms 包，因门户端 PortalMessageController 亦依赖，全量迁移需同步调整门户端。</p>
 *
 * <p>权限：私信中心相关操作统一使用 system:message:* 权限码；
 * 发送消息要求登录态即可（回复是管理员高频操作，不再叠加细粒度权限避免阻碍工作流）。</p>
 *
 * <p>前后台权限区分：
 * <ul>
 *   <li>后台私信（本类，/system/message，system:message:*）：管理员查看会话、回复门户用户</li>
 *   <li>门户私信（{@link com.moyun.portal.controller.PortalMessageController}，/portal/message）：
 *       门户用户发起私信、查看自己的会话，走 portal 鉴权体系</li>
 * </ul>
 * 两者共用 {@link IMessageService}，通过 user_type 区分。</p>
 *
 * @author moyun
 */
@Tag(name = "后台私信管理", description = "管理员私信会话与消息接口")
@RestController
@RequestMapping("/system/message")
public class SysMessageController extends BaseController {

    @Autowired
    private IMessageService messageService;

    /** 管理端发件人类型固定为 sys */
    private static final String SENDER_TYPE = "sys";

    private Long currentUserId() {
        return SecurityUtils.getUserId();
    }

    /**
     * 我的会话列表
     */
    @Operation(summary = "我的会话列表", description = "分页查询当前管理员参与的私信会话，按最后消息时间倒序")
    @PreAuthorize("@ss.hasPermi('system:message:list')")
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
    @PreAuthorize("@ss.hasPermi('system:message:query')")
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
     * 发送消息（回复门户用户）
     */
    @Operation(summary = "发送消息", description = "管理员向指定用户发送私信；receiverType 留空默认 portal")
    @PreAuthorize("@ss.hasPermi('system:message:send')")
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody MessageSendDTO dto) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        // 管理员回复默认发给门户用户，显式传 sys 才给其他管理员发
        String receiverType = (dto.getReceiverType() == null || dto.getReceiverType().isEmpty())
                ? "portal" : dto.getReceiverType();
        MessageVO vo = messageService.sendMessage(userId, SENDER_TYPE, dto.getReceiverId(), receiverType, dto.getContent(), dto.getMsgType());
        return AjaxResult.success(vo);
    }

    /**
     * 标记会话已读
     */
    @Operation(summary = "标记会话已读", description = "清零当前管理员在该会话的未读数，并置接收消息为已读")
    @PreAuthorize("@ss.hasPermi('system:message:query')")
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
    @Operation(summary = "总未读数", description = "查询当前管理员的私信总未读数（用于头部铃铛徽章）")
    @PreAuthorize("@ss.hasPermi('system:message:list')")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(messageService.getUnreadCount(userId, SENDER_TYPE));
    }
}
