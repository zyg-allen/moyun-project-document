package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.dto.DataQueryRequest;
import com.moyun.ext.ai.entity.DataSourceConfig;
import com.moyun.ext.ai.service.DataQualityCheckService;
import com.moyun.ext.ai.service.DataQueryService;
import com.moyun.ext.ai.service.DataSourceService;
import com.moyun.ext.ai.service.ReportGenerationService;
import com.moyun.ext.ai.vo.DataQueryResponse;
import com.moyun.ext.ai.vo.DataSourcePoolStatus;
import com.moyun.ext.ai.vo.TableInfoVO;
import com.moyun.ext.ai.vo.TableSchemaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cms/ai/data-analysis")
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

    @Operation(summary = "获取所有数据源")
    @GetMapping("/datasources")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:list')")
    public AjaxResult listDataSources() {
        return AjaxResult.success(dataSourceService.list());
    }

    @Operation(summary = "创建数据源")
    @PostMapping("/datasources")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:add')")
    public AjaxResult createDataSource(@RequestBody DataSourceConfig config) {
        boolean connected = dataSourceService.testConnection(config);
        if (!connected) {
            return AjaxResult.error("数据源连接失败,请检查配置");
        }

        dataSourceService.save(config);

        new Thread(() -> {
            try {
                dataSourceService.syncTableMetadata(config.getId());
            } catch (Exception e) {
                log.error("同步元数据失败", e);
            }
        }).start();

        return AjaxResult.success(config);
    }

    @Operation(summary = "更新数据源")
    @PutMapping("/datasources/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:edit')")
    public AjaxResult updateDataSource(@PathVariable Long id, 
                                                      @RequestBody DataSourceConfig config) {
        config.setId(id);

        boolean connected = dataSourceService.testConnection(config);
        if (!connected) {
            return AjaxResult.error("数据源连接失败,请检查配置");
        }

        dataSourceService.updateById(config);
        return AjaxResult.success(config);
    }

    @Operation(summary = "删除数据源")
    @DeleteMapping("/datasources/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:remove')")
    public AjaxResult deleteDataSource(@PathVariable Long id) {
        dataSourceService.removeById(id);
        return AjaxResult.success();
    }

    @Operation(summary = "测试数据源连接")
    @PostMapping("/datasources/test")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult testDataSource(@RequestBody DataSourceConfig config) {
        boolean connected = dataSourceService.testConnection(config);
        if (connected) {
            return AjaxResult.success("连接成功", true);
        } else {
            return AjaxResult.error("连接失败");
        }
    }
    
    @Operation(summary = "测试已有数据源连接", description = "通过ID测试数据源连接状态")
    @PostMapping("/datasource/test-connection")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult testConnectionById(@RequestBody java.util.Map<String, Object> request) {
        try {
            Long id = Long.valueOf(request.get("id").toString());
            DataSourceConfig config = dataSourceService.getById(id);
            if (config == null) {
                return AjaxResult.error("数据源不存在");
            }
            
            boolean connected = dataSourceService.testConnection(config);
            if (connected) {
                return AjaxResult.success("连接成功", true);
            } else {
                return AjaxResult.error("连接失败：无法建立连接");
            }
        } catch (Exception e) {
            log.error("测试连接失败", e);
            return AjaxResult.error("连接失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查数据源健康状态")
    @GetMapping("/datasources/{id}/health")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult checkHealth(@PathVariable Long id) {
        String status = dataSourceService.checkHealth(id);
        return AjaxResult.success(status);
    }

    @Operation(summary = "获取数据源的所有表")
    @GetMapping("/datasources/{id}/tables")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:list')")
    public AjaxResult listTables(@PathVariable Long id) {
        List<String> tables = dataSourceService.listTables(id);
        return AjaxResult.success(tables);
    }
    
    @Operation(summary = "获取数据源的所有表详细信息")
    @GetMapping("/datasources/{id}/tables/info")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:list')")
    public AjaxResult listTablesWithInfo(@PathVariable Long id) {
        List<TableInfoVO> tables = dataSourceService.listTablesWithInfo(id);
        return AjaxResult.success(tables);
    }

    @Operation(summary = "获取表结构")
    @GetMapping("/datasources/{datasourceId}/tables/{tableName}/schema")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult getTableSchema(@PathVariable Long datasourceId,
                                                  @PathVariable String tableName) {
        TableSchemaVO schema = dataSourceService.getTableSchema(datasourceId, tableName);
        return AjaxResult.success(schema);
    }

    @Operation(summary = "同步表元数据")
    @PostMapping("/datasources/{id}/sync")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:sync')")
    public AjaxResult syncMetadata(@PathVariable Long id) {
        dataSourceService.syncTableMetadata(id);
        return AjaxResult.success("同步任务已启动", null);
    }

    @Operation(summary = "智能数据查询", description = "使用自然语言查询数据,自动生成SQL并分析")
    @PostMapping("/query")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult intelligentQuery(@RequestBody DataQueryRequest request) {
        DataQueryResponse response = dataQueryService.intelligentQuery(request);
        return AjaxResult.success(response);
    }

    @Operation(summary = "执行SQL查询", description = "直接执行SQL语句")
    @PostMapping("/execute-sql")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult executeSQL(@RequestParam Long datasourceId,
                                                 @RequestParam String sql) {
        DataQueryResponse response = dataQueryService.executeSQL(datasourceId, sql);
        return AjaxResult.success(response);
    }

    @Operation(summary = "生成Markdown报告")
    @PostMapping("/report/markdown")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult generateMarkdownReport(@RequestBody DataQueryRequest request) {
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        String report = reportGenerationService.generateMarkdownReport(response, request.getQuery());

        return AjaxResult.success(report);
    }

    @Operation(summary = "生成HTML报告")
    @PostMapping("/report/html")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult generateHtmlReport(@RequestBody DataQueryRequest request) {
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        String report = reportGenerationService.generateHtmlReport(response, request.getQuery());

        return AjaxResult.success(report);
    }

    @Operation(summary = "下载HTML报告")
    @PostMapping("/report/download/html")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public ResponseEntity<String> downloadHtmlReport(@RequestBody DataQueryRequest request) {
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

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
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult exportToExcel(@RequestBody DataQueryRequest request,
                                         @RequestParam String outputPath) {
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        boolean success = reportGenerationService.exportToExcel(
                response.getData(),
                response.getColumns(),
                outputPath
        );

        if (success) {
            return AjaxResult.success("导出成功", outputPath);
        } else {
            return AjaxResult.error("导出失败");
        }
    }

    @Operation(summary = "数据质量检查")
    @PostMapping("/quality-check")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult checkDataQuality(
            @RequestBody DataQueryRequest request) {
        DataQueryResponse response = dataQueryService.intelligentQuery(request);

        if (!response.getSuccess()) {
            return AjaxResult.error("查询失败，无法进行质量检查");
        }

        DataQualityCheckService.DataQualityReport report = 
            dataQualityCheckService.checkDataQuality(
                response.getData(),
                response.getColumns()
            );

        return AjaxResult.success(report);
    }
    
    @Operation(summary = "获取数据源连接池状态")
    @GetMapping("/datasources/{id}/pool-status")
    @PreAuthorize("@ss.hasPermi('cms:ai:data-analysis:query')")
    public AjaxResult getPoolStatus(@PathVariable Long id) {
        try {
            com.moyun.ext.ai.service.impl.DataSourceServiceImpl serviceImpl =
                (com.moyun.ext.ai.service.impl.DataSourceServiceImpl) dataSourceService;
            DataSourcePoolStatus status = serviceImpl.getPoolStatus(id);
            
            if (status == null) {
                return AjaxResult.error("数据源不存在或连接池未初始化");
            }
            
            return AjaxResult.success(status);
        } catch (Exception e) {
            log.error("获取连接池状态失败", e);
            return AjaxResult.error("获取连接池状态失败: " + e.getMessage());
        }
    }
}
