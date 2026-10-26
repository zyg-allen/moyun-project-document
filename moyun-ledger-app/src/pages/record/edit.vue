<template>
  <view class="page" :style="themeVars">
    <NavBar title="编辑流水" />

    <view class="card" v-if="txn">
      <!-- 类型：只读展示。改类型会改变整笔的资金语义（收/支/转账/借贷），
           故此处不支持切换，需要时删除后重记 -->
      <view class="info-row">
        <text class="info-label">类型</text>
        <text class="info-value type-badge" :class="'type-' + txn.type">{{ typeText(txn.type) }}</text>
      </view>

      <!-- 分类：与「记一笔」口径一致（收支类必填，否则报表分类 TOP 会缺数据） -->
      <view class="info-row" v-if="txn.type !== 'adjust'" @tap="pickCategory">
        <text class="info-label">分类</text>
        <text class="info-value pick" :class="{ placeholder: !selectedCategoryName }">
          {{ selectedCategoryName || '选择分类' }} ▾
        </text>
      </view>

      <!-- 二级分类：选填，不选即归入大类（统计为「大类（未细分）」） -->
      <view class="info-row" v-if="txn.type !== 'adjust' && subCategories.length" @tap="pickSubCategory">
        <text class="info-label">二级</text>
        <text class="info-value pick" :class="{ placeholder: !selectedSubName }">
          {{ selectedSubName || '不细分（归入大类）' }} ▾
        </text>
      </view>

      <!-- 金额 -->
      <view class="info-row">
        <text class="info-label">金额(元)</text>
        <input v-model="amountYuan" type="digit" class="info-input amount" @input="onAmountInput" />
      </view>

      <!-- 账户：这笔钱从哪个账户扣 / 进哪个账户。
           原先缺这一行 —— 用户看不到钱从哪扣，与定时记账执行日志口径同样缺失 -->
      <view class="info-row" @tap="pickAsset">
        <text class="info-label">{{ accountLabel }}</text>
        <text class="info-value pick" :class="{ placeholder: !selectedAssetName }">
          {{ selectedAssetText || '选择账户' }} ▾
        </text>
      </view>

      <!-- 转入账户（仅转账） -->
      <view class="info-row" v-if="needTarget" @tap="pickTarget">
        <text class="info-label">转入账户</text>
        <text class="info-value pick" :class="{ placeholder: !selectedTargetName }">
          {{ selectedTargetText || '选择账户' }} ▾
        </text>
      </view>

      <!-- 借款 / 还款项目 -->
      <view class="info-row" v-if="needLiability" @tap="pickLiability">
        <text class="info-label">{{ txn.type === 'repayment' ? '还款至' : '借款自' }}</text>
        <text class="info-value pick" :class="{ placeholder: !selectedLiabilityName }">
          {{ selectedLiabilityText || '选择借款项目' }} ▾
        </text>
      </view>

      <!-- 日期 -->
      <view class="info-row">
        <text class="info-label">日期</text>
        <CalendarPicker class="fill-picker" :value="txn.transactionDate" @change="(e) => txn.transactionDate = e.detail.value">
          <view class="info-value pick">{{ txn.transactionDate }} ▾</view>
        </CalendarPicker>
      </view>

      <!-- 时间 -->
      <view class="info-row">
        <text class="info-label">时间</text>
        <view class="fill-picker" @tap="openTime">
          <view class="info-value pick">{{ txn.transactionTime || '00:00' }} ▾</view>
        </view>
      </view>

      <!-- 商户 -->
      <view class="info-row">
        <text class="info-label">商户</text>
        <input v-model="txn.merchant" placeholder="选填" class="info-input" />
      </view>

      <!-- 备注（为什么） -->
      <view class="info-row">
        <text class="info-label">备注</text>
        <input v-model="txn.description" placeholder="选填" class="info-input" />
      </view>

      <!-- 凭证 -->
      <view class="info-row">
        <text class="info-label">凭证</text>
        <view class="voucher-area">
          <image v-if="txn.voucherUrl" :src="txn.voucherUrl" mode="aspectFill" class="voucher-thumb"
                 @tap="previewVoucher" />
          <view v-else class="voucher-none" @tap="chooseVoucher">未设置，点击上传</view>
          <view v-if="txn.voucherUrl" class="voucher-ops">
            <text class="voucher-op" @tap="chooseVoucher">更换</text>
            <text class="voucher-op danger" @tap="removeVoucher">移除</text>
          </view>
        </view>
      </view>

      <view class="tip">提示：修改将冲正原记录并按新参数重放，余额以事务方式联动更新</view>
      <view class="btn-primary" :class="{ disabled: submitting || !canSave }"
            @tap="!submitting && save()">保存修改</view>
      <view class="btn-danger" @tap="remove">删除此流水</view>
    </view>

    <!-- 选择弹层：与「记一笔」同一套 KeyboardAwareSheet（键盘安全 + 内容可滚动） -->
    <KeyboardAwareSheet v-model="pickerVisibleModel" :title="picker.title" :max-height-vh="78">
      <scroll-view scroll-y class="picker-list">
        <view v-if="!picker.items.length" class="picker-empty">{{ picker.empty || '暂无数据' }}</view>
        <view v-for="item in picker.items" :key="item.id" class="picker-item"
              :class="{ active: picker.selectedId === item.id }" @tap="confirmPick(item)">
          <text>{{ item.name }}</text>
          <text class="picker-sub" v-if="item.sub">{{ item.sub }}</text>
        </view>
      </scroll-view>
      <view class="picker-cancel" @tap="picker.visible = false">取消</view>
    </KeyboardAwareSheet>
    <PickerWheel
      :visible="timeSheet"
      title="选择时间"
      :columns="[hours, minutes]"
      :value="timeDraft"
      @close="timeSheet = false"
      @confirm="onTimeConfirm"
    />

  </view>
