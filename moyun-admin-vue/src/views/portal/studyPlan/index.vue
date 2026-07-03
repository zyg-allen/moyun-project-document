<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="用户ID" prop="userId">
        <el-input
          v-model="queryParams.userId"
          placeholder="请输入用户ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="进行中" value="active" />
          <el-option label="已完成" value="completed" />
          <el-option label="已放弃" value="abandoned" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划类型" prop="planType">
        <el-select v-model="queryParams.planType" placeholder="请选择类型" clearable style="width: 200px">
          <el-option label="每日刷题" value="daily_question" />
          <el-option label="每周阅读" value="weekly_reading" />
          <el-option label="自定义" value="custom" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入计划标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
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

    <!-- 数据表格（只读） -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="用户ID" align="center" prop="userId" width="100" />
      <el-table-column label="计划标题" align="center" prop="title" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="计划类型" align="center" prop="planType" width="120">
        <template #default="scope">
          <el-tag :type="getPlanTypeTagType(scope.row.planType)">{{ getPlanTypeText(scope.row.planType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标数量" align="center" prop="targetCount" width="100">
        <template #default="scope">
          <span>{{ scope.row.targetCount != null ? scope.row.targetCount : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="目标分类" align="center" prop="targetCategory" width="120" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.targetCategory || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开始日期" align="center" prop="startDate" width="120">
        <template #default="scope">
          <span>{{ scope.row.startDate || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束日期" align="center" prop="endDate" width="120">
        <template #default="scope">
          <span>{{ scope.row.endDate || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
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

    <!-- 详情对话框（只读） -->
    <el-dialog title="学习计划详情" v-model="viewOpen" width="640px" append-to-body>
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="计划ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="计划标题" :span="2">{{ currentRow.title }}</el-descriptions-item>
        <el-descriptions-item label="计划类型">
          <el-tag :type="getPlanTypeTagType(currentRow.planType)">{{ getPlanTypeText(currentRow.planType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标数量">{{ currentRow.targetCount != null ? currentRow.targetCount : '—' }}</el-descriptions-item>
        <el-descriptions-item label="目标分类">{{ currentRow.targetCategory || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ currentRow.startDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ currentRow.endDate || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ parseTime(currentRow.createdTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PortalStudyPlan">
import { listStudyPlan } from "@/api/portal/learn";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const viewOpen = ref(false);
const currentRow = ref(null);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    status: undefined,
    planType: undefined,
    title: undefined
  }
});

const { queryParams } = toRefs(data);

/** 状态文本 */
function getStatusText(status) {
  const map = { active: '进行中', completed: '已完成', abandoned: '已放弃' };
  return map[status] || status || '-';
}

/** 状态标签类型 */
function getStatusTagType(status) {
  const map = { active: 'success', completed: '', abandoned: 'info' };
  return map[status] || 'info';
}

/** 计划类型文本 */
function getPlanTypeText(planType) {
  const map = { daily_question: '每日刷题', weekly_reading: '每周阅读', custom: '自定义' };
  return map[planType] || planType || '-';
}

/** 计划类型标签类型 */
function getPlanTypeTagType(planType) {
  const map = { daily_question: '', weekly_reading: 'success', custom: 'warning' };
  return map[planType] || '';
}

/** 查询列表 */
function getList() {
  loading.value = true;
  listStudyPlan(queryParams.value).then(response => {
    dataList.value = (response.data && response.data.records) || response.rows || [];
    total.value = (response.data && response.data.total) || response.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 详情 */
function handleView(row) {
  currentRow.value = row;
  viewOpen.value = true;
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
