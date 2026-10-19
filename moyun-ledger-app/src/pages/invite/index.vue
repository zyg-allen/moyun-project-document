<template>
  <view class="page" :style="themeVars">
    <NavBar title="仓库邀请" />

    <view v-if="loading" class="box">
      <text class="tip">正在读取邀请信息…</text>
    </view>

    <view v-else-if="err" class="box">
      <view class="icon">⚠️</view>
      <view class="title">邀请已失效</view>
      <view class="tip">{{ err }}</view>
      <view class="btn" @tap="goHome">去首页</view>
    </view>

    <view v-else class="box">
      <view class="icon">📦</view>
      <view class="title">{{ invite.inviterName || '有人' }} 邀请你一起管理</view>
      <view class="wh">{{ invite.warehouseName }}</view>
      <view class="tip">加入后可以看到该仓库的货架、商品和库存，并能出入库</view>
      <view class="tip sub">还可加入 {{ invite.remain }} 人</view>

      <view class="btn" :class="{ dis: submitting }" @tap="join">
        {{ submitting ? '正在加入…' : '加入这个仓库' }}
      </view>
      <view class="btn ghost" @tap="goHome">暂不加入</view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { previewInvite, acceptInvite } from '@/api/ledger';

export default {
  components: { NavBar },
  data() {
    return {
      code: '',
      invite: {},
      loading: true,
      submitting: false,
      err: ''
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onLoad(options) {
    this.code = ((options && options.code) || '').toUpperCase();
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.err = '';
      if (!this.code) {
        this.err = '缺少邀请码';
        this.loading = false;
        return;
      }
      try {
        this.invite = (await previewInvite(this.code)) || {};
      } catch (e) {
        this.err = '邀请码无效或已过期';
      }
      this.loading = false;
    },
    async join() {
      if (this.submitting) return;
      if (!useUserStore().isLoggedIn) {
        uni.showModal({
          title: '请先登录',
          content: '登录后即可加入这个仓库',
          confirmText: '去登录',
          success: (r) => {
            if (r.confirm) uni.navigateTo({ url: '/pages/mine/login/index' });
          }
        });
        return;
      }
      this.submitting = true;
      try {
        await acceptInvite(this.code);
        uni.showToast({ title: '已加入', icon: 'success' });
        setTimeout(() => {
          uni.switchTab({ url: '/pages/warehouse/inout/index' });
        }, 600);
      } catch (e) { /* 拦截器已提示 */ }
      this.submitting = false;
    },
    goHome() { uni.switchTab({ url: '/pages/dashboard/index' }); }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding-top: 24rpx; }
.box {
  margin: 60rpx 40rpx 0; padding: 48rpx 32rpx;
  background: var(--card); border: 1rpx solid var(--border); border-radius: 20rpx;
  display: flex; flex-direction: column; align-items: center;
}
.icon { font-size: 80rpx; }
.title { font-size: 30rpx; font-weight: 700; color: var(--text-main); margin-top: 20rpx; text-align: center; }
.wh { font-size: 36rpx; font-weight: 700; color: var(--primary); margin-top: 16rpx; }
.tip { font-size: 23rpx; color: var(--text-sub); margin-top: 16rpx; text-align: center; line-height: 1.6; }
.tip.sub { color: var(--text-muted); }
.btn {
  width: 100%; margin-top: 36rpx; padding: 26rpx 0; text-align: center;
  border-radius: 14rpx; background: var(--primary); color: #fff;
  font-size: 28rpx; font-weight: 600;
}
.btn.dis { opacity: .6; }
.btn.ghost { background: transparent; color: var(--text-sub); border: 1rpx solid var(--border); margin-top: 16rpx; }
</style>
