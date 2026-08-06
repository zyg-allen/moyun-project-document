<template>
  <div class="tool-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-wrench"></i>
      </div>
      <div class="header-content">
        <h2>工具管理</h2>
        <span class="item-count">共 {{ filteredTools.length }} 个工具</span>
      </div>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="搜索名称/描述..." prefix-icon="Search" clearable style="width: 200px" />
        <el-select v-model="searchCategory" placeholder="分类" clearable style="width: 120px">
          <el-option label="网络" value="network" />
          <el-option label="工具" value="utility" />
          <el-option label="数据" value="data" />
        </el-select>
        <el-select v-model="searchStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="已启用" :value="true" />
          <el-option label="已禁用" :value="false" />
        </el-select>
        <el-button type="primary" @click="showTestDialog = true" class="create-btn">
          <i class="fa-solid fa-flask"></i> 测试工具
        </el-button>
      </div>
    </div>

    <!-- 内容容器 -->
    <div class="content-container">

      <!-- 工具卡片列表 -->
      <div class="tool-cards" v-loading="loading">
      <TransitionGroup name="card-list">
        <div 
          v-for="tool in paginatedTools" 
          :key="tool.id" 
          class="tool-card"
        >
          <!-- 卡片顶部装饰条 -->
          <div class="card-accent" :class="{ active: tool.enabled }"></div>
          
          <!-- 卡片主体内容 -->
          <div class="card-content">
            <!-- 头部：图标 + 标题 + 状态 -->
            <div class="card-top">
              <div class="card-icon" :class="{ active: tool.enabled }">
                <i :class="'fa-solid ' + tool.icon"></i>
              </div>
              <div class="card-title-area">
                <div class="card-title-row">
                  <span class="name">{{ tool.displayName }}</span>
                  <el-tag v-if="tool.isSystem" type="info" size="small" effect="plain" round>
                    <i class="fa-solid fa-shield"></i> 内置
                  </el-tag>
                  <el-tag :type="tool.enabled ? 'success' : 'info'" size="small" effect="plain" round class="status-tag">
                    {{ tool.enabled ? '已启用' : '已禁用' }}
                  </el-tag>
                </div>
                <div class="desc">{{ tool.description }}</div>
              </div>
            </div>
            
            <!-- 工具信息标签 -->
            <div class="tool-tags">
              <span class="tool-tag">
                <i class="fa-solid fa-tag"></i>
                {{ getCategoryLabel(tool.category) }}
              </span>
              <span class="tool-tag">
                <i class="fa-solid fa-code"></i>
                {{ getToolTypeLabel(tool.toolType) }}
              </span>
            </div>

            <!-- 底部：元信息 + 操作按钮 -->
            <div class="card-bottom">
              <div class="card-meta">
                <span class="meta-item">
                  <i class="fa-solid fa-signature"></i>
                  <span>{{ tool.name }}</span>
                </span>
              </div>
              <div class="card-actions" @click.stop>
                <el-tooltip content="测试" placement="top" :show-after="200">
                  <button class="action-btn primary" @click="testTool(tool)">
                    <i class="fa-solid fa-play"></i>
                  </button>
                </el-tooltip>
                <el-tooltip :content="tool.enabled ? '禁用' : '启用'" placement="top" :show-after="200">
                  <button class="action-btn" @click="toggleEnabled(tool)" :disabled="tool.isSystem">
                    <i :class="tool.enabled ? 'fa-solid fa-toggle-on' : 'fa-solid fa-toggle-off'"></i>
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
        :total="toolList.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 测试工具对话框 -->
    <el-dialog v-model="showTestDialog" title="🧪 测试工具" width="600px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="选择工具">
          <el-select v-model="testForm.toolName" placeholder="请选择工具" style="width: 100%;">
            <el-option
              v-for="tool in enabledTools"
              :key="tool.name"
              :label="tool.displayName"
              :value="tool.name"
            >
              <div style="display: flex; align-items: center; gap: 8px;">
                <i :class="'fa-solid ' + tool.icon" style="color: #e6a23c;"></i>
                <span>{{ tool.displayName }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="参数" v-if="selectedToolParams">
          <div style="width: 100%;">
            <div v-for="(param, key) in selectedToolParams" :key="key" style="margin-bottom: 10px;">
              <div style="font-size: 13px; color: #606266; margin-bottom: 5px;">
                {{ key }}
                <span style="color: #909399; font-size: 12px;">{{ param.description }}</span>
              </div>
              <el-input v-model="testForm.params[key]" :placeholder="param.default || ''" />
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="executeTest" :loading="testing">
            <i class="fa-solid fa-play"></i> 执行测试
          </el-button>
        </el-form-item>

        <el-form-item label="执行结果" v-if="testResult">
          <el-card shadow="never" style="width: 100%;">
            <div v-if="testResult.success" style="color: #67c23a;">
              <i class="fa-solid fa-check-circle"></i> 执行成功 ({{ testResult.durationMs }}ms)
            </div>
            <div v-else style="color: #f56c6c;">
              <i class="fa-solid fa-times-circle"></i> 执行失败
            </div>
            <el-divider style="margin: 10px 0;" />
            <pre style="white-space: pre-wrap; word-break: break-all; font-size: 13px; margin: 0;">{{ testResult.content || testResult.errorMessage }}</pre>
          </el-card>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const toolList = ref([])
const loading = ref(false)
const showTestDialog = ref(false)
const testing = ref(false)
const testResult = ref(null)

// 搜索相关
const searchKeyword = ref('')
const searchCategory = ref('')
const searchStatus = ref(null)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)

