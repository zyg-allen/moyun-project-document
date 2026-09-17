package com.moyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.core.mvc.handler.BusinessException;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.domain.entity.UserAccount;
import com.moyun.pay.domain.entity.UserBankCard;
import com.moyun.pay.domain.entity.WithdrawOrder;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.pay.mapper.UserBankCardMapper;
import com.moyun.pay.mapper.WithdrawOrderMapper;
import com.moyun.pay.service.IUserAccountService;
import com.moyun.pay.service.IWithdrawOrderService;
import com.moyun.portal.domain.entity.PortalUser;
import com.moyun.portal.mapper.PortalUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 提现单服务实现（提现闭环）
 *
 * <p>资金模型：公账商户号集中真钱，虚拟余额记账。发起仅校验不扣款；
 * 审核通过事务内原子扣减（balance >= amount 防超扣）+ 复式流水 debit + 置 paid；
 * 真实出金（商户号转账到银行卡）预留调用点，当前演示环境记账先行。
 *
 * @author moyun
 */
@Service
public class WithdrawOrderServiceImpl extends ServiceImpl<WithdrawOrderMapper, WithdrawOrder>
        implements IWithdrawOrderService {

    private static final Logger log = LoggerFactory.getLogger(WithdrawOrderServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private IUserAccountService userAccountService;

    @Autowired
    private UserBankCardMapper bankCardMapper;

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private PortalUserMapper portalUserMapper;

    @Override
    public WithdrawOrder apply(Long userId, BigDecimal amount, Long bankCardId) {
        if (userId == null) {
            throw new BusinessException("USER_NOT_LOGIN", "请先登录");
        }
        if (amount == null || amount.compareTo(new BigDecimal("1")) < 0) {
            throw new BusinessException("WITHDRAW_AMOUNT_INVALID", "提现金额不可低于 1 元");
        }
        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            throw new BusinessException("WITHDRAW_AMOUNT_INVALID", "单笔提现不可超过 50000 元");
        }
        // 1. 绑卡校验：必须为本人已核实卡
        UserBankCard card = bankCardId == null ? null : bankCardMapper.selectById(bankCardId);
        if (card == null || !Objects.equals(card.getUserId(), userId)) {
            throw new BusinessException("WITHDRAW_CARD_INVALID", "请选择本人绑定的银行卡");
        }
        if (!"VERIFIED".equals(card.getVerifyStatus())) {
            throw new BusinessException("WITHDRAW_CARD_NOT_VERIFIED", "该银行卡尚未核实通过");
        }
        // 2. 余额校验（只校验不扣款，审核通过时才原子扣减）
        UserAccount account = userAccountService.getOrCreate(userId);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("WITHDRAW_BALANCE_INSUFFICIENT", "可提现余额不足");
        }

        // 3. 落 auditing 提现单（手续费 0，后续可配置）
        WithdrawOrder order = new WithdrawOrder();
        order.setWithdrawNo(generateWithdrawNo());
        order.setUserId(userId);
        order.setAmount(amount);
        order.setFee(BigDecimal.ZERO);
        order.setBankCardId(bankCardId);
        order.setStatus(WithdrawOrder.STATUS_AUDITING);
        order.setCreateTime(LocalDateTime.now());
        this.save(order);
        log.info("[withdraw] 用户{}发起提现 {}元，单号{}", userId, amount, order.getWithdrawNo());
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditPass(Long withdrawId) {
        WithdrawOrder order = requireAuditing(withdrawId);
        // 1. 原子扣减（balance >= amount 防超扣；失败即驳回）
        boolean debited = userAccountService.debit(order.getUserId(), order.getAmount());
        if (!debited) {
            auditRejectInternal(order, "账户余额不足（并发变动），自动驳回");
            throw new BusinessException("WITHDRAW_BALANCE_INSUFFICIENT", "用户余额不足，已自动驳回");
        }
        // 2. 写资金流水（USER/debit，payNo=withdrawNo）
        UserAccount account = userAccountService.getOrCreate(order.getUserId());
        LedgerEntry entry = new LedgerEntry();
        entry.setPayNo(order.getWithdrawNo());
        entry.setBizType("withdraw");
        entry.setBizNo(String.valueOf(order.getId()));
        entry.setAccountRole(LedgerEntry.ROLE_USER);
        entry.setUserId(order.getUserId());
        entry.setDirection(LedgerEntry.DIRECTION_DEBIT);
        entry.setAmount(order.getAmount());
        entry.setBalanceAfter(account.getBalance());
        entry.setSummary("提现出金-打款至绑定银行卡");
        entry.setCreateTime(LocalDateTime.now());
        ledgerEntryMapper.insert(entry);

        // 3. 置 paid
        order.setStatus(WithdrawOrder.STATUS_PAID);
        order.setAuditTime(LocalDateTime.now());
        order.setPaidTime(LocalDateTime.now());
        this.updateById(order);

        // 4. 真实出金调用点（商户号转账，当前演示环境记账先行）
        log.info("[withdraw] 提现审核通过，单号{} 用户{} 出金{}元（真实打款通道预留）",
                order.getWithdrawNo(), order.getUserId(), order.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditReject(Long withdrawId, String reason) {
        WithdrawOrder order = requireAuditing(withdrawId);
        auditRejectInternal(order, reason == null || reason.isBlank() ? "不符合提现条件" : reason);
    }

    private void auditRejectInternal(WithdrawOrder order, String reason) {
        order.setStatus(WithdrawOrder.STATUS_REJECTED);
        order.setAuditTime(LocalDateTime.now());
        order.setRejectReason(reason);
        this.updateById(order);
        log.info("[withdraw] 提现单{}被驳回：{}", order.getWithdrawNo(), reason);
    }

    private WithdrawOrder requireAuditing(Long withdrawId) {
        WithdrawOrder order = this.getById(withdrawId);
        if (order == null) {
            throw new BusinessException("WITHDRAW_NOT_FOUND", "提现单不存在");
        }
        if (!WithdrawOrder.STATUS_AUDITING.equals(order.getStatus())) {
            throw new BusinessException("WITHDRAW_STATUS_INVALID", "提现单已处理（当前状态：" + order.getStatus() + "）");
        }
        return order;
    }

    @Override
    public IPage<WithdrawOrder> myWithdrawals(Long userId, long current, long size) {
        return this.page(new Page<>(current, size), new QueryWrapper<WithdrawOrder>()
                .eq("user_id", userId)
                .orderByDesc("id"));
    }

    @Override
    public Map<String, Object> adminList(String status, Long userId, long current, long size) {
        Page<WithdrawOrder> page = new Page<>(current, size);
        IPage<WithdrawOrder> result = this.page(page, new QueryWrapper<WithdrawOrder>()
                .eq(status != null && !status.isBlank(), "status", status)
                .eq(userId != null, "user_id", userId)
                .orderByDesc("id"));

        // 批量回填昵称 + 银行卡脱敏（一次 IN，避免 N+1）
        List<WithdrawOrder> records = result.getRecords();
        if (!records.isEmpty()) {
            Set<Long> userIds = records.stream().map(WithdrawOrder::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Long> cardIds = records.stream().map(WithdrawOrder::getBankCardId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, String> nicknameMap = userIds.isEmpty() ? Map.of() : portalUserMapper.selectBatchIds(userIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(PortalUser::getId, u -> u.getNickname() == null ? "" : u.getNickname(), (a, b) -> a));
            Map<Long, UserBankCard> cardMap = cardIds.isEmpty() ? Map.of() : bankCardMapper.selectBatchIds(cardIds).stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(UserBankCard::getId, c -> c, (a, b) -> a));
            for (WithdrawOrder o : records) {
                o.setNickname(nicknameMap.getOrDefault(o.getUserId(), o.getUserId() == null ? "" : "用户" + o.getUserId()));
                UserBankCard card = o.getBankCardId() == null ? null : cardMap.get(o.getBankCardId());
                o.setBankCardDesc(card == null ? "" : (card.getBankName() == null ? "" : card.getBankName()) + " " + card.getCardNoMasked());
            }
        }

        // 汇总卡片（SQL 聚合）
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("auditingAmount", sumStatus(WithdrawOrder.STATUS_AUDITING));
        summary.put("paidAmount", sumStatus(WithdrawOrder.STATUS_PAID));
        summary.put("rejectedAmount", sumStatus(WithdrawOrder.STATUS_REJECTED));

        Map<String, Object> data = new HashMap<>();
        data.put("page", result);
        data.put("summary", summary);
        return data;
    }

    private BigDecimal sumStatus(String status) {
        List<Map<String, Object>> rows = this.listMaps(new QueryWrapper<WithdrawOrder>()
                .select("COALESCE(SUM(amount), 0) AS total", "COUNT(*) AS cnt")
                .eq("status", status));
        if (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).get("total") == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(rows.get(0).get("total").toString());
    }

    @Override
    public Map<String, Object> userSummary(Long userId) {
        UserAccount account = userAccountService.getOrCreate(userId);
        List<Map<String, Object>> rows = this.listMaps(new QueryWrapper<WithdrawOrder>()
                .select("COALESCE(SUM(amount), 0) AS total")
                .eq("user_id", userId)
                .eq("status", WithdrawOrder.STATUS_AUDITING));
        BigDecimal auditing = (rows == null || rows.isEmpty() || rows.get(0) == null || rows.get(0).get("total") == null)
                ? BigDecimal.ZERO : new BigDecimal(rows.get(0).get("total").toString());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("balance", account.getBalance());
        data.put("totalIncome", account.getTotalIncome());
        data.put("totalWithdraw", account.getTotalWithdraw());
        data.put("auditingAmount", auditing);
        return data;
    }

    private String generateWithdrawNo() {
        return "WD" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                + String.format("%06d", RANDOM.nextInt(1000000));
    }
}
