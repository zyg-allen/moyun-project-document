package com.moyun.ext.ai.controller;

import com.moyun.core.base.AjaxResult;
import com.moyun.ext.ai.vo.KnowledgeBaseVO;
import com.moyun.ext.ai.common.ListResponse;
import com.moyun.ext.ai.dto.*;
import com.moyun.ext.ai.entity.DocumentSegment;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.service.DocumentSegmentService;
import com.moyun.ext.ai.service.KnowledgeBaseService;
import com.moyun.ext.ai.service.KnowledgeConfigService;
import com.moyun.ext.ai.service.MinioService;
import com.moyun.ext.ai.entity.KnowledgeConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.List;

@Tag(name = "知识库管理")
@RestController
@RequestMapping("/cms/ai/knowledge-base")
@Slf4j
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentSegmentService documentSegmentService;

    @Autowired
    private KnowledgeConfigService knowledgeConfigService;

    @Autowired
    private MinioService minioService;

    @Autowired
    @Qualifier("knowledgeProcessExecutor")
    private Executor knowledgeProcessExecutor;

    @Operation(summary = "上传知识库文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:add')")
    public AjaxResult uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "libraryId", required = false) Long libraryId) {
        try {
            if (file.isEmpty()) {
                return AjaxResult.error("文件不能为空");
            }

            String filename = file.getOriginalFilename();
            if (filename == null) {
                return AjaxResult.error("文件名不能为空");
            }

            String lowerFilename = filename.toLowerCase();
            boolean isSupported = lowerFilename.endsWith(".pdf") ||
                                 lowerFilename.endsWith(".txt") ||
                                 lowerFilename.endsWith(".md") ||
                                 lowerFilename.endsWith(".doc") ||
                                 lowerFilename.endsWith(".docx") ||
                                 lowerFilename.endsWith(".xls") ||
                                 lowerFilename.endsWith(".xlsx") ||
                                 lowerFilename.endsWith(".ppt") ||
                                 lowerFilename.endsWith(".pptx") ||
                                 lowerFilename.endsWith(".csv");

            if (!isSupported) {
                return AjaxResult.error(
                    "支持的格式：PDF、Word(doc/docx)、Excel(xls/xlsx)、PowerPoint(ppt/pptx)、TXT、MD、CSV"
                );
            }

            if (lowerFilename.endsWith(".doc") && !lowerFilename.endsWith(".docx")) {
                log.info("📄 检测到 .doc 文件: {}，将使用 LibreOffice 进行转换", filename);
            }

            KnowledgeBase knowledge = knowledgeBaseService.uploadFileOnly(file);
            
            if (libraryId != null) {
                knowledge.setLibraryId(libraryId);
                knowledgeBaseService.updateById(knowledge);
                log.info("📚 文档已关联到知识库: libraryId={}, docId={}", libraryId, knowledge.getId());
            }

            List<com.moyun.ext.ai.entity.KnowledgeConfigTemplate> recommendedTemplates =
                knowledgeConfigService.getRecommendedTemplates(knowledge.getFileType());

            KnowledgeUploadResponse response = new KnowledgeUploadResponse();
            response.setKnowledgeId(knowledge.getId());
            response.setFileName(knowledge.getFileName());
            response.setFileType(knowledge.getFileType());
            response.setFileSize(knowledge.getFileSize());
            response.setProcessingStatus(knowledge.getProcessingStatus());
            response.setNeedsConfiguration(true);
            response.setRecommendedTemplates(convertTemplates(recommendedTemplates));
            response.setNextStep("请选择配置模板或自定义配置，然后调用 /cms/ai/knowledge-base/configure 接口");

            log.info("文件上传成功 - ID: {}, 文件名: {}", knowledge.getId(), knowledge.getFileName());
            return AjaxResult.success("文件上传成功，请配置处理参数", response);

        } catch (Exception e) {
            log.error("上传文件失败", e);
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }

    private List<KnowledgeUploadResponse.ConfigTemplateInfo> convertTemplates(
            List<com.moyun.ext.ai.entity.KnowledgeConfigTemplate> templates) {
        if (templates == null) {
            return null;
        }
        return templates.stream().map(t -> {
            KnowledgeUploadResponse.ConfigTemplateInfo info = new KnowledgeUploadResponse.ConfigTemplateInfo();
            info.setTemplateId(t.getId());
            info.setTemplateName(t.getTemplateName());
            info.setTemplateDesc(t.getTemplateDesc());
            info.setTemplateType(t.getTemplateType());
            info.setIsRecommended(t.getIsSystem() != null ? t.getIsSystem() : false);
            return info;
        }).collect(java.util.stream.Collectors.toList());
    }

    private List<KnowledgeTemplateResponse.Template> convertToTemplateResponse(
            List<com.moyun.ext.ai.entity.KnowledgeConfigTemplate> templates) {
        if (templates == null) {
            return null;
        }
        return templates.stream().map(t -> {
            KnowledgeTemplateResponse.Template template = new KnowledgeTemplateResponse.Template();
            template.setId(t.getId().toString());
            template.setTemplateName(t.getTemplateName());
            template.setTemplateDesc(t.getTemplateDesc());
            template.setIsRecommended(t.getIsSystem() != null ? t.getIsSystem() : false);
            template.setFileTypes(java.util.Arrays.asList(t.getTemplateType().split(",")));
            try {
                if (t.getConfigJson() != null && !t.getConfigJson().isEmpty()) {
                    java.util.Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(t.getConfigJson(), new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>(){});
                    template.setConfig(config);
                }
            } catch (Exception e) {
                log.warn("解析模板配置JSON失败 - ID: {}", t.getId(), e);
            }
            return template;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Operation(summary = "获取所有知识库列表")
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:list')")
    public AjaxResult listAll() {
        try {
            List<KnowledgeBaseVO> list = knowledgeBaseService.listAllWithFormat();
            return AjaxResult.success(new ListResponse<>(list));
        } catch (Exception e) {
            log.error("获取知识库列表失败", e);
            return AjaxResult.error("获取列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取单个知识库详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public AjaxResult getById(@PathVariable("id") Long id) {
        try {
            KnowledgeBase knowledge = knowledgeBaseService.getById(id);
            if (knowledge == null) {
                return AjaxResult.error("知识库不存在");
            }

            List<KnowledgeBaseVO> list = knowledgeBaseService.listAllWithFormat();
            KnowledgeBaseVO vo = list.stream()
                .filter(k -> k.getId().equals(id))
                .findFirst()
                .orElse(null);

            return AjaxResult.success(vo != null ? vo : null);
        } catch (Exception e) {
            log.error("获取知识库详情失败 - ID: {}", id, e);
            return AjaxResult.error("获取详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:remove')")
    public AjaxResult delete(@PathVariable("id") Long id) {
        try {
            boolean success = knowledgeBaseService.deleteKnowledge(id);
            if (success) {
                log.info("删除知识库成功 - ID: {}", id);
                return AjaxResult.success("删除成功");
            } else {
                return AjaxResult.error("删除失败，记录不存在");
            }
        } catch (Exception e) {
            log.error("删除知识库失败 - ID: {}", id, e);
            return AjaxResult.error("删除失败: " + e.getMessage());
        }
    }

    @Operation(summary = "重新处理文件")
    @PostMapping("/reprocess/{id}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:edit')")
    public AjaxResult reprocess(@PathVariable("id") Long id) {
        try {
            boolean success = knowledgeBaseService.reprocessFile(id);
            if (success) {
                log.info("重新处理文件成功 - ID: {}", id);
                return AjaxResult.success("重新处理成功");
            } else {
                return AjaxResult.error("重新处理失败");
            }
        } catch (Exception e) {
            log.error("重新处理文件失败 - ID: {}", id, e);
            return AjaxResult.error("重新处理失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取文档分片列表")
    @GetMapping("/{id}/segments")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public AjaxResult getSegments(@PathVariable("id") Long id) {
        try {
            log.info("📋 获取知识库分片列表, ID: {}", id);
            List<DocumentSegment> segments = documentSegmentService.getSegmentsByKnowledgeBaseId(id);

            long withVectorData = segments.stream()
                .filter(s -> s.getVectorData() != null && !s.getVectorData().isEmpty())
                .count();
            long withDimension = segments.stream()
                .filter(s -> s.getVectorDimension() != null && s.getVectorDimension() > 0)
                .count();

            log.info("✅ 共{}个分片, {}个有向量维度, {}个有向量数据",
                    segments.size(), withDimension, withVectorData);

            if (!segments.isEmpty()) {
                DocumentSegment first = segments.get(0);
                log.info("🔍 第一个分片详情: id={}, segmentIndex={}, vectorDimension={}, embeddingId={}, vectorDataLength={}",
                    first.getId(), first.getSegmentIndex(), first.getVectorDimension(), 
                    first.getEmbeddingId(), first.getVectorData() != null ? first.getVectorData().length() : 0);
            }

            if (segments.size() > 0 && withVectorData == 0) {
                log.warn("⚠️ 知识库ID={}有{}个分片但都没有向量数据！", id, segments.size());
                log.warn("💡 请检查处理流程是否正常，向量数据是否正确保存");
            }

            return AjaxResult.success(new ListResponse<>(segments));
        } catch (Exception e) {
            log.error("❌ 获取分片列表失败 - ID: {}", id, e);
            return AjaxResult.error("获取分片列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取文件访问信息")
    @GetMapping("/{id}/file-info")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public AjaxResult getFileInfo(@PathVariable("id") Long id) {
        log.info("🔍 接收到文件信息请求 - ID: {}", id);
        try {
            KnowledgeBase knowledge = knowledgeBaseService.getById(id);
            if (knowledge == null) {
                log.warn("⚠️ 知识库不存在 - ID: {}", id);
                return AjaxResult.error("知识库不存在");
            }

            log.info("✅ 找到知识库 - ID: {}, 文件名: {}, 类型: {}",
                id, knowledge.getFileName(), knowledge.getFileType());

            KnowledgeFileInfoResponse response = new KnowledgeFileInfoResponse();
            response.setFileName(knowledge.getFileName());
            response.setFileType(knowledge.getFileType());
            response.setFilePath(knowledge.getFilePath());
            response.setFileSize(knowledge.getFileSize());
            response.setExists(true);
            response.setAccessUrl("/cms/ai/knowledge-base/" + id + "/download");

            if (knowledge.getPdfFilePath() != null) {
                response.setPdfFilePath(knowledge.getPdfFilePath());
            }

            log.info("📤 返回文件信息成功");
            return AjaxResult.success(response);

        } catch (Exception e) {
            log.error("❌ 获取文件信息失败 - ID: {}", id, e);
            return AjaxResult.error("获取文件信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "预览文件（智能判断格式）")
    @GetMapping("/{id}/preview")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public ResponseEntity<org.springframework.core.io.Resource> previewFile(@PathVariable("id") Long id) {
        try {
            log.info("📂 开始预览文件，知识库ID: {}", id);

            KnowledgeBase knowledge = knowledgeBaseService.getById(id);
            if (knowledge == null) {
                log.error("❌ 知识库不存在，ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            if (knowledge.getFileType() == null || knowledge.getFileType().isEmpty()) {
                log.error("❌ 文件类型为空，知识库ID: {}", id);
                return ResponseEntity.badRequest().build();
            }

            String fileType = knowledge.getFileType().toLowerCase();
            log.info("📄 文件类型: {}", fileType);

            String previewObjectName;
            String contentType;
            String displayFileName;

            if (isImageFile(fileType)) {
                previewObjectName = knowledge.getFilePath();
                contentType = getImageContentType(fileType);
                displayFileName = knowledge.getFileName();
                log.info("🖼️ 图片文件，直接预览原文件");
            }
            else if ("pdf".equals(fileType)) {
                previewObjectName = knowledge.getFilePath();
                contentType = "application/pdf";
                displayFileName = knowledge.getFileName();
                log.info("📑 PDF文件，直接预览");
            }
            else if (fileType.matches("txt|md|csv")) {
                previewObjectName = knowledge.getFilePath();
                contentType = "text/plain; charset=UTF-8";
                displayFileName = knowledge.getFileName();
                log.info("📄 文本文件，直接预览");
            }
            else if (fileType.matches("docx?|xlsx?|pptx?")) {
                if (knowledge.getPdfFilePath() != null && !knowledge.getPdfFilePath().isEmpty()) {
                    previewObjectName = knowledge.getPdfFilePath();
                    contentType = "application/pdf";
                    displayFileName = knowledge.getFileName().replaceAll("\\.[^.]+$", ".pdf");
                    log.info("📊 Office文件，预览PDF版本（用于定位）");
                } else {
                    previewObjectName = knowledge.getFilePath();
                    contentType = getOfficeContentType(fileType);
                    displayFileName = knowledge.getFileName();
                    log.warn("⚠️ Office文件，PDF转换失败或不存在，返回原文件");
                }
            }
            else {
                if (knowledge.getPdfFilePath() != null && !knowledge.getPdfFilePath().isEmpty()) {
                    previewObjectName = knowledge.getPdfFilePath();
                    contentType = "application/pdf";
                    displayFileName = knowledge.getFileName().replaceAll("\\.[^.]+$", ".pdf");
                    log.info("📄 其他文件，使用PDF预览");
                } else {
                    previewObjectName = knowledge.getFilePath();
                    contentType = "application/octet-stream";
                    displayFileName = knowledge.getFileName();
                    log.warn("⚠️ 其他文件，PDF不存在，返回原文件");
                }
            }

            log.info("📂 预览 MinIO 对象: {}", previewObjectName);

            java.io.InputStream inputStream = minioService.getFileStream(
                    previewObjectName, minioService.getKnowledgeBucket());

            if (inputStream == null) {
                log.error("❌ 预览文件不存在于 MinIO，对象名: {}", previewObjectName);
                log.error("💡 提示: 文件可能转换失败或未完成，请检查knowledge_base表的pdf_file_path字段");
                return ResponseEntity.notFound().build();
            }

            org.springframework.core.io.Resource resource = 
                    new org.springframework.core.io.InputStreamResource(inputStream);

            log.info("✅ 预览文件存在于 MinIO，准备返回");

            String encodedFileName = URLEncoder.encode(displayFileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename*=UTF-8''" + encodedFileName)
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType)
                    .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS")
                    .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                    .body(resource);
        } catch (Exception e) {
            log.error("❌ 预览文件失败，知识库ID: {}, 错误: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isImageFile(String fileType) {
        return fileType.matches("jpg|jpeg|png|gif|bmp|webp|svg");
    }

    private boolean isDocumentFile(String fileType) {
        return fileType.matches("docx?|xlsx?|pptx?|txt|md|csv");
    }

    private String getImageContentType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            default:
                return "application/octet-stream";
        }
    }

    private String getOfficeContentType(String fileType) {
        switch (fileType.toLowerCase()) {
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls":
                return "application/vnd.ms-excel";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            default:
                return "application/octet-stream";
        }
    }

    @Operation(summary = "下载原始文件")
    @GetMapping("/{id}/download")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(@PathVariable("id") Long id) {
        try {
            log.info("开始下载文件，知识库ID: {}", id);

            KnowledgeBase knowledge = knowledgeBaseService.getById(id);
            if (knowledge == null) {
                log.error("知识库不存在，ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            log.info("文件信息 - 文件名: {}, MinIO对象: {}, 文件类型: {}",
                    knowledge.getFileName(), knowledge.getFilePath(), knowledge.getFileType());

            java.io.InputStream inputStream = minioService.getFileStream(
                    knowledge.getFilePath(), minioService.getKnowledgeBucket());
            
            if (inputStream == null) {
                log.error("文件不存在于 MinIO，对象名: {}", knowledge.getFilePath());
                return ResponseEntity.notFound().build();
            }

            org.springframework.core.io.Resource resource = 
                    new org.springframework.core.io.InputStreamResource(inputStream);

            log.info("文件存在于 MinIO，准备返回");

            String encodedFileName = URLEncoder.encode(knowledge.getFileName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename*=UTF-8''" + encodedFileName)
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE,
                            getContentType(knowledge.getFileType()))
                    .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS")
                    .header(org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*")
                    .body(resource);
        } catch (Exception e) {
            log.error("下载文件失败，知识库ID: {}, 错误: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "获取配置模板列表")
    @GetMapping("/templates")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:list')")
    public AjaxResult getTemplates() {
        try {
            List<com.moyun.ext.ai.entity.KnowledgeConfigTemplate> entityTemplates = knowledgeConfigService.getAllTemplates();
            List<KnowledgeTemplateResponse.Template> templates = convertToTemplateResponse(entityTemplates);
            KnowledgeTemplateResponse response = new KnowledgeTemplateResponse(templates);
            return AjaxResult.success(response);
        } catch (Exception e) {
            log.error("获取模板列表失败", e);
            return AjaxResult.error("获取模板列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取推荐配置模板")
    @GetMapping("/templates/recommended")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:list')")
    public AjaxResult getRecommendedTemplates(
            @RequestParam("fileType") String fileType) {
        try {
            List<com.moyun.ext.ai.entity.KnowledgeConfigTemplate> entityTemplates = knowledgeConfigService.getRecommendedTemplates(fileType);
            List<KnowledgeTemplateResponse.Template> templates = convertToTemplateResponse(entityTemplates);
            KnowledgeTemplateResponse response = new KnowledgeTemplateResponse(templates);
            return AjaxResult.success(response);
        } catch (Exception e) {
            log.error("获取推荐模板失败 - fileType: {}", fileType, e);
            return AjaxResult.error("获取推荐模板失败: " + e.getMessage());
        }
    }

    @Operation(summary = "配置知识库")
    @PostMapping("/configure")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:edit')")
    public AjaxResult configureKnowledge(
            @RequestBody KnowledgeConfigRequest request) {
        try {
            log.info("配置知识库，knowledgeId={}", request.getKnowledgeId());

            KnowledgeConfig config = knowledgeConfigService.applyConfiguration(request);

            KnowledgeBase knowledge = knowledgeBaseService.getById(request.getKnowledgeId());
            if (knowledge == null) {
                return AjaxResult.error("知识库不存在");
            }

            knowledge.setConfigCompleted(true);
            knowledge.setProcessingStatus("configured");
            knowledgeBaseService.updateById(knowledge);

            String processingStatus = "configured";
            String message = "配置成功";

            if (Boolean.TRUE.equals(request.getStartProcessing())) {
                log.info("开始处理知识库");
                knowledge.setProcessingStatus("processing");
                knowledgeBaseService.updateById(knowledge);
                processingStatus = "processing";
                message = "配置成功，开始处理";

                final Long knowledgeId = knowledge.getId();
                final Long configId = config.getId();
                knowledgeProcessExecutor.execute(() -> {
                    log.info("【异步任务启动】知识库ID={}, 配置ID={}, 线程={}",
                            knowledgeId, configId, Thread.currentThread().getName());
                    try {
                        knowledgeBaseService.processKnowledge(knowledgeId, config);

                        KnowledgeBase kb = knowledgeBaseService.getById(knowledgeId);
                        if (kb != null) {
                            kb.setProcessingStatus("completed");
                            kb.setStatus(2);
                            kb.setErrorMessage(null);
                            knowledgeBaseService.updateById(kb);
                            log.info("【异步任务完成】知识库ID={}", knowledgeId);
                        }
                    } catch (Exception e) {
                        log.error("【异步任务失败】知识库ID={}, 错误: {}", knowledgeId, e.getMessage(), e);

                        KnowledgeBase kb = knowledgeBaseService.getById(knowledgeId);
                        if (kb != null) {
                            kb.setProcessingStatus("failed");
                            kb.setStatus(3);
                            kb.setErrorMessage(e.getMessage());
                            knowledgeBaseService.updateById(kb);
                        }
                    }
                });
            }

            ConfigureKnowledgeResponse response = new ConfigureKnowledgeResponse(
                knowledge.getId(), config, processingStatus
            );
            return AjaxResult.success(message, response);

        } catch (Exception e) {
            log.error("配置知识库失败 - knowledgeId: {}", request.getKnowledgeId(), e);
            return AjaxResult.error("配置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询处理状态")
    @GetMapping("/status/{knowledgeId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public AjaxResult getStatus(@PathVariable Long knowledgeId) {
        try {
            KnowledgeBase knowledge = knowledgeBaseService.getById(knowledgeId);
            if (knowledge == null) {
                return AjaxResult.error("知识库不存在");
            }

            String statusText = "";
            Integer progress = 0;

            String status = knowledge.getProcessingStatus();
            if ("processing".equals(status)) {
                statusText = "正在处理中...";
                progress = 50;
            } else if ("completed".equals(status)) {
                statusText = "处理完成";
                progress = 100;
            } else if ("failed".equals(status)) {
                statusText = "处理失败";
            } else if ("pending".equals(status)) {
                statusText = "等待配置";
            } else if ("configured".equals(status)) {
                statusText = "已配置，等待处理";
            }

            KnowledgeStatusResponse response = new KnowledgeStatusResponse(
                knowledge.getId(),
                knowledge.getProcessingStatus(),
                statusText,
                progress,
                knowledge.getSegmentCount(),
                knowledge.getSegmentCount(),
                knowledge.getErrorMessage()
            );

            return AjaxResult.success(response);

        } catch (Exception e) {
            log.error("查询状态失败 - knowledgeId: {}", knowledgeId, e);
            return AjaxResult.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "重置处理状态")
    @PostMapping("/reset-status/{knowledgeId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:edit')")
    public AjaxResult resetStatus(@PathVariable Long knowledgeId) {
        try {
            KnowledgeBase knowledge = knowledgeBaseService.getById(knowledgeId);
            if (knowledge == null) {
                return AjaxResult.error("知识库不存在");
            }

            knowledge.setProcessingStatus("configured");
            knowledge.setStatus(1);
            knowledge.setErrorMessage(null);
            knowledgeBaseService.updateById(knowledge);

            log.info("重置知识库状态成功 - ID: {}", knowledgeId);
            return AjaxResult.success("状态已重置，可以重新处理");

        } catch (Exception e) {
            log.error("重置状态失败 - knowledgeId: {}", knowledgeId, e);
            return AjaxResult.error("重置失败: " + e.getMessage());
        }
    }

    @Operation(summary = "开始处理知识库")
    @PostMapping("/start-processing/{knowledgeId}")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:edit')")
    public AjaxResult startProcessing(@PathVariable Long knowledgeId) {
        try {
            KnowledgeBase knowledge = knowledgeBaseService.getById(knowledgeId);
            if (knowledge == null) {
                return AjaxResult.error("知识库不存在");
            }

            if (!Boolean.TRUE.equals(knowledge.getConfigCompleted())) {
                return AjaxResult.error("请先完成配置");
            }

            KnowledgeConfig config = knowledgeConfigService.getConfigByKnowledgeId(knowledgeId);

            if (config == null) {
                return AjaxResult.error("配置不存在");
            }

            knowledge.setProcessingStatus("processing");
            knowledge.setStatus(1);
            knowledgeBaseService.updateById(knowledge);

            final Long finalKnowledgeId = knowledgeId;
            final Long configId = config.getId();
            knowledgeProcessExecutor.execute(() -> {
                log.info("【异步任务启动-手动触发】知识库ID={}, 配置ID={}, 线程={}",
                        finalKnowledgeId, configId, Thread.currentThread().getName());
                try {
                    knowledgeBaseService.processKnowledge(finalKnowledgeId, config);

                    KnowledgeBase kb = knowledgeBaseService.getById(finalKnowledgeId);
                    if (kb != null) {
                        kb.setProcessingStatus("completed");
                        kb.setStatus(2);
                        kb.setErrorMessage(null);
                        knowledgeBaseService.updateById(kb);
                        log.info("【异步任务完成-手动触发】知识库ID={}", finalKnowledgeId);
                    }
                } catch (Exception e) {
                    log.error("【异步任务失败-手动触发】知识库ID={}, 错误: {}", finalKnowledgeId, e.getMessage(), e);

                    KnowledgeBase kb = knowledgeBaseService.getById(finalKnowledgeId);
                    if (kb != null) {
                        kb.setProcessingStatus("failed");
                        kb.setStatus(3);
                        kb.setErrorMessage(e.getMessage());
                        knowledgeBaseService.updateById(kb);
                    }
                }
            });

            KnowledgeStatusResponse response = new KnowledgeStatusResponse(
                knowledgeId, "processing", "正在处理中...", 50, 0, 0, null
            );
            return AjaxResult.success("开始处理", response);

        } catch (Exception e) {
            log.error("开始处理失败 - knowledgeId: {}", knowledgeId, e);
            return AjaxResult.error("开始处理失败: " + e.getMessage());
        }
    }

    private String getContentType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain; charset=UTF-8";
            case "md" -> "text/markdown; charset=UTF-8";
            case "csv" -> "text/csv; charset=UTF-8";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "application/octet-stream";
        };
    }
    
    @Operation(summary = "更新知识库元数据")
    @PutMapping("/{id}/metadata")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:edit')")
    public AjaxResult updateMetadata(
            @PathVariable Long id,
            @RequestBody KnowledgeMetadataRequest request) {
        try {
            boolean success = knowledgeBaseService.updateMetadata(
                id,
                request.getCategory(),
                request.getTags(),
                request.getDescription()
            );
            
            if (success) {
                return AjaxResult.success("更新成功", null);
            } else {
                return AjaxResult.error("知识库不存在");
            }
        } catch (Exception e) {
            log.error("更新知识库元数据失败 - id: {}", id, e);
            return AjaxResult.error("更新失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "获取知识库统计信息")
    @GetMapping("/stats")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:list')")
    public AjaxResult getStats() {
        try {
            KnowledgeStatsResponse stats = knowledgeBaseService.getStats();
            return AjaxResult.success("获取成功", stats);
        } catch (Exception e) {
            log.error("获取知识库统计失败", e);
            return AjaxResult.error("获取统计失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "测试知识库检索")
    @PostMapping("/{id}/test-retrieval")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:query')")
    public AjaxResult testRetrieval(
            @PathVariable Long id,
            @RequestBody RetrievalTestRequest request) {
        try {
            List<RetrievalTestResult> results = knowledgeBaseService.testRetrieval(id, request);
            return AjaxResult.success("检索成功", results);
        } catch (Exception e) {
            log.error("检索测试失败 - id: {}", id, e);
            return AjaxResult.error("检索失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "批量操作")
    @PostMapping("/batch")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:edit')")
    public AjaxResult batchOperation(
            @RequestBody BatchOperationRequest request) {
        try {
            boolean success = knowledgeBaseService.batchOperation(
                request.getOperation(),
                request.getIds(),
                request.getCategory(),
                request.getTags()
            );
            
            if (success) {
                return AjaxResult.success("批量操作成功", null);
            } else {
                return AjaxResult.error("批量操作失败");
            }
        } catch (Exception e) {
            log.error("批量操作失败", e);
            return AjaxResult.error("批量操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "修复向量维度")
    @PostMapping("/fix-vector-dimension")
    @PreAuthorize("@ss.hasPermi('cms:ai:knowledge-base:edit')")
    public AjaxResult fixVectorDimension(
            @RequestParam(value = "id", required = false) Long id) {
        try {
            int fixedCount = knowledgeBaseService.fixVectorDimension(id);
            String message = id != null 
                ? String.format("修复完成，知识库ID=%d", id)
                : String.format("批量修复完成，共修复 %d 条记录", fixedCount);
            log.info("✅ {}", message);
            return AjaxResult.success(message);
        } catch (Exception e) {
            log.error("❌ 修复向量维度失败 - id: {}", id, e);
            return AjaxResult.error("修复失败: " + e.getMessage());
        }
    }
}
