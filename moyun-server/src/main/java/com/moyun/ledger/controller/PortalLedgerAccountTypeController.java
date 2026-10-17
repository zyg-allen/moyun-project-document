package com.moyun.ledger.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.ledger.domain.enums.AccountType;
import com.moyun.ledger.domain.enums.LiabilityType;
import com.moyun.system.service.ISysDictTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户-账户类型（资产/负债分类）。
 *
 * <p>从字典树 ledger_account_category 读取（管理员可在字典管理里自定义标签/增删），
 * 字典为空时兜底回退到枚举 {@link AccountType}/{@link LiabilityType}。
 * 不走 PortalDictController 的公开白名单 —— 账户类型属登录后记账域。</p>
 *
 * @author moyun
 */
@Tag(name = "门户-账户类型")
@RestController
@RequestMapping("/portal/ledger/accountTypes")
public class PortalLedgerAccountTypeController {

    private static final String DICT_TYPE = "ledger_account_category";

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Operation(summary = "账户类型树", description = "返回资产/负债各自的账户类型列表（字典优先，枚举兜底）")
    @GetMapping
    public AjaxResult accountTypes() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("asset", buildAsset());
        result.put("liability", buildLiability());
        return AjaxResult.success(result);
    }

    /** 资产账户类型：字典树的 asset 根下子节点 */
    private List<Map<String, String>> buildAsset() {
        List<Map<String, String>> items = childrenOf("asset");
        if (items.isEmpty()) {
            for (AccountType t : AccountType.values()) {
                items.add(item(t.value(), t.label()));
            }
        }
        return items;
    }

    /** 负债账户类型：字典树的 liability 根下子节点 */
    private List<Map<String, String>> buildLiability() {
        List<Map<String, String>> items = childrenOf("liability");
        if (items.isEmpty()) {
            for (LiabilityType t : LiabilityType.values()) {
                items.add(item(t.value(), t.label()));
            }
        }
        return items;
    }

    /** 取某根节点（dict_value=rootValue）的直接子节点 */
    private List<Map<String, String>> childrenOf(String rootValue) {
        List<SysDictData> all = dictTypeService.selectDictDataByType(DICT_TYPE);
        List<Map<String, String>> result = new ArrayList<>();
        if (all == null) {
            return result;
        }
        Long rootId = null;
        for (SysDictData d : all) {
            if ("1".equals(d.getIsFirstLevel()) && rootValue.equals(d.getDictValue())) {
                rootId = d.getDictCode();
                break;
            }
        }
        if (rootId == null) {
            return result;
        }
        for (SysDictData d : all) {
            if (rootId.equals(d.getParentDictId())) {
                result.add(item(d.getDictValue(), d.getDictLabel()));
            }
        }
        return result;
    }

    private Map<String, String> item(String value, String label) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("label", label);
        return m;
    }
}
