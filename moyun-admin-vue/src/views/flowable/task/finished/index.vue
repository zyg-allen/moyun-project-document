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
          v-hasPermi="['system:deployment:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="finishedList" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务编号" align="center" prop="taskId" :show-overflow-tooltip="true" />
      <el-table-column label="流程名称" align="center" prop="procDefName" :show-overflow-tooltip="true" />
      <el-table-column label="任务节点" align="center" prop="taskName" />
      <el-table-column label="流程发起人" align="center">
        <template #="scope">
          <label>{{ scope.row.startUserName }} <el-tag type="info">{{ scope.row.startDeptName }}</el-tag></label>
        </template>
      </el-table-column>
      <el-table-column label="接收时间" align="center" prop="createTime" width="180" />
      <el-table-column label="审批时间" align="center" prop="finishTime" width="180" />
      <el-table-column label="耗时" align="center" prop="duration" width="180" />
      <el-table-column label="操作" width="150" fixed="right" class-name="small-padding fixed-width">
        <template #="scope">
          <el-button type="primary" link icon="tickets" @click="handleFlowRecord(scope.row)">流转记录</el-button>
          <el-button type="primary" link icon="refresh-left" @click="handleRevoke(scope.row)">撤回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList" />
  </div>
</template>

<script setup name="FinishedTask">
import { ref, reactive } from 'vue'
import {
  finishedList as getFinishedList,
  delDeployment,
  revokeProcess
} from '@/api/flowable/finished'

const router = useRouter()
const { proxy } = getCurrentInstance()

// 响应式状态
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)

const finishedList = ref([])

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: null,
  category: null,
  key: null,
  tenantId: null,
  deployTime: null,
  derivedFrom: null,
  derivedFromRoot: null,
  parentDeploymentId: null,
  engineVersion: null
})

/** 获取流程已办任务列表 */
function getList() {
  loading.value = true
  getFinishedList(queryParams).then(response => {
    finishedList.value = response.data.records
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
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 删除按钮操作 */
function handleDelete(row) {
  const _ids = row.id || ids.value
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

/** 流程流转记录 */
function handleFlowRecord(row) {
  router.push({
    path: '/flowable/task/finished/detail/index',
    query: {
      procInsId: row.procInsId,
      deployId: row.deployId,
      taskId: row.taskId
    }
  })
}

/** 撤回任务 */
function handleRevoke(row) {
  const params = {
    instanceId: row.procInsId
  }
  revokeProcess(params).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    getList()
  })
}

// 初始化加载
getList()
</script>
