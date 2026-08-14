package com.moyun.core.base.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通用导入结果 DTO
 * <p>
 * 所有业务的 Excel 批量导入统一返回此结构，前端组件 {@code ImportDialog} 据此渲染
 * 成功/失败统计与失败明细表格，并支持"下载失败行 Excel"二次修正后重导。
 * <p>
 * 设计要点：
 * 1. 结构化错误明细（FailRow 列表），不再依赖拼接字符串（旧 RuoYi importXxx 返回 String 模式）
 * 2. 支持失败行原始数据回传（rowData），前端可下载修正后重导
 * 3. msg 提供摘要文本，便于无明细场景的简化提示
 *
 * @author moyun
 */
@Data
@Schema(description = "通用导入结果")
public class ImportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 总行数（不含表头）
     */
    @Schema(description = "总行数（不含表头）")
    private int totalRows;

    /**
     * 成功条数
     */
    @Schema(description = "成功条数")
    private int successCount;

    /**
     * 失败条数
     */
    @Schema(description = "失败条数")
    private int failCount;

    /**
     * 失败明细列表（按行号升序）
     */
    @Schema(description = "失败明细列表")
    private List<FailRow> failRows = new ArrayList<>();

    /**
     * 摘要消息：如"共 10 条，成功 8 条，失败 2 条"
     */
    @Schema(description = "摘要消息")
    private String msg;

    public ImportResult() {
    }

    /**
     * 构造并自动计算摘要
     */
    public ImportResult(int totalRows, int successCount, int failCount, List<FailRow> failRows) {
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failCount = failCount;
        this.failRows = failRows == null ? Collections.emptyList() : failRows;
        this.msg = String.format("共 %d 条，成功 %d 条，失败 %d 条", totalRows, successCount, failCount);
    }

    /**
     * 快速构建成功结果（无失败行）
     */
    public static ImportResult success(int totalRows) {
        return new ImportResult(totalRows, totalRows, 0, Collections.emptyList());
    }

    /**
     * 失败行明细
     */
    @Data
    @Schema(description = "导入失败行明细")
    public static class FailRow implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Excel 行号（从 1 开始，1 = 第一条数据行，即表头下一行）
         */
        @Schema(description = "Excel 行号（1=第一条数据行）")
        private int rowNo;

        /**
         * 失败原因
         */
        @Schema(description = "失败原因")
        private String reason;

        /**
         * 原始数据（字段名→值），用于失败行 Excel 回导
         */
        @Schema(description = "原始数据（字段名→值）")
        private java.util.Map<String, String> rowData;

        public FailRow() {
        }

        public FailRow(int rowNo, String reason, java.util.Map<String, String> rowData) {
            this.rowNo = rowNo;
            this.reason = reason;
            this.rowData = rowData == null ? Collections.emptyMap() : rowData;
        }
    }
}
