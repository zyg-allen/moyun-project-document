<template>
  <div class="app-container">
    <!-- 搜索区 -->
    <el-form :inline="true" :model="queryParams">
      <el-form-item label="用户ID">
        <el-input v-model="queryParams.userId" placeholder="精确查询" clearable style="width: 180px"
                  @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 用户维度列表 -->
    <el-table v-loading="loading" :data="userList">
      <el-table-column label="用户ID" prop="userId" width="90" />
      <el-table-column label="昵称（脱敏）" prop="nickname" width="140" />
      <el-table-column label="手机（脱敏）" prop="phone" width="140" />
      <el-table-column label="有效流水" prop="txnCount" width="100" sortable />
      <el-table-column label="最近记账" prop="lastTxnDate" width="110" />
      <el-table-column label="AI 报告数" prop="aiReportCount" width="100" />
      <el-table-column label="AI 调用次数" prop="aiCallCount" width="110" />
      <el-table-column label="Token 消耗" prop="aiTokenUsed" width="110">
        <template #default="scope">
          <span>{{ formatNumber(scope.row.aiTokenUsed) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成本（元）" prop="aiCostYuan" width="110">
        <template #default="scope">
          <span>{{ scope.row.aiCostYuan != null ? Number(scope.row.aiCostYuan).toFixed(4) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" v-hasPermi="['cms:ledgerUsers:query']"
                     @click="handleViewTxn(scope.row)">查看流水</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-alert
      title="隐私保护说明：用户身份三要素脱敏展示；流水仅含类型/金额/分类/日期（不含备注、商户、凭证、账户信息）"
      type="info" :closable="false" style="margin-top: 16px" />

    <!-- 流水抽屉（脱敏简易版） -->
    <el-drawer v-model="txnVisible" :title="'用户 ' + activeUser.userId + '（' + activeUser.nickname + '）流水明细'"
               size="620px">
      <el-table v-loading="txnLoading" :data="txnList" height="calc(100vh - 160px)">
        <el-table-column label="日期" prop="transactionDate" width="110" />
        <el-table-column label="类型" prop="typeLabel" width="90">
          <template #default="scope">
            <el-tag :type="typeTag(scope.row.type)" size="small">{{ scope.row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" min-width="110" />
        <el-table-column label="金额" prop="amount" width="120" align="right">
          <template #default="scope">
            <span :style="{ color: scope.row.type === 'income' ? '#67c23a' : (scope.row.type === 'expense' ? '#f56c6c' : '#909399') }">
              {{ scope.row.type === 'income' ? '+' : (scope.row.type === 'expense' ? '-' : '') }}{{ scope.row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="success" size="small">有效</el-tag>
            <el-tag v-else type="info" size="small">已删</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="txnTotal > 0" :total="txnTotal" v-model:page="txnQuery.pageNum"
                  v-model:limit="txnQuery.pageSize" :layout="'total, prev, pager, next'" @pagination="loadTxn" />
    </el-drawer>
  </div>
</template>

<script setup name="LedgerUsers">
import { listLedgerUsers, listUserTransactions } from "@/api/cms/ledger";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const userList = ref([]);
const total = ref(0);
const queryParams = reactive({ pageNum: 1, pageSize: 10, userId: undefined });

const txnVisible = ref(false);
const txnLoading = ref(false);
const txnList = ref([]);
const txnTotal = ref(0);
const activeUser = ref({});
const txnQuery = reactive({ pageNum: 1, pageSize: 20 });

function getList() {
  loading.value = true;
  listLedgerUsers(queryParams).then(response => {
    userList.value = response.rows || [];
    total.value = response.total || 0;
  }).finally(() => loading.value = false);
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.userId = undefined;
  handleQuery();
}

function handleViewTxn(row) {
  activeUser.value = row;
  txnVisible.value = true;
  txnQuery.pageNum = 1;
  loadTxn();
}

function loadTxn() {
  txnLoading.value = true;
  listUserTransactions(activeUser.value.userId, txnQuery).then(response => {
    txnList.value = response.rows || [];
    txnTotal.value = response.total || 0;
  }).finally(() => txnLoading.value = false);
}

function typeTag(type) {
  return { income: 'success', expense: 'danger', transfer: 'info', repayment: 'warning', borrow: 'warning', adjust: 'info' }[type] || 'info';
}

function formatNumber(v) {
  return v == null ? '0' : Number(v).toLocaleString();
}

getList();
</script>
