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
export declare function useApiCall(): {
    run: <T>(fn: () => Promise<T>, options?: RunOptions) => Promise<RunResult<T>>;
    getErrorMessage: (err: unknown, fallback?: string) => string;
};
