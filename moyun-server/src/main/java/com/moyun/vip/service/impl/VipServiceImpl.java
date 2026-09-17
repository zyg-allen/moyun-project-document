package com.moyun.vip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.moyun.core.config.redis.RedisCache;
import com.moyun.system.domain.entity.SysConfig;
import com.moyun.system.mapper.SysConfigMapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.vip.domain.entity.VipBenefit;
import com.moyun.vip.domain.entity.VipTier;
import com.moyun.vip.domain.entity.VipTierBenefit;
import com.moyun.vip.domain.entity.VipUserCard;
import com.moyun.vip.mapper.VipBenefitMapper;
import com.moyun.vip.mapper.VipBenefitUsageMapper;
import com.moyun.vip.mapper.VipTierBenefitMapper;
import com.moyun.vip.mapper.VipTierMapper;
import com.moyun.vip.mapper.VipUserCardMapper;
import com.moyun.vip.service.IVipService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 统一 VIP 服务实现
 *
 * <p>判定链：会员卡定级（无卡/过期=free）→ 等级权益额度（unlimited 或数字）
 * → 周期已用次数（Redis 原子计数优先，DB 兜底）。
 *
 * <p>计数周期：day/month/year/unlimited；Redis key 带周期段自动滚动，
 * 消耗走「先 incr 超限回滚」原子模式，并发安全；异步落库仅供统计口径。
 *
 * @author moyun
 */
@Service
public class VipServiceImpl implements IVipService {

    private static final Logger log = LoggerFactory.getLogger(VipServiceImpl.class);

    /** VIP 总开关配置键（端级 platform_code 优先，NULL 全局兜底） */
    public static final String CFG_KEY_ENABLED = "vip.enabled";

    private static final String ENABLED_CACHE_PREFIX = "vip:cfg:enabled:";
    private static final String USAGE_KEY_PREFIX = "vip:usage:";
    private static final String UNLIMITED = "unlimited";

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private VipTierMapper tierMapper;

    @Autowired
    private VipBenefitMapper benefitMapper;

    @Autowired
    private VipTierBenefitMapper tierBenefitMapper;

    @Autowired
    private VipUserCardMapper cardMapper;

    @Autowired
    private VipBenefitUsageMapper usageMapper;

    @Autowired
    private RedisCache redisCache;

    // ==================== 开关 ====================

