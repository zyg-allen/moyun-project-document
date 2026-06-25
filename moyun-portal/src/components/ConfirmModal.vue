<script setup lang="ts">
import { useConfirmModal } from '@/composables/useConfirmModal'
import { AlertTriangle, X } from 'lucide-vue-next'

const { visible, state, accept, cancel } = useConfirmModal()
</script>

<template>
    <Teleport to="body">
        <Transition name="modal">
            <div
                v-if="visible"
                class="fixed inset-0 z-[10000] flex items-center justify-center p-4"
                role="dialog"
                aria-modal="true"
            >
                <!-- 遮罩 -->
                <div
                    class="absolute inset-0 bg-black/40 backdrop-blur-sm"
                    @click="cancel"
                ></div>

                <!-- 对话框 -->
                <div
                    class="relative w-full max-w-sm rounded-2xl p-6 shadow-2xl"
                    style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
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
                        <h3 class="text-base font-semibold" style="color: var(--theme-text);">
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
