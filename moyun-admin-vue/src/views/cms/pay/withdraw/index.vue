<template>
  <div class="app-container">
    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="mb8">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>审核中金额（元）</template>
          <div class="stat-num" style="color: #e6a23c;">¥{{ fmt(summary.auditingAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>已打款金额（元）</template>
          <div class="stat-num" style="color: #67c23a;">¥{{ fmt(summary.paidAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>已驳回金额（元）</template>
          <div class="stat-num" style="color: #f56c6c;">¥{{ fmt(summary.rejectedAmount) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="70px">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="审核中" value="auditing" />
          <el-option label="已打款" value="paid" />
          <el-option label="已驳回" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item label="归属端" prop="platformCode">
        <el-select v-model="queryParams.platformCode" placeholder="归属端" clearable style="width: 130px">
          <el-option label="门户端" value="portal" />
          <el-option label="记账端" value="ledger" />
        </el-select>
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input v-model="queryParams.userId" placeholder="用户ID" clearable style="width: 140px" @keyup.enter="handleQuery" />
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
      <el-table-column label="提现单号" align="center" prop="withdrawNo" width="230" :show-overflow-tooltip="true" />
      <el-table-column label="归属端" align="center" prop="platformCode" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.platformCode === 'portal'" type="primary" size="small">门户</el-tag>
          <el-tag v-else-if="scope.row.platformCode === 'ledger'" type="success" size="small">记账</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="用户" align="center" width="150">
        <template #default="scope">
          <span>{{ scope.row.nickname || ('用户' + scope.row.userId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="金额（元）" align="center" width="110">
        <template #default="scope">
          <span style="font-weight: 600; color: #409eff;">¥{{ fmt(scope.row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="手续费（元）" align="center" width="100">
        <template #default="scope">¥{{ fmt(scope.row.fee) }}</template>
      </el-table-column>
      <el-table-column label="打款银行卡" align="center" prop="bankCardDesc" width="220" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.bankCardDesc || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" size="small">{{ statusName(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="驳回原因" align="center" prop="rejectReason" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.rejectReason || '—' }}</template>
      </el-table-column>
      <el-table-column label="申请时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="审核/打款时间" align="center" width="170">
        <template #default="scope">{{ parseTime(scope.row.paidTime || scope.row.auditTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template #default="scope">
          <template v-if="scope.row.status === 'auditing'">
            <el-button link type="primary" icon="Check" @click="handlePass(scope.row)">通过打款</el-button>
            <el-button link type="danger" icon="Close" @click="handleReject(scope.row)">驳回</el-button>
          </template>
          <span v-else>—</span>
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

    <!-- 驳回原因对话框 -->
    <el-dialog title="驳回原因" v-model="rejectVisible" width="420px" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因（将展示给用户）" maxlength="200" show-word-limit />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-alert type="info" :closable="false" show-icon class="mt8">
      <p>提现闭环（v11.79）：通过 = 原子扣减用户余额 + 记 debit 流水 + 置已打款（真实出金走公账商户号转账，通道预留）；驳回 = 余额不动。余额不足的审核单会自动驳回。</p>
    </el-alert>
  </div>
</template>

<script setup name="CmsPayWithdraw">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listWithdraw, passWithdraw, rejectWithdraw } from "@/api/cms/pay";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const summary = ref({});

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  platformCode: undefined,
  userId: undefined
});

const rejectVisible = ref(false);
const rejectReason = ref("");
const rejectSubmitting = ref(false);
const rejectTarget = ref(null);

function fmt(v) {
  const n = Number(v || 0);
  return isNaN(n) ? "0.00" : n.toFixed(2);
}

function statusName(status) {
  const map = { auditing: "审核中", paid: "已打款", rejected: "已驳回" };
  return map[status] || status || "-";
}

function statusType(status) {
  const map = { auditing: "warning", paid: "success", rejected: "danger" };
  return map[status] || "info";
}

function getList() {
  loading.value = true;
  listWithdraw(queryParams).then((response) => {
    const data = response.data || {};
    const page = data.page || {};
    dataList.value = page.records || [];
    total.value = page.total || 0;
    summary.value = data.summary || {};
    loading.value = false;
  }).catch(() => {
    loading.value = false;
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

function handlePass(row) {
  proxy.$modal.confirm('确认通过提现单 ' + row.withdrawNo + '（¥' + fmt(row.amount) + '）？通过后将原子扣减余额并打款。').then(() => {
    return passWithdraw(row.id);
  }).then(() => {
    proxy.$modal.msgSuccess("已通过并完成打款记账");
    getList();
  }).catch(() => {});
}

function handleReject(row) {
  rejectTarget.value = row;
  rejectReason.value = "";
  rejectVisible.value = true;
}

function submitReject() {
  rejectSubmitting.value = true;
  rejectWithdraw(rejectTarget.value.id, rejectReason.value).then(() => {
    proxy.$modal.msgSuccess("已驳回");
    rejectVisible.value = false;
    rejectSubmitting.value = false;
    getList();
  }).catch(() => {
    rejectSubmitting.value = false;
  });
}

onMounted(getList);
</script>

<style scoped>
.stat-num {
  font-size: 20px;
  font-weight: 600;
  text-align: center;
}
</style>
