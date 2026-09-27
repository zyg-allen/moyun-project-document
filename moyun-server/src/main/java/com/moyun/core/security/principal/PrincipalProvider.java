package com.moyun.core.security.principal;

/**
 * 主体信息提供者：由 {@code core} 定义、业务模块的登录主体实现（依赖倒置，v13.11）
 *
 * <p>实现方：{@code com.moyun.core.base.model.LoginUser}（后台）、
 * {@code com.moyun.portal.domain.model.PortalLoginUser}（门户）。</p>
 *
 * <p>约定：实现必须**无副作用、不抛异常**（横切组件在日志/限流路径上调用它）。</p>
 *
 * @author moyun
 * @since 2026-09-27
 */
public interface PrincipalProvider {

    /** 转换为模块中立的主体视图 */
    PrincipalInfo toPrincipalInfo();
}
