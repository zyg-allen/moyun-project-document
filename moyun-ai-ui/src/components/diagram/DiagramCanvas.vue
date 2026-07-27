<template>
  <div class="diagram-canvas" ref="containerRef"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Graph } from '@antv/x6'
import { Selection } from '@antv/x6-plugin-selection'
import { Snapline } from '@antv/x6-plugin-snapline'
import { Keyboard } from '@antv/x6-plugin-keyboard'
import { History } from '@antv/x6-plugin-history'
import { Export } from '@antv/x6-plugin-export'

// 引入公共工具模块（仅导入实际使用的函数）
import {
  truncateText,
  getIconInfo,
} from '@/utils/diagram'
import { logger } from '@/utils/logger'

// 注意：calcNodeWidth、getDescendantCount 等函数在各渲染函数内部有本地定义
// 常量配置可按需从 @/constants/diagram 导入

const props = defineProps({
  // 图形数据
  data: {
    type: Object,
    default: null
  },
  // 风格类型: normal | enterprise
  styleType: {
    type: String,
    default: 'normal'
  }
})

const emit = defineEmits(['ready', 'node-click'])

const containerRef = ref(null)
let graph = null

// 注意：iconMap、iconColorMap、getIconInfo、truncateText 已移至公共模块
// 详见：@/utils/diagram.js 和 @/constants/diagram.js

// 初始化画布
onMounted(() => {
  initGraph()
})

// 销毁画布
onUnmounted(() => {
  if (graph) {
    graph.dispose()
  }
})

// 监听数据变化
watch(() => props.data, (newData) => {
  if (newData && graph) {
    renderDiagram(newData)
  }
}, { deep: true })

/**
 * 初始化Graph
 */
function initGraph() {
  graph = new Graph({
    container: containerRef.value,
    autoResize: true,
    background: {
      color: '#fafafa'
    },
    grid: {
      visible: true,
      type: 'doubleMesh',
      args: [
        { color: '#eee', thickness: 1 },
        { color: '#ddd', thickness: 1, factor: 4 }
      ]
    },
    // 启用节点拖拽
    interacting: {
      nodeMovable: true,
      edgeMovable: true,
      edgeLabelMovable: true
    },
    // 拖拽时显示参考线
    translating: {
      restrict: false
    },
    // 启用嵌入分组功能
    embedding: {
      enabled: true,
      findParent({ node }) {
        const bbox = node.getBBox()
        return this.getNodes().filter((n) => {
          const id = n.id
          if (id && id.startsWith('stage-bg-')) {
            const targetBBox = n.getBBox()
            return targetBBox.containsRect(bbox)
          }
          return false
        })
      }
    },
    panning: {
      enabled: true,
      eventTypes: ['leftMouseDown', 'mouseWheel']  // 左键拖动 + 滚轮拖动
    },
    mousewheel: {
      enabled: true,
      modifiers: ['ctrl', 'meta'],
      minScale: 0.3,
      maxScale: 3
    },
    // 启用连线功能
    connecting: {
      anchor: 'center',
      connectionPoint: 'anchor',
      router: 'orth',
      connector: {
        name: 'rounded',
        args: { radius: 8 }
      },
      allowBlank: false,
      allowLoop: false,
      allowNode: true,
      highlight: true
    },
    // 高亮配置
    highlighting: {
      magnetAvailable: {
        name: 'stroke',
        args: { attrs: { stroke: '#3B82F6', strokeWidth: 3 } }
      },
      magnetAdsorbed: {
        name: 'stroke',
        args: { attrs: { stroke: '#3B82F6', strokeWidth: 3 } }
      }
    }
  })

  // 注册插件
  graph.use(new Selection({
    enabled: true,
    multiple: true,
    rubberband: true,
    showNodeSelectionBox: true
  }))

  graph.use(new Snapline({ enabled: true }))
  graph.use(new Keyboard({ enabled: true }))
  graph.use(new History({ enabled: true }))
  graph.use(new Export())

  // 快捷键
  graph.bindKey('ctrl+z', () => graph.undo())
  graph.bindKey('ctrl+y', () => graph.redo())
  graph.bindKey('delete', () => {
    const cells = graph.getSelectedCells()
    if (cells.length) {
      graph.removeCells(cells)
    }
  })

  // 节点点击事件
  graph.on('node:click', ({ node }) => {
    emit('node-click', node.getData())
  })

  emit('ready', graph)
}

/**
 * 渲染架构图 - 根据类型和风格选择不同布局
 */
function renderDiagram(data) {
  if (!graph || !data) return
  graph.clearCells()

  const type = data.type || 'layered'
  const style = props.styleType || 'normal'
  
  logger.log('X6 渲染 - 类型:', type, '风格:', style)
  
  switch (type) {
    case 'org':
      renderOrgDiagram(data)
      break
    case 'topology':
      renderTopologyDiagram(data)
      break
    case 'flow':
      renderFlowDiagram(data)
      break
    case 'layered':
    case 'layered-sidebar':
    case 'enterprise-full':
    default:
      // 根据风格选择渲染方式
      if (style === 'normal') {
        logger.log('>>> X6 使用普通风格')
        renderLayeredDiagramNormal(data)
      } else {
        logger.log('>>> X6 使用企业级风格')
        // 企业级风格支持侧边栏
        renderLayeredDiagram(data)
      }
      break
  }

  // 居中
  setTimeout(() => {
    graph.zoomToFit({ padding: 60, maxScale: 1.2 })
  }, 100)
}

/**
 * 组织架构图 - 树形结构
 */
