<template>
  <div class="smart-chart">
    <el-card shadow="never">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">
            <el-icon><TrendCharts /></el-icon>
            数据可视化
          </span>
          <div class="chart-controls">
            <el-radio-group v-model="currentChartType" size="small" @change="renderChart">
              <el-radio-button v-for="type in availableChartTypes" :key="type.value" :label="type.value">
                {{ type.label }}
              </el-radio-button>
            </el-radio-group>
            <el-button size="small" @click="downloadChart">
              <el-icon><Download /></el-icon>
              下载图表
            </el-button>
          </div>
        </div>
      </template>
      
      <div v-if="loading" class="chart-loading">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>正在生成图表...</p>
      </div>
      
      <div v-if="error" class="chart-error">
        <el-alert
          type="error"
          :title="error"
          :closable="false"
          show-icon>
        </el-alert>
      </div>
      
      <div v-show="!loading && !error" ref="chartRef" class="chart-container"></div>
      
      <div v-if="!loading && !error && chartInsight" class="chart-insight">
        <el-alert type="info" :closable="false">
          <template #title>
            <span style="font-weight: 600;">💡 图表洞察</span>
          </template>
          <div v-html="chartInsight"></div>
        </el-alert>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { TrendCharts, Download, Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  data: {
    type: Array,
    required: true
  },
  columns: {
    type: Array,
    required: true
  },
  autoSelect: {
    type: Boolean,
    default: true
  }
})

const chartRef = ref(null)
const chartInstance = ref(null)
const currentChartType = ref('auto')
const availableChartTypes = ref([])
const chartInsight = ref('')
const loading = ref(false)
const error = ref(null)

// 初始化
onMounted(() => {
  if (chartRef.value) {
    chartInstance.value = echarts.init(chartRef.value)
    window.addEventListener('resize', handleResize)
    analyzeDataAndRender()
  }
})

onUnmounted(() => {
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
  window.removeEventListener('resize', handleResize)
})

// 监听数据变化
watch(() => [props.data, props.columns], () => {
  analyzeDataAndRender()
}, { deep: true })

// 窗口大小调整
const handleResize = () => {
  if (chartInstance.value) {
    chartInstance.value.resize()
  }
}

// 分析数据并渲染
const analyzeDataAndRender = () => {
  if (!props.data || props.data.length === 0) {
    error.value = '没有可用于可视化的数据'
    return
  }
  
  loading.value = true
  error.value = null
  
  try {
    // 1. 分析数据类型
    const analysis = analyzeDataStructure()
    
    // 2. 确定可用图表类型
    availableChartTypes.value = getAvailableChartTypes(analysis)
    
    if (availableChartTypes.value.length === 0) {
      throw new Error('数据结构不适合生成图表')
    }
    
    // 3. 自动选择最佳图表类型
    if (props.autoSelect) {
      currentChartType.value = selectBestChartType(analysis)
    }
    
    // 4. 渲染图表
    renderChart()
    
    // 5. 生成图表洞察
    generateChartInsight(analysis)
  } catch (e) {
    console.error('图表生成失败:', e)
    error.value = e.message || '图表生成失败，请稍后重试'
    ElMessage.error('图表生成失败: ' + error.value)
  } finally {
    // 使用setTimeout确保加载动画显示足够时间
    setTimeout(() => {
      loading.value = false
    }, 300)
  }
}

// 分析数据结构
const analyzeDataStructure = () => {
  const analysis = {
    rowCount: props.data.length,
    columns: [],
    hasTimeColumn: false,
    hasCategoryColumn: false,
    numericColumns: [],
    categoryColumns: [],
    timeColumns: []
  }
  
  props.columns.forEach(col => {
    const colName = col.columnName
    const values = props.data.map(row => row[colName]).filter(v => v != null)
    
    if (values.length === 0) return
    
    const colInfo = {
      name: colName,
      comment: col.comment || colName,
      type: 'unknown',
      uniqueCount: new Set(values).size,
      sampleValues: values.slice(0, 5)
    }
    
    // 判断列类型
    if (isTimeColumn(colName, values)) {
      colInfo.type = 'time'
      analysis.timeColumns.push(colInfo)
      analysis.hasTimeColumn = true
    } else if (isNumericColumn(values)) {
      colInfo.type = 'numeric'
      colInfo.min = Math.min(...values.map(Number))
      colInfo.max = Math.max(...values.map(Number))
      colInfo.avg = values.reduce((a, b) => a + Number(b), 0) / values.length
      analysis.numericColumns.push(colInfo)
    } else {
      colInfo.type = 'category'
      analysis.categoryColumns.push(colInfo)
      if (colInfo.uniqueCount <= 20) {
        analysis.hasCategoryColumn = true
      }
    }
    
    analysis.columns.push(colInfo)
  })
  
  return analysis
}

