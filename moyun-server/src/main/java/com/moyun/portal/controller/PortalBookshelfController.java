package com.moyun.portal.controller;

import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBookshelf;
import com.moyun.portal.domain.query.BookshelfQuery;
import com.moyun.portal.service.IPortalBookshelfService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 读书空间-书架（前台用户）
 *
 * 接口：
 *   POST   /portal/reading/bookshelf/{bookId}        加入书架（toggle，已收藏则取消）
 *   DELETE /portal/reading/bookshelf/{bookId}        移出书架
 *   GET    /portal/reading/bookshelf                 我的书架列表（分页）
 *   GET    /portal/reading/bookshelf/check/{bookId}  检查是否已收藏
 *   PUT    /portal/reading/bookshelf/reorder         书架排序（预留）
 *
 * @author moyun
 */
@Tag(name = "读书空间-书架", description = "用户书架（收藏书籍）接口")
@RestController
@RequestMapping("/portal/reading/bookshelf")
public class PortalBookshelfController extends BaseController {

    @Autowired
    private IPortalBookshelfService bookshelfService;

    @Operation(summary = "加入书架（toggle，已收藏则取消）")
    @PostMapping("/{bookId}")
    public AjaxResult toggleBookshelf(@PathVariable Long bookId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (bookId == null) {
            return AjaxResult.error("书籍ID不能为空");
        }
        boolean inShelf = bookshelfService.toggleBookshelf(userId, bookId);
        Map<String, Object> data = new HashMap<>();
        data.put("inBookshelf", inShelf);
        data.put("message", inShelf ? "已加入书架" : "已移出书架");
        return AjaxResult.success(data);
    }

    @Operation(summary = "移出书架")
    @DeleteMapping("/{bookId}")
    public AjaxResult removeBookshelf(@PathVariable Long bookId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        PortalBookshelf existing = bookshelfService.selectByUserAndBook(userId, bookId);
        if (existing == null) {
            return AjaxResult.error("该书不在书架中");
        }
        bookshelfService.deleteBookshelfById(existing.getId());
        return AjaxResult.success("已移出书架");
    }

    @Operation(summary = "我的书架列表（分页）")
    @GetMapping
    public AjaxResult myBookshelf(BookshelfQuery query) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        query.setUserId(userId);
        // 默认按收藏时间倒序
        if (query.getOrderBy() == null || query.getOrderBy().isEmpty()) {
            query.setOrderBy("latest");
        }
        Page<PortalBookshelf> page = buildPage(query);
        Page<PortalBookshelf> result = bookshelfService.selectBookshelfPage(page, query);
        return AjaxResult.success(result);
    }

    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check/{bookId}")
    public AjaxResult checkBookshelf(@PathVariable Long bookId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            // 未登录返回未收藏状态
            Map<String, Object> data = new HashMap<>();
            data.put("inBookshelf", false);
            return AjaxResult.success(data);
        }
        PortalBookshelf item = bookshelfService.selectByUserAndBook(userId, bookId);
        Map<String, Object> data = new HashMap<>();
        data.put("inBookshelf", item != null);
        if (item != null) {
            data.put("lastChapterId", item.getLastChapterId());
            data.put("lastChapterNo", item.getLastChapterNo());
        }
        return AjaxResult.success(data);
    }

    @Operation(summary = "更新书架项的最后阅读章节（阅读时同步）")
    @PutMapping("/{bookId}/last-chapter")
    public AjaxResult updateLastChapter(@PathVariable Long bookId,
                                        @RequestParam(required = false) Long lastChapterId,
                                        @RequestParam(required = false) Integer lastChapterNo) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        int rows = bookshelfService.updateLastChapter(userId, bookId, lastChapterId, lastChapterNo);
        return rows > 0 ? AjaxResult.success() : AjaxResult.success("书架中无此书，跳过同步");
    }

    // -------------------------------------------------------
    // 分页构建：复用 BaseController 的 buildPage 工具方法（若基类未提供则用默认）
    // -------------------------------------------------------
    private Page<PortalBookshelf> buildPage(BookshelfQuery query) {
        int pageNum = query.getPageNum() == null ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 12 : query.getPageSize();
        if (pageSize > 100) pageSize = 100;
        return new Page<>(pageNum, pageSize);
    }
}
