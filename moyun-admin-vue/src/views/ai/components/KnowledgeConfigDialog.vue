<template>
  <el-dialog
    v-model="visible"
    title="⚙️ 配置知识库处理参数"
    width="700px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="config-container">
      <!-- 文件信息 -->
      <el-alert
        :title="`文件：${fileInfo.fileName} (${fileInfo.fileType.toUpperCase()})`"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <template #default>
          <div>文件大小：{{ formatFileSize(fileInfo.fileSize) }}</div>
          <div>状态：待配置</div>
        </template>
      </el-alert>

      <!-- 配置方式选择 -->
      <el-tabs v-model="configMode" @tab-change="handleModeChange">
        <!-- 快速配置（使用模板） -->
        <el-tab-pane label="📦 快速配置（推荐）" name="template">
          <div class="template-section">
            <el-radio-group v-model="selectedTemplateId" class="template-list">
              <el-radio
                v-for="template in templates"
                :key="template.id"
                :label="template.id"
                class="template-item"
                border
              >
                <div class="template-content">
                  <div class="template-header">
                    <span class="template-name">{{ template.templateName || template.name }}</span>
                    <el-tag v-if="template.isRecommended" type="success" size="small">推荐</el-tag>
                  </div>
                  <div class="template-desc">{{ template.templateDesc || template.description }}</div>
                  <div class="template-stats" v-if="template.useCount">
                    <el-icon><User /></el-icon>
                    已有 {{ template.useCount }} 人使用
                  </div>
                </div>
              </el-radio>
            </el-radio-group>
          </div>
        </el-tab-pane>

        <!-- 自定义配置 -->
        <el-tab-pane label="🔧 自定义配置" name="custom">
          <el-form :model="customConfig" label-width="140px" label-position="left">
            <!-- 分段设置 -->
            <el-divider content-position="left">
              <el-icon><Document /></el-icon>
              分段设置
            </el-divider>
            
            <!-- 🚀 新增：分片策略选择 -->
            <el-form-item label="分片策略">
              <el-radio-group v-model="customConfig.chunkingStrategy">
                <el-radio label="fixed">固定大小</el-radio>
                <el-radio label="document_type">按文档类型</el-radio>
                <el-radio label="adaptive">自适应</el-radio>
              </el-radio-group>
              <div class="form-tip">
                固定大小：使用统一的分片大小 | 按文档类型：根据文档类型使用不同分片大小 | 自适应：自动检测文档类型并调整
              </div>
            </el-form-item>

            <!-- 🚀 新增：文档类型选择（仅在"按文档类型"模式下显示） -->
            <el-form-item label="文档类型" v-if="customConfig.chunkingStrategy === 'document_type'">
              <el-select v-model="customConfig.documentType" placeholder="请选择文档类型">
                <el-option label="通用文档" value="general">
                  <span>通用文档</span>
                  <span style="color: #999; font-size: 12px; margin-left: 10px;">800字符</span>
                </el-option>
                <el-option label="FAQ问答" value="faq">
                  <span>FAQ问答</span>
                  <span style="color: #999; font-size: 12px; margin-left: 10px;">400字符</span>
                </el-option>
                <el-option label="表格文档" value="table">
                  <span>表格文档</span>
                  <span style="color: #999; font-size: 12px; margin-left: 10px;">1500字符</span>
                </el-option>
                <el-option label="代码文档" value="code">
                  <span>代码文档</span>
                  <span style="color: #999; font-size: 12px; margin-left: 10px;">2000字符</span>
                </el-option>
                <el-option label="技术文档" value="technical">
                  <span>技术文档</span>
                  <span style="color: #999; font-size: 12px; margin-left: 10px;">1200字符</span>
                </el-option>
              </el-select>
              <div class="form-tip">
                选择文档类型后，系统会使用对应的推荐分片大小
              </div>
            </el-form-item>
            
            <el-form-item label="分段模式">
              <el-radio-group v-model="customConfig.segmentMode">
                <el-radio label="general">通用模式</el-radio>
                <el-radio label="parent_child">父子分段</el-radio>
              </el-radio-group>
              <div class="form-tip">
                通用模式适合大多数文档，父子分段适合长文档和论文
              </div>
            </el-form-item>

            <!-- 固定大小模式下显示分段长度配置 -->
            <el-form-item label="分段最大长度" v-if="customConfig.chunkingStrategy === 'fixed'">
              <el-input-number
                v-model="customConfig.segmentMaxLength"
                :min="100"
                :max="4096"
                :step="100"
                placeholder="800"
              />
              <div style="color: #999; font-size: 12px; margin-top: 4px;">
                每个文本块的最大字符数。推荐800（通用），400（FAQ），1200（技术文档），1500（表格），2000（代码）
              </div>
            </el-form-item>

            <!-- 🚀 新增：FAQ分片大小配置 -->
            <el-form-item label="FAQ分片大小" v-if="customConfig.chunkingStrategy === 'document_type' && customConfig.documentType === 'faq'">
              <el-input-number
                v-model="customConfig.faqChunkSize"
                :min="200"
                :max="800"
                :step="50"
                placeholder="400"
              />
              <span style="margin-left: 10px;">字符</span>
              <div class="form-tip">推荐300-500字符，确保一个问答对完整</div>
            </el-form-item>

            <!-- 🚀 新增：技术文档分片大小配置 -->
            <el-form-item label="技术文档分片大小" v-if="customConfig.chunkingStrategy === 'document_type' && customConfig.documentType === 'technical'">
              <el-input-number
                v-model="customConfig.technicalChunkSize"
                :min="800"
                :max="2000"
                :step="100"
                placeholder="1200"
              />
              <span style="margin-left: 10px;">字符</span>
              <div class="form-tip">推荐1200-1500字符，保持逻辑完整性</div>
            </el-form-item>

            <el-form-item label="分段重叠长度">
              <el-input-number
                v-model="customConfig.segmentOverlapLength"
                :min="0"
                :max="500"
                :step="10"
              />
              <span style="margin-left: 10px;">字符</span>
              <div class="form-tip">相邻分片之间的重叠字符数，有助于保持上下文连贯性（约15%）</div>
            </el-form-item>

            <!-- 🚀 新增：智能边界检测 -->
            <el-form-item label="智能边界检测">
              <el-switch v-model="customConfig.enableSmartBoundary" />
              <div class="form-tip">
                避免在句子中间切断（表格和代码文档自动禁用）
              </div>
            </el-form-item>

            <!-- 文本预处理 -->
            <el-divider content-position="left">
              <el-icon><Edit /></el-icon>
              文本预处理
            </el-divider>

            <el-form-item label="预处理选项">
              <el-checkbox-group v-model="preprocessOptions">
                <el-checkbox label="replaceSpaces">替换连续空格和制表符</el-checkbox>
                <el-checkbox label="removeUrls">删除URL和邮箱地址</el-checkbox>
                <el-checkbox label="removeNewlines">删除多余换行</el-checkbox>
                <el-checkbox label="removeSpecialChars">删除特殊字符</el-checkbox>
                <el-checkbox label="removeTableDesc">删除表格描述</el-checkbox>
                <el-checkbox label="removeHeaderFooter">删除页眉页脚</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <!-- 索引方式 -->
            <el-divider content-position="left">
              <el-icon><Coin /></el-icon>
              索引方式
            </el-divider>

            <el-form-item label="索引模式">
              <el-radio-group v-model="customConfig.indexMode">
                <el-radio label="high_quality">高质量（推荐）</el-radio>
                <el-radio label="economy">经济模式</el-radio>
              </el-radio-group>
              <div class="form-tip">
                高质量模式：更好的检索效果 | 经济模式：更快的处理速度
              </div>
            </el-form-item>

            <!-- 检索设置 -->
            <el-divider content-position="left">
              <el-icon><Search /></el-icon>
              检索设置
            </el-divider>

            <el-form-item label="检索模式">
              <el-select v-model="customConfig.retrievalMode">
                <el-option label="向量检索" value="vector" />
                <el-option label="关键词检索" value="keyword" />
                <el-option label="混合检索" value="hybrid" />
              </el-select>
            </el-form-item>

            <el-form-item label="Top K">
              <el-input-number
                v-model="customConfig.retrievalTopK"
                :min="1"
                :max="20"
              />
              <div class="form-tip">检索时返回的最相关分片数量</div>
            </el-form-item>

            <el-form-item label="启用重排序">
              <el-switch v-model="customConfig.rerankEnabled" />
              <div class="form-tip">使用重排序模型优化检索结果（需要额外资源）</div>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleConfirm" :loading="processing">
          确认并开始处理
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Edit, Coin, Search, User } from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({
  modelValue: Boolean,
  fileInfo: {
    type: Object,
    required: true
  },
  templates: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 配置模式：template=使用模板，custom=自定义
const configMode = ref('template')
const selectedTemplateId = ref(null)
const processing = ref(false)

// 自定义配置
const customConfig = reactive({
  // 🚀 新增：分片策略配置
  chunkingStrategy: 'fixed',  // fixed=固定大小, document_type=按文档类型, adaptive=自适应
  documentType: 'general',  // general=通用, faq=FAQ, table=表格, code=代码, technical=技术文档
  faqChunkSize: 400,  // FAQ分片大小
  technicalChunkSize: 1200,  // 技术文档分片大小
  enableSmartBoundary: true,  // 启用智能边界检测
  
  segmentMode: 'general',
  segmentMaxLength: 800,  // 800字符，确保题库问答对完整（技术文档可用500，小说可用1500）
  segmentOverlapLength: 100,  // 100字符重叠，保证上下文连贯性
  indexMode: 'high_quality',
  retrievalMode: 'vector',
  retrievalTopK: 10,
  rerankEnabled: false
})

// 预处理选项（用于复选框）
const preprocessOptions = ref(['replaceSpaces', 'removeUrls', 'removeNewlines'])

// 监听模板变化，自动选择推荐模板
watch(() => props.templates, (newTemplates) => {
  if (newTemplates && newTemplates.length > 0) {
    const recommended = newTemplates.find(t => t.isRecommended)
    selectedTemplateId.value = recommended ? recommended.id : newTemplates[0].id
  }
}, { immediate: true })

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 切换配置模式
const handleModeChange = (mode) => {
  console.log('切换到配置模式:', mode)
}

// 确认配置
const handleConfirm = async () => {
  processing.value = true
  
  try {
    let configData = {
      knowledgeId: props.fileInfo.knowledgeId,
      startProcessing: true
    }

    if (configMode.value === 'template') {
      // 使用模板
      if (!selectedTemplateId.value) {
        ElMessage.warning('请选择一个配置模板')
        processing.value = false
        return
      }
      configData.templateId = parseInt(selectedTemplateId.value)
    } else {
      // 自定义配置
      configData = {
        ...configData,
        ...customConfig,
        preprocessReplaceSpaces: preprocessOptions.value.includes('replaceSpaces'),
        preprocessRemoveUrls: preprocessOptions.value.includes('removeUrls'),
        preprocessRemoveExtraNewlines: preprocessOptions.value.includes('removeNewlines'),
        preprocessRemoveSpecialChars: preprocessOptions.value.includes('removeSpecialChars'),
        preprocessRemoveTableDesc: preprocessOptions.value.includes('removeTableDesc'),
        preprocessRemoveHeaderFooter: preprocessOptions.value.includes('removeHeaderFooter')
      }
    }

    console.log('提交配置:', configData)

    const response = await request({ url: '/cms/ai/knowledge-base/configure', method: 'post', data: configData})

    ElMessage.success('配置成功，正在处理...')
    emit('success', response.data)
    visible.value = false

    // 轮询查询状态
    startPollingStatus(props.fileInfo.knowledgeId)
  } catch (error) {
    console.error('配置失败:', error)
    ElMessage.error('配置失败: ' + (error.response?.data?.message || error.message))
  } finally {
    processing.value = false
  }
}

// 轮询查询处理状态
const startPollingStatus = (knowledgeId) => {
  const checkStatus = async () => {
    try {
      const response = await request({ url: `/cms/ai/knowledge-base/status/${knowledgeId}`, method: 'get' })
      const status = response.data.processingStatus
      
      if (status === 'completed') {
        ElMessage.success('处理完成！')
        emit('success', response.data)
      } else if (status === 'failed') {
        ElMessage.error('处理失败: ' + response.data.errorMessage)
      } else if (status === 'processing') {
        // 继续轮询
        setTimeout(checkStatus, 2000)
      }
    } catch (error) {
      console.error('查询状态失败:', error)
    }
  }
  
  // 延迟1秒后开始查询
  setTimeout(checkStatus, 1000)
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}
</script>

<style scoped src="@/views/ai/styles/knowledge-config-dialog.css"></style>