function renderOrgDiagram(data) {
  const { layers, title } = data
  if (!layers || !layers.length) return

  // 收集所有节点
  const allNodes = []
  layers.forEach(layer => {
    (layer.nodes || []).forEach(node => allNodes.push(node))
  })
  
  const roots = allNodes.filter(n => !n.parent)
  
  // 布局参数
  const nodeWidth = 85
  const nodeHeight = 50
  const minNodeSpace = 120  // 每个叶子节点最小占用空间（增加避免重叠）
  const gapY = 80
  
  // 计算后代数量
  const getDescendantCount = (nodeId) => {
    const children = allNodes.filter(n => n.parent === nodeId)
    if (children.length === 0) return 1
    return children.reduce((sum, c) => sum + getDescendantCount(c.id), 0)
  }
  
  // 计算画布宽度
  const totalLeaves = roots.reduce((sum, r) => sum + getDescendantCount(r.id), 0)
  const canvasWidth = Math.max(1400, totalLeaves * minNodeSpace)
  
  // 递归布局
  const layoutNode = (node, x, y, availableWidth) => {
    const iconInfo = getIconInfo(node.icon)
    const nodeX = x + availableWidth / 2 - nodeWidth / 2
    
    // 添加节点 - X6 强调交互性，hover 效果通过 CSS 实现
    graph.addNode({
      id: node.id,
      x: nodeX,
      y,
      width: nodeWidth,
      height: nodeHeight,
      zIndex: 2,
      data: { ...node, type: 'org' },  // X6 优势：可携带完整数据
      markup: [
        { tagName: 'rect', selector: 'body' },
        { tagName: 'rect', selector: 'stripe' },  // X6 特色：左侧色条
        { tagName: 'text', selector: 'icon' },
        { tagName: 'text', selector: 'label' }
      ],
      attrs: {
        body: {
          fill: '#FFFFFF',
          stroke: '#E0E0E0',
          strokeWidth: 1,
          rx: 6,
          ry: 6,
          cursor: 'move',
          filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
        },
        stripe: {
          fill: iconInfo.color,
          width: 4,
          height: nodeHeight,
          x: 0,
          y: 0,
          rx: 6,
          ry: 0
        },
        icon: {
          text: iconInfo.icon,
          fontSize: 14,
          refX: 0.5,
          refY: 0.32,
          textAnchor: 'middle'
        },
        label: {
          text: truncateText(node.label, 6),
          fill: '#333',
          fontSize: 9,
          fontWeight: 500,
          refX: 0.5,
          refY: 0.72,
          textAnchor: 'middle'
        }
      },
      // X6 优势：悬停时可通过 data 查看完整信息
      data: { ...node, fullLabel: node.label }
    })
    
    // 子节点
    const children = allNodes.filter(n => n.parent === node.id)
    if (children.length === 0) return
    
    const childY = y + nodeHeight + gapY
    let childX = x
    const totalDescendants = children.reduce((sum, c) => sum + getDescendantCount(c.id), 0)
    const childWidth = availableWidth / totalDescendants
    
    children.forEach(child => {
      const descendants = getDescendantCount(child.id)
      const childAvailableWidth = childWidth * descendants
      
      // 连线
      graph.addEdge({
        source: { cell: node.id, anchor: 'bottom' },
        target: { cell: child.id, anchor: 'top' },
        connector: { name: 'rounded' },
        router: { name: 'er', args: { direction: 'V' } },
        attrs: {
          line: {
            stroke: '#999',
            strokeWidth: 1.5,
            targetMarker: null
          }
        },
        zIndex: 1
      })
      
      layoutNode(child, childX, childY, childAvailableWidth)
      childX += childAvailableWidth
    })
  }
  
  // 添加标题
  const startY = title ? 70 : 40
  if (title) {
    graph.addNode({
      x: canvasWidth / 2 - 150,
      y: 15,
      width: 300,
      height: 35,
      markup: [{ tagName: 'text', selector: 'label' }],
      attrs: {
        label: {
          text: title,
          fill: '#333',
          fontSize: 18,
          fontWeight: 700,
          refX: 0.5,
          refY: 0.5,
          textAnchor: 'middle'
        }
      }
    })
  }
  
  // 布局根节点
  const rootWidth = canvasWidth / roots.length
  roots.forEach((root, i) => {
    layoutNode(root, i * rootWidth, startY, rootWidth)
  })
}

/**
 * 企业级分层架构图 - 智能布局（支持多行、blocks分组、自适应宽度）
 * 兼容 layered-sidebar（leftSidebar/rightSidebar）和 enterprise-full（leftMonitor/rightSidebars）格式
 */
