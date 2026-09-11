<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import {
  Eye, EyeOff, Lock, User, ArrowRight, AlertCircle, Mail, ShieldCheck, RefreshCw
} from 'lucide-vue-next';
import loginBackground from '@/assets/images/login-background.jpg';
import { useUserStore } from '@/stores/user';
import { registerSchema, validateForm } from '@/utils/validation';
import { useToast } from '@/composables/useToast';
import { getCaptchaImage } from '@/api/user';

const router = useRouter();
const userStore = useUserStore();
const toast = useToast();

// 邮箱注册表单（与后端 RegisterParams 对齐：username/email/password/confirmPassword/emailCode）
const form = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  emailCode: ''
});

const showPassword = ref(false);
const showConfirmPassword = ref(false);
const isLoading = ref(false);
const errors = ref<Record<string, string>>({});
const serverError = ref('');
const agreeTerms = ref(false);

// 邮箱验证码发送：倒计时 + 防重发
const isSendingCode = ref(false);
const countdown = ref(0);
let countdownTimer: ReturnType<typeof setInterval> | null = null;

function startCountdown(seconds: number) {
  countdown.value = seconds;
  if (countdownTimer) clearInterval(countdownTimer);
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      if (countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }
  }, 1000);
}

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer);
});

// 发送邮箱验证码
async function handleSendEmailCode() {
  errors.value.email = '';
  // 前端先校验邮箱格式，避免无效请求
  const email = form.value.email.trim();
  if (!email) {
    errors.value.email = '请先填写邮箱';
    return;
  }
  if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
    errors.value.email = '邮箱格式不正确';
    return;
  }

  isSendingCode.value = true;
  try {
    const { success, message } = await userStore.sendEmailCodeWithApi(email, 'register');
    if (success) {
      toast.success(message || '验证码已发送，请查收邮件');
      startCountdown(60);
    } else {
      toast.error(message || '验证码发送失败');
    }
  } finally {
    isSendingCode.value = false;
  }
}

// 图形验证码（受后端 sys.account.captchaEnabled 开关控制；接口固定返回 captchaEnabled=true）
const captchaEnabled = ref(false);
const captchaImg = ref('');
const captchaUuid = ref('');
const captchaCode = ref('');
const captchaLoading = ref(false);

async function refreshCaptcha() {
  captchaLoading.value = true;
  try {
    const data = await getCaptchaImage();
    captchaEnabled.value = data.captchaEnabled;
    captchaImg.value = data.img;
    captchaUuid.value = data.uuid;
    captchaCode.value = '';
  } catch (error) {
    // 验证码拉取失败时不阻断注册（降级：不展示验证码）
    console.warn('获取验证码失败:', error);
    captchaEnabled.value = false;
    captchaImg.value = '';
    captchaUuid.value = '';
  } finally {
    captchaLoading.value = false;
  }
}

onMounted(() => {
  refreshCaptcha();
});

// 密码强度实时计算（供 UI 提示，最终校验仍由 zod 完成）
type StrengthLevel = { level: 0 | 1 | 2 | 3; label: string; color: string };
const passwordStrength = computed<StrengthLevel>(() => {
  const pwd = form.value.password || '';
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
  return [
    lv >= 1,
    lv >= 2,
    lv >= 3
  ];
});

async function handleRegister() {
  errors.value = {};
  serverError.value = '';

  // 协议同意校验（前端独立校验，zod schema 不含此项）
  if (!agreeTerms.value) {
    serverError.value = '请阅读并同意服务条款';
    return;
  }

  // 接入 zod 校验（含用户名格式、邮箱格式、密码大小写+数字规则、两次密码一致）
  const result = validateForm(registerSchema, form.value);
  if (!result.success) {
    errors.value = result.errors;
    return;
  }

  // 验证码前端校验：仅在开关开启时强制
  if (captchaEnabled.value && !captchaCode.value.trim()) {
    errors.value.code = '请输入验证码';
    return;
  }

  // 邮箱验证码前端校验
  if (!form.value.emailCode.trim()) {
    errors.value.emailCode = '请输入邮箱验证码';
    return;
  }

  isLoading.value = true;

  try {
    const { success, message } = await userStore.registerWithApi({
      username: form.value.username,
      email: form.value.email,
      password: form.value.password,
      confirmPassword: form.value.confirmPassword,
      code: captchaEnabled.value ? captchaCode.value.trim() : undefined,
      uuid: captchaEnabled.value ? captchaUuid.value : undefined,
      emailCode: form.value.emailCode.trim()
    });
    if (success) {
      toast.success('注册成功，请使用新账户登录');
      // 注册成功后跳转到登录页（不自动登录，更符合常见注册流程）
      router.push('/login');
    } else {
      serverError.value = message || '注册失败，请稍后重试';
      // 验证码为一次性凭证，失败后刷新
      refreshCaptcha();
    }
  } catch (error) {
    console.error('注册失败:', error);
    serverError.value = '注册失败，请稍后重试';
    refreshCaptcha();
  } finally {
    isLoading.value = false;
  }
}

