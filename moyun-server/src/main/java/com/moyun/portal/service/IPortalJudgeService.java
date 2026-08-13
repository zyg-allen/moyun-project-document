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
     * 提交代码并执行判题。
     * <p>
     * 行为根据 {@code moyun.judge.async-enabled} 切换：
     * <ul>
     *   <li>{@code false}（默认，开发环境）：同步调用 {@link com.moyun.portal.judge.JudgeEngine}
     *       运行用例并立即返回完整结果；</li>
     *   <li>{@code true}（生产环境）：先写入 PENDING 提交记录并落库，
     *       通过 {@link com.moyun.portal.judge.JudgeQueueService#enqueue} 入队 Redis List，
     *       {@link com.moyun.portal.judge.JudgeAsyncWorker} 异步消费，
     *       本方法立即返回 submissionId + status=PENDING，前端按 {@link #getJudgeResult(Long, Long)} 轮询。</li>
     * </ul>
     *
     * @param dto    提交参数（questionId/code/language）
     * @param userId 当前门户用户ID
     * @return 判题结果（同步场景含逐用例明细；异步场景仅返回 submissionId 与 PENDING 状态）
     */
    JudgeResultVO submitJudge(JudgeSubmitDTO dto, Long userId);

    /**
     * 查询某次提交的判题结果（轮询入口）。
     * <p>
     * 异步场景下若状态仍为 PENDING，则返回 PENDING VO；Worker 完成后回写终态，
     * 调用方按返回的 status 判定是否继续轮询（PENDING → 继续轮询；AC/WA/... → 终止）。
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
