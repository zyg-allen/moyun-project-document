<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Briefcase, BookOpen, Star, ArrowRight, Trophy, FileText,
  TrendingUp, Users, CheckCircle, Target, Zap, Building2, Lightbulb
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getInterviewHome } from '@/api/interview';
import type {
  InterviewCategoryVO, InterviewQuestionVO,
  InterviewExperienceVO, InterviewResumeTemplateVO, InterviewCompanyVO,
} from '@/types/api';

const router = useRouter();
const loading = ref(false);
const error = ref<string | null>(null);
const categories = ref<InterviewCategoryVO[]>([]);
const hotQuestions = ref<InterviewQuestionVO[]>([]);
const hotExperiences = ref<InterviewExperienceVO[]>([]);
const resumeTemplates = ref<InterviewResumeTemplateVO[]>([]);
const hotCompanies = ref<InterviewCompanyVO[]>([]);
const totalQuestionCount = ref<number>(0);
const totalSubmissionCount = ref<number>(0);

const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
let toastTimer: number | null = null;
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value = null; }, 3000);
}

onMounted(() => loadInterviewHome());

async function loadInterviewHome() {
  try {
    loading.value = true;
    error.value = null;
    const res = await getInterviewHome();
    if (res.code === 200 && res.data) {
      const d: any = res.data;
      categories.value = d.categories || [];
      hotQuestions.value = d.hotQuestions || [];
      hotExperiences.value = d.hotExperiences || d.experiences || [];
      resumeTemplates.value = d.resumeTemplates || [];
      hotCompanies.value = d.hotCompanies || [];
      totalQuestionCount.value = d.totalQuestionCount || hotQuestions.value.length * 500 || 0;
      totalSubmissionCount.value = d.totalSubmissionCount || hotQuestions.value.length * 1000 || 0;
    } else {
      error.value = res.message || '加载数据失败';
    }
  } catch (err: any) {
    console.error('加载面试指南失败:', err);
    error.value = '加载数据失败，请稍后重试';
    showToast(err?.message || '加载失败', 'error');
  } finally {
    loading.value = false;
  }
}

function getDifficultyColor(difficulty: string) {
  switch (difficulty) {
    case 'easy': return 'bg-green-100 text-green-700';
    case 'medium': return 'bg-yellow-100 text-yellow-700';
    case 'hard': return 'bg-red-100 text-red-700';
    default: return 'bg-gray-100 text-gray-700';
  }
}
function getDifficultyText(difficulty: string) {
  switch (difficulty) {
    case 'easy': return '简单';
    case 'medium': return '中等';
    case 'hard': return '困难';
    default: return difficulty;
  }
}

function expName(exp: InterviewExperienceVO) {
  const u: any = (exp as any).user;
  if (u?.nickname) return u.nickname;
  if ((exp as any).userNickname) return (exp as any).userNickname;
  return '用户';
}
function expAvatar(exp: InterviewExperienceVO) {
  const u: any = (exp as any).user;
  if (u?.avatar) return u.avatar;
  if ((exp as any).userAvatar) return (exp as any).userAvatar;
  return getSafeAvatar('', String(exp.id));
}

function goQuestion(id: any) {
  router.push(`/interview/question/${id}`);
}
function goExperience(id: any) {
  router.push(`/interview/experience/${id}`);
}
function goResume() {
  router.push('/interview/resume-templates');
}

function formatNumber(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n);
}

useHead(computed(() => generateSeo({
  title: '面试指南 - 题库/面经/简历模板',
  description: '墨韵智库面试指南 - 精选算法题库、面试经验分享、简历模板下载',
})));
</script>

