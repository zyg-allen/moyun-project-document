package com.moyun;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.aspectj.DataScopeAspect;
import com.moyun.core.base.entity.SysUser;
import com.moyun.core.mybatis.SqlTemplateGuardInterceptor;
import org.apache.ibatis.binding.MapperMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 单元测试：{@code ${}} 模板变量清洗器（{@link SqlTemplateGuardInterceptor#harden}）
 *
 * <p>覆盖三种真实参数形态与两类拒绝规则：</p>
 * <ol>
 *   <li>{@code BaseEntity} 直传（{@code selectUserList(SysUser)} 这类无 {@code @Param} 的写法）；</li>
 *   <li>MyBatis {@code ParamMap} 包装（{@code @Param("query")} / {@code @Param("params")}）——
 *       <b>ParamMap 覆写了 {@code get()}，取不存在的键会抛 {@code BindingException}</b>，
 *       清洗器必须用 {@code containsKey} 守卫（首版没守，所有 Wrapper 查询在启动期就炸）；</li>
 *   <li>嵌套形态（ParamMap 里再包一层实体）。</li>
 * </ol>
 *
 * @author moyun
 * @see SqlTemplateInjectionGuardDbTest 真库端到端验证
 */
class SqlTemplateGuardInterceptorTest {

    @Test
    @DisplayName("ParamMap：取不存在的键不得抛 BindingException（启动期炸库的真实回归）")
    void paramMapMissingKeysMustNotThrow() {
        MapperMethod.ParamMap<Object> paramMap = new MapperMethod.ParamMap<>();
        paramMap.put("ew", new Object());
        paramMap.put("param1", new SysUser());

        assertDoesNotThrow(() -> SqlTemplateGuardInterceptor.harden(paramMap),
                "ParamMap 覆写了 get()：清洗器必须 containsKey 守卫后再取值");
    }

    @Test
    @DisplayName("客户端 dataScope 必须被丢弃并清空（无切面时也不得进 SQL）")
    void clientDataScopeIsDropped() {
        SysUser user = new SysUser();
        user.getParams().put(DataScopeAspect.DATA_SCOPE, " AND (1=1 OR (select 1))");

        int changed = SqlTemplateGuardInterceptor.harden(user);

        assertEquals(1, changed, "应记录 1 处丢弃");
        assertEquals("", user.getParams().get(DataScopeAspect.DATA_SCOPE),
                "客户端传入的 dataScope 必须被清空");
    }

    @Test
    @DisplayName("受信 dataScope：切面写入的片段被复制到 ${} 读取的键上")
    void trustedDataScopeIsPropagated() {
        SysUser user = new SysUser();
        user.getParams().put(DataScopeAspect.DATA_SCOPE, " AND (attacker = 1)");
        user.getParams().put(DataScopeAspect.TRUSTED_DATA_SCOPE, " AND (d.dept_id = 1)");

        int changed = SqlTemplateGuardInterceptor.harden(user);

        assertEquals(1, changed);
        assertEquals(" AND (d.dept_id = 1)", user.getParams().get(DataScopeAspect.DATA_SCOPE),
                "受信片段必须覆盖客户端值（即使两者同时存在）");
    }

    @Test
    @DisplayName("嵌套形态：ParamMap 里包装的实体同样被清洗")
    void nestedParamMapIsSanitized() {
        SysUser query = new SysUser();
        query.getParams().put(DataScopeAspect.DATA_SCOPE, " AND (injected = 1)");
        MapperMethod.ParamMap<Object> paramMap = new MapperMethod.ParamMap<>();
        paramMap.put("query", query);

        SqlTemplateGuardInterceptor.harden(paramMap);

        assertEquals("", query.getParams().get(DataScopeAspect.DATA_SCOPE),
                "包装在 ParamMap 里的实体也必须被清洗");
    }

    @Test
    @DisplayName("map 形态排序：列名白名单 + 方向枚举，非法即 fail-closed")
    void mapFormOrderByIsValidated() {
        // 合法：不做任何修改
        Map<String, Object> ok = new HashMap<>();
        ok.put("orderByColumn", "createTime");
        ok.put("isAsc", "desc");
        assertEquals(0, SqlTemplateGuardInterceptor.harden(ok));

        // 非法列名：括号/逗号/空格
        Map<String, Object> badColumn = new HashMap<>();
        badColumn.put("orderByColumn", "id,(select 1)");
        assertThrows(ServiceException.class, () -> SqlTemplateGuardInterceptor.harden(badColumn));

        // 非法方向
        Map<String, Object> badDirection = new HashMap<>();
        badDirection.put("isAsc", "asc; drop table t");
        assertThrows(ServiceException.class, () -> SqlTemplateGuardInterceptor.harden(badDirection));

        // 合法但为 camelCase 的列名允许（与 PageDomain 同口径）
        Map<String, Object> camel = new HashMap<>();
        camel.put("orderByColumn", "createTime");
        assertTrue(SqlTemplateGuardInterceptor.harden(camel) == 0);
    }
}
