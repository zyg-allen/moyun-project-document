<template>
  <view class="page" :style="themeVars">
    <NavBar title="报表中心" />
    <!-- 年份切换 -->
    <view class="year-bar">
      <view class="year-arrow" @tap="changeYear(-1)">‹</view>
      <text class="year-text">{{ year }} 年</text>
      <view class="year-arrow" @tap="changeYear(1)">›</view>
    </view>

    <!-- 年度收支概览 -->
    <view class="card">
      <view class="year-summary">
        <view class="ys-col">
          <view class="ys-label">年收入</view>
          <view class="ys-value income">{{ privacyMode ? '****' : '+' + centToAbsAmount(report.yearIncome || 0) }}</view>
        </view>
        <view class="ys-divider"></view>
        <view class="ys-col">
          <view class="ys-label">年支出</view>
          <view class="ys-value expense">{{ privacyMode ? '****' : '-' + centToAbsAmount(report.yearExpense || 0) }}</view>
        </view>
        <view class="ys-divider"></view>
        <view class="ys-col">
          <view class="ys-label">年结余</view>
          <view class="ys-value">{{ privacyMode ? '****' : centToAmount((report.yearIncome || 0) - (report.yearExpense || 0)) }}</view>
        </view>
      </view>
      <!-- 月度收支柱状图 -->
      <view class="chart-title">月度收支</view>
      <view class="bar-chart">
        <view v-for="m in monthlyTrend" :key="m.month" class="bar-col">
          <view class="bar-pair">
            <view class="bar income" :style="{ height: barHeight(m.income) + 'rpx' }"></view>
            <view class="bar expense" :style="{ height: barHeight(m.expense) + 'rpx' }"></view>
          </view>
          <text class="bar-label">{{ m.month }}</text>
        </view>
      </view>
      <view class="chart-legend">
        <text class="dot income"></text><text class="legend-text">收入</text>
        <text class="dot expense"></text><text class="legend-text">支出</text>
      </view>
    </view>

    <!-- 分类占比 -->
    <view class="card">
      <view class="chart-title">支出分类 TOP</view>
      <view v-if="!categoryExpense.length" class="empty">暂无数据</view>
      <view v-for="(c, i) in categoryExpense.slice(0, 8)" :key="'e' + i" class="rank-row">
        <text class="rank-name">{{ c.name }}</text>
        <view class="rank-bar">
          <view class="rank-inner expense" :style="{ width: rankPercent(c.amount, expenseTotal) + '%' }"></view>
        </view>
        <text class="rank-amount">{{ privacyMode ? '****' : centToAmount(c.amount) }}</text>
      </view>
      <view class="chart-title" style="margin-top: 24rpx">收入分类 TOP</view>
      <view v-if="!categoryIncome.length" class="empty">暂无数据</view>
      <view v-for="(c, i) in categoryIncome.slice(0, 5)" :key="'i' + i" class="rank-row">
        <text class="rank-name">{{ c.name }}</text>
        <view class="rank-bar">
          <view class="rank-inner income" :style="{ width: rankPercent(c.amount, incomeTotal) + '%' }"></view>
        </view>
        <text class="rank-amount">{{ privacyMode ? '****' : centToAmount(c.amount) }}</text>
      </view>
    </view>

    <!-- 净资产趋势（近30天） -->
    <view class="card">
      <view class="chart-title">净资产趋势（近 30 天）</view>
      <view v-if="!netWorthTrend.length" class="empty">暂无快照数据</view>
      <view v-else class="nw-list">
        <view v-for="(p, i) in netWorthPoints" :key="'nw' + i" class="nw-row">
          <text class="nw-date">{{ p.date }}</text>
          <view class="nw-bar-wrap">
            <view class="nw-bar" :style="{ width: nwWidth(p.netWorth) + '%' }"></view>
          </view>
          <text class="nw-value">{{ privacyMode ? '****' : centToAmount(p.netWorth) }}</text>
        </view>
      </view>
    </view>

    <!-- 账户余额分布 -->
    <view class="card">
      <view class="chart-title">账户余额分布</view>
      <view v-if="!accountDistribution.length" class="empty">暂无账户</view>
      <view v-for="(a, i) in accountDistribution" :key="'a' + i" class="rank-row">
        <text class="rank-name">{{ a.name }}</text>
        <view class="rank-bar">
          <view class="rank-inner asset" :style="{ width: rankPercent(a.balance, assetTotal) + '%' }"></view>
        </view>
        <text class="rank-amount">{{ privacyMode ? '****' : centToAmount(a.balance) }}</text>
      </view>
    </view>

    <!-- 在还负债 -->
    <view class="card">
      <view class="chart-title">在还负债</view>
      <view v-if="!liabilityOverview.length" class="empty">无在还负债</view>
      <view v-for="(l, i) in liabilityOverview" :key="'l' + i" class="liab-row">
        <view class="flex-1">
          <view class="liab-name">{{ l.name }}</view>
          <view class="liab-sub">{{ l.dueDate ? '到期 ' + l.dueDate : (l.repaymentDay ? '每月 ' + l.repaymentDay + ' 日还款' : '无还款日') }}</view>
        </view>
        <view class="liab-right">
          <view class="liab-balance">{{ privacyMode ? '****' : centToAmount(l.balance) }}</view>
          <view class="liab-sub" v-if="l.monthlyPayment">月还 {{ privacyMode ? '****' : centToAmount(l.monthlyPayment) }}</view>
        </view>
      </view>
    </view>

    <!-- CSV 导出 -->
    <view class="card">
      <view class="chart-title">数据导出（CSV）</view>
      <view class="export-row">
        <picker mode="date" :value="exportStart" @change="(e) => exportStart = e.detail.value">
          <view class="export-date">{{ exportStart || '开始日期(不限)' }}</view>
        </picker>
        <text class="export-sep">至</text>
        <picker mode="date" :value="exportEnd" @change="(e) => exportEnd = e.detail.value">
          <view class="export-date">{{ exportEnd || '结束日期(不限)' }}</view>
        </picker>
        <view class="btn-export" @tap="doExport">导出</view>
      </view>
    </view>
  </view>
