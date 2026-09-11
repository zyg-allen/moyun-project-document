<script setup lang="ts">
/**
 * 简历维护页 - 底部固定操作栏
 * 对应 vue_resume_spec.md §5.4 ActionBar
 * 左侧：自动保存状态；右侧：撤销/预览/下载/保存/AI优化
 */
import { computed } from 'vue';
import { Undo2, Eye, Download, Save, Sparkles, History } from 'lucide-vue-next';

const props = defineProps<{
  saveStatus?: 'idle' | 'saving' | 'saved' | 'dirty';
  saving?: boolean;
  exporting?: boolean;
  hasId?: boolean;
}>();

const emit = defineEmits<{
  (e: 'undo'): void;
  (e: 'preview'): void;
  (e: 'download'): void;
  (e: 'save'): void;
  (e: 'optimize'): void;
  (e: 'report'): void;
}>();

const statusText = computed(() => {
  if (props.saveStatus === 'saving') return '保存中...';
  if (props.saveStatus === 'saved') return '已自动保存';
  if (props.saveStatus === 'dirty') return '有未保存修改';
  return '未保存';
});
const statusDotClass = computed(() => {
  if (props.saveStatus === 'saving') return 'saving';
  if (props.saveStatus === 'saved') return 'saved';
  return 'idle';
});
</script>

<template>
  <div class="re-ab">
    <div class="re-ab-left">
      <div class="re-ab-save-indicator">
        <span class="re-ab-dot" :class="statusDotClass"></span>
        {{ statusText }}
      </div>
    </div>
    <div class="re-ab-right">
      <button type="button" class="re-ab-btn ghost sm" @click="emit('undo')" title="撤销最近修改">
        <Undo2 class="w-3.5 h-3.5" />
        撤销
      </button>
      <button type="button" class="re-ab-btn outline" @click="emit('preview')">
        <Eye class="w-4 h-4" />
        预览
      </button>
      <button
        type="button"
        class="re-ab-btn outline"
        :disabled="!hasId"
        @click="emit('report')"
        title="评分报告与优化历史"
      >
        <History class="w-4 h-4" />
        评分报告
      </button>
      <button
        type="button"
        class="re-ab-btn outline"
        :disabled="exporting || !hasId"
        @click="emit('download')"
      >
        <Download class="w-4 h-4" />
        {{ exporting ? '导出中' : '下载' }}
      </button>
      <button
        type="button"
        class="re-ab-btn outline"
        :disabled="saving"
        @click="emit('save')"
      >
        <Save class="w-4 h-4" />
        {{ saving ? '保存中' : '保存' }}
      </button>
      <button type="button" class="re-ab-btn primary" @click="emit('optimize')">
        <Sparkles class="w-4 h-4" />
        AI 优化
      </button>
    </div>
  </div>
</template>

<style scoped>
.re-ab {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid var(--theme-border);
  padding: 12px 36px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 50;
}
.re-ab-left { display: flex; align-items: center; gap: 12px; }
.re-ab-save-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #9ca3af;
}
.re-ab-dot { width: 6px; height: 6px; border-radius: 50%; }
.re-ab-dot.idle { background: #d1d5db; }
.re-ab-dot.saving { background: #f59e0b; animation: re-ab-blink 1s ease infinite; }
.re-ab-dot.saved { background: #10b981; animation: re-ab-blink 2s ease infinite; }
@keyframes re-ab-blink { 0%,100%{ opacity:1; } 50%{ opacity:0.3; } }

.re-ab-right { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.re-ab-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  white-space: nowrap;
  line-height: 1.4;
}
.re-ab-btn:active { transform: scale(0.97); }
.re-ab-btn:disabled { opacity: 0.5; cursor: not-allowed; pointer-events: none; }
.re-ab-btn.primary { background: var(--theme-primary); color: #fff; }
.re-ab-btn.primary:hover { background: #b91c1c; box-shadow: 0 2px 8px rgba(220,38,38,0.25); }
.re-ab-btn.outline { background: #fff; color: #374151; border: 1px solid #d1d5db; }
.re-ab-btn.outline:hover { border-color: var(--theme-primary); color: var(--theme-primary); background: #fef2f2; }
.re-ab-btn.ghost { background: transparent; color: #4b5563; }
.re-ab-btn.ghost:hover { background: #f3f4f6; color: var(--theme-text); }
.re-ab-btn.sm { padding: 5px 10px; font-size: 12px; border-radius: 6px; }

@media (max-width: 768px) {
  .re-ab { padding: 10px 14px; }
  .re-ab-btn { padding: 6px 10px; font-size: 12px; }
  .re-ab-btn.sm { display: none; }
}
</style>
