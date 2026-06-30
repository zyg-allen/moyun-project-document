<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Briefcase, Search, Star, ArrowLeft, CheckCircle, Zap,
  ChevronLeft, ChevronRight, BookOpen,
} from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getQuestionList, getInterviewCategoryList } from '@/api/interview';
import type {
  InterviewQuestionVO,
  InterviewCategoryVO,
  InterviewQuestionQuery,
} from '@/types/api';

const route = useRoute();
const router = useRouter();

// ========== 筛选状态（支持从 URL query 初始化） ==========
const activeCategoryId = ref<string | number | null>(
  (route.query.categoryId as string) || null
);
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
const categories = ref<InterviewCategoryVO[]>([]);

// ========== 难度配置 ==========
const difficultyOptions = [
  { key: '', label: '全部' },
  { key: 'easy', label: '简单' },
  { key: 'medium', label: '中等' },
  { key: 'hard', label: '困难' },
];

const difficultyMap: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

// ========== SEO ==========
useHead(computed(() => generateSeo({
  title: '面试题库',
  description: '海量面试题目，涵盖算法、系统设计、前端、后端等方向，助你高效备战面试',
  canonicalPath: '/interview/questions',
})));

// ========== 生命周期 ==========
onMounted(() => {
  loadCategories();
  loadQuestions();
});

watch([activeCategoryId, activeDifficulty, keyword, page], () => {
  loadQuestions();
});

// ========== 数据加载 ==========
async function loadCategories() {
  try {
    const res = await getInterviewCategoryList();
    if (res.code === 200 && res.data) {
      categories.value = res.data;
    }
  } catch (err) {
    console.error('加载分类失败:', err);
  }
}

