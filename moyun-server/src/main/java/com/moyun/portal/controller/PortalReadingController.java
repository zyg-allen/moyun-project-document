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
import com.moyun.portal.domain.entity.PortalBookChapter;
import com.moyun.portal.domain.entity.PortalBookList;
import com.moyun.portal.domain.entity.PortalBookListBookmark;
import com.moyun.portal.domain.entity.PortalBookListItem;
import com.moyun.portal.domain.entity.PortalBookQuote;
import com.moyun.portal.domain.entity.PortalBookRecommend;
import com.moyun.portal.domain.query.BookChapterQuery;
import com.moyun.portal.domain.query.BookListQuery;
import com.moyun.portal.domain.query.BookQuery;
import com.moyun.portal.domain.query.BookQuoteQuery;
import com.moyun.portal.mapper.PortalBookListBookmarkMapper;
import com.moyun.portal.service.IPortalBookChapterService;
import com.moyun.portal.service.IPortalBookListService;
import com.moyun.portal.service.IPortalBookQuoteService;
import com.moyun.portal.service.IPortalBookRecommendService;
import com.moyun.portal.service.IPortalBookService;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.service.IPortalReadingProgressService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.portal.domain.entity.PortalReadingProgress;

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

    @Autowired
    private IPortalBookChapterService bookChapterService;

    @Autowired
    private IPortalReadingProgressService readingProgressService;

    @Autowired
    private IPortalBookRecommendService bookRecommendService;

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

    // ========================================================================
    // 章节阅读相关接口（v1.0 新增）
    // ========================================================================

    /**
     * 获取书籍的章节目录
     */
    @Operation(summary = "获取章节目录", description = "返回书籍的已发布章节列表（不含正文，仅元信息）")
    @GetMapping("/books/{bookId}/chapters")
    @Anonymous
    public AjaxResult getChapterList(@Parameter(description = "书籍ID") @PathVariable Long bookId) {
        BookChapterQuery query = new BookChapterQuery();
        query.setBookId(bookId);
        query.setIsPublished(true);
        query.setWithContent(false);
        List<PortalBookChapter> chapters = bookChapterService.selectChapterList(query);
        return AjaxResult.success(chapters);
    }

    /**
     * 获取章节详情（含正文）
     *
     * <p>VIP 章节访问逻辑（第一阶段简化：返回完整正文，VIP 校验第四阶段实现）：</p>
     * <ul>
     *   <li>章节已发布：返回完整正文</li>
     *   <li>章节未发布：返回错误</li>
     * </ul>
     */
    @Operation(summary = "获取章节详情", description = "根据章节ID获取章节正文")
    @GetMapping("/chapters/{chapterId}")
    @Anonymous
    public AjaxResult getChapterDetail(@Parameter(description = "章节ID") @PathVariable Long chapterId) {
        PortalBookChapter chapter = bookChapterService.selectChapterById(chapterId);
        if (chapter == null) {
            return AjaxResult.error("章节不存在");
        }
        if (!Boolean.TRUE.equals(chapter.getIsPublished())) {
            return AjaxResult.error("章节未发布");
        }
        // 浏览量 +1（异步容错）
        // 说明：书籍阅读数 incrementReadingCount 已在 getBookById 接口中计入，
        // 此处仅累加章节浏览量，避免 ChapterReaderPage 同时调用两个接口时书籍阅读数 +2
        try {
            bookChapterService.incrementViewCount(chapterId);
        } catch (Exception ignored) {
        }
        return AjaxResult.success(chapter);
    }

    /**
     * 获取章节导航（上一章/下一章）
     */
    @Operation(summary = "章节导航", description = "返回上一章/下一章信息")
    @GetMapping("/chapters/{chapterId}/nav")
    @Anonymous
    public AjaxResult getChapterNav(@Parameter(description = "章节ID") @PathVariable Long chapterId) {
        PortalBookChapter current = bookChapterService.selectChapterById(chapterId);
        if (current == null) {
            return AjaxResult.error("章节不存在");
        }
        Long bookId = current.getBookId();
        Integer chapterNo = current.getChapterNo();
        PortalBookChapter prev = bookChapterService.selectPrevChapter(bookId, chapterNo);
        PortalBookChapter next = bookChapterService.selectNextChapter(bookId, chapterNo);
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> prevInfo = new HashMap<>();
        if (prev != null) {
            prevInfo.put("id", prev.getId());
            prevInfo.put("chapterNo", prev.getChapterNo());
            prevInfo.put("title", prev.getTitle());
        }
        Map<String, Object> nextInfo = new HashMap<>();
        if (next != null) {
            nextInfo.put("id", next.getId());
            nextInfo.put("chapterNo", next.getChapterNo());
            nextInfo.put("title", next.getTitle());
        }
        result.put("prev", prevInfo.isEmpty() ? null : prevInfo);
        result.put("next", nextInfo.isEmpty() ? null : nextInfo);
        result.put("current", Map.of(
                "id", current.getId(),
                "chapterNo", current.getChapterNo(),
                "title", current.getTitle(),
                "bookId", bookId
        ));
        return AjaxResult.success(result);
    }

    // ========================================================================
    // 阅读进度相关接口（v1.0 第二阶段新增）
    // ========================================================================

    /**
     * 上报章节级阅读进度（前端 30s 节流调用，章节切换时强制调用）
     */
    @Operation(summary = "上报章节级阅读进度", description = "前端节流 30s 上报，章节切换时强制上报")
    @PostMapping("/progress")
    public AjaxResult reportProgress(@RequestBody PortalReadingProgress progress) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        if (progress.getBookId() == null) {
            return AjaxResult.error("书籍ID不能为空");
        }
        progress.setUserId(userId);
        // 防止前端误传 id 导致 upsert 行为异常
        progress.setId(null);
        readingProgressService.upsertChapterProgress(progress);
        return AjaxResult.success();
    }

    /**
     * 查询最近阅读记录（首页/读书空间入口展示用）
     * 注：此路径必须声明在 /progress/{bookId} 之前，避免 "recent" 被当作 bookId 匹配
     */
    @Operation(summary = "最近阅读记录", description = "返回当前用户最近阅读的书籍列表")
    @GetMapping("/progress/recent")
    public AjaxResult getRecentReading(@Parameter(description = "返回条数，默认10，最大50") @RequestParam(defaultValue = "10") int limit) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            // 未登录返回空列表
            return AjaxResult.success(java.util.Collections.emptyList());
        }
        List<PortalReadingProgress> recent = readingProgressService.selectRecentReading(userId, limit);
        return AjaxResult.success(recent);
    }

    /**
     * 查询当前用户对某书的阅读进度（用于续读恢复）
     */
    @Operation(summary = "查询书籍阅读进度", description = "返回当前用户对某书的章节级阅读进度")
    @GetMapping("/progress/{bookId}")
    public AjaxResult getProgress(@Parameter(description = "书籍ID") @PathVariable Long bookId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            // 未登录返回空进度
            return AjaxResult.success(null);
        }
        PortalReadingProgress progress = readingProgressService.selectByUserAndBook(userId, bookId);
        return AjaxResult.success(progress);
    }

    // ========================================================================
    // 发现与运营接口（v1.0 第三阶段新增）
    // ========================================================================

    /**
     * 发现页聚合数据（单次请求返回 Banner + 热门排行 + 限免 + 最近更新）
     */
    @Operation(summary = "发现页聚合数据", description = "返回发现页所需的多区块数据")
    @GetMapping("/discover")
    @Anonymous
    public AjaxResult getDiscoverData() {
        Map<String, Object> data = new HashMap<>();
        // 1. 发现页 Banner（position=discover_banner）
        data.put("banners", bookRecommendService.selectActiveByPosition("discover_banner"));
        // 2. 热门排行 Top10（按 reading_count desc）
        BookQuery hotQuery = new BookQuery();
        hotQuery.setStatus("active");
        hotQuery.setOrderBy("hot");
        Page<PortalBook> hotPage = portalBookService.selectPortalBookPage(new Page<>(1, 10), hotQuery);
        data.put("hotRanking", hotPage.getRecords());
        // 3. 限免专区（position=limit_free）
        data.put("limitFree", bookRecommendService.selectActiveByPosition("limit_free"));
        // 4. 最近更新 Top10（按 last_update_time desc，仅 novel 类型）
        //    注：orderBy=new 会按 create_time desc，作为最近更新的近似排序
        BookQuery recentQuery = new BookQuery();
        recentQuery.setStatus("active");
        recentQuery.setType("novel");
        recentQuery.setOrderBy("new");
        Page<PortalBook> recentPage = portalBookService.selectPortalBookPage(new Page<>(1, 10), recentQuery);
        data.put("recentUpdate", recentPage.getRecords());
        return AjaxResult.success(data);
    }

    /**
     * 排行榜
     * @param type 排行类型：hot=热门(reading_count)/new=新书(create_time)/completed=完结(is_finished=1)/word_count=字数榜
     */
    @Operation(summary = "排行榜", description = "按类型返回书籍排行")
    @GetMapping("/ranking")
    @Anonymous
    public AjaxResult getRanking(@Parameter(description = "排行类型") @RequestParam(defaultValue = "hot") String type,
                                  @Parameter(description = "返回条数") @RequestParam(defaultValue = "10") int limit) {
        if (limit <= 0 || limit > 50) limit = 10;
        // 复用 selectPortalBookPage，通过 BookQuery 的不同字段组合实现排行
        BookQuery query = new BookQuery();
        query.setStatus("active");
        if ("completed".equals(type)) {
            query.setIsFinished(true);
        }
        if ("novel".equals(type) || "word_count".equals(type)) {
            query.setType("novel");
        }
        // 设置排序方式：hot/默认→阅读数；new→创建时间；word_count→字数
        if ("new".equals(type)) {
            query.setOrderBy("new");
        } else if ("word_count".equals(type)) {
            query.setOrderBy("word_count");
        } else {
            query.setOrderBy("hot");
        }
        Page<PortalBook> page = portalBookService.selectPortalBookPage(new Page<>(1, limit), query);
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return AjaxResult.success(result);
    }

    /**
     * 限免专区（position=limit_free 且在有效期内）
     */
    @Operation(summary = "限免专区", description = "返回当前有效的限免推荐书籍")
    @GetMapping("/limit-free")
    @Anonymous
    public AjaxResult getLimitFree() {
        List<PortalBookRecommend> limitFree = bookRecommendService.selectActiveByPosition("limit_free");
        return AjaxResult.success(limitFree);
    }
}
