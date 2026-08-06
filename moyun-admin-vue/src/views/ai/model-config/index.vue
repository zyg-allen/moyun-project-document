<template>
  <div class="model-config-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-microchip"></i>
      </div>
      <div class="header-content">
        <h2>模型配置</h2>
        <span class="item-count">共 {{ filteredModels.length }} 个模型</span>
      </div>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="搜索名称/模型..." prefix-icon="Search" clearable style="width: 200px" />
        <el-select v-model="searchType" placeholder="类型" clearable style="width: 120px">
          <el-option label="对话模型" value="chat" />
          <el-option label="向量模型" value="embedding" />
        </el-select>
        <el-select v-model="searchProvider" placeholder="提供商" clearable style="width: 120px">
          <el-option label="OpenAI" value="openai" />
          <el-option label="通义千问" value="dashscope" />
          <el-option label="Ollama" value="ollama" />
        </el-select>
        <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
          <i class="fa-solid fa-plus"></i> 新建配置
        </el-button>
      </div>
    </div>

    <!-- 内容容器 -->
    <div class="content-container">

      <!-- 模型配置卡片列表 -->
      <div class="model-cards" v-loading="loading">
      <TransitionGroup name="card-list">
        <div 
          v-for="model in paginatedModels" 
          :key="model.id" 
          class="model-card"
        >
          <!-- 卡片顶部装饰条 -->
          <div class="card-accent" :class="{ active: model.enabled }"></div>
          
          <!-- 卡片主体内容 -->
          <div class="card-content">
            <!-- 头部：图标 + 标题 + 状态 -->
            <div class="card-top">
              <div class="card-icon" :class="{ active: model.enabled }">
                <i class="fa-solid fa-microchip"></i>
              </div>
              <div class="card-title-area">
                <div class="card-title-row">
                  <el-tooltip :content="model.name" placement="top" :show-after="200">
                    <span class="name">{{ model.name }}</span>
                  </el-tooltip>
                  <el-tag v-if="model.isDefault" type="warning" size="small" effect="plain" round>
                    <i class="fa-solid fa-star"></i> 默认
                  </el-tag>
                  <el-tag :type="model.enabled ? 'success' : 'info'" size="small" effect="plain" round class="status-tag">
                    {{ model.enabled ? '已启用' : '已禁用' }}
                  </el-tag>
                </div>
                <el-tooltip :content="getProviderName(model.provider) + ' · ' + model.modelName" placement="top" :show-after="200">
                  <div class="desc">{{ getProviderName(model.provider) }} · {{ model.modelName }}</div>
                </el-tooltip>
              </div>
            </div>
            
            <!-- 模型信息标签 -->
            <div class="model-tags">
              <span class="model-tag">
                <i class="fa-solid fa-tag"></i>
                {{ model.modelType === 'embedding' ? '向量模型' : '对话模型' }}
              </span>
              <span class="model-tag">
                <i class="fa-solid fa-temperature-half"></i>
                温度 {{ model.temperature }}
              </span>
              <span class="model-tag" v-if="model.inputPrice !== null && model.inputPrice !== undefined">
                <i class="fa-solid fa-coins"></i>
                ¥{{ model.inputPrice }}/{{ model.outputPrice || 0 }}
              </span>
            </div>

            <!-- 底部：元信息 + 操作按钮 -->
            <div class="card-bottom">
              <div class="card-meta">
                <span class="meta-item" v-if="model.baseUrl">
                  <i class="fa-solid fa-link"></i>
                  <el-tooltip :content="model.baseUrl" placement="top" :show-after="200">
                    <span>{{ model.baseUrl.substring(0, 30) }}...</span>
                  </el-tooltip>
                </span>
              </div>
              <div class="card-actions" @click.stop>
                <el-tooltip content="编辑" placement="top" :show-after="200">
                  <button class="action-btn" @click="editConfig(model)">
                    <i class="fa-solid fa-pen-to-square"></i>
                  </button>
                </el-tooltip>
                <el-tooltip v-if="!model.isDefault" content="设为默认" placement="top" :show-after="200">
                  <button class="action-btn" @click="setDefault(model.id)">
                    <i class="fa-solid fa-star"></i>
                  </button>
                </el-tooltip>
                <el-tooltip content="测试连接" placement="top" :show-after="200">
                  <button class="action-btn primary" @click="testConnection(model)">
                    <i class="fa-solid fa-plug"></i>
                  </button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top" :show-after="200">
                  <button class="action-btn danger" @click="deleteConfig(model.id)" :disabled="model.isDefault">
                    <i class="fa-solid fa-trash-can"></i>
                  </button>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>
      </TransitionGroup>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 36, 48]"
        :total="filteredModels.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editMode ? '编辑模型配置' : '新建模型配置'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form :model="formData" label-width="120px">
        <el-form-item label="配置名称" required>
          <el-input v-model="formData.name" placeholder="例如：通义千问-Plus" />
        </el-form-item>

        <el-form-item label="模型提供商" required>
          <el-select v-model="formData.provider" placeholder="选择提供商" style="width: 100%">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Ollama (本地)" value="ollama" />
            <el-option label="通义千问 (Dashscope)" value="dashscope" />
          </el-select>
        </el-form-item>

        <el-form-item label="模型类型" required>
          <el-select v-model="formData.modelType" placeholder="选择模型类型" style="width: 100%">
            <el-option label="对话模型 (Chat)" value="chat">
              <span>对话模型 (Chat)</span>
              <span style="color: #909399; font-size: 12px; margin-left: 10px;">用于智能体对话</span>
            </el-option>
            <el-option label="向量模型 (Embedding)" value="embedding">
              <span>向量模型 (Embedding)</span>
              <span style="color: #909399; font-size: 12px; margin-left: 10px;">用于文档向量化</span>
            </el-option>
          </el-select>
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            对话模型用于智能体对话，向量模型用于知识库文档的向量化处理
          </div>
        </el-form-item>

        <el-form-item label="模型名称" required>
          <el-input v-model="formData.modelName" placeholder="例如：qwen-plus, gpt-4, text-embedding-v3" />
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            <span v-if="formData.modelType === 'chat'">对话模型：qwen-plus, gpt-4, deepseek-r1:1.5b 等</span>
            <span v-else-if="formData.modelType === 'embedding'">向量模型：text-embedding-v3, text-embedding-3-large, nomic-embed-text 等</span>
            <span v-else>根据提供商和类型填写对应的模型名称</span>
          </div>
        </el-form-item>

        <el-form-item label="API Key" v-if="formData.provider !== 'ollama'">
          <el-input 
            v-model="formData.apiKey" 
            type="password" 
            show-password
            placeholder="输入API密钥"
          />
        </el-form-item>

        <el-form-item label="Base URL">
          <el-input v-model="formData.baseUrl" placeholder="留空使用默认地址" />
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            OpenAI: https://api.openai.com/v1<br>
            Ollama: http://localhost:11434<br>
            自定义: 填写完整URL
          </div>
        </el-form-item>

        <el-form-item label="温度参数">
          <el-slider v-model="formData.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>

        <el-form-item label="最大Token数">
          <el-input-number v-model="formData.maxTokens" :min="100" :max="32000" :step="100" />
        </el-form-item>

        <el-form-item label="超时时间(秒)">
          <el-input-number v-model="formData.timeout" :min="10" :max="300" :step="10" />
        </el-form-item>

        <el-divider content-position="left">💰 价格配置（元/1000 tokens）</el-divider>

        <el-form-item label="输入价格">
          <el-input-number v-model="formData.inputPrice" :min="0" :max="1" :step="0.0001" :precision="6" />
        </el-form-item>

        <el-form-item label="输出价格">
          <el-input-number v-model="formData.outputPrice" :min="0" :max="1" :step="0.0001" :precision="6" />
        </el-form-item>

        <el-form-item label="流式输出">
          <el-switch v-model="formData.streamingSupported" />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="formData.enabled" />
        </el-form-item>

        <el-form-item label="备注说明">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="2"
            placeholder="选填"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
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

