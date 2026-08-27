import { ref, readonly, onUnmounted } from 'vue';

/**
 * 实时音量可视化 Composable —— V10.1 语音面试官 UI 增强
 *
 * <p>基于 getUserMedia + AudioContext + AnalyserNode 的频域分析，
 * 为"正在聆听"状态提供行业标准的实时音浪柱状动画。
 *
 * <p>对标产品：HireVue / 面试鸭 AI / 牛客 AI 面试的麦克风音量条。
 *
 * <p>特性：
 * <ul>
 *   <li>10 柱频段分桶：取人声主频段（约 85Hz~4kHz）分桶，跳动层次分明</li>
 *   <li>平滑手感：上升快（attack 0.6）、下降慢（release 0.12），行业通用音频表手感</li>
 *   <li>与 Web Speech API 并行：独立的 getUserMedia 流，不干扰 SpeechRecognition</li>
 *   <li>静默检测：level 汇总值可用于"用户正在说话"判断</li>
 *   <li>全量清理：stop 时释放 track / 关闭 AudioContext / 取消 RAF，组件卸载兜底</li>
 * </ul>
 *
 * <p>已知限制：
 * <ul>
 *   <li>需 HTTPS 或 localhost（getUserMedia 安全约束）</li>
 *   <li>Firefox 的 AudioContext 需用户手势后 resume，本组件 start() 均由点击触发，天然满足</li>
 * </ul>
 *
 * @author moyun
 */
export interface AudioLevelOptions {
  /** 柱状条数量，默认 10 */
  bars?: number;
  /** 频域起始 bin（ fftSize=256 时 bin 宽 ≈ 93.75Hz/22050Hz·2 ），默认 2 */
  minBin?: number;
  /** 频域结束 bin，默认 82（约 3.8kHz，覆盖人声主频段） */
  maxBin?: number;
}

export function useAudioLevel(options: AudioLevelOptions = {}) {
  const { bars = 10, minBin = 2, maxBin = 82 } = options;

  /** 各柱高度 0~1（响应式，模板直接绑定） */
  const levels = ref<number[]>(new Array(bars).fill(0));
  /** 总体活跃度 0~1（可用于说话状态判断） */
  const activity = ref(0);
  /** 是否正在采集 */
  const running = ref(false);

  let stream: MediaStream | null = null;
  let audioCtx: AudioContext | null = null;
  let analyser: AnalyserNode | null = null;
  let rafId = 0;
  /** 频域原始数据 */
  let freqData: Uint8Array | null = null;
  /** 每柱平滑后的值（非响应式，RAF 内部使用） */
  const smoothed = new Array(bars).fill(0);

  /** 上升/衰减系数（每帧） */
  const ATTACK = 0.55;
  const RELEASE = 0.12;

  function loop() {
    if (!analyser || !freqData) return;
    analyser.getByteFrequencyData(freqData);

    const usable = Math.max(1, maxBin - minBin);
    const per = Math.max(1, Math.floor(usable / bars));
    let sum = 0;
    for (let i = 0; i < bars; i++) {
      // 分桶平均，映射到 0~1
      let acc = 0;
      const from = minBin + i * per;
      const to = Math.min(maxBin, from + per);
      for (let j = from; j < to; j++) {
        acc += freqData[j] ?? 0;
      }
      const avg = acc / Math.max(1, to - from) / 255;
      // 轻微非线性拉伸，让小音量也有可见跳动
      const target = Math.min(1, Math.pow(avg, 0.75) * 1.35);
      // 平滑：上升快下降慢
      smoothed[i] = target > smoothed[i]
        ? smoothed[i] + (target - smoothed[i]) * ATTACK
        : smoothed[i] * (1 - RELEASE);
      levels.value[i] = smoothed[i];
      sum += smoothed[i];
    }
    activity.value = sum / bars;

    rafId = requestAnimationFrame(loop);
  }

  /**
   * 开始采集（幂等）。失败时静默降级——音浪是增强体验，不应阻塞识别主链路。
   * @returns 是否成功启动
   */
  async function start(): Promise<boolean> {
    if (running.value) return true;
    if (typeof navigator === 'undefined' || !navigator.mediaDevices?.getUserMedia) {
      return false;
    }
    try {
      stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      const Ctx: typeof AudioContext | undefined =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Ctx) {
        stream.getTracks().forEach((t) => t.stop());
        stream = null;
        return false;
      }
      audioCtx = new Ctx();
      // Firefox 需手势后 resume；start 均由点击触发，此处兜底
      if (audioCtx.state === 'suspended') {
        await audioCtx.resume().catch(() => undefined);
      }
      const source = audioCtx.createMediaStreamSource(stream);
      analyser = audioCtx.createAnalyser();
      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.55;
      source.connect(analyser);
      // 只分析不输出（不连 destination），避免回环啸叫
      freqData = new Uint8Array(new ArrayBuffer(analyser.frequencyBinCount));
      smoothed.fill(0);
      levels.value = new Array(bars).fill(0);
      running.value = true;
      rafId = requestAnimationFrame(loop);
      return true;
    } catch (e) {
      console.warn('[AudioLevel] start failed（音浪降级关闭）', e);
      stop();
      return false;
    }
  }

  /** 停止采集并释放全部资源（幂等） */
  function stop() {
    cancelAnimationFrame(rafId);
    rafId = 0;
    if (stream) {
      stream.getTracks().forEach((t) => t.stop());
      stream = null;
    }
    if (audioCtx) {
      audioCtx.close().catch(() => undefined);
      audioCtx = null;
    }
    analyser = null;
    freqData = null;
    smoothed.fill(0);
    levels.value = new Array(bars).fill(0);
    activity.value = 0;
    running.value = false;
  }

  onUnmounted(stop);

  return {
    levels: readonly(levels),
    activity: readonly(activity),
    running: readonly(running),
    start,
    stop,
  };
}
