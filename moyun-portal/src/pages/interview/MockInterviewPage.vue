<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, Loader2, MessageSquare, Award, CheckCircle2, ChevronLeft,
  ChevronRight, Flag, RefreshCw, ListChecks, Sparkles, Trophy, X,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import {
  startMockInterview, getMockInterviewDetail, submitMockAnswer,
  finishMockInterview, getMyMockInterviews,
} from '@/api/mockInterview';
import type { MockInterviewDetailVO, MockInterviewVO } from '@/types/api';

const router = useRouter();

type Stage = 'start' | 'answering' | 'result';
const stage = ref<Stage>('start');

// 开始表单
const position = ref('');
const scene = ref('');
const starting = ref(false);

// 进行中面试
const interview = ref<MockInterviewDetailVO | null>(null);
const currentIdx = ref(0);
const answer = ref('');
const submitting = ref(false);
const finishing = ref(false);

// 我的面试历史
const history = ref<MockInterviewVO[]>([]);
const historyLoading = ref(false);

const scenePresets = ['算法', '系统设计', '前端', '后端', '数据库', '项目深挖'];
const positionPresets = ['后端开发', '前端开发', '全栈工程师', '算法工程师', '数据工程师'];

const currentQa = computed(() => {
  if (!interview.value || !interview.value.qaList) return null;
  return interview.value.qaList[currentIdx.value] || null;
});

const totalQa = computed(() => interview.value?.totalQa || 0);
const answeredCount = computed(() => interview.value?.answeredCount || 0);
const progressPercent = computed(() => {
  if (totalQa.value === 0) return 0;
  return Math.round((answeredCount.value / totalQa.value) * 100);
});

const isCurrentAnswered = computed(() => currentQa.value?.score != null);

