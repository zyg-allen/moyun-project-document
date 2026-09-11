import { httpPost } from './client';

// ==================== 统一 AI 内容分析 API ====================

/** 支持的分析场景（与后端 PortalAiController SCENES 注册表对应） */
export type AiScene = 'article-meta' | 'tags';

/** 统一请求参数 */
export interface AiAnalyzeParams {
    /** 分析场景 */
    scene: AiScene;
    /** 标题（可选） */
    title?: string;
    /** 正文内容（markdown 或 HTML 均可，后端统一转纯文本） */
    content: string;
}

/** 文章元信息场景结果 */
export interface ArticleMetaResult {
    summary: string;
    seoTitle: string;
    seoDescription: string;
    seoKeywords: string;
    /** ai=AI 生成 / fallback=本地兜底 */
    source: 'ai' | 'fallback';
}

/** 标签提取场景结果 */
export interface TagsResult {
    tags: string[];
    source: 'ai' | 'fallback';
}

/**
 * 统一内容分析入口
 * POST /portal/ai/analyze
 * 后端按 scene 返回结构化结果；AI 未配置时返回本地兜底（source=fallback）
 */
export const aiAnalyze = <T = Record<string, any>>(params: AiAnalyzeParams) => {
    return httpPost<T>('/portal/ai/analyze', params);
};
