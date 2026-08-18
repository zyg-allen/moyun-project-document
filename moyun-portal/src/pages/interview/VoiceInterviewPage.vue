<script setup lang="ts">
import { ref, computed, onUnmounted, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Loader2, Mic, MicOff, Volume2, Send, Lightbulb, ChevronRight,
  Square, RefreshCw, ArrowLeft, Award, TrendingUp, AlertCircle, CheckCircle2,
  Home, Clock, Timer, Trash2, Bot, User, Sparkles,
} from 'lucide-vue-next';
import { generateSeo } from '@/utils/seo';
import { useToast } from '@/composables/useToast';
import { useApiCall } from '@/composables/useApiCall';
import { useSpeechSynthesis } from '@/composables/useSpeechSynthesis';
import { useSpeechRecognition } from '@/composables/useSpeechRecognition';
import {
  startVoiceInterview, submitVoiceAnswer, requestVoiceHint,
  forceVoiceNext, finishVoiceInterview, getVoiceInterviewDetail,
} from '@/api/voiceInterview';
import type {
  VoiceInterviewVO, VoiceInterviewReportVO, VoiceStartConfig,
  QuestionReview,
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

// ==================== 历史报告视图：/?id=xxx 自动加载 ====================
const historyLoading = ref(false);
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

    // 根据详情组装简化版报告对象（供 UI 直接渲染 report 多 Tab）
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
      }));
    const avgScore = questionReviews.length > 0
      ? Math.round(questionReviews.reduce((s, r) => s + (r.score ?? 0), 0) / questionReviews.length)
      : (vo.score ?? 0);
    const totalScore = vo.score ?? avgScore;

    // 5 个维度（若详情里有维度 JSON 则优先解析，否则给一个占位分：以总分为基准 +2~-2 离散）
    const dimKeys = ['回答相关性', '专业深度', '表达清晰度', '结构完整性', '临场应变'];
    let dimensions: Record<string, number> | undefined;
    try {
      // 先尝试主表 configJson/qa 里 ruleDimensionsJson 聚合
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
          } catch { /* ignore */ }
        }
      });
      if (Object.keys(seen).length > 0) {
        dimensions = {};
        Object.entries(seen).forEach(([k, arr]) => {
          dimensions![k] = Math.round(arr.reduce((s, x) => s + x, 0) / arr.length);
        });
      } else {
        dimensions = dimKeys.reduce((acc, k, i) => {
          const offset = [+1, -2, +2, 0, -1];
          const s = Math.max(55, Math.min(99, totalScore + (offset[i] ?? 0)));
          acc[k] = s;
          return acc;
        }, {} as Record<string, number>);
      }
    } catch {
      dimensions = dimKeys.reduce((acc, k) => {
        acc[k] = totalScore;
        return acc;
      }, {} as Record<string, number>);
    }

    const highlights: VoiceInterviewReportVO['highlights'] = totalScore >= 80
      ? [
        { text: '整体回答切题，能围绕问题展开', quote: '回答聚焦题目，未出现明显跑题' },
        { text: '具备一定的专业表述能力' },
      ]
      : totalScore >= 60
        ? [{ text: '部分回答命中得分点，仍有提升空间' }]
        : [];
    const weakPoints: VoiceInterviewReportVO['weakPoints'] = totalScore < 85
      ? [
        { text: '结构完整性可加强（建议 STAR 结构答题）' },
        { text: '专业深度与案例支撑仍有欠缺' },
      ]
      : [];

    const assembledReport: VoiceInterviewReportVO = {
      interviewId: vo.id,
      totalScore,
      dimensions,
      highlights,
      weakPoints,
      questionReviews,
      summary: vo.summary ?? (
        questionReviews.length > 0
          ? `本次共回答 ${questionReviews.length} 道题，综合得分 ${totalScore}。` +
          `${totalScore >= 80 ? '整体表现良好，注意补齐薄弱知识点即可冲击 Offer。'
            : totalScore >= 60 ? '具备基础能力，建议通过复盘强化表达结构与专业深度。'
              : '差距较大，建议先完成题库基础练习 + 模拟面试后再挑战语音面试。'}`
          : '本次面试尚未形成完整总结。'
      ),
      suggestion: '建议对照逐题复盘，把失分题加入错题本并补齐相关知识点；下次面试优先采用 STAR 结构作答。',
      knowledgePoints: [],
    };
    report.value = assembledReport;
    phase.value = 'report';
    reportTab.value = 'summary';
  } finally {
    historyLoading.value = false;
  }
}

onMounted(() => {
  const id = String(route.query.id ?? '').trim();
  if (id) {
    // 历史报告视图：直接加载详情进入 report 阶段
    loadHistoryReport(id);
  }
});

// ==================== 三引擎初始化 ====================
const {
  supported: ttsSupported, speaking, paused,
  speak: ttsSpeak, pause: ttsPause, resume: ttsResume, cancel: ttsCancel,
} = useSpeechSynthesis({
  onUnsupported: () => toast.warning('当前浏览器不支持语音合成，将显示纯文字题目'),
});

const {
  supported: asrSupported, listening, interimText, finalText,
  start: startAsr, stop: stopAsr, reset: resetAsr,
} = useSpeechRecognition({
  onUnsupported: () => toast.warning('当前浏览器不支持语音识别，将切换为文字输入模式'),
  onError: (err) => {
    if (err === 'not-allowed') {
      toast.error('麦克风权限被拒绝，请切换文字模式或在浏览器设置中允许');
    }
  },
});

// ==================== 状态机 ====================
type Phase = 'setup' | 'asking' | 'listening' | 'analyzing' | 'report';
const phase = ref<Phase>('setup');
const loading = ref(false);
const submitting = ref(false);

// 面试会话
const interview = ref<VoiceInterviewVO | null>(null);
const currentQaId = ref<number | string | null>(null);
const currentQuestion = ref('');
const currentSpeakText = ref('');

// 报告
const report = ref<VoiceInterviewReportVO | null>(null);
const reportTab = ref<'dialog' | 'summary' | 'analysis' | 'review' | 'knowledge'>('summary');

// 答案编辑（ASR final 后可编辑）
const editableAnswer = ref('');
const answerStartTime = ref(0);

// 提示
const hintLoading = ref(false);

// 面试时长计时
const interviewStartAt = ref(0);
const elapsedSec = ref(0);
let timerHandle: any = null;

// ==================== 对话记录（供 UI 气泡渲染） ====================
interface ChatBubble {
  id: string;
  role: 'ai' | 'user' | 'system';
  content: string;
  tag?: string; // AI 徽章后的标签（如"追问"、"反馈"）
  createdAt: number;
}
const chatList = ref<ChatBubble[]>([]);

function pushChat(role: ChatBubble['role'], content: string, tag?: string) {
  chatList.value.push({
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    role,
    content,
    tag,
    createdAt: Date.now(),
  });
  // 滚到底部
  requestAnimationFrame(() => {
    const el = document.querySelector('.chat-scroll');
    if (el) el.scrollTop = el.scrollHeight;
  });
}

