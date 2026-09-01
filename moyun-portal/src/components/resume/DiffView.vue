<script setup lang="ts">
/**
 * 逐字 diff 视图组件（v10.22 阶段四）
 * 基于 LCS（最长公共子序列）算法实现字符级对比，高亮增删改：
 *   - common（不变）：默认样式
 *   - added（新增）：绿色背景
 *   - removed（删除）：红色背景 + 删除线
 * 简历字段文本通常 50-500 字，简单 LCS 足够，无需第三方库或性能优化。
 * 中文按字、英文按空格拆分；这里统一按字符（Array.from 兼容 emoji/多字节）拆分，最简单且通用。
 */
import { computed } from 'vue';

interface DiffSegment {
  type: 'common' | 'added' | 'removed';
  text: string;
}

const props = defineProps<{
  /** 原文（优化前） */
  original?: string;
  /** 优化后文本 */
  optimized?: string;
}>();

/**
 * 计算字符级 diff 片段
 * 算法：标准 LCS 动态规划 + 回溯，再合并相邻同类型片段
 */
const diffSegments = computed<DiffSegment[]>(() => {
  const a = props.original ?? '';
  const b = props.optimized ?? '';

  // 边界情况：两者都空
  if (!a && !b) return [];
  // 原文为空：全部为新增
  if (!a) return [{ type: 'added', text: b }];
  // 优化后为空：全部为删除
  if (!b) return [{ type: 'removed', text: a }];
  // 完全相同：整段 common
  if (a === b) return [{ type: 'common', text: a }];

  // 按字符拆分（Array.from 正确处理多字节字符与 emoji）
  const aChars = Array.from(a);
  const bChars = Array.from(b);
  const m = aChars.length;
  const n = bChars.length;

  // 构建 LCS 长度 DP 表（m+1) × (n+1)
  const dp: number[][] = Array.from({ length: m + 1 }, () => new Array<number>(n + 1).fill(0));
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      if (aChars[i - 1] === bChars[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1;
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
      }
    }
  }

  // 回溯生成片段（从尾部回溯，再反转）
  const raw: DiffSegment[] = [];
  let i = m;
  let j = n;
  while (i > 0 && j > 0) {
    if (aChars[i - 1] === bChars[j - 1]) {
      raw.push({ type: 'common', text: aChars[i - 1] });
      i--;
      j--;
    } else if (dp[i - 1][j] >= dp[i][j - 1]) {
      raw.push({ type: 'removed', text: aChars[i - 1] });
      i--;
    } else {
      raw.push({ type: 'added', text: bChars[j - 1] });
      j--;
    }
  }
  while (i > 0) {
    raw.push({ type: 'removed', text: aChars[i - 1] });
    i--;
  }
  while (j > 0) {
    raw.push({ type: 'added', text: bChars[j - 1] });
    j--;
  }
  raw.reverse();

  // 合并相邻同类型片段，减少 DOM 节点数量
  const merged: DiffSegment[] = [];
  for (const seg of raw) {
    const last = merged[merged.length - 1];
    if (last && last.type === seg.type) {
      last.text += seg.text;
    } else {
      merged.push({ type: seg.type, text: seg.text });
    }
  }
  return merged;
});
</script>

<template>
  <div class="diff-view">
    <span
      v-for="(seg, i) in diffSegments"
      :key="i"
      :class="['diff-seg', seg.type]"
    >{{ seg.text }}</span>
  </div>
</template>

<style scoped>
.diff-view {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}
/* 不变：默认文本样式，继承父级颜色 */
.diff-seg.common {
  color: inherit;
}
/* 新增：绿色背景 */
.diff-seg.added {
  background-color: rgba(34, 197, 94, 0.18);
  color: #15803d;
  border-radius: 2px;
  padding: 0 1px;
}
/* 删除：红色背景 + 删除线 */
.diff-seg.removed {
  background-color: rgba(239, 68, 68, 0.15);
  color: #b91c1c;
  text-decoration: line-through;
  border-radius: 2px;
  padding: 0 1px;
}
</style>
