<template>
  <view class="navbar" :style="themeVars">
    <view class="navbar-inner">
      <view class="nav-back" v-if="back" @tap="goBack">
        <text class="nav-back-icon">‹</text>
      </view>
      <text class="nav-title">{{ title }}</text>
      <view class="nav-right">
        <slot />
      </view>
    </view>
  </view>
</template>

<script>
import { useThemeStore } from '@/stores/theme';

export default {
  name: 'NavBar',
  props: {
    title: { type: String, default: '' },
    /** 是否显示返回键：Tab 页等没有上级页面时传 false（标题居中不受影响） */
    back: { type: Boolean, default: true }
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  methods: {
    goBack() {
      const pages = getCurrentPages();
      if (pages.length > 1) {
        uni.navigateBack();
      } else {
        // 没有上级页面（如从分享链接直接进入）时回首页，避免卡在本页
        uni.switchTab({ url: '/pages/dashboard/index' });
      }
    }
  }
};
</script>

<style scoped>
.navbar {
  background: var(--card);
  /* 小程序：--status-bar-height 固定 25px（沉浸式下必须留出，否则内容顶进状态栏）；
     H5：该变量为 0，行为与改造前一致 */
  padding-top: var(--status-bar-height);
  /* 跟随主题：原为硬编码 #f0f0f5，暗色/护眼下仍是浅灰边 */
  border-bottom: 1rpx solid var(--border);
  position: sticky;
  top: 0;
  z-index: 20;
}
.navbar-inner {
  /* 标题用绝对定位居中，故这里作为定位参考 */
  position: relative;
  display: flex;
  align-items: center;
  height: 88rpx;
  padding: 0 24rpx;
}
/* 返回键：**热区 88rpx(=44px) 通高，视觉圆底 64rpx**，两者分离。
   原先 width:64rpx 与 min-width:88rpx 互相打架，且 border-radius:32rpx
   落在被 min-* 撑到 88rpx 的盒子上，实际并不是圆。 */
.nav-back {
  position: relative;
  flex: 0 0 88rpx;
  width: 88rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: -12rpx;   /* 视觉左边缘与 24rpx 内距对齐 */
}
.nav-back::before {
  content: '';
  position: absolute;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: var(--primary-soft);
}
.nav-back-icon {
  position: relative;    /* 压在圆底之上 */
  font-size: 44rpx;
  color: var(--primary-text);
  margin-top: -6rpx;
  line-height: 1;
}
/* 标题：绝对定位**真居中**，不再依赖"右侧有多宽"的猜测。
   原实现用固定 margin-right:64rpx 抵消右侧，但右侧插槽实际宽 92~144rpx
   （「确定」/「新建计划」），导致存钱计划页标题偏左约 20px。
   这里用 max-width 左右各预留 150rpx 避让返回键与操作位，超长省略。 */
.nav-title {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  max-width: calc(100% - 300rpx);
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: var(--text-main);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nav-right {
  /* 标题已绝对定位脱离文档流，右侧靠 auto 顶到最右 */
  margin-left: auto;
  display: flex;
  align-items: center;
}
</style>
