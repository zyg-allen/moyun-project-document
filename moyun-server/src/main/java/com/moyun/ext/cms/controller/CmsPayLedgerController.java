package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * CMS 分账流水后台管理 Controller（V11.0）
 *
 * <p>复式记账双视角：全平台流水（含平台分录）+ 汇总统计。
 *
 * @author moyun
 */
@Tag(name = "CMS分账流水管理", description = "平台抽成与用户所得分账流水")
@RestController
@RequestMapping("/cms/pay/ledger")
public class CmsPayLedgerController extends BaseController {

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Operation(summary = "分账流水列表", description = "分页查询全平台资金流水，支持账户角色/方向/支付单号筛选")
    @PreAuthorize("@ss.hasPermi('pay:ledger:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String accountRole,
                           @RequestParam(required = false) String direction,
                           @RequestParam(required = false) String payNo,
                           @RequestParam(required = false) Long userId) {
        Page<LedgerEntry> page = PageUtils.startPage();
        LambdaQueryWrapper<LedgerEntry> wrapper = new LambdaQueryWrapper<LedgerEntry>()
                .eq(accountRole != null && !accountRole.isBlank(), LedgerEntry::getAccountRole, accountRole)
                .eq(direction != null && !direction.isBlank(), LedgerEntry::getDirection, direction)
                .like(payNo != null && !payNo.isBlank(), LedgerEntry::getPayNo, payNo)
                .eq(userId != null, LedgerEntry::getUserId, userId)
                .orderByDesc(LedgerEntry::getId);
        ledgerEntryMapper.selectPage(page, wrapper);
        page.getRecords().forEach(this::fillYuan);
        return success(page);
    }

    @Operation(summary = "单笔支付分账明细", description = "按支付单号查询该单全部分账流水（守恒可见）")
    @PreAuthorize("@ss.hasPermi('pay:ledger:query')")
    @GetMapping("/{payNo}/detail")
    public AjaxResult detail(@PathVariable String payNo) {
        return success(ledgerEntryMapper.selectList(new LambdaQueryWrapper<LedgerEntry>()
                .eq(LedgerEntry::getPayNo, payNo)
                .orderByAsc(LedgerEntry::getId)));
    }

    @Operation(summary = "分账汇总", description = "平台抽成总额 / 用户所得总额 / 流水笔数")
    @PreAuthorize("@ss.hasPermi('pay:ledger:summary')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        long platformTotal = sumAmount(LedgerEntry.ROLE_PLATFORM);
        long userTotal = sumAmount(LedgerEntry.ROLE_USER);
        Long totalEntries = ledgerEntryMapper.selectCount(null);
        Map<String, Object> data = new HashMap<>();
        data.put("platformTotal", platformTotal);
        data.put("platformTotalYuan", BigDecimal.valueOf(platformTotal, 2));
        data.put("userTotal", userTotal);
        data.put("userTotalYuan", BigDecimal.valueOf(userTotal, 2));
        data.put("totalEntries", totalEntries == null ? 0 : totalEntries);
        return success(data);
    }

    private long sumAmount(String accountRole) {
        long total = 0;
        for (LedgerEntry entry : ledgerEntryMapper.selectList(new LambdaQueryWrapper<LedgerEntry>()
                .eq(LedgerEntry::getAccountRole, accountRole)
                .eq(LedgerEntry::getDirection, LedgerEntry.DIRECTION_CREDIT))) {
            if (entry.getAmount() != null) {
                total += entry.getAmount();
            }
        }
        return total;
    }

    private void fillYuan(LedgerEntry entry) {
        if (entry.getAmount() != null) {
            entry.setAmountYuan(BigDecimal.valueOf(entry.getAmount(), 2));
        }
        if (entry.getBalanceAfter() != null) {
            entry.setBalanceAfterYuan(BigDecimal.valueOf(entry.getBalanceAfter(), 2));
        }
    }
}
