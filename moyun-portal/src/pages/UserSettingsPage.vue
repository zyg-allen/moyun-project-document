<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft,
  Lock,
  Bell,
  Shield,
  AlertTriangle,
  Eye,
  EyeOff,
  Save,
  Check,
  UserCheck,
  Heart,
  MessageSquare,
  ThumbsUp
} from 'lucide-vue-next';
import { useUserStore } from '@/stores/user';
import { generateSeo } from '@/utils/seo';
import Breadcrumb from '@/components/Breadcrumb.vue';
import * as userApi from '@/api/user';
import type { UpdatePasswordParams, UpdateUserProfileParams } from '@/types/api';

const router = useRouter();
const userStore = useUserStore();

// 当前选中的设置菜单
const activeSection = ref('account');

// 密码修改表单
const passwordForm = ref<UpdatePasswordParams>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 通知设置
const notifySettings = ref({
  notifyComment: true,
  notifyLike: true,
  notifyFollow: true,
  notifySystem: true
});

// 隐私设置
const privacySettings = ref({
  privacyBookmark: true,
  privacyFollow: true,
  privacyEmail: false,
  privacyPhone: false
});

// 状态
const isLoading = ref(false);
const successMessage = ref('');
const errorMessage = ref('');

// 密码显示切换
const showOldPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

// 危险区域确认
const showDeleteConfirm = ref(false);
const deleteConfirmText = ref('');

// SEO
useHead(
  generateSeo({
    title: '账号设置',
    description: '管理账号安全、通知和隐私设置',
    keywords: ['账号设置', '安全设置', '用户设置'],
    type: 'website'
  })
);

onMounted(async () => {
  // 等待用户状态初始化
  if (!userStore.isUserInitialized) {
    await userStore.initializeUser();
  }

  // 检查是否登录
  if (!userStore.isAuthenticated) {
    router.push('/login');
    return;
  }

  // 加载用户设置
  const user = userStore.user;
  if (user) {
    notifySettings.value = {
      notifyComment: user.notifyComment !== undefined ? user.notifyComment : true,
      notifyLike: user.notifyLike !== undefined ? user.notifyLike : true,
      notifyFollow: user.notifyFollow !== undefined ? user.notifyFollow : true,
      notifySystem: user.notifySystem !== undefined ? user.notifySystem : true
    };
    privacySettings.value = {
      privacyBookmark: user.privacyBookmark !== undefined ? user.privacyBookmark : true,
      privacyFollow: user.privacyFollow !== undefined ? user.privacyFollow : true,
      privacyEmail: user.privacyEmail !== undefined ? user.privacyEmail : false,
      privacyPhone: user.privacyPhone !== undefined ? user.privacyPhone : false
    };
  }
});

// 菜单项目
const menuItems = [
  { id: 'account', label: '账号与安全', icon: Lock },
  { id: 'notification', label: '通知设置', icon: Bell },
  { id: 'privacy', label: '隐私设置', icon: Shield },
  { id: 'danger', label: '账号注销', icon: AlertTriangle }
];

// 返回
function goBack() {
  router.push('/user');
}

// 清除消息
function clearMessages() {
  successMessage.value = '';
  errorMessage.value = '';
}

// 显示成功消息
function showSuccess(message: string) {
  successMessage.value = message;
  errorMessage.value = '';
  setTimeout(() => {
    if (successMessage.value === message) {
      successMessage.value = '';
    }
  }, 3000);
}

// 显示错误消息
function showError(message: string) {
  errorMessage.value = message;
  successMessage.value = '';
  setTimeout(() => {
    if (errorMessage.value === message) {
      errorMessage.value = '';
    }
  }, 3000);
}

