<script setup lang="ts">
import { computed } from 'vue';
import type { LearnCalendarCell } from '@/api/learnStats';

/**
 * 刷题日历热力图卡片（纯 SVG，无外部图表库）
 * 类似 GitHub 贡献图：按周列对齐，颜色深浅表示当日刷题量。
 *
 * <p>渲染的是 **year 指定的自然年**（1/1 ~ 12/31，两端按周对齐补齐）。
 * 此前无论选哪一年都渲染"今天往前 365 天"的固定窗口 ⇒ 选 2024 时统计是 2024、
 * 图形却是近一年，标题还写"近 1 年"，三者互相矛盾。</p>
 */
const props = withDefaults(defineProps<{
  /** 日历数据，每项 { date, count, successCount }（与 year 对应） */
  cells: LearnCalendarCell[];
  /** 要渲染的年份（该年 1/1 ~ 12/31） */
  year: number;
  /** 是否加载中 */
  loading?: boolean;
}>(), {
  loading: false,
});

// ==================== 几何参数 ====================
const CELL = 11;        // 单元格边长
const GAP = 2;          // 单元格间距
const STEP = CELL + GAP; // 每格步长 13
const PAD_LEFT = 28;    // 左侧星期标签宽度
const PAD_TOP = 18;     // 顶部月份标签高度

