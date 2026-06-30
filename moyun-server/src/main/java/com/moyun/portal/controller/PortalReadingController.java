package com.moyun.portal.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalBook;
import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.entity.PortalBookListBookmark;
import com.moyun.portal.domain.entity.PortalBookListItem;
import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.query.BookListQuery;
import com.moyun.portal.domain.query.BookQuery;
import com.moyun.portal.domain.query.BookQuoteQuery;
import com.moyun.portal.mapper.PortalBookListBookmarkMapper;
import com.moyun.portal.service.IPortalBookListService;
import com.moyun.portal.service.IPortalBookQuoteService;
import com.moyun.portal.service.IPortalBookService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.util.PortalSecurityUtils;

/**
 * 读书空间前台Controller
 *
 * NOTE（清理记录）：
 *   本 Controller 曾暴露 10 个前台接口，经核查其中 5 个为无前端调用的死接口，已于本次清理移除：
 *     - GET  /portal/reading/books                  → getBooks        （列表分页，前端改用首页精选数据，未再调用）
 *     - GET  /portal/reading/book-lists             → getBookLists    （列表分页，前端改用首页精选数据，未再调用）
 *     - GET  /portal/reading/quotes                 → getQuotes       （列表分页，前端改用首页精选数据，未再调用）
 *     - POST /portal/reading/quotes/{id}/like       → likeQuote       （点赞接口，前端未接入）
 *     - POST /portal/reading/book-lists/{id}/like   → likeBookList    （点赞接口，前端未接入）
 *   保留以下 5 个仍在使用的接口：
 *     - GET  /portal/reading/home                   → getReadingHome
 *     - GET  /portal/reading/books/{id}             → getBookById
 *     - GET  /portal/reading/book-lists/{id}        → getBookListById
 *     - POST /portal/reading/book-lists/{id}/bookmark → toggleBookListBookmark
 *     - GET  /portal/reading/book-lists/{id}/bookmark → checkBookListBookmark
 *   说明：本次仅删除 Controller 层方法，对应的 Service / Mapper / XML 实现保持不动，避免影响其他调用方。
 *
 * @author moyun
 */
@Tag(name = "读书空间-前台", description = "读书空间前台接口")
@RestController
@RequestMapping("/portal/reading")
public class PortalReadingController extends BaseController {

    @Autowired
    private IPortalBookService portalBookService;

    @Autowired
    private IPortalBookListService portalBookListService;

    @Autowired
    private IPortalBookQuoteService portalBookQuoteService;

    @Autowired
    private PortalBookListBookmarkMapper bookListBookmarkMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    /**
     * 获取读书空间首页数据
     */
    @Operation(summary = "获取读书空间首页数据", description = "获取首页展示的精选书单、精选书籍、精选金句")
    @GetMapping("/home")
    @Anonymous
    public AjaxResult getReadingHome() {
        Map<String, Object> data = new HashMap<>();

        // 精选书单
        BookListQuery listQuery = new BookListQuery();
        listQuery.setIsFeatured(true);
        listQuery.setIsPublic(true);
        listQuery.setStatus("active");
        listQuery.setPageNum(1);
        listQuery.setPageSize(6);
        Page<PortalBookList> listPage = portalBookListService.selectPortalBookListPage(
            new Page<>(1, 6), listQuery);
        data.put("bookLists", listPage.getRecords());

        // 精选书籍
        BookQuery bookQuery = new BookQuery();
        bookQuery.setIsFeatured(true);
        bookQuery.setStatus("active");
        Page<PortalBook> bookPage = portalBookService.selectPortalBookPage(
            new Page<>(1, 8), bookQuery);
        data.put("books", bookPage.getRecords());

        // 精选金句
        BookQuoteQuery quoteQuery = new BookQuoteQuery();
        quoteQuery.setIsFeatured(true);
        quoteQuery.setIsPublic(true);
        Page<PortalBookQuote> quotePage = portalBookQuoteService.selectPortalBookQuotePage(
            new Page<>(1, 5), quoteQuery);
        data.put("quotes", quotePage.getRecords());

        // 统计数据
        data.put("bookCount", bookPage.getTotal());
        data.put("bookListCount", listPage.getTotal());
        data.put("quoteCount", quotePage.getTotal());

        return AjaxResult.success(data);
    }

