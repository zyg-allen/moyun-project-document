package com.moyun.ext.job.task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.portal.domain.entity.PortalTopic;
import com.moyun.portal.domain.entity.PortalTopicPost;
import com.moyun.portal.mapper.PortalTopicMapper;
import com.moyun.portal.mapper.PortalTopicPostMapper;
import com.moyun.system.domain.entity.SysNotification;
import com.moyun.system.service.ISensitiveWordService;
import com.moyun.system.service.ISysNotificationService;

import lombok.extern.slf4j.Slf4j;

/**
 * 敏感词定时扫描任务（轻量级）
 * <p>供 RuoYi Quartz 调度，sys_job.invoke_target 配置示例：
 * <ul>
 *   <li>{@code sensitiveScanTask.scanTopics()} —— 扫描已发布话题，命中转 pending 复审</li>
 *   <li>{@code sensitiveScanTask.scanTopicPosts()} —— 扫描已发布观点，命中软删并通知作者</li>
 * </ul>
 *
 * <p>设计要点：
 * <ul>
 *   <li>轻量级：仅扫 title+description（话题）/ content（观点），不解析富媒体。</li>
 *   <li>分批扫描：每批 200 条，避免大表全量加载导致内存压力。</li>
 *   <li>幂等：已 pending / 已软删的不再扫描。</li>
 *   <li>非阻断：单条扫描异常不影响整批，记录日志后继续。</li>
 * </ul>
 *
 * @author moyun
 */
@Slf4j
@Component("sensitiveScanTask")
public class SensitiveScanTask {

    /** 单批扫描条数 */
    private static final int BATCH_SIZE = 200;

    @Autowired
    private PortalTopicMapper topicMapper;

    @Autowired
    private PortalTopicPostMapper topicPostMapper;

    @Autowired
    private ISensitiveWordService sensitiveWordService;

    @Autowired
    private ISysNotificationService notificationService;

