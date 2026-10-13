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
import interviewerAvatar from '@/assets/ai-interviewer.jpg';
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
import type { UserResumeVO, JobTemplateOptionVO, ResumePreviewVO } from '@/types/api';
import { useUserStore } from '@/stores/user';
import Breadcrumb from '@/components/Breadcrumb.vue';
import type {
  VoiceInterviewVO,
  VoiceInterviewReportVO,
  VoiceStartConfig,
  PointItem,
  WeakPointView,
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
  /** 评分依据说明（雷达图轴标签太短，图例区逐维解释"按什么打分"） */
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

/** 候选人心态状态标签（对齐后端 InterviewTurnResult.sentiment.state） */
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
// 报告 Tab：五 tab（对齐静态页 面试报告v2.html；按需求去掉「语音指标」与「数据接口」）
//   🎯面试背景 → 📋概览评分 → 💬面试回放 → 🔍逐题分析 → 🚀改进建议
//   （advice 内含：发展方向 + 行动建议 + 追问预警 + 7 天补强计划）
const reportTab = ref<'bg' | 'overview' | 'dialog' | 'analysis' | 'advice'>('bg');
const overviewRadarCanvas = ref<HTMLCanvasElement | null>(null);
const historyLoading = ref(false);

// 结束后批量分析进度（前端轮询 analysis 接口驱动进度条）
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

// 全场倒计时（默认 20 分钟，sys_config 可配；归零自动保存并生成报告）
const interviewRemainSec = ref(0);
let globalCountdownHandle: ReturnType<typeof setInterval> | null = null;
/** 归零待收尾标记：正在生成/作答中先补交答案，onEnd 后统一收尾 */
let timeUpPending = false;

// 作答计时（不限时、不自动提交——模拟面试允许充分思考，仅展示已用时长；
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
interface ChatMessage {
  id: string;
  role: 'question' | 'user' | 'ai';
  content: string;
  questionIdx?: number;
  total?: number;
  source?: string;
  tag?: string;
  /** 是否正在流式输出（打字机） */
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

/** 追加流式增量到 streaming 气泡（无则新建） */
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

/** 结束流式气泡（data 事件到达后调用；delta 未输出过则静默移除占位） */
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
  // AI 面试官为男性形象「林川」：优先选系统男声（云希/云健/云扬等），命中即用；
  // 无男声时回落默认中文声并压低音调，尽量贴近男声播报
  voiceNames: ['Yunxi', 'Yunjian', 'Yunyang', 'Kangkang', '云希', '云健', '云扬'],
  pitch: 0.9,
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
  // 最终识别结果增量追加到输入栏（不再覆盖用户手动编辑的内容）
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
// 岗位候选由后台【岗位模板】驱动（GET /portal/interview/jobTemplate/list），
// 岗位配置必须可在后台维护，不能写死在前端。
const DIFFICULTY_OPTIONS = [
  { label: '初级（应届/转行）', value: 'easy' as const },
  { label: '中级（1-3 年）', value: 'medium' as const },
  { label: '高级（3 年以上）', value: 'hard' as const },
];
// 题数口径=考察方向数（1 方向 = 1 主问 + 1~2 轮追问，自我介绍不计入），
// 选项从 3/5/8 扩至 5/8/12/15，匹配 20 分钟标准场的考察密度
const QUESTION_COUNT_OPTIONS = [
  { label: '5 个方向（快速版）', value: 5 },
  { label: '8 个方向（标准版）', value: 8 },
  { label: '12 个方向（深度版）', value: 12 },
  { label: '15 个方向（沉浸版）', value: 15 },
];

const config = ref<VoiceStartConfig>({
  // 岗位留空：由 loadJobTemplates() 用后台配置的**首个岗位模板**回填（清单 #1）。
  // 原实现写死 'Java 后端开发'，导致 "if (!config.value.position)" 恒为 false ——
  // 模板回填分支永不执行，且该写死值不一定对得上任何模板名。
  // 留空是安全的：handleStart() 对空岗位有明确校验与提示。
  position: '',
  difficulty: 'medium',
});
const questionCount = ref(8);
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
  // refreshDevices / requestMicPermission：设备检测的这两个方法当前页面未使用（v14.00 清理）

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

// ==================== 环境噪声检测（3 秒采样取平均，多次防误判） ====================
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

/** 岗位要求 JD（面试官提问方向与深度贴合岗位要求；后端 jobRequirements）
 *  声明提前到岗位模板逻辑之前 —— 后者在 computed 初始化期即引用本 ref，避免 TDZ。 */
const jobRequirements = ref('');

/** 岗位下拉选择值（岗位模板直选；__custom__ 展开自定义输入） */
// 清单 P1（strict）：`computed<string>({get,set})` 不匹配 computed 的重载
//（带 setter 时应由 getter 的返回类型推导），这里给 getter 标注返回类型即可。
const positionSelectValue = computed({
  get: (): string => (useCustomPosition.value ? '__custom__' : config.value.position ?? ''),
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

// ==================== 岗位模板（全 portal 岗位配置唯一来源） ====================
/**
 * 岗位下拉数据源 = GET /portal/interview/jobTemplate/list（后台【岗位模板】可配）。
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
    difficulty: t.difficulty ?? config.value.difficulty ?? 'medium',
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


/** 高级设置折叠（核心只留岗位+JD+简历，其余收进折叠区保持准备页紧凑） */
const advancedOpen = ref(false);

// ==================== 开始面试 5 步准备进度条（点击后展示，完成自动进入面试页） ====================
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

// ==================== 结束触发点（连续跳过 3 题 / 5 分钟无响应，均弹确认） ====================
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

// ==================== 报告等级徽章 + 三段式信息（candidate/jobInfo） ====================
const scoreLevel = computed(() => {
  const s = report.value?.totalScore ?? 0;
  if (s >= 80) return { label: '优秀', cls: 'excellent' };
  if (s >= 70) return { label: '良好', cls: 'good' };
  if (s >= 60) return { label: '合格', cls: 'pass' };
  return { label: '待提升', cls: 'weak' };
});
const candidateInfo = computed(() => report.value?.candidate ?? null);
const jobInfoView = computed(() => report.value?.jobInfo ?? null);
/** 技能字符串拆分为标签（逗号/顿号/斜杠/分号分隔）；原始 JSON 先解析再拆（兜底存量数据） */
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
    // 岗位候选来自后台岗位模板；命中则连带回填该模板的 JD/难度/题量
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
/** 简历收起/展开面板（默认收起保持紧凑；当前选择常显于头部按钮，选中联动求职意向到岗位） */
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

// 简历解析「预览 → 确认」两步式（与编辑页一致）
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
    // 同步规则解析（毫秒级、离线可用、不调 LLM、不落库），随即弹出校对弹窗
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
  // 清单 P1（strict）：config.value.position 可选，兜底为空串后再截断
  return (raw ?? '').slice(0, POSITION_MAX_LEN);
});

onMounted(() => {
  loadResumeList();
  checkActiveInterview();
});

// ==================== 断点续接：意外关闭后恢复进行中面试 ====================
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

// ==================== 全场倒计时 ====================
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

// ==================== 题目进度时间线（时长制：题数软参考，动态扩展） ====================
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

// ==================== 面试背景（右栏卡片） ====================
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
// ==================== 整场 LLM 复盘字段（新字段优先，旧报告回退旧字段） ====================
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
// 「🔮 追问预测」tab 数据
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

// ============================================================================
// 七 tab 新增数据（v7 静态版规范落地：面试背景 / 语音指标 / 改进建议 / 回放播放）
// ============================================================================

/** 题源标签（VoiceInterviewQaVO.questionSource → 中文徽标；缺失不显示） */
const SRC_LABEL: Record<string, string> = {
  bank: '题库',
  resume_project: '简历深挖',
  llm: 'AI 出题',
};

/** 7 天补强计划（静态示例数据 · 对齐静态页 面试报告v2.html，先跑通样式） */
const trainingPlanRows = [
  { days: '第 1–2 天', content: '学习 SkyWalking 官方文档并在本地 Demo 接入，产出 1 份调用链路截图', output: '1 份排查案例' },
  { days: '第 3–4 天', content: '复盘一个高并发场景，写出压测→阈值→降级→熔断的完整方案，背熟 3 个数字', output: '1 页架构笔记' },
  { days: '第 5 天', content: '用「约束-对比-选择-代价」四段式重写 2 个项目的自我介绍稿', output: '2 份逐字稿' },
  { days: '第 6 天', content: '降速训练：朗读逐字稿录音，语速压到 180 字/分，填充词 ≤ 8 次', output: '4 段录音' },
  { days: '第 7 天', content: '完整模拟面试一轮（3 题），对比本报告指标，重点看系统设计与排查维度', output: '1 份新报告' },
];

/** 考察方向 chips（report.knowledgePoints，字符串或 {title/name} 对象 → 统一文案） */
const knowledgeChips = computed<string[]>(() =>
  (report.value?.knowledgePoints ?? [])
    .map((k) => (typeof k === 'string' ? k : k.title || k.name || ''))
    .filter(Boolean),
);

/** 🎙️ 语音指标卡（真实 VO 字段驱动：缺失隐藏 —— 对齐「字段为空按缺失隐藏」惯例） */
const voiceMetricCards = computed(() => {
  const cards: { label: string; value: string; unit: string; tip: string }[] = [];
  const qaTotal = interview.value?.qaList?.length ?? 0;
  const qaAnswered = interview.value?.qaList?.filter((q) => q.userAnswer).length ?? 0;
  if (interview.value?.durationMinutes != null) {
    cards.push({ label: '⏱️ 面试时长', value: String(interview.value.durationMinutes), unit: '分钟', tip: '本场配置时长' });
  }
  if (qaTotal) {
    cards.push({ label: '💬 作答轮数', value: `${qaAnswered}`, unit: `/ ${qaTotal} 轮`, tip: '已作答 / 总轮数' });
  }
  if (report.value?.fluencyAvg != null) {
    cards.push({
      label: '🌊 表达流畅度',
      value: String(report.value.fluencyAvg),
      unit: '/ 100',
      tip: '逐轮语音转写流畅度均分',
    });
  }
  const intro = report.value?.introScore;
  if (intro?.total != null) {
    cards.push({ label: '🎤 自我介绍', value: String(intro.total), unit: '分', tip: '自我介绍独立评分' });
  }
  return cards;
});

/** 逐轮心态文案（nervous/hesitant/calm/confident → 中文，复用全局 SENTIMENT_LABEL） */

/** 回放播放：已点亮轮次集合 + 播放状态（逐条 650ms 淡入，对齐 v7 交互规范） */
const playbackLit = ref<Set<number>>(new Set());
const playbackPlaying = ref(false);
let playbackTimers: ReturnType<typeof setTimeout>[] = [];

/** 播放态激活（播放中或已部分点亮）：列表进入「逐条浮现」模式，未点亮轮次隐藏 */
const playbackActive = computed(() => playbackPlaying.value || playbackLit.value.size > 0);
/** 播放进度百分比（进度条用） */
const playbackProgress = computed(() => {
  const total = interview.value?.qaList?.length ?? 0;
  return total ? Math.round((playbackLit.value.size / total) * 100) : 0;
});

/** 回放「从头播放」：先全灭，再按 650ms 间隔逐条点亮，进度条由 lit 比例驱动 */
function playPlayback() {
  const list = interview.value?.qaList ?? [];
  if (!list.length || playbackPlaying.value) return;
  playbackTimers.forEach(clearTimeout);
  playbackTimers = [];
  playbackLit.value = new Set();
  playbackPlaying.value = true;
  list.forEach((qa, i) => {
    playbackTimers.push(
      setTimeout(() => {
        const next = new Set(playbackLit.value);
        next.add(qa.id);
        playbackLit.value = next;
        if (i === list.length - 1) playbackPlaying.value = false;
      }, 350 + i * 650),
    );
  });
}

/** 组件卸载/重新进入报告时清理播放定时器 */
function stopPlayback() {
  playbackTimers.forEach(clearTimeout);
  playbackTimers = [];
  playbackPlaying.value = false;
}

// ============================================================================
// 「🧭 发展方向」懒生成
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
// 问题分析「按维度聚类」
// ============================================================================
/**
 * 最低维度（雷达六维中得分最低者）。
 *
 * 口径说明：后端**不产出逐题维度分**（`questionReviews` 只有总分与点评），
 * 因此这里不做「把某题归到某维度」的臆测映射 —— 那是无依据的编造。
 * 实际做法：找出最低维度作为**归因锚点**，再把**低分题置顶**供优先复盘。
 */
// 清单 P1（strict）：lowest 只在 forEach 回调里被赋值，TS 会把返回类型收窄为 `null`，
  // 导致模板里 `weakestDimension.label` 报 "Property 'label' does not exist on type 'never'"。
  // 显式声明返回类型即可（运行时不变）。
const weakestDimension = computed<{ key: string; label: string; value: number } | null>(() => {
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
/** 考察者结论判词与候选人画像（2026-10-12 报告评审升级；旧报告 undefined 隐藏） */
const reportVerdict = computed(() => report.value?.verdict || '');
const reportProfile = computed(() => report.value?.profile || '');
/** 五维能力剖面（考察者视角；旧报告 undefined → 空数组隐藏） */
const competencyItems = computed(() => report.value?.competencyViews ?? []);
/** 薄弱点归因卡（结构化归因字段优先；无归因字段回退简版 title+detail） */
const weakPointCards = computed<WeakPointView[]>(() => {
  const views = report.value?.weakPointViews;
  if (views?.length) return views;
  return weakPointItems.value.map((w) => ({ title: w.title, detail: w.detail }));
});
/** 结构化行动建议（suggestionViews 优先；旧报告回退 suggestionItems 文本列表） */
const suggestionCardItems = computed(() => report.value?.suggestionViews ?? []);
/** AI 深度复盘降级标记 → 概要 tab 顶部横幅 + 重新生成入口 */
const reportReviewDegraded = computed(() => report.value?.reviewDegraded === true);
const WEAK_TYPE_META: Record<string, { label: string; cls: string }> = {
  knowledge: { label: '知识缺口', cls: 'knowledge' },
  expression: { label: '表达问题', cls: 'expression' },
  depth: { label: '深度不足', cls: 'depth' },
};
function weakTypeMeta(type?: string) {
  return (type && WEAK_TYPE_META[type]) || null;
}
/** 逐题"你的回答"折叠展开（默认 2 行，点击展开全文） */
const expandedReviews = ref<Set<number>>(new Set());
function toggleReviewExpand(idx: number) {
  const next = new Set(expandedReviews.value);
  if (next.has(idx)) next.delete(idx);
  else next.add(idx);
  expandedReviews.value = next;
}
/** 重新生成报告——确认后重置进度复用现有轮询链路（skipConfirm：历史页入口已确认过） */
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

/** 改进建议卡（合并 improvementSuggestions 标题+分数 与 suggestionViews 动作+验收，对齐静态页 .dev-item） */
const adviceCards = computed(() => {
  const views = report.value?.suggestionViews ?? [];
  const improves = report.value?.improvementSuggestions ?? [];
  const weakViews = report.value?.weakPointViews ?? [];
  const n = Math.max(views.length, improves.length);
  if (n === 0) return [];
  return Array.from({ length: n }, (_, i) => {
    const v = views[i];
    const improve = improves[i] || '';
    // improvementSuggestions 格式："补齐链路追踪能力（预计排查维度 +15 分）"
    const m = improve.match(/^(.+?)（预计(.+?)）/);
    const title = m ? m[1].trim() : (v?.action || improve);
    const gain = m ? m[2].trim() : '';
    const weak = weakViews[i];
    const gap = weak?.evidence || weak?.detail || '';
    return {
      priority: v?.priority ?? (i + 1),
      title,
      gain,
      gap,
      action: v?.action || '',
      acceptance: v?.acceptance || '',
    };
  });
});
function devPriorityClass(p: number) {
  return p <= 1 ? 'p1' : p === 2 ? 'p2' : 'p3';
}

/** 逐题分析：筛选 chips + 折叠展开（对齐静态页 .qf + .q 卡片） */
const qFilter = ref<'all' | 'low' | 'mid' | 'flag'>('all');
const expandedQuestions = ref<Set<number>>(new Set());
function toggleQuestionExpand(idx: number) {
  const next = new Set(expandedQuestions.value);
  if (next.has(idx)) next.delete(idx);
  else next.add(idx);
  expandedQuestions.value = next;
}
const filteredQuestionList = computed(() => {
  const f = qFilter.value;
  return lowScoreFirst.value.filter((q) => {
    const s = q.score ?? 0;
    const risk = (q.tags ?? []).some((t) => t.type === 'red');
    if (f === 'all') return true;
    if (f === 'low') return s < 60;
    if (f === 'mid') return s >= 60 && s < 80;
    if (f === 'flag') return risk;
    return true;
  });
});
function qScoreClass(score: number) {
  return score >= 80 ? 'sc-high' : score >= 60 ? 'sc-mid' : 'sc-low';
}

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

    // 进行中——回准备页走断点续接横幅（历史点错/旧标签页场景）
    if (vo.status === 'in_progress') {
      phase.value = 'setup';
      checkActiveInterview();
      toast.info('该面试尚未结束，可从页面横幅继续');
      return;
    }

    // 已结束但报告仍在生成（刷新/离开后）——进报告页显示进度并轮询，完成后展示完整报告
    if ((vo.analysisStatus ?? 0) < 2) {
      phase.value = 'report';
      reportTab.value = 'bg';
      analysisState.value = { active: true, progress: Math.max(5, vo.analysisProgress ?? 0) };
      startAnalysisPolling();
      return;
    }

    // 报告已生成——finish 幂等拉取库中完整报告（真实数据，不再前端拼凑伪报告）
    phase.value = 'report';
    reportTab.value = 'bg';
    await loadFullReport();

    // 历史页「重新生成报告」入口携带 regenerate=1，报告加载完成后自动触发（入口处已确认过，跳过二次弹窗）
    if (route.query.regenerate === '1') {
      router.replace({ query: { ...route.query, regenerate: undefined } });
      handleRegenerateReport(true);
    }
  } finally {
    historyLoading.value = false;
  }
}

// ========== 概览评分 Canvas 雷达图（六维能力模型） ==========
function easeOutCubic(t: number) { return 1 - Math.pow(1 - t, 3); }
function getScoreColorRadar(score: number) {
  if (score < 60) return '#e11d48';
  if (score < 80) return '#d97706';
  return '#059669';
}
// 雷达数据：优先用能力剖面（competencyViews），回退六维评估维度
const radarDimensions = computed(() => {
  if (competencyItems.value.length) {
    return competencyItems.value.map(c => ({ name: c.label || c.key || '', score: c.score ?? 0 }));
  }
  return DIMENSION_META.map((m, i) => ({ name: m.label, score: reportScores.value[i] ?? 0 }));
});
function drawOverviewRadar(progress: number) {
  const canvas = overviewRadarCanvas.value;
  if (!canvas) return;
  const dims = radarDimensions.value;
  if (!dims.length) return;
  const ctx = canvas.getContext('2d');
  if (!ctx) return;
  const dpr = window.devicePixelRatio || 1;
  const cssW = canvas.clientWidth;
  const cssH = canvas.clientHeight;
  if (cssW === 0 || cssH === 0) return;
  canvas.width = cssW * dpr;
  canvas.height = cssH * dpr;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, cssW, cssH);
  const R = Math.max(40, Math.min(cssW * 0.30, (cssH - 58) / 2));
  const cx = cssW / 2;
  const cy = Math.max(R + 48, cssH / 2 - 8);
  const n = dims.length;
  const angleStep = (Math.PI * 2) / n;
  const startAngle = -Math.PI / 2;
  const vertex = (i: number, r: number) => {
    const a = startAngle + i * angleStep;
    return { x: cx + r * Math.cos(a), y: cy + r * Math.sin(a), angle: a };
  };
  // 4 层同心多边形网格
  const layers = 4;
  for (let l = 1; l <= layers; l++) {
    const r = (R / layers) * l;
    ctx.beginPath();
    for (let i = 0; i < n; i++) {
      const v = vertex(i, r);
      if (i === 0) ctx.moveTo(v.x, v.y); else ctx.lineTo(v.x, v.y);
    }
    ctx.closePath();
    ctx.strokeStyle = l === layers ? '#d1d5db' : '#e5e7eb';
    ctx.lineWidth = 1;
    ctx.stroke();
  }
  // 轴线
  for (let i = 0; i < n; i++) {
    const v = vertex(i, R);
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(v.x, v.y);
    ctx.strokeStyle = '#e5e7eb';
    ctx.lineWidth = 1;
    ctx.stroke();
  }
  // 数据多边形
  ctx.beginPath();
  for (let i = 0; i < n; i++) {
    const dataR = (dims[i].score / 100) * R * progress;
    const v = vertex(i, dataR);
    if (i === 0) ctx.moveTo(v.x, v.y); else ctx.lineTo(v.x, v.y);
  }
  ctx.closePath();
  const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, R);
  grad.addColorStop(0, 'rgba(225,29,72,0.35)');
  grad.addColorStop(1, 'rgba(225,29,72,0.12)');
  ctx.fillStyle = grad;
  ctx.fill();
  ctx.strokeStyle = '#e11d48';
  ctx.lineWidth = 2.5;
  ctx.lineJoin = 'round';
  ctx.stroke();
  // 顶点圆点
  for (let i = 0; i < n; i++) {
    const dataR = (dims[i].score / 100) * R * progress;
    const v = vertex(i, dataR);
    const color = getScoreColorRadar(dims[i].score);
    ctx.beginPath();
    ctx.arc(v.x, v.y, 4.5, 0, Math.PI * 2);
    ctx.fillStyle = '#ffffff';
    ctx.fill();
    ctx.strokeStyle = color;
    ctx.lineWidth = 2.5;
    ctx.stroke();
  }
  // 维度标签（名称在上，分数在下）
  for (let i = 0; i < n; i++) {
    const color = getScoreColorRadar(dims[i].score);
    const labelR = R + 26;
    const lv = vertex(i, labelR);
    const cos = Math.cos(lv.angle);
    let textAlign: CanvasTextAlign = 'center';
    if (cos > 0.3) textAlign = 'left';
    else if (cos < -0.3) textAlign = 'right';
    ctx.textAlign = textAlign;
    ctx.textBaseline = 'middle';
    ctx.font = '13px -apple-system, BlinkMacSystemFont, sans-serif';
    ctx.fillStyle = '#4b5563';
    ctx.fillText(dims[i].name, lv.x, lv.y - 9);
    ctx.font = 'bold 14px -apple-system, BlinkMacSystemFont, sans-serif';
    ctx.fillStyle = color;
    ctx.fillText(String(dims[i].score), lv.x, lv.y + 9);
  }
}
let overviewRadarRAF = 0;
function animateOverviewRadar() {
  cancelAnimationFrame(overviewRadarRAF);
  const duration = 950;
  const startTime = performance.now();
  const frame = (now: number) => {
    const t = Math.min((now - startTime) / duration, 1);
    drawOverviewRadar(easeOutCubic(t));
    if (t < 1) overviewRadarRAF = requestAnimationFrame(frame);
  };
  overviewRadarRAF = requestAnimationFrame(frame);
}
function handleOverviewRadarResize() { drawOverviewRadar(1); }
watch([() => report.value, () => reportTab.value], () => {
  if (reportTab.value === 'overview' && overviewRadarCanvas.value && radarDimensions.value.length) {
    nextTick(() => animateOverviewRadar());
  }
}, { flush: 'post' });

