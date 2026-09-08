import { httpGet, httpPost } from './client';
import { getToken, trackAiSlowRequest, untrackAiSlowRequest } from './client';

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
  /** V11.0：指定面试官智能体（缺省用后台 sys_config 默认） */
  agentId?: number;
  /** V11.0：动态出题模式（缺省用后台 sys_config 开关） */
  dynamicMode?: boolean;
  /** v11.x：岗位模板ID（job 题源出题 + 出题权重默认值） */
  jobTemplateId?: number;
  /** v11.x：出题权重覆盖 job/resume/weak/random */
  questionWeights?: Record<string, number>;
}

/** 可用面试官智能体（/agents 接口返回项） */
export interface VoiceAgentItem {
  id: number;
  name: string;
  description?: string;
  welcomeMessage?: string;
}

/** v11.x：启用中的岗位模板（/jobTemplates 接口返回项） */
export interface VoiceJobTemplateItem {
  id: number;
  name: string;
  category?: string;
  difficulty?: string;
}

/** V11.0：LLM 单轮深度分析（对齐后端 InterviewTurnResult） */
export interface InterviewAnalysis {
  reply?: string;
  score?: number;
  dimensions?: Record<string, number>;
  feedback?: string;
  flaws?: string[];
  redFlags?: string[];
  sentiment?: { state?: string; note?: string };
  fluencyAssessment?: { score?: number; comment?: string };
  completeness?: { covered?: string[]; missing?: string[] };
  level?: string;
  followupWorth?: boolean;
  nextAction?: string;
  nextQuestion?: string;
  candidateId?: number;
  transition?: string;
  guidance?: string;
}

/** 单条问答 VO */
export interface VoiceInterviewQaVO {
  id: number;
  interviewId: number;
  questionId?: number;
  /** V11.0：问题来源 bank=题库/resume_project=简历锚定/llm=智能体生成 */
  questionSource?: string;
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
  /** V11.0：面试官智能体绑定 */
  agentId?: number;
  agentName?: string;
  /** V11.0：出题模式 preset/dynamic */
  questionMode?: string;
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
  /** 分级提示（requestHint 接口返回） */
  hint?: HintVO;
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
  /** V11.0：心态趋势（逐轮 nervous/confident/hesitant/calm） */
  sentimentTrend?: string[];
  /** V11.0：全场可疑信号汇总 */
  redFlags?: string[];
  /** V11.0：表达流畅度均分（0-100） */
  fluencyAvg?: number;
  /** v11.x：自我介绍独立评分（4维度+总分+评语，旧会话无此字段时隐藏） */
  introScore?: IntroScoreView;
  /** v11.x：针对性改进建议（薄弱点/自我介绍不足/错题） */
  improvementSuggestions?: string[];
}

/** v11.x：自我介绍评分视图（对齐后端 VoiceInterviewReportVO.IntroScoreView） */
export interface IntroScoreView {
  dimensions?: Record<string, number>;
  total?: number;
  comment?: string;
  strengths?: string[];
  weaknesses?: string[];
}

/** SSE 事件回调 */
export interface SseCallbacks {
  /** 规则分（立即返回） */
  onScore?: (data: { score: number; dimensions: Record<string, number> }) => void;
  /** V11.0：流式增量文本（打字机效果；data 为 {"t":"增量"} JSON） */
  onDelta?: (text: string) => void;
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
    /** V11.0：agent 动作 deepen/change_topic/wrap_up（旧 nextAction 同时保留） */
    agentAction?: string;
    /** V11.0：换题/收尾过渡话术 */
    transition?: string;
    /** V11.0：LLM 深度分析（心态/流畅度/红旗/完整性） */
    analysis?: InterviewAnalysis;
  }) => void;
  /** 结束 */
  onEnd?: () => void;
  /** 错误 */
  onError?: (msg: string) => void;
  /** V11.0.2：流被服务端异常切断（如后端 SSE 120s 超时收尾），未收到 end 事件 */
  onAborted?: () => void;
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

  // v10.23：SSE 直连 fetch 不经 client.request()，手动登记 AI 慢请求（离开页面提醒）
  const aiTrackKey = trackAiSlowRequest(`/portal/interview/voice/${interviewId}/answer`);
  try {
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
    // 是否收到服务端 end 事件（区分正常结束与流被异常切断，如后端 SSE 超时收尾）
    let ended = false;
    const wrapped: SseCallbacks = {
      ...callbacks,
      onEnd: () => {
        ended = true;
        callbacks?.onEnd?.();
      },
    };

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
          parseSseBlock(block, wrapped);
        }
      }
      // 处理剩余 buffer
      if (buffer.trim()) {
        parseSseBlock(buffer, wrapped);
      }
      // 流关闭但未收到 end：服务端异常切断（SSE 超时收尾/网络中断），回调让页面提示用户
      if (!ended) {
        callbacks?.onAborted?.();
      }
    } catch (e) {
      callbacks?.onError?.(e instanceof Error ? e.message : 'SSE 读取异常');
    }
  } finally {
    untrackAiSlowRequest(aiTrackKey);
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
      case 'delta':
        // V11.0：流式增量 {"t":"..."}；解析失败时按纯文本降级
        try {
          callbacks?.onDelta?.(JSON.parse(data).t ?? '');
        } catch {
          callbacks?.onDelta?.(data);
        }
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

/**
 * 9. 可用面试官智能体列表
 * GET /portal/interview/voice/agents
 */
export const getVoiceAgents = () => {
  return httpGet<VoiceAgentItem[]>('/portal/interview/voice/agents');
};

/**
 * 10. 启用中的岗位模板列表（v11.x 智能出题）
 * GET /portal/interview/voice/jobTemplates
 */
export const getVoiceJobTemplates = () => {
  return httpGet<VoiceJobTemplateItem[]>('/portal/interview/voice/job-templates');
};

/** v11.30.5：生成报告分享令牌（有效期 1-30 天，默认 7 天） */
export const createReportShareToken = (interviewId: number | string, expireDays?: number) => {
  const url = '/portal/interview/voice/' + interviewId + '/share' + (expireDays ? '?expireDays=' + expireDays : '');
  return httpPost<string>(url);
};

/** v11.30.5：通过分享令牌查看报告（免登录公开） */
export const getSharedReport = (shareToken: string) => {
  return httpGet<VoiceInterviewReportVO>('/portal/interview/voice/share/' + shareToken);
};
