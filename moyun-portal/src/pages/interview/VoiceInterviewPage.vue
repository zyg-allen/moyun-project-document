<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { usePromptModal } from '@/composables/usePromptModal';
import { useConfirmModal } from '@/composables/useConfirmModal';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { useHead } from '@vueuse/head';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import ResumeParsePreviewModal from '@/components/resume/ResumeParsePreviewModal.vue';
import { generateSeo } from '@/utils/seo';
import { useToast } from '@/composables/useToast';
import { useApiCall } from '@/composables/useApiCall';
import { useSpeechSynthesis } from '@/composables/useSpeechSynthesis';
import { useSpeechRecognition } from '@/composables/useSpeechRecognition';
import { useAudioLevel } from '@/composables/useAudioLevel';
import { useMediaDevices } from '@/composables/useMediaDevices';
import {
  startVoiceInterview,
  submitVoiceAnswer,
  requestVoiceHint,
  finishVoiceInterview,
  getVoiceAnalysisStatus,
  getVoiceInterviewDetail,
  getActiveVoiceInterview,
  resumeVoiceInterview,
  addQaToWrongBook,
  createReportShareToken,
  regenerateVoiceReport,
  generateVoiceIndustryInsight,
} from '@/api/voiceInterview';
import { getMyResumeList, getJobTemplates } from '@/api/interview';
import { previewResumeParse, confirmResumeParse } from '@/api/resumeParse';
import { getVipStatus, benefitLeft } from '@/api/vip';
import { pollAiTask } from '@/api/aiTask';
import type { UserResumeVO, JobTemplateOptionVO, ResumePreviewVO } from '@/types/api';
import { useUserStore } from '@/stores/user';
import type {
  VoiceInterviewVO,
  VoiceInterviewReportVO,
  VoiceStartConfig,
  PointItem,
  ActiveVoiceInterviewVO,
} from '@/api/voiceInterview';

useHead({
  title: 'AI 语音面试官 - 墨云',
  meta: generateSeo({
    title: 'AI 语音面试官',
    description: '沉浸式语音面试，TTS 播报 + ASR 识别 + 智能评分反馈',
  }),
});



const promptModal = usePromptModal();

const confirmModal = useConfirmModal();

const toast = useToast();
const { run } = useApiCall();
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// ==================== 6 维度元数据（与后端 dimensions key 对齐） ====================
interface DimMeta {
  key: 'relevance' | 'professionalism' | 'fluency' | 'interactivity' | 'confidence' | 'logic';
  label: string;
  /** v11.89：评分依据说明（雷达图轴标签太短，图例区逐维解释"按什么打分"） */
  desc: string;
  vertex: [number, number];
  textPos: { x: number; y: number; anchor: 'middle' | 'start' | 'end' };
}
const DIMENSION_META: DimMeta[] = [
  { key: 'relevance', label: '回答相关性', desc: '是否切题，紧扣问题作答不跑偏', vertex: [100, 20], textPos: { x: 100, y: 15, anchor: 'middle' } },
  { key: 'professionalism', label: '专业度', desc: '技术/业务深度与表述准确性', vertex: [180, 60], textPos: { x: 190, y: 55, anchor: 'start' } },
  { key: 'fluency', label: '表达流畅度', desc: '语速节奏、停顿控制，少口头禅', vertex: [180, 140], textPos: { x: 190, y: 145, anchor: 'start' } },
  { key: 'interactivity', label: '面试互动性', desc: '追问应对从容，回答结构完整', vertex: [100, 180], textPos: { x: 100, y: 195, anchor: 'middle' } },
  { key: 'confidence', label: '自信度', desc: '语气坚定，不犹疑不闪躲', vertex: [20, 140], textPos: { x: 10, y: 145, anchor: 'end' } },
  { key: 'logic', label: '逻辑清晰', desc: '条理分层，建议 STAR 结构作答', vertex: [20, 60], textPos: { x: 10, y: 55, anchor: 'end' } },
];
const RADAR_GRID = [
  '100,20 180,60 180,140 100,180 20,140 20,60',
  '100,40 160,70 160,130 100,160 40,130 40,70',
  '100,60 140,80 140,120 100,140 60,120 60,80',
  '100,80 120,90 120,110 100,120 80,110 80,90',
];
const RADAR_AXES = DIMENSION_META.map((m) => ({ x1: 100, y1: 100, x2: m.vertex[0], y2: m.vertex[1] }));

/** V11.0：候选人心态状态标签（对齐后端 InterviewTurnResult.sentiment.state） */
const SENTIMENT_LABEL: Record<string, string> = {
  nervous: '紧张',
  confident: '自信',
  hesitant: '犹豫',
  calm: '沉稳',
};

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

// 报告
const report = ref<VoiceInterviewReportVO | null>(null);
// v11.97：报告 Tab 收窄（面试官剖析并入概要、相关知识点移除）
// v13.47 批次 2：三层五 tab（复盘区：概要→问题分析→回放殿后；备战区：追问预测→发展方向）
// 主线 = 表现 → 归因 → 证据 → 个人预测 → 环境导航
const reportTab = ref<'summary' | 'analysis' | 'predict' | 'insight' | 'dialog'>('summary');
const historyLoading = ref(false);

// v11.88 V2：结束后批量分析进度（前端轮询 analysis 接口驱动进度条）
const analysisState = ref({ active: false, progress: 0 });
const analysisStepText = computed(() => {
  if (analysisState.value.progress < 40) return '正在逐题深度分析…';
  if (analysisState.value.progress < 80) return '正在融合六维评分…';
  if (analysisState.value.progress < 100) return '正在生成复盘报告…';
  return '报告生成完毕';
});

// 答案编辑
const editableAnswer = ref('');
const answerStartTime = ref(0);

// 卡壳自动提示（每题一次）
const autoHintFired = ref(false);
const questionShownAt = ref(0);

// 面试时长计时
const elapsedSec = ref(0);
let timerHandle: ReturnType<typeof setInterval> | null = null;

// v11.96 时长制：全场倒计时（默认 20 分钟，sys_config 可配；归零自动保存并生成报告）
const interviewRemainSec = ref(0);
let globalCountdownHandle: ReturnType<typeof setInterval> | null = null;
/** 归零待收尾标记：正在生成/作答中先补交答案，onEnd 后统一收尾 */
let timeUpPending = false;

// 作答计时（v13.39 改制：不限时、不自动提交——模拟面试允许充分思考，仅展示已用时长；
// 超过软提醒阈值 toast 一次，思考时长由提交时 latencyMs 上报后台统计）
const ANSWER_SOFT_REMIND_SEC = 90;
// 卡壳自动提示阈值（秒，超时未作答自动给一级提示，每题一次）
const AUTO_HINT_STUCK_SEC = 30;
/** 本题已用秒数（正计时） */
const answerElapsed = ref(0);
/** 软提醒是否已发（每题一次，只提示不强制） */
let softRemindFired = false;
let countdownHandle: ReturnType<typeof setInterval> | null = null;

// ==================== 对话气泡 ====================
// v11.88 V2：analysis 实时分析气泡已移除（六维评分/深度分析统一进结束后报告）
interface ChatMessage {
  id: string;
  role: 'question' | 'user' | 'ai';
  content: string;
  questionIdx?: number;
  total?: number;
  source?: string;
  tag?: string;
  /** V11.0：是否正在流式输出（打字机） */
  streaming?: boolean;
  createdAt: number;
}
const chatList = ref<ChatMessage[]>([]);
const chatScroll = ref<HTMLElement | null>(null);

/** 当前流式输出的气泡 id（onDelta 追加 / onEnd 收尾置空） */
let streamingMsgId: string | null = null;

