<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Loader2, MessageSquare, Award, CheckCircle2, ChevronLeft,
  ChevronRight, Flag, RefreshCw, ListChecks, Sparkles, Trophy,
  Target, Brain, Zap,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import {
  startMockInterview, getMockInterviewDetail, submitMockAnswer,
  finishMockInterview, getMyMockInterviews, getMyMockProfile,
} from '@/api/mockInterview';
import { getInterviewPositions } from '@/api/interview';
import type {
  MockInterviewDetailVO, MockInterviewVO, UserProfileSnapshotVO, WeakTagItem,
  InterviewPositionVO,
} from '@/types/api';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

type Stage = 'start' | 'answering' | 'result';
const stage = ref<Stage>('start');

// 开始表单
const position = ref('');
const scene = ref('');
const personalized = ref(false);
const starting = ref(false);

// 岗位字典（v5.9 阶段1：从后端加载，替换原硬编码 positionPresets）
// 关键修复：前端必须传字典里的 name（如 "Java后端工程师"）才能命中后端 findByName 精确匹配
const positions = ref<InterviewPositionVO[]>([]);
const positionsLoading = ref(false);
// 当前选中的岗位对象（用于展示必备技能/描述）
const selectedPosition = computed<InterviewPositionVO | null>(() =>
  positions.value.find(p => p.name === position.value) || null
);
// 必备技能列表（解析 JSON 字符串）
const selectedPositionSkills = computed<string[]>(() => {
  if (!selectedPosition.value?.requiredSkills) return [];
  try {
    const arr = JSON.parse(selectedPosition.value.requiredSkills);
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
});

// 用户画像（"基于我的画像出题"前置展示）
const profile = ref<UserProfileSnapshotVO | null>(null);
const profileLoading = ref(false);
const profileLoaded = ref(false);

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

// 当前面试是否画像驱动
const isPersonalizedInterview = computed(
  () => !!interview.value && interview.value.isPersonalized === 1
);

// 画像薄弱点排序（兜底，后端已排）
const sortedWeakTags = computed<WeakTagItem[]>(() => {
  const list = profile.value?.weakTags || [];
  return [...list].sort((a, b) => (b.failRate || 0) - (a.failRate || 0));
});

// 画像是否可用（薄弱点 ≥ 1 或 必备技能 ≥ 1）
const profileAvailable = computed(
  () => !!profile.value && profile.value.personalized
);

useHead(computed(() => generateSeo({
  title: 'AI 模拟面试官',
  description: '基于题库的规则化 AI 模拟面试，支持基于用户画像的薄弱点优先抽题，作答后即时评分与反馈',
  keywords: ['AI 模拟面试', '面试练习', '模拟面试官', '画像驱动', '薄弱点', '墨韵'],
  canonicalPath: '/interview/mock',
  robots: 'noindex,nofollow',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '模拟面试' },
]);

onMounted(() => {
  loadPositions();
  loadHistory();
  loadProfile();
});

/** 加载岗位字典（v5.9 阶段1：从后端加载，替换原硬编码 positionPresets） */
async function loadPositions() {
  positionsLoading.value = true;
  try {
    const res = await getInterviewPositions();
    if (res.code === 200 && res.data) {
      positions.value = res.data;
    }
  } catch (err) {
    // 加载失败不阻断主流程，岗位输入框仍可自由输入
    console.error('加载岗位字典失败:', err);
  } finally {
    positionsLoading.value = false;
  }
}

// 岗位/场景变化时重新加载画像（防抖简化为切换时触发）
watch([position, scene], () => {
  // 仅在已加载过一次后再触发刷新（避免初次进入双发请求）
  if (profileLoaded.value) {
    loadProfile(true);
  }
});

async function loadProfile(force = false) {
  if (profileLoading.value) return;
  // 首次加载且非强制时静默；force 表示用户已切换条件
  profileLoading.value = true;
  try {
    const res = await getMyMockProfile({
      position: position.value || undefined,
      scene: scene.value || undefined,
    });
    if (res.code === 200 && res.data) {
      profile.value = res.data;
    } else {
      profile.value = null;
    }
  } catch (err) {
    // 画像加载失败不阻断主流程
    console.error('加载用户画像失败:', err);
    if (force) {
      const e = err as { message?: string };
      toast.error(e?.message || '画像加载失败');
    }
    profile.value = null;
  } finally {
    profileLoaded.value = true;
    profileLoading.value = false;
  }
}

async function loadHistory() {
  historyLoading.value = true;
  try {
    const res = await getMyMockInterviews({ pageNum: 1, pageSize: 10 });
    if (res.code === 200 && res.data) {
      history.value = res.data.list || [];
    }
  } catch (err) {
    console.error('加载面试历史失败:', err);
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
      personalized: personalized.value,
    });
    if (res.code === 200 && res.data) {
      interview.value = res.data;
      currentIdx.value = 0;
      answer.value = '';
      stage.value = 'answering';
      if (interview.value.isPersonalized === 1) {
        toast.success('已根据你的画像抽题');
      }
    } else {
      toast.error(res.message || '开始面试失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '开始面试失败，请稍后重试');
  } finally {
    starting.value = false;
  }
}

async function handleSubmitAnswer() {
  if (!interview.value || submitting.value) return;
  if (!answer.value.trim()) {
    toast.error('答案不能为空');
    return;
  }
  if (isCurrentAnswered.value) {
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
      if (interview.value.qaList) {
        interview.value.qaList[currentIdx.value] = res.data;
        interview.value.answeredCount = (interview.value.answeredCount || 0) + 1;
      }
      toast.success(`评分完成：${res.data.score} 分`);
    } else {
      toast.error(res.message || '提交失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '提交失败，请稍后重试');
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

function syncAnswerFromQa() {
  answer.value = currentQa.value?.userAnswer || '';
}

async function handleFinish() {
  if (!interview.value || finishing.value) return;
  if (answeredCount.value === 0) {
    toast.error('至少回答 1 道题再结束面试');
    return;
  }
  if (!window.confirm(`确定结束本次面试吗？已答 ${answeredCount.value}/${totalQa.value} 题`)) return;
  finishing.value = true;
  try {
    const res = await finishMockInterview(interview.value.id);
    if (res.code === 200 && res.data) {
      interview.value = res.data;
      stage.value = 'result';
      // 结束后刷新画像与历史（薄弱点已根据本次表现重算）
      loadProfile(true);
      loadHistory();
    } else {
      toast.error(res.message || '结束失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '结束失败，请稍后重试');
  } finally {
    finishing.value = false;
  }
}

function restartInterview() {
  interview.value = null;
  currentIdx.value = 0;
  answer.value = '';
  stage.value = 'start';
  // 回到开始页时刷新画像（薄弱点可能已更新）
  loadProfile(true);
}

function viewHistoryDetail(item: MockInterviewVO) {
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
      toast.error(res.message || '加载失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '加载失败');
  }
}

function goBack() {
  if (stage.value !== 'start') {
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

function failRateColor(rate?: number): string {
  if (rate == null) return 'var(--theme-text-secondary)';
  if (rate >= 0.8) return '#ef4444';
  if (rate >= 0.6) return '#f97316';
  return '#f59e0b';
}

function failRateText(rate?: number): string {
  if (rate == null) return '0%';
  return Math.round(rate * 100) + '%';
}

function formatTime(t?: string): string {
  if (!t) return '';
  return t.replace('T', ' ').slice(0, 16);
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
      </div>
    </div>

    <!-- ============ 开始页 ============ -->
    <template v-if="stage === 'start'">
      <div class="flex-1 py-8">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- 左侧：配置卡片 -->
          <div class="lg:col-span-2 space-y-6">
            <div
              class="rounded-xl border p-6"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <h2 class="text-base font-semibold mb-1 flex items-center" style="color: var(--theme-text);">
                <MessageSquare class="w-4 h-4 mr-2" />面试配置
              </h2>
              <p class="text-xs mb-4" style="color: var(--theme-text-secondary);">选择岗位与场景，系统抽取 5 道题，作答后即时规则评分</p>

              <!-- 岗位 -->
              <div class="mb-5">
                <label class="block text-sm mb-2" style="color: var(--theme-text);">面试岗位</label>
                <input
                  v-model="position"
                  type="text"
                  list="mock-position-options"
                  class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                  style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                  placeholder="如 Java后端工程师（可留空，建议从下方选择）"
                />
                <datalist id="mock-position-options">
                  <option v-for="p in positions" :key="p.id" :value="p.name" />
                </datalist>
                <div class="flex flex-wrap gap-1.5 mt-2">
                  <button
                    v-for="p in positions"
                    :key="p.id"
                    @click="position = p.name"
                    class="px-2.5 py-1 rounded-full text-xs transition"
                    :style="position === p.name
                      ? { backgroundColor: 'var(--theme-primary)', color: '#fff' }
                      : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
                    :title="p.description"
                  >{{ p.name }}</button>
                  <span
                    v-if="!positionsLoading && positions.length === 0"
                    class="text-xs"
                    style="color: var(--theme-text-secondary);"
                  >岗位字典加载中或为空，可自由输入</span>
                </div>
                <!-- 选中岗位的必备技能展示（增强画像可感知度） -->
                <div
                  v-if="selectedPositionSkills.length > 0"
                  class="mt-2 rounded-lg p-2.5"
                  style="background-color: color-mix(in srgb, var(--theme-primary) 6%, var(--theme-bg));"
                >
                  <div class="text-xs mb-1.5 flex items-center" style="color: var(--theme-text-secondary);">
                    <Zap class="w-3 h-3 mr-1" />
                    {{ selectedPosition?.name }} 必备技能
                    <span v-if="selectedPosition?.level" class="ml-1 opacity-70">· {{ selectedPosition.level }}</span>
                  </div>
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="skill in selectedPositionSkills"
                      :key="skill"
                      class="text-xs px-2 py-0.5 rounded-full"
                      style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
                    >{{ skill }}</span>
                  </div>
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

              <!-- 画像开关 -->
              <div
                class="rounded-lg p-4 mb-5"
                :style="personalized
                  ? { backgroundColor: 'color-mix(in srgb, var(--theme-primary) 8%, var(--theme-bg))', border: '1px solid color-mix(in srgb, var(--theme-primary) 30%, var(--theme-border))' }
                  : { backgroundColor: 'var(--theme-bg)', border: '1px solid var(--theme-border)' }"
              >
                <label class="flex items-center justify-between cursor-pointer">
                  <div class="flex items-center">
                    <Brain class="w-4 h-4 mr-2" :style="{ color: personalized ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }" />
                    <div>
                      <div class="text-sm font-medium" style="color: var(--theme-text);">基于我的画像出题</div>
                      <div class="text-xs mt-0.5" style="color: var(--theme-text-secondary);">
                        优先抽取你的薄弱点 + 岗位必备技能相关题目
                      </div>
                    </div>
                  </div>
                  <input
                    v-model="personalized"
                    type="checkbox"
                    class="sr-only"
                  />
                  <span
                    class="relative inline-block w-10 h-5 rounded-full transition"
                    :style="{ backgroundColor: personalized ? 'var(--theme-primary)' : 'var(--theme-border)' }"
                  >
                    <span
                      class="absolute top-0.5 w-4 h-4 rounded-full bg-white transition-all"
                      :style="{ left: personalized ? '22px' : '2px' }"
                    ></span>
                  </span>
                </label>
                <p
                  v-if="personalized && profileLoaded && !profileAvailable"
                  class="text-xs mt-2 flex items-center"
                  style="color: #f59e0b;"
                >
                  <Zap class="w-3 h-3 mr-1" />
                  你暂未积累足够答题数据，将自动降级为随机抽题
                </p>
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
                将从题库抽取 5 道题，作答后即时给出规则评分与反馈
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
                    <span class="text-sm font-medium flex items-center" style="color: var(--theme-text);">
                      {{ item.position || '未指定岗位' }} · {{ item.scene || '综合' }}
                      <span
                        v-if="item.isPersonalized === 1"
                        class="ml-2 inline-flex items-center text-xs px-1.5 py-0.5 rounded"
                        style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
                      >
                        <Brain class="w-3 h-3 mr-0.5" />画像
                      </span>
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

          <!-- 右侧：用户画像 -->
          <div class="lg:col-span-1">
            <div
              class="rounded-xl border p-5 sticky top-20"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-sm font-semibold flex items-center" style="color: var(--theme-text);">
                  <Target class="w-4 h-4 mr-1.5" :style="{ color: 'var(--theme-primary)' }" />我的画像
                </h3>
                <button
                  @click="loadProfile(true)"
                  :disabled="profileLoading"
                  class="text-xs flex items-center transition hover:opacity-80 disabled:opacity-50"
                  style="color: var(--theme-text-secondary);"
                >
                  <RefreshCw class="w-3 h-3 mr-1" :class="profileLoading ? 'animate-spin' : ''" />
                  刷新
                </button>
              </div>

              <!-- 加载态 -->
              <div v-if="profileLoading && !profile" class="space-y-3">
                <div v-for="i in 3" :key="i" class="h-4 rounded animate-pulse" style="background-color: var(--theme-bg);"></div>
              </div>

              <!-- 画像内容 -->
              <template v-else-if="profile">
                <!-- 统计 -->
                <div class="grid grid-cols-2 gap-2 mb-4">
                  <div class="rounded-lg p-2.5 text-center" style="background-color: var(--theme-bg);">
                    <div class="text-lg font-bold" style="color: var(--theme-primary);">
                      {{ profile.mockInterviewCount ?? 0 }}
                    </div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">面试次数</div>
                  </div>
                  <div class="rounded-lg p-2.5 text-center" style="background-color: var(--theme-bg);">
                    <div class="text-lg font-bold" :style="{ color: scoreColor(profile.avgMockScore ?? undefined) }">
                      {{ profile.avgMockScore ?? '-' }}
                    </div>
                    <div class="text-xs" style="color: var(--theme-text-secondary);">平均分</div>
                  </div>
                </div>

                <!-- 薄弱知识点 -->
                <div class="mb-4">
                  <div class="text-xs font-medium mb-2 flex items-center justify-between" style="color: var(--theme-text-secondary);">
                    <span>薄弱知识点</span>
                    <span v-if="sortedWeakTags.length > 0">Top {{ sortedWeakTags.length }}</span>
                  </div>
                  <div v-if="sortedWeakTags.length > 0" class="space-y-1.5">
                    <div
                      v-for="tag in sortedWeakTags"
                      :key="tag.tagId"
                      class="flex items-center justify-between px-2.5 py-1.5 rounded"
                      style="background-color: var(--theme-bg);"
                    >
                      <span class="text-xs font-medium truncate" style="color: var(--theme-text);">{{ tag.tagName }}</span>
                      <span class="text-xs ml-2 flex-shrink-0" :style="{ color: failRateColor(tag.failRate) }">
                        {{ tag.solved }}/{{ tag.total }} · {{ failRateText(tag.failRate) }}
                      </span>
                    </div>
                  </div>
                  <div v-else class="text-xs py-2 text-center rounded" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
                    暂无数据，多答题可积累
                  </div>
                </div>

                <!-- 岗位必备技能 -->
                <div v-if="profile.requiredSkills && profile.requiredSkills.length > 0">
                  <div class="text-xs font-medium mb-2" style="color: var(--theme-text-secondary);">
                    岗位必备技能
                    <span v-if="profile.position"> · {{ profile.position }}</span>
                  </div>
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="skill in profile.requiredSkills"
                      :key="skill"
                      class="text-xs px-2 py-0.5 rounded-full"
                      style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
                    >{{ skill }}</span>
                  </div>
                </div>
              </template>

              <!-- 加载失败 -->
              <div v-else class="text-center py-6">
                <Brain class="w-8 h-8 mx-auto mb-2" style="color: var(--theme-text-secondary); opacity: 0.4;" />
                <p class="text-xs mb-2" style="color: var(--theme-text-secondary);">画像加载失败</p>
                <button @click="loadProfile(true)" class="text-xs" style="color: var(--theme-primary);">重试</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 答题页 ============ -->
    <template v-else-if="stage === 'answering' && interview">
      <!-- 进度条 -->
      <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium flex items-center" style="color: var(--theme-text);">
              第 {{ currentIdx + 1 }} / {{ totalQa }} 题
              <span
                v-if="isPersonalizedInterview"
                class="ml-2 inline-flex items-center text-xs px-1.5 py-0.5 rounded"
                style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
              >
                <Brain class="w-3 h-3 mr-0.5" />画像驱动
              </span>
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
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
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
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
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
            <p class="text-sm mb-2 flex items-center justify-center gap-2" style="color: var(--theme-text-secondary);">
              {{ interview.position || '未指定岗位' }} · {{ interview.scene || '综合' }} · 共 {{ interview.totalQa }} 题 · 答完 {{ interview.answeredCount }} 题
              <span
                v-if="isPersonalizedInterview"
                class="inline-flex items-center text-xs px-1.5 py-0.5 rounded"
                style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
              >
                <Brain class="w-3 h-3 mr-0.5" />画像驱动
              </span>
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
