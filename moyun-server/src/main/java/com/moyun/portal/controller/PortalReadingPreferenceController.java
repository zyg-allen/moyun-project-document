package com.moyun.portal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalReadingPreference;
import com.moyun.portal.service.IPortalReadingPreferenceService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 读书空间-阅读偏好（前台用户）
 *
 * 接口：
 *   GET /portal/reading/preference     查询我的阅读偏好（不存在返回默认）
 *   PUT /portal/reading/preference     保存（upsert）阅读偏好
 *
 * @author moyun
 */
@Tag(name = "读书空间-阅读偏好", description = "用户阅读偏好（字号/行距/主题/字体）接口")
@RestController
@RequestMapping("/portal/reading/preference")
public class PortalReadingPreferenceController extends BaseController {

    @Autowired
    private IPortalReadingPreferenceService preferenceService;

    @Operation(summary = "查询我的阅读偏好")
    @GetMapping
    public AjaxResult getMyPreference() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        PortalReadingPreference pref = preferenceService.getOrCreateByUserId(userId);
        return AjaxResult.success(pref);
    }

    @Operation(summary = "保存（upsert）阅读偏好")
    @PutMapping
    public AjaxResult savePreference(@RequestBody PortalReadingPreference preference) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        preference.setUserId(userId);
        // 不允许通过 body 修改 id/createBy 等敏感字段
        preference.setId(null);
        preference.setCreateBy(null);
        preference.setUpdateBy(null);
        int rows = preferenceService.savePreference(preference);
        return rows > 0 ? AjaxResult.success("保存成功") : AjaxResult.success("未发生变更");
    }
}