function pushChat(
  role: ChatMessage['role'],
  content: string,
  opts: Partial<Omit<ChatMessage, 'id' | 'role' | 'content' | 'createdAt'>> = {},
) {
  const id = `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
  chatList.value.push({
    id,
    role,
    content,
    createdAt: Date.now(),
    ...opts,
  });
  scrollChatBottom();
  return id;
}

/** V11.0：追加流式增量到 streaming 气泡（无则新建） */
function appendDelta(text: string) {
  if (!streamingMsgId) {
    streamingMsgId = pushChat('ai', '', { streaming: true, tag: '思考中' });
  }
  const msg = chatList.value.find((m) => m.id === streamingMsgId);
  if (msg) {
    msg.content += text;
    scrollChatBottom();
  }
}

/** V11.0：结束流式气泡（data 事件到达后调用；delta 未输出过则静默移除占位） */
function finishStreaming(tag?: string) {
  if (!streamingMsgId) return;
  const msg = chatList.value.find((m) => m.id === streamingMsgId);
  if (msg) {
    msg.streaming = false;
    msg.tag = tag || '评分反馈';
    if (!msg.content.trim()) {
      chatList.value = chatList.value.filter((m) => m.id !== streamingMsgId);
    }
  }
  streamingMsgId = null;
}
function scrollChatBottom() {
  nextTick(() => {
    const el = chatScroll.value;
    if (el) el.scrollTop = el.scrollHeight;
  });
}

function avatarFor(role: ChatMessage['role']) {
  return role === 'user' ? '👤' : '🤖';
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
  serverMode: asrServerMode,
  streamMode: asrStreamMode,
  transcribing: asrTranscribing,
  whenTranscriptionDone,
  start: startAsr,
  stop: stopAsr,
  abort: abortAsr,
  reset: resetAsr,
} = useSpeechRecognition({
  onUnsupported: () => toast.warning('当前浏览器不支持语音识别，将切换为文字输入模式'),
  onError: (err) => {
    if (err === 'not-allowed' || err === 'service-not-allowed') {
      toast.error('麦克风权限被拒绝，请切换文字模式或在浏览器设置中允许');
    } else if (err === 'server-asr-failed') {
      toast.error('语音转写失败，请重试或手动输入');
    }
  },
  // V10.5：最终识别结果增量追加到输入栏（不再覆盖用户手动编辑的内容）
  onFinalChange: (full) => {
    const chunk = full.length >= prevFinalLen.value
      ? full.slice(prevFinalLen.value).replace(/^\n+/, '')
      : '';
    prevFinalLen.value = full.length;
    if (chunk) {
      editableAnswer.value = editableAnswer.value
        ? editableAnswer.value + chunk
        : chunk;
    }
  },
});

/** 已处理过的 finalText 长度（增量追加用；reset 时清零） */
const prevFinalLen = ref(0);
/** 重置 ASR 并同步清零增量游标 */
function resetAsrWithTracker() {
  resetAsr();
  prevFinalLen.value = 0;
}

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

// 输入栏显示值：聆听时把实时中间识别结果拼在已确认文本后面，用户可见"边说边写"
const answerDisplay = computed(() => {
  if (!listening.value || !interimText.value) return editableAnswer.value;
  return editableAnswer.value
    ? editableAnswer.value + interimText.value
    : interimText.value;
});

/** 用户手动编辑：若输入值以当前 interim 结尾则剥离（interim 由识别流继续维护） */
function onAnswerInput(e: Event) {
  let v = (e.target as HTMLTextAreaElement).value;
  if (listening.value && interimText.value && v.endsWith(interimText.value)) {
    v = v.slice(0, v.length - interimText.value.length);
  }
  editableAnswer.value = v;
}

// ==================== 配置表单 ====================
/** 数据库 position varchar(64)，前端统一上限并预留余量 */
const POSITION_MAX_LEN = 64;
// v13.37：岗位候选已改由后台【岗位模板】驱动（GET /portal/interview/jobTemplate/list），
// 原前端硬编码 POSITION_OPTIONS 已删除 —— 岗位配置必须可在后台维护，不能写死在前端。
const DIFFICULTY_OPTIONS = [
  { label: '初级（应届/转行）', value: 'easy' as const },
  { label: '中级（1-3 年）', value: 'medium' as const },
  { label: '高级（3 年以上）', value: 'hard' as const },
];
const QUESTION_COUNT_OPTIONS = [
  { label: '3 题（快速版）', value: 3 },
  { label: '5 题（标准版）', value: 5 },
  { label: '8 题（深度版）', value: 8 },
];

const config = ref<VoiceStartConfig>({
  position: 'Java 后端开发',
  difficulty: 'medium',
});
const questionCount = ref(5);
const muteMode = ref(false);
/** 自动聆听：AI 播报结束后自动开启麦克风，形成"真人对聊"节奏（行业标准） */
const autoListen = ref(true);

// ==================== 真实设备检测（PC/移动端，输入输出设备 + 权限 + 耳机） ====================
const {
  audioInputs,
  audioOutputs,
  micPermission,
  headphoneState,
  micTesting,
  micLevel,
  micPeakLevel,
  refreshDevices,
  requestMicPermission,
  startMicTest,
  stopMicTest,
  playSpeakerTest,
} = useMediaDevices();

/** 用户选中的输入/输出设备（default = 系统默认） */
const selectedInputId = ref('default');
const selectedOutputId = ref('default');

const micStatusLabelMap: Record<string, string> = {
  granted: '✓ 已授权',
  prompt: '未授权（点击测试将弹窗授权）',
  denied: '✗ 已拒绝',
  unknown: '待检测',
};
const micStatusClass = computed(() =>
  micPermission.value === 'granted' ? 'ok' : micPermission.value === 'denied' ? 'error' : 'checking',
);
const micStatusText = computed(() => micStatusLabelMap[micPermission.value] || '待检测');
const speakerStatusText = computed(() =>
  audioOutputs.value.length > 0 ? `✓ 检测到 ${audioOutputs.value.length} 个输出设备` : '未检测到输出设备',
);
const headphoneStatusText = computed(() =>
  headphoneState.value === 'detected' ? '✓ 已检测到耳机'
  : headphoneState.value === 'not-detected' ? '未检测到耳机（建议佩戴）'
  : '无法自动识别，请自行确认',
);
const headphoneStatusClass = computed(() =>
  headphoneState.value === 'detected' ? 'ok' : headphoneState.value === 'not-detected' ? 'checking' : 'checking',
);

const deviceTesting = ref<Record<string, boolean>>({});
const deviceTested = ref<Record<string, boolean>>({});
const speakerTestDevice = ref<string | null>(null);

/** 麦克风测试：真实采集 + 电平条（5 秒），授权后自动刷新设备列表 */
async function testMic() {
  if (deviceTesting.value.mic) return;
  deviceTesting.value.mic = true;
  try {
    const ok = await startMicTest(selectedInputId.value);
    if (!ok) {
      if (micPermission.value === 'denied') {
        toast.error('麦克风权限被拒绝，请在浏览器地址栏权限图标中允许后重试');
      } else {
        toast.error('无法启动麦克风，请检查设备连接');
      }
      return;
    }
    toast.info('开始录音测试，请说一句话…');
    await new Promise((r) => setTimeout(r, 5000));
    const peak = micPeakLevel.value;
    if (peak > 0.02) {
      deviceTested.value.mic = true;
      toast.success(`麦克风正常（最高音量 ${Math.round(peak * 100)}%）`);
    } else {
      toast.warning('未检测到声音输入，请确认麦克风未被占用或选对了设备');
    }
  } finally {
    await stopMicTest();
    deviceTesting.value.mic = false;
  }
}

/** 扬声器测试：生成真实测试音播放，支持路由到指定输出设备（Chrome/Edge setSinkId） */
async function testSpeaker() {
  if (deviceTesting.value.speaker) return;
  deviceTesting.value.speaker = true;
  try {
    const played = await playSpeakerTest(selectedOutputId.value);
    if (played) {
      speakerTestDevice.value = played;
      deviceTested.value.speaker = true;
      toast.info(`正在通过「${played}」播放测试音`);
    } else {
      toast.error('播放失败，请检查扬声器/音量');
    }
  } finally {
    deviceTesting.value.speaker = false;
  }
}

// ==================== v11.90 V2：环境噪声检测（3 秒采样取平均，多次防误判） ====================
const noiseTesting = ref(false);
const noiseTested = ref(false);
const noiseAvgLevel = ref<number | null>(null);
/** 平均电平阈值（≈ -38dBFS：安静房间本底 <0.03，正常说话 >0.2，此处取嘈杂判定线） */
const NOISY_THRESHOLD = 0.12;
const isNoisy = computed(() => noiseAvgLevel.value !== null && noiseAvgLevel.value >= NOISY_THRESHOLD);
const noiseDb = computed(() => {
  if (noiseAvgLevel.value === null) return '';
  return `${Math.round(20 * Math.log10(Math.max(noiseAvgLevel.value, 1e-4)))} dB`;
});
const noiseStatusText = computed(() =>
  noiseTesting.value ? '检测中…'
    : noiseAvgLevel.value === null ? '未检测'
    : isNoisy.value ? '⚠ 较嘈杂' : '✓ 环境安静',
);
const noiseStatusClass = computed(() =>
  noiseTesting.value || noiseAvgLevel.value === null ? 'checking' : isNoisy.value ? 'warn' : 'ok',
);

/** 环境噪声检测：采样 3 秒平均电平，嘈杂时黄色提示（建议换环境/戴耳机） */
async function testNoise() {
  if (noiseTesting.value) return;
  noiseTesting.value = true;
  try {
    const ok = await startMicTest(selectedInputId.value);
    if (!ok) {
      toast.error('无法访问麦克风，请先通过上方麦克风检测授权');
      return;
    }
    const samples: number[] = [];
    for (let i = 0; i < 15; i++) {
      await new Promise((r) => setTimeout(r, 200));
      samples.push(micLevel.value);
    }
    noiseAvgLevel.value = samples.reduce((a, b) => a + b, 0) / samples.length;
    noiseTested.value = true;
    if (isNoisy.value) {
      toast.warning('环境较嘈杂，建议换到安静环境或佩戴耳机');
    } else {
      toast.success('环境安静，适合面试');
    }
  } finally {
    await stopMicTest();
    noiseTesting.value = false;
  }
}

// ==================== 简历库（真实数据：AI 面试题源依赖） ====================
const resumeList = ref<UserResumeVO[]>([]);
const resumeLoading = ref(false);
const selectedResumeId = ref<number | null>(null);
/** 自定义岗位（当预设岗位都不匹配时） */
const useCustomPosition = ref(false);
const customPosition = ref('');

/** v11.90 V2：岗位要求 JD（面试官提问方向与深度贴合岗位要求；后端 jobRequirements）
 *  声明提前到岗位模板逻辑之前 —— 后者在 computed 初始化期即引用本 ref，避免 TDZ。 */
const jobRequirements = ref('');

/** v11.88：岗位下拉选择值（岗位模板直选；__custom__ 展开自定义输入） */
const positionSelectValue = computed<string>({
  get: () => (useCustomPosition.value ? '__custom__' : config.value.position),
  set: (v: string) => {
    if (v === '__custom__') {
      // 切到自定义：清掉模板回填来源标记，JD 交由用户手输
      useCustomPosition.value = true;
      appliedTemplateId.value = null;
      return;
    }
    useCustomPosition.value = false;
    config.value.position = v;
    onSelectJobTemplate(v);
  },
});

// ==================== v13.37：岗位模板（全 portal 岗位配置唯一来源） ====================
/**
 * 岗位下拉数据源 = GET /portal/interview/jobTemplate/list（后台【岗位模板】可配）。
 * 原为前端硬编码 POSITION_OPTIONS；原「岗位字典」portal_interview_position 已并入岗位模板表。
 */
const jobTemplates = ref<JobTemplateOptionVO[]>([]);
const jobTemplatesLoading = ref(false);
/** 已回填的模板 id（用于展示「已按模板回填」与「恢复模板值」） */
const appliedTemplateId = ref<string | number | null>(null);
/** 已回填的模板快照（用于「恢复模板值」） */
const appliedTemplateSnapshot = ref<{ jd: string; difficulty: string; questionCount: number } | null>(null);
/** 用户是否手动改过岗位要求（改过则切岗位不再覆盖） */
const jdEditedByUser = ref(false);

const appliedTemplate = computed(
  () => jobTemplates.value.find((t) => String(t.id) === String(appliedTemplateId.value)) ?? null,
);
/** 已回填且用户又改过 → 提供「恢复模板值」 */
const canRestoreTemplate = computed(
  () => !useCustomPosition.value && !!appliedTemplateSnapshot.value && jdEditedByUser.value && jobRequirements.value !== appliedTemplateSnapshot.value.jd,
);

/** 拉取岗位模板（失败不阻断页面：下拉退化为「仅自定义」） */
async function loadJobTemplates() {
  jobTemplatesLoading.value = true;
  try {
    const res = await getJobTemplates();
    if (res.code === 200 && Array.isArray(res.data)) {
      jobTemplates.value = res.data;
      // 默认选中首个模板，让首屏即为「已按后台配置回填」的状态
      if (!config.value.position && jobTemplates.value.length > 0) {
        const first = jobTemplates.value[0];
        if (first?.name) {
          config.value.position = first.name;
          applyTemplate(first);
        }
      }
    }
  } catch (e) {
    console.warn('[VoiceInterview] 岗位模板加载失败，下拉仅保留自定义输入：', e);
  } finally {
    jobTemplatesLoading.value = false;
  }
}

/** 按模板回填：JD（可选覆盖）+ 难度 + 题量 */
function applyTemplate(t: JobTemplateOptionVO, force = false) {
  appliedTemplateId.value = t.id;
  appliedTemplateSnapshot.value = {
    jd: t.jobDescription ?? '',
    difficulty: t.difficulty ?? config.value.difficulty,
    questionCount: t.questionCount ?? questionCount.value,
  };
  // JD 回填：用户已手改过则不覆盖（以用户输入为准），除非显式「恢复模板值」
  if (force || !jdEditedByUser.value) {
    jobRequirements.value = t.jobDescription ?? '';
    jdEditedByUser.value = false;
  }
  if (t.difficulty === 'easy' || t.difficulty === 'medium' || t.difficulty === 'hard') {
    config.value.difficulty = t.difficulty;
  }
  if (typeof t.questionCount === 'number' && t.questionCount > 0) {
    questionCount.value = t.questionCount;
  }
}

/** 用户切换岗位：回填该模板的 JD / 难度 / 题量（用户已手改 JD 则不覆盖） */
function onSelectJobTemplate(name: string) {
  const t = jobTemplates.value.find((x) => x.name === name);
  if (t) applyTemplate(t);
  else appliedTemplateId.value = null;
}

/** 「恢复模板值」：丢弃用户改动，回到模板配置 */
function restoreTemplate() {
  if (appliedTemplate.value) {
    applyTemplate(appliedTemplate.value, true);
  } else if (appliedTemplateSnapshot.value) {
    jobRequirements.value = appliedTemplateSnapshot.value.jd;
    config.value.difficulty = appliedTemplateSnapshot.value.difficulty as typeof config.value.difficulty;
    questionCount.value = appliedTemplateSnapshot.value.questionCount;
    jdEditedByUser.value = false;
  }
}

/** 清空岗位要求：不选岗位时以此处为准（= 留空，按岗位通用标准出题） */
function clearJobRequirements() {
  jobRequirements.value = '';
  jdEditedByUser.value = false;
}


/** v11.90 V2：高级设置折叠（核心只留岗位+JD+简历，其余收进折叠区保持准备页紧凑） */
const advancedOpen = ref(false);

// ==================== v11.90 V2：开始面试 5 步准备进度条（点击后展示，完成自动进入面试页） ====================
const PREPARE_STEPS = ['加载简历画像', '解析岗位要求', '生成会话上下文', '生成面试题单', '准备面试环境'];
const prepareState = ref({ active: false, step: 0, progress: 0 });
let prepareTimer: ReturnType<typeof setInterval> | null = null;

function stopPrepareProgress() {
  if (prepareTimer) {
    clearInterval(prepareTimer);
    prepareTimer = null;
  }
  prepareState.value.active = false;
}

// ==================== v11.90 V2：结束触发点（连续跳过 3 题 / 5 分钟无响应，均弹确认） ====================
const SKIP_END_THRESHOLD = 3;
const skipStreak = ref(0);
const IDLE_END_MS = 5 * 60 * 1000;
let idleEndHandle: ReturnType<typeof setTimeout> | null = null;

function clearIdleWatch() {
  if (idleEndHandle) {
    clearTimeout(idleEndHandle);
    idleEndHandle = null;
  }
}

/** 5 分钟无响应结束触发点：新题展示时武装；提交/跳过后由新题重新武装；暂停时挂起顺延 */
function armIdleWatch() {
  clearIdleWatch();
  if (phase.value !== 'interview') return;
  idleEndHandle = setTimeout(async () => {
    if (phase.value !== 'interview') return;
    if (submitting.value || timerPaused.value) {
      armIdleWatch(); // 提交中/暂停：顺延一个周期
      return;
    }
    const end = await confirmModal.confirm(
      '已 5 分钟未作答。是否结束面试并生成报告？',
      { title: '长时间未作答', confirmText: '结束并生成报告', cancelText: '继续面试' },
    );
    if (end && phase.value === 'interview') {
      await handleFinish();
    } else if (phase.value === 'interview') {
      armIdleWatch(); // 继续面试：重新武装
    }
  }, IDLE_END_MS);
}

// ==================== v11.90 V2：报告等级徽章 + 三段式信息（candidate/jobInfo） ====================
const scoreLevel = computed(() => {
  const s = report.value?.totalScore ?? 0;
  if (s >= 80) return { label: '优秀', cls: 'excellent' };
  if (s >= 70) return { label: '良好', cls: 'good' };
  if (s >= 60) return { label: '合格', cls: 'pass' };
  return { label: '待提升', cls: 'weak' };
});
const candidateInfo = computed(() => report.value?.candidate ?? null);
const jobInfoView = computed(() => report.value?.jobInfo ?? null);
/** 技能字符串拆分为标签（逗号/顿号/斜杠/分号分隔）；v11.97：原始 JSON 先解析再拆（兜底存量数据） */
const candidateSkills = computed(() => {
  let raw = candidateInfo.value?.skills;
  if (!raw) return [];
  raw = raw.trim();
  if (raw.startsWith('{') || raw.startsWith('[')) {
    try {
      const parsed = JSON.parse(raw);
      const parts: string[] = Array.isArray(parsed)
        ? parsed.map((p) => (typeof p === 'string' ? p : `${p?.name ?? ''}${p?.level ? '·' + p.level : ''}`))
        : Object.entries(parsed as Record<string, { level?: string } | string>).map(
            ([name, v]) => (typeof v === 'string' ? name : `${name}${v?.level ? '·' + v.level : ''}`),
          );
      return parts.filter(Boolean).slice(0, 12);
    } catch {
      /* 解析失败走原样拆分 */
    }
  }
  return raw.split(/[,，、/；;]+/).map((s) => s.trim()).filter(Boolean).slice(0, 12);
});

async function loadResumeList() {
  if (!userStore.isAuthenticated) return;
  resumeLoading.value = true;
  try {
    const res = await getMyResumeList({ pageNum: 1, pageSize: 50 });
    if (res.code === 200 && res.data) {
      resumeList.value = res.data.list || [];
      // URL 指定简历优先（简历优化页"去面试"闭环入口），否则默认选中第一份，并带入其求职意向岗位
      const wantedId = Number(route.query.resumeId);
      const target = wantedId
        ? resumeList.value.find((r) => Number(r.id) === wantedId)
        : resumeList.value.find((r) => r.id);
      if (target?.id) {
        selectResume(target);
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
    // 简历求职意向优先作为面试岗位（个性化出题），超长截断对齐数据库 varchar(64)
    const safePos = intentPos.slice(0, POSITION_MAX_LEN);
    // v13.37：岗位候选来自后台岗位模板；命中则连带回填该模板的 JD/难度/题量
    const matched = jobTemplates.value.find((o) => o.name === safePos);
    if (!matched) {
      useCustomPosition.value = true;
      customPosition.value = safePos;
      appliedTemplateId.value = null;
    } else {
      useCustomPosition.value = false;
      config.value.position = safePos;
      applyTemplate(matched);
    }
  }
}

// ==================== 上传简历：AI 解析 → 回填默认简历 ====================
/** v11.89：简历收起/展开面板（默认收起保持紧凑；当前选择常显于头部按钮，选中联动求职意向到岗位） */
const resumePanelOpen = ref(false);
const selectedResume = computed(() =>
  resumeList.value.find((x) => Number(x.id) === selectedResumeId.value) ?? null,
);
function toggleResumePanel() {
  resumePanelOpen.value = !resumePanelOpen.value;
}
/** 不选简历：清空选择，按岗位通用题库出题 */
function clearResumeSelection() {
  selectedResumeId.value = null;
}
/** 选中简历并收起面板 */
function chooseResume(r: UserResumeVO) {
  selectResume(r);
  resumePanelOpen.value = false;
}

const resumeUploading = ref(false);
const resumeUploadInput = ref<HTMLInputElement | null>(null);

// v13.38：简历解析「预览 → 确认」两步式（与编辑页一致）
// 解析不落库（避免失败留脏数据），用户左右对照校对后确认才落库
const parsePreviewVisible = ref(false);
const parsePreviewSaving = ref(false);
const parsePreviewData = ref<ResumePreviewVO>({ previewToken: '' });
/** 解析进行中的文案（轮询任务 onTick 更新） */
const resumeParsingMsg = ref('');

function triggerResumeUpload() {
  resumeUploadInput.value?.click();
}

async function handleResumeUpload(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!userStore.isAuthenticated) {
    toast.warning('请先登录后再上传简历');
    return;
  }
  const okExt = /\.(pdf|docx?|txt)$/i.test(file.name);
  if (!okExt) {
    toast.error('仅支持 PDF / Word / TXT 格式简历');
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.error('简历文件不能超过 10MB');
    return;
  }
  resumeUploading.value = true;
  resumeParsingMsg.value = '正在解析附件…';
  try {
    // v13.38：同步规则解析（毫秒级、离线可用、不调 LLM、不落库），随即弹出校对弹窗
    const { data: resp, success } = await run(() => previewResumeParse(file), {
      errorToast: '附件解析失败',
    });
    if (!success || !resp?.data?.previewToken) return;
    parsePreviewData.value = resp.data;
    parsePreviewVisible.value = true;
  } finally {
    resumeUploading.value = false;
    resumeParsingMsg.value = '';
  }
}

/** 放弃解析校对：不落库，无残留 */
function onParsePreviewCancel() {
  parsePreviewVisible.value = false;
  parsePreviewData.value = { previewToken: '' };
}

/** 确认解析：落库为新简历，并自动设为本次面试简历 */
async function onParsePreviewConfirm(payload: ResumePreviewVO) {
  parsePreviewSaving.value = true;
  try {
    const { data: resp, success } = await run(() => confirmResumeParse(payload), {
      errorToast: '保存失败',
    });
    if (!success) return;
    const newId = resp?.data?.resumeId;
    parsePreviewVisible.value = false;
    parsePreviewData.value = { previewToken: '' };
    await loadResumeList();
    const target = resumeList.value.find((r) => Number(r.id) === Number(newId));
    if (target) {
      selectResume(target);
      toast.success('简历解析完成，已设为本次面试简历');
    } else if (newId) {
      selectedResumeId.value = Number(newId);
      toast.success('简历解析完成');
    }
  } finally {
    parsePreviewSaving.value = false;
  }
}

/** 实际生效的面试岗位（统一截断到数据库 varchar(64) 上限） */
const effectivePosition = computed(() => {
  const raw = useCustomPosition.value
    ? customPosition.value.trim() || config.value.position
    : config.value.position;
  return raw.slice(0, POSITION_MAX_LEN);
});

onMounted(() => {
  loadResumeList();
  checkActiveInterview();
});

// ==================== v11.91 断点续接：意外关闭后恢复进行中面试 ====================
const activeInterview = ref<ActiveVoiceInterviewVO | null>(null);
const resuming = ref(false);

/** 进入准备页时静默探测进行中会话（有则展示恢复横幅） */
async function checkActiveInterview() {
  try {
    const { data: resp, success } = await run(() => getActiveVoiceInterview(), { errorToast: '' });
    if (success && resp?.data?.interviewId) {
      activeInterview.value = resp.data;
    } else {
      activeInterview.value = null;
    }
  } catch {
    /* 静默：探测失败不影响新建面试 */
  }
}

// 每次回到准备页重新探测（面试结束"再来一场"时刷新横幅状态）
watch(phase, (p) => {
  if (p === 'setup') checkActiveInterview();
});

/** 继续面试：拉取恢复快照（历史问答 + 当前题），重建面试页从中断点继续 */
async function handleResume() {
  const target = activeInterview.value;
  if (!target?.interviewId || resuming.value) return;
  resuming.value = true;
  try {
    const { data: resp, success } = await run(() => resumeVoiceInterview(target.interviewId!), {
      errorToast: '恢复面试失败',
    });
    if (success && resp?.data) {
      const vo = resp.data;
      const elapsed = target.elapsedSec ?? 0;
      activeInterview.value = null;
      interview.value = vo;
      currentQaId.value = vo.currentQa?.id ?? null;
      phase.value = 'interview';
      chatList.value = [];
      // 重建历史对话（问答主链路；点评细节留在报告中查看）
      (vo.qaList ?? []).forEach((q) => {
        if (q.question) {
          pushChat('question', q.question, {
            questionIdx: q.questionIdx ?? 0,
            total: vo.totalQa ?? 0,
            source: '智能生成',
            tag: '断点前',
          });
        }
        if (q.userAnswer) {
          pushChat('user', q.userAnswer);
        }
      });
      // 计时从中断前继续
      elapsedSec.value = elapsed;
      timerPaused.value = false;
      skipStreak.value = 0;
      startElapsedTimer();
      startGlobalCountdown(); // v11.96 时长制：剩余时长倒计时（扣除中断前已用时）
      // 当前待答题重新展示（重置倒计时 + 武装 5 分钟无响应触发点）
      if (vo.currentQa) {
        presentQuestion(
          vo.currentQa.question,
          vo.currentQa.speakText || vo.currentQa.question,
          vo.currentQa.questionIdx,
          '断点续接',
        );
      }
      toast.success('已恢复到上次中断的面试');
    }
  } finally {
    resuming.value = false;
  }
}

/** 放弃继续：收口旧会话（触发异步批量分析，报告保留在历史记录） */
async function handleAbandonActive() {
  const target = activeInterview.value;
  if (!target?.interviewId) return;
  const confirmed = await confirmModal.confirm(
    '放弃后将自动生成面试报告，稍后可在历史记录中查看。确定放弃这场未完成的面试吗？',
    { title: '放弃未完成面试', confirmText: '放弃', cancelText: '取消' },
  );
  if (!confirmed) return;
  const { success } = await run(() => finishVoiceInterview(target.interviewId!), { errorToast: '收口失败' });
  if (success) {
    activeInterview.value = null;
    toast.success('已收口，面试报告稍后可在历史记录查看');
  }
}

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

// ==================== v11.96 时长制：全场倒计时 ====================
function startGlobalCountdown() {
  stopGlobalCountdown();
  const durationMin = interview.value?.durationMinutes ?? 20;
  interviewRemainSec.value = Math.max(0, durationMin * 60 - Math.max(0, elapsedSec.value));
  timeUpPending = false;
  globalCountdownHandle = setInterval(() => {
    if (timerPaused.value) return;
    if (interviewRemainSec.value > 0) {
      interviewRemainSec.value--;
      if (interviewRemainSec.value === 0) onGlobalTimeUp();
    }
  }, 1000);
}

function stopGlobalCountdown() {
  if (globalCountdownHandle) {
    clearInterval(globalCountdownHandle);
    globalCountdownHandle = null;
  }
}

function formatRemain(sec: number) {
  const m = Math.floor(sec / 60).toString().padStart(2, '0');
  const s = (sec % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

/** 倒计时归零：有未提交作答先自动提交（onEnd 后收尾），否则直接收口生成报告 */
function onGlobalTimeUp() {
  stopGlobalCountdown();
  if (phase.value !== 'interview') return;
  if (submitting.value) {
    timeUpPending = true; // 面试官话术生成中：等 onEnd 后统一收尾
    return;
  }
  const draft = (editableAnswer.value || finalText.value || '').trim();
  if (draft) {
    timeUpPending = true;
    toast.warning('本场时间已到，正在提交最后的作答并生成报告');
    handleSubmitAnswer();
    return;
  }
  toast.warning('本场面试时间已到，正在生成报告');
  handleFinish();
}

function startCountdown() {
  stopCountdown();
  answerElapsed.value = 0;
  softRemindFired = false;
  countdownHandle = setInterval(() => {
    if (timerPaused.value) return;
    answerElapsed.value++;
    // 软提醒（每题一次）：不自动提交，只做友好提示
    if (!softRemindFired && answerElapsed.value >= ANSWER_SOFT_REMIND_SEC) {
      softRemindFired = true;
      toast.info('已作答 90 秒——不用急，想好再提交也完全可以');
    }
    // 卡壳自动提示：超过阈值仍未开始作答（无文字、无语音），自动给一级提示
    if (
      !autoHintFired.value &&
      !submitting.value &&
      !editableAnswer.value &&
      !interimText.value &&
      questionShownAt.value > 0 &&
      Date.now() - questionShownAt.value > AUTO_HINT_STUCK_SEC * 1000
    ) {
      autoHintFired.value = true;
      toast.info('似乎卡壳了？已为你送上第一级提示');
      handleHint();
    }
  }, 1000);
}
function stopCountdown() {
  if (countdownHandle) {
    clearInterval(countdownHandle);
    countdownHandle = null;
  }
}

// ==================== 题目进度时间线（v11.96 时长制：题数软参考，动态扩展） ====================
const progressItems = computed(() => {
  if (!interview.value) return [];
  // 时长制下题目无上限（问到时间结束为止），时间线按实际题数动态扩展
  const total = Math.max(interview.value.totalQa || 0, (interview.value.qaList ?? []).length);
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

// ==================== 面试背景（v11.88 V2：右栏卡片，替代实时评分雷达） ====================
const DIFFICULTY_LABEL: Record<string, string> = {
  easy: '初级（应届/转行）', medium: '中级（1-3 年）', hard: '高级（3 年以上）',
};
const interviewBackground = computed<{ label: string; value: string }[]>(() => {
  const it = interview.value;
  if (!it) return [];
  const items = [
    { label: '目标岗位', value: it.position || '未设置' },
    { label: '题目难度', value: DIFFICULTY_LABEL[it.difficulty ?? ''] || '中级' },
  ];
  const resume = selectedResume.value;
  items.push({
    label: '简历锚定',
    value: resume ? (resume.name || resume.title || `简历 #${resume.id}`) : '未选择（按岗位通用题库出题）',
  });
  return items;
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

/** v11.x 闭环：从弱项文本提取练习搜索关键词（去疑问修饰，截取核心词） */
function practiceKeyword(text: string): string {
  const cleaned = text
    .replace(/请(谈谈|说说|讲讲|描述|解释|说明)?/g, '')
    .replace(/你(对|的|觉得|认为)?/g, '')
    .replace(/(理解|看法|认识|了解|掌握)(如何|怎么样)?/g, '')
    .replace(/[?？。！，、的了吗呢吧]/g, '')
    .trim();
  const kw = (cleaned || text.replace(/[?？。！，、]/g, '')).trim();
  return kw.slice(0, 12);
}

/** v11.x 闭环：弱项 → 去练习（跳转选择题练习，带关键词过滤） */
function gotoPractice(text: string) {
  const kw = practiceKeyword(text);
  router.push(kw ? `/learn/practice/choice?keyword=${encodeURIComponent(kw)}` : '/learn/practice/choice');
}
// ==================== v11.97：整场 LLM 复盘字段（新字段优先，旧报告回退旧字段） ====================
/** 整场总评：overallComment 优先，旧报告回退 summary */
const reportOverall = computed(() => report.value?.overallComment || report.value?.summary || '');
/** 岗位匹配度：jobMatch 优先，旧报告回退 jobInfo.matchRate */
const jobMatchRate = computed(() => {
  const jm = report.value?.jobMatch;
  if (jm?.rate != null) return jm.rate;
  const legacy = Number(report.value?.jobInfo?.matchRate);
  return Number.isFinite(legacy) ? legacy : null;
});
/** 匹配依据（仅新报告有） */
const jobMatchReason = computed(() => report.value?.jobMatch?.reason || '');

// ============================================================================
// v13.47 批次 2：「🔮 追问预测」tab 数据
// ============================================================================
/** 定级徽章文案（junior/mid/senior → 中文；非法值返回空串表示不展示） */
const levelBadge = computed(() => {
  const lv = report.value?.levelEstimate;
  if (lv === 'junior') return { key: 'junior', text: '初级', tip: '基础需夯实' };
  if (lv === 'mid') return { key: 'mid', text: '中级', tip: '框架完整，深度待补' };
  if (lv === 'senior') return { key: 'senior', text: '高级', tip: '具备体系化思维' };
  return null;
});

/** 追问预测全量（缺失/空 → tab 隐藏） */
const predictedAll = computed(() => report.value?.predictedQuestions ?? []);
/** tab 是否展示：有预测数据才展示（对齐「字段为空按缺失隐藏」惯例） */
const showPredictTab = computed(() => predictedAll.value.length > 0);

/** 分组 A「本次已问」：复盘视角 —— 问题 + 你的得分 + 标准答案要点对照 */
const predictedAsked = computed(() =>
  predictedAll.value.filter((p) => p.askedThisRound === true),
);
/** 分组 B「未被问到」：预警视角（核心价值）—— 下次真面试的押题清单 */
const predictedUnasked = computed(() =>
  predictedAll.value.filter((p) => p.askedThisRound !== true),
);

/**
 * v13.49 批次 2：概要 tab「归档区」折叠状态。
 *
 * 归档区放**资料类**内容（自我介绍评分 / 心态趋势 / 可疑信号 / 流畅度）——
 * 它们是佐证材料而非结论，默认折叠（对齐 V1.2 §6.2「归档区（折叠）」与
 * 「资料降权，现状 11 板块无主次」的病症）。
 */
const archiveOpen = ref(false);

// ============================================================================
// v13.50 批次 3：「🧭 发展方向」懒生成
// ============================================================================
/** 生成中标记（防重复点击；后端另有分布式锁兜底） */
const insightLoading = ref(false);
/** 本次会话内的错误提示（生成失败时展示，不阻塞报告其余部分） */
const insightError = ref('');

/** 已生成的发展方向数据（来自 report.industryInsight，缺失则显示占位卡） */
const industryInsight = computed(() => report.value?.industryInsight ?? null);

/** 生成时间展示文案「生成于 X 月 X 日」 */
const insightGeneratedAtText = computed(() => {
  const ts = industryInsight.value?.generatedAt;
  if (!ts) return '';
  const d = new Date(ts);
  if (Number.isNaN(d.getTime())) return '';
  return `生成于 ${d.getMonth() + 1} 月 ${d.getDate()} 日`;
});

/** 是否已超过 7 天（提示可刷新；不强制过期 —— V1.2 §9 Q2 裁决） */
const insightStale = computed(() => {
  const ts = industryInsight.value?.generatedAt;
  if (!ts) return false;
  return Date.now() - ts > 7 * 24 * 60 * 60 * 1000;
});

/**
 * 生成/刷新发展方向分析。
 *
 * **懒生成**：只在用户点按钮时调用（不在 onMounted 自动触发）—— 避免「用户从不看
 * 这个 tab 却为它付费」（V1.2 §1 原则 3「成本按需发生」）。
 */
/** 成熟度 → 样式类：成熟期=绿、上升期=蓝、早期=灰（对齐"越成熟越可信"的直觉） */
function maturityClass(maturity: string) {
  if (maturity.includes('成熟')) return 'mature';
  if (maturity.includes('上升')) return 'rising';
  return 'early';
}

async function handleGenerateInsight(force = false) {
  if (insightLoading.value) return;
  const id = interview.value?.id ?? report.value?.interviewId;
  if (!id) {
    toast.error('面试信息缺失，无法生成');
    return;
  }
  insightLoading.value = true;
  insightError.value = '';
  try {
    const res = await generateVoiceIndustryInsight(id, force);
    if (res?.data) {
      report.value = res.data;
      toast.success(force ? '发展方向已刷新' : '发展方向分析已生成');
    } else {
      insightError.value = '生成失败，请稍后重试';
    }
  } catch (e) {
    insightError.value = e instanceof Error ? e.message : '生成失败，请稍后重试';
    toast.error(insightError.value);
  } finally {
    insightLoading.value = false;
  }
}

// ============================================================================
// v13.49 批次 2：问题分析「按维度聚类」
// ============================================================================
/**
 * 最低维度（雷达六维中得分最低者）。
 *
 * 口径说明：后端**不产出逐题维度分**（`questionReviews` 只有总分与点评），
 * 因此这里不做「把某题归到某维度」的臆测映射 —— 那是无依据的编造。
 * 实际做法：找出最低维度作为**归因锚点**，再把**低分题置顶**供优先复盘。
 */
const weakestDimension = computed(() => {
  // 注意：reportScores 是 **number[]**（与 DIMENSION_META 同序），不是按 key 的 Record。
  const dims = reportScores.value;
  let lowest: { key: string; label: string; value: number } | null = null;
  DIMENSION_META.forEach((m, i) => {
    const v = dims[i];
    if (v == null) return;
    if (lowest == null || v < lowest.value) lowest = { key: m.key, label: m.label, value: v };
  });
  return lowest;
});

/** 低分题优先排序（分数升序；无分排最后，保持原相对顺序） */
const lowScoreFirst = computed(() => {
  const list = [...(report.value?.questionReviews ?? [])];
  return list.sort((a, b) => {
    const av = a.score ?? 999;
    const bv = b.score ?? 999;
    return av - bv;
  });
});

