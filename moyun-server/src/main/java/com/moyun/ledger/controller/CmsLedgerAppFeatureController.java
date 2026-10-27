package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.common.annotation.Log;
import com.moyun.common.annotation.RepeatSubmit;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ledger.domain.entity.LedgerAppFeatureConfig;
import com.moyun.ledger.mapper.LedgerAppFeatureConfigMapper;
import com.moyun.util.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 记账小程序功能入口配置（管理端）
 *
 * <p>"我的"页功能宫格与推荐位由后台可视化运营：名称/图标/主题色/角标/排序/显隐/路由。
 * 与门户端 {@code /portal/ledger/app-features}（只读、仅返回 visible=1）解耦。</p>
 *
 * <p>三条约束：
 * <ul>
 *   <li>内置入口（App 硬编码兜底清单里的 key）<b>不可删除</b>，只能隐藏；</li>
 *   <li>路由只能从白名单选择，避免配出"点了没反应"的死链；</li>
 *   <li>新增默认 visible=0（fail-closed），确认无误后再开启。</li>
 * </ul>
 *
 * @author moyun
 */
@Tag(name = "CMS记账-功能入口", description = "小程序「我的」页功能入口可视化配置")
@RestController
@RequestMapping("/cms/ledger/app-feature")
public class CmsLedgerAppFeatureController extends BaseController {

