<template>
  <view class="page" :style="themeVars">
    <NavBar title="账号登录" />

    <view class="card">
      <view class="field">
        <text class="field-label">账号</text>
        <input v-model="loginForm.username" placeholder="用户名" class="field-input" />
      </view>
      <view class="field">
        <text class="field-label">密码</text>
        <view class="pwd-row">
          <input v-model="loginForm.password" :type="showPwd ? 'text' : 'password'" placeholder="密码" class="field-input pwd-input" />
          <text class="pwd-eye" @tap="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</text>
        </view>
      </view>
      <view class="field" v-if="captcha.enabled">
        <text class="field-label">验证码</text>
        <view class="captcha-row">
          <input v-model="captcha.code" placeholder="请输入验证码" class="field-input captcha-input" />
          <image v-if="captcha.img" :src="captcha.img" mode="aspectFit" class="captcha-img" @tap="loadCaptcha" />
          <view v-else class="captcha-img captcha-loading" @tap="loadCaptcha"><text>加载中</text></view>
        </view>
      </view>
      <view class="btn-primary" @tap="doLogin">登 录</view>
      <view class="login-tip">与墨韵门户共用账号体系</view>
      <view class="register-link" @tap="goRegister">没有账号？立即注册 ›</view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { login, getCaptchaImage } from '@/api/ledger';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';

export default {
  components: { NavBar },
  data() {
    return {
      loginForm: { username: '', password: '' },
      showPwd: false,
      captcha: { enabled: true, uuid: '', img: '', code: '' },
      submitting: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onLoad(payload) {
    // 注册页「返回登录」可携带用户名回填
    if (payload && payload.account) {
      this.loginForm.username = payload.account;
    }
  },
  onShow() {
    useThemeStore().restore();
    if (!useUserStore().isLoggedIn && this.captcha.enabled && !this.captcha.img) {
      this.loadCaptcha();
    }
  },
  methods: {
    async loadCaptcha() {
      try {
        const data = await getCaptchaImage();
        this.captcha.enabled = data.captchaEnabled !== false;
        this.captcha.uuid = data.uuid || '';
        this.captcha.img = data.img ? 'data:image/jpeg;base64,' + data.img : '';
        this.captcha.code = '';
      } catch (e) { this.captcha.img = ''; }
    },
    async doLogin() {
      if (this.submitting) return;
      if (!this.loginForm.username || !this.loginForm.password) {
        uni.showToast({ title: '请输入账号和密码', icon: 'none' });
        return;
      }
      if (this.captcha.enabled && !this.captcha.code) {
        uni.showToast({ title: '请输入验证码', icon: 'none' });
        return;
      }
      this.submitting = true;
      try {
        const payload = {
          username: this.loginForm.username.trim(),
          password: this.loginForm.password,
          code: this.captcha.code.trim(),
          uuid: this.captcha.uuid
        };
        const data = await login(payload);
        const token = data && data.token;
        if (!token) { uni.showToast({ title: '登录失败：未获取到令牌', icon: 'none' }); return; }
        useUserStore().setToken(token);
        useUserStore().setUserInfo(data.user || { username: payload.username });
        uni.showToast({ title: '登录成功', icon: 'success' });
        // 登录成功后回「我的」页（tab 页用 switchTab）
        setTimeout(() => uni.switchTab({ url: '/pages/mine/index' }), 600);
      } catch (e) {
        // 拦截器已提示；验证码已消费，刷新图形码
        this.loadCaptcha();
      }
      this.submitting = false;
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/mine/register/index' });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 60rpx; min-height: 100vh; background: #f5f6f8; }

.card { background: #fff; border-radius: 20rpx; margin: 24rpx; padding: 32rpx; }

.field { margin-bottom: 24rpx; }
.field-label { font-size: 26rpx; color: #666; margin-bottom: 8rpx; display: block; }
.field-input {
  height: 72rpx; line-height: 72rpx; padding: 0 20rpx;
  background: #f5f6f8; border-radius: 12rpx; font-size: 28rpx;
}

.pwd-row { display: flex; align-items: center; background: #f5f6f8; border-radius: 12rpx; }
.pwd-input { flex: 1; background: transparent; }
.pwd-eye {
  padding: 0 20rpx; font-size: 24rpx; color: #4f7cff; height: 72rpx; line-height: 72rpx;
  flex-shrink: 0;
}

.captcha-row { display: flex; align-items: center; }
.captcha-input { flex: 1; }
.captcha-img { width: 200rpx; height: 72rpx; margin-left: 16rpx; border-radius: 12rpx; background: #eee; }
.captcha-loading { display: flex; align-items: center; justify-content: center; font-size: 22rpx; color: #999; }

.btn-primary {
  margin-top: 12rpx; height: 84rpx; line-height: 84rpx; text-align: center;
  background: #4f7cff; color: #fff; border-radius: 16rpx; font-size: 32rpx; font-weight: 500;
}
.btn-primary:active { opacity: .85; }

.login-tip { text-align: center; font-size: 24rpx; color: #999; margin-top: 20rpx; }
.register-link { text-align: center; font-size: 26rpx; color: #4f7cff; margin-top: 16rpx; }
</style>