    @Override
    public boolean isVipEnabled(String platformCode) {
        String cacheKey = ENABLED_CACHE_PREFIX + platformCode;
        try {
            String cached = redisCache.getCacheObject(cacheKey);
            if (cached != null) {
                return Boolean.parseBoolean(cached);
            }
        } catch (Exception e) {
            log.warn("[vip] 开关缓存读取失败，回源 DB", e);
        }
        String value = queryEnabledConfig(platformCode);
        try {
            redisCache.setCacheObject(cacheKey, value, 30, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public void clearEnabledCache() {
        try {
            redisCache.deleteObject(redisCache.keys(ENABLED_CACHE_PREFIX + "*"));
        } catch (Exception ignored) {
        }
    }

    /** 端级配置优先，全局（platform_code IS NULL）兜底，缺省 false（灰度安全侧） */
    private String queryEnabledConfig(String platformCode) {
        SysConfig platformCfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, CFG_KEY_ENABLED)
                .eq(SysConfig::getPlatformCode, platformCode)
                .eq(SysConfig::getDelFlag, "0")
                .last("LIMIT 1"));
        if (platformCfg != null && platformCfg.getConfigValue() != null) {
            return platformCfg.getConfigValue();
        }
        SysConfig globalCfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, CFG_KEY_ENABLED)
                .isNull(SysConfig::getPlatformCode)
                .eq(SysConfig::getDelFlag, "0")
                .last("LIMIT 1"));
        return globalCfg != null && globalCfg.getConfigValue() != null
                ? globalCfg.getConfigValue() : "false";
    }

    // ==================== 会员卡 ====================

    @Override
    public boolean isVip(Long userId, String platformCode) {
        return selectActiveCard(userId, platformCode) != null;
    }

    @Override
    public String currentTierCode(Long userId, String platformCode) {
        VipUserCard card = selectActiveCard(userId, platformCode);
        return card != null ? card.getTierCode() : "free";
    }

    /** 有效会员卡：status=1 且（永久 expire 为空 或 未过期） */
    private VipUserCard selectActiveCard(Long userId, String platformCode) {
        if (userId == null) {
            return null;
        }
        return cardMapper.selectOne(new LambdaQueryWrapper<VipUserCard>()
                .eq(VipUserCard::getUserId, userId)
                .eq(VipUserCard::getPlatformCode, platformCode)
                .eq(VipUserCard::getStatus, 1)
                .and(w -> w.isNull(VipUserCard::getExpireTime)
                        .or().gt(VipUserCard::getExpireTime, LocalDateTime.now()))
                .orderByDesc(VipUserCard::getId)
                .last("LIMIT 1"));
    }

    // ==================== 权益校验与消耗 ====================

    @Override
    public boolean hasBenefit(Long userId, String platformCode, String benefitCode) {
        VipTierBenefit tb = resolveTierBenefit(userId, platformCode, benefitCode);
        if (tb == null) {
            return false;
        }
        if (UNLIMITED.equalsIgnoreCase(tb.getBenefitValue())) {
            return true;
        }
        int limit = parseLimit(tb.getBenefitValue());
        if (limit <= 0) {
            return "0".equals(tb.getBenefitValue().trim());
        }
        int used = getUsedCount(userId, platformCode, benefitCode, tb.getPeriod());
        return used < limit;
    }

    @Override
    public boolean consumeBenefit(Long userId, String platformCode, String benefitCode) {
        VipTierBenefit tb = resolveTierBenefit(userId, platformCode, benefitCode);
        if (tb == null) {
            return false;
        }
        if (UNLIMITED.equalsIgnoreCase(tb.getBenefitValue())) {
            recordUsage(userId, platformCode, benefitCode);
            return true;
        }
        int limit = parseLimit(tb.getBenefitValue());
        if (limit <= 0) {
            return "0".equals(tb.getBenefitValue().trim());
        }
        String period = tb.getPeriod();
        String redisKey = usageKey(userId, platformCode, benefitCode, period);
        try {
            // 原子模式：先 incr，超限回滚（并发安全）
            Long count = redisCache.redisTemplate.opsForValue().increment(redisKey);
            if (count == null) {
                return dbConsume(userId, platformCode, benefitCode, limit);
            }
            if (count > limit) {
                redisCache.redisTemplate.opsForValue().decrement(redisKey);
                return false;
            }
            touchUsageTtl(redisKey, period);
            asyncPersistUsage(userId, platformCode, benefitCode);
            return true;
        } catch (Exception e) {
            log.warn("[vip] Redis 计数不可用，降级 DB 消耗 benefit={}", benefitCode, e);
            return dbConsume(userId, platformCode, benefitCode, limit);
        }
    }

    /** DB 兜底消耗（Redis 不可用时的降级路径） */
    private boolean dbConsume(Long userId, String platformCode, String benefitCode, int limit) {
        VipTierBenefit tb = resolveTierBenefit(userId, platformCode, benefitCode);
        if (tb == null) {
            return false;
        }
        int used = dbUsedCount(userId, platformCode, benefitCode, tb.getPeriod());
        if (used >= limit) {
            return false;
        }
        usageMapper.upsertUsage(userId, platformCode, benefitCode, LocalDate.now());
        return true;
    }

    /** 当前用户等级下的权益配置（无卡=free tier） */
    private VipTierBenefit resolveTierBenefit(Long userId, String platformCode, String benefitCode) {
        String tierCode = currentTierCode(userId, platformCode);
        return tierBenefitMapper.selectOne(new LambdaQueryWrapper<VipTierBenefit>()
                .eq(VipTierBenefit::getPlatformCode, platformCode)
                .eq(VipTierBenefit::getTierCode, tierCode)
                .eq(VipTierBenefit::getBenefitCode, benefitCode)
                .last("LIMIT 1"));
    }

    /** 周期内已用次数：Redis 优先，DB 兜底 */
    private int getUsedCount(Long userId, String platformCode, String benefitCode, String period) {
        try {
            Object v = redisCache.getCacheObject(usageKey(userId, platformCode, benefitCode, period));
            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
            return 0;
        } catch (Exception e) {
            return dbUsedCount(userId, platformCode, benefitCode, period);
        }
    }

    /** DB 统计周期内使用次数（异步落库数据，统计口径） */
    private int dbUsedCount(Long userId, String platformCode, String benefitCode, String period) {
        QueryWrapper<com.moyun.vip.domain.entity.VipBenefitUsage> wrapper =
                new QueryWrapper<com.moyun.vip.domain.entity.VipBenefitUsage>()
                        .select("COALESCE(SUM(usage_count), 0) AS used")
                        .eq("user_id", userId)
                        .eq("platform_code", platformCode)
                        .eq("benefit_code", benefitCode);
        LocalDate start = periodStart(period);
        if (start != null) {
            wrapper.ge("usage_date", start);
        }
        List<Map<String, Object>> rows = usageMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty() || rows.getFirst() == null) {
            return 0;
        }
        Object used = rows.getFirst().get("used");
        return used == null ? 0 : Integer.parseInt(String.valueOf(used));
    }

    /** 周期起始日（unlimited=终身→null；day/month/year） */
    private LocalDate periodStart(String period) {
        LocalDate today = LocalDate.now();
        if ("day".equalsIgnoreCase(period)) {
            return today;
        }
        if ("month".equalsIgnoreCase(period)) {
            return today.withDayOfMonth(1);
        }
        if ("year".equalsIgnoreCase(period)) {
            return today.withDayOfYear(1);
        }
        return null;
    }

    /** 周期段（Redis key 后缀，自动滚动重置额度） */
    private String periodSegment(String period) {
        LocalDate today = LocalDate.now();
        if ("day".equalsIgnoreCase(period)) {
            return today.format(DateTimeFormatter.BASIC_ISO_DATE);
        }
        if ("year".equalsIgnoreCase(period)) {
            return String.valueOf(today.getYear());
        }
        if (UNLIMITED.equalsIgnoreCase(period)) {
            return "all";
        }
        return today.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private String usageKey(Long userId, String platformCode, String benefitCode, String period) {
        return USAGE_KEY_PREFIX + userId + ":" + platformCode + ":" + benefitCode + ":"
                + periodSegment(period);
    }

    /** 周期 key 设置过期（unlimited 不设，其余按周期长度留余量清理） */
    private void touchUsageTtl(String key, String period) {
        try {
            if ("day".equalsIgnoreCase(period)) {
                redisCache.expire(key, 2, TimeUnit.DAYS);
            } else if ("year".equalsIgnoreCase(period)) {
                redisCache.expire(key, 400, TimeUnit.DAYS);
            } else if (!UNLIMITED.equalsIgnoreCase(period)) {
                redisCache.expire(key, 45, TimeUnit.DAYS);
            }
        } catch (Exception ignored) {
        }
    }

    /** 不限次权益也记录使用（统计口径），失败不影响放行 */
    private void recordUsage(Long userId, String platformCode, String benefitCode) {
        asyncPersistUsage(userId, platformCode, benefitCode);
    }

    /** 异步落库（Redis 是周期内计数事实源，DB 仅统计口径，可容忍短暂延迟） */
    private void asyncPersistUsage(Long userId, String platformCode, String benefitCode) {
        CompletableFuture.runAsync(() -> {
            try {
                usageMapper.upsertUsage(userId, platformCode, benefitCode, LocalDate.now());
            } catch (Exception e) {
                log.warn("[vip] 使用记录落库失败（不影响权益放行）benefit={}", benefitCode, e);
            }
        });
    }

    private int parseLimit(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ==================== 售卖/详情视图 ====================

    @Override
    public List<Map<String, Object>> listTiers(String platformCode) {
        List<VipTier> tiers = tierMapper.selectList(new LambdaQueryWrapper<VipTier>()
                .eq(VipTier::getPlatformCode, platformCode)
                .eq(VipTier::getStatus, 1)
                .orderByAsc(VipTier::getSortOrder));
        List<VipTierBenefit> allTb = tierBenefitMapper.selectList(new LambdaQueryWrapper<VipTierBenefit>()
                .eq(VipTierBenefit::getPlatformCode, platformCode));
        List<VipBenefit> allBenefits = benefitMapper.selectList(new LambdaQueryWrapper<VipBenefit>()
                .eq(VipBenefit::getPlatformCode, platformCode)
                .orderByAsc(VipBenefit::getSortOrder));
        Map<String, VipBenefit> benefitMap = new HashMap<>();
        for (VipBenefit b : allBenefits) {
            benefitMap.put(b.getBenefitCode(), b);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (VipTier tier : tiers) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tierCode", tier.getTierCode());
            item.put("tierName", tier.getTierName());
            item.put("price", tier.getPrice());
            item.put("originalPrice", tier.getOriginalPrice());
            item.put("popular", tier.getPopular());
            item.put("durationDays", tier.getDurationDays());
            item.put("description", tier.getDescription());
            List<Map<String, Object>> benefits = new ArrayList<>();
            for (VipTierBenefit tb : allTb) {
                if (!tb.getTierCode().equals(tier.getTierCode())) {
                    continue;
                }
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("code", tb.getBenefitCode());
                VipBenefit meta = benefitMap.get(tb.getBenefitCode());
                b.put("name", meta != null ? meta.getBenefitName() : tb.getBenefitCode());
                b.put("value", tb.getBenefitValue());
                b.put("period", tb.getPeriod());
                benefits.add(b);
            }
            item.put("benefits", benefits);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> getVipDetail(Long userId, String platformCode) {
        Map<String, Object> detail = new LinkedHashMap<>();
        VipUserCard card = selectActiveCard(userId, platformCode);
        String tierCode = card != null ? card.getTierCode() : "free";
        VipTier tier = tierMapper.selectOne(new LambdaQueryWrapper<VipTier>()
                .eq(VipTier::getPlatformCode, platformCode)
                .eq(VipTier::getTierCode, tierCode)
                .last("LIMIT 1"));
        detail.put("isVip", card != null && !"free".equals(tierCode));
        detail.put("tierCode", tierCode);
        detail.put("tierName", tier != null ? tier.getTierName() : tierCode);
        detail.put("expireTime", card != null ? card.getExpireTime() : null);

        List<VipTierBenefit> tbs = tierBenefitMapper.selectList(new LambdaQueryWrapper<VipTierBenefit>()
                .eq(VipTierBenefit::getPlatformCode, platformCode)
                .eq(VipTierBenefit::getTierCode, tierCode));
        List<VipBenefit> allBenefits = benefitMapper.selectList(new LambdaQueryWrapper<VipBenefit>()
                .eq(VipBenefit::getPlatformCode, platformCode)
                .orderByAsc(VipBenefit::getSortOrder));
        Map<String, VipBenefit> benefitMap = new HashMap<>();
        for (VipBenefit b : allBenefits) {
            benefitMap.put(b.getBenefitCode(), b);
        }

        List<Map<String, Object>> benefits = new ArrayList<>();
        for (VipTierBenefit tb : tbs) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("code", tb.getBenefitCode());
            VipBenefit meta = benefitMap.get(tb.getBenefitCode());
            b.put("name", meta != null ? meta.getBenefitName() : tb.getBenefitCode());
            b.put("value", tb.getBenefitValue());
            b.put("period", tb.getPeriod());
            if (UNLIMITED.equalsIgnoreCase(tb.getBenefitValue())) {
                b.put("used", 0);
                b.put("left", null);
            } else {
                int limit = Math.max(parseLimit(tb.getBenefitValue()), 0);
                int used = getUsedCount(userId, platformCode, tb.getBenefitCode(), tb.getPeriod());
                b.put("used", Math.min(used, limit));
                b.put("left", Math.max(limit - used, 0));
            }
            benefits.add(b);
        }
        detail.put("benefits", benefits);
        return detail;
    }

    // ==================== 支付发卡 ====================

    @Override
    public void grantCard(Long userId, String platformCode, String tierCode, Long orderId) {
        VipTier tier = tierMapper.selectOne(new LambdaQueryWrapper<VipTier>()
                .eq(VipTier::getPlatformCode, platformCode)
                .eq(VipTier::getTierCode, tierCode)
                .last("LIMIT 1"));
        if (tier == null) {
            throw new ServiceException("VIP等级不存在：" + platformCode + "/" + tierCode);
        }
        LocalDateTime now = LocalDateTime.now();
        VipUserCard card = selectActiveCard(userId, platformCode);
        // 续费顺延：从 max(now, 现有到期) 起 + duration（升级覆盖 tier；永久置空到期）
        if (card != null) {
            LocalDateTime start = card.getExpireTime() != null && card.getExpireTime().isAfter(now)
                    ? card.getExpireTime() : now;
            card.setTierCode(tierCode);
            card.setOrderId(orderId);
            card.setStartTime(start);
            card.setExpireTime(tier.getDurationDays() != null && tier.getDurationDays() == -1
                    ? null : start.plusDays(tier.getDurationDays()));
            card.setStatus(1);
            cardMapper.updateById(card);
            return;
        }
        VipUserCard newCard = new VipUserCard();
        newCard.setUserId(userId);
        newCard.setPlatformCode(platformCode);
        newCard.setTierCode(tierCode);
        newCard.setOrderId(orderId);
        newCard.setStartTime(now);
        newCard.setExpireTime(tier.getDurationDays() != null && tier.getDurationDays() == -1
                ? null : now.plusDays(tier.getDurationDays()));
        newCard.setStatus(1);
        cardMapper.insert(newCard);
    }
}
