<template>
  <div class="intelligent-query">
    <div class="query-header">
      <h2>🤖 AI智能数据分析</h2>
      <p class="subtitle">AI理解你的意图，自动识别统计/分析/浏览类查询，实事求是地生成SQL并智能分析数据</p>
    </div>

    <!-- 数据源选择 -->
    <el-card class="datasource-card" shadow="never">
      <div class="datasource-selector">
        <label>选择数据源:</label>
        <el-select 
          v-model="selectedDatasource" 
          placeholder="请选择数据源"
          style="width: 300px"
          @change="onDatasourceChange">
          <el-option
            v-for="ds in datasources"
            :key="ds.id"
            :label="`${ds.name} (${ds.type})`"
            :value="ds.id" />
        </el-select>
        <transition name="status-fade">
          <div v-if="selectedDatasource" :class="['connection-status', connectionStatus]">
            <div class="status-indicator" :class="connectionStatus"></div>
            <span class="status-text">{{ connectionStatusText }}</span>
          </div>
        </transition>
      </div>
    </el-card>

    <!-- 查询输入区 -->
    <el-card class="query-card" shadow="never">
      <div class="query-input-area">
        <el-input
          v-model="queryText"
          type="textarea"
          :rows="6"
          placeholder="💡 请用自然语言提问，AI会理解你的意图并生成SQL：&#10;&#10;📊 统计分析（全表查询，实事求是）：&#10;- 用户总数是多少&#10;- 男性有多少人，女性有多少人&#10;- 张三出现了几次&#10;- 每个部门的人数统计&#10;- 分析最近30天的注册趋势&#10;&#10;📝 数据浏览（智能限制返回数量）：&#10;- 查看用户数据&#10;- 显示最近20条订单&#10;- 查询销售额最高的10个产品&#10;&#10;✨ 支持使用中文表名和字段名，AI自动识别意图！"
          @keydown.ctrl.enter="executeQuery" />

        <div class="query-actions">
          <div class="quick-queries">
            <div class="queries-section">
              <span class="label">💡 快速开始：</span>
              <el-button
                v-for="example in displayedExamples"
                :key="example"
                size="small"
                @click="queryText = example">
                {{ example }}
              </el-button>
            </div>
            
            <!-- 查询历史 -->
            <div v-if="queryHistory.length > 0" class="queries-section history-section">
              <span class="label">🕐 最近查询：</span>
              <el-button
                v-for="(history, index) in queryHistory.slice(0, 3)"
                :key="'history-' + index"
                size="small"
                type="info"
                plain
                @click="useHistoryQuery(history.naturalQuery)">
                {{ history.naturalQuery }}
              </el-button>
            </div>
          </div>

          <div class="action-buttons">
            <el-checkbox v-model="needAnalysis">🤖 AI智能分析</el-checkbox>
            <el-button @click="openTemplates">
              <el-icon><FolderOpened /></el-icon>
              模板库
            </el-button>
            <el-button 
              type="primary" 
              @click="executeQuery"
              :loading="querying"
              :disabled="!selectedDatasource || !queryText">
              <el-icon><Search /></el-icon>
              执行查询 (Ctrl+Enter)
            </el-button>
          </div>

          <!-- 查询进度提示 -->
          <div v-if="querying" class="query-progress">
            <el-progress 
              :percentage="queryProgressPercent" 
              :status="queryProgressPercent === 100 ? 'success' : ''"
              :stroke-width="8" />
            <div class="progress-info">
              <span class="progress-text">{{ queryProgress }}</span>
              <el-button 
                v-if="queryProgressPercent < 100"
                size="small" 
                type="danger" 
                text
                @click="cancelQuery">
                <el-icon><Close /></el-icon>
                取消查询
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 结果展示区 -->
    <div v-if="queryResult" class="result-area">
      <!-- 生成的SQL -->
      <el-card class="sql-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span><el-icon><Document /></el-icon> 生成的SQL</span>
            <el-button size="small" @click="copySQL">
              <el-icon><CopyDocument /></el-icon>
              复制
            </el-button>
          </div>
        </template>
        <pre class="sql-code">{{ queryResult.generatedSql }}</pre>
        <div class="sql-meta">
          <el-tag size="small">{{ queryResult.queryType }}</el-tag>
          <span class="exec-time">执行时间: {{ queryResult.executionTime }}ms</span>
          <span class="result-count">结果: {{ queryResult.totalCount }} 条</span>
        </div>
      </el-card>

      <!-- AI智能分析 -->
      <el-card v-if="queryResult.analysis" class="analysis-card" shadow="never">
        <template #header>
          <div class="analysis-header">
            <span class="analysis-title">
              <el-icon class="analysis-icon"><TrendCharts /></el-icon>
              AI 数据洞察分析
            </span>
            <el-tag type="success" size="small" effect="plain">智能生成</el-tag>
          </div>
        </template>
        <div class="analysis-content" v-html="formatAnalysis(queryResult.analysis)"></div>
      </el-card>

      <!-- 数据洞察 -->
      <el-card v-if="queryResult.insights && queryResult.insights.length > 0" 
               class="insights-card" shadow="never">
        <template #header>
          <span>📈 数据洞察</span>
        </template>
        <div class="insights-list">
          <div 
            v-for="(insight, index) in queryResult.insights"
            :key="index"
            class="insight-item"
            :class="`severity-${insight.severity}`">
            <div class="insight-header">
              <el-tag :type="getSeverityType(insight.severity)" size="small">
                {{ insight.severity }}
              </el-tag>
              <span class="insight-title">{{ insight.title }}</span>
            </div>
            <p class="insight-desc">{{ insight.description }}</p>
            <p v-if="insight.recommendation" class="insight-recommendation">
              💡 建议: {{ insight.recommendation }}
            </p>
          </div>
        </div>
      </el-card>

      <!-- 数据可视化 -->
      <SmartChart 
        v-if="queryResult.data && queryResult.data.length > 0 && shouldShowChart"
        :data="queryResult.data"
        :columns="queryResult.columns" />

      <!-- 数据表格 -->
      <el-card class="data-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>
              <el-icon><Grid /></el-icon> 
              数据结果 ({{ queryResult.totalCount }} 条)
              <el-tag v-if="queryResult.totalCount >= 1000" type="warning" size="small" style="margin-left: 10px;">
                数据较多，已显示前1000条
              </el-tag>
            </span>
            <el-dropdown @command="handleExport">
              <el-button size="small">
                <el-icon><Download /></el-icon>
                导出数据
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="csv">
                    <el-icon><Document /></el-icon>
                    导出为 CSV
                  </el-dropdown-item>
                  <el-dropdown-item command="excel">
                    <el-icon><Document /></el-icon>
                    导出为 Excel
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </template>
        <el-table 
          :data="queryResult.data" 
          stripe 
          border
          max-height="400"
          style="width: 100%">
          <el-table-column
            v-for="col in queryResult.columns"
            :key="col.columnName"
            :prop="col.columnName"
            :label="col.comment || col.columnName"
            :min-width="120">
            <template #header>
              <div style="display: flex; flex-direction: column; align-items: center;">
                <span style="font-weight: 600;">{{ col.comment || col.columnName }}</span>
                <span v-if="col.comment" style="font-size: 11px; color: #909399; margin-top: 2px;">{{ col.columnName }}</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty 
      v-if="!queryResult && !querying"
      description="请选择数据源并输入查询问题"
      :image-size="200" />
    
    <!-- 查询模板库 -->
    <QueryTemplates ref="templatesRef" @use-template="handleUseTemplate" />
  </div>