/** 低分题筛选阈值（与逐题卡的"待加强"判定一致：< 80） */
const LOW_SCORE_THRESHOLD = 80;
/** 低分题数量（用于聚类区标题） */
const lowScoreCount = computed(
  () => lowScoreFirst.value.filter((q) => (q.score ?? 0) < LOW_SCORE_THRESHOLD).length,
);
/** 亮点/薄弱点统一结构 {title, detail}：结构化视图优先，旧报告回退字符串数组 */
const highlightItems = computed<{ title: string; detail: string }[]>(() => {
  const views = report.value?.highlightViews;
  if (views?.length) {
    return views.map((v) => ({ title: v.title || '', detail: v.detail || '' }));
  }
  return (report.value?.highlights ?? []).map((p) => ({
    title: pointText(p),
    detail: pointQuote(p),
  }));
});
const weakPointItems = computed<{ title: string; detail: string }[]>(() => {
  const views = report.value?.weakPointViews;
  if (views?.length) {
    return views.map((v) => ({ title: v.title || '', detail: v.detail || '' }));
  }
  return (report.value?.weakPoints ?? []).map((p) => ({
    title: pointText(p),
    detail: pointQuote(p),
  }));
});
/** v11.97：逐题"你的回答"折叠展开（默认 2 行，点击展开全文） */
const expandedReviews = ref<Set<number>>(new Set());
function toggleReviewExpand(idx: number) {
  const next = new Set(expandedReviews.value);
  if (next.has(idx)) next.delete(idx);
  else next.add(idx);
  expandedReviews.value = next;
}
/** v11.97：重新生成报告——确认后重置进度复用现有轮询链路（skipConfirm：历史页入口已确认过） */
async function handleRegenerateReport(skipConfirm = false) {
  const id = interview.value?.id;
  if (!id || analysisState.value.active) return;
  const ok = skipConfirm || await confirmModal.confirm(
    '将重新进行 AI 深度分析并覆盖当前报告（逐题评分 + 整场复盘），约需 1 分钟。确定重新生成吗？',
    { title: '重新生成报告', confirmText: '重新生成', cancelText: '取消' },
  );
  if (!ok) return;
  const { success } = await run(() => regenerateVoiceReport(id), { errorToast: '重新生成失败' });
  if (success) {
    report.value = null;
    expandedReviews.value = new Set();
    analysisState.value = { active: true, progress: 5 };
    startAnalysisPolling();
  }
}
function scoreClass(score: number) {
  return score < 60 ? 'low' : score < 80 ? 'medium' : 'high';
}
function scoreColor(score: number) {
  return score < 60 ? 'var(--error)' : score < 80 ? 'var(--warning)' : 'var(--success)';
}

const suggestionItems = computed(() => {
  // v11.x：优先展示后端针对性改进建议（薄弱点/自我介绍不足），回退旧 suggestion 拆分
  const improve = report.value?.improvementSuggestions ?? [];
  if (improve.length > 0) return improve;
  const s = report.value?.suggestion || '';
  if (!s) return [];
  const lines = s.split(/\n+/).map((l) => l.trim()).filter(Boolean);
  if (lines.length > 1) return lines;
  const numbered = s.split(/\s*\d+[.、)]\s+/).map((l) => l.trim()).filter(Boolean);
  return numbered.length > 1 ? numbered : [s];
});

/** v11.x：自我介绍评分展示（4维度分 + 总评，无 introScore 时为 null 隐藏） */
const introScoreView = computed(() => {
  const intro = report.value?.introScore;
  if (!intro || intro.total == null) return null;
  const labels: Record<string, string> = {
    structure: '逻辑结构',
    awareness: '自我认知',
    matching: '岗位匹配',
    fluency: '表达流畅',
  };
  const dims = Object.entries(intro.dimensions ?? {}).map(([key, val]) => ({
    key,
    label: labels[key] || key,
    value: val ?? 0,
  }));
  return { total: intro.total, comment: intro.comment || '', dims };
});

// ==================== 历史报告：?id=xxx ====================
async function loadHistoryReport(idStr: string) {
  if (!idStr) return;
  historyLoading.value = true;
  try {
    const { data: resp, success } = await run(() => getVoiceInterviewDetail(idStr), {
      errorToast: '加载面试详情失败',
    });
    if (!success || !resp?.data) return;
    const vo = resp.data;
    interview.value = vo;

    // v11.96：进行中——回准备页走断点续接横幅（历史点错/旧标签页场景）
    if (vo.status === 'in_progress') {
      phase.value = 'setup';
      checkActiveInterview();
      toast.info('该面试尚未结束，可从页面横幅继续');
      return;
    }

    // v11.96：已结束但报告仍在生成（刷新/离开后）——进报告页显示进度并轮询，完成后展示完整报告
    if ((vo.analysisStatus ?? 0) < 2) {
      phase.value = 'report';
      reportTab.value = 'summary';
      analysisState.value = { active: true, progress: Math.max(5, vo.analysisProgress ?? 0) };
      startAnalysisPolling();
      return;
    }

    // v11.96：报告已生成——finish 幂等拉取库中完整报告（真实数据，不再前端拼凑伪报告）
    phase.value = 'report';
    reportTab.value = 'summary';
    await loadFullReport();

    // v11.97：历史页「重新生成报告」入口携带 regenerate=1，报告加载完成后自动触发（入口处已确认过，跳过二次弹窗）
    if (route.query.regenerate === '1') {
      router.replace({ query: { ...route.query, regenerate: undefined } });
      handleRegenerateReport(true);
    }
  } finally {
    historyLoading.value = false;
  }
}

onMounted(() => {
  const id = String(route.query.id ?? '').trim();
  if (id) loadHistoryReport(id);
  // v13.37：岗位下拉改由后台【岗位模板】驱动（默认选中首个并回填 JD/难度/题量）
  loadJobTemplates();
  // 页签切走/最小化：立即停止聆听与播报，及时释放麦克风等硬件占用
  document.addEventListener('visibilitychange', releaseOnHidden);
  // v11.89：浏览器关闭/刷新：无条件释放麦克风/播放器；面试进行中先触发浏览器离开确认
  window.addEventListener('beforeunload', handleBeforeUnload);
});

onUnmounted(() => {
  document.removeEventListener('visibilitychange', releaseOnHidden);
  window.removeEventListener('beforeunload', handleBeforeUnload);
  // 卸载即丢弃：立即停麦克风/断 WS/停播报与计时，不等转写收尾（用户已离开页面，无需保留结果）
  releaseMediaResources();
  // v11.88 V2：停止报告分析进度轮询（服务端继续生成，可从历史记录查看）
  stopAnalysisPolling();
  // v11.90 V2：清理结束触发点计时与准备进度条
  clearIdleWatch();
  stopPrepareProgress();
});

/** v11.89：统一释放麦克风/播放器/计时器——结束面试、临时退出、关闭标签页均有始有终 */
function releaseMediaResources() {
  if (speaking.value) ttsCancel();
  if (listening.value) stopAsr();
  abortAsr();
  stopCountdown();
  stopElapsedTimer();
  stopGlobalCountdown(); // v11.96 时长制：全场倒计时统一释放
  if (questionTtsTimer) {
    clearTimeout(questionTtsTimer);
    questionTtsTimer = null;
  }
}

/** v11.89：浏览器关闭/刷新标签页——先释放硬件连接；面试进行中触发浏览器离开确认 */
function handleBeforeUnload(e: BeforeUnloadEvent) {
  releaseMediaResources();
  if (phase.value === 'interview') {
    e.preventDefault();
    e.returnValue = '';
  }
}

// v11.89：面试进行中路由跳转（返回/切页）前确认；确认离开后统一释放麦克风与播放器
onBeforeRouteLeave(async () => {
  if (phase.value !== 'interview') return true;
  const ok = await confirmModal.confirm(
    '面试正在进行中，离开将中断本场面试（已答题目保留，可稍后从「我的面试记录」查看）。确定要临时退出吗？',
    { danger: true, title: '临时退出面试', confirmText: '确定离开', cancelText: '继续面试' },
  );
  if (!ok) return false;
  releaseMediaResources();
  return true;
});

/** 页签不可见时释放麦克风/连接资源；回到页面不自动恢复，由用户重新点麦克风 */
function releaseOnHidden() {
  if (document.visibilityState === 'hidden') {
    if (listening.value) stopAsr();
    if (speaking.value) ttsCancel();
  }
}

// ==================== 开始面试 ====================
/** 题目 TTS 延迟播报定时器（卸载时清理，避免离开页面后仍触发播报） */
let questionTtsTimer: ReturnType<typeof setTimeout> | null = null;

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
  resetAsrWithTracker();
  startCountdown();
  // v11.90 V2：5 分钟无响应触发点——新题展示时武装
  armIdleWatch();
  // 卡壳自动提示：每题重置标记与计时起点
  autoHintFired.value = false;
  questionShownAt.value = Date.now();
  if (!muteMode.value && ttsSupported.value && speakText) {
    if (questionTtsTimer) clearTimeout(questionTtsTimer);
    questionTtsTimer = setTimeout(() => {
      questionTtsTimer = null;
      ttsSpeak(speakText);
    }, 200);
  }
}

/**
 * V3：SSE onEnd 后重置新题答题状态（不推气泡、不 TTS——
 * 面试官话术已通过 delta 流式渲染并分句播报，这里只重置作答现场）
 */
function resetForNewQuestion(question?: string) {
  if (question) {
    currentQuestion.value = question;
    currentSpeakText.value = '';
  }
  editableAnswer.value = '';
  resetAsrWithTracker();
  answerStartTime.value = 0;
  startCountdown();
  // v11.90 V2：5 分钟无响应触发点——新题展示时武装
  armIdleWatch();
  // 卡壳自动提示：每题重置标记与计时起点
  autoHintFired.value = false;
  questionShownAt.value = Date.now();
}

// ==================== V3 分句 TTS（delta 流式增量 → 逐句入队播报） ====================
// useSpeechSynthesis 自带顺序播报队列：句子入队后逐句 onend 串联播放，
// 队列播完 speaking 才置 false，watch(speaking) 届时自动重新开麦（autoListen）
let ttsSentenceBuffer = '';

/** 单句入队播报（静音/不支持 TTS 时跳过） */
function speakSentence(sentence: string) {
  const s = sentence.trim();
  if (!s || muteMode.value || !ttsSupported.value) return;
  ttsSpeak(s);
}

/** delta 增量喂入缓冲，遇句末标点（。！？；或换行）立即把完整句入队播报 */
function feedDeltaToTts(text: string) {
  ttsSentenceBuffer += text;
  const m = ttsSentenceBuffer.match(/^[\s\S]*[。！？；\n]/);
  if (m) {
    ttsSentenceBuffer = ttsSentenceBuffer.slice(m[0].length);
    speakSentence(m[0]);
  }
}

/** 冲刷缓冲：把没有句末标点的尾巴也入队（onEnd 时调用） */
function flushTtsBuffer() {
  const rest = ttsSentenceBuffer;
  ttsSentenceBuffer = '';
  speakSentence(rest);
}

/** 丢弃缓冲（onError/onAborted/新一轮提交前调用，避免残留串场） */
function clearTtsBuffer() {
  ttsSentenceBuffer = '';
}

async function handleStart() {
  // 提交前校验：岗位必填且不超数据库 varchar(64)
  const pos = effectivePosition.value.trim();
  if (!pos) {
    toast.warning('请选择或输入面试岗位');
    return;
  }
  if (pos.length > POSITION_MAX_LEN) {
    toast.warning(`岗位名称不能超过 ${POSITION_MAX_LEN} 个字符`);
    return;
  }
  // v12.0 统一会员前置校验（free 档免费额度 + 开通引导；后端 @VipOnly 兜底）
  try {
    const vipRes = await getVipStatus();
    if (!vipRes.data?.isVip) {
      const left = benefitLeft(vipRes.data, 'interview_unlimited') ?? 0;
      if (left > 0) {
        toast.info(`免费体验剩余 ${left} 次，开通会员可不限次开练`);
      } else {
        const goBuy = await confirmModal.confirm(
          '免费体验次数已用完，开通会员可不限次语音开练',
          { title: '墨韵会员', confirmText: '去开通', cancelText: '暂不' },
        );
        if (goBuy) {
          router.push('/membership');
          return;
        }
      }
    }
  } catch {
    /* 会员状态查询失败不阻断面试（后端 @VipOnly 兜底） */
  }
  loading.value = true;
  // v11.90 V2：5 步准备进度条（简历画像→岗位要求→会话上下文→题单→环境；请求返回即 100%）
  prepareState.value = { active: true, step: 0, progress: 6 };
  prepareTimer = setInterval(() => {
    const s = prepareState.value;
    if (s.progress < 90) {
      s.progress = Math.min(90, s.progress + 5 + Math.random() * 10);
      s.step = Math.min(PREPARE_STEPS.length - 1, Math.floor((s.progress / 100) * PREPARE_STEPS.length));
    }
  }, 600);
  try {
    const payload: VoiceStartConfig = {
      position: pos,
      // v11.90 V2：岗位要求 JD（面试官提问贴合岗位要求）
      jobRequirements: jobRequirements.value.trim() || undefined,
      resumeId: selectedResumeId.value ?? undefined,
      difficulty: config.value.difficulty,
      questionCount: questionCount.value,
    };
    const { data: vo, success } = await run(() => startVoiceInterview(payload), {
      errorToast: '开始失败',
    });
    if (success && vo?.data) {
      prepareState.value.progress = 100;
      prepareState.value.step = PREPARE_STEPS.length;
      await new Promise((r) => setTimeout(r, 450)); // 让用户看到 100% 完成
      stopPrepareProgress();
      interview.value = vo.data;
      currentQaId.value = vo.data.currentQa?.id ?? null;
      phase.value = 'interview';
      chatList.value = [];
      elapsedSec.value = 0;
      timerPaused.value = false;
      skipStreak.value = 0;
      // v11.91：新面试已开始（后端已收口遗留会话），同步清掉恢复横幅
      activeInterview.value = null;
      startElapsedTimer();
      startGlobalCountdown(); // v11.96 时长制：全场倒计时（归零自动保存+生成报告）

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
    stopPrepareProgress();
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
    if (!editableAnswer.value) resetAsrWithTracker();
    answerStartTime.value = Date.now();
    startAsr();
  }
}

function clearAnswer() {
  editableAnswer.value = '';
  resetAsrWithTracker();
}

/** 静音切换：开启时立即停止当前播报，关闭时恢复后续播报 */
function toggleMute() {
  muteMode.value = !muteMode.value;
  if (muteMode.value) ttsCancel();
}

// ==================== 提交答案 / 跳过（SSE：delta/end/error） ====================
/**
 * V3：作答/跳过共用的 SSE 回调——
 * delta 打字机渲染 + 分句 TTS 入队播报；end 按负载推进下一题或收场。
 * @param isSkip 跳过路径不清零 skipStreak（作答成功才清零，保留连续跳过结束逻辑）
 */
function buildAnswerCallbacks(isSkip: boolean) {
  return {
    // 流式增量：打字机气泡 + 分句 TTS（句末标点即入队，边生成边播报）
    onDelta: (text: string) => {
      if (!text) return;
      appendDelta(text);
      feedDeltaToTts(text);
    },
    onEnd: (payload?: { roundDone: number; nextQaId?: number; nextQuestion?: string; finished?: boolean }) => {
      submitting.value = false;
      finishStreaming();
      // 缓冲尾巴（无句末标点的收尾句）入队，保证整段话术播完才重新开麦
      flushTtsBuffer();
      // v11.90 V2：成功作答清零连续跳过计数（跳过路径保留计数）
      if (!isSkip) skipStreak.value = 0;
      if (payload?.finished) {
        toast.info('面试结束，正在生成报告...');
        handleFinish();
        return;
      }
      // v11.96 时长制：倒计时归零后的收尾（答案已自动提交保存，直接进报告）
      if (timeUpPending) {
        timeUpPending = false;
        toast.info('本场时间已到，面试结束，正在生成报告...');
        handleFinish();
        return;
      }
      if (payload?.nextQaId) {
        currentQaId.value = payload.nextQaId;
        if (typeof payload.roundDone === 'number') {
          interview.value!.currentIdx = payload.roundDone;
        }
        // 面试官话术已通过 delta 气泡渲染并播报，这里只重置答题现场
        resetForNewQuestion(payload.nextQuestion);
      }
    },
    // V11.0.2：流被服务端异常切断（后端 SSE 120s 超时收尾/网络中断），提示用户可重答或下一题
    onAborted: () => {
      toast.error('AI 响应超时中断，请重试或点击下一题继续');
      submitting.value = false;
      finishStreaming();
      clearTtsBuffer();
    },
    onError: (msg: string) => {
      toast.error(msg || '提交失败');
      submitting.value = false;
      finishStreaming();
      clearTtsBuffer();
    },
  };
}

async function handleSubmitAnswer() {
  if (!interview.value || !currentQaId.value) return;
  if (listening.value) stopAsr();
  // 服务端 ASR 兜底：等待录音停止与上传转写完成，确保语音文本已并入答案
  await whenTranscriptionDone();
  const transcript = editableAnswer.value || finalText.value;
  if (!transcript.trim()) {
    toast.warning('答案不能为空');
    return;
  }
  stopCountdown();
  submitting.value = true;
  pushChat('user', transcript);
  const latencyMs = answerStartTime.value ? Date.now() - answerStartTime.value : 0;
  clearTtsBuffer();
  // v11.94.1：首字前预建"思考中"占位气泡（delta 到达即续写；失败/中断由 finishStreaming 移除空占位）
  streamingMsgId = pushChat('ai', '', { streaming: true, tag: '思考中' });

  await submitVoiceAnswer(
    String(interview.value.id),
    String(currentQaId.value),
    transcript,
    latencyMs,
    buildAnswerCallbacks(false),
  );
}

// ==================== 智能提示（V3：POST /{id}/hint 返回单句 speakText） ====================
async function handleHint() {
  if (!interview.value || !currentQaId.value) {
    toast.warning('当前题目暂不支持提示');
    return;
  }
  const { data: vo, success } = await run(
    () => requestVoiceHint(interview.value!.id, String(currentQaId.value)),
    { errorToast: '提示获取失败' },
  );
  if (!success || !vo?.data) return;

  const hint = vo.data.hint;
  const text = hint?.speakText || '';
  if (!text) {
    toast.warning('暂无可用提示');
    return;
  }
  const used = vo.data.currentQa?.hintUsed ?? hint?.level ?? 1;
  pushChat('ai', text, { tag: `L${used} 提示` });
  if (!muteMode.value && ttsSupported.value) ttsSpeak(text);
  toast.success(`已获取 L${used} 提示（共 3 级）`);
}

// ==================== 跳过本题（V3：answer 接口 body 加 skip:true，走同一 SSE 渲染路径） ====================
async function handleSkip() {
  if (!interview.value || !currentQaId.value || submitting.value) return;
  // v11.90 V2：连续跳过 3 题触发结束确认（真实面试连续拒答会被终止）
  skipStreak.value++;
  if (skipStreak.value >= SKIP_END_THRESHOLD) {
    const end = await confirmModal.confirm(
      `已连续跳过 ${SKIP_END_THRESHOLD} 题，是否结束面试并生成报告？`,
      { title: '连续跳过', confirmText: '结束并生成报告', cancelText: '继续面试' },
    );
    if (end) {
      await handleFinish();
      return;
    }
    skipStreak.value = 0; // 选择继续：重新计数
  }
  if (speaking.value) ttsCancel();
  if (listening.value) stopAsr();
  stopCountdown();
  submitting.value = true;
  clearTtsBuffer();
  // v11.94.1：首字前预建"思考中"占位气泡（同作答路径）
  streamingMsgId = pushChat('ai', '', { streaming: true, tag: '思考中' });
  await submitVoiceAnswer(
    String(interview.value.id),
    String(currentQaId.value),
    '',
    0,
    buildAnswerCallbacks(true),
    true,
  );
}

// ==================== 结束面试 + 报告 ====================
/**
 * v11.88 V2：finish 同步段仅收口会话并触发异步批量分析（返回报告骨架）。
 * 前端进入报告页后轮询 analysis 接口驱动进度条，analysisStatus=2 后
 * 重新调用 finish（幂等，返回完整报告）拉取最终结果。
 */
async function handleFinish() {
  if (!interview.value) return;
  // v11.89：结束面试统一释放麦克风/播放器/计时器（有始有终）
  releaseMediaResources();
  // v11.90 V2：结束触发点收口——清理无响应计时与准备进度
  clearIdleWatch();
  stopPrepareProgress();
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
      // v11.88 V2：骨架报告（summary=报告生成中…）→ 启动分析进度轮询；
      // 完整报告（幂等重入/历史查看）→ 直接刷新对话回放数据
      if (reportVo.data.summary === '报告生成中…') {
        startAnalysisPolling();
      } else {
        toast.success('面试已结束，报告已生成');
        await refreshDetailAfterFinish();
      }
    }
  } finally {
    loading.value = false;
  }
}

// ==================== v11.88 V2：报告分析进度轮询 ====================
// 5s 间隔 × 120 次 = 10 分钟上限（与记账 AI 任务轮询节奏一致）
const ANALYSIS_POLL_INTERVAL = 5000;
const ANALYSIS_POLL_MAX = 120;
let analysisPollHandle: ReturnType<typeof setTimeout> | null = null;
let analysisPollCount = 0;

function stopAnalysisPolling() {
  if (analysisPollHandle) {
    clearTimeout(analysisPollHandle);
    analysisPollHandle = null;
  }
  analysisPollCount = 0;
}

function startAnalysisPolling() {
  stopAnalysisPolling();
  analysisState.value = { active: true, progress: Math.max(5, analysisState.value.progress) };
  pollAnalysisOnce();
}

async function pollAnalysisOnce() {
  const id = interview.value?.id;
  if (!id || phase.value !== 'report') {
    stopAnalysisPolling();
    return;
  }
  try {
    const { data: vo, success } = await run(() => getVoiceAnalysisStatus(id), { silent: true });
    if (success && vo?.data) {
      // 进度只进不退（服务端条件更新偶发回读旧值时保持前端观感单调）
      analysisState.value.progress = Math.max(analysisState.value.progress, vo.data.analysisProgress ?? 0);
      if (vo.data.analysisStatus === 2) {
        analysisState.value.progress = 100;
        await loadFullReport();
        analysisState.value.active = false;
        return;
      }
    }
  } catch {
    /* 单次轮询失败不中断，下轮重试 */
  }
  analysisPollCount++;
  if (analysisPollCount >= ANALYSIS_POLL_MAX) {
    toast.error('报告生成超时，可稍后从「我的面试记录」查看');
    analysisState.value.active = false;
    return;
  }
  analysisPollHandle = setTimeout(pollAnalysisOnce, ANALYSIS_POLL_INTERVAL);
}

/** analysisStatus=2 后拉取完整报告（finish 幂等：已结束会话直接返回库中报告） */
async function loadFullReport() {
  const id = interview.value?.id;
  if (!id) return;
  const { data: vo, success } = await run(() => finishVoiceInterview(id), { silent: true });
  if (success && vo?.data) {
    report.value = vo.data;
    toast.success('报告已生成');
  }
  await refreshDetailAfterFinish();
}

