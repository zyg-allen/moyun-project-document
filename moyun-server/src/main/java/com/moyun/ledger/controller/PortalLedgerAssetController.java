package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerAssetAccount;
import com.moyun.ledger.service.ILedgerAssetAccountService;
import com.moyun.portal.util.PortalSecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 门户记账-资产账户控制器
 *
 * <p>安全红线：所有操作强制 user_id 归属校验（防水平越权）。
 *
 * @author moyun
 */
@RestController
@RequestMapping("/portal/ledger/assets")
public class PortalLedgerAssetController {

    @Autowired
    private ILedgerAssetAccountService assetAccountService;

    /** 资产账户列表 */
    @GetMapping
    public AjaxResult list(@RequestParam(required = false, defaultValue = "false") boolean includeArchived) {
        Long userId = PortalSecurityUtils.getUserId();
        List<LedgerAssetAccount> list = assetAccountService.listByUser(userId, includeArchived);
        return AjaxResult.success(Map.of("records", list, "total", list.size()));
    }

    /** 资产账户详情（含关联流水分页） */
    @GetMapping("/{id:[0-9]+}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        List<LedgerAssetAccount> list = assetAccountService.listByUser(userId, true);
        return AjaxResult.success(list.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .map(a -> (Object) a)
                .orElse("资产账户不存在"));
    }

    /** 新增资产账户（initialBalance 初始余额以 adjust 流水留痕） */
    @PostMapping
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerAssetAccount account = new LedgerAssetAccount();
        account.setName((String) body.get("name"));
        account.setType((String) body.get("type"));
        account.setIcon((String) body.get("icon"));
        if (body.get("includeInTotal") != null) {
            account.setIncludeInTotal(((Number) body.get("includeInTotal")).intValue());
        }
        BigDecimal initialBalance = body.get("initialBalance") == null ? null
                : new BigDecimal(body.get("initialBalance").toString());
        // 元单位边界校验：0 ≤ 元 ≤ 100 亿元
        final BigDecimal MAX_ASSET = new BigDecimal("10000000000");
        if (initialBalance != null && (initialBalance.compareTo(BigDecimal.ZERO) < 0 || initialBalance.compareTo(MAX_ASSET) > 0)) {
            return AjaxResult.error("初始余额超出允许范围（0 ~ 100 亿元），请确认金额单位");
        }
        return AjaxResult.success(assetAccountService.createAccount(userId, account, initialBalance));
    }

    /**
     * 修改资产账户（不含 balance，余额校准走 adjust 记账）
     *
     * <p>入参用 {@code Map} 而非实体绑定：需要区分"**未传**某字段"与"传了 null"——
     * 后者代表用户主动清空（可空列才允许清空）。原实体绑定 + {@code updateById} 的
     * "null 则跳过"语义会让"清空"静默失效，且会把整表写回（详见 service 内注释）。
     * 这里把 {@code body.keySet()}（显式出现的字段名）一并透传给 service。</p>
     */
    @PutMapping("/{id:[0-9]+}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        LedgerAssetAccount account = new LedgerAssetAccount();
        account.setId(id);
        // 白名单字段显式映射：不在白名单里的键（如 balance/version/status/initialBalance）一律忽略
        if (body.containsKey("name")) account.setName(LedgerAccountFields.asString(body.get("name")));
        if (body.containsKey("type")) account.setType(LedgerAccountFields.asString(body.get("type")));
        if (body.containsKey("icon")) account.setIcon(LedgerAccountFields.asString(body.get("icon")));
        if (body.containsKey("includeInTotal")) account.setIncludeInTotal(LedgerAccountFields.asInt(body.get("includeInTotal")));
        if (body.containsKey("hideBalance")) account.setHideBalance(LedgerAccountFields.asInt(body.get("hideBalance")));
        if (body.containsKey("sortOrder")) account.setSortOrder(LedgerAccountFields.asInt(body.get("sortOrder")));
        if (body.containsKey("valuation")) account.setValuation(LedgerAccountFields.asDecimal(body.get("valuation")));
        assetAccountService.updateAccount(userId, account, body.keySet());
        return AjaxResult.success("修改成功");
    }

    /** 删除资产账户（status=0 停用归档，流水永久保留） */
    @DeleteMapping("/{id:[0-9]+}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        Long userId = PortalSecurityUtils.getUserId();
        assetAccountService.deleteAccount(userId, id);
        return AjaxResult.success("删除成功");
    }
}
