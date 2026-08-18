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
    case 'easy': return 'bg-green-100 text-green-700';
    case 'medium': return 'bg-yellow-100 text-yellow-700';
    case 'hard': return 'bg-red-100 text-red-700';
    default: return 'bg-gray-100 text-gray-700';
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
  description: '墨韵智库面试指南 - 精选算法题库、面试经验分享、简历模板下载',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南' },
]);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <router-link
          to="/interview/resume/edit"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-white transition hover:opacity-90 flex-shrink-0"
          style="background-color: var(--theme-primary);"
          title="维护我的简历（教育、工作、项目、技能等）"
        >
          <FileText class="w-3.5 h-3.5" />
          维护我的简历
        </router-link>
      </div>
    </div>

    <!-- Hero 区 -->
    <div class="py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 20% 30%, rgba(99, 102, 241, 0.4) 0%, transparent 50%), radial-gradient(circle at 80% 70%, rgba(168, 85, 247, 0.4) 0%, transparent 50%), linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);">
      <div class="absolute inset-0 opacity-10 pointer-events-none" aria-hidden="true">
        <svg class="absolute top-6 left-8 w-32 h-32 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0L19.2 12l-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>
        <svg class="absolute bottom-4 right-10 w-40 h-40 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 2c2.21 0 4 1.79 4 4s-1.79 4-4 4-4-1.79-4-4 1.79-4 4-4zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
      </div>
      <div class="relative px-6 py-8 sm:px-10 sm:py-10 text-center">
        <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-6">
          <Briefcase class="w-4 h-4 mr-2" /> 墨韵 · 面试指南
        </div>
        <div class="flex items-center justify-center mb-4">
          <h1 class="text-5xl font-bold tracking-tight">备战面试，直通 Offer</h1>
        </div>
        <p class="text-xl max-w-2xl mx-auto mb-10" style="color: rgba(255,255,255,0.9);">
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
          <button @click="goMyResume" class="px-6 py-3 bg-white text-indigo-700 rounded-lg font-medium hover:bg-blue-50 transition flex items-center">
            <FileText class="w-5 h-5 mr-2" />
            我的简历
          </button>
          <button @click="goResume" class="px-6 py-3 bg-white/10 border border-white/30 text-white rounded-lg font-medium hover:bg-white/20 transition flex items-center backdrop-blur-sm">
            <FileText class="w-5 h-5 mr-2" />
            简历模板
          </button>
          <button @click="router.push('/interview/questions')" class="px-6 py-3 bg-white/10 border border-white/30 text-white rounded-lg font-medium hover:bg-white/20 transition flex items-center backdrop-blur-sm">
            <BookOpen class="w-5 h-5 mr-2" />
            浏览题库
          </button>
        </div>
      </div>
        </div>
      </div>
    </div>

    <!-- ========== AI 语音面试官（V10.1 功能入口横幅） ========== -->
    <div class="pb-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl border shadow-sm"
             style="background: linear-gradient(135deg,#0F766E 0%,#3FA86F 55%,#F0B429 130%); border-color: color-mix(in srgb, var(--theme-primary) 35%, transparent);">
          <!-- 装饰：麦克风音波 -->
          <div class="absolute inset-0 opacity-10 pointer-events-none select-none" aria-hidden="true">
            <svg class="absolute -right-8 -top-8 w-64 h-64 text-white" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 14a3 3 0 0 0 3-3V5a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3zm5-3a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.92V21h2v-3.08A7 7 0 0 0 19 11h-2z"/>
            </svg>
            <svg class="absolute left-10 bottom-4 w-40 h-40 text-yellow-200" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
              <path d="M3 12h2M7 8v8M11 5v14M15 9v6M19 11v2M21 12h-2"/>
            </svg>
          </div>

          <div class="relative px-6 py-8 sm:px-10 sm:py-10 text-white grid md:grid-cols-[1.2fr,1fr] gap-8 items-center">
            <div>
              <div class="inline-flex items-center gap-2 rounded-full bg-white/15 backdrop-blur px-3.5 py-1.5 text-xs font-semibold mb-5 border border-white/25">
                <Sparkles class="w-3.5 h-3.5" />
                V10.1 · AI 语音面试官 · NEW
              </div>
              <h2 class="text-3xl sm:text-4xl font-extrabold tracking-tight mb-3">
                像真人一样，和 AI 对练一场面试
              </h2>
              <p class="text-base sm:text-lg mb-6 max-w-2xl" style="color: rgba(255,255,255,0.92);">
                题目 TTS 朗读、麦克风实时语音识别转写、智能追问、每题规则分+大模型反馈、
                结束自动生成 5 维雷达图报告与逐题复盘，一站式陪你到 Offer。
              </p>

              <!-- 四大能力 -->
              <div class="grid sm:grid-cols-2 gap-3 mb-7">
                <div class="flex items-start gap-2.5 bg-white/10 backdrop-blur rounded-xl p-3 border border-white/15">
                  <div class="shrink-0 w-8 h-8 rounded-lg flex items-center justify-center" style="background: rgba(255,255,255,0.18);">
                    <Mic class="w-4.5 h-4.5" style="width:18px;height:18px;" />
                  </div>
                  <div>
                    <div class="font-semibold mb-0.5">语音问答</div>
                    <div class="text-xs" style="color: rgba(255,255,255,0.85);">TTS 读题 + ASR 实时转写（支持编辑）</div>
                  </div>
                </div>
                <div class="flex items-start gap-2.5 bg-white/10 backdrop-blur rounded-xl p-3 border border-white/15">
                  <div class="shrink-0 w-8 h-8 rounded-lg flex items-center justify-center" style="background: rgba(255,255,255,0.18);">
                    <MessageSquare class="w-4.5 h-4.5" style="width:18px;height:18px;" />
                  </div>
                  <div>
                    <div class="font-semibold mb-0.5">智能追问</div>
                    <div class="text-xs" style="color: rgba(255,255,255,0.85);">根据回答自动跟进，模拟真实场景</div>
                  </div>
                </div>
                <div class="flex items-start gap-2.5 bg-white/10 backdrop-blur rounded-xl p-3 border border-white/15">
                  <div class="shrink-0 w-8 h-8 rounded-lg flex items-center justify-center" style="background: rgba(255,255,255,0.18);">
                    <BarChart3 class="w-4.5 h-4.5" style="width:18px;height:18px;" />
                  </div>
                  <div>
                    <div class="font-semibold mb-0.5">双轨评分</div>
                    <div class="text-xs" style="color: rgba(255,255,255,0.85);">规则引擎先打分 · LLM 深度反馈</div>
                  </div>
                </div>
                <div class="flex items-start gap-2.5 bg-white/10 backdrop-blur rounded-xl p-3 border border-white/15">
                  <div class="shrink-0 w-8 h-8 rounded-lg flex items-center justify-center" style="background: rgba(255,255,255,0.18);">
                    <Clock class="w-4.5 h-4.5" style="width:18px;height:18px;" />
                  </div>
                  <div>
                    <div class="font-semibold mb-0.5">完整报告</div>
                    <div class="text-xs" style="color: rgba(255,255,255,0.85);">5 维雷达图 · 逐题 · 优缺点 · 行动建议</div>
                  </div>
                </div>
              </div>

              <!-- 操作按钮 -->
              <div class="flex flex-wrap items-center gap-3">
                <button
                  @click="goVoiceInterview"
                  class="inline-flex items-center gap-2 px-7 py-3.5 rounded-xl font-semibold text-base shadow-lg transition hover:-translate-y-0.5 hover:shadow-xl"
                  style="background: #fff; color: #0F766E;"
                >
                  <PlayCircle class="w-5 h-5" />
                  立即开始面试
                  <span class="inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-bold text-white" style="background: linear-gradient(90deg,#0F766E,#F0B429);">FREE</span>
                </button>
                <button
                  @click="goMyAttempts"
                  class="inline-flex items-center gap-2 px-6 py-3.5 rounded-xl font-semibold text-base transition hover:bg-white/15 border border-white/30 backdrop-blur-sm"
                >
                  <Trophy class="w-5 h-5" />
                  我的面试记录
                </button>
              </div>
            </div>

            <!-- 右侧：产品形态预览（模拟手机界面） -->
            <div class="hidden md:block">
              <div class="mx-auto max-w-sm rounded-[28px] border border-white/20 bg-white/10 backdrop-blur-md p-3 shadow-2xl">
                <div class="rounded-[22px] bg-white text-slate-700 overflow-hidden">
                  <!-- 模拟面试页顶部 -->
                  <div class="flex items-center justify-between px-4 py-2.5 border-b border-slate-100">
                    <div class="flex items-center gap-2">
                      <div class="w-8 h-8 rounded-full flex items-center justify-center bg-emerald-50 text-emerald-600">
                        <Mic class="w-4 h-4" />
                      </div>
                      <div>
                        <div class="text-sm font-bold">AI 面试官</div>
                        <div class="text-[11px] text-slate-400">Java 后端 · 专业风格</div>
                      </div>
                    </div>
                    <span class="text-[10px] rounded-full bg-red-50 text-red-500 px-2 py-0.5 font-semibold border border-red-100">录音中</span>
                  </div>
                  <!-- 模拟气泡 -->
                  <div class="px-3.5 py-3 space-y-2 bg-slate-50">
                    <div class="max-w-[85%] mr-auto bg-white border border-slate-100 rounded-2xl rounded-tl-sm px-3 py-2 text-xs shadow-sm">
                      你好，欢迎参加本次面试～ 请先做一个 2 分钟的自我介绍。
                    </div>
                    <div class="max-w-[85%] ml-auto bg-emerald-600 text-white rounded-2xl rounded-tr-sm px-3 py-2 text-xs shadow-sm">
                      您好！我是 xxx，3 年 Java 后端开发经验，主要做微服务架构…
                    </div>
                    <div class="max-w-[85%] mr-auto bg-white border border-slate-100 rounded-2xl rounded-tl-sm px-3 py-2 text-xs shadow-sm">
                      <span class="font-semibold text-emerald-600 mr-1">追问：</span>
                      你提到 Spring Cloud Gateway，请说说它的过滤器执行顺序和常用扩展点。
                    </div>
                  </div>
                  <!-- 模拟底部输入条 -->
                  <div class="px-3.5 py-3 border-t border-slate-100">
                    <div class="flex items-center gap-2">
                      <div class="flex-1 h-9 rounded-full bg-slate-50 border border-slate-100 flex items-center px-3 text-[11px] text-slate-400">
                        按住麦克风说话，或在此输入文字…
                      </div>
                      <div class="w-10 h-10 rounded-full bg-emerald-600 flex items-center justify-center shadow-lg">
                        <Mic class="w-5 h-5 text-white" />
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

    <!-- ========== STAR 核心功能矩阵（需求 §界面参考补充-13，V10.1 入口就位） ========== -->
    <div class="pb-6">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="mb-4 flex items-end justify-between gap-4">
          <div>
            <h2 class="text-xl sm:text-2xl font-bold flex items-center gap-2" style="color: var(--theme-text);">
              <Sparkles class="w-5 h-5 sm:w-6 sm:h-6" style="color: var(--theme-primary);" />
              面试空间 · 核心功能
            </h2>
            <p class="mt-1 text-xs sm:text-sm" style="color: var(--theme-text-secondary);">从练习到复盘的完整漏斗：刷题 → 文本快练 → 语音对练 → 复盘报告</p>
          </div>
        </div>
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4">
          <!-- 1. 实时 AI 语音面试（对应文档"实时面试提醒"，V10.1 已交付 MVP） -->
          <button
            @click="goVoiceInterview"
            class="group relative text-left rounded-2xl border p-4 sm:p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-lg overflow-hidden"
            :style="{
              borderColor: 'color-mix(in srgb, var(--theme-primary) 40%, transparent)',
              background: 'linear-gradient(160deg, color-mix(in srgb, var(--theme-primary) 10%, var(--theme-surface)) 0%, var(--theme-surface) 100%)',
            }"
          >
            <span class="absolute top-3 right-3 inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-bold text-white" style="background: linear-gradient(90deg,#ef4444,#f97316);">NEW</span>
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center" style="background-color: var(--theme-primary); color:#fff;">
              <Mic class="w-5.5 h-5.5" style="width:22px;height:22px;" />
            </div>
            <div class="font-bold mb-1" style="color: var(--theme-text);">实时面试提醒</div>
            <div class="text-xs leading-relaxed mb-3" style="color: var(--theme-text-secondary);">
              进入语音面试，实时对练 · 智能追问
            </div>
            <div class="inline-flex items-center gap-1 text-xs font-semibold" style="color: var(--theme-primary);">
              进入体验 <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
            </div>
          </button>

          <!-- 2. AI 模拟面试（文本快练版） -->
          <button
            @click="router.push('/interview/mock')"
            class="group text-left rounded-2xl border p-4 sm:p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-indigo-50 text-indigo-600">
              <Target class="w-5 h-5" />
            </div>
            <div class="font-bold mb-1" style="color: var(--theme-text);">AI 模拟面试</div>
            <div class="text-xs leading-relaxed mb-3" style="color: var(--theme-text-secondary);">
              文本快练版 · 5分钟5题 · 规则评分
            </div>
            <div class="inline-flex items-center gap-1 text-xs font-semibold" style="color: var(--theme-primary);">
              开始快练 <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
            </div>
          </button>

          <!-- 3. 一键 AI 简历（复用简历编辑页已有 AI 建议 Tab） -->
          <button
            @click="goMyResume"
            class="group text-left rounded-2xl border p-4 sm:p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-amber-50 text-amber-600">
              <FileText class="w-5 h-5" />
            </div>
            <div class="font-bold mb-1" style="color: var(--theme-text);">一键 AI 简历</div>
            <div class="text-xs leading-relaxed mb-3" style="color: var(--theme-text-secondary);">
              结构化编辑器 · AI 评分与改写建议
            </div>
            <div class="inline-flex items-center gap-1 text-xs font-semibold" style="color: var(--theme-primary);">
              维护简历 <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
            </div>
          </button>

          <!-- 4. 深度面试复盘（历史答题列表） -->
          <button
            @click="goMyAttempts"
            class="group text-left rounded-2xl border p-4 sm:p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-emerald-50 text-emerald-600">
              <BarChart3 class="w-5 h-5" />
            </div>
            <div class="font-bold mb-1" style="color: var(--theme-text);">深度面试复盘</div>
            <div class="text-xs leading-relaxed mb-3" style="color: var(--theme-text-secondary);">
              历史答题 · 薄弱点 · 错题本闭环
            </div>
            <div class="inline-flex items-center gap-1 text-xs font-semibold" style="color: var(--theme-primary);">
              查看记录 <ArrowRight class="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
            </div>
          </button>

          <!-- 5. 多语言面试支持（V10.3 · 置灰，框架位占位） -->
          <button
            disabled
            class="relative text-left rounded-2xl border p-4 sm:p-5 shadow-sm cursor-not-allowed opacity-75"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <span class="absolute top-3 right-3 inline-flex items-center px-1.5 py-0.5 rounded-full text-[10px] font-semibold" style="background-color: color-mix(in srgb, var(--theme-text-secondary) 20%, transparent); color: var(--theme-text-secondary);">V10.3</span>
            <div class="w-11 h-11 rounded-xl mb-3 flex items-center justify-center bg-slate-50 text-slate-500">
              <Lightbulb class="w-5 h-5" />
            </div>
            <div class="font-bold mb-1" style="color: var(--theme-text);">多语言面试支持</div>
            <div class="text-xs leading-relaxed mb-3" style="color: var(--theme-text-secondary);">
              DashScope CosyVoice 多音色 · 中英混练（敬请期待）
            </div>
            <div class="inline-flex items-center gap-1 text-xs font-semibold" style="color: var(--theme-text-secondary);">
              开发中 <Clock class="w-3 h-3" />
            </div>
          </button>
        </div>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="flex-1 py-8 -mt-10">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading" class="text-center py-12 rounded-xl shadow-sm" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto" style="border-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <div v-else-if="error" class="rounded-xl p-8 max-w-2xl mx-auto text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <p class="mb-4" style="color: var(--theme-primary);">{{ error }}</p>
          <button @click="loadInterviewHome" class="px-4 py-2 text-white rounded-lg transition text-sm" style="background-color: var(--theme-primary);">
            重试
          </button>
        </div>

        <template v-else>
          <!-- 分类快捷入口 -->
          <div v-if="categories.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <BookOpen class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                题目分类
              </h2>
              <span class="text-sm" style="color: var(--theme-text-secondary);">点击卡片进入分类</span>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
              <div
                v-for="cat in categories"
                :key="cat.id"
                @click="router.push(`/interview/questions?categoryId=${cat.id}`)"
                class="rounded-xl p-6 shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <div class="w-12 h-12 rounded-lg flex items-center justify-center mb-4 text-white" style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 70%, #4338ca));">
                  <BookOpen class="w-6 h-6" />
                </div>
                <h3 class="text-base font-semibold mb-2" style="color: var(--theme-text);">{{ cat.name }}</h3>
                <p v-if="cat.description" class="text-sm mb-3 line-clamp-2" style="color: var(--theme-text-secondary);">{{ cat.description }}</p>
                <div class="text-xs font-medium flex items-center" style="color: var(--theme-primary);">
                  {{ cat.questionCount || 0 }} 道题目
                  <ArrowRight class="w-3 h-3 ml-1" />
                </div>
              </div>
            </div>
          </div>

          <!-- 热门题目 -->
          <div v-if="hotQuestions.length > 0" class="mb-12">
            <div class="flex items-center justify-between mb-6">
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <Trophy class="w-6 h-6 mr-2 text-yellow-500" />
                热门题目
              </h2>
              <button @click="router.push('/interview/questions')" class="text-sm font-medium flex items-center" style="color: var(--theme-primary);">
                查看更多 <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="space-y-3">
              <div
                v-for="(q, index) in hotQuestions"
                :key="q.id"
                @click="goQuestion(q.id)"
                class="rounded-xl p-5 shadow-sm hover:shadow-md transition cursor-pointer"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <div class="flex items-start justify-between">
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center mb-2 flex-wrap gap-2">
                      <span class="w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs mr-1" style="background-color: var(--theme-accent); color: var(--theme-primary);">{{ index + 1 }}</span>
                      <span
                        class="px-2.5 py-1 rounded-full text-xs font-medium"
                        :class="getDifficultyColor(q.difficulty)"
                      >
                        {{ getDifficultyText(q.difficulty) }}
                      </span>
                      <span v-if="q.categoryName" class="px-2.5 py-1 rounded-full text-xs" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">{{ q.categoryName }}</span>
                      <span
                        v-for="tag in q.tags?.slice(0, 3)"
                        :key="tag"
                        class="px-2 py-1 rounded text-xs"
                        style="background-color: var(--theme-accent); color: var(--theme-primary);"
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
                    <h3 class="text-base font-semibold mb-2" style="color: var(--theme-text);">{{ q.title }}</h3>
                    <p v-if="q.description" class="text-sm line-clamp-2 mb-2" style="color: var(--theme-text-secondary);">{{ q.description }}</p>
                  </div>
                  <div class="text-right ml-6 flex-shrink-0">
                    <div class="flex items-center justify-end gap-3 text-xs mb-2" style="color: var(--theme-text-secondary);">
                      <span class="flex items-center"><CheckCircle class="w-3 h-3 mr-1 text-green-500" /> {{ q.acceptanceRate }}%</span>
                      <span class="flex items-center"><Zap class="w-3 h-3 mr-1 text-yellow-500" /> {{ q.submissionCount }}</span>
                      <span class="flex items-center"><Star class="w-3 h-3 mr-1 text-orange-500" /> {{ q.likeCount }}</span>
                    </div>
                    <span class="text-xs font-medium flex items-center justify-end" style="color: var(--theme-primary);">
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
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <Briefcase class="w-6 h-6 mr-2 text-orange-500" />
                热门面经
              </h2>
              <button @click="router.push('/interview/experiences')" class="text-sm font-medium flex items-center" style="color: var(--theme-primary);">
                查看更多 <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              <div
                v-for="exp in hotExperiences"
                :key="exp.id"
                @click="goExperience(exp.id)"
                class="rounded-xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer flex flex-col"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <div v-if="exp.coverImage" class="h-36" style="background-color: var(--theme-bg);">
                  <LazyImage :src="exp.coverImage" :alt="exp.title" class="w-full h-full object-cover" />
                </div>
                <div class="p-5 flex flex-col flex-1">
                  <div class="flex items-center gap-2 mb-3 flex-wrap">
                    <span class="px-2.5 py-1 rounded-full text-xs font-medium" style="background-color: var(--theme-accent); color: var(--theme-primary);">{{ exp.company }}</span>
                    <span v-if="exp.position" class="px-2.5 py-1 rounded-full text-xs font-medium" style="background-color: var(--theme-accent); color: var(--theme-primary);">{{ exp.position }}</span>
                    <span v-if="exp.year" class="text-xs" style="color: var(--theme-text-secondary);">{{ exp.year }}年</span>
                  </div>
                  <h3 class="text-lg font-semibold mb-2 line-clamp-2" style="color: var(--theme-text);">{{ exp.title }}</h3>
                  <p v-if="exp.summary || exp.content" class="text-sm mb-4 line-clamp-3 flex-1" style="color: var(--theme-text-secondary);">
                    {{ exp.summary || exp.content }}
                  </p>
                  <div class="flex items-center justify-between">
                    <div class="flex items-center text-sm" style="color: var(--theme-text-secondary);">
                      <div class="w-7 h-7 rounded-full overflow-hidden mr-2" style="background-color: var(--theme-bg);">
                        <LazyImage :src="expAvatar(exp)" :alt="expName(exp)" class="w-full h-full object-cover" />
                      </div>
                      <span class="font-medium text-xs">{{ expName(exp) }}</span>
                    </div>
                    <div class="flex items-center gap-3 text-xs" style="color: var(--theme-text-secondary);">
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
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <FileText class="w-6 h-6 mr-2 text-purple-500" />
                简历模板
              </h2>
              <button @click="goResume" class="text-sm font-medium flex items-center" style="color: var(--theme-primary);">
                查看更多 <ArrowRight class="w-4 h-4 ml-1" />
              </button>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
              <div
                v-for="t in resumeTemplates.slice(0, 4)"
                :key="t.id"
                @click="goResume()"
                class="rounded-xl overflow-hidden shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <div class="h-40" style="background-color: var(--theme-bg);">
                  <LazyImage v-if="t.cover" :src="t.cover" :alt="t.title" class="w-full h-full object-cover" />
                  <div v-else class="flex items-center justify-center h-full" style="background: linear-gradient(135deg, var(--theme-accent), color-mix(in srgb, var(--theme-accent) 50%, #c4b5fd));">
                    <FileText class="w-10 h-10" style="color: var(--theme-primary);" />
                  </div>
                </div>
                <div class="p-4">
                  <h3 class="text-base font-semibold mb-1 line-clamp-1" style="color: var(--theme-text);">{{ t.title }}</h3>
                  <p v-if="t.description" class="text-sm mb-3 line-clamp-2" style="color: var(--theme-text-secondary);">{{ t.description }}</p>
                  <div class="flex items-center justify-between text-xs" style="color: var(--theme-text-secondary);">
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
              <h2 class="text-2xl font-bold flex items-center" style="color: var(--theme-text);">
                <Building2 class="w-6 h-6 mr-2" style="color: var(--theme-primary);" />
                热门公司
              </h2>
              <span class="text-sm" style="color: var(--theme-text-secondary);">高频出现公司</span>
            </div>
            <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
              <div
                v-for="c in hotCompanies"
                :key="c.id"
                class="rounded-xl p-5 shadow-sm hover:shadow-md hover:-translate-y-1 transition cursor-pointer text-center"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <div class="w-14 h-14 mx-auto rounded-xl flex items-center justify-center mb-3 overflow-hidden" style="background-color: var(--theme-bg);">
                  <LazyImage v-if="c.logo" :src="c.logo" :alt="c.name" class="w-full h-full object-contain" />
                  <Lightbulb v-else class="w-7 h-7" style="color: var(--theme-text-secondary);" />
                </div>
                <h3 class="text-sm font-semibold mb-1 line-clamp-1" style="color: var(--theme-text);">{{ c.name }}</h3>
                <p class="text-xs" style="color: var(--theme-text-secondary);">{{ c.questionCount || 0 }} 道题</p>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
