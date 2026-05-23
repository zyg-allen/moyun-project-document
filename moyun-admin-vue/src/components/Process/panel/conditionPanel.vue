<template>
  <div>
    <el-form label-width="100px" @submit.native.prevent>
      <el-form-item>
        <template #label>
          <span>
            流转类型
            <el-tooltip placement="top">
              <template #content>
                <div>
                  普通流转路径：流程执行过程中，一个元素被访问后，会沿着其所有出口顺序流继续执行。
                  <br />默认流转路径：只有当没有其他顺序流可以选择时，才会选择默认顺序流作为活动的出口顺序流。流程会忽略默认顺序流上的条件。
                  <br />条件流转路径：是计算其每个出口顺序流上的条件。当条件计算为true时，选择该出口顺序流。如果该方法选择了多条顺序流，则会生成多个执行，流程会以并行方式继续。
                </div>
              </template>
              <i class="el-icon-question" />
            </el-tooltip>
          </span>
        </template>
        <el-select v-model="bpmnFormData.type" @change="updateFlowType">
          <el-option label="普通流转路径" value="normal" />
          <el-option label="默认流转路径" value="default" />
          <el-option label="条件流转路径" value="condition" />
        </el-select>
      </el-form-item>

      <el-form-item label="条件格式" v-if="bpmnFormData.type === 'condition'" key="condition">
        <el-select v-model="bpmnFormData.conditionType">
          <el-option label="表达式" value="expression" />
          <el-option label="脚本" value="script" />
        </el-select>
      </el-form-item>

      <el-form-item label="表达式"
        v-if="bpmnFormData.conditionType && bpmnFormData.conditionType === 'expression'" key="express">
        <el-input v-model="bpmnFormData.body" clearable @change="updateFlowCondition" />
      </el-form-item>

      <template v-if="bpmnFormData.conditionType && bpmnFormData.conditionType === 'script'">
        <el-form-item label="脚本语言" key="language">
          <el-input v-model="bpmnFormData.language" clearable @change="updateFlowCondition" />
        </el-form-item>
        <el-form-item label="脚本类型" key="scriptType">
          <el-select v-model="bpmnFormData.scriptType">
            <el-option label="内联脚本" value="inlineScript" />
            <el-option label="外部脚本" value="externalScript" />
          </el-select>
        </el-form-item>
        <el-form-item label="脚本" v-if="bpmnFormData.scriptType === 'inlineScript'" key="body">
          <el-input v-model="bpmnFormData.body" type="textarea" clearable @change="updateFlowCondition" />
        </el-form-item>
        <el-form-item label="资源地址" v-if="bpmnFormData.scriptType === 'externalScript'" key="resource">
          <el-input v-model="bpmnFormData.resource" clearable @change="updateFlowCondition" />
        </el-form-item>
      </template>
    </el-form>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { StrUtil } from '@/utils/StrUtil'

// 接收 props
const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

// 获取 modeler 实例
const { proxy } = getCurrentInstance()

// 数据定义
const bpmnElementSource = ref({})
const bpmnElementSourceRef = ref({})
const bpmnFormData = ref({
  body: null
})

/**
 * 根据 BPMN 元素初始化流转条件配置
 */
function resetFlowCondition() {
  // 初始化表单数据
  bpmnFormData.value = {
    body: null
  }

  // 获取源节点和源引用
  bpmnElementSource.value = window.bpmnElement.source
  bpmnElementSourceRef.value = window.bpmnElement.businessObject.sourceRef

  // 判断当前是否为默认流转路径
  if (
    window.bpmnElement.businessObject.sourceRef &&
    window.bpmnElement.businessObject.sourceRef.default &&
    window.bpmnElement.businessObject.sourceRef.default.id === window.bpmnElement.id
  ) {
    bpmnFormData.value['type'] = 'default'
    return
  }

  // 判断是否无条件表达式，即普通流转路径
  if (!window.bpmnElement.businessObject.conditionExpression) {
    bpmnFormData.value['type'] = 'normal'
    return
  }

  // 带条件的情况
  const conditionExpression = window.bpmnElement.businessObject.conditionExpression
  bpmnFormData.value = { ...conditionExpression, type: 'condition' }

  // resource 可直接标识是否为外部脚本
  if (bpmnFormData.value.resource) {
    bpmnFormData.value['conditionType'] = 'script'
    bpmnFormData.value['scriptType'] = 'externalScript'
    return
  }

  // language 存在表示为内联脚本
  if (conditionExpression.language) {
    bpmnFormData.value['conditionType'] = 'script'
    bpmnFormData.value['scriptType'] = 'inlineScript'
    return
  }

  // 默认为表达式
  bpmnFormData.value['conditionType'] = 'expression'
}

/**
 * 更新流转类型
 */
function updateFlowType(flowType) {
  // 条件流转路径
  if (flowType === 'condition') {
    const flowConditionRef = window.bpmnModeler.get('moddle').create('bpmn:FormalExpression')
    window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, {
      conditionExpression: flowConditionRef
    })
    return
  }

  // 默认流转路径
  if (flowType === 'default') {
    window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, {
      conditionExpression: null
    })

    window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement.source, {
      default: window.bpmnElement
    })

    // 清空条件格式
    bpmnFormData.value.conditionType = null
    return
  }

  // 普通流转路径
  if (flowType === 'normal') {
    // 清空条件格式
    bpmnFormData.value.conditionType = null

    // 如果来源节点的默认路径是当前连线，清除父元素的默认路径配置
    if (
      window.bpmnElement.businessObject.sourceRef.default &&
      window.bpmnElement.businessObject.sourceRef.default.id === window.bpmnElement.id
    ) {
      window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement.source, {
        default: null
      })
    }

    window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, {
      conditionExpression: null
    })
  }
}

/**
 * 更新流转条件表达式或脚本
 */
function updateFlowCondition() {
  const { conditionType, scriptType, body, resource, language } = bpmnFormData.value
  let condition

  if (conditionType === 'expression') {
    condition = window.bpmnModeler.get('moddle').create('bpmn:FormalExpression', { body })
  } else {
    if (scriptType === 'inlineScript') {
      condition = window.bpmnModeler.get('moddle').create('bpmn:FormalExpression', { body, language })
      bpmnFormData.value.resource = ''
    } else {
      bpmnFormData.value.body = ''
      condition = window.bpmnModeler.get('moddle').create('bpmn:FormalExpression', { resource, language })
    }
  }

  window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, {
    conditionExpression: condition
  })
}

// 监听 id 变化，重新加载数据
watch(
  () => props.id,
  newVal => {
    if (StrUtil.isNotBlank(newVal)) {
      resetFlowCondition()
    }
  },
  { immediate: true }
)
</script>