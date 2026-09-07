<template>
  <div class="shared-report-page">
    <div v-if="loading" class="state-tip">报告加载中…</div>
    <div v-else-if="!report" class="state-tip error">
      <p>分享链接不存在或已过期</p>
      <p class="sub">请向分享者索取新的链接</p>
    </div>
    <template v-else>
      <header class="report-header">
        <h1>AI 语音面试报告</h1>
        <p class="meta">{{ report.totalScore ?? '-' }} 分 · 由 AI 面试官生成</p>
      </header>

      <section class="card score-card">
        <div class="total">
          <span class="num">{{ report.totalScore ?? '-' }}</span>
          <span class="label">综合得分</span>
        </div>
        <div v-if="levelText" class="level">{{ levelText }}</div>
      </section>

      <section v-if="dims.length" class="card">
        <h2>能力维度</h2>
        <div class="dims">
          <div v-for="d in dims" :key="d.key" class="dim-row">
            <span class="dim-label">{{ d.label }}</span>
            <div class="dim-bar"><div class="dim-fill" :style="{ width: d.value + '%' }" /></div>
            <span class="dim-value">{{ d.value }}</span>
          </div>
        </div>
      </section>

      <section v-if="report.introScore" class="card">
        <h2>自我介绍表现</h2>
        <p class="comment">{{ report.introScore.comment || '—' }}</p>
        <div v-if="introDims.length" class="dims">
          <div v-for="d in introDims" :key="d.key" class="dim-row">
            <span class="dim-label">{{ d.label }}</span>
            <div class="dim-bar"><div class="dim-fill intro" :style="{ width: d.value + '%' }" /></div>
            <span class="dim-value">{{ d.value }}</span>
          </div>
        </div>
      </section>

      <section v-if="report.summary" class="card">
        <h2>AI 总结</h2>
        <p class="comment">{{ report.summary }}</p>
      </section>

      <section v-if="report.highlights?.length" class="card">
        <h2>亮点</h2>
        <ul class="point-list">
          <li v-for="(h, i) in report.highlights" :key="i">✅ {{ h }}</li>
        </ul>
      </section>

      <section v-if="report.weakPoints?.length" class="card">
        <h2>薄弱点</h2>
        <ul class="point-list weak">
          <li v-for="(w, i) in report.weakPoints" :key="i">⚠️ {{ w }}</li>
        </ul>
      </section>

      <section v-if="report.improvementSuggestions?.length" class="card">
        <h2>改进建议</h2>
        <ol class="point-list">
          <li v-for="(s, i) in report.improvementSuggestions" :key="i">{{ s }}</li>
        </ol>
      </section>

      <section v-if="report.questionReviews?.length" class="card">
        <h2>逐题点评</h2>
        <div v-for="(q, i) in report.questionReviews" :key="i" class="qa-item">
          <div class="qa-head">
            <span class="qa-idx">Q{{ i + 1 }}</span>
            <span class="qa-score" :class="scoreClass(q.score)">{{ q.score ?? '-' }} 分</span>
          </div>
          <p class="qa-question">{{ q.question }}</p>
          <p v-if="q.feedback" class="qa-feedback">{{ q.feedback }}</p>
        </div>
      </section>

      <section v-if="kpItems.length" class="card">
        <h2>相关知识点</h2>
        <div class="kp-grid">
          <div v-for="(k, i) in kpItems" :key="i" class="kp-item">
            <div class="kp-title">📚 {{ k.title }}</div>
            <div v-if="k.desc" class="kp-desc">{{ k.desc }}</div>
          </div>
        </div>
      </section>

      <footer class="report-footer">
        <p>本报告由 AI 语音面试官生成，仅代表练习评估参考</p>
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { getSharedReport } from '@/api/voiceInterview';
import type { VoiceInterviewReportVO } from '@/api/voiceInterview';

const route = useRoute();
const loading = ref(true);
const report = ref<VoiceInterviewReportVO | null>(null);

const DIM_META: Record<string, string> = {
  relevance: '回答相关性',
  professionalism: '专业度',
  fluency: '表达流畅度',
  interactivity: '面试互动性',
  confidence: '自信度',
  logic: '逻辑清晰',
  coverage: '知识点覆盖',
  length: '答案充实度',
  structure: '结构化程度',
  awareness: '自我认知',
  matching: '岗位匹配',
};

