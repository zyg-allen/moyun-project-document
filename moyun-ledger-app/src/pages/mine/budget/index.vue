<template>
  <view class="page" :style="themeVars">
    <NavBar title="预算设置" />
    <view class="tip-card">设置后，首页将展示本月支出进度条，超出预算红色预警</view>

    <view class="card">
      <view class="field">
        <text class="field-label">月份</text>
        <view class="fill-picker" @tap="openMonth">
          <view class="field-value">{{ monthStr }} ▾</view>
        </view>
      </view>
      <view class="field">
        <text class="field-label">月度总预算(元)</text>
        <input v-model="totalBudgetYuan" type="digit" placeholder="留空或填 0 表示取消总预算" class="field-input" @input="onTotalBudgetInput" />
      </view>
      <view class="btn-primary" :class="{ disabled: submitting }" @tap="!submitting && saveTotal()">保存总预算</view>
    </view>

    <view class="card">
      <view class="card-title">分类预算（支出）</view>
      <view v-for="c in expenseCategories" :key="c.id" class="field">
        <text class="field-label">{{ c.name }}</text>
        <input :value="budgetOf(c.id)" @input="(e) => setBudgetOf(c.id, e.detail.value)" type="digit"
               placeholder="留空或填 0 取消" class="field-input" />
      </view>
      <view class="btn-primary" :class="{ disabled: submitting }" @tap="!submitting && saveCategoryBudgets()">保存分类预算</view>
    </view>
    <PickerWheel
      :visible="monthSheet"
      title="选择月份"
      :columns="[years, months]"
      :value="monthDraft"
      @close="monthSheet = false"
      @confirm="onMonthConfirm"
    />

  </view>
</template>

<script>
import submitLock from '@/mixins/submitLock';
import { listBudgets, saveBudget, deleteBudget, listCategories } from '@/api/ledger';
import { toNum, toFixedYuan, applyAmountInput } from '@/utils/money';
import { useThemeStore } from '@/stores/theme';
import PickerWheel from '@/components/PickerWheel/PickerWheel.vue';

