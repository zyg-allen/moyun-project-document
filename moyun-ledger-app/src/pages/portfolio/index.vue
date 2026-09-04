<template>
  <view class="page" :style="themeVars">
    <!-- 顶部 Tab 切换 -->
    <view class="seg">
      <view class="seg-item" :class="{ on: activeTab === 'asset' }" @tap="switchTab('asset')">资产</view>
      <view class="seg-item" :class="{ on: activeTab === 'liability' }" @tap="switchTab('liability')">负债</view>
    </view>

    <!-- ========== 资产视图 ========== -->
    <template v-if="activeTab === 'asset'">
      <view class="summary">
        <view class="summary-label">总资产（计入合计的账户）</view>
        <view class="summary-amount">{{ privacyMode ? '****' : '¥ ' + assetTotalText }}</view>
        <view class="summary-sub">共 {{ assetEnabled.length }} 个账户</view>
      </view>

      <view class="card">
        <view class="card-title flex-row">
          <text class="flex-1">账户列表</text>
          <text class="link" @tap="showArchived = !showArchived">{{ showArchived ? '隐藏归档' : '显示归档' }}</text>
        </view>
        <view v-if="assetFilteredList.length === 0" class="empty">还没有资产账户，点下方「+」添加</view>
        <view v-for="a in assetFilteredList" :key="a.id" class="row" @tap="viewTxn('asset', a)">
          <view class="row-icon" :style="{ background: assetIcon(a.type).bg }">
            <text>{{ assetIcon(a.type).emoji }}</text>
          </view>
          <view class="flex-1">
            <view class="row-name">
              {{ a.name }}
              <text v-if="a.status === 0" class="archived-tag">已归档</text>
            </view>
            <view class="row-type">{{ assetTypeText(a.type) }}</view>
          </view>
          <view class="row-actions">
            <view class="pencil-btn" @tap.stop="editAsset(a)">✎</view>
            <view class="row-balance">{{ privacyMode ? '****' : '¥ ' + centToAmount(a.balance) }}</view>
          </view>
        </view>
      </view>
    </template>

    <!-- ========== 负债视图 ========== -->
    <template v-else>
      <view class="summary">
        <view class="summary-label">总负债（计入合计的账户）</view>
        <view class="summary-amount">{{ privacyMode ? '****' : '¥ ' + liabTotalText }}</view>
        <view class="summary-sub">
          在还 {{ liabActive.length }} 笔<text v-if="settledCount > 0"> · 已结清 {{ settledCount }} 笔</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title flex-row">
          <text class="flex-1">负债列表</text>
          <text class="link" @tap="showArchived = !showArchived">{{ showArchived ? '隐藏归档' : '显示全部' }}</text>
        </view>
        <view v-if="liabFilteredList.length === 0" class="empty">无债一身轻～点下方「＋」登记负债</view>
        <view v-for="l in liabFilteredList" :key="l.id" class="row" @tap="viewTxn('liability', l)">
          <view class="row-icon liab" :class="l.type">{{ liabTypeText(l.type).slice(0, 1) }}</view>
          <view class="flex-1">
            <view class="row-name">
              {{ l.name }}
              <text v-if="l.settleFlag === 1" class="settle-tag">已结清</text>
              <text v-if="l.status === 0" class="archived-tag">已归档</text>
            </view>
            <view class="row-type">
              {{ liabTypeText(l.type) }}
              <text v-if="l.monthlyPayment"> · 月供 ¥{{ centToAmount(l.monthlyPayment) }}</text>
              <text v-if="l.repaymentDay"> · {{ l.repaymentDay }}日还款</text>
              <text v-if="l.totalTerms"> · {{ l.paidTerms || 0 }}/{{ l.totalTerms }}期</text>
            </view>
          </view>
          <view class="row-actions">
            <view class="pencil-btn" @tap.stop="editLiability(l)">✎</view>
            <view class="row-balance">{{ privacyMode ? '****' : '¥ ' + centToAmount(l.balance) }}</view>
          </view>
        </view>
      </view>
    </template>

    <!-- 添加按钮（随 Tab 切换） -->
    <view class="fab" @tap="activeTab === 'asset' ? addAsset() : addLiability()">＋</view>

    <!-- 资产弹层 -->
    <view class="mask" v-if="assetEditing" @tap="assetEditing = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ assetForm.id ? '编辑账户' : '新增资产账户' }}</view>
        <view class="field">
          <text class="field-label">名称</text>
          <input v-model="assetForm.name" placeholder="如：招商银行储蓄卡" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">类型</text>
          <picker :range="assetTypeNames" @change="onAssetTypeChange">
            <view class="field-picker">{{ assetTypeText(assetForm.type) }} ▾</view>
          </picker>
        </view>
        <view class="field" v-if="!assetForm.id">
          <text class="field-label">初始余额(元)</text>
          <input v-model="assetForm.initialBalance" type="digit" placeholder="0.00" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">计入总资产</text>
          <switch :checked="assetForm.includeInTotal === 1" @change="onAssetTotalChange" style="transform: scale(0.8)" />
        </view>
        <view class="btn-primary" @tap="saveAsset">保存</view>
        <view class="btn-danger" v-if="assetForm.id" @tap="removeAsset">删除（归档，流水保留）</view>
        <view class="btn-cancel" @tap="assetEditing = false">取消</view>
      </view>
    </view>

    <!-- 负债弹层 -->
    <view class="mask" v-if="liabEditing" @tap="liabEditing = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ liabForm.id ? '编辑负债' : '新增负债' }}</view>
        <view class="field">
          <text class="field-label">名称</text>
          <input v-model="liabForm.name" placeholder="如：招行信用卡" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">类型</text>
          <picker :range="liabTypeNames" @change="onLiabTypeChange">
            <view class="field-picker">{{ liabTypeText(liabForm.type) }} ▾</view>
          </picker>
        </view>
        <view class="field" v-if="!liabForm.id">
          <text class="field-label">当前欠款(元)</text>
          <input v-model="liabForm.initialBalance" type="digit" placeholder="0.00" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">月供(元)</text>
          <input v-model="liabForm.monthlyPayment" type="digit" placeholder="选填" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">还款日</text>
          <picker :range="days" @change="onDayChange">
            <view class="field-picker">{{ liabForm.repaymentDay ? liabForm.repaymentDay + '日' : '选填' }} ▾</view>
          </picker>
        </view>
        <view class="field">
          <text class="field-label">总期数</text>
          <input v-model="liabForm.totalTerms" type="number" placeholder="选填" class="field-input" />
        </view>
        <view class="field">
          <text class="field-label">计入总负债</text>
          <switch :checked="liabForm.includeInTotal === 1" @change="onLiabTotalChange" style="transform: scale(0.8)" />
        </view>
        <view class="btn-primary" @tap="saveLiability">保存</view>
        <view class="btn-danger" v-if="liabForm.id" @tap="removeLiability">删除（归档，流水保留）</view>
        <view class="btn-cancel" @tap="liabEditing = false">取消</view>
      </view>
    </view>
  </view>
