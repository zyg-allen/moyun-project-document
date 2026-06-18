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
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleGrant"
          v-hasPermi="['cms:growth:add']"
        >授予徽章</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="ID" align="center" prop="id" width="80" v-if="columns[0].visible" />
      <el-table-column label="用户ID" align="center" prop="userId" width="120" v-if="columns[1].visible" />
      <el-table-column label="成就ID" align="center" prop="achievementId" width="120" v-if="columns[2].visible" />
      <el-table-column label="获得时间" align="center" prop="createTime" width="180" v-if="columns[3].visible">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="160">
        <template #default="scope">
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleRevoke(scope.row)"
            v-hasPermi="['cms:growth:remove']"
          >撤销徽章</el-button>
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

    <!-- 授予徽章对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="form.userId" placeholder="请输入用户ID" />
        </el-form-item>
        <el-form-item label="成就ID" prop="achievementId">
          <el-input v-model="form.achievementId" placeholder="请输入成就ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsGrowthBadge">
import { listUserBadge, grantUserBadge, revokeUserBadge } from "@/api/cms/growth";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);
const title = ref("");

const columns = ref([
  { key: 0, label: `ID`, visible: true },
  { key: 1, label: `用户ID`, visible: true },
  { key: 2, label: `成就ID`, visible: true },
  { key: 3, label: `获得时间`, visible: true }
]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    userId: undefined
  },
  rules: {
    userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }],
    achievementId: [{ required: true, message: "成就ID不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询列表 */
function getList() {
  loading.value = true;
  listUserBadge(queryParams.value).then(response => {
    dataList.value = (response.data && response.data.records) || response.rows || [];
    total.value = (response.data && response.data.total) || response.total || 0;
    loading.value = false;
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    userId: undefined,
    achievementId: undefined
  };
  proxy.resetForm("formRef");
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

/** 授予徽章按钮操作 */
function handleGrant() {
  reset();
  open.value = true;
  title.value = "授予徽章";
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      grantUserBadge(form.value).then(response => {
        proxy.$modal.msgSuccess("授予成功");
        open.value = false;
        getList();
      });
    }
  });
}

/** 撤销徽章按钮操作 */
function handleRevoke(row) {
  proxy.$modal.confirm('是否确认撤销用户ID为"' + row.userId + '"的成就ID为"' + row.achievementId + '"的徽章？').then(function () {
    return revokeUserBadge({ userId: row.userId, achievementId: row.achievementId });
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("撤销成功");
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
