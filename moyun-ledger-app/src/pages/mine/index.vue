<template>
  <view class="page">
    <!-- 用户卡 -->
    <view class="user-card">
      <view class="avatar">{{ userStore.isLoggedIn ? '墨' : '?' }}</view>
      <view class="flex-1">
        <view class="user-name">{{ userStore.isLoggedIn ? '已登录' : '未登录' }}</view>
        <view class="user-sub">{{ userStore.isLoggedIn ? '门户账号 · 数据云端同步' : '登录后开启记账之旅' }}</view>
      </view>
    </view>

    <!-- 登录表单（未登录时显示） -->
    <view class="card" v-if="!userStore.isLoggedIn">
      <view class="field">
        <text class="field-label">账号</text>
        <input v-model="loginForm.username" placeholder="门户账号/邮箱" class="field-input" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <input v-model="loginForm.password" type="password" placeholder="密码" class="field-input" />
      </view>
      <view class="btn-primary" @tap="doLogin">登录</view>
      <view class="login-tip">与墨韵门户共用账号体系，首次使用请先在门户注册</view>
    </view>

    <!-- 功能入口 -->
    <view class="card" v-if="userStore.isLoggedIn">
      <view class="menu-item" @tap="go('/pages/mine/categories/index')">
        <text>分类管理</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/mine/budget/index')">
        <text>预算设置</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/record/list')">
        <text>流水明细</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/report/index')">
        <text>报表中心</text><text class="arrow">›</text>
      </view>
      <view class="menu-item" @tap="go('/pages/mine/settings/index')">
        <text>设置（隐私模式等）</text><text class="arrow">›</text>
      </view>
    </view>
    <!-- 未登录功能预览（仅展示，引导先登录） -->
    <view class="card" v-else>
      <view class="menu-item preview" v-for="m in menuPreview" :key="m">
        <text>{{ m }}</text><text class="lock-tip">登录后可用</text>
      </view>
    </view>

    <!-- 退出 -->
    <view class="card" v-if="userStore.isLoggedIn">
      <view class="menu-item logout" @tap="doLogout">退出登录</view>
    </view>
  </view>
</template>

<script>
import { login } from '@/api/ledger';
import { useUserStore } from '@/stores/user';

export default {
  data() {
    return {
      loginForm: { username: '', password: '' },
      menuPreview: ['分类管理', '预算设置', '流水明细', '报表中心', '设置（隐私模式等）']
    };
  },
  computed: {
    userStore() { return useUserStore(); }
  },
  methods: {
    async doLogin() {
      if (!this.loginForm.username || !this.loginForm.password) {
        uni.showToast({ title: '请输入账号密码', icon: 'none' });
        return;
      }
      try {
        const data = await login(this.loginForm);
        // AjaxResult data：{ token } 或直接 token（与门户登录结构一致）
        const token = data && data.token ? data.token : data;
        if (!token) {
          uni.showToast({ title: '登录失败：未获取到令牌', icon: 'none' });
          return;
        }
        this.userStore.setToken(token);
        uni.showToast({ title: '登录成功', icon: 'success' });
      } catch (e) { /* 拦截器已提示 */ }
    },
    doLogout() {
      uni.showModal({
        title: '提示',
        content: '确定退出登录？',
        success: (r) => { if (r.confirm) this.userStore.logout(); }
      });
    },
    go(url) {
      if (!this.userStore.isLoggedIn) {
        uni.showToast({ title: '请先登录', icon: 'none' });
        return;
      }
      uni.navigateTo({ url });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 40rpx; }
.user-card {
  display: flex; align-items: center; background: linear-gradient(135deg, #6a4fd4, #8a6fe8);
  color: #fff; padding: 60rpx 40rpx; margin-bottom: 24rpx;
}
.avatar {
  width: 110rpx; height: 110rpx; border-radius: 55rpx; background: rgba(255,255,255,0.25);
  display: flex; align-items: center; justify-content: center; font-size: 44rpx; margin-right: 24rpx;
}
.user-name { font-size: 34rpx; font-weight: 600; }
.user-sub { font-size: 24rpx; opacity: 0.8; margin-top: 8rpx; }

.card { background: #fff; border-radius: 20rpx; padding: 8rpx 32rpx; margin: 0 24rpx 24rpx; }
.field { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.field-label { width: 140rpx; font-size: 28rpx; color: #666; }
.field-input { flex: 1; font-size: 28rpx; text-align: right; }
.btn-primary { margin: 32rpx 0 16rpx; }
.login-tip { font-size: 22rpx; color: #999; padding-bottom: 16rpx; text-align: center; }

.menu-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 32rpx 0; border-bottom: 1rpx solid #f5f5f7; font-size: 28rpx;
}
.menu-item:last-child { border-bottom: none; }
.arrow { color: #ccc; font-size: 36rpx; }
.menu-item.preview { opacity: 0.6; }
.lock-tip { font-size: 22rpx; color: #bbb; }
.logout { color: #e74c3c; justify-content: center; }
</style>
