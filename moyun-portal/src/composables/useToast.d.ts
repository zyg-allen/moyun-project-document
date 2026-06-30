export type ToastType = 'success' | 'error' | 'warning' | 'info';
export interface ToastItem {
    id: number;
    type: ToastType;
    message: string;
    duration: number;
}
/**
 * 顶部 Toast 提示 composable
 *
 * 全局单例，任意组件调用 useToast() 都共享同一份 toasts 列表。
 * 配合 ToastContainer.vue 组件使用，由组件在 App 顶层渲染。
 *
 * 使用示例：
 *   const toast = useToast()
 *   toast.success('保存成功')
 *   toast.error('保存失败')
 *   toast.warning('请先登录')
 *   toast.info('提示内容')
 */
export declare function useToast(): {
    toasts: any;
    show: (message: string, type?: ToastType, duration?: number) => number;
    remove: (id: number) => void;
    clear: () => void;
    success: (message: string, duration?: number) => number;
    error: (message: string, duration?: number) => number;
    warning: (message: string, duration?: number) => number;
    info: (message: string, duration?: number) => number;
};
