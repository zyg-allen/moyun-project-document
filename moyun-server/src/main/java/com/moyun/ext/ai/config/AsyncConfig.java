package com.moyun.ext.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 模块专用线程池配置
 *
 * <p><strong>职责边界</strong>：本类只定义 AI 模块自有专用线程池，
 * 不再承担系统级 {@code applicationTaskExecutor} 职责（已迁至
 * {@code com.moyun.core.config.AsyncTaskConfig} 统一管理）。</p>
 *
 * <p>历史背景：早期 AI 模块曾在本类用双别名"占位"注册 {@code applicationTaskExecutor}，
 * 导致 AI 模块越界承担系统级职责。重构后该职责回归 core 模块。</p>
 *
 * <p>{@code @EnableAsync} 同步迁至 core 模块，避免本类被拆分/移除时
 * 项目内 {@code @Async} 方法（如 ToolRegistry#logToolCallAsync）静默退化为同步执行。</p>
 *
 * <p>本类提供的 Bean：</p>
 * <ul>
 *   <li>{@code knowledgeProcessExecutor} — 知识库文档处理（CPU 密集型：文档解析、向量化）</li>
 *   <li>{@code workflowParallelExecutor} — AI 工作流并行分支执行（替代 WorkflowEngine 历史静态线程池）</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Configuration
public class AsyncConfig {

    /** CPU 核心数 */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 知识库处理线程池（CPU 密集型）。
     *
     * <p>用于文档解析、向量化、切片等 CPU 密集型任务。
     * 被 {@code KnowledgeBaseController} 和 {@code KnowledgeProcessRecoveryListener}
     * 通过 {@code @Qualifier("knowledgeProcessExecutor")} 显式注入使用。</p>
     *
     * <p>线程池配置：</p>
     * <ul>
     *   <li>核心线程数：max(2, CPU 核心数)</li>
     *   <li>最大线程数：核心线程数 * 2</li>
     *   <li>队列容量：50（有界队列，防止 OOM）</li>
     *   <li>拒绝策略：带日志的 CallerRunsPolicy（降级到调用线程执行）</li>
     * </ul>
     *
     * @return 知识库处理线程池
     */
    @Bean(name = "knowledgeProcessExecutor")
    public ThreadPoolTaskExecutor knowledgeProcessExecutor() {
        int corePoolSize = Math.max(2, CPU_COUNT);
        int maxPoolSize = corePoolSize * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("knowledge-");
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("✅ 知识库处理线程池初始化完成: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, 50);
        return executor;
    }

    /**
     * AI 工作流并行分支执行线程池。
     *
     * <p>替代 {@code WorkflowEngine} 历史的 {@code static final PARALLEL_EXECUTOR}
     * （{@code Executors.newFixedThreadPool} 静态字段，脱离 Spring 容器、无优雅停机、无监控）。
     * 改为 Spring Bean 后，由容器统一管理生命周期，支持应用关闭时等待任务完成。</p>
     *
     * <p>线程池配置：</p>
     * <ul>
     *   <li>核心线程数：max(4, CPU 核心数 * 2)</li>
     *   <li>最大线程数：核心线程数 * 2</li>
     *   <li>队列容量：100（并行分支任务通常较短，队列不宜过大）</li>
     *   <li>拒绝策略：带日志的 CallerRunsPolicy</li>
     * </ul>
     *
     * @return AI 工作流并行执行线程池
     */
    @Bean(name = "workflowParallelExecutor")
    public ThreadPoolTaskExecutor workflowParallelExecutor() {
        int corePoolSize = Math.max(4, CPU_COUNT * 2);
        int maxPoolSize = corePoolSize * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("workflow-parallel-");
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("✅ AI 工作流并行线程池初始化完成: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, 100);
        return executor;
    }

    /**
     * 自定义拒绝策略：带日志的 CallerRunsPolicy。
     *
     * <p>在任务被拒绝时记录日志，便于监控和调优；随后降级到调用线程执行，避免任务丢失。</p>
     */
    private static class LoggingCallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("⚠️ 线程池任务队列已满，降级到调用线程执行: pool={}, active={}, queue={}",
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size());
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
