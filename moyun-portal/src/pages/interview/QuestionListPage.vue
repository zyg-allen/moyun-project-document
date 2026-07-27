<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Briefcase, Search, Star, CheckCircle, Zap,
  ChevronLeft, ChevronRight, BookOpen,
  Sparkles, Target, TrendingUp, RefreshCw,
} from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getQuestionList, getInterviewCategoryList, getRecommendedQuestions } from '@/api/interview';
import { getMyMockProfile } from '@/api/mockInterview';
import { useAuth } from '@/composables/useAuth';
import type {
  InterviewQuestionVO,
  InterviewCategoryVO,
  InterviewQuestionQuery,
  UserProfileSnapshotVO,
} from '@/types/api';

const route = useRoute();
const router = useRouter();
const { isAuthenticated } = useAuth();

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

// ========== 画像推荐（v5.9 阶段1） ==========
const recoLoading = ref(false);
const recoQuestions = ref<InterviewQuestionVO[]>([]);
const profile = ref<UserProfileSnapshotVO | null>(null);
const showRecommend = computed(() => isAuthenticated() && recoQuestions.value.length > 0);

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

// ========== 推荐来源映射 ==========
const reasonMap: Record<string, { label: string; class: string; icon: any }> = {
  weak_tag: { label: '薄弱点', class: 'bg-red-50 text-red-600 border border-red-200', icon: Target },
  required_skill: { label: '必备技能', class: 'bg-blue-50 text-blue-600 border border-blue-200', icon: Zap },
  hot: { label: '热门推荐', class: 'bg-amber-50 text-amber-600 border border-amber-200', icon: TrendingUp },
};

