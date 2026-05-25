package com.moyun.ext.generator.controller;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.moyun.common.annotation.Log;
import com.moyun.common.enums.BusinessType;
import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;
import com.moyun.core.base.TableDataInfo;
import com.moyun.core.base.text.Convert;
import com.moyun.ext.generator.config.GenConfig;
import com.moyun.ext.generator.domain.GenTable;
import com.moyun.ext.generator.domain.GenTableColumn;
import com.moyun.ext.generator.service.IGenTableColumnService;
import com.moyun.ext.generator.service.IGenTableService;
import com.moyun.util.security.SecurityUtils;
import com.moyun.util.string.SqlUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 代码生成 操作处理
 *
 * @author ruoyi
 */
@Tag(name = "代码生成", description = "代码生成管理接口")
@RestController
@RequestMapping("/tool/gen")
public class GenController extends BaseController {
    @Autowired
    private IGenTableService genTableService;

    @Autowired
    private IGenTableColumnService genTableColumnService;

    /**
     * 表名合法校验正则：只允许字母、数字、下划线
     */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * 查询代码生成列表
     */
    @Operation(summary = "查询代码生成列表", description = "根据条件查询代码生成列表")
    @PreAuthorize("@ss.hasPermi('tool:gen:list')")
    @GetMapping("/list")
    public TableDataInfo genList(GenTable genTable) {
        startPage();
        List<GenTable> list = genTableService.selectGenTableList(genTable);
        return getDataTable(list);
    }

    /**
     * 获取代码生成信息
     */
    @Operation(summary = "获取代码生成信息", description = "根据表ID获取代码生成详细信息")
    @PreAuthorize("@ss.hasPermi('tool:gen:query')")
    @GetMapping(value = "/{tableId}")
    public AjaxResult getInfo(@Parameter(description = "表ID") @PathVariable Long tableId) {
        GenTable table = genTableService.selectGenTableById(tableId);
        List<GenTable> tables = genTableService.selectGenTableAll();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("info", table);
        map.put("rows", list);
        map.put("tables", tables);
        return success(map);
    }

    /**
     * 查询数据库列表
     */
    @Operation(summary = "查询数据库列表", description = "查询数据库中可导入的表列表")
    @PreAuthorize("@ss.hasPermi('tool:gen:list')")
    @GetMapping("/db/list")
    public TableDataInfo dataList(GenTable genTable) {
        startPage();
        List<GenTable> list = genTableService.selectDbTableList(genTable);
        return getDataTable(list);
    }

    /**
     * 查询数据表字段列表
     */
    @Operation(summary = "查询数据表字段列表", description = "根据表ID查询数据表字段列表")
    @PreAuthorize("@ss.hasPermi('tool:gen:list')")
    @GetMapping(value = "/column/{tableId}")
    public TableDataInfo columnList(@Parameter(description = "表ID") @PathVariable Long tableId) {
        if (tableId == null) {
            return new TableDataInfo();
        }
        TableDataInfo dataInfo = new TableDataInfo();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        dataInfo.setRows(list);
        dataInfo.setTotal(list.size());
        return dataInfo;
    }