    /**
     * 获取书籍详情
     */
    @Operation(summary = "获取书籍详情", description = "根据ID获取书籍详情")
    @GetMapping("/books/{id}")
    @Anonymous
    public AjaxResult getBookById(@Parameter(description = "书籍ID") @PathVariable Long id) {
        portalBookService.incrementReadingCount(id);
        PortalBook book = portalBookService.selectPortalBookById(id);
        if (book == null) {
            return AjaxResult.error("书籍不存在");
        }
        // 获取该书籍相关金句
        List<PortalBookQuote> quotes = portalBookQuoteService.selectQuotesByBookId(id, 10);
        Map<String, Object> result = new HashMap<>();
        result.put("book", book);
        result.put("quotes", quotes);
        return AjaxResult.success(result);
    }

    /**
     * 获取书单详情（包含书籍列表）
     */
    @Operation(summary = "获取书单详情", description = "根据ID获取书单详情及包含的书籍列表")
    @GetMapping("/book-lists/{id}")
    @Anonymous
    public AjaxResult getBookListById(@Parameter(description = "书单ID") @PathVariable Long id) {
        portalBookListService.incrementViewCount(id);
        PortalBookList bookList = portalBookListService.selectPortalBookListById(id);
        if (bookList == null) {
            return AjaxResult.error("书单不存在");
        }
        List<PortalBookListItem> items = portalBookListService.selectBookListItems(id);
        // 查询书籍详情
        List<PortalBook> books = new java.util.ArrayList<>();
        for (PortalBookListItem item : items) {
            PortalBook b = portalBookService.selectPortalBookById(item.getBookId());
            if (b != null) {
                books.add(b);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("bookList", bookList);
        result.put("books", books);
        return AjaxResult.success(result);
    }

    /**
     * 书单收藏/取消收藏
     */
    @Operation(summary = "书单收藏", description = "收藏/取消收藏书单，需登录")
    @PostMapping("/book-lists/{id}/bookmark")
    public AjaxResult toggleBookListBookmark(@Parameter(description = "书单ID") @PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        PortalBookListBookmark existing = bookListBookmarkMapper.selectBookmark(id, userId);
        Map<String, Object> result = new HashMap<>();

        if (existing == null) {
            // 未收藏 → 收藏
            PortalBookListBookmark bookmark = new PortalBookListBookmark();
            bookmark.setBooklistId(id);
            bookmark.setUserId(userId);
            bookmark.setCreateTime(java.time.LocalDateTime.now());
            bookListBookmarkMapper.insert(bookmark);

            // 为书单创建者记录被收藏成长事件
            PortalBookList booklist = portalBookListService.selectPortalBookListById(id);
            if (booklist != null && booklist.getUserId() != null && !booklist.getUserId().equals(userId)) {
                portalGrowthService.recordEventWithTarget("reading", "booklist_bookmarked",
                        booklist.getUserId(), userId, "booklist", id);
            }

            result.put("bookmarked", true);
            result.put("message", "收藏成功");
        } else {
            // 已收藏 → 取消收藏
            bookListBookmarkMapper.deleteById(existing.getId());
            result.put("bookmarked", false);
            result.put("message", "已取消收藏");
        }

        // 返回最新收藏数
        result.put("bookmarkCount", bookListBookmarkMapper.countByBooklist(id));
        return AjaxResult.success(result);
    }

    /**
     * 检查当前用户是否已收藏书单
     */
    @Operation(summary = "检查书单收藏状态", description = "检查当前用户是否已收藏目标书单")
    @GetMapping("/book-lists/{id}/bookmark")
    public AjaxResult checkBookListBookmark(@Parameter(description = "书单ID") @PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("bookmarked", false);
        } else {
            PortalBookListBookmark existing = bookListBookmarkMapper.selectBookmark(id, userId);
            result.put("bookmarked", existing != null);
        }
        result.put("bookmarkCount", bookListBookmarkMapper.countByBooklist(id));
        return AjaxResult.success(result);
    }
}
