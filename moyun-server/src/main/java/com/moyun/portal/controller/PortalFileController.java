package com.moyun.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moyun.common.annotation.Log;
import com.moyun.common.constant.HttpStatus;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 门户文件管理 Controller
 *
 * 提供门户用户上传 / 删除文件能力。删除用于前端「删除/替换附件」时清理
 * MinIO / 本地存储与 sys_file 记录，避免脏数据堆积。
 *
 * 清理说明：
 * 原 list / getInfo 已无前端调用方，已从 Controller 层移除。
 * Service / Mapper / XML 实现保留，便于后续复用。
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

    /**
     * 删除文件（按 fileUrl）
     * 用于前端组件删除/替换附件时清理：组件只持有上传返回的访问 URL。
     * 校验登录态 + 文件归属本人，防止越权删除他人文件。
     * 记录不存在视为已删除，返回 success（幂等）。
     */
    @Operation(summary = "删除文件", description = "按文件URL删除文件（存储+记录），仅允许删除本人上传的文件")
    @Log(title = "门户文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping
    public AjaxResult remove(@Parameter(description = "文件访问URL") @RequestParam("fileUrl") String fileUrl) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        boolean ok = sysFileService.deleteFileByUrl(fileUrl, userId);
        // 幂等：记录不存在视为已删除，不抛错
        return success(ok);
    }
}
