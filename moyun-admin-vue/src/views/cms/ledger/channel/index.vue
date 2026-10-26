<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" class="page-tip"
      title="渠道作为系统预设下发给 App：售卖渠道用于出库时标记「卖到哪里」，进货渠道用于入库时记录「从哪来」。系统预设可维护；用户自建渠道只读。渠道已被库存流水引用时禁止删除，只能停用。" />

    <el-tabs v-model="kind" @tab-change="onKindChange">
      <el-tab-pane label="售卖渠道" name="sale" />
      <el-tab-pane label="进货渠道" name="source" />
    </el-tabs>

    <el-form :model="query" :inline="true" class="query-form">
      <el-form-item label="名称">
        <el-input v-model="query.name" placeholder="渠道名称" clearable style="width: 160px"
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
        <el-button type="primary" plain icon="Plus" @click="openAdd"
          v-hasPermi="['cms:ledgerChannel:add']">新增{{ kindLabel }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-tag type="info" effect="plain">{{ kindTip }}</el-tag>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="渠道名称" prop="name" min-width="200">
        <template #default="scope">
          <span class="ch-name">{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="归属" width="140" align="center">
        <template #default="scope">
          <el-tag v-if="isSystemRow(scope.row)" size="small" type="success" effect="plain">系统预设</el-tag>
          <el-tag v-else size="small" type="warning" effect="plain">用户#{{ scope.row.userId }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="引用" width="100" align="center">
        <template #default="scope">
          <span :class="{ 'used-zero': !scope.row.usedCount }">{{ scope.row.usedCount || 0 }} 笔</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" align="center" fixed="right">
        <template #default="scope">
          <template v-if="isSystemRow(scope.row)">
            <el-button link type="primary" icon="Edit" @click="openEdit(scope.row)"
              v-hasPermi="['cms:ledgerChannel:edit']">修改</el-button>
            <el-button link :type="scope.row.status === 1 ? 'warning' : 'success'"
              :icon="scope.row.status === 1 ? 'CircleClose' : 'CircleCheck'"
              @click="onToggle(scope.row)" v-hasPermi="['cms:ledgerChannel:edit']">
              {{ scope.row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" icon="Delete" @click="onDelete(scope.row)"
              v-hasPermi="['cms:ledgerChannel:edit']">删除</el-button>
          </template>
          <el-tooltip v-else content="用户自建渠道属于用户数据，后台只读" placement="top">
            <el-tag type="info" size="small" effect="plain">只读</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum"
      v-model:limit="query.pageSize" @pagination="getList" />

    <el-dialog v-model="open" :title="dialogTitle" width="460px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item :label="kindLabel" prop="name">
          <el-input v-model="form.name" :placeholder="kindPlaceholder" maxlength="30" show-word-limit />
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

<script setup name="LedgerChannel">
import {
  listLedgerChannel, addLedgerChannel, updateLedgerChannel,
  changeLedgerChannelStatus, delLedgerChannel
} from '@/api/cms/ledger'

const { proxy } = getCurrentInstance()

const kind = ref('sale')
const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const formRef = ref()

const query = ref({ name: '', owner: '', status: undefined, pageNum: 1, pageSize: 10 })
const form = ref({})
const rules = { name: [{ required: true, message: '渠道名称不能为空', trigger: 'blur' }] }

const kindLabel = computed(() => (kind.value === 'sale' ? '售卖渠道' : '进货渠道'))
const kindTip = computed(() => (kind.value === 'sale'
  ? '卖给谁：抖音 / 淘宝 / 闲鱼 / 线下…'
  : '从哪来：拼多多 / 批发市场 / 朋友赠…'))
const kindPlaceholder = computed(() => (kind.value === 'sale' ? '如：抖音小店' : '如：1688 批发'))
const dialogTitle = computed(() => (form.value.id ? '编辑' : '新增') + kindLabel.value)
const isSystemRow = row => Number(row.userId) === 0

function getList() {
  loading.value = true
  const params = { kind: kind.value, pageNum: query.value.pageNum, pageSize: query.value.pageSize }
  if (query.value.name) params.name = query.value.name.trim()
  if (query.value.owner) params.owner = query.value.owner
  if (query.value.status === 0 || query.value.status === 1) params.status = query.value.status
  listLedgerChannel(params).then(res => {
    list.value = res.rows || []
    total.value = Number(res.total || 0)
  }).finally(() => { loading.value = false })
}

function onKindChange() {
  query.value.pageNum = 1
  getList()
}
function handleQuery() { query.value.pageNum = 1; getList() }
function resetQuery() {
  query.value = { name: '', owner: '', status: undefined, pageNum: 1, pageSize: 10 }
  getList()
}

function openAdd() {
  form.value = { id: null, name: '', sortOrder: 0, status: 1 }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}
function openEdit(row) {
  form.value = { id: row.id, name: row.name, sortOrder: row.sortOrder || 0, status: row.status === 0 ? 0 : 1 }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const payload = {
      kind: kind.value,
      name: (form.value.name || '').trim(),
      sortOrder: form.value.sortOrder || 0,
      status: form.value.status
    }
    submitting.value = true
    const req = form.value.id
      ? updateLedgerChannel(kind.value, form.value.id, payload)
      : addLedgerChannel(payload)
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
    ? '停用后 App 端不再展示该渠道（历史流水保留），确认停用？'
    : '确认启用该渠道？').then(() => changeLedgerChannelStatus(kind.value, row.id, next)).then(() => {
    proxy.$modal.msgSuccess(next === 0 ? '已停用' : '已启用')
    getList()
  }).catch(() => {})
}

function onDelete(row) {
  proxy.$modal.confirm('确认删除渠道「' + row.name + '」？已被库存流水引用时会被拒绝。')
    .then(() => delLedgerChannel(kind.value, row.id)).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    }).catch(() => {})
}

getList()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.query-form { margin-bottom: 4px; }
.ch-name { font-weight: 500; }
.used-zero { color: #c0c4cc; }
.field-hint { font-size: 12px; color: #909399; margin-left: 8px; }
</style>
