package com.moyun.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.pay.config.PayProperties;
import com.moyun.pay.domain.entity.LedgerEntry;
import com.moyun.pay.mapper.LedgerEntryMapper;
import com.moyun.pay.service.ILedgerService;
import com.moyun.pay.service.IUserAccountService;
import com.moyun.system.service.ISysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 分账流水服务实现（V11.0 复式记账核心）
 *
 * <p>平台抽成率双轨配置：sys_config("pay.platform.fee-rate") 运行时可调 优先，
 * yaml(moyun.pay.platform-fee-rate) 兜底（默认 0.10 = 10%）。
 *
 * <p>守恒校验：platformAmount + userAmount == amount，任何偏差直接抛异常回滚。
 *
 * @author moyun
 */
@Service
public class LedgerServiceImpl implements ILedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceImpl.class);
    private static final String FEE_RATE_CONFIG_KEY = "pay.platform.fee-rate";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private IUserAccountService userAccountService;

    @Autowired
    private PayProperties payProperties;

    @Autowired
    private ISysConfigService configService;

    @Override
    public List<LedgerEntry> settle(String payNo, String bizType, String bizNo, long amount,
                                    Long userId, String summary) {
        double feeRate = resolveFeeRate();
        long platformAmount = Math.round(amount * feeRate);
        long userAmount = amount - platformAmount;

        // 守恒校验（红线）
        if (platformAmount + userAmount != amount || userAmount < 0) {
            throw new IllegalStateException("分账金额守恒校验失败 payNo=" + payNo
                    + " platform=" + platformAmount + " user=" + userAmount + " total=" + amount);
        }

        String settleNo = generateSettleNo();

        // 1. 用户入账（原子）并取回 balanceAfter
        long balanceAfter = userAccountService.credit(userId, userAmount);

        // 2. 平台分录
        LedgerEntry platformEntry = new LedgerEntry();
        platformEntry.setSettleNo(settleNo);
        platformEntry.setPayNo(payNo);
        platformEntry.setBizType(bizType);
        platformEntry.setBizNo(bizNo);
        platformEntry.setAccountRole(LedgerEntry.ROLE_PLATFORM);
        platformEntry.setUserId(0L);
        platformEntry.setDirection(LedgerEntry.DIRECTION_CREDIT);
        platformEntry.setAmount(platformAmount);
        platformEntry.setBalanceAfter(null);
        platformEntry.setSummary(summary + "服务费-平台抽成");
        platformEntry.setCreateTime(LocalDateTime.now());
        ledgerEntryMapper.insert(platformEntry);

        // 3. 用户分录（回填 balanceAfter）
        LedgerEntry userEntry = new LedgerEntry();
        userEntry.setSettleNo(settleNo);
        userEntry.setPayNo(payNo);
        userEntry.setBizType(bizType);
        userEntry.setBizNo(bizNo);
        userEntry.setAccountRole(LedgerEntry.ROLE_USER);
        userEntry.setUserId(userId);
        userEntry.setDirection(LedgerEntry.DIRECTION_CREDIT);
        userEntry.setAmount(userAmount);
        userEntry.setBalanceAfter(balanceAfter);
        userEntry.setSummary(summary + "收入-用户所得");
        userEntry.setCreateTime(LocalDateTime.now());
        ledgerEntryMapper.insert(userEntry);

        log.info("[ledger] 分账完成 payNo={} settleNo={} amount={}分 platform={}分({}%) user={}分 balanceAfter={}分",
                payNo, settleNo, amount, platformAmount, feeRate * 100, userAmount, balanceAfter);

        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(platformEntry);
        entries.add(userEntry);
        return entries;
    }

    @Override
    public IPage<LedgerEntry> myEntries(Long userId, long current, long size) {
        Page<LedgerEntry> page = new Page<>(current, size);
        IPage<LedgerEntry> result = ledgerEntryMapper.selectPage(page, new LambdaQueryWrapper<LedgerEntry>()
                .eq(LedgerEntry::getUserId, userId)
                .orderByDesc(LedgerEntry::getId));
        result.getRecords().forEach(this::fillYuan);
        return result;
    }

    /** 费率双轨：sys_config 运行时 > yaml 兜底 */
    private double resolveFeeRate() {
        try {
            String configValue = configService.selectConfigByKey(FEE_RATE_CONFIG_KEY);
            if (configValue != null && !configValue.isBlank()) {
                double parsed = Double.parseDouble(configValue.trim());
                if (parsed >= 0 && parsed < 1) {
                    return parsed;
                }
                log.warn("[ledger] 费率配置非法（需 0<=rate<1）: {}，回退 yaml", configValue);
            }
        } catch (Exception e) {
            log.warn("[ledger] 读取费率配置失败，回退 yaml：{}", e.getMessage());
        }
        return payProperties.getPlatformFeeRate();
    }

    private void fillYuan(LedgerEntry entry) {
        if (entry.getAmount() != null) {
            entry.setAmountYuan(BigDecimal.valueOf(entry.getAmount(), 2));
        }
        if (entry.getBalanceAfter() != null) {
            entry.setBalanceAfterYuan(BigDecimal.valueOf(entry.getBalanceAfter(), 2));
        }
    }

    private String generateSettleNo() {
        StringBuilder sb = new StringBuilder("STL").append(TS.format(LocalDateTime.now()));
        for (int i = 0; i < 6; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
