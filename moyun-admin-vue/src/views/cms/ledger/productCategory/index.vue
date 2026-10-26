<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" class="page-tip"
      title="商品类目（两级树）：作为系统预设提供给所有用户选择；用户自建类目只读。类目下有商品或子类目时禁止删除，只能停用。" />

    <el-form :model="query" :inline="true" class="query-form">
      <el-form-item label="名称">
        <el-input v-model="query.name" placeholder="类目名称" clearable style="width: 160px"
          @keyup.enter="handleQuery" @clear="handleQuery" />
      </el-form-item>
      <el-form-item label="归属">
        <el-select v-model="query.owner" placeholder="全部" clearable style="width: 120px" @change="handleQuery">
          <el-option label="系统预设" value="system" />
          <el-option label="用户自建" value="user" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px" @change="handleQuery">
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
        <el-button type="primary" plain icon="Plus" @click="openAdd(null)"
          v-hasPermi="['cms:ledgerProductCategory:add']">新增一级类目</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain :icon="isExpandAll ? 'Fold' : 'Expand'" @click="toggleExpandAll">
          {{ isExpandAll ? '收起' : '展开' }}
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-tag type="info" effect="plain">最多两级：一级（食品/日用品…）→ 二级（零食/生鲜…）</el-tag>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-if="refreshTable" v-loading="loading" :data="list" row-key="id"
      :default-expand-all="isExpandAll" :tree-props="{ children: 'children' }" border>
      <el-table-column label="类目名称" min-width="240">
        <template #default="scope">
          <span class="cat-name" :class="{ 'is-sub': !isTopLevel(scope.row) }">
            <span class="cat-icon">{{ scope.row.icon || '📦' }}</span>
            {{ scope.row.name }}
          </span>
          <el-tag v-if="!isTopLevel(scope.row)" size="small" type="info" effect="plain" class="ml8">二级</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="归属" width="140" align="center">
        <template #default="scope">
          <el-tag v-if="isSystemRow(scope.row)" size="small" type="success" effect="plain">系统预设</el-tag>
          <el-tag v-else size="small" type="warning" effect="plain">用户#{{ scope.row.userId }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="商品数" width="90" align="center">
        <template #default="scope">
          <span :class="{ 'used-zero': !scope.row.usedCount }">{{ scope.row.usedCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center" fixed="right">
        <template #default="scope">
          <template v-if="isSystemRow(scope.row)">
            <el-button v-if="isTopLevel(scope.row)" link type="primary" icon="Plus"
              @click="openAdd(scope.row)" v-hasPermi="['cms:ledgerProductCategory:add']">子类目</el-button>
            <el-button link type="primary" icon="Edit" @click="openEdit(scope.row)"
              v-hasPermi="['cms:ledgerProductCategory:edit']">修改</el-button>
            <el-button link :type="scope.row.status === 1 ? 'warning' : 'success'"
              :icon="scope.row.status === 1 ? 'CircleClose' : 'CircleCheck'"
              @click="onToggle(scope.row)" v-hasPermi="['cms:ledgerProductCategory:edit']">
              {{ scope.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" icon="Delete" @click="onDelete(scope.row)"
              v-hasPermi="['cms:ledgerProductCategory:edit']">删除</el-button>
          </template>
          <el-tooltip v-else content="用户自建类目属于用户数据，后台只读" placement="top">
            <el-tag type="info" size="small" effect="plain">只读</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum"
      v-model:limit="query.pageSize" @pagination="getList" />

    <el-dialog v-model="open" :title="dialogTitle" width="520px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="父类目">
          <el-select v-model="form.parentId" style="width: 100%" clearable placeholder="留空 = 一级类目">
            <el-option label="无（一级类目）" :value="0" />
            <el-option v-for="p in parentOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类目名称" prop="name">
          <el-input v-model="form.name" placeholder="如：食品 / 零食" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji，如 🍱（选填）" maxlength="8" style="width: 160px" />
          <span class="field-hint">App 端直接展示该 emoji</span>
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

<script setup name="LedgerProductCategory">
import {
  listProductCategory, addProductCategory, updateProductCategory,
  changeProductCategoryStatus, delProductCategory
} from '@/api/cms/ledger'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const isExpandAll = ref(true)
const refreshTable = ref(true)
const open = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const formRef = ref()

const query = ref({ name: '', owner: '', status: undefined, pageNum: 1, pageSize: 10 })
const form = ref({})
const rules = { name: [{ required: true, message: '类目名称不能为空', trigger: 'blur' }] }

const dialogTitle = computed(() => {
  if (form.value.id) return '编辑类目'
  return form.value.parentId ? '新增二级类目' : '新增一级类目'
})

const isSystemRow = row => Number(row.userId) === 0
const isTopLevel = row => !row.parentId || Number(row.parentId) === 0
const parentOptions = computed(() => list.value.filter(c =>
  isTopLevel(c) && String(c.id) !== String(form.value.id)
))

function getList() {
  loading.value = true
  const params = { pageNum: query.value.pageNum, pageSize: query.value.pageSize }
  if (query.value.name) params.name = query.value.name.trim()
  if (query.value.owner) params.owner = query.value.owner
  if (query.value.status === 0 || query.value.status === 1) params.status = query.value.status
  listProductCategory(params).then(res => {
    list.value = res.rows || []
    total.value = Number(res.total || 0)
  }).finally(() => { loading.value = false })
}

function handleQuery() { query.value.pageNum = 1; getList() }
function resetQuery() {
  query.value = { name: '', owner: '', status: undefined, pageNum: 1, pageSize: 10 }
  getList()
}
function toggleExpandAll() {
  refreshTable.value = false
  isExpandAll.value = !isExpandAll.value
  nextTick(() => { refreshTable.value = true })
}

function openAdd(parent) {
  form.value = {
    id: null,
    parentId: parent ? parent.id : 0,
    name: '', icon: '', sortOrder: 0, status: 1
  }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function openEdit(row) {
  form.value = {
    id: row.id,
    parentId: isTopLevel(row) ? 0 : Number(row.parentId || 0),
    name: row.name, icon: row.icon || '', sortOrder: row.sortOrder || 0, status: row.status === 0 ? 0 : 1
  }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const payload = {
      parentId: form.value.parentId === 0 ? 0 : form.value.parentId,
      name: (form.value.name || '').trim(),
      icon: (form.value.icon || '').trim() || null,
      sortOrder: form.value.sortOrder || 0,
      status: form.value.status
    }
    submitting.value = true
    const req = form.value.id
      ? updateProductCategory(form.value.id, payload)
      : addProductCategory(payload)
    req.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}

function onToggle(row) {
  const next = row.status === 1 ? 0 : 1
  proxy.$modal.confirm(next === 0
    ? '停用后 App 端不再展示该类目（已归属商品保留），确认停用？'
    : '确认启用该类目？').then(() => changeProductCategoryStatus(row.id, next)).then(() => {
    proxy.$modal.msgSuccess(next === 0 ? '已停用' : '已启用')
    getList()
  }).catch(() => {})
}

function onDelete(row) {
  proxy.$modal.confirm('确认删除类目「' + row.name + '」？有商品引用或含子类目时会被拒绝。')
    .then(() => delProductCategory(row.id)).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    }).catch(() => {})
}

getList()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.cat-name { display: inline-flex; align-items: center; font-size: 14px; }
.cat-name.is-sub { padding-left: 8px; color: #606266; }
.cat-icon { margin-right: 8px; font-size: 16px; }
.ml8 { margin-left: 8px; }
.used-zero { color: #c0c4cc; }
.field-hint { font-size: 12px; color: #909399; margin-left: 8px; }
</style>
