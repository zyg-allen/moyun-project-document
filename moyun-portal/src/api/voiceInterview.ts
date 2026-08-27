import { httpGet, httpPost } from './client';
import { getToken } from './client';

/**
 * 语音面试官 API（V10.0 + V10.1）
 * 后端：PortalVoiceInterviewController，路径 /portal/interview/voice
 */

// ==================== V10.0 HintEngine（保留） ====================

/** 提示级别 1~3 */
export type HintLevel = 1 | 2 | 3;

/** 分级提示返回对象（对齐后端 HintVO） */
export interface HintVO {
  level: number;
  title: string;
  keywords: string[];
  structureHint?: string;
  examinePoints?: string[];
  speakText?: string;
}

export const getInterviewHint = (questionId: number | string, level: HintLevel = 1) => {
  return httpGet<HintVO>('/portal/interview/voice/hint', { questionId, level });
};

export const getInterviewKeywords = (questionId: number | string) => {
  return httpGet<string[]>('/portal/interview/voice/keywords', { questionId });
};

// ==================== V10.1 语音面试官 MVP ====================

/** 面试官风格 */
export type VoiceStyle = 'professional' | 'friendly' | 'strict';

/** 难度 */
export type VoiceDifficulty = 'easy' | 'medium' | 'hard';

/** 开始面试请求配置 */
export interface VoiceStartConfig {
  position?: string;
  scene?: string;
  resumeId?: number;
  style?: VoiceStyle;
  difficulty?: VoiceDifficulty;
  personalized?: boolean;
  hintsEnabled?: boolean;
  stuckThreshold?: number;
}

/** 单条问答 VO */
export interface VoiceInterviewQaVO {
  id: number;
  interviewId: number;
  questionId?: number;
  questionIdx: number;
  parentQaId?: number;
  question: string;
  userAnswer?: string;
  transcriptionEdited?: number;
  aiFeedback?: string;
  speakText?: string;
  score?: number;
  ruleDimensionsJson?: string;
  hintUsed?: number;
  latencyMs?: number;
  nextAction?: string;
  createTime?: string;
}

/** 面试详情 VO */
export interface VoiceInterviewVO {
  id: number;
  userId: number;
  position?: string;
  scene?: string;
  resumeId?: number;
  status: string;
  style?: string;
  difficulty?: string;
  totalQa: number;
  currentIdx: number;
  score?: number;
  summary?: string;
  configJson?: string;
  isPersonalized?: number;
  createTime?: string;
  qaList?: VoiceInterviewQaVO[];
  currentQa?: VoiceInterviewQaVO;
  greetText?: string;
}

/** 报告逐题点评项 */
export interface QuestionReview {
  questionIdx: number;
  question: string;
  score: number;
  feedback: string;
  /** 用户作答（后端可能暂未返回，可选） */
  userAnswer?: string;
  /** 问题深度分析（后端可能暂未返回，可选，优先展示 analysis，否则 fallback 到 feedback） */
  analysis?: string;
  /** 问答ID（用于加入错题本） */
  qaId?: number;
}

/** 亮点/薄弱点条目（兼容纯字符串或带原文引用的对象） */
export type PointItem =
  | string
  | { text?: string; quote?: string };

/** 知识点条目（兼容纯字符串或带标题/描述的对象） */
export type KnowledgePointItem =
  | string
  | { title?: string; name?: string; desc?: string; description?: string };

/** 面试报告 VO */
export interface VoiceInterviewReportVO {
  interviewId: number;
  totalScore: number;
  dimensions?: Record<string, number>;
  highlights?: PointItem[];
  weakPoints?: PointItem[];
  questionReviews?: QuestionReview[];
  summary?: string;
  suggestion?: string;
  /** 相关知识点归纳（V10.2 LLM 版启用，MVP 可空） */
  knowledgePoints?: KnowledgePointItem[];
}

/** SSE 事件回调 */
export interface SseCallbacks {
  /** 规则分（立即返回） */
  onScore?: (data: { score: number; dimensions: Record<string, number> }) => void;
  /** LLM 话术 */
  onSpeak?: (text: string) => void;
  /** 完整数据 */
  onData?: (data: {
    qaId: number;
    score: number;
    feedback: string;
    nextAction: string;
    speakText?: string;
    nextQaId?: number;
    nextQuestion?: string;
    nextSpeakText?: string;
    /** V10.4：LLM 引导提示（回答跑偏时） */
    guidance?: string;
  }) => void;
  /** 结束 */
  onEnd?: () => void;
  /** 错误 */
  onError?: (msg: string) => void;
}