function renderLayeredDiagram(data) {
  const { layers } = data
  if (!layers || !layers.length) return
  
  // 兼容两种侧边栏格式
  const leftSidebar = data.leftSidebar || data.leftMonitor
  const rightSidebar = data.rightSidebar || (data.rightSidebars?.[0])

  // === 布局参数 - 紧凑布局 ===
  const cfg = {
    labelWidth: 85,           // 左侧标签宽度
    labelGap: 2,
    nodeHeight: 24,           // 节点高度（减小）
    nodeGapX: 6,              // 节点水平间距（减小）
    nodeGapY: 4,              // 节点垂直间距
    layerPaddingX: 8,         // 层水平边距
    layerPaddingY: 8,         // 层垂直边距
    layerGap: 3,              // 层间距
    maxRowWidth: 700,         // 每行最大宽度
    blockGapX: 8,             // blocks 间距（减小）
    blockPaddingX: 6,         // block 内边距（减小）
    blockPaddingY: 5,
    blockTitleH: 18,          // block 标题高度（减小）
    sidebarWidth: 100,        // 侧边栏宽度（增加以容纳长文字）
    sidebarNodeH: 36,         // 侧边栏节点高度
    sidebarNodeGap: 2,        // 侧边栏节点间距
    sidebarPadding: 4,
    sidebarTitleH: 28,        // 侧边栏标题高度
  }
  
  // 计算节点宽度（更紧凑）
  const calcNodeWidth = (label) => {
    let textWidth = 0
    for (const char of (label || '')) {
      textWidth += /[\u4e00-\u9fa5]/.test(char) ? 11 : 5.5
    }
    return Math.max(55, textWidth + 12)
  }
  
  // === 企业级统一配色 - 蓝色调 ===
  const colors = {
    layerBg: '#F0F7FF',
    layerBorder: '#ADC6FF',
    labelBg: '#1890FF',
    labelText: '#FFFFFF',
    blockBg: '#FFFFFF',
    blockBorder: '#E8E8E8',
    blockTitleBg: { blue: '#1890FF', green: '#52C41A', yellow: '#FAAD14', purple: '#722ED1', pink: '#EB2F96', gray: '#8C8C8C', orange: '#FA8C16', cyan: '#13C2C2' },
    blockTitleText: '#FFFFFF',
    nodeBg: '#FFFFFF',
    nodeBorder: '#D9D9D9',
    nodeText: '#333333',
    sidebarBg: '#E6F4FF',
    sidebarBorder: '#91CAFF',
    sidebarNodeBg: '#1890FF',
    sidebarNodeText: '#FFFFFF',
  }
  
  // === 判断是否有侧边栏 ===
  const hasLeft = leftSidebar?.nodes?.length > 0
  const hasRight = rightSidebar?.nodes?.length > 0
  const leftNodes = leftSidebar?.nodes || []
  const rightNodes = rightSidebar?.nodes || []
  
  // === 第一遍：计算每层的布局信息 ===
  const layerInfos = []
  let maxLayerWidth = 400
  
  layers.forEach((layer) => {
    const layerBlocks = layer.blocks || layer.groups || []
    const hasBlocks = layerBlocks.length > 0
    
    let layerWidth = 0
    let layerHeight = 0
    let blockInfos = []
    let nodeRows = []
    
    if (hasBlocks) {
      // === 有 blocks：支持 block 内多行节点 ===
      layerBlocks.forEach(block => {
        const nodes = block.nodes || []
        if (nodes.length === 0) return
        
        const nodeWidths = nodes.map(n => calcNodeWidth(n.label))
        
        // 计算 block 内节点分行
        const blockRows = []
        let currentRow = []
        let currentRowWidths = []
        let currentRowWidth = 0
        const blockMaxRowWidth = 220  // 减小以支持更多换行
        
        nodes.forEach((node, idx) => {
          const w = nodeWidths[idx]
          if (currentRowWidth + w + (currentRow.length > 0 ? cfg.nodeGapX : 0) > blockMaxRowWidth && currentRow.length > 0) {
            blockRows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
            currentRow = [node]
            currentRowWidths = [w]
            currentRowWidth = w
          } else {
            currentRow.push(node)
            currentRowWidths.push(w)
            currentRowWidth += w + (currentRow.length > 1 ? cfg.nodeGapX : 0)
          }
        })
        if (currentRow.length > 0) {
          blockRows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
        }
        
        const maxRowW = blockRows.length > 0 ? Math.max(...blockRows.map(r => r.totalWidth)) : 100
        const blockW = maxRowW + cfg.blockPaddingX * 2
        const blockH = cfg.blockTitleH + blockRows.length * cfg.nodeHeight + (blockRows.length - 1) * cfg.nodeGapY + cfg.blockPaddingY * 2
        blockInfos.push({ block, blockRows, width: blockW, height: blockH })
        layerWidth += blockW + cfg.blockGapX
      })
      
      if (blockInfos.length > 0) {
        layerWidth -= cfg.blockGapX
        layerWidth += cfg.layerPaddingX * 2
        layerHeight = Math.max(...blockInfos.map(b => b.height)) + cfg.layerPaddingY * 2
      } else {
        layerWidth = 400
        layerHeight = 50
      }
    } else {
      // === 无 blocks：智能提取节点，支持多行 ===
      let allNodes = []
      if (layer.nodes?.length > 0) {
        allNodes = [...layer.nodes]
      } else if (layer.systems?.length > 0) {
        layer.systems.forEach(sys => {
          (sys.subsystems || []).forEach(ss => {
            (ss.nodes || []).forEach(n => allNodes.push(n))
          })
        })
      }
      
      // 计算每个节点宽度并分行
      const nodeWidths = allNodes.map(n => calcNodeWidth(n.label))
      let currentRow = []
      let currentRowWidths = []
      let currentRowWidth = 0
      
      allNodes.forEach((node, idx) => {
        const w = nodeWidths[idx]
        if (currentRowWidth + w + (currentRow.length > 0 ? cfg.nodeGapX : 0) > cfg.maxRowWidth && currentRow.length > 0) {
          // 换行
          nodeRows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
          currentRow = [node]
          currentRowWidths = [w]
          currentRowWidth = w
        } else {
          currentRow.push(node)
          currentRowWidths.push(w)
          currentRowWidth += w + (currentRow.length > 1 ? cfg.nodeGapX : 0)
        }
      })
      if (currentRow.length > 0) {
        nodeRows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
      }
      
      const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 0
      // 紧凑布局：减少最小宽度
      layerWidth = Math.max(maxRowWidth, 100) + cfg.layerPaddingX * 2
      layerHeight = Math.max(nodeRows.length, 1) * cfg.nodeHeight + Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY + cfg.layerPaddingY * 2
    }
    
    maxLayerWidth = Math.max(maxLayerWidth, layerWidth)
    layerInfos.push({ layer, hasBlocks, blockInfos, nodeRows, width: layerWidth, height: layerHeight })
  })
  
  // === 计算侧边栏高度 ===
  // 侧边栏高度计算（标题 + 节点 + 间距）
  const leftSidebarH = cfg.sidebarTitleH + leftNodes.length * cfg.sidebarNodeH + (leftNodes.length - 1) * cfg.sidebarNodeGap + cfg.sidebarPadding * 2
  const rightSidebarH = cfg.sidebarTitleH + rightNodes.length * cfg.sidebarNodeH + (rightNodes.length - 1) * cfg.sidebarNodeGap + cfg.sidebarPadding * 2
  
  // === 计算位置（更紧凑）===
  const leftX = 5
  const mainX = hasLeft ? leftX + cfg.sidebarWidth + 5 : 0
  const contentStartX = mainX + cfg.labelWidth + cfg.labelGap
  const rightX = contentStartX + maxLayerWidth + 5
  
  let currentY = 15
  const startY = currentY
  
  // === 渲染左侧边栏 ===
  if (hasLeft) {
    const totalMainH = layerInfos.reduce((s, l) => s + l.height + cfg.layerGap, 0)
    const sbH = Math.max(leftSidebarH, totalMainH)
    
    graph.addNode({
      id: 'left-sidebar-bg',
      x: leftX, y: startY, width: cfg.sidebarWidth, height: sbH, zIndex: 0,
      markup: [{ tagName: 'rect', selector: 'body' }],
      attrs: { body: { fill: colors.sidebarBg, stroke: colors.sidebarBorder, strokeWidth: 1, strokeDasharray: '4,2', rx: 4, ry: 4 } }
    })
    
    // 侧边栏标题（带背景色）
    graph.addNode({
      id: 'left-sidebar-title',
      x: leftX + 4, y: startY + 4, width: cfg.sidebarWidth - 8, height: cfg.sidebarTitleH - 4, zIndex: 1,
      markup: [{ tagName: 'rect', selector: 'body' }, { tagName: 'text', selector: 'label' }],
      attrs: { 
        body: { fill: '#1890FF', stroke: 'none', rx: 4, ry: 4 },
        label: { text: leftSidebar?.title || '', fill: '#FFFFFF', fontSize: 10, fontWeight: 600, refX: 0.5, refY: 0.5, textAnchor: 'middle', textWrap: { width: cfg.sidebarWidth - 16, ellipsis: false } } 
      }
    })
    
    let nodeY = startY + cfg.sidebarTitleH + cfg.sidebarPadding
    leftNodes.forEach((node, i) => {
      graph.addNode({
        id: node.id,
        x: leftX + 4, y: nodeY, width: cfg.sidebarWidth - 8, height: cfg.sidebarNodeH, zIndex: 2,
        markup: [
          { tagName: 'rect', selector: 'body' },
          { tagName: 'text', selector: 'label' }
        ],
        attrs: {
          body: { fill: colors.sidebarNodeBg, stroke: 'none', rx: 4, ry: 4 },
          label: { text: node.label, fill: colors.sidebarNodeText, fontSize: 8, fontWeight: 500, refX: 0.5, refY: 0.5, textAnchor: 'middle', textWrap: { width: cfg.sidebarWidth - 16, ellipsis: true } }
        }
      })
      nodeY += cfg.sidebarNodeH + cfg.sidebarNodeGap
    })
  }
  
  // === 渲染每层 ===
  layerInfos.forEach((info, layerIndex) => {
    const { layer, hasBlocks, blockInfos, nodeRows, height: layerHeight } = info
    
    // 层背景（从 mainX 开始，包含标签和内容）
    const layerBgX = hasLeft ? mainX : 0
    const layerBgWidth = cfg.labelWidth + maxLayerWidth + cfg.labelGap
    graph.addNode({
      id: `layer-bg-${layerIndex}`,
      x: layerBgX, y: currentY, width: layerBgWidth, height: layerHeight, zIndex: 0,
      markup: [{ tagName: 'rect', selector: 'body' }],
      attrs: { body: { fill: colors.layerBg, stroke: colors.layerBorder, strokeWidth: 1, rx: 0, ry: 0 } }
    })
    
    // 左侧标签
    graph.addNode({
      id: `layer-label-${layerIndex}`,
      x: layerBgX, y: currentY, width: cfg.labelWidth, height: layerHeight, zIndex: 1,
      markup: [{ tagName: 'rect', selector: 'body' }, { tagName: 'text', selector: 'label' }],
      attrs: {
        body: { fill: colors.labelBg, stroke: 'none', rx: 0, ry: 0 },
        label: { text: layer.name || '', fill: colors.labelText, fontSize: 11, fontWeight: 600, refX: 0.5, refY: 0.5, textAnchor: 'middle' }
      }
    })
    
    if (hasBlocks) {
      // === 渲染 blocks（支持多行节点）===
      let blockX = contentStartX + cfg.layerPaddingX
      const blockY = currentY + cfg.layerPaddingY
      
      blockInfos.forEach((bInfo, bIdx) => {
        const { block, blockRows, width: blockW, height: blockH } = bInfo
        const blockColor = colors.blockTitleBg[block.color] || colors.blockTitleBg.blue
        
        // block 背景
        graph.addNode({
          id: `block-bg-${layerIndex}-${bIdx}`,
          x: blockX, y: blockY, width: blockW, height: blockH, zIndex: 1,
          markup: [{ tagName: 'rect', selector: 'body' }],
          attrs: { body: { fill: colors.blockBg, stroke: colors.blockBorder, strokeWidth: 1, rx: 2, ry: 2 } }
        })
        
        // block 标题
        graph.addNode({
          id: `block-title-${layerIndex}-${bIdx}`,
          x: blockX, y: blockY, width: blockW, height: cfg.blockTitleH, zIndex: 2,
          markup: [{ tagName: 'rect', selector: 'body' }, { tagName: 'text', selector: 'label' }],
          attrs: {
            body: { fill: blockColor, stroke: 'none', rx: 2, ry: 2 },
            label: { text: block.name || '', fill: colors.blockTitleText, fontSize: 10, fontWeight: 600, refX: 0.5, refY: 0.5, textAnchor: 'middle' }
          }
        })
        
        // block 内多行节点
        let rowY = blockY + cfg.blockTitleH + cfg.blockPaddingY
        blockRows.forEach(row => {
          let nodeX = blockX + cfg.blockPaddingX
          row.nodes.forEach((node, nIdx) => {
            const nodeW = row.widths[nIdx]
            graph.addNode({
              id: node.id,
              x: nodeX, y: rowY, width: nodeW, height: cfg.nodeHeight, zIndex: 3,
              markup: [{ tagName: 'rect', selector: 'body' }, { tagName: 'text', selector: 'label' }],
              attrs: {
                body: { fill: colors.nodeBg, stroke: colors.nodeBorder, strokeWidth: 1, rx: 0, ry: 0 },
                label: { text: node.label, fill: colors.nodeText, fontSize: 10, fontWeight: 500, refX: 0.5, refY: 0.5, textAnchor: 'middle' }
              }
            })
            nodeX += nodeW + cfg.nodeGapX
          })
          rowY += cfg.nodeHeight + cfg.nodeGapY
        })
        
        blockX += blockW + cfg.blockGapX
      })
    } else {
      // === 渲染多行节点 ===
      let rowY = currentY + cfg.layerPaddingY
      nodeRows.forEach(row => {
        let nodeX = contentStartX + cfg.layerPaddingX
        row.nodes.forEach((node, idx) => {
          const nodeW = row.widths[idx]
          graph.addNode({
            id: node.id,
            x: nodeX, y: rowY, width: nodeW, height: cfg.nodeHeight, zIndex: 3,
            markup: [{ tagName: 'rect', selector: 'body' }, { tagName: 'text', selector: 'label' }],
            attrs: {
              body: { fill: colors.nodeBg, stroke: colors.nodeBorder, strokeWidth: 1, rx: 0, ry: 0 },
              label: { text: node.label, fill: colors.nodeText, fontSize: 10, fontWeight: 500, refX: 0.5, refY: 0.5, textAnchor: 'middle' }
            }
          })
          nodeX += nodeW + cfg.nodeGapX
        })
        rowY += cfg.nodeHeight + cfg.nodeGapY
      })
    }
    
    currentY += layerHeight + cfg.layerGap
  })
  
  // === 渲染右侧边栏 ===
  if (hasRight) {
    const totalMainH = currentY - startY
    const sbH = Math.max(rightSidebarH, totalMainH)
    
    graph.addNode({
      id: 'right-sidebar-bg',
      x: rightX, y: startY, width: cfg.sidebarWidth, height: sbH, zIndex: 0,
      markup: [{ tagName: 'rect', selector: 'body' }],
      attrs: { body: { fill: colors.sidebarBg, stroke: colors.sidebarBorder, strokeWidth: 1, strokeDasharray: '4,2', rx: 4, ry: 4 } }
    })
    
    // 右侧边栏标题（带背景色）
    graph.addNode({
      id: 'right-sidebar-title',
      x: rightX + 4, y: startY + 4, width: cfg.sidebarWidth - 8, height: cfg.sidebarTitleH - 4, zIndex: 1,
      markup: [{ tagName: 'rect', selector: 'body' }, { tagName: 'text', selector: 'label' }],
      attrs: { 
        body: { fill: '#FA8C16', stroke: 'none', rx: 4, ry: 4 },
        label: { text: rightSidebar?.title || '', fill: '#FFFFFF', fontSize: 10, fontWeight: 600, refX: 0.5, refY: 0.5, textAnchor: 'middle', textWrap: { width: cfg.sidebarWidth - 16, ellipsis: false } } 
      }
    })
    
    let nodeY = startY + cfg.sidebarTitleH + cfg.sidebarPadding
    rightNodes.forEach((node, i) => {
      graph.addNode({
        id: node.id,
        x: rightX + 4, y: nodeY, width: cfg.sidebarWidth - 8, height: cfg.sidebarNodeH, zIndex: 2,
        markup: [
          { tagName: 'rect', selector: 'body' },
          { tagName: 'text', selector: 'label' }
        ],
        attrs: {
          body: { fill: colors.sidebarNodeBg, stroke: 'none', rx: 4, ry: 4 },
          label: { text: node.label, fill: colors.sidebarNodeText, fontSize: 8, fontWeight: 500, refX: 0.5, refY: 0.5, textAnchor: 'middle', textWrap: { width: cfg.sidebarWidth - 16, ellipsis: true } }
        }
      })
      nodeY += cfg.sidebarNodeH + cfg.sidebarNodeGap
    })
  }
}

