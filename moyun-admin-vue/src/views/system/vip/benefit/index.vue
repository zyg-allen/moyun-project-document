<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="所属端" prop="platformCode">
            <el-select v-model="queryParams.platformCode" placeholder="所属端" clearable style="width: 200px">
               <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
            </el-select>
         </el-form-item>
         <el-form-item label="权益名称" prop="benefitName">
            <el-input
               v-model="queryParams.benefitName"
               placeholder="请输入权益名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 200px">
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
            <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:vip:benefit:add']">新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:vip:benefit:edit']">修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:vip:benefit:remove']">删除</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="benefitList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="主键" align="center" prop="id" width="80" />
         <el-table-column label="所属端" align="center" prop="platformCode" />
         <el-table-column label="权益编码" align="center" prop="benefitCode" />
         <el-table-column label="权益名称" align="center" prop="benefitName" />
         <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
         <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
         <el-table-column label="状态" align="center" prop="status" width="80">
            <template #default="scope">
               <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="right" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:vip:benefit:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:vip:benefit:remove']">删除</el-button>
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

      <!-- 添加或修改VIP权益对话框 -->
      <el-dialog :title="title" v-model="open" width="560px" append-to-body>
         <el-form ref="benefitRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="所属端" prop="platformCode">
               <el-select v-model="form.platformCode" placeholder="请选择所属端" style="width: 100%">
                  <el-option v-for="p in platformOptions" :key="p.platformCode" :label="p.platformName" :value="p.platformCode" />
               </el-select>
            </el-form-item>
            <el-form-item label="权益编码" prop="benefitCode">
               <el-input v-model="form.benefitCode" placeholder="请输入权益编码" :disabled="form.id != undefined" />
            </el-form-item>
            <el-form-item label="权益名称" prop="benefitName">
               <el-input v-model="form.benefitName" placeholder="请输入权益名称" />
            </el-form-item>
            <el-form-item label="描述" prop="description">
               <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
            </el-form-item>
            <el-form-item label="排序" prop="sortOrder">
               <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
               <el-radio-group v-model="form.status">
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

<script setup name="VipBenefit">
import { listBenefit, addBenefit, updateBenefit, delBenefit } from "@/api/system/vip";
import { platformOptionselect } from "@/api/system/platform";

const { proxy } = getCurrentInstance();

const benefitList = ref([]);
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
    benefitName: undefined,
    status: undefined
  },
  rules: {
    platformCode: [{ required: true, message: "所属端不能为空", trigger: "change" }],
    benefitCode: [{ required: true, message: "权益编码不能为空", trigger: "blur" }],
    benefitName: [{ required: true, message: "权益名称不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 查询VIP权益列表 */
function getList() {
  loading.value = true;
  listBenefit(queryParams.value).then(response => {
    benefitList.value = response.data.records;
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
    benefitCode: undefined,
    benefitName: undefined,
    description: undefined,
    sortOrder: 0,
    status: 1
  };
  proxy.resetForm("benefitRef");
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
  title.value = "添加VIP权益";
}
/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  form.value = { ...row };
  open.value = true;
  title.value = "修改VIP权益";
}
/** 提交按钮 */
function submitForm() {
  proxy.$refs["benefitRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateBenefit(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addBenefit(form.value).then(() => {
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
  const benefitIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除VIP权益编号为"' + benefitIds + '"的数据项？').then(function () {
    return delBenefit(benefitIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

getPlatformOptions();
getList();
</script>
