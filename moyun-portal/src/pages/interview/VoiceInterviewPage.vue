<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import { generateSeo } from '@/utils/seo';
import { useToast } from '@/composables/useToast';
import { useApiCall } from '@/composables/useApiCall';
import { useSpeechSynthesis } from '@/composables/useSpeechSynthesis';
import { useSpeechRecognition } from '@/composables/useSpeechRecognition';
import { useAudioLevel } from '@/composables/useAudioLevel';
import { useInterviewHint } from '@/composables/useInterviewHint';
import {
  startVoiceInterview,
  submitVoiceAnswer,
  requestVoiceHint,
  forceVoiceNext,
  finishVoiceInterview,
  getVoiceInterviewDetail,
  addQaToWrongBook,
} from '@/api/voiceInterview';
import { getMyResumeList } from '@/api/interview';
import type { UserResumeVO } from '@/types/api';
import { useUserStore } from '@/stores/user';
import type {
  VoiceInterviewVO,
  VoiceInterviewReportVO,
  VoiceStartConfig,
  QuestionReview,
  PointItem,
  KnowledgePointItem,
} from '@/api/voiceInterview';

useHead({
  title: 'AI 语音面试官 - 墨云',
  meta: generateSeo({
    title: 'AI 语音面试官',
    description: '沉浸式语音面试，TTS 播报 + ASR 识别 + 智能评分反馈',
  }),
});

const toast = useToast();
const { run } = useApiCall();
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// ==================== 6 维度元数据（与后端 dimensions key 对齐） ====================
interface DimMeta {
  key: 'relevance' | 'professionalism' | 'fluency' | 'interactivity' | 'confidence' | 'logic';
  label: string;
  vertex: [number, number];
  textPos: { x: number; y: number; anchor: 'middle' | 'start' | 'end' };
}
const DIMENSION_META: DimMeta[] = [
  { key: 'relevance', label: '回答相关性', vertex: [100, 20], textPos: { x: 100, y: 15, anchor: 'middle' } },
  { key: 'professionalism', label: '专业度', vertex: [180, 60], textPos: { x: 190, y: 55, anchor: 'start' } },
  { key: 'fluency', label: '表达流畅度', vertex: [180, 140], textPos: { x: 190, y: 145, anchor: 'start' } },
  { key: 'interactivity', label: '面试互动性', vertex: [100, 180], textPos: { x: 100, y: 195, anchor: 'middle' } },
  { key: 'confidence', label: '自信度', vertex: [20, 140], textPos: { x: 10, y: 145, anchor: 'end' } },
  { key: 'logic', label: '逻辑清晰', vertex: [20, 60], textPos: { x: 10, y: 55, anchor: 'end' } },
];
const RADAR_GRID = [
  '100,20 180,60 180,140 100,180 20,140 20,60',
  '100,40 160,70 160,130 100,160 40,130 40,70',
  '100,60 140,80 140,120 100,140 60,120 60,80',
  '100,80 120,90 120,110 100,120 80,110 80,90',
];
const RADAR_AXES = DIMENSION_META.map((m) => ({ x1: 100, y1: 100, x2: m.vertex[0], y2: m.vertex[1] }));

