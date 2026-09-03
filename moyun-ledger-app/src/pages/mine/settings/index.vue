<template>
  <view class="page">
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
export default {
  data() {
    return { privacy: false };
  },
  onShow() {
    this.privacy = uni.getStorageSync('ledger_privacy') === '1';
  },
  methods: {
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
.card { background: #fff; border-radius: 20rpx; padding: 8rpx 32rpx; margin-bottom: 24rpx; }
.setting-row { display: flex; align-items: center; padding: 32rpx 0; }
.setting-name { font-size: 30rpx; font-weight: 500; }
.setting-sub { font-size: 24rpx; color: #999; margin-top: 8rpx; }
.about { text-align: center; font-size: 30rpx; font-weight: 600; padding: 24rpx 0 12rpx; }
.about-sub { text-align: center; font-size: 22rpx; color: #999; padding: 4rpx 0; }
</style>
