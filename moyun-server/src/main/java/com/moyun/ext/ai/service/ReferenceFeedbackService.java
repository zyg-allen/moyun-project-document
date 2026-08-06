package com.moyun.ext.ai.service;

import com.moyun.ext.ai.entity.ReferenceFeedback;

/**
 * 引用反馈服务接口
 *
 * <p>收集用户对RAG检索结果的反馈，用于优化检索质量</p>
 *
 * @author laomao
 */
public interface ReferenceFeedbackService {

    /**
     * 保存反馈
     *
     * @param feedback 反馈实体
     */
    void saveFeedback(ReferenceFeedback feedback);
}
