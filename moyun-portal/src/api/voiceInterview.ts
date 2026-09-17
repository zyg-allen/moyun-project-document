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

/** 难度 */
export type VoiceDifficulty = 'easy' | 'medium' | 'hard';

/** 开始面试请求配置（V3 纯 agent 自由面试：5 个配置字段） */
export interface VoiceStartConfig {
  position?: string;
  /** v11.90 V2：岗位要求 JD（面试官提问方向与深度贴合岗位要求，上限 2000 字） */
  jobRequirements?: string;
  resumeId?: number;
  difficulty?: VoiceDifficulty;
  questionCount?: number;
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
  resumeId?: number;
  /** V11.0：面试官智能体绑定 */
  agentId?: number;
  agentName?: string;
  status: string;
  difficulty?: string;
  totalQa: number;
  currentIdx: number;
  score?: number;
  summary?: string;
  configJson?: string;
  createTime?: string;
  /** v11.96 时长制：本场面试时长（分钟，缺省 20） */
  durationMinutes?: number;
  /** v11.96：报告分析状态（0未分析/1分析中/2已完成） */
  analysisStatus?: number;
  /** v11.96：报告分析进度（0-100） */
  analysisProgress?: number;
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
  /** v11.90 V2：面试者简介（第一栏：简历提取 + 口头自我介绍） */
  candidate?: VoiceCandidateInfo;
  /** v11.90 V2：岗位信息（第二栏：岗位 + JD + 匹配度） */
  jobInfo?: VoiceJobInfo;
  /** v11.97：整场 LLM 复盘总评（3-5 句；旧报告缺失时回退 summary） */
  overallComment?: string;
  /** v11.97：LLM 岗位匹配度评估（旧报告缺失时回退 jobInfo.matchRate） */
  jobMatch?: { rate?: number; reason?: string };
  /** v11.97：结构化亮点（旧报告缺失时回退 highlights） */
  highlightViews?: { title?: string; detail?: string }[];
  /** v11.97：结构化薄弱点（旧报告缺失时回退 weakPoints） */
  weakPointViews?: { title?: string; detail?: string }[];
}

/** v11.90 V2：面试者简介（对齐后端 buildCandidateProfile） */
export interface VoiceCandidateInfo {
  name?: string;
  skills?: string;
  resumeSelfIntro?: string;
  interviewSelfIntro?: string;
  aiScore?: string;
}

/** v11.90 V2：岗位信息（对齐后端 buildJobInfo） */
export interface VoiceJobInfo {
  position?: string;
  jobRequirements?: string;
  matchRate?: string;
}

/** v11.x：自我介绍评分视图（对齐后端 VoiceInterviewReportVO.IntroScoreView） */
export interface IntroScoreView {
  dimensions?: Record<string, number>;
  total?: number;
  comment?: string;
  strengths?: string[];
  weaknesses?: string[];
}

/** end 事件负载 */
export interface VoiceInterviewEndPayload {
  /** 本轮完成后已作答轮数 */
  roundDone: number;
  /** 下一问 qaId（追问或新题；无则本场结束） */
  nextQaId?: number;
  /** 下一问文本 */
  nextQuestion?: string;
  /** 是否本场结束 */
  finished?: boolean;
}