/**
 * 1. 开始语音面试
 * POST /portal/interview/voice/start
 */
export const startVoiceInterview = (config: VoiceStartConfig) => {
  return httpPost<VoiceInterviewVO>('/portal/interview/voice/start', config);
};

/**
 * 2. 提交答案（SSE 双通道流）
 * POST /portal/interview/voice/{id}/answer
 *
 * 使用 fetch + ReadableStream 解析 SSE 事件流（EventSource 不支持 POST + body）
 */
export const submitVoiceAnswer = async (
  interviewId: number | string,
  qaId: number | string,
  transcript: string,
  latencyMs?: number,
  callbacks?: SseCallbacks,
): Promise<void> => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api';
  const url = `${baseURL}/portal/interview/voice/${interviewId}/answer`;
  const token = getToken();

  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ qaId, transcript, latencyMs }),
  });

  if (!resp.ok || !resp.body) {
    const msg = `SSE 连接失败: ${resp.status}`;
    callbacks?.onError?.(msg);
    return;
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      // SSE 事件以 \n\n 分隔
      let idx;
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const block = buffer.slice(0, idx);
        buffer = buffer.slice(idx + 2);
        parseSseBlock(block, callbacks);
      }
    }
    // 处理剩余 buffer
    if (buffer.trim()) {
      parseSseBlock(buffer, callbacks);
    }
  } catch (e) {
    callbacks?.onError?.(e instanceof Error ? e.message : 'SSE 读取异常');
  }
};

/** 解析单个 SSE 事件块 */
function parseSseBlock(block: string, callbacks?: SseCallbacks) {
  const lines = block.split('\n');
  let event = '';
  let data = '';
  for (const line of lines) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim();
    } else if (line.startsWith('data:')) {
      data += line.slice(5).trim();
    }
  }
  if (!event) return;

  try {
    switch (event) {
      case 'score':
        callbacks?.onScore?.(JSON.parse(data));
        break;
      case 'speak':
        callbacks?.onSpeak?.(data);
        break;
      case 'data':
        callbacks?.onData?.(JSON.parse(data));
        break;
      case 'end':
        callbacks?.onEnd?.();
        break;
      case 'error':
        callbacks?.onError?.(data);
        break;
    }
  } catch (e) {
    // JSON 解析失败时降级为纯文本
    if (event === 'error') {
      callbacks?.onError?.(data);
    }
  }
}

/**
 * 3. 请求分级提示
 * POST /portal/interview/voice/{id}/hint
 */
export const requestVoiceHint = (interviewId: number | string, qaId: number | string) => {
  return httpPost<VoiceInterviewVO>(`/portal/interview/voice/${interviewId}/hint`, { qaId });
};

/**
 * 4. 强制下一题
 * POST /portal/interview/voice/{id}/next
 */
export const forceVoiceNext = (interviewId: number | string, reason = 'user_skip') => {
  return httpPost<VoiceInterviewVO>(`/portal/interview/voice/${interviewId}/next`, { reason });
};

/**
 * 5. 结束面试
 * POST /portal/interview/voice/{id}/finish
 */
export const finishVoiceInterview = (interviewId: number | string) => {
  return httpPost<VoiceInterviewReportVO>(`/portal/interview/voice/${interviewId}/finish`);
};

/**
 * 6. 我的语音面试列表
 * GET /portal/interview/voice/my/list
 */
export const getMyVoiceInterviewList = (params: { pageNum?: number; pageSize?: number }) => {
  return httpGet<{ records: VoiceInterviewVO[]; total: number }>(
    '/portal/interview/voice/my/list',
    params,
  );
};

/**
 * 7. 面试详情
 * GET /portal/interview/voice/{id}
 */
export const getVoiceInterviewDetail = (interviewId: number | string) => {
  return httpGet<VoiceInterviewVO>(`/portal/interview/voice/${interviewId}`);
};

/**
 * 8. 薄弱题一键加入错题本
 * POST /portal/interview/voice/qa/{qaId}/toWrongBook
 */
export const addQaToWrongBook = (qaId: number | string) => {
  return httpPost<number>(`/portal/interview/voice/qa/${qaId}/toWrongBook`);
};
