package com.moyun.ext.cms.service;

/**
 * 举报联动下架服务
 * <p>当举报被处理为「已解决（resolved）」时，根据 targetType + targetId
 * 联动下架被举报的内容，避免违规内容继续曝光。</p>
 *
 * <p>支持的目标类型：
 * <ul>
 *   <li>article：文章 → status='rejected'（沿用审核驳回状态，触发审核字段记录）</li>
 *   <li>comment：通用评论 → 物理软删（del_flag，沿用 BaseMapper 逻辑删除）</li>
 *   <li>topic：话题 → status='archived'（归档下架，保留数据可申诉恢复）</li>
 *   <li>topic_post：话题观点 → is_deleted=1（软删）</li>
 *   <li>topic_comment：话题评论 → is_deleted=1（软删）</li>
 *   <li>column：专栏 → status='archived'（归档下架）</li>
 * </ul>
 *
 * <p>user 类型不在本服务范围（涉及账户封禁流程，由用户管理模块独立处理）。</p>
 *
 * @author moyun
 */
public interface IReportTakedownService {

    /**
     * 根据 targetType + targetId 联动下架被举报内容
     *
     * @param targetType 目标类型：article/comment/topic/topic_post/topic_comment/column
     * @param targetId   目标ID
     * @param handler    处理人（系统用户名，用于审计）
     * @return true 表示下架成功；false 表示目标不存在或不支持；null 表示无需下架（如 user 类型）
     */
    boolean takedown(String targetType, Long targetId, String handler);
}
