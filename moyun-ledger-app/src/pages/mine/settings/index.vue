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

    <!-- 外观模式（日间/暗色/护眼）：注入 --bg/--card/--text-* 等外观层变量 -->
    <view class="card">
      <view class="setting-title">外观模式</view>
      <view class="setting-sub">日间 / 暗色 / 护眼，立即生效并记住选择</view>
      <view class="appearance-grid">
        <view
          v-for="a in appearanceList"
          :key="a.key"
          class="appearance-cell"
          :class="{ selected: appearanceKey === a.key }"
          @tap="pickAppearance(a.key)"
        >
          <view class="appearance-swatch" :class="'sw-' + a.key">
            <text v-if="appearanceKey === a.key" class="appearance-check">✓</text>
          </view>
          <text class="appearance-name" :class="{ selected: appearanceKey === a.key }">{{ a.name }}</text>
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
      <view class="about">{{ appVersionText }}</view>
      <view class="about-sub">个人资产管理 · 一个数字看清全部身家</view>
      <view class="about-sub">数据与墨韵门户账号云端同步</view>
    </view>
  </view>
</template>

<script>
import { useThemeStore } from '@/stores/theme';
import { APP_VERSION_TEXT } from '@/utils/appInfo';

export default {
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    themeList() { return useThemeStore().themeList; },
    appearanceList() { return useThemeStore().appearanceList; },
    appearanceKey() { return useThemeStore().appearanceKey; },
    current() { return useThemeStore().themeKey; },
    /** 版本号统一来自 utils/appInfo.js（此前这里与「我的」页各写一份且不一致） */
    appVersionText() { return APP_VERSION_TEXT; }
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
    /** 切换外观模式（日间/暗色/护眼）；页面根元素由 themeVars 自动跟随 */
    pickAppearance(key) {
      useThemeStore().setAppearance(key);
      uni.showToast({ title: '已切换外观', icon: 'none' });
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
.card { background: var(--card); border-radius: 20rpx; padding: 32rpx; margin-bottom: 24rpx; }
.setting-title { font-size: 30rpx; font-weight: 600; }
.setting-sub { font-size: 24rpx; color: var(--text-sub); margin-top: 8rpx; }
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
/* 打勾位于「主题色圆点」上（底色即强调色）→ 用 --on-primary 深色勾，白勾在浅强调色上仅 2.1~3.8:1 */
.theme-check { color: var(--on-primary); font-size: 36rpx; font-weight: 700; }
.theme-name { font-size: 22rpx; color: var(--text-sub); margin-top: 12rpx; }
.theme-name.selected { color: var(--primary-text); font-weight: 600; }
.about { text-align: center; font-size: 30rpx; font-weight: 600; padding: 24rpx 0 12rpx; }
.about-sub { text-align: center; font-size: 22rpx; color: var(--text-sub); padding: 4rpx 0; }

/* ---------- 外观模式选择（日间/暗色/护眼） ---------- */
.appearance-grid { display: flex; gap: 24rpx; margin-top: 24rpx; }
.appearance-cell {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 12rpx;
  /* 热区 88rpx 以上，避免点不中 */
  min-height: 88rpx; justify-content: center;
}
.appearance-swatch {
  width: 96rpx; height: 96rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  border: 3rpx solid var(--border);
  position: relative;
}
.appearance-cell.selected .appearance-swatch { border-color: var(--primary-strong); }
/* 用真实外观色做预览，用户一眼可辨 */
.appearance-swatch.sw-light { background: #F7F8FA; }
.appearance-swatch.sw-dark { background: #0F1419; }
.appearance-swatch.sw-eye { background: #F4F1EA; }
.appearance-check { font-size: 40rpx; font-weight: 700; color: var(--primary-text); }
.appearance-swatch.sw-dark .appearance-check { color: #5B8DEF; }
.appearance-name { font-size: 24rpx; color: var(--text-sub); }
.appearance-name.selected { color: var(--primary-text); font-weight: 600; }
</style>
