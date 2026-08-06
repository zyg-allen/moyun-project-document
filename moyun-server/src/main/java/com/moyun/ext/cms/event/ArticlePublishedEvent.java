package com.moyun.ext.cms.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文章审核通过事件
 * <p>由 CmsArticleServiceImpl.auditArticle 在文章状态 pending -> published 时发布。
 * 监听器（{@link ArticleAuditEventListener}）在事务提交后异步处理 Feed 联动与积分补发，
 * 避免监听器异常影响审核主流程。
 *
 * <p>设计要点：
 * <ul>
 *   <li>幂等：监听器先查询 portal_feed_event / portal_growth_log 是否已存在同 entity 记录，
 *       有则跳过，避免 publishArticle 阶段已发过的场景下重复触发。</li>
 *   <li>解耦：审核服务不直接依赖 FeedService / GrowthService，便于扩展更多联动场景。</li>
 * </ul>
 *
 * @author moyun
 */
@Getter
public class ArticlePublishedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /** 文章ID */
    private final Long articleId;

    /** 作者ID（portal_user.id） */
    private final Long authorId;

    /** 文章标题（Feed 流展示用） */
    private final String title;

    /** 文章摘要（Feed 流展示用） */
    private final String excerpt;

    /** 文章封面（Feed 流展示用） */
    private final String cover;

    public ArticlePublishedEvent(Object source, Long articleId, Long authorId,
                                 String title, String excerpt, String cover) {
        super(source);
        this.articleId = articleId;
        this.authorId = authorId;
        this.title = title;
        this.excerpt = excerpt;
        this.cover = cover;
    }
}
