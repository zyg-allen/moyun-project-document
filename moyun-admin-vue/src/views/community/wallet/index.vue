<template>
  <div class="app-container">
    <el-card class="box-card" style="margin-bottom: 20px">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总用户数</div>
            <div class="stat-value">{{ stats.totalUsers }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总余额</div>
            <div class="stat-value">¥{{ stats.totalBalance }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总充值</div>
            <div class="stat-value">¥{{ stats.totalRecharge }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总提现</div>
            <div class="stat-value">¥{{ stats.totalWithdraw }}</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="钱包列表" name="wallet">
        <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
          <el-form-item label="用户ID" prop="userId">
            <el-input
              v-model="queryParams.userId"
              placeholder="请输入用户ID"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="queryParams.username"
              placeholder="请输入用户名"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>

        <el-table v-loading="loading" :data="walletList">
          <el-table-column label="钱包ID" align="center" prop="walletId" width="80" />
          <el-table-column label="用户ID" align="center" prop="userId" width="80" />
          <el-table-column label="用户名" align="center" prop="username" width="120" />
          <el-table-column label="余额" align="center" prop="balance" width="100">
            <template #default="scope">
              ¥{{ scope.row.balance }}
            </template>
          </el-table-column>
          <el-table-column label="冻结金额" align="center" prop="frozenBalance" width="100">
            <template #default="scope">
              ¥{{ scope.row.frozenBalance }}
            </template>
          </el-table-column>
          <el-table-column label="总充值" align="center" prop="totalRecharge" width="100">
            <template #default="scope">
              ¥{{ scope.row.totalRecharge }}
            </template>
          </el-table-column>
          <el-table-column label="总提现" align="center" prop="totalWithdraw" width="100">
            <template #default="scope">
              ¥{{ scope.row.totalWithdraw }}
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </el-tab-pane>

      <el-tab-pane label="交易记录" name="transaction">
        <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
          <el-form-item label="用户ID" prop="userId">
            <el-input
              v-model="queryParams.userId"
              placeholder="请输入用户ID"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="交易类型" prop="type">
            <el-select v-model="queryParams.type" placeholder="请选择类型" clearable>
              <el-option label="充值" value="recharge" />
              <el-option label="消费" value="consume" />
              <el-option label="提现" value="withdraw" />
              <el-option label="退款" value="refund" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>

        <el-table v-loading="loading" :data="transactionList">
          <el-table-column label="交易ID" align="center" prop="transactionId" width="80" />
          <el-table-column label="用户ID" align="center" prop="userId" width="80" />
          <el-table-column label="交易类型" align="center" prop="type" width="100">
            <template #default="scope">
              <el-tag v-if="scope.row.type === 'recharge'" type="success">充值</el-tag>
              <el-tag v-else-if="scope.row.type === 'consume'" type="primary">消费</el-tag>
              <el-tag v-else-if="scope.row.type === 'withdraw'" type="warning">提现</el-tag>
              <el-tag v-else type="info">退款</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" align="center" prop="amount" width="100">
            <template #default="scope">
              {{ scope.row.type === 'consume' || scope.row.type === 'withdraw' ? '-' : '+' }}¥{{ scope.row.amount }}
            </template>
          </el-table-column>
          <el-table-column label="变动前" align="center" prop="balanceBefore" width="100">
            <template #default="scope">
              ¥{{ scope.row.balanceBefore }}
            </template>
          </el-table-column>
          <el-table-column label="变动后" align="center" prop="balanceAfter" width="100">
            <template #default="scope">
              ¥{{ scope.row.balanceAfter }}
            </template>
          </el-table-column>
          <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
          <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </el-tab-pane>

      <el-tab-pane label="充值记录" name="recharge">
        <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
          <el-form-item label="用户ID" prop="userId">
            <el-input
              v-model="queryParams.userId"
              placeholder="请输入用户ID"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="待支付" value="pending" />
              <el-option label="已支付" value="paid" />
              <el-option label="已取消" value="cancelled" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>

        <el-table v-loading="loading" :data="rechargeList">
          <el-table-column label="充值ID" align="center" prop="rechargeId" width="80" />
          <el-table-column label="用户ID" align="center" prop="userId" width="80" />
          <el-table-column label="订单号" align="center" prop="orderNo" width="180" />
          <el-table-column label="金额" align="center" prop="amount" width="100">
            <template #default="scope">
              ¥{{ scope.row.amount }}
            </template>
          </el-table-column>
          <el-table-column label="支付方式" align="center" prop="payMethod" width="100" />
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 'pending'" type="warning">待支付</el-tag>
              <el-tag v-else-if="scope.row.status === 'paid'" type="success">已支付</el-tag>
              <el-tag v-else type="info">已取消</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
          <el-table-column label="支付时间" align="center" prop="payTime" width="160" />
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </el-tab-pane>

      <el-tab-pane label="提现记录" name="withdraw">
        <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
          <el-form-item label="用户ID" prop="userId">
            <el-input
              v-model="queryParams.userId"
              placeholder="请输入用户ID"
              clearable
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
              <el-option label="待审核" value="pending" />
              <el-option label="已通过" value="approved" />
              <el-option label="已拒绝" value="rejected" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>

        <el-table v-loading="loading" :data="withdrawList">
          <el-table-column label="提现ID" align="center" prop="withdrawId" width="80" />
          <el-table-column label="用户ID" align="center" prop="userId" width="80" />
          <el-table-column label="订单号" align="center" prop="orderNo" width="180" />
          <el-table-column label="金额" align="center" prop="amount" width="100">
            <template #default="scope">
              ¥{{ scope.row.amount }}
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="scope">
              <el-tag v-if="scope.row.status === 'pending'" type="warning">待审核</el-tag>
              <el-tag v-else-if="scope.row.status === 'approved'" type="success">已通过</el-tag>
              <el-tag v-else type="danger">已拒绝</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
          <el-table-column label="操作" align="center" width="180" fixed="right">
            <template #default="scope">
              <template v-if="scope.row.status === 'pending'">
                <el-button
                  link
                  type="success"
                  @click="handleApprove(scope.row)"
                >通过</el-button>
                <el-button
                  link
                  type="danger"
                  @click="handleReject(scope.row)"
                >拒绝</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="Wallet">
import { listWallet, listTransaction, listRecharge, listWithdraw, approveWithdraw, rejectWithdraw } from '@/api/community/wallet'

const loading = ref(true)
const showSearch = ref(true)
const activeTab = ref('wallet')
const walletList = ref([])
const transactionList = ref([])
const rechargeList = ref([])
const withdrawList = ref([])
const total = ref(0)
const stats = ref({
  totalUsers: 0,
  totalBalance: 0,
  totalRecharge: 0,
  totalWithdraw: 0
})

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  userId: null,
  username: null,
  type: null,
  status: null
})

function getList() {
  loading.value = true
  let apiCall
  switch (activeTab.value) {
    case 'wallet':
      apiCall = listWallet(queryParams.value)
      break
    case 'transaction':
      apiCall = listTransaction(queryParams.value)
      break
    case 'recharge':
      apiCall = listRecharge(queryParams.value)
      break
    case 'withdraw':
      apiCall = listWithdraw(queryParams.value)
      break
  }
  apiCall.then(response => {
    switch (activeTab.value) {
      case 'wallet':
        walletList.value = response.data || []
        break
      case 'transaction':
        transactionList.value = response.data || []
        break
      case 'recharge':
        rechargeList.value = response.data || []
        break
      case 'withdraw':
        withdrawList.value = response.data || []
        break
    }
    total.value = response.total || 0
    loading.value = false
  })
}

function handleTabClick() {
  queryParams.value.pageNum = 1
  getList()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    userId: null,
    username: null,
    type: null,
    status: null
  }
  handleQuery()
}

function handleApprove(row) {
  approveWithdraw(row.withdrawId).then(() => {
    ElMessage.success('操作成功')
    getList()
  })
}

function handleReject(row) {
  ElMessageBox.prompt('请输入拒绝原因', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S/,
    inputErrorMessage: '请输入拒绝原因'
  }).then(({ value }) => {
    rejectWithdraw(row.withdrawId, value).then(() => {
      ElMessage.success('操作成功')
      getList()
    })
  })
}

getList()
</script>

<style scoped>
.stat-item {
  text-align: center;
  padding: 20px;
}
.stat-label {
  color: #909399;
  font-size: 14px;
}
.stat-value {
  color: #303133;
  font-size: 24px;
  font-weight: bold;
  margin-top: 10px;
}
</style>
