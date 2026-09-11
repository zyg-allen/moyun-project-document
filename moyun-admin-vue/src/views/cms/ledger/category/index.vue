<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="分类类型">
        <el-select v-model="queryParams.type" placeholder="分类类型" clearable style="width: 200px">
          <el-option label="支出" value="expense" />
          <el-option label="收入" value="income" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cms:ledgerCategory:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="categoryList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="名称" prop="name" />
      <el-table-column label="类型" prop="type" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.type === 'income' ? 'success' : 'danger'">
            {{ scope.row.type === 'income' ? '收入' : '支出' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="图标" prop="icon" width="120" />
      <el-table-column label="颜色" prop="color" width="120">
        <template #default="scope">
          <span class="color-dot" :style="{ background: scope.row.color }"></span>{{ scope.row.color }}
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="80" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cms:ledgerCategory:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cms:ledgerCategory:edit']">停用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="支出" value="expense" />
            <el-option label="收入" value="income" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="图标标识，如 food" />
        </el-form-item>
        <el-form-item label="颜色" prop="color">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
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

<script setup name="LedgerCategory">
import { listLedgerCategory, addLedgerCategory, updateLedgerCategory, delLedgerCategory } from "@/api/cms/ledger";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const showSearch = ref(true);
const categoryList = ref([]);
const open = ref(false);
const title = ref("");
const queryParams = ref({ type: "" });
const form = ref({});
const rules = {
  name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }],
  type: [{ required: true, message: "分类类型不能为空", trigger: "change" }]
};

function getList() {
  loading.value = true;
  listLedgerCategory(queryParams.value.type || {}).then(response => {
    const data = response.data || response;
    categoryList.value = data.records || [];
    loading.value = false;
  }).catch(() => { loading.value = false; });
}

function resetQuery() {
  queryParams.value = { type: "" };
  getList();
}

function reset() {
  form.value = { id: null, name: "", type: "expense", icon: "", color: "#6a4fd4", sortOrder: 0, status: 1 };
  proxy.resetForm("categoryRef");
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增预设分类";
}

function handleUpdate(row) {
  form.value = { ...row };
  open.value = true;
  title.value = "修改预设分类";
}

function submitForm() {
  proxy.$refs["categoryRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateLedgerCategory(form.value.id, form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addLedgerCategory(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  proxy.$modal.confirm('是否停用预设分类"' + row.name + '"？停用后 App 端不再展示，用户历史流水不受影响').then(() => {
    return delLedgerCategory(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("已停用");
  }).catch(() => {});
}

function cancel() {
  open.value = false;
  reset();
}

getList();
</script>

<style scoped>
.color-dot {
  display: inline-block;
  width: 14px;
  height: 14px;
  border-radius: 7px;
  margin-right: 8px;
  vertical-align: -2px;
  border: 1px solid #eee;
}
</style>
