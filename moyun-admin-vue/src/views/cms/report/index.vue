<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="举报类型" prop="reportType">
        <el-select v-model="queryParams.reportType" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="opt in reportTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="举报人" prop="username">
        <el-input v-model="queryParams.username" placeholder="请输入用户名" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['cms:report:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reportList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="举报类型" align="center" width="120">
        <template #default="scope">
          <el-tag :type="typeTagType(scope.row.reportType)">{{ getTypeLabel(scope.row.reportType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="问题描述" align="left" prop="description" :show-overflow-tooltip="true" min-width="220" />
      <el-table-column label="目标链接" align="left" prop="targetUrl" :show-overflow-tooltip="true" width="180" />
      <el-table-column label="举报人" align="center" prop="username" width="120" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理人" align="center" prop="handler" width="100" />
      <el-table-column label="提交时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['cms:report:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleHandle(scope.row)" v-hasPermi="['cms:report:handle']">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 详情对话框 -->
    <el-dialog title="举报详情" v-model="viewOpen" width="640px" append-to-body>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="举报编号">{{ viewForm.id }}</el-descriptions-item>
        <el-descriptions-item label="举报类型">{{ getTypeLabel(viewForm.reportType) }}</el-descriptions-item>
        <el-descriptions-item label="举报人">{{ viewForm.username }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ viewForm.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ viewForm.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="目标链接" :span="2">
          <el-link v-if="viewForm.targetUrl" type="primary" :href="viewForm.targetUrl" target="_blank">{{ viewForm.targetUrl }}</el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">{{ viewForm.description }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">{{ getStatusLabel(viewForm.status) }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ viewForm.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ viewForm.handleTime ? parseTime(viewForm.handleTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理结果" :span="2">{{ viewForm.handleResult || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 处理对话框 -->
    <el-dialog title="处理举报" v-model="handleOpen" width="560px" append-to-body>
      <el-form ref="handleRef" :model="handleForm" :rules="handleRules" label-width="100px">
        <el-form-item label="举报编号">
          <span>{{ handleForm.id }}</span>
        </el-form-item>
        <el-form-item label="问题描述">
          <span>{{ handleForm.description }}</span>
        </el-form-item>
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="handleForm.status" placeholder="请选择处理状态" style="width: 100%">
            <el-option label="处理中" value="processing" />
            <el-option label="已解决" value="resolved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理结果" prop="handleResult">
          <el-input v-model="handleForm.handleResult" type="textarea" :rows="4" placeholder="请输入处理结果说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleOpen = false">取 消</el-button>
        <el-button type="primary" @click="submitHandle">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Report">
import { listReport, getReport, handleReport, delReport } from '@/api/cms/report'

const { proxy } = getCurrentInstance()

const reportList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const viewOpen = ref(false)
const handleOpen = ref(false)
const viewForm = ref({})
const handleForm = ref({})
const handleRef = ref(null)

const reportTypeOptions = [
  { value: 'spam', label: '垃圾内容' },
  { value: 'inappropriate', label: '不当内容' },
  { value: 'infringement', label: '侵权内容' },
  { value: 'fraud', label: '欺诈行为' },
  { value: 'other', label: '其他问题' }
]
const statusOptions = [
  { value: 'pending', label: '待处理' },
  { value: 'processing', label: '处理中' },
  { value: 'resolved', label: '已解决' },
  { value: 'rejected', label: '已驳回' }
]

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  reportType: undefined,
  status: undefined,
  username: undefined
})

const handleRules = {
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
  handleResult: [{ required: true, message: '请输入处理结果说明', trigger: 'blur' }]
}

function getTypeLabel(val) {
  const item = reportTypeOptions.find(o => o.value === val)
  return item ? item.label : val
}
function getStatusLabel(val) {
  const item = statusOptions.find(o => o.value === val)
  return item ? item.label : val
}
function typeTagType(val) {
  return { spam: 'info', inappropriate: 'warning', infringement: 'danger', fraud: 'danger', other: '' }[val] || ''
}
function statusTagType(val) {
  return { pending: 'warning', processing: 'primary', resolved: 'success', rejected: 'info' }[val] || ''
}

function getList() {
  loading.value = true
  listReport(queryParams.value).then(res => {
    reportList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}
function handleView(row) {
  getReport(row.id).then(res => {
    viewForm.value = res.data
    viewOpen.value = true
  })
}
function handleHandle(row) {
  handleForm.value = { id: row.id, description: row.description, status: 'resolved', handleResult: '' }
  handleOpen.value = true
}
function submitHandle() {
  handleRef.value.validate(valid => {
    if (!valid) return
    // 仅提交必要字段，防止字段篡改
    const payload = { id: handleForm.value.id, status: handleForm.value.status, handleResult: handleForm.value.handleResult }
    handleReport(payload).then(() => {
      proxy.$modal.msgSuccess('处理成功')
      handleOpen.value = false
      getList()
    })
  })
}
function handleDelete(row) {
  const delIds = row.id ? [row.id] : ids.value
  proxy.$modal.confirm('确认删除选中的 ' + delIds.length + ' 条举报记录？').then(() => {
    return delReport(delIds.join(','))
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
