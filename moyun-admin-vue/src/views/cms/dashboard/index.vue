<template>
  <div class="app-container">
    <!-- 核心指标卡片 -->
    <el-row :gutter="20" class="mb20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>门户用户</span></template>
          <div class="stat-value">{{ stats.userCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>文章总数</span></template>
          <div class="stat-value">{{ stats.articleCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>面试题目</span></template>
          <div class="stat-value">{{ stats.questionCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header><span>今日新增用户</span></template>
          <div class="stat-value" style="color: #67c23a;">{{ stats.todayNewUsers || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-row :gutter="20" class="mb20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>近7天用户登录趋势</span></template>
          <div ref="loginChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>近7天内容发布趋势</span></template>
          <div ref="publishChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门排行 + 审核待办 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>热门文章 Top5</span></template>
          <el-table :data="hotArticles" size="small">
            <el-table-column label="排名" type="index" width="60" align="center" />
            <el-table-column label="标题" prop="title" :show-overflow-tooltip="true" />
            <el-table-column label="阅读量" prop="viewCount" width="100" align="center" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <span>审核待办</span>
            <el-badge :value="pendingCount" :max="99" class="ml10" />
          </template>
          <el-table :data="pendingList" size="small">
            <el-table-column label="类型" prop="type" width="100" align="center" />
            <el-table-column label="标题" prop="title" :show-overflow-tooltip="true" />
            <el-table-column label="提交时间" prop="createTime" width="160" align="center" />
          </el-table>
          <div v-if="pendingCount > 0" style="text-align: center; margin-top: 10px;">
            <el-button type="primary" link @click="goToAudit">前往审核中心</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="BusinessDashboard">
import * as echarts from 'echarts';
import { getDashboardMetrics, getLoginTrend, getPublishTrend, getHotArticles, getTodoTasks } from '@/api/system/dashboard';

const stats = ref({});
const hotArticles = ref([]);
const pendingList = ref([]);
const pendingCount = ref(0);
const loginChartRef = ref(null);
const publishChartRef = ref(null);

let loginChart = null;
let publishChart = null;

function loadStats() {
  getDashboardMetrics().then(res => {
    stats.value = res.data || {};
  });
}

function loadLoginTrend() {
  getLoginTrend().then(res => {
    const data = res.data || [];
    loginChart = echarts.init(loginChartRef.value);
    loginChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: data.map(d => d.date) },
      yAxis: { type: 'value' },
      series: [{ name: '登录人数', type: 'line', data: data.map(d => d.count), smooth: true, areaStyle: {} }]
    });
  });
}

function loadPublishTrend() {
  getPublishTrend().then(res => {
    const data = res.data || [];
    publishChart = echarts.init(publishChartRef.value);
    publishChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['文章', '面经'] },
      xAxis: { type: 'category', data: data.map(d => d.date) },
      yAxis: { type: 'value' },
      series: [
        { name: '文章', type: 'bar', data: data.map(d => d.articleCount) },
        { name: '面经', type: 'bar', data: data.map(d => d.experienceCount) }
      ]
    });
  });
}

function loadHotArticles() {
  getHotArticles().then(res => {
    hotArticles.value = res.data || [];
  });
}

function loadPendingAudit() {
  getTodoTasks().then(res => {
    pendingList.value = (res.data || []).slice(0, 5);
    pendingCount.value = res.total || 0;
  });
}

function goToAudit() {
  router.push('/portal/audit-center');
}

onMounted(() => {
  loadStats();
  loadLoginTrend();
  loadPublishTrend();
  loadHotArticles();
  loadPendingAudit();
  window.addEventListener('resize', () => {
    loginChart && loginChart.resize();
    publishChart && publishChart.resize();
  });
});

onBeforeUnmount(() => {
  loginChart && loginChart.dispose();
  publishChart && publishChart.dispose();
});
</script>

<style scoped>
.stat-value {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  padding: 10px 0;
}
.mb20 {
  margin-bottom: 20px;
}
.ml10 {
  margin-left: 10px;
}
</style>
