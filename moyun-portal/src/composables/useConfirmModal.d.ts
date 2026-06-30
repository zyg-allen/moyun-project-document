export interface ConfirmOptions {
    /** 对话框标题 */
    title?: string;
    /** 确认按钮文案 */
    confirmText?: string;
    /** 取消按钮文案 */
    cancelText?: string;
    /** 是否危险操作（红色确认按钮） */
    danger?: boolean;
}
/**
 * 确认对话框 composable，用于替换原生 window.confirm()
 *
 * 全局单例，配合 ConfirmModal.vue 组件使用（在 App.vue 顶层渲染一次）。
 * 返回 Promise<boolean>，点击确认返回 true，取消返回 false。
 *
 * 使用示例：
 *   const confirmModal = useConfirmModal()
 *   if (!await confirmModal.confirm('确认删除？')) return
 */
export declare function useConfirmModal(): {
    visible: any;
    state: any;
    confirm: (message: string, options?: ConfirmOptions) => Promise<boolean>;
    accept: () => void;
    cancel: () => void;
};
