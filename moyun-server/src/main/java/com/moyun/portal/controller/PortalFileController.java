package com.moyun.portal.controller;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "门户文件管理", description = "门户用户文件管理接口")
@RestController
@RequestMapping("/portal/file")
public class PortalFileController extends BaseController {

    @Autowired
    private ISysFileService sysFileService;

    @Operation(summary = "获取我的文件列表", description = "查询当前门户用户上传的文件列表")
    @GetMapping("/list")
    public AjaxResult list(SysFile query) {
        query.setUploadUserId(com.moyun.portal.util.PortalSecurityUtils.getUserId());
        Page<SysFile> page = PageUtils.buildPage(query);
        page = sysFileService.selectFilePage(page, query);
        return success(page);
    }

    @Operation(summary = "获取文件详情", description = "根据文件ID获取文件详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "文件ID") @PathVariable Long id) {
        SysFile file = sysFileService.selectFileById(id);
        if (file == null) {
            return error("文件不存在");
        }
        Long currentUserId = com.moyun.portal.util.PortalSecurityUtils.getUserId();
        if (!currentUserId.equals(file.getUploadUserId())) {
            return error("无权访问此文件");
        }
        return success(file);
    }

    @Operation(summary = "上传文件", description = "门户用户上传文件")
    @Log(title = "门户文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "businessType", required = false) String businessType,
                             @RequestParam(value = "businessId", required = false) String businessId) {
        SysFile sysFile = sysFileService.uploadFileForPortal(file, businessType, businessId);
        return success(sysFile);
    }

    @Operation(summary = "删除文件", description = "删除自己上传的文件")
    @Log(title = "门户文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@Parameter(description = "文件ID") @PathVariable Long id) {
        SysFile file = sysFileService.selectFileById(id);
        if (file == null) {
            return error("文件不存在");
        }
        Long currentUserId = com.moyun.portal.util.PortalSecurityUtils.getUserId();
        if (!currentUserId.equals(file.getUploadUserId())) {
            return error("无权删除此文件");
        }
        return toAjax(sysFileService.deleteFileById(id));
    }
}
