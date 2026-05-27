<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { Eye, EyeOff, Mail, Lock, ArrowRight, AlertCircle } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
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
  <div class="min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50 flex items-center justify-center py-12 px-4 flex flex-col">
    <div class="max-w-md w-full">
      <!-- Logo -->
      <div class="text-center mb-10">
        <Link to="/" class="inline-flex items-center space-x-2">
          <div class="w-12 h-12 bg-gradient-to-br from-red-600 to-orange-600 rounded-2xl flex items-center justify-center">
            <span class="text-white font-bold text-xl">墨</span>
          </div>
          <span class="text-2xl font-bold bg-gradient-to-r from-red-600 to-orange-600 bg-clip-text text-transparent">
            墨韵·智库
          </span>
        </Link>
      </div>

      <!-- Login Card -->
      <div class="bg-white rounded-3xl shadow-xl overflow-hidden">
        <div class="p-8">
          <h1 class="text-3xl font-bold text-gray-900 mb-2">欢迎回来</h1>
          <p class="text-gray-600 mb-8">登录你的账户继续探索</p>

          <!-- Server Error -->
          <div v-if="serverError" class="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm flex items-start gap-2">
            <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
            <span>{{ serverError }}</span>
          </div>

          <!-- Form -->
          <form @submit.prevent="handleLogin" class="space-y-6">
            <!-- Username -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
              <div class="relative">
                <Mail class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="form.username"
                  type="text"
                  placeholder="请输入用户名"
                  @input="clearError('username')"
                  class="w-full pl-12 pr-4 py-3.5 border rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all"
                  :class="{ 'border-red-500': errors.username, 'border-gray-200': !errors.username }"
                  :disabled="isLoading"
                />
              </div>
              <p v-if="errors.username" class="mt-1.5 text-sm text-red-600 flex items-center gap-1">
                <AlertCircle class="w-3.5 h-3.5" />
                {{ errors.username }}
              </p>
            </div>

            <!-- Password -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
              <div class="relative">
                <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="••••••••"
                  @input="clearError('password')"
                  class="w-full pl-12 pr-12 py-3.5 border rounded-xl focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent transition-all"
                  :class="{ 'border-red-500': errors.password, 'border-gray-200': !errors.password }"
                  :disabled="isLoading"
                />
                <button
                  type="button"
                  @click="showPassword = !showPassword"
                  class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                  :disabled="isLoading"
                >
                  <Eye v-if="!showPassword" class="w-5 h-5" />
                  <EyeOff v-else class="w-5 h-5" />
                </button>
              </div>
              <p v-if="errors.password" class="mt-1.5 text-sm text-red-600 flex items-center gap-1">
                <AlertCircle class="w-3.5 h-3.5" />
                {{ errors.password }}
              </p>
            </div>

            <!-- Remember & Forgot -->
            <div class="flex items-center justify-between">
              <label class="flex items-center">
                <input type="checkbox" class="w-4 h-4 rounded border-gray-300 text-red-600 focus:ring-red-500" />
                <span class="ml-2 text-sm text-gray-600">记住我</span>
              </label>
              <a href="#" class="text-sm text-red-600 hover:text-red-700 font-medium">忘记密码？</a>
            </div>

            <!-- Submit -->
            <button
              type="submit"
              :disabled="isLoading"
              class="w-full py-3.5 bg-gradient-to-r from-red-600 to-orange-600 text-white rounded-xl font-semibold hover:shadow-lg hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
            >
              <span v-if="!isLoading">登录</span>
              <span v-else>登录中...</span>
            </button>
          </form>

          <!-- Divider -->
          <div class="my-8 flex items-center">
            <div class="flex-1 border-t border-gray-200" />
            <span class="mx-4 text-sm text-gray-500">或</span>
            <div class="flex-1 border-t border-gray-200" />
          </div>

          <!-- Social Login -->
          <div class="grid grid-cols-3 gap-4">
            <button class="py-3 border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors">
              <svg class="w-5 h-5 mx-auto" viewBox="0 0 24 24">
                <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
              </svg>
            </button>
            <button class="py-3 border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors">
              <svg class="w-5 h-5 mx-auto" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
              </svg>
            </button>
            <button class="py-3 border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors">
              <svg class="w-5 h-5 mx-auto text-gray-800" fill="currentColor" viewBox="0 0 24 24">
                <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- Footer -->
        <div class="px-8 py-6 bg-gray-50 border-t border-gray-100 text-center">
          <p class="text-gray-600">
            还没有账户？
            <Link to="/register" class="text-red-600 hover:text-red-700 font-semibold ml-1">立即注册</Link>
          </p>
        </div>
      </div>

      <!-- Back Home -->
      <div class="text-center mt-8">
        <Link to="/" class="inline-flex items-center space-x-1 text-gray-500 hover:text-gray-700 transition-colors">
          <ArrowRight class="w-4 h-4 rotate-180" />
          <span>返回首页</span>
        </Link>
      </div>

      <!-- 公共Footer组件 -->
      <div class="mt-8 sm:mt-10 lg:mt-12">
        <SiteFooter />
      </div>
    </div>
  </div>
</template>
