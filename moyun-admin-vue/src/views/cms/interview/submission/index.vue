<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="题目ID" prop="questionId">
        <el-input
          v-model="queryParams.questionId"
          placeholder="请输入题目ID"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="精选状态" prop="isFeatured">
        <el-select v-model="queryParams.isFeatured" placeholder="请选择" clearable style="width: 200px">
          <el-option label="已精选" :value="true" />
          <el-option label="未精选" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 提交笔记表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" prop="id" width="80" align="center" />
      <el-table-column label="题目ID" prop="questionId" width="100" align="center" />
      <el-table-column label="用户ID" prop="userId" width="100" align="center" />
      <el-table-column label="答案类型" prop="answerType" width="100" align="center">
        <template #default="scope">
          <el-tag>{{ scope.row.answerType || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否通过" prop="isSuccess" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isSuccess ? 'success' : 'danger'">
            {{ scope.row.isSuccess ? '通过' : '未通过' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="笔记内容" prop="note" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="精选状态" prop="isFeatured" width="100" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isFeatured ? 'warning' : 'info'">
            {{ scope.row.isFeatured ? '已精选' : '未精选' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="精选时间" prop="featuredTime" width="160" align="center">
        <template #default="scope">
          <span>{{ scope.row.featuredTime ? parseTime(scope.row.featuredTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="createTime" width="160" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            v-if="!scope.row.isFeatured"
            link
            type="warning"
            icon="Star"
            @click="handleFeature(scope.row)"
            v-hasPermi="['cms:interview:edit']"
          >采纳精选</el-button>
          <el-button
            v-else
            link
            type="info"
            icon="CircleClose"
            @click="handleUnfeature(scope.row)"
            v-hasPermi="['cms:interview:edit']"
          >取消精选</el-button>
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
  </div>
</template>

<script setup name="CmsInterviewSubmission">
import { listInterviewSubmission, featureSubmission, unfeatureSubmission } from "@/api/cms/interview";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    questionId: undefined,
    isFeatured: undefined
  }
});

const { queryParams } = toRefs(data);

/** 查询列表 */
function getList() {
  loading.value = true;
  listInterviewSubmission(queryParams.value).then(response => {
    dataList.value = response.data.records || response.rows || [];
    total.value = response.data.total || response.total || 0;
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

/** 采纳精选 */
function handleFeature(row) {
  proxy.$modal.confirm('是否确认将此笔记采纳为精选笔记？采纳后将触发 note_adopted 成长事件（+50成长值）。').then(function() {
    return featureSubmission(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("采纳成功");
  }).catch(() => {});
}

/** 取消精选 */
function handleUnfeature(row) {
  proxy.$modal.confirm('是否确认取消此笔记的精选状态？').then(function() {
    return unfeatureSubmission(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("取消成功");
  }).catch(() => {});
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
