<template>
  <div class="shared-report-page">
    <div v-if="loading" class="state-tip">报告加载中…</div>
    <!--
      清单 P2：原先任何异常（网络/代理返回 HTML/5xx）都只显示"分享链接不存在或已过期"，且**没有重试入口**，
      把"加载失败"误说成"链接失效"。这里展示真实原因并给出重试。
    -->
    <div v-else-if="!report" class="state-tip error">
      <p>{{ loadError || '分享链接不存在或已过期' }}</p>
      <p class="sub">请向分享者索取新的链接</p>
      <button v-if="canRetry" class="retry-btn" @click="loadReport">重试</button>
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

      <!-- 清单 P2：原"相关知识点"区块已移除（后端不再产出该字段，恒为 null，属死 UI） -->

      <footer class="report-footer">
        <p>本报告由 AI 语音面试官生成，仅代表练习评估参考</p>
      </footer>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import { getSharedReport } from '@/api/voiceInterview';
import type { VoiceInterviewReportVO } from '@/api/voiceInterview';

const route = useRoute();
const loading = ref(true);
const report = ref<VoiceInterviewReportVO | null>(null);

/**
 * SEO 与分享卡片（清单 P2）。
 *
 * <p>本页是**免登录公开页**（路由 `requiresAuth: false` 且声明了 `robots`），
 * 但全仓库没有任何代码读取 `route.meta.robots`，而本页原先**完全没有 useHead** ⇒
 * 既没有 robots meta，也没有可被社交平台抓取的 title/description/og 标签。
 * 这里补上（robots 取 noindex：分享链接不应被搜索引擎收录，但需要社交卡片信息）。</p>
 */
useHead(
  computed(() => ({
    title: 'AI 语音面试报告 · 旭林知行',
    meta: [
      { name: 'description', content: '由 AI 面试官生成的语音面试能力评估报告（脱敏公开分享）。' },
      { name: 'robots', content: 'noindex,nofollow' },
      { property: 'og:title', content: 'AI 语音面试报告 · 旭林知行' },
      { property: 'og:description', content: '由 AI 面试官生成的语音面试能力评估报告（脱敏公开分享）。' },
      { property: 'og:type', content: 'article' },
    ],
  })),
);

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

// 清单 P2：后端已在报告生成处显式移除"相关知识点"产出
//（VoiceInterviewServiceImpl 注释：题库 tags 聚合对 agent 自由面试无参考意义，前端 Tab 已删），
// 全仓库再无 setKnowledgePoints 调用 ⇒ knowledgePoints 恒为 null。
// 原先此处的双形态兼容解析（string / {title,desc}）与对应模板区块都是**永不渲染的死代码**，已移除。

/**
 * 等级文案（清单 P2）。
 *
 * <p>原先阈值为 85/70/60，而**同一份报告**在站内主报告页用的是 80/70/60 ⇒ 同一分数在两处显示不同等级。
 * 这里统一到 80/70/60（与主报告页 & scoreClass 口径一致）。
 * 后端另有结构化定级字段 `levelEstimate`（junior/mid/senior），如需彻底统一应改用它，属后续项。</p>
 */
const levelText = computed(() => {
  const s = report.value?.totalScore ?? 0;
  if (s >= 80) return '优秀 · 具备冲击大厂的实力';
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

/** 失败原因（清单 P2）：区分"链接失效"与"加载失败"，后者可重试 */
const loadError = ref<string | null>(null);
/** 仅当失败原因可能是临时性的（网络/服务异常）才提供重试 */
const canRetry = ref(false);

/** 加载分享报告；抽成独立函数以便失败后重试 */
async function loadReport() {
  const token = String(route.params.token ?? '');
  loading.value = true;
  loadError.value = null;
  canRetry.value = false;
  try {
    const res = await getSharedReport(token);
    const data = (res as { data?: VoiceInterviewReportVO })?.data ?? null;
    report.value = data;
    if (!data) {
      // 接口成功但没有内容：确实是无效/过期的分享链接
      loadError.value = '分享链接不存在或已过期';
      canRetry.value = false;
    }
  } catch (e) {
    report.value = null;
    const msg = (e as { message?: string })?.message || '';
    // 带后端业务文案的失败（如"分享不存在"）按失效处理；其余（网络/服务异常）提示可重试
    const looksLikeMissing = /不存在|已过期|无效|未找到/.test(msg);
    loadError.value = msg || '报告加载失败，请稍后重试';
    canRetry.value = !looksLikeMissing;
  } finally {
    loading.value = false;
  }
}

onMounted(loadReport);
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
.retry-btn {
  margin-top: 12px;
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid currentColor;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 14px;
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