/** SSE 事件回调（V3：delta/end/error 三类事件） */
export interface SseCallbacks {
  /** 流式增量文本（打字机效果；data 为 {"t":"增量"} JSON） */
  onDelta?: (text: string) => void;
  /** 结束（data 为 {roundDone, nextQaId?, nextQuestion?, finished?}；解析失败兜底 undefined） */
  onEnd?: (payload?: VoiceInterviewEndPayload) => void;
  /** 错误 */
  onError?: (msg: string) => void;
  /** 流被服务端异常切断（如后端 SSE 120s 超时收尾），未收到 end 事件 */
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
 * 2. 提交答案（SSE 流：delta/end/error 三类事件）
 * POST /portal/interview/voice/{id}/answer
 *
 * 使用 fetch + ReadableStream 解析 SSE 事件流（EventSource 不支持 POST + body）
 * @param skip 跳过本题（transcript 可为空）
 */
export const submitVoiceAnswer = async (
  interviewId: number | string,
  qaId: number | string,
  transcript: string,
  latencyMs?: number,
  callbacks?: SseCallbacks,
  skip?: boolean,
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
      body: JSON.stringify({ qaId, transcript, latencyMs, skip: skip || undefined }),
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
      onEnd: (payload) => {
        ended = true;
        callbacks?.onEnd?.(payload);
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

/** 解析单个 SSE 事件块（V3：只处理 delta/end/error） */
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

  switch (event) {
    case 'delta': {
      // 流式增量 {"t":"..."}；解析失败时按纯文本降级
      try {
        callbacks?.onDelta?.(JSON.parse(data).t ?? '');
      } catch {
        callbacks?.onDelta?.(data);
      }
      break;
    }
    case 'end': {
      // {roundDone, nextQaId?, nextQuestion?, finished?}；解析失败兜底传 undefined
      let payload: VoiceInterviewEndPayload | undefined;
      try {
        payload = JSON.parse(data);
      } catch {
        payload = undefined;
      }
      callbacks?.onEnd?.(payload);
      break;
    }
    case 'error':
      callbacks?.onError?.(data);
      break;
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
 * 5. 结束面试
 * POST /portal/interview/voice/{id}/finish
 * v11.88 V2：仅收口会话并触发异步批量分析，返回报告骨架；
 * 进度轮询走 5.1 analysis 接口，analysisStatus=2 后拉取完整报告。
 */
export const finishVoiceInterview = (interviewId: number | string) => {
  return httpPost<VoiceInterviewReportVO>(`/portal/interview/voice/${interviewId}/finish`);
};

/**
 * 5.1 报告分析状态（v11.88 V2：前端进度条轮询）
 * GET /portal/interview/voice/{id}/analysis
 */
export interface VoiceAnalysisStatusVO {
  analysisStatus: number; // 0未分析 1分析中 2已完成
  analysisProgress: number; // 0-100
  status: string;
}
export const getVoiceAnalysisStatus = (interviewId: number | string) => {
  return httpGet<VoiceAnalysisStatusVO>(`/portal/interview/voice/${interviewId}/analysis`);
};

/**
 * 5.2 重新生成报告（v11.97）
 * POST /portal/interview/voice/{id}/regenerate-report
 * 重置分析状态后重跑异步批量分析链路（逐题补分析 + 聚合 + 整场 LLM 复盘）；
 * 轮询 5.1 analysis 接口直至 analysisStatus=2 后拉取完整报告。
 */
export const regenerateVoiceReport = (interviewId: number | string) => {
  return httpPost<VoiceInterviewReportVO>(`/portal/interview/voice/${interviewId}/regenerate-report`);
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
 * 7.1 查询进行中会话（v11.91 断点续接）
 * GET /portal/interview/voice/active
 * <p>意外关闭后再次进入，返回最近一个未结束的面试；空对象表示无。
 */
export interface ActiveVoiceInterviewVO {
  interviewId?: number;
  position?: string;
  scene?: string;
  startTime?: string;
  answered?: number;
  totalQa?: number;
  elapsedSec?: number;
}
export const getActiveVoiceInterview = () => {
  return httpGet<ActiveVoiceInterviewVO>('/portal/interview/voice/active');
};

/**
 * 7.2 恢复进行中会话（v11.91 断点续接）
 * GET /portal/interview/voice/{id}/resume
 * <p>返回恢复快照（qaList 历史问答 + currentQa 待答题），前端据此重建面试页。
 */
export const resumeVoiceInterview = (interviewId: number | string) => {
  return httpGet<VoiceInterviewVO>(`/portal/interview/voice/${interviewId}/resume`);
};

/**
 * 8. 薄弱题一键加入错题本
 * POST /portal/interview/voice/qa/{qaId}/toWrongBook
 */
export const addQaToWrongBook = (qaId: number | string) => {
  return httpPost<number>(`/portal/interview/voice/qa/${qaId}/toWrongBook`);
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
