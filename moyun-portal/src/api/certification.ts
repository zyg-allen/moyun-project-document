import { httpGet, httpPost } from './client';

/** 认证申请记录（与后端 PortalCreatorCertification 实体对齐） */
export interface CreatorCertification {
  id?: string | number;
  userId?: string | number;
  realName: string;
  /** 认证类型 identity/creator/expert */
  certType: 'identity' | 'creator' | 'expert';
  certNo?: string;
  certImage?: string;
  /** 身份证正面（人像面）URL */
  certImageFront?: string;
  /** 身份证背面（国徽面）URL */
  certImageBack?: string;
  intro?: string;
  /** 代表作链接 */
  works?: string;
  /** 审核状态 pending/approved/rejected */
  status?: 'pending' | 'approved' | 'rejected';
  auditorId?: string | number;
  auditRemark?: string;
  createdTime?: string;
  auditedTime?: string;
  /** 后台列表接口附加字段：申请人昵称 */
  nickname?: string;
}

/**
 * 认证类型下拉选项
 * 注意：desc 必须与后端实际权限拦截一致。
 * 当前后端拦截：文章发布 / 专栏创建 / 面经发布 需"创作者认证"；
 * 话题创建、评论、点赞、收藏等对任何登录用户开放。
 */
export const CERT_TYPE_OPTIONS: { value: CreatorCertification['certType']; label: string; desc: string }[] = [
  { value: 'identity', label: '身份认证', desc: '基础实名身份认证（满足平台实名要求）' },
  { value: 'creator', label: '创作者认证', desc: '认证为平台创作者，可发布文章、创建专栏、发布面经' },
  { value: 'expert', label: '专家认证', desc: '专业领域权威认证，可申请专家专栏' },
];

/**
 * 提交认证申请（需登录）
 * POST /portal/creator/certification/apply
 */
export const applyCertification = (data: CreatorCertification) => {
  return httpPost<CreatorCertification>(
    '/portal/creator/certification/apply',
    data as unknown as Record<string, unknown>
  );
};

/**
 * 我的认证状态（需登录）
 * GET /portal/creator/certification/my
 */
export const getMyCertification = () => {
  return httpGet<CreatorCertification | null>('/portal/creator/certification/my');
};

/** 身份证 OCR 识别结果（与后端 IdCardOcrResult 对齐） */
export interface IdCardOcrResult {
  /** 识别面：front=人像面 / back=国徽面 */
  side: 'front' | 'back';
  /** 姓名（仅 front） */
  name?: string;
  /** 性别：男 / 女（仅 front） */
  gender?: string;
  /** 民族（仅 front） */
  nation?: string;
  /** 出生日期：yyyy-MM-dd（仅 front） */
  birthDate?: string;
  /** 住址（仅 front） */
  address?: string;
  /** 公民身份号码（仅 front） */
  idNo?: string;
  /** 签发机关（仅 back） */
  authority?: string;
  /** 有效期限（仅 back） */
  validPeriod?: string;
  /** 是否识别成功 */
  success: boolean;
  /** 错误信息 */
  errorMessage?: string;
}

/**
 * 身份证 OCR 识别（需登录）
 * POST /portal/ocr/id-card
 *
 * 当前后端为 STUB 实现，接入真实 OCR API 后会返回真实识别结果。
 */
export const recognizeIdCard = (file: File, side: 'front' | 'back') => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('side', side);
  return httpPost<IdCardOcrResult>('/portal/ocr/id-card', formData);
};
