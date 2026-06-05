/**
 * 智能摘要提取工具
 * 为未来 AI 扩展预留架构
 */

/**
 * 摘要提取策略接口
 */
interface ExcerptStrategy {
  name: string;
  extract(content: string, editorMode: 'richtext' | 'markdown', maxLength?: number): Promise<string>;
}

/**
 * 基础本地策略（当前使用）
 */
class LocalExcerptStrategy implements ExcerptStrategy {
  name = 'local';

  /**
   * 清理 HTML 标签
   */
  private stripHtml(html: string): string {
    if (!html) return '';
    
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = html;
    let text = tempDiv.textContent || tempDiv.innerText || '';
    
    return text
      .replace(/\s+/g, ' ')
      .replace(/[\u200B-\u200D\uFEFF]/g, '')
      .trim();
  }

  /**
   * 清理 Markdown 格式
   */
  private stripMarkdown(markdown: string): string {
    if (!markdown) return '';
    
    return markdown
      .replace(/^#+\s+/gm, '')
      .replace(/\*\*(.*?)\*\*/g, '$1')
      .replace(/\*(.*?)\*/g, '$1')
      .replace(/__(.*?)__/g, '$1')
      .replace(/_(.*?)_/g, '$1')
      .replace(/`([^`]+)`/g, '$1')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
      .replace(/!\[([^\]]*)\]\([^)]*\)/g, '')
      .replace(/^>\s+/gm, '')
      .replace(/^[*-]\s+/gm, '')
      .replace(/^\d+\.\s+/gm, '')
      .replace(/^---+$/gm, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }

  /**
   * 提取完整的句子
   */
  private extractCompleteSentences(text: string, maxLength: number = 200): string {
    if (!text) return '';
    
    const sentenceEndings = /[。！？!?]/g;
    const endings: number[] = [];
    let match: RegExpExecArray | null;
    
    while ((match = sentenceEndings.exec(text)) !== null) {
      endings.push(match.index + 1);
    }
    
    if (endings.length === 0) {
      return this.truncateAtWord(text, maxLength);
    }
    
    let selectedEnding = 0;
    for (const ending of endings) {
      if (ending <= maxLength) {
        selectedEnding = ending;
      } else {
        break;
      }
    }
    
    if (selectedEnding === 0) {
      return this.truncateAtWord(text, maxLength);
    }
    
    return text.substring(0, selectedEnding).trim();
  }

  /**
   * 在单词边界处截断文本
   */
  private truncateAtWord(text: string, maxLength: number): string {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    
    const truncated = text.substring(0, maxLength);
    const lastSpace = truncated.lastIndexOf(' ');
    const lastPunctuation = Math.max(
      truncated.lastIndexOf('，'),
      truncated.lastIndexOf(','),
      truncated.lastIndexOf('。'),
      truncated.lastIndexOf('！'),
      truncated.lastIndexOf('？'),
      truncated.lastIndexOf('!'),
      truncated.lastIndexOf('?')
    );
    
    const breakPoint = Math.max(lastSpace, lastPunctuation);
    
    if (breakPoint > maxLength * 0.5) {
      return text.substring(0, breakPoint).trim() + '...';
    }
    
    return text.substring(0, maxLength).trim() + '...';
  }

  async extract(
    content: string,
    editorMode: 'richtext' | 'markdown' = 'richtext',
    maxLength: number = 200
  ): Promise<string> {
    if (!content) return '';
    
    let cleanText = content;
    
    if (editorMode === 'richtext') {
      cleanText = this.stripHtml(content);
    } else {
      cleanText = this.stripMarkdown(content);
    }
    
    return this.extractCompleteSentences(cleanText, maxLength);
  }
}

// /**
//  * AI 策略（预留，未来实现）
//  */
// class AIExcerptStrategy implements ExcerptStrategy {
//   name = 'ai';
//   
//   // 预留 API 配置
//   private apiConfig = {
//     endpoint: '',
//     apiKey: '',
//     model: ''
//   };
// 
//   async extract(
//     content: string,
//     editorMode: 'richtext' | 'markdown' = 'richtext',
//     maxLength: number = 200
//   ): Promise<string> {
//     // 未来实现 AI 摘要提取
//     // 这里可以调用 OpenAI、Claude 或其他 AI 服务
//     throw new Error('AI 策略尚未实现，请配置 API');
//   }
// }

/**
 * 摘要提取管理器
 * 支持策略切换，为未来 AI 扩展做准备
 */
class ExcerptExtractor {
  private currentStrategy: ExcerptStrategy;
  private strategies: Map<string, ExcerptStrategy>;

  constructor() {
    this.strategies = new Map();
    
    // 注册策略
    const localStrategy = new LocalExcerptStrategy();
    this.strategies.set(localStrategy.name, localStrategy);
    
    // 预留 AI 策略位置
    // const aiStrategy = new AIExcerptStrategy();
    // this.strategies.set(aiStrategy.name, aiStrategy);
    
    // 默认使用本地策略
    this.currentStrategy = localStrategy;
  }

  /**
   * 切换策略
   */
  // useStrategy(strategyName: string): boolean {
  //   const strategy = this.strategies.get(strategyName);
  //   if (strategy) {
  //     this.currentStrategy = strategy;
  //     return true;
  //   }
  //   return false;
  // }

  /**
   * 注册新策略（用于未来扩展）
   */
  // registerStrategy(strategy: ExcerptStrategy): void {
  //   this.strategies.set(strategy.name, strategy);
  // }

  /**
   * 获取可用策略列表
   */
  // getAvailableStrategies(): string[] {
  //   return Array.from(this.strategies.keys());
  // }

  /**
   * 提取摘要
   */
  async extract(
    content: string,
    editorMode: 'richtext' | 'markdown' = 'richtext',
    maxLength: number = 200
  ): Promise<string> {
    return this.currentStrategy.extract(content, editorMode, maxLength);
  }
}

// 导出单例
const excerptExtractor = new ExcerptExtractor();

/**
 * 快速提取摘要（向后兼容）
 */
export async function extractExcerpt(
  content: string,
  editorMode: 'richtext' | 'markdown' = 'richtext',
  maxLength: number = 200
): Promise<string> {
  return excerptExtractor.extract(content, editorMode, maxLength);
}

/**
 * 切换摘要提取策略
 */
// export function useExcerptStrategy(strategyName: string): boolean {
//   return excerptExtractor.useStrategy(strategyName);
// }

/**
 * 获取可用策略列表
 */
// export function getExcerptStrategies(): string[] {
//   return excerptExtractor.getAvailableStrategies();
// }

/**
 * 导出管理器（用于高级用途）
 */
export { excerptExtractor, ExcerptExtractor };
