<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { formatDate } from '@/utils/date';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Target, FolderOpen, Bot, GitCompare, Eye, Plus, ArrowRight, ArrowLeft,
  CheckCircle2, X, Sparkles, Save, Star, RefreshCw, FileText, Trash2, Rocket,
  AlertCircle, PartyPopper, Pencil as PencilIcon, History,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import JobTargetForm from '@/components/resume/JobTargetForm.vue';
import JobMatchPanel from '@/components/resume/JobMatchPanel.vue';
import OptimizeProgress from '@/components/resume/OptimizeProgress.vue';
import OptimizeCompare from '@/components/resume/OptimizeCompare.vue';
import ScoreReportDialog from '@/components/resume/ScoreReportDialog.vue';
import { generateSeo } from '@/utils/seo';
import { getMyResumeList, scoreResume, getResumeDetail } from '@/api/interview';
import {
  getJobTargets, createJobTarget, deleteJobTarget, runJobMatch,
  generateDeepOptimize, applyDeepOptimize, getOptimizeHistory,
  saveScoreReport, getScoreReports,
} from '@/api/resumeOptimize';
import type {
  UserResumeVO, ResumeJobTarget, ResumeJobMatchReport,
  ResumeDeepOptimizeVO, ResumeOptimizeItem, ResumeOptimizeHistory,
  ResumeScoreReport,
} from '@/types/api';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();

useHead(computed(() => generateSeo({
  title: 'AI 简历优化工作台',
  description: '岗位匹配评分与深度优化：粘贴JD精准分析，AI逐项优化前后对比，采纳建议生成新版本',
  keywords: ['简历优化', '岗位匹配', 'AI简历', '简历评分'],
  canonicalPath: '/interview/resume/optimize',
  robots: 'noindex,nofollow',
})));

const breadcrumbs = computed(() => [
  { label: '个人空间', path: '/user' },
  { label: 'AI 简历优化工作台' },
]);

// ==================== 步骤状态 ====================
const step = ref(1); // 1岗位 2简历 3分析 4对比优化 5预览
const steps = [
  { n: 1, label: '选择岗位', icon: Target },
  { n: 2, label: '选择简历', icon: FolderOpen },
  { n: 3, label: 'AI 分析', icon: Bot },
  { n: 4, label: '对比优化', icon: GitCompare },
  { n: 5, label: '预览保存', icon: Eye },
];

// 岗位目标
const jobTargets = ref<ResumeJobTarget[]>([]);
const selectedTargetId = ref<number | string | null>(null);
const jobModalVisible = ref(false);
const jobSaving = ref(false);
const jobForm = ref<{ position: string; company: string; city: string; jobType: string; jdText: string }>({
  position: '', company: '', city: '', jobType: '全职', jdText: '',
});

// 简历
const resumes = ref<UserResumeVO[]>([]);
const resumesLoading = ref(false);
const selectedResumeId = ref<number | string | null>(null);

// 分析
const analyzing = ref(false);
const progressPercent = ref(0);
const progressStep = ref(1);
const matchReport = ref<ResumeJobMatchReport | null>(null);

// 深度优化
const optimizing = ref(false);
const optimizeResult = ref<ResumeDeepOptimizeVO | null>(null);
const adoptedSet = ref<Set<number>>(new Set());

// 保存
const saving = ref(false);
const savedResumeId = ref<number | string | null>(null);
const rescoredScore = ref<number | null>(null);
const rematchedScore = ref<number | null>(null);
const rematching = ref(false);

const selectedTarget = computed(() => jobTargets.value.find(t => t.id === selectedTargetId.value) || null);
const selectedResume = computed(() => resumes.value.find(r => r.id === selectedResumeId.value) || null);

const JOB_TYPE_OPTIONS = ['全职', '兼职', '实习', '校招'];

// ==================== 优化历史 ====================
const historyLoading = ref(false);
const optimizeHistory = ref<ResumeOptimizeHistory[]>([]);
const historyVisibleCount = ref(5);

async function loadOptimizeHistory() {
  if (!selectedResumeId.value) {
    optimizeHistory.value = [];
    return;
  }
  historyLoading.value = true;
  try {
    const res = await getOptimizeHistory(selectedResumeId.value);
    optimizeHistory.value = res.code === 200 ? (res.data ?? []) : [];
  } catch (e) {
    console.warn('[optimize] 历史加载失败', e);
  } finally {
    historyLoading.value = false;
  }
}

// ==================== 评分报告存档（v10.18 阶段五） ====================
const scoreReports = ref<ResumeScoreReport[]>([]);
const scoreReportVisible = ref(false);
const scoreReportLoading = ref(false);

