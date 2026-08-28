<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="岗位" prop="position">
        <el-input
          v-model="queryParams.position"
          placeholder="请输入岗位"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="interviewList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户名" align="center" prop="username" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="岗位" align="left" prop="position" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分数" align="center" width="80">
        <template #default="scope">
          <span v-if="scope.row.score != null" :style="getScoreStyle(scope.row.score)">{{ scope.row.score }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="题数" align="center" prop="totalQa" width="80" />
      <el-table-column label="难度" align="center" width="90">
        <template #default="scope">
          <span>{{ getDifficultyLabel(scope.row.difficulty) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="right" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['cms:voiceInterview:query']">复盘详情</el-button>
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

    <!-- 复盘详情对话框 -->
    <el-dialog title="语音面试复盘详情" v-model="detailOpen" width="900px" append-to-body>
      <el-descriptions :column="3" border v-loading="detailLoading">
        <el-descriptions-item label="面试ID">{{ detailForm.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailForm.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ detailForm.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailForm.status)">{{ getStatusLabel(detailForm.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总分">
          <span v-if="detailForm.score != null" :style="getScoreStyle(detailForm.score)">{{ detailForm.score }}</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="总题数">{{ detailForm.totalQa }}</el-descriptions-item>
        <el-descriptions-item label="难度">{{ getDifficultyLabel(detailForm.difficulty) }}</el-descriptions-item>
        <el-descriptions-item label="面试风格">{{ getStyleLabel(detailForm.style) }}</el-descriptions-item>
        <el-descriptions-item label="个性化">{{ detailForm.isPersonalized === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="场景" :span="3">{{ detailForm.scene || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(detailForm.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="面试总结" :span="2">{{ detailForm.summary || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">问答详情（共 {{ (detailForm.qaList || []).length }} 题）</el-divider>

      <el-table :data="detailForm.qaList || []" border style="width: 100%" empty-text="暂无问答记录">
        <el-table-column label="题号" align="center" prop="questionIdx" width="60" />
        <el-table-column label="问题" align="left" prop="question" min-width="200" :show-overflow-tooltip="true" />
        <el-table-column label="用户回答" align="left" min-width="220" :show-overflow-tooltip="true">
          <template #default="scope">
            <span>{{ scope.row.userAnswer || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" align="center" width="70">
          <template #default="scope">
            <span v-if="scope.row.score != null" :style="getScoreStyle(scope.row.score)">{{ scope.row.score }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="AI反馈" align="left" min-width="200" :show-overflow-tooltip="true">
          <template #default="scope">
            <span>{{ scope.row.aiFeedback || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提示" align="center" width="60">
          <template #default="scope">
            <el-tag v-if="scope.row.hintUsed > 0" type="warning" size="small">已用</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" align="center" width="80">
          <template #default="scope">
            <span v-if="scope.row.latencyMs != null">{{ formatLatency(scope.row.latencyMs) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" align="center" width="150">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="VoiceInterview">
import { getVoiceInterviewList, getVoiceInterviewDetail } from '@/api/voiceInterview'

const { proxy } = getCurrentInstance()

const interviewList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailForm = ref({})

const statusOptions = [
  { value: 'ongoing', label: '进行中' },
  { value: 'finished', label: '已完成' },
  { value: 'abandoned', label: '已放弃' }
]

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  username: undefined,
  position: undefined,
  status: undefined
})

/** 查询列表 */
function getList() {
  loading.value = true
  getVoiceInterviewList(queryParams.value)
    .then((res) => {
      interviewList.value = res.rows
      total.value = res.total
    })
    .finally(() => {
      loading.value = false
    })
}

/** 搜索 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置 */
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

/** 查看复盘详情 */
function handleView(row) {
  detailLoading.value = true
  detailOpen.value = true
  detailForm.value = {}
  getVoiceInterviewDetail(row.id)
    .then((res) => {
      detailForm.value = res.data
    })
    .finally(() => {
      detailLoading.value = false
    })
}

/** 状态标签类型 */
function getStatusType(status) {
  const map = { ongoing: 'warning', finished: 'success', abandoned: 'info' }
  return map[status] || 'info'
}

/** 状态标签文本 */
function getStatusLabel(status) {
  const item = statusOptions.find((d) => d.value === status)
  return item ? item.label : status || '-'
}

/** 难度文本 */
function getDifficultyLabel(difficulty) {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty || '-'
}

/** 面试风格文本 */
function getStyleLabel(style) {
  const map = { professional: '专业', friendly: '友好', strict: '严格' }
  return map[style] || style || '-'
}

/** 分数样式（低于60分红色，60-80橙色，80+绿色） */
function getScoreStyle(score) {
  if (score == null) return {}
  if (score >= 80) return { color: '#67c23a', fontWeight: 'bold' }
  if (score >= 60) return { color: '#e6a23c', fontWeight: 'bold' }
  return { color: '#f56c6c', fontWeight: 'bold' }
}

/** 格式化回答耗时 */
function formatLatency(ms) {
  if (ms == null) return '-'
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(1) + 's'
}

getList()
</script>