onMounted(() => {
  const id = String(route.query.id ?? '').trim();
  if (id) loadHistoryReport(id);
  // 岗位下拉由后台【岗位模板】驱动（默认选中首个并回填 JD/难度/题量）
  loadJobTemplates();
  // 页签切走/最小化：立即停止聆听与播报，及时释放麦克风等硬件占用
  document.addEventListener('visibilitychange', releaseOnHidden);
  // 浏览器关闭/刷新：无条件释放麦克风/播放器；面试进行中先触发浏览器离开确认
  window.addEventListener('beforeunload', handleBeforeUnload);
  window.addEventListener('resize', handleOverviewRadarResize);
});

onUnmounted(() => {
  document.removeEventListener('visibilitychange', releaseOnHidden);
  window.removeEventListener('beforeunload', handleBeforeUnload);
  window.removeEventListener('resize', handleOverviewRadarResize);
  cancelAnimationFrame(overviewRadarRAF);
  // 卸载即丢弃：立即停麦克风/断 WS/停播报与计时，不等转写收尾（用户已离开页面，无需保留结果）
  releaseMediaResources();
  // 停止报告分析进度轮询（服务端继续生成，可从历史记录查看）
  stopAnalysisPolling();
  // 停止回放播放定时器
  stopPlayback();
  // 清理结束触发点计时与准备进度条
  clearIdleWatch();
  stopPrepareProgress();
});

/** 统一释放麦克风/播放器/计时器——结束面试、临时退出、关闭标签页均有始有终 */
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

/** 浏览器关闭/刷新标签页——先释放硬件连接；面试进行中触发浏览器离开确认 */
function handleBeforeUnload(e: BeforeUnloadEvent) {
  releaseMediaResources();
  if (phase.value === 'interview') {
    e.preventDefault();
    e.returnValue = '';
  }
}

// 面试进行中路由跳转（返回/切页）前确认；确认离开后统一释放麦克风与播放器
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
  // 5 分钟无响应触发点——新题展示时武装
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
  // 5 分钟无响应触发点——新题展示时武装
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
  // 【合规改造（2026-10）】全站免费化：语音面试不再校验会员权益与免费次数，
  // 也不再有"开通会员"引导（平台不提供有偿信息服务）。
  loading.value = true;
  // 5 步准备进度条（简历画像→岗位要求→会话上下文→题单→环境；请求返回即 100%）
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
      // 岗位要求 JD（面试官提问贴合岗位要求）
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
      // 新面试已开始（后端已收口遗留会话），同步清掉恢复横幅
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
      // 成功作答清零连续跳过计数（跳过路径保留计数）
      if (!isSkip) skipStreak.value = 0;
      if (payload?.finished) {
        toast.info('面试结束，正在生成报告...');
        handleFinish();
        return;
      }
      // 倒计时归零后的收尾（答案已自动提交保存，直接进报告）
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
    // 流被服务端异常切断（后端 SSE 120s 超时收尾/网络中断），提示用户可重答或下一题
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
  if (submitting.value) return;   // 清单 P2：在途防重
  // 清单 P2：submitting 原先在 `await whenTranscriptionDone()` **之后**才置 true ——
  // 等待转写期间「回答完毕」按钮仍是可点状态，连点会并发提交。这里提前置位。
  submitting.value = true;
  if (listening.value) stopAsr();
  // 服务端 ASR 兜底：等待录音停止与上传转写完成，确保语音文本已并入答案
  await whenTranscriptionDone();
  const transcript = editableAnswer.value || finalText.value;
  if (!transcript.trim()) {
    submitting.value = false;   // 提前置位后，早退分支必须复位
    toast.warning('答案不能为空');
    return;
  }
  stopCountdown();
  pushChat('user', transcript);
  const latencyMs = answerStartTime.value ? Date.now() - answerStartTime.value : 0;
  clearTtsBuffer();
  // 首字前预建"思考中"占位气泡（delta 到达即续写；失败/中断由 finishStreaming 移除空占位）
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
  // 连续跳过 3 题触发结束确认（真实面试连续拒答会被终止）
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
  // 首字前预建"思考中"占位气泡（同作答路径）
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
 * finish 同步段仅收口会话并触发异步批量分析（返回报告骨架）。
 * 前端进入报告页后轮询 analysis 接口驱动进度条，analysisStatus=2 后
 * 重新调用 finish（幂等，返回完整报告）拉取最终结果。
 */
