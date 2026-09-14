<template>
  <div class="app-container">
    <!-- 顶部指标卡（口径见页面底部说明） -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>全平台成交总额（GMV）</template>
          <div class="metric">¥{{ fmt(totals.grandTotal) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>平台直接所得</template>
          <div class="metric" style="color: #f56c6c;">¥{{ fmt(totals.platformDirect) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>作者/用户所得</template>
          <div class="metric" style="color: #67c23a;">¥{{ fmt(totals.userShare) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>订单总数</template>
          <div class="metric">{{ totals.orderCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二行指标：退款 / 提现 / 钱包 / 守恒（v11.79） -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>累计退款</template>
          <div class="metric" style="color: #909399;">¥{{ fmt(totals.refundedAmount) }}<span class="metric-sub"> / {{ totals.refundedCount || 0 }} 单</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>累计提现打款</template>
          <div class="metric" style="color: #e6a23c;">¥{{ fmt(totals.withdrawnAmount) }}<span class="metric-sub"> / {{ totals.withdrawCount || 0 }} 单</span></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>用户余额合计</template>
          <div class="metric" style="color: #67c23a;">¥{{ fmt(totals.userBalanceSum) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <template #header>理论公账余额</template>
          <div class="metric" style="color: #409eff;">¥{{ fmt(totals.theoreticalAccount) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 平台 → 渠道 两级划分 -->
    <el-row :gutter="16" class="mb8" v-loading="loading">
      <el-col v-for="p in platforms" :key="p.code" :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="platform-header">
              <el-tag type="primary" effect="dark">{{ p.name }}</el-tag>
              <span class="platform-total">合计 ¥{{ fmt(p.totalAmount) }} / {{ p.orderCount || 0 }} 单</span>
            </div>
          </template>
          <el-table :data="p.channels" size="small">
            <el-table-column label="收入渠道" prop="name" min-width="150" />
            <el-table-column label="金额（元）" align="center" width="110">
              <template #default="scope">
                <span :style="{ fontWeight: 600, color: scope.row.amount > 0 ? '#409eff' : '#999' }">¥{{ fmt(scope.row.amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="订单数" align="center" prop="orderCount" width="80" />
            <el-table-column label="支付方式" min-width="170">
              <template #default="scope">
                <template v-if="scope.row.payWays && scope.row.payWays.length">
                  <span v-for="(w, i) in scope.row.payWays" :key="w.way" class="pay-way">
                    {{ w.wayName }} ¥{{ fmt(w.amount) }}（{{ w.orderCount }}）{{ i < scope.row.payWays.length - 1 ? '；' : '' }}
                  </span>
                </template>
                <span v-else class="pay-way">—</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" align="center" width="90">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 'active' ? '运营中' : '规划中' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近6月趋势 -->
    <el-card shadow="never" class="mb8">
      <template #header>近 6 月成交趋势（元 / 单，App + 门户）</template>
      <el-table :data="monthlyTrend" size="small">
        <el-table-column label="月份" prop="ym" width="120" />
        <el-table-column label="成交金额（元）" align="center" width="160">
          <template #default="scope">
            <span style="font-weight: 600;">¥{{ fmt(scope.row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单数" align="center" prop="orderCount" width="120" />
        <el-table-column label="占比条" min-width="300">
          <template #default="scope">
            <el-progress :percentage="percentOf(scope.row.amount)" :stroke-width="14" :show-text="false" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 相关页面 -->
    <el-card shadow="never" class="mb8">
      <template #header>相关管理</template>
      <el-button type="primary" plain @click="go('/pay/income-order')">收入订单（业务订单）</el-button>
      <el-button type="primary" plain @click="go('/pay/wallet')">用户钱包（余额/流水/对账）</el-button>
      <el-button type="primary" plain @click="go('/pay/withdraw')">提现审核（打款/驳回）</el-button>
      <el-button type="primary" plain @click="go('/pay/order')">支付订单（通道单据）</el-button>
      <el-button type="primary" plain @click="go('/pay/config')">支付配置（费率/开关）</el-button>
    </el-card>

    <!-- 口径说明 -->
    <el-alert type="info" :closable="false" show-icon>
      <p>统计口径（前后台一致，金额单位：元，状态枚举 v11.79 统一）：GMV = 各渠道成功订单合计（App 打赏 + 门户打赏/付费阅读，status=paid）；平台直接所得 = App 打赏（平台对象）+ 公共通道平台抽成；pay_order 为通道单据不重复计入 GMV（业务订单为准，避免双算）；守恒对账：理论公账余额 = 平台抽成累计 + Σ用户余额（提现已从余额扣除）；面试会员 / 简历优化 / 记账 VIP 为规划中渠道，暂无订单表。</p>
    </el-alert>
  </div>
</template>

<script setup name="CmsPayRevenue">
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { getRevenueOverview } from "@/api/cms/pay";

const router = useRouter();
const loading = ref(false);
const totals = ref({});
const platforms = ref([]);
const monthlyTrend = ref([]);

function fmt(v) {
  const n = Number(v || 0);
  return isNaN(n) ? "0.00" : n.toFixed(2);
}

function percentOf(amount) {
  const max = Math.max(...monthlyTrend.value.map(r => Number(r.amount || 0)), 0);
  return max > 0 ? Math.round((Number(amount || 0) / max) * 100) : 0;
}

function go(path) {
  router.push(path);
}

function load() {
  loading.value = true;
  getRevenueOverview().then((response) => {
    const data = response.data || {};
    totals.value = data.totals || {};
    platforms.value = data.platforms || [];
    monthlyTrend.value = data.monthlyTrend || [];
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
}

onMounted(load);
</script>

<style scoped>
.metric { font-size: 24px; font-weight: 700; }
.metric-sub { font-size: 13px; font-weight: 400; color: #999; }
.platform-header { display: flex; align-items: center; justify-content: space-between; }
.platform-total { font-size: 14px; color: #666; }
.pay-way { font-size: 12px; color: #666; }
</style>