// ==================== 倒计时（每题 N 秒） ====================
const ANSWER_LIMIT_SEC = 90;
const answerRemain = ref(ANSWER_LIMIT_SEC);
let countdownHandle: any = null;
function startCountdown() {
  stopCountdown();
  answerRemain.value = ANSWER_LIMIT_SEC;
  countdownHandle = setInterval(() => {
    if (answerRemain.value > 0) answerRemain.value--;
    else {
      stopCountdown();
      if (phase.value === 'listening') {
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

// ==================== 配置表单 ====================
const config = ref<VoiceStartConfig>({
  position: '后端开发',
  scene: 'Java',
  style: 'professional',
  difficulty: 'medium',
  personalized: true,
  hintsEnabled: true,
  stuckThreshold: 30,
});

// ==================== 开始面试 ====================
async function handleStart() {
  loading.value = true;
  try {
    const { data: vo, success } = await run(() => startVoiceInterview(config.value), {
      errorToast: '开始失败',
    });
    if (success && vo?.data) {
      interview.value = vo.data;
      currentQaId.value = vo.data.currentQa?.id ?? null;
      currentQuestion.value = vo.data.currentQa?.question ?? '';
      currentSpeakText.value = vo.data.greetText || vo.data.currentQa?.speakText || '';
      phase.value = 'asking';
      chatList.value = [];
      pushChat('system', '欢迎进入模拟面试，点击开始面试后，这里会显示你面试官的对话历史。');
      if (currentSpeakText.value) pushChat('ai', currentSpeakText.value);
      if (currentQuestion.value && currentQuestion.value !== currentSpeakText.value) {
        pushChat('ai', currentQuestion.value);
      }
      interviewStartAt.value = Date.now();
      elapsedSec.value = 0;
      timerHandle = setInterval(() => { elapsedSec.value = Math.floor((Date.now() - interviewStartAt.value) / 1000); }, 1000);
      if (ttsSupported.value && currentSpeakText.value) {
        ttsSpeak(currentSpeakText.value);
      }
      toast.success('面试已开始');
    }
  } finally {
    loading.value = false;
  }
}

function formatElapsed(sec: number) {
  const m = Math.floor(sec / 60).toString().padStart(2, '0');
  const s = (sec % 60).toString().padStart(2, '0');
  return `${m}:${s}`;
}

// ==================== 开始聆听 ====================
function handleStartListening() {
  if (!asrSupported.value) {
    toast.warning('浏览器不支持语音识别，请在下方文字框输入答案');
    phase.value = 'listening';
    startCountdown();
    return;
  }
  if (speaking.value) {
    ttsCancel();
  }
  resetAsr();
  editableAnswer.value = '';
  answerStartTime.value = Date.now();
  startAsr();
  phase.value = 'listening';
  startCountdown();
}

// ==================== 提交答案（SSE） ====================
async function handleSubmitAnswer() {
  if (!interview.value || !currentQaId.value) return;
  const transcript = editableAnswer.value || finalText.value;
  if (!transcript.trim()) {
    toast.warning('答案不能为空');
    return;
  }
  // 停止 ASR/倒计时
  if (listening.value) stopAsr();
  stopCountdown();
  submitting.value = true;
  phase.value = 'analyzing';
  pushChat('user', transcript);
  const latencyMs = answerStartTime.value ? Date.now() - answerStartTime.value : 0;

  await submitVoiceAnswer(String(interview.value.id), String(currentQaId.value), transcript, latencyMs, {
    onScore: (data) => {
      toast.info(`规则评分：${data.score} 分`);
    },
    onSpeak: (text) => {
      if (text) pushChat('ai', text, '评分反馈');
      if (ttsSupported.value && text) {
        ttsSpeak(text);
      }
    },
    onData: (data) => {
      // 处理下一步动作
      if (data.nextAction === 'next' && data.nextQaId) {
        currentQaId.value = data.nextQaId;
        currentQuestion.value = data.nextQuestion || '';
        currentSpeakText.value = data.nextSpeakText || '';
        if (currentQuestion.value) pushChat('ai', currentQuestion.value);
      } else if (data.nextAction === 'followup' && data.nextQaId) {
        currentQaId.value = data.nextQaId;
        currentQuestion.value = data.nextQuestion || '';
        currentSpeakText.value = data.nextSpeakText || '';
        toast.info('面试官追问，请补充回答');
        if (currentQuestion.value) pushChat('ai', currentQuestion.value, '追问');
      } else if (data.nextAction === 'report') {
        toast.info('面试结束，正在生成报告...');
        handleFinish();
        return;
      }
      // 回到 asking
      phase.value = 'asking';
      editableAnswer.value = '';
      resetAsr();
      if (ttsSupported.value && currentSpeakText.value) {
        setTimeout(() => ttsSpeak(currentSpeakText.value), 500);
      }
    },
    onEnd: () => {
      submitting.value = false;
    },
    onError: (msg) => {
      toast.error(msg || '提交失败');
      submitting.value = false;
      phase.value = 'listening';
    },
  });
}

// ==================== 请求提示 ====================
async function handleRequestHint() {
  if (!interview.value || !currentQaId.value) return;
  hintLoading.value = true;
  try {
    const { data: vo, success } = await run(
      () => requestVoiceHint(interview.value.id, String(currentQaId.value)),
      { errorToast: '提示获取失败' },
    );
    if (success && vo?.data) {
      const hintUsed = vo.data.currentQa?.hintUsed ?? 0;
      const speak = vo.data.currentQa?.speakText;
      if (speak) pushChat('ai', `💡 ${speak}`, `L${hintUsed} 提示`);
      toast.success(`已获取 L${hintUsed} 提示（共3级）`);
      if (ttsSupported.value && speak) {
        ttsSpeak(speak);
      }
    }
  } finally {
    hintLoading.value = false;
  }
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
      if (vo.data.status === 'finished') {
        await handleFinish();
      } else if (vo.data.currentQa) {
        currentQaId.value = vo.data.currentQa.id;
        currentQuestion.value = vo.data.currentQa.question;
        currentSpeakText.value = vo.data.currentQa.speakText || '';
        phase.value = 'asking';
        editableAnswer.value = '';
        resetAsr();
        if (currentQuestion.value) pushChat('ai', currentQuestion.value, '下一题');
        if (ttsSupported.value && currentSpeakText.value) {
          setTimeout(() => ttsSpeak(currentSpeakText.value), 300);
        }
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
  if (timerHandle) { clearInterval(timerHandle); timerHandle = null; }
  loading.value = true;
  try {
    const { data: reportVo, success } = await run(
      () => finishVoiceInterview(interview.value!.id),
      { errorToast: '结束面试失败' },
    );
    if (success && reportVo?.data) {
      report.value = reportVo.data;
      phase.value = 'report';
      toast.success('面试已结束，报告已生成');
    }
  } finally {
    loading.value = false;
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
  resetAsr();
  ttsCancel();
  stopCountdown();
  if (timerHandle) { clearInterval(timerHandle); timerHandle = null; }
  elapsedSec.value = 0;
  phase.value = 'setup';
}

// ==================== TTS 控制 ====================
function handlePauseResume() {
  if (paused.value) ttsResume();
  else ttsPause();
}
function handleReplayQuestion() {
  if (currentSpeakText.value) ttsSpeak(currentSpeakText.value);
}

// ==================== 实时答案同步 ====================
const displayAnswer = computed(() => {
  if (editableAnswer.value) return editableAnswer.value;
  if (finalText.value) return finalText.value;
  return interimText.value;
});

// ASR 实时结果同步到 editableAnswer 气泡预览
watch(displayAnswer, (v) => {
  if (!editableAnswer.value && v && phase.value === 'listening') {
    // 仅预览，不覆盖
  }
});

// ==================== 进度 ====================
const progress = computed(() => {
  if (!interview.value) return 0;
  const idx = interview.value.currentIdx ?? 0;
  const total = interview.value.totalQa ?? 5;
  return Math.round(((idx + 1) / total) * 100);
});

// ==================== 报告对话记录（来自 report） ====================
const reportDialogList = computed<ChatBubble[]>(() => {
  const list: ChatBubble[] = [];
  (report.value?.questionReviews ?? []).forEach((r) => {
    list.push({ id: `q-${r.questionIdx}`, role: 'ai', content: r.question, createdAt: 0 });
    if (r.userAnswer) list.push({ id: `a-${r.questionIdx}`, role: 'user', content: r.userAnswer, createdAt: 0 });
  });
  return list;
});

// ==================== 雷达图数据（5 维，缺失则兜底） ====================
const radarAxes = [
  { key: 'relevance', label: '回答相关性' },
  { key: 'professional', label: '专业度' },
  { key: 'confidence', label: '自信度' },
  { key: 'interactivity', label: '面试互动性' },
  { key: 'fluency', label: '表达流畅度' },
];
const radarValues = computed<number[]>(() => {
  const dims = (report.value?.dimensions ?? {}) as Record<string, number>;
  return radarAxes.map((a) => {
    const v = dims[a.key] ?? dims[a.label];
    return typeof v === 'number' ? v : (report.value?.totalScore ?? 70);
  });
});

// ==================== 卸载清理 ====================
onUnmounted(() => {
  if (listening.value) stopAsr();
  ttsCancel();
  stopCountdown();
  if (timerHandle) clearInterval(timerHandle);
});
</script>

<template>
  <div class="vi-app">
    <!-- ============================================================= -->
    <!-- 全局顶部栏（setup 时隐藏）                                    -->
    <!-- ============================================================= -->
    <header v-if="phase !== 'setup'" class="vi-topbar">
      <div class="vi-topbar-inner vi-shell">
        <div class="vi-brand">
          <span class="vi-brand-emoji">🪶</span>
          <span class="vi-brand-name">墨韵 · AI 面试官</span>
        </div>
        <div class="vi-topbar-right">
          <div class="vi-duration">
            <Clock :size="14" />
            <span>面试时长：{{ formatElapsed(elapsedSec) }}</span>
          </div>
          <button v-if="phase !== 'report'" class="vi-btn vi-btn-danger" @click="handleFinish">
            <span class="dot-red"></span>
            结束面试
          </button>
        </div>
      </div>
    </header>

    <!-- ============================================================= -->
    <!-- Phase: Setup 配置页                                           -->
    <!-- ============================================================= -->
    <header v-if="phase === 'setup' && !route.query.id" class="vi-topbar">
      <div class="vi-topbar-inner vi-shell">
        <div class="vi-brand">
          <span class="vi-brand-emoji">🪶</span>
          <span class="vi-brand-name">墨韵 · AI 面试官</span>
        </div>
        <router-link to="/interview" class="vi-btn vi-btn-ghost">
          <ArrowLeft :size="15" /> 返回面试空间
        </router-link>
      </div>
    </header>

    <section v-if="phase === 'setup'" class="vi-setup vi-shell">
      <div class="vi-setup-card">
        <div class="vi-setup-hero">
          <div class="vi-avatar vi-avatar-xl">
            <Bot :size="42" />
          </div>
          <h1 class="vi-setup-title">AI 语音面试官</h1>
          <p class="vi-setup-subtitle">
            沉浸式语音面试体验 · TTS 播报题目 · ASR 识别回答 · 智能评分反馈
          </p>
          <div class="vi-status-chips">
            <span :class="['vi-chip', ttsSupported ? 'ok' : 'fail']">
              <Volume2 :size="12" /> TTS {{ ttsSupported ? '已就绪' : '不支持' }}
            </span>
            <span :class="['vi-chip', asrSupported ? 'ok' : 'fail']">
              <Mic :size="12" /> ASR {{ asrSupported ? '已就绪' : '不支持' }}
            </span>
            <span v-if="!asrSupported" class="vi-chip warn">
              <AlertCircle :size="12" /> 已切换文字模式
            </span>
          </div>
        </div>

        <div class="vi-form-grid">
          <label class="vi-field">
            <span>面试岗位</span>
            <input v-model="config.position" class="vi-input" placeholder="如：Java 后端 / 前端" />
          </label>
          <label class="vi-field">
            <span>面试场景</span>
            <input v-model="config.scene" class="vi-input" placeholder="如：算法 / 系统设计 / 项目深挖" />
          </label>
          <label class="vi-field">
            <span>面试官风格</span>
            <select v-model="config.style" class="vi-input">
              <option value="professional">🧑‍💼 专业客观</option>
              <option value="friendly">😊 亲和鼓励</option>
              <option value="strict">🎯 严格压力</option>
            </select>
          </label>
          <label class="vi-field">
            <span>面试难度</span>
            <select v-model="config.difficulty" class="vi-input">
              <option value="easy">🌱 入门</option>
              <option value="medium">🌿 中等</option>
              <option value="hard">🌳 高级</option>
            </select>
          </label>
          <label class="vi-field vi-field-row">
            <input v-model="config.personalized" type="checkbox" />
            <span>基于我的画像出题（薄弱点优先）</span>
          </label>
          <label class="vi-field vi-field-row">
            <input v-model="config.hintsEnabled" type="checkbox" />
            <span>开启提示功能（卡住时可请求分级提示）</span>
          </label>
        </div>

        <button class="vi-btn vi-btn-primary vi-btn-xl" :disabled="loading" @click="handleStart">
          <Loader2 v-if="loading" :size="18" class="spin" />
          <Mic v-else :size="18" />
          开始语音面试
        </button>
      </div>
    </section>

    <!-- ============================================================= -->
    <!-- Phase: Asking/Listening/Analyzing  —— 三栏面试页              -->
    <!-- ============================================================= -->
    <section v-else-if="phase !== 'report'" class="vi-interview vi-shell">
      <!-- 左栏：面试官 + 题目 -->
      <aside class="vi-col vi-col-left">
        <div class="vi-card vi-interviewer-card">
          <div class="vi-avatar vi-avatar-lg">
            <Bot :size="28" />
          </div>
          <div class="vi-interviewer-name">面试官</div>
          <div class="vi-interviewer-tag">
            <Sparkles :size="12" /> AI 驱动
          </div>
        </div>

        <div class="vi-card vi-question-card">
          <div class="vi-question-header">
            <Lightbulb :size="18" />
            <span>当前题目 · 第 {{ (interview?.currentIdx ?? 0) + 1 }}/{{ interview?.totalQa ?? '?' }} 题</span>
          </div>
          <div class="vi-question-text">{{ currentQuestion || '题目加载中...' }}</div>
          <div class="vi-question-meta">
            <span class="vi-style-tag">{{ config.style === 'friendly' ? '亲和' : config.style === 'strict' ? '严格' : '专业' }}</span>
            <span class="vi-diff-tag">
              难度：{{ config.difficulty === 'easy' ? '入门' : config.difficulty === 'hard' ? '高级' : '中等' }}
            </span>
          </div>
          <div class="vi-question-actions">
            <button class="vi-btn vi-btn-ghost-sm" :disabled="!ttsSupported || speaking" @click="handleReplayQuestion">
              <Volume2 :size="13" /> 重播
            </button>
            <button v-if="speaking" class="vi-btn vi-btn-ghost-sm" @click="handlePauseResume">
              {{ paused ? '恢复 TTS' : '暂停 TTS' }}
            </button>
          </div>
          <div class="vi-progress-mini">
            <div class="vi-progress-bar-mini" :style="{ width: progress + '%' }"></div>
          </div>
        </div>
      </aside>

      <!-- 中栏：对话气泡 + 底部输入条 -->
      <main class="vi-col vi-col-center">
        <div class="vi-card vi-chat-card">
          <div class="chat-scroll">
            <div
              v-for="b in chatList"
              :key="b.id"
              :class="['vi-bubble', `role-${b.role}`]"
            >
              <div v-if="b.role !== 'user'" class="vi-bubble-avatar">
                <Bot v-if="b.role === 'ai'" :size="16" />
                <Sparkles v-else :size="16" />
              </div>
              <div class="vi-bubble-body">
                <div v-if="b.role !== 'user'" class="vi-bubble-head">
                  <span class="vi-bubble-role">{{ b.role === 'ai' ? 'AI' : '系统' }}</span>
                  <span v-if="b.tag" class="vi-bubble-tag">{{ b.tag }}</span>
                </div>
                <div class="vi-bubble-content">{{ b.content }}</div>
              </div>
            </div>
            <div v-if="phase === 'listening' && displayAnswer" class="vi-bubble role-user typing">
              <div class="vi-bubble-avatar">
                <User :size="16" />
              </div>
              <div class="vi-bubble-body">
                <div class="vi-bubble-head">
                  <span class="vi-bubble-role">你</span>
                  <span class="vi-bubble-tag typing-tag">{{ listening ? '聆听中...' : '转写完成' }}</span>
                </div>
                <div class="vi-bubble-content">{{ displayAnswer }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部输入条 -->
        <div class="vi-input-bar">
          <div v-if="phase === 'listening'" class="vi-countdown" :class="{ warn: answerRemain <= 10 }">
            <Timer :size="14" /> {{ answerRemain }} 秒内作答
          </div>
          <div v-else class="vi-input-hint">
            {{ phase === 'asking' ? '点击"开始作答"按钮开始语音回答' :
               phase === 'analyzing' ? 'AI 正在评分与生成反馈...' : '' }}
          </div>

          <div class="vi-textarea-wrap">
            <span class="vi-textarea-ph">🎙️ 面试官你好，我认为...</span>
            <textarea
              v-model="editableAnswer"
              class="vi-textarea"
              :disabled="phase === 'analyzing'"
              placeholder="语音自动转写，也可手动输入..."
              rows="2"
              @input="resetAsr()"
            ></textarea>
          </div>

          <div class="vi-input-actions">
            <button class="vi-btn vi-btn-ghost-sm" :disabled="!editableAnswer" @click="editableAnswer = ''">
              <Trash2 :size="13" /> 清空文本
            </button>

            <button
              v-if="phase === 'asking'"
              class="vi-btn vi-btn-primary"
              @click="handleStartListening"
            >
              <Mic :size="15" /> 开始作答
            </button>
            <button
              v-else-if="phase === 'listening'"
              class="vi-btn vi-btn-success"
              :disabled="submitting || !displayAnswer.trim()"
              @click="handleSubmitAnswer"
            >
              <CheckCircle2 :size="15" /> 回答完毕
            </button>
            <button
              v-else-if="phase === 'analyzing'"
              class="vi-btn vi-btn-success"
              disabled
            >
              <Loader2 :size="15" class="spin" /> 评分分析中
            </button>

            <button
              class="vi-btn vi-btn-ghost-sm"
              :disabled="hintLoading || !config.hintsEnabled"
              @click="handleRequestHint"
            >
              <Lightbulb :size="13" />
              <span v-if="!hintLoading">提示</span>
              <Loader2 v-else :size="13" class="spin" />
            </button>

            <button
              class="vi-btn vi-btn-ghost-sm"
              :disabled="loading"
              @click="handleForceNext"
            >
              <ChevronRight :size="13" /> 下一题
            </button>
          </div>
        </div>
      </main>

      <!-- 右栏：系统消息 + AI 记录 -->
      <aside class="vi-col vi-col-right">
        <div class="vi-card">
          <div class="vi-card-head">
            <span class="vi-role-chip role-system">系统</span>
          </div>
          <p class="vi-right-text">
            你好，欢迎进入模拟面试，点击开始面试后，这里会显示你面试官的对话历史。
          </p>
        </div>

        <div class="vi-card">
          <div class="vi-card-head">
            <span class="vi-role-chip role-ai">面试官</span>
          </div>
          <p class="vi-right-text">
            在跨部门或跨团队的项目合作中，你是如何有效地与来自不同背景、专业领域和工作方式的同事进行沟通和协作的？
            <br /><br />
            能否举例说明你曾经遇到的挑战，以及你是如何克服这些挑战，最终实现团队目标的？
          </p>
        </div>

        <div class="vi-card vi-record-card">
          <div class="vi-card-head">
            <span class="vi-role-chip role-ai">AI</span>
          </div>
          <p class="vi-right-text vi-right-quote">
            {{ (chatList.filter(c => c.role === 'user').slice(-1)[0]?.content) || '这里会完整显示你刚才的作答记录，便于复盘...' }}
          </p>
        </div>
      </aside>
    </section>

    <!-- ============================================================= -->
    <!-- Phase: Report  报告页                                         -->
    <!-- ============================================================= -->
    <section v-else-if="phase === 'report' && report" class="vi-report">
      <!-- 报告顶栏 -->
      <header class="vi-topbar vi-topbar-report">
        <div class="vi-topbar-inner vi-shell">
          <div class="vi-brand">
            <span class="vi-brand-emoji">🪶</span>
            <span class="vi-brand-name">墨韵 · 面试报告</span>
          </div>
          <router-link to="/interview" class="vi-btn vi-btn-ghost">
            <Home :size="14" /> 返回首页
          </router-link>
        </div>
      </header>

      <div class="vi-report-body vi-shell">
        <!-- 左：面试对话列表 -->
        <aside class="vi-report-dialog">
          <div class="vi-card vi-report-dialog-card">
            <div class="vi-card-title">面试对话</div>
            <div class="vi-dialog-scroll">
              <div
                v-for="b in reportDialogList"
                :key="b.id"
                :class="['vi-dialog-item', `role-${b.role}`]"
              >
                <div class="vi-dialog-role">
                  <Bot v-if="b.role === 'ai'" :size="13" /> 面试官
                </div>
                <div class="vi-dialog-role role-user">
                  <User :size="13" /> 你
                </div>
                <div class="vi-dialog-content">
                  <template v-if="b.role === 'ai'">{{ b.content }}</template>
                  <template v-else>{{ b.content }}</template>
                </div>
              </div>
              <div v-if="!reportDialogList.length" class="vi-empty">暂无对话记录</div>
            </div>
          </div>
        </aside>

        <!-- 右：Tab 内容 -->
        <main class="vi-report-main">
          <div class="vi-card">
            <nav class="vi-tab-bar">
              <button
                v-for="t in ([
                  ['dialog','面试对话'],
                  ['summary','面试概要'],
                  ['analysis','问题分析'],
                  ['review','面试官剖析'],
                  ['knowledge','相关知识点和概念'],
                ] as const)"
                :key="t[0]"
                :class="['vi-tab', { active: reportTab === t[0] }]"
                @click="reportTab = t[0]"
              >
                {{ t[1] }}
              </button>
            </nav>

            <!-- =============== Tab: 面试概要 =============== -->
            <div v-if="reportTab === 'summary'" class="vi-tab-pane">
              <h3 class="vi-section-title">面试概要</h3>

              <div class="vi-summary-grid">
                <!-- 雷达图 -->
                <div class="vi-card vi-radar-card">
                  <svg viewBox="-120 -120 240 240" class="vi-radar-svg">
                    <!-- 背景 5 边形网格 3 层 -->
                    <polygon
                      v-for="(r, idx) in [40, 72, 100]"
                      :key="'g'+idx"
                      :points="radarAxes.map((_, i) => {
                        const a = (Math.PI * 2 * i) / 5 - Math.PI / 2;
                        return (r * Math.cos(a)).toFixed(1) + ',' + (r * Math.sin(a)).toFixed(1);
                      }).join(' ')"
                      fill="none" stroke="#E6EAF2"
                    />
                    <!-- 轴线 -->
                    <line
                      v-for="(_, i) in radarAxes"
                      :key="'a'+i"
                      x1="0" y1="0"
                      :x2="(100 * Math.cos((Math.PI * 2 * i) / 5 - Math.PI / 2)).toFixed(1)"
                      :y2="(100 * Math.sin((Math.PI * 2 * i) / 5 - Math.PI / 2)).toFixed(1)"
                      stroke="#E6EAF2"
                    />
                    <!-- 数据多边形 -->
                    <polygon
                      :points="radarAxes.map((_, i) => {
                        const v = (radarValues[i] ?? 0) / 100;
                        const r = 95 * v;
                        const a = (Math.PI * 2 * i) / 5 - Math.PI / 2;
                        return (r * Math.cos(a)).toFixed(1) + ',' + (r * Math.sin(a)).toFixed(1);
                      }).join(' ')"
                      fill="rgba(63,168,111,0.25)"
                      stroke="#3FA86F"
                      stroke-width="2"
                    />
                    <!-- 轴标签 -->
                    <g v-for="(ax, i) in radarAxes" :key="'l'+i">
                      <circle
                        :cx="((radarValues[i] ?? 0) / 100 * 95 * Math.cos((Math.PI * 2 * i) / 5 - Math.PI / 2)).toFixed(1)"
                        :cy="((radarValues[i] ?? 0) / 100 * 95 * Math.sin((Math.PI * 2 * i) / 5 - Math.PI / 2)).toFixed(1)"
                        r="3.5"
                        fill="#3FA86F"
                      />
                      <text
                        :x="(118 * Math.cos((Math.PI * 2 * i) / 5 - Math.PI / 2)).toFixed(1)"
                        :y="(118 * Math.sin((Math.PI * 2 * i) / 5 - Math.PI / 2)).toFixed(1)"
                        text-anchor="middle"
                        dominant-baseline="middle"
                        class="vi-radar-label"
                      >
                        {{ ax.label }}
                      </text>
                    </g>
                  </svg>
                </div>

                <!-- 概要描述 + 总分 -->
                <div class="vi-card vi-summary-text">
                  <div class="vi-summary-top">
                    <div class="vi-score-ring">
                      <span class="vi-score-num">{{ report.totalScore ?? '-' }}</span>
                      <span class="vi-score-unit">/100 分</span>
                    </div>
                    <div class="vi-summary-meta">
                      <div class="vi-meta-row">
                        <span>岗位</span><b>{{ interview?.position || '-' }}</b>
                      </div>
                      <div class="vi-meta-row">
                        <span>场景</span><b>{{ interview?.scene || '-' }}</b>
                      </div>
                      <div class="vi-meta-row">
                        <span>用时</span><b>{{ formatElapsed(elapsedSec) }}</b>
                      </div>
                      <div class="vi-meta-row">
                        <span>题量</span><b>{{ interview?.totalQa ?? '-' }} 题</b>
                      </div>
                    </div>
                  </div>
                  <div class="vi-summary-desc">
                    {{ report.summary || '（暂无文字概要）' }}
                  </div>
                </div>
              </div>

              <!-- 缺点与优点 -->
              <div class="vi-points-grid">
                <div class="vi-card vi-points-card weak">
                  <h4 class="vi-points-title">
                    <span class="vi-p-icon red"><AlertCircle :size="16" /></span>
                    缺点
                  </h4>
                  <ul v-if="(report.weakPoints ?? []).length">
                    <li v-for="(w, i) in report.weakPoints" :key="'w'+i">
                      <span class="vi-points-main">{{ typeof w === 'string' ? w : (w.text || '') }}</span>
                      <span v-if="typeof w !== 'string' && w.quote" class="vi-points-quote">❝ {{ w.quote }} ❞</span>
                    </li>
                  </ul>
                  <div v-else class="vi-empty">暂无不足点</div>
                </div>

                <div class="vi-card vi-points-card high">
                  <h4 class="vi-points-title">
                    <span class="vi-p-icon green"><CheckCircle2 :size="16" /></span>
                    优点
                  </h4>
                  <ul v-if="(report.highlights ?? []).length">
                    <li v-for="(h, i) in report.highlights" :key="'h'+i">
                      <span class="vi-points-main">{{ typeof h === 'string' ? h : (h.text || '') }}</span>
                      <span v-if="typeof h !== 'string' && h.quote" class="vi-points-quote">❝ {{ h.quote }} ❞</span>
                    </li>
                  </ul>
                  <div v-else class="vi-empty">暂无亮点记录</div>
                </div>
              </div>
            </div>

            <!-- =============== Tab: 面试对话（重复展示左栏风格） =============== -->
            <div v-else-if="reportTab === 'dialog'" class="vi-tab-pane">
              <h3 class="vi-section-title">完整面试对话</h3>
              <div class="vi-dialog-full">
                <div
                  v-for="(b, i) in reportDialogList"
                  :key="'d'+i"
                  :class="['vi-dialog-item', `role-${b.role}`]"
                >
                  <div class="vi-dialog-role">
                    <Bot v-if="b.role === 'ai'" :size="13" /> 面试官
                  </div>
                  <div class="vi-dialog-role role-user">
                    <User :size="13" /> 你
                  </div>
                  <div class="vi-dialog-content">{{ b.content }}</div>
                </div>
                <div v-if="!reportDialogList.length" class="vi-empty">暂无对话</div>
              </div>
            </div>

            <!-- =============== Tab: 问题分析 =============== -->
            <div v-else-if="reportTab === 'analysis'" class="vi-tab-pane">
              <h3 class="vi-section-title">逐题问题分析</h3>
              <div v-if="(report.questionReviews ?? []).length" class="vi-reviews">
                <div v-for="r in report.questionReviews" :key="'r'+r.questionIdx" class="vi-review-card vi-card">
                  <div class="vi-review-head">
                    <span class="vi-review-idx">第 {{ r.questionIdx + 1 }} 题</span>
                    <span :class="['vi-review-score', r.score >= 80 ? 'good' : r.score >= 60 ? 'ok' : 'bad']">
                      {{ r.score ?? '-' }} 分
                    </span>
                  </div>
                  <div class="vi-review-question">{{ r.question }}</div>
                  <div class="vi-review-block">
                    <label>你的回答</label>
                    <p>{{ r.userAnswer || '（未作答）' }}</p>
                  </div>
                  <div class="vi-review-block">
                    <label>问题分析</label>
                    <p>{{ r.analysis || r.feedback || '（暂无分析）' }}</p>
                  </div>
                </div>
              </div>
              <div v-else class="vi-empty">暂无逐题分析</div>
            </div>

            <!-- =============== Tab: 面试官剖析 =============== -->
            <div v-else-if="reportTab === 'review'" class="vi-tab-pane">
              <h3 class="vi-section-title">面试官整体剖析</h3>
              <div class="vi-card vi-review-overall">
                <div class="vi-review-overall-head">
                  <div class="vi-avatar vi-avatar-md"><Bot :size="22" /></div>
                  <div>
                    <div class="vi-interviewer-name">面试官点评</div>
                    <div class="vi-interviewer-tag"><Sparkles :size="12" /> AI 模拟面试官</div>
                  </div>
                </div>
                <p class="vi-review-overall-text">
                  {{ report.summary || '（暂无总结）' }}
                </p>
              </div>
              <div v-if="report.suggestion" class="vi-card vi-suggest-card">
                <h4 class="vi-points-title">
                  <span class="vi-p-icon blue"><TrendingUp :size="16" /></span>
                  提升建议
                </h4>
                <p>{{ report.suggestion }}</p>
              </div>
            </div>

            <!-- =============== Tab: 相关知识点 =============== -->
            <div v-else-if="reportTab === 'knowledge'" class="vi-tab-pane">
              <h3 class="vi-section-title">相关知识点和概念</h3>
              <div class="vi-card vi-knowledge-card">
                <div v-if="(report.knowledgePoints ?? []).length" class="vi-knowledge-list">
                  <div v-for="(k, i) in report.knowledgePoints" :key="'k'+i" class="vi-knowledge-item">
                    <div class="vi-knowledge-name">{{ typeof k === 'string' ? k : (k.title || k.name || '') }}</div>
                    <div v-if="typeof k !== 'string' && (k.desc || k.description)" class="vi-knowledge-desc">{{ k.desc || k.description }}</div>
                  </div>
                </div>
                <div v-else class="vi-empty">
                  暂无自动归纳知识点。你可在面试报告页结合问题分析自行整理相关考点。
                </div>
              </div>
            </div>
          </div>

          <!-- 报告操作 -->
          <div class="vi-report-actions">
            <button class="vi-btn vi-btn-primary" @click="handleRestart">
              <RefreshCw :size="15" /> 再来一场
            </button>
            <router-link to="/interview/my/attempts" class="vi-btn vi-btn-ghost">
              <Award :size="15" /> 我的面试记录
            </router-link>
            <router-link to="/interview" class="vi-btn vi-btn-ghost">
              <ArrowLeft :size="15" /> 返回面试列表
            </router-link>
          </div>
        </main>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ==================== 全局色板（OfferGoose 米绿风） ==================== */
:root {
  --vi-green: #3FA86F;      /* 主色 AI 绿 */
  --vi-green-soft: #F0FAF3;
  --vi-green-deep: #2F8A58;
  --vi-amber: #FFF6E5;      /* 题目卡米黄 */
  --vi-amber-deep: #F7B955;
  --vi-red: #D64545;
  --vi-red-soft: #FCEAEA;
  --vi-blue-soft: #EEF4FF;
  --vi-text-1: #1F2937;
  --vi-text-2: #5E6875;
  --vi-text-3: #8B94A3;
  --vi-border: #E6EAF2;
  --vi-bg: #F6F8FA;
  --vi-card: #FFFFFF;
}

.vi-app {
  min-height: 100vh;
  background: var(--vi-bg);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI",
    "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  color: var(--vi-text-1);
  font-size: 14px;
}

/* 与首页 section 一致的外层宽度约束：max-w-7xl(1280px) + 响应式 padding */
.vi-shell {
  max-width: 1280px;
  margin-left: auto;
  margin-right: auto;
  padding-left: 16px;
  padding-right: 16px;
}
@media (min-width: 640px) {
  .vi-shell { padding-left: 24px; padding-right: 24px; }
}
@media (min-width: 1024px) {
  .vi-shell { padding-left: 32px; padding-right: 32px; }
}

/* ==================== 顶栏 ==================== */
.vi-topbar {
  background: #fff;
  border-bottom: 1px solid var(--vi-border);
  position: sticky;
  top: 0;
  z-index: 50;
}
.vi-topbar-inner {
  /* 使用与首页一致的 vi-shell：max-width + padding 响应式 */
  padding-top: 10px;
  padding-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.vi-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: var(--vi-green-deep);
}
.vi-brand-emoji { font-size: 20px; }
.vi-brand-name { font-size: 15px; letter-spacing: 0.3px; }
.vi-topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}
.vi-duration {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--vi-text-2);
  font-size: 13px;
  font-variant-numeric: tabular-nums;
}

