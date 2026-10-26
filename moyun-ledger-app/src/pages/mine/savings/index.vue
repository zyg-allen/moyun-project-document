<template>
  <view class="page" :style="themeVars">
    <NavBar title="存钱">
      <view class="nav-add" @tap="goCreate">新建计划</view>
    </NavBar>

    <!-- 总览卡片 -->
    <view class="overview-card">
      <view class="ov-label">剩余需存(元)</view>
      <view class="ov-amount">{{ toFixedYuan(overview.totalRemaining) }} 元</view>
      <view class="ov-sub">
        <text>累计存入 {{ toFixedYuan(overview.totalSaved) }} 元</text>
        <text>目标金额 {{ toFixedYuan(overview.totalTarget) }} 元</text>
      </view>
    </view>

    <view class="section-title">我的存钱计划</view>

    <!-- 计划列表 -->
    <view class="plan-list" v-if="plans.length">
      <view class="plan-card" v-for="p in plans" :key="p.id" @tap="goDetail(p.id)">
        <view class="plan-header">
          <text class="plan-name">{{ p.name }}</text>
          <text class="plan-method">{{ methodLabel(p.method) }}</text>
        </view>
        <view class="plan-amount">{{ toFixedYuan(p.targetAmount) }} 元</view>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: progressPercent(p) + '%' }"></view>
        </view>
        <view class="plan-footer">
          <text>已存入：{{ toFixedYuan(p.currentAmount) }} 元</text>
          <text :class="['plan-status', statusClass(p.status)]">{{ statusText(p.status) }}</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty" v-else>
      <text class="empty-icon">🏦</text>
      <text class="empty-text">暂无存钱计划</text>
      <view class="empty-btn" @tap="goCreate">立即创建</view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { listSavingPlans } from '@/api/ledger';
import { toFixedYuan } from '@/utils/money';
import { loadDicts, asLabelMap } from '@/utils/ledgerDict';

const METHOD_LABELS = {
  '52week': '52周存钱法',
  fixed: '固定金额',
  monthly: '每月固定存',
  custom: '自定义递增'
};
const STATUS_TEXT = { 1: '进行中', 2: '已达成', 3: '已失败' };

export default {
  components: { NavBar },
  data() {
    return {
      plans: [],
      methodLabels: { ...METHOD_LABELS },
      overview: { totalTarget: 0, totalSaved: 0, totalRemaining: 0 }
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() {
    useThemeStore().restore();
    this.loadSavingDict();
    if (useUserStore().isLoggedIn) this.load();
  },
  methods: {
    /** 存钱方式标签走后台字典（失败保留内置） */
    async loadSavingDict() {
      const d = await loadDicts(['ledger_saving_method']);
      this.methodLabels = asLabelMap(d.ledger_saving_method, METHOD_LABELS);
    },
    toFixedYuan,
    methodLabel(m) { return this.methodLabels[m] || m; },
    statusText(s) { return STATUS_TEXT[s] || ''; },
    statusClass(s) { return s === 2 ? 'ok' : (s === 3 ? 'fail' : ''); },
    progressPercent(p) {
      if (!p.targetAmount) return 0;
      return Math.min(100, Math.round((p.currentAmount / p.targetAmount) * 100));
    },
    async load() {
      try {
        const data = await listSavingPlans() || {};
        this.plans = data.plans || [];
        this.overview = {
          totalTarget: data.totalTarget || 0,
          totalSaved: data.totalSaved || 0,
          totalRemaining: data.totalRemaining || 0
        };
      } catch (e) { /* 拦截器已提示 */ }
    },
    goCreate() { uni.navigateTo({ url: '/pages/mine/savings/create' }); },
    goDetail(id) { uni.navigateTo({ url: '/pages/mine/savings/detail?id=' + id }); }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; min-height: 100vh; background: var(--surface); }
.nav-add { font-size: 26rpx; color: var(--primary-text); padding: 8rpx 20rpx; min-height: 88rpx; display: flex; align-items: center; }

/* 总览卡：与头部统一为「白卡」语言（原为强调色底 + 白字，白字对比度不足），
   沿用资产负债汇总卡与记一笔金额卡的做法：中性卡面 + 左侧强调色竖条点出主次 */
.overview-card {
  background: var(--card); color: var(--text-main);
  margin: 24rpx; border-radius: 20rpx; padding: 32rpx;
  border-left: 8rpx solid var(--primary-strong);
}
.ov-label { font-size: 24rpx; color: var(--text-sub); }
.ov-amount { font-size: 52rpx; font-weight: 700; margin: 12rpx 0; color: var(--text-main); }
.ov-sub { display: flex; justify-content: space-between; font-size: 22rpx; color: var(--text-sub); }

.section-title { font-size: 28rpx; font-weight: 600; color: var(--text-main); padding: 8rpx 32rpx 16rpx; }

.plan-list { padding: 0 24rpx; }
.plan-card {
  background: var(--card); border-radius: 20rpx; padding: 28rpx; margin-bottom: 20rpx;
  border-left: 8rpx solid var(--primary-strong);
}
.plan-header { display: flex; justify-content: space-between; align-items: center; }
.plan-name { font-size: 28rpx; font-weight: 600; color: var(--text-main); }
.plan-method { font-size: 22rpx; color: var(--primary-text); background: var(--primary-soft); padding: 4rpx 16rpx; border-radius: 16rpx; }
.plan-amount { font-size: 36rpx; font-weight: 700; color: var(--primary-text); margin: 16rpx 0 12rpx; }
.progress-bar { height: 16rpx; background: var(--surface); border-radius: 8rpx; overflow: hidden; }
.progress-fill { height: 100%; background: var(--primary-strong); border-radius: 8rpx; transition: width 0.3s; }
.plan-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 12rpx; font-size: 22rpx; color: var(--text-sub); }
.plan-status.ok { color: var(--income); }
.plan-status.fail { color: var(--expense); }

.empty { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; }
.empty-icon { font-size: 100rpx; margin-bottom: 24rpx; }
.empty-text { font-size: 28rpx; color: var(--text-sub); margin-bottom: 32rpx; }
.empty-btn {
  padding: 16rpx 48rpx; background: var(--primary-strong); color: var(--on-primary);
  border-radius: 32rpx; font-size: 26rpx;
}
</style>
