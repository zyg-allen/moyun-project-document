package com.moyun.ext.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyun.ext.ai.entity.DocumentSegment;
import com.moyun.ext.ai.mapper.DocumentSegmentMapper;
import com.moyun.ext.ai.service.DocumentSegmentService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文档分片服务实现类
 */
@Service
public class DocumentSegmentServiceImpl extends ServiceImpl<DocumentSegmentMapper, DocumentSegment> implements DocumentSegmentService {

    @Override
    public List<DocumentSegment> getSegmentsByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<DocumentSegment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentSegment::getKnowledgeBaseId, knowledgeBaseId)
               .orderByAsc(DocumentSegment::getSegmentIndex);
        return this.list(wrapper);
    }

    @Override
    public boolean deleteByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<DocumentSegment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentSegment::getKnowledgeBaseId, knowledgeBaseId);
        return this.remove(wrapper);
    }
}
