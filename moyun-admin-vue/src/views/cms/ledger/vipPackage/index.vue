<template>
  <div class="app-container">
    <!-- 搜索：名称 -->
    <el-form :inline="true" class="search-form">
      <el-form-item label="套餐名称">
        <el-input v-model="queryName" placeholder="套餐名称模糊搜索" clearable style="width: 200px" @keyup.enter="loadList" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openDialog(null)">新增套餐</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="套餐名称" prop="name" align="center" min-width="120" />
      <el-table-column label="售价（元）" align="center" width="110">
        <template #default="scope">¥{{ scope.row.price }}</template>
      </el-table-column>
      <el-table-column label="划线原价" align="center" width="100">
        <template #default="scope">{{ scope.row.originalPrice ? '¥' + scope.row.originalPrice : '-' }}</template>
      </el-table-column>
      <el-table-column label="时长（天）" prop="durationDays" align="center" width="100" />
      <el-table-column label="权益说明" prop="description" align="center" min-width="220" show-overflow-tooltip />
      <el-table-column label="推荐" align="center" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.popular" type="warning" size="small">推荐</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" align="center" width="70" />
      <el-table-column label="上架状态" align="center" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.status" :active-value="true" :inactive-value="false"
                     @change="toggleStatus(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="openDialog(scope.row)">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 套餐编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.form.id ? '修改套餐' : '新增套餐'" width="520px" append-to-body>
      <el-form :model="dialog.form" label-width="90px">
        <el-form-item label="套餐名称" required>
          <el-input v-model="dialog.form.name" placeholder="如：月度VIP" maxlength="32" />
        </el-form-item>
        <el-form-item label="售价（元）" required>
          <el-input-number v-model="dialog.form.price" :min="0.01" :max="10000" :precision="2" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="划线原价">
          <el-input-number v-model="dialog.form.originalPrice" :min="0" :max="10000" :precision="2" :step="1" style="width: 200px" placeholder="可空" />
        </el-form-item>
        <el-form-item label="时长（天）" required>
          <el-input-number v-model="dialog.form.durationDays" :min="1" :max="3650" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="权益说明">
          <el-input v-model="dialog.form.description" type="textarea" :rows="2" maxlength="255" placeholder="如：AI 分析报告、多账本、Excel 导出、云备份" />
        </el-form-item>
        <el-form-item label="推荐展示">
          <el-switch v-model="dialog.form.popular" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dialog.form.sort" :min="0" :max="999" :step="1" style="width: 200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="saveDialog">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LedgerVipPackage">
import { listVipPackage, addVipPackage, updateVipPackage, delVipPackage } from "@/api/cms/ledger";

const { proxy } = getCurrentInstance();

const loading = ref(false);
const dataList = ref([]);
const queryName = ref("");
const dialog = reactive({
  visible: false,
  saving: false,
  form: {}
});

/** 列表加载 */
function loadList() {
  loading.value = true;
  listVipPackage({ name: queryName.value || undefined }).then(response => {
    dataList.value = response.records || [];
  }).finally(() => {
    loading.value = false;
  });
}

/** 重置搜索 */
function resetQuery() {
  queryName.value = "";
  loadList();
}

/** 打开编辑弹窗（row=null 新增） */
function openDialog(row) {
  dialog.form = row ? { ...row } : { name: "", price: 15, originalPrice: null, durationDays: 30, description: "", popular: false, sort: 0 };
  dialog.visible = true;
}

/** 保存（新增/修改） */
function saveDialog() {
  const form = dialog.form;
  if (!form.name || !form.name.trim()) return proxy.$modal.msgError("套餐名不能为空");
  if (!form.price || form.price <= 0) return proxy.$modal.msgError("售价必须大于 0 元");
  if (!form.durationDays || form.durationDays <= 0) return proxy.$modal.msgError("时长必须大于 0 天");
  dialog.saving = true;
  const req = form.id ? updateVipPackage(form) : addVipPackage(form);
  req.then(() => {
    proxy.$modal.msgSuccess(form.id ? "修改成功" : "新增成功");
    dialog.visible = false;
    loadList();
  }).finally(() => {
    dialog.saving = false;
  });
}

/** 上下架 */
function toggleStatus(row) {
  updateVipPackage({ id: row.id, status: row.status }).then(() => {
    proxy.$modal.msgSuccess(row.status ? "已上架" : "已下架");
  }).catch(() => {
    row.status = !row.status;
  });
}

/** 删除（有订单仅可下架） */
function handleDelete(row) {
  proxy.$modal.confirm('确认删除套餐「' + row.name + '」？（已有订阅订单的套餐仅可下架）').then(() => {
    return delVipPackage(row.id);
  }).then(() => {
    proxy.$modal.msgSuccess("删除成功");
    loadList();
  }).catch(() => {});
}

loadList();
</script>

<style scoped>
.search-form { padding: 12px 16px 0; }
</style>
