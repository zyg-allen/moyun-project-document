<script setup lang="ts">
import { computed } from 'vue';
import {
  CheckCircle, XCircle, Clock, Cpu, AlertTriangle,
  ChevronDown, ChevronUp, Play, FileText,
} from 'lucide-vue-next';
import { ref } from 'vue';
import type { JudgeResultVO, JudgeStatusCode } from '@/types/api';

const props = defineProps<{
  /** 判题结果，为 null 时不展示 */
  result: JudgeResultVO | null;
  /** 是否加载中 */
  loading?: boolean;
}>();

const showFailedDetail = ref(false);

/** 状态映射：徽章文案 + 颜色 + 图标 */
const statusMap: Record<string, { label: string; class: string; icon: any }> = {
  AC: { label: '通过', class: 'bg-green-50 text-green-600 border border-green-200', icon: CheckCircle },
  WA: { label: '答案错误', class: 'bg-red-50 text-red-600 border border-red-200', icon: XCircle },
  TLE: { label: '超时', class: 'bg-orange-50 text-orange-600 border border-orange-200', icon: Clock },
  MLE: { label: '超内存', class: 'bg-orange-50 text-orange-600 border border-orange-200', icon: Cpu },
  RE: { label: '运行错误', class: 'bg-orange-50 text-orange-600 border border-orange-200', icon: AlertTriangle },
  CE: { label: '编译失败', class: 'bg-yellow-50 text-yellow-600 border border-yellow-200', icon: AlertTriangle },
  SE: { label: '系统错误', class: 'bg-red-50 text-red-600 border border-red-200', icon: AlertTriangle },
  PENDING: { label: '判题中', class: 'bg-blue-50 text-blue-600 border border-blue-200', icon: Clock },
};

const statusInfo = computed(() => {
  if (!props.result) return null;
  return statusMap[(props.result.status as string)] || statusMap.WA;
});

const passedRate = computed(() => {
  if (!props.result) return 0;
  const total = props.result.totalCount || 0;
  if (total === 0) return 0;
  return Math.round((props.result.passedCount / total) * 100);
});
</script>

