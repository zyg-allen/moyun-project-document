<template>
  <div class="app-container">
    <el-alert type="warning" :closable="false" class="page-tip"
      title="合规说明：本页默认只展示脱敏聚合指标。查看用户个体流水须该用户在 App「我的 → 隐私与授权」中主动授权，且只能在授权范围与有效期内查看；每次查看都会写入访问审计日志。" />

    <el-form :inline="true" :model="queryParams" class="query-form">
      <el-form-item label="用户ID">
        <el-input v-model="queryParams.userId" placeholder="精确查询" clearable style="width: 180px"
          @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="授权状态">
        <el-select v-model="queryParams.authStatus" placeholder="全部" clearable style="width: 130px" @change="handleQuery">
          <el-option label="已授权" value="granted" />
          <el-option label="未授权" value="none" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button icon="QuestionFilled" @click="helpVisible = true">授权流程说明</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="userList" border>
      <el-table-column label="用户ID" prop="userId" width="90" align="center" />
      <el-table-column label="昵称（脱敏）" prop="nickname" width="130" />
      <el-table-column label="手机（脱敏）" prop="phone" width="130" />
      <el-table-column label="数据授权" width="150" align="center">
        <template #default="scope">
          <el-tooltip v-if="scope.row.authStatus === 'granted'"
            :content="'授权范围：' + (scope.row.authScopes || '流水') + '；到期：' + (scope.row.authExpireAt || '长期')"
            placement="top">
            <el-tag type="success" size="small" effect="plain">已授权</el-tag>
          </el-tooltip>
          <el-tag v-else type="info" size="small" effect="plain">未授权</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效流水" prop="txnCount" width="100" align="center" sortable />
      <el-table-column label="最近记账" prop="lastTxnDate" width="120" />
      <el-table-column label="AI 报告" prop="aiReportCount" width="90" align="center" />
      <el-table-column label="Token" width="100" align="right">
        <template #default="scope">{{ formatNumber(scope.row.aiTokenUsed) }}</template>
      </el-table-column>
      <el-table-column label="成本（元）" width="110" align="right">
        <template #default="scope">{{ scope.row.aiCostYuan != null ? Number(scope.row.aiCostYuan).toFixed(4) : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" align="center" fixed="right">
        <template #default="scope">
          <el-tooltip :disabled="scope.row.authStatus === 'granted'"
            content="该用户未授权查看流水，请让其在 App 内开启授权" placement="top">
            <span>
              <el-button link type="primary" icon="View"
                :disabled="scope.row.authStatus !== 'granted'"
                v-hasPermi="['cms:ledgerUsers:query']"
                @click="handleViewTxn(scope.row)">查看流水</el-button>
            </span>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 流水抽屉（仅在授权有效时可打开） -->
    <el-drawer v-model="txnVisible" :title="drawerTitle" size="640px">
      <el-alert v-if="authTip" type="success" :closable="false" class="drawer-tip" :title="authTip" />
      <el-table v-loading="txnLoading" :data="txnList" height="calc(100vh - 220px)" border>
        <el-table-column label="日期" prop="transactionDate" width="110" />
        <el-table-column label="类型" width="90">
          <template #default="scope">
            <el-tag :type="typeTag(scope.row.type)" size="small">{{ scope.row.typeLabel || scope.row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" min-width="120" />
        <el-table-column label="金额" prop="amount" width="120" align="right">
          <template #default="scope">
            <span :style="{ color: scope.row.type === 'income' ? '#67c23a' : (scope.row.type === 'expense' ? '#f56c6c' : '#909399') }">
              {{ scope.row.type === 'income' ? '+' : (scope.row.type === 'expense' ? '-' : '') }}{{ scope.row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 1" type="success" size="small">有效</el-tag>
            <el-tag v-else type="info" size="small">已删除</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="txnTotal > 0" :total="txnTotal" v-model:page="txnQuery.pageNum"
        v-model:limit="txnQuery.pageSize" :layout="'total, prev, pager, next'" @pagination="loadTxn" />
    </el-drawer>

    <!-- 授权流程说明 -->
    <el-dialog v-model="helpVisible" title="用户数据授权流程" width="560px" append-to-body>
      <el-steps direction="vertical" :active="4" finish-status="success" style="padding-left: 8px">
        <el-step title="用户在 App 发起授权" description="我的 → 隐私与授权，选择范围（流水/账户/预算/库存/AI）与期限，并填写用途。" />
        <el-step title="后台获得授权凭证" description="本页「数据授权」列变为「已授权」，并显示范围与到期时间。" />
        <el-step title="查看时最小必要" description="仅返回授权范围内的必要字段（如流水只含类型/金额/分类/日期）。" />
        <el-step title="留痕与失效" description="每次查看写入访问审计；到期或用户撤回后立即恢复「未授权」，后台不可再查看。" />
      </el-steps>
      <el-alert type="info" :closable="false" title="后台不能代替用户授权，也不能延长授权期限。" />
    </el-dialog>
  </div>
</template>

<script setup name="LedgerUsers">
import { listLedgerUsers, listUserTransactions } from '@/api/cms/ledger'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const userList = ref([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, userId: undefined, authStatus: undefined })

const helpVisible = ref(false)
const txnVisible = ref(false)
const txnLoading = ref(false)
const txnList = ref([])
const txnTotal = ref(0)
const activeUser = ref({})
const txnQuery = reactive({ pageNum: 1, pageSize: 20 })

const drawerTitle = computed(() =>
  '用户 ' + (activeUser.value.userId || '') + '（' + (activeUser.value.nickname || '') + '）流水明细')
const authTip = computed(() => {
  const a = activeUser.value
  if (!a || a.authStatus !== 'granted') return ''
  return '本次查看基于用户授权：范围 ' + (a.authScopes || '流水') +
    '；到期 ' + (a.authExpireAt || '长期') + '；已写入访问审计'
})

function getList() {
  loading.value = true
  listLedgerUsers(queryParams).then(response => {
    userList.value = response.rows || []
    total.value = Number(response.total || 0)
  }).finally(() => { loading.value = false })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  queryParams.userId = undefined
  queryParams.authStatus = undefined
  handleQuery()
}

function handleViewTxn(row) {
  if (row.authStatus !== 'granted') {
    proxy.$modal.msgWarning('该用户未授权查看流水')
    return
  }
  activeUser.value = row
  txnVisible.value = true
  txnQuery.pageNum = 1
  loadTxn()
}

function loadTxn() {
  txnLoading.value = true
  listUserTransactions(activeUser.value.userId, txnQuery).then(response => {
    txnList.value = response.rows || []
    txnTotal.value = Number(response.total || 0)
  }).finally(() => { txnLoading.value = false })
}

function typeTag(type) {
  return {
    income: 'success', expense: 'danger', transfer: 'info',
    repayment: 'warning', borrow: 'warning', adjust: 'info',
    lend: 'warning', collect: 'success'
  }[type] || 'info'
}

function formatNumber(v) {
  return v == null ? '0' : Number(v).toLocaleString()
}

getList()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.query-form { margin-bottom: 4px; }
.drawer-tip { margin-bottom: 12px; }
</style>
