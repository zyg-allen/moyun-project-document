<template>
  <div class="app-container ledger-cat">

    <!-- ===== 顶部工具栏 ===== -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <el-radio-group v-model="query.type" @change="getList">
          <el-radio-button label="">全部大类</el-radio-button>
          <el-radio-button v-for="(label, key) in TYPE_LABELS" :key="key" :label="key">{{ label }}</el-radio-button>
        </el-radio-group>

        <div class="toolbar-right">
          <el-input v-model="query.name" placeholder="搜索分类名称" clearable style="width: 200px"
            @keyup.enter="getList" @clear="getList" />
          <el-select v-model="query.owner" placeholder="归属" clearable style="width: 130px" @change="getList">
            <el-option label="系统预设" value="system" />
            <el-option label="用户自定义" value="user" />
          </el-select>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button type="primary" icon="Plus" @click="openAdd(null)" v-hasPermi="['cms:ledgerCategory:add']">
            新增一级分类
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- ===== 一级分类卡片网格 ===== -->
    <div v-loading="loading" class="cat-grid">
      <el-card v-for="g in groups" :key="g.id" shadow="hover" class="cat-card"
        :class="{ 'is-off': g.status === 0 }">
        <!-- 卡片头：名称 + 徽标 + 操作 -->
        <div class="cat-head">
          <div class="cat-title">
            <span class="cat-dot" :style="{ background: g.color || '#6a4fd4' }"></span>
            <span class="cat-name">{{ g.name }}</span>
            <el-tag size="small" effect="plain">{{ TYPE_LABELS[g.type] || g.type }}</el-tag>
            <el-tag size="small" type="info" round>{{ g.children.length }}</el-tag>
            <el-tag v-if="g.isSystem === 1" size="small" type="success" effect="plain">预设</el-tag>
            <el-tag v-else size="small" type="warning" effect="plain">自定义</el-tag>
            <el-tag v-if="g.status === 0" size="small" type="danger" effect="plain">已停用</el-tag>
          </div>
          <div class="cat-actions">
            <el-button link type="primary" icon="Plus" size="small"
              @click="openAdd(g)" v-hasPermi="['cms:ledgerCategory:add']">子分类</el-button>
            <el-button link type="primary" icon="Edit" size="small"
              @click="openEdit(g)" v-hasPermi="['cms:ledgerCategory:edit']">编辑</el-button>
            <el-button link :type="g.status === 1 ? 'warning' : 'success'" size="small"
              :icon="g.status === 1 ? 'CircleClose' : 'CircleCheck'"
              @click="onToggle(g)" v-hasPermi="['cms:ledgerCategory:edit']">{{ g.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" icon="Delete" size="small"
              @click="onDelete(g)" v-hasPermi="['cms:ledgerCategory:edit']">删除</el-button>
          </div>
        </div>

        <!-- 二级分类列表 -->
        <div v-if="g.children.length" class="sub-list">
          <div v-for="s in g.children" :key="s.id" class="sub-item" :class="{ 'is-off': s.status === 0 }">
            <span class="sub-dot" :style="{ background: s.color || '#cbd5e1' }"></span>
            <span class="sub-name">{{ s.name }}</span>
            <el-tag v-if="s.status === 0" size="small" type="danger" effect="plain">停用</el-tag>
            <span class="sub-actions">
              <el-button link type="primary" size="small" @click="openEdit(s)"
                v-hasPermi="['cms:ledgerCategory:edit']">编辑</el-button>
              <el-button link :type="s.status === 1 ? 'warning' : 'success'" size="small"
                @click="onToggle(s)" v-hasPermi="['cms:ledgerCategory:edit']">{{ s.status === 1 ? '停用' : '启用' }}</el-button>
              <el-button link type="danger" size="small" @click="onDelete(s)"
                v-hasPermi="['cms:ledgerCategory:edit']">删除</el-button>
            </span>
          </div>
        </div>
        <div v-else class="sub-empty">暂无子分类，点击「子分类」添加</div>
      </el-card>

      <el-empty v-if="!groups.length && !loading" description="暂无分类，点击右上角「新增一级分类」" class="empty-block" />
    </div>

    <!-- ===== 新增/编辑弹层 ===== -->
    <el-dialog v-model="open" :title="form.id ? '编辑分类' : (form.parentId ? '新增子分类' : '新增一级分类')"
      width="520px" append-to-body destroy-on-close>
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
          <span class="color-hint">用于卡片圆点与 App 端图标底色</span>
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="color-hint">数字越小越靠前</span>
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
const open = ref(false);
const list = ref([]);
const query = ref({ type: "", name: "", owner: "" });
const formRef = ref();
const form = ref({});

const rules = {
  type: [{ required: true, message: "请选择所属大类", trigger: "change" }],
  name: [{ required: true, message: "分类名称不能为空", trigger: "blur" }]
};

/** 一级分类（parentId 为 null/0）+ 其二级子分类，按 type → sortOrder 排序 */
const groups = computed(() => {
  const all = list.value;
  const tops = all.filter(c => !c.parentId || c.parentId === 0);
  const byParent = {};
  all.forEach(c => {
    if (c.parentId && c.parentId !== 0) {
      (byParent[c.parentId] = byParent[c.parentId] || []).push(c);
    }
  });
  return tops
    .map(t => ({ ...t, children: (byParent[t.id] || []).sort(byOrder) }))
    .sort((a, b) => (TYPE_ORDER[a.type] ?? 99) - (TYPE_ORDER[b.type] ?? 99) || byOrder(a, b));
});

const TYPE_ORDER = { income: 1, expense: 2, transfer: 3, lend: 4, collect: 5, borrow: 6, repayment: 7, adjust: 8 };
function byOrder(a, b) {
  return (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || (a.id ?? 0) - (b.id ?? 0);
}

/** 父分类下拉：当前大类下的一级分类，编辑时排除自己 */
const parentOptions = computed(() =>
  list.value.filter(c =>
    (!c.parentId || c.parentId === 0) &&
    (!form.value.type || c.type === form.value.type) &&
    c.id !== form.value.id
  )
);

function getList() {
  loading.value = true;
  const params = {};
  if (query.value.type) params.type = query.value.type;
  if (query.value.name) params.name = query.value.name;
  if (query.value.owner) params.owner = query.value.owner;
  listLedgerCategory(params)
    .then(res => {
      const data = res.data || res;
      list.value = data.records || [];
    })
    .finally(() => { loading.value = false; });
}

function resetQuery() {
  query.value = { type: "", name: "", owner: "" };
  getList();
}

/** 打开新增：parent=null 为一级；传一级对象则新增其子分类 */
function openAdd(parent) {
  form.value = {
    id: null,
    type: parent ? parent.type : (query.value.type || "expense"),
    parentId: parent ? parent.id : 0,
    name: "",
    icon: "",
    color: "#6a4fd4",
    sortOrder: 0,
    status: 1
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

/** 删除：后端校验「有流水/有子分类」会拒绝并提示原因 */
function onDelete(row) {
  proxy.$modal.confirm(`删除「${row.name}」？已绑定流水或含子分类的分类不能删除`).then(() => delLedgerCategory(row.id))
    .then(() => { getList(); proxy.$modal.msgSuccess("已删除"); })
    .catch(() => {});
}

getList();
</script>

<style scoped>
.ledger-cat { padding-bottom: 24px; }

/* 工具栏 */
.toolbar-card { margin-bottom: 16px; border-radius: 10px; }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.toolbar-right { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

/* 卡片网格 */
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(420px, 1fr)); gap: 16px; }
.cat-card { border-radius: 12px; transition: box-shadow .2s, opacity .2s; }
.cat-card:hover { box-shadow: 0 6px 18px rgba(0,0,0,.08); }
.cat-card.is-off { opacity: .62; }
.empty-block { grid-column: 1 / -1; }

/* 卡片头 */
.cat-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 8px; flex-wrap: wrap; }
.cat-title { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; min-width: 0; }
.cat-dot { width: 10px; height: 10px; border-radius: 50%; flex: 0 0 auto; }
.cat-name { font-size: 15px; font-weight: 600; color: var(--el-text-color-primary); }
.cat-actions { display: flex; align-items: center; gap: 2px; flex-wrap: wrap; }

/* 二级列表 */
.sub-list { margin-top: 12px; border-top: 1px dashed var(--el-border-color-lighter); padding-top: 10px; }
.sub-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 8px; border-radius: 8px; transition: background .15s;
}
.sub-item:hover { background: var(--el-fill-color-lighter); }
.sub-item.is-off { opacity: .55; }
.sub-dot { width: 6px; height: 6px; border-radius: 50%; flex: 0 0 auto; }
.sub-name { flex: 1; font-size: 13px; color: var(--el-text-color-regular); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sub-actions { display: flex; align-items: center; gap: 2px; }
.sub-empty { margin-top: 12px; font-size: 12px; color: var(--el-text-color-placeholder); }

.color-hint { margin-left: 10px; font-size: 12px; color: var(--el-text-color-placeholder); }
</style>
