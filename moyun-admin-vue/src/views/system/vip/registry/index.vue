<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="所属端" prop="platformCode">
            <el-select v-model="queryParams.platformCode" placeholder="所属端" clearable style="width: 180px">
               <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
            </el-select>
         </el-form-item>
         <el-form-item label="权益编码" prop="benefitCode">
            <el-input
               v-model="queryParams.benefitCode"
               placeholder="请输入权益编码"
               clearable
               style="width: 180px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="接口路径" prop="apiPattern">
            <el-input
               v-model="queryParams.apiPattern"
               placeholder="请输入接口路径"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="enabled">
            <el-select v-model="queryParams.enabled" placeholder="状态" clearable style="width: 160px">
               <el-option label="启用" :value="1" />
               <el-option label="停用" :value="0" />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:vip:registry:edit']">修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="warning" plain icon="Refresh" @click="handleScan" v-hasPermi="['system:vip:registry:scan']">重新扫描</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="registryList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="主键" align="center" prop="id" width="70" />
         <el-table-column label="所属端" align="center" prop="platformCode" width="90" />
         <el-table-column label="权益编码" align="center" prop="benefitCode" width="120" :show-overflow-tooltip="true" />
         <el-table-column label="接口路径" align="center" prop="apiPattern" :show-overflow-tooltip="true" />
         <el-table-column label="请求方式" align="center" prop="httpMethod" width="80" />
         <el-table-column label="类名" align="center" prop="className" :show-overflow-tooltip="true" />
         <el-table-column label="方法名" align="center" prop="methodName" width="140" :show-overflow-tooltip="true" />
         <el-table-column label="接口描述" align="center" prop="apiDesc" :show-overflow-tooltip="true" />
         <el-table-column label="状态" align="center" prop="enabled" width="80">
            <template #default="scope">
               <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">{{ scope.row.enabled === 1 ? '启用' : '停用' }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="扫描时间" align="center" prop="scanTime" width="160">
            <template #default="scope">
               <span>{{ parseTime(scope.row.scanTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="right" width="120" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:vip:registry:edit']">修改</el-button>
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

      <!-- 修改接口注册对话框（仅 apiDesc/enabled 可改） -->
      <el-dialog title="修改接口注册" v-model="open" width="560px" append-to-body>
         <el-form ref="registryRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="接口路径">
               <el-input v-model="form.apiPattern" disabled />
            </el-form-item>
            <el-form-item label="接口描述" prop="apiDesc">
               <el-input v-model="form.apiDesc" type="textarea" placeholder="请输入接口描述" />
            </el-form-item>
            <el-form-item label="状态" prop="enabled">
               <el-radio-group v-model="form.enabled">
                  <el-radio :label="1">启用</el-radio>
                  <el-radio :label="0">停用</el-radio>
               </el-radio-group>
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

<script setup name="VipRegistry">
import { listRegistry, updateRegistry, scanRegistry } from "@/api/system/vip";
import { platformOptionselect } from "@/api/system/platform";

const { proxy } = getCurrentInstance();

const registryList = ref([]);
const platformOptions = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const total = ref(0);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    platformCode: undefined,
    benefitCode: undefined,
    apiPattern: undefined,
    enabled: undefined
  },
  rules: {}
});

const { queryParams, form, rules } = toRefs(data);

/** 查询接口注册列表 */
function getList() {
  loading.value = true;
  listRegistry(queryParams.value).then(response => {
    registryList.value = response.data.records;
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
/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}
/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    apiPattern: undefined,
    apiDesc: undefined,
    enabled: 1
  };
  proxy.resetForm("registryRef");
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
/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const r = row.id ? row : registryList.value.find(item => item.id === ids.value[0]);
  form.value = { id: r.id, apiPattern: r.apiPattern, apiDesc: r.apiDesc, enabled: r.enabled };
  open.value = true;
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["registryRef"].validate(valid => {
    if (valid) {
      updateRegistry(form.value).then(() => {
        proxy.$modal.msgSuccess("修改成功");
        open.value = false;
        getList();
      });
    }
  });
}
/** 重新扫描接口 */
function handleScan() {
  proxy.$modal.confirm("是否确认重新扫描权益接口？").then(function () {
    return scanRegistry();
  }).then(response => {
    proxy.$modal.msgSuccess("扫描完成，共识别 " + response.data.count + " 条接口");
    getList();
  }).catch(() => {});
}

getPlatformOptions();
getList();
</script>
