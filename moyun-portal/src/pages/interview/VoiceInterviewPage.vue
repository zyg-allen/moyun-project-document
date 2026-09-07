<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { usePromptModal } from '@/composables/usePromptModal';
import { useConfirmModal } from '@/composables/useConfirmModal';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import MarkdownRenderer from '@/components/MarkdownRenderer.vue';
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
  forceVoiceNext,
  finishVoiceInterview,
  getVoiceInterviewDetail,
  addQaToWrongBook,
  getVoiceAgents,
  getVoiceJobTemplates,
  createReportShareToken,
} from '@/api/voiceInterview';
import { getMyResumeList, parseResumeAttachment } from '@/api/interview';
import { pollAiTask } from '@/api/aiTask';
import type { UserResumeVO } from '@/types/api';
import { useUserStore } from '@/stores/user';
import type {
  VoiceInterviewVO,
  VoiceInterviewReportVO,
  VoiceStartConfig,
  QuestionReview,
  PointItem,
  KnowledgePointItem,
  VoiceAgentItem,
  VoiceJobTemplateItem,
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

// 卡壳自动提示（stuckThreshold 秒无作答自动给一级提示，每题一次）
const autoHintFired = ref(false);
const questionShownAt = ref(0);

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
  /** V11.0：是否正在流式输出（打字机） */
  streaming?: boolean;
  /** V11.0：LLM 深度分析摘要（心态/流畅度/红旗） */
  sentimentState?: string;
  fluencyScore?: number;
  redFlags?: string[];
  createdAt: number;
}
const chatList = ref<ChatMessage[]>([]);
const chatScroll = ref<HTMLElement | null>(null);

/** V11.0：当前流式输出的气泡 id（onDelta 追加 / onData 收尾置空） */
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
const POSITION_OPTIONS = [
  { title: 'Java 后端开发', meta: '后端服务 · 高并发 · 中间件', position: 'Java 后端开发' },
  { title: '前端开发', meta: 'Vue/React · 工程化 · 性能优化', position: '前端开发' },
  { title: '算法工程师', meta: '机器学习 · 深度学习 · 推荐/NLP', position: '算法工程师' },
  { title: '测试开发', meta: '自动化测试 · 质量保障 · 工具建设', position: '测试开发' },
  { title: '运维开发', meta: 'Linux · K8s · CI/CD · 稳定性', position: '运维开发' },
];
const STYLE_OPTIONS = [
  { label: '温和型 - 鼓励式提问', value: 'friendly' as const },
  { label: '标准型 - 专业严谨', value: 'professional' as const },
  { label: '压力型 - 挑战式追问', value: 'strict' as const },
];
const DIFFICULTY_OPTIONS = [
  { label: '初级（应届/转行）', value: 'easy' as const },
  { label: '中级（1-3 年）', value: 'medium' as const },
  { label: '高级（3 年以上）', value: 'hard' as const },
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
  position: 'Java 后端开发',
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

// ==================== 简历库（真实数据：AI 面试题源依赖） ====================
const resumeList = ref<UserResumeVO[]>([]);
const resumeLoading = ref(false);
const selectedResumeId = ref<number | null>(null);
/** 自定义岗位（当预设岗位都不匹配时） */
const useCustomPosition = ref(false);
const customPosition = ref('');

// ==================== V11.0 面试官智能体选择 ====================
const agentList = ref<VoiceAgentItem[]>([]);
const selectedAgentId = ref<number | null>(null);
/** 动态出题模式（未开启时走预生成题单；agent 不可用时自动隐藏开关） */
const dynamicMode = ref(true);

async function loadAgentList() {
  try {
    const res = await getVoiceAgents();
    if (res.code === 200 && Array.isArray(res.data) && res.data.length > 0) {
      agentList.value = res.data;
      // 默认选中第一个 agent
      selectedAgentId.value = res.data[0].id;
    }
  } catch {
    /* AI 未配置/接口异常时静默：走旧 preset 链路 */
  }
}

// ==================== v11.x 岗位模板选择（job 题源智能出题） ====================
const jobTemplateList = ref<VoiceJobTemplateItem[]>([]);
const selectedJobTemplateId = ref<number | null>(null);

async function loadJobTemplateList() {
  try {
    const res = await getVoiceJobTemplates();
    if (res.code === 200 && Array.isArray(res.data)) {
      jobTemplateList.value = res.data;
    }
  } catch {
    /* 后台未配置岗位模板时静默：隐藏选择器 */
  }
}

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
    // 简历求职意向优先作为面试岗位（个性化出题），超长截断对齐数据库 varchar(64)
    const safePos = intentPos.slice(0, POSITION_MAX_LEN);
    const matched = POSITION_OPTIONS.find((o) => o.position === safePos);
    if (!matched) {
      useCustomPosition.value = true;
      customPosition.value = safePos;
    } else {
      useCustomPosition.value = false;
      config.value.position = safePos;
    }
  }
}

