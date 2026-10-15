<template>
  <view class="page" :style="themeVars">
    <!-- 合并顶栏：返回 | 日历/流水切换(居中) | 公用月份选择 -->
    <view class="header-bar">
      <view class="header-back" @tap="goBack">
        <text class="back-icon">‹</text>
      </view>
      <view class="header-switch">
        <view class="header-switch-inner">
          <view class="sw-item" :class="{ active: activeTab === 'calendar' }" @tap="switchTab('calendar')">日历</view>
          <view class="sw-item" :class="{ active: activeTab === 'list' }" @tap="switchTab('list')">流水</view>
        </view>
      </view>
      <picker mode="date" fields="month" :value="monthValue" @change="onMonthPick" class="header-right">
        <view class="month-picker">
          <text class="month-text">{{ year }}-{{ pad(month) }}</text>
          <text class="month-arrow">▾</text>
        </view>
      </picker>
    </view>

    <!-- ==================== 流水视图 ==================== -->
    <block v-if="activeTab === 'list'">
      <!-- 账户/负债筛选条（从资产负债页点入时出现） -->
      <view class="filter-banner" v-if="accountId || liabilityId">
        <text class="banner-icon">🔗</text>
        <view class="banner-body">
          <view class="banner-title">仅显示「{{ filterName }}」的关联流水</view>
          <view class="banner-sub" v-if="accountId">资产账户 · 作为转出 / 转入方都会匹配</view>
          <view class="banner-sub" v-else>负债账户 · 借款 / 还款 / 校准联动记录</view>
        </view>
        <text class="banner-close" @tap="clearAccountFilter">×</text>
      </view>
      <!-- 筛选栏：类型 + 月份范围（与顶栏月份联动，可手动微调） -->
      <view class="filter-bar">
        <picker :range="typeOptions" range-key="label" @change="onTypeChange">
          <view class="filter-item">{{ currentTypeLabel }} ▾</view>
        </picker>
        <CalendarPicker :value="startDate" @change="(e) => startDate = e.detail.value" @tap.stop>
          <view class="filter-item">{{ startDate || '开始日期' }}</view>
        </CalendarPicker>
        <CalendarPicker :value="endDate" @change="(e) => endDate = e.detail.value">
          <view class="filter-item">{{ endDate || '结束日期' }}</view>
        </CalendarPicker>
        <view class="filter-btn" @tap="reload">查询</view>
        <view class="filter-btn reset" @tap="resetFilter">重置</view>
      </view>

      <!-- 汇总条 -->
      <view class="summary-bar" v-if="list.length">
        <text>共 {{ total }} 笔</text>
        <view class="summary-nums">
          <text class="income">收 {{ sumIncomeText }}</text>
          <text class="expense">支 {{ sumExpenseText }}</text>
        </view>
      </view>

      <!-- 流水列表（按日分组） -->
      <view class="card">
        <view v-if="loadError && loaded" class="empty" @tap="reload">加载失败，点击重试</view>
        <view v-else-if="list.length === 0 && loaded" class="empty">暂无流水</view>
        <view v-for="group in groups" :key="group.date" class="day-group">
          <view class="day-header">
            <text class="day-date">{{ group.date === today ? '今天' : group.date }}</text>
            <text class="day-sub">{{ daySubtotal(group) }}</text>
          </view>
          <view v-for="(t, ti) in group.items" :key="t.id" class="txn-row"
                :class="{ 'last-row': ti === group.items.length - 1 }" @tap="goEdit(t)">
            <view class="txn-icon" :class="t.type">{{ typeText(t.type).slice(0, 1) }}</view>
            <view class="flex-1 min-w-0">
              <view class="txn-top">
                <text class="txn-name">{{ truncate(t.description, 10) || typeText(t.type) }}<text v-if="t.status === 0" class="deleted-tag">已删除</text></text>
                <text class="txn-amount" :class="t.type">{{ amountOf(t) }}</text>
              </view>
              <view class="txn-meta">
                <text class="meta-tags">
                  <text v-if="t.categoryName" class="meta-tag">{{ t.categoryName }}</text>
                  <text v-if="t.merchant" class="meta-tag">{{ t.merchant }}</text>
                  <text v-if="accountInfo(t)" class="meta-tag">{{ accountInfo(t) }}</text>
                </text>
                <view class="meta-right">
                  <text v-if="t.voucherUrl" class="voucher-flag" @tap.stop="previewVoucher(t)">凭证</text>
                </view>
              </view>
            </view>
          </view>
        </view>
        <view v-if="hasMore" class="load-more" @tap="loadMore">加载更多</view>
      </view>

      <!-- 分页栏 -->
      <view class="pagination" v-if="total > 0">
        <view class="pg-btn" :class="{ disabled: pageNum <= 1 }" @tap="goPage(pageNum - 1)">‹</view>
        <view class="pg-info">
          <text class="pg-current">{{ pageNum }}</text>
          <text class="pg-sep"> / </text>
          <text class="pg-total">{{ totalPages }}</text>
        </view>
        <view class="pg-btn" :class="{ disabled: pageNum >= totalPages }" @tap="goPage(pageNum + 1)">›</view>
        <view class="pg-count">共 {{ total }} 笔</view>
      </view>
    </block>

    <!-- ==================== 日历视图 ==================== -->
    <block v-if="activeTab === 'calendar'">
      <!-- 日历区 -->
      <view class="calendar-card">
        <!-- 星期标题 -->
        <view class="week-row">
          <text v-for="(w, wi) in ['日','一','二','三','四','五','六']" :key="w" class="week-cell"
                :class="{ weekend: wi === 0 || wi === 6 }">{{ w }}</text>
        </view>
        <!-- 日期网格 -->
        <view class="grid">
          <view v-for="(cell, i) in cells" :key="cell ? cell.date : 'blank-' + i" class="day-cell"
                :class="{ empty: !cell, selected: cell && cell.date === selectedDate, today: cell && cell.date === todayStr, weekend: cell && cell.weekend }"
                @tap="cell && pickDay(cell.date)">
            <template v-if="cell">
              <text class="day-num">{{ cell.day }}</text>
              <text v-if="cell.net !== 0" class="day-net" :class="cell.net > 0 ? 'in' : 'out'">{{ cell.net > 0 ? '+' : '-' }}{{ formatNet(cell.net) }}</text>
              <view v-else-if="cell.count" class="day-dot"></view>
            </template>
          </view>
        </view>
      </view>

      <!-- 选中日汇总 -->
      <view class="day-summary">
        <view class="ds-left">
          <text class="ds-date">{{ selectedLabel }}</text>
        </view>
        <view class="ds-right">
          <text class="ds-in" v-if="dayIncome > 0">收入:{{ formatAmount(dayIncome) }}</text>
          <text class="ds-out" v-if="dayExpense > 0">支出:{{ formatAmount(dayExpense) }}</text>
        </view>
      </view>

      <!-- 当日流水列表 -->
      <view class="card">
        <view v-if="loadError && calLoaded" class="empty" @tap="loadMonth">加载失败，点击重试</view>
        <view v-else-if="!dayItems.length && calLoaded" class="empty">当日暂无流水</view>
        <view v-for="(t, ti) in dayItems" :key="t.id" class="txn-row"
              :class="{ 'last-row': ti === dayItems.length - 1 }" @tap="goEdit(t)">
          <view class="txn-icon" :class="t.type">{{ typeText(t.type).slice(0, 1) }}</view>
          <view class="flex-1 min-w-0">
            <view class="txn-top">
              <text class="txn-name">{{ truncate(t.description, 10) || typeText(t.type) }}<text v-if="t.status === 0" class="deleted-tag">已删除</text></text>
              <text class="txn-amount" :class="t.type">{{ amountOf(t) }}</text>
            </view>
            <view class="txn-meta">
              <text class="meta-tags">
                <text v-if="t.categoryName" class="meta-tag">{{ t.categoryName }}</text>
                <text v-if="accountInfo(t)" class="meta-tag">{{ accountInfo(t) }}</text>
              </text>
              <text class="txn-time">{{ t.transactionTime || '' }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 月度汇总 -->
      <view class="month-summary">
        <text>{{ year }}年{{ month }}月 · 共 {{ monthCount }} 笔</text>
        <view class="ms-nums">
          <text class="in">收 {{ formatAmount(monthIncome) }}</text>
          <text class="out">支 {{ formatAmount(monthExpense) }}</text>
        </view>
      </view>
    </block>
  </view>
</template>

<script>
import CalendarPicker from '@/components/CalendarPicker/CalendarPicker.vue';
import { pageTransactions } from '@/api/ledger';
import { toFixedYuan, formatAmount, formatSigned, typeText, toNum } from '@/utils/money';
import { truncate } from '@/utils/text';
import { useThemeStore } from '@/stores/theme';

const TYPE_OPTIONS = [
  { key: '', label: '全部' },
  { key: 'income', label: '收入' },
  { key: 'expense', label: '支出' },
  { key: 'transfer', label: '转账' },
  { key: 'repayment', label: '还款' },
  { key: 'borrow', label: '借款' },
  { key: 'adjust', label: '校准' }
];

const pad = (n) => String(n).padStart(2, '0');
const fmtDate = (y, m, d) => `${y}-${pad(m)}-${pad(d)}`;

export default {
  components: { CalendarPicker },  // 自绘月历选择器
  data() {
    const now = new Date();
    const today = new Date();
    return {
      // 视图切换
      activeTab: 'list',
      // 公用月份状态
      year: now.getFullYear(),
      month: now.getMonth() + 1,
      todayStr: fmtDate(today.getFullYear(), today.getMonth() + 1, today.getDate()),
      today: `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`,
      // ---- 流水视图 ----
      typeOptions: TYPE_OPTIONS,
      type: '',
      startDate: '',
      endDate: '',
      list: [],
      pageNum: 1,
      // 列表加载锁：loadMore / goPage 共用，防止连点造成重复追加或跳页
      loadingList: false,
      pageSize: 20,
      total: 0,
      loaded: false,
      loadError: false,   // 加载失败（与"真的没数据"区分，避免误导用户以为数据丢了）
      accountId: null,
      liabilityId: null,
      filterName: '',
      // ---- 日历视图 ----
      selectedDate: fmtDate(today.getFullYear(), today.getMonth() + 1, today.getDate()),
      monthTxns: [],
      calLoaded: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    monthValue() { return `${this.year}-${pad(this.month)}`; },
    monthStart() { return fmtDate(this.year, this.month, 1); },
    monthEnd() { return fmtDate(this.year, this.month, this.daysInMonth()); },
    // ---- 流水视图 ----
    currentTypeLabel() {
      const found = TYPE_OPTIONS.find(t => t.key === this.type);
      return found ? found.label : '全部';
    },
    hasMore() { return this.list.length < this.total; },
    totalPages() { return Math.max(1, Math.ceil(this.total / this.pageSize)); },
    groups() {
      const map = [];
      const index = {};
      for (const t of this.list) {
        const d = t.transactionDate;
        if (index[d] === undefined) {
          index[d] = map.length;
          map.push({ date: d, items: [] });
        }
        map[index[d]].items.push(t);
      }
      return map;
    },
    sumIncomeText() {
      const sum = this.list.filter(t => t.type === 'income').reduce((s, t) => s + toNum(t.amount), 0);
      return formatAmount(sum);
    },
    sumExpenseText() {
      const sum = this.list.filter(t => t.type === 'expense').reduce((s, t) => s + toNum(t.amount), 0);
      return formatAmount(sum);
    },
    // ---- 日历视图 ----
    cells() {
      const first = new Date(this.year, this.month - 1, 1);
      const lead = first.getDay();
      const byDate = {};
      for (const t of this.monthTxns) {
        const d = t.transactionDate;
        if (!byDate[d]) byDate[d] = { income: 0, expense: 0, count: 0 };
        byDate[d].count++;
        if (t.type === 'income') byDate[d].income += toNum(t.amount);
        if (t.type === 'expense') byDate[d].expense += toNum(t.amount);
      }
      const arr = [];
      for (let i = 0; i < lead; i++) arr.push(null);
      for (let d = 1; d <= this.daysInMonth(); d++) {
        const date = fmtDate(this.year, this.month, d);
        const agg = byDate[date];
        const net = agg ? Number((agg.income - agg.expense).toFixed(2)) : 0;
        const col = (lead + d - 1) % 7;
        arr.push({ day: d, date, net, count: agg ? agg.count : 0, weekend: col === 0 || col === 6 });
      }
      return arr;
    },
    dayItems() {
      return this.monthTxns.filter(t => t.transactionDate === this.selectedDate);
    },
    dayIncome() {
      return this.dayItems.filter(t => t.type === 'income').reduce((s, t) => s + toNum(t.amount), 0);
    },
    dayExpense() {
      return this.dayItems.filter(t => t.type === 'expense').reduce((s, t) => s + toNum(t.amount), 0);
    },
    monthIncome() {
      return this.monthTxns.filter(t => t.type === 'income').reduce((s, t) => s + toNum(t.amount), 0);
    },
    monthExpense() {
      return this.monthTxns.filter(t => t.type === 'expense').reduce((s, t) => s + toNum(t.amount), 0);
    },
    monthCount() { return this.monthTxns.length; },
    selectedLabel() {
      const [, m, d] = this.selectedDate.split('-');
      return `${Number(m)}月${Number(d)}日`;
    }
  },
  onLoad(options) {
    if (options && options.accountId) {
      this.accountId = Number(options.accountId) || null;
      this.liabilityId = null;
    }
    if (options && options.liabilityId) {
      this.liabilityId = Number(options.liabilityId) || null;
      this.accountId = null;
    }
    if (options && options.name) {
      try { this.filterName = decodeURIComponent(options.name); } catch (_) { this.filterName = options.name; }
    }
    // 默认按当前月份筛选
    this.startDate = this.monthStart;
    this.endDate = this.monthEnd;
  },
  onShow() {
    useThemeStore().restore();
    this.reload();
  },
  methods: {
    typeText,
    truncate,
    formatAmount,
    pad,
    daysInMonth() { return new Date(this.year, this.month, 0).getDate(); },
    amountOf(t) {
      if (t.type === 'income') return '+' + toFixedYuan(t.amount);
      if (t.type === 'expense') return '-' + toFixedYuan(t.amount);
      if (t.type === 'adjust') return formatSigned(t.amount);
      return toFixedYuan(t.amount);
    },
    accountInfo(t) {
      const parts = [];
      if (t.accountName) parts.push(t.accountName);
      if (t.targetAccountName) parts.push('→ ' + t.targetAccountName);
      if (t.liabilityName) parts.push(t.liabilityName);
      return parts.join(' ');
    },
    /** 切换视图：同页内切换，不跳转 */
    switchTab(tab) {
      if (this.activeTab === tab) return;
      this.activeTab = tab;
      // 切到流水时若未加载则加载
      if (tab === 'list' && !this.loaded) this.reload();
      // 切到日历时若当月数据未加载则加载
      if (tab === 'calendar' && !this.calLoaded) this.loadMonth();
    },
    /** 公用月份切换：同步到流水的起止日期并重新加载流水 + 日历 */
    onMonthPick(e) {
      const [y, m] = (e.detail.value || '').split('-').map(Number);
      if (!y || !m) return;
      this.year = y;
      this.month = m;
      // 流水视图按新月刷新
      this.startDate = this.monthStart;
      this.endDate = this.monthEnd;
      this.reload();
      // 日历视图按新月刷新，选中日归一
      const now = new Date();
      const isCurrent = y === now.getFullYear() && m === now.getMonth() + 1;
      this.selectedDate = isCurrent ? this.todayStr : fmtDate(y, m, 1);
      this.loadMonth();
    },
    // ---- 流水视图方法 ----
    daySubtotal(group) {
      const income = group.items.filter(t => t.type === 'income').reduce((s, t) => s + toNum(t.amount), 0);
      const expense = group.items.filter(t => t.type === 'expense').reduce((s, t) => s + toNum(t.amount), 0);
      const segs = [];
      if (income > 0) segs.push('收 ' + formatAmount(income));
      if (expense > 0) segs.push('支 ' + formatAmount(expense));
      return segs.join(' · ');
    },
    previewVoucher(t) {
      if (!t.voucherUrl) return;
      uni.previewImage({ urls: [t.voucherUrl] });
    },
    resetFilter() {
      this.type = '';
      this.startDate = this.monthStart;
      this.endDate = this.monthEnd;
      this.reload();
    },
    clearAccountFilter() {
      this.accountId = null;
      this.liabilityId = null;
      this.filterName = '';
      this.reload();
    },
    onTypeChange(e) {
      this.type = TYPE_OPTIONS[e.detail.value].key;
      this.reload();
    },
    async reload() {
      this.pageNum = 1;
      this.list = [];
      await this.load();
    },
    async loadMore() {
      // 并发保护：连点「加载更多」会连发多个请求，每次都 pageNum++ 并 concat，
      // 结果既会重复、又会**跳过中间页**（第 N+1 页的响应被页码变更判定为过期丢弃）。
      if (this.loadingList) return;
      this.loadingList = true;
      try {
        this.pageNum++;
        await this.load();
      } finally {
        this.loadingList = false;
      }
    },
    async goPage(page) {
      if (page < 1 || page > this.totalPages || page === this.pageNum) return;
      if (this.loadingList) return;   // 与 loadMore 共用锁，避免清空列表与追加剧烈交叉
      this.loadingList = true;
      try {
        this.pageNum = page;
        this.list = [];
        await this.load();
      } finally {
        this.loadingList = false;
      }
      uni.pageScrollTo && uni.pageScrollTo({ scrollTop: 0, duration: 200 });
    },
    async load() {
      // 捕获本次请求对应的页码：await 期间 pageNum 可能被改（切页/重新加载），
      // 若不捕获就会出现「用新页码去拼接旧响应」的错位。
      const page = this.pageNum;
      try {
        const res = await pageTransactions({
          type: this.type || undefined,
          startDate: this.startDate || undefined,
          endDate: this.endDate || undefined,
          accountId: this.accountId || undefined,
          liabilityId: this.liabilityId || undefined,
          pageNum: page,
          pageSize: this.pageSize
        });
        // 期间用户已切页/重新加载 → 丢弃这次结果，否则会把旧数据拼进新列表
        if (page !== this.pageNum) return;
        const records = (res && res.records) || [];
        this.list = page === 1 ? records : this.list.concat(records);
        this.total = (res && res.total) || 0;
        this.loaded = true;
        this.loadError = false;
      } catch (e) {
        // 失败必须与"确实没有流水"区分：否则网络抖动时用户以为数据丢了，会去重复录入
        this.loaded = true;
        this.loadError = true;
      }
    },
    // ---- 日历视图方法 ----
    formatNet(net) {
      const abs = Math.abs(net);
      if (abs >= 10000) return (abs / 10000).toFixed(1) + 'w';
      return Number(abs.toFixed(2));
    },
    pickDay(date) { this.selectedDate = date; },
    async loadMonth() {
      this.calLoaded = false;
      try {
        let pageNum = 1;
        let all = [];
        let total = 0;
        for (let i = 0; i < 10; i++) {
          const res = await pageTransactions({
            startDate: this.monthStart,
            endDate: this.monthEnd,
            pageNum,
            pageSize: 100
          });
          const records = (res && res.records) || [];
          total = (res && res.total) || 0;
          all = all.concat(records);
          if (all.length >= total || records.length === 0) break;
          pageNum++;
        }
        this.monthTxns = all;
      } catch (e) {
        this.monthTxns = [];
      }
      this.calLoaded = true;
      this.loadError = false;
    },
    goEdit(t) {
      if (t.status === 0) {
        uni.showToast({ title: '已删除流水不可编辑', icon: 'none' });
        return;
      }
      uni.navigateTo({ url: '/pages/record/edit?id=' + t.id });
    },
    goBack() {
      const pages = getCurrentPages();
      if (pages.length > 1) {
        uni.navigateBack();
      } else {
        uni.switchTab({ url: '/pages/dashboard/index' });
      }
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; min-height: 100vh; background: var(--surface); }

/* 合并顶栏：返回 | 日历/流水切换(居中) | 月份
   与 NavBar 统一为「白卡头部」（原为强调色底 + 白字）。
   原因：强调色底 + 白字在 5 套强调色上对比度仅 1.9~4.3:1，
   没有一套达到小字 AA 的 4.5:1（薄荷最低 1.9:1）。 */
.header-bar {
  display: flex; align-items: center;
  background: var(--card);
  padding: calc(var(--status-bar-height) + 16rpx) 24rpx 20rpx;
  border-bottom: 1rpx solid var(--border);
}
/* 返回键与 NavBar 对齐：浅强调色圆底 + 强调色箭头 */
.header-back {
  width: 60rpx; height: 60rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: var(--primary-soft); flex-shrink: 0;
}
.header-back:active { opacity: 0.7; }
.back-icon { font-size: 44rpx; color: var(--primary-strong); margin-top: -4rpx; line-height: 1; }
.header-switch {
  flex: 1; display: flex; justify-content: center;
}
/* 分段控件：浅灰轨道 + 白底选中态（原为白色半透明，仅适用于彩色底） */
.header-switch-inner {
  display: flex; background: var(--surface);
  border-radius: 32rpx; padding: 6rpx;
}
.sw-item {
  padding: 8rpx 28rpx; font-size: 26rpx; color: var(--text-sub);
  border-radius: 26rpx;
}
.sw-item.active { background: var(--card); color: var(--primary-strong); font-weight: 600; }
.header-right { flex-shrink: 0; }
.month-picker { display: flex; align-items: center; }
.month-text { font-size: 28rpx; font-weight: 600; color: var(--text-main); }
.month-arrow { font-size: 22rpx; margin-left: 6rpx; color: var(--primary-strong); }

/* 筛选栏 */
.filter-bar {
  display: flex; flex-wrap: wrap; gap: 12rpx; padding: 20rpx 24rpx; background: var(--card);
}
.filter-item {
  padding: 8rpx 20rpx; background: var(--surface); border-radius: 28rpx; font-size: 24rpx; color: var(--text-sub);
}
.filter-btn {
  padding: 8rpx 28rpx; background: var(--primary-strong); color: #fff; border-radius: 28rpx; font-size: 24rpx;
}
.filter-btn.reset { background: #999; }

.summary-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin: 16rpx 24rpx 0; padding: 0 8rpx; font-size: 22rpx; color: var(--text-sub);
}
.summary-nums { display: flex; gap: 24rpx; }
.summary-nums .income { color: var(--income); }
.summary-nums .expense { color: var(--expense); }

.card { background: var(--card); border-radius: 20rpx; padding: 8rpx 24rpx; margin: 16rpx 24rpx; }

.filter-banner {
  display: flex; align-items: center; gap: 16rpx;
  margin: 12rpx 24rpx 0; padding: 20rpx 24rpx;
  background: linear-gradient(135deg, #eaf4ff, #f4faff);
  border: 1rpx solid #dbeafe; border-radius: 16rpx;
}
.banner-icon { font-size: 32rpx; }
.banner-body { flex: 1; min-width: 0; }
.banner-title { font-size: 26rpx; font-weight: 600; color: #1e3a5f; }
.banner-sub { font-size: 22rpx; color: #64748b; margin-top: 4rpx; }
.banner-close {
  width: 48rpx; height: 48rpx; line-height: 44rpx; text-align: center;
  border-radius: 50%; background: var(--card); color: var(--text-sub); font-size: 30rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.banner-close:active { background: var(--surface); }
.empty { text-align: center; color: var(--text-muted); padding: 60rpx 0; font-size: 26rpx; }

.day-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 8rpx 8rpx; border-bottom: 1rpx solid #f0f0f5;
}
.day-date { font-size: 26rpx; font-weight: 600; color: var(--text-main); }
.day-sub { font-size: 22rpx; color: var(--text-sub); }

.txn-row { display: flex; align-items: center; padding: 14rpx 8rpx; border-bottom: 1rpx solid #f5f5f7; }
.txn-row.last-row { border-bottom: none; }
.txn-icon {
  width: 56rpx; height: 56rpx; border-radius: 28rpx; margin-right: 16rpx; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 24rpx; font-weight: 600; color: #fff; background: var(--primary-strong);
}
.txn-icon.expense { background: var(--expense); }
.txn-icon.income { background: var(--income); }
.txn-icon.transfer { background: #4a90d9; }
.txn-icon.repayment { background: #a04000; }
.txn-icon.borrow { background: #8e44ad; }
.txn-icon.adjust { background: #95a5a6; }
.txn-top { display: flex; justify-content: space-between; align-items: baseline; }
.txn-name { font-size: 27rpx; color: var(--text-main); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.deleted-tag { font-size: 20rpx; color: #fff; background: var(--text-muted); border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.txn-amount { font-size: 28rpx; font-weight: 600; flex-shrink: 0; margin-left: 16rpx; }
.txn-amount.income { color: var(--income); }
.txn-amount.expense { color: var(--expense); }
.txn-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 4rpx; }
.meta-tags { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta-tag {
  display: inline-block; font-size: 20rpx; color: var(--text-sub); background: var(--surface);
  border-radius: 6rpx; padding: 2rpx 10rpx; margin-right: 10rpx;
}
.meta-right { display: flex; align-items: center; gap: 12rpx; flex-shrink: 0; }
.voucher-flag { font-size: 20rpx; color: var(--primary-strong); border: 1rpx solid var(--primary-strong); border-radius: 6rpx; padding: 0 8rpx; }
.load-more { text-align: center; color: var(--primary-strong); font-size: 26rpx; padding: 24rpx 0; }

/* 分页栏 */
.pagination {
  display: flex; align-items: center; justify-content: center; gap: 20rpx;
  padding: 24rpx 24rpx 40rpx;
}
.pg-btn {
  width: 64rpx; height: 64rpx; line-height: 60rpx; text-align: center;
  border-radius: 50%; background: var(--card); color: var(--primary-strong);
  font-size: 36rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.06);
}
.pg-btn:active { background: var(--primary-soft); }
.pg-btn.disabled { opacity: 0.3; }
.pg-info { font-size: 28rpx; color: var(--text-main); }
.pg-current { font-weight: 700; color: var(--primary-strong); }
.pg-sep { color: var(--text-muted); }
.pg-total { color: var(--text-sub); }
.pg-count { font-size: 22rpx; color: var(--text-sub); margin-left: 8rpx; }

/* ============ 日历视图样式 ============ */
.calendar-card { background: var(--card); border-radius: 20rpx; margin: 16rpx 24rpx 0; padding: 20rpx 16rpx 8rpx; }
.week-row { display: flex; border-bottom: 1rpx solid #f5f5f7; }
.week-cell { flex: 1; text-align: center; font-size: 24rpx; color: var(--text-sub); padding: 12rpx 0; }
.week-cell.weekend { color: var(--expense); }
.grid { display: flex; flex-wrap: wrap; }
.day-cell {
  width: 14.28%; height: 104rpx; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
}
.day-cell.empty { pointer-events: none; }
.day-cell.selected { background: var(--primary-soft); border-radius: 12rpx; }
.day-num { font-size: 26rpx; color: var(--text-main); }
.day-cell.weekend .day-num { color: var(--expense); }
.day-cell.selected .day-num { color: var(--primary-strong); font-weight: 700; }
.day-cell.today .day-num { color: var(--primary-strong); }
.day-cell.today.weekend .day-num { color: var(--primary-strong); }
.day-net { font-size: 18rpx; margin-top: 2rpx; transform: scale(0.9); }
.day-net.in { color: var(--income); }
.day-net.out { color: var(--expense); }
.day-dot { width: 8rpx; height: 8rpx; border-radius: 4rpx; background: #ccc; margin-top: 4rpx; }
.day-cell.selected .day-dot { background: var(--primary-strong); }

.day-summary {
  display: flex; justify-content: space-between; align-items: center;
  margin: 16rpx 32rpx 0;
}
.ds-left { display: flex; align-items: center; }
.ds-date { font-size: 28rpx; font-weight: 600; color: var(--text-main); }
.ds-right { display: flex; gap: 20rpx; font-size: 22rpx; color: var(--text-sub); }
.ds-in { color: var(--income); }
.ds-out { color: var(--expense); }

.txn-time { font-size: 20rpx; color: var(--text-sub); flex-shrink: 0; }

.month-summary {
  display: flex; justify-content: space-between; align-items: center;
  margin: 16rpx 32rpx 0; font-size: 22rpx; color: var(--text-sub);
}
.ms-nums { display: flex; gap: 24rpx; }
.ms-nums .in { color: var(--income); }
.ms-nums .out { color: var(--expense); }
</style>
