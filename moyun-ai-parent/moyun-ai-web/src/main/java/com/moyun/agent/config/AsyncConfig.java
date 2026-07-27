package com.moyun.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * 
 * <p>为不同类型的异步任务配置独立的线程池，避免相互影响：</p>
 * <ul>
 *     <li>knowledgeProcessExecutor：知识库文档处理（CPU密集型）</li>
 *     <li>asyncTaskExecutor：通用异步任务（I/O密集型）</li>
 * </ul>
 * 
 * <p>线程池配置原则：</p>
 * <ul>
 *     <li>CPU密集型：核心线程数 = CPU核心数 + 1</li>
 *     <li>I/O密集型：核心线程数 = CPU核心数 * 2</li>
 *     <li>使用有界队列，防止OOM</li>
 *     <li>拒绝策略：CallerRunsPolicy，避免任务丢失</li>
 * </ul>
 *
 * @author laomao
 * @since 2025-11-30
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /** CPU核心数 */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 知识库处理线程池（CPU密集型）
     * 
     * <p>用于文档解析、向量化等CPU密集型任务</p>
     * 
     * <p>线程池配置：</p>
     * <ul>
     *   <li>核心线程数：CPU核心数</li>
     *   <li>最大线程数：CPU核心数 * 2</li>
     *   <li>队列容量：50（有界队列，防止OOM）</li>
     *   <li>拒绝策略：CallerRunsPolicy（降级到调用线程执行）</li>
     *   <li>线程空闲时间：60秒</li>
     * </ul>
     *
     * @return 线程池执行器
     */
    @Bean(name = "knowledgeProcessExecutor")
    public Executor knowledgeProcessExecutor() {
        int corePoolSize = Math.max(2, CPU_COUNT);
        int maxPoolSize = corePoolSize * 2;
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数（CPU密集型）
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        
        // 队列容量（有界队列，防止OOM）
        executor.setQueueCapacity(50);
        
        // 线程名前缀
        executor.setThreadNamePrefix("knowledge-");
        
        // 拒绝策略：由调用线程处理（避免任务丢失，但可能阻塞调用线程）
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        
        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);
        
        // 允许核心线程超时（节省资源）
        executor.setAllowCoreThreadTimeOut(true);
        
        // 等待所有任务结束后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);
        
        // 自定义初始化回调
        executor.setThreadNamePrefix("knowledge-");
        executor.initialize();
        
        log.info("✅ 知识库处理线程池初始化完成: core={}, max={}, queue={}", 
                corePoolSize, maxPoolSize, 50);
        
        return executor;
    }
    
    /**
     * 通用异步任务线程池（I/O密集型）
     * 
     * <p>用于HTTP调用、数据库查询等I/O密集型任务</p>
     * 
     * <p>线程池配置：</p>
     * <ul>
     *   <li>核心线程数：CPU核心数 * 2</li>
     *   <li>最大线程数：CPU核心数 * 4</li>
     *   <li>队列容量：200</li>
     *   <li>拒绝策略：CallerRunsPolicy</li>
     * </ul>
     *
     * @return 线程池执行器
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        int corePoolSize = Math.max(4, CPU_COUNT * 2);
        int maxPoolSize = corePoolSize * 2;
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new LoggingCallerRunsPolicy());
        executor.setKeepAliveSeconds(120);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("✅ 通用异步线程池初始化完成: core={}, max={}, queue={}", 
                corePoolSize, maxPoolSize, 200);
        
        return executor;
    }
    
    /**
     * 自定义拒绝策略：带日志的CallerRunsPolicy
     * 
     * <p>在任务被拒绝时记录日志，便于监控和调优</p>
     */
    private static class LoggingCallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("⚠️  线程池任务队列已满，降级到调用线程执行: pool={}, active={}, queue={}", 
                    executor.getPoolSize(), 
                    executor.getActiveCount(), 
                    executor.getQueue().size());
            
            // 如果线程池未关闭，由调用线程执行
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}