// ==================== 上传简历：AI 解析 → 回填默认简历 ====================
const resumeUploading = ref(false);
const resumeUploadInput = ref<HTMLInputElement | null>(null);
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
  resumeParsingMsg.value = '正在上传附件…';
  try {
    const { data: resp, success } = await run(() => parseResumeAttachment(file), {
      errorToast: '上传失败',
    });
    if (!success || !resp?.data) return;
    const { resumeId, taskId } = resp.data;
    resumeParsingMsg.value = 'AI 正在解析简历…';
    try {
      await pollAiTask(taskId, {
        intervalMs: 3000,
        onTick: (task) => {
          resumeParsingMsg.value = task.progressMsg || 'AI 正在解析简历…';
        },
      });
      // 解析完成：刷新简历库并自动回填选中该简历
      await loadResumeList();
      const target = resumeList.value.find((r) => Number(r.id) === Number(resumeId));
      if (target) {
        selectResume(target);
        toast.success('简历解析完成，已设为本次面试简历');
      } else {
        selectedResumeId.value = Number(resumeId);
        toast.success('简历解析完成');
      }
    } catch (parseErr: any) {
      toast.error(parseErr?.message || 'AI 解析失败，可稍后在简历库手动完善');
    }
  } finally {
    resumeUploading.value = false;
    resumeParsingMsg.value = '';
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
  loadAgentList();
  loadJobTemplateList();
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
    // 卡壳自动提示：超过阈值仍未开始作答（无文字、无语音），自动给一级提示
    const stuckSec = config.value.stuckThreshold ?? 30;
    if (
      hintsEnabled.value &&
      !autoHintFired.value &&
      !submitting.value &&
      !editableAnswer.value &&
      !interimText.value &&
      questionShownAt.value > 0 &&
      Date.now() - questionShownAt.value > stuckSec * 1000
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
  // 页签切走/最小化：立即停止聆听与播报，及时释放麦克风等硬件占用
  document.addEventListener('visibilitychange', releaseOnHidden);
  // 浏览器关闭/刷新：主动断开 ASR 流式连接（SPA 卸载钩子不一定来得及执行）
  window.addEventListener('beforeunload', abortAsr);
});

