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
}
