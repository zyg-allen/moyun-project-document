<template>
  <div class="app-container">
    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>平台累计抽成</template>
          <div style="font-size: 24px; font-weight: 700; color: #f56c6c;">¥{{ summary.platformTotalYuan || '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>用户累计所得</template>
          <div style="font-size: 24px; font-weight: 700; color: #67c23a;">¥{{ summary.userTotalYuan || '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>流水总笔数</template>
          <div style="font-size: 24px; font-weight: 700;">{{ summary.totalEntries || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="视角" prop="accountRole">
        <el-select v-model="queryParams.accountRole" placeholder="账户视角" clearable style="width: 140px">
          <el-option label="平台抽成" value="PLATFORM" />
          <el-option label="用户所得" value="USER" />
        </el-select>
      </el-form-item>
      <el-form-item label="支付单号" prop="payNo">
        <el-input v-model="queryParams.payNo" placeholder="支付单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="方向" prop="direction">
        <el-select v-model="queryParams.direction" placeholder="方向" clearable style="width: 120px">
          <el-option label="收入" value="credit" />
          <el-option label="支出" value="debit" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="支付单号" align="center" prop="payNo" width="230" :show-overflow-tooltip="true" />
      <el-table-column label="业务类型" align="center" prop="bizType" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.bizType === 'tip'" type="warning" size="small">打赏</el-tag>
          <span v-else>{{ scope.row.bizType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="账户角色" align="center" prop="accountRole" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.accountRole === 'PLATFORM' ? 'danger' : 'success'" size="small">
            {{ scope.row.accountRole === 'PLATFORM' ? '平台' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="用户ID" align="center" prop="userId" width="90" />
      <el-table-column label="方向" align="center" prop="direction" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.direction === 'credit' ? 'success' : 'danger'" size="small">
            {{ scope.row.direction === 'credit' ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" align="center" prop="amountYuan" width="110">
        <template #default="scope">
          <span style="font-weight: 600;">¥{{ scope.row.amountYuan }}</span>
        </template>
      </el-table-column>
      <el-table-column label="变动后余额" align="center" prop="balanceAfterYuan" width="110">
        <template #default="scope">
          {{ scope.row.balanceAfterYuan != null ? '¥' + scope.row.balanceAfterYuan : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="摘要" align="center" prop="summary" :show-overflow-tooltip="true" />
      <el-table-column label="时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="CmsPayLedger">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listPayLedger, payLedgerSummary } from "@/api/cms/pay";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const summary = ref({});

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  accountRole: undefined,
  payNo: undefined,
  direction: undefined
});

function getList() {
  loading.value = true;
  listPayLedger(queryParams).then((response) => {
    dataList.value = response.data.records || [];
    total.value = response.data.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
}

function loadSummary() {
  payLedgerSummary().then((response) => {
    summary.value = response.data || {};
  });
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

onMounted(() => {
  getList();
  loadSummary();
});
</script>
