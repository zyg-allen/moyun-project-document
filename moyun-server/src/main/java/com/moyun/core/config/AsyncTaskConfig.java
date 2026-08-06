package com.moyun.core.config;

import com.moyun.util.spring.Threads;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 系统级异步任务与线程池统一配置
 *
 * <p>本类是项目线程池的<strong>唯一权威定义点</strong>，取代历史上分散在
 * {@code com.moyun.core.config.ThreadPoolConfig}（RuoYi 风格）与
 * {@code com.moyun.ext.ai.config.AsyncConfig}（AI 模块越界注册
 * {@code applicationTaskExecutor}）两处的配置。</p>
 *
 * <p>设计原则：</p>
 * <ul>
 *   <li><strong>职责分层</strong>：系统级线程池归 core 模块统一管理，不属任何业务模块；
 *       业务模块（如 AI）只保留自有专用线程池，避免越界</li>
 *   <li><strong>单一权威</strong>：{@code @EnableAsync} 与 {@code applicationTaskExecutor}
 *       只在此处声明一次，杜绝"AI 模块被禁用/移除后 Flowable 启动失败"的连锁问题</li>
 *   <li><strong>Spring 容器管理</strong>：所有线程池显式 {@code initialize()}，
 *       支持 {@code waitForTasksToCompleteOnShutdown} 优雅停机</li>
 * </ul>
 *
 * <p>Bean 矩阵：</p>
 * <ul>
 *   <li>{@code applicationTaskExecutor}（{@code @Primary}）— Spring Boot 3.x 默认异步执行器约定名，
 *       满足 Flowable 7.1.0 {@code ProcessEngineAutoConfiguration} 通过
 *       {@code @Qualifier("applicationTaskExecutor")} 解析 {@code AsyncTaskExecutor} 的依赖；
 *       同时作为项目内 {@code @Async} 默认执行器（如 ToolRegistry#logToolCallAsync）</li>
 *   <li>{@code threadPoolTaskExecutor} — 通用业务线程池（保留 RuoYi 兼容）</li>
 *   <li>{@code scheduledExecutorService} — 定时任务调度池（AsyncManager 登录日志/操作日志）</li>
 * </ul>
 *
 * <p>说明：Flowable 的 {@code flowable.async-executor-activate: false} 保持关闭，
 * Flowable 不启用自身异步执行器，仅依赖本类提供的 {@code applicationTaskExecutor} Bean 完成启动注入。</p>
 *
 * @author laomao
 * @since 2026-08-06
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncTaskConfig {

    /** CPU 核心数（用于自适应线程数计算） */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 系统级默认异步执行器（Spring Boot 3.x 约定名为 {@code applicationTaskExecutor}）。
     *
     * <p>同时满足两类调用方：</p>
     * <ol>
     *   <li>Flowable 7.1.0 启动时通过 {@code @Qualifier("applicationTaskExecutor")}
     *       解析 {@code AsyncTaskExecutor}（见 ProcessEngineAutoConfiguration#springProcessEngineConfiguration）</li>
     *   <li>项目内未显式指定 executor 的 {@code @Async} 方法（如 ToolRegistry#logToolCallAsync）</li>
     * </ol>
     *
     * <p>使用 {@code @Primary} 确保按类型注入时优先命中本 Bean，
     * 避免与 AI 模块的 {@code knowledgeProcessExecutor}、{@code workflowParallelExecutor} 产生歧义。</p>
     *
     * @return Spring 容器管理的通用异步执行器
     */
    @Primary
    @Bean(name = "applicationTaskExecutor")
    public ThreadPoolTaskExecutor applicationTaskExecutor() {
        int corePoolSize = Math.max(8, CPU_COUNT * 2);
        int maxPoolSize = corePoolSize * 2;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("app-async-");
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("✅ 系统级 applicationTaskExecutor 初始化完成: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, 500);
        return executor;
    }

    /**
     * 通用业务线程池（保留 RuoYi 风格命名，向后兼容）。
     *
     * <p>历史 {@code ThreadPoolConfig#threadPoolTaskExecutor} 迁移而来，
     * 原 RuoYi 配置未调用 {@code initialize()}，存在风格不一致问题，此处统一修复。</p>
     *
     * @return 通用业务线程池
     */
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(300);
        executor.setThreadNamePrefix("biz-pool-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 定时任务调度池（AsyncManager 使用）。
     *
     * <p>历史 {@code ThreadPoolConfig#scheduledExecutorService} 迁移而来，
     * 保留 {@code afterExecute} 异常打印行为。</p>
     *
     * @return 定时任务调度执行器
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService() {
        int corePoolSize = 50;
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }

    /**
     * 自定义拒绝策略：带日志的 CallerRunsPolicy。
     *
     * <p>任务被拒绝时记录线程池状态，便于监控调优；
     * 随后降级到调用线程执行，避免任务丢失。</p>
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
