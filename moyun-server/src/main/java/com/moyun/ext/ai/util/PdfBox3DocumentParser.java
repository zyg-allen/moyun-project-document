package com.moyun.ext.ai.util;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 PDFBox 3.x 的文档解析器（替代 langchain4j 的 ApachePdfBoxDocumentParser）。
 *
 * 背景：
 * - moyun-server 主项目使用 PDFBox 3.0.3。
 * - langchain4j-document-parser-apache-pdfbox 1.0.0-beta3 编译时依赖 PDFBox 2.0.32，
 *   内部调用 PDDocument.load(InputStream) 静态方法；该方法在 PDFBox 3.x 中已被移除，
 *   改为 Loader.loadPDF(InputStream)。
 * - Maven 依赖仲裁后运行时仅加载 3.0.3，直接使用 ApachePdfBoxDocumentParser
 *   会抛 NoSuchMethodError。
 *
 * 实现：
 * - 使用 PDFBox 3.x 的 Loader.loadPDF(InputStream) 加载 PDF。
 * - 复用 langchain4j 的 Document/Metadata 模型，接口与 DocumentParser 兼容。
 *
 * 注意：使用后必须自行关闭返回的 PDDocument（try-with-resources）。
 */
public class PdfBox3DocumentParser implements DocumentParser {

    @Override
    public Document parse(InputStream inputStream) {
        try (PDDocument pdfDocument = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            Metadata metadata = new Metadata();
            // 保留与 ApachePdfBoxDocumentParser 一致的元数据字段名
            metadata.put("page_count", String.valueOf(pdfDocument.getNumberOfPages()));

            return Document.from(text, metadata);
        } catch (IOException e) {
            throw new RuntimeException("PDF 解析失败：" + e.getMessage(), e);
        }
    }
}
