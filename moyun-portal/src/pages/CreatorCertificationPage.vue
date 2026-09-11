<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Save, Upload, CheckCircle2, Clock, XCircle, Loader2,
  ShieldCheck, IdCard, Sparkles, Award, ScanLine, FileText,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import {
  applyCertification, getMyCertification, CERT_TYPE_OPTIONS,
  recognizeIdCard, type CreatorCertification,
} from '@/api/certification';
import { uploadPortalFile, deletePortalFile } from '@/api/file';
import { useToast } from '@/composables/useToast';
import { validateIdCard } from '@/utils/idCard';

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
  certImageFront: '',
  certImageBack: '',
  intro: '',
  works: '',
});

// 本地证件照预览
const certImagePreview = ref('');
// 双面预览（仅身份认证 identity 类型使用）
const certImageFrontPreview = ref('');
const certImageBackPreview = ref('');

// 身份证号实时校验提示（null=未校验/通过；非空=错误原因）
const idCardError = ref<string | null>(null);
// OCR 识别中标记（仅 front 面）
const ocrLoading = ref(false);

const certTypeOptions = CERT_TYPE_OPTIONS;

const currentCertType = computed(() =>
  certTypeOptions.find(o => o.value === form.certType)
);

// 是否为身份认证（identity）：双面身份证 + OCR
const isIdentityType = computed(() => form.certType === 'identity');

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
  keywords: ['创作者认证', '身份认证', '专家认证', '旭林知行'],
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

// 证件照上传（旧单图，certType != identity 时使用）
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

// 身份证双面上传（仅 certType=identity 使用）
// side='front' 上传成功后自动触发 OCR，识别 name/idNo 回填表单（用户仍可手动修改）
async function handleIdCardImageChange(event: Event, side: 'front' | 'back') {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  if (uploading.value || ocrLoading.value) return;

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

  // 当前 side 对应的 form 字段 / 预览 ref / 已上传 URL 快照
  const oldUrl = side === 'front' ? (form.certImageFront || '') : (form.certImageBack || '');
  const previewRef = side === 'front' ? certImageFrontPreview : certImageBackPreview;
  const previousPreview = previewRef.value;
  const blobUrl = URL.createObjectURL(file);
  previewRef.value = blobUrl;

  uploading.value = true;
  try {
    const res = await uploadPortalFile(file, 'creator_certification');
    if (res.code === 200 && res.data) {
      const newUrl = res.data.fileUrl || '';
      if (side === 'front') {
        form.certImageFront = newUrl;
      } else {
        form.certImageBack = newUrl;
      }
      previewRef.value = newUrl;
      URL.revokeObjectURL(blobUrl);
      if (oldUrl && /^https?:\/\//.test(oldUrl)) {
        try {
          await deletePortalFile(oldUrl);
        } catch (e) {
          console.warn(`旧身份证${side === 'front' ? '正面' : '背面'}清理失败：`, e);
        }
      }
      toast.success(`身份证${side === 'front' ? '正面' : '背面'}上传成功`);
      // 仅 front 面自动触发 OCR 识别回填
      if (side === 'front') {
        await runOcrRecognition(file);
      }
    } else {
      if (side === 'front') {
        form.certImageFront = oldUrl;
      } else {
        form.certImageBack = oldUrl;
      }
      previewRef.value = previousPreview;
      URL.revokeObjectURL(blobUrl);
      toast.error(res.message || '上传失败');
    }
  } catch (err) {
    if (side === 'front') {
      form.certImageFront = oldUrl;
    } else {
      form.certImageBack = oldUrl;
    }
    previewRef.value = previousPreview;
    URL.revokeObjectURL(blobUrl);
    const e = err as { message?: string };
    toast.error(e?.message || '上传失败，请稍后重试');
  } finally {
    uploading.value = false;
    if (target) target.value = '';
  }
}

