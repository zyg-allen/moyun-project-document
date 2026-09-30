package com.moyun.ext.cms.service.impl;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.core.redis.DistributedLockUtil;
import com.moyun.ext.cms.service.interview.InterviewAgentClient;
import com.moyun.ext.cms.service.interview.InterviewChatMemoryService;
import com.moyun.ext.cms.service.interview.ScoringEngine;
import com.moyun.portal.domain.entity.PortalVoiceInterview;
import com.moyun.portal.domain.entity.PortalVoiceInterviewEvent;
import com.moyun.portal.domain.entity.PortalVoiceInterviewQA;
import com.moyun.portal.mapper.PortalVoiceInterviewEventMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewMapper;
import com.moyun.portal.mapper.PortalVoiceInterviewQAMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 答题幂等守卫测试（v13.57 批次 0 补测 / 安全网）。
 *
 * <p><b>为什么必须补</b>：批次 0 的 T2.1 是《报告七再评审》<b>R1</b> 的修复
 * ——原实现「读后不锁」会导致 <b>①答案后写覆盖前写 ②双倍 token
 * ③同一 questionIdx 插入两条主问</b>。这是本次整改中**唯一会损坏数据**的问题，
 * 却只有实现、没有测试。DB 层已有唯一约束兜底（v13.53），但应用层守卫若失效，
 * 用户仍会看到"该题已作答却又被覆盖"的错误行为。</p>
 *
 * <p><b>覆盖点</b>：两道应用层守卫（<b>分布式锁</b> + <b>已作答状态机</b>）
 * 各自的拒绝语义与事件留痕，以及"正常路径确实放行"（避免守卫过严误杀）。</p>
 *
 * <p><b>测试环境</b>：守卫都在 <b>提交 SSE 线程之前</b>抛出，故无需真实 SSE / Redis
 * —— 用 mock 的 {@code DistributedLockUtil} 精确控制"抢到锁 / 抢不到锁"。</p>
 *
 * @author laomao
 */
class AnswerIdempotencyGuardTest {

    private static final Long INTERVIEW_ID = 900001L;
    private static final Long USER_ID = 900002L;
    private static final Long QA_ID = 900003L;

    private VoiceInterviewServiceImpl service;
    private PortalVoiceInterviewMapper interviewMapper;
    private PortalVoiceInterviewQAMapper qaMapper;
    private PortalVoiceInterviewEventMapper eventMapper;
    private DistributedLockUtil lockUtil;
    private PortalVoiceInterviewQA qa;
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new VoiceInterviewServiceImpl();
        interviewMapper = mock(PortalVoiceInterviewMapper.class);
        qaMapper = mock(PortalVoiceInterviewQAMapper.class);
        eventMapper = mock(PortalVoiceInterviewEventMapper.class);
        lockUtil = mock(DistributedLockUtil.class);

        PortalVoiceInterview interview = new PortalVoiceInterview();
        interview.setId(INTERVIEW_ID);
        interview.setUserId(USER_ID);
        interview.setStatus("in_progress");
        interview.setDifficulty("medium");
        interview.setPosition("Java 后端");
        interview.setTotalQa(5);
        interview.setCurrentIdx(0);
        when(interviewMapper.selectById(anyLong())).thenReturn(interview);

        // 默认：QA 未作答（可正常提交）
        qa = new PortalVoiceInterviewQA();
        qa.setId(QA_ID);
        qa.setInterviewId(INTERVIEW_ID);
        qa.setQuestionIdx(0);
        qa.setQuestion("请自我介绍");
        qa.setUserAnswer(null);
        when(qaMapper.selectById(anyLong())).thenReturn(qa);
        when(qaMapper.updateById(any(PortalVoiceInterviewQA.class))).thenReturn(1);

        // 事件留痕捕获（用于断言"拒绝时确实记了事件"）
        when(eventMapper.insert(any(PortalVoiceInterviewEvent.class))).thenAnswer(inv -> {
            Object e = inv.getArgument(0);
            try {
                Object type = e.getClass().getMethod("getEventType").invoke(e);
                events.add(String.valueOf(type));
            } catch (Exception ignored) {
                events.add("?");
            }
            return 1;
        });

