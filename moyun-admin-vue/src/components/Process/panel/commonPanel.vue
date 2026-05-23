<template>
  <div>
    <el-form :model="bpmnFormData" label-width="80px" :rules="rules">
      <el-form-item
        :label="bpmnFormData.$type === 'bpmn:Process' ? '流程标识' : '节点ID'"
        prop="id"
        @change="updateElementTask('id')"
      >
        <el-input v-model="bpmnFormData.id" />
      </el-form-item>
      <el-form-item
        :label="bpmnFormData.$type === 'bpmn:Process' ? '流程名称' : '节点名称'"
        prop="name"
      >
        <el-input v-model="bpmnFormData.name" @change="updateElementTask('name')" />
      </el-form-item>

      <!--流程的基础属性-->
      <template v-if="bpmnFormData.$type === 'bpmn:Process'">
        <el-form-item label="流程分类" prop="processCategory">
          <el-select
            v-model="bpmnFormData.processCategory"
            placeholder="请选择流程分类"
            @change="updateElementTask('processCategory')"
          >
            <el-option
              v-for="dict in dict.sys_process_category"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item v-if="bpmnFormData.$type === 'bpmn:SubProcess'" label="状态">
        <el-switch
          v-model="bpmnFormData.isExpanded"
          active-text="展开"
          inactive-text="折叠"
          @change="updateElementTask('isExpanded')"
        />
      </el-form-item>
      <el-form-item label="节点描述">
        <el-input
          :rows="2"
          type="textarea"
          v-model="bpmnFormData.documentationValue"
          @change="updateDocumentation"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, watch, getCurrentInstance } from 'vue'
import { StrUtil } from '@/utils/StrUtil'

// 接收父组件传值
const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

// 定义 emit
const emit = defineEmits(['save'])

// 表单验证规则
const rules = ref({
  id: [
    { required: true, message: '节点Id 不能为空', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '节点名称不能为空', trigger: 'blur' }
  ]
})

// 数据定义
const bpmnFormData = ref({})
const dict = ref({})

const { proxy } = getCurrentInstance()

// created 阶段：获取字典数据
dict.value = proxy.useDict('sys_process_category')

// Watch id 变化，触发表单重置
watch(() => props.id, (newVal) => {
  if (StrUtil.isNotBlank(newVal)) {
    resetTaskForm()
  }
}, { immediate: true })

// 方法定义

/**
 * 重置任务表单数据
 */
function resetTaskForm() {
  // this.bpmnFormData = JSON.parse(JSON.stringify(window.bpmnElement.businessObject));
  bpmnFormData.value = Object.assign({}, window.bpmnElement.businessObject)

  // 使用 $set 确保 documentationValue 是响应式的
  bpmnFormData.value['documentationValue'] =
    window.bpmnElement.businessObject.documentation?.[0]?.text || ''
}

/**
 * 更新 BPMN 元素属性
 * @param key 属性名
 */
function updateElementTask(key) {
  const taskAttr = Object.create(null)
  taskAttr[key] = bpmnFormData.value[key] || null
  window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, taskAttr)
}

/**
 * 更新文档内容
 */
function updateDocumentation() {
  // 确保 modelerStore 是 BPMN.js 的 Modeler 实例
  const modeler = window.bpmnModeler // 获取实际的 modeler 实例
  const moddle = modeler.get('moddle') // 通过 modeler 获取 moddle
  const modeling = modeler.get('modeling') // 通过 modeler 获取 modeling

  // 创建新的文档对象
  const documentation = moddle.create('bpmn:Documentation', {
    text: bpmnFormData.value.documentationValue
  })

  // 获取当前元素的扩展元素
  let extensionElements = window.bpmnElement.businessObject.extensionElements

  if (!extensionElements) {
    // 如果没有扩展元素，创建一个新的
    extensionElements = moddle.create('bpmn:ExtensionElements', {
      values: []
    })
  }

  // 更新文档
  modeling.updateProperties(window.bpmnElement, {
    documentation: [documentation],
    extensionElements: extensionElements
  })

  // 强制更新模型
  window.bpmnModeler.get('commandStack').execute('element.updateProperties', {
    element: window.bpmnElement,
    properties: {
      documentation: [documentation]
    }
  })

  emit('save')
}
</script>