/** v11.30.3：finish 后重拉详情填充 qaList（对话回放 Tab 数据源） */
async function refreshDetailAfterFinish() {
  try {
    const detailId = interview.value?.id;
    if (detailId) {
      const { data: detailResp, success: detailOk } = await run(
        () => getVoiceInterviewDetail(detailId),
        { silent: true },
      );
      if (detailOk && detailResp?.data?.qaList?.length) {
        interview.value = detailResp.data;
      }
    }
  } catch {
    /* 详情刷新失败不影响报告展示（可从历史记录重新进入查看回放） */
  }
}

async function confirmEnd() {
  if (await confirmModal.confirm('确定要结束面试吗？将生成复盘报告。', { danger: true,  title: '确认操作'})) {
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
  resetAsrWithTracker();
  // v11.89：重开面试同样统一释放，避免残留占用
  releaseMediaResources();
  // v11.88 V2：重开时停止上一场的分析进度轮询并复位进度
  stopAnalysisPolling();
  analysisState.value = { active: false, progress: 0 };
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
async function downloadReport() {
  toast.info('正在生成 PDF 报告，请稍候...');
  setTimeout(() => window.print(), 300);
}

async function shareReport() {
  const id = interview.value?.id ?? report.value?.interviewId;
  if (!id) {
    toast.warning('暂无可分享的报告');
    return;
  }
  // v11.30.5：token 分享（免登录公开，7 天有效），替代旧的需登录 ?id= 链接
  const { success, data } = await run(() => createReportShareToken(id), { errorToast: '生成分享链接失败' });
  const token = (data as unknown as { data?: string })?.data ?? (data as unknown as string);
  if (!success || !token) {
    return;
  }
  const url = `${window.location.origin}/interview/share/${token}`;
  try {
    await navigator.clipboard.writeText(url);
    toast.success('分享链接已复制（7 天内有效，无需登录即可查看）');
  } catch {
    await promptModal.prompt('复制分享链接（7 天内有效）：', { title: '分享报告', defaultValue: url });
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
  <div class="vi-shell max-w-7xl mx-auto px-4 sm:px-6 lg:px-8" :class="`vi-shell--${phase}`">
    <!-- ==================== 准备页 ==================== -->
    <div v-if="phase === 'setup'" class="prep-page">
      <div class="prep-container">
        <!-- 顶部工具行：标题 + 描述 + 操作。
             品牌 / 导航 / 面包屑由站点 Navbar 统一提供，页面内不再重复绘制
             （原自画顶栏造成"双重导航 + 双重留白"，v13.37 移除） -->
        <div class="prep-toolbar">
          <div class="prep-toolbar-main">
            <h1 class="prep-toolbar-title">AI 语音面试准备</h1>
            <p class="prep-toolbar-desc">完成以下准备步骤，开始你的模拟面试</p>
          </div>
          <div class="prep-toolbar-actions">
            <button class="prep-ghost-btn" @click="router.push('/interview/voice/history')">📋 我的面试记录</button>
          </div>
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

        <!-- v11.91 断点续接横幅：意外关闭后再次进入，提示继续上次面试 -->
        <div v-if="activeInterview" class="resume-banner">
          <div class="resume-banner-info">
            <span class="resume-banner-title">🔄 检测到未完成的面试</span>
            <span class="resume-banner-meta">
              {{ activeInterview.position || '未指定岗位' }} · 已答 {{ activeInterview.answered ?? 0 }}/{{
                activeInterview.totalQa ?? '?'
              }}题{{ activeInterview.elapsedSec ? ` · 已进行 ${formatElapsed(activeInterview.elapsedSec)}` : '' }}
            </span>
          </div>
          <div class="resume-banner-actions">
            <button class="resume-btn primary" :disabled="resuming" @click="handleResume">
              {{ resuming ? '恢复中…' : '▶ 继续面试' }}
            </button>
            <button class="resume-btn ghost" @click="handleAbandonActive">放弃并生成报告</button>
          </div>
        </div>

        <!-- 第一步：设备检测（真实枚举 + 权限查询/授权 + 耳机识别） -->
        <div class="prep-card">
          <div class="prep-card-title"><span class="step-badge">1</span>设备检测</div>
          <div class="device-check-list">
            <!-- 麦克风：权限状态 + 输入设备选择 + 真实采集电平测试 -->
            <div class="device-item">
              <div class="device-info"><div class="device-icon">🎤</div><span class="device-name">麦克风</span></div>
              <div class="device-status">
                <span :class="['status-badge', micStatusClass]">{{ micStatusText }}</span>
                <select v-model="selectedInputId" class="device-select" title="选择输入设备">
                  <option value="default">系统默认输入设备</option>
                  <option v-for="d in audioInputs" :key="d.deviceId" :value="d.deviceId">{{ d.label }}</option>
                </select>
                <button class="test-btn" :disabled="deviceTesting.mic" @click="testMic">
                  {{ deviceTesting.mic ? '测试中…' : deviceTested.mic ? '✓ 通过 · 重测' : '测试' }}
                </button>
              </div>
              <!-- 真实电平条：说话时跳动 -->
              <div v-if="micTesting" class="mic-level-row">
                <div class="mic-level-bar">
                  <div class="mic-level-fill" :style="{ width: `${Math.min(100, micLevel * 100)}%` }"></div>
                </div>
                <span class="mic-level-text">请说话… {{ Math.round(micLevel * 100) }}%</span>
              </div>
            </div>

            <!-- 扬声器：输出设备选择 + 真实测试音播放（支持指定设备路由） -->
            <div class="device-item">
              <div class="device-info"><div class="device-icon">🔊</div><span class="device-name">扬声器</span></div>
              <div class="device-status">
                <span :class="['status-badge', audioOutputs.length > 0 ? 'ok' : 'checking']">{{ speakerStatusText }}</span>
                <select v-model="selectedOutputId" class="device-select" title="选择输出设备（TTS 与测试音从该设备播放）">
                  <option value="default">系统默认输出设备</option>
                  <option v-for="d in audioOutputs" :key="d.deviceId" :value="d.deviceId">{{ d.label }}</option>
                </select>
                <button class="test-btn" :disabled="deviceTesting.speaker" @click="testSpeaker">
                  {{ deviceTesting.speaker ? '播放中…' : deviceTested.speaker ? '✓ 通过 · 重测' : '测试' }}
                </button>
              </div>
              <div v-if="speakerTestDevice" class="mic-level-row">
                <span class="mic-level-text">↪ 正在通过「{{ speakerTestDevice }}」播放测试音，请确认能听到</span>
              </div>
            </div>

            <!-- 耳机：基于设备 label 的启发式检测 -->
            <div class="device-item">
              <div class="device-info"><div class="device-icon">🎧</div><span class="device-name">耳机（推荐）</span></div>
              <div class="device-status"><span :class="['status-badge', headphoneStatusClass]">{{ headphoneStatusText }}</span></div>
            </div>

            <!-- v11.90 V2：环境噪声检测（3 秒采样取平均；嘈杂时黄色提示换环境/戴耳机） -->
            <div class="device-item">
              <div class="device-info"><div class="device-icon">🌊</div><span class="device-name">环境噪声</span></div>
              <div class="device-status">
                <span :class="['status-badge', noiseStatusClass]">{{ noiseStatusText }}</span>
                <button class="test-btn" :disabled="noiseTesting" @click="testNoise">
                  {{ noiseTesting ? '检测中…' : noiseTested ? '✓ 已检测 · 重测' : '检测' }}
                </button>
              </div>
              <div v-if="noiseTesting" class="mic-level-row">
                <div class="mic-level-bar">
                  <div class="mic-level-fill" :style="{ width: `${Math.min(100, micLevel * 100)}%` }"></div>
                </div>
                <span class="mic-level-text">正在采样环境音… {{ Math.round(micLevel * 100) }}%</span>
              </div>
              <div v-else-if="noiseAvgLevel !== null" :class="['noise-result', { noisy: isNoisy }]">
                {{ isNoisy
                  ? `⚠ 环境较嘈杂（约 ${noiseDb}），建议换到安静环境或佩戴耳机`
                  : `✅ 环境安静（约 ${noiseDb}），适合面试` }}
              </div>
            </div>
          </div>
          <div class="device-tip">💡 建议佩戴耳机避免回声。麦克风权限可在浏览器地址栏图标中管理；插拔设备后列表会自动刷新。</div>
        </div>

        <!-- 第二步：岗位与简历（v11.90 V2：核心只留岗位 + JD + 简历，其余配置收进高级设置） -->
        <div class="prep-card">
          <div class="prep-card-title"><span class="step-badge">2</span>岗位与简历</div>

          <!-- v11.88：岗位选择保持紧凑下拉 -->
          <div class="config-grid">
            <div class="config-item">
              <label class="config-label">🎯 面试岗位</label>
              <select v-model="positionSelectValue" class="config-select" :disabled="jobTemplatesLoading">
                <option v-if="jobTemplatesLoading" value="">正在加载岗位模板…</option>
                <option v-for="opt in jobTemplates" :key="opt.id" :value="opt.name">
                  {{ opt.name }}{{ opt.category ? ` · ${opt.category}` : '' }}
                </option>
                <option value="__custom__">自定义（手动输入岗位名称）</option>
              </select>
              <div v-if="!jobTemplatesLoading && jobTemplates.length === 0" class="config-hint">
                暂无可选岗位模板，请在后台【岗位模板】中配置；也可选「自定义」直接输入
              </div>
            </div>
          </div>

          <!-- 自定义岗位：选中"自定义"时展开输入（对齐数据库 varchar(64)） -->
          <div v-if="useCustomPosition" class="config-item custom-position-item">
            <label class="config-label">自定义岗位名称（上限 {{ POSITION_MAX_LEN }} 字）</label>
            <div class="custom-position-wrap">
              <input
                v-model="customPosition"
                class="custom-position-input"
                :maxlength="POSITION_MAX_LEN"
                placeholder="输入目标岗位，如：Go 后端开发"
              />
              <span class="custom-position-counter">{{ customPosition.length }}/{{ POSITION_MAX_LEN }}</span>
            </div>
          </div>

          <!-- v11.90 V2：岗位要求 JD（面试官 AI 交流贴合岗位要求，限制因素之一）
               v13.37：选中岗位模板后自动回填模板 JD，用户可自由修改；未选岗位/自定义则以手输为准 -->
          <div class="config-item jd-item">
            <label class="config-label">
              📋 岗位要求（选填）
              <span v-if="appliedTemplate" class="jd-source-badge">已按模板「{{ appliedTemplate.name }}」回填 · 可修改</span>
            </label>
            <textarea
              v-model="jobRequirements"
              class="jd-textarea"
              rows="4"
              maxlength="2000"
              placeholder="粘贴目标岗位 JD，AI 面试官将据此调整提问方向和深度，例如：&#10;1. 5年以上Java开发经验，精通Spring Boot&#10;2. 熟悉微服务架构，有分布式系统设计经验"
              @input="jdEditedByUser = true"
            ></textarea>
            <div class="jd-counter">
              <span class="jd-counter-actions">
                <button v-if="canRestoreTemplate" type="button" class="jd-inline-btn" @click="restoreTemplate">
                  ↺ 恢复模板值
                </button>
                <button v-if="jobRequirements" type="button" class="jd-inline-btn" @click="clearJobRequirements">
                  ✕ 清空
                </button>
              </span>
              <span>{{ jobRequirements.length }}/2000</span>
            </div>
          </div>
          <div class="agent-hint">
            ⓘ 选中岗位后自动带入该岗位的 JD / 难度 / 题量（后台【岗位模板】可配），你可随时修改；
            <strong>不选岗位则以此处输入为准</strong>，留空则按岗位通用标准出题
          </div>

          <div class="resume-section">
            <!-- 简历库加载中 -->
            <div v-if="resumeLoading" class="resume-empty">
              <div class="empty-icon">⏳</div>
              <div class="empty-title">正在加载简历库…</div>
            </div>

            <!-- 简历库为空：引导上传（AI 解析回填）或创建 -->
            <div v-else-if="resumeList.length === 0" class="resume-empty">
              <div class="empty-icon">📋</div>
              <div class="empty-title">还没有在线简历</div>
              <div class="empty-desc">上传附件简历，AI 自动解析并回填为默认简历，面试官将针对你的项目经历深挖提问</div>
              <!-- 解析进度 -->
              <div v-if="resumeUploading" class="resume-parsing-msg">⏳ {{ resumeParsingMsg || '处理中…' }}</div>
              <div class="empty-actions">
                <button class="resume-action-btn primary" :disabled="resumeUploading" @click="triggerResumeUpload">
                  {{ resumeUploading ? '解析中…' : '⬆ 上传简历（AI 解析）' }}
                </button>
                <button class="resume-action-btn" @click="router.push('/interview/my/resumes')">去创建在线简历</button>
              </div>
            </div>

            <!-- v11.89：简历收起/展开面板（默认收起保持紧凑；按钮常显当前选择，选中联动求职意向到岗位） -->
            <template v-else>
              <button class="resume-collapse-btn" @click="toggleResumePanel">
                <span class="resume-collapse-title">
                  📄 {{ selectedResume
                    ? (selectedResume.title || `${selectedResume.name || '我的'}的简历`)
                    : '选择简历（可选）' }}
                </span>
                <span class="resume-collapse-state">
                  {{ selectedResume ? '已选择' : '未选择' }}
                  <span class="resume-collapse-arrow" :class="{ open: resumePanelOpen }">▾</span>
                </span>
              </button>
              <div v-if="resumePanelOpen" class="resume-panel">
                <div
                  :class="['resume-option-row', { active: selectedResumeId === null }]"
                  @click="clearResumeSelection"
                >
                  <span class="resume-option-title">不选择（按岗位通用题库出题）</span>
                </div>
                <div
                  v-for="r in resumeList"
                  :key="r.id"
                  :class="['resume-option-row', { active: Number(selectedResumeId) === Number(r.id) }]"
                  @click="chooseResume(r)"
                >
                  <span class="resume-option-title">
                    {{ r.title || `${r.name || '我的'}的简历` }}{{ r.jobIntention?.position ? ` · 期望：${r.jobIntention.position}` : '' }}
                  </span>
                  <span class="resume-option-meta">
                    {{ r.score ? `AI评分 ${r.score}` : '' }}{{ r.updateTime ? ` · 更新 ${r.updateTime.slice(0, 10)}` : '' }}
                  </span>
                </div>
                <div class="resume-manage-row">
                  <button class="resume-action-btn" :disabled="resumeUploading" @click="triggerResumeUpload">
                    {{ resumeUploading ? (resumeParsingMsg || '解析中…') : '⬆ 上传新简历（AI 解析）' }}
                  </button>
                  <button class="resume-action-btn" @click="router.push('/interview/my/resumes')">管理简历库</button>
                  <span class="resume-manage-hint">选择简历后，AI 将针对项目经历深挖提问</span>
                </div>
              </div>
            </template>
            <!-- 隐藏文件选择：上传附件简历 → AI 解析 → 回填默认简历 -->
            <input
              ref="resumeUploadInput"
              type="file"
              accept=".pdf,.doc,.docx,.txt"
              class="hidden-file-input"
              @change="handleResumeUpload"
            />
          </div>
        </div>

        <!-- v11.90 V2：高级设置折叠（其余配置可酌情保留；默认收起保持准备页紧凑） -->
        <div class="prep-card advanced-card">
          <button class="advanced-toggle" type="button" @click="advancedOpen = !advancedOpen">
            <span class="advanced-toggle-title"><span class="step-badge">⚙</span>高级设置（选填）</span>
            <span class="advanced-toggle-state">{{ advancedOpen ? '收起 ▴' : '展开 ▾' }}</span>
          </button>
          <div v-show="advancedOpen" class="advanced-body">
            <div class="config-grid">
              <div class="config-item">
                <label class="config-label">难度等级</label>
                <select v-model="config.difficulty" class="config-select">
                  <option v-for="o in DIFFICULTY_OPTIONS" :key="o.label" :value="o.value">{{ o.label }}</option>
                </select>
              </div>
              <div class="config-item">
                <label class="config-label">题目数量</label>
                <select v-model.number="questionCount" class="config-select">
                  <option v-for="o in QUESTION_COUNT_OPTIONS" :key="o.value" :value="o.value">{{ o.label }}</option>
                </select>
              </div>
            </div>
            <div class="toggle-row">
              <span class="toggle-label">🔇 静音模式（仅文字，不播放语音）</span>
              <label class="toggle-switch">
                <input v-model="muteMode" type="checkbox">
                <span class="toggle-slider"></span>
              </label>
            </div>
            <div class="toggle-row">
              <span class="toggle-label">🎧 自动聆听（面试官说完自动开麦，随时可打断）</span>
              <label class="toggle-switch">
                <input v-model="autoListen" type="checkbox">
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
        </div>

        <button class="start-btn" :disabled="loading" @click="handleStart">
          {{ loading ? '正在开启...' : '🚀 开始 AI 语音面试' }}
        </button>
      </div>

      <!-- v11.90 V2：准备进度覆盖层（不可关闭；完成自动进入面试页） -->
      <div v-if="prepareState.active" class="prepare-overlay">
        <div class="prepare-modal">
          <div class="prepare-title">🔄 正在准备面试…</div>
          <div class="prepare-track">
            <div class="prepare-fill" :style="{ width: prepareState.progress + '%' }"></div>
          </div>
          <div class="prepare-steps-list">
            <div
              v-for="(s, i) in PREPARE_STEPS"
              :key="s"
              :class="['prepare-step-row', { done: prepareState.step > i, current: prepareState.step === i }]"
            >
              <span class="prepare-step-icon">{{ prepareState.step > i ? '✅' : prepareState.step === i ? '🔄' : '⏳' }}</span>
              <span class="prepare-step-text">{{ s }}（{{ i + 1 }}/{{ PREPARE_STEPS.length }}）</span>
            </div>
          </div>
          <div class="prepare-eta">面试题单基于你的岗位要求、简历与配置生成，请稍候</div>
        </div>
      </div>
    </div>

    <!-- ==================== 面试进行页 ==================== -->
    <div v-else-if="phase === 'interview'" class="interview-page">
      <div class="top-bar">
        <div class="top-bar-left">
          <div class="top-bar-logo">🎙️ 旭林知行</div>
          <div
            :class="['timer-display', { urgent: interviewRemainSec > 0 && interviewRemainSec <= 300 }]"
            :title="`已进行 ${formatElapsed(elapsedSec)}`"
          ><span>⏳</span><span>{{ formatRemain(interviewRemainSec) }}</span></div>
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
          <button class="control-btn" :disabled="loading || submitting" @click="handleSkip">⏭️ 跳过本题</button>
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
            <div class="interviewer-name">{{ interview?.agentName || 'AI 面试官' }}</div>
            <div class="interviewer-meta">
              <div v-if="speaking" class="interviewer-state">🔊 正在播报</div>
              <div v-else-if="submitting" class="interviewer-state thinking">🧠 分析中</div>
            </div>
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
                <div v-else-if="m.tag" class="ai-tag">{{ m.tag }}</div>

                <template v-if="m.role === 'ai'">
                  <MarkdownRenderer editor-mode="markdown" :content-markdown="m.content" prose-width="none" />
                  <span v-if="m.streaming" class="streaming-cursor"></span>
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
              <span>⏱️</span><span>已用时 {{ answerElapsed }} 秒 · 不限时，想好再提交</span>
              <span v-if="asrTranscribing" class="interim-hint">· 正在转写语音…</span>
              <span v-else-if="interimText" class="interim-hint">· 实时识别：{{ interimText }}</span>
            </div>
            <div class="input-container">
              <textarea
                :value="answerDisplay"
                class="input-textarea"
                :class="{ 'asr-live': listening && (!!interimText || asrServerMode) }"
                :placeholder="listening
                  ? (asrServerMode && !asrStreamMode
                    ? '正在聆听，请开始作答…（停止录音后自动转写为文字）'
                    : '正在聆听，请开始作答…（语音将实时转写到这里，可随时手动修改）')
                  : '面试官你好，我认为...'"
                rows="2"
                :disabled="submitting"
                @input="onAnswerInput"
              ></textarea>
              <button
                class="mic-btn"
                :class="{ recording: listening }"
                :disabled="submitting"
                @click="toggleMic"
              >
                {{ asrTranscribing ? '⏳' : listening ? '⏹️' : '🎙️' }}
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

        <!-- 右面板（v11.88 V2：实时评分雷达已移除，改面试背景+提示，化繁为简） -->
        <div class="right-panel">
          <div class="analysis-card">
            <div class="analysis-card-title">🎯 面试背景</div>
            <div class="dimension-legend">
              <div v-for="item in interviewBackground" :key="item.label" class="dimension-legend-row">
                <span class="legend-label">{{ item.label }}</span>
                <span class="legend-desc">{{ item.value }}</span>
              </div>
              <div class="legend-tip">六维评分与深度分析将在面试结束后的报告中统一呈现</div>
            </div>
          </div>
          <div class="analysis-card">
            <div class="analysis-card-title">💡 面试官提示</div>
            <div class="hint-text">
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
        <!-- v11.88 V2：批量分析进度条（结束后轮询 analysis 接口驱动，完成自动拉取完整报告） -->
        <div v-if="analysisState.active" class="report-analysis-progress">
          <div class="rap-header">
            <span class="rap-spinner"></span>
            <span class="rap-step">{{ analysisStepText }}</span>
            <span class="rap-num">{{ analysisState.progress }}%</span>
          </div>
          <div class="rap-track">
            <div class="rap-fill" :style="{ width: analysisState.progress + '%' }"></div>
          </div>
          <div class="rap-tip">深度分析需要一点时间，可先切换到「对话回放」浏览；完成后报告自动呈现</div>
        </div>
        <!-- v11.97：报告总览横幅卡——综合分 + 等级 + 整场总评（LLM 复盘）+ 岗位匹配度（含依据） -->
        <div v-if="report && !analysisState.active" class="report-banner-card">
          <div class="banner-score">
            <span class="banner-score-value" :style="{ color: scoreColor(report.totalScore ?? 0) }">
              {{ report.totalScore ?? 0 }}
            </span>
            <span class="banner-score-label">综合得分</span>
            <span :class="['score-level-badge', scoreLevel.cls]">{{ scoreLevel.label }}</span>
            <!-- v13.47 批次 2：水平定级徽章（LLM 结构化输出 levelEstimate；缺失则不展示） -->
            <span v-if="levelBadge" :class="['level-estimate-badge', levelBadge.key]" :title="levelBadge.tip">
              定级 · {{ levelBadge.text }}
            </span>
          </div>
          <div class="banner-divider"></div>
          <div class="banner-main">
            <div class="banner-comment">{{ reportOverall || '本次面试尚未形成完整总结。' }}</div>
            <div v-if="jobMatchReason" class="banner-match-reason">🎯 {{ jobMatchReason }}</div>
          </div>
          <div v-if="jobMatchRate != null" class="banner-match">
            <span class="match-label">岗位匹配度</span>
            <span class="match-rate" :style="{ color: scoreColor(jobMatchRate) }">{{ jobMatchRate }}%</span>
            <div class="match-bar">
              <div class="match-fill" :style="{ width: jobMatchRate + '%' }"></div>
            </div>
          </div>
        </div>
        <!-- v11.90 V2：报告三段式——第一栏 面试者简介（简历提取 + 口头自我介绍）+ 第二栏 岗位信息 -->
        <div v-if="candidateInfo || jobInfoView" class="report-profile-grid">
          <div v-if="candidateInfo" class="profile-card">
            <div class="profile-card-title">👤 面试者简介</div>
            <div class="candidate-name-row">
              <span class="candidate-name">{{ candidateInfo.name || '面试者' }}</span>
              <span v-if="candidateInfo.aiScore" class="candidate-ai-score">简历 AI 评分 {{ candidateInfo.aiScore }}</span>
            </div>
            <div v-if="candidateSkills.length" class="candidate-skills-row">
              <span v-for="s in candidateSkills" :key="s" class="candidate-skill">{{ s }}</span>
            </div>
            <div v-if="candidateInfo.resumeSelfIntro" class="candidate-intro-block">
              <span class="intro-block-label">📝 简历自我介绍</span>
              <p class="intro-block-text">{{ candidateInfo.resumeSelfIntro }}</p>
            </div>
            <div v-if="candidateInfo.interviewSelfIntro" class="candidate-intro-block">
              <span class="intro-block-label">🎤 面试口头自我介绍</span>
              <p class="intro-block-text">{{ candidateInfo.interviewSelfIntro }}</p>
            </div>
          </div>
          <div v-if="jobInfoView" class="profile-card">
            <div class="profile-card-title">🎯 岗位信息</div>
            <div class="job-position">{{ jobInfoView.position || interview?.position || '未指定岗位' }}</div>
            <div v-if="jobInfoView.jobRequirements" class="candidate-intro-block">
              <span class="intro-block-label">📋 岗位要求</span>
              <p class="intro-block-text jd-text">{{ jobInfoView.jobRequirements }}</p>
            </div>
            <div v-if="jobInfoView.matchRate != null" class="job-match-row">
              <span class="match-label">岗位匹配度</span>
              <span class="match-rate" :style="{ color: scoreColor(Number(jobInfoView.matchRate)) }">
                {{ jobInfoView.matchRate }}%
              </span>
              <div class="match-bar">
                <div class="match-fill" :style="{ width: jobInfoView.matchRate + '%' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 历史保留元信息：岗位/难度/时间 -->
        <div class="report-meta-bar">
          <div class="meta-chip">
            <span class="meta-label">目标岗位</span>
            <span class="meta-value">{{ interview?.position || '未指定' }}</span>
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
        <!-- v13.47 批次 2：三层五 tab —— 复盘区（概要→归因→证据殿后）+ 备战区（个人预测→环境导航） -->
        <div class="report-tabs">
          <span class="report-tabs-group">复盘</span>
          <button
            :class="['report-tab', { active: reportTab === 'summary' }]"
            @click="reportTab = 'summary'"
          >📋 面试概要</button>
          <button
            :class="['report-tab', { active: reportTab === 'analysis' }]"
            @click="reportTab = 'analysis'"
          >🔍 问题分析</button>
          <span class="report-tabs-group">备战</span>
          <button
            v-if="showPredictTab"
            :class="['report-tab', { active: reportTab === 'predict' }]"
            @click="reportTab = 'predict'"
          >🔮 追问预测</button>
          <button
        <!-- ==================================================================
             Tab「🧭 发展方向」（备战区·环境）v13.50 批次 3
             懒生成：不自动触发，用户点按钮才调 LLM（不开不花）
             口径诚实：LLM 无实时行业数据 → 只做方向性判断，不承诺实时动态
             ================================================================== -->
        <div v-if="reportTab === 'insight'" class="tab-content active">
          <div class="insight-head">
            <div class="insight-head-title">🧭 发展方向</div>
            <div class="insight-head-desc">
              基于本场表现与岗位方向的个人化分析 —— 把本场暴露的短板放进岗位坐标系，告诉你往哪补最划算。
            </div>
            <div v-if="industryInsight" class="insight-head-meta">
              <span class="insight-stamp">{{ insightGeneratedAtText }}</span>
              <span v-if="insightStale" class="insight-stale">· 已超过 7 天，建议刷新</span>
              <button class="insight-refresh" :disabled="insightLoading" @click="handleGenerateInsight(true)">
                {{ insightLoading ? '生成中…' : '🔄 刷新' }}
              </button>
            </div>
          </div>

          <!-- 占位：未生成（懒生成，必须用户点击） -->
          <div v-if="!industryInsight" class="insight-placeholder">
            <div class="insight-placeholder-icon">🧭</div>
            <div class="insight-placeholder-title">生成发展方向分析</div>
            <div class="insight-placeholder-desc">
              约需 10 秒。内容包含技术趋势、技能供需结构与 3 条针对你本场薄弱点的行动建议。
            </div>
            <button
              class="insight-placeholder-btn"
              :disabled="insightLoading"
              @click="handleGenerateInsight(false)"
            >{{ insightLoading ? '生成中，请稍候…' : '开始生成' }}</button>
            <div v-if="insightError" class="insight-error">{{ insightError }}</div>
          </div>

          <!-- 已生成 -->
          <template v-else>
            <!-- ① 技术趋势 -->
            <section v-if="industryInsight.trends?.length" class="insight-section">
              <div class="insight-section-title">📈 技术趋势</div>
              <div class="trend-list">
                <div v-for="(t, i) in industryInsight.trends" :key="'tr' + i" class="trend-card">
                  <div class="trend-card-head">
                    <span class="trend-title">{{ t.title }}</span>
                    <span v-if="t.maturity" :class="['trend-maturity', maturityClass(t.maturity)]">
                      {{ t.maturity }}
                    </span>
                  </div>
                  <div v-if="t.detail" class="trend-detail">{{ t.detail }}</div>
                </div>
              </div>
            </section>

            <!-- ② 技能供需结构 -->
            <section
              v-if="industryInsight.supplyDemand?.existing?.length || industryInsight.supplyDemand?.missing?.length"
              class="insight-section"
            >
              <div class="insight-section-title">🧩 技能供需结构</div>
              <div class="sd-row">
                <span class="sd-label">✅ 已具备</span>
                <span class="sd-tags">
                  <span
                    v-for="(s, i) in (industryInsight.supplyDemand?.existing ?? [])"
                    :key="'se' + i"
                    class="sd-tag have"
                  >{{ s }}</span>
                  <span v-if="!industryInsight.supplyDemand?.existing?.length" class="sd-empty">简历未提取到技能标签</span>
                </span>
              </div>
              <div class="sd-row">
                <span class="sd-label">⚠️ 建议补充</span>
                <span class="sd-tags">
                  <span
                    v-for="(s, i) in (industryInsight.supplyDemand?.missing ?? [])"
                    :key="'sm' + i"
                    class="sd-tag miss"
                  >{{ s }}</span>
                  <span v-if="!industryInsight.supplyDemand?.missing?.length" class="sd-empty">暂无建议</span>
                </span>
              </div>
            </section>

            <!-- ③ 行动建议（每条锚定本场真实薄弱点） -->
            <section v-if="industryInsight.actions?.length" class="insight-section">
              <div class="insight-section-title">🎯 行动建议</div>
              <div class="action-list">
                <div v-for="(a, i) in industryInsight.actions" :key="'ac' + i" class="action-card">
                  <div class="action-number">{{ i + 1 }}</div>
                  <div class="action-body">
                    <div class="action-content">{{ a.content }}</div>
                    <div v-if="a.relatedWeakPoint" class="action-anchor">
                      ↳ 对应本场薄弱点：{{ a.relatedWeakPoint }}
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <div class="insight-disclaimer">
              本分析为方向性判断（基于你本场的简历、岗位与表现），不包含实时行业数据。
            </div>
          </template>
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

        <!-- ==================================================================
             Tab 1: 面试概要（v13.49 批次 2：四区重排，对齐 V1.2 §6.2）
             A 首屏（结论）→ B 叙事区 → C 行动区（薄弱与建议相邻）→ D 归档区（折叠）
             设计意图：原 11 板块无主次 —— 资料类内容（自介分/心态/信号）降权折叠，
             结论与行动提到一屏内可见。
             ================================================================== -->
        <div v-if="reportTab === 'summary'" class="tab-content active">
          <!-- ---------- A 首屏：总分大数字 + 六维雷达 + 匹配度 + 定级徽章 ---------- -->
          <div class="sum-hero">
            <div class="sum-hero-score">
              <svg class="radar-chart" viewBox="0 0 200 200" style="max-width: 170px;">
                <polygon
                  v-for="(p, i) in RADAR_GRID"
                  :key="'hg' + i"
                  :points="p"
                  fill="none"
                  stroke="var(--theme-border)"
                  stroke-width="1"
                />
                <line
                  v-for="(a, i) in RADAR_AXES"
                  :key="'ha' + i"
                  :x1="a.x1" :y1="a.y1" :x2="a.x2" :y2="a.y2"
                  stroke="var(--theme-border)"
                  stroke-width="1"
                />
                <polygon
                  :points="reportRadarPoints"
                  :fill="scoreColor(report?.totalScore ?? 0)"
                  fill-opacity="0.22"
                  :stroke="scoreColor(report?.totalScore ?? 0)"
                  stroke-width="2"
                />
                <text
                  v-for="(m, i) in DIMENSION_META"
                  :key="'ht' + i"
                  :x="m.textPos.x" :y="m.textPos.y" :text-anchor="m.textPos.anchor"
                  font-size="9"
                  fill="var(--theme-text-secondary)"
                >{{ m.label }}</text>
              </svg>
            </div>
            <div class="sum-hero-meta">
              <div class="sum-hero-total">
                <span class="sum-hero-total-value" :style="{ color: scoreColor(report?.totalScore ?? 0) }">
                  {{ report?.totalScore ?? 0 }}
                </span>
                <span class="sum-hero-total-unit">分</span>
              </div>
              <div class="sum-hero-badges">
                <span :class="['score-level-badge', scoreLevel.cls]">{{ scoreLevel.label }}</span>
                <span
                  v-if="levelBadge"
                  :class="['level-estimate-badge', levelBadge.key]"
                  :title="levelBadge.tip"
                >定级 · {{ levelBadge.text }}</span>
              </div>
              <div v-if="jobMatchRate != null" class="sum-hero-match">
                <span class="sum-hero-match-label">岗位匹配度</span>
                <div class="match-bar sum-hero-match-bar">
                  <div class="match-fill" :style="{ width: jobMatchRate + '%' }"></div>
                </div>
                <span class="sum-hero-match-value">{{ jobMatchRate }}%</span>
              </div>
              <div class="sum-hero-dim-legend">
                <span v-for="(m, mi) in DIMENSION_META" :key="'hl' + m.key" class="sum-hero-dim-chip">
                  {{ m.label }} <strong>{{ reportScores[mi] ?? 0 }}</strong>
                </span>
              </div>
            </div>
          </div>

          <!-- ---------- B 叙事区 + C 行动区（两栏：左=总评/亮点，右=薄弱点/建议） ---------- -->
          <div class="sum-main-grid">
            <!-- B 叙事区 -->
            <div class="sum-zone">
              <div class="sum-zone-title">📖 整场总评</div>
              <p class="summary-paragraph">{{ reportOverall || '本次面试尚未形成完整总结。' }}</p>
              <div v-if="jobMatchReason" class="banner-match-reason">🎯 {{ jobMatchReason }}</div>

              <div class="sum-zone-title sum-zone-title-spaced">✅ 亮点（{{ highlightItems.length }}）</div>
              <div v-for="(p, i) in highlightItems.slice(0, 4)" :key="'hp' + i" class="pros-item">
                <div class="pros-item-title">{{ p.title }}</div>
                <div v-if="p.detail" class="pros-item-quote">{{ p.detail }}</div>
              </div>
              <div v-if="highlightItems.length === 0" class="empty-tip">暂无亮点数据</div>
            </div>

            <!-- C 行动区：薄弱点与改进建议相邻（问题挨着答案） -->
            <div class="sum-zone">
              <div class="sum-zone-title">⚠️ 薄弱点（{{ weakPointItems.length }}）</div>
              <div v-for="(c, i) in weakPointItems.slice(0, 4)" :key="'cw' + i" class="pros-item">
                <div class="pros-item-title">{{ c.title }}</div>
                <div v-if="c.detail" class="pros-item-quote">{{ c.detail }}</div>
              </div>
              <div v-if="weakPointItems.length === 0" class="empty-tip">暂无薄弱点数据</div>

              <div class="sum-zone-title sum-zone-title-spaced">📋 改进建议</div>
              <ul class="suggestion-list">
                <li v-for="(s, i) in suggestionItems" :key="'sg' + i" class="suggestion-item">
                  <div class="suggestion-number">{{ i + 1 }}</div>
                  <div class="suggestion-content">{{ s }}</div>
                </li>
                <li v-if="suggestionItems.length === 0" class="empty-tip">暂无改进建议</li>
              </ul>
              <div class="sum-zone-actions">
                <button class="practice-btn" @click="reportTab = 'analysis'">🔍 查看逐题分析 →</button>
                <button class="practice-btn" @click="router.push('/interview/resume/optimize')">📝 按建议优化简历</button>
              </div>
            </div>
          </div>

          <!-- ---------- D 归档区（折叠）：资料类内容降权 ---------- -->
          <div
            v-if="introScoreView || report?.sentimentTrend?.length || report?.redFlags?.length || report?.fluencyAvg != null"
            class="sum-archive"
          >
            <button class="sum-archive-toggle" @click="archiveOpen = !archiveOpen">
              <span class="sum-archive-caret">{{ archiveOpen ? '▾' : '▸' }}</span>
              🗂 面试过程资料（自我介绍评分 · 心态趋势 · 可疑信号）
              <span class="sum-archive-hint">{{ archiveOpen ? '收起' : '展开' }}</span>
            </button>
            <div v-if="archiveOpen" class="sum-archive-body">
              <!-- 自我介绍评分 -->
              <div v-if="introScoreView" class="archive-block">
                <div class="archive-block-title">
                  🎤 自我介绍评分
                  <span class="intro-score-total" :style="{ color: scoreColor(introScoreView.total) }">
                    {{ introScoreView.total }} 分
                  </span>
                </div>
                <div class="intro-score-dims">
                  <div v-for="d in introScoreView.dims" :key="d.key" class="intro-score-dim">
                    <span class="intro-dim-label">{{ d.label }}</span>
                    <div class="intro-dim-bar">
                      <div :class="['score-fill', scoreClass(d.value)]" :style="{ width: d.value + '%' }"></div>
                    </div>
                    <span class="intro-dim-value">{{ d.value }}</span>
                  </div>
                </div>
                <div v-if="introScoreView.comment" class="intro-score-comment">{{ introScoreView.comment }}</div>
              </div>
              <!-- 心态趋势 / 流畅度 / 可疑信号 -->
              <div class="archive-block">
                <div class="archive-block-title">🧠 过程信号</div>
                <div v-if="report?.sentimentTrend?.length" class="deep-review-row">
                  <span class="deep-review-label">心态趋势</span>
                  <span class="sentiment-track">
                    <span
                      v-for="(s, i) in report.sentimentTrend"
                      :key="'as' + i"
                      :class="['sentiment-dot', s]"
                      :title="`第 ${i + 1} 轮：${SENTIMENT_LABEL[s] || s}`"
                    ></span>
                  </span>
                </div>
                <div v-if="report?.fluencyAvg != null" class="deep-review-row">
                  <span class="deep-review-label">表达流畅度</span>
                  <span class="deep-review-value">{{ report.fluencyAvg }} / 100</span>
                </div>
                <div v-if="report?.redFlags?.length" class="deep-review-row">
                  <span class="deep-review-label">可疑信号</span>
                  <span class="deep-review-flags">
                    <span v-for="(fg, i) in report.redFlags" :key="'af' + i" class="redflag-item">⚠ {{ fg }}</span>
                  </span>
                </div>
                <div
                  v-if="!report?.sentimentTrend?.length && report?.fluencyAvg == null && !report?.redFlags?.length"
                  class="empty-tip"
                >本次未产出过程信号数据</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab 2: 问题分析 -->
        <div v-if="reportTab === 'analysis'" class="tab-content active">
          <!-- v13.49 批次 2：按维度聚类（归因锚点 + 低分题置顶） -->
          <div v-if="weakestDimension || lowScoreCount" class="cluster-banner">
            <div class="cluster-banner-head">
              <span class="cluster-banner-icon">🧭</span>
              <span class="cluster-banner-title">最该补的维度</span>
              <span v-if="weakestDimension" class="cluster-dim-chip">
                {{ weakestDimension.label }} <strong>{{ weakestDimension.value }}</strong>
              </span>
            </div>
            <div class="cluster-banner-desc">
              <template v-if="lowScoreCount">
                下列 <strong>{{ lowScoreCount }}</strong> 道题得分低于 {{ LOW_SCORE_THRESHOLD }} 分，已<strong>置顶</strong>按分数升序排列，建议优先复盘。
              </template>
              <template v-else>本次各题得分均不低于 {{ LOW_SCORE_THRESHOLD }} 分，按分数升序排列便于定位短板。</template>
            </div>
          </div>

          <!-- v11.97：薄弱点收口（结构化 title+detail + 去练习） -->
          <div v-if="weakPointItems.length" class="weak-points-card">
            <div class="analysis-card-title">⚠️ 待提升（{{ weakPointItems.length }}）</div>
            <div v-for="(c, i) in weakPointItems" :key="i" class="weak-point-row">
              <div class="weak-point-text">
                <div class="weak-point-title">{{ c.title }}</div>
                <div v-if="c.detail" class="weak-point-quote">{{ c.detail }}</div>
              </div>
              <button class="practice-btn" @click="gotoPractice(c.title)">🎯 去练习</button>
            </div>
          </div>
          <div class="analysis-grid">
            <div
              v-for="(q, i) in lowScoreFirst"
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
              <!-- v11.97：你的回答折叠展开（默认 2 行，点击展开全文） -->
              <div v-if="q.userAnswer" class="analysis-user-answer" :class="{ expanded: expandedReviews.has(q.questionIdx ?? i) }">
                <div class="analysis-user-answer-text">
                  <strong>你的回答：</strong>{{ q.userAnswer }}
                </div>
                <button
                  v-if="q.userAnswer.length > 80"
                  class="answer-expand-btn"
                  @click="toggleReviewExpand(q.questionIdx ?? i)"
                >
                  {{ expandedReviews.has(q.questionIdx ?? i) ? '收起' : '展开全文' }}
                </button>
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
            <div v-if="lowScoreFirst.length === 0" class="empty-tip">暂无逐题分析数据</div>
          </div>
        </div>

        <!-- ==================================================================
             v13.47 批次 2：Tab「🔮 追问预测」（备战区·个人）
             分组 A「本次已问」= 复盘视角（答得对不对）
             分组 B「未被问到」= 预警视角（下次真面试的押题清单）★核心价值
             数据源：report.predictedQuestions（缺失则整 tab 不渲染，由 tab 按钮 v-if 控制）
             ================================================================== -->
        <div v-if="reportTab === 'predict'" class="tab-content active">
          <div class="predict-intro">
            <div class="predict-intro-title">🔮 真面试官拿到这份简历，还会问什么</div>
            <div class="predict-intro-desc">
              基于你本场的简历、岗位与对话记录推演，<strong>未被问到的部分</strong>是下次面试的重点准备方向。
            </div>
          </div>

          <!-- 分组 A：本次已问 -->
          <div v-if="predictedAsked.length" class="predict-group">
            <div class="predict-group-title">
              ✅ 本次已问
              <span class="predict-group-sub">复盘视角：对照要点，看看你答得够不够</span>
            </div>
            <div class="predict-list">
              <div v-for="(p, i) in predictedAsked" :key="'a' + i" class="predict-card">
                <div class="predict-card-head">
                  <div class="predict-q">{{ p.question }}</div>
                  <div v-if="p.askedScore != null" :class="['predict-score', scoreClass(p.askedScore)]">
                    {{ p.askedScore }} 分
                  </div>
                </div>
                <div v-if="p.knowledgePoint" class="predict-tag">考点：{{ p.knowledgePoint }}</div>
                <div v-if="p.briefAnswer" class="predict-block">
                  <span class="predict-block-label">要点参考</span>
                  <span class="predict-block-text">{{ p.briefAnswer }}</span>
                </div>
                <div v-if="p.analysis" class="predict-block">
                  <span class="predict-block-label">提问动机</span>
                  <span class="predict-block-text">{{ p.analysis }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 分组 B：未被问到 ★核心价值 -->
          <div v-if="predictedUnasked.length" class="predict-group">
            <div class="predict-group-title">
              ⭐ 未被问到
              <span class="predict-group-sub">预警视角：这些是你简历里写了、但本场没深挖的点</span>
            </div>
            <div class="predict-list">
              <div v-for="(p, i) in predictedUnasked" :key="'u' + i" class="predict-card highlight">
                <div class="predict-card-head">
                  <div class="predict-q">{{ p.question }}</div>
                </div>
                <div v-if="p.knowledgePoint" class="predict-tag">考点：{{ p.knowledgePoint }}</div>
                <div v-if="p.briefAnswer" class="predict-block">
                  <span class="predict-block-label">要点参考</span>
                  <span class="predict-block-text">{{ p.briefAnswer }}</span>
                </div>
                <div v-if="p.analysis" class="predict-block">
                  <span class="predict-block-label">提问动机</span>
                  <span class="predict-block-text">{{ p.analysis }}</span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="!predictedAsked.length && !predictedUnasked.length" class="empty-tip">
            本次未产出追问预测数据。
          </div>
        </div>

        <!-- ==================================================================
             v13.47 批次 2：Tab「🧭 发展方向」（备战区·环境）
             懒生成设计：本批先落占位卡（不自动触发），生成接口与内容渲染在批次 3 交付。
             口径诚实：LLM 无实时行业数据 → 不承诺"实时行业动态"，只做方向性判断。
             ================================================================== -->
        <div v-if="reportTab === 'insight'" class="tab-content active">
          <div class="insight-head">
            <div class="insight-head-title">🧭 发展方向</div>
            <div class="insight-head-desc">
              基于本场表现与岗位方向的个人化分析 —— 把本场暴露的短板放进岗位坐标系，告诉你往哪补最划算。
            </div>
          </div>
          <div class="insight-placeholder">
            <div class="insight-placeholder-icon">🧭</div>
            <div class="insight-placeholder-title">生成发展方向分析</div>
            <div class="insight-placeholder-desc">
              约需 10 秒。内容包含技术趋势、技能供需结构与 3 条针对你本场薄弱点的行动建议。
              <br />（本批先交付占位形态，生成能力在下一批开通）
            </div>
            <button class="insight-placeholder-btn" disabled>即将开通</button>
          </div>
        </div>

        <div class="report-actions">
          <button
            class="report-btn"
            :disabled="analysisState.active"
            @click="handleRegenerateReport()"
          >{{ analysisState.active ? '⏳ 报告生成中…' : '🔄 重新生成报告' }}</button>
          <button class="report-btn" @click="addAllWeakToWrongBook">📚 薄弱点入错题本</button>
          <button class="report-btn" @click="downloadReport">📥 下载 PDF 报告</button>
          <button class="report-btn" @click="shareReport">🔗 分享报告</button>
          <button class="report-btn primary" @click="startNewInterview">🚀 开始新的面试</button>
        </div>
      </div>
    </div>
  </div>
  <!-- v11.94.1：站点尾部放模板根级（vi-shell 外层，不受其水平 padding 挤压），
       宽度与首页一致（content-container 1280px 居中），全阶段统一显示 -->
  <SiteFooter />

<!-- v13.38：简历解析校对（U1 左右对照）——解析不落库，确认才落库 -->
<ResumeParsePreviewModal
  :visible="parsePreviewVisible"
  :data="parsePreviewData"
  :saving="parsePreviewSaving"
  @cancel="onParsePreviewCancel"
  @confirm="onParsePreviewConfirm"
/>
</template>

<style scoped>
/* ========== 视觉系统 · 红色主题（CSS 变量，与 demo HTML 一致） ========== */
.vi-shell {
  /* v11.88：视觉系统 · 跟随站点主题（light/dark/eye 由 html class 切换 --theme-* 变量） */
  --primary: var(--theme-primary);
  --primary-light: var(--theme-primary-hover);
  --primary-dark: var(--theme-primary-active);
  --primary-bg: var(--theme-primary-soft);
  --primary-border: var(--theme-accent-hover);
  --primary-gradient: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-primary-hover) 100%);
  --success: var(--theme-success);
  --success-bg: var(--theme-success-bg);
  --warning: var(--theme-warning);
  --warning-bg: var(--theme-warning-bg);
  --error: var(--theme-danger);
  --error-bg: var(--theme-danger-bg);
  --info: var(--theme-info);
  --info-bg: var(--theme-info-bg);
  --gray-50: var(--theme-bg-elevated);
  --gray-100: var(--theme-surface);
  --gray-200: var(--theme-border);
  --gray-300: var(--theme-border-strong);
  --gray-400: var(--theme-text-disabled);
  --gray-500: var(--theme-text-tertiary);
  --gray-600: var(--theme-text-secondary);
  --gray-700: var(--theme-text-secondary);
  --gray-800: var(--theme-text);
  --gray-900: var(--theme-text);
  --ai-bubble: var(--theme-surface);
  --ai-bubble-border: var(--theme-border);
  --user-bubble: var(--theme-primary-soft);
  --user-bubble-border: var(--theme-accent-hover);
  --question-bubble: var(--theme-warning-bg);
  --question-bubble-border: var(--theme-warning-bg-strong);
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

  /* 宽度/内边距统一交给外层 Tailwind 规范类：max-w-7xl mx-auto px-4 sm:px-6 lg:px-8
     （对齐《项目开发规范》§4.4⑤「主内容区域宽度规范」） */
  font-family: var(--font-sans);
  color: var(--gray-800);
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
}
.vi-shell button { font-family: inherit; }
.vi-shell { color-scheme: light; }
.dark .vi-shell { color-scheme: dark; }
.eye .vi-shell { color-scheme: light; }
.vi-shell * { box-sizing: border-box; }

/* ========== 准备页 ========== */
/* 不再强制 min-height:100vh —— 外层 Layout <main class="flex-1"> 已保证高度，
   页面按内容自适应，避免"一屏占满 + 中间大片空白" */
.prep-page { background: var(--theme-bg); }
.prep-container { padding: 0.5rem 0 1.25rem; }

/* 顶部工具行：标题 + 描述 + 操作（替代原自画品牌顶栏；全局导航由站点 Navbar 提供） */
.prep-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; flex-wrap: wrap; margin-bottom: 0.75rem; }
.prep-toolbar-main { display: flex; flex-direction: column; gap: 0.125rem; min-width: 0; }
.prep-toolbar-title { font-size: 1.25rem; font-weight: 700; color: var(--gray-900); line-height: 1.3; }
.prep-toolbar-desc { font-size: 0.875rem; color: var(--gray-500); }
.prep-toolbar-actions { display: flex; align-items: center; gap: 0.5rem; flex-shrink: 0; }
.prep-ghost-btn { padding: 0.375rem 0.75rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; color: var(--gray-600); display: inline-flex; align-items: center; gap: 0.375rem; transition: all 0.2s; white-space: nowrap; }
.prep-ghost-btn:hover { background: var(--gray-50); border-color: var(--gray-300); color: var(--gray-800); }

/* ==================== v11.91 断点续接横幅 ==================== */
.resume-banner { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; padding: 0.875rem 1.25rem; margin-bottom: 1.25rem; border-radius: var(--radius-lg); background: var(--primary-bg); border: 1px solid var(--primary); }
.resume-banner-info { display: flex; flex-direction: column; gap: 0.25rem; }
.resume-banner-title { font-size: 0.9375rem; font-weight: 700; color: var(--primary); }
.resume-banner-meta { font-size: 0.8125rem; color: var(--gray-600); }
.resume-banner-actions { display: flex; gap: 0.625rem; }
.resume-btn { padding: 0.5rem 1rem; border-radius: var(--radius-md); font-size: 0.875rem; font-weight: 600; cursor: pointer; transition: all 0.2s; white-space: nowrap; }
.resume-btn.primary { background: var(--primary); color: #fff; border: none; }
.resume-btn.primary:hover { filter: brightness(1.08); }
.resume-btn.primary:disabled { opacity: 0.6; cursor: not-allowed; }
.resume-btn.ghost { background: var(--theme-bg-elevated); color: var(--gray-600); border: 1px solid var(--gray-200); }
.resume-btn.ghost:hover { border-color: var(--error); color: var(--error); }
.report-header-actions { display: flex; align-items: center; gap: 0.75rem; }
.prep-steps { display: flex; align-items: center; justify-content: center; gap: 0; margin-bottom: 0.875rem; }
.prep-step { display: flex; align-items: center; gap: 0.625rem; }
.step-circle { width: 28px; height: 28px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 0.75rem; border: 2px solid var(--gray-200); color: var(--gray-400); background: var(--theme-bg-elevated); transition: all 0.3s; flex-shrink: 0; }
.step-circle.active { border-color: var(--primary); color: var(--theme-on-primary); background: var(--primary); box-shadow: 0 0 0 4px var(--theme-primary-soft); }
.step-circle.completed { border-color: var(--success); color: var(--theme-on-primary); background: var(--success); }
.step-label { font-size: 0.875rem; color: var(--gray-400); font-weight: 500; white-space: nowrap; }
.step-label.active { color: var(--gray-800); font-weight: 600; }
.step-connector { width: 28px; height: 2px; background: var(--gray-200); margin: 0 0.625rem; border-radius: 1px; flex-shrink: 0; }
.step-connector.completed { background: var(--success); }

.prep-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 0.875rem 1rem; box-shadow: var(--shadow-sm); margin-bottom: 0.75rem; border: 1px solid var(--gray-100); }
.prep-card-title { font-size: 0.9375rem; font-weight: 600; color: var(--gray-900); margin-bottom: 0.625rem; display: flex; align-items: center; gap: 0.5rem; }
.prep-card-title .step-badge { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: var(--radius-full); background: var(--primary-bg); color: var(--primary); font-size: 0.75rem; font-weight: 700; }

.device-check-list { display: grid; gap: 0.375rem; }
.device-item { display: flex; align-items: center; justify-content: space-between; padding: 0.5rem 0.75rem; background: var(--gray-50); border: 1px solid var(--gray-100); border-radius: var(--radius-md); transition: all 0.2s; }
.device-item:hover { border-color: var(--gray-200); }
.device-info { display: flex; align-items: center; gap: 0.875rem; }
.device-icon { width: 32px; height: 32px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 1rem; background: var(--theme-bg-elevated); border: 1px solid var(--gray-100); }
.device-name { font-weight: 500; color: var(--gray-800); font-size: 0.9375rem; }
.device-status { display: flex; align-items: center; gap: 0.625rem; }
.status-badge { padding: 0.25rem 0.75rem; border-radius: var(--radius-full); font-size: 0.75rem; font-weight: 500; }
.status-badge.ok { background: var(--success-bg); color: var(--success); }
.status-badge.checking { background: var(--warning-bg); color: var(--warning); }
.status-badge.error { background: var(--error-bg); color: var(--error); }
.status-badge.warn { background: var(--warning-bg); color: var(--warning); border: 1px solid var(--warning); }

/* ==================== v11.90 V2：环境噪声检测结果 ==================== */
.noise-result { margin-top: 0.375rem; font-size: 0.8125rem; padding: 0.5rem 0.75rem; border-radius: var(--radius-md); background: var(--success-bg); color: var(--success); }
.noise-result.noisy { background: var(--warning-bg); color: var(--warning); border-left: 3px solid var(--warning); }

/* ==================== v11.90 V2：岗位要求 JD 输入 ==================== */
.jd-item { margin-top: 0.75rem; }
.jd-textarea { width: 100%; min-height: 96px; padding: 0.625rem 0.875rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); font-size: 0.875rem; line-height: 1.6; resize: vertical; background: var(--theme-surface); color: var(--gray-800); font-family: var(--font-sans); }
.jd-textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-bg); }
.jd-counter { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; font-size: 0.75rem; color: var(--gray-500); margin-top: 0.25rem; }
.jd-counter-actions { display: inline-flex; align-items: center; gap: 0.5rem; }
.jd-inline-btn { padding: 0.125rem 0.5rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-sm); font-size: 0.75rem; color: var(--gray-600); cursor: pointer; transition: all 0.2s; }
.jd-inline-btn:hover { border-color: var(--primary); color: var(--primary); }
/* v13.37：岗位模板回填来源标记 */
.jd-source-badge { margin-left: 0.5rem; padding: 0.0625rem 0.5rem; border-radius: var(--radius-full); background: var(--primary-bg); color: var(--primary); font-size: 0.6875rem; font-weight: 500; }
/* v13.37：岗位下拉加载失败/为空的提示 */
.config-hint { margin-top: 0.375rem; font-size: 0.75rem; line-height: 1.5; color: var(--warning); }

