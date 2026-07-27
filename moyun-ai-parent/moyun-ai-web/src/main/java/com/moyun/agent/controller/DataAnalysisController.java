package com.moyun.agent.controller;

import com.moyun.agent.common.Result;
import com.moyun.agent.dto.DataQueryRequest;
import com.moyun.agent.entity.DataSourceConfig;
import com.moyun.agent.service.DataQualityCheckService;
import com.moyun.agent.service.DataQueryService;
import com.moyun.agent.service.DataSourceService;
import com.moyun.agent.service.ReportGenerationService;
import com.moyun.agent.vo.DataQueryResponse;
import com.moyun.agent.vo.DataSourcePoolStatus;
import com.moyun.agent.vo.TableInfoVO;
import com.moyun.agent.vo.TableSchemaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据分析 Controller
 *
 * @author laomao
 */
@Slf4j
@RestController
@RequestMapping("/api/data-analysis")
@Tag(name = "数据分析", description = "AI智能数据分析接口")
public class DataAnalysisController {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DataQueryService dataQueryService;

    @Autowired
    private ReportGenerationService reportGenerationService;

    @Autowired
    private DataQualityCheckService dataQualityCheckService;

    // ==================== 数据源管理 ====================

    @Operation(summary = "获取所有数据源")
    @GetMapping("/datasources")
    public Result<List<DataSourceConfig>> listDataSources() {
        return Result.success(dataSourceService.list());
    }

    @Operation(summary = "创建数据源")
    @PostMapping("/datasources")
    public Result<DataSourceConfig> createDataSource(@RequestBody DataSourceConfig config) {
        // 测试连接
        boolean connected = dataSourceService.testConnection(config);
        if (!connected) {
            return Result.error("数据源连接失败,请检查配置");
        }

        // 保存配置
        dataSourceService.save(config);

        // 异步同步元数据
        new Thread(() -> {
            try {
                dataSourceService.syncTableMetadata(config.getId());
            } catch (Exception e) {
                log.error("同步元数据失败", e);
            }
        }).start();

        return Result.success(config);
    }

    @Operation(summary = "更新数据源")
    @PutMapping("/datasources/{id}")
    public Result<DataSourceConfig> updateDataSource(@PathVariable Long id, 
                                                      @RequestBody DataSourceConfig config) {
        config.setId(id);

        // 测试连接
        boolean connected = dataSourceService.testConnection(config);
        if (!connected) {
            return Result.error("数据源连接失败,请检查配置");
        }

        dataSourceService.updateById(config);
        return Result.success(config);
    }

    @Operation(summary = "删除数据源")
    @DeleteMapping("/datasources/{id}")
    public Result<Void> deleteDataSource(@PathVariable Long id) {
        dataSourceService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "测试数据源连接")
    @PostMapping("/datasources/test")
    public Result<Boolean> testDataSource(@RequestBody DataSourceConfig config) {
        boolean connected = dataSourceService.testConnection(config);
        if (connected) {
            return Result.success("连接成功", true);
        } else {
            return Result.error("连接失败");
        }
    }
    
