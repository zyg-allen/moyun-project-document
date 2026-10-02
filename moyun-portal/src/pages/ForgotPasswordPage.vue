<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { Mail, Lock, AlertCircle, ShieldCheck, Eye, EyeOff, ArrowLeft, KeyRound, RefreshCw, X, Smartphone } from 'lucide-vue-next';
import loginBackground from '@/assets/images/login-background.jpg';
import { useUserStore } from '@/stores/user';
import { useToast } from '@/composables/useToast';
import { getCaptchaImage } from '@/api/user';

const router = useRouter();
const userStore = useUserStore();
const toast = useToast();

// 找回方式：邮箱（邮箱注册用户）/ 手机号（手机号注册用户）
const method = ref<'email' | 'phone'>('email');

function switchMethod(m: 'email' | 'phone') {
  if (method.value === m) return;
  method.value = m;
  errors.value = {};
  serverError.value = '';
  serverSuccess.value = '';
  // 清单 P2：原先"验证码/密码字段保留"，但两种通道的验证码在后端是**独立生成、独立限流**的，
  // 保留会让用户把邮箱验证码直接提交给短信重置接口（必然失败且浪费一次校验）。
  // 这里切换即清空验证码，并把倒计时复位 —— 另一个通道有自己的限流窗口，
  // 沿用上一个通道的剩余秒数会让用户误以为"还没到重发时间"。
  form.value.code = '';
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null; }
  countdown.value = 0;
}

// 找回密码表单：邮箱/手机号 → 验证码 → 新密码
const form = ref({
  email: '',
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
});

const showPassword = ref(false);
const showConfirmPassword = ref(false);
const isLoading = ref(false);
const errors = ref<Record<string, string>>({});
const serverError = ref('');
const serverSuccess = ref('');

// 邮箱验证码倒计时
const isSendingCode = ref(false);
const countdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;
/** 重置成功后的跳转定时器（清单 P2：需受管，避免卸载后仍跳转） */
let redirectTimer: ReturnType<typeof setTimeout> | null = null;

function startCountdown(seconds: number) {
  countdown.value = seconds;
  if (countdownTimer) clearInterval(countdownTimer);
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
  }, 1000);
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
  if (redirectTimer) { clearTimeout(redirectTimer); redirectTimer = null; }
});

// 图形验证码（发送邮箱验证码前弹窗人机校验，受 sys.account.captchaEnabled 开关控制）
const captchaEnabled = ref(false);

async function probeCaptchaEnabled() {
  try {
    const data = await getCaptchaImage();
    captchaEnabled.value = data.captchaEnabled;
  } catch (error) {
    // 验证码探测失败时降级：不弹窗
    console.warn('获取验证码开关失败:', error);
    captchaEnabled.value = false;
  }
}

onMounted(() => {
  probeCaptchaEnabled();
});

function clearError(field: string) {
  if (errors.value[field]) delete errors.value[field];
}

// 发送验证码前的图形码弹窗（独立 uuid，一次性使用；失败留在弹窗内刷新重试）
const captchaModal = ref({
  visible: false,
  img: '',
  uuid: '',
  code: '',
  loading: false,
  error: ''
});

async function loadModalCaptcha() {
  captchaModal.value.loading = true;
  try {
    const data = await getCaptchaImage();
    captchaModal.value.img = data.img;
    captchaModal.value.uuid = data.uuid;
    captchaModal.value.code = '';
  } catch (error) {
    console.warn('获取弹窗验证码失败:', error);
    captchaModal.value.img = '';
    captchaModal.value.uuid = '';
  } finally {
    captchaModal.value.loading = false;
  }
}

function openCaptchaModal() {
  captchaModal.value.visible = true;
  captchaModal.value.error = '';
  captchaModal.value.code = '';
  loadModalCaptcha();
}

function closeCaptchaModal() {
  captchaModal.value.visible = false;
}

// 发送找回密码验证码：点击「获取验证码」→ 弹出图形码弹窗 → 填写确认后才真正发送
async function handleSendCode() {
  errors.value.email = '';
  errors.value.phone = '';
  if (method.value === 'email') {
    const email = form.value.email.trim();
    if (!email) {
      errors.value.email = '请先填写注册邮箱';
      return;
    }
    if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
      errors.value.email = '邮箱格式不正确';
      return;
    }
  } else {
    const phone = form.value.phone.trim();
    if (!phone) {
      errors.value.phone = '请先填写注册手机号';
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      errors.value.phone = '手机号格式不正确';
      return;
    }
  }
  // 图形验证码开关开启时，先弹窗完成人机校验
  if (captchaEnabled.value) {
    openCaptchaModal();
    return;
  }
  await doSendCode();
}

