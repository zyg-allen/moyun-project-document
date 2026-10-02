import { useToast } from '@/composables/useToast';

/** run 方法的配置选项 */
export interface RunOptions {
  /** 静默模式：只 console.error + 返回 error，不弹 toast（适用于数据加载） */
  silent?: boolean;
  /** 自定义错误 toast 文案（适用于用户主动操作） */
  errorToast?: string;
  /** 成功 toast 文案（可选） */
  successToast?: string;
}

/** run 方法的返回结果 */
export interface RunResult<T> {
  data: T | null;
  error: Error | null;
  success: boolean;
}

/**
 * 统一的 API 调用错误处理 composable
 *
 * 解决全站 try/catch + 仅 console.error 无用户反馈的问题。
 *
 * 使用示例：
 *   const { run } = useApiCall();
 *   // 静默模式（数据加载，不弹 toast，只返回 error 状态供页面渲染）
 *   const { data, error } = await run(() => articleApi.getList(), { silent: true });
 *   // 提示模式（用户主动操作，失败弹 toast）
 *   const { data } = await run(() => articleApi.save(form), { errorToast: '保存失败' });
 */
export function useApiCall() {
  const toast = useToast();

  /**
   * 提取错误信息（兼容 unknown 类型 catch）
   */
  function getErrorMessage(err: unknown, fallback = '操作失败，请稍后重试'): string {
    if (!err) return fallback;
    if (typeof err === 'string') return err;
    if (err instanceof Error) return err.message || fallback;
    const anyErr = err as any;
    if (anyErr?.message) return anyErr.message;
    if (anyErr?.data?.message) return anyErr.data.message;
    return fallback;
  }

  async function run<T>(
    fn: () => Promise<T>,
    options: RunOptions = {},
  ): Promise<RunResult<T>> {
    const { silent = false, errorToast, successToast } = options;

    try {
      const data = await fn();
      if (successToast) toast.success(successToast);
      return { data, error: null, success: true };
    } catch (err: unknown) {
      // 文案优先级（清单 P2）：
      //   1) **业务错误**（client.ts 附了 code，如 402 会员权益不足）→ 必须原样展示服务端文案，
      //      否则"次数已用完，请开通会员"这类关键引导会被页面的通用 errorToast（"开始失败"）吃掉；
      //   2) 页面显式 errorToast → 用于网络/未知失败时给出更贴合场景的提示；
      //   3) 通用兜底文案。
      const bizCode = (err as { code?: number } | null)?.code;
      const serverMessage = getErrorMessage(err, '');
      const message = (bizCode !== undefined && serverMessage)
        ? serverMessage
        : (errorToast || serverMessage || '操作失败，请稍后重试');
      // 始终 console.error 便于调试
      console.error('[API Error]', err);
      // 静默模式不弹 toast（由页面通过 error 状态自行渲染错误 UI）
      if (!silent) {
        toast.error(message);
      }
      const error = err instanceof Error ? err : new Error(message);
      return { data: null, error, success: false };
    }
  }

  return {
    run,
    getErrorMessage,
  };
}