</template>

<script>
import CalendarPicker from '@/components/CalendarPicker/CalendarPicker.vue';
import PickerWheel from '@/components/PickerWheel/PickerWheel.vue';
import KeyboardAwareSheet from '@/components/KeyboardAwareSheet/KeyboardAwareSheet.vue';
import submitLock from '@/mixins/submitLock';
import {
  getTransaction, updateTransaction, deleteTransaction, uploadVoucher,
  listAssets, listLiabilities, listCategories
} from '@/api/ledger';
import { toFixedYuan, toNum, typeText, applyAmountInput, formatAmount, formatAbsAmount } from '@/utils/money';
import { useThemeStore } from '@/stores/theme';

export default {
  components: { PickerWheel, CalendarPicker, KeyboardAwareSheet },
  mixins: [submitLock],
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    /** KeyboardAwareSheet 用 v-model，而 picker.visible 是对象字段，这里做个代理 */
    pickerVisibleModel: {
      get() { return this.picker.visible; },
      set(v) { this.picker.visible = v; }
    },
    needTarget() { return !!this.txn && this.txn.type === 'transfer'; },
    needLiability() { return !!this.txn && ['repayment', 'borrow'].includes(this.txn.type); },
    /** 账户行的文案随类型变化，说清「钱从哪扣/进哪」 */
    accountLabel() {
      const map = {
        expense: '支出账户', income: '收入账户', transfer: '转出账户',
        repayment: '扣款账户', borrow: '入账账户(选填)', adjust: '校准账户'
      };
      return (this.txn && map[this.txn.type]) || '账户';
    },
    selectedCategoryName() { return this.nameOf(this.categories, this.txn && this.txn.categoryId); },
    /** 当前一级分类下的二级（编辑时选填，不选即归入大类） */
    subCategories() {
      if (!this.txn || !this.txn.categoryId) return [];
      return this.categories.filter(c => String(c.parentId) === String(this.txn.categoryId));
    },
    selectedSubName() { return this.nameOf(this.categories, this.txn && this.txn.subCategoryId); },
    selectedAssetName() { return this.nameOf(this.assets, this.txn && this.txn.accountId); },
    selectedTargetName() { return this.nameOf(this.assets, this.txn && this.txn.targetAccountId); },
    selectedLiabilityName() { return this.nameOf(this.liabilities, this.txn && this.txn.liabilityId); },
    /** 账户带余额后缀，让「从哪扣、扣完还剩多少」一目了然 */
    selectedAssetText() { return this.withBalance(this.assets, this.txn && this.txn.accountId, '余额'); },
    selectedTargetText() { return this.withBalance(this.assets, this.txn && this.txn.targetAccountId, '余额'); },
    selectedLiabilityText() { return this.withBalance(this.liabilities, this.txn && this.txn.liabilityId, '欠款'); },
    /** 与「记一笔」的 canSave 同口径，避免保存出后端拒绝的非法组合 */
    canSave() {
      if (!this.txn) return false;
      const amountNum = toNum(this.amountYuan);
      if (this.txn.type === 'adjust') return amountNum !== 0 && !!this.txn.accountId;
      if (amountNum <= 0) return false;
      if (this.needTarget) return !!this.txn.accountId && !!this.txn.targetAccountId;
      if (this.needLiability) return this.txn.type === 'borrow' || !!this.txn.accountId;
      if (this.txn.type === 'expense' || this.txn.type === 'income') {
        return !!this.txn.accountId && !!this.txn.categoryId;
      }
      return !!this.txn.accountId;
    }
  },
  data() {
    return {
      timeSheet: false,
      timeDraft: [0, 0],
      hours: HOURS,
      minutes: MINUTES,
      id: null,
      txn: null,
      amountYuan: '',
      assets: [],
      liabilities: [],
      categories: [],
      picker: { visible: false, mode: '', title: '', items: [], selectedId: null, empty: '' }
    };
  },
  onLoad(options) {
    this.id = options.id;
    this.load();
  },

  /** 同其它页面：uni-app H5 每页各自新建 uni-page-body，进入本页需重新注入主题 */
  onShow() {
    useThemeStore().applyToPageBody();
  },
  methods: {
    openTime() {
      const parts = String((this.txn && this.txn.transactionTime) || '08:00').split(':');
      this.timeDraft = [Number(parts[0]) || 0, Number(parts[1]) || 0];
      this.timeSheet = true;
    },
    onTimeConfirm(idx) {
      this.onTimeChange({ detail: { value: pad2(idx[0]) + ':' + pad2(idx[1]) } });
      this.timeSheet = false;
    },
    /** 金额输入净化（见 utils/money.applyAmountInput） */
    onAmountInput(e) { applyAmountInput(e, this, 'amountYuan'); },
    typeText,
    /** 在选项数组里按 id 取名称 */
    nameOf(list, id) {
      const item = (list || []).find(x => x.id === id);
      return item ? item.name : '';
    },
    /** 「名称（余额 ¥x）」；找不到名称时回退到流水自带的中文名（选项还没加载完也能显示） */
    withBalance(list, id, label) {
      const item = (list || []).find(x => x.id === id);
      if (item) {
        const v = label === '欠款' ? formatAbsAmount(item.balance) : formatAmount(item.balance);
        return `${item.name}（${label} ${v} 元）`;
      }
      const fallback = this.fallbackName(id);
      return fallback || '';
    },
    /** 选项未加载完时的兜底：用详情接口已回填的 xxxName 字段 */
    fallbackName(id) {
      if (!this.txn || !id) return '';
      if (this.txn.accountId === id) return this.txn.accountName || '';
      if (this.txn.targetAccountId === id) return this.txn.targetAccountName || '';
      if (this.txn.liabilityId === id) return this.txn.liabilityName || '';
      if (this.txn.categoryId === id) return this.txn.categoryName || '';
      return '';
    },
    async load() {
      try {
        // 按 id 直查单笔（原先从「最近 50 条」分页里 find，翻到第 4 页以上会误报"不存在"）
        const res = await getTransaction(this.id);
        const txn = (res && res.data) || res;
        if (!txn || !txn.id) {
          uni.showToast({ title: '流水不存在或已删除', icon: 'none' });
          setTimeout(() => uni.navigateBack(), 1200);
          return;
        }
        this.txn = txn;
        this.amountYuan = toFixedYuan(txn.amount);
        // 回填交易时间：原先不传会导致保存时被服务端改写为"当前时刻"
        if (!this.txn.transactionTime) this.txn.transactionTime = '00:00';
        // 选项依赖类型（分类按类型过滤），故在拿到流水后再加载
        this.loadOptions();
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 账户/负债/分类选项：与「记一笔」同样的数据源与过滤口径 */
    async loadOptions() {
      try {
        const [assets, liabilities] = await Promise.all([listAssets(false), listLiabilities(false)]);
        this.assets = (assets && assets.records) || [];
        this.liabilities = ((liabilities && liabilities.records) || []).filter(l => l.settleFlag !== 1);
      } catch (e) { /* 拦截器已提示；名称仍有 fallbackName 兜底 */ }
      try {
        const res = await listCategories(this.txn ? this.txn.type : null);
        this.categories = (res && res.records) || [];
      } catch (e) { /* 忽略 */ }
    },
    /** 时间选择（HH:mm） */
    onTimeChange(e) { this.txn.transactionTime = e.detail.value; },

    // ---------------- 选项弹层 ----------------
    pickCategory() {
      this.openPicker('category', '选择分类',
        this.categories.map(c => ({ id: c.id, name: c.name })), this.txn.categoryId);
    },
    pickAsset() {
      this.openPicker('asset', '选择账户',
        this.assets.map(a => ({ id: a.id, name: a.name, sub: '余额 ' + formatAmount(a.balance) + ' 元' })),
        this.txn.accountId);
    },
    pickTarget() {
      this.openPicker('target', '选择转入账户',
        this.assets.filter(a => a.id !== this.txn.accountId)
          .map(a => ({ id: a.id, name: a.name, sub: '余额 ' + formatAmount(a.balance) + ' 元' })),
        this.txn.targetAccountId);
    },
    pickLiability() {
      this.openPicker('liability', '选择借款项目',
        this.liabilities.map(l => ({ id: l.id, name: l.name, sub: '欠款 ' + formatAbsAmount(l.balance) + ' 元' })),
        this.txn.liabilityId);
    },
    pickSubCategory() {
      const items = [{ id: null, name: '不细分（归入大类）' }]
        .concat(this.subCategories.map(c => ({ id: c.id, name: c.name })));
      this.openPicker('subcategory', '选择二级分类', items, this.txn.subCategoryId);
    },
    openPicker(mode, title, items, selectedId) {
      const empty = {
        asset: '暂无可用账户，可到「资产负债」页添加',
        target: '暂无可用账户，可到「资产负债」页添加',
        liability: '暂无借款项目，可到「资产负债」页补录',
        category: '该类型下暂无可选分类'
      }[mode] || '暂无数据';
      this.picker = { visible: true, mode, title, items, selectedId, empty };
    },
    confirmPick(item) {
      const mode = this.picker.mode;
      if (mode === 'category') {
        // 换一级分类后，原二级可能不属于新大类 → 必须清空
        if (String(this.txn.categoryId) !== String(item.id)) this.txn.subCategoryId = null;
        this.txn.categoryId = item.id;
      }
      if (mode === 'subcategory') this.txn.subCategoryId = item.id || null;
      if (mode === 'asset') this.txn.accountId = item.id;
      if (mode === 'target') this.txn.targetAccountId = item.id;
      if (mode === 'liability') this.txn.liabilityId = item.id;
      this.picker.visible = false;
    },

    async save() {
      return this.withSubmitLock(async () => {
              if (!this.canSave) {
                uni.showToast({ title: this.saveHint(), icon: 'none' });
                return;
              }
              const amountNum = toNum(this.amountYuan);
              try {
                await updateTransaction(this.id, {
                  type: this.txn.type,
                  amount: amountNum,
                  categoryId: this.txn.categoryId,
                  subCategoryId: this.txn.subCategoryId || null,
                  accountId: this.txn.accountId,
                  liabilityId: this.txn.liabilityId,
                  targetAccountId: this.txn.targetAccountId,
                  description: this.txn.description || null,
                  merchant: this.txn.merchant || null,
                  voucherUrl: this.txn.voucherUrl || '',
                  transactionDate: this.txn.transactionDate,
                  // 必须回传：服务端 transactionTime 为空时会取当前时间，导致原时间被静默改写
                  transactionTime: this.txn.transactionTime || null
                });
                uni.showToast({ title: '已修改', icon: 'success' });
                setTimeout(() => uni.navigateBack(), 800);
              } catch (e) { /* 拦截器已提示 */ }
      });
    },
    /** 保存被拦下时说清缺哪一项 */
    saveHint() {
      const amountNum = toNum(this.amountYuan);
      if (this.txn.type === 'adjust') {
        if (!this.txn.accountId) return '请选择' + this.accountLabel;
        return '校准金额不能为 0';
      }
      if (amountNum <= 0) return '请输入金额';
      if (this.needTarget) return this.txn.accountId ? '请选择转入账户' : '请选择' + this.accountLabel;
      if (this.needLiability && this.txn.type !== 'borrow' && !this.txn.accountId) return '请选择' + this.accountLabel;
      if ((this.txn.type === 'expense' || this.txn.type === 'income') && !this.txn.categoryId) return '请选择分类';
      return '请选择' + this.accountLabel;
    },

    // ---------------- 凭证 ----------------
    chooseVoucher() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          uni.showLoading({ title: '上传中', mask: true });
          uploadVoucher(res.tempFilePaths[0]).then((url) => {
            uni.hideLoading();
            this.txn.voucherUrl = url;
          }).catch(() => uni.hideLoading());
        }
      });
    },
    previewVoucher() {
      if (!this.txn.voucherUrl) return;
      uni.previewImage({ urls: [this.txn.voucherUrl] });
    },
    removeVoucher() {
      this.txn.voucherUrl = '';
    },
    remove() {
      uni.showModal({
        title: '删除确认',
        content: '将冲正余额并归档该流水，是否删除？',
        success: async (r) => {
          if (r.confirm) {
            await deleteTransaction(this.id);
            uni.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => uni.navigateBack(), 800);
          }
        }
      });
    }
  }
};
</script>

