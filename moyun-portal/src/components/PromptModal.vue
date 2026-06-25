<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { usePromptModal } from '@/composables/usePromptModal'
import { X } from 'lucide-vue-next'

const { visible, state, inputValue, accept, cancel } = usePromptModal()

const inputEl = ref<HTMLInputElement | null>(null)

// 打开时自动聚焦并选中默认值
watch(visible, async (v) => {
    if (v) {
        await nextTick()
        inputEl.value?.focus()
        inputEl.value?.select()
    }
})

function onKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') {
        e.preventDefault()
        accept()
    } else if (e.key === 'Escape') {
        e.preventDefault()
        cancel()
    }
}
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
                    <h3 class="text-base font-semibold mb-3" style="color: var(--theme-text);">
                        {{ state?.options.title }}
                    </h3>

                    <!-- 内容 -->
                    <p class="text-sm mb-3 leading-relaxed" style="color: var(--theme-text-secondary);">
                        {{ state?.message }}
                    </p>

                    <!-- 输入框 -->
                    <input
                        ref="inputEl"
                        v-model="inputValue"
                        type="text"
                        :placeholder="state?.options.placeholder"
                        @keydown="onKeydown"
                        class="w-full px-3 py-2 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                        style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
                    />

                    <!-- 按钮组 -->
                    <div class="flex gap-3 justify-end mt-5">
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
                            style="background-color: var(--theme-primary);"
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
</style>
