<template>
  <div v-loading="props.isView" class="flow-containers" :class="{ 'view-mode': props.isView }">
    <el-container style="height: 100%">
      <el-header style="border-bottom: 1px solid rgb(218 218 218); height: auto; padding-left: 0">
        <div style="display: flex; padding: 10px 0; justify-content: space-between;">
          <el-button-group>
            <el-upload action="" :before-upload="openBpmn" style="margin-right: 10px; display:inline-block;">
              <el-tooltip effect="dark" content="加载xml" placement="bottom">
                <el-button icon="FolderOpened" />
              </el-tooltip>
            </el-upload>
            <el-tooltip effect="dark" content="新建" placement="bottom">
              <el-button icon="CirclePlus" @click="newDiagram" />
            </el-tooltip>
            <el-tooltip effect="dark" content="自适应屏幕" placement="bottom">
              <el-button icon="Rank" @click="fitViewport" />
            </el-tooltip>
            <el-tooltip effect="dark" content="放大" placement="bottom">
              <el-button icon="ZoomIn" @click="zoomViewport(true)" />
            </el-tooltip>
            <el-tooltip effect="dark" content="缩小" placement="bottom">
              <el-button icon="ZoomOut" @click="zoomViewport(false)" />
            </el-tooltip>
            <el-tooltip effect="dark" content="后退" placement="bottom">
              <el-button icon="Back" @click="undo" />
            </el-tooltip>
            <el-tooltip effect="dark" content="前进" placement="bottom">
              <el-button icon="Right" @click="redo" />
            </el-tooltip>
          </el-button-group>

          <el-button-group>
            <el-button icon="View" @click="showXML">查看xml</el-button>
            <el-button icon="Download" @click="saveXML(true)">下载xml</el-button>
            <el-button icon="Picture" @click="saveImg('svg', true)">下载svg</el-button>
            <el-button type="primary" @click="save">保存模型</el-button>
            <el-button type="danger" @click="goBack">关闭</el-button>
          </el-button-group>
        </div>
      </el-header>

      <!-- 流程设计页面 -->
      <el-container style="align-items: stretch">
        <el-main>
          <div ref="canvasRef" class="canvas" />
        </el-main>

        <!--右侧属性栏-->
        <el-card shadow="never" class="normalPanel">
          <designer v-if="loadCanvas" />
        </el-card>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, markRaw } from 'vue'
import customTranslate from './customPanel/customTranslate'
import Modeler from 'bpmn-js/lib/Modeler'
import Designer from './designer.vue'
import getInitStr from './flowable/init'
import { StrUtil } from '@/utils/StrUtil'
import FlowableModule from './flowable/flowable.json'
import customControlsModule from './customPanel'

// props
const props = defineProps({
  xml: {
    type: String,
    default: ''
  },
  isView: {
    type: Boolean,
    default: false
  }
})

// refs
const canvasRef = ref(null)
const zoom = ref(1)
const loadCanvas = ref(false)


// additionalModules 计算属性
const additionalModules = [
  customControlsModule,
  {
    translate: ['value', customTranslate]
  }
]

// 监听 xml 变化，创建流程图
watch(
  () => props.xml,
  (newVal) => {
    if (StrUtil.isNotBlank(newVal)) {
      createNewDiagram(newVal)
    } else {
      newDiagram()
    }
  },
  // { immediate: true }
)

// 创建 Modeler 实例
function initModeler() {
  const bpmnModeler = markRaw(
    new Modeler({
      container: canvasRef.value,
      additionalModules: additionalModules,
      moddleExtensions: {
        flowable: FlowableModule
      },
      keyboard: { bindTo: document }
    })
  )

  window.bpmnModeler = bpmnModeler

  // 初始化流程图
  if (StrUtil.isBlank(props.xml)) {
    newDiagram()
  } else {
    createNewDiagram(props.xml)
  }
}

// 新建流程图
function newDiagram() {
  createNewDiagram(getInitStr())
}

// 根据 XML 创建流程图
async function createNewDiagram(data) {
  data = data.replace(/<!$CDATA$$(.+?)$$/g, (match, str) => str.replace(/</g, '&lt;'))
  try {
    await window.bpmnModeler.importXML(data)
    fitViewport()
  } catch (err) {
    console.error(err.message, err.warnings)
  }
}

// 自适应视口
function fitViewport() {
  const canvas = window.bpmnModeler.get('canvas')
  zoom.value = canvas.zoom('fit-viewport')

  const bbox = document.querySelector('.flow-containers .viewport')?.getBBox()
  const currentViewBox = canvas.viewbox()

  if (bbox && currentViewBox) {
    const elementMid = {
      x: bbox.x + bbox.width / 2 - 65,
      y: bbox.y + bbox.height / 2
    }

    canvas.viewbox({
      x: elementMid.x - currentViewBox.width / 2,
      y: elementMid.y - currentViewBox.height / 2,
      width: currentViewBox.width,
      height: currentViewBox.height
    })

    zoom.value = (bbox.width / currentViewBox.width) * 1.8
  }

  loadCanvas.value = true
}

