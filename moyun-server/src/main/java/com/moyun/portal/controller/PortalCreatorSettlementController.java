package com.moyun.portal.controller;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.ICreatorSettlementService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创作者分成结算 Controller（门户端，任务 4.7）
 * <p>
 * 接口列表：
 *   GET /portal/creator/settlement/my      我的结算单分页（需登录 + 创作者身份）
 *   GET /portal/creator/settlement/{id}     结算单详情（需登录 + 归属校验）
 *
 * @author moyun
 */
@Tag(name = "创作者分成结算", description = "创作者查询结算单与收入明细")
@RestController
@RequestMapping("/portal/creator/settlement")
public class PortalCreatorSettlementController extends BaseController {

    @Autowired
    private ICreatorSettlementService settlementService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "我的结算单", description = "分页查询当前创作者的结算单（含月度收入明细与状态）")
    @GetMapping("/my")
    public AjaxResult my(PageDomain query) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        return AjaxResult.success(settlementService.mySettlements(userId, query));
    }

    @Operation(summary = "结算单详情", description = "查询结算单详情（仅结算单归属的创作者本人可查看）")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            com.moyun.portal.domain.entity.PortalCreatorSettlement vo = settlementService.detail(id);
            if (!userId.equals(vo.getCreatorId())) {
                return AjaxResult.error("无权查看该结算单");
            }
            return AjaxResult.success(vo);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