</template>

<script>
import { getReportOverview, exportTransactionsCsv } from '@/api/ledger';
import { centToAmount, centToAbsAmount, toNum } from '@/utils/money';
import { useThemeStore } from '@/stores/theme';

export default {
  data() {
    return {
      year: new Date().getFullYear(),
      report: {},
      privacyMode: false,
      exportStart: '',
      exportEnd: ''
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    monthlyTrend() { return this.report.monthlyTrend || []; },
    categoryExpense() { return this.report.categoryExpense || []; },
    categoryIncome() { return this.report.categoryIncome || []; },
    netWorthTrend() { return this.report.netWorthTrend || []; },
    accountDistribution() { return this.report.accountDistribution || []; },
    liabilityOverview() { return this.report.liabilityOverview || []; },
    maxMonthly() {
      return Math.max(1, ...this.monthlyTrend.map(m => Math.max(m.income || 0, m.expense || 0)));
    },
    expenseTotal() { return this.categoryExpense.reduce((s, c) => s + toNum(c.amount), 0) || 1; },
    incomeTotal() { return this.categoryIncome.reduce((s, c) => s + toNum(c.amount), 0) || 1; },
    assetTotal() { return this.accountDistribution.reduce((s, a) => s + toNum(a.balance), 0) || 1; },
    nwRange() {
      if (!this.netWorthTrend.length) return { min: 0, max: 1 };
      const vals = this.netWorthTrend.map(p => p.netWorth || 0);
      const min = Math.min(...vals), max = Math.max(...vals);
      return { min, max: max > min ? max : min + 1 };
    },
    // 净资产取关键点，最多展示 8 个，避免过密
    netWorthPoints() {
      const list = this.netWorthTrend;
      if (!list.length) return [];
      if (list.length <= 8) return list;
      const step = Math.floor(list.length / 7);
      const points = [];
      for (let i = 0; i < list.length; i += step) points.push(list[i]);
      if (points[points.length - 1] !== list[list.length - 1]) points.push(list[list.length - 1]);
      return points.slice(-8);
    }
  },
  onShow() {
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    this.load();
  },
  methods: {
    centToAmount,
    load() {
      getReportOverview(this.year).then((data) => {
        this.report = data || {};
      }).catch(() => { /* 拦截器已提示 */ });
    },
    changeYear(delta) {
      const y = this.year + delta;
      if (y < 2000 || y > 2100) return;
      this.year = y;
      this.load();
    },
    barHeight(cent) {
      // 最大 160rpx 的柱高
      return Math.round(Math.min((cent || 0) / this.maxMonthly, 1) * 160);
    },
    rankPercent(amount, total) {
      return Math.round((amount / total) * 100);
    },
    nwWidth(val) {
      const { min, max } = this.nwRange;
      return Math.round(((val - min) / (max - min)) * 80) + 20;
    },
    doExport() {
      exportTransactionsCsv(this.exportStart || null, this.exportEnd || null).then(() => {
        uni.showToast({ title: '已导出', icon: 'success' });
      }).catch(() => { /* 已提示 */ });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.year-bar {
  display: flex; align-items: center; justify-content: center; gap: 40rpx;
  background: var(--primary-strong); color: #fff; padding: 28rpx 0;
}
.year-arrow { font-size: 44rpx; padding: 0 24rpx; opacity: 0.85; }
.year-text { font-size: 34rpx; font-weight: 700; }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin: 0 24rpx 24rpx; }
.chart-title { font-size: 28rpx; font-weight: 600; margin: 8rpx 0 20rpx; }
.empty { text-align: center; color: #bbb; padding: 40rpx 0; font-size: 24rpx; }

.year-summary { display: flex; align-items: center; margin-bottom: 24rpx; }
.ys-col { flex: 1; text-align: center; }
.ys-label { font-size: 22rpx; color: #999; margin-bottom: 6rpx; }
.ys-value { font-size: 30rpx; font-weight: 700; }
.ys-value.income { color: #27ae60; }
.ys-value.expense { color: #e74c3c; }
.ys-divider { width: 1rpx; height: 48rpx; background: #f0f0f5; }

/* 月度柱状图 */
.bar-chart { display: flex; align-items: flex-end; height: 200rpx; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; }
.bar-pair { display: flex; align-items: flex-end; gap: 4rpx; height: 160rpx; }
.bar { width: 14rpx; border-radius: 6rpx 6rpx 0 0; min-height: 4rpx; }
.bar.income { background: #27ae60; }
.bar.expense { background: #e74c3c; }
.bar-label { font-size: 20rpx; color: #999; margin-top: 8rpx; }
.chart-legend { display: flex; justify-content: center; gap: 8rpx; margin-top: 16rpx; }
.dot { width: 16rpx; height: 16rpx; border-radius: 8rpx; }
.dot.income { background: #27ae60; }
.dot.expense { background: #e74c3c; }
.legend-text { font-size: 22rpx; color: #999; margin-right: 16rpx; }

/* 分类排名条 */
.rank-row { display: flex; align-items: center; margin-bottom: 16rpx; }
.rank-name { width: 140rpx; font-size: 24rpx; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-bar { flex: 1; height: 16rpx; background: #f5f6f8; border-radius: 8rpx; overflow: hidden; margin: 0 16rpx; }
.rank-inner { height: 100%; border-radius: 8rpx; }
.rank-inner.expense { background: #e74c3c; }
.rank-inner.income { background: #27ae60; }
.rank-inner.asset { background: var(--primary-strong); }
.rank-amount { width: 130rpx; text-align: right; font-size: 22rpx; color: #666; }

/* 净资产趋势 */
.nw-row { display: flex; align-items: center; margin-bottom: 14rpx; }
.nw-date { width: 150rpx; font-size: 20rpx; color: #999; }
.nw-bar-wrap { flex: 1; margin: 0 16rpx; }
.nw-bar { height: 12rpx; background: linear-gradient(90deg, var(--primary-strong), var(--primary)); border-radius: 6rpx; }
.nw-value { width: 150rpx; text-align: right; font-size: 22rpx; color: #333; }

/* 负债 */
.liab-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.liab-row:last-child { border-bottom: none; }
.liab-name { font-size: 27rpx; color: #333; }
.liab-sub { font-size: 20rpx; color: #999; margin-top: 4rpx; }
.liab-right { text-align: right; }
.liab-balance { font-size: 28rpx; font-weight: 600; color: #a04000; }

/* 导出 */
.export-row { display: flex; align-items: center; gap: 12rpx; }
.export-date {
  padding: 10rpx 18rpx; background: #f5f6f8; border-radius: 10rpx;
  font-size: 22rpx; color: #555;
}
.export-sep { font-size: 22rpx; color: #999; }
.btn-export {
  margin-left: auto; padding: 10rpx 40rpx; background: var(--primary-strong); color: #fff;
  border-radius: 10rpx; font-size: 24rpx;
}
</style>
