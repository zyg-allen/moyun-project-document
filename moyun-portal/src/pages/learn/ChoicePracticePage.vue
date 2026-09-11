<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  CheckCircle2, XCircle, Clock, ChevronLeft, ChevronRight,
  Loader2, AlertCircle, Lightbulb, RefreshCw, List,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getQuestionDetail, getQuestionNeighbor, submitAnswer as submitAnswerApi } from '@/api/interview';
import type { InterviewQuestionDetailVO, InterviewQuestionNeighborVO } from '@/types/api';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const loading = ref(true);
const error = ref<string | null>(null);
const question = ref<InterviewQuestionDetailVO | null>(null);

// 相邻题目导航（与来源列表页筛选同源：sort 升序 + createTime 降序）
const neighbor = ref<InterviewQuestionNeighborVO | null>(null);

// 选项解析
interface QuestionOption {
  label: string;
  text: string;
  is_correct?: boolean; // 仅本地预填态展示用，判分以服务端返回为准
}
const options = ref<QuestionOption[]>([]);

// 作答状态：单选题仅保留一个选中项，多选题可累积多个
const selectedLabels = ref<string[]>([]);
const submitted = ref(false);
const isCorrect = ref(false);
const submitting = ref(false);
const submitError = ref<string | null>(null);
const serverAnalysis = ref('');
const serverCorrectAnswer = ref('');

// 多选判定：correctAnswer 含多个选项字母即多选题（后端按选项字母集合判分）
const correctLabelSet = computed(() => {
  const answer = question.value?.correctAnswer || '';
  const labels = answer.trim().toUpperCase().split(/[^A-Z]+/).filter(Boolean);
  return new Set(labels);
});
const isMulti = computed(() => correctLabelSet.value.size > 1);

// 提交时所选答案快照（判定区展示，重做后清空）
const submittedAnswerText = computed(() => {
  if (!submitted.value) return '';
  return [...selectedLabels.value].sort().join('、');
});

useHead(computed(() => generateSeo({
  title: question.value ? `选择题练习 - ${question.value.title}` : '选择题练习',
  description: '在线选择题练习，即时判定与解析',
  keywords: ['选择题', '练习', '旭林'],
  canonicalPath: `/learn/practice/choice/${route.params.id}`,
})));

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '刷题中心', path: '/learn/practice' },
  { label: '选择题', path: '/learn/practice/choice' },
  { label: '做题' },
]);

// 是否有选择题选项数据
const hasOptions = computed(() => options.value.length > 0);

const DIFFICULTY_MAP: Record<string, { label: string; class: string }> = {
  easy: { label: '简单', class: 'bg-green-100 text-green-700' },
  medium: { label: '中等', class: 'bg-yellow-100 text-yellow-700' },
  hard: { label: '困难', class: 'bg-red-100 text-red-700' },
};

async function loadQuestion() {
  loading.value = true;
  error.value = null;
  try {
    const id = route.params.id;
    const res = await getQuestionDetail(id as string | number);
    if (res.code === 200 && res.data) {
      question.value = res.data;
      // 解析 options JSON
      if (res.data.options) {
        try {
          options.value = JSON.parse(res.data.options);
        } catch {
          options.value = [];
        }
      }
    } else {
      error.value = res.message || '加载题目失败';
    }
  } catch (err: any) {
    error.value = err?.message || '加载题目失败，请稍后重试';
  } finally {
    loading.value = false;
  }
  loadNeighbor();
}

/** 相邻题目导航：与来源列表页同源筛选（difficulty/keyword 由列表页跳转时透传） */
async function loadNeighbor() {
  neighbor.value = null;
  try {
    const params: { practiceMode: string; difficulty?: string; keyword?: string } = {
      practiceMode: 'choice',
    };
    const q = route.query;
    if (typeof q.difficulty === 'string' && q.difficulty) params.difficulty = q.difficulty;
    if (typeof q.keyword === 'string' && q.keyword) params.keyword = q.keyword;
    const res = await getQuestionNeighbor(route.params.id as string | number, params);
    if (res.code === 200 && res.data) {
      neighbor.value = res.data;
    }
  } catch {
    // 导航数据加载失败不影响做题主流程
  }
}

/** 切题跳转（保留筛选上下文）；未提交时确认防误触丢失作答 */
function gotoNeighbor(id: string | number | null | undefined, dir: 'prev' | 'next') {
  if (id == null) return;
  const hasUnsaved = selectedLabels.value.length > 0 && !submitted.value;
  if (hasUnsaved) {
    if (!window.confirm('当前作答尚未提交，切换题目将丢弃已选内容，确定切换吗？')) return;
  }
  const query: Record<string, string> = {};
  if (typeof route.query.difficulty === 'string' && route.query.difficulty) query.difficulty = route.query.difficulty;
  if (typeof route.query.keyword === 'string' && route.query.keyword) query.keyword = route.query.keyword;
  toast.info(dir === 'prev' ? '已切换到上一题' : '已切换到下一题');
  router.push({ path: `/learn/practice/choice/${id}`, query });
  window.scrollTo({ top: 0 });
}