async function loadScoreReports(resumeId?: number | string | null) {
  const id = resumeId ?? savedResumeId.value ?? selectedResumeId.value;
  if (!id) {
    scoreReports.value = [];
    return;
  }
  scoreReportLoading.value = true;
  try {
    const res = await getScoreReports(id);
    scoreReports.value = res.code === 200 ? (res.data ?? []) : [];
  } catch (e) {
    console.warn('[optimize] 评分报告加载失败', e);
  } finally {
    scoreReportLoading.value = false;
  }
}

async function openScoreReportDialog() {
  await Promise.all([loadOptimizeHistory(), loadScoreReports(savedResumeId.value)]);
  scoreReportVisible.value = true;
}

async function refreshScoreReports() {
  await Promise.all([loadOptimizeHistory(), loadScoreReports(savedResumeId.value)]);
}

const visibleHistory = computed(() => optimizeHistory.value.slice(0, historyVisibleCount.value));

function formatHistoryTime(dt?: string | null): string {
  if (!dt) return '';
  const d = new Date(dt);
  if (Number.isNaN(d.getTime())) return dt;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function scoreDelta(h: ResumeOptimizeHistory): number {
  const after = h.scoreAfter ?? 0;
  const before = h.scoreBefore ?? 0;
  return after - before;
}

function goEditFromHistory(h: ResumeOptimizeHistory) {
  router.push({ name: 'ResumeEdit', query: { id: String(h.resumeId) } });
}

// ==================== 数据加载 ====================
onMounted(async () => {
  loadJobTargets();
  await loadResumes();
  // 从列表/编辑页带参：?resumeId=xx
  const q = route.query.resumeId as string;
  if (q && resumes.value.some(r => String(r.id) === q)) {
    selectedResumeId.value = q;
  }
  loadOptimizeHistory();
});

watch(selectedResumeId, () => {
  loadOptimizeHistory();
});

async function loadJobTargets() {
  try {
    const res = await getJobTargets();
    if (res.code === 200 && res.data) {
      jobTargets.value = res.data;
      const def = res.data.find(t => t.isDefault === 1) || res.data[0];
      if (def?.id) selectedTargetId.value = def.id;
    }
  } catch { /* 静默 */ }
}

async function loadResumes() {
  try {
    resumesLoading.value = true;
    const res = await getMyResumeList({ pageNum: 1, pageSize: 50 });
    if (res.code === 200 && res.data) {
      resumes.value = res.data.list || [];
    }
  } catch { /* 静默 */ } finally {
    resumesLoading.value = false;
  }
}

// ==================== STEP1：岗位 ====================
function openJobModal() {
  jobForm.value = { position: '', company: '', city: '', jobType: '全职', jdText: '' };
  jobModalVisible.value = true;
}

async function saveJobTarget() {
  if (!jobForm.value.position.trim()) {
    toast.error('请填写目标岗位名称');
    return;
  }
  if (!jobForm.value.jdText.trim()) {
    toast.error('请粘贴岗位 JD 描述');
    return;
  }
  try {
    jobSaving.value = true;
    const res = await createJobTarget(jobForm.value);
    if (res.code === 200) {
      toast.success('岗位已创建');
      jobModalVisible.value = false;
      await loadJobTargets();
      if (res.data) selectedTargetId.value = res.data;
    } else {
      toast.error(res.message || '创建失败');
    }
  } finally {
    jobSaving.value = false;
  }
}

async function removeJobTarget(id: number | string) {
  const res = await deleteJobTarget(id);
  if (res.code === 200) {
    toast.success('已删除');
    if (selectedTargetId.value === id) selectedTargetId.value = null;
    await loadJobTargets();
  }
}

// ==================== STEP2 → STEP3：分析 ====================
function goStep2() {
  if (!selectedTargetId.value) {
    toast.error('请先选择目标岗位');
    return;
  }
  step.value = 2;
}

async function startAnalyze() {
  if (!selectedResumeId.value) {
    toast.error('请选择要优化的简历');
    return;
  }
  step.value = 3;
  analyzing.value = true;
  progressPercent.value = 0;
  progressStep.value = 1;
  matchReport.value = null;

  // 进度动画（分析通常 5-20s）
  const timer = setInterval(() => {
    if (progressPercent.value < 90) {
      progressPercent.value += Math.random() * 8 + 2;
      progressStep.value = Math.min(4, Math.floor(progressPercent.value / 25) + 1);
    }
  }, 600);

  try {
    const res = await runJobMatch(selectedResumeId.value, selectedTargetId.value!);
    if (res.code === 200 && res.data) {
      matchReport.value = res.data;
      progressPercent.value = 100;
      progressStep.value = 5;
      setTimeout(() => {
        analyzing.value = false;
        step.value = 4;
      }, 500);
    } else {
      analyzing.value = false;
      toast.error(res.message || '分析失败');
      step.value = 2;
    }
  } catch (e: unknown) {
    analyzing.value = false;
    clearInterval(timer);
    toast.error((e as Error)?.message || '分析失败');
    step.value = 2;
    return;
  }
  clearInterval(timer);
}

// ==================== STEP4：深度优化对比 ====================
const gradeLabel: Record<string, string> = {
  excellent: '优秀匹配', good: '良好匹配', medium: '中等匹配', poor: '匹配较弱',
};

async function generateOptimize() {
  if (!selectedResumeId.value || !selectedTargetId.value) return;
  try {
    optimizing.value = true;
    const res = await generateDeepOptimize(selectedResumeId.value, selectedTargetId.value);
    if (res.code === 200 && res.data) {
      optimizeResult.value = res.data;
      adoptedSet.value = new Set();
      toast.success(`已生成 ${res.data.items.length} 项优化建议`);
    } else {
      toast.error(res.message || '生成失败');
    }
  } finally {
    optimizing.value = false;
  }
}

function toggleAdopted(idx: number) {
  const s = new Set(adoptedSet.value);
  if (s.has(idx)) {
    s.delete(idx);
  } else {
    s.add(idx);
  }
  adoptedSet.value = s;
}

function adoptAll() {
  if (!optimizeResult.value) return;
  adoptedSet.value = new Set(optimizeResult.value.items.map((_, i) => i));
  toast.success('已全选');
}

const SECTION_LABEL: Record<string, string> = {
  objective: '求职意向', education: '教育背景', work: '工作经历',
  project: '项目经历', skills: '专业技能', selfIntro: '自我评价',
};

function sectionLabel(item: ResumeOptimizeItem): string {
  const base = SECTION_LABEL[item.section] || item.section;
  return item.section === 'selfIntro' || item.section === 'objective' || item.section === 'skills'
    ? base
    : `${base} #${(item.index ?? 0) + 1}`;
}

// ==================== STEP5：预览保存（含完整度，参考熊猫简历） ====================
const previewResume = ref<UserResumeVO | null>(null);

async function goPreview() {
  if (!adoptedSet.value.size) {
    toast.error('请至少采纳一项优化建议');
    return;
  }
  try {
    const res = await getResumeDetail(selectedResumeId.value!);
    if (res.code === 200 && res.data) {
      previewResume.value = applyOptimizes(res.data);
      step.value = 5;
    }
  } catch (e) {
    toast.error((e as Error)?.message || '加载简历失败');
  }
}

/** 前端预演：将采纳的建议应用到简历副本（用于预览与完整度计算） */
function applyOptimizes(source: UserResumeVO): UserResumeVO {
  const r: UserResumeVO = JSON.parse(JSON.stringify(source));
  if (!optimizeResult.value) return r;
  optimizeResult.value.items.forEach((item, i) => {
    if (!adoptedSet.value.has(i)) return;
    const text = item.optimized.trim();
    const idx = item.index ?? 0;
    switch (item.section) {
      case 'selfIntro': r.selfIntro = text; break;
      case 'objective':
        if (r.jobIntention && item.field === 'position') r.jobIntention.position = text;
        break;
      case 'education':
        if (r.educations?.[idx] && item.field === 'description') r.educations[idx].description = text;
        break;
      case 'work':
        if (r.works?.[idx] && item.field === 'description') r.works[idx].description = text;
        break;
      case 'project':
        if (r.projects?.[idx] && item.field === 'description') r.projects[idx].description = text;
        break;
      case 'skills': appendSkills(r, text); break;
    }
  });
  return r;
}

function appendSkills(r: UserResumeVO, text: string) {
  r.skills = r.skills || [];
  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim();
    if (!line) continue;
    let level = '熟练';
    let names = line;
    const m = line.match(/^(精通|熟练|了解|一般)[:：]\s*(.+)$/);
    if (m) { level = m[1]; names = m[2]; }
    for (const raw of names.split(/[、,，;；]/)) {
      const name = raw.trim().replace(/[（(].*?[)）]/g, '').trim();
      if (name && name.length <= 30 && !name.startsWith('[') && !r.skills!.some(s => s.name === name)) {
        r.skills!.push({ name, level, category: '' });
      }
    }
  }
}