/**
 * 普通风格分层架构图 - 多彩、居中标题、虚线边框（支持 blocks/groups、自适应宽度）
 */
function renderLayeredDiagramNormal(data) {
  const { layers } = data
  if (!layers || !layers.length) return

  // 多彩配色
  const layerColors = [
    { border: '#42A5F5', title: '#1976D2' },
    { border: '#66BB6A', title: '#388E3C' },
    { border: '#FFCA28', title: '#F9A825' },
    { border: '#66BB6A', title: '#388E3C' },
    { border: '#EF5350', title: '#D32F2F' },
    { border: '#AB47BC', title: '#7B1FA2' },
  ]

  const cfg = {
    canvasWidth: 900,
    layerPadding: 25,
    nodeH: 40,
    nodeGapX: 12,
    nodeGapY: 8,
    layerGap: 30,
    maxRowWidth: 700,
  }
  
  // 计算节点宽度（更紧凑，与其他渲染器一致）
  const calcNodeWidth = (label) => {
    let textWidth = 0
    for (const char of (label || '')) {
      textWidth += /[\u4e00-\u9fa5]/.test(char) ? 11 : 5.5
    }
    return Math.max(55, textWidth + 12)
  }

  let currentY = 30
  let prevLayerBottomY = 0

  layers.forEach((layer, layerIndex) => {
    // 智能提取所有节点
    let allNodes = []
    const layerGroups = layer.blocks || layer.groups || []
    
    if (layer.nodes?.length > 0) {
      allNodes = [...layer.nodes]
    } else if (layerGroups.length > 0) {
      layerGroups.forEach(g => {
        (g.nodes || []).forEach(n => allNodes.push(n))
      })
    } else if (layer.systems?.length > 0) {
      layer.systems.forEach(sys => {
        (sys.subsystems || []).forEach(ss => {
          (ss.nodes || []).forEach(n => allNodes.push(n))
        })
      })
    }

    if (allNodes.length === 0) return

    // 计算节点宽度并分行
    const nodeWidths = allNodes.map(n => calcNodeWidth(n.label))
    const nodeRows = []
    let currentRow = []
    let currentRowWidths = []
    let currentRowWidth = 0
    
    allNodes.forEach((node, idx) => {
      const w = nodeWidths[idx]
      if (currentRowWidth + w + (currentRow.length > 0 ? cfg.nodeGapX : 0) > cfg.maxRowWidth && currentRow.length > 0) {
        nodeRows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
        currentRow = [node]
        currentRowWidths = [w]
        currentRowWidth = w
      } else {
        currentRow.push(node)
        currentRowWidths.push(w)
        currentRowWidth += w + (currentRow.length > 1 ? cfg.nodeGapX : 0)
      }
    })
    if (currentRow.length > 0) {
      nodeRows.push({ nodes: currentRow, widths: currentRowWidths, totalWidth: currentRowWidth })
    }

    const color = layerColors[layerIndex % layerColors.length]
    const layerContentH = Math.max(nodeRows.length, 1) * cfg.nodeH + Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY
    const layerHeight = 35 + layerContentH + 15
    const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 200
    const layerWidth = maxRowWidth + cfg.layerPadding * 2
    const layerX = (cfg.canvasWidth - layerWidth) / 2

    // 层间连线
    if (layerIndex > 0 && prevLayerBottomY > 0) {
      graph.addEdge({
        source: { x: cfg.canvasWidth / 2, y: prevLayerBottomY },
        target: { x: cfg.canvasWidth / 2, y: currentY },
        router: { name: 'normal' },
        connector: { name: 'normal' },
        attrs: {
          line: { stroke: '#BDBDBD', strokeWidth: 2, strokeDasharray: '8 4', targetMarker: { name: 'classic', size: 8 } }
        }
      })
    }

    // 层背景
    graph.addNode({
      id: `layer-${layerIndex}`,
      x: layerX,
      y: currentY,
      width: layerWidth,
      height: layerHeight,
      zIndex: 0,
      markup: [{ tagName: 'rect', selector: 'body' }],
      attrs: {
        body: { fill: 'transparent', stroke: color.border, strokeWidth: 2, strokeDasharray: '8 4', rx: 8, ry: 8 }
      }
    })

    // 层标题
    graph.addNode({
      id: `layer-title-${layerIndex}`,
      x: layerX,
      y: currentY + 5,
      width: layerWidth,
      height: 25,
      zIndex: 1,
      markup: [{ tagName: 'text', selector: 'label' }],
      attrs: {
        label: { text: layer.name || '', fill: color.title, fontSize: 14, fontWeight: 600, refX: 0.5, refY: 0.5, textAnchor: 'middle' }
      }
    })

    // 渲染多行节点
    let rowY = currentY + 35
    nodeRows.forEach(row => {
      const rowStartX = (cfg.canvasWidth - row.totalWidth) / 2
      let nodeX = rowStartX
      
      row.nodes.forEach((node, idx) => {
        const nodeW = row.widths[idx]
        graph.addNode({
          id: node.id,
          x: nodeX,
          y: rowY,
          width: nodeW,
          height: cfg.nodeH,
          zIndex: 2,
          markup: [{ tagName: 'rect', selector: 'body' }, { tagName: 'text', selector: 'label' }],
          attrs: {
            body: { fill: '#FFFFFF', stroke: '#E0E0E0', strokeWidth: 1, rx: 8, ry: 8, filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))' },
            label: { text: node.label, fill: '#424242', fontSize: 12, refX: 0.5, refY: 0.5, textAnchor: 'middle' }
          },
          data: { type: 'normal-node', ...node }
        })
        nodeX += nodeW + cfg.nodeGapX
      })
      rowY += cfg.nodeH + cfg.nodeGapY
    })

    prevLayerBottomY = currentY + layerHeight
    currentY += layerHeight + cfg.layerGap
  })
}

