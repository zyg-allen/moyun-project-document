package com.moyun.portal.service.impl;

import com.moyun.portal.domain.query.ChapterSplitRule;
import com.moyun.portal.domain.vo.DocumentParseResult;
import com.moyun.portal.domain.vo.ParsedChapter;
import com.moyun.portal.service.IDocumentParseService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 通用文档解析服务实现
 *
 * <p>支持 txt/markdown/docx/pdf 四种格式</p>
 *
 * @author moyun
 */
@Service
public class DocumentParseServiceImpl implements IDocumentParseService {

    /** 文件大小上限：50MB */
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    @Autowired
    private ChapterSplitter chapterSplitter;

    @Override
    public DocumentParseResult parseDocument(MultipartFile file, ChapterSplitRule rule) {
        if (file == null || file.isEmpty()) {
            return DocumentParseResult.fail(null, "文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return DocumentParseResult.fail(null, "文件过大，限制 50MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return DocumentParseResult.fail(null, "无法识别文件名");
        }
        String ext = getExtension(filename);
        String sourceFormat = ext;

        try {
            String text;
            String author = null;
            switch (ext) {
                case "txt":
                case "md":
                case "markdown":
                    text = readText(file);
                    break;
                case "docx":
                    DocxParseResult dr = readDocx(file);
                    text = dr.text;
                    author = dr.author;
                    break;
                case "pdf":
                    text = readPdf(file);
                    break;
                default:
                    return DocumentParseResult.fail(ext, "不支持的文件格式：" + ext + "（仅支持 txt/markdown/docx/pdf）");
            }

            // Markdown 文件直接作为 Markdown，否则按章节切
            if ("md".equals(ext) || "markdown".equals(ext)) {
                // Markdown 文件也走分章逻辑（按标题识别）
            }

            if (text == null || text.trim().isEmpty()) {
                return DocumentParseResult.fail(ext, "文档内容为空");
            }

            List<ParsedChapter> chapters = chapterSplitter.split(text, rule);
            if (chapters == null || chapters.isEmpty()) {
                return DocumentParseResult.fail(ext, "未识别到章节，请调整分章规则或检查文档格式");
            }

            // 自动识别标题
            String title = extractTitle(filename, chapters);
            return DocumentParseResult.ok(title, author, sourceFormat, chapters);

        } catch (Exception e) {
            return DocumentParseResult.fail(ext, "解析失败：" + e.getMessage());
        }
    }

    @Override
    public DocumentParseResult parseText(String text, ChapterSplitRule rule) {
        if (text == null || text.trim().isEmpty()) {
            return DocumentParseResult.fail("text", "文本内容为空");
        }
        try {
            List<ParsedChapter> chapters = chapterSplitter.split(text, rule);
            if (chapters == null || chapters.isEmpty()) {
                return DocumentParseResult.fail("text", "未识别到章节");
            }
            String title = extractTitle(null, chapters);
            return DocumentParseResult.ok(title, null, "text", chapters);
        } catch (Exception e) {
            return DocumentParseResult.fail("text", "解析失败：" + e.getMessage());
        }
    }

    @Override
    public String parseToMarkdown(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件过大，限制 50MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) return null;
        String ext = getExtension(filename);

        try {
            switch (ext) {
                case "txt":
                    return readText(file);
                case "md":
                case "markdown":
                    return readText(file);
                case "docx":
                    return readDocx(file).text;
                case "pdf":
                    return readPdf(file);
                default:
                    throw new IllegalArgumentException("不支持的文件格式：" + ext);
            }
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败：" + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------
    // 各格式解析实现
    // -------------------------------------------------------

    /**
     * 读取 TXT/MD 文件，自动识别编码
     */
    private String readText(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        // 检测 BOM
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }
        // 尝试 UTF-8，失败则用 GBK
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(bytes, Charset.forName("GBK"));
        }
    }

    /**
     * 读取 DOCX 文件（POI）
     */
    private DocxParseResult readDocx(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            // 抽取作者信息
            String author = null;
            try {
                if (doc.getProperties() != null
                        && doc.getProperties().getCoreProperties() != null) {
                    author = doc.getProperties().getCoreProperties().getCreator();
                }
            } catch (Exception ignored) {
            }
            return new DocxParseResult(text, author);
        }
    }

    /**
     * 读取 PDF 文件（pdfbox）
     */
    private String readPdf(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            // 检测加密
            if (doc.isEncrypted()) {
                throw new IllegalStateException("PDF 文件已加密，请提供未加密的文档");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            // 设置按页输出，页间用换行分隔
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalStateException("PDF 无文本层（可能是扫描件），请提供文本版 PDF");
            }
            return text;
        }
    }

    // -------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).toLowerCase();
    }

    /**
     * 提取文档标题：优先取第一个章节前的内容，否则用文件名
     */
    private String extractTitle(String filename, List<ParsedChapter> chapters) {
        if (chapters != null && !chapters.isEmpty()) {
            ParsedChapter first = chapters.get(0);
            if (first.getChapterNo() != null && first.getChapterNo() == 0) {
                // 序章
                return first.getTitle();
            }
        }
        if (filename != null) {
            int idx = filename.lastIndexOf('.');
            return idx > 0 ? filename.substring(0, idx) : filename;
        }
        return "未命名";
    }

    /**
     * docx 解析结果
     */
    private static class DocxParseResult {
        final String text;
        final String author;

        DocxParseResult(String text, String author) {
            this.text = text;
            this.author = author;
        }
    }
}
