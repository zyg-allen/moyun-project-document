<template>
  <view class="stepper">
    <view class="step-btn" :class="{ dis: num <= min }" @tap.stop="changeStep(-1)">
      <text class="step-sign">−</text>
    </view>
    <input
      class="step-input"
      type="number"
      :value="display"
      :placeholder="placeholder"
      @input="onInput"
      @blur="onBlur"
      @confirm="onBlur"
    />
    <text v-if="unit" class="step-unit">{{ unit }}</text>
    <view class="step-btn" :class="{ dis: max !== null && max !== undefined && num >= max }" @tap.stop="changeStep(1)">
      <text class="step-sign">＋</text>
    </view>
  </view>
</template>

<script>
/**
 * 数量步进器：− [ 10 ] 件 ＋
 *
 * 适合件 / 个 / 斤 这类可以按 1 递增的定额数量。
 * 直接用键盘输入也支持（失焦时把非法值修正到合法区间）。
 */
export default {
  name: 'NumberStepper',
  props: {
    modelValue: { type: [Number, String], default: '' },
    min: { type: Number, default: 1 },
    max: { type: Number, default: null },
    step: { type: Number, default: 1 },
    unit: { type: String, default: '' },
    placeholder: { type: String, default: '0' }
  },
  computed: {
    num() {
      const n = Number(this.modelValue);
      return isNaN(n) ? 0 : n;
    },
    display() {
      const v = this.modelValue;
      return v === '' || v === null || v === undefined ? '' : String(v);
    }
  },
  methods: {
    clamp(n) {
      let v = Math.round(n);
      if (isNaN(v)) v = this.min;
      if (v < this.min) v = this.min;
      if (this.max !== null && this.max !== undefined && v > this.max) v = this.max;
      return v;
    },
    emit(v) {
      this.$emit('update:modelValue', v);
      this.$emit('change', v);
    },
    changeStep(dir) {
      const base = this.num || this.min;
      const next = this.clamp(base + dir * this.step);
      // 只在真的变化时才发，避免边界处空转
      if (next !== this.num) this.emit(next);
    },
    onInput(e) {
      const raw = e && e.detail ? e.detail.value : '';
      this.$emit('update:modelValue', raw === '' ? '' : Number(raw));
    },
    onBlur() {
      // 空值或非法值 → 收敛到合法区间
      const v = this.clamp(this.num);
      if (String(v) !== String(this.modelValue)) this.emit(v);
    }
  }
};
</script>

<style scoped>
.stepper {
  display: flex; align-items: center; gap: 10rpx;
}
.step-btn {
  width: 72rpx; height: 72rpx; flex: 0 0 auto;
  display: flex; align-items: center; justify-content: center;
  border-radius: 10rpx;
  border: 1rpx solid var(--border);
  background: var(--bg);
}
.step-btn.dis { opacity: .35; }
.step-sign { font-size: 34rpx; line-height: 1; color: var(--text-main); }
.step-input {
  flex: 1; min-width: 0; text-align: center;
  font-size: 44rpx; font-weight: 700; color: var(--text-main);
}
.step-unit { font-size: 22rpx; color: var(--text-sub); flex: 0 0 auto; }
</style>
