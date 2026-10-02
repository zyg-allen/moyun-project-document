package com.moyun.core.portal;

import java.util.Map;

/**
 * 审核业务内容端口（防腐层）
 *
 * <h3>为什么需要它</h3>
 * <p>管理端（{@code system}）的审核中心需要读取/落地<b>用户端（portal）</b>的各类业务内容
 * （文章、专栏、认证、反馈、面试评论/经验、举报、话题）。历史实现是 8 个
 * {@code *AuditBizHandler} <b>各自直接 import 门户实体与 Mapper</b>：
 * {@code system -> portal} 一共 24 条依赖边，管理端直接扎进用户端数据层——门户换个字段/表结构，
 * 管理端编译期就崩，且没有任何一层conversion 约束。</p>
 *
 * <h3>契约（只暴露中性类型）</h3>
 * <ul>
 *   <li>入参与返回**只用 JDK 类型**（{@code String}/{@code Long}/{@code Map<String,Object>}），
 *       门户实体与 Mapper 一律不出现在接口签名里；</li>
 *   <li>实现由业务模块（门户侧）提供，见 {@code com.moyun.portal.audit.AuditContentAdapter}；</li>
 *   <li>{@code taskType} 取值即审核任务类型（{@code article}/{@code column}/{@code certification}/
 *       {@code feedback}/{@code interview_comment}/{@code interview_exp}/{@code report}/{@code topic}）。</li>
 * </ul>
 *
 * <p>这样依赖方向变成 {@code system -> core <- portal}（依赖倒置），与
 * {@code core.security.principal} 同一手法。</p>
 *
 * @author moyun
 */
public interface AuditContentPort {

    /**
     * 读取审核详情（供审核中心展示）
     *
     * @param taskType 审核任务类型
     * @param bizId    业务记录ID
     * @return 中性字段 Map；记录不存在返回 {@code null}
     */
    Map<String, Object> loadDetail(String taskType, Long bizId);

    /**
     * 落地审核结论
     *
     * @param taskType    审核任务类型
     * @param bizId       业务记录ID
     * @param approved    true=通过，false=驳回
     * @param auditorId   审核人（系统用户）ID
     * @param auditorName 审核人名称（部分业务落库为 handler 字段）
     * @param opinion     审核意见（可空）
     */
    void applyAudit(String taskType, Long bizId, boolean approved,
                    Long auditorId, String auditorName, String opinion);
}