/**
 * 拓扑图 - 改用清晰的分层网格布局（支持主题颜色）
 */
function renderTopologyDiagram(data) {
  const { layers, edges, theme } = data
  if (!layers || !layers.length) return

  const nodeWidth = 100
  const nodeHeight = 60
  const nodeGapX = 40
  const nodeGapY = 80
  const startX = 100
  const startY = 80
  const canvasWidth = 1000
  const nodePositions = {}
  
  // 主题颜色
  const tc = themeColors[theme]
  const useTheme = tc && tc.accent

  // 按层渲染节点（无容器框，更简洁）
  layers.forEach((layer, layerIndex) => {
    const nodes = layer.nodes || []
    const fallback = defaultColors[layerIndex % defaultColors.length]
    const accentColor = layer.accentColor || fallback.accent

    // 计算本层宽度和起始X（居中）
    const totalWidth = nodes.length * nodeWidth + (nodes.length - 1) * nodeGapX
    const layerStartX = (canvasWidth - totalWidth) / 2
    const y = startY + layerIndex * (nodeHeight + nodeGapY)

    // 渲染节点
    nodes.forEach((node, nodeIndex) => {
      const x = layerStartX + nodeIndex * (nodeWidth + nodeGapX)

      nodePositions[node.id] = {
        x: x + nodeWidth / 2,
        y: y + nodeHeight / 2,
        layerIndex
      }

      // 使用主题颜色或默认颜色
      const nodeColor = useTheme ? tc.accent : accentColor
      addSimpleNode(node, x, y, nodeWidth, nodeHeight, nodeColor)
    })
  })

  // 渲染连线（使用主题颜色）
  const edgeColor = useTheme ? tc.accent : '#999'
  if (edges && edges.length > 0) {
    renderSmartEdges(edges, nodePositions, edgeColor)
  }
}