// 判断是否为时间列
const isTimeColumn = (colName, values) => {
  const timeKeywords = ['time', 'date', 'created', 'updated', 'year', 'month', 'day']
  if (timeKeywords.some(k => colName.toLowerCase().includes(k))) {
    return true
  }
  
  // 检查值格式
  const sample = String(values[0])
  return /^\d{4}-\d{2}-\d{2}/.test(sample) || /^\d{4}\/\d{2}\/\d{2}/.test(sample)
}

// 判断是否为数值列
const isNumericColumn = (values) => {
  const numericCount = values.filter(v => !isNaN(Number(v))).length
  return numericCount / values.length > 0.8
}

// 获取可用图表类型
const getAvailableChartTypes = (analysis) => {
  const types = [{ value: 'table', label: '表格' }]
  
  if (analysis.hasTimeColumn && analysis.numericColumns.length > 0) {
    types.push({ value: 'line', label: '折线图' })
    types.push({ value: 'area', label: '面积图' })
  }
  
  if (analysis.hasCategoryColumn && analysis.numericColumns.length > 0) {
    types.push({ value: 'bar', label: '柱状图' })
    types.push({ value: 'pie', label: '饼图' })
  }
  
  if (analysis.numericColumns.length >= 2) {
    types.push({ value: 'scatter', label: '散点图' })
  }
  
  return types
}

// 自动选择最佳图表类型
const selectBestChartType = (analysis) => {
  // 规则1: 时间序列 → 折线图
  if (analysis.hasTimeColumn && analysis.numericColumns.length > 0) {
    return 'line'
  }
  
  // 规则2: 分类统计 + 类别少 → 饼图
  if (analysis.hasCategoryColumn && analysis.numericColumns.length > 0) {
    const mainCategory = analysis.categoryColumns[0]
    if (mainCategory && mainCategory.uniqueCount <= 10) {
      return 'pie'
    }
    return 'bar'
  }
  
  // 规则3: 多数值列 → 散点图
  if (analysis.numericColumns.length >= 2) {
    return 'scatter'
  }
  
  return 'table'
}

// 渲染图表
const renderChart = () => {
  if (!chartInstance.value) return
  
  const analysis = analyzeDataStructure()
  let option = null
  
  switch (currentChartType.value) {
    case 'line':
    case 'area':
      option = generateLineChartOption(analysis, currentChartType.value === 'area')
      break
    case 'bar':
      option = generateBarChartOption(analysis)
      break
    case 'pie':
      option = generatePieChartOption(analysis)
      break
    case 'scatter':
      option = generateScatterChartOption(analysis)
      break
    default:
      return
  }
  
  if (option) {
    chartInstance.value.setOption(option, true)
  }
}

// 生成折线图配置
const generateLineChartOption = (analysis, isArea = false) => {
  const timeCol = analysis.timeColumns[0]
  const numericCols = analysis.numericColumns.slice(0, 3) // 最多3条线
  
  if (!timeCol || numericCols.length === 0) return null
  
  const xAxisData = props.data.map(row => row[timeCol.name])
  const series = numericCols.map(col => ({
    name: col.comment,
    type: 'line',
    data: props.data.map(row => row[col.name]),
    smooth: true,
    areaStyle: isArea ? {} : undefined,
    emphasis: {
      focus: 'series'
    }
  }))
  
  return {
    title: {
      text: '趋势分析',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross'
      }
    },
    legend: {
      data: numericCols.map(col => col.comment),
      top: 30
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 80,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData
    },
    yAxis: {
      type: 'value'
    },
    series: series,
    dataZoom: [
      {
        type: 'inside',
        start: 0,
        end: 100
      },
      {
        start: 0,
        end: 100
      }
    ]
  }
}