<style scoped>
/* 与全站约定完全一致：页面根**不设任何内边距**。
   左右留白由 .card 的 margin: 0 24rpx（App.vue 全局 .card）承担，
   上下由 .card 的 margin-bottom 与列表自身间距承担。
   这里原先是 padding: 24rpx，与 .card 的左右 margin 叠加成 48rpx（比其它页窄一圈），
   上边还额外多出 24rpx，使 NavBar 与卡片之间凭空多一段 —— 与 budget / categories /
   record-list 等所有带 NavBar 的页面都不一致。故整条规则去掉。 */
.card { background: var(--card); border-radius: 20rpx; padding: 24rpx 32rpx; }
.info-row {
  display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid var(--border);
}
.info-row:last-child { border-bottom: none; }
.info-label { width: 180rpx; color: var(--text-sub); font-size: 28rpx; flex-shrink: 0; }
.info-value { flex: 1; text-align: right; font-size: 28rpx; color: var(--text-main); }
.info-input { flex: 1; text-align: right; font-size: 28rpx; color: var(--text-main); }
.info-input.amount { font-size: 40rpx; font-weight: 700; }
/* 可选中的值：与普通只读值区分，提示「可点」 */
.info-value.pick { color: var(--primary-text); }
.info-value.placeholder { color: var(--text-muted); }
/* 类型徽标 */
.type-badge { font-weight: 600; }
.type-badge.type-income { color: var(--income); }
.type-badge.type-expense { color: var(--expense); }
.type-badge.type-transfer,
.type-badge.type-repayment,
.type-badge.type-borrow,
.type-badge.type-adjust { color: var(--primary-text); }

