<template>
  <view class="page" :style="themeVars" v-if="plan">
    <NavBar :title="plan.name" />

    <!-- 概览 -->
    <view class="overview">
      <view class="ov-row">
        <view class="ov-item">
          <text class="ov-val">¥{{ plan.currentAmount.toFixed(2) }}</text>
          <text class="ov-lab">已存入</text>
        </view>
        <view class="ov-divider"></view>
        <view class="ov-item">
          <text class="ov-val">¥{{ plan.targetAmount.toFixed(2) }}</text>
          <text class="ov-lab">目标金额</text>
        </view>
        <view class="ov-divider"></view>
        <view class="ov-item">
          <text class="ov-val">{{ progressPercent }}%</text>
          <text class="ov-lab">完成进度</text>
        </view>
      </view>
      <view class="progress-bar">
        <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
      </view>
    </view>

    <!-- 统计 -->
    <view class="stats-row">
      <view class="stat success">
        <text class="stat-num">{{ successCount }}</text>
        <text class="stat-lab">成功</text>
      </view>
      <view class="stat fail">
        <text class="stat-num">{{ failCount }}</text>
        <text class="stat-lab">失败</text>
      </view>
      <view class="stat pending">
        <text class="stat-num">{{ pendingCount }}</text>
        <text class="stat-lab">待存</text>
      </view>
    </view>

    <!-- 期次列表 -->
    <view class="period-list">
      <view class="period-title">存钱记录（{{ plan.periods.length }}期）</view>
      <view class="period-card" v-for="p in plan.periods" :key="p.index">
        <view class="p-left">
          <view class="p-no">第{{ p.index }}期</view>
          <view class="p-amount">目标 ¥{{ p.targetAmount }}</view>
          <view class="p-date" v-if="p.date">{{ p.date }}</view>
        </view>
        <view class="p-right">
          <view class="p-status" :class="p.status">
            {{ statusText(p.status) }}
          </view>
          <view class="p-actions" v-if="p.status === 'pending'">
            <view class="p-btn ok" @tap="markPeriod(p, 'success')">存入</view>
            <view class="p-btn no" @tap="markPeriod(p, 'failed')">放弃</view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { storage } from '@/utils/storage';

const STATUS_TEXT = { pending: '待存', success: '成功', failed: '失败' };

export default {
  components: { NavBar },
  data() { return { plan: null }; },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    progressPercent() {
      if (!this.plan || !this.plan.targetAmount) return 0;
      return Math.min(100, Math.round((this.plan.currentAmount / this.plan.targetAmount) * 100));
    },
    successCount() { return this.plan.periods.filter(p => p.status === 'success').length; },
    failCount() { return this.plan.periods.filter(p => p.status === 'failed').length; },
    pendingCount() { return this.plan.periods.filter(p => p.status === 'pending').length; }
  },
  onLoad(options) {
    this.planId = options.id;
    this.loadPlan();
  },
  onShow() { useThemeStore().restore(); },
  methods: {
    statusText(s) { return STATUS_TEXT[s] || s; },
    loadPlan() {
      const plans = storage.get('savings_plans', []) || [];
      this.plan = plans.find(p => p.id === this.planId) || null;
    },
    markPeriod(p, status) {
      if (status === 'success') {
        p.deposited = p.targetAmount;
        p.date = new Date().toISOString().slice(0, 10);
      } else {
        p.deposited = 0;
        p.date = new Date().toISOString().slice(0, 10);
      }
      p.status = status;
      // 汇总已存入金额
      this.plan.currentAmount = this.plan.periods
        .filter(x => x.status === 'success')
        .reduce((s, x) => s + x.targetAmount, 0);
      this.savePlan();
      uni.showToast({ title: status === 'success' ? '已记录存入' : '已标记放弃', icon: 'none' });
    },
    savePlan() {
      const plans = storage.get('savings_plans', []) || [];
      const idx = plans.findIndex(p => p.id === this.plan.id);
      if (idx >= 0) { plans[idx] = this.plan; storage.set('savings_plans', plans); }
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; min-height: 100vh; background: #f5f6f8; }
.overview {
  background: var(--primary); color: #fff; margin: 24rpx; border-radius: 20rpx; padding: 32rpx;
}
.ov-row { display: flex; align-items: center; justify-content: space-around; }
.ov-item { display: flex; flex-direction: column; align-items: center; }
.ov-val { font-size: 32rpx; font-weight: 700; }
.ov-lab { font-size: 22rpx; opacity: 0.85; margin-top: 4rpx; }
.ov-divider { width: 1rpx; height: 60rpx; background: rgba(255,255,255,0.3); }
.progress-bar { height: 16rpx; background: rgba(255,255,255,0.3); border-radius: 8rpx; overflow: hidden; margin-top: 24rpx; }
.progress-fill { height: 100%; background: #fff; border-radius: 8rpx; transition: width 0.3s; }

.stats-row { display: flex; margin: 0 24rpx; gap: 16rpx; }
.stat {
  flex: 1; background: #fff; border-radius: 16rpx; padding: 20rpx;
  display: flex; flex-direction: column; align-items: center;
}
.stat-num { font-size: 36rpx; font-weight: 700; }
.stat-lab { font-size: 22rpx; color: #999; margin-top: 4rpx; }
.stat.success .stat-num { color: #27ae60; }
.stat.fail .stat-num { color: #e74c3c; }
.stat.pending .stat-num { color: #999; }

.period-list { padding: 0 24rpx; margin-top: 24rpx; }
.period-title { font-size: 26rpx; color: #666; margin-bottom: 16rpx; }
.period-card {
  background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx;
  display: flex; justify-content: space-between; align-items: center;
}
.p-no { font-size: 26rpx; font-weight: 600; color: #333; }
.p-amount { font-size: 22rpx; color: #999; margin-top: 4rpx; }
.p-date { font-size: 20rpx; color: #bbb; margin-top: 4rpx; }
.p-right { display: flex; flex-direction: column; align-items: flex-end; gap: 12rpx; }
.p-status {
  font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 16rpx;
}
.p-status.success { background: #e8f7ef; color: #27ae60; }
.p-status.failed { background: #fdeaea; color: #e74c3c; }
.p-status.pending { background: #f5f6f8; color: #999; }
.p-actions { display: flex; gap: 12rpx; }
.p-btn {
  font-size: 22rpx; padding: 6rpx 20rpx; border-radius: 20rpx; color: #fff;
}
.p-btn.ok { background: var(--primary-strong); }
.p-btn.no { background: #e74c3c; }
</style>
