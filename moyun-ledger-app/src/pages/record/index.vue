<template>
  <view class="page" :style="themeVars">
    <!-- 顶部：类型Tab + 语义提示 -->
    <view class="type-bar">
      <view class="type-bar-inner">
        <view v-for="t in visibleTypes" :key="t.key" class="type-item" :class="{ active: form.type === t.key }" @tap="switchType(t.key)">
          {{ t.label }}
        </view>
      </view>
    </view>
    <view class="type-hint" v-if="typeHint">{{ typeHint }}</view>

    <!-- 分类宫格（按类型过滤，常用优先） -->
    <view class="cat-section">
      <view class="cat-section-title">
        <text>选择分类</text>
        <view class="cat-title-right" @tap="catCollapsed = !catCollapsed">
          <text class="cat-count">{{ selectedCategoryName || '选填' }}</text>
          <text class="cat-toggle">{{ catCollapsed ? '展开 ▾' : '收起 ▴' }}</text>
        </view>
      </view>
      <scroll-view v-show="!catCollapsed" scroll-y class="cat-grid-wrap">
        <view class="cat-grid">
          <view v-for="c in sortedCategories" :key="c.id" class="cat-cell" @tap="pickCategoryCell(c)">
            <view class="cat-icon" :class="{ selected: form.categoryId === c.id }"
                  :style="form.categoryId === c.id ? '' : 'background:' + (c.color || '#BDC3C7')">
              <text>{{ iconOf(c.icon) }}</text>
            </view>
            <text class="cat-name" :class="{ selected: form.categoryId === c.id }">{{ c.name }}</text>
          </view>
          <view v-if="!sortedCategories.length" class="cat-empty">
            {{ userStore.isLoggedIn ? '暂无分类，可在「我的-分类管理」添加' : '登录后可选择分类' }}
          </view>
        </view>
      </scroll-view>
      <!-- 二级分类（预留：一级带子分类时横滑展示） -->
      <scroll-view v-if="subCategories.length" scroll-x class="sub-cat-bar">
        <view v-for="s in subCategories" :key="s.id" class="sub-cat-tag" :class="{ active: form.subCategoryId === s.id }"
              @tap="form.subCategoryId = form.subCategoryId === s.id ? null : s.id">
          {{ s.name }}
        </view>
      </scroll-view>
    </view>

    <!-- 备注行（突出） -->
    <view class="note-card">
      <text class="note-label">备注</text>
      <input v-model="form.description" placeholder="点击补充说明" class="note-input" @input="onDescInput" />
    </view>

    <!-- 金额区（突出） -->
    <view class="amount-area" @tap="kbVisible = true">
      <view class="amount-left">
        <text class="amount-label">金额</text>
        <text class="amount-date">{{ form.transactionDate }} {{ form.transactionTime }}</text>
      </view>
      <view class="amount-right">
        <text class="currency">¥</text>
        <text class="amount-text">{{ amountYuan || '0.00' }}</text>
        <text v-if="!kbVisible" class="kb-open-btn">⌨</text>
      </view>
    </view>

    <!-- 账户/负债选择（次级卡片） -->
    <view class="opt-card">
      <!-- 日期选择（独立选择器，支持历史补录） -->
      <view class="pick-row">
        <text class="pick-label">日期</text>
        <picker mode="date" :value="form.transactionDate" :end="todayStr" @change="onDateChange" class="pick-picker">
          <view class="pick-value">{{ form.transactionDate }} ▾</view>
        </picker>
        <view class="today-btn" @tap="setToday">今天</view>
      </view>
      <!-- 时间必填（独立选择器，默认当前时间） -->
      <view class="pick-row">
        <text class="pick-label">时间</text>
        <picker mode="time" :value="form.transactionTime" @change="onTimeChange" class="pick-picker">
          <view class="pick-value">{{ form.transactionTime }} ▾</view>
        </picker>
      </view>
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
        <view class="pick-value-area">
          <text class="pick-value">{{ selectedLiabilityName || '选择借款项目' }} ▾</text>
          <text v-if="form.liabilityId" class="pick-clear" @tap.stop="clearLiability">✕ 清除</text>
        </view>
      </view>
      <view class="pick-row" v-if="form.type === 'expense'">
        <text class="pick-label">商户</text>
        <input v-model="form.merchant" placeholder="选填" class="pick-input" />
      </view>
      <!-- 凭证截图 -->
      <view class="voucher-row">
        <text class="pick-label">凭证</text>
        <view class="voucher-area">
          <image v-if="form.voucherUrl" :src="form.voucherUrl" mode="aspectFill" class="voucher-thumb"
                 @tap.stop="previewVoucher" />
          <view v-else class="voucher-add" @tap="chooseVoucher">
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
          <view class="qa-tip">仅填写历史欠款（此前已借未还的部分）；本次要记的借款金额勿填在这里，保存记账时会自动累加到该项目，填了会双倍计入。</view>
          <view class="qa-row">
            <text class="qa-label">名称</text>
            <input v-model="quickAdd.name" placeholder="如：向朋友A借款" class="qa-input" />
          </view>
          <view class="qa-row">
            <text class="qa-label">历史欠款(元)</text>
            <input v-model="quickAdd.balanceYuan" type="digit" placeholder="0.00（没有留空）" class="qa-input" />
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
        <view class="picker-cancel" @tap="picker.visible = false">取消</view>
      </view>
    </view>
  </view>
