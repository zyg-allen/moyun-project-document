<template>
  <view class="page" :style="themeVars">
    <!-- 头部 -->
    <NavBar title="找回密码" />

    <!-- 内容 -->
    <view class="content">
      <view class="tip-card">
        输入注册手机号，通过短信验证码重置密码。已注销的账号完成重置后将自动恢复，历史数据保留。
      </view>

      <view class="card">
        <view class="row">
          <text class="label">手机号</text>
          <input class="input" type="number" v-model="form.phone" placeholder="请输入注册手机号" maxlength="11" />
        </view>
        <view class="row">
          <text class="label">验证码</text>
          <input class="input code-input" type="number" v-model="form.smsCode" placeholder="短信验证码" maxlength="6" />
          <view class="code-btn" :class="{ disabled: smsCountdown > 0 }" @tap="sendSms">
            {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
          </view>
        </view>
        <view class="row">
          <text class="label">新密码</text>
          <view class="pwd-wrap">
            <input class="input pwd-input" :type="showPwd ? 'text' : 'password'" v-model="form.newPassword" placeholder="新密码（6-20位，含大小写字母和数字）" maxlength="20" />
            <text class="pwd-eye" @tap="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</text>
          </view>
        </view>
        <view class="row">
          <text class="label">确认密码</text>
          <view class="pwd-wrap">
            <input class="input pwd-input" :type="showPwd ? 'text' : 'password'" v-model="confirmPassword" placeholder="再次输入新密码" maxlength="20" />
            <text class="pwd-eye" @tap="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</text>
          </view>
        </view>

        <view class="btn-primary" @tap="doReset">重置密码</view>
        <view class="tip">与墨韵门户共用账号体系，重置后两端均可登录</view>
        <view class="link" @tap="goLogin">返回登录</view>
      </view>
    </view>

    <!-- 脚部 -->
    <view class="footer">墨韵记账 · 账号安全</view>

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
import { sendSmsCode, resetPassword, getCaptchaImage } from '@/api/ledger';

export default {
  components: { NavBar },
  data() {
    return {
      form: { phone: '', smsCode: '', newPassword: '' },
      confirmPassword: '',
      showPwd: false,
      smsCountdown: 0,
      submitting: false,
      // 发送短信前的人机校验弹窗（独立 uuid，一次性）
      captchaDialog: { visible: false, img: '', uuid: '', code: '' }
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; }
  },
  onShow() {
    useThemeStore().restore();
  },
  methods: {
    startCountdown() {
      this.smsCountdown = 60;
      const timer = setInterval(() => {
        this.smsCountdown--;
        if (this.smsCountdown <= 0) clearInterval(timer);
      }, 1000);
    },
    async sendSms() {
      if (this.smsCountdown > 0) return;
      const phone = this.form.phone.trim();
      if (!/^1[3-9]\d{9}$/.test(phone)) { uni.showToast({ title: '手机号格式不正确', icon: 'none' }); return; }
      // 图形码开关开启时，先弹窗人机校验再发送
      const cap = await this.probeCaptcha();
      if (cap && cap.captchaEnabled) {
        this.openCaptchaDialog();
        return;
      }
      await this.doSendSms();
    },
    /** 探测图形码开关（不展示图片，仅判断是否需要人机校验） */
    async probeCaptcha() {
      try { return await getCaptchaImage(); } catch (e) { return null; }
    },
    async doSendSms(captcha) {
      try {
        const payload = { phone: this.form.phone.trim(), scene: 'reset_password' };
        if (captcha) { payload.code = captcha.code; payload.uuid = captcha.uuid; }
        await sendSmsCode(payload);
        // 注销账号放行发码，message 会提示"完成验证后将自动恢复"，拦截器透传展示
        uni.showToast({ title: '验证码已发送', icon: 'success' });
        this.startCountdown();
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
      await this.doSendSms(captcha);
    },
    validate() {
      const f = this.form;
      if (!/^1[3-9]\d{9}$/.test(f.phone.trim())) return '请输入正确的手机号';
      if (!/^\d{6}$/.test(f.smsCode.trim())) return '请输入6位短信验证码';
      if (!f.newPassword || f.newPassword.length < 6 || f.newPassword.length > 20) return '密码长度必须为6-20位';
      if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/.test(f.newPassword)) return '密码必须包含大小写字母和数字';
      if (f.newPassword !== this.confirmPassword) return '两次输入的密码不一致';
      return null;
    },
    async doReset() {
      if (this.submitting) return;
      const err = this.validate();
      if (err) { uni.showToast({ title: err, icon: 'none' }); return; }
      this.submitting = true;
      try {
        await resetPassword({
          phone: this.form.phone.trim(),
          code: this.form.smsCode.trim(),
          newPassword: this.form.newPassword,
          confirmPassword: this.confirmPassword
        });
        // 后端成功 message="密码重置成功，账号已恢复，请使用新密码登录"由拦截器展示
        uni.showToast({ title: '重置成功', icon: 'success' });
        setTimeout(() => {
          uni.redirectTo({ url: '/pages/mine/login/index?account=' + encodeURIComponent(this.form.phone.trim()) });
        }, 900);
      } catch (e) { /* 拦截器已提示 */ }
      this.submitting = false;
    },
    goLogin() {
      uni.redirectTo({ url: '/pages/mine/login/index' });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 60rpx; min-height: 100vh; background: #f5f6f8; display: flex; flex-direction: column; }

.content { flex: 1; }

.tip-card {
  margin: 24rpx 24rpx 0; padding: 20rpx 24rpx;
  background: #f0f7ff; border-left: 6rpx solid var(--primary-strong);
  border-radius: 12rpx; font-size: 24rpx; color: #55708c; line-height: 1.7;
}

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
.pwd-eye { flex-shrink: 0; font-size: 24rpx; color: var(--primary-strong); padding: 0 20rpx; height: 72rpx; line-height: 72rpx; }
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

.footer {
  padding: 24rpx 0 calc(24rpx + env(safe-area-inset-bottom));
  text-align: center; font-size: 22rpx; color: #c3c8cf;
}

/* 人机校验弹窗 */
.mask {
  position: fixed; left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5); z-index: 999;
  display: flex; align-items: center; justify-content: center;
}
.captcha-dialog { width: 580rpx; background: #fff; border-radius: 24rpx; padding: 40rpx 36rpx 32rpx; }
.dialog-title { font-size: 32rpx; font-weight: 600; color: #333; text-align: center; }
.dialog-tip { font-size: 24rpx; color: #999; text-align: center; margin: 12rpx 0 28rpx; }
.dialog-body { display: flex; align-items: center; gap: 16rpx; }
.dialog-input {
  flex: 1; font-size: 28rpx; background: #f8f9fa; border-radius: 12rpx;
  height: 80rpx; line-height: 80rpx; padding: 0 20rpx; box-sizing: border-box;
}
.dialog-img { flex-shrink: 0; width: 200rpx; height: 80rpx; border-radius: 8rpx; }
.dialog-btns { display: flex; gap: 20rpx; margin-top: 32rpx; }
.dialog-btn { flex: 1; height: 80rpx; line-height: 80rpx; text-align: center; border-radius: 40rpx; font-size: 28rpx; }
.dialog-btn.cancel { background: #f2f3f5; color: #666; }
.dialog-btn.ok { background: var(--primary-strong); color: #fff; font-weight: 600; }
.dialog-btn:active { opacity: 0.85; }
</style>
