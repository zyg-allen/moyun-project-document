package com.moyun.portal.controller;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.moyun.common.constant.HttpStatus;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalArticle;
import com.moyun.portal.domain.entity.PortalBookmark;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.portal.mapper.PortalBookmarkMapper;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.portal.service.IPortalGrowthService;
import com.moyun.portal.util.PortalSecurityUtils;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISysNotificationService;

/**
 * 门户收藏 Controller
 *
 * <p>说明：原通用 CRUD 接口（list/export/getInfo/add/edit/remove/checkBookmarkStatus）已删除，
 * 前端从未调用。文章收藏统一走 {@link #toggleBookmark(Long)} 接口（幂等 toggle）。
 * Service / Mapper / XML 保留，因 Service 方法可能被其他模块复用。</p>
 *
 * @author moyun
 */
@Tag(name = "门户收藏", description = "门户文章收藏接口")
@Slf4j
@RestController
@RequestMapping("/portal/bookmark")
public class PortalBookmarkController extends BaseController {

    @Autowired
    private PortalBookmarkMapper portalBookmarkMapper;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Autowired
    private ISysNotificationService notificationService;

    /**
     * 文章收藏 - 行业标准实现
     * 1. 检查是否已收藏
     * 2. 如果未收藏，则收藏
     * 3. 如果已收藏，则取消收藏
     * 4. 返回最新收藏状态
     */
    @Operation(summary = "文章收藏/取消收藏", description = "收藏或取消收藏文章，返回最新状态")
    @PostMapping("/{articleId}/toggle")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult toggleBookmark(@PathVariable Long articleId) {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        // 校验文章存在性，避免对已删除文章产生脏收藏记录
        com.moyun.portal.domain.entity.PortalArticle article = portalArticleMapper.selectById(articleId);
        if (article == null) {
            return error("文章不存在");
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
            try {
                portalBookmarkMapper.insert(bookmark);
            } catch (DuplicateKeyException e) {
                // 并发场景：另一事务已先插入（uk_user_article 唯一索引触发）
                // 按"已收藏→取消"语义处理，保证幂等
                PortalBookmark conflict = portalBookmarkMapper.selectOne(wrapper);
                if (conflict != null) {
                    portalBookmarkMapper.deleteById(conflict.getId());
                }
                // 下界保护：仅当原计数 > 0 时才递减，避免出现负数
                if (article.getBookmarkCount() != null && article.getBookmarkCount() > 0) {
                    portalArticleMapper.incrementBookmarkCount(articleId, -1);
                }
                Map<String, Object> r = new HashMap<>();
                r.put("isBookmarked", false);
                r.put("articleId", articleId);
                return success(r);
            }

            // 原子增加文章收藏数
            portalArticleMapper.incrementBookmarkCount(articleId, 1);

            // 为文章作者记录被收藏成长事件
            if (article.getAuthorId() != null && !article.getAuthorId().equals(userId)) {
                portalGrowthService.recordEventWithTarget("article", "receive_bookmark",
                        article.getAuthorId(), userId, "article", articleId);
                // 主动通知作者（非阻塞，失败不影响主流程）
                sendBookmarkNotification(article.getAuthorId(), userId, articleId, article.getTitle());
            }

            isBookmarked = true;
        } else {
            // 已收藏，取消收藏
            portalBookmarkMapper.deleteById(existingBookmark.getId());

            // 原子减少文章收藏数（下界保护：仅当原计数 > 0 时才递减）
            if (article.getBookmarkCount() != null && article.getBookmarkCount() > 0) {
                portalArticleMapper.incrementBookmarkCount(articleId, -1);
            }

            isBookmarked = false;
        }

        // 返回最新状态
        Map<String, Object> result = new HashMap<>();
        result.put("isBookmarked", isBookmarked);
        result.put("articleId", articleId);
        return success(result);
    }

    /**
     * 我的收藏列表 - 获取当前用户收藏的文章列表
     * 1. 查询当前用户的所有收藏记录（按收藏时间倒序）
     * 2. 批量查询已发布文章详情（含作者/分类信息）
     * 3. 仅返回 published 状态文章，避免展示草稿/已删除文章
     */
    @Operation(summary = "我的收藏列表", description = "获取当前用户收藏的文章列表（仅已发布）")
    @GetMapping("/my")
    public AjaxResult getMyBookmarks() {
        Long userId = PortalSecurityUtils.getUserId();
        if (userId == null) {
            return AjaxResult.error(HttpStatus.UNAUTHORIZED, "请先登录");
        }

        // 1. 查询当前用户所有收藏记录（按收藏时间倒序）
        LambdaQueryWrapper<PortalBookmark> bw = new LambdaQueryWrapper<>();
        bw.eq(PortalBookmark::getUserId, userId).orderByDesc(PortalBookmark::getCreateTime);
        List<PortalBookmark> bookmarks = portalBookmarkMapper.selectList(bw);

        Map<String, Object> result = new HashMap<>();
        if (bookmarks.isEmpty()) {
            result.put("list", Collections.emptyList());
            result.put("total", 0);
            return success(result);
        }

        // 2. 提取文章ID
        List<Long> articleIds = bookmarks.stream()
                .map(PortalBookmark::getArticleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (articleIds.isEmpty()) {
            result.put("list", Collections.emptyList());
            result.put("total", 0);
            return success(result);
        }

        // 3. 批量查询已发布文章详情（JOIN 作者与分类）
        List<PortalArticle> articles = portalArticleMapper.selectArticlesByIds(articleIds);

        result.put("list", articles);
        result.put("total", articles.size());
        return success(result);
    }

    /**
     * 发送收藏通知给文章作者
     * <p>非阻塞：失败仅记日志，不影响主流程
     *
     * @param authorId   文章作者ID（接收方）
     * @param fromUserId 收藏者ID（发起方）
     * @param articleId  文章ID
     * @param articleTitle 文章标题
     */
    private void sendBookmarkNotification(Long authorId, Long fromUserId,
                                          Long articleId, String articleTitle) {
        try {
            PortalUser fromUser = portalUserMapper.selectById(fromUserId);
            String fromNickname = fromUser != null && fromUser.getNickname() != null
                    ? fromUser.getNickname()
                    : (fromUser != null ? fromUser.getUsername() : "用户" + fromUserId);
            SysNotification notification = new SysNotification();
            notification.setType("bookmark");
            notification.setScope("user");
            notification.setUserId(authorId);
            notification.setUserType("portal");
            notification.setNoticeType("1");
            notification.setStatus("0");
            notification.setTitle(fromNickname + " 收藏了你的文章");
            notification.setContent(fromNickname + " 收藏了你的文章《" + articleTitle + "》");
            notification.setData("{\"bizType\":\"bookmark\",\"articleId\":" + articleId
                    + ",\"fromUserId\":" + fromUserId + "}");
            notificationService.insertNotification(notification);
        } catch (Exception e) {
            log.warn("收藏通知发送失败（不影响主流程）：authorId={}, articleId={}, err={}",
                    authorId, articleId, e.getMessage());
        }
    }
}
