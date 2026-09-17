package com.moyun.vip.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.util.bean.PageUtils;
import com.moyun.vip.domain.entity.SysPlatform;
import com.moyun.vip.mapper.SysPlatformMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 端定义管理 Controller（全局公共概念：用户/支付/VIP/配置/统计统一引用）
 *
 * @author moyun
 */
@Tag(name = "端管理", description = "全局端定义的增删改查")
@RestController
@RequestMapping("/system/platform")
public class SysPlatformController extends BaseController {

    @Autowired
    private SysPlatformMapper platformMapper;

    @Operation(summary = "端列表")
    @PreAuthorize("@ss.hasPermi('system:platform:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        List<SysPlatform> list = platformMapper.selectList(
                new LambdaQueryWrapper<SysPlatform>().orderByAsc(SysPlatform::getSortOrder));
        Page<SysPlatform> page = new Page<>();
        page.setRecords(list);
        page.setTotal(list.size());
        return success(page);
    }

    /** 下拉选项（全端，供 VIP 等级/权益筛选） */
    @Operation(summary = "端下拉选项")
    @GetMapping("/optionselect")
    public AjaxResult optionselect() {
        return AjaxResult.success(platformMapper.selectList(
                new LambdaQueryWrapper<SysPlatform>()
                        .eq(SysPlatform::getStatus, 1)
                        .orderByAsc(SysPlatform::getSortOrder)));
    }

    @Operation(summary = "端详情")
    @PreAuthorize("@ss.hasPermi('system:platform:list')")
    @GetMapping("/{id:[0-9]+}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(platformMapper.selectById(id));
    }

    @Operation(summary = "新增端")
    @PreAuthorize("@ss.hasPermi('system:platform:add')")
    @Log(title = "端管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysPlatform platform) {
        long exists = platformMapper.selectCount(new LambdaQueryWrapper<SysPlatform>()
                .eq(SysPlatform::getPlatformCode, platform.getPlatformCode()));
        if (exists > 0) {
            return error("端代码已存在：" + platform.getPlatformCode());
        }
        platform.setId(null);
        return toAjax(platformMapper.insert(platform));
    }

    @Operation(summary = "修改端")
    @PreAuthorize("@ss.hasPermi('system:platform:edit')")
    @Log(title = "端管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysPlatform platform) {
        if (platform.getId() == null) {
            return error("端ID不能为空");
        }
        return toAjax(platformMapper.updateById(platform));
    }

    @Operation(summary = "删除端（有关联等级/权益时禁止）")
    @PreAuthorize("@ss.hasPermi('system:platform:remove')")
    @Log(title = "端管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            SysPlatform platform = platformMapper.selectById(id);
            if (platform == null) {
                continue;
            }
            // 预置四端禁止删除（portal/ledger/admin/personality 为系统骨架）
            if ("portal".equals(platform.getPlatformCode()) || "ledger".equals(platform.getPlatformCode())
                    || "admin".equals(platform.getPlatformCode())) {
                return error("系统预置端不可删除：" + platform.getPlatformCode());
            }
        }
        return toAjax(platformMapper.deleteBatchIds(java.util.Arrays.asList(ids)));
    }
}
