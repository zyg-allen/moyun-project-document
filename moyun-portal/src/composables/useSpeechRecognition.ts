import { ref, readonly, onUnmounted } from 'vue';

/**
 * Web Speech API 类型声明（TS DOM lib 未内置）
 * 兼容标准 SpeechRecognition 与 webkit 前缀
 */
interface SpeechRecognitionAlternative {
  transcript: string;
  confidence: number;
}
interface SpeechRecognitionResult {
  readonly length: number;
  item(index: number): SpeechRecognitionAlternative;
  [index: number]: SpeechRecognitionAlternative;
  isFinal: boolean;
}
interface SpeechRecognitionResultList {
  readonly length: number;
  item(index: number): SpeechRecognitionResult;
  [index: number]: SpeechRecognitionResult;
}
interface SpeechRecognitionEvent extends Event {
  readonly resultIndex: number;
  readonly results: SpeechRecognitionResultList;
}
interface SpeechRecognitionErrorEvent extends Event {
  readonly error: string;
  readonly message: string;
}
interface SpeechRecognitionLike extends EventTarget {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives: number;
  start(): void;
  stop(): void;
  abort(): void;
  onresult: ((ev: SpeechRecognitionEvent) => void) | null;
  onerror: ((ev: SpeechRecognitionErrorEvent) => void) | null;
  onend: (() => void) | null;
  onstart: (() => void) | null;
}
type SpeechRecognitionCtor = new () => SpeechRecognitionLike;

/**
 * 语音识别（ASR）Composable —— V10.0 语音面试官
 *
 * <p>基于浏览器原生 Web Speech API（SpeechRecognition），免费、离线、低延迟。
 * 特性：
 * <ul>
 *   <li>实时中间结果（interim）：边说边显示，面试者可见识别进度</li>
 *   <li>最终结果累积（final）：多轮识别结果自动拼接，支持长答案</li>
 *   <li>55s 续期：Chrome 在 ~60s 后自动停止，55s 主动重启续期，无缝衔接</li>
 *   <li>自动重连：异常中断（网络/麦克风）后自动重启，最多 3 次</li>
 *   <li>降级回调：浏览器不支持时 onUnsupported，UI 层降级为文本输入</li>
 * </ul>
 *
 * <p>已知限制：
 * <ul>
 *   <li>仅 Chrome/Edge 支持 webkitSpeechRecognition，Safari/Firefox 部分支持</li>
 *   <li>需 HTTPS 或 localhost 才能获取麦克风权限</li>
 *   <li>识别结果为本地猜测，高精度场景需走服务端 ASR（paraformer 等）</li>
 * </ul>
 *
 * @author moyun
 */
export interface SpeechRecognitionOptions {
  /** 识别语言，默认 zh-CN */
  lang?: string;
  /** 是否连续识别（true=持续，false=单次），默认 true */
  continuous?: boolean;
  /** 是否返回中间结果，默认 true */
  interimResults?: boolean;
  /** 最大重连次数，默认 3 */
  maxRetries?: number;
  /** 续期间隔（ms），默认 55000（55s，早于 Chrome 60s 停止） */
  renewInterval?: number;
  /** 浏览器不支持时的降级回调 */
  onUnsupported?: () => void;
  /** 最终结果变更回调（每次新增 final 片段触发） */
  onFinalChange?: (finalText: string) => void;
  /** 错误回调 */
  onError?: (error: string) => void;
}

