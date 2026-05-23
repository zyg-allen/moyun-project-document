<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true">
      <el-form-item label="角色名称" prop="roleName">
        <el-input v-model="queryParams.roleName" placeholder="请输入角色名称" clearable style="width: 240px"
          @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
        <el-button icon="refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-show="checkType === 'multiple'" ref="dataTable" v-loading="loading" :data="roleList"
      @selection-change="handleMultipleRoleSelect">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="角色编号" prop="roleId" width="120" />
      <el-table-column label="角色名称" prop="roleName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="权限字符" prop="roleKey" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="显示顺序" prop="roleSort" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
    </el-table>
    <el-table v-show="checkType === 'single'" v-loading="loading" :data="roleList"
      @current-change="handleSingleRoleSelect">
      <el-table-column width="55" align="center">
        <template #="scope">
          <!-- 可以手动的修改label的值，从而控制选择哪一项 -->
          <el-radio v-model="radioSelected" :label="scope.row.roleId">{{ '' }}</el-radio>
        </template>
      </el-table-column>
      <el-table-column label="角色编号" prop="roleId" width="120" />
      <el-table-column label="角色名称" prop="roleName" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="权限字符" prop="roleKey" :show-overflow-tooltip="true" width="150" />
      <el-table-column label="显示顺序" prop="roleSort" width="100" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page-sizes="[5, 10]" layout="prev, pager, next"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup>
import { ref, reactive, watch, getCurrentInstance } from 'vue'
import { listRole } from '@/api/system/role'
import { StrUtil } from '@/utils/StrUtil'

// 接受父组件的值
const props = defineProps({
  // 回显数据传值
  selectValues: {
    type: [Number, String, Array],
    default: null,
    required: false
  },
  checkType: {
    type: String,
    default: 'multiple',
    required: false
  }
})

// 定义 emit
const emit = defineEmits(['handleRoleSelect'])

// 数据定义
const loading = ref(true)
const total = ref(0)
const roleList = ref([])
const queryParams = reactive({
  pageNum: 1,
  pageSize: 5,
  roleName: undefined,
  roleKey: undefined,
  status: undefined
})
const radioSelected = ref(0) // 单选框传值
const selectRoleList = ref([]) // 回显数据传值
const dict = ref({})
const { proxy } = getCurrentInstance()

// Watchers
watch(() => props.selectValues, (newVal) => {
  if (StrUtil.isNotBlank(newVal)) {
    if (newVal instanceof Number || newVal instanceof String) {
      radioSelected.value = newVal
    } else {
      selectRoleList.value = newVal
    }
  }
}, { immediate: true })

watch(roleList, (newVal) => {
  if (StrUtil.isNotBlank(newVal) && selectRoleList.value.length > 0) {
    proxy.$nextTick(() => {
      proxy.$refs.dataTable.clearSelection()
      selectRoleList.value?.split(',').forEach(key => {
        const role = newVal.find(item => key == item.roleId)
        if (role) {
          proxy.$refs.dataTable.toggleRowSelection(role, true)
        }
      })
    })
  }
})

// 生命周期 created
dict.value = proxy.useDict('sys_normal_disable')
getList()

// Methods
function getList() {
  loading.value = true
  listRole(queryParams).then(response => {
    roleList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleMultipleRoleSelect(selection) {
  const idList = selection.map(item => item.roleId)
  const nameList = selection.map(item => item.roleName)
  emit('handleRoleSelect', idList.join(','), nameList.join(','))
}

function handleSingleRoleSelect(selection) {
  radioSelected.value = selection.roleId
  const roleName = selection.roleName
  emit('handleRoleSelect', radioSelected.value.toString(), roleName)
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm("queryForm");
  handleQuery()
}
</script>

<style>
/*隐藏radio展示的label及本身自带的样式*/
/*.el-radio__label{*/
/*  display:none;*/
/*}*/
</style>