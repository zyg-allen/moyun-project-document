import { useToast } from '@/composables/useToast';
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
    function getErrorMessage(err, fallback = '操作失败，请稍后重试') {
        if (!err)
            return fallback;
        if (typeof err === 'string')
            return err;
        if (err instanceof Error)
            return err.message || fallback;
        const anyErr = err;
        if (anyErr?.message)
            return anyErr.message;
        if (anyErr?.data?.message)
            return anyErr.data.message;
        return fallback;
    }
    async function run(fn, options = {}) {
        const { silent = false, errorToast, successToast } = options;
        try {
            const data = await fn();
            if (successToast)
                toast.success(successToast);
            return { data, error: null, success: true };
        }
        catch (err) {
            const message = errorToast || getErrorMessage(err);
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
