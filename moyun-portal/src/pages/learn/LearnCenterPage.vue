<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  BookOpen, Target, AlertCircle, Flame, CheckCircle2,
  Calendar, ChevronRight, Loader2, LogIn, TrendingUp,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { useUserStore } from '@/stores/user';
import { getLearnDashboard } from '@/api/learn';
import type { LearnDashboard, StudyPlanVO, WrongQuestionVO } from '@/api/learn';

const router = useRouter();
const userStore = useUserStore();

const loading = ref(true);
const error = ref<string | null>(null);
const dashboard = ref<LearnDashboard | null>(null);

useHead(computed(() => generateSeo({
  title: '学习中心',
  description: '墨韵智库学习中心 - 今日计划进度、连续打卡、错题本入口、学习统计一站式聚合',
  keywords: ['学习中心', '刷题', '错题本', '学习计划', '打卡', '墨韵'],
  canonicalPath: '/learn',
})));

onMounted(async () => {
  await loadDashboard();
});

async function loadDashboard() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getLearnDashboard();
    if (res.code === 200 && res.data) {
      dashboard.value = res.data;
    } else {
      error.value = res.message || '加载学习中心数据失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载学习中心数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

const isLoggedIn = computed(() => !!dashboard.value?.loggedIn);
const greeting = computed(() => {
  if (!isLoggedIn.value) return '欢迎来到学习中心';
  const name = dashboard.value?.nickname || userStore.nickname;
  return name ? `${name}，继续加油` : '继续加油';
});

const activePlans = computed<StudyPlanVO[]>(() => dashboard.value?.activePlans || []);
const recentWrong = computed<WrongQuestionVO[]>(() => dashboard.value?.recentWrongQuestions || []);

const breadcrumbs = computed(() => [{ label: '学习中心' }]);

function goPlan() {
  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: '/learn/plan' } });
    return;
  }
  router.push('/learn/plan');
}

function goWrong() {
  if (!isLoggedIn.value) {
    router.push({ name: 'login', query: { redirect: '/learn/wrong' } });
    return;
  }
  router.push('/learn/wrong');
}

function goQuestion(id: number) {
  router.push(`/interview/question/${id}`);
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: '/learn' } });
}

function planTypeText(t: string | null) {
  const map: Record<string, string> = {
    daily_question: '每日刷题',
    weekly_reading: '每周阅读',
    custom: '自定义',
  };
  return (t && map[t]) || '学习计划';
}

function statusText(s: string) {
  const map: Record<string, string> = { active: '进行中', completed: '已完成', abandoned: '已放弃' };
  return map[s] || s;
}

function difficultyText(d: string | null) {
  const map: Record<string, string> = { easy: '简单', medium: '中等', hard: '困难' };
  return (d && map[d]) || '未分级';
}

