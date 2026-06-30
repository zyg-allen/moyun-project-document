package com.moyun.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;

/**
 * 门户文件管理 Controller
 *
 * 清理说明：
 * 本 Controller 仅保留文件上传接口（upload），供 QuillEditor / MarkdownEditor / PublishPage 调用。
 * 原 list（获取文件列表）、getInfo（获取文件详情）、remove（删除文件）三个接口已无前端调用方，
 * 属于死接口，已从 Controller 层移除。对应的 Service / Mapper / XML 实现保留不动，便于后续复用或重新启用。
 */
@Tag(name = "门户文件管理", description = "门户用户文件管理接口")
@RestController
@RequestMapping("/portal/file")
public class PortalFileController extends BaseController {

    @Autowired
    private ISysFileService sysFileService;

    @Operation(summary = "上传文件", description = "门户用户上传文件")
    @Log(title = "门户文件管理", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "businessType", required = false) String businessType,
                             @RequestParam(value = "businessId", required = false) String businessId) {
        SysFile sysFile = sysFileService.uploadFileForPortal(file, businessType, businessId);
        return success(sysFile);
    }
}
