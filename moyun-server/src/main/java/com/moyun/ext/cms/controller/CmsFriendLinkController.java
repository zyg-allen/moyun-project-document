package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.cms.domain.query.CmsFriendLinkQuery;
import com.moyun.ext.cms.domain.vo.CmsFriendLinkVO;
import com.moyun.ext.cms.service.ICmsFriendLinkService;
import com.moyun.portal.domain.entity.PortalFriendLink;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "CMS友情链接管理", description = "CMS友情链接管理接口")
@RestController
@RequestMapping("/cms/friend-link")
public class CmsFriendLinkController extends BaseController
{
    @Autowired
    private ICmsFriendLinkService cmsFriendLinkService;

    @Operation(summary = "获取友情链接列表", description = "根据条件分页查询友情链接列表")
    @PreAuthorize("@ss.hasPermi('cms:friend-link:list')")
    @GetMapping("/list")
    public AjaxResult list(CmsFriendLinkQuery query)
    {
        Page<CmsFriendLinkVO> page = PageUtils.buildPage(query);
        page = cmsFriendLinkService.selectFriendLinkPage(page, query);
        return success(page);
    }

    @Operation(summary = "获取友情链接详情", description = "根据友情链接ID获取友情链接详细信息")
    @PreAuthorize("@ss.hasPermi('cms:friend-link:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "友情链接ID") @PathVariable Long id)
    {
        return success(cmsFriendLinkService.selectFriendLinkById(id));
    }

    @Operation(summary = "新增友情链接", description = "创建新友情链接")
    @PreAuthorize("@ss.hasPermi('cms:friend-link:add')")
    @Log(title = "友情链接管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalFriendLink friendLink)
    {
        return toAjax(cmsFriendLinkService.insertFriendLink(friendLink));
    }

    @Operation(summary = "修改友情链接", description = "更新友情链接信息")
    @PreAuthorize("@ss.hasPermi('cms:friend-link:edit')")
    @Log(title = "友情链接管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalFriendLink friendLink)
    {
        return toAjax(cmsFriendLinkService.updateFriendLink(friendLink));
    }

    @Operation(summary = "删除友情链接", description = "批量删除友情链接")
    @PreAuthorize("@ss.hasPermi('cms:friend-link:remove')")
    @Log(title = "友情链接管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "友情链接ID数组") @PathVariable Long[] ids)
    {
        return toAjax(cmsFriendLinkService.deleteFriendLinkByIds(ids));
    }
}
