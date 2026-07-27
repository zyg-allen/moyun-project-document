package com.moyun.agent.util;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;

/**
 * 文档解析器工厂
 * 根据文件类型返回对应的解析器
 */
public class DocumentParserFactory {

    /**
     * 根据文件扩展名获取对应的文档解析器
     */
    public static DocumentParser getParser(String fileExtension) {
        String ext = fileExtension.toLowerCase();

        return switch (ext) {
            // PDF 文档
            case "pdf" -> new ApachePdfBoxDocumentParser();

            // Word 文档
            case "doc", "docx" -> new ApachePoiDocumentParser();

            // Excel 表格
            case "xls", "xlsx" -> new ApachePoiDocumentParser();

            // PowerPoint 演示文稿
            case "ppt", "pptx" -> new ApachePoiDocumentParser();

            // 纯文本文件
            case "txt", "md", "csv" -> new TextDocumentParser();

            // 默认使用文本解析器
            default -> new TextDocumentParser();
        };
    }

    /**
     * 判断文件类型是否支持
     */
    public static boolean isSupported(String fileExtension) {
        String ext = fileExtension.toLowerCase();
        return ext.equals("pdf") ||
               ext.equals("doc") || ext.equals("docx") ||
               ext.equals("xls") || ext.equals("xlsx") ||
               ext.equals("ppt") || ext.equals("pptx") ||
               ext.equals("txt") || ext.equals("md") ||
               ext.equals("csv");
    }

    /**
     * 获取文件类型的显示名称
     */
    public static String getFileTypeName(String fileExtension) {
        String ext = fileExtension.toLowerCase();
        return switch (ext) {
            case "pdf" -> "PDF文档";
            case "doc", "docx" -> "Word文档";
            case "xls", "xlsx" -> "Excel表格";
            case "ppt", "pptx" -> "PowerPoint演示";
            case "txt" -> "文本文件";
            case "md" -> "Markdown文档";
            case "csv" -> "CSV表格";
            default -> "未知格式";
        };
    }

    /**
     * 判断是否为 Office 文档（需要特殊处理）
     */
    public static boolean isOfficeDocument(String fileExtension) {
        String ext = fileExtension.toLowerCase();
        return ext.equals("doc") || ext.equals("docx") ||
               ext.equals("xls") || ext.equals("xlsx") ||
               ext.equals("ppt") || ext.equals("pptx");
    }

    /**
     * 判断是否为表格文件
     */
    public static boolean isSpreadsheet(String fileExtension) {
        String ext = fileExtension.toLowerCase();
        return ext.equals("xls") || ext.equals("xlsx") || ext.equals("csv");
    }

    /**
     * 判断是否为演示文稿
     */
    public static boolean isPresentation(String fileExtension) {
        String ext = fileExtension.toLowerCase();
        return ext.equals("ppt") || ext.equals("pptx");
    }
}
