import { ref, readonly } from 'vue';
const visible = ref(false);
const state = ref(null);
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
export function useConfirmModal() {
    function confirm(message, options = {}) {
        const merged = {
            title: options.title || '确认操作',
            confirmText: options.confirmText || '确认',
            cancelText: options.cancelText || '取消',
            danger: options.danger ?? false,
        };
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
        _resolve(true);
    }
    function cancel() {
        _resolve(false);
    }
    return {
        visible: readonly(visible),
        state: readonly(state),
        confirm,
        accept,
        cancel,
    };
}
