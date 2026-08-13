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
import com.moyun.portal.domain.dto.JudgeSubmitDTO;
import com.moyun.portal.domain.dto.TestCaseUpsertDTO;
import com.moyun.portal.domain.entity.PortalInterviewQuestion;
import com.moyun.portal.domain.entity.PortalInterviewQuestionTestCase;
import com.moyun.portal.domain.entity.PortalInterviewSubmission;
import com.moyun.portal.domain.vo.JudgeResultVO;
import com.moyun.portal.domain.vo.TestCaseVO;
import com.moyun.portal.judge.CaseJudgeResult;
import com.moyun.portal.judge.JudgeEngine;
import com.moyun.portal.judge.JudgeResult;
import com.moyun.portal.judge.JudgeStatus;
import com.moyun.portal.mapper.PortalInterviewQuestionMapper;
import com.moyun.portal.mapper.PortalInterviewQuestionTestCaseMapper;
import com.moyun.portal.mapper.PortalInterviewSubmissionMapper;
import com.moyun.portal.service.IPortalJudgeService;

/**
 * OJ 判题业务实现（v6.3 OJ 判题系统）
 *
 * @author moyun
 */
@Service
public class PortalJudgeServiceImpl implements IPortalJudgeService {

    private static final Logger log = LoggerFactory.getLogger(PortalJudgeServiceImpl.class);

    /** 默认单用例运行超时（毫秒）：2 秒，与规划文档 TLE 限制一致 */
    private static final long DEFAULT_TIMEOUT_MS = 2000L;

    @Autowired private JudgeEngine judgeEngine;
    @Autowired private PortalInterviewQuestionMapper questionMapper;
    @Autowired private PortalInterviewQuestionTestCaseMapper testCaseMapper;
    @Autowired private PortalInterviewSubmissionMapper submissionMapper;

    // ==================== 判题 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JudgeResultVO submitJudge(JudgeSubmitDTO dto, Long userId) {
        if (userId == null) throw new ServiceException("请登录后提交");
        PortalInterviewQuestion question = questionMapper.selectById(dto.getQuestionId());
        if (question == null) throw new ServiceException("题目不存在");

        // 拉取题目用例（按 order 升序）
        List<PortalInterviewQuestionTestCase> cases = testCaseMapper.selectByQuestionId(dto.getQuestionId());
        if (cases.isEmpty()) {
            throw new ServiceException("题目尚未配置测试用例，无法判题");
        }

        // 执行判题
        JudgeResult result = judgeEngine.judge(dto.getLanguage(), dto.getCode(), cases, DEFAULT_TIMEOUT_MS);

        // 落库提交记录
        PortalInterviewSubmission submission = buildSubmission(dto, userId, result);
        submissionMapper.insert(submission);

        // 更新题目统计（提交数 + 通过率）
        updateQuestionStats(question);

        log.info("[OJ] 判题完成 userId={} qid={} lang={} status={} pass={}/{}",
                userId, dto.getQuestionId(), dto.getLanguage(),
                result.getStatus().getCode(), result.getPassedCount(), result.getTotalCount());

        return toVO(submission, result);
    }

    @Override
    public JudgeResultVO getJudgeResult(Long submissionId, Long userId) {
        if (userId == null) throw new ServiceException("请登录");
        PortalInterviewSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) throw new ServiceException("提交记录不存在");
        if (!userId.equals(sub.getUserId())) {
            throw new ServiceException("无权查看他人提交");
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
