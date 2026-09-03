<template>
  <view class="page">
    <!-- ========== 未登录：平台价值与操作引导 ========== -->
    <template v-if="!isLoggedIn">
      <view class="hero brand-hero">
        <view class="brand-name">墨韵记账</view>
        <view class="brand-slogan">看清身家，才敢做决定</view>
        <view class="brand-sub">个人资产负债 · 一目了然</view>
      </view>

      <view class="card intro-card">
        <view class="intro-title">我们解决什么痛点</view>
        <view class="pain-item" v-for="p in pains" :key="p.t">
          <text class="pain-icon">{{ p.i }}</text>
          <view class="flex-1">
            <view class="pain-t">{{ p.t }}</view>
            <view class="pain-d">{{ p.d }}</view>
          </view>
        </view>
      </view>

      <view class="card intro-card">
        <view class="intro-title">三步看清你的身家</view>
        <view class="guide-step">
          <text class="step-num">1</text>
          <view class="flex-1">
            <view class="step-name">把资产和负债都录进来</view>
            <view class="step-desc">现金、银行卡、花呗、房贷、朋友借款……一处汇总</view>
          </view>
        </view>
        <view class="guide-step">
          <text class="step-num">2</text>
          <view class="flex-1">
            <view class="step-name">随手记一笔</view>
            <view class="step-desc">支出/收入/转账/借款/还款，支持凭证截图留痕</view>
          </view>
        </view>
        <view class="guide-step">
          <text class="step-num">3</text>
          <view class="flex-1">
            <view class="step-name">净资产趋势自动生成</view>
            <view class="step-desc">每日快照 + 月度报表，钱去哪了一眼看穿</view>
          </view>
        </view>
      </view>

      <view class="intro-action">
        <view class="btn-login" @tap="goLogin">登录 / 注册，开始记账</view>
        <view class="intro-note">与墨韵门户共用账号，数据云端同步</view>
      </view>
    </template>

    <!-- ========== 已登录：数据总览 + 操作提示 ========== -->
    <template v-else>
    <!-- 操作提示条（可关闭） -->
    <view class="tips-bar" v-if="showTips">
      <text class="tips-text">记一笔支持备注、分类打标签和凭证截图；还款余额不足会提示补录资金来源</text>
      <text class="tips-close" @tap="dismissTips">×</text>
    </view>

    <!-- 新手引导（无任何账户时显示） -->
    <view class="guide-card" v-if="showGuide">
      <view class="guide-title">欢迎使用记账 · 3 步开始</view>
      <view class="guide-step" @tap="goTab('/pages/asset/index')">
        <text class="step-num">1</text>
        <view class="flex-1">
          <view class="step-name">添加一个资产账户</view>
          <view class="step-desc">现金、银行卡、支付宝等，填当前余额即可</view>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="guide-step" @tap="goTab('/pages/record/index')">
        <text class="step-num">2</text>
        <view class="flex-1">
          <view class="step-name">记第一笔账</view>
          <view class="step-desc">支出/收入/转账，支持附凭证截图</view>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="guide-step" @tap="goTab('/pages/mine/index')">
        <text class="step-num">3</text>
        <view class="flex-1">
          <view class="step-name">设置月预算（可选）</view>
          <view class="step-desc">超支 80%/100% 会自动提醒你</view>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="guide-close" @tap="dismissGuide">我知道了，不再显示</view>
    </view>

    <!-- 净资产主卡 -->
    <view class="hero">
      <view class="hero-top">
        <view class="hero-label">我的净资产</view>
        <view class="privacy-toggle" @tap="togglePrivacy">{{ privacyMode ? '隐私 开' : '隐私 关' }}</view>
      </view>
      <view class="hero-amount">
        <text v-if="!privacyMode">¥ {{ netWorthText }}</text>
        <text v-else>¥ ****</text>
      </view>
      <view class="hero-change" v-if="!privacyMode && dashboard.netWorthChange !== 0" :class="changeClass">
        较昨日 {{ changeText }}
      </view>
      <view class="hero-row">
        <view class="hero-cell">
          <view class="cell-label">总资产</view>
          <view class="cell-value">{{ privacyMode ? '****' : assetText }}</view>
        </view>
        <view class="hero-divider"></view>
        <view class="hero-cell">
          <view class="cell-label">总负债</view>
          <view class="cell-value">{{ privacyMode ? '****' : liabilityText }}</view>
        </view>
      </view>
    </view>

    <!-- 本月收支 -->
    <view class="card month-card">
      <view class="card-title flex-row">
        <text class="flex-1">本月收支</text>
        <text class="link" @tap="goReport">报表</text>
      </view>
      <view class="month-row">
        <view class="month-col">
          <view class="month-label">收入</view>
          <view class="month-value income">{{ privacyMode ? '****' : '+' + monthIncomeText }}</view>
        </view>
        <view class="month-col">
          <view class="month-label">支出</view>
          <view class="month-value expense">{{ privacyMode ? '****' : '-' + monthExpenseText }}</view>
        </view>
      </view>
      <!-- 预算进度条 -->
      <view class="budget-line" v-if="budget && budget.amount > 0">
        <view class="budget-text">
          <text>月预算 {{ budgetAmountText }} · 已用 {{ budgetUsedText }}</text>
          <text :class="budget.rate > 100 ? 'over' : ''">{{ budget.rate }}%</text>
        </view>
        <view class="budget-bar">
          <view class="budget-inner" :class="{ over: budget.rate > 100 }" :style="{ width: Math.min(budget.rate, 100) + '%' }"></view>
        </view>
      </view>
    </view>

    <!-- 最近流水 -->
    <view class="card">
      <view class="card-title flex-row">
        <text class="flex-1">最近流水</text>
        <text class="link" @tap="goRecordList">全部</text>
      </view>
      <view v-if="recentList.length === 0" class="empty">暂无流水，点下方「记一笔」开始记账</view>
      <view v-for="t in recentList" :key="t.id" class="txn-row" @tap="goEdit(t)">
        <view class="txn-icon" :class="t.type">{{ typeText(t.type).slice(0, 1) }}</view>
        <view class="flex-1">
          <view class="txn-name">{{ t.description || typeText(t.type) }}</view>
          <view class="txn-date">{{ t.transactionDate }}</view>
        </view>
        <view class="txn-amount" :class="t.type">{{ amountOf(t) }}</view>
      </view>
    </view>
    </template>
  </view>
