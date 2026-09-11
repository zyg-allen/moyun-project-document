<template>
  <view class="page" :style="themeVars">
    <NavBar title="注册账号" />

    <!-- 注册方式切换 -->
    <view class="mode-bar">
      <view class="mode" :class="{ active: mode === 'phone' }" @tap="mode = 'phone'">📱 手机注册</view>
      <view class="mode" :class="{ active: mode === 'email' }" @tap="mode = 'email'">📧 邮箱注册</view>
    </view>

    <view class="card">
      <!-- 手机模式 -->
      <block v-if="mode === 'phone'">
        <view class="row">
          <text class="label">手机号</text>
          <input class="input" type="number" v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </view>
        <view class="row">
          <text class="label">验证码</text>
          <input class="input code-input" type="number" v-model="form.smsCode" placeholder="短信验证码" maxlength="6" />
          <view class="code-btn" :class="{ disabled: smsCountdown > 0 }" @tap="sendSms">
            {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
          </view>
        </view>
      </block>

      <!-- 邮箱模式 -->
      <block v-else>
        <view class="row">
          <text class="label">邮箱</text>
          <input class="input" v-model="form.email" placeholder="请输入邮箱" maxlength="100" />
        </view>
        <view class="row">
          <text class="label">验证码</text>
          <input class="input code-input" type="number" v-model="form.emailCode" placeholder="邮箱验证码" maxlength="6" />
          <view class="code-btn" :class="{ disabled: emailCountdown > 0 }" @tap="sendEmail">
            {{ emailCountdown > 0 ? emailCountdown + 's' : '获取验证码' }}
          </view>
        </view>
      </block>

      <!-- 账号设置 -->
      <view class="row">
        <text class="label">用户名</text>
        <input class="input" v-model="form.username" placeholder="设置用户名（登录用，3-30字符）" maxlength="30" />
      </view>
      <view class="row">
        <text class="label">密码</text>
        <view class="pwd-wrap">
          <input class="input pwd-input" :type="showPwd ? 'text' : 'password'" v-model="form.password" placeholder="设置密码（6-20位，含大小写字母和数字）" maxlength="20" />
          <text class="pwd-eye" @tap="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</text>
        </view>
      </view>
      <view class="row">
        <text class="label">确认密码</text>
        <view class="pwd-wrap">
          <input class="input pwd-input" :type="showPwd ? 'text' : 'password'" v-model="confirmPassword" placeholder="再次输入密码" maxlength="20" />
          <text class="pwd-eye" @tap="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</text>
        </view>
      </view>

      <view class="btn-primary" @tap="doRegister">注 册</view>
      <view class="tip">注册成功将自动登录，与墨韵门户共用账号体系</view>
      <view class="link" @tap="goLogin">已有账号？返回登录</view>
    </view>

    <!-- 发送短信前的人机校验弹窗（图形码独立加载，一次性使用） -->
    <view class="mask" v-if="captchaDialog.visible" @tap="closeCaptchaDialog">
      <view class="captcha-dialog" @tap.stop>
        <view class="dialog-title">安全校验</view>
        <view class="dialog-tip">为防止恶意刷验证码，请先完成图形验证</view>
        <view class="dialog-body">
          <input class="dialog-input" v-model="captchaDialog.code" placeholder="输入图形验证码" maxlength="5" />
          <image class="dialog-img" :src="captchaDialog.img" mode="aspectFit" @tap="loadDialogCaptcha" />
        </view>
        <view class="dialog-btns">
          <view class="dialog-btn cancel" @tap="closeCaptchaDialog">取消</view>
          <view class="dialog-btn ok" @tap="confirmCaptchaSend">发送验证码</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { useUserStore } from '@/stores/user';
import { register, sendEmailCode, sendSmsCode, getCaptchaImage } from '@/api/ledger';

export default {
  components: { NavBar },
  data() {
    return {
      mode: 'phone',
      form: { phone: '', smsCode: '', email: '', emailCode: '', username: '', password: '' },
      showPwd: false,
      confirmPassword: '',
      captchaEnabled: false,
      smsCountdown: 0,
      emailCountdown: 0,
      submitting: false,
      // 发送短信前的人机校验弹窗（独立 uuid，一次性）
      captchaDialog: { visible: false, img: '', uuid: '', code: '' }
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onLoad() {
    this.loadCaptcha();
  },
  onShow() {
    useThemeStore().restore();
  },
  methods: {
    async loadCaptcha() {
      // v11.42：仅探测开关（captchaEnabled 控制发送验证码前是否弹窗人机校验）；
      // 图形码校验已由弹窗完成，注册提交不再携带 code/uuid
      try {
        const cap = await getCaptchaImage();
        this.captchaEnabled = !!cap.captchaEnabled;
      } catch (e) { this.captchaEnabled = false; }
    },
    startCountdown(type) {
      const key = type + 'Countdown';
      this[key] = 60;
      const timer = setInterval(() => {
        this[key]--;
        if (this[key] <= 0) clearInterval(timer);
      }, 1000);
    },
    async sendSms() {
      if (this.smsCountdown > 0) return;
      const phone = this.form.phone.trim();
      if (!/^1[3-9]\d{9}$/.test(phone)) { uni.showToast({ title: '手机号格式不正确', icon: 'none' }); return; }
      // v11.42：图形码开关开启时，先弹窗人机校验再发送
      if (this.captchaEnabled) {
        this.openCaptchaDialog();
        return;
      }
      await this.doSendSms();
    },
    async doSendSms(captcha) {
      try {
        const payload = { phone: this.form.phone.trim(), scene: 'register' };
        if (captcha) { payload.code = captcha.code; payload.uuid = captcha.uuid; }
        await sendSmsCode(payload);
        uni.showToast({ title: '验证码已发送', icon: 'success' });
        this.startCountdown('sms');
      } catch (e) { /* 拦截器已提示 */ }
    },
    // ==================== 图形码弹窗 ====================
    openCaptchaDialog() {
      this.captchaDialog.visible = true;
      this.captchaDialog.code = '';
      this.loadDialogCaptcha();
    },
    closeCaptchaDialog() {
      this.captchaDialog.visible = false;
    },
    async loadDialogCaptcha() {
      try {
        const cap = await getCaptchaImage();
        this.captchaDialog.img = cap.img ? 'data:image/jpeg;base64,' + cap.img : '';
        this.captchaDialog.uuid = cap.uuid || '';
        this.captchaDialog.code = '';
      } catch (e) { this.captchaDialog.img = ''; }
    },
    async confirmCaptchaSend() {
      const code = this.captchaDialog.code.trim();
      if (!code) { uni.showToast({ title: '请输入图形验证码', icon: 'none' }); return; }
      const captcha = { code, uuid: this.captchaDialog.uuid };
      this.captchaDialog.visible = false;
      // 按当前注册方式分发（手机短信 / 邮箱共用同一弹窗）
      if (this.mode === 'phone') {
        await this.doSendSms(captcha);
      } else {
        await this.doSendEmail(captcha);
      }
    },
    async sendEmail() {
      if (this.emailCountdown > 0) return;
      const email = this.form.email.trim();
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { uni.showToast({ title: '邮箱格式不正确', icon: 'none' }); return; }
      // v11.42：图形码开关开启时，先弹窗人机校验再发送
      if (this.captchaEnabled) {
        this.openCaptchaDialog();
        return;
      }
      await this.doSendEmail();
    },
    async doSendEmail(captcha) {
      try {
        const payload = { email: this.form.email.trim(), type: 'register' };
        if (captcha) { payload.code = captcha.code; payload.uuid = captcha.uuid; }
        await sendEmailCode(payload);
        uni.showToast({ title: '验证码已发送', icon: 'success' });
        this.startCountdown('email');
      } catch (e) { /* 拦截器已提示 */ }
    },
    validate() {
      const f = this.form;
      if (this.mode === 'phone') {
        if (!/^1[3-9]\d{9}$/.test(f.phone.trim())) return '请输入正确的手机号';
        if (!/^\d{6}$/.test(f.smsCode.trim())) return '请输入6位短信验证码';
      } else {
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) return '请输入正确的邮箱';
        if (!/^\d{6}$/.test(f.emailCode.trim())) return '请输入6位邮箱验证码';
      }
      if (!f.username.trim() || f.username.trim().length < 3) return '用户名至少3个字符';
      if (!f.password || f.password.length < 6 || f.password.length > 20) return '密码长度必须为6-20位';
      if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/.test(f.password)) return '密码必须包含大小写字母和数字';
      if (f.password !== this.confirmPassword) return '两次输入的密码不一致';
      return null;
    },
    async doRegister() {
      if (this.submitting) return;
      const err = this.validate();
      if (err) { uni.showToast({ title: err, icon: 'none' }); return; }
      this.submitting = true;
      try {
        // v11.42：人机校验已由发送验证码弹窗完成（一次性作废），提交不再携带图形码
        const payload = {
          username: this.form.username.trim(),
          password: this.form.password
        };
        if (this.mode === 'phone') {
          payload.phone = this.form.phone.trim();
          payload.smsCode = this.form.smsCode.trim();
        } else {
          payload.email = this.form.email.trim();
          payload.emailCode = this.form.emailCode.trim();
        }
        const data = await register(payload);
        const token = data && data.token;
        if (!token) { uni.showToast({ title: '注册失败：未获取到令牌', icon: 'none' }); return; }
        // 注册即登录：写 token 并跳回「我的」
        useUserStore().setToken(token);
        useUserStore().setUserInfo(data.user || { username: payload.username });
        uni.showToast({ title: '注册成功', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 800);
      } catch (e) { /* 拦截器已提示 */ }
      this.submitting = false;
    },
    // 跳独立登录页，携带用户名回填（登录仅支持 username，手机号/邮箱不能作为登录账号）
    goLogin() {
      const account = this.form.username.trim();
      uni.navigateTo({ url: '/pages/mine/login/index' + (account ? '?account=' + encodeURIComponent(account) : '') });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 60rpx; min-height: 100vh; background: #f5f6f8; }

.mode-bar {
  display: flex; background: #fff; margin: 24rpx 24rpx 0; border-radius: 20rpx; padding: 8rpx;
}
.mode {
  flex: 1; text-align: center; padding: 20rpx 0; font-size: 28rpx; color: #666; border-radius: 16rpx;
}
.mode.active { background: var(--primary-strong); color: #fff; font-weight: 600; }

.card { background: #fff; margin: 24rpx; border-radius: 20rpx; padding: 8rpx 32rpx 40rpx; }
.row { display: flex; align-items: center; padding: 26rpx 0; border-bottom: 1rpx solid #f5f5f7; gap: 16rpx; }
.row:last-of-type { border-bottom: none; }
.label { width: 150rpx; font-size: 26rpx; color: #333; flex-shrink: 0; }
.input {
  flex: 1; font-size: 26rpx; background: #f8f9fa; border-radius: 12rpx;
  height: 72rpx; line-height: 72rpx; padding: 0 20rpx; box-sizing: border-box;
}
.code-input { flex: 1; min-width: 0; }
.pwd-wrap { flex: 1; display: flex; align-items: center; background: #f8f9fa; border-radius: 12rpx; }
.pwd-wrap .pwd-input { background: transparent; }
.pwd-eye {
  flex-shrink: 0; font-size: 24rpx; color: var(--primary-strong);
  padding: 0 20rpx; height: 72rpx; line-height: 72rpx;
}
.code-btn {
  flex-shrink: 0; font-size: 24rpx; color: var(--primary-strong);
  border: 1rpx solid var(--primary-strong); border-radius: 28rpx; padding: 10rpx 24rpx;
}
.code-btn.disabled { color: #bbb; border-color: #e5e6e8; }

.btn-primary {
  margin-top: 36rpx; height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff; border-radius: 44rpx;
  font-size: 30rpx; font-weight: 600;
}
.btn-primary:active { opacity: 0.85; }
.tip { font-size: 22rpx; color: #999; text-align: center; margin-top: 20rpx; }
.link { font-size: 24rpx; color: var(--primary-strong); text-align: center; margin-top: 16rpx; padding: 10rpx; }

/* 人机校验弹窗 */
.mask {
  position: fixed; left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.captcha-dialog {
  width: 580rpx; background: #fff; border-radius: 24rpx; padding: 40rpx 36rpx 32rpx;
}
.dialog-title { font-size: 32rpx; font-weight: 600; color: #333; text-align: center; }
.dialog-tip { font-size: 24rpx; color: #999; text-align: center; margin: 12rpx 0 28rpx; }
.dialog-body { display: flex; align-items: center; gap: 16rpx; }
.dialog-input {
  flex: 1; font-size: 28rpx; background: #f8f9fa; border-radius: 12rpx;
  height: 80rpx; line-height: 80rpx; padding: 0 20rpx; box-sizing: border-box;
}
.dialog-img { flex-shrink: 0; width: 200rpx; height: 80rpx; border-radius: 8rpx; }
.dialog-btns { display: flex; gap: 20rpx; margin-top: 32rpx; }
.dialog-btn {
  flex: 1; height: 80rpx; line-height: 80rpx; text-align: center;
  border-radius: 40rpx; font-size: 28rpx;
}
.dialog-btn.cancel { background: #f2f3f5; color: #666; }
.dialog-btn.ok { background: var(--primary-strong); color: #fff; font-weight: 600; }
.dialog-btn:active { opacity: 0.85; }
</style>