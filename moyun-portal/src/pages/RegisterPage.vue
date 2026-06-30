<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { 
  Eye, EyeOff, Lock, User, ArrowRight, AlertCircle, Smartphone, MessageSquare
} from 'lucide-vue-next';
import loginBackground from '@/assets/images/login-background.jpg';
import { useUserStore } from '@/stores/user';
import { registerSchema, validateForm } from '@/utils/validation';

const router = useRouter();
const userStore = useUserStore();

const form = ref({
  username: '',
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
});

const showPassword = ref(false);
const showConfirmPassword = ref(false);
const isLoading = ref(false);
const isSendingCode = ref(false);
const countdown = ref(0);
const errors = ref<Record<string, string>>({});
const serverError = ref('');
const agreeTerms = ref(false);

// 倒计时
function startCountdown() {
  countdown.value = 60;
  const timer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      clearInterval(timer);
    }
  }, 1000);
}

// 发送短信验证码
async function sendSmsCode() {
  if (!form.value.phone || !/^1[3-9]\d{9}$/.test(form.value.phone)) {
    errors.value.phone = '请输入正确的手机号';
    return;
  }
  
  isSendingCode.value = true;
  try {
    // 模拟发送短信
    await new Promise(resolve => setTimeout(resolve, 1000));
    startCountdown();
  } catch (error) {
    serverError.value = '发送失败，请稍后重试';
  } finally {
    isSendingCode.value = false;
  }
}

async function handleRegister() {
  errors.value = {};
  serverError.value = '';

  // 验证手机号和验证码
  if (!form.value.phone || !/^1[3-9]\d{9}$/.test(form.value.phone)) {
    errors.value.phone = '请输入正确的手机号';
    return;
  }
  if (!form.value.code || form.value.code.length < 4) {
    errors.value.code = '请输入正确的验证码';
    return;
  }
  
  // 验证密码
  if (form.value.password !== form.value.confirmPassword) {
    errors.value.confirmPassword = '两次输入的密码不一致';
    return;
  }

  if (!agreeTerms.value) {
    serverError.value = '请阅读并同意服务条款';
    return;
  }

  isLoading.value = true;

  try {
    const { success, message } = await userStore.registerWithApi({
      username: form.value.username,
      email: form.value.phone, // 使用手机号作为邮箱注册
      password: form.value.password,
      confirmPassword: form.value.confirmPassword
    });
    if (success) {
      router.push('/');
    } else {
      serverError.value = message || '注册失败，请稍后重试';
    }
  } catch (error) {
    console.error('注册失败:', error);
    serverError.value = '注册失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
}

function clearError(field: string) {
  if (errors.value[field]) {
    errors.value[field] = '';
  }
}
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
                    placeholder="请输入用户名"
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

              <!-- Phone -->
              <div class="group">
                <label for="register-phone" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">手机号</label>
                <div class="relative">
                  <Smartphone class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="register-phone"
                    v-model="form.phone"
                    type="tel"
                    placeholder="请输入手机号"
                    @input="clearError('phone')"
                    class="w-full pl-12 pr-4 py-4 bg-slate-50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-400 text-slate-800"
                    :class="{
                      'border-red-300 focus:border-red-400 bg-red-50': errors.phone,
                      'border-slate-200 focus:border-amber-400 focus:shadow-lg focus:shadow-amber-500/10': !errors.phone
                    }"
                    :disabled="isLoading"
                  />
                </div>
                <p v-if="errors.phone" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.phone }}
                </p>
              </div>

              <!-- Verification Code -->
              <div class="group">
                <label for="register-code" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">验证码</label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <MessageSquare class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                    <input
                      id="register-code"
                      v-model="form.code"
                      type="text"
                      placeholder="请输入验证码"
                      @input="clearError('code')"
                      maxlength="6"
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
                    @click="sendSmsCode"
                    :disabled="isSendingCode || countdown > 0"
                    class="px-4 py-4 bg-slate-100 text-slate-700 font-medium rounded-2xl transition-all duration-300 hover:bg-slate-200 disabled:opacity-50 disabled:cursor-not-allowed whitespace-nowrap min-w-[110px]"
                  >
                    {{ countdown > 0 ? `${countdown}s` : (isSendingCode ? '发送中' : '获取验证码') }}
                  </button>
                </div>
                <p v-if="errors.code" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.code }}
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
                    placeholder="请设置密码（至少6位）"
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
                <p v-if="errors.password" class="mt-2 text-xs text-red-500 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.password }}
                </p>
              </div>

              <!-- Confirm Password -->
              <div class="group">
                <label for="register-confirm-password" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">确认密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="register-confirm-password"
                    v-model="form.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    placeholder="请再次输入密码"
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
        Copyright © 2026 墨韵·智库 · 京ICP备xxxxxxxx号-2
      </div>
    </div>
  </div>
</template>
