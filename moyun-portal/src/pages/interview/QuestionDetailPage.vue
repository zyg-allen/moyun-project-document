<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ChevronDown, ChevronUp, ThumbsUp, Bookmark,
  Code2, MessageSquare, CheckCircle, XCircle, Clock, Zap, Lightbulb, BookOpen, Star, Award,
  Layers, Cpu, GitBranch, FolderKanban, Users, Target, ListChecks, FileText, Link2,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import CodeEditor from '@/components/CodeEditor.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import JudgeResultPanel from '@/components/JudgeResultPanel.vue';
import { generateSeo } from '@/utils/seo';
import {
  getQuestionDetail, submitAnswer, toggleQuestionLike, toggleQuestionBookmark,
  getFeaturedNotes,
} from '@/api/interview';
import { submitJudge, getSampleTestCases } from '@/api/judge';
import type {
  InterviewQuestionDetailVO, InterviewSubmissionVO,
  JudgeResultVO, TestCaseVO,
} from '@/types/api';
import { getSafeAvatar } from '@/utils/avatar';
import { useToast } from '@/composables/useToast';
import { useDictData, dictBadgeClass } from '@/composables/useDictData';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const questionId = computed(() => route.params.id as string);

const loading = ref(false);
const submitting = ref(false);
const question = ref<InterviewQuestionDetailVO | null>(null);
const submissions = ref<InterviewSubmissionVO[]>([]);
const featuredNotes = ref<InterviewSubmissionVO[]>([]);

const showHint = ref(false);
const showSolution = ref(false);
const showAnswerOutline = ref(false);
const showReferenceAnswer = ref(false);

const answerType = ref<'code' | 'text'>('code');
const language = ref('javascript');
const codeContent = ref('');
const textContent = ref('');

// ========== OJ 判题（v6.3） ==========
const judging = ref(false);
const judgeResult = ref<JudgeResultVO | null>(null);
const sampleCases = ref<TestCaseVO[]>([]);

/** 是否为算法题（走 OJ 判题流程，其它题型走文本提交） */
const isAlgorithmQuestion = computed(() => {
  if (!question.value) return false;
  // 已结构化的题目按 questionType 判断；未结构化的旧题，若用户切换到 code 模式也走判题
  if (question.value.questionType) {
    return question.value.questionType === 'algorithm';
  }
  return answerType.value === 'code';
});

// ========== 难度/题型展示映射（字典驱动，本地默认兜底；与题库列表保持一致） ==========
const dictMap = useDictData(['portal_question_difficulty', 'portal_question_type']);

const DEFAULT_DIFFICULTY_MAP: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

const DEFAULT_QUESTION_TYPE_MAP: Record<string, { label: string; class: string; icon: any }> = {
  algorithm: { label: '算法', class: 'bg-blue-50 text-blue-600 border border-blue-200', icon: Cpu },
  bagwen: { label: '八股', class: 'bg-purple-50 text-purple-600 border border-purple-200', icon: BookOpen },
  system_design: { label: '系统设计', class: 'bg-indigo-50 text-indigo-600 border border-indigo-200', icon: GitBranch },
  project: { label: '项目', class: 'bg-emerald-50 text-emerald-600 border border-emerald-200', icon: FolderKanban },
  hr: { label: 'HR', class: 'bg-amber-50 text-amber-600 border border-amber-200', icon: Users },
};

/** 难度徽章映射（label 优先取字典；颜色优先取字典 listClass 映射） */
const difficultyMap = computed<Record<string, { label: string; class: string }>>(() => {
  const map: Record<string, { label: string; class: string }> = { ...DEFAULT_DIFFICULTY_MAP };
  (dictMap['portal_question_difficulty'] || []).forEach(i => {
    map[i.dictValue] = {
      label: i.dictLabel,
      class: dictBadgeClass(i.listClass) || map[i.dictValue]?.class || 'bg-gray-100 text-gray-700',
    };
  });
  return map;
});