export function useSpeechRecognition(options: SpeechRecognitionOptions = {}) {
  const {
    lang = 'zh-CN',
    continuous = true,
    interimResults = true,
    maxRetries = 3,
    renewInterval = 55000,
    onUnsupported,
    onFinalChange,
    onError,
  } = options;

  /** 获取 SpeechRecognition 构造函数（兼容 webkit 前缀） */
  function getRecognitionCtor(): SpeechRecognitionCtor | null {
    if (typeof window === 'undefined') return null;
    const w = window as any;
    return (w.SpeechRecognition || w.webkitSpeechRecognition) as SpeechRecognitionCtor | null;
  }

  const Ctor = getRecognitionCtor();
  const supported = ref<boolean>(!!Ctor);

  if (!supported.value) {
    onUnsupported?.();
  }

  /** 是否正在监听 */
  const listening = ref(false);
  /** 中间识别结果（实时变化） */
  const interimText = ref('');
  /** 最终识别结果（累积） */
  const finalText = ref('');
  /** 错误信息 */
  const errorMessage = ref('');

  let recognition: SpeechRecognitionLike | null = null;
  /** 续期定时器 */
  let renewTimer: ReturnType<typeof setTimeout> | null = null;
  /** 当前重连次数 */
  let retryCount = 0;
  /** 用户是否主动停止（区分主动停止与异常中断） */
  let userStopped = false;

  /** 创建识别实例 */
  function createRecognition(): SpeechRecognitionLike | null {
    if (!Ctor) return null;
    const rec = new Ctor();
    rec.lang = lang;
    rec.continuous = continuous;
    rec.interimResults = interimResults;
    rec.maxAlternatives = 1;

    rec.onstart = () => {
      listening.value = true;
      errorMessage.value = '';
    };

    rec.onresult = (event: SpeechRecognitionEvent) => {
      let interim = '';
      let finalChunk = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const result = event.results[i];
        const transcript = result[0]?.transcript || '';
        if (result.isFinal) {
          finalChunk += transcript;
        } else {
          interim += transcript;
        }
      }
      if (interim) {
        interimText.value = interim;
      }
      if (finalChunk) {
        // 累积最终结果，以换行分隔多轮
        finalText.value = finalText.value
          ? finalText.value + '\n' + finalChunk
          : finalChunk;
        interimText.value = '';
        onFinalChange?.(finalText.value);
      }
    };

    rec.onerror = (event: SpeechRecognitionErrorEvent) => {
      console.warn('[ASR] recognition error', event.error, event.message);
      errorMessage.value = event.error;
      onError?.(event.error);
      // no-speech / aborted 不重连，其余按需重连
      if (event.error === 'not-allowed' || event.error === 'service-not-allowed') {
        // 麦克风权限拒绝，不重连
        userStopped = true;
      }
    };

    rec.onend = () => {
      listening.value = false;
      clearRenewTimer();
      // 异常中断且未主动停止且未超重连上限：自动重连
      if (!userStopped && retryCount < maxRetries) {
        retryCount++;
        console.info(`[ASR] 自动重连第 ${retryCount} 次`);
        // 延迟 200ms 重连，避免死循环
        setTimeout(() => {
          if (!userStopped) {
            startInternal();
          }
        }, 200);
      } else if (!userStopped && retryCount >= maxRetries) {
        console.warn('[ASR] 超过最大重连次数，停止重连');
        errorMessage.value = '识别多次中断，请检查麦克风/网络后重试';
      }
    };

    return rec;
  }

  /** 启动续期定时器（55s 主动重启，规避 Chrome 60s 停止） */
  function startRenewTimer() {
    clearRenewTimer();
    renewTimer = setTimeout(() => {
      // 主动 stop 触发 onend，onend 中会因 userStopped=false 自动重连
      if (recognition && !userStopped) {
        try {
          recognition.stop();
        } catch (e) {
          console.warn('[ASR] renew stop error', e);
        }
      }
    }, renewInterval);
  }

  function clearRenewTimer() {
    if (renewTimer) {
      clearTimeout(renewTimer);
      renewTimer = null;
    }
  }

  /** 内部启动（不重置 retryCount） */
  function startInternal() {
    if (!Ctor) {
      onUnsupported?.();
      return;
    }
    try {
      recognition = createRecognition();
      if (recognition) {
        recognition.start();
        startRenewTimer();
      }
    } catch (e) {
      console.warn('[ASR] start error', e);
      // 重复 start 会抛 InvalidStateError，忽略
    }
  }

  /** 开始监听 */
  function start() {
    if (!supported.value) {
      onUnsupported?.();
      return;
    }
    if (listening.value) return;
    userStopped = false;
    retryCount = 0;
    interimText.value = '';
    // finalText 不清空，支持追加模式；如需清空请调用 reset()
    startInternal();
  }

  /** 主动停止监听 */
  function stop() {
    userStopped = true;
    clearRenewTimer();
    if (recognition) {
      try {
        recognition.stop();
      } catch (e) {
        console.warn('[ASR] stop error', e);
      }
    }
    listening.value = false;
  }

  /** 立即中止（不等 onend） */
  function abort() {
    userStopped = true;
    clearRenewTimer();
    if (recognition) {
      try {
        recognition.abort();
      } catch (e) {
        console.warn('[ASR] abort error', e);
      }
    }
    listening.value = false;
    interimText.value = '';
  }

  /** 重置累积文本 */
  function reset() {
    finalText.value = '';
    interimText.value = '';
    errorMessage.value = '';
  }

  /** 组件卸载时清理 */
  onUnmounted(() => {
    abort();
  });

  return {
    supported: readonly(supported),
    listening: readonly(listening),
    interimText: readonly(interimText),
    finalText: readonly(finalText),
    errorMessage: readonly(errorMessage),
    start,
    stop,
    abort,
    reset,
  };
}
