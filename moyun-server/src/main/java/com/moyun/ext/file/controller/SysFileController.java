package com.moyun.ext.file.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.config.MinioConfig;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.file.domain.entity.SysFile;
import com.moyun.ext.file.service.ISysFileService;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.MinioUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "文件管理", description = "文件管理接口")
@RestController
@RequestMapping("/system/file")
public class SysFileController extends BaseController {

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioUtils minioUtils;

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

    /**
     * 按文件URL删除（存储 + 记录）
     * 用于前端组件「删除/替换附件」时清理：组件只持有访问 URL，无 fileId。
     * 需 system:file:remove 权限（后台用户），不校验上传者（后台可管理所有文件）。
     * 记录不存在视为已删除（幂等）。
     */
    @Operation(summary = "按URL删除文件", description = "根据文件访问URL删除文件（存储+记录），用于附件替换/删除清理")
    @PreAuthorize("@ss.hasPermi('system:file:remove')")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/byUrl")
    public AjaxResult removeByUrl(@Parameter(description = "文件访问URL") @RequestParam("fileUrl") String fileUrl) {
        boolean ok = sysFileService.deleteFileByUrl(fileUrl, null);
        return success(ok);
    }

    @Operation(summary = "批量删除文件", description = "根据文件ID列表批量删除文件")
    @PreAuthorize("@ss.hasPermi('system:file:remove')")
    @Log(title = "文件管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult removeByIds(@Parameter(description = "文件ID列表") @PathVariable Long[] ids) {
        return toAjax(sysFileService.deleteFileByIds(ids));
    }

    // ==================== 存储模式管理 ====================

    /**
     * 查询当前存储状态：配置启用状态、降级开关、服务真实可达性
     */
    @Operation(summary = "查询存储状态", description = "返回 MinIO 配置启用、自动降级开关、手动降级开关、服务真实可达性")
    @PreAuthorize("@ss.hasPermi('system:file:list')")
    @GetMapping("/storage/status")
    public AjaxResult storageStatus() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", minioConfig.getEnabled());
        data.put("autoFallback", minioConfig.getAutoFallback());
        data.put("fallbackToLocal", minioConfig.getFallbackToLocal());
        data.put("effectiveStorage", minioUtils.isEnabled() ? "minio" : "local");
        // 服务真实可达性（仅当配置启用时才探测）
        data.put("minioAvailable", minioUtils.isEnabled() && minioUtils.isAvailable());
        return success(data);
    }

    /**
     * 手动切换存储模式（无需重启）。
     * mode=minio：恢复正常 MinIO 存储（清除降级标记）
     * mode=local：强制降级到本地存储
     */
    @Operation(summary = "切换存储模式", description = "手动切换 MinIO/本地 存储模式，运行期生效，无需重启")
    @PreAuthorize("@ss.hasPermi('system:file:add')")
    @Log(title = "文件管理", businessType = BusinessType.UPDATE)
    @PutMapping("/storage/switch")
    public AjaxResult switchStorage(@Parameter(description = "目标模式：minio 或 local") @RequestParam String mode) {
        if (!"minio".equalsIgnoreCase(mode) && !"local".equalsIgnoreCase(mode)) {
            return error("模式参数非法，仅支持 minio / local");
        }
        if (!Boolean.TRUE.equals(minioConfig.getEnabled())) {
            return error("当前配置未启用 MinIO（minio.enabled=false），无需切换");
        }
        if ("minio".equalsIgnoreCase(mode)) {
            minioConfig.setFallbackToLocal(false);
            if (!minioUtils.isAvailable()) {
                return warn("已切换回 MinIO 模式，但当前 MinIO 服务不可达，后续上传将触发自动降级");
            }
        } else {
            minioConfig.setFallbackToLocal(true);
        }
        return success();
    }
}
