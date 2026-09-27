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
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构守卫：**模块依赖方向**（报告 §6.3「分层与模块边界」的可执行门禁）
 *
 * <h3>为什么需要</h3>
 * <p>分层方向的破坏是"编译能过、测试能过、没人发现"的典型：{@code core} 里 import 一个
 * {@code portal} 主体类、{@code util} 里 import 一个门户实体，都能正常构建。报告 §6.3 记了
 * 4 类问题（{@code core} 反向依赖、{@code portal}⇄{@code ext.cms} 循环、{@code system}→{@code portal}
 * 无防腐层、{@code pay}/{@code vip}/{@code ledger} 咬合）。本守卫把"能接受的现状"变成
 * **显式冻结清单**（带理由与精确计数），任何**新增**违规都会让测试失败。</p>
 *
 * <h3>两条规则</h3>
 * <ol>
 *   <li><b>硬零规则</b>：基础设施模块不得依赖业务/上层模块 ——
 *       {@code core → {portal, ext.*, ledger, pay, vip}} 与
 *       {@code util → {portal, ext.*, ledger, pay, vip}} 必须为 0（v13.11 已清理完毕）；</li>
 *   <li><b>冻结清单</b>：RuoYi 遗留/需独立批次处理的耦合，按 {@code 边 → 精确计数} 锁定，
 *       并在注释里写明**为什么现在不动**。计数变化（无论增减）都会失败，强制清单保持真实。</li>
 * </ol>
 *
 * <p>依赖方向原则（本项目）：{@code common/util}（基础）→ {@code core}（安全/配置/横切）→
 * {@code system}（管理端）/ {@code portal}（门户端）/ {@code ledger·pay·vip}（业务域）→ {@code ext.*}（扩展域）。
 * 业务域之间、业务域与扩展域之间的双向依赖属于**已登记债务**，不得扩大。</p>
 *
 * @author moyun
 * @see AsyncSelfInvocationGuardTest 同族守卫（把架构约束变成测试）
 * @see ExecutorGovernanceGuardTest
 * @see MapperTemplateGuardTest
 */
class ModuleDependencyGuardTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java");

    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+);", Pattern.MULTILINE);

    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?(com\\.moyun\\.[\\w.]+);",
            Pattern.MULTILINE);

    /** 基础设施模块：不得依赖业务/上层 */
    private static final List<String> INFRA_MODULES = List.of("core", "util");

    /** 业务/上层模块前缀（基础设施不得依赖它们） */
    private static final List<String> BUSINESS_MODULES = List.of("portal", "ledger", "pay", "vip", "ext.");

    /**
     * 冻结清单：边 → 精确 import 计数（必须与实测一致）。
     *
     * <p>每条都写明"为什么现在不动"。改动耦合后必须同步这里（增减都会失败，防止清单腐烂）。</p>
     */
    private static final Map<String, Integer> FROZEN_EDGES = new LinkedHashMap<>();

    /** 冻结理由（键同 {@link #FROZEN_EDGES}） */
    private static final Map<String, String> FROZEN_REASONS = new LinkedHashMap<>();

    static {
        // RuoYi 认证/审计层：AsyncFactory / LogAspect / SysLoginService / SysRegisterService /
        // UserDetailsServiceImpl / SysPermissionService 直接使用 system 的实体与服务。
        // 彻底修复需把这些实现类迁到 system（core 只留安全基建）或做接口倒置 —— 影响登录鉴权主干，
        // 需独立批次 + 登录链路回归，故本批冻结。
        FROZEN_EDGES.put("core -> system", 13);
        FROZEN_REASONS.put("core -> system", "RuoYi 认证/审计层遗留（登录鉴权主干，需独立批次做接口倒置/迁移）");

        // 「注解 → 序列化器」配对：common 的 @Sensitive 必须引用序列化器，而序列化器需要登录态
        // （core.base.model.LoginUser）判断管理员是否脱敏 —— v13.11 已删掉 common 里的重复副本，
        // 只剩这 1 处结构上不可消除的引用。
        FROZEN_EDGES.put("common -> core", 1);
        FROZEN_REASONS.put("common -> core", "@Sensitive 注解→序列化器配对，序列化器需登录态；重复副本已删，剩 1 处不可消除");

        // 管理端直连门户数据层：v13.22 已为 8 个 *AuditBizHandler 建好防腐层
        // （system 侧只依赖 core.portal.AuditContentPort，实现落在 portal.audit.AuditContentAdapter），
        // 24 → 6。剩余 6 条：SysDashboardServiceImpl(2: 门户文章/浏览 Mapper)、
        // SysNotificationServiceImpl(1: 门户用户 Mapper)、SysMessageController(3: 门户用户服务/实体/DTO)
        // —— 属看板聚合、通知收件人与"管理员代发私信"，各需独立端口，列为后续批次。
        FROZEN_EDGES.put("system -> portal", 6);
        FROZEN_REASONS.put("system -> portal", "管理端直连门户数据层：审核中心 8 个 Handler 已于 v13.22 走防腐层端口"
                + "（24→6）；余 6 条为看板聚合/通知收件人/管理员代发私信，待各自建端口");

        // portal ⇄ ext.cms 双向咬合：共享实体与查询对象、控制器互调。
        // 拆分需要抽出共享领域模块，属架构级改造，需独立批次。
        // v13.22：防腐层适配器落在门户侧，需调用 CMS 的文章/专栏/面试/举报下架服务，故 78 → 82（+4，ACL 的合理代价）。
        FROZEN_EDGES.put("portal -> ext.cms", 82);
        FROZEN_REASONS.put("portal -> ext.cms", "CMS 与门户共享实体/查询对象（拆分需抽公共领域模块）；"
                + "v13.22 防腐层适配器 AuditContentAdapter 调用 CMS 4 个业务服务，78→82");
        FROZEN_EDGES.put("ext.cms -> portal", 278);
        FROZEN_REASONS.put("ext.cms -> portal", "同上（反向）；v13.11 迁入 ImportExportHelper 后为 280，"
                + "v13.16 删掉 CmsInterviewController 两个已失效的 portal 依赖（PortalUserStatsMapper/IPortalGrowthService）后降至 278");

        // pay / vip / ledger 三者咬合：记账权威实体寄居 pay（LedgerEntry），VIP 依赖支付网关。
        FROZEN_EDGES.put("ledger -> pay", 12);
        FROZEN_REASONS.put("ledger -> pay", "记账权威实体寄居 pay（LedgerEntry），迁移需独立批次");
        FROZEN_EDGES.put("vip -> pay", 4);
        FROZEN_REASONS.put("vip -> pay", "VIP 发卡依赖支付回调/网关类型");
        FROZEN_EDGES.put("ledger -> vip", 4);
        FROZEN_REASONS.put("ledger -> vip", "记账端 VIP 付费点（@VipOnly + 套餐实体）");
    }

    @Test
    @DisplayName("硬零规则：core/util 不得依赖业务模块（portal/ext.*/ledger/pay/vip）")
    void infrastructureMustNotDependOnBusinessModules() throws IOException {
        Map<String, Integer> edges = scanEdges(loadSources());
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : edges.entrySet()) {
            String[] parts = entry.getKey().split(" -> ");
            if (!INFRA_MODULES.contains(parts[0])) {
                continue;
            }
            boolean business = BUSINESS_MODULES.stream().anyMatch(prefix -> parts[1].startsWith(prefix));
            if (business) {
                violations.add(entry.getKey() + "（" + entry.getValue() + " 处 import）");
            }
        }
        assertTrue(violations.isEmpty(),
                "基础设施模块不得依赖业务模块（core/util 是底层，反向依赖会让分层失效）。\n"
                        + "修法：在 core/util 侧定义接口或做依赖倒置，由业务模块实现；"
                        + "跨模块的数据结构请下沉到 common/core。\n  " + String.join("\n  ", violations));
    }

    @Test
    @DisplayName("冻结清单：已登记债务的计数必须与实测完全一致（增减都要更新清单）")
    void frozenEdgesMatchExactly() throws IOException {
        Map<String, Integer> edges = scanEdges(loadSources());
        List<String> problems = new ArrayList<>();

        for (Map.Entry<String, Integer> frozen : FROZEN_EDGES.entrySet()) {
            int expected = frozen.getValue();
            int actual = edges.getOrDefault(frozen.getKey(), 0);
            if (actual != expected) {
                problems.add(frozen.getKey() + "：期望 " + expected + "，实际 " + actual
                        + "（" + FROZEN_REASONS.get(frozen.getKey()) + "）");
            }
        }
        // 反向自检：清单里每条都必须有理由
        for (String edge : FROZEN_EDGES.keySet()) {
            assertTrue(FROZEN_REASONS.containsKey(edge) && FROZEN_REASONS.get(edge).length() > 10,
                    "冻结清单必须写明理由：" + edge);
        }
        assertTrue(problems.isEmpty(),
                "模块耦合计数与冻结清单不一致。\n"
                        + "  · 计数**增加** = 新增了跨模块依赖，请先判断方向是否正确，必要时改为依赖倒置；\n"
                        + "  · 计数**减少** = 债务已偿还，请同步下调清单数字（防止清单腐烂）。\n  "
                        + String.join("\n  ", problems));
    }

    @Test
    @DisplayName("清单自检：冻结边不得是硬零规则已消灭的边（防豁免残留）")
    void frozenEdgesMustNotContainHardZeroEdges() {
        for (String edge : FROZEN_EDGES.keySet()) {
            String[] parts = edge.split(" -> ");
            if (!INFRA_MODULES.contains(parts[0])) {
                continue;
            }
            boolean business = BUSINESS_MODULES.stream().anyMatch(prefix -> parts[1].startsWith(prefix));
            assertFalse(business, "该边已被硬零规则禁止，不应出现在冻结清单：" + edge);
        }
    }

    @Test
    @DisplayName("扫描器自检：能识别边与计数，忽略自依赖与外部包")
    void scannerWorksOnFixtures() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("com/moyun/core/Demo.java",
                "package com.moyun.core;\nimport com.moyun.portal.domain.entity.PortalUser;\n"
                        + "import com.moyun.core.base.AjaxResult;\nimport java.util.List;\n");
        sources.put("com/moyun/ext/cms/Other.java",
                "package com.moyun.ext.cms;\nimport com.moyun.portal.domain.model.PortalLoginUser;\n");
        sources.put("com/moyun/portal/Third.java",
                "package com.moyun.portal;\nimport static com.moyun.ext.cms.Foo.bar;\n");

        Map<String, Integer> edges = scanEdges(sources);

        assertEquals(1, edges.getOrDefault("core -> portal", 0), "core→portal 应被识别");
        assertEquals(1, edges.getOrDefault("ext.cms -> portal", 0), "ext.cms 应按子模块归类");
        assertEquals(1, edges.getOrDefault("portal -> ext.cms", 0), "静态 import 也要识别");
        assertFalse(edges.containsKey("core -> core"), "同模块依赖不应计入");
        assertEquals(3, edges.size(), "应产出 3 条边（core→portal、ext.cms→portal、portal→ext.cms）；"
                + "同模块与 java.util 等外部包被忽略，实际=" + edges.keySet());
    }

    @Test
    @DisplayName("扫描器自检：方法体内的全限定名不计入（只统计 import，避免误报）")
    void scannerCountsImportsOnly() {
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("com/moyun/util/Demo.java",
                "package com.moyun.util;\npublic class Demo {\n"
                        + "  void run() { com.moyun.portal.util.PortalSecurityUtils.getUserId(); }\n}\n");

        Map<String, Integer> edges = scanEdges(sources);

        assertFalse(edges.containsKey("util -> portal"),
                "方法体内的 FQN 不构成模块依赖声明（import 才是编译期契约）；如需收敛请补 import");
    }

    // ==================== 扫描实现 ====================

    /** 扫描全部源码：返回 边（"a -> b"）→ import 计数 */
    static Map<String, Integer> scanEdges(Map<String, String> sources) {
        Map<String, Integer> edges = new TreeMap<>();
        for (Map.Entry<String, String> entry : sources.entrySet()) {
            String content = entry.getValue();
            String from = moduleOf(findPackage(content));
            if (from == null) {
                continue;
            }
            Matcher matcher = IMPORT.matcher(content);
            while (matcher.find()) {
                String to = moduleOf(matcher.group(1));
                if (to == null || to.equals(from)) {
                    continue;
                }
                edges.merge(from + " -> " + to, 1, Integer::sum);
            }
        }
        return edges;
    }

    private static String findPackage(String content) {
        Matcher matcher = PACKAGE.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** 取模块名：{@code com.moyun.core.*} → {@code core}；{@code com.moyun.ext.cms.*} → {@code ext.cms} */
    static String moduleOf(String pkg) {
        if (pkg == null || !pkg.startsWith("com.moyun.")) {
            return null;
        }
        String[] parts = pkg.split("\\.");
        if (parts.length < 3) {
            return null; // com.moyun.Xxx（启动类等）不计
        }
        if ("ext".equals(parts[2])) {
            return parts.length >= 4 ? "ext." + parts[3] : null;
        }
        return parts[2];
    }

    private static Map<String, String> loadSources() throws IOException {
        Map<String, String> sources = new LinkedHashMap<>();
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                sources.put(SOURCE_ROOT.relativize(file).toString().replace('\\', '/'),
                        Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return sources;
    }
}
