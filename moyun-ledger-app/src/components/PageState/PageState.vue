<template>
  <view class="page-state">
    <!-- 加载：骨架行 -->
    <view v-if="loading" class="skeleton">
      <view class="sk-row" v-for="i in skeletonRows" :key="i">
        <view class="sk-avatar"></view>
        <view class="sk-lines">
          <view class="sk-line w70"></view>
          <view class="sk-line w90"></view>
          <view class="sk-line w40"></view>
        </view>
      </view>
    </view>

    <!-- 失败 -->
    <view v-else-if="error" class="state-body">
      <text class="state-icon">⚠️</text>
      <text class="state-text">{{ errorText }}</text>
      <view class="state-btn" @tap="$emit('retry')">点击重试</view>
    </view>

    <!-- 空 -->
    <view v-else class="state-body">
      <text class="state-icon">{{ emptyIcon }}</text>
      <text class="state-text">{{ emptyText }}</text>
      <view v-if="actionText" class="state-btn" @tap="$emit('action')">{{ actionText }}</view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'PageState',
  props: {
    loading: { type: Boolean, default: false },
    error: { type: Boolean, default: false },
    emptyText: { type: String, default: '暂无数据' },
    emptyIcon: { type: String, default: '📭' },
    errorText: { type: String, default: '加载失败，请重试' },
    actionText: { type: String, default: '' },
    skeletonRows: { type: Number, default: 4 }
  },
  emits: ['retry', 'action']
};
</script>

<style scoped>
.page-state { padding: 24rpx; }
.skeleton { display: flex; flex-direction: column; gap: 20rpx; }
.sk-row { display: flex; gap: 20rpx; background: var(--card); border-radius: 16rpx; padding: 24rpx; }
.sk-avatar { width: 80rpx; height: 80rpx; border-radius: 16rpx; background: var(--surface); flex-shrink: 0; }
.sk-lines { flex: 1; display: flex; flex-direction: column; gap: 12rpx; }
.sk-line { height: 22rpx; border-radius: 8rpx; background: var(--surface); }
.w70 { width: 70%; }
.w90 { width: 90%; }
.w40 { width: 40%; }
.state-body { padding: 120rpx 0; display: flex; flex-direction: column; align-items: center; gap: 14rpx; }
.state-icon { font-size: 64rpx; }
.state-text { font-size: 26rpx; color: var(--text-muted); }
.state-btn {
  margin-top: 12rpx; padding: 10rpx 34rpx; border-radius: 999rpx;
  background: var(--primary-soft); color: var(--primary-text); font-size: 24rpx;
}
</style>
