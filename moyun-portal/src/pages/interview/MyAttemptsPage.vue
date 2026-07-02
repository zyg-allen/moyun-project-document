<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, CheckCircle, XCircle, ChevronLeft, ChevronRight,
  Code, FileText, PenTool, Clock,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
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

function goBack() {
  router.push('/interview');
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
    <!-- 顶部返回栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-80"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回面试指南
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">我的答题</span>
        <span class="w-20"></span>
      </div>
    </div>

    <!-- Hero 区 -->
    <div
      class="text-white py-12 relative overflow-hidden"
      style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 60%, #4338ca 100%));"
    >
      <div class="absolute inset-0 opacity-10 pointer-events-none">
        <div class="absolute top-8 left-10 w-56 h-56 rounded-full bg-white"></div>
        <div class="absolute bottom-0 right-20 w-72 h-72 rounded-full bg-white"></div>
      </div>
      <div class="relative max-w-5xl mx-auto px-4 text-center">
        <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-4">
          <Code class="w-4 h-4 mr-2" /> 墨韵 · 答题历史
        </div>
        <h1 class="text-3xl md:text-4xl font-bold mb-3">我的答题</h1>
        <p class="text-sm md:text-base opacity-90">回顾每一次提交，从结果中复盘成长</p>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-5xl mx-auto px-4">
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
          <button
            @click="router.push('/interview/questions')"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            去做题
          </button>
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
