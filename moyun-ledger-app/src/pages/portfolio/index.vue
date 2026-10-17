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
        <view class="summary-sub">共 {{ assetEnabled.length }} 个账户
          <text class="link" @tap="showArchived = !showArchived">{{ showArchived ? ' · 隐藏归档' : ' · 显示归档' }}</text>
        </view>
      </view>

      <view v-for="g in assetGroups" :key="g.type" class="card">
        <view class="card-title flex-row" @tap="toggleGroup('a_' + g.type)">
          <text class="flex-1">{{ g.label }} <text class="gcount">{{ g.items.length }}</text></text>
          <text class="add-mini" @tap.stop="addAsset(g.type)">＋</text>
          <text class="collapse-arrow">{{ isCollapsed('a_' + g.type) ? '▸' : '▾' }}</text>
        </view>
        <view v-if="!isCollapsed('a_' + g.type)">
          <view v-if="g.items.length === 0" class="empty">暂无{{ g.label }}，点右侧「＋」添加</view>
          <view v-for="a in g.items" :key="a.id" class="row" @tap="viewTxn('asset', a)">
            <view class="row-icon" :style="{ background: assetIcon(a.accountType).bg }">
              <text>{{ assetIcon(a.accountType).emoji }}</text>
            </view>
            <view class="flex-1">
              <view class="row-name">
                {{ a.name }}
                <text v-if="a.status === 0" class="archived-tag">已归档</text>
              </view>
              <view class="row-type">{{ assetTypeText(a.accountType) }}</view>
            </view>
            <view class="row-actions">
              <view class="pencil-btn" @tap.stop="editAsset(a)">✎</view>
              <view class="row-balance">{{ privacyMode ? '****' : '¥ ' + formatAmount(a.balance) }}</view>
            </view>
          </view>
        </view>
      </view>
    </template>
    <!-- ========== 负债视图 ========== -->
    <template v-else>
      <view class="summary liability">
        <view class="summary-label">总负债（计入合计的账户）</view>
        <view class="summary-amount">{{ privacyMode ? '****' : '¥ ' + liabTotalText }}</view>
        <view class="summary-sub">
          在还 {{ liabActive.length }} 笔<text v-if="settledCount > 0"> · 已结清 {{ settledCount }} 笔</text>
          <text class="link" @tap="showArchived = !showArchived">{{ showArchived ? ' · 隐藏归档' : ' · 显示归档' }}</text>
        </view>
      </view>

      <view v-for="g in liabGroups" :key="g.type" class="card">
        <view class="card-title flex-row" @tap="toggleGroup('l_' + g.type)">
          <text class="flex-1">{{ g.label }} <text class="gcount">{{ g.items.length }}</text></text>
          <text class="add-mini" @tap.stop="addLiability(g.type)">＋</text>
          <text class="collapse-arrow">{{ isCollapsed('l_' + g.type) ? '▸' : '▾' }}</text>
        </view>
        <view v-if="!isCollapsed('l_' + g.type)">
          <view v-if="g.items.length === 0" class="empty">暂无{{ g.label }}，点右侧「＋」添加</view>
          <view v-for="l in g.items" :key="l.id" class="row" @tap="viewTxn('liability', l)">
            <view class="row-icon liab" :class="l.accountType">{{ liabTypeText(l.accountType).slice(0, 1) }}</view>
            <view class="flex-1">
              <view class="row-name">
                {{ l.name }}
                <text v-if="l.settleFlag === 1" class="settle-tag">已结清</text>
                <text v-if="l.status === 0" class="archived-tag">已归档</text>
              </view>
              <view class="row-type">
                {{ liabTypeText(l.accountType) }}
                <text v-if="l.monthlyPayment"> · 月供 ¥{{ formatAmount(l.monthlyPayment) }}</text>
                <text v-if="l.repaymentDay"> · {{ l.repaymentDay }}日还款</text>
                <text v-if="l.totalTerms"> · {{ l.paidTerms || 0 }}/{{ l.totalTerms }}期</text>
              </view>
            </view>
            <view class="row-actions">
              <view class="pencil-btn" @tap.stop="editLiability(l)">✎</view>
              <view class="row-balance liab">{{ privacyMode ? '****' : '¥ ' + formatAmount(l.balance) }}</view>
            </view>
          </view>
        </view>
      </view>
    </template>

    <!-- 添加按钮已移到各分类组标题右侧（点「＋」= 新增该类型账户） -->

    <!-- 资产弹层（键盘安全：H5 抬升 + 小程序 adjust-position + 内容可滚动） -->
    <KeyboardAwareSheet v-model="assetEditing" :title="assetSheetTitle">
      <view class="field">
        <text class="field-label">名称</text>
        <input v-model="assetForm.name" :placeholder="assetNamePlaceholder" class="field-input"
          adjust-position
          :cursor-spacing="24"
         />
      </view>
      <view class="field">
        <text class="field-label">类型</text>
        <picker class="fill-picker" :range="assetTypeNames" @change="onAssetTypeChange">
          <view class="field-picker">{{ assetTypeText(assetForm.accountType) }} ▾</view>
        </picker>
      </view>
      <view class="field" v-if="!assetForm.id">
        <text class="field-label">初始余额(元)</text>
        <input v-model="assetForm.initialBalance" type="digit" placeholder="0.00" class="field-input" @input="onAssetBalanceInput"
          adjust-position
          :cursor-spacing="24"
         />
      </view>
      <view class="field">
        <text class="field-label">计入总资产</text>
        <switch :checked="assetForm.includeInTotal === 1" @change="onAssetTotalChange" style="transform: scale(0.8)" />
      </view>
      <view class="btn-primary" :class="{ disabled: submitting }" @tap="!submitting && saveAsset()">保存</view>
      <view class="btn-danger" v-if="assetForm.id" @tap="removeAsset">删除（归档，流水保留）</view>
      <view class="btn-cancel" @tap="assetEditing = false">取消</view>
    </KeyboardAwareSheet>

    <!-- 负债弹层（键盘安全：同资产弹层） -->
    <KeyboardAwareSheet v-model="liabEditing" :title="liabSheetTitle">
      <view class="sheet" @tap.stop>
        <view class="field">
          <text class="field-label">名称</text>
          <input v-model="liabForm.name" :placeholder="liabNamePlaceholder" class="field-input"
            adjust-position
            :cursor-spacing="24"
           />
        </view>
        <view class="field">
          <text class="field-label">类型</text>
          <picker class="fill-picker" :range="liabTypeNames" @change="onLiabTypeChange">
            <view class="field-picker">{{ liabTypeText(liabForm.accountType) }} ▾</view>
          </picker>
        </view>
        <view class="field" v-if="!liabForm.id">
          <text class="field-label">当前欠款(元)</text>
          <input v-model="liabForm.initialBalance" type="digit" placeholder="0.00" class="field-input" @input="onLiabBalanceInput"
            adjust-position
            :cursor-spacing="24"
           />
        </view>
        <view class="field">
          <text class="field-label">月供(元)</text>
          <input v-model="liabForm.monthlyPayment" type="digit" placeholder="选填" class="field-input" @input="onLiabMonthlyInput"
            adjust-position
            :cursor-spacing="24"
           />
        </view>
        <view class="field">
          <text class="field-label">还款日</text>
          <picker class="fill-picker" :range="days" @change="onDayChange">
            <view class="field-picker">{{ liabForm.repaymentDay ? liabForm.repaymentDay + '日' : '选填' }} ▾</view>
          </picker>
        </view>
        <view class="field">
          <text class="field-label">总期数</text>
          <input v-model="liabForm.totalTerms" type="number" placeholder="选填" class="field-input"
            adjust-position
            :cursor-spacing="24"
           />
        </view>
        <view class="field">
          <text class="field-label">计入总负债</text>
          <switch :checked="liabForm.includeInTotal === 1" @change="onLiabTotalChange" style="transform: scale(0.8)" />
        </view>
        <view class="btn-primary" :class="{ disabled: submitting }" @tap="!submitting && saveLiability()">保存</view>
        <view class="btn-danger" v-if="liabForm.id" @tap="removeLiability">删除（归档，流水保留）</view>
        <view class="btn-cancel" @tap="liabEditing = false">取消</view>
      </view>
    </KeyboardAwareSheet>
  </view>
