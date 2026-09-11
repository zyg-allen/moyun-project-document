package com.moyun.ext.ai2.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.ext.ai2.entity.AiExecuteLog;
import com.moyun.ext.ai2.mapper.AiExecuteLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 执行日志管理 Controller（v11.60 P1-1：可观测性补齐——数据在采也要有人看）
 *
 * <p>统一网关 ai_execute_log 的管理端查询页：列表（requestId/场景/模型/状态/耗时/token 筛选）
 * + 汇总卡片（调用量/成功率/Token/成本/平均耗时，随筛选联动）+ 详情 + 清理。</p>
 *
 * <p>路径前缀 /cms/ai/execute-log（admin 鉴权体系）。日志由
 * {@code AiExecuteLogService.record} 异步落库，本接口只读为主，删除用于过期数据清理。</p>
 *
 * @author laomao
 * @since 2026-09-11
 */
@Slf4j
@Tag(name = "AI执行日志管理", description = "统一网关执行日志查询与清理（可观测性）")
@RestController
@RequestMapping("/cms/ai/execute-log")
public class AiExecuteLogController extends BaseController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private AiExecuteLogMapper executeLogMapper;

    @Operation(summary = "执行日志分页列表", description = "支持 requestId/场景/模型/状态/日期范围筛选，按时间倒序")
    @PreAuthorize("@ss.hasPermi('cms:ai:execute-log:list')")
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String sceneCode,
            @RequestParam(required = false) String modelUsed,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<AiExecuteLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiExecuteLog> wrapper = new LambdaQueryWrapper<>();
        if (requestId != null && !requestId.isBlank()) {
            wrapper.eq(AiExecuteLog::getRequestId, requestId.trim());
        }
        if (sceneCode != null && !sceneCode.isBlank()) {
            wrapper.eq(AiExecuteLog::getSceneCode, sceneCode.trim());
        }
        if (modelUsed != null && !modelUsed.isBlank()) {
            wrapper.like(AiExecuteLog::getModelUsed, modelUsed.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AiExecuteLog::getStatus, status.trim());
        }
        LocalDateTime begin = parseBegin(beginDate);
        LocalDateTime end = parseEnd(endDate);
        wrapper.ge(begin != null, AiExecuteLog::getCreateTime, begin);
        wrapper.le(end != null, AiExecuteLog::getCreateTime, end);
        wrapper.orderByDesc(AiExecuteLog::getCreateTime);
        wrapper.orderByDesc(AiExecuteLog::getId);
        return success(executeLogMapper.selectPage(page, wrapper));
    }

    @Operation(summary = "执行日志汇总", description = "当前筛选下的调用量/成功率/Token/成本/平均耗时（汇总卡片）")
    @PreAuthorize("@ss.hasPermi('cms:ai:execute-log:list')")
    @GetMapping("/summary")
    public AjaxResult summary(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String sceneCode,
            @RequestParam(required = false) String modelUsed,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate) {
        QueryWrapper<AiExecuteLog> wrapper = new QueryWrapper<>();
        if (requestId != null && !requestId.isBlank()) {
            wrapper.eq("request_id", requestId.trim());
        }
        if (sceneCode != null && !sceneCode.isBlank()) {
            wrapper.eq("scene_code", sceneCode.trim());
        }
        if (modelUsed != null && !modelUsed.isBlank()) {
            wrapper.like("model_used", modelUsed.trim());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq("status", status.trim());
        }
        LocalDateTime begin = parseBegin(beginDate);
        LocalDateTime end = parseEnd(endDate);
        wrapper.ge(begin != null, "create_time", begin);
        wrapper.le(end != null, "create_time", end);
        wrapper.select(
                "COUNT(*) AS totalCount",
                "SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) AS successCount",
                "COALESCE(SUM(token_used), 0) AS totalTokens",
                "COALESCE(SUM(cost_yuan), 0) AS totalCost",
                "COALESCE(AVG(elapsed_ms), 0) AS avgElapsed");
        List<Map<String, Object>> rows = executeLogMapper.selectMaps(wrapper);
        Map<String, Object> result = new HashMap<>();
        if (rows != null && !rows.isEmpty() && rows.get(0) != null) {
            result.putAll(rows.get(0));
        }
        // 失败 = 总数 - 成功（含 fail/timeout 全口径）
        long total = toLong(result.get("totalCount"));
        long success = toLong(result.get("successCount"));
        result.put("failCount", total - success);
        result.put("successRate", total > 0 ? Math.round(success * 1000.0 / total) / 10.0 : 0.0);
        return success(result);
    }

    @Operation(summary = "日志中出现的场景代码", description = "distinct scene_code，供筛选下拉")
    @PreAuthorize("@ss.hasPermi('cms:ai:execute-log:list')")
    @GetMapping("/scene-options")
    public AjaxResult sceneOptions() {
        QueryWrapper<AiExecuteLog> wrapper = new QueryWrapper<>();
        wrapper.select("DISTINCT scene_code");
        wrapper.isNotNull("scene_code");
        wrapper.orderByAsc("scene_code");
        List<Map<String, Object>> rows = executeLogMapper.selectMaps(wrapper);
        return success(rows == null ? List.of()
                : rows.stream().map(r -> String.valueOf(r.get("scene_code"))).toList());
    }

    @Operation(summary = "执行日志详情", description = "含输入/输出摘要、错误信息、工具调用记录")
    @PreAuthorize("@ss.hasPermi('cms:ai:execute-log:query')")
    @GetMapping("/{id}")
    public AjaxResult getDetail(@PathVariable("id") Long id) {
        AiExecuteLog logEntry = executeLogMapper.selectById(id);
        if (logEntry == null) {
            return error("日志不存在或已清理");
        }
        return success(logEntry);
    }

    @Operation(summary = "删除执行日志", description = "批量物理删除，用于过期数据清理（日志只增不改）")
    @PreAuthorize("@ss.hasPermi('cms:ai:execute-log:remove')")
    @Log(title = "AI执行日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable("ids") Long[] ids) {
        if (ids == null || ids.length == 0) {
            return error("请选择要删除的日志");
        }
        return toAjax(executeLogMapper.deleteBatchIds(java.util.Arrays.asList(ids)));
    }

    // ==================== 辅助 ====================

    /** "yyyy-MM-dd" → 当日 00:00:00；非法/空返回 null（不过滤） */
    private LocalDateTime parseBegin(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim(), DATE_FMT).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    /** "yyyy-MM-dd" → 当日 23:59:59；非法/空返回 null（不过滤） */
    private LocalDateTime parseEnd(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim(), DATE_FMT).atTime(23, 59, 59);
        } catch (Exception e) {
            return null;
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }
}
