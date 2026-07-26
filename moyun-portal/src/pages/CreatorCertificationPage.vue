<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Save, Upload, CheckCircle2, Clock, XCircle, Loader2,
  ShieldCheck, IdCard, Sparkles, Award,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import {
  applyCertification, getMyCertification, CERT_TYPE_OPTIONS,
  type CreatorCertification,
} from '@/api/certification';
import { uploadPortalFile, deletePortalFile } from '@/api/file';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

// 加载 / 状态
const loading = ref(false);
const submitting = ref(false);
const uploading = ref(false);

// 当前认证记录（最近一条）
const current = ref<CreatorCertification | null>(null);

// 表单
const form = reactive<CreatorCertification>({
  realName: '',
  certType: 'identity',
  certNo: '',
  certImage: '',
  intro: '',
  works: '',
});

// 本地证件照预览
const certImagePreview = ref('');

const certTypeOptions = CERT_TYPE_OPTIONS;

const currentCertType = computed(() =>
  certTypeOptions.find(o => o.value === form.certType)
);

// 状态判断
const isApproved = computed(() => current.value?.status === 'approved');
const isPending = computed(() => current.value?.status === 'pending');
const isRejected = computed(() => current.value?.status === 'rejected');

// 是否允许再次提交：approved 禁止，pending 禁止，rejected 或无记录允许
const canApply = computed(() => {
  if (!current.value) return true;
  return current.value.status === 'rejected';
});

const breadcrumbs = computed(() => [{ label: '创作者认证' }]);

useHead(computed(() => generateSeo({
  title: '创作者认证',
  description: '提交创作者认证申请，认证后可发布专栏、连载等内容',
  keywords: ['创作者认证', '身份认证', '专家认证', '墨韵智库'],
  canonicalPath: '/creator/certification',
  robots: 'noindex,nofollow',
})));

onMounted(() => {
  loadMy();
});

async function loadMy() {
  loading.value = true;
  try {
    const res = await getMyCertification();
    if (res.code === 200) {
      current.value = res.data || null;
    }
  } catch (err) {
    const e = err as { message?: string };
    // 静默处理：未登录等场景由路由守卫负责
    console.warn('加载认证状态失败:', e?.message);
  } finally {
    loading.value = false;
  }
}

// 证件照上传
// 替换语义（与其他附件组件统一）：先上传新 → 成功后再删旧 → 失败恢复旧值，避免丢失原证件照。
// 额外触发动作：本场景证件照为「先上传到服务器保存 URL，后随表单提交」，
// 因此替换时仅清理「本次会话内已上传但被替换掉的旧 URL」，未提交表单前不涉及业务记录的级联。
async function handleCertImageChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  if (uploading.value) return;

  if (!file.type.startsWith('image/')) {
    toast.error('请选择图片文件');
    if (target) target.value = '';
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    toast.error('图片大小不能超过 5MB');
    if (target) target.value = '';
    return;
  }

  // 记录旧证件照（已上传 URL 才需后端清理；首次上传 form.certImage 为空则跳过）
  const oldCertImage = form.certImage || '';
  const previousPreview = certImagePreview.value;
  // 本地预览：使用同步 createObjectURL 避免 FileReader 异步回调时序竞争（大文件快速失败时 onload 可能晚于清空触发）
  const blobUrl = URL.createObjectURL(file);
  certImagePreview.value = blobUrl;

  uploading.value = true;
  try {
    const res = await uploadPortalFile(file, 'creator_certification');
    if (res.code === 200 && res.data) {
      const newUrl = res.data.fileUrl || '';
      form.certImage = newUrl;
      // 切换预览为正式 URL，并释放本地 blob URL（避免内存泄漏）
      certImagePreview.value = newUrl;
      URL.revokeObjectURL(blobUrl);
      // 新证件照上传成功后，清理旧证件照文件（DB+存储），失败仅警告不阻断主流程
      if (oldCertImage && /^https?:\/\//.test(oldCertImage)) {
        try {
          await deletePortalFile(oldCertImage);
        } catch (e) {
          console.warn('旧证件照清理失败：', e);
        }
      }
      toast.success('证件照上传成功');
    } else {
      // 上传失败：恢复旧值（替换语义——不丢失原证件照），释放本次失败的 blob URL
      form.certImage = oldCertImage;
      certImagePreview.value = previousPreview;
      URL.revokeObjectURL(blobUrl);
      toast.error(res.message || '上传失败');
    }
  } catch (err) {
    form.certImage = oldCertImage;
    certImagePreview.value = previousPreview;
    URL.revokeObjectURL(blobUrl);
    const e = err as { message?: string };
    toast.error(e?.message || '上传失败，请稍后重试');
  } finally {
    uploading.value = false;
    // 清空 input 以便重复选择同一文件
    if (target) target.value = '';
  }
}

