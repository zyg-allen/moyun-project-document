<template>
  <div class="token-usage-stats" v-loading="loading">
    <div class="stats-header">
      <div class="header-left">
        <h2><i class="fa-solid fa-chart-line"></i> Token 使用统计</h2>
        <!-- 系统状态内联显示 -->
        <div class="inline-status" v-if="metrics.pendingLogs !== undefined">
          <span class="status-dot" :class="{ warning: metrics.pendingLogs > 100 }"></span>
          <span>队列 <strong>{{ metrics.pendingLogs }}</strong></span>
          <span class="sep">·</span>
          <span>已处理 <strong>{{ formatNumber(metrics.totalProcessed) }}</strong></span>
          <el-button size="small" link type="primary" @click="flushLogs" :loading="flushing">
            <i class="fa-solid fa-sync"></i>
          </el-button>
          <el-popover placement="bottom" :width="320" trigger="hover">
            <template #reference>
              <i class="fa-solid fa-circle-question help-icon"></i>
            </template>
            <div class="help-popover">
              <div class="help-row"><el-tag size="small" type="primary">chat</el-tag> 对话请求（实际Token）</div>
              <div class="help-row"><el-tag size="small" type="success">embedding_query</el-tag> 查询向量化</div>
              <div class="help-row"><el-tag size="small" type="warning">embedding_document</el-tag> 文档向量化</div>
              <div class="help-row"><el-tag size="small" type="danger">embedding_image</el-tag> 图片向量化</div>
              <div class="help-footer">Token估算：中文≈1.5字符/token · 费用基于模型配置</div>
            </div>
          </el-popover>
        </div>
      </div>
      <div class="date-picker">
        <el-button-group style="margin-right: 10px;">
          <el-button :type="quickRange === 'today' ? 'primary' : ''" @click="setQuickRange('today')">今日</el-button>
          <el-button :type="quickRange === 'week' ? 'primary' : ''" @click="setQuickRange('week')">近7天</el-button>
          <el-button :type="quickRange === 'month' ? 'primary' : ''" @click="setQuickRange('month')">近30天</el-button>
        </el-button-group>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="onDateChange"
        />
        <el-button type="primary" @click="loadStats" style="margin-left: 10px;">
          <i class="fa-solid fa-sync"></i> 刷新
        </el-button>
      </div>
    </div>

    <!-- 总体统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
          <i class="fa-solid fa-comments"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(totalStats.request_count || 0) }}</div>
          <div class="stat-label">请求次数</div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
          <i class="fa-solid fa-arrow-right-to-bracket"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(totalStats.total_input || 0) }}</div>
          <div class="stat-label">输入 Token</div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
          <i class="fa-solid fa-arrow-right-from-bracket"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(totalStats.total_output || 0) }}</div>
          <div class="stat-label">输出 Token</div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
          <i class="fa-solid fa-calculator"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(totalStats.total_tokens || 0) }}</div>
          <div class="stat-label">总 Token</div>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
          <i class="fa-solid fa-coins"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">¥{{ formatCost(totalStats.total_cost || 0) }}</div>
          <div class="stat-label">预估费用</div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-container">
      <!-- 按日期统计 -->
      <div class="chart-card">
        <h3><i class="fa-solid fa-calendar-days"></i> 每日使用趋势</h3>
        <div class="chart-content">
          <el-table :data="dateStats" stripe style="width: 100%" max-height="300">
            <el-table-column prop="stat_date" label="日期" width="120" />
            <el-table-column prop="model_name" label="模型" width="140" />
            <el-table-column prop="request_count" label="请求数" width="100" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.request_count) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_input" label="输入Token" width="120" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.total_input) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_output" label="输出Token" width="120" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.total_output) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_cost" label="费用" width="100" align="right">
              <template #default="{ row }">
                ¥{{ formatCost(row.total_cost) }}
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无数据" :image-size="80" />
            </template>
          </el-table>
        </div>
      </div>

      <!-- 按模型统计 -->
      <div class="chart-card">
        <h3><i class="fa-solid fa-robot"></i> 模型使用分布</h3>
        <div class="chart-content">
          <el-table :data="modelStats" stripe style="width: 100%">
            <el-table-column prop="model_name" label="模型名称" />
            <el-table-column prop="model_provider" label="提供商" width="100" />
            <el-table-column prop="request_count" label="请求数" width="100" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.request_count) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_tokens" label="总Token" width="120" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.total_tokens) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_cost" label="费用" width="100" align="right">
              <template #default="{ row }">
                ¥{{ formatCost(row.total_cost) }}
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无数据" :image-size="80" />
            </template>
          </el-table>
        </div>
      </div>

      <!-- 按请求类型统计 -->
      <div class="chart-card">
        <h3><i class="fa-solid fa-tags"></i> 请求类型分布</h3>
        <div class="chart-content">
          <el-table :data="typeStats" stripe style="width: 100%">
            <el-table-column prop="request_type" label="请求类型" width="180">
              <template #default="{ row }">
                <el-tag :type="getTypeTagType(row.request_type)" size="small">
                  {{ formatRequestType(row.request_type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="request_count" label="请求数" width="100" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.request_count) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_input" label="输入Token" width="120" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.total_input) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_tokens" label="总Token" width="120" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.total_tokens) }}
              </template>
            </el-table-column>
            <el-table-column prop="total_cost" label="费用" align="right">
              <template #default="{ row }">
                ¥{{ formatCost(row.total_cost) }}
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无数据" :image-size="80" />
            </template>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 底部趋势图表 -->
    <div class="trend-section" v-if="dateStats.length > 0">
      <div class="trend-card">
        <h3><i class="fa-solid fa-chart-area"></i> Token 使用趋势</h3>
        <div ref="trendChart" class="trend-chart"></div>
      </div>
      <div class="trend-card">
        <h3><i class="fa-solid fa-pie-chart"></i> 模型使用占比</h3>
        <div ref="modelPieChart" class="trend-chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