</template>

<script>
import { getDashboard } from '@/api/ledger';
import { centToYuan, centToAmount, centToSigned, typeText } from '@/utils/money';
import { useUserStore } from '@/stores/user';

export default {
  data() {
    return {
      dashboard: {},
      privacyMode: false,
      pains: [
        { i: '💸', t: '钱花哪了说不清', d: '账单散落各处，月底一对账就懵' },
        { i: '📉', t: '净资产是笔糊涂账', d: '资产、负债、花呗分期从来没人帮你算总账' },
        { i: '🤝', t: '借出去的钱没人管', d: '朋友借款、还款随手一记，到期不再尴尬' }
      ]
    };
  },
  computed: {
    isLoggedIn() { return useUserStore().isLoggedIn; },
    showTips() { return uni.getStorageSync('ledger_tips_dismissed') !== '1'; },
    netWorthText() { return centToAmount(this.dashboard.netWorth); },
    assetText() { return centToAmount(this.dashboard.totalAsset); },
    liabilityText() { return centToAmount(this.dashboard.totalLiability); },
    changeText() { return centToSigned(this.dashboard.netWorthChange); },
    changeClass() { return this.dashboard.netWorthChange >= 0 ? 'up' : 'down'; },
    monthIncomeText() { return centToAmount(this.dashboard.monthIncome); },
    monthExpenseText() { return centToAmount(this.dashboard.monthExpense); },
    budget() { return this.dashboard.budget || null; },
    budgetAmountText() { return centToAmount(this.budget && this.budget.amount); },
    budgetUsedText() { return centToAmount(this.budget && this.budget.used); },
    recentList() { return this.dashboard.recentTransactions || []; },
    // 新手引导：无任何资产/负债账户，且未手动关闭过
    showGuide() {
      const dismissed = uni.getStorageSync('ledger_guide_dismissed') === '1';
      if (dismissed) return false;
      return !Number(this.dashboard.assetAccountCount) && !Number(this.dashboard.liabilityAccountCount);
    }
  },
  onShow() {
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    const userStore = useUserStore();
    if (!userStore.isLoggedIn) {
      // 未登录：不请求接口（避免 401 提示），仅展示平台价值
      this.dashboard = {};
      return;
    }
    this.load();
  },
  onPullDownRefresh() {
    const userStore = useUserStore();
    if (!userStore.isLoggedIn) {
      uni.stopPullDownRefresh();
      return;
    }
    this.load().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    typeText,
    togglePrivacy() {
      this.privacyMode = !this.privacyMode;
      uni.setStorageSync('ledger_privacy', this.privacyMode ? '1' : '0');
    },
    async load() {
      try {
        this.dashboard = await getDashboard() || {};
      } catch (e) { /* 拦截器已提示 */ }
    },
    amountOf(t) {
      if (t.type === 'income') return '+' + centToYuan(t.amount);
      if (t.type === 'expense') return '-' + centToYuan(t.amount);
      if (t.type === 'adjust') return centToSigned(t.amount);
      return centToYuan(t.amount);
    },
    goRecordList() {
      uni.navigateTo({ url: '/pages/record/list' });
    },
    goReport() {
      uni.navigateTo({ url: '/pages/report/index' });
    },
    goEdit(t) {
      uni.navigateTo({ url: '/pages/record/edit?id=' + t.id });
    },
    goTab(url) {
      uni.switchTab({ url });
    },
    goLogin() {
      uni.switchTab({ url: '/pages/mine/index' });
    },
    dismissTips() {
      uni.setStorageSync('ledger_tips_dismissed', '1');
    },
    dismissGuide() {
      uni.setStorageSync('ledger_guide_dismissed', '1');
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
/* ===== 未登录品牌与价值展示 ===== */
.brand-hero { text-align: center; padding-top: 100rpx; padding-bottom: 80rpx; }
.brand-name { font-size: 52rpx; font-weight: 800; letter-spacing: 4rpx; }
.brand-slogan { font-size: 32rpx; margin-top: 20rpx; font-weight: 600; }
.brand-sub { font-size: 24rpx; opacity: 0.75; margin-top: 12rpx; }
.intro-card { padding: 28rpx; }
.intro-title { font-size: 30rpx; font-weight: 700; color: #333; margin-bottom: 12rpx; }
.pain-item { display: flex; align-items: flex-start; padding: 18rpx 0; }
.pain-icon { font-size: 40rpx; margin-right: 20rpx; }
.pain-t { font-size: 28rpx; color: #333; font-weight: 600; }
.pain-d { font-size: 23rpx; color: #999; margin-top: 4rpx; }
.intro-action { margin: 8rpx 24rpx 24rpx; }
.btn-login {
  background: #6a4fd4; color: #fff; border-radius: 44rpx; height: 88rpx;
  line-height: 88rpx; text-align: center; font-size: 30rpx; font-weight: 600;
}
.intro-note { font-size: 22rpx; color: #999; text-align: center; margin-top: 16rpx; }

/* ===== 登录后操作提示条 ===== */
.tips-bar {
  display: flex; align-items: center; background: #f0ebfb; margin: 24rpx 24rpx 0;
  border-radius: 12rpx; padding: 14rpx 20rpx;
}
.tips-text { flex: 1; font-size: 22rpx; color: #6a4fd4; line-height: 1.5; }
.tips-close { color: #a99ae0; font-size: 34rpx; padding: 0 8rpx; }

/* 新手引导卡片 */
.guide-card {
  background: #fff; border-radius: 20rpx; margin: 24rpx 24rpx 0; padding: 28rpx;
}
.guide-title { font-size: 30rpx; font-weight: 700; color: #333; margin-bottom: 20rpx; }
.guide-step {
  display: flex; align-items: center; padding: 18rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.guide-step:last-of-type { border-bottom: none; }
.step-num {
  width: 44rpx; height: 44rpx; border-radius: 22rpx; background: #6a4fd4; color: #fff;
  font-size: 24rpx; font-weight: 600; display: flex; align-items: center; justify-content: center;
  margin-right: 20rpx; flex-shrink: 0;
}
.step-name { font-size: 27rpx; color: #333; }
.step-desc { font-size: 22rpx; color: #999; margin-top: 4rpx; }
.guide-card .arrow { color: #ccc; font-size: 32rpx; }
.guide-close {
  margin-top: 16rpx; text-align: center; font-size: 24rpx; color: #999;
  padding-top: 16rpx; border-top: 1rpx solid #f5f5f7;
}
.hero {
  background: linear-gradient(135deg, #6a4fd4, #8a6fe8);
  color: #fff; padding: 60rpx 40rpx 40rpx; margin-bottom: 24rpx;
}
.hero-label { font-size: 26rpx; opacity: 0.8; }
.hero-top { display: flex; justify-content: space-between; align-items: center; }
.privacy-toggle {
  font-size: 22rpx; color: #fff; background: rgba(255,255,255,0.18);
  border-radius: 24rpx; padding: 6rpx 20rpx;
}
.hero-amount { font-size: 72rpx; font-weight: 700; margin: 16rpx 0 8rpx; }
.hero-change { font-size: 24rpx; margin-bottom: 24rpx; }
.hero-change.up { color: #d4ffd4; }
.hero-change.down { color: #ffd4d4; }
.hero-row { display: flex; align-items: center; background: rgba(255,255,255,0.12); border-radius: 16rpx; padding: 24rpx 0; }
.hero-cell { flex: 1; text-align: center; }
.cell-label { font-size: 24rpx; opacity: 0.8; margin-bottom: 8rpx; }
.cell-value { font-size: 34rpx; font-weight: 600; }
.hero-divider { width: 1rpx; height: 48rpx; background: rgba(255,255,255,0.25); }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin: 0 24rpx 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 20rpx; }
.link { color: #6a4fd4; font-size: 26rpx; font-weight: 400; }
.month-row { display: flex; }
.month-col { flex: 1; }
.month-label { font-size: 24rpx; color: #999; margin-bottom: 8rpx; }
.month-value { font-size: 40rpx; font-weight: 700; }
.month-value.income { color: #27ae60; }
.month-value.expense { color: #e74c3c; }
.budget-line { margin-top: 24rpx; }
.budget-text { display: flex; justify-content: space-between; font-size: 24rpx; color: #999; margin-bottom: 12rpx; }
.budget-bar { height: 12rpx; background: #f0f0f5; border-radius: 6rpx; overflow: hidden; }
.budget-inner { height: 100%; background: #6a4fd4; border-radius: 6rpx; }
.budget-inner.over { background: #e74c3c; }
.budget-text .over { color: #e74c3c; font-weight: 600; }

.empty { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }
.txn-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.txn-row:last-child { border-bottom: none; }
.txn-icon {
  width: 72rpx; height: 72rpx; border-radius: 36rpx; margin-right: 20rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 28rpx; font-weight: 600; color: #fff; background: #6a4fd4;
}
.txn-icon.expense { background: #e74c3c; }
.txn-icon.income { background: #27ae60; }
.txn-icon.transfer { background: #4a90d9; }
.txn-icon.repayment { background: #a04000; }
.txn-icon.borrow { background: #8e44ad; }
.txn-icon.adjust { background: #95a5a6; }
.txn-name { font-size: 28rpx; }
.txn-date { font-size: 22rpx; color: #bbb; margin-top: 4rpx; }
.txn-amount { font-size: 30rpx; font-weight: 600; }
.txn-amount.income { color: #27ae60; }
.txn-amount.expense { color: #e74c3c; }
</style>
