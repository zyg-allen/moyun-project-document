<template>
  <div class="smart-diagram-container" ref="containerRef">
    <div class="diagram-toolbar" v-if="showToolbar">
      <el-button-group>
        <el-button size="small" @click="fitView">
          <el-icon><FullScreen /></el-icon> 适应
        </el-button>
        <el-button size="small" @click="zoomIn">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
        <el-button size="small" @click="zoomOut">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
        <el-button size="small" @click="exportImage">
          <el-icon><Download /></el-icon> 导出
        </el-button>
      </el-button-group>
      <span class="diagram-info">
        节点: {{ nodes.length }} | 边: {{ edges.length }}
      </span>
    </div>
    <div class="diagram-canvas" ref="canvasRef"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { FullScreen, ZoomIn, ZoomOut, Download } from '@element-plus/icons-vue'
import { SmartDiagramRenderer } from '@/utils/diagram/SmartDiagramRenderer'

const props = defineProps({
  graphData: {
    type: Object,
    default: () => ({ nodes: [], edges: [] })
  },
  showToolbar: {
    type: Boolean,
    default: true
  },
  theme: {
    type: String,
    default: 'light'
  }
})

const emit = defineEmits(['nodeClick', 'edgeClick', 'layoutComplete'])

const containerRef = ref(null)
const canvasRef = ref(null)
const nodes = ref([])
const edges = ref([])
const isLoading = ref(false)

let renderer = null

onMounted(async () => {
  await nextTick()
  if (canvasRef.value) {
    await initRenderer()
  }
})

onUnmounted(() => {
  // 清理渲染器，防止内存泄漏
  if (renderer) {
    renderer.destroy()
    renderer = null
  }
})

watch(() => props.graphData, async (newData) => {
  if (newData && newData.nodes && newData.nodes.length > 0) {
    await updateDiagram(newData)
  }
}, { deep: true })

async function initRenderer() {
  if (!canvasRef.value) return
  
  // 如果已存在渲染器，先销毁
  if (renderer) {
    renderer.destroy()
  }
  
  renderer = new SmartDiagramRenderer(canvasRef.value, {
    theme: props.theme,
    onNodeClick: (node) => emit('nodeClick', node),
    onEdgeClick: (edge) => emit('edgeClick', edge),
    onLayoutComplete: (result) => emit('layoutComplete', result)
  })
  
  // 如果有初始数据，立即渲染
  if (props.graphData?.nodes?.length > 0) {
    await updateDiagram(props.graphData)
  }
}

async function updateDiagram(data) {
  if (!renderer || !data) return
  
  try {
    const result = await renderer.render(data)
    nodes.value = result.nodes
    edges.value = result.edges
  } catch (error) {
    console.error('[SmartDiagram] 渲染失败:', error)
  }
}

function fitView() {
  renderer?.fitView()
}

function zoomIn() {
  renderer?.zoomIn()
}

function zoomOut() {
  renderer?.zoomOut()
}

async function exportImage() {
  const dataUrl = await renderer?.exportImage()
  if (dataUrl) {
    const link = document.createElement('a')
    link.download = 'diagram.png'
    link.href = dataUrl
    link.click()
  }
}

defineExpose({
  fitView,
  zoomIn,
  zoomOut,
  exportImage,
  getRenderer: () => renderer
})
</script>

<style scoped>
.smart-diagram-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-radius: 8px;
  overflow: hidden;
}

.diagram-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.diagram-info {
  font-size: 12px;
  color: #999;
}

.diagram-canvas {
  flex: 1;
  position: relative;
  overflow: hidden;
}
</style>