// 放大/缩小
function zoomViewport(zoomIn = true) {
  const canvas = window.bpmnModeler.get('canvas')
  zoom.value = canvas.zoom()
  zoom.value += zoomIn ? 0.1 : -0.1
  canvas.zoom(zoom.value)
}

// 撤销
function undo() {
  window.bpmnModeler.get('commandStack').undo()
}

// 重做
function redo() {
  window.bpmnModeler.get('commandStack').redo()
}

// 获取流程基础信息
function getProcess() {
  const element = getProcessElement()
  return {
    id: element.id,
    name: element.name,
    category: element.processCategory
  }
}

// 获取流程主节点
function getProcessElement() {
  const definitions = window.bpmnModeler.getDefinitions()
  const rootElements = definitions.rootElements
  for (let i = 0; i < rootElements.length; i++) {
    if (rootElements[i].$type === 'bpmn:Process') return rootElements[i]
  }
  return null
}

// 保存 XML
async function saveXML(download = false) {
  try {
    const { xml } = await window.bpmnModeler.saveXML({ format: true })
    if (download) {
      downloadFile(`${getProcessElement().name}.bpmn20.xml`, xml, 'application/xml')
    }
    return xml
  } catch (err) {
    console.error(err)
  }
}

// 查看 XML
async function showXML() {
  try {
    const xmlStr = await saveXML()
    emit('showXML', xmlStr)
  } catch (err) {
    console.error(err)
  }
}

// 保存 SVG
async function saveImg(type = 'svg', download = false) {
  try {
    const { svg } = await window.bpmnModeler.saveSVG({ format: true })
    if (download) {
      downloadFile(getProcessElement().name, svg, 'image/svg+xml')
    }
    return svg
  } catch (err) {
    console.error(err)
  }
}

// 保存流程图
async function save() {
  const process = getProcess()
  const xml = await saveXML()
  const svg = await saveImg()
  const result = { process, xml, svg }
  emit('save', result)
  window.parent.postMessage(result, '*')
  goBack()
}

// 打开 BPMN 文件
function openBpmn(file) {
  const reader = new FileReader()
  reader.readAsText(file, 'utf-8')
  reader.onload = () => {
    createNewDiagram(reader.result)
  }
  return false
}

// 下载文件
function downloadFile(filename, data, type) {
  const a = document.createElement('a')
  const url = window.URL.createObjectURL(new Blob([data], { type }))
  a.href = url
  a.download = filename
  a.click()
  window.URL.revokeObjectURL(url)
}

// 返回上一页
function goBack() {
  // const obj = { path: '/flowable/definition', query: { t: Date.now() } }
  // window.$tab.closeOpenPage(obj)
  emit('goBack')
}

// 挂载时初始化
onMounted(() => {
  initModeler()
})

const emit = defineEmits(['save', 'showXML', 'goBack'])
</script>

<style lang="scss">
/*左边工具栏以及编辑节点的样式*/
@import "bpmn-js/dist/assets/diagram-js.css";
@import "bpmn-js/dist/assets/bpmn-font/css/bpmn.css";
@import "bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css";
@import "bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css";

//@import "bpmn-js-bpmnlint/dist/assets/css/bpmn-js-bpmnlint.css";
.view-mode {

  .el-header,
  .el-aside,
  .djs-palette,
  .bjs-powered-by {
    display: none;
  }

  .el-loading-mask {
    background-color: initial;
  }

  .el-loading-spinner {
    display: none;
  }
}

.flow-containers {
  width: 100%;
  height: 100%;

  .canvas {
    // min-height: calc(100% - 100px);
    width: 100%;
    height: calc(100vh - 200px);
    ;
    background: url("data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImEiIHdpZHRoPSI0MCIgaGVpZ2h0PSI0MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTTAgMTBoNDBNMTAgMHY0ME0wIDIwaDQwTTIwIDB2NDBNMCAzMGg0ME0zMCAwdjQwIiBmaWxsPSJub25lIiBzdHJva2U9IiNlMGUwZTAiIG9wYWNpdHk9Ii4yIi8+PHBhdGggZD0iTTQwIDBIMHY0MCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjZTBlMGUwIi8+PC9wYXR0ZXJuPjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI2EpIi8+PC9zdmc+")
  }

  .panel {
    position: absolute;
    right: 0;
    top: 50px;
    width: 300px;
  }

  .load {
    margin-right: 10px;
  }

  .normalPanel {
    width: 460px;
    height: 100%;
    padding: 20px 20px;
  }

  .el-main {
    position: relative;
    padding: 0;
  }

  .el-main .button-group {
    display: flex;
    flex-direction: column;
    position: absolute;
    width: auto;
    height: auto;
    top: 10px;
    right: 10px;
  }

  .button-group .el-button {
    width: 100%;
    margin: 0 05px;
  }
}
</style>