/* ==================== 按钮 ==================== */
.vi-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  border: 1px solid var(--vi-border);
  background: #fff;
  color: var(--vi-text-1);
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  transition: all 0.15s ease;
  text-decoration: none;
  white-space: nowrap;
}
.vi-btn:hover:not(:disabled) { background: #F7F9FC; }
.vi-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.vi-btn-xl { padding: 13px 28px; font-size: 15px; }
.vi-btn-primary { background: var(--vi-green); border-color: var(--vi-green); color: #fff; font-weight: 600; }
.vi-btn-primary:hover:not(:disabled) { background: var(--vi-green-deep); border-color: var(--vi-green-deep); }
.vi-btn-success { background: var(--vi-green); border-color: var(--vi-green); color: #fff; font-weight: 600; }
.vi-btn-success:hover:not(:disabled) { background: var(--vi-green-deep); }
.vi-btn-danger { background: var(--vi-red-soft); border-color: transparent; color: var(--vi-red); font-weight: 600; }
.vi-btn-danger:hover:not(:disabled) { background: #F8DADA; }
.vi-btn-ghost { background: transparent; }
.vi-btn-ghost-sm { padding: 5px 10px; font-size: 12px; background: transparent; }
.vi-btn-ghost-sm:hover:not(:disabled) { background: #F3F5F9; }
.dot-red {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--vi-red);
  box-shadow: 0 0 0 3px rgba(214, 69, 69, 0.18);
  animation: blinkDot 1.2s infinite;
}
@keyframes blinkDot { 50% { opacity: 0.4; } }

/* ==================== 卡片 / chip ==================== */
.vi-card {
  background: var(--vi-card);
  border: 1px solid var(--vi-border);
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.03);
}
.vi-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}
.vi-chip.ok { background: #E6F7ED; color: var(--vi-green-deep); }
.vi-chip.fail { background: #FBE7E7; color: var(--vi-red); }
.vi-chip.warn { background: #FEF3C7; color: #B45309; }

/* ==================== Setup 配置页 ==================== */
.vi-setup {
  min-height: calc(100vh - 52px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}
.vi-setup-card {
  width: 100%;
  max-width: 920px;
  background: #fff;
  border: 1px solid var(--vi-border);
  border-radius: 18px;
  padding: 40px 44px;
  box-shadow: 0 20px 50px -20px rgba(63, 168, 111, 0.18);
}
.vi-setup-hero { text-align: center; margin-bottom: 28px; }
.vi-avatar {
  width: 56px; height: 56px;
  display: inline-flex;
  align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--vi-green), #74C497);
  color: #fff; border-radius: 50%;
  box-shadow: 0 8px 18px rgba(63, 168, 111, 0.28);
}
.vi-avatar-xl { width: 76px; height: 76px; margin-bottom: 14px; }
.vi-avatar-lg { width: 68px; height: 68px; margin: 0 auto 8px; }
.vi-avatar-md { width: 44px; height: 44px; }
.vi-setup-title {
  font-size: 24px; font-weight: 700; margin: 0 0 6px; color: var(--vi-text-1);
}
.vi-setup-subtitle { color: var(--vi-text-2); font-size: 13.5px; margin: 0 0 16px; }
.vi-status-chips { display: flex; justify-content: center; gap: 8px; flex-wrap: wrap; }
.vi-form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 22px;
}
.vi-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 13px;
  color: var(--vi-text-2);
}
.vi-field-row {
  flex-direction: row;
  align-items: center;
  gap: 8px;
  color: var(--vi-text-1);
  font-size: 13px;
  padding: 6px 2px;
}
.vi-input {
  padding: 9px 12px;
  border: 1px solid var(--vi-border);
  border-radius: 8px;
  font-size: 13.5px;
  background: #F9FAFC;
  color: var(--vi-text-1);
  transition: all 0.15s;
}
.vi-input:focus {
  outline: none;
  background: #fff;
  border-color: var(--vi-green);
  box-shadow: 0 0 0 3px rgba(63, 168, 111, 0.12);
}
.vi-setup > .vi-setup-card > .vi-btn-primary {
  width: 100%;
  padding: 12px;
  font-size: 15px;
}

/* ==================== 面试页 3 栏布局 ==================== */
.vi-interview {
  /* 外层宽度约束交给 .vi-shell，与首页 section 一致 */
  padding-top: 18px;
  padding-bottom: 24px;
  display: grid;
  grid-template-columns: 300px 1fr 320px;
  gap: 16px;
  align-items: start;
}
.vi-col { display: flex; flex-direction: column; gap: 14px; }

/* 左栏 */
.vi-interviewer-card { text-align: center; }
.vi-interviewer-name { font-weight: 700; font-size: 15px; margin-bottom: 4px; }
.vi-interviewer-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--vi-text-2);
  font-size: 12px;
  background: #F5F7FB;
  padding: 3px 10px;
  border-radius: 999px;
}
.vi-question-card { background: var(--vi-amber); border-color: #F5E4B8; }
.vi-question-header {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #9A6A1C;
  font-weight: 600;
  font-size: 12.5px;
  margin-bottom: 10px;
}
.vi-question-text {
  color: #4A3A10;
  line-height: 1.65;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}
.vi-question-meta { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.vi-style-tag, .vi-diff-tag {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(255,255,255,0.7);
  color: #8A5E12;
  border: 1px solid #F0DDA6;
}
.vi-question-actions { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.vi-progress-mini { height: 5px; background: #F5E4B8; border-radius: 999px; overflow: hidden; }
.vi-progress-bar-mini {
  height: 100%;
  background: linear-gradient(90deg, var(--vi-amber-deep), var(--vi-green));
  transition: width 0.4s;
}

/* 中栏 */
.vi-chat-card {
  padding: 0;
  height: calc(100vh - 260px);
  min-height: 400px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.chat-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background:
    radial-gradient(1200px 300px at 80% -10%, #F2FBF5 0%, transparent 70%),
    #FCFDFE;
}
.vi-bubble {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  max-width: 88%;
}
.vi-bubble.role-user { align-self: flex-end; flex-direction: row-reverse; }
.vi-bubble-avatar {
  width: 28px; height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center; justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.vi-bubble.role-ai .vi-bubble-avatar { background: var(--vi-green); }
.vi-bubble.role-system .vi-bubble-avatar { background: #8B94A3; }
.vi-bubble.role-user .vi-bubble-avatar { background: #6366F1; }
.vi-bubble-body {
  padding: 10px 13px;
  border-radius: 12px;
  line-height: 1.65;
  word-break: break-word;
}
.vi-bubble.role-ai .vi-bubble-body {
  background: var(--vi-green-soft);
  border-top-left-radius: 4px;
  color: #1F3C2A;
}
.vi-bubble.role-system .vi-bubble-body {
  background: #F2F4F8;
  color: var(--vi-text-2);
  font-size: 13px;
  border-top-left-radius: 4px;
}
.vi-bubble.role-user .vi-bubble-body {
  background: var(--vi-blue-soft);
  border-top-right-radius: 4px;
  color: #1E2A5A;
}
.vi-bubble-head {
  display: flex; align-items: center; gap: 6px; margin-bottom: 3px;
  font-size: 11.5px;
}
.vi-bubble-role {
  font-weight: 700;
  color: var(--vi-green-deep);
  background: rgba(63,168,111,0.14);
  padding: 1px 8px;
  border-radius: 999px;
}
.role-user .vi-bubble-role {
  background: rgba(99, 102, 241, 0.15);
  color: #4338CA;
}
.role-system .vi-bubble-role {
  background: #E3E7EF; color: #5B6472;
}
.vi-bubble-tag {
  color: var(--vi-text-3);
  font-weight: 500;
}
.vi-bubble.typing .vi-bubble-body { opacity: 0.75; }
.typing-tag {
  color: var(--vi-green-deep) !important;
  font-weight: 600 !important;
  animation: pulse 1s infinite;
}
@keyframes pulse { 50% { opacity: 0.5; } }

/* 输入条 */
.vi-input-bar {
  background: #fff;
  border: 1px solid var(--vi-border);
  border-radius: 14px;
  padding: 10px 12px 12px;
  box-shadow: 0 2px 8px rgba(16, 24, 40, 0.04);
}
.vi-countdown {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: var(--vi-amber-deep);
  background: #FFFAE8;
  border: 1px solid #F3E0A3;
  padding: 3px 10px;
  border-radius: 999px;
  margin-bottom: 8px;
}
.vi-countdown.warn {
  color: var(--vi-red);
  background: var(--vi-red-soft);
  border-color: #F2B6B6;
  animation: pulse 1s infinite;
}
.vi-input-hint {
  font-size: 12px;
  color: var(--vi-text-3);
  margin-bottom: 8px;
}
.vi-textarea-wrap {
  position: relative;
  margin-bottom: 8px;
}
.vi-textarea-ph {
  position: absolute;
  left: 12px; top: 11px;
  pointer-events: none;
  color: #B4BAC5;
  font-size: 13.5px;
}
.vi-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  font-family: inherit;
  font-size: 13.5px;
  line-height: 1.6;
  border: 1px solid var(--vi-border);
  border-radius: 10px;
  background: #F9FAFC;
  color: var(--vi-text-1);
  resize: vertical;
  transition: all 0.15s;
}
.vi-textarea:focus {
  outline: none;
  background: #fff;
  border-color: var(--vi-green);
  box-shadow: 0 0 0 3px rgba(63, 168, 111, 0.12);
}
.vi-textarea:disabled { opacity: 0.55; cursor: not-allowed; }
.vi-input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

/* 右栏 */
.vi-card-head { margin-bottom: 8px; }
.vi-role-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}
.vi-role-chip.role-ai { background: var(--vi-green-soft); color: var(--vi-green-deep); }
.vi-role-chip.role-system { background: #EEF1F6; color: #4E5768; }
.vi-right-text {
  margin: 0;
  color: var(--vi-text-1);
  line-height: 1.7;
  font-size: 13px;
}
.vi-right-quote {
  background: #FAFBFD;
  padding: 10px 12px;
  border-radius: 8px;
  border-left: 3px solid var(--vi-green);
  color: var(--vi-text-2);
  font-size: 13px;
}
.vi-record-card { border-left: 4px solid var(--vi-green); }

/* ==================== 报告页 ==================== */
.vi-report { padding-bottom: 40px; }
.vi-report-body {
  /* 外层宽度约束交给 .vi-shell，与首页 section 一致 */
  padding-top: 18px;
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  align-items: start;
}
.vi-report-dialog-card { height: calc(100vh - 130px); min-height: 560px; display: flex; flex-direction: column; }
.vi-card-title {
  font-weight: 700;
  font-size: 15px;
  margin: 0 0 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--vi-border);
  color: var(--vi-text-1);
}
.vi-dialog-scroll {
  overflow-y: auto;
  padding-right: 4px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}
.vi-dialog-item {
  padding: 10px 12px;
  border-radius: 10px;
  background: #F7F9FC;
}
.vi-dialog-item.role-user { background: var(--vi-blue-soft); }
.vi-dialog-item.role-ai { background: var(--vi-green-soft); }
.vi-dialog-role {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(63,168,111,0.14);
  color: var(--vi-green-deep);
  margin-right: 4px;
  margin-bottom: 5px;
}
.vi-dialog-role.role-user {
  background: rgba(99, 102, 241, 0.14);
  color: #4338CA;
}
.vi-dialog-content {
  font-size: 13px;
  line-height: 1.65;
  color: var(--vi-text-1);
}

/* Tab */
.vi-tab-bar {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: #F3F5F9;
  border-radius: 10px;
  margin: -10px -10px 18px;
}
.vi-tab {
  flex: 1;
  padding: 8px 10px;
  border: 0;
  background: transparent;
  font-size: 13px;
  color: var(--vi-text-2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.vi-tab:hover { color: var(--vi-text-1); }
.vi-tab.active {
  background: #fff;
  color: var(--vi-green-deep);
  box-shadow: 0 1px 3px rgba(16,24,40,0.06);
  font-weight: 700;
}
.vi-tab-pane > .vi-section-title {
  font-size: 17px;
  font-weight: 700;
  margin: 0 0 14px;
  color: var(--vi-text-1);
}

/* 概要 */
.vi-summary-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 14px;
  margin-bottom: 16px;
}
.vi-radar-card { display: flex; align-items: center; justify-content: center; }
.vi-radar-svg { width: 100%; max-width: 320px; height: auto; }
.vi-radar-label { font-size: 11px; fill: var(--vi-text-2); }

.vi-summary-top {
  display: flex;
  gap: 18px;
  align-items: center;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--vi-border);
}
.vi-score-ring {
  width: 96px; height: 96px; border-radius: 50%;
  background: linear-gradient(135deg, var(--vi-green), #74C497);
  color: #fff;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  box-shadow: 0 10px 22px rgba(63, 168, 111, 0.28);
}
.vi-score-num { font-size: 30px; font-weight: 700; line-height: 1; }
.vi-score-unit { font-size: 12px; margin-top: 2px; opacity: 0.9; }
.vi-summary-meta {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 14px;
}
.vi-meta-row {
  display: flex; justify-content: space-between;
  font-size: 13px;
  padding: 5px 10px;
  background: #F6F8FB;
  border-radius: 6px;
}
.vi-meta-row span { color: var(--vi-text-3); }
.vi-meta-row b { color: var(--vi-text-1); font-weight: 600; }
.vi-summary-desc {
  background: #FAFBFD;
  padding: 12px 14px;
  border-radius: 8px;
  border-left: 3px solid var(--vi-green);
  line-height: 1.75;
  color: var(--vi-text-1);
  font-size: 13.5px;
}

/* 优缺点 */
.vi-points-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.vi-points-card.weak { background: #FFFAFA; border-color: #F3CFCF; }
.vi-points-card.high { background: #F3FBF6; border-color: #C8E9D5; }
.vi-points-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 14px; font-weight: 700;
  margin: 0 0 12px;
}
.vi-p-icon {
  width: 24px; height: 24px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
}
.vi-p-icon.red { background: var(--vi-red-soft); color: var(--vi-red); }
.vi-p-icon.green { background: var(--vi-green-soft); color: var(--vi-green-deep); }
.vi-p-icon.blue { background: var(--vi-blue-soft); color: #3B5BDB; }
.vi-points-card ul { margin: 0; padding: 0; list-style: none; }
.vi-points-card li {
  padding: 10px 12px;
  background: #fff;
  border: 1px solid var(--vi-border);
  border-radius: 8px;
  margin-bottom: 8px;
}
.vi-points-card li:last-child { margin-bottom: 0; }
.vi-points-main {
  display: block;
  font-size: 13px;
  line-height: 1.6;
  color: var(--vi-text-1);
}
.vi-points-quote {
  display: block;
  margin-top: 6px;
  padding: 6px 8px;
  background: #F7F9FC;
  border-left: 2px solid var(--vi-amber-deep);
  font-size: 12px;
  color: var(--vi-text-2);
  line-height: 1.55;
  border-radius: 0 6px 6px 0;
}

/* 逐题分析 */
.vi-reviews { display: flex; flex-direction: column; gap: 12px; }
.vi-review-head {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 6px;
}
.vi-review-idx {
  font-size: 12px; font-weight: 600; color: var(--vi-text-3);
  background: #F0F2F6; padding: 2px 9px; border-radius: 999px;
}
.vi-review-score {
  font-size: 13px; font-weight: 700;
  padding: 3px 10px; border-radius: 999px;
}
.vi-review-score.good { background: var(--vi-green-soft); color: var(--vi-green-deep); }
.vi-review-score.ok { background: #FEF3C7; color: #9A6A1C; }
.vi-review-score.bad { background: var(--vi-red-soft); color: var(--vi-red); }
.vi-review-question {
  font-weight: 600; font-size: 14px; margin-bottom: 10px; line-height: 1.6;
}
.vi-review-block { margin-bottom: 10px; }
.vi-review-block:last-child { margin-bottom: 0; }
.vi-review-block label {
  display: block;
  font-size: 11.5px; font-weight: 700;
  color: var(--vi-text-3);
  letter-spacing: 0.4px;
  text-transform: uppercase;
  margin-bottom: 4px;
}
.vi-review-block p {
  margin: 0;
  padding: 9px 11px;
  background: #F7F9FC;
  border-radius: 6px;
  line-height: 1.65;
  font-size: 13px;
  color: var(--vi-text-1);
}

/* 面试官剖析 */
.vi-review-overall-head {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 14px;
}
.vi-review-overall-text {
  margin: 0;
  line-height: 1.8;
  color: var(--vi-text-1);
  font-size: 14px;
}
.vi-suggest-card { margin-top: 14px; }
.vi-suggest-card p {
  margin: 0;
  padding: 10px 12px;
  background: var(--vi-blue-soft);
  border-radius: 8px;
  line-height: 1.75;
  color: #1E2A5A;
  font-size: 13.5px;
}

/* 知识点 */
.vi-knowledge-list { display: flex; flex-direction: column; gap: 8px; }
.vi-knowledge-item {
  padding: 11px 14px;
  background: linear-gradient(90deg, #F0FAF3, #EEF4FF);
  border-radius: 8px;
  border-left: 3px solid var(--vi-green);
}
.vi-knowledge-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--vi-green-deep);
  margin-bottom: 4px;
}
.vi-knowledge-desc {
  font-size: 13px;
  line-height: 1.65;
  color: var(--vi-text-2);
}

.vi-empty {
  text-align: center;
  padding: 20px 10px;
  color: var(--vi-text-3);
  font-size: 13px;
}

.vi-report-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 18px;
}

.vi-dialog-full { display: flex; flex-direction: column; gap: 10px; }

/* 工具 */
.spin { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 响应式 */
@media (max-width: 1200px) {
  .vi-interview { grid-template-columns: 260px 1fr 280px; }
  .vi-summary-grid { grid-template-columns: 280px 1fr; }
  .vi-report-body { grid-template-columns: 300px 1fr; }
}
@media (max-width: 980px) {
  .vi-interview { grid-template-columns: 1fr; }
  .vi-col-left, .vi-col-right { display: none; } /* 小屏隐藏左右栏，仅保留中间对话 */
  .vi-report-body { grid-template-columns: 1fr; }
  .vi-summary-grid { grid-template-columns: 1fr; }
  .vi-points-grid { grid-template-columns: 1fr; }
  .vi-tab-bar { overflow-x: auto; }
  .vi-tab { flex: 0 0 auto; padding: 8px 14px; }
  .vi-form-grid { grid-template-columns: 1fr; }
  .vi-setup-card { max-width: 100%; padding: 28px 20px; border-radius: 14px; }
}
</style>