// 修改密码
async function updatePassword() {
  clearMessages();

  // 验证
  if (!passwordForm.value.oldPassword) {
    showError('请输入当前密码');
    return;
  }
  if (!passwordForm.value.newPassword) {
    showError('请输入新密码');
    return;
  }
  if (passwordForm.value.newPassword.length < 6) {
    showError('新密码长度不能少于6位');
    return;
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    showError('两次输入的密码不一致');
    return;
  }

  isLoading.value = true;
  try {
    const response = await userApi.updatePassword(passwordForm.value);
    if (response && (response.code === 200 || response.code === 0)) {
      showSuccess('密码修改成功');
      passwordForm.value = {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      };
    } else {
      showError(response.message || '密码修改失败');
    }
  } catch (error) {
    console.error('密码修改失败:', error);
    showError('密码修改失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
}

// 保存通知设置
async function saveNotifySettings() {
  clearMessages();
  isLoading.value = true;

  try {
    const params: UpdateUserProfileParams = { ...notifySettings.value };
    const result = await userStore.updateUserWithApi(params);
    if (result.success) {
      showSuccess('通知设置保存成功');
    } else {
      showError(result.message || '保存失败');
    }
  } catch (error) {
    console.error('保存失败:', error);
    showError('保存失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
}

// 保存隐私设置
async function savePrivacySettings() {
  clearMessages();
  isLoading.value = true;

  try {
    const params: UpdateUserProfileParams = { ...privacySettings.value };
    const result = await userStore.updateUserWithApi(params);
    if (result.success) {
      showSuccess('隐私设置保存成功');
    } else {
      showError(result.message || '保存失败');
    }
  } catch (error) {
    console.error('保存失败:', error);
    showError('保存失败，请稍后重试');
  } finally {
    isLoading.value = false;
  }
}

// 注销账号
function confirmDelete() {
  if (deleteConfirmText.value !== '注销账号') {
    showError('请在输入框中输入"注销账号"以确认');
    return;
  }
  showSuccess('账号注销申请已提交，请等待处理');
  showDeleteConfirm.value = false;
  deleteConfirmText.value = '';
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between gap-4">
          <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '个人中心', path: '/user' }, { label: '账号设置' }]" />
          <button
            @click="goBack"
            class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
          >
            <ArrowLeft class="w-4 h-4" />
            返回
          </button>
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="py-8 flex-1">
      <div class="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 提示信息 -->
        <div v-if="successMessage" class="mb-6 p-4 rounded-xl text-sm flex items-center gap-2" style="background-color: #dcfce7; color: #166534; border: 1px solid #86efac;">
          <Check class="w-5 h-5 flex-shrink-0" />
          {{ successMessage }}
        </div>
        <div v-if="errorMessage" class="mb-6 p-4 rounded-xl text-sm flex items-center gap-2" style="background-color: #fef2f2; color: #991b1b; border: 1px solid #fecaca;">
          <AlertTriangle class="w-5 h-5 flex-shrink-0" />
          {{ errorMessage }}
        </div>

        <div class="flex flex-col lg:flex-row gap-6">
          <!-- 左侧菜单 - 移动端变为顶部滚动 -->
          <div class="w-full lg:w-64 flex-shrink-0">
            <div class="lg:sticky lg:top-8">
              <!-- PC端: 垂直菜单 -->
              <nav class="hidden lg:block p-4 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h2 class="text-lg font-semibold mb-4" style="color: var(--theme-text);">设置</h2>
                <div class="space-y-1">
                  <button
                    v-for="item in menuItems"
                    :key="item.id"
                    @click="activeSection = item.id"
                    class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors"
                    :style="activeSection === item.id
                      ? { backgroundColor: 'var(--theme-primary)', color: 'white' }
                      : { color: 'var(--theme-text)', backgroundColor: 'transparent' }"
                  >
                    <component :is="item.icon" class="w-5 h-5" />
                    {{ item.label }}
                  </button>
                </div>
              </nav>

              <!-- 移动端: 水平滚动标签 -->
              <nav class="lg:hidden flex gap-2 overflow-x-auto pb-2 -mx-4 px-4 mb-4">
                <button
                  v-for="item in menuItems"
                  :key="item.id"
                  @click="activeSection = item.id"
                  class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-colors whitespace-nowrap"
                  :style="activeSection === item.id
                    ? { backgroundColor: 'var(--theme-primary)', color: 'white' }
                    : { color: 'var(--theme-text)', backgroundColor: 'var(--theme-surface)', border: '1px solid var(--theme-border)' }"
                >
                  <component :is="item.icon" class="w-4 h-4" />
                  {{ item.label }}
                </button>
              </nav>
            </div>
          </div>

          <!-- 右侧内容区 -->
          <div class="flex-1 min-w-0">
            <!-- 账号与安全 -->
            <div v-if="activeSection === 'account'" class="space-y-6">
              <!-- 修改密码 -->
              <div class="p-6 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h3 class="text-lg font-semibold mb-6 flex items-center gap-2" style="color: var(--theme-text);">
                  <Lock class="w-5 h-5" style="color: var(--theme-primary);" />
                  修改密码
                </h3>

                <div class="space-y-4">
                  <!-- 当前密码 -->
                  <div>
                    <label for="settings-old-password" class="block text-sm font-medium mb-2" style="color: var(--theme-text);">当前密码</label>
                    <div class="relative">
                      <input
                        id="settings-old-password"
                        v-model="passwordForm.oldPassword"
                        :type="showOldPassword ? 'text' : 'password'"
                        placeholder="请输入当前密码"
                        class="w-full px-4 py-3 pr-12 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                        style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                      />
                      <button
                        @click="showOldPassword = !showOldPassword"
                        class="absolute right-4 top-1/2 -translate-y-1/2"
                        style="color: var(--theme-text-secondary);"
                        :aria-label="showOldPassword ? '隐藏密码' : '显示密码'"
                        :aria-pressed="showOldPassword"
                      >
                        <Eye v-if="!showOldPassword" class="w-5 h-5" />
                        <EyeOff v-else class="w-5 h-5" />
                      </button>
                    </div>
                  </div>

                  <!-- 新密码 -->
                  <div>
                    <label for="settings-new-password" class="block text-sm font-medium mb-2" style="color: var(--theme-text);">新密码</label>
                    <div class="relative">
                      <input
                        id="settings-new-password"
                        v-model="passwordForm.newPassword"
                        :type="showNewPassword ? 'text' : 'password'"
                        placeholder="请输入新密码（至少6位）"
                        class="w-full px-4 py-3 pr-12 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                        style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                      />
                      <button
                        @click="showNewPassword = !showNewPassword"
                        class="absolute right-4 top-1/2 -translate-y-1/2"
                        style="color: var(--theme-text-secondary);"
                        :aria-label="showNewPassword ? '隐藏密码' : '显示密码'"
                        :aria-pressed="showNewPassword"
                      >
                        <Eye v-if="!showNewPassword" class="w-5 h-5" />
                        <EyeOff v-else class="w-5 h-5" />
                      </button>
                    </div>
                  </div>

                  <!-- 确认新密码 -->
                  <div>
                    <label for="settings-confirm-password" class="block text-sm font-medium mb-2" style="color: var(--theme-text);">确认新密码</label>
                    <div class="relative">
                      <input
                        id="settings-confirm-password"
                        v-model="passwordForm.confirmPassword"
                        :type="showConfirmPassword ? 'text' : 'password'"
                        placeholder="请再次输入新密码"
                        class="w-full px-4 py-3 pr-12 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                        style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                      />
                      <button
                        @click="showConfirmPassword = !showConfirmPassword"
                        class="absolute right-4 top-1/2 -translate-y-1/2"
                        style="color: var(--theme-text-secondary);"
                        :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'"
                        :aria-pressed="showConfirmPassword"
                      >
                        <Eye v-if="!showConfirmPassword" class="w-5 h-5" />
                        <EyeOff v-else class="w-5 h-5" />
                      </button>
                    </div>
                  </div>

                  <!-- 保存按钮 -->
                  <div class="pt-4">
                    <button
                      @click="updatePassword"
                      :disabled="isLoading"
                      class="flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-medium transition-colors"
                      style="background-color: var(--theme-primary); color: white;"
                    >
                      <Save class="w-4 h-4" />
                      {{ isLoading ? '保存中...' : '保存密码' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 通知设置 -->
            <div v-if="activeSection === 'notification'" class="space-y-6">
              <div class="p-6 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h3 class="text-lg font-semibold mb-6 flex items-center gap-2" style="color: var(--theme-text);">
                  <Bell class="w-5 h-5" style="color: var(--theme-primary);" />
                  通知设置
                </h3>

                <div class="space-y-4">
                  <!-- 评论通知 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <MessageSquare class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">评论通知</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">当你的文章收到评论时通知你</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="notifySettings.notifyComment" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="notifySettings.notifyComment ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="notifySettings.notifyComment ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>

                  <!-- 点赞通知 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <ThumbsUp class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">点赞通知</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">当你的文章或评论被点赞时通知你</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="notifySettings.notifyLike" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="notifySettings.notifyLike ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="notifySettings.notifyLike ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>

                  <!-- 关注通知 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <UserCheck class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">关注通知</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">当有用户关注你时通知你</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="notifySettings.notifyFollow" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="notifySettings.notifyFollow ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="notifySettings.notifyFollow ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>

                  <!-- 系统通知 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <Bell class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">系统通知</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">接收系统维护、活动等重要通知</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="notifySettings.notifySystem" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="notifySettings.notifySystem ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="notifySettings.notifySystem ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>
                </div>

                <!-- 保存按钮 -->
                <div class="pt-6">
                  <button
                    @click="saveNotifySettings"
                    :disabled="isLoading"
                    class="flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-medium transition-colors"
                    style="background-color: var(--theme-primary); color: white;"
                  >
                    <Save class="w-4 h-4" />
                    {{ isLoading ? '保存中...' : '保存设置' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- 隐私设置 -->
            <div v-if="activeSection === 'privacy'" class="space-y-6">
              <div class="p-6 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
                <h3 class="text-lg font-semibold mb-6 flex items-center gap-2" style="color: var(--theme-text);">
                  <Shield class="w-5 h-5" style="color: var(--theme-primary);" />
                  隐私设置
                </h3>

                <div class="space-y-4">
                  <!-- 公开收藏夹 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <Heart class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">公开收藏夹</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">其他用户可以查看你的收藏夹</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="privacySettings.privacyBookmark" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="privacySettings.privacyBookmark ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="privacySettings.privacyBookmark ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>

                  <!-- 允许关注 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <UserCheck class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">允许关注</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">其他用户可以关注你</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="privacySettings.privacyFollow" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="privacySettings.privacyFollow ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="privacySettings.privacyFollow ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>

                  <!-- 公开邮箱 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <MessageSquare class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">公开邮箱</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">在个人主页显示你的邮箱</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="privacySettings.privacyEmail" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="privacySettings.privacyEmail ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="privacySettings.privacyEmail ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>

                  <!-- 公开手机号 -->
                  <div class="flex items-center justify-between p-4 rounded-xl" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                    <div class="flex items-center gap-3">
                      <Bell class="w-5 h-5" style="color: var(--theme-text-secondary);" />
                      <div>
                        <p class="text-sm font-medium" style="color: var(--theme-text);">公开手机号</p>
                        <p class="text-xs" style="color: var(--theme-text-secondary);">在个人主页显示你的手机号</p>
                      </div>
                    </div>
                    <label class="relative inline-flex items-center cursor-pointer">
                      <input type="checkbox" v-model="privacySettings.privacyPhone" class="sr-only peer" />
                      <div class="w-11 h-6 rounded-full transition-colors" :style="privacySettings.privacyPhone ? { backgroundColor: 'var(--theme-primary)' } : { backgroundColor: 'var(--theme-border)' }"></div>
                      <div class="absolute left-0.5 top-0.5 bg-white w-5 h-5 rounded-full transition-transform" :style="privacySettings.privacyPhone ? { transform: 'translateX(20px)' } : { transform: 'translateX(0)' }"></div>
                    </label>
                  </div>
                </div>

                <!-- 保存按钮 -->
                <div class="pt-6">
                  <button
                    @click="savePrivacySettings"
                    :disabled="isLoading"
                    class="flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-medium transition-colors"
                    style="background-color: var(--theme-primary); color: white;"
                  >
                    <Save class="w-4 h-4" />
                    {{ isLoading ? '保存中...' : '保存设置' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- 账号注销 -->
            <div v-if="activeSection === 'danger'" class="space-y-6">
              <div class="p-6 sm:p-8 rounded-2xl border-2" style="border-color: #dc2626; background-color: #fef2f2;">
                <div class="flex items-start gap-4">
                  <div class="w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0" style="background-color: #fee2e2;">
                    <AlertTriangle class="w-6 h-6 text-red-600" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <h3 class="text-lg font-semibold mb-2 text-red-700">注销账号</h3>
                    <p class="text-sm mb-4 text-red-600">注销账号是不可逆的操作，你的所有数据（包括文章、评论、收藏等）将被永久删除，请谨慎操作。</p>

                    <div class="space-y-3">
                      <div class="p-4 rounded-xl border" style="border-color: #fca5a5; background-color: white;">
                        <p class="text-xs text-red-600 mb-2">注销前请确认：</p>
                        <ul class="text-xs text-red-700 space-y-1 ml-4 list-disc">
                          <li>你已备份所有需要保留的文章和数据</li>
                          <li>你的账户余额已提现或使用完毕</li>
                          <li>你已解除与第三方账号的绑定</li>
                        </ul>
                      </div>

                      <div v-if="!showDeleteConfirm">
                        <button
                          @click="showDeleteConfirm = true"
                          class="px-6 py-3 rounded-xl text-sm font-medium transition-colors bg-red-600 text-white hover:bg-red-700"
                        >
                          申请注销账号
                        </button>
                      </div>

                      <!-- 确认对话框 -->
                      <div v-else class="p-4 rounded-xl border" style="border-color: #fca5a5; background-color: white;">
                        <p class="text-sm font-medium mb-3" style="color: var(--theme-text);">
                          为防止误操作，请在下方输入框中输入 "注销账号" 以确认：
                        </p>
                        <label for="settings-delete-confirm" class="sr-only">输入"注销账号"以确认</label>
                        <input
                          id="settings-delete-confirm"
                          v-model="deleteConfirmText"
                          type="text"
                          placeholder="注销账号"
                          class="w-full px-4 py-3 rounded-xl border mb-3 focus:outline-none focus:ring-2"
                          style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                        />
                        <div class="flex gap-3">
                          <button
                            @click="showDeleteConfirm = false; deleteConfirmText = '';"
                            class="flex-1 px-4 py-2 rounded-xl text-sm font-medium transition-colors"
                            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
                          >
                            取消
                          </button>
                          <button
                            @click="confirmDelete"
                            class="flex-1 px-4 py-2 rounded-xl text-sm font-medium transition-colors bg-red-600 text-white hover:bg-red-700"
                          >
                            确认注销
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