// 完整度（熊猫简历式：百分比 + 待核对清单）
const completeness = computed(() => {
  const r = previewResume.value;
  if (!r) return { percent: 0, todos: [] as { label: string; why: string }[] };
  const todos: { label: string; why: string }[] = [];
  if (!r.name) todos.push({ label: '姓名', why: '姓名是企业识别候选人的基本信息' });
  if (!r.phone) todos.push({ label: '手机号', why: '手机号是企业联系你的必要信息' });
  if (!r.email) todos.push({ label: '邮箱', why: '邮箱是企业联系你的重要方式' });
  if (!r.jobIntention?.city) todos.push({ label: '意向城市', why: '写明意向城市，有利于企业主动联系你' });
  if (!r.jobIntention?.position) todos.push({ label: '期望职位', why: '明确的职位方向让 HR 快速定位你' });
  if (!r.educations?.length) todos.push({ label: '教育背景', why: '教育背景是筛选的硬性条件' });
  if (!r.works?.length && !r.projects?.length) todos.push({ label: '经历', why: '工作或项目经历是简历的核心' });
  if (!r.skills?.length) todos.push({ label: '专业技能', why: '技能清单帮助 HR 快速匹配岗位要求' });
  if (!r.selfIntro) todos.push({ label: '自我评价', why: '自我评价是展示个人亮点的窗口' });
  const total = 9;
  return { percent: Math.round(((total - todos.length) / total) * 100), todos };
});

