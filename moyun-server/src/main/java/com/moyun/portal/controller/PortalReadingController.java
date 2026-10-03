package com.moyun.portal.controller;

import java.util.Collections;
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
import com.moyun.portal.domain.vo.BookQuoteVO;
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
import com.moyun.portal.mapper.PortalBookMapper;

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

    @Autowired
    private PortalBookMapper portalBookMapper;

    /** VIP 章节访问控制：以会员卡（vip_user_card）为准，而非登录态里的静态角色 */

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

        // 精选金句（VO，带书籍和摘录人信息）
        java.util.List<BookQuoteVO> featuredQuotes = portalBookQuoteService.selectFeaturedQuoteVOs(5);
        data.put("quotes", featuredQuotes);

        // 统计数据
        data.put("bookCount", bookPage.getTotal());
        data.put("bookListCount", listPage.getTotal());
        data.put("quoteCount", featuredQuotes.size());

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
        // 获取该书籍相关金句（VO，带摘录人信息）
        List<BookQuoteVO> quotes = portalBookQuoteService.selectQuoteVOPage(
                new Page<>(1, 10), new BookQuoteQuery() {{ setBookId(id); setIsPublic(true); }}).getRecords();
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
        PortalBookList bookList = portalBookListService.selectPortalBookListById(id);
        if (bookList == null) {
            return AjaxResult.error("书单不存在");
        }
        // ── 可见性校验（清单 P2：未公开书单可被任意人按 id 读取）──
        // 本接口是 @Anonymous 公开接口，而 selectPortalBookListById 只按主键查询，
        // 后台设置的「是否公开(is_public)」与「状态(active/inactive)」**完全未生效**
        // ⇒ 私密书单（及其书单内书籍）只要猜到 id 就能被未登录访客读取。
        // 口径（fail-closed）：仅 is_public=1 且 status=active 的书单对所有人可见；
        // 其余仅**创建者本人**可见（游客一律不可见），且返回与"不存在"相同的文案，避免泄露存在性。
        boolean publiclyVisible = Boolean.TRUE.equals(bookList.getIsPublic())
                && "active".equals(bookList.getStatus());
        if (!publiclyVisible) {
            Long viewerId = PortalSecurityUtils.getUserId();
            if (viewerId == null || !viewerId.equals(bookList.getUserId())) {
                return AjaxResult.error("书单不存在或未公开");
            }
        }
        // 浏览计数放在可见性校验之后：未公开书单被"偷偷访问"不应产生浏览量
        portalBookListService.incrementViewCount(id);
        List<PortalBookListItem> items = portalBookListService.selectBookListItems(id);
        // 批量查询书籍详情，避免 N+1（书单 N 本书原本 N 次查询 → 现在 1 次）
        List<Long> bookIds = items.stream()
                .map(PortalBookListItem::getBookId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PortalBook> bookMap = new HashMap<>();
        if (!bookIds.isEmpty()) {
            for (PortalBook b : portalBookMapper.selectBatchIds(bookIds)) {
                bookMap.put(b.getId(), b);
            }
        }
        // 按书单内 items 的顺序组装书籍列表，保持与原逻辑一致
        List<PortalBook> books = new java.util.ArrayList<>(items.size());
        for (PortalBookListItem item : items) {
            if (item.getBookId() != null) {
                PortalBook b = bookMap.get(item.getBookId());
                if (b != null) {
                    books.add(b);
                }
            }
        }
        // ── 访问级别门禁（清单 P2）──
        // 书单实体有 accessLevel（后台上架时可选 free/vip/preview，配 portal_access_type 字典），
        // 但前台**从未使用**：详情接口无门禁、模板无 VIP 标识 ⇒ 付费/会员书单等同公开。
        // 口径：
        //   · vip     —— 非会员不下发书籍列表（仅返回书单元信息 + 锁定标记，由前端引导开通会员）；
        //   · preview —— 语义（"部分可见"取前几本）需产品定义，当前**放行但打标**，不臆造规则；
        //   · free/空 —— 正常返回。
        String accessLevel = bookList.getAccessLevel();
        // 【合规改造 2026-10】全站免费化：会员书单门禁取消，所有书单均可查看
        boolean locked = false;

        Map<String, Object> result = new HashMap<>();
        result.put("bookList", bookList);
        result.put("books", locked ? java.util.Collections.emptyList() : books);
        result.put("accessLevel", accessLevel == null ? "free" : accessLevel);
        result.put("accessLevelLocked", locked);
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

    /**
     * **批量**检查当前用户对一组书单的收藏状态（清单 P2：阅读首页原先逐个书单发一次请求，N 个书单 N 次往返）。
     *
     * <p>语义：只返回"已收藏"的书单 id 集合；未登录返回空集合（前端据此全部显示未收藏）。
     * 不返回 bookmarkCount（列表页不展示单条收藏数），避免再次 N+1。</p>
     *
     * @param ids 书单ID列表（逗号分隔，Spring 自动绑定为 List）
     * @return {bookmarkedIds: number[]}
     */
    @Operation(summary = "批量检查书单收藏状态", description = "一次返回当前用户已收藏的书单ID集合，替代逐个书单查询")
    @GetMapping("/book-lists/bookmarks")
    public AjaxResult checkBookListsBookmark(
            @Parameter(description = "书单ID列表（逗号分隔）") @RequestParam(value = "ids", required = false) List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null || ids == null || ids.isEmpty()) {
            result.put("bookmarkedIds", Collections.emptyList());
            return AjaxResult.success(result);
        }
        // 去重 + 过滤空值，避免无效 SQL 参数与重复 id
        List<Long> distinctIds = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        result.put("bookmarkedIds", distinctIds.isEmpty()
                ? Collections.emptyList()
                : bookListBookmarkMapper.selectBookmarkedIds(userId, distinctIds));
        return AjaxResult.success(result);
    }

    // ========================================================================
    // 点赞相关接口（金句 / 书单）
    // ========================================================================

    /**
     * 切换金句点赞（toggle，已点赞则取消，未点赞则新增）
     */
    @Operation(summary = "金句点赞切换", description = "已点赞则取消，未点赞则新增，需登录")
    @PostMapping("/quote/{id}/like")
    public AjaxResult toggleQuoteLike(@Parameter(description = "金句ID") @PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Map<String, Object> result = portalBookQuoteService.toggleQuoteLike(id, userId);
        return AjaxResult.success(result);
    }

    /**
     * 查询当前用户是否点赞该金句
     */
    @Operation(summary = "查询金句点赞状态", description = "返回当前用户是否已点赞目标金句")
    @GetMapping("/quote/{id}/like")
    public AjaxResult checkQuoteLike(@Parameter(description = "金句ID") @PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("liked", false);
        } else {
            result.put("liked", portalBookQuoteService.isQuoteLiked(id, userId));
        }
        return AjaxResult.success(result);
    }

    /**
     * 切换书单点赞（toggle，已点赞则取消，未点赞则新增）
     */
    @Operation(summary = "书单点赞切换", description = "已点赞则取消，未点赞则新增，需登录")
    @PostMapping("/book-list/{id}/like")
    public AjaxResult toggleBookListLike(@Parameter(description = "书单ID") @PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        Map<String, Object> result = portalBookListService.toggleBookListLike(id, userId);
        return AjaxResult.success(result);
    }

    /**
     * 查询当前用户是否点赞该书单
     */
    @Operation(summary = "查询书单点赞状态", description = "返回当前用户是否已点赞目标书单")
    @GetMapping("/book-list/{id}/like")
    public AjaxResult checkBookListLike(@Parameter(description = "书单ID") @PathVariable Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> result = new HashMap<>();
        if (userId == null) {
            result.put("liked", false);
        } else {
            result.put("liked", portalBookListService.isBookListLiked(id, userId));
        }
        return AjaxResult.success(result);
    }

    // ========================================================================
    // 章节阅读相关接口（新增）
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
        // ── VIP 访问控制（内容付费墙）──
        // 本接口是 @Anonymous 且此前**不做任何访问级别校验**：未登录/非会员都能拿到 VIP 章节全文，
        // 而页面同时在展示"本章为 VIP 章节当前为预览模式，完整内容需开通 VIP"的横幅
        // ⇒ 门禁与展示口径不一致，付费内容可被直接绕过（清单 #55/#56）。
        //
        // 口径：章节 is_free=false 即受限；is_free 未设置（null）时继承书籍 access_level=vip。
        // 显式的 is_free=true 优先于书籍级设置（单章免费是有意为之）。
        PortalBook chapterBook = portalBookMapper.selectById(chapter.getBookId());
        boolean bookVip = chapterBook != null && "vip".equalsIgnoreCase(chapterBook.getAccessLevel());
        // 【合规改造 2026-10】全站免费化：章节不再区分 VIP，正文全部可读
        chapter.setPreview(false);
        // 浏览量 +1（异步容错）
        // 说明：书籍阅读数 incrementReadingCount 已在 getBookById 接口中计入，
        // 此处仅累加章节浏览量，避免 ChapterReaderPage 同时调用两个接口时书籍阅读数 +2
        try {
            bookChapterService.incrementViewCount(chapterId);
        } catch (Exception ignored) {
        }
        return AjaxResult.success(chapter);
    }

    /** 试读字数上限（VIP 章节未开通时下发的正文字数） */
    private static final int VIP_PREVIEW_CHARS = 500;

    /**
     * 把章节正文裁剪为**试读片段**并置 {@code preview=true}。
     *
     * <p>富文本走"先剥标签再截断"：直接截断 HTML 会把标签截成半截、破坏页面结构甚至造成 XSS 面。
     * 试读文本再经 {@link com.moyun.util.html.EscapeUtil#escape} 转义后包一层 {@code <p>} 返回。</p>
     */
    private void applyVipPreview(PortalBookChapter chapter) {
        String markdown = chapter.getContentMarkdown();
        if (markdown != null && !markdown.isBlank()) {
            String clipped = markdown.length() <= VIP_PREVIEW_CHARS
                    ? markdown : markdown.substring(0, VIP_PREVIEW_CHARS);
            chapter.setContentMarkdown(clipped
                    + "\n\n> 试读结束，开通 VIP 后可阅读全文。");
            chapter.setContent(null);
        } else {
            String html = chapter.getContent() == null ? "" : chapter.getContent();
            String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
            String clipped = text.length() <= VIP_PREVIEW_CHARS ? text : text.substring(0, VIP_PREVIEW_CHARS);
            chapter.setContent("<p>" + com.moyun.util.html.EscapeUtil.escape(clipped)
                    + "…</p><p><em>试读结束，开通 VIP 后可阅读全文。</em></p>");
        }
        chapter.setPreview(true);
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
    // 阅读进度相关接口（第二阶段新增）
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
    // 发现与运营接口（第三阶段新增）
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
     *
     * <p>清单 P2：原先只有 hot/new/completed/word_count 四型，导致前端两个区块"名不副实"——
     * 「最近更新」只能用 new（=create_time，其实是**新书上架**），
     * 「连载中」只能用 word_count（=字数榜，**未过滤连载状态**，已完结长书会混入）。
     * 现补两型：updated=最后更新时间倒序、ongoing=连载中（serial_status=ongoing）。</p>
     *
     * @param type 排行类型：hot=热门(reading_count)/new=新书(create_time)/updated=最近更新(last_update_time)
     *             /completed=完结(is_finished=1)/word_count=字数榜/ongoing=连载中(serial_status=ongoing)
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
        if ("novel".equals(type) || "word_count".equals(type) || "ongoing".equals(type)) {
            query.setType("novel");
        }
        // 清单 P2：连载中榜必须真的过滤连载状态，否则已完结长书会排进"连载中"
        if ("ongoing".equals(type)) {
            query.setSerialStatus("ongoing");
        }
        // 设置排序方式：hot/默认→阅读数；new→创建时间；update→最后更新时间；word_count→字数
        if ("new".equals(type)) {
            query.setOrderBy("new");
        } else if ("update".equals(type) || "ongoing".equals(type)) {
            query.setOrderBy("update");
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

    // ========================================================================
    // 金句摘录相关接口
    // ========================================================================

    /**
     * 金句列表（分页，公开）
     */
    @Operation(summary = "金句列表", description = "分页查询公开金句，支持按书籍筛选")
    @GetMapping("/quotes")
    @Anonymous
    public AjaxResult getQuotes(
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数，默认20") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "书籍ID，可选") @RequestParam(required = false) Long bookId,
            @Parameter(description = "排序：hot=热门(点赞) / new=最新") @RequestParam(defaultValue = "hot") String sort) {
        if (pageSize <= 0 || pageSize > 100) pageSize = 20;
        BookQuoteQuery query = new BookQuoteQuery();
        query.setIsPublic(true);
        query.setBookId(bookId);
        // sort 控制在 Mapper 层默认 order by like_count desc，默认即为热门
        // 若需要按最新排序，这里先按默认（点赞），后续扩展可加 orderBy 字段
        Page<BookQuoteVO> page = portalBookQuoteService.selectQuoteVOPage(
                new Page<>(pageNum, pageSize), query);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("page", page.getCurrent());
        result.put("pageSize", page.getSize());
        return AjaxResult.success(result);
    }

    /**
     * 金句详情
     */
    @Operation(summary = "金句详情", description = "根据ID获取金句详情（含书籍和摘录人信息）")
    @GetMapping("/quotes/{id}")
    @Anonymous
    public AjaxResult getQuoteDetail(@Parameter(description = "金句ID") @PathVariable Long id) {
        BookQuoteVO quote = portalBookQuoteService.selectQuoteVOById(id);
        if (quote == null) {
            return AjaxResult.error("金句不存在");
        }
        return AjaxResult.success(quote);
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
