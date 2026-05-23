<template>
  <div>
    <el-row :gutter="20">
      <!--部门数据-->
      <el-col :span="6" :xs="24">
        <div class="head-container">
          <el-input v-model="deptName" placeholder="请输入部门名称" clearable prefix-icon="search"
            style="margin-bottom: 20px" />
        </div>
        <div class="head-container">
          <el-tree :data="deptOptions" :props="defaultProps" :expand-on-click-node="false"
            :filter-node-method="filterNode" ref="tree" node-key="id" default-expand-all highlight-current
            @node-click="handleNodeClick" />
        </div>
      </el-col>
      <!--用户数据-->
      <el-col :span="18" :xs="24">
        <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px">
          <el-form-item label="用户名称" prop="userName">
            <el-input v-model="queryParams.userName" placeholder="请输入用户名称" clearable style="width: 150px"
              @keyup.enter.native="handleQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
            <el-button icon="refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-show="checkType === 'multiple'" ref="dataTable" v-loading="loading" :row-key="getRowKey"
          :data="userList" @selection-change="handleMultipleUserSelect">
          <el-table-column type="selection" :reserve-selection="true" width="50" align="center" />
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns[0].visible" />
          <el-table-column label="登录账号" align="center" key="userName" prop="userName" v-if="columns[1].visible"
            :show-overflow-tooltip="true" />
          <el-table-column label="用户姓名" align="center" key="nickName" prop="nickName" v-if="columns[2].visible"
            :show-overflow-tooltip="true" />
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns[3].visible"
            :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns[4].visible"
            width="120" />
        </el-table>
        <el-table v-show="checkType === 'single'" v-loading="loading" :data="userList"
          @current-change="handleSingleUserSelect">
          <el-table-column width="55" align="center">
            <template #="scope">
              <el-radio v-model="radioSelected" :label="scope.row.userId">{{ '' }}</el-radio>
            </template>
          </el-table-column>
          <el-table-column label="用户编号" align="center" key="userId" prop="userId" v-if="columns[0].visible" />
          <el-table-column label="登录账号" align="center" key="userName" prop="userName" v-if="columns[1].visible"
            :show-overflow-tooltip="true" />
          <el-table-column label="用户姓名" align="center" key="nickName" prop="nickName" v-if="columns[2].visible"
            :show-overflow-tooltip="true" />
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns[3].visible"
            :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns[4].visible"
            width="120" />
        </el-table>
        <pagination v-show="total > 0" :total="total" :page-sizes="[5, 10]" layout="prev, pager, next"
          v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, watch, getCurrentInstance } from 'vue'
import { listUser, deptTreeSelect } from '@/api/system/user'
import { StrUtil } from '@/utils/StrUtil'

// 接受父组件的值
const props = defineProps({
  // 回显数据传值
  selectValues: {
    type: [Number, String, Array],
    default: null,
    required: false
  },
  // 表格类型
  checkType: {
    type: String,
    default: 'multiple',
    required: true
  }
})

// 定义 emit
const emit = defineEmits(['handleUserSelect'])

// 数据定义
const loading = ref(true)
const total = ref(0)
const userList = ref([])
const deptOptions = ref(undefined)
const deptName = ref(undefined)
const queryParams = reactive({
  pageNum: 1,
  pageSize: 5,
  userName: undefined,
  phonenumber: undefined,
  status: undefined,
  deptId: undefined
})
const columns = reactive([
  { key: 0, label: `用户编号`, visible: true },
  { key: 1, label: `用户名称`, visible: true },
  { key: 2, label: `用户昵称`, visible: true },
  { key: 3, label: `部门`, visible: true },
  { key: 4, label: `手机号码`, visible: true },
  { key: 5, label: `状态`, visible: true },
  { key: 6, label: `创建时间`, visible: true }
])
const radioSelected = ref(0) // 单选框传值
const selectUserList = ref([]) // 回显数据传值
const dict = ref({})
const { proxy } = getCurrentInstance()

// 默认树结构配置
const defaultProps = {
  children: 'children',
  label: 'label'
}

// Watchers
watch(deptName, (val) => {
  if (proxy.$refs.tree && proxy.$refs.tree.filter) {
    proxy.$refs.tree.filter(val)
  }
})

watch(() => props.selectValues, (newVal) => {
  if (StrUtil.isNotBlank(newVal)) {
    if (typeof newVal === 'number') {
      radioSelected.value = newVal
    } else {
      selectUserList.value = newVal
    }
  }
}, { immediate: true })

watch(userList, (newVal) => {
  if (StrUtil.isNotBlank(newVal) && selectUserList.value.length > 0) {
    proxy.$nextTick(() => {
      proxy.$refs.dataTable.clearSelection()
      selectUserList.value?.split(',').forEach(key => {
        const user = newVal.find(item => key == item.userId)
        if (user) {
          proxy.$refs.dataTable.toggleRowSelection(user, true)
        }
      })
    })
  }
})

// 生命周期 created
dict.value = proxy.useDict('sys_normal_disable', 'sys_user_sex')
getList()
getDeptTree()

// Methods
function getList() {
  loading.value = true
  listUser(queryParams).then(response => {
    userList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
  })
}

function getRowKey(row) {
  return row.id
}

function filterNode(value, data) {
  if (!value) return true
  return data.label.indexOf(value) !== -1
}

function handleNodeClick(data) {
  queryParams.deptId = data.id
  handleQuery()
}

function handleMultipleUserSelect(selection) {
  emit('handleUserSelect', selection)
}

function handleSingleUserSelect(selection) {
  radioSelected.value = selection.userId
  emit('handleUserSelect', selection)
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryForm')
  queryParams.deptId = undefined
  proxy.$refs.tree.setCurrentKey(null)
  handleQuery()
}
</script>

<style>
/*隐藏radio展示的label及本身自带的样式*/
/*.el-radio__label{*/
/*  display:none;*/
/*}*/
</style>