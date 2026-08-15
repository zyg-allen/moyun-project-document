<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <!-- 钱包列表 -->
      <el-tab-pane label="钱包列表" name="wallet">
        <el-form :model="walletQuery" ref="walletQueryRef" :inline="true" v-show="showSearch">
          <el-form-item label="用户ID" prop="userId">
            <el-input v-model="walletQuery.userId" placeholder="请输入用户ID" clearable style="width: 200px" @keyup.enter="handleWalletQuery" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleWalletQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetWalletQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="walletLoading" :data="walletList">
          <el-table-column label="编号" align="center" prop="id" width="80" />
          <el-table-column label="用户ID" align="center" prop="userId" width="100" />
          <el-table-column label="可用余额" align="center" prop="balance" width="120">
            <template #default="scope">
              <span style="color: #67c23a; font-weight: bold;">{{ scope.row.balance }}</span>
            </template>
          </el-table-column>
          <el-table-column label="冻结余额" align="center" prop="frozenBalance" width="120">
            <template #default="scope">
              <span style="color: #e6a23c;">{{ scope.row.frozenBalance }}</span>
            </template>
          </el-table-column>
          <el-table-column label="累计充值" align="center" prop="totalRecharge" width="120" />
          <el-table-column label="累计提现" align="center" prop="totalWithdraw" width="120" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
              <span>{{ scope.row.createTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
            <template #default="scope">
              <span>{{ scope.row.updateTime || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="walletTotal > 0" :total="walletTotal" v-model:page="walletQuery.pageNum" v-model:limit="walletQuery.pageSize" @pagination="getWalletList" />
      </el-tab-pane>

      <!-- 交易流水 -->
      <el-tab-pane label="交易流水" name="transaction">
        <el-form :model="transQuery" ref="transQueryRef" :inline="true" v-show="showSearch">
          <el-form-item label="用户ID" prop="userId">
            <el-input v-model="transQuery.userId" placeholder="请输入用户ID" clearable style="width: 200px" @keyup.enter="handleTransQuery" />
          </el-form-item>
          <el-form-item label="交易类型" prop="type">
            <el-select v-model="transQuery.type" placeholder="交易类型" clearable style="width: 200px">
              <el-option label="充值" value="recharge" />
              <el-option label="消费" value="consume" />
              <el-option label="退款" value="refund" />
              <el-option label="提现" value="withdraw" />
              <el-option label="打赏" value="tip" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleTransQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetTransQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="transLoading" :data="transList">
          <el-table-column label="编号" align="center" prop="id" width="80" />
          <el-table-column label="用户ID" align="center" prop="userId" width="100" />
          <el-table-column label="交易类型" align="center" prop="type" width="100">
            <template #default="scope">
              <el-tag :type="transTagType(scope.row.type)">{{ transLabel(scope.row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" align="center" prop="amount" width="120">
            <template #default="scope">
              <span :style="{ color: scope.row.type === 'recharge' || scope.row.type === 'refund' ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
                {{ scope.row.type === 'recharge' || scope.row.type === 'refund' ? '+' : '-' }}{{ scope.row.amount }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="交易前余额" align="center" prop="balanceBefore" width="120" />
          <el-table-column label="交易后余额" align="center" prop="balanceAfter" width="120" />
          <el-table-column label="关联订单" align="center" prop="orderId" width="100">
            <template #default="scope">
              <span>{{ scope.row.orderId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" min-width="150" />
          <el-table-column label="时间" align="center" prop="createTime" width="180">
            <template #default="scope">
              <span>{{ scope.row.createTime || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <pagination v-show="transTotal > 0" :total="transTotal" v-model:page="transQuery.pageNum" v-model:limit="transQuery.pageSize" @pagination="getTransList" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="Wallet">
import { listWallet, listTransaction } from "@/api/cms/wallet";

const { proxy } = getCurrentInstance();

const activeTab = ref("wallet");
const showSearch = ref(true);

// 钱包列表
const walletList = ref([]);
const walletLoading = ref(true);
const walletTotal = ref(0);
const walletQuery = reactive({ pageNum: 1, pageSize: 10, userId: undefined });

// 交易流水
const transList = ref([]);
const transLoading = ref(true);
const transTotal = ref(0);
const transQuery = reactive({ pageNum: 1, pageSize: 10, userId: undefined, type: undefined });

function getWalletList() {
  walletLoading.value = true;
  listWallet(walletQuery).then(response => {
    walletList.value = response.data.records;
    walletTotal.value = response.data.total;
    walletLoading.value = false;
  });
}

function getTransList() {
  transLoading.value = true;
  listTransaction(transQuery).then(response => {
    transList.value = response.data.records;
    transTotal.value = response.data.total;
    transLoading.value = false;
  });
}

function handleWalletQuery() {
  walletQuery.pageNum = 1;
  getWalletList();
}

function resetWalletQuery() {
  walletQuery.userId = undefined;
  handleWalletQuery();
}

function handleTransQuery() {
  transQuery.pageNum = 1;
  getTransList();
}

function resetTransQuery() {
  transQuery.userId = undefined;
  transQuery.type = undefined;
  handleTransQuery();
}

function transLabel(type) {
  const map = { recharge: '充值', consume: '消费', refund: '退款', withdraw: '提现', tip: '打赏' };
  return map[type] || type;
}

function transTagType(type) {
  const map = { recharge: 'success', consume: 'danger', refund: 'warning', withdraw: 'info', tip: '' };
  return map[type] || '';
}

watch(activeTab, (val) => {
  if (val === 'wallet' && walletList.value.length === 0) getWalletList();
  if (val === 'transaction' && transList.value.length === 0) getTransList();
});

getWalletList();
</script>
