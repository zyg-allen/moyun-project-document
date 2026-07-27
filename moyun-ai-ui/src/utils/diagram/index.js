/**
 * 智能图表模块入口
 * 使用 Draw.io 渲染
 */

import { SmartDiagramRenderer } from './SmartDiagramRenderer.js'

// 渲染模式（只保留Draw.io）
export const RenderMode = {
  DRAWIO: 'drawio',
  SMART: 'smart'
}

/**
 * 图表工厂
 */
export class DiagramFactory {
  /**
   * 创建渲染器
   */
  static createRenderer(mode, container, options = {}) {
    if (mode === RenderMode.SMART) {
      return new SmartDiagramRenderer(container, options)
    }
    // 默认使用 Draw.io 渲染（通过 XML）
    return null
  }
}

export { SmartDiagramRenderer }
