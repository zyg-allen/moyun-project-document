<template>
  <!--
    岗位匹配结果面板（v10.18 阶段三独立组件抽离）
    设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段三
    职责：纯展示，封装 keywordsOf / dimRows / gradeLabel 渲染逻辑
  -->
  <div v-if="report" class="bg-theme-surface rounded-xl border border-theme-border p-6">
    <div class="flex items-center justify-between flex-wrap gap-3 mb-4">
      <h3 class="font-semibold flex items-center gap-2">
        <Target class="w-4 h-4" style="color: var(--theme-primary);" /> 岗位匹配结果
      </h3>
      <span v-if="report.aiPowered" class="text-[11px] inline-flex items-center gap-1 px-2 py-1 rounded-full bg-theme-accent text-theme-primary border border-theme-border">
        <Sparkles class="w-3 h-3" /> AI 深度分析
      </span>
      <span v-else class="text-[11px] px-2 py-1 rounded-full bg-theme-bg text-theme-text-secondary border border-theme-border">规则分析（配置 AI 后更精准）</span>
    </div>

    <div class="flex items-center gap-6 flex-wrap mb-4">
      <div class="text-center">
        <div class="text-4xl font-extrabold" :style="{ color: report.matchScore >= 70 ? 'var(--theme-success)' : report.matchScore >= 50 ? 'var(--theme-warning)' : 'var(--theme-danger)' }">
          {{ report.matchScore }}%
        </div>
        <div class="text-xs text-theme-text-secondary mt-1">{{ gradeLabel[report.grade || ''] || '综合匹配度' }}</div>
      </div>
      <div class="flex-1 min-w-[240px] space-y-2">
        <div v-for="row in dimRows" :key="row.key" class="flex items-center gap-3 text-xs">
          <span class="w-16 text-right text-theme-text-secondary flex-shrink-0">{{ row.label }}</span>
          <div class="flex-1 h-2 bg-theme-surface rounded-full overflow-hidden">
            <div class="h-full rounded-full" :style="{ width: row.dim!.score + '%', background: row.dim!.score >= 70 ? 'var(--theme-success)' : row.dim!.score >= 50 ? 'var(--theme-warning)' : 'var(--theme-danger)' }"></div>
          </div>
          <span class="w-8 font-semibold" :style="{ color: row.dim!.score >= 70 ? 'var(--theme-success)' : row.dim!.score >= 50 ? 'var(--theme-warning)' : 'var(--theme-danger)' }">{{ row.dim!.score }}</span>
        </div>
      </div>
    </div>

    <!-- 关键词 -->
    <div v-if="matchedKeywords.length" class="mb-3">
      <div class="text-xs text-theme-text-secondary mb-1.5">✅ 已匹配关键词（{{ matchedKeywords.length }}）</div>
      <div class="flex flex-wrap gap-1.5">
        <span v-for="k in matchedKeywords" :key="k" class="text-xs px-2 py-0.5 rounded bg-theme-success-bg text-theme-success border border-theme-success-bg">{{ k }}</span>
      </div>
    </div>
    <div v-if="missingKeywords.length" class="mb-3">
      <div class="text-xs text-theme-text-secondary mb-1.5">⚠️ 缺失关键词（{{ missingKeywords.length }}）</div>
      <div class="flex flex-wrap gap-1.5">
        <span v-for="k in missingKeywords" :key="k" class="text-xs px-2 py-0.5 rounded bg-theme-warning-bg text-theme-warning border border-theme-warning-bg">{{ k }}</span>
      </div>
    </div>
    <p v-if="report.summary" class="text-sm text-theme-text-secondary bg-theme-bg rounded-lg p-3 leading-relaxed">{{ report.summary }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Target, Sparkles } from 'lucide-vue-next';
import type { ResumeJobMatchReport } from '@/types/api';

/**
 * 岗位匹配结果面板（v10.18 阶段三独立组件）
 * props:
 *   - report: 匹配报告对象（含 matchScore/grade/dimensions/matchedKeywords/missingKeywords/summary/aiPowered）
 *   - gradeLabelMap: 评级文案映射（默认 { excellent:'优秀匹配', good:'良好匹配', medium:'中等匹配', poor:'匹配较弱' }）
 */
const props = defineProps<{
  report: ResumeJobMatchReport | null;
  gradeLabelMap?: Record<string, string>;
}>();

const DEFAULT_GRADE_LABEL: Record<string, string> = {
  excellent: '优秀匹配', good: '良好匹配', medium: '中等匹配', poor: '匹配较弱',
};
const gradeLabel = props.gradeLabelMap ?? DEFAULT_GRADE_LABEL;

/** 解析关键词字符串为数组（兼容 '、' 与 ',' 分隔） */
function keywordsOf(s?: string): string[] {
  if (!s) return [];
  return s.split(/[、,]/).map(t => t.trim()).filter(Boolean);
}

const matchedKeywords = computed(() => keywordsOf(props.report?.matchedKeywords));
const missingKeywords = computed(() => keywordsOf(props.report?.missingKeywords));

/** 四维评分行（用于渲染评分条） */
const dimRows = computed(() => {
  const d = props.report?.dimensions;
  if (!d) return [];
  return [
    { key: 'keywordMatch', label: '关键词匹配', dim: d.keywordMatch },
    { key: 'experienceMatch', label: '经验匹配', dim: d.experienceMatch },
    { key: 'skillMatch', label: '技能匹配', dim: d.skillMatch },
    { key: 'structureMatch', label: '结构完整度', dim: d.structureMatch },
  ].filter(x => x.dim && typeof x.dim.score === 'number');
});
</script>
