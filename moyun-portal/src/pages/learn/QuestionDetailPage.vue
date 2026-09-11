<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ChevronDown, ChevronUp, ChevronLeft, ChevronRight, ThumbsUp, Bookmark,
  MessageSquare, Zap, Lightbulb, BookOpen, Star, Award,
  Layers, Cpu, GitBranch, FolderKanban, Users, Target, ListChecks, FileText, Link2,
  CheckCircle, AlertCircle, PlayCircle, ListFilter,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { generateSeo } from '@/utils/seo.ts';
import {
  getQuestionDetail, toggleQuestionLike, toggleQuestionBookmark,
  getFeaturedNotes, recordQuestionRead, getQuestionNeighbor,
} from '@/api/interview.ts';
import type {
  InterviewQuestionDetailVO, InterviewSubmissionVO,
  InterviewQuestionNeighborVO, InterviewQuestionQuery,
} from '@/types/api.ts';
import { getSafeAvatar } from '@/utils/avatar.ts';
import { useToast } from '@/composables/useToast.ts';
import { useDictData, dictBadgeClass } from '@/composables/useDictData.ts';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const questionId = computed(() => route.params.id as string);

const loading = ref(false);
const question = ref<InterviewQuestionDetailVO | null>(null);
const featuredNotes = ref<InterviewSubmissionVO[]>([]);

// 相邻题目导航（与来源列表页筛选同源：sort 升序 + createTime 降序）
const neighbor = ref<InterviewQuestionNeighborVO | null>(null);

const showHint = ref(false);

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

/** 是否有参考答案或解析内容 */
const hasAnswerContent = computed(() => {
  if (!question.value) return false;
  const q = question.value;
  return !!(q.referenceAnswer || q.solution || q.answerOutline
    || (q.scoringCriteria && q.scoringCriteria.length)
    || q.correctAnswer || q.analysis);
});

/** 选择题选项（阅读模式展示；后端已脱敏 is_correct） */
const practiceOptions = computed(() => {
  if (!question.value?.options || question.value.practiceMode !== 'choice') return [];
  try {
    return JSON.parse(question.value.options) as { label: string; text: string }[];
  } catch {
    return [];
  }
});

/** 正确答案选项 label 集合（与后端归一化对齐：支持 "A,B"、"A B" 等分隔写法） */
const correctLabels = computed(() => {
  const answer = question.value?.correctAnswer;
  if (!answer) return new Set<string>();
  return new Set(answer.trim().toUpperCase().split(/[^A-Z]+/).filter(Boolean));
});

/** 练习模式标签与做题入口（choice/coding 才提供"去练习"） */
const PRACTICE_MODE_MAP: Record<string, { label: string; action: string; path: (id: string) => string } | null> = {
  reading: null,
  choice: { label: '选择题', action: '去练习', path: (id) => `/learn/practice/choice/${id}` },
  coding: { label: '编程题', action: '去写代码', path: (id) => `/learn/practice/coding/${id}` },
};
const practiceEntry = computed(() => {
  if (!question.value) return null;
  const mode = question.value.practiceMode || 'reading';
  return PRACTICE_MODE_MAP[mode] || null;
});

function gotoPractice() {
  if (practiceEntry.value && question.value) {
    router.push(practiceEntry.value.path(String(question.value.id)));
  }
}

onMounted(() => {
  loadQuestionDetail();
});

// 上一题/下一题切换：同路由不同参数时组件复用，需监听参数变化重新加载
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    question.value = null;
    featuredNotes.value = [];
    showHint.value = false;
    window.scrollTo({ top: 0 });
    loadQuestionDetail();
  }
});

