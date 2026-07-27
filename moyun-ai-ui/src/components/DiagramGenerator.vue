<template>
  <div class="diagram-generator">
    <!-- 左侧面板 -->
    <div class="left-panel">
      <!-- Logo 标题 -->
      <div class="logo-header">
        <i class="fa-solid fa-diagram-project"></i>
        <span>AI 架构图生成器</span>
      </div>

      <!-- AI 生成区域 -->
      <div class="ai-section">
        <div class="section-title">
          <i class="fa-solid fa-wand-magic-sparkles"></i>
          <span>AI 智能生成</span>
        </div>
        
        <!-- 风格选择 -->
        <div class="style-selector">
          <span class="style-label">图表风格</span>
          <el-radio-group v-model="diagramStyle" size="small">
            <el-radio-button label="normal">
              <i class="fa-solid fa-palette"></i> 普通
            </el-radio-button>
            <el-radio-button label="enterprise">
              <i class="fa-solid fa-building"></i> 企业级
            </el-radio-button>
          </el-radio-group>
        </div>
        
        <el-input
          v-model="inputContent"
          type="textarea"
          :rows="10"
          placeholder="描述您的架构，例如：&#10;• 电商系统架构&#10;• 微服务调用关系&#10;• Dify AI 平台架构&#10;&#10;支持详细描述各个模块和流程..."
          resize="vertical"
          @input="handleInputChange"
        />
        
        <!-- 智能分析提示 -->
        <div class="smart-analysis" v-if="smartAnalysis && inputContent.trim().length > 3">
          <div class="analysis-header">
            <i class="fa-solid fa-brain"></i>
            <span>智能分析</span>
          </div>
          <div class="analysis-content">
            <div class="analysis-item">
              <span class="analysis-label">系统类型:</span>
              <el-tag size="small" :type="getSystemTypeTag(smartAnalysis.systemType)">
                {{ getSystemTypeName(smartAnalysis.systemType) }}
              </el-tag>
            </div>
            <div class="analysis-item">
              <span class="analysis-label">复杂度:</span>
              <el-tag size="small" :type="smartAnalysis.complexity === 'detailed' ? 'warning' : 'info'">
                {{ smartAnalysis.complexity === 'detailed' ? '详细' : smartAnalysis.complexity === 'simple' ? '简洁' : '标准' }}
              </el-tag>
            </div>
            <div class="analysis-item" v-if="smartAnalysis.needLeftSidebar || smartAnalysis.needRightSidebar">
              <span class="analysis-label">侧边栏:</span>
              <el-tag size="small" type="success" v-if="smartAnalysis.needLeftSidebar">监控</el-tag>
              <el-tag size="small" type="success" v-if="smartAnalysis.needRightSidebar">治理</el-tag>
            </div>
          </div>
        </div>
        
        <el-button 
          type="primary" 
          :loading="generating"
          @click="handleGenerate"
          class="generate-btn"
        >
          <i class="fa-solid fa-wand-magic-sparkles"></i>
          {{ generating ? '生成中...' : '生成架构图' }}
        </el-button>
      </div>
    </div>

    <!-- 右侧编辑器区域 -->
    <div class="right-panel">
      <!-- 顶部工具栏 -->
      <div class="right-toolbar">
        <div class="toolbar-title">
          <i class="fa-solid fa-diagram-project"></i>
          <span>Draw.io 编辑器</span>
        </div>
      </div>

      <!-- 编辑器内容 -->
      <div class="editor-content">
        <!-- Draw.io 编辑器 -->
        <iframe 
          ref="drawioFrame"
          class="drawio-frame"
          :src="drawioUrl"
          frameborder="0"
        ></iframe>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { generateDiagram, analyzeUserInput } from '@/api/diagram'

// 引入公共工具模块
import { validateAndFixDiagramData, truncateText, escapeXml } from '@/utils/diagram'
import { logger } from '@/utils/logger'

// 引入常量配置
import { STYLE_MAP, DEFAULT_STYLE, EDGE_TYPES, LEGEND_CONFIG } from '@/constants/diagram'

// 状态
const inputContent = ref('')
const generating = ref(false)
const smartAnalysis = ref(null)  // 智能分析结果

// 系统类型名称映射
const SYSTEM_TYPE_NAMES = {
  agent: 'AI 智能体平台',
  ai: 'AI/LLM 系统',
  ecommerce: '电商系统',
  microservice: '微服务架构',
  iot: '物联网系统',
  bigdata: '大数据系统',
  cms: '内容管理系统',
  erp: '企业管理系统',
  gaming: '游戏系统',
  general: '通用架构'
}

// 系统类型标签颜色
const getSystemTypeTag = (type) => {
  const tagTypes = {
    agent: 'danger',
    ai: 'danger',
    ecommerce: 'success',
    microservice: 'warning',
    iot: 'info',
    bigdata: '',
    general: 'info'
  }
  return tagTypes[type] || 'info'
}

const getSystemTypeName = (type) => SYSTEM_TYPE_NAMES[type] || '通用架构'

// 防抖处理输入分析
let inputDebounceTimer = null
const handleInputChange = () => {
  if (inputDebounceTimer) clearTimeout(inputDebounceTimer)
  inputDebounceTimer = setTimeout(() => {
    if (inputContent.value.trim().length > 3) {
      smartAnalysis.value = analyzeUserInput(inputContent.value)
    } else {
      smartAnalysis.value = null
    }
  }, 300)
}
const diagramStyle = ref('normal')  // normal | enterprise
const drawioFrame = ref(null)
const diagramData = ref(null)
const drawioReady = ref(false)
const lastGeneratedXml = ref('')  // 缓存最后生成的 XML

// Draw.io 嵌入 URL（完整界面）
const drawioUrl = 'https://embed.diagrams.net/?embed=1&proto=json&spin=1&ui=atlas&libraries=1'

// Draw.io 安全域名白名单
const DRAWIO_ALLOWED_ORIGINS = ['https://embed.diagrams.net', 'https://app.diagrams.net']

// 监听 draw.io 消息（安全增强版）
const handleDrawioMessage = (event) => {
  // 严格校验来源域名
  const isAllowed = DRAWIO_ALLOWED_ORIGINS.some(origin => event.origin.startsWith(origin))
  if (!isAllowed) return
  
  try {
    const msg = JSON.parse(event.data)
    
    if (msg.event === 'init') {
      drawioReady.value = true
      // 如果已有数据，加载
      if (lastGeneratedXml.value) {
        sendToDrawio({
          action: 'load',
          autosave: 0,
          xml: lastGeneratedXml.value
        })
      } else {
        sendToDrawio({
          action: 'load',
          xml: getEmptyDiagram()
        })
      }
    }
    
    if (msg.event === 'load') {
      // 图表加载完成
    }
    
    if (msg.event === 'save') {
      ElMessage.success('保存成功')
    }
    
    if (msg.event === 'export') {
      // 处理导出
      downloadFile(msg.data, msg.format)
    }
  } catch (e) {
    // 非 JSON 消息忽略
  }
}

// 发送消息到 draw.io（安全版本）
const sendToDrawio = (msg) => {
  if (drawioFrame.value?.contentWindow) {
    // 使用明确的目标域名，避免消息泄露
    drawioFrame.value.contentWindow.postMessage(JSON.stringify(msg), 'https://embed.diagrams.net')
  }
}

// 获取空白图表
const getEmptyDiagram = () => {
  return `<mxGraphModel>
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
    </root>
  </mxGraphModel>`
}

// 注意：数据校验函数已移至 @/utils/diagram.js 的 validateAndFixDiagramData

// AI 生成架构图
// 重试次数
const MAX_RETRY = 2

const handleGenerate = async () => {
  if (!inputContent.value.trim()) {
    ElMessage.warning('请输入架构描述')
    return
  }
  
  generating.value = true
  let retryCount = 0
  let lastError = null
  
  while (retryCount <= MAX_RETRY) {
    try {
      if (retryCount > 0) {
        logger.log(`🔄 第 ${retryCount} 次重试...`)
        ElMessage.info(`正在重试 (${retryCount}/${MAX_RETRY})...`)
      }
      
      const res = await generateDiagram(inputContent.value, diagramStyle.value)
      
      if (res.code === 200 && res.data) {
        // 使用公共模块的数据校验函数
        const fixedData = validateAndFixDiagramData(res.data)
        logger.log('AI 返回数据:', res.data)
        logger.log('修复后数据:', fixedData)
        
        // 检查数据有效性
        if (!fixedData.layers || fixedData.layers.length === 0) {
          throw new Error('生成的架构图数据无效')
        }
        
        // 保存数据和 XML
        diagramData.value = fixedData
        lastGeneratedXml.value = convertToDrawioXml(fixedData)
        
        // 发送到 Draw.io 编辑器
        sendToDrawio({
          action: 'load',
          autosave: 0,
          xml: lastGeneratedXml.value
        })
        
        ElMessage.success('架构图生成成功')
        generating.value = false
        return // 成功，退出
      } else {
        lastError = res.message || '生成失败'
        throw new Error(lastError)
      }
    } catch (error) {
      logger.error(`生成失败 (尝试 ${retryCount + 1}):`, error)
      lastError = error.message || '生成失败'
      retryCount++
      
      if (retryCount <= MAX_RETRY) {
        // 等待 1 秒后重试
        await new Promise(resolve => setTimeout(resolve, 1000))
      }
    }
  }
  
  // 所有重试都失败
  generating.value = false
  ElMessage.error(lastError || '生成失败，请稍后重试')
}

// 主题颜色配置（用于 Draw.io 渲染）
const themeColors = {
  gray: { accent: '#6B7280', bg: '#F3F4F6', border: '#9CA3AF', text: '#374151' },
  blue: { accent: '#3B82F6', bg: '#EFF6FF', border: '#60A5FA', text: '#1E40AF' },
  green: { accent: '#10B981', bg: '#ECFDF5', border: '#34D399', text: '#065F46' },
  colorful: null,  // 使用默认多彩
  default: null    // 使用图标默认颜色
}