function difficultyColor(d: string | null) {
  if (d === 'easy') return 'text-emerald-600';
  if (d === 'medium') return 'text-amber-600';
  if (d === 'hard') return 'text-rose-600';
  return 'text-slate-500';
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2"></div>
      </div>
    </div>

    <!-- Hero 区 -->
    <div class="py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 25% 25%, rgba(20, 184, 166, 0.4) 0%, transparent 50%), radial-gradient(circle at 75% 75%, rgba(59, 130, 246, 0.4) 0%, transparent 50%), linear-gradient(135deg, #0f766e 0%, #1e40af 100%);">
      <div class="absolute inset-0 opacity-10 pointer-events-none" aria-hidden="true">
        <svg class="absolute top-6 left-8 w-32 h-32 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M12 3L1 9l4 2.18v6L12 21l7-3.82v-6l2-1.09V17h2V9L12 3zm6.82 6L12 12.72 5.18 9 12 5.28 18.82 9zM17 15.99l-5 2.73-5-2.73v-3.72L12 15l5-2.73v3.72z"/></svg>
        <svg class="absolute bottom-4 right-10 w-40 h-40 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"/></svg>
      </div>
      <div class="relative px-6 py-8 sm:px-10 sm:py-10">
        <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-3 py-1 rounded-full text-sm mb-4">
          <BookOpen class="w-4 h-4 mr-2" /> 学习者成长闭环
        </div>
        <h1 class="text-3xl md:text-4xl font-bold tracking-tight mb-2">{{ greeting }}</h1>
        <p class="text-base text-white/90 max-w-2xl">
          刷题有计划、答题有反馈、学习有同伴。一站式管理今日计划、连续打卡与错题复习。
        </p>
        <div v-if="!isLoggedIn" class="mt-5">
          <button
            @click="goLogin"
            class="inline-flex items-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
            style="background-color: rgba(255,255,255,0.2);"
          >
            <LogIn class="w-4 h-4 mr-1.5" />
            登录查看我的学习数据
          </button>
        </div>
      </div>
        </div>
      </div>
    </div>

    <main class="flex-1 max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 py-8">
      <!-- 加载态 -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-20">
        <Loader2 class="w-8 h-8 animate-spin" style="color: var(--theme-primary);" />
        <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error" class="flex flex-col items-center justify-center py-20">
        <AlertCircle class="w-10 h-10" style="color: var(--theme-danger, #ef4444);" />
        <p class="mt-3 text-sm" style="color: var(--theme-text-secondary);">{{ error }}</p>
        <button
          @click="loadDashboard"
          class="mt-4 px-4 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
          style="background-color: var(--theme-primary);"
        >
          重新加载
        </button>
      </div>

      <template v-else-if="dashboard">
        <!-- 统计卡片 -->
        <section class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
          <div
            class="rounded-xl p-5 border shadow-sm"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs" style="color: var(--theme-text-secondary);">累计答题</span>
              <TrendingUp class="w-4 h-4" style="color: var(--theme-primary);" />
            </div>
            <div class="text-2xl font-bold" style="color: var(--theme-text);">{{ dashboard.totalQuestionCount }}</div>
          </div>

          <div
            class="rounded-xl p-5 border shadow-sm"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs" style="color: var(--theme-text-secondary);">通过率</span>
              <CheckCircle2 class="w-4 h-4" style="color: #10b981;" />
            </div>
            <div class="text-2xl font-bold" style="color: var(--theme-text);">
              {{ dashboard.passRate }}<span class="text-base">%</span>
            </div>
            <div class="text-xs mt-1" style="color: var(--theme-text-secondary);">
              通过 {{ dashboard.successCount }} / {{ dashboard.totalQuestionCount }}
            </div>
          </div>

          <div
            class="rounded-xl p-5 border shadow-sm"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs" style="color: var(--theme-text-secondary);">连续打卡</span>
              <Flame class="w-4 h-4" style="color: #f59e0b;" />
            </div>
            <div class="text-2xl font-bold" style="color: var(--theme-text);">{{ dashboard.streakDays }}</div>
            <div class="text-xs mt-1" style="color: var(--theme-text-secondary);">天</div>
          </div>

          <div
            class="rounded-xl p-5 border shadow-sm cursor-pointer transition hover:shadow-md"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            @click="goWrong"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs" style="color: var(--theme-text-secondary);">错题本</span>
              <AlertCircle class="w-4 h-4" style="color: #ef4444;" />
            </div>
            <div class="text-2xl font-bold" style="color: var(--theme-text);">{{ dashboard.wrongCount }}</div>
            <div class="text-xs mt-1" style="color: var(--theme-primary);">
              今日待复习 {{ dashboard.todayReviewCount }} 道
            </div>
          </div>
        </section>

        <!-- 今日任务区 -->
        <section class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- 今日计划 -->
          <div
            class="lg:col-span-2 rounded-xl border shadow-sm"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <div
              class="px-5 py-4 border-b flex items-center justify-between"
              style="border-color: var(--theme-border);"
            >
              <div class="flex items-center">
                <Target class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
                <h2 class="text-base font-semibold" style="color: var(--theme-text);">进行中的学习计划</h2>
              </div>
              <button
                @click="goPlan"
                class="flex items-center text-sm transition hover:opacity-80"
                style="color: var(--theme-primary);"
              >
                查看全部 <ChevronRight class="w-4 h-4 ml-0.5" />
              </button>
            </div>
            <div class="p-5">
              <div v-if="!isLoggedIn" class="py-8 text-center">
                <Target class="w-10 h-10 mx-auto mb-3 opacity-40" style="color: var(--theme-text-secondary);" />
                <p class="text-sm" style="color: var(--theme-text-secondary);">
                  登录后查看你的学习计划
                </p>
                <button
                  @click="goLogin"
                  class="mt-3 px-4 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
                  style="background-color: var(--theme-primary);"
                >
                  立即登录
                </button>
              </div>
              <div v-else-if="activePlans.length === 0" class="py-8 text-center">
                <Target class="w-10 h-10 mx-auto mb-3 opacity-40" style="color: var(--theme-text-secondary);" />
                <p class="text-sm" style="color: var(--theme-text-secondary);">
                  还没有学习计划，开始创建第一个吧
                </p>
                <button
                  @click="goPlan"
                  class="mt-3 px-4 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
                  style="background-color: var(--theme-primary);"
                >
                  创建计划
                </button>
              </div>
              <ul v-else class="space-y-3">
                <li
                  v-for="plan in activePlans"
                  :key="plan.id"
                  class="rounded-lg border p-4 transition hover:shadow-sm cursor-pointer"
                  style="border-color: var(--theme-border); background-color: var(--theme-bg);"
                  @click="goPlan"
                >
                  <div class="flex items-center justify-between mb-2">
                    <div class="flex items-center min-w-0">
                      <span
                        class="inline-block px-2 py-0.5 rounded text-xs mr-2 flex-shrink-0"
                        style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
                      >
                        {{ planTypeText(plan.planType) }}
                      </span>
                      <span class="text-sm font-medium truncate" style="color: var(--theme-text);">{{ plan.title }}</span>
                    </div>
                    <span class="text-xs flex-shrink-0 ml-2" style="color: var(--theme-text-secondary);">
                      {{ statusText(plan.status) }}
                    </span>
                  </div>
                  <div class="flex items-center justify-between text-xs mb-2" style="color: var(--theme-text-secondary);">
                    <span>已完成 {{ plan.doneCount }} / {{ plan.targetCount || '∞' }}</span>
                    <span>今日 {{ plan.todayDoneCount }} 题 · 连续 {{ plan.streakDays }} 天</span>
                  </div>
                  <div class="h-1.5 rounded-full overflow-hidden" style="background-color: var(--theme-border);">
                    <div
                      class="h-full rounded-full transition-all"
                      :style="{ width: (plan.progressPercent || 0) + '%', backgroundColor: 'var(--theme-primary)' }"
                    ></div>
                  </div>
                </li>
              </ul>
            </div>
          </div>

          <!-- 今日数据 + 错题入口 -->
          <div class="space-y-6">
            <!-- 今日完成 -->
            <div
              class="rounded-xl border shadow-sm p-5"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <div class="flex items-center mb-3">
                <Calendar class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
                <h2 class="text-base font-semibold" style="color: var(--theme-text);">今日任务</h2>
              </div>
              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text-secondary);">今日答题</span>
                  <span class="text-sm font-semibold" style="color: var(--theme-text);">
                    {{ dashboard.todayDoneCount }} 道
                  </span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text-secondary);">进行中计划</span>
                  <span class="text-sm font-semibold" style="color: var(--theme-text);">
                    {{ dashboard.activePlanCount }} 个
                  </span>
                </div>
                <div class="flex items-center justify-between">
                  <span class="text-sm" style="color: var(--theme-text-secondary);">待复习错题</span>
                  <span class="text-sm font-semibold" style="color: #ef4444;">
                    {{ dashboard.todayReviewCount }} 道
                  </span>
                </div>
              </div>
            </div>

            <!-- 错题本入口 -->
            <div
              class="rounded-xl border shadow-sm p-5 cursor-pointer transition hover:shadow-md"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
              @click="goWrong"
            >
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center">
                  <AlertCircle class="w-5 h-5 mr-2" style="color: #ef4444;" />
                  <h2 class="text-base font-semibold" style="color: var(--theme-text);">错题本</h2>
                </div>
                <ChevronRight class="w-4 h-4" style="color: var(--theme-text-secondary);" />
              </div>
              <div v-if="!isLoggedIn" class="text-sm py-2" style="color: var(--theme-text-secondary);">
                登录后查看错题记录
              </div>
              <div v-else-if="recentWrong.length === 0" class="text-sm py-2" style="color: var(--theme-text-secondary);">
                暂无错题，继续保持！
              </div>
              <ul v-else class="space-y-2">
                <li
                  v-for="wq in recentWrong"
                  :key="wq.id"
                  class="text-sm truncate cursor-pointer hover:underline"
                  style="color: var(--theme-text);"
                  @click.stop="goQuestion(wq.questionId)"
                >
                  · {{ wq.questionTitle || `题目 #${wq.questionId}` }}
                  <span class="ml-1 text-xs" style="color: var(--theme-text-secondary);">
                    (错 {{ wq.wrongCount }} 次)
                  </span>
                </li>
              </ul>
            </div>
          </div>
        </section>
      </template>
    </main>

    <SiteFooter />
  </div>
</template>
