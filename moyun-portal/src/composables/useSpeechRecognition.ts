import { ref, readonly, onUnmounted } from 'vue';
import { httpPost, getToken } from '@/api/client';

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

/** 服务端引擎记忆标记：Web Speech 曾因国内网络失败，下次直接走服务端引擎，跳过无效等待 */
const SERVER_MODE_KEY = 'moyun_asr_server_mode';

/**
 * AudioWorklet 处理器源码（字符串 → Blob URL 加载，免去构建配置）：
 * 采集 Float32（原生采样率）→ 线性插值降采样 16kHz → Int16 PCM，
 * 累计约 100ms（1600 样本）一批 postMessage，控制消息频率。
 */
const PCM_WORKLET_SRC = `
class MoyunPcm16k extends AudioWorkletProcessor {
  constructor() {
    super();
    this._ratio = sampleRate / 16000;
    this._carry = new Float32Array(0);
    this._batch = new Int16Array(0);
  }
  process(inputs) {
    const ch = inputs[0] && inputs[0][0];
    if (!ch) return true;
    const merged = new Float32Array(this._carry.length + ch.length);
    merged.set(this._carry, 0);
    merged.set(ch, this._carry.length);
    const n = Math.floor((merged.length - 1) / this._ratio);
    if (n > 0) {
      const out = new Int16Array(n);
      for (let i = 0; i < n; i++) {
        const p = i * this._ratio;
        const i0 = Math.floor(p);
        const i1 = Math.min(i0 + 1, merged.length - 1);
        const t = p - i0;
        const v = merged[i0] * (1 - t) + merged[i1] * t;
        out[i] = v < 0 ? v * 0x8000 : v * 0x7fff;
      }
      const consumed = Math.min(merged.length, Math.ceil(n * this._ratio) + 1);
      this._carry = merged.slice(consumed);
      const newBatch = new Int16Array(this._batch.length + out.length);
      newBatch.set(this._batch, 0);
      newBatch.set(out, this._batch.length);
      this._batch = newBatch;
      if (this._batch.length >= 1600) {
        this.port.postMessage(this._batch.buffer, [this._batch.buffer]);
        this._batch = new Int16Array(0);
      }
    } else {
      this._carry = merged;
    }
    return true;
  }
}
registerProcessor('moyun-pcm-16k', MoyunPcm16k);
`;

/** Worklet 模块 Blob URL（模块级缓存，重复创建 AudioContext 复用） */
let workletModuleUrl: string | null = null;
function getWorkletModuleUrl(): string {
  if (!workletModuleUrl) {
    workletModuleUrl = URL.createObjectURL(
      new Blob([PCM_WORKLET_SRC], { type: 'application/javascript' }),
    );
  }
  return workletModuleUrl;
}

/** 流式中继 WS 地址：绝对地址直连后端（http→ws）；相对地址（'/api'）按当前页面协议推导同源反代，兼容 https/wss */
function buildStreamWsUrl(): string {
  if (typeof window === 'undefined') return '';
  const base = (import.meta.env.VITE_API_BASE_URL as string | undefined) || '';
  if (/^https?:\/\//i.test(base)) {
    return `${base.replace(/^http/i, 'ws').replace(/\/+$/, '')}/ws-asr`;
  }
  const { protocol, host } = window.location;
  const prefix = (base || '/api').replace(/\/+$/, '');
  return `${protocol === 'https:' ? 'wss:' : 'ws:'}//${host}${prefix}/ws-asr`;
}

