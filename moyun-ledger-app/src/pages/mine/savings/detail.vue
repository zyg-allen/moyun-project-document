<template>
  <view class="page" :style="themeVars">
    <!-- NavBar 必须在 v-if 之外：原先整页（含导航栏）都在 v-if="plan" 内，
         加载中或加载失败时页面全白且**没有任何返回入口**，用户会被卡住 -->
    <NavBar :title="plan ? plan.name : '计划详情'" />

    <template v-if="plan">

    <!-- 概览 -->
    <view class="overview">
      <view class="ov-row">
        <view class="ov-item">
          <text class="ov-val">¥{{ toFixedYuan(plan.currentAmount) }}</text>
          <text class="ov-lab">已存入</text>
        </view>
        <view class="ov-divider"></view>
        <view class="ov-item">
          <text class="ov-val">¥{{ toFixedYuan(plan.targetAmount) }}</text>
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
      <view class="period-title">存钱记录（{{ records.length }}期）</view>
      <view class="period-card" v-for="r in records" :key="r.id">
        <view class="p-left">
          <view class="p-no">第{{ r.periodIndex }}期</view>
          <view class="p-amount">目标 ¥{{ toFixedYuan(r.targetAmount) }}</view>
          <view class="p-date" v-if="r.recordDate">{{ r.recordDate }}</view>
          <view class="p-reason" v-if="r.status === 2 && r.failReason">{{ r.failReason }}</view>
        </view>
        <view class="p-right">
          <view class="p-status" :class="statusClass(r.status)">
            {{ statusText(r.status) }}
          </view>
          <view class="p-actions" v-if="r.status === 0">
            <view class="p-btn ok" @tap="deposit(r)">存入</view>
            <view class="p-btn no" @tap="fail(r)">放弃</view>
          </view>
          <view class="p-actual" v-if="r.status === 1">实存 ¥{{ toFixedYuan(r.amount) }}</view>
        </view>
      </view>
    </view>

    <!-- 删除计划 -->
    <view class="delete-btn" @tap="delPlan">删除计划</view>
    </template>

    <!-- 加载中 / 加载失败占位：有 NavBar 兜底，用户始终能返回 -->
    <view v-else class="detail-placeholder">{{ loadFailed ? '计划不存在或已删除' : '加载中…' }}</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { getSavingPlan, depositSavingRecord, failSavingRecord, deleteSavingPlan } from '@/api/ledger';
import { toFixedYuan } from '@/utils/money';

