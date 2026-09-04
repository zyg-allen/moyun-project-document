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

    /** 修改资产账户（不含 balance，余额校准走 adjust 记账） */
    @PutMapping("/{id:[0-9]+}")
    public AjaxResult update(@PathVariable("id") Long id, @RequestBody LedgerAssetAccount account) {
        Long userId = PortalSecurityUtils.getUserId();
        account.setId(id);
        assetAccountService.updateAccount(userId, account);
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
