<template>
  <div>
    <div class="card-header">
      <span>{{ translateNodeName(elementType) }}</span>
    </div>
    <el-collapse v-model="activeName">
      <!-- 常规信息 -->
      <el-collapse-item name="common">
        <template #title>
          <i class="el-icon-info"></i> 常规信息
        </template>
        <common-panel :id="elementId" />
      </el-collapse-item>

      <!-- 任务配置 -->
      <el-collapse-item name="Task" v-if="elementType.includes('Task')">
        <template #title>
          <i class="el-icon-s-claim"></i> 任务配置
        </template>
        <user-task-panel :id="elementId" />
      </el-collapse-item>

      <!-- 表单配置 -->
      <el-collapse-item name="form" v-if="formVisible">
        <template #title>
          <i class="el-icon-s-order"></i> 表单配置
        </template>
        <form-panel :id="elementId" />
      </el-collapse-item>

      <!-- 执行监听器 -->
      <el-collapse-item name="executionListener">
        <template #title>
          <i class="el-icon-s-promotion"></i> 执行监听器
          <el-badge :value="executionListenerCount" class="item" type="primary" />
        </template>
        <execution-listener :id="elementId" @getExecutionListenerCount="handleGetExecutionListenerCount" />
      </el-collapse-item>

      <!-- 任务监听器 -->
      <el-collapse-item name="taskListener" v-if="elementType === 'UserTask'">
        <template #title>
          <i class="el-icon-s-flag"></i> 任务监听器
          <el-badge :value="taskListenerCount" class="item" type="primary" />
        </template>
        <task-listener :id="elementId" @getTaskListenerCount="handleGetTaskListenerCount" />
      </el-collapse-item>

      <!-- 多实例 -->
      <el-collapse-item name="multiInstance" v-if="elementType.includes('Task')">
        <template #title>
          <i class="el-icon-s-grid"></i> 多实例
        </template>
        <multi-instance :id="elementId" />
      </el-collapse-item>

      <!-- 流转条件 -->
      <el-collapse-item name="condition" v-if="conditionVisible">
        <template #title>
          <i class="el-icon-share"></i> 流转条件
        </template>
        <condition-panel :id="elementId" />
      </el-collapse-item>

      <!-- 扩展属性 -->
      <el-collapse-item name="properties">
        <template #title>
          <i class="el-icon-circle-plus"></i> 扩展属性
        </template>
        <properties-panel :id="elementId" />
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import ExecutionListener from './panel/executionListener.vue'
import TaskListener from './panel/taskListener.vue'
import MultiInstance from './panel/multiInstance.vue'
import CommonPanel from './panel/commonPanel.vue'
import UserTaskPanel from './panel/taskPanel.vue'
import ConditionPanel from './panel/conditionPanel.vue'
import FormPanel from './panel/formPanel.vue'
import PropertiesPanel from './panel/PropertiesPanel.vue'

import { translateNodeName } from './common/bpmnUtils'


// 响应式数据
const activeName = ref('common')
const executionListenerCount = ref(0)
const taskListenerCount = ref(0)
const elementId = ref('')
const elementType = ref('')
const conditionVisible = ref(false)
const formVisible = ref(false)

// 初始化方法
function initModels() {
  getActiveElement()
}

function getActiveElement() {
  initFormOnChanged(null)

  window.bpmnModeler.on('import.done', (e) => {
    initFormOnChanged(null)
  })

  window.bpmnModeler.on('selection.changed', ({ newSelection }) => {
    initFormOnChanged(newSelection[0] || null)
  })

  window.bpmnModeler.on('element.changed', ({ element }) => {
    if (element && element.id === elementId.value) {
      initFormOnChanged(element)
    }
  })
}

function initFormOnChanged(element) {
  let activatedElement = element

  if (!activatedElement) {
    activatedElement =
      window.bpmnModeler.get("elementRegistry").find((el) => el.type === 'bpmn:Process') ??
      window.bpmnModeler.get("elementRegistry").find((el) => el.type === 'bpmn:Collaboration')
  }

  if (!activatedElement) return

  window.bpmnElement = activatedElement;
  elementId.value = activatedElement.id
  elementType.value = activatedElement.type.split(':')[1] || ''

  conditionVisible.value =
    elementType.value === 'SequenceFlow' &&
    activatedElement.source &&
    !activatedElement.source.type.includes('StartEvent')

  formVisible.value = ['UserTask', 'StartEvent'].includes(elementType.value)
}

function handleGetExecutionListenerCount(value) {
  executionListenerCount.value = value
}

function handleGetTaskListenerCount(value) {
  taskListenerCount.value = value
}

// Watch elementId 变化
// watch(
//   () => elementId.value,
//   () => {
//     activeName.value = 'common'
//   }
// )

// 生命周期挂载
onMounted(() => {
  initModels()
})
</script>

<style lang="scss">
/* 保留原有样式 */
</style>