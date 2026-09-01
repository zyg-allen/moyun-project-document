<template>
  <!--
    单字段重新生成候选弹窗（v10.20 交互优化）
    职责：调用方拉取 3 个候选版本后，本组件展示供用户选择，选择后 emit select 回传选中文本
    场景：用户对某条 AI 优化建议不满意，点「重新生成」按钮，弹窗展示 3 版本对比，选中替换该条 optimized 内容
  -->
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" @click.self="$emit('update:visible', false)">
      <div class="bg-theme-surface rounded-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <!-- 头部 -->
        <div class="flex items-center justify-between px-5 py-4 border-b sticky top-0 bg-theme-surface">
          <h3 class="font-semibold flex items-center gap-2">
            <RefreshCw class="w-4 h-4" :class="loading ? 'animate-spin' : ''" style="color: var(--theme-primary);" />
            重新生成 · {{ sectionTitle }}
          </h3>
          <button class="text-theme-text-secondary hover:text-theme-text-secondary" @click="$emit('update:visible', false)"><X class="w-4 h-4" /></button>
        </div>

        <div class="p-5 space-y-4">
          <!-- 原文参照 -->
          <div v-if="originalText" class="bg-theme-bg rounded-lg p-3 text-sm">
            <div class="text-xs text-theme-text-secondary mb-1.5">📌 原文</div>
            <p class="text-theme-text-secondary leading-relaxed whitespace-pre-line">{{ originalText }}</p>
          </div>

          <!-- 加载中 -->
          <div v-if="loading" class="py-10 text-center text-sm text-theme-text-secondary">
            <Sparkles class="w-5 h-5 mx-auto mb-2 animate-pulse" style="color: var(--theme-primary);" />
            AI 正在生成 3 个候选版本...
          </div>

          <!-- 候选版本列表 -->
          <div v-else-if="candidates.length" class="space-y-3">
            <div
              v-for="(text, i) in candidates"
              :key="i"
              class="rounded-lg border-2 cursor-pointer transition-all p-3"
              :class="selectedIdx === i ? 'border-theme-primary bg-theme-accent' : 'border-theme-border hover:border-theme-primary/50'"
              @click="selectedIdx = i"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs font-medium px-2 py-0.5 rounded-full" :class="selectedIdx === i ? 'text-white' : 'bg-theme-accent text-theme-text-secondary'" :style="selectedIdx === i ? 'background: var(--theme-primary);' : ''">
                  候选 {{ i + 1 }}
                </span>
                <button
                  v-if="selectedIdx === i"
                  class="text-xs inline-flex items-center gap-1 px-2 py-0.5 rounded text-theme-success"
                >
                  <CheckCircle2 class="w-3 h-3" /> 已选
                </button>
              </div>
              <p class="text-sm text-theme-text leading-relaxed whitespace-pre-line">{{ text }}</p>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="py-10 text-center text-sm text-theme-text-secondary">
            暂无候选版本，请稍后重试
          </div>

          <!-- 错误提示 -->
          <div v-if="errorMsg" class="text-xs text-red-500 bg-red-50 rounded-lg p-2.5">{{ errorMsg }}</div>
        </div>

        <!-- 底部操作 -->
        <div class="flex justify-between items-center px-5 py-4 border-t sticky bottom-0 bg-theme-surface">
          <button class="text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="$emit('update:visible', false)">取消</button>
          <button
            class="inline-flex items-center gap-1.5 text-sm px-5 py-2 rounded-lg text-white font-medium disabled:opacity-50"
            style="background: var(--theme-primary);"
            :disabled="loading || selectedIdx < 0 || !candidates[selectedIdx]"
            @click="onConfirm"
          >
            <Check class="w-4 h-4" /> 应用此版本
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { RefreshCw, X, Sparkles, CheckCircle2, Check } from 'lucide-vue-next';

/**
 * 单字段重新生成候选弹窗（v10.20）
 * props:
 *   - visible: 是否显示（支持 v-model:visible）
 *   - sectionTitle: 段落标题（如「工作经历 #1 · 描述」）
 *   - originalText: 原文参照（用户重新生成时提醒「我要替换什么」）
 *   - candidates: 候选版本数组（调用方拉取后传入）
 *   - loading: 是否正在拉取候选版本
 *   - errorMsg: 拉取失败错误信息
 * emits:
 *   - update:visible: 关闭弹窗
 *   - select(text): 用户选中并应用某版本，回传选中文本
 */
const props = defineProps<{
  visible: boolean;
  sectionTitle?: string;
  originalText?: string;
  candidates: string[];
  loading?: boolean;
  errorMsg?: string;
}>();

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void;
  (e: 'select', text: string): void;
}>();

const selectedIdx = ref(-1);

// 弹窗打开或候选列表变化时，默认选中第一个
watch(() => props.visible, (v) => {
  if (v) selectedIdx.value = props.candidates.length ? 0 : -1;
});
watch(() => props.candidates, (list) => {
  if (props.visible && list.length) selectedIdx.value = 0;
}, { deep: true });

function onConfirm() {
  if (selectedIdx.value < 0 || !props.candidates[selectedIdx.value]) return;
  emit('select', props.candidates[selectedIdx.value]);
  emit('update:visible', false);
}
</script>
