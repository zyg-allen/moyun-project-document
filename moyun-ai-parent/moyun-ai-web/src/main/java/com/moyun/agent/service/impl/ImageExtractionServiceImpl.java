package com.moyun.agent.service.impl;

import com.moyun.agent.dto.ImageAnalysisResult;
import com.moyun.agent.dto.ImageFilterContext;
import com.moyun.agent.dto.ImagePosition;
import com.moyun.agent.entity.DocumentImage;
import com.moyun.agent.entity.KnowledgeBase;
import com.moyun.agent.entity.ModelConfig;
import com.moyun.agent.filter.ImageFilter;
import com.moyun.agent.parser.ImageAnalysisParser;
import com.moyun.agent.stats.ImageFilterStats;
import com.moyun.agent.service.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片提取服务实现类
 *
 * <p>从PDF文档中提取图片，使用多模态模型理解图片内容，并将描述向量化存储</p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class ImageExtractionServiceImpl implements ImageExtractionService {

    @Autowired
    private DocumentImageService documentImageService;

    @Autowired
    @Qualifier("embeddingStore")
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private MultimodalService multimodalService;

    @Autowired
    private TokenUsageService tokenUsageService;

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ImageFilter imageFilter;

    @Autowired
    private ImageAnalysisParser analysisParser;

    private ImageFilterStats stats = new ImageFilterStats();

    @Override
    public List<DocumentImage> extractAndVectorizeImages(KnowledgeBase knowledge, EmbeddingModel embeddingModel) {
        List<DocumentImage> result = new ArrayList<>();
        
        stats.reset();
        imageFilter.reset();
        imageFilter.setStats(stats);

        log.info("开始图片提取 - 知识库ID: {}, MinIO对象: {}", knowledge.getId(), knowledge.getFilePath());

        cleanOldImages(knowledge.getId());

        // 确定要处理的PDF对象名
        String pdfObjectName = knowledge.getPdfFilePath();
        if (pdfObjectName == null || pdfObjectName.isEmpty()) {
            pdfObjectName = knowledge.getFilePath();
        }

        // 从 MinIO 下载 PDF 文件到临时目录
        java.io.InputStream inputStream = minioService.getFileStream(pdfObjectName, minioService.getKnowledgeBucket());
        if (inputStream == null) {
            log.error("无法从 MinIO 获取文件: {}", pdfObjectName);
            return result;
        }

        java.nio.file.Path tempPdfFile = null;
        try {
            // 创建临时文件
            tempPdfFile = java.nio.file.Files.createTempFile("minio_pdf_extract_", ".pdf");
            java.nio.file.Files.copy(inputStream, tempPdfFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            log.info("✓ PDF文件已下载到临时目录: {}", tempPdfFile);

            try (PDDocument document = PDDocument.load(tempPdfFile.toFile())) {
                int totalPages = document.getNumberOfPages();
                log.info("开始提取PDF图片，共 {} 页", totalPages);

                firstPassCollectHashes(document);
                
                for (int pageNum = 0; pageNum < totalPages; pageNum++) {
                    extractImagesFromPage(document.getPage(pageNum), pageNum, knowledge, embeddingModel, result);
                }

                log.info(stats.getSummary());

                recordTokenUsage(result);
            }

        } catch (Exception e) {
            log.error("提取PDF图片失败", e);
        } finally {
            // 清理临时文件
            if (tempPdfFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempPdfFile);
                } catch (Exception e) {
                    log.warn("清理临时文件失败: {}", e.getMessage());
                }
            }
        }

        return result;
    }

    /**
     * 清理旧的图片记录
     */
    private void cleanOldImages(Long knowledgeBaseId) {
        try {
            List<DocumentImage> oldImages = documentImageService.listByKnowledgeBaseId(knowledgeBaseId);
            if (!oldImages.isEmpty()) {
                log.info("发现 {} 条旧图片记录，准备清理", oldImages.size());

                // 从 MinIO 删除旧图片
                minioService.deleteImagesByKnowledgeBaseId(knowledgeBaseId);

                documentImageService.deleteByKnowledgeBaseId(knowledgeBaseId);
            }
        } catch (Exception e) {
            log.warn("清理旧数据时出错: {}", e.getMessage());
        }
    }

    private void firstPassCollectHashes(PDDocument document) {
        log.info("第一遍扫描：收集图片hash统计重复次数");
        
        for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
            PDPage page = document.getPage(pageNum);
            PDResources resources = page.getResources();
            
            for (COSName name : resources.getXObjectNames()) {
                try {
                    PDXObject xObject = resources.getXObject(name);
                    if (!(xObject instanceof PDImageXObject)) {
                        continue;
                    }
                    
                    PDImageXObject imageXObject = (PDImageXObject) xObject;
                    BufferedImage image = imageXObject.getImage();
                    
                    if (image != null) {
                        String hash = imageFilter.calculateImageHash(image);
                        imageFilter.recordImageHash(hash);
                    }
                } catch (Exception e) {
                    log.debug("收集hash失败: {}", e.getMessage());
                }
            }
        }
        
        log.info("第一遍扫描完成");
    }

    private void extractImagesFromPage(PDPage page, int pageNum, KnowledgeBase knowledge,
                                       EmbeddingModel embeddingModel, List<DocumentImage> result) {
        PDResources resources = page.getResources();
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        int imageIndex = 0;

        for (COSName name : resources.getXObjectNames()) {
            try {
                PDXObject xObject = resources.getXObject(name);
                if (!(xObject instanceof PDImageXObject)) {
                    continue;
                }

                PDImageXObject imageXObject = (PDImageXObject) xObject;
                BufferedImage image = imageXObject.getImage();

                if (image == null) {
                    imageIndex++;
                    continue;
                }
                
                stats.setTotalExtracted(stats.getTotalExtracted() + 1);
                
                ImagePosition position = estimateImagePosition(imageIndex, pageWidth, pageHeight);
                
                processImage(image, position, pageNum + 1, imageIndex, knowledge, embeddingModel, result);
                imageIndex++;

            } catch (Exception e) {
                log.warn("处理图片失败 - 第{}页, 索引{}: {}", pageNum + 1, imageIndex, e.getMessage());
                imageIndex++;
            }
        }
    }
    
    private ImagePosition estimateImagePosition(int imageIndex, float pageWidth, float pageHeight) {
        float estimatedY = pageHeight * 0.5f;
        float estimatedX = pageWidth * 0.5f;
        
        return new ImagePosition(estimatedX, estimatedY, 0, 0, pageWidth, pageHeight);
    }

    private void processImage(BufferedImage image, ImagePosition position, int pageNumber, int imageIndex,
                              KnowledgeBase knowledge, EmbeddingModel embeddingModel,
                              List<DocumentImage> result) {
        try {
            String imageHash = imageFilter.calculateImageHash(image);
            int repeatCount = imageFilter.getRepeatCount(imageHash);
            
            ImageFilterContext context = new ImageFilterContext(image, position);
            context.setImageHash(imageHash);
            context.setRepeatCount(repeatCount);
            context.setPageNumber(pageNumber);
            context.setImageIndex(imageIndex);
            
            context.setUniqueColors(imageFilter.calculateUniqueColors(image));
            context.setEdgeDensity(imageFilter.calculateEdgeDensity(image));
            
            if (!imageFilter.shouldProcess(context)) {
                return;
            }
            
            stats.setAiAnalyzed(stats.getAiAnalyzed() + 1);
            
            String positionInfo = buildPositionInfo(position, context);
            String prompt = buildAnalysisPrompt(positionInfo);
            
            String aiResponse = multimodalService.understandImage(image, prompt);
            
            if (aiResponse == null || aiResponse.isEmpty()) {
                log.warn("AI分析返回空，跳过图片");
                stats.setAiRejectedInvalidAnalysis(stats.getAiRejectedInvalidAnalysis() + 1);
                return;
            }
            
            ImageAnalysisResult analysis = analysisParser.parse(aiResponse);
            
            if (analysis == null || !analysis.shouldStore()) {
                stats.setAiRejectedLowValue(stats.getAiRejectedLowValue() + 1);
                log.info("AI判断丢弃: 第{}页索引{}, 类型:{}, 理由:{}", 
                    pageNumber, imageIndex, analysis != null ? analysis.getImageType() : "未知",
                    analysis != null ? analysis.getReason() : "解析失败");
                return;
            }
            
            if (!analysisParser.isValidAnalysis(analysis)) {
                stats.setAiRejectedInvalidAnalysis(stats.getAiRejectedInvalidAnalysis() + 1);
                log.warn("AI分析质量不达标，跳过存储 - 第{}页索引{}", pageNumber, imageIndex);
                log.warn("AI完整响应: {}", aiResponse);
                return;
            }
            
            String detailedDescription = analysis.getDetailedDescription();
            if (detailedDescription == null || detailedDescription.length() < 20) {
                stats.setAiRejectedShortDescription(stats.getAiRejectedShortDescription() + 1);
                log.warn("描述内容过少，跳过存储 - 第{}页索引{}, 长度: {}", 
                    pageNumber, imageIndex, detailedDescription != null ? detailedDescription.length() : 0);
                log.debug("描述内容: {}", detailedDescription);
                return;
            }
            
            String imagePath = saveImage(image, knowledge.getId(), pageNumber, imageIndex);
            if (imagePath == null) {
                return;
            }
            
            Embedding embedding = embeddingModel.embed(detailedDescription).content();
            
            String embeddingId = UUID.randomUUID().toString();
            TextSegment segment = TextSegment.from(detailedDescription);
            segment.metadata().put("type", "image");
            segment.metadata().put("imagePath", imagePath);
            segment.metadata().put("pageNumber", String.valueOf(pageNumber));
            segment.metadata().put("knowledgeBaseId", String.valueOf(knowledge.getId()));
            segment.metadata().put("embeddingId", embeddingId);
            segment.metadata().put("fileName", knowledge.getFileName());
            segment.metadata().put("imageIndex", String.valueOf(imageIndex));
            segment.metadata().put("imageType", analysis.getImageType());
            if (position != null) {
                segment.metadata().put("position", position.getRegion());
            }

            embeddingStore.add(embedding, segment);

            DocumentImage docImage = new DocumentImage();
            docImage.setKnowledgeBaseId(knowledge.getId());
            docImage.setImagePath(imagePath);
            docImage.setPageNumber(pageNumber);
            docImage.setImageIndex(imageIndex);
            docImage.setEmbeddingId(embeddingId);
            docImage.setVectorDimension(embedding.dimension());
            docImage.setWidth(image.getWidth());
            docImage.setHeight(image.getHeight());
            docImage.setDescription(detailedDescription);
            docImage.setDescriptionLanguage("zh");
            docImage.setCreateTime(LocalDateTime.now());

            documentImageService.save(docImage);
            result.add(docImage);
            stats.setFinalStored(stats.getFinalStored() + 1);

            log.info("✅ 图片存储成功: 第{}页索引{}, 类型:{}, 关键词数:{}, 位置:{}", 
                pageNumber, imageIndex, analysis.getImageType(), analysis.getKeywordCount(),
                position != null ? position.getRegion() : "未知");

        } catch (Exception e) {
            log.error("处理图片失败: 第{}页索引{}", pageNumber, imageIndex, e);
        }
    }
    
    private String buildPositionInfo(ImagePosition position, ImageFilterContext context) {
        if (position == null) {
            return String.format("尺寸%dx%d", context.getWidth(), context.getHeight());
        }
        
        return String.format("位于页面%s区域，尺寸%dx%d，重复出现%d次", 
            position.getRegion(), context.getWidth(), context.getHeight(), context.getRepeatCount());
    }
    
    private String buildAnalysisPrompt(String positionInfo) {
        return "你是一个专业的图片分析专家，请仔细分析图片内容并严格按格式输出：\n\n" +
               "【图片类型】架构图/流程图/界面截图/数据图表/示意图/Logo/装饰图/纯色背景/其他\n" +
               "【信息价值】高/低（高=有文字/结构/数据，低=Logo/装饰/纯色）\n" +
               "【存储建议】存储/丢弃\n" +
               "【理由】判断依据（10字内）\n" +
               "【主题】一句话概括图片核心内容（15字内）\n" +
               "【关键词】图中所有可见文字（包括标题、标签、按钮文字、数值、代码、配置项等），用逗号分隔，至少3个\n" +
               "【关系】核心流程或结构关系（如：用户→认证→授权→访问资源）\n" +
               "【组件】图中的主要组件或模块名称（如：数据库、缓存、API网关等）\n" +
               "【补充】重要特征或技术细节（选填）\n\n" +
               "图片信息：" + positionInfo + "\n\n" +
               "⚠️ 分析要求：\n" +
               "1. 必须输出所有字段，不能省略\n" +
               "2. 【关键词】必须提取图中所有可见文字，包括：\n" +
               "   - 标题和副标题\n" +
               "   - 按钮和菜单文字\n" +
               "   - 配置项名称和值\n" +
               "   - 代码片段中的关键字\n" +
               "   - 数据表格中的列名和数值\n" +
               "   - 流程图中的步骤名称\n" +
               "3. 如果图中无文字，关键词写\"无文字内容\"\n" +
               "4. 【组件】要列出所有可识别的技术组件或模块\n" +
               "5. 【关系】要描述清楚组件之间的连接和数据流向\n" +
               "6. 总字数200字内\n\n" +
               "示例1（架构图）：\n" +
               "【图片类型】架构图\n" +
               "【信息价值】高\n" +
               "【存储建议】存储\n" +
               "【理由】包含系统架构\n" +
               "【主题】微服务系统架构\n" +
               "【关键词】用户服务,订单服务,商品服务,消息队列,Redis缓存,MySQL数据库,Nginx网关\n" +
               "【关系】客户端→Nginx→微服务→消息队列→数据库\n" +
               "【组件】用户服务,订单服务,商品服务,RabbitMQ,Redis,MySQL,Nginx\n" +
               "【补充】采用微服务架构，服务间通过消息队列异步通信\n\n" +
               "示例2（配置界面）：\n" +
               "【图片类型】界面截图\n" +
               "【信息价值】高\n" +
               "【存储建议】存储\n" +
               "【理由】包含配置信息\n" +
               "【主题】HTTPS配置界面\n" +
               "【关键词】启用HTTPS,端口号,443,证书路径,/etc/ssl/cert.pem,私钥路径,/etc/ssl/key.pem,保存,取消\n" +
               "【关系】配置项→保存→应用生效\n" +
               "【组件】HTTPS配置模块,证书管理\n" +
               "【补充】需要配置证书和私钥路径";
    }

    /**
     * 保存图片到 MinIO
     */
    private String saveImage(BufferedImage image, Long knowledgeBaseId, int pageNumber, int imageIndex) {
        try {
            // 上传图片到 MinIO
            String objectName = minioService.uploadImage(image, knowledgeBaseId, pageNumber, imageIndex);
            if (objectName == null) {
                return null;
            }
            return objectName;
        } catch (Exception e) {
            log.error("保存图片到 MinIO 失败", e);
            return null;
        }
    }

    /**
     * 记录Token使用
     */
    private void recordTokenUsage(List<DocumentImage> images) {
        if (images.isEmpty() || tokenUsageService == null) {
            return;
        }

        try {
            List<String> descriptions = images.stream()
                    .map(DocumentImage::getDescription)
                    .filter(d -> d != null && !d.isEmpty())
                    .collect(Collectors.toList());

            if (!descriptions.isEmpty()) {
                ModelConfig embeddingConfig = modelConfigService.getDefaultEmbeddingConfig();
                if (embeddingConfig != null) {
                    tokenUsageService.recordEmbeddingBatchUsageAsync(
                            null,
                            embeddingConfig.getModelName(),
                            embeddingConfig.getProvider(),
                            descriptions,
                            "embedding_image"
                    );
                }
            }
        } catch (Exception e) {
            log.warn("记录图片Embedding Token使用失败: {}", e.getMessage());
        }
    }
}
