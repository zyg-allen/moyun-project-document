package com.moyun.ext.cms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.portal.domain.entity.PortalInterviewVipOrder;
import com.moyun.portal.domain.entity.PortalInterviewVipPackage;
import com.moyun.portal.mapper.PortalInterviewVipOrderMapper;
import com.moyun.portal.mapper.PortalInterviewVipPackageMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CMS 面试会员套餐管理 Controller
 *
 * <p>套餐价格/时长/上下架后台可配（门户面试频道实时读取上架套餐，调价无需改代码）。
 * 删除规则：已有订单的套餐不可删除（仅可下架），保证历史订单快照可追溯。
 *
 * @author moyun
 */
@Tag(name = "CMS面试会员套餐", description = "平台直收类订阅套餐维护（价格/时长/上下架）")
@RestController
@RequestMapping("/cms/interview/vipPackage")
public class CmsInterviewVipPackageController extends BaseController {

    @Autowired
    private PortalInterviewVipPackageMapper packageMapper;

    @Autowired
    private PortalInterviewVipOrderMapper orderMapper;

    @Operation(summary = "套餐列表")
    @PreAuthorize("@ss.hasPermi('cms:interview:vip:list')")
    @GetMapping("/list")
    public AjaxResult list(String name) {
        LambdaQueryWrapper<PortalInterviewVipPackage> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            qw.like(PortalInterviewVipPackage::getName, name.trim());
        }
        qw.orderByAsc(PortalInterviewVipPackage::getSort).orderByAsc(PortalInterviewVipPackage::getId);
        List<PortalInterviewVipPackage> list = packageMapper.selectList(qw);
        return success(Map.of("records", list, "total", list.size()));
    }

    @Operation(summary = "新增套餐")
    @PreAuthorize("@ss.hasPermi('cms:interview:vip:add')")
    @PostMapping
    public AjaxResult add(@RequestBody PortalInterviewVipPackage pkg) {
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
    @PreAuthorize("@ss.hasPermi('cms:interview:vip:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody PortalInterviewVipPackage pkg) {
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
    @PreAuthorize("@ss.hasPermi('cms:interview:vip:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<PortalInterviewVipOrder>()
                .eq(PortalInterviewVipOrder::getPackageId, id));
        if (orderCount != null && orderCount > 0) {
            return error("该套餐已有订阅订单，仅可下架（status=0）不可删除");
        }
        packageMapper.deleteById(id);
        return success();
    }

    /** 套餐字段校验（价格>0 且 scale≤2、时长>0） */
    private String validate(PortalInterviewVipPackage pkg) {
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
