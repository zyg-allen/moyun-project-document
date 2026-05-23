package com.moyun.community.wallet.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.wallet.entity.CmsWithdraw;
import com.moyun.community.wallet.service.ICmsWithdrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/wallet/withdraw")
public class CmsWithdrawAdminController extends BaseController {

    @Autowired
    private ICmsWithdrawService withdrawService;

    @PreAuthorize("@ss.hasPermi('wallet:withdraw:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsWithdraw> list = withdrawService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('wallet:withdraw:query')")
    @GetMapping(value = "/{withdrawId}")
    public AjaxResult getInfo(@PathVariable("withdrawId") Long withdrawId) {
        return success(withdrawService.getById(withdrawId));
    }

    @PreAuthorize("@ss.hasPermi('wallet:withdraw:edit')")
    @Log(title = "提现管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsWithdraw withdraw) {
        return toAjax(withdrawService.updateById(withdraw));
    }
}
