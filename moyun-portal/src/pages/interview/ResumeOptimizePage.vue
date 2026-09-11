<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
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
import FieldRegenerateDialog from '@/components/resume/FieldRegenerateDialog.vue';
import { aiFieldAssist } from '@/api/resumeOptimize';
import { generateSeo } from '@/utils/seo';
import { getMyResumeList, scoreResume, getResumeDetail } from '@/api/interview';
import {
  getJobTargets, createJobTarget, deleteJobTarget,
  applyDeepOptimize, getOptimizeHistory,
  saveScoreReport, getScoreReports,
  submitDeepOptimizeTask, getDeepOptimizeTaskStatus,
} from '@/api/resumeOptimize';
import { submitAiTask, pollAiTask } from '@/api/aiTask';
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
/** v10.23：岗位匹配异步任务 ID（用于 URL 参数化与刷新恢复轮询） */
const matchTaskId = ref<number | string | null>(null);

// 深度优化
const optimizing = ref(false);
const optimizeResult = ref<ResumeDeepOptimizeVO | null>(null);
const adoptedSet = ref<Set<number>>(new Set());
/** v10.21：当前深度优化异步任务ID（用于刷新页面后恢复轮询） */
const currentAsyncTaskId = ref<number | string | null>(null);

// 保存
const saving = ref(false);
const savedResumeId = ref<number | string | null>(null);
const rescoredScore = ref<number | null>(null);
const rematchedScore = ref<number | null>(null);
const rematching = ref(false);

// ==================== v10.21：页面状态持久化（刷新后恢复原状态） ====================
// 持久化关键状态到 localStorage，按 resumeId 分 key 避免多简历串扰。
// 含 step/selectedTargetId/matchReport/optimizeResult/adoptedSet/savedResumeId/currentAsyncTaskId
// 若异步任务进行中刷新，恢复后自动继续轮询任务状态直到 success/failed。
const STATE_STORAGE_PREFIX = 'moyun:resume-optimize:state:';
const STATE_STORAGE_TTL = 2 * 60 * 60 * 1000; // 2 小时过期（防止陈旧状态误恢复）

interface PersistedState {
  ts: number;
  step: number;
  selectedTargetId: number | string | null;
  matchReport: ResumeJobMatchReport | null;
  optimizeResult: ResumeDeepOptimizeVO | null;
  adoptedIndexes: number[];
  savedResumeId: number | string | null;
  asyncTaskId: number | string | null;
  /** v10.23：岗位匹配异步任务 ID（刷新恢复轮询用；旧快照可能缺失） */
  matchTaskId?: number | string | null;
}

function stateStorageKey(resumeId: number | string | null): string | null {
  if (resumeId === null || resumeId === undefined || resumeId === '') return null;
  return `${STATE_STORAGE_PREFIX}${resumeId}`;
}

function saveStateToStorage() {
  const key = stateStorageKey(selectedResumeId.value);
  if (!key) return;
  const state: PersistedState = {
    ts: Date.now(),
    step: step.value,
    selectedTargetId: selectedTargetId.value,
    matchReport: matchReport.value,
    optimizeResult: optimizeResult.value,
    adoptedIndexes: Array.from(adoptedSet.value),
    savedResumeId: savedResumeId.value,
    asyncTaskId: currentAsyncTaskId.value,
    matchTaskId: matchTaskId.value,
  };
  try {
    localStorage.setItem(key, JSON.stringify(state));
  } catch {
    // 容量超限或隐私模式，静默忽略（不影响功能）
  }
}

function loadStateFromStorage(resumeId: number | string | null): PersistedState | null {
  const key = stateStorageKey(resumeId);
  if (!key) return null;
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    const state = JSON.parse(raw) as PersistedState;
    if (Date.now() - state.ts > STATE_STORAGE_TTL) {
      localStorage.removeItem(key);
      return null;
    }
    return state;
  } catch {
    return null;
  }
}

function clearStateFromStorage(resumeId: number | string | null) {
  const key = stateStorageKey(resumeId);
  if (key) localStorage.removeItem(key);
}

