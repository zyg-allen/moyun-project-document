package com.moyun.agent.service.impl;

import com.moyun.agent.entity.ReferenceFeedback;
import com.moyun.agent.mapper.ReferenceFeedbackMapper;
import com.moyun.agent.service.ReferenceFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 引用反馈服务实现类
 *
 * <p>收集用户对RAG检索结果的反馈，用于优化检索质量</p>
 *
 * @author laomao
 */
@Slf4j
@Service
public class ReferenceFeedbackServiceImpl implements ReferenceFeedbackService {

    @Autowired
    private ReferenceFeedbackMapper feedbackMapper;

    @Override
    public void saveFeedback(ReferenceFeedback feedback) {
        feedback.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);

        log.info("📝 收到参考来源反馈：");
        log.info("  类型：{}", feedback.getFeedbackType());
        log.info("  查询：{}", feedback.getUserQuery());
        log.info("  来源：{} 第{}页", feedback.getFileName(), feedback.getPageNumber());
        log.info("  重排分数：{}", feedback.getRerankScore());
    }
}
