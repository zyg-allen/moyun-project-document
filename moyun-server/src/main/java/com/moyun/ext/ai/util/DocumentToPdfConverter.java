package com.moyun.ext.ai.util;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.*;
import java.nio.file.Files;

/**
 * 文档转 PDF 工具类
 *
 * <p>支持多种转换方式：</p>
 * <ul>
 * <li>JodConverter + LibreOffice - 格式保真度最高，支持 .doc/.docx/.ppt/.pptx/.xls/.xlsx</li>
 * <li>POI + iText - 备用方案</li>
 * </ul>
 */
@Slf4j
@Component
public class DocumentToPdfConverter {

    @Autowired(required = false)
    private OfficeManager officeManager;

    private static OfficeManager staticOfficeManager;

    @PostConstruct
    public void init() {
        staticOfficeManager = this.officeManager;
    }

    /**
     * LibreOffice 支持的文件格式
     */
    private static final java.util.Set<String> LIBREOFFICE_SUPPORTED_TYPES = java.util.Set.of(
            "doc", "docx", // Word
            "xls", "xlsx", // Excel
            "ppt", "pptx", // PowerPoint
            "odt", "ods", "odp", // OpenDocument
            "rtf" // Rich Text Format
    );

    /**
     * 检查 LibreOffice 是否可用
     */
    public static boolean isLibreOfficeAvailable() {
        return staticOfficeManager != null && staticOfficeManager.isRunning();
    }

    /**
     * 检查文件类型是否支持 LibreOffice 转换
     */
    public static boolean isLibreOfficeSupportedType(String fileType) {
        return LIBREOFFICE_SUPPORTED_TYPES.contains(fileType.toLowerCase());
    }

