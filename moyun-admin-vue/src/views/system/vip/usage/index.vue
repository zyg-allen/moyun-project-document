<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="所属端" prop="platformCode">
            <el-select v-model="queryParams.platformCode" placeholder="所属端" clearable style="width: 200px">
               <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
            </el-select>
         </el-form-item>
         <el-form-item label="权益编码" prop="benefitCode">
            <el-input
               v-model="queryParams.benefitCode"
               placeholder="请输入权益编码"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
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

      <el-row :gutter="10" class="mb8">
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="usageList">
         <el-table-column label="主键" align="center" prop="id" width="80" />
         <el-table-column label="用户ID" align="center" prop="userId" width="120" />
         <el-table-column label="所属端" align="center" prop="platformCode" width="100" />
         <el-table-column label="权益编码" align="center" prop="benefitCode" />
         <el-table-column label="使用日期" align="center" prop="usageDate" width="120">
            <template #default="scope">
               <span>{{ parseTime(scope.row.usageDate, '{y}-{m}-{d}') }}</span>
            </template>
         </el-table-column>
         <el-table-column label="使用次数" align="center" prop="usageCount" width="100" />
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

<script setup name="VipUsage">
import { listUsage } from "@/api/system/vip";
import { platformOptionselect } from "@/api/system/platform";

const { proxy } = getCurrentInstance();

const usageList = ref([]);
const platformOptions = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    platformCode: undefined,
    benefitCode: undefined,
    userId: undefined
  }
});

const { queryParams } = toRefs(data);

/** 查询权益使用统计列表 */
function getList() {
  loading.value = true;
  listUsage(queryParams.value).then(response => {
    usageList.value = response.data.records;
    total.value = response.data.total;
    loading.value = false;
  });
}
/** 加载平台端下拉 */
function getPlatformOptions() {
  platformOptionselect().then(response => {
    platformOptions.value = response.data;
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

getPlatformOptions();
getList();
</script>
