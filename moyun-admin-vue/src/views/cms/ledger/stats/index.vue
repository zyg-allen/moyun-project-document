<template>
  <div class="app-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>记账用户数</span></template>
          <div class="stat-value">{{ stats.userCount || 0 }}</div>
          <div class="stat-sub">持有任一启用账户（资产∪负债）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>近30日活跃</span></template>
          <div class="stat-value">{{ stats.activeUserCount30d || 0 }}</div>
          <div class="stat-sub">近30日有记账行为的用户</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>累计流水数</span></template>
          <div class="stat-value">{{ stats.transactionCount || 0 }}</div>
          <div class="stat-sub">含已删除（历史行为量）</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>有效流水数</span></template>
          <div class="stat-value">{{ validTxnCount }}</div>
          <div class="stat-sub">当前有效记账记录</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 20px">
      <template #header><span>记账类型分布（有效流水）</span></template>
      <el-table :data="typeRows" v-loading="loading">
        <el-table-column label="类型" prop="label" width="200">
          <template #default="scope">
            <el-tag>{{ scope.row.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="笔数" prop="count" />
        <el-table-column label="占比" prop="rate" width="200">
          <template #default="scope">
            <el-progress :percentage="scope.row.rate" :stroke-width="12" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-alert
      title="脱敏说明：本页仅展示聚合指标，不提供任何用户个体数据、金额明细与账户信息查询（用户隐私红线）"
      type="info"
      :closable="false"
      style="margin-top: 20px"
    />
  </div>
</template>

<script setup name="LedgerStats">
import { getLedgerStats } from "@/api/cms/ledger";

const loading = ref(false);
const stats = ref({});

const TYPE_LABELS = {
  income: '收入', expense: '支出', transfer: '转账',
  repayment: '还款', borrow: '借款', adjust: '余额校准'
};

const validTxnCount = computed(() => {
  const dist = stats.value.typeDistribution || {};
  return Object.values(dist).reduce((s, v) => s + v, 0);
});

const typeRows = computed(() => {
  const dist = stats.value.typeDistribution || {};
  const total = validTxnCount.value || 1;
  return Object.entries(dist).map(([type, count]) => ({
    type,
    label: TYPE_LABELS[type] || type,
    count,
    rate: Math.round((count / total) * 100)
  })).sort((a, b) => b.count - a.count);
});

function load() {
  loading.value = true;
  getLedgerStats().then(response => {
    stats.value = (response.data || response) || {};
    loading.value = false;
  }).catch(() => { loading.value = false; });
}

load();
</script>

<style scoped>
.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #6a4fd4;
}
.stat-sub {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}
</style>
