package com.moyun.portal.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.page.PageDomain;
import com.moyun.portal.domain.entity.PortalWritingPrompt;
import com.moyun.portal.service.IPortalWritingPromptService;

/**
 * 每日写作 prompt Controller（门户端，全部公开）
 *
 * @author moyun
 */
@Tag(name = "每日写作 prompt", description = "每日写作灵感 prompt，激发创作")
@RestController
@RequestMapping("/portal/prompt")
public class PortalWritingPromptController extends BaseController {

    @Autowired
    private IPortalWritingPromptService promptService;

    @Operation(summary = "今日 prompt", description = "返回当天的写作 prompt，未配置则返回最近一条")
    @GetMapping("/today")
    @Anonymous
    public AjaxResult today() {
        PortalWritingPrompt prompt = promptService.getToday();
        return AjaxResult.success(prompt);
    }

    @Operation(summary = "历史 prompt", description = "分页查询历史 prompt（按日期降序）")
    @GetMapping("/history")
    @Anonymous
    public AjaxResult history(PageDomain pageDomain,
                              @Parameter(description = "分类筛选") @RequestParam(required = false) String category) {
        Page<PortalWritingPrompt> page = promptService.listHistory(pageDomain, category);
        return AjaxResult.success(page);
    }
}
