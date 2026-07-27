package com.moyun.agent.controller;

import com.moyun.agent.common.ApiResponse;
import com.moyun.agent.entity.DomainDictionary;
import com.moyun.agent.service.DomainDictionaryService;
import com.moyun.agent.service.QueryExpansionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 领域词典管理控制器
 *
 * <p>提供领域词典的增删改查功能，用于RAG查询扩展</p>
 *
 * @author laomao
 */
@Slf4j
@Tag(name = "领域词典管理")
@RestController
@RequestMapping("/api/domain-dictionary")
public class DomainDictionaryController {

    @Autowired
    private DomainDictionaryService dictionaryService;

    @Autowired
    private QueryExpansionService queryExpansionService;

    @Operation(summary = "获取所有词典")
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<DomainDictionary>>> list(
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

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @Operation(summary = "创建词典")
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<DomainDictionary>> create(@RequestBody DomainDictionary dictionary) {
        dictionary.setCreateTime(LocalDateTime.now());
        dictionary.setUpdateTime(LocalDateTime.now());

        if (dictionary.getIsGlobal() == null) {
            dictionary.setIsGlobal(true);
        }

        boolean success = dictionaryService.save(dictionary);

        if (success) {
            // 重新加载词典到内存
            queryExpansionService.loadFromDatabase();
            return ResponseEntity.ok(ApiResponse.success(dictionary));
        } else {
            return ResponseEntity.ok(ApiResponse.error("创建失败"));
        }
    }

    @Operation(summary = "更新词典")
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<String>> update(@RequestBody DomainDictionary dictionary) {
        dictionary.setUpdateTime(LocalDateTime.now());
        boolean success = dictionaryService.updateById(dictionary);

        if (success) {
            // 重新加载词典到内存
            queryExpansionService.loadFromDatabase();
            return ResponseEntity.ok(ApiResponse.success("更新成功"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("更新失败"));
        }
    }

    @Operation(summary = "删除词典")
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long id) {
        boolean success = dictionaryService.removeById(id);

        if (success) {
            // 重新加载词典到内存
            queryExpansionService.loadFromDatabase();
            return ResponseEntity.ok(ApiResponse.success("删除成功"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("删除失败"));
        }
    }

    @Operation(summary = "批量导入词典")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<String>> importDictionaries(@RequestBody List<DomainDictionary> dictionaries) {
        boolean success = dictionaryService.saveBatch(dictionaries);

        if (success) {
            // 重新加载词典到内存
            queryExpansionService.loadFromDatabase();
            return ResponseEntity.ok(ApiResponse.success("导入成功，共" + dictionaries.size() + "条"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("导入失败"));
        }
    }

    @Operation(summary = "重新加载词典到内存")
    @PostMapping("/reload")
    public ResponseEntity<ApiResponse<String>> reload() {
        queryExpansionService.loadFromDatabase();
        int size = queryExpansionService.getDictionarySize();
        return ResponseEntity.ok(ApiResponse.success("重新加载成功，共" + size + "个核心词"));
    }
}
