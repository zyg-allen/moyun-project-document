package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.system.service.ISysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户-记账字典（登录后可用）
 *
 * <p>记账模块的下拉候选（交易类型/仓库类型/货架类型/出入库原因/来源/包装/单位/保质期/成色/
 * 存钱方式/定时周期/提醒规则）统一由 {@code sys_dict_data} 提供，后台字典管理维护。</p>
 *
 * <p><b>为什么不复用 {@code /portal/dict/**}</b>：那个接口只放行 {@code portal_}/{@code cms_} 前缀，
 * 记账字典属登录后业务域，单独走本接口，避免把 ledger_* 字典暴露给未登录端。</p>
 *
 * @author moyun
 */
@Tag(name = "门户-记账字典", description = "记账下拉候选（字典驱动，登录后可用）")
@RestController
@RequestMapping("/portal/ledger/dict")
public class PortalLedgerDictController extends BaseController {

    /** 记账字典前缀：只允许 ledger_ 开头的类型 */
    private static final String PREFIX = "ledger_";

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Operation(summary = "单个字典的启用项")
    @GetMapping("/{dictType}")
    public AjaxResult one(@PathVariable String dictType) {
        if (!isLedgerType(dictType)) {
            return success(List.of());
        }
        return success(items(dictType));
    }

    @Operation(summary = "批量字典（types 逗号分隔，一次拉多个）")
    @GetMapping
    public AjaxResult batch(@RequestParam(required = false) String types) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (types == null || types.isBlank()) {
            return success(result);
        }
        for (String raw : types.split(",")) {
            String type = raw.trim();
            if (type.isEmpty() || result.containsKey(type)) {
                continue;
            }
            result.put(type, isLedgerType(type) ? items(type) : List.of());
        }
        return success(result);
    }

    /** 启用项（只返回前台需要的字段） */
    private List<Map<String, Object>> items(String dictType) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<SysDictData> list = dictTypeService.selectDictDataByType(dictType);
        if (list == null) {
            return out;
        }
        for (SysDictData d : list) {
            if (!"0".equals(d.getStatus())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", d.getDictValue());
            item.put("label", d.getDictLabel());
            item.put("sort", d.getDictSort());
            out.add(item);
        }
        out.sort((a, b) -> Integer.compare(
                a.get("sort") == null ? 0 : ((Number) a.get("sort")).intValue(),
                b.get("sort") == null ? 0 : ((Number) b.get("sort")).intValue()));
        return out;
    }

    private boolean isLedgerType(String type) {
        return type != null && type.startsWith(PREFIX);
    }
}