</template>

<script>
import keyboardAware from '@/mixins/keyboardAware';
import submitLock from '@/mixins/submitLock';
import KeyboardAwareSheet from '@/components/KeyboardAwareSheet/KeyboardAwareSheet.vue';
import { listAssets, listAccountTypes, createAsset, updateAsset, deleteAsset, listLiabilities, createLiability, updateLiability, deleteLiability } from '@/api/ledger';
import { formatAmount, toFixedYuan, toNum, safeSum, applyAmountInput } from '@/utils/money';
import { useThemeStore } from '@/stores/theme';

const ASSET_TYPES = [
  ['fund', '资金账户'], ['receivable', '应收款'], ['investment', '投资'], ['fixed', '固定资产']
];
const ASSET_ICONS = {
  fund: { emoji: '💰', bg: '#F5C518' },
  receivable: { emoji: '🤝', bg: '#EE8FA9' },
  investment: { emoji: '📈', bg: '#6BCB77' },
  fixed: { emoji: '🏠', bg: '#9AD2A9' }
};
const LIAB_TYPES = [
  ['debt', '普通负债'], ['credit', '赊账应付']
];

export default {
  mixins: [keyboardAware, submitLock],
  components: { KeyboardAwareSheet },  // 键盘安全弹层
  data() {
    return {
      // 应收款（借出）：只取未结清的，已结清的早已变成现金，再列会重复计算
      receivables: [],
      // 应付款（借款）：同理只取未结清的
      payables: [],
      activeTab: 'asset',
      // 资产
      accounts: [],
      showArchived: false,
      privacyMode: false,
      // 分组折叠状态（key = 'a_'+类型 / 'l_'+类型，true=收起）
      collapsed: {},
      assetEditing: false,
      assetForm: { id: null, name: '', accountType: 'fund', initialBalance: '', includeInTotal: 1 },
      assetTypes: ASSET_TYPES,
      // 负债
      liabilities: [],
      liabEditing: false,
      liabForm: this.emptyLiabForm(),
      liabTypes: LIAB_TYPES,
      days: Array.from({ length: 28 }, (_, i) => (i + 1) + '日')
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    assetTypeNames() { return this.assetTypes.map(t => t[1]); },
    liabTypeNames() { return this.liabTypes.map(t => t[1]); },
    assetEnabled() { return this.accounts.filter(a => a.status === 1); },
    assetFilteredList() { return this.showArchived ? this.accounts : this.assetEnabled; },
    assetTotalText() {
      const list = this.assetEnabled.filter(a => a.includeInTotal === 1);
      return formatAmount(safeSum(list, 'balance'));
    },
    liabActive() { return this.liabilities.filter(l => l.status === 1 && l.settleFlag !== 1); },
    settledCount() { return this.liabilities.filter(l => l.settleFlag === 1).length; },
    liabFilteredList() {
      return this.showArchived ? this.liabilities : this.liabilities.filter(l => l.status === 1);
    },
    liabTotalText() {
      const list = this.liabilities.filter(l => l.status === 1 && l.settleFlag !== 1 && l.includeInTotal === 1);
      return formatAmount(safeSum(list, 'balance'));
    },
    /** 资产按账户类型分组（资金/应收/投资/固定），每组标题右侧可「＋」新增该类账户 */
    assetGroups() {
      return this.assetTypes.map(([type, label]) => ({
        type,
        label,
        items: this.assetFilteredList.filter(a => a.accountType === type)
      }));
    },
    /** 负债按账户类型分组（普通负债/赊账应付） */
    liabGroups() {
      return this.liabTypes.map(([type, label]) => ({
        type,
        label,
        items: this.liabFilteredList.filter(l => l.accountType === type)
      }));
    },
    /** 弹层标题（避免在模板里写嵌套引号，模板属性用双引号包单引号易被编译器误判） */
    /** 弹层标题：按账户类型动态（新增/编辑 + 类型名） */
    assetSheetTitle() {
      const t = this.assetTypes.find(x => x[0] === this.assetForm.accountType);
      return (this.assetForm.id ? '编辑' : '新增') + (t ? t[1] : '账户');
    },
    liabSheetTitle() {
      const t = this.liabTypes.find(x => x[0] === this.liabForm.accountType);
      return (this.liabForm.id ? '编辑' : '新增') + (t ? t[1] : '负债');
    },
    /** 名称输入框提示：按账户类型给不同例子 */
    assetNamePlaceholder() {
      const map = {
        fund: '如：微信 / 招行储蓄卡 / 现金',
        receivable: '如：张三（借款）',
        investment: '如：沪深300基金 / 茅台股票',
        fixed: '如：自住房 / 家用车'
      };
      return map[this.assetForm.accountType] || '给账户起个名字';
    },
    liabNamePlaceholder() {
      const map = {
        debt: '如：招行信用卡 / 房贷',
        credit: '如：李四（欠款） / 拼多多待结算'
      };
      return map[this.liabForm.accountType] || '给账户起个名字';
    },
  },
  onShow() {
    useThemeStore().restore();
    this.privacyMode = uni.getStorageSync('ledger_privacy') === '1';
    this.loadAssets();
    this.loadLiabilities();
    this.loadAccountTypes();
  },
  onPullDownRefresh() {
    Promise.all([this.loadAssets(), this.loadLiabilities()]).finally(() => uni.stopPullDownRefresh());
  },

  methods: {
    /** 分组是否收起（key = 'a_'+类型 / 'l_'+类型） */
    isCollapsed(key) { return !!this.collapsed[key]; },
    /** 点分组标题收起/展开 */
    toggleGroup(key) { this.$set(this.collapsed, key, !this.collapsed[key]); },
    /** 金额输入净化（见 utils/money.applyAmountInput） */
    onAssetBalanceInput(e) { applyAmountInput(e, this, 'assetForm.initialBalance'); },
    /** 金额输入净化（见 utils/money.applyAmountInput） */
    onLiabBalanceInput(e) { applyAmountInput(e, this, 'liabForm.initialBalance'); },
    /** 金额输入净化（见 utils/money.applyAmountInput） */
    onLiabMonthlyInput(e) { applyAmountInput(e, this, 'liabForm.monthlyPayment'); },
    viewTxn(kind, item) {
      const p = [];
      if (kind === 'asset') { p.push('accountId=' + item.id); p.push('name=' + encodeURIComponent(item.name)); }
      else { p.push('liabilityId=' + item.id); p.push('name=' + encodeURIComponent(item.name)); }
      uni.navigateTo({ url: '/pages/record/list?' + p.join('&') });
    },
    switchTab(tab) { this.activeTab = tab; },
    formatAmount,
    // ===== 资产 =====
    assetIcon(type) { return ASSET_ICONS[type] || ASSET_ICONS.other; },
    assetTypeText(type) {
      const found = this.assetTypes.find(t => t[0] === type);
      return found ? found[1] : type;
    },
    /** 账户类型：字典优先，枚举兜底 */
    async loadAccountTypes() {
      try {
        const res = await listAccountTypes() || {};
        if (res.asset && res.asset.length) this.assetTypes = res.asset.map(x => [x.value, x.label]);
        if (res.liability && res.liability.length) this.liabTypes = res.liability.map(x => [x.value, x.label]);
      } catch (e) { /* 拦截器已提示，用枚举兜底 */ }
    },
    async loadAssets() {
      try {
        const res = await listAssets(true);
        this.accounts = (res && res.records) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    addAsset(type) {
      this.assetForm = { id: null, name: '', accountType: type || 'fund', initialBalance: '', includeInTotal: 1 };
      this.assetEditing = true;
    },
    editAsset(a) {
      this.assetForm = { id: a.id, name: a.name, accountType: a.accountType, initialBalance: '', includeInTotal: a.includeInTotal };
      this.assetEditing = true;
    },
    onAssetTypeChange(e) { this.assetForm.accountType = this.assetTypes[e.detail.value][0]; },
    onAssetTotalChange(e) { this.assetForm.includeInTotal = e.detail.value ? 1 : 0; },
    async saveAsset() {
      return this.withSubmitLock(async () => {
              if (!this.assetForm.name.trim()) {
                uni.showToast({ title: '请输入账户名称', icon: 'none' });
                return;
              }
              try {
                if (this.assetForm.id) {
                  await updateAsset(this.assetForm.id, {
                    name: this.assetForm.name, accountType: this.assetForm.accountType, includeInTotal: this.assetForm.includeInTotal
                  });
                } else {
                  await createAsset({
                    name: this.assetForm.name, accountType: this.assetForm.accountType, includeInTotal: this.assetForm.includeInTotal,
                    initialBalance: toNum(this.assetForm.initialBalance)
                  });
                }
                this.assetEditing = false;
                this.loadAssets();
              } catch (e) { /* 拦截器已提示 */ }
      });
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
      return { id: null, name: '', accountType: 'debt', initialBalance: '', monthlyPayment: '', repaymentDay: null, totalTerms: '', includeInTotal: 1 };
    },
    liabTypeText(type) {
      const found = this.liabTypes.find(t => t[0] === type);
      return found ? found[1] : type;
    },
    async loadLiabilities() {
      try {
        const res = await listLiabilities(true);
        this.liabilities = (res && res.records) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    addLiability(type) {
      this.liabForm = this.emptyLiabForm();
      if (type) this.liabForm.accountType = type;
      this.liabEditing = true;
    },
    editLiability(l) {
      this.liabForm = {
        id: l.id, name: l.name, accountType: l.accountType, initialBalance: '',
        monthlyPayment: l.monthlyPayment ? toFixedYuan(l.monthlyPayment) : '',
        repaymentDay: l.repaymentDay, totalTerms: l.totalTerms || '', includeInTotal: l.includeInTotal
      };
      this.liabEditing = true;
    },
    onLiabTypeChange(e) { this.liabForm.accountType = this.liabTypes[e.detail.value][0]; },
    onDayChange(e) { this.liabForm.repaymentDay = e.detail.value + 1; },
    onLiabTotalChange(e) { this.liabForm.includeInTotal = e.detail.value ? 1 : 0; },
    async saveLiability() {
      return this.withSubmitLock(async () => {
              if (!this.liabForm.name.trim()) {
                uni.showToast({ title: '请输入负债名称', icon: 'none' });
                return;
              }
              try {
                if (this.liabForm.id) {
                  await updateLiability(this.liabForm.id, {
                    name: this.liabForm.name, accountType: this.liabForm.accountType, includeInTotal: this.liabForm.includeInTotal,
                    monthlyPayment: this.liabForm.monthlyPayment ? toNum(this.liabForm.monthlyPayment) : null,
                    repaymentDay: this.liabForm.repaymentDay || null,
                    totalTerms: this.liabForm.totalTerms ? Number(this.liabForm.totalTerms) : null
                  });
                } else {
                  await createLiability({
                    name: this.liabForm.name, accountType: this.liabForm.accountType, includeInTotal: this.liabForm.includeInTotal,
                    initialBalance: toNum(this.liabForm.initialBalance),
                    monthlyPayment: this.liabForm.monthlyPayment ? toNum(this.liabForm.monthlyPayment) : null,
                    repaymentDay: this.liabForm.repaymentDay || null,
                    totalTerms: this.liabForm.totalTerms ? Number(this.liabForm.totalTerms) : null
                  });
                }
                this.liabEditing = false;
                this.loadLiabilities();
              } catch (e) { /* 拦截器已提示 */ }
      });
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
  display: flex; background: var(--card);
  padding: calc(var(--status-bar-height) + 16rpx) 24rpx 16rpx;
}
.seg-item {
  flex: 1; text-align: center; font-size: 30rpx; color: var(--text-sub);
  padding: 16rpx 0; border-radius: 40rpx; margin: 0 8rpx;
  background: var(--surface); transition: all 0.2s;
}
.seg-item.on { color: var(--on-primary); background: var(--primary); font-weight: 600; }

/*
 * 汇总卡配色（v13.68 改版）
 *
 * 原实现：资产用 var(--primary)（薄荷绿）+ 负债用红渐变 →
 * 两块卡上下相邻形成"红配绿"，既刺眼又廉价，且绿在语义上并不代表"资产"。
 *
 * 现改为**中性卡面 + 左侧语义色竖条 + 数字用语义色**：
 * 大面积用 --card 保持克制，只在关键数字上给颜色，信息层级反而更清楚，
 * 同时天然适配日间/暗色/护眼三种外观。
 */
.summary {
  position: relative;
  background: var(--card);
  color: var(--text-main);
  border-radius: 20rpx;
  padding: 32rpx 40rpx 40rpx;
  margin: 0 24rpx 24rpx;
  overflow: hidden;
}
/* 左侧语义色竖条：资产=收入色（青绿），负债=支出色（暖橙） */
.summary::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 8rpx;
  background: var(--income);
}
.summary.liability::before { background: var(--expense); }
.summary-label { font-size: 26rpx; color: var(--text-sub); }
.summary-amount {
  font-size: 60rpx; font-weight: 700; margin: 12rpx 0;
  /* 数字给语义色，一眼分辨资产/负债 */
  color: var(--income);
}
.summary.liability .summary-amount { color: var(--expense); }
.summary-sub { font-size: 24rpx; color: var(--text-sub); }

.card { background: var(--card); border-radius: 20rpx; padding: 24rpx 32rpx; margin: 0 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 12rpx; }
/* 分类组右侧「＋」：新增该类型账户 */
.add-mini {
  width: 48rpx; height: 48rpx; line-height: 44rpx; text-align: center;
  border-radius: 50%; background: var(--primary-soft, #eef2ff);
  color: var(--primary); font-size: 34rpx; font-weight: 600;
  margin-left: 16rpx;
}
/* 分组标题：数量 + 折叠箭头 */
.gcount { font-size: 24rpx; color: var(--text-muted); font-weight: 400; }
.collapse-arrow { font-size: 24rpx; color: var(--text-muted); margin-left: 12rpx; }
.link { color: var(--primary-text); font-size: 26rpx; font-weight: 400; }
.empty { text-align: center; color: var(--text-muted); padding: 60rpx 0; font-size: 26rpx; }

.row { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid var(--border); }
.row-icon {
  width: 72rpx; height: 72rpx; border-radius: 36rpx; margin-right: 20rpx;
  display: flex; align-items: center; justify-content: center;
  color: var(--on-primary); font-size: 28rpx; font-weight: 600; background: var(--primary);
}
/* 负债图标：暖橙系底（与资产主题蓝区分） */
.row-icon.liab { background: linear-gradient(135deg, #f2913d, #e8703a); }
.row-name { font-size: 28rpx; }
.archived-tag { font-size: 20rpx; color: var(--text-sub); background: var(--surface); border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.settle-tag { font-size: 20rpx; color: var(--income); border: 1rpx solid var(--income); border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 12rpx; }
.row-type { font-size: 22rpx; color: var(--text-muted); margin-top: 4rpx; }
.row-balance { font-size: 32rpx; font-weight: 600; }
/* 负债金额：红色（欠的钱），资产金额保持默认 */
.row-balance.liab { color: var(--expense); }

.fab {
  position: fixed; right: 40rpx; bottom: 200rpx;
  width: 100rpx; height: 100rpx; border-radius: 50rpx;
  background: var(--primary); color: var(--on-primary); font-size: 48rpx;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx var(--primary-shadow);
}

.sheet-title { font-size: 34rpx; font-weight: 600; text-align: center; margin-bottom: 32rpx; }
.field { display: flex; align-items: center; padding: 24rpx 0; border-bottom: 1rpx solid var(--border); }
.field-label { width: 200rpx; font-size: 28rpx; color: var(--text-sub); }
.field-input { flex: 1; font-size: 28rpx; text-align: right; }
.field-picker { flex: 1; font-size: 28rpx; text-align: right; color: var(--text-main); }
.btn-primary { margin-top: 40rpx; }
.btn-danger {
  margin-top: 24rpx; text-align: center; color: var(--expense); font-size: 28rpx;
  border: 1rpx solid var(--expense); border-radius: 44rpx; height: 80rpx; line-height: 80rpx;
}
.btn-cancel {
  margin-top: 20rpx; text-align: center; color: var(--text-sub); font-size: 26rpx;
  height: 72rpx; line-height: 72rpx;
}

.row-actions { display: flex; align-items: center; gap: 12rpx; flex-shrink: 0; }
.pencil-btn {
  /* 热区 88rpx(=44px)：原 52rpx(26px) 偏小 */
  min-width: 88rpx; min-height: 88rpx;
  display: flex; align-items: center; justify-content: center;
  width: 52rpx; height: 52rpx; line-height: 52rpx; text-align: center;
  border-radius: 50%; background: var(--surface); color: var(--text-sub); font-size: 22rpx;
}
.pencil-btn:active { background: var(--border); }
/* 提交锁生效时给出视觉反馈（与全局 .btn-primary.disabled 一致） */
.btn-primary.disabled { opacity: 0.5; }
</style>