/**
 * 语音识别（ASR）Composable —— V10.1 语音面试官
 *
 * <p>三引擎设计（按优先级）：
 * <ul>
 *   <li><b>引擎一：Web Speech API</b>。实时中间结果、55s 续期、自动重连。仅 Chrome/Edge
 *       支持 webkitSpeechRecognition，且国内网络常因无法访问 Google 识别服务报 network 错误。</li>
 *   <li><b>引擎二：流式服务端 ASR（serverMode 首选）</b>。AudioWorklet 采集 PCM 16kHz →
 *       WebSocket 实时上传后端中继（fun-asr-realtime）→ 增量返回中间/定稿文本，
 *       体验与 Web Speech 一致（边说边出字）。Web Speech 失败后自动切换，并记忆到
 *       localStorage，下次跳过无效等待直接走本引擎。</li>
 *   <li><b>引擎三：批式 MediaRecorder 兜底</b>。流式 WS 不可用（连接失败/浏览器不支持
 *       AudioWorklet）时：本地录音 → 前端转码 WAV 16kHz → 上传 /portal/interview/voice/asr
 *       → 整段转写。</li>
 * </ul>
 *
 * <p>对外接口三引擎一致：start/stop/abort/reset + listening/interimText/finalText。
 * 批式模式无实时中间结果；transcribing 标记转写进行中，提交答案前应 await
 * whenTranscriptionDone() 确保转写文本已并入输入栏。</p>
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
  /** 批式服务端 ASR 接口地址 */
  serverAsrUrl?: string;
  /** 浏览器不支持时的降级回调（三引擎均不可用） */
  onUnsupported?: () => void;
  /** 最终结果变更回调（每次新增 final 片段触发） */
  onFinalChange?: (finalText: string) => void;
  /** 错误回调 */
  onError?: (error: string) => void;
}

/** 录音 blob 转码：任意浏览器音频 → WAV 16kHz 单声道（16-bit PCM），保证服务端 ASR 格式兼容 */
async function blobToWav16kMono(blob: Blob): Promise<Blob> {
  const arrayBuffer = await blob.arrayBuffer();
  const AudioCtx = (window.AudioContext || (window as any).webkitAudioContext) as
    | typeof AudioContext
    | undefined;
  if (!AudioCtx) throw new Error('当前浏览器不支持音频解码');
  const audioCtx = new AudioCtx();
  let audioBuffer: AudioBuffer;
  try {
    audioBuffer = await audioCtx.decodeAudioData(arrayBuffer.slice(0));
  } finally {
    void audioCtx.close();
  }

  // 下混单声道
  const channels = audioBuffer.numberOfChannels;
  const srcLength = audioBuffer.length;
  const mono = new Float32Array(srcLength);
  for (let ch = 0; ch < channels; ch++) {
    const data = audioBuffer.getChannelData(ch);
    for (let i = 0; i < srcLength; i++) mono[i] += data[i] / channels;
  }

  // 线性插值降采样到 16kHz
  const targetRate = 16000;
  let out = mono;
  if (audioBuffer.sampleRate !== targetRate) {
    const ratio = audioBuffer.sampleRate / targetRate;
    const newLength = Math.max(1, Math.round(srcLength / ratio));
    out = new Float32Array(newLength);
    for (let i = 0; i < newLength; i++) {
      const pos = i * ratio;
      const i0 = Math.floor(pos);
      const i1 = Math.min(i0 + 1, srcLength - 1);
      const t = pos - i0;
      out[i] = mono[i0] * (1 - t) + mono[i1] * t;
    }
  }

  // 16-bit PCM WAV 编码
  const bytesPerSample = 2;
  const dataSize = out.length * bytesPerSample;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);
  const writeString = (offset: number, s: string) => {
    for (let i = 0; i < s.length; i++) view.setUint8(offset + i, s.charCodeAt(i));
  };
  writeString(0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeString(8, 'WAVE');
  writeString(12, 'fmt ');
  view.setUint32(16, 16, true);          // fmt chunk size
  view.setUint16(20, 1, true);           // PCM
  view.setUint16(22, 1, true);           // mono
  view.setUint32(24, targetRate, true);  // sample rate
  view.setUint32(28, targetRate * bytesPerSample, true); // byte rate
  view.setUint16(32, bytesPerSample, true);              // block align
  view.setUint16(34, 16, true);          // bits per sample
  writeString(36, 'data');
  view.setUint32(40, dataSize, true);
  let offset = 44;
  for (let i = 0; i < out.length; i++, offset += 2) {
    const s = Math.max(-1, Math.min(1, out[i]));
    view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7fff, true);
  }
  return new Blob([buffer], { type: 'audio/wav' });
}

