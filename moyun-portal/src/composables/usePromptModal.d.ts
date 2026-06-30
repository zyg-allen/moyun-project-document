export interface PromptOptions {
    /** 对话框标题 */
    title?: string;
    /** 输入框 placeholder */
    placeholder?: string;
    /** 默认值 */
    defaultValue?: string;
    /** 确认按钮文案 */
    confirmText?: string;
    /** 取消按钮文案 */
    cancelText?: string;
}
/**
 * 输入对话框 composable，用于替换原生 window.prompt()
 *
 * 全局单例，配合 PromptModal.vue 组件使用（在 App.vue 顶层渲染一次）。
 * 返回 Promise<string|null>，点击确认返回输入的字符串，取消返回 null。
 *
 * 使用示例：
 *   const promptModal = usePromptModal()
 *   const url = await promptModal.prompt('请输入链接地址：')
 *   if (url) { ... }
 */
export declare function usePromptModal(): {
    visible: any;
    inputValue: any;
    state: any;
    prompt: (message: string, options?: PromptOptions) => Promise<string | null>;
    accept: () => void;
    cancel: () => void;
};
