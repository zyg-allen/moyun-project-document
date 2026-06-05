<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { Eye, EyeOff, Mail, Lock, ArrowRight, AlertCircle } from 'lucide-vue-next';
import { useUserStore } from '@/stores/user';
import { loginSchema, validateForm } from '@/utils/validation';

const router = useRouter();
const userStore = useUserStore();

const form = ref({
  username: '',
  password: ''
});

const showPassword = ref(false);
const isLoading = ref(false);
const errors = ref<Record<string, string>>({});
const serverError = ref('');

async function handleLogin() {
  errors.value = {};
  serverError.value = '';

  const result = validateForm(loginSchema, form.value);
  
  if (!result.success) {
    errors.value = result.errors;
    return;
  }

  isLoading.value = true;

  try {
    const { success, message } = await userStore.loginWithApi(form.value);
    if (success) {
      router.push('/');
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
          linear-gradient(rgba(0, 242, 254, 0.03) 1px, transparent 1px),
          linear-gradient(90deg, rgba(0, 242, 254, 0.03) 1px, transparent 1px);
        background-size: 50px 50px;
      "></div>
    </div>

    <!-- 霓虹发光装饰 -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-40 -left-40 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl animate-pulse"></div>
      <div class="absolute -bottom-40 -right-40 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl animate-pulse" style="animation-delay: 1s;"></div>
      <div class="absolute top-1/3 right-1/4 w-64 h-64 bg-pink-500/5 rounded-full blur-3xl animate-pulse" style="animation-delay: 2s;"></div>
    </div>
    
    <div class="max-w-md w-full relative z-10">
      <!-- Logo -->
      <div class="text-center mb-10">
        <Link to="/" class="inline-flex items-center space-x-3 transition-all duration-300 hover:scale-105 hover:shadow-lg">
          <div class="w-16 h-16 bg-gradient-to-br from-cyan-500 to-purple-600 rounded-2xl flex items-center justify-center shadow-2xl shadow-cyan-500/25 border border-cyan-400/30">
            <span class="text-white font-bold text-3xl">墨</span>
          </div>
          <div class="text-left">
            <span class="block text-3xl font-bold bg-gradient-to-r from-cyan-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
              墨韵·智库
            </span>
            <span class="block text-xs text-cyan-300/60 tracking-widest mt-1">KNOWLEDGE HUB</span>
          </div>
        </Link>
      </div>

      <!-- Login Card -->
      <div class="relative group">
        <!-- 霓虹边框 -->
        <div class="absolute -inset-1 bg-gradient-to-r from-cyan-500 via-purple-500 to-pink-500 rounded-3xl blur opacity-30 group-hover:opacity-50 transition-all duration-1000"></div>
        
        <!-- 卡片主体 -->
        <div class="relative bg-slate-900/90 backdrop-blur-xl rounded-3xl shadow-2xl overflow-hidden border border-slate-700/50">
          <!-- 顶部光效 -->
          <div class="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-cyan-500/50 to-transparent"></div>
          
          <div class="p-8">
            <div class="text-center mb-8">
              <h1 class="text-3xl font-bold bg-gradient-to-r from-white via-cyan-100 to-purple-100 bg-clip-text text-transparent mb-2">欢迎回来</h1>
              <p class="text-cyan-300/80 text-sm mb-1">在浮躁的世界，留一页纸给灵魂。</p>
              <p class="text-slate-500 text-xs">我有一纸墨，足以慰风尘。</p>
            </div>

            <!-- Server Error -->
            <div v-if="serverError" class="mb-6 p-4 bg-red-950/50 border border-red-500/30 rounded-xl text-red-300 text-sm flex items-start gap-2 animate-shake">
              <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5 text-red-400" />
              <span>{{ serverError }}</span>
            </div>

            <!-- Form -->
            <form @submit.prevent="handleLogin" class="space-y-5">
              <!-- Username -->
              <div class="group">
                <label class="block text-xs font-medium text-cyan-300 mb-2 ml-1 tracking-wider">用户名</label>
                <div class="relative">
                  <Mail class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-cyan-400/60 group-focus-within:text-cyan-400 transition-colors" />
                  <input
                    v-model="form.username"
                    type="text"
                    placeholder="请输入用户名"
                    @input="clearError('username')"
                    class="w-full pl-12 pr-4 py-4 bg-slate-800/50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-600 text-slate-200"
                    :class="{
                      'border-red-500/50 focus:border-red-400 bg-red-950/20': errors.username,
                      'border-slate-700/50 focus:border-cyan-400 focus:shadow-lg focus:shadow-cyan-500/25': !errors.username
                    }"
                    :disabled="isLoading"
                  />
                  <!-- 输入框底部光效 -->
                  <div class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-transparent via-cyan-400 to-transparent transition-all duration-300 group-focus-within:w-full"></div>
                </div>
                <p v-if="errors.username" class="mt-2 text-xs text-red-400 flex items-center gap-1 ml-1">
                  <AlertCircle class="w-3.5 h-3.5" />
                  {{ errors.username }}
                </p>
              </div>

              <!-- Password -->
              <div class="group">
                <label class="block text-xs font-medium text-cyan-300 mb-2 ml-1 tracking-wider">密码</label>
                <div class="relative">
                  <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-cyan-400/60 group-focus-within:text-cyan-400 transition-colors" />
                  <input
                    v-model="form.password"
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="••••••••"
                    @input="clearError('password')"
                    class="w-full pl-12 pr-12 py-4 bg-slate-800/50 border-2 rounded-2xl focus:outline-none focus:ring-0 transition-all duration-300 placeholder:text-slate-600 text-slate-200"
                    :class="{
                      'border-red-500/50 focus:border-red-400 bg-red-950/20': errors.password,
                      'border-slate-700/50 focus:border-cyan-400 focus:shadow-lg focus:shadow-cyan-500/25': !errors.password
                    }"
                    :disabled="isLoading"
                  />
                  <!-- 输入框底部光效 -->
                  <div class="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-transparent via-cyan-400 to-transparent transition-all duration-300 group-focus-within:w-full"></div>
                  <button
                    type="button"
                    @click="showPassword = !showPassword"
                    class="absolute right-4 top-1/2 -translate-y-1/2 text-slate-500 hover:text-cyan-400 transition-colors duration-300"
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

              <!-- Remember & Forgot -->
              <div class="flex items-center justify-between">
                <label class="flex items-center cursor-pointer group">
                  <div class="relative">
                    <input type="checkbox" class="sr-only peer" />
                    <div class="w-4 h-4 border-2 border-slate-600 rounded transition-all duration-300 peer-checked:border-cyan-400 peer-checked:bg-cyan-400/20"></div>
                    <div class="absolute inset-0 flex items-center justify-center opacity-0 peer-checked:opacity-100 transition-opacity duration-300">
                      <div class="w-2 h-2 bg-cyan-400 rounded-full"></div>
                    </div>
                  </div>
                  <span class="ml-2 text-sm text-slate-400 group-hover:text-cyan-300 transition-colors">记住我</span>
                </label>
                <a href="#" class="text-sm text-cyan-400 hover:text-cyan-300 font-medium transition-colors hover:underline underline-offset-4">忘记密码？</a>
              </div>

              <!-- Submit -->
              <button
                type="submit"
                :disabled="isLoading"
                class="relative w-full py-4 bg-gradient-to-r from-cyan-500 via-purple-500 to-pink-500 text-white font-semibold rounded-2xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed overflow-hidden group"
              >
                <!-- 按钮光效 -->
                <div class="absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000"></div>
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
              <div class="flex-1 h-px bg-gradient-to-r from-transparent via-slate-600 to-transparent"></div>
              <span class="text-xs text-slate-500 tracking-wider">或</span>
              <div class="flex-1 h-px bg-gradient-to-r from-transparent via-slate-600 to-transparent"></div>
            </div>

            <!-- Social Login -->
            <div class="grid grid-cols-3 gap-3">
              <button class="py-3.5 bg-slate-800/50 border-2 border-slate-700/50 rounded-2xl hover:border-cyan-500/50 hover:bg-slate-800 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/10 group">
                <svg class="w-5 h-5 mx-auto text-slate-400 group-hover:text-cyan-400 transition-colors" viewBox="0 0 24 24">
                  <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
              </button>
              <button class="py-3.5 bg-slate-800/50 border-2 border-slate-700/50 rounded-2xl hover:border-cyan-500/50 hover:bg-slate-800 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/10 group">
                <svg class="w-5 h-5 mx-auto text-slate-400 group-hover:text-cyan-400 transition-colors" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                </svg>
              </button>
              <button class="py-3.5 bg-slate-800/50 border-2 border-slate-700/50 rounded-2xl hover:border-cyan-500/50 hover:bg-slate-800 transition-all duration-300 hover:shadow-lg hover:shadow-cyan-500/10 group">
                <svg class="w-5 h-5 mx-auto text-slate-400 group-hover:text-cyan-400 transition-colors" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- Footer -->
          <div class="px-8 py-6 bg-slate-900/50 border-t border-slate-800/50 text-center">
            <p class="text-slate-500 text-sm">
              还没有账户？
              <Link to="/register" class="text-cyan-400 hover:text-cyan-300 font-semibold ml-1 transition-colors hover:underline underline-offset-4">立即注册</Link>
            </p>
          </div>
        </div>
      </div>

      <!-- Back Home -->
      <div class="text-center mt-8">
        <Link to="/" class="inline-flex items-center space-x-1 text-slate-500 hover:text-cyan-400 transition-colors group">
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