/* ==================== v11.90 V2：高级设置折叠 ==================== */
.advanced-card { padding: 0; overflow: hidden; }
.advanced-toggle { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 0.875rem 1.25rem; background: transparent; border: none; cursor: pointer; font-size: 0.9375rem; font-weight: 600; color: var(--gray-800); }
.advanced-toggle-title { display: flex; align-items: center; gap: 0.5rem; }
.advanced-toggle-state { font-size: 0.8125rem; font-weight: 500; color: var(--primary); }
.advanced-body { padding: 0.25rem 1.25rem 1.25rem; border-top: 1px dashed var(--gray-200); }

/* ==================== v11.90 V2：准备进度覆盖层 ==================== */
.prepare-overlay { position: fixed; inset: 0; z-index: 1200; background: rgba(0, 0, 0, 0.55); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; padding: 1.5rem; }
.prepare-modal { width: 100%; max-width: 420px; background: var(--theme-surface); border-radius: var(--radius-xl); box-shadow: var(--shadow-xl); padding: 1.75rem 1.5rem; }
.prepare-title { font-size: 1.0625rem; font-weight: 700; color: var(--gray-900); margin-bottom: 1rem; text-align: center; }
.prepare-track { height: 8px; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; margin-bottom: 1.125rem; }
.prepare-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-full); transition: width 0.5s ease; }
.prepare-steps-list { display: flex; flex-direction: column; gap: 0.375rem; margin-bottom: 1rem; }
.prepare-step-row { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; color: var(--gray-500); padding: 0.375rem 0.5rem; border-radius: var(--radius-sm); }
.prepare-step-row.done { color: var(--gray-600); }
.prepare-step-row.current { color: var(--gray-900); background: var(--primary-bg); font-weight: 600; }
.prepare-eta { font-size: 0.8125rem; color: var(--gray-500); text-align: center; }