export function useSpeechRecognition(options: SpeechRecognitionOptions = {}) {
  const {
    lang = 'zh-CN',
    continuous = true,
    interimResults = true,
    maxRetries = 3,
    renewInterval = 55000,
    serverAsrUrl = '/portal/interview/voice/asr',
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

  /** MediaRecorder 引擎可用性 */
  function canRecord(): boolean {
    return (
      typeof navigator !== 'undefined' &&
      !!navigator.mediaDevices?.getUserMedia &&
      typeof MediaRecorder !== 'undefined'
    );
  }

  /** 流式引擎可用性：需 AudioWorklet + getUserMedia（WebSocket 连不上时运行期再降级批式） */
  function canStream(): boolean {
    return (
      typeof window !== 'undefined' &&
      typeof AudioWorkletNode !== 'undefined' &&
      typeof WebSocket !== 'undefined' &&
      canRecord()
    );
  }

  const Ctor = getRecognitionCtor();
  const webSpeechSupported = !!Ctor;
  /** Web Speech 曾失败的记忆（跳过下次无效等待） */
  const rememberedServerMode =
    typeof localStorage !== 'undefined' && localStorage.getItem(SERVER_MODE_KEY) === '1';
  /** 任一引擎可用即视为支持 */
  const supported = ref<boolean>(webSpeechSupported || canRecord());

  if (!supported.value) {
    onUnsupported?.();
  }

  /** 是否正在监听 */
  const listening = ref(false);
  /** 中间识别结果（实时变化；Web Speech 与流式引擎均有值） */
  const interimText = ref('');
  /** 最终识别结果（累积） */
  const finalText = ref('');
  /** 错误信息 */
  const errorMessage = ref('');
  /** 是否运行在服务端 ASR 模式（流式优先，批式兜底） */
  const serverMode = ref(!webSpeechSupported || rememberedServerMode);
  /** 当前服务端模式实际引擎：true=流式（边说边出字），false=批式（停止后整段转写） */
  const streamMode = ref(false);
  /** 上传转写进行中（批式模式） */
  const transcribing = ref(false);

  let recognition: SpeechRecognitionLike | null = null;
  /** 续期定时器 */
  let renewTimer: ReturnType<typeof setTimeout> | null = null;
  /** 当前重连次数 */
  let retryCount = 0;
  /** 用户是否主动停止（区分主动停止与异常中断） */
  let userStopped = false;

  // ==================== 引擎二：流式服务端 ASR（WebSocket + AudioWorklet） ====================

  let streamWs: WebSocket | null = null;
  let streamCtx: AudioContext | null = null;
  let streamNode: AudioWorkletNode | null = null;
  let streamSource: MediaStreamAudioSourceNode | null = null;
  let streamMic: MediaStream | null = null;
  /** 流式会话进行中（从 ready 到 finished/error），whenTranscriptionDone 等待依据 */
  let streamActive = false;
  /** 后端 ready 已收到（用于区分握手阶段断开与会中异常断开） */
  let streamReadyReceived = false;
  /** WS ready 之前失败 → 降级批式标记 */
  let streamFallbackToBatch = false;

  /** 释放流式引擎的麦克风与音频图（不动 WS） */
  function teardownStreamAudio() {
    try {
      streamNode?.disconnect();
      streamSource?.disconnect();
      void streamCtx?.close();
    } catch (e) {
      console.warn('[ASR] stream audio teardown error', e);
    }
    streamNode = null;
    streamSource = null;
    streamCtx = null;
    streamMic?.getTracks().forEach((t) => t.stop());
    streamMic = null;
  }

  /** 关闭流式 WS 并终结会话 */
  function closeStreamSession() {
    if (streamWs) {
      try {
        streamWs.close();
      } catch (e) {
        console.warn('[ASR] stream ws close error', e);
      }
      streamWs = null;
    }
    streamActive = false;
  }

  /** 结束本次流式识别（stop 调用）：通知后端 finish，等待剩余结果回吐 */
  function requestStreamFinish() {
    if (streamWs && streamWs.readyState === WebSocket.OPEN) {
      try {
        streamWs.send(JSON.stringify({ action: 'stop' }));
      } catch (e) {
        console.warn('[ASR] stream stop send error', e);
      }
    }
    teardownStreamAudio();
    // 保底：10s 未收到 finished/error 则强制收尾
    setTimeout(() => {
      if (streamActive) {
        console.warn('[ASR] 等待流式收尾超时，强制关闭');
        closeStreamSession();
      }
    }, 10_000);
  }

  /** 启动流式引擎。WS 连接/握手失败时自动降级批式引擎。 */
  async function startStreaming(): Promise<void> {
    const token = getToken();
    if (!token || !canStream()) {
      streamMode.value = false;
      void startRecorder();
      return;
    }

    let ws: WebSocket;
    try {
      ws = new WebSocket(`${buildStreamWsUrl()}?token=${encodeURIComponent(token)}`);
      ws.binaryType = 'arraybuffer';
    } catch (e) {
      console.warn('[ASR] 流式 WS 创建失败，降级批式', e);
      streamMode.value = false;
      void startRecorder();
      return;
    }
    streamWs = ws;
    streamMode.value = true;
    streamActive = true;
    streamReadyReceived = false;
    streamFallbackToBatch = false;

    // 连接/就绪超时兜底：5s 内未 ready 则降级批式
    const connectTimer = setTimeout(() => {
      if (streamActive && !streamReadyReceived) {
        console.warn('[ASR] 流式连接超时，降级批式');
        streamFallbackToBatch = true;
        closeStreamSession();
        streamMode.value = false;
        void startRecorder();
      }
    }, 5_000);

    ws.onmessage = (ev: MessageEvent) => {
      if (typeof ev.data !== 'string') return;
      let msg: any;
      try {
        msg = JSON.parse(ev.data);
      } catch {
        return;
      }
      switch (msg.type) {
        case 'ready':
          streamReadyReceived = true;
          void openStreamMic().then((ok) => {
            clearTimeout(connectTimer);
            if (ok) {
              listening.value = true;
              errorMessage.value = '';
            } else {
              // 麦克风失败：结束流式会话并报错
              onError?.('not-allowed');
              errorMessage.value = 'not-allowed';
              closeStreamSession();
            }
          });
          break;
        case 'interim':
          interimText.value = String(msg.text || '');
          break;
        case 'final': {
          const text = String(msg.text || '').trim();
          if (text) {
            finalText.value = finalText.value ? finalText.value + '\n' + text : text;
            interimText.value = '';
            onFinalChange?.(finalText.value);
          }
          break;
        }
        case 'finished':
          clearTimeout(connectTimer);
          teardownStreamAudio();
          interimText.value = '';
          closeStreamSession();
          listening.value = false;
          break;
        case 'error':
          clearTimeout(connectTimer);
          console.warn('[ASR] 流式识别失败:', msg.message);
          errorMessage.value = 'server-asr-failed';
          onError?.('server-asr-failed');
          teardownStreamAudio();
          interimText.value = '';
          closeStreamSession();
          listening.value = false;
          break;
      }
    };

    ws.onclose = () => {
      clearTimeout(connectTimer);
      // ready 前断开（握手失败/后端未部署）：降级批式
      if (streamActive && !streamReadyReceived && !streamFallbackToBatch) {
        console.warn('[ASR] 流式 WS 握手阶段断开，降级批式');
        streamFallbackToBatch = true;
        closeStreamSession();
        streamMode.value = false;
        void startRecorder();
        return;
      }
      if (streamActive) {
        // 会中异常断开：释放麦克风并收尾（保留已收到的 final 文本）
        teardownStreamAudio();
        closeStreamSession();
        listening.value = false;
      }
    };

    ws.onerror = () => {
      // 具体处理交给 onclose（onerror 后必触发 onclose）
    };
  }

  /** 打开麦克风并搭建 PCM 采集音频图，返回是否成功 */
  async function openStreamMic(): Promise<boolean> {
    try {
      streamMic = await navigator.mediaDevices.getUserMedia({
        audio: { echoCancellation: true, noiseSuppression: true },
      });
      streamCtx = new AudioContext();
      await streamCtx.audioWorklet.addModule(getWorkletModuleUrl());
      streamNode = new AudioWorkletNode(streamCtx, 'moyun-pcm-16k');
      streamNode.port.onmessage = (e: MessageEvent) => {
        const ws = streamWs;
        if (ws && ws.readyState === WebSocket.OPEN && e.data instanceof ArrayBuffer) {
          try {
            ws.send(e.data);
          } catch (err) {
            console.warn('[ASR] 音频帧发送失败', err);
          }
        }
      };
      streamSource = streamCtx.createMediaStreamSource(streamMic);
      streamSource.connect(streamNode);
      // 注意：不连接 destination，避免回放啸叫
      if (streamCtx.state === 'suspended') {
        await streamCtx.resume();
      }
      return true;
    } catch (err) {
      console.warn('[ASR] 流式麦克风启动失败', err);
      teardownStreamAudio();
      return false;
    }
  }

  // ==================== 引擎三：批式 MediaRecorder + 服务端 ASR ====================

  let mediaRecorder: MediaRecorder | null = null;
  let audioChunks: Blob[] = [];
  /** stop 已请求、等待 onstop（用于 whenTranscriptionDone 等待判定） */
  let recorderStopping = false;
  /** 录音处于活跃状态 */
  let recorderActive = false;
  /** 丢弃本次录音（abort 场景，如组件卸载） */
  let discardRecording = false;
  /** 进行中的上传转写 Promise */
  let pendingUpload: Promise<void> | null = null;

  function pickMimeType(): string | undefined {
    const candidates = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4'];
    for (const mime of candidates) {
      if (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(mime)) {
        return mime;
      }
    }
    return undefined;
  }

  async function startRecorder() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mime = pickMimeType();
      const rec = mime ? new MediaRecorder(stream, { mimeType: mime }) : new MediaRecorder(stream);
      audioChunks = [];
      discardRecording = false;
      rec.ondataavailable = (e: BlobEvent) => {
        if (e.data && e.data.size > 0) audioChunks.push(e.data);
      };
      rec.onstop = () => {
        recorderActive = false;
        recorderStopping = false;
        mediaRecorder = null;
        stream.getTracks().forEach((t) => t.stop());
        listening.value = false;
        if (!discardRecording && audioChunks.length > 0) {
          pendingUpload = uploadTranscription().finally(() => {
            pendingUpload = null;
          });
        }
      };
      mediaRecorder = rec;
      rec.start();
      recorderActive = true;
      listening.value = true;
      errorMessage.value = '';
    } catch (err) {
      console.warn('[ASR] MediaRecorder start error', err);
      listening.value = false;
      errorMessage.value = 'not-allowed';
      onError?.('not-allowed');
    }
  }

  async function uploadTranscription() {
    const raw = new Blob(audioChunks, { type: audioChunks[0]?.type || 'audio/webm' });
    audioChunks = [];
    if (raw.size === 0) return;
    transcribing.value = true;
    try {
      const wav = await blobToWav16kMono(raw);
      const formData = new FormData();
      formData.append('audio', wav, 'recording.wav');
      const { data } = await httpPost<{ transcript: string }>(serverAsrUrl, formData);
      const text = (data?.transcript || '').trim();
      if (text) {
        finalText.value = finalText.value ? finalText.value + '\n' + text : text;
        onFinalChange?.(finalText.value);
      }
    } catch (e) {
      console.warn('[ASR] 服务端转写失败', e);
      errorMessage.value = 'server-asr-failed';
      onError?.('server-asr-failed');
    } finally {
      transcribing.value = false;
    }
  }

  /**
   * 等待识别与转写完成（提交答案前调用，确保语音文本已并入输入栏）。
   * Web Speech 引擎下立即返回；流式/批式模式下等待各自收尾。
   */
  async function whenTranscriptionDone(): Promise<void> {
    const deadline = Date.now() + 30_000;
    while (Date.now() < deadline) {
      const batchIdle = !recorderActive && !recorderStopping && !pendingUpload;
      const streamIdle = !streamActive;
      if (batchIdle && streamIdle) return;
      await new Promise((r) => setTimeout(r, 100));
    }
    console.warn('[ASR] 等待转写超时');
  }

  // ==================== 引擎一：Web Speech API ====================

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
      if (event.error === 'not-allowed' || event.error === 'service-not-allowed') {
        // 麦克风权限拒绝：不重连；service-not-allowed（国内无法访问 Google 服务）：切服务端引擎
        userStopped = true;
        if (event.error === 'service-not-allowed' && (canStream() || canRecord())) {
          switchToServerMode();
        }
      } else if (event.error === 'network' && (canStream() || canRecord())) {
        // 国内 Chrome 无法访问 Google 识别服务：切换服务端流式引擎
        switchToServerMode();
      }
    };

    rec.onend = () => {
      listening.value = false;
      clearRenewTimer();
      if (serverMode.value) {
        // 已切换服务端模式：Web Speech 引擎退出，无缝接续服务端引擎
        if (!userStopped) {
          void startServerEngine();
        }
        return;
      }
      // 异常中断且未主动停止且未超重连上限：自动重连
      if (!userStopped && retryCount < maxRetries) {
        retryCount++;
        console.info(`[ASR] 自动重连第 ${retryCount} 次`);
        // 延迟 200ms 重连，避免死循环
        setTimeout(() => {
          if (!userStopped && !serverMode.value) {
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

  /** 切换到服务端引擎（Web Speech 不可用/网络失败时调用），并持久化记忆 */
  function switchToServerMode() {
    if (serverMode.value) return;
    console.info('[ASR] Web Speech 不可用，切换服务端引擎（已记忆，下次跳过 Web Speech）');
    serverMode.value = true;
    try {
      localStorage.setItem(SERVER_MODE_KEY, '1');
    } catch {
      // localStorage 不可用（隐私模式等）：仅本次会话生效
    }
    clearRenewTimer();
    if (recognition) {
      try {
        recognition.stop();
      } catch (e) {
        console.warn('[ASR] switch stop error', e);
      }
    }
    // onend 中会因 serverMode=true 自动接续 startServerEngine()
  }

  /** 服务端引擎入口：流式优先，WS 不可用自动降级批式（startStreaming 内部处理） */
  function startServerEngine() {
    if (canStream()) {
      void startStreaming();
    } else {
      streamMode.value = false;
      void startRecorder();
    }
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
      if (canRecord() || canStream()) {
        serverMode.value = true;
        startServerEngine();
      } else {
        onUnsupported?.();
      }
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
    if (serverMode.value) {
      startServerEngine();
    } else {
      startInternal();
    }
  }

  /** 主动停止监听（流式：通知后端收尾；批式：触发录音上传转写） */
  function stop() {
    userStopped = true;
    clearRenewTimer();
    if (streamActive) {
      requestStreamFinish();
    } else if (serverMode.value && mediaRecorder && mediaRecorder.state !== 'inactive') {
      recorderStopping = true;
      try {
        mediaRecorder.stop();
      } catch (e) {
        console.warn('[ASR] recorder stop error', e);
      }
    } else if (recognition) {
      try {
        recognition.stop();
      } catch (e) {
        console.warn('[ASR] stop error', e);
      }
    }
    listening.value = false;
  }

  /** 立即中止（丢弃未完成的识别/录音转写） */
  function abort() {
    userStopped = true;
    clearRenewTimer();
    if (streamActive) {
      try {
        streamWs?.send(JSON.stringify({ action: 'abort' }));
      } catch (e) {
        console.warn('[ASR] stream abort send error', e);
      }
      teardownStreamAudio();
      closeStreamSession();
    } else if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      discardRecording = true;
      try {
        mediaRecorder.stop();
      } catch (e) {
        console.warn('[ASR] recorder abort error', e);
      }
    } else if (recognition) {
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
    /** 服务端 ASR 模式（Web Speech 不可用/网络失败时激活） */
    serverMode: readonly(serverMode),
    /** 服务端模式实际引擎：true=流式（边说边出字），false=批式（停止后整段转写） */
    streamMode: readonly(streamMode),
    /** 上传转写进行中（仅批式模式） */
    transcribing: readonly(transcribing),
    /** 等待识别与转写完成（提交答案前调用） */
    whenTranscriptionDone,
    start,
    stop,
    abort,
    reset,
  };
}
