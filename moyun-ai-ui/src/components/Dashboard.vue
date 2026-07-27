<template>
  <div class="dashboard-container">
    <!-- 背景动效 -->
    <div class="bg-effects">
      <div class="grid-bg"></div>
      <div class="scan-line"></div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <div class="glow glow-3"></div>
      <div class="particles">
        <div class="particle" v-for="i in 20" :key="i" :style="{ '--i': i }"></div>
      </div>
    </div>

    <!-- 顶部标题栏 -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo-area">
          <div class="logo-icon">
            <i class="fa-solid fa-bolt"></i>
          </div>
          <span class="system-name">Lynx AI</span>
        </div>
      </div>
      <div class="header-center">
        <div class="title-wrapper">
          <div class="title-decoration left"></div>
          <h1 class="main-title">
            <span class="title-text">智能体运营数据中心</span>
          </h1>
          <div class="title-decoration right"></div>
        </div>
      </div>
      <div class="header-right">
        <div class="datetime">
          <div class="date">{{ currentDate }}</div>
          <div class="time">{{ currentTime }}</div>
        </div>
      </div>
    </header>

    <!-- 主体内容 -->
    <main class="dashboard-main">
      <!-- 第一行：核心指标 -->
      <section class="metrics-row">
        <div class="metric-card" v-for="(metric, index) in coreMetrics" :key="metric.key" :style="{'--delay': index * 0.1 + 's'}">
          <div class="card-glow" :style="{ background: metric.gradient }"></div>
          <div class="card-border-glow"></div>
          <div class="metric-icon" :style="{ background: metric.gradient }">
            <i :class="metric.icon"></i>
          </div>
          <div class="metric-info">
            <div class="metric-value">
              <span class="number" :style="{ color: metric.color }">{{ formatNumber(metric.value) }}</span>
              <span class="unit" v-if="metric.unit">{{ metric.unit }}</span>
            </div>
            <div class="metric-label">{{ metric.label }}</div>
          </div>
          <div class="metric-trend" v-if="metric.trend">
            <i :class="metric.trend > 0 ? 'fa-solid fa-arrow-up' : 'fa-solid fa-arrow-down'" :style="{ color: metric.trend > 0 ? '#34d399' : '#f87171' }"></i>
            <span :style="{ color: metric.trend > 0 ? '#34d399' : '#f87171' }">{{ Math.abs(metric.trend) }}%</span>
          </div>
        </div>
      </section>

      <!-- 主要内容区 - 4列网格布局 -->
      <section class="main-grid">
        <!-- 第1列：趋势图 + 智能体排行 -->
        <div class="grid-col col-1">
          <div class="panel trend-panel">
            <div class="card-border"></div>
            <div class="panel-header">
              <span><i class="fa-solid fa-chart-area"></i> Token趋势</span>
              <div class="live-badge"><span class="dot"></span>实时</div>
            </div>
            <div class="panel-body">
              <div ref="trendChart" class="chart-box"></div>
            </div>
          </div>
          <div class="panel rank-panel">
            <div class="card-border"></div>
            <div class="panel-header">
              <span><i class="fa-solid fa-trophy"></i> 智能体排行</span>
            </div>
            <div class="panel-body">
              <div class="rank-list">
                <div class="rank-row" v-for="(agent, idx) in agentStats.slice(0, 4)" :key="agent.id">
                  <span class="rank-no" :class="'top-' + (idx+1)">{{ idx + 1 }}</span>
                  <span class="rank-name">{{ agent.name }}</span>
                  <div class="rank-bar"><div class="bar-fill" :style="{width: getAgentPercent(agent)+'%'}"></div></div>
                  <span class="rank-val">{{ formatNumber(agent.requestCount || 0) }}</span>
                </div>
                <div v-if="!agentStats.length" class="empty">暂无数据</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 第2列：今日+本月统计 -->
        <div class="grid-col col-2">
          <div class="panel stats-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-bolt"></i> 今日统计</span></div>
            <div class="panel-body stats-grid">
              <div class="stat-box">
                <div class="stat-num blue">{{ formatNumber(todayStats.request_count || 0) }}</div>
                <div class="stat-txt">请求数</div>
              </div>
              <div class="stat-box">
                <div class="stat-num cyan">{{ formatNumber(todayStats.total_tokens || 0) }}</div>
                <div class="stat-txt">Token</div>
              </div>
              <div class="stat-box">
                <div class="stat-num green">{{ formatNumber(todayStats.total_input || 0) }}</div>
                <div class="stat-txt">输入</div>
              </div>
              <div class="stat-box">
                <div class="stat-num purple">{{ formatNumber(todayStats.total_output || 0) }}</div>
                <div class="stat-txt">输出</div>
              </div>
              <div class="stat-box wide">
                <div class="stat-num gold">¥{{ formatCost(todayStats.total_cost || 0) }}</div>
                <div class="stat-txt">今日费用</div>
              </div>
            </div>
          </div>
          <div class="panel stats-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-calendar"></i> 本月累计</span></div>
            <div class="panel-body stats-grid">
              <div class="stat-box">
                <div class="stat-num blue">{{ formatNumber(monthStats.request_count || 0) }}</div>
                <div class="stat-txt">请求数</div>
              </div>
              <div class="stat-box">
                <div class="stat-num cyan">{{ formatNumber(monthStats.total_tokens || 0) }}</div>
                <div class="stat-txt">Token</div>
              </div>
              <div class="stat-box">
                <div class="stat-num green">{{ formatNumber(monthStats.total_input || 0) }}</div>
                <div class="stat-txt">输入</div>
              </div>
              <div class="stat-box">
                <div class="stat-num purple">{{ formatNumber(monthStats.total_output || 0) }}</div>
                <div class="stat-txt">输出</div>
              </div>
              <div class="stat-box wide">
                <div class="stat-num gold">¥{{ formatCost(monthStats.total_cost || 0) }}</div>
                <div class="stat-txt">本月费用</div>
              </div>
            </div>
          </div>
          <div class="panel sys-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-microchip"></i> 系统状态</span></div>
            <div class="panel-body">
              <div class="sys-row"><span>队列待处理</span><span class="sys-val" :class="{warn: systemStatus.pendingLogs > 50}">{{ systemStatus.pendingLogs || 0 }}</span></div>
              <div class="sys-row"><span>累计处理</span><span class="sys-val">{{ formatNumber(systemStatus.totalProcessed || 0) }}</span></div>
              <div class="sys-row"><span>价格缓存</span><span class="sys-val">{{ systemStatus.priceCacheSize || 0 }} 个</span></div>
            </div>
          </div>
        </div>

        <!-- 第3列：模型分布 + 类型分布 -->
        <div class="grid-col col-3">
          <div class="panel chart-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-brain"></i> 模型分布</span></div>
            <div class="panel-body">
              <div ref="modelChart" class="chart-box"></div>
            </div>
          </div>
          <div class="panel chart-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-tags"></i> 请求类型</span></div>
            <div class="panel-body">
              <div ref="typeChart" class="chart-box"></div>
            </div>
          </div>
        </div>

        <!-- 第4列：模型列表 + 知识库 -->
        <div class="grid-col col-4">
          <div class="panel list-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-server"></i> 模型配置</span><span class="badge">{{ coreMetrics[2]?.value || 0 }}</span></div>
            <div class="panel-body">
              <div class="info-list">
                <div class="info-item" v-for="m in modelUsage.slice(0,5)" :key="m.model_name">
                  <i class="fa-solid fa-cube"></i>
                  <span class="info-name">{{ m.model_name }}</span>
                  <span class="info-val">{{ formatNumber(m.total_tokens || 0) }}</span>
                </div>
                <div v-if="!modelUsage.length" class="empty">暂无数据</div>
              </div>
            </div>
          </div>
          <div class="panel list-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-robot"></i> 智能体</span><span class="badge">{{ coreMetrics[0]?.value || 0 }}</span></div>
            <div class="panel-body">
              <div class="info-list">
                <div class="info-item" v-for="a in agentStats.slice(0,5)" :key="a.id">
                  <i class="fa-solid fa-robot"></i>
                  <span class="info-name">{{ a.name }}</span>
                  <span class="info-val" :class="{active: a.enabled}">{{ a.enabled ? '启用' : '禁用' }}</span>
                </div>
                <div v-if="!agentStats.length" class="empty">暂无数据</div>
              </div>
            </div>
          </div>
          <div class="panel workflow-panel">
            <div class="card-border"></div>
            <div class="panel-header"><span><i class="fa-solid fa-diagram-project"></i> 工作流</span><span class="badge">{{ workflowStats.totalCount || 0 }}</span></div>
            <div class="panel-body">
              <div class="workflow-stats">
                <div class="wf-stat-row">
                  <span class="wf-label">已启用</span>
                  <span class="wf-value enabled">{{ workflowStats.enabledCount || 0 }}</span>
                </div>
                <div class="wf-stat-row">
                  <span class="wf-label">今日执行</span>
                  <span class="wf-value">{{ workflowStats.todayExecutions || 0 }}</span>
                </div>
                <div class="wf-stat-row">
                  <span class="wf-label">成功率</span>
                  <span class="wf-value success">{{ workflowStats.successRate || 0 }}%</span>
                </div>
                <div class="wf-stat-row">
                  <span class="wf-label">总执行</span>
                  <span class="wf-value">{{ formatNumber(workflowStats.totalExecutions || 0) }}</span>
                </div>
              </div>
              <div class="wf-ranking" v-if="workflowRanking.length">
                <div class="wf-rank-title">执行排行</div>
                <div class="wf-rank-item" v-for="(wf, idx) in workflowRanking.slice(0, 3)" :key="wf.id">
                  <span class="wf-rank-no" :class="'top-' + (idx+1)">{{ idx + 1 }}</span>
                  <span class="wf-rank-name">{{ wf.name }}</span>
                  <span class="wf-rank-count">{{ wf.executionCount }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 底部状态栏 -->
    <footer class="dashboard-footer">
      <div class="footer-left">
        <div class="status-indicator online">
          <span class="indicator-dot"></span>
          <span>系统运行正常</span>
        </div>
      </div>
      <div class="footer-center">
        <span class="footer-text">Lynx AI · 智能体运营平台</span>
      </div>
      <div class="footer-right">
        <span class="update-time">{{ lastUpdateTime }} 更新</span>
        <button class="refresh-btn" @click="loadData">
          <i class="fa-solid fa-rotate"></i>
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'

const currentDate = ref('')
const currentTime = ref('')
const lastUpdateTime = ref('--:--:--')
// 预设默认数据，避免空白
const coreMetrics = ref([
  { key: 'agents', label: '智能体', value: '-', icon: 'fa-solid fa-robot', gradient: 'linear-gradient(135deg, #667eea, #764ba2)', color: '#a78bfa' },
  { key: 'knowledge', label: '知识库', value: '-', icon: 'fa-solid fa-book', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)', color: '#f472b6' },
  { key: 'models', label: '模型', value: '-', icon: 'fa-solid fa-brain', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)', color: '#22d3ee' },
  { key: 'tools', label: '工具', value: '-', icon: 'fa-solid fa-wrench', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)', color: '#34d399' },
  { key: 'workflows', label: '工作流', value: '-', icon: 'fa-solid fa-diagram-project', gradient: 'linear-gradient(135deg, #6366f1, #8b5cf6)', color: '#a78bfa' },
  { key: 'conversations', label: '会话', value: '-', icon: 'fa-solid fa-comments', gradient: 'linear-gradient(135deg, #fa709a, #fee140)', color: '#fbbf24' }
])
const todayStats = ref({ request_count: 0, total_tokens: 0, total_input: 0, total_output: 0, total_cost: 0 })
const monthStats = ref({ request_count: 0, total_tokens: 0, total_input: 0, total_output: 0, total_cost: 0 })
const agentStats = ref([])
const systemStatus = ref({ pendingLogs: 0, totalProcessed: 0, priceCacheSize: 0 })
const tokenTrend = ref([])
const modelUsage = ref([])
const typeUsage = ref([])
const workflowStats = ref({ totalCount: 0, enabledCount: 0, todayExecutions: 0, successRate: 0 })
const workflowRanking = ref([])

let trendChartInstance = null
let modelChartInstance = null
let typeChartInstance = null
const trendChart = ref(null)
const modelChart = ref(null)
const typeChart = ref(null)
let timeTimer = null
let dataTimer = null

const updateTime = () => {
  const now = new Date()
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'long' })
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const loadData = async () => {
  try {
    const response = await axios.get('/api/dashboard/overview')
    if (response.data.success) {
      const data = response.data.data
      const metrics = data.coreMetrics || {}
      coreMetrics.value = [
        { key: 'agents', label: '智能体', value: metrics.agentCount || 0, icon: 'fa-solid fa-robot', gradient: 'linear-gradient(135deg, #667eea, #764ba2)', color: '#a78bfa' },
        { key: 'knowledge', label: '知识库', value: metrics.knowledgeCount || 0, icon: 'fa-solid fa-book', gradient: 'linear-gradient(135deg, #f093fb, #f5576c)', color: '#f472b6' },
        { key: 'models', label: '模型', value: metrics.modelCount || 0, icon: 'fa-solid fa-brain', gradient: 'linear-gradient(135deg, #4facfe, #00f2fe)', color: '#22d3ee' },
        { key: 'tools', label: '工具', value: metrics.toolCount || 0, icon: 'fa-solid fa-wrench', gradient: 'linear-gradient(135deg, #43e97b, #38f9d7)', color: '#34d399' },
        { key: 'workflows', label: '工作流', value: metrics.workflowCount || 0, icon: 'fa-solid fa-diagram-project', gradient: 'linear-gradient(135deg, #6366f1, #8b5cf6)', color: '#a78bfa' },
        { key: 'conversations', label: '会话', value: metrics.conversationCount || 0, icon: 'fa-solid fa-comments', gradient: 'linear-gradient(135deg, #fa709a, #fee140)', color: '#fbbf24' }
      ]
      workflowStats.value = data.workflowStats || workflowStats.value
      workflowRanking.value = data.workflowRanking || []
      todayStats.value = data.todayTokenStats || todayStats.value
      monthStats.value = data.monthTokenStats || monthStats.value
      agentStats.value = data.agentStats || []
      systemStatus.value = data.systemStatus || systemStatus.value
      tokenTrend.value = data.tokenTrend || []
      modelUsage.value = data.modelUsage || []
      typeUsage.value = data.typeUsage || []
      lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
      await nextTick()
      initCharts()
    }
  } catch (error) {
    console.error('数据加载失败:', error)
  }
}