/** 题型展示映射（label 优先取字典，颜色/图标沿用本地） */
const questionTypeMap = computed<Record<string, { label: string; class: string; icon: any }>>(() => {
  const map: Record<string, { label: string; class: string; icon: any }> = { ...DEFAULT_QUESTION_TYPE_MAP };
  (dictMap['portal_question_type'] || []).forEach(i => {
    map[i.dictValue] = {
      label: i.dictLabel,
      class: map[i.dictValue]?.class || 'bg-gray-100 text-gray-700',
      icon: map[i.dictValue]?.icon || Layers,
    };
  });
  return map;
});

/** 是否展示结构化字段区（任一结构化字段非空即展示） */
const hasStructuredFields = computed(() => {
  if (!question.value) return false;
  const q = question.value;
  return !!(q.questionType || (q.examinePoints && q.examinePoints.length) || q.answerOutline
    || (q.scoringCriteria && q.scoringCriteria.length) || q.referenceAnswer
    || (q.prerequisiteIds && q.prerequisiteIds.length));
});

onMounted(() => {
  loadQuestionDetail();
});

async function loadQuestionDetail() {
  try {
    loading.value = true;
    const res = await getQuestionDetail(questionId.value);
    if (res.code === 200 && res.data) {
      question.value = res.data;
      submissions.value = res.data.mySubmissions?.slice(0, 10) || [];
      // v6.3 题目结构化：按题型自动切换作答模式（algorithm → 代码，其余 → 文本）
      if (res.data.questionType && res.data.questionType !== 'algorithm') {
        answerType.value = 'text';
      } else {
        answerType.value = 'code';
      }
      // v6.3 OJ 判题：算法题加载样例用例展示
      if (isAlgorithmQuestion.value || !res.data.questionType) {
        loadSampleCases();
      }
    } else {
      toast.error(res.message || '加载题目失败');
    }
    // 并行加载精选笔记
    loadFeaturedNotes();
  } catch (err: any) {
    console.error('加载题目详情失败:', err);
    toast.error(err?.message || '加载题目详情失败，请稍后重试');
  } finally {
    loading.value = false;
  }
}

/**
 * 加载题目样例用例（OJ 判题 v6.3）
 */
async function loadSampleCases() {
  if (!question.value) return;
  try {
    const res = await getSampleTestCases(question.value.id);
    if (res.code === 200 && res.data) {
      sampleCases.value = res.data;
    }
  } catch (err) {
    // 加载失败静默处理，不影响主流程
    console.warn('加载样例用例失败:', err);
  }
}

async function loadFeaturedNotes() {
  try {
    const res = await getFeaturedNotes(questionId.value);
    if (res.code === 200 && res.data) {
      featuredNotes.value = res.data;
    }
  } catch (err) {
    // 精选笔记加载失败不影响主流程
    console.warn('加载精选笔记失败:', err);
  }
}