/* ==================== v11.90 V2：报告三段式（面试者简介 + 岗位信息） ==================== */
.report-profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.profile-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); padding: 1.125rem 1.25rem; }
.profile-card-title { font-size: 0.9375rem; font-weight: 700; color: var(--gray-900); margin-bottom: 0.875rem; }
.candidate-name-row { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; margin-bottom: 0.5rem; }
.candidate-name { font-size: 1.125rem; font-weight: 700; color: var(--gray-900); }
.candidate-ai-score { font-size: 0.75rem; font-weight: 600; padding: 0.25rem 0.625rem; border-radius: var(--radius-full); background: var(--primary-bg); color: var(--primary); white-space: nowrap; }
.candidate-skills-row { display: flex; flex-wrap: wrap; gap: 0.375rem; margin-bottom: 0.625rem; }
.candidate-skill { font-size: 0.75rem; padding: 0.1875rem 0.5rem; border-radius: var(--radius-full); background: var(--gray-50); color: var(--gray-600); border: 1px solid var(--gray-200); }
.candidate-intro-block { margin-top: 0.5rem; }
.intro-block-label { display: inline-block; font-size: 0.75rem; font-weight: 600; color: var(--gray-500); margin-bottom: 0.25rem; }
.intro-block-text { font-size: 0.8438rem; line-height: 1.7; color: var(--gray-600); margin: 0; white-space: pre-wrap; word-break: break-word; }
.intro-block-text.jd-text { max-height: 132px; overflow-y: auto; }
.job-position { font-size: 1.0625rem; font-weight: 700; color: var(--gray-900); margin-bottom: 0.375rem; }
.job-match-row { display: flex; align-items: center; gap: 0.625rem; margin-top: 0.875rem; }
.match-label { font-size: 0.8125rem; color: var(--gray-500); white-space: nowrap; }
.match-rate { font-size: 1.125rem; font-weight: 700; }
.match-bar { flex: 1; height: 8px; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; }
.match-fill { height: 100%; background: var(--primary-gradient); border-radius: var(--radius-full); }

/* ==================== v11.90 V2：综合得分等级徽章 + 概要入口 ==================== */
.score-level-badge { display: inline-block; margin-top: 0.375rem; padding: 0.1875rem 0.75rem; border-radius: var(--radius-full); font-size: 0.75rem; font-weight: 600; }
.score-level-badge.excellent { background: var(--success-bg); color: var(--success); }
.score-level-badge.good { background: var(--info-bg); color: var(--info); }
.score-level-badge.pass { background: var(--warning-bg); color: var(--warning); }
.score-level-badge.weak { background: var(--error-bg); color: var(--error); }
.detail-entry-btn { width: 100%; margin-top: 0.25rem; }

/* ==================== v11.97：报告总览横幅卡（综合分 + 等级 + LLM 总评 + 岗位匹配） ==================== */
.report-banner-card { display: flex; align-items: center; gap: 1.5rem; background: var(--theme-bg-elevated); border: 1px solid var(--gray-100); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); padding: 1.5rem 1.75rem; margin-bottom: 1.25rem; flex-wrap: wrap; }
.banner-score { display: flex; flex-direction: column; align-items: center; justify-content: center; min-width: 108px; padding: 0.5rem 0.75rem; background: var(--gray-50); border-radius: var(--radius-md); }
.banner-score-value { font-size: 2.5rem; font-weight: 800; line-height: 1.1; }
.banner-score-label { font-size: 0.75rem; color: var(--gray-400); margin-top: 0.25rem; }
.banner-score .score-level-badge { margin-top: 0.5rem; }
.banner-divider { width: 1px; align-self: stretch; background: var(--gray-100); }
.banner-main { flex: 1; min-width: 240px; }
.banner-comment { font-size: 0.9375rem; line-height: 1.8; color: var(--gray-600); }
.banner-match-reason { margin-top: 0.75rem; font-size: 0.8438rem; line-height: 1.65; color: var(--gray-500); background: var(--gray-50); border-left: 3px solid var(--primary); border-radius: var(--radius-md); padding: 0.5rem 0.875rem; }
.banner-match { display: flex; flex-direction: column; gap: 0.375rem; min-width: 168px; max-width: 220px; padding: 0.5rem 0.875rem; background: var(--gray-50); border-radius: var(--radius-md); }
.banner-match .match-rate { font-size: 1.5rem; }

/* ==================== v11.90 V2：问题分析 Tab 薄弱点区块 ==================== */
.weak-points-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); padding: 1rem 1.25rem; margin-bottom: 1.25rem; }
.weak-point-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.625rem 0.5rem; border-bottom: 1px dashed var(--gray-200); }
.weak-point-row:last-child { border-bottom: none; }
.weak-point-title { font-size: 0.875rem; font-weight: 600; color: var(--gray-800); }
.weak-point-quote { font-size: 0.8125rem; color: var(--gray-500); margin-top: 0.125rem; }
.test-btn { padding: 0.375rem 0.875rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); color: var(--gray-600); border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; font-weight: 500; transition: all 0.2s; }
.test-btn:hover:not(:disabled) { background: var(--primary); color: var(--theme-on-primary); border-color: var(--primary); }
.test-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.device-tip { margin-top: 0.75rem; font-size: 0.8125rem; color: var(--gray-400); padding: 0.625rem 0.875rem; background: var(--gray-50); border-radius: var(--radius-md); border-left: 3px solid var(--warning); }
.device-select { max-width: 180px; padding: 0.375rem 1.75rem 0.375rem 0.625rem; border: 1px solid var(--gray-200); border-radius: var(--radius-sm); font-size: 0.75rem; color: var(--gray-600); background: var(--theme-bg-elevated); outline: none; cursor: pointer; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 8px center; transition: border-color 0.2s; }
.device-select:focus { border-color: var(--primary); }
.mic-level-row { display: flex; align-items: center; gap: 0.625rem; margin-top: 0.5rem; padding-left: 2.5rem; }
.mic-level-bar { flex: 1; max-width: 280px; height: 6px; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; }
.mic-level-fill { height: 100%; background: var(--success); border-radius: var(--radius-full); transition: width 0.1s linear; }
.mic-level-text { font-size: 0.75rem; color: var(--gray-500); }

.section-label { font-size: 0.8125rem; font-weight: 600; color: var(--gray-700); margin-bottom: 0.5rem; display: flex; align-items: center; gap: 0.375rem; }


/* ==================== V10.3 简历库选择器 ==================== */
.resume-empty { border: 2px dashed var(--gray-200); border-radius: var(--radius-md); padding: 1.25rem; text-align: center; background: var(--gray-50); }
.empty-icon { font-size: 1.5rem; margin-bottom: 0.375rem; }
.empty-title { font-size: 0.9375rem; font-weight: 600; color: var(--gray-700); margin-bottom: 0.375rem; }
.empty-desc { font-size: 0.8125rem; color: var(--gray-400); margin-bottom: 1rem; }
.empty-actions { display: flex; justify-content: center; gap: 0.625rem; }



.resume-manage-row { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.625rem; flex-wrap: wrap; }
/* v11.89：简历收起/展开面板 */
.resume-collapse-btn { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.5625rem 0.875rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); background: var(--theme-bg-elevated); cursor: pointer; font-size: 0.8125rem; color: var(--gray-800); transition: border-color 0.2s; }
.resume-collapse-btn:hover { border-color: var(--primary); }
.resume-collapse-title { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resume-collapse-state { display: inline-flex; align-items: center; gap: 0.25rem; font-size: 0.6875rem; color: var(--gray-500); white-space: nowrap; }
.resume-collapse-arrow { display: inline-block; transition: transform 0.2s; }
.resume-collapse-arrow.open { transform: rotate(180deg); }
.resume-panel { margin-top: 0.5rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); padding: 0.5rem; background: var(--gray-50); display: flex; flex-direction: column; gap: 0.375rem; }
.resume-option-row { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.5rem 0.75rem; border: 1px solid transparent; border-radius: var(--radius-sm); cursor: pointer; transition: all 0.15s; }
.resume-option-row:hover { border-color: var(--primary-border); background: var(--theme-bg-elevated); }
.resume-option-row.active { border-color: var(--primary); background: var(--theme-bg-elevated); }
.resume-option-title { font-size: 0.8125rem; color: var(--gray-800); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.resume-option-meta { font-size: 0.6875rem; color: var(--gray-400); white-space: nowrap; }
.resume-manage-hint { font-size: 0.75rem; color: var(--gray-400); }
.resume-action-btn.primary { border-color: var(--primary); color: var(--primary); }

.custom-position-item { margin-top: 0.75rem; }
.custom-position-wrap { display: flex; align-items: center; gap: 0.625rem; }
.custom-position-input { flex: 1; padding: 0.5rem 0.75rem; border: 1px solid var(--gray-200); border-radius: var(--radius-sm); font-size: 0.8125rem; color: var(--gray-700); background: var(--theme-bg-elevated); outline: none; transition: border-color 0.2s; }
.custom-position-input:focus { border-color: var(--primary); }
.custom-position-counter { font-size: 0.6875rem; color: var(--gray-400); white-space: nowrap; }
.resume-parsing-msg { margin: 0.5rem 0; font-size: 0.8125rem; color: var(--primary); }
.hidden-file-input { display: none; }
.asr-live { border-color: var(--primary) !important; box-shadow: 0 0 0 2px var(--theme-primary-soft); }

.resume-action-btn { padding: 0.375rem 0.75rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-sm); cursor: pointer; font-size: 0.75rem; color: var(--gray-600); transition: all 0.2s; white-space: nowrap; }
.resume-action-btn:hover { border-color: var(--primary); color: var(--primary); }

.config-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 0.75rem; }
.agent-hint { margin-top: 0.625rem; font-size: 0.75rem; color: var(--gray-500); padding: 0.5rem 0.75rem; background: var(--gray-50); border-radius: var(--radius-md); border-left: 3px solid var(--primary); }
.config-item { display: flex; flex-direction: column; gap: 0.375rem; }
.config-label { font-size: 0.8125rem; font-weight: 500; color: var(--gray-600); }
.config-select { padding: 0.5rem 0.75rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); font-size: 0.8125rem; background: var(--theme-bg-elevated); cursor: pointer; color: var(--gray-700); transition: all 0.2s; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 12px center; }
.config-select:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--theme-primary-soft); }

.toggle-row { display: flex; align-items: center; justify-content: space-between; padding: 0.625rem 1rem; background: var(--gray-50); border-radius: var(--radius-md); margin-top: 0.5rem; border: 1px solid var(--gray-100); }
.toggle-label { font-size: 0.875rem; color: var(--gray-600); }
.toggle-switch { position: relative; width: 44px; height: 24px; flex-shrink: 0; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; cursor: pointer; inset: 0; background: var(--gray-300); border-radius: var(--radius-full); transition: 0.25s; }
.toggle-slider::before { content: ''; position: absolute; height: 18px; width: 18px; left: 3px; bottom: 3px; background: var(--theme-bg-elevated); border-radius: var(--radius-full); transition: 0.25s; box-shadow: var(--shadow-sm); }
.toggle-switch input:checked + .toggle-slider { background: var(--primary); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(20px); }

