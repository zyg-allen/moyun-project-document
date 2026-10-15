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
          <text class="cat-count">{{ selectedCategoryName || '请选择' }}</text>
          <text class="cat-toggle">{{ catCollapsed ? '展开 ▾' : '收起 ▴' }}</text>
        </view>
      </view>
      <scroll-view v-show="!catCollapsed" scroll-y class="cat-grid-wrap">
        <view class="cat-grid">
          <view v-for="c in sortedCategories" :key="c.id" class="cat-cell" @tap="pickCategoryCell(c)"
                @longpress="onCategoryLongPress(c)">
            <view class="cat-icon" :class="{ selected: form.categoryId === c.id }"
                  :style="form.categoryId === c.id ? '' : 'background:' + softColor(c.color)">
              <text>{{ iconOf(c.icon) }}</text>
            </view>
            <text class="cat-name" :class="{ selected: form.categoryId === c.id }">{{ c.name }}</text>
            <!-- 自定义分类：右上角 × 删除（不再显示「自」角标），@tap.stop 避免触发选分类 -->
            <text v-if="c.isSystem === 0" class="cat-del-x" @tap.stop="onCategoryLongPress(c)">×</text>
          </view>
          <!-- 在当前大类下新增自定义分类（仅自己可见，可长按自定义分类删除） -->
          <view v-if="userStore.isLoggedIn" class="cat-cell" @tap="addCustomCategory">
            <view class="cat-icon cat-add-icon">＋</view>
            <text class="cat-name">添加</text>
          </view>
          <view v-if="!sortedCategories.length" class="cat-empty">
            {{ userStore.isLoggedIn ? '点击「添加」在当前大类下新增分类' : '登录后可选择分类' }}
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

    <!-- 金额区：原生数字输入（系统输入法自带、位置跟随，无需自绘键盘）
         排版：金额（CNY）： 数值 元 —— 币种写进标签括号，单位后置于数值之后；
         不用强调色底，与其它卡片一致的卡面，仅以左侧强调色竖条点出主次 -->
    <view class="amount-area">
      <text class="amount-label">金额（CNY）：</text>
      <view class="amount-right">
        <input
          v-model="amountYuan"
          class="amount-input"
          type="digit"
          placeholder="0.00"
          :cursor-spacing="24"
          adjust-position
          @input="onAmountInput"
        />
        <text class="amount-suffix">元</text>
      </view>
    </view>

    <!-- 账户/负债选择（次级卡片）
         顺序：账户组 → 日期/时间 → 商户/凭证
         账户紧邻金额：记账最自然的顺序是「多少钱 → 从/到哪个账户」，
         而日期/时间默认即当下，通常无需改动，放其后不打断主流程 -->
    <view class="opt-card">
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
      <!-- 日期 + 时间（合并同一行；日期支持历史补录，时间默认当前） -->
      <view class="pick-row pick-row-datetime">
        <text class="pick-label">日期时间</text>
        <CalendarPicker :value="form.transactionDate" :end="todayStr" @change="onDateChange" class="pick-picker">
          <view class="pick-value">{{ form.transactionDate }} ▾</view>
        </CalendarPicker>
        <picker mode="time" :value="form.transactionTime" @change="onTimeChange" class="pick-picker pick-picker-time">
          <view class="pick-value">{{ form.transactionTime }} ▾</view>
        </picker>
        <view class="today-btn" @tap="setToday">今天</view>
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


    <!-- 保存 -->
    <view class="save-bar">
      <view class="btn-save" :class="{ disabled: !canSave }" @tap="save">保存</view>
    </view>

    <!-- 选择弹层（键盘安全：H5 抬升 + 小程序 adjust-position + 内容可滚动） -->
    <KeyboardAwareSheet v-model="pickerVisibleModel" :title="picker.title" :max-height-vh="78">

        <!-- 负债快速补录表单 -->
        <view v-if="picker.mode === 'liability' && !quickAdd.visible" class="quick-add-entry" @tap="quickAdd.visible = true">
          ＋ 没有要选的借款项目？快速补录一个
        </view>
        <view v-if="picker.mode === 'liability' && quickAdd.visible" class="quick-add-form">
          <view class="qa-title">快速补录借款项目</view>
          <view class="qa-tip">仅填写历史欠款（此前已借未还的部分）；本次要记的借款金额勿填在这里，保存记账时会自动累加到该项目，填了会双倍计入。</view>
          <view class="qa-row">
            <text class="qa-label">名称</text>
            <input v-model="quickAdd.name" placeholder="如：向朋友A借款" class="qa-input"
              adjust-position
              :cursor-spacing="24"
             />
          </view>
          <view class="qa-row">
            <text class="qa-label">历史欠款(元)</text>
            <input v-model="quickAdd.balanceYuan" type="digit" placeholder="0.00（没有留空）" class="qa-input"
              adjust-position
              :cursor-spacing="24"
             />
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
    </KeyboardAwareSheet>
  </view>
