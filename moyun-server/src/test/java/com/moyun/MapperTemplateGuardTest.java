package com.moyun;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：mapper XML 里的 {@code ${}} 模板变量只允许出现在**登记过的**位置
 *
 * <h3>为什么需要</h3>
 * <p>MyBatis 的 {@code ${}} 是文本替换、没有绑定保护，是否安全完全取决于"谁写的内容"。
 * 项目现状（v13.8 实测清点）：三类共 24 处，全部有明确信任来源——</p>
 *
 * <table border="1">
 *   <tr><th>类别</th><th>处数</th><th>信任来源 / 保护</th></tr>
 *   <tr>
 *     <td>{@code ${params.dataScope}}</td><td>9</td>
 *     <td>只能来自 {@code DataScopeAspect}：切面写受信键 {@code params[trustedDataScope]}，
 *         {@code SqlTemplateGuardInterceptor} 在 SQL 渲染前复制/清空 —— 客户端绑定的值进不去</td>
 *   </tr>
 *   <tr>
 *     <td>{@code ${params.orderByColumn}} / {@code ${params.isAsc}}</td><td>14</td>
 *     <td>入参继承 {@code PageDomain}（setter 白名单校验）；map 形态另有拦截器兜底</td>
 *   </tr>
 *   <tr>
 *     <td>{@code ${sql}}（代码生成器 DDL）</td><td>1</td>
 *     <td>管理端代码生成专用，语句本身由后端按表元数据拼装（DDL 无法参数化）</td>
 *   </tr>
 * </table>
 *
 * <p>本守卫把这 24 处**登记在册**：新增任何 {@code ${}} 都会让测试失败，必须先说明它的信任来源；
 * 反过来，登记表里过期的条目也会失败（防止豁免长期留在表里）。</p>
 *
 * @author moyun
 * @see SqlTemplateGuardInterceptorTest 运行时防线
 * @see SqlTemplateInjectionGuardDbTest 真库端到端
 */
class MapperTemplateGuardTest {

    private static final Path MAPPER_ROOT = Path.of("src/main/resources/mapper");

    /** 任意 {@code ${...}} 模板变量（不含 XML 注释里的示例） */
    private static final Pattern TEMPLATE_VAR = Pattern.compile("\\$\\{[^}]+}");

    /**
     * 允许使用 {@code ${}} 的文件 → 该文件里允许的变量集合（必须写明信任来源）。
     */
    private static final Map<String, String[]> ALLOWED_TEMPLATE_VARS = new LinkedHashMap<>();

    static {
        String[] dataScopeOnly = {"params.dataScope"};
        String[] orderBy = {"params.orderByColumn", "params.isAsc"};
        String[] ddl = {"sql"};

        // 数据权限片段：只信 DataScopeAspect（受信键 + 拦截器）
        // 注：本文件同时存在 params.dataScope 与 query.params.dataScope 两种写法，由 contains() 归一处理
        // 键为相对 src/main/resources/mapper 的路径
        ALLOWED_TEMPLATE_VARS.put("system/SysUserMapper.xml", dataScopeOnly);
        ALLOWED_TEMPLATE_VARS.put("system/SysRoleMapper.xml", dataScopeOnly);
        ALLOWED_TEMPLATE_VARS.put("system/SysDeptMapper.xml", dataScopeOnly);

        // 动态排序：PageDomain setter 白名单 + 拦截器兜底
        ALLOWED_TEMPLATE_VARS.put("portal/PortalAdSlotMapper.xml", orderBy);
        ALLOWED_TEMPLATE_VARS.put("portal/PortalArticleMapper.xml", orderBy);
        ALLOWED_TEMPLATE_VARS.put("portal/PortalBookmarkMapper.xml", orderBy);
        ALLOWED_TEMPLATE_VARS.put("portal/PortalCategoryMapper.xml", orderBy);
        ALLOWED_TEMPLATE_VARS.put("portal/PortalFollowMapper.xml", orderBy);
        ALLOWED_TEMPLATE_VARS.put("portal/PortalFriendLinkMapper.xml", orderBy);
        ALLOWED_TEMPLATE_VARS.put("portal/PortalTagMapper.xml", orderBy);

        // 代码生成器建表 DDL：管理端专用，DDL 无法参数化
        ALLOWED_TEMPLATE_VARS.put("generator/GenTableMapper.xml", ddl);
    }

