package com.moyun.portal.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moyun.common.exception.system.ServiceException;
import com.moyun.ext.cms.service.IPortalInterviewService;
import com.moyun.portal.domain.dto.JudgeSubmitDTO;
import com.moyun.portal.domain.dto.TestCaseUpsertDTO;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.domain.vo.JudgeResultVO;
import com.moyun.portal.domain.vo.TestCaseVO;
import com.moyun.portal.judge.CaseJudgeResult;
import com.moyun.portal.judge.JudgeEngine;
import com.moyun.portal.judge.JudgeProperties;
import com.moyun.portal.judge.JudgeQueueService;
import com.moyun.portal.judge.JudgeResult;
import com.moyun.portal.judge.JudgeStatus;
import com.moyun.portal.judge.JudgeTask;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalInterviewQuestionTestCaseMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.service.IPortalJudgeService;

/**
 * OJ 判题业务实现（v6.3 OJ 判题系统 / v8.0 沙箱与异步演进）
 *
 * @author moyun
 */
@Service
public class PortalJudgeServiceImpl implements IPortalJudgeService {

    private static final Logger log = LoggerFactory.getLogger(PortalJudgeServiceImpl.class);

    @Autowired private JudgeEngine judgeEngine;
    @Autowired private PortalInterviewQuestionMapper questionMapper;
    @Autowired private PortalInterviewQuestionTestCaseMapper testCaseMapper;
    @Autowired private PortalInterviewSubmissionMapper submissionMapper;
    @Autowired private JudgeProperties judgeProperties;
    @Autowired private JudgeQueueService judgeQueueService;
    @Autowired private IPortalInterviewService interviewService;