async function loadQuestionDetail() {
  try {
    loading.value = true;
    const res = await getQuestionDetail(questionId.value);
    if (res.code === 200 && res.data) {
      question.value = res.data;
      // 阅读埋点：停留 ≥60s 上报（同题每日幂等，未登录/失败静默，不打扰阅读体验）
      scheduleReadReport();
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
  loadNeighbor();
}

/**
 * 相邻题目导航：与来源列表页（/learn/questions）同源筛选，
 * categoryId/questionType/difficulty/keyword 由列表页跳转时透传；
 * 无筛选上下文（收藏/时间线等入口进入）时按题库全集顺序导航
 */
async function loadNeighbor() {
  neighbor.value = null;
  try {
    const params: Pick<InterviewQuestionQuery, 'categoryId' | 'questionType' | 'difficulty' | 'keyword'> = {};
    const q = route.query;
    if (typeof q.categoryId === 'string' && q.categoryId) params.categoryId = q.categoryId;
    if (typeof q.questionType === 'string' && q.questionType) params.questionType = q.questionType;
    if (typeof q.difficulty === 'string' && q.difficulty) params.difficulty = q.difficulty;
    if (typeof q.keyword === 'string' && q.keyword) params.keyword = q.keyword;
    const res = await getQuestionNeighbor(questionId.value, params);
    if (res.code === 200 && res.data) {
      neighbor.value = res.data;
    }
  } catch {
    // 导航数据加载失败不影响阅读主流程
  }
}

/** 切题跳转（保留筛选上下文）；阅读模式无作答状态，直接切换 */
function gotoNeighbor(id: string | number | null | undefined, dir: 'prev' | 'next') {
  if (id == null) return;
  const query: Record<string, string> = {};
  const q = route.query;
  if (typeof q.categoryId === 'string' && q.categoryId) query.categoryId = q.categoryId;
  if (typeof q.questionType === 'string' && q.questionType) query.questionType = q.questionType;
  if (typeof q.difficulty === 'string' && q.difficulty) query.difficulty = q.difficulty;
  if (typeof q.keyword === 'string' && q.keyword) query.keyword = q.keyword;
  toast.info(dir === 'prev' ? '已切换到上一题' : '已切换到下一题');
  router.push({ path: `/interview/question/${id}`, query });
  window.scrollTo({ top: 0 });
}

async function reportRead() {
  try {
    const res = await recordQuestionRead(questionId.value);
    if (res.code === 200 && res.data?.readRecorded) {
      toast.success('已记录今日阅读，成长 +1');
    }
  } catch {
    // 未登录或网络异常：阅读埋点静默失败
  }
}

// 阅读埋点：停留 ≥60s 才上报（防刷记录；切题/离开清除计时，同题每日幂等）
const READ_REPORT_DELAY_MS = 60_000;
let readReportTimer: number | null = null;

function scheduleReadReport() {
  clearReadReportTimer();
  readReportTimer = window.setTimeout(() => {
    readReportTimer = null;
    reportRead();
  }, READ_REPORT_DELAY_MS);
}

function clearReadReportTimer() {
  if (readReportTimer != null) {
    window.clearTimeout(readReportTimer);
    readReportTimer = null;
  }
}

onUnmounted(clearReadReportTimer);

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
  { label: '学习中心', path: '/learn' },
  { label: '面试题库', path: '/learn/questions' },
  { label: '题目详情' },
]);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <!-- 顶部紧凑切题（长文阅读时随时可切换） -->
        <div v-if="neighbor" class="hidden sm:flex items-center gap-2 flex-shrink-0">
          <button
            @click="gotoNeighbor(neighbor?.prevId, 'prev')"
            :disabled="!neighbor?.prevId"
            :title="neighbor?.prevTitle ? `上一题：${neighbor.prevTitle}` : '已是第一题'"
            class="inline-flex items-center gap-1 text-xs px-2.5 py-1.5 rounded border transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            style="border-color: var(--theme-border); color: var(--theme-text-secondary);"
          >
            <ChevronLeft class="w-3 h-3" />
            <span>上一题</span>
          </button>
          <span
            v-if="neighbor?.currentIndex"
            class="text-xs font-mono px-2 py-0.5 rounded"
            style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
            title="当前题目在筛选结果中的位置"
          >
            {{ neighbor.currentIndex }}/{{ neighbor.total }}
          </span>
          <button
            @click="gotoNeighbor(neighbor?.nextId, 'next')"
            :disabled="!neighbor?.nextId"
            :title="neighbor?.nextTitle ? `下一题：${neighbor.nextTitle}` : '已是最后一题'"
            class="inline-flex items-center gap-1 text-xs px-2.5 py-1.5 rounded border transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
            :style="neighbor?.nextId
              ? { borderColor: 'var(--theme-primary)', color: 'var(--theme-primary)' }
              : { borderColor: 'var(--theme-border)', color: 'var(--theme-text-secondary)' }"
          >
            <span>下一题</span>
            <ChevronRight class="w-3 h-3" />
          </button>
        </div>
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
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <!-- 左侧：题目内容 + 答案解析 -->
            <div class="lg:col-span-2 space-y-6">
              <!-- 题目标题区 -->
              <div class="rounded-xl shadow-sm p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
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
                      <span
                        v-if="question.categoryName" class="px-3 py-1 rounded-full text-xs font-medium" style="background-color: var(--theme-accent); color: var(--theme-primary);">
                        <BookOpen class="w-3 h-3 inline mr-1" />
                        {{ question.categoryName }}
                      </span>
                      <span
                        v-if="practiceEntry"
                        class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                        style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
                      >
                        <ListFilter class="w-3 h-3 inline mr-1" />
                        {{ practiceEntry.label }}模式
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
                      v-if="practiceEntry"
                      @click="gotoPractice"
                      class="flex items-center px-4 py-2 rounded-lg text-sm text-white transition hover:opacity-90"
                      style="background-color: var(--theme-primary);"
                      :title="`进入${practiceEntry.label}练习，提交作答后计入成长记录`"
                    >
                      <PlayCircle class="w-4 h-4 mr-1" />
                      {{ practiceEntry.action }}
                    </button>
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
              <div class="rounded-xl shadow-sm p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h2 class="text-lg font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                  <FileText class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
                  题目描述
                </h2>
                <div class="leading-relaxed text-sm" style="color: var(--theme-text-secondary);">
                  <MarkdownRenderer v-if="question.description" editor-mode="markdown" :content-markdown="question.description" prose-width="normal" />
                  <p v-else>暂无题目描述</p>
                </div>

                <!-- 选择题选项（题干组成部分，阅读模式展示） -->
                <div v-if="practiceOptions.length" class="mt-5 space-y-2.5">
                  <div
                    v-for="opt in practiceOptions"
                    :key="opt.label"
                    class="flex items-center gap-3 p-3 rounded-xl border"
                    :class="correctLabels.has(opt.label) ? 'border-green-300 bg-green-50' : ''"
                    :style="!correctLabels.has(opt.label)
                      ? { borderColor: 'var(--theme-border)', backgroundColor: 'var(--theme-bg)' }
                      : {}"
                  >
                    <div
                      class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0"
                      :style="correctLabels.has(opt.label)
                        ? { backgroundColor: '#10B981', color: '#fff' }
                        : { backgroundColor: 'var(--theme-surface)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
                    >
                      {{ opt.label }}
                    </div>
                    <span class="flex-1 text-sm" style="color: var(--theme-text);">{{ opt.text }}</span>
                    <CheckCircle v-if="correctLabels.has(opt.label)" class="w-5 h-5 text-green-500 flex-shrink-0" />
                  </div>
                </div>
              </div>

              <!-- 考察点 + 前置题目 -->
              <div
                v-if="(question.examinePoints && question.examinePoints.length) || (question.prerequisiteIds && question.prerequisiteIds.length)"
                class="rounded-xl shadow-sm p-6"
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

              <!-- Hint（可折叠） -->
              <div v-if="question.hint" class="rounded-xl shadow-sm overflow-hidden" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
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
                  <div class="text-sm leading-relaxed bg-yellow-50 p-4 rounded-lg" style="color: var(--theme-text-secondary);">
                    {{ question.hint }}
                  </div>
                </div>
              </div>

              <!-- 参考答案与解析 -->
              <div class="rounded-xl shadow-sm p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
                  <CheckCircle class="w-5 h-5 mr-2 text-green-500" />
                  参考答案与解析
                </h2>

                <!-- 无答案内容时的提示 -->
                <div v-if="!hasAnswerContent" class="text-center py-8">
                  <AlertCircle class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
                  <p class="text-sm" style="color: var(--theme-text-secondary);">该题目暂无参考答案</p>
                </div>

                <!-- 答题大纲 -->
                <div v-if="question.answerOutline" class="mb-6 last:mb-0">
                  <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                    <ListChecks class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                    答题大纲
                  </h3>
                  <div class="p-4 rounded-lg" style="background-color: var(--theme-bg);">
                    <MarkdownRenderer editor-mode="markdown" :content-markdown="question.answerOutline" prose-width="normal" />
                  </div>
                </div>

                <!-- 正确答案（选择题） -->
                <div v-if="question.correctAnswer && question.practiceMode === 'choice'" class="mb-6 last:mb-0">
                  <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                    <CheckCircle class="w-4 h-4 mr-2 text-green-500" />
                    正确答案
                  </h3>
                  <div class="p-4 rounded-lg border-l-4 border-green-400" style="background-color: var(--theme-bg);">
                    <span class="text-lg font-bold tracking-wide" style="color: var(--theme-text);">
                      {{ question.correctAnswer }}
                    </span>
                    <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">（绿色高亮选项为正确项）</span>
                  </div>
                </div>

                <!-- 官方参考答案 -->
                <div v-if="question.referenceAnswer" class="mb-6 last:mb-0">
                  <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                    <BookOpen class="w-4 h-4 mr-2 text-green-500" />
                    官方参考答案
                  </h3>
                  <div class="p-4 rounded-lg border-l-4 border-green-400" style="background-color: var(--theme-bg);">
                    <MarkdownRenderer editor-mode="markdown" :content-markdown="question.referenceAnswer" prose-width="normal" />
                  </div>
                </div>

                <!-- 参考代码（算法题） -->
                <div v-if="question.solution" class="mb-6 last:mb-0">
                  <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                    <FileText class="w-4 h-4 mr-2 text-blue-500" />
                    参考代码
                  </h3>
                  <pre class="bg-gray-900 text-gray-100 rounded-lg p-4 text-xs overflow-x-auto"><code>{{ question.solution }}</code></pre>
                </div>

                <!-- 题目解析 -->
                <div v-if="question.analysis" class="mb-6 last:mb-0">
                  <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                    <Lightbulb class="w-4 h-4 mr-2 text-yellow-500" />
                    题目解析
                  </h3>
                  <div class="p-4 rounded-lg border-l-4 border-yellow-400" style="background-color: var(--theme-bg);">
                    <div class="text-sm leading-relaxed whitespace-pre-wrap" style="color: var(--theme-text);">
                      {{ question.analysis }}
                    </div>
                  </div>
                </div>

                <!-- 评分标准 -->
                <div v-if="question.scoringCriteria && question.scoringCriteria.length" class="last:mb-0">
                  <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                    <Layers class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                    评分标准
                  </h3>
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
              </div>

              <!-- 上一题/下一题导航（与来源列表筛选同源，连续阅读） -->
              <div
                v-if="neighbor"
                class="rounded-xl shadow-sm p-4 flex items-center justify-between gap-3"
                style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
              >
                <button
                  @click="gotoNeighbor(neighbor?.prevId, 'prev')"
                  :disabled="!neighbor?.prevId"
                  :title="neighbor?.prevTitle ? `上一题：${neighbor.prevTitle}` : '已是第一题'"
                  class="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed max-w-[42%]"
                  style="border-color: var(--theme-border); color: var(--theme-text);"
                >
                  <ChevronLeft class="w-4 h-4 flex-shrink-0" />
                  <span class="truncate">{{ neighbor?.prevTitle || '上一题' }}</span>
                </button>
                <span
                  v-if="neighbor?.currentIndex"
                  class="text-xs font-mono px-2.5 py-1 rounded-full flex-shrink-0 whitespace-nowrap"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                  title="当前题目在筛选结果中的位置"
                >
                  第 {{ neighbor.currentIndex }} / {{ neighbor.total }} 题
                </span>
                <button
                  @click="gotoNeighbor(neighbor?.nextId, 'next')"
                  :disabled="!neighbor?.nextId"
                  :title="neighbor?.nextTitle ? `下一题：${neighbor.nextTitle}` : '已是最后一题'"
                  class="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed max-w-[42%]"
                  :style="neighbor?.nextId
                    ? { borderColor: 'var(--theme-primary)', color: 'var(--theme-primary)' }
                    : { borderColor: 'var(--theme-border)', color: 'var(--theme-text)' }"
                >
                  <span class="truncate">{{ neighbor?.nextTitle || '下一题' }}</span>
                  <ChevronRight class="w-4 h-4 flex-shrink-0" />
                </button>
              </div>
            </div>

            <!-- 右侧：精选笔记 -->
            <div class="space-y-6">
              <div v-if="featuredNotes.length > 0" class="rounded-xl shadow-sm p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
                  <Award class="w-5 h-5 mr-2 text-yellow-500" />
                  精选笔记 ({{ featuredNotes.length }})
                </h2>
                <div class="space-y-4">
                  <div
                    v-for="note in featuredNotes"
                    :key="note.id"
                    class="border rounded-lg p-4 transition"
                    style="border-color: var(--theme-border);"
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

              <!-- 快速导航提示 -->
              <div class="rounded-xl shadow-sm p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h3 class="text-sm font-semibold mb-3 flex items-center" style="color: var(--theme-text);">
                  <BookOpen class="w-4 h-4 mr-2" style="color: var(--theme-primary);" />
                  学习建议
                </h3>
                <ul class="text-xs space-y-2" style="color: var(--theme-text-secondary);">
                  <li class="flex items-start">
                    <span class="mr-2">·</span>
                    先仔细阅读题目描述，理解考察方向
                  </li>
                  <li class="flex items-start">
                    <span class="mr-2">·</span>
                    尝试自己思考答案，再对照参考答案
                  </li>
                  <li class="flex items-start">
                    <span class="mr-2">·</span>
                    用"上一题/下一题"按当前筛选顺序连续浏览
                  </li>
                  <li v-if="practiceEntry" class="flex items-start">
                    <span class="mr-2">·</span>
                    点击"{{ practiceEntry.action }}"进入练习，答对计入成长记录
                  </li>
                  <li class="flex items-start">
                    <span class="mr-2">·</span>
                    今日首次阅读本题已记入成长时间线
                  </li>
                  <li class="flex items-start">
                    <span class="mr-2">·</span>
                    收藏题目方便后续复习回顾
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="text-center py-12">
          <p style="color: var(--theme-text-secondary);">未找到题目信息</p>
          <button
            @click="router.push('/learn/questions')"
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