// 保存优化结果（直接更新原简历，幂等）
async function saveOptimize() {
  if (!optimizeResult.value || !selectedResumeId.value) return;
  try {
    saving.value = true;
    const res = await applyDeepOptimize({
      resumeId: selectedResumeId.value,
      optimize: optimizeResult.value,
      adopted: Array.from(adoptedSet.value),
    });
    if (res.code === 200 && res.data) {
      savedResumeId.value = res.data as string | number;
      toast.success('优化结果已保存');
      loadOptimizeHistory();
    } else {
      toast.error(res.message || '保存失败');
    }
  } finally {
    saving.value = false;
  }
}

// 重新评分（以最终结果为准，v10.18 同步入库存档为 source=optimize 报告）
async function rescore() {
  if (!savedResumeId.value) {
    toast.error('请先保存优化结果');
    return;
  }
  try {
    // 调用 saveScoreReport：后端先 scoreResume 触发评分（自动入库 source=manual），
    // 再 UPDATE 刚插入的报告 source=optimize + jobTargetId + position（标记为优化后重新评分）
    const res = await saveScoreReport({
      resumeId: savedResumeId.value,
      source: 'optimize',
      jobTargetId: selectedTargetId.value ?? undefined,
      position: selectedTarget.value?.position,
    });
    if (res.code === 200 && res.data) {
      rescoredScore.value = res.data.score ?? null;
      toast.success('重新评分完成（已存档为评分报告）');
      // 刷新评分报告与历史（不阻塞主流程）
      refreshScoreReports();
    } else {
      toast.error(res.message || '评分失败');
    }
  } catch (e) {
    toast.error((e as Error)?.message || '评分失败');
  }
}

// 重新匹配分析（对比优化前后匹配度变化）
async function rematch() {
  if (!savedResumeId.value || !selectedTargetId.value) {
    toast.error('请先保存优化结果');
    return;
  }
  try {
    rematching.value = true;
    const res = await runJobMatch(savedResumeId.value, selectedTargetId.value);
    if (res.code === 200 && res.data) {
      rematchedScore.value = res.data.matchScore;
      toast.success('重新匹配完成');
    } else {
      toast.error(res.message || '匹配失败');
    }
  } finally {
    rematching.value = false;
  }
}

function gotoEditResume() {
  if (savedResumeId.value) router.push(`/interview/resume/edit/${savedResumeId.value}`);
}

function keywordsOf(s?: string): string[] {
  return s ? s.split('、').filter(Boolean) : [];
}

function dimRows() {
  const d = matchReport.value?.dimensions;
  if (!d) return [];
  return [
    { key: 'keywordMatch', label: '关键词匹配', dim: d.keywordMatch },
    { key: 'experienceMatch', label: '经验匹配', dim: d.experienceMatch },
    { key: 'skillMatch', label: '技能匹配', dim: d.skillMatch },
    { key: 'structureMatch', label: '结构完整度', dim: d.structureMatch },
  ].filter(x => x.dim && typeof x.dim.score === 'number');
}
</script>