    /**
     * 扫描已发布话题（status=active），命中敏感词则转 pending 复审。
     * <p>适用场景：词库更新后回溯历史内容，或新词上线后的存量扫描。</p>
     */
    public void scanTopics() {
        log.info("[敏感词扫描] 话题扫描开始");
        long total = 0;
        long hitCount = 0;
        Long lastId = 0L;
        while (true) {
            LambdaQueryWrapper<PortalTopic> qw = new LambdaQueryWrapper<>();
            qw.eq(PortalTopic::getStatus, "active")
                    .gt(PortalTopic::getId, lastId)
                    .orderByAsc(PortalTopic::getId)
                    .last("LIMIT " + BATCH_SIZE);
            List<PortalTopic> batch = topicMapper.selectList(qw);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (PortalTopic topic : batch) {
                lastId = topic.getId();
                total++;
                try {
                    String scanText = buildTopicScanText(topic);
                    List<String> hits = sensitiveWordService.detectAndLog(
                            "topic", topic.getId(), topic.getCreatorId(), scanText, "pending");
                    if (hits != null && !hits.isEmpty()) {
                        hitCount++;
                        // 转 pending 复审（乐观锁：仅 active 可转，避免并发覆盖）
                        LambdaUpdateWrapper<PortalTopic> uw = new LambdaUpdateWrapper<>();
                        uw.eq(PortalTopic::getId, topic.getId())
                                .eq(PortalTopic::getStatus, "active")
                                .set(PortalTopic::getStatus, "pending")
                                .set(PortalTopic::getAuditRemark,
                                        "定时扫描命中敏感词，转待复审（命中：" + String.join(",", hits) + "）")
                                .set(PortalTopic::getAuditTime, LocalDateTime.now());
                        topicMapper.update(null, uw);
                        log.warn("[敏感词扫描] 话题命中：topicId={}, hits={}", topic.getId(), hits);
                        notifyCreator(topic.getCreatorId(), "topic", "话题", topic.getId(),
                                topic.getTitle(), "已转待复审，请前往「我的话题」查看");
                    }
                } catch (Exception e) {
                    log.error("[敏感词扫描] 话题扫描异常：topicId={}, err={}", topic.getId(), e.getMessage());
                }
            }
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        log.info("[敏感词扫描] 话题扫描结束，扫描 {} 条，命中 {} 条", total, hitCount);
    }

    /**
     * 扫描已发布观点（is_deleted=0），命中敏感词则软删并通知作者。
     * <p>观点无审核流，命中即软删，与发布时拦截策略一致。</p>
     */
    public void scanTopicPosts() {
        log.info("[敏感词扫描] 观点扫描开始");
        long total = 0;
        long hitCount = 0;
        Long lastId = 0L;
        while (true) {
            LambdaQueryWrapper<PortalTopicPost> qw = new LambdaQueryWrapper<>();
            qw.eq(PortalTopicPost::getIsDeleted, 0)
                    .gt(PortalTopicPost::getId, lastId)
                    .orderByAsc(PortalTopicPost::getId)
                    .last("LIMIT " + BATCH_SIZE);
            List<PortalTopicPost> batch = topicPostMapper.selectList(qw);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            for (PortalTopicPost post : batch) {
                lastId = post.getId();
                total++;
                try {
                    List<String> hits = sensitiveWordService.detectAndLog(
                            "topic_post", post.getId(), post.getUserId(), post.getContent(), "block");
                    if (hits != null && !hits.isEmpty()) {
                        hitCount++;
                        // 软删（乐观锁：仅 is_deleted=0 可软删，避免并发重复处理）
                        LambdaUpdateWrapper<PortalTopicPost> uw = new LambdaUpdateWrapper<>();
                        uw.eq(PortalTopicPost::getId, post.getId())
                                .eq(PortalTopicPost::getIsDeleted, 0)
                                .set(PortalTopicPost::getIsDeleted, 1)
                                .set(PortalTopicPost::getUpdatedTime, LocalDateTime.now());
                        int rows = topicPostMapper.update(null, uw);
                        if (rows > 0) {
                            // 同步减少话题观点数
                            if (post.getTopicId() != null) {
                                try {
                                    topicMapper.decrementPostCount(post.getTopicId());
                                } catch (Exception e) {
                                    log.warn("[敏感词扫描] 观点软删后同步话题计数失败：postId={}, err={}",
                                            post.getId(), e.getMessage());
                                }
                            }
                            log.warn("[敏感词扫描] 观点命中已软删：postId={}, hits={}", post.getId(), hits);
                            notifyCreator(post.getUserId(), "topic_post", "观点", post.getId(),
                                    "观点 #" + post.getId(), "因命中敏感词已被下架，如有异议请联系客服");
                        }
                    }
                } catch (Exception e) {
                    log.error("[敏感词扫描] 观点扫描异常：postId={}, err={}", post.getId(), e.getMessage());
                }
            }
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
        log.info("[敏感词扫描] 观点扫描结束，扫描 {} 条，命中 {} 条", total, hitCount);
    }

    /**
     * 构建话题扫描文本（标题+描述）
     */
    private String buildTopicScanText(PortalTopic topic) {
        StringBuilder sb = new StringBuilder();
        if (topic.getTitle() != null) sb.append(topic.getTitle());
        if (topic.getDescription() != null) sb.append(" ").append(topic.getDescription());
        return sb.toString();
    }

    /**
     * 通知内容创建者（非阻塞，失败不影响扫描主流程）
     *
     * @param creatorId 创建者ID
     * @param bizType   业务类型英文枚举（topic / topic_post），用于 data.bizType 前端路由
     * @param bizLabel  业务类型中文文案（话题 / 观点），用于通知标题与正文
     * @param bizId     业务ID
     * @param title     业务标题
     * @param action    通知动作描述
     */
    private void notifyCreator(Long creatorId, String bizType, String bizLabel, Long bizId, String title, String action) {
        if (creatorId == null) {
            return;
        }
        try {
            SysNotification notification = new SysNotification();
            notification.setType("system");
            notification.setScope("user");
            notification.setUserId(creatorId);
            notification.setUserType("portal");
            notification.setNoticeType("1");
            notification.setStatus("0");
            notification.setTitle("您的" + bizLabel + "已被系统扫描标记：" + title);
            notification.setContent("您发布的" + bizLabel + "（" + title + "）" + action + "。");
            notification.setData("{\"bizType\":\"" + bizType + "\",\"id\":" + bizId + "}");
            notificationService.insertNotification(notification);
        } catch (Exception e) {
            log.warn("[敏感词扫描] 通知发送失败：creatorId={}, bizId={}, err={}", creatorId, bizId, e.getMessage());
        }
    }
}
