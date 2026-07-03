import { httpGet, httpPost, httpGetList } from './client';
import type { CodeRunVO } from '@/types/api';

/**
 * 在线代码运行（任务 3.6 学习者成长闭环）
 * 后端：PortalCodeRunController，路径 /portal/code
 */

/** 执行代码请求体 */
export interface CodeRunBody {
  /** java/python/javascript */
  language: string;
  /** 源代码 */
  code: string;
  /** 标准输入（可空） */
  stdin?: string;
}

/**
 * 执行代码（需登录）
 * POST /portal/code/run
 * 同步返回运行结果（output/errorMsg/status/runtimeMs）
 */
export const runCode = (data: CodeRunBody) => {
  return httpPost<CodeRunVO>('/portal/code/run', data as unknown as Record<string, unknown>);
};

/**
 * 我的运行历史（需登录，分页）
 * GET /portal/code/my/runs
 */
export const getMyCodeRuns = (params?: { pageNum?: number; pageSize?: number }) => {
  return httpGetList<CodeRunVO>('/portal/code/my/runs', params);
};

/**
 * 运行详情（需登录，仅本人）
 * GET /portal/code/run/{id}
 */
export const getCodeRunDetail = (id: string | number) => {
  return httpGet<CodeRunVO>(`/portal/code/run/${id}`);
};
