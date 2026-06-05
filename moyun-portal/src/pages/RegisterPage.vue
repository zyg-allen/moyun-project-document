<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { Eye, EyeOff, Mail, Lock, User, ArrowRight, AlertCircle } from 'lucide-vue-next';
import { useUserStore } from '@/stores/user';
import { registerSchema, validateForm } from '@/utils/validation';

const router = useRouter();
const userStore = useUserStore();
const form = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const showPassword = ref(false);
const showConfirmPassword = ref(false);
const isLoading = ref(false);
const errors = ref<Record<string, string>>({});
const serverError = ref('');
const agreeTerms = ref(false);

async function handleRegister() {
  errors.value = {};
  serverError.value = '';

  const result = validateForm(registerSchema, form.value);
  
  if (!result.success) {
    errors.value = result.errors;
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
      email: form.value.email,
      password: form.value.password
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
  <div class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex flex-col items-center justify-center py-12 px-4 relative overflow-hidden">
    <!-- 背景网格 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute inset-0" style="
        background-image: 
          linear-gradient(rgba(168, 85, 247, 0.03) 1px, transparent 1px),
          linear-gradient(90deg, rgba(168, 85, 247, 0.03) 1px, transparent 1px);
        background-size: 50px 50px;
      "></div>
    </div>

    <!-- 霓虹发光装饰 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-40 -left-40 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl animate-pulse"></div>
      <div class="absolute -bottom-40 -right-40 w-96 h-96 bg-blue-500/10 rounded-full blur-3xl animate-pulse" style="animation-delay: 1s;"></div>
      <div class="absolute top-1/3 right-1/4 w-64 h-64 bg-cyan-500/5 rounded-full blur-3xl animate-pulse" style="animation-delay: 2s;"></div>
    </div>
    
    <div class="max-w-md w-full relative z-10">
      <!-- Logo -->
      <div class="text-center mb-10">
        <Link to="/" class="inline-flex items-center space-x-3 transition-all duration-300 hover:scale-105 hover:shadow-lg">
          <div class="w-16 h-16 bg-gradient-to-br from-purple-500 to-blue-500 rounded-2xl flex items-center justify-center shadow-2xl shadow-purple-500/25 border border-purple-400/30">
            <span class="text-white font-bold text-3xl">墨</span>
          </div>
          <div class="text-left">
            <span class="block text-3xl font-bold bg-gradient-to-r from-purple-400 via-blue-400 to-cyan-400 bg-clip-text text-transparent">
              墨韵·智库
            </span>
            <span class="block text-xs text-purple-300/60 tracking-widest mt-1">KNOWLEDGE HUB</span>
          </div>
        </Link>
      </div>

      <!-- Register Card -->
      <div class="relative group">
        <!-- 霓虹边框 -->
        <div class="absolute -inset-1 bg-gradient-to-r from-purple-500 via-blue-500 to-cyan-500 rounded-3xl blur opacity-30 group-hover:opacity-50 transition-all duration-1000"></div>
        
        <!-- 卡片主体 -->
        <div class="relative bg-slate-900/90 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-slate-700/50">
          <!-- 顶部光效 -->
          <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-purple-500/50 to-transparent"></div>
          
          <div class="p-8">
            <div class="text-center mb-8">
              <h1 class="text-3xl font-bold bg-gradient-to-r from-white via-purple-100 to-cyan-100 bg-clip-text text-transparent mb-2">创建账户</h1>
              <p class="text-purple-300/80 text-sm mb-1">开始你的书写之旅。</p>
              <p class="text-slate-500 text-xs">记录学习、分享技能、安放心灵。</p>
            </div>

            <!-- Server Error -->
            <div v-if="serverError" class="mb-6 p-4 bg-red-950/50 border border-red-500/30 rounded-xl text-red-300 text-sm flex items-start gap-2">
              <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5 text-red-400" />
              <span>{{ serverError }}</span>
            </div>

            <!-- Form -->
            <form @submit.prevent="handleRegister" class="space-y-4">
              <!-- Username -->
              <div class="group">
                <label class="block text-xs font-medium text-purple-300 mb-2 ml-1 tracking-wider">用户名</label>
                <div class="relative">
                  <User class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-purple-400/60 group-focus-within:text-purple-400 transition-colors" />
                  <input
                    v-model="form.username"
                    type="text"
                    placeholder="你的用户名"
                    @input="clearError('username')"
                    class="w-full pl-12 pr-4 py-4 bg-slate-800/50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-600 text-slate-200"
                    :class="{
                      'border-red-500/50 focus:border-red-400 bg-red-950/20': errors.username,
                      'border-slate-700/50 focus:border-purple-400 focus:shadow-lg focus:shadow-purple-500/25': !errors.username
                    }"
                    :disabled="isLoading"
                  />
                  <div class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-transparent via-purple-400 to-transparent transition-all duration-300 group-focus-within:w-full"></div>
                </div>
                <p v-if="errors.username" class="mt-2 text-xs text-red-400 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.username }}
                </p>
              </div>

              <!-- Email -->
              <div class="group">
                <label class="block text-xs font-medium text-purple-300 mb-2 ml-1 tracking-wider">邮箱地址</label>
                <div class="relative">
                  <Mail class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-purple-400/60 group-focus-within:text-purple-400 transition-colors" />
                  <input
                    v-model="form.email"
                    type="email"
                    placeholder="your@email.com"
                    @input="clearError('email')"
                    class="w-full pl-12 pr-4 py-4 bg-slate-800/50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-600 text-slate-200"
                    :class="{
                      'border-red-500/50 focus:border-red-400 bg-red-950/20': errors.email,
                      'border-slate-700/50 focus:border-purple-400 focus:shadow-lg focus:shadow-purple-500/25': !errors.email
                    }"
                    :disabled="isLoading"
                  />
                  <div class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-transparent via-purple-400 to-transparent transition-all duration-300 group-focus-within:w-full"></div>
                </div>
                <p v-if="errors.email" class="mt-2 text-xs text-red-400 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.email }}
                </p>
              </div>

              <!-- Password -->
              <div class="group">
                <label class="block text-xs font-medium text-purple-300 mb-2 ml-1 tracking-wider">密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-purple-400/60 group-focus-within:text-purple-400 transition-colors" />
                  <input
                    v-model="form.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="至少6个字符，包含大小写字母和数字"
                    @input="clearError('password')"
                    class="w-full pl-12 pr-12 py-4 bg-slate-800/50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-600 text-slate-200"
                    :class="{
                      'border-red-500/50 focus:border-red-400 bg-red-950/20': errors.password,
                      'border-slate-700/50 focus:border-purple-400 focus:shadow-lg focus:shadow-purple-500/25': !errors.password
                    }"
                    :disabled="isLoading"
                  />
                  <div class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-transparent via-purple-400 to-transparent transition-all duration-300 group-focus-within:w-full"></div>
                  <button
                    type="button"
                    @click="showPassword = !showPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-purple-400 transition-colors duration-300"
                    :disabled="isLoading"
                  >
                    <Eye v-if="!showPassword" class="w-5 h-5" />
                    <EyeOff v-else class="w-5 h-5" />
                  </button>
                </div>
                <p v-if="errors.password" class="mt-2 text-xs text-red-400 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.password }}
                </p>
              </div>

              <!-- Confirm Password -->
              <div class="group">
                <label class="block text-xs font-medium text-purple-300 mb-2 ml-1 tracking-wider">确认密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-purple-400/60 group-focus-within:text-purple-400 transition-colors" />
                  <input
                    v-model="form.confirmPassword"
                    :type="showConfirmPassword ? 'text' : 'password'"
                    placeholder="再次输入密码"
                    @input="clearError('confirmPassword')"
                    class="w-full pl-12 pr-12 py-4 bg-slate-800/50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-600 text-slate-200"
                    :class="{
                      'border-red-500/50 focus:border-red-400 bg-red-950/20': errors.confirmPassword,
                      'border-slate-700/50 focus:border-purple-400 focus:shadow-lg focus:shadow-purple-500/25': !errors.confirmPassword
                    }"
                    :disabled="isLoading"
                  />
                  <div class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-transparent via-purple-400 to-transparent transition-all duration-300 group-focus-within:w-full"></div>
                  <button
                    type="button"
                    @click="showConfirmPassword = !showConfirmPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-purple-400 transition-colors duration-300"
                    :disabled="isLoading"
                  >
                    <Eye v-if="!showConfirmPassword" class="w-5 h-5" />
                    <EyeOff v-else class="w-5 h-5" />
                  </button>
                </div>
                <p v-if="errors.confirmPassword" class="mt-2 text-xs text-red-400 flex items-center gap-1 ml-1">
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
                    <div class="w-4 h-4 border-2 border-slate-600 rounded mt-1 transition-all duration-300 peer-checked:border-purple-400 peer-checked:bg-purple-400/20"></div>
                    <div class="absolute inset-0 flex items-center justify-center opacity-0 peer-checked:opacity-100 transition-opacity duration-300 mt-1">
                      <div class="w-2 h-2 bg-purple-400 rounded-full"></div>
                    </div>
                  </div>
                  <span class="ml-2 text-sm text-slate-400">
                    我已阅读并同意
                    <Link to="/agreement" class="text-purple-400 hover:text-purple-300 font-medium transition-colors">服务条款</Link>
                    和
                    <Link to="/agreement" class="text-purple-400 hover:text-purple-300 font-medium transition-colors">隐私政策</Link>
                  </span>
                </label>
              </div>

              <!-- Submit -->
              <button
                type="submit"
                :disabled="isLoading"
                class="relative w-full py-4 bg-gradient-to-r from-purple-500 via-blue-500 to-cyan-500 text-white font-semibold rounded-2xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden group"
              >
                <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000"></div>
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
          <div class="px-8 py-6 bg-slate-900/50 border-t border-slate-800/50 text-center">
            <p class="text-slate-500 text-sm">
              已有账户？
              <Link to="/login" class="text-purple-400 hover:text-purple-300 font-semibold ml-1 transition-colors hover:underline underline-offset-4">立即登录</Link>
            </p>
          </div>
        </div>
      </div>

      <!-- Back Home -->
      <div class="text-center mt-8">
        <Link to="/" class="inline-flex items-center space-x-1 text-slate-500 hover:text-purple-400 transition-colors group">
          <ArrowRight class="w-4 h-4 rotate-180 group-hover:-translate-x-0.5 transition-transform" />
          <span>返回首页</span>
        </Link>
      </div>

      <!-- 版权提示 -->
      <div class="mt-8 text-center text-xs text-slate-600">
        Copyright © 2026 墨韵·智库 · 京ICP备xxxxxxxx号-2
      </div>
    </div>
  </div>
</template>
