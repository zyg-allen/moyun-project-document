<template>
  <!--
    评分报告弹窗（v10.18 阶段五独立组件抽离）
    设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段五
    职责：纯展示 + 事件回传，调用方负责 loadOptimizeHistory / loadScoreReports / goEditFromHistory
    同时承载两类报告：
      - 优化历史（ResumeOptimizeHistory，含 scoreBefore/scoreAfter 采纳前后对比）
      - 评分报告（ResumeScoreReport，含 score/scoreDetail/source 单次评分快照）
  -->
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" @click.self="$emit('update:visible', false)">
      <div class="bg-theme-surface rounded-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between px-5 py-4 border-b sticky top-0 bg-theme-surface">
          <h3 class="font-semibold flex items-center gap-2">
            <History class="w-4 h-4" style="color: var(--theme-primary);" />
            评分报告与优化历史
          </h3>
          <button class="text-theme-text-secondary hover:text-theme-text-secondary" @click="$emit('update:visible', false)"><X class="w-4 h-4" /></button>
        </div>

        <div class="p-5 space-y-5">
          <!-- 评分报告列表（ResumeScoreReport，单次评分快照） -->
          <section v-if="reports.length">
            <div class="flex items-center justify-between mb-2">
              <h4 class="text-sm font-semibold flex items-center gap-1.5">
                <Star class="w-4 h-4" style="color: var(--theme-primary);" />
                评分报告（{{ reports.length }} 次）
              </h4>
              <button class="text-xs text-theme-text-secondary hover:text-theme-primary" :disabled="loading" @click="$emit('refresh')">刷新</button>
            </div>
            <div class="max-h-56 overflow-y-auto divide-y divide-theme-border">
              <div
                v-for="r in reports.slice(0, reportsVisibleCount)"
                :key="r.id"
                class="py-2 flex items-center justify-between text-xs cursor-pointer hover:bg-theme-muted/40 px-1 rounded"
                @click="$emit('selectReport', r)"
              >
                <span class="text-theme-text-secondary">{{ formatTime(r.createTime) }}</span>
                <div class="flex items-center gap-3">
                  <span class="text-theme-text">
                    评分 <span class="font-semibold" :style="{ color: r.score >= 70 ? 'var(--theme-success)' : r.score >= 50 ? 'var(--theme-warning)' : 'var(--theme-danger)' }">{{ r.score }}</span>
                  </span>
                  <span v-if="r.positionSnapshot" class="text-theme-text-secondary">岗位：{{ r.positionSnapshot }}</span>
                  <span v-if="r.source" class="text-[10px] px-1.5 py-0.5 rounded bg-theme-accent text-theme-text-secondary">{{ sourceLabel(r.source) }}</span>
                </div>
              </div>
              <button v-if="reports.length > reportsVisibleCount" class="w-full py-2 text-xs text-theme-primary hover:underline" @click="reportsVisibleCount += 5">
                展开更多（{{ reports.length - reportsVisibleCount }} 条）
              </button>
            </div>
          </section>
          <div v-else class="text-center py-6 text-sm text-theme-text-secondary border border-dashed border-theme-border rounded-lg">
            暂无评分报告，点击「重新评分」生成首份报告
          </div>

          <!-- 优化历史（ResumeOptimizeHistory，采纳前后对比） -->
          <section v-if="history.length" class="border-t border-theme-border pt-4">
            <div class="flex items-center justify-between mb-2">
              <h4 class="text-sm font-semibold text-theme-text flex items-center gap-1.5">
                <GitCompare class="w-4 h-4" style="color: var(--theme-primary);" />
                优化历史（{{ history.length }} 次）
              </h4>
            </div>
            <div class="max-h-48 overflow-y-auto divide-y divide-theme-border">
              <div
                v-for="h in history.slice(0, historyVisibleCount)"
                :key="h.id"
                class="py-2 flex items-center justify-between text-xs cursor-pointer hover:bg-theme-muted/40 px-1 rounded"
                @click="$emit('selectHistory', h)"
              >
                <span class="text-theme-text-secondary">{{ formatTime(h.createTime) }}</span>
                <div class="flex items-center gap-3">
                  <span v-if="h.scoreBefore != null || h.scoreAfter != null" class="text-theme-text">
                    评分 {{ h.scoreBefore ?? '-' }} → <span class="font-semibold" :style="{ color: scoreDelta(h) > 0 ? 'var(--theme-success)' : scoreDelta(h) < 0 ? 'var(--theme-danger)' : 'inherit' }">{{ h.scoreAfter ?? '-' }}</span>
                  </span>
                  <span v-if="h.matchScoreBefore != null || h.matchScoreAfter != null" class="text-theme-text-secondary">
                    匹配 {{ h.matchScoreBefore ?? '-' }}% → {{ h.matchScoreAfter ?? '-' }}%
                  </span>
                </div>
              </div>
              <button v-if="history.length > historyVisibleCount" class="w-full py-2 text-xs text-theme-primary hover:underline" @click="historyVisibleCount += 5">
                展开更多（{{ history.length - historyVisibleCount }} 条）
              </button>
            </div>
          </section>
        </div>

        <div class="flex justify-end gap-2 px-5 py-4 border-t">
          <button class="text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="$emit('update:visible', false)">关闭</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { History, X, Star, GitCompare } from 'lucide-vue-next';
import type { ResumeOptimizeHistory, ResumeScoreReport } from '@/types/api';

/**
 * 评分报告弹窗（v10.18 阶段五独立组件）
 * props:
 *   - visible: 弹窗显示
 *   - reports: 评分报告列表（ResumeScoreReport[]，按时间倒序）
 *   - history: 优化历史列表（ResumeOptimizeHistory[]，按时间倒序）
 *   - loading: 加载中
 *   - initialVisibleCount: 初始展示条数（默认 5）
 * emits:
 *   - update:visible: 关闭弹窗
 *   - refresh: 刷新报告与历史
 *   - selectReport(report): 点击某条评分报告（调用方决定如何展示明细）
 *   - selectHistory(history): 点击某条优化历史（调用方决定是否跳转编辑页）
 */
const props = withDefaults(defineProps<{
  visible: boolean;
  reports: ResumeScoreReport[];
  history: ResumeOptimizeHistory[];
  loading?: boolean;
  initialVisibleCount?: number;
}>(), {
  loading: false,
  initialVisibleCount: 5,
});

defineEmits<{
  (e: 'update:visible', v: boolean): void;
  (e: 'refresh'): void;
  (e: 'selectReport', report: ResumeScoreReport): void;
  (e: 'selectHistory', history: ResumeOptimizeHistory): void;
}>();

const reportsVisibleCount = ref(props.initialVisibleCount);
const historyVisibleCount = ref(props.initialVisibleCount);

/** 格式化时间：保留 yyyy-MM-dd HH:mm */
function formatTime(s?: string): string {
  if (!s) return '-';
  return s.replace('T', ' ').slice(0, 16);
}

/** 评分差值：>0 表示提升，<0 表示下降 */
function scoreDelta(h: ResumeOptimizeHistory): number {
  if (h.scoreAfter == null || h.scoreBefore == null) return 0;
  return h.scoreAfter - h.scoreBefore;
}

/** 评分来源文案 */
function sourceLabel(source: string): string {
  switch (source) {
    case 'manual': return '单独评分';
    case 'optimize': return '优化后';
    case 'template': return '模板套用';
    default: return source;
  }
}
</script>