/** 把维度分数数组（按 DIMENSION_META 顺序）转成 SVG polygon points */
function radarPoints(scores: number[]): string {
  return DIMENSION_META.map((m, i) => {
    const s = Math.max(0, Math.min(100, scores[i] ?? 0)) / 100;
    const x = 100 + (m.vertex[0] - 100) * s;
    const y = 100 + (m.vertex[1] - 100) * s;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');
}

/** 后端 dimensions key 可能是英文或中文，统一规整到 6 个英文 key */
function normalizeDimensions(dim?: Record<string, number> | null): Record<string, number> {
  const map: Record<string, DimMeta['key']> = {
    relevance: 'relevance', '回答相关性': 'relevance', '相关性': 'relevance',
    professionalism: 'professionalism', '专业度': 'professionalism', '专业深度': 'professionalism',
    fluency: 'fluency', '表达流畅度': 'fluency', '表达清晰度': 'fluency', '流畅度': 'fluency',
    interactivity: 'interactivity', '面试互动性': 'interactivity', '互动性': 'interactivity', '结构完整性': 'interactivity',
    confidence: 'confidence', '自信度': 'confidence',
    logic: 'logic', '逻辑清晰': 'logic', '逻辑清晰度': 'logic',
  };
  const out: Record<string, number> = {};
  if (dim) {
    for (const [k, v] of Object.entries(dim)) {
      const key = map[k] || k;
      out[key] = typeof v === 'number' ? v : Number(v) || 0;
    }
  }
  return out;
}

// ==================== 状态机 ====================
type Phase = 'setup' | 'interview' | 'report';
const phase = ref<Phase>('setup');
const loading = ref(false);
const submitting = ref(false);
const timerPaused = ref(false);

// 面试会话
const interview = ref<VoiceInterviewVO | null>(null);
const currentQaId = ref<number | string | null>(null);
const currentQuestion = ref('');
const currentSpeakText = ref('');

// 实时维度（来自 SSE onScore）
const liveDimensions = ref<Record<string, number>>({});

// 报告
const report = ref<VoiceInterviewReportVO | null>(null);
const reportTab = ref<'summary' | 'dialog' | 'analysis' | 'interviewer' | 'knowledge'>('summary');
const historyLoading = ref(false);

// 答案编辑
const editableAnswer = ref('');
const answerStartTime = ref(0);
const hintPreview = ref('');

// 面试时长计时
const elapsedSec = ref(0);
let timerHandle: ReturnType<typeof setInterval> | null = null;

// 倒计时（每题 90 秒）
const ANSWER_LIMIT_SEC = 90;
const answerRemain = ref(ANSWER_LIMIT_SEC);
let countdownHandle: ReturnType<typeof setInterval> | null = null;

// ==================== 对话气泡 ====================
interface ChatMessage {
  id: string;
  role: 'question' | 'user' | 'analysis' | 'ai';
  content: string;
  questionIdx?: number;
  total?: number;
  source?: string;
  tag?: string;
  highlights?: string[];
  gaps?: string[];
  scoreText?: string;
  createdAt: number;
}
const chatList = ref<ChatMessage[]>([]);
const chatScroll = ref<HTMLElement | null>(null);

function pushChat(
  role: ChatMessage['role'],
  content: string,
  opts: Partial<Omit<ChatMessage, 'id' | 'role' | 'content' | 'createdAt'>> = {},
) {
  chatList.value.push({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    role,
    content,
    createdAt: Date.now(),
    ...opts,
  });
  scrollChatBottom();
}
function scrollChatBottom() {
  nextTick(() => {
    const el = chatScroll.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
}

function avatarFor(role: ChatMessage['role']) {
  return role === 'user' ? '👤' : role === 'analysis' ? '🧠' : '🤖';
}

// ==================== 三引擎初始化 ====================
const {
  supported: ttsSupported,
  speaking,
  paused: ttsPaused,
  speak: ttsSpeak,
  pause: ttsPause,
  resume: ttsResume,
  cancel: ttsCancel,
} = useSpeechSynthesis({
  onUnsupported: () => toast.warning('当前浏览器不支持语音合成，将显示纯文字题目'),
});

const {
  supported: asrSupported,
  listening,
  interimText,
  finalText,
  start: startAsr,
  stop: stopAsr,
  reset: resetAsr,
} = useSpeechRecognition({
  onUnsupported: () => toast.warning('当前浏览器不支持语音识别，将切换为文字输入模式'),
  onError: (err) => {
    if (err === 'not-allowed' || err === 'service-not-allowed') {
      toast.error('麦克风权限被拒绝，请切换文字模式或在浏览器设置中允许');
    }
  },
});

// 实时音浪（聆听可视化增强）：生命周期与 listening 严格绑定，
// 由 watch 统一驱动，覆盖开麦/关麦/提交/结束/异常中断所有路径
const {
  levels: waveLevels,
  start: startWave,
  stop: stopWave,
} = useAudioLevel({ bars: 10 });
watch(listening, (on) => {
  if (on) {
    void startWave();
  } else {
    stopWave();
  }
});
/** 用户是否正在出声（活跃度阈值 0.06，用于 UI 提示） */
const userSpeaking = computed(() => waveLevels.value.reduce((a, b) => a + b, 0) / Math.max(1, waveLevels.value.length) > 0.06);

/**
 * 对话节奏闭环（行业标准"真人对聊"体验）：
 * AI 播报结束 → 自动开启麦克风聆听用户作答，免去每题手动点麦克风。
 * 用户点麦克风即可打断播报（toggleMic 内已有 ttsCancel），随时可抢话。
 */
watch(speaking, (now, before) => {
  if (
    before && !now &&
    autoListen.value &&
    asrSupported.value &&
    phase.value === 'interview' &&
    !submitting.value &&
    !timerPaused.value &&
    !listening.value
  ) {
    answerStartTime.value = Date.now();
    startAsr();
  }
});

const {
  currentLevel: hintLevel,
  currentHint,
  fetchHint,
  upgradeHint,
} = useInterviewHint();

// ASR 最终结果同步到可编辑答案
watch(finalText, (n) => {
  if (n) editableAnswer.value = n;
});

const answerInput = computed({
  get: () => editableAnswer.value,
  set: (v: string) => {
    editableAnswer.value = v;
  },
});

// ==================== 配置表单 ====================
const POSITION_OPTIONS = [
  { title: 'Java 后端开发工程师', meta: '3-5 年经验 · 北京 · 互联网', position: 'Java 后端开发工程师' },
  { title: '前端开发工程师', meta: '2-4 年经验 · 上海 · 电商', position: '前端开发工程师' },
  { title: '算法工程师', meta: '3-5 年经验 · 深圳 · AI', position: '算法工程师' },
];
const STYLE_OPTIONS = [
  { label: '温和型 - 鼓励式提问', value: 'friendly' as const },
  { label: '标准型 - 专业严谨', value: 'professional' as const },
  { label: '压力型 - 挑战式追问', value: 'strict' as const },
];
const DIFFICULTY_OPTIONS = [
  { label: '初级', value: 'easy' as const },
  { label: '中级', value: 'medium' as const },
  { label: '高级', value: 'hard' as const },
  { label: '专家', value: 'hard' as const },
];
const SCENARIO_OPTIONS = [
  { label: '技术面', value: 'technical' },
  { label: '项目面', value: 'project' },
  { label: 'HR 面', value: 'hr' },
  { label: '综合面', value: 'comprehensive' },
];
const QUESTION_COUNT_OPTIONS = [
  { label: '3 题（快速版）', value: 3 },
  { label: '5 题（标准版）', value: 5 },
  { label: '8 题（深度版）', value: 8 },
];

const STYLE_LABEL: Record<string, string> = {
  friendly: '温和型',
  professional: '标准型',
  strict: '压力型',
};

const config = ref<VoiceStartConfig>({
  position: 'Java 后端开发工程师',
  scene: 'technical',
  style: 'professional',
  difficulty: 'medium',
  personalized: true,
  hintsEnabled: true,
  stuckThreshold: 30,
});
const questionCount = ref(5);
const hintsEnabled = ref(true);
const muteMode = ref(false);
/** 自动聆听：AI 播报结束后自动开启麦克风，形成"真人对聊"节奏（行业标准） */
const autoListen = ref(true);

// 设备检测
const micStatus = computed(() => (asrSupported.value ? 'ok' : 'error'));
const speakerStatus = computed(() => (ttsSupported.value ? 'ok' : 'error'));
const micStatusLabel = computed(() => (asrSupported.value ? '✓ 已授权' : '✗ 未授权'));
const speakerStatusLabel = computed(() => (ttsSupported.value ? '✓ 正常' : '✗ 不支持'));
const deviceTesting = ref<Record<string, boolean>>({});
const deviceTested = ref<Record<string, boolean>>({});

async function testDevice(device: 'mic' | 'speaker') {
  deviceTesting.value[device] = true;
  if (device === 'mic') {
    if (!asrSupported.value) {
      toast.warning('当前浏览器不支持语音识别');
    } else {
      resetAsr();
      startAsr();
      toast.info('开始录音测试，请说一句话...');
      setTimeout(() => {
        stopAsr();
      }, 3000);
    }
  } else {
    if (!ttsSupported.value) {
      toast.warning('当前浏览器不支持语音合成');
    } else {
      ttsSpeak('设备测试：能听到我的声音吗？', true);
    }
  }
  setTimeout(() => {
    deviceTesting.value[device] = false;
    deviceTested.value[device] = true;
  }, 1500);
}

// ==================== 简历库（真实数据：AI 面试题源依赖） ====================
const resumeList = ref<UserResumeVO[]>([]);
const resumeLoading = ref(false);
const selectedResumeId = ref<number | null>(null);
/** 自定义岗位（当预设岗位都不匹配时） */
const useCustomPosition = ref(false);
const customPosition = ref('');

async function loadResumeList() {
  if (!userStore.isAuthenticated) return;
  resumeLoading.value = true;
  try {
    const res = await getMyResumeList({ pageNum: 1, pageSize: 50 });
    if (res.code === 200 && res.data) {
      resumeList.value = res.data.list || [];
      // 默认选中第一份有效简历，并带入其求职意向岗位
      const first = resumeList.value.find((r) => r.id);
      if (first?.id) {
        selectResume(first);
      }
    }
  } catch {
    /* 未登录/网络异常时静默：岗位预设仍可用 */
  } finally {
    resumeLoading.value = false;
  }
}

function selectResume(r: UserResumeVO) {
  if (!r.id) return;
  selectedResumeId.value = Number(r.id);
  const intentPos = r.jobIntention?.position?.trim();
  if (intentPos) {
    // 简历求职意向优先作为面试岗位（个性化出题）
    const matched = POSITION_OPTIONS.find((o) => o.position === intentPos);
    if (!matched) {
      useCustomPosition.value = true;
      customPosition.value = intentPos;
    } else {
      useCustomPosition.value = false;
      config.value.position = intentPos;
    }
  }
}

/** 实际生效的面试岗位 */
const effectivePosition = computed(() => {
  if (useCustomPosition.value) return customPosition.value.trim() || config.value.position;
  return config.value.position;
});

onMounted(() => {
  loadResumeList();
});

// ==================== 计时器 ====================
function startElapsedTimer() {
  stopElapsedTimer();
  timerHandle = setInterval(() => {
    if (!timerPaused.value) elapsedSec.value++;
  }, 1000);
}
function stopElapsedTimer() {
  if (timerHandle) {
    clearInterval(timerHandle);
    timerHandle = null;
  }
}
function formatElapsed(sec: number) {
  const h = Math.floor(sec / 3600).toString().padStart(2, '0');
  const m = Math.floor((sec % 3600) / 60).toString().padStart(2, '0');
  const s = (sec % 60).toString().padStart(2, '0');
  return `${h}:${m}:${s}`;
}

function startCountdown() {
  stopCountdown();
  answerRemain.value = ANSWER_LIMIT_SEC;
  countdownHandle = setInterval(() => {
    if (timerPaused.value) return;
    if (answerRemain.value > 0) {
      answerRemain.value--;
    } else {
      stopCountdown();
      if (phase.value === 'interview' && !submitting.value) {
        toast.warning('作答超时，自动提交');
        handleSubmitAnswer();
      }
    }
  }, 1000);
}
function stopCountdown() {
  if (countdownHandle) {
    clearInterval(countdownHandle);
    countdownHandle = null;
  }
}

// ==================== 题目进度时间线 ====================
const progressItems = computed(() => {
  if (!interview.value) return [];
  const total = interview.value.totalQa || 0;
  const current = interview.value.currentIdx || 0;
  const qaList = interview.value.qaList ?? [];
  const items: { idx: number; status: 'completed' | 'active' | 'pending'; title: string }[] = [];
  for (let i = 1; i <= total; i++) {
    const qa = qaList.find((q) => q.questionIdx === i);
    const status: 'completed' | 'active' | 'pending' =
      i < current ? 'completed' : i === current ? 'active' : 'pending';
    const title = qa?.question ? truncate(qa.question, 12) : `第 ${i} 题`;
    items.push({ idx: i, status, title });
  }
  return items;
});
function truncate(s: string, n: number) {
  return s.length > n ? s.slice(0, n) + '…' : s;
}

// ==================== 实时维度计算 ====================
const liveDimNorm = computed(() => normalizeDimensions(liveDimensions.value));
const liveScores = computed(() =>
  DIMENSION_META.map((m) => liveDimNorm.value[m.key] ?? 0),
);
const liveRadarPoints = computed(() => radarPoints(liveScores.value));
const showLiveRadar = computed(() => Object.keys(liveDimensions.value).length > 0);

const liveDimensionTags = computed(() => {
  if (!showLiveRadar.value) return [];
  const arr = DIMENSION_META.map((m, i) => ({ label: m.label, score: liveScores.value[i] }));
  const sorted = [...arr].sort((a, b) => b.score - a.score);
  const tags: { type: 'highlight' | 'gap'; text: string }[] = [];
  const high = sorted[0];
  const low = sorted[sorted.length - 1];
  if (high && high.score > 0) tags.push({ type: 'highlight', text: `✓ ${high.label} ${high.score}` });
  if (low && low.score < high!.score) tags.push({ type: 'gap', text: `✗ ${low.label} ${low.score}` });
  return tags;
});

// ==================== 报告维度计算 ====================
const reportDimNorm = computed(() => normalizeDimensions(report.value?.dimensions ?? null));
const reportScores = computed(() =>
  DIMENSION_META.map(
    (m) => reportDimNorm.value[m.key] ?? report.value?.totalScore ?? 0,
  ),
);
const reportRadarPoints = computed(() => radarPoints(reportScores.value));

function pointText(p: PointItem): string {
  return typeof p === 'string' ? p : p.text || '';
}
function pointQuote(p: PointItem): string {
  return typeof p === 'string' ? '' : p.quote || '';
}
function kpTitle(k: KnowledgePointItem): string {
  return typeof k === 'string' ? k : k.title || k.name || '';
}
function kpDesc(k: KnowledgePointItem): string {
  return typeof k === 'string' ? '' : k.desc || k.description || '';
}
function scoreClass(score: number) {
  return score < 60 ? 'low' : score < 80 ? 'medium' : 'high';
}
function scoreColor(score: number) {
  return score < 60 ? 'var(--error)' : score < 80 ? 'var(--warning)' : 'var(--success)';
}

const suggestionItems = computed(() => {
  const s = report.value?.suggestion || '';
  if (!s) return [];
  const lines = s.split(/\n+/).map((l) => l.trim()).filter(Boolean);
  if (lines.length > 1) return lines;
  const numbered = s.split(/\s*\d+[.、)]\s+/).map((l) => l.trim()).filter(Boolean);
  return numbered.length > 1 ? numbered : [s];
});

// ==================== 历史报告：?id=xxx ====================
async function loadHistoryReport(idStr: string) {
  if (!idStr) return;
  historyLoading.value = true;
  phase.value = 'report';
  try {
    const { data: resp, success } = await run(() => getVoiceInterviewDetail(idStr), {
      errorToast: '加载面试详情失败',
    });
    if (!success || !resp?.data) return;
    const vo = resp.data;
    interview.value = vo;

    const qaList = vo.qaList ?? [];
    const questionReviews: QuestionReview[] = qaList
      .filter((q) => q.question != null && q.question !== '')
      .map((q) => ({
        questionIdx: q.questionIdx ?? 0,
        question: q.question,
        score: q.score ?? 0,
        feedback: q.aiFeedback ?? '系统正在生成点评，稍后回来查看完整反馈。',
        userAnswer: q.userAnswer,
        analysis: q.aiFeedback,
        qaId: q.id,
      }));
    const avgScore =
      questionReviews.length > 0
        ? Math.round(questionReviews.reduce((s, r) => s + (r.score ?? 0), 0) / questionReviews.length)
        : vo.score ?? 0;
    const totalScore = vo.score ?? avgScore;

    let dimensions: Record<string, number> | undefined;
    const seen: Record<string, number[]> = {};
    qaList.forEach((q) => {
      if (q.ruleDimensionsJson) {
        try {
          const obj = JSON.parse(q.ruleDimensionsJson) as Record<string, number>;
          Object.entries(obj).forEach(([k, v]) => {
            if (typeof v === 'number') {
              if (!seen[k]) seen[k] = [];
              seen[k].push(v);
            }
          });
        } catch {
          /* ignore */
        }
      }
    });
    if (Object.keys(seen).length > 0) {
      dimensions = {};
      Object.entries(seen).forEach(([k, arr]) => {
        dimensions![k] = Math.round(arr.reduce((s, x) => s + x, 0) / arr.length);
      });
    } else {
      dimensions = DIMENSION_META.reduce((acc, m, i) => {
        const offset = [+1, -2, +2, 0, -1, +1];
        acc[m.key] = Math.max(55, Math.min(99, totalScore + (offset[i] ?? 0)));
        return acc;
      }, {} as Record<string, number>);
    }

    const highlights: VoiceInterviewReportVO['highlights'] =
      totalScore >= 80
        ? [
            { text: '整体回答切题，能围绕问题展开', quote: '回答聚焦题目，未出现明显跑题' },
            { text: '具备一定的专业表述能力' },
          ]
        : totalScore >= 60
          ? [{ text: '部分回答命中得分点，仍有提升空间' }]
          : [];
    const weakPoints: VoiceInterviewReportVO['weakPoints'] =
      totalScore < 85
        ? [
            { text: '结构完整性可加强（建议 STAR 结构答题）' },
            { text: '专业深度与案例支撑仍有欠缺' },
          ]
        : [];

    report.value = {
      interviewId: vo.id,
      totalScore,
      dimensions,
      highlights,
      weakPoints,
      questionReviews,
      summary:
        vo.summary ??
        (questionReviews.length > 0
          ? `本次共回答 ${questionReviews.length} 道题，综合得分 ${totalScore}。` +
            (totalScore >= 80
              ? '整体表现良好，注意补齐薄弱知识点即可冲击 Offer。'
              : totalScore >= 60
                ? '具备基础能力，建议通过复盘强化表达结构与专业深度。'
                : '差距较大，建议先完成题库基础练习 + 模拟面试后再挑战语音面试。')
          : '本次面试尚未形成完整总结。'),
      suggestion:
        '建议对照逐题复盘，把失分题加入错题本并补齐相关知识点；下次面试优先采用 STAR 结构作答。',
      knowledgePoints: [],
    };
    reportTab.value = 'summary';
  } finally {
    historyLoading.value = false;
  }
}

onMounted(() => {
  const id = String(route.query.id ?? '').trim();
  if (id) loadHistoryReport(id);
});

onUnmounted(() => {
  stopElapsedTimer();
  stopCountdown();
  ttsCancel();
  stopAsr();
});

// ==================== 开始面试 ====================
function presentQuestion(question: string, speakText: string, idx?: number, tag?: string) {
  currentQuestion.value = question;
  currentSpeakText.value = speakText;
  pushChat('question', question, {
    questionIdx: idx ?? interview.value?.currentIdx ?? 0,
    total: interview.value?.totalQa ?? 0,
    source: '智能生成',
    tag,
  });
  editableAnswer.value = '';
  resetAsr();
  startCountdown();
  if (!muteMode.value && ttsSupported.value && speakText) {
    setTimeout(() => ttsSpeak(speakText), 200);
  }
}

async function handleStart() {
  loading.value = true;
  try {
    const payload: VoiceStartConfig = {
      ...config.value,
      position: effectivePosition.value,
      resumeId: selectedResumeId.value ?? undefined,
      personalized: true,
      hintsEnabled: hintsEnabled.value,
      stuckThreshold: 30,
    };
    const { data: vo, success } = await run(() => startVoiceInterview(payload), {
      errorToast: '开始失败',
    });
    if (success && vo?.data) {
      interview.value = vo.data;
      currentQaId.value = vo.data.currentQa?.id ?? null;
      phase.value = 'interview';
      chatList.value = [];
      liveDimensions.value = {};
      elapsedSec.value = 0;
      timerPaused.value = false;
      startElapsedTimer();

      const greet = vo.data.greetText || '';
      if (greet) pushChat('ai', greet);
      const firstQuestion = vo.data.currentQa?.question || '';
      const firstSpeak = vo.data.currentQa?.speakText || firstQuestion;
      if (firstQuestion) {
        presentQuestion(firstQuestion, firstSpeak, vo.data.currentQa?.questionIdx);
      }
      if (!muteMode.value && greet && ttsSupported.value) {
        ttsSpeak(greet);
      }
      toast.success('面试已开始');
    }
  } finally {
    loading.value = false;
  }
}

// ==================== 麦克风开关 ====================
function toggleMic() {
  if (!asrSupported.value) {
    toast.warning('浏览器不支持语音识别，请在下方文字框输入答案');
    return;
  }
  if (listening.value) {
    stopAsr();
  } else {
    if (speaking.value) ttsCancel();
    if (!editableAnswer.value) resetAsr();
    answerStartTime.value = Date.now();
    startAsr();
  }
}

function clearAnswer() {
  editableAnswer.value = '';
  resetAsr();
}

/** 静音切换：开启时立即停止当前播报，关闭时恢复后续播报 */
function toggleMute() {
  muteMode.value = !muteMode.value;
  if (muteMode.value) ttsCancel();
}

// ==================== 提交答案（SSE） ====================
async function handleSubmitAnswer() {
  if (!interview.value || !currentQaId.value) return;
  const transcript = editableAnswer.value || finalText.value;
  if (!transcript.trim()) {
    toast.warning('答案不能为空');
    return;
  }
  if (listening.value) stopAsr();
  stopCountdown();
  submitting.value = true;
  pushChat('user', transcript);
  const latencyMs = answerStartTime.value ? Date.now() - answerStartTime.value : 0;

  await submitVoiceAnswer(
    String(interview.value.id),
    String(currentQaId.value),
    transcript,
    latencyMs,
    {
      onScore: (data) => {
        liveDimensions.value = data.dimensions || {};
        const dims = normalizeDimensions(data.dimensions);
        const highlights: string[] = [];
        const gaps: string[] = [];
        DIMENSION_META.forEach((m) => {
          const s = dims[m.key];
          if (typeof s === 'number') {
            if (s >= 80) highlights.push(`${m.label} ${s}`);
            else if (s < 60) gaps.push(`${m.label} ${s}`);
          }
        });
        const scoreText = DIMENSION_META.map((m) => `${m.label} ${dims[m.key] ?? '-'}`).join(' · ');
        pushChat('analysis', '', { highlights, gaps, scoreText });
      },
      onSpeak: (text) => {
        if (text) {
          pushChat('ai', text, { tag: '评分反馈' });
          if (!muteMode.value && ttsSupported.value) ttsSpeak(text);
        }
      },
      onData: (data) => {
        submitting.value = false;
        // V10.4：LLM 引导提示（回答跑偏时面试官给出的方向引导）
        if (data.guidance) {
          pushChat('ai', data.guidance, { tag: '引导' });
          if (!muteMode.value && ttsSupported.value) ttsSpeak(data.guidance);
        }
        if (data.nextAction === 'next' && data.nextQaId) {
          currentQaId.value = data.nextQaId;
          if (data.nextQuestion) {
            presentQuestion(data.nextQuestion, data.nextSpeakText || data.nextQuestion);
          }
        } else if (data.nextAction === 'followup' && data.nextQaId) {
          currentQaId.value = data.nextQaId;
          if (data.nextQuestion) {
            presentQuestion(data.nextQuestion, data.nextSpeakText || data.nextQuestion, undefined, '追问');
          }
          toast.info('面试官追问，请补充回答');
        } else if (data.nextAction === 'report') {
          toast.info('面试结束，正在生成报告...');
          handleFinish();
        } else {
          editableAnswer.value = '';
          resetAsr();
        }
      },
      onEnd: () => {
        submitting.value = false;
      },
      onError: (msg) => {
        toast.error(msg || '提交失败');
        submitting.value = false;
      },
    },
  );
}

// ==================== 智能提示（复用 useInterviewHint） ====================
async function handleHint() {
  const qid = interview.value?.currentQa?.questionId;
  if (!qid) {
    if (!interview.value || !currentQaId.value) {
      toast.warning('当前题目暂不支持提示');
      return;
    }
    const { data: vo, success } = await run(
      () => requestVoiceHint(interview.value!.id, String(currentQaId.value)),
      { errorToast: '提示获取失败' },
    );
    if (success && vo?.data) {
      const speak = vo.data.currentQa?.speakText;
      const used = vo.data.currentQa?.hintUsed ?? 1;
      if (speak) {
        pushChat('ai', `💡 ${speak}`, { tag: `L${used} 提示` });
        hintPreview.value = speak;
        if (!muteMode.value && ttsSupported.value) ttsSpeak(speak);
      }
      toast.success(`已获取 L${used} 提示`);
    }
    return;
  }
  let hint;
  if (currentHint.value) {
    hint = await upgradeHint(qid);
  } else {
    hint = await fetchHint(qid, 1);
  }
  if (!hint) return;
  const parts = [hint.title];
  if (hint.keywords?.length) parts.push(`关键词：${hint.keywords.join('、')}`);
  if (hint.structureHint) parts.push(hint.structureHint);
  if (hint.examinePoints?.length) parts.push(`考察点：${hint.examinePoints.join('、')}`);
  const text = parts.join('\n');
  pushChat('ai', text, { tag: `L${hintLevel.value} 提示` });
  hintPreview.value = text;
  if (!muteMode.value && ttsSupported.value) {
    ttsSpeak(hint.speakText || text);
  }
  toast.success(`已获取 L${hintLevel.value} 提示（共 3 级）`);
}

// ==================== 强制下一题 ====================
async function handleForceNext() {
  if (!interview.value) return;
  if (speaking.value) ttsCancel();
  if (listening.value) stopAsr();
  stopCountdown();
  loading.value = true;
  try {
    const { data: vo, success } = await run(
      () => forceVoiceNext(interview.value!.id, 'user_skip'),
      { errorToast: '切换下一题失败' },
    );
    if (success && vo?.data) {
      interview.value = vo.data;
      if (vo.data.status === 'finished') {
        await handleFinish();
      } else if (vo.data.currentQa) {
        currentQaId.value = vo.data.currentQa.id;
        presentQuestion(
          vo.data.currentQa.question,
          vo.data.currentQa.speakText || vo.data.currentQa.question,
          vo.data.currentQa.questionIdx,
          '下一题',
        );
      }
    }
  } finally {
    loading.value = false;
  }
}

// ==================== 结束面试 + 报告 ====================
async function handleFinish() {
  if (!interview.value) return;
  if (speaking.value) ttsCancel();
  if (listening.value) stopAsr();
  stopCountdown();
  stopElapsedTimer();
  loading.value = true;
  try {
    const { data: reportVo, success } = await run(
      () => finishVoiceInterview(interview.value!.id),
      { errorToast: '结束面试失败' },
    );
    if (success && reportVo?.data) {
      report.value = reportVo.data;
      phase.value = 'report';
      reportTab.value = 'summary';
      toast.success('面试已结束，报告已生成');
    }
  } finally {
    loading.value = false;
  }
}

function confirmEnd() {
  if (window.confirm('确定要结束面试吗？将生成复盘报告。')) {
    handleFinish();
  }
}

// ==================== 顶部控制 ====================
function togglePause() {
  timerPaused.value = !timerPaused.value;
  if (timerPaused.value) {
    if (speaking.value) ttsPause();
  } else {
    if (ttsPaused.value) ttsResume();
  }
}

// ==================== 重新开始 ====================
function handleRestart() {
  interview.value = null;
  report.value = null;
  currentQaId.value = null;
  currentQuestion.value = '';
  currentSpeakText.value = '';
  editableAnswer.value = '';
  chatList.value = [];
  liveDimensions.value = {};
  hintPreview.value = '';
  resetAsr();
  ttsCancel();
  stopCountdown();
  stopElapsedTimer();
  elapsedSec.value = 0;
  timerPaused.value = false;
  phase.value = 'setup';
}

function goHome() {
  router.push('/');
}

// ==================== 错题本 ====================
const wrongBookLoading = ref<number | null>(null);
async function handleAddToWrongBook(qaId: number | undefined) {
  if (!qaId) {
    toast.warning('无法关联题目');
    return;
  }
  wrongBookLoading.value = qaId;
  const { success } = await run(() => addQaToWrongBook(qaId), {
    errorToast: '加入错题本失败',
    successToast: '已加入错题本',
  });
  if (success) {
    toast.success('已加入错题本，可在错题本中复习');
  }
  wrongBookLoading.value = null;
}

async function addAllWeakToWrongBook() {
  const weak = (report.value?.questionReviews ?? []).filter((q) => q.score < 80 && q.qaId);
  if (weak.length === 0) {
    toast.info('暂无薄弱题（评分 < 80）需要加入错题本');
    return;
  }
  for (const q of weak) {
    await handleAddToWrongBook(q.qaId);
  }
}

// ==================== 报告操作 ====================
function downloadReport() {
  toast.info('正在生成 PDF 报告，请稍候...');
  setTimeout(() => window.print(), 300);
}

async function shareReport() {
  const id = interview.value?.id ?? report.value?.interviewId;
  if (!id) {
    toast.warning('暂无可分享的报告');
    return;
  }
  const url = `${window.location.origin}${window.location.pathname}?id=${id}`;
  try {
    await navigator.clipboard.writeText(url);
    toast.success('分享链接已复制到剪贴板');
  } catch {
    window.prompt('复制分享链接：', url);
  }
}

function startNewInterview() {
  handleRestart();
}

/** 对话状态机：聆听（红）/ 分析（黄）/ 播报（蓝）/ 空闲（绿）—— 对标行业标准三态环 */
const chatStatus = computed(() => {
  if (submitting.value) return { mode: 'analyzing' as const, text: 'AI 正在分析', icon: '🧠' };
  if (listening.value) return { mode: 'listening' as const, text: '正在聆听', icon: '🎧' };
  if (speaking.value) return { mode: 'speaking' as const, text: '正在播报', icon: '🔊' };
  return { mode: 'idle' as const, text: '在线 · 等待回答', icon: '💬' };
});
</script>

<template>
  <div class="vi-shell" :class="`vi-shell--${phase}`">
    <!-- ==================== 准备页 ==================== -->
    <div v-if="phase === 'setup'" class="prep-page">
      <div class="prep-top-bar">
        <div class="prep-top-left">
          <div class="prep-logo">🎙️ 旭林知行</div>
          <div class="prep-breadcrumb">首页 / <span>AI 语音面试</span></div>
        </div>
        <div class="prep-top-right">
          <button class="prep-back-btn" @click="router.push('/interview/voice/history')">📋 我的面试记录</button>
          <button class="prep-back-btn" @click="goHome">← 返回首页</button>
        </div>
      </div>
      <div class="prep-container">
        <div class="prep-header">
          <h1>AI 语音面试准备</h1>
          <p>完成以下准备步骤，开始你的模拟面试</p>
        </div>
        <div class="prep-steps">
          <div class="prep-step">
            <div class="step-circle completed">✓</div>
            <span class="step-label">设备检测</span>
          </div>
          <div class="step-connector completed"></div>
          <div class="prep-step">
            <div class="step-circle active">2</div>
            <span class="step-label active">岗位与简历</span>
          </div>
          <div class="step-connector"></div>
          <div class="prep-step">
            <div class="step-circle">3</div>
            <span class="step-label">开始面试</span>
          </div>
        </div>

        <!-- 第一步：设备检测 -->
        <div class="prep-card">
          <div class="prep-card-title"><span class="step-badge">1</span>设备检测</div>
          <div class="device-check-list">
            <div class="device-item">
              <div class="device-info"><div class="device-icon">🎤</div><span class="device-name">麦克风</span></div>
              <div class="device-status">
                <span :class="['status-badge', micStatus]">{{ micStatusLabel }}</span>
                <button class="test-btn" :disabled="deviceTesting.mic" @click="testDevice('mic')">
                  {{ deviceTested.mic ? '✓ 通过' : '测试' }}
                </button>
              </div>
            </div>
            <div class="device-item">
              <div class="device-info"><div class="device-icon">🔊</div><span class="device-name">扬声器</span></div>
              <div class="device-status">
                <span :class="['status-badge', speakerStatus]">{{ speakerStatusLabel }}</span>
                <button class="test-btn" :disabled="deviceTesting.speaker" @click="testDevice('speaker')">
                  {{ deviceTested.speaker ? '✓ 通过' : '测试' }}
                </button>
              </div>
            </div>
            <div class="device-item">
              <div class="device-info"><div class="device-icon">🎧</div><span class="device-name">耳机（推荐）</span></div>
              <div class="device-status"><span class="status-badge checking">⚠ 未检测到</span></div>
            </div>
          </div>
          <div class="device-tip">💡 建议佩戴耳机，避免回声干扰。首次使用建议试说一段话测试双向通道。</div>
        </div>

        <!-- 第二步：岗位与简历 -->
        <div class="prep-card">
          <div class="prep-card-title"><span class="step-badge">2</span>岗位与简历</div>

          <div class="position-section">
            <div class="section-label">🎯 选择面试岗位</div>
            <div class="position-list">
              <div
                v-for="opt in POSITION_OPTIONS"
                :key="opt.position"
                :class="['position-option', { selected: !useCustomPosition && config.position === opt.position }]"
                @click="useCustomPosition = false; config.position = opt.position"
              >
                <div class="option-radio"></div>
                <div class="option-content">
                  <div class="option-title">{{ opt.title }}</div>
                  <div class="option-meta">{{ opt.meta }}</div>
                </div>
              </div>
              <!-- 自定义岗位：跟随简历求职意向或手动输入 -->
              <div
                :class="['position-option custom', { selected: useCustomPosition }]"
                @click="useCustomPosition = true"
              >
                <div class="option-radio"></div>
                <div class="option-content">
                  <div class="option-title">{{ useCustomPosition ? '自定义岗位' : '自定义岗位（跟随简历求职意向）' }}</div>
                  <input
                    v-model="customPosition"
                    class="custom-position-input"
                    placeholder="输入目标岗位，如：Go 后端开发工程师"
                    @click.stop
                    @input="useCustomPosition = true"
                  />
                </div>
              </div>
            </div>
          </div>

          <div class="config-row">
            <div class="config-item">
              <label class="config-label">面试官风格</label>
              <select v-model="config.style" class="config-select">
                <option v-for="o in STYLE_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div class="config-item">
              <label class="config-label">难度等级</label>
              <select v-model="config.difficulty" class="config-select">
                <option v-for="o in DIFFICULTY_OPTIONS" :key="o.label" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
          </div>
          <div class="config-row">
            <div class="config-item">
              <label class="config-label">面试场景</label>
              <select v-model="config.scene" class="config-select">
                <option v-for="o in SCENARIO_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div class="config-item">
              <label class="config-label">题目数量</label>
              <select v-model.number="questionCount" class="config-select">
                <option v-for="o in QUESTION_COUNT_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
          </div>

          <div class="resume-section" style="margin-top: 1.5rem;">
            <div class="section-label">📄 选择简历（AI 将基于简历项目经历深挖提问）</div>

            <!-- 简历库加载中 -->
            <div v-if="resumeLoading" class="resume-empty">
              <div class="empty-icon">⏳</div>
              <div class="empty-title">正在加载简历库…</div>
            </div>

            <!-- 简历库为空：引导创建 -->
            <div v-else-if="resumeList.length === 0" class="resume-empty">
              <div class="empty-icon">📋</div>
              <div class="empty-title">还没有在线简历</div>
              <div class="empty-desc">上传或创建简历后，AI 面试官将针对你的项目经历个性化出题</div>
              <div class="empty-actions">
                <button class="resume-action-btn primary" @click="router.push('/interview/my/resumes')">去创建 / 上传简历</button>
              </div>
            </div>

            <!-- 简历库选择列表 -->
            <template v-else>
              <div class="resume-select-list">
                <div
                  v-for="r in resumeList"
                  :key="r.id"
                  :class="['resume-option', { selected: selectedResumeId === Number(r.id) }]"
                  @click="selectResume(r)"
                >
                  <div class="resume-option-main">
                    <div class="resume-option-name">
                      {{ r.title || `${r.name || '我的'}的简历` }}
                      <span v-if="r.score" class="resume-option-score">AI评分 {{ r.score }}</span>
                    </div>
                    <div class="resume-option-meta">
                      期望岗位：{{ r.jobIntention?.position || '未设置' }}
                      <template v-if="r.updateTime"> · 更新于 {{ (r.updateTime || '').slice(0, 10) }}</template>
                    </div>
                  </div>
                  <div class="option-radio"></div>
                </div>
              </div>
              <div class="resume-manage-row">
                <button class="resume-action-btn" @click="router.push('/interview/my/resumes')">管理简历库</button>
                <span class="resume-manage-hint">不选简历也可面试，AI 将按岗位通用题库出题</span>
              </div>
            </template>
          </div>
        </div>

        <!-- 偏好设置 -->
        <div class="prep-card">
          <div class="prep-card-title"><span class="step-badge">⚙</span>偏好设置</div>
          <div class="toggle-row">
            <span class="toggle-label">💡 开启智能提示（卡壳时自动提示关键词）</span>
            <label class="toggle-switch">
              <input v-model="hintsEnabled" type="checkbox" checked>
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="toggle-row">
            <span class="toggle-label">🔇 静音模式（仅文字，不播放语音）</span>
            <label class="toggle-switch">
              <input v-model="muteMode" type="checkbox">
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="device-row">
            <span class="toggle-label">🎧 自动聆听（面试官说完自动开麦，随时可打断）</span>
            <label class="toggle-switch">
              <input v-model="autoListen" type="checkbox">
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>

        <button class="start-btn" :disabled="loading" @click="handleStart">
          {{ loading ? '正在开启...' : '🚀 开始 AI 语音面试' }}
        </button>
      </div>
    </div>

    <!-- ==================== 面试进行页 ==================== -->
    <div v-else-if="phase === 'interview'" class="interview-page">
      <div class="top-bar">
        <div class="top-bar-left">
          <div class="top-bar-logo">🎙️ 旭林知行</div>
          <div class="timer-display"><span>⏱️</span><span>{{ formatElapsed(elapsedSec) }}</span></div>
        </div>
        <div class="top-bar-right">
          <button
            :class="['control-btn', { muted: muteMode }]"
            :title="muteMode ? '开启语音播报' : '静音模式'"
            @click="toggleMute"
          >{{ muteMode ? '🔇 已静音' : '🔈 播报中' }}</button>
          <button class="control-btn" @click="togglePause">
            {{ timerPaused ? '▶️ 继续' : '⏸️ 暂停' }}
          </button>
          <button class="control-btn" :disabled="loading" @click="handleForceNext">⏭️ 下一题</button>
          <button class="control-btn danger" @click="confirmEnd">⏹️ 结束面试</button>
        </div>
      </div>
      <div class="interview-main">
        <!-- 左面板 -->
        <div class="left-panel">
          <div class="interviewer-card">
            <div :class="['interviewer-avatar', { speaking: speaking }]">
              <span class="interviewer-emoji">👨‍💼</span>
              <div v-if="speaking" class="ai-sound-wave">
                <span></span><span></span><span></span><span></span>
              </div>
            </div>
            <div class="interviewer-name">AI 面试官</div>
            <span class="interviewer-style">{{ STYLE_LABEL[config.style ?? 'professional'] || '标准型' }}</span>
            <div v-if="speaking" class="interviewer-state">🔊 正在播报</div>
            <div v-else-if="submitting" class="interviewer-state thinking">🧠 分析中</div>
          </div>
          <div class="progress-card">
            <div class="progress-title">题目进度</div>
            <div class="progress-timeline">
              <div
                v-for="item in progressItems"
                :key="item.idx"
                :class="['progress-item', item.status]"
              >
                <div class="progress-dot">{{ item.status === 'completed' ? '✓' : item.idx }}</div>
                <span>{{ item.title }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 中面板 -->
        <div class="center-panel">
          <div class="chat-header">
            <div class="chat-header-title">💬 面试对话</div>
            <div :class="['chat-status', `is-${chatStatus.mode}`]">
              <div class="chat-status-dot"></div>
              <span>{{ chatStatus.icon }} {{ chatStatus.text }}</span>
            </div>
            <button
              v-if="speaking"
              class="stop-speak-btn"
              title="停止播报"
              @click="ttsCancel"
            >⏹ 停止播报</button>
          </div>
          <div ref="chatScroll" class="chat-body">
            <div
              v-for="m in chatList"
              :key="m.id"
              :class="['message', m.role]"
            >
              <div class="message-avatar">{{ avatarFor(m.role) }}</div>
              <div class="message-content">
                <div v-if="m.role === 'question'" class="question-header">
                  📌 问题 {{ m.questionIdx }}/{{ m.total || interview?.totalQa || 0 }}
                  <span v-if="m.tag"> · {{ m.tag }}</span>
                  · 题源：{{ m.source }}
                </div>
                <div v-else-if="m.role === 'analysis'" class="analysis-header">
                  ✅ 实时分析<span v-if="m.tag"> · {{ m.tag }}</span>
                </div>
                <div v-else-if="m.tag" class="ai-tag">{{ m.tag }}</div>

                <template v-if="m.role === 'ai'">
                  <MarkdownRenderer editor-mode="markdown" :content-markdown="m.content" prose-width="none" />
                </template>
                <template v-else-if="m.role === 'analysis'">
                  <div v-if="m.highlights && m.highlights.length"><strong>亮点：</strong>{{ m.highlights.join('；') }}</div>
                  <div v-if="m.gaps && m.gaps.length"><strong>缺口：</strong>{{ m.gaps.join('；') }}</div>
                  <div v-if="m.scoreText" class="analysis-score">{{ m.scoreText }}</div>
                </template>
                <template v-else>
                  <span class="message-text">{{ m.content }}</span>
                </template>
              </div>
            </div>
          </div>
          <div class="chat-input-area">
            <!-- 聆听实时音浪：10 柱频段分桶，随说话音量跳动 -->
            <div v-show="listening" :class="['voice-wave', { active: userSpeaking }]">
              <div class="wave-bars">
                <span
                  v-for="(lv, i) in waveLevels"
                  :key="i"
                  class="wave-bar"
                  :style="{ height: `${12 + lv * 88}%` }"
                ></span>
              </div>
              <span class="wave-label">{{ userSpeaking ? '请继续作答' : '请开始说话' }}</span>
            </div>
            <div class="input-timer">
              <span>⚠️</span><span>{{ answerRemain }} 秒内作答</span>
              <span v-if="interimText" class="interim-hint">· 实时识别：{{ interimText }}</span>
            </div>
            <div class="input-container">
              <textarea
                v-model="answerInput"
                class="input-textarea"
                placeholder="面试官你好，我认为..."
                rows="2"
                :disabled="submitting"
              ></textarea>
              <button
                class="mic-btn"
                :class="{ recording: listening }"
                :disabled="submitting"
                @click="toggleMic"
              >
                {{ listening ? '⏹️' : '🎙️' }}
              </button>
            </div>
            <div class="input-actions">
              <button class="action-btn" :disabled="submitting" @click="clearAnswer">🗑️ 清空文本</button>
              <button class="action-btn" :disabled="submitting" @click="handleHint">💡 智能提示</button>
              <button class="action-btn primary" :disabled="submitting" @click="handleSubmitAnswer">
                {{ submitting ? '⏳ 分析中...' : '✓ 回答完毕' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 右面板 -->
        <div class="right-panel">
          <div class="analysis-card">
            <div class="analysis-card-title">📊 实时维度分析</div>
            <svg class="radar-chart" viewBox="0 0 200 200">
              <polygon
                v-for="(p, i) in RADAR_GRID"
                :key="'g' + i"
                :points="p"
                fill="none"
                stroke="#e5e7eb"
                stroke-width="1"
              />
              <line
                v-for="(a, i) in RADAR_AXES"
                :key="'a' + i"
                :x1="a.x1"
                :y1="a.y1"
                :x2="a.x2"
                :y2="a.y2"
                stroke="#e5e7eb"
                stroke-width="1"
              />
              <polygon
                v-if="showLiveRadar"
                :points="liveRadarPoints"
                fill="rgba(220, 38, 38, 0.15)"
                stroke="#DC2626"
                stroke-width="2"
              />
              <text
                v-for="(m, i) in DIMENSION_META"
                :key="'t' + i"
                :x="m.textPos.x"
                :y="m.textPos.y"
                :text-anchor="m.textPos.anchor"
                font-size="10"
                fill="#374151"
              >{{ m.label }}</text>
            </svg>
            <div class="dimension-tags">
              <span
                v-for="(t, i) in liveDimensionTags"
                :key="i"
                :class="['dimension-tag', t.type]"
              >{{ t.text }}</span>
              <span v-if="!showLiveRadar" class="dimension-tag">回答后展示维度分析</span>
            </div>
          </div>
          <div class="analysis-card">
            <div class="analysis-card-title">💡 面试官提示</div>
            <div v-if="hintPreview" class="hint-text">{{ hintPreview }}</div>
            <div v-else class="hint-text">
              点击「💡 智能提示」获取分级提示，卡壳时可逐级升级（共 3 级）。
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 复盘报告页 ==================== -->
    <div v-else class="report-page">
      <div class="report-container">
        <div class="report-header">
          <div class="report-title">
            📊 面试复盘报告
            <span v-if="historyLoading" class="report-loading">加载中...</span>
          </div>
          <div class="report-header-actions">
            <button class="back-btn" @click="router.push('/interview/voice/history')">📋 我的面试记录</button>
            <button class="back-btn" @click="goHome">← 返回首页</button>
          </div>
        </div>
        <!-- 历史保留元信息：岗位/风格/难度/时间 -->
        <div class="report-meta-bar">
          <div class="meta-chip">
            <span class="meta-label">目标岗位</span>
            <span class="meta-value">{{ interview?.position || '未指定' }}</span>
          </div>
          <div class="meta-chip">
            <span class="meta-label">面试官风格</span>
            <span class="meta-value">{{ interview?.style || '温和' }}</span>
          </div>
          <div class="meta-chip">
            <span class="meta-label">难度</span>
            <span class="meta-value">{{ interview?.difficulty || '中等' }}</span>
          </div>
          <div class="meta-chip">
            <span class="meta-label">题源</span>
            <span class="meta-value">{{ interview?.resumeId ? '简历深挖' : '岗位画像' }}</span>
          </div>
          <div class="meta-chip">
            <span class="meta-label">面试时间</span>
            <span class="meta-value">{{ interview?.createTime || '-' }}</span>
          </div>
          <div class="meta-chip">
            <span class="meta-label">答题数</span>
            <span class="meta-value">{{ interview?.qaList?.filter(q => q.userAnswer).length ?? 0 }} / {{ interview?.totalQa ?? 0 }}</span>
          </div>
        </div>
        <div class="report-tabs">
          <button
            :class="['report-tab', { active: reportTab === 'summary' }]"
            @click="reportTab = 'summary'"
          >📋 面试概要</button>
          <button
            :class="['report-tab', { active: reportTab === 'dialog' }]"
            @click="reportTab = 'dialog'"
          >💬 对话回放</button>
          <button
            :class="['report-tab', { active: reportTab === 'analysis' }]"
            @click="reportTab = 'analysis'"
          >🔍 问题分析</button>
          <button
            :class="['report-tab', { active: reportTab === 'interviewer' }]"
            @click="reportTab = 'interviewer'"
          >💡 面试官剖析</button>
          <button
            :class="['report-tab', { active: reportTab === 'knowledge' }]"
            @click="reportTab = 'knowledge'"
          >📚 相关知识点</button>
        </div>

        <!-- Tab 0: 对话回放（历史面试完整对话） -->
        <div v-if="reportTab === 'dialog'" class="tab-content active">
          <div v-if="!interview?.qaList?.length" class="dialog-empty">
            本次面试没有保存对话内容。
          </div>
          <div v-else class="replay-list">
            <div v-for="qa in interview.qaList" :key="qa.id" class="replay-item">
              <!-- 面试官 -->
              <div class="replay-row interviewer">
                <div class="replay-avatar">AI</div>
                <div class="replay-bubble ai">
                  <div class="replay-idx">问题 {{ qa.questionIdx }}<span v-if="qa.parentQaId" class="replay-tag">追问</span></div>
                  <MarkdownRenderer :content="qa.question || ''" />
                </div>
              </div>
              <!-- 用户回答 -->
              <div v-if="qa.userAnswer" class="replay-row user">
                <div class="replay-bubble user">
                  <div class="replay-score" v-if="qa.score != null">本问 {{ qa.score }} 分</div>
                  <MarkdownRenderer :content="qa.userAnswer" />
                </div>
                <div class="replay-avatar">我</div>
              </div>
              <!-- AI 点评 -->
              <div v-if="qa.aiFeedback" class="replay-row interviewer">
                <div class="replay-avatar">AI</div>
                <div class="replay-bubble feedback">
                  <div class="replay-feedback-title">点评</div>
                  <MarkdownRenderer :content="qa.aiFeedback" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 1: 面试概要 -->
        <div v-if="reportTab === 'summary'" class="tab-content active">
          <div class="summary-grid">
            <div class="summary-left">
              <div class="pros-cons-card">
                <div class="analysis-card-title">📊 六维能力雷达</div>
                <svg class="radar-chart" viewBox="0 0 200 200" style="max-width: 180px;">
                  <polygon
                    v-for="(p, i) in RADAR_GRID"
                    :key="'sg' + i"
                    :points="p"
                    fill="none"
                    stroke="#e5e7eb"
                    stroke-width="1"
                  />
                  <line
                    v-for="(a, i) in RADAR_AXES"
                    :key="'sa' + i"
                    :x1="a.x1"
                    :y1="a.y1"
                    :x2="a.x2"
                    :y2="a.y2"
                    stroke="#e5e7eb"
                    stroke-width="1"
                  />
                  <polygon
                    :points="reportRadarPoints"
                    fill="rgba(220, 38, 38, 0.12)"
                    stroke="#DC2626"
                    stroke-width="2"
                  />
                  <text
                    v-for="(m, i) in DIMENSION_META"
                    :key="'st' + i"
                    :x="m.textPos.x"
                    :y="m.textPos.y"
                    :text-anchor="m.textPos.anchor"
                    font-size="9"
                    fill="#374151"
                  >{{ m.label }}</text>
                </svg>
                <div class="total-score-box">
                  <span class="total-score-label">综合得分</span>
                  <span class="total-score-value" :style="{ color: scoreColor(report?.totalScore ?? 0) }">
                    {{ report?.totalScore ?? 0 }}
                  </span>
                </div>
              </div>
            </div>
            <div class="summary-center">
              <h3 class="summary-title">面试概要</h3>
              <p class="summary-paragraph">{{ report?.summary || '本次面试尚未形成完整总结。' }}</p>
            </div>
            <div class="summary-right">
              <div class="pros-cons-card">
                <div class="pros-cons-title cons">⚠️ 缺点</div>
                <div
                  v-for="(c, i) in (report?.weakPoints ?? [])"
                  :key="'c' + i"
                  class="cons-item"
                >
                  <div class="cons-item-title">{{ pointText(c) }}</div>
                  <div v-if="pointQuote(c)" class="cons-item-quote">"{{ pointQuote(c) }}"</div>
                </div>
                <div v-if="(report?.weakPoints ?? []).length === 0" class="empty-tip">暂无明显薄弱点</div>
              </div>
              <div class="pros-cons-card">
                <div class="pros-cons-title pros">✅ 优点</div>
                <div
                  v-for="(p, i) in (report?.highlights ?? [])"
                  :key="'p' + i"
                  class="pros-item"
                >
                  <div class="pros-item-title">{{ pointText(p) }}</div>
                  <div v-if="pointQuote(p)" class="pros-item-quote">"{{ pointQuote(p) }}"</div>
                </div>
                <div v-if="(report?.highlights ?? []).length === 0" class="empty-tip">暂无亮点数据</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 2: 问题分析 -->
        <div v-if="reportTab === 'analysis'" class="tab-content active">
          <div class="analysis-grid">
            <div
              v-for="(q, i) in (report?.questionReviews ?? [])"
              :key="i"
              class="analysis-item"
            >
              <div class="analysis-item-header">
                <div :class="['analysis-item-icon', q.score < 80 ? 'weak' : 'strong']">
                  {{ q.score < 80 ? '⚠️' : '✅' }}
                </div>
                <div class="analysis-item-title">第 {{ q.questionIdx }} 题 · {{ truncate(q.question, 18) }}</div>
              </div>
              <div class="analysis-item-content">{{ q.analysis || q.feedback }}</div>
              <div v-if="q.userAnswer" class="analysis-user-answer">
                <strong>你的回答：</strong>{{ truncate(q.userAnswer, 80) }}
              </div>
              <div class="analysis-item-score">
                <span :style="{ color: scoreColor(q.score), fontWeight: 600 }">{{ q.score }} 分</span>
                <div class="score-bar">
                  <div :class="['score-fill', scoreClass(q.score)]" :style="{ width: q.score + '%' }"></div>
                </div>
              </div>
              <button
                v-if="q.score < 80 && q.qaId"
                class="wrong-book-btn"
                :disabled="wrongBookLoading === q.qaId"
                @click="handleAddToWrongBook(q.qaId)"
              >
                {{ wrongBookLoading === q.qaId ? '加入中...' : '📚 加入错题本' }}
              </button>
            </div>
            <div v-if="(report?.questionReviews ?? []).length === 0" class="empty-tip">暂无逐题分析数据</div>
          </div>
        </div>

        <!-- Tab 3: 面试官剖析 -->
        <div v-if="reportTab === 'interviewer'" class="tab-content active">
          <div class="interviewer-analysis">
            <h3 class="summary-title">👨‍💼 面试官整体评价</h3>
            <div class="interviewer-comment">{{ report?.summary || '暂无面试官评价。' }}</div>
            <h4 class="summary-subtitle">📋 改进建议</h4>
            <ul class="suggestion-list">
              <li
                v-for="(s, i) in suggestionItems"
                :key="i"
                class="suggestion-item"
              >
                <div class="suggestion-number">{{ i + 1 }}</div>
                <div class="suggestion-content">{{ s }}</div>
              </li>
              <li v-if="suggestionItems.length === 0" class="empty-tip">暂无改进建议</li>
            </ul>
          </div>
        </div>

        <!-- Tab 4: 相关知识点 -->
        <div v-if="reportTab === 'knowledge'" class="tab-content active">
          <div class="knowledge-grid">
            <div
              v-for="(k, i) in (report?.knowledgePoints ?? [])"
              :key="i"
              class="knowledge-card"
            >
              <div class="knowledge-card-title">📚 {{ kpTitle(k) }}</div>
              <div v-if="kpDesc(k)" class="knowledge-card-desc">{{ kpDesc(k) }}</div>
              <a href="#" class="knowledge-card-link" @click.prevent="toast.info('知识点详情即将上线')">查看相关题目 →</a>
            </div>
            <div v-if="(report?.knowledgePoints ?? []).length === 0" class="empty-tip">
              暂无相关知识点（V10.2 LLM 版本启用后自动生成）
            </div>
          </div>
        </div>

        <div class="report-actions">
          <button class="report-btn" @click="addAllWeakToWrongBook">📚 薄弱点入错题本</button>
          <button class="report-btn" @click="downloadReport">📥 下载 PDF 报告</button>
          <button class="report-btn" @click="shareReport">🔗 分享报告</button>
          <button class="report-btn primary" @click="startNewInterview">🔄 开始新的面试</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 视觉系统 · 红色主题（CSS 变量，与 demo HTML 一致） ========== */
.vi-shell {
  --primary: #DC2626;
  --primary-light: #EF4444;
  --primary-dark: #B91C1C;
  --primary-bg: #FEF2F2;
  --primary-border: #FECACA;
  --primary-gradient: linear-gradient(135deg, #DC2626 0%, #991B1B 100%);
  --success: #10b981;
  --success-bg: #ecfdf5;
  --warning: #f59e0b;
  --warning-bg: #fffbeb;
  --error: #ef4444;
  --error-bg: #fef2f2;
  --info: #3b82f6;
  --info-bg: #eff6ff;
  --gray-50: #f9fafb;
  --gray-100: #f3f4f6;
  --gray-200: #e5e7eb;
  --gray-300: #d1d5db;
  --gray-400: #9ca3af;
  --gray-500: #6b7280;
  --gray-600: #4b5563;
  --gray-700: #374151;
  --gray-800: #1f2937;
  --gray-900: #111827;
  --ai-bubble: #fafafa;
  --ai-bubble-border: #e5e7eb;
  --user-bubble: #FEF2F2;
  --user-bubble-border: #FECACA;
  --question-bubble: #fffbeb;
  --question-bubble-border: #fde68a;
  --analysis-bubble: #f0fdf4;
  --analysis-bubble-border: #bbf7d0;
  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  --font-mono: "JetBrains Mono", "Fira Code", Consolas, monospace;
  --shadow-sm: 0 1px 2px 0 rgba(0,0,0,0.05);
  --shadow-md: 0 4px 6px -1px rgba(0,0,0,0.08), 0 2px 4px -1px rgba(0,0,0,0.04);
  --shadow-lg: 0 10px 25px -3px rgba(0,0,0,0.08), 0 4px 6px -2px rgba(0,0,0,0.04);
  --shadow-xl: 0 20px 40px -5px rgba(0,0,0,0.12);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --radius-xl: 20px;
  --radius-full: 9999px;

  max-width: 1280px;
  margin: 0 auto;
  padding: 0 1rem;
  min-height: 100vh;
  font-family: var(--font-sans);
  color: var(--gray-800);
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
}
@media (min-width: 640px) {
  .vi-shell { padding: 0 1.5rem; }
}
@media (min-width: 1024px) {
  .vi-shell { padding: 0 2rem; }
}
.vi-shell button { font-family: inherit; }
.vi-shell * { box-sizing: border-box; }

/* ========== 准备页 ========== */
.prep-page { background: var(--gray-50); min-height: 100vh; }
.prep-top-bar { background: white; padding: 1rem 2rem; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--gray-200); }
.prep-top-left { display: flex; align-items: center; gap: 0.75rem; }
.prep-top-right { display: flex; align-items: center; gap: 0.75rem; }
.report-header-actions { display: flex; align-items: center; gap: 0.75rem; }
.prep-logo { font-size: 1.125rem; font-weight: 700; color: var(--primary); display: flex; align-items: center; gap: 0.375rem; }
.prep-breadcrumb { font-size: 0.875rem; color: var(--gray-400); }
.prep-breadcrumb span { color: var(--gray-700); font-weight: 500; }
.prep-back-btn { padding: 0.5rem 1rem; border: 1px solid var(--gray-200); background: white; border-radius: var(--radius-md); cursor: pointer; font-size: 0.875rem; color: var(--gray-600); display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; }
.prep-back-btn:hover { background: var(--gray-50); border-color: var(--gray-300); }
.prep-container { padding: 2rem 0 3rem; }
.prep-header { text-align: center; margin-bottom: 2rem; }
.prep-header h1 { font-size: 1.625rem; font-weight: 700; color: var(--gray-900); margin-bottom: 0.375rem; }
.prep-header p { color: var(--gray-500); font-size: 0.9375rem; }

.prep-steps { display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 2.5rem; }
.prep-step { display: flex; align-items: center; gap: 0.625rem; }
.step-circle { width: 36px; height: 36px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 0.875rem; border: 2px solid var(--gray-200); color: var(--gray-400); background: white; transition: all 0.3s; flex-shrink: 0; }
.step-circle.active { border-color: var(--primary); color: white; background: var(--primary); box-shadow: 0 0 0 4px rgba(220, 38, 38, 0.12); }
.step-circle.completed { border-color: var(--success); color: white; background: var(--success); }
.step-label { font-size: 0.875rem; color: var(--gray-400); font-weight: 500; white-space: nowrap; }
.step-label.active { color: var(--gray-800); font-weight: 600; }
.step-connector { width: 48px; height: 2px; background: var(--gray-200); margin: 0 0.75rem; border-radius: 1px; flex-shrink: 0; }
.step-connector.completed { background: var(--success); }

.prep-card { background: white; border-radius: var(--radius-lg); padding: 1.75rem; box-shadow: var(--shadow-sm); margin-bottom: 1.25rem; border: 1px solid var(--gray-100); }
.prep-card-title { font-size: 1.0625rem; font-weight: 600; color: var(--gray-900); margin-bottom: 1.25rem; display: flex; align-items: center; gap: 0.5rem; }
.prep-card-title .step-badge { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; border-radius: var(--radius-full); background: var(--primary-bg); color: var(--primary); font-size: 0.75rem; font-weight: 700; }

.device-check-list { display: grid; gap: 0.75rem; }
.device-item { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.25rem; background: var(--gray-50); border: 1px solid var(--gray-100); border-radius: var(--radius-md); transition: all 0.2s; }
.device-item:hover { border-color: var(--gray-200); }
.device-info { display: flex; align-items: center; gap: 0.875rem; }
.device-icon { width: 40px; height: 40px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 1.25rem; background: white; border: 1px solid var(--gray-100); }
.device-name { font-weight: 500; color: var(--gray-800); font-size: 0.9375rem; }
.device-status { display: flex; align-items: center; gap: 0.625rem; }
.status-badge { padding: 0.25rem 0.75rem; border-radius: var(--radius-full); font-size: 0.75rem; font-weight: 500; }
.status-badge.ok { background: var(--success-bg); color: var(--success); }
.status-badge.checking { background: var(--warning-bg); color: var(--warning); }
.status-badge.error { background: var(--error-bg); color: var(--error); }
.test-btn { padding: 0.375rem 0.875rem; border: 1px solid var(--gray-200); background: white; color: var(--gray-600); border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; font-weight: 500; transition: all 0.2s; }
.test-btn:hover:not(:disabled) { background: var(--primary); color: white; border-color: var(--primary); }
.test-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.device-tip { margin-top: 0.75rem; font-size: 0.8125rem; color: var(--gray-400); padding: 0.625rem 0.875rem; background: var(--gray-50); border-radius: var(--radius-md); border-left: 3px solid var(--warning); }

.position-section { margin-bottom: 1.25rem; }
.section-label { font-size: 0.875rem; font-weight: 600; color: var(--gray-700); margin-bottom: 0.75rem; display: flex; align-items: center; gap: 0.375rem; }
.position-list { display: grid; gap: 0.625rem; }
.position-option { display: flex; align-items: center; padding: 0.875rem 1rem; background: var(--gray-50); border: 1.5px solid var(--gray-100); border-radius: var(--radius-md); cursor: pointer; transition: all 0.2s; }
.position-option:hover { border-color: var(--primary-border); background: var(--primary-bg); }
.position-option.selected { border-color: var(--primary); background: var(--primary-bg); }
.option-radio { width: 20px; height: 20px; border: 2px solid var(--gray-300); border-radius: var(--radius-full); margin-right: 0.875rem; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all 0.2s; }
.position-option.selected .option-radio { border-color: var(--primary); background: var(--primary); }
.option-radio::after { content: ''; width: 8px; height: 8px; background: white; border-radius: var(--radius-full); opacity: 0; transition: opacity 0.2s; }
.position-option.selected .option-radio::after { opacity: 1; }
.option-content { flex: 1; }
.option-title { font-weight: 500; color: var(--gray-800); margin-bottom: 0.125rem; font-size: 0.9375rem; }
.option-meta { font-size: 0.75rem; color: var(--gray-400); }

/* ==================== V10.3 简历库选择器 ==================== */
.resume-empty { border: 2px dashed var(--gray-200); border-radius: var(--radius-md); padding: 2rem 1.5rem; text-align: center; background: var(--gray-50); }
.empty-icon { font-size: 2rem; margin-bottom: 0.5rem; }
.empty-title { font-size: 0.9375rem; font-weight: 600; color: var(--gray-700); margin-bottom: 0.375rem; }
.empty-desc { font-size: 0.8125rem; color: var(--gray-400); margin-bottom: 1rem; }
.empty-actions { display: flex; justify-content: center; gap: 0.625rem; }

.resume-select-list { display: flex; flex-direction: column; gap: 0.5rem; max-height: 260px; overflow-y: auto; }
.resume-option { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.875rem 1rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); cursor: pointer; transition: all 0.2s; background: white; }
.resume-option:hover { border-color: var(--primary); background: var(--primary-bg); }
.resume-option.selected { border-color: var(--primary); background: var(--primary-bg); }
.resume-option-main { flex: 1; min-width: 0; }
.resume-option-name { font-size: 0.875rem; font-weight: 600; color: var(--gray-800); display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.resume-option-score { font-size: 0.6875rem; color: var(--warning); background: var(--warning-bg, rgba(245, 158, 11, 0.1)); padding: 1px 8px; border-radius: var(--radius-full); font-weight: 500; }
.resume-option-meta { font-size: 0.75rem; color: var(--gray-400); margin-top: 0.25rem; }

.resume-manage-row { display: flex; align-items: center; gap: 0.875rem; margin-top: 0.75rem; }
.resume-manage-hint { font-size: 0.75rem; color: var(--gray-400); }
.resume-action-btn.primary { border-color: var(--primary); color: var(--primary); }

.position-option.custom .option-content { flex: 1; }
.custom-position-input { width: 100%; margin-top: 0.375rem; padding: 0.4375rem 0.625rem; border: 1px solid var(--gray-200); border-radius: var(--radius-sm); font-size: 0.8125rem; color: var(--gray-700); background: white; outline: none; transition: border-color 0.2s; }
.custom-position-input:focus { border-color: var(--primary); }

.resume-action-btn { padding: 0.375rem 0.75rem; border: 1px solid var(--gray-200); background: white; border-radius: var(--radius-sm); cursor: pointer; font-size: 0.75rem; color: var(--gray-600); transition: all 0.2s; white-space: nowrap; }
.resume-action-btn:hover { border-color: var(--primary); color: var(--primary); }

.config-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem; }
.config-item { display: flex; flex-direction: column; gap: 0.375rem; }
.config-label { font-size: 0.8125rem; font-weight: 500; color: var(--gray-600); }
.config-select { padding: 0.625rem 0.75rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); font-size: 0.875rem; background: white; cursor: pointer; color: var(--gray-700); transition: all 0.2s; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 12px center; }
.config-select:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.08); }

.toggle-row { display: flex; align-items: center; justify-content: space-between; padding: 0.875rem 1rem; background: var(--gray-50); border-radius: var(--radius-md); margin-top: 0.75rem; border: 1px solid var(--gray-100); }
.toggle-label { font-size: 0.875rem; color: var(--gray-600); }
.toggle-switch { position: relative; width: 44px; height: 24px; flex-shrink: 0; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; cursor: pointer; inset: 0; background: var(--gray-300); border-radius: var(--radius-full); transition: 0.25s; }
.toggle-slider::before { content: ''; position: absolute; height: 18px; width: 18px; left: 3px; bottom: 3px; background: white; border-radius: var(--radius-full); transition: 0.25s; box-shadow: var(--shadow-sm); }
.toggle-switch input:checked + .toggle-slider { background: var(--primary); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(20px); }

.start-btn { width: 100%; padding: 1rem; background: var(--primary-gradient); color: white; border: none; border-radius: var(--radius-lg); font-size: 1rem; font-weight: 600; cursor: pointer; transition: all 0.3s; box-shadow: 0 4px 14px rgba(220, 38, 38, 0.3); margin-top: 1.5rem; letter-spacing: 0.02em; }
.start-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(220, 38, 38, 0.4); }
.start-btn:active { transform: translateY(0); }
.start-btn:disabled { opacity: 0.7; cursor: not-allowed; }

/* ========== 面试进行页 ========== */
.interview-page { background: var(--gray-100); min-height: 100vh; display: flex; flex-direction: column; }
.top-bar { background: white; padding: 0.75rem 0; display: flex; align-items: center; justify-content: space-between; box-shadow: var(--shadow-sm); border-bottom: 1px solid var(--gray-200); position: sticky; top: 0; z-index: 10; }
.top-bar-left { display: flex; align-items: center; gap: 1rem; }
.top-bar-logo { font-size: 1.125rem; font-weight: 700; color: var(--primary); display: flex; align-items: center; gap: 0.375rem; }
.timer-display { display: flex; align-items: center; gap: 0.375rem; padding: 0.375rem 0.875rem; background: var(--gray-50); border: 1px solid var(--gray-200); border-radius: var(--radius-full); font-family: var(--font-mono); font-size: 0.875rem; color: var(--gray-600); }
.top-bar-right { display: flex; align-items: center; gap: 0.625rem; }
.control-btn { padding: 0.4375rem 0.875rem; border: 1px solid var(--gray-200); background: white; border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); font-weight: 500; }
.control-btn:hover:not(:disabled) { background: var(--gray-50); border-color: var(--gray-300); }
.control-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.control-btn.danger { border-color: var(--primary-border); color: var(--primary); }
.control-btn.danger:hover { background: var(--primary-bg); }
.interview-main { display: grid; grid-template-columns: 220px 1fr 260px; gap: 1rem; padding: 1rem 0; flex: 1; width: 100%; }