    /**
     * 将文档转换为 PDF
     *
     * <p>转换策略：</p>
     * <ol>
     * <li>如果 LibreOffice 可用且支持该格式，优先使用 LibreOffice（格式保真度最高）</li>
     * <li>LibreOffice 不可用或转换失败时，回退到 Java 方案</li>
     * </ol>
     *
     * @param sourceFile 源文件
     * @param targetPdfFile 目标 PDF 文件
     * @param fileType 文件类型
     * @return 是否转换成功
     */
    public static boolean convertToPdf(File sourceFile, File targetPdfFile, String fileType) {
        try {
            String type = fileType.toLowerCase();
            log.info("========== 开始文档转PDF ==========");
            log.info("源文件: {}, 大小: {} bytes", sourceFile.getName(), sourceFile.length());
            log.info("文件类型: {}", type);
            log.info("目标PDF: {}", targetPdfFile.getAbsolutePath());

            // PDF 不需要转换，直接返回成功
            if ("pdf".equals(type)) {
                log.info("✅ 文件已是PDF格式，无需转换");
                if (!sourceFile.getAbsolutePath().equals(targetPdfFile.getAbsolutePath())) {
                    Files.copy(sourceFile.toPath(), targetPdfFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            }

            boolean libreOfficeAvailable = isLibreOfficeAvailable();
            boolean supportedType = isLibreOfficeSupportedType(type);
            log.info("LibreOffice服务可用: {}", libreOfficeAvailable);
            log.info("文件类型支持LibreOffice: {}", supportedType);

            // 优先使用 LibreOffice 转换（如果可用且支持该格式）
            if (libreOfficeAvailable && supportedType) {
                log.info("🚀 使用 LibreOffice 本地转换: {} ({})", sourceFile.getName(), type);
                if (convertWithLibreOffice(sourceFile, targetPdfFile)) {
                    log.info("✅ LibreOffice转换成功");
                    log.info("========== LibreOffice转换完成 ==========");
                    return true;
                }
                log.warn("⚠️ LibreOffice 转换失败，尝试 Java 备用方案");
            } else {
                if (!libreOfficeAvailable) {
                    log.warn("⚠️ LibreOffice服务不可用，使用Java备用方案");
                } else {
                    log.warn("⚠️ 文件类型 {} 不支持LibreOffice转换，使用Java备用方案", type);
                }
            }

            // Java 备用方案
            log.info("🔧 使用Java备用方案转换: {}", type);
            boolean success = false;
            switch (type) {
                case "docx":
                    success = convertDocxToSimplePdf(sourceFile, targetPdfFile);
                    break;
                case "xlsx":
                    success = convertXlsxToPdf(sourceFile, targetPdfFile);
                    break;
                case "xls":
                    success = convertXlsToPdf(sourceFile, targetPdfFile);
                    break;
                case "txt":
                case "md":
                case "csv":
                    success = convertTextToPdf(sourceFile, targetPdfFile);
                    break;
                default:
                    log.warn("❌ 不支持的文件类型: {}", fileType);
                    return false;
            }

            if (success) {
                log.info("✅ Java备用方案转换成功");
            } else {
                log.error("❌ Java备用方案转换失败");
            }
            log.info("========== 文档转PDF完成 ==========");
            return success;

        } catch (Exception e) {
            log.error("❌ 文档转 PDF 失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 使用 LibreOffice 本地转换文档为 PDF
     */
    private static boolean convertWithLibreOffice(File sourceFile, File targetPdfFile) {
        if (staticOfficeManager == null || !staticOfficeManager.isRunning()) {
            log.warn("❌ LibreOffice服务不可用");
            return false;
        }
        try {
            log.info("========== LibreOffice 本地转换 ==========");
            log.info("📄 源文件: {}, 大小: {} bytes", sourceFile.getName(), sourceFile.length());
            long startTime = System.currentTimeMillis();

            // 使用 JodConverter 进行转换
            LocalConverter.builder()
                    .officeManager(staticOfficeManager)
                    .build()
                    .convert(sourceFile)
                    .to(targetPdfFile)
                    .execute();

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("✅ LibreOffice本地转换成功");
            log.info("📄 PDF文件: {}", targetPdfFile.getAbsolutePath());
            log.info("📊 PDF大小: {} bytes", targetPdfFile.length());
            log.info("⏱️ 转换耗时: {}ms", totalTime);
            log.info("========== LibreOffice转换完成 ==========");
            return true;
        } catch (Exception e) {
            log.error("❌ LibreOffice本地转换异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取一个支持中文的 BaseFont 对象。
     * 会按顺序尝试多种字体，确保在不同环境下都能找到可用字体。
     *
     * @return 可用的 BaseFont 对象，如果所有字体都加载失败，则返回 null
     */
    private static BaseFont getChineseBaseFont() {
        // 1. 尝试使用 iText 内置的字体（推荐，跨平台兼容性最好）
        try {
            return BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.warn("⚠️ 无法加载 iText 内置字体 STSongStd-Light，将尝试系统字体。");
        }

        // 2. 尝试常见的系统中文字体路径
        String[] fontPaths = {
                "c:/windows/fonts/simsun.ttc,0", // Windows 宋体
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc", // Linux (Ubuntu/Debian) 文泉驿正黑
                "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc", // Linux (Ubuntu/Debian) 文泉驿微米黑
                "/usr/share/fonts/chinese/TrueType/song.ttf",     // 其他 Linux 发行版可能的路径
                "/System/Library/Fonts/PingFang.ttc",             // macOS 苹方
                "/System/Library/Fonts/STHeiti Light.ttc"         // macOS 华文黑体
        };

        for (String fontPath : fontPaths) {
            try {
                return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception e) {
                // 忽略，继续尝试下一个字体
            }
        }

        // 3. 如果所有中文字体都失败，最后回退到 Helvetica（不支持中文，但至少不会崩溃）
        log.error("❌ 所有中文字体均加载失败，将使用不支持中文的默认字体，可能导致乱码。");
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            log.error("❌ 连默认的 Helvetica 字体都无法加载，PDF生成将失败。", e);
            return null;
        }
    }

    /**
     * DOCX转PDF：提取纯文本转PDF
     */
    private static boolean convertDocxToSimplePdf(File sourceFile, File targetPdfFile) {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             XWPFDocument document = new XWPFDocument(fis)) {
            log.info("📝 开始提取DOCX文本内容...");
            // 提取所有段落文本
            StringBuilder textBuilder = new StringBuilder();
            int paraCount = 0;
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph para : document.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.trim().isEmpty()) {
                    textBuilder.append(text).append("\n\n");
                    paraCount++;
                }
            }
            // 提取表格内容
            for (org.apache.poi.xwpf.usermodel.XWPFTable table : document.getTables()) {
                for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
                    for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText != null && !cellText.trim().isEmpty()) {
                            textBuilder.append(cellText).append("\t");
                        }
                    }
                    textBuilder.append("\n");
                }
                textBuilder.append("\n");
            }
            String extractedText = textBuilder.toString();
            log.info("📊 提取完成：共{}个段落，文本长度{}字符", paraCount, extractedText.length());
            if (extractedText.isEmpty()) {
                log.warn("⚠️ 文档为空或无法提取文本");
                return false;
            }

            // 使用iText创建PDF
            Document pdfDoc = new Document(PageSize.A4);
            pdfDoc.setMargins(50, 50, 50, 50);
            PdfWriter.getInstance(pdfDoc, new FileOutputStream(targetPdfFile));
            pdfDoc.open();

            // 【修改点】使用新的字体加载方法
            BaseFont bfChinese = getChineseBaseFont();
            if (bfChinese == null) return false; // 字体加载失败，直接返回
            Font font = new Font(bfChinese, 12, Font.NORMAL);

            // 分段添加文本
            String[] paragraphs = extractedText.split("\n");
            for (String para : paragraphs) {
                if (!para.trim().isEmpty()) {
                    Paragraph pdfPara = new Paragraph(para + "\n", font);
                    pdfPara.setSpacingAfter(5f);
                    pdfDoc.add(pdfPara);
                }
            }
            pdfDoc.close();
            log.info("✅ DOCX转PDF成功");
            return true;
        } catch (Exception e) {
            log.error("❌ DOCX转PDF失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 转换 XLSX 到 PDF
     */
    private static boolean convertXlsxToPdf(File sourceFile, File targetPdfFile) {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            return convertWorkbookToPdf(workbook, targetPdfFile);
        } catch (Exception e) {
            log.error("XLSX 转 PDF 失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 转换 XLS 到 PDF
     */
    private static boolean convertXlsToPdf(File sourceFile, File targetPdfFile) {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
            return convertWorkbookToPdf(workbook, targetPdfFile);
        } catch (Exception e) {
            log.error("XLS 转 PDF 失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 通用的 Workbook 转 PDF 方法
     */
    private static boolean convertWorkbookToPdf(Workbook workbook, File targetPdfFile) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(targetPdfFile));
            document.open();

            // 【修改点】使用新的字体加载方法
            BaseFont bfChinese = getChineseBaseFont();
            if (bfChinese == null) return false; // 字体加载失败，直接返回
            Font font = new Font(bfChinese, 10, Font.NORMAL);

            // 遍历所有工作表
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (i > 0) {
                    document.newPage();
                }
                Paragraph title = new Paragraph(sheet.getSheetName(), new Font(bfChinese, 14, Font.BOLD));
                title.setSpacingAfter(10);
                document.add(title);

                PdfPTable table = new PdfPTable(getMaxColumns(sheet));
                table.setWidthPercentage(100);
                for (Row row : sheet) {
                    for (int j = 0; j < getMaxColumns(sheet); j++) {
                        Cell cell = row.getCell(j);
                        String cellValue = getCellValue(cell);
                        com.itextpdf.text.pdf.PdfPCell pdfCell = new com.itextpdf.text.pdf.PdfPCell(new Phrase(cellValue, font));
                        table.addCell(pdfCell);
                    }
                }
                document.add(table);
            }
            document.close();
            log.info("Excel 转 PDF 成功");
            return true;
        } catch (Exception e) {
            log.error("Excel 转 PDF 失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取工作表的最大列数
     */
    private static int getMaxColumns(Sheet sheet) {
        int maxColumns = 0;
        for (Row row : sheet) {
            if (row.getLastCellNum() > maxColumns) {
                maxColumns = row.getLastCellNum();
            }
        }
        return maxColumns > 0 ? maxColumns : 1;
    }

    /**
     * 获取单元格的值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * 转换文本文件到 PDF
     */
    private static boolean convertTextToPdf(File sourceFile, File targetPdfFile) {
        try {
            String content = new String(Files.readAllBytes(sourceFile.toPath()), "UTF-8");
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(targetPdfFile));
            document.open();

            // 【修改点】使用新的字体加载方法
            BaseFont bfChinese = getChineseBaseFont();
            if (bfChinese == null) return false; // 字体加载失败，直接返回
            Font font = new Font(bfChinese, 12, Font.NORMAL);

            String[] lines = content.split("\n");
            for (String line : lines) {
                Paragraph para = new Paragraph(line, font);
                document.add(para);
            }
            document.close();
            log.info("文本文件转 PDF 成功");
            return true;
        } catch (Exception e) {
            log.error("文本文件转 PDF 失败: {}", e.getMessage(), e);
            return false;
        }
    }
}