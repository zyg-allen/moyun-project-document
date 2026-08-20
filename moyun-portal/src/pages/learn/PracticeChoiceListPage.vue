<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Search, CheckSquare, ChevronLeft, ChevronRight,
  Loader2, AlertCircle, BookOpen, Filter,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getQuestionList } from '@/api/interview';
import type { InterviewQuestionVO, InterviewQuestionQuery } from '@/types/api';

const router = useRouter();
const route = useRoute();

// ========== 筛选 ==========
const activeDifficulty = ref<string>((route.query.difficulty as string) || '');
const keyword = ref<string>((route.query.keyword as string) || '');
const searchInput = ref(keyword.value);

// ========== 分页 ==========
const page = ref<number>(parseInt(route.query.page as string) || 1);
const pageSize = 10;
const total = ref(0);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));

// ========== 数据 ==========
const loading = ref(false);
const error = ref<string | null>(null);
const questions = ref<InterviewQuestionVO[]>([]);

useHead(computed(() => generateSeo({
  title: '选择题练习',
  description: '在线选择题练习 - 计算机基础、八股文选择题库，即时判定与解析',
  keywords: ['选择题', '练习', '八股', '计算机基础', '墨韵'],
  canonicalPath: '/learn/practice/choice',
})));

// 难度配置
const DIFFICULTY_OPTIONS = [
  { label: '全部', value: '' },
  { label: '简单', value: 'easy' },
  { label: '中等', value: 'medium' },
  { label: '困难', value: 'hard' },
];
const DIFFICULTY_MAP: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '刷题中心', path: '/learn/practice' },
  { label: '选择题' },
]);