</template>

<script setup>
import cache from '@/plugins/cache'

import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Search, Document, CopyDocument, Star, 
  Grid, Download, TrendCharts, ArrowDown, FolderOpened, Close
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { v4 as uuidv4 } from 'uuid'
import SmartChart from '../components/SmartChart.vue'
import QueryTemplates from '../components/QueryTemplates.vue'

const route = useRoute()

const datasources = ref([])
const selectedDatasource = ref(null)
const queryText = ref('')
const needAnalysis = ref(true)
const querying = ref(false)
const queryResult = ref(null)
const queryProgress = ref('')
const queryProgressPercent = ref(0)
const abortController = ref(null)

// 连接状态：testing(测试中), connected(已连接), failed(连接失败)
const connectionStatus = ref(null)
const connectionStatusText = computed(() => {
  switch(connectionStatus.value) {
    case 'testing':
      return '测试连接中...'
    case 'connected':
      return '已连接'
    case 'failed':
      return '连接失败'
    default:
      return ''
  }
})

// 会话ID - 用于多轮对话
const sessionId = ref(null)

const queryExamples = ref([
  '用户总数是多少',
  '男性有多少人',
  '每个部门有多少人',
  '分析最近30天的注册趋势',
  '查看用户数据',
  '最近20条订单'
])

