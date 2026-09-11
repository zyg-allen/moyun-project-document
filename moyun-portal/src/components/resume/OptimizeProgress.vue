<template>
  <!--
    分析进度弹窗（v10.18 阶段四独立组件抽离）
    设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段四
    职责：纯展示，由调用方驱动 analyzing/percent/step 状态
  -->
  <div class="bg-theme-surface rounded-xl border border-theme-border p-10 text-center">
    <div class="text-5xl mb-3">🤖</div>
    <h3 class="text-lg font-semibold mb-1">AI 正在深度分析你的简历</h3>
    <p class="text-sm text-theme-text-secondary mb-6">
      目标岗位：<strong class="text-theme-text">{{ targetPosition }}</strong>
    </p>

    <div v-if="analyzing" class="max-w-md mx-auto">
      <div class="flex items-center justify-between text-sm mb-2">
        <span class="text-theme-text-secondary">分析进度</span>
        <span class="font-bold" style="color: var(--theme-primary);">{{ Math.round(percent) }}%</span>
      </div>
      <div class="h-2 bg-theme-surface rounded-full overflow-hidden mb-5">
        <div class="h-full rounded-full transition-all duration-500" :style="{ background: 'var(--theme-primary)', width: percent + '%' }"></div>
      </div>
      <div class="text-left space-y-2 text-sm">
        <div v-for="i in 5" :key="i" class="flex items-center gap-2">
          <CheckCircle2 v-if="step > i" class="w-4 h-4 text-theme-success" />
          <RefreshCw v-else-if="step === i" class="w-4 h-4 animate-spin" style="color: var(--theme-primary);" />
          <span v-else class="w-4 h-4 rounded-full border border-theme-border inline-block" />
          <span :class="step >= i ? 'text-theme-text' : 'text-theme-text-secondary'">
            {{ steps[i - 1] }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckCircle2, RefreshCw } from 'lucide-vue-next';

/**
 * 分析进度弹窗（v10.18 阶段四独立组件）
 * props:
 *   - analyzing: 是否分析中（false 时不渲染进度条）
 *   - percent: 进度百分比 0-100
 *   - step: 当前分析步骤 1-5
 *   - targetPosition: 目标岗位文案（标题下方展示）
 *   - steps: 5 步文案数组（默认 ['加载并解析简历结构','提取关键信息与技能','对比岗位要求进行分析','生成匹配评分报告','输出分析结果']）
 */
withDefaults(defineProps<{
  analyzing: boolean;
  percent: number;
  step: number;
  targetPosition?: string;
  steps?: string[];
}>(), {
  targetPosition: '',
  steps: () => ['加载并解析简历结构', '提取关键信息与技能', '对比岗位要求进行分析', '生成匹配评分报告', '输出分析结果'],
});
</script>
