<template>
  <view class="page">
    <!-- 类型切换 -->
    <view class="type-bar">
      <view v-for="t in types" :key="t.key" class="type-item" :class="{ active: form.type === t.key }" @tap="switchType(t.key)">
        {{ t.label }}
      </view>
    </view>

    <!-- 类型语义提示 -->
    <view class="type-hint" v-if="typeHint">{{ typeHint }}</view>

    <view class="body">
      <!-- 金额输入区（收起键盘时点击展开） -->
      <view class="amount-area" @tap="kbVisible = true">
        <text class="currency">¥</text>
        <input v-model="amountYuan" type="digit" class="amount-input" placeholder="0.00"
               :placeholder-style="'color:rgba(255,255,255,0.4);font-size:64rpx'" />
        <text v-if="!kbVisible" class="kb-open-btn">⌨ 输入</text>
      </view>

      <!-- 账户/负债选择行 -->
      <view class="pick-row" @tap="pickAsset">
        <text class="pick-label">{{ accountLabel }}</text>
        <text class="pick-value">{{ selectedAssetName || '选择账户' }} ▾</text>
      </view>
      <view class="pick-row" v-if="needTarget" @tap="pickTarget">
        <text class="pick-label">转入账户</text>
        <text class="pick-value">{{ selectedTargetName || '选择账户' }} ▾</text>
      </view>
      <view class="pick-row" v-if="needLiability" @tap="pickLiability">
        <text class="pick-label">{{ form.type === 'repayment' ? '还款至' : '借款自' }}</text>
        <text class="pick-value">{{ selectedLiabilityName || '选择借款项目' }} ▾</text>
      </view>

      <!-- 分类选择（所有类型可选标签） -->
      <view class="pick-row" @tap="pickCategory">
        <text class="pick-label">分类</text>
        <text class="pick-value muted">{{ selectedCategoryName || '选填，打标签' }} ▾</text>
      </view>

      <!-- 备注/商户 -->
      <view class="pick-row">
        <text class="pick-label">备注</text>
        <input v-model="form.description" placeholder="选填" class="pick-input" />
      </view>
      <view class="pick-row" v-if="form.type === 'expense'">
        <text class="pick-label">商户</text>
        <input v-model="form.merchant" placeholder="选填" class="pick-input" />
      </view>

      <!-- 日期 -->
      <view class="pick-row" @tap="pickDate">
        <text class="pick-label">日期</text>
        <text class="pick-value">{{ form.transactionDate }} ▾</text>
      </view>

      <!-- 凭证截图 -->
      <view class="voucher-row">
        <text class="pick-label">凭证</text>
        <view class="voucher-area" @tap="chooseVoucher">
          <image v-if="form.voucherUrl" :src="form.voucherUrl" mode="aspectFill" class="voucher-thumb"
                 @tap.stop="previewVoucher" />
          <view v-else class="voucher-add">
            <text class="voucher-add-icon">📷</text>
            <text class="voucher-add-text">截图</text>
          </view>
          <view v-if="form.voucherUrl" class="voucher-remove" @tap.stop="removeVoucher">×</view>
        </view>
      </view>
    </view>

    <!-- 数字键盘（可收起） -->
    <view class="keyboard" v-if="kbVisible">
      <view class="kb-handle" @tap="kbVisible = false">
        <text class="kb-handle-text">收起键盘 ∨</text>
      </view>
      <view class="kb-row" v-for="(row, i) in keys" :key="i">
        <view v-for="k in row" :key="k" class="kb-key" :class="{ 'kb-fn': k === 'del' }" @tap="tapKey(k)">
          <text v-if="k !== 'del'">{{ k }}</text>
          <text v-else class="kb-del">⌫</text>
        </view>
      </view>
    </view>

    <!-- 保存 -->
    <view class="save-bar">
      <view class="btn-save" :class="{ disabled: !canSave }" @tap="save">保存</view>
    </view>

    <!-- 选择弹层 -->
    <view class="mask" v-if="picker.visible" @tap="picker.visible = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">{{ picker.title }}</view>

        <!-- 负债快速补录表单 -->
        <view v-if="picker.mode === 'liability' && !quickAdd.visible" class="quick-add-entry" @tap="quickAdd.visible = true">
          ＋ 没有要选的借款项目？快速补录一个
        </view>
        <view v-if="picker.mode === 'liability' && quickAdd.visible" class="quick-add-form">
          <view class="qa-title">快速补录借款项目</view>
          <view class="qa-row">
            <text class="qa-label">名称</text>
            <input v-model="quickAdd.name" placeholder="如：向朋友A借款" class="qa-input" />
          </view>
          <view class="qa-row">
            <text class="qa-label">当前欠款(元)</text>
            <input v-model="quickAdd.balanceYuan" type="digit" placeholder="0.00" class="qa-input" />
          </view>
          <view class="qa-actions">
            <view class="qa-btn cancel" @tap="quickAdd.visible = false">取消</view>
            <view class="qa-btn ok" :class="{ disabled: !quickAddOk }" @tap="saveQuickAdd">保存并选用</view>
          </view>
        </view>

        <scroll-view scroll-y class="picker-list" v-if="!quickAdd.visible">
          <view v-if="!picker.items.length" class="picker-empty" @tap="pickerEmptyTap">{{ picker.empty || '暂无数据' }}</view>
          <view v-for="item in picker.items" :key="item.id" class="picker-item"
                :class="{ active: picker.selectedId === item.id }" @tap="confirmPick(item)">
            <text>{{ item.name }}</text>
            <text class="picker-sub" v-if="item.sub">{{ item.sub }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { createTransaction, listAssets, listLiabilities, listCategories, createLiability, uploadVoucher } from '@/api/ledger';
import { yuanToCent, centToAmount } from '@/utils/money';
import { useUserStore } from '@/stores/user';

const TYPE_DEFS = [
  { key: 'expense', label: '支出' },
  { key: 'income', label: '收入' },
  { key: 'transfer', label: '转账' },
  { key: 'repayment', label: '还款' },
  { key: 'borrow', label: '借款' },
  { key: 'adjust', label: '校准' }
];

const TYPE_HINTS = {
  transfer: '转账 = 资产账户间互转：A 减、B 加，总资产不变',
  repayment: '还款 = 资产账户出钱，欠款对应减少；余额不足会提示补录资金来源',
  borrow: '借款 = 欠款增加，钱进入所选资产账户（可不选账户，仅记录欠款）',
  adjust: '校准 = 将账户余额直接修正为目标值（差额可为负）'
};

export default {
  data() {
    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    return {
      types: TYPE_DEFS,
      amountYuan: '',
      assets: [],
      liabilities: [],
      categories: [],
      kbVisible: true,
      quickAdd: { visible: false, name: '', balanceYuan: '' },
      form: {
        type: 'expense', accountId: null, targetAccountId: null, liabilityId: null,
        categoryId: null, description: '', merchant: '', voucherUrl: '',
        transactionDate: `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
      },
      keys: [
        ['1', '2', '3'],
        ['4', '5', '6'],
        ['7', '8', '9'],
        ['.', '0', 'del']
      ],
      picker: { visible: false, mode: '', title: '', items: [], selectedId: null }
    };
  },
  computed: {
    typeHint() { return TYPE_HINTS[this.form.type] || ''; },
    needTarget() { return this.form.type === 'transfer'; },
    needLiability() { return ['repayment', 'borrow'].includes(this.form.type); },
    accountLabel() {
      return { expense: '支出账户', income: '收入账户', transfer: '转出账户', repayment: '扣款账户', borrow: '入账账户(选填)', adjust: '校准账户' }[this.form.type] || '账户';
    },
    selectedAssetName() {
      const a = this.assets.find(x => x.id === this.form.accountId);
      return a ? a.name : '';
    },
    selectedTargetName() {
      const a = this.assets.find(x => x.id === this.form.targetAccountId);
      return a ? a.name : '';
    },
    selectedLiabilityName() {
      const l = this.liabilities.find(x => x.id === this.form.liabilityId);
      return l ? `${l.name}（欠 ${centToAmount(l.balance)}）` : '';
    },
    selectedCategoryName() {
      const c = this.categories.find(x => x.id === this.form.categoryId);
      return c ? c.name : '';
    },
    quickAddOk() {
      return !!(this.quickAdd.name && this.quickAdd.name.trim()) && yuanToCent(this.quickAdd.balanceYuan) > 0;
    },
    canSave() {
      const cent = yuanToCent(this.amountYuan);
      if (this.form.type === 'adjust') return cent !== 0 && !!this.form.accountId;
      if (cent <= 0) return false;
      if (this.needTarget) return !!this.form.accountId && !!this.form.targetAccountId;
      if (this.needLiability) return !!this.form.liabilityId && (this.form.type === 'borrow' || !!this.form.accountId);
      return !!this.form.accountId;
    }
  },
  onShow() {
    const userStore = useUserStore();
    if (!userStore.isLoggedIn) {
      // 未登录：清空选项并引导去「我的」登录，避免弹出空列表
      this.assets = [];
      this.liabilities = [];
      this.categories = [];
      this.promptLogin();
      return;
    }
    this.loadOptions();
  },
  methods: {
    promptLogin() {
      uni.showModal({
        title: '提示',
        content: '登录后才能选择账户和负债，请先在「我的」页面登录',
        confirmText: '去登录',
        success: (r) => {
          if (r.confirm) uni.switchTab({ url: '/pages/mine/index' });
        }
      });
    },
    async loadOptions() {
      try {
        const [assets, liabilities] = await Promise.all([listAssets(false), listLiabilities(false)]);
        this.assets = (assets && assets.records) || [];
        this.liabilities = ((liabilities && liabilities.records) || []).filter(l => l.settleFlag !== 1);
        this.loadCategories();
      } catch (e) {
        // 登录过期（HTTP 200 + code:401）时拦截器已清除 token，这里引导登录
        if (e && e.code === 401) this.promptLogin();
      }
    },
    async loadCategories() {
      try {
        // 不区分类型拉全部可用分类，任何行为都可打标签
        const res = await listCategories();
        this.categories = (res && res.records) || [];
      } catch (e) { /* 忽略 */ }
    },
    switchType(key) {
      this.form.type = key;
      this.form.categoryId = null;
    },
    pickAsset() {
      this.openPicker('asset', '选择账户', this.assets.map(a => ({
        id: a.id, name: a.name, sub: '余额 ¥' + centToAmount(a.balance)
      })), this.form.accountId);
    },
    pickTarget() {
      this.openPicker('target', '选择转入账户', this.assets.filter(a => a.id !== this.form.accountId)
        .map(a => ({ id: a.id, name: a.name, sub: '余额 ¥' + centToAmount(a.balance) })), this.form.targetAccountId);
    },
    pickLiability() {
      this.openPicker('liability', '选择借款项目', this.liabilities.map(l => ({
        id: l.id, name: l.name, sub: '欠款 ¥' + centToAmount(l.balance)
      })), this.form.liabilityId);
    },
    pickCategory() {
      this.openPicker('category', '选择分类', this.categories.map(c => ({
        id: c.id, name: c.name
      })), this.form.categoryId);
    },
    openPicker(mode, title, items, selectedId) {
      this.quickAdd.visible = false;
      const emptyMap = {
        asset: '暂无可用账户，点击去「资产」页添加',
        target: '暂无可用账户，点击去「资产」页添加',
        liability: '暂无借款项目，可在上方快速补录',
        category: '暂无分类，点击去「我的-分类管理」添加'
      };
      this.picker = { visible: true, mode, title, items, selectedId, empty: emptyMap[mode] || '暂无数据' };
    },
    pickerEmptyTap() {
      const mode = this.picker.mode;
      this.picker.visible = false;
      if (mode === 'liability') {
        uni.switchTab({ url: '/pages/liability/index' });
      } else if (mode === 'category') {
        uni.navigateTo({ url: '/pages/mine/categories/index' });
      } else {
        uni.switchTab({ url: '/pages/asset/index' });
      }
    },
    confirmPick(item) {
      const mode = this.picker.mode;
      if (mode === 'asset') this.form.accountId = item.id;
      if (mode === 'target') this.form.targetAccountId = item.id;
      if (mode === 'liability') this.form.liabilityId = item.id;
      if (mode === 'category') this.form.categoryId = item.id;
      this.picker.visible = false;
    },
    /** 快速补录借款项目（含初始欠款），保存后自动选用 */
    async saveQuickAdd() {
      if (!this.quickAddOk) return;
      try {
        const created = await createLiability({
          name: this.quickAdd.name.trim(),
          type: 'personal', // 默认私人借款
          initialBalance: yuanToCent(this.quickAdd.balanceYuan),
          includeInTotal: 1
        });
        await this.refreshLiabilities();
        const id = created && created.id;
        if (id) {
          this.form.liabilityId = id;
          uni.showToast({ title: '已补录并选用', icon: 'success' });
        } else {
          uni.showToast({ title: '已补录，请手动选择', icon: 'none' });
        }
        this.quickAdd = { visible: false, name: '', balanceYuan: '' };
        this.picker.visible = false;
      } catch (e) { /* 拦截器已提示 */ }
    },
    async refreshLiabilities() {
      const liabilities = await listLiabilities(false);
      this.liabilities = ((liabilities && liabilities.records) || []).filter(l => l.settleFlag !== 1);
    },
    pickDate() {
      uni.showModal({ title: '提示', content: '当前默认今天，历史补录请先保存后在流水中编辑', showCancel: false });
    },
    tapKey(k) {
      if (k === 'del') {
        this.amountYuan = this.amountYuan.slice(0, -1);
        return;
      }
      const v = this.amountYuan;
      if (k === '.') {
        if (v.includes('.')) return;
        this.amountYuan = v === '' ? '0.' : v + '.';
        return;
      }
      if (v.includes('.') && v.split('.')[1].length >= 2) return;
      if (v === '0' && k !== '.') { this.amountYuan = k; return; }
      if (v.replace('.', '').length >= 10) return;
      this.amountYuan = v + k;
    },
    async save() {
      if (!this.canSave) return;
      const userStore = useUserStore();
      if (!userStore.isLoggedIn) {
        uni.showToast({ title: '请先在「我的」登录', icon: 'none' });
        uni.switchTab({ url: '/pages/mine/index' });
        return;
      }
      const cent = yuanToCent(this.amountYuan);

      // 还款资金校验：扣款账户余额不足时，引导补录资金来源（说明钱从哪来）
      if (this.form.type === 'repayment' && this.form.accountId) {
        const acc = this.assets.find(x => x.id === this.form.accountId);
        if (acc && acc.balance < cent) {
          const that = this;
          uni.showModal({
            title: '账户余额不足',
            content: `「${acc.name}」当前余额 ¥${centToAmount(acc.balance)}，不足以覆盖此笔还款 ¥${centToAmount(cent)}。\n若钱实际已到位（如刚到账的奖金未记账），请先补录一笔该账户的收入说明资金来源，再保存此笔还款。`,
            confirmText: '去补录收入',
            cancelText: '仍要保存',
            success: (r) => {
              if (r.confirm) {
                // 切到收入类型，预选同一账户，保留金额便于直接补录
                that.form.type = 'income';
                that.kbVisible = true;
                uni.showToast({ title: '已切换为收入，请补录资金来源', icon: 'none' });
              } else {
                that.doSave(cent);
              }
            }
          });
          return;
        }
      }
      this.doSave(cent);
    },
    async doSave(cent) {
      const data = {
        type: this.form.type,
        amount: cent,
        categoryId: this.form.categoryId,
        accountId: this.form.accountId,
        liabilityId: this.form.liabilityId,
        targetAccountId: this.form.targetAccountId,
        description: this.form.description || null,
        merchant: this.form.merchant || null,
        voucherUrl: this.form.voucherUrl || null,
        transactionDate: this.form.transactionDate
      };
      try {
        const res = await createTransaction(data);
        // 站内预算提醒（≥80%/≥100%，后端每阈值每月仅推一次）
        const alertMsg = res && res.budgetAlert;
        if (alertMsg) {
          uni.showModal({ title: '预算提醒', content: alertMsg, showCancel: false });
        } else {
          uni.showToast({ title: '已保存', icon: 'success' });
        }
        // 重置金额/备注/凭证/分类，保留账户便于连续记账
        this.amountYuan = '';
        this.form.description = '';
        this.form.merchant = '';
        this.form.voucherUrl = '';
        this.form.categoryId = null;
        // 刷新账户余额（还款校验用最新值）
        const assets = await listAssets(false).catch(() => null);
        if (assets && assets.records) this.assets = assets.records;
      } catch (e) { /* 拦截器已提示 */ }
    },
    // ---------------- 凭证截图 ----------------
    chooseVoucher() {
      const userStore = useUserStore();
      if (!userStore.isLoggedIn) {
        this.promptLogin();
        return;
      }
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.uploadVoucherFile(res.tempFilePaths[0]);
        }
      });
    },
    async uploadVoucherFile(filePath) {
      uni.showLoading({ title: '上传中', mask: true });
      try {
        const url = await uploadVoucher(filePath);
        this.form.voucherUrl = url;
        uni.hideLoading();
      } catch (e) {
        uni.hideLoading();
      }
    },
    previewVoucher() {
      if (!this.form.voucherUrl) return;
      uni.previewImage({ urls: [this.form.voucherUrl] });
    },
    removeVoucher() {
      this.form.voucherUrl = '';
    }
  }
};
</script>

<style scoped>
.page { display: flex; flex-direction: column; height: 100vh; background: #f5f6f8; }

.type-bar {
  display: flex; background: #6a4fd4; padding: 0 12rpx; padding-top: calc(90rpx + env(safe-area-inset-top));
}
.type-item {
  flex: 1; text-align: center; color: rgba(255,255,255,0.65); font-size: 28rpx;
  padding: 20rpx 0 24rpx; position: relative;
}
.type-item.active { color: #fff; font-weight: 600; }
.type-item.active::after {
  content: ''; position: absolute; left: 50%; transform: translateX(-50%);
  bottom: 8rpx; width: 40rpx; height: 6rpx; border-radius: 3rpx; background: #fff;
}

/* 类型语义提示 */
.type-hint {
  background: #f0ebfb; color: #6a4fd4; font-size: 22rpx;
  padding: 10rpx 32rpx;
}

.body { flex: 1; overflow-y: auto; }
.amount-area {
  display: flex; align-items: baseline; padding: 40rpx 40rpx 30rpx; background: #6a4fd4;
}
.currency { color: #fff; font-size: 44rpx; font-weight: 600; margin-right: 16rpx; }
.amount-input { flex: 1; color: #fff; font-size: 64rpx; font-weight: 700; }
.kb-open-btn { color: rgba(255,255,255,0.75); font-size: 26rpx; }

.pick-row {
  display: flex; align-items: center; background: #fff;
  padding: 22rpx 32rpx; border-bottom: 1rpx solid #f5f5f7;
}
.pick-label { width: 200rpx; color: #666; font-size: 28rpx; }
.pick-value { flex: 1; text-align: right; color: #333; font-size: 28rpx; }
.pick-value.muted { color: #999; }
.pick-input { flex: 1; text-align: right; font-size: 28rpx; }

/* 凭证截图行 */
.voucher-row {
  display: flex; align-items: center; background: #fff;
  padding: 20rpx 32rpx; border-bottom: 1rpx solid #f5f5f7;
}
.voucher-area { flex: 1; display: flex; justify-content: flex-end; position: relative; }
.voucher-thumb {
  width: 120rpx; height: 120rpx; border-radius: 12rpx; border: 1rpx solid #eee;
}
.voucher-remove {
  position: absolute; top: -14rpx; right: -14rpx; width: 36rpx; height: 36rpx;
  border-radius: 18rpx; background: #e74c3c; color: #fff; font-size: 24rpx;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}
.voucher-add {
  width: 120rpx; height: 120rpx; border-radius: 12rpx; background: #f5f6f8;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.voucher-add-icon { font-size: 36rpx; }
.voucher-add-text { font-size: 20rpx; color: #999; margin-top: 4rpx; }

/* 数字键盘（可收起） */
.keyboard { background: #fff; padding-bottom: env(safe-area-inset-bottom); }
.kb-handle { display: flex; justify-content: center; padding: 8rpx 0; }
.kb-handle-text { font-size: 22rpx; color: #999; padding: 4rpx 24rpx; }
.kb-row { display: flex; }
.kb-key {
  flex: 1; height: 88rpx; display: flex; align-items: center; justify-content: center;
  font-size: 34rpx; font-weight: 500; border-top: 1rpx solid #f0f0f5; color: #333;
}
.kb-key:active { background: #efecfb; }
.kb-del { font-size: 38rpx; }

.save-bar { background: #fff; padding: 12rpx 32rpx calc(12rpx + env(safe-area-inset-bottom)); }
.btn-save {
  background: #6a4fd4; color: #fff; border-radius: 40rpx; height: 76rpx;
  line-height: 76rpx; text-align: center; font-size: 30rpx; font-weight: 600;
}
.btn-save.disabled { opacity: 0.4; }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 99; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 28rpx 32rpx; max-height: 82vh; display: flex; flex-direction: column; }
.sheet-title { font-size: 30rpx; font-weight: 600; text-align: center; margin-bottom: 16rpx; }
.picker-list { max-height: 70vh; }
.picker-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 16rpx; border-bottom: 1rpx solid #f5f5f7; font-size: 28rpx;
}
.picker-item.active { color: #6a4fd4; font-weight: 600; }
.picker-sub { color: #999; font-size: 24rpx; }
.picker-empty {
  padding: 60rpx 0; text-align: center; color: #6a4fd4; font-size: 26rpx;
}

/* 快速补录借款项目 */
.quick-add-entry {
  margin: 0 0 16rpx; padding: 18rpx; text-align: center;
  border: 1rpx dashed #6a4fd4; border-radius: 12rpx; color: #6a4fd4; font-size: 26rpx;
}
.quick-add-form {
  background: #f9f7fe; border-radius: 16rpx; padding: 20rpx; margin-bottom: 16rpx;
}
.qa-title { font-size: 26rpx; font-weight: 600; color: #6a4fd4; margin-bottom: 12rpx; }
.qa-row { display: flex; align-items: center; padding: 12rpx 0; }
.qa-label { width: 190rpx; font-size: 25rpx; color: #666; }
.qa-input { flex: 1; font-size: 27rpx; text-align: right; }
.qa-actions { display: flex; gap: 16rpx; margin-top: 12rpx; }
.qa-btn {
  flex: 1; text-align: center; border-radius: 10rpx; padding: 14rpx 0; font-size: 26rpx;
}
.qa-btn.cancel { background: #eee; color: #666; }
.qa-btn.ok { background: #6a4fd4; color: #fff; }
.qa-btn.ok.disabled { opacity: 0.4; }
</style>
