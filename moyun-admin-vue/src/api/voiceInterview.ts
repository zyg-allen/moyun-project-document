import request from '@/utils/request'

/**
 * 语音面试管理 API（Admin 端只读复盘）
 * 后端：CmsVoiceInterviewController，路径 /cms/voice-interview
 * 类型参考：portal 端 voiceInterview.ts
 */

// ==================== 类型定义 ====================

/** 单条问答 VO */
export interface VoiceInterviewQaVO {
  id: number
  interviewId: number
  questionId?: number
  questionIdx: number
  parentQaId?: number
  question: string
  userAnswer?: string
  transcriptionEdited?: number
  aiFeedback?: string
  speakText?: string
  score?: number
  ruleDimensionsJson?: string
  hintUsed?: number
  latencyMs?: number
  nextAction?: string
  createTime?: string
}

/** 面试详情 VO（含 qaList） */
export interface VoiceInterviewVO {
  id: number
  userId: number
  username?: string
  position?: string
  scene?: string
  resumeId?: number
  status: string
  style?: string
  difficulty?: string
  totalQa: number
  currentIdx: number
  score?: number
  summary?: string
  configJson?: string
  isPersonalized?: number
  createTime?: string
  qaList?: VoiceInterviewQaVO[]
  currentQa?: VoiceInterviewQaVO
  greetText?: string
}

/** 列表查询参数 */
export interface VoiceInterviewQuery {
  pageNum?: number
  pageSize?: number
  username?: string
  position?: string
  status?: string
}

// ==================== API 方法 ====================

/**
 * 分页查询语音面试列表（所有用户）
 * GET /cms/voice-interview/list
 */
export function getVoiceInterviewList(params: VoiceInterviewQuery) {
  return request({
    url: '/cms/voice-interview/list',
    method: 'get',
    params
  })
}

/**
 * 查询语音面试详情（含 qaList 问答列表）
 * GET /cms/voice-interview/{id}
 */
export function getVoiceInterviewDetail(id: number | string) {
  return request({
    url: '/cms/voice-interview/' + id,
    method: 'get'
  })
}