function toggleOption(label: string) {
  if (submitted.value) return;
  if (isMulti.value) {
    // 多选：点击切换选中态
    const idx = selectedLabels.value.indexOf(label);
    if (idx >= 0) selectedLabels.value.splice(idx, 1);
    else selectedLabels.value.push(label);
  } else {
    // 单选：仅保留当前项
    selectedLabels.value = [label];
  }
}

async function submitAnswer() {
  if (!selectedLabels.value.length || !question.value || submitting.value) return;
  submitting.value = true;
  submitError.value = null;
  try {
    const res = await submitAnswerApi(question.value.id, {
      answerType: 'choice',
      // 多选按逗号拼接（后端按选项字母集合判分，顺序不敏感）
      answer: selectedLabels.value.join(','),
    });
    if (res.code === 200 && res.data) {
      submitted.value = true;
      // 服务端权威判分结果（后端已隐藏正确答案，客户端无法比对）
      isCorrect.value = !!res.data.passed;
      serverAnalysis.value = res.data.analysis || '';
      serverCorrectAnswer.value = res.data.correctAnswer || '';
      if (isCorrect.value) {
        toast.success('回答正确！练习已记录，首次通过计入成长');
      }
    }
  } catch (err: any) {
    submitError.value = err?.message || '提交失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
}

function resetAnswer() {
  selectedLabels.value = [];
  submitted.value = false;
  isCorrect.value = false;
  serverAnalysis.value = '';
  serverCorrectAnswer.value = '';
  submitError.value = null;
}

function gotoList() {
  router.push('/learn/practice/choice');
}

function gotoDetail() {
  if (question.value) {
    router.push(`/interview/question/${question.value.id}`);
  }
}

onMounted(() => {
  loadQuestion();
});

// 同页面切题（上一题/下一题）：路由参数变化时重置作答状态并加载新题
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    resetAnswer();
    question.value = null;
    options.value = [];
    loadQuestion();
  }
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <main class="flex-1 w-full max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <Breadcrumb :items="breadcrumbs" />

      <!-- 返回按钮 -->
      <button
        @click="gotoList"
        class="mt-4 mb-4 inline-flex items-center gap-1.5 text-sm hover:opacity-80 transition-opacity"
        style="color: var(--theme-text-secondary);"
      >
        <ChevronLeft class="w-4 h-4" />
        返回选择题列表
      </button>

      <!-- 加载中 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20">
        <Loader2 class="w-8 h-8 animate-spin mb-3" style="color: var(--theme-primary);" />
        <p class="text-sm" style="color: var(--theme-text-secondary);">加载题目中...</p>
      </div>

      <!-- 错误 -->
      <div v-else-if="error" class="flex flex-col items-center justify-center py-20">
        <AlertCircle class="w-8 h-8 mb-3" style="color: #DC2626;" />
        <p class="text-sm mb-4" style="color: var(--theme-text);">{{ error }}</p>
        <button
          @click="loadQuestion"
          class="px-4 py-2 text-sm text-white rounded-lg"
          style="background-color: var(--theme-primary);"
        >重试</button>
      </div>

      <!-- 做题区 -->
      <div v-else-if="question">
        <!-- 题目卡片 -->
        <div class="p-6 rounded-2xl border shadow-sm"
             style="background-color: var(--theme-card-bg); border-color: var(--theme-border);">
          <!-- 题头 -->
          <div class="flex items-center gap-2 mb-4 flex-wrap">
            <span class="text-xs px-2 py-0.5 rounded font-medium"
                  :class="DIFFICULTY_MAP[question.difficulty]?.class || 'bg-gray-100 text-gray-600'">
              {{ DIFFICULTY_MAP[question.difficulty]?.label || question.difficulty || '未分级' }}
            </span>
            <span v-if="question.tags && question.tags.length" class="text-[10px]" style="color: var(--theme-text-secondary);">
              {{ question.tags.join('、') }}
            </span>
            <!-- 筛选结果集进度（与列表页同源筛选） -->
            <span v-if="neighbor && neighbor.currentIndex" class="ml-auto text-xs font-medium px-2 py-0.5 rounded-full"
                  style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
              第 {{ neighbor.currentIndex }} / {{ neighbor.total }} 题
            </span>
          </div>

          <!-- 题目标题 -->
          <h1 class="text-lg font-bold mb-3" style="color: var(--theme-text);">
            {{ question.title }}
          </h1>

          <!-- 题目描述 -->
          <div v-if="question.description" class="text-sm leading-relaxed mb-5 whitespace-pre-wrap"
               style="color: var(--theme-text);">
            {{ question.description }}
          </div>

          <!-- 无选项数据时的降级提示 -->
          <div v-if="!hasOptions" class="p-5 rounded-xl border text-center"
               style="background-color: var(--theme-bg); border-color: var(--theme-border);">
            <AlertCircle class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
            <p class="text-sm font-medium mb-1" style="color: var(--theme-text);">该题目暂不支持选择题练习模式</p>
            <p class="text-xs mb-4" style="color: var(--theme-text-secondary);">请前往题目详情页查看完整内容与参考答案</p>
            <button
              @click="gotoDetail"
              class="px-4 py-2 text-sm font-medium text-white rounded-lg"
              style="background-color: var(--theme-primary);"
            >
              查看题目详情
            </button>
          </div>

          <!-- 单选/多选题型提示 -->
          <div v-if="hasOptions" class="flex items-center gap-1.5 mb-3 text-xs" style="color: var(--theme-text-secondary);">
            <Clock class="w-3.5 h-3.5" />
            <span v-if="isMulti">多选题：请选出所有正确选项，少选或多选均判错</span>
            <span v-else>单选题：点击选项作答，提交后由服务端判分</span>
          </div>

          <!-- 选项列表 -->
          <div v-if="hasOptions" class="space-y-2.5">
            <button
              v-for="opt in options"
              :key="opt.label"
              @click="toggleOption(opt.label)"
              :disabled="submitted"
              :class="[
                'w-full flex items-center gap-3 p-3 rounded-xl border transition-all text-left',
                selectedLabels.includes(opt.label) ? 'border-2' : '',
                submitted && selectedLabels.includes(opt.label) && isCorrect ? 'border-green-500 bg-green-50' : '',
                submitted && selectedLabels.includes(opt.label) && !isCorrect ? 'border-red-500 bg-red-50' : '',
                !submitted && !selectedLabels.includes(opt.label) ? 'hover:shadow-sm' : '',
              ]"
              :style="selectedLabels.includes(opt.label) && !submitted
                ? { borderColor: 'var(--theme-primary)', backgroundColor: 'var(--theme-primary-bg)' }
                : { borderColor: 'var(--theme-border)', backgroundColor: 'var(--theme-bg)' }"
            >
              <!-- 选项标记：多选用方形，单选用圆形 -->
              <div
                :class="[
                  'flex items-center justify-center text-xs font-bold flex-shrink-0',
                  isMulti ? 'w-6 h-6 rounded-md' : 'w-7 h-7 rounded-full',
                  submitted && selectedLabels.includes(opt.label) ? 'text-white' : '',
                ]"
                :style="submitted && selectedLabels.includes(opt.label)
                  ? { backgroundColor: isCorrect ? '#10B981' : '#EF4444' }
                  : selectedLabels.includes(opt.label) && !submitted
                    ? { backgroundColor: 'var(--theme-primary)' }
                    : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
              >
                {{ opt.label }}
              </div>
              <!-- 选项文本 -->
              <span class="flex-1 text-sm" style="color: var(--theme-text);">{{ opt.text }}</span>
              <!-- 判定图标（基于服务端判定：仅标出用户所选） -->
              <CheckCircle2 v-if="submitted && selectedLabels.includes(opt.label) && isCorrect" class="w-5 h-5 text-green-500 flex-shrink-0" />
              <XCircle v-else-if="submitted && selectedLabels.includes(opt.label) && !isCorrect" class="w-5 h-5 text-red-500 flex-shrink-0" />
            </button>
          </div>

          <!-- 提交后判定结果（服务端权威判分） -->
          <div v-if="submitted" class="mt-4 p-3 rounded-xl" :style="{
            backgroundColor: isCorrect ? '#ECFDF5' : '#FEF2F2',
            border: `1px solid ${isCorrect ? '#A7F3D0' : '#FECACA'}`
          }">
            <div class="flex items-center gap-2 mb-1">
              <CheckCircle2 v-if="isCorrect" class="w-5 h-5 text-green-600" />
              <XCircle v-else class="w-5 h-5 text-red-600" />
              <span class="text-sm font-bold" :style="{ color: isCorrect ? '#059669' : '#DC2626' }">
                {{ isCorrect ? '回答正确！' : '回答错误' }}
              </span>
            </div>
            <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs" style="color: var(--theme-text-secondary);">
              <span>你的答案：{{ submittedAnswerText || '—' }}</span>
              <span v-if="!isCorrect && serverCorrectAnswer">正确答案：{{ serverCorrectAnswer }}</span>
            </div>
            <p v-if="!isCorrect" class="text-xs mt-1.5" style="color: var(--theme-text-secondary);">
              别灰心，可点击「再做一次」重试，或「查看完整解析」掌握解题思路
            </p>
          </div>

          <!-- 解析（服务端判分后下发，做题前不下发防作弊） -->
          <div v-if="submitted && serverAnalysis" class="mt-4 p-4 rounded-xl border"
               style="background-color: var(--theme-bg); border-color: var(--theme-border);">
            <div class="flex items-center gap-1.5 mb-2">
              <Lightbulb class="w-4 h-4" style="color: #D97706;" />
              <span class="text-sm font-bold" style="color: var(--theme-text);">题目解析</span>
            </div>
            <p class="text-xs leading-relaxed whitespace-pre-wrap" style="color: var(--theme-text);">
              {{ serverAnalysis }}
            </p>
          </div>

          <!-- 提交错误提示 -->
          <div v-if="submitError" class="mt-4 p-3 rounded-xl flex items-center gap-2"
               style="background-color: #FEF2F2; border: 1px solid #FECACA;">
            <AlertCircle class="w-4 h-4 text-red-500 flex-shrink-0" />
            <span class="text-xs" style="color: #DC2626;">{{ submitError }}</span>
          </div>

          <!-- 操作按钮 -->
          <div class="flex flex-wrap items-center gap-2 mt-5">
            <button
              v-if="!submitted"
              @click="submitAnswer"
              :disabled="!selectedLabels.length || submitting"
              :title="!selectedLabels.length ? '请先选择选项' : ''"
              class="inline-flex items-center gap-1.5 px-5 py-2 text-sm font-medium text-white rounded-lg disabled:opacity-40 transition-opacity"
              style="background-color: var(--theme-primary);"
            >
              <Loader2 v-if="submitting" class="w-3.5 h-3.5 animate-spin" />
              {{ submitting ? '判分中...' : '提交答案' }}
            </button>
            <button
              v-else
              @click="resetAnswer"
              class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border"
              style="border-color: var(--theme-border); color: var(--theme-text);"
            >
              <RefreshCw class="w-3.5 h-3.5" />
              再做一次
            </button>
            <button
              v-if="submitted"
              @click="gotoDetail"
              class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border"
              style="border-color: var(--theme-border); color: var(--theme-text);"
              title="查看答题大纲、参考答案等完整内容"
            >
              <List class="w-3.5 h-3.5" />
              查看完整解析
            </button>
            <button
              @click="gotoList"
              class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border"
              style="border-color: var(--theme-border); color: var(--theme-text);"
            >
              <List class="w-3.5 h-3.5" />
              返回列表
            </button>
          </div>
        </div>

        <!-- 上一题/下一题导航（与来源列表页同源筛选，连续练习） -->
        <div class="mt-4 p-4 rounded-2xl border flex items-center justify-between gap-3"
             style="background-color: var(--theme-card-bg); border-color: var(--theme-border);">
          <button
            @click="gotoNeighbor(neighbor?.prevId, 'prev')"
            :disabled="!neighbor?.prevId"
            :title="neighbor?.prevTitle ? `上一题：${neighbor.prevTitle}` : '已是第一题'"
            class="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed max-w-[45%]"
            style="border-color: var(--theme-border); color: var(--theme-text);"
          >
            <ChevronLeft class="w-4 h-4 flex-shrink-0" />
            <span class="truncate">{{ neighbor?.prevTitle || '上一题' }}</span>
          </button>
          <button
            @click="gotoNeighbor(neighbor?.nextId, 'next')"
            :disabled="!neighbor?.nextId"
            :title="neighbor?.nextTitle ? `下一题：${neighbor.nextTitle}` : '已是最后一题'"
            class="flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border transition-colors disabled:opacity-40 disabled:cursor-not-allowed max-w-[45%]"
            :style="neighbor?.nextId
              ? { borderColor: 'var(--theme-primary)', color: 'var(--theme-primary)' }
              : { borderColor: 'var(--theme-border)', color: 'var(--theme-text)' }"
          >
            <span class="truncate">{{ neighbor?.nextTitle || '下一题' }}</span>
            <ChevronRight class="w-4 h-4 flex-shrink-0" />
          </button>
        </div>
        <p v-if="neighbor && !neighbor.nextId" class="mt-2 text-center text-xs" style="color: var(--theme-text-secondary);">
          已是当前筛选范围内的最后一题，可返回列表换个难度继续练习
        </p>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>
