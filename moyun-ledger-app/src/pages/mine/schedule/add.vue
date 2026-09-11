<template>
  <view class="page" :style="themeVars">
    <NavBar title="添加计划">
      <view class="nav-ok" @tap="save">确定</view>
    </NavBar>

    <!-- 周期设置 -->
    <view class="form-card">
      <view class="form-row">
        <text class="form-label">循环周期</text>
        <picker :range="cycleLabels" :value="cycleIndex" @change="onCycleChange">
          <view class="form-value">{{ cycleLabels[cycleIndex] }} ›</view>
        </picker>
      </view>
      <view class="form-row" v-if="form.cycle === 'weekly'">
        <text class="form-label">每周几</text>
        <picker :range="weekLabels" :value="form.dayOfWeek - 1" @change="(e) => form.dayOfWeek = e.detail.value + 1">
          <view class="form-value">周{{ weekLabels[form.dayOfWeek - 1] }} ›</view>
        </picker>
      </view>
      <view class="form-row" v-if="form.cycle === 'monthly'">
        <text class="form-label">每月几号</text>
        <picker :range="dayList" :value="form.dayOfMonth - 1" @change="(e) => form.dayOfMonth = e.detail.value + 1">
          <view class="form-value">{{ form.dayOfMonth }}号 ›</view>
        </picker>
      </view>
      <view class="form-row" v-if="form.cycle === 'interval'">
        <text class="form-label">间隔天数</text>
        <input class="form-input" type="number" v-model="form.intervalDays" placeholder="如 3" />
      </view>
      <view class="form-row">
        <text class="form-label">执行时间</text>
        <picker mode="time" :value="form.execTime" @change="(e) => form.execTime = e.detail.value">
          <view class="form-value">{{ form.execTime }} ›</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">开始时间</text>
        <picker mode="date" :value="form.startDate" @change="(e) => form.startDate = e.detail.value">
          <view class="form-value">{{ form.startDate }} ›</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">结束时间</text>
        <picker mode="date" :value="form.endDate" @change="(e) => form.endDate = e.detail.value">
          <view class="form-value">{{ form.endDate || '不填表示永不结束' }} ›</view>
        </picker>
      </view>
    </view>

    <!-- 记账内容 -->
    <view class="form-card">
      <view class="form-row col">
        <text class="form-label">类型</text>
        <view class="type-group">
          <view class="type-tag" :class="{ active: form.type === 'expense' }" @tap="onTypeChange('expense')">支出</view>
          <view class="type-tag" :class="{ active: form.type === 'income' }" @tap="onTypeChange('income')">收入</view>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">分类</text>
        <picker :range="categoryNames" @change="onCategoryChange">
          <view class="form-value" :class="{ placeholder: !form.categoryId }">
            {{ form.categoryId ? categoryName : '请选择分类' }} ›
          </view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">金额</text>
        <input class="form-input amount" type="digit" v-model="form.amount" placeholder="0" />
      </view>
      <view class="form-row">
        <text class="form-label">账户</text>
        <picker :range="accountNames" @change="onAccountChange">
          <view class="form-value" :class="{ placeholder: !form.accountId }">
            {{ form.accountId ? accountName : '请选择资产' }} ›
          </view>
        </picker>
      </view>
    </view>

    <!-- 备注 -->
    <view class="form-card">
      <view class="form-row col">
        <text class="form-label">备注</text>
        <textarea class="form-textarea" v-model="form.description" placeholder="点击输入备注..." maxlength="200" />
      </view>
    </view>

    <view class="preview" v-if="nextExecText">下次记账时间：{{ nextExecText }}</view>

    <view class="submit-btn" @tap="save">确定</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { listCategories, listAssets, createScheduleTask } from '@/api/ledger';

