<template>
  <el-dialog
    v-model="visible"
    title="📚 新增知识库"
    width="750px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-steps :active="currentStep" align-center style="margin-bottom: 30px;">
      <el-step title="选择文件" icon="Upload" />
      <el-step title="配置参数" icon="Setting" />
      <el-step title="完成" icon="Check" />
    </el-steps>

    <!-- 步骤1: 选择文件 -->
    <div v-show="currentStep === 0" class="step-content">
      <el-upload
        ref="uploadRef"
        class="upload-demo"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.jpg,.jpeg,.png,.gif,.bmp"
      >
        <i class="fa-solid fa-cloud-arrow-up" style="font-size: 60px; color: #409eff;"></i>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击选择文件</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word(doc/docx)、Excel、PowerPoint、TXT、MD、CSV、图片等格式，单个文件不超过 50MB
          </div>
        </template>
      </el-upload>

      <div v-if="selectedFile" class="file-info">
        <el-alert
          :title="`已选择: ${selectedFile.name}`"
          type="success"
          :closable="false"
        >
          <template #default>
            <div>文件大小: {{ formatFileSize(selectedFile.size) }}</div>
            <div>文件类型: {{ selectedFile.name.split('.').pop().toUpperCase() }}</div>
          </template>
        </el-alert>
      </div>
    </div>

    <!-- 步骤2: 配置参数 -->
    <div v-show="currentStep === 1" class="step-content">
      <el-tabs v-model="configMode">
        <!-- 快速配置 -->
        <el-tab-pane label=" 快速配置（推荐）" name="template">
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
                    <div class="template-header-right">
                      <!-- 问号图标，点击展开详情 -->
                      <el-tooltip content="查看详细配置说明" placement="top" v-if="getTemplateDetails(template.templateName || template.name)">
                        <i 
                          class="fa-solid fa-circle-question detail-toggle"
                          :class="{ active: expandedTemplateId === template.id }"
                          @click.stop="toggleTemplateDetail(template.id)"
                        ></i>
                      </el-tooltip>
                      <el-tag v-if="template.isRecommended" type="success" size="small">推荐</el-tag>
                    </div>
                  </div>
                  <div class="template-desc">{{ template.templateDesc || template.description }}</div>
                  <!-- 详细技术说明（折叠） -->
                  <transition name="slide-fade">
                    <div class="template-details" v-if="expandedTemplateId === template.id && getTemplateDetails(template.templateName || template.name)">
                      <div class="detail-item" v-for="(detail, idx) in getTemplateDetails(template.templateName || template.name)" :key="idx">
                        <i class="fa-solid fa-check" style="color: #67c23a; margin-right: 6px;"></i>
                        <span>{{ detail }}</span>
                      </div>
                    </div>
                  </transition>
                </div>
              </el-radio>
            </el-radio-group>
          </div>
        </el-tab-pane>

        <!-- 自定义配置 -->
        <el-tab-pane label=" 自定义配置" name="custom">
          <el-form :model="customConfig" label-width="140px" label-position="left">
            <el-form-item label="分段最大长度">
              <el-input-number
                v-model="customConfig.segmentMaxLength"
                :min="100"
                :max="4096"
                :step="100"
                placeholder="400"
              />
              <span style="margin-left: 10px;">字符</span>
              <div style="color: #999; font-size: 12px; margin-top: 4px;">
                推荐400（题库/试卷），800（长文档），根据内容类型调整
              </div>
            </el-form-item>

            <el-form-item label="分段重叠长度">
              <el-input-number
                v-model="customConfig.segmentOverlapLength"
                :min="0"
                :max="500"
                :step="10"
              />
              <span style="margin-left: 10px;">字符</span>
            </el-form-item>

            <el-form-item label="预处理选项">
              <el-checkbox-group v-model="preprocessOptions">
                <el-checkbox label="replaceSpaces">替换连续空格</el-checkbox>
                <el-checkbox label="removeUrls">删除URL</el-checkbox>
                <el-checkbox label="removeNewlines">删除多余换行</el-checkbox>
              </el-checkbox-group>
            </el-form-item>

            <el-form-item label="索引模式">
              <el-radio-group v-model="customConfig.indexMode">
                <el-radio label="high_quality">高质量</el-radio>
                <el-radio label="economy">经济模式</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="Top K">
              <el-input-number
                v-model="customConfig.retrievalTopK"
                :min="1"
                :max="20"
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
        <el-button
          v-if="currentStep === 0"
          type="primary"
          @click="nextStep"
          :disabled="!selectedFile"
        >
          下一步：配置参数
        </el-button>
        <el-button
          v-if="currentStep === 1"
          type="primary"
          @click="handleSubmit"
          :loading="submitting"
        >
          确认创建
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps({
  modelValue: Boolean,
  libraryId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const currentStep = ref(0)
const selectedFile = ref(null)
const uploadRef = ref()
const templates = ref([])
const selectedTemplateId = ref(null)
const configMode = ref('template')
const submitting = ref(false)
const expandedTemplateId = ref(null)  // 当前展开详情的模板ID

// 切换模板详情展开/折叠
const toggleTemplateDetail = (templateId) => {
  if (expandedTemplateId.value === templateId) {
    expandedTemplateId.value = null
  } else {
    expandedTemplateId.value = templateId
  }
}

// 自定义配置（推荐默认值）
const customConfig = reactive({
  segmentMode: 'general',
  segmentMaxLength: 400,  // 推荐400，确保题目能单独检索
  segmentOverlapLength: 50,  // 推荐50，保持上下文连贯
  indexMode: 'high_quality',
  retrievalMode: 'vector',
  retrievalTopK: 10,
  rerankEnabled: false
})

const preprocessOptions = ref(['replaceSpaces', 'removeUrls', 'removeNewlines'])

// 模板详细说明映射
const templateDetailsMap = {
  '经济快速模式': [
    '分段长度500字符，重叠50字符',
    '使用经济索引模式，降低Embedding API调用成本',
    '关闭Rerank重排序，减少计算资源消耗',
    'Top K=5，返回较少结果加快响应速度',
    '适合：大批量文档导入、测试环境、对精度要求不高的场景'
  ],
  '长文档深度模式': [
    '分段长度1200字符，重叠200字符（保留更多上下文）',
    '使用高质量索引模式，确保语义理解准确',
    '启用Rerank重排序，提升检索结果相关性',
    'Top K=8，平衡召回率和精确度',
    '适合：长篇论文、研究报告、小说、法律文书等需要完整上下文的内容'
  ],
  '题库/QA精准模式': [
    '分段长度400字符，重叠50字符（确保每道题独立）',
    '使用QA专用分段模式，智能识别问答对边界',
    '启用Rerank重排序，精准匹配问题与答案',
    'Top K=15，提高题目召回率',
    '适合：题库、FAQ、问答对、试卷、知识问答等短文本场景'
  ],
  '标准文档': [
    '分段长度800字符，重叠100字符（平衡性能与准确度）',
    '使用高质量索引模式，通用性强',
    '启用Rerank重排序，优化检索排序',
    'Top K=10，适中的返回数量',
    '适合：技术手册、产品文档、操作指南等通用文档'
  ],
  '代码技术文档': [
    '分段长度600字符，重叠80字符（保持代码块完整）',
    '使用代码专用分段模式，识别代码边界',
    '关闭Rerank（代码匹配更依赖关键词）',
    'Top K=12，覆盖更多代码片段',
    '适合：API文档、代码注释、技术规范、编程教程'
  ]
}

// 获取模板详细说明
const getTemplateDetails = (templateName) => {
  return templateDetailsMap[templateName] || null
}

// 文件选择
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 下一步
const nextStep = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  // 获取推荐模板
  const fileType = selectedFile.value.name.split('.').pop()
  try {
    const response = await request({ url: `/cms/ai/knowledge-base/templates/recommended?fileType=${fileType}`, method: 'get' })
    templates.value = response.data.templates || []
    if (templates.value.length > 0) {
      const recommended = templates.value.find(t => t.isRecommended)
      selectedTemplateId.value = recommended ? recommended.id : templates.value[0].id
    }
    currentStep.value = 1
  } catch (error) {
    console.error('获取模板失败:', error)
    ElMessage.error('获取配置模板失败')
  }
}

// 提交
const handleSubmit = async () => {
  submitting.value = true

  try {
    // 1. 上传文件
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    // 如果有libraryId，添加到表单中
    if (props.libraryId) {
      formData.append('libraryId', props.libraryId)
    }

    const uploadResponse = await request({ url: '/cms/ai/knowledge-base/upload', method: 'post', data: formData, headers: {
      'Content-Type': 'multipart/form-data'
    } })

    const knowledgeId = uploadResponse.data.knowledgeId

    // 2. 应用配置
    let configData = {
      knowledgeId: knowledgeId,
      startProcessing: true
    }

    if (configMode.value === 'template') {
      configData.templateId = parseInt(selectedTemplateId.value)
    } else {
      configData = {
        ...configData,
        ...customConfig,
        preprocessReplaceSpaces: preprocessOptions.value.includes('replaceSpaces'),
        preprocessRemoveUrls: preprocessOptions.value.includes('removeUrls'),
        preprocessRemoveExtraNewlines: preprocessOptions.value.includes('removeNewlines')
      }
    }

    const configResponse = await request({ url: '/cms/ai/knowledge-base/configure', method: 'post', data: configData})

    ElMessage.success('文档上传成功，正在处理中...')
    emit('success', configResponse.data)
    visible.value = false
    resetDialog()
  } catch (error) {
    console.error('创建失败:', error)
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  resetDialog()
  visible.value = false
}

// 重置对话框
const resetDialog = () => {
  currentStep.value = 0
  selectedFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  templates.value = []
  selectedTemplateId.value = null
  configMode.value = 'template'
}
</script>

<style scoped src="@/views/ai/styles/create-knowledge-dialog.css"></style>
