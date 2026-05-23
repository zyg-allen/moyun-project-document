<template>
  <div class="app-container">
    <el-form
      :model="queryParams"
      ref="queryRef"
      :inline="true"
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="开始时间" prop="deployTime">
        <el-date-picker
          clearable
          v-model="queryParams.deployTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="选择时间"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['system:deployment:add']"
        >新增流程</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['system:deployment:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table
      v-loading="loading"
      :data="myProcessList"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="流程编号" align="center" prop="procInsId" show-overflow-tooltip />
      <el-table-column label="流程名称" align="center" prop="procDefName" show-overflow-tooltip />
      <el-table-column label="流程类别" align="center" prop="category" width="100px" />
      <el-table-column label="流程版本" align="center" width="80px">
        <template #default="scope">
          <el-tag>v{{ scope.row.procDefVersion }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" align="center" prop="createTime" width="180" />
      <el-table-column label="流程状态" align="center" width="100">
        <template #default="scope">
          <el-tag v-if="!scope.row.finishTime">进行中</el-tag>
          <el-tag v-else type="success">已完成</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="耗时" align="center" prop="duration" width="180" />
      <el-table-column label="当前节点" align="center" prop="taskName" />
      <el-table-column label="办理人" align="center">
        <template #default="scope">
          <label v-if="scope.row.assigneeName">
            {{ scope.row.assigneeName }}
            <el-tag type="info">{{ scope.row.assigneeDeptName }}</el-tag>
          </label>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button @click="handleFlowRecord(scope.row)" type="primary" link>详情</el-button>
          <el-button @click="handleStop(scope.row)" type="primary" link>取消申请</el-button>
          <el-button
            @click="handleDelete(scope.row)"
            type="primary"
            link
            v-hasPermi="['system:deployment:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 发起流程 -->
    <el-dialog :title="title" v-model="open" width="60%" append-to-body>
      <el-form
        :model="queryProcessParams"
        ref="queryProcessForm"
        :inline="true"
        v-show="showSearch"
        label-width="68px"
      >
        <el-form-item label="名称" prop="name">
          <el-input
            v-model="queryProcessParams.name"
            placeholder="请输入名称"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleProcessQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetProcessQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table
        v-loading="processLoading"
        fit
        :data="definitionList"
        border
      >
        <el-table-column label="流程名称" align="center" prop="name" />
        <el-table-column label="流程版本" align="center">
          <template #default="scope">
            <el-tag>v{{ scope.row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="流程分类" align="center" prop="category" />
        <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
          <template #default="scope">
            <el-button
              type="primary"
              link
              icon="Edit"
              @click="handleStartProcess(scope.row)"
            >发起流程</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="processTotal > 0"
        :total="processTotal"
        v-model:page="queryProcessParams.pageNum"
        v-model:limit="queryProcessParams.pageSize"
        @pagination="getListDefinition"
      />
    </el-dialog>
  </div>
</template>

<script setup name="MyProcess">
import { ref, reactive } from 'vue'
import {
  getDeployment,
  delDeployment,
  addDeployment,
  updateDeployment,
  exportDeployment,
  flowRecord
} from "@/api/flowable/finished";
import { myProcessList as fetchMyProcessList, stopProcess } from "@/api/flowable/process";
import { listDefinition } from "@/api/flowable/definition";

const loading = ref(true)
const processLoading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const processTotal = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const title = ref('')
const open = ref(false)
const myProcessList = ref([])
const definitionList = ref([])

const router = useRouter()

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

const queryProcessParams = reactive({
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

function getList() {
  loading.value = true
  fetchMyProcessList(queryParams).then(response => {
    myProcessList.value = response.data.records
    total.value = response.data.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  // 清空查询表单逻辑
  queryParams.name = null
  queryParams.deployTime = null
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.procInsId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

function handleAdd() {
  open.value = true
  title.value = "发起流程"
  getListDefinition()
}

function getListDefinition() {
  processLoading.value = true
  listDefinition(queryProcessParams).then(response => {
    definitionList.value = response.data.records
    processTotal.value = response.data.total
    processLoading.value = false
  })
}

function handleStartProcess(row) {
  router.push({
    path: '/flowable/task/myProcess/send/index',
    query: {
      deployId: row.deploymentId,
      procDefId: row.id
    }
  })
}

function handleStop(row) {
  const params = {
    instanceId: row.procInsId
  }
  stopProcess(params).then(res => {
    console.log(res.msg)
    getList()
  })
}

function handleFlowRecord(row) {
  router.push({
    path: '/flowable/task/myProcess/detail/index',
    query: {
      procInsId: row.procInsId,
      deployId: row.deployId,
      taskId: row.taskId
    }
  })
}

function handleDelete(row) {
  const ids = row.procInsId || this.ids
  ElMessageBox.confirm('是否确认删除流程定义编号为"' + ids + '"的数据项?', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    return delDeployment(ids);
  }).then(() => {
    getList();
    ElMessage.success("删除成功");
  })
}

function handleProcessQuery() {
  queryProcessParams.pageNum = 1
  getListDefinition()
}

function resetProcessQuery() {
  queryProcessParams.name = null
  handleProcessQuery()
}

// 初始加载
getList()
</script>