const dims = computed(() => {
  const raw = report.value?.dimensions ?? {};
  return Object.entries(raw)
    .map(([key, value]) => ({ key, label: DIM_META[key] ?? key, value: value ?? 0 }))
    .filter((d) => typeof d.value === 'number');
});

const introDims = computed(() => {
  const raw = report.value?.introScore?.dimensions ?? {};
  return Object.entries(raw)
    .map(([key, value]) => ({ key, label: DIM_META[key] ?? key, value: value ?? 0 }))
    .filter((d) => typeof d.value === 'number');
});

const kpItems = computed(() =>
  (report.value?.knowledgePoints ?? []).map((k) =>
    typeof k === 'string' ? { title: k, desc: '' } : { title: k.title ?? k.name ?? '', desc: k.desc ?? k.description ?? '' },
  ),
);

const levelText = computed(() => {
  const s = report.value?.totalScore ?? 0;
  if (s >= 85) return '优秀 · 具备冲击大厂的实力';
  if (s >= 70) return '良好 · 框架完整，细节待补';
  if (s >= 60) return '合格 · 基础尚可，需要体系化梳理';
  return '待提升 · 建议系统性复习后再战';
});

function scoreClass(score?: number) {
  if (score == null) return '';
  if (score >= 80) return 'good';
  if (score >= 60) return 'mid';
  return 'bad';
}

onMounted(async () => {
  const token = String(route.params.token ?? '');
  try {
    const res = await getSharedReport(token);
    report.value = (res as { data?: VoiceInterviewReportVO })?.data ?? null;
  } catch {
    report.value = null;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.shared-report-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}
.state-tip {
  text-align: center;
  padding: 80px 16px;
  color: var(--text-secondary, #666);
}
.state-tip.error .sub {
  font-size: 13px;
  margin-top: 8px;
  opacity: 0.7;
}
.report-header {
  text-align: center;
  margin-bottom: 20px;
}
.report-header h1 {
  font-size: 22px;
  margin: 0 0 6px;
}
.report-header .meta {
  color: var(--text-secondary, #666);
  font-size: 14px;
}
.card {
  background: var(--bg-card, #fff);
  border-radius: 14px;
  padding: 18px;
  margin-bottom: 14px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
}
.card h2 {
  font-size: 16px;
  margin: 0 0 12px;
}
.score-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.score-card .total {
  display: flex;
  align-items: baseline;
  gap: 8px;
}
.score-card .num {
  font-size: 44px;
  font-weight: 700;
  color: var(--primary, #409eff);
}
.score-card .label {
  color: var(--text-secondary, #666);
}
.level {
  font-size: 14px;
  color: var(--primary, #409eff);
  background: rgba(64, 158, 255, 0.08);
  padding: 6px 12px;
  border-radius: 999px;
}
.dims {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.dim-row {
  display: grid;
  grid-template-columns: 90px 1fr 40px;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}
.dim-bar {
  height: 8px;
  border-radius: 4px;
  background: rgba(127, 127, 127, 0.15);
  overflow: hidden;
}
.dim-fill {
  height: 100%;
  border-radius: 4px;
  background: var(--primary, #409eff);
}
.dim-fill.intro {
  background: #67c23a;
}
.dim-value {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.comment {
  margin: 0 0 12px;
  line-height: 1.7;
  font-size: 14px;
  white-space: pre-wrap;
}
.point-list {
  margin: 0;
  padding-left: 20px;
  line-height: 1.9;
  font-size: 14px;
}
.point-list.weak li {
  color: #e6a23c;
}
.qa-item {
  padding: 12px 0;
  border-bottom: 1px solid rgba(127, 127, 127, 0.12);
}
.qa-item:last-child {
  border-bottom: none;
}
.qa-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.qa-idx {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-secondary, #666);
}
.qa-score {
  font-size: 13px;
  font-weight: 600;
}
.qa-score.good { color: #67c23a; }
.qa-score.mid { color: #e6a23c; }
.qa-score.bad { color: #f56c6c; }
.qa-question {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 500;
}
.qa-feedback {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary, #666);
  line-height: 1.6;
}
.kp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}
.kp-item {
  border: 1px solid rgba(127, 127, 127, 0.18);
  border-radius: 10px;
  padding: 10px 12px;
}
.kp-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}
.kp-desc {
  font-size: 12px;
  color: var(--text-secondary, #666);
  line-height: 1.5;
}
.report-footer {
  text-align: center;
  color: var(--text-secondary, #999);
  font-size: 12px;
  margin-top: 24px;
}
</style>