// ==================== 日期 → 字符串 ====================
function toDateStr(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

// ==================== 构建热力列（一年，按周对齐） ====================
interface HeatCell {
  date: string;
  count: number;
  successCount: number;
  month: number;
  /** 是否属于未来日期（当前年份里今天之后） */
  inFuture: boolean;
  /** 是否落在所选年份之外（首尾按周补齐产生的相邻年日期，仅占位不渲染） */
  outside: boolean;
}

const heatColumns = computed<HeatCell[][]>(() => {
  const countMap = new Map<string, LearnCalendarCell>();
  (props.cells || []).forEach((c) => countMap.set(c.date, c));

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // 所选年份 1/1 → 12/31；首尾各自对齐到周日/周六，保证每列是完整一周
  const start = new Date(props.year, 0, 1);
  start.setDate(start.getDate() - start.getDay());
  const end = new Date(props.year, 11, 31);
  end.setDate(end.getDate() + (6 - end.getDay()));

  const columns: HeatCell[][] = [];
  const cursor = new Date(start);
  while (cursor <= end) {
    const col: HeatCell[] = [];
    for (let d = 0; d < 7; d++) {
      const dateStr = toDateStr(cursor);
      const inFuture = cursor > today;
      const outside = cursor.getFullYear() !== props.year;
      const hit = countMap.get(dateStr);
      col.push({
        date: dateStr,
        count: (inFuture || outside) ? 0 : (hit?.count ?? 0),
        successCount: (inFuture || outside) ? 0 : (hit?.successCount ?? 0),
        month: cursor.getMonth(),
        inFuture,
        outside,
      });
      cursor.setDate(cursor.getDate() + 1);
    }
    columns.push(col);
  }
  return columns;
});

const svgWidth = computed(() => PAD_LEFT + heatColumns.value.length * STEP + 2);
const svgHeight = PAD_TOP + 7 * STEP + 2;

// ==================== 颜色与统计 ====================
const heatMax = computed(() => {
  let m = 0;
  heatColumns.value.forEach((col) => col.forEach((c) => {
    if (!c.inFuture && !c.outside && c.count > m) m = c.count;
  }));
  return m;
});

function heatColor(count: number, inFuture: boolean, outside = false): string {
  if (inFuture || outside) return 'transparent';
  if (count <= 0) return 'var(--theme-border)';
  const max = Math.max(1, heatMax.value);
  const ratio = Math.min(1, count / max);
  if (ratio <= 0.25) return '#9be9a8';
  if (ratio <= 0.5) return '#40c463';
  if (ratio <= 0.75) return '#30a14e';
  return '#216e39';
}

// 月份标签：每列首日所属月份变化时打标
const monthLabels = computed<{ x: number; text: string }[]>(() => {
  const labels: { x: number; text: string }[] = [];
  heatColumns.value.forEach((col, idx) => {
    const first = col.find((c) => !c.inFuture && !c.outside);
    if (!first) return;
    const prevMonth = idx > 0
      ? heatColumns.value[idx - 1].find((c) => !c.inFuture && !c.outside)?.month
      : undefined;
    if (first.month !== prevMonth) {
      labels.push({ x: PAD_LEFT + idx * STEP, text: `${first.month + 1}月` });
    }
  });
  return labels;
});

// 星期标签：仅显示一/三/五
const dayLabels = [
  { y: PAD_TOP + 1 * STEP + CELL, text: '一' },
  { y: PAD_TOP + 3 * STEP + CELL, text: '三' },
  { y: PAD_TOP + 5 * STEP + CELL, text: '五' },
];

const totals = computed(() => {
  let count = 0;
  let success = 0;
  let activeDays = 0;
  (props.cells || []).forEach((c) => {
    count += c.count || 0;
    success += c.successCount || 0;
    if ((c.count || 0) > 0) activeDays++;
  });
  return { count, success, activeDays };
});
</script>

<template>
  <div class="w-full">
    <!-- 顶部统计 -->
    <div class="flex flex-wrap items-center gap-x-6 gap-y-1 mb-3 text-sm" style="color: var(--theme-text-secondary);">
      <span>{{ year }} 年共 <b class="font-semibold" style="color: var(--theme-text);">{{ totals.count }}</b> 次提交</span>
      <span>通过 <b class="font-semibold" style="color: var(--theme-text);">{{ totals.success }}</b> 次</span>
      <span>活跃 <b class="font-semibold" style="color: var(--theme-text);">{{ totals.activeDays }}</b> 天</span>
    </div>

    <!-- SVG 热力图 -->
    <div class="w-full overflow-x-auto">
      <svg
        :viewBox="`0 0 ${svgWidth} ${svgHeight}`"
        class="w-full h-auto"
        style="min-width: 640px; max-width: 100%;"
        role="img"
        aria-label="刷题日历热力图"
      >
        <!-- 月份标签 -->
        <g>
          <text
            v-for="(lbl, i) in monthLabels"
            :key="`m-${i}`"
            :x="lbl.x"
            :y="12"
            font-size="10"
            fill="var(--theme-text-secondary)"
          >{{ lbl.text }}</text>
        </g>
        <!-- 星期标签 -->
        <g>
          <text
            v-for="(lbl, i) in dayLabels"
            :key="`d-${i}`"
            :x="0"
            :y="lbl.y"
            font-size="10"
            fill="var(--theme-text-secondary)"
          >{{ lbl.text }}</text>
        </g>
        <!-- 热力方块 -->
        <g>
          <template v-for="(col, ci) in heatColumns" :key="`col-${ci}`">
            <rect
              v-for="(cell, ri) in col"
              :key="`cell-${ci}-${ri}`"
              :x="PAD_LEFT + ci * STEP"
              :y="PAD_TOP + ri * STEP"
              :width="CELL"
              :height="CELL"
              rx="2"
              ry="2"
              :fill="heatColor(cell.count, cell.inFuture, cell.outside)"
            >
              <title>{{ (cell.inFuture || cell.outside) ? '' : `${cell.date}：提交 ${cell.count} 次 / 通过 ${cell.successCount} 次` }}</title>
            </rect>
          </template>
        </g>
      </svg>
    </div>

    <!-- 图例 -->
    <div class="flex items-center gap-2 mt-2 text-[10px]" style="color: var(--theme-text-secondary);">
      <span>少</span>
      <span class="inline-block rounded-sm" :style="{ width: CELL + 'px', height: CELL + 'px', backgroundColor: 'var(--theme-border)' }"></span>
      <span class="inline-block rounded-sm" :style="{ width: CELL + 'px', height: CELL + 'px', backgroundColor: '#9be9a8' }"></span>
      <span class="inline-block rounded-sm" :style="{ width: CELL + 'px', height: CELL + 'px', backgroundColor: '#40c463' }"></span>
      <span class="inline-block rounded-sm" :style="{ width: CELL + 'px', height: CELL + 'px', backgroundColor: '#30a14e' }"></span>
      <span class="inline-block rounded-sm" :style="{ width: CELL + 'px', height: CELL + 'px', backgroundColor: '#216e39' }"></span>
      <span>多</span>
    </div>

    <!-- 加载占位 -->
    <div v-if="loading" class="text-center py-6 text-sm" style="color: var(--theme-text-secondary);">
      正在加载日历数据...
    </div>
  </div>
</template>
