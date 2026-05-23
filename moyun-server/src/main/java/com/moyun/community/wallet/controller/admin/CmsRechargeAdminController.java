package com.moyun.community.wallet.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.wallet.entity.CmsRecharge;
import com.moyun.community.wallet.service.ICmsRechargeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/wallet/recharge")
public class CmsRechargeAdminController extends BaseController {

    @Autowired
    private ICmsRechargeService rechargeService;

    @PreAuthorize("@ss.hasPermi('wallet:recharge:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsRecharge> list = rechargeService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('wallet:recharge:query')")
    @GetMapping(value = "/{rechargeId}")
    public AjaxResult getInfo(@PathVariable("rechargeId") Long rechargeId) {
        return success(rechargeService.getById(rechargeId));
    }

    @PreAuthorize("@ss.hasPermi('wallet:recharge:edit')")
    @Log(title = "充值管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsRecharge recharge) {
        return toAjax(rechargeService.updateById(recharge));
    }
}