async function handleFinish() {
  if (!interview.value) return;
  // 结束面试统一释放麦克风/播放器/计时器（有始有终）
  releaseMediaResources();
  // 结束触发点收口——清理无响应计时与准备进度
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
      reportTab.value = 'bg';
      // 骨架报告（summary=报告生成中…）→ 启动分析进度轮询；
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

// ==================== 报告分析进度轮询 ====================
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

/** finish 后重拉详情填充 qaList（对话回放 Tab 数据源） */
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
  // 重开面试同样统一释放，避免残留占用
  releaseMediaResources();
  // 重开时停止上一场的分析进度轮询并复位进度
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
/** 批量加入错题本进行中（清单 P2：原批量入口无 loading/防重复，连点会重复请求） */
const wrongBookBatchLoading = ref(false);

async function handleAddToWrongBook(qaId: number | undefined, silentToast = false): Promise<boolean> {
  if (!qaId) {
    toast.warning('无法关联题目');
    return false;
  }
  if (wrongBookLoading.value != null) return false;   // 在途防重
  wrongBookLoading.value = qaId;
  // 清单 P2：原先既传 successToast:'已加入错题本' 又手动 toast.success('…可在错题本中复习')
  // ⇒ 一次操作弹两条提示。这里统一：批量场景静默（由批量结果统一提示），单条场景只弹一条。
  const { success } = await run(() => addQaToWrongBook(qaId), {
    errorToast: '加入错题本失败',
    successToast: silentToast ? undefined : '已加入错题本，可在错题本中复习',
  });
  wrongBookLoading.value = null;
  return success;
}

async function addAllWeakToWrongBook() {
  if (wrongBookBatchLoading.value) return;      // 在途防重
  const weak = (report.value?.questionReviews ?? []).filter((q) => q.score < 80 && q.qaId);
  if (weak.length === 0) {
    toast.info('暂无薄弱题（评分 < 80）需要加入错题本');
    return;
  }
  wrongBookBatchLoading.value = true;
  try {
    let ok = 0;
    for (const q of weak) {
      const success = await handleAddToWrongBook(q.qaId, true);
      if (success) ok += 1;
    }
    if (ok === weak.length) {
      toast.success(`已加入错题本 ${ok} 题，可在错题本中复习`);
    } else if (ok > 0) {
      toast.warning(`已加入 ${ok} / ${weak.length} 题，其余失败请重试`);
    }
  } finally {
    wrongBookBatchLoading.value = false;
  }
}

// ==================== 报告操作 ====================
/**
 * 打印/另存为 PDF（清单 P2）。
 *
 * <p>原函数名与按钮文案都写「下载 PDF 报告」，但实现是 `window.print()` ——
 * 得到的是**浏览器打印对话框**（用户可"另存为 PDF"），并非文件下载。
 * 这里把文案与注释改成与实际行为一致，避免误导。</p>
 */
async function downloadReport() {
  toast.info('已打开打印窗口，可选择"另存为 PDF"');
  setTimeout(() => window.print(), 300);
}
/**
 * HTML 转义（导出简历用；避免自我介绍里的尖括号破坏导出页结构）
 */
function escapeHtmlForExport(input: string): string {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * 导出简历 PDF（对齐静态页「📄 导出简历 PDF」）：
 * 生成独立 A4 简历 HTML → 新窗口打开 → 自动调起打印，用户可「另存为 PDF」。
 * 内容取候选人概要：姓名 / 目标岗位 / 经验年限 / 候选人ID / 导出日期 /
 * 个人简介 / 技能标签 / 求职意向 / 面试口头自我介绍。
 */
function exportResumePdf() {
  const c = candidateInfo.value as Record<string, any> | null | undefined;
  if (!c) {
    toast.warning('暂无候选人信息，无法导出简历');
    return;
  }
  const esc = escapeHtmlForExport;
  const name = esc(String(c.name || '面试者'));
  const pos = esc(String(interview.value?.position || '未指定岗位'));
  const years = c.yearsExp != null ? esc(String(c.yearsExp)) + ' 年' : '未填写';
  const cid = esc(String(c.candidateId || '-'));
  const today = new Date().toLocaleDateString('zh-CN');
  const intro = esc(String(c.resumeSelfIntro || '-'));
  const verbal = esc(String(c.interviewSelfIntro || '-'));
  const skillChips = (candidateSkills.value || [])
    .map((s: string) => '<span class="skill">' + esc(String(s)) + '</span>')
    .join('');

  const html = [
    '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="utf-8" />',
    '<title>简历 - ' + name + '</title>',
    '<style>',
    '  @page { size: A4; margin: 16mm 14mm; }',
    '  * { box-sizing: border-box; }',
    '  body { margin: 0; color: #1f2937; line-height: 1.75;',
    '    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif; }',
    '  .head { padding: 22px 24px; border-radius: 16px; color: #fff;',
    '    background: linear-gradient(135deg, #5b4bd6, #7c3aed 55%, #a855f7); }',
    '  .head h1 { margin: 0; font-size: 26px; font-weight: 800; letter-spacing: 1px; }',
    '  .head .pos { margin-top: 5px; font-size: 13.5px; opacity: .9; }',
    '  .meta { display: flex; flex-wrap: wrap; gap: 18px; margin-top: 14px; font-size: 12px; opacity: .92; }',
    '  h2 { margin: 22px 0 9px; padding-left: 10px; font-size: 14.5px; font-weight: 800;',
    '    border-left: 3.5px solid #4f46e5; }',
    '  p { margin: 0; font-size: 13px; color: #374151; white-space: pre-wrap; }',
    '  .skill { display: inline-block; margin: 0 6px 6px 0; padding: 3px 11px; border-radius: 7px;',
    '    font-size: 12px; background: #eef2ff; color: #4f46e5; border: 1px solid #c7d2fe; }',
    '  .box { padding: 13px 15px; border-radius: 10px; background: #f8f9fc; border: 1px solid #e8eaef; }',
    '</style></head><body>',
    '  <div class="head">',
    '    <h1>' + name + '</h1>',
    '    <div class="pos">求职意向：' + pos + '</div>',
    '    <div class="meta"><span>经验年限：' + years + '</span><span>候选人 ID：' + cid + '</span>',
    '      <span>导出日期：' + today + '</span></div>',
    '  </div>',
    '  <h2>个人简介</h2><div class="box"><p>' + intro + '</p></div>',
    skillChips ? '  <h2>技能标签</h2><div>' + skillChips + '</div>' : '',
    '  <h2>面试口头自我介绍</h2><div class="box"><p>' + verbal + '</p></div>',
    '  <scr' + 'ipt>window.addEventListener("load", function () { setTimeout(function () { window.print(); }, 260); });</scr' + 'ipt>',
    '</body></html>'
  ].join('\n');

  const w = window.open('', '_blank');
  if (!w) {
    toast.warning('浏览器拦截了新窗口，请允许弹窗后重试');
    return;
  }
  w.document.open();
  w.document.write(html);
  w.document.close();
}

async function shareReport() {
  const id = interview.value?.id ?? report.value?.interviewId;
  if (!id) {
    toast.warning('暂无可分享的报告');
    return;
  }
  // token 分享（免登录公开，7 天有效）
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
        <!-- 面包屑（替代原「AI 语音面试准备 + 描述 + 我的面试记录」工具行：
             站点 Navbar 已提供「面试专区」入口，「我的面试记录」在报告页仍可进入） -->
        <!-- 面包屑 + 历史入口（「我的面试记录」是本页唯一入口，必须保留；
             标题与描述已由面包屑承担，不再重复文字） -->
        <div class="prep-topline">
          <Breadcrumb
            class="prep-crumb"
            :items="[
              { label: '面试专区', path: '/interview' },
              { label: 'AI 语音面试', path: '/interview/voice' },
              { label: '面试准备' },
            ]"
          />
          <button class="prep-ghost-btn" @click="router.push('/interview/voice/history')">📋 我的面试记录</button>
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

        <!-- 断点续接横幅：意外关闭后再次进入，提示继续上次面试 -->
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

            <!-- 环境噪声检测（3 秒采样取平均；嘈杂时黄色提示换环境/戴耳机） -->
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

        <!-- 第二步：岗位与简历（核心只留岗位 + JD + 简历，其余配置收进高级设置） -->
        <div class="prep-card">
          <div class="prep-card-title"><span class="step-badge">2</span>岗位与简历</div>

          <!-- 岗位选择保持紧凑下拉 -->
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

          <!-- 岗位要求 JD（选中岗位模板后自动回填，可自由修改；未选或自定义则以手输为准） -->
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

            <!-- 简历收起/展开面板（默认收起保持紧凑；按钮常显当前选择，选中联动求职意向到岗位） -->
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

        <!-- 高级设置折叠（其余配置可酌情保留；默认收起保持准备页紧凑） -->
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
                <p class="config-hint">1 个方向 = 1 个主问题 + 1~2 轮追问；自我介绍不计入，实际对话轮数约为方向数的 2 倍</p>
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

      <!-- 准备进度覆盖层（不可关闭；完成自动进入面试页） -->
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
          <Breadcrumb
            class="proc-crumb"
            :items="[
              { label: '面试专区', path: '/interview' },
              { label: 'AI 语音面试', path: '/interview/voice' },
              { label: '面试进行中' },
            ]"
          />
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
          <button class="control-btn danger" @click="confirmEnd">⏹️ 结束面试</button>
        </div>
      </div>
      <div class="interview-main">
        <!-- 左面板 -->
        <div class="left-panel">
          <div class="interviewer-card">
            <div :class="['interviewer-avatar', { speaking: speaking }]">
              <img
                :src="interviewerAvatar"
                alt="AI 面试官 林川"
                class="interviewer-photo"
                draggable="false"
              />
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

        <!-- 右面板（面试背景 + 提示） -->
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
        <!-- 面包屑（替代原「📊 面试复盘报告 + 我的面试记录 + 返回首页」标题区：
             站点 Navbar 已提供首页/面试专区入口，页内不再重复导航文字与按钮） -->
        <Breadcrumb
          class="rpt-crumb"
          :items="[
            { label: '面试专区', path: '/interview' },
            { label: 'AI 语音面试', path: '/interview/voice' },
            { label: '评估报告' },
          ]"
        />
        <!-- 批量分析进度条（结束后轮询 analysis 接口驱动，完成自动拉取完整报告） -->
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
        <!-- ==================================================================
             紫色 Hero + 悬浮评分卡（严格对齐静态页 面试报告v2.html）
             Hero：135deg 紫渐变 + 两处光晕 + 玻璃按钮 + 元信息网格（5 项）
             评分卡：-14px 负边距上浮与 Hero 重叠 + 56px 大分数 + 匹配度条
             ================================================================== -->
        <div class="rpt-hero">
          <div class="rpt-hero-top">
            <div>
              <h1>AI 语音模拟面试评估报告</h1>
              <div class="rpt-hero-sub">Voice Mock Interview Analysis Report</div>
            </div>
            <div class="rpt-hero-actions">
              <button class="rpt-btn-glass" :disabled="!report" @click="exportResumePdf">📄 导出简历 PDF</button>
              <button class="rpt-btn-glass" @click="shareReport">🔗 分享报告</button>
            </div>
          </div>
          <div class="rpt-hero-meta">
            <div class="rpt-meta-item">
              <div class="rpt-meta-label">候选人</div>
              <div class="rpt-meta-value">{{ candidateInfo?.name || '面试者' }}</div>
            </div>
            <div class="rpt-meta-item">
              <div class="rpt-meta-label">目标岗位</div>
              <div class="rpt-meta-value">{{ interview?.position || '未指定' }}</div>
            </div>
            <div class="rpt-meta-item">
              <div class="rpt-meta-label">面试时间</div>
              <div class="rpt-meta-value">{{ interview?.createTime || '-' }}</div>
            </div>
            <div class="rpt-meta-item">
              <div class="rpt-meta-label">面试时长</div>
              <div class="rpt-meta-value">{{ interview?.durationMinutes != null ? interview.durationMinutes + ' 分钟' : '-' }}</div>
            </div>
            <div class="rpt-meta-item">
              <div class="rpt-meta-label">面试编号</div>
              <div class="rpt-meta-value">#{{ interview?.id ?? '-' }}</div>
            </div>
          </div>
        </div>

        <!-- 评分卡（-14px 负边距与 Hero 重叠；报告未就绪或分析中不渲染） -->
        <div v-if="report && !analysisState.active" class="rpt-score-card">
          <div class="rpt-score-flex">
            <div class="rpt-score-block">
              <div :class="['rpt-score-num', scoreClass(report.totalScore ?? 0)]">{{ report.totalScore ?? 0 }}</div>
              <div class="rpt-score-label">综合得分</div>
              <div :class="['rpt-score-badge', scoreClass(report.totalScore ?? 0)]">{{ scoreLevel.label }}</div>
              <div v-if="levelBadge" class="rpt-level-badge" :title="levelBadge.tip">定级 · {{ levelBadge.text }}</div>
            </div>
            <div class="rpt-score-desc">
              {{ reportOverall || '本次面试尚未形成完整总结。' }}
              <span v-if="jobMatchReason" class="rpt-score-muted">🎯 {{ jobMatchReason }}</span>
            </div>
            <div v-if="jobMatchRate != null" class="rpt-match-block">
              <div class="rpt-match-label">岗位匹配度</div>
              <div :class="['rpt-match-num', scoreClass(jobMatchRate)]">{{ jobMatchRate }}%</div>
              <div class="rpt-match-bar"><i :style="{ width: jobMatchRate + '%' }"></i></div>
            </div>
          </div>
        </div>
        <!-- 六 tab（严格对齐静态页 面试报告v2.html）：背景 → 概览 → 回放 → 逐题 → 语音 → 建议 -->
        <div class="report-tabs">
          <button
            :class="['report-tab', { active: reportTab === 'bg' }]"
            @click="reportTab = 'bg'"
            >🎯 面试背景</button>
          <button
            :class="['report-tab', { active: reportTab === 'overview' }]"
            @click="reportTab = 'overview'"
            >📋 概览评分</button>
          <button
            :class="['report-tab', { active: reportTab === 'dialog' }]"
            @click="reportTab = 'dialog'"
            >💬 面试回放</button>
          <button
            :class="['report-tab', { active: reportTab === 'analysis' }]"
            @click="reportTab = 'analysis'"
            >🔍 逐题分析</button>
          <button
            :class="['report-tab', { active: reportTab === 'advice' }]"
            @click="reportTab = 'advice'"
            >🚀 改进建议</button>
        </div>

        <!-- ==================================================================
             Tab 1: 🎯 面试背景（默认）—— 候选人概要 + 岗位信息 + 简历概要
             + 考察方向（v7 静态版结构，原常驻区移入；自介评分移至概览评分 Tab）
             ================================================================== -->
        <div v-if="reportTab === 'bg'" class="tab-content active">
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
              <div class="bg-meta-row">
                <span class="bg-meta-item">难度：{{ interview?.difficulty || '中等' }}</span>
                <span class="bg-meta-item">题源：{{ interview?.resumeId ? '简历深挖' : '岗位画像' }}</span>
              </div>
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

          <!-- 考察方向（本场预设知识点） -->
          <div v-if="knowledgeChips.length" class="archive-block">
            <div class="archive-block-title">🧭 本次考察方向（{{ knowledgeChips.length }}）</div>
            <div class="candidate-skills-row">
              <span v-for="(k, i) in knowledgeChips" :key="'kp' + i" class="candidate-skill">{{ k }}</span>
            </div>
          </div>

          <div v-if="!candidateInfo && !jobInfoView && !knowledgeChips.length" class="empty-tip">
            暂无背景数据。
          </div>
        </div>

        <!-- ==================================================================
             Tab 3: 💬 面试回放 —— 聊天式布局，仅保留 AI 提问与我的回答
             （点评移至「逐题分析」查看）；支持从头播放（逐条 650ms 淡入）
             ================================================================== -->
        <div v-if="reportTab === 'dialog'" class="tab-content active">
          <!-- 播放控制条（v7：▶ 从头播放 + 进度条 + N/M 计数器） -->
          <div v-if="interview?.qaList?.length" class="replay-controls">
            <button class="replay-play-btn" :disabled="playbackPlaying" @click="playPlayback">
              {{ playbackPlaying ? '⏳ 播放中…' : '▶ 从头播放' }}
            </button>
            <div class="replay-progress">
              <div class="replay-progress-fill" :style="{ width: playbackProgress + '%' }"></div>
            </div>
            <span class="replay-counter">{{ playbackLit.size }} / {{ interview.qaList.length }}</span>
          </div>
          <div v-if="!interview?.qaList?.length" class="dialog-empty">
            本次面试没有保存对话内容。
          </div>
          <div v-else class="replay-list">
            <div
              v-for="qa in interview.qaList"
              :key="qa.id"
              class="replay-item"
              :class="{ hidden: playbackActive && !playbackLit.has(qa.id), lit: playbackLit.has(qa.id) }"
            >
              <!-- 面试官 -->
              <div class="replay-row interviewer">
                <div class="replay-avatar">AI</div>
                <div class="replay-bubble ai">
                  <div class="replay-idx">
                    问题 {{ qa.questionIdx }}
                    <span v-if="qa.parentQaId" class="replay-tag">追问</span>
                    <span v-if="qa.questionSource && SRC_LABEL[qa.questionSource]" class="replay-tag src">{{ SRC_LABEL[qa.questionSource] }}</span>
                  </div>
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
            </div>
          </div>
        </div>

        <!-- ==================================================================
             Tab 2: 📋 概览评分（v7 规范）—— 雷达首屏（结论）→ 叙事区（总评/亮点）
             → 薄弱点简版（归因卡详版在「逐题分析」）；改进建议移至「改进建议」tab，
             资料类内容拆出：自介分→面试背景、过程信号→语音指标
             ================================================================== -->
        <div v-if="reportTab === 'overview'" class="tab-content active">
          <!-- AI 深度复盘降级横幅（模型无输出/解析失败 → 规则聚合数据仍有效，提供重新生成入口） -->
          <div v-if="reportReviewDegraded" class="report-degraded-banner">
            <span class="report-degraded-text">⚠️ AI 深度复盘未完成，当前仅展示基础聚合数据（逐题评分有效，总评/能力剖面缺失）。</span>
            <button class="report-degraded-btn" @click="handleRegenerateReport()">重新生成报告</button>
          </div>

          <!-- 1. 能力雷达 + 总分 + 定级 + 匹配度 -->
          <div class="card">
            <div class="card-title radar-card-title">
              <span class="bar"></span>能力雷达
              <span class="tip">六维能力模型</span>
            </div>
            <div class="overview-radar-wrap">
              <div class="overview-radar">
                <canvas ref="overviewRadarCanvas" class="overview-radar-canvas"></canvas>
              </div>
              <div class="overview-radar-meta">
                <div class="overview-total-row">
                  <span class="overview-total-label">总分</span>
                  <strong :style="{ color: scoreColor(report?.totalScore ?? 0) }">{{ report?.totalScore ?? 0 }}</strong>
                  <span class="overview-total-unit">分</span>
                </div>
                <div class="overview-badges">
                  <span :class="['score-level-badge', scoreLevel.cls]">{{ scoreLevel.label }}</span>
                  <span v-if="levelBadge" :class="['level-estimate-badge', levelBadge.key]" :title="levelBadge.tip">定级 · {{ levelBadge.text }}</span>
                </div>
                <div v-if="jobMatchRate != null" class="overview-match">
                  <span class="overview-match-label">岗位匹配度</span>
                  <div class="match-bar overview-match-bar">
                    <div class="match-fill" :style="{ width: jobMatchRate + '%' }"></div>
                  </div>
                  <span class="overview-match-value">{{ jobMatchRate }}%</span>
                </div>
                <div v-if="reportVerdict" class="overview-verdict">
                  <span class="verdict-label">考察结论</span>
                  <span class="verdict-text">{{ reportVerdict }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 2. 维度评分明细 -->
          <div v-if="competencyItems.length" class="card">
            <div class="card-title"><span class="bar"></span>维度评分明细<span class="tip">共 {{ competencyItems.length }} 项</span></div>
            <div class="dim-list">
              <div v-for="c in competencyItems" :key="c.key" class="dim-row">
                <div class="dim-row-head">
                  <span class="dim-name">{{ c.label || c.key }}</span>
                  <div class="dim-bar">
                    <div :class="['dim-fill', scoreClass(c.score ?? 0)]" :style="{ width: (c.score ?? 0) + '%' }"></div>
                  </div>
                  <span :class="['dim-score', scoreClass(c.score ?? 0)]">{{ c.score ?? '-' }}</span>
                </div>
                <div v-if="c.comment" class="dim-note">{{ c.comment }}</div>
                <div v-if="c.evidenceIdx?.length" class="dim-evidence">证据：第 {{ c.evidenceIdx.join('、') }} 题</div>
              </div>
            </div>
          </div>

          <!-- 自我介绍评分（v7 从背景 Tab 移入概览评分 Tab） -->
          <div v-if="introScoreView" class="card">
            <div class="card-title">
              <span class="bar"></span>🎤 自我介绍评分
              <span class="tip">总分 <strong :style="{ color: scoreColor(introScoreView.total) }">{{ introScoreView.total }}</strong> 分</span>
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

          <!-- 3. 核心优势与待改进项（左右并排卡片） -->
          <div class="overview-pros-cons">
            <div class="card pros-card">
              <div class="card-title"><span class="bar green"></span>核心优势<span class="tip">{{ highlightItems.length }} 项</span></div>
              <div v-if="reportOverall || reportProfile" class="overview-summary">
                <p v-if="reportProfile" class="summary-paragraph profile-paragraph">🧑‍💼 {{ reportProfile }}</p>
                <p v-if="reportOverall" class="summary-paragraph">{{ reportOverall }}</p>
              </div>
              <div v-for="(p, i) in highlightItems" :key="'hp' + i" class="pros-item">
                <span class="pros-check">✅</span>
                <div class="pros-item-body">
                  <div class="pros-item-title">{{ p.title }}</div>
                  <div v-if="p.detail" class="pros-item-quote">{{ p.detail }}</div>
                </div>
              </div>
              <div v-if="highlightItems.length === 0" class="empty-tip">暂无亮点数据</div>
            </div>
            <div class="card cons-card">
              <div class="card-title"><span class="bar amber"></span>待改进项<span class="tip">{{ weakPointItems.length }} 项</span></div>
              <div v-for="(c, i) in weakPointItems" :key="'cw' + i" class="cons-item">
                <span class="cons-warn">⚠️</span>
                <div class="cons-item-body">
                  <div class="cons-item-title">{{ c.title }}</div>
                  <div v-if="c.detail" class="cons-item-quote">{{ c.detail }}</div>
                </div>
              </div>
              <div v-if="weakPointItems.length === 0" class="empty-tip">暂无薄弱点数据</div>
            </div>
          </div>
        </div>

        <!-- ==================================================================
             Tab 6: 🚀 改进建议 —— 改进卡（编号+标题+分数标签+差距+行动）
             + 追问预警（.pq-item 结构）+ 7 天补强计划表
             ================================================================== -->
        <div v-if="reportTab === 'advice'" class="tab-content active">
          <!-- 改进建议卡（对齐静态页 .dev-item） -->
          <div class="card">
            <div class="card-title"><span class="bar"></span>改进建议<span class="tip">按优先级排序</span></div>
            <p class="dev-lead">基于本场暴露的短板，按优先级给出「动作 + 验收口径」—— 做到验收口径描述的程度，即为过关。</p>
            <div v-if="adviceCards.length" class="dev-list">
              <div v-for="(c, i) in adviceCards" :key="'adv' + i" :class="['dev-item', devPriorityClass(c.priority)]">
                <div class="dev-head">
                  <div class="dev-rank">{{ c.priority }}</div>
                  <h5>{{ c.title }}</h5>
                  <span v-if="c.gain" class="dev-gain">{{ c.gain }}</span>
                </div>
                <div v-if="c.gap" class="dev-row"><span class="k">当前差距</span><span class="v">{{ c.gap }}</span></div>
                <div v-if="c.action" class="dev-row"><span class="k">行动建议</span><span class="v">{{ c.action }}</span></div>
                <div v-if="c.acceptance" class="dev-row"><span class="k">验收口径</span><span class="v">{{ c.acceptance }}</span></div>
              </div>
            </div>
            <div v-else class="empty-tip">暂无改进建议</div>
          </div>

          <!-- 追问预警（未问在前=预警；已问在后=复盘；对齐静态页 .pq-item） -->
          <div v-if="showPredictTab" class="card">
            <div class="card-title"><span class="bar"></span>追问预警<span class="tip">未问到的题 = 下次最可能被问</span></div>
            <div class="pq-list">
              <div v-for="(p, i) in predictedUnasked" :key="'u' + i" class="pq-item">
                <div class="pq-head">
                  <span class="pq-kp">{{ p.knowledgePoint || '综合' }}</span>
                  <span class="pq-state">⚠️ 未被问到</span>
                </div>
                <div class="pq-q">{{ p.question }}</div>
                <div v-if="p.briefAnswer" class="pq-a"><span class="k">要点</span>{{ p.briefAnswer }}</div>
                <div v-if="p.analysis" class="pq-a"><span class="k">缘由</span>{{ p.analysis }}</div>
              </div>
              <div v-for="(p, i) in predictedAsked" :key="'a' + i" class="pq-item pq-asked">
                <div class="pq-head">
                  <span class="pq-kp">{{ p.knowledgePoint || '综合' }}</span>
                  <span class="pq-state">本场已问 · {{ p.askedScore ?? '--' }} 分</span>
                </div>
                <div class="pq-q">{{ p.question }}</div>
                <div v-if="p.briefAnswer" class="pq-a"><span class="k">要点</span>{{ p.briefAnswer }}</div>
                <div v-if="p.analysis" class="pq-a"><span class="k">缘由</span>{{ p.analysis }}</div>
              </div>
            </div>
          </div>

          <!-- 7 天补强计划（对齐静态页 advice Tab §7 天补强计划表） -->
          <div v-if="trainingPlanRows.length" class="card">
            <div class="card-title"><span class="bar"></span>7 天补强计划<span class="tip">示例计划 · 按薄弱点替换</span></div>
            <table class="plan-table">
              <thead>
                <tr>
                  <th style="width:82px">时间</th>
                  <th>训练内容</th>
                  <th style="width:104px">目标产出</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(p, i) in trainingPlanRows" :key="'tp' + i">
                  <td class="day">{{ p.days }}</td>
                  <td>{{ p.content }}</td>
                  <td>{{ p.output }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- ==================================================================
             Tab 4: 📝 逐题分析 —— 筛选 chips + .q 折叠卡 + 6 块（意图/分析/点评/框架/差距/建议）+ 标签行
             ================================================================== -->
        <div v-if="reportTab === 'analysis'" class="tab-content active">
          <div class="card">
            <div class="card-title">
              <span class="bar"></span>逐题表现分析
              <span class="tip">共 {{ lowScoreFirst.length }} 题</span>
            </div>
            <!-- 筛选 chips -->
            <div class="qf">
              <div :class="['chip', { active: qFilter === 'all' }]" @click="qFilter = 'all'">全部</div>
              <div :class="['chip', { active: qFilter === 'low' }]" @click="qFilter = 'low'">薄弱 &lt; 60</div>
              <div :class="['chip', { active: qFilter === 'mid' }]" @click="qFilter = 'mid'">中等 60–79</div>
              <div :class="['chip', { active: qFilter === 'flag' }]" @click="qFilter = 'flag'">含风险点</div>
            </div>
            <!-- 逐题折叠卡 -->
            <div class="q-list">
              <div
                v-for="(q, i) in filteredQuestionList"
                :key="i"
                :class="['q', { open: expandedQuestions.has(q.questionIdx ?? i) }]"
              >
                <div class="q-head" @click="toggleQuestionExpand(q.questionIdx ?? i)">
                  <div class="q-title">
                    <span class="q-no">Q{{ q.questionIdx }}</span>
                    <span>{{ q.question }}</span>
                    <span v-if="q.selfIntro" class="pb-self">🎤 自我介绍</span>
                  </div>
                  <div class="q-right">
                    <div :class="['q-score', qScoreClass(q.score)]">{{ q.score }} 分</div>
                    <span class="q-arrow">▼</span>
                  </div>
                </div>
                <div class="q-body">
                  <div class="q-block">
                    <span class="bk">🎯 考察意图</span>
                    <div class="qa-intent">{{ q.intent || '--' }}</div>
                  </div>
                  <div class="q-block">
                    <span class="bk">回答分析</span>
                    <div class="answer">{{ q.analysis || q.feedback || '--' }}</div>
                  </div>
                  <div class="q-block">
                    <span class="bk">AI 点评</span>
                    <div class="comment">{{ q.feedback || '--' }}</div>
                  </div>
                  <div class="q-block">
                    <span class="bk">参考框架</span>
                    <ul class="qa-fw">
                      <li v-for="(f, fi) in (q.framework ?? [])" :key="fi">{{ f }}</li>
                      <li v-if="!q.framework?.length">--</li>
                    </ul>
                  </div>
                  <div class="q-block">
                    <span class="bk">差距分析</span>
                    <div class="answer">{{ q.gap || '--' }}</div>
                  </div>
                  <div class="q-block">
                    <span class="bk">提升建议</span>
                    <div class="suggest">{{ q.advice || '--' }}</div>
                  </div>
                  <div v-if="q.tags?.length" class="tagrow">
                    <span
                      v-for="(t, ti) in q.tags"
                      :key="ti"
                      :class="['tag', t.type]"
                    >{{ t.text }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="filteredQuestionList.length === 0" class="empty-tip">暂无符合筛选条件的题目</div>
          </div>
        </div>

        <!-- 底部操作：分享/导出已在 Hero 提供，此处不再重复；
             「重新生成报告」仅在 AI 复盘降级时出现（正常报告无需暴露） -->
        <div class="report-actions">
          <button v-if="reportReviewDegraded" class="report-btn" :disabled="analysisState.active" @click="handleRegenerateReport()">
            {{ analysisState.active ? '⏳ 报告生成中…' : '🔄 重新生成报告' }}
          </button>
          <button v-if="weakPointItems.length" class="report-btn" @click="addAllWeakToWrongBook">📚 薄弱点入错题本</button>
          <button class="report-btn primary" @click="startNewInterview">🚀 开始新的面试</button>
        </div>
      </div>
    </div>
  </div>
  <!-- 站点尾部放模板根级（vi-shell 外层，不受其水平 padding 挤压），
       宽度与首页一致（content-container 1280px 居中），全阶段统一显示 -->
  <SiteFooter />

<!-- 简历解析校对（U1 左右对照）——解析不落库，确认才落库 -->
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
  /* 视觉系统 · 跟随站点主题（light/dark/eye 由 html class 切换 --theme-* 变量） */
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

  /* 静态页 CSS 变量映射（逐题/改进/追问预警/表格 复用静态页固定色名） */
  --border: var(--gray-200);
  --sub: var(--gray-500);
  --primary-soft: var(--primary-bg);
  --red: var(--error);
  --red-soft: var(--error-bg);
  --amber: var(--warning);
  --amber-soft: var(--warning-bg);
  --green: var(--success);
  --green-soft: var(--success-bg);
  --border-2: var(--gray-200);
  --primary-2: var(--theme-primary-hover);

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

/* ==================== 断点续接横幅 ==================== */
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

/* ==================== 环境噪声检测结果 ==================== */
.noise-result { margin-top: 0.375rem; font-size: 0.8125rem; padding: 0.5rem 0.75rem; border-radius: var(--radius-md); background: var(--success-bg); color: var(--success); }
.noise-result.noisy { background: var(--warning-bg); color: var(--warning); border-left: 3px solid var(--warning); }

/* ==================== 岗位要求 JD 输入 ==================== */
.jd-item { margin-top: 0.75rem; }
.jd-textarea { width: 100%; min-height: 96px; padding: 0.625rem 0.875rem; border: 1px solid var(--gray-200); border-radius: var(--radius-md); font-size: 0.875rem; line-height: 1.6; resize: vertical; background: var(--theme-surface); color: var(--gray-800); font-family: var(--font-sans); }
.jd-textarea:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-bg); }
.jd-counter { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; font-size: 0.75rem; color: var(--gray-500); margin-top: 0.25rem; }
.jd-counter-actions { display: inline-flex; align-items: center; gap: 0.5rem; }
.jd-inline-btn { padding: 0.125rem 0.5rem; border: 1px solid var(--gray-200); background: var(--theme-bg-elevated); border-radius: var(--radius-sm); font-size: 0.75rem; color: var(--gray-600); cursor: pointer; transition: all 0.2s; }
.jd-inline-btn:hover { border-color: var(--primary); color: var(--primary); }
/* 岗位模板回填来源标记 */
.jd-source-badge { margin-left: 0.5rem; padding: 0.0625rem 0.5rem; border-radius: var(--radius-full); background: var(--primary-bg); color: var(--primary); font-size: 0.6875rem; font-weight: 500; }
/* 岗位下拉加载失败/为空的提示 */
.config-hint { margin-top: 0.375rem; font-size: 0.75rem; line-height: 1.5; color: var(--warning); }

/* ==================== 高级设置折叠 ==================== */
.advanced-card { padding: 0; overflow: hidden; }
.advanced-toggle { display: flex; align-items: center; justify-content: space-between; width: 100%; padding: 0.875rem 1.25rem; background: transparent; border: none; cursor: pointer; font-size: 0.9375rem; font-weight: 600; color: var(--gray-800); }
.advanced-toggle-title { display: flex; align-items: center; gap: 0.5rem; }
.advanced-toggle-state { font-size: 0.8125rem; font-weight: 500; color: var(--primary); }
.advanced-body { padding: 0.25rem 1.25rem 1.25rem; border-top: 1px dashed var(--gray-200); }

/* ==================== 准备进度覆盖层 ==================== */
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

/* ==================== 报告三段式（面试者简介 + 岗位信息） ==================== */
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

/* ==================== 综合得分等级徽章 + 概要入口 ==================== */
.score-level-badge { display: inline-block; margin-top: 0.375rem; padding: 0.1875rem 0.75rem; border-radius: var(--radius-full); font-size: 0.75rem; font-weight: 600; }
.score-level-badge.excellent { background: var(--success-bg); color: var(--success); }
.score-level-badge.good { background: var(--info-bg); color: var(--info); }
.score-level-badge.pass { background: var(--warning-bg); color: var(--warning); }
.score-level-badge.weak { background: var(--error-bg); color: var(--error); }
.detail-entry-btn { width: 100%; margin-top: 0.25rem; }

/* ==================== 报告总览横幅卡（综合分 + 等级 + LLM 总评 + 岗位匹配） ==================== */
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

/* ==================== 问题分析 Tab 薄弱点区块 ==================== */
.weak-points-card { background: var(--theme-bg-elevated); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); padding: 1rem 1.25rem; margin-bottom: 1.25rem; }
.weak-point-row { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.625rem 0.5rem; border-bottom: 1px dashed var(--gray-200); }
.weak-point-row:last-child { border-bottom: none; }
.weak-point-title { font-size: 0.875rem; font-weight: 600; color: var(--gray-800); }
.weak-point-quote { font-size: 0.8125rem; color: var(--gray-500); margin-top: 0.125rem; }
/* ===== 2026-10-12 报告评审升级：考察者视角新板块 ===== */
.report-degraded-banner { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.625rem 1rem; margin-bottom: 1rem; background: #fff7ed; border: 1px solid #fed7aa; border-radius: var(--radius-md); }
.report-degraded-text { font-size: 0.8125rem; color: #9a3412; }
.report-degraded-btn { flex-shrink: 0; padding: 0.375rem 0.875rem; background: #ea580c; color: #fff; border: none; border-radius: var(--radius-md); font-size: 0.8125rem; cursor: pointer; transition: opacity 0.2s; }
.report-degraded-btn:hover { opacity: 0.85; }
.verdict-card { display: flex; align-items: flex-start; gap: 0.75rem; padding: 18px 20px; margin-bottom: 16px; background: linear-gradient(135deg, rgba(59, 130, 246, 0.06), rgba(139, 92, 246, 0.06)); border: 1px solid var(--border); border-radius: 13px; }
.verdict-label { flex-shrink: 0; padding: 0.125rem 0.5rem; background: var(--primary); color: var(--theme-on-primary); border-radius: var(--radius-sm); font-size: 0.75rem; font-weight: 600; }
.verdict-text { font-size: 0.9375rem; font-weight: 600; color: var(--gray-800); line-height: 1.6; }
.competency-card { background: var(--theme-bg-elevated); border: 1px solid var(--border); border-radius: 13px; padding: 18px 20px; margin-bottom: 16px; }
.archive-block { border: 1px solid var(--border); border-radius: 13px; padding: 18px 20px; margin-bottom: 16px; background: var(--theme-bg-elevated); }
.archive-block-title { font-size: 14px; font-weight: 800; color: var(--gray-800); margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.sum-zone { border: 1px solid var(--border); border-radius: 13px; padding: 18px 20px; background: var(--theme-bg-elevated); }
.competency-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 0.875rem; }
.competency-item { padding: 0.75rem 0.875rem; background: var(--gray-50); border: 1px solid var(--gray-100); border-radius: var(--radius-md); }
.competency-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.375rem; }
.competency-label { font-size: 0.8125rem; font-weight: 600; color: var(--gray-700); }
.competency-score { font-size: 1rem; font-weight: 700; }
.competency-bar { height: 6px; background: var(--gray-200); border-radius: 3px; overflow: hidden; margin-bottom: 0.5rem; }
.competency-comment { font-size: 0.8125rem; color: var(--gray-600); line-height: 1.6; }
.competency-evidence { margin-top: 0.375rem; font-size: 0.75rem; color: var(--gray-400); }
.profile-paragraph { padding: 0.625rem 0.875rem; background: var(--gray-50); border-left: 3px solid var(--primary); border-radius: var(--radius-sm); }
.suggestion-action { font-size: 0.875rem; color: var(--gray-800); }
.suggestion-acceptance { margin-top: 0.25rem; font-size: 0.75rem; color: var(--gray-500); }
.suggestion-priority { flex-shrink: 0; align-self: flex-start; padding: 0.125rem 0.375rem; background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; border-radius: var(--radius-sm); font-size: 0.6875rem; font-weight: 600; }
.weak-point-card { padding: 0.75rem 0.875rem; border-bottom: 1px dashed var(--gray-200); }
.weak-point-card:last-child { border-bottom: none; }
.weak-point-head { display: flex; align-items: center; gap: 0.5rem; }
.weak-point-head .weak-point-title { flex: 1; }
.weak-point-head .practice-btn { flex-shrink: 0; }
.weak-type-badge { flex-shrink: 0; padding: 0.125rem 0.4375rem; border-radius: var(--radius-sm); font-size: 0.6875rem; font-weight: 600; }
.weak-type-badge.knowledge { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }
.weak-type-badge.expression { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
.weak-type-badge.depth { background: #faf5ff; color: #7e22ce; border: 1px solid #e9d5ff; }
.weak-attrib-row { display: flex; gap: 0.5rem; margin-top: 0.5rem; font-size: 0.8125rem; color: var(--gray-600); line-height: 1.6; }
.weak-attrib-label { flex-shrink: 0; padding: 0 0.375rem; background: var(--gray-100); color: var(--gray-500); border-radius: var(--radius-sm); font-size: 0.6875rem; font-weight: 600; height: fit-content; }
.weak-attrib-row.remedy { color: var(--gray-800); }
.weak-attrib-row.remedy .weak-attrib-label { background: #ecfdf5; color: #059669; }
.self-intro-tag { flex-shrink: 0; padding: 0.125rem 0.4375rem; background: #fff7ed; color: #c2410c; border: 1px solid #fed7aa; border-radius: var(--radius-sm); font-size: 0.6875rem; font-weight: 600; white-space: nowrap; }
.review-fourpart { margin-top: 0.625rem; padding: 0.625rem 0.75rem; background: var(--gray-50); border: 1px solid var(--gray-100); border-radius: var(--radius-md); }
.fourpart-row { display: flex; gap: 0.5rem; padding: 0.25rem 0; font-size: 0.8125rem; color: var(--gray-600); line-height: 1.6; }
.fourpart-row + .fourpart-row { border-top: 1px dashed var(--gray-200); }
.fourpart-label { flex-shrink: 0; width: 60px; text-align: center; padding: 0.0625rem 0; border-radius: var(--radius-sm); font-size: 0.6875rem; font-weight: 600; height: fit-content; }
.fourpart-label.intent { background: #eff6ff; color: #1d4ed8; }
.fourpart-label.framework { background: #f0fdf4; color: #15803d; }
.fourpart-label.gap { background: #fef2f2; color: #b91c1c; }
.fourpart-label.advice { background: #ecfdf5; color: #059669; }
.fourpart-text { flex: 1; }
.fourpart-framework { flex: 1; margin: 0; padding-left: 1.125rem; }
.fourpart-framework li { margin: 0.125rem 0; }
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


/* ==================== 简历库选择器 ==================== */
.resume-empty { border: 2px dashed var(--gray-200); border-radius: var(--radius-md); padding: 1.25rem; text-align: center; background: var(--gray-50); }
.empty-icon { font-size: 1.5rem; margin-bottom: 0.375rem; }
.empty-title { font-size: 0.9375rem; font-weight: 600; color: var(--gray-700); margin-bottom: 0.375rem; }
.empty-desc { font-size: 0.8125rem; color: var(--gray-400); margin-bottom: 1rem; }
.empty-actions { display: flex; justify-content: center; gap: 0.625rem; }



.resume-manage-row { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.625rem; flex-wrap: wrap; }
/* 简历收起/展开面板 */
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

/* --- 流式打字机光标 --- */
.streaming-cursor { display: inline-block; width: 2px; height: 1em; margin-left: 2px; vertical-align: text-bottom; background: var(--primary); animation: cursor-blink 0.8s steps(1) infinite; }
@keyframes cursor-blink { 50% { opacity: 0; } }

/* --- agent 选择提示 --- */
.agent-hint { margin-top: 0.625rem; font-size: 0.75rem; line-height: 1.5; color: var(--gray-400); background: var(--primary-bg); border-radius: var(--radius-md); padding: 0.5rem 0.75rem; }

/* --- 报告 AI 深度复盘 --- */
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
/* 六维评分标准图例（逐维说明评分依据 + 实时得分） */
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

/* 报告元信息条 */
.report-meta-bar { display: flex; flex-wrap: wrap; gap: 0.625rem; margin-bottom: 1.5rem; padding: 0.875rem 1.125rem; background: var(--theme-bg-elevated); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); }
.meta-chip { display: flex; align-items: center; gap: 0.375rem; padding: 0.3125rem 0.75rem; background: var(--gray-50); border-radius: var(--radius-full); font-size: 0.8125rem; }
.meta-label { color: var(--gray-500); }
.meta-value { color: var(--gray-900); font-weight: 600; max-width: 10rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 批量分析进度条（结束后轮询驱动） */
.report-analysis-progress { margin-bottom: 1.5rem; padding: 1rem 1.25rem; background: var(--theme-bg-elevated); border: 1px solid var(--gray-100); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.rap-header { display: flex; align-items: center; gap: 0.625rem; margin-bottom: 0.75rem; }
.rap-spinner { width: 1rem; height: 1rem; border: 2px solid var(--theme-primary-soft); border-top-color: var(--theme-primary); border-radius: var(--radius-full); animation: rap-spin 0.8s linear infinite; flex-shrink: 0; }
@keyframes rap-spin { to { transform: rotate(360deg); } }
.rap-step { font-size: 0.9375rem; font-weight: 600; color: var(--gray-900); }
.rap-num { margin-left: auto; font-size: 0.875rem; font-weight: 600; color: var(--theme-primary); font-variant-numeric: tabular-nums; }
.rap-track { height: 0.5rem; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; }
.rap-fill { height: 100%; background: linear-gradient(90deg, var(--theme-primary), var(--theme-accent-hover, var(--theme-primary))); border-radius: var(--radius-full); transition: width 0.6s ease; }
.rap-tip { margin-top: 0.625rem; font-size: 0.75rem; color: var(--gray-400); }

.report-tabs { display: flex; gap: 0.3125rem; margin-bottom: 1.75rem; background: var(--theme-bg-elevated); padding: 0.3125rem; border-radius: var(--radius-md); box-shadow: var(--shadow-sm); border: 1px solid var(--gray-100); width: 100%; overflow-x: auto; scrollbar-width: none; }
.report-tabs::-webkit-scrollbar { display: none; }
.report-tab { flex: 1; min-width: 88px; text-align: center; padding: 0.5625rem 0.8125rem; border: none; background: none; cursor: pointer; font-size: 0.8125rem; font-weight: 600; color: var(--gray-500); border-radius: 0.625rem; white-space: nowrap; transition: all 0.18s; user-select: none; }
.report-tab:hover { background: var(--gray-50); color: var(--gray-800); }
.report-tab.active { background: linear-gradient(135deg, var(--theme-primary), var(--theme-primary-hover)); color: #fff; box-shadow: 0 6px 14px -8px rgba(79, 70, 229, 0.7); }

/* ==================== 对话回放与历史保留 ==================== */
.replay-list { display: flex; flex-direction: column; gap: 1.25rem; }
.replay-item { display: flex; flex-direction: column; gap: 0.625rem; }
.replay-item + .replay-item { padding-top: 1.25rem; border-top: 1px dashed var(--gray-100); }
/* 播放模式：未点亮轮次隐藏，已点亮轮次淡入上移（650ms 间隔驱动，对齐 v7 交互规范） */
.replay-item.hidden { display: none; }
.replay-item.lit { animation: replayFadeIn 0.45s ease both; }
@keyframes replayFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: none; }
}
/* 回放播放控制条（▶ 从头播放 + 进度条 + N/M 计数器） */
.replay-controls {
  display: flex; align-items: center; gap: 0.75rem;
  padding: 0.625rem 0.875rem; margin-bottom: 1.25rem;
  background: var(--theme-bg-elevated);
  border: 1px solid var(--gray-100); border-radius: var(--radius-md);
}
.replay-play-btn {
  flex-shrink: 0; padding: 0.375rem 0.875rem; border: none; cursor: pointer;
  border-radius: var(--radius-sm); font-size: 0.8125rem; font-weight: 600;
  background: var(--primary-gradient, var(--primary)); color: var(--theme-on-primary, #fff);
}
.replay-play-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.replay-progress {
  flex: 1; height: 6px; border-radius: 99px;
  background: var(--gray-100); overflow: hidden;
}
.replay-progress-fill {
  height: 100%; border-radius: 99px;
  background: var(--primary-gradient, var(--primary));
  transition: width 0.45s cubic-bezier(0.4, 0, 0.2, 1);
}
.replay-counter { flex-shrink: 0; font-size: 0.75rem; font-weight: 600; color: var(--gray-500); font-variant-numeric: tabular-nums; }
/* 题源 / 自我介绍徽标（区别于橙色「追问」标签） */
.replay-tag.src { background: var(--info-bg, #eff6ff); color: var(--info, #3b82f6); }
.replay-tag.intro { background: var(--success-bg); color: var(--success); }
/* 面试背景 Tab：难度/题源行 */
.bg-meta-row { display: flex; flex-wrap: wrap; gap: 0.5rem 1.25rem; margin: 0.5rem 0 0.75rem; }
.bg-meta-item { font-size: 0.8125rem; color: var(--gray-600); }
/* 语音指标 Tab：指标卡网格 + 逐轮心态列表 */
.voice-metric-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 0.75rem; margin-bottom: 1rem; }
.voice-metric-card {
  padding: 0.875rem 1rem;
  background: var(--theme-bg-elevated);
  border: 1px solid var(--gray-100); border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}
.vm-label { font-size: 0.75rem; font-weight: 600; color: var(--gray-500); }
.vm-value { margin: 0.25rem 0 0.125rem; font-size: 1.5rem; font-weight: 800; color: var(--gray-800); line-height: 1.2; }
.vm-unit { font-size: 0.75rem; font-weight: 500; color: var(--gray-400); }
.vm-tip { font-size: 0.6875rem; color: var(--gray-400); }
.voice-sentiment-list { display: flex; flex-direction: column; gap: 0.5rem; }
.voice-sentiment-item { display: flex; align-items: center; gap: 0.625rem; font-size: 0.8125rem; color: var(--gray-600); }
.vs-round { width: 3.5rem; flex-shrink: 0; font-size: 0.75rem; color: var(--gray-400); }
.vs-label { font-weight: 600; }
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
.tab-content { display: none; }
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
/* 你的回答默认 2 行截断，点击展开全文 */
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
/* 概要 Tab 中栏改进建议块 */
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
  /* 横幅卡窄屏纵向堆叠（分隔线隐藏，匹配块铺满） */
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
  /* ==================== 三层五 tab ==================== */

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

  /* ==================== 概要 tab 四区 ==================== */

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

  /* ---- 资料卡（自我介绍评分 / 考察方向 / 语音信号共用） ---- */
  .archive-block {
    padding: 1rem 1.125rem;
    background: var(--theme-bg-elevated);
    border: 1px solid var(--gray-100); border-radius: var(--radius-md);
    margin-bottom: 1rem;
  }
  .archive-block-title {
    display: flex; align-items: baseline; justify-content: space-between; gap: 0.75rem;
    font-size: 0.875rem; font-weight: 700; color: var(--gray-700);
    margin-bottom: 0.75rem;
  }

  /* 水平定级徽章 */
  .level-estimate-badge {
    margin-left: 0.375rem; padding: 0.0625rem 0.4375rem;
    border-radius: var(--radius-sm); font-size: 0.6875rem; font-weight: 700;
    border: 1px solid var(--gray-200); color: var(--gray-500); background: var(--gray-50);
    cursor: help;
  }
  .level-estimate-badge.junior { border-color: var(--warning, #f59e0b); color: var(--warning); background: var(--warning-bg, #fffbeb); }
  .level-estimate-badge.mid { border-color: var(--primary, #3b82f6); color: var(--primary, #3b82f6); background: var(--info-bg, #eff6ff); }
  .level-estimate-badge.senior { border-color: var(--success, #22c55e); color: var(--success); background: var(--success-bg); }

  /* ---- Tab「🚀 改进建议」追问预警（未问卡右上徽标） ---- */
  .predict-unasked-badge {
    flex-shrink: 0; padding: 0.125rem 0.5rem; border-radius: var(--radius-full);
    font-size: 0.6875rem; font-weight: 700;
    background: var(--warning); color: var(--theme-on-primary);
  }
  .predict-intro {
    padding: 0.875rem 1rem;
    margin-bottom: 1rem;
    border-radius: var(--radius-md);
    background: var(--gray-50);
    border-left: 3px solid var(--primary, #3b82f6);
  }
  .predict-intro-desc { margin-top: 0.25rem; font-size: 0.75rem; color: var(--gray-500); line-height: 1.6; }
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

  /* ---- Tab「🧭 发展方向」 ---- */
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

  /* ---- Tab「🧭 发展方向」占位 ---- */
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

/* ==================== Tab「🔌 数据接口」+ advice 内发展方向/补强计划 ==================== */

/* ---- 通用卡片标题（api / advice 共用） ---- */
.api-card, .advice-insight-card, .advice-plan-card {
  margin-top: 1rem; padding: 1.25rem 1.5rem;
  background: var(--theme-surface); border: 1px solid var(--theme-border);
  border-radius: var(--radius-md); box-shadow: var(--shadow-sm);
}
.api-card-title, .advice-card-title {
  display: flex; align-items: center; gap: 0.5rem;
  font-size: 0.9375rem; font-weight: 700; color: var(--gray-800);
  margin-bottom: 1rem;
}
.api-bar {
  width: 3.5px; height: 16px; border-radius: 2px;
  background: linear-gradient(180deg, var(--theme-primary), var(--theme-primary-hover));
  flex-shrink: 0;
}
.api-tip { margin-left: auto; font-size: 0.7rem; font-weight: 400; color: var(--gray-500); }

/* ---- 接口清单 ---- */
.api-endpoint {
  display: flex; align-items: center; gap: 0.625rem;
  padding: 0.75rem 0.9375rem; border: 1px solid var(--theme-border);
  border-radius: var(--radius-sm); background: var(--gray-50);
  margin-bottom: 0.625rem; flex-wrap: wrap;
}
.api-endpoint:last-child { margin-bottom: 0; }
.api-method {
  font-size: 0.65rem; font-weight: 800; padding: 0.1875rem 0.5625rem;
  border-radius: 0.375rem; letter-spacing: 0.5px; flex-shrink: 0;
}
.api-method.m-post { background: #dcfce7; color: #15803d; }
.api-method.m-get { background: #dbeafe; color: #1d4ed8; }
.api-path {
  font-family: var(--font-mono); font-size: 0.8125rem; color: var(--gray-800);
  word-break: break-all;
}
.api-opt {
  font-size: 0.65rem; padding: 0.125rem 0.4375rem; border-radius: 0.3125rem;
  background: var(--gray-100); color: var(--gray-500);
  border: 1px solid var(--gray-200); flex-shrink: 0;
}
.api-desc { margin-left: auto; font-size: 0.7rem; color: var(--gray-500); text-align: right; }
@media (max-width: 760px) { .api-desc { margin-left: 0; text-align: left; width: 100%; } }

/* ---- 代码块 + 复制按钮 ---- */
.api-code-wrap { position: relative; }
.api-copy-btn {
  position: absolute; top: 0.625rem; right: 0.75rem; z-index: 2;
  font-size: 0.7rem; padding: 0.3125rem 0.6875rem; border-radius: 0.4375rem;
  background: rgba(255,255,255,.12); color: #cbd5e1;
  border: 1px solid rgba(255,255,255,.18); transition: background .18s;
}
.api-copy-btn:hover { background: rgba(255,255,255,.22); color: #fff; }
.api-copy-btn.done { background: var(--success); color: #fff; border-color: var(--success); }
.api-code-box {
  background: #0f172a; color: #e2e8f0; border-radius: var(--radius-sm);
  padding: 1rem 1.125rem; overflow: auto; margin: 0;
  font-family: var(--font-mono); font-size: 0.75rem; line-height: 1.75;
  max-height: 30rem; white-space: pre;
}
.api-prompt-box {
  background: var(--gray-50); border: 1px solid var(--theme-border);
  border-radius: var(--radius-sm); padding: 1rem 1.125rem;
  font-size: 0.8125rem; line-height: 1.9; color: var(--gray-800);
  white-space: pre-wrap; max-height: 32rem; overflow: auto;
}

/* ---- 字段说明表 ---- */
.api-field-table, .api-plan-table {
  width: 100%; border-collapse: collapse; font-size: 0.8125rem;
}
.api-field-table th, .api-plan-table th {
  text-align: left; font-size: 0.7rem; color: var(--gray-500);
  font-weight: 700; padding: 0.5625rem 0.6875rem;
  background: var(--gray-50); border-bottom: 1px solid var(--theme-border);
}
.api-field-table td, .api-plan-table td {
  padding: 0.625rem 0.6875rem; border-bottom: 1px solid var(--gray-200);
  vertical-align: top; line-height: 1.7;
}
.api-field-table tr:last-child td, .api-plan-table tr:last-child td { border-bottom: none; }
.fd-name { font-family: var(--font-mono); color: var(--theme-primary); font-size: 0.7rem; white-space: nowrap; }
.fd-type { color: var(--gray-500); white-space: nowrap; font-size: 0.7rem; }
.fd-desc { color: var(--gray-700); }
.ai-tag { font-size: 0.6rem; margin-left: 0.3125rem; }
.plan-day { font-weight: 700; color: var(--theme-primary); white-space: nowrap; }

/* ---- advice 内发展方向卡 ---- */
.advice-insight-card { border-left: 3px solid var(--theme-primary); }
.advice-insight-lead { font-size: 0.8125rem; color: var(--gray-500); line-height: 1.8; margin-bottom: 0.75rem; }
.advice-plan-card { border-left: 3px solid var(--success); }

/* ==================== 语音对话可视化增强 ==================== */

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
/* AI 面试官形象图（与首页 Hero 轮播同一张，保持形象一致） */
.interviewer-photo {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: inherit;
  user-select: none;
}
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

/* ========== 卡片（对齐静态页 .card/.card-title） ========== */
.card { border: 1px solid var(--border); border-radius: 13px; padding: 18px 20px; margin-bottom: 16px; background: var(--theme-bg-elevated); }
.card-title { font-size: 14px; font-weight: 800; display: flex; align-items: center; gap: 8px; margin-bottom: 14px; color: var(--gray-800); }
.card-title .bar { width: 4px; height: 16px; border-radius: 2px; background: linear-gradient(180deg, var(--primary), var(--primary-2)); flex-shrink: 0; }
.card-title .tip { font-size: 11.5px; font-weight: 500; color: var(--sub); margin-left: auto; }
.pb-self { font-size: 11px; font-weight: 700; color: var(--primary); background: var(--primary-soft); border-radius: 6px; padding: 2px 8px; flex-shrink: 0; }

/* ========== 逐题分析（对齐静态页 .qf/.chip/.q） ========== */
.qf { display: flex; gap: 7px; flex-wrap: wrap; margin-bottom: 14px; }
.chip { font-size: 12px; padding: 5px 13px; border-radius: 99px; background: var(--gray-50); color: var(--gray-600); border: 1px solid transparent; transition: 0.18s; font-weight: 500; cursor: pointer; }
.chip:hover { background: var(--gray-100); }
.chip.active { background: var(--primary-soft); color: var(--primary); border-color: var(--primary-border); font-weight: 700; }
.q { border: 1px solid var(--border); border-radius: 13px; margin-bottom: 12px; overflow: hidden; transition: 0.2s; background: var(--theme-bg-elevated); }
.q:hover { border-color: var(--primary-border); }
.q-head { display: flex; justify-content: space-between; align-items: center; gap: 13px; padding: 15px 18px; cursor: pointer; user-select: none; }
.q-title { font-size: 13.5px; font-weight: 600; display: flex; gap: 9px; align-items: flex-start; flex: 1; line-height: 1.65; }
.q-no { background: var(--primary-soft); color: var(--primary); font-size: 11.5px; font-weight: 800; border-radius: 6px; padding: 2px 8px; flex-shrink: 0; margin-top: 2px; }
.q-right { display: flex; align-items: center; gap: 9px; flex-shrink: 0; }
.q-score { font-size: 12.5px; font-weight: 800; padding: 3px 10px; border-radius: 7px; white-space: nowrap; }
.sc-low { background: var(--red-soft); color: var(--red); border: 1px solid var(--red); }
.sc-mid { background: var(--amber-soft); color: var(--amber); border: 1px solid var(--amber); }
.sc-high { background: var(--green-soft); color: var(--green); border: 1px solid var(--green); }
.q-arrow { font-size: 10px; color: var(--sub); transition: transform 0.3s; }
.q.open .q-arrow { transform: rotate(180deg); }
.q-body { display: none; padding: 0 18px 18px; animation: qfade 0.28s ease; }
.q.open .q-body { display: block; }
@keyframes qfade { from { opacity: 0; transform: translateY(-5px); } to { opacity: 1; transform: none; } }
.q-block { margin-top: 14px; }
.q-block .bk { font-size: 11.5px; font-weight: 800; color: var(--sub); letter-spacing: 0.4px; margin-bottom: 5px; display: block; }
.q-block .qa-intent { font-size: 12.5px; color: var(--gray-800); line-height: 1.8; }
.q-block .qa-fw { margin: 0; padding-left: 18px; }
.q-block .qa-fw li { font-size: 12.5px; color: var(--gray-600); line-height: 1.8; }
.answer { background: var(--gray-50); border-left: 3px solid var(--gray-300); border-radius: 0 8px 8px 0; padding: 10px 13px; color: var(--gray-600); font-size: 12.5px; line-height: 1.8; }
.comment { color: var(--gray-800); font-size: 12.5px; line-height: 1.8; }
.suggest { background: var(--primary-soft); border-radius: 9px; padding: 11px 13px; font-size: 12.5px; color: var(--primary); line-height: 1.8; }
.tagrow { display: flex; gap: 5px; flex-wrap: wrap; margin-top: 11px; }
.tag { font-size: 11px; padding: 2px 8px; border-radius: 99px; background: var(--gray-50); color: var(--gray-600); border: 1px solid var(--gray-200); }
.tag.red { background: var(--red-soft); color: var(--red); border-color: var(--red); }
.tag.green { background: var(--green-soft); color: var(--green); border-color: var(--green); }
.tag.amber { background: var(--amber-soft); color: var(--amber); border-color: var(--amber); }

/* ========== 改进建议（对齐静态页 .dev-item） ========== */
.dev-lead { font-size: 13px; color: var(--sub); margin-bottom: 16px; line-height: 1.8; }
.dev-item { border: 1px solid var(--border); border-radius: 13px; padding: 16px 18px; margin-bottom: 12px; background: var(--theme-bg-elevated); transition: 0.2s; position: relative; overflow: hidden; }
.dev-item:hover { border-color: var(--primary-border); box-shadow: 0 8px 22px -18px rgba(79,70,229,0.7); }
.dev-item::before { content: ""; position: absolute; left: 0; top: 0; bottom: 0; width: 3px; }
.dev-item.p1::before { background: linear-gradient(180deg, #e11d48, #fb7185); }
.dev-item.p2::before { background: linear-gradient(180deg, #d97706, #fbbf24); }
.dev-item.p3::before { background: linear-gradient(180deg, #4f46e5, #a855f7); }
.dev-head { display: flex; align-items: center; gap: 10px; margin-bottom: 9px; flex-wrap: wrap; }
.dev-rank { width: 24px; height: 24px; border-radius: 8px; display: grid; place-items: center; font-size: 12px; font-weight: 800; color: #fff; flex-shrink: 0; }
.p1 .dev-rank { background: linear-gradient(135deg, #e11d48, #fb7185); }
.p2 .dev-rank { background: linear-gradient(135deg, #d97706, #fbbf24); }
.p3 .dev-rank { background: linear-gradient(135deg, #4f46e5, #a855f7); }
.dev-head h5 { font-size: 14px; font-weight: 800; margin: 0; }
.dev-gain { margin-left: auto; font-size: 11.5px; font-weight: 800; color: var(--green); background: var(--green-soft); padding: 3px 10px; border-radius: 99px; border: 1px solid var(--green); }
.dev-row { display: flex; gap: 9px; font-size: 12.5px; margin-top: 7px; line-height: 1.75; }
.dev-row .k { flex-shrink: 0; width: 58px; color: var(--sub); font-weight: 700; }
.dev-row .v { flex: 1; color: var(--gray-800); }

/* ========== 追问预警（对齐静态页 .pq-item） ========== */
.pq-item { border: 1px solid var(--border-2); border-radius: 11px; padding: 12px 14px; margin-bottom: 10px; background: var(--theme-bg-elevated); }
.pq-item.pq-asked { background: var(--gray-50); opacity: 0.85; }
.pq-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 7px; }
.pq-kp { font-size: 11px; font-weight: 700; color: var(--primary); background: var(--primary-soft); border-radius: 99px; padding: 2px 10px; }
.pq-state { font-size: 11px; font-weight: 700; color: var(--amber); background: var(--amber-soft); border: 1px solid var(--amber); border-radius: 99px; padding: 2px 10px; }
.pq-asked .pq-state { color: var(--green); background: var(--green-soft); border-color: var(--green); }
.pq-q { font-size: 13px; font-weight: 700; color: var(--gray-800); line-height: 1.7; margin-bottom: 6px; }
.pq-a { font-size: 12px; color: var(--gray-600); line-height: 1.8; margin-top: 3px; }
.pq-a .k { font-weight: 800; color: var(--sub); margin-right: 7px; }

/* ========== 7天补强计划表格（对齐静态页 table） ========== */
.plan-table { width: 100%; border-collapse: collapse; font-size: 12.5px; }
.plan-table th { text-align: left; font-size: 11.5px; color: var(--sub); font-weight: 700; padding: 9px 11px; background: var(--gray-50); border-bottom: 1px solid var(--border); }
.plan-table td { padding: 10px 11px; border-bottom: 1px solid var(--gray-100); vertical-align: top; line-height: 1.7; }
.plan-table tr:last-child td { border-bottom: none; }
.plan-table td.day { font-weight: 800; color: var(--primary); white-space: nowrap; }

/* ========== 概览评分 Tab（能力雷达 + 维度明细 + 优势/待改进） ========== */
/* --- 能力雷达标题区（按雷达图规范） ---
   竖条 4×18px 圆角 2px #7c3aed；标题 18px 加粗 #1f2937；副标题右对齐 13px #9ca3af --- */
.report-page .radar-card-title {
  gap: 10px;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 800;
  color: #1f2937;
}
.report-page .radar-card-title .bar {
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: #7c3aed;
}
.report-page .radar-card-title .tip {
  margin-left: auto;
  font-size: 13px;
  font-weight: 400;
  color: #9ca3af;
  text-align: right;
}
/* 总分（移入右侧 meta 面板） */
.overview-total-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.overview-total-label {
  font-size: 12px;
  color: var(--sub);
}
.overview-total-row strong {
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.overview-total-unit {
  font-size: 12px;
  color: var(--sub);
}

.overview-total { margin-left: auto; font-size: 13px; font-weight: 600; color: var(--sub); }
.overview-total strong { font-size: 18px; }
.overview-radar-wrap { display: flex; align-items: center; gap: 28px; flex-wrap: wrap; }
.overview-radar { flex-shrink: 0; }
.overview-radar-canvas { display: block; width: 100%; max-width: 420px; height: 360px; }
.overview-radar-meta { flex: 1; min-width: 200px; display: flex; flex-direction: column; gap: 12px; }
.overview-badges { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.overview-match { display: flex; align-items: center; gap: 8px; }
.overview-match-label { flex-shrink: 0; font-size: 12px; color: var(--sub); }
.overview-match-bar { flex: 1; max-width: 200px; }
.overview-match-value { font-size: 13px; font-weight: 700; color: var(--gray-700); }
.overview-verdict { display: flex; align-items: flex-start; gap: 8px; padding: 10px 14px; background: linear-gradient(135deg, rgba(59,130,246,0.06), rgba(139,92,246,0.06)); border-radius: 9px; }
.card-title .bar.green { background: var(--success); }
.card-title .bar.amber { background: var(--warning); }

/* 维度评分明细 */
.dim-list { }
.dim-row { padding: 10px 0; border-bottom: 1px solid var(--gray-100); }
.dim-row:last-child { border-bottom: none; }
.dim-row-head { display: flex; align-items: center; gap: 12px; }
.dim-name { width: 72px; flex-shrink: 0; font-size: 13px; font-weight: 700; color: var(--gray-800); }
.dim-bar { flex: 1; height: 8px; background: var(--gray-100); border-radius: 99px; overflow: hidden; }
.dim-fill { height: 100%; border-radius: 99px; transition: width 0.5s ease; }
.dim-fill.low { background: var(--error); }
.dim-fill.mid { background: var(--warning); }
.dim-fill.high { background: var(--success); }
.dim-score { width: 32px; flex-shrink: 0; text-align: right; font-size: 15px; font-weight: 800; font-variant-numeric: tabular-nums; }
.dim-score.low { color: var(--error); }
.dim-score.mid { color: var(--warning); }
.dim-score.high { color: var(--success); }
.dim-note { font-size: 12px; color: var(--gray-600); line-height: 1.7; margin-top: 5px; padding-left: 84px; }
.dim-evidence { font-size: 11px; color: var(--gray-400); margin-top: 3px; padding-left: 84px; }

/* 核心优势与待改进项（左右并排） */
.overview-pros-cons { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.overview-summary { padding: 10px 12px; background: var(--gray-50); border-radius: 9px; margin-bottom: 12px; }
.pros-card { border-left: 3px solid var(--success); }
.cons-card { border-left: 3px solid var(--warning); }
.pros-item, .cons-item { display: flex; align-items: flex-start; gap: 8px; margin-bottom: 4px; }
.pros-check { flex-shrink: 0; font-size: 14px; line-height: 1.6; }
.cons-warn { flex-shrink: 0; font-size: 14px; line-height: 1.6; }
.pros-item-body, .cons-item-body { flex: 1; }
.pros-item-title, .cons-item-title { font-size: 13px; font-weight: 700; color: var(--gray-800); line-height: 1.5; }
.pros-item-quote, .cons-item-quote { font-size: 12px; color: var(--gray-600); line-height: 1.7; margin-top: 2px; }

@media (max-width: 768px) {
  .overview-pros-cons { grid-template-columns: 1fr; }
  .overview-radar-wrap { flex-direction: column; }
  .dim-name { width: 60px; }
  .dim-note, .dim-evidence { padding-left: 0; }
}

/* ====================================================================
   报告页视觉重构（严格对齐静态页 面试报告v2.html）
   —— 局部覆写 --primary 系列为紫色：项目全局主色是红（--theme-primary），
      而静态页的报告页视觉基调是紫（#4f46e5 / #7c3aed），故仅在报告页内覆写，
      不影响站点其它页面；语义色（success/warning/danger）仍跟随站点三主题。
   ==================================================================== */
.report-page {
  --primary: #4f46e5;
  --primary-2: #7c3aed;
  --primary-soft: #eef2ff;
  --border: #e8eaef;
  --border-2: #eef0f6;
  --sub: #6b7280;
  --amber: #d97706;
  --amber-soft: #fffbeb;
  --green: #059669;
  --green-soft: #ecfdf5;
  --red: #e11d48;
  --red-soft: #fff1f3;
  --rpt-card-bg: #fff;
  --rpt-text: #1f2937;
  --rpt-text-2: #374151;
  --rpt-border: #e8eaef;
}

/* ---------- 紫色 Hero ---------- */
.rpt-hero {
  position: relative;
  overflow: hidden;
  padding: 30px 32px 26px;
  border-radius: 22px;
  color: #fff;
  background: linear-gradient(135deg, #5b4bd6 0%, #7c3aed 50%, #a855f7 100%);
  box-shadow: 0 14px 36px -16px rgba(91, 75, 214, 0.65);
}
.rpt-hero::after {
  content: "";
  position: absolute;
  right: -80px;
  top: -80px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}
.rpt-hero::before {
  content: "";
  position: absolute;
  right: 90px;
  bottom: -120px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
}
.rpt-hero-top {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}
.rpt-hero h1 {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.5px;
  line-height: 1.35;
}
.rpt-hero-sub {
  font-size: 12.5px;
  opacity: 0.85;
  margin-top: 4px;
  letter-spacing: 0.3px;
}
.rpt-hero-actions {
  display: flex;
  gap: 9px;
  flex-wrap: wrap;
}
.rpt-btn-glass {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 15px;
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 600;
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(6px);
  transition: background 0.2s ease;
}
.rpt-btn-glass:hover {
  background: rgba(255, 255, 255, 0.3);
}
.rpt-btn-glass:disabled {
  opacity: 0.6;
  cursor: wait;
}

.rpt-hero-meta {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
  margin-top: 26px;
}
.rpt-meta-item {
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
}
.rpt-meta-label {
  font-size: 11px;
  opacity: 0.82;
  letter-spacing: 0.4px;
}
.rpt-meta-value {
  font-size: 14.5px;
  font-weight: 700;
  margin-top: 3px;
  word-break: break-all;
}

/* ---------- 评分卡（上浮与 Hero 重叠） ---------- */
.rpt-score-card {
  position: relative;
  z-index: 3;
  margin-top: -14px;
  padding: 24px 28px;
  border-radius: 20px;
  background: var(--rpt-card-bg);
  border: 1px solid var(--rpt-border);
  box-shadow: 0 8px 24px -18px rgba(16, 24, 40, 0.3);
}
.rpt-score-flex {
  display: flex;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
}
.rpt-score-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 104px;
}
.rpt-score-num {
  font-size: 56px;
  font-weight: 900;
  line-height: 0.95;
  letter-spacing: -2px;
  color: var(--red);
  font-variant-numeric: tabular-nums;
}
.rpt-score-num.mid {
  color: var(--amber);
}
.rpt-score-num.high {
  color: var(--green);
}
.rpt-score-label {
  font-size: 12px;
  color: var(--sub);
  margin-top: 5px;
}
.rpt-score-badge {
  margin-top: 8px;
  padding: 3px 12px;
  border-radius: 99px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--red);
  background: var(--red-soft);
  border: 1px solid #fecdd3;
}
.rpt-score-badge.mid {
  color: var(--amber);
  background: var(--amber-soft);
  border-color: #fde68a;
}
.rpt-score-badge.high {
  color: var(--green);
  background: var(--green-soft);
  border-color: #a7f3d0;
}
.rpt-level-badge {
  margin-top: 6px;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-soft);
  border: 1px solid #c7d2fe;
}
.rpt-score-desc {
  flex: 1;
  min-width: 260px;
  font-size: 13.5px;
  color: var(--rpt-text-2);
  line-height: 1.85;
}
.rpt-score-muted {
  display: block;
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--sub);
}
.rpt-match-block {
  min-width: 132px;
  text-align: right;
}
.rpt-match-label {
  font-size: 12px;
  color: var(--sub);
}
.rpt-match-num {
  font-size: 32px;
  font-weight: 900;
  line-height: 1.1;
  color: var(--red);
  font-variant-numeric: tabular-nums;
}
.rpt-match-num.mid {
  color: var(--amber);
}
.rpt-match-num.high {
  color: var(--green);
}
.rpt-match-bar {
  height: 6px;
  margin-top: 8px;
  border-radius: 99px;
  background: #eef0f6;
  overflow: hidden;
}
.rpt-match-bar i {
  display: block;
  height: 100%;
  width: 0;
  border-radius: 99px;
  background: linear-gradient(90deg, #fb7185, #e11d48);
  transition: width 1.1s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ---------- Tab 栏（白底胶囊容器 + 激活紫色渐变） ---------- */
.report-page .report-tabs {
  display: flex;
  gap: 5px;
  margin-top: 18px;
  padding: 5px;
  border-radius: 14px;
  background: var(--rpt-card-bg);
  border: 1px solid var(--rpt-border);
  overflow-x: auto;
  scrollbar-width: none;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}
.report-page .report-tabs::-webkit-scrollbar {
  display: none;
}
.report-page .report-tab {
  flex: 1;
  min-width: 88px;
  text-align: center;
  white-space: nowrap;
  padding: 9px 13px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--sub);
  transition: all 0.18s ease;
  user-select: none;
}
.report-page .report-tab:hover {
  background: #f6f7fb;
  color: var(--rpt-text);
}
.report-page .report-tab.active {
  color: #fff;
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  box-shadow: 0 6px 14px -8px rgba(79, 70, 229, 0.7);
}

/* ---------- 内容卡片（对齐静态页 .card） ---------- */
.report-page .card {
  margin-top: 16px;
  padding: 22px 24px;
  border-radius: 16px;
  background: var(--rpt-card-bg);
  border: 1px solid var(--rpt-border);
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.04);
}
.report-page .card-title {
  margin-bottom: 16px;
  font-size: 15px;
  font-weight: 800;
}
.report-page .card-title .bar {
  width: 3.5px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--primary), #a855f7);
}
.report-page .card-title .tip {
  font-size: 11.5px;
  font-weight: 400;
  color: var(--sub);
  margin-left: auto;
}

/* ---------- 响应式 ---------- */
@media (max-width: 820px) {
  .rpt-hero {
    padding: 24px 20px 22px;
  }
  .rpt-score-card {
    padding: 20px 18px;
  }
  .rpt-match-block {
    min-width: 0;
    text-align: left;
  }
  .rpt-match-bar {
    max-width: 200px;
  }
}
@media (max-width: 760px) {
  .rpt-hero h1 {
    font-size: 19px;
  }
  .rpt-score-flex {
    gap: 20px;
  }
  .rpt-score-num {
    font-size: 46px;
  }
}

/* ====================================================================
   面包屑（三页统一：准备页 / 过程页 / 报告页）
   对齐静态页 面试过程页v2.html 的 .breadcrumb 规范：
   紫色悬停胶囊 + 灰色分隔符 › + 当前页深色加粗。
   注：Breadcrumb 组件用 Tailwind 类 + 内联 style 渲染，
       故用 nav[aria-label] 提高特异性以覆盖内联色值。
   ==================================================================== */
.prep-crumb,
.proc-crumb,
.rpt-crumb {
  display: block;
  margin-bottom: 12px;
  font-size: 13px;
}
.prep-crumb nav[aria-label],
.proc-crumb nav[aria-label],
.rpt-crumb nav[aria-label] {
  display: flex;
  align-items: center;
  gap: 2px;
  overflow: hidden;
  color: #6b7280;
  font-size: 13px;
}
/* 可点击层：紫色悬停胶囊 */
.prep-crumb nav[aria-label] > button,
.proc-crumb nav[aria-label] > button,
.rpt-crumb nav[aria-label] > button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 7px;
  border-radius: 7px;
  color: #6b7280;
  font-weight: 500;
  white-space: nowrap;
  transition: all 0.18s ease;
}
.prep-crumb nav[aria-label] > button:hover,
.proc-crumb nav[aria-label] > button:hover,
.rpt-crumb nav[aria-label] > button:hover {
  color: #4f46e5;
  background: #eef2ff;
  opacity: 1;
}
/* 图标 */
.prep-crumb nav[aria-label] svg,
.proc-crumb nav[aria-label] svg,
.rpt-crumb nav[aria-label] svg {
  width: 13px;
  height: 13px;
  color: #9ca3af;
}
/* 分隔符（ChevronRight 图标）在 button/span 之后 */
.prep-crumb nav[aria-label] > svg,
.proc-crumb nav[aria-label] > svg,
.rpt-crumb nav[aria-label] > svg {
  width: 13px;
  height: 13px;
  color: #c7cbd8;
  flex-shrink: 0;
}
/* 当前页（最后一个 span） */
.prep-crumb nav[aria-label] > span,
.proc-crumb nav[aria-label] > span,
.rpt-crumb nav[aria-label] > span {
  padding: 3px 6px;
  color: #1f2937;
  font-weight: 700;
  white-space: nowrap;
}
@media (max-width: 760px) {
  .prep-crumb,
  .proc-crumb,
  .rpt-crumb {
    margin-bottom: 9px;
  }
  /* 窄屏只保留「首页 › 当前页」：隐藏中间层与其后的分隔符 */
  .prep-crumb nav[aria-label] > *:nth-child(3),
  .prep-crumb nav[aria-label] > *:nth-child(4),
  .proc-crumb nav[aria-label] > *:nth-child(3),
  .proc-crumb nav[aria-label] > *:nth-child(4),
  .rpt-crumb nav[aria-label] > *:nth-child(3),
  .rpt-crumb nav[aria-label] > *:nth-child(4) {
    display: none;
  }
}

/* ====================================================================
   准备页视觉重构（严格对齐静态页 面试准备页v2.html）
   —— 局部覆写 --primary 系列为紫色（同报告页口径），
      页面级径向光晕背景 + 卡片/步骤条/设备行/表单/主按钮全部按静态页重绘。
   ==================================================================== */
.prep-page {
  --primary: #4f46e5;
  --primary-2: #7c3aed;
  --primary-soft: #eef2ff;
  --green: #059669;
  --green-soft: #ecfdf5;
  --amber: #d97706;
  --amber-soft: #fffbeb;
  --red: #e11d48;
  --red-soft: #fff1f3;
  --red-2: #f43f5e;
  --text: #1f2937;
  --sub: #6b7280;
  --muted: #9ca3af;
  --border: #e8eaef;
  --border-2: #eef0f6;
  --card: #fff;
  --rpt-shadow-sm: 0 1px 3px rgba(16, 24, 40, 0.04);
  --rpt-shadow-md: 0 4px 16px -8px rgba(16, 24, 40, 0.12);

  background-color: #f5f6f9;
  background-image:
    radial-gradient(circle at 15% 0%, rgba(124, 58, 237, 0.05), transparent 45%),
    radial-gradient(circle at 85% 100%, rgba(79, 70, 229, 0.05), transparent 45%);
  background-attachment: fixed;
}

/* ---------- 顶行：面包屑（左） + 历史入口（右） ---------- */
.prep-page .prep-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}
.prep-page .prep-topline .prep-crumb {
  margin-bottom: 0;
}

/* ---------- 顶部工具行 ---------- */
.prep-page .prep-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.prep-page .prep-toolbar-main {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}
.prep-page .prep-toolbar-main::before {
  content: "旭";
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 16px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  box-shadow: 0 8px 18px -8px rgba(79, 70, 229, 0.85);
}
.prep-page .prep-toolbar-title {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.2px;
  line-height: 1.3;
  color: var(--text);
}
.prep-page .prep-toolbar-desc {
  font-size: 11.5px;
  color: var(--sub);
  margin-top: 1px;
}
.prep-page .prep-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
}
.prep-page .prep-ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 9px 18px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  color: #374151;
  background: #fff;
  border: 1px solid var(--border);
  transition: all 0.2s ease;
}
.prep-page .prep-ghost-btn:hover {
  background: #f7f8fb;
  border-color: #d5d9e2;
  transform: translateY(-1px);
}

/* ---------- 步骤指示器（渐变卡 + 涟漪 + 流动光条） ---------- */
.prep-page .prep-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 22px 30px;
  margin-bottom: 18px;
  flex-wrap: wrap;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: linear-gradient(135deg, #fff 0%, #fbfbff 100%);
  box-shadow: var(--rpt-shadow-sm);
}
.prep-page .prep-step {
  display: flex;
  align-items: center;
  gap: 11px;
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  transition: all 0.3s ease;
}
.prep-page .step-circle {
  position: relative;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12.5px;
  font-weight: 800;
  color: #9ca3af;
  background: #f3f4f8;
  transition: all 0.3s ease;
}
.prep-page .step-circle.completed {
  color: #fff;
  background: var(--green);
  box-shadow: 0 4px 10px -4px rgba(5, 150, 105, 0.6);
}
.prep-page .step-circle.active {
  color: #fff;
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  box-shadow: 0 6px 14px -4px rgba(79, 70, 229, 0.75);
}
.prep-page .step-circle.active::before {
  content: "";
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(79, 70, 229, 0.28);
  animation: prep-ripple 2s infinite;
}
@keyframes prep-ripple {
  0% {
    transform: scale(1);
    opacity: 0.7;
  }
  100% {
    transform: scale(1.35);
    opacity: 0;
  }
}
.prep-page .step-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted);
  white-space: nowrap;
  transition: all 0.3s ease;
}
.prep-page .step-label.active {
  color: var(--text);
  font-weight: 600;
}
.prep-page .step-connector {
  flex: 1;
  height: 2px;
  min-width: 40px;
  margin: 0 18px;
  border-radius: 99px;
  background: var(--border-2);
  overflow: hidden;
}
.prep-page .step-connector.completed {
  background: var(--green);
}

/* ---------- 卡片 ---------- */
.prep-page .prep-card {
  padding: 24px 26px;
  margin-bottom: 16px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: var(--card);
  box-shadow: var(--rpt-shadow-sm);
  transition: all 0.25s ease;
}
.prep-page .prep-card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  font-size: 15px;
  font-weight: 800;
  color: var(--text);
}
.prep-page .prep-card-title::before {
  content: "";
  width: 4px;
  height: 17px;
  flex-shrink: 0;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--primary), #a855f7);
}
.prep-page .prep-card-title .step-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  box-shadow: 0 4px 10px -4px rgba(79, 70, 229, 0.6);
}
.prep-page .prep-card-title .tip {
  margin-left: auto;
  font-size: 11.5px;
  font-weight: 400;
  color: var(--sub);
}

/* ---------- 设备检测行 ---------- */
.prep-page .device-check-list {
  display: grid;
  gap: 11px;
}
.prep-page .device-item {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  padding: 15px 18px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  transition: all 0.25s ease;
}
.prep-page .device-item::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--green);
  opacity: 0;
  transition: opacity 0.25s ease;
}
.prep-page .device-item:hover {
  border-color: #c7d2fe;
  background: linear-gradient(90deg, #fafbff, #fff);
  transform: translateX(2px);
  box-shadow: var(--rpt-shadow-md);
}
.prep-page .device-item:hover::before {
  opacity: 1;
}
.prep-page .device-info {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 130px;
}
.prep-page .device-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border: none;
  border-radius: 11px;
  display: grid;
  place-items: center;
  font-size: 17px;
  background: linear-gradient(135deg, #f3f4f8, #eceef6);
  transition: all 0.25s ease;
}
.prep-page .device-item:hover .device-icon {
  background: linear-gradient(135deg, var(--primary-soft), #e0e7ff);
  transform: scale(1.05);
}
.prep-page .device-name {
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text);
}
.prep-page .device-status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.prep-page .status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 99px;
  font-size: 11.5px;
  font-weight: 700;
  white-space: nowrap;
}
.prep-page .status-badge.ok {
  color: var(--green);
  background: var(--green-soft);
  border: 1px solid #a7f3d0;
}
.prep-page .status-badge.checking,
.prep-page .status-badge.warn {
  color: var(--amber);
  background: var(--amber-soft);
  border: 1px solid #fde68a;
}
.prep-page .status-badge.error {
  color: var(--red);
  background: var(--red-soft);
  border: 1px solid #fecdd3;
}
.prep-page .test-btn {
  padding: 8px 16px;
  border-radius: 9px;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  color: #374151;
  background: #fff;
  border: 1px solid var(--border);
  transition: all 0.2s ease;
}
.prep-page .test-btn:hover:not(:disabled) {
  background: #f7f8fb;
  border-color: #c7d2fe;
  color: var(--primary);
}
.prep-page .test-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.prep-page .device-select {
  min-width: 190px;
  flex-shrink: 0;
  padding: 8px 13px;
  border-radius: 9px;
  border: 1px solid var(--border);
  background: #fff;
  font-size: 12.5px;
  color: #4b5563;
  cursor: pointer;
  transition: all 0.2s ease;
}
.prep-page .device-select:hover {
  border-color: #c7d2fe;
}
.prep-page .device-tip {
  display: flex;
  gap: 11px;
  padding: 13px 16px;
  margin-top: 16px;
  border-radius: 12px;
  font-size: 12.5px;
  line-height: 1.75;
  color: #92400e;
  background: linear-gradient(135deg, #fffbeb, #fff7ed);
  border: 1px solid #fde68a;
}
.prep-page .noise-result {
  margin-top: 6px;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 12.5px;
  color: #047857;
  background: var(--green-soft);
  border: 1px solid #a7f3d0;
}
.prep-page .noise-result.noisy {
  color: #b45309;
  background: var(--amber-soft);
  border-color: #fde68a;
}

/* ---------- 表单（JD / 岗位 / 简历） ---------- */
.prep-page .jd-textarea,
.prep-page .form-input,
.prep-page .form-select {
  width: 100%;
  padding: 12px 15px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fafbfe;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text);
  transition: all 0.2s ease;
}
.prep-page .jd-textarea {
  min-height: 120px;
  line-height: 1.85;
  resize: vertical;
}
.prep-page .jd-textarea:hover,
.prep-page .form-input:hover,
.prep-page .form-select:hover {
  background: #f7f8fd;
}
.prep-page .jd-textarea:focus,
.prep-page .form-input:focus,
.prep-page .form-select:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}
.prep-page .jd-counter {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 9px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--muted);
  font-variant-numeric: tabular-nums;
}
.prep-page .jd-inline-btn {
  padding: 3px 8px;
  border-radius: 6px;
  border: none;
  background: transparent;
  font-size: 12px;
  color: var(--sub);
  cursor: pointer;
  transition: all 0.18s ease;
}
.prep-page .jd-inline-btn:hover {
  color: var(--red);
  background: var(--red-soft);
}
.prep-page .jd-source-badge {
  margin-left: 8px;
  padding: 3px 10px;
  border-radius: 99px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--primary);
  background: var(--primary-soft);
  border: 1px solid #c7d2fe;
}

/* ---------- 高级设置折叠 ---------- */
.prep-page .advanced-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 17px 26px;
  margin-bottom: 16px;
  border-radius: 16px;
  border: 1px solid var(--border);
  background: #fff;
  box-shadow: var(--rpt-shadow-sm);
  transition: all 0.25s ease;
  overflow: hidden;
}
.prep-page .advanced-card:hover {
  border-color: #c7d2fe;
  background: #fafbff;
  transform: translateY(-1px);
  box-shadow: var(--rpt-shadow-md);
}
.prep-page .advanced-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text);
}
.prep-page .advanced-toggle-state {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  font-weight: 600;
  color: var(--sub);
}

/* ---------- 断点续接横幅 ---------- */
.prep-page .resume-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 14px 20px;
  margin-bottom: 16px;
  border-radius: 14px;
  border: 1px solid #c7d2fe;
  background: linear-gradient(135deg, #f5f7ff, #eef2ff);
  box-shadow: var(--rpt-shadow-sm);
}
.prep-page .resume-banner-title {
  font-size: 13.5px;
  font-weight: 800;
  color: var(--primary);
}
.prep-page .resume-banner-meta {
  font-size: 12px;
  color: var(--sub);
}
.prep-page .resume-btn {
  padding: 9px 18px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s ease;
}
.prep-page .resume-btn.primary {
  color: #fff;
  border: none;
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  box-shadow: 0 6px 14px -8px rgba(79, 70, 229, 0.7);
}
.prep-page .resume-btn.primary:hover {
  filter: brightness(1.06);
  transform: translateY(-1px);
}
.prep-page .resume-btn.ghost {
  color: #374151;
  background: #fff;
  border: 1px solid var(--border);
}
.prep-page .resume-btn.ghost:hover {
  border-color: #d5d9e2;
  background: #f7f8fb;
}

/* ---------- 主按钮（红色渐变 + 流光） ---------- */
.prep-page .main-cta,
.prep-page .prep-submit-btn {
  position: relative;
  overflow: hidden;
  width: 100%;
  padding: 18px 24px;
  border: none;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 11px;
  font-size: 15.5px;
  font-weight: 800;
  letter-spacing: 0.4px;
  color: #fff;
  background: linear-gradient(135deg, var(--red) 0%, var(--red-2) 100%);
  box-shadow: 0 14px 32px -14px rgba(225, 29, 72, 0.75);
  transition: all 0.25s ease;
}
.prep-page .main-cta::before,
.prep-page .prep-submit-btn::before {
  content: "";
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(120deg, transparent 30%, rgba(255, 255, 255, 0.25) 50%, transparent 70%);
  transition: transform 0.6s ease;
}
.prep-page .main-cta:hover::before,
.prep-page .prep-submit-btn:hover::before {
  transform: translateX(100%);
}
.prep-page .main-cta:hover,
.prep-page .prep-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 40px -16px rgba(225, 29, 72, 0.95);
}
.prep-page .main-cta:disabled,
.prep-page .prep-submit-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

/* ---------- 响应式 ---------- */
@media (max-width: 640px) {
  .prep-page .prep-steps {
    padding: 18px 14px;
  }
  .prep-page .prep-step {
    gap: 7px;
    font-size: 11.5px;
  }
  .prep-page .step-circle {
    width: 26px;
    height: 26px;
    font-size: 11.5px;
  }
  .prep-page .step-connector {
    min-width: 20px;
    margin: 0 8px;
  }
  .prep-page .prep-card {
    padding: 18px 16px;
  }
  .prep-page .device-select {
    min-width: 0;
    width: 100%;
  }
  .prep-page .prep-toolbar-title {
    font-size: 16px;
  }
  .prep-page .main-cta,
  .prep-page .prep-submit-btn {
    font-size: 14px;
    padding: 16px 20px;
  }
}

/* ====================================================================
   过程页视觉重构（严格对齐静态页 面试过程页v2.html）
   —— 紫色口径 + 毛玻璃状态栏（面包屑 / 计时 / 播报 / 暂停 / 结束）+ 三栏布局。
   ==================================================================== */
.interview-page {
  --primary: #4f46e5;
  --primary-2: #7c3aed;
  --primary-soft: #eef2ff;
  --green: #059669;
  --green-soft: #ecfdf5;
  --amber: #d97706;
  --amber-soft: #fffbeb;
  --red: #e11d48;
  --red-soft: #fff1f3;
  --text: #1f2937;
  --sub: #6b7280;
  --border: #e8eaef;
  --rpt-shadow-sm: 0 1px 3px rgba(16, 24, 40, 0.04);
  --rpt-shadow-md: 0 4px 16px -8px rgba(16, 24, 40, 0.12);
}

/* ---------- 顶部悬浮栏（毛玻璃 + 底部细线） ---------- */
.interview-page .top-bar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 12px 18px;
  margin: 0 0 14px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 4px 20px -14px rgba(16, 24, 40, 0.3);
}
.interview-page .top-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}
.interview-page .top-bar-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

/* ---------- 计时胶囊（绿点脉冲 + 等宽数字） ---------- */
.interview-page .timer-display {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 14px;
  border-radius: 99px;
  border: 1px solid var(--border);
  background: #fff;
  box-shadow: 0 2px 8px -6px rgba(16, 24, 40, 0.2);
  font-size: 13.5px;
  font-weight: 800;
  letter-spacing: 0.3px;
  color: #374151;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.interview-page .timer-display > span:first-child {
  font-size: 12px;
}
.interview-page .timer-display::before {
  content: "";
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--green);
  animation: proc-pulse 1.6s infinite;
  flex-shrink: 0;
}
.interview-page .timer-display.urgent {
  color: var(--red);
  border-color: #fecdd3;
  background: var(--red-soft);
}
.interview-page .timer-display.urgent::before {
  background: var(--red);
  animation-duration: 0.9s;
}
@keyframes proc-pulse {
  0% {
    opacity: 1;
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(5, 150, 105, 0.6);
  }
  50% {
    opacity: 0.6;
    transform: scale(0.85);
    box-shadow: 0 0 0 6px rgba(5, 150, 105, 0);
  }
  100% {
    opacity: 1;
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(5, 150, 105, 0);
  }
}

/* ---------- 顶栏操作按钮 ---------- */
.interview-page .control-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 13px;
  border-radius: 9px;
  border: 1px solid var(--border);
  background: #fff;
  color: #4b5563;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  transition: all 0.2s ease;
}
.interview-page .control-btn:hover:not(:disabled) {
  background: #f7f8fb;
  border-color: #d5d9e2;
  color: #1f2937;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px -8px rgba(16, 24, 40, 0.3);
}
.interview-page .control-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.interview-page .control-btn.muted {
  background: var(--primary-soft);
  border-color: #c7d2fe;
  color: var(--primary);
}
.interview-page .control-btn.danger {
  color: var(--red);
  border-color: #fecdd3;
  font-weight: 700;
}
.interview-page .control-btn.danger:hover:not(:disabled) {
  background: var(--red-soft);
  border-color: #fda4af;
}

/* ---------- 三栏布局 ---------- */
.interview-page .interview-main {
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr) 288px;
  gap: 14px;
  align-items: start;
  padding: 14px 0 0;
}
.interview-page .left-panel,
.interview-page .center-panel,
.interview-page .right-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

/* ---------- 卡片统一（面试官 / 考察点 / 对话 / 背景 / 提示） ---------- */
.interview-page .interviewer-card,
.interview-page .progress-card,
.interview-page .analysis-card,
.interview-page .chat-container,
.interview-page .chat-panel {
  border-radius: 16px;
  border: 1px solid var(--border);
  background: #fff;
  box-shadow: var(--rpt-shadow-sm);
}
.interview-page .interviewer-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 18px;
  text-align: center;
}
.interview-page .interviewer-avatar {
  position: relative;
  width: 104px;
  height: 104px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  box-shadow: 0 10px 26px -14px rgba(37, 99, 235, 0.7);
}
.interview-page .interviewer-avatar::after {
  content: "";
  position: absolute;
  inset: -5px;
  border-radius: 50%;
  border: 2px solid rgba(79, 70, 229, 0.22);
}
.interview-page .interviewer-avatar.speaking::after {
  animation: proc-ripple 1.6s infinite;
}
@keyframes proc-ripple {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.14);
    opacity: 0;
  }
}
.interview-page .interviewer-name {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  color: var(--text);
}
.interview-page .interviewer-meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  font-size: 11.5px;
  color: var(--sub);
}

/* 考察知识点 */
.interview-page .progress-card {
  padding: 16px 18px;
}
.interview-page .progress-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13.5px;
  font-weight: 800;
  color: var(--text);
}
.interview-page .progress-timeline {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.interview-page .progress-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 11px;
  border-radius: 9px;
  font-size: 12.5px;
  color: #4b5563;
  background: #f8f9fc;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  cursor: pointer;
}
.interview-page .progress-item:hover {
  background: var(--primary-soft);
  border-color: #c7d2fe;
  color: var(--primary);
}
.interview-page .progress-item.active {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--red), var(--primary-2));
  box-shadow: 0 6px 14px -8px rgba(225, 29, 72, 0.6);
}

/* ---------- 对话区 ---------- */
.interview-page .chat-container,
.interview-page .chat-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.interview-page .chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 13px 18px;
  border-bottom: 1px solid var(--border);
  font-size: 13.5px;
  font-weight: 800;
  color: var(--text);
}
.interview-page .chat-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--green);
}
.interview-page .chat-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--green);
  animation: proc-pulse 1.6s infinite;
}
.interview-page .chat-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px;
  overflow-y: auto;
}

