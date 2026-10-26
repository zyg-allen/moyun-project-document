<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" class="page-tip"
      title="记账模块运营总览：仅聚合指标（无用户个体数据）。指标口径以卡片说明为准；样本用户数小于 10 的模块显示为「<10」以避免反识别。" />

    <!-- 1. 核心指标 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>记账用户数</span></template>
          <div class="stat-value">{{ stats.userCount || 0 }}</div>
          <div class="stat-sub">口径：持有任一启用资产/负债账户的去重用户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>近30日活跃</span></template>
          <div class="stat-value">{{ stats.activeUserCount30d || 0 }}</div>
          <div class="stat-sub">口径：近 30 日有「有效流水」的去重用户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>有效流水数</span></template>
          <div class="stat-value">{{ validTxnCount }}</div>
          <div class="stat-sub">口径：status=1 的流水（已删不计）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>历史流水总量</span></template>
          <div class="stat-value">{{ stats.transactionCount || 0 }}</div>
          <div class="stat-sub">口径：含已删除，反映历史记账行为量</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 2. 模块使用情况 -->
    <el-card shadow="hover" class="mt20">
      <template #header><span>模块使用情况（去重用户数 / 对记账用户的渗透率）</span></template>
      <el-table :data="moduleRows" v-loading="loading" size="small">
        <el-table-column label="模块" prop="label" width="170">
          <template #default="scope">
            <el-tag size="small">{{ scope.row.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用用户数" width="130" align="center">
          <template #default="scope">{{ scope.row.masked ? '<10' : scope.row.userCount }}</template>
        </el-table-column>
        <el-table-column label="渗透率（÷ 记账用户数）" min-width="220">
          <template #default="scope">
            <el-progress v-if="!scope.row.masked" :percentage="scope.row.rate" :stroke-width="12" />
            <span v-else class="stat-sub">样本过小，不展示比例</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="desc" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>

    <!-- 3. AI 与 Token 消耗 -->
    <el-row :gutter="20" class="mt20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>AI 累计调用</span></template>
          <div class="stat-value">{{ aiStats.callCount || 0 }}</div>
          <div class="stat-sub">网关执行日志全量（含系统调用）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>累计 Token 消耗</span></template>
          <div class="stat-value">{{ formatToken(aiStats.tokenTotal) }}</div>
          <div class="stat-sub">全场景 Token 用量</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>累计调用成本（元）</span></template>
          <div class="stat-value">{{ aiStats.costYuan || '0.00' }}</div>
          <div class="stat-sub">模型侧成本，不等同于用户收费</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>记账类型分布</span></template>
          <div v-for="r in typeRows" :key="r.type" class="type-line">
            <el-tag size="small">{{ r.label }}</el-tag>
            <span class="type-count">{{ r.count }} 笔（{{ r.rate }}%）</span>
          </div>
          <div v-if="!typeRows.length" class="stat-sub">暂无有效流水</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 4. AI 场景分布 -->
    <el-card shadow="hover" class="mt20" v-if="sceneRows.length">
      <template #header><span>AI 场景分布（调用量 / Token / 成本）</span></template>
      <el-table :data="sceneRows" size="small">
        <el-table-column label="场景" prop="sceneCode" width="220" />
        <el-table-column label="调用次数" prop="calls" width="120" />
        <el-table-column label="Token 消耗" prop="tokens" width="140">
          <template #default="scope">{{ formatToken(scope.row.tokens) }}</template>
        </el-table-column>
        <el-table-column label="成本（元）" prop="costYuan" width="120" />
        <el-table-column label="说明" min-width="200">
          <template #default="scope">{{ SCENE_LABELS[scope.row.sceneCode] || '—' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 5. 快捷入口 -->
    <el-card shadow="hover" class="mt20">
      <template #header><span>记账运营快捷入口</span></template>
      <el-space wrap :size="12">
        <el-button @click="go('/ledger-app/users')">用户数据（授权查询）</el-button>
        <el-button @click="go('/ledger-app/ledger-category')">预设分类</el-button>
        <el-button @click="go('/ledger-app/product-category')">商品类目</el-button>
        <el-button @click="go('/ledger-app/channel')">渠道管理</el-button>
        <el-button @click="go('/ledger-app/app-feature')">功能入口配置</el-button>
        <el-button @click="go('/ledger-app/auth')">授权与审计</el-button>
        <el-button type="primary" plain @click="load">刷新本页</el-button>
      </el-space>
      <div class="stat-sub mt12">
        记账字典（账户类型/身份标签/交易类型）统一在「系统管理 → 字典管理」维护，账本类下拉不再写死在 App 代码里。
      </div>
    </el-card>

    <el-alert class="mt20" type="info" :closable="false"
      title="隐私说明：本页只返回聚合指标，不返回任何用户个体数据、金额明细与账户信息；个体数据查询请到「用户数据（授权查询）」，并受用户授权约束。" />
  </div>
</template>

<script setup name="LedgerStats">
import { getLedgerStats } from '@/api/cms/ledger'

const router = useRouter()

const loading = ref(false)
const stats = ref({})

const TYPE_LABELS = {
  income: '收入', expense: '支出', transfer: '转账', lend: '借出',
  collect: '收款', repayment: '还款', borrow: '借款', adjust: '余额校准'
}

const SCENE_LABELS = {
  finance_analysis: 'AI 财务分析（记账报告）'
}

/** 模块说明（与后端 moduleUsage.key 对应） */
const MODULE_DESC = {
  transaction: '记过至少一笔流水的用户',
  assetAccount: '有启用资产/负债账户的用户',
  liability: '有启用负债账户的用户',
  budget: '设置过预算的用户',
  saving: '创建过存钱计划的用户',
  schedule: '创建过定时记账任务的用户',
  memo: '使用过备忘录的用户',
  inventory: '创建过仓库/商品的用户',
  aiReport: '生成过 AI 财务分析报告的用户'
}

const aiStats = computed(() => stats.value.aiStats || {})

const validTxnCount = computed(() => {
  const dist = stats.value.typeDistribution || {}
  return Object.values(dist).reduce((s, v) => s + Number(v || 0), 0)
})

const typeRows = computed(() => {
  const dist = stats.value.typeDistribution || {}
  const total = validTxnCount.value || 1
  return Object.entries(dist).map(([type, count]) => ({
    type,
    label: TYPE_LABELS[type] || type,
    count,
    rate: Math.round((Number(count) / total) * 100)
  })).sort((a, b) => b.count - a.count)
})

const moduleRows = computed(() => {
  const rows = stats.value.moduleUsage || []
  const base = Math.max(stats.value.userCount || 0, 1)
  return rows.map(m => {
    // 后端已做 k-匿名：userCount<10 时 userCount=null、masked=true
    const masked = m.masked === true
    return {
      ...m,
      desc: MODULE_DESC[m.key] || '',
      masked,
      rate: Math.min(100, Math.round((Number(m.userCount || 0) / base) * 100))
    }
  })
})

const sceneRows = computed(() => aiStats.value.sceneDistribution || [])

const formatToken = v => {
  const n = Number(v) || 0
  return n >= 10000 ? (n / 10000).toFixed(1) + ' 万' : String(n)
}

function go(path) {
  router.push(path)
}

function load() {
  loading.value = true
  getLedgerStats().then(response => {
    stats.value = (response.data || response) || {}
  }).finally(() => { loading.value = false })
}

load()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.mt20 { margin-top: 20px; }
.mt12 { margin-top: 12px; }
.stat-value { font-size: 30px; font-weight: 700; color: #6a4fd4; }
.stat-sub { font-size: 12px; color: #909399; margin-top: 8px; line-height: 1.6; }
.type-line { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.type-line:first-child { margin-top: 0; }
.type-count { font-size: 12px; color: #606266; }
</style>
