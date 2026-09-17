package com.moyun.vip.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.util.bean.PageUtils;
import com.moyun.vip.domain.entity.VipApiRegistry;
import com.moyun.vip.domain.entity.VipBenefit;
import com.moyun.vip.domain.entity.VipBenefitUsage;
import com.moyun.vip.domain.entity.VipTier;
import com.moyun.vip.domain.entity.VipTierBenefit;
import com.moyun.vip.domain.entity.VipUserCard;
import com.moyun.vip.mapper.VipApiRegistryMapper;
import com.moyun.vip.mapper.VipBenefitMapper;
import com.moyun.vip.mapper.VipBenefitUsageMapper;
import com.moyun.vip.mapper.VipTierBenefitMapper;
import com.moyun.vip.mapper.VipTierMapper;
import com.moyun.vip.mapper.VipUserCardMapper;
import com.moyun.vip.scanner.VipApiScanner;
import com.moyun.vip.service.IVipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Map;

/**
 * VIP 管理 Controller（等级/权益/等级权益矩阵/接口注册/会员卡/使用统计）
 *
 * <p>路径约定：/system/vip/{resource}/**，与后台菜单六页一一对应。
 *
 * @author moyun
 */
@Tag(name = "VIP管理", description = "统一VIP体系后台管理")
@RestController
@RequestMapping("/system/vip")
public class VipAdminController extends BaseController {

    @Autowired
    private VipTierMapper tierMapper;

    @Autowired
    private VipBenefitMapper benefitMapper;

    @Autowired
    private VipTierBenefitMapper tierBenefitMapper;

    @Autowired
    private VipApiRegistryMapper registryMapper;

    @Autowired
    private VipUserCardMapper cardMapper;

    @Autowired
    private VipBenefitUsageMapper usageMapper;

    @Autowired
    private VipApiScanner apiScanner;

    @Autowired
    private IVipService vipService;

    // ==================== 等级管理 ====================

    @Operation(summary = "等级列表")
    @PreAuthorize("@ss.hasPermi('system:vip:tier:list')")
    @GetMapping("/tier/list")
    public AjaxResult tierList(@RequestParam(required = false) String platformCode) {
        LambdaQueryWrapper<VipTier> wrapper = new LambdaQueryWrapper<VipTier>()
                .orderByAsc(VipTier::getPlatformCode).orderByAsc(VipTier::getSortOrder);
        if (platformCode != null && !platformCode.isBlank()) {
            wrapper.eq(VipTier::getPlatformCode, platformCode);
        }
        return success(list(tierMapper, wrapper));
    }

    @Operation(summary = "新增等级")
    @PreAuthorize("@ss.hasPermi('system:vip:tier:add')")
    @Log(title = "VIP等级", businessType = BusinessType.INSERT)
    @PostMapping("/tier")
    public AjaxResult tierAdd(@RequestBody VipTier tier) {
        long exists = tierMapper.selectCount(new LambdaQueryWrapper<VipTier>()
                .eq(VipTier::getPlatformCode, tier.getPlatformCode())
                .eq(VipTier::getTierCode, tier.getTierCode()));
        if (exists > 0) {
            return error("同端下等级代码已存在：" + tier.getTierCode());
        }
        tier.setId(null);
        return toAjax(tierMapper.insert(tier));
    }

    @Operation(summary = "修改等级")
    @PreAuthorize("@ss.hasPermi('system:vip:tier:edit')")
    @Log(title = "VIP等级", businessType = BusinessType.UPDATE)
    @PutMapping("/tier")
    public AjaxResult tierEdit(@RequestBody VipTier tier) {
        if (tier.getId() == null) {
            return error("等级ID不能为空");
        }
        return toAjax(tierMapper.updateById(tier));
    }