// 动态查询建议
const dynamicSuggestions = ref([])

// 查询历史
const queryHistory = ref([])
const showHistory = ref(false)

// 是否显示图表（数据适合可视化）
const shouldShowChart = computed(() => {
  if (!queryResult.value || !queryResult.value.data || queryResult.value.data.length === 0) {
    return false
  }
  
  // 至少2条数据才显示图表
  if (queryResult.value.data.length < 2) {
    return false
  }
  
  // 至少2列才有可视化意义
  if (!queryResult.value.columns || queryResult.value.columns.length < 2) {
    return false
  }
  
  return true
})

// 根据数据源类型决定显示的示例数量
const displayedExamples = computed(() => {
  const currentDatasource = datasources.value.find(ds => ds.id === selectedDatasource.value)
  const isElasticsearch = currentDatasource && currentDatasource.type === 'elasticsearch'
  
  // ES数据源：显示4个示例（避免换行）
  // MySQL数据源：显示4个示例
  return queryExamples.value.slice(0, 4)
})

onMounted(() => {
  // 生成会话ID用于多轮对话
  sessionId.value = uuidv4()
  console.log('会话ID已生成:', sessionId.value)
  
  loadDataSources()
})

// 监听数据源变化，生成动态建议和加载历史
watch(selectedDatasource, async (newVal) => {
  if (newVal) {
    await generateDynamicSuggestions(newVal)
    await loadQueryHistory(newVal)
  }
})

const loadDataSources = async () => {
  try {
    const res = await request({ url: '/cms/ai/data-analysis/datasources', method: 'get' })
    datasources.value = (res.data || []).filter(ds => ds.enabled)
    
    // 检查是否有路由参数传入的数据源ID
    const datasourceId = route.query.datasourceId
    const tableName = route.query.tableName
    const tableComment = route.query.tableComment
    const queryHint = route.query.queryHint
    
    if (datasourceId && datasources.value.some(ds => ds.id === Number(datasourceId))) {
      selectedDatasource.value = Number(datasourceId)
      // 测试连接
      await testConnection()
      
      if (tableName) {
        // 自动填充查询文本，优先使用智能提示
        queryText.value = queryHint || `查询${tableComment || tableName}的数据`
        const displayName = tableComment || tableName
        ElMessage.success({
          message: `已自动选择数据源，可以直接使用"${displayName}"来查询，无需记忆英文表名！`,
          duration: 5000
        })
      }
    }
    // 默认不自动选择数据源，让用户手动选择
  } catch (error) {
    ElMessage.error('加载数据源失败')
  }
}

const onDatasourceChange = async () => {
  queryResult.value = null
  
  if (!selectedDatasource.value) {
    connectionStatus.value = null
    return
  }
  
  // 测试连接
  await testConnection()
}

// 测试数据源连接
const testConnection = async () => {
  connectionStatus.value = 'testing'
  
  try {
    const response = await request({ url: '/cms/ai/data-analysis/datasource/test-connection', method: 'post', data: {
      id: selectedDatasource.value
    }})

    connectionStatus.value = 'connected'
    ElMessage.success('数据源连接成功')
  } catch (error) {
    connectionStatus.value = 'failed'
    ElMessage.error(`连接失败：${error.message}`)
    console.error('测试连接失败:', error)
  }
}

const useExample = (example) => {
  queryText.value = example
}

