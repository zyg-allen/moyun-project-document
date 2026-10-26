<template>
  <view class="page" :style="themeVars">
    <!-- ========== 未登录：平台价值与操作引导 ========== -->
    <template v-if="!isLoggedIn">
      <view class="hero brand-hero">
        <view class="brand-name">墨韵记账</view>
        <view class="brand-slogan">记账 + 库存，个人和小生意的一本账</view>
        <view class="brand-sub">资产 · 负债 · 收支 · 库存，一处算清</view>
      </view>

      <view class="card intro-card">
        <view class="intro-title">你是否也有这些烦恼</view>
        <view class="pain-item" v-for="p in pains" :key="p.t">
          <text class="pain-icon">{{ p.i }}</text>
          <view class="flex-1">
            <view class="pain-t">{{ p.t }}</view>
            <view class="pain-d">{{ p.d }}</view>
          </view>
        </view>
      </view>

      <view class="card intro-card">
        <view class="intro-title">三步了解你的身家</view>
        <view class="guide-step">
          <text class="step-num">1</text>
          <view class="flex-1">
            <view class="step-name">把资产和货录进来</view>
            <view class="step-desc">现金、银行卡、房贷，以及仓库里的商品，先摸清家底</view>
          </view>
        </view>
        <view class="guide-step">
          <text class="step-num">2</text>
          <view class="flex-1">
            <view class="step-name">日常随手记一笔</view>
            <view class="step-desc">收支、转账、借还款，进出货也能记，可附凭证留痕</view>
          </view>
        </view>
        <view class="guide-step">
          <text class="step-num">3</text>
          <view class="flex-1">
            <view class="step-name">净资产与毛利自动算清</view>
            <view class="step-desc">每天快照净资产；库存成本与销售毛利同步更新</view>
          </view>
        </view>
      </view>

      <view class="intro-action">
        <view class="btn-login" @tap="goLogin">登录 / 注册，开始理账</view>
        <view class="intro-note">门户账号可直接登录，无需重复注册</view>
      </view>
    </template>

    <!-- ========== 已登录：数据总览 + 操作提示 ========== -->
    <template v-else>

    <!-- 新手引导（无任何账户时显示） -->
    <view class="guide-card" v-if="showGuide">
      <view class="guide-title">欢迎使用记账 · 3 步开始</view>
      <view class="guide-step" @tap="goTab('/pages/portfolio/index')">
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
      <view class="hero-greet">{{ greeting }} · {{ todayLabel }}</view>
      <view class="hero-top">
        <view class="hero-label">我的净资产</view>
        <view class="privacy-toggle" @tap="togglePrivacy">{{ privacyMode ? '隐私 开' : '隐私 关' }}</view>
      </view>
      <view class="hero-amount">
        <text v-if="!privacyMode">{{ netWorthText }}<text class="hero-unit"> 元</text></text>
        <text v-else>****</text>
      </view>
      <view class="hero-change" v-if="!privacyMode && dashboard.netWorthChange !== 0" :class="changeClass">
        较昨日 {{ changeText }}
      </view>
      <view class="hero-row">
        <view class="hero-cell">
          <view class="cell-label">总资产</view>
          <view class="cell-value asset">{{ privacyMode ? '****' : assetText }} 元</view>
        </view>
        <view class="hero-divider"></view>
        <view class="hero-cell">
          <view class="cell-label">总负债</view>
          <view class="cell-value liability">{{ privacyMode ? '****' : liabilityText }} 元</view>
        </view>
      </view>
    </view>

    <!-- 快捷操作：把最高频的四个动作提到首屏 -->
    <view class="card quick-card">
      <view class="quick-grid">
        <view class="quick-item" @tap="goTab('/pages/record/index')">
          <view class="quick-icon">✏️</view>
          <text class="quick-label">记一笔</text>
        </view>
        <view class="quick-item" @tap="goReport">
          <view class="quick-icon">📊</view>
          <text class="quick-label">报表</text>
        </view>
        <view class="quick-item" @tap="goAnalysis">
          <view class="quick-icon">🧮</view>
          <text class="quick-label">财务分析</text>
        </view>
        <view class="quick-item" @tap="goMemo">
          <view class="quick-icon">📝</view>
          <text class="quick-label">待办</text>
        </view>
      </view>
    </view>
    <!-- 操作提示条（可关闭） -->
    <view class="tips-bar" v-if="showTips">
      <text class="tips-text">记一笔支持备注、分类打标签和凭证截图；还款余额不足会提示补录资金来源</text>
      <text class="tips-close" @tap="dismissTips">×</text>
    </view>
    <!-- 本月收支 + 预算，右上「更多」进报表中心 -->
    <view class="card month-card">
      <view class="card-title flex-row">
        <text class="flex-1">本月收支</text>
        <text class="link" @tap="goReport">更多 ›</text>
      </view>
      <view class="month-row">
        <view class="month-col">
          <view class="month-label">收入</view>
          <view class="month-value income">{{ privacyMode ? '****' : '+' + monthIncomeText + ' 元' }}</view>
        </view>
        <view class="month-col">
          <view class="month-label">支出</view>
          <view class="month-value expense">{{ privacyMode ? '****' : '-' + monthExpenseText + ' 元' }}</view>
        </view>
        <view class="month-col">
          <view class="month-label">结余</view>
          <view class="month-value" :class="monthBalanceClass">{{ privacyMode ? '****' : monthBalanceText + ' 元' }}</view>
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
    <!-- 财务分析：右上「更多」进入分析页（已从底部导航移出） -->
    <view class="card">
      <view class="card-title flex-row">
        <text class="flex-1">财务分析</text>
        <text class="link" @tap="goAnalysis">更多 ›</text>
      </view>
      <view class="entry-sub" style="padding-bottom: 8rpx;">资产负债 · 收支结构 · 六项比率 · 健康评分</view>
    </view>

    <!-- 仓库管理（库存开启时）：概览数字 + 快捷入口 -->
    <view class="card" v-if="warehouseOn">
      <view class="card-title flex-row">
        <text class="flex-1">仓库管理</text>
        <text class="link" @tap="goWarehouse">进入 ›</text>
      </view>
      <view class="wh-stats" v-if="overview.productCount !== undefined">
        <view class="wh-cell">
          <view class="wh-num">{{ formatAmount(overview.stockValue) }} 元</view>
          <view class="wh-label">库存总值</view>
        </view>
        <view class="wh-cell">
          <view class="wh-num">{{ overview.totalQuantity || 0 }}</view>
          <view class="wh-label">待售件数</view>
        </view>
        <view class="wh-cell">
          <view class="wh-num">{{ overview.productCount || 0 }}</view>
          <view class="wh-label">商品数</view>
        </view>
      </view>
      <view class="wh-tip" v-if="overview.zeroStockCount > 0">⚠ {{ overview.zeroStockCount }} 个商品零库存，可考虑补货</view>
      <view class="wh-tip" v-if="overview.expiredCount > 0">⛔ {{ overview.expiredCount }} 个商品已过期</view>
      <view class="entry-row" @tap="goInout">
        <view class="flex-1">
          <view class="entry-title">今日出入库</view>
          <view class="entry-sub">入库 / 出库 / 移库，库存与成本自动联动</view>
        </view>
        <text class="entry-arrow">›</text>
      </view>
    </view>
    <!-- 待办事项（来自备忘录） -->
    <view class="card" v-if="pendingTodos.length">
      <view class="card-title flex-row">
        <text class="flex-1">待办事项</text>
        <text class="link" @tap="goMemo">全部 ›</text>
      </view>
      <view v-for="t in pendingTodos.slice(0, 3)" :key="t.id" class="todo-row" @tap="goMemo">
        <view class="todo-dot"></view>
        <text class="todo-text flex-1">{{ t.title || t.content }}</text>
        <text class="todo-date" v-if="t.todoDate">{{ t.todoDate }}</text>
      </view>
    </view>

    <!-- 最近流水 -->
    <view class="card">
      <view class="card-title flex-row">
        <text class="flex-1">最近流水</text>
        <text class="link" @tap="goRecordList">全部 ›</text>
      </view>
      <view v-if="recentList.length === 0" class="e-empty">
        <text class="e-empty-emoji">🧾</text>
        <text class="e-empty-title">还没有流水</text>
        <text class="e-empty-sub">记下第一笔，报表与预算才会开始有数</text>
        <view class="e-empty-action" @tap="goTab('/pages/record/index')">去记一笔</view>
      </view>
      <view v-for="t in recentList" :key="t.id" class="txn-row" @tap="goEdit(t)">
        <view class="txn-icon" :class="t.type">{{ typeText(t.type).slice(0, 1) }}</view>
        <!-- min-w-0 必须加：flex:1 的默认 min-width:auto 会让长备注撑破整行，
             把金额挤出屏幕，同时使 .txn-name 的 ellipsis 失效 -->
        <view class="flex-1 min-w-0">
          <view class="txn-name">{{ truncate(t.description, 14) || typeText(t.type) }}<text v-if="t.status === 0" class="deleted-tag">已删除</text></view>
          <view class="txn-date">{{ t.transactionDate }} · {{ typeText(t.type) }}</view>
        </view>
        <view class="txn-amount" :class="t.type">{{ amountOf(t) }}<text class="txn-unit">元</text></view>
      </view>
      <!-- 还有更多：提示"往上滑动"，同时保留点击（滑动是隐式入口，点击是显式入口）。
           刻意不显示条数 —— 用户关心的是"还有没有"，不是"现在几条"。 -->
      <view class="txn-more" v-if="recentList.length && txnPage < txnPages" @tap="loadMoreTxns">
        {{ txnLoading ? '加载中…' : '往上滑动显示更多' }}
      </view>
      <view class="txn-more done" v-else-if="recentList.length">已加载全部</view>
    </view>
    </template>
  </view>
