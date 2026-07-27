package com.moyun.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyun.agent.entity.DocumentImage;

import java.util.List;

/**
 * 文档图片服务接口
 *
 * <p>管理从文档中提取的图片数据</p>
 *
 * @author laomao
 */
public interface DocumentImageService extends IService<DocumentImage> {

    /**
     * 根据知识库ID查询所有图片
     *
     * @param knowledgeBaseId 知识库ID
     * @return 图片列表
     */
    List<DocumentImage> listByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * 删除知识库的所有图片
     *
     * @param knowledgeBaseId 知识库ID
     * @return 是否删除成功
     */
    boolean deleteByKnowledgeBaseId(Long knowledgeBaseId);
}
