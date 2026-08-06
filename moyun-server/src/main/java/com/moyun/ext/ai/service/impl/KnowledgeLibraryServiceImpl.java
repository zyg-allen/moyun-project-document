package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyun.ext.ai.dto.KnowledgeLibraryDTO;
import com.moyun.ext.ai.exception.BusinessException;
import com.moyun.ext.ai.exception.ErrorCode;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.entity.KnowledgeLibrary;
import com.moyun.ext.ai.entity.KnowledgeLibraryConfig;
import com.moyun.ext.ai.mapper.KnowledgeLibraryConfigMapper;
import com.moyun.ext.ai.mapper.KnowledgeLibraryMapper;
import com.moyun.ext.ai.service.KnowledgeBaseService;
import com.moyun.ext.ai.service.KnowledgeLibraryService;
import com.moyun.ext.ai.vo.KnowledgeLibraryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务实现类
 *
 * @author laomao
 */
@Slf4j
@Service
public class KnowledgeLibraryServiceImpl extends ServiceImpl<KnowledgeLibraryMapper, KnowledgeLibrary> implements KnowledgeLibraryService {

    @Autowired
    private KnowledgeLibraryConfigMapper configMapper;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createLibrary(KnowledgeLibraryDTO dto) {
        log.info("创建知识库: {}", dto.getName());

        // 1. 创建知识库主记录
        KnowledgeLibrary library = new KnowledgeLibrary();
        library.setName(dto.getName());
        library.setDescription(dto.getDescription());
        library.setIcon(dto.getIcon() != null ? dto.getIcon() : "📚");
        library.setDocumentCount(0);
        library.setTotalSegments(0);
        library.setTotalSize(0L);
        library.setUsageCount(0);
        library.setHitCount(0);
        library.setStatus("active");
        library.setIsPublic(true);
        library.setCreatedAt(LocalDateTime.now());
        library.setUpdatedAt(LocalDateTime.now());

        this.save(library);
        log.info("知识库创建成功, ID: {}", library.getId());

        // 2. 创建知识库配置
        KnowledgeLibraryConfig config = new KnowledgeLibraryConfig();
        config.setLibraryId(library.getId());

        // 如果指定了模板，使用模板配置
        if (dto.getTemplateId() != null) {
            applyTemplateConfig(config, dto.getTemplateId());
        } else {
            // 使用DTO中的配置或默认值
            config.setSegmentMode(dto.getSegmentMode() != null ? dto.getSegmentMode() : "general");
            config.setSegmentMaxLength(dto.getSegmentMaxLength() != null ? dto.getSegmentMaxLength() : 800);
            config.setSegmentOverlapLength(dto.getSegmentOverlapLength() != null ? dto.getSegmentOverlapLength() : 100);
            config.setIndexMode(dto.getIndexMode() != null ? dto.getIndexMode() : "high_quality");
            config.setRetrievalMode(dto.getRetrievalMode() != null ? dto.getRetrievalMode() : "hybrid");
            config.setRetrievalTopK(dto.getRetrievalTopK() != null ? dto.getRetrievalTopK() : 10);
            config.setRerankEnabled(dto.getRerankEnabled() != null ? dto.getRerankEnabled() : false);
            config.setPreprocessReplaceSpaces(dto.getPreprocessReplaceSpaces() != null ? dto.getPreprocessReplaceSpaces() : true);
            config.setPreprocessRemoveUrls(dto.getPreprocessRemoveUrls() != null ? dto.getPreprocessRemoveUrls() : true);
            config.setPreprocessRemoveExtraNewlines(dto.getPreprocessRemoveExtraNewlines() != null ? dto.getPreprocessRemoveExtraNewlines() : true);
        }

        config.setSegmentSeparator("\n\n");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        configMapper.insert(config);
        log.info("知识库配置创建成功");

        return library.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLibrary(KnowledgeLibraryDTO dto) {
        log.info("更新知识库: {}", dto.getId());

        KnowledgeLibrary library = this.getById(dto.getId());
        if (library == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        if (StringUtils.hasText(dto.getName())) {
            library.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            library.setDescription(dto.getDescription());
        }
        if (dto.getIcon() != null) {
            library.setIcon(dto.getIcon());
        }
        library.setUpdatedAt(LocalDateTime.now());

        this.updateById(library);

        // 更新配置
        KnowledgeLibraryConfig config = configMapper.selectByLibraryId(dto.getId());
        if (config != null) {
            if (dto.getSegmentMode() != null) config.setSegmentMode(dto.getSegmentMode());
            if (dto.getSegmentMaxLength() != null) config.setSegmentMaxLength(dto.getSegmentMaxLength());
            if (dto.getSegmentOverlapLength() != null) config.setSegmentOverlapLength(dto.getSegmentOverlapLength());
            if (dto.getIndexMode() != null) config.setIndexMode(dto.getIndexMode());
            if (dto.getRetrievalMode() != null) config.setRetrievalMode(dto.getRetrievalMode());
            if (dto.getRetrievalTopK() != null) config.setRetrievalTopK(dto.getRetrievalTopK());
            if (dto.getRerankEnabled() != null) config.setRerankEnabled(dto.getRerankEnabled());
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(config);
        }

        log.info("知识库更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLibrary(Long libraryId) {
        log.info("删除知识库: {}", libraryId);

        KnowledgeLibrary library = this.getById(libraryId);
        if (library == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        // 1. 删除所有文档（包括向量和文件）
        List<KnowledgeBase> documents = knowledgeBaseService.list(
                new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getLibraryId, libraryId)
        );

        for (KnowledgeBase doc : documents) {
            try {
                knowledgeBaseService.deleteKnowledge(doc.getId());
            } catch (Exception e) {
                log.warn("删除文档失败: {}, 错误: {}", doc.getId(), e.getMessage());
            }
        }

        // 2. 删除配置
        configMapper.delete(new LambdaQueryWrapper<KnowledgeLibraryConfig>()
                .eq(KnowledgeLibraryConfig::getLibraryId, libraryId));

        // 3. 删除知识库
        this.removeById(libraryId);

        log.info("知识库删除成功");
    }

    @Override
    public KnowledgeLibraryVO getLibraryDetail(Long libraryId) {
        KnowledgeLibrary library = this.getById(libraryId);
        if (library == null) {
            return null;
        }

        KnowledgeLibraryVO vo = convertToVO(library);

        // 获取文档列表
        List<KnowledgeBase> documents = knowledgeBaseService.list(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getLibraryId, libraryId)
                        .orderByDesc(KnowledgeBase::getUploadTime)
        );

        List<KnowledgeLibraryVO.DocumentVO> docVOs = documents.stream()
                .map(this::convertToDocumentVO)
                .collect(Collectors.toList());

        vo.setDocuments(docVOs);

        // 使用实际文档数量，确保显示准确
        vo.setDocumentCount(documents.size());

        return vo;
    }

    @Override
    public Page<KnowledgeLibraryVO> listLibraries(int page, int size, String keyword, String category) {
        Page<KnowledgeLibrary> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<KnowledgeLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeLibrary::getStatus, "active");

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(KnowledgeLibrary::getName, keyword)
                    .or().like(KnowledgeLibrary::getDescription, keyword));
        }
        wrapper.orderByDesc(KnowledgeLibrary::getCreatedAt);