    @Operation(summary = "删除等级（有会员卡/权益配置时禁止）")
    @PreAuthorize("@ss.hasPermi('system:vip:tier:remove')")
    @Log(title = "VIP等级", businessType = BusinessType.DELETE)
    @DeleteMapping("/tier/{ids}")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult tierRemove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            VipTier tier = tierMapper.selectById(id);
            if (tier == null) {
                continue;
            }
            if ("free".equals(tier.getTierCode())) {
                return error("免费等级（free）为系统骨架，不可删除");
            }
            long cards = cardMapper.selectCount(new LambdaQueryWrapper<VipUserCard>()
                    .eq(VipUserCard::getPlatformCode, tier.getPlatformCode())
                    .eq(VipUserCard::getTierCode, tier.getTierCode()));
            if (cards > 0) {
                return error("等级「" + tier.getTierName() + "」存在会员卡记录，不可删除");
            }
        }
        for (Long id : ids) {
            VipTier tier = tierMapper.selectById(id);
            if (tier != null) {
                tierBenefitMapper.delete(new LambdaQueryWrapper<VipTierBenefit>()
                        .eq(VipTierBenefit::getPlatformCode, tier.getPlatformCode())
                        .eq(VipTierBenefit::getTierCode, tier.getTierCode()));
            }
        }
        return toAjax(tierMapper.deleteBatchIds(java.util.Arrays.asList(ids)));
    }

    // ==================== 权益管理 ====================

    @Operation(summary = "权益列表")
    @PreAuthorize("@ss.hasPermi('system:vip:benefit:list')")
    @GetMapping("/benefit/list")
    public AjaxResult benefitList(@RequestParam(required = false) String platformCode) {
        LambdaQueryWrapper<VipBenefit> wrapper = new LambdaQueryWrapper<VipBenefit>()
                .orderByAsc(VipBenefit::getPlatformCode).orderByAsc(VipBenefit::getSortOrder);
        if (platformCode != null && !platformCode.isBlank()) {
            wrapper.eq(VipBenefit::getPlatformCode, platformCode);
        }
        return success(list(benefitMapper, wrapper));
    }

    @Operation(summary = "新增权益")
    @PreAuthorize("@ss.hasPermi('system:vip:benefit:add')")
    @Log(title = "VIP权益", businessType = BusinessType.INSERT)
    @PostMapping("/benefit")
    public AjaxResult benefitAdd(@RequestBody VipBenefit benefit) {
        long exists = benefitMapper.selectCount(new LambdaQueryWrapper<VipBenefit>()
                .eq(VipBenefit::getPlatformCode, benefit.getPlatformCode())
                .eq(VipBenefit::getBenefitCode, benefit.getBenefitCode()));
        if (exists > 0) {
            return error("同端下权益代码已存在：" + benefit.getBenefitCode());
        }
        benefit.setId(null);
        return toAjax(benefitMapper.insert(benefit));
    }

    @Operation(summary = "修改权益")
    @PreAuthorize("@ss.hasPermi('system:vip:benefit:edit')")
    @Log(title = "VIP权益", businessType = BusinessType.UPDATE)
    @PutMapping("/benefit")
    public AjaxResult benefitEdit(@RequestBody VipBenefit benefit) {
        if (benefit.getId() == null) {
            return error("权益ID不能为空");
        }
        return toAjax(benefitMapper.updateById(benefit));
    }

    @Operation(summary = "删除权益（有等级配置引用时禁止）")
    @PreAuthorize("@ss.hasPermi('system:vip:benefit:remove')")
    @Log(title = "VIP权益", businessType = BusinessType.DELETE)
    @DeleteMapping("/benefit/{ids}")
    public AjaxResult benefitRemove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            VipBenefit benefit = benefitMapper.selectById(id);
            if (benefit == null) {
                continue;
            }
            long refs = tierBenefitMapper.selectCount(new LambdaQueryWrapper<VipTierBenefit>()
                    .eq(VipTierBenefit::getPlatformCode, benefit.getPlatformCode())
                    .eq(VipTierBenefit::getBenefitCode, benefit.getBenefitCode()));
            if (refs > 0) {
                return error("权益「" + benefit.getBenefitName() + "」已被等级配置引用，请先解除");
            }
        }
        return toAjax(benefitMapper.deleteBatchIds(java.util.Arrays.asList(ids)));
    }

    // ==================== 等级权益矩阵 ====================

    @Operation(summary = "等级权益矩阵（按端查询：等级×权益）")
    @PreAuthorize("@ss.hasPermi('system:vip:tierBenefit:list')")
    @GetMapping("/tier-benefit/list")
    public AjaxResult tierBenefitList(@RequestParam String platformCode) {
        return success(Map.of(
                "tiers", tierMapper.selectList(new LambdaQueryWrapper<VipTier>()
                        .eq(VipTier::getPlatformCode, platformCode)
                        .orderByAsc(VipTier::getSortOrder)),
                "benefits", benefitMapper.selectList(new LambdaQueryWrapper<VipBenefit>()
                        .eq(VipBenefit::getPlatformCode, platformCode)
                        .orderByAsc(VipBenefit::getSortOrder)),
                "matrix", tierBenefitMapper.selectList(new LambdaQueryWrapper<VipTierBenefit>()
                        .eq(VipTierBenefit::getPlatformCode, platformCode))));
    }

    /**
     * 保存矩阵（整体覆盖式）：body = { platformCode, items: [{tierCode, benefitCode, benefitValue, period}] }
     * benefitValue 为空表示解除该权益。
     */
    @Operation(summary = "保存等级权益矩阵")
    @PreAuthorize("@ss.hasPermi('system:vip:tierBenefit:edit')")
    @Log(title = "等级权益配置", businessType = BusinessType.UPDATE)
    @PostMapping("/tier-benefit/save")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult tierBenefitSave(@RequestBody Map<String, Object> body) {
        String platformCode = String.valueOf(body.get("platformCode"));
        if (platformCode == null || "null".equals(platformCode) || platformCode.isBlank()) {
            return error("platformCode 不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        tierBenefitMapper.delete(new LambdaQueryWrapper<VipTierBenefit>()
                .eq(VipTierBenefit::getPlatformCode, platformCode));
        if (items != null) {
            for (Map<String, Object> item : items) {
                String value = item.get("benefitValue") == null ? null
                        : String.valueOf(item.get("benefitValue")).trim();
                if (value == null || value.isBlank()) {
                    continue;
                }
                VipTierBenefit tb = new VipTierBenefit();
                tb.setPlatformCode(platformCode);
                tb.setTierCode(String.valueOf(item.get("tierCode")));
                tb.setBenefitCode(String.valueOf(item.get("benefitCode")));
                tb.setBenefitValue(value);
                tb.setPeriod(item.get("period") == null ? "month" : String.valueOf(item.get("period")));
                tierBenefitMapper.insert(tb);
            }
        }
        // 额度变更后清开关缓存，避免旧周期计数口径残留
        vipService.clearEnabledCache();
        return success();
    }

    // ==================== 接口注册管理 ====================

    @Operation(summary = "接口注册列表")
    @PreAuthorize("@ss.hasPermi('system:vip:registry:list')")
    @GetMapping("/registry/list")
    public AjaxResult registryList(@RequestParam(required = false) String platformCode,
                                   @RequestParam(required = false) String benefitCode) {
        LambdaQueryWrapper<VipApiRegistry> wrapper = new LambdaQueryWrapper<VipApiRegistry>()
                .orderByDesc(VipApiRegistry::getScanTime);
        if (platformCode != null && !platformCode.isBlank()) {
            wrapper.eq(VipApiRegistry::getPlatformCode, platformCode);
        }
        if (benefitCode != null && !benefitCode.isBlank()) {
            wrapper.eq(VipApiRegistry::getBenefitCode, benefitCode);
        }
        return success(list(registryMapper, wrapper));
    }

    /** 仅允许编辑运营字段（描述/启停）；代码侧字段由扫描维护 */
    @Operation(summary = "编辑接口（描述/启停校验）")
    @PreAuthorize("@ss.hasPermi('system:vip:registry:edit')")
    @Log(title = "VIP接口注册", businessType = BusinessType.UPDATE)
    @PutMapping("/registry")
    public AjaxResult registryEdit(@RequestBody VipApiRegistry registry) {
        if (registry.getId() == null) {
            return error("注册ID不能为空");
        }
        VipApiRegistry update = new VipApiRegistry();
        update.setId(registry.getId());
        update.setApiDesc(registry.getApiDesc());
        update.setEnabled(registry.getEnabled());
        return toAjax(registryMapper.updateById(update));
    }

    @Operation(summary = "手动重新扫描（热更新入口）")
    @PreAuthorize("@ss.hasPermi('system:vip:registry:scan')")
    @Log(title = "VIP接口扫描", businessType = BusinessType.UPDATE)
    @PostMapping("/registry/scan")
    public AjaxResult registryScan() {
        return success(Map.of("count", apiScanner.scan()));
    }

    // ==================== 会员卡 ====================

    @Operation(summary = "会员卡列表")
    @PreAuthorize("@ss.hasPermi('system:vip:card:list')")
    @GetMapping("/card/list")
    public AjaxResult cardList(@RequestParam(required = false) String platformCode,
                               @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<VipUserCard> wrapper = new LambdaQueryWrapper<VipUserCard>()
                .orderByDesc(VipUserCard::getId);
        if (platformCode != null && !platformCode.isBlank()) {
            wrapper.eq(VipUserCard::getPlatformCode, platformCode);
        }
        if (userId != null) {
            wrapper.eq(VipUserCard::getUserId, userId);
        }
        return success(list(cardMapper, wrapper));
    }

    /** 作废会员卡（status=0，立即失效） */
    @Operation(summary = "作废会员卡")
    @PreAuthorize("@ss.hasPermi('system:vip:card:remove')")
    @Log(title = "VIP会员卡", businessType = BusinessType.DELETE)
    @DeleteMapping("/card/{ids}")
    public AjaxResult cardRemove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            VipUserCard card = new VipUserCard();
            card.setId(id);
            card.setStatus(0);
            cardMapper.updateById(card);
        }
        return success();
    }

    // ==================== 使用统计 ====================

    @Operation(summary = "权益使用统计")
    @PreAuthorize("@ss.hasPermi('system:vip:usage:list')")
    @GetMapping("/usage/list")
    public AjaxResult usageList(@RequestParam(required = false) String platformCode,
                                @RequestParam(required = false) String benefitCode,
                                @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<VipBenefitUsage> wrapper = new LambdaQueryWrapper<VipBenefitUsage>()
                .orderByDesc(VipBenefitUsage::getUsageDate).orderByDesc(VipBenefitUsage::getId);
        if (platformCode != null && !platformCode.isBlank()) {
            wrapper.eq(VipBenefitUsage::getPlatformCode, platformCode);
        }
        if (benefitCode != null && !benefitCode.isBlank()) {
            wrapper.eq(VipBenefitUsage::getBenefitCode, benefitCode);
        }
        if (userId != null) {
            wrapper.eq(VipBenefitUsage::getUserId, userId);
        }
        return success(list(usageMapper, wrapper));
    }

    /** 通用分页查询（后台小数据量，统一 page 返回 records/total） */
    private <T> Page<T> list(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                             LambdaQueryWrapper<T> wrapper) {
        Page<T> page = PageUtils.startPage();
        mapper.selectPage(page, wrapper);
        return page;
    }
}
