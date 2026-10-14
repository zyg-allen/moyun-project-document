<template>
  <view class="navbar" :style="themeVars">
    <view class="navbar-inner">
      <view class="nav-back" @tap="goBack">
        <text class="nav-back-icon">‹</text>
      </view>
      <text class="nav-title">{{ title }}</text>
      <view class="nav-right">
        <slot />
      </view>
    </view>
  </view>
</template>

<script>
import { useThemeStore } from '@/stores/theme';

export default {
  name: 'NavBar',
  props: {
    title: { type: String, default: '' }
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  methods: {
    goBack() {
      const pages = getCurrentPages();
      if (pages.length > 1) {
        uni.navigateBack();
      } else {
        uni.switchTab({ url: '/pages/dashboard/index' });
      }
    }
  }
};
</script>

<style scoped>
.navbar {
  background: var(--card);
  /* 小程序：--status-bar-height 固定 25px（沉浸式下必须留出，否则内容顶进状态栏）；
     H5：该变量为 0，行为与改造前一致 */
  padding-top: var(--status-bar-height);
  border-bottom: 1rpx solid #f0f0f5;
  position: sticky;
  top: 0;
  z-index: 20;
}
.navbar-inner {
  display: flex;
  align-items: center;
  height: 88rpx;
  padding: 0 24rpx;
}
.nav-back {
  /* 热区 88rpx(=44px)：原 64rpx(32px) 偏小，且每个二级页都有此键 */
  min-width: 88rpx; min-height: 88rpx;
  display: flex; align-items: center; justify-content: center;
  width: 64rpx; height: 64rpx; border-radius: 32rpx;
  display: flex; align-items: center; justify-content: center;
  background: var(--primary-soft);
}
.nav-back-icon {
  font-size: 44rpx; color: var(--primary-strong);
  margin-top: -6rpx; line-height: 1;
}
.nav-title {
  flex: 1; text-align: center;
  font-size: 32rpx; font-weight: 600; color: var(--text-main);
  /* 抵消右侧占位，保证视觉居中 */
  margin-right: 64rpx;
}
.nav-right { display: flex; align-items: center; }
</style>