<template>
  <div
    v-if="loading || result"
    class="rounded-xl shadow-sm p-5"
    style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
  >
    <!-- 加载态 -->
    <div v-if="loading" class="flex items-center justify-center py-6">
      <div class="animate-spin rounded-full h-6 w-6 border-2 border-t-transparent" style="border-color: var(--theme-primary);"></div>
      <span class="ml-3 text-sm" style="color: var(--theme-text-secondary);">判题中...</span>
    </div>

    <template v-else-if="result && statusInfo">
      <!-- 状态摘要 -->
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center">
          <component
            :is="statusInfo.icon"
            class="w-5 h-5 mr-2"
            :style="{ color: result.accepted ? '#16a34a' : '#dc2626' }"
          />
          <span class="text-base font-semibold" style="color: var(--theme-text);">
            {{ result.statusName || statusInfo.label }}
          </span>
          <span
            class="ml-3 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
            :class="statusInfo.class"
          >
            {{ result.status }}
          </span>
        </div>
        <div class="text-xs flex items-center gap-3" style="color: var(--theme-text-secondary);">
          <span v-if="result.maxRuntime != null" class="flex items-center">
            <Clock class="w-3.5 h-3.5 mr-1" />
            {{ result.maxRuntime }} ms
          </span>
          <span v-if="result.maxMemory != null && result.maxMemory > 0" class="flex items-center">
            <Cpu class="w-3.5 h-3.5 mr-1" />
            {{ result.maxMemory }} KB
          </span>
        </div>
      </div>

      <!-- 用例通过情况 -->
      <div v-if="result.caseResults && result.caseResults.length" class="mb-4">
        <div class="flex items-center justify-between mb-2">
          <span class="text-xs font-medium" style="color: var(--theme-text-secondary);">
            用例通过情况
          </span>
          <span class="text-xs font-semibold" style="color: var(--theme-primary);">
            {{ result.passedCount }} / {{ result.totalCount }}
            （{{ passedRate }}%）
          </span>
        </div>
        <!-- 用例通过方块 -->
        <div class="flex flex-wrap gap-1.5">
          <div
            v-for="c in result.caseResults"
            :key="c.caseIndex"
            class="flex items-center justify-center w-9 h-7 rounded text-xs font-medium cursor-default"
            :style="{
              backgroundColor: c.passed ? 'rgba(34,197,94,0.12)' : 'rgba(239,68,68,0.12)',
              color: c.passed ? '#16a34a' : '#dc2626',
              border: c.passed ? '1px solid rgba(34,197,94,0.3)' : '1px solid rgba(239,68,68,0.3)'
            }"
            :title="`用例 ${c.caseIndex}：${c.passed ? '通过' : '失败'}${c.isSample ? '（样例）' : '（隐藏）'}${c.runtime ? ' · ' + c.runtime + 'ms' : ''}`"
          >
            <CheckCircle v-if="c.passed" class="w-3.5 h-3.5" />
            <XCircle v-else class="w-3.5 h-3.5" />
          </div>
        </div>
      </div>

      <!-- 编译错误展示 -->
      <div
        v-if="result.status === 'CE' && result.errorMessage"
        class="mb-3"
      >
        <div class="text-xs font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
          <AlertTriangle class="w-3.5 h-3.5 mr-1" style="color: #dc2626;" />
          编译错误信息
        </div>
        <pre
          class="text-xs leading-relaxed p-3 rounded-lg overflow-x-auto max-h-48 overflow-y-auto"
          style="background-color: #1f2937; color: #f3f4f6;"
        >{{ result.errorMessage }}</pre>
      </div>

      <!-- 运行错误展示 -->
      <div
        v-else-if="result.status === 'RE' && result.errorMessage"
        class="mb-3"
      >
        <div class="text-xs font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
          <AlertTriangle class="w-3.5 h-3.5 mr-1" style="color: #ea580c;" />
          运行错误信息
        </div>
        <pre
          class="text-xs leading-relaxed p-3 rounded-lg overflow-x-auto max-h-48 overflow-y-auto"
          style="background-color: #1f2937; color: #f3f4f6;"
        >{{ result.errorMessage }}</pre>
      </div>

      <!-- 系统错误展示 -->
      <div
        v-else-if="result.status === 'SE' && result.errorMessage"
        class="mb-3"
      >
        <div class="text-xs font-semibold mb-2 flex items-center" style="color: var(--theme-text);">
          <AlertTriangle class="w-3.5 h-3.5 mr-1" style="color: #dc2626;" />
          系统错误
        </div>
        <pre
          class="text-xs leading-relaxed p-3 rounded-lg overflow-x-auto"
          style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
        >{{ result.errorMessage }}</pre>
      </div>

      <!-- 失败用例详情（可折叠，仅样例可见 input/expected/actual） -->
      <div
        v-if="!result.accepted && result.failedCaseExpected && (result.failedCaseInput || result.failedCaseActual)"
        class="border-t pt-3"
        style="border-color: var(--theme-border);"
      >
        <button
          @click="showFailedDetail = !showFailedDetail"
          class="w-full flex items-center justify-between text-xs font-semibold"
          style="color: var(--theme-text);"
        >
          <span class="flex items-center">
            <FileText class="w-3.5 h-3.5 mr-1" style="color: var(--theme-primary);" />
            首个失败用例详情
            <span v-if="result.failedCaseInput === null || result.failedCaseInput === undefined" class="ml-2 font-normal" style="color: var(--theme-text-secondary);">（隐藏用例，仅展示输出比对）</span>
          </span>
          <component :is="showFailedDetail ? ChevronUp : ChevronDown" class="w-4 h-4" style="color: var(--theme-text-secondary);" />
        </button>
        <div v-if="showFailedDetail" class="mt-3 space-y-3">
          <div v-if="result.failedCaseInput">
            <div class="text-xs font-medium mb-1" style="color: var(--theme-text-secondary);">输入</div>
            <pre class="text-xs p-2 rounded-lg overflow-x-auto" style="background-color: var(--theme-bg); color: var(--theme-text);">{{ result.failedCaseInput }}</pre>
          </div>
          <div>
            <div class="text-xs font-medium mb-1" style="color: var(--theme-text-secondary);">期望输出</div>
            <pre class="text-xs p-2 rounded-lg overflow-x-auto" style="background-color: rgba(34,197,94,0.08); color: #16a34a;">{{ result.failedCaseExpected }}</pre>
          </div>
          <div v-if="result.failedCaseActual">
            <div class="text-xs font-medium mb-1" style="color: var(--theme-text-secondary);">实际输出</div>
            <pre class="text-xs p-2 rounded-lg overflow-x-auto" style="background-color: rgba(239,68,68,0.08); color: #dc2626;">{{ result.failedCaseActual }}</pre>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