async function loadQuestions() {
  try {
    loading.value = true;
    error.value = null;
    const params: InterviewQuestionQuery = {
      pageNum: page.value,
      pageSize,
    };
    if (activeCategoryId.value) params.categoryId = activeCategoryId.value;
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
    console.error('加载题目列表失败:', err);
    error.value = err?.message || '加载题目失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

// ========== 事件处理 ==========
function doSearch() {
  keyword.value = searchInput.value.trim();
  page.value = 1;
}

function selectCategory(id: string | number | null) {
  activeCategoryId.value = id;
  page.value = 1;
}

function selectDifficulty(d: string) {
  activeDifficulty.value = d;
  page.value = 1;
}

function gotoQuestion(id: string | number) {
  router.push(`/interview/question/${id}`);
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages.value) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function goBack() {
  router.push('/interview');
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部导航栏 -->
    <div
      class="border-b sticky top-0 z-30"
      style="background-color: var(--theme-bg); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 py-3">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-70"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回面试指南
        </button>
      </div>
    </div>

    <!-- Hero 区 -->
    <div
      class="text-white py-14 relative overflow-hidden"
      style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 60%, #4338ca 100%));"
    >
      <div class="absolute inset-0 opacity-10 pointer-events-none">
        <div class="absolute top-10 left-10 w-64 h-64 rounded-full bg-white"></div>
        <div class="absolute bottom-0 right-20 w-80 h-80 rounded-full bg-white"></div>
      </div>
      <div class="relative max-w-7xl mx-auto px-4 text-center">
        <h1 class="text-4xl font-bold mb-3">面试题库</h1>
        <p class="text-lg mb-8 opacity-90">海量面试题目，涵盖算法、系统设计、前端、后端等方向</p>
        <div class="relative max-w-xl mx-auto rounded-lg p-2 flex" style="background-color: var(--theme-bg);">
          <Search
            class="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2"
            style="color: var(--theme-text-secondary);"
          />
          <label for="question-search-input" class="sr-only">搜索题目</label>
          <input
            id="question-search-input"
            v-model="searchInput"
            @keyup.enter="doSearch"
            type="text"
            placeholder="搜索题目..."
            class="search-input flex-1 pl-10 pr-3 py-2 focus:outline-none text-sm"
            style="color: var(--theme-text);"
          />
          <button
            @click="doSearch"
            class="px-4 py-2 text-white rounded-md text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            搜索
          </button>
        </div>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex flex-col lg:flex-row gap-6">
          <!-- 左侧筛选栏 -->
          <aside class="w-full lg:w-64 flex-shrink-0">
            <!-- 分类筛选 -->
            <div
              class="rounded-xl shadow-sm p-4 mb-4"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                <BookOpen class="w-4 h-4 mr-2" />
                题目分类
              </h3>
              <div class="space-y-1">
                <button
                  @click="selectCategory(null)"
                  class="filter-btn w-full text-left px-3 py-2 rounded-lg text-sm transition"
                  :class="{ active: activeCategoryId === null }"
                >
                  全部题目
                </button>
                <button
                  v-for="cat in categories"
                  :key="cat.id"
                  @click="selectCategory(cat.id)"
                  class="filter-btn w-full text-left px-3 py-2 rounded-lg text-sm transition flex items-center justify-between"
                  :class="{ active: activeCategoryId === cat.id }"
                >
                  <span class="truncate">{{ cat.name }}</span>
                  <span class="text-xs opacity-70 ml-2 flex-shrink-0">{{ cat.questionCount || 0 }}</span>
                </button>
              </div>
            </div>

            <!-- 难度筛选 -->
            <div
              class="rounded-xl shadow-sm p-4"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                <Zap class="w-4 h-4 mr-2" />
                难度
              </h3>
              <div class="space-y-1">
                <button
                  v-for="d in difficultyOptions"
                  :key="d.key"
                  @click="selectDifficulty(d.key)"
                  class="filter-btn w-full text-left px-3 py-2 rounded-lg text-sm transition"
                  :class="{ active: activeDifficulty === d.key }"
                >
                  {{ d.label }}
                </button>
              </div>
            </div>
          </aside>

          <!-- 右侧题目列表 -->
          <div class="flex-1 min-w-0">
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
              class="rounded-xl p-8 text-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <p class="mb-4" style="color: var(--theme-primary);">{{ error }}</p>
              <button
                @click="loadQuestions"
                class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
                style="background-color: var(--theme-primary);"
              >
                重试
              </button>
            </div>

            <!-- 空数据状态 -->
            <div
              v-else-if="questions.length === 0"
              class="rounded-xl py-16 text-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <BookOpen
                class="w-12 h-12 mx-auto mb-3"
                style="color: var(--theme-text-secondary); opacity: 0.4;"
              />
              <p style="color: var(--theme-text-secondary);">暂无题目</p>
            </div>

            <!-- 题目列表 -->
            <template v-else>
              <div class="space-y-4">
                <div
                  v-for="q in questions"
                  :key="q.id"
                  @click="gotoQuestion(q.id)"
                  class="rounded-xl shadow-sm hover:shadow-md transition cursor-pointer p-5"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
                >
                  <!-- 标签行 -->
                  <div class="flex items-center flex-wrap gap-2 mb-2">
                    <span
                      class="px-2.5 py-1 rounded-full text-xs font-medium"
                      :class="difficultyMap[q.difficulty]?.class"
                    >
                      {{ difficultyMap[q.difficulty]?.label || q.difficulty }}
                    </span>
                    <span
                      v-if="q.categoryName"
                      class="px-2.5 py-1 rounded-full text-xs font-medium"
                      style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                    >
                      <BookOpen class="w-3 h-3 inline mr-1" />
                      {{ q.categoryName }}
                    </span>
                    <span
                      v-for="tag in (q.tags || []).slice(0, 3)"
                      :key="tag"
                      class="px-2 py-1 rounded text-xs"
                      style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                    >
                      #{{ tag }}
                    </span>
                  </div>

                  <!-- 标题 -->
                  <h3 class="text-base font-semibold mb-1" style="color: var(--theme-text);">
                    {{ q.title }}
                  </h3>

                  <!-- 描述 -->
                  <p
                    v-if="q.description"
                    class="text-sm line-clamp-2 mb-3"
                    style="color: var(--theme-text-secondary);"
                  >
                    {{ q.description }}
                  </p>

                  <!-- 公司标签 -->
                  <div
                    v-if="q.companies && q.companies.length > 0"
                    class="flex items-center flex-wrap gap-2 mb-3"
                  >
                    <Briefcase class="w-3 h-3 flex-shrink-0" style="color: var(--theme-text-secondary);" />
                    <span
                      v-for="c in q.companies.slice(0, 4)"
                      :key="c.id"
                      class="px-2 py-0.5 rounded text-xs"
                      style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                    >
                      {{ c.name }}
                    </span>
                  </div>

                  <!-- 统计信息 -->
                  <div class="flex items-center gap-4 text-xs" style="color: var(--theme-text-secondary);">
                    <span class="flex items-center">
                      <CheckCircle class="w-3 h-3 mr-1" />
                      通过率 {{ q.acceptanceRate }}%
                    </span>
                    <span class="flex items-center">
                      <Zap class="w-3 h-3 mr-1" />
                      {{ q.submissionCount }} 次提交
                    </span>
                    <span class="flex items-center">
                      <Star class="w-3 h-3 mr-1" />
                      {{ q.likeCount }} 点赞
                    </span>
                  </div>
                </div>
              </div>

              <!-- 分页 -->
              <div v-if="totalPages > 1" class="flex items-center justify-center gap-3 mt-8">
                <button
                  @click="gotoPage(page - 1)"
                  :disabled="page === 1"
                  :aria-label="`第 ${page - 1} 页`"
                  class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);"
                >
                  <ChevronLeft class="w-4 h-4" />
                </button>
                <span class="text-sm" style="color: var(--theme-text-secondary);">
                  第 {{ page }} / {{ totalPages }} 页 · 共 {{ total }} 题
                </span>
                <button
                  @click="gotoPage(page + 1)"
                  :disabled="page === totalPages"
                  :aria-label="`第 ${page + 1} 页`"
                  class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40"
                  style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);"
                >
                  <ChevronRight class="w-4 h-4" />
                </button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>

<style scoped>
.search-input::placeholder {
  color: var(--theme-text-secondary);
  opacity: 0.5;
}

.filter-btn {
  color: var(--theme-text-secondary);
}
.filter-btn:hover {
  background-color: var(--theme-surface);
}
.filter-btn.active {
  background-color: var(--theme-primary);
  color: #ffffff;
}
.filter-btn.active:hover {
  opacity: 0.9;
}
</style>
