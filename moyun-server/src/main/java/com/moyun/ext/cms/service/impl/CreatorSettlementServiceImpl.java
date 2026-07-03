package com.moyun.ext.cms.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.page.PageDomain;
import com.moyun.ext.cms.service.ICreatorSettlementService;
import com.moyun.portal.domain.entity.PortalCreatorSettlement;
import com.moyun.portal.mapper.PortalCreatorSettlementMapper;
import com.moyun.system.service.ISysConfigService;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.string.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * 创作者分成结算 Service 实现（任务 4.7）
 *
 * <p>收入聚合来源：portal_tip_order（status=paid，paid_time 在周期内）。
 * 平台抽成比例读取 sys_config.platform_fee_rate（默认 0.1）。</p>
 *
 * @author moyun
 */
@Service
public class CreatorSettlementServiceImpl implements ICreatorSettlementService {

    /** 平台抽成比例配置键 */
    private static final String CONFIG_PLATFORM_FEE_RATE = "platform_fee_rate";

    /** 默认平台抽成比例（配置缺失时兜底） */
    private static final BigDecimal DEFAULT_FEE_RATE = new BigDecimal("0.1");

    @Autowired private PortalCreatorSettlementMapper settlementMapper;
    @Autowired private ISysConfigService sysConfigService;

    // ========================================================================
    // 月度生成
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateMonthlySettlement(String period) {
        // 1. 解析周期：为空时默认上个月
        YearMonth ym = parsePeriod(period);
        String periodKey = ym.toString(); // yyyy-MM
        LocalDateTime startTime = ym.atDay(1).atStartOfDay();
        LocalDateTime endTime = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 2. 聚合该周期内有收入的创作者
        List<Long> creatorIds = settlementMapper.selectCreatorsWithIncome(startTime, endTime);
        if (creatorIds == null || creatorIds.isEmpty()) {
            return 0;
        }

        BigDecimal feeRate = readFeeRate();
        int generated = 0;
        for (Long creatorId : creatorIds) {
            // 幂等：已存在则跳过（避免重复执行重复生成）
            if (settlementMapper.countByCreatorAndPeriod(creatorId, periodKey) > 0) {
                continue;
            }

            BigDecimal tipIncome = nz(settlementMapper.sumTipIncome(creatorId, startTime, endTime));
            BigDecimal paidReadIncome = nz(settlementMapper.sumPaidReadIncome(creatorId, startTime, endTime));
            BigDecimal columnIncome = nz(settlementMapper.sumColumnIncome(creatorId, startTime, endTime));
            BigDecimal totalIncome = tipIncome.add(paidReadIncome).add(columnIncome);

            // 三项均为 0 跳过（理论上 selectCreatorsWithIncome 已过滤，这里兜底）
            if (totalIncome.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal platformFee = totalIncome.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal creatorIncome = totalIncome.subtract(platformFee).setScale(2, RoundingMode.HALF_UP);

            PortalCreatorSettlement entity = new PortalCreatorSettlement();
            entity.setCreatorId(creatorId);
            entity.setPeriod(periodKey);
            entity.setTipIncome(tipIncome);
            entity.setPaidReadIncome(paidReadIncome);
            entity.setColumnIncome(columnIncome);
            entity.setTotalIncome(totalIncome);
            entity.setPlatformFee(platformFee);
            entity.setCreatorIncome(creatorIncome);
            entity.setStatus("pending");
            entity.setCreateTime(LocalDateTime.now());

            settlementMapper.insert(entity);
            generated++;
        }
        return generated;
    }

    // ========================================================================
    // 我的结算单
    // ========================================================================
    @Override
    public Page<PortalCreatorSettlement> mySettlements(Long creatorId, PageDomain query) {
        if (creatorId == null) {
            throw new ServiceException("请登录后操作");
        }
        Page<PortalCreatorSettlement> page = PageUtils.buildPage(query);
        return settlementMapper.selectMySettlementsPage(page, creatorId);
    }

    // ========================================================================
    // 后台列表
    // ========================================================================
    @Override
    public Page<PortalCreatorSettlement> list(PortalCreatorSettlement query, PageDomain pageDomain) {
        Page<PortalCreatorSettlement> page = PageUtils.buildPage(pageDomain);
        if (query == null) {
            query = new PortalCreatorSettlement();
        }
        return settlementMapper.selectAdminPage(page, query);
    }

    // ========================================================================
    // 详情
    // ========================================================================
    @Override
    public PortalCreatorSettlement detail(Long id) {
        if (id == null) {
            throw new ServiceException("结算单ID不能为空");
        }
        PortalCreatorSettlement vo = settlementMapper.selectDetailById(id);
        if (vo == null) {
            throw new ServiceException("结算单不存在");
        }
        return vo;
    }

    // ========================================================================
    // 确认（pending -> confirmed）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirm(Long id) {
        PortalCreatorSettlement entity = mustExist(id);
        if (!"pending".equals(entity.getStatus())) {
            throw new ServiceException("仅待确认状态的结算单可确认");
        }
        entity.setStatus("confirmed");
        return settlementMapper.updateById(entity);
    }

    // ========================================================================
    // 标记已打款（confirmed -> paid）
    // ========================================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markPaid(Long id) {
        PortalCreatorSettlement entity = mustExist(id);
        if (!"confirmed".equals(entity.getStatus())) {
            throw new ServiceException("仅已确认状态的结算单可标记打款");
        }
        entity.setStatus("paid");
        entity.setPaidTime(LocalDateTime.now());
        return settlementMapper.updateById(entity);
    }

    // ========================================================================
    // 内部工具
    // ========================================================================

    private PortalCreatorSettlement mustExist(Long id) {
        PortalCreatorSettlement entity = settlementMapper.selectById(id);
        if (entity == null) {
            throw new ServiceException("结算单不存在");
        }
        return entity;
    }

    /**
     * 解析周期字符串（yyyy-MM），为空时返回上个月
     */
    private YearMonth parsePeriod(String period) {
        if (StringUtils.isNotEmpty(period)) {
            try {
                return YearMonth.parse(period);
            } catch (Exception e) {
                throw new ServiceException("周期格式错误，应为 yyyy-MM，如 2026-07");
            }
        }
        return YearMonth.now().minusMonths(1);
    }

    /**
     * 读取平台抽成比例配置（缺失或非法时使用默认 0.1）
     */
    private BigDecimal readFeeRate() {
        try {
            String value = sysConfigService.selectConfigByKey(CONFIG_PLATFORM_FEE_RATE);
            if (StringUtils.isNotEmpty(value)) {
                BigDecimal rate = new BigDecimal(value.trim());
                if (rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(BigDecimal.ONE) <= 0) {
                    return rate;
                }
            }
        } catch (Exception ignored) {
            // 配置读取异常时兜底
        }
        return DEFAULT_FEE_RATE;
    }

    /**
     * null 转 0
     */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
