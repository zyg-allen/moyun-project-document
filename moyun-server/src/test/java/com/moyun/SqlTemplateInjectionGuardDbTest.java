package com.moyun;

import com.moyun.common.constant.UserConstants;
import com.moyun.core.aspectj.DataScopeAspect;
import com.moyun.core.base.entity.SysRole;
import com.moyun.core.base.entity.SysUser;
import com.moyun.core.base.model.LoginUser;
import com.moyun.core.security.context.PermissionContextHolder;
import com.moyun.portal.domain.query.ArticleQuery;
import com.moyun.portal.mapper.PortalArticleMapper;
import com.moyun.system.mapper.SysUserMapper;
import com.moyun.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真库 · SQL 模板变量（{@code ${}}）注入防线 —— {@code params.dataScope} 的信任边界
 *
 * <h3>被验证的缺陷（P1 · 遗留项，附录 B3 记为"参数化重构待排期"）</h3>
 * <p>P0-1（附录 A-2.1）修的是"切面取错参数 + 补 {@code @DataScope}"，使
 * {@code ${params.dataScope}} 的内容变为**受控片段**。但该防线**依赖切面被调用**：
 * {@code params} 是 {@code BaseEntity} 上的 {@code Map}，Spring 可以把请求里的
 * {@code ?params[dataScope]=...} 直接绑定进去；只要某条 SQL 走了
 * {@code ${params.dataScope}} 却**没有经过 {@code @DataScope} 切面**（直连 mapper、
 * 新增方法漏注解、异步/内部调用），客户端字符串就会原样进入 SQL。</p>
 *
 * <p>本测试用**语法上必然报错**的载荷（不存在的列名）来给出确定性证据：
 * 若载荷进入 SQL，MySQL 会抛 {@code Unknown column}；被丢弃则查询正常。</p>
 *
 * <h3>另一半：为什么不需要"参数化 orderBy"</h3>
 * <p>7 个 portal mapper 用 {@code ${params.orderByColumn}}，但它们的入参类型都继承
 * {@code PageDomain}（{@code @Param("params")} 绑定的是**查询对象**而不是 params map），
 * 而 {@code PageDomain#setOrderByColumn} 自带正则白名单、{@code setIsAsc} 只允许 asc/desc
 * ——注入在**绑定入口**就被拒绝（见 {@link #pageDomainOrderByRejectsInjection}）。</p>
 *
 * @author moyun
 */
@SpringBootTest
class SqlTemplateInjectionGuardDbTest {

    /** 用户 2（ry）不是超管（isAdmin 只认 userId=1），用于非超管数据范围验证 */
    private static final long NON_ADMIN_USER_ID = 2L;
    private static final long DEPT_ID = 1L;
    /** 不存在的部门：部门范围过滤应匹配 0 行 */
    private static final long NON_EXISTENT_DEPT_ID = 999999L;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private PortalArticleMapper portalArticleMapper;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("客户端可绑定的 params[dataScope] 不得进入 SQL（无切面路径也必须被拦住）")
    void clientSuppliedDataScopeCannotReachSql() {
        SysUser query = new SysUser();
        // 模拟 Spring 绑定 ?params[dataScope]=<载荷>：内容与合法片段同形，只是多了一个不存在的列
        query.getParams().put(DataScopeAspect.DATA_SCOPE, " AND (no_such_column_zz = 1)");

        // 直连 mapper：这条路径上没有 @DataScope 切面，历史实现会把载荷拼进 SQL
        List<SysUser> rows = sysUserMapper.selectUserList(query);

        assertTrue(rows.size() >= 2,
                "载荷被丢弃后应正常返回全部用户，实际 = " + rows.size());
    }

    @Test
    @DisplayName("信任通道仍然生效：部门 / 仅本人 / 全部 三种数据范围")
    void trustedDataScopeStillApplies() {
        prepareRequestContext();

        // ① 部门范围（部门不存在）→ 0 行
        authenticate(roleWithScope(DataScopeAspect.DATA_SCOPE_DEPT), NON_ADMIN_USER_ID, NON_EXISTENT_DEPT_ID);
        assertEquals(0, sysUserService.selectUserList(new SysUser()).size(),
                "部门范围过滤未生效：dept=" + NON_EXISTENT_DEPT_ID + " 不该匹配到用户");

        // ② 仅本人 → 恰好自己那一行
        authenticate(roleWithScope(DataScopeAspect.DATA_SCOPE_SELF), NON_ADMIN_USER_ID, DEPT_ID);
        assertEquals(1, sysUserService.selectUserList(new SysUser()).size(),
                "仅本人范围应只返回自己（userId=" + NON_ADMIN_USER_ID + "）");

        // ③ 全部数据 → 不过滤
        authenticate(roleWithScope(DataScopeAspect.DATA_SCOPE_ALL), NON_ADMIN_USER_ID, DEPT_ID);
        assertTrue(sysUserService.selectUserList(new SysUser()).size() >= 2,
                "全部数据范围不应过滤任何行");

        // ④ 角色不具备该权限字符 → 一律查不到（RuoYi 语义：没权限即无数据）
        authenticate(roleWithPermission("other:perm"), NON_ADMIN_USER_ID, DEPT_ID);
        assertEquals(0, sysUserService.selectUserList(new SysUser()).size(),
                "角色不含目标权限字符时应返回 0 行");
    }

    @Test
    @DisplayName("orderBy 无需参数化：PageDomain 在绑定入口就拒绝注入载荷")
    void pageDomainOrderByRejectsInjection() {
        ArticleQuery query = new ArticleQuery();

        // 合法列名：走通（并顺带证明 portal 动态排序确实可用）
        query.setOrderByColumn("id");
        assertTrue(portalArticleMapper.selectPortalArticleList(query) != null,
                "合法 orderBy 应正常执行");

        // 注入载荷：绑定入口（setter）直接拒绝
        assertThrows(IllegalArgumentException.class,
                () -> new ArticleQuery().setOrderByColumn("id, (select 1)"),
                "含逗号/括号/空格的排序列名必须在 setter 被拒绝");
        assertThrows(IllegalArgumentException.class,
                () -> new ArticleQuery().setIsAsc("asc; drop table t"),
                "非 asc/desc 的排序方向必须在 setter 被拒绝");
    }

    // ==================== 内部 ====================

    /** {@code PermissionContextHolder} 依赖请求上下文，测试里手工挂一个 */
    private void prepareRequestContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        PermissionContextHolder.setContext("system:user:list");
    }

    private void authenticate(SysRole role, long userId, long deptId) {
        SysUser current = new SysUser();
        current.setUserId(userId);
        current.setDeptId(deptId);
        current.setUserName("test-user");
        current.setRoles(List.of(role));
        LoginUser loginUser = new LoginUser(userId, deptId, current, Set.of("system:user:list"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private SysRole roleWithScope(String dataScope) {
        return roleWithPermission("system:user:list", dataScope);
    }

    private SysRole roleWithPermission(String permissions) {
        return roleWithPermission(permissions, DataScopeAspect.DATA_SCOPE_ALL);
    }

    private SysRole roleWithPermission(String permissions, String dataScope) {
        SysRole role = new SysRole();
        role.setRoleId(2L);
        role.setRoleKey("test-role");
        role.setDataScope(dataScope);
        role.setStatus(UserConstants.ROLE_NORMAL);
        role.setPermissions(Set.of(permissions));
        return role;
    }
}
