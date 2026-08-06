package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.entity.QueryHistory;
import com.moyun.ext.ai.mapper.QueryHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cms/ai/query-history")
public class QueryHistoryController {
    
    @Autowired
    private QueryHistoryMapper queryHistoryMapper;
    
    @GetMapping("/datasource/{datasourceId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:query-history:list')")
    public AjaxResult getByDatasource(
            @PathVariable Long datasourceId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<QueryHistory> histories = queryHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QueryHistory>()
                    .eq(QueryHistory::getDatasourceId, datasourceId)
                    .eq(QueryHistory::getStatus, "success")
                    .orderByDesc(QueryHistory::getCreateTime)
                    .last("LIMIT " + limit)
            );
            
            return AjaxResult.success(histories);
        } catch (Exception e) {
            log.error("获取查询历史失败", e);
            return AjaxResult.error("获取查询历史失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/recent")
    @PreAuthorize("@ss.hasPermi('cms:ai:query-history:list')")
    public AjaxResult getRecent(@RequestParam(defaultValue = "20") Integer limit) {
        try {
            List<QueryHistory> histories = queryHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QueryHistory>()
                    .eq(QueryHistory::getStatus, "success")
                    .orderByDesc(QueryHistory::getCreateTime)
                    .last("LIMIT " + limit)
            );
            
            return AjaxResult.success(histories);
        } catch (Exception e) {
            log.error("获取最近查询历史失败", e);
            return AjaxResult.error("获取查询历史失败: " + e.getMessage());
        }
    }
}