    /**
     * 导入表结构（保存）
     */
    @Operation(summary = "导入表结构", description = "从数据库导入表结构到代码生成器")
    @PreAuthorize("@ss.hasPermi('tool:gen:import')")
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    @PostMapping("/importTable")
    public AjaxResult importTableSave(@Parameter(description = "表名称，多个用逗号分隔") String tables) {
        String[] tableNames = Convert.toStrArray(tables);
        // 查询表信息
        List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames);
        genTableService.importGenTable(tableList, SecurityUtils.getUsername());
        return success();
    }

    /**
     * 创建表结构（保存）
     */
    @Operation(summary = "创建表结构", description = "通过SQL语句创建数据库表并导入到代码生成器")
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "创建表", businessType = BusinessType.OTHER)
    @PostMapping("/createTable")
    public AjaxResult createTableSave(@Parameter(description = "SQL建表语句") String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return AjaxResult.error("SQL语句不能为空");
        }
        try {
            SqlUtil.filterKeyword(sql);
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            List<String> tableNames = new ArrayList<>();
            for (SQLStatement sqlStatement : sqlStatements) {
                if (sqlStatement instanceof MySqlCreateTableStatement) {
                    MySqlCreateTableStatement createTableStatement = (MySqlCreateTableStatement) sqlStatement;
                    String tableName = createTableStatement.getTableName().replaceAll("", "");
                    // 校验表名合法性
                    if (!isValidTableName(tableName)) {
                        return AjaxResult.error("表名格式不合法：" + tableName);
                    }
                    if (genTableService.createTable(createTableStatement.toString())) {
                        tableNames.add(tableName);
                    }
                } else {
                    logger.warn("仅支持CREATE TABLE语句，当前语句类型：{}", sqlStatement.getClass().getSimpleName());
                }
            }
            if (tableNames.isEmpty()) {
                return AjaxResult.error("未创建任何表，请检查SQL语句");
            }
            List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames.toArray(new String[0]));
            String operName = SecurityUtils.getUsername();
            genTableService.importGenTable(tableList, operName);
            return AjaxResult.success("创建表结构成功");
        } catch (Exception e) {
            logger.error("创建表结构异常：{}", e.getMessage(), e);
            return AjaxResult.error("创建表结构失败，请检查SQL语法是否正确");
        }
    }

    /**
     * 修改保存代码生成业务
     */
    @Operation(summary = "修改代码生成配置", description = "修改并保存代码生成业务配置")
    @PreAuthorize("@ss.hasPermi('tool:gen:edit')")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult editSave(@Validated @RequestBody GenTable genTable) {
        genTableService.validateEdit(genTable);
        genTableService.updateGenTable(genTable);
        return success();
    }

    /**
     * 删除代码生成
     */
    @Operation(summary = "删除代码生成", description = "删除代码生成配置")
    @PreAuthorize("@ss.hasPermi('tool:gen:remove')")
    @Log(title = "代码生成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tableIds}")
    public AjaxResult remove(@Parameter(description = "表ID数组") @PathVariable Long[] tableIds) {
        genTableService.deleteGenTableByIds(tableIds);
        return success();
    }

    /**
     * 预览代码
     */
    @Operation(summary = "预览代码", description = "预览生成的代码")
    @PreAuthorize("@ss.hasPermi('tool:gen:preview')")
    @GetMapping("/preview/{tableId}")
    public AjaxResult preview(@Parameter(description = "表ID") @PathVariable("tableId") Long tableId) throws IOException {
        Map<String, String> dataMap = genTableService.previewCode(tableId);
        return success(dataMap);
    }

    /**
     * 生成代码（下载方式）
     */
    @Operation(summary = "生成代码（下载方式）", description = "生成代码并下载zip压缩包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回zip文件"),
            @ApiResponse(responseCode = "400", description = "表名格式不合法")
    })
    @PreAuthorize("@ss.hasPermi('tool:gen:code')")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/download/{tableName}")
    public void download(HttpServletResponse response, @Parameter(description = "表名称") @PathVariable("tableName") String tableName) throws IOException {
        // 校验表名合法性，防止路径遍历攻击
        if (!isValidTableName(tableName)) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":400,\"msg\":\"表名格式不合法\"}");
            return;
        }
        byte[] data = genTableService.downloadCode(tableName);
        genCode(response, data, tableName);
    }

    /**
     * 生成代码（自定义路径）
     */
    @Operation(summary = "生成代码（自定义路径）", description = "生成代码到自定义路径")
    @PreAuthorize("@ss.hasPermi('tool:gen:code')")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/genCode/{tableName}")
    public AjaxResult genCode(@Parameter(description = "表名称") @PathVariable("tableName") String tableName) {
        // 校验表名合法性
        if (!isValidTableName(tableName)) {
            return AjaxResult.error("表名格式不合法");
        }
        if (!GenConfig.isAllowOverwrite()) {
            return AjaxResult.error("【系统预设】不允许生成文件覆盖到本地");
        }
        genTableService.generatorCode(tableName);
        return success();
    }

    /**
     * 同步数据库
     */
    @Operation(summary = "同步数据库", description = "同步数据库表结构到代码生成器")
    @PreAuthorize("@ss.hasPermi('tool:gen:edit')")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @GetMapping("/synchDb/{tableName}")
    public AjaxResult synchDb(@Parameter(description = "表名称") @PathVariable("tableName") String tableName) {
        // 校验表名合法性
        if (!isValidTableName(tableName)) {
            return AjaxResult.error("表名格式不合法");
        }
        genTableService.synchDb(tableName);
        return success();
    }

    /**
     * 批量生成代码
     */
    @Operation(summary = "批量生成代码", description = "批量生成代码并下载zip压缩包")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功返回zip文件"),
            @ApiResponse(responseCode = "400", description = "表名格式不合法")
    })
    @PreAuthorize("@ss.hasPermi('tool:gen:code')")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/batchGenCode")
    public void batchGenCode(HttpServletResponse response, @Parameter(description = "表名称，多个用逗号分隔") String tables) throws IOException {
        String[] tableNames = Convert.toStrArray(tables);
        // 校验所有表名合法性
        for (String tableName : tableNames) {
            if (!isValidTableName(tableName)) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":400,\"msg\":\"表名格式不合法：" + tableName + "\"}");
                return;
            }
        }
        byte[] data = genTableService.downloadCode(tableNames);
        genCode(response, data, "batch_code");
    }

    /**
     * 生成zip文件
     *
     * @param response HTTP响应
     * @param data     文件数据
     * @param fileName 文件名（不含扩展名）
     */
    private void genCode(HttpServletResponse response, byte[] data, String fileName) throws IOException {
        response.reset();
        // 移除通配符配置，避免CSRF风险
        // 如需跨域支持，请在安全配置中统一配置允许的域名白名单
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(fileName + ".zip", StandardCharsets.UTF_8.name()) + "\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(data, response.getOutputStream());
    }

    /**
     * 校验表名是否合法
     * 只允许字母、数字、下划线，且不能以数字开头
     *
     * @param tableName 表名
     * @return 是否合法
     */
    private boolean isValidTableName(String tableName) {
        return tableName != null && TABLE_NAME_PATTERN.matcher(tableName).matches();
    }
}
