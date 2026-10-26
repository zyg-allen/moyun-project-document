<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" class="page-tip"
      title="授权与审计：授权由用户在 App 内主动发起，后台只读（不可代授权、不可延期）；访问日志记录每一次个体数据查看，用于事后追溯。" />

    <el-tabs v-model="tab">
      <!-- 授权记录 -->
      <el-tab-pane label="授权记录" name="auth">
        <el-form :inline="true" :model="authQuery" class="query-form">
          <el-form-item label="用户ID">
            <el-input v-model="authQuery.userId" placeholder="精确查询" clearable style="width: 160px"
              @keyup.enter="loadAuth" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="authQuery.status" placeholder="全部" clearable style="width: 130px" @change="loadAuth">
              <el-option label="有效" value="valid" />
              <el-option label="已过期" value="expired" />
              <el-option label="已撤回" value="revoked" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadAuth">查询</el-button>
            <el-button icon="Refresh" @click="resetAuth">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="authLoading" :data="authList" border>
          <el-table-column label="用户ID" prop="userId" width="90" align="center" />
          <el-table-column label="昵称（脱敏）" prop="nickname" width="130" />
          <el-table-column label="授权范围" min-width="180">
            <template #default="scope">
              <el-tag v-for="s in splitScopes(scope.row.scopes)" :key="s" size="small" effect="plain" class="scope-tag">
                {{ SCOPE_LABELS[s] || s }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="用途" prop="purpose" min-width="160" show-overflow-tooltip />
          <el-table-column label="发起渠道" width="100" align="center">
            <template #default="scope">{{ scope.row.channel === 'app' ? 'App' : (scope.row.channel || '—') }}</template>
          </el-table-column>
          <el-table-column label="授权时间" prop="grantedAt" width="160" />
          <el-table-column label="到期时间" width="160">
            <template #default="scope">{{ scope.row.expireAt || '长期（可随时撤回）' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="authTagType(scope.row.status)" size="small">{{ authTagText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="撤回时间" width="160">
            <template #default="scope">{{ scope.row.revokedAt || '—' }}</template>
          </el-table-column>
        </el-table>

        <pagination v-show="authTotal > 0" :total="authTotal" v-model:page="authQuery.pageNum"
          v-model:limit="authQuery.pageSize" @pagination="loadAuth" />
      </el-tab-pane>

      <!-- 访问日志 -->
      <el-tab-pane label="访问日志" name="log">
        <el-form :inline="true" :model="logQuery" class="query-form">
          <el-form-item label="操作人">
            <el-input v-model="logQuery.operatorName" placeholder="后台账号" clearable style="width: 150px"
              @keyup.enter="loadLog" />
          </el-form-item>
          <el-form-item label="被查用户">
            <el-input v-model="logQuery.targetUserId" placeholder="用户ID" clearable style="width: 140px"
              @keyup.enter="loadLog" />
          </el-form-item>
          <el-form-item label="结果">
            <el-select v-model="logQuery.result" placeholder="全部" clearable style="width: 120px" @change="loadLog">
              <el-option label="允许" value="allowed" />
              <el-option label="拒绝" value="denied" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadLog">查询</el-button>
            <el-button icon="Refresh" @click="resetLog">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="logLoading" :data="logList" border>
          <el-table-column label="访问时间" prop="accessTime" width="170" />
          <el-table-column label="操作人" prop="operatorName" width="130" />
          <el-table-column label="被查用户" prop="targetUserId" width="100" align="center" />
          <el-table-column label="范围" width="120" align="center">
            <template #default="scope">{{ SCOPE_LABELS[scope.row.scope] || scope.row.scope }}</template>
          </el-table-column>
          <el-table-column label="动作" width="90" align="center">
            <template #default="scope">{{ scope.row.action === 'export' ? '导出' : '查看' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="90" align="center">
            <template #default="scope">
              <el-tag :type="scope.row.result === 'allowed' ? 'success' : 'danger'" size="small">
                {{ scope.row.result === 'allowed' ? '允许' : '拒绝' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="查询条件 / 条数" prop="queryDesc" min-width="200" show-overflow-tooltip />
          <el-table-column label="IP" prop="clientIp" width="140" />
        </el-table>

        <pagination v-show="logTotal > 0" :total="logTotal" v-model:page="logQuery.pageNum"
          v-model:limit="logQuery.pageSize" @pagination="loadLog" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="LedgerAuth">
import { listDataAuth, listAccessLog } from '@/api/cms/ledger'

const tab = ref('auth')

const SCOPE_LABELS = {
  transaction: '流水',
  account: '账户余额',
  budget: '预算',
  inventory: '库存',
  ai: 'AI 报告',
  profile: '身份/联系方式'
}

const authLoading = ref(false)
const authList = ref([])
const authTotal = ref(0)
const authQuery = reactive({ userId: undefined, status: undefined, pageNum: 1, pageSize: 10 })

const logLoading = ref(false)
const logList = ref([])
const logTotal = ref(0)
const logQuery = reactive({ operatorName: undefined, targetUserId: undefined, result: undefined, pageNum: 1, pageSize: 10 })

const splitScopes = scopes => (scopes ? String(scopes).split(',').filter(Boolean) : [])
const authTagType = s => ({ valid: 'success', expired: 'info', revoked: 'warning' }[s] || 'info')
const authTagText = s => ({ valid: '有效', expired: '已过期', revoked: '已撤回' }[s] || s)

function loadAuth() {
  authLoading.value = true
  listDataAuth(authQuery).then(res => {
    authList.value = res.rows || []
    authTotal.value = Number(res.total || 0)
  }).finally(() => { authLoading.value = false })
}

function resetAuth() {
  authQuery.userId = undefined
  authQuery.status = undefined
  authQuery.pageNum = 1
  loadAuth()
}

function loadLog() {
  logLoading.value = true
  listAccessLog(logQuery).then(res => {
    logList.value = res.rows || []
    logTotal.value = Number(res.total || 0)
  }).finally(() => { logLoading.value = false })
}

function resetLog() {
  logQuery.operatorName = undefined
  logQuery.targetUserId = undefined
  logQuery.result = undefined
  logQuery.pageNum = 1
  loadLog()
}

loadAuth()
loadLog()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.query-form { margin-bottom: 4px; }
.scope-tag { margin: 2px 4px 2px 0; }
</style>
