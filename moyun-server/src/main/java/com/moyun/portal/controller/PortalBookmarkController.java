package com.moyun.portal.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.domain.query.BookmarkQuery;
import com.moyun.portal.mapper.PortalBookmarkMapper;
import com.moyun.portal.service.IPortalBookmarkService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;

@Tag(name = "门户收藏", description = "门户收藏的增删改查操作接口")
@RestController
@RequestMapping("/portal/bookmark")
public class PortalBookmarkController extends BaseController {

    @Autowired
    private IPortalBookmarkService portalBookmarkService;

    @Autowired
    private PortalBookmarkMapper portalBookmarkMapper;

    @Operation(summary = "获取收藏列表", description = "根据条件分页查询收藏列表")
    @GetMapping("/list")
    public AjaxResult list(BookmarkQuery query) {
        Page<PortalBookmark> page = PageUtils.buildPage(query);
        Page<PortalBookmark> resultPage = portalBookmarkService.selectPortalBookmarkPage(page, query);
        return success(resultPage);
    }

    @Operation(summary = "导出收藏", description = "导出收藏数据到Excel文件")
    @Log(title = "门户收藏", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BookmarkQuery query) {
        List<PortalBookmark> list = portalBookmarkService.selectPortalBookmarkList(query);
        ExcelUtil<PortalBookmark> util = new ExcelUtil<PortalBookmark>(PortalBookmark.class);
        util.exportExcel(response, list, "门户收藏数据");
    }

    @Operation(summary = "获取收藏详情", description = "根据收藏ID获取收藏详细信息")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@Parameter(description = "收藏ID") @PathVariable Long id) {
        return success(portalBookmarkService.selectPortalBookmarkById(id));
    }

    @Operation(summary = "新增收藏", description = "创建新收藏")
    @Log(title = "门户收藏", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody PortalBookmark portalBookmark) {
        return toAjax(portalBookmarkService.insertPortalBookmark(portalBookmark));
    }

    @Operation(summary = "修改收藏", description = "更新收藏信息")
    @Log(title = "门户收藏", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody PortalBookmark portalBookmark) {
        return toAjax(portalBookmarkService.updatePortalBookmark(portalBookmark));
    }

    @Operation(summary = "删除收藏", description = "批量删除收藏")
    @Log(title = "门户收藏", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@Parameter(description = "收藏ID数组") @PathVariable Long[] ids) {
        return toAjax(portalBookmarkService.deletePortalBookmarkByIds(ids));
    }

    /**
     * 文章收藏 - 行业标准实现
     * 1. 检查是否已收藏
     * 2. 如果未收藏，则收藏
     * 3. 如果已收藏，则取消收藏
     * 4. 返回最新收藏状态
     */
    @Operation(summary = "文章收藏/取消收藏", description = "收藏或取消收藏文章，返回最新状态")
    @PostMapping("/{articleId}/toggle")
    @Transactional
    public AjaxResult toggleBookmark(@PathVariable Long articleId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return error("请先登录");
        }

        // 查询是否已收藏
        LambdaQueryWrapper<PortalBookmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalBookmark::getUserId, userId)
                .eq(PortalBookmark::getArticleId, articleId);
        PortalBookmark existingBookmark = portalBookmarkMapper.selectOne(wrapper);

        boolean isBookmarked;

        if (existingBookmark == null) {
            // 未收藏，执行收藏
            PortalBookmark bookmark = new PortalBookmark();
            bookmark.setUserId(userId);
            bookmark.setArticleId(articleId);
            bookmark.setCreateTime(LocalDateTime.now());
            portalBookmarkMapper.insert(bookmark);
            isBookmarked = true;
        } else {
            // 已收藏，取消收藏
            portalBookmarkMapper.deleteById(existingBookmark.getId());
            isBookmarked = false;
        }

        // 返回最新状态
        Map<String, Object> result = new HashMap<>();
        result.put("isBookmarked", isBookmarked);
        result.put("articleId", articleId);
        return success(result);
    }

    /**
     * 检查是否已收藏
     */
    @Operation(summary = "检查收藏状态", description = "检查当前用户是否已收藏该文章")
    @GetMapping("/{articleId}/check")
    public AjaxResult checkBookmarkStatus(@PathVariable Long articleId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("isBookmarked", false);
            return success(result);
        }

        LambdaQueryWrapper<PortalBookmark> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PortalBookmark::getUserId, userId)
                .eq(PortalBookmark::getArticleId, articleId);
        long count = portalBookmarkMapper.selectCount(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("isBookmarked", count > 0);
        return success(result);
    }
}