/* 气泡：AI 灰底 / 我的回答浅红底 / 评分反馈灰底 */
.interview-page .message {
  display: flex;
  gap: 10px;
  max-width: 92%;
}
.interview-page .message.user {
  flex-direction: row-reverse;
  margin-left: auto;
}
.interview-page .message-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(135deg, #f43f5e, #fb923c);
}
.interview-page .message.user .message-avatar {
  background: linear-gradient(135deg, #94a3b8, #cbd5e1);
}
.interview-page .message-content {
  padding: 11px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.8;
  color: #374151;
  background: #f3f4f8;
  border: 1px solid var(--border);
}
.interview-page .message.user .message-content {
  background: var(--red-soft);
  border-color: #fecdd3;
}
.interview-page .message.question .message-content {
  background: var(--amber-soft);
  border-color: #fde68a;
}

/* 输入区 */
.interview-page .chat-input-area {
  padding: 14px 18px;
  border-top: 1px solid var(--border);
  background: #fbfcfe;
}
.interview-page .input-timer {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 9px;
  font-size: 11.5px;
  color: var(--sub);
}
.interview-page .input-textarea {
  width: 100%;
  min-height: 72px;
  padding: 12px 15px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  font-size: 13px;
  line-height: 1.75;
  color: var(--text);
  resize: vertical;
  transition: all 0.2s ease;
}
.interview-page .input-textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}
.interview-page .input-actions {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.interview-page .action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 9px 15px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #fff;
  color: #4b5563;
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  transition: all 0.2s ease;
}
.interview-page .action-btn:hover:not(:disabled) {
  background: #f7f8fb;
  border-color: #d5d9e2;
  color: #1f2937;
}
.interview-page .action-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.interview-page .action-btn.primary {
  margin-left: auto;
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, var(--primary), var(--primary-2));
  box-shadow: 0 6px 14px -8px rgba(79, 70, 229, 0.7);
}
.interview-page .action-btn.primary:hover:not(:disabled) {
  filter: brightness(1.06);
  transform: translateY(-1px);
}

