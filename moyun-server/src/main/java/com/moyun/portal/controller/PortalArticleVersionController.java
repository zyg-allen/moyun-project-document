package com.moyun.portal.controller;

import java.util.List;
import java.util.Map;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalArticleVersion;
import com.moyun.portal.service.IPortalArticleVersionService;
import com.moyun.portal.util.PortalSecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章版本管理 Controller（所有接口均需登录）
 * <p>
 * 提供版本列表、详情、回滚、对比能力。
 * 对比仅返回两个版本的 title + content 文本，前端做展示，不实现真正的 diff 算法。
 *
 * @author moyun
 */
@Tag(name = "文章版本管理", description = "草稿版本快照、回滚、对比")
@RestController
@RequestMapping("/portal/article")
public class PortalArticleVersionController extends BaseController {

    @Autowired
    private IPortalArticleVersionService portalArticleVersionService;

    private Long currentUserId() {
        return PortalSecurityUtils.getUserId();
    }

    @Operation(summary = "版本列表", description = "查询指定文章的版本列表（按版本号降序，不含大字段）")
    @GetMapping("/{id}/versions")
    public AjaxResult listVersions(@PathVariable("id") Long id) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            List<PortalArticleVersion> list = portalArticleVersionService.listVersions(id);
            return AjaxResult.success(list);
        } catch (RuntimeException e) {
            // 归属校验失败等业务异常，返回友好提示
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "查询版本列表失败");
        }
    }

    @Operation(summary = "版本详情", description = "查询指定版本的完整内容快照")
    @GetMapping("/version/{versionId}")
    public AjaxResult versionDetail(@PathVariable("versionId") Long versionId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            PortalArticleVersion version = portalArticleVersionService.getVersion(versionId);
            if (version == null) {
                return AjaxResult.error("版本不存在");
            }
            return AjaxResult.success(version);
        } catch (RuntimeException e) {
            // 归属校验失败等业务异常，返回友好提示
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "查询版本详情失败");
        }
    }

    @Operation(summary = "回滚版本", description = "将文章内容覆盖回指定版本，并生成回滚后的新版本快照")
    @PostMapping("/{id}/rollback/{versionId}")
    public AjaxResult rollback(@PathVariable("id") Long id,
                               @PathVariable("versionId") Long versionId) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            PortalArticleVersion snapshot = portalArticleVersionService.rollback(id, versionId, userId);
            return AjaxResult.success(snapshot);
        } catch (RuntimeException e) {
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "回滚失败");
        }
    }

    @Operation(summary = "版本对比", description = "返回两个版本的 title + content 文本，前端做展示（不实现真正的 diff 算法）")
    @GetMapping("/{id}/diff/{v1}/{v2}")
    public AjaxResult diff(@PathVariable("id") Long id,
                          @PathVariable("v1") Integer v1,
                          @PathVariable("v2") Integer v2) {
        Long userId = currentUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        try {
            Map<String, Object> result = portalArticleVersionService.diff(id, v1, v2);
            return AjaxResult.success(result);
        } catch (RuntimeException e) {
            // 归属校验失败等业务异常，返回友好提示
            return AjaxResult.error(e.getMessage() != null ? e.getMessage() : "版本对比失败");
        }
    }
}
