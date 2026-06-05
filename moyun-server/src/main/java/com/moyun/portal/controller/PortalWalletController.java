package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.AjaxResult;
import com.moyun.common.enums.BusinessType;
import com.moyun.util.file.ExcelUtil;
import com.moyun.portal.domain.entity.PortalWallet;
import com.moyun.portal.service.IPortalWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "门户钱包", description = "门户钱包的增删改查操作接口")
@RestController
@RequestMapping("/portal/wallet")
public class PortalWalletController extends BaseController {

    @Autowired
    private IPortalWalletService portalWalletService;

    @Operation(summary = "获取钱包列表", description = "根据条件分页查询钱包列表")
    @GetMapping("/list")
    public AjaxResult list(PortalWallet portalWallet) {
        // 使用 startPage() 创建分页对象，自动从请求参数获取 page 和 pageSize
        Page<PortalWallet> page = startPage();
        // 调用分页查询方法
        Page<PortalWallet> resultPage = portalWalletService.selectPortalWalletPage(page, portalWallet);
        // 直接返回 Page 对象，MyBatis-Plus 会自动处理分页
        return success(resultPage);
    }

    @Operation(summary = "导出钱包", description = "导出钱包数据到Excel文件")
    @Log(title = "门户钱包", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, PortalWallet portalWallet) {
        // 导出时不分页，调用不分页的查询方法
        List<PortalWallet> list = portalWalletService.selectPortalWalletList(portalWallet);
        ExcelUtil<PortalWallet> util = new ExcelUtil<PortalWallet>(PortalWallet.class);
        util.exportExcel(response, list, "门户钱包数据");
    }

    @Operation(summary = "获取钱包详情", description = "根据钱包ID获取钱包详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "钱包ID") @PathVariable Long id) {
        return success(portalWalletService.selectPortalWalletById(id));
    }

    @Operation(summary = "新增钱包", description = "创建新钱包")
    @Log(title = "门户钱包", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalWallet portalWallet) {
        return toAjax(portalWalletService.insertPortalWallet(portalWallet));
    }

    @Operation(summary = "修改钱包", description = "更新钱包信息")
    @Log(title = "门户钱包", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalWallet portalWallet) {
        return toAjax(portalWalletService.updatePortalWallet(portalWallet));
    }

    @Operation(summary = "删除钱包", description = "批量删除钱包")
    @Log(title = "门户钱包", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "钱包ID数组") @PathVariable Long[] ids) {
        return toAjax(portalWalletService.deletePortalWalletByIds(ids));
    }
}