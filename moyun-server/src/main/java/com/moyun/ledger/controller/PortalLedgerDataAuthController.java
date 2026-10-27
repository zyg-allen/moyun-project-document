package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.PortalDataAuth;
import com.moyun.ledger.service.ILedgerDataAuthService;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户-用户数据授权（App「我的 → 隐私与授权」）
 *
 * <p>这是用户个体数据对后台开放的<b>唯一入口</b>：用户主动勾选范围、填写用途、选择期限，
 * 后台据此才可查看；用户可随时撤回，到期自动失效。</p>
 *
 * <p>与后台 {@code /cms/ledger/auth/**} 完全独立：后台只读授权与审计，不能代授权。</p>
 *
 * @author moyun
 */
@Tag(name = "门户-数据授权", description = "用户主动授权/撤回，供后台在授权范围内查看个体数据")
@RestController
@RequestMapping("/portal/ledger/data-auth")
public class PortalLedgerDataAuthController {

    /** 授权告知与隐私政策版本（正文由运营在 CMS 维护，此处仅版本号对齐） */
    private static final String POLICY_VERSION = "v1.0";

    /** 可选授权范围（key/label/desc，App 直接渲染） */
    private static final List<Map<String, String>> SCOPE_OPTIONS = new ArrayList<>();

    static {
        SCOPE_OPTIONS.add(scope(PortalDataAuth.SCOPE_TRANSACTION, "记账流水",
                "后台可查看你的收支流水类型、金额、分类与日期（不含备注、商户、凭证）"));
        SCOPE_OPTIONS.add(scope(PortalDataAuth.SCOPE_ACCOUNT, "账户余额",
                "后台可查看你的资产/负债账户余额与账户名称"));
        SCOPE_OPTIONS.add(scope(PortalDataAuth.SCOPE_BUDGET, "预算与存钱计划",
                "后台可查看你的预算设置与存钱计划进度"));
        SCOPE_OPTIONS.add(scope(PortalDataAuth.SCOPE_INVENTORY, "库存与商品",
                "后台可查看你的仓库、商品与出入库记录"));
        SCOPE_OPTIONS.add(scope(PortalDataAuth.SCOPE_AI, "AI 分析报告",
                "后台可查看你的 AI 财务分析报告内容"));
        SCOPE_OPTIONS.add(scope(PortalDataAuth.SCOPE_PROFILE, "身份与联系方式",
                "后台可查看你的手机号/邮箱等身份信息明文（用于客服核对）"));
    }

    @Autowired
    private ILedgerDataAuthService dataAuthService;

    /** 当前授权状态（未授权时 granted=false） */
    @Operation(summary = "当前授权状态")
    @GetMapping
    public AjaxResult status() {
        Long userId = PortalSecurityUtils.getUserId();
        PortalDataAuth auth = dataAuthService.findActive(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("granted", auth != null);
        data.put("status", dataAuthService.statusOf(auth));
        data.put("scopes", auth == null ? List.of() : splitScopes(auth.getScopes()));
        data.put("purpose", auth == null ? null : auth.getPurpose());
        data.put("grantedAt", auth == null ? null : format(auth.getGrantedAt()));
        data.put("expireAt", auth == null ? null : format(auth.getExpireAt()));
        return AjaxResult.success(data);
    }

    /** 授权告知：可选范围 + 说明 + 政策版本 */
    @Operation(summary = "授权范围与告知文案")
    @GetMapping("/policy")
    public AjaxResult policy() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("version", POLICY_VERSION);
        data.put("scopes", SCOPE_OPTIONS);
        data.put("notice", "授权后，平台客服可在你选择的范围内查看对应数据，用于你主动申请的核对/排障；"
                + "每次查看都会记录操作人与时间，到期自动失效，你可随时撤回。");
        return AjaxResult.success(data);
    }

    /** 发起授权（同域只保留最新一条有效授权） */
    @Operation(summary = "发起授权", description = "scopes 多选 + purpose 用途 + hours 有效小时（1~168，默认 24）")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerdataauth:", time = 60, count = 120)
    @PostMapping("/grant")
    public AjaxResult grant(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        List<String> scopes = asStringList(body.get("scopes"));
        String purpose = body.get("purpose") == null ? null : String.valueOf(body.get("purpose"));
        Integer hours = body.get("hours") == null ? 24 : Integer.valueOf(String.valueOf(body.get("hours")));
        PortalDataAuth auth = dataAuthService.grant(userId, scopes, purpose, hours, POLICY_VERSION);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("scopes", splitScopes(auth.getScopes()));
        data.put("expireAt", format(auth.getExpireAt()));
        return AjaxResult.success(data);
    }

    /** 撤回授权（立即生效） */
    @Operation(summary = "撤回授权")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "portalledgerdataauth:", time = 60, count = 120)
    @PutMapping("/revoke")
    public AjaxResult revoke(@RequestBody(required = false) Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        String reason = body == null || body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        dataAuthService.revoke(userId, reason);
        return AjaxResult.success();
    }

    // ==================== 工具 ====================

    private static Map<String, String> scope(String key, String label, String desc) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("desc", desc);
        return m;
    }

    private List<String> splitScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(scopes.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object raw) {
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
            return out;
        }
        return List.of();
    }

    private String format(LocalDateTime time) {
        return time == null ? null : time.toString().replace('T', ' ');
    }
}
