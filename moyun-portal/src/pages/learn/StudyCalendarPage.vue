<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useHead } from '@vueuse/head';
import { Calendar as CalendarIcon, ChevronLeft, Loader2, RefreshCw } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { getLearnCalendar } from '@/api/learnStats';
import type { LearnCalendarCell } from '@/api/learnStats';
import StudyCalendarCard from './StudyCalendarCard.vue';

useHead(computed(() => generateSeo({
  title: '刷题日历',
  description: '墨韵智库刷题日历热力图 - 一眼回顾过去一年的刷题足迹与坚持。',
  canonicalPath: '/learn/calendar',
})));

// ==================== 状态 ====================
const loading = ref(true);
const error = ref<string | null>(null);
const cells = ref<LearnCalendarCell[]>([]);

const now = new Date();
const currentYear = now.getFullYear();
// 可选年份：当前年、前两年
const yearOptions = [currentYear, currentYear - 1, currentYear - 2];
const selectedYear = ref<number>(currentYear);

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '刷题日历' },
]);

async function loadCalendar() {
  loading.value = true;
  error.value = null;
  try {
    const res = await getLearnCalendar(selectedYear.value);
    if (res.code === 200) {
      cells.value = res.data || [];
    } else {
      error.value = res.message || '加载日历数据失败';
    }
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载日历数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

watch(selectedYear, loadCalendar);
onMounted(loadCalendar);

// ==================== 汇总 ====================
const summary = computed(() => {
  let count = 0;
  let success = 0;
  let activeDays = 0;
  cells.value.forEach((c) => {
    count += c.count || 0;
    success += c.successCount || 0;
    if ((c.count || 0) > 0) activeDays++;
  });
  // 计算最长连续打卡天数
  const sorted = [...cells.value].sort((a, b) => a.date.localeCompare(b.date));
  let maxStreak = 0;
  let cur = 0;
  let prev: string | null = null;
  sorted.forEach((c) => {
    if ((c.count || 0) > 0) {
      if (prev) {
        const prevDate = new Date(prev);
        const thisDate = new Date(c.date);
        const diff = Math.round((thisDate.getTime() - prevDate.getTime()) / 86400000);
        cur = diff === 1 ? cur + 1 : 1;
      } else {
        cur = 1;
      }
      if (cur > maxStreak) maxStreak = cur;
      prev = c.date;
    } else {
      cur = 0;
      prev = null;
    }
  });
  const passRate = count > 0 ? Math.round((success / count) * 100) : 0;
  return { count, success, activeDays, maxStreak, passRate };
});
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

    <!-- 顶部条 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center gap-3 h-16">
          <router-link to="/learn" class="p-2 rounded-lg transition-colors hover:bg-gray-100" style="color: var(--theme-text-secondary);">
            <ChevronLeft class="w-5 h-5" />
          </router-link>
          <h1 class="text-lg sm:text-xl font-semibold flex items-center gap-2" style="color: var(--theme-text);">
            <CalendarIcon class="w-5 h-5" style="color: var(--theme-primary);" />
            刷题日历
          </h1>
          <!-- 年份选择 -->
          <div class="ml-auto flex items-center gap-2">
            <select
              v-model="selectedYear"
              class="rounded-lg border px-3 py-1.5 text-sm focus:outline-none focus:ring-2"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            >
              <option v-for="y in yearOptions" :key="y" :value="y">{{ y }} 年</option>
            </select>
            <button
              @click="loadCalendar"
              class="p-1.5 rounded-lg transition-colors hover:bg-gray-100"
              style="color: var(--theme-text-secondary);"
              title="刷新"
            >
              <RefreshCw class="w-4 h-4" :class="{ 'animate-spin': loading }" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        <!-- 加载中 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20" style="color: var(--theme-text-secondary);">
          <Loader2 class="w-8 h-8 animate-spin mb-3" />
          <span>正在加载刷题日历...</span>
        </div>

        <!-- 错误 -->
        <div v-else-if="error" class="rounded-lg p-6 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
          {{ error }}
          <button @click="loadCalendar" class="ml-3 underline">重试</button>
        </div>

        <template v-else>
          <!-- 汇总卡片 -->
          <section class="grid grid-cols-2 md:grid-cols-4 gap-3">
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">总提交</div>
              <div class="text-2xl font-bold mt-1" style="color: var(--theme-text);">{{ summary.count }}</div>
            </div>
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">通过次数</div>
              <div class="text-2xl font-bold mt-1" style="color: #10b981;">{{ summary.success }}</div>
            </div>
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">活跃天数</div>
              <div class="text-2xl font-bold mt-1" style="color: var(--theme-text);">{{ summary.activeDays }}</div>
            </div>
            <div class="rounded-lg p-4" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <div class="text-xs" style="color: var(--theme-text-secondary);">最长连续打卡</div>
              <div class="text-2xl font-bold mt-1" style="color: #f59e0b;">{{ summary.maxStreak }} 天</div>
            </div>
          </section>

          <!-- 日历热力图 -->
          <section class="rounded-lg p-4 sm:p-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <h2 class="font-semibold mb-4" style="color: var(--theme-text);">刷题热力图（近 365 天）</h2>
            <StudyCalendarCard :cells="cells" :loading="false" />
          </section>

          <!-- 空状态提示 -->
          <section v-if="cells.length === 0" class="rounded-lg p-8 text-center" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <p class="text-sm" style="color: var(--theme-text-secondary);">
              {{ selectedYear }} 年还没有刷题记录，去
              <router-link to="/interview/questions" class="underline" style="color: var(--theme-primary);">刷第一道题</router-link>
              开启你的打卡之旅吧。
            </p>
          </section>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
