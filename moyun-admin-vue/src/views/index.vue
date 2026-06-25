<template>
  <div class="dashboard-container">
    <!-- 顶部欢迎条 -->
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-flex">
        <div class="welcome-info">
          <el-avatar :size="56" :src="userStore.avatar" class="welcome-avatar" />
          <div class="welcome-text">
            <div class="welcome-hello">{{ greeting }}，{{ userStore.name || '管理员' }}，欢迎回来</div>
            <div class="welcome-sub">
              今天是 {{ todayStr }}，祝您工作顺利！
              <el-tag v-if="todoCount > 0" type="warning" size="small" class="ml8">待办 {{ todoCount }}</el-tag>
              <el-tag v-else type="success" size="small" class="ml8">无待办</el-tag>
            </div>
          </div>
        </div>
        <div class="welcome-actions">
          <el-button type="primary" plain icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
          <el-button type="success" plain icon="Delete" :loading="cacheRefreshing" @click="handleRefreshCache" v-hasPermi="['system:dashboard:refresh']">刷新缓存</el-button>
        </div>
      </div>
    </el-card>

    <!-- 核心指标卡片 -->
    <el-row :gutter="16" class="metric-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="m in metrics" :key="m.key">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-flex">
            <div class="metric-icon" :class="metricColorClass(m.key)">
              <el-icon><component :is="metricIcon(m.icon)" /></el-icon>
            </div>
            <div class="metric-body">
              <div class="metric-value">{{ formatNum(m.value) }}</div>
              <div class="metric-label">{{ m.label }}</div>
              <div class="metric-trend" :class="trendClass(m.trend)">
                <el-icon v-if="m.trend > 0"><Top /></el-icon>
                <el-icon v-else-if="m.trend < 0"><Bottom /></el-icon>
                {{ m.trend > 0 ? '+' : '' }}{{ m.trend || 0 }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日统计 -->
    <el-row :gutter="16" class="block-row">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="today-card" shadow="hover">
          <div class="today-num">{{ formatNum(todayStats.todayVisitors) }}</div>
          <div class="today-label">今日访客数 (UV)</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="today-card" shadow="hover">
          <div class="today-num">{{ formatNum(todayStats.todayPageViews) }}</div>
          <div class="today-label">今日浏览量 (PV)</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="today-card" shadow="hover">
          <div class="today-num">{{ formatNum(todayStats.todayLoginUsers) }}</div>
          <div class="today-label">今日登录人数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="today-card" shadow="hover">
          <div class="today-num">{{ formatNum(todayStats.todayNewArticles) }}</div>
          <div class="today-label">今日新增文章</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图表 -->
    <el-row :gutter="16" class="block-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><TrendCharts /></el-icon> 近7天登录趋势</span>
            </div>
          </template>
          <div ref="loginChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><DataLine /></el-icon> 近7天文章发布趋势</span>
            </div>
          </template>
          <div ref="publishChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办 / 已办 / 栏目排行 -->
    <el-row :gutter="16" class="block-row">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover" class="todo-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Bell /></el-icon> 待办任务</span>
              <el-tag type="warning" size="small">{{ todoTasks.length }}</el-tag>
            </div>
          </template>
          <div v-loading="loading" class="task-list">
            <div v-for="t in todoTasks" :key="t.type + '_' + t.id" class="task-item" @click="goTask(t)">
              <div class="task-main">
                <el-tag :type="priorityTag(t.priority)" size="small" effect="plain">{{ priorityLabel(t.priority) }}</el-tag>
                <span class="task-title">{{ t.title }}</span>
              </div>
              <div class="task-meta">
                <span>{{ t.submitter }}</span>
                <span class="task-time">{{ relativeTime(t.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && todoTasks.length === 0" description="暂无待办" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover" class="done-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><CircleCheck /></el-icon> 与我相关（已办）</span>
              <el-tag type="success" size="small">{{ myTasks.length }}</el-tag>
            </div>
          </template>
          <div v-loading="loading" class="task-list">
            <div v-for="t in myTasks" :key="t.type + '_' + t.id" class="task-item done-item" @click="goTask(t)">
              <div class="task-main">
                <el-tag type="success" size="small" effect="plain">已办</el-tag>
                <span class="task-title">{{ t.title }}</span>
              </div>
              <div class="task-meta">
                <span>{{ t.submitter }}</span>
                <span class="task-time">{{ relativeTime(t.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && myTasks.length === 0" description="暂无已办" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><TrophyBase /></el-icon> 栏目排行榜 Top{{ categoryRanking.length }}</span>
            </div>
          </template>
          <div v-loading="loading" class="rank-list">
            <div v-for="r in categoryRanking" :key="r.categoryId" class="rank-item">
              <div class="rank-no" :class="rankClass(r.rank)">{{ r.rank }}</div>
              <div class="rank-content">
                <div class="rank-name">{{ r.categoryName }}</div>
                <el-progress :percentage="rankPercent(r)" :show-text="false" :stroke-width="6" :color="rankColor(r.rank)" />
              </div>
              <div class="rank-stats">
                <div>{{ formatNum(r.totalViews) }} 浏览</div>
                <div class="rank-sub">{{ formatNum(r.articleCount) }} 文章</div>
              </div>
            </div>
            <el-empty v-if="!loading && categoryRanking.length === 0" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 系统动态 / 热门文章 / 系统配置 -->
    <el-row :gutter="16" class="block-row">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><List /></el-icon> 系统更新动态</span>
            </div>
          </template>
          <div v-loading="loading" class="activity-list">
            <el-timeline>
              <el-timeline-item
                v-for="a in systemActivities"
                :key="a.id"
                :timestamp="relativeTime(a.createTime)"
                placement="top"
                :type="activityTagType(a.businessType)"
              >
                <div class="activity-content">
                  <el-tag :type="activityTagType(a.businessType)" size="small" effect="plain">{{ a.module }}</el-tag>
                  <span class="activity-text">{{ a.content }}</span>
                </div>
                <div class="activity-meta">操作人：{{ a.operator }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="!loading && systemActivities.length === 0" description="暂无动态" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><Star /></el-icon> 热门文章 Top{{ hotArticles.length }}</span>
              <el-tooltip content="基于浏览量+点赞数加权计算（Redis ZSet 维护）" placement="top">
                <el-icon class="tip-icon"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div v-loading="loading" class="hot-list">
            <div v-for="h in hotArticles" :key="h.id" class="hot-item" @click="goArticle(h)">
              <div class="hot-no" :class="rankClass(h.rank)">{{ h.rank }}</div>
              <div class="hot-body">
                <div class="hot-title">{{ h.title }}</div>
                <div class="hot-meta">
                  <span>{{ h.author }}</span>
                  <span>· {{ formatNum(h.views) }} 浏览</span>
                  <span>· {{ formatNum(h.likes) }} 点赞</span>
                </div>
              </div>
              <div class="hot-score">{{ formatScore(h.score) }}</div>
            </div>
            <el-empty v-if="!loading && hotArticles.length === 0" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span><el-icon><Setting /></el-icon> 系统配置概览</span>
            </div>
          </template>
          <div v-loading="loading" class="config-list">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="站点名">{{ configOverview.siteName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="站点描述">{{ configOverview.siteDescription || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前版本">{{ configOverview.version || '-' }}</el-descriptions-item>
              <el-descriptions-item label="运行时长">{{ configOverview.uptimeHours || 0 }} 小时</el-descriptions-item>
              <el-descriptions-item label="缓存命中率">{{ formatRate(configOverview.cacheHitRate) }}</el-descriptions-item>
              <el-descriptions-item label="数据库表数">{{ configOverview.tableCount || 0 }}</el-descriptions-item>
              <el-descriptions-item label="Redis 内存">{{ formatMemory(configOverview.redisMemoryMb) }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import useUserStore from '@/store/modules/user'
import {
  getDashboardAll,
  refreshDashboardCache
} from '@/api/system/dashboard'

const router = useRouter()
const userStore = useUserStore()

// ===== 数据 =====
const loading = ref(false)
const metrics = ref([])
const todayStats = ref({})
const loginTrend = ref([])
const publishTrend = ref([])
const categoryRanking = ref([])
const todoTasks = ref([])
const myTasks = ref([])
const systemActivities = ref([])
const hotArticles = ref([])
const configOverview = ref({})

// ===== 计算属性 =====
const todoCount = computed(() => todoTasks.value.length)
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 9) return '早上好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const todayStr = computed(() => {
  const d = new Date()
  const weeks = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weeks[d.getDay()]}`
})

// ===== 图表 =====
const loginChartRef = ref(null)
const publishChartRef = ref(null)
let loginChart = null
let publishChart = null

function renderLoginChart() {
  if (!loginChartRef.value) return
  if (!loginChart) loginChart = echarts.init(loginChartRef.value, 'macarons')
  // 按 date 分组：success / fail
  const grouped = {}
  loginTrend.value.forEach(p => {
    if (!grouped[p.date]) grouped[p.date] = { success: 0, fail: 0 }
    grouped[p.date][p.label] = p.value
  })
  const dates = Object.keys(grouped).sort()
  const successArr = dates.map(d => grouped[d].success || 0)
  const failArr = dates.map(d => grouped[d].fail || 0)
  loginChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成功', '失败'], right: 10, top: 0 },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { formatter: v => v.slice(5) } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '成功', type: 'line', smooth: true, areaStyle: {}, data: successArr, itemStyle: { color: '#67c23a' } },
      { name: '失败', type: 'line', smooth: true, areaStyle: {}, data: failArr, itemStyle: { color: '#f56c6c' } }
    ]
  })
}

function renderPublishChart() {
  if (!publishChartRef.value) return
  if (!publishChart) publishChart = echarts.init(publishChartRef.value, 'macarons')
  const grouped = {}
  publishTrend.value.forEach(p => {
    grouped[p.date] = (grouped[p.date] || 0) + (p.value || 0)
  })
  const dates = Object.keys(grouped).sort()
  const arr = dates.map(d => grouped[d])
  publishChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates, axisLabel: { formatter: v => v.slice(5) } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '发布数', type: 'bar', barWidth: '50%', data: arr,
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] }
      }
    ]
  })
}

function resizeCharts() {
  loginChart && loginChart.resize()
  publishChart && publishChart.resize()
}

// ===== 数据加载 =====
function loadAll() {
  loading.value = true
  getDashboardAll().then(res => {
    const d = res.data || {}
    metrics.value = d.metrics || []
    todayStats.value = d.todayStats || {}
    loginTrend.value = d.loginTrend || []
    publishTrend.value = d.publishTrend || []
    categoryRanking.value = d.categoryRanking || []
    todoTasks.value = d.todoTasks || []
    myTasks.value = d.myTasks || []
    systemActivities.value = d.systemActivities || []
    hotArticles.value = d.hotArticles || []
    configOverview.value = d.configOverview || {}
    nextTick(() => {
      renderLoginChart()
      renderPublishChart()
    })
  }).catch(err => {
    ElMessage.error('首页数据加载失败：' + (err?.message || '未知错误'))
  }).finally(() => {
    loading.value = false
  })
}

const cacheRefreshing = ref(false)
function handleRefreshCache() {
  cacheRefreshing.value = true
  refreshDashboardCache().then(() => {
    ElMessage.success('缓存已刷新')
    loadAll()
  }).catch(err => {
    ElMessage.error('刷新缓存失败：' + (err?.message || '未知错误'))
  }).finally(() => {
    cacheRefreshing.value = false
  })
}

// ===== 跳转联动 =====
function goTask(task) {
  if (!task || !task.routePath) return
  router.push(task.routePath).catch(() => {
    ElMessage.warning('目标页面不可达：' + task.routePath)
  })
}

function goArticle(article) {
  if (!article || !article.id) return
  // edit.vue 读取 route.query.id，必须用 query 形式跳转
  router.push({ path: '/cms/article/edit', query: { id: article.id } }).catch(() => {
    ElMessage.warning('文章编辑页不可达')
  })
}

// ===== 格式化工具 =====
function formatNum(v) {
  const n = Number(v || 0)
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
function formatScore(v) {
  const n = Number(v || 0)
  return n.toFixed(0)
}
function formatRate(v) {
  const n = Number(v || 0)
  return n.toFixed(1) + '%'
}
function formatMemory(mb) {
  const n = Number(mb || 0)
  if (n >= 1024) return (n / 1024).toFixed(2) + ' GB'
  return n.toFixed(1) + ' MB'
}
function relativeTime(time) {
  if (!time) return ''
  const t = new Date(time).getTime()
  if (isNaN(t)) return time
  const diff = Date.now() - t
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 86400000 * 30) return Math.floor(diff / 86400000) + ' 天前'
  return time.slice(0, 10)
}

// ===== 标签 / 颜色 =====
const iconMap = {
  Document: 'Document',
  CircleCheck: 'CircleCheck',
  Clock: 'Clock',
  View: 'View',
  Star: 'Star',
  ChatDotRound: 'ChatDotRound'
}
function metricIcon(name) { return iconMap[name] || 'DataAnalysis' }
function metricColorClass(key) {
  return { articleCount: 'metric-blue', publishedArticles: 'metric-green', pendingArticles: 'metric-orange',
    totalViews: 'metric-purple', totalLikes: 'metric-red', totalComments: 'metric-cyan' }[key] || 'metric-blue'
}
function trendClass(t) {
  if (t > 0) return 'trend-up'
  if (t < 0) return 'trend-down'
  return 'trend-flat'
}
function priorityLabel(p) { return { high: '紧急', medium: '普通', low: '低' }[p] || '普通' }
function priorityTag(p) { return { high: 'danger', medium: 'warning', low: 'info' }[p] || 'info' }
function rankClass(rank) {
  if (rank === 1) return 'rank-top1'
  if (rank === 2) return 'rank-top2'
  if (rank === 3) return 'rank-top3'
  return ''
}
function rankColor(rank) {
  return ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399'][Math.min(rank - 1, 4)] || '#909399'
}
function rankPercent(item) {
  if (!categoryRanking.value.length) return 0
  const max = Math.max(...categoryRanking.value.map(r => r.totalViews || 0))
  if (!max) return 0
  return Math.round((item.totalViews || 0) * 100 / max)
}
function activityTagType(bt) {
  return { INSERT: 'success', UPDATE: 'primary', DELETE: 'danger', EXPORT: 'warning', OTHER: 'info' }[bt] || 'info'
}

// ===== 生命周期 =====
onMounted(() => {
  loadAll()
  window.addEventListener('resize', resizeCharts)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  loginChart && loginChart.dispose()
  publishChart && publishChart.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-container {
  padding: 16px;
  background: #f0f2f5;
  min-height: calc(100vh - 84px);
}

/* 欢迎条 */
.welcome-card {
  margin-bottom: 16px;
  .welcome-flex {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
  }
  .welcome-info {
    display: flex;
    align-items: center;
    gap: 14px;
  }
  .welcome-avatar {
    background: linear-gradient(135deg, #409eff, #67c23a);
    flex-shrink: 0;
  }
  .welcome-text { line-height: 1.6; }
  .welcome-hello { font-size: 18px; font-weight: 600; color: #303133; }
  .welcome-sub { font-size: 13px; color: #909399; }
  .ml8 { margin-left: 8px; }
}

/* 指标卡片 */
.metric-row { margin-bottom: 16px; }
.metric-card { margin-bottom: 12px; }
.metric-flex { display: flex; align-items: center; gap: 12px; }
.metric-icon {
  width: 48px; height: 48px;
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: #fff;
  flex-shrink: 0;
}
.metric-blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
.metric-green { background: linear-gradient(135deg, #67c23a, #85ce61); }
.metric-orange { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.metric-purple { background: linear-gradient(135deg, #9c27b0, #ba68c8); }
.metric-red { background: linear-gradient(135deg, #f56c6c, #f89898); }
.metric-cyan { background: linear-gradient(135deg, #00bcd4, #4dd0e1); }
.metric-body { flex: 1; min-width: 0; }
.metric-value { font-size: 22px; font-weight: 700; color: #303133; line-height: 1.2; }
.metric-label { font-size: 12px; color: #909399; margin-top: 2px; }
.metric-trend { font-size: 11px; margin-top: 2px; display: flex; align-items: center; gap: 2px; }
.trend-up { color: #f56c6c; }
.trend-down { color: #67c23a; }
.trend-flat { color: #909399; }

/* 今日统计 */
.block-row { margin-bottom: 16px; }
.today-card {
  text-align: center;
  margin-bottom: 12px;
  .today-num { font-size: 28px; font-weight: 700; color: #409eff; }
  .today-label { font-size: 13px; color: #909399; margin-top: 4px; }
}

/* 卡片头部 */
.card-header {
  display: flex; align-items: center; justify-content: space-between;
  font-weight: 600; color: #303133;
  span { display: flex; align-items: center; gap: 6px; }
  .tip-icon { color: #909399; cursor: help; }
}

/* 图表 */
.chart-box { height: 280px; }

/* 任务列表 */
.task-list { max-height: 360px; overflow-y: auto; }
.task-item {
  padding: 10px 8px;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
  &:hover { background: #f5f7fa; }
  &:last-child { border-bottom: none; }
  .task-main { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
  .task-title {
    font-size: 13px; color: #303133;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    flex: 1; min-width: 0;
  }
  .task-meta {
    display: flex; justify-content: space-between;
    font-size: 12px; color: #909399;
    padding-left: 56px;
  }
  .task-time { font-size: 11px; }
}

/* 栏目排行 */
.rank-list { max-height: 360px; overflow-y: auto; }
.rank-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
  &:last-child { border-bottom: none; }
}
.rank-no {
  width: 24px; height: 24px;
  border-radius: 4px;
  background: #ebeef5; color: #909399;
  font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.rank-top1 { background: #f56c6c; color: #fff; }
.rank-top2 { background: #e6a23c; color: #fff; }
.rank-top3 { background: #409eff; color: #fff; }
.rank-content { flex: 1; min-width: 0; }
.rank-name { font-size: 13px; color: #303133; margin-bottom: 4px; }
.rank-stats {
  text-align: right;
  font-size: 12px; color: #606266;
  flex-shrink: 0;
  .rank-sub { color: #909399; font-size: 11px; margin-top: 2px; }
}

/* 系统动态 */
.activity-list { max-height: 360px; overflow-y: auto; }
.activity-content {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px;
  .activity-text {
    color: #303133;
    overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  }
}
.activity-meta { font-size: 11px; color: #909399; margin-top: 2px; }

/* 热门文章 */
.hot-list { max-height: 360px; overflow-y: auto; }
.hot-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
  &:hover { background: #f5f7fa; }
  &:last-child { border-bottom: none; }
}
.hot-no {
  width: 24px; height: 24px;
  border-radius: 4px;
  background: #ebeef5; color: #909399;
  font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.hot-body { flex: 1; min-width: 0; }
.hot-title {
  font-size: 13px; color: #303133;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hot-meta {
  font-size: 11px; color: #909399;
  margin-top: 2px;
  display: flex; gap: 6px;
}
.hot-score {
  font-size: 14px; font-weight: 700; color: #f56c6c;
  flex-shrink: 0;
}

/* 配置 */
.config-list { font-size: 13px; }
</style>
