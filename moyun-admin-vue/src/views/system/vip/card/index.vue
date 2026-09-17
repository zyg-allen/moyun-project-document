<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="所属端" prop="platformCode">
            <el-select v-model="queryParams.platformCode" placeholder="所属端" clearable style="width: 200px">
               <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
            </el-select>
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

      <el-table v-loading="loading" :data="cardList">
         <el-table-column label="主键" align="center" prop="id" width="80" />
         <el-table-column label="用户ID" align="center" prop="userId" width="100" />
         <el-table-column label="所属端" align="center" prop="platformCode" width="90" />
         <el-table-column label="等级编码" align="center" prop="tierCode" width="110" />
         <el-table-column label="来源" align="center" prop="source" width="100" />
         <el-table-column label="订单号" align="center" prop="orderId" :show-overflow-tooltip="true" />
         <el-table-column label="开始时间" align="center" prop="startTime" width="160">
            <template #default="scope">
               <span>{{ parseTime(scope.row.startTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="到期时间" align="center" prop="expireTime" width="160">
            <template #default="scope">
               <span>{{ scope.row.expireTime ? parseTime(scope.row.expireTime) : '永久' }}</span>
            </template>
         </el-table-column>
         <el-table-column label="状态" align="center" prop="status" width="80">
            <template #default="scope">
               <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '生效' : '作废' }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="right" width="120" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button
                  v-if="scope.row.status === 1"
                  link
                  type="primary"
                  icon="Delete"
                  @click="handleDelete(scope.row)"
                  v-hasPermi="['system:vip:card:remove']"
               >作废</el-button>
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

<script setup name="VipCard">
import { listCard, delCard } from "@/api/system/vip";
import { platformOptionselect } from "@/api/system/platform";

const { proxy } = getCurrentInstance();

const cardList = ref([]);
const platformOptions = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const total = ref(0);

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    platformCode: undefined,
    userId: undefined
  }
});

const { queryParams } = toRefs(data);

/** 查询会员卡列表 */
function getList() {
  loading.value = true;
  listCard(queryParams.value).then(response => {
    cardList.value = response.data.records;
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
/** 作废按钮操作 */
function handleDelete(row) {
  proxy.$modal.confirm('是否确认作废会员卡编号为"' + row.id + '"的数据项？').then(function () {
    return delCard(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("作废成功");
  }).catch(() => {});
}

getPlatformOptions();
getList();
</script>
