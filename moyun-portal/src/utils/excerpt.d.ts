/**
 * 智能摘要提取工具
 * 为未来 AI 扩展预留架构
 */
/**
 * 摘要提取管理器
 * 支持策略切换，为未来 AI 扩展做准备
 */
declare class ExcerptExtractor {
    private currentStrategy;
    private strategies;
    constructor();
    /**
     * 切换策略
     */
    /**
     * 注册新策略（用于未来扩展）
     */
    /**
     * 获取可用策略列表
     */
    /**
     * 提取摘要
     */
    extract(content: string, editorMode?: 'richtext' | 'markdown', maxLength?: number): Promise<string>;
}
declare const excerptExtractor: ExcerptExtractor;
/**
 * 快速提取摘要（向后兼容）
 */
export declare function extractExcerpt(content: string, editorMode?: 'richtext' | 'markdown', maxLength?: number): Promise<string>;
/**
 * 切换摘要提取策略
 */
/**
 * 获取可用策略列表
 */
/**
 * 导出管理器（用于高级用途）
 */
export { excerptExtractor, ExcerptExtractor };
