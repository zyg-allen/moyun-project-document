package com.moyun.community.wallet.controller.admin;

import com.moyun.common.annotation.Log;
import com.moyun.common.core.controller.BaseController;
import com.moyun.common.core.domain.AjaxResult;
import com.moyun.common.core.domain.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.community.wallet.entity.CmsWallet;
import com.moyun.community.wallet.service.ICmsWalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/wallet")
public class CmsWalletAdminController extends BaseController {

    @Autowired
    private ICmsWalletService walletService;

    @PreAuthorize("@ss.hasPermi('wallet:list')")
    @GetMapping("/list")
    public TableDataInfo list() {
        startPage();
        List<CmsWallet> list = walletService.list();
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('wallet:query')")
    @GetMapping(value = "/{userId}")
    public AjaxResult getInfo(@PathVariable("userId") Long userId) {
        return success(walletService.getById(userId));
    }

    @PreAuthorize("@ss.hasPermi('wallet:edit')")
    @Log(title = "钱包管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CmsWallet wallet) {
        return toAjax(walletService.updateById(wallet));
    }
}
