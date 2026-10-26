<template>
  <view class="sk">
    <view class="sk-row" v-for="i in rows" :key="i">
      <view class="sk-avatar"></view>
      <view class="sk-lines">
        <view class="sk-line w60"></view>
        <view class="sk-line w30"></view>
      </view>
      <view class="sk-amount"></view>
    </view>
  </view>
</template>

<script>
/**
 * 列表骨架屏：首屏加载时占位，避免"白屏 + 加载中"的廉价感。
 * 只做视觉占位，不参与任何业务逻辑。
 */
export default {
  name: 'SkeletonList',
  props: {
    rows: { type: Number, default: 3 }
  }
};
</script>

<style scoped>
.sk { padding: 8rpx 32rpx; }
.sk-row {
  display: flex; align-items: center;
  padding: 24rpx 0; border-bottom: 1rpx solid var(--border);
  animation: sk-pulse 1.2s ease-in-out infinite;
}
.sk-row:last-child { border-bottom: none; }
.sk-avatar { width: 56rpx; height: 56rpx; border-radius: 16rpx; background: var(--surface); flex-shrink: 0; }
.sk-lines { flex: 1; margin-left: 20rpx; }
.sk-line { height: 22rpx; border-radius: 11rpx; background: var(--surface); margin-bottom: 10rpx; }
.sk-line:last-child { margin-bottom: 0; }
.w60 { width: 60%; }
.w30 { width: 30%; }
.sk-amount { width: 110rpx; height: 28rpx; border-radius: 14rpx; background: var(--surface); flex-shrink: 0; }
@keyframes sk-pulse {
  0% { opacity: 1; }
  50% { opacity: 0.45; }
  100% { opacity: 1; }
}
</style>
