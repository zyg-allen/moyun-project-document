<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useHead } from '@vueuse/head';
import {
  Loader2, Volume2, Play, Pause, Square, Mic, MicOff,
  ChevronUp, ChevronDown, KeyRound, ListChecks, Lightbulb, Send, RefreshCw,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { useSpeechSynthesis } from '@/composables/useSpeechSynthesis';
import { useSpeechRecognition } from '@/composables/useSpeechRecognition';
import { useInterviewHint } from '@/composables/useInterviewHint';
import { useToast } from '@/composables/useToast';
import type { HintLevel } from '@/api/voiceInterview';

useHead({
  title: '语音引擎验证页 - 墨云',
  meta: generateSeo({
    title: '语音引擎验证页',
    description: 'V10.0 语音面试官三引擎链路验证（TTS + ASR + HintEngine）',
  }),
});

const toast = useToast();

// ==================== 三引擎初始化 ====================
const {
  supported: ttsSupported, speaking, paused, currentText,
  speak, pause, resume, cancel,
} = useSpeechSynthesis({
  rate: 1,
  onUnsupported: () => toast.warning('当前浏览器不支持语音合成，请使用 Chrome/Edge'),
});

const {
  supported: asrSupported, listening, interimText, finalText, errorMessage,
  start: startAsr, stop: stopAsr, reset: resetAsr,
} = useSpeechRecognition({
  onUnsupported: () => toast.warning('当前浏览器不支持语音识别，请使用 Chrome/Edge'),
  onError: (err) => {
    if (err === 'not-allowed') {
      toast.error('麦克风权限被拒绝，请在浏览器设置中允许');
    }
  },
});

const {
  currentLevel, currentHint, loading: hintLoading,
  fetchHint, upgradeHint, downgradeHint,
} = useInterviewHint();

/**
 * ASR 错误码 → 用户可读文案（清单 P2）。
 *
 * <p>composable 在 errorMessage 里放的是**内部错误码**（not-allowed / server-asr-failed），
 * 浏览器 SpeechRecognition 还会给出 network / no-speech / audio-capture / aborted 等；
 * 原实现把原始码直接渲染给用户，等于没给任何可操作的信息。</p>
 *
 * <p>未知码不臆测：给通用文案，并把原始码放进 title 便于排查。</p>
 */
const ASR_ERROR_TEXT: Record<string, string> = {
  'not-allowed': '麦克风权限被拒绝，请在浏览器设置中允许后重试',
  'service-not-allowed': '浏览器拒绝了语音识别服务，请检查站点权限',
  'server-asr-failed': '服务端语音识别失败，请稍后重试',
  'network': '网络异常导致识别中断，请检查网络后重试',
  'no-speech': '没有检测到语音，请靠近麦克风再试一次',
  'audio-capture': '未检测到可用的麦克风设备',
  'aborted': '识别已中止',
  'language-not-supported': '当前语言不被识别引擎支持',
};

const asrErrorText = computed(() => {
  const raw = (errorMessage.value || '').trim();
  if (!raw) return '';
  if (ASR_ERROR_TEXT[raw]) return ASR_ERROR_TEXT[raw];
  // 已是可读中文（如"识别多次中断，请检查麦克风/网络后重试"）→ 原样展示
  if (/[\u4e00-\u9fa5]/.test(raw)) return raw;
  return '语音识别失败，请重试';
});

// ==================== 题目输入 ====================
const questionId = ref<number>(1);
const customSpeakText = ref('你好，欢迎参加本次面试。请先做一个简短的自我介绍，包括你的技术栈和项目经验。');

// ==================== TTS 控制 ====================
function handleSpeak(text: string, immediate = true) {
  if (!text.trim()) {
    toast.warning('播报内容不能为空');
    return;
  }
  speak(text, immediate);
}

function handlePauseResume() {
  if (paused.value) {
    resume();
  } else {
    pause();
  }
}

// ==================== ASR 控制 ====================
function handleToggleAsr() {
  if (listening.value) {
    stopAsr();
  } else {
    resetAsr();
    startAsr();
  }
}


// ==================== HintEngine 控制 ====================
async function handleFetchHint(level: HintLevel) {
  if (!questionId.value || questionId.value <= 0) {
    toast.warning('请输入有效的题目ID');
    return;
  }
  await fetchHint(questionId.value, level);
  if (currentHint.value?.speakText) {
    toast.success(`已获取 ${currentHint.value.title}`);
  }
}

async function handleUpgrade() {
  if (!questionId.value) return;
  await upgradeHint(questionId.value);
}

async function handleDowngrade() {
  if (!questionId.value) return;
  await downgradeHint(questionId.value);
}

/** 用 HintEngine 的 speakText 驱动 TTS 播报 */
function speakHint() {
  if (!currentHint.value?.speakText) {
    toast.warning('当前无提示内容');
    return;
  }
  handleSpeak(currentHint.value.speakText);
}

// ==================== 三引擎联动 ====================

/** 联动流程的延时器（清单 P2：持有 id 以便卸载时清理，避免页面已离开仍启动 ASR） */
let pipelineTimer: ReturnType<typeof setTimeout> | null = null;
/** 模拟面试官流程：TTS 播报题目 -> ASR 聆听 -> 获取提示 */
async function runFullPipeline() {
  // 1. 获取提示
  await fetchHint(questionId.value, 1);
  if (!currentHint.value) {
    toast.error('提示获取失败，请检查题目ID与登录状态');
    return;
  }
  // 2. TTS 播报引导语
  if (currentHint.value.speakText) {
    handleSpeak(currentHint.value.speakText);
  }
  // 3. 延迟开启 ASR（等 TTS 说完前半句）
  // 清单 P2：原先裸 setTimeout 且 id 未保存 —— 组件卸载时 composable 只 abort 当前会话，
  // 1.5s 后定时器仍会执行 resetAsr()+startAsr()（在已卸载页面上重新申请麦克风）。
  // 这里持有 id，并在卸载时清理。
  if (pipelineTimer != null) clearTimeout(pipelineTimer);
  pipelineTimer = setTimeout(() => {
    pipelineTimer = null;
    if (!listening.value) {
      resetAsr();
      startAsr();
    }
  }, 1500);
  toast.info('三引擎联动已启动：TTS 播报 → ASR 聆听');
}

onMounted(() => {
  // 不自动调用，等用户点击
});

onUnmounted(() => {
  // 清单 P2：卸载时清掉联动延时器，避免"页面已离开仍在启动 ASR"
  if (pipelineTimer != null) {
    clearTimeout(pipelineTimer);
    pipelineTimer = null;
  }
});
</script>

<template>
  <div class="voice-demo-page">
    <Breadcrumb :items="[{ label: '面试空间', path: '/interview' }, { label: '语音引擎验证' }]" />

    <div class="demo-container">
      <h1 class="page-title">
        <Lightbulb :size="24" />
        V10.0 语音引擎验证页
      </h1>
      <p class="page-desc">
        串联 TTS（语音合成）+ ASR（语音识别）+ HintEngine（提示引擎）三引擎链路。
        仅用于 V10.0 开发验证，后续将被正式语音面试页面替代。
      </p>

      <!-- 引擎支持状态 -->
      <div class="status-bar">
        <span :class="['status-chip', ttsSupported ? 'ok' : 'fail']">
          TTS: {{ ttsSupported ? '支持' : '不支持' }}
        </span>
        <span :class="['status-chip', asrSupported ? 'ok' : 'fail']">
          ASR: {{ asrSupported ? '支持' : '不支持' }}
        </span>
        <span class="status-chip info">HintEngine: 规则版</span>
      </div>

      <!-- 三引擎联动 -->
      <section class="card">
        <h2 class="card-title"><Send :size="18" /> 三引擎联动</h2>
        <p class="card-desc">一键启动：获取提示 → TTS 播报引导语 → ASR 开始聆听</p>
        <div class="row">
          <label class="field">
            <span>题目ID</span>
            <input v-model.number="questionId" type="number" min="1" class="input" />
          </label>
          <button class="btn primary" :disabled="hintLoading" @click="runFullPipeline">
            <Loader2 v-if="hintLoading" :size="16" class="spin" />
            <Play v-else :size="16" />
            启动联动
          </button>
        </div>
      </section>

      <!-- TTS 模块 -->
      <section class="card">
        <h2 class="card-title">
          <Volume2 :size="18" />
          TTS 语音合成
          <span v-if="speaking" class="badge">播报中</span>
          <span v-if="paused" class="badge warn">已暂停</span>
        </h2>
        <div class="row">
          <input v-model="customSpeakText" class="input flex-1" placeholder="输入要播报的文本" />
          <button class="btn primary" :disabled="!ttsSupported" @click="handleSpeak(customSpeakText)">
            <Play :size="16" /> 播报
          </button>
        </div>
        <div class="row">
          <button class="btn" :disabled="!speaking" @click="handlePauseResume">
            <component :is="paused ? Play : Pause" :size="16" />
            {{ paused ? '恢复' : '暂停' }}
          </button>
          <button class="btn danger" :disabled="!speaking" @click="cancel">
            <Square :size="16" /> 停止
          </button>
        </div>
        <div v-if="currentText" class="output">
          <span class="label">当前播报：</span>{{ currentText }}
        </div>
      </section>

      <!-- ASR 模块 -->
      <section class="card">
        <h2 class="card-title">
          <Mic :size="18" />
          ASR 语音识别
          <span v-if="listening" class="badge">聆听中</span>
        </h2>
        <div class="row">
          <button
            :class="['btn', listening ? 'danger' : 'primary']"
            :disabled="!asrSupported"
            @click="handleToggleAsr"
          >
            <component :is="listening ? MicOff : Mic" :size="16" />
            {{ listening ? '停止聆听' : '开始聆听' }}
          </button>
          <button class="btn" @click="resetAsr">
            <RefreshCw :size="16" /> 清空
          </button>
        </div>
        <!-- 清单 P2：展示可读文案；原始错误码保留在 title 中便于排查 -->
        <div v-if="errorMessage" class="output error" :title="'原始错误码: ' + errorMessage">{{ asrErrorText }}</div>
        <div v-if="interimText" class="output interim">
          <span class="label">实时识别：</span>{{ interimText }}
        </div>
        <div v-if="finalText" class="output">
          <span class="label">最终结果：</span>
          <pre class="final-text">{{ finalText }}</pre>
        </div>
      </section>

      <!-- HintEngine 模块 -->
      <section class="card">
        <h2 class="card-title">
          <KeyRound :size="18" />
          HintEngine 提示引擎
          <span v-if="currentHint" class="badge">L{{ currentLevel }}</span>
        </h2>
        <div class="row">
          <button class="btn" :disabled="hintLoading" @click="handleFetchHint(1)">
            <Lightbulb :size="16" /> 切入点(L1)
          </button>
          <button class="btn" :disabled="hintLoading" @click="handleFetchHint(2)">
            <ListChecks :size="16" /> 结构(L2)
          </button>
          <button class="btn" :disabled="hintLoading" @click="handleFetchHint(3)">
            <ListChecks :size="16" /> 全量(L3)
          </button>
        </div>
        <div class="row">
          <button class="btn" :disabled="!currentHint || currentLevel <= 1" @click="handleDowngrade">
            <ChevronDown :size="16" /> 降级
          </button>
          <button class="btn" :disabled="!currentHint || currentLevel >= 3" @click="handleUpgrade">
            <ChevronUp :size="16" /> 升级
          </button>
          <button class="btn primary" :disabled="!currentHint" @click="speakHint">
            <Volume2 :size="16" /> 播报提示
          </button>
        </div>

        <div v-if="currentHint" class="hint-result">
          <div class="hint-title">{{ currentHint.title }}（L{{ currentHint.level }}）</div>
          <div v-if="currentHint.keywords?.length" class="hint-section">
            <span class="label">关键词：</span>
            <span v-for="kw in currentHint.keywords" :key="kw" class="tag">{{ kw }}</span>
          </div>
          <div v-if="currentHint.examinePoints?.length" class="hint-section">
            <span class="label">考察点：</span>
            <span v-for="p in currentHint.examinePoints" :key="p" class="tag warn">{{ p }}</span>
          </div>
          <div v-if="currentHint.structureHint" class="hint-section">
            <span class="label">结构提示：</span>
            <pre class="structure">{{ currentHint.structureHint }}</pre>
          </div>
          <div v-if="currentHint.speakText" class="hint-section">
            <span class="label">话术：</span>{{ currentHint.speakText }}
          </div>
        </div>
      </section>
    </div>

    <SiteFooter />
  </div>
</template>

<style scoped>
.voice-demo-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 16px;
}
.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 22px;
  font-weight: 700;
  margin: 16px 0 4px;
}
.page-desc {
  color: var(--text-secondary, #666);
  font-size: 13px;
  margin-bottom: 16px;
}
.status-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}
.status-chip {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.status-chip.ok { background: #e6f9ee; color: #16a34a; }
.status-chip.fail { background: #fde8e8; color: #dc2626; }
.status-chip.info { background: #e8f0fe; color: #2563eb; }

.card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #eee);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px;
}
.card-desc {
  font-size: 12px;
  color: var(--text-secondary, #888);
  margin: 0 0 12px;
}
.badge {
  margin-left: auto;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: #e6f9ee;
  color: #16a34a;
}
.badge.warn { background: #fef3c7; color: #d97706; }

.row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.input {
  padding: 6px 10px;
  border: 1px solid var(--border, #ddd);
  border-radius: 6px;
  font-size: 13px;
  min-width: 80px;
}
.input.flex-1 { flex: 1; min-width: 200px; }
.btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border: 1px solid var(--border, #ddd);
  border-radius: 6px;
  background: var(--bg-card, #fff);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}
.btn:hover:not(:disabled) { background: var(--bg-hover, #f5f5f5); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn.primary:hover:not(:disabled) { background: #1d4ed8; }
.btn.danger { background: #dc2626; color: #fff; border-color: #dc2626; }
.btn.danger:hover:not(:disabled) { background: #b91c1c; }

.output {
  margin-top: 8px;
  padding: 8px 10px;
  background: var(--bg-soft, #f9fafb);
  border-radius: 6px;
  font-size: 13px;
}
.output.interim { color: var(--text-secondary, #888); font-style: italic; }
.output.error { color: #dc2626; }
.output .label { font-weight: 600; color: var(--text-primary, #333); }
.final-text {
  margin: 4px 0 0;
  white-space: pre-wrap;
  font-family: inherit;
  font-size: 13px;
}

.hint-result {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-soft, #f9fafb);
  border-radius: 8px;
}
.hint-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
}
.hint-section {
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.6;
}
.hint-section .label { font-weight: 600; }
.tag {
  display: inline-block;
  margin: 2px 4px 2px 0;
  padding: 2px 8px;
  background: #e8f0fe;
  color: #2563eb;
  border-radius: 4px;
  font-size: 12px;
}
.tag.warn { background: #fef3c7; color: #d97706; }
.structure {
  margin: 4px 0 0;
  padding: 8px;
  background: var(--bg-card, #fff);
  border-radius: 4px;
  white-space: pre-wrap;
  font-size: 12px;
  font-family: inherit;
}

.spin { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
