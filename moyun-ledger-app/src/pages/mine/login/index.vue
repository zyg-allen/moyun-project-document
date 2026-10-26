<template>
  <view class="page" :style="themeVars">
    <NavBar title="账号登录" />

    <view class="value-line"><text class="value-hl">记账 + 库存</text>，个人和小生意的一本账</view>

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
      <view class="login-links">
        <text class="register-link" @tap="goRegister">没有账号？立即注册 ›</text>
        <text class="forgot-link" @tap="goForgotPassword">忘记密码？</text>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { login, getCaptchaImage, listAgreements, consentAgreement } from '@/api/ledger';
import { LEGAL_VERSION } from '@/utils/legal';
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
        // 协议换版重签：老用户登录时补一次同意留痕（接口异常不阻断登录）
        await this.ensureAgreementConsent();
        // 登录成功后回「更多」页（tab 页用 switchTab）
    // 从邀请落地页带着邀请码来登录 → 登录成功后自动加入该仓库
    const pending = uni.getStorageSync('moyun_pending_invite') || '';
    if (pending) {
      uni.removeStorageSync('moyun_pending_invite');
      import('@/api/ledger').then(async ({ acceptInvite }) => {
        try {
          await acceptInvite(pending);
          uni.showToast({ title: '已加入仓库', icon: 'success' });
          setTimeout(() => uni.switchTab({ url: '/pages/warehouse/inout/index' }), 700);
        } catch (e) {
          setTimeout(() => uni.switchTab({ url: '/pages/mine/index' }), 600);
        }
      });
      return;
    }
    setTimeout(() => uni.switchTab({ url: '/pages/mine/index' }), 600);
      } catch (e) {
        // 拦截器已提示；验证码已消费，刷新图形码
        this.loadCaptcha();
      }
      this.submitting = false;
    },
    /** 当前版本未同意时，登录后强制补签一次（门户共用表留痕） */
    async ensureAgreementConsent() {
      try {
        const rows = await listAgreements() || [];
        const docs = ['terms', 'privacy'];
        const ok = docs.every(d => rows.some(r => r.docType === d && r.version === LEGAL_VERSION));
        if (ok) return;
        const agreed = await new Promise((resolve) => {
          uni.showModal({
            title: '协议更新',
            content: '《用户协议》与《隐私政策》已更新，请阅读并同意后继续使用（全文见「我的 - 设置」）。',
            confirmText: '同意并继续',
            showCancel: false,
            success: (r) => resolve(!!r.confirm),
            fail: () => resolve(false)
          });
        });
        if (agreed) await consentAgreement(LEGAL_VERSION);
      } catch (e) { /* 接口异常不阻断登录 */ }
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/mine/register/index' });
    },
    goForgotPassword() {
      uni.navigateTo({ url: '/pages/mine/forgot-password/index' });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 60rpx; min-height: 100vh; background: var(--surface); }

.card { background: var(--card); border-radius: 20rpx; margin: 24rpx; padding: 24rpx 32rpx; }

.field { margin-bottom: 24rpx; }
.field-label { font-size: 26rpx; color: var(--text-sub); margin-bottom: 8rpx; display: block; }
.field-input {
  height: 72rpx; line-height: 72rpx; padding: 0 20rpx;
  background: var(--surface); border-radius: 12rpx; font-size: 28rpx;
}

.pwd-row { display: flex; align-items: center; background: var(--surface); border-radius: 12rpx; }
.pwd-input { flex: 1; background: transparent; }
.pwd-eye {
  padding: 0 20rpx; font-size: 24rpx; color: var(--primary-text); height: 72rpx; line-height: 72rpx;
  flex-shrink: 0;
}

.captcha-row { display: flex; align-items: center; }
.captcha-input { flex: 1; }
.captcha-img { width: 200rpx; height: 72rpx; margin-left: 16rpx; border-radius: 12rpx; background: var(--surface); }
.captcha-loading { display: flex; align-items: center; justify-content: center; font-size: 22rpx; color: var(--text-sub); }

.btn-primary {
  margin-top: 12rpx; height: 84rpx; line-height: 84rpx; text-align: center;
  background: var(--primary-strong); color: var(--on-primary); border-radius: 16rpx; font-size: 32rpx; font-weight: 500;
}
.btn-primary:active { opacity: .85; }

.login-links { display: flex; justify-content: space-between; padding: 16rpx 0 4rpx; }
.register-link { font-size: 26rpx; color: var(--primary-text); }
.forgot-link { font-size: 26rpx; color: var(--text-sub); }

/* ---- 登录表单视觉（表单卡 / 输入框 / 主按钮） ---- */




.card { margin: 24rpx; border-radius: 24rpx; padding: 32rpx; box-shadow: 0 8rpx 24rpx rgba(17, 24, 39, 0.05); }
.field-input { height: 84rpx; line-height: 84rpx; border-radius: 14rpx; }
.pwd-eye { height: 84rpx; line-height: 84rpx; }
.captcha-img { height: 84rpx; }
.btn-primary {
  height: 88rpx; line-height: 88rpx; border-radius: 16rpx;
  font-size: 30rpx; font-weight: 600;
  box-shadow: 0 10rpx 24rpx var(--primary-shadow);
}
.value-line {
  margin: 28rpx 40rpx 0; text-align: center;
  font-size: 26rpx; color: var(--text-sub); line-height: 1.6; letter-spacing: 0.5rpx;
}
.value-hl { color: var(--primary-text); font-weight: 600; }
</style>
