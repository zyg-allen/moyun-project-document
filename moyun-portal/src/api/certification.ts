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

/** 认证类型下拉选项 */
export const CERT_TYPE_OPTIONS: { value: CreatorCertification['certType']; label: string; desc: string }[] = [
  { value: 'identity', label: '身份认证', desc: '基础实名身份认证' },
  { value: 'creator', label: '创作者认证', desc: '认证为平台创作者，可发布专栏、连载' },
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
