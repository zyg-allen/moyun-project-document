package com.moyun.ext.ai.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.ext.ai.entity.KnowledgeBase;
import com.moyun.ext.ai.service.KnowledgeBaseService;
import com.moyun.ext.ai.service.KnowledgeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 知识库处理恢复监听器
 *
 * <p>应用启动时检查是否有未完成的处理任务，自动恢复处理</p>
 */
@Slf4j
@Component
public class KnowledgeProcessRecoveryListener {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    @Qualifier("knowledgeProcessExecutor")
    private Executor knowledgeProcessExecutor;

    @Autowired
    private KnowledgeConfigService knowledgeConfigService;

    /**
     * 应用启动完成后执行
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("========== 检查未完成的知识库处理任务 ==========");

        try {
            // 查询所有处理中的知识库
            LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(KnowledgeBase::getProcessingStatus, "processing")
                   .eq(KnowledgeBase::getConfigCompleted, true);

            List<KnowledgeBase> processingList = knowledgeBaseService.list(wrapper);

            if (processingList.isEmpty()) {
                log.info("✅ 没有未完成的处理任务");
                return;
            }

            log.info("⚠️ 发现 {} 个未完成的处理任务，准备恢复", processingList.size());

            for (KnowledgeBase knowledge : processingList) {
                log.info("🔄 恢复处理任务 - ID={}, 文件名={}", knowledge.getId(), knowledge.getFileName());

                // 获取配置
                com.moyun.ext.ai.entity.KnowledgeConfig config =
                    knowledgeConfigService.getConfigByKnowledgeId(knowledge.getId());

                if (config == null) {
                    log.warn("⚠️ 配置不存在，跳过 - ID={}", knowledge.getId());
                    // 更新为失败状态
                    knowledge.setProcessingStatus("failed");
                    knowledge.setStatus(3);
                    knowledge.setErrorMessage("配置丢失");
                    knowledgeBaseService.updateById(knowledge);
                    continue;
                }

                // 异步恢复处理
                final Long knowledgeId = knowledge.getId();
                final Long configId = config.getId();
                knowledgeProcessExecutor.execute(() -> {
                    log.info("【恢复任务启动】知识库ID={}, 配置ID={}, 线程={}",
                            knowledgeId, configId, Thread.currentThread().getName());
                    try {
                        knowledgeBaseService.processKnowledge(knowledgeId, config);

                        KnowledgeBase kb = knowledgeBaseService.getById(knowledgeId);
                        if (kb != null) {
                            kb.setProcessingStatus("completed");
                            kb.setStatus(2);
                            kb.setErrorMessage(null);
                            knowledgeBaseService.updateById(kb);
                            log.info("【恢复任务完成】知识库ID={}", knowledgeId);
                        }
                    } catch (Exception e) {
                        log.error("【恢复任务失败】知识库ID={}, 错误: {}", knowledgeId, e.getMessage(), e);

                        KnowledgeBase kb = knowledgeBaseService.getById(knowledgeId);
                        if (kb != null) {
                            kb.setProcessingStatus("failed");
                            kb.setStatus(3);
                            kb.setErrorMessage("恢复处理失败: " + e.getMessage());
                            knowledgeBaseService.updateById(kb);
                        }
                    }
                });
            }

            log.info("========== 恢复任务已提交 ==========");

        } catch (Exception e) {
            log.error("❌ 检查未完成任务失败", e);
        }
    }
}
