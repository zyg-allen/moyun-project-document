package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.entity.SysDictType;
import com.moyun.system.service.ISysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 后台-记账字典总览
 *
 * <p>记账模块的下拉候选全部由字典驱动。本接口只做"总览 + 跳转"，
 * 具体维护仍走系统字典管理（单一维护入口，避免两套编辑逻辑）。</p>
 *
 * <p><b>值锁定</b>：记账字典的 {@code dict_value} 参与历史数据匹配（如流水类型、账户类型），
 * 一旦改值历史数据就对不上，因此禁止改值；其中交易类型与账户类型属计算口径，
 * 也不允许新增取值。校验在 {@code SysDictDataServiceImpl} 内统一实施。</p>
 *
 * @author moyun
 */
@Tag(name = "CMS记账-字典总览", description = "记账下拉字典一览（维护跳转系统字典）")
@RestController
@RequestMapping("/cms/ledger/dict")
public class CmsLedgerDictController extends BaseController {

    /** 记账字典清单（type → 说明），与 App 下拉一一对应 */
    private static final Map<String, String> TYPES = new LinkedHashMap<>();

    /** 语义锁定：不允许新增取值（参与资金/统计计算口径） */
    private static final Set<String> SEMANTIC_LOCKED = new LinkedHashSet<>();

    static {
        TYPES.put("ledger_txn_type", "交易类型（收入/支出/转账/借出/收款/借款/还款/校准）");
        TYPES.put("ledger_account_category", "账户类型（资产/负债，树形字典）");
        TYPES.put("ledger_identity_tag", "身份标签（AI 财务分析画像）");
        TYPES.put("ledger_warehouse_type", "仓库类型（个人/家庭/合伙）");
        TYPES.put("ledger_shelf_type", "货架类型（普通/冷藏/上锁/暂存）");
        TYPES.put("ledger_stock_in_reason", "入库原因");
        TYPES.put("ledger_stock_out_reason", "出库原因");
        TYPES.put("ledger_product_source", "商品来源（采购/自产/自用转卖/赠品）");
        TYPES.put("ledger_out_type", "出库类型（卖出/自己用/损耗）");
        TYPES.put("ledger_packaging_type", "包装类型");
        TYPES.put("ledger_product_unit", "计量单位");
        TYPES.put("ledger_shelf_life", "保质期预设");
        TYPES.put("ledger_condition_grade", "二手成色");
        TYPES.put("ledger_saving_method", "存钱方式");
        TYPES.put("ledger_schedule_cycle", "定时记账周期");
        TYPES.put("ledger_remind_rule", "备忘提醒规则");
        TYPES.put("cms_feedback_type", "反馈类型（App 意见反馈）");

        SEMANTIC_LOCKED.add("ledger_txn_type");
        SEMANTIC_LOCKED.add("ledger_account_category");
    }

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Operation(summary = "记账字典总览（类型/名称/条数/锁定级别）")
    @PreAuthorize("@ss.hasPermi('cms:ledgerDict:list')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        List<Map<String, Object>> rows = TYPES.entrySet().stream().map(e -> {
            String type = e.getKey();
            SysDictType dictType = dictTypeService.selectDictTypeByType(type);
            int count = dictTypeService.selectDictDataByType(type) == null
                    ? 0 : dictTypeService.selectDictDataByType(type).size();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("dictType", type);
            row.put("dictName", dictType == null ? e.getValue() : dictType.getDictName());
            row.put("dictId", dictType == null ? null : dictType.getDictId());
            row.put("count", count);
            row.put("missing", dictType == null);
            row.put("semanticLocked", SEMANTIC_LOCKED.contains(type));
            row.put("valueLocked", true);
            return row;
        }).toList();
        return success(rows);
    }
}