// 生成柱状图配置
const generateBarChartOption = (analysis) => {
  const categoryCol = analysis.categoryColumns[0]
  const numericCol = analysis.numericColumns[0]
  
  if (!categoryCol || !numericCol) return null
  
  // 聚合数据
  const aggregated = {}
  props.data.forEach(row => {
    const category = row[categoryCol.name]
    const value = Number(row[numericCol.name])
    if (!aggregated[category]) {
      aggregated[category] = 0
    }
    aggregated[category] += value
  })
  
  const categories = Object.keys(aggregated)
  const values = Object.values(aggregated)
  
  return {
    title: {
      text: `${categoryCol.comment} - ${numericCol.comment}`,
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 60,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: {
        interval: 0,
        rotate: categories.length > 10 ? 45 : 0
      }
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      name: numericCol.comment,
      type: 'bar',
      data: values,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      },
      emphasis: {
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#764ba2' },
            { offset: 1, color: '#667eea' }
          ])
        }
      }
    }]
  }
}

// 生成饼图配置
const generatePieChartOption = (analysis) => {
  const categoryCol = analysis.categoryColumns[0]
  const numericCol = analysis.numericColumns[0]
  
  if (!categoryCol || !numericCol) return null
  
  // 聚合数据
  const aggregated = {}
  props.data.forEach(row => {
    const category = row[categoryCol.name]
    const value = Number(row[numericCol.name])
    if (!aggregated[category]) {
      aggregated[category] = 0
    }
    aggregated[category] += value
  })
  
  const data = Object.entries(aggregated).map(([name, value]) => ({
    name,
    value
  }))
  
  return {
    title: {
      text: `${categoryCol.comment}分布`,
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      type: 'scroll'
    },
    series: [{
      name: numericCol.comment,
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}: {d}%'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 16,
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: true
      },
      data: data
    }]
  }
}

// 生成散点图配置
const generateScatterChartOption = (analysis) => {
  if (analysis.numericColumns.length < 2) return null
  
  const xCol = analysis.numericColumns[0]
  const yCol = analysis.numericColumns[1]
  
  const data = props.data.map(row => [
    Number(row[xCol.name]),
    Number(row[yCol.name])
  ])
  
  return {
    title: {
      text: `${xCol.comment} vs ${yCol.comment}`,
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        return `${xCol.comment}: ${params.value[0]}<br/>${yCol.comment}: ${params.value[1]}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: 60,
      containLabel: true
    },
    xAxis: {
      name: xCol.comment,
      nameLocation: 'middle',
      nameGap: 30
    },
    yAxis: {
      name: yCol.comment,
      nameLocation: 'middle',
      nameGap: 50
    },
    series: [{
      type: 'scatter',
      data: data,
      symbolSize: 10,
      itemStyle: {
        color: '#667eea',
        opacity: 0.7
      },
      emphasis: {
        itemStyle: {
          color: '#764ba2',
          opacity: 1
        }
      }
    }]
  }
}

// 生成图表洞察
const generateChartInsight = (analysis) => {
  const insights = []
  
  // 数据量洞察
  if (analysis.rowCount > 0) {
    insights.push(`📊 共分析 <strong>${analysis.rowCount}</strong> 条数据`)
  }
  
  // 趋势洞察
  if (analysis.hasTimeColumn && analysis.numericColumns.length > 0) {
    const numCol = analysis.numericColumns[0]
    const values = props.data.map(row => Number(row[numCol.name]))
    const trend = values[values.length - 1] - values[0]
    const trendText = trend > 0 ? '📈 上升' : trend < 0 ? '📉 下降' : '➡️ 平稳'
    insights.push(`趋势：${trendText} ${Math.abs(trend).toFixed(2)}`)
  }
  
  // 分布洞察
  if (analysis.hasCategoryColumn && analysis.numericColumns.length > 0) {
    const categoryCol = analysis.categoryColumns[0]
    insights.push(`分类维度：<strong>${categoryCol.comment}</strong>（${categoryCol.uniqueCount} 个类别）`)
  }
  
  chartInsight.value = insights.join(' | ')
}

// 下载图表
const downloadChart = () => {
  if (!chartInstance.value) return
  
  const url = chartInstance.value.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#fff'
  })
  
  const link = document.createElement('a')
  link.href = url
  link.download = `chart_${Date.now()}.png`
  link.click()
  
  ElMessage.success('图表已下载')
}
</script>

<style scoped>
.smart-chart {
  width: 100%;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.chart-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-loading {
  width: 100%;
  height: 450px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #667eea;
}

.chart-loading p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.chart-error {
  width: 100%;
  padding: 20px 0;
}

.chart-container {
  width: 100%;
  height: 450px;
  min-height: 450px;
}

.chart-insight {
  margin-top: 20px;
}

.chart-insight :deep(.el-alert__content) {
  line-height: 1.8;
}

@media (max-width: 768px) {
  .chart-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .chart-container {
    height: 350px;
    min-height: 350px;
  }
}
</style>