onUnmounted(() => {
  document.removeEventListener('visibilitychange', releaseOnHidden);
  window.removeEventListener('beforeunload', abortAsr);
  stopElapsedTimer();
  stopCountdown();
  if (questionTtsTimer) clearTimeout(questionTtsTimer);
  ttsCancel();
  // 卸载即丢弃：立即停麦克风/断 WS，不等转写收尾（用户已离开页面，无需保留结果）
  abortAsr();
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
  loading.value = true;
  try {
    const payload: VoiceStartConfig = {
      ...config.value,
      position: pos,
      resumeId: selectedResumeId.value ?? undefined,
      personalized: true,
      hintsEnabled: hintsEnabled.value,
      stuckThreshold: 30,
      // V11.0：面试官智能体 + 动态出题（agent 不可用时留空走旧链路）
      agentId: selectedAgentId.value ?? undefined,
      dynamicMode: selectedAgentId.value != null ? dynamicMode.value : undefined,
      // v11.x：岗位模板（job 题源 + 出题权重默认值）
      jobTemplateId: selectedJobTemplateId.value ?? undefined,
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

// ==================== 提交答案（SSE） ====================
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
      // V11.0：流式增量（打字机气泡；delta 到 speak/data 前实时渲染面试官回复）
      onDelta: (text) => {
        if (text) appendDelta(text);
      },
      onSpeak: (text) => {
        if (text) {
          // 流式气泡已渲染完同样内容时不重复推（旧链路无 delta 直推）
          if (streamingMsgId) {
            finishStreaming('评分反馈');
          } else {
            pushChat('ai', text, { tag: '评分反馈' });
          }
          if (!muteMode.value && ttsSupported.value) ttsSpeak(text);
        }
      },
      onData: (data) => {
        submitting.value = false;
        // V11.0：流式兜底收尾（speak 事件缺失时）
        finishStreaming();
        // V11.0：LLM 深度分析摘要注入最近一条 analysis 气泡
        if (data.analysis) {
          const lastAnalysis = [...chatList.value].reverse().find((m) => m.role === 'analysis');
          if (lastAnalysis) {
            lastAnalysis.sentimentState = data.analysis.sentiment?.state;
            lastAnalysis.fluencyScore = data.analysis.fluencyAssessment?.score;
            lastAnalysis.redFlags = data.analysis.redFlags?.length ? data.analysis.redFlags : undefined;
            // v11.30.2：LLM 融合 6 维到达后刷新亮点/缺口（覆盖规则版初值，实时分析不再固定）
            if (data.analysis.dimensions && Object.keys(data.analysis.dimensions).length > 0) {
              const dims = normalizeDimensions(data.analysis.dimensions);
              const highlights: string[] = [];
              const gaps: string[] = [];
              DIMENSION_META.forEach((m) => {
                const s = dims[m.key];
                if (typeof s === 'number') {
                  if (s >= 80) highlights.push(`${m.label} ${s}`);
                  else if (s < 60) gaps.push(`${m.label} ${s}`);
                }
              });
              if (highlights.length || gaps.length) {
                lastAnalysis.highlights = highlights;
                lastAnalysis.gaps = gaps;
                lastAnalysis.scoreText = DIMENSION_META.map((m) => `${m.label} ${dims[m.key] ?? '-'}`).join(' · ');
              }
            }
          }
        }
        // V10.4：LLM 引导提示（回答跑偏时面试官给出的方向引导）
        if (data.guidance) {
          pushChat('ai', data.guidance, { tag: '引导' });
          if (!muteMode.value && ttsSupported.value) ttsSpeak(data.guidance);
        }
        // V11.0：agentAction 优先（deepen/change_topic/wrap_up），旧 nextAction 兼容保留
        const action = data.agentAction
          ? (data.agentAction === 'deepen' ? 'followup'
            : data.agentAction === 'change_topic' ? 'next' : 'report')
          : data.nextAction;
        if (action === 'next' && data.nextQaId) {
          currentQaId.value = data.nextQaId;
          if (data.nextQuestion) {
            presentQuestion(data.nextQuestion, data.nextSpeakText || data.nextQuestion);
          }
        } else if (action === 'followup' && data.nextQaId) {
          currentQaId.value = data.nextQaId;
          if (data.nextQuestion) {
            presentQuestion(data.nextQuestion, data.nextSpeakText || data.nextQuestion, undefined, '追问');
          }
          toast.info('面试官追问，请补充回答');
        } else if (action === 'report') {
          toast.info('面试结束，正在生成报告...');
          handleFinish();
        } else {
          editableAnswer.value = '';
          resetAsrWithTracker();
        }
      },
      onEnd: () => {
        submitting.value = false;
        finishStreaming();
      },
      // V11.0.2：流被服务端异常切断（后端 SSE 120s 超时收尾/网络中断），提示用户可重答或下一题
      onAborted: () => {
        toast.error('AI 响应超时中断，请重试或点击下一题继续');
        submitting.value = false;
        finishStreaming();
      },
      onError: (msg) => {
        toast.error(msg || '提交失败');
        submitting.value = false;
        finishStreaming();
      },
    },
  );
}

// ==================== 智能提示（V10.1 统一走 POST /{id}/hint） ====================
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
  const used = vo.data.currentQa?.hintUsed ?? hint?.level ?? 1;
  // 后端返回分级提示（title/keywords/structureHint/examinePoints）
  if (hint) {
    const parts = [hint.title];
    if (hint.keywords?.length) parts.push(`关键词：${hint.keywords.join('、')}`);
    if (hint.structureHint) parts.push(hint.structureHint);
    if (hint.examinePoints?.length) parts.push(`考察点：${hint.examinePoints.join('、')}`);
    const text = parts.join('\n');
    pushChat('ai', text, { tag: `L${used} 提示` });
    hintPreview.value = text;
    if (!muteMode.value && ttsSupported.value) {
      ttsSpeak(hint.speakText || text);
    }
  } else if (vo.data.currentQa?.speakText) {
    // 兜底：无结构化提示时播报题目引导话术
    const speak = vo.data.currentQa.speakText;
    pushChat('ai', `💡 ${speak}`, { tag: `L${used} 提示` });
    hintPreview.value = speak;
    if (!muteMode.value && ttsSupported.value) ttsSpeak(speak);
  }
  toast.success(`已获取 L${used} 提示（共 3 级）`);
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
      // v11.30.3：finish 后重拉详情填充 qaList（对话回放 Tab 数据源；start 返回的 VO 不含完整问答）
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
  } finally {
    loading.value = false;
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
  liveDimensions.value = {};
  hintPreview.value = '';
  resetAsrWithTracker();
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
          </div>
          <div class="device-tip">💡 建议佩戴耳机避免回声。麦克风权限可在浏览器地址栏图标中管理；插拔设备后列表会自动刷新。</div>
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
              <!-- 自定义岗位：跟随简历求职意向或手动输入（对齐数据库 varchar(64)） -->
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
                    :maxlength="POSITION_MAX_LEN"
                    placeholder="输入目标岗位，如：Go 后端开发"
                    @click.stop
                    @input="useCustomPosition = true"
                  />
                  <div class="custom-position-counter">{{ customPosition.length }}/{{ POSITION_MAX_LEN }}</div>
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

          <!-- V11.0：面试官智能体（AI 模块配置的 agent 动态下发；无可用 agent 时整块隐藏） -->
          <div v-if="agentList.length > 0" class="config-row agent-row">
            <div class="config-item">
              <label class="config-label">面试官智能体</label>
              <select v-model.number="selectedAgentId" class="config-select">
                <option v-for="a in agentList" :key="a.id" :value="a.id">{{ a.name }}</option>
              </select>
            </div>
            <div class="config-item">
              <label class="config-label">出题方式</label>
              <select v-model="dynamicMode" class="config-select">
                <option :value="true">智能动态出题（结合简历与上下文）</option>
                <option :value="false">固定题单（预设题库抽题）</option>
              </select>
            </div>
          </div>
          <div v-if="agentList.length > 0 && dynamicMode" class="agent-hint">
            💡 动态出题模式：AI 面试官将结合你的简历、画像与实时对话上下文随机应变——
            回答出彩会深挖追问，答非所问会引导纠偏，全程像一个真实会话。
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

          <!-- v11.x：岗位模板（后台配置的 JD 关键词 + 出题权重驱动 job 题源；未配置时隐藏） -->
          <div v-if="jobTemplateList.length > 0" class="config-row">
            <div class="config-item">
              <label class="config-label">岗位模板</label>
              <select v-model.number="selectedJobTemplateId" class="config-select">
                <option :value="null">不使用（按简历/画像出题）</option>
                <option v-for="jt in jobTemplateList" :key="jt.id" :value="jt.id">
                  {{ jt.name }}{{ jt.difficulty ? `（${jt.difficulty}）` : '' }}
                </option>
              </select>
            </div>
          </div>
          <div v-if="jobTemplateList.length > 0 && selectedJobTemplateId != null" class="agent-hint">
            💡 已选择岗位模板：面试题将优先覆盖该岗位 JD 核心考点，并结合你的简历与薄弱点智能配比。
          </div>

          <div class="resume-section" style="margin-top: 1.5rem;">
            <div class="section-label">📄 选择简历（AI 将基于简历项目经历深挖提问）</div>

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
                <button class="resume-action-btn" :disabled="resumeUploading" @click="triggerResumeUpload">
                  {{ resumeUploading ? (resumeParsingMsg || '解析中…') : '⬆ 上传新简历（AI 解析）' }}
                </button>
                <button class="resume-action-btn" @click="router.push('/interview/my/resumes')">管理简历库</button>
                <span class="resume-manage-hint">不选简历也可面试，AI 将按岗位通用题库出题</span>
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
            <div class="interviewer-name">{{ interview?.agentName || 'AI 面试官' }}</div>
            <div class="interviewer-meta">
              <span class="interviewer-style">{{ STYLE_LABEL[config.style ?? 'professional'] || '标准型' }}</span>
              <span v-if="interview?.questionMode === 'dynamic'" class="interviewer-style">动态出题</span>
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
                <div v-else-if="m.role === 'analysis'" class="analysis-header">
                  ✅ 实时分析<span v-if="m.tag"> · {{ m.tag }}</span>
                </div>
                <div v-else-if="m.tag" class="ai-tag">{{ m.tag }}</div>

                <template v-if="m.role === 'ai'">
                  <MarkdownRenderer editor-mode="markdown" :content-markdown="m.content" prose-width="none" />
                  <span v-if="m.streaming" class="streaming-cursor"></span>
                </template>
                <template v-else-if="m.role === 'analysis'">
                  <div v-if="m.highlights && m.highlights.length"><strong>亮点：</strong>{{ m.highlights.join('；') }}</div>
                  <div v-if="m.gaps && m.gaps.length"><strong>缺口：</strong>{{ m.gaps.join('；') }}</div>
                  <div v-if="m.scoreText" class="analysis-score">{{ m.scoreText }}</div>
                  <!-- V11.0：LLM 深度分析摘要（心态/流畅度/可疑信号） -->
                  <div v-if="m.sentimentState || m.fluencyScore || m.redFlags?.length" class="analysis-insight">
                    <span v-if="m.sentimentState" class="insight-chip sentiment">心态：{{ SENTIMENT_LABEL[m.sentimentState] || m.sentimentState }}</span>
                    <span v-if="m.fluencyScore != null" class="insight-chip fluency">流畅度 {{ m.fluencyScore }}</span>
                    <span v-if="m.redFlags?.length" class="insight-chip redflag" :title="m.redFlags.join('；')">⚠ {{ m.redFlags.length }} 个可疑信号</span>
                  </div>
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
              <!-- V11.0：AI 深度复盘（Agent 模式产出；旧数据无此字段时隐藏） -->
              <div v-if="report?.sentimentTrend?.length || report?.redFlags?.length || report?.fluencyAvg != null" class="deep-review">
                <div class="deep-review-title">🧠 AI 深度复盘</div>
                <div v-if="report?.sentimentTrend?.length" class="deep-review-row">
                  <span class="deep-review-label">心态趋势</span>
                  <span class="sentiment-track">
                    <span
                      v-for="(s, i) in report.sentimentTrend"
                      :key="i"
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
                    <span v-for="(f, i) in report.redFlags" :key="i" class="redflag-item">⚠ {{ f }}</span>
                  </span>
                </div>
              </div>
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
            <!-- v11.x：自我介绍独立评分（4维度加权；旧会话无此数据时隐藏） -->
            <div v-if="introScoreView" class="intro-score-card">
              <div class="intro-score-header">
                <h3 class="summary-title">🎤 自我介绍评分</h3>
                <span class="intro-score-total" :style="{ color: scoreColor(introScoreView.total) }">
                  {{ introScoreView.total }} 分
                </span>
              </div>
              <div class="intro-score-dims">
                <div v-for="d in introScoreView.dims" :key="d.key" class="intro-score-dim">
                  <span class="intro-dim-label">{{ d.label }}</span>
                  <div class="intro-dim-bar">
                    <div
                      :class="['score-fill', scoreClass(d.value)]"
                      :style="{ width: d.value + '%' }"
                    ></div>
                  </div>
                  <span class="intro-dim-value">{{ d.value }}</span>
                </div>
              </div>
              <div v-if="introScoreView.comment" class="intro-score-comment">{{ introScoreView.comment }}</div>
            </div>
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
              本场面试暂无题库关联知识点（知识点来自题库题目标签聚合，纯 AI 动态出题的场次不生成）
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
.device-select { max-width: 180px; padding: 0.375rem 1.75rem 0.375rem 0.625rem; border: 1px solid var(--gray-200); border-radius: var(--radius-sm); font-size: 0.75rem; color: var(--gray-600); background: white; outline: none; cursor: pointer; appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='10' viewBox='0 0 24 24' fill='none' stroke='%239ca3af' stroke-width='2'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 8px center; transition: border-color 0.2s; }
.device-select:focus { border-color: var(--primary); }
.mic-level-row { display: flex; align-items: center; gap: 0.625rem; margin-top: 0.5rem; padding-left: 2.5rem; }
.mic-level-bar { flex: 1; max-width: 280px; height: 6px; background: var(--gray-100); border-radius: var(--radius-full); overflow: hidden; }
.mic-level-fill { height: 100%; background: var(--success); border-radius: var(--radius-full); transition: width 0.1s linear; }
.mic-level-text { font-size: 0.75rem; color: var(--gray-500); }

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
.custom-position-counter { margin-top: 0.25rem; font-size: 0.6875rem; color: var(--gray-400); text-align: right; }
.resume-parsing-msg { margin: 0.5rem 0; font-size: 0.8125rem; color: var(--primary); }
.hidden-file-input { display: none; }
.asr-live { border-color: var(--primary) !important; box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.08); }

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
.interviewer-meta { display: flex; flex-direction: column; align-items: center; gap: 0.375rem; }
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

