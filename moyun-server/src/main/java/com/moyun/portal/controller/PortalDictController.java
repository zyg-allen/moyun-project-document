package com.moyun.portal.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.moyun.common.annotation.Anonymous;
import com.moyun.common.constant.UserConstants;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.system.service.ISysDictTypeService;

/**
 * 门户公开字典 Controller
 *
 * 说明：前台（moyun-portal）免登录拉取业务字典（题目难度/题型、简历分类、AI场景、举报/反馈类型等）。
 * 安全：仅允许 portal_ / cms_ 前缀的字典类型，防止泄露 sys_user_sex 等系统字典；
 * 查询走 ISysDictTypeService.selectDictDataByType（DictUtils 缓存优先，与后台字典接口一致）。
 */
@Anonymous
@Tag(name = "门户公开字典", description = "前台免登录获取业务字典数据（仅放行 portal_/cms_ 前缀）")
@RestController
@RequestMapping("/portal/dict")
public class PortalDictController extends BaseController {

    /**
     * 允许公开访问的字典类型前缀白名单
     */
    private static final String[] ALLOWED_PREFIXES = {"portal_", "cms_"};

    @Autowired
    private ISysDictTypeService dictTypeService;

    /**
     * 按类型获取字典启用项
     *
     * @param dictType 字典类型（如 portal_question_difficulty）
     */
    @Operation(summary = "按类型获取字典数据", description = "返回指定字典类型的全部启用项，仅放行 portal_/cms_ 前缀")
    @GetMapping("/{dictType}")
    public AjaxResult getByType(@PathVariable("dictType") String dictType) {
        return success(selectDictItems(dictType));
    }

    /**
     * 批量获取字典启用项（前台一次拉多个字典）
     *
     * @param types 逗号分隔的字典类型，如 portal_question_difficulty,portal_question_type
     */
    @Operation(summary = "批量获取字典数据", description = "一次拉取多个字典类型，types 为逗号分隔，仅放行 portal_/cms_ 前缀")
    @GetMapping("/types")
    public AjaxResult getByTypes(@RequestParam("types") String types) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        if (types == null || types.isBlank()) {
            return success(result);
        }
        for (String type : types.split(",")) {
            String dictType = type.trim();
            // 去重、跳过空串；非法前缀由 selectDictItems 兜底返回空列表，不报错
            if (dictType.isEmpty() || result.containsKey(dictType)) {
                continue;
            }
            result.put(dictType, selectDictItems(dictType));
        }
        return success(result);
    }

    /**
     * 查询指定字典类型的启用项（仅必要字段），非法前缀或无数据时返回空列表
     */
    private List<Map<String, Object>> selectDictItems(String dictType) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (!isAllowedType(dictType)) {
            return items;
        }
        List<SysDictData> dictDatas = dictTypeService.selectDictDataByType(dictType);
        if (dictDatas == null) {
            return items;
        }
        for (SysDictData data : dictDatas) {
            if (!UserConstants.DICT_NORMAL.equals(data.getStatus())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dictLabel", data.getDictLabel());
            item.put("dictValue", data.getDictValue());
            item.put("listClass", data.getListClass());
            item.put("dictSort", data.getDictSort());
            items.add(item);
        }
        return items;
    }

    /**
     * 校验字典类型前缀是否在公开白名单内
     */
    private boolean isAllowedType(String dictType) {
        if (dictType == null || dictType.isEmpty()) {
            return false;
        }
        for (String prefix : ALLOWED_PREFIXES) {
            if (dictType.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