async function loadQuestions() {
  loading.value = true;
  error.value = null;
  try {
    const params: InterviewQuestionQuery = {
      pageNum: page.value,
      pageSize,
      // v10.6：按练习模式筛选选择题（practice_mode=choice）
      practiceMode: 'choice',
    };
    if (activeDifficulty.value) params.difficulty = activeDifficulty.value;
    if (keyword.value) params.keyword = keyword.value;
    const res = await getQuestionList(params);
    if (res.code === 200 && res.data) {
      questions.value = res.data.list || [];
      total.value = res.data.total || 0;
    } else {
      error.value = res.message || '加载题目失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载题目失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function doSearch() {
  keyword.value = searchInput.value.trim();
  page.value = 1;
}

function selectDifficulty(d: string) {
  activeDifficulty.value = d;
  page.value = 1;
}

function gotoQuestion(id: string | number) {
  // v10.6：选择题跳转做题页
  router.push(`/learn/practice/choice/${id}`);
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 监听筛选与分页变化自动加载
watch([activeDifficulty, keyword, page], () => {
  loadQuestions();
});

onMounted(() => {
  loadQuestions();
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <main class="flex-1 w-full max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <!-- 面包屑 -->
      <Breadcrumb :items="breadcrumbs" />

      <!-- 页头 -->
      <div class="mt-4 mb-6 flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl flex items-center justify-center text-white"
             style="background: linear-gradient(135deg, #DC2626, #B91C1C);">
          <CheckSquare class="w-5 h-5" />
        </div>
        <div>
          <h1 class="text-xl font-bold" style="color: var(--theme-text);">选择题练习</h1>
          <p class="text-xs" style="color: var(--theme-text-secondary);">在线选择题练习 · 即时判定与解析</p>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="mb-5 p-4 rounded-xl border" style="background-color: var(--theme-card-bg); border-color: var(--theme-border);">
        <div class="flex flex-wrap items-center gap-3">
          <!-- 难度筛选 -->
          <div class="flex items-center gap-1.5">
            <Filter class="w-3.5 h-3.5" style="color: var(--theme-text-secondary);" />
            <span class="text-xs" style="color: var(--theme-text-secondary);">难度</span>
            <button
              v-for="opt in DIFFICULTY_OPTIONS"
              :key="opt.value"
              @click="selectDifficulty(opt.value)"
              :class="[
                'px-2.5 py-1 rounded-md text-xs font-medium transition-colors',
                activeDifficulty === opt.value
                  ? 'text-white'
                  : 'border'
              ]"
              :style="activeDifficulty === opt.value
                ? { backgroundColor: 'var(--theme-primary)' }
                : { borderColor: 'var(--theme-border)', color: 'var(--theme-text)' }"
            >
              {{ opt.label }}
            </button>
          </div>
          <!-- 搜索 -->
          <div class="flex-1 min-w-[200px] flex items-center gap-1.5">
            <div class="relative flex-1">
              <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4" style="color: var(--theme-text-secondary);" />
              <input
                v-model="searchInput"
                @keyup.enter="doSearch"
                type="text"
                placeholder="搜索题目..."
                class="w-full pl-8 pr-3 py-1.5 text-sm rounded-md border outline-none"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>
            <button
              @click="doSearch"
              class="px-3 py-1.5 text-xs font-medium text-white rounded-md"
              style="background-color: var(--theme-primary);"
            >搜索</button>
          </div>
        </div>
      </div>

      <!-- 内容区 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-16">
        <Loader2 class="w-6 h-6 animate-spin mb-2" style="color: var(--theme-primary);" />
        <p class="text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <div v-else-if="error" class="flex flex-col items-center justify-center py-16">
        <AlertCircle class="w-6 h-6 mb-2" style="color: #DC2626;" />
        <p class="text-sm mb-3" style="color: var(--theme-text);">{{ error }}</p>
        <button
          @click="loadQuestions"
          class="px-3 py-1.5 text-xs text-white rounded-md"
          style="background-color: var(--theme-primary);"
        >重试</button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="questions.length === 0" class="flex flex-col items-center justify-center py-16">
        <BookOpen class="w-10 h-10 mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
        <p class="text-sm mb-1" style="color: var(--theme-text);">暂无选择题</p>
        <p class="text-xs" style="color: var(--theme-text-secondary);">题库正在建设中，敬请期待</p>
      </div>

      <!-- 题目列表 -->
      <div v-else class="space-y-2.5">
        <div
          v-for="q in questions"
          :key="q.id"
          @click="gotoQuestion(q.id)"
          class="p-3.5 rounded-xl border cursor-pointer transition-all hover:shadow-md group"
          style="background-color: var(--theme-card-bg); border-color: var(--theme-border);"
        >
          <div class="flex items-start gap-3">
            <!-- 序号 -->
            <div class="w-8 h-8 rounded-md flex items-center justify-center text-xs font-bold flex-shrink-0 text-white"
                 style="background: linear-gradient(135deg, #DC2626, #B91C1C);">
              <CheckSquare class="w-3.5 h-3.5" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-xs px-1.5 py-0.5 rounded font-medium"
                      :class="DIFFICULTY_MAP[q.difficulty]?.class || 'bg-gray-100 text-gray-600'">
                  {{ DIFFICULTY_MAP[q.difficulty]?.label || q.difficulty || '未分级' }}
                </span>
                <span v-if="q.tags && q.tags.length" class="text-[10px]" style="color: var(--theme-text-secondary);">{{ q.tags.join('、') }}</span>
              </div>
              <h3 class="text-sm font-semibold line-clamp-2 group-hover:text-red-600 transition-colors"
                  style="color: var(--theme-text);">
                {{ q.title }}
              </h3>
              <p v-if="q.description" class="text-xs mt-1 line-clamp-2"
                 style="color: var(--theme-text-secondary);" v-html="q.description.slice(0, 120) + (q.description.length > 120 ? '...' : '')"></p>
              <div class="flex items-center gap-3 mt-2 text-[10px]" style="color: var(--theme-text-secondary);">
                <span v-if="q.companies && q.companies.length">🏢 {{ q.companies.map(c => c.name).join('、') }}</span>
                <span v-if="q.submissionCount">📝 {{ q.submissionCount }} 人练过</span>
                <span v-if="q.acceptanceRate">✓ {{ q.acceptanceRate }}% 通过</span>
              </div>
            </div>
            <ChevronRight class="w-4 h-4 flex-shrink-0 mt-1 group-hover:text-red-600 transition-colors"
                          style="color: var(--theme-text-secondary);" />
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="flex items-center justify-center gap-2 mt-6">
        <button
          @click="gotoPage(page - 1)"
          :disabled="page <= 1"
          class="px-2.5 py-1 rounded-md text-xs border disabled:opacity-40"
          style="border-color: var(--theme-border); color: var(--theme-text);"
        >
          <ChevronLeft class="w-3.5 h-3.5" />
        </button>
        <span class="text-xs px-2" style="color: var(--theme-text);">
          {{ page }} / {{ totalPages }}
        </span>
        <button
          @click="gotoPage(page + 1)"
          :disabled="page >= totalPages"
          class="px-2.5 py-1 rounded-md text-xs border disabled:opacity-40"
          style="border-color: var(--theme-border); color: var(--theme-text);"
        >
          <ChevronRight class="w-3.5 h-3.5" />
        </button>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>
