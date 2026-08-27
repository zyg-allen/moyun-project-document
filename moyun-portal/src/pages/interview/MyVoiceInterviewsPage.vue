<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Mic, Clock, ChevronLeft, ChevronRight, RefreshCw, PlayCircle, FileText,
  Target, Sparkles, RotateCcw,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getMyVoiceInterviewList } from '@/api/voiceInterview';
import type { VoiceInterviewVO } from '@/api/voiceInterview';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

const loading = ref(false);
const list = ref<VoiceInterviewVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

useHead(computed(() => generateSeo({
  title: '我的面试记录',
  description: '查看历史 AI 语音面试记录，回看完整面试对话、评分报告与逐题点评',
  keywords: ['面试记录', '语音面试', '面试复盘', 'AI 面试'],
  canonicalPath: '/interview/voice/history',
  robots: 'noindex,nofollow',
})));

const breadcrumbs = computed(() => [
  { label: '个人空间', path: '/user' },
  { label: '我的面试记录' },
]);

// 状态映射：进行中=info，已完成=success，已中止=warning
const statusMeta: Record<string, { label: string; class: string }> = {
  in_progress: { label: '进行中', class: 'bg-theme-info-bg text-theme-info' },
  finished: { label: '已完成', class: 'bg-theme-success-bg text-theme-success' },
  terminated: { label: '已中止', class: 'bg-theme-warning-bg text-theme-warning' },
};

const styleMeta: Record<string, string> = {
  friendly: '温和引导',
  professional: '专业沉稳',
  stress: '压力挑战',
};

const difficultyMeta: Record<string, string> = {
  easy: '简单',
  medium: '中等',
  hard: '困难',
};

function statusOf(v: VoiceInterviewVO) {
  return statusMeta[v.status] || { label: v.status || '-', class: 'bg-theme-surface text-theme-text-secondary' };
}

function formatTime(t?: string) {
  if (!t) return '-';
  return t.slice(0, 16).replace('T', ' ');
}