// 获取节点颜色（根据主题）
const getNodeColor = (theme, iconName) => {
  if (theme && themeColors[theme] && themeColors[theme].accent) {
    return themeColors[theme]
  }
  // 默认使用图标颜色（使用导入的 STYLE_MAP）
  const st = STYLE_MAP[iconName] || DEFAULT_STYLE
  return { accent: st.color, bg: '#FFFFFF', border: st.color, text: '#333333' }
}

// ========== 生成节点 XML（支持 tooltip） ==========
const generateNodeXml = (node, x, y, width, height, style) => {
  // 如果节点被截断，添加 tooltip 显示完整内容
  const tooltip = node.truncated && node.fullLabel 
    ? `tooltip="${escapeXml(node.fullLabel)}"` 
    : ''
  
  return `<mxCell id="${node.id}" value="${escapeXml(node.label)}" ${tooltip} style="${style}" vertex="1" parent="1">
    <mxGeometry x="${x}" y="${y}" width="${width}" height="${height}" as="geometry"/>
  </mxCell>`
}

// ========== 渲染 Edges（数据流箭头） ==========
// 注意：Draw.io 的自动布局对复杂连线支持有限
// 为避免视觉混乱，Draw.io 模式暂时简化 edges 渲染
const renderEdgesToXml = (edges, nodePositions, enableEdges = false) => {
  // Draw.io 模式下，edges 渲染容易导致视觉混乱
  // 设置 enableEdges = false 可禁用（默认禁用）
  if (!enableEdges || !edges || edges.length === 0) return ''
  
  let edgesXml = ''
  let edgeId = 1
  
  // 只渲染前 5 条最重要的 edges，避免过于复杂
  const limitedEdges = edges.slice(0, 5)
  
  limitedEdges.forEach(edge => {
    const edgeType = EDGE_TYPES[edge.type] || EDGE_TYPES.sync
    const dashPattern = edge.type === 'async' ? 'dashed=1;dashPattern=8 4;' : edge.type === 'event' ? 'dashed=1;dashPattern=2 2;' : ''
    
    // 使用更简洁的直线样式，减少视觉干扰
    edgesXml += `<mxCell id="edge-${edgeId++}" value="${escapeXml(edge.label || '')}" style="edgeStyle=none;curved=1;rounded=1;html=1;strokeColor=${edgeType.color};strokeWidth=1.5;${dashPattern}endArrow=classic;endFill=1;fontSize=9;fontColor=#666;exitX=0.5;exitY=1;entryX=0.5;entryY=0;" edge="1" parent="1" source="${edge.source}" target="${edge.target}">
      <mxGeometry relative="1" as="geometry"/>
    </mxCell>`
  })
  
  return edgesXml
}

// ========== 渲染图例 (Legend) ==========
const renderLegendToXml = (x, y, width = 160) => {
  if (!LEGEND_CONFIG.enabled) return ''
  
  let legendXml = ''
  const hasDescription = LEGEND_CONFIG.showDescription && LEGEND_CONFIG.description
  const descHeight = hasDescription ? 24 : 0
  const legendH = 22 + LEGEND_CONFIG.items.length * 22 + descHeight
  
  // 图例背景
  legendXml += `<mxCell id="legend-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#E0E0E0;strokeWidth=1;shadow=1;opacity=95;" vertex="1" parent="1">
    <mxGeometry x="${x}" y="${y}" width="${width}" height="${legendH}" as="geometry"/>
  </mxCell>`
  
  // 图例标题
  legendXml += `<mxCell id="legend-title" value="图例说明" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=#333;" vertex="1" parent="1">
    <mxGeometry x="${x}" y="${y + 2}" width="${width}" height="18" as="geometry"/>
  </mxCell>`
  
  // 图例项
  let itemY = y + 22
  LEGEND_CONFIG.items.forEach((item, idx) => {
    const dashPattern = item.style === 'dashed' ? 'dashed=1;dashPattern=8 4;' : item.style === 'dotted' ? 'dashed=1;dashPattern=2 2;' : ''
    
    // 线条示意
    legendXml += `<mxCell id="legend-line-${idx}" value="" style="endArrow=classic;html=1;strokeColor=${item.color};strokeWidth=2;${dashPattern}" edge="1" parent="1">
      <mxGeometry relative="1" as="geometry">
        <mxPoint x="${x + 10}" y="${itemY + 10}" as="sourcePoint"/>
        <mxPoint x="${x + 45}" y="${itemY + 10}" as="targetPoint"/>
      </mxGeometry>
    </mxCell>`
    
    // 标签
    legendXml += `<mxCell id="legend-label-${idx}" value="${escapeXml(item.label)}" style="text;html=1;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;fontSize=10;fontColor=#666;" vertex="1" parent="1">
      <mxGeometry x="${x + 50}" y="${itemY}" width="${width - 60}" height="20" as="geometry"/>
    </mxCell>`
    
    itemY += 22
  })
  
  // 描述文字
  if (hasDescription) {
    legendXml += `<mxCell id="legend-desc" value="${escapeXml(LEGEND_CONFIG.description)}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=9;fontColor=#999;fontStyle=2;" vertex="1" parent="1">
      <mxGeometry x="${x}" y="${itemY}" width="${width}" height="20" as="geometry"/>
    </mxCell>`
  }
  
  return legendXml
}

// 将 AI 生成的 JSON 转换为 draw.io XML（智能化）
const convertToDrawioXml = (data) => {
  const style = diagramStyle.value
  const type = data.type || 'layered'
  
  logger.log('=== 渲染图表 ===')
  logger.log('数据类型:', type)
  logger.log('选择风格:', style)
  
  if (style === 'normal') {
    logger.log('>>> 普通风格')
    return convertLayeredToDrawioNormal(data)
  } else {
    logger.log('>>> 企业级风格')
    
    // enterprise-full 类型：检查是否有专用组件（topUsers/bottomInfra）
    if (type === 'enterprise-full' && (data.topUsers || data.bottomInfra)) {
      logger.log('>>> 使用全景企业架构渲染器（有 topUsers/bottomInfra）')
      return convertEnterpriseFullToDrawio(data)
    }
    
    // 检测侧边栏（兼容两种格式）
    const hasSidebar = data.leftSidebar || data.rightSidebar || data.leftMonitor || data.rightSidebars?.length > 0
    if (type === 'layered-sidebar' || type === 'enterprise-full' || hasSidebar) {
      logger.log('>>> 使用侧边栏布局')
      return convertLayeredSidebarToDrawio(data)
    }
    
    // 默认使用简单分层布局
    return convertLayeredToDrawio(data)
  }
}

