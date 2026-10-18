package com.moyun.ledger.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.base.AjaxResult;
import com.moyun.ledger.domain.entity.LedgerWarehouseConfig;
import com.moyun.ledger.mapper.LedgerWarehouseConfigMapper;
import com.moyun.portal.util.PortalSecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 仓库常用配置：存 ledger_warehouse_config（key-value）。
 *
 * <p>warehouse_id 预留按仓配置，当前前端统一写全局（0）。</p>
 */
@Tag(name = "门户-仓库配置")
@RestController
@RequestMapping("/portal/ledger/warehouse-config")
public class PortalLedgerWarehouseConfigController {

    @Autowired
    private LedgerWarehouseConfigMapper configMapper;

    @Operation(summary = "读取仓库配置（key-value 对象）")
    @GetMapping
    public AjaxResult get() {
        Long userId = PortalSecurityUtils.getUserId();
        Map<String, Object> out = new HashMap<>();
        for (LedgerWarehouseConfig c : configMapper.selectList(new LambdaQueryWrapper<LedgerWarehouseConfig>()
                .eq(LedgerWarehouseConfig::getUserId, userId)
                .eq(LedgerWarehouseConfig::getWarehouseId, LedgerWarehouseConfig.GLOBAL_WAREHOUSE))) {
            out.put(c.getCfgKey(), c.getCfgValue());
        }
        return AjaxResult.success(out);
    }

    @Operation(summary = "保存仓库配置（部分更新）")
    @PutMapping
    public AjaxResult save(@RequestBody Map<String, Object> body) {
        Long userId = PortalSecurityUtils.getUserId();
        Object raw = body.get("cfg");
        if (!(raw instanceof Map)) {
            throw new ServiceException("缺少 cfg");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> cfg = (Map<String, Object>) raw;
        for (Map.Entry<String, Object> e : cfg.entrySet()) {
            String key = e.getKey() == null ? "" : e.getKey().trim();
            if (key.isEmpty() || key.length() > 50) {
                continue;
            }
            String val = e.getValue() == null ? null : String.valueOf(e.getValue());
            if (val != null && val.length() > 500) {
                val = val.substring(0, 500);
            }
            Long exists = configMapper.selectCount(new LambdaQueryWrapper<LedgerWarehouseConfig>()
                    .eq(LedgerWarehouseConfig::getUserId, userId)
                    .eq(LedgerWarehouseConfig::getWarehouseId, LedgerWarehouseConfig.GLOBAL_WAREHOUSE)
                    .eq(LedgerWarehouseConfig::getCfgKey, key));
            if (exists != null && exists > 0) {
                configMapper.update(null, new LambdaUpdateWrapper<LedgerWarehouseConfig>()
                        .eq(LedgerWarehouseConfig::getUserId, userId)
                        .eq(LedgerWarehouseConfig::getWarehouseId, LedgerWarehouseConfig.GLOBAL_WAREHOUSE)
                        .eq(LedgerWarehouseConfig::getCfgKey, key)
                        .set(LedgerWarehouseConfig::getCfgValue, val));
            } else {
                LedgerWarehouseConfig c = new LedgerWarehouseConfig();
                c.setUserId(userId);
                c.setWarehouseId(LedgerWarehouseConfig.GLOBAL_WAREHOUSE);
                c.setCfgKey(key);
                c.setCfgValue(val);
                configMapper.insert(c);
            }
        }
        return AjaxResult.success();
    }
}