// 提交申请
async function handleSubmit() {
  if (submitting.value) return;
  if (!form.realName.trim()) {
    toast.error('请输入真实姓名');
    return;
  }
  if (!form.certType) {
    toast.error('请选择证件类型');
    return;
  }
  if (!form.certImage) {
    toast.error('请上传证件照');
    return;
  }

  submitting.value = true;
  try {
    const res = await applyCertification({ ...form });
    if (res.code === 200 && res.data) {
      current.value = res.data;
      toast.success('申请提交成功，请等待审核');
      // 滚动到状态展示区
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      toast.error(res.message || '提交失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/user');
  }
}

function statusLabel(status?: string) {
  switch (status) {
    case 'pending': return '审核中';
    case 'approved': return '已通过';
    case 'rejected': return '已驳回';
    default: return '未申请';
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

    <!-- 加载状态 -->
    <div v-if="loading" class="flex flex-col items-center justify-center py-24">
      <Loader2 class="w-10 h-10 animate-spin" style="color: var(--theme-primary);" />
      <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
    </div>

    <div v-else class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 已通过认证徽章提示 -->
        <div
          v-if="isApproved"
          class="mb-6 p-5 rounded-xl flex items-center gap-3"
          style="background: linear-gradient(135deg, color-mix(in srgb, var(--theme-primary) 12%, var(--theme-surface)), var(--theme-surface)); border: 1px solid color-mix(in srgb, var(--theme-primary) 30%, var(--theme-border));"
        >
          <div
            class="w-12 h-12 rounded-full flex items-center justify-center flex-shrink-0"
            style="background-color: var(--theme-primary);"
          >
            <ShieldCheck class="w-6 h-6 text-white" />
          </div>
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <span class="text-base font-semibold" style="color: var(--theme-text);">已认证创作者</span>
              <span
                class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium text-white"
                style="background-color: var(--theme-primary);"
              >
                <Award class="w-3 h-3 mr-1" />已认证
              </span>
            </div>
            <p class="text-sm" style="color: var(--theme-text-secondary);">
              {{ currentCertType?.desc || '您已完成创作者认证' }}
            </p>
          </div>
        </div>

        <!-- 当前认证状态展示（pending / rejected 时） -->
        <div
          v-if="current && (isPending || isRejected)"
          class="mb-6 rounded-xl border p-5"
          :style="{
            backgroundColor: 'var(--theme-surface)',
            borderColor: isPending ? 'color-mix(in srgb, #f59e0b 40%, var(--theme-border))' : 'color-mix(in srgb, #ef4444 40%, var(--theme-border))',
          }"
        >
          <div class="flex items-start gap-3">
            <component
              :is="isPending ? Clock : XCircle"
              class="w-6 h-6 flex-shrink-0 mt-0.5"
              :style="{ color: isPending ? '#f59e0b' : '#ef4444' }"
            />
            <div class="flex-1">
              <div class="flex items-center gap-2 mb-1">
                <span class="text-base font-semibold" style="color: var(--theme-text);">
                  {{ statusLabel(current.status) }}
                </span>
                <span
                  class="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium"
                  :style="{
                    color: isPending ? '#b45309' : '#b91c1c',
                    backgroundColor: isPending ? 'rgba(245,158,11,0.12)' : 'rgba(239,68,68,0.12)',
                  }"
                >
                  {{ current.status }}
                </span>
              </div>
              <p v-if="isPending" class="text-sm" style="color: var(--theme-text-secondary);">
                您的认证申请已提交，平台将在 1-3 个工作日内完成审核，请耐心等待。
              </p>
              <template v-else>
                <p class="text-sm mb-2" style="color: var(--theme-text-secondary);">
                  您的认证申请未通过，可修改后重新提交。
                </p>
                <div v-if="current.auditRemark" class="text-sm p-3 rounded-lg" style="background-color: var(--theme-bg); color: var(--theme-text);">
                  <span style="color: var(--theme-text-secondary);">审核备注：</span>{{ current.auditRemark }}
                </div>
              </template>
              <div class="mt-3 text-xs" style="color: var(--theme-text-secondary);">
                <span>申请类型：{{ currentCertType?.label || current.certType }}</span>
                <span class="mx-2">·</span>
                <span v-if="current.createdTime">提交时间：{{ current.createdTime }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 申请表单（无认证记录 / 已驳回 时展示） -->
        <div
          v-if="canApply"
          class="rounded-xl border p-6 sm:p-8"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <h2 class="text-lg font-semibold mb-6 flex items-center" style="color: var(--theme-text);">
            <IdCard class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
            认证申请表
          </h2>

          <div class="space-y-6">
            <!-- 真实姓名 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                真实姓名 <span style="color: #ef4444;">*</span>
              </label>
              <input
                v-model="form.realName"
                type="text"
                maxlength="64"
                placeholder="请输入真实姓名"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 证件类型 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                证件类型 <span style="color: #ef4444;">*</span>
              </label>
              <select
                v-model="form.certType"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              >
                <option v-for="opt in certTypeOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }} - {{ opt.desc }}
                </option>
              </select>
              <p v-if="currentCertType" class="mt-2 text-xs" style="color: var(--theme-text-secondary);">
                <Sparkles class="w-3 h-3 inline mr-1" />{{ currentCertType.desc }}
              </p>
            </div>

            <!-- 证件号 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                证件号
              </label>
              <input
                v-model="form.certNo"
                type="text"
                maxlength="64"
                placeholder="请输入证件号码（身份证/护照等）"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
            </div>

            <!-- 证件照上传 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                证件照 <span style="color: #ef4444;">*</span>
              </label>
              <div class="flex items-start gap-4">
                <div class="relative group flex-shrink-0">
                  <img
                    v-if="certImagePreview"
                    :src="certImagePreview"
                    alt="证件照预览"
                    class="w-32 h-32 rounded-xl object-cover border"
                    style="border-color: var(--theme-border);"
                  />
                  <div
                    v-else
                    class="w-32 h-32 rounded-xl flex items-center justify-center border"
                    style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                  >
                    <IdCard class="w-12 h-12" style="color: var(--theme-text-secondary); opacity: 0.5;" />
                  </div>
                  <label
                    class="absolute bottom-0 right-0 w-9 h-9 rounded-lg flex items-center justify-center cursor-pointer transition-opacity shadow"
                    style="background-color: var(--theme-primary);"
                    :title="form.certImage ? '重新上传' : '上传证件照'"
                  >
                    <component :is="uploading ? Loader2 : Upload" class="w-4 h-4 text-white" :class="uploading ? 'animate-spin' : ''" />
                    <input
                      type="file"
                      accept="image/*"
                      @change="handleCertImageChange"
                      class="hidden"
                      :disabled="uploading"
                    />
                  </label>
                </div>
                <div class="flex-1 pt-1">
                  <p class="text-sm mb-1" style="color: var(--theme-text);">支持 JPG、PNG 格式</p>
                  <p class="text-xs" style="color: var(--theme-text-secondary);">图片大小不超过 5MB，请保证证件信息清晰可见</p>
                  <p v-if="uploading" class="text-xs mt-2" style="color: var(--theme-primary);">上传中...</p>
                </div>
              </div>
            </div>

            <!-- 自我介绍 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                自我介绍
              </label>
              <textarea
                v-model="form.intro"
                rows="4"
                maxlength="500"
                placeholder="简要介绍您的创作领域、专业背景等"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition resize-y"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              ></textarea>
            </div>

            <!-- 代表作链接 -->
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                代表作链接
              </label>
              <input
                v-model="form.works"
                type="text"
                maxlength="500"
                placeholder="https://your-works.example.com"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition"
                style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
              />
              <p class="mt-2 text-xs" style="color: var(--theme-text-secondary);">填写您最具代表性的作品链接（如专栏、文章等）</p>
            </div>

            <!-- 提交按钮 -->
            <div class="flex items-center justify-end gap-3 pt-2">
              <button
                @click="goBack"
                class="px-5 py-2.5 rounded-xl text-sm font-medium transition hover:opacity-80"
                style="background-color: var(--theme-bg); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
              >
                取消
              </button>
              <button
                @click="handleSubmit"
                :disabled="submitting"
                class="inline-flex items-center px-5 py-2.5 rounded-xl text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
                style="background-color: var(--theme-primary);"
              >
                <component :is="submitting ? Loader2 : Save" class="w-4 h-4 mr-1.5" :class="submitting ? 'animate-spin' : ''" />
                {{ submitting ? '提交中...' : '提交申请' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 已通过：仅展示认证信息（不可再次申请） -->
        <div
          v-else-if="isApproved"
          class="rounded-xl border p-6 sm:p-8"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <h2 class="text-lg font-semibold mb-4 flex items-center" style="color: var(--theme-text);">
            <CheckCircle2 class="w-5 h-5 mr-2" style="color: var(--theme-primary);" />
            认证信息
          </h2>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
            <div>
              <span style="color: var(--theme-text-secondary);">真实姓名：</span>
              <span style="color: var(--theme-text);">{{ current?.realName || '-' }}</span>
            </div>
            <div>
              <span style="color: var(--theme-text-secondary);">认证类型：</span>
              <span style="color: var(--theme-text);">{{ currentCertType?.label || current?.certType }}</span>
            </div>
            <div v-if="current?.certNo">
              <span style="color: var(--theme-text-secondary);">证件号：</span>
              <span style="color: var(--theme-text);">{{ current.certNo }}</span>
            </div>
            <div v-if="current?.createdTime">
              <span style="color: var(--theme-text-secondary);">申请时间：</span>
              <span style="color: var(--theme-text);">{{ current.createdTime }}</span>
            </div>
            <div v-if="current?.auditedTime">
              <span style="color: var(--theme-text-secondary);">通过时间：</span>
              <span style="color: var(--theme-text);">{{ current.auditedTime }}</span>
            </div>
            <div v-if="current?.works">
              <span style="color: var(--theme-text-secondary);">代表作：</span>
              <a
                :href="current.works"
                target="_blank"
                rel="noopener noreferrer"
                class="hover:underline"
                style="color: var(--theme-primary);"
              >{{ current.works }}</a>
            </div>
          </div>
          <div v-if="current?.intro" class="mt-4">
            <span class="block text-sm mb-1" style="color: var(--theme-text-secondary);">自我介绍</span>
            <p class="text-sm p-3 rounded-lg" style="background-color: var(--theme-bg); color: var(--theme-text);">{{ current.intro }}</p>
          </div>
          <div v-if="current?.certImage" class="mt-4">
            <span class="block text-sm mb-2" style="color: var(--theme-text-secondary);">证件照</span>
            <img
              :src="current.certImage"
              alt="证件照"
              class="w-32 h-32 rounded-xl object-cover border"
              style="border-color: var(--theme-border);"
            />
          </div>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
