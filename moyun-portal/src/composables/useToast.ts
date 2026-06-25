import { ref, readonly } from 'vue'

export type ToastType = 'success' | 'error' | 'warning' | 'info'

export interface ToastItem {
    id: number
    type: ToastType
    message: string
    duration: number
}

const toasts = ref<ToastItem[]>([])
let seed = 0

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
    function show(message: string, type: ToastType = 'info', duration = 2500) {
        const id = ++seed
        toasts.value.push({ id, type, message, duration })
        // 自动移除（比 duration 多 300ms 容忍动画）
        const lifetime = duration + 300
        window.setTimeout(() => remove(id), lifetime)
        return id
    }

    function remove(id: number) {
        const idx = toasts.value.findIndex(t => t.id === id)
        if (idx >= 0) toasts.value.splice(idx, 1)
    }

    function clear() {
        toasts.value.splice(0, toasts.value.length)
    }

    return {
        toasts: readonly(toasts),
        show,
        remove,
        clear,
        success: (message: string, duration?: number) => show(message, 'success', duration),
        error: (message: string, duration?: number) => show(message, 'error', duration),
        warning: (message: string, duration?: number) => show(message, 'warning', duration),
        info: (message: string, duration?: number) => show(message, 'info', duration),
    }
}