const executeQuery = async () => {
  if (!selectedDatasource.value) {
    ElMessage.warning('请先选择数据源')
    return
  }

  if (!queryText.value.trim()) {
    ElMessage.warning('请输入查询问题')
    return
  }

  querying.value = true
  queryResult.value = null
  queryProgressPercent.value = 0
  
  // 创建新的AbortController用于取消请求
  abortController.value = new AbortController()
  
  // 模拟进度更新
  queryProgress.value = '🔍 正在分析您的问题...'
  queryProgressPercent.value = 20
  
  // 延迟一下让用户看到进度
  await new Promise(resolve => setTimeout(resolve, 300))

  try {
    queryProgress.value = '🤖 正在生成SQL...'
    queryProgressPercent.value = 40
    
    const res = await request({ url: '/cms/ai/data-analysis/query', method: 'post', data: {
      datasourceId: selectedDatasource.value,
      query: queryText.value,
      needAnalysis: needAnalysis.value,
      needChart: false,
      sessionId: sessionId.value  // 使用会话ID支持多轮对话
    }, signal: abortController.value.signal })
    
    queryProgress.value = '📊 正在处理结果...'
    queryProgressPercent.value = 80

    if (res.data) {
      queryResult.value = res.data

      // 调试日志
      console.log('查询结果:', {
        totalCount: res.data?.totalCount,
        dataLength: res.data?.data?.length,
        columns: res.data?.columns?.length,
        sql: res.data?.generatedSql
      })

      // 检查是否有数据
      queryProgress.value = '✅ 查询完成'
      queryProgressPercent.value = 100

      if (res.data?.data?.length === 0) {
        ElMessage.warning(`查询成功，但未找到数据（共${res.data.totalCount}条）`)
      } else {
        ElMessage.success(`查询成功，返回${res.data?.totalCount || 0}条数据`)

        // 保存成功的查询到本地历史
        saveQueryToLocalHistory(queryText.value)
      }
    }
  } catch (error) {
    // 区分取消和其他错误
    if (error.name === 'CanceledError' || error.code === 'ERR_CANCELED') {
      queryProgress.value = '🚫 查询已取消'
      ElMessage.info('查询已取消')
    } else {
      queryProgress.value = '❌ 查询失败'
      ElMessage.error('查询失败: ' + (error.response?.data?.message || error.message))
    }
  } finally {
    setTimeout(() => {
      querying.value = false
      queryProgressPercent.value = 0
      abortController.value = null
    }, 500)
  }
}

// 取消查询
const cancelQuery = () => {
  if (abortController.value) {
    abortController.value.abort()
    ElMessage.info('正在取消查询...')
  }
}

const copySQL = () => {
  navigator.clipboard.writeText(queryResult.value.generatedSql)
  ElMessage.success('SQL已复制')
}

