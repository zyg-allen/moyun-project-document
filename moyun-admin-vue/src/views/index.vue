<template>
  <div class="ops-dashboard">
    <!-- ============ ① 平台定位 / 品牌条 ============ -->
    <section class="hero">
      <div class="hero-glow hero-glow-a" />
      <div class="hero-glow hero-glow-b" />
      <div class="hero-main">
        <div class="hero-left">
          <div class="hero-brand">
            <span class="hero-logo">旭</span>
            <div class="hero-title-wrap">
              <h1 class="hero-title">{{ identity.name || '旭林知行' }}</h1>
              <p class="hero-slogan">{{ identity.slogan || '知行合一，助你上岸' }}</p>
            </div>
          </div>
          <p class="hero-positioning">{{ identity.positioning || 'AI 驱动的求职面试与学习成长平台' }}</p>
          <div v-if="identity.strategy" class="hero-strategy">
            <el-icon><Guide /></el-icon>
            <span>{{ identity.strategy }}</span>
          </div>
        </div>
        <div class="hero-right">
          <div class="hero-greet">{{ greeting }}，{{ userStore.name || '管理员' }}</div>
          <div class="hero-date">{{ todayStr }}</div>
          <div class="hero-actions">
            <el-button size="small" type="primary" plain icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
            <el-button
              size="small"
              plain
              icon="Delete"
              :loading="cacheRefreshing"
              v-hasPermi="['system:dashboard:refresh']"
              @click="handleRefreshCache"
            >刷新缓存</el-button>
          </div>
        </div>
      </div>

      <!-- 四端定位小卡 -->
      <div class="hero-platforms">
        <div
          v-for="p in identity.platforms || []"
          :key="p.code"
          class="plat-chip"
          :class="{ 'plat-chip--reserved': p.dataReady === false }"
        >
          <div class="plat-chip-icon">
            <el-icon><component :is="resolveIcon(p.icon)" /></el-icon>
          </div>
          <div class="plat-chip-body">
            <div class="plat-chip-head">
              <span class="plat-chip-name">{{ p.name }}</span>
              <span class="plat-chip-type">{{ p.type }}</span>
              <span v-if="p.dataReady === false" class="plat-chip-badge">预留</span>
            </div>
            <div class="plat-chip-desc">{{ p.description || '—' }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ ② 运营警报（无警报则整条隐藏） ============ -->
    <section v-if="alerts.length" class="alert-bar">
      <div class="alert-bar-title">
        <el-icon><WarningFilled /></el-icon>
        <span>运营提醒</span>
      </div>
      <div class="alert-bar-list">
        <div
          v-for="(a, i) in alerts"
          :key="i"
          class="alert-item"
          :class="'alert-item--' + a.level"
          @click="goPath(a.routePath)"
        >
          <span class="alert-item-title">{{ a.title }}</span>
          <el-tooltip :content="a.detail" placement="top" :show-after="200">
            <el-icon class="alert-item-info"><InfoFilled /></el-icon>
          </el-tooltip>
          <el-icon class="alert-item-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
    </section>

    <!-- ============ ③ 分平台运营统计 ============ -->
    <section v-loading="loading" class="platform-section">
      <div
        v-for="plat in platformStats"
        :key="plat.platformCode"
        class="plat-block"
        :class="{ 'plat-block--reserved': plat.dataReady === false }"
      >
        <!-- 端头部 -->
        <header class="plat-head">
          <div class="plat-head-left">
            <div class="plat-head-icon" :class="'tone-' + platformTone(plat.platformCode)">
              <el-icon><component :is="resolveIcon(plat.icon)" /></el-icon>
            </div>
            <div>
              <div class="plat-head-name">
                {{ plat.platformName }}
                <span class="plat-head-type">{{ plat.platformType }}</span>
                <el-tag v-if="plat.dataReady === false" size="small" type="info" effect="plain">预留端</el-tag>
              </div>
              <div class="plat-head-desc">{{ plat.description }}</div>
            </div>
          </div>
        </header>

        <!-- 预留端占位 -->
        <div v-if="plat.dataReady === false" class="plat-reserved">
          <el-icon><Clock /></el-icon>
          <span>该端已登记但尚未接入业务数据，暂无运营统计。待模块上线后此处自动展示。</span>
        </div>

        <template v-else>
          <!-- 端级 KPI -->
          <div class="kpi-row">
            <div v-for="k in plat.kpis" :key="k.key" class="kpi-card">
              <div class="kpi-label">{{ k.label }}</div>
              <div class="kpi-value" :class="'tone-text-' + (k.tone || 'blue')">
                {{ formatValue(k) }}<span v-if="k.unit" class="kpi-unit">{{ k.unit }}</span>
              </div>
            </div>
          </div>

          <!-- 端内模块统计 -->
          <div class="module-grid">
            <div v-for="m in plat.modules" :key="m.moduleCode" class="module-card">
              <div class="module-head">
                <div class="module-head-left">
                  <el-icon class="module-icon"><component :is="resolveIcon(m.icon)" /></el-icon>
                  <span class="module-name">{{ m.moduleName }}</span>
                </div>
                <el-link
                  v-if="m.routePath"
                  type="primary"
                  :underline="false"
                  class="module-more"
                  @click="goPath(m.routePath)"
                >查看<el-icon><ArrowRight /></el-icon></el-link>
              </div>
              <div class="module-metrics">
                <div v-for="(mm, idx) in m.metrics" :key="mm.key" class="metric-cell">
                  <div class="metric-cell-value" :class="'tone-text-' + (mm.tone || 'blue')">
                    {{ formatValue(mm) }}<span v-if="mm.unit" class="metric-cell-unit">{{ mm.unit }}</span>
                  </div>
                  <div class="metric-cell-label">{{ mm.label }}</div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </section>

    <!-- ============ ④ 待办 / 已办 ============ -->
    <el-row :gutter="16" class="block-row">
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="task-card">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><Bell /></el-icon> 运营待办</span>
              <div class="header-right">
                <el-tag :type="todoTasks.length ? 'warning' : 'info'" size="small">{{ todoTasks.length }}</el-tag>
                <el-link type="primary" :underline="false" class="more-link" @click="goAuditCenter('pending')">
                  审核中心<el-icon class="more-arrow"><ArrowRight /></el-icon>
                </el-link>
              </div>
            </div>
          </template>
          <div v-loading="loading" class="task-list">
            <div v-for="t in todoTasks" :key="'todo_' + t.type + '_' + t.id" class="task-item" @click="goTask(t)">
              <div class="task-main">
                <el-tag :type="priorityTag(t.priority)" size="small" effect="plain">{{ priorityLabel(t.priority) }}</el-tag>
                <span class="task-title">{{ t.title }}</span>
              </div>
              <div class="task-meta">
                <span>{{ t.submitter }}</span>
                <span class="task-time">{{ relativeTime(t.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && !todoTasks.length" description="暂无待办，运营状态良好" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="hover" class="task-card">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><CircleCheck /></el-icon> 我处理的（已办）</span>
              <div class="header-right">
                <el-tag type="success" size="small">{{ myTasks.length }}</el-tag>
                <el-link type="primary" :underline="false" class="more-link" @click="goAuditCenter('done')">
                  审核中心<el-icon class="more-arrow"><ArrowRight /></el-icon>
                </el-link>
              </div>
            </div>
          </template>
          <div v-loading="loading" class="task-list">
            <div v-for="t in myTasks" :key="'done_' + t.type + '_' + t.id" class="task-item" @click="goTask(t)">
              <div class="task-main">
                <el-tag type="success" size="small" effect="plain">{{ t.description || '已办' }}</el-tag>
                <span class="task-title">{{ t.title }}</span>
              </div>
              <div class="task-meta">
                <span>{{ t.submitter }}</span>
                <span class="task-time">{{ relativeTime(t.createTime) }}</span>
              </div>
            </div>
            <el-empty v-if="!loading && !myTasks.length" description="暂无已办记录" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ ⑤ 今日数据 + 趋势 ============ -->
    <el-row :gutter="16" class="block-row">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover" class="today-card-wrap">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><DataLine /></el-icon> 今日运营数据</span>
            </div>
          </template>
          <div class="today-grid">
            <div v-for="t in todayCells" :key="t.key" class="today-cell">
              <div class="today-cell-value" :class="'tone-text-' + t.tone">{{ t.value }}</div>
              <div class="today-cell-label">{{ t.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><TrendCharts /></el-icon> 近 7 天登录趋势</span>
            </div>
          </template>
          <div ref="loginChartRef" class="chart-box" />
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><Histogram /></el-icon> 近 7 天内容发布</span>
            </div>
          </template>
          <div ref="publishChartRef" class="chart-box" />
        </el-card>
      </el-col>
    </el-row>

    <!-- ============ ⑥ 榜单 / 动态 / 系统 ============ -->
    <el-row :gutter="16" class="block-row">
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><TrophyBase /></el-icon> 内容榜单</span>
            </div>
          </template>
          <el-tabs v-model="rankTab" class="rank-tabs">
            <el-tab-pane label="热门文章" name="hot">
              <div v-loading="loading" class="rank-list">
                <div v-for="h in hotArticles" :key="h.id" class="rank-item" @click="goArticle(h)">
                  <div class="rank-no" :class="rankClass(h.rank)">{{ h.rank }}</div>
                  <div class="rank-content">
                    <div class="rank-name">{{ h.title }}</div>
                    <div class="rank-meta">{{ h.author }} · {{ formatNum(h.views) }} 浏览 · {{ formatNum(h.likes) }} 点赞</div>
                  </div>
                </div>
                <el-empty v-if="!loading && !hotArticles.length" description="暂无数据" :image-size="60" />
              </div>
            </el-tab-pane>
            <el-tab-pane label="栏目排行" name="category">
              <div v-loading="loading" class="rank-list">
                <div v-for="r in categoryRanking" :key="r.categoryId" class="rank-item">
                  <div class="rank-no" :class="rankClass(r.rank)">{{ r.rank }}</div>
                  <div class="rank-content">
                    <div class="rank-name">{{ r.categoryName }}</div>
                    <el-progress
                      :percentage="rankPercent(r)"
                      :show-text="false"
                      :stroke-width="6"
                      :color="rankColor(r.rank)"
                    />
                  </div>
                  <div class="rank-stats">
                    <div>{{ formatNum(r.totalViews) }} 浏览</div>
                    <div class="rank-sub">{{ formatNum(r.articleCount) }} 文章</div>
                  </div>
                </div>
                <el-empty v-if="!loading && !categoryRanking.length" description="暂无数据" :image-size="60" />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><List /></el-icon> 平台动态</span>
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
            <el-empty v-if="!loading && !systemActivities.length" description="暂无动态" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="card-header-title"><el-icon><Setting /></el-icon> 系统运行概览</span>
              <el-link type="primary" :underline="false" class="more-link" @click="goPath('/monitor/server-panel')">
                监控<el-icon class="more-arrow"><ArrowRight /></el-icon>
              </el-link>
            </div>
          </template>
          <div v-loading="loading" class="config-list">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="站点名">{{ configOverview.siteName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="站点描述">{{ configOverview.siteDescription || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前版本">{{ configOverview.version || '-' }}</el-descriptions-item>
              <el-descriptions-item label="运行时长">{{ configOverview.uptimeHours || 0 }} 小时</el-descriptions-item>
              <el-descriptions-item label="缓存命中率">
                {{ configOverview.cacheHitRate == null ? '-' : formatRate(configOverview.cacheHitRate) }}
              </el-descriptions-item>
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
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import useUserStore from '@/store/modules/user'
import { getDashboardAll, refreshDashboardCache } from '@/api/system/dashboard'

const router = useRouter()
const userStore = useUserStore()

// ===== 数据 =====
const loading = ref(false)
const cacheRefreshing = ref(false)
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
// 平台定位 / 分平台统计 / 运营警报
const identity = ref({})
const platformStats = ref([])
const alerts = ref([])
const rankTab = ref('hot')

// ===== 计算属性 =====
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

/** 今日运营数据（按运营关注度排序，非堆砌全部字段） */
const todayCells = computed(() => {
  const t = todayStats.value || {}
  return [
    { key: 'uv', label: '今日访客 (UV)', value: formatNum(t.todayVisitors), tone: 'blue' },
    { key: 'pv', label: '今日浏览 (PV)', value: formatNum(t.todayPageViews), tone: 'cyan' },
    { key: 'newUser', label: '新增用户', value: formatNum(t.todayNewUsers), tone: 'purple' },
    { key: 'newArticle', label: '新增内容', value: formatNum(t.todayNewArticles), tone: 'green' },
    { key: 'loginPortal', label: '前台登录', value: formatNum(t.todayPortalLoginUsers), tone: 'blue' },
    { key: 'loginSys', label: '后台登录', value: formatNum(t.todaySysLoginUsers), tone: 'orange' },
    { key: 'loginRate', label: '登录成功率', value: formatRate(t.loginSuccessRate), tone: 'green' },
    { key: 'loginCount', label: '登录次数', value: formatNum(t.todayLoginCount), tone: 'gray' }
  ]
})

// ===== 图表 =====
const loginChartRef = ref(null)
const publishChartRef = ref(null)
let loginChart = null
let publishChart = null

function renderLoginChart() {
  if (!loginChartRef.value) return
  if (!loginChart) loginChart = echarts.init(loginChartRef.value, 'macarons')
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
        name: '发布数',
        type: 'bar',
        barWidth: '50%',
        data: arr,
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
  getDashboardAll()
    .then(res => {
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
      identity.value = d.platformIdentity || {}
      platformStats.value = d.platformStats || []
      alerts.value = d.alerts || []
      nextTick(() => {
        renderLoginChart()
        renderPublishChart()
      })
    })
    .catch(err => {
      ElMessage.error('首页数据加载失败：' + (err?.message || '未知错误'))
    })
    .finally(() => {
      loading.value = false
    })
}

function handleRefreshCache() {
  cacheRefreshing.value = true
  refreshDashboardCache()
    .then(() => {
      ElMessage.success('缓存已刷新')
      loadAll()
    })
    .catch(err => {
      ElMessage.error('刷新缓存失败：' + (err?.message || '未知错误'))
    })
    .finally(() => {
      cacheRefreshing.value = false
    })
}

// ===== 跳转联动 =====
function goPath(path) {
  if (!path) return
  router.push({ path }).catch(() => {
    ElMessage.warning('目标页面不可达：' + path)
  })
}

function goTask(task) {
  if (!task || !task.routePath) return
  const path = task.routePath.replace(/^\/cms\/audit-center/, '/portal/audit-center')
  const query = {}
  if (task.id) query.taskId = task.id
  if (task.taskType || task.type) query.tab = task.taskType || task.type
  router.push({ path, query }).catch(() => {
    ElMessage.warning('目标页面不可达：' + path)
  })
}

function goAuditCenter(tab) {
  router.push({ path: '/portal/audit-center', query: { activeTab: tab } }).catch(() => {
    ElMessage.warning('审核中心页面不可达')
  })
}

function goArticle(article) {
  if (!article || !article.id) return
  router.push({ path: '/cms/article/edit', query: { id: article.id } }).catch(() => {
    ElMessage.warning('文章编辑页不可达')
  })
}

// ===== 格式化工具 =====
function formatNum(v) {
  const n = Number(v || 0)
  if (n >= 100000000) return (n / 100000000).toFixed(1) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
/** KPI/指标卡取值：沿用 formatNum，避免大数字撑破卡片 */
function formatValue(card) {
  return formatNum(card && card.value)
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

// ===== 图标 / 主题色 =====
/** 后端下发的 icon 名 → Element Plus 图标组件（未知时回退，不留空白） */
const ICON_MAP = {
  portal: 'HomeFilled',
  ledger: 'Wallet',
  admin: 'Setting',
  peoples: 'UserFilled',
  documentation: 'Document',
  job: 'Suitcase',
  clipboard: 'Tickets',
  education: 'Reading',
  star: 'Star',
  money: 'Money',
  chart: 'TrendCharts',
  list: 'List',
  monitor: 'Monitor',
  system: 'Tools',
  // 指标图标
  Document: 'Document',
  CircleCheck: 'CircleCheck',
  CircleClose: 'CircleClose',
  Clock: 'Clock',
  View: 'View',
  Star2: 'Star',
  ChatDotRound: 'ChatDotRound',
  Mic: 'Microphone',
  TrendCharts: 'TrendCharts',
  Plus: 'Plus',
  Search: 'Search',
  MagicStick: 'MagicStick',
  Upload: 'Upload',
  DataLine: 'DataLine',
  Medal: 'Medal',
  Trophy: 'Trophy',
  Minus: 'Minus',
  CreditCard: 'CreditCard',
  Tickets: 'Tickets',
  Wallet: 'Wallet',
  User: 'User',
  Grid: 'Grid',
  Cpu: 'Cpu',
  WarningFilled: 'WarningFilled',
  Money: 'Money',
  Coin: 'Coin',
  Timer: 'Timer',
  Finished: 'Finished',
  Document2: 'Document'
}
function resolveIcon(name) {
  return ICON_MAP[name] || 'DataAnalysis'
}
/** 端主题色：门户蓝 / 记账绿 / 管理紫 / 其他灰 */
function platformTone(code) {
  if (code === 'portal') return 'blue'
  if (code === 'ledger') return 'green'
  if (code === 'admin') return 'purple'
  return 'gray'
}
function priorityLabel(p) {
  return { high: '紧急', medium: '普通', low: '低' }[p] || '普通'
}
function priorityTag(p) {
  return { high: 'danger', medium: 'warning', low: 'info' }[p] || 'info'
}
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
  return {
    INSERT: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
    EXPORT: 'warning',
    OTHER: 'info',
    PUBLISH: 'success',
    REGISTER: 'warning',
    NOTIFICATION: 'info'
  }[bt] || 'info'
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
.ops-dashboard {
  padding: 16px;
  background: #f0f2f5;
  min-height: calc(100vh - 84px);
}

/* ============ 品牌 / 定位条 ============ */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 14px;
  padding: 22px 24px 18px;
  margin-bottom: 16px;
  color: #fff;
  background: linear-gradient(120deg, #1f3a8a 0%, #2f6fd0 45%, #17a2b8 100%);
  box-shadow: 0 6px 20px rgba(31, 58, 138, 0.18);
}
.hero-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(38px);
  opacity: 0.45;
  pointer-events: none;
}
.hero-glow-a {
  width: 260px;
  height: 260px;
  right: -60px;
  top: -110px;
  background: #64e3ff;
}
.hero-glow-b {
  width: 200px;
  height: 200px;
  right: 220px;
  bottom: -140px;
  background: #8ab4ff;
}
.hero-main {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
}
.hero-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}
.hero-logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  flex-shrink: 0;
}
.hero-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
  line-height: 1.2;
}
.hero-slogan {
  margin: 2px 0 0;
  font-size: 13px;
  opacity: 0.9;
  letter-spacing: 2px;
}
.hero-positioning {
  position: relative;
  margin: 14px 0 0;
  font-size: 14px;
  font-weight: 500;
  opacity: 0.96;
}
.hero-strategy {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
}
.hero-right {
  position: relative;
  text-align: right;
  min-width: 210px;
}
.hero-greet {
  font-size: 15px;
  font-weight: 600;
}
.hero-date {
  font-size: 12px;
  opacity: 0.85;
  margin-top: 4px;
}
.hero-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* 端定位小卡 */
.hero-platforms {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(215px, 1fr));
  gap: 10px;
  margin-top: 18px;
}
.plat-chip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: background 0.2s, transform 0.2s;
  &:hover {
    background: rgba(255, 255, 255, 0.22);
    transform: translateY(-2px);
  }
  &--reserved {
    opacity: 0.62;
    background: rgba(255, 255, 255, 0.08);
    border-style: dashed;
  }
}
.plat-chip-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}
.plat-chip-body {
  min-width: 0;
}
.plat-chip-head {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.plat-chip-name {
  font-size: 13px;
  font-weight: 600;
}
.plat-chip-type {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.22);
}
.plat-chip-badge {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px dashed rgba(255, 255, 255, 0.4);
}
.plat-chip-desc {
  font-size: 12px;
  opacity: 0.88;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ============ 运营警报 ============ */
.alert-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 16px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 10px;
  border-left: 4px solid #e6a23c;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  flex-wrap: wrap;
}
.alert-bar-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #b88230;
  flex-shrink: 0;
}
.alert-bar-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}
.alert-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: filter 0.2s;
  &:hover {
    filter: brightness(0.97);
  }
  &--danger {
    background: #fef0f0;
    color: #f56c6c;
    border: 1px solid #fbc4c4;
  }
  &--warning {
    background: #fdf6ec;
    color: #b88230;
    border: 1px solid #f5dab1;
  }
  &--info {
    background: #ecf5ff;
    color: #409eff;
    border: 1px solid #b3d8ff;
  }
}
.alert-item-title {
  font-weight: 500;
}
.alert-item-info {
  opacity: 0.7;
}
.alert-item-arrow {
  font-size: 11px;
  opacity: 0.7;
}

/* ============ 分平台区块 ============ */
.platform-section {
  margin-bottom: 4px;
}
.plat-block {
  background: #fff;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  &--reserved {
    background: #fafbfc;
    border: 1px dashed #dcdfe6;
    box-shadow: none;
  }
}
.plat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
}
.plat-head-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.plat-head-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  flex-shrink: 0;
  &.tone-blue {
    background: linear-gradient(135deg, #409eff, #66b1ff);
  }
  &.tone-green {
    background: linear-gradient(135deg, #67c23a, #85ce61);
  }
  &.tone-purple {
    background: linear-gradient(135deg, #7c5cff, #a08cff);
  }
  &.tone-gray {
    background: linear-gradient(135deg, #a8abb2, #c8c9cc);
  }
}
.plat-head-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.plat-head-type {
  font-size: 11px;
  font-weight: 400;
  color: #909399;
  padding: 1px 6px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}
.plat-head-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}

/* 端级 KPI */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin: 14px 0 16px;
}
.kpi-card {
  padding: 12px 14px;
  border-radius: 10px;
  background: linear-gradient(180deg, #fafbfd, #f5f7fa);
  border: 1px solid #eef1f5;
}
.kpi-label {
  font-size: 12px;
  color: #909399;
}
.kpi-value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 4px;
  line-height: 1.2;
}
.kpi-unit {
  font-size: 12px;
  font-weight: 400;
  margin-left: 3px;
  color: #909399;
}

/* 模块网格 */
.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(290px, 1fr));
  gap: 12px;
}
.module-card {
  border: 1px solid #eef1f5;
  border-radius: 10px;
  padding: 12px 14px;
  background: #fff;
  transition: box-shadow 0.2s, transform 0.2s;
  &:hover {
    box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
    transform: translateY(-1px);
  }
}
.module-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ebeef5;
}
.module-head-left {
  display: flex;
  align-items: center;
  gap: 6px;
}
.module-icon {
  color: #409eff;
  font-size: 15px;
}
.module-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.module-more {
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.module-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 8px;
  margin-top: 12px;
}
.metric-cell-value {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.2;
}
.metric-cell-unit {
  font-size: 11px;
  font-weight: 400;
  margin-left: 2px;
  color: #909399;
}
.metric-cell-label {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

/* 主题色文本 */
.tone-text-blue {
  color: #409eff;
}
.tone-text-green {
  color: #67c23a;
}
.tone-text-orange {
  color: #e6a23c;
}
.tone-text-purple {
  color: #7c5cff;
}
.tone-text-red {
  color: #f56c6c;
}
.tone-text-cyan {
  color: #17a2b8;
}
.tone-text-gray {
  color: #a8abb2;
}

/* 预留端占位 */
.plat-reserved {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 4px 6px;
  font-size: 13px;
  color: #909399;
}

/* ============ 卡片通用 ============ */
.block-row {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #303133;
}
.card-header-title {
  display: flex;
  align-items: center;
  gap: 6px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.more-link {
  font-size: 13px;
  font-weight: normal;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.more-arrow {
  font-size: 12px;
}

/* 图表 */
.chart-box {
  height: 250px;
}

/* 待办 / 已办 */
.task-list {
  max-height: 320px;
  overflow-y: auto;
}
.task-item {
  padding: 10px 8px;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
  &:hover {
    background: #f5f7fa;
  }
  &:last-child {
    border-bottom: none;
  }
}
.task-main {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.task-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}
.task-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  padding-left: 56px;
}
.task-time {
  font-size: 11px;
}

/* 今日数据 */
.today-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px 10px;
}
.today-cell-value {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}
.today-cell-label {
  font-size: 12px;
  color: #909399;
  margin-top: 3px;
}

/* 榜单 */
.rank-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 6px;
  }
}
.rank-list {
  max-height: 268px;
  overflow-y: auto;
}
.rank-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
  &:last-child {
    border-bottom: none;
  }
}
.rank-no {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: #ebeef5;
  color: #909399;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rank-top1 {
  background: #f56c6c;
  color: #fff;
}
.rank-top2 {
  background: #e6a23c;
  color: #fff;
}
.rank-top3 {
  background: #409eff;
  color: #fff;
}
.rank-content {
  flex: 1;
  min-width: 0;
}
.rank-name {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 3px;
}
.rank-meta {
  font-size: 11px;
  color: #909399;
}
.rank-stats {
  text-align: right;
  font-size: 12px;
  color: #606266;
  flex-shrink: 0;
}
.rank-sub {
  color: #909399;
  font-size: 11px;
  margin-top: 2px;
}

/* 动态 */
.activity-list {
  max-height: 320px;
  overflow-y: auto;
}
.activity-content {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.activity-text {
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.activity-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

/* 配置 */
.config-list {
  font-size: 13px;
}

/* 窄屏适配 */
@media (max-width: 768px) {
  .hero-right {
    text-align: left;
  }
  .hero-actions {
    justify-content: flex-start;
  }
  .module-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
