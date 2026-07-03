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
      <el-form-item label="模块" prop="module">
        <el-select v-model="queryParams.module" placeholder="请选择模块" clearable style="width: 200px">
          <el-option label="文章" value="article" />
          <el-option label="读书" value="reading" />
          <el-option label="面试" value="interview" />
          <el-option label="通用" value="all" />
        </el-select>
      </el-form-item>
      <el-form-item label="行为" prop="action">
        <el-input
          v-model="queryParams.action"
          placeholder="请输入行为编码"
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
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" align="center" prop="id" width="80" v-if="columns[0].visible" />
      <el-table-column label="用户ID" align="center" prop="userId" width="100" v-if="columns[1].visible" />
      <el-table-column label="目标用户ID" align="center" prop="targetUserId" width="110" v-if="columns[2].visible">
        <template #default="scope">
          <span>{{ scope.row.targetUserId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="模块" align="center" prop="module" width="90" v-if="columns[3].visible">
        <template #default="scope">
          <el-tag :type="getModuleTagType(scope.row.module)">{{ getModuleText(scope.row.module) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="行为" align="center" prop="action" width="160" :show-overflow-tooltip="true" v-if="columns[4].visible" />
      <el-table-column label="实体类型" align="center" prop="entityType" width="100" v-if="columns[5].visible">
        <template #default="scope">
          <span>{{ scope.row.entityType || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实体ID" align="center" prop="entityId" width="100" v-if="columns[6].visible">
        <template #default="scope">
          <span>{{ scope.row.entityId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成长值变化" align="center" prop="growthDelta" width="110" v-if="columns[7].visible">
        <template #default="scope">
          <el-tag :type="scope.row.growthDelta >= 0 ? 'success' : 'danger'">
            {{ scope.row.growthDelta >= 0 ? '+' + scope.row.growthDelta : scope.row.growthDelta }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" min-width="180" :show-overflow-tooltip="true" v-if="columns[8].visible" />
      <el-table-column label="时间" align="center" prop="createTime" width="160" v-if="columns[9].visible">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
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
  </div>
</template>

<script setup name="CmsGrowthLog">
import { listGrowthLog } from "@/api/cms/growth";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);

const columns = ref([
  { key: 0, label: `ID`, visible: true },
  { key: 1, label: `用户ID`, visible: true },
  { key: 2, label: `目标用户ID`, visible: true },
  { key: 3, label: `模块`, visible: true },
  { key: 4, label: `行为`, visible: true },
  { key: 5, label: `实体类型`, visible: true },
  { key: 6, label: `实体ID`, visible: true },
  { key: 7, label: `成长值变化`, visible: true },
  { key: 8, label: `描述`, visible: true },
  { key: 9, label: `时间`, visible: true }
]);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined,
    module: undefined,
    action: undefined
  }
});

const { queryParams } = toRefs(data);

/** 模块文本 */
function getModuleText(module) {
  const map = { article: '文章', reading: '读书', interview: '面试', all: '通用' };
  return map[module] || module || '-';
}

/** 模块标签类型 */
function getModuleTagType(module) {
  const map = { article: '', reading: 'success', interview: 'warning', all: 'info' };
  return map[module] || '';
}

/** 查询列表 */
function getList() {
  loading.value = true;
  listGrowthLog(queryParams.value).then(response => {
    dataList.value = (response.data && response.data.records) || response.rows || [];
    total.value = (response.data && response.data.total) || response.total || 0;
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

onMounted(() => {
  getList();
});
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