</template>

<script>
import { listAssets, createAsset, updateAsset, deleteAsset, listLiabilities, createLiability, updateLiability, deleteLiability } from '@/api/ledger';
import { centToAmount, centToYuan, yuanToCent, safeSumCents } from '@/utils/money';
import { useThemeStore } from '@/stores/theme';

const ASSET_TYPES = [
  ['cash', '现金'], ['savings', '储蓄卡'], ['ewallet', '电子钱包'],
  ['stored_value', '储值卡'], ['investment', '投资'], ['fixed_asset', '固定资产'],
  ['receivable', '债权'], ['other', '其他']
];
const ASSET_ICONS = {
  cash: { emoji: '💰', bg: '#F5C518' },
  savings: { emoji: '🏦', bg: '#4A9EFF' },
  ewallet: { emoji: '📱', bg: '#91CEA1' },
  stored_value: { emoji: '🎫', bg: '#FF9F43' },
  investment: { emoji: '📈', bg: '#6BCB77' },
  fixed_asset: { emoji: '🏠', bg: '#9AD2A9' },
  receivable: { emoji: '🤝', bg: '#EE8FA9' },
  other: { emoji: '🔖', bg: '#B0B0B0' }
};
const LIAB_TYPES = [
  ['credit_card', '信用卡'], ['consumer_loan', '消费贷'],
  ['bank_loan', '银行贷款'], ['personal_loan', '个人借款'], ['other', '其他']
];

