package com.moyun.ledger.service;

import com.moyun.ledger.domain.entity.PortalDataAuth;

import java.util.Collection;
import java.util.Map;

/**
 * 用户数据授权服务（记账域 C 档数据的准入校验 + 访问审计）
 *
 * <p>职责边界：
 * <ul>
 *   <li>只读授权（授权由 App 用户主动写入，后台不得代写/延期）；</li>
 *   <li>提供"是否授权某范围"的判定与批量判定；</li>
 *   <li>每次个体数据读取都写一条访问日志（允许/拒绝都写）。</li>
 * </ul>
 *
 * @author moyun
 */
public interface ILedgerDataAuthService {

    /**
     * 用户当前有效的记账授权（无则 null）
     */
    PortalDataAuth findActive(Long userId);

    /**
     * 批量取有效授权（按 userId 映射，取最近一次授权）
     */
    Map<Long, PortalDataAuth> findActiveBatch(Collection<Long> userIds);

    /**
     * 是否已授权某范围（未授权/已过期/已撤回 均返回 false）
     */
    boolean isAuthorized(Long userId, String scope);

    /**
     * 记录一次敏感数据访问
     *
     * @param targetUserId 被查看用户
     * @param scope        授权范围
     * @param action       query/export
     * @param queryDesc    查询条件与条数（不含字段值）
     * @param result       allowed/denied
     * @param authId       关联授权 id（拒绝时传 null）
     */
    void recordAccess(Long targetUserId, String scope, String action,
                      String queryDesc, String result, Long authId);

    /**
     * 授权状态：valid / expired / revoked
     */
    String statusOf(PortalDataAuth auth);

    /**
     * 用户主动授权（App 调用）
     *
     * <p>语义：同域同用户只保留一条有效授权；再次授权会先撤回旧记录再新增，
     * 保证"有效期/范围"以最新一次为准，且历史记录可追溯。</p>
     *
     * @param userId     授权人
     * @param scopes     授权范围（transaction/account/budget/inventory/ai/profile）
     * @param purpose    用途（用户填写，必填）
     * @param hours      有效小时数（1~168，默认 24）
     * @param docVersion 隐私政策版本
     * @return 新建的授权记录
     */
    PortalDataAuth grant(Long userId, java.util.List<String> scopes, String purpose,
                         Integer hours, String docVersion);

    /**
     * 用户撤回授权（立即生效）
     *
     * @param userId 授权人
     * @param reason 撤回原因（可空）
     */
    void revoke(Long userId, String reason);
}