</template>

<script>
import { createTransaction, listAssets, listLiabilities, listCategories, createLiability, uploadVoucher } from '@/api/ledger';
import { yuanToCent, centToAmount, centToAbsAmount, toNum } from '@/utils/money';
import { useUserStore } from '@/stores/user';
import { useThemeStore } from '@/stores/theme';

const TYPE_DEFS = [
  { key: 'expense', label: '支出' },
  { key: 'income', label: '收入' },
  { key: 'transfer', label: '转账' },
  { key: 'repayment', label: '还款' },
  { key: 'borrow', label: '借款' },
  { key: 'adjust', label: '校准' }
];

const TYPE_HINTS = {
  expense: '支出 = 从所选资产账户扣钱，计入当月预算统计',
  income: '收入 = 钱进入所选资产账户，计入月度收支统计',
  transfer: '转账 = 资产账户间互转：A 减、B 加，总资产不变',
  repayment: '还款 = 资产账户出钱，欠款对应减少；余额不足会提示补录资金来源',
  borrow: '借款 = 欠款增加，钱进入所选资产账户（可不选账户，仅记录欠款）',
  adjust: '校准 = 在账户当前余额上累加差额（正数调增、负数调减），用于补记遗漏造成的余额偏差，不计入收支统计'
};

/** 分类 icon 标识 → emoji（彩色圆底内） */
const ICON_MAP = {
  food: '🍜', transport: '🚌', shopping: '🛍️', home: '🏠', entertainment: '🎮',
  medical: '💊', education: '📚', phone: '📱', daily: '🧻', gift: '🎁',
  pet: '🐾', travel: '✈️', 'house-loan': '🏦', 'car-loan': '🚗', repayment: '💳',
  interest: '📈', other: '🔖', salary: '💰', bonus: '🎉', parttime: '💼',
  invest: '📊', redpacket: '🧧', refund: '↩️', 'borrow-in': '🤝', secondhand: '♻️',
  // 转账
  'transfer-self': '🔄', 'transfer-friend': '👥', 'transfer-proxy': '🔀', 'transfer-refund': '↩️', 'transfer-other': '🔖',
  // 还款
  'repay-card': '💳', 'repay-loan': '🏦', 'repay-personal': '🤝', 'repay-interest': '📈', 'repay-other': '🔖',
  // 借款
  'borrow-card': '💳', 'borrow-online': '🌐', 'borrow-bank': '🏦', 'borrow-installment': '📅', 'borrow-personal': '🤝', 'borrow-other': '🔖',
  // 校准
  'adjust-balance': '⚖️', 'adjust-fee': '💸', 'adjust-fx': '💱', 'adjust-other': '🔖'
};