export default {
  components: { NavBar },
  data() {
    const today = new Date();
    return {
      cycleLabels: ['每天', '每周', '每月', '每N天'],
      weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
      dayList: Array.from({ length: 28 }, (_, i) => (i + 1) + '号'),
      cycleKeys: ['daily', 'weekly', 'monthly', 'interval'],
      form: {
        name: '',
        type: 'expense',
        amount: '',
        categoryId: null,
        accountId: null,
        description: '',
        cycle: 'daily',
        dayOfWeek: 1,
        dayOfMonth: 1,
        intervalDays: '',
        execTime: '08:00',
        startDate: `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`,
        endDate: ''
      },
      categories: [],
      accounts: []
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    cycleIndex() { return this.cycleKeys.indexOf(this.form.cycle); },
    categoryNames() { return this.categories.map(c => c.name); },
    categoryName() {
      const c = this.categories.find(x => x.id === this.form.categoryId);
      return c ? c.name : '';
    },
    accountNames() { return this.accounts.map(a => a.name); },
    accountName() {
      const a = this.accounts.find(x => x.id === this.form.accountId);
      return a ? a.name : '';
    },
    nextExecText() {
      const d = this.firstExecDate();
      return d ? `${d} ${this.form.execTime}` : '';
    }
  },
  onShow() {
    useThemeStore().restore();
    this.loadOptions();
  },
  methods: {
    async loadOptions() {
      try {
        const catData = await listCategories(this.form.type) || {};
        this.categories = catData.records || [];
      } catch (e) { }
      try {
        const accData = await listAssets() || {};
        this.accounts = accData.records || [];
      } catch (e) { }
    },
    onCycleChange(e) {
      this.form.cycle = this.cycleKeys[e.detail.value];
    },
    onTypeChange(type) {
      if (this.form.type === type) return;
      this.form.type = type;
      this.form.categoryId = null;
      this.loadOptions();
    },
    onCategoryChange(e) {
      const c = this.categories[e.detail.value];
      this.form.categoryId = c ? c.id : null;
    },
    onAccountChange(e) {
      const a = this.accounts[e.detail.value];
      this.form.accountId = a ? a.id : null;
    },
    /** 与后端 firstExecDate 相同的规则：从开始日期起按周期推算第一个执行日 */
    firstExecDate() {
      const start = this.form.startDate ? new Date(this.form.startDate + 'T00:00:00') : new Date();
      const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
      const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
      let candidate = addDays(start, 1);
      switch (this.form.cycle) {
        case 'daily': return fmt(candidate);
        case 'weekly': {
          const jsDow = start.getDay() === 0 ? 7 : start.getDay();
          const diff = (this.form.dayOfWeek - jsDow + 7) % 7;
          return fmt(diff === 0 ? addDays(start, 7) : addDays(start, diff));
        }
        case 'monthly': {
          const base = new Date(start.getFullYear(), start.getMonth(), this.form.dayOfMonth);
          if (base >= start) base.setMonth(base.getMonth() + 1);
          return fmt(base);
        }
        case 'interval': {
          const n = parseInt(this.form.intervalDays);
          return fmt(addDays(start, n > 0 ? n : 1));
        }
      }
      return '';
    },
    async save() {
      const amount = parseFloat(this.form.amount);
      if (!amount || amount <= 0) { uni.showToast({ title: '请输入金额', icon: 'none' }); return; }
      if (!this.form.categoryId) { uni.showToast({ title: '请选择分类', icon: 'none' }); return; }
      if (!this.form.accountId) { uni.showToast({ title: '请选择资产账户', icon: 'none' }); return; }
      if (this.form.cycle === 'interval' && !(parseInt(this.form.intervalDays) > 0)) {
        uni.showToast({ title: '请输入间隔天数', icon: 'none' }); return;
      }
      const payload = {
        name: this.form.description ? this.form.description.slice(0, 50)
              : (this.form.type === 'expense' ? '定时支出' : '定时收入'),
        type: this.form.type,
        amount,
        categoryId: this.form.categoryId,
        accountId: this.form.accountId,
        description: this.form.description || null,
        cycle: this.form.cycle,
        dayOfWeek: this.form.cycle === 'weekly' ? this.form.dayOfWeek : null,
        dayOfMonth: this.form.cycle === 'monthly' ? this.form.dayOfMonth : null,
        intervalDays: this.form.cycle === 'interval' ? parseInt(this.form.intervalDays) : null,
        execTime: this.form.execTime,
        startDate: this.form.startDate,
        endDate: this.form.endDate || null
      };
      try {
        await createScheduleTask(payload);
        uni.showToast({ title: '创建成功', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 600);
      } catch (e) { /* 拦截器已提示 */ }
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: #f5f6f8; }
.nav-ok { font-size: 26rpx; color: var(--primary-strong); }

.form-card { background: #fff; margin: 24rpx 24rpx 0; border-radius: 20rpx; padding: 8rpx 32rpx; }
.form-row {
  display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.form-row:last-child { border-bottom: none; }
.form-row.col { flex-direction: column; align-items: flex-start; gap: 20rpx; }
.form-label { width: 180rpx; font-size: 28rpx; color: #333; flex-shrink: 0; }
.form-row.col .form-label { width: auto; }
.form-input { flex: 1; text-align: right; font-size: 28rpx; }
.form-input.amount { font-size: 34rpx; font-weight: 700; color: var(--primary-strong); }
.form-value { font-size: 28rpx; color: #333; }
.form-value.placeholder { color: #bbb; }

.type-group { display: flex; gap: 16rpx; }
.type-tag {
  font-size: 26rpx; padding: 10rpx 40rpx; border-radius: 32rpx;
  background: #f5f6f8; color: #666;
}
.type-tag.active { background: var(--primary-strong); color: #fff; }

.form-textarea {
  width: 100%; min-height: 120rpx; font-size: 26rpx; color: #333;
  background: #f8f9fa; border-radius: 12rpx; padding: 20rpx; box-sizing: border-box;
}

.preview {
  margin: 24rpx 32rpx 0; font-size: 24rpx; color: var(--primary-strong);
  background: var(--primary-soft); border-radius: 12rpx; padding: 16rpx 24rpx;
}

.submit-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff;
  border-radius: 44rpx; font-size: 30rpx; font-weight: 600;
}
</style>