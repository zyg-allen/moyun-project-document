<template>
  <div class="app-container">
    <el-alert type="info" :closable="false" class="page-tip"
      title="「我的」页功能入口：显隐/排序/图标/角标/跳转链接均在此配置，App 启动时拉取；隐藏后立即不再下发。内置入口不可删除（可隐藏），自定义入口可删除。" />

    <el-row :gutter="16">
      <!-- 左：配置表 -->
      <el-col :span="16">
        <el-card shadow="never">
          <el-tabs v-model="group" @tab-change="onGroupChange">
            <el-tab-pane label="主功能宫格" name="main" />
            <el-tab-pane label="推荐小功能" name="recommend" />
          </el-tabs>

          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="primary" plain icon="Plus" @click="openAdd"
                v-hasPermi="['cms:ledgerAppFeature:add']">新增入口</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-tag type="info" effect="plain">共 {{ rows.length }} 项，展示 {{ visibleCount }} 项</el-tag>
            </el-col>
            <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
          </el-row>

          <el-table v-loading="loading" :data="rows" size="small" border>
            <el-table-column label="排序" prop="sortNum" width="70" align="center" />
            <el-table-column label="图标" width="80" align="center">
              <template #default="scope">
                <span class="fe-icon" :style="{ background: (scope.row.iconColor || '#7fbf94') + '22', color: scope.row.iconColor || '#7fbf94' }">
                  {{ scope.row.icon || '▦' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="功能名称" min-width="130">
              <template #default="scope">
                {{ scope.row.featureName }}
                <el-tag v-if="scope.row.badge" type="danger" size="small" effect="dark" class="ml6">{{ scope.row.badge }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="标识 / 路由" min-width="180">
              <template #default="scope">
                <div class="fe-key">{{ scope.row.featureKey }}</div>
                <div class="fe-route">{{ routeLabel(scope.row.route) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="上线" width="86" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'done' ? 'success' : 'info'" size="small">
                  {{ scope.row.status === 'done' ? '已上线' : '开发中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="展示" width="80" align="center">
              <template #default="scope">
                <el-switch v-model="scope.row.visible" :active-value="1" :inactive-value="0"
                  :loading="scope.row._switching" @change="onToggleVisible(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" align="center" fixed="right">
              <template #default="scope">
                <el-button link type="primary" icon="Edit" @click="openEdit(scope.row)"
                  v-hasPermi="['cms:ledgerAppFeature:edit']">编辑</el-button>
                <el-tooltip v-if="scope.row.builtin" content="内置入口不可删除，请用「展示」开关控制" placement="top">
                  <span><el-button link type="info" icon="Delete" disabled>删除</el-button></span>
                </el-tooltip>
                <el-button v-else link type="danger" icon="Delete" @click="onDelete(scope.row)"
                  v-hasPermi="['cms:ledgerAppFeature:edit']">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右：实时预览 -->
      <el-col :span="8">
        <el-card shadow="never" class="preview-card">
          <template #header>
            <span>手机预览（仅展示 visible=1 且已上线）</span>
          </template>
          <div class="phone">
            <div class="phone-title">我的</div>
            <div class="phone-grid">
              <div v-for="m in previewList" :key="m.featureKey" class="phone-cell">
                <div class="phone-icon" :style="{ background: (m.iconColor || '#7fbf94') + '22' }">
                  {{ m.icon || '▦' }}
                </div>
                <div class="phone-label">{{ m.featureName }}</div>
                <span v-if="m.badge" class="phone-badge">{{ m.badge }}</span>
              </div>
              <div v-if="!previewList.length" class="phone-empty">当前分组暂无展示项</div>
            </div>
          </div>
          <div class="preview-hint">预览仅反映配置结果，实际布局以 App 版本为准</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="open" :title="dialogTitle" width="560px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="分组" prop="groupType">
          <el-radio-group v-model="form.groupType" :disabled="!!form.id">
            <el-radio label="main">主功能宫格</el-radio>
            <el-radio label="recommend">推荐小功能</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="功能标识" prop="featureKey">
          <el-input v-model="form.featureKey" placeholder="英文标识，如 memo（创建后不可修改）"
            maxlength="40" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="功能名称" prop="featureName">
          <el-input v-model="form.featureName" placeholder="展示给用户的名称" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="跳转路由">
          <el-select v-model="form.route" placeholder="从 App 支持的路由中选择" clearable filterable style="width: 100%">
            <el-option v-for="r in routeOptions" :key="r.value" :label="r.label" :value="r.value">
              <span class="route-opt">{{ r.label }}</span>
              <span class="route-val">{{ r.value }}</span>
            </el-option>
          </el-select>
          <div class="field-hint">只允许已注册路由，避免配置出「点了没反应」的死链</div>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji 或符号，如 📝" maxlength="8" style="width: 140px" />
          <el-color-picker v-model="form.iconColor" style="margin-left: 12px" />
          <span class="field-hint">图标 + 主题色</span>
        </el-form-item>
        <el-form-item label="角标">
          <el-input v-model="form.badge" placeholder="选填，如 NEW / HOT" maxlength="10" style="width: 160px" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNum" :min="0" :max="999" />
          <span class="field-hint">数字越小越靠前</span>
        </el-form-item>
        <el-form-item label="上线状态">
          <el-radio-group v-model="form.status">
            <el-radio label="done">已上线</el-radio>
            <el-radio label="dev">开发中</el-radio>
          </el-radio-group>
          <div class="field-hint">开发中入口建议同时关闭展示</div>
        </el-form-item>
        <el-form-item label="是否展示">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200"
            placeholder="运营备注；链接型入口在此填完整 URL" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LedgerAppFeature">
import {
  listAppFeatures, addAppFeature, updateAppFeature, delAppFeature, getAppFeatureRoutes
} from '@/api/cms/ledger'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const showSearch = ref(true)
const group = ref('main')
const allRows = ref([])
const open = ref(false)
const submitting = ref(false)
const formRef = ref()
const routeOptions = ref([])
const form = ref({})
const rules = {
  featureKey: [
    { required: true, message: '功能标识不能为空', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9]{1,39}$/, message: '仅允许字母开头的字母/数字组合', trigger: 'blur' }
  ],
  featureName: [{ required: true, message: '功能名称不能为空', trigger: 'blur' }],
  groupType: [{ required: true, message: '请选择分组', trigger: 'change' }]
}

const rows = computed(() => allRows.value
  .filter(r => r.groupType === group.value)
  .sort((a, b) => (a.sortNum || 0) - (b.sortNum || 0)))
const visibleCount = computed(() => rows.value.filter(r => r.visible === 1).length)
const previewList = computed(() => rows.value.filter(r => r.visible === 1))
const dialogTitle = computed(() => (form.value.id ? '编辑功能入口' : '新增功能入口'))

const routeLabel = route => {
  if (!route) return '未绑定路由'
  const hit = routeOptions.value.find(r => r.value === route)
  return hit ? hit.label : route
}

function getList() {
  loading.value = true
  listAppFeatures().then(res => {
    allRows.value = (res.data || res.rows || []).map(r => ({ ...r, _switching: false }))
  }).finally(() => { loading.value = false })
}

function getRoutes() {
  getAppFeatureRoutes().then(res => {
    routeOptions.value = res.data || []
  }).catch(() => {})
}

function onGroupChange() { /* 纯前端分组切换，无需重新请求 */ }
function handleQuery() { getList() }

function openAdd() {
  form.value = {
    id: null, featureKey: '', featureName: '', groupType: group.value,
    route: '', icon: '', iconColor: '#7fbf94', badge: '', sortNum: 0,
    status: 'done', visible: 0, remark: ''
  }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function openEdit(row) {
  form.value = {
    id: row.id, featureKey: row.featureKey, featureName: row.featureName,
    groupType: row.groupType, route: row.route || '', icon: row.icon || '',
    iconColor: row.iconColor || '#7fbf94', badge: row.badge || '',
    sortNum: row.sortNum || 0, status: row.status || 'done',
    visible: row.visible === 1 ? 1 : 0, remark: row.remark || ''
  }
  open.value = true
  nextTick(() => formRef.value && formRef.value.clearValidate())
}

function submitForm() {
  formRef.value.validate(valid => {
    if (!valid) return
    const payload = {
      featureKey: (form.value.featureKey || '').trim(),
      featureName: (form.value.featureName || '').trim(),
      groupType: form.value.groupType,
      // 空串=显式清空（后端按「字段存在且为空白」置 NULL），不要转成 null 否则清不掉
      route: (form.value.route || '').trim(),
      icon: (form.value.icon || '').trim(),
      iconColor: form.value.iconColor || '',
      badge: (form.value.badge || '').trim(),
      sortNum: form.value.sortNum || 0,
      status: form.value.status,
      visible: form.value.visible,
      remark: (form.value.remark || '').trim()
    }
    submitting.value = true
    const req = form.value.id ? updateAppFeature(form.value.id, payload) : addAppFeature(payload)
    req.then(() => {
      proxy.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功')
      open.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}

function onToggleVisible(row) {
  row._switching = true
  updateAppFeature(row.id, { visible: row.visible }).then(() => {
    proxy.$modal.msgSuccess(row.visible === 1 ? '已开启展示' : '已隐藏（App 下次拉取即生效）')
  }).catch(() => {
    row.visible = row.visible === 1 ? 0 : 1
  }).finally(() => { row._switching = false })
}

function onDelete(row) {
  proxy.$modal.confirm('确认删除自定义入口「' + row.featureName + '」？删除后 App 端不再展示。')
    .then(() => delAppFeature(row.id)).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    }).catch(() => {})
}

getRoutes()
getList()
</script>

<style scoped>
.page-tip { margin-bottom: 16px; }
.fe-icon {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border-radius: 8px; font-size: 16px;
}
.fe-key { font-size: 13px; color: #303133; }
.fe-route { font-size: 12px; color: #909399; margin-top: 2px; }
.route-opt { display: inline-block; width: 180px; }
.route-val { font-size: 12px; color: #909399; }
.ml6 { margin-left: 6px; }
.field-hint { font-size: 12px; color: #909399; margin-left: 8px; }
.preview-card { position: sticky; top: 16px; }
.phone {
  width: 260px; margin: 0 auto; border: 8px solid #2c2f33; border-radius: 28px;
  background: #f5f6f8; min-height: 380px; padding: 14px 10px;
}
.phone-title { text-align: center; font-size: 13px; color: #606266; margin-bottom: 12px; }
.phone-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px 6px; }
.phone-cell { position: relative; display: flex; flex-direction: column; align-items: center; }
.phone-icon {
  width: 38px; height: 38px; border-radius: 12px; display: flex;
  align-items: center; justify-content: center; font-size: 18px;
}
.phone-label { font-size: 10px; color: #303133; margin-top: 4px; text-align: center; line-height: 1.3; }
.phone-badge {
  position: absolute; top: -4px; right: 2px; background: #f56c6c; color: #fff;
  font-size: 8px; padding: 0 3px; border-radius: 6px; transform: scale(.9);
}
.phone-empty { grid-column: 1 / -1; text-align: center; color: #c0c4cc; font-size: 12px; padding: 30px 0; }
.preview-hint { font-size: 12px; color: #909399; text-align: center; margin-top: 12px; }
</style>