/** 将持久化的状态恢复到各 ref */
function applyRestoredState(state: PersistedState) {
  step.value = state.step;
  if (state.selectedTargetId !== null && state.selectedTargetId !== undefined) {
    selectedTargetId.value = state.selectedTargetId;
  }
  matchReport.value = state.matchReport;
  optimizeResult.value = state.optimizeResult;
  adoptedSet.value = new Set(state.adoptedIndexes || []);
  savedResumeId.value = state.savedResumeId;
  currentAsyncTaskId.value = state.asyncTaskId;
  matchTaskId.value = state.matchTaskId ?? null;
  // v10.23：报告已就绪但停在分析页（任务完成瞬间的快照），直接进步骤4
  if (matchReport.value && step.value === 3) step.value = 4;
}

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

  // v10.21：刷新页面后恢复原状态（不重新开始）
  // 必须在 selectedResumeId 设置之后（持久化 key 按 resumeId 分）
  const restored = loadStateFromStorage(selectedResumeId.value);
  if (restored) {
    applyRestoredState(restored);
  }

  // v10.23：URL 参数优先于 localStorage 快照（支持多次刷新/分享恢复）
  const qStep = Number(route.query.step);
  if (Number.isInteger(qStep) && qStep >= 1 && qStep <= 5) step.value = qStep;
  const qTargetId = route.query.targetId as string | undefined;
  if (qTargetId) selectedTargetId.value = qTargetId;
  const qTaskId = route.query.taskId as string | undefined;
  if (qTaskId) currentAsyncTaskId.value = qTaskId;
  const qMatchTaskId = route.query.matchTaskId as string | undefined;
  if (qMatchTaskId) matchTaskId.value = qMatchTaskId;

  // 深度优化异步任务进行中（pending/running），恢复轮询
  if (currentAsyncTaskId.value !== null && currentAsyncTaskId.value !== undefined
      && currentAsyncTaskId.value !== '' && step.value === 4) {
    resumeAsyncPolling(currentAsyncTaskId.value);
  }
  // v10.23：匹配任务进行中且尚无报告，恢复轮询（含进度动画）
  if (matchTaskId.value !== null && matchTaskId.value !== undefined
      && matchTaskId.value !== '' && !matchReport.value) {
    resumeMatchPolling(matchTaskId.value);
  }

  loadOptimizeHistory();

  // v10.23：恢复完成后开启 URL 同步并做一次初始同步
  urlSyncReady = true;
  syncStateToUrl();
});

watch(selectedResumeId, (newId, oldId) => {
  loadOptimizeHistory();
  // v10.21：切换简历时恢复对应的状态快照（或重置）
  if (newId === oldId) return;
  const restored = loadStateFromStorage(newId);
  if (restored) {
    applyRestoredState(restored);
    if (restored.asyncTaskId !== null && restored.asyncTaskId !== undefined
        && restored.asyncTaskId !== '' && restored.step === 4) {
      resumeAsyncPolling(restored.asyncTaskId);
    }
    // v10.23：匹配任务进行中且尚无报告，恢复轮询
    if (restored.matchTaskId !== null && restored.matchTaskId !== undefined
        && restored.matchTaskId !== '' && !restored.matchReport) {
      resumeMatchPolling(restored.matchTaskId);
    }
  } else {
    // 新简历无快照：重置到 step1（避免上一简历状态残留）
    step.value = 1;
    matchReport.value = null;
    optimizeResult.value = null;
    adoptedSet.value = new Set();
    savedResumeId.value = null;
    currentAsyncTaskId.value = null;
    matchTaskId.value = null; // v10.23：旧匹配任务结果作废（轮询回调会丢弃）
    stopOptimizePolling();
    stopMatchProgress();
    optimizing.value = false;
    analyzing.value = false;
  }
});

