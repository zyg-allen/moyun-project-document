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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分账流水服务实现（复式记账核心）
 *
 * <p>平台抽成率双轨配置：sys_config("pay.platform.fee-rate") 运行时可调 优先，
 * yaml(moyun.pay.platform-fee-rate) 兜底（默认 0.10 = 10%）。
 *
 * <p>金额单位：元（BigDecimal，统一）。
 * 守恒校验：platformAmount + userAmount == amount（分），任何偏差直接抛异常回滚。
 *
 * @author moyun
 */
@Service
public class LedgerServiceImpl implements ILedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceImpl.class);
    private static final String FEE_RATE_CONFIG_KEY = "pay.platform.fee-rate";

    @Autowired
    private LedgerEntryMapper ledgerEntryMapper;

    @Autowired
    private IUserAccountService userAccountService;

    @Autowired
    private PayProperties payProperties;

    @Autowired
    private ISysConfigService configService;

    @Override
    public List<LedgerEntry> settle(String payNo, String bizType, String bizNo, BigDecimal amount,
                                    Long userId, String summary, String platformCode) {
        double feeRate = resolveFeeRate(platformCode);
        BigDecimal platformAmount = amount.multiply(BigDecimal.valueOf(feeRate))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal userAmount = amount.subtract(platformAmount);

        // 守恒校验（红线）
        if (platformAmount.add(userAmount).compareTo(amount) != 0
                || userAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("分账金额守恒校验失败 payNo=" + payNo
                    + " platform=" + platformAmount + " user=" + userAmount + " total=" + amount);
        }

        // 1. 用户入账（原子）并取回 balanceAfter
        BigDecimal balanceAfter = userAccountService.credit(userId, userAmount);

        // 2. 平台分录
        LedgerEntry platformEntry = new LedgerEntry();
        platformEntry.setPayNo(payNo);
        platformEntry.setBizType(bizType);
        platformEntry.setBizNo(bizNo);
        platformEntry.setAccountRole(LedgerEntry.ROLE_PLATFORM);
        platformEntry.setUserId(0L);
        platformEntry.setDirection(LedgerEntry.DIRECTION_CREDIT);
        platformEntry.setAmount(platformAmount);
        platformEntry.setBalanceAfter(null);
        platformEntry.setSummary(summary + "服务费-平台抽成");
        platformEntry.setPlatformCode(platformCode);
        platformEntry.setCreateTime(LocalDateTime.now());
        ledgerEntryMapper.insert(platformEntry);

        // 3. 用户分录（回填 balanceAfter）
        LedgerEntry userEntry = new LedgerEntry();
        userEntry.setPayNo(payNo);
        userEntry.setBizType(bizType);
        userEntry.setBizNo(bizNo);
        userEntry.setAccountRole(LedgerEntry.ROLE_USER);
        userEntry.setUserId(userId);
        userEntry.setDirection(LedgerEntry.DIRECTION_CREDIT);
        userEntry.setAmount(userAmount);
        userEntry.setBalanceAfter(balanceAfter);
        userEntry.setSummary(summary + "收入-用户所得");
        userEntry.setPlatformCode(platformCode);
        userEntry.setCreateTime(LocalDateTime.now());
        ledgerEntryMapper.insert(userEntry);

        log.info("[ledger] 分账完成 payNo={} amount={}元 platform={}元({}%) user={}元 balanceAfter={}元 platformCode={}",
                payNo, amount, platformAmount, feeRate * 100, userAmount, balanceAfter, platformCode);

        List<LedgerEntry> entries = new ArrayList<>();
        entries.add(platformEntry);
        entries.add(userEntry);
        return entries;
    }

    @Override
    public LedgerEntry settlePlatform(String payNo, String bizType, String bizNo, BigDecimal amount,
                                      String summary, String platformCode) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("平台全额入账金额非法 payNo=" + payNo + " amount=" + amount);
        }
        // 平台全额分录（无第三方收款人，如记账App打赏）
        LedgerEntry platformEntry = new LedgerEntry();
        platformEntry.setPayNo(payNo);
        platformEntry.setBizType(bizType);
        platformEntry.setBizNo(bizNo);
        platformEntry.setAccountRole(LedgerEntry.ROLE_PLATFORM);
        platformEntry.setUserId(0L);
        platformEntry.setDirection(LedgerEntry.DIRECTION_CREDIT);
        platformEntry.setAmount(amount);
        platformEntry.setBalanceAfter(null);
        platformEntry.setSummary(summary);
        platformEntry.setPlatformCode(platformCode);
        platformEntry.setCreateTime(LocalDateTime.now());
        ledgerEntryMapper.insert(platformEntry);
        log.info("[ledger] 平台全额入账完成 payNo={} amount={}元 bizType={} bizNo={} platformCode={}",
                payNo, amount, bizType, bizNo, platformCode);
        return platformEntry;
    }

    @Override
    public IPage<LedgerEntry> myEntries(Long userId, long current, long size) {
        Page<LedgerEntry> page = new Page<>(current, size);
        IPage<LedgerEntry> result = ledgerEntryMapper.selectPage(page, new LambdaQueryWrapper<LedgerEntry>()
                .eq(LedgerEntry::getUserId, userId)
                .orderByDesc(LedgerEntry::getId));
        return result;
    }

    /**
     * 费率三轨：sys_config(platform_code=端级) > sys_config(platform_code IS NULL=全局) > yaml 兜底
     * 端级费率键：pay.{platformCode}.fee-rate（如 pay.ledger.fee-rate）
     * 全局费率键：pay.platform.fee-rate
     */
    private double resolveFeeRate(String platformCode) {
        try {
            // 1. 端级费率优先
            if (platformCode != null && !platformCode.isBlank()) {
                String platformKey = "pay." + platformCode + ".fee-rate";
                String platformValue = configService.selectConfigByKey(platformKey);
                if (platformValue != null && !platformValue.isBlank()) {
                    double parsed = Double.parseDouble(platformValue.trim());
                    if (parsed >= 0 && parsed < 1) {
                        return parsed;
                    }
                    log.warn("[ledger] 端级费率配置非法（需 0<=rate<1）: {}={}，回退全局", platformKey, platformValue);
                }
            }
            // 2. 全局费率
            String configValue = configService.selectConfigByKey(FEE_RATE_CONFIG_KEY);
            if (configValue != null && !configValue.isBlank()) {
                double parsed = Double.parseDouble(configValue.trim());
                if (parsed >= 0 && parsed < 1) {
                    return parsed;
                }
                log.warn("[ledger] 全局费率配置非法（需 0<=rate<1）: {}，回退 yaml", configValue);
            }
        } catch (Exception e) {
            log.warn("[ledger] 读取费率配置失败，回退 yaml：{}", e.getMessage());
        }
        // 3. yaml 兜底
        return payProperties.getPlatformFeeRate();
    }


}
