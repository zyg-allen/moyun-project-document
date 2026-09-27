package com.moyun.core.mybatis;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.aspectj.DataScopeAspect;
import com.moyun.core.base.BaseEntity;
import com.moyun.core.base.page.TableSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * SQL 模板变量（{@code ${}}）最后一道闸：{@code params[dataScope]} 必须来自数据权限切面
 *
 * <h3>为什么需要它（v13.8）</h3>
 * <p>MyBatis 的 {@code ${}} 是**文本替换**，没有参数绑定保护，安全与否完全取决于"内容是谁写的"。
 * 项目里两类 {@code ${}} 的信任来源不同：</p>
 *
 * <table border="1">
 *   <tr><th>模板变量</th><th>信任来源</th><th>结论</th></tr>
 *   <tr>
 *     <td>{@code ${params.dataScope}}</td>
 *     <td>{@code DataScopeAspect} 依据**当前登录人角色**生成的片段（内容只含白名单数字 id）</td>
 *     <td>内容可信，但 {@code params} 是 {@code BaseEntity} 上的 {@code Map}，Spring 可把
 *         {@code ?params[dataScope]=...} 直接绑进去 → **只要哪条 SQL 走了该变量却没经过切面**
 *         （直连 mapper、新增方法漏注解、内部/异步调用），客户端字符串就原样进 SQL
 *         （已用真库实测复现：{@code AND (no_such_column_zz = 1)} 原样出现在 where 子句）</td>
 *   </tr>
 *   <tr>
 *     <td>{@code ${params.orderByColumn}} / {@code ${params.isAsc}}</td>
 *     <td>portal 系列 mapper 的入参都继承 {@code PageDomain}（{@code @Param("params")}
 *         绑定的是查询对象，不是 params map），其 setter 自带白名单校验</td>
 *     <td>绑定入口即被拒绝；但若将来出现"params map 形态"的排序就没有这层校验 —— 本类一并兜住</td>
 *   </tr>
 * </table>
 *
 * <h3>做法：把"信任"变成显式契约</h3>
 * <ul>
 *   <li>{@code DataScopeAspect} 只写 {@code params[}{@link DataScopeAspect#TRUSTED_DATA_SCOPE}{@code ]}
 *       （受信键），**不再直接写** {@code params[dataScope]}；</li>
 *   <li>本插件在语句执行前：有受信键 → 用它覆盖 {@code params[dataScope]}；没有 → 把
 *       {@code params[dataScope]} 清空（客户端塞进来的值一律丢弃）并告警；</li>
 *   <li>顺带对 map 形态的 {@code orderByColumn}（标识符白名单）与 {@code isAsc}（asc/desc）
 *       做 **fail-closed** 校验，非法直接抛错。</li>
 * </ul>
 * <p>于是"能不能拼进 SQL"不再取决于调用链是否记得加注解，而是**结构上只有切面能写**。</p>
 *
 * <h3>为什么必须是外层 MyBatis 插件，而不是 {@code InnerInterceptor}</h3>
 * <p>实测教训：{@code ${}} 的文本替换发生在 {@code MappedStatement#getBoundSql(parameter)}
 * 里，而 {@code InnerInterceptor#beforeQuery} 拿到的 {@code BoundSql} <b>已经渲染完毕</b>——
 * 在那里改参数对本次执行无效（首版就是这样：告警打了、"丢弃"也记了，SQL 里载荷照旧）。
 * 本类作为**最外层插件**挂在 {@code Executor} 上，在 MyBatis 内部构建 {@code BoundSql} 之前
 * 完成清洗，因此对 4 参与 6 参两条查询路径都生效。</p>
 *
 * @author moyun
 * @see DataScopeAspect
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
                        CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SqlTemplateGuardInterceptor implements Interceptor {

    /** 排序列名白名单（与 {@code PageDomain#setOrderByColumn} 同一口径） */
    private static final Pattern ORDER_BY_COLUMN_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");

    private static final String ORDER_BY_COLUMN = TableSupport.ORDER_BY_COLUMN;
    private static final String IS_ASC = TableSupport.IS_ASC;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        Object parameter = args.length > 1 ? args[1] : null;

        int changed = harden(parameter);

        // 6 参签名：BoundSql 已由调用方构建 → 用清洗后的参数重新生成，保证本次执行立即生效
        if (changed > 0 && args.length == 6 && args[0] instanceof MappedStatement ms
                && args[5] instanceof BoundSql) {
            args[5] = ms.getBoundSql(parameter);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 无需配置项
    }

    /**
     * 加固参数中所有 {@code params} 容器（{@link BaseEntity#getParams()} 或 MyBatis ParamMap）。
     *
     * @param parameter 语句参数（可为 null / 任意类型）
     * @return 发生修改的处数（含丢弃的可疑 {@code dataScope}）
     */
    public static int harden(Object parameter) {
        return harden(parameter, 0);
    }

    private static int harden(Object parameter, int depth) {
        if (parameter == null || depth > 4) {
            return 0;
        }
        int changed = 0;
        if (parameter instanceof BaseEntity entity) {
            return sanitize(entity.getParams());
        }
        if (parameter instanceof Map<?, ?> map) {
            // ① 该 Map 自身可能是 params 容器（@Param("params") 传 Map、或 ParamMap 里直接放 dataScope）
            changed += sanitize(map);
            // ② 递归其值：覆盖 @Param("query") / @Param("params") 这类包装
            for (Object value : map.values()) {
                changed += harden(value, depth + 1);
            }
        }
        return changed;
    }

    /**
     * 清洗单个 params 容器：受信键存在 → 覆盖 {@code dataScope}；不存在 → 清空 {@code dataScope}。
     * {@code orderByColumn}/{@code isAsc} 非法即 fail-closed。
     */
    @SuppressWarnings("unchecked")
    static int sanitize(Map<?, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            return 0;
        }
        Map<String, Object> params = (Map<String, Object>) raw;
        int changed = 0;

        // ⚠️ 必须用 containsKey 守卫后再 get：MyBatis 的 MapperMethod.ParamMap 覆写了 get()，
        // 取不存在的键会直接抛 BindingException（"Parameter 'x' not found"）——
        // 首版没守卫，导致所有 Wrapper 形态查询在启动期就炸（真库实测）。
        Object trusted = valueOrNull(params, DataScopeAspect.TRUSTED_DATA_SCOPE);
        if (trusted instanceof String fragment && !fragment.isEmpty()) {
            params.put(DataScopeAspect.DATA_SCOPE, fragment);
            changed++;
        } else if (params.containsKey(DataScopeAspect.DATA_SCOPE)) {
            Object clientValue = params.get(DataScopeAspect.DATA_SCOPE);
            if (clientValue instanceof String value && !value.isEmpty()) {
                log.warn("[sql-guard] 丢弃未经数据权限切面产出的 params[dataScope]（{} 字符）：疑似注入尝试。"
                                + "合法片段只能由 DataScopeAspect 写入 params[{}]",
                        value.length(), DataScopeAspect.TRUSTED_DATA_SCOPE);
            }
            params.put(DataScopeAspect.DATA_SCOPE, "");
            changed++;
        }

        Object orderByColumn = valueOrNull(params, ORDER_BY_COLUMN);
        if (orderByColumn instanceof String column && !column.isEmpty()
                && !ORDER_BY_COLUMN_PATTERN.matcher(column).matches()) {
            throw new ServiceException("排序列名不合法：" + column);
        }
        Object isAsc = valueOrNull(params, IS_ASC);
        if (isAsc instanceof String direction && !direction.isEmpty()
                && !"asc".equals(direction.toLowerCase(Locale.ROOT))
                && !"desc".equals(direction.toLowerCase(Locale.ROOT))) {
            throw new ServiceException("排序方向不合法：" + direction);
        }
        return changed;
    }

    /** {@code containsKey} 守卫的取值（兼容覆写 get() 的 Map 实现） */
    private static Object valueOrNull(Map<String, Object> params, String key) {
        return params.containsKey(key) ? params.get(key) : null;
    }
}
