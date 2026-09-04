<template>
  <view class="page" :style="themeVars">
    <NavBar :title="navTitle" />
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
    <!-- 筛选栏 -->
    <view class="filter-bar">
      <picker :range="typeOptions" range-key="label" @change="onTypeChange">
        <view class="filter-item">{{ currentTypeLabel }} ▾</view>
      </picker>
      <picker mode="date" :value="startDate" @change="(e) => startDate = e.detail.value" @tap.stop>
        <view class="filter-item">{{ startDate || '开始日期' }}</view>
      </picker>
      <picker mode="date" :value="endDate" @change="(e) => endDate = e.detail.value">
        <view class="filter-item">{{ endDate || '结束日期' }}</view>
      </picker>
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
      <view v-if="list.length === 0 && loaded" class="empty">暂无流水</view>
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
              <text class="txn-name">{{ t.description || typeText(t.type) }}<text v-if="t.status === 0" class="deleted-tag">已删除</text></text>
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
  </view>
</template>

<script>
import { pageTransactions } from '@/api/ledger';
import { centToYuan, centToAmount, centToSigned, typeText, toNum } from '@/utils/money';
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

export default {
  data() {
    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    return {
      typeOptions: TYPE_OPTIONS,
      type: '',
      startDate: '',
      endDate: '',
      list: [],
      pageNum: 1,
      pageSize: 20,
      total: 0,
      loaded: false,
      accountId: null,
      liabilityId: null,
      filterName: '',
      today: `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    navTitle() {
      if (this.filterName) return this.filterName + ' · 流水';
      return '流水明细';
    },
    currentTypeLabel() {
      const found = TYPE_OPTIONS.find(t => t.key === this.type);
      return found ? found.label : '全部';
    },
    hasMore() { return this.list.length < this.total; },
    // 按日期分组（保持后端排序：日期倒序、ID倒序）
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
      return centToAmount(sum);
    },
    sumExpenseText() {
      const sum = this.list.filter(t => t.type === 'expense').reduce((s, t) => s + toNum(t.amount), 0);
      return centToAmount(sum);
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
  },
  onShow() {
    this.reload();
  },
  methods: {
    typeText,
    amountOf(t) {
      if (t.type === 'income') return '+' + centToYuan(t.amount);
      if (t.type === 'expense') return '-' + centToYuan(t.amount);
      if (t.type === 'adjust') return centToSigned(t.amount);
      return centToYuan(t.amount);
    },
    accountInfo(t) {
      const parts = [];
      if (t.accountName) parts.push(t.accountName);
      if (t.targetAccountName) parts.push('→ ' + t.targetAccountName);
      if (t.liabilityName) parts.push(t.liabilityName);
      return parts.join(' ');
    },
    daySubtotal(group) {
      const income = group.items.filter(t => t.type === 'income').reduce((s, t) => s + toNum(t.amount), 0);
      const expense = group.items.filter(t => t.type === 'expense').reduce((s, t) => s + toNum(t.amount), 0);
      const segs = [];
      if (income > 0) segs.push('收 ' + centToAmount(income));
      if (expense > 0) segs.push('支 ' + centToAmount(expense));
      return segs.join(' · ');
    },
    previewVoucher(t) {
      if (!t.voucherUrl) return;
      uni.previewImage({ urls: [t.voucherUrl] });
    },
    resetFilter() {
      this.type = '';
      this.startDate = '';
      this.endDate = '';
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
      this.pageNum++;
      await this.load();
    },
    async load() {
      try {
        const res = await pageTransactions({
          type: this.type || undefined,
          startDate: this.startDate || undefined,
          endDate: this.endDate || undefined,
          accountId: this.accountId || undefined,
          liabilityId: this.liabilityId || undefined,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        });
        const records = (res && res.records) || [];
        this.list = this.pageNum === 1 ? records : this.list.concat(records);
        this.total = (res && res.total) || 0;
        this.loaded = true;
      } catch (e) { this.loaded = true; }
    },
    goEdit(t) {
      if (t.status === 0) {
        uni.showToast({ title: '已删除流水不可编辑', icon: 'none' });
        return;
      }
      uni.navigateTo({ url: '/pages/record/edit?id=' + t.id });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.filter-bar {
  display: flex; flex-wrap: wrap; gap: 12rpx; padding: 20rpx 24rpx; background: #fff;
}
.filter-item {
  padding: 8rpx 20rpx; background: #f5f6f8; border-radius: 28rpx; font-size: 24rpx; color: #555;
}
.filter-btn {
  padding: 8rpx 28rpx; background: var(--primary-strong); color: #fff; border-radius: 28rpx; font-size: 24rpx;
}
.filter-btn.reset { background: #999; }

.summary-bar {
  display: flex; justify-content: space-between; align-items: center;
  margin: 16rpx 24rpx 0; padding: 0 8rpx; font-size: 22rpx; color: #999;
}
.summary-nums { display: flex; gap: 24rpx; }
.summary-nums .income { color: #27ae60; }
.summary-nums .expense { color: #e74c3c; }

.card { background: #fff; border-radius: 20rpx; padding: 8rpx 24rpx; margin: 16rpx 24rpx; }

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
  border-radius: 50%; background: #fff; color: #94a3b8; font-size: 30rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.04);
}
.banner-close:active { background: #f1f5f9; }
.empty { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }

.day-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 8rpx 8rpx; border-bottom: 1rpx solid #f0f0f5;
}
.day-date { font-size: 26rpx; font-weight: 600; color: #333; }
.day-sub { font-size: 22rpx; color: #999; }

.txn-row { display: flex; align-items: center; padding: 14rpx 8rpx; border-bottom: 1rpx solid #f5f5f7; }
.txn-row.last-row { border-bottom: none; }
.txn-icon {
  width: 56rpx; height: 56rpx; border-radius: 28rpx; margin-right: 16rpx; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 24rpx; font-weight: 600; color: #fff; background: var(--primary-strong);
}
.txn-icon.expense { background: #e74c3c; }
.txn-icon.income { background: #27ae60; }
.txn-icon.transfer { background: #4a90d9; }
.txn-icon.repayment { background: #a04000; }
.txn-icon.borrow { background: #8e44ad; }
.txn-icon.adjust { background: #95a5a6; }
.txn-top { display: flex; justify-content: space-between; align-items: baseline; }
.txn-name { font-size: 27rpx; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.deleted-tag { font-size: 20rpx; color: #fff; background: #bbb; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.txn-amount { font-size: 28rpx; font-weight: 600; flex-shrink: 0; margin-left: 16rpx; }
.txn-amount.income { color: #27ae60; }
.txn-amount.expense { color: #e74c3c; }
.txn-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 4rpx; }
.meta-tags { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta-tag {
  display: inline-block; font-size: 20rpx; color: #888; background: #f5f6f8;
  border-radius: 6rpx; padding: 2rpx 10rpx; margin-right: 10rpx;
}
.meta-right { display: flex; align-items: center; gap: 12rpx; flex-shrink: 0; }
.voucher-flag { font-size: 20rpx; color: var(--primary-strong); border: 1rpx solid var(--primary-strong); border-radius: 6rpx; padding: 0 8rpx; }
.load-more { text-align: center; color: var(--primary-strong); font-size: 26rpx; padding: 24rpx 0; }
</style>
