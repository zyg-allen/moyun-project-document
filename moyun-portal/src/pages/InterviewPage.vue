<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Briefcase, BookOpen, Star, ArrowRight, Trophy, FileText,
  TrendingUp, Users, CheckCircle, Target, Zap, Building2, Lightbulb,
  Mic, Sparkles, PlayCircle, Clock, BarChart3, MessageSquare
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getInterviewHome } from '@/api/interview';
import { useToast } from '@/composables/useToast';
import { useDictData, dictBadgeClass } from '@/composables/useDictData';
import { useAuth } from '@/composables/useAuth';
import type {
  InterviewCategoryVO, InterviewQuestionVO,
  InterviewExperienceVO, InterviewResumeTemplateVO, InterviewCompanyVO,
} from '@/types/api';

const router = useRouter();
const toast = useToast();
const { requireAuth } = useAuth();
const loading = ref(false);
const error = ref<string | null>(null);
const categories = ref<InterviewCategoryVO[]>([]);
const hotQuestions = ref<InterviewQuestionVO[]>([]);
const hotExperiences = ref<InterviewExperienceVO[]>([]);
const resumeTemplates = ref<InterviewResumeTemplateVO[]>([]);
const hotCompanies = ref<InterviewCompanyVO[]>([]);
const totalQuestionCount = ref<number>(0);
const totalSubmissionCount = ref<number>(0);

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
    toast.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

// 难度展示（字典 portal_question_difficulty 驱动，本地默认兜底）
const dictMap = useDictData(['portal_question_difficulty']);

function findDifficultyItem(difficulty: string) {
  return (dictMap['portal_question_difficulty'] || []).find(d => d.dictValue === difficulty);
}
function getDifficultyColor(difficulty: string) {
  const dictClass = dictBadgeClass(findDifficultyItem(difficulty)?.listClass);
  if (dictClass) return dictClass;
  switch (difficulty) {
    case 'easy': return 'bg-theme-success-bg text-theme-success';
    case 'medium': return 'bg-theme-warning-bg text-theme-warning';
    case 'hard': return 'bg-theme-danger-bg text-theme-danger';
    default: return 'bg-theme-bg text-theme-text-secondary';
  }
}
function getDifficultyText(difficulty: string) {
  const dictLabel = findDifficultyItem(difficulty)?.dictLabel;
  if (dictLabel) return dictLabel;
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
function goMyResume() {
  router.push('/interview/my/resumes');
}
function goVoiceInterview() {
  if (!requireAuth('/interview/voice')) return;
  router.push('/interview/voice');
}
function goMyAttempts() {
  if (!requireAuth('/interview/my/attempts')) return;
  router.push('/interview/my/attempts');
}

function formatNumber(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n);
}