async function loadList() {
  loading.value = true;
  try {
    const res = await getMyVoiceInterviewList({ pageNum: page.value, pageSize });
    const data = res.data;
    list.value = (data?.records as VoiceInterviewVO[]) || [];
    total.value = data?.total || 0;
  } catch (err: any) {
    toast.error(err?.message || '加载面试记录失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function viewReport(v: VoiceInterviewVO) {
  if (!v.id) return;
  router.push({ path: '/interview/voice', query: { id: String(v.id) } });
}

function startNew() {
  router.push('/interview/voice');
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  loadList();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

onMounted(() => {
  loadList();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <button
          @click="startNew"
          class="inline-flex items-center gap-1.5 px-3.5 py-1.5 text-sm font-medium transition-colors"
          style="background-color: var(--theme-primary); color: var(--theme-primary-contrast, #fff); border-radius: var(--theme-radius-md);"
        >
          <RotateCw :size="15" />
          再来一场
        </button>
      </div>
    </div>

    <main class="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
      <!-- 页头 -->
      <div class="mb-6 flex items-center gap-3">
        <div
          class="w-11 h-11 flex items-center justify-center"
          style="background-color: var(--theme-primary-bg); border-radius: var(--theme-radius-lg); color: var(--theme-primary);"
        >
          <Mic :size="22" />
        </div>
        <div>
          <h1 class="text-xl font-bold" style="color: var(--theme-text-primary);">我的面试记录</h1>
          <p class="text-sm mt-0.5" style="color: var(--theme-text-secondary);">
            共 {{ total }} 场面试 · 完整保留对话内容、面试岗位与评分报告
          </p>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-24 gap-3" style="color: var(--theme-text-secondary);">
        <RefreshCw :size="28" class="animate-spin" />
        <span class="text-sm">加载中...</span>
      </div>

      <!-- 空态 -->
      <div v-else-if="list.length === 0" class="flex flex-col items-center justify-center py-24 gap-4">
        <div
          class="w-16 h-16 flex items-center justify-center"
          style="background-color: var(--theme-primary-bg); border-radius: var(--theme-radius-full); color: var(--theme-primary);"
        >
          <Mic :size="30" />
        </div>
        <div class="text-center">
          <p class="font-medium" style="color: var(--theme-text-primary);">还没有面试记录</p>
          <p class="text-sm mt-1" style="color: var(--theme-text-secondary);">
            完成第一场 AI 语音面试后，对话与评分报告会保存在这里
          </p>
        </div>
        <button
          @click="startNew"
          class="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium transition-colors"
          style="background-color: var(--theme-primary); color: var(--theme-primary-contrast, #fff); border-radius: var(--theme-radius-md);"
        >
          <PlayCircle :size="16" />
          开始第一场面试
        </button>
      </div>

      <!-- 记录卡片列表 -->
      <div v-else class="space-y-4">
        <div
          v-for="item in list"
          :key="item.id"
          class="border p-5 transition-all hover:shadow-md cursor-pointer group"
          style="background-color: var(--theme-surface); border-color: var(--theme-border); border-radius: var(--theme-radius-lg);"
          @click="viewReport(item)"
        >
          <div class="flex items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <!-- 岗位 + 状态 -->
              <div class="flex items-center gap-2.5 flex-wrap">
                <h3 class="font-semibold truncate group-hover:underline" style="color: var(--theme-text-primary);">
                  {{ item.position || '未指定岗位' }}
                </h3>
                <span
                  class="inline-flex items-center px-2 py-0.5 text-xs font-medium"
                  :class="statusOf(item).class"
                  style="border-radius: var(--theme-radius-full);"
                >
                  {{ statusOf(item).label }}
                </span>
              </div>

              <!-- 元信息 -->
              <div class="flex items-center gap-4 mt-2 text-xs flex-wrap" style="color: var(--theme-text-secondary);">
                <span class="inline-flex items-center gap-1">
                  <Clock :size="12" />
                  {{ formatTime(item.createTime) }}
                </span>
                <span class="inline-flex items-center gap-1">
                  <Target :size="12" />
                  {{ styleMeta[item.style || ''] || '专业沉稳' }} · {{ difficultyMeta[item.difficulty || ''] || '中等' }}
                </span>
                <span class="inline-flex items-center gap-1">
                  <FileText :size="12" />
                  {{ item.totalQa || 0 }} 个问答
                </span>
                <span v-if="item.isPersonalized === 1" class="inline-flex items-center gap-1" style="color: var(--theme-primary);">
                  <Sparkles :size="12" />
                  简历定制
                </span>
              </div>

              <!-- 摘要 -->
              <p v-if="item.summary" class="mt-2.5 text-sm line-clamp-2" style="color: var(--theme-text-secondary);">
                {{ item.summary }}
              </p>
            </div>

            <!-- 得分环 -->
            <div class="flex flex-col items-center shrink-0">
              <div
                class="w-14 h-14 flex items-center justify-center border-2"
                :style="{
                  borderColor: (item.score ?? 0) >= 80 ? 'var(--theme-success)' : (item.score ?? 0) >= 60 ? 'var(--theme-warning)' : 'var(--theme-error)',
                  color: (item.score ?? 0) >= 80 ? 'var(--theme-success)' : (item.score ?? 0) >= 60 ? 'var(--theme-warning)' : 'var(--theme-error)',
                  borderRadius: 'var(--theme-radius-full)',
                }"
              >
                <span class="text-lg font-bold">{{ item.score ?? '-' }}</span>
              </div>
              <span class="text-xs mt-1" style="color: var(--theme-text-secondary);">综合分</span>
            </div>
          </div>

          <!-- 悬停操作提示 -->
          <div class="mt-3 pt-3 border-t flex items-center justify-between text-xs" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
            <span>查看完整对话 · 逐题点评 · 面试报告</span>
            <span class="inline-flex items-center gap-1 group-hover:translate-x-0.5 transition-transform" style="color: var(--theme-primary);">
              进入复盘
              <ChevronRight :size="13" />
            </span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="flex items-center justify-center gap-2 mt-8">
        <button
          :disabled="page <= 1"
          @click="gotoPage(page - 1)"
          class="p-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); border-radius: var(--theme-radius-md); color: var(--theme-text-primary);"
        >
          <ChevronLeft :size="16" />
        </button>
        <span class="text-sm px-3" style="color: var(--theme-text-secondary);">
          {{ page }} / {{ totalPages }}
        </span>
        <button
          :disabled="page >= totalPages"
          @click="gotoPage(page + 1)"
          class="p-2 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); border-radius: var(--theme-radius-md); color: var(--theme-text-primary);"
        >
          <ChevronRight :size="16" />
        </button>
      </div>
    </main>
  </div>
</template>