useHead(computed(() => generateSeo({
  title: 'AI 模拟面试官',
  description: '基于题库的规则化 AI 模拟面试，从岗位/场景抽取题目，作答后即时评分与反馈',
  keywords: ['AI 模拟面试', '面试练习', '模拟面试官', '墨韵'],
  canonicalPath: '/interview/mock',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadHistory();
});

async function loadHistory() {
  historyLoading.value = true;
  try {
    const res = await getMyMockInterviews({ pageNum: 1, pageSize: 10 });
    if (res.code === 200 && res.data) {
      history.value = res.data.list || [];
    }
  } catch (err) {
    // 历史加载失败不阻断主流程
  } finally {
    historyLoading.value = false;
  }
}

async function handleStart() {
  if (starting.value) return;
  starting.value = true;
  try {
    const res = await startMockInterview({
      position: position.value || undefined,
      scene: scene.value || undefined,
    });
    if (res.code === 200 && res.data) {
      interview.value = res.data;
      currentIdx.value = 0;
      answer.value = '';
      stage.value = 'answering';
    } else {
      showToast(res.message || '开始面试失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '开始面试失败，请稍后重试', 'error');
  } finally {
    starting.value = false;
  }
}

async function handleSubmitAnswer() {
  if (!interview.value || submitting.value) return;
  if (!answer.value.trim()) {
    showToast('答案不能为空', 'error');
    return;
  }
  if (isCurrentAnswered.value) {
    // 已作答，直接跳下一题
    gotoNext();
    return;
  }
  submitting.value = true;
  try {
    const res = await submitMockAnswer(interview.value.id, {
      questionIdx: currentIdx.value,
      answer: answer.value,
    });
    if (res.code === 200 && res.data) {
      // 局部更新当前题，避免整页刷新丢失其他题状态
      if (interview.value.qaList) {
        interview.value.qaList[currentIdx.value] = res.data;
        interview.value.answeredCount = (interview.value.answeredCount || 0) + 1;
      }
      showToast(`评分完成：${res.data.score} 分`, 'success');
    } else {
      showToast(res.message || '提交失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '提交失败，请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
}

function gotoPrev() {
  if (currentIdx.value === 0) return;
  currentIdx.value -= 1;
  syncAnswerFromQa();
}

function gotoNext() {
  if (!interview.value) return;
  if (currentIdx.value >= totalQa.value - 1) return;
  currentIdx.value += 1;
  syncAnswerFromQa();
}

function gotoQuestion(idx: number) {
  if (idx < 0 || idx >= totalQa.value) return;
  currentIdx.value = idx;
  syncAnswerFromQa();
}

// 切题时同步答案：已答显示历史答案，未答清空
function syncAnswerFromQa() {
  answer.value = currentQa.value?.userAnswer || '';
}

async function handleFinish() {
  if (!interview.value || finishing.value) return;
  if (answeredCount.value === 0) {
    showToast('至少回答 1 道题再结束面试', 'error');
    return;
  }
  if (!window.confirm(`确定结束本次面试吗？已答 ${answeredCount.value}/${totalQa.value} 题`)) return;
  finishing.value = true;
  try {
    const res = await finishMockInterview(interview.value.id);
    if (res.code === 200 && res.data) {
      interview.value = res.data;
      stage.value = 'result';
      loadHistory();
    } else {
      showToast(res.message || '结束失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '结束失败，请稍后重试', 'error');
  } finally {
    finishing.value = false;
  }
}

function restartInterview() {
  interview.value = null;
  currentIdx.value = 0;
  answer.value = '';
  stage.value = 'start';
}

function viewHistoryDetail(item: MockInterviewVO) {
  // 跳转到结果页查看历史详情
  loadInterviewDetail(item.id);
}

async function loadInterviewDetail(id: string | number) {
  try {
    const res = await getMockInterviewDetail(id);
    if (res.code === 200 && res.data) {
      interview.value = res.data;
      currentIdx.value = 0;
      syncAnswerFromQa();
      stage.value = res.data.status === 'finished' ? 'result' : 'answering';
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      showToast(res.message || '加载失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '加载失败', 'error');
  }
}

function goBack() {
  if (stage.value !== 'start') {
    // 面试进行中/结果页，先回开始页
    if (stage.value === 'answering' && answeredCount.value > 0) {
      if (!window.confirm('退出将不会自动结束面试，可稍后从历史继续。是否退出？')) return;
    }
    restartInterview();
    return;
  }
  router.push('/interview');
}

function scoreColor(score?: number): string {
  if (score == null) return 'var(--theme-text-secondary)';
  if (score >= 80) return '#16a34a';
  if (score >= 60) return '#f59e0b';
  if (score >= 40) return '#f97316';
  return '#ef4444';
}

function formatTime(t?: string): string {
  if (!t) return '';
  return t.replace('T', ' ').slice(0, 16);
}

// Toast
const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
let toastTimer: number | null = null;
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value = null; }, 3000);
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-80"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          {{ stage === 'start' ? '返回面试指南' : '返回' }}
        </button>
        <span class="text-sm font-medium flex items-center" style="color: var(--theme-text);">
          <MessageSquare class="w-4 h-4 mr-1.5" />AI 模拟面试官
        </span>
        <span class="w-16"></span>
      </div>
    </div>

    <!-- Toast -->
    <div
      v-if="toast"
      class="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-lg shadow-lg text-sm"
      :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- ============ 开始页 ============ -->
    <template v-if="stage === 'start'">
      <div
        class="relative overflow-hidden text-white py-12"
        style="background: linear-gradient(135deg, var(--theme-primary), color-mix(in srgb, var(--theme-primary) 60%, #4338ca 100%));"
      >
        <div class="absolute inset-0 opacity-10 pointer-events-none">
          <div class="absolute top-6 left-10 w-40 h-40 rounded-full bg-white"></div>
          <div class="absolute bottom-6 right-16 w-64 h-64 rounded-full bg-white"></div>
        </div>
        <div class="relative max-w-4xl mx-auto px-4 text-center">
          <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-3">
            <Sparkles class="w-4 h-4 mr-2" /> 墨韵 · 模拟面试
          </div>
          <h1 class="text-2xl md:text-3xl font-bold mb-2">AI 模拟面试官</h1>
          <p class="text-sm opacity-90">选择岗位与场景，系统抽取 5 道题，作答后即时规则评分</p>
        </div>
      </div>

      <div class="flex-1 py-8">
        <div class="max-w-2xl mx-auto px-4">
          <!-- 配置卡片 -->
          <div
            class="rounded-xl border p-6 mb-6"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <h2 class="text-base font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
              <MessageSquare class="w-4 h-4 mr-2" />面试配置
            </h2>

            <!-- 岗位 -->
            <div class="mb-5">
              <label class="block text-sm mb-2" style="color: var(--theme-text);">面试岗位</label>
              <input
                v-model="position"
                type="text"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                placeholder="如 后端开发工程师（可留空）"
              />
              <div class="flex flex-wrap gap-1.5 mt-2">
                <button
                  v-for="p in positionPresets"
                  :key="p"
                  @click="position = p"
                  class="px-2.5 py-1 rounded-full text-xs transition"
                  :style="position === p
                    ? { backgroundColor: 'var(--theme-primary)', color: '#fff' }
                    : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
                >{{ p }}</button>
              </div>
            </div>

            <!-- 场景 -->
            <div class="mb-6">
              <label class="block text-sm mb-2" style="color: var(--theme-text);">面试场景</label>
              <input
                v-model="scene"
                type="text"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                placeholder="如 算法 / 系统设计（按题目标签匹配，可留空）"
              />
              <div class="flex flex-wrap gap-1.5 mt-2">
                <button
                  v-for="s in scenePresets"
                  :key="s"
                  @click="scene = s"
                  class="px-2.5 py-1 rounded-full text-xs transition"
                  :style="scene === s
                    ? { backgroundColor: 'var(--theme-primary)', color: '#fff' }
                    : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
                >{{ s }}</button>
              </div>
            </div>

            <button
              @click="handleStart"
              :disabled="starting"
              class="w-full inline-flex items-center justify-center px-4 py-2.5 text-white rounded-lg text-sm transition hover:opacity-90 disabled:opacity-50"
              style="background-color: var(--theme-primary);"
            >
              <Loader2 v-if="starting" class="w-4 h-4 mr-1.5 animate-spin" />
              <Sparkles v-else class="w-4 h-4 mr-1.5" />
              {{ starting ? '正在抽取题目…' : '开始模拟面试' }}
            </button>
            <p class="mt-3 text-xs text-center" style="color: var(--theme-text-secondary);">
              将从题库随机抽取 5 道题，作答后即时给出规则评分与反馈
            </p>
          </div>

          <!-- 历史记录 -->
          <div v-if="history.length > 0" class="rounded-xl border" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <div class="px-5 py-3 border-b flex items-center" style="border-color: var(--theme-border);">
              <ListChecks class="w-4 h-4 mr-1.5" style="color: var(--theme-text-secondary);" />
              <span class="text-sm font-medium" style="color: var(--theme-text);">我的模拟面试</span>
            </div>
            <div class="divide-y" style="border-color: var(--theme-border);">
              <button
                v-for="item in history"
                :key="item.id"
                @click="viewHistoryDetail(item)"
                class="w-full text-left px-5 py-3 transition hover:opacity-80"
                style="border-color: var(--theme-border);"
              >
                <div class="flex items-center justify-between mb-1">
                  <span class="text-sm font-medium" style="color: var(--theme-text);">
                    {{ item.position || '未指定岗位' }} · {{ item.scene || '综合' }}
                  </span>
                  <span
                    class="text-xs px-2 py-0.5 rounded"
                    :style="item.status === 'finished'
                      ? { backgroundColor: 'rgba(22,163,74,0.12)', color: '#16a34a' }
                      : { backgroundColor: 'rgba(245,158,11,0.12)', color: '#f59e0b' }"
                  >
                    {{ item.status === 'finished' ? '已结束' : '进行中' }}
                  </span>
                </div>
                <div class="flex items-center gap-4 text-xs" style="color: var(--theme-text-secondary);">
                  <span>共 {{ item.totalQa }} 题</span>
                  <span v-if="item.score != null" :style="{ color: scoreColor(item.score) }">
                    得分 {{ item.score }}
                  </span>
                  <span>{{ formatTime(item.createTime) }}</span>
                </div>
              </button>
            </div>
          </div>
          <div
            v-else-if="!historyLoading"
            class="rounded-xl border p-8 text-center"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <MessageSquare class="w-10 h-10 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.4;" />
            <p class="text-sm" style="color: var(--theme-text-secondary);">还没有模拟面试记录，开始第一次吧</p>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 答题页 ============ -->
    <template v-else-if="stage === 'answering' && interview">
      <!-- 进度条 -->
      <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div class="max-w-3xl mx-auto px-4 py-3">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium" style="color: var(--theme-text);">
              第 {{ currentIdx + 1 }} / {{ totalQa }} 题
            </span>
            <span class="text-xs" style="color: var(--theme-text-secondary);">
              已答 {{ answeredCount }} / {{ totalQa }} 题
            </span>
          </div>
          <div class="h-1.5 rounded-full overflow-hidden" style="background-color: var(--theme-bg);">
            <div
              class="h-full rounded-full transition-all"
              :style="{ width: progressPercent + '%', backgroundColor: 'var(--theme-primary)' }"
            ></div>
          </div>
        </div>
      </div>

      <div class="flex-1 py-8">
        <div class="max-w-3xl mx-auto px-4">
          <!-- 题目导航 -->
          <div class="flex items-center gap-1.5 mb-4 flex-wrap">
            <button
              v-for="(qa, idx) in interview.qaList"
              :key="qa.id"
              @click="gotoQuestion(idx)"
              class="w-8 h-8 rounded-full text-xs font-medium transition flex items-center justify-center"
              :style="idx === currentIdx
                ? { backgroundColor: 'var(--theme-primary)', color: '#fff' }
                : qa.score != null
                  ? { backgroundColor: 'rgba(22,163,74,0.12)', color: '#16a34a', border: '1px solid rgba(22,163,74,0.3)' }
                  : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
              :title="`第 ${idx + 1} 题`"
            >{{ idx + 1 }}</button>
          </div>

          <!-- 题目卡片 -->
          <div
            class="rounded-xl border p-5 mb-4"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-start">
              <span
                class="inline-flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold mr-3 flex-shrink-0"
                style="background-color: var(--theme-primary); color: #fff;"
              >{{ currentIdx + 1 }}</span>
              <p class="text-base font-medium leading-relaxed" style="color: var(--theme-text);">
                {{ currentQa?.question }}
              </p>
            </div>
          </div>

          <!-- 答案输入 -->
          <div
            class="rounded-xl border overflow-hidden mb-4"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="px-4 py-2 border-b flex items-center justify-between" style="border-color: var(--theme-border); background-color: var(--theme-bg);">
              <span class="text-xs font-medium" style="color: var(--theme-text-secondary);">你的回答</span>
              <span v-if="isCurrentAnswered" class="text-xs flex items-center" :style="{ color: scoreColor(currentQa?.score) }">
                <CheckCircle2 class="w-3.5 h-3.5 mr-1" />已评分：{{ currentQa?.score }} 分
              </span>
            </div>
            <textarea
              v-model="answer"
              :disabled="isCurrentAnswered"
              class="w-full p-4 text-sm resize-none focus:outline-none disabled:opacity-70"
              style="background-color: var(--theme-surface); color: var(--theme-text); height: 200px;"
              placeholder="请在此输入你的回答，提交后将获得 AI 规则评分…"
            ></textarea>
          </div>

          <!-- 已答题反馈 -->
          <div
            v-if="isCurrentAnswered && currentQa?.aiFeedback"
            class="rounded-xl border p-4 mb-4"
            style="background-color: var(--theme-bg); border-color: var(--theme-border);"
          >
            <div class="flex items-center mb-2">
              <Sparkles class="w-4 h-4 mr-1.5" :style="{ color: scoreColor(currentQa?.score) }" />
              <span class="text-sm font-medium" style="color: var(--theme-text);">AI 反馈</span>
              <span class="ml-auto text-xs" :style="{ color: scoreColor(currentQa?.score) }">
                得分 {{ currentQa?.score }} / 100
              </span>
            </div>
            <p class="text-sm leading-relaxed" style="color: var(--theme-text-secondary);">
              {{ currentQa?.aiFeedback }}
            </p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center justify-between gap-3">
            <button
              @click="gotoPrev"
              :disabled="currentIdx === 0"
              class="inline-flex items-center px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed"
              style="background-color: var(--theme-surface); color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              <ChevronLeft class="w-4 h-4 mr-1" />上一题
            </button>

            <button
              v-if="!isCurrentAnswered"
              @click="handleSubmitAnswer"
              :disabled="submitting"
              class="inline-flex items-center px-5 py-2 text-white rounded-lg text-sm transition hover:opacity-90 disabled:opacity-50"
              style="background-color: var(--theme-primary);"
            >
              <Loader2 v-if="submitting" class="w-4 h-4 mr-1 animate-spin" />
              <CheckCircle2 v-else class="w-4 h-4 mr-1" />
              {{ submitting ? '评分中…' : '提交并评分' }}
            </button>
            <button
              v-else-if="currentIdx < totalQa - 1"
              @click="gotoNext"
              class="inline-flex items-center px-5 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              下一题<ChevronRight class="w-4 h-4 ml-1" />
            </button>
            <button
              v-else
              @click="handleFinish"
              :disabled="finishing || answeredCount === 0"
              class="inline-flex items-center px-5 py-2 text-white rounded-lg text-sm transition hover:opacity-90 disabled:opacity-50"
              style="background-color: #16a34a;"
            >
              <Loader2 v-if="finishing" class="w-4 h-4 mr-1 animate-spin" />
              <Flag v-else class="w-4 h-4 mr-1" />
              {{ finishing ? '生成总结中…' : '结束面试' }}
            </button>

            <button
              v-if="currentIdx < totalQa - 1 || answeredCount < totalQa"
              @click="handleFinish"
              :disabled="finishing || answeredCount === 0"
              class="inline-flex items-center px-3 py-2 rounded-lg text-sm transition disabled:opacity-40"
              style="color: #16a34a; border: 1px solid var(--theme-border); background-color: var(--theme-surface);"
              title="结束面试并生成总结"
            >
              <Flag class="w-4 h-4 mr-1" />结束
            </button>
            <span v-else class="w-20"></span>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 结果页 ============ -->
    <template v-else-if="stage === 'result' && interview">
      <div class="flex-1 py-8">
        <div class="max-w-3xl mx-auto px-4">
          <!-- 总分卡片 -->
          <div
            class="rounded-xl border p-6 mb-5 text-center"
            style="background: linear-gradient(135deg, var(--theme-surface), color-mix(in srgb, var(--theme-primary) 8%, var(--theme-surface))); border-color: var(--theme-border);"
          >
            <div class="inline-flex items-center justify-center w-14 h-14 rounded-full mb-3" :style="{ backgroundColor: 'color-mix(in srgb, ' + scoreColor(interview.score) + ' 15%, transparent)' }">
              <Trophy class="w-7 h-7" :style="{ color: scoreColor(interview.score) }" />
            </div>
            <h2 class="text-lg font-semibold mb-1" style="color: var(--theme-text);">模拟面试完成</h2>
            <div class="flex items-center justify-center gap-2 mb-2">
              <span class="text-4xl font-bold" :style="{ color: scoreColor(interview.score) }">{{ interview.score ?? 0 }}</span>
              <span class="text-sm" style="color: var(--theme-text-secondary);">/ 100</span>
            </div>
            <p class="text-sm mb-3" style="color: var(--theme-text-secondary);">
              {{ interview.position || '未指定岗位' }} · {{ interview.scene || '综合' }} · 共 {{ interview.totalQa }} 题 · 答完 {{ interview.answeredCount }} 题
            </p>
            <p v-if="interview.summary" class="text-sm leading-relaxed px-4 py-3 rounded-lg" style="background-color: var(--theme-bg); color: var(--theme-text);">
              {{ interview.summary }}
            </p>
          </div>

          <!-- 每题反馈 -->
          <div class="space-y-3 mb-6">
            <h3 class="text-sm font-medium flex items-center" style="color: var(--theme-text);">
              <Award class="w-4 h-4 mr-1.5" />逐题反馈
            </h3>
            <div
              v-for="(qa, idx) in interview.qaList"
              :key="qa.id"
              class="rounded-xl border p-4"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <div class="flex items-start mb-2">
                <span
                  class="inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-bold mr-3 flex-shrink-0"
                  :style="{ backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text)' }"
                >{{ idx + 1 }}</span>
                <p class="text-sm font-medium flex-1" style="color: var(--theme-text);">{{ qa.question }}</p>
                <span
                  v-if="qa.score != null"
                  class="text-sm font-bold ml-2"
                  :style="{ color: scoreColor(qa.score) }"
                >{{ qa.score }}分</span>
                <span v-else class="text-xs ml-2" style="color: var(--theme-text-secondary);">未作答</span>
              </div>
              <div v-if="qa.userAnswer" class="text-xs mb-2 px-3 py-2 rounded" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
                <span style="color: var(--theme-text);">你的回答：</span>{{ qa.userAnswer }}
              </div>
              <div v-if="qa.aiFeedback" class="text-xs flex items-start" style="color: var(--theme-text-secondary);">
                <Sparkles class="w-3.5 h-3.5 mr-1 mt-0.5 flex-shrink-0" :style="{ color: scoreColor(qa.score) }" />
                <span>{{ qa.aiFeedback }}</span>
              </div>
            </div>
          </div>

          <!-- 操作 -->
          <div class="flex items-center justify-center gap-3">
            <button
              @click="restartInterview"
              class="inline-flex items-center px-5 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
              style="background-color: var(--theme-primary);"
            >
              <RefreshCw class="w-4 h-4 mr-1" />再来一次
            </button>
            <button
              @click="goBack"
              class="inline-flex items-center px-5 py-2 rounded-lg text-sm transition hover:opacity-80"
              style="background-color: var(--theme-surface); color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              返回
            </button>
          </div>
        </div>
      </div>
    </template>

    <SiteFooter />
  </div>
</template>
