<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink as Link, useRouter } from 'vue-router';
import { Eye, EyeOff, Mail, Lock, User, ArrowRight, AlertCircle } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
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
  <div class="min-h-screen bg-gradient-to-br from-purple-50 via-blue-50 to-cyan-50 flex items-center justify-center py-12 px-4 flex flex-col">
    <div class="max-w-md w-full">
      <!-- Logo -->
      <div class="text-center mb-10">
        <Link to="/" class="inline-flex items-center space-x-2">
          <div class="w-12 h-12 bg-gradient-to-br from-purple-600 to-blue-600 rounded-2xl flex items-center justify-center">
            <span class="text-white font-bold text-xl">墨</span>
          </div>
          <span class="text-2xl font-bold bg-gradient-to-r from-purple-600 to-blue-600 bg-clip-text text-transparent">
            墨韵·智库
          </span>
        </Link>
      </div>

      <!-- Register Card -->
      <div class="bg-white rounded-3xl shadow-xl overflow-hidden">
        <div class="p-8">
          <h1 class="text-3xl font-bold text-gray-900 mb-2">创建账户</h1>
          <p class="text-gray-600 mb-8">加入我们，开始你的创作之旅</p>

          <!-- Server Error -->
          <div v-if="serverError" class="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm flex items-start gap-2">
            <AlertCircle class="w-4 h-4 flex-shrink-0 mt-0.5" />
            <span>{{ serverError }}</span>
          </div>

          <!-- Form -->
          <form @submit.prevent="handleRegister" class="space-y-5">
            <!-- Username -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
              <div class="relative">
                <User class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="form.username"
                  type="text"
                  placeholder="你的用户名"
                  @input="clearError('username')"
                  class="w-full pl-12 pr-4 py-3.5 border rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
                  :class="{ 'border-red-500': errors.username, 'border-gray-200': !errors.username }"
                  :disabled="isLoading"
                />
              </div>
              <p v-if="errors.username" class="mt-1.5 text-sm text-red-600 flex items-center gap-1">
                <AlertCircle class="w-3.5 h-3.5" />
                {{ errors.username }}
              </p>
            </div>

            <!-- Email -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">邮箱地址</label>
              <div class="relative">
                <Mail class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="your@email.com"
                  @input="clearError('email')"
                  class="w-full pl-12 pr-4 py-3.5 border rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
                  :class="{ 'border-red-500': errors.email, 'border-gray-200': !errors.email }"
                  :disabled="isLoading"
                />
              </div>
              <p v-if="errors.email" class="mt-1.5 text-sm text-red-600 flex items-center gap-1">
                <AlertCircle class="w-3.5 h-3.5" />
                {{ errors.email }}
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
                  placeholder="至少6个字符，包含大小写字母和数字"
                  @input="clearError('password')"
                  class="w-full pl-12 pr-12 py-3.5 border rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
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

            <!-- Confirm Password -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">确认密码</label>
              <div class="relative">
                <Lock class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  v-model="form.confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  placeholder="再次输入密码"
                  @input="clearError('confirmPassword')"
                  class="w-full pl-12 pr-12 py-3.5 border rounded-xl focus:outline-none focus:ring-2 focus:ring-purple-500 focus:border-transparent transition-all"
                  :class="{ 'border-red-500': errors.confirmPassword, 'border-gray-200': !errors.confirmPassword }"
                  :disabled="isLoading"
                />
                <button
                  type="button"
                  @click="showConfirmPassword = !showConfirmPassword"
                  class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition-colors"
                  :disabled="isLoading"
                >
                  <Eye v-if="!showConfirmPassword" class="w-5 h-5" />
                  <EyeOff v-else class="w-5 h-5" />
                </button>
              </div>
              <p v-if="errors.confirmPassword" class="mt-1.5 text-sm text-red-600 flex items-center gap-1">
                <AlertCircle class="w-3.5 h-3.5" />
                {{ errors.confirmPassword }}
              </p>
            </div>

            <!-- Terms -->
            <div>
              <label class="flex items-start">
                <input
                  v-model="agreeTerms"
                  type="checkbox"
                  class="w-4 h-4 mt-1 rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                  :disabled="isLoading"
                />
                <span class="ml-2 text-sm text-gray-600">
                  我已阅读并同意
                  <Link to="/agreement" class="text-purple-600 hover:text-purple-700 font-medium">服务条款</Link>
                  和
                  <Link to="/agreement" class="text-purple-600 hover:text-purple-700 font-medium">隐私政策</Link>
                </span>
              </label>
            </div>

            <!-- Submit -->
            <button
              type="submit"
              :disabled="isLoading"
              class="w-full py-3.5 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-xl font-semibold hover:shadow-lg hover:-translate-y-0.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
            >
              <span v-if="!isLoading">创建账户</span>
              <span v-else>注册中...</span>
            </button>
          </form>
        </div>

        <!-- Footer -->
        <div class="px-8 py-6 bg-gray-50 border-t border-gray-100 text-center">
          <p class="text-gray-600">
            已有账户？
            <Link to="/login" class="text-purple-600 hover:text-purple-700 font-semibold ml-1">立即登录</Link>
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
