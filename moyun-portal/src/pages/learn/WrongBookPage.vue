<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, AlertCircle, Loader2, CheckCircle2, RefreshCw, Search,
  ChevronLeft, ChevronRight, Clock, Repeat,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import {
  getWrongQuestions, markWrongQuestionMastered, getWrongQuestionCount,
} from '@/api/learn';
import type { WrongQuestionVO, WrongQuestionQuery, WrongQuestionCount } from '@/api/learn';

const router = useRouter();

const loading = ref(false);
const error = ref<string | null>(null);
const list = ref<WrongQuestionVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;
const actionId = ref<number | null>(null);
const stats = ref<WrongQuestionCount | null>(null);

type StatusFilter = '' | 'wrong' | 'reviewing' | 'mastered';
const statusFilter = ref<StatusFilter>('wrong');
const tagInput = ref('');
const tagFilter = ref('');
const keyword = ref('');

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

useHead(computed(() => generateSeo({
  title: '错题本',
  description: '墨韵智库错题本 - 错题记录、按标签筛选、复习追踪、标记掌握',
  keywords: ['错题本', '错题复习', '艾宾浩斯', '刷题', '墨韵'],
  canonicalPath: '/learn/wrong',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadStats();
  loadList();
});

watch(page, () => {
  loadList();
});

watch(statusFilter, () => {
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadList();
  }
});

async function loadStats() {
  try {
    const res = await getWrongQuestionCount();
    if (res.code === 200 && res.data) {
      stats.value = res.data;
    }
  } catch {
    // 统计失败不阻断列表
  }
}

async function loadList() {
  loading.value = true;
  error.value = null;
  try {
    const params: WrongQuestionQuery = {
      pageNum: page.value,
      pageSize,
    };
    if (statusFilter.value) params.status = statusFilter.value;
    if (tagFilter.value) params.tag = tagFilter.value;
    if (keyword.value) params.keyword = keyword.value;
    const res = await getWrongQuestions(params);
    if (res.code === 200 && res.data) {
      list.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function doSearch() {
  tagFilter.value = tagInput.value.trim();
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadList();
  }
}

function resetSearch() {
  tagInput.value = '';
  tagFilter.value = '';
  keyword.value = '';
  if (page.value !== 1) {
    page.value = 1;
  } else {
    loadList();
  }
}

async function markMastered(wq: WrongQuestionVO) {
  actionId.value = wq.id;
  try {
    await markWrongQuestionMastered(wq.questionId);
    // 局部更新
    wq.status = 'mastered';
    await loadStats();
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '标记失败';
  } finally {
    actionId.value = null;
  }
}

function redoQuestion(wq: WrongQuestionVO) {
  router.push(`/interview/question/${wq.questionId}`);
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/learn');
  }
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function statusText(s: string) {
  const map: Record<string, string> = { wrong: '待复习', reviewing: '复习中', mastered: '已掌握' };
  return map[s] || s;
}

function statusColor(s: string) {
  if (s === 'mastered') return '#10b981';
  if (s === 'reviewing') return '#3b82f6';
  return '#ef4444';
}

function difficultyText(d: string | null) {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' };
  return (d && map[d]) || '未分级';
}

function difficultyColor(d: string | null) {
  if (d === 'easy') return 'text-emerald-600';
  if (d === 'medium') return 'text-amber-600';
  if (d === 'hard') return 'text-rose-600';
  return 'text-slate-500';
}

function formatReviewTime(t: string | null) {
  if (!t) return null;
  const dt = new Date(t);
  const now = new Date();
  const diff = dt.getTime() - now.getTime();
  if (diff <= 0) return '已到复习时间';
  const hours = Math.floor(diff / (1000 * 60 * 60));
  if (hours < 24) return `还有 ${hours} 小时复习`;
  const days = Math.floor(hours / 24);
  return `还有 ${days} 天复习`;
}

const statusTabs: { value: StatusFilter; label: string }[] = [
  { value: 'wrong', label: '待复习' },
  { value: 'reviewing', label: '复习中' },
  { value: 'mastered', label: '已掌握' },
  { value: '', label: '全部' },
];
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-80"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          学习中心
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">错题本</span>
        <span class="w-16"></span>
      </div>
    </div>

    <main class="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
      <!-- 标题区 -->
      <div class="mb-6">
        <h1 class="text-2xl md:text-3xl font-bold mb-2" style="color: var(--theme-text);">错题本</h1>
        <p class="text-sm" style="color: var(--theme-text-secondary);">
          答错自动收录，按艾宾浩斯曲线安排复习，标记已掌握以过滤
        </p>
      </div>

      <!-- 统计卡片 -->
      <div v-if="stats" class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
        <div class="rounded-lg border p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
          <div class="text-xs" style="color: var(--theme-text-secondary);">待复习</div>
          <div class="text-xl font-bold mt-1" style="color: #ef4444;">{{ stats.unMasteredCount }}</div>
        </div>
        <div class="rounded-lg border p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
          <div class="text-xs" style="color: var(--theme-text-secondary);">复习中</div>
          <div class="text-xl font-bold mt-1" style="color: #3b82f6;">{{ stats.reviewingCount }}</div>
        </div>
        <div class="rounded-lg border p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
          <div class="text-xs" style="color: var(--theme-text-secondary);">已掌握</div>
          <div class="text-xl font-bold mt-1" style="color: #10b981;">{{ stats.masteredCount }}</div>
        </div>
        <div
          class="rounded-lg border p-4 cursor-pointer transition hover:shadow-md"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          @click="statusFilter = 'wrong'; page = 1; loadList()"
        >
          <div class="text-xs" style="color: var(--theme-text-secondary);">今日待复习</div>
          <div class="text-xl font-bold mt-1" style="color: var(--theme-primary);">{{ stats.todayReviewCount }}</div>
        </div>
      </div>

      <!-- 筛选区 -->
      <div class="mb-6 space-y-3">
        <div class="flex items-center gap-1 p-1 rounded-lg w-fit" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            @click="statusFilter = tab.value"
            class="px-3 py-1.5 rounded-md text-sm font-medium transition"
            :style="statusFilter === tab.value
              ? 'background-color: var(--theme-primary); color: #fff;'
              : 'color: var(--theme-text-secondary);'"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="flex items-center gap-2">
          <div
            class="flex-1 flex items-center rounded-lg px-3 py-2"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <Search class="w-4 h-4 mr-2 flex-shrink-0" style="color: var(--theme-text-secondary);" />
            <input
              v-model="tagInput"
              @keyup.enter="doSearch"
              type="text"
              placeholder="按题目标签筛选，如：算法、动态规划、MySQL"
              class="flex-1 text-sm focus:outline-none"
              style="color: var(--theme-text);"
            />
          </div>
          <button
            @click="doSearch"
            class="px-4 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            筛选
          </button>
          <button
            @click="resetSearch"
            class="px-3 py-2 rounded-lg text-sm transition hover:opacity-80 inline-flex items-center"
            style="border: 1px solid var(--theme-border); color: var(--theme-text-secondary);"
            title="重置筛选"
          >
            <RefreshCw class="w-4 h-4" />
          </button>
        </div>
      </div>

      <!-- 错误提示 -->
      <div
        v-if="error"
        class="mb-4 rounded-lg p-3 flex items-start text-sm"
        style="background-color: color-mix(in srgb, #ef4444 10%, transparent); color: #ef4444;"
      >
        <AlertCircle class="w-4 h-4 mr-2 mt-0.5 flex-shrink-0" />
        <span>{{ error }}</span>
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20">
        <Loader2 class="w-8 h-8 animate-spin" style="color: var(--theme-primary);" />
        <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <!-- 空态 -->
      <div
        v-else-if="list.length === 0"
        class="flex flex-col items-center justify-center py-20 rounded-xl border"
        style="border-color: var(--theme-border); background-color: var(--theme-surface);"
      >
        <CheckCircle2 class="w-12 h-12 mb-4" style="color: #10b981;" />
        <p class="text-sm" style="color: var(--theme-text-secondary);">
          {{ statusFilter === 'wrong' ? '没有待复习的错题，继续保持！' : '该状态下暂无错题' }}
        </p>
      </div>

      <!-- 错题列表 -->
      <div v-else class="space-y-3">
        <div
          v-for="wq in list"
          :key="wq.id"
          class="rounded-xl border p-4 transition hover:shadow-md"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="flex-1 min-w-0 cursor-pointer" @click="redoQuestion(wq)">
              <div class="flex items-center mb-1.5 flex-wrap gap-2">
                <span
                  class="inline-block px-2 py-0.5 rounded text-xs"
                  :style="{ color: '#fff', backgroundColor: statusColor(wq.status) }"
                >
                  {{ statusText(wq.status) }}
                </span>
                <span
                  v-if="wq.questionDifficulty"
                  class="text-xs"
                  :class="difficultyColor(wq.questionDifficulty)"
                >
                  {{ difficultyText(wq.questionDifficulty) }}
                </span>
                <span class="text-xs" style="color: var(--theme-text-secondary);">
                  错 {{ wq.wrongCount }} 次
                </span>
                <span
                  v-if="formatReviewTime(wq.nextReviewTime)"
                  class="text-xs inline-flex items-center"
                  style="color: var(--theme-text-secondary);"
                >
                  <Clock class="w-3 h-3 mr-1" />
                  {{ formatReviewTime(wq.nextReviewTime) }}
                </span>
              </div>
              <h3 class="text-sm font-medium mb-1 line-clamp-2 hover:underline" style="color: var(--theme-text);">
                {{ wq.questionTitle || `题目 #${wq.questionId}` }}
              </h3>
              <div v-if="wq.questionTags" class="flex flex-wrap gap-1">
                <span
                  v-for="tag in wq.questionTags.split(',').filter(t => t.trim()).slice(0, 5)"
                  :key="tag"
                  class="text-xs px-1.5 py-0.5 rounded"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                >{{ tag.trim() }}</span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex flex-col gap-2 flex-shrink-0">
              <button
                @click="redoQuestion(wq)"
                class="inline-flex items-center justify-center px-3 py-1.5 rounded-md text-xs font-medium text-white transition hover:opacity-90"
                style="background-color: var(--theme-primary);"
                title="重做该题"
              >
                <Repeat class="w-3.5 h-3.5 mr-1" /> 重做
              </button>
              <button
                v-if="wq.status !== 'mastered'"
                :disabled="actionId === wq.id"
                @click="markMastered(wq)"
                class="inline-flex items-center justify-center px-3 py-1.5 rounded-md text-xs font-medium transition hover:opacity-80 disabled:opacity-40"
                style="border: 1px solid #10b981; color: #10b981;"
                title="标记为已掌握"
              >
                <CheckCircle2 class="w-3.5 h-3.5 mr-1" /> 标记掌握
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="mt-8 flex items-center justify-center gap-2">
        <button
          :disabled="page <= 1"
          @click="gotoPage(page - 1)"
          class="px-3 py-1.5 rounded-md text-sm transition disabled:opacity-40 inline-flex items-center"
          style="border: 1px solid var(--theme-border); color: var(--theme-text);"
        >
          <ChevronLeft class="w-4 h-4 mr-0.5" /> 上一页
        </button>
        <span class="text-sm" style="color: var(--theme-text-secondary);">
          {{ page }} / {{ totalPages }}（共 {{ total }} 题）
        </span>
        <button
          :disabled="page >= totalPages"
          @click="gotoPage(page + 1)"
          class="px-3 py-1.5 rounded-md text-sm transition disabled:opacity-40 inline-flex items-center"
          style="border: 1px solid var(--theme-border); color: var(--theme-text);"
        >
          下一页 <ChevronRight class="w-4 h-4 ml-0.5" />
        </button>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>
