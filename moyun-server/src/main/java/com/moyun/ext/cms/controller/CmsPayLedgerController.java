package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMS 分账流水后台管理 Controller（V11.0）
 *
 * <p>复式记账双视角：全平台流水（含平台分录）+ 汇总统计。
 *
 * <p>v11.78：列表关联用户昵称（portal_user）；汇总改为 SQL SUM 聚合（禁全表 selectList 内存累加）。
 *
 * @author moyun
 */
@Tag(name = "CMS分账流水管理", description = "平台抽成与用户所得分账流水")
@RestController
@RequestMapping("/cms/pay/ledger")
public class CmsPayLedgerController extends BaseController {

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Operation(summary = "分账流水列表", description = "分页查询全平台资金流水，支持账户角色/方向/支付单号/用户筛选，含用户昵称")
    @PreAuthorize("@ss.hasPermi('cms:payLedger:list')")
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
        fillNicknames(page.getRecords());
        return success(page);
    }

    @Operation(summary = "单笔支付分账明细", description = "按支付单号查询该单全部分账流水（守恒可见）")
    @PreAuthorize("@ss.hasPermi('cms:payLedger:query')")
    @GetMapping("/{payNo}/detail")
    public AjaxResult detail(@PathVariable String payNo) {
        List<LedgerEntry> entries = ledgerEntryMapper.selectList(new LambdaQueryWrapper<LedgerEntry>()
                .eq(LedgerEntry::getPayNo, payNo)
                .orderByAsc(LedgerEntry::getId));
        fillNicknames(entries);
        return success(entries);
    }

    @Operation(summary = "分账汇总", description = "平台抽成总额 / 用户所得总额 / 流水笔数（SQL 聚合）")
    @PreAuthorize("@ss.hasPermi('cms:payLedger:summary')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        BigDecimal platformTotal = sumAmount(LedgerEntry.ROLE_PLATFORM);
        BigDecimal userTotal = sumAmount(LedgerEntry.ROLE_USER);
        Long totalEntries = ledgerEntryMapper.selectCount(null);
        Map<String, Object> data = new HashMap<>();
        data.put("platformTotal", platformTotal);
        data.put("userTotal", userTotal);
        data.put("totalEntries", totalEntries == null ? 0 : totalEntries);
        return success(data);
    }

    /**
     * SQL SUM 聚合（v11.78：替代原 selectList 内存累加，遵守"禁止全表内存聚合"铁律）
     */
    private BigDecimal sumAmount(String accountRole) {
        QueryWrapper<LedgerEntry> qw = new QueryWrapper<LedgerEntry>()
                .select("COALESCE(SUM(amount), 0) AS total")
                .eq("account_role", accountRole)
                .eq("direction", LedgerEntry.DIRECTION_CREDIT);
        List<Map<String, Object>> rows = ledgerEntryMapper.selectMaps(qw);
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        Object total = rows.get(0).get("total");
        return total == null ? BigDecimal.ZERO.setScale(2)
                : new BigDecimal(total.toString()).setScale(2);
    }

    /**
     * 批量填充用户昵称（v11.78：仅对当前页 USER 分录做一次 IN 查询，避免 N+1）
     */
    private void fillNicknames(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        Set<Long> userIds = entries.stream()
                .filter(e -> e.getUserId() != null && e.getUserId() > 0)
                .map(LedgerEntry::getUserId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> nicknameMap = portalUserMapper.selectBatchIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PortalUser::getId, u -> u.getNickname() == null ? "" : u.getNickname(), (a, b) -> a));
        for (LedgerEntry entry : entries) {
            if (entry.getUserId() == null || entry.getUserId() <= 0) {
                continue; // PLATFORM 分录：前端显示"平台"
            }
            String nickname = nicknameMap.getOrDefault(entry.getUserId(), "");
            entry.setNickname(nickname.isEmpty() ? "用户" + entry.getUserId() : nickname);
        }
    }
}
