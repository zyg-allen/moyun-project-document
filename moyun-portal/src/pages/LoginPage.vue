<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink as Link, useRouter, useRoute } from 'vue-router';
import { 
  Eye, EyeOff, Lock, ArrowRight, AlertCircle, 
  Smartphone, MessageSquare, User 
} from 'lucide-vue-next';
import loginBackground from '@/assets/images/login-background.jpg';
import { useUserStore } from '@/stores/user';
import { loginSchema, validateForm } from '@/utils/validation';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const toast = useToast();

// 登录方式切换
const loginType = ref<'password' | 'sms'>('password');

// 密码登录表单
const passwordForm = ref({
  username: '',
  password: ''
});

// 短信登录表单
const smsForm = ref({
  phone: '',
  code: ''
});

const showPassword = ref(false);
const isLoading = ref(false);
const isSendingCode = ref(false);
const countdown = ref(0);
const errors = ref<Record<string, string>>({});
const serverError = ref('');

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
  if (!smsForm.value.phone || !/^1[3-9]\d{9}$/.test(smsForm.value.phone)) {
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

// 密码登录
async function handlePasswordLogin() {
  errors.value = {};
  serverError.value = '';

  const result = validateForm(loginSchema, passwordForm.value);
  
  if (!result.success) {
    errors.value = result.errors;
    return;
  }

  isLoading.value = true;

  try {
    const { success, message } = await userStore.loginWithApi(passwordForm.value);
    if (success) {
      // 登录成功后，根据 redirect 参数跳转到原页面
      const redirect = route.query.redirect as string;
      if (redirect) {
        router.push(redirect);
      } else {
        router.push('/');
      }
    } else {
      serverError.value = message || '登录失败，请检查用户名和密码';
    }
  } catch (error) {
    console.error('登录失败:', error);
    serverError.value = '登录失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
}

// 短信登录
async function handleSmsLogin() {
  errors.value = {};
  serverError.value = '';

  if (!smsForm.value.phone || !/^1[3-9]\d{9}$/.test(smsForm.value.phone)) {
    errors.value.phone = '请输入正确的手机号';
    return;
  }
  if (!smsForm.value.code || smsForm.value.code.length < 4) {
    errors.value.code = '请输入正确的验证码';
    return;
  }

  isLoading.value = true;

  try {
    // 模拟短信登录
    await new Promise(resolve => setTimeout(resolve, 1000));
    // 登录成功后，根据 redirect 参数跳转到原页面
    const redirect = route.query.redirect as string;
    if (redirect) {
      router.push(redirect);
    } else {
      router.push('/');
    }
  } catch (error) {
    console.error('登录失败:', error);
    serverError.value = '登录失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
}

// 第三方登录
function handleSocialLogin(type: 'wechat' | 'gitee') {
  toast.info(`${type === 'wechat' ? '微信' : 'Gitee'}登录功能开发中...`);
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

      <!-- Login Card -->
      <div class="relative group">
        <!-- 发光边框 -->
        <div class="absolute -inset-1 bg-gradient-to-r from-amber-500 via-orange-500 to-yellow-500 rounded-3xl blur opacity-30 group-hover:opacity-50 transition-all duration-1000"></div>
        
        <!-- 卡片主体 -->
        <div class="relative bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-white/20">
          <!-- 顶部光效 -->
          <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-amber-500/50 to-transparent"></div>
          
          <div class="p-8">
            <div class="text-center mb-8">
              <h1 class="text-3xl font-bold text-slate-800 mb-2">欢迎回来</h1>
              <p class="text-slate-500 text-sm">在浮躁的世界，留一页纸给灵魂。</p>
            </div>

            <!-- Server Error -->
            <div v-if="serverError" class="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm flex items-start gap-2">
              <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>{{ serverError }}</span>
            </div>

            <!-- 登录方式切换 -->
            <div class="flex gap-2 mb-8 bg-slate-100 p-1.5 rounded-2xl">
              <button 
                @click="loginType = 'password'"
                class="flex-1 py-2.5 px-4 rounded-xl text-sm font-medium transition-all duration-300 flex items-center justify-center gap-2"
                :class="loginType === 'password' 
                  ? 'bg-white text-slate-800 shadow-md' 
                  : 'text-slate-500 hover:text-slate-700'">
                <User class="w-4 h-4" />
                <span>密码登录</span>
              </button>
              <button 
                @click="loginType = 'sms'"
                class="flex-1 py-2.5 px-4 rounded-xl text-sm font-medium transition-all duration-300 flex items-center justify-center gap-2"
                :class="loginType === 'sms' 
                  ? 'bg-white text-slate-800 shadow-md' 
                  : 'text-slate-500 hover:text-slate-700'">
                <Smartphone class="w-4 h-4" />
                <span>短信登录</span>
              </button>
            </div>

            <!-- 密码登录表单 -->
            <form v-if="loginType === 'password'" @submit.prevent="handlePasswordLogin" class="space-y-5">
              <!-- Username -->
              <div class="group">
                <label class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">用户名</label>
                <div class="relative">
                  <User class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    v-model="passwordForm.username"
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

              <!-- Password -->
              <div class="group">
                <label class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    v-model="passwordForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
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

              <!-- Remember & Forgot -->
              <div class="flex items-center justify-between">
                <label class="flex items-center cursor-pointer group">
                  <div class="relative">
                    <input type="checkbox" class="sr-only peer" />
                    <div class="w-4 h-4 border-2 border-slate-300 rounded transition-all duration-300 peer-checked:border-amber-500 peer-checked:bg-amber-50"></div>
                    <div class="absolute inset-0 flex items-center justify-center opacity-0 peer-checked:opacity-100 transition-opacity duration-300">
                      <div class="w-2 h-2 bg-amber-500 rounded-full"></div>
                    </div>
                  </div>
                  <span class="ml-2 text-sm text-slate-500 group-hover:text-slate-700 transition-colors">记住我</span>
                </label>
                <a href="#" class="text-sm text-amber-600 hover:text-amber-700 font-medium transition-colors hover:underline underline-offset-4">忘记密码？</a>
              </div>

              <!-- Submit -->
              <button
                type="submit"
                :disabled="isLoading"
                class="relative w-full py-4 bg-gradient-to-r from-amber-500 via-orange-500 to-yellow-500 text-white font-semibold rounded-2xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden group shadow-lg shadow-orange-500/20 hover:shadow-orange-500/30"
              >
                <span class="relative flex items-center justify-center gap-2" v-if="!isLoading">
                  <span>登录</span>
                  <ArrowRight class="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </span>
                <span v-else class="relative flex items-center justify-center gap-2">
                  <span>登录中...</span>
                  <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                </span>
              </button>
            </form>

            <!-- 短信登录表单 -->
            <form v-else @submit.prevent="handleSmsLogin" class="space-y-5">
              <!-- Phone -->
              <div class="group">
                <label class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">手机号</label>
                <div class="relative">
                  <Smartphone class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    v-model="smsForm.phone"
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
                <label class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">验证码</label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <MessageSquare class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                    <input
                      v-model="smsForm.code"
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

              <!-- Submit -->
              <button
                type="submit"
                :disabled="isLoading"
                class="relative w-full py-4 bg-gradient-to-r from-amber-500 via-orange-500 to-yellow-500 text-white font-semibold rounded-2xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden group shadow-lg shadow-orange-500/20 hover:shadow-orange-500/30"
              >
                <span class="relative flex items-center justify-center gap-2" v-if="!isLoading">
                  <span>登录</span>
                  <ArrowRight class="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </span>
                <span v-else class="relative flex items-center justify-center gap-2">
                  <span>登录中...</span>
                  <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                </span>
              </button>
            </form>

            <!-- Divider -->
            <div class="my-8 flex items-center gap-4">
              <div class="flex-1 h-px bg-gradient-to-r from-transparent via-slate-300 to-transparent"></div>
              <span class="text-xs text-slate-400 tracking-wider">其他登录方式</span>
              <div class="flex-1 h-px bg-gradient-to-r from-transparent via-slate-300 to-transparent"></div>
            </div>

            <!-- Social Login -->
            <div class="grid grid-cols-2 gap-4">
              <button 
                @click="handleSocialLogin('wechat')"
                class="py-3.5 bg-white border-2 border-slate-200 rounded-2xl hover:border-green-400 hover:bg-green-50 transition-all duration-300 hover:shadow-lg hover:shadow-green-500/10 group">
                <div class="flex items-center justify-center gap-2">
                  <MessageSquare class="w-5 h-5 text-slate-500 group-hover:text-green-500 transition-colors" />
                  <span class="text-sm font-medium text-slate-600 group-hover:text-green-600">微信登录</span>
                </div>
              </button>
              <button 
                @click="handleSocialLogin('gitee')"
                class="py-3.5 bg-white border-2 border-slate-200 rounded-2xl hover:border-red-400 hover:bg-red-50 transition-all duration-300 hover:shadow-lg hover:shadow-red-500/10 group">
                <div class="flex items-center justify-center gap-2">
                  <svg class="w-5 h-5 text-slate-500 group-hover:text-red-500 transition-colors" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M11.984 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 11.984 0zm5.362 7.488c.19 2.118-.948 3.864-3.063 4.87l-.003.002 1.515 4.546a.24.24 0 0 1-.095.274.238.238 0 0 1-.285-.03l-5.295-3.95-2.037 1.52a.29.29 0 0 1-.328-.01.288.288 0 0 1-.099-.222v-1.718l-2.363 1.76a.239.239 0 0 1-.331-.07.238.238 0 0 1-.03-.277l2.71-4.34-.003-.003c-1.722-.97-2.445-2.61-1.98-4.518.396-1.62 1.772-2.727 3.643-2.727.84 0 1.636.263 2.298.744a4.7 4.7 0 0 1 2.191-.693c2.188 0 3.98 1.62 4.25 3.834h-.002z"/>
                  </svg>
                  <span class="text-sm font-medium text-slate-600 group-hover:text-red-600">Gitee登录</span>
                </div>
              </button>
            </div>
          </div>

          <!-- Footer -->
          <div class="px-8 py-6 bg-slate-50 border-t border-slate-100 text-center">
            <p class="text-slate-500 text-sm">
              还没有账户？
              <Link to="/register" class="text-amber-600 hover:text-amber-700 font-semibold ml-1 transition-colors hover:underline underline-offset-4">立即注册</Link>
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
