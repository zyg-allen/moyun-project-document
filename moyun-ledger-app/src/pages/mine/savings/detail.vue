<template>
  <view class="page" :style="themeVars">
    <NavBar title="存钱详情" />

    <view v-if="loading" class="tip">加载中…</view>
    <view v-else-if="error" class="tip" @tap="load">加载失败，点击重试</view>

    <block v-else>
      <view class="summary">
        <view class="row"><text class="label">存钱方式</text><text class="val">{{ methodText }}</text></view>
        <view class="row"><text class="label">目标</text><text class="val">{{ money(plan.targetAmount) }} 元</text></view>
        <view class="row"><text class="label">进度</text><text class="val">{{ successCount }}/{{ periods.length }} 期 · {{ progress }}%</text></view>
        <view class="row"><text class="label">已攒</text><text class="val">{{ money(plan.currentAmount) }} 元</text></view>
        <view class="bar"><view class="fill" :style="{ width: progress + '%' }"></view></view>
      </view>

      <view class="card">
        <view class="grid">
          <template v-for="g in groups" :key="g.year">
            <view class="year">{{ g.year }} 年</view>
            <view v-for="r in g.items" :key="r.id" class="cell" :class="cellClass(r)" @tap="select(r)">
              <text v-if="r.id === selectedId && r.recordDate" class="cell-text">{{ shortDate(r.recordDate) }}</text>
              <text v-else class="cell-text">{{ r.periodIndex }}</text>
            </view>
          </template>
        </view>
        <view class="legend">
          <text class="dot pending"></text><text>未到期</text>
          <text class="dot unfinished"></text><text>未完成</text>
          <text class="dot done"></text><text>已完成</text>
        </view>
      </view>

      <view class="actions" v-if="selected">
        <template v-if="selected.status === 1">
          <view class="btn" @tap="revertDone">回退为未完成</view>
        </template>
        <template v-else-if="isUnfinished(selected)">
          <view class="btn primary" @tap="openTransfer">补录</view>
          <view class="btn" @tap="doneSheet = true">完成</view>
        </template>
        <template v-else>
          <view class="btn primary" @tap="doneSheet = true">提前完成</view>
        </template>
      </view>

      <view class="card info">
        <view class="info-row"><text>转出账户</text><text>{{ sourceName || '未绑定' }}</text></view>
        <view class="info-row"><text>存钱账户</text><text>{{ savingsName || '未绑定' }}</text></view>
        <view class="info-row" v-if="selected"><text>选中期次</text><text>第 {{ selected.periodIndex }} 期 · {{ selected.recordDate || '日期待定' }}</text></view>
      </view>
    </block>

    <view class="mask" v-if="doneSheet" @tap="doneSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">第 {{ selected && selected.periodIndex }} 期完成</view>
        <view class="sheet-item" @tap="quickMark(1)">仅标记完成</view>
        <view class="sheet-item" @tap="finishLink">完成并关联流水</view>
        <view class="sheet-item" @tap="finishTransfer">完成并创建流水</view>
        <view class="sheet-cancel" @tap="doneSheet = false">取消</view>
      </view>
    </view>

    <view class="mask" v-if="linkSheet" @tap="linkSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">选择要关联的流水</view>
        <scroll-view scroll-y class="link-list">
          <view v-for="t in linkable" :key="t.id" class="link-item" @tap="doLink(t)">
            <view class="link-main">
              <text class="link-name">{{ t.description || typeText(t.type) }}</text>
              <text class="link-sub">{{ t.transactionDate }}</text>
            </view>
            <text class="link-amt">{{ money(t.amount) }}</text>
          </view>
          <view v-if="!linkable.length" class="tip">近 15 天没有可关联的流水</view>
        </scroll-view>
        <view class="sheet-cancel" @tap="linkSheet = false">取消</view>
      </view>
    </view>

    <view class="mask" v-if="transferSheet" @tap="transferSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">创建转账并关联</view>
        <view class="f-row"><text class="f-label">金额</text><input class="f-input" type="digit" v-model="transfer.amount" /></view>
        <view class="f-row"><text class="f-label">日期</text><input class="f-input" type="text" v-model="transfer.date" placeholder="YYYY-MM-DD" /></view>
        <view class="f-row" @tap="pickSource"><text class="f-label">转出账户</text><text class="f-value">{{ sourceName || '请选择' }} ›</text></view>
        <view class="f-row"><text class="f-label">存入账户</text><text class="f-value">{{ savingsName || '按计划绑定' }}</text></view>
        <view class="btn primary" @tap="doTransfer">确认转账</view>
        <view class="sheet-cancel" @tap="transferSheet = false">取消</view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import {
  getSavingPlan, markSavingRecord, linkSavingTransaction, unlinkSavingTransaction,
  createSavingTransfer, listLinkableTransactions, listAssets
} from '@/api/ledger';
import { listOf } from '@/utils/response';
import { formatAmount } from '@/utils/money';

