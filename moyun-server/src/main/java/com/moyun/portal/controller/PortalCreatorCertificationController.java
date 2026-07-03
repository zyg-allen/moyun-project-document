package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalCreatorCertification;
import com.moyun.portal.service.IPortalCreatorCertificationService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 创作者认证 Controller（门户端，创作者天堂入口）
 * <p>
 * 接口列表：
 *   POST /portal/creator/certification/apply  提交认证申请（需登录）
 *   GET  /portal/creator/certification/my    我的认证状态（需登录）
 *
 * @author moyun
 */
@Tag(name = "创作者认证", description = "创作者认证申请与状态查询")
@RestController
@RequestMapping("/portal/creator/certification")
public class PortalCreatorCertificationController extends BaseController {

    @Autowired
    private IPortalCreatorCertificationService certificationService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "提交认证申请", description = "提交创作者认证申请，同用户已有 pending 申请时拒绝重复提交")
    @PostMapping("/apply")
    public AjaxResult apply(@RequestBody PortalCreatorCertification dto) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        // 简单参数校验
        if (dto.getRealName() == null || dto.getRealName().trim().isEmpty()) {
            return AjaxResult.error("真实姓名不能为空");
        }
        if (dto.getCertType() == null || dto.getCertType().trim().isEmpty()) {
            return AjaxResult.error("证件类型不能为空");
        }
        try {
            return AjaxResult.success(certificationService.apply(userId, dto));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "我的认证状态", description = "查询当前用户最近一条认证申请记录及审核状态")
    @GetMapping("/my")
    public AjaxResult my() {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(certificationService.getMy(userId));
    }
}
