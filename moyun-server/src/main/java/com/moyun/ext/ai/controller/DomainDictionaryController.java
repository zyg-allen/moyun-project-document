package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.entity.DomainDictionary;
import com.moyun.ext.ai.service.DomainDictionaryService;
import com.moyun.ext.ai.service.QueryExpansionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Tag(name = "领域词典管理")
@RestController
@RequestMapping("/cms/ai/domain-dictionary")
public class DomainDictionaryController {

    @Autowired
    private DomainDictionaryService dictionaryService;

    @Autowired
    private QueryExpansionService queryExpansionService;

    @Operation(summary = "获取所有词典")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:domain-dictionary:list')")
    public AjaxResult list(
            @RequestParam(required = false) Boolean isGlobal) {

        List<DomainDictionary> list;
        if (isGlobal != null) {
            if (isGlobal) {
                list = dictionaryService.listGlobal();
            } else {
                list = dictionaryService.listProfessional();
            }
        } else {
            list = dictionaryService.list();
        }

        return AjaxResult.success(list);
    }

    @Operation(summary = "创建词典")
    @PostMapping("/create")
    @PreAuthorize("@ss.hasPermi('cms:ai:domain-dictionary:add')")
    public AjaxResult create(@RequestBody DomainDictionary dictionary) {
        dictionary.setCreateTime(LocalDateTime.now());
        dictionary.setUpdateTime(LocalDateTime.now());

        if (dictionary.getIsGlobal() == null) {
            dictionary.setIsGlobal(true);
        }

        boolean success = dictionaryService.save(dictionary);

        if (success) {
            queryExpansionService.loadFromDatabase();
            return AjaxResult.success(dictionary);
        } else {
            return AjaxResult.error("创建失败");
        }
    }

    @Operation(summary = "更新词典")
    @PutMapping("/update")
    @PreAuthorize("@ss.hasPermi('cms:ai:domain-dictionary:edit')")
    public AjaxResult update(@RequestBody DomainDictionary dictionary) {
        dictionary.setUpdateTime(LocalDateTime.now());
        boolean success = dictionaryService.updateById(dictionary);

        if (success) {
            queryExpansionService.loadFromDatabase();
            return AjaxResult.success("更新成功");
        } else {
            return AjaxResult.error("更新失败");
        }
    }

    @Operation(summary = "删除词典")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:domain-dictionary:remove')")
    public AjaxResult delete(@PathVariable Long id) {
        boolean success = dictionaryService.removeById(id);

        if (success) {
            queryExpansionService.loadFromDatabase();
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }

    @Operation(summary = "批量导入词典")
    @PostMapping("/import")
    @PreAuthorize("@ss.hasPermi('cms:ai:domain-dictionary:add')")
    public AjaxResult importDictionaries(@RequestBody List<DomainDictionary> dictionaries) {
        boolean success = dictionaryService.saveBatch(dictionaries);

        if (success) {
            queryExpansionService.loadFromDatabase();
            return AjaxResult.success("导入成功，共" + dictionaries.size() + "条");
        } else {
            return AjaxResult.error("导入失败");
        }
    }

    @Operation(summary = "重新加载词典到内存")
    @PostMapping("/reload")
    @PreAuthorize("@ss.hasPermi('cms:ai:domain-dictionary:edit')")
    public AjaxResult reload() {
        queryExpansionService.loadFromDatabase();
        int size = queryExpansionService.getDictionarySize();
        return AjaxResult.success("重新加载成功，共" + size + "个核心词");
    }
}
