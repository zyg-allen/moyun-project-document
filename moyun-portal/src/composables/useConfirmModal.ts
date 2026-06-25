import { ref, readonly } from 'vue'

export interface ConfirmOptions {
    /** 对话框标题 */
    title?: string
    /** 确认按钮文案 */
    confirmText?: string
    /** 取消按钮文案 */
    cancelText?: string
    /** 是否危险操作（红色确认按钮） */
    danger?: boolean
}

interface ConfirmState {
    message: string
    options: Required<ConfirmOptions>
    resolve: (value: boolean) => void
}

const visible = ref(false)
const state = ref<ConfirmState | null>(null)

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
    function confirm(message: string, options: ConfirmOptions = {}): Promise<boolean> {
        const merged: Required<ConfirmOptions> = {
            title: options.title || '确认操作',
            confirmText: options.confirmText || '确认',
            cancelText: options.cancelText || '取消',
            danger: options.danger ?? false,
        }
        return new Promise<boolean>((resolve) => {
            state.value = { message, options: merged, resolve }
            visible.value = true
        })
    }

    function _resolve(value: boolean) {
        if (state.value) {
            state.value.resolve(value)
        }
        visible.value = false
        state.value = null
    }

    function accept() {
        _resolve(true)
    }

    function cancel() {
        _resolve(false)
    }

    return {
        visible: readonly(visible),
        state: readonly(state),
        confirm,
        accept,
        cancel,
    }
}