</template>

<script>
import { getDashboard, listMemos, pageTransactions, listWarehouses, getWarehouseOverview } from '@/api/ledger';
import { toFixedYuan, formatAmount, formatSigned, typeText, toNum } from '@/utils/money';
import { truncate } from '@/utils/text';
import { useUserStore } from '@/stores/user';
import { useThemeStore } from '@/stores/theme';
import { isWarehouseEnabled } from '@/utils/warehouseSwitch';

/** 首页「最近流水」每页条数（分页请求与"是否到底"的兜底判定共用同一常量） */
const TXN_PAGE_SIZE = 10;

export default {
  data() {
    return {
      dashboard: {},
      privacyMode: false,
      // 库存开关：决定总览是否展示「经营」卡（出入库 tab 常驻）
      warehouseOn: false,
      // 库存概览（仓库开关开启时加载）
      overview: {},
      todos: [],
      // ---- 最近流水分页（加载更多，每页 10 条）----
      txnList: [],
      txnPage: 1,
      txnPages: 1,
      txnTotal: 0,
      txnLoading: false,
      pains: [
        { i: '💸', t: '钱花哪了说不清', d: '只记流水不分类，月底一对账就懵' },
        { i: '📉', t: '净资产是笔糊涂账', d: '存款、房贷、花呗散落各处，总账没人帮你算' },
        { i: '🤝', t: '借出去的钱没着落', d: '朋友借款靠记忆，借了多少还了多少全凭印象' },
        { i: '📦', t: '货压了多少钱算不清', d: '进货、卖货、库存成本散在表格里，毛利全靠估' }
      ]
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    isLoggedIn() { return useUserStore().isLoggedIn; },
    greeting() {
      const h = new Date().getHours();
      if (h < 6) return '夜深了';
      if (h < 11) return '早上好';
      if (h < 14) return '中午好';
      if (h < 18) return '下午好';
      return '晚上好';
    },
    todayLabel() {
      const d = new Date();
      const w = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()];
      return (d.getMonth() + 1) + '月' + d.getDate() + '日 周' + w;
    },
    monthBalanceText() {
      return formatSigned(toNum(this.dashboard.monthIncome) - toNum(this.dashboard.monthExpense));
    },
    monthBalanceClass() {
      return toNum(this.dashboard.monthIncome) - toNum(this.dashboard.monthExpense) >= 0 ? 'income' : 'expense';
    },
    showTips() { return uni.getStorageSync('ledger_tips_dismissed') !== '1'; },
    netWorthText() { return formatAmount(this.dashboard.netWorth); },
    assetText() { return formatAmount(this.dashboard.totalAsset); },
    liabilityText() { return formatAmount(this.dashboard.totalLiability); },
    changeText() { return formatSigned(this.dashboard.netWorthChange); },
    changeClass() { return this.dashboard.netWorthChange >= 0 ? 'up' : 'down'; },
    monthIncomeText() { return formatAmount(this.dashboard.monthIncome); },
    monthExpenseText() { return formatAmount(this.dashboard.monthExpense); },
    budget() { return this.dashboard.budget || null; },
    budgetAmountText() { return formatAmount(this.budget && this.budget.amount); },
    budgetUsedText() { return formatAmount(this.budget && this.budget.used); },
    recentList() { return this.txnList; },
    pendingTodos() { return (this.todos || []).filter(t => !t.done); },
    // 新手引导：无任何资产/负债账户，且未手动关闭过
    showGuide() {
      const dismissed = uni.getStorageSync('ledger_guide_dismissed') === '1';
      if (dismissed) return false;
      return !Number(this.dashboard.assetAccountCount) && !Number(this.dashboard.liabilityAccountCount);
    }
  },
  onShow() {
    useThemeStore().restore();
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    this.warehouseOn = isWarehouseEnabled();
    if (this.warehouseOn) this.loadWarehouse();
    const userStore = useUserStore();
    if (!userStore.isLoggedIn) {
      // 未登录：不请求接口（避免 401 提示），仅展示平台价值
      this.dashboard = {};
      return;
    }
    this.load();
  },
  /** 触底自动加载下一页（页级滚动） */
  onReachBottom() {
    this.loadMoreTxns();
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
    formatAmount,
    formatSigned,
    typeText,
    truncate,
    togglePrivacy() {
      this.privacyMode = !this.privacyMode;
      uni.setStorageSync('ledger_privacy', this.privacyMode ? '1' : '0');
    },
    async load() {
      try {
        this.dashboard = await getDashboard() || {};
      } catch (e) { /* 拦截器已提示 */ }
      this.loadTransactions(true);   // 流水从第 1 页重新取
      this.loadTodos(); // v11.33 首页待办（备忘录）
    },
    /**
     * 加载最近流水（分页）。
     *
     * @param reset true=从第 1 页重新加载（首次进入 / 下拉刷新）；
     *              false=追加下一页（触底 / 点「加载更多」）
     */
    async loadTransactions(reset = false) {
      if (this.txnLoading) return;                          // 并发保护
      if (!reset && this.txnPage >= this.txnPages) return;   // 已到底不再请求
      this.txnLoading = true;
      try {
        const page = reset ? 1 : this.txnPage + 1;
        const res = await pageTransactions({ pageNum: page, pageSize: TXN_PAGE_SIZE }) || {};
        const records = res.records || [];
        this.txnList = reset ? records : this.txnList.concat(records);
        this.txnPage = page;
        // pages 以接口返回为准；接口没给才按 total/每页条数兜底。
        // ⚠️ 不能写 `res.pages || 1` —— 接口漏返回 pages 时会被兜成 1，
        //    于是第一页就被判成"已到底"，出现"只加载了 10 条却提示已加载全部"。
        this.txnPages = Number(res.pages) > 0
          ? Number(res.pages)
          : Math.max(1, Math.ceil((Number(res.total) || records.length) / TXN_PAGE_SIZE));
        this.txnTotal = res.total || records.length;
      } catch (e) { /* 拦截器已提示 */ } finally {
        this.txnLoading = false;
      }
    },
    loadMoreTxns() { this.loadTransactions(false); },
    async loadTodos() {
      try {
        const data = await listMemos() || {};
        this.todos = (data.records || []).map(t => ({ ...t, done: !!t.done }));
      } catch (e) { /* 拦截器已提示 */ }
    },
    goMemo() {
      uni.navigateTo({ url: '/pages/mine/memo/index' });
    },
    amountOf(t) {
      if (t.type === 'income') return '+' + formatAmount(t.amount);
      if (t.type === 'expense') return '-' + formatAmount(t.amount);
      if (t.type === 'adjust') return formatSigned(t.amount);
      return formatAmount(t.amount);
    },
    goRecordList() {
      uni.navigateTo({ url: '/pages/record/list' });
    },
    /** 财务分析（已从底部导航移出，改为二级页） */
    goAnalysis() {
      uni.navigateTo({ url: '/pages/analysis/index' });
    },
    /** 库存概览：取第一个仓库（后续可支持默认仓） */
    async loadWarehouse() {
      try {
        const list = await listWarehouses() || [];
        if (!list.length) { this.overview = {}; return; }
        const wh = list[0];
        this.overview = (await getWarehouseOverview(wh.id)) || {};
      } catch (e) { this.overview = {}; }
    },
    /** 库存管理（经营用户） */
    goWarehouse() {
      uni.navigateTo({ url: '/pages/warehouse/index' });
    },
    /** 出入库是 tab 页，必须 switchTab */
    goInout() {
      uni.switchTab({ url: '/pages/warehouse/inout/index' });
    },    goReport() {
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
/* 未登录品牌区：白卡底，品牌名用强调色做识别，副文案走次级灰 */
.brand-hero { text-align: center; padding-top: calc(var(--status-bar-height) + 64rpx); padding-bottom: 56rpx; }
.brand-name { font-size: 46rpx; font-weight: 700; letter-spacing: 2rpx; color: var(--primary-strong); }
.brand-slogan { font-size: 30rpx; margin-top: 16rpx; font-weight: 600; color: var(--text-main); }
.brand-sub { font-size: 24rpx; color: var(--text-sub); margin-top: 12rpx; }
.intro-card { padding: 28rpx; }
.intro-title { font-size: 30rpx; font-weight: 700; color: var(--text-main); margin-bottom: 12rpx; padding-left: 18rpx; border-left: 6rpx solid var(--primary); line-height: 1.2; }
.pain-item { display: flex; align-items: flex-start; padding: 18rpx 0; }
.pain-icon { font-size: 40rpx; margin-right: 20rpx; }
.pain-t { font-size: 28rpx; color: var(--text-main); font-weight: 600; }
.pain-d { font-size: 23rpx; color: var(--text-sub); margin-top: 4rpx; }
.intro-action { margin: 8rpx 24rpx 24rpx; }
.btn-login {
  background: var(--primary-strong); color: var(--on-primary); border-radius: 44rpx; height: 88rpx;
  line-height: 88rpx; text-align: center; font-size: 30rpx; font-weight: 600;
}
.intro-note { font-size: 22rpx; color: var(--text-sub); text-align: center; margin-top: 16rpx; }

/* ===== 登录后操作提示条 ===== */
.tips-bar {
  display: flex; align-items: center; background: var(--primary-soft); margin: 24rpx 24rpx 0;
  border-radius: 12rpx; padding: 14rpx 20rpx;
}
.tips-text { flex: 1; font-size: 22rpx; color: var(--primary-text); line-height: 1.5; }
.tips-close { color: var(--primary-text); font-size: 34rpx; padding: 0 8rpx; }

/* 新手引导卡片 */
.guide-card {
  background: var(--card); border-radius: 20rpx; margin: 24rpx 24rpx 0; padding: 28rpx;
}
.guide-title { font-size: 30rpx; font-weight: 700; color: var(--text-main); margin-bottom: 20rpx; }
.guide-step {
  display: flex; align-items: center; padding: 18rpx 0; border-bottom: 1rpx solid var(--border);
}
.guide-step:last-of-type { border-bottom: none; }
.step-num {
  width: 44rpx; height: 44rpx; border-radius: 22rpx; background: var(--primary-strong); color: var(--on-primary);
  font-size: 24rpx; font-weight: 600; display: flex; align-items: center; justify-content: center;
  margin-right: 20rpx; flex-shrink: 0;
}
.step-name { font-size: 26rpx; color: var(--text-main); }
.step-desc { font-size: 22rpx; color: var(--text-sub); margin-top: 4rpx; }
.guide-card .arrow { color: var(--text-muted); font-size: 32rpx; }
.guide-close {
  margin-top: 16rpx; text-align: center; font-size: 24rpx; color: var(--text-sub);
  padding-top: 16rpx; border-top: 1rpx solid var(--border);
}
/* 净资产主卡：与 NavBar 统一为「白卡」语言（原为强调色底 + 白字）。
   为什么改：强调色底 + 白字在 5 套强调色上对比度仅 1.9~4.3:1，无一达到
   小字 AA 的 4.5:1（薄荷最低 1.9:1）；强调色改为用在真正需要强调的地方。 */
.hero {
  background: var(--hero-bg, linear-gradient(165deg, var(--primary-soft) 0%, var(--card) 64%));
  color: var(--text-main);
  padding: 60rpx 40rpx 40rpx; margin-bottom: 24rpx;
  padding-top: calc(var(--status-bar-height) + 50rpx);
  border-radius: 0 0 32rpx 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.05);
}
.hero-label { font-size: 26rpx; color: var(--text-sub); }
.hero-unit { font-size: 26rpx; font-weight: 500; color: var(--text-sub); margin-left: 6rpx; }
.hero-top { display: flex; justify-content: space-between; align-items: center; }
.privacy-toggle {
  font-size: 22rpx; color: var(--primary-text); background: var(--primary-soft);
  border-radius: 24rpx; padding: 6rpx 20rpx;
}
.hero-amount { font-size: 52rpx; font-weight: 700; margin: 12rpx 0 8rpx; letter-spacing: 0.5rpx; font-variant-numeric: tabular-nums; }
.hero-change { font-size: 24rpx; margin-bottom: 24rpx; }
/* 涨跌改用语义色（原 #d4ffd4 / #ffd4d4 是"浅色底上的浅色字"，白卡上不可读） */
.hero-change.up { color: var(--income); }
.hero-change.down { color: var(--expense); }
.hero-row { display: flex; align-items: center; background: var(--hero-row-bg, rgba(255, 255, 255, 0.72)); border: 1rpx solid var(--border); border-radius: 16rpx; padding: 24rpx 0; }
.hero-cell { flex: 1; text-align: center; }
.cell-label { font-size: 24rpx; color: var(--text-sub); margin-bottom: 8rpx; }
.cell-value { font-size: 34rpx; font-weight: 600; }
.hero-divider { width: 1rpx; height: 48rpx; background: var(--border); }
.hero-cell.asset .cell-label::before,
.hero-cell.liability .cell-label::before {
  content: ''; display: inline-block; width: 10rpx; height: 10rpx;
  border-radius: 50%; margin-right: 8rpx; vertical-align: 2rpx;
}
.hero-cell.asset .cell-label::before { background: var(--income); }
.hero-cell.liability .cell-label::before { background: var(--expense); }
.cell-value.asset { color: var(--income); }
.cell-value.liability { color: var(--expense); }
.quick-item:active { opacity: .65; }
.month-col + .month-col { border-left: 1rpx solid var(--border); }

.card { background: var(--card); border-radius: 20rpx; padding: 24rpx 32rpx; margin: 0 24rpx 24rpx; box-shadow: 0 4rpx 16rpx rgba(17, 24, 39, 0.03); }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 20rpx; }
.entry-row { display: flex; align-items: center; padding: 18rpx 0; border-top: 1rpx solid var(--border); }
.entry-row:first-of-type { border-top: none; }
.entry-title { font-size: 28rpx; color: var(--text-main); font-weight: 500; }
.entry-sub { font-size: 23rpx; color: var(--text-sub); margin-top: 6rpx; line-height: 1.5; }
.entry-arrow { font-size: 32rpx; color: var(--text-muted); margin-left: 12rpx; }
.link { color: var(--primary-text); font-size: 26rpx; font-weight: 400; }
.month-row { display: flex; }
.month-col { flex: 1; min-width: 0; padding: 0 8rpx; }
.month-label { font-size: 24rpx; color: var(--text-sub); margin-bottom: 8rpx; }
.month-value { font-size: 28rpx; font-weight: 700; font-variant-numeric: tabular-nums; }
.month-value.income { color: var(--income); }
.month-value.expense { color: var(--expense); }
.budget-line { margin-top: 24rpx; }
.budget-text { display: flex; justify-content: space-between; font-size: 24rpx; color: var(--text-sub); margin-bottom: 12rpx; }
.budget-bar { height: 12rpx; background: var(--surface); border-radius: 6rpx; overflow: hidden; }
.budget-inner { height: 100%; background: var(--primary-strong); border-radius: 6rpx; }
.budget-inner.over { background: var(--expense); }
.budget-text .over { color: var(--expense); font-weight: 600; }

.empty { text-align: center; color: var(--text-muted); padding: 60rpx 0; font-size: 26rpx; }
.txn-row { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid var(--border); }
.txn-row:last-child { border-bottom: none; }
.txn-icon {
  width: 72rpx; height: 72rpx; border-radius: 36rpx; margin-right: 20rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 28rpx; font-weight: 600; color: var(--on-primary); background: var(--primary-strong);
}
.txn-icon.expense { background: var(--expense); color: var(--on-expense); }
.txn-icon.income { background: var(--income); color: var(--on-income); }
.txn-icon.transfer { background: var(--type-transfer); color: var(--on-type); }
.txn-icon.repayment { background: var(--type-repayment); color: var(--on-type); }
.txn-icon.borrow { background: var(--type-borrow); color: var(--on-type); }
.txn-icon.adjust { background: var(--type-adjust); color: var(--on-type); }
/* 备注按 10 字截断后一般不会溢出，ellipsis 作为第二道防线（窄屏/超长单词） */
.txn-name {
  font-size: 26rpx; color: var(--text-main);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
/* 已归档（逻辑删除）流水标记，与「流水明细」页一致 */
.deleted-tag {
  font-size: 20rpx; color: var(--text-sub); background: var(--surface);
  border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx;
}
.txn-date { font-size: 22rpx; color: var(--text-muted); margin-top: 4rpx; }
.txn-unit { font-size: 18rpx; color: var(--text-muted); margin-left: 2rpx; font-weight: 400; }
.txn-amount {
  font-variant-numeric: tabular-nums; font-size: 28rpx; font-weight: 600; }

/* ---- 最近流水分页入口 ---- */
.txn-more {
  text-align: center; font-size: 24rpx; color: var(--primary-text);
  padding: 24rpx 0 8rpx; border-top: 1rpx solid var(--border); margin-top: 8rpx;
}
/* 到底的收尾文案：弱化，不要和「加载更多」抢注意力 */
.txn-more.done { color: var(--text-muted); }
.txn-amount.income { color: var(--income); }
.txn-amount.expense { color: var(--expense); }
.hero-greet { font-size: 24rpx; color: var(--text-sub); margin-bottom: 16rpx; }
.quick-card { padding: 24rpx 12rpx; }
.quick-grid { display: flex; }
.quick-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 10rpx; }
.quick-icon {
  width: 76rpx; height: 76rpx; border-radius: 22rpx;
  background: var(--primary-soft);
  display: flex; align-items: center; justify-content: center;
  font-size: 36rpx; line-height: 1;
}
.quick-label { font-size: 22rpx; color: var(--text-sub); }
.wh-stats { display: flex; padding: 4rpx 0 12rpx; }
.wh-cell { flex: 1; min-width: 0; text-align: center; }
.wh-cell + .wh-cell { border-left: 1rpx solid var(--border); }
.wh-num { font-size: 30rpx; font-weight: 700; color: var(--text-main); font-variant-numeric: tabular-nums; }
.wh-label { margin-top: 4rpx; font-size: 22rpx; color: var(--text-muted); }
.wh-tip { font-size: 22rpx; color: var(--expense); padding: 8rpx 0; }
</style>
