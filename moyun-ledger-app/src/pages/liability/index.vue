<template>
  <view class="page">
    <view class="summary">
      <view class="summary-label">总负债（计入合计的账户）</view>
      <view class="summary-amount">{{ privacyMode ? '****' : '¥ ' + totalText }}</view>
      <view class="summary-sub">
        在还 {{ activeList.length }} 笔<text v-if="settledCount > 0"> · 已结清 {{ settledCount }} 笔</text>
      </view>
    </view>

    <view class="card">
      <view class="card-title flex-row">
        <text class="flex-1">负债列表</text>
        <text class="link" @tap="showArchived = !showArchived">{{ showArchived ? '隐藏归档' : '显示全部' }}</text>
      </view>
      <view v-if="filteredList.length === 0" class="empty">无债一身轻～点下方「＋」登记负债</view>
      <view v-for="l in filteredList" :key="l.id" class="liab-row" @tap="editAccount(l)">
        <view class="liab-icon" :class="l.type">{{ typeText(l.type).slice(0, 1) }}</view>
        <view class="flex-1">
          <view class="liab-name">
            {{ l.name }}
            <text v-if="l.settleFlag === 1" class="settle-tag">已结清</text>
            <text v-if="l.status === 0" class="archived-tag">已归档</text>
          </view>
          <view class="liab-meta">
            {{ typeText(l.type) }}
            <text v-if="l.monthlyPayment"> · 月供 ¥{{ monthlyText(l) }}</text>
            <text v-if="l.repaymentDay"> · {{ l.repaymentDay }}日还款</text>
            <text v-if="l.totalTerms"> · {{ l.paidTerms || 0 }}/{{ l.totalTerms }}期</text>
          </view>
        </view>
        <view class="liab-balance">{{ privacyMode ? '****' : '¥ ' + balanceText(l) }}</view>
      </view>
    </view>

    <view class="fab" @tap="addAccount">＋</view>

    <view class="mask" v-if="editing" @tap="editing = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ form.id ? '编辑负债' : '新增负债' }}</view>
        <view class="field">
          <text class="field-label">名称</text>
          <input v-model="form.name" placeholder="如：招行信用卡" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">类型</text>
          <picker :range="typeNames" @change="onTypeChange">
            <view class="field-picker">{{ typeText(form.type) }} ▾</view>
          </picker>
        </view>
        <view class="field" v-if="!form.id">
          <text class="field-label">当前欠款(元)</text>
          <input v-model="form.initialBalance" type="digit" placeholder="0.00" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">月供(元)</text>
          <input v-model="form.monthlyPayment" type="digit" placeholder="选填" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">还款日</text>
          <picker :range="days" @change="onDayChange">
            <view class="field-picker">{{ form.repaymentDay ? form.repaymentDay + '日' : '选填' }} ▾</view>
          </picker>
        </view>
        <view class="field">
          <text class="field-label">总期数</text>
          <input v-model="form.totalTerms" type="number" placeholder="选填" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">计入总负债</text>
          <switch :checked="form.includeInTotal === 1" @change="onTotalChange" style="transform: scale(0.8)" />
        </view>
        <view class="btn-primary" @tap="saveAccount">保存</view>
        <view class="btn-danger" v-if="form.id" @tap="removeAccount">删除（归档，流水保留）</view>
      </view>
    </view>
  </view>
</template>

<script>
import { listLiabilities, createLiability, updateLiability, deleteLiability } from '@/api/ledger';
import { centToAmount, yuanToCent } from '@/utils/money';

const TYPES = [
  ['credit_card', '信用卡'], ['consumer_loan', '消费贷'],
  ['bank_loan', '银行贷款'], ['personal_loan', '个人借款'], ['other', '其他']
];