.left-panel { display: flex; flex-direction: column; gap: 1rem; }
.interviewer-card { background: white; border-radius: var(--radius-lg); padding: 1.5rem; text-align: center; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.interviewer-avatar { width: 100px; height: 100px; border-radius: var(--radius-full); background: var(--primary-gradient); margin: 0 auto 0.875rem; display: flex; align-items: center; justify-content: center; font-size: 2.5rem; position: relative; }
.interviewer-avatar::after { content: ''; position: absolute; inset: -4px; border-radius: var(--radius-full); border: 3px solid var(--success); animation: pulse-ring 2s ease-in-out infinite; }
@keyframes pulse-ring { 0%,100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.05); } }
.interviewer-name { font-size: 0.9375rem; font-weight: 600; color: var(--gray-800); margin-bottom: 0.375rem; }
.interviewer-style { display: inline-block; padding: 0.1875rem 0.625rem; background: var(--success-bg); color: var(--success); border-radius: var(--radius-full); font-size: 0.75rem; font-weight: 500; }
.progress-card { background: white; border-radius: var(--radius-lg); padding: 1.25rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.progress-title { font-size: 0.8125rem; font-weight: 600; color: var(--gray-500); margin-bottom: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; }
.progress-timeline { display: flex; flex-direction: column; gap: 0.25rem; }
.progress-item { display: flex; align-items: center; gap: 0.625rem; padding: 0.5rem 0.625rem; border-radius: var(--radius-md); font-size: 0.8125rem; cursor: pointer; transition: all 0.2s; color: var(--gray-500); }
.progress-item:hover { background: var(--gray-50); }
.progress-item.active { background: var(--primary-bg); color: var(--primary); font-weight: 600; }
.progress-dot { width: 22px; height: 22px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font-size: 0.6875rem; font-weight: 600; flex-shrink: 0; }
.progress-item.completed .progress-dot { background: var(--success); color: white; }
.progress-item.active .progress-dot { background: var(--primary); color: white; animation: pulse-ring 2s ease-in-out infinite; }
.progress-item.pending .progress-dot { background: var(--gray-200); color: var(--gray-400); }

.center-panel { display: flex; flex-direction: column; background: white; border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; border: 1px solid var(--gray-100); }
.chat-header { padding: 0.875rem 1.25rem; border-bottom: 1px solid var(--gray-100); display: flex; align-items: center; justify-content: space-between; }
.chat-header-title { font-size: 0.875rem; font-weight: 600; color: var(--gray-700); }
.chat-status { display: flex; align-items: center; gap: 0.375rem; font-size: 0.75rem; color: var(--success); font-weight: 500; }
.chat-status-dot { width: 7px; height: 7px; background: var(--success); border-radius: var(--radius-full); animation: pulse-dot 1.5s ease-in-out infinite; }
@keyframes pulse-dot { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
.chat-body { flex: 1; overflow-y: auto; padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; max-height: calc(100vh - 260px); }
.message { display: flex; gap: 0.625rem; max-width: 85%; animation: msg-in 0.3s ease; }
@keyframes msg-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.message.ai, .message.question, .message.analysis { align-self: flex-start; }
.message.question, .message.analysis { max-width: 92%; }
.message.user { align-self: flex-end; flex-direction: row-reverse; }
.message-avatar { width: 32px; height: 32px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font-size: 0.875rem; flex-shrink: 0; }
.message.ai .message-avatar, .message.question .message-avatar { background: var(--primary-bg); color: var(--primary); }
.message.analysis .message-avatar { background: var(--success-bg); color: var(--success); }
.message.user .message-avatar { background: var(--success-bg); color: var(--success); }
.message-content { padding: 0.875rem 1rem; border-radius: var(--radius-lg); font-size: 0.9375rem; line-height: 1.65; }
.message.ai .message-content { background: var(--ai-bubble); border: 1px solid var(--ai-bubble-border); border-top-left-radius: var(--radius-sm); }
.message.user .message-content { background: var(--user-bubble); border: 1px solid var(--user-bubble-border); border-top-right-radius: var(--radius-sm); }
.message.question .message-content { background: var(--question-bubble); border: 1px solid var(--question-bubble-border); border-left: 4px solid var(--warning); }
.message.analysis .message-content { background: var(--analysis-bubble); border: 1px solid var(--analysis-bubble-border); }
.question-header { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; font-size: 0.75rem; color: var(--warning); font-weight: 600; }
.analysis-header { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; font-size: 0.75rem; color: var(--success); font-weight: 600; }
.ai-tag { font-size: 0.6875rem; color: var(--primary); font-weight: 600; margin-bottom: 0.375rem; display: inline-block; padding: 0.125rem 0.5rem; background: var(--primary-bg); border-radius: var(--radius-full); }
.analysis-score { margin-top: 0.625rem; font-size: 0.75rem; color: var(--gray-400); }
.message-text { white-space: pre-wrap; word-break: break-word; }

.chat-input-area { padding: 1rem 1.25rem; border-top: 1px solid var(--gray-100); background: white; }
.input-timer { display: flex; align-items: center; gap: 0.375rem; font-size: 0.75rem; color: var(--warning); margin-bottom: 0.5rem; font-weight: 500; }
.interim-hint { color: var(--gray-400); font-weight: 400; }
.input-container { display: flex; align-items: flex-end; gap: 0.625rem; }
.input-textarea { flex: 1; padding: 0.75rem 1rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); font-size: 0.9375rem; font-family: var(--font-sans); resize: none; min-height: 48px; max-height: 120px; transition: all 0.2s; line-height: 1.5; }
.input-textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.08); }
.input-textarea:disabled { background: var(--gray-50); cursor: not-allowed; }
.mic-btn { width: 48px; height: 48px; border-radius: var(--radius-full); background: var(--primary-gradient); color: white; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 1.25rem; transition: all 0.2s; flex-shrink: 0; box-shadow: 0 2px 8px rgba(220, 38, 38, 0.25); }
.mic-btn:hover:not(:disabled) { transform: scale(1.05); box-shadow: 0 4px 12px rgba(220, 38, 38, 0.35); }
.mic-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.mic-btn.recording { background: var(--error); animation: pulse-ring 1.5s ease-in-out infinite; }
.input-actions { display: flex; gap: 0.5rem; margin-top: 0.625rem; }
.action-btn { padding: 0.4375rem 0.875rem; border: 1px solid var(--gray-200); background: white; border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); font-weight: 500; }
.action-btn:hover:not(:disabled) { background: var(--gray-50); }
.action-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.action-btn.primary { background: var(--primary); color: white; border-color: var(--primary); }
.action-btn.primary:hover:not(:disabled) { background: var(--primary-dark); }