.tip { font-size: 22rpx; color: var(--text-sub); padding: 24rpx 0; line-height: 1.6; }
.voucher-area { flex: 1; display: flex; justify-content: flex-end; align-items: center; }
.voucher-thumb { width: 110rpx; height: 110rpx; border-radius: 12rpx; border: 1rpx solid var(--border); }
.voucher-none { font-size: 24rpx; color: var(--primary-text); }
.voucher-ops { display: flex; flex-direction: column; margin-left: 16rpx; gap: 8rpx; }
.voucher-op { font-size: 22rpx; color: var(--primary-text); line-height: 1.4; }
.voucher-op.danger { color: var(--expense); }
.btn-primary { margin-top: 24rpx; }
.btn-danger {
  margin-top: 24rpx; text-align: center; color: var(--expense); font-size: 28rpx;
  border: 1rpx solid var(--expense); border-radius: 44rpx; height: 80rpx; line-height: 80rpx;
}

/* 选项弹层内部（KeyboardAwareSheet 提供遮罩/面板/关闭） */
.picker-list { max-height: 60vh; }
.picker-empty { text-align: center; color: var(--text-muted); font-size: 26rpx; padding: 60rpx 0; }
.picker-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 28rpx 8rpx; border-bottom: 1rpx solid var(--border);
  font-size: 28rpx; color: var(--text-main);
}
.picker-item.active { color: var(--primary-text); font-weight: 600; }
.picker-sub { font-size: 22rpx; color: var(--text-sub); }
.picker-cancel {
  margin-top: 20rpx; text-align: center; padding: 18rpx;
  background: var(--surface); border-radius: 36rpx; font-size: 26rpx; color: var(--text-sub);
}
</style>
