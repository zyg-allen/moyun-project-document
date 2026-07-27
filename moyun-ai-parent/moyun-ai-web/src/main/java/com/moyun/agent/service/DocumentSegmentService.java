package com.moyun.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.agent.entity.DocumentSegment;

import java.util.List;

/**
 * 文档分片服务接口
 *
 * <p>管理知识库文档的分片数据</p>
 *
 * @author laomao
 */
public interface DocumentSegmentService extends IService<DocumentSegment> {

    /**
     * 根据知识库ID获取所有分片
     *
     * @param knowledgeBaseId 知识库ID
     * @return 分片列表
     */
    List<DocumentSegment> getSegmentsByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 删除知识库的所有分片
     *
     * @param knowledgeBaseId 知识库ID
     * @return 是否删除成功
     */
    boolean deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