    // ==================== 判题 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeResultVO submitJudge(JudgeSubmitDTO dto, Long userId) {
        if (userId == null) throw new ServiceException("请登录后提交");
        PortalInterviewQuestion question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) throw new ServiceException("题目不存在");

        boolean runOnly = "run".equals(dto.getMode());

        // run（样例自测）：仅执行样例用例，不落提交记录、不计统计、不触发成长闭环
        if (runOnly) {
            List<PortalInterviewQuestionTestCase> samples = casesForRun(dto.getQuestionId());
            if (samples.isEmpty()) {
                throw new ServiceException("该题目暂无样例用例，可直接提交全量判定");
            }
            long timeoutMs = judgeProperties.getTimeoutMs() > 0
                    ? judgeProperties.getTimeoutMs() : 2000L;
            JudgeResult result = judgeEngine.judge(dto.getLanguage(), dto.getCode(), samples, timeoutMs);
            log.info("[OJ] 样例自测 userId={} qid={} lang={} pass={}/{}",
                    userId, dto.getQuestionId(), dto.getLanguage(),
                    result.getPassedCount(), result.getTotalCount());
            return toVO(result);
        }

        // 拉取题目用例（按 order 升序）
        List<PortalInterviewQuestionTestCase> cases = testCaseMapper.selectByQuestionId(dto.getQuestionId());
        if (cases.isEmpty()) {
            throw new ServiceException("题目尚未配置测试用例，无法判题");
        }

        long timeoutMs = judgeProperties.getTimeoutMs() > 0
                ? judgeProperties.getTimeoutMs() : 2000L;

        // 异步判题：先落库 PENDING 提交记录，再入队，前端轮询 /portal/judge/result/{id}
        if (judgeProperties.isAsyncEnabled()) {
            PortalInterviewSubmission pending = buildPendingSubmission(dto, userId);
            submissionMapper.insert(pending);
            // 题目提交数 +1；通过率由 Worker 完成后刷新
            updateQuestionStats(question);

            JudgeTask task = new JudgeTask(
                    pending.getId(),
                    dto.getQuestionId(),
                    userId,
                    dto.getLanguage(),
                    dto.getCode(),
                    timeoutMs,
                    0,
                    System.currentTimeMillis());
            judgeQueueService.enqueue(task);
            judgeQueueService.cacheStatus(pending.getId(), JudgeStatus.PENDING.getCode());

            log.info("[OJ] 异步入队 userId={} qid={} lang={} submissionId={}",
                    userId, dto.getQuestionId(), dto.getLanguage(), pending.getId());
            return toPendingVO(pending);
        }

        // 同步判题（开发环境 / 默认）
        JudgeResult result = judgeEngine.judge(dto.getLanguage(), dto.getCode(), cases, timeoutMs);

        // 落库提交记录
        PortalInterviewSubmission submission = buildSubmission(dto, userId, result);
        submissionMapper.insert(submission);

        // 更新题目统计（提交数 + 通过率）
        updateQuestionStats(question);

        // 判题终态回调：与选择题共享「做题记录 + 首次通过成长事件 + 答题动态」闭环
        finalizeCallback(dto.getQuestionId(), userId, result.getStatus().isAccepted());

        log.info("[OJ] 判题完成 userId={} qid={} lang={} status={} pass={}/{}",
                userId, dto.getQuestionId(), dto.getLanguage(),
                result.getStatus().getCode(), result.getPassedCount(), result.getTotalCount());

        return toVO(submission, result);
    }

    /** run 模式取样例用例（is_sample=1）；无样例时回退全量用例首条，保证可自测 */
    private List<PortalInterviewQuestionTestCase> casesForRun(Long questionId) {
        List<PortalInterviewQuestionTestCase> samples = testCaseMapper.selectSamplesByQuestionId(questionId);
        if (samples != null && !samples.isEmpty()) return samples;
        List<PortalInterviewQuestionTestCase> all = testCaseMapper.selectByQuestionId(questionId);
        return all == null ? Collections.emptyList() : all;
    }

    /** 判题终态回调：成长闭环失败不阻断判题主流程（结果已落库） */
    private void finalizeCallback(Long questionId, Long userId, boolean accepted) {
        try {
            interviewService.finalizeJudgeResult(questionId, userId, accepted);
        } catch (Exception e) {
            log.error("[OJ] 判题终态成长回调失败 qid={} userId={} accepted={} err={}",
                    questionId, userId, accepted, e.getMessage(), e);
        }
    }

    @Override
    public JudgeResultVO getJudgeResult(Long submissionId, Long userId) {
        if (userId == null) throw new ServiceException("请登录");
        PortalInterviewSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new ServiceException("提交记录不存在");
        if (!userId.equals(sub.getUserId())) {
            throw new ServiceException("无权查看他人提交");
        }
        // 异步路径下，若状态缓存仍为 PENDING，直接返回 PENDING VO，避免每次回查 Worker 是否完成
        JudgeStatus status = JudgeStatus.fromCode(sub.getStatus());
        if (!status.isFinal() && judgeProperties.isAsyncEnabled()) {
            String cached = judgeQueueService.readStatus(submissionId);
            if (JudgeStatus.PENDING.getCode().equals(cached)) {
                return toPendingVO(sub);
            }
        }
        return toVO(submissionOnly(sub));
    }

    // ==================== 用例管理 ====================

    @Override
    public List<TestCaseVO> listSampleCases(Long questionId) {
        if (questionId == null) return Collections.emptyList();
        return testCaseMapper.selectSamplesByQuestionId(questionId).stream()
                .map(this::toSampleVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TestCaseVO> listAllCases(Long questionId) {
        if (questionId == null) return Collections.emptyList();
        return testCaseMapper.selectByQuestionId(questionId).stream()
                .map(this::toAllVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TestCaseVO createTestCase(TestCaseUpsertDTO dto) {
        PortalInterviewQuestion question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) throw new ServiceException("题目不存在");

        PortalInterviewQuestionTestCase entity = new PortalInterviewQuestionTestCase();
        entity.setQuestionId(dto.getQuestionId());
        entity.setInput(dto.getInput());
        entity.setExpectedOutput(dto.getExpectedOutput());
        entity.setIsSample(Boolean.TRUE.equals(dto.getIsSample()) ? 1 : 0);
        entity.setOrderNum(dto.getOrderNum() == null ? (int) testCaseMapper.countByQuestionId(dto.getQuestionId()) : dto.getOrderNum());
        entity.setExplanation(dto.getExplanation());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        testCaseMapper.insert(entity);
        return toAllVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TestCaseVO updateTestCase(Long id, TestCaseUpsertDTO dto) {
        PortalInterviewQuestionTestCase entity = testCaseMapper.selectById(id);
        if (entity == null) throw new ServiceException("测试用例不存在");
        if (dto.getQuestionId() != null) entity.setQuestionId(dto.getQuestionId());
        if (dto.getInput() != null) entity.setInput(dto.getInput());
        if (dto.getExpectedOutput() != null) entity.setExpectedOutput(dto.getExpectedOutput());
        if (dto.getIsSample() != null) entity.setIsSample(Boolean.TRUE.equals(dto.getIsSample()) ? 1 : 0);
        if (dto.getOrderNum() != null) entity.setOrderNum(dto.getOrderNum());
        if (dto.getExplanation() != null) entity.setExplanation(dto.getExplanation());
        entity.setUpdateTime(LocalDateTime.now());
        testCaseMapper.updateById(entity);
        return toAllVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTestCase(Long id) {
        PortalInterviewQuestionTestCase entity = testCaseMapper.selectById(id);
        if (entity == null) return;
        testCaseMapper.deleteById(id);
    }

    // ==================== 私有辅助 ====================

    /** 异步判题场景下构建 PENDING 提交记录（已落库，待 Worker 拉取执行后回写结果） */
    private PortalInterviewSubmission buildPendingSubmission(JudgeSubmitDTO dto, Long userId) {
        PortalInterviewSubmission s = new PortalInterviewSubmission();
        s.setQuestionId(dto.getQuestionId());
        s.setUserId(userId);
        s.setCode(dto.getCode());
        s.setLanguage(dto.getLanguage());
        s.setAnswerType("code");
        s.setStatus(JudgeStatus.PENDING.getCode());
        s.setIsSuccess(false);
        s.setPassedCaseCount(0);
        s.setTotalCaseCount(0);
        s.setCreateTime(LocalDateTime.now());
        return s;
    }

    /** 异步路径下立即返回的 PENDING VO，前端按 submissionId 轮询最终结果 */
    private JudgeResultVO toPendingVO(PortalInterviewSubmission s) {
        JudgeResultVO vo = new JudgeResultVO();
        vo.setSubmissionId(s.getId());
        vo.setStatus(s.getStatus());
        vo.setStatusName(JudgeStatus.PENDING.getDisplayName());
        vo.setAccepted(false);
        vo.setPassedCount(0);
        vo.setTotalCount(0);
        vo.setCaseResults(Collections.emptyList());
        return vo;
    }

    private PortalInterviewSubmission buildSubmission(JudgeSubmitDTO dto, Long userId, JudgeResult result) {
        PortalInterviewSubmission s = new PortalInterviewSubmission();
        s.setQuestionId(dto.getQuestionId());
        s.setUserId(userId);
        s.setCode(dto.getCode());
        s.setLanguage(dto.getLanguage());
        s.setAnswerType("code");
        s.setStatus(result.getStatus().getCode());
        s.setIsSuccess(result.getStatus().isAccepted());
        s.setRuntime(result.getMaxRuntimeMs());
        s.setMemoryUsage(result.getMaxMemoryKb() <= 0 ? null : result.getMaxMemoryKb());
        s.setPassedCaseCount(result.getPassedCount());
        s.setTotalCaseCount(result.getTotalCount());
        s.setFailedCaseId(result.getFailedCaseId());
        s.setFailedCaseInput(result.getFailedCaseInput());
        s.setFailedCaseExpected(result.getFailedCaseExpected());
        s.setFailedCaseActual(result.getFailedCaseActual());
        s.setErrorMessage(result.getErrorMessage());
        s.setCreateTime(LocalDateTime.now());
        return s;
    }

    private void updateQuestionStats(PortalInterviewQuestion question) {
        question.setSubmissionCount((question.getSubmissionCount() == null ? 0L : question.getSubmissionCount()) + 1);
        long total = submissionMapper.countSubmissionsByQuestion(question.getId());
        long success = submissionMapper.countSuccessByQuestion(question.getId());
        BigDecimal rate = total > 0 ? BigDecimal.valueOf(success * 100.0 / total) : BigDecimal.ZERO;
        question.setAcceptanceRate(rate);
        questionMapper.updateById(question);
    }

    private JudgeResult toSubmissionOnlyResult(PortalInterviewSubmission s) {
        JudgeResult r = JudgeResult.of(JudgeStatus.fromCode(s.getStatus()));
        r.setPassedCount(s.getPassedCaseCount() == null ? 0 : s.getPassedCaseCount());
        r.setTotalCount(s.getTotalCaseCount() == null ? 0 : s.getTotalCaseCount());
        r.setMaxRuntimeMs(s.getRuntime() == null ? 0 : s.getRuntime());
        r.setMaxMemoryKb(s.getMemoryUsage() == null ? 0 : s.getMemoryUsage());
        r.setFailedCaseId(s.getFailedCaseId());
        r.setFailedCaseInput(s.getFailedCaseInput());
        r.setFailedCaseExpected(s.getFailedCaseExpected());
        r.setFailedCaseActual(s.getFailedCaseActual());
        r.setErrorMessage(s.getErrorMessage());
        r.setCaseResults(Collections.emptyList());
        return r;
    }

    private JudgeResultVO toVO(PortalInterviewSubmission s, JudgeResult r) {
        JudgeResultVO vo = new JudgeResultVO();
        vo.setSubmissionId(s.getId());
        vo.setStatus(s.getStatus());
        vo.setStatusName(JudgeStatus.fromCode(s.getStatus()).getDisplayName());
        vo.setAccepted(s.getIsSuccess() != null && s.getIsSuccess());
        vo.setPassedCount(s.getPassedCaseCount());
        vo.setTotalCount(s.getTotalCaseCount());
        vo.setMaxRuntime(s.getRuntime());
        vo.setMaxMemory(s.getMemoryUsage());
        vo.setFailedCaseId(s.getFailedCaseId());
        vo.setFailedCaseInput(s.getFailedCaseInput());
        vo.setFailedCaseExpected(s.getFailedCaseExpected());
        vo.setFailedCaseActual(s.getFailedCaseActual());
        vo.setErrorMessage(s.getErrorMessage());
        if (r != null && r.getCaseResults() != null) {
            vo.setCaseResults(r.getCaseResults().stream().map(this::toCaseItem).collect(Collectors.toList()));
        } else {
            vo.setCaseResults(Collections.emptyList());
        }
        return vo;
    }

    private JudgeResultVO toVO(JudgeResult r) {
        JudgeResultVO vo = new JudgeResultVO();
        vo.setStatus(r.getStatus().getCode());
        vo.setStatusName(r.getStatus().getDisplayName());
        vo.setAccepted(r.getStatus().isAccepted());
        vo.setPassedCount(r.getPassedCount());
        vo.setTotalCount(r.getTotalCount());
        vo.setMaxRuntime(r.getMaxRuntimeMs());
        vo.setMaxMemory(r.getMaxMemoryKb() <= 0 ? null : r.getMaxMemoryKb());
        vo.setFailedCaseId(r.getFailedCaseId());
        vo.setFailedCaseInput(r.getFailedCaseInput());
        vo.setFailedCaseExpected(r.getFailedCaseExpected());
        vo.setFailedCaseActual(r.getFailedCaseActual());
        vo.setErrorMessage(r.getErrorMessage());
        if (r.getCaseResults() != null) {
            vo.setCaseResults(r.getCaseResults().stream().map(this::toCaseItem).collect(Collectors.toList()));
        } else {
            vo.setCaseResults(Collections.emptyList());
        }
        return vo;
    }

    private JudgeResultVO.CaseResultItem toCaseItem(CaseJudgeResult c) {
        JudgeResultVO.CaseResultItem item = new JudgeResultVO.CaseResultItem();
        item.setCaseId(c.getCaseId());
        item.setCaseIndex(c.getCaseIndex());
        item.setIsSample(c.getIsSample());
        item.setPassed(c.getPassed());
        item.setRuntime(c.getRuntime());
        // 安全策略：仅样例用例的失败详情对前端可见，隐藏用例不泄露 input/output
        if (Boolean.TRUE.equals(c.getIsSample()) && Boolean.FALSE.equals(c.getPassed())) {
            item.setActualOutput(c.getActualOutput());
        }
        item.setErrorMessage(c.getErrorMessage());
        return item;
    }

    private TestCaseVO toSampleVO(PortalInterviewQuestionTestCase tc) {
        TestCaseVO vo = new TestCaseVO();
        vo.setId(tc.getId());
        vo.setQuestionId(tc.getQuestionId());
        vo.setInput(tc.getInput());
        vo.setExpectedOutput(tc.getExpectedOutput());
        vo.setIsSample(Integer.valueOf(1).equals(tc.getIsSample()));
        vo.setOrderNum(tc.getOrderNum());
        vo.setExplanation(tc.getExplanation());
        return vo;
    }

    private TestCaseVO toAllVO(PortalInterviewQuestionTestCase tc) {
        TestCaseVO vo = new TestCaseVO();
        vo.setId(tc.getId());
        vo.setQuestionId(tc.getQuestionId());
        vo.setInput(tc.getInput());
        vo.setExpectedOutput(tc.getExpectedOutput());
        vo.setIsSample(Integer.valueOf(1).equals(tc.getIsSample()));
        vo.setOrderNum(tc.getOrderNum());
        vo.setExplanation(tc.getExplanation());
        return vo;
    }

    /** 旧 status 兼容场景下由 submission 构造结果（不含逐用例明细） */
    private JudgeResult submissionOnly(PortalInterviewSubmission s) {
        return toSubmissionOnlyResult(s);
    }
}
