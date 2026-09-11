import { ref, readonly, onUnmounted } from 'vue';

/**
 * 语音合成（TTS）Composable —— V10.0 语音面试官
 *
 * <p>基于浏览器原生 Web Speech API（SpeechSynthesis），免费、离线、低延迟。
 * 特性：
 * <ul>
 *   <li>队列播报：支持长文本按句切分入队，顺序播报，避免互相打断</li>
 *   <li>暂停/恢复：调用 pause()/resume()，对应面试官"等一下"场景</li>
 *   <li>降级策略：浏览器不支持时 onUnsupported 回调，UI 层可降级为纯文本展示</li>
 *   <li>中文语音优选：优先选取 zh-CN 语音，无则用默认语音</li>
 *   <li>自动清理：组件卸载时 cancel 队列，避免内存泄漏与孤儿音频</li>
 * </ul>
 *
 * <p>已知问题：Chrome 在长时间运行后 utterance 队列会卡死（~15s 不触发 onend），
 * 通过 keepAlive 机制（每 10s 触发一次 resume）缓解。
 *
 * @author moyun
 */
export interface SpeechSynthesisOptions {
  /** 语速 0.1~10，默认 1 */
  rate?: number;
  /** 音调 0~2，默认 1 */
  pitch?: number;
  /** 音量 0~1，默认 1 */
  volume?: number;
  /** 强制使用的语音名称（如 'zh-CN-XiaoxiaoNeural'），不传则自动选中文语音 */
  voiceName?: string;
  /** 浏览器不支持时的降级回调 */
  onUnsupported?: () => void;
  /** 队列全部播报完成回调 */
  onQueueEnd?: () => void;
}

export function useSpeechSynthesis(options: SpeechSynthesisOptions = {}) {
  const {
    rate = 1,
    pitch = 1,
    volume = 1,
    voiceName,
    onUnsupported,
    onQueueEnd,
  } = options;

  /** 是否支持 Web Speech API */
  const supported = ref<boolean>(typeof window !== 'undefined' && 'speechSynthesis' in window);

  /** 是否正在播报 */
  const speaking = ref(false);
  /** 是否暂停 */
  const paused = ref(false);
  /** 当前播报文本 */
  const currentText = ref('');

  /** 播报队列（按句切分后的片段） */
  const queue: string[] = [];
  /** 选中的语音对象 */
  let selectedVoice: SpeechSynthesisVoice | null = null;
  /** keepAlive 定时器，缓解 Chrome 长时间卡死 */
  let keepAliveTimer: ReturnType<typeof setInterval> | null = null;

  /** 初始化中文语音（语音列表异步加载，需监听 voiceschanged） */
  function initVoice() {
    if (!supported.value) return;
    const voices = window.speechSynthesis.getVoices();
    if (!voices || voices.length === 0) return;
    // 优先精确匹配 voiceName
    if (voiceName) {
      const exact = voices.find((v) => v.name === voiceName);
      if (exact) {
        selectedVoice = exact;
        return;
      }
    }
    // 其次中文语音（zh-CN 优先，其次 zh-*）
    const zhVoice =
      voices.find((v) => v.lang === 'zh-CN') ||
      voices.find((v) => v.lang && v.lang.startsWith('zh'));
    selectedVoice = zhVoice || voices[0];
  }

  if (supported.value) {
    initVoice();
    // 语音列表异步加载，监听变更
    window.speechSynthesis.onvoiceschanged = () => initVoice();
  } else {
    // 不支持时触发降级
    onUnsupported?.();
  }

  /** 启动 keepAlive 定时器（Chrome 长时间播报卡死缓解） */
  function startKeepAlive() {
    if (keepAliveTimer) return;
    keepAliveTimer = setInterval(() => {
      if (supported.value && speaking.value && !paused.value) {
        // 暂停后立即恢复，触发引擎心跳
        window.speechSynthesis.pause();
        window.speechSynthesis.resume();
      }
    }, 10000);
  }

  function stopKeepAlive() {
    if (keepAliveTimer) {
      clearInterval(keepAliveTimer);
      keepAliveTimer = null;
    }
  }

  /** 将长文本按句切分（。！？.!? 换行） */
  function splitText(text: string): string[] {
    if (!text) return [];
    return text
      .split(/(?<=[。！？.!?])\s*|[\n\r]+/)
      .map((s) => s.trim())
      .filter((s) => s.length > 0);
  }

  /** 播报队列中下一段 */
  function playNext() {
    if (!supported.value) {
      onUnsupported?.();
      return;
    }
    if (queue.length === 0) {
      speaking.value = false;
      paused.value = false;
      currentText.value = '';
      stopKeepAlive();
      onQueueEnd?.();
      return;
    }
    const chunk = queue.shift()!;
    currentText.value = chunk;
    const utterance = new SpeechSynthesisUtterance(chunk);
    utterance.rate = rate;
    utterance.pitch = pitch;
    utterance.volume = volume;
    utterance.lang = selectedVoice?.lang || 'zh-CN';
    if (selectedVoice) {
      utterance.voice = selectedVoice;
    }
    utterance.onend = () => {
      // 暂停状态下不自动播下一段（等待 resume）
      if (!paused.value) {
        playNext();
      }
    };
    utterance.onerror = (e) => {
      console.warn('[TTS] utterance error', e);
      // 错误时跳过当前段，继续下一段，避免队列卡死
      if (!paused.value) {
        playNext();
      }
    };
    window.speechSynthesis.speak(utterance);
  }

  /**
   * 加入播报队列（不立即打断当前播报）
   * @param text 待播报文本
   * @param immediate 是否立即清空队列并播报（默认 false）
   */
  function speak(text: string, immediate = false) {
    if (!supported.value) {
      onUnsupported?.();
      return;
    }
    if (immediate) {
      window.speechSynthesis.cancel();
      queue.length = 0;
    }
    const chunks = splitText(text);
    queue.push(...chunks);
    if (!speaking.value) {
      speaking.value = true;
      paused.value = false;
      startKeepAlive();
      playNext();
    }
  }

  /** 暂停播报 */
  function pause() {
    if (!supported.value || !speaking.value) return;
    window.speechSynthesis.pause();
    paused.value = true;
  }

  /** 恢复播报 */
  function resume() {
    if (!supported.value || !speaking.value) return;
    window.speechSynthesis.resume();
    paused.value = false;
    // 若队列已空但 speaking 仍为 true（边界），触发下一轮
    if (queue.length > 0) {
      playNext();
    }
  }

  /** 立即停止并清空队列 */
  function cancel() {
    if (!supported.value) return;
    window.speechSynthesis.cancel();
    queue.length = 0;
    speaking.value = false;
    paused.value = false;
    currentText.value = '';
    stopKeepAlive();
  }

  /** 组件卸载时清理 */
  onUnmounted(() => {
    cancel();
    if (supported.value) {
      window.speechSynthesis.onvoiceschanged = null;
    }
  });

  return {
    supported: readonly(supported),
    speaking: readonly(speaking),
    paused: readonly(paused),
    currentText: readonly(currentText),
    speak,
    pause,
    resume,
    cancel,
  };
}
