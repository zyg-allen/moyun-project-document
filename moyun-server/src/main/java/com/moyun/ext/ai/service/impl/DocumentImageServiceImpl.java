package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.entity.DocumentImage;
import com.moyun.ext.ai.mapper.DocumentImageMapper;
import com.moyun.ext.ai.service.DocumentImageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentImageServiceImpl extends ServiceImpl<DocumentImageMapper, DocumentImage> implements DocumentImageService {

    @Override
    public List<DocumentImage> listByKnowledgeBaseId(Long knowledgeBaseId) {
        return this.lambdaQuery()
                .eq(DocumentImage::getKnowledgeBaseId, knowledgeBaseId)
                .orderByAsc(DocumentImage::getPageNumber)
                .orderByAsc(DocumentImage::getImageIndex)
                .list();
    }

    @Override
    public boolean deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        return this.lambdaUpdate()
                .eq(DocumentImage::getKnowledgeBaseId, knowledgeBaseId)
                .remove();
    }
}
