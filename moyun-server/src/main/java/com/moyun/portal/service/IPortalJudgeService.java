package com.moyun.portal.service;

import java.util.List;

import com.moyun.portal.domain.dto.JudgeSubmitDTO;
import com.moyun.portal.domain.dto.TestCaseUpsertDTO;
import com.moyun.portal.domain.vo.JudgeResultVO;
import com.moyun.portal.domain.vo.TestCaseVO;

/**
 * OJ 判题业务层（v6.3 OJ 判题系统）
 *
 * @author moyun
 */
public interface IPortalJudgeService {

    /**
     * 提交代码并执行判题，同步返回结果。
     * <p>
     * 当前实现为同步判题：调用 {@link com.moyun.portal.judge.JudgeEngine} 运行用例。
     * 高并发场景可演进为：先写入 pending 提交记录，异步 Worker 消费，前端轮询结果。
     *
     * @param dto    提交参数（questionId/code/language）
     * @param userId 当前门户用户ID
     * @return 判题结果（含逐用例明细）
     */
    JudgeResultVO submitJudge(JudgeSubmitDTO dto, Long userId);

    /**
     * 查询某次提交的判题结果（用于轮询场景）。
     */
    JudgeResultVO getJudgeResult(Long submissionId, Long userId);

    /**
     * 获取题目的样例用例（公开接口，仅返回 is_sample=1 的用例）。
     * 隐藏用例不返回，避免泄露判题数据。
     */
    List<TestCaseVO> listSampleCases(Long questionId);

    /**
     * 获取题目的全部用例（CMS 后台使用，含隐藏用例）。
     */
    List<TestCaseVO> listAllCases(Long questionId);

    /**
     * 新增测试用例（CMS 后台使用）。
     */
    TestCaseVO createTestCase(TestCaseUpsertDTO dto);

    /**
     * 修改测试用例（CMS 后台使用）。
     */
    TestCaseVO updateTestCase(Long id, TestCaseUpsertDTO dto);

    /**
     * 删除测试用例（CMS 后台使用）。
     */
    void deleteTestCase(Long id);
}
