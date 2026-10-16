<template>
  <view class="page" :style="themeVars">
    <NavBar title="债权债务" />

    <!-- 应收（别人欠我）/ 应付（我欠别人）：左右对称的两个页签 -->
    <view class="tabs">
      <view class="tab" :class="{ active: type === 'receivable' }" @tap="switchType('receivable')">应收（借出）</view>
      <view class="tab" :class="{ active: type === 'payable' }" @tap="switchType('payable')">应付（借款）</view>
    </view>

    <!-- 汇总：只算未结余额（已结清的早已变成现金/已从现金支出，再算会重复计算） -->
    <view class="summary">
      <text class="summary-label">{{ type === 'receivable' ? '待收合计' : '待还合计' }}</text>
      <text class="summary-amount">{{ privacyMode ? '****' : '¥ ' + formatAmount(outstandingTotal) }}</text>
    </view>

    <!-- 状态筛选 -->
    <view class="filters">
      <view v-for="f in filters" :key="f.key" class="filter-tag"
            :class="{ active: settleStatus === f.key }" @tap="switchStatus(f.key)">{{ f.label }}</view>
    </view>

    <view v-if="list.length === 0" class="empty">
      {{ type === 'receivable' ? '没有别人欠你的钱' : '没有你欠别人的钱' }}
    </view>

    <view class="card" v-else>
      <view v-for="d in list" :key="d.id" class="row" @tap="openSettle(d)">
        <view class="flex-1 min-w-0">
          <view class="row-name">{{ d.counterparty }}</view>
          <view class="row-sub">
            <text class="status" :class="d.settleStatus">{{ settleText(d.settleStatus) }}</text>
            <text class="row-date" v-if="d.dueDate"> · {{ d.dueDate }} 到期</text>
          </view>
        </view>
        <view class="row-amount">
          <text class="amount-out">{{ privacyMode ? '****' : '¥ ' + formatAmount(outstanding(d)) }}</text>
          <text class="amount-total">总额 {{ formatAmount(d.totalAmount) }}</text>
        </view>
      </view>
    </view>

    <!-- 分页：与定时任务列表同一套交互（仅多于 1 页时出现） -->
    <view class="pager" v-if="pages > 1">
      <view class="pager-btn" :class="{ disabled: pageNum <= 1 }" @tap="prevPage">上一页</view>
      <text class="pager-info">{{ pageNum }} / {{ pages }}</text>
      <view class="pager-btn" :class="{ disabled: pageNum >= pages }" @tap="nextPage">下一页</view>
    </view>

    <!-- 核销弹层：填金额 + 选资金账户 -->
    <view class="mask" v-if="settleTarget" @tap="closeSettle">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">核销 · {{ settleTarget.counterparty }}</view>
        <view class="sheet-sub">
          未结余额 ¥{{ formatAmount(outstanding(settleTarget)) }}
        </view>
        <view class="f-row">
          <text class="f-label">本次金额</text>
          <input class="f-input" type="digit" v-model="settleAmount"
                 :placeholder="'最多 ' + formatAmount(outstanding(settleTarget))" @input="onAmountInput" />
        </view>
        <view class="f-row">
          <text class="f-label">{{ type === 'receivable' ? '收款账户' : '付款账户' }}</text>
          <picker class="fill-picker" :range="accountNames" @change="onAccountChange">
            <view class="f-value" :class="{ placeholder: !settleAccountId }">
              {{ settleAccountId ? accountName : '请选择账户' }} ›
            </view>
          </picker>
        </view>
        <view class="f-row">
          <text class="f-label">备注</text>
          <input class="f-input" v-model="settleRemark" placeholder="选填" maxlength="50" />
        </view>
        <view class="sheet-actions">
          <view class="btn ghost" @tap="closeSettle">取消</view>
          <view class="btn" :class="{ disabled: submitting }" @tap="!submitting && doSettle()">
            {{ type === 'receivable' ? '确认收款' : '确认还款' }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { listAssets, listReceivables, listPayables, settleDebt } from '@/api/ledger';
import { applyAmountInput, formatAmount } from '@/utils/money';
import submitLock from '@/mixins/submitLock';

const FILTERS = [
  { key: '', label: '全部' },
  { key: 'pending', label: '未还' },
  { key: 'partial', label: '部分还' },
  { key: 'settled', label: '已结清' }
];

export default {
  components: { NavBar },
  mixins: [submitLock],
  data() {
    return {
      type: 'receivable',
      settleStatus: '',
      list: [],
      pageNum: 1,
      pages: 1,
      pageSize: 20,
      privacyMode: false,
      accounts: [],
      settleTarget: null,
      settleAmount: '',
      settleAccountId: null,
      settleRemark: ''
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    filters() { return FILTERS; },
    /** 当前页未结余额合计（不含已结清） */
    outstandingTotal() {
      return this.list.reduce((sum, d) => sum + this.outstanding(d), 0);
    },
    accountNames() { return this.accounts.map(a => a.name); },
    accountName() {
      const a = this.accounts.find(x => String(x.id) === String(this.settleAccountId));
      return a ? a.name : '';
    }
  },
  onLoad(options) {
    // 从资产负债页的分组点进来时带 type，直接定位到对应页签
    if (options && options.type === 'payable') this.type = 'payable';
  },
  onShow() {
    useThemeStore().restore();
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    this.loadAccounts();
    this.load();
  },
  methods: {
    formatAmount,
    /** 未结余额 = 总额 - 已结（与后端口径一致，不落库避免三方不一致） */
    outstanding(d) {
      return Number(d.totalAmount || 0) - Number(d.paidAmount || 0);
    },
    settleText(s) {
      return s === 'partial' ? '部分还' : (s === 'settled' ? '已结清' : '未还');
    },
    async loadAccounts() {
      try {
        const res = await listAssets() || {};
        this.accounts = res.records || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    async load() {
      try {
        const fn = this.type === 'receivable' ? listReceivables : listPayables;
        const res = await fn({
          settleStatus: this.settleStatus || undefined,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }) || {};
        this.list = res.records || [];
        this.pages = Number(res.pages) > 0 ? Number(res.pages) : 1;
      } catch (e) { /* 拦截器已提示 */ }
    },
    switchType(t) {
      if (this.type === t) return;
      this.type = t;
      this.pageNum = 1;
      this.load();
    },
    switchStatus(k) {
      this.settleStatus = k;
      this.pageNum = 1;
      this.load();
    },
    prevPage() {
      if (this.pageNum <= 1) return;
      this.pageNum -= 1;
      this.load();
    },
    nextPage() {
      if (this.pageNum >= this.pages) return;
      this.pageNum += 1;
      this.load();
    },
    openSettle(d) {
      // 已结清的没有可核销余额，直接提示而不进入表单
      if (d.settleStatus === 'settled') {
        uni.showToast({ title: '该笔已结清', icon: 'none' });
        return;
      }
      this.settleTarget = d;
      this.settleAmount = '';
      this.settleAccountId = null;
      this.settleRemark = '';
    },
    closeSettle() { this.settleTarget = null; },
    onAmountInput(e) { applyAmountInput(e, this, 'settleAmount'); },
    onAccountChange(e) {
      const a = this.accounts[e.detail.value];
      this.settleAccountId = a ? a.id : null;
    },
    async doSettle() {
      const t = this.settleTarget;
      if (!t) return;
      const amount = parseFloat(this.settleAmount);
      const rest = this.outstanding(t);
      if (!amount || amount <= 0) { uni.showToast({ title: '请输入金额', icon: 'none' }); return; }
      if (amount > rest) { uni.showToast({ title: '超过未结余额 ¥' + this.formatAmount(rest), icon: 'none' }); return; }
      if (!this.settleAccountId) { uni.showToast({ title: '请选择账户', icon: 'none' }); return; }
      await this.withSubmitLock(async () => {
        try {
          await settleDebt({
            debtType: this.type,
            debtId: t.id,
            amount,
            accountId: this.settleAccountId,
            remark: this.settleRemark || undefined
          });
          uni.showToast({ title: this.type === 'receivable' ? '已收款' : '已还款', icon: 'success' });
          this.closeSettle();
          this.load();
        } catch (e) { /* 拦截器已提示 */ }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.tabs { display: flex; margin: 20rpx 24rpx 0; background: var(--card); border-radius: 16rpx; overflow: hidden; }
.tab { flex: 1; text-align: center; padding: 24rpx 0; font-size: 28rpx; color: var(--text-sub); }
.tab.active { color: var(--primary-text); font-weight: 600; background: var(--primary-soft); }
.summary { padding: 24rpx 32rpx 0; }
.summary-label { font-size: 24rpx; color: var(--text-sub); }
.summary-amount { display: block; font-size: 44rpx; font-weight: 700; color: var(--text-main); margin-top: 8rpx; }
.filters { display: flex; gap: 16rpx; padding: 20rpx 24rpx 0; }
.filter-tag { font-size: 24rpx; padding: 10rpx 28rpx; border-radius: 28rpx; background: var(--surface); color: var(--text-sub); }
.filter-tag.active { background: var(--primary-soft); color: var(--primary-text); font-weight: 600; }
.empty { text-align: center; color: var(--text-muted); padding: 80rpx 0; font-size: 26rpx; }
.card { background: var(--card); border-radius: 20rpx; margin: 20rpx 24rpx 0; padding: 8rpx 32rpx; }
.row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid var(--border); }
.row:last-child { border-bottom: none; }
.row-name { font-size: 28rpx; color: var(--text-main); }
.row-sub { margin-top: 6rpx; font-size: 22rpx; color: var(--text-muted); }
.status { padding: 2rpx 12rpx; border-radius: 8rpx; background: var(--surface); color: var(--text-sub); }
.status.pending { background: var(--expense-bg); color: var(--expense); }
.status.partial { background: var(--warning-bg, var(--surface)); color: var(--warning); }
.status.settled { background: var(--income-bg); color: var(--income); }
.row-amount { text-align: right; flex-shrink: 0; margin-left: 16rpx; }
.amount-out { display: block; font-size: 30rpx; font-weight: 600; color: var(--text-main); }
.amount-total { display: block; font-size: 20rpx; color: var(--text-muted); margin-top: 4rpx; }
.pager { display: flex; align-items: center; justify-content: center; gap: 32rpx; padding: 28rpx 24rpx 40rpx; }
.pager-btn { font-size: 26rpx; padding: 14rpx 40rpx; border-radius: 32rpx; background: var(--surface); color: var(--text-main); }
.pager-btn.disabled { color: var(--text-muted); background: var(--card); }
.pager-info { font-size: 24rpx; color: var(--text-sub); }
.mask { position: fixed; top: 0; right: 0; bottom: 0; left: 0; background: rgba(0,0,0,0.45); z-index: 999; display: flex; align-items: flex-start; justify-content: center; overflow-y: auto; padding: 120rpx 0 60rpx; box-sizing: border-box; }
.sheet { width: 660rpx; margin: auto 0; flex-shrink: 0; background: var(--card); border-radius: 24rpx; padding: 32rpx; }
.sheet-title { font-size: 30rpx; font-weight: 600; text-align: center; color: var(--text-main); }
.sheet-sub { font-size: 24rpx; color: var(--text-sub); text-align: center; margin: 12rpx 0 20rpx; }
.f-row { display: flex; align-items: center; padding: 22rpx 0; border-bottom: 1rpx solid var(--border); }
.f-label { width: 160rpx; font-size: 26rpx; color: var(--text-main); flex-shrink: 0; }
.f-input { flex: 1; min-width: 0; font-size: 26rpx; text-align: right; color: var(--text-main); }
.f-value { font-size: 26rpx; color: var(--text-main); }
.f-value.placeholder { color: var(--text-muted); }
.sheet-actions { display: flex; gap: 20rpx; margin-top: 30rpx; }
.btn { flex: 1; text-align: center; padding: 22rpx 0; border-radius: 44rpx; font-size: 28rpx; background: var(--primary); color: var(--on-primary); }
.btn.ghost { background: var(--surface); color: var(--text-sub); }
.btn.disabled { opacity: 0.6; }
</style>
