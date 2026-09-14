<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="大类">
        <el-select v-model="queryParams.type" placeholder="全部大类" clearable style="width: 160px">
          <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="queryParams.name" placeholder="分类名称" clearable style="width: 180px"
          @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="归属">
        <el-select v-model="queryParams.owner" placeholder="全部" clearable style="width: 140px">
          <el-option label="系统预设" value="system" />
          <el-option label="用户自定义" value="user" />
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
      <el-col :span="1.5">
        <el-button type="info" plain :icon="isExpandAll ? 'Fold' : 'Expand'" @click="toggleExpandAll">
          {{ isExpandAll ? '收起' : '展开' }}
        </el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 大类（类型）分组树：一级=大类，二级=分类 -->
    <el-table v-if="refreshTable" v-loading="loading" :data="categoryTree" row-key="rowKey"
      :default-expand-all="isExpandAll" :tree-props="{ children: 'children' }">
      <el-table-column label="名称" prop="name" min-width="200">
        <template #default="scope">
          <el-tag v-if="scope.row.isTypeNode" type="primary" effect="dark">{{ scope.row.name }}</el-tag>
          <span v-else>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="归属" width="110" align="center">
        <template #default="scope">
          <el-tag v-if="!scope.row.isTypeNode" :type="scope.row.userId === 0 ? '' : 'warning'" size="small">
            {{ scope.row.userId === 0 ? '系统预设' : '用户自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="图标" prop="icon" width="120" />
      <el-table-column label="颜色" prop="color" width="120">
        <template #default="scope">
          <template v-if="!scope.row.isTypeNode">
            <span class="color-dot" :style="{ background: scope.row.color }"></span>{{ scope.row.color }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="80" />
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template #default="scope">
          <el-tag v-if="!scope.row.isTypeNode" :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <template v-if="!scope.row.isTypeNode">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cms:ledgerCategory:edit']">修改</el-button>
            <el-button link type="primary" :icon="scope.row.status === 1 ? 'CircleClose' : 'CircleCheck'"
              @click="handleToggleStatus(scope.row)" v-hasPermi="['cms:ledgerCategory:edit']">
              {{ scope.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cms:ledgerCategory:edit']">删除</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="categoryRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="大类" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" />
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
import { listLedgerCategory, addLedgerCategory, updateLedgerCategory,
  changeLedgerCategoryStatus, delLedgerCategory } from "@/api/cms/ledger";

const { proxy } = getCurrentInstance();

/** 大类（交易类型）标签：一级=大类，二级=分类（v11.76 大类二级模型） */
const TYPE_LABELS = {
  income: '收入', expense: '支出', transfer: '转账',
  repayment: '还款', borrow: '借款', adjust: '校准'
};

const loading = ref(false);
const showSearch = ref(true);
const categoryList = ref([]);
const categoryTree = ref([]);
const open = ref(false);
const title = ref("");
const isExpandAll = ref(false);
const refreshTable = ref(true);
const queryParams = ref({ type: "", name: "", owner: "" });
const form = ref({});
const rules = {
  name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }],
  type: [{ required: true, message: "所属大类不能为空", trigger: "change" }]
};

/** 按大类（类型）组装两级树：一级=大类节点，二级=该类下的分类 */
function buildTree(list) {
  const childrenMap = {};
  list.forEach(item => {
    const key = item.type || 'other';
    (childrenMap[key] = childrenMap[key] || []).push(item);
  });
  const tree = [];
  Object.entries(TYPE_LABELS).forEach(([type, label]) => {
    const children = childrenMap[type] || [];
    if (!children.length) return;
    tree.push({ rowKey: 'type-' + type, id: null, isTypeNode: true, name: `${label}（${children.length}）`,
      type, children });
  });
  // 兜底：未知类型归入"其他"
  Object.keys(childrenMap).forEach(type => {
    if (!TYPE_LABELS[type] && childrenMap[type].length) {
      tree.push({ rowKey: 'type-' + type, id: null, isTypeNode: true,
        name: `${type}（${childrenMap[type].length}）`, type, children: childrenMap[type] });
    }
  });
  return tree;
}

function getList() {
  loading.value = true;
  const params = {};
  if (queryParams.value.type) params.type = queryParams.value.type;
  if (queryParams.value.name) params.name = queryParams.value.name;
  if (queryParams.value.owner) params.owner = queryParams.value.owner;
  listLedgerCategory(params).then(response => {
    const data = response.data || response;
    categoryList.value = data.records || [];
    categoryList.value.forEach(c => { c.rowKey = 'cat-' + c.id; });
    categoryTree.value = buildTree(categoryList.value);
    loading.value = false;
  }).catch(() => { loading.value = false; });
}

function resetQuery() {
  queryParams.value = { type: "", name: "", owner: "" };
  getList();
}

function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => { refreshTable.value = true; });
}

function reset() {
  form.value = { id: null, name: "", type: "expense", icon: "", color: "#6a4fd4", sortOrder: 0, status: 1 };
  proxy.resetForm("categoryRef");
}

function handleAdd() {
  reset();
  if (queryParams.value.type) {
    form.value.type = queryParams.value.type;
  }
  open.value = true;
  title.value = "新增预设分类";
}

function handleUpdate(row) {
  form.value = { id: row.id, name: row.name, type: row.type, icon: row.icon,
    color: row.color, sortOrder: row.sortOrder, status: row.status };
  open.value = true;
  title.value = "修改分类";
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

/** 启用/停用：下架不删数据，历史流水不受影响 */
function handleToggleStatus(row) {
  const next = row.status === 1 ? 0 : 1;
  const tip = next === 1
    ? `启用分类"${row.name}"？`
    : `停用分类"${row.name}"？停用后 App 端不再展示，用户历史流水不受影响`;
  proxy.$modal.confirm(tip).then(() => {
    return changeLedgerCategoryStatus(row.id, next);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess(next === 1 ? "已启用" : "已停用");
  }).catch(() => {});
}

/** 删除：已绑定有效流水的分类后端会拒绝并提示笔数 */
function handleDelete(row) {
  proxy.$modal.confirm(`删除分类"${row.name}"？已绑定流水的分类不能删除`).then(() => {
    return delLedgerCategory(row.id);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("已删除");
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
