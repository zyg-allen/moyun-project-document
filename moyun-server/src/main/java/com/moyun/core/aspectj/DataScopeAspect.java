package com.moyun.core.aspectj;

import com.moyun.common.annotation.DataScope;
import com.moyun.common.constant.UserConstants;
import com.moyun.core.base.BaseEntity;
import com.moyun.core.base.entity.SysRole;
import com.moyun.core.base.entity.SysUser;
import com.moyun.core.base.model.LoginUser;
import com.moyun.core.base.text.Convert;
import com.moyun.core.security.context.PermissionContextHolder;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据过滤处理
 *
 * @author allen-zyg
 */
@Aspect
@Component
public class DataScopeAspect {
    /**
     * 全部数据权限
     */
    public static final String DATA_SCOPE_ALL = "1";

    /**
     * 自定数据权限
     */
    public static final String DATA_SCOPE_CUSTOM = "2";

    /**
     * 部门数据权限
     */
    public static final String DATA_SCOPE_DEPT = "3";

    /**
     * 部门及以下数据权限
     */
    public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";

    /**
     * 仅本人数据权限
     */
    public static final String DATA_SCOPE_SELF = "5";

    /**
     * 数据权限过滤关键字（**不受信**：XML 里 {@code ${params.dataScope}} 读的就是这个键）
     */
    public static final String DATA_SCOPE = "dataScope";

    /**
     * 数据权限过滤关键字（**受信**）：切面产出的片段只写这个键。
     *
     * <p>信任是显式契约：{@link com.moyun.core.mybatis.SqlTemplateGuardInterceptor}
     * 在语句执行前用本键覆盖 {@code params[dataScope]}；没有本键时一律把
     * {@code params[dataScope]} 清空——客户端通过 {@code ?params[dataScope]=...}
     * 绑进来的值结构上无法进入 SQL。</p>
     */
    public static final String TRUSTED_DATA_SCOPE = "trustedDataScope";

    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable {
        clearDataScope(point);
        handleDataScope(point, controllerDataScope);
    }

    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope) {
        // 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser)) {
            SysUser currentUser = loginUser.getUser();
            // 如果是超级管理员，则不过滤数据
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin()) {
                String permission = StringUtils.defaultIfEmpty(controllerDataScope.permission(), PermissionContextHolder.getContext());
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.deptAlias(), controllerDataScope.userAlias(), permission);
            }
        }
    }

    /**
     * 数据范围过滤
     *
     * @param joinPoint  切点
     * @param user       用户
     * @param deptAlias  部门别名
     * @param userAlias  用户别名
     * @param permission 权限字符
     */
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String deptAlias, String userAlias, String permission) {
        StringBuilder sqlString = new StringBuilder();
        List<String> conditions = new ArrayList<String>();
        List<String> scopeCustomIds = new ArrayList<String>();
        user.getRoles().forEach(role -> {
            if (DATA_SCOPE_CUSTOM.equals(role.getDataScope()) && StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission))) {
                scopeCustomIds.add(Convert.toStr(role.getRoleId()));
            }
        });

        for (SysRole role : user.getRoles()) {
            String dataScope = role.getDataScope();
            if (conditions.contains(dataScope) || StringUtils.equals(role.getStatus(), UserConstants.ROLE_DISABLE)) {
                continue;
            }
            if (!StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission))) {
                continue;
            }
            if (DATA_SCOPE_ALL.equals(dataScope)) {
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            } else if (DATA_SCOPE_CUSTOM.equals(dataScope)) {
                if (scopeCustomIds.size() > 1) {
                    // 多个自定数据权限使用in查询，避免多次拼接。
                    sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id in ({}) ) ", deptAlias, String.join(",", scopeCustomIds)));
                } else {
                    sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", deptAlias, role.getRoleId()));
                }
            } else if (DATA_SCOPE_DEPT.equals(dataScope)) {
                sqlString.append(StringUtils.format(" OR {}.dept_id = {} ", deptAlias, user.getDeptId()));
            } else if (DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope)) {
                sqlString.append(StringUtils.format(" OR {}.dept_id IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set( {} , ancestors ) )", deptAlias, user.getDeptId(), user.getDeptId()));
            } else if (DATA_SCOPE_SELF.equals(dataScope)) {
                if (StringUtils.isNotBlank(userAlias)) {
                    sqlString.append(StringUtils.format(" OR {}.user_id = {} ", userAlias, user.getUserId()));
                } else {
                    // 数据权限为仅本人且没有userAlias别名不查询任何数据
                    sqlString.append(StringUtils.format(" OR {}.dept_id = 0 ", deptAlias));
                }
            }
            conditions.add(dataScope);
        }

        // 角色都不包含传递过来的权限字符，这个时候sqlString也会为空，所以要限制一下,不查询任何数据
        if (StringUtils.isEmpty(conditions)) {
            sqlString.append(StringUtils.format(" OR {}.dept_id = 0 ", deptAlias));
        }

        if (StringUtils.isNotBlank(sqlString.toString())) {
            BaseEntity baseEntity = resolveDataScopeParam(joinPoint);
            if (baseEntity != null) {
                // 只写"受信键"：真正落到 ${params.dataScope} 的值由 SqlTemplateGuardInterceptor 从本键复制
                baseEntity.getParams().put(TRUSTED_DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    /**
     * 定位承载 dataScope 的实体参数：返回方法参数中第一个 {@link BaseEntity}。
     *
     * <p>不能固定取 {@code joinPoint.getArgs()[0]}：对
     * {@code selectUserPage(IPage, SysUser)} 这类「分页对象在前、查询实体在后」的方法会取错对象，
     * 导致 {@code clearDataScope} 未清空用户传入的 {@code params[dataScope]}、
     * {@code dataScopeFilter} 也未写入权限片段 —— 非超管可借
     * {@code ?params[dataScope]=...} 注入任意 SQL。</p>
     *
     * <p>遍历参数定位实体，使 {@code @DataScope} 可安全用于任意参数位置。</p>
     *
     * @return 承载参数；方法参数中无 BaseEntity 时返回 null
     */
    private static BaseEntity resolveDataScopeParam(final JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof BaseEntity baseEntity) {
                return baseEntity;
            }
        }
        return null;
    }

    /**
     * 拼接权限sql前先清空params.dataScope参数防止注入
     */
    private void clearDataScope(final JoinPoint joinPoint) {
        BaseEntity baseEntity = resolveDataScopeParam(joinPoint);
        if (baseEntity != null) {
            baseEntity.getParams().put(DATA_SCOPE, "");
        }
    }
}
