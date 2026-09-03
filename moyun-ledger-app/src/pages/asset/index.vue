<template>
  <view class="page">
    <view class="summary">
      <view class="summary-label">总资产（计入合计的账户）</view>
      <view class="summary-amount">{{ privacyMode ? '****' : '¥ ' + totalText }}</view>
      <view class="summary-sub">共 {{ enabled.length }} 个账户</view>
    </view>

    <view class="card">
      <view class="card-title flex-row">
        <text class="flex-1">账户列表</text>
        <text class="link" @tap="showArchived = !showArchived">{{ showArchived ? '隐藏归档' : '显示归档' }}</text>
      </view>
      <view v-if="filteredList.length === 0" class="empty">还没有资产账户，点下方「+」添加</view>
      <view v-for="a in filteredList" :key="a.id" class="acct-row" @tap="editAccount(a)">
        <view class="acct-icon" :class="a.type">{{ typeText(a.type).slice(0, 1) }}</view>
        <view class="flex-1">
          <view class="acct-name">
            {{ a.name }}
            <text v-if="a.status === 0" class="archived-tag">已归档</text>
          </view>
          <view class="acct-type">{{ typeText(a.type) }}</view>
        </view>
        <view class="acct-balance">{{ privacyMode ? '****' : '¥ ' + balanceText(a) }}</view>
      </view>
    </view>

    <!-- 添加按钮 -->
    <view class="fab" @tap="addAccount">＋</view>

    <!-- 新增/编辑弹层 -->
    <view class="mask" v-if="editing" @tap="editing = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ form.id ? '编辑账户' : '新增资产账户' }}</view>
        <view class="field">
          <text class="field-label">名称</text>
          <input v-model="form.name" placeholder="如：招商银行储蓄卡" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">类型</text>
          <picker :range="typeNames" @change="onTypeChange">
            <view class="field-picker">{{ typeText(form.type) }} ▾</view>
          </picker>
        </view>
        <view class="field" v-if="!form.id">
          <text class="field-label">初始余额(元)</text>
          <input v-model="form.initialBalance" type="digit" placeholder="0.00" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">计入总资产</text>
          <switch :checked="form.includeInTotal === 1" @change="onTotalChange" style="transform: scale(0.8)" />
        </view>
        <view class="btn-primary" @tap="saveAccount">保存</view>
        <view class="btn-danger" v-if="form.id" @tap="removeAccount">删除（归档，流水保留）</view>
      </view>
    </view>
  </view>
</template>

<script>
import { listAssets, createAsset, updateAsset, deleteAsset } from '@/api/ledger';
import { centToAmount, yuanToCent } from '@/utils/money';

const TYPES = [
  ['cash', '现金'], ['savings', '储蓄卡'], ['ewallet', '电子钱包'],
  ['stored_value', '储值卡'], ['investment', '投资'], ['fixed_asset', '固定资产'],
  ['receivable', '债权'], ['other', '其他']
];

export default {
  data() {
    return {
      accounts: [],
      showArchived: false,
      privacyMode: false,
      editing: false,
      form: { id: null, name: '', type: 'cash', initialBalance: '', includeInTotal: 1 },
      typeNames: TYPES.map(t => t[1])
    };
  },
  computed: {
    enabled() { return this.accounts.filter(a => a.status === 1); },
    filteredList() { return this.showArchived ? this.accounts : this.enabled; },
    totalText() { return centToAmount(this.enabled.filter(a => a.includeInTotal === 1).reduce((s, a) => s + (a.balance || 0), 0)); }
  },
  onShow() {
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    this.load();
  },
  onPullDownRefresh() {
    this.load().finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    typeText(type) {
      const found = TYPES.find(t => t[0] === type);
      return found ? found[1] : type;
    },
    balanceText(a) { return centToAmount(a.balance); },
    async load() {
      try {
        const res = await listAssets(true);
        this.accounts = (res && res.records) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    addAccount() {
      this.form = { id: null, name: '', type: 'cash', initialBalance: '', includeInTotal: 1 };
      this.editing = true;
    },
    editAccount(a) {
      this.form = { id: a.id, name: a.name, type: a.type, initialBalance: '', includeInTotal: a.includeInTotal };
      this.editing = true;
    },
    onTypeChange(e) {
      this.form.type = TYPES[e.detail.value][0];
    },
    onTotalChange(e) {
      this.form.includeInTotal = e.detail.value ? 1 : 0;
    },
    async saveAccount() {
      if (!this.form.name.trim()) {
        uni.showToast({ title: '请输入账户名称', icon: 'none' });
        return;
      }
      try {
        if (this.form.id) {
          await updateAsset(this.form.id, {
            name: this.form.name, type: this.form.type, includeInTotal: this.form.includeInTotal
          });
        } else {
          await createAsset({
            name: this.form.name, type: this.form.type, includeInTotal: this.form.includeInTotal,
            initialBalance: yuanToCent(this.form.initialBalance)
          });
        }
        this.editing = false;
        this.load();
      } catch (e) { /* 拦截器已提示 */ }
    },
    removeAccount() {
      uni.showModal({
        title: '删除确认',
        content: '账户将归档停用，历史流水永久保留，是否继续？',
        success: async (r) => {
          if (r.confirm) {
            await deleteAsset(this.form.id);
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
.summary { background: linear-gradient(135deg, #27ae60, #2ecc71); color: #fff; padding: 50rpx 40rpx; margin-bottom: 24rpx; }
.summary-label { font-size: 26rpx; opacity: 0.85; }
.summary-amount { font-size: 60rpx; font-weight: 700; margin: 12rpx 0; }
.summary-sub { font-size: 24rpx; opacity: 0.8; }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin: 0 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 12rpx; }
.link { color: #6a4fd4; font-size: 26rpx; font-weight: 400; }
.empty { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }

.acct-row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.acct-icon {
  width: 72rpx; height: 72rpx; border-radius: 36rpx; margin-right: 20rpx;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 28rpx; font-weight: 600; background: #27ae60;
}
.acct-name { font-size: 28rpx; }
.archived-tag { font-size: 20rpx; color: #fff; background: #bbb; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.acct-type { font-size: 22rpx; color: #bbb; margin-top: 4rpx; }
.acct-balance { font-size: 32rpx; font-weight: 600; }

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
