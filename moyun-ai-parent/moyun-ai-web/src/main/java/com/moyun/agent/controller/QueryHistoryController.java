package com.moyun.agent.controller;

import com.moyun.agent.common.Result;
import com.moyun.agent.entity.QueryHistory;
import com.moyun.agent.mapper.QueryHistoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 查询历史接口
 */
@Slf4j
@RestController
@RequestMapping("/api/query-history")
public class QueryHistoryController {
    
    @Autowired
    private QueryHistoryMapper queryHistoryMapper;
    
    /**
     * 获取指定数据源的查询历史
     */
    @GetMapping("/datasource/{datasourceId}")
    public Result<List<QueryHistory>> getByDatasource(
            @PathVariable Long datasourceId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            // 只返回成功的查询
            List<QueryHistory> histories = queryHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QueryHistory>()
                    .eq(QueryHistory::getDatasourceId, datasourceId)
                    .eq(QueryHistory::getStatus, "success")
                    .orderByDesc(QueryHistory::getCreateTime)
                    .last("LIMIT " + limit)
            );
            
            return Result.success(histories);
        } catch (Exception e) {
            log.error("获取查询历史失败", e);
            return Result.error("获取查询历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近的查询历史（不分数据源）
     */
    @GetMapping("/recent")
    public Result<List<QueryHistory>> getRecent(@RequestParam(defaultValue = "20") Integer limit) {
        try {
            List<QueryHistory> histories = queryHistoryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QueryHistory>()
                    .eq(QueryHistory::getStatus, "success")
                    .orderByDesc(QueryHistory::getCreateTime)
                    .last("LIMIT " + limit)
            );
            
            return Result.success(histories);
        } catch (Exception e) {
            log.error("获取最近查询历史失败", e);
            return Result.error("获取查询历史失败: " + e.getMessage());
        }
    }
}