// 流程图布局 - 阶段分组（支持自动换行）
const convertFlowToDrawio = (data) => {
  const { layers } = data
  if (!layers || !layers.length) return ''
  
  const nodeW = 100
  const nodeH = 50
  const nodeGapX = 20
  const nodeGapY = 15  // 行间距
  const stageGapY = 90
  const canvasWidth = 1000
  const maxNodesPerRow = 6  // 每行最多节点数
  let currentY = 50
  
  const colors = ['#1976D2', '#388E3C', '#F57C00', '#C2185B', '#7B1FA2', '#0097A7']
  let cellsXml = ''
  const stageInfo = []
  let edgeId = 0

  layers.forEach((layer, layerIndex) => {
    const nodes = layer.nodes || []
    if (nodes.length === 0) return
    
    const color = colors[layerIndex % colors.length]
    
    // 计算行数和布局
    const rowCount = Math.ceil(nodes.length / maxNodesPerRow)
    const stageHeight = rowCount * nodeH + (rowCount - 1) * nodeGapY + 40
    
    // 阶段背景框宽度取最宽行
    const nodesInFirstRow = Math.min(nodes.length, maxNodesPerRow)
    const maxRowWidth = nodesInFirstRow * nodeW + (nodesInFirstRow - 1) * nodeGapX
    const startX = (canvasWidth - maxRowWidth) / 2
    
    stageInfo.push({
      id: `stage-bg-${layerIndex}`,
      centerX: canvasWidth / 2,
      topY: currentY - 5,
      bottomY: currentY + stageHeight - 5
    })

    // 阶段背景框
    cellsXml += `
      <mxCell id="stage-bg-${layerIndex}" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${color};fillOpacity=6;strokeColor=${color};strokeOpacity=50;strokeWidth=2;arcSize=12;" vertex="1" parent="1">
        <mxGeometry x="${startX - 15}" y="${currentY - 5}" width="${maxRowWidth + 30}" height="${stageHeight}" as="geometry" />
      </mxCell>`

    // 阶段标题
    cellsXml += `
      <mxCell id="stage-title-${layerIndex}" value="&lt;b&gt;${escapeXml(layer.name || '')}&lt;/b&gt;" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=12;fontColor=${color};labelBackgroundColor=#FFFFFF;" vertex="1" parent="1">
        <mxGeometry x="${startX - 15}" y="${currentY - 2}" width="${maxRowWidth + 30}" height="22" as="geometry" />
      </mxCell>`

    // 节点布局（支持多行）
    let prevNodeId = null
    let prevRowLastId = null
    nodes.forEach((node, nodeIndex) => {
      const row = Math.floor(nodeIndex / maxNodesPerRow)
      const col = nodeIndex % maxNodesPerRow
      const nodesInThisRow = Math.min(maxNodesPerRow, nodes.length - row * maxNodesPerRow)
      const rowWidth = nodesInThisRow * nodeW + (nodesInThisRow - 1) * nodeGapX
      const rowStartX = (canvasWidth - rowWidth) / 2
      
      const x = rowStartX + col * (nodeW + nodeGapX)
      const y = currentY + 20 + row * (nodeH + nodeGapY)
      const st = STYLE_MAP[node.icon] || DEFAULT_STYLE
      const nodeId = `node-${layerIndex}-${nodeIndex}`
      
      cellsXml += `
        <mxCell id="${nodeId}" value="${st.icon} ${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fontSize=10;fillColor=#FFFFFF;strokeColor=${st.color};strokeWidth=2;" vertex="1" parent="1">
          <mxGeometry x="${x}" y="${y}" width="${nodeW}" height="${nodeH}" as="geometry" />
        </mxCell>`

      // 同行内横向连线
      if (col > 0 && prevNodeId) {
        cellsXml += `
          <mxCell id="edge-h-${edgeId++}" style="edgeStyle=none;html=1;strokeColor=${color};strokeWidth=1.5;endArrow=classic;endFill=1;" edge="1" parent="1" source="${prevNodeId}" target="${nodeId}">
            <mxGeometry relative="1" as="geometry" />
          </mxCell>`
      }
      
      // 换行时，上一行最后一个连到这一行第一个
      if (col === 0 && row > 0 && prevRowLastId) {
        cellsXml += `
          <mxCell id="edge-wrap-${edgeId++}" style="edgeStyle=orthogonalEdgeStyle;rounded=1;html=1;strokeColor=${color};strokeWidth=1.5;endArrow=classic;dashed=1;" edge="1" parent="1" source="${prevRowLastId}" target="${nodeId}">
            <mxGeometry relative="1" as="geometry" />
          </mxCell>`
      }
      
      prevNodeId = nodeId
      if (col === nodesInThisRow - 1) prevRowLastId = nodeId
    })

    currentY += stageHeight + stageGapY
  })

  // 阶段间垂直连线（居中虚线，连接上下阶段）
  for (let i = 0; i < stageInfo.length - 1; i++) {
    const from = stageInfo[i]
    const to = stageInfo[i + 1]
    
    cellsXml += `
      <mxCell id="stage-edge-${i}" style="edgeStyle=orthogonalEdgeStyle;orthogonalLoop=1;jettySize=auto;html=1;strokeColor=#888888;strokeWidth=2;dashed=1;dashPattern=8 4;endArrow=blockThin;endFill=1;rounded=1;" edge="1" parent="1" source="${from.id}" target="${to.id}">
        <mxGeometry relative="1" as="geometry" />
      </mxCell>`
  }
  
  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${canvasWidth}" pageHeight="${currentY + 50}" math="0" shadow="0">
  <root>
    <mxCell id="0" />
    <mxCell id="1" parent="0" />
    ${cellsXml}
  </root>
</mxGraphModel>`
  
  return xml
}

// 组织架构图布局 - 树形结构
const convertOrgToDrawio = (data) => {
  const { layers, title } = data
  if (!layers || !layers.length) return ''
  
  // 收集所有节点并建立父子关系
  const allNodes = []
  const nodeMap = {}
  
  layers.forEach(layer => {
    (layer.nodes || []).forEach(node => {
      allNodes.push(node)
      nodeMap[node.id] = node
    })
  })
  
  // 构建树形结构
  const roots = allNodes.filter(n => !n.parent)
  
  // 布局参数
  const nodeW = 90
  const nodeH = 45
  const minNodeSpace = 100  // 每个叶子节点最小占用空间
  const gapY = 70
  let cellsXml = ''
  
  // 计算每个节点的子节点数量（递归）
  const getDescendantCount = (nodeId) => {
    const children = allNodes.filter(n => n.parent === nodeId)
    if (children.length === 0) return 1
    return children.reduce((sum, c) => sum + getDescendantCount(c.id), 0)
  }
  
  // 计算画布宽度
  const totalLeaves = roots.reduce((sum, r) => sum + getDescendantCount(r.id), 0)
  const canvasWidth = Math.max(1600, totalLeaves * minNodeSpace)
  
  // 递归布局节点
  const layoutNode = (node, x, y, availableWidth) => {
    const st = STYLE_MAP[node.icon] || DEFAULT_STYLE
    const nodeX = x + availableWidth / 2 - nodeW / 2
    
    // 添加节点 - Draw.io 使用渐变填充和阴影效果
    const displayLabel = truncateText(node.label, 6)
    cellsXml += `
      <mxCell id="${node.id}" value="${st.icon} ${escapeXml(displayLabel)}" style="rounded=1;whiteSpace=wrap;html=1;fontSize=10;fillColor=#FFFFFF;strokeColor=${st.color};strokeWidth=2;shadow=1;glass=0;gradientColor=#F8F9FA;gradientDirection=south;" vertex="1" parent="1">
        <mxGeometry x="${nodeX}" y="${y}" width="${nodeW}" height="${nodeH}" as="geometry" />
      </mxCell>`
    
    // 获取子节点
    const children = allNodes.filter(n => n.parent === node.id)
    if (children.length === 0) return
    
    // 计算子节点布局
    const childY = y + nodeH + gapY
    let childX = x
    const totalDescendants = children.reduce((sum, c) => sum + getDescendantCount(c.id), 0)
    const childWidth = availableWidth / totalDescendants
    
    children.forEach(child => {
      const descendants = getDescendantCount(child.id)
      const childAvailableWidth = childWidth * descendants
      
      // 添加连线 - 组织架构使用树形连线（更清晰的层级感）
      cellsXml += `
        <mxCell id="edge-${node.id}-${child.id}" style="edgeStyle=elbowEdgeStyle;elbow=vertical;rounded=0;orthogonalLoop=1;html=1;strokeColor=#999999;strokeWidth=1.5;endArrow=none;exitX=0.5;exitY=1;entryX=0.5;entryY=0;" edge="1" parent="1" source="${node.id}" target="${child.id}">
          <mxGeometry relative="1" as="geometry" />
        </mxCell>`
      
      layoutNode(child, childX, childY, childAvailableWidth)
      childX += childAvailableWidth
    })
  }
  
  // 添加标题
  if (title) {
    cellsXml += `
      <mxCell id="org-title" value="${escapeXml(title)}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=18;fontStyle=1;fontColor=#333333;" vertex="1" parent="1">
        <mxGeometry x="${canvasWidth/2 - 200}" y="20" width="400" height="30" as="geometry" />
      </mxCell>`
  }
  
  // 布局所有根节点
  const startY = title ? 70 : 40
  const rootWidth = canvasWidth / roots.length
  roots.forEach((root, i) => {
    layoutNode(root, i * rootWidth, startY, rootWidth)
  })
  
  // 计算画布高度
  const maxDepth = (node, depth = 0) => {
    const children = allNodes.filter(n => n.parent === node.id)
    if (children.length === 0) return depth
    return Math.max(...children.map(c => maxDepth(c, depth + 1)))
  }
  const treeDepth = roots.length > 0 ? Math.max(...roots.map(r => maxDepth(r))) + 1 : 1
  const canvasHeight = startY + treeDepth * (nodeH + gapY) + 50
  
  const xml = `<?xml version="1.0" encoding="UTF-8"?>
<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${canvasWidth}" pageHeight="${canvasHeight}" math="0" shadow="0">
  <root>
    <mxCell id="0" />
    <mxCell id="1" parent="0" />
    ${cellsXml}
  </root>
</mxGraphModel>`
  
  return xml
}

// ========== 普通风格：多彩、居中标题、虚线边框、层间连线（支持多行）==========
const convertLayeredToDrawioNormal = (data) => {
  const { layers, title } = data
  if (!layers || !layers.length) return ''
  
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
    maxRowWidth: 700,
    layerGap: 30,
  }
  
  // 计算节点宽度（更紧凑，与其他渲染器一致）
  const calcNodeWidth = (label) => {
    let textWidth = 0
    for (const char of (label || '')) {
      textWidth += /[\u4e00-\u9fa5]/.test(char) ? 11 : 5.5
    }
    return Math.max(55, textWidth + 12)
  }
  
  let cells = ''
  let currentY = 30
  let prevLayerBottomY = 0
  let edgeId = 1000
  
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
    const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 200
    const layerContentH = Math.max(nodeRows.length, 1) * cfg.nodeH + Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY
    const layerHeight = 35 + layerContentH + 15
    const layerWidth = maxRowWidth + cfg.layerPadding * 2
    const layerX = (cfg.canvasWidth - layerWidth) / 2
    
    // 层间连线
    if (layerIndex > 0 && prevLayerBottomY > 0) {
      cells += `<mxCell id="edge-${edgeId++}" value="" style="endArrow=classic;html=1;strokeColor=#BDBDBD;strokeWidth=2;dashed=1;dashPattern=8 4;" edge="1" parent="1">
        <mxGeometry relative="1" as="geometry">
          <mxPoint x="${cfg.canvasWidth / 2}" y="${prevLayerBottomY}" as="sourcePoint"/>
          <mxPoint x="${cfg.canvasWidth / 2}" y="${currentY}" as="targetPoint"/>
        </mxGeometry>
      </mxCell>`
    }
    
    // 层背景
    cells += `<mxCell id="layer-${layerIndex}" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=none;strokeColor=${color.border};strokeWidth=2;dashed=1;dashPattern=8 4;arcSize=6;" vertex="1" parent="1">
      <mxGeometry x="${layerX}" y="${currentY}" width="${layerWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    // 层标题
    cells += `<mxCell id="layer-title-${layerIndex}" value="${escapeXml(layer.name || '')}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=14;fontStyle=1;fontColor=${color.title};" vertex="1" parent="1">
      <mxGeometry x="${layerX}" y="${currentY + 5}" width="${layerWidth}" height="25" as="geometry"/>
    </mxCell>`
    
    // 渲染多行节点
    let rowY = currentY + 35
    nodeRows.forEach(row => {
      const rowStartX = layerX + (layerWidth - row.totalWidth) / 2
      let nodeX = rowStartX
      
      row.nodes.forEach((node, idx) => {
        const nodeW = row.widths[idx]
        cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#E0E0E0;strokeWidth=1;fontSize=12;fontColor=#424242;shadow=1;arcSize=15;" vertex="1" parent="1">
          <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeH}" as="geometry"/>
        </mxCell>`
        nodeX += nodeW + cfg.nodeGapX
      })
      rowY += cfg.nodeH + cfg.nodeGapY
    })
    
    prevLayerBottomY = currentY + layerHeight
    currentY += layerHeight + cfg.layerGap
  })
  
  return `<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${cfg.canvasWidth}" pageHeight="${currentY}">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ========== 企业级风格：智能布局（支持多行、blocks分组、自适应宽度）==========
const convertLayeredToDrawio = (data) => {
  const { layers, theme, title } = data
  let cells = ''
  
  // 紧凑布局参数
  const cfg = {
    labelWidth: 85,
    labelGap: 2,
    nodeHeight: 24,           // 减小
    nodeGapX: 6,              // 减小
    nodeGapY: 4,
    maxRowWidth: 700,
    blockGapX: 8,             // 减小
    blockPaddingX: 6,         // 减小
    blockPaddingY: 5,
    blockTitleH: 18,          // 减小
    layerPaddingX: 8,
    layerPaddingY: 8,
    layerGap: 3,
  }
  
  // 计算节点宽度（更紧凑）
  const calcNodeWidth = (label) => {
    let textWidth = 0
    for (const char of (label || '')) {
      textWidth += /[\u4e00-\u9fa5]/.test(char) ? 11 : 5.5
    }
    return Math.max(55, textWidth + 12)
  }
  
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
  }
  
  // 第一遍：计算每层布局
  const layerInfos = []
  let maxLayerWidth = 400
  
  layers?.forEach((layer) => {
    const layerBlocks = layer.blocks || layer.groups || []
    const hasBlocks = layerBlocks.length > 0
    
    let layerWidth = 0
    let layerHeight = 0
    let blockInfos = []
    let nodeRows = []
    
    if (hasBlocks) {
      // 有 blocks - 支持 block 内多行节点
      layerBlocks.forEach(block => {
        const nodes = block.nodes || []
        if (nodes.length === 0) return
        
        const nodeWidths = nodes.map(n => calcNodeWidth(n.label))
        
        // 计算 block 内节点分行（更紧凑）
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
      
      const nodeWidths = allNodes.map(n => calcNodeWidth(n.label))
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
      
      const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 0
      // 紧凑布局：减少最小宽度
      layerWidth = Math.max(maxRowWidth, 100) + cfg.layerPaddingX * 2
      layerHeight = Math.max(nodeRows.length, 1) * cfg.nodeHeight + Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY + cfg.layerPaddingY * 2
    }
    
    maxLayerWidth = Math.max(maxLayerWidth, layerWidth)
    layerInfos.push({ layer, hasBlocks, blockInfos, nodeRows, width: layerWidth, height: layerHeight })
  })
  
  const contentStartX = cfg.labelWidth + cfg.labelGap
  const canvasWidth = contentStartX + maxLayerWidth + 20
  let currentY = 15
  
  // 渲染每层
  layerInfos.forEach((info, layerIndex) => {
    const { layer, hasBlocks, blockInfos, nodeRows, height: layerHeight } = info
    
    // 层背景
    cells += `<mxCell id="layer-bg-${layerIndex}" value="" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.layerBg};strokeColor=${colors.layerBorder};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${contentStartX}" y="${currentY}" width="${maxLayerWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    // 左侧标签
    cells += `<mxCell id="layer-label-${layerIndex}" value="${escapeXml(layer.name || '')}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.labelBg};strokeColor=none;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=${colors.labelText};" vertex="1" parent="1">
      <mxGeometry x="0" y="${currentY}" width="${cfg.labelWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    if (hasBlocks) {
      // 渲染 blocks（支持多行节点）
      let blockX = contentStartX + cfg.layerPaddingX
      const blockY = currentY + cfg.layerPaddingY
      
      blockInfos.forEach((bInfo, bIdx) => {
        const { block, blockRows, width: blockW, height: blockH } = bInfo
        const blockColor = colors.blockTitleBg[block.color] || colors.blockTitleBg.blue
        
        cells += `<mxCell id="block-bg-${layerIndex}-${bIdx}" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.blockBg};strokeColor=${colors.blockBorder};strokeWidth=1;" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${blockY}" width="${blockW}" height="${blockH}" as="geometry"/>
        </mxCell>`
        
        cells += `<mxCell id="block-title-${layerIndex}-${bIdx}" value="${escapeXml(block.name || '')}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${blockColor};strokeColor=none;align=center;verticalAlign=middle;fontSize=9;fontStyle=1;fontColor=${colors.blockTitleText};" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${blockY}" width="${blockW}" height="${cfg.blockTitleH}" as="geometry"/>
        </mxCell>`
        
        // block 内多行节点
        let rowY = blockY + cfg.blockTitleH + cfg.blockPaddingY
        blockRows.forEach(row => {
          let nodeX = blockX + cfg.blockPaddingX
          row.nodes.forEach((node, nIdx) => {
            const nodeW = row.widths[nIdx]
            cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.nodeBg};strokeColor=${colors.nodeBorder};strokeWidth=1;fontSize=9;fontColor=${colors.nodeText};" vertex="1" parent="1">
              <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeHeight}" as="geometry"/>
            </mxCell>`
            nodeX += nodeW + cfg.nodeGapX
          })
          rowY += cfg.nodeHeight + cfg.nodeGapY
        })
        
        blockX += blockW + cfg.blockGapX
      })
    } else {
      let rowY = currentY + cfg.layerPaddingY
      nodeRows.forEach(row => {
        let nodeX = contentStartX + cfg.layerPaddingX
        row.nodes.forEach((node, idx) => {
          const nodeW = row.widths[idx]
          cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.nodeBg};strokeColor=${colors.nodeBorder};strokeWidth=1;fontSize=9;fontColor=${colors.nodeText};" vertex="1" parent="1">
            <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeHeight}" as="geometry"/>
          </mxCell>`
          nodeX += nodeW + cfg.nodeGapX
        })
        rowY += cfg.nodeHeight + cfg.nodeGapY
      })
    }
    
    currentY += layerHeight + cfg.layerGap
  })
  
  // Draw.io 模式暂时禁用 edges 渲染（避免视觉混乱）
  // AntV X6 模式下 edges 正常工作
  const edgesXml = '' // renderEdgesToXml(data.edges, {}, true) 启用时改为 true
  const legendXml = '' // 同步禁用图例
  
  return `<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${canvasWidth}" pageHeight="${currentY + 15}">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
      ${edgesXml}
      ${legendXml}
    </root>
  </mxGraphModel>`
}

// 带侧边栏的分层架构图 - 智能布局（支持多行、blocks分组、自适应宽度）
// 兼容 layered-sidebar（leftSidebar/rightSidebar）和 enterprise-full（leftMonitor/rightSidebars）格式
const convertLayeredSidebarToDrawio = (data) => {
  const { layers, title } = data
  // 兼容两种侧边栏格式
  const leftSidebar = data.leftSidebar || data.leftMonitor
  const rightSidebar = data.rightSidebar || (data.rightSidebars?.[0])
  let cells = ''
  
  // 布局参数 - 优化紧凑布局
  const cfg = {
    sidebarWidth: 100,         // 侧边栏宽度（增加以容纳长文字）
    sidebarNodeH: 36,          // 侧边栏节点高度
    sidebarNodeGap: 2,         // 侧边栏节点间距
    sidebarPadding: 4,
    sidebarTitleH: 28,         // 侧边栏标题高度
    mainPaddingX: 8,           // 主区域水平边距
    nodeHeight: 24,            // 节点高度（减小）
    nodeGapX: 6,               // 节点水平间距（减小）
    nodeGapY: 4,               // 节点垂直间距
    maxRowWidth: 700,          // 每行最大宽度
    blockGapX: 8,              // blocks 间距（减小）
    blockPaddingX: 6,          // block 内边距（减小）
    blockPaddingY: 5,
    blockTitleH: 18,           // block 标题高度（减小）
    layerPaddingY: 8,          // 层垂直边距
    layerGap: 3,               // 层间距
    labelWidth: 85,            // 左侧标签宽度
  }
  
  // 计算节点宽度（更紧凑）
  const calcNodeWidth = (label) => {
    let textWidth = 0
    for (const char of (label || '')) {
      textWidth += /[\u4e00-\u9fa5]/.test(char) ? 11 : 5.5
    }
    return Math.max(55, textWidth + 12)
  }
  
  // 配色 - 参考截图的蓝色调
  const colors = {
    sidebarBg: '#E6F4FF',
    sidebarBorder: '#91CAFF',
    sidebarNodeBg: '#1890FF',
    sidebarNodeText: '#FFFFFF',
    mainBg: '#F0F7FF',
    mainBorder: '#ADC6FF',
    blockBg: '#FFFFFF',
    blockBorder: '#E8E8E8',
    blockTitleBg: { 
      blue: '#1890FF', 
      green: '#52C41A', 
      yellow: '#FAAD14', 
      purple: '#722ED1', 
      pink: '#EB2F96', 
      gray: '#8C8C8C',
      orange: '#FA8C16',
      cyan: '#13C2C2'
    },
    blockTitleText: '#FFFFFF',
    nodeBg: '#FFFFFF',
    nodeBorder: '#D9D9D9',
    nodeText: '#333333',
    labelBg: '#1890FF',
    labelText: '#FFFFFF',
  }
  
  const leftNodes = leftSidebar?.nodes || []
  const rightNodes = rightSidebar?.nodes || []
  const hasLeft = leftNodes.length > 0
  const hasRight = rightNodes.length > 0
  
  // 第一遍：计算每层布局
  const layerInfos = []
  let maxMainWidth = 400
  
  layers?.forEach((layer) => {
    const layerBlocks = layer.blocks || layer.groups || []
    const hasBlocks = layerBlocks.length > 0
    
    let layerWidth = 0
    let layerHeight = 0
    let blockInfos = []
    let nodeRows = []
    
    if (hasBlocks) {
      // 有 blocks - 支持 block 内多行节点
      layerBlocks.forEach(block => {
        const nodes = block.nodes || []
        if (nodes.length === 0) return
        
        const nodeWidths = nodes.map(n => calcNodeWidth(n.label))
        
        // 计算 block 内节点分行（更紧凑）
        const blockRows = []
        let currentRow = []
        let currentRowWidths = []
        let currentRowWidth = 0
        const blockMaxRowWidth = 220  // block 内每行最大宽度（减小以支持更多换行）
        
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
        layerWidth += cfg.mainPaddingX * 2 + cfg.labelWidth
        layerHeight = Math.max(...blockInfos.map(b => b.height)) + cfg.layerPaddingY * 2
      } else {
        layerWidth = 400
        layerHeight = 50
      }
    } else {
      // 无 blocks：智能提取节点
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
      
      // 分行
      const nodeWidths = allNodes.map(n => calcNodeWidth(n.label))
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
      
      const maxRowWidth = nodeRows.length > 0 ? Math.max(...nodeRows.map(r => r.totalWidth)) : 0
      // 紧凑布局：最小宽度减小，让内容更紧凑
      layerWidth = Math.max(maxRowWidth, 100) + cfg.mainPaddingX * 2 + cfg.labelWidth
      layerHeight = Math.max(nodeRows.length, 1) * cfg.nodeHeight + Math.max(nodeRows.length - 1, 0) * cfg.nodeGapY + cfg.layerPaddingY * 2
    }
    
    maxMainWidth = Math.max(maxMainWidth, layerWidth)
    layerInfos.push({ layer, hasBlocks, blockInfos, nodeRows, width: layerWidth, height: layerHeight })
  })
  
  const mainWidth = maxMainWidth
  
  // 计算位置 - 更紧凑间距
  const leftX = 5
  const mainX = hasLeft ? leftX + cfg.sidebarWidth + 5 : 5
  const rightX = mainX + mainWidth + 5
  const canvasWidth = (hasRight ? rightX + cfg.sidebarWidth : mainX + mainWidth) + 10
  
  let currentY = 20
  const startY = currentY
  
  // 渲染左侧边栏
  if (hasLeft) {
    const totalMainH = layerInfos.reduce((s, l) => s + l.height + cfg.layerGap, 0)
    const sbContentH = leftNodes.length * cfg.sidebarNodeH + (leftNodes.length - 1) * cfg.sidebarNodeGap
    const sbH = Math.max(cfg.sidebarTitleH + sbContentH + cfg.sidebarPadding * 2, totalMainH)
    
    // 侧边栏背景
    cells += `<mxCell id="left-sidebar-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarBg};strokeColor=${colors.sidebarBorder};strokeWidth=1;dashed=1;" vertex="1" parent="1">
      <mxGeometry x="${leftX}" y="${startY}" width="${cfg.sidebarWidth}" height="${sbH}" as="geometry"/>
    </mxCell>`
    
    // 侧边栏标题（带背景色，更醒目）
    cells += `<mxCell id="left-sidebar-title" value="${escapeXml(leftSidebar?.title || '')}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#1890FF;strokeColor=none;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#FFFFFF;" vertex="1" parent="1">
      <mxGeometry x="${leftX + 4}" y="${startY + 4}" width="${cfg.sidebarWidth - 8}" height="${cfg.sidebarTitleH - 4}" as="geometry"/>
    </mxCell>`
    
    // 侧边栏节点（支持换行，字体更小）
    let nodeY = startY + cfg.sidebarTitleH + cfg.sidebarPadding
    leftNodes.forEach((node) => {
      cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarNodeBg};strokeColor=none;fontSize=8;fontColor=${colors.sidebarNodeText};fontStyle=1;verticalAlign=middle;spacing=2;" vertex="1" parent="1">
        <mxGeometry x="${leftX + 4}" y="${nodeY}" width="${cfg.sidebarWidth - 8}" height="${cfg.sidebarNodeH}" as="geometry"/>
      </mxCell>`
      nodeY += cfg.sidebarNodeH + cfg.sidebarNodeGap
    })
  }
  
  // 渲染每层
  layerInfos.forEach((info, layerIndex) => {
    const { layer, hasBlocks, blockInfos, nodeRows, height: layerHeight } = info
    
    // 层背景
    cells += `<mxCell id="layer-bg-${layerIndex}" value="" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.mainBg};strokeColor=${colors.mainBorder};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${mainX}" y="${currentY}" width="${mainWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    // 左侧标签
    cells += `<mxCell id="layer-label-${layerIndex}" value="${escapeXml(layer.name || '')}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.labelBg};strokeColor=none;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=${colors.labelText};" vertex="1" parent="1">
      <mxGeometry x="${mainX}" y="${currentY}" width="${cfg.labelWidth}" height="${layerHeight}" as="geometry"/>
    </mxCell>`
    
    if (hasBlocks) {
      // 渲染 blocks（支持多行节点）
      let blockX = mainX + cfg.labelWidth + cfg.mainPaddingX
      const blockY = currentY + cfg.layerPaddingY
      
      blockInfos.forEach((bInfo, bIdx) => {
        const { block, blockRows, width: blockW, height: blockH } = bInfo
        const blockColor = colors.blockTitleBg[block.color] || colors.blockTitleBg.blue
        
        // block 背景
        cells += `<mxCell id="block-bg-${layerIndex}-${bIdx}" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.blockBg};strokeColor=${colors.blockBorder};strokeWidth=1;" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${blockY}" width="${blockW}" height="${blockH}" as="geometry"/>
        </mxCell>`
        
        // block 标题
        cells += `<mxCell id="block-title-${layerIndex}-${bIdx}" value="${escapeXml(block.name || '')}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${blockColor};strokeColor=none;align=center;verticalAlign=middle;fontSize=9;fontStyle=1;fontColor=${colors.blockTitleText};" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${blockY}" width="${blockW}" height="${cfg.blockTitleH}" as="geometry"/>
        </mxCell>`
        
        // block 内多行节点
        let rowY = blockY + cfg.blockTitleH + cfg.blockPaddingY
        blockRows.forEach(row => {
          let nodeX = blockX + cfg.blockPaddingX
          row.nodes.forEach((node, nIdx) => {
            const nodeW = row.widths[nIdx]
            cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.nodeBg};strokeColor=${colors.nodeBorder};strokeWidth=1;fontSize=9;fontColor=${colors.nodeText};" vertex="1" parent="1">
              <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeHeight}" as="geometry"/>
            </mxCell>`
            nodeX += nodeW + cfg.nodeGapX
          })
          rowY += cfg.nodeHeight + cfg.nodeGapY
        })
        
        blockX += blockW + cfg.blockGapX
      })
    } else {
      // 渲染多行节点
      let rowY = currentY + cfg.layerPaddingY
      nodeRows.forEach(row => {
        let nodeX = mainX + cfg.labelWidth + cfg.mainPaddingX
        row.nodes.forEach((node, idx) => {
          const nodeW = row.widths[idx]
          cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=0;whiteSpace=wrap;html=1;fillColor=${colors.nodeBg};strokeColor=${colors.nodeBorder};strokeWidth=1;fontSize=9;fontColor=${colors.nodeText};" vertex="1" parent="1">
            <mxGeometry x="${nodeX}" y="${rowY}" width="${nodeW}" height="${cfg.nodeHeight}" as="geometry"/>
          </mxCell>`
          nodeX += nodeW + cfg.nodeGapX
        })
        rowY += cfg.nodeHeight + cfg.nodeGapY
      })
    }
    
    currentY += layerHeight + cfg.layerGap
  })
  
  // 渲染右侧边栏
  if (hasRight) {
    const sbContentH = rightNodes.length * cfg.sidebarNodeH + (rightNodes.length - 1) * cfg.sidebarNodeGap
    const sbH = Math.max(cfg.sidebarTitleH + sbContentH + cfg.sidebarPadding * 2, currentY - startY)
    
    // 右侧边栏背景
    cells += `<mxCell id="right-sidebar-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarBg};strokeColor=${colors.sidebarBorder};strokeWidth=1;dashed=1;" vertex="1" parent="1">
      <mxGeometry x="${rightX}" y="${startY}" width="${cfg.sidebarWidth}" height="${sbH}" as="geometry"/>
    </mxCell>`
    
    // 右侧边栏标题（带背景色，更醒目）
    cells += `<mxCell id="right-sidebar-title" value="${escapeXml(rightSidebar?.title || '')}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FA8C16;strokeColor=none;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#FFFFFF;" vertex="1" parent="1">
      <mxGeometry x="${rightX + 4}" y="${startY + 4}" width="${cfg.sidebarWidth - 8}" height="${cfg.sidebarTitleH - 4}" as="geometry"/>
    </mxCell>`
    
    // 右侧边栏节点（支持换行，字体更小）
    let nodeY = startY + cfg.sidebarTitleH + cfg.sidebarPadding
    rightNodes.forEach((node) => {
      cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${colors.sidebarNodeBg};strokeColor=none;fontSize=8;fontColor=${colors.sidebarNodeText};fontStyle=1;verticalAlign=middle;spacing=2;" vertex="1" parent="1">
        <mxGeometry x="${rightX + 4}" y="${nodeY}" width="${cfg.sidebarWidth - 8}" height="${cfg.sidebarNodeH}" as="geometry"/>
      </mxCell>`
      nodeY += cfg.sidebarNodeH + cfg.sidebarNodeGap
    })
  }
  
  const canvasHeight = currentY + 15
  
  // Draw.io 模式暂时禁用 edges 渲染（避免视觉混乱）
  // AntV X6 模式下 edges 正常工作
  const edgesXml = '' // renderEdgesToXml(data.edges, {}, true) 启用时改为 true
  const legendXml = '' // 同步禁用图例
  
  return `<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${canvasWidth}" pageHeight="${canvasHeight}">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
      ${edgesXml}
      ${legendXml}
    </root>
  </mxGraphModel>`
}

// 带功能栏的产品架构图（类似 Tinyflow）
const convertLayeredWithFeaturesToDrawio = (data) => {
  const { title, mainArea, rightFeatures } = data
  let cells = ''
  
  // 配色方案
  const colorMap = {
    blue: { bg: '#E6F7FF', border: '#69C0FF', node: '#1890FF', text: '#FFFFFF' },
    purple: { bg: '#F9F0FF', border: '#B37FEB', node: '#722ED1', text: '#FFFFFF' },
    orange: { bg: '#FFF7E6', border: '#FFD591', node: '#FA8C16', text: '#FFFFFF' },
    yellow: { bg: '#FFFBE6', border: '#FFE58F', node: '#FAAD14', text: '#000000' },
    green: { bg: '#F6FFED', border: '#95DE64', node: '#52C41A', text: '#FFFFFF' },
    pink: { bg: '#FFF0F6', border: '#FFADD2', node: '#EB2F96', text: '#FFFFFF' },
    gray: { bg: '#FAFAFA', border: '#D9D9D9', node: '#F5F5F5', text: '#262626' },
  }
  
  // 布局参数
  const cfg = {
    padding: 20,
    sectionGap: 15,
    sectionPadding: 15,
    sectionTitleH: 24,
    nodeW: 90,
    nodeH: 32,
    nodeGapX: 10,
    nodeGapY: 8,
    featureW: 120,
    featureNodeH: 36,
    featureGap: 8,
  }
  
  // 计算主区域尺寸
  const sections = mainArea?.sections || []
  let mainWidth = 0
  let mainHeight = cfg.padding
  
  sections.forEach(sec => {
    const nodes = sec.nodes || []
    const cols = Math.min(nodes.length, 4)
    const rows = Math.ceil(nodes.length / 4)
    const secW = cols * cfg.nodeW + (cols - 1) * cfg.nodeGapX + cfg.sectionPadding * 2
    const secH = cfg.sectionTitleH + cfg.sectionPadding + rows * cfg.nodeH + (rows - 1) * cfg.nodeGapY + cfg.sectionPadding
    mainWidth = Math.max(mainWidth, secW)
    mainHeight += secH + cfg.sectionGap
  })
  mainHeight -= cfg.sectionGap
  mainHeight += cfg.padding
  mainWidth += cfg.padding * 2
  
  // 计算右侧功能栏
  const features = rightFeatures?.nodes || []
  const featureH = features.length * cfg.featureNodeH + (features.length - 1) * cfg.featureGap + 50
  
  // 画布
  const hasFeatures = features.length > 0
  const canvasW = mainWidth + (hasFeatures ? cfg.featureW + 30 : 0) + 40
  const canvasH = Math.max(mainHeight, featureH) + 60
  
  let startY = 40
  
  // 标题
  if (title) {
    cells += `<mxCell id="title" value="${escapeXml(title)}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=16;fontStyle=1;fontColor=#262626;" vertex="1" parent="1">
      <mxGeometry x="20" y="10" width="${canvasW - 40}" height="30" as="geometry"/>
    </mxCell>`
  }
  
  // 主区域背景
  cells += `<mxCell id="main-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#E0E0E0;strokeWidth=1;" vertex="1" parent="1">
    <mxGeometry x="20" y="${startY}" width="${mainWidth}" height="${mainHeight}" as="geometry"/>
  </mxCell>`
  
  // 渲染每个分组
  let secY = startY + cfg.padding
  sections.forEach((sec, si) => {
    const nodes = sec.nodes || []
    const cols = Math.min(nodes.length, 4)
    const rows = Math.ceil(nodes.length / 4)
    const color = colorMap[sec.color] || colorMap.gray
    const secW = mainWidth - cfg.padding * 2
    const secH = cfg.sectionTitleH + cfg.sectionPadding + rows * cfg.nodeH + (rows - 1) * cfg.nodeGapY + cfg.sectionPadding
    
    // 分组背景
    cells += `<mxCell id="sec-bg-${si}" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${color.bg};strokeColor=${color.border};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${20 + cfg.padding}" y="${secY}" width="${secW}" height="${secH}" as="geometry"/>
    </mxCell>`
    
    // 分组标题
    cells += `<mxCell id="sec-title-${si}" value="${sec.name || ''}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=11;fontStyle=0;fontColor=#8C8C8C;" vertex="1" parent="1">
      <mxGeometry x="${20 + cfg.padding}" y="${secY + 5}" width="${secW}" height="18" as="geometry"/>
    </mxCell>`
    
    // 分组内节点
    const nodeStartX = 20 + cfg.padding + cfg.sectionPadding
    const nodeStartY = secY + cfg.sectionTitleH + cfg.sectionPadding
    nodes.forEach((node, ni) => {
      const row = Math.floor(ni / 4)
      const col = ni % 4
      const nx = nodeStartX + col * (cfg.nodeW + cfg.nodeGapX)
      const ny = nodeStartY + row * (cfg.nodeH + cfg.nodeGapY)
      
      cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${color.node};strokeColor=none;fontSize=11;fontColor=${color.text};fontStyle=1;" vertex="1" parent="1">
        <mxGeometry x="${nx}" y="${ny}" width="${cfg.nodeW}" height="${cfg.nodeH}" as="geometry"/>
      </mxCell>`
    })
    
    secY += secH + cfg.sectionGap
  })
  
  // 右侧功能栏
  if (hasFeatures) {
    const fColor = colorMap[rightFeatures?.color] || colorMap.pink
    const fX = mainWidth + 40
    const fH = features.length * cfg.featureNodeH + (features.length - 1) * cfg.featureGap + 50
    
    // 功能栏背景
    cells += `<mxCell id="feature-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${fColor.bg};strokeColor=${fColor.border};strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${fX}" y="${startY}" width="${cfg.featureW}" height="${fH}" as="geometry"/>
    </mxCell>`
    
    // 功能栏标题
    cells += `<mxCell id="feature-title" value="${escapeXml(rightFeatures?.title || '')}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=12;fontStyle=1;fontColor=${fColor.node};" vertex="1" parent="1">
      <mxGeometry x="${fX}" y="${startY + 10}" width="${cfg.featureW}" height="24" as="geometry"/>
    </mxCell>`
    
    // 功能节点
    let fNodeY = startY + 45
    features.forEach((node) => {
      cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${fColor.node};strokeColor=none;fontSize=11;fontColor=${fColor.text};fontStyle=1;" vertex="1" parent="1">
        <mxGeometry x="${fX + 10}" y="${fNodeY}" width="${cfg.featureW - 20}" height="${cfg.featureNodeH}" as="geometry"/>
      </mxCell>`
      fNodeY += cfg.featureNodeH + cfg.featureGap
    })
  }
  
  return `<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${canvasW}" pageHeight="${canvasH}">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// ========== 全景企业架构（enterprise-full）==========
const convertEnterpriseFullToDrawio = (data) => {
  logger.log('>>> 进入 convertEnterpriseFullToDrawio 函数')
  const { title, topUsers, leftMonitor, rightSidebars, layers, bottomInfra } = data
  logger.log('topUsers:', topUsers?.length)
  logger.log('leftMonitor:', leftMonitor?.nodes?.length)
  logger.log('rightSidebars:', rightSidebars?.length)
  logger.log('layers:', layers?.length)
  logger.log('bottomInfra:', bottomInfra?.nodes?.length)
  let cells = ''
  
  // 配色方案
  const colorMap = {
    pink: { bg: '#FFF0F5', border: '#FFB6C1', node: '#FFE4E9', text: '#333' },
    yellow: { bg: '#FFFACD', border: '#FFD700', node: '#FFF8DC', text: '#333' },
    blue: { bg: '#E6F3FF', border: '#87CEEB', node: '#E0F0FF', text: '#333' },
    green: { bg: '#F0FFF0', border: '#90EE90', node: '#E8FFE8', text: '#333' },
    gray: { bg: '#F5F5F5', border: '#D3D3D3', node: '#FAFAFA', text: '#333' },
    purple: { bg: '#F5F0FF', border: '#DDA0DD', node: '#F0E6FF', text: '#333' },
  }
  
  // 用户图标映射
  const userIconMap = {
    developer: '👨‍💻',
    merchant: '🏪',
    channel: '🔗',
    finance: '💰',
    operator: '👤',
    ops: '🔧',
    default: '👤'
  }
  
  // 基础设施图标映射
  const infraIconMap = {
    nginx: '🌐',
    mysql: '🗄️',
    redis: '⚡',
    mq: '📨',
    storage: '📦',
    zk: '🔗',
    default: '📦'
  }
  
  // 布局参数
  const cfg = {
    canvasWidth: 1400,
    topUserH: 60,
    leftLabelW: 80,
    leftMonitorW: 100,
    rightSidebarW: 100,
    rightSidebarGap: 10,
    layerGap: 2,
    layerPadding: 10,
    nodeW: 80,
    nodeH: 28,
    nodeGap: 6,
    blockGap: 10,
    blockPadding: 8,
    blockTitleH: 24,
    subsystemGap: 8,
    subsystemPadding: 6,
    subsystemTitleH: 22,
    bottomInfraH: 70,
  }
  
  // ========== 动态计算布局（根据实际组件调整）==========
  
  // 判断哪些可选组件存在
  const hasLeftMonitor = leftMonitor && leftMonitor.nodes && leftMonitor.nodes.length > 0
  const rightSidebarsArr = rightSidebars || []
  const hasRightSidebars = rightSidebarsArr.length > 0
  const hasTopUsers = topUsers && topUsers.length > 0
  const hasBottomInfra = bottomInfra && bottomInfra.nodes && bottomInfra.nodes.length > 0
  
  // 动态计算右侧边栏总宽度
  const rightTotalW = hasRightSidebars 
    ? rightSidebarsArr.length * cfg.rightSidebarW + (rightSidebarsArr.length - 1) * cfg.rightSidebarGap 
    : 0
  
  // 动态计算主内容区宽度（根据是否有监控栏和右侧边栏调整）
  const mainStartX = cfg.leftLabelW + (hasLeftMonitor ? cfg.leftMonitorW + 10 : 5)
  const mainEndX = cfg.canvasWidth - (hasRightSidebars ? rightTotalW + 20 : 20)
  const mainWidth = mainEndX - mainStartX
  
  let currentY = 20
  
  // ========== 1. 顶部用户角色（可选）==========
  if (hasTopUsers) {
    const userW = 100
    const totalUserW = topUsers.length * userW
    const startX = (cfg.canvasWidth - totalUserW) / 2
    
    topUsers.forEach((user, i) => {
      const icon = userIconMap[user.icon] || userIconMap.default
      const x = startX + i * userW
      cells += `<mxCell id="${user.id}" value="${icon}&#xa;${user.label}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#E0E0E0;fontSize=11;fontColor=#333;verticalAlign=middle;spacing=2;" vertex="1" parent="1">
        <mxGeometry x="${x}" y="${currentY}" width="${userW - 10}" height="${cfg.topUserH}" as="geometry"/>
      </mxCell>`
    })
    currentY += cfg.topUserH + 15
  }
  
  // 计算层的总高度（用于左侧监控栏）
  let layersTotalHeight = 0
  const layersStartY = currentY
  
  // 预计算层高度
  const layerHeights = []
  layers?.forEach(layer => {
    let layerH = 0
    // 兼容处理：groups → blocks
    const layerBlocks = layer.blocks || layer.groups
    
    if (layerBlocks && layerBlocks.length > 0) {
      // 有 blocks 的层 - 动态计算列数
      let maxBlockH = 0
      layerBlocks.forEach(block => {
        const nodes = block.nodes || []
        const cols = Math.min(nodes.length, 3) || 1  // 与渲染时一致：最多3列，最少1列
        const rows = Math.ceil(nodes.length / cols) || 1
        const blockH = cfg.blockTitleH + cfg.blockPadding * 2 + rows * cfg.nodeH + (rows - 1) * cfg.nodeGap
        maxBlockH = Math.max(maxBlockH, blockH)
      })
      layerH = maxBlockH + cfg.layerPadding * 2
    } else if (layer.systems) {
      // 有 systems 的层（3层嵌套）
      let maxSysH = 0
      layer.systems.forEach(sys => {
        // 子系统是水平排列的，取最大高度
        const subsystems = sys.subsystems || []
        let maxSubsysH = 0
        subsystems.forEach(ss => {
          const nodes = ss.nodes || []
          const cols = Math.min(nodes.length, 2) || 1  // 动态列数：最多2列，最少1列
          const rows = Math.ceil(nodes.length / cols) || 1
          const ssH = cfg.subsystemTitleH + cfg.subsystemPadding * 2 + rows * cfg.nodeH + (rows - 1) * cfg.nodeGap
          maxSubsysH = Math.max(maxSubsysH, ssH)
        })
        const sysH = subsystems.length > 0 ? (30 + maxSubsysH) : 50 // 有子系统则计算，否则最小高度
        maxSysH = Math.max(maxSysH, sysH)
      })
      layerH = maxSysH + cfg.layerPadding * 2
    } else {
      // 简单层
      const nodes = layer.nodes || []
      if (nodes.length > 0) {
        const rows = Math.ceil(nodes.length / 6)
        layerH = rows * cfg.nodeH + (rows - 1) * cfg.nodeGap + cfg.layerPadding * 2
      } else {
        layerH = cfg.nodeH + cfg.layerPadding * 2 // 空层最小高度
      }
    }
    layerHeights.push(layerH)
    layersTotalHeight += layerH + cfg.layerGap
  })
  
  // ========== 2. 左侧监控栏（可选）==========
  if (hasLeftMonitor) {
    const monitorNodes = leftMonitor.nodes
    const monitorH = Math.max(layersTotalHeight, 200) // 最小高度200
    
    // 监控栏背景
    cells += `<mxCell id="left-monitor-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#F5F5F5;strokeColor=#E0E0E0;strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${cfg.leftLabelW + 5}" y="${layersStartY}" width="${cfg.leftMonitorW}" height="${monitorH}" as="geometry"/>
    </mxCell>`
    
    // 监控栏标题
    if (leftMonitor.title) {
      cells += `<mxCell id="left-monitor-title" value="${escapeXml(leftMonitor.title)}" style="text;html=1;strokeColor=none;fillColor=#4A90A4;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#FFFFFF;rounded=1;" vertex="1" parent="1">
        <mxGeometry x="${cfg.leftLabelW + 5}" y="${layersStartY}" width="${cfg.leftMonitorW}" height="22" as="geometry"/>
      </mxCell>`
    }
    
    // 监控节点（支持换行，字体更小）
    const titleOffset = leftMonitor.title ? 26 : 10
    const availableH = monitorH - titleOffset - 10
    const nodeH = Math.min(42, Math.max(35, availableH / monitorNodes.length))
    let nodeY = layersStartY + titleOffset
    monitorNodes.forEach((node, i) => {
      cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#E8E8E8;fontSize=8;fontColor=#666;verticalAlign=middle;spacing=2;" vertex="1" parent="1">
        <mxGeometry x="${cfg.leftLabelW + 8}" y="${nodeY}" width="${cfg.leftMonitorW - 6}" height="${nodeH - 4}" as="geometry"/>
      </mxCell>`
      nodeY += nodeH
    })
  }
  
  // ========== 3. 右侧边栏（可选：公共服务、运维等）==========
  if (hasRightSidebars) {
    let sidebarX = mainEndX + 10
    const sidebarH = Math.max(layersTotalHeight, 200) // 最小高度200
    
    rightSidebarsArr.forEach((sidebar, si) => {
      const color = colorMap[sidebar.color] || colorMap.gray
      const sidebarNodes = sidebar.nodes || []
      
      // 侧边栏背景
      cells += `<mxCell id="right-sidebar-${si}-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${color.bg};strokeColor=${color.border};strokeWidth=1;" vertex="1" parent="1">
        <mxGeometry x="${sidebarX}" y="${layersStartY}" width="${cfg.rightSidebarW}" height="${sidebarH}" as="geometry"/>
      </mxCell>`
      
      // 侧边栏标题
      cells += `<mxCell id="right-sidebar-${si}-title" value="${escapeXml(sidebar.title)}" style="text;html=1;strokeColor=none;fillColor=${color.border};align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=#333;" vertex="1" parent="1">
        <mxGeometry x="${sidebarX}" y="${layersStartY}" width="${cfg.rightSidebarW}" height="24" as="geometry"/>
      </mxCell>`
      
      // 侧边栏节点（支持换行，字体更小）
      const availableH = sidebarH - 30
      const nodeH = Math.min(42, Math.max(35, availableH / Math.max(sidebarNodes.length, 1)))
      let nodeY = layersStartY + 28
      sidebarNodes.forEach((node) => {
        cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=${color.border};fontSize=8;fontColor=#333;verticalAlign=middle;spacing=2;" vertex="1" parent="1">
          <mxGeometry x="${sidebarX + 4}" y="${nodeY}" width="${cfg.rightSidebarW - 8}" height="${nodeH - 4}" as="geometry"/>
        </mxCell>`
        nodeY += nodeH
      })
      
      sidebarX += cfg.rightSidebarW + cfg.rightSidebarGap
    })
  }
  
  // ========== 4. 中间层区域 ==========
  logger.log('开始渲染层区域, layersStartY=', layersStartY, 'mainStartX=', mainStartX, 'mainWidth=', mainWidth)
  let layerY = layersStartY
  layers?.forEach((layer, layerIndex) => {
    const layerH = layerHeights[layerIndex]
    const color = colorMap[layer.color] || colorMap.gray
    
    const hasBlocks = layer.blocks?.length > 0 || layer.groups?.length > 0
    const hasSystems = layer.systems?.length > 0
    const hasNodes = layer.nodes?.length > 0
    logger.log(`[enterprise-full] 层${layerIndex} [${layer.name}]: layerH=${layerH}, hasBlocks=${hasBlocks}, hasSystems=${hasSystems}, hasNodes=${hasNodes}`)
    
    // 左侧层标签
    cells += `<mxCell id="layer-label-${layerIndex}" value="${layer.name}" style="text;html=1;strokeColor=none;fillColor=#4A90A4;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=#FFFFFF;rounded=0;" vertex="1" parent="1">
      <mxGeometry x="0" y="${layerY}" width="${cfg.leftLabelW}" height="${layerH}" as="geometry"/>
    </mxCell>`
    
    // 层背景
    cells += `<mxCell id="layer-bg-${layerIndex}" value="" style="rounded=0;whiteSpace=wrap;html=1;fillColor=#FAFAFA;strokeColor=#E8E8E8;strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${mainStartX}" y="${layerY}" width="${mainWidth}" height="${layerH}" as="geometry"/>
    </mxCell>`
    
    // 兼容处理：groups → blocks
    const layerBlocks = layer.blocks || layer.groups
    
    if (layerBlocks && layerBlocks.length > 0) {
      logger.log(`  渲染 ${layerBlocks.length} 个 blocks`)
      // ========== 有 blocks 的层 ==========
      const blocks = layerBlocks
      const blockCount = blocks.length
      const blockW = (mainWidth - cfg.layerPadding * 2 - (blockCount - 1) * cfg.blockGap) / blockCount
      let blockX = mainStartX + cfg.layerPadding
      
      blocks.forEach((block, bi) => {
        const blockColor = colorMap[block.color] || colorMap.gray
        const nodes = block.nodes || []
        const rows = Math.ceil(nodes.length / 3)
        const blockH = cfg.blockTitleH + cfg.blockPadding * 2 + rows * cfg.nodeH + (rows - 1) * cfg.nodeGap
        
        // 区块背景
        cells += `<mxCell id="block-${layerIndex}-${bi}-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${blockColor.bg};strokeColor=${blockColor.border};strokeWidth=1;" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${layerY + cfg.layerPadding}" width="${blockW}" height="${blockH}" as="geometry"/>
        </mxCell>`
        
        // 区块标题
        cells += `<mxCell id="block-${layerIndex}-${bi}-title" value="${block.name}" style="text;html=1;strokeColor=none;fillColor=none;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#666;" vertex="1" parent="1">
          <mxGeometry x="${blockX}" y="${layerY + cfg.layerPadding}" width="${blockW}" height="${cfg.blockTitleH}" as="geometry"/>
        </mxCell>`
        
        // 区块内节点（根据节点数量自动调整列数）
        const cols = Math.min(nodes.length, 3) || 1  // 最多3列，最少1列
        const nodeW = (blockW - cfg.blockPadding * 2 - (cols - 1) * cfg.nodeGap) / cols
        let nodeY = layerY + cfg.layerPadding + cfg.blockTitleH + cfg.blockPadding
        nodes.forEach((node, ni) => {
          const row = Math.floor(ni / cols)
          const col = ni % cols
          const nodeX = blockX + cfg.blockPadding + col * (nodeW + cfg.nodeGap)
          const ny = nodeY + row * (cfg.nodeH + cfg.nodeGap)
          
          cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${blockColor.node};strokeColor=${blockColor.border};fontSize=9;fontColor=#333;" vertex="1" parent="1">
            <mxGeometry x="${nodeX}" y="${ny}" width="${nodeW}" height="${cfg.nodeH}" as="geometry"/>
          </mxCell>`
        })
        
        blockX += blockW + cfg.blockGap
      })
    } else if (layer.systems) {
      // ========== 有 systems 的层（3层嵌套）==========
      const systems = layer.systems
      const sysCount = systems.length
      const sysW = (mainWidth - cfg.layerPadding * 2 - (sysCount - 1) * cfg.blockGap) / sysCount
      let sysX = mainStartX + cfg.layerPadding
      
      systems.forEach((sys, si) => {
        // 系统容器标题
        cells += `<mxCell id="sys-${layerIndex}-${si}-title" value="${sys.name}" style="text;html=1;strokeColor=none;fillColor=#E8E8E8;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#333;rounded=1;" vertex="1" parent="1">
          <mxGeometry x="${sysX}" y="${layerY + cfg.layerPadding}" width="${sysW}" height="24" as="geometry"/>
        </mxCell>`
        
        // 子系统
        let ssY = layerY + cfg.layerPadding + 28
        const subsystems = sys.subsystems || []
        if (subsystems.length === 0) return // 没有子系统则跳过
        
        const ssW = (sysW - (subsystems.length - 1) * cfg.subsystemGap) / subsystems.length
        let ssX = sysX
        
        subsystems.forEach((ss, ssi) => {
          const ssColor = colorMap[ss.color] || colorMap.gray
          const nodes = ss.nodes || []
          const cols = Math.min(nodes.length, 2) || 1  // 动态列数：最多2列，最少1列
          const rows = Math.ceil(nodes.length / cols) || 1
          const ssH = cfg.subsystemTitleH + cfg.subsystemPadding * 2 + rows * cfg.nodeH + (rows - 1) * cfg.nodeGap
          
          // 子系统背景
          cells += `<mxCell id="ss-${layerIndex}-${si}-${ssi}-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${ssColor.bg};strokeColor=${ssColor.border};strokeWidth=1;" vertex="1" parent="1">
            <mxGeometry x="${ssX}" y="${ssY}" width="${ssW}" height="${ssH}" as="geometry"/>
          </mxCell>`
          
          // 子系统标题
          cells += `<mxCell id="ss-${layerIndex}-${si}-${ssi}-title" value="${ss.name}" style="text;html=1;strokeColor=none;fillColor=${ssColor.border};align=center;verticalAlign=middle;fontSize=9;fontStyle=1;fontColor=#333;rounded=0;" vertex="1" parent="1">
            <mxGeometry x="${ssX}" y="${ssY}" width="${ssW}" height="${cfg.subsystemTitleH}" as="geometry"/>
          </mxCell>`
          
          // 子系统内节点 - 动态列数
          const nodeW = (ssW - cfg.subsystemPadding * 2 - (cols - 1) * cfg.nodeGap) / cols
          let nodeY = ssY + cfg.subsystemTitleH + cfg.subsystemPadding
          nodes.forEach((node, ni) => {
            const row = Math.floor(ni / cols)
            const col = ni % cols
            const nodeX = ssX + cfg.subsystemPadding + col * (nodeW + cfg.nodeGap)
            const ny = nodeY + row * (cfg.nodeH + cfg.nodeGap)
            
            cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${ssColor.node};strokeColor=${ssColor.border};fontSize=8;fontColor=#333;" vertex="1" parent="1">
              <mxGeometry x="${nodeX}" y="${ny}" width="${nodeW}" height="${cfg.nodeH}" as="geometry"/>
            </mxCell>`
          })
          
          ssX += ssW + cfg.subsystemGap
        })
        
        sysX += sysW + cfg.blockGap
      })
    } else {
      // ========== 简单层 ==========
      const nodes = layer.nodes || []
      if (nodes.length > 0) {
        const nodeCount = nodes.length
        const nodeW = Math.min(cfg.nodeW, (mainWidth - cfg.layerPadding * 2 - (nodeCount - 1) * cfg.nodeGap) / nodeCount)
        const totalNodesW = nodeCount * nodeW + (nodeCount - 1) * cfg.nodeGap
        let nodeX = mainStartX + (mainWidth - totalNodesW) / 2
        const nodeY = layerY + cfg.layerPadding
        
        nodes.forEach((node) => {
          cells += `<mxCell id="${node.id}" value="${escapeXml(node.label)}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=${color.node || '#FFFFFF'};strokeColor=${color.border || '#E0E0E0'};fontSize=10;fontColor=#333;" vertex="1" parent="1">
            <mxGeometry x="${nodeX}" y="${nodeY}" width="${nodeW}" height="${cfg.nodeH}" as="geometry"/>
          </mxCell>`
          nodeX += nodeW + cfg.nodeGap
        })
      }
      // 空层不渲染节点，只保留层背景和标签
    }
    
    layerY += layerH + cfg.layerGap
  })
  
  currentY = layerY + 10
  
  // ========== 5. 底部基础设施（可选）==========
  if (hasBottomInfra) {
    const infraNodes = bottomInfra.nodes
    const infraW = mainWidth
    
    // 左侧层标签（基础设施层）
    cells += `<mxCell id="infra-label" value="${escapeXml(bottomInfra.title || '基础设施层')}" style="text;html=1;strokeColor=none;fillColor=#4A90A4;align=center;verticalAlign=middle;fontSize=11;fontStyle=1;fontColor=#FFFFFF;rounded=0;" vertex="1" parent="1">
      <mxGeometry x="0" y="${currentY}" width="${cfg.leftLabelW}" height="${cfg.bottomInfraH}" as="geometry"/>
    </mxCell>`
    
    // 基础设施背景
    cells += `<mxCell id="infra-bg" value="" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#F0F0F0;strokeColor=#D0D0D0;strokeWidth=1;" vertex="1" parent="1">
      <mxGeometry x="${mainStartX}" y="${currentY}" width="${infraW}" height="${cfg.bottomInfraH}" as="geometry"/>
    </mxCell>`
    
    // 基础设施节点 - 动态计算
    const nodeCount = infraNodes.length
    const infraTitleW = 75  // 内部标题宽度
    const infraTitleH = 20
    
    // 基础设施内部标题
    cells += `<mxCell id="infra-inner-title" value="基础设施" style="text;html=1;strokeColor=none;fillColor=#D0D0D0;align=center;verticalAlign=middle;fontSize=10;fontStyle=1;fontColor=#333;rounded=1;" vertex="1" parent="1">
      <mxGeometry x="${mainStartX + 5}" y="${currentY + 5}" width="${infraTitleW}" height="${infraTitleH}" as="geometry"/>
    </mxCell>`
    
    if (nodeCount > 0) {
      const nodeGap = 15
      const availableW = infraW - infraTitleW - 10
      const nodeW = Math.min(100, (availableW - (nodeCount - 1) * nodeGap) / nodeCount)
      let nodeX = mainStartX + infraTitleW + 10
      const nodeY = currentY + 8
      
      infraNodes.forEach((node) => {
        const icon = infraIconMap[node.icon] || infraIconMap.default
        cells += `<mxCell id="${node.id}" value="${icon}&#xa;${node.label}" style="rounded=1;whiteSpace=wrap;html=1;fillColor=#FFFFFF;strokeColor=#E0E0E0;fontSize=10;fontColor=#333;verticalAlign=middle;" vertex="1" parent="1">
          <mxGeometry x="${nodeX}" y="${nodeY}" width="${nodeW}" height="${cfg.bottomInfraH - 16}" as="geometry"/>
        </mxCell>`
        nodeX += nodeW + nodeGap
      })
    }
    
    currentY += cfg.bottomInfraH + 20
  }
  
  return `<mxGraphModel dx="0" dy="0" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="${cfg.canvasWidth}" pageHeight="${currentY}">
    <root>
      <mxCell id="0"/>
      <mxCell id="1" parent="0"/>
      ${cells}
    </root>
  </mxGraphModel>`
}

// 下载文件
const downloadFile = (data, format) => {
  const blob = new Blob([data], { type: format === 'svg' ? 'image/svg+xml' : 'image/png' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `architecture.${format}`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => {
  window.addEventListener('message', handleDrawioMessage)
})

onUnmounted(() => {
  // 清理事件监听器
  window.removeEventListener('message', handleDrawioMessage)
  // 清理定时器
  if (inputDebounceTimer) {
    clearTimeout(inputDebounceTimer)
    inputDebounceTimer = null
  }
})
</script>

<style scoped>
.diagram-generator {
  display: flex;
  height: 100%;
  background: #f0f2f5;
}

/* 左侧面板 */
.left-panel {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* Logo 标题 */
.logo-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 17px;
  font-weight: 600;
}

.logo-header i {
  font-size: 22px;
}

/* AI 生成区域 */
.ai-section {
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 风格选择器 */
.style-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.style-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.style-selector :deep(.el-radio-button__inner) {
  padding: 6px 12px;
  font-size: 12px;
}

.style-selector :deep(.el-radio-button__inner i) {
  margin-right: 4px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 14px;
}

.section-title i {
  color: #667eea;
}

.ai-section :deep(.el-textarea__inner) {
  font-size: 13px;
  line-height: 1.6;
  border-radius: 8px;
  border-color: #d0d0d0;
  padding: 12px;
  min-height: 200px !important;
  max-height: calc(100vh - 450px);
  flex: 1;
}

.ai-section :deep(.el-textarea__inner):focus {
  border-color: #667eea;
}

/* 智能分析区域 */
.smart-analysis {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f0f4ff 0%, #faf0ff 100%);
  border-radius: 8px;
  border: 1px solid #e0e7ff;
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #667eea;
  margin-bottom: 8px;
}

.analysis-header i {
  font-size: 14px;
}

.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.analysis-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.analysis-label {
  color: #666;
  min-width: 60px;
}

.analysis-item :deep(.el-tag) {
  font-size: 11px;
}

.generate-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  margin-top: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
}

.generate-btn i {
  margin-right: 8px;
}

.generate-btn:hover {
  background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
}

/* 右侧编辑器 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fafafa;
  position: relative;
}

.right-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.toolbar-title i {
  color: #667eea;
  font-size: 18px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-actions :deep(.el-divider) {
  margin: 0 4px;
}

.editor-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.drawio-frame {
  flex: 1;
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
</style>
