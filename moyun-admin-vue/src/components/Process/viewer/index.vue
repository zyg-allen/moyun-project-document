<template>
  <div class="containers">
    <el-container style="align-items: stretch">
      <el-main class="flow-viewer">
        <div class="process-status">
          <span class="intro">状态：</span>
          <div class="finish">已办理</div>
          <div class="processing">处理中</div>
          <div class="todo">未进行</div>
        </div>

        <!-- 流程图显示 -->
        <div v-loading="loading" class="canvas" ref="flowCanvas"></div>

        <!-- 按钮区域 -->
        <el-button-group class="button-group">
          <el-tooltip effect="dark" content="适中" placement="bottom">
            <el-button icon="rank" @click="fitViewport" />
          </el-tooltip>
          <el-tooltip effect="dark" content="放大" placement="bottom">
            <el-button icon="zoom-in" @click="zoomViewport(true)" />
          </el-tooltip>
          <el-tooltip effect="dark" content="缩小" placement="bottom">
            <el-button icon="zoom-out" @click="zoomViewport(false)" />
          </el-tooltip>
        </el-button-group>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { CustomViewer as BpmnViewer } from "@/components/Process/common"

// 接收 props
const props = defineProps({
  // 回显数据传值
  flowData: {
    type: Object,
    default: () => ({}),
    required: false
  },
  procInsId: {
    type: String,
    default: ''
  }
})

// 数据定义
const bpmnViewer = ref(null)
const loading = ref(true)
const flowCanvas = ref(null)
/**
 * 加载流程图片
 */
async function loadFlowCanvas(flowData) {
  try {
    await bpmnViewer.value.importXML(flowData.xmlData)
    await fitViewport()

    // 流程线高亮设置
    if (flowData.nodeData !== undefined && flowData.nodeData.length > 0 && props.procInsId) {
      await fillColor(flowData.nodeData)
    }
  } catch (err) {
    console.error(err.message, err.warnings)
  }
}

/**
 * 让图能自适应屏幕
 */
function fitViewport() {
  const canvas = bpmnViewer.value.get('canvas')
  canvas.zoom("fit-viewport", "auto")
  loading.value = false
}

/**
 * 放大 / 缩小
 */
function zoomViewport(zoomIn = true) {
  const canvas = bpmnViewer.value.get('canvas')
  let currentZoom = canvas.zoom()
  currentZoom += (zoomIn ? 0.1 : -0.1)
  if (currentZoom >= 0.2) {
    canvas.zoom(currentZoom)
  }
}

/**
 * 设置高亮颜色
 */
function fillColor(nodeData) {
  const canvas = bpmnViewer.value.get('canvas')
  const rootElements = bpmnViewer.value.getDefinitions().rootElements
  const flowElements = rootElements[0].flowElements

  flowElements.forEach(n => {
    const completeTask = nodeData.find(m => m.key === n.id)
    const todoTask = nodeData.find(m => !m.completed)
    const endTask = nodeData[nodeData.length - 1]

    if (n.$type === 'bpmn:UserTask') {
      if (completeTask) {
        canvas.addMarker(n.id, completeTask.completed ? 'highlight' : 'highlight-todo')

        if (n.outgoing) {
          n.outgoing.forEach(nn => {
            const targetTask = nodeData.find(m => m.key === nn.targetRef.id)
            if (targetTask) {
              if (todoTask && completeTask.key === todoTask.key && !todoTask.completed) {
                canvas.addMarker(nn.id, todoTask.completed ? 'highlight' : 'highlight-todo')
                canvas.addMarker(nn.targetRef.id, todoTask.completed ? 'highlight' : 'highlight-todo')
              } else {
                canvas.addMarker(nn.id, targetTask.completed ? 'highlight' : 'highlight-todo')
                canvas.addMarker(nn.targetRef.id, targetTask.completed ? 'highlight' : 'highlight-todo')
              }
            }
          })
        }
      }

    } else if (n.$type === 'bpmn:ExclusiveGateway') {
      if (completeTask) {
        canvas.addMarker(n.id, completeTask.completed ? 'highlight' : 'highlight-todo')
        n.outgoing?.forEach(nn => {
          const targetTask = nodeData.find(m => m.key === nn.targetRef.id)
          if (targetTask) {
            canvas.addMarker(nn.id, targetTask.completed ? 'highlight' : 'highlight-todo')
            canvas.addMarker(nn.targetRef.id, targetTask.completed ? 'highlight' : 'highlight-todo')
          }
        })
      }

    } else if (n.$type === 'bpmn:ParallelGateway') {
      if (completeTask) {
        canvas.addMarker(n.id, completeTask.completed ? 'highlight' : 'highlight-todo')
        n.outgoing?.forEach(nn => {
          const targetTask = nodeData.find(m => m.key === nn.targetRef.id)
          if (targetTask) {
            canvas.addMarker(nn.id, targetTask.completed ? 'highlight' : 'highlight-todo')
            canvas.addMarker(nn.targetRef.id, targetTask.completed ? 'highlight' : 'highlight-todo')
          }
        })
      }

    } else if (n.$type === 'bpmn:StartEvent') {
      n.outgoing?.forEach(nn => {
        const completeTask = nodeData.find(m => m.key === nn.targetRef.id)
        if (completeTask) {
          canvas.addMarker(nn.id, 'highlight')
          canvas.addMarker(n.id, 'highlight')
          return
        }
      })

    } else if (n.$type === 'bpmn:EndEvent') {
      if (endTask.key === n.id && endTask.completed) {
        canvas.addMarker(n.id, 'highlight')
        return
      }
    }
  })
}

// 监听 flowData 变化，重新加载流程图
watch(
  () => props.flowData,
  newValue => {
    if (Object.keys(newValue).length > 0) {
      // 销毁旧实例
      if (bpmnViewer.value) {
        bpmnViewer.value.destroy()
      }

      // 创建新实例
      bpmnViewer.value = new BpmnViewer({
        container: flowCanvas.value,
        height: 'calc(100vh - 200px)'
      })

      loadFlowCanvas(newValue)
    }
  },
  { deep: true, immediate: true }
)

// 组件卸载前销毁 bpmnViewer
onBeforeUnmount(() => {
  if (bpmnViewer.value) {
    bpmnViewer.value.destroy()
  }
})
</script>

<style lang="scss">
@import "../style/flow-viewer.scss";
</style>
