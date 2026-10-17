<template>
  <div class="app-container ledger-cat">

    <!-- ===== 查询条件 ===== -->
    <el-form :model="query" :inline="true" label-width="72px" class="query-form">
      <el-form-item label="大类">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px" @change="handleQuery">
          <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="query.name" placeholder="分类名称" clearable style="width: 180px"
          @keyup.enter="handleQuery" @clear="handleQuery" />
      </el-form-item>
      <el-form-item label="归属">
        <el-select v-model="query.owner" placeholder="全部" clearable style="width: 130px" @change="handleQuery">
          <el-option label="系统预设" value="system" />
          <el-option label="用户自定义" value="user" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- ===== 操作栏 ===== -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openAdd(null)"
          v-hasPermi="['cms:ledgerCategory:add']">新增一级分类</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain :icon="isExpandAll ? 'Fold' : 'Expand'" @click="toggleExpandAll">
          {{ isExpandAll ? '收起' : '展开' }}
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-tag type="info" effect="plain">用户自定义分类为只读，后台不可改删</el-tag>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- ===== 树形表格（一级 + 二级嵌套） ===== -->
    <el-table v-if="refreshTable" v-loading="loading" :data="list" row-key="id"
      :default-expand-all="isExpandAll" :tree-props="{ children: 'children' }" border>
      <el-table-column label="分类名称" prop="name" min-width="240">
        <template #default="scope">
          <span class="cat-name" :class="{ 'is-sub': scope.row.parentId }">
            <span class="cat-dot" :style="{ background: scope.row.color || '#cbd5e1' }"></span>
            {{ scope.row.name }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="大类" width="90" align="center">
        <template #default="scope">
          <el-tag size="small" effect="plain">{{ TYPE_LABELS[scope.row.type] || scope.row.type }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="归属 / 创建人" min-width="150" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.userId === 0" size="small" type="success" effect="plain">系统预设</el-tag>
          <el-tag v-else size="small" type="warning" effect="plain">
            {{ scope.row.userName || ('用户#' + scope.row.userId) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="图标" prop="icon" width="90" align="center" show-overflow-tooltip />

      <el-table-column label="排序" prop="sortOrder" width="70" align="center" />

      <el-table-column label="状态" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="290" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <!-- 系统预设：可维护 -->
          <template v-if="scope.row.userId === 0">
            <el-button v-if="!scope.row.parentId" link type="primary" icon="Plus"
              @click="openAdd(scope.row)" v-hasPermi="['cms:ledgerCategory:add']">子分类</el-button>
            <el-button link type="primary" icon="Edit" @click="openEdit(scope.row)"
              v-hasPermi="['cms:ledgerCategory:edit']">修改</el-button>
            <el-button link :type="scope.row.status === 1 ? 'warning' : 'success'"
              :icon="scope.row.status === 1 ? 'CircleClose' : 'CircleCheck'"
              @click="onToggle(scope.row)" v-hasPermi="['cms:ledgerCategory:edit']">
              {{ scope.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" icon="Delete" @click="onDelete(scope.row)"
              v-hasPermi="['cms:ledgerCategory:edit']">删除</el-button>
          </template>
          <!-- 用户自定义：只读 -->
          <span v-else class="readonly-hint">只读</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- ===== 分页 ===== -->
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum"
      v-model:limit="query.pageSize" @pagination="getList" />

    <!-- ===== 新增/编辑弹层 ===== -->
    <el-dialog v-model="open" :title="dialogTitle" width="520px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="所属大类" prop="type">
          <el-select v-model="form.type" style="width: 100%" :disabled="!!form.id">
            <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>

        <el-form-item label="父分类" prop="parentId">
          <el-select v-model="form.parentId" style="width: 100%" clearable placeholder="留空 = 一级分类">
            <el-option label="无（一级分类）" :value="0" />
            <el-option v-for="p in parentOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：交通 / 地铁" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="选填，如：food / car" maxlength="30" />
        </el-form-item>

        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="field-hint">数字越小越靠前</span>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="open = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LedgerCategory">
import { listLedgerCategory, addLedgerCategory, updateLedgerCategory,
  changeLedgerCategoryStatus, delLedgerCategory } from "@/api/cms/ledger";

const { proxy } = getCurrentInstance();

/** 大类（交易类型）中文名 */
const TYPE_LABELS = {
  income: '收入', expense: '支出', transfer: '转账',
  lend: '借出', collect: '收款', borrow: '借款', repayment: '还款', adjust: '校准'
};

const loading = ref(false);
const submitting = ref(false);
const showSearch = ref(true);
const isExpandAll = ref(true);
const refreshTable = ref(true);
const open = ref(false);
const list = ref([]);
const total = ref(0);
const formRef = ref();
const form = ref({});

const query = ref({ type: "", name: "", owner: "", pageNum: 1, pageSize: 10 });

const rules = {
  type: [{ required: true, message: "请选择所属大类", trigger: "change" }],
  name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }]
};

const dialogTitle = computed(() => {
  if (form.value.id) return "编辑分类";
  return form.value.parentId ? "新增子分类" : "新增一级分类";
});

/** 父分类下拉：当前大类下的一级分类（编辑时排除自身） */
const parentOptions = computed(() =>
  list.value.filter(c =>
    !c.parentId && (!form.value.type || c.type === form.value.type) && c.id !== form.value.id
  )
);

function getList() {
  loading.value = true;
  const params = {
    pageNum: query.value.pageNum,
    pageSize: query.value.pageSize
  };
  if (query.value.type) params.type = query.value.type;
  if (query.value.name) params.name = query.value.name;
  if (query.value.owner) params.owner = query.value.owner;
  listLedgerCategory(params).then(res => {
    const data = res.data || res;
    list.value = data.records || [];
    total.value = Number(data.total || 0);
  }).finally(() => { loading.value = false; });
}

function handleQuery() {
  query.value.pageNum = 1;
  getList();
}

function resetQuery() {
  query.value = { type: "", name: "", owner: "", pageNum: 1, pageSize: 10 };
  getList();
}

function toggleExpandAll() {
  refreshTable.value = false;
  isExpandAll.value = !isExpandAll.value;
  nextTick(() => { refreshTable.value = true; });
}

/** parent=null 新增一级；传一级行则新增其子分类 */
function openAdd(parent) {
  form.value = {
    id: null,
    type: parent ? parent.type : (query.value.type || "expense"),
    parentId: parent ? parent.id : 0,
    name: "", icon: "", color: "#6a4fd4", sortOrder: 0, status: 1
  };
  open.value = true;
  nextTick(() => formRef.value && formRef.value.clearValidate());
}

function openEdit(row) {
  form.value = {
    id: row.id, type: row.type, parentId: row.parentId || 0, name: row.name,
    icon: row.icon || "", color: row.color || "#6a4fd4",
    sortOrder: row.sortOrder ?? 0, status: row.status ?? 1
  };
  open.value = true;
  nextTick(() => formRef.value && formRef.value.clearValidate());
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return;
    submitting.value = true;
    const payload = { ...form.value, parentId: form.value.parentId || 0 };
    const req = payload.id
      ? updateLedgerCategory(payload.id, payload)
      : addLedgerCategory(payload);
    req.then(() => {
      proxy.$modal.msgSuccess(payload.id ? "修改成功" : "新增成功");
      open.value = false;
      getList();
    }).catch(() => {}).finally(() => { submitting.value = false; });
  });
}

/** 启用/停用：不删数据，历史流水不受影响，App 端不再展示停用项 */
function onToggle(row) {
  const next = row.status === 1 ? 0 : 1;
  const tip = next === 1
    ? `启用「${row.name}」？`
    : `停用「${row.name}」？停用后 App 端不再展示，历史流水不受影响`;
  proxy.$modal.confirm(tip).then(() => changeLedgerCategoryStatus(row.id, next))
    .then(() => { getList(); proxy.$modal.msgSuccess(next === 1 ? "已启用" : "已停用"); })
    .catch(() => {});
}

/** 删除：后端校验「有流水 / 被作为二级引用 / 有子分类」会拒绝并给出原因 */
function onDelete(row) {
  proxy.$modal.confirm(`删除「${row.name}」？已绑定流水或含子分类的分类不能删除`).then(() => delLedgerCategory(row.id))
    .then(() => { getList(); proxy.$modal.msgSuccess("已删除"); })
    .catch(() => {});
}

getList();
</script>

<style scoped>
.ledger-cat { padding-bottom: 24px; }
.query-form { margin-bottom: 8px; }
.cat-name { display: inline-flex; align-items: center; gap: 6px; }
.cat-name.is-sub { padding-left: 8px; color: var(--el-text-color-regular); }
.cat-dot { width: 8px; height: 8px; border-radius: 50%; flex: 0 0 auto; }
.field-hint { margin-left: 10px; font-size: 12px; color: var(--el-text-color-placeholder); }
.readonly-hint { font-size: 12px; color: var(--el-text-color-placeholder); }
</style>
