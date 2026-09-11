package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.IPortalInterviewConfigService;
import com.moyun.portal.domain.entity.PortalInterviewConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 面试配置管理 Controller（v11.x 智能面试）
 *
 * <p>人设/提示词模板/评分权重/追问策略/自我介绍环节管理。</p>
 *
 * @author moyun
 */
@Tag(name = "面试配置管理")
@RestController
@RequestMapping("/cms/interview/config")
public class CmsInterviewConfigController extends BaseController {

    @Autowired
    private IPortalInterviewConfigService interviewConfigService;

    @Operation(summary = "面试配置分页列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:config:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<PortalInterviewConfig> page = new Page<>(pageNum, pageSize);
        page = interviewConfigService.page(page, new LambdaQueryWrapper<PortalInterviewConfig>()
                .orderByDesc(PortalInterviewConfig::getIsDefault)
                .orderByDesc(PortalInterviewConfig::getCreateTime));
        return success(page);
    }

    @Operation(summary = "面试配置详情")
    @PreAuthorize("@ss.hasPermi('cms:interview:config:query')")
    @GetMapping("/{id}")
    public AjaxResult getDetail(@PathVariable Long id) {
        return success(interviewConfigService.getById(id));
    }

    @Operation(summary = "新增面试配置")
    @PreAuthorize("@ss.hasPermi('cms:interview:config:create')")
    @PostMapping
    public AjaxResult create(@RequestBody PortalInterviewConfig config) {
        if (config.getConfigName() == null || config.getConfigName().isBlank()) {
            return error("配置名称不能为空");
        }
        return toAjax(interviewConfigService.save(config));
    }

    @Operation(summary = "修改面试配置")
    @PreAuthorize("@ss.hasPermi('cms:interview:config:update')")
    @PutMapping
    public AjaxResult update(@RequestBody PortalInterviewConfig config) {
        if (config.getId() == null) {
            return error("ID不能为空");
        }
        return toAjax(interviewConfigService.updateById(config));
    }

    @Operation(summary = "删除面试配置")
    @PreAuthorize("@ss.hasPermi('cms:interview:config:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        PortalInterviewConfig config = interviewConfigService.getById(id);
        if (config != null && Integer.valueOf(1).equals(config.getIsDefault())) {
            return error("默认配置不可删除，请先将其他配置设为默认");
        }
        return toAjax(interviewConfigService.removeById(id));
    }
}