        ReflectionTestUtils.setField(service, "interviewMapper", interviewMapper);
        ReflectionTestUtils.setField(service, "qaMapper", qaMapper);
        ReflectionTestUtils.setField(service, "eventMapper", eventMapper);
        ReflectionTestUtils.setField(service, "lockUtil", lockUtil);
        ReflectionTestUtils.setField(service, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
        ReflectionTestUtils.setField(service, "scoringEngine", new ScoringEngine());
        ReflectionTestUtils.setField(service, "agentClient", (InterviewAgentClient) null);
        ReflectionTestUtils.setField(service, "memoryService", (InterviewChatMemoryService) null);
    }

    // ==================== 用例 ====================

    @Test
    @DisplayName("守卫①分布式锁：抢不到锁时拒绝重复提交，且记 answer_dup_rejected 事件")
    void rejectsWhenLockNotAcquired() {
        // 抢不到锁 = 已有同题请求在飞（并发/连点/重试）
        when(lockUtil.tryLock(any(), any(Duration.class))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitAnswer(INTERVIEW_ID, USER_ID, QA_ID, "我的作答", 1000, false));

        assertTrue(ex.getMessage().contains("正在处理中"),
                "抢不到锁应提示「正在处理中」，实际: " + ex.getMessage());
        assertTrue(events.contains("answer_dup_rejected"),
                "拒绝时必须记 answer_dup_rejected 事件（可观测性），实际事件: " + events);
        // 关键：不得写入作答（否则就是"后写覆盖前写"）
        assertTrue(qa.getUserAnswer() == null,
                "被拒绝的重复请求不得写入 userAnswer（防答案覆盖）");
    }

    @Test
    @DisplayName("守卫②状态机：该题已作答时拒绝再次进入轮次，并释放锁")
    void rejectsWhenAlreadyAnswered() {
        // 抢到锁，但该题已有作答 → 状态机守卫应拒绝
        DistributedLockUtil.Lock lock = mock(DistributedLockUtil.Lock.class);
        when(lockUtil.tryLock(any(), any(Duration.class))).thenReturn(lock);
        qa.setUserAnswer("之前的作答");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitAnswer(INTERVIEW_ID, USER_ID, QA_ID, "新的作答", 1000, false));