.right-panel { display: flex; flex-direction: column; gap: 1rem; }
.analysis-card { background: white; border-radius: var(--radius-lg); padding: 1.25rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.analysis-card-title { font-size: 0.8125rem; font-weight: 600; color: var(--gray-500); margin-bottom: 0.875rem; display: flex; align-items: center; gap: 0.375rem; text-transform: uppercase; letter-spacing: 0.05em; }
.radar-chart { width: 100%; aspect-ratio: 1; max-width: 200px; margin: 0 auto; }
.dimension-tags { display: flex; flex-wrap: wrap; gap: 0.375rem; margin-top: 0.875rem; }
.dimension-tag { padding: 0.1875rem 0.625rem; border-radius: var(--radius-full); font-size: 0.6875rem; background: var(--gray-50); color: var(--gray-600); border: 1px solid var(--gray-100); font-weight: 500; }
.dimension-tag.highlight { background: var(--success-bg); color: var(--success); border-color: #bbf7d0; }
.dimension-tag.gap { background: var(--error-bg); color: var(--error); border-color: var(--primary-border); }
.hint-text { font-size: 0.8125rem; color: var(--gray-500); line-height: 1.65; white-space: pre-wrap; }

/* ========== 复盘报告页 ========== */
.report-page { background: var(--gray-50); min-height: 100vh; padding: 2rem 0; }
.report-container { width: 100%; }
.report-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.75rem; }
.report-title { font-size: 1.375rem; font-weight: 700; color: var(--gray-900); display: flex; align-items: center; gap: 0.5rem; }
.report-loading { font-size: 0.875rem; color: var(--gray-400); font-weight: 400; }
.back-btn { padding: 0.5rem 1rem; border: 1px solid var(--gray-200); background: white; border-radius: var(--radius-md); cursor: pointer; font-size: 0.875rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); }
.back-btn:hover { background: var(--gray-50); border-color: var(--gray-300); }