async function loadJobTargets() {
  try {
    const res = await getJobTargets();
    if (res.code === 200 && res.data) {
      jobTargets.value = res.data;
      // v10.21：仅在未选中时设置默认岗位（避免覆盖已从持久化恢复的 selectedTargetId）
      const def = res.data.find(t => t.isDefault === 1) || res.data[0];
      if (def?.id && !selectedTargetId.value) selectedTargetId.value = def.id;
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

// ==================== STEP2 → STEP3：分析（v10.23 岗位匹配异步任务化） ====================

// 匹配进度动画定时器（组件卸载/任务结束时清理）
let matchProgressTimer: ReturnType<typeof setInterval> | null = null;
/** 匹配任务轮询进行中标志（防止刷新恢复逻辑重复触发轮询） */
let matchPollingActive = false;

function stopMatchProgress() {
  if (matchProgressTimer) {
    clearInterval(matchProgressTimer);
    matchProgressTimer = null;
  }
}

/** 启动匹配进度动画（上限 90 等待后端确认） */
function startMatchProgress() {
  matchProgressTimer = setInterval(() => {
    if (progressPercent.value < 90) {
      progressPercent.value += Math.random() * 8 + 2;
      progressStep.value = Math.min(4, Math.floor(progressPercent.value / 25) + 1);
    }
  }, 600);
}

/**
 * v10.23：岗位匹配通用 AI 异步任务（startAnalyze / rematch 公共方法）
 * 提交 job_match 任务 → 轮询到 success 返回匹配报告；失败/超时抛错（含任务 error 信息）
 */
async function runMatchAsync(resumeId: number | string, jobTargetId: number | string): Promise<ResumeJobMatchReport> {
  const submitRes = await submitAiTask('job_match', { resumeId, jobTargetId });
  if (submitRes.code !== 200 || !submitRes.data?.taskId) {
    throw new Error(submitRes.message || '提交匹配任务失败');
  }
  // 记录 taskId（URL/持久化恢复用）
  matchTaskId.value = submitRes.data.taskId;
  saveStateToStorage();
  try {
    return await pollAiTask<ResumeJobMatchReport>(submitRes.data.taskId);
  } finally {
    // 任务结束（成功/失败）后清 taskId
    matchTaskId.value = null;
    saveStateToStorage();
  }
}

/**
 * v10.23：刷新恢复匹配轮询（URL/storage 里的 matchTaskId 仍 pending/running 时）
 * 恢复 loading 动画 + 继续轮询；完成写回 matchReport 并清 matchTaskId
 */
function resumeMatchPolling(taskId: number | string) {
  if (matchPollingActive) return; // 已在轮询，避免重复恢复
  matchPollingActive = true;
  analyzing.value = true;
  progressPercent.value = 10; // 至少给个起始进度
  progressStep.value = 1;
  matchTaskId.value = taskId;
  stopMatchProgress();
  startMatchProgress();

  pollAiTask<ResumeJobMatchReport>(taskId)
    .then((report) => {
      matchPollingActive = false;
      if (matchTaskId.value !== taskId) return; // 已切换简历/取消，丢弃过期结果
      stopMatchProgress();
      matchReport.value = report;
      matchTaskId.value = null;
      progressPercent.value = 100;
      progressStep.value = 5;
      saveStateToStorage();
      setTimeout(() => {
        analyzing.value = false;
        step.value = 4;
      }, 500);
    })
    .catch((e: unknown) => {
      matchPollingActive = false;
      if (matchTaskId.value !== taskId) return; // 已切换简历/取消，不弹错误
      stopMatchProgress();
      analyzing.value = false;
      matchTaskId.value = null;
      saveStateToStorage();
      toast.error((e as Error)?.message || '匹配任务失败');
    });
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
  matchTaskId.value = null;
  stopMatchProgress();
  startMatchProgress();

  try {
    // v10.23：岗位匹配改为通用 AI 异步任务（提交 → 轮询）
    const report = await runMatchAsync(selectedResumeId.value, selectedTargetId.value!);
    matchReport.value = report;
    progressPercent.value = 100;
    progressStep.value = 5;
    setTimeout(() => {
      analyzing.value = false;
      step.value = 4;
    }, 500);
  } catch (e: unknown) {
    analyzing.value = false;
    stopMatchProgress();
    toast.error((e as Error)?.message || '分析失败');
    step.value = 2;
    return;
  }
  stopMatchProgress();
}

// ==================== STEP4：深度优化对比 ====================
const gradeLabel: Record<string, string> = {
  excellent: '优秀匹配', good: '良好匹配', medium: '中等匹配', poor: '匹配较弱',
};

// v10.19：深度优化异步任务轮询定时器（组件卸载时需清理）
let optimizePollingTimer: ReturnType<typeof setInterval> | null = null;
let optimizeProgressTimer: ReturnType<typeof setInterval> | null = null;

function stopOptimizePolling() {
  if (optimizeProgressTimer) {
    clearInterval(optimizeProgressTimer);
    optimizeProgressTimer = null;
  }
  if (optimizePollingTimer) {
    clearInterval(optimizePollingTimer);
    optimizePollingTimer = null;
  }
}

async function generateOptimize() {
  if (!selectedResumeId.value || !selectedTargetId.value) return;
  // 清理上一次的轮询（防止重复触发）
  stopOptimizePolling();
  optimizePollingActive = false;

  optimizing.value = true;
  optimizeResult.value = null;
  adoptedSet.value = new Set();
  currentAsyncTaskId.value = null;
  progressPercent.value = 0;
  progressStep.value = 1;

  try {
    // 1. 提交异步任务（立即返回 taskId）
    const submitRes = await submitDeepOptimizeTask(selectedResumeId.value, selectedTargetId.value);
    if (submitRes.code !== 200 || !submitRes.data) {
      optimizing.value = false;
      toast.error(submitRes.message || '提交失败');
      return;
    }
    const taskId = submitRes.data;
    // v10.21：记录 taskId 到 ref + 持久化，刷新页面后可据此恢复轮询
    currentAsyncTaskId.value = taskId;
    saveStateToStorage();

    // 2. 进度动画（pending→running 阶段渐进，给用户视觉反馈）
    optimizeProgressTimer = setInterval(() => {
      if (progressPercent.value < 90) {
        progressPercent.value += Math.random() * 5 + 1;
        progressStep.value = Math.min(4, Math.floor(progressPercent.value / 25) + 1);
      }
    }, 800);

    // 3. 任务状态轮询（4 秒一次）
    optimizePollingTimer = setInterval(() => pollOptimizeTaskStatus(taskId), 4000);
    // 立即触发一次（避免等 4 秒才看到状态变化）
    pollOptimizeTaskStatus(taskId);
  } catch (e: unknown) {
    optimizing.value = false;
    currentAsyncTaskId.value = null;
    stopOptimizePolling();
    toast.error((e as Error)?.message || '提交失败');
  }
}

/** 轮询深度优化任务状态 */
async function pollOptimizeTaskStatus(taskId: number | string) {
  try {
    const res = await getDeepOptimizeTaskStatus(taskId);
    if (res.code !== 200 || !res.data) return;

    const task = res.data;
    // 同步后端进度（取后端返回与前端动画的较大值，避免倒退）
    if (task.progress > progressPercent.value) {
      progressPercent.value = task.progress;
      progressStep.value = Math.min(4, Math.floor(progressPercent.value / 25) + 1);
    }

    if (task.status === 'success' && task.result) {
      stopOptimizePolling();
      optimizePollingActive = false;
      progressPercent.value = 100;
      progressStep.value = 5;
      optimizeResult.value = task.result;
      adoptedSet.value = new Set();
      // v10.21：任务完成，清除 taskId（不再需要恢复轮询），持久化最新结果
      currentAsyncTaskId.value = null;
      saveStateToStorage();
      toast.success(`已生成 ${task.result.items.length} 项优化建议`);
      setTimeout(() => { optimizing.value = false; }, 500);
    } else if (task.status === 'failed') {
      stopOptimizePolling();
      optimizePollingActive = false;
      optimizing.value = false;
      currentAsyncTaskId.value = null;
      saveStateToStorage();
      toast.error(task.errorMsg || 'AI 生成失败，请稍后重试');
    }
    // pending / running 继续轮询
  } catch (e: unknown) {
    // 网络偶发异常不中断轮询，下次自动重试
    console.warn('[optimize] 轮询失败，将重试', e);
  }
}

/**
 * v10.21：恢复异步轮询（页面刷新后，若 taskId 仍 pending/running 则继续轮询）
 * 用于 onMounted 中检测到持久化的 asyncTaskId 时重建轮询
 * v10.23：加 active 守卫，避免 onMounted 与切换简历 watch 双触发导致重复轮询
 */
let optimizePollingActive = false;

function resumeAsyncPolling(taskId: number | string) {
  if (optimizePollingActive) return; // 已在轮询，避免重复恢复
  optimizePollingActive = true;
  optimizing.value = true;
  progressPercent.value = 10; // 至少给个起始进度
  progressStep.value = 1;
  currentAsyncTaskId.value = taskId;

  // 进度动画（与 generateOptimize 一致，上限 90 等待后端确认）
  optimizeProgressTimer = setInterval(() => {
    if (progressPercent.value < 90) {
      progressPercent.value += Math.random() * 5 + 1;
      progressStep.value = Math.min(4, Math.floor(progressPercent.value / 25) + 1);
    }
  }, 800);

  optimizePollingTimer = setInterval(() => pollOptimizeTaskStatus(taskId), 4000);
  // 立即触发一次，确认任务实际状态（可能已完成，直接收尾）
  pollOptimizeTaskStatus(taskId);
}

onUnmounted(() => {
  stopOptimizePolling();
  stopMatchProgress();
});

// v10.21：关键状态变化时自动持久化（刷新页面可恢复）
// 深度监听对象/集合内部变化，保存最新快照到 localStorage
// v10.23：新增 matchTaskId（岗位匹配异步任务恢复用）
watch(
  [step, selectedTargetId, matchReport, optimizeResult, adoptedSet, savedResumeId, currentAsyncTaskId, matchTaskId],
  () => { saveStateToStorage(); },
  { deep: true },
);

// ==================== v10.23：URL 参数化（关键状态同步到 query，支持多次刷新恢复） ====================
// watch 监听的是本地 ref，router.replace 只改 query 不会再次触发本 watch，无死循环；
// onMounted 恢复阶段（urlSyncReady=false）跳过，避免恢复值反向覆盖 URL。

/** onMounted 恢复完成前不同步 URL */
let urlSyncReady = false;

/** 将关键状态同步到 URL query（router.replace 保持不产生历史记录，保留无关参数） */
function syncStateToUrl() {
  // 保留当前 query 中的无关参数（如外链带参）
  const next: Record<string, string> = {};
  for (const [k, v] of Object.entries(route.query)) {
    if (typeof v === 'string' && v) next[k] = v;
  }
  const apply = (key: string, val: unknown) => {
    if (val !== null && val !== undefined && val !== '') {
      next[key] = String(val);
    } else {
      delete next[key]; // 值为空时从 query 中删除该键
    }
  };
  apply('step', step.value);
  apply('resumeId', selectedResumeId.value);
  apply('targetId', selectedTargetId.value);
  apply('taskId', currentAsyncTaskId.value);
  apply('matchTaskId', matchTaskId.value);
  // query 无变化时跳过，避免无效 replace
  const currentKeys = Object.keys(route.query).filter((k) => typeof route.query[k] === 'string');
  const nextKeys = Object.keys(next);
  const unchanged = currentKeys.length === nextKeys.length
    && nextKeys.every((k) => route.query[k] === next[k]);
  if (unchanged) return;
  router.replace({ query: next });
}

watch(
  [step, selectedResumeId, selectedTargetId, currentAsyncTaskId, matchTaskId],
  () => {
    if (!urlSyncReady) return;
    syncStateToUrl();
  },
);

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

// v10.20：step4 内就地预览面板（不跳转 step5）
const quickPreviewVisible = ref(false);
const quickPreviewBaseResume = ref<UserResumeVO | null>(null);
const quickPreviewLoading = ref(false);
const quickPreviewResume = computed<UserResumeVO | null>(() => {
  if (!quickPreviewVisible.value || !quickPreviewBaseResume.value) return null;
  // 复用 applyOptimizes 计算采纳后的简历（每次 adoptedSet 变化自动重算）
  return applyOptimizes(quickPreviewBaseResume.value);
});

async function toggleQuickPreview() {
  quickPreviewVisible.value = !quickPreviewVisible.value;
  if (quickPreviewVisible.value && !quickPreviewBaseResume.value && selectedResumeId.value) {
    // 首次展开加载完整简历详情
    quickPreviewLoading.value = true;
    try {
      const res = await getResumeDetail(selectedResumeId.value);
      if (res.code === 200 && res.data) {
        quickPreviewBaseResume.value = res.data;
      }
    } catch (e) {
      toast.error((e as Error)?.message || '加载简历失败');
      quickPreviewVisible.value = false;
    } finally {
      quickPreviewLoading.value = false;
    }
  }
}

// v10.20：单字段重新生成候选弹窗状态
const regenDialogVisible = ref(false);
const regenLoading = ref(false);
const regenCandidates = ref<string[]>([]);
const regenErrorMsg = ref('');
const regenTargetIdx = ref(-1);
const regeneratingIdx = ref(-1); // 卡片级 loading 索引

const SECTION_LABEL_FOR_REGEN: Record<string, string> = {
  objective: '求职意向', education: '教育背景', work: '工作经历',
  project: '项目经历', skills: '专业技能', selfIntro: '自我评价',
};

const FIELD_LABEL_FOR_REGEN: Record<string, string> = {
  position: '岗位', description: '描述', name: '名称',
};

const regenDialogTitle = computed(() => {
  if (!optimizeResult.value || regenTargetIdx.value < 0) return '';
  const item = optimizeResult.value.items[regenTargetIdx.value];
  if (!item) return '';
  const base = SECTION_LABEL_FOR_REGEN[item.section] || item.section;
  const idxPart = (item.section === 'selfIntro' || item.section === 'objective' || item.section === 'skills')
    ? '' : ` #${(item.index ?? 0) + 1}`;
  const fieldPart = item.field ? ` · ${FIELD_LABEL_FOR_REGEN[item.field] || item.field}` : '';
  return `${base}${idxPart}${fieldPart}`;
});

const regenOriginalText = computed(() => {
  if (!optimizeResult.value || regenTargetIdx.value < 0) return '';
  const item = optimizeResult.value.items[regenTargetIdx.value];
  return item?.original || '';
});

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

// v10.20：单字段重新生成（拉取 3 候选版本弹窗）
async function regenerateItem(idx: number) {
  if (!optimizeResult.value || idx < 0) return;
  const item = optimizeResult.value.items[idx];
  if (!item) return;

  // 映射 section+field 到 aiFieldAssist 支持的 field 类型
  const fieldKey = item.section === 'selfIntro'
    ? 'self_intro'
    : item.section === 'skills'
      ? 'skills'
      : item.section === 'work'
        ? 'work_description'
        : item.section === 'project'
          ? 'project_description'
          : null;

  if (!fieldKey) {
    toast.error('该字段暂不支持单字段重新生成');
    return;
  }

  regenTargetIdx.value = idx;
  regenCandidates.value = [];
  regenErrorMsg.value = '';
  regenDialogVisible.value = true;
  regenLoading.value = true;
  regeneratingIdx.value = idx;

  try {
    const res = await aiFieldAssist({
      field: fieldKey as 'work_description' | 'project_description' | 'self_intro' | 'skills',
      originalText: item.original || '',
      position: selectedTarget.value?.position,
      skillNames: item.section === 'skills' ? extractSkillNames(item.original) : undefined,
    });
    if (res.code === 200 && res.data && res.data.length) {
      regenCandidates.value = res.data.map(s => s.text);
    } else {
      regenErrorMsg.value = res.message || 'AI 未返回候选版本';
    }
  } catch (e) {
    regenErrorMsg.value = (e as Error)?.message || 'AI 调用失败，请稍后重试';
  } finally {
    regenLoading.value = false;
    regeneratingIdx.value = -1;
  }
}

/** 从原文提取技能名（用于 skills 重新生成时传给 AI） */
function extractSkillNames(text: string): string[] {
  if (!text) return [];
  const names: string[] = [];
  for (const rawLine of text.split('\n')) {
    const line = rawLine.trim();
    if (!line) continue;
    const m = line.match(/^(精通|熟练|了解|一般)[:：]\s*(.+)$/);
    if (m) {
      for (const n of m[2].split(/[、,，]/)) {
        const t = n.trim();
        if (t) names.push(t);
      }
    } else {
      for (const n of line.split(/[、,，]/)) {
        const t = n.trim();
        if (t) names.push(t);
      }
    }
  }
  return names;
}

/** 应用重新生成的候选版本（替换该条建议的 optimized 内容） */
function applyRegenCandidate(text: string) {
  if (!optimizeResult.value || regenTargetIdx.value < 0) return;
  const idx = regenTargetIdx.value;
  const item = optimizeResult.value.items[idx];
  if (!item) return;
  // 不可变替换：重建 items 数组，触发 Vue 响应式更新
  const newItem = { ...item, optimized: text };
  const newItems = optimizeResult.value.items.slice();
  newItems[idx] = newItem;
  optimizeResult.value = { ...optimizeResult.value, items: newItems };
  // 同步刷新就地预览
  if (quickPreviewBaseResume.value) {
    quickPreviewBaseResume.value = { ...quickPreviewBaseResume.value };
  }
  toast.success('已应用新版本');
}

/** 前端预演：将采纳的建议应用到简历副本（用于预览与完整度计算） */
function applyOptimizes(source: UserResumeVO): UserResumeVO {
  const r: UserResumeVO = JSON.parse(JSON.stringify(source));
  if (!optimizeResult.value) return r;
  optimizeResult.value.items.forEach((item, i) => {
    if (!adoptedSet.value.has(i)) return;
    const text = item.optimized.trim();
    const idx = item.index ?? 0;
    // v10.20：section 归一化（与后端 normalizeSection 保持一致，兼容 LLM 返回 works/projects 等变体）
    const section = normalizeSection(item.section);
    const field = (item.field ?? '').trim();
    switch (section) {
      case 'selfIntro': r.selfIntro = text; break;
      case 'objective':
        if (r.jobIntention && field === 'position') r.jobIntention.position = text;
        break;
      case 'education':
        if (r.educations?.[idx] && field === 'description') r.educations[idx].description = text;
        break;
      case 'work':
        if (r.works?.[idx] && field === 'description') r.works[idx].description = text;
        break;
      case 'project':
        if (r.projects?.[idx] && field === 'description') r.projects[idx].description = text;
        break;
      case 'skills': appendSkills(r, text); break;
    }
  });
  return r;
}

/**
 * v10.20：section 归一化（与后端 ResumeDeepOptimizeService.normalizeSection 保持一致）
 * 兼容 LLM 返回 works/projects/experience/self_intro 等变体，避免 case 走不到导致采纳无效
 */
function normalizeSection(raw?: string | null): string {
  if (!raw) return '';
  const s = raw.trim().toLowerCase();
  switch (s) {
    case 'works':
    case 'experience':
    case 'experiences':
    case 'working':
      return 'work';
    case 'projects':
    case 'project_experience':
      return 'project';
    case 'educations':
    case 'education_experience':
      return 'education';
    case 'self_intro':
    case 'selfintro':
    case 'selfintroduction':
    case 'introduction':
    case 'intro':
    case 'summary':
      return 'selfIntro';
    case 'job_intention':
    case 'jobintention':
    case 'intention':
      return 'objective';
    case 'skill':
    case 'skill_list':
    case 'skilllist':
      return 'skills';
    default:
      return s;
  }
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

// 重新匹配分析（对比优化前后匹配度变化；v10.23 改为通用 AI 异步任务）
async function rematch() {
  if (!savedResumeId.value || !selectedTargetId.value) {
    toast.error('请先保存优化结果');
    return;
  }
  try {
    rematching.value = true;
    const report = await runMatchAsync(savedResumeId.value, selectedTargetId.value);
    rematchedScore.value = report.matchScore;
    toast.success('重新匹配完成');
  } catch (e) {
    toast.error((e as Error)?.message || '匹配失败');
  } finally {
    rematching.value = false;
  }
}

function gotoEditResume() {
  if (savedResumeId.value) router.push(`/interview/resume/edit/${savedResumeId.value}`);
}

/** v11.x 闭环：优化后的简历 → 去面试（语音面试页按 resumeId 预选该简历） */
function gotoInterview() {
  if (savedResumeId.value) router.push(`/interview/voice?resumeId=${savedResumeId.value}`);
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

        <!-- 深度优化建议（v10.18 抽离为 OptimizeCompare 组件 / v10.20 交互优化） -->
        <OptimizeCompare
          :result="optimizeResult"
          :adopted-set="adoptedSet"
          :optimizing="optimizing"
          :target-position="selectedTarget?.position"
          :preview-visible="quickPreviewVisible"
          :regenerating-idx="regeneratingIdx"
          @generate="generateOptimize"
          @toggle="toggleAdopted"
          @adopt-all="adoptAll"
          @regenerate="regenerateItem"
          @preview="toggleQuickPreview"
        />

        <!-- v10.20：step4 内就地预览面板（不跳转 step5，采纳后实时反映） -->
        <div v-if="quickPreviewVisible" class="bg-theme-surface rounded-xl border border-theme-border p-6 mt-4">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-semibold flex items-center gap-2">
              <Eye class="w-4 h-4" style="color: var(--theme-primary);" /> 采纳结果预览（{{ adoptedSet.size }} 项已应用）
            </h3>
            <button class="text-xs text-theme-text-secondary hover:text-theme-primary" @click="quickPreviewVisible = false">收起</button>
          </div>
          <div v-if="quickPreviewLoading" class="py-10 text-center text-sm text-theme-text-secondary">加载简历中...</div>
          <div v-else-if="quickPreviewResume" class="border border-theme-border rounded-lg p-6 max-h-[480px] overflow-y-auto">
            <div class="text-center border-b pb-3 mb-4">
              <div class="text-xl font-bold">{{ quickPreviewResume.name || '未填写姓名' }}</div>
              <div class="text-xs text-theme-text-secondary mt-1">
                {{ quickPreviewResume.phone || '未填手机' }} · {{ quickPreviewResume.email || '未填邮箱' }}
                <template v-if="quickPreviewResume.jobIntention?.city"> · {{ quickPreviewResume.jobIntention.city }}</template>
              </div>
              <div v-if="quickPreviewResume.jobIntention?.position" class="text-sm mt-1.5" style="color: var(--theme-primary); font-weight: 600;">
                求职意向：{{ quickPreviewResume.jobIntention.position }}
              </div>
            </div>
            <div v-if="quickPreviewResume.works?.length" class="mb-4">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">工作经历</div>
              <div v-for="(w, i) in quickPreviewResume.works" :key="i" class="mb-3">
                <div class="text-sm font-medium flex justify-between">
                  <span>{{ w.company }} · {{ w.position }}</span>
                  <span class="text-xs text-theme-text-secondary">{{ w.startDate }} - {{ w.endDate || '至今' }}</span>
                </div>
                <p class="text-xs text-theme-text-secondary mt-1 leading-relaxed whitespace-pre-line">{{ w.description }}</p>
              </div>
            </div>
            <div v-if="quickPreviewResume.projects?.length" class="mb-4">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">项目经历</div>
              <div v-for="(p, i) in quickPreviewResume.projects" :key="i" class="mb-3">
                <div class="text-sm font-medium">{{ p.name }}<span v-if="p.role" class="text-theme-text-secondary font-normal"> · {{ p.role }}</span></div>
                <p class="text-xs text-theme-text-secondary mt-1 leading-relaxed whitespace-pre-line">{{ p.description }}</p>
              </div>
            </div>
            <div v-if="quickPreviewResume.skills?.length" class="mb-4">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">专业技能</div>
              <div class="flex flex-wrap gap-1.5">
                <span v-for="(s, i) in quickPreviewResume.skills" :key="i" class="text-xs px-2 py-1 rounded bg-theme-surface text-theme-text">
                  {{ s.name }}<span v-if="s.level" class="text-theme-text-secondary">（{{ s.level }}）</span>
                </span>
              </div>
            </div>
            <div v-if="quickPreviewResume.selfIntro">
              <div class="text-sm font-bold border-b-2 pb-1 mb-2" style="border-color: var(--theme-primary);">自我评价</div>
              <p class="text-xs text-theme-text-secondary leading-relaxed whitespace-pre-line">{{ quickPreviewResume.selfIntro }}</p>
            </div>
          </div>
          <div v-else class="py-10 text-center text-sm text-theme-text-secondary">请先采纳至少一项优化建议</div>
        </div>

        <div class="flex justify-between">
          <button class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="step = 2">
            <ArrowLeft class="w-4 h-4" /> 重新选择
          </button>
          <div class="flex gap-2">
            <button
              class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg border border-theme-primary text-theme-primary font-medium"
              :disabled="!optimizeResult || !adoptedSet.size"
              @click="goPreview"
            >
              <Eye class="w-4 h-4" /> 完整预览
            </button>
            <button
              class="inline-flex items-center gap-1.5 text-sm px-5 py-2 rounded-lg text-white font-medium disabled:opacity-50"
              style="background: var(--theme-primary);"
              :disabled="!optimizeResult || !adoptedSet.size"
              @click="saveOptimize"
            >
              <Save class="w-4 h-4" /> 保存优化结果
            </button>
          </div>
        </div>

        <!-- v10.20：单字段重新生成候选弹窗（Teleport 到 body，放 step4 内部不打断 v-else-if 链） -->
        <FieldRegenerateDialog
          v-model:visible="regenDialogVisible"
          :section-title="regenDialogTitle"
          :original-text="regenOriginalText"
          :candidates="regenCandidates"
          :loading="regenLoading"
          :error-msg="regenErrorMsg"
          @select="applyRegenCandidate"
        />
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
              <button
                v-if="savedResumeId"
                class="inline-flex items-center gap-1.5 text-sm px-4 py-2 rounded-lg text-white font-medium"
                style="background: var(--theme-primary);"
                @click="gotoInterview"
              >
                <Rocket class="w-4 h-4" /> 去面试
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