/**
 * 流程图 - 阶段分组布局（阶段内横向，阶段间垂直）
 */
function renderFlowDiagram(data) {
  const { layers } = data
  if (!layers || !layers.length) return

  const nodeWidth = 100
  const nodeHeight = 55
  const nodeGapX = 25
  const stageGapY = 100
  const canvasWidth = 900
  let currentY = 60
  
  const colors = ['#1976D2', '#388E3C', '#F57C00', '#C2185B', '#7B1FA2', '#0097A7']
  const stageIds = [] // 存储每个阶段的ID

  layers.forEach((layer, layerIndex) => {
    const nodes = layer.nodes || []
    if (nodes.length === 0) return
    
    const color = colors[layerIndex % colors.length]
    const totalWidth = nodes.length * nodeWidth + (nodes.length - 1) * nodeGapX
    const startX = (canvasWidth - totalWidth) / 2
    const centerX = canvasWidth / 2

    const stageId = `stage-bg-${layerIndex}`
    stageIds.push(stageId)
    
    // 阶段分组（可拖动，带动子节点）
    const stageNode = graph.addNode({
      id: stageId,
      x: startX - 20,
      y: currentY - 10,
      width: totalWidth + 40,
      height: nodeHeight + 50,
      zIndex: 0,
      markup: [
        { tagName: 'rect', selector: 'body' },
        { tagName: 'rect', selector: 'labelBg' },
        { tagName: 'text', selector: 'label' }
      ],
      attrs: {
        body: {
          fill: color,
          fillOpacity: 0.08,
          stroke: color,
          strokeOpacity: 0.4,
          strokeWidth: 2,
          rx: 12,
          ry: 12,
          cursor: 'move'
        },
        labelBg: {
          fill: '#fff',
          refWidth: '100%',
          height: 20,
          refX: 0,
          y: 2,
          rx: 0,
          ry: 0
        },
        label: {
          text: layer.name || `阶段${layerIndex + 1}`,
          fill: color,
          fontSize: 12,
          fontWeight: 600,
          refX: 0.5,
          refY: 12,
          textAnchor: 'middle'
        }
      }
    })

    // 节点布局（支持多行）
    const maxNodesPerRow = 5
    const nodeRowGap = 15
    let prevNodeId = null
    let prevRowLastId = null
    
    nodes.forEach((node, nodeIndex) => {
      const row = Math.floor(nodeIndex / maxNodesPerRow)
      const col = nodeIndex % maxNodesPerRow
      const nodesInThisRow = Math.min(maxNodesPerRow, nodes.length - row * maxNodesPerRow)
      const rowWidth = nodesInThisRow * nodeWidth + (nodesInThisRow - 1) * nodeGapX
      const rowStartX = (canvasWidth - rowWidth) / 2
      
      const x = rowStartX + col * (nodeWidth + nodeGapX)
      const y = currentY + 25 + row * (nodeHeight + nodeRowGap)
      const iconInfo = getIconInfo(node.icon)
      const nodeId = node.id || `node-${layerIndex}-${nodeIndex}`
      
      graph.addNode({
        id: nodeId,
        x,
        y,
        width: nodeWidth,
        height: nodeHeight,
        zIndex: 2,
        parent: stageId,
        markup: [
          { tagName: 'rect', selector: 'body' },
          { tagName: 'text', selector: 'icon' },
          { tagName: 'text', selector: 'label' }
        ],
        attrs: {
          body: {
            fill: '#FFFFFF',
            stroke: color,
            cursor: 'move',
            strokeWidth: 2,
            rx: 8,
            ry: 8
          },
          icon: {
            text: iconInfo.icon,
            fontSize: 18,
            refX: 0.5,
            refY: 0.35,
            textAnchor: 'middle'
          },
          label: {
            text: node.label,
            fill: '#333',
            fontSize: 10,
            fontWeight: 500,
            refX: 0.5,
            refY: 0.75,
            textAnchor: 'middle'
          }
        }
      })

      // 同行内横向连线
      if (col > 0 && prevNodeId) {
        graph.addEdge({
          source: prevNodeId,
          target: nodeId,
          connector: { name: 'normal' },
          attrs: {
            line: { stroke: color, strokeWidth: 1.5, targetMarker: { name: 'classic', size: 6 } }
          },
          zIndex: 1
        })
      }
      
      // 换行连线（上一行末尾 → 下一行开头）
      if (col === 0 && row > 0 && prevRowLastId) {
        graph.addEdge({
          source: prevRowLastId,
          target: nodeId,
          connector: { name: 'rounded' },
          router: { name: 'er', args: { direction: 'V' } },
          attrs: {
            line: { stroke: color, strokeWidth: 1.5, strokeDasharray: '4,2', targetMarker: { name: 'classic', size: 6 } }
          },
          zIndex: 1
        })
      }
      
      prevNodeId = nodeId
      if (col === nodesInThisRow - 1) prevRowLastId = nodeId
    })

    // 计算实际阶段高度
    const rowCount = Math.ceil(nodes.length / maxNodesPerRow)
    const actualStageHeight = rowCount * nodeHeight + (rowCount - 1) * nodeRowGap + 50
    stageNode.resize(totalWidth + 40, actualStageHeight)
    
    currentY += actualStageHeight + stageGapY - 50
  })

  // 阶段间垂直连线（连接到阶段节点，支持拖动更新）
  for (let i = 0; i < stageIds.length - 1; i++) {
    graph.addEdge({
      id: `stage-edge-${i}`,
      source: { cell: stageIds[i], anchor: 'bottom' },
      target: { cell: stageIds[i + 1], anchor: 'top' },
      connector: { name: 'normal' },
      router: { name: 'normal' },
      attrs: {
        line: {
          stroke: '#999',
          strokeWidth: 2,
          strokeDasharray: '6,4',
          targetMarker: { name: 'classic', size: 8, fill: '#999' }
        }
      },
      zIndex: 1
    })
  }
}

