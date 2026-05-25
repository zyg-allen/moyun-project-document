package com.moyun.ext.generator.controller;

import com.moyun.common.annotation.Log;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.TableDataInfo;
import com.moyun.common.enums.BusinessType;
import com.moyun.common.exception.system.BaseException;
import com.moyun.ext.generator.domain.entity.GenTable;
import com.moyun.ext.generator.service.IGenTableService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成 操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/tool/gen")
public class GenController {

    @Autowired
    private IGenTableService genTableService;

    @PreAuthorize("@ss.hasPermi('tool:gen:list')")
    @GetMapping("/list")
    public TableDataInfo genList(GenTable genTable) {
        startPage();
        List<GenTable> list = genTableService.selectGenTableList(genTable);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('tool:gen:export')")
    @Log(title = "代码生成", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GenTable genTable) {
        List<GenTable> list = genTableService.selectGenTableList(genTable);
    }

    @GetMapping(value = "/{tableId}")
    public AjaxResult getInfo(@PathVariable Long tableId) {
        GenTable table = genTableService.selectGenTableById(tableId);
        List<GenTable> columns = genTableService.selectGenTableListByIds(new Long[]{tableId});
        Map<String, Object> map = new HashMap<>();
        map.put("info", table);
        map.put("rows", columns);
        return success(map);
    }

    @PreAuthorize("@ss.hasPermi('tool:gen:query')")
    @GetMapping(value = "/{tableName}/{businessName}")
    public AjaxResult getInfo(@PathVariable("tableName") String tableName,
                              @PathVariable("businessName") String businessName) {
        GenTable table = genTableService.selectGenTableByName(tableName);
        List<GenTable> columns = genTableService.selectGenTableListByIds(new Long[]{table.getTableId()});
        Map<String, Object> map = new HashMap<>();
        map.put("info", table);
        map.put("rows", columns);
        return success(map);
    }
}