const handleExport = (format) => {
  if (!queryResult.value || !queryResult.value.data || queryResult.value.data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  const loadingMsg = ElMessage({
    message: '正在准备导出...',
    type: 'info',
    duration: 0,
    icon: 'Loading'
  })

  // 使用setTimeout让加载提示有时间显示
  setTimeout(() => {
    try {
      if (format === 'csv') {
        exportToCSV()
      } else if (format === 'excel') {
        exportToExcel()
      }
    } finally {
      loadingMsg.close()
    }
  }, 100)
}

const exportToCSV = () => {
  try {
    const columns = queryResult.value.columns
    const data = queryResult.value.data

    // 构建CSV内容
    let csvContent = '\ufeff' // UTF-8 BOM

    // 添加表头（使用注释或字段名）
    const headers = columns.map(col => col.comment || col.columnName)
    csvContent += headers.join(',') + '\n'

    // 添加数据行
    data.forEach(row => {
      const values = columns.map(col => {
        const value = row[col.columnName]
        // 处理包含逗号、引号、换行的值
        if (value === null || value === undefined) {
          return ''
        }
        const strValue = String(value)
        if (strValue.includes(',') || strValue.includes('"') || strValue.includes('\n')) {
          return '"' + strValue.replace(/"/g, '""') + '"'
        }
        return strValue
      })
      csvContent += values.join(',') + '\n'
    })

    // 创建下载链接
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `query_result_${Date.now()}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('CSV导出成功')
  } catch (error) {
    console.error('导出CSV失败:', error)
    ElMessage.error('导出失败: ' + error.message)
  }
}

const exportToExcel = async () => {
  try {
    // 动态导入xlsx库
    const XLSX = await import('xlsx')

    const columns = queryResult.value.columns
    const data = queryResult.value.data

    // 构建工作表数据
    const wsData = []

    // 添加表头
    const headers = columns.map(col => col.comment || col.columnName)
    wsData.push(headers)

    // 添加数据行
    data.forEach(row => {
      const rowData = columns.map(col => {
        const value = row[col.columnName]
        return value === null || value === undefined ? '' : value
      })
      wsData.push(rowData)
    })

    // 创建工作表
    const ws = XLSX.utils.aoa_to_sheet(wsData)

    // 设置列宽
    const colWidths = columns.map(() => ({ wch: 15 }))
    ws['!cols'] = colWidths

    // 创建工作簿
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, 'Query Result')

    // 导出文件
    XLSX.writeFile(wb, `query_result_${Date.now()}.xlsx`)

    ElMessage.success('Excel导出成功')
  } catch (error) {
    console.error('导出Excel失败:', error)
    if (error.message && error.message.includes('Cannot find module')) {
      ElMessage.error('请先安装 xlsx 库: npm install xlsx')
    } else {
      ElMessage.error('导出失败: ' + error.message)
    }
  }
}

const formatAnalysis = (text) => {
  return text.replace(/\n/g, '<br>')
}

const getSeverityType = (severity) => {
  const map = {
    'low': 'info',
    'medium': 'warning',
    'high': 'danger'
  }
  return map[severity] || 'info'
}

const generateSessionId = () => {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

// 加载查询历史
const loadQueryHistory = async (datasourceId) => {
  try {
    const res = await request({ url: `/cms/ai/query-history/datasource/${datasourceId}`, method: 'get', params: { limit: 5 } })
    if (res.data) {
      queryHistory.value = res.data.filter(h => h.status === 'success')
    }
  } catch (error) {
    console.error('加载查询历史失败:', error)
  }
}

// 使用历史查询
const useHistoryQuery = (query) => {
  queryText.value = query
  showHistory.value = false
}

// 保存查询到本地历史
const saveQueryToLocalHistory = (query) => {
  try {
    const key = `query_history_${selectedDatasource.value}`
    const history = JSON.parse(cache.local.get(key) || '[]')
    
    // 去重
    const filtered = history.filter(q => q !== query)
    
    // 添加到开头
    filtered.unshift(query)
    
    // 最多保存10条
    const limited = filtered.slice(0, 10)
    
    cache.local.set(key, JSON.stringify(limited))
  } catch (error) {
    console.error('保存查询历史失败:', error)
  }
}

// 生成动态查询建议
const generateDynamicSuggestions = async (datasourceId) => {
  try {
    const res = await request({ url: `/cms/ai/data-analysis/datasources/${datasourceId}/tables/info`, method: 'get' })
    if (res.data && res.data.length > 0) {
      const tables = res.data
      const suggestions = []
      
      // 为每个表生成2-3个查询建议
      tables.slice(0, 3).forEach(table => {
        const tableName = table.tableComment || table.tableName
        suggestions.push(`查看${tableName}的前20条数据`)
        suggestions.push(`统计${tableName}的总数`)
        if (table.rowCount && table.rowCount > 0) {
          suggestions.push(`分析${tableName}的数据分布`)
        }
      })
      
      dynamicSuggestions.value = suggestions
      
      // 合并固定示例和动态建议
      // ES类型数据源减少示例数量，避免按钮换行
      if (suggestions.length > 0) {
        const currentDatasource = datasources.value.find(ds => ds.id === datasourceId)
        const isElasticsearch = currentDatasource && currentDatasource.type === 'elasticsearch'
        
        if (isElasticsearch) {
          // ES数据源：显示2个动态建议 + 2个固定示例
          queryExamples.value = [...suggestions.slice(0, 2), ...queryExamples.value.slice(0, 2)]
        } else {
          // MySQL数据源：显示3个动态建议 + 3个固定示例
          queryExamples.value = [...suggestions.slice(0, 3), ...queryExamples.value.slice(0, 3)]
        }
      }
    }
  } catch (error) {
    console.error('生成动态建议失败:', error)
  }
}

// 模板库相关
const templatesRef = ref(null)

const openTemplates = () => {
  if (templatesRef.value) {
    templatesRef.value.open()
  }
}

const handleUseTemplate = (templateQuery) => {
  queryText.value = templateQuery
  ElMessage.success('模板已应用，可以修改后执行查询')
}
</script>

<style scoped>
.intelligent-query {
  padding: 40px 20px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
}

.intelligent-query::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1440 320"><path fill="%23ffffff" fill-opacity="0.05" d="M0,96L48,112C96,128,192,160,288,186.7C384,213,480,235,576,213.3C672,192,768,128,864,128C960,128,1056,192,1152,197.3C1248,203,1344,149,1392,122.7L1440,96L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z"></path></svg>') no-repeat bottom;
  background-size: cover;
  pointer-events: none;
}

.query-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
  z-index: 1;
}

.query-header h2 {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #fff 0%, #f0f0f0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 15px;
  text-shadow: 0 4px 20px rgba(0,0,0,0.1);
  animation: fadeInDown 0.6s ease-out;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.subtitle {
  color: rgba(255, 255, 255, 0.95);
  font-size: 16px;
  font-weight: 400;
  text-shadow: 0 2px 10px rgba(0,0,0,0.2);
  animation: fadeInUp 0.6s ease-out 0.2s both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.datasource-card,
.query-card {
  margin-bottom: 24px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.15);
  transition: all 0.3s ease;
  animation: fadeInScale 0.6s ease-out 0.3s both;
  position: relative;
  z-index: 1;
}

@keyframes fadeInScale {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.datasource-card:hover,
.query-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 48px rgba(31, 38, 135, 0.25);
}

.datasource-selector {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px 0;
}

.datasource-selector label {
  font-weight: 600;
  color: #4a5568;
  font-size: 15px;
}

/* 连接状态显示优化 */
.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 20px;
  animation: statusSlideIn 0.3s ease-out;
  transition: all 0.3s ease;
}

/* 测试连接中 - 蓝色 */
.connection-status.testing {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 已连接 - 绿色 */
.connection-status.connected {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}

/* 连接失败 - 红色 */
.connection-status.failed {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.status-indicator {
  width: 8px;
  height: 8px;
  background: #fff;
  border-radius: 50%;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.8);
}

/* 测试中 - 旋转动画 */
.status-indicator.testing {
  animation: statusRotate 1s linear infinite;
}

/* 已连接 - 呼吸动画 */
.status-indicator.connected {
  animation: statusPulse 2s ease-in-out infinite;
}

/* 连接失败 - 闪烁动画 */
.status-indicator.failed {
  animation: statusBlink 0.8s ease-in-out infinite;
}

.status-text {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* 状态指示灯动画 */
@keyframes statusPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
}

/* 旋转动画 - 测试连接中 */
@keyframes statusRotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 闪烁动画 - 连接失败 */
@keyframes statusBlink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

/* 状态出现动画 */
@keyframes statusSlideIn {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 过渡动画 */
.status-fade-enter-active {
  transition: all 0.3s ease-out;
}

.status-fade-leave-active {
  transition: all 0.2s ease-in;
}

.status-fade-enter-from {
  opacity: 0;
  transform: translateX(-10px) scale(0.9);
}

.status-fade-leave-to {
  opacity: 0;
  transform: translateX(10px) scale(0.9);
}

.query-input-area {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.query-input-area :deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 2px solid #e2e8f0;
  font-size: 15px;
  line-height: 1.6;
  transition: all 0.3s ease;
  background: #f8fafc;
}

.query-input-area :deep(.el-textarea__inner):focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  background: #fff;
}

.query-actions {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 15px;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: wrap;
}

.query-progress {
  width: 100%;
  flex-basis: 100%;
  margin-top: 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.progress-text {
  color: #667eea;
  font-weight: 600;
  font-size: 14px;
}

.quick-queries {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.queries-section {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.history-section {
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.quick-queries .label {
  color: #718096;
  font-size: 14px;
  font-weight: 600;
  min-width: 90px;
}

.quick-queries :deep(.el-button) {
  border-radius: 20px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
  transition: all 0.3s ease;
}

.quick-queries :deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
  border-color: #667eea;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 15px;
}

.action-buttons :deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 25px;
  padding: 12px 30px;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
}

.action-buttons :deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(102, 126, 234, 0.5);
}

.result-area {
  margin-top: 30px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: fadeInUp 0.6s ease-out;
  position: relative;
  z-index: 1;
}

.result-area :deep(.el-card) {
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.12);
  transition: all 0.3s ease;
}

.result-area :deep(.el-card:hover) {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(31, 38, 135, 0.18);
}

.result-area :deep(.el-card__header) {
  background: linear-gradient(135deg, #fafbfc 0%, #f3f4f6 100%);
  border-bottom: 2px solid #e5e7eb;
  padding: 18px 24px;
  border-radius: 16px 16px 0 0;
}

.result-area :deep(.el-card__body) {
  padding: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-weight: 600;
  font-size: 16px;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header .el-icon {
  font-size: 18px;
  color: #667eea;
}

.sql-code {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  padding: 24px;
  border-radius: 12px;
  font-family: 'Fira Code', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
  overflow-x: auto;
  margin: 0;
  color: #10b981;
  box-shadow: inset 0 2px 12px rgba(0,0,0,0.4), 0 1px 3px rgba(0,0,0,0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.sql-meta {
  display: flex;
  gap: 20px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  font-size: 13px;
  color: #64748b;
  flex-wrap: wrap;
}

.sql-meta .exec-time,
.sql-meta .result-count {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.sql-meta :deep(.el-tag) {
  border-radius: 12px;
  padding: 5px 12px;
  font-weight: 600;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #1e293b;
}

.analysis-icon {
  color: #667eea;
  font-size: 18px;
}

.analysis-content {
  line-height: 2;
  color: #2d3748;
  font-size: 15px;
  padding: 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px;
  border-left: 4px solid #667eea;
  box-shadow: inset 0 1px 3px rgba(0,0,0,0.05);
}

.analysis-content :deep(p) {
  margin: 12px 0;
}

.analysis-content :deep(strong) {
  color: #667eea;
  font-weight: 600;
}

.insights-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.insight-item {
  padding: 18px 20px;
  border-radius: 12px;
  border-left: 4px solid #667eea;
  background: linear-gradient(135deg, #ebf4ff 0%, #e0e7ff 100%);
  transition: all 0.3s ease;
  cursor: pointer;
}

.insight-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.2);
}

.insight-item.severity-high {
  border-left-color: #f56c6c;
  background: linear-gradient(135deg, #fee 0%, #fdd 100%);
}

.insight-item.severity-high:hover {
  box-shadow: 0 4px 20px rgba(245, 108, 108, 0.2);
}

.insight-item.severity-medium {
  border-left-color: #e6a23c;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
}

.insight-item.severity-medium:hover {
  box-shadow: 0 4px 20px rgba(230, 162, 60, 0.2);
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.insight-title {
  font-weight: 600;
  color: #1e293b;
  font-size: 15px;
}

.insight-desc {
  margin: 0;
  color: #475569;
  line-height: 1.8;
  font-size: 14px;
}

.insight-recommendation {
  margin: 10px 0 0 0;
  color: #10b981;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 12px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 8px;
  display: inline-block;
}

.chart-container {
  width: 100%;
  height: 400px;
}

/* 数据表格优化 */
.data-card :deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

.data-card :deep(.el-table th) {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #1e293b;
  font-weight: 600;
  border-bottom: 2px solid #e5e7eb;
}

.data-card :deep(.el-table tr:hover > td) {
  background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
}

.data-card :deep(.el-table__body tr.el-table__row--striped) {
  background: #fafbfc;
}

.data-card :deep(.el-table td) {
  color: #475569;
  font-size: 14px;
}

.data-card :deep(.el-table__empty-text) {
  color: #94a3b8;
}

/* 数据表格表头优化 */
.data-card :deep(.el-table th .cell) {
  padding: 12px 8px;
  line-height: 1.4;
}

/* 空状态优化 */
.el-empty {
  padding: 80px 0;
}

.el-empty :deep(.el-empty__description) {
  color: rgba(255, 255, 255, 0.8);
  font-size: 16px;
}

/* 导出按钮优化 */
.card-header :deep(.el-dropdown) {
  margin-left: 12px;
}

.card-header :deep(.el-button) {
  transition: all 0.3s ease;
}

.card-header :deep(.el-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.card-header :deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
}

.card-header :deep(.el-dropdown-menu__item:hover) {
  background: linear-gradient(135deg, #f3e8ff 0%, #e0e7ff 100%);
  color: #667eea;
}
</style>
