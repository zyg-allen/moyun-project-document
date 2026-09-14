package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ledger.domain.entity.LedgerVipOrder;
import com.moyun.ledger.domain.entity.LedgerVipPackage;
import com.moyun.ledger.mapper.LedgerVipOrderMapper;
import com.moyun.ledger.mapper.LedgerVipPackageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CMS 记账VIP套餐管理 Controller（v11.81）
 *
 * <p>套餐价格/时长/上下架后台可配（App 端实时读取上架套餐，调价无需改代码）。
 * 删除规则：已有订单的套餐不可删除（仅可下架），保证历史订单快照可追溯。
 *
 * @author moyun
 */
@Tag(name = "CMS记账VIP套餐", description = "平台直收类订阅套餐维护（价格/时长/上下架）")
@RestController
@RequestMapping("/cms/ledger/vipPackage")
public class CmsLedgerVipPackageController extends BaseController {

    @Autowired
    private LedgerVipPackageMapper packageMapper;

    @Autowired
    private LedgerVipOrderMapper orderMapper;

    @Operation(summary = "套餐列表")
    @PreAuthorize("@ss.hasPermi('cms:ledgerVip:list')")
    @GetMapping("/list")
    public AjaxResult list(String name) {
        LambdaQueryWrapper<LedgerVipPackage> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            qw.like(LedgerVipPackage::getName, name.trim());
        }
        qw.orderByAsc(LedgerVipPackage::getSort).orderByAsc(LedgerVipPackage::getId);
        List<LedgerVipPackage> list = packageMapper.selectList(qw);
        return success(Map.of("records", list, "total", list.size()));
    }

    @Operation(summary = "新增套餐")
    @PreAuthorize("@ss.hasPermi('cms:ledgerVip:add')")
    @PostMapping
    public AjaxResult add(@RequestBody LedgerVipPackage pkg) {
        String err = validate(pkg);
        if (err != null) {
            return error(err);
        }
        pkg.setId(null);
        if (pkg.getStatus() == null) {
            pkg.setStatus(Boolean.TRUE);
        }
        if (pkg.getPopular() == null) {
            pkg.setPopular(Boolean.FALSE);
        }
        pkg.setCreateTime(LocalDateTime.now());
        packageMapper.insert(pkg);
        return success(pkg);
    }

    @Operation(summary = "修改套餐")
    @PreAuthorize("@ss.hasPermi('cms:ledgerVip:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody LedgerVipPackage pkg) {
        if (pkg.getId() == null) {
            return error("缺少套餐ID");
        }
        String err = validate(pkg);
        if (err != null) {
            return error(err);
        }
        pkg.setUpdateTime(LocalDateTime.now());
        packageMapper.updateById(pkg);
        return success();
    }

    @Operation(summary = "删除套餐（有订单仅可下架）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerVip:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<LedgerVipOrder>()
                .eq(LedgerVipOrder::getPackageId, id));
        if (orderCount != null && orderCount > 0) {
            return error("该套餐已有订阅订单，仅可下架（status=0）不可删除");
        }
        packageMapper.deleteById(id);
        return success();
    }

    /** 套餐字段校验（价格>0 且 scale≤2、时长>0） */
    private String validate(LedgerVipPackage pkg) {
        if (pkg.getName() == null || pkg.getName().isBlank()) {
            return "套餐名不能为空";
        }
        if (pkg.getPrice() == null || pkg.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "售价必须大于 0 元";
        }
        if (pkg.getPrice().stripTrailingZeros().scale() > 2) {
            return "售价最多两位小数";
        }
        if (pkg.getDurationDays() == null || pkg.getDurationDays() <= 0) {
            return "时长（天）必须大于 0";
        }
        return null;
    }
}
