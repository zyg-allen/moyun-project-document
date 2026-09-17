<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="所属端" prop="platformCode">
            <el-select v-model="queryParams.platformCode" placeholder="所属端" clearable style="width: 200px">
               <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
            </el-select>
         </el-form-item>
         <el-form-item label="等级名称" prop="tierName">
            <el-input
               v-model="queryParams.tierName"
               placeholder="请输入等级名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 200px">
               <el-option label="上架" :value="1" />
               <el-option label="下架" :value="0" />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:vip:tier:add']">新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:vip:tier:edit']">修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:vip:tier:remove']">删除</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="tierList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="主键" align="center" prop="id" width="80" />
         <el-table-column label="所属端" align="center" prop="platformCode" />
         <el-table-column label="等级编码" align="center" prop="tierCode" />
         <el-table-column label="等级名称" align="center" prop="tierName" />
         <el-table-column label="售价(元)" align="center" prop="price" width="100">
            <template #default="scope">
               <span>¥{{ Number(scope.row.price).toFixed(2) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="原价(元)" align="center" prop="originalPrice" width="100">
            <template #default="scope">
               <span v-if="scope.row.originalPrice != null">¥{{ Number(scope.row.originalPrice).toFixed(2) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="有效天数" align="center" prop="durationDays" width="90">
            <template #default="scope">
               <span>{{ scope.row.durationDays === -1 ? '永久' : scope.row.durationDays + '天' }}</span>
            </template>
         </el-table-column>
         <el-table-column label="热门" align="center" prop="popular" width="80">
            <template #default="scope">
               <el-tag v-if="scope.row.popular === 1" type="danger">热门</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
         <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
         <el-table-column label="状态" align="center" prop="status" width="80">
            <template #default="scope">
               <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '上架' : '下架' }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="right" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:vip:tier:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:vip:tier:remove']">删除</el-button>
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

      <!-- 添加或修改VIP等级对话框 -->
      <el-dialog :title="title" v-model="open" width="620px" append-to-body>
         <el-form ref="tierRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="所属端" prop="platformCode">
               <el-select v-model="form.platformCode" placeholder="请选择所属端" style="width: 100%">
                  <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
               </el-select>
            </el-form-item>
            <el-form-item label="等级编码" prop="tierCode">
               <el-input v-model="form.tierCode" placeholder="请输入等级编码" :disabled="form.id != undefined" />
            </el-form-item>
            <el-form-item label="等级名称" prop="tierName">
               <el-input v-model="form.tierName" placeholder="请输入等级名称" />
            </el-form-item>
            <el-form-item label="售价(元)" prop="price">
               <el-input-number v-model="form.price" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="原价(元)" prop="originalPrice">
               <el-input-number v-model="form.originalPrice" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="有效天数" prop="durationDays">
               <el-input-number v-model="form.durationDays" :min="-1" :precision="0" controls-position="right" />
               <span style="margin-left: 8px">-1 表示永久</span>
            </el-form-item>
            <el-form-item label="热门" prop="popular">
               <el-switch v-model="form.popular" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item label="描述" prop="description">
               <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
            </el-form-item>
            <el-form-item label="排序" prop="sortOrder">
               <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
               <el-radio-group v-model="form.status">
                  <el-radio :label="1">上架</el-radio>
                  <el-radio :label="0">下架</el-radio>
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

<script setup name="VipTier">
import { listTier, addTier, updateTier, delTier } from "@/api/system/vip";
import { platformOptionselect } from "@/api/system/platform";

const { proxy } = getCurrentInstance();

const tierList = ref([]);
const platformOptions = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    platformCode: undefined,
    tierName: undefined,
    status: undefined
  },
  rules: {
    platformCode: [{ required: true, message: "所属端不能为空", trigger: "change" }],
    tierCode: [{ required: true, message: "等级编码不能为空", trigger: "blur" }],
    tierName: [{ required: true, message: "等级名称不能为空", trigger: "blur" }],
    price: [{ required: true, message: "售价不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询VIP等级列表 */
function getList() {
  loading.value = true;
  listTier(queryParams.value).then(response => {
    tierList.value = response.data.records;
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
    platformCode: undefined,
    tierCode: undefined,
    tierName: undefined,
    price: undefined,
    originalPrice: undefined,
    durationDays: 30,
    popular: 0,
    description: undefined,
    sortOrder: 0,
    status: 1
  };
  proxy.resetForm("tierRef");
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
  multiple.value = !selection.length;
}
/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加VIP等级";
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  form.value = { ...row };
  open.value = true;
  title.value = "修改VIP等级";
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["tierRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateTier(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addTier(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}
/** 删除按钮操作 */
function handleDelete(row) {
  const tierIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除VIP等级编号为"' + tierIds + '"的数据项？').then(function () {
    return delTier(tierIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getPlatformOptions();
getList();
</script>