export default {
  components: { NavBar },
  data() {
    return { planId: null, plan: null, records: [], loadFailed: false };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    progressPercent() {
      if (!this.plan || !this.plan.targetAmount) return 0;
      return Math.min(100, Math.round((this.plan.currentAmount / this.plan.targetAmount) * 100));
    },
    successCount() { return this.records.filter(r => r.status === 1).length; },
    failCount() { return this.records.filter(r => r.status === 2).length; },
    pendingCount() { return this.records.filter(r => r.status === 0).length; }
  },
  onLoad(options) {
    this.planId = options.id;
  },
  onShow() {
    useThemeStore().restore();
    if (this.planId) this.load();
  },
  methods: {
    toFixedYuan,
    statusText(s) { return s === 1 ? '成功' : (s === 2 ? '失败' : '待存'); },
    statusClass(s) { return s === 1 ? 'success' : (s === 2 ? 'failed' : 'pending'); },
    async load() {
      try {
        const data = await getSavingPlan(this.planId) || {};
        this.plan = data.plan || null;
        this.records = data.records || [];
        // 接口成功但没返回 plan（如已被删除）也算失败，避免永远停在"加载中…"
        this.loadFailed = !this.plan;
      } catch (e) {
        this.loadFailed = true;
        /* 拦截器已提示 */
      }
    },
    deposit(r) {
      uni.showModal({
        title: '存入第' + r.periodIndex + '期',
        content: '按目标金额 ¥' + toFixedYuan(r.targetAmount) + ' 存入？',
        editable: true,
        placeholderText: '可输入实际存入金额（留空按目标金额）',
        success: async (res) => {
          if (!res.confirm) return;
          const actual = res.content ? parseFloat(res.content) : null;
          try {
            await depositSavingRecord(this.planId, r.id, actual && actual > 0 ? actual : null);
            uni.showToast({ title: '存入成功', icon: 'success' });
            this.load();
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    fail(r) {
      uni.showModal({
        title: '放弃第' + r.periodIndex + '期',
        content: '标记为本期失败（记录失败原因，可后续补存）？',
        editable: true,
        placeholderText: '失败原因（留空=主动放弃）',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await failSavingRecord(this.planId, r.id, res.content || null);
            this.load();
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    delPlan() {
      uni.showModal({
        title: '删除计划',
        content: '删除后不可恢复（期次记录保留），确定删除？',
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await deleteSavingPlan(this.planId);
            uni.showToast({ title: '已删除', icon: 'success' });
            setTimeout(() => uni.navigateBack(), 600);
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 60rpx; min-height: 100vh; background: var(--surface); }
.overview {
  background: var(--card); margin: 24rpx; border-radius: 20rpx; padding: 32rpx;
}
.ov-row { display: flex; align-items: center; margin-bottom: 24rpx; }
.ov-item { flex: 1; display: flex; flex-direction: column; align-items: center; }
.ov-val { font-size: 32rpx; font-weight: 700; color: var(--primary-strong); }
.ov-lab { font-size: 22rpx; color: var(--text-sub); margin-top: 8rpx; }
.ov-divider { width: 1rpx; height: 48rpx; background: var(--surface); }
.progress-bar { height: 16rpx; background: var(--surface); border-radius: 8rpx; overflow: hidden; }
.progress-fill { height: 100%; background: var(--primary-strong); border-radius: 8rpx; transition: width 0.3s; }

.stats-row { display: flex; gap: 20rpx; margin: 0 24rpx 24rpx; }
.stat {
  flex: 1; background: var(--card); border-radius: 16rpx; padding: 24rpx;
  display: flex; flex-direction: column; align-items: center;
}
.stat-num { font-size: 36rpx; font-weight: 700; }
.stat-lab { font-size: 22rpx; color: var(--text-sub); margin-top: 4rpx; }
.stat.success .stat-num { color: var(--income); }
.stat.fail .stat-num { color: var(--expense); }
.stat.pending .stat-num { color: var(--warning); }

.period-list { margin: 0 24rpx; }
.period-title { font-size: 28rpx; font-weight: 600; color: var(--text-main); margin-bottom: 16rpx; }
.period-card {
  background: var(--card); border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx;
  display: flex; align-items: center; justify-content: space-between;
}
.p-no { font-size: 26rpx; font-weight: 600; color: var(--text-main); }
.p-amount { font-size: 24rpx; color: var(--text-sub); margin-top: 6rpx; }
.p-date { font-size: 20rpx; color: var(--text-muted); margin-top: 4rpx; }
.p-reason { font-size: 20rpx; color: var(--expense); margin-top: 4rpx; }
.p-right { display: flex; flex-direction: column; align-items: flex-end; }
.p-status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 16rpx; }
.p-status.success { color: var(--income); background: var(--income-bg); }
.p-status.failed { color: var(--expense); background: var(--expense-bg); }
.p-status.pending { color: var(--warning); background: var(--surface); }
.p-actions { display: flex; gap: 12rpx; margin-top: 12rpx; }
.p-btn { font-size: 22rpx; padding: 6rpx 20rpx; border-radius: 20rpx; }
.p-btn.ok { background: var(--primary-strong); color: #fff; }
.p-btn.no { background: var(--expense-bg); color: var(--expense); }
.p-actual { font-size: 20rpx; color: var(--text-sub); margin-top: 8rpx; }

.delete-btn {
  margin: 40rpx 24rpx 0; text-align: center; padding: 24rpx;
  background: var(--card); border-radius: 16rpx; color: var(--expense); font-size: 28rpx;
}
/* 加载中 / 加载失败占位（与 NavBar 一起保证页面不会全白无出口） */
.detail-placeholder {
  padding: 160rpx 0; text-align: center;
  color: var(--text-muted); font-size: 28rpx;
}
</style>