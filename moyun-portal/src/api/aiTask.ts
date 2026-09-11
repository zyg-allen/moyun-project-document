import { httpGet, httpPost } from './client';

/**
 * 通用 AI 异步任务 API（v10.23，对应后端 PortalAiTaskController）
 * 链路：submitAiTask 提交任务立即返回 taskId → pollAiTask 轮询到完成并取回结果
 * 适用：resume_parse（简历解析）/ job_match（岗位匹配）/ ai_draft（空字段草稿）/ deep_optimize（深度优化）
 */

/** 通用 AI 任务状态（轮询返回结构） */
export interface AiTaskVO<T = unknown> {
  id: number;
  /** resume_parse / job_match / ai_draft / deep_optimize */
  taskType: string;
  /** pending（排队）/ running（执行中）/ success（成功）/ failed（失败） */
  status: 'pending' | 'running' | 'success' | 'failed';
  /** 进度提示文案（前端直接展示） */
  progressMsg?: string | null;
  /** 任务结果 JSON（status=success 时填充，结构由 taskType 决定） */
  result?: T | null;
  /** 失败原因（status=failed 时填充） */
  error?: string | null;
  createTime?: string;
  finishTime?: string;
}

/** 提交通用 AI 异步任务（立即返回 taskId，不阻塞） */
export const submitAiTask = (taskType: string, bizRef: Record<string, unknown>) => {
  return httpPost<{ taskId: number }>('/portal/ai/task/submit', { taskType, bizRef });
};

/** 查询通用 AI 任务状态（前端轮询，建议 3-4 秒一次） */
export const getAiTask = <T = unknown>(taskId: number | string) => {
  return httpGet<AiTaskVO<T>>(`/portal/ai/task/${taskId}`);
};

/** 轮询选项 */
export interface PollAiTaskOptions {
  /** 轮询间隔毫秒（默认 4000） */
  intervalMs?: number;
  /** 单次轮询回调（pending/running 时收到任务状态，用于更新进度文案） */
  onTick?: (task: AiTaskVO) => void;
}

/**
 * 轮询直到任务完成，成功时 resolve 任务结果内容（task.result），失败时 reject。
 * 注意：组件卸载后轮询仍在后台进行，调用方需自行忽略过期结果（如校验 taskId 一致性）。
 */
export function pollAiTask<T = unknown>(
  taskId: number | string,
  options: PollAiTaskOptions = {},
): Promise<T> {
  const { intervalMs = 4000, onTick } = options;
  return new Promise<T>((resolve, reject) => {
    let timer: number | undefined;
    const stop = () => {
      if (timer !== undefined) window.clearTimeout(timer);
    };
    const tick = async () => {
      try {
        const res = await getAiTask<T>(taskId);
        const task = res.data;
        onTick?.(task);
        if (task.status === 'success') {
          stop();
          resolve(task.result as T);
        } else if (task.status === 'failed') {
          stop();
          reject(new Error(task.error || 'AI 任务处理失败'));
        } else {
          timer = window.setTimeout(tick, intervalMs);
        }
      } catch (e) {
        stop();
        reject(e);
      }
    };
    void tick();
  });
}
