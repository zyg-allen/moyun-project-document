<template>
  <div class="workflow-ai-generator">
    <!-- 触发按钮（在对话框外使用） -->
    <!--    <slot name="trigger">-->
    <!--      <el-button type="primary" @click="visible = true" class="ai-btn">-->
    <!--        <i class="fa-solid fa-wand-magic-sparkles"></i> AI生成工作流-->
    <!--      </el-button>-->
    <!--    </slot>-->

    <!-- 生成对话框 -->
    <el-dialog
      v-model="visible"
      width="700px"
      :close-on-click-modal="false"
      class="ai-generator-dialog"
    >
      <template #header>
        <div class="dialog-title">
          <i class="fa-solid fa-wand-magic-sparkles"></i>
          <span>AI智能生成工作流</span>
        </div>
      </template>
      <div class="generator-content">
        <!-- 顶部工具栏 -->
        <div class="top-toolbar" v-if="!generating && !result">
          <el-button
            link
            type="primary"
            @click="showHistory = !showHistory"
            v-if="generationHistory.length > 0"
          >
            <i class="fa-solid fa-clock-rotate-left"></i>
            历史记录 ({{ generationHistory.length }})
          </el-button>
        </div>

        <!-- 历史记录面板 -->
        <div class="history-panel" v-if="showHistory && generationHistory.length > 0">
          <div class="history-header">
            <span><i class="fa-solid fa-history"></i> 最近生成</span>
            <el-button link type="primary" @click="showHistory = false">收起</el-button>
          </div>
          <div class="history-list">
            <div
              v-for="item in generationHistory.slice(0, 5)"
              :key="item.id"
              class="history-item"
              @click="useHistoryItem(item)"
            >
              <div class="history-desc">{{ item.description }}</div>
              <div class="history-meta">
                <span class="history-time">{{ item.timestamp }}</span>
                <span class="history-nodes">{{ item.nodeCount }} 个节点</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-section">
          <div class="section-header">
            <i class="fa-solid fa-keyboard"></i>
            <span>描述你想要的工作流</span>
            <el-tag v-if="analyzing" size="small" type="info">
              <i class="fa-solid fa-spinner fa-spin"></i> 分析中...
            </el-tag>
          </div>
          <el-input
            v-model="description"
            type="textarea"
            :rows="5"
            :placeholder="placeholderText"
            :disabled="generating"
          />
          <div class="char-count">{{ description.length }} 字符</div>
        </div>

        <!-- 智能分析结果 -->
        <div class="analysis-section" v-if="analysisResult && !generating && !result">
          <div class="analysis-card">
            <div class="analysis-header">
              <i class="fa-solid fa-brain"></i>
              <span>智能分析</span>
            </div>
            <div class="analysis-content">
              <div class="analysis-metrics">
                <div class="metric-item">
                  <span class="metric-label">预估复杂度</span>
                  <el-tag
                    :type="analysisResult.complexity === '简单' ? 'success' : analysisResult.complexity === '中等' ? 'warning' : 'danger'"
                    size="small"
                  >
                    {{ analysisResult.complexity }}
                  </el-tag>
                </div>
                <div class="metric-item">
                  <span class="metric-label">预计节点数</span>
                  <span class="metric-value">{{ analysisResult.estimatedNodes }} 个</span>
                </div>
              </div>
              <div class="analysis-features" v-if="analysisResult.hasCondition || analysisResult.hasKnowledge || analysisResult.hasLoop">
                <span class="feature-label">识别到的功能：</span>
                <el-tag v-if="analysisResult.hasCondition" size="small" type="warning">条件分支</el-tag>
                <el-tag v-if="analysisResult.hasKnowledge" size="small" type="primary">知识库</el-tag>
                <el-tag v-if="analysisResult.hasLoop" size="small" type="danger">循环处理</el-tag>
              </div>
              <div class="analysis-suggestions" v-if="analysisResult.suggestions.length > 0">
                <div v-for="(suggestion, idx) in analysisResult.suggestions" :key="idx" class="suggestion-item">
                  {{ suggestion }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 示例区域（分类展示） -->
        <div class="examples-section" v-if="!result && !generating">
          <div class="section-header">
            <i class="fa-solid fa-lightbulb"></i>
            <span>快速开始</span>
            <el-button link type="primary" @click="showAllExamples = !showAllExamples">
              {{ showAllExamples ? '收起分类' : '查看分类' }}
            </el-button>
          </div>

          <!-- 分类展示 -->
          <template v-if="showAllExamples">
            <div v-for="(examples, category) in exampleCategories" :key="category" class="example-category">
              <div class="category-title">
                <i class="fa-solid fa-folder-open"></i>
                {{ category }}
              </div>
              <div class="category-examples">
                <div
                  v-for="(example, idx) in examples"
                  :key="idx"
                  class="example-card"
                  @click="useExample(example)"
                >
                  <div class="example-text">{{ example.text }}</div>
                  <div class="example-info">
                    <el-tag size="small" :type="example.complexity === '简单' ? 'success' : example.complexity === '中等' ? 'warning' : 'danger'">
                      {{ example.complexity }}
                    </el-tag>
                    <span class="example-nodes">{{ example.nodes }} 个节点</span>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- 简单展示 -->
          <div v-else class="example-chips">
            <div
              v-for="(example, index) in displayExamples.slice(0, 4)"
              :key="index"
              class="example-chip"
              @click="useExample(example)"
            >
              <i class="fa-solid fa-quote-left"></i>
              {{ example }}
            </div>
          </div>
        </div>

        <!-- 提示区域 -->
        <div class="tips-section" v-if="!generating && !result">
          <div class="tip-item">
            <i class="fa-solid fa-robot"></i>
            <span>可以说「用<strong>XX智能体</strong>处理」自动关联智能体</span>
          </div>
          <div class="tip-item">
            <i class="fa-solid fa-database"></i>
            <span>可以说「从<strong>XX知识库</strong>查询」自动关联知识库</span>
          </div>
          <div class="tip-item">
            <i class="fa-solid fa-code-branch"></i>
            <span>支持条件分支：「如果...则...否则...」</span>
          </div>
        </div>

        <!-- 生成中状态 -->
        <div class="generating-section" v-if="generating">
          <div class="generating-animation">
            <i class="fa-solid fa-wand-magic-sparkles fa-bounce"></i>
          </div>
          <div class="generating-text">AI正在设计工作流...</div>
          <div class="generating-steps">
            <div class="step" :class="{ active: currentStep >= 1 }">
              <i class="fa-solid fa-check-circle" v-if="currentStep > 1"></i>
              <i class="fa-solid fa-spinner fa-spin" v-else-if="currentStep === 1"></i>
              <i class="fa-regular fa-circle" v-else></i>
              <span>分析需求</span>
            </div>
            <div class="step" :class="{ active: currentStep >= 2 }">
              <i class="fa-solid fa-check-circle" v-if="currentStep > 2"></i>
              <i class="fa-solid fa-spinner fa-spin" v-else-if="currentStep === 2"></i>
              <i class="fa-regular fa-circle" v-else></i>
              <span>设计节点</span>
            </div>
            <div class="step" :class="{ active: currentStep >= 3 }">
              <i class="fa-solid fa-check-circle" v-if="currentStep > 3"></i>
              <i class="fa-solid fa-spinner fa-spin" v-else-if="currentStep === 3"></i>
              <i class="fa-regular fa-circle" v-else></i>
              <span>生成连接</span>
            </div>
          </div>
        </div>

        <!-- 结果展示 -->
        <div class="result-section" v-if="result && !generating">
          <div class="result-header" :class="{ success: result.success, error: !result.success }">
            <i :class="result.success ? 'fa-solid fa-circle-check' : 'fa-solid fa-circle-xmark'"></i>
            <span>{{ result.success ? '工作流生成成功！' : '生成失败' }}</span>
          </div>

          <template v-if="result.success">
            <div class="result-info">
              <div class="info-item">
                <span class="label">工作流名称</span>
                <span class="value">{{ result.workflowName }}</span>
              </div>
              <div class="info-item">
                <span class="label">节点数量</span>
                <span class="value">{{ result.nodeCount }} 个</span>
              </div>
            </div>

            <div class="result-description" v-if="result.workflowDescription">
              <span class="label">描述：</span>
              {{ result.workflowDescription }}
            </div>

            <div class="result-explanation" v-if="result.explanation">
              <div class="explanation-content" v-html="formatExplanation(result.explanation)"></div>
            </div>
          </template>

          <template v-else>
            <div class="error-message">
              {{ result.errorMessage }}
            </div>
          </template>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleClose">取消</el-button>
          <el-button @click="reset" v-if="result">重新生成</el-button>
          <el-button
            type="primary"
            @click="handleGenerate"
            :loading="generating"
            :disabled="!description.trim()"
          >
            <i class="fa-solid fa-wand-magic-sparkles" v-if="!generating"></i>
            {{ generating ? '生成中...' : (result?.success ? '应用到编辑器' : '开始生成') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const emit = defineEmits(['generated', 'apply'])

// 状态
const visible = ref(false)
const description = ref('')
const generating = ref(false)
const currentStep = ref(0)
const result = ref(null)
const showAllExamples = ref(false)
const inputMode = ref('simple') // simple | guided
const guidedStep = ref(1)
const guidedData = ref({ goal: '', steps: [], conditions: [] })
const analyzing = ref(false)
const analysisResult = ref(null)
const showHistory = ref(false)
const generationHistory = ref([])
const smartSuggestions = ref([])

// 示例列表（按分类组织）
const exampleCategories = {
  '客服助理': [
    { text: '智能客服：判断问题类型→知识库查询→AI回答', complexity: '中等', nodes: 5 },
    { text: '投诉处理：情绪分析→严重度判断→自动分类→人工提醒', complexity: '高', nodes: 6 },
    { text: '常见问答：FAQ匹配→标准回复', complexity: '简单', nodes: 3 }
  ],
  '内容处理': [
    { text: '文章生成：大纲设计→分段撰写→整合润色', complexity: '中等', nodes: 4 },
    { text: '内容审核：敏感词检测→合规判断→通过/拒绝', complexity: '简单', nodes: 4 },
    { text: '翻译流程：检测语言→翻译→质量检查', complexity: '简单', nodes: 4 }
  ],
  '数据分析': [
    { text: '销售报告：数据获取→趋势分析→图表生成→报告输出', complexity: '高', nodes: 7 },
    { text: '用户画像：数据收集→特征提取→分类标签', complexity: '中等', nodes: 5 }
  ],
  '自动化任务': [
    { text: '邮件处理：分类→重要邮件提取→自动回复', complexity: '中等', nodes: 5 },
    { text: '定时提醒：条件检查→内容生成→发送通知', complexity: '简单', nodes: 3 }
  ]
}

const examples = Object.values(exampleCategories).flat().map(e => e.text)

const displayExamples = computed(() =>
  showAllExamples.value ? examples : examples.slice(0, 3)
)

const placeholderText = computed(() => {
  if (smartSuggestions.value.length > 0) {
    return '💡 建议：' + smartSuggestions.value[0]
  }
  return `详细描述您的需求，例如：
• 创建一个翻译流程：接收用户输入，检测语言，翻译成英文后返回
• 客服流程：判断问题类型，技术问题从知识库查询，其他直接AI回答
• 内容审核：检测敏感内容，有问题拒绝并说明原因，无问题通过

💡 提示：说得越详细，生成的工作流越精准`
})

// 使用示例
const useExample = (example) => {
  description.value = typeof example === 'string' ? example : example.text
  // 触发智能分析
  analyzeInput()
}

// 智能分析输入
const analyzeInput = async () => {
  if (!description.value.trim() || description.value.length < 10) {
    analysisResult.value = null
    return
  }

  analyzing.value = true

  // 模拟分析（实际应该调用API）
  setTimeout(() => {
    const text = description.value.toLowerCase()
    const hasCondition = /如果|判断|条件|分支/.test(text)
    const hasKnowledge = /知识库|检索|查询/.test(text)
    const hasLoop = /循环|遍历|批量/.test(text)
    const hasMultiStep = /先|然后|接着|最后/.test(text)

    // 预估复杂度和节点数
    let estimatedNodes = 2 // 至少有开始和结束
    let complexity = '简单'

    if (hasMultiStep) estimatedNodes += 2
    if (hasCondition) { estimatedNodes += 2; complexity = '中等' }
    if (hasKnowledge) { estimatedNodes += 1; complexity = '中等' }
    if (hasLoop) { estimatedNodes += 2; complexity = '高' }

    if (estimatedNodes > 6) complexity = '高'
    else if (estimatedNodes > 4) complexity = '中等'

    analysisResult.value = {
      estimatedNodes,
      complexity,
      hasCondition,
      hasKnowledge,
      hasLoop,
      suggestions: generateSuggestions(text, hasCondition, hasKnowledge, hasLoop)
    }

    analyzing.value = false
  }, 500)
}

// 生成智能建议
const generateSuggestions = (text, hasCondition, hasKnowledge, hasLoop) => {
  const suggestions = []

  if (!hasCondition && (text.includes('问题') || text.includes('分类'))) {
    suggestions.push('💡 建议添加条件判断来处理不同类型的情况')
  }

  if (!hasKnowledge && (text.includes('回答') || text.includes('查询'))) {
    suggestions.push('💡 考虑使用知识库节点来提供更准确的答案')
  }

  if (text.includes('多个') && !hasLoop) {
    suggestions.push('💡 需要处理多个项目时，建议使用循环节点')
  }

  if (text.length < 30) {
    suggestions.push('💡 描述越详细，生成的工作流越符合需求')
  }

  return suggestions
}

// 监听输入变化
watch(description, () => {
  analyzeInput()
})

// 格式化说明
const formatExplanation = (text) => {
  return text
    .replace(/## (.*)/g, '<h4>$1</h4>')
    .replace(/### (.*)/g, '<h5>$1</h5>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

// 保存到历史记录
const saveToHistory = (generatedData) => {
  const historyItem = {
    id: Date.now(),
    description: description.value,
    timestamp: new Date().toLocaleString('zh-CN'),
    ...generatedData
  }

  generationHistory.value.unshift(historyItem)

  // 只保留最近10条
  if (generationHistory.value.length > 10) {
    generationHistory.value = generationHistory.value.slice(0, 10)
  }

  // 保存到localStorage
  try {
    localStorage.setItem('ai_workflow_history', JSON.stringify(generationHistory.value))
  } catch (e) {
    console.warn('保存历史记录失败:', e)
  }
}

// 从历史记录加载
const loadHistory = () => {
  try {
    const saved = localStorage.getItem('ai_workflow_history')
    if (saved) {
      generationHistory.value = JSON.parse(saved)
    }
  } catch (e) {
    console.warn('加载历史记录失败:', e)
  }
}

// 使用历史记录
const useHistoryItem = (item) => {
  description.value = item.description
  showHistory.value = false
}

// 生成工作流
const handleGenerate = async () => {
  if (result.value?.success) {
    // 已有结果，应用到编辑器
    emit('apply', result.value)
    handleClose()
    return
  }

  if (!description.value.trim()) {
    ElMessage.warning('请输入工作流描述')
    return
  }

  generating.value = true
  currentStep.value = 1
  result.value = null

  // 模拟步骤进度
  const stepInterval = setInterval(() => {
    if (currentStep.value < 3) {
      currentStep.value++
    }
  }, 1500)

  try {
    const response = await request({
      url: '/cms/ai/workflow-generator/generate',
      method: 'post',
      data: { description: description.value }
    })

    clearInterval(stepInterval)
    currentStep.value = 3

    console.log('AI生成响应:', response)

    // request 拦截器保证 code===200 才到这里，response.data 即 GenerateResult
    const data = response.data
    if (data && data.success) {
      result.value = {
        success: true,
        workflowName: data.workflowName || '生成的工作流',
        workflowDescription: data.workflowDescription || '',
        nodeCount: data.nodeCount || 0,
        graphData: data.graphData,
        explanation: data.explanation || ''
      }

      if (result.value.graphData && result.value.nodeCount > 0) {
        emit('generated', result.value)
        ElMessage.success(`工作流生成成功！包含 ${result.value.nodeCount} 个节点`)
        // 保存到历史记录
        saveToHistory(result.value)
      } else {
        result.value.success = false
        result.value.errorMessage = '生成的工作流数据不完整'
        ElMessage.error(result.value.errorMessage)
      }
    } else {
      result.value = {
        success: false,
        errorMessage: (data && data.errorMessage) || response.msg || '生成失败'
      }
      ElMessage.error(result.value.errorMessage)
    }
  } catch (error) {
    clearInterval(stepInterval)
    console.error('生成失败:', error)
    result.value = {
      success: false,
      errorMessage: error.message || '网络错误'
    }
    // request 拦截器已统一错误提示，这里不重复 ElMessage
  } finally {
    generating.value = false
  }
}

// 组件挂载时加载历史
watch(visible, (newVal) => {
  if (newVal) {
    loadHistory()
  }
})

// 重置
const reset = () => {
  result.value = null
  currentStep.value = 0
}

// 关闭
const handleClose = () => {
  visible.value = false
  // 延迟重置状态
  setTimeout(() => {
    result.value = null
    currentStep.value = 0
  }, 300)
}

// 暴露方法供父组件调用
defineExpose({
  open: () => { visible.value = true },
  close: handleClose
})
</script>

<style scoped>
.workflow-ai-generator {
  display: inline-block;
}

.ai-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;

  &:hover {
    background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
  }
}

.generator-content {
  min-height: 300px;
}

/* 顶部工具栏 */
.top-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

/* 历史记录面板 */
.history-panel {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 20px;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
  color: #303133;

  i {
    color: #667eea;
    margin-right: 6px;
  }
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  padding: 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e4e7ed;

  &:hover {
    border-color: #667eea;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
    transform: translateX(4px);
  }
}

.history-desc {
  font-size: 13px;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.5;
}

.history-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.history-nodes {
  color: #667eea;
  font-weight: 500;
}

/* 智能分析区域 */
.analysis-section {
  margin-bottom: 20px;
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.analysis-card {
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border: 1px solid #667eea30;
  border-radius: 12px;
  padding: 16px;
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 600;
  color: #667eea;

  i {
    font-size: 16px;
  }
}

.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.analysis-metrics {
  display: flex;
  gap: 24px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 8px;

  .metric-label {
    font-size: 13px;
    color: #606266;
  }

  .metric-value {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
}

.analysis-features {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;

  .feature-label {
    font-size: 13px;
    color: #606266;
  }
}

.analysis-suggestions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.suggestion-item {
  padding: 8px 12px;
  background: #fff;
  border-left: 3px solid #fbbf24;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-weight: 500;
  color: #303133;

  i {
    color: #667eea;
  }
}

.input-section {
  margin-bottom: 20px;
  position: relative;

  :deep(.el-textarea__inner) {
    font-size: 14px;
    line-height: 1.6;

    &::placeholder {
      color: #c0c4cc;
    }
  }

  .char-count {
    position: absolute;
    right: 10px;
    bottom: 8px;
    font-size: 12px;
    color: #909399;
  }
}

.examples-section {
  margin-bottom: 20px;

  .section-header {
    justify-content: space-between;
  }
}

.example-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.example-chip {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: flex-start;
  gap: 6px;

  i {
    color: #c0c4cc;
    font-size: 10px;
    margin-top: 3px;
  }

  &:hover {
    background: #e8f4ff;
    color: #409eff;
    transform: translateY(-2px);

    i {
      color: #409eff;
    }
  }
}

/* 分类示例 */
.example-category {
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.category-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 8px 8px 0 0;
  font-size: 14px;
  font-weight: 600;

  i {
    font-size: 14px;
  }
}

.category-examples {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 8px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 0 0 8px 8px;
}

.example-card {
  padding: 12px;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e4e7ed;

  &:hover {
    border-color: #667eea;
    box-shadow: 0 2px 12px rgba(102, 126, 234, 0.15);
    transform: translateY(-2px);
  }
}

.example-text {
  font-size: 13px;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.5;
}

.example-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.example-nodes {
  font-size: 12px;
  color: #909399;
}

.tips-section {
  background: #fafafa;
  border-radius: 8px;
  padding: 12px 16px;

  .tip-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
    font-size: 13px;
    color: #606266;

    i {
      width: 16px;
      color: #909399;
    }

    strong {
      color: #667eea;
    }
  }
}

.generating-section {
  text-align: center;
  padding: 40px 20px;

  .generating-animation {
    font-size: 48px;
    color: #667eea;
    margin-bottom: 16px;
  }

  .generating-text {
    font-size: 16px;
    color: #303133;
    margin-bottom: 24px;
  }

  .generating-steps {
    display: flex;
    justify-content: center;
    gap: 40px;

    .step {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #c0c4cc;
      font-size: 14px;

      i {
        font-size: 16px;
      }

      &.active {
        color: #667eea;
      }

      .fa-check-circle {
        color: #67c23a;
      }
    }
  }
}

.result-section {
  .result-header {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    border-radius: 8px;
    margin-bottom: 16px;
    font-size: 15px;
    font-weight: 500;

    &.success {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.error {
      background: #fef0f0;
      color: #f56c6c;
    }

    i {
      font-size: 20px;
    }
  }

  .result-info {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;

    .info-item {
      .label {
        color: #909399;
        font-size: 13px;
        margin-right: 8px;
      }

      .value {
        color: #303133;
        font-weight: 500;
      }
    }
  }

  .result-description {
    padding: 12px 16px;
    background: #f5f7fa;
    border-radius: 8px;
    font-size: 14px;
    color: #606266;
    margin-bottom: 16px;

    .label {
      color: #909399;
    }
  }

  .result-explanation {
    padding: 16px;
    background: #fafafa;
    border-radius: 8px;
    max-height: 200px;
    overflow-y: auto;

    .explanation-content {
      font-size: 13px;
      color: #606266;
      line-height: 1.8;

      :deep(h4) {
        font-size: 14px;
        color: #303133;
        margin: 12px 0 8px 0;

        &:first-child {
          margin-top: 0;
        }
      }

      :deep(h5) {
        font-size: 13px;
        color: #606266;
        margin: 8px 0 4px 0;
      }

      :deep(strong) {
        color: #667eea;
      }
    }
  }

  .error-message {
    padding: 16px;
    background: #fef0f0;
    border-radius: 8px;
    color: #f56c6c;
    font-size: 14px;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 对话框标题 */
.dialog-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.dialog-title i {
  color: #8b5cf6;
  font-size: 20px;
}

/* 对话框样式覆盖 */
:deep(.ai-generator-dialog) {
  .el-dialog__header {
    padding: 20px 24px 16px;
    border-bottom: 1px solid #f0f0f0;

    .el-dialog__title {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .el-dialog__body {
    padding: 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