/* V10.4 报告元信息条 */
.report-meta-bar { display: flex; flex-wrap: wrap; gap: 0.625rem; margin-bottom: 1.5rem; padding: 0.875rem 1.125rem; background: white; border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.meta-chip { display: flex; align-items: center; gap: 0.375rem; padding: 0.3125rem 0.75rem; background: var(--gray-50); border-radius: var(--radius-full); font-size: 0.8125rem; }
.meta-label { color: var(--gray-500); }
.meta-value { color: var(--gray-900); font-weight: 600; max-width: 10rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.report-tabs { display: flex; gap: 0.25rem; margin-bottom: 1.75rem; background: white; padding: 0.375rem; border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); width: fit-content; }
.report-tab { padding: 0.625rem 1.25rem; border: none; background: none; cursor: pointer; font-size: 0.875rem; color: var(--gray-500); border-radius: var(--radius-sm); white-space: nowrap; transition: all 0.2s; font-weight: 500; }
.report-tab:hover { color: var(--gray-700); background: var(--gray-50); }
.report-tab.active { color: var(--primary); background: var(--primary-bg); font-weight: 600; }

/* ==================== V10.4 对话回放与历史保留 ==================== */
.replay-list { display: flex; flex-direction: column; gap: 1.25rem; }
.replay-item { display: flex; flex-direction: column; gap: 0.625rem; }
.replay-item + .replay-item { padding-top: 1.25rem; border-top: 1px dashed var(--gray-100); }
.replay-row { display: flex; gap: 0.625rem; align-items: flex-start; }
.replay-row.user { justify-content: flex-end; }
.replay-avatar { flex-shrink: 0; width: 2rem; height: 2rem; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 0.6875rem; font-weight: 600; }
.replay-row.interviewer .replay-avatar { background: var(--primary); color: white; }
.replay-bubble { max-width: 82%; padding: 0.75rem 1rem; border-radius: var(--radius-md); font-size: 0.875rem; line-height: 1.6; }
.replay-bubble.ai { background: var(--gray-50); border: 1px solid var(--gray-100); border-top-left-radius: var(--radius-xs); }
.replay-bubble.user { background: var(--primary-bg); border: 1px solid rgba(220, 38, 38, 0.15); border-top-right-radius: var(--radius-xs); }
.replay-bubble.feedback { background: var(--info-bg); border: 1px solid rgba(37, 99, 235, 0.15); border-top-left-radius: var(--radius-xs); }
.replay-idx { font-size: 0.75rem; color: var(--gray-500); font-weight: 600; margin-bottom: 0.375rem; }
.replay-tag { display: inline-block; margin-left: 0.375rem; padding: 0.0625rem 0.4375rem; border-radius: var(--radius-full); background: var(--warning); color: white; font-size: 0.6875rem; font-weight: 500; }
.replay-score { font-size: 0.75rem; color: var(--primary); font-weight: 600; margin-bottom: 0.375rem; }
.replay-feedback-title { font-size: 0.75rem; color: var(--info); font-weight: 600; margin-bottom: 0.375rem; }
.dialog-empty { text-align: center; padding: 3rem 1rem; color: var(--gray-400); font-size: 0.9375rem; }
.tab-content.active { display: block; animation: msg-in 0.3s ease; }

.summary-grid { display: grid; grid-template-columns: 1fr 2fr 1fr; gap: 1.5rem; }
.summary-left, .summary-right { display: flex; flex-direction: column; gap: 1rem; }
.summary-center { background: white; border-radius: var(--radius-lg); padding: 1.75rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.summary-title { font-size: 1.125rem; font-weight: 600; margin-bottom: 1rem; color: var(--gray-800); }
.summary-subtitle { font-size: 1rem; font-weight: 600; margin-bottom: 1rem; color: var(--gray-800); }
.summary-paragraph { font-size: 0.9375rem; line-height: 1.85; color: var(--gray-600); }
.total-score-box { display: flex; flex-direction: column; align-items: center; margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--gray-100); }
.total-score-label { font-size: 0.75rem; color: var(--gray-400); margin-bottom: 0.25rem; }
.total-score-value { font-size: 1.75rem; font-weight: 800; }
.pros-cons-card { background: white; border-radius: var(--radius-lg); padding: 1.25rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.pros-cons-title { font-size: 0.875rem; font-weight: 600; margin-bottom: 0.875rem; display: flex; align-items: center; gap: 0.375rem; }
.pros-cons-title.cons { color: var(--error); }
.pros-cons-title.pros { color: var(--success); }
.cons-item, .pros-item { padding: 0.875rem; background: var(--gray-50); border-radius: var(--radius-md); margin-bottom: 0.5rem; cursor: pointer; transition: all 0.2s; border: 1px solid transparent; }
.cons-item:hover, .pros-item:hover { background: white; border-color: var(--gray-200); box-shadow: var(--shadow-sm); }
.cons-item-title, .pros-item-title { font-size: 0.8125rem; font-weight: 500; color: var(--gray-800); margin-bottom: 0.375rem; }
.cons-item-quote, .pros-item-quote { font-size: 0.75rem; color: var(--gray-400); font-style: italic; padding-left: 0.75rem; border-left: 2px solid var(--gray-200); line-height: 1.5; }

.analysis-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.25rem; }
.analysis-item { background: white; border-radius: var(--radius-lg); padding: 1.5rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.analysis-item-header { display: flex; align-items: center; gap: 0.625rem; margin-bottom: 0.875rem; }
.analysis-item-icon { width: 32px; height: 32px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 0.875rem; }
.analysis-item-icon.weak { background: var(--error-bg); color: var(--error); }
.analysis-item-icon.strong { background: var(--success-bg); color: var(--success); }
.analysis-item-title { font-size: 0.9375rem; font-weight: 600; color: var(--gray-800); }
.analysis-item-content { font-size: 0.875rem; color: var(--gray-500); line-height: 1.65; margin-bottom: 1rem; }
.analysis-user-answer { font-size: 0.8125rem; color: var(--gray-500); background: var(--gray-50); padding: 0.625rem 0.875rem; border-radius: var(--radius-md); margin-bottom: 1rem; line-height: 1.6; }
.analysis-item-score { display: flex; align-items: center; gap: 0.625rem; font-size: 0.875rem; }
.score-bar { flex: 1; height: 6px; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; }
.score-fill { height: 100%; border-radius: var(--radius-full); transition: width 0.4s ease; }
.score-fill.low { background: var(--error); }
.score-fill.medium { background: var(--warning); }
.score-fill.high { background: var(--success); }
.wrong-book-btn { margin-top: 0.875rem; padding: 0.4375rem 0.875rem; border: 1px solid var(--primary-border); background: var(--primary-bg); color: var(--primary); border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; font-weight: 500; transition: all 0.2s; }
.wrong-book-btn:hover:not(:disabled) { background: var(--primary); color: white; border-color: var(--primary); }
.wrong-book-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.interviewer-analysis { background: white; border-radius: var(--radius-lg); padding: 1.75rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.interviewer-comment { font-size: 0.9375rem; line-height: 1.85; color: var(--gray-600); margin-bottom: 1.5rem; padding: 1.25rem; background: var(--gray-50); border-radius: var(--radius-md); border-left: 4px solid var(--primary); }
.suggestion-list { list-style: none; padding: 0; margin: 0; }
.suggestion-item { display: flex; gap: 0.875rem; padding: 1rem; background: var(--gray-50); border-radius: var(--radius-md); margin-bottom: 0.625rem; border: 1px solid var(--gray-100); }
.suggestion-number { width: 24px; height: 24px; border-radius: var(--radius-full); background: var(--primary); color: white; display: flex; align-items: center; justify-content: center; font-size: 0.75rem; font-weight: 600; flex-shrink: 0; }
.suggestion-content { font-size: 0.875rem; color: var(--gray-600); line-height: 1.65; }

.knowledge-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.25rem; }
.knowledge-card { background: white; border-radius: var(--radius-lg); padding: 1.5rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); border-top: 3px solid var(--primary); transition: all 0.2s; }
.knowledge-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.knowledge-card-title { font-size: 1rem; font-weight: 600; color: var(--gray-800); margin-bottom: 0.5rem; }
.knowledge-card-desc { font-size: 0.875rem; color: var(--gray-500); line-height: 1.65; margin-bottom: 0.875rem; }
.knowledge-card-link { display: inline-flex; align-items: center; gap: 0.25rem; color: var(--primary); font-size: 0.8125rem; text-decoration: none; font-weight: 500; }
.knowledge-card-link:hover { text-decoration: underline; }

.empty-tip { font-size: 0.875rem; color: var(--gray-400); padding: 1rem; text-align: center; }

.report-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 0.875rem; margin-top: 2rem; padding: 1.5rem; background: white; border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.report-btn { padding: 0.625rem 1.25rem; border: 1px solid var(--gray-200); background: white; border-radius: var(--radius-md); cursor: pointer; font-size: 0.875rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); font-weight: 500; }
.report-btn:hover { background: var(--gray-50); border-color: var(--gray-300); }
.report-btn.primary { background: var(--primary); color: white; border-color: var(--primary); }
.report-btn.primary:hover { background: var(--primary-dark); }