export default {
  data() {
    return {
      liabilities: [],
      showArchived: false,
      privacyMode: false,
      editing: false,
      typeNames: TYPES.map(t => t[1]),
      days: Array.from({ length: 28 }, (_, i) => (i + 1) + '日'),
      form: this.emptyForm()
    };
  },
  computed: {
    activeList() { return this.liabilities.filter(l => l.status === 1 && l.settleFlag !== 1); },
    settledCount() { return this.liabilities.filter(l => l.settleFlag === 1).length; },
    filteredList() {
      return this.showArchived ? this.liabilities : this.liabilities.filter(l => l.status === 1);
    },
    totalText() {
      return centToAmount(
        this.liabilities.filter(l => l.status === 1 && l.settleFlag !== 1 && l.includeInTotal === 1)
          .reduce((s, l) => s + (l.balance || 0), 0)
      );
    }
  },
  onShow() {
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    this.load();
  },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    emptyForm() {
      return { id: null, name: '', type: 'credit_card', initialBalance: '', monthlyPayment: '', repaymentDay: null, totalTerms: '', includeInTotal: 1 };
    },
    typeText(type) {
      const found = TYPES.find(t => t[0] === type);
      return found ? found[1] : type;
    },
    balanceText(l) { return centToAmount(l.balance); },
    monthlyText(l) { return centToAmount(l.monthlyPayment); },
    async load() {
      try {
        const res = await listLiabilities(true);
        this.liabilities = (res && res.records) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    addAccount() {
      this.form = this.emptyForm();
      this.editing = true;
    },
    editAccount(l) {
      this.form = {
        id: l.id, name: l.name, type: l.type, initialBalance: '',
        monthlyPayment: l.monthlyPayment ? centToAmount(l.monthlyPayment) : '',
        repaymentDay: l.repaymentDay, totalTerms: l.totalTerms || '', includeInTotal: l.includeInTotal
      };
      this.editing = true;
    },
    onTypeChange(e) { this.form.type = TYPES[e.detail.value][0]; },
    onDayChange(e) { this.form.repaymentDay = e.detail.value + 1; },
    onTotalChange(e) { this.form.includeInTotal = e.detail.value ? 1 : 0; },
    async saveAccount() {
      if (!this.form.name.trim()) {
        uni.showToast({ title: '请输入负债名称', icon: 'none' });
        return;
      }
      try {
        if (this.form.id) {
          await updateLiability(this.form.id, {
            name: this.form.name, type: this.form.type, includeInTotal: this.form.includeInTotal,
            monthlyPayment: this.form.monthlyPayment ? yuanToCent(this.form.monthlyPayment) : null,
            repaymentDay: this.form.repaymentDay || null,
            totalTerms: this.form.totalTerms ? Number(this.form.totalTerms) : null
          });
        } else {
          await createLiability({
            name: this.form.name, type: this.form.type, includeInTotal: this.form.includeInTotal,
            initialBalance: yuanToCent(this.form.initialBalance),
            monthlyPayment: this.form.monthlyPayment ? yuanToCent(this.form.monthlyPayment) : null,
            repaymentDay: this.form.repaymentDay || null,
            totalTerms: this.form.totalTerms ? Number(this.form.totalTerms) : null
          });
        }
        this.editing = false;
        this.load();
      } catch (e) { /* 拦截器已提示 */ }
    },
    removeAccount() {
      uni.showModal({
        title: '删除确认',
        content: '负债将归档停用，历史流水永久保留，是否继续？',
        success: async (r) => {
          if (r.confirm) {
            await deleteLiability(this.form.id);
            this.editing = false;
            this.load();
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; }
.summary { background: linear-gradient(135deg, #c0392b, #e74c3c); color: #fff; padding: 50rpx 40rpx; margin-bottom: 24rpx; }
.summary-label { font-size: 26rpx; opacity: 0.85; }
.summary-amount { font-size: 60rpx; font-weight: 700; margin: 12rpx 0; }
.summary-sub { font-size: 24rpx; opacity: 0.8; }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin: 0 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 12rpx; }
.link { color: #6a4fd4; font-size: 26rpx; font-weight: 400; }
.empty { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }

.liab-row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.liab-icon {
  width: 72rpx; height: 72rpx; border-radius: 36rpx; margin-right: 20rpx;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 28rpx; font-weight: 600; background: #e74c3c;
}
.liab-name { font-size: 28rpx; }
.settle-tag { font-size: 20rpx; color: #27ae60; border: 1rpx solid #27ae60; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.archived-tag { font-size: 20rpx; color: #fff; background: #bbb; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.liab-meta { font-size: 22rpx; color: #bbb; margin-top: 4rpx; }
.liab-balance { font-size: 32rpx; font-weight: 600; }

.fab {
  position: fixed; right: 40rpx; bottom: 200rpx;
  width: 100rpx; height: 100rpx; border-radius: 50rpx;
  background: #6a4fd4; color: #fff; font-size: 48rpx;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(106, 79, 212, 0.4);
}

.mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 99; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 40rpx 32rpx calc(40rpx + env(safe-area-inset-bottom)); }
.sheet-title { font-size: 34rpx; font-weight: 600; text-align: center; margin-bottom: 32rpx; }
.field { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.field-label { width: 200rpx; font-size: 28rpx; color: #666; }
.field-input { flex: 1; font-size: 28rpx; text-align: right; }
.field-picker { flex: 1; font-size: 28rpx; text-align: right; color: #333; }
.btn-primary { margin-top: 40rpx; }
.btn-danger {
  margin-top: 24rpx; text-align: center; color: #e74c3c; font-size: 28rpx;
  border: 1rpx solid #e74c3c; border-radius: 44rpx; height: 80rpx; line-height: 80rpx;
}
</style>