// 图表相关
const trendChart = ref(null)
const modelPieChart = ref(null)
let trendChartInstance = null
let modelPieChartInstance = null

// 日期范围
const dateRange = ref([
  new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  new Date().toISOString().split('T')[0]
])

// 统计数据
const totalStats = ref({})
const dateStats = ref([])
const modelStats = ref([])
const typeStats = ref([])
const loading = ref(false)
const quickRange = ref('week')
const metrics = ref({})
const flushing = ref(false)

// 设置快捷日期范围
const setQuickRange = (range) => {
  quickRange.value = range
  const today = new Date().toISOString().split('T')[0]
  let startDate = today
  
  if (range === 'today') {
    startDate = today
  } else if (range === 'week') {
    startDate = new Date(Date.now() - 6 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  } else if (range === 'month') {
    startDate = new Date(Date.now() - 29 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  }
  
  dateRange.value = [startDate, today]
  loadStats()
}

// 日期选择器变化时清除快捷选择状态
const onDateChange = () => {
  quickRange.value = ''
  loadStats()
}

// 加载统计数据
const loadStats = async () => {
  loading.value = true
  try {
    const [startDate, endDate] = dateRange.value || []
    const params = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate

    const response = await request({ url: '/cms/ai/token-usage/overview', method: 'get', params })
    // request 拦截器返回 AjaxResult {code, msg, data}，data 即统计数据对象
    const data = response.data
    totalStats.value = data.total || {}
    dateStats.value = data.byDate || []
    modelStats.value = data.byModel || []
    typeStats.value = data.byType || []
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 格式化数字
const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  return num.toLocaleString()
}

// 格式化费用
const formatCost = (cost) => {
  if (cost === null || cost === undefined) return '0.00'
  return parseFloat(cost).toFixed(4)
}

// 格式化请求类型
const formatRequestType = (type) => {
  const typeMap = {
    'chat': '对话请求',
    'embedding_query': '查询向量化',
    'embedding_document': '文档向量化',
    'embedding_image': '图片向量化'
  }
  return typeMap[type] || type
}

// 获取请求类型标签颜色
const getTypeTagType = (type) => {
  const colorMap = {
    'chat': 'primary',
    'embedding_query': 'success',
    'embedding_document': 'warning',
    'embedding_image': 'danger'
  }
  return colorMap[type] || 'info'
}

// 加载监控指标
const loadMetrics = async () => {
  try {
    const response = await request({ url: '/cms/ai/token-usage/metrics', method: 'get' })
    metrics.value = response.data
  } catch (error) {
    console.error('加载监控指标失败:', error)
  }
}

// 手动刷新日志到数据库
const flushLogs = async () => {
  flushing.value = true
  try {
    const response = await request({ url: '/cms/ai/token-usage/flush', method: 'post'})
    ElMessage.success(`已同步 ${response.data.flushed} 条日志`)
    loadMetrics()
  } catch (error) {
    console.error('同步失败:', error)
    ElMessage.error('同步失败')
  } finally {
    flushing.value = false
  }
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChart.value || dateStats.value.length === 0) return
  
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChart.value)
  }
  
  // 按日期聚合数据
  const dateMap = {}
  dateStats.value.forEach(item => {
    const date = item.stat_date
    if (!dateMap[date]) {
      dateMap[date] = { tokens: 0, cost: 0 }
    }
    dateMap[date].tokens += item.total_tokens || 0
    dateMap[date].cost += parseFloat(item.total_cost) || 0
  })
  
  const dates = Object.keys(dateMap).sort()
  const tokens = dates.map(d => dateMap[d].tokens)
  const costs = dates.map(d => dateMap[d].cost)
  
  trendChartInstance.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['Token数', '费用(¥)'], top: 0 },
    grid: { left: 50, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates.map(d => d.substring(5)), boundaryGap: false },
    yAxis: [
      { type: 'value', name: 'Token', position: 'left', axisLabel: { formatter: v => v >= 1000 ? (v/1000).toFixed(0) + 'K' : v } },
      { type: 'value', name: '费用', position: 'right', axisLabel: { formatter: '¥{value}' } }
    ],
    series: [
      { name: 'Token数', type: 'line', smooth: true, data: tokens, areaStyle: { opacity: 0.3 }, itemStyle: { color: '#409eff' } },
      { name: '费用(¥)', type: 'line', smooth: true, yAxisIndex: 1, data: costs, itemStyle: { color: '#67c23a' } }
    ]
  })
}

// 初始化模型饼图
const initModelPieChart = () => {
  if (!modelPieChart.value || modelStats.value.length === 0) return
  
  if (!modelPieChartInstance) {
    modelPieChartInstance = echarts.init(modelPieChart.value)
  }
  
  const data = modelStats.value.map(item => ({
    name: item.model_name,
    value: item.total_tokens || 0
  }))
  
  modelPieChartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'center', type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data,
      color: ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399']
    }]
  })
}

// 监听数据变化更新图表
watch([dateStats, modelStats], () => {
  nextTick(() => {
    initTrendChart()
    initModelPieChart()
  })
})

// 窗口大小变化时重绘图表
const handleResize = () => {
  trendChartInstance?.resize()
  modelPieChartInstance?.resize()
}

onMounted(() => {
  loadStats()
  loadMetrics()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChartInstance?.dispose()
  modelPieChartInstance?.dispose()
})
</script>

<style scoped src="@/views/ai/styles/token-usage-stats.css"></style>
