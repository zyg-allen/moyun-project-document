<template>
  <view class="page" :style="themeVars">
    <NavBar title="权限管理" />

    <view class="card">
      <view class="tip">这些权限只在你主动使用对应功能时才会申请；可随时到系统设置关闭。</view>

      <view class="row" v-for="p in list" :key="p.key">
        <view class="row-icon">{{ p.icon }}</view>
        <view class="flex-1">
          <view class="row-name">{{ p.name }}</view>
          <view class="row-sub">{{ p.desc }}</view>
        </view>
        <view class="row-btn" @tap="openSetting">{{ p.action }}</view>
      </view>
    </view>

    <view class="card">
      <view class="row" @tap="openSetting">
        <view class="flex-1">
          <view class="row-name">打开系统设置</view>
          <view class="row-sub">通知、麦克风、相册、定位均可在此管理</view>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';

export default {
  components: { NavBar },
  data() {
    return {
      list: [
        { key: 'notify', icon: '🔔', name: '通知', desc: '到期提醒、记账结果、系统消息', action: '去开启' },
        { key: 'mic', icon: '🎙️', name: '麦克风', desc: '语音记账、语音搜索', action: '去开启' },
        { key: 'album', icon: '🖼️', name: '相册', desc: '上传凭证、头像、商品图片', action: '去开启' },
        { key: 'location', icon: '📍', name: '位置', desc: '仅用于记录商户位置（可选）', action: '去开启' }
      ]
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() { useThemeStore().restore(); },
  methods: {
    openSetting() {
      // #ifdef MP-WEIXIN
      uni.openSetting({ fail: () => uni.showToast({ title: '请在小程序设置中开启', icon: 'none' }) });
      return;
      // #endif
      // #ifdef APP-PLUS
      uni.showToast({ title: '请在系统设置中开启', icon: 'none' });
      return;
      // #endif
      // #ifdef H5
      if (typeof Notification !== 'undefined') {
        Notification.requestPermission().then((p) => {
          uni.showToast({ title: p === 'granted' ? '通知已开启' : '未开启通知', icon: 'none' });
        });
      } else {
        uni.showToast({ title: '请在浏览器设置中开启通知', icon: 'none' });
      }
      // #endif
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: var(--surface); padding-bottom: 40rpx; }
.card { background: var(--card); margin: 24rpx; border-radius: 20rpx; padding: 8rpx 28rpx; }
.tip { font-size: 23rpx; color: var(--text-muted); line-height: 1.6; padding: 20rpx 0; }
.row { display: flex; align-items: center; gap: 20rpx; padding: 26rpx 0; border-top: 1rpx solid var(--border); }
.row-icon { font-size: 36rpx; }
.flex-1 { flex: 1; min-width: 0; }
.row-name { font-size: 28rpx; color: var(--text-main); font-weight: 500; }
.row-sub { font-size: 22rpx; color: var(--text-sub); margin-top: 6rpx; }
.row-btn { font-size: 23rpx; color: var(--primary-text); background: var(--primary-soft); padding: 8rpx 20rpx; border-radius: 999rpx; }
.arrow { font-size: 32rpx; color: var(--text-muted); }
</style>
