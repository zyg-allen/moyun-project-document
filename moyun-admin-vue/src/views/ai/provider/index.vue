<template>
  <div class="provider-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-plug-circle-bolt"></i>
      </div>
      <div class="header-content">
        <h2>提供商管理</h2>
        <span class="item-count">共 {{ providerList.length }} 个提供商（{{ enabledCount }} 个启用）</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openCreate" class="create-btn">
          <i class="fa-solid fa-plus"></i> 注册提供商
        </el-button>
      </div>
    </div>

    <!-- 内容容器 -->
    <div class="content-container">
      <el-table :data="providerList" v-loading="loading" style="width: 100%">
        <el-table-column label="编码" prop="code" width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名称" prop="name" min-width="150" show-overflow-tooltip />
        <el-table-column label="API 风格" width="170">
          <template #default="{ row }">
            <el-tag :type="row.apiStyle === 'ollama_native' ? 'warning' : 'primary'" size="small" effect="plain">
              {{ row.apiStyle === 'ollama_native' ? 'Ollama 原生' : 'OpenAI 兼容' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认 Base URL" prop="defaultBaseUrl" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.defaultBaseUrl">{{ row.defaultBaseUrl }}</span>
            <span v-else style="color: #909399;">—</span>
          </template>
        </el-table-column>
        <el-table-column label="流式" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.supportsStreaming ? 'success' : 'info'" size="small" effect="plain">
              {{ row.supportsStreaming ? '支持' : '不支持' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="需要Key" width="90" align="center">
          <template #default="{ row }">
            {{ row.requiresApiKey ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.remark">{{ row.remark }}</span>
            <span v-else style="color: #909399;">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">
              <i class="fa-solid fa-pen-to-square"></i> 编辑
            </el-button>
            <el-button link type="danger" @click="deleteProvider(row)">
              <i class="fa-solid fa-trash-can"></i> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="page-tip">
        <i class="fa-solid fa-circle-info"></i>
        提供商能力元数据（API 风格 / 流式 / 默认地址）全部由此注册表驱动：新增任何 OpenAI 兼容提供商
        （DeepSeek/Moonshot/智谱/Groq 等）只需在此登记并启用，模型配置即可选择，全链路零代码改动。
      </div>
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="editMode ? '编辑提供商' : '注册提供商'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="110px">
        <el-form-item label="提供商编码" required>
          <el-input
            v-model="formData.code"
            placeholder="例如：deepseek（小写唯一，模型配置关联值）"
            :disabled="editMode"
          />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="formData.name" placeholder="例如：DeepSeek" />
        </el-form-item>
        <el-form-item label="API 风格" required>
          <el-select v-model="formData.apiStyle" style="width: 100%">
            <el-option label="OpenAI 兼容端点（绝大多数云厂商）" value="openai_compatible" />
            <el-option label="Ollama 原生 API（本地部署）" value="ollama_native" />
          </el-select>
          <div class="form-tip">模型工厂按此协议分支构建客户端，与提供商名称无关</div>
        </el-form-item>
        <el-form-item label="默认 Base URL">
          <el-input v-model="formData.defaultBaseUrl" placeholder="例如：https://api.deepseek.com/v1" />
          <div class="form-tip">模型配置 Base URL 留空时自动使用此地址兜底</div>
        </el-form-item>
        <el-form-item label="支持流式">
          <el-switch v-model="formData.supportsStreaming" />
          <div class="form-tip">该提供商 chat 模型是否支持流式输出（对话模型流式判定的数据来源）</div>
        </el-form-item>
        <el-form-item label="需要 API Key">
          <el-switch v-model="formData.requiresApiKey" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="formData.enabled" />
          <div class="form-tip">停用后模型配置表单不再展示该提供商</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            {{ editMode ? '更新' : '创建' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const providerList = ref([])
const loading = ref(false)
const showDialog = ref(false)
const editMode = ref(false)
const submitting = ref(false)

const enabledCount = computed(() => providerList.value.filter(p => p.enabled).length)

const defaultForm = () => ({
  id: null,
  code: '',
  name: '',
  apiStyle: 'openai_compatible',
  defaultBaseUrl: '',
  supportsStreaming: true,
  requiresApiKey: true,
  enabled: true,
  sortOrder: 99,
  remark: ''
})
const formData = ref(defaultForm())

const loadProviders = async () => {
  loading.value = true
  try {
    const response = await request({ url: '/cms/ai/provider/list', method: 'get' })
    providerList.value = response.data || []
  } catch (error) {
    console.error('加载提供商列表失败:', error)
    ElMessage.error('加载提供商列表失败')
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  editMode.value = false
  formData.value = defaultForm()
  showDialog.value = true
}

const openEdit = (row) => {
  editMode.value = true
  formData.value = { ...row }
  showDialog.value = true
}

const submitForm = async () => {
  if (!formData.value.code || !formData.value.name) {
    ElMessage.warning('请填写提供商编码与名称')
    return
  }
  submitting.value = true
  try {
    const url = editMode.value ? '/cms/ai/provider/update' : '/cms/ai/provider/create'
    const method = editMode.value ? 'put' : 'post'
    await request({ url, method, data: formData.value })
    ElMessage.success(editMode.value ? '更新成功（即时生效）' : '创建成功')
    showDialog.value = false
    loadProviders()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const toggleEnabled = async (row) => {
  try {
    await request({ url: '/cms/ai/provider/update', method: 'put', data: { ...row } })
    ElMessage.success(row.enabled ? '已启用' : '已停用')
  } catch (error) {
    row.enabled = !row.enabled
    console.error('切换状态失败:', error)
    ElMessage.error('切换状态失败')
  }
}

const deleteProvider = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除提供商「${row.name}」吗？已被模型配置引用时不建议删除，可改为停用。`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await request({ url: `/cms/ai/provider/${row.id}`, method: 'delete' })
    ElMessage.success('删除成功')
    loadProviders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadProviders()
})
</script>

<style scoped>
.provider-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  flex-shrink: 0;
}

.header-content h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.item-count {
  font-size: 13px;
  color: #909399;
}

.header-actions {
  margin-left: auto;
}

.content-container {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.page-tip {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f4f6fa;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.page-tip i {
  color: #667eea;
  margin-right: 6px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.5;
}
</style>
