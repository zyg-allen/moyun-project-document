<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="开始时间" prop="deployTime">
        <el-date-picker clearable v-model="queryParams.deployTime" type="date" value-format="yyyy-MM-dd"
          placeholder="选择时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
        <el-button icon="refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="delete" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['system:deployment:remove']">删除
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="todoList" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务编号" align="center" prop="taskId" :show-overflow-tooltip="true" />
      <el-table-column label="流程名称" align="center" prop="procDefName" />
      <el-table-column label="当前节点" align="center" prop="taskName" />
      <el-table-column label="流程版本" align="center">
        <template #="scope">
          <el-tag>v{{ scope.row.procDefVersion }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="流程发起人" align="center">
        <template #="scope">
          <label>{{ scope.row.startUserName }} <el-tag type="info">{{ scope.row.startDeptName }}</el-tag></label>
        </template>
      </el-table-column>
      <el-table-column label="接收时间" align="center" prop="createTime" width="180" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #="scope">
          <el-button type="primary" link icon="edit-outline" @click="handleProcess(scope.row)">处理
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList" />
  </div>
</template>

<script setup name="TodoTask">
import { ref, reactive } from 'vue'
import {
  todoList as fetchTodoList,
  delDeployment
} from '@/api/flowable/todo'

const router = useRouter()
const { proxy } = getCurrentInstance()

// 响应式数据
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)

const todoList = ref([])

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: null,
  category: null
})

/** 获取流程待办任务列表 */
function getList() {
  loading.value = true
  fetchTodoList(queryParams).then(response => {
    todoList.value = response.data.records
    total.value = response.data.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.name = null
  queryParams.category = null
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.taskId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.taskId || ids.value
  proxy.$confirm('是否确认删除流程定义编号为"' + _ids + '"的数据项?', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    return delDeployment(_ids)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  })
}

/** 跳转到处理页面 */
function handleProcess(row) {
  router.push({
    path: '/flowable/task/todo/detail/index',
    query: {
      procInsId: row.procInsId,
      executionId: row.executionId,
      deployId: row.deployId,
      taskId: row.taskId,
      taskName: row.taskName,
      startUser: row.startUserName + '-' + row.startDeptName
    }
  })
}

getList()
</script>