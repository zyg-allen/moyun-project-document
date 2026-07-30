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
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.service.IPortalUserService;
import com.moyun.util.security.SecurityUtils;

/**
 * 私信 Controller（后台管理端）
 *
 * <p>管理员接收门户用户私信、回复私信的入口，与门户端 {@link com.moyun.portal.controller.PortalMessageController}
 * 共用同一套 Service/数据模型。</p>
 *
 * <p><b>身份桥接机制</b>：后台管理员发文章时会自动建立 role=admin 的门户影子账户
 * （portal_user.user_id = sys_user.user_id）。当门户用户私信该门户身份（如"墨韵管理员1"）时，
 * 管理员在后台私信中心可通过身份桥接以同一门户身份查看/回复，实现消息可达与闭环回复——
 * 管理员无需登录前台即可收到并回复门户用户的私信。</p>
 *
 * <p>具体逻辑：管理员每次操作私信时，先尝试反查绑定的门户身份（优先 role=admin 的影子账户）。
 * 若存在绑定，则以其门户身份（portal 身份）读写会话——与门户用户看到的是同一条会话，零数据冗余；
 * 若无绑定，回退到 sys 身份（保留原行为，门户用户侧会看到"系统管理员"发来的消息）。</p>
 *
 * <p>归属调整：原 {@code com.moyun.ext.cms.controller.SysMessageController} 已迁移至此
 * （与 {@link SysNotificationController} 同级），私信是全局系统级能力，不应归属 CMS 业务模块。
 * Service/VO 暂留在 ext.cms 包，因门户端 PortalMessageController 亦依赖，全量迁移需同步调整门户端。</p>
 *
 * <p>权限：私信中心相关操作统一使用 system:message:* 权限码；
 * 发送消息要求登录态即可（回复是管理员高频操作，不再叠加细粒度权限避免阻碍工作流）。</p>
 *
 * @author moyun
 */
@Tag(name = "后台私信管理", description = "管理员私信会话与消息接口")
@RestController
@RequestMapping("/system/message")
public class SysMessageController extends BaseController {

    @Autowired
    private IMessageService messageService;

    @Autowired
    private IPortalUserService portalUserService;

    /** sys 体系发件人类型 */
    private static final String TYPE_SYS = "sys";
    /** portal 体系发件人类型 */
    private static final String TYPE_PORTAL = "portal";

    /**
     * 解析当前管理员的消息身份（身份桥接核心）
     *
     * <p>优先使用绑定的门户身份（role=admin 的影子账户），使后台管理员与门户作者共享同一条会话；
     * 无绑定则回退到 sys 身份，保留原有行为。</p>
     *
     * @return 解析后的身份（userId + userType）；未登录返回 null
     */
    private ResolvedIdentity resolveIdentity() {
        Long sysUserId = SecurityUtils.getUserId();
        if (sysUserId == null) {
            return null;
        }
        // 身份桥接：查绑定的门户身份
        PortalUser boundPortal = portalUserService.findBoundPortalIdentity(sysUserId);
        if (boundPortal != null) {
            return new ResolvedIdentity(boundPortal.getId(), TYPE_PORTAL);
        }
        // 无绑定，回退 sys 身份
        return new ResolvedIdentity(sysUserId, TYPE_SYS);
    }

    /**
     * 我的会话列表
     */
    @Operation(summary = "我的会话列表", description = "分页查询当前管理员参与的私信会话，按最后消息时间倒序。已绑定门户身份的管理员会看到以该门户身份收发的会话。")
    @PreAuthorize("@ss.hasPermi('system:message:list')")
    @GetMapping("/sessions")
    public AjaxResult listSessions(PageDomain query) {
        ResolvedIdentity identity = resolveIdentity();
        if (identity == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<MessageSessionVO> page = messageService.listSessions(identity.userId, identity.userType, query);
        return AjaxResult.success(page);
    }

    /**
     * 历史消息
     */
    @Operation(summary = "历史消息", description = "分页查询指定会话的历史消息（仅会话成员可查）")
    @PreAuthorize("@ss.hasPermi('system:message:query')")
    @GetMapping("/{sessionId}/history")
    public AjaxResult listHistory(@Parameter(description = "会话ID") @PathVariable("sessionId") Long sessionId, PageDomain query) {
        ResolvedIdentity identity = resolveIdentity();
        if (identity == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Page<MessageVO> page = messageService.listHistory(sessionId, identity.userId, identity.userType, query);
        return AjaxResult.success(page);
    }

    /**
     * 发送消息（回复门户用户）
     *
     * <p>已绑定门户身份的管理员，回复时以其门户身份发送——门户用户侧看到的回复来自该门户作者（如"墨韵管理员1"），
     * 与门户用户发起的会话是同一条，实现闭环回复。</p>
     */
    @Operation(summary = "发送消息", description = "管理员向指定用户发送私信；receiverType 留空默认 portal。已绑定门户身份的管理员以门户身份发送。")
    @PreAuthorize("@ss.hasPermi('system:message:send')")
    @PostMapping("/send")
    public AjaxResult send(@Valid @RequestBody MessageSendDTO dto) {
        ResolvedIdentity identity = resolveIdentity();
        if (identity == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        // 管理员回复默认发给门户用户，显式传 sys 才给其他管理员发
        String receiverType = (dto.getReceiverType() == null || dto.getReceiverType().isEmpty())
                ? TYPE_PORTAL : dto.getReceiverType();
        MessageVO vo = messageService.sendMessage(identity.userId, identity.userType, dto.getReceiverId(), receiverType, dto.getContent(), dto.getMsgType());
        return AjaxResult.success(vo);
    }

    /**
     * 标记会话已读
     */
    @Operation(summary = "标记会话已读", description = "清零当前管理员在该会话的未读数，并置接收消息为已读")
    @PreAuthorize("@ss.hasPermi('system:message:query')")
    @PutMapping("/session/{id}/read")
    public AjaxResult markSessionRead(@Parameter(description = "会话ID") @PathVariable("id") Long id) {
        ResolvedIdentity identity = resolveIdentity();
        if (identity == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        boolean ok = messageService.markSessionRead(id, identity.userId, identity.userType);
        return ok ? AjaxResult.success() : AjaxResult.error("会话不存在或无权操作");
    }

    /**
     * 总未读数
     */
    @Operation(summary = "总未读数", description = "查询当前管理员的私信总未读数（用于头部铃铛徽章）。已绑定门户身份的管理员统计的是该门户身份的未读数。")
    @PreAuthorize("@ss.hasPermi('system:message:list')")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        ResolvedIdentity identity = resolveIdentity();
        if (identity == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(messageService.getUnreadCount(identity.userId, identity.userType));
    }

    /** 解析后的消息身份（userId + userType） */
    private record ResolvedIdentity(Long userId, String userType) {}
}