.start-btn { width: 100%; padding: 0.875rem; background: var(--primary-gradient); color: var(--theme-on-primary); border: none; border-radius: var(--radius-lg); font-size: 0.9375rem; font-weight: 600; cursor: pointer; transition: all 0.3s; box-shadow: 0 4px 14px var(--theme-primary-soft); margin-top: 1.25rem; letter-spacing: 0.02em; }
.start-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px var(--theme-primary-soft-hover); }
.start-btn:active { transform: translateY(0); }
.start-btn:disabled { opacity: 0.7; cursor: not-allowed; }

/* ========== 面试进行页 ========== */
.interview-page { background: var(--theme-bg); min-height: 100vh; display: flex; flex-direction: column; }
.top-bar { background: var(--theme-bg-elevated); padding: 0.75rem 0; display: flex; align-items: center; justify-content: space-between; box-shadow: var(--shadow-sm); border-bottom: 1px solid var(--gray-200); position: sticky; top: 0; z-index: 10; }
.top-bar-left { display: flex; align-items: center; gap: 1rem; }
.top-bar-logo { font-size: 1.125rem; font-weight: 700; color: var(--primary); display: flex; align-items: center; gap: 0.375rem; }
.timer-display { display: flex; align-items: center; gap: 0.375rem; padding: 0.375rem 0.875rem; background: var(--gray-50); border: 1px solid var(--gray-200); border-radius: var(--radius-full); font-family: var(--font-mono); font-size: 0.875rem; color: var(--gray-600); }
.timer-display.urgent { background: var(--warning-bg, #fef3c7); border-color: var(--warning, #d97706); color: var(--warning, #b45309); font-weight: 600; }
.top-bar-right { display: flex; align-items: center; gap: 0.625rem; }
.control-btn { padding: 0.4375rem 0.875rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); font-weight: 500; }
.control-btn:hover:not(:disabled) { background: var(--gray-50); border-color: var(--gray-300); }
.control-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.control-btn.danger { border-color: var(--primary-border); color: var(--primary); }
.control-btn.danger:hover { background: var(--primary-bg); }
.interview-main { display: grid; grid-template-columns: 220px 1fr 260px; gap: 1rem; padding: 1rem 0; flex: 1; width: 100%; }

.left-panel { display: flex; flex-direction: column; gap: 1rem; }
.interviewer-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.5rem; text-align: center; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.interviewer-avatar { width: 100px; height: 100px; border-radius: var(--radius-full); background: var(--primary-gradient); margin: 0 auto 0.875rem; display: flex; align-items: center; justify-content: center; font-size: 2.5rem; position: relative; }
.interviewer-avatar::after { content: ''; position: absolute; inset: -4px; border-radius: var(--radius-full); border: 3px solid var(--success); animation: pulse-ring 2s ease-in-out infinite; }
@keyframes pulse-ring { 0%,100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.05); } }
.interviewer-name { font-size: 0.9375rem; font-weight: 600; color: var(--gray-800); margin-bottom: 0.375rem; }
.interviewer-meta { display: flex; flex-direction: column; align-items: center; gap: 0.375rem; }
.progress-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.25rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.progress-title { font-size: 0.8125rem; font-weight: 600; color: var(--gray-500); margin-bottom: 0.875rem; text-transform: uppercase; letter-spacing: 0.05em; }
.progress-timeline { display: flex; flex-direction: column; gap: 0.25rem; }
.progress-item { display: flex; align-items: center; gap: 0.625rem; padding: 0.5rem 0.625rem; border-radius: var(--radius-md); font-size: 0.8125rem; cursor: pointer; transition: all 0.2s; color: var(--gray-500); }
.progress-item:hover { background: var(--gray-50); }
.progress-item.active { background: var(--primary-bg); color: var(--primary); font-weight: 600; }
.progress-dot { width: 22px; height: 22px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font-size: 0.6875rem; font-weight: 600; flex-shrink: 0; }
.progress-item.completed .progress-dot { background: var(--success); color: var(--theme-on-primary); }
.progress-item.active .progress-dot { background: var(--primary); color: var(--theme-on-primary); animation: pulse-ring 2s ease-in-out infinite; }
.progress-item.pending .progress-dot { background: var(--gray-200); color: var(--gray-400); }

.center-panel { display: flex; flex-direction: column; background: var(--theme-bg-elevated); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); overflow: hidden; border: 1px solid var(--gray-100); }
.chat-header { padding: 0.875rem 1.25rem; border-bottom: 1px solid var(--gray-100); display: flex; align-items: center; justify-content: space-between; }
.chat-header-title { font-size: 0.875rem; font-weight: 600; color: var(--gray-700); }
.chat-status { display: flex; align-items: center; gap: 0.375rem; font-size: 0.75rem; color: var(--success); font-weight: 500; }
.chat-status-dot { width: 7px; height: 7px; background: var(--success); border-radius: var(--radius-full); animation: pulse-dot 1.5s ease-in-out infinite; }
@keyframes pulse-dot { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }
.chat-body { flex: 1; overflow-y: auto; padding: 1.25rem; display: flex; flex-direction: column; gap: 1rem; max-height: 60vh; }
.message { display: flex; gap: 0.625rem; max-width: 85%; animation: msg-in 0.3s ease; }
@keyframes msg-in { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.message.ai, .message.question { align-self: flex-start; }
.message.question { max-width: 92%; }
.message.user { align-self: flex-end; flex-direction: row-reverse; }
.message-avatar { width: 32px; height: 32px; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; font-size: 0.875rem; flex-shrink: 0; }
.message.ai .message-avatar, .message.question .message-avatar { background: var(--primary-bg); color: var(--primary); }
.message.user .message-avatar { background: var(--success-bg); color: var(--success); }
.message-content { padding: 0.875rem 1rem; border-radius: var(--radius-lg); font-size: 0.9375rem; line-height: 1.65; }
.message.ai .message-content { background: var(--ai-bubble); border: 1px solid var(--ai-bubble-border); border-top-left-radius: var(--radius-sm); }
.message.user .message-content { background: var(--user-bubble); border: 1px solid var(--user-bubble-border); border-top-right-radius: var(--radius-sm); }
.message.question .message-content { background: var(--question-bubble); border: 1px solid var(--question-bubble-border); border-left: 4px solid var(--warning); }
.question-header { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.5rem; font-size: 0.75rem; color: var(--warning); font-weight: 600; }
.ai-tag { font-size: 0.6875rem; color: var(--primary); font-weight: 600; margin-bottom: 0.375rem; display: inline-block; padding: 0.125rem 0.5rem; background: var(--primary-bg); border-radius: var(--radius-full); }

/* --- V11.0 流式打字机光标 --- */
.streaming-cursor { display: inline-block; width: 2px; height: 1em; margin-left: 2px; vertical-align: text-bottom; background: var(--primary); animation: cursor-blink 0.8s steps(1) infinite; }
@keyframes cursor-blink { 50% { opacity: 0; } }

/* --- V11.0 agent 选择提示 --- */
.agent-hint { margin-top: 0.625rem; font-size: 0.75rem; line-height: 1.5; color: var(--gray-400); background: var(--primary-bg); border-radius: var(--radius-md); padding: 0.5rem 0.75rem; }

/* --- V11.0 报告 AI 深度复盘 --- */
.deep-review { margin-top: 1.25rem; background: var(--theme-bg-elevated); border: 1px solid var(--gray-100); border-radius: var(--radius-lg); padding: 1rem 1.25rem; box-shadow: var(--shadow-sm); }
.deep-review-title { font-size: 0.8125rem; font-weight: 700; color: var(--gray-600); margin-bottom: 0.75rem; }
.deep-review-row { display: flex; align-items: flex-start; gap: 0.75rem; padding: 0.375rem 0; font-size: 0.8125rem; }
.deep-review-label { flex-shrink: 0; width: 4.5rem; color: var(--gray-400); font-weight: 600; }
.deep-review-value { color: var(--gray-600); font-weight: 600; }
.sentiment-track { display: inline-flex; align-items: center; gap: 0.375rem; }
.sentiment-dot { width: 12px; height: 12px; border-radius: 50%; flex-shrink: 0; }
.sentiment-dot.nervous { background: #f59e0b; }
.sentiment-dot.confident { background: #10b981; }
.sentiment-dot.hesitant { background: #6366f1; }
.sentiment-dot.calm { background: #0ea5e9; }
.deep-review-flags { display: flex; flex-direction: column; gap: 0.25rem; }
.redflag-item { color: var(--gray-600); font-size: 0.75rem; }
.message-text { white-space: pre-wrap; word-break: break-word; }

.chat-input-area { padding: 1rem 1.25rem; border-top: 1px solid var(--gray-100); background: var(--theme-bg-elevated); }
.input-timer { display: flex; align-items: center; gap: 0.375rem; font-size: 0.75rem; color: var(--warning); margin-bottom: 0.5rem; font-weight: 500; }
.interim-hint { color: var(--gray-400); font-weight: 400; }
.input-container { display: flex; align-items: flex-end; gap: 0.625rem; }
.input-textarea { flex: 1; padding: 0.75rem 1rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); font-size: 0.9375rem; font-family: var(--font-sans); resize: none; min-height: 48px; max-height: 120px; transition: all 0.2s; line-height: 1.5; }
.input-textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--theme-primary-soft); }
.input-textarea:disabled { background: var(--gray-50); cursor: not-allowed; }
.mic-btn { width: 48px; height: 48px; border-radius: var(--radius-full); background: var(--primary-gradient); color: var(--theme-on-primary); border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 1.25rem; transition: all 0.2s; flex-shrink: 0; box-shadow: 0 2px 8px var(--theme-primary-soft-hover); }
.mic-btn:hover:not(:disabled) { transform: scale(1.05); box-shadow: 0 4px 12px var(--theme-primary-soft-hover); }
.mic-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.mic-btn.recording { background: var(--error); animation: pulse-ring 1.5s ease-in-out infinite; }
.input-actions { display: flex; gap: 0.5rem; margin-top: 0.625rem; }
.action-btn { padding: 0.4375rem 0.875rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); font-weight: 500; }
.action-btn:hover:not(:disabled) { background: var(--gray-50); }
.action-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.action-btn.primary { background: var(--primary); color: var(--theme-on-primary); border-color: var(--primary); }
.action-btn.primary:hover:not(:disabled) { background: var(--primary-dark); }

.right-panel { display: flex; flex-direction: column; gap: 1rem; }
.analysis-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.25rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.analysis-card-title { font-size: 0.8125rem; font-weight: 600; color: var(--gray-500); margin-bottom: 0.875rem; display: flex; align-items: center; gap: 0.375rem; text-transform: uppercase; letter-spacing: 0.05em; }
.radar-chart { width: 100%; aspect-ratio: 1; max-width: 200px; margin: 0 auto; }
/* v11.89：六维评分标准图例（逐维说明评分依据 + 实时得分） */
.dimension-legend { display: flex; flex-direction: column; gap: 0.25rem; margin-top: 0.625rem; }
.dimension-legend-row { display: flex; align-items: center; gap: 0.5rem; padding: 0.25rem 0.375rem; border-radius: var(--radius-sm); }
.dimension-legend-row:hover { background: var(--gray-50); }
.legend-label { font-size: 0.75rem; font-weight: 600; color: var(--gray-700); white-space: nowrap; }
.legend-score { min-width: 2rem; text-align: center; font-size: 0.6875rem; font-weight: 700; border-radius: var(--radius-full); padding: 0.0625rem 0.375rem; }
.legend-score.low { background: var(--error-bg); color: var(--error); }
.legend-score.medium { background: var(--warning-bg); color: var(--warning); }
.legend-score.high { background: var(--success-bg); color: var(--success); }
.legend-desc { font-size: 0.6875rem; color: var(--gray-500); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.legend-tip { font-size: 0.6875rem; color: var(--gray-400); padding: 0.25rem 0.375rem; }
.hint-text { font-size: 0.8125rem; color: var(--gray-500); line-height: 1.65; white-space: pre-wrap; }

/* ========== 复盘报告页 ========== */
.report-page { background: var(--theme-bg); min-height: 100vh; padding: 2rem 0; }
.report-container { width: 100%; }
.report-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.75rem; }
.report-title { font-size: 1.375rem; font-weight: 700; color: var(--gray-900); display: flex; align-items: center; gap: 0.5rem; }
.report-loading { font-size: 0.875rem; color: var(--gray-400); font-weight: 400; }
.back-btn { padding: 0.5rem 1rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-md); cursor: pointer; font-size: 0.875rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); }
.back-btn:hover { background: var(--gray-50); border-color: var(--gray-300); }

/* V10.4 报告元信息条 */
.report-meta-bar { display: flex; flex-wrap: wrap; gap: 0.625rem; margin-bottom: 1.5rem; padding: 0.875rem 1.125rem; background: var(--theme-bg-elevated); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.meta-chip { display: flex; align-items: center; gap: 0.375rem; padding: 0.3125rem 0.75rem; background: var(--gray-50); border-radius: var(--radius-full); font-size: 0.8125rem; }
.meta-label { color: var(--gray-500); }
.meta-value { color: var(--gray-900); font-weight: 600; max-width: 10rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* v11.88 V2：批量分析进度条（结束后轮询驱动） */
.report-analysis-progress { margin-bottom: 1.5rem; padding: 1rem 1.25rem; background: var(--theme-bg-elevated); border: 1px solid var(--gray-100); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.rap-header { display: flex; align-items: center; gap: 0.625rem; margin-bottom: 0.75rem; }
.rap-spinner { width: 1rem; height: 1rem; border: 2px solid var(--theme-primary-soft); border-top-color: var(--theme-primary); border-radius: var(--radius-full); animation: rap-spin 0.8s linear infinite; flex-shrink: 0; }
@keyframes rap-spin { to { transform: rotate(360deg); } }
.rap-step { font-size: 0.9375rem; font-weight: 600; color: var(--gray-900); }
.rap-num { margin-left: auto; font-size: 0.875rem; font-weight: 600; color: var(--theme-primary); font-variant-numeric: tabular-nums; }
.rap-track { height: 0.5rem; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; }
.rap-fill { height: 100%; background: linear-gradient(90deg, var(--theme-primary), var(--theme-accent-hover, var(--theme-primary))); border-radius: var(--radius-full); transition: width 0.6s ease; }
.rap-tip { margin-top: 0.625rem; font-size: 0.75rem; color: var(--gray-400); }

.report-tabs { display: flex; gap: 0.25rem; margin-bottom: 1.75rem; background: var(--theme-bg-elevated); padding: 0.375rem; border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); width: fit-content; }
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
.replay-row.interviewer .replay-avatar { background: var(--primary); color: var(--theme-on-primary); }
.replay-bubble { max-width: 82%; padding: 0.75rem 1rem; border-radius: var(--radius-md); font-size: 0.875rem; line-height: 1.6; }
.replay-bubble.ai { background: var(--gray-50); border: 1px solid var(--gray-100); border-top-left-radius: var(--radius-xs); }
.replay-bubble.user { background: var(--primary-bg); border: 1px solid var(--theme-accent-hover); border-top-right-radius: var(--radius-xs); }
.replay-bubble.feedback { background: var(--info-bg); border: 1px solid rgba(37, 99, 235, 0.15); border-top-left-radius: var(--radius-xs); }
.replay-idx { font-size: 0.75rem; color: var(--gray-500); font-weight: 600; margin-bottom: 0.375rem; }
.replay-tag { display: inline-block; margin-left: 0.375rem; padding: 0.0625rem 0.4375rem; border-radius: var(--radius-full); background: var(--warning); color: var(--theme-on-primary); font-size: 0.6875rem; font-weight: 500; }
.replay-score { font-size: 0.75rem; color: var(--primary); font-weight: 600; margin-bottom: 0.375rem; }
.replay-feedback-title { font-size: 0.75rem; color: var(--info); font-weight: 600; margin-bottom: 0.375rem; }
.dialog-empty { text-align: center; padding: 3rem 1rem; color: var(--gray-400); font-size: 0.9375rem; }
.tab-content.active { display: block; animation: msg-in 0.3s ease; }

.summary-grid { display: grid; grid-template-columns: 1fr 2fr 1fr; gap: 1.5rem; }
.summary-left, .summary-right { display: flex; flex-direction: column; gap: 1rem; }
.summary-center { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.75rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.summary-title { font-size: 1.125rem; font-weight: 600; margin-bottom: 1rem; color: var(--gray-800); }
.summary-subtitle { font-size: 1rem; font-weight: 600; margin-bottom: 1rem; color: var(--gray-800); }
.summary-paragraph { font-size: 0.9375rem; line-height: 1.85; color: var(--gray-600); }
.total-score-box { display: flex; flex-direction: column; align-items: center; margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--gray-100); }
.total-score-label { font-size: 0.75rem; color: var(--gray-400); margin-bottom: 0.25rem; }
.total-score-value { font-size: 1.75rem; font-weight: 800; }
.pros-cons-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.25rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.pros-cons-title { font-size: 0.875rem; font-weight: 600; margin-bottom: 0.875rem; display: flex; align-items: center; gap: 0.375rem; }
.pros-cons-title.cons { color: var(--error); }
.pros-cons-title.pros { color: var(--success); }
.cons-item, .pros-item { padding: 0.875rem; background: var(--gray-50); border-radius: var(--radius-md); margin-bottom: 0.5rem; cursor: pointer; transition: all 0.2s; border: 1px solid transparent; }
.cons-item:hover, .pros-item:hover { background: var(--theme-bg-elevated); border-color: var(--gray-200); box-shadow: var(--shadow-sm); }
.cons-item-title, .pros-item-title { font-size: 0.8125rem; font-weight: 500; color: var(--gray-800); margin-bottom: 0.375rem; }
.cons-item-quote, .pros-item-quote { font-size: 0.75rem; color: var(--gray-400); font-style: italic; padding-left: 0.75rem; border-left: 2px solid var(--gray-200); line-height: 1.5; }

.analysis-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.25rem; }
.analysis-item { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.5rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.analysis-item-header { display: flex; align-items: center; gap: 0.625rem; margin-bottom: 0.875rem; }
.analysis-item-icon { width: 32px; height: 32px; border-radius: var(--radius-md); display: flex; align-items: center; justify-content: center; font-size: 0.875rem; }
.analysis-item-icon.weak { background: var(--error-bg); color: var(--error); }
.analysis-item-icon.strong { background: var(--success-bg); color: var(--success); }
.analysis-item-title { font-size: 0.9375rem; font-weight: 600; color: var(--gray-800); }
.analysis-item-content { font-size: 0.875rem; color: var(--gray-500); line-height: 1.65; margin-bottom: 1rem; }
.analysis-user-answer { font-size: 0.8125rem; color: var(--gray-500); background: var(--gray-50); padding: 0.625rem 0.875rem; border-radius: var(--radius-md); margin-bottom: 1rem; line-height: 1.6; }
/* v11.97：你的回答默认 2 行截断，点击展开全文 */
.analysis-user-answer-text { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; line-clamp: 2; overflow: hidden; word-break: break-word; }
.analysis-user-answer.expanded .analysis-user-answer-text { -webkit-line-clamp: unset; line-clamp: unset; overflow: visible; }
.answer-expand-btn { margin-top: 0.375rem; border: none; background: transparent; color: var(--primary); font-size: 0.75rem; font-weight: 600; cursor: pointer; padding: 0; }
.answer-expand-btn:hover { text-decoration: underline; }
.analysis-item-score { display: flex; align-items: center; gap: 0.625rem; font-size: 0.875rem; }
.score-bar { flex: 1; height: 6px; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; }
.score-fill { height: 100%; border-radius: var(--radius-full); transition: width 0.4s ease; }
.score-fill.low { background: var(--error); }
.score-fill.medium { background: var(--warning); }
.score-fill.high { background: var(--success); }
.wrong-book-btn, .practice-btn { margin-top: 0.875rem; padding: 0.4375rem 0.875rem; border: 1px solid var(--primary-border); background: var(--primary-bg); color: var(--primary); border-radius: var(--radius-md); cursor: pointer; font-size: 0.8125rem; font-weight: 500; transition: all 0.2s; }
.wrong-book-btn:hover:not(:disabled), .practice-btn:hover { background: var(--primary); color: var(--theme-on-primary); border-color: var(--primary); }
.wrong-book-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.interviewer-analysis { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.75rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.interviewer-comment { font-size: 0.9375rem; line-height: 1.85; color: var(--gray-600); margin-bottom: 1.5rem; padding: 1.25rem; background: var(--gray-50); border-radius: var(--radius-md); border-left: 4px solid var(--primary); }
.suggestion-list { list-style: none; padding: 0; margin: 0; }
/* v11.97：概要 Tab 中栏改进建议块 */
.summary-suggestion-block { margin-top: 1.25rem; padding-top: 1.25rem; border-top: 1px dashed var(--gray-200); }
.summary-suggestion-block .practice-btn { margin-top: 0.875rem; }
/* v11.x：自我介绍独立评分卡 */
.intro-score-card { background: var(--gray-50); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 1.5rem; border: 1px solid var(--gray-100); }
.intro-score-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.intro-score-header .summary-title { margin: 0; }
.intro-score-total { font-size: 1.4rem; font-weight: 700; }
.intro-score-dims { display: grid; gap: 0.6rem; }
.intro-score-dim { display: grid; grid-template-columns: 64px 1fr 36px; align-items: center; gap: 0.75rem; }
.intro-dim-label { font-size: 0.8rem; color: var(--text-secondary); }
.intro-dim-bar { height: 8px; background: var(--gray-200); border-radius: 4px; overflow: hidden; }
.intro-dim-value { font-size: 0.8rem; font-weight: 600; text-align: right; }
.intro-score-comment { margin-top: 0.9rem; font-size: 0.85rem; color: var(--text-secondary); line-height: 1.6; }
.suggestion-item { display: flex; gap: 0.875rem; padding: 1rem; background: var(--gray-50); border-radius: var(--radius-md); margin-bottom: 0.625rem; border: 1px solid var(--gray-100); }
.suggestion-number { width: 24px; height: 24px; border-radius: var(--radius-full); background: var(--primary); color: var(--theme-on-primary); display: flex; align-items: center; justify-content: center; font-size: 0.75rem; font-weight: 600; flex-shrink: 0; }
.suggestion-content { font-size: 0.875rem; color: var(--gray-600); line-height: 1.65; }

.knowledge-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.25rem; }
.knowledge-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); padding: 1.5rem; box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); border-top: 3px solid var(--primary); transition: all 0.2s; }
.knowledge-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.knowledge-card-title { font-size: 1rem; font-weight: 600; color: var(--gray-800); margin-bottom: 0.5rem; }
.knowledge-card-desc { font-size: 0.875rem; color: var(--gray-500); line-height: 1.65; margin-bottom: 0.875rem; }
.knowledge-card-link { display: inline-flex; align-items: center; gap: 0.25rem; color: var(--primary); font-size: 0.8125rem; text-decoration: none; font-weight: 500; }
.knowledge-card-link:hover { text-decoration: underline; }

.empty-tip { font-size: 0.875rem; color: var(--gray-400); padding: 1rem; text-align: center; }

