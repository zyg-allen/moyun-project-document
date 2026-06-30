import { ref, readonly } from 'vue';
const visible = ref(false);
const inputValue = ref('');
const state = ref(null);
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
export function usePromptModal() {
    function prompt(message, options = {}) {
        const merged = {
            title: options.title || '请输入',
            placeholder: options.placeholder || '',
            defaultValue: options.defaultValue || '',
            confirmText: options.confirmText || '确认',
            cancelText: options.cancelText || '取消',
        };
        inputValue.value = merged.defaultValue;
        return new Promise((resolve) => {
            state.value = { message, options: merged, resolve };
            visible.value = true;
        });
    }
    function _resolve(value) {
        if (state.value) {
            state.value.resolve(value);
        }
        visible.value = false;
        state.value = null;
    }
    function accept() {
        _resolve(inputValue.value);
    }
    function cancel() {
        _resolve(null);
    }
    return {
        visible: readonly(visible),
        inputValue,
        state: readonly(state),
        prompt,
        accept,
        cancel,
    };
}
