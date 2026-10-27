package com.moyun.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.entity.SysDictData;
import com.moyun.system.service.ISysDictDataService;
import com.moyun.system.service.ISysDictTypeService;
import com.moyun.util.bean.PageUtils;
import com.moyun.util.file.ExcelUtil;
import com.moyun.util.string.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据字典信息
 *
 * @author allen-zyg
 */
@Tag(name = "字典数据管理", description = "字典数据的增删改查、导出等操作接口")
@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController extends BaseController {
    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Operation(summary = "获取字典数据列表", description = "分页查询字典数据列表")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public AjaxResult list(SysDictData dictData) {
        Page<SysDictData> page = PageUtils.startPage();
        dictDataService.selectDictDataPage(page, dictData);
        return success(page);
    }

    @Operation(summary = "导出字典数据", description = "导出字典数据到Excel文件")
    @Log(title = "字典数据", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:dict:export')")
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "sysdictdata:", time = 60, count = 120)
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDictData dictData) {
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        ExcelUtil<SysDictData> util = new ExcelUtil<SysDictData>(SysDictData.class);
        util.exportExcel(response, list, "字典数据");
    }

    /**
     * 查询字典数据详细
     */
    @Operation(summary = "获取字典数据详情", description = "根据字典编码获取字典数据详细信息")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping(value = "/{dictCode}")
    public AjaxResult getInfo(@Parameter(description = "字典编码") @PathVariable Long dictCode) {
        return success(dictDataService.selectDictDataById(dictCode));
    }

    /**
     * 根据字典类型查询字典数据信息
     */
    @Operation(summary = "根据字典类型查询字典数据", description = "根据字典类型获取字典数据列表")
    @GetMapping(value = "/type/{dictType}")
    public AjaxResult dictType(@Parameter(description = "字典类型") @PathVariable String dictType) {
        List<SysDictData> data = dictTypeService.selectDictDataByType(dictType);
        if (StringUtils.isNull(data)) {
            data = new ArrayList<SysDictData>();
        }
        return success(data);
    }

    /**
     * 根据字典类型查询树形字典（公共查询，仅启用节点，带缓存）
     */
    @Operation(summary = "根据字典类型查询树形字典", description = "返回组装好的树形字典数据（仅启用节点），供前端级联与下拉使用")
    @GetMapping(value = "/tree/{dictType}")
    public AjaxResult dictTree(@Parameter(description = "字典类型") @PathVariable String dictType) {
        List<SysDictData> data = dictTypeService.selectDictTreeByType(dictType);
        if (StringUtils.isNull(data)) {
            data = new ArrayList<SysDictData>();
        }
        return success(data);
    }

    /**
     * 根据字典类型查询树形字典全量数据（管理页用，含停用节点，平铺返回）
     */
    @Operation(summary = "根据字典类型查询树形字典全量数据", description = "返回指定类型的全量字典数据（含停用节点），供树形字典维护页组装树")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping(value = "/tree/all/{dictType}")
    public AjaxResult dictTreeAll(@Parameter(description = "字典类型") @PathVariable String dictType) {
        SysDictData query = new SysDictData();
        query.setDictType(dictType);
        List<SysDictData> data = dictDataService.selectDictDataList(query);
        if (StringUtils.isNull(data)) {
            data = new ArrayList<SysDictData>();
        }
        return success(data);
    }

    /**
     * 新增字典类型
     */
    @Operation(summary = "新增字典数据", description = "创建新字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @Log(title = "字典数据", businessType = BusinessType.INSERT)
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "sysdictdata:", time = 60, count = 120)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDictData dict) {
        dict.setCreateBy(getUsername());
        return toAjax(dictDataService.insertDictData(dict));
    }

    /**
     * 修改保存字典类型
     */
    @Operation(summary = "修改字典数据", description = "更新字典数据信息")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "sysdictdata:", time = 60, count = 120)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDictData dict) {
        dict.setUpdateBy(getUsername());
        return toAjax(dictDataService.updateDictData(dict));
    }

    /**
     * 删除字典类型
     */
    @Operation(summary = "删除字典数据", description = "批量删除字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @com.moyun.common.annotation.RepeatSubmit(interval = 2000)
    @com.moyun.common.annotation.RateLimiter(key = "sysdictdata:", time = 60, count = 120)
    @DeleteMapping("/{dictCodes}")
    public AjaxResult remove(@Parameter(description = "字典编码数组") @PathVariable Long[] dictCodes) {
        dictDataService.deleteDictDataByIds(dictCodes);
        return success();
    }
}
