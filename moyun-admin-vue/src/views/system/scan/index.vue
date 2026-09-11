<template>
  <div class="app-container scan-page">
    <div class="page-header">
      <span class="page-title">扫描结果</span>
      <el-tag type="info" size="small">定时任务扫描出的问题</el-tag>
    </div>

    <el-alert
      title="扫描结果说明"
      type="info"
      :closable="false"
      show-icon
      class="notice-alert"
    >
      此处展示定时任务扫描出的异常或待处理项（如数据一致性、孤立记录、配置异常等）。
      后台定时任务扫描发现问题后会自动记录到 sys_job_scan_issue 表，由人工介入处理并查看详细日志。
      <span v-if="!apiReady">扫描任务后端接口待接入，当前为占位展示。</span>
    </el-alert>

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" class="search-form">
      <el-form-item label="问题类型" prop="issueType">
        <el-select v-model="queryParams.issueType" placeholder="全部类型" clearable style="width: 160px">
          <el-option label="敏感词" value="sensitive_word" />
          <el-option label="待处理超期" value="pending_overdue" />
          <el-option label="异常" value="anomaly" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="待处理" value="pending" />
          <el-option label="已处理" value="handled" />
          <el-option label="已忽略" value="ignored" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" plain @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="issueList">
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="问题类型" align="center" width="120">
        <template #default="scope">
          <el-tag size="small" :type="issueTypeTagType(scope.row.issueType)">{{ issueTypeLabel(scope.row.issueType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="问题描述" align="left" prop="issueDesc" :show-overflow-tooltip="true" min-width="260" />
      <el-table-column label="关联任务" align="center" prop="jobName" width="140" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag size="small" :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发现时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="right" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)">查看日志</el-button>
          <el-button v-if="scope.row.status === 'pending'" link type="success" icon="Check" @click="handleResolve(scope.row)">处理</el-button>
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

    <el-empty v-if="!loading && issueList.length === 0" description="暂无扫描结果" />
  </div>
</template>

<script setup name="SystemScanIndex">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const showSearch = ref(true)
const issueList = ref([])
const total = ref(0)
const apiReady = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  issueType: undefined,
  status: undefined
})

function issueTypeLabel(type) {
  const map = {
    sensitive_word: '敏感词',
    pending_overdue: '待处理超期',
    anomaly: '异常',
    other: '其他'
  }
  return map[type] || type || '-'
}
function issueTypeTagType(type) {
  return { sensitive_word: 'danger', pending_overdue: 'warning', anomaly: 'danger', other: 'info' }[type] || 'info'
}
function statusLabel(status) {
  return { pending: '待处理', handled: '已处理', ignored: '已忽略' }[status] || status
}
function statusTagType(status) {
  return { pending: 'warning', handled: 'success', ignored: 'info' }[status] || 'info'
}

async function getList() {
  // 后端扫描结果接口待接入（/system/scan/list），接口就绪后此处自动生效
  loading.value = true
  try {
    const res = await request({
      url: '/system/scan/list',
      method: 'get',
      params: queryParams
    })
    apiReady.value = true
    issueList.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    // 接口未就绪时保持空列表，不报错
    apiReady.value = false
    issueList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.issueType = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleView(row) {
  ElMessage.info('日志详情查看功能待后端接口接入')
}
function handleResolve(row) {
  ElMessage.info('问题处理功能待后端接口接入')
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.scan-page { padding: 16px; }
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title { font-size: 18px; font-weight: 600; color: #303133; }
.notice-alert { margin-bottom: 16px; }
.search-form { margin-bottom: 12px; }
</style>