useHead(computed(() => generateSeo({
  title: '面试指南 - 题库/面经/简历模板',
  description: '旭林知行面试指南 - 精选算法题库、面试经验分享、简历模板下载',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南' },
]);
</script>

<template>
  <div class="min-h-screen flex flex-col bg-theme-bg">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b border-theme-border sticky top-0 z-30 backdrop-blur-sm py-3 bg-theme-surface/90">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <router-link
          to="/interview/resume/edit"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg meta-text font-medium text-white transition hover:opacity-90 flex-shrink-0 bg-theme-primary"
          title="维护我的简历（教育、工作、项目、技能等）"
        >
          <FileText class="w-3.5 h-3.5" />
          维护我的简历
        </router-link>
      </div>
    </div>

    <!-- Hero 区：轻量、无深色背景、压缩高度 -->
    <div class="pt-4 pb-5 sm:pt-6 sm:pb-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-theme-primary-soft to-theme-surface border border-theme-border">
          <div class="relative px-5 py-6 sm:px-8 sm:py-7 text-center">
            <h1 class="page-title mb-2">备战面试，直通 Offer</h1>
            <p class="body-text text-theme-text-secondary max-w-2xl mx-auto mb-4">
              海量算法题库 · 真实面试经验 · 精选简历模板
            </p>

            <!-- 平台统计：横向紧凑排列，弱化模块感 -->
            <div class="flex flex-wrap items-center justify-center gap-x-6 gap-y-2 mb-4">
              <div class="flex items-center gap-1.5">
                <Target class="w-4 h-4 text-theme-primary" />
                <span class="font-bold text-theme-text">{{ formatNumber(totalQuestionCount) }}</span>
                <span class="meta-text">题目</span>
              </div>
              <div class="flex items-center gap-1.5">
                <Zap class="w-4 h-4 text-theme-primary" />
                <span class="font-bold text-theme-text">{{ formatNumber(totalSubmissionCount) }}</span>
                <span class="meta-text">提交</span>
              </div>
              <div class="flex items-center gap-1.5">
                <Users class="w-4 h-4 text-theme-primary" />
                <span class="font-bold text-theme-text">{{ categories.length }}</span>
                <span class="meta-text">分类</span>
              </div>
              <div class="flex items-center gap-1.5">
                <Building2 class="w-4 h-4 text-theme-primary" />
                <span class="font-bold text-theme-text">{{ hotCompanies.length }}</span>
                <span class="meta-text">公司</span>
              </div>
            </div>

            <!-- 快捷入口 -->
            <div class="flex flex-wrap items-center justify-center gap-2">
              <button @click="goMyResume" class="theme-btn theme-btn-primary px-4 py-2 rounded-lg text-sm">
                <FileText class="w-4 h-4" />
                我的简历
              </button>
              <button @click="goResume" class="theme-btn theme-btn-secondary px-4 py-2 rounded-lg text-sm">
                <FileText class="w-4 h-4" />
                简历模板
              </button>
              <button @click="router.push('/learn/questions')" class="theme-btn theme-btn-secondary px-4 py-2 rounded-lg text-sm">
                <BookOpen class="w-4 h-4" />
                浏览题库
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== AI 语音面试官（压缩、主题色统一） ========== -->
    <div class="pb-5 sm:pb-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl border shadow-sm bg-gradient-to-br from-theme-primary to-theme-primary-hover border-theme-border/50">
          <div class="relative px-5 py-5 sm:px-8 sm:py-6 text-white">
            <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
              <div class="flex-1 min-w-0">
                <div class="inline-flex items-center gap-2 rounded-full bg-white/15 backdrop-blur px-3 py-1 caption-text font-semibold mb-3 border border-white/25">
                  <Sparkles class="w-3.5 h-3.5" />
                  V10.1 · AI 语音面试官 · NEW
                </div>
                <h2 class="section-title text-white mb-1">像真人一样，和 AI 对练一场面试</h2>
                <p class="meta-text text-white/85 max-w-2xl mb-4">
                  TTS 读题 · ASR 实时转写 · 智能追问 · 双轨评分 · 5 维雷达报告
                </p>

                <!-- 四大能力：横向紧凑排列 -->
                <div class="flex flex-wrap gap-x-5 gap-y-2 mb-4">
                  <div class="flex items-center gap-1.5">
                    <Mic class="w-3.5 h-3.5 text-white/90" />
                    <span class="meta-text text-white/90">语音问答</span>
                  </div>
                  <div class="flex items-center gap-1.5">
                    <MessageSquare class="w-3.5 h-3.5 text-white/90" />
                    <span class="meta-text text-white/90">智能追问</span>
                  </div>
                  <div class="flex items-center gap-1.5">
                    <BarChart3 class="w-3.5 h-3.5 text-white/90" />
                    <span class="meta-text text-white/90">双轨评分</span>
                  </div>
                  <div class="flex items-center gap-1.5">
                    <Clock class="w-3.5 h-3.5 text-white/90" />
                    <span class="meta-text text-white/90">完整报告</span>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="flex flex-wrap items-center gap-2">
                  <button
                    @click="goVoiceInterview"
                    class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg font-semibold shadow-md transition hover:-translate-y-0.5 hover:shadow-lg bg-white text-theme-primary meta-text"
                  >
                    <PlayCircle class="w-4 h-4" />
                    立即开始面试
                    <span class="inline-flex items-center px-1.5 py-0.5 rounded-full caption-text font-bold text-white bg-theme-primary/80">FREE</span>
                  </button>
                  <button
                    @click="goMyAttempts"
                    class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg font-semibold transition hover:bg-white/15 border border-white/30 backdrop-blur-sm meta-text text-white"
                  >
                    <Trophy class="w-4 h-4" />
                    我的面试记录
                  </button>
                </div>
              </div>

              <!-- 右侧：精简版产品预览 -->
              <div class="hidden lg:block shrink-0">
                <div class="w-52 rounded-2xl border border-white/20 bg-white/10 backdrop-blur-md p-2 shadow-xl">
                  <div class="rounded-xl bg-white text-theme-text overflow-hidden">
                    <div class="flex items-center gap-2 px-3 py-2 border-b border-theme-border">
                      <div class="w-7 h-7 rounded-full flex items-center justify-center bg-theme-primary-soft text-theme-primary">
                        <Mic class="w-3.5 h-3.5" />
                      </div>
                      <div>
                        <div class="card-title text-xs">AI 面试官</div>
                        <div class="caption-text text-theme-text-tertiary">Java 后端 · 专业风格</div>
                      </div>
                    </div>
                    <div class="px-2.5 py-2 space-y-1.5 bg-theme-bg">
                      <div class="max-w-[90%] mr-auto bg-theme-surface border border-theme-border rounded-xl rounded-tl-sm px-2.5 py-1.5 caption-text leading-snug">
                        请先做一个 2 分钟自我介绍。
                      </div>
                      <div class="max-w-[90%] ml-auto bg-theme-primary text-white rounded-xl rounded-tr-sm px-2.5 py-1.5 caption-text leading-snug">
                        您好！我是 xxx，3 年 Java…
                      </div>
                    </div>
                    <div class="px-2.5 py-2 border-t border-theme-border flex items-center gap-1.5">
                      <div class="flex-1 h-6 rounded-full bg-theme-bg border border-theme-border flex items-center px-2 caption-text text-theme-text-tertiary">
                        按住麦克风说话…
                      </div>
                      <div class="w-6 h-6 rounded-full bg-theme-primary flex items-center justify-center shadow">
                        <Mic class="w-3 h-3 text-white" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== STAR 核心功能矩阵 ========== -->
    <div class="pb-5 sm:pb-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="mb-3 sm:mb-4">
          <h2 class="section-title flex items-center gap-2 mb-1">
            <Sparkles class="w-5 h-5 sm:w-6 sm:h-6 text-theme-primary" />
            面试空间 · 核心功能
          </h2>
          <p class="meta-text">从练习到复盘的完整漏斗：刷题 → 语音对练 → 复盘报告</p>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 sm:gap-4">
          <!-- 1. 实时 AI 语音面试 -->
          <button
            @click="goVoiceInterview"
            class="group relative text-left rounded-2xl border p-4 sm:p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-lg overflow-hidden bg-theme-surface border-theme-primary/40"
          >
            <span class="absolute top-3 right-3 inline-flex items-center px-1.5 py-0.5 rounded-full caption-text font-bold text-white bg-theme-primary">NEW</span>
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-theme-primary text-white">
              <Mic class="w-5 h-5" />
            </div>
            <div class="card-title mb-1">实时面试提醒</div>
            <div class="card-summary leading-relaxed mb-3">
              进入语音面试，实时对练 · 智能追问
            </div>
            <div class="inline-flex items-center gap-1 meta-text font-semibold text-theme-primary">
              进入体验 <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
            </div>
          </button>

          <!-- 2. 一键 AI 简历 -->
          <button
            @click="goMyResume"
            class="group text-left rounded-2xl border p-4 sm:p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md bg-theme-surface border-theme-border"
          >
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-theme-primary-soft text-theme-primary">
              <FileText class="w-5 h-5" />
            </div>
            <div class="card-title mb-1">一键 AI 简历</div>
            <div class="card-summary leading-relaxed mb-3">
              结构化编辑器 · AI 评分与改写建议
            </div>
            <div class="inline-flex items-center gap-1 meta-text font-semibold text-theme-primary">
              维护简历 <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
            </div>
          </button>

          <!-- 3. 深度面试复盘 -->
          <button
            @click="goMyAttempts"
            class="group text-left rounded-2xl border p-4 sm:p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md bg-theme-surface border-theme-border"
          >
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-theme-primary-soft text-theme-primary">
              <BarChart3 class="w-5 h-5" />
            </div>
            <div class="card-title mb-1">深度面试复盘</div>
            <div class="card-summary leading-relaxed mb-3">
              历史答题 · 薄弱点 · 错题本闭环
            </div>
            <div class="inline-flex items-center gap-1 meta-text font-semibold text-theme-primary">
              查看记录 <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
            </div>
          </button>

          <!-- 4. 多语言面试支持（V10.3 · 置灰占位） -->
          <button
            disabled
            class="relative text-left rounded-2xl border p-4 sm:p-5 shadow-sm cursor-not-allowed opacity-75 bg-theme-surface border-theme-border"
          >
            <span class="absolute top-3 right-3 inline-flex items-center px-1.5 py-0.5 rounded-full caption-text font-semibold bg-theme-text-secondary/20 text-theme-text-secondary">V10.3</span>
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-theme-bg text-theme-text-secondary">
              <Lightbulb class="w-5 h-5" />
            </div>
            <div class="card-title mb-1">多语言面试支持</div>
            <div class="card-summary leading-relaxed mb-3">
              DashScope CosyVoice 多音色 · 中英混练（敬请期待）
            </div>
            <div class="inline-flex items-center gap-1 meta-text font-semibold text-theme-text-secondary">
              开发中 <Clock class="w-3 h-3" />
            </div>
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="flex-1 py-5 sm:py-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading" class="text-center py-12 rounded-xl shadow-sm bg-theme-surface border border-theme-border">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto border-theme-primary"></div>
          <p class="mt-4 meta-text">加载中...</p>
        </div>

        <div v-else-if="error" class="rounded-xl p-8 max-w-2xl mx-auto text-center bg-theme-surface border border-theme-border">
          <p class="mb-4 text-theme-primary">{{ error }}</p>
          <button @click="loadInterviewHome" class="theme-btn theme-btn-primary px-4 py-2 rounded-lg text-sm">
            重试
          </button>
        </div>

        <template v-else>
          <!-- 分类快捷入口 -->
          <div v-if="categories.length > 0" class="mb-8 sm:mb-10">
            <div class="flex items-center justify-between mb-4">
              <h2 class="section-title flex items-center gap-2">
                <BookOpen class="w-5 h-5 sm:w-6 sm:h-6 text-theme-primary" />
                题目分类
              </h2>
              <span class="meta-text">点击卡片进入分类</span>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4">
              <div
                v-for="cat in categories"
                :key="cat.id"
                @click="router.push(`/learn/questions?categoryId=${cat.id}`)"
                class="rounded-xl p-4 sm:p-5 shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer bg-theme-surface border border-theme-border"
              >
                <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-lg flex items-center justify-center mb-3 text-white bg-gradient-to-br from-theme-primary to-theme-primary-hover">
                  <BookOpen class="w-5 h-5 sm:w-6 sm:h-6" />
                </div>
                <h3 class="card-title mb-1">{{ cat.name }}</h3>
                <p v-if="cat.description" class="card-summary line-clamp-2 mb-2">{{ cat.description }}</p>
                <div class="meta-text font-medium flex items-center text-theme-primary">
                  {{ cat.questionCount || 0 }} 道题目
                  <ArrowRight class="w-3 h-3 ml-1" />
                </div>
              </div>
            </div>
          </div>

          <!-- 热门题目 -->
          <div v-if="hotQuestions.length > 0" class="mb-8 sm:mb-10">
            <div class="flex items-center justify-between mb-4">
              <h2 class="section-title flex items-center gap-2">
                <Trophy class="w-5 h-5 sm:w-6 sm:h-6 text-theme-primary" />
                热门题目
              </h2>
              <button @click="router.push('/learn/questions')" class="meta-text font-medium flex items-center text-theme-primary">
                查看更多 <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="space-y-3">
              <div
                v-for="(q, index) in hotQuestions"
                :key="q.id"
                @click="goQuestion(q.id)"
                class="rounded-xl p-4 sm:p-5 shadow-sm hover:shadow-md transition cursor-pointer bg-theme-surface border border-theme-border"
              >
                <div class="flex items-start justify-between gap-4">
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center mb-2 flex-wrap gap-2">
                      <span class="w-6 h-6 rounded-full flex items-center justify-center font-bold caption-text mr-1 bg-theme-accent text-theme-primary">{{ index + 1 }}</span>
                      <span
                        class="px-2 py-0.5 rounded-full caption-text font-medium"
                        :class="getDifficultyColor(q.difficulty)"
                      >
                        {{ getDifficultyText(q.difficulty) }}
                      </span>
                      <span v-if="q.categoryName" class="px-2 py-0.5 rounded-full caption-text bg-theme-accent text-theme-text-secondary">{{ q.categoryName }}</span>
                      <span
                        v-for="tag in q.tags?.slice(0, 3)"
                        :key="tag"
                        class="px-1.5 py-0.5 rounded caption-text bg-theme-accent text-theme-primary"
                      >
                        #{{ tag }}
                      </span>
                      <span
                        v-for="c in q.companies?.slice(0, 2)"
                        :key="c.id"
                        class="px-1.5 py-0.5 rounded caption-text bg-theme-primary-soft text-theme-primary"
                      >
                        {{ c.name }}
                      </span>
                    </div>
                    <h3 class="card-title mb-1">{{ q.title }}</h3>
                    <p v-if="q.description" class="card-summary line-clamp-2 mb-1">{{ q.description }}</p>
                  </div>
                  <div class="text-right flex-shrink-0 hidden sm:block">
                    <div class="flex items-center justify-end gap-3 meta-text mb-1 text-theme-text-secondary">
                      <span class="flex items-center"><CheckCircle class="w-3 h-3 mr-1 text-theme-success" /> {{ q.acceptanceRate }}%</span>
                      <span class="flex items-center"><Zap class="w-3 h-3 mr-1 text-theme-primary" /> {{ q.submissionCount }}</span>
                      <span class="flex items-center"><Star class="w-3 h-3 mr-1 text-theme-warning" /> {{ q.likeCount }}</span>
                    </div>
                    <span class="meta-text font-medium flex items-center justify-end text-theme-primary">
                      查看题目 <ArrowRight class="w-3 h-3 ml-1" />
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门面经 -->
          <div v-if="hotExperiences.length > 0" class="mb-8 sm:mb-10">
            <div class="flex items-center justify-between mb-4">
              <h2 class="section-title flex items-center gap-2">
                <Briefcase class="w-5 h-5 sm:w-6 sm:h-6 text-theme-primary" />
                热门面经
              </h2>
              <button @click="router.push('/interview/experiences')" class="meta-text font-medium flex items-center text-theme-primary">
                查看更多 <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5">
              <div
                v-for="exp in hotExperiences"
                :key="exp.id"
                @click="goExperience(exp.id)"
                class="rounded-xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer flex flex-col bg-theme-surface border border-theme-border"
              >
                <div v-if="exp.coverImage" class="h-32 sm:h-36 bg-theme-bg">
                  <LazyImage :src="exp.coverImage" :alt="exp.title" class="w-full h-full object-cover" />
                </div>
                <div class="p-4 sm:p-5 flex flex-col flex-1">
                  <div class="flex items-center gap-2 mb-2 flex-wrap">
                    <span class="px-2 py-0.5 rounded-full caption-text font-medium bg-theme-accent text-theme-primary">{{ exp.company }}</span>
                    <span v-if="exp.position" class="px-2 py-0.5 rounded-full caption-text font-medium bg-theme-accent text-theme-primary">{{ exp.position }}</span>
                    <span v-if="exp.year" class="meta-text">{{ exp.year }}年</span>
                  </div>
                  <h3 class="card-title mb-1 line-clamp-2">{{ exp.title }}</h3>
                  <p v-if="exp.summary || exp.content" class="card-summary line-clamp-3 flex-1 mb-3">
                    {{ exp.summary || exp.content }}
                  </p>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center meta-text text-theme-text-secondary">
                      <div class="w-6 h-6 rounded-full overflow-hidden mr-2 bg-theme-bg">
                        <LazyImage :src="expAvatar(exp)" :alt="expName(exp)" class="w-full h-full object-cover" />
                      </div>
                      <span class="font-medium">{{ expName(exp) }}</span>
                    </div>
                    <div class="flex items-center gap-3 meta-text text-theme-text-secondary">
                      <span class="flex items-center"><Star class="w-3 h-3 mr-1 text-theme-warning" />{{ exp.likeCount }}</span>
                      <span class="flex items-center"><TrendingUp class="w-3 h-3 mr-1 text-theme-primary" />{{ exp.viewCount }}</span>
                      <span class="flex items-center"><BookOpen class="w-3 h-3 mr-1 text-theme-info" />{{ exp.commentCount }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 简历模板 -->
          <div v-if="resumeTemplates.length > 0" class="mb-8 sm:mb-10">
            <div class="flex items-center justify-between mb-4">
              <h2 class="section-title flex items-center gap-2">
                <FileText class="w-5 h-5 sm:w-6 sm:h-6 text-theme-primary" />
                简历模板
              </h2>
              <button @click="goResume" class="meta-text font-medium flex items-center text-theme-primary">
                查看更多 <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
              <div
                v-for="t in resumeTemplates.slice(0, 4)"
                :key="t.id"
                @click="goResume()"
                class="rounded-xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer bg-theme-surface border border-theme-border"
              >
                <div class="h-36 sm:h-40 bg-theme-bg">
                  <LazyImage v-if="t.cover" :src="t.cover" :alt="t.title" class="w-full h-full object-cover" />
                  <div v-else class="flex items-center justify-center h-full bg-gradient-to-br from-theme-accent to-theme-primary-soft">
                    <FileText class="w-10 h-10 text-theme-primary" />
                  </div>
                </div>
                <div class="p-4">
                  <h3 class="card-title mb-1 line-clamp-1">{{ t.title }}</h3>
                  <p v-if="t.description" class="card-summary line-clamp-2 mb-2">{{ t.description }}</p>
                  <div class="flex items-center justify-between meta-text text-theme-text-secondary">
                    <span class="flex items-center"><Star class="w-3 h-3 mr-1 text-theme-warning" />{{ t.likeCount }}</span>
                    <span class="flex items-center"><FileText class="w-3 h-3 mr-1 text-theme-primary" />{{ t.downloadCount }} 下载</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 热门公司墙 -->
          <div v-if="hotCompanies.length > 0" class="mb-8 sm:mb-10">
            <div class="flex items-center justify-between mb-4">
              <h2 class="section-title flex items-center gap-2">
                <Building2 class="w-5 h-5 sm:w-6 sm:h-6 text-theme-primary" />
                热门公司
              </h2>
              <span class="meta-text">高频出现公司</span>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3 sm:gap-4">
              <div
                v-for="c in hotCompanies"
                :key="c.id"
                class="rounded-xl p-4 sm:p-5 shadow-sm hover:shadow-md hover:-translate-y-1 transition cursor-pointer text-center bg-theme-surface border border-theme-border"
              >
                <div class="w-12 h-12 sm:w-14 sm:h-14 mx-auto rounded-xl flex items-center justify-center mb-2 sm:mb-3 overflow-hidden bg-theme-bg">
                  <LazyImage v-if="c.logo" :src="c.logo" :alt="c.name" class="w-full h-full object-contain" />
                  <Lightbulb v-else class="w-6 h-6 sm:w-7 sm:h-7 text-theme-text-secondary" />
                </div>
                <h3 class="card-title mb-0.5 line-clamp-1">{{ c.name }}</h3>
                <p class="meta-text">{{ c.questionCount || 0 }} 道题</p>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
