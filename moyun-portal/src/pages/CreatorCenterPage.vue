<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, BarChart3, Calendar as CalendarIcon, Users,
  Eye, Heart, Bookmark, UserPlus, MapPin, Clock, Loader2,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import {
  getCreatorDashboard,
  getCreatorCalendar,
  getReaderProfile,
} from '@/api/creator';
import type {
  CreatorDashboard,
  CalendarCell,
  ReaderProfile,
} from '@/api/creator';

useHead(computed(() => generateSeo({
  title: '创作者中心',
  description: '墨韵智库创作者中心 - 数据看板、创作日历与读者画像',
  canonicalPath: '/creator',
})));

// ==================== 状态 ====================
const loading = ref(true);
const error = ref<string | null>(null);

const dashboard = ref<CreatorDashboard | null>(null);
const calendar = ref<CalendarCell[]>([]);
const readerProfile = ref<ReaderProfile | null>(null);

onMounted(async () => {
  loading.value = true;
  error.value = null;
  try {
    const [dashRes, calRes, readerRes] = await Promise.all([
      getCreatorDashboard(),
      getCreatorCalendar(),
      getReaderProfile(),
    ]);
    if (dashRes.code === 200) dashboard.value = dashRes.data;
    if (calRes.code === 200) calendar.value = calRes.data || [];
    if (readerRes.code === 200) readerProfile.value = readerRes.data;
  } catch (err) {
    const e = err as { message?: string };
    error.value = e?.message || '加载创作者数据失败，请稍后重试';
  } finally {
    loading.value = false;
  }
});

// ==================== 数据看板：折线图（SVG） ====================
// 各系列汇总
const totals = computed(() => {
  const d = dashboard.value;
  if (!d) return { views: 0, likes: 0, bookmarks: 0, followers: 0 };
  const sum = (arr: number[] | undefined) => (arr || []).reduce((a, b) => a + b, 0);
  return {
    views: sum(d.views),
    likes: sum(d.likes),
    bookmarks: sum(d.bookmarks),
    followers: sum(d.followers),
  };
});

// SVG 画布参数
const CHART_W = 760;
const CHART_H = 240;
const PAD_L = 40;
const PAD_R = 16;
const PAD_T = 16;
const PAD_B = 28;

const chartMax = computed(() => {
  const d = dashboard.value;
  if (!d) return 10;
  const all = [
    ...(d.views || []),
    ...(d.likes || []),
    ...(d.bookmarks || []),
    ...(d.followers || []),
  ];
  const m = Math.max(1, ...all);
  // 向上取整到 5 的倍数，避免顶点贴边
  return Math.max(5, Math.ceil(m / 5) * 5);
});

interface Series {
  key: string;
  label: string;
  color: string;
  values: number[];
}

const series = computed<Series[]>(() => {
  const d = dashboard.value;
  if (!d) return [];
  return [
    { key: 'views', label: '阅读', color: 'var(--theme-primary)', values: d.views || [] },
    { key: 'likes', label: '点赞', color: '#ef4444', values: d.likes || [] },
    { key: 'bookmarks', label: '收藏', color: '#f59e0b', values: d.bookmarks || [] },
    { key: 'followers', label: '新增粉丝', color: '#10b981', values: d.followers || [] },
  ];
});

const xCount = computed(() => dashboard.value?.dates?.length || 30);

function xCoord(i: number): number {
  const innerW = CHART_W - PAD_L - PAD_R;
  const n = Math.max(1, xCount.value - 1);
  return PAD_L + (innerW * i) / n;
}

function yCoord(v: number): number {
  const innerH = CHART_H - PAD_T - PAD_B;
  const max = chartMax.value || 1;
  return PAD_T + innerH - (innerH * v) / max;
}

function buildPath(values: number[]): string {
  if (!values.length) return '';
  return values
    .map((v, i) => `${i === 0 ? 'M' : 'L'} ${xCoord(i).toFixed(1)} ${yCoord(v).toFixed(1)}`)
    .join(' ');
}

// Y 轴刻度（5 档）
const yTicks = computed(() => {
  const max = chartMax.value;
  return [0, max / 4, max / 2, (max * 3) / 4, max].map((v) => Math.round(v));
});

// X 轴标签（每 5 天一个，避免拥挤）
const xLabels = computed(() => {
  const dates = dashboard.value?.dates || [];
  const labels: { x: number; text: string }[] = [];
  dates.forEach((date, i) => {
    if (i % 5 === 0 || i === dates.length - 1) {
      labels.push({ x: xCoord(i), text: date.slice(5) });
    }
  });
  return labels;
});

