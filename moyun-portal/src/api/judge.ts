import { httpGet, httpPost, httpPut, httpDelete } from './client';
import type {
  JudgeResultVO,
  JudgeSubmitParams,
  TestCaseVO,
  TestCaseUpsertParams,
} from '@/types/api';

// ==================== 判题 ====================

/**
 * 提交代码判题（v6.3 OJ 判题系统）
 * POST /portal/judge/submit
 * 同步执行判题：调用判题引擎运行用户代码并比对用例输出
 */
export const submitJudge = (params: JudgeSubmitParams) => {
  return httpPost<JudgeResultVO>('/portal/judge/submit', params);
};

/**
 * 查询判题结果（用于异步场景轮询）
 * GET /portal/judge/result/{submissionId}
 */
export const getJudgeResult = (submissionId: string | number) => {
  return httpGet<JudgeResultVO>(`/portal/judge/result/${submissionId}`);
};

/**
 * 获取题目样例用例（公开接口）
 * GET /portal/judge/cases/sample/{questionId}
 * 仅返回 is_sample=1 的用例，隐藏用例不下发，避免泄露判题数据
 */
export const getSampleTestCases = (questionId: string | number) => {
  return httpGet<TestCaseVO[]>(`/portal/judge/cases/sample/${questionId}`);
};

// ==================== 测试用例管理（CMS 后台） ====================

/**
 * 获取题目全部用例（CMS，含隐藏用例）
 * GET /portal/judge/admin/cases/{questionId}
 */
export const listAllTestCases = (questionId: string | number) => {
  return httpGet<TestCaseVO[]>(`/portal/judge/admin/cases/${questionId}`);
};

/**
 * 新增测试用例（CMS）
 * POST /portal/judge/admin/cases
 */
export const createTestCase = (params: TestCaseUpsertParams) => {
  return httpPost<TestCaseVO>('/portal/judge/admin/cases', params);
};

/**
 * 修改测试用例（CMS）
 * PUT /portal/judge/admin/cases/{id}
 */
export const updateTestCase = (id: string | number, params: TestCaseUpsertParams) => {
  return httpPut<TestCaseVO>(`/portal/judge/admin/cases/${id}`, params);
};

/**
 * 删除测试用例（CMS）
 * DELETE /portal/judge/admin/cases/{id}
 */
export const deleteTestCase = (id: string | number) => {
  return httpDelete<void>(`/portal/judge/admin/cases/${id}`);
};