const initCharts = () => { initTrendChart(); initModelChart(); initTypeChart() }

const initTrendChart = () => {
  if (!trendChart.value) return
  if (!trendChartInstance) {
    trendChartInstance = echarts.init(trendChart.value, null, { renderer: 'canvas' })
  }
  const dates = tokenTrend.value.map(i => i.stat_date?.substring(5) || '')
  const tokens = tokenTrend.value.map(i => i.total_tokens || 0)
  trendChartInstance.setOption({
    backgroundColor: 'transparent',
    animation: false,
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.95)', borderColor: '#38bdf8', borderWidth: 1, textStyle: { color: '#e2e8f0', fontSize: 13 }, padding: [10, 16] },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#1e293b' } }, axisLabel: { color: '#94a3b8', fontSize: 12 }, axisTick: { show: false } },
    yAxis: { type: 'value', axisLine: { show: false }, axisLabel: { color: '#94a3b8', fontSize: 11 }, splitLine: { lineStyle: { color: 'rgba(56,189,248,0.08)' } }, splitNumber: 4 },
    series: [{
      type: 'bar', data: tokens, barWidth: 24, barGap: '30%',
      itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#38bdf8' }, { offset: 1, color: '#0ea5e9' }]) },
      emphasis: { itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#67e8f9' }, { offset: 1, color: '#38bdf8' }]) } }
    }]
  }, true)
}