// ==================== 创作日历热力图 ====================
// 近 365 天日期序列，按周列对齐（GitHub 风格）
interface HeatCell {
  date: string;
  count: number;
  month: number;
}

const heatColumns = computed<HeatCell[][]>(() => {
  const countMap = new Map<string, number>();
  calendar.value.forEach((c) => countMap.set(c.date, c.count));

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  // 从今天往前回退到一年前的"周日"作为第一列起点，使每列为一周
  const start = new Date(today);
  start.setDate(start.getDate() - 364);
  // 对齐到周日（getDay() 0=周日）
  const dayOfWeek = start.getDay();
  start.setDate(start.getDate() - dayOfWeek);

  const columns: HeatCell[][] = [];
  const cursor = new Date(start);
  // 共生成足够覆盖一整年的列数（最多 53 列）
  while (cursor <= today) {
    const col: HeatCell[] = [];
    for (let d = 0; d < 7; d++) {
      const dateStr = toDateStr(cursor);
      if (cursor > today) {
        // 未来日期占位，count 为 -1 表示不渲染
        col.push({ date: dateStr, count: -1, month: cursor.getMonth() });
      } else {
        col.push({
          date: dateStr,
          count: countMap.get(dateStr) || 0,
          month: cursor.getMonth(),
        });
      }
      cursor.setDate(cursor.getDate() + 1);
    }
    columns.push(col);
  }
  return columns;
});

const heatMax = computed(() => {
  let m = 0;
  heatColumns.value.forEach((col) => col.forEach((c) => { if (c.count > m) m = c.count; }));
  return m;
});

function heatColor(count: number): string {
  if (count < 0) return 'transparent';
  if (count === 0) return 'var(--theme-border)';
  const max = Math.max(1, heatMax.value);
  const ratio = Math.min(1, count / max);
  // 4 档绿色深浅，类似 GitHub
  if (ratio <= 0.25) return '#9be9a8';
  if (ratio <= 0.5) return '#40c463';
  if (ratio <= 0.75) return '#30a14e';
  return '#216e39';
}

// 月份标签（每列首日所属月份）
const monthLabels = computed(() => {
  const labels: { x: number; text: string }[] = [];
  heatColumns.value.forEach((col, idx) => {
    const first = col.find((c) => c.count >= 0);
    if (!first) return;
    const prevMonth = idx > 0
      ? heatColumns.value[idx - 1].find((c) => c.count >= 0)?.month
      : undefined;
    if (first.month !== prevMonth) {
      labels.push({ x: idx, text: `${first.month + 1}月` });
    }
  });
  return labels;
});

const totalContributions = computed(() =>
  calendar.value.reduce((sum, c) => sum + c.count, 0)
);

// ==================== 读者画像 ====================
const maxRegionValue = computed(() => {
  const regions = readerProfile.value?.regions || [];
  return Math.max(1, ...regions.map((r) => r.value));
});

const maxHourValue = computed(() => {
  const hours = readerProfile.value?.hours || [];
  return Math.max(1, ...hours.map((h) => h.value));
});