.report-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 0.875rem; margin-top: 2rem; padding: 1.5rem; background: var(--theme-bg-elevated); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.report-btn { padding: 0.625rem 1.25rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-md); cursor: pointer; font-size: 0.875rem; display: flex; align-items: center; gap: 0.375rem; transition: all 0.2s; color: var(--gray-600); font-weight: 500; }
.report-btn:hover { background: var(--gray-50); border-color: var(--gray-300); }
.report-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.report-btn.primary { background: var(--primary); color: var(--theme-on-primary); border-color: var(--primary); }
.report-btn.primary:hover { background: var(--primary-dark); }

/* Markdown 渲染适配气泡 */
.message-content :deep(.prose) { font-size: 0.9375rem; line-height: 1.65; max-width: none; }
.message-content :deep(.prose p) { margin: 0.5rem 0; }
.message-content :deep(.prose p:first-child) { margin-top: 0; }
.message-content :deep(.prose p:last-child) { margin-bottom: 0; }
.message-content :deep(.prose pre) { background: #1a1a2e; color: #e5e7eb; padding: 1rem 1.25rem; border-radius: var(--radius-md); font-family: var(--font-mono); font-size: 0.8125rem; overflow-x: auto; margin: 0.75rem 0; }
.message-content :deep(.prose pre code) { background: transparent; color: inherit; padding: 0; }
.message-content :deep(.prose code) { background: var(--theme-primary-soft); color: var(--primary-dark); padding: 0.125rem 0.375rem; border-radius: var(--radius-sm); font-size: 0.8125rem; }
.message-content :deep(.prose strong) { color: var(--gray-900); font-weight: 700; }

/* ========== 响应式 ========== */
/* 桌面三栏（>1024px）：面试页锁定视口高度不整页滚动——顶栏固定 + 三栏各自内部滚动 +
   对话区撑满剩余高度（替代原 max-height 硬编码 magic number，聆听音浪出现/题目多时不再双重滚动） */
@media (min-width: 1025px) {
  .interview-page { height: 100vh; overflow: hidden; }
  .interview-main { flex: 1; min-height: 0; }
  .left-panel { min-height: 0; overflow-y: auto; }
  .center-panel { min-height: 0; }
  .chat-body { flex: 1; min-height: 0; max-height: none; }
  .right-panel { min-height: 0; overflow-y: auto; }
  /* 倒计时行：实时识别长文本可换行，防溢出挤压 */
  .input-timer { flex-wrap: wrap; }
}
/* 平板（≤1024px）：三栏收敛为单列，对话区优先——面试官信息做顶部紧凑条，维度分析沉底 */
@media (max-width: 1024px) {
  .interview-main { grid-template-columns: 1fr; }
  .left-panel { order: -1; }
  .center-panel { order: 0; }
  .right-panel { order: 1; }
  .summary-grid { grid-template-columns: 1fr; }
  .report-profile-grid { grid-template-columns: 1fr; }
}
/* 手机（≤768px）：全面移动端适配 */
@media (max-width: 768px) {
  /* 准备页 */
  .prep-container { padding: 0.5rem 0 1rem; }
  .prep-toolbar { gap: 0.5rem; margin-bottom: 0.625rem; }
  .prep-toolbar-title { font-size: 1.125rem; }
  .prep-toolbar-actions { width: 100%; }
  .prep-ghost-btn { flex: 1; justify-content: center; font-size: 0.8125rem; padding: 0.4rem 0.5rem; }
  .prep-card { padding: 0.75rem; }
  /* 步骤条保持横向，缩小间距适配窄屏 */
  .prep-steps { gap: 0; flex-wrap: nowrap; }
  .prep-step { gap: 0.375rem; }
  .step-circle { width: 30px; height: 30px; font-size: 0.8rem; }
  .step-label { font-size: 0.75rem; }
  .step-connector { width: 24px; margin: 0 0.25rem; }
  .config-grid { grid-template-columns: 1fr; }
  /* 设备检测行：状态区（徽章+下拉+按钮）换行铺满，避免横向溢出 */
  .device-item { flex-wrap: wrap; row-gap: 0.5rem; }
  .device-status { width: 100%; flex-wrap: wrap; }
  .device-select { max-width: 100%; flex: 1; min-width: 0; }
  .resume-manage-row { flex-wrap: wrap; }
  .resume-manage-hint { width: 100%; }

  /* --- 面试页顶栏：两行布局（品牌+计时 / 四个控制按钮等宽铺满） --- */
  .top-bar { padding: 0.5rem 0; row-gap: 0.5rem; }
  .top-bar-left { width: 100%; justify-content: space-between; gap: 0.5rem; }
  .top-bar-logo { font-size: 1rem; }
  .top-bar-right { width: 100%; display: grid; grid-template-columns: repeat(4, 1fr); gap: 0.375rem; }
  .control-btn { justify-content: center; padding: 0.5rem 0.25rem; font-size: 0.75rem; gap: 0.25rem; }

  /* --- 左面板横向紧凑条：面试官卡片 + 进度时间线并排 --- */
  .interview-main { gap: 0.625rem; padding: 0.625rem 0; }
  .left-panel { flex-direction: row; align-items: stretch; gap: 0.625rem; }
  .interviewer-card { flex: 1; display: flex; flex-direction: row; align-items: center; gap: 0.75rem; text-align: left; padding: 0.75rem 0.875rem; }
  .interviewer-avatar { width: 44px; height: 44px; margin: 0; font-size: 1.375rem; flex-shrink: 0; }
  .interviewer-avatar::after { inset: -2px; border-width: 2px; }
  .interviewer-name { margin-bottom: 0; font-size: 0.875rem; }
  .interviewer-meta { align-items: flex-start; gap: 0.25rem; }
  .progress-card { flex: 1.4; min-width: 0; padding: 0.75rem 0.875rem; display: flex; flex-direction: column; justify-content: center; }
  .progress-title { display: none; }
  /* 进度时间线：纵向列表 → 横向滑动（点在上、标题在下） */
  .progress-timeline { flex-direction: row; overflow-x: auto; gap: 0.375rem; -webkit-overflow-scrolling: touch; scrollbar-width: none; }
  .progress-timeline::-webkit-scrollbar { display: none; }
  .progress-item { flex-shrink: 0; flex-direction: column; gap: 0.25rem; padding: 0.375rem 0.5rem; text-align: center; font-size: 0.6875rem; }

  /* --- 对话区：占满主视口高度，气泡放宽宽度 --- */
  .chat-header { padding: 0.625rem 0.875rem; }
  .chat-body { max-height: none; height: 46vh; min-height: 260px; padding: 0.875rem; gap: 0.75rem; }
  .message { max-width: 94%; }
  .message.question { max-width: 96%; }
  .message-content { padding: 0.625rem 0.875rem; font-size: 0.875rem; }

  /* --- 输入区：加大触控目标，textarea 16px 防 iOS 聚焦自动放大 --- */
  .chat-input-area { padding: 0.75rem; }
  .input-timer { font-size: 0.6875rem; flex-wrap: wrap; }
  .input-textarea { font-size: 16px; min-height: 52px; }
  .mic-btn { width: 52px; height: 52px; }
  .input-actions { gap: 0.375rem; }
  .action-btn { flex: 1; justify-content: center; padding: 0.625rem 0.25rem; font-size: 0.8125rem; }

  /* --- 右面板（维度分析/提示）：跟随对话区之后 --- */
  .analysis-card { padding: 1rem; }
  .radar-chart { max-width: 170px; }

  /* --- 报告页 --- */
  .report-page { padding: 1rem 0.5rem; }
  /* v11.97：横幅卡窄屏纵向堆叠（分隔线隐藏，匹配块铺满） */
  .report-banner-card { flex-direction: column; align-items: stretch; gap: 0.875rem; padding: 1.125rem; }
  .banner-score { flex-direction: row; min-width: 0; justify-content: center; gap: 0.75rem; }
  .banner-score-value { font-size: 2rem; }
  .banner-divider { display: none; }
  .banner-main { min-width: 0; }
  .banner-match { max-width: none; flex-direction: row; align-items: center; }
  .banner-match .match-bar { flex: 1; }
  .report-header { flex-wrap: wrap; gap: 0.5rem; }
  .report-header-actions { width: 100%; }
  .report-header-actions .back-btn { flex: 1; justify-content: center; }
  .report-tabs { flex-wrap: nowrap; overflow-x: auto; padding: 0.25rem; -webkit-overflow-scrolling: touch; }
  .report-tab { padding: 0.5rem 0.75rem; font-size: 0.8125rem; }
  .analysis-grid, .knowledge-grid { grid-template-columns: 1fr; }
  .report-actions { gap: 0.5rem; }
  .report-btn { flex: 1 1 calc(50% - 0.5rem); justify-content: center; }
  .replay-bubble { max-width: 88%; }
}

@media (max-width: 900px) {
  .sum-hero { flex-direction: column; align-items: stretch; gap: 1rem; }
  .sum-hero-score { text-align: center; }
  .sum-main-grid { grid-template-columns: 1fr; }
}
@media print {
  .top-bar, .chat-input-area, .report-actions, .report-tabs { display: none; }
  .report-page { background: var(--theme-bg-elevated); padding: 0; }
}
  /* ==================== v13.47 批次 2：三层五 tab ==================== */

  /* tab 分组标签（复盘 / 备战） */
  .report-tabs-group {
    align-self: center;
    padding: 0 0.5rem 0 0.25rem;
    font-size: 0.6875rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    color: var(--gray-400);
    white-space: nowrap;
    user-select: none;
  }
  .report-tabs-group:not(:first-child)::before {
    content: '';
    display: inline-block;
    width: 1px;
    height: 12px;
    margin-right: 0.5rem;
    vertical-align: -1px;
    background: var(--gray-200);
  }

  /* ==================== v13.49 批次 2：概要 tab 四区 ==================== */

  /* ---- 问题分析：按维度聚类归因横幅 ---- */
  .cluster-banner {
    padding: 0.875rem 1rem; margin-bottom: 1rem;
    border-radius: var(--radius-md);
    background: var(--info-bg, #eff6ff);
    border-left: 3px solid var(--primary, #3b82f6);
  }
  .cluster-banner-head { display: flex; align-items: center; flex-wrap: wrap; gap: 0.5rem; }
  .cluster-banner-icon { font-size: 1rem; }
  .cluster-banner-title { font-size: 0.875rem; font-weight: 700; color: var(--gray-800); }
  .cluster-dim-chip {
    padding: 0.0625rem 0.4375rem; border-radius: var(--radius-sm);
    background: var(--theme-bg-elevated); border: 1px solid var(--gray-200);
    font-size: 0.75rem; color: var(--gray-600);
  }
  .cluster-dim-chip strong { color: var(--error); font-variant-numeric: tabular-nums; }
  .cluster-banner-desc { margin-top: 0.375rem; font-size: 0.75rem; color: var(--gray-500); line-height: 1.65; }

  /* ---- A 首屏 ---- */
  .sum-hero {
    display: flex; align-items: center; gap: 1.75rem;
    padding: 1.25rem 1.5rem; margin-bottom: 1.25rem;
    background: var(--theme-bg-elevated);
    border: 1px solid var(--gray-100); border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }
  .sum-hero-score { flex-shrink: 0; }
  .sum-hero-meta { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 0.625rem; }
  .sum-hero-total { display: flex; align-items: baseline; gap: 0.25rem; }
  .sum-hero-total-value { font-size: 2.5rem; font-weight: 800; line-height: 1; font-variant-numeric: tabular-nums; }
  .sum-hero-total-unit { font-size: 0.875rem; color: var(--gray-400); }
  .sum-hero-badges { display: flex; align-items: center; flex-wrap: wrap; gap: 0.375rem; }
  .sum-hero-match { display: flex; align-items: center; gap: 0.5rem; }
  .sum-hero-match-label { flex-shrink: 0; font-size: 0.75rem; color: var(--gray-500); }
  .sum-hero-match-bar { flex: 1; max-width: 220px; }
  .sum-hero-match-value { font-size: 0.8125rem; font-weight: 700; color: var(--gray-700); font-variant-numeric: tabular-nums; }
  .sum-hero-dim-legend { display: flex; flex-wrap: wrap; gap: 0.375rem; }
  .sum-hero-dim-chip {
    padding: 0.125rem 0.4375rem; border-radius: var(--radius-sm);
    background: var(--gray-50); border: 1px solid var(--gray-100);
    font-size: 0.6875rem; color: var(--gray-500);
  }
  .sum-hero-dim-chip strong { color: var(--gray-800); font-variant-numeric: tabular-nums; }

  /* ---- B 叙事区 + C 行动区（两栏） ---- */
  .sum-main-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; margin-bottom: 1.25rem; }
  .sum-zone {
    padding: 1.25rem 1.375rem;
    background: var(--theme-bg-elevated);
    border: 1px solid var(--gray-100); border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }
  .sum-zone-title {
    font-size: 0.9375rem; font-weight: 700; color: var(--gray-800);
    margin-bottom: 0.75rem;
  }
  .sum-zone-title-spaced { margin-top: 1.5rem; }
  .sum-zone-actions { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-top: 1.25rem; }

  /* ---- D 归档区（折叠） ---- */
  .sum-archive {
    background: var(--gray-50);
    border: 1px solid var(--gray-100); border-radius: var(--radius-lg);
    overflow: hidden;
  }
  .sum-archive-toggle {
    width: 100%; display: flex; align-items: center; gap: 0.5rem;
    padding: 0.75rem 1.125rem;
    background: transparent; border: none; cursor: pointer;
    font-size: 0.8125rem; font-weight: 600; color: var(--gray-600);
    text-align: left;
  }
  .sum-archive-toggle:hover { background: var(--gray-100); }
  .sum-archive-caret { color: var(--gray-400); font-size: 0.75rem; }
  .sum-archive-hint { margin-left: auto; font-size: 0.75rem; font-weight: 400; color: var(--gray-400); }
  .sum-archive-body {
    padding: 0 1.125rem 1.125rem;
    display: flex; flex-direction: column; gap: 1rem;
  }
  .archive-block {
    padding: 1rem 1.125rem;
    background: var(--theme-bg-elevated);
    border: 1px solid var(--gray-100); border-radius: var(--radius-md);
  }
  .archive-block-title {
    display: flex; align-items: baseline; justify-content: space-between; gap: 0.75rem;
    font-size: 0.875rem; font-weight: 700; color: var(--gray-700);
    margin-bottom: 0.75rem;
  }

  /* 水平定级徽章（v13.47 批次 2） */
  .level-estimate-badge {
    margin-left: 0.375rem; padding: 0.0625rem 0.4375rem;
    border-radius: var(--radius-sm); font-size: 0.6875rem; font-weight: 700;
    border: 1px solid var(--gray-200); color: var(--gray-500); background: var(--gray-50);
    cursor: help;
  }
  .level-estimate-badge.junior { border-color: var(--warning, #f59e0b); color: var(--warning); background: var(--warning-bg, #fffbeb); }
  .level-estimate-badge.mid { border-color: var(--primary, #3b82f6); color: var(--primary, #3b82f6); background: var(--info-bg, #eff6ff); }
  .level-estimate-badge.senior { border-color: var(--success, #22c55e); color: var(--success); background: var(--success-bg); }

  /* ---- Tab「🔮 追问预测」 ---- */
  .predict-intro {
    padding: 0.875rem 1rem;
    margin-bottom: 1rem;
    border-radius: var(--radius-md);
    background: var(--gray-50);
    border-left: 3px solid var(--primary, #3b82f6);
  }
  .predict-intro-title { font-size: 0.875rem; font-weight: 700; color: var(--gray-800); }
  .predict-intro-desc { margin-top: 0.25rem; font-size: 0.75rem; color: var(--gray-500); line-height: 1.6; }
  .predict-group { margin-bottom: 1.5rem; }
  .predict-group-title {
    display: flex; align-items: baseline; flex-wrap: wrap; gap: 0.5rem;
    margin-bottom: 0.625rem; font-size: 0.875rem; font-weight: 700; color: var(--gray-800);
  }
  .predict-group-sub { font-size: 0.75rem; font-weight: 400; color: var(--gray-400); }
  .predict-list { display: flex; flex-direction: column; gap: 0.75rem; }
  .predict-card {
    padding: 0.875rem 1rem;
    border-radius: var(--radius-md);
    background: var(--theme-bg-elevated);
    border: 1px solid var(--gray-200);
  }
  /* 分组 B（未被问到）视觉加重——它是本 tab 的核心价值 */
  .predict-card.highlight { border-color: var(--warning, #f59e0b); background: var(--warning-bg, #fffbeb); }
  .predict-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 0.75rem; }
  .predict-q { flex: 1; font-size: 0.875rem; font-weight: 600; color: var(--gray-800); line-height: 1.6; }
  .predict-score {
    flex-shrink: 0; padding: 0.125rem 0.5rem; border-radius: var(--radius-full);
    font-size: 0.6875rem; font-weight: 700;
  }
  .predict-score.low { background: var(--error-bg); color: var(--error); }
  .predict-score.medium { background: var(--warning-bg); color: var(--warning); }
  .predict-score.high { background: var(--success-bg); color: var(--success); }
  .predict-tag {
    display: inline-block; margin-top: 0.375rem; padding: 0.0625rem 0.4375rem;
    border-radius: var(--radius-sm); background: var(--gray-100); color: var(--gray-600);
    font-size: 0.6875rem;
  }
  .predict-block { display: flex; gap: 0.5rem; margin-top: 0.5rem; font-size: 0.8125rem; line-height: 1.65; }
  .predict-block-label {
    flex-shrink: 0; width: 3.75rem; color: var(--gray-400); font-size: 0.75rem; padding-top: 0.0625rem;
  }
  .predict-block-text { flex: 1; color: var(--gray-600); }

  /* ---- Tab「🧭 发展方向」（v13.50 批次 3 开通） ---- */
  .insight-head-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 0.5rem; margin-top: 0.5rem; }
  .insight-stamp { font-size: 0.75rem; color: var(--gray-400); }
  .insight-stale { font-size: 0.75rem; color: var(--warning); }
  .insight-refresh {
    padding: 0.1875rem 0.625rem; border-radius: var(--radius-sm);
    border: 1px solid var(--gray-200); background: var(--theme-bg-elevated);
    color: var(--gray-600); font-size: 0.75rem; cursor: pointer;
  }
  .insight-refresh:disabled { opacity: 0.6; cursor: not-allowed; }
  .insight-error { margin-top: 0.75rem; font-size: 0.75rem; color: var(--error); }
  .insight-section { margin-bottom: 1.5rem; }
  .insight-section-title { font-size: 0.875rem; font-weight: 700; color: var(--gray-800); margin-bottom: 0.75rem; }

  .trend-list { display: flex; flex-direction: column; gap: 0.625rem; }
  .trend-card {
    padding: 0.75rem 1rem; border-radius: var(--radius-md);
    background: var(--theme-bg-elevated); border: 1px solid var(--gray-200);
  }
  .trend-card-head { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; }
  .trend-title { font-size: 0.875rem; font-weight: 600; color: var(--gray-800); }
  .trend-maturity {
    flex-shrink: 0; padding: 0.0625rem 0.4375rem; border-radius: var(--radius-sm);
    font-size: 0.6875rem; font-weight: 700;
  }
  .trend-maturity.mature { background: var(--success-bg); color: var(--success); }
  .trend-maturity.rising { background: var(--info-bg, #eff6ff); color: var(--primary, #3b82f6); }
  .trend-maturity.early { background: var(--gray-100); color: var(--gray-500); }
  .trend-detail { margin-top: 0.375rem; font-size: 0.8125rem; color: var(--gray-500); line-height: 1.65; }

  .sd-row { display: flex; align-items: flex-start; gap: 0.625rem; margin-bottom: 0.625rem; }
  .sd-label { flex-shrink: 0; width: 5rem; padding-top: 0.1875rem; font-size: 0.75rem; color: var(--gray-500); }
  .sd-tags { flex: 1; display: flex; flex-wrap: wrap; gap: 0.375rem; }
  .sd-tag { padding: 0.125rem 0.5rem; border-radius: var(--radius-full); font-size: 0.75rem; }
  .sd-tag.have { background: var(--success-bg); color: var(--success); }
  .sd-tag.miss { background: var(--warning-bg, #fffbeb); color: var(--warning); }
  .sd-empty { font-size: 0.75rem; color: var(--gray-400); }

  .action-list { display: flex; flex-direction: column; gap: 0.625rem; }
  .action-card {
    display: flex; gap: 0.75rem;
    padding: 0.875rem 1rem; border-radius: var(--radius-md);
    background: var(--theme-bg-elevated); border: 1px solid var(--gray-200);
  }
  .action-number {
    flex-shrink: 0; width: 1.375rem; height: 1.375rem; border-radius: var(--radius-full);
    background: var(--primary, #3b82f6); color: #fff;
    font-size: 0.75rem; font-weight: 700;
    display: flex; align-items: center; justify-content: center;
  }
  .action-body { flex: 1; min-width: 0; }
  .action-content { font-size: 0.8125rem; color: var(--gray-700); line-height: 1.7; }
  .action-anchor { margin-top: 0.375rem; font-size: 0.6875rem; color: var(--gray-400); }
  .insight-disclaimer {
    margin-top: 0.5rem; padding-top: 0.75rem;
    border-top: 1px dashed var(--gray-200);
    font-size: 0.6875rem; color: var(--gray-400); line-height: 1.6;
  }

  /* ---- Tab「🧭 发展方向」占位（批次 3 开通） ---- */
  .insight-head { margin-bottom: 1rem; }
  .insight-head-title { font-size: 0.9375rem; font-weight: 700; color: var(--gray-800); }
  .insight-head-desc { margin-top: 0.25rem; font-size: 0.75rem; color: var(--gray-500); line-height: 1.6; }
  .insight-placeholder {
    padding: 2.5rem 1.5rem; text-align: center;
    border: 1px dashed var(--gray-300); border-radius: var(--radius-md);
    background: var(--gray-50);
  }
  .insight-placeholder-icon { font-size: 2rem; line-height: 1; }
  .insight-placeholder-title { margin-top: 0.75rem; font-size: 0.9375rem; font-weight: 700; color: var(--gray-700); }
  .insight-placeholder-desc { margin-top: 0.375rem; font-size: 0.75rem; color: var(--gray-500); line-height: 1.7; }
  .insight-placeholder-btn {
    margin-top: 1rem; padding: 0.5rem 1.25rem; border-radius: var(--radius-md);
    border: 1px solid var(--gray-200); background: var(--gray-100); color: var(--gray-400);
    font-size: 0.8125rem; cursor: not-allowed;
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
  background: var(--theme-primary-soft);
  border: 1px solid var(--theme-accent-hover);
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
  background: var(--theme-primary-soft-hover);
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
  background: var(--theme-bg-elevated);
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
  font-size: 0.6875rem;
  color: var(--info);
  font-weight: 600;
  animation: pulse-dot 1.5s ease-in-out infinite;
}
.interviewer-state.thinking { color: var(--warning); }
</style>
