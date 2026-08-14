<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RouterLink as Link, useRouter, useRoute } from 'vue-router';
import { Eye, EyeOff, Lock, ArrowRight, AlertCircle, User, ShieldCheck, RefreshCw } from 'lucide-vue-next';
import loginBackground from '@/assets/images/login-background.jpg';
import { useUserStore } from '@/stores/user';
import { loginSchema, validateForm } from '@/utils/validation';
import { useToast } from '@/composables/useToast';
import { getCaptchaImage } from '@/api/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const toast = useToast();

// 密码登录表单
const passwordForm = ref({
  username: '',
  password: ''
});

const showPassword = ref(false);
const isLoading = ref(false);
const errors = ref<Record<string, string>>({});
const serverError = ref('');

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
    // 验证码拉取失败时不阻断登录（降级：不展示验证码）
    console.warn('获取验证码失败:', error);
    captchaEnabled.value = false;
    captchaImg.value = '';
    captchaUuid.value = '';
  } finally {
    captchaLoading.value = false;
  }
}

// 记住我：从 localStorage 读取上次记住的用户名
const REMEMBER_KEY = 'moyun:login:remember';
const rememberMe = ref(false);

onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY);
  if (saved) {
    passwordForm.value.username = saved;
    rememberMe.value = true;
  }
  // 进入页面即拉取验证码，避免提交时才发现需要填写
  refreshCaptcha();
});

// 密码登录
async function handlePasswordLogin() {
  errors.value = {};
  serverError.value = '';

  const result = validateForm(loginSchema, passwordForm.value);
  if (!result.success) {
    errors.value = result.errors;
    return;
  }

  // 验证码前端校验：仅在开关开启时强制
  if (captchaEnabled.value && !captchaCode.value.trim()) {
    errors.value.code = '请输入验证码';
    return;
  }

  isLoading.value = true;

  try {
    const { success, message } = await userStore.loginWithApi({
      username: passwordForm.value.username,
      password: passwordForm.value.password,
      code: captchaEnabled.value ? captchaCode.value.trim() : undefined,
      uuid: captchaEnabled.value ? captchaUuid.value : undefined
    });
    if (success) {
      // 登录成功：按 rememberMe 持久化/清除用户名
      if (rememberMe.value) {
        localStorage.setItem(REMEMBER_KEY, passwordForm.value.username);
      } else {
        localStorage.removeItem(REMEMBER_KEY);
      }
      // 根据 redirect 参数跳转回原页面，否则回首页
      const redirect = route.query.redirect as string;
      router.push(redirect || '/');
    } else {
      serverError.value = message || '登录失败，请检查用户名和密码';
      // 失败时清空密码，避免密码泄露风险
      passwordForm.value.password = '';
      // 验证码为一次性凭证，失败后刷新
      refreshCaptcha();
    }
  } catch (error) {
    console.error('登录失败:', error);
    serverError.value = '登录失败，请稍后重试';
    passwordForm.value.password = '';
    refreshCaptcha();
  } finally {
    isLoading.value = false;
  }
}

// 忘记密码（暂未实现，给出友好提示）
function handleForgotPassword() {
  toast.info('如需重置密码，请联系管理员或在注册页重新创建账户');
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

            <!-- 登录表单 -->
            <form @submit.prevent="handlePasswordLogin" class="space-y-5">
              <!-- Username -->
              <div class="group">
                <label for="login-username" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">用户名</label>
                <div class="relative">
                  <User class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="login-username"
                    v-model="passwordForm.username"
                    type="text"
                    placeholder="请输入用户名"
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

              <!-- Password -->
              <div class="group">
                <label for="login-password" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                  <input
                    id="login-password"
                    v-model="passwordForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
                    autocomplete="current-password"
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

              <!-- Captcha -->
              <div v-if="captchaEnabled" class="group">
                <label for="login-captcha" class="block text-xs font-medium text-slate-600 mb-2 ml-1 tracking-wider">验证码</label>
                <div class="flex gap-3">
                  <div class="relative flex-1">
                    <ShieldCheck class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400 group-focus-within:text-amber-500 transition-colors" />
                    <input
                      id="login-captcha"
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

              <!-- Remember & Forgot -->
              <div class="flex items-center justify-between">
                <label class="flex items-center cursor-pointer group">
                  <div class="relative">
                    <input v-model="rememberMe" type="checkbox" class="sr-only peer" />
                    <div class="w-4 h-4 border-2 border-slate-300 rounded transition-all duration-300 peer-checked:border-amber-500 peer-checked:bg-amber-50"></div>
                    <div class="absolute inset-0 flex items-center justify-center opacity-0 peer-checked:opacity-100 transition-opacity duration-300">
                      <div class="w-2 h-2 bg-amber-500 rounded-full"></div>
                    </div>
                  </div>
                  <span class="ml-2 text-sm text-slate-500 group-hover:text-slate-700 transition-colors">记住我</span>
                </label>
                <button type="button" @click="handleForgotPassword" class="text-sm text-amber-600 hover:text-amber-700 font-medium transition-colors hover:underline underline-offset-4">忘记密码？</button>
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
        Copyright © {{ copyrightYear }} 墨韵·智库 · 京ICP备xxxxxxxx号-2
      </div>
    </div>
  </div>
</template>
