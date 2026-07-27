package com.moyun.agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moyun.agent.common.Result;
import com.moyun.agent.dto.KnowledgeLibraryDTO;
import com.moyun.agent.service.KnowledgeLibraryService;
import com.moyun.agent.vo.KnowledgeLibraryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 * 
 * <p>提供知识库的CRUD操作、文档上传和管理等API</p>
 *
 * @author laomao
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge-library")
public class KnowledgeLibraryController {

    @Autowired
    private KnowledgeLibraryService knowledgeLibraryService;

    /**
     * 创建知识库
     */
    @PostMapping
    public Result<Map<String, Object>> createLibrary(@RequestBody KnowledgeLibraryDTO dto) {
        try {
            Long libraryId = knowledgeLibraryService.createLibrary(dto);
            Map<String, Object> result = new HashMap<>();
            result.put("libraryId", libraryId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建知识库失败", e);
            return Result.error("创建知识库失败: " + e.getMessage());
        }
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{id}")
    public Result<Void> updateLibrary(@PathVariable Long id, @RequestBody KnowledgeLibraryDTO dto) {
        try {
            dto.setId(id);
            knowledgeLibraryService.updateLibrary(dto);
            return Result.success(null);
        } catch (Exception e) {
            log.error("更新知识库失败", e);
            return Result.error("更新知识库失败: " + e.getMessage());
        }
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteLibrary(@PathVariable Long id) {
        try {
            knowledgeLibraryService.deleteLibrary(id);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除知识库失败", e);
            return Result.error("删除知识库失败: " + e.getMessage());
        }
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeLibraryVO> getLibraryDetail(@PathVariable Long id) {
        try {
            KnowledgeLibraryVO vo = knowledgeLibraryService.getLibraryDetail(id);
            if (vo == null) {
                return Result.error("知识库不存在");
            }
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取知识库详情失败", e);
            return Result.error("获取知识库详情失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询知识库列表
     */
    @GetMapping("/list")
    public Result<Page<KnowledgeLibraryVO>> listLibraries(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        try {
            Page<KnowledgeLibraryVO> result = knowledgeLibraryService.listLibraries(page, size, keyword, category);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询知识库列表失败", e);
            return Result.error("查询知识库列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有知识库（用于下拉选择）
     */
    @GetMapping("/all")
    public Result<List<KnowledgeLibraryVO>> listAllLibraries() {
        try {
            List<KnowledgeLibraryVO> list = knowledgeLibraryService.listAllLibraries();
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return Result.error("获取知识库列表失败: " + e.getMessage());
        }
    }

    /**
     * 上传文档到知识库
     */
    @PostMapping("/{libraryId}/documents")
    public Result<Map<String, Object>> uploadDocument(
            @PathVariable Long libraryId,
            @RequestParam("file") MultipartFile file) {
        try {
            Long documentId = knowledgeLibraryService.uploadDocument(libraryId, file);
            Map<String, Object> result = new HashMap<>();
            result.put("documentId", documentId);
            result.put("fileName", file.getOriginalFilename());
            return Result.success(result);
        } catch (Exception e) {
            log.error("上传文档失败", e);
            return Result.error("上传文档失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文档到知识库
     */
    @PostMapping("/{libraryId}/documents/batch")
    public Result<Map<String, Object>> uploadDocuments(
            @PathVariable Long libraryId,
            @RequestParam("files") List<MultipartFile> files) {
        try {
            List<Long> documentIds = knowledgeLibraryService.uploadDocuments(libraryId, files);
            Map<String, Object> result = new HashMap<>();
            result.put("documentIds", documentIds);
            result.put("count", documentIds.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量上传文档失败", e);
            return Result.error("批量上传文档失败: " + e.getMessage());
        }
    }

    /**
     * 从知识库删除文档
     */
    @DeleteMapping("/{libraryId}/documents/{documentId}")
    public Result<Void> deleteDocument(
            @PathVariable Long libraryId,
            @PathVariable Long documentId) {
        try {
            knowledgeLibraryService.deleteDocument(libraryId, documentId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            return Result.error("删除文档失败: " + e.getMessage());
        }
    }

    /**
     * 处理知识库中的文档
     */
    @PostMapping("/{libraryId}/process")
    public Result<Void> processDocuments(@PathVariable Long libraryId) {
        try {
            knowledgeLibraryService.processDocuments(libraryId);
            return Result.success(null);
        } catch (Exception e) {
            log.error("处理文档失败", e);
            return Result.error("处理文档失败: " + e.getMessage());
        }
    }

    /**
     * 知识库检索测试 - 检索知识库下所有文档
     */
    @PostMapping("/{libraryId}/test-retrieval")
    public Result<List<com.moyun.agent.dto.RetrievalTestResult>> testRetrieval(
            @PathVariable Long libraryId,
            @RequestBody com.moyun.agent.dto.RetrievalTestRequest request) {
        try {
            log.info("知识库检索测试 - libraryId: {}, query: {}", libraryId, request.getQuery());
            List<com.moyun.agent.dto.RetrievalTestResult> results =
                knowledgeLibraryService.testRetrieval(libraryId, request);
            return Result.success("检索成功", results);
        } catch (Exception e) {
            log.error("知识库检索测试失败 - libraryId: {}", libraryId, e);
            return Result.error("检索失败: " + e.getMessage());
        }
    }
}
