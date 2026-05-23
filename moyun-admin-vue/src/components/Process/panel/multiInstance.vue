<template>
  <div class="panel-tab__content">
    <el-form label-width="70px" @submit.native.prevent>
      <el-form-item label="参数说明">
        <el-button type="primary" @click="dialogVisible = true">查看</el-button>
      </el-form-item>
      <el-form-item label="回路特性">
        <el-select v-model="loopCharacteristics" @change="changeLoopCharacteristicsType">
          <!--bpmn:MultiInstanceLoopCharacteristics-->
          <el-option label="并行多重事件" value="ParallelMultiInstance" />
          <el-option label="时序多重事件" value="SequentialMultiInstance" />
          <!--bpmn:StandardLoopCharacteristics-->
          <el-option label="循环事件" value="StandardLoop" />
          <el-option label="无" value="Null" />
        </el-select>
      </el-form-item>
      <template
        v-if="loopCharacteristics === 'ParallelMultiInstance' || loopCharacteristics === 'SequentialMultiInstance'">
        <el-form-item label="循环基数" key="loopCardinality">
          <el-input v-model="loopInstanceForm.loopCardinality" clearable @change="updateLoopCardinality" />
        </el-form-item>
        <el-form-item label="集合" key="collection">
          <el-input v-model="loopInstanceForm.collection" clearable @change="updateLoopBase" />
        </el-form-item>
        <el-form-item label="元素变量" key="elementVariable">
          <el-input v-model="loopInstanceForm.elementVariable" clearable @change="updateLoopBase" />
        </el-form-item>
        <el-form-item label="完成条件" key="completionCondition">
          <el-input v-model="loopInstanceForm.completionCondition" clearable @change="updateLoopCondition" />
        </el-form-item>
        <el-form-item label="异步状态" key="async">
          <el-checkbox v-model="loopInstanceForm.asyncBefore" label="异步前"
            @change="updateLoopAsync('asyncBefore')" />
          <el-checkbox v-model="loopInstanceForm.asyncAfter" label="异步后"
            @change="updateLoopAsync('asyncAfter')" />
          <el-checkbox v-model="loopInstanceForm.exclusive"
            v-if="loopInstanceForm.asyncAfter || loopInstanceForm.asyncBefore" label="排除"
            @change="updateLoopAsync('exclusive')" />
        </el-form-item>
        <el-form-item label="重试周期" prop="timeCycle"
          v-if="loopInstanceForm.asyncAfter || loopInstanceForm.asyncBefore" key="timeCycle">
          <el-input v-model="loopInstanceForm.timeCycle" clearable @change="updateLoopTimeCycle" />
        </el-form-item>
      </template>
    </el-form>

    <!-- 参数说明 -->
    <el-dialog title="多实例参数" v-model="dialogVisible" width="680px" @closed="$emit('close')">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="使用说明">按照BPMN2.0规范的要求，用于为每个实例创建执行的父执行，会提供下列变量:</el-descriptions-item>
        <el-descriptions-item label="collection(集合变量)">传入List参数, 一般为用户ID集合</el-descriptions-item>
        <el-descriptions-item label="elementVariable(元素变量)">List中单个参数的名称</el-descriptions-item>
        <el-descriptions-item label="loopCardinality(基数)">List循环次数</el-descriptions-item>
        <el-descriptions-item label="isSequential(串并行)">Parallel: 并行多实例，Sequential: 串行多实例</el-descriptions-item>
        <el-descriptions-item label="completionCondition(完成条件)">任务出口条件</el-descriptions-item>
        <el-descriptions-item label="nrOfInstances(实例总数)">实例总数</el-descriptions-item>
        <el-descriptions-item label="nrOfActiveInstances(未完成数)">当前活动的（即未完成的），实例数量。对于顺序多实例，这个值总为1</el-descriptions-item>
        <el-descriptions-item label="nrOfCompletedInstances(已完成数)">已完成的实例数量</el-descriptions-item>
        <el-descriptions-item label="loopCounter">给定实例在for-each循环中的index</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
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

// 定义 emit
const emit = defineEmits(['close'])

// 获取 modeler 实例
const { proxy } = getCurrentInstance()

// 数据定义
const dialogVisible = ref(false)
const loopCharacteristics = ref('')
const loopInstanceForm = ref({})
const multiLoopInstance = ref(null)
const defaultLoopInstanceForm = {
  completionCondition: '',
  loopCardinality: '',
  extensionElements: [],
  asyncAfter: false,
  asyncBefore: false,
  exclusive: false
}

/**
 * 根据 BPMN 元素初始化多实例配置
 */