async function doSendCode(captcha?: { code: string; uuid: string }) {
  isSendingCode.value = true;
  try {
    const { success, message } = method.value === 'email'
      ? await userStore.sendEmailCodeWithApi(form.value.email.trim(), 'reset_password', captcha)
      : await userStore.sendSmsCodeWithApi(form.value.phone.trim(), 'reset_password', captcha);
    if (success) {
      toast.success(message || (method.value === 'email' ? '验证码已发送至邮箱' : '验证码已发送至手机'));
      startCountdown(60);
      captchaModal.value.visible = false;
    } else {
      const msg = message || '验证码发送失败，请稍后重试';
      if (captchaModal.value.visible) {
        // 发送失败：留在弹窗内刷新图形码重新输入
        captchaModal.value.error = msg;
        loadModalCaptcha();
      } else {
        // 弹窗不可见（captchaEnabled=false 直接发送）：写弹窗等于静默失败，改走 toast
        toast.error(msg);
      }
    }
  } finally {
    isSendingCode.value = false;
  }
}

// 弹窗内确认发送
async function confirmModalSend() {
  const code = captchaModal.value.code.trim();
  if (!code) {
    captchaModal.value.error = '请输入图形验证码';
    return;
  }
  captchaModal.value.error = '';
  await doSendCode({ code, uuid: captchaModal.value.uuid });
}

// 密码强度实时计算
type StrengthLevel = { level: 0 | 1 | 2 | 3; label: string; color: string };
const passwordStrength = computed<StrengthLevel>(() => {
  const pwd = form.value.newPassword || '';
  if (!pwd) return { level: 0, label: '', color: 'transparent' };
  let score = 0;
  if (pwd.length >= 6) score++;
  if (pwd.length >= 10) score++;
  if (/[a-z]/.test(pwd) && /[A-Z]/.test(pwd)) score++;
  if (/\d/.test(pwd)) score++;
  if (/[^a-zA-Z0-9]/.test(pwd)) score++;
  if (score <= 1) return { level: 1, label: '弱', color: '#ef4444' };
  if (score === 2 || score === 3) return { level: 2, label: '中', color: '#f59e0b' };
  return { level: 3, label: '强', color: '#10b981' };
});

const strengthBars = computed(() => {
  const lv = passwordStrength.value.level;
  return [lv >= 1, lv >= 2, lv >= 3];
});

// 提交重置密码
async function handleReset() {
  errors.value = {};
  serverError.value = '';

  const email = form.value.email.trim();
  const phone = form.value.phone.trim();
  const code = form.value.code.trim();
  const newPassword = form.value.newPassword;
  const confirmPassword = form.value.confirmPassword;

  if (method.value === 'email') {
    if (!email) {
      errors.value.email = '请填写注册邮箱';
      return;
    }
    if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
      errors.value.email = '邮箱格式不正确';
      return;
    }
    if (!code) {
      errors.value.code = '请输入邮箱验证码';
      return;
    }
  } else {
    if (!phone) {
      errors.value.phone = '请填写注册手机号';
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      errors.value.phone = '手机号格式不正确';
      return;
    }
    if (!code) {
      errors.value.code = '请输入短信验证码';
      return;
    }
  }
  if (newPassword.length < 6 || newPassword.length > 20) {
    errors.value.newPassword = '密码长度必须为 6-20 位';
    return;
  }
  if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/.test(newPassword)) {
    errors.value.newPassword = '密码必须包含大小写字母和数字';
    return;
  }
  if (newPassword !== confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致';
    return;
  }

  isLoading.value = true;
  try {
    const { success, message } = method.value === 'email'
      ? await userStore.resetPasswordWithApi(email, code, newPassword)
      : await userStore.resetPasswordBySmsWithApi(phone, code, newPassword, confirmPassword);
    if (success) {
      serverSuccess.value = message || '密码重置成功';
      toast.success('密码重置成功，请使用新密码登录');
      // 清单 P2：原为裸 setTimeout，未保存句柄也未在卸载时清理 ——
      // 用户 1.5s 内主动离开也会被强制跳转。这里受管 + 卸载清理。
      if (redirectTimer) clearTimeout(redirectTimer);
      redirectTimer = setTimeout(() => {
        redirectTimer = null;
        router.push('/login');
      }, 1500);
    } else {
      serverError.value = message || '重置失败，请稍后重试';
    }
  } catch (error) {
    console.error('重置密码失败:', error);
    serverError.value = '重置失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
}

