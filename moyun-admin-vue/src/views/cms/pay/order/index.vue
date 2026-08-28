<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="支付单号" prop="payNo">
        <el-input v-model="queryParams.payNo" placeholder="支付单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="业务类型" prop="bizType">
        <el-select v-model="queryParams.bizType" placeholder="业务类型" clearable style="width: 140px">
          <el-option label="打赏" value="tip" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 150px">
          <el-option label="待支付" value="CREATED" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已分账" value="SETTLED" />
          <el-option label="已关闭" value="CLOSED" />
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
      <el-table-column label="支付单号" align="center" prop="payNo" width="250" :show-overflow-tooltip="true" />
      <el-table-column label="业务类型" align="center" prop="bizType" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.bizType === 'tip'" type="warning">打赏</el-tag>
          <span v-else>{{ scope.row.bizType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="业务单号" align="center" prop="bizNo" width="90" />
      <el-table-column label="渠道" align="center" prop="channel" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.channel === 'wechat'" type="success" size="small">微信</el-tag>
          <span v-else>{{ scope.row.channel }}</span>
        </template>
      </el-table-column>
      <el-table-column label="金额" align="center" prop="amountYuan" width="110">
        <template #default="scope">
          <span style="color: #f56c6c; font-weight: 600;">¥{{ scope.row.amountYuan }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="三方单号" align="center" prop="channelOrderNo" width="200" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.channelOrderNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="right" width="180" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleView(scope.row)" v-hasPermi="['cms:payOrder:query']">详情</el-button>
          <el-button
            v-if="scope.row.status === 'CREATED'"
            link
            type="danger"
            icon="CircleClose"
            @click="handleClose(scope.row)"
            v-hasPermi="['cms:payOrder:close']"
          >关单</el-button>
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

    <!-- 详情抽屉（含分账明细） -->
    <el-drawer v-model="viewOpen" title="支付订单详情" size="560px">
      <el-descriptions v-if="currentRow" :column="1" border>
        <el-descriptions-item label="支付单号">{{ currentRow.payNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentRow.status)">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="业务类型 / 单号">{{ currentRow.bizType }} / {{ currentRow.bizNo }}</el-descriptions-item>
        <el-descriptions-item label="支付渠道">{{ currentRow.channel === 'wechat' ? '微信支付' : currentRow.channel }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ currentRow.amountYuan }}</el-descriptions-item>
        <el-descriptions-item label="商品描述">{{ currentRow.subject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="三方交易单号">{{ currentRow.channelOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="三方交易状态">{{ currentRow.tradeState || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付成功时间">{{ currentRow.paySuccessTime ? parseTime(currentRow.paySuccessTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="分账完成时间">{{ currentRow.settleTime ? parseTime(currentRow.settleTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ currentRow.expireTime ? parseTime(currentRow.expireTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="关单原因">{{ currentRow.closeReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关单时间">{{ currentRow.closedTime ? parseTime(currentRow.closedTime) : '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 分账明细 -->
      <div v-if="ledgerEntries.length > 0" style="margin-top: 20px;">
        <el-divider content-position="left">分账明细（复式记账）</el-divider>
        <el-table :data="ledgerEntries" size="small">
          <el-table-column label="账户角色" align="center" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.accountRole === 'PLATFORM' ? 'danger' : 'success'" size="small">
                {{ scope.row.accountRole === 'PLATFORM' ? '平台' : '用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="方向" align="center" prop="direction" width="80" />
          <el-table-column label="金额" align="center" prop="amountYuan" width="110">
            <template #default="scope">¥{{ scope.row.amountYuan }}</template>
          </el-table-column>
          <el-table-column label="摘要" align="center" prop="summary" :show-overflow-tooltip="true" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="CmsPayOrder">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listPayOrder, getPayOrder, closePayOrder, getPayLedgerDetail } from "@/api/cms/pay";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const viewOpen = ref(false);
const currentRow = ref(null);
const ledgerEntries = ref([]);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  payNo: undefined,
  bizType: undefined,
  status: undefined
});

function statusLabel(status) {
  const map = { CREATED: '待支付', PAID: '已支付', SETTLED: '已分账', CLOSED: '已关闭' };
  return map[status] || status;
}

function statusTagType(status) {
  const map = { CREATED: 'info', PAID: 'primary', SETTLED: 'success', CLOSED: 'danger' };
  return map[status] || 'info';
}

function getList() {
  loading.value = true;
  listPayOrder(queryParams).then((response) => {
    dataList.value = response.data.records || [];
    total.value = response.data.total || 0;
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

function handleView(row) {
  currentRow.value = row;
  ledgerEntries.value = [];
  viewOpen.value = true;
  // 详情 + 分账明细
  getPayOrder(row.payNo).then((response) => {
    if (response.data && response.data.order) currentRow.value = response.data.order;
  });
  getPayLedgerDetail(row.payNo).then((response) => {
    ledgerEntries.value = response.data || [];
  });
}

function handleClose(row) {
  proxy.$modal.confirm('确认关闭支付单 "' + row.payNo + '" 吗？关闭后不可恢复。').then(() => {
    return closePayOrder(row.payNo);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("关单成功");
  }).catch(() => {});
}

onMounted(() => {
  getList();
});
</script>
