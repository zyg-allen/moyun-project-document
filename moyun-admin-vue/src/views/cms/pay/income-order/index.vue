<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="70px">
      <el-form-item label="平台" prop="platform">
        <el-select v-model="queryParams.platform" placeholder="全部平台" clearable style="width: 140px">
          <el-option label="记账App" value="ledger_app" />
          <el-option label="墨韵门户" value="portal" />
        </el-select>
      </el-form-item>
      <el-form-item label="渠道" prop="channelCode">
        <el-select v-model="queryParams.channelCode" placeholder="全部渠道" clearable style="width: 150px">
          <el-option label="App记账打赏" value="app_tip" />
          <el-option label="门户文章打赏" value="portal_tip" />
          <el-option label="付费阅读" value="paid_reading" />
          <el-option label="VIP订阅" value="vip" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="已支付" value="paid" />
          <el-option label="待支付" value="pending" />
          <el-option label="已退款" value="refunded" />
          <el-option label="已关闭" value="closed" />
        </el-select>
      </el-form-item>
      <el-form-item label="下单时间" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
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
      <el-table-column label="平台" align="center" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.platform === 'ledger_app' ? 'primary' : 'success'" size="small">
            {{ scope.row.platform === 'ledger_app' ? '记账App' : '墨韵门户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收入渠道" align="center" width="120">
            <template #default="scope">
              <span>{{ channelName(scope.row.channel_code) }}</span>
            </template>
          </el-table-column>
      <el-table-column label="订单号" align="center" prop="order_id" width="80" />
      <el-table-column label="用户" align="center" prop="nickname" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="打赏对象" align="center" width="110">
        <template #default="scope">
          <span>{{ targetName(scope.row.platform, scope.row.target_type) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="金额（元）" align="center" width="110">
        <template #default="scope">
          <span style="font-weight: 600; color: #409eff;">¥{{ fmt(scope.row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template #default="scope">
          <el-tag :type="statusType(scope.row.status)" size="small">{{ statusName(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付渠道" align="center" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.pay_channel === 'points'" type="warning" size="small">积分</el-tag>
          <span v-else>{{ scope.row.pay_channel === 'alipay' ? '支付宝' : '微信支付' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注/留言" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="支付时间" align="center" prop="pay_time" width="160">
        <template #default="scope">{{ parseTime(scope.row.pay_time) }}</template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-alert type="info" :closable="false" show-icon class="mt8">
      <p>统一业务订单视图（v12.0）：合并 ledger_tip_order + portal_tip_order + VIP 订阅（pay_order biz_type='vip'，统一会员后收敛到公共支付通道），状态/渠道枚举全局统一（pending/paid/refunded/closed；wechat/alipay/points）；其余 pay_order 通道单据见"支付订单"页，不重复计入。</p>
    </el-alert>
  </div>
</template>

<script setup name="CmsPayIncomeOrder">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listIncomeOrder } from "@/api/cms/pay";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const dateRange = ref([]);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  platform: undefined,
  channelCode: undefined,
  status: undefined
});

function fmt(v) {
  const n = Number(v || 0);
  return isNaN(n) ? "0.00" : n.toFixed(2);
}

function channelName(code) {
  const map = { app_tip: "App记账打赏", portal_tip: "门户文章打赏", paid_reading: "付费阅读", vip: "VIP订阅" };
  return map[code] || code || "-";
}

function targetName(platform, targetType) {
  if (platform === "ledger_app") {
    return targetType === "platform" ? "平台" : "开发者";
  }
  const map = { article: "文章", column: "专栏", article_paid: "付费文章" };
  return map[targetType] || targetType || "-";
}

function statusName(status) {
  const map = { paid: "已支付", pending: "待支付", refunded: "已退款", closed: "已关闭" };
  return map[status] || status || "-";
}

function statusType(status) {
  const map = { paid: "success", pending: "warning", refunded: "info", closed: "danger" };
  return map[status] || "info";
}

function getList() {
  loading.value = true;
  const params = { ...queryParams };
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0] + " 00:00:00";
    params.endTime = dateRange.value[1] + " 23:59:59";
  }
  listIncomeOrder(params).then((response) => {
    const data = response.data || {};
    dataList.value = data.records || [];
    total.value = data.total || 0;
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
  dateRange.value = [];
  proxy.resetForm("queryRef");
  handleQuery();
}

onMounted(getList);
</script>