    @Operation(summary = "测试已有数据源连接", description = "通过ID测试数据源连接状态")
    @PostMapping("/datasource/test-connection")
    public Result<Boolean> testConnectionById(@RequestBody java.util.Map<String, Object> request) {
        try {
            Long id = Long.valueOf(request.get("id").toString());
            DataSourceConfig config = dataSourceService.getById(id);
            if (config == null) {
                return Result.error("数据源不存在");
            }
            
            boolean connected = dataSourceService.testConnection(config);
            if (connected) {
                return Result.success("连接成功", true);
            } else {
                return Result.error("连接失败：无法建立连接");
            }
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return Result.error("连接失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查数据源健康状态")
    @GetMapping("/datasources/{id}/health")
    public Result<String> checkHealth(@PathVariable Long id) {
        String status = dataSourceService.checkHealth(id);
        return Result.<String>success(status);
    }

    // ==================== 表结构管理 ====================

    @Operation(summary = "获取数据源的所有表")
    @GetMapping("/datasources/{id}/tables")
    public Result<List<String>> listTables(@PathVariable Long id) {
        List<String> tables = dataSourceService.listTables(id);
        return Result.success(tables);
    }
    
    @Operation(summary = "获取数据源的所有表详细信息")
    @GetMapping("/datasources/{id}/tables/info")
    public Result<List<TableInfoVO>> listTablesWithInfo(@PathVariable Long id) {
        List<TableInfoVO> tables = dataSourceService.listTablesWithInfo(id);
        return Result.success(tables);
    }

    @Operation(summary = "获取表结构")
    @GetMapping("/datasources/{datasourceId}/tables/{tableName}/schema")
    public Result<TableSchemaVO> getTableSchema(@PathVariable Long datasourceId,
                                                  @PathVariable String tableName) {
        TableSchemaVO schema = dataSourceService.getTableSchema(datasourceId, tableName);
        return Result.success(schema);
    }

    @Operation(summary = "同步表元数据")
    @PostMapping("/datasources/{id}/sync")
    public Result<Void> syncMetadata(@PathVariable Long id) {
        dataSourceService.syncTableMetadata(id);
        return Result.success("同步任务已启动", null);
    }

    // ==================== 智能查询 ====================

    @Operation(summary = "智能数据查询", description = "使用自然语言查询数据,自动生成SQL并分析")
    @PostMapping("/query")
    public Result<DataQueryResponse> intelligentQuery(@RequestBody DataQueryRequest request) {
        DataQueryResponse response = dataQueryService.intelligentQuery(request);
        return Result.success(response);
    }

    @Operation(summary = "执行SQL查询", description = "直接执行SQL语句")
    @PostMapping("/execute-sql")
    public Result<DataQueryResponse> executeSQL(@RequestParam Long datasourceId,
                                                 @RequestParam String sql) {
        DataQueryResponse response = dataQueryService.executeSQL(datasourceId, sql);
        return Result.success(response);
    }

    // ==================== 报告生成 ====================

    @Operation(summary = "生成Markdown报告")
    @PostMapping("/report/markdown")
    public Result<String> generateMarkdownReport(@RequestBody DataQueryRequest request) {
        // 先执行查询
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        // 生成报告
        String report = reportGenerationService.generateMarkdownReport(response, request.getQuery());

        return Result.<String>success(report);
    }

    @Operation(summary = "生成HTML报告")
    @PostMapping("/report/html")
    public Result<String> generateHtmlReport(@RequestBody DataQueryRequest request) {
        // 先执行查询
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        // 生成报告
        String report = reportGenerationService.generateHtmlReport(response, request.getQuery());

        return Result.<String>success(report);
    }

    @Operation(summary = "下载HTML报告")
    @PostMapping("/report/download/html")
    public ResponseEntity<String> downloadHtmlReport(@RequestBody DataQueryRequest request) {
        // 先执行查询
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        // 生成报告
        String report = reportGenerationService.generateHtmlReport(response, request.getQuery());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        headers.setContentDispositionFormData("attachment", "report.html");

        return ResponseEntity.ok()
                .headers(headers)
                .body(report);
    }

    @Operation(summary = "导出Excel")
    @PostMapping("/export/excel")
    public Result<String> exportToExcel(@RequestBody DataQueryRequest request,
                                         @RequestParam String outputPath) {
        // 先执行查询
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        // 导出Excel
        boolean success = reportGenerationService.exportToExcel(
                response.getData(),
                response.getColumns(),
                outputPath
        );

        if (success) {
            return Result.success("导出成功", outputPath);
        } else {
            return Result.error("导出失败");
        }
    }

    // ==================== 数据质量检查 ====================

    @Operation(summary = "数据质量检查")
    @PostMapping("/quality-check")
    public Result<DataQualityCheckService.DataQualityReport> checkDataQuality(
            @RequestBody DataQueryRequest request) {
        // 先执行查询
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        if (!response.getSuccess()) {
            return Result.error("查询失败，无法进行质量检查");
        }

        // 执行质量检查
        DataQualityCheckService.DataQualityReport report = 
            dataQualityCheckService.checkDataQuality(
                response.getData(),
                response.getColumns()
            );

        return Result.success(report);
    }
    
    // ==================== 连接池监控 ====================
    
    @Operation(summary = "获取数据源连接池状态")
    @GetMapping("/datasources/{id}/pool-status")
    public Result<DataSourcePoolStatus> getPoolStatus(@PathVariable Long id) {
        try {
            com.moyun.agent.service.impl.DataSourceServiceImpl serviceImpl =
                (com.moyun.agent.service.impl.DataSourceServiceImpl) dataSourceService;
            DataSourcePoolStatus status = serviceImpl.getPoolStatus(id);
            
            if (status == null) {
                return Result.error("数据源不存在或连接池未初始化");
            }
            
            return Result.success(status);
        } catch (Exception e) {
            log.error("获取连接池状态失败", e);
            return Result.error("获取连接池状态失败: " + e.getMessage());
        }
    }
}
