<template>
  <div class="app-container ledger-preset">
    <el-alert type="info" :closable="false" class="page-tip"
      title="系统预设分类：后台可增删改/排序/启停；用户自建分类只读（属用户数据）。分类已被流水引用或存在子分类时禁止删除，只能停用。" />

    <!-- 查询区 -->
    <el-form :model="query" :inline="true" class="query-form">
      <el-form-item label="记账类型">
        <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 140px" @change="handleQuery">
          <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="名称">
        <el-input v-model="query.name" placeholder="分类名称" clearable style="width: 160px"
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

    <!-- 操作栏 -->
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
        <el-tag type="info" effect="plain">结构：记账类型 → 一级分类 → 二级分类（最多两级）</el-tag>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <!-- 树形表格 -->
    <el-table v-if="refreshTable" v-loading="loading" :data="list" row-key="id"
      :default-expand-all="isExpandAll" :tree-props="{ children: 'children' }" border>
      <el-table-column label="分类名称" min-width="220">
        <template #default="scope">
          <span class="cat-name" :class="{ 'is-sub': !isTopLevel(scope.row) }">
            <span class="cat-dot" :style="{ background: scope.row.color || '#cbd5e1' }"></span>
            {{ scope.row.name }}
          </span>
          <el-tag v-if="!isTopLevel(scope.row)" size="small" type="info" effect="plain" class="ml8">二级</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="记账类型" width="100" align="center">
        <template #default="scope">
          <el-tag size="small" effect="plain">{{ TYPE_LABELS[scope.row.type] || scope.row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分组" width="100" align="center">
        <template #default="scope">{{ scope.row.groupName || '—' }}</template>
      </el-table-column>
      <el-table-column label="归属" width="130" align="center">
        <template #default="scope">
          <el-tag v-if="isSystemRow(scope.row)" size="small" type="success" effect="plain">系统预设</el-tag>
          <el-tag v-else size="small" type="warning" effect="plain">{{ ownerLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="图标" width="70" align="center">
        <template #default="scope">
          <img v-if="isImgIcon(scope.row.icon)" :src="scope.row.icon" class="icon-img" :title="scope.row.icon" />
          <span v-else class="icon-emoji" :title="scope.row.icon || ''">{{ iconEmoji(scope.row.icon) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="引用" width="90" align="center">
        <template #default="scope">
          <span :class="{ 'used-zero': !scope.row.usedCount }">{{ scope.row.usedCount || 0 }} 笔</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="70" align="center" />
      <el-table-column label="状态" width="80" align="center">
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
          <el-tooltip v-else content="用户自建分类属于用户数据，后台只读" placement="top">
            <el-tag type="info" size="small" effect="plain">只读</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum"
      v-model:limit="query.pageSize" @pagination="getList" />

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="open" :title="dialogTitle" width="560px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="记账类型" prop="type">
          <el-select v-model="form.type" style="width: 100%" :disabled="!!form.id" @change="onTypeChange">
            <el-option v-for="(label, key) in TYPE_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
          <div class="field-hint">分类归属的记账行为类型，创建后不可修改（影响历史统计口径）</div>
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" style="width: 100%" clearable placeholder="留空 = 一级分类">
            <el-option label="无（一级分类）" :value="0" />
            <el-option v-for="p in parentOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="如：交通 / 地铁" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分组名">
          <el-input v-model="form.groupName" placeholder="选填，如：生活刚需 / 负债还款（App 按此分组展示）"
            maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="图标">
          <div style="width: 100%">
            <el-select v-model="form.icon" placeholder="选择 emoji 图标（选填）" clearable filterable style="width: 100%">
              <el-option v-for="ic in ICON_OPTIONS" :key="ic.key" :label="ic.label" :value="ic.key">
                <span class="opt-emoji">{{ ic.emoji }}</span>{{ ic.label }}
              </el-option>
            </el-select>
            <el-input v-model="form.iconUrlInput" placeholder="或粘贴图片 URL（优先于 emoji）"
              style="margin-top: 8px" clearable />
          </div>
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
          <span class="field-hint" style="margin-left: 12px">用于 App 分类圆点与浅色底</span>
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
import {
  listLedgerCategory, addLedgerCategory, updateLedgerCategory,
  changeLedgerCategoryStatus, delLedgerCategory
} from '@/api/cms/ledger'

const { proxy } = getCurrentInstance()

const TYPE_LABELS = {
  income: '收入', expense: '支出', transfer: '转账',
  lend: '借出', collect: '收款', borrow: '借款', repayment: '还款', adjust: '校准'
}

/** 与 App utils/categoryIcon.js 的 ICON_MAP 对齐（value 是 App 识别的 icon key） */
const ICON_EMOJI = {
  food: '🍜', transport: '🚌', shopping: '🛍️', home: '🏠', entertainment: '🎮',
  medical: '💊', education: '📚', phone: '📱', daily: '🧻', gift: '🎁',
  pet: '🐾', travel: '✈️', 'house-loan': '🏦', 'car-loan': '🚗', repayment: '💳',
  interest: '📈', other: '🔖',
  salary: '💰', bonus: '🎉', parttime: '💼', invest: '📊', redpacket: '🧧',
  refund: '↩️', 'borrow-in': '🤝', secondhand: '♻️',
  'transfer-self': '🔄', 'transfer-friend': '👥', 'transfer-proxy': '🔀',
  'transfer-refund': '↩️', 'transfer-other': '🔖',
  'repay-card': '💳', 'repay-loan': '🏦', 'repay-personal': '🤝',
  'repay-interest': '📈', 'repay-other': '🔖',
  'borrow-card': '💳', 'borrow-online': '🌐', 'borrow-bank': '🏦',
  'borrow-installment': '📅', 'borrow-personal': '🤝', 'borrow-other': '🔖',
  'adjust-balance': '⚖️', 'adjust-fee': '💸', 'adjust-fx': '💱', 'adjust-other': '🔖'
}
const ICON_CN = {
  food: '餐饮', transport: '交通', shopping: '购物', home: '居家', entertainment: '娱乐',
  medical: '医疗', education: '教育', phone: '通讯', daily: '日用', gift: '礼物',
  pet: '宠物', travel: '旅行', 'house-loan': '房贷', 'car-loan': '车贷', repayment: '还款',
  interest: '利息', other: '其他', salary: '工资', bonus: '奖金', parttime: '兼职',
  invest: '投资', redpacket: '红包', refund: '退款', 'borrow-in': '借入', secondhand: '二手',
  'transfer-self': '账户互转', 'transfer-friend': '转亲友', 'transfer-proxy': '代付代收',
  'transfer-refund': '退款退回', 'transfer-other': '其他转账',
  'repay-card': '信用卡还款', 'repay-loan': '贷款还款', 'repay-personal': '私人还款',
  'repay-interest': '利息支出', 'repay-other': '其他还款',
  'borrow-card': '信用卡消费', 'borrow-online': '网贷', 'borrow-bank': '银行贷款',
  'borrow-installment': '消费分期', 'borrow-personal': '私人借款', 'borrow-other': '其他借款',
  'adjust-balance': '余额校准', 'adjust-fee': '手续费', 'adjust-fx': '汇率差', 'adjust-other': '其他校准'
}
const ICON_OPTIONS = Object.keys(ICON_EMOJI).map(key => ({
  key, emoji: ICON_EMOJI[key], label: ICON_CN[key] || key
}))

const loading = ref(false)
const showSearch = ref(true)
const isExpandAll = ref(true)
const refreshTable = ref(true)
const open = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const formRef = ref()

const query = ref({ type: '', name: '', owner: '', status: undefined, pageNum: 1, pageSize: 10 })
const form = ref({})
const rules = {
  type: [{ required: true, message: '请选择记账类型', trigger: 'change' }],
  name: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }]
}

const dialogTitle = computed(() => {
  if (form.value.id) return '编辑分类'
  return form.value.parentId ? '新增二级分类' : '新增一级分类'
})

/** 父分类候选：同类型一级分类（编辑时排除自身） */
const parentOptions = computed(() => {
  const roots = []
  list.value.forEach(c => {
    if (!isTopLevel(c)) return
    if (form.value.type && c.type !== form.value.type) return
    if (String(c.id) === String(form.value.id)) return
    roots.push(c)
  })
  return roots
})

const isSystemRow = row => Number(row.userId) === 0
const isTopLevel = row => !row.parentId || Number(row.parentId) === 0
const iconEmoji = icon => ICON_EMOJI[icon] || (icon || '🏷️')
const isImgIcon = icon => {
  if (!icon) return false
  const s = String(icon)
  return s.startsWith('http') || s.startsWith('/') || s.startsWith('data:')
}
/** 用户自建分类只显示用户编号，不暴露昵称（用户数据最小必要） */
const ownerLabel = row => (row.userName && String(row.userName).startsWith('用户#') ? row.userName : '用户#' + row.userId)

function getList() {
  loading.value = true
  const params = { pageNum: query.value.pageNum, pageSize: query.value.pageSize }
  if (query.value.type) params.type = query.value.type
  if (query.value.name) params.name = query.value.name.trim()
  if (query.value.owner) params.owner = query.value.owner
  if (query.value.status === 0 || query.value.status === 1) params.status = query.value.status
  listLedgerCategory(params).then(res => {
    list.value = res.rows || []
    total.value = Number(res.total || 0)
  }).finally(() => { loading.value = false })
}

function handleQuery() {
  query.value.pageNum = 1
  getList()
}
function resetQuery() {
  query.value = { type: '', name: '', owner: '', status: undefined, pageNum: 1, pageSize: 10 }
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
    type: parent ? parent.type : (query.value.type || 'expense'),
    parentId: parent ? parent.id : 0,
    name: '', groupName: parent ? (parent.groupName || '') : '',
    icon: '', iconUrlInput: '', color: '#6a4fd4', sortOrder: 0, status: 1
  }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function openEdit(row) {
  const imgIcon = isImgIcon(row.icon)
  form.value = {
    id: row.id, type: row.type,
    parentId: isTopLevel(row) ? 0 : Number(row.parentId || 0),
    name: row.name, groupName: row.groupName || '',
    icon: imgIcon ? '' : (row.icon || ''), iconUrlInput: imgIcon ? row.icon : '',
    color: row.color || '#6a4fd4',
    sortOrder: row.sortOrder || 0, status: row.status === 0 ? 0 : 1
  }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function onTypeChange() {
  form.value.parentId = 0
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const payload = {
      type: form.value.type,
      parentId: form.value.parentId === 0 ? null : form.value.parentId,
      name: (form.value.name || '').trim(),
      groupName: (form.value.groupName || '').trim() || null,
      icon: (form.value.iconUrlInput || '').trim() || form.value.icon || null,
      color: form.value.color || null,
      sortOrder: form.value.sortOrder || 0,
      status: form.value.status
    }
    submitting.value = true
    const req = form.value.id
      ? updateLedgerCategory(form.value.id, payload)
      : addLedgerCategory(payload)
    req.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}

function onToggle(row) {
  const next = row.status === 1 ? 0 : 1
  const tip = next === 0 ? '停用后 App 端将不再展示该分类（历史流水保留），确认停用？' : '确认启用该分类？'
  proxy.$modal.confirm(tip).then(() => changeLedgerCategoryStatus(row.id, next)).then(() => {
    proxy.$modal.msgSuccess(next === 0 ? '已停用' : '已启用')
    getList()
  }).catch(() => {})
}

function onDelete(row) {
  proxy.$modal.confirm('确认删除分类「' + row.name + '」？已绑定流水或存在子分类时会被拒绝。').then(() => {
    return delLedgerCategory(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.query-form { margin-bottom: 4px; }
.cat-name { display: inline-flex; align-items: center; font-size: 14px; }
.cat-name.is-sub { padding-left: 8px; color: #606266; }
.cat-dot { width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; flex-shrink: 0; }
.ml8 { margin-left: 8px; }
.icon-img { width: 22px; height: 22px; object-fit: cover; border-radius: 4px; }
.icon-emoji { font-size: 18px; }
.opt-emoji { margin-right: 8px; }
.used-zero { color: #c0c4cc; }
.field-hint { font-size: 12px; color: #909399; margin-left: 8px; }
</style>
