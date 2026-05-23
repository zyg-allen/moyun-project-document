<template>
  <div>
    <el-form label-width="80px" @submit.native.prevent>
      <el-form-item label="流程表单">
        <el-select v-model="bpmnFormData.formKey" clearable class="m-2" placeholder="挂载节点表单"
          @change="updateElementFormKey">
          <el-option v-for="item in formList" :key="item.value" :label="item.formName" :value="item.formId" />
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { listAllForm } from '@/api/flowable/form'
import { StrUtil } from '@/utils/StrUtil'

// 接收父组件传值
const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

// 数据定义
const formList = ref([]) // 表单数据
const bpmnFormData = ref({})

// 获取当前实例
const { proxy } = getCurrentInstance()

/**
 * 重置流程表单字段
 */
function resetFlowForm() {
  bpmnFormData.value.formKey = window.bpmnElement.businessObject.formKey
}

/**
 * 更新 BPMN 元素的 formKey 属性
 */
function updateElementFormKey(val) {
  if (StrUtil.isBlank(val)) {
    delete window.bpmnElement.businessObject['formKey']
  } else {
    window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, { 'formKey': val })
  }
}

/**
 * 获取所有表单数据
 */
function getListForm() {
  listAllForm().then(res => {
    res.data.forEach(item => {
      item.formId = item.formId.toString()
    })
    formList.value = res.data
  })
}

// 监听 id 变化，触发初始化加载
watch(() => props.id, (newVal) => {
  if (StrUtil.isNotBlank(newVal)) {
    getListForm()
    resetFlowForm()
  }
}, { immediate: true })

// 初始化时也执行一次
resetFlowForm()
getListForm()
</script>