function toDateStr(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 页面顶部条 -->
    <div class="border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center gap-3 h-16">
          <router-link to="/user"
            class="p-2 rounded-lg transition-colors hover:bg-gray-100"
            style="color: var(--theme-text-secondary);"
          >
            <ArrowLeft class="w-5 h-5" />
          </router-link>
          <h1 class="text-lg sm:text-xl font-semibold flex items-center gap-2" style="color: var(--theme-text);">
            <BarChart3 class="w-5 h-5" style="color: var(--theme-primary);" />
            创作者中心
          </h1>
        </div>
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        <!-- 加载中 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20" style="color: var(--theme-text-secondary);">
          <Loader2 class="w-8 h-8 animate-spin mb-3" />
          <span>正在加载创作者数据...</span>
        </div>

        <!-- 错误 -->
        <div v-else-if="error" class="rounded-lg p-6 text-center"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);"
        >
          {{ error }}
        </div>

        <template v-else>
          <!-- ==================== 顶部：30 天数据趋势 ==================== -->
          <section class="rounded-lg p-4 sm:p-6"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
              <h2 class="font-semibold flex items-center gap-2" style="color: var(--theme-text);">
                <Eye class="w-5 h-5" style="color: var(--theme-primary);" />
                近 30 天数据趋势
              </h2>
              <!-- 汇总卡片 -->
              <div class="flex flex-wrap gap-3 text-sm">
                <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                  <Eye class="w-4 h-4" style="color: var(--theme-primary);" />
                  阅读 <span class="font-semibold" style="color: var(--theme-text);">{{ totals.views }}</span>
                </div>
                <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                  <Heart class="w-4 h-4 text-red-500" />
                  点赞 <span class="font-semibold" style="color: var(--theme-text);">{{ totals.likes }}</span>
                </div>
                <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                  <Bookmark class="w-4 h-4 text-amber-500" />
                  收藏 <span class="font-semibold" style="color: var(--theme-text);">{{ totals.bookmarks }}</span>
                </div>
                <div class="flex items-center gap-1.5" style="color: var(--theme-text-secondary);">
                  <UserPlus class="w-4 h-4 text-emerald-500" />
                  新增粉丝 <span class="font-semibold" style="color: var(--theme-text);">{{ totals.followers }}</span>
                </div>
              </div>
            </div>

            <!-- SVG 折线图 -->
            <div class="w-full overflow-x-auto">
              <svg :viewBox="`0 0 ${CHART_W} ${CHART_H}`" class="w-full h-auto" style="min-width: 560px;">
                <!-- Y 轴网格线 + 刻度 -->
                <g>
                  <line
                    v-for="(t, i) in yTicks" :key="`grid-${i}`"
                    :x1="PAD_L" :x2="CHART_W - PAD_R"
                    :y1="yCoord(t)" :y2="yCoord(t)"
                    stroke="var(--theme-border)" stroke-width="1" stroke-dasharray="3 3"
                  />
                  <text
                    v-for="(t, i) in yTicks" :key="`ytick-${i}`"
                    :x="PAD_L - 6" :y="yCoord(t) + 4"
                    text-anchor="end" font-size="10" fill="var(--theme-text-secondary)"
                  >{{ t }}</text>
                </g>
                <!-- 折线 -->
                <g>
                  <path
                    v-for="s in series" :key="`line-${s.key}`"
                    :d="buildPath(s.values)"
                    :stroke="s.color" stroke-width="2" fill="none"
                    stroke-linejoin="round" stroke-linecap="round"
                  />
                </g>
                <!-- X 轴标签 -->
                <g>
                  <text
                    v-for="(lbl, i) in xLabels" :key="`xlabel-${i}`"
                    :x="lbl.x" :y="CHART_H - 8"
                    text-anchor="middle" font-size="10" fill="var(--theme-text-secondary)"
                  >{{ lbl.text }}</text>
                </g>
              </svg>
            </div>

            <!-- 图例 -->
            <div class="flex flex-wrap items-center gap-4 mt-3 text-xs" style="color: var(--theme-text-secondary);">
              <div v-for="s in series" :key="`legend-${s.key}`" class="flex items-center gap-1.5">
                <span class="inline-block w-3 h-3 rounded-sm" :style="{ backgroundColor: s.color }"></span>
                {{ s.label }}
              </div>
            </div>
          </section>

          <!-- ==================== 中部：创作日历热力图 ==================== -->
          <section class="rounded-lg p-4 sm:p-6"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
              <h2 class="font-semibold flex items-center gap-2" style="color: var(--theme-text);">
                <CalendarIcon class="w-5 h-5" style="color: var(--theme-primary);" />
                创作日历
              </h2>
              <span class="text-sm" style="color: var(--theme-text-secondary);">
                近 1 年共 <span class="font-semibold" style="color: var(--theme-text);">{{ totalContributions }}</span> 次创作活动
              </span>
            </div>

            <div class="w-full overflow-x-auto">
              <div class="inline-block">
                <!-- 月份标签行 -->
                <div class="flex ml-7 mb-1">
                  <div
                    v-for="(lbl, i) in monthLabels" :key="`mlbl-${i}`"
                    class="text-[10px]" style="color: var(--theme-text-secondary); position: absolute;"
                    :style="{ marginLeft: (lbl.x * 14) + 'px' }"
                  >{{ lbl.text }}</div>
                </div>
                <div class="flex gap-1">
                  <!-- 星期标签 -->
                  <div class="flex flex-col gap-1 mr-1 text-[10px] pt-0.5" style="color: var(--theme-text-secondary);">
                    <div class="h-3 leading-3">&nbsp;</div>
                    <div class="h-3 leading-3">一</div>
                    <div class="h-3 leading-3">&nbsp;</div>
                    <div class="h-3 leading-3">三</div>
                    <div class="h-3 leading-3">&nbsp;</div>
                    <div class="h-3 leading-3">五</div>
                    <div class="h-3 leading-3">&nbsp;</div>
                  </div>
                  <!-- 热力方块 -->
                  <div class="flex gap-1">
                    <div v-for="(col, ci) in heatColumns" :key="`col-${ci}`" class="flex flex-col gap-1">
                      <div
                        v-for="(cell, ri) in col" :key="`cell-${ci}-${ri}`"
                        class="w-3 h-3 rounded-sm"
                        :style="{ backgroundColor: heatColor(cell.count) }"
                        :title="cell.count >= 0 ? `${cell.date}：${cell.count} 次` : ''"
                      ></div>
                    </div>
                  </div>
                </div>
                <!-- 图例 -->
                <div class="flex items-center gap-2 mt-3 ml-7 text-[10px]" style="color: var(--theme-text-secondary);">
                  <span>少</span>
                  <span class="w-3 h-3 rounded-sm" style="background-color: var(--theme-border);"></span>
                  <span class="w-3 h-3 rounded-sm" style="background-color: #9be9a8;"></span>
                  <span class="w-3 h-3 rounded-sm" style="background-color: #40c463;"></span>
                  <span class="w-3 h-3 rounded-sm" style="background-color: #30a14e;"></span>
                  <span class="w-3 h-3 rounded-sm" style="background-color: #216e39;"></span>
                  <span>多</span>
                </div>
              </div>
            </div>
          </section>

          <!-- ==================== 底部：读者画像 ==================== -->
          <section class="rounded-lg p-4 sm:p-6"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          >
            <h2 class="font-semibold flex items-center gap-2 mb-4" style="color: var(--theme-text);">
              <Users class="w-5 h-5" style="color: var(--theme-primary);" />
              读者画像（近 30 天）
            </h2>

            <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <!-- 地域分布 Top10 -->
              <div>
                <h3 class="text-sm font-medium flex items-center gap-2 mb-3" style="color: var(--theme-text-secondary);">
                  <MapPin class="w-4 h-4" />
                  地域分布 Top10
                </h3>
                <div v-if="(readerProfile?.regions || []).length" class="space-y-2">
                  <div v-for="(r, i) in readerProfile?.regions" :key="`region-${i}`" class="flex items-center gap-2">
                    <span class="text-xs w-28 truncate" :title="r.region" style="color: var(--theme-text-secondary);">{{ r.region }}</span>
                    <div class="flex-1 h-4 rounded overflow-hidden" style="background-color: var(--theme-bg);">
                      <div
                        class="h-full rounded transition-all"
                        :style="{
                          width: ((r.value / maxRegionValue) * 100) + '%',
                          backgroundColor: 'var(--theme-primary)',
                        }"
                      ></div>
                    </div>
                    <span class="text-xs w-10 text-right font-medium" style="color: var(--theme-text);">{{ r.value }}</span>
                  </div>
                </div>
                <div v-else class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
                  暂无地域分布数据
                </div>
              </div>

              <!-- 时段分布 24 小时 -->
              <div>
                <h3 class="text-sm font-medium flex items-center gap-2 mb-3" style="color: var(--theme-text-secondary);">
                  <Clock class="w-4 h-4" />
                  时段分布（0-23 时）
                </h3>
                <div v-if="(readerProfile?.hours || []).length" class="flex items-end gap-1 h-40">
                  <div
                    v-for="h in readerProfile?.hours" :key="`hour-${h.hour}`"
                    class="flex-1 flex flex-col items-center justify-end group"
                  >
                    <div
                      class="w-full rounded-t transition-all"
                      :style="{
                        height: ((h.value / maxHourValue) * 100) + '%',
                        minHeight: h.value > 0 ? '4px' : '1px',
                        backgroundColor: 'var(--theme-primary)',
                        opacity: h.value > 0 ? 1 : 0.25,
                      }"
                      :title="`${h.hour}时：${h.value} 次`"
                    ></div>
                  </div>
                </div>
                <div v-else class="text-sm py-8 text-center" style="color: var(--theme-text-secondary);">
                  暂无时段分布数据
                </div>
                <div class="flex justify-between mt-1 text-[10px]" style="color: var(--theme-text-secondary);">
                  <span>0时</span>
                  <span>6时</span>
                  <span>12时</span>
                  <span>18时</span>
                  <span>23时</span>
                </div>
              </div>
            </div>
          </section>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
