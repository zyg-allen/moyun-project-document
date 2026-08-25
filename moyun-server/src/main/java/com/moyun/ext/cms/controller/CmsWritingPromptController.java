package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ICmsWritingPromptService;
import com.moyun.portal.domain.entity.PortalWritingPrompt;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 每日写作 prompt 管理 Controller
 *
 * @author moyun
 */
@Tag(name = "CMS写作Prompt管理", description = "每日写作 prompt 的增删改查接口")
@RestController
@RequestMapping("/cms/writing-prompt")
public class CmsWritingPromptController extends BaseController {

    @Autowired
    private ICmsWritingPromptService cmsWritingPromptService;

    @Operation(summary = "查询 prompt 列表", description = "分页查询写作 prompt")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalWritingPrompt prompt) {
        Page<PortalWritingPrompt> page = PageUtils.startPage();
        cmsWritingPromptService.selectPromptPage(page, prompt);
        return success(page);
    }

    @Operation(summary = "获取 prompt 详情", description = "根据ID获取 prompt 详情")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsWritingPromptService.selectPromptById(id));
    }

    @Operation(summary = "新增 prompt", description = "新增写作 prompt")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:add')")
    @Log(title = "写作Prompt", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalWritingPrompt prompt) {
        prompt.setCreateBy(getUsername());
        return toAjax(cmsWritingPromptService.insertPrompt(prompt));
    }

    @Operation(summary = "修改 prompt", description = "修改写作 prompt")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:edit')")
    @Log(title = "写作Prompt", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalWritingPrompt prompt) {
        prompt.setUpdateBy(getUsername());
        return toAjax(cmsWritingPromptService.updatePrompt(prompt));
    }

    @Operation(summary = "删除 prompt", description = "批量删除写作 prompt")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:remove')")
    @Log(title = "写作Prompt", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cmsWritingPromptService.deletePromptByIds(ids));
    }

    @Operation(summary = "AI 生成 prompt", description = "AI 为指定日期生成写作提示（已存在则跳过）；结合节日/节气上下文，AI 失败回退内置主题池")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:add')")
    @Log(title = "写作Prompt", businessType = BusinessType.INSERT)
    @PostMapping("/ai-generate")
    public AjaxResult aiGenerate(@RequestParam(value = "date", required = false)
                                 @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                 java.time.LocalDate date) {
        java.time.LocalDate target = date != null ? date : java.time.LocalDate.now();
        return success(cmsWritingPromptService.aiGenerateForDate(target));
    }

    @Operation(summary = "AI 批量补生成", description = "从起始日起连续 N 天补生成缺失的写作提示（已存在跳过），用于初始化或补漏")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:add')")
    @Log(title = "写作Prompt", businessType = BusinessType.INSERT)
    @PostMapping("/ai-generate-range")
    public AjaxResult aiGenerateRange(@RequestParam(value = "startDate", required = false)
                                      @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                      java.time.LocalDate startDate,
                                      @RequestParam(value = "days", defaultValue = "7") Integer days) {
        java.time.LocalDate start = startDate != null ? startDate : java.time.LocalDate.now();
        int bounded = Math.max(1, Math.min(days, 30));
        int count = cmsWritingPromptService.aiGenerateRange(start, bounded);
        return success(count);
    }

    @Operation(summary = "AI 重新生成", description = "AI 覆盖式重新生成指定 prompt 的内容（保留日期与ID）")
    @PreAuthorize("@ss.hasPermi('cms:writing-prompt:edit')")
    @Log(title = "写作Prompt", businessType = BusinessType.UPDATE)
    @PutMapping("/ai-regenerate/{id}")
    public AjaxResult aiRegenerate(@PathVariable Long id) {
        return success(cmsWritingPromptService.aiRegenerate(id));
    }
}
