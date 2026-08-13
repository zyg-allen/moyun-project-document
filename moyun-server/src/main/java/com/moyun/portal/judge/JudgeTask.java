package com.moyun.portal.judge;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OJ 异步判题任务（v8.0 异步判题队列负载）
 * <p>
 * 由 {@code PortalJudgeServiceImpl#submitJudge} 在异步开关启用时构造并 LPUSH 到 Redis 队列，
 * {@link JudgeAsyncWorker} 通过 BLPOP 拉取后调用 {@link JudgeEngine} 执行判题，
 * 并将结果回写到 {@code portal_interview_submission} 表，前端通过 {@code /portal/judge/result/{id}} 轮询。
 *
 * @author moyun
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联 submission 记录 ID（必填，Worker 据此回写结果） */
    private Long submissionId;

    /** 题目 ID（用于 Worker 重新加载用例） */
    private Long questionId;

    /** 用户 ID（仅用于日志与权限校验） */
    private Long userId;

    /** 编程语言 */
    private String language;

    /** 用户代码（Worker 直接转发给 JudgeEngine，避免再次查库） */
    private String code;

    /** 单用例运行超时（毫秒） */
    private long timeoutMs;

    /** 重试次数（Worker 执行失败后递增，达到上限则落 SE 状态） */
    private int retry;

    /** 入队时间戳（毫秒，用于排查队列延迟） */
    private long enqueuedAt;
}
