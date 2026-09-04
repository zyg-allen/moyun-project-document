<template>
  <view class="page" :style="themeVars">
    <NavBar title="预算设置" />
    <view class="tip-card">设置后，首页将展示本月支出进度条，超出预算红色预警</view>

    <view class="card">
      <view class="field">
        <text class="field-label">月份</text>
        <picker mode="date" fields="month" :value="monthStr" @change="onMonthChange">
          <view class="field-value">{{ monthStr }} ▾</view>
        </picker>
      </view>
      <view class="field">
        <text class="field-label">月度总预算(元)</text>
        <input v-model="totalBudgetYuan" type="digit" placeholder="0.00 表示不设总预算" class="field-input" />
      </view>
      <view class="btn-primary" @tap="saveTotal">保存总预算</view>
    </view>

    <view class="card">
      <view class="card-title">分类预算（支出）</view>
      <view v-for="c in expenseCategories" :key="c.id" class="field">
        <text class="field-label">{{ c.name }}</text>
        <input :value="budgetOf(c.id)" @input="(e) => setBudgetOf(c.id, e.detail.value)" type="digit"
               placeholder="不设" class="field-input" />
      </view>
      <view class="btn-primary" @tap="saveCategoryBudgets">保存分类预算</view>
    </view>
  </view>
</template>

<script>
import { listBudgets, saveBudget, listCategories } from '@/api/ledger';
import { yuanToCent, centToYuan } from '@/utils/money';
import { useThemeStore } from '@/stores/theme';

export default {
  data() {
    const now = new Date();
    return {
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
    this.load();
  },
  methods: {
    async load() {
      try {
        const [budgetRes, catRes] = await Promise.all([
          listBudgets(this.year, this.month),
          listCategories('expense')
        ]);
        this.budgets = (budgetRes && budgetRes.records) || [];
        this.expenseCategories = (catRes && catRes.records) || [];
        const total = this.budgets.find(b => !b.categoryId);
        this.totalBudgetYuan = total ? centToYuan(total.amount) : '';
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
      return b ? centToYuan(b.amount) : '';
    },
    setBudgetOf(categoryId, yuan) {
      this.categoryBudgetInput[categoryId] = yuan;
    },
    async saveTotal() {
      try {
        await saveBudget({
          categoryId: null,
          year: this.year,
          month: this.month,
          amount: yuanToCent(this.totalBudgetYuan || '0')
        });
        uni.showToast({ title: '已保存', icon: 'success' });
        this.load();
      } catch (e) { /* 拦截器已提示 */ }
    },
    async saveCategoryBudgets() {
      try {
        for (const c of this.expenseCategories) {
          const yuan = this.categoryBudgetInput[c.id];
          if (yuan === undefined || yuan === '') continue;
          await saveBudget({
            categoryId: c.id,
            year: this.year,
            month: this.month,
            amount: yuanToCent(yuan)
          });
        }
        uni.showToast({ title: '已保存', icon: 'success' });
        this.load();
      } catch (e) { /* 拦截器已提示 */ }
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.tip-card {
  margin: 24rpx; padding: 20rpx 28rpx; background: var(--primary-soft); color: var(--primary-strong);
  border-radius: 16rpx; font-size: 24rpx;
}
.card { background: #fff; border-radius: 20rpx; padding: 24rpx 32rpx; margin: 0 24rpx 24rpx; }
.card-title { font-size: 30rpx; font-weight: 600; margin-bottom: 8rpx; }
.field { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.field:last-of-type { border-bottom: none; }
.field-label { width: 260rpx; font-size: 28rpx; color: #666; }
.field-value { flex: 1; text-align: right; font-size: 28rpx; }
.field-input { flex: 1; text-align: right; font-size: 28rpx; }
.btn-primary { margin: 32rpx 0 8rpx; }
</style>
