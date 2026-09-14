<template>
  <div class="app-container">
    <!-- 页面定位说明 -->
    <el-alert type="info" :closable="false" style="margin-bottom: 16px"
      title="记账模块运营总览：用户规模 → 模块使用 → AI/Token 消耗 → 收益现状（脱敏聚合，无用户个体数据）" />

    <!-- 1. 核心指标 -->
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

    <!-- 2. 模块使用情况 -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header><span>模块使用情况（使用用户数 / 对记账用户的渗透率）</span></template>
      <el-table :data="moduleRows" v-loading="loading">
        <el-table-column label="模块" prop="label" width="160">
          <template #default="scope">
            <el-tag>{{ scope.row.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用用户数" prop="userCount" width="120" />
        <el-table-column label="渗透率（÷ 记账用户数）" min-width="200">
          <template #default="scope">
            <el-progress :percentage="scope.row.rate" :stroke-width="12" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 3. AI 与 Token 消耗 -->
    <el-row :gutter="20" style="margin-top: 20px">
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
          <div class="stat-sub">模型侧成本，VIP/激励广告定价参考</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>记账类型分布</span></template>
          <div v-for="r in typeRows" :key="r.type" class="type-line">
            <el-tag size="small">{{ r.label }}</el-tag>
            <span class="type-count">{{ r.count }} 笔（{{ r.rate }}%）</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 4. AI 场景分布 -->
    <el-card shadow="hover" style="margin-top: 20px" v-if="sceneRows.length">
      <template #header><span>AI 场景分布（调用量 / Token / 成本）</span></template>
      <el-table :data="sceneRows" size="small">
        <el-table-column label="场景" prop="sceneCode" width="200" />
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

    <!-- 5. 收益现状与规划 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>收益现状：全平台收入（已上线）</span></template>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="成功赞赏笔数">{{ tipStats.orderCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="累计赞赏金额（元）">{{ tipStats.amountYuan || '0.00' }}</el-descriptions-item>
            <el-descriptions-item label="赞赏开发者">{{ (tipStats.byTarget || {}).developer || 0 }} 笔</el-descriptions-item>
            <el-descriptions-item label="赞赏平台">{{ (tipStats.byTarget || {}).platform || 0 }} 笔</el-descriptions-item>
          </el-descriptions>
          <div class="stat-sub" style="margin-top: 12px">
            全平台收入（App 打赏 + 门户打赏/付费阅读 + 提现/对账）已统一收敛至「收入管理」模块
            <el-link type="primary" :underline="false" style="margin-left: 4px; vertical-align: baseline;" @click="goRevenue">前往收入总览 →</el-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>收益规划（轻变现原则：不伤用户认知）</span></template>
          <el-timeline style="padding-left: 4px">
            <el-timeline-item timestamp="近期" type="primary">
              <b>VIP 会员（免费功能全保留）</b>：AI 深度分析加量、多账本、数据导出、云备份——工具型行业标准做法
            </el-timeline-item>
            <el-timeline-item timestamp="近期" type="primary">
              <b>激励广告（可选替代付费）</b>：仅在"解锁额外 AI 分析次数"等主动场景展示，禁止开屏/插页打扰
            </el-timeline-item>
            <el-timeline-item timestamp="进行中" type="success">
              <b>生态互导（零成本启动）</b>：App 引导至墨韵门户（社区/专栏/打赏），内容侧变现
            </el-timeline-item>
          </el-timeline>
          <div class="stat-sub">原则：基础记账/预算/报表永久免费，仅对增值能力收费；上线前在「功能配置」开启对应入口</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 6. 相关链接（运营联动 + 引导使用门户平台） -->
    <el-card shadow="hover" style="margin-top: 20px">
      <template #header><span>相关链接（记账运营联动）</span></template>
      <el-space wrap :size="12">
        <el-button @click="go('/ledger/users')"><el-icon style="margin-right: 4px"><User /></el-icon>用户管理（流水/AI 使用明细）</el-button>
        <el-button @click="go('/ledger/category')"><el-icon style="margin-right: 4px"><FolderOpened /></el-icon>预设分类管理</el-button>
        <el-button @click="go('/ledger/app-feature')"><el-icon style="margin-right: 4px"><Setting /></el-icon>小程序功能配置（门户社区入口在此开启）</el-button>
        <el-button type="primary" plain @click="go('/ledger/stats')"><el-icon style="margin-right: 4px"><DataAnalysis /></el-icon>刷新本页数据</el-button>
      </el-space>
      <div class="stat-sub" style="margin-top: 12px">
        引导使用门户平台：用户端已规划「墨韵社区」入口（小程序"我的"页推荐区），由后台功能配置可视化开启/调整链接；门户侧内容运营（专栏/打赏/交易）在门户管理菜单维护
      </div>
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

const router = useRouter();

const loading = ref(false);
const stats = ref({});

const TYPE_LABELS = {
  income: '收入', expense: '支出', transfer: '转账',
  repayment: '还款', borrow: '借款', adjust: '余额校准'
};

const SCENE_LABELS = {
  finance_analysis: 'AI 财务分析（记账报告）'
};

const aiStats = computed(() => stats.value.aiStats || {});
const tipStats = computed(() => stats.value.tipStats || {});

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

const moduleRows = computed(() => {
  const rows = stats.value.moduleUsage || [];
  const base = Math.max(stats.value.userCount || 0, 1);
  return rows.map(m => ({ ...m, rate: Math.min(100, Math.round((m.userCount / base) * 100)) }));
});

const sceneRows = computed(() => aiStats.value.sceneDistribution || []);

const formatToken = (v) => {
  const n = Number(v) || 0;
  return n >= 10000 ? (n / 10000).toFixed(1) + ' 万' : String(n);
};

function go(path) {
  router.push(path);
}

/** 跳转收入管理-收入总览（v11.79 全平台收入收敛） */
function goRevenue() {
  router.push('/pay/revenue');
}

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
.type-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.type-line:first-child {
  margin-top: 0;
}
.type-count {
  font-size: 12px;
  color: #666;
}
</style>
