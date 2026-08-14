package com.moyun.portal.judge;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalInterviewQuestionTestCaseMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * OJ 异步判题 Worker（v8.0 高并发演进）
 * <p>
 * 启动 N 个后台线程，循环从 Redis 队列 {@code BLPOP} 拉取 {@link JudgeTask}，
 * 调用 {@link JudgeEngine} 执行判题，并将 {@link JudgeResult} 回写到 {@code portal_interview_submission}。
 * <p>
 * 仅当 {@code moyun.judge.async-enabled=true} 时启用，避免开发环境空跑。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>使用固定线程池（{@link Executors#newFixedThreadPool(int)}），线程数=worker.concurrency；</li>
 *   <li>BLPOP 超时后继续下一轮，循环退出由 {@code running} 标志控制；</li>
 *   <li>任务执行异常被捕获并落库为 SYSTEM_ERROR，避免线程退出；</li>
 *   <li>支持重试：retry &lt; worker.maxRetry 时重新入队，否则落 SE 终态。</li>
 * </ul>
 *
 * @author moyun
 */
@Component
@ConditionalOnProperty(prefix = "moyun.judge", name = "async-enabled", havingValue = "true")
public class JudgeAsyncWorker {

    private static final Logger log = LoggerFactory.getLogger(JudgeAsyncWorker.class);

    @Autowired
    private JudgeQueueService queueService;
    @Autowired
    private JudgeProperties properties;
    @Autowired
    private JudgeEngine judgeEngine;
    @Autowired
    private PortalInterviewSubmissionMapper submissionMapper;
    @Autowired
    private PortalInterviewQuestionMapper questionMapper;
    @Autowired
    private PortalInterviewQuestionTestCaseMapper testCaseMapper;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        int concurrency = Math.max(1, properties.getWorker().getConcurrency());
        running.set(true);
        executor = Executors.newFixedThreadPool(concurrency, r -> {
            Thread t = new Thread(r, "oj-judge-worker");
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < concurrency; i++) {
            executor.submit(this::workerLoop);
        }
        log.info("[OJ-Worker] 异步判题 Worker 已启动，并发={} 队列={}",
                concurrency, properties.getQueue().getName());
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (executor != null) {
            executor.shutdown();
            try {
                long awaitMs = properties.getWorker().getShutdownAwaitMs();
                if (!executor.awaitTermination(awaitMs, TimeUnit.MILLISECONDS)) {
                    log.warn("[OJ-Worker] 关闭超时，强制 shutdownNow");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
        log.info("[OJ-Worker] 已关闭");
    }

    private void workerLoop() {
        long timeoutSec = Math.max(1, properties.getWorker().getPopTimeoutSeconds());
        while (running.get()) {
            JudgeTask task = null;
            try {
                task = queueService.blockingPop(timeoutSec);
            } catch (Exception e) {
                log.error("[OJ-Worker] BLPOP 异常 err={} sleep 1s 重试", e.getMessage());
                sleepQuietly(1000L);
            }
            if (task == null) {
                continue;
            }
            try {
                execute(task);
            } catch (Throwable t) {
                // 任何异常都不应让 Worker 线程退出
                log.error("[OJ-Worker] 任务执行未捕获异常 submissionId={} err={}",
                        task.getSubmissionId(), t.getMessage(), t);
                markSystemError(task, "Worker 未捕获异常: " + t.getMessage());
            }
        }
    }

    private void execute(JudgeTask task) {
        long start = System.currentTimeMillis();
        Long submissionId = task.getSubmissionId();
        if (submissionId == null) {
            log.warn("[OJ-Worker] 任务缺少 submissionId，已丢弃");
            return;
        }
        PortalInterviewSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            log.warn("[OJ-Worker] submissionId={} 不存在，丢弃任务", submissionId);
            return;
        }
        // 已被其他 Worker 处理或同步执行过（重试场景常见），幂等保护
        if (submission.getStatus() != null
                && JudgeStatus.fromCode(submission.getStatus()).isFinal()) {
            log.info("[OJ-Worker] submissionId={} 已为终态 {}，跳过", submissionId, submission.getStatus());
            queueService.evictStatus(submissionId);
            return;
        }
        // 加载用例（Worker 直接读库，避免任务负载过大）
        List<PortalInterviewQuestionTestCase> cases = testCaseMapper.selectByQuestionId(task.getQuestionId());
        if (cases == null || cases.isEmpty()) {
            markSystemError(task, "题目未配置测试用例");
            return;
        }
        try {
            JudgeResult result = judgeEngine.judge(task.getLanguage(), task.getCode(), cases,
                    task.getTimeoutMs() > 0 ? task.getTimeoutMs() : properties.getTimeoutMs());
            applyResult(submission, result, task);
            queueService.evictStatus(submissionId);
            log.info("[OJ-Worker] 判题完成 submissionId={} qid={} lang={} status={} pass={}/{} 耗时={}ms",
                    submissionId, task.getQuestionId(), task.getLanguage(),
                    result.getStatus().getCode(), result.getPassedCount(), result.getTotalCount(),
                    System.currentTimeMillis() - start);
        } catch (Throwable t) {
            log.error("[OJ-Worker] 判题引擎异常 submissionId={} err={}", submissionId, t.getMessage(), t);
            // 重试：retry < maxRetry 时重新入队
            if (task.getRetry() < properties.getWorker().getMaxRetry()) {
                task.setRetry(task.getRetry() + 1);
                queueService.enqueue(task);
                log.info("[OJ-Worker] submissionId={} 重试入队 retry={}", submissionId, task.getRetry());
            } else {
                markSystemError(task, "判题引擎异常（已耗尽重试）: " + t.getMessage());
            }
        }
    }

    /** 回写判题结果到 submission 记录，并更新题目通过率统计 */
    private void applyResult(PortalInterviewSubmission submission, JudgeResult result, JudgeTask task) {
        submission.setStatus(result.getStatus().getCode());
        submission.setIsSuccess(result.getStatus().isAccepted());
        submission.setRuntime(result.getMaxRuntimeMs());
        submission.setMemoryUsage(result.getMaxMemoryKb() <= 0 ? null : result.getMaxMemoryKb());
        submission.setPassedCaseCount(result.getPassedCount());
        submission.setTotalCaseCount(result.getTotalCount());
        submission.setFailedCaseId(result.getFailedCaseId());
        submission.setFailedCaseInput(result.getFailedCaseInput());
        submission.setFailedCaseExpected(result.getFailedCaseExpected());
        submission.setFailedCaseActual(result.getFailedCaseActual());
        submission.setErrorMessage(result.getErrorMessage());
        submissionMapper.updateById(submission);

        // 题目统计：异步路径下，提交数已在入队前自增，这里仅刷新通过率
        try {
            PortalInterviewQuestion question = questionMapper.selectById(task.getQuestionId());
            if (question != null) {
                long total = submissionMapper.countSubmissionsByQuestion(question.getId());
                long success = submissionMapper.countSuccessByQuestion(question.getId());
                java.math.BigDecimal rate = total > 0
                        ? java.math.BigDecimal.valueOf(success * 100.0 / total)
                        : java.math.BigDecimal.ZERO;
                question.setAcceptanceRate(rate);
                questionMapper.updateById(question);
            }
        } catch (Exception e) {
            log.warn("[OJ-Worker] 题目通过率刷新失败 qid={} err={}", task.getQuestionId(), e.getMessage());
        }
    }

    /** 标记为系统错误（终态），避免队列积压 */
    private void markSystemError(JudgeTask task, String reason) {
        if (task == null || task.getSubmissionId() == null) return;
        try {
            PortalInterviewSubmission sub = submissionMapper.selectById(task.getSubmissionId());
            if (sub == null) return;
            sub.setStatus(JudgeStatus.SYSTEM_ERROR.getCode());
            sub.setIsSuccess(false);
            sub.setErrorMessage(reason);
            submissionMapper.updateById(sub);
            queueService.evictStatus(task.getSubmissionId());
            log.warn("[OJ-Worker] submissionId={} 标记 SE: {}", task.getSubmissionId(), reason);
        } catch (Exception e) {
            log.error("[OJ-Worker] 标记 SE 失败 submissionId={} err={}",
                    task.getSubmissionId(), e.getMessage(), e);
        }
    }

    private void sleepQuietly(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
