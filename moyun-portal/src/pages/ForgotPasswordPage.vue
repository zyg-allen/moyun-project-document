<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import {
  Mail, Lock, ArrowRight, AlertCircle, ShieldCheck, Eye, EyeOff, ArrowLeft, KeyRound
} from 'lucide-vue-next';
import loginBackground from '@/assets/images/login-background.jpg';
import { useUserStore } from '@/stores/user';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const userStore = useUserStore();
const toast = useToast();

// 找回密码表单：邮箱 → 验证码 → 新密码
const form = ref({
  email: '',
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
});

function clearError(field: string) {
  if (errors.value[field]) delete errors.value[field];
}

// 发送找回密码验证码
async function handleSendCode() {
  errors.value.email = '';
  const email = form.value.email.trim();
  if (!email) {
    errors.value.email = '请先填写注册邮箱';
    return;
  }
  if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) {
    errors.value.email = '邮箱格式不正确';
    return;
  }

  isSendingCode.value = true;
  try {
    const { success, message } = await userStore.sendEmailCodeWithApi(email, 'reset_password');
    if (success) {
      toast.success(message || '验证码已发送至邮箱');
      startCountdown(60);
    } else {
      toast.error(message || '验证码发送失败');
    }
  } finally {
    isSendingCode.value = false;
  }
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
  const code = form.value.code.trim();
  const newPassword = form.value.newPassword;
  const confirmPassword = form.value.confirmPassword;

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
    const { success, message } = await userStore.resetPasswordWithApi(email, code, newPassword);
    if (success) {
      serverSuccess.value = message || '密码重置成功';
      toast.success('密码重置成功，请使用新密码登录');
      setTimeout(() => router.push('/login'), 1500);
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
              墨韵·智库
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
            <div class="text-center mb-8">
              <h1 class="text-3xl font-bold text-slate-800 mb-2">找回密码</h1>
              <p class="text-slate-500 text-sm">输入注册邮箱，通过邮箱验证码重置密码。</p>
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
              <!-- Email -->
              <div class="group">
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

              <!-- Email Code -->
              <div class="group">
                <label for="forgot-code" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">邮箱验证码</label>
                <div class="relative">
                  <ShieldCheck class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="forgot-code"
                    v-model="form.code"
                    type="text"
                    inputmode="numeric"
                    placeholder="请输入邮箱收到的 6 位验证码"
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
        © {{ copyrightYear }} 墨韵智库 · 保留所有权利
      </p>
    </div>
  </div>
</template>