</template>

<script>
import CalendarPicker from '@/components/CalendarPicker/CalendarPicker.vue';
import keyboardAware from '@/mixins/keyboardAware';
import KeyboardAwareSheet from '@/components/KeyboardAwareSheet/KeyboardAwareSheet.vue';
import { createTransaction, listAssets, listLiabilities, listCategories, createCategory, deleteCategory, createLiability, uploadVoucher } from '@/api/ledger';
import { toNum, formatAmount, formatAbsAmount } from '@/utils/money';
import { useUserStore } from '@/stores/user';
import { useThemeStore } from '@/stores/theme';
import { categoryIcon, softColor as toSoftColor } from '@/utils/categoryIcon';

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

/** 分类 icon 标识 → emoji 已收敛到 @/utils/categoryIcon 统一维护 */

const TYPE_NAMES = { expense: '支出', income: '收入', transfer: '转账', repayment: '还款', borrow: '借款', adjust: '校准' };

export default {
  mixins: [keyboardAware],
  components: { CalendarPicker,  KeyboardAwareSheet },  // 键盘安全弹层
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
      quickAdd: { visible: false, name: '', balanceYuan: '' },
      /** 用户是否手动改过备注（未改过时自动跟随类型/分类/日期刷新默认前缀） */
      descEdited: false,
      form: {
        type: 'expense', accountId: null, targetAccountId: null, liabilityId: null,
        categoryId: null, subCategoryId: null, description: '', merchant: '', voucherUrl: '',
        transactionDate: `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`,
        transactionTime: `${pad(today.getHours())}:${pad(today.getMinutes())}`
      },
      picker: { visible: false, mode: '', title: '', items: [], selectedId: null },
      /** 账户/负债选项加载失败（与"确实没有账户"区分，避免误去重复建账户） */
      optionsError: false,
      /** 保存进行中（防重复提交锁） */
      saving: false
    };
  },
  computed: {
    /** 弹层显隐代理（KeyboardAwareSheet 用 v-model，picker.visible 是对象字段） */
    pickerVisibleModel: {
      get() { return this.picker.visible; },
      set(v) { this.picker.visible = v; }
    },
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
      return l ? `${l.name}（欠 ${formatAbsAmount(l.balance)}）` : '';
    },
    selectedCategoryName() {
      const c = this.categories.find(x => x.id === this.form.categoryId);
      return c ? c.name : '';
    },
    quickAddOk() {
      // 历史欠款可空（=0）：借款页补录时通常只建项目，欠款由本次记账累加
      return !!(this.quickAdd.name && this.quickAdd.name.trim()) && toNum(this.quickAdd.balanceYuan || '0') >= 0;
    },
    canSave() {
      const amountNum = toNum(this.amountYuan);
      if (this.form.type === 'adjust') return amountNum !== 0 && !!this.form.accountId;
      if (amountNum <= 0) return false;
      if (this.needTarget) return !!this.form.accountId && !!this.form.targetAccountId;
      if (this.needLiability) return  (this.form.type === 'borrow' || !!this.form.accountId);
      // 收支类必须选分类：否则报表「分类 TOP」按 categoryId 聚合时会被跳过，
      // 导致「收入/支出分类 TOP」缺数据（此前收入可无分类保存 → category_id=NULL）
      if (this.form.type === 'expense' || this.form.type === 'income') {
        return !!this.form.accountId && !!this.form.categoryId;
      }
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
    iconOf(icon) { return categoryIcon(icon); },
    /** 分类颜色 → 浅色底（与分类管理页一致，避免 emoji 撞色） */
    softColor(color) { return toSoftColor(color); },
    promptLogin() {
      uni.showModal({
        title: '提示',
        content: '登录后才能选择账户和负债',
        confirmText: '去登录',
        success: (r) => {
          if (r.confirm) uni.navigateTo({ url: '/pages/mine/login/index' });
        }
      });
    },
    async loadOptions() {
      try {
        const [assets, liabilities] = await Promise.all([listAssets(false), listLiabilities(false)]);
        this.assets = (assets && assets.records) || [];
        this.liabilities = ((liabilities && liabilities.records) || []).filter(l => l.settleFlag !== 1);
        this.optionsError = false;
        this.loadCategories();
      } catch (e) {
        // 失败标记：否则 picker 提示"暂无账户"，用户会真去建重复账户
        // （401 已由 utils/request.js 的 handleUnauthorized 统一登出并跳登录页，
        //   这里不再重复弹窗，否则会叠在登录页之上）
        this.optionsError = true;
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
      // 切换类型后分类列表整体换批且已清空选择 → 自动展开，便于直接点选新类型的分类
      this.catCollapsed = false;
      this.refreshRemark();
    },
    pickCategoryCell(c) {
      const selecting = this.form.categoryId !== c.id;
      this.form.categoryId = selecting ? c.id : null;
      this.form.subCategoryId = null;
      this.refreshRemark();
      // 选中分类后自动收起宫格：把屏幕让给金额/备注等后续输入（再次点击同一分类=取消选中，保持展开）
      if (selecting) this.catCollapsed = true;
    },
    pickSubCategory(s) {
      this.form.subCategoryId = this.form.subCategoryId === s.id ? null : s.id;
      this.refreshRemark();
    },
    /** 在当前大类下新增自定义分类（仅自己可见，创建后自动选中） */
    addCustomCategory() {
      if (!this.userStore.isLoggedIn) { this.promptLogin(); return; }
      const typeName = TYPE_NAMES[this.form.type] || this.form.type;
      uni.showModal({
        title: `新增${typeName}分类`,
        editable: true,
        placeholderText: '分类名称（仅自己可见）',
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) { uni.showToast({ title: '请输入分类名称', icon: 'none' }); return; }
          try {
            const created = await createCategory({ name, type: this.form.type });
            await this.loadCategories();
            const id = created && created.id;
            if (id) {
              this.form.categoryId = id;
              // 新增即选中：与「点选分类」保持一致，自动收起宫格
              this.catCollapsed = true;
            }
            this.refreshRemark();
            uni.showToast({ title: '已添加并选中', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 长按删除自己创建的自定义分类（绑定流水的分类后端拒绝删除） */
    onCategoryLongPress(c) {
      if (c.isSystem !== 0) return;
      uni.showModal({
        title: '删除自定义分类',
        content: `删除「${c.name}」？已绑定流水的分类不能删除`,
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteCategory(c.id);
            if (this.form.categoryId === c.id) {
              this.form.categoryId = null;
              // 选中的分类被删 → 展开宫格便于重新选择（否则收起状态下还得先点「展开」）
              this.catCollapsed = false;
              this.refreshRemark();
            }
            await this.loadCategories();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示（含绑定流水笔数） */ }
        }
      });
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
        id: a.id, name: a.name, sub: '余额 ¥' + formatAmount(a.balance)
      })), this.form.accountId);
    },
    pickTarget() {
      this.openPicker('target', '选择转入账户', this.assets.filter(a => a.id !== this.form.accountId)
        .map(a => ({ id: a.id, name: a.name, sub: '余额 ¥' + formatAmount(a.balance) })), this.form.targetAccountId);
    },
    pickLiability() {
      this.openPicker('liability', '选择借款项目', this.liabilities.map(l => ({
        id: l.id, name: l.name, sub: '欠款 ¥' + formatAbsAmount(l.balance)
      })), this.form.liabilityId);
    },
    openPicker(mode, title, items, selectedId) {
      this.quickAdd.visible = false;
      const emptyMap = {
        asset: '暂无可用账户，点击去「资产」页添加',
        target: '暂无可用账户，点击去「资产」页添加',
        liability: '暂无借款项目，可在上方快速补录'
      };
      const emptyText = this.optionsError
        ? '账户/负债加载失败，点击重试'   // 失败态：给"重试"而非"去添加"
        : (emptyMap[mode] || '暂无数据');
      this.picker = { visible: true, mode, title, items, selectedId, empty: emptyText };
    },
    pickerEmptyTap() {
      // 选项加载失败时点击应重试，而不是把用户带去「资产」页重复建账户
      if (this.optionsError) { this.loadOptions(); this.picker.visible = false; uni.showToast({ title: '正在重新加载', icon: 'none' }); return; }
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
        const amountNum = toNum(this.quickAdd.balanceYuan || '0');
        const created = await createLiability({
          name: this.quickAdd.name.trim(),
          type: 'personal',
          // 历史欠款为 0 时传 null：不生成"初始欠款"流水，欠款完全由本次借款流水累加（防双倍）
          initialBalance: amountNum > 0 ? amountNum : null,
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
    /**
     * 金额输入（原生 input type=digit）：
     * 只保留数字与一个小数点，最多两位小数，避免用户输入非法金额。
     */
    onAmountInput(e) {
      let v = String((e && e.detail && e.detail.value) || '').replace(/[^0-9.]/g, '');
      const firstDot = v.indexOf('.');
      if (firstDot >= 0) {
        // 只保留第一个小数点
        v = v.slice(0, firstDot + 1) + v.slice(firstDot + 1).replace(/\./g, '');
        // 最多两位小数
        const intPart = v.slice(0, firstDot);
        const decPart = v.slice(firstDot + 1, firstDot + 3);
        v = intPart + '.' + decPart;
      }
      // 去掉多余前导 0（保留 "0." 形态）
      if (/^0[0-9]/.test(v)) v = v.replace(/^0+/, '');
      this.amountYuan = v;
    },
    async save() {
      // 入口即上锁（而非等到真正提交）：本方法在多个分支会先 await 确认弹窗，
      // 原实现锁在提交方法里才置位 → 连点「保存」会叠出两个确认弹窗，
      // 分别确认即产生两笔（clientUuid 每次新生成，后端按 uuid 去重拦不住）
      if (this.saving) return;
      if (!this.canSave) {
        // 给出缺失原因，避免按钮灰色但用户不知道缺什么
        const amountNum = toNum(this.amountYuan);
        if (amountNum <= 0) { uni.showToast({ title: '请输入金额', icon: 'none' }); return; }
        if (!this.form.accountId) { uni.showToast({ title: '请选择账户', icon: 'none' }); return; }
        if ((this.form.type === 'expense' || this.form.type === 'income') && !this.form.categoryId) {
          uni.showToast({ title: '请选择分类', icon: 'none' }); return;
        }
        if (this.needTarget && !this.form.targetAccountId) { uni.showToast({ title: '请选择转入账户', icon: 'none' }); return; }
        return;
      }
      if (!this.userStore.isLoggedIn) {
        uni.showToast({ title: '请先登录后再操作', icon: 'none' });
        uni.switchTab({ url: '/pages/mine/index' });
        return;
      }
      const amountNum = toNum(this.amountYuan);
      const typeName = { expense: '支出', income: '收入', transfer: '转账', repayment: '还款', borrow: '借款', adjust: '校准' }[this.form.type];
      this.saving = true;
      try {
        await this.saveInner(amountNum, typeName);
      } finally {
        // 覆盖所有 early return 分支（取消确认、余额不足引导、类型切换等）
        // 注意：实际提交方法 saveTransaction 已不再自行加锁，锁统一由本方法收口
        this.saving = false;
      }
    },
    /**
     * 保存的实际流程（拆分出来只为让 save() 能在入口统一上锁）。
     * 所有 early return 都由 save() 的 finally 负责复位 saving。
     */
    async saveInner(amountNum, typeName) {
      // 类型最终确认（防 tab 误触）：校准/借款等特殊类型必须人工确认
      if (this.form.type === 'adjust' || this.form.type === 'borrow' || this.form.type === 'repayment') {
        const confirmed = await new Promise((resolve) => {
          uni.showModal({
            title: '确认保存' + typeName,
            content: this.confirmText(amountNum),
            confirmText: '保存',
            cancelText: '再检查',
            success: (r) => resolve(!!r.confirm),
            fail: () => resolve(false)
          });
        });
        if (!confirmed) return;
      }

      // 支出/转账资金校验（账目可信度）：扣款账户余额不足时阻断保存，
      // 必须先记一笔收入或转账说明资金来源——否则支出凭空出现，钱从哪来说不清楚
      if ((this.form.type === 'expense' || this.form.type === 'transfer') && this.form.accountId) {
        const acc = this.assets.find(x => x.id === this.form.accountId);
        if (acc && toNum(acc.balance) < amountNum) {
          const label = this.form.type === 'expense' ? '支出' : '转出';
          uni.showModal({
            title: '账户余额不足',
            content: `「${acc.name}」当前余额 ¥${formatAmount(acc.balance)}，不足以覆盖此笔${label} ¥${formatAmount(amountNum)}。\n支出的钱必须有来源：请先记一笔该账户的收入（如工资、奖金），或从余额充足的账户转账过来，再记此笔${label}。`,
            confirmText: '去记收入',
            cancelText: '去转账',
            success: (r) => {
              // confirm→记收入补足资金来源；cancel→从其他账户转入
              // switchType 保留所选账户，金额保留，切换后补选分类即可保存
              this.switchType(r.confirm ? 'income' : 'transfer');
              uni.showToast({ title: r.confirm ? '已切换为收入，请记录资金来源' : '已切换为转账，请选择转出账户', icon: 'none' });
            }
          });
          return;
        }
      }

      // 还款资金校验：扣款账户余额不足时，引导补录资金来源（说明钱从哪来）
      if (this.form.type === 'repayment' && this.form.accountId) {
        const acc = this.assets.find(x => x.id === this.form.accountId);
        if (acc && toNum(acc.balance) < amountNum) {
          const that = this;
          uni.showModal({
            title: '账户余额不足',
            content: `「${acc.name}」当前余额 ¥${formatAmount(acc.balance)}，不足以覆盖此笔还款 ¥${formatAmount(amountNum)}。\n若钱实际已到位（如刚到账的奖金未记账），请先补录一笔该账户的收入说明资金来源，再保存此笔还款。`,
            confirmText: '去补录收入',
            cancelText: '仍要保存',
            success: (r) => {
              if (r.confirm) {
                that.form.type = 'income';

                uni.showToast({ title: '已切换为收入，请补录资金来源', icon: 'none' });
              } else {
                that.saveTransaction(amountNum);
              }
            }
          });
          return;
        }
      }
      this.saveTransaction(amountNum);
    },
    /** 保存确认文案：按类型说清这笔账会怎么动账 */
    confirmText(amountNum) {
      const amt = formatAmount(Math.abs(amountNum));
      const acc = this.selectedAssetName || '未选账户';
      switch (this.form.type) {
        case 'adjust':
          return `将在「${acc}」当前余额上${amountNum > 0 ? '加' : '减'} ¥${amt}（不是设为该值），确认无误？`;
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
    /**
     * 实际提交（**不加锁**）。
     *
     * 锁已由 `save()` 在入口统一持有并覆盖整个异步流程 —— 若此处再加
     * `if (this.saving) return` 会因锁已被持有而永远直接返回（保存失效）。
     */
    async saveTransaction(amountNum) {
      // 客户端幂等键：后端按 clientUuid 查重（DB 唯一索引兜底），双击/网络重试不会重复入账
      const clientUuid = 'tx-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10);
      const data = {
        type: this.form.type,
        amount: amountNum,
        categoryId: this.form.categoryId,
        accountId: this.form.accountId,
        liabilityId: this.form.liabilityId,
        targetAccountId: this.form.targetAccountId,
        description: this.form.description || null,
        merchant: this.form.merchant || null,
        voucherUrl: this.form.voucherUrl || null,
        transactionDate: this.form.transactionDate,
        transactionTime: this.form.transactionTime,
        clientUuid
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
        // 分类已清空 → 展开宫格，下笔可直接点选（避免收起状态下还要先点一次「展开」）
        this.catCollapsed = false;
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
.page { display: flex; flex-direction: column; min-height: 100vh; background: var(--surface); padding-bottom: env(safe-area-inset-bottom); }

/* 顶部类型Tab（白底胶囊） */
.type-bar { background: var(--card); padding: calc(20rpx + var(--status-bar-height)) 24rpx 16rpx; }
.type-bar-inner { display: flex; background: var(--surface); border-radius: 44rpx; padding: 8rpx; }
.type-item {
  flex: 1; text-align: center; color: var(--text-sub); font-size: 28rpx;
  padding: 14rpx 0; border-radius: 36rpx;
}
.type-item.active { background: var(--primary); color: var(--on-primary); font-weight: 600; }

/* 类型语义提示 */
.type-hint { background: var(--primary-soft); color: var(--primary-text); font-size: 22rpx; padding: 10rpx 32rpx; }

/* 分类宫格 */
.cat-section { background: var(--card); margin: 20rpx 24rpx; border-radius: 20rpx; padding: 24rpx 12rpx 8rpx; }
.cat-section-title { display: flex; justify-content: space-between; align-items: center; padding: 0 20rpx 16rpx; font-size: 28rpx; font-weight: 600; }
.cat-title-right { display: flex; align-items: center; }
.cat-count { font-size: 22rpx; color: var(--primary-text); font-weight: 400; margin-right: 16rpx; }
.cat-toggle { font-size: 22rpx; color: var(--text-sub); padding: 4rpx 12rpx; background: var(--surface); border-radius: 20rpx; }
.cat-grid-wrap { max-height: 560rpx; }
.cat-grid { display: flex; flex-wrap: wrap; }
.cat-cell { position: relative; width: 25%; display: flex; flex-direction: column; align-items: center; padding: 12rpx 0; }
.cat-icon {
  width: 72rpx; height: 72rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 34rpx;
  box-shadow: 0 4rpx 10rpx rgba(0, 0, 0, 0.08);
}
.cat-icon.selected { background: var(--primary) !important; transform: scale(1.06); }
.cat-name { font-size: 22rpx; color: var(--text-sub); margin-top: 8rpx; }
.cat-name.selected { color: var(--primary-text); font-weight: 600; }
.cat-empty { width: 100%; text-align: center; color: var(--text-muted); font-size: 26rpx; padding: 40rpx 0; }
/* 添加自定义分类入口 + 自定义角标 */
.cat-add-icon {
  background: var(--surface); color: var(--primary-text); font-size: 40rpx;
  border: 1rpx dashed var(--border-strong); box-shadow: none;
}
/* 自定义分类右上角删除 ×：热区 88rpx，与分类点击互不干扰 */
.cat-del-x {
  position: absolute; top: 0; right: 6rpx;
  min-width: 64rpx; min-height: 64rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 32rpx; line-height: 1; color: var(--text-muted);
  border-radius: 50%;
}
.cat-del-x:active { background: var(--expense-bg); color: var(--expense); }

/* 二级分类横滑 */
.sub-cat-bar { white-space: nowrap; padding: 8rpx 20rpx 16rpx; border-top: 1rpx solid var(--border); }
.sub-cat-tag {
  display: inline-block; font-size: 24rpx; color: var(--text-sub);
  background: var(--surface); border-radius: 28rpx; padding: 8rpx 28rpx; margin-right: 16rpx;
}
.sub-cat-tag.active { background: var(--primary); color: var(--on-primary); }

/* 备注行（突出） */
.note-card {
  display: flex; align-items: center; background: var(--card);
  margin: 0 24rpx 20rpx; border-radius: 20rpx; padding: 28rpx 32rpx;
}
.note-label { font-size: 28rpx; font-weight: 600; margin-right: 24rpx; }
.note-input { flex: 1; font-size: 30rpx; }

/* 金额区
   普通卡面（与 note-card / opt-card 一致，不用强调色底，避免大色块喧宾夺主）；
   左侧 8rpx 强调色竖条作点缀 —— 与资产负债汇总卡同一视觉语言，
   既点出主次，又不引入整块底色。 */
.amount-area {
  display: flex; align-items: center;
  background: var(--card);
  padding: 30rpx 32rpx 30rpx 40rpx; margin: 0 24rpx 20rpx;
  border-radius: 20rpx;
  border-left: 8rpx solid var(--primary-strong);
}
.amount-label { font-size: 28rpx; color: var(--text-sub); flex-shrink: 0; }
/* 数值占满剩余宽度并右对齐，单位「元」紧随数值之后 */
.amount-right { display: flex; align-items: baseline; flex: 1; min-width: 0; justify-content: flex-end; }
.amount-input {
  flex: 1; min-width: 0; text-align: right;
  font-size: 56rpx; font-weight: 700; color: var(--text-main);
}
.amount-suffix { font-size: 28rpx; color: var(--text-sub); margin-left: 8rpx; flex-shrink: 0; }

/* 次级选择卡片 */
.opt-card { background: var(--card); margin: 0 24rpx 20rpx; border-radius: 20rpx; padding: 8rpx 32rpx; }
.pick-row {
  display: flex; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid var(--border);
}
.pick-row:last-child { border-bottom: none; }
.pick-label { width: 200rpx; color: var(--text-sub); font-size: 28rpx; }
.pick-value-area { flex: 1; display: flex; align-items: center; justify-content: flex-end; }
.pick-clear {
  margin-left: 16rpx; padding: 4rpx 16rpx; border-radius: 20rpx;
  background: var(--surface); color: var(--expense); font-size: 22rpx;
}
.pick-value { flex: 1; text-align: right; color: var(--text-main); font-size: 28rpx; }
.pick-input { flex: 1; text-align: right; font-size: 28rpx; }
.pick-picker { flex: 1; display: flex; justify-content: flex-end; align-items: center; }
/* 日期与时间同行：日期占剩余宽度（右对齐后自然贴近时间），时间按内容宽度紧随其后 */
.pick-picker-time { flex: none; margin-left: 12rpx; }
.pick-picker-time .pick-value { flex: none; white-space: nowrap; }
/* 日期时间行：合并成一行后单个选择器变窄、可点高度只有一行文字，
   手指容易点不中。这里把「标签宽度」与「行内上下空隙」都让给选择器：
   - 标签仅 4 字，宽度 200rpx→140rpx，释放 60rpx 给两个选择器
   - 行 padding 24rpx→12rpx，让出的高度给选择器本身（min-height 76rpx ≈ 38px） */
.pick-row-datetime { padding: 12rpx 0; }
.pick-row-datetime .pick-label { width: 140rpx; }
.pick-row-datetime .pick-picker { min-height: 76rpx; }
.today-btn {
  margin-left: 16rpx; padding: 6rpx 24rpx; border-radius: 24rpx;
  background: var(--primary-soft); color: var(--primary-text); font-size: 24rpx;
  flex-shrink: 0;
}

/* 凭证截图行 */
.voucher-row {
  display: flex; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid var(--border);
}
.voucher-row:last-child { border-bottom: none; }
.voucher-area { flex: 1; display: flex; justify-content: flex-end; position: relative; }
.voucher-thumb {
  width: 120rpx; height: 120rpx; border-radius: 12rpx; border: 1rpx solid #eee;
}
.voucher-remove {
  /* 热区 88rpx(=44px)：原 36rpx(18px) 远低于可点最小值，删除类按钮尤其容易误触 */
  min-width: 88rpx; min-height: 88rpx;
  display: flex; align-items: center; justify-content: center;
  position: absolute; top: -14rpx; right: -14rpx; width: 36rpx; height: 36rpx;
  border-radius: 18rpx; background: var(--expense); color: var(--on-expense); font-size: 24rpx;
  display: flex; align-items: center; justify-content: center; line-height: 1;
}
.voucher-add {
  width: 120rpx; height: 120rpx; border-radius: 12rpx; background: var(--surface);
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.voucher-add-icon { font-size: 36rpx; }
.voucher-add-text { font-size: 20rpx; color: var(--text-sub); margin-top: 4rpx; }


.save-bar { background: var(--card); padding: 12rpx 32rpx calc(12rpx + env(safe-area-inset-bottom)); }
.btn-save {
  background: var(--primary); color: var(--on-primary); border-radius: 44rpx; height: 84rpx;
  line-height: 84rpx; text-align: center; font-size: 32rpx; font-weight: 600;
  box-shadow: 0 8rpx 20rpx var(--primary-shadow);
}
.btn-save.disabled { opacity: 0.4; box-shadow: none; }

.sheet { width: 100%; background: var(--card); border-radius: 32rpx 32rpx 0 0; padding: 28rpx 32rpx; max-height: 82vh; display: flex; flex-direction: column; }
.picker-list { max-height: 56vh; }
.picker-cancel {
  margin-top: 20rpx; text-align: center; color: var(--text-sub); font-size: 26rpx;
  height: 72rpx; line-height: 72rpx; border-top: 1rpx solid var(--border);
}
.picker-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20rpx 16rpx; border-bottom: 1rpx solid var(--border); font-size: 28rpx;
}
.picker-item.active { color: var(--primary-text); font-weight: 600; }
.picker-sub { color: var(--text-sub); font-size: 24rpx; }
.picker-empty {
  padding: 60rpx 0; text-align: center; color: var(--primary-text); font-size: 26rpx;
}

/* 快速补录借款项目 */
.quick-add-entry {
  margin: 0 0 16rpx; padding: 18rpx; text-align: center;
  border: 1rpx dashed var(--primary); border-radius: 12rpx; color: var(--primary-text); font-size: 26rpx;
}
.quick-add-form {
  background: var(--primary-soft); border-radius: 16rpx; padding: 20rpx; margin-bottom: 16rpx;
}
.qa-title { font-size: 26rpx; font-weight: 600; color: var(--primary-text); margin-bottom: 12rpx; }
.qa-tip {
  font-size: 22rpx; color: var(--warning); background: #fdf3e7;
  border-radius: 8rpx; padding: 12rpx 16rpx; margin-bottom: 8rpx; line-height: 1.6;
}
.qa-row { display: flex; align-items: center; padding: 12rpx 0; }
.qa-label { width: 190rpx; font-size: 25rpx; color: var(--text-sub); }
.qa-input { flex: 1; font-size: 27rpx; text-align: right; }
.qa-actions { display: flex; gap: 16rpx; margin-top: 12rpx; }
.qa-btn {
  flex: 1; text-align: center; border-radius: 10rpx; padding: 14rpx 0; font-size: 26rpx;
}
.qa-btn.cancel { background: var(--surface); color: var(--text-sub); }
.qa-btn.ok { background: var(--primary); color: var(--on-primary); }
.qa-btn.ok.disabled { opacity: 0.4; }
</style>
