<template>
  <!--
    深度优化前后对比组件（v10.18 阶段四独立组件抽离）
    设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段四
    职责：纯展示 + 事件回传，调用方负责 generateOptimize / toggleAdopted / adoptAll
  -->
  <div class="bg-theme-surface rounded-xl border border-theme-border p-6">
    <div class="flex items-center justify-between flex-wrap gap-3 mb-4">
      <h3 class="font-semibold flex items-center gap-2">
        <GitCompare class="w-4 h-4" style="color: var(--theme-primary);" /> 深度优化 · 前后对比
      </h3>
      <div class="flex gap-2">
        <button
          v-if="!result"
          class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg text-white font-medium disabled:opacity-50"
          style="background: var(--theme-primary);"
          :disabled="optimizing"
          @click="$emit('generate')"
        >
          <Sparkles class="w-4 h-4" /> {{ optimizing ? 'AI 生成中...' : '生成深度优化建议' }}
        </button>
        <template v-else>
          <button class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-primary text-theme-primary font-medium" @click="$emit('adoptAll')">
            <CheckCircle2 class="w-4 h-4" /> 全部采纳
          </button>
          <button class="text-sm px-3 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="$emit('generate')" :disabled="optimizing">重新生成</button>
        </template>
      </div>
    </div>

    <p v-if="result?.summary" class="text-sm text-theme-text-secondary bg-theme-accent border border-theme-border rounded-lg p-3 mb-4">{{ result.summary }}</p>

    <div v-if="!result" class="py-8 text-center text-sm text-theme-text-secondary border border-dashed border-theme-border rounded-lg">
      基于「{{ targetPosition }}」的岗位要求，AI 将逐项改写简历内容（STAR 法则 + 量化数据）
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="(item, i) in result.items"
        :key="i"
        class="rounded-lg border transition-all"
        :class="adoptedSet.has(i) ? 'border-theme-success bg-theme-success-bg/30' : 'border-theme-border'"
      >
        <div class="flex items-center justify-between px-4 py-2.5 bg-theme-bg rounded-t-lg">
          <div class="flex items-center gap-2 text-sm font-medium">
            <component :is="adoptedSet.has(i) ? CheckCircle2 : AlertCircle" class="w-4 h-4" :class="adoptedSet.has(i) ? 'text-theme-success' : 'text-theme-text-secondary'" />
            {{ sectionLabel(item) }}
          </div>
          <button
            class="text-xs px-3 py-1.5 rounded-md font-medium"
            :class="adoptedSet.has(i) ? 'bg-theme-accent text-theme-text-secondary' : 'text-white'"
            :style="adoptedSet.has(i) ? '' : 'background: var(--theme-primary);'"
            @click="$emit('toggle', i)"
          >
            {{ adoptedSet.has(i) ? '取消采纳' : '采纳' }}
          </button>
        </div>
        <div class="p-4 grid md:grid-cols-2 gap-3 text-sm">
          <div>
            <div class="text-xs text-theme-text-secondary mb-1.5">📌 优化前</div>
            <p class="text-theme-text-secondary leading-relaxed bg-theme-bg rounded-lg p-2.5 whitespace-pre-line">{{ item.original || '（空）' }}</p>
          </div>
          <div>
            <div class="text-xs text-theme-success mb-1.5 flex items-center gap-1"><Sparkles class="w-3 h-3" /> AI 优化后</div>
            <p class="text-theme-text leading-relaxed bg-theme-success-bg rounded-lg p-2.5 whitespace-pre-line">{{ item.optimized }}</p>
          </div>
        </div>
        <p v-if="item.reason" class="px-4 pb-3 text-xs text-theme-text-secondary">💡 {{ item.reason }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { GitCompare, Sparkles, CheckCircle2, AlertCircle } from 'lucide-vue-next';
import type { ResumeDeepOptimizeVO, ResumeOptimizeItem } from '@/types/api';

/**
 * 深度优化前后对比组件（v10.18 阶段四独立组件）
 * props:
 *   - result: 深度优化结果（含 items[]）
 *   - adoptedSet: 已采纳索引集合
 *   - optimizing: AI 生成中
 *   - targetPosition: 目标岗位文案（空状态占位展示）
 *   - sectionLabelMap: 段落标题映射（key=section, value=中文标签）
 * emits:
 *   - generate: 生成/重新生成深度优化建议
 *   - toggle(index): 切换采纳状态
 *   - adoptAll: 全部采纳
 */
const props = defineProps<{
  result: ResumeDeepOptimizeVO | null;
  adoptedSet: Set<number>;
  optimizing: boolean;
  targetPosition?: string;
  sectionLabelMap?: Record<string, string>;
}>();

defineEmits<{
  (e: 'generate'): void;
  (e: 'toggle', index: number): void;
  (e: 'adoptAll'): void;
}>();

const DEFAULT_SECTION_LABELS: Record<string, string> = {
  objective: '求职意向',
  education: '教育经历',
  work: '工作经历',
  project: '项目经历',
  skills: '专业技能',
  selfIntro: '自我评价',
};

/** 计算单项标题：section + index（如「工作经历 #2」） */
function sectionLabel(item: ResumeOptimizeItem): string {
  const map = props.sectionLabelMap ?? DEFAULT_SECTION_LABELS;
  const base = map[item.section] || item.section;
  if (item.index != null && item.index > 0) {
    return `${base} #${item.index}`;
  }
  return base;
}
</script>