<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <!-- Toast -->
    <div
      v-if="toast"
      class="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-lg shadow-lg text-sm"
      :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- Hero 区 -->
    <div class="bg-gradient-to-br from-blue-700 via-indigo-700 to-purple-800 text-white pt-16 pb-20 relative overflow-hidden">
      <div class="absolute inset-0 opacity-10">
        <div class="absolute top-10 left-10 w-64 h-64 rounded-full bg-white"></div>
        <div class="absolute bottom-10 right-20 w-80 h-80 rounded-full bg-white"></div>
      </div>
      <div class="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
        <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-6">
          <Briefcase class="w-4 h-4 mr-2" /> 墨韵 · 面试指南
        </div>
        <div class="flex items-center justify-center mb-4">
          <h1 class="text-5xl font-bold tracking-tight">备战面试，直通 Offer</h1>
        </div>
        <p class="text-xl text-blue-100 max-w-2xl mx-auto mb-10">
          海量算法题库 · 真实面试经验 · 精选简历模板 — 一站式求职备战平台
        </p>
        <!-- 平台统计 -->
        <div class="max-w-4xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <div class="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
            <Target class="w-6 h-6 mx-auto mb-2 text-blue-200" />
            <div class="text-3xl font-bold mb-1">{{ formatNumber(totalQuestionCount) }}</div>
            <div class="text-sm text-blue-100">题目总数</div>
          </div>
          <div class="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
            <Zap class="w-6 h-6 mx-auto mb-2 text-yellow-200" />
            <div class="text-3xl font-bold mb-1">{{ formatNumber(totalSubmissionCount) }}</div>
            <div class="text-sm text-blue-100">提交总数</div>
          </div>
          <div class="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
            <Users class="w-6 h-6 mx-auto mb-2 text-green-200" />
            <div class="text-3xl font-bold mb-1">{{ categories.length }}</div>
            <div class="text-sm text-blue-100">题目分类</div>
          </div>
          <div class="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/20">
            <Building2 class="w-6 h-6 mx-auto mb-2 text-orange-200" />
            <div class="text-3xl font-bold mb-1">{{ hotCompanies.length }}</div>
            <div class="text-sm text-blue-100">热门公司</div>
          </div>
        </div>
        <!-- 快捷入口 -->
        <div class="flex flex-wrap items-center justify-center gap-3">
          <button @click="goResume" class="px-6 py-3 bg-white text-indigo-700 rounded-lg font-medium hover:bg-blue-50 transition flex items-center">
            <FileText class="w-5 h-5 mr-2" />
            简历模板
          </button>
          <button @click="() => {}" class="px-6 py-3 bg-white/10 border border-white/30 text-white rounded-lg font-medium hover:bg-white/20 transition flex items-center backdrop-blur-sm">
            <BookOpen class="w-5 h-5 mr-2" />
            浏览题库
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="flex-1 py-8 -mt-10">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading" class="text-center py-12 bg-white rounded-xl shadow-sm">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p class="mt-4 text-gray-600">加载中...</p>
        </div>

        <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-xl p-8 max-w-2xl mx-auto text-center">
          <p class="text-red-600 mb-4">{{ error }}</p>
          <button @click="loadInterviewHome" class="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition text-sm">
            重试
          </button>
        </div>

        <template v-else>
          <!-- 分类快捷入口 -->
          <div v-if="categories.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <BookOpen class="w-6 h-6 mr-2 text-blue-600" />
                题目分类
              </h2>
              <span class="text-sm text-gray-500">点击卡片进入分类</span>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              <div
                v-for="cat in categories"
                :key="cat.id"
                @click="goQuestion(cat.id)"
                class="bg-white rounded-xl p-6 shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer border border-gray-100"
              >
                <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-500 rounded-lg flex items-center justify-center mb-4 text-white">
                  <BookOpen class="w-6 h-6" />
                </div>
                <h3 class="text-base font-semibold text-gray-900 mb-2">{{ cat.name }}</h3>
                <p v-if="cat.description" class="text-gray-500 text-sm mb-3 line-clamp-2">{{ cat.description }}</p>
                <div class="text-xs text-blue-600 font-medium flex items-center">
                  {{ cat.questionCount || 0 }} 道题目
                  <ArrowRight class="w-3 h-3 ml-1" />
                </div>
              </div>
            </div>
          </div>

          <!-- 热门题目 -->
          <div v-if="hotQuestions.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <Trophy class="w-6 h-6 mr-2 text-yellow-500" />
                热门题目
              </h2>
            </div>
            <div class="space-y-3">
              <div
                v-for="(q, index) in hotQuestions"
                :key="q.id"
                @click="goQuestion(q.id)"
                class="bg-white rounded-xl p-5 shadow-sm hover:shadow-md hover:border-blue-200 border border-transparent transition cursor-pointer"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center mb-2 flex-wrap gap-2">
                      <span class="w-7 h-7 bg-blue-50 rounded-full flex items-center justify-center text-blue-600 font-bold text-xs mr-1">{{ index + 1 }}</span>
                      <span
                        class="px-2.5 py-1 rounded-full text-xs font-medium"
                        :class="getDifficultyColor(q.difficulty)"
                      >
                        {{ getDifficultyText(q.difficulty) }}
                      </span>
                      <span v-if="q.categoryName" class="px-2.5 py-1 bg-gray-100 text-gray-600 rounded-full text-xs">{{ q.categoryName }}</span>
                      <span
                        v-for="tag in q.tags?.slice(0, 3)"
                        :key="tag"
                        class="px-2 py-1 bg-indigo-50 text-indigo-600 rounded text-xs"
                      >
                        #{{ tag }}
                      </span>
                      <span
                        v-for="c in q.companies?.slice(0, 2)"
                        :key="c.id"
                        class="px-2 py-1 bg-orange-50 text-orange-700 rounded text-xs"
                      >
                        {{ c.name }}
                      </span>
                    </div>
                    <h3 class="text-base font-semibold text-gray-900 mb-2">{{ q.title }}</h3>
                    <p v-if="q.description" class="text-sm text-gray-500 line-clamp-2 mb-2">{{ q.description }}</p>
                  </div>
                  <div class="text-right ml-6 flex-shrink-0">
                    <div class="flex items-center justify-end gap-3 text-xs text-gray-500 mb-2">
                      <span class="flex items-center"><CheckCircle class="w-3 h-3 mr-1 text-green-500" /> {{ q.acceptanceRate }}%</span>
                      <span class="flex items-center"><Zap class="w-3 h-3 mr-1 text-yellow-500" /> {{ q.submissionCount }}</span>
                      <span class="flex items-center"><Star class="w-3 h-3 mr-1 text-orange-500" /> {{ q.likeCount }}</span>
                    </div>
                    <span class="text-xs text-blue-600 font-medium flex items-center justify-end">
                      查看题目 <ArrowRight class="w-3 h-3 ml-1" />
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门面经 -->
          <div v-if="hotExperiences.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <Briefcase class="w-6 h-6 mr-2 text-orange-500" />
                热门面经
              </h2>
              <span class="text-sm text-gray-500">真实面试经验分享</span>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              <div
                v-for="exp in hotExperiences"
                :key="exp.id"
                @click="goExperience(exp.id)"
                class="bg-white rounded-xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer border border-gray-100 flex flex-col"
              >
                <div v-if="exp.coverImage" class="h-36 bg-gray-100">
                  <LazyImage :src="exp.coverImage" :alt="exp.title" class="w-full h-full object-cover" />
                </div>
                <div class="p-5 flex flex-col flex-1">
                  <div class="flex items-center gap-2 mb-3 flex-wrap">
                    <span class="px-2.5 py-1 bg-indigo-50 text-indigo-700 rounded-full text-xs font-medium">{{ exp.company }}</span>
                    <span v-if="exp.position" class="px-2.5 py-1 bg-blue-50 text-blue-700 rounded-full text-xs font-medium">{{ exp.position }}</span>
                    <span v-if="exp.year" class="text-xs text-gray-400">{{ exp.year }}年</span>
                  </div>
                  <h3 class="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">{{ exp.title }}</h3>
                  <p v-if="exp.summary || exp.content" class="text-sm text-gray-600 mb-4 line-clamp-3 flex-1">
                    {{ exp.summary || exp.content }}
                  </p>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center text-sm text-gray-600">
                      <div class="w-7 h-7 rounded-full overflow-hidden mr-2 bg-gray-200">
                        <LazyImage :src="expAvatar(exp)" :alt="expName(exp)" class="w-full h-full object-cover" />
                      </div>
                      <span class="font-medium text-xs">{{ expName(exp) }}</span>
                    </div>
                    <div class="flex items-center gap-3 text-xs text-gray-500">
                      <span class="flex items-center"><Star class="w-3 h-3 mr-1 text-orange-400" />{{ exp.likeCount }}</span>
                      <span class="flex items-center"><TrendingUp class="w-3 h-3 mr-1 text-blue-400" />{{ exp.viewCount }}</span>
                      <span class="flex items-center"><BookOpen class="w-3 h-3 mr-1 text-green-400" />{{ exp.commentCount }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 简历模板 -->
          <div v-if="resumeTemplates.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <FileText class="w-6 h-6 mr-2 text-purple-500" />
                简历模板
              </h2>
              <button @click="goResume" class="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center">
                查看更多 <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
              <div
                v-for="t in resumeTemplates.slice(0, 4)"
                :key="t.id"
                @click="goResume()"
                class="bg-white rounded-xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer border border-gray-100"
              >
                <div class="h-40 bg-gray-100">
                  <LazyImage v-if="t.cover" :src="t.cover" :alt="t.title" class="w-full h-full object-cover" />
                  <div v-else class="flex items-center justify-center h-full bg-gradient-to-br from-purple-100 to-indigo-100">
                    <FileText class="w-10 h-10 text-purple-400" />
                  </div>
                </div>
                <div class="p-4">
                  <h3 class="text-base font-semibold text-gray-900 mb-1 line-clamp-1">{{ t.title }}</h3>
                  <p v-if="t.description" class="text-sm text-gray-500 mb-3 line-clamp-2">{{ t.description }}</p>
                  <div class="flex items-center justify-between text-xs text-gray-500">
                    <span class="flex items-center"><Star class="w-3 h-3 mr-1 text-orange-400" />{{ t.likeCount }}</span>
                    <span class="flex items-center"><FileText class="w-3 h-3 mr-1 text-blue-400" />{{ t.downloadCount }} 下载</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门公司墙 -->
          <div v-if="hotCompanies.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold text-gray-900 flex items-center">
                <Building2 class="w-6 h-6 mr-2 text-blue-500" />
                热门公司
              </h2>
              <span class="text-sm text-gray-500">高频出现公司</span>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
              <div
                v-for="c in hotCompanies"
                :key="c.id"
                class="bg-white rounded-xl p-5 shadow-sm hover:shadow-md hover:-translate-y-1 transition cursor-pointer text-center border border-gray-100"
              >
                <div class="w-14 h-14 mx-auto rounded-xl bg-gradient-to-br from-gray-100 to-gray-200 flex items-center justify-center mb-3 overflow-hidden">
                  <LazyImage v-if="c.logo" :src="c.logo" :alt="c.name" class="w-full h-full object-contain" />
                  <Lightbulb v-else class="w-7 h-7 text-gray-400" />
                </div>
                <h3 class="text-sm font-semibold text-gray-900 mb-1 line-clamp-1">{{ c.name }}</h3>
                <p class="text-xs text-gray-500">{{ c.questionCount || 0 }} 道题</p>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