async function handleSubmit() {
  if (!question.value) return;

  // 算法题（代码模式）走 OJ 判题流程；其它题型走原文本提交
  if (isAlgorithmQuestion.value && answerType.value === 'code') {
    return runJudge();
  }

  const body: any = { answerType: answerType.value };
  if (answerType.value === 'code') {
    if (!codeContent.value.trim()) {
      toast.error('请输入代码内容');
      return;
    }
    body.code = codeContent.value;
    body.language = language.value;
  } else {
    if (!textContent.value.trim()) {
      toast.error('请输入答案内容');
      return;
    }
    body.content = textContent.value;
    body.language = language.value;
  }
  try {
    submitting.value = true;
    const res = await submitAnswer(question.value.id, body);
    if (res.code === 200 && res.data) {
      submissions.value = [res.data, ...submissions.value].slice(0, 10);
      toast.success('提交成功！');
    } else {
      toast.error(res.message || '提交失败');
    }
  } catch (err: any) {
    console.error('提交答案失败:', err);
    toast.error(err?.message || '提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

/**
 * OJ 判题流程（v6.3）：调用判题引擎运行代码并展示结果
 */
async function runJudge() {
  if (!question.value) return;
  if (!codeContent.value.trim()) {
    toast.error('请输入代码内容');
    return;
  }
  try {
    judging.value = true;
    judgeResult.value = null;
    const res = await submitJudge({
      questionId: question.value.id,
      code: codeContent.value,
      language: language.value,
    });
    if (res.code === 200 && res.data) {
      judgeResult.value = res.data;
      if (res.data.accepted) {
        toast.success(`通过全部 ${res.data.totalCount} 个用例！`);
      } else {
        toast.warning(`通过 ${res.data.passedCount}/${res.data.totalCount} 用例，${res.data.statusName || '未通过'}`);
      }
    } else {
      toast.error(res.message || '判题失败');
    }
  } catch (err: any) {
    console.error('判题失败:', err);
    toast.error(err?.message || '判题失败，请稍后重试');
  } finally {
    judging.value = false;
  }
}

async function handleLike() {
  if (!question.value) return;
  try {
    const res = await toggleQuestionLike(question.value.id);
    if (res.code === 200 && res.data) {
      question.value.liked = res.data.liked;
      question.value.likeCount = res.data.likeCount;
      toast.success(res.data.liked ? '点赞成功' : '已取消点赞');
    } else {
      toast.error(res.message || '操作失败');
    }
  } catch (err: any) {
    toast.error(err?.message || '操作失败');
  }
}

async function handleBookmark() {
  if (!question.value) return;
  try {
    const res = await toggleQuestionBookmark(question.value.id);
    if (res.code === 200 && res.data) {
      question.value.bookmarked = res.data.bookmarked;
      toast.success(res.data.bookmarked ? '收藏成功' : '已取消收藏');
    } else {
      toast.error(res.message || '操作失败');
    }
  } catch (err: any) {
    toast.error(err?.message || '操作失败');
  }
}

useHead(
  computed(() => {
    return generateSeo({
      title: question.value?.title || '题目详情',
      description: question.value?.description?.slice(0, 120) || '面试题目',
    });
  })
);

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '面试题库', path: '/interview/questions' },
  { label: '题目详情' },
]);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- Loading -->
        <div v-if="loading && !question" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto" style="border-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <template v-else-if="question">
          <!-- 题目标题区 -->
          <div class="rounded-xl shadow-sm p-6 mb-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-start justify-between mb-4">
              <div class="flex-1">
                <div class="flex items-center flex-wrap gap-2 mb-3">
                  <span
                    v-if="question.questionType && questionTypeMap[question.questionType]"
                    class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                    :class="questionTypeMap[question.questionType].class"
                  >
                    <component :is="questionTypeMap[question.questionType].icon" class="w-3 h-3 inline mr-1" />
                    {{ questionTypeMap[question.questionType].label }}
                  </span>
                  <span
                    class="px-3 py-1 rounded-full text-xs font-medium"
                    :class="difficultyMap[question.difficulty]?.class || 'bg-[var(--theme-bg)] text-[var(--theme-text-secondary)]'"
                  >
                    {{ difficultyMap[question.difficulty]?.label || question.difficulty }}
                  </span>
                  <span v-if="question.categoryName" class="px-3 py-1 rounded-full text-xs font-medium" style="background-color: var(--theme-accent); color: var(--theme-primary);">
                    <BookOpen class="w-3 h-3 inline mr-1" />
                    {{ question.categoryName }}
                  </span>
                  <span
                    v-for="tag in question.tags"
                    :key="tag"
                    class="px-2 py-1 rounded text-xs"
                    style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                  >
                    #{{ tag }}
                  </span>
                </div>
                <h1 class="text-2xl font-bold mb-2" style="color: var(--theme-text);">{{ question.title }}</h1>
                <div class="flex flex-wrap items-center gap-4 text-sm" style="color: var(--theme-text-secondary);">
                  <span class="flex items-center">
                    <Zap class="w-4 h-4 mr-1 text-yellow-500" />
                    通过率 {{ question.acceptanceRate }}%
                  </span>
                  <span class="flex items-center">
                    <MessageSquare class="w-4 h-4 mr-1" />
                    {{ question.submissionCount }} 次提交
                  </span>
                  <span class="flex items-center">
                    <ThumbsUp class="w-4 h-4 mr-1" />
                    {{ question.likeCount }} 点赞
                  </span>
                </div>
              </div>

              <!-- 操作区 -->
              <div class="flex items-center gap-2 ml-4">
                <button
                  @click="handleLike"
                  class="flex items-center px-4 py-2 rounded-lg text-sm transition"
                  :class="question.liked ? 'bg-[var(--theme-accent)] text-[var(--theme-primary)] border border-[var(--theme-border)]' : 'bg-[var(--theme-bg)] text-[var(--theme-text-secondary)] border border-[var(--theme-border)] hover:border-[var(--theme-primary)]'"
                >
                  <ThumbsUp class="w-4 h-4 mr-1" />
                  {{ question.liked ? '已点赞' : '点赞' }}
                </button>
                <button
                  @click="handleBookmark"
                  class="flex items-center px-4 py-2 rounded-lg text-sm transition"
                  :class="question.bookmarked ? 'bg-[var(--theme-accent)] text-[var(--theme-primary)] border border-[var(--theme-border)]' : 'bg-[var(--theme-bg)] text-[var(--theme-text-secondary)] border border-[var(--theme-border)] hover:border-[var(--theme-primary)]'"
                >
                  <Bookmark class="w-4 h-4 mr-1" />
                  {{ question.bookmarked ? '已收藏' : '收藏' }}
                </button>
              </div>
            </div>

            <!-- 公司标签 -->
            <div v-if="question.companies && question.companies.length > 0" class="flex items-center flex-wrap gap-2">
              <span class="text-sm mr-2" style="color: var(--theme-text-secondary);">出现公司：</span>
              <span
                v-for="c in question.companies"
                :key="c.id"
                class="px-3 py-1 rounded-full text-xs font-medium"
                style="background-color: var(--theme-accent); color: var(--theme-primary);"
              >
                {{ c.name }}
              </span>
            </div>
          </div>

          <!-- 题目描述 -->
          <div class="rounded-xl shadow-sm p-6 mb-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="text-lg font-semibold mb-3" style="color: var(--theme-text);">题目描述</h2>
            <div class="leading-relaxed whitespace-pre-wrap text-sm" style="color: var(--theme-text-secondary);">
              {{ question.description || '暂无题目描述' }}
            </div>
          </div>

          <!-- 结构化字段：考察点 + 前置题目（v6.3 题目结构化） -->
          <div
            v-if="hasStructuredFields && ((question.examinePoints && question.examinePoints.length) || (question.prerequisiteIds && question.prerequisiteIds.length))"
            class="rounded-xl shadow-sm p-6 mb-6"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <!-- 考察点 -->
            <div v-if="question.examinePoints && question.examinePoints.length" class="mb-4 last:mb-0">
              <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                <Target class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                考察点
              </h3>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="(point, idx) in question.examinePoints"
                  :key="idx"
                  class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                  style="background-color: var(--theme-accent); color: var(--theme-primary);"
                >
                  {{ point }}
                </span>
              </div>
            </div>

            <!-- 前置题目（学习路径） -->
            <div v-if="question.prerequisiteIds && question.prerequisiteIds.length">
              <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                <Link2 class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                前置题目
                <span class="text-xs font-normal ml-2" style="color: var(--theme-text-secondary);">建议先完成以下题目再挑战本题</span>
              </h3>
              <div class="flex flex-wrap gap-2">
                <router-link
                  v-for="pid in question.prerequisiteIds"
                  :key="pid"
                  :to="`/interview/question/${pid}`"
                  class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium transition hover:opacity-80"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
                >
                  <FileText class="w-3 h-3 mr-1" />
                  题目 #{{ pid }}
                </router-link>
              </div>
            </div>
          </div>

          <!-- Hint -->
          <div v-if="question.hint" class="rounded-xl shadow-sm mb-6 overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <button
              @click="showHint = !showHint"
              class="w-full px-6 py-4 flex items-center justify-between hover:bg-[var(--theme-accent)] transition"
            >
              <div class="flex items-center" style="color: var(--theme-text);">
                <Lightbulb class="w-5 h-5 mr-2 text-yellow-500" />
                <span class="font-medium">提示 (Hint)</span>
              </div>
              <ChevronDown v-if="!showHint" class="w-5 h-5" style="color: var(--theme-text-secondary);" />
              <ChevronUp v-else class="w-5 h-5" style="color: var(--theme-text-secondary);" />
            </button>
            <div v-if="showHint" class="px-6 pb-6 border-t border-[var(--theme-border)] pt-4">
              <div class="text-sm leading-relaxed whitespace-pre-wrap bg-yellow-50 p-4 rounded-lg" style="color: var(--theme-text-secondary);">
                {{ question.hint }}
              </div>
            </div>
          </div>

          <!-- Solution -->
          <div v-if="question.solution" class="rounded-xl shadow-sm mb-6 overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <button
              @click="showSolution = !showSolution"
              class="w-full px-6 py-4 flex items-center justify-between hover:bg-[var(--theme-accent)] transition"
            >
              <div class="flex items-center" style="color: var(--theme-text);">
                <CheckCircle class="w-5 h-5 mr-2 text-green-500" />
                <span class="font-medium">参考代码 (Solution)</span>
              </div>
              <ChevronDown v-if="!showSolution" class="w-5 h-5" style="color: var(--theme-text-secondary);" />
              <ChevronUp v-else class="w-5 h-5" style="color: var(--theme-text-secondary);" />
            </button>
            <div v-if="showSolution" class="px-6 pb-6 border-t border-[var(--theme-border)] pt-4">
              <pre class="bg-gray-900 text-gray-100 rounded-lg p-4 text-xs overflow-x-auto"><code>{{ question.solution }}</code></pre>
            </div>
          </div>

          <!-- 答题大纲（v6.3 题目结构化） -->
          <div v-if="question.answerOutline" class="rounded-xl shadow-sm mb-6 overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <button
              @click="showAnswerOutline = !showAnswerOutline"
              class="w-full px-6 py-4 flex items-center justify-between hover:bg-[var(--theme-accent)] transition"
            >
              <div class="flex items-center" style="color: var(--theme-text);">
                <ListChecks class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
                <span class="font-medium">答题大纲</span>
              </div>
              <ChevronDown v-if="!showAnswerOutline" class="w-5 h-5" style="color: var(--theme-text-secondary);" />
              <ChevronUp v-else class="w-5 h-5" style="color: var(--theme-text-secondary);" />
            </button>
            <div v-if="showAnswerOutline" class="px-6 pb-6 border-t border-[var(--theme-border)] pt-4">
              <MarkdownRenderer editor-mode="markdown" :content-markdown="question.answerOutline" prose-width="normal" />
            </div>
          </div>

          <!-- 评分标准（v6.3 题目结构化） -->
          <div v-if="question.scoringCriteria && question.scoringCriteria.length" class="rounded-xl shadow-sm p-6 mb-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <Layers class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
              评分标准
            </h2>
            <div class="space-y-3">
              <div
                v-for="(crit, idx) in question.scoringCriteria"
                :key="idx"
                class="border rounded-lg p-4"
                style="border-color: var(--theme-border); background-color: var(--theme-bg);"
              >
                <div class="flex items-center justify-between mb-2">
                  <span class="text-sm font-medium" style="color: var(--theme-text);">{{ crit.dimension }}</span>
                  <span
                    v-if="crit.weight != null"
                    class="px-2 py-0.5 rounded text-xs font-medium"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                  >
                    权重 {{ crit.weight }}%
                  </span>
                </div>
                <p v-if="crit.description" class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
                  {{ crit.description }}
                </p>
              </div>
            </div>
          </div>

          <!-- 官方参考答案（v6.3 题目结构化） -->
          <div v-if="question.referenceAnswer" class="rounded-xl shadow-sm mb-6 overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <button
              @click="showReferenceAnswer = !showReferenceAnswer"
              class="w-full px-6 py-4 flex items-center justify-between hover:bg-[var(--theme-accent)] transition"
            >
              <div class="flex items-center" style="color: var(--theme-text);">
                <BookOpen class="w-5 h-5 mr-2 text-green-500" />
                <span class="font-medium">官方参考答案</span>
              </div>
              <ChevronDown v-if="!showReferenceAnswer" class="w-5 h-5" style="color: var(--theme-text-secondary);" />
              <ChevronUp v-else class="w-5 h-5" style="color: var(--theme-text-secondary);" />
            </button>
            <div v-if="showReferenceAnswer" class="px-6 pb-6 border-t border-[var(--theme-border)] pt-4">
              <MarkdownRenderer editor-mode="markdown" :content-markdown="question.referenceAnswer" prose-width="normal" />
            </div>
          </div>

          <!-- 练习区 -->
          <div class="rounded-xl shadow-sm p-6 mb-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-semibold flex items-center" style="color: var(--theme-text);">
                <Code2 class="w-5 h-5 mr-2 text-blue-500" />
                编写你的答案
              </h2>
              <div class="flex items-center gap-3 text-sm">
                <div class="flex items-center rounded-lg overflow-hidden" style="background-color: var(--theme-bg);">
                  <button
                    @click="answerType = 'code'"
                    class="px-3 py-1.5 transition"
                    :class="answerType === 'code' ? 'bg-[var(--theme-primary)] text-white' : 'text-[var(--theme-text-secondary)]'"
                  >
                    代码
                  </button>
                  <button
                    @click="answerType = 'text'"
                    class="px-3 py-1.5 transition"
                    :class="answerType === 'text' ? 'bg-[var(--theme-primary)] text-white' : 'text-[var(--theme-text-secondary)]'"
                  >
                    文本
                  </button>
                </div>
                <select
                  v-if="answerType === 'code'"
                  v-model="language"
                  class="px-3 py-1.5 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                >
                  <option value="javascript">JavaScript</option>
                  <option value="typescript">TypeScript</option>
                  <option value="python">Python</option>
                  <option value="java">Java</option>
                  <option value="go">Go</option>
                  <option value="cpp">C++</option>
                  <option value="rust">Rust</option>
                </select>
              </div>
            </div>

            <CodeEditor
              v-if="answerType === 'code'"
              v-model="codeContent"
              :language="language"
              height="320px"
              :submit-shortcut="true"
              placeholder="在此输入你的代码解... 例如:&#10;function twoSum(nums, target) {&#10;  // TODO: 你的答案&#10;}"
              @submit="handleSubmit"
            />

            <textarea
              v-else
              v-model="textContent"
              class="w-full h-64 p-4 border rounded-lg text-sm input-focus"
              style="background-color: var(--theme-bg); color: var(--theme-text); border-color: var(--theme-border);"
              placeholder="在此输入你的分析或文字答案..."
            ></textarea>

            <div class="flex items-center justify-between mt-4">
              <span class="text-xs" style="color: var(--theme-text-secondary);">
                {{ isAlgorithmQuestion && answerType === 'code' ? '提示：提交后将运行全部测试用例判题' : '提示：你可以多次提交，最近 10 次会在下方展示' }}
              </span>
              <button
                @click="handleSubmit"
                :disabled="submitting || judging"
                class="px-6 py-2 text-white rounded-lg text-sm font-medium transition flex items-center hover:opacity-90 disabled:bg-gray-400"
                style="background-color: var(--theme-primary);"
              >
                <span v-if="submitting || judging" class="w-4 h-4 rounded-full border-2 border-white border-t-transparent animate-spin mr-2"></span>
                {{ (isAlgorithmQuestion && answerType === 'code') ? (judging ? '判题中...' : '提交判题') : (submitting ? '提交中...' : '提交答案') }}
              </button>
            </div>
          </div>

          <!-- OJ 判题结果（v6.3） -->
          <JudgeResultPanel
            v-if="isAlgorithmQuestion || judgeResult || judging"
            :result="judgeResult"
            :loading="judging"
            class="mb-6"
          />

          <!-- 样例用例（v6.3 OJ 判题） -->
          <div
            v-if="sampleCases.length > 0"
            class="rounded-xl shadow-sm p-6 mb-6"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <FileText class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
              样例用例
              <span class="text-xs font-normal ml-2" style="color: var(--theme-text-secondary);">提交后将运行这些样例与隐藏用例判题</span>
            </h2>
            <div class="space-y-3">
              <div
                v-for="(tc, idx) in sampleCases"
                :key="tc.id"
                class="border rounded-lg p-4"
                style="border-color: var(--theme-border); background-color: var(--theme-bg);"
              >
                <div class="text-xs font-semibold mb-2" style="color: var(--theme-text);">
                  用例 {{ idx + 1 }}
                  <span v-if="tc.explanation" class="font-normal ml-2" style="color: var(--theme-text-secondary);">{{ tc.explanation }}</span>
                </div>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <div class="text-xs font-medium mb-1" style="color: var(--theme-text-secondary);">输入</div>
                    <pre class="text-xs p-2 rounded-lg overflow-x-auto" style="background-color: var(--theme-surface); color: var(--theme-text);">{{ tc.input || '(无输入)' }}</pre>
                  </div>
                  <div>
                    <div class="text-xs font-medium mb-1" style="color: var(--theme-text-secondary);">输出</div>
                    <pre class="text-xs p-2 rounded-lg overflow-x-auto" style="background-color: var(--theme-surface); color: var(--theme-text);">{{ tc.expectedOutput || '(无输出)' }}</pre>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 精选笔记 -->
          <div v-if="featuredNotes.length > 0" class="rounded-xl shadow-sm p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <Award class="w-5 h-5 mr-2 text-yellow-500" />
              精选笔记 ({{ featuredNotes.length }})
            </h2>
            <div class="space-y-4">
              <div
                v-for="note in featuredNotes"
                :key="note.id"
                class="border border-[var(--theme-border)] rounded-lg p-4 hover:border-[var(--theme-primary)] transition"
              >
                <div class="flex items-center gap-3 mb-3">
                  <img
                    :src="getSafeAvatar(note.userAvatar, String(note.userId))"
                    :alt="note.userNickname || '用户'"
                    class="w-8 h-8 rounded-full object-cover"
                    @error="(e: Event) => (e.target as HTMLImageElement).src = getSafeAvatar(null, String(note.userId))"
                  />
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2">
                      <span class="text-sm font-medium" style="color: var(--theme-text);">{{ note.userNickname || '匿名用户' }}</span>
                      <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs bg-yellow-100 text-yellow-700">
                        <Star class="w-3 h-3" />
                        精选
                      </span>
                    </div>
                    <p class="text-xs" style="color: var(--theme-text-secondary);">{{ note.featuredTime || note.createTime || '-' }}</p>
                  </div>
                </div>
                <div class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--theme-text-secondary);">{{ note.note }}</div>
              </div>
            </div>
          </div>

          <!-- 提交历史 -->
          <div v-if="submissions.length > 0" class="rounded-xl shadow-sm p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <Clock class="w-5 h-5 mr-2" />
              最近提交 ({{ submissions.length }})
            </h2>
            <div class="overflow-hidden rounded-lg border" style="border-color: var(--theme-border);">
              <table class="w-full text-sm">
                <thead style="background-color: var(--theme-surface); color: var(--theme-text-secondary);">
                  <tr>
                    <th class="px-4 py-2 text-left font-medium">时间</th>
                    <th class="px-4 py-2 text-left font-medium">语言/类型</th>
                    <th class="px-4 py-2 text-left font-medium">状态</th>
                    <th class="px-4 py-2 text-left font-medium">运行</th>
                    <th class="px-4 py-2 text-left font-medium">内存</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="s in submissions"
                    :key="s.id"
                    class="border-t border-[var(--theme-border)] hover:bg-[var(--theme-accent)] transition"
                  >
                    <td class="px-4 py-3" style="color: var(--theme-text-secondary);">{{ s.createTime || '-' }}</td>
                    <td class="px-4 py-3" style="color: var(--theme-text-secondary);">{{ s.language || s.answerType || '-' }}</td>
                    <td class="px-4 py-3">
                      <span
                        v-if="s.isSuccess"
                        class="inline-flex items-center text-green-600"
                      >
                        <CheckCircle class="w-4 h-4 mr-1" />
                        通过
                      </span>
                      <span v-else class="inline-flex items-center text-red-600">
                        <XCircle class="w-4 h-4 mr-1" />
                        {{ s.status || '未通过' }}
                      </span>
                    </td>
                    <td class="px-4 py-3" style="color: var(--theme-text-secondary);">{{ s.runtime ? s.runtime + ' ms' : '-' }}</td>
                    <td class="px-4 py-3" style="color: var(--theme-text-secondary);">{{ s.memoryUsage ? s.memoryUsage + ' KB' : '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>

        <div v-else class="text-center py-12">
          <p style="color: var(--theme-text-secondary);">未找到题目信息</p>
          <button
            @click="router.push('/interview/questions')"
            class="mt-4 px-4 py-2 text-white rounded-lg text-sm hover:opacity-90 transition"
            style="background-color: var(--theme-primary);"
          >
            返回题库
          </button>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