const testForm = ref({
  toolName: '',
  params: {}
})

// 启用的工具列表
const enabledTools = computed(() => toolList.value.filter(t => t.enabled))

// 过滤后的列表
const filteredTools = computed(() => {
  let result = toolList.value
  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(tool =>
      tool.displayName?.toLowerCase().includes(keyword) ||
      tool.name?.toLowerCase().includes(keyword) ||
      tool.description?.toLowerCase().includes(keyword)
    )
  }
  // 分类筛选
  if (searchCategory.value) {
    result = result.filter(tool => tool.category === searchCategory.value)
  }
  // 状态筛选
  if (searchStatus.value !== null && searchStatus.value !== '') {
    result = result.filter(tool => tool.enabled === searchStatus.value)
  }
  return result
})

// 分页后的列表
const paginatedTools = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTools.value.slice(start, end)
})

// 选中工具的参数定义
const selectedToolParams = computed(() => {
  if (!testForm.value.toolName) return null
  const tool = toolList.value.find(t => t.name === testForm.value.toolName)
  if (!tool || !tool.parameters) return null
  try {
    const schema = JSON.parse(tool.parameters)
    return schema.properties || null
  } catch {
    return null
  }
})

// 监听工具选择变化，重置参数
watch(() => testForm.value.toolName, () => {
  testForm.value.params = {}
  testResult.value = null
})

// 加载工具列表
const loadToolList = async () => {
  loading.value = true
  try {
    const response = await request({ url: '/cms/ai/tool/list', method: 'get' })
    toolList.value = response.data || []
  } catch (error) {
    console.error('加载工具列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 切换启用状态
const toggleEnabled = async (tool) => {
  try {
    await request({ url: `/cms/ai/tool/${tool.id}/toggle?enabled=${tool.enabled}`, method: 'put'})
    ElMessage.success(tool.enabled ? '已启用' : '已禁用')
  } catch (error) {
    console.error('切换状态失败:', error)
    tool.enabled = !tool.enabled
    ElMessage.error('操作失败')
  }
}

// 测试工具
const testTool = (tool) => {
  testForm.value.toolName = tool.name
  testForm.value.params = {}
  testResult.value = null
  showTestDialog.value = true
}

// 执行测试
const executeTest = async () => {
  if (!testForm.value.toolName) {
    ElMessage.warning('请选择工具')
    return
  }

  testing.value = true
  testResult.value = null

  try {
    const response = await request({ url: `/cms/ai/tool/test/${testForm.value.toolName}`, method: 'post', data: testForm.value.params})
    testResult.value = response.data
  } catch (error) {
    console.error('测试失败:', error)
    testResult.value = { success: false, errorMessage: error.message }
  } finally {
    testing.value = false
  }
}

// 分类标签
const getCategoryLabel = (category) => {
  const map = {
    'utility': '实用工具',
    'information': '信息查询',
    'action': '执行操作',
    'data': '数据查询',
    'general': '通用'
  }
  return map[category] || category
}

const getCategoryType = (category) => {
  const map = {
    'utility': 'info',
    'information': 'success',
    'action': 'warning',
    'data': 'danger',
    'general': ''
  }
  return map[category] || ''
}

const getToolTypeLabel = (type) => {
  const map = {
    'builtin': '内置',
    'http': 'HTTP',
    'database': '数据库'
  }
  return map[type] || type
}

onMounted(() => {
  loadToolList()
})
</script>

<style scoped src="@/views/ai/styles/tool-manage.css"></style>