const copyrightYear = computed(() => new Date().getFullYear());
</script>

<template>
  <div class="min-h-screen flex flex-col items-center justify-center py-12 px-4 relative overflow-hidden"
       :style="{ backgroundImage: `url(${loginBackground})`, backgroundSize: 'cover', backgroundPosition: 'center' }">

    <!-- 背景遮罩 -->
    <div class="absolute inset-0 bg-gradient-to-br from-black/60 via-black/40 to-black/60"></div>

    <div class="max-w-md w-full relative z-10">
      <!-- Logo -->
      <div class="text-center mb-10">
        <Link to="/" class="inline-flex items-center space-x-3 transition-all duration-300 hover:scale-105 hover:shadow-lg">
          <div class="w-16 h-16 bg-gradient-to-br from-amber-500 to-orange-600 rounded-2xl flex items-center justify-center shadow-2xl shadow-amber-500/25 border border-amber-400/30">
            <span class="text-white font-bold text-3xl">墨</span>
          </div>
          <div class="text-left">
            <span class="block text-3xl font-bold bg-gradient-to-r from-amber-200 via-orange-200 to-yellow-200 bg-clip-text text-transparent">
              旭林知行
            </span>
            <span class="block text-xs text-amber-200/70 tracking-widest mt-1">KNOWLEDGE HUB</span>
          </div>
        </Link>
      </div>

      <!-- Reset Card -->
      <div class="relative group">
        <div class="absolute -inset-1 bg-gradient-to-r from-amber-500 via-orange-500 to-yellow-500 rounded-3xl blur opacity-30 group-hover:opacity-50 transition-all duration-1000"></div>

        <div class="relative bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-white/20">
          <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-amber-500/50 to-transparent"></div>

          <div class="p-8">
            <div class="text-center mb-6">
              <h1 class="text-3xl font-bold text-slate-800 mb-2">找回密码</h1>
              <p class="text-slate-500 text-sm">通过注册邮箱或注册手机号的验证码重置密码。</p>
            </div>

            <!-- 找回方式切换（邮箱 / 手机号，同页切换不跳转） -->
            <div class="flex p-1 mb-6 bg-slate-100 rounded-2xl">
              <button
                type="button"
                class="flex-1 py-2.5 rounded-xl text-sm font-medium transition-all duration-300"
                :class="method === 'email'
                  ? 'bg-white text-amber-600 shadow-md'
                  : 'text-slate-500 hover:text-slate-700'"
                @click="switchMethod('email')"
              >
                邮箱找回
              </button>
              <button
                type="button"
                class="flex-1 py-2.5 rounded-xl text-sm font-medium transition-all duration-300"
                :class="method === 'phone'
                  ? 'bg-white text-amber-600 shadow-md'
                  : 'text-slate-500 hover:text-slate-700'"
                @click="switchMethod('phone')"
              >
                手机号找回
              </button>
            </div>

            <!-- 成功提示 -->
            <div v-if="serverSuccess" class="mb-6 p-4 bg-green-50 border border-green-200 rounded-xl text-green-700 text-sm flex items-start gap-2">
              <ShieldCheck class="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{{ serverSuccess }}</span>
            </div>

            <!-- 服务器错误 -->
            <div v-if="serverError" class="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm flex items-start gap-2">
              <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{{ serverError }}</span>
            </div>

            <form @submit.prevent="handleReset" class="space-y-5">
              <!-- Email（邮箱找回） -->
              <div v-if="method === 'email'" class="group">
                <label for="forgot-email" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">注册邮箱</label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <Mail class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                    <input
                      id="forgot-email"
                      v-model="form.email"
                      type="email"
                      placeholder="请输入注册时的邮箱"
                      autocomplete="email"
                      @input="clearError('email')"
                      class="w-full pl-12 pr-4 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                      :class="{
                        'border-red-300 focus:border-red-400 bg-red-50': errors.email,
                        'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.email
                      }"
                      :disabled="isLoading"
                    />
                  </div>
                  <button
                    type="button"
                    @click="handleSendCode"
                    :disabled="isSendingCode || countdown > 0 || isLoading"
                    class="flex-shrink-0 px-5 py-4 rounded-2xl border-2 text-sm font-medium transition-all duration-300 disabled:cursor-not-allowed"
                    :class="countdown > 0
                      ? 'border-slate-200 text-slate-400 bg-slate-50'
                      : 'border-amber-400 text-amber-600 bg-amber-50 hover:bg-amber-100'"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重发` : (isSendingCode ? '发送中…' : '获取验证码') }}
                  </button>
                </div>
                <p v-if="errors.email" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.email }}
                </p>
              </div>

              <!-- Phone（手机号找回） -->
              <div v-if="method === 'phone'" class="group">
                <label for="forgot-phone" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">注册手机号</label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <Smartphone class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                    <input
                      id="forgot-phone"
                      v-model="form.phone"
                      type="tel"
                      placeholder="请输入注册时的手机号"
                      autocomplete="tel"
                      maxlength="11"
                      @input="clearError('phone')"
                      class="w-full pl-12 pr-4 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                      :class="{
                        'border-red-300 focus:border-red-400 bg-red-50': errors.phone,
                        'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.phone
                      }"
                      :disabled="isLoading"
                    />
                  </div>
                  <button
                    type="button"
                    @click="handleSendCode"
                    :disabled="isSendingCode || countdown > 0 || isLoading"
                    class="flex-shrink-0 px-5 py-4 rounded-2xl border-2 text-sm font-medium transition-all duration-300 disabled:cursor-not-allowed"
                    :class="countdown > 0
                      ? 'border-slate-200 text-slate-400 bg-slate-50'
                      : 'border-amber-400 text-amber-600 bg-amber-50 hover:bg-amber-100'"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重发` : (isSendingCode ? '发送中…' : '获取验证码') }}
                  </button>
                </div>
                <p v-if="errors.phone" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.phone }}
                </p>
              </div>

              <!-- Email Code -->
              <div class="group">
                <label for="forgot-code" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">{{ method === 'email' ? '邮箱验证码' : '短信验证码' }}</label>
                <div class="relative">
                  <ShieldCheck class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="forgot-code"
                    v-model="form.code"
                    type="text"
                    inputmode="numeric"
                    :placeholder="method === 'email' ? '请输入邮箱收到的 6 位验证码' : '请输入短信收到的 6 位验证码'"
                    autocomplete="one-time-code"
                    maxlength="6"
                    @input="clearError('code')"
                    class="w-full pl-12 pr-4 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800 tracking-widest"
                    :class="{
                      'border-red-300 focus:border-red-400 bg-red-50': errors.code,
                      'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.code
                    }"
                    :disabled="isLoading"
                  />
                </div>
                <p v-if="errors.code" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.code }}
                </p>
              </div>

              <!-- New Password -->
              <div class="group">
                <label for="forgot-password" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">新密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="forgot-password"
                    v-model="form.newPassword"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="6-20位，含大小写字母和数字"
                    autocomplete="new-password"
                    @input="clearError('newPassword')"
                    class="w-full pl-12 pr-12 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                    :class="{
                      'border-red-300 focus:border-red-400 bg-red-50': errors.newPassword,
                      'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.newPassword
                    }"
                    :disabled="isLoading"
                  />
                  <button
                    type="button"
                    @click="showPassword = !showPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-amber-500 transition-colors"
                    tabindex="-1"
                  >
                    <Eye v-if="!showPassword" class="w-5 h-5" />
                    <EyeOff v-else class="w-5 h-5" />
                  </button>
                </div>
                <!-- 密码强度条 -->
                <div v-if="form.newPassword" class="flex gap-1.5 mt-2 ml-1">
                  <div
                    v-for="(active, i) in strengthBars"
                    :key="i"
                    class="h-1 flex-1 rounded-full transition-all duration-300"
                    :style="{ backgroundColor: active ? passwordStrength.color : '#e2e8f0' }"
                  ></div>
                </div>
                <p v-if="errors.newPassword" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.newPassword }}
                </p>
              </div>

              <!-- Confirm Password -->
              <div class="group">
                <label for="forgot-confirm" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">确认新密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="forgot-confirm"
                    v-model="form.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    placeholder="请再次输入新密码"
                    autocomplete="new-password"
                    @input="clearError('confirmPassword')"
                    class="w-full pl-12 pr-12 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                    :class="{
                      'border-red-300 focus:border-red-400 bg-red-50': errors.confirmPassword,
                      'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.confirmPassword
                    }"
                    :disabled="isLoading"
                  />
                  <button
                    type="button"
                    @click="showConfirmPassword = !showConfirmPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-amber-500 transition-colors"
                    tabindex="-1"
                  >
                    <Eye v-if="!showConfirmPassword" class="w-5 h-5" />
                    <EyeOff v-else class="w-5 h-5" />
                  </button>
                </div>
                <p v-if="errors.confirmPassword" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.confirmPassword }}
                </p>
              </div>

              <!-- Submit -->
              <button
                type="submit"
                :disabled="isLoading"
                class="w-full py-4 bg-gradient-to-r from-amber-500 to-orange-600 text-white font-semibold rounded-2xl hover:from-amber-600 hover:to-orange-700 transition-all duration-300 shadow-lg shadow-amber-500/30 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 group"
              >
                <KeyRound class="w-5 h-5 transition-transform group-hover:rotate-12" />
                <span>{{ isLoading ? '重置中…' : '重置密码' }}</span>
              </button>

              <!-- Back to Login -->
              <div class="text-center pt-2">
                <Link to="/login" class="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-amber-600 transition-colors">
                  <ArrowLeft class="w-4 h-4" />
                  返回登录
                </Link>
              </div>
            </form>
          </div>
        </div>
      </div>

      <!-- Copyright -->
      <p class="text-center text-xs text-white/60 mt-8">
        © {{ copyrightYear }} 旭林知行 · 保留所有权利
      </p>
    </div>

    <!-- 发送验证码前的图形码人机校验弹窗（验证码一次性作废；失败留在弹窗内刷新重试） -->
    <div
      v-if="captchaModal.visible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
      @click.self="closeCaptchaModal"
    >
      <div class="w-full max-w-sm bg-white rounded-3xl shadow-2xl p-6 relative">
        <button
          type="button"
          class="absolute top-4 right-4 text-slate-400 hover:text-slate-600 transition-colors"
          aria-label="关闭弹窗"
          @click="closeCaptchaModal"
        >
          <X class="w-5 h-5" />
        </button>
        <h3 class="text-lg font-bold text-slate-800 text-center">安全校验</h3>
        <p class="text-xs text-slate-500 text-center mt-1 mb-5">为防止恶意刷验证码，请先完成图形验证</p>

        <div v-if="captchaModal.error" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl text-red-600 text-xs flex items-start gap-2">
          <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
          <span>{{ captchaModal.error }}</span>
        </div>

        <div class="flex gap-3">
          <input
            v-model="captchaModal.code"
            type="text"
            placeholder="输入图形验证码"
            autocomplete="off"
            maxlength="10"
            class="flex-1 px-4 py-3 bg-slate-50 border-2 border-slate-200 rounded-2xl focus:outline-none focus:border-amber-400 transition-all placeholder:text-slate-400 text-slate-800"
            @keyup.enter="confirmModalSend"
          />
          <button
            type="button"
            class="relative h-[50px] w-[110px] flex-shrink-0 overflow-hidden rounded-2xl border-2 border-slate-200 bg-slate-50 hover:border-amber-400 transition-all disabled:opacity-50 flex items-center justify-center"
            aria-label="点击刷新验证码"
            :disabled="captchaModal.loading"
            @click="loadModalCaptcha"
          >
            <img v-if="captchaModal.img" :src="captchaModal.img" alt="验证码" class="w-full h-full object-cover" />
            <RefreshCw v-else class="w-5 h-5 text-slate-400" :class="{ 'animate-spin': captchaModal.loading }" />
          </button>
        </div>

        <div class="flex gap-3 mt-5">
          <button
            type="button"
            class="flex-1 py-3 rounded-2xl border-2 border-slate-200 text-slate-500 text-sm font-medium hover:bg-slate-50 transition-all"
            @click="closeCaptchaModal"
          >
            取消
          </button>
          <button
            type="button"
            class="flex-1 py-3 rounded-2xl bg-gradient-to-r from-amber-500 to-orange-600 text-white text-sm font-semibold hover:from-amber-600 hover:to-orange-700 transition-all disabled:opacity-50"
            :disabled="isSendingCode"
            @click="confirmModalSend"
          >
            {{ isSendingCode ? '发送中…' : '发送验证码' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