/* ---------- 右栏（面试背景 + 面试提示） ---------- */
.interview-page .analysis-card {
  padding: 16px 18px;
}
.interview-page .analysis-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 13.5px;
  font-weight: 800;
  color: var(--text);
}
.interview-page .dimension-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.interview-page .dimension-legend-row {
  display: flex;
  gap: 10px;
  font-size: 12.5px;
  line-height: 1.7;
}
.interview-page .legend-label {
  flex-shrink: 0;
  width: 64px;
  color: var(--sub);
}

/* ---------- 响应式 ---------- */
@media (max-width: 1024px) {
  .interview-page .interview-main {
    grid-template-columns: 1fr;
  }
  .interview-page .left-panel {
    order: -1;
  }
}
@media (max-width: 960px) {
  .interview-page .top-bar {
    padding: 10px 14px;
    gap: 10px;
  }
  .interview-page .timer-display {
    padding: 5px 11px;
    font-size: 12.5px;
  }
}
@media (max-width: 768px) {
  .interview-page .top-bar-left,
  .interview-page .top-bar-right {
    width: 100%;
  }
  .interview-page .top-bar-right {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 6px;
  }
  .interview-page .control-btn {
    justify-content: center;
    padding: 8px 4px;
    font-size: 11.5px;
  }
  .interview-page .action-btn {
    flex: 1;
    justify-content: center;
  }
  .interview-page .action-btn.primary {
    margin-left: 0;
    flex-basis: 100%;
  }
  .interview-page .message {
    max-width: 96%;
  }
}
</style>
