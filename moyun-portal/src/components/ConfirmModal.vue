<script setup lang="ts">
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { useConfirmModal } from '@/composables/useConfirmModal'
import { AlertTriangle, X } from 'lucide-vue-next'

const { visible, state, accept, cancel } = useConfirmModal()

const dialogRef = ref<HTMLElement | null>(null)
let lastFocused: HTMLElement | null = null

function getFocusable(): HTMLElement[] {
    if (!dialogRef.value) return []
    return Array.from(
        dialogRef.value.querySelectorAll<HTMLElement>('button, [href], input, select, textarea')
    ).filter((el) => !el.hasAttribute('disabled'))
}

function handleTab(e: KeyboardEvent) {
    const focusable = getFocusable()
    if (focusable.length === 0) return
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    const active = document.activeElement as HTMLElement | null
    if (e.shiftKey) {
        if (active === first) {
            e.preventDefault()
            last.focus()
        }
    } else {
        if (active === last) {
            e.preventDefault()
            first.focus()
        }
    }
}

watch(visible, (val) => {
    if (val) {
        lastFocused = document.activeElement as HTMLElement
        nextTick(() => {
            const focusable = getFocusable()
            // 优先 autofocus 到 accept 按钮（最后一个可聚焦元素），否则首个
            const target = focusable[focusable.length - 1] || focusable[0]
            target?.focus()
        })
    } else if (lastFocused) {
        lastFocused.focus()
        lastFocused = null
    }
})

onBeforeUnmount(() => {
    if (lastFocused) {
        lastFocused.focus()
        lastFocused = null
    }
})
</script>

<template>
    <Teleport to="body">
        <Transition name="modal">
            <div
                v-if="visible"
                class="fixed inset-0 z-[10000] flex items-center justify-center p-4"
                role="dialog"
                aria-modal="true"
                aria-labelledby="confirm-modal-title"
            >
                <!-- 遮罩 -->
                <div
                    class="absolute inset-0 bg-black/40 backdrop-blur-sm"
                    @click="cancel"
                ></div>

                <!-- 对话框 -->
                <div
                    ref="dialogRef"
                    class="relative w-full max-w-sm rounded-2xl p-6 shadow-2xl"
                    style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                    @keydown.esc.prevent="cancel"
                    @keydown.tab="handleTab"
                >
                    <button
                        @click="cancel"
                        class="absolute top-3 right-3 text-gray-400 hover:text-gray-600 transition-colors"
                        aria-label="关闭"
                    >
                        <X class="w-5 h-5" />
                    </button>

                    <!-- 标题 -->
                    <div class="flex items-center gap-3 mb-4">
                        <div
                            class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
                            :style="state?.options.danger
                                ? 'background-color: #fee2e2;'
                                : 'background-color: #fef3c7;'"
                        >
                            <AlertTriangle
                                class="w-5 h-5"
                                :style="{ color: state?.options.danger ? '#ef4444' : '#f59e0b' }"
                            />
                        </div>
                        <h3 id="confirm-modal-title" class="text-base font-semibold" style="color: var(--theme-text);">
                            {{ state?.options.title }}
                        </h3>
                    </div>

                    <!-- 内容 -->
                    <p class="text-sm mb-6 leading-relaxed" style="color: var(--theme-text-secondary);">
                        {{ state?.message }}
                    </p>

                    <!-- 按钮组 -->
                    <div class="flex gap-3 justify-end">
                        <button
                            @click="cancel"
                            class="px-4 py-2 rounded-xl text-sm font-medium transition-colors"
                            style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);"
                        >
                            {{ state?.options.cancelText }}
                        </button>
                        <button
                            @click="accept"
                            class="px-4 py-2 rounded-xl text-sm font-medium text-white transition-opacity hover:opacity-90"
                            :style="{ backgroundColor: state?.options.danger ? '#ef4444' : 'var(--theme-primary)' }"
                        >
                            {{ state?.options.confirmText }}
                        </button>
                    </div>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
    transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
    opacity: 0;
}
.modal-enter-active > div:last-child,
.modal-leave-active > div:last-child {
    transition: transform 0.2s ease;
}
.modal-enter-from > div:last-child {
    transform: scale(0.95);
}
</style>