const configList = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const editMode = ref(false)
const submitting = ref(false)

// 搜索相关
const searchKeyword = ref('')
const searchType = ref('')
const searchProvider = ref('')

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)

// 过滤后的列表
const filteredModels = computed(() => {
  let result = configList.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(model => 
      model.name?.toLowerCase().includes(keyword) ||
      model.modelName?.toLowerCase().includes(keyword)
    )
  }
  if (searchType.value) {
    result = result.filter(model => model.modelType === searchType.value)
  }
  if (searchProvider.value) {
    result = result.filter(model => model.provider === searchProvider.value)
  }
  return result
})

const formData = ref({
  id: null,
  name: '',
  provider: 'dashscope',
  modelType: 'chat',  // 默认为对话模型
  modelName: '',
  apiKey: '',
  baseUrl: '',
  temperature: 0.7,
  maxTokens: 2000,
  timeout: 60,
  streamingSupported: true,
  enabled: true,
  description: '',
  inputPrice: 0.001,
  outputPrice: 0.002
})

// 加载配置列表
const loadConfigList = async () => {
  loading.value = true
  try {
    const response = await request({ url: '/cms/ai/model-config/list', method: 'get' })
    // 后端返回的是 ListResponse 格式：{ list: [], total: n }
    configList.value = response.data?.list || []
  } catch (error) {
    console.error('加载配置列表失败:', error)
    ElMessage.error('加载配置列表失败')
  } finally {
    loading.value = false
  }
}

