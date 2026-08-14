<template>
  <div class="app-container audit-done">
    <div class="page-header">
      <span class="page-title">我的已办</span>
      <el-tag v-if="total > 0" type="success" size="small">{{ total }} 项已处理</el-tag>
    </div>

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" class="search-form">
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="全部类型" clearable style="width: 160px">
          <el-option v-for="opt in taskTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="处理结果" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="已通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
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

    <el-table v-loading="loading" :data="taskList" @row-click="handleView" class="audit-table">
      <el-table-column label="任务类型" align="center" width="120">
        <template #default="scope">
          <el-tag size="small" :type="taskTypeTagType(scope.row.taskType)">{{ scope.row.taskTypeLabel || scope.row.taskType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="left" prop="title" :show-overflow-tooltip="true" min-width="240" />
      <el-table-column label="提交人" align="center" prop="submitterName" width="120" />
      <el-table-column label="处理结果" align="center" width="100">
        <template #default="scope">
          <el-tag size="small" :type="statusTagType(scope.row.status)">{{ scope.row.statusLabel || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理时间" align="center" prop="auditTime" width="160" />
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click.stop="handleView(scope.row)">详情</el-button>
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

    <!-- 已办详情：只读模式 -->
    <AuditTaskDetailDialog
      v-model="detailOpen"
      :task-id="detailTaskId"
      :can-handle="false"
    />
  </div>
</template>

<script setup name="SystemAuditDone">
import { ref, reactive, onMounted } from 'vue'
import { listMyHandled } from '@/api/system/auditTask'
import AuditTaskDetailDialog from '@/components/AuditTaskDetailDialog/index.vue'

const loading = ref(false)
const showSearch = ref(true)
const taskList = ref([])
const total = ref(0)

const detailOpen = ref(false)
const detailTaskId = ref(null)

const taskTypeOptions = [
  { value: 'article', label: '文章审核' },
  { value: 'column', label: '专栏审核' },
  { value: 'topic', label: '话题审核' },
  { value: 'interview_exp', label: '面经审核' },
  { value: 'interview_comment', label: '面经评论审核' },
  { value: 'certification', label: '创作者认证' },
  { value: 'feedback', label: '意见反馈' },
  { value: 'report', label: '举报' }
]

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  taskType: undefined,
  title: undefined,
  status: undefined
})

function taskTypeTagType(type) {
  const map = {
    article: 'primary', column: 'success', topic: 'warning',
    interview_exp: 'danger', interview_comment: 'info',
    certification: '', feedback: 'warning', report: 'danger'
  }
  return map[type] || 'info'
}
function statusTagType(status) {
  return { pending: 'warning', approved: 'success', rejected: 'danger' }[status] || 'info'
}

async function getList() {
  loading.value = true
  try {
    const res = await listMyHandled(queryParams)
    taskList.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载已办列表失败', e)
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.taskType = undefined
  queryParams.title = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleView(row) {
  detailTaskId.value = row.id
  detailOpen.value = true
}

onMounted(() => {
  getList()
})
</script>

<style scoped>
.audit-done { padding: 16px; }
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.page-title { font-size: 18px; font-weight: 600; color: #303133; }
.search-form { margin-bottom: 12px; }
.audit-table { cursor: pointer; }
</style>
