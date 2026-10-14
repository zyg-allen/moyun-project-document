<template>
  <view v-if="modelValue" class="kas-mask" :style="maskStyle" @tap="onMaskTap">
    <view class="kas-sheet" @tap.stop>
      <!-- 标题栏（可选关闭按钮） -->
      <view v-if="title || closable" class="kas-head">
        <text v-if="title" class="kas-title">{{ title }}</text>
        <text v-if="closable" class="kas-close" @tap="close">✕</text>
      </view>

      <!-- 内容区：可滚动，且为键盘留出底部空间 -->
      <scroll-view
        class="kas-body"
        :style="bodyStyle"
        scroll-y
        :show-scrollbar="false"
        :scroll-top="scrollTop"
      >
        <slot />
        <!-- 键盘占位（仅 H5 生效；小程序由 adjust-position 自动上推） -->
        <view v-if="kbHeight > 0" :style="{ height: kbHeight + 'px' }" />
      </scroll-view>
    </view>
  </view>
</template>

<script>
import keyboardAware from '@/mixins/keyboardAware';

/**
 * 键盘安全底部弹层（uni-app H5 / 小程序 / PC 通用）
 *
 * 解决的痛点：手机端弹出系统键盘后，底部弹层里的输入框被键盘遮挡，
 * 用户看不到自己在输入什么（portfolio 新增资产/负债弹窗即为此问题）。
 *
 * 跨端策略（键盘高度探测统一由 `@/mixins/keyboardAware` 提供）：
 *  - **H5**：键盘弹出时给遮罩注入 padding-bottom 抬升，并收紧内容区高度保证可滚动；
 *  - **小程序**：键盘不改变 window 尺寸，本组件 kbHeight 恒为 0，
 *           改由输入框自身 `adjust-position` 自动上推（两者互补，不冲突）；
 *  - **PC / H5 无键盘**：行为与普通底部弹层完全一致。
 *
 * 使用方注意：内容里的 `<input>` 建议加 `adjust-position` 与 `:cursor-spacing="24"`。
 */
export default {
  name: 'KeyboardAwareSheet',
  mixins: [keyboardAware],
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
    /** 是否显示右上角关闭按钮 */
    closable: { type: Boolean, default: false },
    /** 内容区最大高度（vh），防止超长表单溢出屏幕 */
    maxHeightVh: { type: Number, default: 62 },
    /** 点击遮罩是否关闭 */
    maskClosable: { type: Boolean, default: true }
  },
  emits: ['update:modelValue', 'close'],
  data() {
    return {
      scrollTop: 0
    };
  },
  computed: {
    /** 键盘弹出时把整个弹层抬到键盘上方 */
    maskStyle() {
      return this.kbHeight > 0 ? { paddingBottom: this.kbHeight + 'px' } : {};
    },
    /** 键盘占位已在内容里，这里再收紧最大高度，保证内容可滚动 */
    bodyStyle() {
      const vh = this.kbHeight > 0 ? Math.min(this.maxHeightVh, 40) : this.maxHeightVh;
      return { maxHeight: vh + 'vh' };
    }
  },
  watch: {
    modelValue(val) {
      // 关闭时复位键盘高度，避免下次打开残留偏移
      if (!val) this.kbHeight = 0;
      // 打开时重置滚动位置
      if (val) this.scrollTop = 0;
    }
  },
  methods: {
    close() {
      this.$emit('update:modelValue', false);
      this.$emit('close');
    },
    onMaskTap() {
      if (this.maskClosable) this.close();
    }
  }
};
</script>

<style scoped>
.kas-mask {
  position: fixed;
  /* 用四边写法而非 inset 简写：inset 需 Safari ≥14.1 / Chrome ≥87，
     旧 WKWebView 下不生效会导致遮罩塌缩成内容宽 */
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  /* 必须高于 H5 底部 TabBar（uni-tabbar z-index:998）；
     与 uni-app 官方弹窗/picker（999）同级，使 picker 后渲染时自然盖在上层 */
  z-index: 999;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  /* padding-bottom 由 JS 按键盘高度动态注入 */
  transition: padding-bottom 0.18s ease;
}
.kas-sheet {
  width: 100%;
  background: var(--card, #fff);
  border-radius: 32rpx 32rpx 0 0;
  padding: 32rpx 32rpx calc(32rpx + env(safe-area-inset-bottom));
  display: flex;
  flex-direction: column;
}
.kas-head {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
}
.kas-title {
  font-size: 34rpx;
  font-weight: 600;
  color: var(--text-main, #1a1d26);
}
.kas-close {
  position: absolute;
  right: -12rpx;
  top: -20rpx;
  /* 热区 88rpx(=44px) 达到移动端可点最小尺寸，避免点不中 */
  width: 88rpx;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 28rpx;
  color: var(--text-sub, #6b7280);
  background: var(--surface, #f0f2f6);
}
</style>
