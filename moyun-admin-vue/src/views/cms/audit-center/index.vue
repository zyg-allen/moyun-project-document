<template>
  <div class="app-container audit-center">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" class="search-form">
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" placeholder="全部类型" clearable style="width: 160px">
          <el-option v-for="d in cms_audit_task_type" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="提交人" prop="submitterName">
        <el-input v-model="queryParams.submitterName" placeholder="请输入提交人" clearable style="width: 160px" @keyup.enter="handleQuery" />
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

    <!-- Tab 切换：待办 / 我的已办 / 全部 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="audit-tabs">
      <el-tab-pane name="pending">
        <template #label>
          待办
          <el-badge v-if="pendingCount > 0" :value="pendingCount" type="warning" class="tab-badge" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="我的已办" name="done" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="taskList" @row-click="handleRowClick" class="audit-table">
      <el-table-column label="任务类型" align="center" width="120">
        <template #default="scope">
          <el-tag size="small" :type="taskTypeTagType(scope.row.taskType)">{{ scope.row.taskTypeLabel || scope.row.taskType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="left" prop="title" :show-overflow-tooltip="true" min-width="220" />
      <el-table-column label="提交人" align="center" prop="submitterName" width="120" />
      <el-table-column label="优先级" align="center" width="90">
        <template #default="scope">
          <el-tag size="small" :type="priorityTagType(scope.row.priority)" effect="plain">{{ scope.row.priorityLabel || scope.row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag size="small" :type="statusTagType(scope.row.status)">{{ scope.row.statusLabel || scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理人" align="center" prop="auditorName" width="100" />
      <el-table-column label="提交时间" align="center" prop="submitTime" width="160" />
      <el-table-column label="处理时间" align="center" prop="auditTime" width="160" />
      <el-table-column label="操作" align="right" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click.stop="handleView(scope.row)">详情</el-button>
          <el-button v-if="scope.row.status === 'pending'" link type="success" icon="Check" @click.stop="handleView(scope.row)">处理</el-button>
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

    <!-- 详情弹窗 -->
    <AuditTaskDetailDialog
      v-model="detailOpen"
      :task-id="detailTaskId"
      :can-handle="true"
      @success="onHandleSuccess"
    />
  </div>
</template>

<script setup name="CmsAuditCenter">
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listPending, listMyHandled, listAll, countByType } from '@/api/system/auditTask'
import AuditTaskDetailDialog from '@/components/AuditTaskDetailDialog/index.vue'

const { proxy } = getCurrentInstance()
const { cms_audit_task_type } = proxy.useDict('cms_audit_task_type')

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const taskList = ref([])
const total = ref(0)
const activeTab = ref('pending')
const pendingCount = ref(0)

const detailOpen = ref(false)
const detailTaskId = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  taskType: undefined,
  title: undefined,
  submitterName: undefined,
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
function priorityTagType(p) {
  return { high: 'danger', medium: 'warning', low: 'info' }[p] || 'info'
}

async function getList() {
  loading.value = true
  try {
    const params = { ...queryParams }
    let res
    if (activeTab.value === 'pending') {
      params.status = 'pending'
      res = await listPending(params)
    } else if (activeTab.value === 'done') {
      res = await listMyHandled(params)
    } else {
      res = await listAll(params)
    }
    taskList.value = res.rows || []
    total.value = res.total || 0
  } catch (e) {
    console.error('加载审核列表失败', e)
  } finally {
    loading.value = false
  }
}

/** 加载待办角标数 */
async function loadPendingCount() {
  try {
    const res = await countByType()
    const data = res.data
    if (data && typeof data === 'object') {
      // data 为 { article: 2, feedback: 1, ... } 或 { total: 3, types: {...} }
      if (typeof data.total === 'number') {
        pendingCount.value = data.total
      } else {
        pendingCount.value = Object.values(data).reduce((sum, n) => sum + (Number(n) || 0), 0)
      }
    } else if (typeof data === 'number') {
      pendingCount.value = data
    }
  } catch (e) {
    // 角标失败不影响主流程
  }
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.taskType = undefined
  queryParams.title = undefined
  queryParams.submitterName = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleTabChange() {
  queryParams.pageNum = 1
  getList()
}

function handleView(row) {
  detailTaskId.value = row.id
  detailOpen.value = true
}

function handleRowClick(row) {
  handleView(row)
}

function onHandleSuccess() {
  getList()
  loadPendingCount()
}

/** 从首页/业务页跳转：
 *  - ?taskId=xxx&tab=article  → 打开该任务详情（tab 为 taskType，用于过滤）
 *  - ?tab=article&bizId=123   → 按任务类型过滤列表并打开该业务对应的任务详情（从业务管理页跳入）
 *  - ?activeTab=pending|done|all → 切换顶部 Tab（首页"更多"入口使用）
 *    注意：activeTab 与 tab 语义不同——activeTab 切换待办/已办/全部视图，
 *    tab 是 taskType 列表过滤；两者可共存（如 ?activeTab=done&tab=article）。
 */
function handleRouteQuery() {
  const { taskId, tab, bizId, activeTab: tabParam } = route.query
  // activeTab：切换 pending/done/all 视图（合法值才生效，防注入）
  // 注意：解构重命名为 tabParam，避免与 ref 变量 activeTab 同名冲突
  if (tabParam === 'pending' || tabParam === 'done' || tabParam === 'all') {
    activeTab.value = tabParam
  }
  // tab 实际是 taskType（如 article/feedback），用于过滤列表
  if (tab && typeof tab === 'string') {
    queryParams.taskType = tab
  }
  // 先加载列表，再打开详情
  getList().then(() => {
    if (taskId) {
      detailTaskId.value = Number(taskId)
      detailOpen.value = true
    } else if (bizId) {
      // 从业务管理页（文章/专栏/话题列表）跳入：按 bizId 匹配任务并打开详情
      const matched = taskList.value.find(
        (t) => String(t.bizId) === String(bizId)
      )
      if (matched) {
        detailTaskId.value = matched.id
        detailOpen.value = true
      }
    }
  })
  // 清除 URL query，避免刷新重复触发
  if (taskId || bizId) {
    router.replace({ query: {} })
  }
}

onMounted(() => {
  if (route.query.taskId || route.query.tab || route.query.activeTab) {
    handleRouteQuery()
  } else {
    getList()
  }
  loadPendingCount()
})
</script>

<style scoped>
.audit-center { padding: 16px; }
.search-form { margin-bottom: 12px; }
.audit-tabs { margin-bottom: 12px; }
.tab-badge { margin-left: 4px; }
.audit-table { cursor: pointer; }
</style>
