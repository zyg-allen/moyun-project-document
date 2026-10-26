<template>
  <view class="page" :style="themeVars">
    <NavBar :title="active === 'terms' ? TERMS.title : PRIVACY.title" />

    <view class="tabs">
      <view class="tab" :class="{ on: active === 'terms' }" @tap="active = 'terms'">用户协议</view>
      <view class="tab" :class="{ on: active === 'privacy' }" @tap="active = 'privacy'">隐私政策</view>
    </view>

    <view class="card">
      <view class="doc-head">
        <text class="doc-title">{{ doc.title }}</text>
        <text class="doc-meta">版本 {{ LEGAL_VERSION }} · 生效 {{ LEGAL_EFFECTIVE_DATE }}</text>
      </view>
      <view v-for="(s, i) in doc.sections" :key="i" class="section">
        <view class="sec-title">{{ s.t }}</view>
        <view class="sec-body">{{ s.c }}</view>
      </view>
    </view>

    <view class="foot">如对条款有疑问，可通过「意见反馈」联系我们</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { TERMS, PRIVACY, LEGAL_VERSION, LEGAL_EFFECTIVE_DATE } from '@/utils/legal';

export default {
  components: { NavBar },
  data() {
    return {
      active: 'privacy',
      TERMS,
      PRIVACY,
      LEGAL_VERSION,
      LEGAL_EFFECTIVE_DATE
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    doc() { return this.active === 'terms' ? TERMS : PRIVACY; }
  },
  onLoad(options) {
    this.active = options && options.type === 'terms' ? 'terms' : 'privacy';
  },
  onShow() {
    useThemeStore().restore();
  }
};
</script>

<style scoped>
.page { min-height: 100vh; background: var(--surface); padding-bottom: 48rpx; }
.tabs { display: flex; gap: 16rpx; padding: 20rpx 24rpx 0; }
.tab {
  flex: 1; text-align: center; padding: 16rpx 0; border-radius: 999rpx;
  font-size: var(--fs-body); color: var(--text-sub); background: var(--card);
}
.tab.on { color: var(--on-primary); background: var(--primary); font-weight: 600; }
.card { margin: 20rpx 24rpx 0; padding: 28rpx 32rpx; background: var(--card); border-radius: 20rpx; }
.doc-head { padding-bottom: 12rpx; border-bottom: 1rpx solid var(--border); }
.doc-title { font-size: var(--fs-title); font-weight: 700; color: var(--text-main); }
.doc-meta { display: block; margin-top: 8rpx; font-size: var(--fs-unit); color: var(--text-muted); }
.section { padding-top: 24rpx; }
.sec-title { font-size: var(--fs-subtitle); font-weight: 600; color: var(--text-main); }
.sec-body { margin-top: 8rpx; font-size: var(--fs-body); color: var(--text-sub); line-height: 1.8; }
.foot { margin: 28rpx 32rpx; text-align: center; font-size: var(--fs-unit); color: var(--text-muted); }
</style>
