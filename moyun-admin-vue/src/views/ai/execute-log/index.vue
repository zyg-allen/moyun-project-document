<template>
  <div class="app-container">
    <!-- 筛选区 -->
    <el-card shadow="never" class="filter-card">
      <el-form :model="queryParams" :inline="true" class="search-form">
        <el-form-item label="场景">
          <el-select v-model="queryParams.sceneCode" placeholder="全部场景" clearable filterable style="width: 180px" @change="handleQuery">
            <el-option v-for="s in sceneOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="请求ID">
          <el-input v-model="queryParams.requestId" placeholder="requestId 精确查询" clearable style="width: 220px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="queryParams.modelUsed" placeholder="模型名关键词" clearable style="width: 160px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px" @change="handleQuery">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
            <el-option label="超时" value="timeout" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            @change="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总卡片（随筛选联动） -->
    <el-row :gutter="12" class="summary-row">
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ formatNumber(summary.totalCount) }}</div>
          <div class="stat-label">调用次数</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value" :class="successRateClass">{{ summary.successRate }}%</div>
          <div class="stat-label">成功率（失败 {{ formatNumber(summary.failCount) }}）</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ formatNumber(summary.totalTokens) }}</div>
          <div class="stat-label">Token 消耗</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value cost">¥{{ formatCost(summary.totalCost) }}</div>
          <div class="stat-label">成本</div>
        </el-card>
      </el-col>
      <el-col :span="5">
        <el-card shadow="never" class="stat-card">
          <div class="stat-value">{{ formatElapsed(summary.avgElapsed) }}</div>
          <div class="stat-label">平均耗时</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>执行日志</span>
          <span class="header-tip">统一网关全量调用记录：场景/模型/Token/成本/耗时，LLM 调用收口后的可观测性入口</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="logList" stripe>
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="请求ID" width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" class="request-id" @click="copyText(row.requestId)">
              {{ row.requestId || '-' }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="场景" prop="sceneCode" width="150">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.sceneCode || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模型 / Agent" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.modelUsed || '-' }}</span>
            <el-tag v-if="row.agentUsed" size="small" type="warning" class="agent-tag">{{ row.agentUsed }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Token" prop="tokenUsed" width="90" align="right">
          <template #default="{ row }">{{ row.tokenUsed != null ? formatNumber(row.tokenUsed) : '-' }}</template>
        </el-table-column>
        <el-table-column label="成本(¥)" width="100" align="right">
          <template #default="{ row }">{{ row.costYuan != null ? formatCost(row.costYuan) : '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">{{ row.elapsedMs != null ? row.elapsedMs + 'ms' : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" prop="createTime" width="165" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-hasPermi="['cms:ai:execute-log:remove']" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无执行日志（网关调用后自动产生）" :image-size="80" />
        </template>
      </el-table>

      <pagination
        v-show="total > 0"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="执行日志详情" size="560px">
      <el-descriptions v-if="detail" :column="1" border size="small">
        <el-descriptions-item label="请求ID">{{ detail.requestId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="场景 / Handler">
          {{ detail.sceneCode || '-' }} / {{ detail.handlerName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="绑定类型">{{ detail.bindType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ detail.modelUsed || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Agent">{{ detail.agentUsed || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Token / 成本">
          {{ detail.tokenUsed != null ? detail.tokenUsed : '-' }} / ¥{{ detail.costYuan != null ? formatCost(detail.costYuan) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.elapsedMs != null ? detail.elapsedMs + 'ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="输入摘要">
          <pre class="summary-pre">{{ detail.inputSummary || '（无）' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="输出摘要">
          <pre class="summary-pre">{{ detail.outputSummary || '（无）' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.toolCalls" label="工具调用">
          <pre class="summary-pre">{{ prettyJson(detail.toolCalls) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息">
          <pre class="summary-pre error-pre">{{ detail.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listExecuteLog, getExecuteLogSummary, getSceneOptions, getExecuteLog, delExecuteLog } from '@/api/ai/execute-log';

// 筛选参数（与后端 list/summary 共用）
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  sceneCode: '',
  requestId: '',
  modelUsed: '',
  status: ''
});
const dateRange = ref([]);

const loading = ref(false);
const logList = ref([]);
const total = ref(0);
const summary = ref({});
const sceneOptions = ref([]);
const detailVisible = ref(false);
const detail = ref(null);

const successRateClass = computed(() => {
  const rate = Number(summary.value.successRate) || 0;
  if (rate >= 99) return 'ok';
  if (rate >= 90) return 'warn';
  return 'bad';
});

onMounted(() => {
  getList();
  loadSummary();
  loadSceneOptions();
});

function getList() {
  loading.value = true;
  listExecuteLog(buildQuery()).then(response => {
    const page = response.data || {};
    logList.value = page.records || [];
    total.value = page.total || 0;
  }).finally(() => {
    loading.value = false;
  });
}

function loadSummary() {
  getExecuteLogSummary(buildQuery(true)).then(response => {
    summary.value = response.data || {};
  });
}

function loadSceneOptions() {
  getSceneOptions().then(response => {
    sceneOptions.value = response.data || [];
  });
}

// 组装查询参数（summary 不带分页）
function buildQuery(forSummary = false) {
  const query = {
    sceneCode: queryParams.sceneCode || undefined,
    requestId: queryParams.requestId || undefined,
    modelUsed: queryParams.modelUsed || undefined,
    status: queryParams.status || undefined
  };
  if (dateRange.value && dateRange.value.length === 2) {
    query.beginDate = dateRange.value[0];
    query.endDate = dateRange.value[1];
  }
  if (!forSummary) {
    query.pageNum = queryParams.pageNum;
    query.pageSize = queryParams.pageSize;
  }
  return query;
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
  loadSummary();
}

function resetQuery() {
  queryParams.sceneCode = '';
  queryParams.requestId = '';
  queryParams.modelUsed = '';
  queryParams.status = '';
  dateRange.value = [];
  handleQuery();
}

function handleDetail(row) {
  getExecuteLog(row.id).then(response => {
    detail.value = response.data;
    detailVisible.value = true;
  });
}

function handleDelete(row) {
  ElMessageBox.confirm(`确认删除日志 #${row.id}（${row.sceneCode || '-'}）？删除后不可恢复`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delExecuteLog(row.id);
  }).then(() => {
    ElMessage.success('删除成功');
    getList();
    loadSummary();
  }).catch(() => {});
}

// ==================== 展示辅助 ====================

function statusLabel(status) {
  return { success: '成功', fail: '失败', timeout: '超时' }[status] || (status || '-');
}

function statusTagType(status) {
  return { success: 'success', fail: 'danger', timeout: 'warning' }[status] || 'info';
}

function formatNumber(num) {
  if (num === null || num === undefined || num === '') return '0';
  return Number(num).toLocaleString();
}

function formatCost(cost) {
  if (cost === null || cost === undefined || cost === '') return '0.0000';
  return Number(cost).toFixed(4);
}

function formatElapsed(ms) {
  const v = Number(ms) || 0;
  if (v < 1000) return Math.round(v) + 'ms';
  return (v / 1000).toFixed(1) + 's';
}

function prettyJson(text) {
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch (e) {
    return text;
  }
}

function copyText(text) {
  if (!text) return;
  navigator.clipboard?.writeText(text).then(() => {
    ElMessage.success('已复制: ' + text);
  }).catch(() => {
    ElMessage.warning('复制失败，请手动选择');
  });
}
</script>

<style scoped>
.filter-card :deep(.el-card__body) { padding-bottom: 2px; }
.search-form { margin-bottom: -6px; }
.summary-row { margin-bottom: 12px; }
.stat-card { text-align: center; }
.stat-card :deep(.el-card__body) { padding: 14px 8px; }
.stat-value { font-size: 22px; font-weight: 600; color: #303133; }
.stat-value.ok { color: #67c23a; }
.stat-value.warn { color: #e6a23c; }
.stat-value.bad { color: #f56c6c; }
.stat-value.cost { color: #409eff; }
.stat-label { font-size: 12px; color: #909399; margin-top: 4px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-tip { font-size: 12px; color: #909399; font-weight: normal; }
.request-id { font-size: 12px; font-family: monospace; }
.agent-tag { margin-left: 6px; }
.summary-pre {
  margin: 0; max-height: 200px; overflow: auto;
  white-space: pre-wrap; word-break: break-all;
  font-size: 12px; line-height: 1.5; font-family: monospace;
}
.error-pre { color: #f56c6c; }
</style>