// 调用 OCR 接口识别身份证正面信息（当前后端为 STUB，未返回真实数据）
// 接入真实 API 后自动回填 name + idNo，并触发 idCardError 实时校验
async function runOcrRecognition(file: File) {
  if (ocrLoading.value) return;
  ocrLoading.value = true;
  try {
    const res = await recognizeIdCard(file, 'front');
    if (res.code === 200 && res.data) {
      const result = res.data;
      if (result.success) {
        // 回填姓名 / 身份证号（仅当识别出对应字段时，避免覆盖用户已输入）
        if (result.name) form.realName = result.name;
        if (result.idNo) {
          form.certNo = result.idNo;
          // 触发实时校验
          idCardError.value = validateIdCard(form.certNo);
        }
        toast.success('OCR 识别成功，已自动回填');
      } else {
        // STUB 阶段会落入此处，提示用户当前 OCR 未启用，需手动填写
        toast.info(result.errorMessage || 'OCR 未识别出有效信息，请手动填写');
      }
    } else {
      toast.error(res.message || 'OCR 识别失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    console.warn('OCR 识别异常:', e?.message);
    toast.error('OCR 识别异常，请手动填写');
  } finally {
    ocrLoading.value = false;
  }
}

// 身份证号实时校验（blur 时触发）
function onIdCardBlur() {
  if (!isIdentityType.value) {
    idCardError.value = null;
    return;
  }
  // 留空时不立刻报错，由提交时统一拦截
  if (!form.certNo) {
    idCardError.value = null;
    return;
  }
  idCardError.value = validateIdCard(form.certNo);
}

// 证件类型切换时清空身份证号错误提示
function onCertTypeChange() {
  idCardError.value = null;
}

// OCR 重试按钮：因接口要求 multipart 上传，无法用已上传 URL 重新识别；
// 提示用户重新上传正面照片即可再次触发自动 OCR
function onOcrRetryClick() {
  if (ocrLoading.value || uploading.value) return;
  if (form.certImageFront) {
    toast.info('如需重新识别，请重新上传正面照片');
  } else {
    toast.info('请先上传身份证正面照片');
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
  // 身份认证：强制校验身份证号 + 双面照片
  if (isIdentityType.value) {
    const idCardErrorText = validateIdCard(form.certNo);
    if (idCardErrorText) {
      toast.error(idCardErrorText);
      idCardError.value = idCardErrorText;
      return;
    }
    if (!form.certImageFront) {
      toast.error('请上传身份证正面（人像面）照片');
      return;
    }
    if (!form.certImageBack) {
      toast.error('请上传身份证背面（国徽面）照片');
      return;
    }
    // 同步兼容字段 certImage = certImageFront，保证老记录读取一致
    form.certImage = form.certImageFront;
  } else {
    // 其他类型：保留原单图上传
    if (!form.certImage) {
      toast.error('请上传证件照');
      return;
    }
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
                @change="onCertTypeChange"
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
                证件号 <span v-if="isIdentityType" style="color: #ef4444;">*</span>
              </label>
              <input
                v-model="form.certNo"
                @blur="onIdCardBlur"
                type="text"
                :maxlength="isIdentityType ? 18 : 64"
                :placeholder="isIdentityType ? '请输入 18 位身份证号' : '请输入证件号码（身份证/护照等）'"
                class="w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 transition"
                :style="{
                  backgroundColor: 'var(--theme-bg)',
                  borderColor: idCardError ? '#ef4444' : 'var(--theme-border)',
                  color: 'var(--theme-text)',
                }"
              />
              <!-- 身份证号实时校验错误 -->
              <p v-if="idCardError" class="mt-2 text-xs" style="color: #ef4444;">
                {{ idCardError }}
              </p>
              <!-- 身份证号格式提示 -->
              <p v-else-if="isIdentityType" class="mt-2 text-xs" style="color: var(--theme-text-secondary);">
                18 位居民身份证号，支持末位 X；将通过校验位 MOD 11-2 算法验证
              </p>
              <!-- 隐私安全提示 -->
              <div
                class="mt-2 flex items-start gap-1.5 text-xs p-2 rounded-lg"
                style="background-color: color-mix(in srgb, var(--theme-primary) 6%, var(--theme-bg)); color: var(--theme-text-secondary);"
              >
                <ShieldCheck class="w-3.5 h-3.5 flex-shrink-0 mt-0.5" style="color: var(--theme-primary);" />
                <span>证件号将采用国密级 AES-GCM 加密存储，仅用于平台审核与风控，不会向任何第三方披露，页面展示时自动脱敏。</span>
              </div>
            </div>

            <!-- 身份证双面上传（仅 certType=identity 时显示，含示例图 + OCR） -->
            <div v-if="isIdentityType">
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                身份证双面照片 <span style="color: #ef4444;">*</span>
              </label>
              <p class="text-xs mb-3" style="color: var(--theme-text-secondary);">
                请上传身份证正反两面，确保信息完整、清晰可见、无遮挡。上传正面后可点击 OCR 自动识别回填姓名和号码。
              </p>

              <!-- 示例图（纯 CSS 示意，避免使用真实身份证图像，保护隐私） -->
              <div class="grid grid-cols-2 gap-3 mb-4 max-w-md">
                <div class="rounded-lg border border-dashed p-3 flex flex-col items-center"
                     style="background-color: var(--theme-bg); border-color: color-mix(in srgb, var(--theme-primary) 40%, var(--theme-border));">
                  <div class="w-full aspect-[1.585/1] rounded border flex items-center justify-center mb-2 relative overflow-hidden"
                       style="background: linear-gradient(135deg, color-mix(in srgb, var(--theme-primary) 8%, var(--theme-surface)), var(--theme-surface)); border-color: var(--theme-border);">
                    <div class="absolute inset-0 flex items-center">
                      <div class="w-10 h-12 rounded ml-3 flex-shrink-0" style="background-color: color-mix(in srgb, var(--theme-text-secondary) 30%, transparent);"></div>
                      <div class="ml-3 flex-1 space-y-1">
                        <div class="h-1.5 w-16 rounded" style="background-color: var(--theme-text-secondary); opacity: 0.4;"></div>
                        <div class="h-1.5 w-24 rounded" style="background-color: var(--theme-text-secondary); opacity: 0.4;"></div>
                        <div class="h-1.5 w-20 rounded" style="background-color: var(--theme-text-secondary); opacity: 0.4;"></div>
                      </div>
                    </div>
                    <span class="absolute top-1 right-2 text-[10px] font-medium px-1.5 py-0.5 rounded"
                          style="background-color: color-mix(in srgb, var(--theme-primary) 15%, var(--theme-surface)); color: var(--theme-primary);">
                      示例
                    </span>
                  </div>
                  <span class="text-xs font-medium" style="color: var(--theme-text);">人像面（正面）</span>
                  <span class="text-[10px]" style="color: var(--theme-text-secondary);">含照片、姓名、号码</span>
                </div>
                <div class="rounded-lg border border-dashed p-3 flex flex-col items-center"
                     style="background-color: var(--theme-bg); border-color: color-mix(in srgb, var(--theme-primary) 40%, var(--theme-border));">
                  <div class="w-full aspect-[1.585/1] rounded border flex items-center justify-center mb-2 relative overflow-hidden"
                       style="background: linear-gradient(135deg, color-mix(in srgb, var(--theme-primary) 8%, var(--theme-surface)), var(--theme-surface)); border-color: var(--theme-border);">
                    <FileText class="w-10 h-10" style="color: var(--theme-text-secondary); opacity: 0.4;" />
                    <span class="absolute top-1 right-2 text-[10px] font-medium px-1.5 py-0.5 rounded"
                          style="background-color: color-mix(in srgb, var(--theme-primary) 15%, var(--theme-surface)); color: var(--theme-primary);">
                      示例
                    </span>
                  </div>
                  <span class="text-xs font-medium" style="color: var(--theme-text);">国徽面（背面）</span>
                  <span class="text-[10px]" style="color: var(--theme-text-secondary);">含签发机关、有效期</span>
                </div>
              </div>

              <!-- 双面上传组件 -->
              <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <!-- 正面（人像面） -->
                <div>
                  <div class="relative group w-full" style="max-width: 240px;">
                    <img
                      v-if="certImageFrontPreview"
                      :src="certImageFrontPreview"
                      alt="身份证正面预览"
                      class="w-full aspect-[1.585/1] rounded-xl object-cover border"
                      style="border-color: var(--theme-border);"
                    />
                    <div
                      v-else
                      class="w-full aspect-[1.585/1] rounded-xl flex flex-col items-center justify-center border-2 border-dashed"
                      style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                    >
                      <IdCard class="w-10 h-10 mb-1" style="color: var(--theme-text-secondary); opacity: 0.5;" />
                      <span class="text-xs" style="color: var(--theme-text-secondary);">点击上传人像面</span>
                    </div>
                    <label
                      class="absolute bottom-2 right-2 w-9 h-9 rounded-lg flex items-center justify-center cursor-pointer shadow"
                      style="background-color: var(--theme-primary);"
                      :title="form.certImageFront ? '重新上传正面' : '上传正面'"
                    >
                      <component :is="uploading || ocrLoading ? Loader2 : Upload" class="w-4 h-4 text-white" :class="uploading || ocrLoading ? 'animate-spin' : ''" />
                      <input
                        type="file"
                        accept="image/*"
                        @change="(e) => handleIdCardImageChange(e, 'front')"
                        class="hidden"
                        :disabled="uploading || ocrLoading"
                      />
                    </label>
                    <!-- OCR 识别中遮罩 -->
                    <div v-if="ocrLoading" class="absolute inset-0 rounded-xl flex items-center justify-center"
                         style="background-color: rgba(0,0,0,0.4);">
                      <div class="flex items-center gap-2 px-3 py-1.5 rounded-lg" style="background-color: var(--theme-surface);">
                        <ScanLine class="w-4 h-4 animate-pulse" style="color: var(--theme-primary);" />
                        <span class="text-xs font-medium" style="color: var(--theme-text);">OCR 识别中</span>
                      </div>
                    </div>
                  </div>
                  <div class="mt-2">
                    <button
                      type="button"
                      @click="onOcrRetryClick"
                      :disabled="ocrLoading || uploading"
                      class="inline-flex items-center px-3 py-1.5 rounded-lg text-xs font-medium transition hover:opacity-90 disabled:opacity-50"
                      style="background-color: color-mix(in srgb, var(--theme-primary) 12%, var(--theme-surface)); color: var(--theme-primary); border: 1px solid color-mix(in srgb, var(--theme-primary) 30%, var(--theme-border));"
                    >
                      <ScanLine class="w-3 h-3 mr-1" />OCR 自动识别
                    </button>
                    <p class="text-[10px] mt-1" style="color: var(--theme-text-secondary);">
                      上传正面后自动触发，识别结果可手动修改
                    </p>
                  </div>
                </div>

                <!-- 背面（国徽面） -->
                <div>
                  <div class="relative group w-full" style="max-width: 240px;">
                    <img
                      v-if="certImageBackPreview"
                      :src="certImageBackPreview"
                      alt="身份证背面预览"
                      class="w-full aspect-[1.585/1] rounded-xl object-cover border"
                      style="border-color: var(--theme-border);"
                    />
                    <div
                      v-else
                      class="w-full aspect-[1.585/1] rounded-xl flex flex-col items-center justify-center border-2 border-dashed"
                      style="background-color: var(--theme-bg); border-color: var(--theme-border);"
                    >
                      <FileText class="w-10 h-10 mb-1" style="color: var(--theme-text-secondary); opacity: 0.5;" />
                      <span class="text-xs" style="color: var(--theme-text-secondary);">点击上传国徽面</span>
                    </div>
                    <label
                      class="absolute bottom-2 right-2 w-9 h-9 rounded-lg flex items-center justify-center cursor-pointer shadow"
                      style="background-color: var(--theme-primary);"
                      :title="form.certImageBack ? '重新上传背面' : '上传背面'"
                    >
                      <component :is="uploading ? Loader2 : Upload" class="w-4 h-4 text-white" :class="uploading ? 'animate-spin' : ''" />
                      <input
                        type="file"
                        accept="image/*"
                        @change="(e) => handleIdCardImageChange(e, 'back')"
                        class="hidden"
                        :disabled="uploading"
                      />
                    </label>
                  </div>
                </div>
              </div>
            </div>

            <!-- 证件照上传（非 identity 类型保留原单图上传） -->
            <div v-else>
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
              <span class="inline-flex items-center gap-1" style="color: var(--theme-text);">
                {{ current.certNo }}
                <ShieldCheck class="w-3.5 h-3.5" style="color: var(--theme-primary);" title="已加密存储，展示为脱敏值" />
              </span>
            </div>
            <div v-if="current?.derivedGender">
              <span style="color: var(--theme-text-secondary);">性别：</span>
              <span style="color: var(--theme-text);">{{ current.derivedGender }}</span>
            </div>
            <div v-if="current?.derivedBirth">
              <span style="color: var(--theme-text-secondary);">出生日期：</span>
              <span style="color: var(--theme-text);">{{ current.derivedBirth }}</span>
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
          <p
            class="mt-6 text-xs flex items-center gap-1.5 p-3 rounded-lg"
            style="background-color: color-mix(in srgb, var(--theme-primary) 6%, var(--theme-bg)); color: var(--theme-text-secondary);"
          >
            <ShieldCheck class="w-3.5 h-3.5 flex-shrink-0" style="color: var(--theme-primary);" />
            您的证件信息已加密存储（核验渠道：{{ current?.verifyChannel === 'manual' ? '人工审核' : current?.verifyChannel || '人工审核' }}），页面仅展示脱敏值。如需注销实名信息，请联系平台客服。
          </p>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