    private static final Set<String> BUILTIN_KEYS = new LinkedHashSet<>();

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,39}$");

    static {
        // 与 App pages.json / 内置兜底清单对齐（新增内置项时需同步这里，否则会被当成自定义入口允许删除）
        BUILTIN_KEYS.add("category");
        BUILTIN_KEYS.add("setting");
        BUILTIN_KEYS.add("savings");
        BUILTIN_KEYS.add("schedule");
        BUILTIN_KEYS.add("feedback");
        BUILTIN_KEYS.add("tip");
        BUILTIN_KEYS.add("personalize");
        BUILTIN_KEYS.add("catIcon");
        BUILTIN_KEYS.add("auto");
        BUILTIN_KEYS.add("backup");
        BUILTIN_KEYS.add("import");
        BUILTIN_KEYS.add("export");
        BUILTIN_KEYS.add("widget");
        BUILTIN_KEYS.add("tag");
        BUILTIN_KEYS.add("remind");
        BUILTIN_KEYS.add("reimburse");
        BUILTIN_KEYS.add("share");
        BUILTIN_KEYS.add("rate");
        BUILTIN_KEYS.add("qq");
        BUILTIN_KEYS.add("memo");
        BUILTIN_KEYS.add("list");
        BUILTIN_KEYS.add("translate");
        BUILTIN_KEYS.add("stock");
        BUILTIN_KEYS.add("gold");
        BUILTIN_KEYS.add("coupon");
        BUILTIN_KEYS.add("portal");
        BUILTIN_KEYS.add("warehouse");
        BUILTIN_KEYS.add("warehouseSetting");
        BUILTIN_KEYS.add("notifications");
        BUILTIN_KEYS.add("productCategory");
    }

    @Autowired
    private LedgerAppFeatureConfigMapper featureMapper;

    @Operation(summary = "功能配置列表（全量，含隐藏项）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerAppFeature:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        List<LedgerAppFeatureConfig> rows = featureMapper.selectList(
                new LambdaQueryWrapper<LedgerAppFeatureConfig>()
                        .orderByAsc(LedgerAppFeatureConfig::getGroupType)
                        .orderByAsc(LedgerAppFeatureConfig::getSortNum)
                        .orderByAsc(LedgerAppFeatureConfig::getId));
        rows.forEach(r -> r.setBuiltin(BUILTIN_KEYS.contains(r.getFeatureKey())));
        return AjaxResult.success(rows);
    }

    @Operation(summary = "新增自定义入口（默认隐藏）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerAppFeature:add')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-功能入口", businessType = BusinessType.INSERT)
    @com.moyun.common.annotation.RateLimiter(key = "cmsledgerappfeature:", time = 60, count = 120)
    @PostMapping
    public AjaxResult add(@RequestBody LedgerAppFeatureConfig body) {
        String key = requireKey(body.getFeatureKey());
        Long dup = featureMapper.selectCount(new LambdaQueryWrapper<LedgerAppFeatureConfig>()
                .eq(LedgerAppFeatureConfig::getFeatureKey, key));
        if (dup != null && dup > 0) {
            throw new ServiceException("功能标识「" + key + "」已存在");
        }
        LedgerAppFeatureConfig entity = new LedgerAppFeatureConfig();
        entity.setFeatureKey(key);
        entity.setFeatureName(requireName(body.getFeatureName()));
        entity.setGroupType(normalizeGroup(body.getGroupType()));
        entity.setIcon(trimToNull(body.getIcon()));
        entity.setIconColor(trimToNull(body.getIconColor()));
        entity.setBadge(trimToNull(body.getBadge()));
        entity.setSortNum(body.getSortNum() == null ? 0 : body.getSortNum());
        entity.setStatus(normalizeStatus(body.getStatus()));
        // fail-closed：新入口默认不展示，避免误上线
        entity.setVisible(body.getVisible() == null ? 0 : (body.getVisible() == 1 ? 1 : 0));
        entity.setRemark(trimToNull(body.getRemark()));
        entity.setRoute(validateRoute(body.getRoute()));
        entity.setCreateBy(SecurityUtils.getUsername());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return toAjax(featureMapper.insert(entity));
    }

    @Operation(summary = "修改功能配置（运营字段全量支持，空串=清空）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerAppFeature:edit')")
    @RepeatSubmit(interval = 3000)
    @Log(title = "记账-功能入口", businessType = BusinessType.UPDATE)
    @com.moyun.common.annotation.RateLimiter(key = "cmsledgerappfeature:", time = 60, count = 120)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody LedgerAppFeatureConfig body) {
        LedgerAppFeatureConfig db = featureMapper.selectById(id);
        if (db == null) {
            return AjaxResult.error("功能配置不存在");
        }
        LambdaUpdateWrapper<LedgerAppFeatureConfig> uw = new LambdaUpdateWrapper<>();
        uw.eq(LedgerAppFeatureConfig::getId, id)
                .set(body.getFeatureName() != null, LedgerAppFeatureConfig::getFeatureName,
                        body.getFeatureName() == null ? null : requireName(body.getFeatureName()))
                .set(body.getIcon() != null, LedgerAppFeatureConfig::getIcon, trimToNull(body.getIcon()))
                .set(body.getIconColor() != null, LedgerAppFeatureConfig::getIconColor, trimToNull(body.getIconColor()))
                .set(body.getBadge() != null, LedgerAppFeatureConfig::getBadge, trimToNull(body.getBadge()))
                .set(body.getSortNum() != null, LedgerAppFeatureConfig::getSortNum, body.getSortNum())
                .set(body.getVisible() != null, LedgerAppFeatureConfig::getVisible,
                        body.getVisible() != null && body.getVisible() == 1 ? 1 : 0)
                .set(body.getStatus() != null, LedgerAppFeatureConfig::getStatus, normalizeStatus(body.getStatus()))
                .set(body.getRemark() != null, LedgerAppFeatureConfig::getRemark, trimToNull(body.getRemark()))
                .set(body.getRoute() != null, LedgerAppFeatureConfig::getRoute, validateRoute(body.getRoute()))
                .set(body.getGroupType() != null, LedgerAppFeatureConfig::getGroupType,
                        body.getGroupType() == null ? null : normalizeGroup(body.getGroupType()))
                .set(LedgerAppFeatureConfig::getUpdateBy, SecurityUtils.getUsername())
                .set(LedgerAppFeatureConfig::getUpdateTime, LocalDateTime.now());
        return toAjax(featureMapper.update(null, uw));
    }

    @Operation(summary = "删除自定义入口（内置入口不可删除）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerAppFeature:edit')")
    @Log(title = "记账-功能入口", businessType = BusinessType.DELETE)
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "cmsledgerappfeature:", time = 60, count = 120)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        LedgerAppFeatureConfig db = featureMapper.selectById(id);
        if (db == null) {
            return AjaxResult.error("功能配置不存在");
        }
        if (BUILTIN_KEYS.contains(db.getFeatureKey())) {
            return AjaxResult.error("内置入口不可删除，请用「展示」开关控制");
        }
        return toAjax(featureMapper.deleteById(id));
    }

    // ==================== 校验 ====================

    private String requireKey(String key) {
        String k = key == null ? "" : key.trim();
        if (k.isEmpty()) {
            throw new ServiceException("功能标识不能为空");
        }
        if (!KEY_PATTERN.matcher(k).matches()) {
            throw new ServiceException("功能标识只能由字母开头的字母/数字组成（2~40 位）");
        }
        return k;
    }

    private String requireName(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) {
            throw new ServiceException("功能名称不能为空");
        }
        if (n.length() > 20) {
            throw new ServiceException("功能名称不能超过 20 个字");
        }
        return n;
    }

    private String normalizeGroup(String group) {
        if ("recommend".equals(group)) {
            return "recommend";
        }
        if (group == null || group.isBlank() || "main".equals(group)) {
            return "main";
        }
        throw new ServiceException("分组不合法（main/recommend）");
    }

    private String normalizeStatus(String status) {
        return "dev".equals(status) ? "dev" : "done";
    }

    /** 路由格式：功能入口由 admin 配置，后端只校验路径形态，不维护 App 页面清单 */
    private static final Pattern ROUTE_PATTERN = Pattern.compile("^/pages/[A-Za-z0-9_\\-/]+$");

    private String validateRoute(String route) {
        String r = trimToNull(route);
        if (r == null) {
            return null;
        }
        if (r.length() > 200 || !ROUTE_PATTERN.matcher(r).matches() || r.contains("..")) {
            throw new ServiceException("路由格式不合法，应以 /pages/ 开头且只含字母、数字、下划线、斜杠");
        }
        return r;
    }

    private String trimToNull(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        return t.isEmpty() ? null : t;
    }
}
