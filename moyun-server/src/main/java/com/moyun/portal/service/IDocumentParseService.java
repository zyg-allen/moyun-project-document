package com.moyun.portal.service;

import com.moyun.portal.domain.query.ChapterSplitRule;
import com.moyun.portal.domain.vo.DocumentParseResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文档解析服务
 *
 * <p>支持 txt/markdown/docx/pdf 格式，自动识别章节</p>
 *
 * @author moyun
 */
public interface IDocumentParseService {

    /**
     * 解析文档并自动分章
     *
     * @param file 上传的文件
     * @param rule 分章规则（null 时用默认规则）
     * @return 解析结果（含元信息 + 章节列表）
     */
    DocumentParseResult parseDocument(MultipartFile file, ChapterSplitRule rule);

    /**
     * 解析文本内容并自动分章（不依赖文件上传）
     *
     * @param text 文本内容
     * @param rule 分章规则
     * @return 解析结果
     */
    DocumentParseResult parseText(String text, ChapterSplitRule rule);

    /**
     * 解析文档为单篇 Markdown（不分章，用于面经/长文场景）
     *
     * @param file 上传的文件
     * @return Markdown 字符串
     */
    String parseToMarkdown(MultipartFile file);
}