/**
 * 添加简洁节点（分层图用）
 */
function addSimpleNode(node, x, y, width, height, accentColor) {
  const iconInfo = getIconInfo(node.icon)
  
  graph.addNode({
    id: node.id,
    x,
    y,
    width,
    height,
    zIndex: 2,
    markup: [
      { tagName: 'rect', selector: 'body' },
      { tagName: 'text', selector: 'icon' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      body: {
        fill: '#FFFFFF',
        stroke: accentColor,
        strokeWidth: 2,
        rx: 8,
        ry: 8
      },
      icon: {
        text: iconInfo.icon,
        fontSize: 20,
        refX: 0.5,
        refY: 0.35,
        textAnchor: 'middle'
      },
      label: {
        text: node.label,
        fill: '#333',
        fontSize: 11,
        fontWeight: 500,
        refX: 0.5,
        refY: 0.75,
        textAnchor: 'middle'
      }
    },
    data: { type: 'node', ...node }
  })
}

/**
 * 添加流程节点（流程图用）
 */
function addFlowNode(node, x, y, width, height, accentColor) {
  const iconInfo = getIconInfo(node.icon)
  
  graph.addNode({
    id: node.id,
    x,
    y,
    width,
    height,
    zIndex: 2,
    markup: [
      { tagName: 'rect', selector: 'body' },
      { tagName: 'text', selector: 'icon' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      body: {
        fill: accentColor,
        fillOpacity: 0.1,
        stroke: accentColor,
        strokeWidth: 2,
        rx: height / 2,
        ry: height / 2
      },
      icon: {
        text: iconInfo.icon,
        fontSize: 16,
        refX: 0.15,
        refY: 0.5,
        textAnchor: 'middle'
      },
      label: {
        text: node.label,
        fill: '#333',
        fontSize: 12,
        fontWeight: 500,
        refX: 0.58,
        refY: 0.5,
        textAnchor: 'middle'
      }
    },
    data: { type: 'node', ...node }
  })
}

/**
 * 智能连线（分层图用）- 简洁的垂直连线
 */
function renderSmartEdges(edges, nodePositions, edgeColor = '#9CA3AF') {
  edges.forEach((edge, index) => {
    const source = nodePositions[edge.source]
    const target = nodePositions[edge.target]
    if (!source || !target) return

    // 判断是上下层还是同层
    const isDownward = target.layerIndex > source.layerIndex
    const isUpward = target.layerIndex < source.layerIndex
    const isSameLayer = source.layerIndex === target.layerIndex
    
    // 简单的正交连线，从底部出发到顶部
    graph.addEdge({
      id: `edge-${index}`,
      source: { 
        cell: edge.source, 
        anchor: isDownward ? 'bottom' : (isUpward ? 'top' : 'right')
      },
      target: { 
        cell: edge.target, 
        anchor: isDownward ? 'top' : (isUpward ? 'bottom' : 'left')
      },
      // 简单正交连线，不要复杂绕行
      router: { name: 'orth', args: { padding: 10 } },
      connector: { name: 'rounded', args: { radius: 4 } },
      attrs: {
        line: {
          stroke: edgeColor,
          strokeWidth: 1.5,
          targetMarker: { name: 'classic', size: 5, fill: edgeColor }
        }
      },
      labels: edge.label ? [{
        attrs: {
          label: { text: edge.label, fill: '#6B7280', fontSize: 9 },
          rect: { fill: '#fff', rx: 2, ry: 2 }
        },
        position: { distance: 0.5, offset: 15 }
      }] : [],
      zIndex: 0
    })
  })
}

/**
 * 曲线连线（拓扑图用）- 简洁的正交连线
 */
function renderCurvedEdges(edges, nodePositions, edgeColor = '#9CA3AF') {
  edges.forEach((edge, index) => {
    const source = nodePositions[edge.source]
    const target = nodePositions[edge.target]
    if (!source || !target) return

    const isDownward = target.layerIndex > source.layerIndex
    const isUpward = target.layerIndex < source.layerIndex

    graph.addEdge({
      id: `edge-${index}`,
      source: { 
        cell: edge.source, 
        anchor: isDownward ? 'bottom' : (isUpward ? 'top' : 'right')
      },
      target: { 
        cell: edge.target, 
        anchor: isDownward ? 'top' : (isUpward ? 'bottom' : 'left')
      },
      router: { name: 'orth', args: { padding: 10 } },
      connector: { name: 'rounded', args: { radius: 4 } },
      attrs: {
        line: {
          stroke: edgeColor,
          strokeWidth: 1.5,
          targetMarker: { name: 'classic', size: 5, fill: edgeColor }
        }
      },
      labels: edge.label ? [{
        attrs: {
          label: { text: edge.label, fill: '#6B7280', fontSize: 9 },
          rect: { fill: '#fff', rx: 2, ry: 2 }
        },
        position: { distance: 0.5, offset: 15 }
      }] : [],
      zIndex: 0
    })
  })
}

/**
 * 流程连线（流程图用）- 简洁的正交连线
 */
function renderFlowEdges(edges, nodePositions, edgeColor = '#3B82F6') {
  edges.forEach((edge, index) => {
    const source = nodePositions[edge.source]
    const target = nodePositions[edge.target]
    if (!source || !target) return

    const isDownward = target.layerIndex > source.layerIndex
    const isUpward = target.layerIndex < source.layerIndex

    graph.addEdge({
      id: `edge-${index}`,
      source: { 
        cell: edge.source, 
        anchor: isDownward ? 'bottom' : (isUpward ? 'top' : 'right')
      },
      target: { 
        cell: edge.target, 
        anchor: isDownward ? 'top' : (isUpward ? 'bottom' : 'left')
      },
      router: { name: 'orth', args: { padding: 10 } },
      connector: { name: 'rounded', args: { radius: 4 } },
      attrs: {
        line: {
          stroke: edgeColor,
          strokeWidth: 1.5,
          targetMarker: { name: 'classic', size: 5, fill: edgeColor }
        }
      },
      labels: edge.label ? [{
        attrs: {
          label: { text: edge.label, fill: edgeColor, fontSize: 9, fontWeight: 500 },
          rect: { fill: '#fff', rx: 2, ry: 2 }
        },
        position: { distance: 0.5, offset: 15 }
      }] : [],
      zIndex: 0
    })
  })
}

/**
 * 放大
 */
function zoomIn() {
  if (graph) {
    graph.zoom(0.1)
  }
}

/**
 * 缩小
 */
function zoomOut() {
  if (graph) {
    graph.zoom(-0.1)
  }
}

/**
 * 适应画布
 */
function fitView() {
  if (graph) {
    graph.zoomToFit({ padding: 40 })
  }
}

/**
 * 导出PNG
 */
async function exportPNG() {
  if (graph) {
    graph.exportPNG('architecture-diagram', {
      backgroundColor: '#fff',
      padding: 20,
      quality: 1
    })
  }
}

/**
 * 导出SVG
 */
async function exportSVG() {
  if (graph) {
    graph.exportSVG('architecture-diagram', {
      preserveDimensions: true
    })
  }
}

/**
 * 撤销
 */
function undo() {
  if (graph) {
    graph.undo()
  }
}

/**
 * 重做
 */
function redo() {
  if (graph) {
    graph.redo()
  }
}

/**
 * 删除选中
 */
function deleteSelected() {
  if (graph) {
    const cells = graph.getSelectedCells()
    if (cells.length) {
      graph.removeCells(cells)
    }
  }
}

/**
 * 添加节点（专业模式拖拽用）
 */
function addNode(nodeData) {
  if (!graph) return
  
  const iconInfo = getIconInfo(nodeData.icon)
  const width = 100
  const height = 60
  
  graph.addNode({
    id: nodeData.id,
    x: nodeData.x - width / 2,
    y: nodeData.y - height / 2,
    width,
    height,
    zIndex: 2,
    markup: [
      { tagName: 'rect', selector: 'body' },
      { tagName: 'text', selector: 'icon' },
      { tagName: 'text', selector: 'label' }
    ],
    attrs: {
      body: {
        fill: '#FFFFFF',
        stroke: '#3B82F6',
        strokeWidth: 1.5,
        rx: 6,
        ry: 6,
        filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
      },
      icon: {
        text: iconInfo.icon,
        fontSize: 20,
        refX: 0.5,
        refY: 0.35,
        textAnchor: 'middle'
      },
      label: {
        text: nodeData.label,
        fill: '#333',
        fontSize: 11,
        fontWeight: 500,
        refX: 0.5,
        refY: 0.75,
        textAnchor: 'middle'
      }
    },
    // 添加连接桩用于连线
    ports: {
      groups: {
        default: {
          markup: [{ tagName: 'circle', selector: 'portBody' }],
          attrs: {
            portBody: {
              r: 4,
              fill: '#3B82F6',
              stroke: '#fff',
              strokeWidth: 1,
              magnet: true
            }
          }
        }
      },
      items: [
        { id: 'top', group: 'default', args: { x: '50%', y: 0 } },
        { id: 'bottom', group: 'default', args: { x: '50%', y: '100%' } },
        { id: 'left', group: 'default', args: { x: 0, y: '50%' } },
        { id: 'right', group: 'default', args: { x: '100%', y: '50%' } }
      ]
    },
    data: { type: 'node', ...nodeData }
  })
}

/**
 * 更新节点图标
 */
function updateNodeIcon(nodeId, iconType) {
  if (!graph) return
  
  const node = graph.getCellById(nodeId)
  if (node) {
    const iconInfo = getIconInfo(iconType)
    node.attr('icon/text', iconInfo.icon)
  }
}

// 暴露方法
defineExpose({
  zoomIn,
  zoomOut,
  fitView,
  exportPNG,
  exportSVG,
  undo,
  redo,
  deleteSelected,
  addNode,
  updateNodeIcon,
  getGraph: () => graph
})
</script>

<style scoped>
.diagram-canvas {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
