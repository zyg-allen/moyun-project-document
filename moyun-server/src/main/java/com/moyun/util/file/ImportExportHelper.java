package com.moyun.util.file;

import com.moyun.core.base.dto.ImportResult;
import com.moyun.portal.domain.entity.PortalImportTemplateConfig;
import com.moyun.util.string.StringUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 通用导入导出辅助工具（不依赖 @Excel 注解，与 ExcelUtil 互补）
 * <p>
 * 设计目的：ExcelUtil 强依赖 @Excel 注解反射，无法灵活应对"动态模板字段配置"
 * 与"失败行原始数据回导"场景。本类提供轻量级、配置驱动的 Excel 处理能力：
 * <p>
 * 1. {@link #writeDynamicTemplate} 按运营配置生成动态模板（表头+说明+示例行+下拉+数据校验）
 * 2. {@link #readRows} 解析 Excel 为 List<Map<字段名,值>>，保留原始数据便于失败回导
 * 3. {@link #writeFailRows} 导出失败行（原始数据+失败原因列），供运营修正后重导
 *
 * @author moyun
 */
@Slf4j
public class ImportExportHelper {

    private ImportExportHelper() {
    }

    // ==================== 动态模板生成 ====================

    /**
     * 生成动态导入模板并写入 response。
     * <p>
     * 模板结构（自上而下）：
     * 1. 表头行（columnName）
     * 2. 说明行（description，灰色斜体，描述字段含义/必填/示例）
     * 3. 示例行（exampleValue，便于运营理解填写格式）
     * 4. 数据校验：comboValues/dictType → Excel 下拉选择；required → 注释提示
     *
     * @param response  HTTP 响应
     * @param fileName  下载文件名（不含扩展名）
     * @param sheetName 工作表名
     * @param configs   字段配置列表（已按 sort 排序）
     */
    public static void writeDynamicTemplate(HttpServletResponse response,
                                            String fileName,
                                            String sheetName,
                                            List<PortalImportTemplateConfig> configs) throws IOException {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("模板字段配置为空，无法生成模板");
        }
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            wb.setCompressTempFiles(true);
            Sheet sheet = wb.createSheet(sheetName);

            // 样式
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle descStyle = createDescStyle(wb);
            CellStyle exampleStyle = createExampleStyle(wb);

            // 第 1 行：表头
            Row headerRow = sheet.createRow(0);
            // 第 2 行：说明
            Row descRow = sheet.createRow(1);
            // 第 3 行：示例
            Row exampleRow = sheet.createRow(2);

            for (int i = 0; i < configs.size(); i++) {
                PortalImportTemplateConfig c = configs.get(i);
                int col = i;

                // 表头
                Cell hCell = headerRow.createCell(col);
                String headerText = c.getColumnName();
                if (Integer.valueOf(1).equals(c.getRequired())) {
                    headerText += " *"; // 必填标记
                }
                hCell.setCellValue(headerText);
                hCell.setCellStyle(headerStyle);

                // 说明
                Cell dCell = descRow.createCell(col);
                String desc = c.getDescription();
                if (StringUtils.isEmpty(desc)) {
                    desc = Integer.valueOf(1).equals(c.getRequired()) ? "必填" : "可选";
                }
                dCell.setCellValue(desc);
                dCell.setCellStyle(descStyle);

                // 示例
                Cell eCell = exampleRow.createCell(col);
                eCell.setCellValue(c.getExampleValue() == null ? "" : c.getExampleValue());
                eCell.setCellStyle(exampleStyle);

                // 列宽
                int width = c.getColumnWidth() == null ? 20 : Math.max(8, c.getColumnWidth());
                sheet.setColumnWidth(col, width * 256);

                // 下拉校验（comboValues 优先于 dictType）
                if (StringUtils.isNotEmpty(c.getComboValues())) {
                    String[] items = c.getComboValues().split(",");
                    applyDropdown(sheet, col, 3, 500, items);
                }
            }

            // 冻结表头（前 3 行）
            sheet.createFreezePane(0, 3);

            writeToResponse(response, fileName, wb);
        }
    }

    // ==================== Excel 读取（保留原始数据） ====================

    /**
     * 读取 Excel 为行 Map 列表。
     * <p>
     * 与 ExcelUtil.importExcel 不同：
     * - 返回 List<Map<字段名, 字符串值>>，不绑定实体类，避免反射复杂度
     * - 表头必须与 config.columnName 一致才能映射到 fieldName；未匹配的列忽略
     * - 跳过说明行（第 2 行）和示例行（第 3 行），从第 4 行开始读取数据
     *
     * @param is      输入流
     * @param configs 字段配置（用于建立 columnName→fieldName 映射）
     * @return 行数据列表，每行是 fieldName→value 的 Map
     */
    public static List<Map<String, String>> readRows(java.io.InputStream is,
                                                     List<PortalImportTemplateConfig> configs) throws IOException {
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("字段配置为空，无法解析");
        }
        // columnName → fieldName 映射（trim 后比对，容错空格）
        Map<String, String> nameToField = new HashMap<>();
        for (PortalImportTemplateConfig c : configs) {
            if (StringUtils.isNotEmpty(c.getColumnName()) && StringUtils.isNotEmpty(c.getFieldName())) {
                nameToField.put(c.getColumnName().trim(), c.getFieldName());
            }
        }

        List<Map<String, String>> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) return rows;

            // 表头行（第 1 行）
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return rows;

            // 列索引 → fieldName
            Map<Integer, String> colToField = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String headerName = getCellStringValue(cell).trim();
                // 去除必填标记 " *"
                if (headerName.endsWith(" *")) {
                    headerName = headerName.substring(0, headerName.length() - 2).trim();
                }
                String fieldName = nameToField.get(headerName);
                if (fieldName != null) {
                    colToField.put(i, fieldName);
                }
            }

            // 数据从第 4 行起（1=表头,2=说明,3=示例）
            int lastRow = sheet.getLastRowNum();
            for (int r = 3; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                Map<String, String> rowData = new LinkedHashMap<>();
                for (Map.Entry<Integer, String> e : colToField.entrySet()) {
                    Cell cell = row.getCell(e.getKey());
                    rowData.put(e.getValue(), cell == null ? "" : getCellStringValue(cell));
                }
                rows.add(rowData);
            }
        }
        return rows;
    }

    // ==================== 失败行导出 ====================

    /**
     * 导出失败行 Excel（原始数据 + 失败原因列），供运营修正后重导。
     *
     * @param response    HTTP 响应
     * @param fileName    下载文件名（不含扩展名）
     * @param sheetName   工作表名
     * @param configs     字段配置（用于生成表头，与原模板一致）
     * @param failRows    失败明细列表
     */
    public static void writeFailRows(HttpServletResponse response,
                                     String fileName,
                                     String sheetName,
                                     List<PortalImportTemplateConfig> configs,
                                     List<ImportResult.FailRow> failRows) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle = createDataStyle(wb);
            CellStyle failStyle = createFailStyle(wb);

            // 表头：原字段列 + 失败原因 + 原行号
            Row headerRow = sheet.createRow(0);
            int col = 0;
            for (PortalImportTemplateConfig c : configs) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(c.getColumnName());
                cell.setCellStyle(headerStyle);
                int width = c.getColumnWidth() == null ? 20 : Math.max(8, c.getColumnWidth());
                sheet.setColumnWidth(col, width * 256);
                col++;
            }
            Cell reasonCell = headerRow.createCell(col);
            reasonCell.setCellValue("失败原因");
            reasonCell.setCellStyle(headerStyle);
            sheet.setColumnWidth(col, 40 * 256);
            col++;
            Cell rowNoCell = headerRow.createCell(col);
            rowNoCell.setCellValue("原 Excel 行号");
            rowNoCell.setCellStyle(headerStyle);
            sheet.setColumnWidth(col, 14 * 256);

            // 数据行
            if (failRows != null) {
                int rowIdx = 1;
                for (ImportResult.FailRow fr : failRows) {
                    Row row = sheet.createRow(rowIdx++);
                    int cIdx = 0;
                    Map<String, String> data = fr.getRowData() == null ? Collections.emptyMap() : fr.getRowData();
                    for (PortalImportTemplateConfig c : configs) {
                        Cell cell = row.createCell(cIdx++);
                        String val = data.getOrDefault(c.getFieldName(), "");
                        cell.setCellValue(val);
                        cell.setCellStyle(dataStyle);
                    }
                    Cell rCell = row.createCell(cIdx++);
                    rCell.setCellValue(fr.getReason() == null ? "" : fr.getReason());
                    rCell.setCellStyle(failStyle);
                    Cell nCell = row.createCell(cIdx);
                    nCell.setCellValue(fr.getRowNo());
                    nCell.setCellStyle(dataStyle);
                }
            }

            writeToResponse(response, fileName, wb);
        }
    }

    // ==================== 内部样式/工具方法 ====================

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private static CellStyle createDescStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 9);
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        return style;
    }

    private static CellStyle createExampleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setItalic(true);
        style.setFont(font);
        return style;
    }

    private static CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private static CellStyle createFailStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setWrapText(true);
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    private static void applyDropdown(Sheet sheet, int col, int startRow, int endRow, String[] items) {
        if (items == null || items.length == 0) return;
        // Excel 下拉项总字符数限制 255，超长则跳过（避免 POI 抛异常）
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() > 0) sb.append(",");
            sb.append(item);
        }
        if (sb.length() > 255) return;

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(items);
        CellRangeAddressList range = new CellRangeAddressList(startRow, endRow, col, col);
        DataValidation validation = helper.createValidation(constraint, range);
        validation.setSuppressDropDownArrow(true);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private static String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "";
                    }
                }
            default:
                return "";
        }
    }

    private static boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !(cell.getCellType() == CellType.STRING && cell.getStringCellValue().trim().isEmpty())) {
                return false;
            }
        }
        return true;
    }

    private static void writeToResponse(HttpServletResponse response, String fileName, Workbook wb) throws IOException {
        String encoded = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded);
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        try (OutputStream os = response.getOutputStream()) {
            wb.write(os);
            os.flush();
        } finally {
            if (wb instanceof SXSSFWorkbook) {
                ((SXSSFWorkbook) wb).dispose();
            }
        }
    }
}
