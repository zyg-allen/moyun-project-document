<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  CheckCircle2, XCircle, Clock, ChevronLeft, ChevronRight,
  Loader2, AlertCircle, Lightbulb, RefreshCw, List, Home,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getQuestionDetail } from '@/api/interview';
import type { InterviewQuestionDetailVO } from '@/types/api';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const error = ref<string | null>(null);
const question = ref<InterviewQuestionDetailVO | null>(null);

// 选项解析
interface QuestionOption {
  label: string;
  text: string;
  is_correct?: boolean;
}
const options = ref<QuestionOption[]>([]);

// 答题状态
const selectedAnswer = ref<string | null>(null);
const submitted = ref(false);
const isCorrect = ref(false);

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
}

function selectOption(label: string) {
  if (submitted.value) return;
  selectedAnswer.value = label;
}

function submitAnswer() {
  if (!selectedAnswer.value || !question.value) return;
  submitted.value = true;
  isCorrect.value = selectedAnswer.value === question.value.correctAnswer;
}

function resetAnswer() {
  selectedAnswer.value = null;
  submitted.value = false;
  isCorrect.value = false;
}

function gotoList() {
  router.push('/learn/practice/choice');
}

onMounted(() => {
  loadQuestion();
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
      <div v-else-if="question" class="max-w-3xl mx-auto">
        <!-- 题目卡片 -->
        <div class="p-6 rounded-2xl border shadow-sm"
             style="background-color: var(--theme-card-bg); border-color: var(--theme-border);">
          <!-- 题头 -->
          <div class="flex items-center gap-2 mb-4">
            <span class="text-xs px-2 py-0.5 rounded font-medium"
                  :class="DIFFICULTY_MAP[question.difficulty]?.class || 'bg-gray-100 text-gray-600'">
              {{ DIFFICULTY_MAP[question.difficulty]?.label || question.difficulty || '未分级' }}
            </span>
            <span v-if="question.tags && question.tags.length" class="text-[10px]" style="color: var(--theme-text-secondary);">
              {{ question.tags.join('、') }}
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

          <!-- 选项列表 -->
          <div class="space-y-2.5">
            <button
              v-for="opt in options"
              :key="opt.label"
              @click="selectOption(opt.label)"
              :disabled="submitted"
              :class="[
                'w-full flex items-center gap-3 p-3 rounded-xl border transition-all text-left',
                selectedAnswer === opt.label ? 'border-2' : '',
                submitted && opt.is_correct ? 'border-green-500 bg-green-50' : '',
                submitted && selectedAnswer === opt.label && !opt.is_correct ? 'border-red-500 bg-red-50' : '',
                !submitted && selectedAnswer !== opt.label ? 'hover:shadow-sm' : '',
              ]"
              :style="selectedAnswer === opt.label && !submitted
                ? { borderColor: 'var(--theme-primary)', backgroundColor: 'var(--theme-primary-bg)' }
                : { borderColor: 'var(--theme-border)', backgroundColor: 'var(--theme-bg)' }"
            >
              <!-- 选项标记 -->
              <div
                :class="[
                  'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0',
                  submitted && opt.is_correct ? 'bg-green-500 text-white' : '',
                  submitted && selectedAnswer === opt.label && !opt.is_correct ? 'bg-red-500 text-white' : '',
                  selectedAnswer === opt.label && !submitted ? 'text-white' : '',
                ]"
                :style="selectedAnswer === opt.label && !submitted
                  ? { backgroundColor: 'var(--theme-primary)' }
                  : (submitted && opt.is_correct) || (submitted && selectedAnswer === opt.label && !opt.is_correct)
                    ? {}
                    : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
              >
                {{ opt.label }}
              </div>
              <!-- 选项文本 -->
              <span class="flex-1 text-sm" style="color: var(--theme-text);">{{ opt.text }}</span>
              <!-- 判定图标 -->
              <CheckCircle2 v-if="submitted && opt.is_correct" class="w-5 h-5 text-green-500 flex-shrink-0" />
              <XCircle v-else-if="submitted && selectedAnswer === opt.label && !opt.is_correct" class="w-5 h-5 text-red-500 flex-shrink-0" />
            </button>
          </div>

          <!-- 提交后判定结果 -->
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
            <p v-if="!isCorrect && question.correctAnswer" class="text-xs" style="color: var(--theme-text-secondary);">
              正确答案：{{ question.correctAnswer }}
            </p>
          </div>

          <!-- 解析 -->
          <div v-if="submitted && question.analysis" class="mt-4 p-4 rounded-xl border"
               style="background-color: var(--theme-bg); border-color: var(--theme-border);">
            <div class="flex items-center gap-1.5 mb-2">
              <Lightbulb class="w-4 h-4" style="color: #D97706;" />
              <span class="text-sm font-bold" style="color: var(--theme-text);">题目解析</span>
            </div>
            <p class="text-xs leading-relaxed whitespace-pre-wrap" style="color: var(--theme-text);">
              {{ question.analysis }}
            </p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center gap-2 mt-5">
            <button
              v-if="!submitted"
              @click="submitAnswer"
              :disabled="!selectedAnswer"
              class="px-5 py-2 text-sm font-medium text-white rounded-lg disabled:opacity-40 transition-opacity"
              style="background-color: var(--theme-primary);"
            >
              提交答案
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
              @click="gotoList"
              class="inline-flex items-center gap-1.5 px-4 py-2 text-sm font-medium rounded-lg border"
              style="border-color: var(--theme-border); color: var(--theme-text);"
            >
              <List class="w-3.5 h-3.5" />
              返回列表
            </button>
          </div>
        </div>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>
