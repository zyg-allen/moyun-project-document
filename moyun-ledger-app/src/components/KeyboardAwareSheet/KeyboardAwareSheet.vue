<template>
  <view v-if="modelValue" class="kas-mask" :style="maskStyle" @tap="onMaskTap">
    <view class="kas-sheet" :style="sheetStyle" @tap.stop>
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
/**
 * 键盘安全底部弹层（uni-app H5 / 小程序 / PC 通用）
 *
 * 解决的痛点：手机端弹出系统键盘后，底部弹层里的输入框被键盘遮挡，
 * 用户看不到自己在输入什么（portfolio 新增资产/负债弹窗即为此问题）。
 *
 * 跨端策略：
 *  - **H5**：监听 visualViewport（不支持时回退 window resize），算出键盘高度，
 *           同时给弹层加 bottom 偏移并压缩内容区高度，输入框始终可见；
 *  - **小程序**：弹层不跟着键盘上移时，靠输入框自身的 `adjust-position` 自动上推，
 *           本组件只负责 max-height，避免内容超出屏幕无法滚动；
 *  - **PC / H5 无键盘**：kbHeight 恒为 0，行为与普通底部弹层完全一致。
 *
 * 输入框建议同时加上：`adjust-position` 与 `:cursor-spacing="24"`（见 README 说明）。
 */
export default {
  name: 'KeyboardAwareSheet',
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
      kbHeight: 0,
      scrollTop: 0,
      _baseHeight: 0
    };
  },
  computed: {
    maskStyle() {
      // H5 有键盘时：把整个弹层抬到键盘上方，避免被遮挡
      return this.kbHeight > 0 ? { paddingBottom: this.kbHeight + 'px' } : {};
    },
    sheetStyle() {
      return {};
    },
    bodyStyle() {
      // 键盘占位已在内容里，这里再收紧最大高度，保证可滚动
      const vh = this.kbHeight > 0 ? Math.min(this.maxHeightVh, 40) : this.maxHeightVh;
      return { maxHeight: vh + 'vh' };
    }
  },
  watch: {
    modelValue(val) {
      if (val) {
        this.measure();
        this.bindKeyboard();
        // 打开时重置滚动位置
        this.scrollTop = 0;
      } else {
        this.unbindKeyboard();
        this.kbHeight = 0;
      }
    }
  },
  beforeUnmount() {
    this.unbindKeyboard();
  },
  // uni-app 页面卸载钩子（小程序端 beforeUnmount 可能不触发）
  onUnload() {
    this.unbindKeyboard();
  },
  methods: {
    close() {
      this.$emit('update:modelValue', false);
      this.$emit('close');
    },
    onMaskTap() {
      if (this.maskClosable) this.close();
    },
    measure() {
      // #ifdef H5
      this._baseHeight = window.innerHeight || 0;
      // #endif
    },
    /** H5：按 visualViewport 与 window 高度差推算键盘高度 */
    onViewportResize() {
      // #ifdef H5
      const vv = window.visualViewport;
      const winH = window.innerHeight || 0;
      const visible = vv ? vv.height : winH;
      // 键盘高度 ≈ 布局视口 - 可见视口；减去阈值过滤地址栏收缩等小幅变化
      const diff = Math.max(0, (this._baseHeight || winH) - visible);
      this.kbHeight = diff > 120 ? Math.round(diff) : 0;
      // #endif
    },
    bindKeyboard() {
      // #ifdef H5
      if (window.visualViewport) {
        window.visualViewport.addEventListener('resize', this.onViewportResize);
        window.visualViewport.addEventListener('scroll', this.onViewportResize);
      }
      window.addEventListener('resize', this.onViewportResize);
      // #endif
    },
    unbindKeyboard() {
      // #ifdef H5
      if (window.visualViewport) {
        window.visualViewport.removeEventListener('resize', this.onViewportResize);
        window.visualViewport.removeEventListener('scroll', this.onViewportResize);
      }
      window.removeEventListener('resize', this.onViewportResize);
      // #endif
    }
  }
};
</script>

<style scoped>
.kas-mask {
  position: fixed;
  inset: 0;
  z-index: 99;
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
  right: 0;
  top: -8rpx;
  width: 56rpx;
  height: 56rpx;
  line-height: 56rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 28rpx;
  color: var(--text-sub, #6b7280);
  background: var(--surface, #f0f2f6);
}
.kas-body {
  /* max-height 由 JS 动态给出 */
}
</style>
