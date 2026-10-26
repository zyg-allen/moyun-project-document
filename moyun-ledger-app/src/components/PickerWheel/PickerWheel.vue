<template>
  <view v-if="visible" class="wheel-mask" @tap="close">
    <view class="wheel-sheet" @tap.stop>
      <view class="wheel-head">
        <text class="wheel-cancel" @tap="close">取消</text>
        <text class="wheel-title">{{ title }}</text>
        <text class="wheel-done" @tap="confirm">确定</text>
      </view>
      <view class="wheel-body">
        <scroll-view
          v-for="(col, ci) in columns"
          :key="ci"
          scroll-y
          class="wheel-col"
          :scroll-into-view="'w' + ci + '_' + (draft[ci] - 1)"
        >
          <view class="wheel-pad"></view>
          <view
            v-for="(item, ii) in col"
            :key="ii"
            :id="'w' + ci + '_' + ii"
            class="wheel-item"
            :class="{ on: draft[ci] === ii }"
            @tap="pick(ci, ii)"
          >{{ item }}</view>
          <view class="wheel-pad"></view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
/**
 * 主题化滚轮选择：替代 <picker mode="date|time"> 的自绘面板。
 * 支持 1~2 列（月/时间），列内点击选择，确定时抛出索引数组。
 */
export default {
  name: 'PickerWheel',
  props: {
    visible: { type: Boolean, default: false },
    title: { type: String, default: '' },
    columns: { type: Array, default: () => [] },
    value: { type: Array, default: () => [] }
  },
  data() {
    return { draft: [] };
  },
  watch: {
    visible(v) {
      if (v) this.draft = this.columns.map((c, i) => (this.value[i] == null ? 0 : this.value[i]));
    }
  },
  methods: {
    close() { this.$emit('close'); },
    pick(ci, ii) { this.$set(this.draft, ci, ii); },
    confirm() { this.$emit('confirm', this.draft.slice()); this.close(); }
  }
};
</script>

<style scoped>
.wheel-mask {
  position: fixed; top: 0; right: 0; bottom: 0; left: 0;
  background: rgba(0, 0, 0, 0.45); z-index: 1000;
  display: flex; align-items: flex-end;
}
.wheel-sheet {
  width: 100%; background: var(--card);
  border-radius: 32rpx 32rpx 0 0; padding-bottom: env(safe-area-inset-bottom);
}
.wheel-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 24rpx 32rpx; border-bottom: 1rpx solid var(--border);
}
.wheel-title { font-size: 30rpx; font-weight: 600; color: var(--text-main); }
.wheel-cancel { font-size: 26rpx; color: var(--text-sub); }
.wheel-done { font-size: 26rpx; color: var(--primary-text); font-weight: 600; }
.wheel-body { display: flex; height: 420rpx; }
.wheel-col { flex: 1; height: 420rpx; }
.wheel-pad { height: 160rpx; }
.wheel-item {
  height: 80rpx; line-height: 80rpx; text-align: center;
  font-size: 32rpx; color: var(--text-main);
}
.wheel-item.on { color: var(--primary-text); font-weight: 700; }
@media (min-width: 768px) {
  .wheel-sheet { width: 560px; margin: 0 auto; border-radius: 24rpx 24rpx 0 0; }
}
</style>
