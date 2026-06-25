<script setup lang="ts">
import { computed } from 'vue'
import { CheckCircle2, XCircle, AlertTriangle, Info, X } from 'lucide-vue-next'
import { useToast } from '@/composables/useToast'

const { toasts, remove } = useToast()

// 类型 → 图标/颜色配置
const typeConfig = {
    success: {
        icon: CheckCircle2,
        bg: '#10b981',
        border: '#34d399',
    },
    error: {
        icon: XCircle,
        bg: '#ef4444',
        border: '#f87171',
    },
    warning: {
        icon: AlertTriangle,
        bg: '#f59e0b',
        border: '#fbbf24',
    },
    info: {
        icon: Info,
        bg: '#3b82f6',
        border: '#60a5fa',
    },
} as const

const visibleToasts = computed(() => toasts.value)
</script>

<template>
    <Teleport to="body">
        <!-- 顶部居中 Toast 容器：固定在视口顶部，多个提示自上而下堆叠 -->
        <div
            class="fixed top-0 left-1/2 -translate-x-1/2 z-[9999] flex flex-col items-center gap-2 w-full max-w-md px-4 pt-4 pointer-events-none"
            aria-live="polite"
            aria-atomic="true"
        >
            <TransitionGroup name="toast">
                <div
                    v-for="toast in visibleToasts"
                    :key="toast.id"
                    class="toast-item pointer-events-auto w-full flex items-start gap-3 px-4 py-3 rounded-xl shadow-lg"
                    :style="{
                        backgroundColor: typeConfig[toast.type].bg,
                        border: `1px solid ${typeConfig[toast.type].border}`,
                    }"
                    role="alert"
                >
                    <component
                        :is="typeConfig[toast.type].icon"
                        class="w-5 h-5 flex-shrink-0 text-white mt-0.5"
                    />
                    <p class="flex-1 text-sm text-white leading-relaxed break-words">{{ toast.message }}</p>
                    <button
                        @click="remove(toast.id)"
                        class="flex-shrink-0 text-white/80 hover:text-white transition-colors"
                        aria-label="关闭提示"
                    >
                        <X class="w-4 h-4" />
                    </button>
                </div>
            </TransitionGroup>
        </div>
    </Teleport>
</template>

<style scoped>
/* 进入 / 离开动画 */
.toast-enter-active,
.toast-leave-active {
    transition: all 0.3s ease;
}
.toast-enter-from {
    opacity: 0;
    transform: translateY(-16px) scale(0.95);
}
.toast-leave-to {
    opacity: 0;
    transform: translateY(-16px) scale(0.95);
}
/* 多个 toast 同时存在时移动动画 */
.toast-move {
    transition: transform 0.3s ease;
}
</style>
