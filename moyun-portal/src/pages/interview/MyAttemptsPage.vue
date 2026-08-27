<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  CheckCircle, XCircle, ChevronLeft, ChevronRight,
  Code, FileText, PenTool, Clock, Mic, Sparkles, History,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getMySubmissionList } from '@/api/interview';
import type { InterviewSubmissionVO } from '@/types/api';

const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const submissions = ref<InterviewSubmissionVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 10;

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

const difficultyMap: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

const answerTypeMap: Record<string, { label: string; icon: any }> = {
  code: { label: '编程题', icon: Code },
  text: { label: '文字题', icon: FileText },
  design: { label: '设计题', icon: PenTool },
};

useHead(computed(() => generateSeo({
  title: '我的答题',
  description: '查看我的答题历史，回顾每次提交的题目与结果，持续提升答题能力',
  keywords: ['我的答题', '答题历史', '提交记录', '面试题'],
  canonicalPath: '/interview/my/attempts',
  robots: 'noindex,nofollow',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '个人空间', path: '/user' },
  { label: '我的答题' },
]);

onMounted(() => {
  loadSubmissions();
});

watch(page, () => {
  loadSubmissions();
});

async function loadSubmissions() {
  try {
    loading.value = true;
    error.value = null;
    const res = await getMySubmissionList({ pageNum: page.value, pageSize });
    if (res.code === 200 && res.data) {
      submissions.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载答题记录失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载答题记录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function gotoQuestion(sub: InterviewSubmissionVO) {
  router.push(`/interview/question/${sub.questionId}`);
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 题目标题：后端可能在提交记录上附带 question 嵌套对象或 questionTitle 字段
function questionTitle(sub: any): string {
  if (sub.question?.title) return sub.question.title;
  if (sub.questionTitle) return sub.questionTitle;
  return `题目 #${sub.questionId}`;
}

function questionDifficulty(sub: any): string {
  const d = sub.question?.difficulty || sub.difficulty;
  return d || '';
}

function diffLabel(sub: any) {
  const d = questionDifficulty(sub);
  return difficultyMap[d]?.label || d || '未知';
}

function diffClass(sub: any) {
  const d = questionDifficulty(sub);
  return difficultyMap[d]?.class || 'bg-gray-100 text-gray-700';
}

function answerType(sub: InterviewSubmissionVO) {
  const t = sub.answerType || '';
  return answerTypeMap[t]?.label || (t || '未知');
}

function answerTypeIcon(sub: InterviewSubmissionVO) {
  const t = sub.answerType || '';
  return answerTypeMap[t]?.icon || FileText;
}

function isPass(sub: InterviewSubmissionVO) {
  // 兼容 isSuccess 布尔与 status 字符串两种情况
  if (typeof sub.isSuccess === 'boolean') return sub.isSuccess;
  const st = (sub.status || '').toLowerCase();
  return st === 'success' || st === 'accepted' || st === 'pass' || st === 'solved';
}

function passLabel(sub: InterviewSubmissionVO) {
  return isPass(sub) ? '通过' : '未通过';
}
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
          @click="router.push('/interview/voice/history')"
          class="shrink-0 inline-flex items-center gap-1.5 px-3.5 py-2 text-sm font-semibold rounded-lg transition-all hover:scale-[1.02]"
          :style="{
            color: 'var(--theme-primary)',
            backgroundColor: 'color-mix(in srgb, var(--theme-primary) 8%, transparent)',
            border: '1px solid color-mix(in srgb, var(--theme-primary) 25%, transparent)',
          }"
        >
          <History class="w-4 h-4" />
          <span class="hidden sm:inline">我的面试记录</span>
          <span class="inline sm:hidden">记录</span>
        </button>
        <button
          @click="router.push('/interview/voice')"
          class="shrink-0 inline-flex items-center gap-1.5 px-3.5 py-2 text-sm font-semibold rounded-lg transition-all hover:scale-[1.02]"
          :style="{
            color: 'var(--theme-primary)',
            backgroundColor: 'color-mix(in srgb, var(--theme-primary) 8%, transparent)',
            border: '1px solid color-mix(in srgb, var(--theme-primary) 25%, transparent)',
          }"
        >
          <Mic class="w-4 h-4" />
          <span class="hidden sm:inline">AI 语音面试官</span>
          <span class="inline sm:hidden">语音</span>
          <span
            class="ml-0.5 inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-bold text-white"
            style="background: linear-gradient(90deg,#ef4444,#f97316);"
          >NEW</span>
        </button>
      </div>
    </div>

    <!-- V10.1 AI 语音面试官导流 Banner -->
    <div
      class="border-b"
      style="background: linear-gradient(90deg, #ecfdf5, #ede9fe); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2.5 flex items-center justify-between gap-3 flex-wrap">
        <div class="flex items-center gap-2 text-sm min-w-0">
          <Sparkles :size="16" class="text-emerald-600 shrink-0" />
          <span class="truncate">
            刷题 + 复盘 + <strong style="color: var(--theme-primary);">语音模拟面试</strong> 闭环上线，
            5 题快练 · 五维雷达图 · 逐题点评一次性配齐
          </span>
        </div>
        <button
          class="shrink-0 text-sm font-semibold rounded-md px-3 py-1.5 transition hover:opacity-90"
          style="background-color: var(--theme-primary); color:#fff;"
          @click="router.push('/interview/voice')"
        >
          <span class="inline-flex items-center gap-1">
            立即体验语音版 <ChevronRight :size="14" />
          </span>
        </button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loading" class="text-center py-16">
          <div
            class="animate-spin rounded-full h-10 w-10 border-2 mx-auto"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="error"
          class="rounded-xl border p-8 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
          <button
            @click="loadSubmissions"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空状态 -->
        <div
          v-else-if="submissions.length === 0"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <Code class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm mb-4" style="color: var(--theme-text-secondary);">还没有答题记录</p>
          <div class="flex flex-wrap items-center justify-center gap-3">
            <button
              @click="router.push('/interview/questions')"
              class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              去做题
            </button>
            <button
              @click="router.push('/interview/voice')"
              class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold transition hover:opacity-90"
              :style="{
                color: 'var(--theme-primary)',
                backgroundColor: 'color-mix(in srgb, var(--theme-primary) 10%, transparent)',
                border: '1px solid color-mix(in srgb, var(--theme-primary) 25%, transparent)',
              }"
            >
              <Mic class="w-4 h-4" />
              试试 AI 语音面试
              <span
                class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-bold text-white"
                style="background: linear-gradient(90deg,#ef4444,#f97316);"
              >NEW</span>
            </button>
          </div>
        </div>

        <!-- 答题记录列表 -->
        <template v-else>
          <div class="space-y-4">
            <div
              v-for="sub in submissions"
              :key="sub.id"
              @click="gotoQuestion(sub)"
              class="rounded-xl shadow-sm hover:shadow-md transition cursor-pointer p-5"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <!-- 标签行 -->
              <div class="flex items-center flex-wrap gap-2 mb-2">
                <span
                  class="px-2.5 py-1 rounded-full text-xs font-medium"
                  :class="diffClass(sub)"
                >
                  {{ diffLabel(sub) }}
                </span>
                <span
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                >
                  <component :is="answerTypeIcon(sub)" class="w-3 h-3 mr-1" />
                  {{ answerType(sub) }}
                </span>
                <!-- 通过 / 未通过 -->
                <span
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                  :class="isPass(sub) ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'"
                >
                  <CheckCircle v-if="isPass(sub)" class="w-3 h-3 mr-1" />
                  <XCircle v-else class="w-3 h-3 mr-1" />
                  {{ passLabel(sub) }}
                </span>
                <span
                  v-if="sub.language"
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                  style="background-color: var(--theme-bg); color: var(--theme-primary);"
                >
                  <Code class="w-3 h-3 mr-1" />
                  {{ sub.language }}
                </span>
              </div>

              <!-- 题目标题 -->
              <h3 class="text-base font-semibold mb-1" style="color: var(--theme-text);">
                {{ questionTitle(sub) }}
              </h3>

              <!-- 答案片段 -->
              <p
                v-if="sub.code || sub.content"
                class="text-sm line-clamp-2 mb-3 font-mono"
                style="color: var(--theme-text-secondary);"
              >
                {{ sub.code || sub.content }}
              </p>

              <!-- 底部信息 -->
              <div class="flex items-center justify-between pt-3 border-t text-xs" style="border-color: var(--theme-border); color: var(--theme-text-secondary);">
                <span v-if="sub.runtime" class="flex items-center">
                  <Clock class="w-3 h-3 mr-1" />
                  耗时 {{ sub.runtime }}ms
                </span>
                <span v-else></span>
                <span class="flex items-center">
                  <Clock class="w-3 h-3 mr-1" />
                  提交于 {{ sub.createTime || '-' }}
                </span>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages > 1" class="flex flex-wrap items-center justify-center gap-2 mt-8">
            <button
              @click="gotoPage(page - 1)"
              :disabled="page === 1"
              :aria-label="`第 ${page - 1} 页`"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <ChevronLeft class="w-4 h-4" />
              上一页
            </button>
            <span class="px-4 py-2 text-sm" style="color: var(--theme-text-secondary);">
              第 {{ page }} / {{ totalPages }} 页
            </span>
            <button
              @click="gotoPage(page + 1)"
              :disabled="page === totalPages"
              :aria-label="`第 ${page + 1} 页`"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              下一页
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 条</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
