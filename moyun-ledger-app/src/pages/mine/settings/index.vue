<template>
  <view class="page" :style="themeVars">
    <NavBar title="设置" />
    <!-- 主题切换 -->
    <view class="card">
      <view class="setting-title">主题风格</view>
      <view class="setting-sub">切换全局配色，立即生效并记住选择</view>
      <view class="theme-grid">
        <view v-for="t in themeList" :key="t.key" class="theme-cell" @tap="pickTheme(t.key)">
          <view class="theme-dot" :class="{ selected: current === t.key }" :style="{ background: t.color }">
            <text v-if="current === t.key" class="theme-check">✓</text>
          </view>
          <text class="theme-name" :class="{ selected: current === t.key }">{{ t.name }}</text>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="setting-row">
        <view class="flex-1">
          <view class="setting-name">隐私模式</view>
          <view class="setting-sub">首页与列表中金额显示为 ****</view>
        </view>
        <switch :checked="privacy" @change="onPrivacyChange" style="transform: scale(0.85)" />
      </view>
    </view>

    <view class="card">
      <view class="about">墨韵记账 v1.0.0</view>
      <view class="about-sub">个人资产管理 · 一个数字看清全部身家</view>
      <view class="about-sub">数据与墨韵门户账号云端同步</view>
    </view>
  </view>
</template>

<script>
import { useThemeStore } from '@/stores/theme';

export default {
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    themeList() { return useThemeStore().themeList; },
    current() { return useThemeStore().themeKey; }
  },
  data() {
    return { privacy: false };
  },
  onShow() {
    this.privacy = uni.getStorageSync('ledger_privacy') === '1';
  },
  methods: {
    pickTheme(key) {
      useThemeStore().setTheme(key);
      uni.showToast({ title: '已切换主题', icon: 'none' });
    },
    onPrivacyChange(e) {
      this.privacy = e.detail.value;
      uni.setStorageSync('ledger_privacy', e.detail.value ? '1' : '0');
      uni.showToast({ title: e.detail.value ? '已开启' : '已关闭', icon: 'none' });
    }
  }
};
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 32rpx; margin-bottom: 24rpx; }
.setting-title { font-size: 30rpx; font-weight: 600; }
.setting-sub { font-size: 24rpx; color: #999; margin-top: 8rpx; }
.setting-row { display: flex; align-items: center; padding: 8rpx 0; }
.setting-name { font-size: 30rpx; font-weight: 500; }
.theme-grid { display: flex; flex-wrap: wrap; margin-top: 28rpx; }
.theme-cell { width: 20%; display: flex; flex-direction: column; align-items: center; }
.theme-dot {
  width: 84rpx; height: 84rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.12);
}
.theme-dot.selected { border: 6rpx solid #fff; outline: 3rpx solid var(--primary-strong); }
.theme-check { color: #fff; font-size: 36rpx; font-weight: 700; }
.theme-name { font-size: 22rpx; color: #666; margin-top: 12rpx; }
.theme-name.selected { color: var(--primary-strong); font-weight: 600; }
.about { text-align: center; font-size: 30rpx; font-weight: 600; padding: 24rpx 0 12rpx; }
.about-sub { text-align: center; font-size: 22rpx; color: #999; padding: 4rpx 0; }
</style>
