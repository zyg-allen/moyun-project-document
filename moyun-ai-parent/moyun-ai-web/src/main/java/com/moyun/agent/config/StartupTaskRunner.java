package com.moyun.agent.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyun.agent.entity.KnowledgeBase;
import com.moyun.agent.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 应用启动任务执行器
 *
 * <p>应用启动时执行，检查并修复异常状态的知识库（如上次异常终止导致的处理中状态）</p>
 *
 * @author laomao
 */
@Slf4j
@Component
public class StartupTaskRunner implements ApplicationRunner {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 应用启动后执行
     *
     * <p>检查所有处理中的知识库，将超时或异常的状态修复为失败</p>
     *
     * @param args 应用启动参数
     * @throws Exception 执行异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("========================================");
        log.info("应用启动检查：检测处理中的知识库");
        log.info("========================================");

        try {
            // 查询所有状态为"处理中"的知识库
            LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(KnowledgeBase::getProcessingStatus, "processing")
                       .or()
                       .eq(KnowledgeBase::getStatus, 1);

            List<KnowledgeBase> processingList = knowledgeBaseService.list(queryWrapper);

            if (processingList.isEmpty()) {
                log.info("✓ 没有发现处理中的知识库");
                return;
            }

            log.warn("⚠ 发现 {} 个处理中的知识库（可能是上次异常终止）", processingList.size());

            // 修复这些知识库的状态
            for (KnowledgeBase knowledge : processingList) {
                log.warn("修复知识库ID={}, 文件名={}", knowledge.getId(), knowledge.getFileName());

                // 检查是否超时（超过30分钟视为超时）
                LocalDateTime uploadTime = knowledge.getUploadTime();
                if (uploadTime != null) {
                    long minutes = ChronoUnit.MINUTES.between(uploadTime, LocalDateTime.now());
                    log.warn("  - 上传时间: {}, 已经过去 {} 分钟", uploadTime, minutes);
                }

                // 将状态改为失败
                knowledge.setProcessingStatus("failed");
                knowledge.setStatus(3);
                knowledge.setErrorMessage("系统重启导致处理中断，请重新处理");
                knowledgeBaseService.updateById(knowledge);

                log.info("  ✓ 已将状态改为失败，可以重新处理");
            }

            log.info("========================================");
            log.info("状态修复完成：共修复 {} 个知识库", processingList.size());
            log.info("========================================");

        } catch (Exception e) {
            log.error("启动检查失败", e);
        }
    }
}
