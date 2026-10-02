package com.moyun.core.security.principal;

/**
 * 当前登录主体的**模块中立视图**
 *
 * <p>为什么需要它：{@code core} 是基础设施模块，**不应反向依赖业务模块**。但横切组件
 * （{@code LogAspect} 操作日志、{@code RateLimiterAspect} 限流键）必须知道"当前是谁"。</p>
 *
 * <p>做法：把"主体身份"抽象成本接口放在 {@code core}，由两侧的主体类各自实现——
 * {@code LoginUser}（后台）与 {@code PortalLoginUser}（门户）→ 依赖方向反转为
 * {@code portal → core}（本就允许），{@code core → portal} 归零。</p>
 *
 * @param userId     主体 id（后台 {@code sys_user.user_id} / 门户 {@code portal_user.id}）
 * @param username   账号名（写入操作日志的 {@code oper_name}）
 * @param nickname   昵称（门户侧用于 {@code dept_name} 占位，可为 null）
 * @param portalSide 是否门户端主体（决定操作日志的落库口径）
 * @author moyun
 * @since 2026-09-27
 */
public record PrincipalInfo(Long userId, String username, String nickname, boolean portalSide) {
}
