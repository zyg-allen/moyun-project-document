package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.service.*;
import com.moyun.ext.ai.util.JsonUtils;
import com.moyun.ext.ai.vo.KnowledgeBaseVO;
import com.moyun.ext.ai.entity.DocumentImage;
import com.moyun.ext.ai.entity.DocumentSegment;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.entity.ModelConfig;
import com.moyun.ext.ai.mapper.KnowledgeBaseMapper;
import com.moyun.ext.ai.dto.KnowledgeStatsResponse;
import com.moyun.ext.ai.dto.RetrievalTestResult;
import com.moyun.ext.ai.entity.KnowledgeConfig;
import com.moyun.ext.ai.entity.KnowledgeLibraryConfig;
import com.moyun.ext.ai.util.DocumentToPdfConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import com.moyun.ext.ai.util.PdfBox3DocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.moyun.ext.ai.store.VectorStoreExtension;

/**
 * 知识库服务实现类
 *
 * <p>核心功能：</p>
 * <ul>
 *     <li>文档上传和解析（支持PDF/Word/Excel/TXT/Markdown）</li>
 *     <li>文档分块和Embedding向量化</li>
 *     <li>向量存储和检索（Redis 8.0+ RediSearch / Elasticsearch 可切换）</li>
 *     <li>支持多模态（图片提取和分析）</li>
 *     <li>异步处理和进度跟踪</li>
 *     <li>知识库管理（CRUD操作）</li>
 * </ul>
 *
 * <p>技术栈：</p>
 * <ul>
 *     <li>LangChain4j - 文档处理和Embedding</li>
 *     <li>VectorStoreExtension - 向量存储抽象（redis / es 可切换）</li>
 *     <li>Apache PDFBox/POI - 文档解析</li>
 *     <li>MinIO - 文件存储</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    // ==================== 依赖注入 ====================

    private final VectorStoreExtension embeddingStore;
    private final DocumentSegmentService documentSegmentService;
    private final ModelConfigService modelConfigService;
    private final ImageExtractionService imageExtractionService;
    private final DocumentImageService documentImageService;
    private final com.moyun.ext.ai.service.AgentService agentService;
    private final com.moyun.ext.ai.service.KnowledgeProcessProgressService progressService;
    private final com.moyun.ext.ai.service.TokenUsageService tokenUsageService;
    private final MinioService minioService;
    private final DocumentTypeDetector documentTypeDetector;
    private final AdaptiveChunkingService adaptiveChunkingService;
    private final KnowledgeConfigService knowledgeConfigService;
    
    @Autowired(required = false)
    private KnowledgeIncrementalService knowledgeIncrementalService;

    @Autowired
    private LargeDocumentProcessor largeDocumentProcessor;

    // ==================== 配置属性 ====================

    /** JSON序列化工具 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Embedding模型，通过@PostConstruct动态初始化 */
    private EmbeddingModel embeddingModel;

    // ==================== 构造函数 ====================

    public KnowledgeBaseServiceImpl(
            @Qualifier("embeddingStore") VectorStoreExtension embeddingStore,
            DocumentSegmentService documentSegmentService,
            ModelConfigService modelConfigService,
            ImageExtractionService imageExtractionService,
            DocumentImageService documentImageService,
            com.moyun.ext.ai.service.AgentService agentService,
            com.moyun.ext.ai.service.KnowledgeProcessProgressService progressService,
            com.moyun.ext.ai.service.TokenUsageService tokenUsageService,
            MinioService minioService,
            DocumentTypeDetector documentTypeDetector,
            AdaptiveChunkingService adaptiveChunkingService,
            KnowledgeConfigService knowledgeConfigService) {
        this.embeddingStore = embeddingStore;
        this.documentSegmentService = documentSegmentService;
        this.modelConfigService = modelConfigService;
        this.imageExtractionService = imageExtractionService;
        this.documentImageService = documentImageService;
        this.agentService = agentService;
        this.progressService = progressService;
        this.tokenUsageService = tokenUsageService;
        this.minioService = minioService;
        this.documentTypeDetector = documentTypeDetector;
        this.adaptiveChunkingService = adaptiveChunkingService;
        this.knowledgeConfigService = knowledgeConfigService;
    }

    /**
     * 初始化 Embedding 模型（应用启动时执行）
     * 如果有默认配置则使用，没有则等待用户配置
     */
    @PostConstruct
    public void init() {
        // 初始化 Embedding 模型
        try {
            ModelConfig config = modelConfigService.getDefaultEmbeddingConfig();
            if (config != null) {
                this.embeddingModel = modelConfigService.createEmbeddingModel(config.getId());
                log.info("✅ 使用动态配置的 Embedding 模型: {} ({})", config.getName(), config.getModelName());
            } else {
                log.warn("⚠️ 未找到默认 Embedding 模型配置，请在'模型配置管理'中添加并设置默认模型");
                log.warn("⚠️ 在配置 Embedding 模型之前，无法进行文档向量化");
            }
        } catch (Exception e) {
            log.error("❌ 初始化 Embedding 模型失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public KnowledgeBase uploadFile(MultipartFile file) throws Exception {
        // 创建知识库记录
        KnowledgeBase knowledge = new KnowledgeBase();
        knowledge.setFileName(file.getOriginalFilename());
        knowledge.setFileSize(file.getSize());
        knowledge.setFileType(getFileExtension(file.getOriginalFilename()));
        knowledge.setUploadTime(LocalDateTime.now());
        knowledge.setStatus(1); // 处理中

        // 上传文件到 MinIO
        String savedFilePath = minioService.uploadKnowledgeFile(file, file.getOriginalFilename());
        knowledge.setFilePath(savedFilePath);
        log.info("✅ 文件已上传到 MinIO: {}", savedFilePath);

        // 保存到数据库（先保存，后续异步处理）
        this.save(knowledge);

        // 异步处理 PDF 转换和向量化
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始异步处理文档，ID: {}", knowledge.getId());

                // 1. 转换为 PDF（所有非PDF文件都会转换）
                log.info("开始文档转换，MinIO对象: {}, 类型: {}", savedFilePath, knowledge.getFileType());
                String pdfObjectName = convertToPdfIfNeeded(savedFilePath, knowledge.getFileType());

                // 更新 PDF 路径（MinIO 对象名）
                knowledge.setPdfFilePath(pdfObjectName);
                this.updateById(knowledge);
                log.info("✅ PDF 转换完成，ID: {}, MinIO对象: {}", knowledge.getId(), pdfObjectName);

                // 2. 处理文本向量化
                log.info("开始文本向量化，ID: {}", knowledge.getId());
                processVectorization(knowledge);
                log.info("✅ 文本向量化完成，ID: {}", knowledge.getId());

                // 3. 提取图片并使用多模态模型理解（所有转换后的PDF都会提取图片）
                if (embeddingModel != null) {
                    try {
                        log.info("========== 开始图片提取流程 ==========");
                        log.info("文档ID: {}, 原始类型: {}, PDF对象: {}",
                                knowledge.getId(), knowledge.getFileType(), pdfObjectName);

                        List<DocumentImage> images = imageExtractionService.extractAndVectorizeImages(knowledge, embeddingModel);

                        log.info("✅ 图片提取和向量化完成，ID: {}, 共 {} 张图片", knowledge.getId(), images.size());
                        log.info("========================================");
                    } catch (Exception e) {
                        log.error("❌ 图片提取失败，ID: {}, 错误: {}", knowledge.getId(), e.getMessage(), e);
                        log.error("堆栈信息: ", e);
                        // 图片提取失败不影响整体处理流程
                    }
                } else {
                    log.warn("⚠️ Embedding 模型未配置，跳过图片提取");
                }

            } catch (Exception e) {
                log.error("文档处理失败，ID: {}, 错误: {}", knowledge.getId(), e.getMessage(), e);
                knowledge.setStatus(3); // 处理失败
                knowledge.setErrorMessage(e.getMessage());
                this.updateById(knowledge);
            }
        });

        return knowledge;
    }

    @Override
    public List<KnowledgeBaseVO> listAllWithFormat() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(KnowledgeBase::getUploadTime);
        List<KnowledgeBase> list = this.list(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public boolean deleteKnowledge(Long id) {
        KnowledgeBase knowledge = this.getById(id);
        if (knowledge == null) {
            return false;
        }

        log.info("开始删除知识库 - ID: {}", id);

        // 0. 检查是否有智能体关联
        List<com.moyun.ext.ai.entity.Agent> agents = checkAgentAssociation(id);
        if (!agents.isEmpty()) {
            String agentNames = agents.stream()
                .map(com.moyun.ext.ai.entity.Agent::getName)
                .collect(Collectors.joining(", "));
            log.warn("❌ 知识库被智能体关联，无法删除 - 关联的智能体: {}", agentNames);
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_IN_USE, "知识库被以下智能体关联，无法删除：" + agentNames);
        }

        // 1. 删除图片记录和文件（从 MinIO）
        try {
            int deletedCount = minioService.deleteImagesByKnowledgeBaseId(id);
            log.info("从 MinIO 删除 {} 张图片", deletedCount);

            documentImageService.deleteByKnowledgeBaseId(id);
            log.info("删除图片数据库记录完成");
        } catch (Exception e) {
            log.error("删除图片数据失败", e);
        }

        // 2. 删除文本分片记录
        documentSegmentService.deleteByKnowledgeBaseId(id);
        log.info("删除文本分片记录完成");

        // 3. 删除向量数据（从向量库，redis/es 通用）
        try {
            deleteVectorsByKnowledgeBaseId(id);
            log.info("✅ 删除向量数据完成");
        } catch (Exception e) {
            log.error("删除向量数据失败", e);
        }

        // 4. 删除原始文件（从 MinIO）
        try {
            minioService.deleteFile(knowledge.getFilePath(), minioService.getKnowledgeBucket());
            log.info("从 MinIO 删除原始文件: {}", knowledge.getFilePath());
        } catch (Exception e) {
            log.error("删除原始文件失败: {}", e);
        }

        // 5. 删除PDF文件（从 MinIO）
        if (knowledge.getPdfFilePath() != null && !knowledge.getPdfFilePath().isEmpty()) {
            try {
                minioService.deleteFile(knowledge.getPdfFilePath(), minioService.getKnowledgeBucket());
                log.info("从 MinIO 删除PDF文件: {}", knowledge.getPdfFilePath());
            } catch (Exception e) {
                log.error("删除PDF文件失败: {}", e);
            }
        }

        // 6. 删除数据库记录
        boolean result = this.removeById(id);
        log.info("删除知识库完成 - ID: {}, 结果: {}", id, result);
        return result;
    }

    @Override
    public boolean reprocessFile(Long id) {
        KnowledgeBase knowledge = this.getById(id);
        if (knowledge == null) {
            return false;
        }

        knowledge.setStatus(1); // 处理中
        this.updateById(knowledge);

        // 异步重新处理
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始重新处理文档，ID: {}", knowledge.getId());

                // 如果没有 PDF 文件，先转换
                if (knowledge.getPdfFilePath() == null || knowledge.getPdfFilePath().isEmpty()) {
                    String pdfFilePath = convertToPdfIfNeeded(knowledge.getFilePath(), knowledge.getFileType());
                    knowledge.setPdfFilePath(pdfFilePath);
                    this.updateById(knowledge);
                    log.info("PDF 转换完成，ID: {}", knowledge.getId());
                }

                // 处理向量化
                processVectorization(knowledge);
                log.info("重新处理完成，ID: {}", knowledge.getId());

                // 重新提取图片
                if (embeddingModel != null) {
                    try {
                        log.info("重新提取和处理文档图片，ID: {}", knowledge.getId());
                        List<DocumentImage> images = imageExtractionService.extractAndVectorizeImages(knowledge, embeddingModel);
                        log.info("✅ 图片重新提取完成，ID: {}, 共 {} 张图片", knowledge.getId(), images.size());
                    } catch (Exception imgEx) {
                        log.error("❌ 图片提取失败，ID: {}, 错误: {}", knowledge.getId(), imgEx.getMessage());
                    }
                }

            } catch (Exception e) {
                log.error("重新处理失败，ID: {}, 错误: {}", knowledge.getId(), e.getMessage(), e);
                knowledge.setStatus(3); // 处理失败
                knowledge.setErrorMessage(e.getMessage());
                this.updateById(knowledge);
            }
        });

        return true;
    }

    /**
     * 处理文件向量化
     */
    private void processVectorization(KnowledgeBase knowledge) throws Exception {
        // 检查 Embedding 模型是否已配置
        if (embeddingModel == null) {
            log.error("❌ Embedding 模型未配置，无法进行向量化处理");
            knowledge.setStatus(3); // 处理失败
            knowledge.setProcessTime(LocalDateTime.now());
            this.updateById(knowledge);
            throw new BusinessException(ErrorCode.EMBEDDING_MODEL_NOT_CONFIGURED, "Embedding 模型未配置，请在'模型配置管理'中添加并设置默认的向量模型");
        }

        knowledge.setStatus(1); // 处理中
        this.updateById(knowledge);

        String fileType = knowledge.getFileType().toLowerCase();

        // 如果有 PDF 文件，统一使用 PDF 进行向量化（保证页码准确）
        boolean usePdfForProcessing = knowledge.getPdfFilePath() != null &&
                                       !knowledge.getPdfFilePath().isEmpty() &&
                                       !knowledge.getPdfFilePath().equals(knowledge.getFilePath());

        if (usePdfForProcessing) {
            log.info("使用转换后的 PDF 文件进行向量化: {}", knowledge.getPdfFilePath());
            processPdfDocument(knowledge);
            // processPdfDocument 内部已经更新状态，直接返回
            return;
        }

        // 根据文件类型选择不同的处理方式
        if ("pdf".equals(fileType)) {
            processPdfDocument(knowledge);
            // processPdfDocument 内部已经更新状态，直接返回
            return;
        } else if ("txt".equals(fileType) || "md".equals(fileType) || "csv".equals(fileType)) {
            processTextDocument(knowledge);
        } else if ("doc".equals(fileType) || "docx".equals(fileType)) {
            processWordDocument(knowledge);
        } else if ("xls".equals(fileType) || "xlsx".equals(fileType)) {
            processExcelDocument(knowledge);
        } else if ("ppt".equals(fileType) || "pptx".equals(fileType)) {
            processPowerPointDocument(knowledge);
        } else {
            processGenericDocument(knowledge);
        }

        // 更新状态（PDF 已在 processPdfDocument 中更新）
        knowledge.setStatus(2); // 处理成功
        knowledge.setProcessTime(LocalDateTime.now());
        this.updateById(knowledge);
    }

    /**
     * 处理 PDF 文档（记录页码）
     * 从 MinIO 下载文件到临时目录进行处理
     * 
     * <p>自动检测文件大小：</p>
     * <ul>
     *   <li>小文件（<20MB）：使用标准处理方式</li>
     *   <li>大文件（≥20MB）：使用流式分页处理避免OOM</li>
     * </ul>
     * 
     * <p>性能优化：</p>
     * <ul>
     *   <li>预先提取所有页面文本并缓存</li>
     *   <li>页码计算时直接从缓存查找，避免重复提取</li>
     *   <li>时间复杂度从 O(n²) 降到 O(n)</li>
     * </ul>
     */
    private void processPdfDocument(KnowledgeBase knowledge) throws Exception {
        log.info("开始处理 PDF 文档: {}", knowledge.getFileName());

        // 确定使用哪个 MinIO 对象名（优先使用 PDF 文件路径）
        String pdfObjectName = knowledge.getPdfFilePath() != null && !knowledge.getPdfFilePath().isEmpty()
                ? knowledge.getPdfFilePath()
                : knowledge.getFilePath();

        log.info("使用 MinIO 对象: {}", pdfObjectName);

        // 从 MinIO 下载文件到临时目录
        java.io.InputStream inputStream = minioService.getFileStream(pdfObjectName, minioService.getKnowledgeBucket());
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "无法从 MinIO 获取文件: " + pdfObjectName);
        }

        Path tempPdfFile = Files.createTempFile("minio_pdf_process_", ".pdf");
        try {
            Files.copy(inputStream, tempPdfFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            log.info("✓ PDF文件已下载到临时目录: {}", tempPdfFile);

            // 记录解析方式
            knowledge.setParseMethod("PDFBox");

            // 检查文件大小，决定处理方式
            long fileSize = Files.size(tempPdfFile);
            log.info("📊 PDF 文件大小: {} MB", fileSize / 1024 / 1024);

            // 加载 PDF
            PDDocument pdfDoc = Loader.loadPDF(tempPdfFile.toFile());
            int totalPages = pdfDoc.getNumberOfPages();
            log.info("📄 PDF 总页数: {}", totalPages);

            try {
                if (LargeDocumentProcessor.isLargeFile(fileSize) || totalPages > 100) {
                    // 大文件或页数多的文件：使用流式处理
                    log.info("🚀 检测到大文件，使用流式分页处理");
                    int segmentCount = largeDocumentProcessor.processLargePdf(
                        knowledge, pdfDoc, embeddingModel, embeddingStore);
                    
                    knowledge.setSegmentCount(segmentCount);
                    knowledge.setStatus(2); // 处理成功
                    knowledge.setProcessTime(LocalDateTime.now());
                    this.updateById(knowledge);
                } else {
                    // 小文件：使用标准处理方式（带页面文本缓存优化）
                    log.info("📝 使用标准处理方式（带页面文本缓存优化）");
                    
                    // 🚀 性能优化：预先提取所有页面文本并缓存
                    long cacheStartTime = System.currentTimeMillis();
                    Map<Integer, String> pageTextCache = buildPageTextCache(pdfDoc);
                    long cacheDuration = System.currentTimeMillis() - cacheStartTime;
                    log.info("✅ 页面文本缓存完成，共 {} 页，耗时 {}ms", pageTextCache.size(), cacheDuration);
                    
                    // 解析文档
                    DocumentParser parser = new PdfBox3DocumentParser();
                    Document document;
                    try (java.io.InputStream fis = Files.newInputStream(tempPdfFile)) {
                        document = parser.parse(fis);
                    }
                    
                    processPdfDocumentInternalWithCache(knowledge, document, pdfDoc, pageTextCache);
                }
            } finally {
                pdfDoc.close();
            }
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempPdfFile);
        }
    }
    
    /**
     * 构建页面文本缓存
     * 
     * <p>一次性提取所有页面的文本内容并缓存，避免后续重复提取</p>
     * 
     * @param pdfDoc PDF文档对象
     * @return 页码到文本内容的映射
     */
    private Map<Integer, String> buildPageTextCache(PDDocument pdfDoc) throws Exception {
        Map<Integer, String> cache = new HashMap<>();
        PDFTextStripper stripper = new PDFTextStripper();
        
        for (int page = 1; page <= pdfDoc.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            String pageText = stripper.getText(pdfDoc);
            // 标准化文本（去除多余空白）
            String normalizedText = pageText.replaceAll("\\s+", " ").trim();
            cache.put(page, normalizedText);
        }
        
        return cache;
    }

    /**
     * 处理 PDF 文档内部逻辑（带页面文本缓存优化）
     * 
     * @param knowledge 知识库对象
     * @param document 解析后的文档
     * @param pdfDoc PDF文档对象
     * @param pageTextCache 页面文本缓存（页码 -> 文本内容）
     */
    private void processPdfDocumentInternalWithCache(KnowledgeBase knowledge, Document document, 
                                                      PDDocument pdfDoc, Map<Integer, String> pageTextCache) throws Exception {

        // 🚀 自适应分片策略
        // 根据文档类型和配置动态调整分片大小
        String contentSample = document.text().substring(0, Math.min(1000, document.text().length()));
        DocumentSplitter splitter = createAdaptiveDocumentSplitter(knowledge.getId(), knowledge.getFileName(), contentSample);
        List<TextSegment> segments = splitter.split(document);

        // 生成向量ID（用于关联分片，不再保存到knowledge表）
        String vectorId = UUID.randomUUID().toString();
        knowledge.setSegmentCount(segments.size());
        // 维度将在第一次 embedding 后获取

        // 🚀 性能优化：使用缓存计算所有分片的页码
        long pageCalcStartTime = System.currentTimeMillis();
        int[] pageNumbers = new int[segments.size()];
        for (int i = 0; i < segments.size(); i++) {
            pageNumbers[i] = calculatePdfPageNumberWithCache(segments.get(i).text(), pageTextCache);
            log.debug("分片 {} 定位到第 {} 页", i, pageNumbers[i]);
        }
        long pageCalcDuration = System.currentTimeMillis() - pageCalcStartTime;
        log.info("✅ 页码计算完成，共 {} 个分片，耗时 {}ms（平均 {}ms/分片）", 
                 segments.size(), pageCalcDuration, pageCalcDuration / segments.size());

        // 第二遍：处理每个分片
        for (int i = 0; i < segments.size(); i++) {
            TextSegment originalSegment = segments.get(i);
            String originalText = originalSegment.text();
            String embeddingId = vectorId + "_" + i;
            int pageNumber = pageNumbers[i];

            // 构建用于向量化的增强内容（不添加标记，保持自然语义）
            // 策略：将前后文自然地拼接，提高语义连贯性
            StringBuilder enhancedContent = new StringBuilder();

            // 添加前文的最后部分（自然过渡，不加标记）
            if (i > 0) {
                String prevText = segments.get(i - 1).text();
                // 取前文最后80字符作为上下文
                String prevContext = prevText.substring(Math.max(0, prevText.length() - 80)).trim();
                if (!originalText.startsWith(prevContext.substring(Math.max(0, prevContext.length() - 30)))) {
                    // 只有当没有重叠时才添加
                    enhancedContent.append(prevContext).append(" ");
                }
            }

            // 添加当前分片内容
            enhancedContent.append(originalText);

            // 添加后文的开头部分（自然过渡，不加标记）
            if (i < segments.size() - 1) {
                String nextText = segments.get(i + 1).text();
                // 取后文前80字符作为上下文
                String nextContext = nextText.substring(0, Math.min(80, nextText.length())).trim();
                if (!originalText.endsWith(nextContext.substring(0, Math.min(30, nextContext.length())))) {
                    // 只有当没有重叠时才添加
                    enhancedContent.append(" ").append(nextContext);
                }
            }

            // 创建用于向量存储的分片（增强内容用于更好的语义匹配）
            // 但存储的text仍然是原始内容，只是向量是基于增强内容生成的
            TextSegment segmentForStorage = TextSegment.from(
                originalText,  // 存储原始内容，检索返回时显示干净
                Metadata.from("vectorId", vectorId)
                        .put("fileName", knowledge.getFileName())
                        .put("fileType", knowledge.getFileType() != null ? knowledge.getFileType() : "pdf")
                        .put("segmentIndex", String.valueOf(i))
                        .put("knowledgeBaseId", String.valueOf(knowledge.getId()))
                        .put("embeddingId", embeddingId)
                        .put("pageNumber", String.valueOf(pageNumber))
                        .put("totalSegments", String.valueOf(segments.size()))
            );

            // 生成向量时使用增强内容（提高检索准确性）
            // 但存储时使用原始内容（显示干净）
            Embedding embedding = embeddingModel.embed(enhancedContent.toString()).content();
            embeddingStore.add(embedding, segmentForStorage);

            // 在第一次 embedding 后设置向量维度
            if (i == 0) {
                knowledge.setVectorDimension(embedding.dimension());
            }

            // 保存到数据库
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setPageNumber(pageNumber);
            docSegment.setContent(originalText);
            docSegment.setContentLength(originalText.length());
            docSegment.setEmbeddingId(embeddingId);
            docSegment.setVectorDimension(embedding.dimension());

            try {
                String vectorJson = objectMapper.writeValueAsString(embedding.vector());
                docSegment.setVectorData(vectorJson);
                log.debug("✓ 向量数据序列化成功，长度: {} 字符", vectorJson.length());
            } catch (Exception e) {
                log.error("❌ 向量数据序列化失败: {}", e.getMessage(), e);
            }

            docSegment.setCreateTime(LocalDateTime.now());

            log.debug("准备保存分片到数据库: index={}, dimension={}, vectorDataLength={}",
                    i, docSegment.getVectorDimension(),
                    docSegment.getVectorData() != null ? docSegment.getVectorData().length() : 0);

            try {
                boolean saved = documentSegmentService.save(docSegment);
                if (!saved) {
                    log.error("❌ 保存分片失败: index={}, knowledgeBaseId={}", i, knowledge.getId());
                    throw new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "保存分片失败");
                } else {
                    log.debug("✓ 分片保存成功: id={}, vectorDataLength={}",
                            docSegment.getId(),
                            docSegment.getVectorData() != null ? docSegment.getVectorData().length() : 0);
                }
            } catch (Exception e) {
                log.error("❌ 保存分片到数据库异常: index={}, error={}", i, e.getMessage(), e);
                throw e;
            }
        }

        // 更新状态（向量维度已在循环中设置）
        knowledge.setStatus(2); // 处理成功
        knowledge.setProcessTime(LocalDateTime.now());
        this.updateById(knowledge);

        log.info("✅ PDF 文档处理完成，共 {} 个分片，向量维度: {}", segments.size(), knowledge.getVectorDimension());
    }

    /**
     * 处理文本文档（记录行号）
     * 从 MinIO 下载文件到临时目录进行处理
     */
    private void processTextDocument(KnowledgeBase knowledge) throws Exception {
        log.info("开始处理文本文档: {}", knowledge.getFileName());

        // 从 MinIO 下载文件到临时目录
        java.io.InputStream inputStream = minioService.getFileStream(knowledge.getFilePath(), minioService.getKnowledgeBucket());
        if (inputStream == null) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "无法从 MinIO 获取文件: " + knowledge.getFilePath());
        }

        Path tempFile = Files.createTempFile("minio_text_", "." + knowledge.getFileType());
        try {
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            log.info("✓ 文本文件已下载到临时目录: {}", tempFile);

            // 记录解析方式
            knowledge.setParseMethod("Text");

            // 读取文件内容
            String fullContent = Files.readString(tempFile, StandardCharsets.UTF_8);

            // 解析文档
            DocumentParser parser = new TextDocumentParser();
            Document document;
            try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                document = parser.parse(fis);
            }

            processTextDocumentInternal(knowledge, document, fullContent);
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 处理文本文档内部逻辑
     */
    private void processTextDocumentInternal(KnowledgeBase knowledge, Document document, String fullContent) throws Exception {

        // 🚀 自适应分片策略
        String contentSample = document.text().substring(0, Math.min(1000, document.text().length()));
        DocumentSplitter splitter = createAdaptiveDocumentSplitter(knowledge.getId(), knowledge.getFileName(), contentSample);
        List<TextSegment> segments = splitter.split(document);

        // 生成向量ID（用于关联分片，不再保存到knowledge表）
        String vectorId = UUID.randomUUID().toString();
        knowledge.setSegmentCount(segments.size());
        // 维度将在第一次 embedding 后获取

        // 处理每个分片
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String embeddingId = vectorId + "_" + i;

            // 计算行号范围
            int[] lineRange = calculateLineRange(fullContent, segment.text());
            int lineStart = lineRange[0];
            int lineEnd = lineRange[1];
            log.info("分片 {} 定位到第 {}-{} 行", i, lineStart, lineEnd);

            // 添加元数据
            segment.metadata().put("vectorId", vectorId);
            segment.metadata().put("fileName", knowledge.getFileName());
            segment.metadata().put("fileType", knowledge.getFileType());
            segment.metadata().put("segmentIndex", String.valueOf(i));
            segment.metadata().put("knowledgeBaseId", String.valueOf(knowledge.getId()));
            segment.metadata().put("embeddingId", embeddingId);
            segment.metadata().put("lineStart", String.valueOf(lineStart));
            segment.metadata().put("lineEnd", String.valueOf(lineEnd));

            // 生成向量并存储
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            // 在第一次 embedding 后设置向量维度
            if (i == 0) {
                knowledge.setVectorDimension(embedding.dimension());
            }

            // 保存到数据库
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setLineStart(lineStart);
            docSegment.setLineEnd(lineEnd);
            docSegment.setContent(segment.text());
            docSegment.setContentLength(segment.text().length());
            docSegment.setEmbeddingId(embeddingId);
            docSegment.setVectorDimension(embedding.dimension());

            try {
                String vectorJson = objectMapper.writeValueAsString(embedding.vector());
                docSegment.setVectorData(vectorJson);
            } catch (Exception e) {
                log.warn("向量数据序列化失败: {}", e.getMessage());
            }

            docSegment.setCreateTime(LocalDateTime.now());
            documentSegmentService.save(docSegment);
        }

        // 保存向量维度到数据库
        this.updateById(knowledge);
        log.info("✅ 文本文档处理完成，共 {} 个分片，向量维度: {}", segments.size(), knowledge.getVectorDimension());
    }

    /**
     * 处理通用文档
     * 从 MinIO 下载文件到临时目录进行处理
     */
    private void processGenericDocument(KnowledgeBase knowledge) throws Exception {
        log.info("开始处理通用文档: {}", knowledge.getFileName());

        // 从 MinIO 下载文件到临时目录
        Path tempFile = downloadToTempFile(knowledge.getFilePath(), "." + knowledge.getFileType());
        try {
            // 记录解析方式
            knowledge.setParseMethod("Generic");

            // 根据文件类型选择解析器
            DocumentParser parser = getDocumentParser(knowledge.getFileType());

            // 解析文档
            Document document;
            try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                document = parser.parse(fis);
            }

            processGenericDocumentInternal(knowledge, document);
        } finally {
            // 清理临时文件
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 处理通用文档内部逻辑
     */
    private void processGenericDocumentInternal(KnowledgeBase knowledge, Document document) throws Exception {

        // 文档分割 - TODO: 应使用knowledge_config配置
        DocumentSplitter splitter = DocumentSplitters.recursive(800, 100);
        List<TextSegment> segments = splitter.split(document);

        // 生成向量ID（用于关联分片，不再保存到knowledge表）
        String vectorId = UUID.randomUUID().toString();
        knowledge.setSegmentCount(segments.size());
        // 维度将在第一次 embedding 后获取

        // 处理每个分片
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String embeddingId = vectorId + "_" + i;

            // 添加元数据
            segment.metadata().put("vectorId", vectorId);
            segment.metadata().put("fileName", knowledge.getFileName());
            segment.metadata().put("fileType", knowledge.getFileType());
            segment.metadata().put("segmentIndex", String.valueOf(i));
            segment.metadata().put("knowledgeBaseId", String.valueOf(knowledge.getId()));
            segment.metadata().put("embeddingId", embeddingId);

            // 生成向量并存储
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            // 在第一次 embedding 后设置向量维度
            if (i == 0) {
                knowledge.setVectorDimension(embedding.dimension());
            }

            // 保存到数据库
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setContent(segment.text());
            docSegment.setContentLength(segment.text().length());
            docSegment.setEmbeddingId(embeddingId);
            docSegment.setVectorDimension(embedding.dimension());

            try {
                String vectorJson = objectMapper.writeValueAsString(embedding.vector());
                docSegment.setVectorData(vectorJson);
            } catch (Exception e) {
                log.warn("向量数据序列化失败: {}", e.getMessage());
            }

            docSegment.setCreateTime(LocalDateTime.now());
            documentSegmentService.save(docSegment);
        }

        // 保存向量维度到数据库
        this.updateById(knowledge);
        log.info("✅ 通用文档处理完成，共 {} 个分片，向量维度: {}", segments.size(), knowledge.getVectorDimension());
    }

    /**
     * 计算 PDF 页码（使用缓存优化版本）
     * 
     * <p>性能优化：</p>
     * <ul>
     *   <li>直接从缓存中查找，避免重复提取页面文本</li>
     *   <li>时间复杂度从 O(n²) 降到 O(n)</li>
     *   <li>100页PDF处理时间从10分钟降到1分钟</li>
     * </ul>
     *
     * @param segmentText 分片文本
     * @param pageTextCache 页面文本缓存（页码 -> 标准化后的文本）
     * @return 页码（1-based）
     */
    private int calculatePdfPageNumberWithCache(String segmentText, Map<Integer, String> pageTextCache) {
        try {
            String normalizedSegment = segmentText.replaceAll("\\s+", " ").trim();

            // 策略1：使用分片中间部分的80个字符（避免跨页问题）
            int midStart = Math.max(0, normalizedSegment.length() / 2 - 40);
            int midEnd = Math.min(normalizedSegment.length(), midStart + 80);
            String midFeature = normalizedSegment.substring(midStart, midEnd).trim();

            if (midFeature.length() >= 20) {
                log.debug("策略1：使用中间特征文本: {}", midFeature.substring(0, Math.min(50, midFeature.length())));

                // 🚀 直接从缓存中查找，避免重复提取
                for (Map.Entry<Integer, String> entry : pageTextCache.entrySet()) {
                    if (entry.getValue().contains(midFeature)) {
                        log.debug("✓ 分片定位到第 {} 页（策略1：中间特征匹配）", entry.getKey());
                        return entry.getKey();
                    }
                }
            }

            // 策略2：使用分片后半部分的特征（避免开头跨页）
            int latterStart = Math.max(0, (int)(normalizedSegment.length() * 0.6));
            int latterEnd = Math.min(normalizedSegment.length(), latterStart + 60);
            String latterFeature = normalizedSegment.substring(latterStart, latterEnd).trim();

            if (latterFeature.length() >= 20) {
                log.debug("策略2：使用后半部分特征: {}", latterFeature.substring(0, Math.min(40, latterFeature.length())));

                for (Map.Entry<Integer, String> entry : pageTextCache.entrySet()) {
                    if (entry.getValue().contains(latterFeature)) {
                        log.debug("✓ 分片定位到第 {} 页（策略2：后半部分匹配）", entry.getKey());
                        return entry.getKey();
                    }
                }
            }

            // 策略3：使用开头特征（兜底，可能不准确）
            String headFeature = normalizedSegment.substring(0, Math.min(80, normalizedSegment.length())).trim();
            log.debug("策略3：使用开头特征: {}", headFeature.substring(0, Math.min(50, headFeature.length())));

            for (Map.Entry<Integer, String> entry : pageTextCache.entrySet()) {
                if (entry.getValue().contains(headFeature)) {
                    log.debug("✓ 分片定位到第 {} 页（策略3：开头特征匹配）", entry.getKey());
                    return entry.getKey();
                }
            }

            // 策略4：基于页面数量估算（最后的兜底）
            log.debug("策略4：基于页面数量估算");
            int totalPages = pageTextCache.size();
            int estimatedPage = Math.max(1, Math.min(totalPages / 2, totalPages));
            log.debug("✓ 分片定位到第 {} 页（策略4：估算）", estimatedPage);
            return estimatedPage;

        } catch (Exception e) {
            log.error("计算 PDF 页码失败", e);
        }
        return 1;
    }
    
    /**
     * 计算 PDF 页码（改进版：使用分片中间部分定位，避免跨页问题）
     * 
     * @deprecated 使用 {@link #calculatePdfPageNumberWithCache(String, Map)} 替代，性能更好
     *
     * 问题：分片可能跨页，如果用开头匹配，可能匹配到上一页的末尾
     * 解决：优先使用分片的中间部分进行匹配，这样更准确地定位到内容主体所在的页
     */
    @Deprecated
    private int calculatePdfPageNumber(PDDocument pdfDoc, String segmentText, String fullText) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String normalizedSegment = segmentText.replaceAll("\\s+", " ").trim();

            // 策略1：使用分片中间部分的80个字符（避免跨页问题）
            // 分片可能跨页，开头可能在上一页末尾，所以用中间部分更准确
            int midStart = Math.max(0, normalizedSegment.length() / 2 - 40);
            int midEnd = Math.min(normalizedSegment.length(), midStart + 80);
            String midFeature = normalizedSegment.substring(midStart, midEnd).trim();

            if (midFeature.length() >= 20) {
                log.debug("策略1：使用中间特征文本: {}", midFeature.substring(0, Math.min(50, midFeature.length())));

                for (int page = 1; page <= pdfDoc.getNumberOfPages(); page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String pageText = stripper.getText(pdfDoc);
                    String normalizedPageText = pageText.replaceAll("\\s+", " ").trim();

                    if (normalizedPageText.contains(midFeature)) {
                        log.info("✓ 分片定位到第 {} 页（策略1：中间特征匹配）", page);
                        return page;
                    }
                }
            }

            // 策略2：使用分片后半部分的特征（避免开头跨页）
            int latterStart = Math.max(0, (int)(normalizedSegment.length() * 0.6));
            int latterEnd = Math.min(normalizedSegment.length(), latterStart + 60);
            String latterFeature = normalizedSegment.substring(latterStart, latterEnd).trim();

            if (latterFeature.length() >= 20) {
                log.debug("策略2：使用后半部分特征: {}", latterFeature.substring(0, Math.min(40, latterFeature.length())));

                for (int page = 1; page <= pdfDoc.getNumberOfPages(); page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String pageText = stripper.getText(pdfDoc);
                    String normalizedPageText = pageText.replaceAll("\\s+", " ").trim();

                    if (normalizedPageText.contains(latterFeature)) {
                        log.info("✓ 分片定位到第 {} 页（策略2：后半部分匹配）", page);
                        return page;
                    }
                }
            }

            // 策略3：使用开头特征（兜底，可能不准确）
            String headFeature = normalizedSegment.substring(0, Math.min(80, normalizedSegment.length())).trim();
            log.debug("策略3：使用开头特征: {}", headFeature.substring(0, Math.min(50, headFeature.length())));

            for (int page = 1; page <= pdfDoc.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(pdfDoc);
                String normalizedPageText = pageText.replaceAll("\\s+", " ").trim();

                if (normalizedPageText.contains(headFeature)) {
                    log.info("✓ 分片定位到第 {} 页（策略3：开头特征匹配）", page);
                    return page;
                }
            }

            // 策略4：基于全文位置估算
            log.debug("策略4：基于位置估算");
            String normalizedFull = fullText.replaceAll("\\s+", " ").trim();
            int position = normalizedFull.indexOf(normalizedSegment);

            if (position != -1) {
                double relativePosition = (double) position / normalizedFull.length();
                int estimatedPage = (int) Math.ceil(relativePosition * pdfDoc.getNumberOfPages());
                estimatedPage = Math.max(1, Math.min(estimatedPage, pdfDoc.getNumberOfPages()));
                log.info("✓ 分片定位到第 {} 页（策略4：位置估算）", estimatedPage);
                return estimatedPage;
            }

            log.warn("⚠ 所有策略失败，返回第1页");
        } catch (Exception e) {
            log.error("计算 PDF 页码失败", e);
        }
        return 1;
    }

    /**
     * 计算行号范围
     */
    private int[] calculateLineRange(String fullContent, String segmentText) {
        int startIndex = fullContent.indexOf(segmentText);
        if (startIndex == -1) {
            return new int[]{1, 1};
        }

        String beforeSegment = fullContent.substring(0, startIndex);
        int lineStart = beforeSegment.split("\n", -1).length;
        int lineEnd = lineStart + segmentText.split("\n", -1).length - 1;

        return new int[]{lineStart, lineEnd};
    }

    /**
     * 保存文件到 MinIO
     * @deprecated 请使用 minioService.uploadKnowledgeFile() 方法
     */
    @Deprecated
    private String saveFile(MultipartFile file) throws IOException {
        return minioService.uploadKnowledgeFile(file, file.getOriginalFilename());
    }

    /**
     * 从 MinIO 下载文件到临时目录
     * @param objectName MinIO 对象名
     * @param suffix 临时文件后缀
     * @return 临时文件路径
     */
    private Path downloadToTempFile(String objectName, String suffix) throws IOException {
        java.io.InputStream inputStream = minioService.getFileStream(objectName, minioService.getKnowledgeBucket());
        if (inputStream == null) {
            throw new IOException("无法从 MinIO 获取文件: " + objectName);
        }

        Path tempFile = Files.createTempFile("minio_", suffix);
        Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        inputStream.close();
        log.info("✓ 文件已下载到临时目录: {}", tempFile);
        return tempFile;
    }

    /**
     * 获取文档解析器
     */
    private DocumentParser getDocumentParser(String fileType) {
        String type = fileType.toLowerCase();
        return switch (type) {
            case "pdf" -> new PdfBox3DocumentParser();
            case "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> new ApachePoiDocumentParser();
            default -> new TextDocumentParser();
        };
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null) {
            return "0 B";
        }
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 获取状态文本
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 0:
                return "待处理";
            case 1:
                return "处理中";
            case 2:
                return "处理成功";
            case 3:
                return "处理失败";
            default:
                return "未知";
        }
    }

    /**
     * 检查知识库是否被智能体关联
     */
    private List<com.moyun.ext.ai.entity.Agent> checkAgentAssociation(Long knowledgeBaseId) {
        List<com.moyun.ext.ai.entity.Agent> allAgents = agentService.list();
        List<com.moyun.ext.ai.entity.Agent> associatedAgents = new ArrayList<>();

        String targetId = String.valueOf(knowledgeBaseId);

        for (com.moyun.ext.ai.entity.Agent agent : allAgents) {
            String knowledgeBaseIds = agent.getKnowledgeBaseIds();
            if (knowledgeBaseIds != null && !knowledgeBaseIds.isEmpty()) {
                // 解析知识库ID列表
                String[] ids = knowledgeBaseIds.split(",");
                for (String id : ids) {
                    if (id.trim().equals(targetId)) {
                        associatedAgents.add(agent);
                        break;
                    }
                }
            }
        }

        return associatedAgents;
    }

    /**
     * 删除知识库的所有向量数据
     *
     * <p>通过 {@link VectorStoreExtension#deleteByKnowledgeBaseId(String)} 统一删除，
     * 底层实际向量库（Redis RediSearch / Elasticsearch）由配置决定，业务层无感知。</p>
     *
     * <p>安全性保证：</p>
     * <ul>
     *   <li>按 knowledgeBaseId 精确过滤，不影响其他知识库</li>
     *   <li>删除前后都有详细日志记录</li>
     * </ul>
     */
    private void deleteVectorsByKnowledgeBaseId(Long knowledgeBaseId) {
        try {
            log.info("========== 开始删除向量数据 ==========");
            log.info("目标知识库ID: {}", knowledgeBaseId);

            int deleted = embeddingStore.deleteByKnowledgeBaseId(String.valueOf(knowledgeBaseId));

            log.info("✅ 删除向量数据完成");
            log.info("删除数量: {}", deleted);
            log.info("========================================");

            if (deleted == 0) {
                log.warn("⚠️ 未删除任何向量数据，可能该知识库没有向量数据或已被删除");
            }
        } catch (Exception e) {
            log.error("❌ 删除向量数据失败 - knowledgeBaseId: {}", knowledgeBaseId, e);
            // 不抛出，避免影响知识库删除主流程（向量数据已属辅助资源）
        }
    }

    /**
     * 转换为VO对象
     */
    private KnowledgeBaseVO convertToVO(KnowledgeBase knowledge) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        vo.setId(knowledge.getId());
        vo.setFileName(knowledge.getFileName());
        vo.setFilePath(knowledge.getFilePath());
        vo.setFileSize(formatFileSize(knowledge.getFileSize()));
        vo.setFileType(knowledge.getFileType());
        vo.setSegmentCount(knowledge.getSegmentCount());
        vo.setVectorDimension(knowledge.getVectorDimension());
        vo.setStatus(knowledge.getStatus());
        vo.setStatusText(getStatusText(knowledge.getStatus()));
        vo.setProcessingStatus(knowledge.getProcessingStatus());
        vo.setConfigCompleted(knowledge.getConfigCompleted());
        vo.setErrorMessage(knowledge.getErrorMessage());
        vo.setUploadTime(knowledge.getUploadTime());
        vo.setProcessTime(knowledge.getProcessTime());

        // 新增字段
        vo.setCategory(knowledge.getCategory());
        vo.setTags(knowledge.getTags());
        vo.setDescription(knowledge.getDescription());
        vo.setUsageCount(knowledge.getUsageCount());
        vo.setHitCount(knowledge.getHitCount());
        vo.setLastUsedTime(knowledge.getLastUsedTime());
        vo.setParseMethod(knowledge.getParseMethod());

        return vo;
    }

    /**
     * 处理 Word 文档
     * 从 MinIO 下载文件到临时目录进行处理
     *
     * <p>解析策略：</p>
     * <ol>
     *     <li>优先使用 LibreOffice 转换后的 PDF 进行解析（格式最准确）</li>
     *     <li>对于 .doc 文件：使用 POI 直接提取文本</li>
     *     <li>对于 .docx 文件：使用 Apache POI 解析器</li>
     * </ol>
     */
    private void processWordDocument(KnowledgeBase knowledge) throws Exception {
        log.info("开始处理 Word 文档: {}", knowledge.getFileName());

        Document document;
        String parseMethod;
        Path tempFile = null;

        try {
            // 优先使用转换后的 PDF 进行解析（格式更准确）
            String pdfObjectName = knowledge.getPdfFilePath();
            if (pdfObjectName != null && pdfObjectName.endsWith(".pdf")) {
                log.info("使用转换后的 PDF 解析: {}", pdfObjectName);
                tempFile = downloadToTempFile(pdfObjectName, ".pdf");
                DocumentParser pdfParser = new PdfBox3DocumentParser();
                try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                    document = pdfParser.parse(fis);
                    parseMethod = "PDFBox";
                }
            } else if ("doc".equalsIgnoreCase(knowledge.getFileType())) {
                // .doc 文件使用 POI 直接解析
                log.info("使用 POI 解析 .doc 文件");
                tempFile = downloadToTempFile(knowledge.getFilePath(), ".doc");
                document = parseDocDirectly(tempFile.toString());
                parseMethod = "POI";
            } else {
                // .docx 文件使用标准 POI 解析器
                tempFile = downloadToTempFile(knowledge.getFilePath(), ".docx");
                DocumentParser parser = new ApachePoiDocumentParser();
                try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                    document = parser.parse(fis);
                }
                parseMethod = "POI";
            }

            log.info("📄 Word 文档解析完成 - 解析方式: {}, 文件: {}", parseMethod, knowledge.getFileName());

            // 保存解析方式到知识库记录
            knowledge.setParseMethod(parseMethod);

            processWordDocumentInternal(knowledge, document);
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * 处理 Word 文档内部逻辑
     */
    private void processWordDocumentInternal(KnowledgeBase knowledge, Document document) throws Exception {

        // 🚀 自适应分片策略
        String contentSample = document.text().substring(0, Math.min(1000, document.text().length()));
        DocumentSplitter splitter = createAdaptiveDocumentSplitter(knowledge.getId(), knowledge.getFileName(), contentSample);
        List<TextSegment> segments = splitter.split(document);

        // 生成向量ID（用于关联分片，不再保存到knowledge表）
        String vectorId = UUID.randomUUID().toString();
        knowledge.setSegmentCount(segments.size());
        // 维度将在第一次 embedding 后获取

        // 处理每个分片（类似文本文档，记录行号）
        String fullText = document.text();

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String embeddingId = vectorId + "_" + i;

            // 计算行号
            int[] lineRange = calculateLineRange(fullText, segment.text());
            int lineStart = lineRange[0];
            int lineEnd = lineRange[1];

            log.info("分片 {} 定位到第 {}-{} 行", i, lineStart, lineEnd);

            // 添加元数据
            segment.metadata().put("vectorId", vectorId);
            segment.metadata().put("fileName", knowledge.getFileName());
            segment.metadata().put("fileType", knowledge.getFileType());
            segment.metadata().put("segmentIndex", String.valueOf(i));
            segment.metadata().put("knowledgeBaseId", String.valueOf(knowledge.getId()));
            segment.metadata().put("embeddingId", embeddingId);
            segment.metadata().put("lineStart", String.valueOf(lineStart));
            segment.metadata().put("lineEnd", String.valueOf(lineEnd));

            // 生成向量并存储
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            // 在第一次 embedding 后设置向量维度
            if (i == 0) {
                knowledge.setVectorDimension(embedding.dimension());
            }

            // 保存到数据库
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setLineStart(lineStart);
            docSegment.setLineEnd(lineEnd);
            docSegment.setContent(segment.text());
            docSegment.setContentLength(segment.text().length());
            docSegment.setEmbeddingId(embeddingId);
            docSegment.setVectorDimension(embedding.dimension());

            try {
                String vectorJson = objectMapper.writeValueAsString(embedding.vector());
                docSegment.setVectorData(vectorJson);
            } catch (Exception e) {
                log.warn("向量数据序列化失败: {}", e.getMessage());
            }

            docSegment.setCreateTime(LocalDateTime.now());
            documentSegmentService.save(docSegment);
        }

        // 保存向量维度到数据库
        this.updateById(knowledge);
        log.info("✅ Word 文档处理完成，共 {} 个分片，向量维度: {}", segments.size(), knowledge.getVectorDimension());
    }

    /**
     * 直接解析 .doc 文件（最终备用方法）
     *
     * <p>使用 Apache POI 的 HWPFDocument 直接提取文本</p>
     * <p>注意：此方法可能丢失格式信息，仅提取纯文本</p>
     *
     * @param filePath 临时文件路径
     * @return 解析后的文档
     * @throws Exception 解析失败时抛出异常
     */
    private Document parseDocDirectly(String filePath) throws Exception {
        log.info("使用 POI 直接解析 .doc 文件");
        try (java.io.InputStream fis = Files.newInputStream(Path.of(filePath))) {
            org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(fis);
            // 直接使用 Range 提取文本，避免 WordExtractor 的 bug
            String text = doc.getRange().text();
            log.info("✅ POI 提取文本成功，长度: {} 字符", text.length());
            return Document.from(text);
        } catch (Exception e) {
            log.error("❌ POI 解析 .doc 失败: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 处理 Excel 文档
     */
    private void processExcelDocument(KnowledgeBase knowledge) throws Exception {
        log.info("开始处理 Excel 文档: {}", knowledge.getFileName());

        // 从 MinIO 下载文件到临时目录
        Path tempFile = downloadToTempFile(knowledge.getFilePath(), "." + knowledge.getFileType());
        try {
            // 记录解析方式
            knowledge.setParseMethod("POI");

            // 使用 Apache POI 解析器
            DocumentParser parser = new ApachePoiDocumentParser();
            Document document;
            try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                document = parser.parse(fis);
            }

            // 🚀 自适应分片策略（Excel 表格文档）
            String contentSample = document.text().substring(0, Math.min(1000, document.text().length()));
            DocumentSplitter splitter = createAdaptiveDocumentSplitter(knowledge.getId(), knowledge.getFileName(), contentSample);
            List<TextSegment> segments = splitter.split(document);

            processExcelDocumentInternal(knowledge, segments);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void processExcelDocumentInternal(KnowledgeBase knowledge, List<TextSegment> segments) throws Exception {

        // 生成向量ID（用于关联分片，不再保存到knowledge表）
        String vectorId = UUID.randomUUID().toString();
        knowledge.setSegmentCount(segments.size());
        // 维度将在第一次 embedding 后获取

        // 处理每个分片
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String embeddingId = vectorId + "_" + i;

            log.info("处理 Excel 分片 {}", i);

            // 添加元数据
            segment.metadata().put("vectorId", vectorId);
            segment.metadata().put("fileName", knowledge.getFileName());
            segment.metadata().put("fileType", knowledge.getFileType());
            segment.metadata().put("segmentIndex", String.valueOf(i));
            segment.metadata().put("knowledgeBaseId", String.valueOf(knowledge.getId()));
            segment.metadata().put("embeddingId", embeddingId);

            // 生成向量并存储
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            // 在第一次 embedding 后设置向量维度
            if (i == 0) {
                knowledge.setVectorDimension(embedding.dimension());
            }

            // 保存到数据库
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setContent(segment.text());
            docSegment.setContentLength(segment.text().length());
            docSegment.setEmbeddingId(embeddingId);
            docSegment.setVectorDimension(embedding.dimension());

            try {
                String vectorJson = objectMapper.writeValueAsString(embedding.vector());
                docSegment.setVectorData(vectorJson);
            } catch (Exception e) {
                log.warn("向量数据序列化失败: {}", e.getMessage());
            }

            docSegment.setCreateTime(LocalDateTime.now());
            documentSegmentService.save(docSegment);
        }

        // 保存向量维度到数据库
        this.updateById(knowledge);
        log.info("✅ Excel 文档处理完成，共 {} 个分片，向量维度: {}", segments.size(), knowledge.getVectorDimension());
    }

    /**
     * 处理 PowerPoint 文档
     */
    private void processPowerPointDocument(KnowledgeBase knowledge) throws Exception {
        log.info("开始处理 PowerPoint 文档: {}", knowledge.getFileName());

        // 从 MinIO 下载文件到临时目录
        Path tempFile = downloadToTempFile(knowledge.getFilePath(), "." + knowledge.getFileType());
        try {
            // 记录解析方式
            knowledge.setParseMethod("POI");

            // 使用 Apache POI 解析器
            DocumentParser parser = new ApachePoiDocumentParser();
            Document document;
            try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                document = parser.parse(fis);
            }

            // 文档分割 - TODO: 应使用knowledge_config配置
            DocumentSplitter splitter = DocumentSplitters.recursive(800, 100);
            List<TextSegment> segments = splitter.split(document);

            processPowerPointDocumentInternal(knowledge, segments);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void processPowerPointDocumentInternal(KnowledgeBase knowledge, List<TextSegment> segments) throws Exception {
        // 生成向量ID（用于关联分片，不再保存到knowledge表）
        String vectorId = UUID.randomUUID().toString();
        knowledge.setSegmentCount(segments.size());
        // 维度将在第一次 embedding 后获取

        // 处理每个分片（PPT 可以记录大致的幻灯片位置）
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String embeddingId = vectorId + "_" + i;

            // 估算幻灯片编号（假设每个分片对应一个幻灯片）
            int slideNumber = i + 1;

            log.info("处理 PowerPoint 分片 {}，估算幻灯片 {}", i, slideNumber);

            // 添加元数据
            segment.metadata().put("vectorId", vectorId);
            segment.metadata().put("fileName", knowledge.getFileName());
            segment.metadata().put("fileType", knowledge.getFileType());
            segment.metadata().put("segmentIndex", String.valueOf(i));
            segment.metadata().put("knowledgeBaseId", String.valueOf(knowledge.getId()));
            segment.metadata().put("embeddingId", embeddingId);
            segment.metadata().put("pageNumber", String.valueOf(slideNumber)); // 幻灯片编号

            // 生成向量并存储
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);

            // 在第一次 embedding 后设置向量维度
            if (i == 0) {
                knowledge.setVectorDimension(embedding.dimension());
            }

            // 保存到数据库
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setPageNumber(slideNumber); // 幻灯片编号
            docSegment.setContent(segment.text());
            docSegment.setContentLength(segment.text().length());
            docSegment.setEmbeddingId(embeddingId);
            docSegment.setVectorDimension(embedding.dimension());

            try {
                String vectorJson = objectMapper.writeValueAsString(embedding.vector());
                docSegment.setVectorData(vectorJson);
            } catch (Exception e) {
                log.warn("向量数据序列化失败: {}", e.getMessage());
            }

            docSegment.setCreateTime(LocalDateTime.now());
            documentSegmentService.save(docSegment);
        }

        // 保存向量维度到数据库
        this.updateById(knowledge);
        log.info("✅ PowerPoint 文档处理完成，共 {} 个分片，向量维度: {}", segments.size(), knowledge.getVectorDimension());
    }

    /**
     * 如果需要，将文档转换为 PDF
     * 从 MinIO 下载文件到临时目录，转换后再上传回 MinIO
     */
    private String convertToPdfIfNeeded(String originalObjectName, String fileType) {
        try {
            String type = fileType.toLowerCase();
            log.info("📄 开始文件类型转换检查 - 文件类型: {}, MinIO对象: {}", type, originalObjectName);

            // 如果已经是 PDF，直接返回原路径
            if ("pdf".equals(type)) {
                log.info("✅ 文件已是PDF格式，无需转换");
                return originalObjectName;
            }

            // 从 MinIO 下载文件到临时目录
            java.io.InputStream inputStream = minioService.getFileStream(originalObjectName, minioService.getKnowledgeBucket());
            if (inputStream == null) {
                log.error("❌ 无法从 MinIO 获取文件: {}", originalObjectName);
                return originalObjectName;
            }

            // 创建临时文件
            String extension = originalObjectName.contains(".") ?
                    originalObjectName.substring(originalObjectName.lastIndexOf('.')) : "." + fileType;
            Path tempSourceFile = Files.createTempFile("minio_source_", extension);
            Files.copy(inputStream, tempSourceFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            log.info("✓ 文件已下载到临时目录: {}, 大小: {} bytes", tempSourceFile, Files.size(tempSourceFile));

            // 生成临时 PDF 文件路径
            Path tempPdfFile = Files.createTempFile("minio_pdf_", ".pdf");

            // 执行转换
            log.info("🔄 调用转换器: DocumentToPdfConverter.convertToPdf()");
            boolean success = DocumentToPdfConverter.convertToPdf(tempSourceFile.toFile(), tempPdfFile.toFile(), fileType);
            log.info("📊 转换结果: {}", success ? "成功" : "失败");

            if (success && Files.exists(tempPdfFile) && Files.size(tempPdfFile) > 0) {
                // 上传 PDF 到 MinIO
                String pdfObjectName = originalObjectName.substring(0, originalObjectName.lastIndexOf('.')) + ".pdf";
                try (java.io.InputStream pdfInputStream = Files.newInputStream(tempPdfFile)) {
                    pdfObjectName = minioService.uploadKnowledgeFile(pdfInputStream, pdfObjectName, "application/pdf");
                }
                log.info("✅ 文档转 PDF 成功并上传到 MinIO: {}, PDF大小: {} bytes",
                        pdfObjectName, Files.size(tempPdfFile));

                // 清理临时文件
                Files.deleteIfExists(tempSourceFile);
                Files.deleteIfExists(tempPdfFile);

                return pdfObjectName;
            } else {
                log.warn("⚠️ 文档转 PDF 失败，使用原文件: {}", originalObjectName);
                // 清理临时文件
                Files.deleteIfExists(tempSourceFile);
                Files.deleteIfExists(tempPdfFile);
                return originalObjectName;
            }
        } catch (Exception e) {
            log.error("❌ 文档转 PDF 异常: {}, 堆栈: ", e.getMessage(), e);
            return originalObjectName;
        }
    }

    // ==================== 新流程：两阶段处理 ====================

    /**
     * 只上传文件，不进行处理（新流程第一阶段）
     */
    @Override
    @Transactional
    public KnowledgeBase uploadFileOnly(MultipartFile file) throws Exception {
        log.info("========== 开始上传文件（不处理）==========");
        log.info("文件名: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名不能为空");
        }

        // 1. 上传文件到 MinIO
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String filePath = minioService.uploadKnowledgeFile(file, fileName);
        log.info("✅ 文件已上传到 MinIO: {}", filePath);

        // 2. 计算文件内容哈希（用于增量更新检测）
        String contentHash = null;
        if (knowledgeIncrementalService != null) {
            try {
                contentHash = knowledgeIncrementalService.calculateContentHash(file.getInputStream());
                log.info("✅ 文件哈希计算完成: {}", contentHash);
            } catch (Exception e) {
                log.warn("⚠️ 计算文件哈希失败: {}", e.getMessage());
            }
        }

        // 3. 创建知识库记录（状态：待配置）
        KnowledgeBase knowledge = new KnowledgeBase();
        knowledge.setFileName(fileName);
        knowledge.setFilePath(filePath);
        knowledge.setFileType(fileType);
        knowledge.setFileSize(file.getSize());
        knowledge.setUploadTime(LocalDateTime.now());
        knowledge.setContentHash(contentHash); // 保存哈希值

        // 新字段：设置为待配置状态
        knowledge.setProcessingStatus("pending");
        knowledge.setConfigCompleted(false);
        knowledge.setStatus(0); // 兼容旧字段

        // 3. 保存到数据库
        save(knowledge);
        log.info("✅ 知识库记录已创建，ID={}, 状态=pending", knowledge.getId());

        // 4. 创建默认配置（可选，或等用户配置）
        knowledgeConfigService.createDefaultConfig(knowledge.getId());
        log.info("✅ 默认配置已创建");

        log.info("========== 文件上传完成（等待配置）==========");
        return knowledge;
    }

    /**
     * 根据配置处理知识库（新流程第二阶段）
     * 注意：此方法会在异步线程中调用，不使用@Transactional
     */
    @Override
    public void processKnowledge(Long knowledgeId, com.moyun.ext.ai.entity.KnowledgeConfig config) throws Exception {
        log.info("========== 开始处理知识库 ==========");
        log.info("知识库ID: {}", knowledgeId);
        log.info("配置: 分段模式={}, 最大长度={}, 重叠长度={}",
                config.getSegmentMode(), config.getSegmentMaxLength(), config.getSegmentOverlapLength());

        // 添加线程信息日志
        log.info("当前线程: {}", Thread.currentThread().getName());

        // 🔒 防重复处理：尝试获取锁
        if (!progressService.tryLock(knowledgeId)) {
            log.warn("⚠️ 知识库正在处理中，跳过重复任务 - ID={}", knowledgeId);
            return;
        }

        try {
            KnowledgeBase knowledge = getById(knowledgeId);
            if (knowledge == null) {
                throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "知识库不存在: " + knowledgeId);
            }

            // 📊 初始化进度：0%
            progressService.updateProgress(knowledgeId, 0, "开始处理", "初始化");

            // 更新状态为处理中
            knowledge.setProcessingStatus("processing");
            knowledge.setStatus(1);
            updateById(knowledge);

            // 1. 转换为PDF（如果需要）- 进度10%
            progressService.updateProgress(knowledgeId, 10, "转换文档格式", "PDF转换");
            String pdfPath = convertToPdfIfNeeded(knowledge.getFilePath(), knowledge.getFileType());
            knowledge.setPdfFilePath(pdfPath);
            updateById(knowledge);
            log.info("✅ PDF转换完成: {}", pdfPath);

            // 2. 根据文件类型处理文档 - 进度20-70%
            progressService.updateProgress(knowledgeId, 20, "解析文档内容", "文档解析");
            String fileType = knowledge.getFileType().toLowerCase();
            switch (fileType) {
                case "pdf":
                    processPdfDocumentWithConfig(knowledge, config);
                    break;
                case "docx":
                case "doc":
                    processWordDocumentWithConfig(knowledge, config);
                    break;
                case "txt":
                case "md":
                    processTextDocumentWithConfig(knowledge, config);
                    break;
                case "xlsx":
                case "xls":
                    processExcelDocument(knowledge); // Excel暂时使用旧逻辑
                    break;
                case "pptx":
                case "ppt":
                    processPowerPointDocument(knowledge); // PPT使用POI解析
                    break;
                default:
                    log.warn("⚠️ 不支持的文件类型: {}", fileType);
            }
            progressService.updateProgress(knowledgeId, 70, "文档向量化完成", "向量化");

            // 3. 提取图片并向量化（如果配置了多模态）- 进度70-90%
            if (pdfPath != null && pdfPath.endsWith(".pdf")) {
                try {
                    progressService.updateProgress(knowledgeId, 75, "提取图片", "图片处理");
                    log.info("🖼️ 开始提取图片（此过程可能较慢）");
                    imageExtractionService.extractAndVectorizeImages(knowledge, embeddingModel);
                    progressService.updateProgress(knowledgeId, 90, "图片向量化完成", "图片处理");
                    log.info("✅ 图片提取和向量化完成");
                } catch (Exception e) {
                    log.error("❌ 图片提取失败: {}", e.getMessage(), e);
                    // 图片处理失败不影响整体流程
                    progressService.updateProgress(knowledgeId, 90, "图片处理失败，继续", "完成");
                }
            } else {
                progressService.updateProgress(knowledgeId, 90, "跳过图片处理", "完成");
            }

            // 4. 更新数据库状态
            progressService.updateProgress(knowledgeId, 95, "保存处理结果", "完成");
            knowledge.setProcessingStatus("completed");
            knowledge.setStatus(2);
            knowledge.setErrorMessage(null); // 清除之前的错误信息
            knowledge.setProcessTime(LocalDateTime.now());
            knowledge.setLastProcessedTime(LocalDateTime.now()); // 增量更新：记录处理时间
            knowledge.setNeedReprocess(false); // 增量更新：标记已处理
            updateById(knowledge);

            // 5. 所有操作完成后才设置100%
            progressService.updateProgress(knowledgeId, 100, "处理完成", "完成");
            log.info("========== 知识库处理完成 ==========");
            log.info("✅ 最终状态: processingStatus={}, status={}", knowledge.getProcessingStatus(), knowledge.getStatus());

            // ✅ 处理完成，保留进度信息5秒让前端获取，然后自动过期（Redis TTL）
            // 不主动清除，让Redis自动过期即可
            log.info("✅ 进度信息将在{}小时后自动过期", com.moyun.ext.ai.constant.RedisKeys.KNOWLEDGE_PROGRESS_EXPIRE_HOURS);

        } catch (Exception e) {
            log.error("❌ 处理知识库失败", e);

            // 更新失败状态
            KnowledgeBase knowledge = getById(knowledgeId);
            if (knowledge != null) {
                knowledge.setProcessingStatus("failed");
                knowledge.setStatus(3);
                knowledge.setErrorMessage(e.getMessage());
                updateById(knowledge);
            }

            // 更新进度为失败
            progressService.updateProgress(knowledgeId, -1, "处理失败: " + e.getMessage(), "失败");

            throw e;
        } finally {
            // 🔓 释放锁
            progressService.releaseLock(knowledgeId);
        }
    }

    /**
     * 使用配置处理PDF文档
     */
    private void processPdfDocumentWithConfig(KnowledgeBase knowledge, com.moyun.ext.ai.entity.KnowledgeConfig config) throws Exception {
        log.info("开始处理 PDF 文档（使用配置）: {}", knowledge.getFileName());

        // 从 MinIO 下载文件到临时目录
        String pdfObjectName = knowledge.getPdfFilePath() != null ? knowledge.getPdfFilePath() : knowledge.getFilePath();
        Path tempFile = downloadToTempFile(pdfObjectName, ".pdf");

        try {
            // 1. 解析PDF
            DocumentParser parser = new PdfBox3DocumentParser();
            Document document;

            // 使用PDDocument获取PDF以便后续计算页码
            org.apache.pdfbox.pdmodel.PDDocument pdfDoc = org.apache.pdfbox.Loader.loadPDF(tempFile.toFile());

            try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                document = parser.parse(fis);
            }

            String rawText = document.text();
            log.info("PDF原始文本长度: {} 字符", rawText.length());

            // 2. 应用预处理
            String processedText = knowledgeConfigService.preprocessText(rawText, config);
            log.info("预处理后文本长度: {} 字符", processedText.length());

            // 3. 根据配置分段
            List<TextSegment> segments = splitTextWithConfig(processedText, config);
            log.info("分段完成，共 {} 个分片", segments.size());

            // 4. 向量化并存储（传入pdfDoc用于计算页码）
            vectorizeAndStoreWithPageNumber(knowledge, segments, config, pdfDoc, document.text());

            pdfDoc.close();

            log.info("✅ PDF 文档处理完成");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 使用配置处理Word文档
     */
    private void processWordDocumentWithConfig(KnowledgeBase knowledge, com.moyun.ext.ai.entity.KnowledgeConfig config) throws Exception {
        log.info("开始处理 Word 文档（使用配置）: {}", knowledge.getFileName());

        // Word已经转换为PDF，使用PDF进行处理以获取准确页码
        String pdfObjectName = knowledge.getPdfFilePath();
        if (pdfObjectName != null && pdfObjectName.endsWith(".pdf")) {
            log.info("使用转换后的PDF进行处理: {}", pdfObjectName);

            // 从 MinIO 下载 PDF 文件
            Path tempFile = downloadToTempFile(pdfObjectName, ".pdf");
            try {
                // 设置解析方式为 PDFBox（因为最终是用 PDFBox 解析 PDF）
                knowledge.setParseMethod("PDFBox");

                // 加载PDF文档
                org.apache.pdfbox.pdmodel.PDDocument pdfDoc = org.apache.pdfbox.Loader.loadPDF(tempFile.toFile());
                DocumentParser parser = new PdfBox3DocumentParser();
                Document document;
                try (java.io.InputStream fis = Files.newInputStream(tempFile)) {
                    document = parser.parse(fis);
                }

                String rawText = document.text();
                log.info("PDF原始文本长度: {} 字符", rawText.length());

                // 应用预处理
                String processedText = knowledgeConfigService.preprocessText(rawText, config);
                log.info("预处理后文本长度: {} 字符", processedText.length());

                // 分段
                List<TextSegment> segments = splitTextWithConfig(processedText, config);
                log.info("分段完成，共 {} 个分片", segments.size());

                // 向量化并存储（带页码）
                vectorizeAndStoreWithPageNumber(knowledge, segments, config, pdfDoc, document.text());

                pdfDoc.close();
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } else {
            log.warn("⚠️ Word文档未找到PDF，使用无页码模式");

            // 降级处理：从 MinIO 下载原 Word 文档并使用 POI 解析
            Path tempFile = downloadToTempFile(knowledge.getFilePath(), "." + knowledge.getFileType());
            try {
                String rawText = parseWordWithPoi(tempFile.toString());
                String parseMethod = "POI";

                log.info("Word原始文本长度: {} 字符, 解析方式: {}", rawText.length(), parseMethod);
                knowledge.setParseMethod(parseMethod);

                // 应用预处理
                String processedText = knowledgeConfigService.preprocessText(rawText, config);
                log.info("预处理后文本长度: {} 字符", processedText.length());

                // 分段
                List<TextSegment> segments = splitTextWithConfig(processedText, config);
                log.info("分段完成，共 {} 个分片", segments.size());

                // 向量化并存储（无页码）
                vectorizeAndStore(knowledge, segments, config);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        log.info("✅ Word 文档处理完成");
    }

    /**
     * 使用配置处理文本文档
     */
    private void processTextDocumentWithConfig(KnowledgeBase knowledge, com.moyun.ext.ai.entity.KnowledgeConfig config) throws Exception {
        log.info("开始处理文本文档（使用配置）: {}", knowledge.getFileName());

        // 从 MinIO 下载文件
        Path tempFile = downloadToTempFile(knowledge.getFilePath(), "." + knowledge.getFileType());
        try {
            // 读取文本
            String rawText = Files.readString(tempFile, StandardCharsets.UTF_8);
            log.info("原始文本长度: {} 字符", rawText.length());

            // 应用预处理
            String processedText = knowledgeConfigService.preprocessText(rawText, config);
            log.info("预处理后文本长度: {} 字符", processedText.length());

            // 分段
            List<TextSegment> segments = splitTextWithConfig(processedText, config);
            log.info("分段完成，共 {} 个分片", segments.size());

            // 向量化并存储
            vectorizeAndStore(knowledge, segments, config);

            log.info("✅ 文本文档处理完成");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * 使用 POI 解析 Word 文档
     *
     * <p>对于 .doc 文件，使用 WordExtractor 直接提取文本，避免格式转换问题</p>
     * <p>注意：filePath 应该是临时文件路径</p>
     */
    private String parseWordWithPoi(String filePath) throws Exception {
        Path path = Path.of(filePath);
        String fileName = path.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".doc") && !fileName.endsWith(".docx")) {
            // .doc 文件使用 WordExtractor 直接提取文本（更稳定）
            return parseDocWithWordExtractor(filePath);
        } else {
            // .docx 文件使用标准解析器
            DocumentParser parser = new ApachePoiDocumentParser();
            try (java.io.InputStream fis = Files.newInputStream(path)) {
                Document document = parser.parse(fis);
                return document.text();
            }
        }
    }

    /**
     * 使用 WordExtractor 解析 .doc 文件
     *
     * <p>直接提取文本，不做格式转换，避免 listTables 等警告</p>
     * <p>注意：filePath 应该是临时文件路径</p>
     */
    private String parseDocWithWordExtractor(String filePath) throws Exception {
        try (java.io.InputStream fis = Files.newInputStream(Path.of(filePath));
             org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(fis)) {

            // 使用 Range 直接获取文本，比 WordExtractor 更稳定
            org.apache.poi.hwpf.usermodel.Range range = doc.getRange();
            StringBuilder text = new StringBuilder();

            for (int i = 0; i < range.numParagraphs(); i++) {
                org.apache.poi.hwpf.usermodel.Paragraph para = range.getParagraph(i);
                String paraText = para.text();
                // 清理特殊字符
                paraText = paraText.replace("\u0007", "")  // 表格单元格结束符
                                   .replace("\u0001", "")  // 嵌入对象
                                   .replace("\u0013", "")  // 字段开始
                                   .replace("\u0014", "")  // 字段分隔
                                   .replace("\u0015", ""); // 字段结束
                if (!paraText.trim().isEmpty()) {
                    text.append(paraText);
                    if (!paraText.endsWith("\n") && !paraText.endsWith("\r")) {
                        text.append("\n");
                    }
                }
            }

            log.info("✅ 使用 WordExtractor 解析 .doc 成功，提取文本长度: {} 字符", text.length());
            return text.toString();
        }
    }

    /**
     * 根据配置进行文本分段
     */
    private List<TextSegment> splitTextWithConfig(String text, com.moyun.ext.ai.entity.KnowledgeConfig config) {
        log.info("使用配置进行文本分段");
        log.info("分段参数: maxLength={}, overlap={}, separator={}",
                config.getSegmentMaxLength(), config.getSegmentOverlapLength(), config.getSegmentSeparator());

        DocumentSplitter splitter = DocumentSplitters.recursive(
                config.getSegmentMaxLength(),
                config.getSegmentOverlapLength()
        );

        Document doc = Document.from(text);
        return splitter.split(doc);
    }

    /**
     * 向量化并存储
     */
    private void vectorizeAndStore(KnowledgeBase knowledge, List<TextSegment> segments, com.moyun.ext.ai.entity.KnowledgeConfig config) {
        log.info("开始向量化，共 {} 个分片", segments.size());

        // 先生成所有向量（批量操作更高效）
        List<Embedding> embeddings = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<TextSegment> enrichedSegments = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String embeddingId = knowledge.getId() + "_segment_" + i;
            ids.add(embeddingId);

            // 添加元数据
            TextSegment enrichedSegment = TextSegment.from(
                    segment.text(),
                    Metadata.from("knowledgeBaseId", String.valueOf(knowledge.getId()))
                            .put("fileName", knowledge.getFileName())
                            .put("segmentIndex", String.valueOf(i))
                            .put("totalSegments", String.valueOf(segments.size()))
                            .put("embeddingId", embeddingId)
            );
            enrichedSegments.add(enrichedSegment);

            // 生成向量
            log.debug("生成向量 [{}/{}]", i + 1, segments.size());
            Embedding embedding = embeddingModel.embed(enrichedSegment).content();
            embeddings.add(embedding);

            // 存储到向量库
            embeddingStore.add(embedding, enrichedSegment);

            // 更新进度：70% + (i / segments.size()) * 15%，范围70%-85%
            if (i % 10 == 0 || i == segments.size() - 1) {
                int progress = 70 + (int)((i + 1.0) / segments.size() * 15);
                progressService.updateProgress(knowledge.getId(), progress,
                    String.format("向量化中 %d/%d", i + 1, segments.size()), "向量化");
            }
        }

        log.info("✅ 向量生成完成，共 {} 个向量", embeddings.size());

        // 📊 记录Embedding Token使用（文档向量化）
        try {
            ModelConfig embeddingConfig = modelConfigService.getDefaultEmbeddingConfig();
            if (embeddingConfig != null) {
                List<String> texts = segments.stream()
                        .map(TextSegment::text)
                        .collect(Collectors.toList());
                tokenUsageService.recordEmbeddingBatchUsageAsync(
                        null,  // 文档向量化不关联特定智能体
                        embeddingConfig.getModelName(),
                        embeddingConfig.getProvider(),
                        texts,
                        "embedding_document"
                );
            }
        } catch (Exception e) {
            log.warn("记录Embedding Token使用失败: {}", e.getMessage());
        }

        // 更新知识库记录
        if (!embeddings.isEmpty()) {
            knowledge.setSegmentCount(segments.size());
            knowledge.setVectorDimension(embeddings.get(0).dimension());
            updateById(knowledge);
            log.info("✅ 知识库记录已更新：segmentCount={}, vectorDimension={}",
                    segments.size(), embeddings.get(0).dimension());
        }

        // 保存文档分片记录（包含向量维度和向量数据）
        log.info("开始保存分片到数据库...");
        for (int i = 0; i < segments.size(); i++) {
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setContent(segments.get(i).text());
            docSegment.setContentLength(segments.get(i).text().length());
            docSegment.setEmbeddingId(ids.get(i));
            docSegment.setVectorDimension(embeddings.get(i).dimension());

            // 序列化向量数据
            try {
                String vectorJson = objectMapper.writeValueAsString(embeddings.get(i).vector());
                docSegment.setVectorData(vectorJson);
                log.debug("✓ 分片{}向量数据序列化成功，长度: {} 字符", i, vectorJson.length());
            } catch (Exception e) {
                log.error("❌ 分片{}向量数据序列化失败: {}", i, e.getMessage(), e);
            }

            docSegment.setCreateTime(LocalDateTime.now());

            try {
                boolean saved = documentSegmentService.save(docSegment);
                if (!saved) {
                    log.error("❌ 保存分片{}失败", i);
                    throw new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "保存分片失败: index=" + i);
                } else {
                    log.debug("✓ 分片{}保存成功: id={}, vectorDim={}, vectorDataLen={}",
                            i, docSegment.getId(),
                            docSegment.getVectorDimension(),
                            docSegment.getVectorData() != null ? docSegment.getVectorData().length() : 0);
                }

                // 更新进度：85% + (i / segments.size()) * 5%，范围85%-90%
                if (i % 10 == 0 || i == segments.size() - 1) {
                    int progress = 85 + (int)((i + 1.0) / segments.size() * 5);
                    progressService.updateProgress(knowledge.getId(), progress,
                        String.format("保存分片 %d/%d", i + 1, segments.size()), "保存");
                }
            } catch (Exception e) {
                log.error("❌ 保存分片{}到数据库异常: {}", i, e.getMessage(), e);
                throw new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "保存分片异常: index=" + i, e);
            }
        }

        log.info("✅ 向量化和存储完成，已保存 {} 个分片到数据库", segments.size());
    }

    /**
     * 向量化并存储（带页码计算）
     */
    private void vectorizeAndStoreWithPageNumber(KnowledgeBase knowledge, List<TextSegment> segments,
                                                  com.moyun.ext.ai.entity.KnowledgeConfig config,
                                                  org.apache.pdfbox.pdmodel.PDDocument pdfDoc,
                                                  String fullText) {
        log.info("开始向量化（带页码），共 {} 个分片", segments.size());

        // 先生成所有向量
        List<Embedding> embeddings = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        List<TextSegment> enrichedSegments = new ArrayList<>();
        List<Integer> pageNumbers = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            String embeddingId = knowledge.getId() + "_segment_" + i;
            ids.add(embeddingId);

            // 计算页码
            int pageNumber = calculatePdfPageNumber(pdfDoc, segment.text(), fullText);
            pageNumbers.add(pageNumber);
            log.debug("分片 {} 定位到第 {} 页", i, pageNumber);

            // 添加元数据（包含页码）
            TextSegment enrichedSegment = TextSegment.from(
                    segment.text(),
                    Metadata.from("knowledgeBaseId", String.valueOf(knowledge.getId()))
                            .put("fileName", knowledge.getFileName())
                            .put("segmentIndex", String.valueOf(i))
                            .put("totalSegments", String.valueOf(segments.size()))
                            .put("embeddingId", embeddingId)
                            .put("pageNumber", String.valueOf(pageNumber))
            );
            enrichedSegments.add(enrichedSegment);

            // 生成向量
            log.debug("生成向量 [{}/{}]", i + 1, segments.size());
            Embedding embedding = embeddingModel.embed(enrichedSegment).content();
            embeddings.add(embedding);

            // 存储到向量库
            embeddingStore.add(embedding, enrichedSegment);

            // 更新进度：70% + (i / segments.size()) * 15%，范围70%-85%
            if (i % 10 == 0 || i == segments.size() - 1) {
                int progress = 70 + (int)((i + 1.0) / segments.size() * 15);
                progressService.updateProgress(knowledge.getId(), progress,
                    String.format("向量化中 %d/%d", i + 1, segments.size()), "向量化");
            }
        }

        log.info("✅ 向量生成完成，共 {} 个向量", embeddings.size());

        // 📊 记录Embedding Token使用（文档向量化-PDF带页码）
        try {
            ModelConfig embeddingConfig = modelConfigService.getDefaultEmbeddingConfig();
            if (embeddingConfig != null) {
                List<String> texts = segments.stream()
                        .map(TextSegment::text)
                        .collect(Collectors.toList());
                tokenUsageService.recordEmbeddingBatchUsageAsync(
                        null,
                        embeddingConfig.getModelName(),
                        embeddingConfig.getProvider(),
                        texts,
                        "embedding_document"
                );
            }
        } catch (Exception e) {
            log.warn("记录Embedding Token使用失败: {}", e.getMessage());
        }

        // 更新知识库记录
        if (!embeddings.isEmpty()) {
            knowledge.setSegmentCount(segments.size());
            knowledge.setVectorDimension(embeddings.get(0).dimension());
            updateById(knowledge);
            log.info("✅ 知识库记录已更新：segmentCount={}, vectorDimension={}",
                    segments.size(), embeddings.get(0).dimension());
        }

        // 保存文档分片记录（包含向量维度、向量数据和页码）
        log.info("开始保存分片到数据库...");
        for (int i = 0; i < segments.size(); i++) {
            DocumentSegment docSegment = new DocumentSegment();
            docSegment.setKnowledgeBaseId(knowledge.getId());
            docSegment.setSegmentIndex(i);
            docSegment.setPageNumber(pageNumbers.get(i));
            docSegment.setContent(segments.get(i).text());
            docSegment.setContentLength(segments.get(i).text().length());
            docSegment.setEmbeddingId(ids.get(i));
            docSegment.setVectorDimension(embeddings.get(i).dimension());

            // 序列化向量数据
            try {
                String vectorJson = objectMapper.writeValueAsString(embeddings.get(i).vector());
                docSegment.setVectorData(vectorJson);
                log.debug("✓ 分片{}向量数据序列化成功，长度: {} 字符", i, vectorJson.length());
            } catch (Exception e) {
                log.error("❌ 分片{}向量数据序列化失败: {}", i, e.getMessage(), e);
            }

            docSegment.setCreateTime(LocalDateTime.now());

            try {
                boolean saved = documentSegmentService.save(docSegment);
                if (!saved) {
                    log.error("❌ 保存分片{}失败", i);
                    throw new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "保存分片失败: index=" + i);
                } else {
                    log.debug("✓ 分片{}保存成功: id={}, page={}, vectorDim={}, vectorDataLen={}",
                            i, docSegment.getId(), pageNumbers.get(i),
                            docSegment.getVectorDimension(),
                            docSegment.getVectorData() != null ? docSegment.getVectorData().length() : 0);
                }

                // 更新进度：85% + (i / segments.size()) * 5%，范围85%-90%
                if (i % 10 == 0 || i == segments.size() - 1) {
                    int progress = 85 + (int)((i + 1.0) / segments.size() * 5);
                    progressService.updateProgress(knowledge.getId(), progress,
                        String.format("保存分片 %d/%d", i + 1, segments.size()), "保存");
                }
            } catch (Exception e) {
                log.error("❌ 保存分片{}到数据库异常: {}", i, e.getMessage(), e);
                throw new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "保存分片异常: index=" + i, e);
            }
        }

        log.info("✅ 向量化和存储完成，已保存 {} 个分片到数据库（含页码）", segments.size());
    }

    /**
     * 更新知识库元数据
     */
    @Override
    public boolean updateMetadata(Long id, String category, String tags, String description) {
        if (id == null) {
            log.error("知识库ID不能为空");
            return false;
        }

        KnowledgeBase knowledge = getById(id);
        if (knowledge == null) {
            log.warn("知识库不存在 - ID: {}", id);
            return false;
        }

        log.info("更新知识库元数据 - ID: {}, 分组: {}, 标签数: {}",
            id, category, tags != null ? "有" : "无");

        // 允许设置为null来清空字段
        knowledge.setCategory(category);
        knowledge.setTags(tags);
        knowledge.setDescription(description);

        boolean success = updateById(knowledge);
        if (success) {
            log.info("✅ 元数据更新成功 - ID: {}", id);
        } else {
            log.error("❌ 元数据更新失败 - ID: {}", id);
        }
        return success;
    }

    /**
     * 获取知识库统计信息
     */
    @Override
    public KnowledgeStatsResponse getStats() {
        log.info("开始计算知识库统计信息");
        KnowledgeStatsResponse response = new KnowledgeStatsResponse();

        // 获取所有知识库
        List<KnowledgeBase> allKnowledge = list();
        log.debug("知识库总数: {}", allKnowledge.size());

        // 总数
        response.setTotal(allKnowledge.size());

        // 已完成数量
        long completed = allKnowledge.stream()
            .filter(kb -> "completed".equals(kb.getProcessingStatus()))
            .count();
        response.setCompleted((int) completed);

        // 总使用次数
        int totalUsage = allKnowledge.stream()
            .mapToInt(kb -> kb.getUsageCount() != null ? kb.getUsageCount() : 0)
            .sum();
        response.setTotalUsage(totalUsage);

        // 平均命中率
        double avgHitRate = allKnowledge.stream()
            .filter(kb -> kb.getUsageCount() != null && kb.getUsageCount() > 0)
            .mapToDouble(kb -> {
                int usage = kb.getUsageCount();
                int hit = kb.getHitCount() != null ? kb.getHitCount() : 0;
                return (double) hit / usage * 100;
            })
            .average()
            .orElse(0.0);
        response.setAvgHitRate(Math.round(avgHitRate * 100.0) / 100.0);

        // 热门知识库 Top 10
        List<KnowledgeBaseVO> topKnowledge = allKnowledge.stream()
            .filter(kb -> "completed".equals(kb.getProcessingStatus()))
            .sorted((a, b) -> {
                int usageA = a.getUsageCount() != null ? a.getUsageCount() : 0;
                int usageB = b.getUsageCount() != null ? b.getUsageCount() : 0;
                return Integer.compare(usageB, usageA);
            })
            .limit(10)
            .map(this::convertToVO)
            .collect(Collectors.toList());
        response.setTopKnowledge(topKnowledge);

        log.info("✅ 统计计算完成 - 总数: {}, 已完成: {}, 总使用: {}, 平均命中率: {}%",
            response.getTotal(), response.getCompleted(), response.getTotalUsage(),
            response.getAvgHitRate());

        return response;
    }

    /**
     * 测试知识库检索
     */
    @Override
    public List<com.moyun.ext.ai.dto.RetrievalTestResult> testRetrieval(Long id, com.moyun.ext.ai.dto.RetrievalTestRequest request) {
        // 参数验证
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "知识库ID不能为空");
        }
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "查询文本不能为空");
        }
        if (request.getTopK() == null || request.getTopK() < 1 || request.getTopK() > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "TopK必须在1-20之间");
        }

        log.info("开始检索测试 - 知识库ID: {}, 查询: {}, 模式: {}, TopK: {}",
            id, request.getQuery(), request.getRetrievalMode(), request.getTopK());

        KnowledgeBase knowledge = getById(id);
        if (knowledge == null) {
            log.error("知识库不存在 - ID: {}", id);
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        if (!"completed".equals(knowledge.getProcessingStatus())) {
            log.error("知识库未完成处理 - ID: {}, 状态: {}", id, knowledge.getProcessingStatus());
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_READY, "知识库未完成处理，当前状态：" + knowledge.getProcessingStatus());
        }

        try {
            // 检查嵌入模型是否已配置
            if (this.embeddingModel == null) {
                log.error("Embedding模型未配置");
                throw new BusinessException(ErrorCode.EMBEDDING_MODEL_NOT_CONFIGURED);
            }

            // 生成查询向量
            Embedding queryEmbedding = this.embeddingModel.embed(request.getQuery()).content();

            // 构建检索请求 - 添加knowledgeBaseId过滤，只检索当前文档
            int topK = request.getTopK() != null ? request.getTopK() : 5;
            // 由于会过滤图片位置，请求3倍的结果以确保有足够的有效结果
            int requestTopK = topK * 3;
            dev.langchain4j.store.embedding.filter.Filter filter =
                dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey("knowledgeBaseId")
                    .isEqualTo(id.toString());

            dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest =
                dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(requestTopK)
                    .minScore(0.0) // 不设置最小分数限制，返回所有结果
                    .filter(filter) // 添加过滤条件
                    .build();

            log.info("检索过滤条件: knowledgeBaseId = {}", id);

            // 从向量存储中检索
            dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> searchResult =
                embeddingStore.search(searchRequest);

            // 转换为结果
            List<com.moyun.ext.ai.dto.RetrievalTestResult> results = new ArrayList<>();
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            int index = 1;
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : matches) {
                // 如果已经收集够topK个结果，就停止
                if (results.size() >= topK) {
                    break;
                }

                TextSegment segment = match.embedded();
                String content = segment.text();

                // 过滤掉图片位置信息，只保留有实际文本内容的段落
                if (content != null && !content.trim().isEmpty()
                    && !content.startsWith("图片位置：")
                    && !content.contains("图片位置：第")
                    && content.length() > 10) {  // 确保有足够的文本内容

                    RetrievalTestResult result = new RetrievalTestResult();
                    result.setSegmentIndex(index++);
                    result.setContent(content);
                    result.setScore(match.score());

                    // 提取元数据
                    Metadata metadata = segment.metadata();
                    if (metadata != null) {
                        result.setMetadata(metadata.toMap().toString());
                    }

                    results.add(result);
                }
            }

            log.info("✅ 检索测试完成 - 找到 {} 个相关结果", results.size());

            // 更新使用统计（检索测试也算一次使用）
            updateUsageStats(id, results.size());

            return results;

        } catch (Exception e) {
            log.error("❌ 检索测试失败 - knowledgeId: {}", id, e);
            throw new BusinessException(ErrorCode.RAG_RETRIEVAL_FAILED, "检索测试失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量操作
     */
    /**
     * 批量操作知识库
     *
     * <p>性能优化：使用批量更新而非逐个更新</p>
     *
     * @param operation 操作类型：setCategory（设置分组）、addTags（添加标签）
     * @param ids 知识库ID列表
     * @param category 分组名称
     * @param tags 标签JSON字符串
     * @return 操作是否成功
     */
    @Override
    @Transactional
    public boolean batchOperation(String operation, List<Long> ids, String category, String tags) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        log.info("🔄 批量操作开始: operation={}, count={}", operation, ids.size());

        switch (operation) {
            case "setCategory":
                // 批量设置分组（使用SQL批量更新，避免逐个查询）
                return batchSetCategory(ids, category);

            case "addTags":
                // 批量添加标签
                return batchAddTags(ids, tags);

            default:
                log.warn("⚠️  未知的批量操作类型: {}", operation);
                return false;
        }
    }

    /**
     * 批量设置分组
     *
     * <p>使用LambdaUpdateWrapper批量更新，避免N+1查询</p>
     */
    private boolean batchSetCategory(List<Long> ids, String category) {
        try {
            LambdaUpdateWrapper<KnowledgeBase> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(KnowledgeBase::getId, ids)
                        .set(KnowledgeBase::getCategory, category);

            boolean result = this.update(updateWrapper);
            log.info("✅ 批量设置分组完成: count={}, category={}", ids.size(), category);
            return result;
        } catch (Exception e) {
            log.error("❌ 批量设置分组失败", e);
            return false;
        }
    }

    /**
     * 批量添加标签
     *
     * <p>需要逐个处理以合并现有标签，使用JsonUtils统一JSON处理</p>
     */
    private boolean batchAddTags(List<Long> ids, String tags) {
        try {
            // 解析新标签
            List<String> newTags = JsonUtils.fromJson(tags, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            if (newTags == null || newTags.isEmpty()) {
                log.warn("⚠️  新标签为空，忽略操作");
                return false;
            }

            // 批量查询需要更新的知识库
            List<KnowledgeBase> knowledgeBases = this.listByIds(ids);
            if (knowledgeBases.isEmpty()) {
                log.warn("⚠️  未找到需要更新的知识库");
                return false;
            }

            // 更新每个知识库的标签
            for (KnowledgeBase kb : knowledgeBases) {
                List<String> existingTags = new ArrayList<>();
                if (kb.getTags() != null && !kb.getTags().isEmpty()) {
                    List<String> parsed = JsonUtils.fromJson(kb.getTags(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                    if (parsed != null) {
                        existingTags = parsed;
                    }
                }

                // 合并去重
                existingTags.addAll(newTags);
                List<String> uniqueTags = existingTags.stream().distinct().collect(Collectors.toList());

                // 使用JsonUtils序列化
                String tagsJson = JsonUtils.toJson(uniqueTags);
                if (tagsJson != null) {
                    kb.setTags(tagsJson);
                }
            }

            // 批量更新
            boolean result = this.updateBatchById(knowledgeBases);
            log.info("✅ 批量添加标签完成: count={}", knowledgeBases.size());
            return result;
        } catch (Exception e) {
            log.error("❌ 批量添加标签失败", e);
            return false;
        }
    }

    /**
     * 更新知识库使用统计
     *
     * <p>性能优化：使用SQL直接更新，避免先查询再更新</p>
     * <p>线程安全：使用数据库的原子递增操作</p>
     *
     * @param knowledgeId 知识库ID
     * @param hitCount 本次命中数量
     */
    @Override
    public void updateUsageStats(Long knowledgeId, int hitCount) {
        if (knowledgeId == null) {
            return;
        }

        try {
            // 使用LambdaUpdateWrapper直接更新，避免查询-修改-更新的并发问题
            LambdaUpdateWrapper<KnowledgeBase> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(KnowledgeBase::getId, knowledgeId)
                        .setSql("usage_count = COALESCE(usage_count, 0) + 1")
                        .set(KnowledgeBase::getLastUsedTime, LocalDateTime.now());

            // 如果有命中，增加命中次数
            if (hitCount > 0) {
                updateWrapper.setSql("hit_count = COALESCE(hit_count, 0) + " + hitCount);
            }

            boolean result = this.update(updateWrapper);
            if (result) {
                log.debug("📊 更新知识库统计: knowledgeId={}, hitCount={}", knowledgeId, hitCount);
            }
        } catch (Exception e) {
            log.error("❌ 更新知识库统计失败: knowledgeId={}", knowledgeId, e);
        }
    }

    /**
     * 只上传文件，返回文档ID
     */
    @Override
    public Long uploadKnowledge(MultipartFile file) {
        try {
            KnowledgeBase kb = uploadFileOnly(file);
            return kb.getId();
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "上传文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用知识库配置处理文档
     */
    @Override
    public void processKnowledgeWithConfig(Long documentId, KnowledgeLibraryConfig config) {
        if (config == null) {
            log.warn("知识库配置为空，使用默认配置处理文档: {}", documentId);
            reprocessFile(documentId);
            return;
        }

        // 将KnowledgeLibraryConfig转换为KnowledgeConfig
        KnowledgeConfig kc = new KnowledgeConfig();
        kc.setKnowledgeId(documentId);
        kc.setSegmentMode(config.getSegmentMode());
        kc.setSegmentSeparator(config.getSegmentSeparator());
        kc.setSegmentMaxLength(config.getSegmentMaxLength());
        kc.setSegmentOverlapLength(config.getSegmentOverlapLength());
        kc.setPreprocessReplaceSpaces(config.getPreprocessReplaceSpaces());
        kc.setPreprocessRemoveUrls(config.getPreprocessRemoveUrls());
        kc.setPreprocessRemoveExtraNewlines(config.getPreprocessRemoveExtraNewlines());
        kc.setIndexMode(config.getIndexMode());
        kc.setRetrievalMode(config.getRetrievalMode());
        kc.setRetrievalTopK(config.getRetrievalTopK());
        kc.setRerankEnabled(config.getRerankEnabled());

        try {
            processKnowledge(documentId, kc);
        } catch (Exception e) {
            log.error("处理文档失败: {}", documentId, e);
            throw new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "处理文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 多文档检索测试
     */
    @Override
    public List<com.moyun.ext.ai.dto.RetrievalTestResult> testRetrievalMultiple(List<Long> documentIds, com.moyun.ext.ai.dto.RetrievalTestRequest request) {
        if (documentIds == null || documentIds.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("多文档检索测试 - documentIds: {}, query: {}, mode: {}, topK: {}",
            documentIds, request.getQuery(), request.getRetrievalMode(), request.getTopK());

        try {
            // 检查嵌入模型是否已配置
            if (this.embeddingModel == null) {
                throw new BusinessException(ErrorCode.EMBEDDING_MODEL_NOT_CONFIGURED);
            }

            // 生成查询向量
            Embedding queryEmbedding = this.embeddingModel.embed(request.getQuery()).content();

            // 构建检索请求 - 使用IsIn过滤器检索多个文档
            int topK = request.getTopK() != null ? request.getTopK() : 10;
            // 由于会过滤图片位置，请求3倍的结果以确保有足够的有效结果
            int requestTopK = topK * 3;

            // 将文档ID列表转换为字符串列表
            List<String> documentIdStrings = documentIds.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.toList());

            dev.langchain4j.store.embedding.filter.Filter filter =
                dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey("knowledgeBaseId")
                    .isIn(documentIdStrings);

            dev.langchain4j.store.embedding.EmbeddingSearchRequest searchRequest =
                dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(requestTopK)
                    .minScore(0.0)
                    .filter(filter)
                    .build();

            log.info("检索过滤条件: knowledgeBaseId in {}", documentIdStrings);

            // 从向量存储中检索
            dev.langchain4j.store.embedding.EmbeddingSearchResult<TextSegment> searchResult =
                embeddingStore.search(searchRequest);

            // 转换为结果
            List<com.moyun.ext.ai.dto.RetrievalTestResult> results = new ArrayList<>();
            List<dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            // 获取文档ID到文件名的映射
            Map<Long, String> docIdToFileName = new java.util.HashMap<>();
            for (Long docId : documentIds) {
                KnowledgeBase kb = getById(docId);
                if (kb != null) {
                    docIdToFileName.put(docId, kb.getFileName());
                }
            }

            int index = 1;
            for (dev.langchain4j.store.embedding.EmbeddingMatch<TextSegment> match : matches) {
                // 如果已经收集够topK个结果，就停止
                if (results.size() >= topK) {
                    break;
                }

                TextSegment segment = match.embedded();
                String content = segment.text();

                // 过滤掉图片位置信息，只保留有实际文本内容的段落
                if (content != null && !content.trim().isEmpty()
                    && !content.startsWith("图片位置：")
                    && !content.contains("图片位置：第")
                    && content.length() > 10) {  // 确保有足够的文本内容

                    RetrievalTestResult result = new RetrievalTestResult();
                    result.setSegmentIndex(index++);
                    result.setContent(content);
                    result.setScore(match.score());

                    // 提取元数据
                    Metadata metadata = segment.metadata();
                    if (metadata != null) {
                        result.setMetadata(metadata.toMap().toString());
                        // 提取文件名 - 使用toMap()方法获取元数据
                        Map<String, Object> metadataMap = metadata.toMap();
                        Object kbId = metadataMap.get("knowledgeBaseId");
                        if (kbId != null) {
                            try {
                                Long docId = Long.parseLong(kbId.toString());
                                result.setFileName(docIdToFileName.get(docId));
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }
                    }

                    results.add(result);
                }
            }

            log.info("✅ 多文档检索完成 - 找到 {} 个相关结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("❌ 多文档检索失败", e);
            throw new BusinessException(ErrorCode.RAG_RETRIEVAL_FAILED, "检索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修复向量维度
     * 从 document_segment 表中读取向量维度并更新到 knowledge_base 表
     */
    @Override
    public int fixVectorDimension(Long id) {
        log.info("🔧 开始修复向量维度 - knowledgeId={}", id);
        int fixedCount = 0;

        try {
            // 查询需要修复的知识库
            List<KnowledgeBase> knowledgeList;
            if (id != null) {
                KnowledgeBase knowledge = getById(id);
                if (knowledge == null) {
                    log.warn("⚠️ 知识库不存在 - ID: {}", id);
                    return 0;
                }
                knowledgeList = List.of(knowledge);
            } else {
                // 查询所有已完成处理但向量维度为空的知识库
                LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(KnowledgeBase::getProcessingStatus, "completed")
                       .and(w -> w.isNull(KnowledgeBase::getVectorDimension)
                                  .or()
                                  .eq(KnowledgeBase::getVectorDimension, 0));
                knowledgeList = list(wrapper);
                log.info("📋 找到 {} 个需要修复的知识库", knowledgeList.size());
            }

            // 逐个修复
            for (KnowledgeBase knowledge : knowledgeList) {
                try {
                    // 查询该知识库的第一个分片
                    LambdaQueryWrapper<DocumentSegment> segmentWrapper = new LambdaQueryWrapper<>();
                    segmentWrapper.eq(DocumentSegment::getKnowledgeBaseId, knowledge.getId())
                                 .isNotNull(DocumentSegment::getVectorDimension)
                                 .gt(DocumentSegment::getVectorDimension, 0)
                                 .orderByAsc(DocumentSegment::getSegmentIndex)
                                 .last("LIMIT 1");

                    DocumentSegment firstSegment = documentSegmentService.getOne(segmentWrapper);

                    if (firstSegment != null && firstSegment.getVectorDimension() != null) {
                        Integer vectorDimension = firstSegment.getVectorDimension();
                        knowledge.setVectorDimension(vectorDimension);
                        updateById(knowledge);
                        fixedCount++;
                        log.info("✅ 修复成功 - ID: {}, 向量维度: {}", knowledge.getId(), vectorDimension);
                    } else {
                        log.warn("⚠️ 未找到有效的分片向量维度 - knowledgeId: {}", knowledge.getId());
                    }
                } catch (Exception e) {
                    log.error("❌ 修复失败 - knowledgeId: {}, 错误: {}", knowledge.getId(), e.getMessage(), e);
                }
            }

            log.info("🎉 向量维度修复完成 - 共修复 {} 条记录", fixedCount);
            return fixedCount;

        } catch (Exception e) {
            log.error("❌ 修复向量维度失败", e);
            throw new BusinessException(ErrorCode.DOCUMENT_PROCESS_FAILED, "修复失败: " + e.getMessage(), e);
        }
    }

    // ==================== 自适应分片策略辅助方法 ====================

    /**
     * 获取知识库配置
     * 如果没有配置，返回默认配置
     *
     * @param knowledgeId 知识库ID
     * @return 知识库配置
     */
    private KnowledgeConfig getKnowledgeConfig(Long knowledgeId) {
        try {
            // 从数据库查询配置
            KnowledgeConfig config = knowledgeConfigService.getConfigByKnowledgeId(knowledgeId);
            
            if (config == null) {
                // 返回默认配置
                config = new KnowledgeConfig();
                config.setKnowledgeId(knowledgeId);
                config.setChunkingStrategy("fixed");
                config.setDocumentType("general");
                config.setSegmentMaxLength(800);
                config.setSegmentOverlapLength(150);
                config.setEnableSmartBoundary(true);
                config.setFaqChunkSize(400);
                config.setTechnicalChunkSize(1200);
                log.debug("使用默认分片配置: 固定大小800字符");
            } else {
                log.debug("使用知识库配置: 策略={}, 类型={}, 大小={}", 
                         config.getChunkingStrategy(), config.getDocumentType(), config.getSegmentMaxLength());
            }
            
            return config;
        } catch (Exception e) {
            log.warn("获取知识库配置失败，使用默认配置: {}", e.getMessage());
            // 返回默认配置
            KnowledgeConfig config = new KnowledgeConfig();
            config.setKnowledgeId(knowledgeId);
            config.setChunkingStrategy("fixed");
            config.setDocumentType("general");
            config.setSegmentMaxLength(800);
            config.setSegmentOverlapLength(150);
            config.setEnableSmartBoundary(true);
            return config;
        }
    }

    /**
     * 创建自适应文档分片器
     *
     * @param knowledgeId 知识库ID
     * @param fileName 文件名
     * @param contentSample 文档内容样本（用于类型检测）
     * @return 文档分片器
     */
    private DocumentSplitter createAdaptiveDocumentSplitter(Long knowledgeId, String fileName, String contentSample) {
        try {
            // 1. 获取知识库配置
            KnowledgeConfig config = getKnowledgeConfig(knowledgeId);
            
            // 2. 检测文档类型（如果配置为自适应）
            String documentType = config.getDocumentType();
            if ("adaptive".equals(config.getChunkingStrategy())) {
                documentType = documentTypeDetector.detectDocumentType(fileName, contentSample);
                log.info("✅ 自动检测文档类型: {} -> {}", fileName, documentType);
            }
            
            // 3. 获取自适应分片大小
            int chunkSize = adaptiveChunkingService.getAdaptiveChunkSize(config, documentType, contentSample);
            
            // 4. 计算重叠大小（15%重叠率）
            int overlapSize = (int) (chunkSize * 0.15);
            
            log.info("📄 分片配置: 策略={}, 类型={}, 大小={}, 重叠={}", 
                     config.getChunkingStrategy(), documentType, chunkSize, overlapSize);
            
            // 5. 创建分片器
            return DocumentSplitters.recursive(chunkSize, overlapSize);
            
        } catch (Exception e) {
            log.warn("创建自适应分片器失败，使用默认配置: {}", e.getMessage());
            // 降级到默认配置
            return DocumentSplitters.recursive(800, 150);
        }
    }
}

