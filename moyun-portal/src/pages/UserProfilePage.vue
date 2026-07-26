<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Camera,
  Save,
  User as UserIcon,
  Mail,
  MapPin,
  Briefcase,
  Globe,
  Github,
  Building2,
  GraduationCap,
  Calendar,
  MessageCircle
} from 'lucide-vue-next';
import { useUserStore } from '@/stores/user';
import { generateSeo } from '@/utils/seo';
import Breadcrumb from '@/components/Breadcrumb.vue';
import * as userApi from '@/api/user';
import { deletePortalFile } from '@/api/file';
import type { User, UpdateUserProfileParams } from '@/types/api';

const router = useRouter();
const userStore = useUserStore();

// 表单数据
const profileForm = ref<UpdateUserProfileParams>({
  nickname: '',
  username: '',
  email: '',
  phone: '',
  wechat: '',
  bio: '',
  position: '',
  avatar: '',
  gender: '',
  birthday: '',
  location: '',
  website: '',
  github: '',
  company: '',
  school: ''
});

const isLoading = ref(false);
const isAvatarUploading = ref(false);
const avatarPreview = ref('');
const successMessage = ref('');
const errorMessage = ref('');

const breadcrumbs = computed(() => [
  { label: '个人中心', path: '/user' },
  { label: '资料编辑' },
]);

// SEO
useHead(
  generateSeo({
    title: '编辑个人资料',
    description: '更新个人资料信息',
    keywords: ['编辑资料', '个人资料', '用户信息'],
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

  // 加载用户信息
  const user = userStore.user;
  if (user) {
    profileForm.value = {
      nickname: user.nickname || '',
      username: user.username || '',
      email: user.email || '',
      phone: user.phone || '',
      wechat: user.wechat || '',
      bio: user.bio || '',
      position: user.position || '',
      avatar: user.avatar || '',
      gender: user.gender || '',
      birthday: user.birthday || '',
      location: user.location || '',
      website: user.website || '',
      github: user.github || '',
      company: user.company || '',
      school: user.school || ''
    };
    avatarPreview.value = user.avatar || '';
  }
});

// 处理头像选择
// 替换语义（与其他附件组件统一）：先上传新 → 成功后再删旧 → 失败恢复旧值，避免丢失原头像。
// 额外触发动作：头像上传走专用 /portal/user/avatar 接口会同步更新 user.avatar 字段，
// 因此替换时需清理「被覆盖的旧头像文件」（DB+存储），未上传前不涉及。
async function handleAvatarChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  if (isAvatarUploading.value) return;

  // 校验文件类型
  if (!file.type.startsWith('image/')) {
    errorMessage.value = '请选择图片文件';
    if (target) target.value = '';
    return;
  }

  // 校验文件大小 (5MB)
  if (file.size > 5 * 1024 * 1024) {
    errorMessage.value = '图片大小不能超过5MB';
    if (target) target.value = '';
    return;
  }

  // 记录旧头像（已上传 URL 才需后端清理；首次设置无旧值则跳过）
  const oldAvatar = profileForm.value.avatar || '';
  const previousPreview = avatarPreview.value;
  // 本地预览：使用同步 createObjectURL 避免 FileReader 异步回调时序竞争
  const blobUrl = URL.createObjectURL(file);
  avatarPreview.value = blobUrl;

  isAvatarUploading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const response = await userApi.uploadAvatar(file);
    if (response && response.code === 200 && response.data) {
      const newAvatar = (response.data as any).avatar || (response.data as any).url || (response.data as User).avatar || '';
      profileForm.value.avatar = newAvatar;
      if (newAvatar) {
        // 切换预览为正式 URL，并释放本地 blob URL（避免内存泄漏）
        avatarPreview.value = newAvatar;
        URL.revokeObjectURL(blobUrl);
      } else {
        // 后端未返回 URL 时保留 blob 预览，待后续保存资料时再处理
      }
      successMessage.value = '头像上传成功';
      // 更新本地 store 中的用户信息
      if (userStore.user && newAvatar) {
        userStore.updateUserWithApi({ avatar: newAvatar });
      }
      // 新头像上传成功后，清理旧头像文件（DB+存储），失败仅警告不阻断主流程
      // 注意：旧头像可能由专用 avatar 接口写入 sys_file 表，也可能为 profile 目录相对路径，后者 deletePortalFile 查不到记录幂等返回
      if (oldAvatar && /^https?:\/\//.test(oldAvatar)) {
        try {
          await deletePortalFile(oldAvatar);
        } catch (e) {
          console.warn('旧头像清理失败：', e);
        }
      }
    } else {
      // 上传失败：恢复旧值（替换语义——不丢失原头像），释放本次失败的 blob URL
      profileForm.value.avatar = oldAvatar;
      avatarPreview.value = previousPreview;
      URL.revokeObjectURL(blobUrl);
      errorMessage.value = response?.message || '头像上传失败';
    }
  } catch (error) {
    profileForm.value.avatar = oldAvatar;
    avatarPreview.value = previousPreview;
    URL.revokeObjectURL(blobUrl);
    console.error('头像上传失败:', error);
    errorMessage.value = '头像上传失败，请稍后重试';
  } finally {
    isAvatarUploading.value = false;
    // 清空 input 以便重复选择同一文件
    if (target) target.value = '';
  }
}

