<script setup lang="ts">
/**
 * 简历维护页 - 右侧评分面板
 * 对应 vue_resume_spec.md §5.3 ScorePanel
 * 评分仪表盘(SVG环) + 四维度进度条 + 洞察提示 + 文件列表
 */
import { computed } from 'vue';
import { Sparkles } from 'lucide-vue-next';
import type { UserResumeScoreItem } from '@/types/api';

const props = defineProps<{
  /** 综合评分 0-100 */
  score?: number;
  /** 评分明细（后端返回的多维度评分） */
  scoreDetail?: UserResumeScoreItem[];
  /** 最后评分时间 */
  scoredTime?: string;
  /** 是否评分中 */
  scoring?: boolean;
  /** 可优化项数 */
  optimizeCount?: number;
}>();

const emit = defineEmits<{
  (e: 'optimize'): void;
  (e: 'rescore'): void;
}>();

// SVG 环形图参数：r=42, 周长 ≈ 263.9
const CIRCUM = 2 * Math.PI * 42;
const scoreNum = computed(() => props.score ?? 0);
const dashOffset = computed(() => {
  const pct = Math.max(0, Math.min(100, scoreNum.value)) / 100;
  return CIRCUM * (1 - pct);
});
const gradeText = computed(() => {
  const s = scoreNum.value;
  if (s >= 85) return '优秀 · 简历竞争力强';
  if (s >= 70) return '良好 · 仍有提升空间';
  if (s >= 60) return '及格 · 建议优化';
  return '偏弱 · 急需优化';
});
const gradeClass = computed(() => {
  const s = scoreNum.value;
  if (s >= 85) return 'text-emerald-600';
  if (s >= 70) return 'text-amber-600';
  if (s >= 60) return 'text-amber-600';
  return 'text-red-600';
});
const gaugeColor = computed(() => {
  const s = scoreNum.value;
  if (s >= 85) return '#10b981';
  if (s >= 70) return '#f59e0b';
  return '#ef4444';
});

// 四维度指标（映射 scoreDetail 前 4 项，不足则补默认）
const metrics = computed(() => {
  const detail = props.scoreDetail || [];
  const fallback: Array<{ label: string; val: number; color: string }> = [];
  const palette = ['#2563eb', '#f59e0b', '#ef4444', '#7c3aed'];
  const defaultLabels = ['内容完整', '关键词', '专业表达', '数据量化'];
  for (let i = 0; i < 4; i++) {
    const d = detail[i];
    if (d && d.maxScore > 0) {
      const pct = Math.round((d.score / d.maxScore) * 100);
      fallback.push({ label: d.item, val: pct, color: palette[i] });
    } else {
      fallback.push({ label: defaultLabels[i], val: 0, color: palette[i] });
    }
  }
  return fallback;
});

const optCount = computed(() => props.optimizeCount ?? (scoreNum.value > 0 && scoreNum.value < 85 ? 3 : 0));
</script>

<template>
  <aside class="re-rp">
    <!-- 评分卡 -->
    <div class="re-rp-block">
      <div class="re-rp-title">简历质量评分</div>
      <div class="re-rp-score-card">
        <div class="re-rp-gauge">
          <svg width="100" height="100" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="42" fill="none" stroke-width="8" stroke="#f3f4f6" />
            <circle
              cx="50" cy="50" r="42" fill="none" stroke-width="8"
              :stroke="gaugeColor"
              :stroke-dasharray="CIRCUM"
              :stroke-dashoffset="dashOffset"
              stroke-linecap="round"
              style="transform: rotate(-90deg); transform-origin: center; transition: stroke-dashoffset 1.2s cubic-bezier(0,0,0.2,1);"
            />
          </svg>
          <div class="re-rp-gauge-val">
            <div class="re-rp-gauge-num">{{ scoreNum }}</div>
            <div class="re-rp-gauge-total">/ 100</div>
          </div>
        </div>
        <div v-if="score !== undefined && score > 0" class="re-rp-grade" :class="gradeClass">{{ gradeText }}</div>
        <div v-else class="re-rp-grade text-gray-400">尚未评分</div>
        <div v-if="scoredTime" class="re-rp-subtitle">评分于 {{ scoredTime }}</div>
        <button
          v-if="score !== undefined && score > 0"
          type="button"
          class="re-rp-rescore-btn"
          :disabled="scoring"
          @click="emit('rescore')"
        >{{ scoring ? '评分中...' : '重新评分' }}</button>
      </div>

      <!-- 四维度进度条 -->
      <div class="re-rp-metrics" v-if="score !== undefined && score > 0">
        <div v-for="m in metrics" :key="m.label" class="re-rp-metric">
          <span class="re-rp-metric-label">{{ m.label }}</span>
          <div class="re-rp-metric-bar">
            <div class="re-rp-metric-fill" :style="{ width: m.val + '%', background: m.color }"></div>
          </div>
          <span class="re-rp-metric-val" :style="{ color: m.color }">{{ m.val }}</span>
        </div>
      </div>

      <!-- 洞察提示 -->
      <div class="re-rp-insight">
        <p v-if="optCount > 0">
          🔍 发现 <strong class="re-rp-insight-stat">{{ optCount }} 项</strong>可优化内容，AI 深度优化预计提升 <strong class="re-rp-insight-stat">20+ 分</strong>
        </p>
        <p v-else-if="score !== undefined && score >= 85">
          ✨ 简历质量优秀，可继续完善细节
        </p>
        <p v-else>
          💡 完善简历后可进行 AI 深度优化
        </p>
        <button type="button" class="re-rp-insight-btn" @click="emit('optimize')">
          <Sparkles class="w-3.5 h-3.5" />
          {{ score !== undefined && score > 0 ? '立即优化' : '去完善简历' }}
        </button>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="re-rp-block">
      <div class="re-rp-title">我的简历</div>
      <div class="re-rp-file-item current">
        <span class="re-rp-fl-icon re-rp-fl-online">在线</span>
        <div class="re-rp-fl-info">
          <div class="re-rp-fl-name">在线简历</div>
          <div class="re-rp-fl-time">当前编辑中</div>
        </div>
        <span class="re-rp-fl-tag current">当前</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.re-rp {
  width: 260px;
  background: #fff;
  border-left: 1px solid var(--theme-border);
  padding: 20px 16px;
  position: sticky;
  top: 72px;
  height: calc(100vh - 72px);
  overflow-y: auto;
  flex-shrink: 0;
}
.re-rp::-webkit-scrollbar { width: 4px; }
.re-rp::-webkit-scrollbar-thumb { background: var(--theme-border); border-radius: 2px; }