/* --- V11.0 流式打字机光标 --- */
.streaming-cursor { display: inline-block; width: 2px; height: 1em; margin-left: 2px; vertical-align: text-bottom; background: var(--primary); animation: cursor-blink 0.8s steps(1) infinite; }
@keyframes cursor-blink { 50% { opacity: 0; } }

/* --- V11.0 分析气泡深度摘要 chips --- */
.analysis-insight { display: flex; flex-wrap: wrap; gap: 0.375rem; margin-top: 0.625rem; }
.insight-chip { font-size: 0.6875rem; padding: 0.125rem 0.5rem; border-radius: var(--radius-full); font-weight: 600; }
.insight-chip.sentiment { background: #ede9fe; color: #7c3aed; }
.insight-chip.fluency { background: #dbeafe; color: #2563eb; }
.insight-chip.redflag { background: #fee2e2; color: #dc2626; cursor: help; }

/* --- V11.0 agent 选择提示 --- */
.agent-hint { margin-top: 0.625rem; font-size: 0.75rem; line-height: 1.5; color: var(--gray-400); background: var(--primary-bg); border-radius: var(--radius-md); padding: 0.5rem 0.75rem; }

/* --- V11.0 报告 AI 深度复盘 --- */
.deep-review { margin-top: 1.25rem; background: white; border: 1px solid var(--gray-100); border-radius: var(--radius-lg); padding: 1rem 1.25rem; box-shadow: var(--shadow-sm); }
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
/* 平板（≤1024px）：三栏收敛为单列，对话区优先——面试官信息做顶部紧凑条，维度分析沉底 */
@media (max-width: 1024px) {
  .interview-main { grid-template-columns: 1fr; }
  .left-panel { order: -1; }
  .center-panel { order: 0; }
  .right-panel { order: 1; }
  .summary-grid { grid-template-columns: 1fr; }
}
/* 手机（≤768px）：全面移动端适配 */
@media (max-width: 768px) {
  .vi-shell { padding: 0 0.625rem; }

  /* --- 准备页 --- */
  .prep-top-bar { padding: 0.625rem 0.75rem; flex-wrap: wrap; gap: 0.5rem; }
  .prep-breadcrumb { display: none; }
  .prep-top-right { width: 100%; }
  .prep-top-right .prep-back-btn { flex: 1; justify-content: center; font-size: 0.8125rem; padding: 0.5rem 0.5rem; }
  .prep-container { padding: 1.25rem 0 2rem; }
  .prep-header { margin-bottom: 1.25rem; }
  .prep-header h1 { font-size: 1.375rem; }
  .prep-card { padding: 1.125rem; }
  /* 步骤条保持横向，缩小间距适配窄屏 */
  .prep-steps { gap: 0; flex-wrap: nowrap; }
  .prep-step { gap: 0.375rem; }
  .step-circle { width: 30px; height: 30px; font-size: 0.8rem; }
  .step-label { font-size: 0.75rem; }
  .step-connector { width: 24px; margin: 0 0.25rem; }
  .config-row { grid-template-columns: 1fr; }
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
  .message.question, .message.analysis { max-width: 96%; }
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
  font-size: 0.6875rem;
  color: var(--info);
  font-weight: 600;
  animation: pulse-dot 1.5s ease-in-out infinite;
}
.interviewer-state.thinking { color: var(--warning); }
</style>
