import { ref, readonly } from 'vue';
const toasts = ref([]);
let seed = 0;
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
export function useToast() {
    function show(message, type = 'info', duration = 2500) {
        const id = ++seed;
        toasts.value.push({ id, type, message, duration });
        // 自动移除（比 duration 多 300ms 容忍动画）
        const lifetime = duration + 300;
        window.setTimeout(() => remove(id), lifetime);
        return id;
    }
    function remove(id) {
        const idx = toasts.value.findIndex(t => t.id === id);
        if (idx >= 0)
            toasts.value.splice(idx, 1);
    }
    function clear() {
        toasts.value.splice(0, toasts.value.length);
    }
    return {
        toasts: readonly(toasts),
        show,
        remove,
        clear,
        success: (message, duration) => show(message, 'success', duration),
        error: (message, duration) => show(message, 'error', duration),
        warning: (message, duration) => show(message, 'warning', duration),
        info: (message, duration) => show(message, 'info', duration),
    };
}
