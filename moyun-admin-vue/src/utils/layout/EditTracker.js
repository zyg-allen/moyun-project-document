/**
 * 编辑追踪器
 * 追踪用户在 Draw.io 中手动编辑的节点
 */

/**
 * 编辑追踪器类
 */
export class EditTracker {
  constructor() {
    this.nodeStates = new Map() // nodeId -> NodeEditState
    this.lastKnownXml = ''
    this.lastUpdateTime = 0
  }

  /**
   * 当 Draw.io 报告 autosave 时调用
   * @param {string} newXml - 新的 XML 内容
   */
  onXmlChanged(newXml) {
    if (!newXml) return

    if (!this.lastKnownXml) {
      // 首次，记录所有节点位置
      this.initFromXml(newXml)
      this.lastKnownXml = newXml
      return
    }

    // 比较新旧 XML，找出位置变化的节点
    const oldPositions = this.extractPositions(this.lastKnownXml)
    const newPositions = this.extractPositions(newXml)

    for (const [id, newPos] of newPositions) {
      const oldPos = oldPositions.get(id)
      const state = this.nodeStates.get(id)

      if (oldPos && state) {
        // 检查位置是否变化（超过 5px 视为手动移动）
        const posChanged = Math.abs(newPos.x - oldPos.x) > 5 ||
                          Math.abs(newPos.y - oldPos.y) > 5

        if (posChanged) {
          state.currentPosition = { ...newPos }
          state.isManuallyEdited = true
          state.lastEditTime = Date.now()
          console.log(`[EditTracker] 节点 ${id} 被手动移动到 (${newPos.x}, ${newPos.y})`)
        }
      } else if (!state) {
        // 新节点
        this.nodeStates.set(id, {
          id,
          originalPosition: { ...newPos },
          currentPosition: { ...newPos },
          isManuallyEdited: false,
          lastEditTime: 0
        })
      }
    }

    // 检测被删除的节点
    for (const id of this.nodeStates.keys()) {
      if (!newPositions.has(id)) {
        this.nodeStates.delete(id)
      }
    }

    this.lastKnownXml = newXml
    this.lastUpdateTime = Date.now()
  }

  /**
   * 获取被手动编辑的节点 ID 集合
   * @returns {Set<string>}
   */
  getManuallyEditedNodeIds() {
    const result = new Set()
    for (const [id, state] of this.nodeStates) {
      if (state.isManuallyEdited) {
        result.add(id)
      }
    }
    return result
  }

  /**
   * 获取锁定节点的位置（用于增量布局）
   * @returns {Map<string, {x: number, y: number}>}
   */
  getLockedPositions() {
    const result = new Map()
    for (const [id, state] of this.nodeStates) {
      if (state.isManuallyEdited) {
        result.set(id, { ...state.currentPosition })
      }
    }
    return result
  }

  /**
   * 判断是否需要增量布局
   * @returns {boolean}
   */
  hasManualEdits() {
    for (const state of this.nodeStates.values()) {
      if (state.isManuallyEdited) return true
    }
    return false
  }

  /**
   * 当 AI 生成新图表时，重置追踪状态
   */
  resetForNewDiagram() {
    this.nodeStates.clear()
    this.lastKnownXml = ''
    this.lastUpdateTime = 0
    console.log('[EditTracker] 已重置')
  }

  /**
   * 保留已追踪的编辑状态（用于增量更新）
   * @param {Set<string>} newNodeIds - 新图表的节点ID集合
   */
  preserveEditedNodes(newNodeIds) {
    for (const id of this.nodeStates.keys()) {
      if (!newNodeIds.has(id)) {
        this.nodeStates.delete(id)
      }
    }
  }

  // ========== 私有方法 ==========

  /**
   * 从 XML 初始化节点状态
   */
  initFromXml(xml) {
    const positions = this.extractPositions(xml)
    for (const [id, pos] of positions) {
      this.nodeStates.set(id, {
        id,
        originalPosition: { ...pos },
        currentPosition: { ...pos },
        isManuallyEdited: false,
        lastEditTime: 0
      })
    }
    console.log('[EditTracker] 初始化，节点数:', this.nodeStates.size)
  }

  /**
   * 从 XML 提取节点位置
   * @param {string} xml
   * @returns {Map<string, {x: number, y: number, width: number, height: number}>}
   */
  extractPositions(xml) {
    const result = new Map()

    // 匹配 vertex 节点（不是边）
    // 格式：<mxCell id="xxx" ... vertex="1" ...><mxGeometry x="..." y="..." ...
    const cellRegex = /<mxCell[^>]*\sid="([^"]+)"[^>]*vertex="1"[^>]*>[\s\S]*?<mxGeometry[^>]*x="([^"]+)"[^>]*y="([^"]+)"[^>]*width="([^"]+)"[^>]*height="([^"]+)"/g

    let match
    while ((match = cellRegex.exec(xml)) !== null) {
      const [, id, x, y, width, height] = match
      // 排除根节点
      if (id !== '0' && id !== '1') {
        result.set(id, {
          x: parseFloat(x),
          y: parseFloat(y),
          width: parseFloat(width),
          height: parseFloat(height)
        })
      }
    }

    return result
  }

  /**
   * 获取统计信息
   */
  getStats() {
    const total = this.nodeStates.size
    const edited = this.getManuallyEditedNodeIds().size
    return {
      totalNodes: total,
      editedNodes: edited,
      lastUpdateTime: this.lastUpdateTime
    }
  }
}

// 导出单例
export const editTracker = new EditTracker()