export default {
  components: { PickerWheel },
  mixins: [submitLock],
  data() {
    const now = new Date();
    return {
      monthSheet: false,
      monthDraft: [0, 0],
      years: [],
      months: [],
      year: now.getFullYear(),
      month: now.getMonth() + 1,
      totalBudgetYuan: '',
      expenseCategories: [],
      budgets: [],           // { categoryId, amount(分) }
      categoryBudgetInput: {} // categoryId -> 元字符串
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    monthStr() {
      return `${this.year}-${String(this.month).padStart(2, '0')}`;
    }
  },
  onShow() {
    // uni-app H5 每个页面各自新建 uni-page-body：必须每次进入都重新注入主题，
    // 否则从别的页跳进来时，本页最外层仍是 page{} 的日间默认（暗色下发白）。
    useThemeStore().applyToPageBody();
    this.load();
  },
  methods: {
    openMonth() {
      const now = new Date();
      const start = 2020;
      const end = now.getFullYear() + 1;
      this.months = Array.from({ length: 12 }, (_, i) => (i + 1 < 10 ? '0' + (i + 1) : '' + (i + 1)));
      this.years = [];
      for (let y = start; y <= end; y++) this.years.push(String(y));
      const yi = Math.max(0, this.years.indexOf(String(this.year)));
      const mi = Math.max(0, this.month - 1);
      this.monthDraft = [yi, mi];
      this.monthSheet = true;
    },
    onMonthConfirm(idx) {
      const v = this.years[idx[0]] + '-' + this.months[idx[1]];
      this.onMonthChange({ detail: { value: v } });
      this.monthSheet = false;
    },
    /** 总预算输入净化（见 utils/money.applyAmountInput） */
    onTotalBudgetInput(e) { applyAmountInput(e, this, 'totalBudgetYuan'); },
    async load() {
      try {
        const [budgetRes, catRes] = await Promise.all([
          listBudgets(this.year, this.month),
          listCategories('expense')
        ]);
        this.budgets = (budgetRes && budgetRes.records) || [];
        this.expenseCategories = (catRes && catRes.records) || [];
        const total = this.budgets.find(b => !b.categoryId);
        this.totalBudgetYuan = total ? toFixedYuan(total.amount) : '';
      } catch (e) { /* 拦截器已提示 */ }
    },
    onMonthChange(e) {
      const [y, m] = e.detail.value.split('-').map(Number);
      this.year = y;
      this.month = m;
      this.load();
    },
    budgetOf(categoryId) {
      const b = this.budgets.find(x => x.categoryId === categoryId);
      return b ? toFixedYuan(b.amount) : '';
    },
    setBudgetOf(categoryId, yuan) {
      this.categoryBudgetInput[categoryId] = yuan;
    },
    async saveTotal() {
      return this.withSubmitLock(async () => {
              // 与服务端约束对齐：金额必须大于 0；因此「留空」或「填 0」表达的是"取消预算"，
              // 而不是保存一个 0（后者会被后端拒绝，用户按提示填 0 反而报错）。
              const raw = String(this.totalBudgetYuan == null ? '' : this.totalBudgetYuan).trim();
              const wantCancel = raw === '' || toNum(raw) <= 0;
              try {
                if (wantCancel) {
                  await deleteBudget(this.year, this.month, null);
                  uni.showToast({ title: '已取消总预算', icon: 'none' });
                } else {
                  await saveBudget({ categoryId: null, year: this.year, month: this.month, amount: toNum(raw) });
                  uni.showToast({ title: '已保存', icon: 'success' });
                }
                this.totalBudgetYuan = ''
                this.load();
              } catch (e) { /* 拦截器已提示 */ }
      });
    },
    async saveCategoryBudgets() {
      return this.withSubmitLock(async () => {
              // 逐条容错：单个分类失败不影响其余（原实现一个 0 会中断整批，
              // 导致"排在它后面的分类全部不保存"且无任何提示）。
              const failed = [];
              let saved = 0;
              let cancelled = 0;
              for (const c of this.expenseCategories) {
                const yuan = this.categoryBudgetInput[c.id];
                if (yuan === undefined) continue;   // 用户没动过这一项
                const raw = String(yuan == null ? '' : yuan).trim();
                const wantCancel = raw === '' || toNum(raw) <= 0;
                try {
                  if (wantCancel) {
                    await deleteBudget(this.year, this.month, c.id);
                    cancelled++;
                  } else {
                    await saveBudget({ categoryId: c.id, year: this.year, month: this.month, amount: toNum(raw) });
                    saved++;
                  }
                } catch (e) {
                  failed.push(c.name || ('分类' + c.id));
                }
              }
              this.categoryBudgetInput = {};
              this.load();
              if (failed.length) {
                uni.showModal({
                  title: '部分未保存',
                  content: '以下分类保存失败：' + failed.join('、'),
                  showCancel: false
                });
              } else if (cancelled && !saved) {
                uni.showToast({ title: '已取消 ' + cancelled + ' 项', icon: 'none' });
              } else {
                uni.showToast({ title: '已保存', icon: 'success' });
              }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.tip-card {
  margin: 24rpx; padding: 20rpx 28rpx; background: var(--primary-soft); color: var(--primary-text);
  border-radius: 16rpx; font-size: 24rpx;
}
.card { background: var(--card); border-radius: 20rpx; padding: 24rpx 32rpx; margin: 0 24rpx 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 8rpx; }
.field { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid var(--border); }
.field:last-of-type { border-bottom: none; }
.field-label { width: 260rpx; font-size: 28rpx; color: var(--text-sub); }
.field-value { flex: 1; text-align: right; font-size: 28rpx; }
.field-input { flex: 1; text-align: right; font-size: 28rpx; }
.btn-primary { margin: 32rpx 0 8rpx; }
</style>