        assertTrue(ex.getMessage().contains("已作答"),
                "已作答题应提示「该题已作答」，实际: " + ex.getMessage());
        assertEquals("之前的作答", qa.getUserAnswer(),
                "原有作答不得被覆盖（这正是 R1 要防的数据损坏）");
        // 守卫在拒绝路径上必须释放锁，否则该题在 TTL 内无法重试
        org.mockito.Mockito.verify(lock).close();
    }

    @Test
    @DisplayName("正常路径：未作答 + 抢到锁 → 守卫放行并落库原始作答")
    void allowsNormalSubmission() {
        DistributedLockUtil.Lock lock = mock(DistributedLockUtil.Lock.class);
        when(lockUtil.tryLock(any(), any(Duration.class))).thenReturn(lock);

        try {
            service.submitAnswer(INTERVIEW_ID, USER_ID, QA_ID, "我的作答", 1234, false);
        } catch (Exception e) {
            // 守卫之后的链路（SSE/线程池）在本测试环境未装配，允许其失败；
            // 本用例只关心"守卫是否放行 + 原始作答是否落库"
        }

        assertEquals("我的作答", qa.getUserAnswer(), "正常路径必须落库原始作答（铁律：先存原始）");
        assertEquals("我的作答", qa.getAnswerRaw(), "answerRaw 也应同步写入（防后续失败丢对话）");
        assertTrue(events.contains("answer"), "正常提交应记 answer 事件，实际: " + events);
    }

    @Test
    @DisplayName("空答案守卫：非跳过且答案为空时先行拒绝（不消耗锁额度）")
    void rejectsEmptyAnswerBeforeLock() {
        AtomicInteger lockCalls = new AtomicInteger();
        when(lockUtil.tryLock(any(), any(Duration.class))).thenAnswer(inv -> {
            lockCalls.incrementAndGet();
            return null;
        });

        assertThrows(ServiceException.class,
                () -> service.submitAnswer(INTERVIEW_ID, USER_ID, QA_ID, "   ", 1000, false));
        assertEquals(0, lockCalls.get(),
                "空答案应在取锁之前就被拒绝（不该白占一次锁与事件）");
    }

    @Test
    @DisplayName("超时口径：超过「配置时长 + 宽限」后拒绝继续作答并自动收口（数据不丢）")
    void closesInterviewAfterDurationAndGrace() {
        DistributedLockUtil.Lock lock = mock(DistributedLockUtil.Lock.class);
        when(lockUtil.tryLock(any(), any(Duration.class))).thenReturn(lock);

        // 夹具 durationMinutes=20（默认）+ DURATION_GRACE_MINUTES=2 ⇒ 阈值 22 分钟
        PortalVoiceInterview stale = new PortalVoiceInterview();
        stale.setId(INTERVIEW_ID);
        stale.setUserId(USER_ID);
        stale.setStatus("in_progress");
        stale.setTotalQa(5);
        stale.setCurrentIdx(0);
        stale.setConfigJson("{\"durationMinutes\":20}");
        // 30 分钟前开面 → 已超阈值
        stale.setCreateTime(java.time.LocalDateTime.now().minusMinutes(30));
        when(interviewMapper.selectById(anyLong())).thenReturn(stale);

        try {
            service.submitAnswer(INTERVIEW_ID, USER_ID, QA_ID, "超时后的作答", 1000, false);
        } catch (Exception e) {
            // 收口链路中的异步/SSE 部分在本测试环境未装配；本用例只验证超时判定与收口
        }

        assertTrue(events.contains("timeout_close"),
                "超阈值应记 timeout_close 事件，实际: " + events);
        assertEquals("finished", stale.getStatus(), "超时后会话应被自动收口为 finished");
        assertEquals("timeout", stale.getClosedReason(), "关闭原因应记为 timeout");
        assertEquals("超时后的作答", qa.getUserAnswer(),
                "铁律：原始作答必须先落库（超时收口也不得丢数据）");
    }

    @Test
    @DisplayName("超时口径：未超阈值时不得误收口（防守卫过严）")
    void doesNotCloseBeforeDuration() {
        DistributedLockUtil.Lock lock = mock(DistributedLockUtil.Lock.class);
        when(lockUtil.tryLock(any(), any(Duration.class))).thenReturn(lock);

        PortalVoiceInterview fresh = new PortalVoiceInterview();
        fresh.setId(INTERVIEW_ID);
        fresh.setUserId(USER_ID);
        fresh.setStatus("in_progress");
        fresh.setTotalQa(5);
        fresh.setCurrentIdx(0);
        fresh.setConfigJson("{\"durationMinutes\":20}");
        fresh.setCreateTime(java.time.LocalDateTime.now().minusMinutes(3)); // 仅 3 分钟
        when(interviewMapper.selectById(anyLong())).thenReturn(fresh);

        try {
            service.submitAnswer(INTERVIEW_ID, USER_ID, QA_ID, "正常作答", 1000, false);
        } catch (Exception e) {
            // 同上：后续链路未装配
        }

        assertTrue(!events.contains("timeout_close"),
                "未超阈值不得记 timeout_close（否则正常面试会被误收口），实际: " + events);
        assertEquals("in_progress", fresh.getStatus(), "未超阈值会话状态不得被改");
    }

    @Test
    @DisplayName("越权守卫：非本人面试直接拒绝（不进入任何后续守卫）")
    void rejectsOtherUsersInterview() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.submitAnswer(INTERVIEW_ID, 999999L, QA_ID, "作答", 1000, false));
        assertTrue(ex.getMessage().contains("无权") || ex.getMessage().contains("不存在"),
                "非本人面试应被拒绝，实际: " + ex.getMessage());
    }
}
