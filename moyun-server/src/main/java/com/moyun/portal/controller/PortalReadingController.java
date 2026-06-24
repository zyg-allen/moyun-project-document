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
     * 获取书籍列表（分页）
     */
    @Operation(summary = "获取书籍列表", description = "分页获取书籍列表，支持分类、标签筛选")
    @GetMapping("/books")
    @Anonymous
    public AjaxResult getBooks(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否精选") @RequestParam(required = false) Boolean isFeatured,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "12") Integer pageSize) {
        BookQuery query = new BookQuery();
        query.setCategoryId(categoryId);
        query.setTitle(keyword);
        query.setIsFeatured(isFeatured);
        query.setStatus("active");

        Page<PortalBook> result = portalBookService.selectPortalBookPage(new Page<>(page, pageSize), query);

        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        res.put("page", page);
        res.put("pageSize", pageSize);
        return AjaxResult.success(res);
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
     * 获取书单列表（分页）
     */
    @Operation(summary = "获取书单列表", description = "分页获取书单列表")
    @GetMapping("/book-lists")
    @Anonymous
    public AjaxResult getBookLists(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "是否公开") @RequestParam(defaultValue = "true") Boolean isPublic,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "12") Integer pageSize) {
        BookListQuery query = new BookListQuery();
        query.setCategoryId(categoryId);
        query.setTitle(keyword);
        query.setIsPublic(isPublic);
        query.setStatus("active");

        Page<PortalBookList> result = portalBookListService.selectPortalBookListPage(new Page<>(page, pageSize), query);

        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        res.put("page", page);
        res.put("pageSize", pageSize);
        return AjaxResult.success(res);
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
     * 获取金句列表（分页）
     */
    @Operation(summary = "获取金句列表", description = "分页获取公开金句摘录")
    @GetMapping("/quotes")
    @Anonymous
    public AjaxResult getQuotes(
            @Parameter(description = "书籍ID") @RequestParam(required = false) Long bookId,
            @Parameter(description = "是否精选") @RequestParam(required = false) Boolean isFeatured,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize) {
        BookQuoteQuery query = new BookQuoteQuery();
        query.setBookId(bookId);
        query.setIsFeatured(isFeatured);
        query.setIsPublic(true);
        query.setContent(keyword);

        Page<PortalBookQuote> result = portalBookQuoteService.selectPortalBookQuotePage(new Page<>(page, pageSize), query);

        Map<String, Object> res = new HashMap<>();
        res.put("list", result.getRecords());
        res.put("total", result.getTotal());
        res.put("page", page);
        res.put("pageSize", pageSize);
        return AjaxResult.success(res);
    }

    /**
     * 金句点赞
     */
    @Operation(summary = "金句点赞", description = "对金句进行点赞")
    @PostMapping("/quotes/{id}/like")
    @Anonymous
    public AjaxResult likeQuote(@Parameter(description = "金句ID") @PathVariable Long id) {
        portalBookQuoteService.incrementLikeCount(id);
        return AjaxResult.success("点赞成功");
    }

    /**
     * 书单点赞
     */
    @Operation(summary = "书单点赞", description = "对书单进行点赞")
    @PostMapping("/book-lists/{id}/like")
    @Anonymous
    public AjaxResult likeBookList(@Parameter(description = "书单ID") @PathVariable Long id) {
        portalBookListService.incrementLikeCount(id);
        return AjaxResult.success("点赞成功");
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