function getElementLoop(businessObject) {
  if (!businessObject.loopCharacteristics) {
    loopCharacteristics.value = 'Null'
    loopInstanceForm.value = {}
    return
  }

  if (businessObject.loopCharacteristics.$type === 'bpmn:StandardLoopCharacteristics') {
    loopCharacteristics.value = 'StandardLoop'
    loopInstanceForm.value = {}
    return
  }

  if (businessObject.loopCharacteristics.isSequential) {
    loopCharacteristics.value = 'SequentialMultiInstance'
  } else {
    loopCharacteristics.value = 'ParallelMultiInstance'
  }

  // 合并配置
  loopInstanceForm.value = {
    ...defaultLoopInstanceForm,
    ...businessObject.loopCharacteristics,
    completionCondition: businessObject.loopCharacteristics?.completionCondition?.body ?? '',
    loopCardinality: businessObject.loopCharacteristics?.loopCardinality?.body ?? ''
  }

  // 保留当前元素 businessObject 上的 loopCharacteristics 实例
  multiLoopInstance.value = window.bpmnElement.businessObject.loopCharacteristics

  // 更新表单
  if (
    businessObject.loopCharacteristics.extensionElements &&
    businessObject.loopCharacteristics.extensionElements.values &&
    businessObject.loopCharacteristics.extensionElements.values.length
  ) {
    loopInstanceForm.value['timeCycle'] = businessObject.loopCharacteristics.extensionElements.values[0].body
  }
}

/**
 * 回路类型切换处理
 */
function changeLoopCharacteristicsType(type) {
  // 切换类型取消原表单配置
  loopInstanceForm.value = { ...defaultLoopInstanceForm }

  // 取消多实例配置
  if (type === 'Null') {
    window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, { loopCharacteristics: null })
    return
  }

  // 配置循环
  if (type === 'StandardLoop') {
    const loopCharacteristicsObject = window.bpmnModeler.get('moddle').create('bpmn:StandardLoopCharacteristics')
    window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, {
      loopCharacteristics: loopCharacteristicsObject
    })
    multiLoopInstance.value = null
    return
  }

  // 时序或并行多实例
  if (type === 'SequentialMultiInstance') {
    multiLoopInstance.value = window.bpmnModeler.get('moddle').create('bpmn:MultiInstanceLoopCharacteristics', {
      isSequential: true
    })
  } else {
    multiLoopInstance.value = window.bpmnModeler.get('moddle').create('bpmn:MultiInstanceLoopCharacteristics')
  }

  window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, {
    loopCharacteristics: multiLoopInstance.value
  })
}

/**
 * 更新循环基数
 */
function updateLoopCardinality(cardinality) {
  let loopCardinality = null
  if (cardinality && cardinality.length) {
    loopCardinality = window.bpmnModeler.get('moddle').create('bpmn:FormalExpression', { body: cardinality })
  }
  window.bpmnModeler.get('modeling').updateModdleProperties(window.bpmnElement, multiLoopInstance.value, {
    loopCardinality
  })
}

/**
 * 更新完成条件
 */
function updateLoopCondition(condition) {
  let completionCondition = null
  if (condition && condition.length) {
    completionCondition = window.bpmnModeler.get('moddle').create('bpmn:FormalExpression', { body: condition })
  }
  window.bpmnModeler.get('modeling').updateModdleProperties(window.bpmnElement, multiLoopInstance.value, {
    completionCondition
  })
}

/**
 * 更新重试周期
 */
function updateLoopTimeCycle(timeCycle) {
  const extensionElements = window.bpmnModeler.get('moddle').create('bpmn:ExtensionElements', {
    values: [
      window.bpmnModeler.get('moddle').create(`flowable:FailedJobRetryTimeCycle`, {
        body: timeCycle
      })
    ]
  })

  window.bpmnModeler.get('modeling').updateModdleProperties(window.bpmnElement, multiLoopInstance.value, {
    extensionElements
  })
}

/**
 * 更新基础字段（collection / elementVariable）
 */
function updateLoopBase() {
  window.bpmnModeler.get('modeling').updateModdleProperties(window.bpmnElement, multiLoopInstance.value, {
    collection: loopInstanceForm.value.collection || null,
    elementVariable: loopInstanceForm.value.elementVariable || null
  })
}

/**
 * 更新异步状态
 */
function updateLoopAsync(key) {
  const { asyncBefore, asyncAfter } = loopInstanceForm.value
  let asyncAttr = Object.create(null)

  if (!asyncBefore && !asyncAfter) {
    loopInstanceForm.value['exclusive'] = false
    asyncAttr = { asyncBefore: false, asyncAfter: false, exclusive: false, extensionElements: null }
  } else {
    asyncAttr[key] = loopInstanceForm.value[key]
  }

  window.bpmnModeler.get('modeling').updateModdleProperties(window.bpmnElement, multiLoopInstance.value, asyncAttr)
}

// 监听 id 变化，重新加载数据
watch(() => props.id, newVal => {
  if (StrUtil.isNotBlank(newVal)) {
    getElementLoop(window.bpmnElement.businessObject)
  }
}, { immediate: true })
</script>

<style lang="scss">
@import '../style/process-panel';
</style>