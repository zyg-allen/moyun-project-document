package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.service.ICmsContestService;
import com.moyun.portal.domain.entity.PortalWritingContest;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * CMS 创作挑战/征文活动管理 Controller
 *
 * 仅提供基础 CRUD，不接入评审流程。
 *
 * @author moyun
 */
@Tag(name = "CMS创作挑战管理", description = "征文活动的增删改查接口")
@RestController
@RequestMapping("/cms/contest")
public class CmsContestController extends BaseController {

    @Autowired
    private ICmsContestService cmsContestService;

    @Operation(summary = "查询活动列表", description = "分页查询征文活动")
    @PreAuthorize("@ss.hasPermi('cms:contest:list')")
    @GetMapping("/list")
    public AjaxResult list(PortalWritingContest contest) {
        Page<PortalWritingContest> page = PageUtils.startPage();
        cmsContestService.selectContestPage(page, contest);
        return success(page);
    }

    @Operation(summary = "获取活动详情", description = "根据ID获取活动详情")
    @PreAuthorize("@ss.hasPermi('cms:contest:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(cmsContestService.selectContestById(id));
    }

    @Operation(summary = "新增活动", description = "新增征文活动")
    @PreAuthorize("@ss.hasPermi('cms:contest:add')")
    @Log(title = "创作挑战", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalWritingContest contest) {
        contest.setCreateBy(getUsername());
        return toAjax(cmsContestService.insertContest(contest));
    }

    @Operation(summary = "修改活动", description = "修改征文活动")
    @PreAuthorize("@ss.hasPermi('cms:contest:edit')")
    @Log(title = "创作挑战", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalWritingContest contest) {
        contest.setUpdateBy(getUsername());
        return toAjax(cmsContestService.updateContest(contest));
    }

    @Operation(summary = "删除活动", description = "批量删除征文活动")
    @PreAuthorize("@ss.hasPermi('cms:contest:remove')")
    @Log(title = "创作挑战", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cmsContestService.deleteContestByIds(ids));
    }
}