// ========== SEO ==========
useHead(computed(() => generateSeo({
  title: '面试题库',
  description: '海量面试题目，涵盖算法、系统设计、前端、后端等方向，助你高效备战面试',
  canonicalPath: '/interview/questions',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '面试题库' },
]);

// ========== 生命周期 ==========
onMounted(() => {
  loadCategories();
  loadQuestions();
  loadRecommendations();
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

/** 加载画像推荐题目与画像快照（仅登录用户） */
async function loadRecommendations() {
  if (!isAuthenticated()) return;
  try {
    recoLoading.value = true;
    // 并行拉取推荐题目与画像快照
    const [recoRes, profileRes] = await Promise.all([
      getRecommendedQuestions(6),
      getMyMockProfile({}).catch(() => null),
    ]);
    if (recoRes.code === 200 && recoRes.data) {
      recoQuestions.value = recoRes.data;
    }
    if (profileRes && profileRes.code === 200 && profileRes.data) {
      profile.value = profileRes.data;
    }
  } catch (err) {
    // 未登录或画像构建失败时静默隐藏推荐模块
    console.warn('加载推荐题目失败:', err);
    recoQuestions.value = [];
    profile.value = null;
  } finally {
    recoLoading.value = false;
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
        <span class="w-12"></span>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 搜索工具栏 -->
        <div class="mb-6 max-w-xl mx-auto">
          <div class="flex items-center rounded-xl border px-3 py-1" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <Search class="w-5 h-5 flex-shrink-0" style="color: var(--theme-text-secondary);" />
            <label for="question-search-input" class="sr-only">搜索题目</label>
            <input
              id="question-search-input"
              v-model="searchInput"
              @keyup.enter="doSearch"
              type="text"
              placeholder="搜索题目..."
              class="search-input flex-1 px-3 py-2 focus:outline-none text-sm"
              style="color: var(--theme-text);"
            />
            <button
              @click="doSearch"
              class="px-5 py-1.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              搜索
            </button>
          </div>
        </div>

        <!-- 为你推荐（v5.9 阶段1：基于用户画像推荐） -->
        <section
          v-if="showRecommend"
          class="mb-6 rounded-2xl overflow-hidden"
          style="background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-primary-dark, #4f46e5) 100%);"
        >
          <div class="px-5 py-4 sm:px-6 sm:py-5 text-white">
            <!-- 标题行 -->
            <div class="flex items-center justify-between gap-3 mb-3">
              <div class="flex items-center gap-2">
                <Sparkles class="w-5 h-5 flex-shrink-0" />
                <h2 class="text-base sm:text-lg font-semibold">为你推荐</h2>
                <span class="text-xs opacity-80 hidden sm:inline">基于你的画像（薄弱点 · 岗位必备技能）智能召回</span>
              </div>
              <button
                @click="loadRecommendations"
                :disabled="recoLoading"
                class="flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs bg-white/15 hover:bg-white/25 transition disabled:opacity-50"
                aria-label="刷新推荐"
              >
                <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': recoLoading }" />
                <span class="hidden sm:inline">刷新</span>
              </button>
            </div>

            <!-- 画像摘要（薄弱点 + 必备技能 + 面试统计） -->
            <div
              v-if="profile"
              class="flex flex-wrap items-center gap-2 mb-4 text-xs"
            >
              <span
                v-if="profile.weakTags && profile.weakTags.length > 0"
                class="flex items-center gap-1 px-2 py-1 rounded-full bg-white/15"
              >
                <Target class="w-3 h-3" />
                薄弱点 {{ profile.weakTags.length }}
              </span>
              <span
                v-if="profile.requiredSkills && profile.requiredSkills.length > 0"
                class="flex items-center gap-1 px-2 py-1 rounded-full bg-white/15"
              >
                <Zap class="w-3 h-3" />
                必备技能 {{ profile.requiredSkills.length }}
              </span>
              <span
                v-if="profile.mockInterviewCount != null && profile.mockInterviewCount > 0"
                class="flex items-center gap-1 px-2 py-1 rounded-full bg-white/15"
              >
                <CheckCircle class="w-3 h-3" />
                模拟面试 {{ profile.mockInterviewCount }} 次
              </span>
              <span
                v-if="profile.avgMockScore != null && profile.avgMockScore > 0"
                class="flex items-center gap-1 px-2 py-1 rounded-full bg-white/15"
              >
                <Star class="w-3 h-3" />
                平均分 {{ profile.avgMockScore }}
              </span>
              <span
                v-if="!profile.personalized"
                class="px-2 py-1 rounded-full bg-white/15"
              >
                暂无画像数据，先答题或模拟面试以激活个性化推荐
              </span>
            </div>

            <!-- 薄弱点标签云 -->
            <div
              v-if="profile && profile.weakTags && profile.weakTags.length > 0"
              class="flex flex-wrap items-center gap-1.5 mb-4"
            >
              <span class="text-xs opacity-80 mr-1">薄弱：</span>
              <span
                v-for="wt in profile.weakTags.slice(0, 6)"
                :key="wt.tagId"
                class="px-2 py-0.5 rounded text-xs bg-white/15 hover:bg-white/25 transition cursor-default"
                :title="`答 ${wt.total} 题，通过 ${wt.solved}，失败率 ${(wt.failRate * 100).toFixed(0)}%`"
              >
                {{ wt.tagName }}
              </span>
            </div>
          </div>

          <!-- 推荐题目卡片网格 -->
          <div class="px-3 sm:px-4 pb-4">
            <div
              v-if="recoLoading"
              class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3"
            >
              <div
                v-for="n in 6"
                :key="n"
                class="rounded-xl p-4 animate-pulse"
                style="background-color: rgba(255,255,255,0.95); height: 120px;"
              >
                <div class="h-3 w-16 bg-gray-200 rounded mb-3"></div>
                <div class="h-4 w-3/4 bg-gray-200 rounded mb-2"></div>
                <div class="h-3 w-1/2 bg-gray-100 rounded"></div>
              </div>
            </div>
            <div
              v-else
              class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3"
            >
              <div
                v-for="q in recoQuestions"
                :key="q.id"
                @click="gotoQuestion(q.id)"
                class="rounded-xl p-4 transition cursor-pointer hover:shadow-lg hover:-translate-y-0.5"
                style="background-color: rgba(255,255,255,0.97);"
              >
                <!-- 推荐来源徽章 + 难度 -->
                <div class="flex items-center justify-between gap-2 mb-2">
                  <span
                    v-if="q.recommendReason && reasonMap[q.recommendReason]"
                    class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium"
                    :class="reasonMap[q.recommendReason].class"
                  >
                    <component
                      :is="reasonMap[q.recommendReason].icon"
                      class="w-3 h-3"
                    />
                    {{ reasonMap[q.recommendReason].label }}
                  </span>
                  <span
                    v-else
                    class="px-2 py-0.5 rounded text-xs font-medium"
                    :class="difficultyMap[q.difficulty]?.class"
                  >
                    {{ difficultyMap[q.difficulty]?.label || q.difficulty }}
                  </span>
                  <span
                    v-if="q.recommendTag"
                    class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 truncate max-w-[50%]"
                    :title="q.recommendTag"
                  >
                    #{{ q.recommendTag }}
                  </span>
                </div>

                <!-- 标题 -->
                <h3 class="text-sm font-semibold mb-1.5 line-clamp-2" style="color: var(--theme-text);">
                  {{ q.title }}
                </h3>

                <!-- 描述 -->
                <p
                  v-if="q.description"
                  class="text-xs line-clamp-2 mb-2"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ q.description }}
                </p>

                <!-- 统计 -->
                <div class="flex items-center gap-3 text-xs" style="color: var(--theme-text-secondary);">
                  <span class="flex items-center">
                    <CheckCircle class="w-3 h-3 mr-1" />
                    {{ q.acceptanceRate }}%
                  </span>
                  <span class="flex items-center">
                    <Zap class="w-3 h-3 mr-1" />
                    {{ q.submissionCount }}
                  </span>
                  <span class="flex items-center">
                    <Star class="w-3 h-3 mr-1" />
                    {{ q.likeCount }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>

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
