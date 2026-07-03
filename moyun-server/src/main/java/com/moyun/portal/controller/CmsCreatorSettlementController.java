package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.ICreatorSettlementService;
import com.moyun.portal.domain.entity.PortalCreatorSettlement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创作者分成结算 后台 Controller（任务 4.7）
 *
 * <p>供后台管理页面调用，提供结算单分页查询、详情、确认、打款与月度生成能力。</p>
 * <p>路径前缀 /cms/creator/settlement，权限标识 portal:settlement:list / confirm / pay / generate。</p>
 *
 * @author moyun
 */
@Tag(name = "创作者分成结算管理", description = "结算单查询、确认、打款与月度生成")
@RestController
@RequestMapping("/cms/creator/settlement")
public class CmsCreatorSettlementController extends BaseController {

    @Autowired
    private ICreatorSettlementService settlementService;

    @Operation(summary = "结算单分页列表", description = "后台分页查询结算单，支持按 period/status/creatorId 过滤")
    @PreAuthorize("@ss.hasPermi('portal:settlement:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalCreatorSettlement query,
                            @RequestParam(defaultValue = "1") Integer pageNum,
                            @RequestParam(defaultValue = "10") Integer pageSize) {
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(pageNum);
        pageDomain.setPageSize(pageSize);
        Page<PortalCreatorSettlement> result = settlementService.list(query, pageDomain);
        return AjaxResult.success(result);
    }

    @Operation(summary = "结算单详情", description = "查询结算单详情（含创作者信息）")
    @PreAuthorize("@ss.hasPermi('portal:settlement:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        try {
            return AjaxResult.success(settlementService.detail(id));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "确认结算单", description = "pending -> confirmed")
    @PreAuthorize("@ss.hasPermi('portal:settlement:confirm')")
    @Log(title = "创作者分成结算", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/confirm")
    public AjaxResult confirm(@PathVariable("id") Long id) {
        try {
            return AjaxResult.success(settlementService.confirm(id));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "标记已打款", description = "confirmed -> paid")
    @PreAuthorize("@ss.hasPermi('portal:settlement:pay')")
    @Log(title = "创作者分成结算", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/pay")
    public AjaxResult markPaid(@PathVariable("id") Long id) {
        try {
            return AjaxResult.success(settlementService.markPaid(id));
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "月度生成结算单", description = "聚合某周期收入生成结算单，period 为空时默认上个月；已存在的跳过")
    @PreAuthorize("@ss.hasPermi('portal:settlement:generate')")
    @Log(title = "创作者分成结算", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestParam(value = "period", required = false) String period) {
        try {
            int count = settlementService.generateMonthlySettlement(period);
            return AjaxResult.success(count);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