const TYPE_LABELS = { income: '收入', expense: '支出', transfer: '转账', repayment: '还款', borrow: '借款', adjust: '校准' };

export default {
  components: { NavBar },
  data() {
    return {
      id: null, loading: true, error: false,
      plan: {}, records: [], selectedId: null,
      doneSheet: false, linkSheet: false, transferSheet: false,
      linkable: [], assets: [],
      transfer: { amount: '', date: '', sourceAccountId: null, targetAccountId: null }
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    periods() { return this.records.slice().sort((a, b) => (a.periodIndex || 0) - (b.periodIndex || 0)); },
    groups() {
      const m = new Map();
      this.periods.forEach(r => {
        const y = (r.recordDate || '').slice(0, 4) || '待定';
        if (!m.has(y)) m.set(y, []);
        m.get(y).push(r);
      });
      return Array.from(m, ([year, items]) => ({ year, items }));
    },
    successCount() { return this.records.filter(r => r.status === 1).length; },
    progress() {
      const total = this.periods.length;
      return total ? Math.round((this.successCount / total) * 100) : 0;
    },
    selected() { return this.records.find(r => r.id === this.selectedId) || null; },
    methodText() {
      return { '52week': '52周存钱法', fixed: '固定金额分期', monthly: '每月固定存', custom: '自定义递增' }[this.plan.method] || '';
    },
    sourceName() { return this.accountName(this.plan.sourceAccountId); },
    savingsName() { return this.accountName(this.plan.savingsAccountId); }
  },
  onLoad(o) { this.id = o.id; this.load(); },
  methods: {
    money(v) { return formatAmount(Number(v || 0)); },
    typeText(t) { return TYPE_LABELS[t] || t || '流水'; },
    shortDate(d) { return d.slice(5).replace('-', '/'); },
    today() {
      const d = new Date();
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    },
    isUnfinished(r) {
      return r.status === 2 || (r.status === 0 && r.recordDate && r.recordDate < this.today());
    },
    accountName(id) {
      if (!id) return '';
      const a = this.assets.find(x => String(x.id) === String(id));
      return a ? a.name : '';
    },
    async load() {
      this.loading = true; this.error = false;
      try {
        const data = await getSavingPlan(this.id);
        this.plan = (data && data.plan) || {};
        this.records = (data && data.records) || [];
        if (!this.selectedId && this.periods.length) {
          const p = this.periods.find(r => r.status === 0);
          this.selectedId = (p || this.periods[0]).id;
        }
        try { this.assets = listOf(await listAssets()); } catch (e) { this.assets = []; }
      } catch (e) { this.error = true; } finally { this.loading = false; }
    },
    select(r) { if (r) this.selectedId = r.id; },
    cellClass(r) {
      return {
        done: r.status === 1,
        unfinished: this.isUnfinished(r),
        active: r.id === this.selectedId
      };
    },
    async quickMark(status, reason) {
      if (!this.selected) return;
      const amount = status === 1 ? Number(this.selected.amount || this.selected.targetAmount || 0) : null;
      try {
        await markSavingRecord(this.id, this.selected.id, { status, amount, reason: reason || null });
        this.doneSheet = false;
        uni.showToast({ title: '已更新', icon: 'success' });
        await this.load();
      } catch (e) { /* 提示 */ }
    },
    revertDone() {
      uni.showModal({
        title: '回退为未完成',
        content: '会解除该期与流水的关联（不删除流水），状态变为未完成。',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            if (this.selected.transactionId) {
              await unlinkSavingTransaction(this.id, this.selected.id, false);
            }
            await markSavingRecord(this.id, this.selected.id, { status: 2, amount: null, reason: '手动回退' });
            uni.showToast({ title: '已回退', icon: 'none' });
            await this.load();
          } catch (e) { /* 提示 */ }
        }
      });
    },
    finishLink() { this.doneSheet = false; this.openLink(); },
    finishTransfer() { this.doneSheet = false; this.openTransfer(); },
    async openLink() {
      try {
        this.linkable = listOf(await listLinkableTransactions(this.id, {
          date: this.selected && this.selected.recordDate,
          amount: this.selected && this.selected.targetAmount
        }));
        this.linkSheet = true;
      } catch (e) { /* 提示 */ }
    },
    async doLink(t) {
      try {
        await linkSavingTransaction(this.id, this.selected.id, t.id);
        this.linkSheet = false;
        uni.showToast({ title: '已关联', icon: 'success' });
        await this.load();
      } catch (e) { /* 提示 */ }
    },
    openTransfer() {
      const r = this.selected || {};
      this.transfer = {
        amount: String(r.targetAmount || ''),
        date: r.recordDate || this.today(),
        sourceAccountId: this.plan.sourceAccountId || null,
        targetAccountId: this.plan.savingsAccountId || null
      };
      this.transferSheet = true;
    },
    pickSource() {
      if (!this.assets.length) { uni.showToast({ title: '暂无可用账户', icon: 'none' }); return; }
      uni.showActionSheet({
        itemList: this.assets.slice(0, 6).map(a => a.name),
        success: (res) => {
          const a = this.assets[res.tapIndex];
          if (a) this.transfer.sourceAccountId = a.id;
        }
      });
    },
    async doTransfer() {
      if (!this.transfer.sourceAccountId) { uni.showToast({ title: '请选择转出账户', icon: 'none' }); return; }
      try {
        await createSavingTransfer(this.id, this.selected.id, {
          accountId: this.transfer.sourceAccountId,
          targetAccountId: this.transfer.targetAccountId,
          amount: Number(this.transfer.amount || 0),
          transactionDate: this.transfer.date,
          remark: '存钱：第 ' + (this.selected.periodIndex || '') + ' 期'
        });
        this.transferSheet = false;
        uni.showToast({ title: '已转入并关联', icon: 'success' });
        await this.load();
      } catch (e) { /* 提示 */ }
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: var(--surface); padding-bottom: 40rpx; }
.tip { padding: 120rpx 0; text-align: center; color: var(--text-muted); font-size: 26rpx; }
.summary { margin: 20rpx; padding: 20rpx 24rpx; border-radius: 18rpx; background: var(--card); }
.row { display: flex; justify-content: space-between; padding: 8rpx 0; font-size: 24rpx; }
.label { color: var(--text-sub); }
.val { color: var(--text-main); font-weight: 600; }
.bar { height: 8rpx; border-radius: 999rpx; background: var(--surface); margin-top: 12rpx; overflow: hidden; }
.fill { height: 100%; background: var(--primary); border-radius: 999rpx; }
.card { margin: 0 20rpx 14rpx; padding: 18rpx; border-radius: 18rpx; background: var(--card); }
.grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6rpx; justify-items: center; }
.year { grid-column: 1 / -1; font-size: 20rpx; color: var(--text-muted); padding: 10rpx 0 4rpx; }
.cell { width: 58rpx; height: 58rpx; border-radius: 50%; background: var(--surface); display: flex; align-items: center; justify-content: center; border: 2rpx solid transparent; }
.cell-text { font-size: 22rpx; font-weight: 600; color: var(--text-muted); }
.cell.done { background: var(--income); }
.cell.done .cell-text { color: var(--on-income); }
.cell.unfinished { background: rgba(232, 101, 74, 0.14); }
.cell.unfinished .cell-text { color: var(--expense); }
.cell.active { transform: scale(1.22); border-color: var(--primary); z-index: 2; box-shadow: 0 4rpx 12rpx rgba(0,0,0,.12); }
.cell.active .cell-text { font-size: 20rpx; }
.cell.done.active { box-shadow: 0 0 0 3rpx var(--income); }
.cell.unfinished.active { box-shadow: 0 0 0 3rpx var(--expense); }
.legend { display: flex; align-items: center; gap: 6rpx; margin-top: 14rpx; font-size: 18rpx; color: var(--text-muted); }
.dot { width: 14rpx; height: 14rpx; border-radius: 50%; display: inline-block; margin-left: 10rpx; }
.dot.pending { background: var(--border); }
.dot.unfinished { background: var(--expense); }
.dot.done { background: var(--income); }
.actions { display: flex; gap: 12rpx; margin: 0 20rpx 14rpx; }
.btn { flex: 1; text-align: center; padding: 16rpx 0; border-radius: 12rpx; font-size: 25rpx; background: var(--card); color: var(--text-main); border: 1rpx solid var(--border); }
.btn.primary { background: var(--primary); color: var(--on-primary); border-color: var(--primary); }
.info-row { display: flex; justify-content: space-between; padding: 8rpx 0; font-size: 23rpx; color: var(--text-sub); }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 2000; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 28rpx; padding-bottom: calc(28rpx + env(safe-area-inset-bottom)); }
.sheet-title { text-align: center; font-size: 28rpx; font-weight: 600; color: var(--text-main); margin-bottom: 12rpx; }
.sheet-item { padding: 26rpx 8rpx; font-size: 28rpx; color: var(--text-main); border-top: 1rpx solid var(--border); }
.sheet-cancel { margin-top: 16rpx; text-align: center; padding: 22rpx; font-size: 26rpx; color: var(--text-muted); }
.link-list { max-height: 520rpx; }
.link-item { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 4rpx; border-top: 1rpx solid var(--border); }
.link-main { display: flex; flex-direction: column; gap: 4rpx; }
.link-name { font-size: 26rpx; color: var(--text-main); }
.link-sub { font-size: 21rpx; color: var(--text-muted); }
.link-amt { font-size: 26rpx; color: var(--text-main); }
.f-row { display: flex; align-items: center; justify-content: space-between; padding: 22rpx 0; border-top: 1rpx solid var(--border); }
.f-label { font-size: 26rpx; color: var(--text-sub); }
.f-input { text-align: right; font-size: 26rpx; color: var(--text-main); flex: 1; }
.f-value { font-size: 26rpx; color: var(--text-main); }
</style>