export default {
  data() {
    return {
      activeTab: 'asset',
      // 资产
      accounts: [],
      showArchived: false,
      privacyMode: false,
      assetEditing: false,
      assetForm: { id: null, name: '', type: 'cash', initialBalance: '', includeInTotal: 1 },
      assetTypeNames: ASSET_TYPES.map(t => t[1]),
      // 负债
      liabilities: [],
      liabEditing: false,
      liabForm: this.emptyLiabForm(),
      liabTypeNames: LIAB_TYPES.map(t => t[1]),
      days: Array.from({ length: 28 }, (_, i) => (i + 1) + '日')
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    assetEnabled() { return this.accounts.filter(a => a.status === 1); },
    assetFilteredList() { return this.showArchived ? this.accounts : this.assetEnabled; },
    assetTotalText() {
      const list = this.assetEnabled.filter(a => a.includeInTotal === 1);
      return centToAmount(safeSumCents(list, 'balance'));
    },
    liabActive() { return this.liabilities.filter(l => l.status === 1 && l.settleFlag !== 1); },
    settledCount() { return this.liabilities.filter(l => l.settleFlag === 1).length; },
    liabFilteredList() {
      return this.showArchived ? this.liabilities : this.liabilities.filter(l => l.status === 1);
    },
    liabTotalText() {
      const list = this.liabilities.filter(l => l.status === 1 && l.settleFlag !== 1 && l.includeInTotal === 1);
      return centToAmount(safeSumCents(list, 'balance'));
    }
  },
  onShow() {
    useThemeStore().restore();
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    this.loadAssets();
    this.loadLiabilities();
  },
  onPullDownRefresh() {
    Promise.all([this.loadAssets(), this.loadLiabilities()]).finally(() => uni.stopPullDownRefresh());
  },
  methods: {
    viewTxn(kind, item) {
      const p = [];
      if (kind === 'asset') { p.push('accountId=' + item.id); p.push('name=' + encodeURIComponent(item.name)); }
      else { p.push('liabilityId=' + item.id); p.push('name=' + encodeURIComponent(item.name)); }
      uni.navigateTo({ url: '/pages/record/list?' + p.join('&') });
    },
    switchTab(tab) { this.activeTab = tab; },
    centToAmount,
    // ===== 资产 =====
    assetIcon(type) { return ASSET_ICONS[type] || ASSET_ICONS.other; },
    assetTypeText(type) {
      const found = ASSET_TYPES.find(t => t[0] === type);
      return found ? found[1] : type;
    },
    async loadAssets() {
      try {
        const res = await listAssets(true);
        this.accounts = (res && res.records) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    addAsset() {
      this.assetForm = { id: null, name: '', type: 'cash', initialBalance: '', includeInTotal: 1 };
      this.assetEditing = true;
    },
    editAsset(a) {
      this.assetForm = { id: a.id, name: a.name, type: a.type, initialBalance: '', includeInTotal: a.includeInTotal };
      this.assetEditing = true;
    },
    onAssetTypeChange(e) { this.assetForm.type = ASSET_TYPES[e.detail.value][0]; },
    onAssetTotalChange(e) { this.assetForm.includeInTotal = e.detail.value ? 1 : 0; },
    async saveAsset() {
      if (!this.assetForm.name.trim()) {
        uni.showToast({ title: '请输入账户名称', icon: 'none' });
        return;
      }
      try {
        if (this.assetForm.id) {
          await updateAsset(this.assetForm.id, {
            name: this.assetForm.name, type: this.assetForm.type, includeInTotal: this.assetForm.includeInTotal
          });
        } else {
          await createAsset({
            name: this.assetForm.name, type: this.assetForm.type, includeInTotal: this.assetForm.includeInTotal,
            initialBalance: yuanToCent(this.assetForm.initialBalance)
          });
        }
        this.assetEditing = false;
        this.loadAssets();
      } catch (e) { /* 拦截器已提示 */ }
    },
    removeAsset() {
      uni.showModal({
        title: '删除确认',
        content: '账户将归档停用，历史流水永久保留，是否继续？',
        success: async (r) => {
          if (r.confirm) {
            await deleteAsset(this.assetForm.id);
            this.assetEditing = false;
            this.loadAssets();
          }
        }
      });
    },
    // ===== 负债 =====
    emptyLiabForm() {
      return { id: null, name: '', type: 'credit_card', initialBalance: '', monthlyPayment: '', repaymentDay: null, totalTerms: '', includeInTotal: 1 };
    },
    liabTypeText(type) {
      const found = LIAB_TYPES.find(t => t[0] === type);
      return found ? found[1] : type;
    },
    async loadLiabilities() {
      try {
        const res = await listLiabilities(true);
        this.liabilities = (res && res.records) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    addLiability() {
      this.liabForm = this.emptyLiabForm();
      this.liabEditing = true;
    },
    editLiability(l) {
      this.liabForm = {
        id: l.id, name: l.name, type: l.type, initialBalance: '',
        monthlyPayment: l.monthlyPayment ? centToYuan(l.monthlyPayment) : '',
        repaymentDay: l.repaymentDay, totalTerms: l.totalTerms || '', includeInTotal: l.includeInTotal
      };
      this.liabEditing = true;
    },
    onLiabTypeChange(e) { this.liabForm.type = LIAB_TYPES[e.detail.value][0]; },
    onDayChange(e) { this.liabForm.repaymentDay = e.detail.value + 1; },
    onLiabTotalChange(e) { this.liabForm.includeInTotal = e.detail.value ? 1 : 0; },
    async saveLiability() {
      if (!this.liabForm.name.trim()) {
        uni.showToast({ title: '请输入负债名称', icon: 'none' });
        return;
      }
      try {
        if (this.liabForm.id) {
          await updateLiability(this.liabForm.id, {
            name: this.liabForm.name, type: this.liabForm.type, includeInTotal: this.liabForm.includeInTotal,
            monthlyPayment: this.liabForm.monthlyPayment ? yuanToCent(this.liabForm.monthlyPayment) : null,
            repaymentDay: this.liabForm.repaymentDay || null,
            totalTerms: this.liabForm.totalTerms ? Number(this.liabForm.totalTerms) : null
          });
        } else {
          await createLiability({
            name: this.liabForm.name, type: this.liabForm.type, includeInTotal: this.liabForm.includeInTotal,
            initialBalance: yuanToCent(this.liabForm.initialBalance),
            monthlyPayment: this.liabForm.monthlyPayment ? yuanToCent(this.liabForm.monthlyPayment) : null,
            repaymentDay: this.liabForm.repaymentDay || null,
            totalTerms: this.liabForm.totalTerms ? Number(this.liabForm.totalTerms) : null
          });
        }
        this.liabEditing = false;
        this.loadLiabilities();
      } catch (e) { /* 拦截器已提示 */ }
    },
    removeLiability() {
      uni.showModal({
        title: '删除确认',
        content: '负债将归档停用，历史流水永久保留，是否继续？',
        success: async (r) => {
          if (r.confirm) {
            await deleteLiability(this.liabForm.id);
            this.liabEditing = false;
            this.loadLiabilities();
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; }

/* 顶部 Tab 切换（吸顶，带安全区） */
.seg {
  position: sticky; top: 0; z-index: 10;
  display: flex; background: #fff;
  padding: calc(env(safe-area-inset-top) + 16rpx) 24rpx 16rpx;
}
.seg-item {
  flex: 1; text-align: center; font-size: 30rpx; color: #999;
  padding: 16rpx 0; border-radius: 40rpx; margin: 0 8rpx;
  background: #f5f5f7; transition: all 0.2s;
}
.seg-item.on { color: #fff; background: var(--primary); font-weight: 600; }

.summary {
  background: var(--primary); color: #fff;
  padding: 32rpx 40rpx 50rpx; margin-bottom: 24rpx;
}
.summary-label { font-size: 26rpx; opacity: 0.85; }
.summary-amount { font-size: 60rpx; font-weight: 700; margin: 12rpx 0; }
.summary-sub { font-size: 24rpx; opacity: 0.8; }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin: 0 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 12rpx; }
.link { color: var(--primary-strong); font-size: 26rpx; font-weight: 400; }
.empty { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }

.row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.row-icon {
  width: 72rpx; height: 72rpx; border-radius: 36rpx; margin-right: 20rpx;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 28rpx; font-weight: 600; background: var(--primary);
}
.row-name { font-size: 28rpx; }
.archived-tag { font-size: 20rpx; color: #fff; background: #bbb; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.settle-tag { font-size: 20rpx; color: #27ae60; border: 1rpx solid #27ae60; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.row-type { font-size: 22rpx; color: #bbb; margin-top: 4rpx; }
.row-balance { font-size: 32rpx; font-weight: 600; }

.fab {
  position: fixed; right: 40rpx; bottom: 200rpx;
  width: 100rpx; height: 100rpx; border-radius: 50rpx;
  background: var(--primary); color: #fff; font-size: 48rpx;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx var(--primary-shadow);
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
.btn-cancel {
  margin-top: 20rpx; text-align: center; color: #999; font-size: 26rpx;
  height: 72rpx; line-height: 72rpx;
}

.row-actions { display: flex; align-items: center; gap: 12rpx; flex-shrink: 0; }
.pencil-btn {
  width: 52rpx; height: 52rpx; line-height: 52rpx; text-align: center;
  border-radius: 50%; background: #f5f6f8; color: #888; font-size: 22rpx;
}
.pencil-btn:active { background: #e8e8ea; }
</style>
