package com.moyun.ext.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件管理", description = "文件管理接口")
@RestController
@RequestMapping("/system/file")
public class SysFileController extends BaseController {

    @Autowired
    private ISysFileService sysFileService;

    @Operation(summary = "获取文件列表", description = "根据条件分页查询文件列表")
    @PreAuthorize("@ss.hasPermi('system:file:list')")
    @GetMapping("/list")
    public AjaxResult list(SysFile query) {
        Page<SysFile> page = PageUtils.buildPage(query);
        page = sysFileService.selectFilePage(page, query);
        return success(page);
    }

    @Operation(summary = "获取文件详情", description = "根据文件ID获取文件详细信息")
    @PreAuthorize("@ss.hasPermi('system:file:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "文件ID") @PathVariable Long id) {
        return success(sysFileService.selectFileById(id));
    }

    @Operation(summary = "上传文件", description = "上传文件并保存到文件管理系统")
    @PreAuthorize("@ss.hasPermi('system:file:add')")
    @Log(title = "文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "businessType", required = false) String businessType,
                             @RequestParam(value = "businessId", required = false) String businessId) {
        SysFile sysFile = sysFileService.uploadFile(file, businessType, businessId);
        return success(sysFile);
    }

    @Operation(summary = "删除文件", description = "根据文件ID删除文件")
    @PreAuthorize("@ss.hasPermi('system:file:remove')")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@Parameter(description = "文件ID") @PathVariable Long id) {
        return toAjax(sysFileService.deleteFileById(id));
    }

    @Operation(summary = "批量删除文件", description = "根据文件ID列表批量删除文件")
    @PreAuthorize("@ss.hasPermi('system:file:remove')")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult removeByIds(@Parameter(description = "文件ID列表") @PathVariable Long[] ids) {
        return toAjax(sysFileService.deleteFileByIds(ids));
    }
}