// 保存个人资料
async function saveProfile() {
  isLoading.value = true;
  errorMessage.value = '';
  successMessage.value = '';

  try {
    const result = await userStore.updateUserWithApi(profileForm.value);
    if (result.success) {
      successMessage.value = '资料更新成功！';
      setTimeout(() => {
        router.push('/user');
      }, 1000);
    } else {
      errorMessage.value = result.message || '更新失败';
    }
  } catch (error) {
    console.error('更新失败:', error);
    errorMessage.value = '更新失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
}

// 返回上一页
function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/user');
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 提示信息 -->
        <div v-if="successMessage" class="mb-6 p-4 rounded-xl text-sm" style="background-color: #dcfce7; color: #166534; border: 1px solid #86efac;">
          {{ successMessage }}
        </div>
        <div v-if="errorMessage" class="mb-6 p-4 rounded-xl text-sm" style="background-color: #fef2f2; color: #991b1b; border: 1px solid #fecaca;">
          {{ errorMessage }}
        </div>

        <!-- 头像区域 -->
        <div class="mb-8 p-6 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <h2 class="text-lg font-semibold mb-6" style="color: var(--theme-text);">头像</h2>
          <div class="flex flex-col sm:flex-row items-start sm:items-center gap-6">
            <div class="relative group">
              <img
                v-if="avatarPreview"
                :src="avatarPreview"
                alt="头像"
                class="w-32 h-32 rounded-2xl object-cover"
              />
              <div
                v-else
                class="w-32 h-32 rounded-2xl flex items-center justify-center"
                style="background-color: var(--theme-accent);"
              >
                <UserIcon class="w-16 h-16" style="color: var(--theme-text-secondary);" />
              </div>
              <label
                class="absolute bottom-0 right-0 w-10 h-10 rounded-xl flex items-center justify-center cursor-pointer transition-opacity"
                style="background-color: var(--theme-primary);"
              >
                <Camera class="w-5 h-5 text-white" />
                <input
                  type="file"
                  accept="image/*"
                  @change="handleAvatarChange"
                  class="hidden"
                />
              </label>
            </div>
            <div class="flex-1">
              <p class="text-sm mb-2" style="color: var(--theme-text);">支持 JPG、PNG、GIF 格式</p>
              <p class="text-sm" style="color: var(--theme-text-secondary);">图片大小不超过 5MB</p>
              <p v-if="isAvatarUploading" class="text-sm mt-2" style="color: var(--theme-primary);">上传中...</p>
            </div>
          </div>
        </div>

        <!-- 基本信息 -->
        <div class="mb-8 p-6 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <h2 class="text-lg font-semibold mb-6" style="color: var(--theme-text);">基本信息</h2>

          <div class="space-y-6">
            <!-- 昵称 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <UserIcon class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  昵称
                </span>
              </label>
              <input
                v-model="profileForm.nickname"
                type="text"
                placeholder="请输入昵称"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 用户名 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <UserIcon class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  用户名
                </span>
              </label>
              <input
                v-model="profileForm.username"
                type="text"
                placeholder="请输入用户名"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 个人简介 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <MessageCircle class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  个人简介
                </span>
              </label>
              <textarea
                v-model="profileForm.bio"
                rows="3"
                placeholder="介绍一下你自己..."
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all resize-none"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              ></textarea>
              <p class="text-xs mt-1" style="color: var(--theme-text-secondary);">最多 500 个字符</p>
            </div>

            <!-- 职位 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <Briefcase class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  职位
                </span>
              </label>
              <input
                v-model="profileForm.position"
                type="text"
                placeholder="例如：前端开发工程师"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 所在城市 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <MapPin class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  所在城市
                </span>
              </label>
              <input
                v-model="profileForm.location"
                type="text"
                placeholder="例如：北京市"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 性别 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">性别</label>
              <select
                v-model="profileForm.gender"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              >
                <option value="">未设置</option>
                <option value="male">男</option>
                <option value="female">女</option>
                <option value="other">其他</option>
              </select>
            </div>

            <!-- 生日 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <Calendar class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  生日
                </span>
              </label>
              <input
                v-model="profileForm.birthday"
                type="date"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>
          </div>
        </div>

        <!-- 联系方式 -->
        <div class="mb-8 p-6 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <h2 class="text-lg font-semibold mb-6" style="color: var(--theme-text);">联系方式</h2>

          <div class="space-y-6">
            <!-- 邮箱 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <Mail class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  邮箱
                </span>
              </label>
              <input
                v-model="profileForm.email"
                type="email"
                placeholder="请输入邮箱地址"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
              <p v-if="userStore.user && userStore.user.isPhoneVerified === true" class="text-xs mt-1" style="color: #16a34a;">已验证</p>
            </div>

            <!-- 手机号 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <MessageCircle class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  手机号
                </span>
              </label>
              <input
                v-model="profileForm.phone"
                type="tel"
                placeholder="请输入手机号"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 微信号 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <MessageCircle class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  微信号
                </span>
              </label>
              <input
                v-model="profileForm.wechat"
                type="text"
                placeholder="请输入微信号"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>
          </div>
        </div>

        <!-- 教育工作信息 -->
        <div class="mb-8 p-6 sm:p-8 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <h2 class="text-lg font-semibold mb-6" style="color: var(--theme-text);">教育与工作</h2>

          <div class="space-y-6">
            <!-- 公司 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <Building2 class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  公司
                </span>
              </label>
              <input
                v-model="profileForm.company"
                type="text"
                placeholder="例如：某某科技有限公司"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 学校 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <GraduationCap class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  学校
                </span>
              </label>
              <input
                v-model="profileForm.school"
                type="text"
                placeholder="例如：清华大学"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 个人网站 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <Globe class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  个人网站
                </span>
              </label>
              <input
                v-model="profileForm.website"
                type="url"
                placeholder="https://your-website.com"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- GitHub -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                <span class="flex items-center gap-2">
                  <Github class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                  GitHub
                </span>
              </label>
              <input
                v-model="profileForm.github"
                type="url"
                placeholder="https://github.com/username"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition-all"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>
          </div>
        </div>

        <!-- 保存按钮 -->
        <div class="flex flex-col sm:flex-row gap-4 sm:justify-end">
          <button
            @click="goBack"
            class="px-6 py-3 rounded-xl text-sm font-medium transition-colors"
            style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
          >
            取消
          </button>
          <button
            @click="saveProfile"
            :disabled="isLoading"
            class="flex items-center justify-center gap-2 px-6 py-3 rounded-xl text-sm font-medium transition-colors"
            style="background-color: var(--theme-primary); color: white;"
          >
            <Save class="w-4 h-4" />
            {{ isLoading ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
