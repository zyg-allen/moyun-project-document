import { ref, computed, onUnmounted } from 'vue';

/**
 * 真实媒体设备检测 Composable（AI 语音面试设备准备）
 *
 * 能力：
 * - 枚举真实输入/输出音频设备（enumerateDevices，授权后返回 label）
 * - 麦克风/扬声器权限查询（Permissions API，带变更监听）
 * - 授权引导（getUserMedia 触发浏览器授权弹窗）
 * - 耳机检测（基于设备 label 的启发式识别 + 移动端降级提示）
 * - 真实麦克风电平测试（getUserMedia + AnalyserNode）
 * - 扬声器测试（生成测试音频，支持 setSinkId 路由到指定输出设备）
 *
 * 已知限制：
 * - enumerateDevices 的 label 在授权前为空（浏览器安全策略）
 * - iOS Safari 不支持 permissions.query('microphone')，降级为 unknown
 * - setSinkId 仅 Chrome/Edge 支持，其他浏览器播放到默认设备
 */

export interface MediaDeviceInfoLike {
  deviceId: string;
  kind: string;
  label: string;
}

export type PermissionState = 'granted' | 'prompt' | 'denied' | 'unknown';
export type HeadphoneState = 'detected' | 'not-detected' | 'unknown';

export function useMediaDevices() {
  const audioInputs = ref<MediaDeviceInfoLike[]>([]);
  const audioOutputs = ref<MediaDeviceInfoLike[]>([]);
  const micPermission = ref<PermissionState>('unknown');
  const speakerPermission = ref<PermissionState>('unknown');
  const supportPermissions = ref(false);
  const supportSinkId = ref(false);

  /** 测试会话状态 */
  const micTesting = ref(false);
  const micLevel = ref(0);
  const micPeakLevel = ref(0);

  let mediaStream: MediaStream | null = null;
  let audioCtx: AudioContext | null = null;
  let analyser: AnalyserNode | null = null;
  let rafId: number | null = null;
  let testAudioEl: HTMLAudioElement | null = null;

  // ==================== 设备枚举 ====================

  async function refreshDevices() {
    if (!navigator.mediaDevices?.enumerateDevices) return;
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      audioInputs.value = devices
        .filter((d) => d.kind === 'audioinput')
        .map((d) => ({ deviceId: d.deviceId, kind: d.kind, label: d.label || '麦克风（未授权时无法显示名称）' }));
      audioOutputs.value = devices
        .filter((d) => d.kind === 'audiooutput')
        .map((d) => ({ deviceId: d.deviceId, kind: d.kind, label: d.label || '扬声器（未授权时无法显示名称）' }));
    } catch (e) {
      console.warn('[MediaDevices] enumerateDevices failed', e);
    }
  }

  // ==================== 权限查询 ====================

  async function refreshPermissions() {
    if (!navigator.permissions?.query) {
      supportPermissions.value = false;
      return;
    }
    supportPermissions.value = true;
    try {
      const micStatus = await navigator.permissions.query({ name: 'microphone' as PermissionName });
      micPermission.value = micStatus.state as PermissionState;
      micStatus.onchange = () => {
        micPermission.value = micStatus.state as PermissionState;
        // 授权状态变化后重新枚举（label 从未授权的空变为真实名称）
        void refreshDevices();
      };
    } catch {
      micPermission.value = 'unknown';
    }
    try {
      const spkStatus = await navigator.permissions.query({ name: 'speaker' as PermissionName });
      speakerPermission.value = spkStatus.state as PermissionState;
      spkStatus.onchange = () => {
        speakerPermission.value = spkStatus.state as PermissionState;
      };
    } catch {
      // Chrome 之外普遍不支持 speaker 权限查询，不阻塞
      speakerPermission.value = 'unknown';
    }
  }

  // ==================== 授权引导 ====================

  /** 触发浏览器麦克风授权弹窗（真实 getUserMedia），授权成功后刷新设备列表 */
  async function requestMicPermission(): Promise<boolean> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach((t) => t.stop());
      micPermission.value = 'granted';
      await refreshDevices();
      return true;
    } catch (e: any) {
      if (e?.name === 'NotAllowedError' || e?.name === 'PermissionDeniedError') {
        micPermission.value = 'denied';
      }
      return false;
    }
  }

  // ==================== 耳机检测 ====================

  const HEADPHONE_KEYWORDS = [
    'headset', 'headphone', 'earbuds', 'earphone', 'airpods', 'beats',
    'buds', '耳机', '头戴', '耳麦', 'air ', 'freebuds', 'wh-', 'wf-',
  ];

  /**
   * 耳机启发式检测：输入或输出设备 label 命中耳机关键词即认为佩戴。
   * 未授权（label 为空）或移动端无法区分时返回 unknown。
   */
  const headphoneState = computed<HeadphoneState>(() => {
    const isMobile = /Android|iPhone|iPad|HarmonyOS/i.test(navigator.userAgent);
    const all = [...audioInputs.value, ...audioOutputs.value];
    const hasLabel = all.some((d) => d.label && !d.label.includes('未授权'));
    if (!hasLabel) return 'unknown';
    const detected = all.some((d) => {
      const label = d.label.toLowerCase();
      return HEADPHONE_KEYWORDS.some((k) => label.includes(k));
    });
    if (detected) return 'detected';
    // 移动端外放与耳机难以从 label 区分，保守返回 unknown
    if (isMobile) return 'unknown';
    return 'not-detected';
  });

  // ==================== 麦克风电平测试（真实采集） ====================

  /** 开始真实麦克风测试：采集 + 实时电平，返回是否成功启动 */
  async function startMicTest(deviceId?: string): Promise<boolean> {
    await stopMicTest();
    try {
      const constraints: MediaStreamConstraints = {
        audio: deviceId && deviceId !== 'default'
          ? { deviceId: { exact: deviceId } }
          : true,
      };
      mediaStream = await navigator.mediaDevices.getUserMedia(constraints);
      audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const source = audioCtx.createMediaStreamSource(mediaStream);
      analyser = audioCtx.createAnalyser();
      analyser.fftSize = 512;
      source.connect(analyser);
      micTesting.value = true;
      micPeakLevel.value = 0;
      const data = new Uint8Array(analyser.frequencyBinCount);
      const loop = () => {
        if (!analyser) return;
        analyser.getByteFrequencyData(data);
        const avg = data.reduce((a, b) => a + b, 0) / data.length / 255;
        micLevel.value = Math.round(avg * 100) / 100;
        if (avg > micPeakLevel.value) micPeakLevel.value = micLevel.value;
        rafId = requestAnimationFrame(loop);
      };
      loop();
      return true;
    } catch (e: any) {
      console.warn('[MediaDevices] mic test failed', e);
      if (e?.name === 'NotAllowedError') micPermission.value = 'denied';
      await stopMicTest();
      return false;
    }
  }

  async function stopMicTest() {
    if (rafId !== null) {
      cancelAnimationFrame(rafId);
      rafId = null;
    }
    micTesting.value = false;
    micLevel.value = 0;
    if (mediaStream) {
      mediaStream.getTracks().forEach((t) => t.stop());
      mediaStream = null;
    }
    if (audioCtx) {
      try {
        await audioCtx.close();
      } catch {
        /* ignore */
      }
      audioCtx = null;
      analyser = null;
    }
  }

  // ==================== 扬声器测试（真实播放 + 可选路由） ====================

  /** 生成 0.8s 的 660Hz 测试音（WAV data URI，避免依赖外部音频文件） */
  function buildTestToneDataUri(): string {
    const sampleRate = 44100;
    const duration = 0.8;
    const samples = Math.floor(sampleRate * duration);
    const buffer = new ArrayBuffer(44 + samples * 2);
    const view = new DataView(buffer);
    const writeStr = (offset: number, s: string) => {
      for (let i = 0; i < s.length; i++) view.setUint8(offset + i, s.charCodeAt(i));
    };
    writeStr(0, 'RIFF');
    view.setUint32(4, 36 + samples * 2, true);
    writeStr(8, 'WAVE');
    writeStr(12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true);
    view.setUint16(22, 1, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * 2, true);
    view.setUint16(32, 2, true);
    view.setUint16(34, 16, true);
    writeStr(36, 'data');
    view.setUint32(40, samples * 2, true);
    for (let i = 0; i < samples; i++) {
      // 淡入淡出防爆音
      const fade = Math.min(1, i / (sampleRate * 0.05), (samples - i) / (sampleRate * 0.05));
      const v = Math.sin((2 * Math.PI * 660 * i) / sampleRate) * 0.4 * fade;
      view.setInt16(44 + i * 2, v * 32767, true);
    }
    // ArrayBuffer → base64
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const chunk = 0x8000;
    for (let i = 0; i < bytes.length; i += chunk) {
      binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
    }
    return `data:audio/wav;base64,${btoa(binary)}`;
  }

  /**
   * 播放扬声器测试音。deviceId 指定时尝试 setSinkId 路由（Chrome/Edge）。
   * 返回实际播放的设备描述（用于 UI 提示），失败返回 null。
   */
  async function playSpeakerTest(deviceId?: string): Promise<string | null> {
    try {
      stopSpeakerTest();
      testAudioEl = new Audio(buildTestToneDataUri());
      supportSinkId.value = typeof (testAudioEl as any).setSinkId === 'function';
      if (deviceId && deviceId !== 'default' && supportSinkId.value) {
        await (testAudioEl as any).setSinkId(deviceId);
      }
      await testAudioEl.play();
      return deviceId && deviceId !== 'default' && supportSinkId.value
        ? audioOutputs.value.find((d) => d.deviceId === deviceId)?.label || '指定输出设备'
        : '系统默认输出设备';
    } catch (e) {
      console.warn('[MediaDevices] speaker test failed', e);
      return null;
    }
  }

  function stopSpeakerTest() {
    if (testAudioEl) {
      testAudioEl.pause();
      testAudioEl.src = '';
      testAudioEl = null;
    }
  }

  // ==================== 初始化与清理 ====================

  function onDeviceChange() {
    void refreshDevices();
  }

  function init() {
    void refreshDevices();
    void refreshPermissions();
    navigator.mediaDevices?.addEventListener?.('devicechange', onDeviceChange);
  }

  init();
  onUnmounted(() => {
    navigator.mediaDevices?.removeEventListener?.('devicechange', onDeviceChange);
    void stopMicTest();
    stopSpeakerTest();
  });

  return {
    audioInputs,
    audioOutputs,
    micPermission,
    speakerPermission,
    supportPermissions,
    supportSinkId,
    headphoneState,
    micTesting,
    micLevel,
    micPeakLevel,
    refreshDevices,
    refreshPermissions,
    requestMicPermission,
    startMicTest,
    stopMicTest,
    playSpeakerTest,
    stopSpeakerTest,
  };
}