<template>
  <div class="min-h-screen bg-theme-bg">
    <div class="max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <Breadcrumb :items="breadcrumbs" />

      <!-- 页头 -->
      <div class="mb-6">
        <h1 class="text-2xl font-bold flex items-center gap-2">
          <Sparkles class="w-6 h-6" style="color: var(--theme-primary);" />
          AI 简历优化工作台
        </h1>
        <p class="text-sm text-theme-text-secondary mt-1">粘贴岗位 JD 精准分析 → AI 逐项优化前后对比 → 采纳建议生成新版本</p>
      </div>

      <!-- 步骤条 -->
      <div class="bg-theme-surface rounded-xl border border-theme-border p-4 mb-6 flex items-center justify-between overflow-x-auto">
        <template v-for="(s, i) in steps" :key="s.n">
          <div class="flex items-center gap-2 flex-shrink-0">
            <div
              class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold"
              :class="step > s.n ? 'bg-theme-success text-white' : step === s.n ? 'text-white' : 'bg-theme-surface text-theme-text-secondary'"
              :style="step === s.n ? 'background: var(--theme-primary);' : ''"
            >
              <CheckCircle2 v-if="step > s.n" class="w-4 h-4" />
              <component v-else :is="s.icon" class="w-4 h-4" />
            </div>
            <span class="text-sm" :class="step >= s.n ? 'text-theme-text font-medium' : 'text-theme-text-secondary'">{{ s.label }}</span>
          </div>
          <div v-if="i < steps.length - 1" class="flex-1 h-px mx-3 min-w-[24px]" :class="step > s.n ? 'bg-theme-success' : 'bg-theme-accent'" />
        </template>
      </div>

      <!-- ==================== STEP 1：选择岗位 ==================== -->
      <div v-if="step === 1" class="bg-theme-surface rounded-xl border border-theme-border p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-semibold flex items-center gap-2">
            <Target class="w-4 h-4" style="color: var(--theme-primary);" /> 选择目标岗位
          </h3>
          <span class="text-xs text-theme-text-secondary">AI 将基于岗位要求精准优化简历</span>
        </div>

        <div v-if="jobTargets.length" class="space-y-3 mb-4">
          <label
            v-for="t in jobTargets"
            :key="t.id"
            class="flex items-start gap-3 p-4 rounded-lg border cursor-pointer transition-all"
            :class="selectedTargetId === t.id ? 'border-theme-primary bg-theme-info-bg/50 ring-1 ring-theme-primary' : 'border-theme-border hover:border-theme-border'"
          >
            <input v-model="selectedTargetId" type="radio" :value="t.id" class="mt-1" />
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <strong class="text-sm">{{ t.position }}</strong>
                <span v-if="t.company" class="text-xs text-theme-text-secondary">{{ t.company }}</span>
                <span v-if="t.city" class="text-xs text-theme-text-secondary">· {{ t.city }}</span>
                <span v-if="t.jobType" class="text-[11px] px-2 py-0.5 rounded bg-theme-info-bg text-theme-primary">{{ t.jobType }}</span>
                <span v-if="t.isDefault === 1" class="text-[11px] px-2 py-0.5 rounded bg-theme-warning-bg text-theme-warning">默认</span>
              </div>
              <p class="text-xs text-theme-text-secondary mt-1 line-clamp-2 whitespace-pre-line">{{ t.jdText.slice(0, 120) }}{{ t.jdText.length > 120 ? '...' : '' }}</p>
            </div>
            <button class="text-theme-text-secondary hover:text-theme-danger flex-shrink-0" title="删除岗位" @click.prevent="t.id && removeJobTarget(t.id)">
              <Trash2 class="w-4 h-4" />
            </button>
          </label>
        </div>
        <div v-else class="text-center py-8 text-sm text-theme-text-secondary border border-dashed border-theme-border rounded-lg mb-4">
          暂无岗位目标，点击下方按钮创建（粘贴 BOSS/拉勾的岗位描述效果最佳）
        </div>

        <div class="flex justify-between">
          <button class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-border hover:border-theme-primary hover:text-theme-primary" @click="openJobModal">
            <Plus class="w-4 h-4" /> 新建岗位
          </button>
          <button
            class="inline-flex items-center gap-1.5 text-sm px-5 py-2 rounded-lg text-white font-medium disabled:opacity-50"
            style="background: var(--theme-primary);"
            :disabled="!selectedTargetId"
            @click="goStep2"
          >
            下一步：选择简历 <ArrowRight class="w-4 h-4" />
          </button>
        </div>
      </div>

      <!-- ==================== STEP 2：选择简历 ==================== -->
      <div v-else-if="step === 2" class="bg-theme-surface rounded-xl border border-theme-border p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-semibold flex items-center gap-2">
            <FolderOpen class="w-4 h-4" style="color: var(--theme-primary);" /> 选择要优化的简历
          </h3>
          <span v-if="selectedTarget" class="text-xs text-theme-text-secondary">目标岗位：{{ selectedTarget.position }}</span>
        </div>

        <div v-if="resumesLoading" class="py-10 text-center text-sm text-theme-text-secondary">加载中...</div>
        <div v-else-if="!resumes.length" class="py-10 text-center border border-dashed border-theme-border rounded-lg">
          <p class="text-sm text-theme-text-secondary mb-3">还没有在线简历</p>
          <button class="text-sm text-white px-4 py-2 rounded-lg" style="background: var(--theme-primary);" @click="router.push('/interview/resume/edit')">去创建</button>
        </div>
        <div v-else class="grid md:grid-cols-2 gap-3 mb-4">
          <label
            v-for="r in resumes"
            :key="r.id"
            class="p-4 rounded-lg border cursor-pointer transition-all"
            :class="selectedResumeId === r.id ? 'border-theme-primary bg-theme-info-bg/50 ring-1 ring-theme-primary' : 'border-theme-border hover:border-theme-border'"
          >
            <input v-model="selectedResumeId" type="radio" :value="r.id" class="hidden" />
            <div class="font-medium text-sm flex items-center gap-2">
              <FileText class="w-4 h-4 text-theme-text-secondary" /> {{ r.title }}
            </div>
            <div class="text-xs text-theme-text-secondary mt-1">
              v{{ r.versionNo || 1 }} · 更新于 {{ formatDate(r.updateTime, 'YYYY-MM-DD HH:mm') }}
              <span v-if="r.score" class="ml-1 text-theme-warning">⭐ {{ r.score }}分</span>
            </div>
          </label>
        </div>

        <div class="flex justify-between">
          <button class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary hover:border-theme-border" @click="step = 1">
            <ArrowLeft class="w-4 h-4" /> 返回岗位
          </button>
          <button
            class="inline-flex items-center gap-1.5 text-sm px-5 py-2 rounded-lg text-white font-medium disabled:opacity-50"
            style="background: var(--theme-primary);"
            :disabled="!selectedResumeId"
            @click="startAnalyze"
          >
            <Rocket class="w-4 h-4" /> 开始 AI 分析
          </button>
        </div>
      </div>

      <!-- ==================== STEP 3：分析进度（v10.18 抽离为 OptimizeProgress 组件） ==================== -->
      <OptimizeProgress
        v-else-if="step === 3"
        :analyzing="analyzing"
        :percent="progressPercent"
        :step="progressStep"
        :target-position="selectedTarget?.position"
      />

      <!-- ==================== STEP 4：匹配结果 + 深度优化对比 ==================== -->
      <div v-else-if="step === 4" class="space-y-4">
        <!-- 匹配摘要（v10.18 抽离为 JobMatchPanel 组件） -->
        <JobMatchPanel :report="matchReport" />

        <!-- 深度优化建议（v10.18 抽离为 OptimizeCompare 组件） -->
        <OptimizeCompare
          :result="optimizeResult"
          :adopted-set="adoptedSet"
          :optimizing="optimizing"
          :target-position="selectedTarget?.position"
          @generate="generateOptimize"
          @toggle="toggleAdopted"
          @adopt-all="adoptAll"
        />

        <div class="flex justify-between">
          <button class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="step = 2">
            <ArrowLeft class="w-4 h-4" /> 重新选择
          </button>
          <button
            class="inline-flex items-center gap-1.5 text-sm px-5 py-2 rounded-lg text-white font-medium disabled:opacity-50"
            style="background: var(--theme-primary);"
            :disabled="!optimizeResult || !adoptedSet.size"
            @click="goPreview"
          >
            预览最终结果 <ArrowRight class="w-4 h-4" />
          </button>
        </div>
      </div>

      <!-- ==================== STEP 5：预览保存 ==================== -->
      <div v-else-if="step === 5" class="space-y-4">
        <!-- 完整度（参考熊猫简历） -->
        <div class="bg-theme-surface rounded-xl border border-theme-border p-6">
          <div class="flex items-center gap-3 mb-3">
            <div class="flex-1">
              <div class="flex items-center justify-between text-sm mb-1.5">
                <span class="font-medium">简历完整度</span>
                <span class="font-bold text-lg" :style="{ color: completeness.percent >= 80 ? 'var(--theme-success)' : 'var(--theme-warning)' }">{{ completeness.percent }}%</span>
              </div>
              <div class="h-2.5 bg-theme-surface rounded-full overflow-hidden">
                <div class="h-full rounded-full transition-all" :style="{ width: completeness.percent + '%', background: completeness.percent >= 80 ? 'var(--theme-success)' : 'var(--theme-warning)' }"></div>
              </div>
              <div class="text-xs text-theme-text-secondary mt-1.5">
                {{ completeness.todos.length ? `有 ${completeness.todos.length} 项待核对` : '太棒了，简历信息完整！' }}
              </div>
            </div>
          </div>
          <div v-if="completeness.todos.length" class="space-y-1.5">
            <div v-for="todo in completeness.todos" :key="todo.label" class="flex items-start gap-2 text-xs text-theme-text-secondary">
              <AlertCircle class="w-3.5 h-3.5 text-theme-warning mt-0.5 flex-shrink-0" />
              <span><strong class="text-theme-text">{{ todo.label }}</strong>：{{ todo.why }}</span>
            </div>
          </div>
        </div>

        <!-- 最终预览 -->
        <div class="bg-theme-surface rounded-xl border border-theme-border p-6">
          <div class="flex items-center justify-between flex-wrap gap-2 mb-4">
            <h3 class="font-semibold flex items-center gap-2">
              <PartyPopper class="w-4 h-4 text-theme-success" /> 优化完成 · 预览最终简历
            </h3>
            <span class="text-xs text-theme-text-secondary">已应用 {{ adoptedSet.size }} 项优化</span>
          </div>

          <div v-if="selectedTarget" class="text-sm bg-theme-info-bg border border-theme-info-bg rounded-lg p-3 mb-4">
            <strong>目标岗位：</strong>{{ selectedTarget.position }}{{ selectedTarget.company ? ' · ' + selectedTarget.company : '' }}
            <span class="ml-3 text-xs text-theme-text-secondary">
              匹配度：<strong :style="{ color: (matchReport?.matchScore ?? 0) >= 70 ? 'var(--theme-success)' : 'var(--theme-warning)' }">{{ matchReport?.matchScore }}%</strong>
              <template v-if="rematchedScore !== null">
                → 优化后 <strong :style="{ color: rematchedScore >= 70 ? 'var(--theme-success)' : 'var(--theme-warning)' }">{{ rematchedScore }}%</strong>
              </template>
            </span>
          </div>

          <div v-if="previewResume" class="border border-theme-border rounded-lg p-6 max-h-[480px] overflow-y-auto">
            <div class="text-center border-b pb-3 mb-4">
              <div class="text-xl font-bold">{{ previewResume.name || '未填写姓名' }}</div>
              <div class="text-xs text-theme-text-secondary mt-1">
                {{ previewResume.phone || '未填手机' }} · {{ previewResume.email || '未填邮箱' }}
                <template v-if="previewResume.jobIntention?.city"> · {{ previewResume.jobIntention.city }}</template>
              </div>
              <div v-if="previewResume.jobIntention?.position" class="text-sm mt-1.5" style="color: var(--theme-primary); font-weight: 600;">
                求职意向：{{ previewResume.jobIntention.position }}
              </div>
            </div>

            <div v-if="previewResume.works?.length" class="mb-4">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">工作经历</div>
              <div v-for="(w, i) in previewResume.works" :key="i" class="mb-3">
                <div class="text-sm font-medium flex justify-between">
                  <span>{{ w.company }} · {{ w.position }}</span>
                  <span class="text-xs text-theme-text-secondary">{{ w.startDate }} - {{ w.endDate || '至今' }}</span>
                </div>
                <p class="text-xs text-theme-text-secondary mt-1 leading-relaxed whitespace-pre-line">{{ w.description }}</p>
              </div>
            </div>

            <div v-if="previewResume.projects?.length" class="mb-4">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">项目经历</div>
              <div v-for="(p, i) in previewResume.projects" :key="i" class="mb-3">
                <div class="text-sm font-medium">{{ p.name }}<span v-if="p.role" class="text-theme-text-secondary font-normal"> · {{ p.role }}</span></div>
                <p class="text-xs text-theme-text-secondary mt-1 leading-relaxed whitespace-pre-line">{{ p.description }}</p>
              </div>
            </div>

            <div v-if="previewResume.skills?.length" class="mb-4">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">专业技能</div>
              <div class="flex flex-wrap gap-1.5">
                <span v-for="(s, i) in previewResume.skills" :key="i" class="text-xs px-2 py-1 rounded bg-theme-surface text-theme-text">
                  {{ s.name }}<span v-if="s.level" class="text-theme-text-secondary">（{{ s.level }}）</span>
                </span>
              </div>
            </div>

            <div v-if="previewResume.selfIntro">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">自我评价</div>
              <p class="text-xs text-theme-text-secondary leading-relaxed whitespace-pre-line">{{ previewResume.selfIntro }}</p>
            </div>
          </div>

          <div class="flex justify-between items-center flex-wrap gap-3 pt-4 mt-4 border-t">
            <button class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="step = 4">
              <ArrowLeft class="w-4 h-4" /> 返回对比
            </button>
            <div class="flex gap-2 flex-wrap">
              <button v-if="savedResumeId" class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="gotoEditResume">
                <PencilIcon class="w-4 h-4" /> 去微调
              </button>
              <button
                class="inline-flex items-center gap-1.5 text-sm px-5 py-2 rounded-lg text-white font-medium disabled:opacity-50"
                style="background: var(--theme-primary);"
                :disabled="saving || !!savedResumeId"
                @click="saveOptimize"
              >
                <Save class="w-4 h-4" /> {{ savedResumeId ? '已保存' : saving ? '保存中...' : '保存优化结果' }}
              </button>
              <button
                v-if="savedResumeId"
                class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-warning text-theme-warning font-medium disabled:opacity-50"
                :disabled="rematching"
                @click="rematch"
              >
                <Target class="w-4 h-4" /> {{ rematching ? '分析中...' : rematchedScore !== null ? `匹配度 ${rematchedScore}%` : '重新匹配' }}
              </button>
              <button
                v-if="savedResumeId"
                class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg text-white font-medium disabled:opacity-50"
                style="background: var(--theme-success);"
                :disabled="rescoredScore !== null"
                @click="rescore"
              >
                <Star class="w-4 h-4" /> {{ rescoredScore !== null ? `最终评分 ${rescoredScore} 分` : '重新评分' }}
              </button>
            </div>

            <!-- 优化历史：评分前后对比 -->
            <div v-if="optimizeHistory.length" class="border-t border-theme-border pt-4 mt-4">
              <div class="flex items-center justify-between mb-2">
                <h4 class="text-sm font-semibold text-theme-text flex items-center gap-1.5">
                  <History class="w-4 h-4" style="color: var(--theme-primary);" /> 优化历史（{{ optimizeHistory.length }} 次）
                </h4>
                <button class="text-xs text-theme-text-secondary hover:text-theme-primary" :disabled="historyLoading" @click="loadOptimizeHistory">刷新</button>
              </div>
              <div class="max-h-48 overflow-y-auto divide-y divide-theme-border">
                <div v-for="h in visibleHistory" :key="h.id" class="py-2 flex items-center justify-between text-xs cursor-pointer hover:bg-theme-muted/40 px-1 rounded" @click="goEditFromHistory(h)">
                  <span class="text-theme-text-secondary">{{ formatHistoryTime(h.createTime) }}</span>
                  <div class="flex items-center gap-3">
                    <span v-if="h.scoreBefore != null || h.scoreAfter != null" class="text-theme-text">
                      评分 {{ h.scoreBefore ?? '-' }} → <span class="font-semibold" :style="{ color: scoreDelta(h) > 0 ? 'var(--theme-success)' : scoreDelta(h) < 0 ? 'var(--theme-danger)' : 'inherit' }">{{ h.scoreAfter ?? '-' }}</span>
                    </span>
                    <span v-if="h.matchScoreBefore != null || h.matchScoreAfter != null" class="text-theme-text-secondary">
                      匹配 {{ h.matchScoreBefore ?? '-' }}% → {{ h.matchScoreAfter ?? '-' }}%
                    </span>
                  </div>
                </div>
                <button v-if="optimizeHistory.length > historyVisibleCount" class="w-full py-2 text-xs text-theme-primary hover:underline" @click="historyVisibleCount += 5">
                  展开更多（{{ optimizeHistory.length - historyVisibleCount }} 条）
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建岗位弹窗（v10.18 抽离为 JobTargetForm 组件） -->
    <JobTargetForm
      v-model:visible="jobModalVisible"
      v-model:form="jobForm"
      :saving="jobSaving"
      :job-type-options="JOB_TYPE_OPTIONS"
      @save="saveJobTarget"
    />

    <!-- 评分报告弹窗（v10.18 阶段五：评分报告存档 + 优化历史整合） -->
    <ScoreReportDialog
      v-model:visible="scoreReportVisible"
      :reports="scoreReports"
      :history="optimizeHistory"
      :loading="scoreReportLoading"
      @refresh="refreshScoreReports"
      @select-history="goEditFromHistory"
    />

    <SiteFooter />
  </div>
</template>
