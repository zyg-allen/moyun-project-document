package com.moyun.ext.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.dto.KnowledgeLibraryDTO;
import com.moyun.ext.ai.service.KnowledgeLibraryService;
import com.moyun.ext.ai.vo.KnowledgeLibraryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cms/ai/knowledge-library")
public class KnowledgeLibraryController {

    @Autowired
    private KnowledgeLibraryService knowledgeLibraryService;

    @PostMapping
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:add')")
    public AjaxResult createLibrary(@RequestBody KnowledgeLibraryDTO dto) {
        try {
            Long libraryId = knowledgeLibraryService.createLibrary(dto);
            Map<String, Object> result = new HashMap<>();
            result.put("libraryId", libraryId);
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("创建知识库失败", e);
            return AjaxResult.error("创建知识库失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:edit')")
    public AjaxResult updateLibrary(@PathVariable Long id, @RequestBody KnowledgeLibraryDTO dto) {
        try {
            dto.setId(id);
            knowledgeLibraryService.updateLibrary(dto);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("更新知识库失败", e);
            return AjaxResult.error("更新知识库失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:remove')")
    public AjaxResult deleteLibrary(@PathVariable Long id) {
        try {
            knowledgeLibraryService.deleteLibrary(id);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            return AjaxResult.error("删除知识库失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:query')")
    public AjaxResult getLibraryDetail(@PathVariable Long id) {
        try {
            KnowledgeLibraryVO vo = knowledgeLibraryService.getLibraryDetail(id);
            if (vo == null) {
                return AjaxResult.error("知识库不存在");
            }
            return AjaxResult.success(vo);
        } catch (Exception e) {
            log.error("获取知识库详情失败", e);
            return AjaxResult.error("获取知识库详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:list')")
    public AjaxResult listLibraries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        try {
            Page<KnowledgeLibraryVO> result = knowledgeLibraryService.listLibraries(page, size, keyword, category);
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("查询知识库列表失败", e);
            return AjaxResult.error("查询知识库列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:list')")
    public AjaxResult listAllLibraries() {
        try {
            List<KnowledgeLibraryVO> list = knowledgeLibraryService.listAllLibraries();
            return AjaxResult.success(list);
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return AjaxResult.error("获取知识库列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/{libraryId}/documents")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:add')")
    public AjaxResult uploadDocument(
            @PathVariable Long libraryId,
            @RequestParam("file") MultipartFile file) {
        try {
            Long documentId = knowledgeLibraryService.uploadDocument(libraryId, file);
            Map<String, Object> result = new HashMap<>();
            result.put("documentId", documentId);
            result.put("fileName", file.getOriginalFilename());
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("上传文档失败", e);
            return AjaxResult.error("上传文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/{libraryId}/documents/batch")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:add')")
    public AjaxResult uploadDocuments(
            @PathVariable Long libraryId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<Long> documentIds = knowledgeLibraryService.uploadDocuments(libraryId, files);
            Map<String, Object> result = new HashMap<>();
            result.put("documentIds", documentIds);
            result.put("count", documentIds.size());
            return AjaxResult.success(result);
        } catch (Exception e) {
            log.error("批量上传文档失败", e);
            return AjaxResult.error("批量上传文档失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{libraryId}/documents/{documentId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:remove')")
    public AjaxResult deleteDocument(
            @PathVariable Long libraryId,
            @PathVariable Long documentId) {
        try {
            knowledgeLibraryService.deleteDocument(libraryId, documentId);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return AjaxResult.error("删除文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/{libraryId}/process")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:process')")
    public AjaxResult processDocuments(@PathVariable Long libraryId) {
        try {
            knowledgeLibraryService.processDocuments(libraryId);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("处理文档失败", e);
            return AjaxResult.error("处理文档失败: " + e.getMessage());
        }
    }

    @PostMapping("/{libraryId}/test-retrieval")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-library:query')")
    public AjaxResult testRetrieval(
            @PathVariable Long libraryId,
            @RequestBody com.moyun.ext.ai.dto.RetrievalTestRequest request) {
        try {
            log.info("知识库检索测试 - libraryId: {}, query: {}", libraryId, request.getQuery());
            List<com.moyun.ext.ai.dto.RetrievalTestResult> results =
                knowledgeLibraryService.testRetrieval(libraryId, request);
            return AjaxResult.success("检索成功", results);
        } catch (Exception e) {
            log.error("知识库检索测试失败 - libraryId: {}", libraryId, e);
            return AjaxResult.error("检索失败: " + e.getMessage());
        }
    }
}
