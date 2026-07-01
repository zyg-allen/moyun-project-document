package com.moyun.portal.controller;

import com.moyun.portal.domain.query.ChapterSplitRule;
import com.moyun.portal.domain.vo.DocumentParseResult;
import com.moyun.portal.service.IDocumentParseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moyun.core.base.AjaxResult;
import com.moyun.core.base.BaseController;

import java.util.Map;

/**
 * 文档导入Controller（后台）
 *
 * <p>提供文档解析接口，用于章节导入向导的"解析预览"步骤。
 * 实际入库走 {@link PortalBookChapterAdminController#batchImport}。</p>
 *
 * @author moyun
 */
@Tag(name = "读书空间-文档导入", description = "文档解析与导入接口")
@RestController
@RequestMapping("/portal/admin/document")
public class DocumentImportController extends BaseController {

    @Autowired
    private IDocumentParseService documentParseService;

    @Operation(summary = "上传文档并解析（不入库，仅返回解析结果供预览）")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:add')")
    @PostMapping("/parse")
    public AjaxResult parse(@RequestParam("file") MultipartFile file,
                            ChapterSplitRule rule) {
        if (file == null || file.isEmpty()) {
            return error("文件不能为空");
        }
        DocumentParseResult result = documentParseService.parseDocument(file, rule);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return success(result);
        }
        return error(result.getErrorMsg());
    }

    @Operation(summary = "解析文本内容（不依赖文件上传，用于直接粘贴文本）")
    @PreAuthorize("@ss.hasPermi('portal:bookChapter:add')")
    @PostMapping("/parse-text")
    public AjaxResult parseText(@RequestBody Map<String, Object> body) {
        String text = body.get("text") == null ? null : String.valueOf(body.get("text"));
        ChapterSplitRule rule = new ChapterSplitRule();
        Object mode = body.get("mode");
        if (mode != null) rule.setMode(String.valueOf(mode));
        Object regex = body.get("regex");
        if (regex != null) rule.setRegex(String.valueOf(regex));
        Object headingLevel = body.get("headingLevel");
        if (headingLevel != null) rule.setHeadingLevel(String.valueOf(headingLevel));
        Object fixedWordCount = body.get("fixedWordCount");
        if (fixedWordCount != null) rule.setFixedWordCount(Integer.parseInt(String.valueOf(fixedWordCount)));
        // 补全分卷识别与短章合并阈值（与 parse 接口保持一致）
        Object detectVolume = body.get("detectVolume");
        if (detectVolume != null) rule.setDetectVolume(Boolean.valueOf(String.valueOf(detectVolume)));
        Object minChapterWords = body.get("minChapterWords");
        if (minChapterWords != null) rule.setMinChapterWords(Integer.parseInt(String.valueOf(minChapterWords)));

        DocumentParseResult result = documentParseService.parseText(text, rule);
        if (Boolean.TRUE.equals(result.getSuccess())) {
            return success(result);
        }
        return error(result.getErrorMsg());
    }

    @Operation(summary = "上传文档转为 Markdown（用于面经/长文录入辅助）")
    @PreAuthorize("@ss.hasPermi('cms:experience:add') or @ss.hasPermi('cms:article:add')")
    @PostMapping("/to-markdown")
    public AjaxResult toMarkdown(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return error("文件不能为空");
        }
        try {
            String markdown = documentParseService.parseToMarkdown(file);
            return success(markdown);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }
}
