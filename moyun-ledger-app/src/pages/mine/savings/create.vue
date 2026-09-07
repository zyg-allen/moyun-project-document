<template>
  <view class="page" :style="themeVars">
    <NavBar title="新建计划" />

    <view class="form-card">
      <view class="form-row">
        <text class="form-label">计划名称</text>
        <input class="form-input" v-model="form.name" placeholder="例如：买房基金" />
      </view>
      <view class="form-row">
        <text class="form-label">目标金额</text>
        <input class="form-input" type="digit" v-model="form.targetAmount" placeholder="请输入目标金额" />
      </view>
      <view class="form-row">
        <text class="form-label">存钱方式</text>
        <view class="method-group">
          <view class="method-tag" :class="{ active: form.method === '52week' }" @tap="form.method = '52week'">52周存钱法</view>
          <view class="method-tag" :class="{ active: form.method === 'fixed' }" @tap="form.method = 'fixed'">固定金额</view>
          <view class="method-tag" :class="{ active: form.method === 'custom' }" @tap="form.method = 'custom'">自定义</view>
        </view>
      </view>
      <view class="form-row" v-if="form.method === 'fixed'">
        <text class="form-label">每期金额</text>
        <input class="form-input" type="digit" v-model="form.periodAmount" placeholder="每期存入金额" />
      </view>
      <view class="form-row">
        <text class="form-label">开始日期</text>
        <picker mode="date" :value="form.startDate" @change="(e) => form.startDate = e.detail.value">
          <view class="form-value">{{ form.startDate }} ›</view>
        </picker>
      </view>
    </view>

    <!-- 52周存钱法说明 -->
    <view class="tip-card" v-if="form.method === '52week'">
      <text class="tip-title">52周存钱法</text>
      <text class="tip-desc">第1周存¥10，第2周存¥20，……第52周存¥520，累计目标¥13,780。可点击「生成计划」按此规则自动生成52期。</text>
      <view class="gen-btn" @tap="gen52Week">生成52期计划</view>
    </view>

    <view class="submit-btn" @tap="save">保存计划</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { storage, uid } from '@/utils/storage';

export default {
  components: { NavBar },
  data() {
    const today = new Date();
    return {
      form: {
        name: '',
        targetAmount: '',
        method: '52week',
        periodAmount: '',
        startDate: `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`
      }
    };
  },
  computed: { themeVars() { return useThemeStore().themeVars; } },
  onShow() { useThemeStore().restore(); },
  methods: {
    /** 生成52周存钱法的期次 */
    gen52Week() {
      let total = 0;
      for (let w = 1; w <= 52; w++) total += w * 10;
      this.form.targetAmount = String(total);
      uni.showToast({ title: '已生成目标¥' + total.toLocaleString(), icon: 'none' });
    },
    save() {
      if (!this.form.name) { uni.showToast({ title: '请输入计划名称', icon: 'none' }); return; }
      const target = parseFloat(this.form.targetAmount);
      if (!target || target <= 0) { uni.showToast({ title: '请输入目标金额', icon: 'none' }); return; }

      const periods = this.buildPeriods(target);
      const plan = {
        id: uid(),
        name: this.form.name,
        targetAmount: target,
        currentAmount: 0,
        method: this.form.method,
        startDate: this.form.startDate,
        periods,
        createTime: Date.now()
      };
      const plans = storage.get('savings_plans', []) || [];
      plans.unshift(plan);
      storage.set('savings_plans', plans);
      uni.showToast({ title: '创建成功', icon: 'success' });
      setTimeout(() => uni.navigateBack(), 600);
    },
    /** 根据方式生成期次列表 */
    buildPeriods(target) {
      if (this.form.method === '52week') {
        const arr = [];
        for (let w = 1; w <= 52; w++) {
          arr.push({ index: w, targetAmount: w * 10, deposited: 0, status: 'pending', date: '' });
        }
        return arr;
      }
      if (this.form.method === 'fixed') {
        const pa = parseFloat(this.form.periodAmount) || 100;
        const count = Math.ceil(target / pa);
        const arr = [];
        for (let i = 1; i <= count; i++) {
          arr.push({ index: i, targetAmount: pa, deposited: 0, status: 'pending', date: '' });
        }
        return arr;
      }
      // custom：单期
      return [{ index: 1, targetAmount: target, deposited: 0, status: 'pending', date: '' }];
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 120rpx; min-height: 100vh; background: #f5f6f8; }
.form-card { background: #fff; margin: 24rpx; border-radius: 20rpx; padding: 8rpx 32rpx; }
.form-row {
  display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7;
}
.form-row:last-child { border-bottom: none; }
.form-label { width: 180rpx; font-size: 28rpx; color: #333; }
.form-input { flex: 1; text-align: right; font-size: 28rpx; }
.form-value { font-size: 28rpx; color: #333; }
.method-group { flex: 1; display: flex; gap: 16rpx; justify-content: flex-end; flex-wrap: wrap; }
.method-tag {
  font-size: 24rpx; padding: 8rpx 20rpx; border-radius: 24rpx;
  background: #f5f6f8; color: #666;
}
.method-tag.active { background: var(--primary-soft); color: var(--primary-strong); }

.tip-card {
  background: #fffbe6; border: 1rpx solid #ffe58f;
  margin: 0 24rpx; border-radius: 16rpx; padding: 24rpx;
}
.tip-title { font-size: 26rpx; font-weight: 600; color: #d48806; }
.tip-desc { display: block; font-size: 22rpx; color: #8c6d1f; line-height: 1.6; margin-top: 8rpx; }
.gen-btn {
  margin-top: 16rpx; display: inline-block; padding: 8rpx 24rpx;
  background: var(--primary-strong); color: #fff; border-radius: 24rpx; font-size: 24rpx;
}

.submit-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff;
  border-radius: 44rpx; font-size: 30rpx; font-weight: 600;
}
</style>