    @Test
    @DisplayName("全量 mapper XML：${} 只允许出现在登记表内，且变量名必须在允许集合中")
    void onlyWhitelistedTemplateVars() throws IOException {
        List<String> violations = new ArrayList<>();
        forEachMapper((relativePath, source) -> {
            String[] allowed = ALLOWED_TEMPLATE_VARS.get(relativePath);
            Matcher matcher = TEMPLATE_VAR.matcher(source);
            while (matcher.find()) {
                String variable = matcher.group().substring(2, matcher.group().length() - 1).trim();
                if (allowed == null) {
                    violations.add(relativePath + ":" + lineOf(source, matcher.start())
                            + " 未登记的文件使用了 ${" + variable + "}");
                } else if (!contains(allowed, variable)) {
                    violations.add(relativePath + ":" + lineOf(source, matcher.start())
                            + " 未登记的模板变量 ${" + variable + "}");
                }
            }
        });
        assertTrue(violations.isEmpty(),
                "mapper 里新增 ${} 必须先说明信任来源（并在本测试登记）。"
                        + "提示：数据权限片段只能用 params.dataScope（由 SqlTemplateGuardInterceptor 保证只接受切面产出）；"
                        + "排序列名请走 PageDomain；其余情况优先用 #{}\n  " + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("登记表自检：不得包含已不存在的文件（防止豁免长期留在表里）")
    void allowlistIsNotStale() {
        for (String key : ALLOWED_TEMPLATE_VARS.keySet()) {
            String path = key.contains("#") ? key.substring(0, key.indexOf('#')) : key;
            assertTrue(Files.exists(MAPPER_ROOT.resolve(path)), "登记表条目已失效（文件不存在）：" + key);
        }
    }

    @Test
    @DisplayName("登记表自检：每个登记文件确实仍在使用 ${}（否则应删除登记）")
    void allowlistEntriesAreStillUsed() throws IOException {
        List<String> stale = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : ALLOWED_TEMPLATE_VARS.entrySet()) {
            String key = entry.getKey();
            if (key.contains("#")) {
                continue; // 仅作为同文件第二种写法的说明条目
            }
            String source = Files.readString(MAPPER_ROOT.resolve(key), StandardCharsets.UTF_8);
            if (!TEMPLATE_VAR.matcher(source).find()) {
                stale.add(key);
            }
        }
        assertTrue(stale.isEmpty(), "登记表条目已不再使用 ${}，请删除登记：\n  " + String.join("\n  ", stale));
    }

    // ==================== 内部 ====================

    private static boolean contains(String[] values, String target) {
        for (String value : values) {
            if (value.equals(target) || value.equals(target.replace("query.", ""))) {
                return true;
            }
        }
        return false;
    }

    private static void forEachMapper(MapperConsumer consumer) throws IOException {
        try (Stream<Path> files = Files.walk(MAPPER_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".xml")).toList()) {
                // 键统一为相对 mapper 根目录的路径，如 system/SysUserMapper.xml
                consumer.accept(MAPPER_ROOT.relativize(file).toString().replace('\\', '/'),
                        Files.readString(file, StandardCharsets.UTF_8));
            }
        }
    }

    private static int lineOf(String source, int index) {
        int line = 1;
        for (int i = 0; i < index && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    @FunctionalInterface
    private interface MapperConsumer {
        void accept(String relativePath, String source);
    }
}