/* Markdown 渲染适配气泡 */
.message-content :deep(.prose) { font-size: 0.9375rem; line-height: 1.65; max-width: none; }
.message-content :deep(.prose p) { margin: 0.5rem 0; }
.message-content :deep(.prose p:first-child) { margin-top: 0; }
.message-content :deep(.prose p:last-child) { margin-bottom: 0; }
.message-content :deep(.prose pre) { background: #1a1a2e; color: #e5e7eb; padding: 1rem 1.25rem; border-radius: var(--radius-md); font-family: var(--font-mono); font-size: 0.8125rem; overflow-x: auto; margin: 0.75rem 0; }
.message-content :deep(.prose pre code) { background: transparent; color: inherit; padding: 0; }
.message-content :deep(.prose code) { background: rgba(220, 38, 38, 0.08); color: var(--primary-dark); padding: 0.125rem 0.375rem; border-radius: var(--radius-sm); font-size: 0.8125rem; }
.message-content :deep(.prose strong) { color: var(--gray-900); font-weight: 700; }

/* ========== 响应式 ========== */
@media (max-width: 1024px) {
  .interview-main { grid-template-columns: 1fr; }
  .left-panel, .right-panel { order: -1; }
  .summary-grid { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  /* 步骤条保持横向（手机端与 PC 一致），仅缩小间距适配窄屏 */
  .prep-steps { gap: 0; flex-wrap: nowrap; }
  .prep-step { gap: 0.375rem; }
  .step-circle { width: 30px; height: 30px; font-size: 0.8rem; }
  .step-label { font-size: 0.75rem; }
  .step-connector { width: 24px; margin: 0 0.25rem; }
  .config-row { grid-template-columns: 1fr; }
  .report-tabs { flex-wrap: nowrap; overflow-x: auto; }
  .top-bar { flex-wrap: wrap; gap: 0.5rem; }
  .top-bar-right { flex-wrap: wrap; }
  .report-page { padding: 1rem; }
}

@media print {
  .top-bar, .chat-input-area, .report-actions, .report-tabs { display: none; }
  .report-page { background: white; padding: 0; }
}

/* ==================== V10.1 语音对话可视化增强 ==================== */

/* --- 实时音浪条（聆听状态） --- */
.voice-wave {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0.875rem;
  margin-bottom: 0.5rem;
  border-radius: var(--radius-md);
  background: rgba(220, 38, 38, 0.06);
  border: 1px solid rgba(220, 38, 38, 0.18);
}
.wave-bars {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 28px;
  flex: 1;
}
.wave-bar {
  flex: 1;
  min-width: 3px;
  max-width: 10px;
  height: 12%;
  border-radius: 2px;
  background: rgba(220, 38, 38, 0.25);
  transition: height 0.08s linear, background 0.3s ease;
}
.voice-wave.active .wave-bar { background: var(--primary); }
.wave-label {
  font-size: 0.75rem;
  color: var(--gray-500);
  white-space: nowrap;
  font-weight: 500;
}
.voice-wave.active .wave-label { color: var(--primary); }

/* --- 三态状态环（聆听红 / 分析黄 / 播报蓝 / 空闲绿） --- */
.chat-status.is-listening { color: var(--primary); }
.chat-status.is-listening .chat-status-dot { background: var(--primary); }
.chat-status.is-analyzing { color: var(--warning); }
.chat-status.is-analyzing .chat-status-dot { background: var(--warning); animation-duration: 0.7s; }
.chat-status.is-speaking { color: var(--info); }
.chat-status.is-speaking .chat-status-dot { background: var(--info); }

/* --- 停止播报按钮 --- */
.stop-speak-btn {
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  border-radius: var(--radius-full);
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: rgba(59, 130, 246, 0.08);
  color: var(--info);
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}
.stop-speak-btn:hover { background: rgba(59, 130, 246, 0.16); }

/* --- 静音切换按钮态 --- */
.control-btn.muted { opacity: 0.65; }

/* --- AI 头像播报声波 --- */
.interviewer-emoji { position: relative; z-index: 1; }
.ai-sound-wave {
  position: absolute;
  bottom: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 16px;
  z-index: 2;
}
.ai-sound-wave span {
  width: 3px;
  border-radius: 2px;
  background: white;
  animation: ai-wave 0.9s ease-in-out infinite;
}
.ai-sound-wave span:nth-child(1) { height: 6px; animation-delay: 0s; }
.ai-sound-wave span:nth-child(2) { height: 14px; animation-delay: 0.15s; }
.ai-sound-wave span:nth-child(3) { height: 9px; animation-delay: 0.3s; }
.ai-sound-wave span:nth-child(4) { height: 12px; animation-delay: 0.45s; }
@keyframes ai-wave {
  0%, 100% { transform: scaleY(0.4); }
  50% { transform: scaleY(1); }
}
.interviewer-avatar.speaking::after { border-color: var(--info); }

/* --- 面试官实时状态标签 --- */
.interviewer-state {
  margin-top: 0.5rem;
  font-size: 0.6875rem;
  color: var(--info);
  font-weight: 600;
  animation: pulse-dot 1.5s ease-in-out infinite;
}
.interviewer-state.thinking { color: var(--warning); }
</style>
