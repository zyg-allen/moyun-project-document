package com.moyun.ext.cms.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ext.cms.service.IFeedService;
import com.moyun.portal.domain.entity.PortalFeedEvent;
import com.moyun.portal.domain.entity.PortalGrowthLog;
import com.moyun.portal.mapper.PortalFeedEventMapper;
import com.moyun.portal.mapper.PortalGrowthLogMapper;
import com.moyun.portal.service.IPortalGrowthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文章审核事件监听器
 * <p>监听 {@link ArticlePublishedEvent}，在审核事务提交后处理联动：
 * <ul>
 *   <li>Feed 联动：调用 {@link IFeedService#publishEvent} 补发动态流，
 *       先查询 {@code portal_feed_event} 是否已存在同 entity 的事件以避免重复。</li>
 *   <li>积分联动：调用 {@link IPortalGrowthService#recordEvent} 补发 publish_article 成长值，
 *       先查询 {@code portal_growth_log} 是否已发过以避免重复。</li>
 * </ul>
 *
 * <p>设计原因：
 * <ul>
 *   <li>前台 publishArticle 路径在 pending 阶段已发过 Feed/积分，auditArticle 审核通过时
 *       不应重复发，监听器做幂等检查保证。</li>
 *   <li>使用 {@link TransactionalEventListener} + {@link TransactionPhase#AFTER_COMMIT}：
 *       审核事务回滚时不触发联动，避免脏数据。</li>
 *   <li>同步执行：监听器内部 try-catch 包裹所有副作用，不阻塞主流程返回（即使失败也仅记日志）。</li>
 * </ul>
 *
 * @author moyun
 */
@Slf4j
@Component
public class ArticleAuditEventListener {

    @Autowired
    private IFeedService feedService;

    @Autowired
    private PortalFeedEventMapper feedEventMapper;

    @Autowired
    private IPortalGrowthService portalGrowthService;

    @Autowired
    private PortalGrowthLogMapper growthLogMapper;

    /**
     * 文章审核通过：补发 Feed 流 + 积分联动（幂等）
     * <p>仅在审核事务提交后触发，避免回滚时产生脏数据
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticlePublished(ArticlePublishedEvent event) {
        if (event.getArticleId() == null || event.getAuthorId() == null) {
            log.warn("[ArticlePublishedEvent] 参数缺失：articleId={}, authorId={}",
                    event.getArticleId(), event.getAuthorId());
            return;
        }
        log.info("[ArticlePublishedEvent] 处理审核通过联动：articleId={}, authorId={}",
                event.getArticleId(), event.getAuthorId());
        publishFeedIfNeeded(event);
        recordGrowthIfNeeded(event);
    }

    /**
     * 补发 Feed 流（幂等：若 portal_feed_event 已存在同 entity 的事件则跳过）
     */
    private void publishFeedIfNeeded(ArticlePublishedEvent event) {
        try {
            // 查询是否已存在 targetType=article, targetId=articleId 的 Feed 事件
            Long existing = feedEventMapper.selectCount(
                    new LambdaQueryWrapper<PortalFeedEvent>()
                            .eq(PortalFeedEvent::getTargetType, "article")
                            .eq(PortalFeedEvent::getTargetId, event.getArticleId())
            );
            if (existing != null && existing > 0) {
                log.info("[ArticlePublishedEvent] Feed 已存在，跳过补发：articleId={}", event.getArticleId());
                return;
            }
            // 补发 Feed 事件
            feedService.publishEvent(
                    event.getAuthorId(),
                    "publish_article",
                    "article",
                    event.getArticleId(),
                    event.getTitle(),
                    event.getExcerpt(),
                    event.getCover()
            );
            log.info("[ArticlePublishedEvent] Feed 补发成功：articleId={}", event.getArticleId());
        } catch (Exception e) {
            log.error("[ArticlePublishedEvent] Feed 补发失败（不影响审核主流程）：articleId={}",
                    event.getArticleId(), e);
        }
    }

    /**
     * 补发 publish_article 成长值（幂等：若 portal_growth_log 已存在同 entity 的 publish_article 流水则跳过）
     */
    private void recordGrowthIfNeeded(ArticlePublishedEvent event) {
        try {
            // 查询是否已存在 module=article, action=publish_article, entityId=articleId 的成长流水
            Long existing = growthLogMapper.selectCount(
                    new LambdaQueryWrapper<PortalGrowthLog>()
                            .eq(PortalGrowthLog::getUserId, event.getAuthorId())
                            .eq(PortalGrowthLog::getModule, "article")
                            .eq(PortalGrowthLog::getAction, "publish_article")
                            .eq(PortalGrowthLog::getEntityType, "article")
                            .eq(PortalGrowthLog::getEntityId, event.getArticleId())
            );
            if (existing != null && existing > 0) {
                log.info("[ArticlePublishedEvent] 成长值已发放，跳过补发：articleId={}, authorId={}",
                        event.getArticleId(), event.getAuthorId());
                return;
            }
            // 补发成长值（recordEvent 内部含每日上限校验，可能因上限未获得，但不视为错误）
            int delta = portalGrowthService.recordEvent(
                    "article", "publish_article",
                    event.getAuthorId(), "article", event.getArticleId()
            );
            log.info("[ArticlePublishedEvent] 成长值补发完成：articleId={}, authorId={}, delta={}",
                    event.getArticleId(), event.getAuthorId(), delta);
        } catch (Exception e) {
            log.error("[ArticlePublishedEvent] 成长值补发失败（不影响审核主流程）：articleId={}",
                    event.getArticleId(), e);
        }
    }
}
