package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalWalletTransaction;
import com.moyun.portal.mapper.PortalWalletTransactionMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 钱包交易流水 Controller
 *
 * 后台查询钱包交易流水，仅提供查询能力。
 *
 * @author moyun
 */
@Tag(name = "CMS钱包交易流水", description = "钱包交易流水查询接口")
@RestController
@RequestMapping("/cms/wallet")
public class CmsWalletTransactionController extends BaseController {

    @Autowired
    private PortalWalletTransactionMapper portalWalletTransactionMapper;

    @Operation(summary = "查询交易流水", description = "分页查询钱包交易流水")
    @PreAuthorize("@ss.hasPermi('cms:wallet:list')")
    @GetMapping("/transaction/list")
    public AjaxResult transactionList(PortalWalletTransaction transaction) {
        Page<PortalWalletTransaction> page = PageUtils.startPage();
        LambdaQueryWrapper<PortalWalletTransaction> wrapper = new LambdaQueryWrapper<>();
        if (transaction.getUserId() != null) {
            wrapper.eq(PortalWalletTransaction::getUserId, transaction.getUserId());
        }
        if (transaction.getType() != null && !transaction.getType().isEmpty()) {
            wrapper.eq(PortalWalletTransaction::getType, transaction.getType());
        }
        wrapper.orderByDesc(PortalWalletTransaction::getCreateTime);
        portalWalletTransactionMapper.selectPage(page, wrapper);
        return success(page);
    }

    @Operation(summary = "获取交易详情", description = "根据ID获取交易流水详情")
    @PreAuthorize("@ss.hasPermi('cms:wallet:query')")
    @GetMapping("/transaction/{id}")
    public AjaxResult transactionInfo(@PathVariable Long id) {
        return success(portalWalletTransactionMapper.selectById(id));
    }
}