// 编辑配置
const editConfig = (config) => {
  editMode.value = true
  formData.value = { ...config }
  showCreateDialog.value = true
}

// 提交表单
const submitForm = async () => {
  if (!formData.value.name || !formData.value.provider || !formData.value.modelName) {
    ElMessage.warning('请填写必填项')
    return
  }

  if (formData.value.provider !== 'ollama' && !formData.value.apiKey) {
    ElMessage.warning('请填写API Key')
    return
  }

  submitting.value = true
  try {
    const url = editMode.value ? '/cms/ai/model-config/update' : '/cms/ai/model-config/create'
    const method = editMode.value ? 'put' : 'post'

    const response = await request({ url, method, data: formData.value })

    ElMessage.success(editMode.value ? '更新成功' : '创建成功')
    showCreateDialog.value = false
    loadConfigList()
    resetForm()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// 删除配置
const deleteConfig = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个配置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await request({ url: `/cms/ai/model-config/${id}`, method: 'delete'})
    ElMessage.success('删除成功')
    loadConfigList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 设为默认
const setDefault = async (id) => {
  try {
    const response = await request({ url: `/cms/ai/model-config/set-default/${id}`, method: 'post'})
    ElMessage.success('设置成功')
    loadConfigList()
  } catch (error) {
    console.error('设置默认失败:', error)
    ElMessage.error('设置失败')
  }
}

// 测试连接
const testConnection = async (config) => {
  try {
    ElMessage.info('测试连接中...')
    const response = await request({ url: '/cms/ai/model-config/test', method: 'post', data: config})
    ElMessage.success('连接测试成功')
  } catch (error) {
    console.error('测试连接失败:', error)
    ElMessage.error('连接测试失败')
  }
}

// 重置表单
const resetForm = () => {
  editMode.value = false
  formData.value = {
    id: null,
    name: '',
    provider: 'dashscope',
    modelType: 'chat',
    modelName: '',
    apiKey: '',
    baseUrl: '',
    temperature: 0.7,
    maxTokens: 2000,
    timeout: 60,
    streamingSupported: true,
    enabled: true,
    description: '',
    inputPrice: 0.001,
    outputPrice: 0.002
  }
}

// 获取提供商类型
const getProviderType = (provider) => {
  const typeMap = {
    'openai': 'primary',
    'ollama': 'success',
    'dashscope': 'warning'
  }
  return typeMap[provider] || 'info'
}

// 分页数据（基于过滤后的列表）
const paginatedModels = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredModels.value.slice(start, end)
})

// 获取提供商名称
const getProviderName = (provider) => {
  const nameMap = {
    'openai': 'OpenAI',
    'ollama': 'Ollama',
    'dashscope': '通义千问'
  }
  return nameMap[provider] || provider
}

onMounted(() => {
  loadConfigList()
})
</script>

<style scoped src="@/views/ai/styles/model-config-manage.css"></style>