const TYPE_NAMES = { expense: '支出', income: '收入', transfer: '转账', repayment: '还款', borrow: '借款', adjust: '校准' };

export default {
  data() {
    const today = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    return {
      types: TYPE_DEFS,
      amountYuan: '',
      catCollapsed: false,
      assets: [],
      liabilities: [],
      categories: [],
      kbVisible: true,
      quickAdd: { visible: false, name: '', balanceYuan: '' },
      /** 用户是否手动改过备注（未改过时自动跟随类型/分类/日期刷新默认前缀） */
      descEdited: false,
      form: {
        type: 'expense', accountId: null, targetAccountId: null, liabilityId: null,
        categoryId: null, subCategoryId: null, description: '', merchant: '', voucherUrl: '',
        transactionDate: `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`,
        transactionTime: `${pad(today.getHours())}:${pad(today.getMinutes())}`
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
    themeVars() { return useThemeStore().themeVars; },
    userStore() { return useUserStore(); },
    todayStr() {
      const t = new Date();
      const pad = (n) => String(n).padStart(2, '0');
      return `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`;
    },
    visibleTypes() { return this.types; },
    typeHint() { return TYPE_HINTS[this.form.type] || ''; },
    needTarget() { return this.form.type === 'transfer'; },
    needLiability() { return ['repayment', 'borrow'].includes(this.form.type); },
    accountLabel() {
      return { expense: '支出账户', income: '收入账户', transfer: '转出账户', repayment: '扣款账户', borrow: '入账账户(选填)', adjust: '校准账户' }[this.form.type] || '账户';
    },
    /** 按当前类型过滤分类：每种交易类型只显示对应分类 */
    filteredCategories() {
      const t = this.form.type;
      return this.categories.filter(c => c.type === t && !c.parentId);
    },
    /** 常用优先：使用次数降序，同频按 sortOrder */
    sortedCategories() {
      return [...this.filteredCategories].sort((a, b) =>
        toNum(b.usedCount) - toNum(a.usedCount) || toNum(a.sortOrder) - toNum(b.sortOrder)
      );
    },
    /** 一级分类的二级子分类（选中一级后出现） */
    subCategories() {
      if (!this.form.categoryId) return [];
      return this.categories.filter(c => c.parentId === this.form.categoryId);
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
      return l ? `${l.name}（欠 ${centToAbsAmount(l.balance)}）` : '';
    },
    selectedCategoryName() {
      const c = this.categories.find(x => x.id === this.form.categoryId);
      return c ? c.name : '';
    },
    quickAddOk() {
      // 历史欠款可空（=0）：借款页补录时通常只建项目，欠款由本次记账累加
      return !!(this.quickAdd.name && this.quickAdd.name.trim()) && yuanToCent(this.quickAdd.balanceYuan || '0') >= 0;
    },
    canSave() {
      const cent = yuanToCent(this.amountYuan);
      if (this.form.type === 'adjust') return cent !== 0 && !!this.form.accountId;
      if (cent <= 0) return false;
      if (this.needTarget) return !!this.form.accountId && !!this.form.targetAccountId;
      if (this.needLiability) return  (this.form.type === 'borrow' || !!this.form.accountId);
      return !!this.form.accountId;
    }
  },
  onShow() {
    useThemeStore().restore();
    if (!this.userStore.isLoggedIn) {
      this.assets = [];
      this.liabilities = [];
      this.categories = [];
      this.promptLogin();
      return;
    }
    this.loadOptions();
  },
  methods: {
    iconOf(icon) { return ICON_MAP[icon] || '🏷️'; },
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
        if (e && e.code === 401) this.promptLogin();
      }
    },
    async loadCategories() {
      try {
        const res = await listCategories();
        this.categories = (res && res.records) || [];
      } catch (e) { /* 忽略 */ }
    },
    switchType(key) {
      this.form.type = key;
      this.form.categoryId = null;
      this.form.subCategoryId = null;
      this.refreshRemark();
    },
    pickCategoryCell(c) {
      this.form.categoryId = this.form.categoryId === c.id ? null : c.id;
      this.form.subCategoryId = null;
      this.refreshRemark();
    },
    pickSubCategory(s) {
      this.form.subCategoryId = this.form.subCategoryId === s.id ? null : s.id;
      this.refreshRemark();
    },
    /** 备注默认前缀：yyyyMMdd 类型-具体类目（大类型名称—小类型名称），用户在此基础上补充说明 */
    buildRemarkPrefix() {
      const dateStr = (this.form.transactionDate || '').replace(/-/g, '');
      const typeName = TYPE_NAMES[this.form.type] || this.form.type;
      const parent = this.categories.find(c => c.id === this.form.categoryId);
      const sub = this.categories.find(c => c.id === this.form.subCategoryId);
      let cat = '';
      if (parent && sub) {
        cat = `-${sub.name}（${parent.name}—${sub.name}）`;
      } else if (parent) {
        cat = `-${parent.name}（${parent.name}）`;
      }
      return `${dateStr} ${typeName}${cat}`;
    },
    /** 未手动编辑过备注时，自动刷新默认前缀（类型/分类/日期变化联动） */
    refreshRemark() {
      if (this.descEdited) return;
      this.form.description = this.buildRemarkPrefix();
    },
    onDescInput() {
      this.descEdited = true;
    },
    onDateChange(e) {
      this.form.transactionDate = e.detail.value;
      this.refreshRemark();
    },
    onTimeChange(e) {
      this.form.transactionTime = e.detail.value;
    },
    /** 快速按钮：今天 = 系统当前日期 + 当前时间 */
    setToday() {
      const t = new Date();
      const pad = (n) => String(n).padStart(2, '0');
      this.form.transactionDate = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`;
      this.form.transactionTime = `${pad(t.getHours())}:${pad(t.getMinutes())}`;
      this.refreshRemark();
      uni.showToast({ title: '已选今天', icon: 'none' });
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
        id: l.id, name: l.name, sub: '欠款 ¥' + centToAbsAmount(l.balance)
      })), this.form.liabilityId);
    },
    openPicker(mode, title, items, selectedId) {
      this.quickAdd.visible = false;
      const emptyMap = {
        asset: '暂无可用账户，点击去「资产」页添加',
        target: '暂无可用账户，点击去「资产」页添加',
        liability: '暂无借款项目，可在上方快速补录'
      };
      this.picker = { visible: true, mode, title, items, selectedId, empty: emptyMap[mode] || '暂无数据' };
    },
    pickerEmptyTap() {
      const mode = this.picker.mode;
      this.picker.visible = false;
      uni.switchTab({ url: '/pages/portfolio/index' });
      uni.showToast({ title: mode === 'liability' ? '可在「负债」Tab 补录' : '可在「资产」Tab 补录', icon: 'none' });
    },
    confirmPick(item) {
      const mode = this.picker.mode;
      if (mode === 'asset') this.form.accountId = item.id;
      if (mode === 'target') this.form.targetAccountId = item.id;
      if (mode === 'liability') this.form.liabilityId = item.id;
      this.picker.visible = false;
    },
    clearLiability() {
      this.form.liabilityId = null;
      uni.showToast({ title: '已清除，可重新选择', icon: 'none' });
    },
    async saveQuickAdd() {
      if (!this.quickAddOk) return;
      try {
        const cent = yuanToCent(this.quickAdd.balanceYuan || '0');
        const created = await createLiability({
          name: this.quickAdd.name.trim(),
          type: 'personal',
          // 历史欠款为 0 时传 null：不生成"初始欠款"流水，欠款完全由本次借款流水累加（防双倍）
          initialBalance: cent > 0 ? cent : null,
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
      if (!this.userStore.isLoggedIn) {
        uni.showToast({ title: '请先在「我的」登录', icon: 'none' });
        uni.switchTab({ url: '/pages/mine/index' });
        return;
      }
      const cent = yuanToCent(this.amountYuan);
      const typeName = { expense: '支出', income: '收入', transfer: '转账', repayment: '还款', borrow: '借款', adjust: '校准' }[this.form.type];

      // 类型最终确认（防 tab 误触）：校准/借款等特殊类型必须人工确认
      if (this.form.type === 'adjust' || this.form.type === 'borrow' || this.form.type === 'repayment') {
        const that = this;
        const confirmed = await new Promise((resolve) => {
          uni.showModal({
            title: '确认保存' + typeName,
            content: this.confirmText(cent),
            confirmText: '保存',
            cancelText: '再检查',
            success: (r) => resolve(!!r.confirm),
            fail: () => resolve(false)
          });
        });
        if (!confirmed) return;
      }

      // 还款资金校验：扣款账户余额不足时，引导补录资金来源（说明钱从哪来）
      if (this.form.type === 'repayment' && this.form.accountId) {
        const acc = this.assets.find(x => x.id === this.form.accountId);
        if (acc && toNum(acc.balance) < cent) {
          const that = this;
          uni.showModal({
            title: '账户余额不足',
            content: `「${acc.name}」当前余额 ¥${centToAmount(acc.balance)}，不足以覆盖此笔还款 ¥${centToAmount(cent)}。\n若钱实际已到位（如刚到账的奖金未记账），请先补录一笔该账户的收入说明资金来源，再保存此笔还款。`,
            confirmText: '去补录收入',
            cancelText: '仍要保存',
            success: (r) => {
              if (r.confirm) {
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
    /** 保存确认文案：按类型说清这笔账会怎么动账 */
    confirmText(cent) {
      const amt = centToAmount(Math.abs(cent));
      const acc = this.selectedAssetName || '未选账户';
      switch (this.form.type) {
        case 'adjust':
          return `将在「${acc}」当前余额上${cent > 0 ? '加' : '减'} ¥${amt}（不是设为该值），确认无误？`;
        case 'borrow': {
          const li = this.selectedLiabilityName || '自动新建借款项目';
          return `记借款 ¥${amt}：欠款增加（${li}）${this.form.accountId ? `，资金进入「${acc}」` : '，不入资产账户'}。确认？`;
        }
        case 'repayment': {
          const li = this.selectedLiabilityName || '';
          return `记还款 ¥${amt}：「${acc}」扣款，${li || '指定借款项目'}欠款减少。确认？`;
        }
        default:
          return '';
      }
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
        const alertMsg = res && res.budgetAlert;
        if (alertMsg) {
          uni.showModal({ title: '预算提醒', content: alertMsg, showCancel: false });
        } else {
          uni.showToast({ title: '已保存', icon: 'success' });
        }
        this.amountYuan = '';
        this.form.merchant = '';
        this.form.voucherUrl = '';
        this.form.categoryId = null;
        this.form.subCategoryId = null;
        this.form.liabilityId = null;
        // 保存后重置备注为新的默认前缀（日期已可能变化）
        this.descEdited = false;
        const t = new Date();
        const pad = (n) => String(n).padStart(2, '0');
        this.form.transactionDate = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`;
        this.form.transactionTime = `${pad(t.getHours())}:${pad(t.getMinutes())}`;
        this.refreshRemark();
        const assets = await listAssets(false).catch(() => null);
        if (assets && assets.records) this.assets = assets.records;
      } catch (e) { /* 拦截器已提示 */ }
    },
    // ---------------- 凭证截图 ----------------
    chooseVoucher() {
      if (!this.userStore.isLoggedIn) {
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
.page { display: flex; flex-direction: column; min-height: 100vh; background: #f5f6f8; padding-bottom: env(safe-area-inset-bottom); }

/* 顶部类型Tab（白底胶囊） */
.type-bar { background: #fff; padding: calc(20rpx + env(safe-area-inset-top)) 24rpx 16rpx; }
.type-bar-inner { display: flex; background: #f5f6f8; border-radius: 44rpx; padding: 8rpx; }
.type-item {
  flex: 1; text-align: center; color: #666; font-size: 28rpx;
  padding: 14rpx 0; border-radius: 36rpx;
}
.type-item.active { background: var(--primary); color: #fff; font-weight: 600; }

/* 类型语义提示 */
.type-hint { background: var(--primary-soft); color: var(--primary-strong); font-size: 22rpx; padding: 10rpx 32rpx; }

/* 分类宫格 */
.cat-section { background: #fff; margin: 20rpx 24rpx; border-radius: 20rpx; padding: 24rpx 12rpx 8rpx; }
.cat-section-title { display: flex; justify-content: space-between; align-items: center; padding: 0 20rpx 16rpx; font-size: 28rpx; font-weight: 600; }
.cat-title-right { display: flex; align-items: center; }
.cat-count { font-size: 22rpx; color: var(--primary-strong); font-weight: 400; margin-right: 16rpx; }
.cat-toggle { font-size: 22rpx; color: #999; padding: 4rpx 12rpx; background: #f5f5f7; border-radius: 20rpx; }
.cat-grid-wrap { max-height: 560rpx; }
.cat-grid { display: flex; flex-wrap: wrap; }
.cat-cell { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 16rpx 0 20rpx; }
.cat-icon {
  width: 88rpx; height: 88rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 40rpx;
  box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.08);
}
.cat-icon.selected { background: var(--primary) !important; transform: scale(1.06); }
.cat-name { font-size: 24rpx; color: #666; margin-top: 10rpx; }
.cat-name.selected { color: var(--primary-strong); font-weight: 600; }
.cat-empty { width: 100%; text-align: center; color: #bbb; font-size: 26rpx; padding: 40rpx 0; }

/* 二级分类横滑 */
.sub-cat-bar { white-space: nowrap; padding: 8rpx 20rpx 16rpx; border-top: 1rpx solid #f5f5f7; }
.sub-cat-tag {
  display: inline-block; font-size: 24rpx; color: #666;
  background: #f5f6f8; border-radius: 28rpx; padding: 8rpx 28rpx; margin-right: 16rpx;
}
.sub-cat-tag.active { background: var(--primary); color: #fff; }

/* 备注行（突出） */
.note-card {
  display: flex; align-items: center; background: #fff;
  margin: 0 24rpx 20rpx; border-radius: 20rpx; padding: 28rpx 32rpx;
}
.note-label { font-size: 28rpx; font-weight: 600; margin-right: 24rpx; }
.note-input { flex: 1; font-size: 30rpx; }

/* 金额区（突出，主色底） */
.amount-area {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--primary); padding: 36rpx 40rpx; margin: 0 24rpx 20rpx;
  border-radius: 20rpx; color: #fff;
}
.amount-left { display: flex; flex-direction: column; }
.amount-label { font-size: 26rpx; opacity: 0.85; }
.amount-date { font-size: 24rpx; opacity: 0.7; margin-top: 8rpx; }
.amount-right { display: flex; align-items: baseline; }
.currency { font-size: 40rpx; font-weight: 600; margin-right: 12rpx; }
.amount-text { font-size: 64rpx; font-weight: 700; letter-spacing: 2rpx; }
.kb-open-btn { font-size: 32rpx; margin-left: 16rpx; opacity: 0.8; }

/* 次级选择卡片 */
.opt-card { background: #fff; margin: 0 24rpx 20rpx; border-radius: 20rpx; padding: 8rpx 32rpx; }
.pick-row {
  display: flex; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.pick-row:last-child { border-bottom: none; }
.pick-label { width: 200rpx; color: #666; font-size: 28rpx; }
.pick-value-area { flex: 1; display: flex; align-items: center; justify-content: flex-end; }
.pick-clear {
  margin-left: 16rpx; padding: 4rpx 16rpx; border-radius: 20rpx;
  background: #f0f0f5; color: #e74c3c; font-size: 22rpx;
}
.pick-value { flex: 1; text-align: right; color: #333; font-size: 28rpx; }
.pick-input { flex: 1; text-align: right; font-size: 28rpx; }
.pick-picker { flex: 1; display: flex; justify-content: flex-end; }
.today-btn {
  margin-left: 16rpx; padding: 6rpx 24rpx; border-radius: 24rpx;
  background: var(--primary-soft); color: var(--primary-strong); font-size: 24rpx;
}

/* 凭证截图行 */
.voucher-row {
  display: flex; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.voucher-row:last-child { border-bottom: none; }
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
  flex: 1; height: 96rpx; display: flex; align-items: center; justify-content: center;
  font-size: 36rpx; font-weight: 500; border-top: 1rpx solid #f0f0f5; color: #333;
}
.kb-key:active { background: var(--primary-soft); }
.kb-del { font-size: 40rpx; }

.save-bar { background: #fff; padding: 12rpx 32rpx calc(12rpx + env(safe-area-inset-bottom)); }
.btn-save {
  background: var(--primary); color: #fff; border-radius: 44rpx; height: 84rpx;
  line-height: 84rpx; text-align: center; font-size: 32rpx; font-weight: 600;
  box-shadow: 0 8rpx 20rpx var(--primary-shadow);
}
.btn-save.disabled { opacity: 0.4; box-shadow: none; }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 99; display: flex; align-items: flex-end; }
.sheet { width: 100%; background: #fff; border-radius: 32rpx 32rpx 0 0; padding: 28rpx 32rpx; max-height: 82vh; display: flex; flex-direction: column; }
.sheet-title { font-size: 30rpx; font-weight: 600; text-align: center; margin-bottom: 16rpx; }
.picker-list { max-height: 70vh; }
.picker-cancel {
  margin-top: 20rpx; text-align: center; color: #999; font-size: 26rpx;
  height: 72rpx; line-height: 72rpx; border-top: 1rpx solid #f5f5f7;
}
.picker-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 16rpx; border-bottom: 1rpx solid #f5f5f7; font-size: 28rpx;
}
.picker-item.active { color: var(--primary-strong); font-weight: 600; }
.picker-sub { color: #999; font-size: 24rpx; }
.picker-empty {
  padding: 60rpx 0; text-align: center; color: var(--primary-strong); font-size: 26rpx;
}

/* 快速补录借款项目 */
.quick-add-entry {
  margin: 0 0 16rpx; padding: 18rpx; text-align: center;
  border: 1rpx dashed var(--primary); border-radius: 12rpx; color: var(--primary-strong); font-size: 26rpx;
}
.quick-add-form {
  background: var(--primary-soft); border-radius: 16rpx; padding: 20rpx; margin-bottom: 16rpx;
}
.qa-title { font-size: 26rpx; font-weight: 600; color: var(--primary-strong); margin-bottom: 12rpx; }
.qa-tip {
  font-size: 22rpx; color: #e67e22; background: #fdf3e7;
  border-radius: 8rpx; padding: 12rpx 16rpx; margin-bottom: 8rpx; line-height: 1.6;
}
.qa-row { display: flex; align-items: center; padding: 12rpx 0; }
.qa-label { width: 190rpx; font-size: 25rpx; color: #666; }
.qa-input { flex: 1; font-size: 27rpx; text-align: right; }
.qa-actions { display: flex; gap: 16rpx; margin-top: 12rpx; }
.qa-btn {
  flex: 1; text-align: center; border-radius: 10rpx; padding: 14rpx 0; font-size: 26rpx;
}
.qa-btn.cancel { background: #eee; color: #666; }
.qa-btn.ok { background: var(--primary); color: #fff; }
.qa-btn.ok.disabled { opacity: 0.4; }
</style>