const initModelChart = () => {
  if (!modelChart.value) return
  if (!modelChartInstance) {
    modelChartInstance = echarts.init(modelChart.value, null, { renderer: 'canvas' })
  }
  const data = modelUsage.value.map(i => ({ name: i.model_name?.split('-').pop() || i.model_name, value: i.total_tokens || 0 }))
  if (data.length === 0) return
  modelChartInstance.setOption({
    backgroundColor: 'transparent',
    animation: false,
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15,23,42,0.95)', borderColor: '#38bdf8', textStyle: { color: '#e2e8f0', fontSize: 13 }, formatter: '{b}<br/>{c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['42%', '72%'], center: ['50%', '50%'],
      itemStyle: { borderRadius: 6, borderColor: '#020617', borderWidth: 3 },
      label: { show: true, color: '#e2e8f0', fontSize: 12, position: 'outside', formatter: '{b}' },
      labelLine: { length: 12, length2: 12, lineStyle: { color: '#475569', width: 1 } },
      emphasis: { scale: true, scaleSize: 8 },
      data, color: ['#38bdf8', '#a78bfa', '#34d399', '#fbbf24', '#f472b6']
    }]
  }, true)
}

const initTypeChart = () => {
  if (!typeChart.value) return
  if (!typeChartInstance) {
    typeChartInstance = echarts.init(typeChart.value, null, { renderer: 'canvas' })
  }
  const labels = { chat: '对话请求', embedding_query: '向量查询', embedding_document: '文档处理', embedding_image: '图片处理' }
  const data = typeUsage.value.map(i => ({ name: labels[i.request_type] || i.request_type, value: i.request_count || 0 }))
  if (data.length === 0) return
  typeChartInstance.setOption({
    backgroundColor: 'transparent',
    animation: false,
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15,23,42,0.95)', borderColor: '#38bdf8', textStyle: { color: '#e2e8f0', fontSize: 13 } },
    series: [{
      type: 'pie', radius: ['38%', '68%'], center: ['50%', '50%'],
      itemStyle: { borderRadius: 6, borderColor: '#020617', borderWidth: 3 },
      label: { show: true, color: '#e2e8f0', fontSize: 11, formatter: '{b}\n{d}%' },
      labelLine: { length: 10, length2: 14, lineStyle: { color: '#475569', width: 1 } },
      emphasis: { scale: true, scaleSize: 8 },
      data, color: ['#38bdf8', '#34d399', '#fbbf24', '#f472b6']
    }]
  }, true)
}

const formatNumber = (n) => n >= 1e6 ? (n/1e6).toFixed(1)+'M' : n >= 1e3 ? (n/1e3).toFixed(1)+'K' : n
const formatCost = (c) => (parseFloat(c) || 0).toFixed(4)
const getAgentPercent = (a) => { const max = Math.max(...agentStats.value.map(x => x.requestCount || 0)); return max ? ((a.requestCount||0)/max)*100 : 0 }
const handleResize = () => { trendChartInstance?.resize(); modelChartInstance?.resize(); typeChartInstance?.resize() }

onMounted(() => { updateTime(); loadData(); timeTimer = setInterval(updateTime, 1000); dataTimer = setInterval(loadData, 15000); window.addEventListener('resize', handleResize) })
onUnmounted(() => { clearInterval(timeTimer); clearInterval(dataTimer); window.removeEventListener('resize', handleResize); trendChartInstance?.dispose(); modelChartInstance?.dispose(); typeChartInstance?.dispose() })
</script>

<style scoped src="@/styles/dashboard.css"></style>