        Page<KnowledgeLibrary> result = this.page(pageParam, wrapper);

        List<KnowledgeLibraryVO> vos = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 批量查询每个知识库的实际文档数量，确保显示准确
        if (!vos.isEmpty()) {
            List<Long> libraryIds = vos.stream().map(KnowledgeLibraryVO::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> documentCountMap = knowledgeBaseService.list(
                new LambdaQueryWrapper<KnowledgeBase>()
                    .select(KnowledgeBase::getLibraryId)
                    .in(KnowledgeBase::getLibraryId, libraryIds)
            ).stream()
             .collect(Collectors.groupingBy(KnowledgeBase::getLibraryId, Collectors.counting()));

            vos.forEach(vo -> vo.setDocumentCount(documentCountMap.getOrDefault(vo.getId(), 0L).intValue()));
        }

        Page<KnowledgeLibraryVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);

        return voPage;
    }

    @Override
    public List<KnowledgeLibraryVO> listAllLibraries() {
        List<KnowledgeLibrary> libraries = baseMapper.selectActiveLibraries();
        List<KnowledgeLibraryVO> vos = libraries.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 批量查询每个知识库的实际文档数量，确保显示准确
        if (!vos.isEmpty()) {
            List<Long> libraryIds = vos.stream().map(KnowledgeLibraryVO::getId).collect(Collectors.toList());
            java.util.Map<Long, Long> documentCountMap = knowledgeBaseService.list(
                new LambdaQueryWrapper<KnowledgeBase>()
                    .select(KnowledgeBase::getLibraryId)
                    .in(KnowledgeBase::getLibraryId, libraryIds)
            ).stream()
             .collect(Collectors.groupingBy(KnowledgeBase::getLibraryId, Collectors.counting()));

            vos.forEach(vo -> vo.setDocumentCount(documentCountMap.getOrDefault(vo.getId(), 0L).intValue()));
        }

        return vos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocument(Long libraryId, MultipartFile file) {
        log.info("上传文档到知识库: libraryId={}, fileName={}", libraryId, file.getOriginalFilename());

        KnowledgeLibrary library = this.getById(libraryId);
        if (library == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        // 调用原有的上传逻辑
        Long documentId = knowledgeBaseService.uploadKnowledge(file);

        // 关联到知识库
        KnowledgeBase document = knowledgeBaseService.getById(documentId);
        document.setLibraryId(libraryId);
        knowledgeBaseService.updateById(document);

        // 更新统计
        updateStatistics(libraryId);

        return documentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> uploadDocuments(Long libraryId, List<MultipartFile> files) {
        List<Long> documentIds = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Long docId = uploadDocument(libraryId, file);
                documentIds.add(docId);
            } catch (Exception e) {
                log.error("上传文档失败: {}, 错误: {}", file.getOriginalFilename(), e.getMessage());
            }
        }
        return documentIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long libraryId, Long documentId) {
        log.info("从知识库删除文档: libraryId={}, documentId={}", libraryId, documentId);

        KnowledgeBase document = knowledgeBaseService.getById(documentId);
        if (document == null || !libraryId.equals(document.getLibraryId())) {
            throw new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在或不属于该知识库");
        }

        knowledgeBaseService.deleteKnowledge(documentId);
        updateStatistics(libraryId);
    }

    @Override
    public void processDocuments(Long libraryId) {
        log.info("处理知识库文档: libraryId={}", libraryId);

        // 获取知识库配置
        KnowledgeLibraryConfig config = configMapper.selectByLibraryId(libraryId);

        // 获取待处理的文档
        List<KnowledgeBase> pendingDocs = knowledgeBaseService.list(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getLibraryId, libraryId)
                        .in(KnowledgeBase::getStatus, 0, 3)  // 待处理或失败的
        );

        for (KnowledgeBase doc : pendingDocs) {
            try {
                // 使用知识库配置处理文档
                knowledgeBaseService.processKnowledgeWithConfig(doc.getId(), config);
            } catch (Exception e) {
                log.error("处理文档失败: {}, 错误: {}", doc.getId(), e.getMessage());
            }
        }

        updateStatistics(libraryId);
    }

    @Override
    public void updateStatistics(Long libraryId) {
        baseMapper.updateStatistics(libraryId);
    }

    // ==================== 私有方法 ====================

    private KnowledgeLibraryVO convertToVO(KnowledgeLibrary library) {
        KnowledgeLibraryVO vo = new KnowledgeLibraryVO();
        BeanUtils.copyProperties(library, vo);

        // 格式化文件大小
        vo.setTotalSizeFormatted(formatFileSize(library.getTotalSize()));

        return vo;
    }

    private KnowledgeLibraryVO.DocumentVO convertToDocumentVO(KnowledgeBase doc) {
        KnowledgeLibraryVO.DocumentVO vo = new KnowledgeLibraryVO.DocumentVO();
        vo.setId(doc.getId());
        vo.setFileName(doc.getFileName());
        vo.setFileType(doc.getFileType());
        vo.setFileSize(doc.getFileSize());
        vo.setFileSizeFormatted(formatFileSize(doc.getFileSize()));
        vo.setSegmentCount(doc.getSegmentCount());
        vo.setVectorDimension(doc.getVectorDimension());
        vo.setStatus(doc.getStatus());
        vo.setProcessingStatus(doc.getProcessingStatus());
        vo.setErrorMessage(doc.getErrorMessage());
        vo.setUploadTime(doc.getUploadTime());
        vo.setProcessTime(doc.getProcessTime());
        return vo;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    private void applyTemplateConfig(KnowledgeLibraryConfig config, Long templateId) {
        // TODO: 从模板表获取配置并应用
        // 这里先使用默认值
        config.setSegmentMode("general");
        config.setSegmentMaxLength(800);
        config.setSegmentOverlapLength(100);
        config.setIndexMode("high_quality");
        config.setRetrievalMode("hybrid");
        config.setRetrievalTopK(10);
        config.setRerankEnabled(false);
        config.setPreprocessReplaceSpaces(true);
        config.setPreprocessRemoveUrls(true);
        config.setPreprocessRemoveExtraNewlines(true);
    }

    @Override
    public List<com.moyun.ext.ai.dto.RetrievalTestResult> testRetrieval(Long libraryId, com.moyun.ext.ai.dto.RetrievalTestRequest request) {
        // 验证知识库存在
        KnowledgeLibrary library = getById(libraryId);
        if (library == null) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND);
        }

        // 获取知识库下所有已完成处理的文档ID
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getLibraryId, libraryId)
               .eq(KnowledgeBase::getProcessingStatus, "completed");
        List<KnowledgeBase> documents = knowledgeBaseService.list(wrapper);

        if (documents.isEmpty()) {
            log.warn("知识库 {} 下没有已完成处理的文档", libraryId);
            return new ArrayList<>();
        }

        List<Long> documentIds = documents.stream()
            .map(KnowledgeBase::getId)
            .collect(Collectors.toList());

        log.info("知识库检索 - libraryId: {}, 文档数: {}, documentIds: {}", libraryId, documentIds.size(), documentIds);

        // 调用知识库服务进行多文档检索
        List<com.moyun.ext.ai.dto.RetrievalTestResult> results =
            knowledgeBaseService.testRetrievalMultiple(documentIds, request);

        // 更新知识库使用统计
        library.setUsageCount((library.getUsageCount() != null ? library.getUsageCount() : 0) + 1);
        library.setLastUsedTime(LocalDateTime.now());
        updateById(library);

        return results;
    }
}
