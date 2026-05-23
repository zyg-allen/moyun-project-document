<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入表达式名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
        <el-button icon="refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="expressionList" @current-change="handleSingleExpSelect">
      <el-table-column width="55" align="center">
        <template #="scope">
          <!-- 可以手动的修改label的值，从而控制选择哪一项 -->
          <el-radio v-model="radioSelected" :label="scope.row.id">{{ '' }}</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="名称" align="center" prop="name" />
      <el-table-column label="表达式内容" align="center" prop="expression" />
      <el-table-column label="表达式类型" align="center" prop="dataType">
        <template #="scope">
          <dict-tag :options="dict.exp_data_type" :value="scope.row.dataType" />
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page-sizes="[5, 10]" layout="prev, pager, next"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup>
import { ref, reactive, watch, getCurrentInstance } from 'vue'
import { listExpression } from '@/api/flowable/expression'
import { StrUtil } from '@/utils/StrUtil'

// 接受父组件的值
const props = defineProps({
  // 回显数据传值
  selectValues: {
    type: [Number, String],
    default: null,
    required: false
  }
})

// 定义 emit
const emit = defineEmits(['handleSingleExpSelect'])

// 数据定义
const loading = ref(true)
const total = ref(0)
const expressionList = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: null,
  expression: null,
  status: null
})
const radioSelected = ref(null) // 单选框传值
const dict = ref({})
const { proxy } = getCurrentInstance()

// Watchers
watch(() => props.selectValues, (newVal) => {
  if (StrUtil.isNotBlank(newVal)) {
    radioSelected.value = newVal
  }
}, { immediate: true })

// 生命周期 created
dict.value = proxy.useDict('sys_common_status', 'exp_data_type')
getList()

// Methods
function getList() {
  loading.value = true
  listExpression(queryParams).then(response => {
    expressionList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryForm')
  handleQuery()
}

function handleSingleExpSelect(selection) {
  radioSelected.value = selection.id // 点击当前行时,radio同样有选中效果
  emit('handleSingleExpSelect', selection)
}
</script>