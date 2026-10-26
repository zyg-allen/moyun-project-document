<template>
  <view v-if="visible" class="picker-mask" @tap="close">
    <view class="picker-sheet" @tap.stop>
      <view class="picker-head">
        <text class="picker-cancel" @tap="close">取消</text>
        <text class="picker-title">{{ title }}</text>
        <text class="picker-done" @tap="close">完成</text>
      </view>
      <scroll-view scroll-y class="picker-body">
        <view
          v-for="(label, i) in options"
          :key="label + i"
          class="picker-item"
          :class="{ on: i === selected }"
          @tap="pick(i)"
        >
          <text>{{ label }}</text>
          <text v-if="i === selected" class="picker-check">✓</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
/**
 * 主题化单选面板：替代原生 picker（原生下拉在暗色下仍是系统白底，无法跟随主题）。
 * 用法：<PickerSheet :visible="show" title="身份标签" :options="labels" :selected="idx"
 *                    @close="show=false" @select="onSelect" />
 */
export default {
  name: 'PickerSheet',
  props: {
    visible: { type: Boolean, default: false },
    title: { type: String, default: '请选择' },
    options: { type: Array, default: () => [] },
    selected: { type: Number, default: -1 }
  },
  methods: {
    close() { this.$emit('close'); },
    pick(i) {
      this.$emit('select', i);
      this.close();
    }
  }
};
</script>

<style scoped>
.picker-mask {
  position: fixed; top: 0; right: 0; bottom: 0; left: 0;
  background: rgba(0, 0, 0, 0.45); z-index: 1000;
  display: flex; align-items: flex-end;
}
.picker-sheet {
  width: 100%; max-height: 76vh; display: flex; flex-direction: column;
  background: var(--card); border-radius: 32rpx 32rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
}
.picker-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24rpx 32rpx; border-bottom: 1rpx solid var(--border);
}
.picker-title { font-size: 30rpx; font-weight: 600; color: var(--text-main); }
.picker-cancel { font-size: 26rpx; color: var(--text-sub); }
.picker-done { font-size: 26rpx; color: var(--primary-text); font-weight: 600; }
.picker-body { max-height: 60vh; }
.picker-item {
  display: flex; align-items: center; justify-content: center; gap: 12rpx;
  padding: 28rpx 32rpx; font-size: 30rpx; color: var(--text-main);
  border-bottom: 1rpx solid var(--border);
}
.picker-item:last-child { border-bottom: none; }
.picker-item.on { color: var(--primary-text); font-weight: 600; background: var(--primary-soft); }
.picker-check { font-size: 26rpx; }
@media (min-width: 768px) {
  .picker-sheet { width: 560px; margin: 0 auto; border-radius: 24rpx 24rpx 0 0; }
}
</style>