.re-rp-block { margin-bottom: 20px; }
.re-rp-title {
  font-size: 12px;
  font-weight: 700;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 12px;
}

.re-rp-score-card {
  text-align: center;
  padding: 20px 16px;
  background: linear-gradient(180deg, #fcfcfd, #fff);
  border-radius: 14px;
  border: 1px solid var(--theme-border);
}
.re-rp-gauge { position: relative; width: 100px; height: 100px; margin: 0 auto 12px; }
.re-rp-gauge-val {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}
.re-rp-gauge-num { font-size: 32px; font-weight: 800; color: var(--theme-text); line-height: 1; }
.re-rp-gauge-total { font-size: 11px; color: #9ca3af; }
.re-rp-grade { font-size: 13px; font-weight: 600; margin-bottom: 4px; }
.re-rp-subtitle { font-size: 11px; color: #9ca3af; }
.re-rp-rescore-btn {
  margin-top: 10px;
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 6px;
  background: #f9fafb;
  color: #6b7280;
  border: 1px solid var(--theme-border);
  cursor: pointer;
  transition: all 0.15s;
}
.re-rp-rescore-btn:hover:not(:disabled) { border-color: var(--theme-primary); color: var(--theme-primary); }
.re-rp-rescore-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.re-rp-metrics { margin-top: 14px; }
.re-rp-metric { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.re-rp-metric:last-child { margin-bottom: 0; }
.re-rp-metric-label { font-size: 11px; color: #6b7280; width: 56px; flex-shrink: 0; }
.re-rp-metric-bar { flex: 1; height: 6px; background: #f3f4f6; border-radius: 3px; overflow: hidden; }
.re-rp-metric-fill { height: 100%; border-radius: 3px; transition: width 0.8s cubic-bezier(0,0,0.2,1); }
.re-rp-metric-val { font-size: 11px; font-weight: 700; width: 22px; text-align: right; flex-shrink: 0; }

.re-rp-insight {
  margin-top: 14px;
  padding: 12px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 10px;
}
.re-rp-insight p { font-size: 12px; color: #4b5563; line-height: 1.5; margin-bottom: 8px; }
.re-rp-insight-stat { font-weight: 700; color: #d97706; }
.re-rp-insight-btn {
  width: 100%;
  padding: 8px;
  background: var(--theme-primary);
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.re-rp-insight-btn:hover { background: #b91c1c; }

.re-rp-file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--theme-border);
  margin-bottom: 6px;
}
.re-rp-file-item.current { background: #fef2f2; border-color: #fecaca; }
.re-rp-fl-icon {
  width: 28px; height: 28px;
  border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-size: 10px; font-weight: 700;
  flex-shrink: 0;
}
.re-rp-fl-online { background: #fef2f2; color: var(--theme-primary); }
.re-rp-fl-info { flex: 1; min-width: 0; }
.re-rp-fl-name { font-size: 12px; font-weight: 600; color: var(--theme-text); }
.re-rp-fl-time { font-size: 10px; color: #9ca3af; margin-top: 1px; }
.re-rp-fl-tag {
  font-size: 9px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 700;
  letter-spacing: 0.3px;
  flex-shrink: 0;
}
.re-rp-fl-tag.current { background: #ecfdf5; color: #059669; }

@media (max-width: 1280px) {
  .re-rp { display: none; }
}
</style>
