<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="目标类型" prop="targetType">
        <el-select v-model="queryParams.targetType" placeholder="目标类型" clearable style="width: 160px">
          <el-option label="文章打赏" value="article" />
          <el-option label="专栏打赏" value="column" />
          <el-option label="付费阅读" value="article_paid" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 140px">
          <el-option label="待支付" value="pending" />
          <el-option label="已支付" value="paid" />
          <el-option label="已退款" value="refunded" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间" prop="dateRange">
        <el-date-picker
          v-model="dateRange"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="datetimerange"
          range-separator="-"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="打赏者" align="center" prop="userNickname" width="140" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.userNickname || '-' }}</span>
          <div style="font-size: 12px; color: #909399;">ID: {{ scope.row.userId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="被打赏者" align="center" prop="authorNickname" width="140" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.authorNickname || '-' }}</span>
          <div style="font-size: 12px; color: #909399;">ID: {{ scope.row.authorId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="目标类型" align="center" prop="targetType" width="110">
        <template #default="scope">
          <el-tag :type="getTargetTypeTagType(scope.row.targetType)" size="small">
            {{ getTargetTypeText(scope.row.targetType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标ID" align="center" prop="targetId" width="90" />
      <el-table-column label="金额" align="center" prop="amount" width="100">
        <template #default="scope">
          <span style="color: #f56c6c; font-weight: 600;">¥{{ scope.row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)" size="small">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付方式" align="center" prop="payMethod" width="100" />
      <el-table-column label="创建时间" align="center" prop="createdTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
            v-hasPermi="['portal:tip:query']"
          >详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 详情对话框 -->
    <el-dialog title="打赏订单详情" v-model="viewOpen" width="640px" append-to-body>
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="订单ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="打赏者">{{ currentRow.userNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="打赏者ID">{{ currentRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="被打赏者">{{ currentRow.authorNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被打赏者ID">{{ currentRow.authorId }}</el-descriptions-item>
        <el-descriptions-item label="目标类型">
          <el-tag :type="getTargetTypeTagType(currentRow.targetType)">{{ getTargetTypeText(currentRow.targetType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标ID">{{ currentRow.targetId }}</el-descriptions-item>
        <el-descriptions-item label="金额">
          <span style="color: #f56c6c; font-weight: 600;">¥{{ currentRow.amount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ currentRow.payMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(currentRow.createdTime) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ currentRow.paidTime ? parseTime(currentRow.paidTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="留言" :span="2">
          <span v-if="currentRow.message">{{ currentRow.message }}</span>
          <span v-else style="color: #c0c4cc;">（无）</span>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsTip">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listTip } from "@/api/cms/tip";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const dateRange = ref([]);
const viewOpen = ref(false);
const currentRow = ref(null);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  targetType: undefined,
  status: undefined
});

function getTargetTypeText(targetType) {
  const map = { article: "文章打赏", column: "专栏打赏", article_paid: "付费阅读" };
  return map[targetType] || targetType || "-";
}

function getTargetTypeTagType(targetType) {
  const map = { article: "", column: "success", article_paid: "warning" };
  return map[targetType] || "info";
}

function getStatusText(status) {
  const map = { pending: "待支付", paid: "已支付", refunded: "已退款" };
  return map[status] || status || "-";
}

function getStatusTagType(status) {
  const map = { pending: "warning", paid: "success", refunded: "danger" };
  return map[status] || "info";
}

function getList() {
  loading.value = true;
  const params = { ...queryParams };
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0];
    params.endTime = dateRange.value[1];
  }
  listTip(params).then((response) => {
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
  queryParams.targetType = undefined;
  queryParams.status = undefined;
  dateRange.value = [];
  handleQuery();
}

function handleView(row) {
  currentRow.value = row;
  viewOpen.value = true;
}

onMounted(() => {
  getList();
});
</script>
