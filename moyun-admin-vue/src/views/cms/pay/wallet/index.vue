<template>
  <div class="app-container">
    <!-- 守恒对账汇总卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :sm="12" :md="4">
        <el-card shadow="hover">
          <template #header>钱包账户数</template>
          <div class="stat-num">{{ summary.accountCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :sm="12" :md="4">
        <el-card shadow="hover">
          <template #header>用户余额合计（元）</template>
          <div class="stat-num">{{ fmt(summary.userBalanceSum) }}</div>
        </el-card>
      </el-col>
      <el-col :sm="12" :md="4">
        <el-card shadow="hover">
          <template #header>用户累计收入（元）</template>
          <div class="stat-num">{{ fmt(summary.totalIncomeSum) }}</div>
        </el-card>
      </el-col>
      <el-col :sm="12" :md="4">
        <el-card shadow="hover">
          <template #header>累计提现（元）</template>
          <div class="stat-num">{{ fmt(summary.totalWithdrawSum) }}</div>
        </el-card>
      </el-col>
      <el-col :sm="12" :md="4">
        <el-card shadow="hover">
          <template #header>平台抽成累计（元）</template>
          <div class="stat-num" style="color: #e6a23c;">{{ fmt(summary.platformFeeSum) }}</div>
        </el-card>
      </el-col>
      <el-col :sm="12" :md="4">
        <el-card shadow="hover">
          <template #header>理论公账余额（元）</template>
          <div class="stat-num" style="color: #409eff;">{{ fmt(summary.theoreticalAccountBalance) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab">
      <!-- Tab 1：钱包账户列表 -->
      <el-tab-pane label="钱包账户" name="account">
        <el-form :model="accountQuery" ref="accountQueryRef" :inline="true" v-show="showSearch" label-width="70px">
          <el-form-item label="用户ID" prop="userId">
            <el-input v-model="accountQuery.userId" placeholder="用户ID" clearable style="width: 140px" @keyup.enter="handleAccountQuery" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="accountQuery.nickname" placeholder="用户昵称模糊" clearable style="width: 160px" @keyup.enter="handleAccountQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleAccountQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetAccountQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="accountLoading" :data="accountList">
          <el-table-column label="用户ID" align="center" prop="userId" width="90" />
          <el-table-column label="昵称" align="center" prop="nickname" width="160" :show-overflow-tooltip="true" />
          <el-table-column label="可用余额（元）" align="center" width="130">
            <template #default="scope">
              <span style="font-weight: 600; color: #409eff;">¥{{ fmt(scope.row.balance) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="累计收入（元）" align="center" width="130">
            <template #default="scope">¥{{ fmt(scope.row.totalIncome) }}</template>
          </el-table-column>
          <el-table-column label="累计提现（元）" align="center" width="130">
            <template #default="scope">¥{{ fmt(scope.row.totalWithdraw) }}</template>
          </el-table-column>
          <el-table-column label="乐观锁" align="center" prop="version" width="80" />
          <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
            <template #default="scope">{{ parseTime(scope.row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="120">
            <template #default="scope">
              <el-button link type="primary" icon="Search" @click="viewUserLedger(scope.row)">查流水</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="accountTotal > 0"
          :total="accountTotal"
          v-model:page="accountQuery.pageNum"
          v-model:limit="accountQuery.pageSize"
          @pagination="getAccountList"
        />
      </el-tab-pane>

      <!-- Tab 2：全平台资金流水（复式记账） -->
      <el-tab-pane label="资金流水" name="ledger">
        <el-form :model="ledgerQuery" ref="ledgerQueryRef" :inline="true" v-show="showSearch" label-width="70px">
          <el-form-item label="账户" prop="accountRole">
            <el-select v-model="ledgerQuery.accountRole" placeholder="全部账户" clearable style="width: 120px">
              <el-option label="用户" value="USER" />
              <el-option label="平台" value="PLATFORM" />
            </el-select>
          </el-form-item>
          <el-form-item label="方向" prop="direction">
            <el-select v-model="ledgerQuery.direction" placeholder="全部方向" clearable style="width: 110px">
              <el-option label="入账" value="credit" />
              <el-option label="出账" value="debit" />
            </el-select>
          </el-form-item>
          <el-form-item label="支付单号" prop="payNo">
            <el-input v-model="ledgerQuery.payNo" placeholder="pay_no 模糊" clearable style="width: 180px" @keyup.enter="handleLedgerQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleLedgerQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetLedgerQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="ledgerLoading" :data="ledgerList">
          <el-table-column label="流水号" align="center" prop="id" width="90" />
          <el-table-column label="账户" align="center" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.accountRole === 'PLATFORM' ? 'warning' : 'primary'" size="small">
                {{ scope.row.accountRole === 'PLATFORM' ? '平台' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="用户" align="center" prop="nickname" width="150" :show-overflow-tooltip="true">
            <template #default="scope">
              <span v-if="scope.row.accountRole === 'PLATFORM'">—</span>
              <span v-else>{{ scope.row.nickname || ('用户' + scope.row.userId) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="方向" align="center" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.direction === 'credit' ? 'success' : 'danger'" size="small">
                {{ scope.row.direction === 'credit' ? '入账' : '出账' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额（元）" align="center" width="110">
            <template #default="scope">¥{{ fmt(scope.row.amount) }}</template>
          </el-table-column>
          <el-table-column label="变动后余额（元）" align="center" width="130">
            <template #default="scope">¥{{ fmt(scope.row.balanceAfter) }}</template>
          </el-table-column>
          <el-table-column label="业务类型" align="center" prop="bizType" width="100" />
          <el-table-column label="支付单号" align="center" prop="payNo" width="220" :show-overflow-tooltip="true" />
          <el-table-column label="摘要" align="center" prop="summary" :show-overflow-tooltip="true" />
          <el-table-column label="时间" align="center" prop="createTime" width="170">
            <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="ledgerTotal > 0"
          :total="ledgerTotal"
          v-model:page="ledgerQuery.pageNum"
          v-model:limit="ledgerQuery.pageSize"
          @pagination="getLedgerList"
        />
      </el-tab-pane>
    </el-tabs>

    <el-alert type="info" :closable="false" show-icon class="mt8">
      <p>单钱包架构（v11.79）：pay_user_account 为全平台唯一钱包，社区钱包已废弃。守恒对账：真钱集中于平台公账商户号，理论公账余额 = 平台抽成累计 + Σ用户余额（提现已从余额扣除）。</p>
    </el-alert>
  </div>
</template>

<script setup name="CmsPayWallet">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listWalletAccounts, walletSummary, listPayLedger } from "@/api/cms/pay";

const { proxy } = getCurrentInstance();

const activeTab = ref("account");
const showSearch = ref(true);

// ===== 守恒对账汇总 =====
const summary = ref({});

// ===== Tab 1：钱包账户 =====
const accountList = ref([]);
const accountLoading = ref(true);
const accountTotal = ref(0);
const accountQuery = reactive({ pageNum: 1, pageSize: 10, userId: undefined, nickname: undefined });

// ===== Tab 2：资金流水 =====
const ledgerList = ref([]);
const ledgerLoading = ref(false);
const ledgerTotal = ref(0);
const ledgerQuery = reactive({ pageNum: 1, pageSize: 10, accountRole: undefined, direction: undefined, payNo: undefined, userId: undefined });

function fmt(v) {
  const n = Number(v || 0);
  return isNaN(n) ? "0.00" : n.toFixed(2);
}

function getSummary() {
  walletSummary().then((response) => {
    summary.value = response.data || {};
  });
}

function getAccountList() {
  accountLoading.value = true;
  listWalletAccounts(accountQuery).then((response) => {
    const page = response.data || {};
    accountList.value = page.records || [];
    accountTotal.value = page.total || 0;
    accountLoading.value = false;
  }).catch(() => {
    accountLoading.value = false;
  });
}

function handleAccountQuery() {
  accountQuery.pageNum = 1;
  getAccountList();
}

function resetAccountQuery() {
  proxy.resetForm("accountQueryRef");
  handleAccountQuery();
}

function getLedgerList() {
  ledgerLoading.value = true;
  listPayLedger(ledgerQuery).then((response) => {
    const page = response.data || {};
    ledgerList.value = page.records || [];
    ledgerTotal.value = page.total || 0;
    ledgerLoading.value = false;
  }).catch(() => {
    ledgerLoading.value = false;
  });
}

function handleLedgerQuery() {
  ledgerQuery.pageNum = 1;
  getLedgerList();
}

function resetLedgerQuery() {
  proxy.resetForm("ledgerQueryRef");
  ledgerQuery.userId = undefined;
  handleLedgerQuery();
}

/** 从账户行查看该用户流水 */
function viewUserLedger(row) {
  activeTab.value = "ledger";
  ledgerQuery.userId = row.userId;
  ledgerQuery.accountRole = undefined;
  ledgerQuery.direction = undefined;
  ledgerQuery.payNo = undefined;
  ledgerQuery.pageNum = 1;
  getLedgerList();
}

onMounted(() => {
  getSummary();
  getAccountList();
});
</script>

<style scoped>
.stat-num {
  font-size: 20px;
  font-weight: 600;
  text-align: center;
}
</style>
