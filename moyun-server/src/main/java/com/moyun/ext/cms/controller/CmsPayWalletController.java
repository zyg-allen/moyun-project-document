package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.UserAccount;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.pay.mapper.UserAccountMapper;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import com.moyun.util.bean.PageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CMS 用户钱包 Controller（收入管理模块）
 *
 * <p>单钱包架构：pay_user_account 为全平台唯一钱包（社区钱包 portal_wallet 已废弃删除）。
 * 资金流水 Tab 沿用 /cms/pay/ledger/list（pay_ledger_entry 复式记账）。
 * 守恒对账：理论公账余额 = 平台抽成累计 + Σ用户余额。
 *
 * @author moyun
 */
@Tag(name = "CMS用户钱包", description = "唯一钱包余额列表 + 守恒对账汇总")
@RestController
@RequestMapping("/cms/pay/wallet")
public class CmsPayWalletController extends BaseController {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Operation(summary = "用户钱包列表", description = "pay_user_account 分页 + 昵称回填")
    @PreAuthorize("@ss.hasPermi('cms:payWallet:list')")
    @GetMapping("/accounts")
    public AjaxResult accounts(@RequestParam(required = false) Long userId,
                                @RequestParam(required = false) String nickname) {
        Page<UserAccount> page = PageUtils.startPage();
        QueryWrapper<UserAccount> qw = new QueryWrapper<UserAccount>()
                .eq(userId != null, "user_id", userId)
                .orderByDesc("update_time");
        // 昵称模糊筛选：先解析昵称→用户ID集合（有值才收窄，无匹配返回空页）
        if (nickname != null && !nickname.isBlank()) {
            Set<Long> matched = portalUserMapper.selectList(new QueryWrapper<PortalUser>()
                            .like("nickname", nickname.trim())
                            .select("id"))
                    .stream().map(PortalUser::getId).collect(Collectors.toSet());
            if (matched.isEmpty()) {
                return success(page);
            }
            qw.in("user_id", matched);
        }
        IPage<UserAccount> result = userAccountMapper.selectPage(page, qw);

        // 昵称批量回填（一次 IN）
        List<UserAccount> records = result.getRecords();
        if (!records.isEmpty()) {
            Set<Long> userIds = records.stream().map(UserAccount::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, String> nicknameMap = portalUserMapper.selectBatchIds(userIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(PortalUser::getId,
                            u -> u.getNickname() == null ? "" : u.getNickname(), (a, b) -> a));
            for (UserAccount a : records) {
                a.setNickname(nicknameMap.getOrDefault(a.getUserId(), "用户" + a.getUserId()));
            }
        }
        return success(result);
    }

    @Operation(summary = "钱包守恒对账汇总", description = "理论公账余额=平台抽成累计+Σ用户余额（App演示打赏无真实资金不计入）")
    @PreAuthorize("@ss.hasPermi('cms:payWallet:list')")
    @GetMapping("/summary")
    public AjaxResult summary() {
        // 钱包账户汇总（SQL 聚合）
        List<Map<String, Object>> accountRows = userAccountMapper.selectMaps(new QueryWrapper<UserAccount>()
                .select("COUNT(*) AS cnt", "COALESCE(SUM(balance), 0) AS total_balance",
                        "COALESCE(SUM(total_income), 0) AS total_income",
                        "COALESCE(SUM(total_withdraw), 0) AS total_withdraw"));
        Map<String, Object> accountRow = (accountRows == null || accountRows.isEmpty()) ? Map.of() : accountRows.get(0);

        // 平台抽成累计（pay_ledger_entry PLATFORM/credit，SQL SUM）
        List<Map<String, Object>> feeRows = ledgerEntryMapper.selectMaps(new QueryWrapper<LedgerEntry>()
                .select("COALESCE(SUM(amount), 0) AS total")
                .eq("account_role", LedgerEntry.ROLE_PLATFORM)
                .eq("direction", LedgerEntry.DIRECTION_CREDIT));
        BigDecimal platformFee = feeRows == null || feeRows.isEmpty() || feeRows.get(0).get("total") == null
                ? BigDecimal.ZERO : new BigDecimal(feeRows.get(0).get("total").toString());

        BigDecimal userBalanceSum = accountRow.get("total_balance") == null
                ? BigDecimal.ZERO : new BigDecimal(accountRow.get("total_balance").toString());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountCount", accountRow.getOrDefault("cnt", 0L));
        data.put("userBalanceSum", userBalanceSum);
        data.put("totalIncomeSum", accountRow.get("total_income") == null
                ? BigDecimal.ZERO : new BigDecimal(accountRow.get("total_income").toString()));
        data.put("totalWithdrawSum", accountRow.get("total_withdraw") == null
                ? BigDecimal.ZERO : new BigDecimal(accountRow.get("total_withdraw").toString()));
        data.put("platformFeeSum", platformFee);
        // 守恒：真钱全部在公账商户号，理论余额 = 平台抽成累计 + Σ用户余额（提现已从余额扣除）
        data.put("theoreticalAccountBalance", platformFee.add(userBalanceSum));
        return success(data);
    }
}
