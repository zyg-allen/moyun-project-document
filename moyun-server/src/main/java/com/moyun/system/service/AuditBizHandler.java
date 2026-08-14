package com.moyun.system.service;

import java.util.Map;

/**
 * 审核业务处理器（策略模式，v8.1）
 * <p>
 * 每个审核业务（文章/专栏/话题/面经/面经评论/认证/反馈/举报）实现本接口，
 * 由 {@code IAuditTaskService.handle()} 根据任务类型分发调用。
 * <p>
 * 实现类需在 Spring 容器中注册为 Bean，并通过 Bean 名匹配 {@code AuditTaskType.code}：
 * <ul>
 *   <li>实现类 Bean 名约定为 {@code auditBizHandler_<taskType>}（如 auditBizHandler_article）；</li>
 *   <li>或在实现类构造时显式声明支持的 taskType。</li>
 * </ul>
 *
 * @author moyun
 */
public interface AuditBizHandler {

    /**
     * 支持的任务类型（对应 {@code AuditTaskType.code}）。
     */
    String supportedTaskType();

    /**
     * 同意审核：业务表状态置为通过态（如 article→published / certification→approved）。
     * <p>
     * 实现要点：
     * <ul>
     *   <li>更新业务表 status、auditor_id、audit_remark/audit_opinion、audit_time；</li>
     *   <li>触发业务后置动作（如文章发布 Feed、积分发放、下架被举报内容等）；</li>
     *   <li>幂等：若业务表已为终态，直接返回成功。</li>
     * </ul>
     *
     * @param bizId     业务记录ID
     * @param auditorId 处理人系统用户ID
     * @param auditorName 处理人用户名
     * @param opinion   审核意见（同意时可能为空）
     */
    void approve(Long bizId, Long auditorId, String auditorName, String opinion);

    /**
     * 驳回审核：业务表状态置为驳回态（rejected）。
     * <p>
     * 实现要点：
     * <ul>
     *   <li>更新业务表 status=rejected、auditor_id、audit_remark/audit_opinion、audit_time；</li>
     *   <li>驳回时 opinion 一定非空（由上层保证校验）；</li>
     *   <li>触发业务后置动作（如回滚成长值）。</li>
     * </ul>
     *
     * @param bizId       业务记录ID
     * @param auditorId   处理人系统用户ID
     * @param auditorName 处理人用户名
     * @param opinion     驳回原因（必填）
     */
    void reject(Long bizId, Long auditorId, String auditorName, String opinion);

    /**
     * 获取业务详情（供审核中心详情弹窗展示）。
     * <p>
     * 返回的 Map 结构因业务而异，前端按 task_type 渲染对应详情卡片。
     *
     * @param bizId 业务记录ID
     * @return 业务详情字段 Map（如文章返回 title/content/author；举报返回 description/images/targetUrl）
     */
    Map<String, Object> getBizDetail(Long bizId);
}