function clearError(field: string) {
  if (errors.value[field]) {
    errors.value[field] = '';
  }
}

// 版权年份
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

      <!-- Register Card -->
      <div class="relative group">
        <!-- 发光边框 -->
        <div class="absolute -inset-1 bg-gradient-to-r from-amber-500 via-orange-500 to-yellow-500 rounded-3xl blur opacity-30 group-hover:opacity-50 transition-all duration-1000"></div>

        <!-- 卡片主体 -->
        <div class="relative bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-white/20">
          <!-- 顶部光效 -->
          <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-amber-500/50 to-transparent"></div>

          <div class="p-8">
            <div class="text-center mb-8">
              <h1 class="text-3xl font-bold text-slate-800 mb-2">创建账户</h1>
              <p class="text-slate-500 text-sm">记录学习、分享技能、安放心灵。</p>
            </div>

            <!-- Server Error -->
            <div v-if="serverError" class="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm flex items-start gap-2">
              <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{{ serverError }}</span>
            </div>

            <!-- Form -->
            <form @submit.prevent="handleRegister" class="space-y-4">
              <!-- Username -->
              <div class="group">
                <label for="register-username" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">用户名</label>
                <div class="relative">
                  <User class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="register-username"
                    v-model="form.username"
                    type="text"
                    placeholder="3-20位，字母/数字/下划线"
                    autocomplete="username"
                    @input="clearError('username')"
                    class="w-full pl-12 pr-4 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                    :class="{
                      'border-red-300 focus:border-red-400 bg-red-50': errors.username,
                      'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.username
                    }"
                    :disabled="isLoading"
                  />
                </div>
                <p v-if="errors.username" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.username }}
                </p>
              </div>

              <!-- Email -->
              <div class="group">
                <label for="register-email" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">邮箱</label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <Mail class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                    <input
                      id="register-email"
                      v-model="form.email"
                      type="email"
                      placeholder="请输入有效邮箱（用于接收验证码）"
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
                    @click="handleSendEmailCode"
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

              <!-- Email Verification Code -->
              <div class="group">
                <label for="register-email-code" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">邮箱验证码</label>
                <div class="relative">
                  <ShieldCheck class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="register-email-code"
                    v-model="form.emailCode"
                    type="text"
                    inputmode="numeric"
                    placeholder="请输入邮箱收到的 6 位验证码"
                    autocomplete="one-time-code"
                    maxlength="6"
                    @input="clearError('emailCode')"
                    class="w-full pl-12 pr-4 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800 tracking-widest"
                    :class="{
                      'border-red-300 focus:border-red-400 bg-red-50': errors.emailCode,
                      'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.emailCode
                    }"
                    :disabled="isLoading"
                  />
                </div>
                <p v-if="errors.emailCode" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.emailCode }}
                </p>
              </div>

              <!-- Password -->
              <div class="group">
                <label for="register-password" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="register-password"
                    v-model="form.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="6-20位，需含大小写字母和数字"
                    autocomplete="new-password"
                    @input="clearError('password')"
                    class="w-full pl-12 pr-12 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                    :class="{
                      'border-red-300 focus:border-red-400 bg-red-50': errors.password,
                      'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.password
                    }"
                    :disabled="isLoading"
                  />
                  <button
                    type="button"
                    @click="showPassword = !showPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-amber-500 transition-colors duration-300"
                    :disabled="isLoading"
                    :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                    :aria-pressed="showPassword"
                  >
                    <Eye v-if="!showPassword" class="w-5 h-5" />
                    <EyeOff v-else class="w-5 h-5" />
                  </button>
                </div>
                <!-- 密码强度实时提示 -->
                <div v-if="form.password" class="mt-2 flex items-center gap-2 ml-1">
                  <div class="flex gap-1 flex-1">
                    <div
                      v-for="(active, idx) in strengthBars"
                      :key="idx"
                      class="h-1 flex-1 rounded-full transition-all duration-300"
                      :style="{ backgroundColor: active ? passwordStrength.color : '#e2e8f0' }"
                    ></div>
                  </div>
                  <span class="text-xs font-medium" :style="{ color: passwordStrength.color }">
                    {{ passwordStrength.label }}
                  </span>
                </div>
                <p v-if="errors.password" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.password }}
                </p>
              </div>

              <!-- Confirm Password -->
              <div class="group">
                <label for="register-confirm-password" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">确认密码</label>
                <div class="relative">
                  <ShieldCheck class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="register-confirm-password"
                    v-model="form.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    placeholder="请再次输入密码"
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
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 hover:text-amber-500 transition-colors duration-300"
                    :disabled="isLoading"
                    :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
                    :aria-pressed="showConfirmPassword"
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

              <!-- Captcha -->
              <div v-if="captchaEnabled" class="group">
                <label for="register-captcha" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">验证码</label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <ShieldCheck class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                    <input
                      id="register-captcha"
                      v-model="captchaCode"
                      type="text"
                      placeholder="请输入图中结果"
                      autocomplete="off"
                      maxlength="10"
                      @input="clearError('code')"
                      class="w-full pl-12 pr-4 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                      :class="{
                        'border-red-300 focus:border-red-400 bg-red-50': errors.code,
                        'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.code
                      }"
                      :disabled="isLoading"
                    />
                  </div>
                  <button
                    type="button"
                    @click="refreshCaptcha"
                    :disabled="captchaLoading || isLoading"
                    class="relative h-[58px] w-[120px] flex-shrink-0 overflow-hidden rounded-2xl border-2 border-slate-200 bg-slate-50 hover:border-amber-400 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
                    aria-label="点击刷新验证码"
                  >
                    <img v-if="captchaImg" :src="captchaImg" alt="验证码" class="w-full h-full object-cover" />
                    <RefreshCw v-else class="w-5 h-5 text-slate-400 animate-spin" />
                    <span v-if="captchaLoading" class="absolute inset-0 bg-white/60 flex items-center justify-center">
                      <RefreshCw class="w-5 h-5 text-amber-500 animate-spin" />
                    </span>
                  </button>
                </div>
                <p v-if="errors.code" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.code }}
                </p>
              </div>

              <!-- Terms -->
              <div>
                <label class="flex items-start cursor-pointer group">
                  <div class="relative">
                    <input
                      v-model="agreeTerms"
                      type="checkbox"
                      class="sr-only peer"
                      :disabled="isLoading"
                    />
                    <div class="w-4 h-4 border-2 border-slate-300 rounded mt-1 transition-all duration-300 peer-checked:border-amber-500 peer-checked:bg-amber-50"></div>
                    <div class="absolute inset-0 flex items-center justify-center opacity-0 peer-checked:opacity-100 transition-opacity duration-300 mt-1">
                      <div class="w-2 h-2 bg-amber-500 rounded-full"></div>
                    </div>
                  </div>
                  <span class="ml-2 text-sm text-slate-500">
                    我已阅读并同意
                    <Link to="/agreement" class="text-amber-600 hover:text-amber-700 font-medium transition-colors">服务条款</Link>
                    和
                    <Link to="/agreement" class="text-amber-600 hover:text-amber-700 font-medium transition-colors">隐私政策</Link>
                  </span>
                </label>
              </div>

              <!-- Submit -->
              <button
                type="submit"
                :disabled="isLoading"
                class="relative w-full py-4 bg-gradient-to-r from-amber-500 via-orange-500 to-yellow-500 text-white font-semibold rounded-2xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden group shadow-lg shadow-orange-500/20 hover:shadow-orange-500/30"
              >
                <span class="relative flex items-center justify-center gap-2" v-if="!isLoading">
                  <span>创建账户</span>
                  <ArrowRight class="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </span>
                <span v-else class="relative flex items-center justify-center gap-2">
                  <span>注册中...</span>
                  <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                </span>
              </button>
            </form>
          </div>

          <!-- Footer -->
          <div class="px-8 py-6 bg-slate-50 border-t border-slate-100 text-center">
            <p class="text-slate-500 text-sm">
              已有账户？
              <Link to="/login" class="text-amber-600 hover:text-amber-700 font-semibold ml-1 transition-colors hover:underline underline-offset-4">立即登录</Link>
            </p>
          </div>
        </div>
      </div>

      <!-- Back Home -->
      <div class="text-center mt-8">
        <Link to="/" class="inline-flex items-center space-x-1 text-white/70 hover:text-white transition-colors group">
          <ArrowRight class="w-4 h-4 rotate-180 group-hover:-translate-x-0.5 transition-transform" />
          <span>返回首页</span>
        </Link>
      </div>

      <!-- 版权提示 -->
      <div class="mt-8 text-center text-xs text-white/50">
        Copyright © {{ copyrightYear }} 旭林知行 · 京ICP备xxxxxxxx号-2
      </div>
    </div>
  </div>
</template>
