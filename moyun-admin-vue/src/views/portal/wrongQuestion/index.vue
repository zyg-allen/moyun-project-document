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
          <el-option label="待复习" value="wrong" />
          <el-option label="复习中" value="reviewing" />
          <el-option label="已掌握" value="mastered" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签" prop="tag">
        <el-input
          v-model="queryParams.tag"
          placeholder="按题目标签筛选"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="题目标题" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="请输入题目标题"
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
      <el-table-column label="题目ID" align="center" prop="questionId" width="100" />
      <el-table-column label="题目标题" align="center" prop="questionTitle" min-width="200" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.questionTitle || ('题目 #' + scope.row.questionId) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="难度" align="center" prop="questionDifficulty" width="100">
        <template #default="scope">
          <el-tag :type="getDifficultyTagType(scope.row.questionDifficulty)">{{ getDifficultyText(scope.row.questionDifficulty) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="答错次数" align="center" prop="wrongCount" width="100" />
      <el-table-column label="最近答错" align="center" prop="lastWrongTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.lastWrongTime ? parseTime(scope.row.lastWrongTime) : '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="下次复习" align="center" prop="nextReviewTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.nextReviewTime ? parseTime(scope.row.nextReviewTime) : '—' }}</span>
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
    <el-dialog title="错题详情" v-model="viewOpen" width="640px" append-to-body>
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="错题ID">{{ currentRow.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentRow.userId }}</el-descriptions-item>
        <el-descriptions-item label="题目ID">{{ currentRow.questionId }}</el-descriptions-item>
        <el-descriptions-item label="最近答题ID">{{ currentRow.attemptId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="题目标题" :span="2">{{ currentRow.questionTitle || ('题目 #' + currentRow.questionId) }}</el-descriptions-item>
        <el-descriptions-item label="题目难度">
          <el-tag :type="getDifficultyTagType(currentRow.questionDifficulty)">{{ getDifficultyText(currentRow.questionDifficulty) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTagType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="题目标签" :span="2">{{ currentRow.questionTags || '—' }}</el-descriptions-item>
        <el-descriptions-item label="答错次数">{{ currentRow.wrongCount }}</el-descriptions-item>
        <el-descriptions-item label="题目分类ID">{{ currentRow.questionCategoryId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="最近答错时间">{{ currentRow.lastWrongTime ? parseTime(currentRow.lastWrongTime) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="下次复习时间">{{ currentRow.nextReviewTime ? parseTime(currentRow.nextReviewTime) : '—' }}</el-descriptions-item>
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

<script setup name="PortalWrongQuestion">
import { listWrongQuestion } from "@/api/portal/learn";

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
    tag: undefined,
    keyword: undefined
  }
});

const { queryParams } = toRefs(data);

/** 状态文本 */
function getStatusText(status) {
  const map = { wrong: '待复习', reviewing: '复习中', mastered: '已掌握' };
  return map[status] || status || '-';
}

/** 状态标签类型 */
function getStatusTagType(status) {
  const map = { wrong: 'danger', reviewing: 'warning', mastered: 'success' };
  return map[status] || 'info';
}

/** 难度文本 */
function getDifficultyText(difficulty) {
  const map = { easy: '简单', medium: '中等', hard: '困难' };
  return map[difficulty] || difficulty || '未分级';
}

/** 难度标签类型 */
function getDifficultyTagType(difficulty) {
  const map = { easy: 'success', medium: 'warning', hard: 'danger' };
  return map[difficulty] || 'info';
}

/** 查询列表 */
function getList() {
  loading.value = true;
  listWrongQuestion(queryParams.value).then(response => {
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
