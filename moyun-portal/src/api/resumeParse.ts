import { httpPost } from './client';
import type { ResumePreviewVO } from '@/types/api';

/**
 * 简历附件解析「预览」（同步、不落库）
 *
 * <p>链路：上传附件 → 规则解析引擎毫秒级抽取（纯 Java，不调 LLM，离线可用）
 * → 返回 previewToken + 解析结果 + 原文，供用户左右对照校对。</p>
 *
 * <p><b>本接口不写库</b>：用户校对后调用 {@link confirmResumeParse} 才落库 ——
 * 因此解析失败或用户放弃都不会产生空简历脏数据。原始附件不落盘、不进对象存储。</p>
 */
export const previewResumeParse = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return httpPost<ResumePreviewVO>('/portal/interview/resume/user/parse/preview', formData);
};

/**
 * 确认解析预览并落库
 *
 * <p>把经用户校对的预览结果保存为新简历记录（只落结构化字段 + 原文全文），
 * 返回新简历 ID。原始附件不保留。</p>
 */
export const confirmResumeParse = (data: ResumePreviewVO) => {
  return httpPost<{ resumeId: string | number }>(
    '/portal/interview/resume/user/parse/confirm',
    data,
  );
};
