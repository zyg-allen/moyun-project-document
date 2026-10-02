<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { AlertTriangle, MessageSquare, CheckCircle, Upload, X, History } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';
import { CONTACT_EMAIL } from '@/constants/site';
import { useToast } from '@/composables/useToast';
import { useAuth } from '@/composables/useAuth';
import { submitReport, submitFeedback, type ReportTargetType } from '@/api/report';
import { uploadImage } from '@/api/upload';
import { deletePortalFile } from '@/api/file';
import { useConfirmModal } from '@/composables/useConfirmModal';
import { useDictData } from '@/composables/useDictData';

const router = useRouter();
const route = useRoute();   // 清单 P2：读取 ?tab= 以支持从"我的反馈"直接进入反馈表单
const toast = useToast();
const { isAuthenticated, requireAuth } = useAuth();

useHead(
  generateSeo({
    title: '举报与反馈',
    description: '提交举报或反馈，帮助我们改进平台。',
    keywords: ['举报', '反馈', '意见'],
    type: 'website'
  })
);

// 'report' 或 'feedback'；query.tab=feedback 时直接进入反馈表单（清单 P2：原默认写死 'report'，不读参数）
const activeTab = ref(route.query.tab === 'feedback' ? 'feedback' : 'report');

/**
 * 举报目标上下文（清单 P2）。
 *
 * <p>后端与举报接口都支持 `targetType/targetId`（`buildReportExtra` 会写入审核详情），
 * 但页面原先只有自由文本 `targetUrl`，也不读 `route.query`，仓库内更没有页面带参跳转过来 ⇒
 * 从内容页发起举报时，"举报的是哪条内容"只能靠用户手抄 URL。这里从查询参数接收上下文。</p>
 */
const REPORT_TARGET_TYPES: ReportTargetType[] = ['comment', 'article', 'user'];

function normalizeTargetType(raw: unknown): ReportTargetType | '' {
  // 路由参数是任意字符串，只有落在后端支持的枚举内才采用（否则视为未指定）
  return typeof raw === 'string' && (REPORT_TARGET_TYPES as string[]).includes(raw)
    ? (raw as ReportTargetType)
    : '';
}

const targetContext = ref<{ targetType: ReportTargetType | ''; targetId: string } | null>(
  typeof route.query.targetId === 'string' && route.query.targetId
    ? { targetType: normalizeTargetType(route.query.targetType), targetId: route.query.targetId }
    : null,
);

const reportForm = ref({
  reportType: 'spam',
  targetUrl: '',
  description: '',
  contact: '',
  images: [] as string[]
});

const feedbackForm = ref({
  feedbackType: 'suggestion',
  subject: '',
  description: '',
  contact: ''
});

const isSubmitting = ref(false);
const isUploading = ref(false);
const submitSuccess = ref(false);

// 举报/反馈类型（字典 cms_report_type / cms_feedback_type 驱动，本地默认兜底）
const dictMap = useDictData(['cms_report_type', 'cms_feedback_type']);

const DEFAULT_REPORT_TYPES = [
  { value: 'spam', label: '垃圾内容' },
  { value: 'inappropriate', label: '不当内容' },
  { value: 'infringement', label: '侵权内容' },
  { value: 'fraud', label: '欺诈行为' },
  { value: 'other', label: '其他问题' }
];

const DEFAULT_FEEDBACK_TYPES = [
  { value: 'suggestion', label: '功能建议' },
  { value: 'bug', label: 'Bug反馈' },
  { value: 'experience', label: '体验问题' },
  { value: 'other', label: '其他' }
];

const reportTypes = computed(() => {
  const items = dictMap['cms_report_type'];
  if (items && items.length > 0) {
    return items.map(i => ({ value: i.dictValue, label: i.dictLabel }));
  }
  return DEFAULT_REPORT_TYPES;
});

const feedbackTypes = computed(() => {
  const items = dictMap['cms_feedback_type'];
  if (items && items.length > 0) {
    return items.map(i => ({ value: i.dictValue, label: i.dictLabel }));
  }
  return DEFAULT_FEEDBACK_TYPES;
});

const confirmModal = useConfirmModal();

const MAX_IMAGES = 3;

/** 选择/上传图片 */
async function handleImageSelect(event: Event) {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files || files.length === 0) return;

  const remain = MAX_IMAGES - reportForm.value.images.length;
  if (remain <= 0) {
    toast.warning(`最多上传 ${MAX_IMAGES} 张图片`);
    input.value = '';
    return;
  }

  const toUpload = Array.from(files).slice(0, remain);
  isUploading.value = true;
  // 上传成功/失败计数（清单 P2）：原实现无论成败都提示"图片上传成功"
  let uploadedCount = 0;
  let failedCount = 0;
  try {
    for (const file of toUpload) {
      // 类型与大小校验
      if (!file.type.startsWith('image/')) {
        toast.error(`${file.name} 不是图片文件`);
        continue;
      }
      if (file.size > 5 * 1024 * 1024) {
        toast.error(`${file.name} 超过 5MB`);
        continue;
      }
      const res = await uploadImage(file, { businessType: 'report' });
      if (res.data?.fileUrl) {
        reportForm.value.images.push(res.data.fileUrl);
        uploadedCount += 1;
      } else {
        // 业务失败（类型/大小/存储被后端拒绝）时 httpUpload 仍会 resolve 返回信封，
        // 原实现只 console.warn 跳过，循环结束后却**无条件** toast.success ⇒ 用户以为传上去了（清单 P2）
        failedCount += 1;
        console.warn('图片上传失败：', res.message || '未返回 fileUrl');
      }
    }
    // 按真实结果反馈，不再把"部分/全部失败"说成成功
    if (failedCount === 0) {
      toast.success(`图片上传成功（${uploadedCount} 张）`);
    } else if (uploadedCount === 0) {
      toast.error(`图片上传失败（${failedCount} 张）`);
    } else {
      toast.warning(`部分图片上传失败：成功 ${uploadedCount} 张 / 失败 ${failedCount} 张`);
    }
  } catch (e: any) {
    toast.error(e?.message || '图片上传失败');
  } finally {
    isUploading.value = false;
    input.value = '';
  }
}

/** 移除已上传图片 */
/**
 * 移除单张图片（清单 P2）。
 *
 * <p>原先只从本地数组 `splice`，**不调用后端删除** ⇒ 已上传文件成为孤儿文件（占用存储且后台文件管理里堆积）。</p>
 */
async function removeImageAndClean(idx: number, url: string) {
  const ok = await confirmModal.confirm('确认移除这张图片？已上传的文件将一并清理。', {
    title: '移除图片',
    confirmText: '确认移除',
    danger: true,
  });
  if (!ok) return;
  removeImage(idx);
  if (url) {
    try {
      await deletePortalFile(url);
    } catch (e) {
      console.warn('图片文件清理失败（仅本地移除）：', e);
    }
  }
}

function removeImage(idx: number) {
  reportForm.value.images.splice(idx, 1);
}

const handleSubmitReport = async () => {
  if (!reportForm.value.description.trim()) {
    toast.warning('请描述问题详情');
    return;
  }
  if (!isAuthenticated()) {
    toast.warning('请先登录后再提交举报');
    requireAuth();
    return;
  }

  isSubmitting.value = true;
  try {
    await submitReport({
      reportType: reportForm.value.reportType as any,
      targetUrl: reportForm.value.targetUrl || undefined,
      description: reportForm.value.description.trim(),
      contact: reportForm.value.contact || undefined,
      images: reportForm.value.images,
      // 清单 P2：把从 route.query 接收到的举报目标上下文一并提交
      //（后端 buildReportExtra 会写入审核详情；原先页面完全不带该上下文）
      ...(targetContext.value && targetContext.value.targetType
        ? { targetType: targetContext.value.targetType, targetId: targetContext.value.targetId }
        : {}),
    });
    submitSuccess.value = true;
    toast.success('举报提交成功，我们会尽快处理');
    setTimeout(() => {
      submitSuccess.value = false;
      reportForm.value = {
        reportType: 'spam',
        targetUrl: '',
        description: '',
        contact: '',
        images: []
      };
    }, 2000);
  } catch (e: any) {
    toast.error(e?.message || '举报提交失败，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
};

const handleSubmitFeedback = async () => {
  if (!feedbackForm.value.description.trim()) {
    toast.warning('请填写反馈内容');
    return;
  }
  if (!isAuthenticated()) {
    toast.warning('请先登录后再提交反馈');
    requireAuth();
    return;
  }

  isSubmitting.value = true;
  try {
    await submitFeedback({
      feedbackType: feedbackForm.value.feedbackType as any,
      subject: feedbackForm.value.subject || undefined,
      description: feedbackForm.value.description.trim(),
      contact: feedbackForm.value.contact || undefined
    });
    submitSuccess.value = true;
    toast.success('反馈提交成功，感谢您的支持');
    setTimeout(() => {
      submitSuccess.value = false;
      feedbackForm.value = {
        feedbackType: 'suggestion',
        subject: '',
        description: '',
        contact: ''
      };
    }, 2000);
  } catch (e: any) {
    toast.error(e?.message || '反馈提交失败，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 面包屑 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="[{ label: '举报与反馈' }]" />
        <button
          @click="router.push(activeTab === 'report' ? '/my/reports' : '/my/feedback')"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm transition-colors hover:opacity-80"
          style="color: var(--theme-primary); background-color: var(--theme-accent);"
        >
          <History class="w-4 h-4" />
          <span class="hidden sm:inline">{{ activeTab === 'report' ? '我的举报' : '我的反馈' }}</span>
        </button>
      </div>
    </div>

    <!-- 主内容 - 与列表页和详情页保持一致的宽度 -->
    <div class="flex-1 py-8 sm:py-12">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="text-center mb-8">
        <AlertTriangle class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-3" style="color: var(--theme-primary);" />
        <h1 class="text-2xl sm:text-3xl font-bold" style="color: var(--theme-text);">举报与反馈</h1>
        <p class="text-xs sm:text-sm mt-2" style="color: var(--theme-text-secondary);">
          感谢您帮助我们改进平台，您的意见对我们很重要
        </p>
      </div>

      <!-- Tab 切换 -->
      <div class="flex mb-6 rounded-xl p-1" style="background-color: var(--theme-surface);">
        <button
          @click="activeTab = 'report'"
          class="flex-1 py-2.5 sm:py-3 px-4 rounded-lg font-medium text-sm sm:text-base transition-all"
          :style="
            activeTab === 'report'
              ? 'background-color: var(--theme-primary); color: white;'
              : 'color: var(--theme-text-secondary);'
          "
        >
          <AlertTriangle class="w-4 h-4 inline mr-1.5" />
          内容举报
        </button>
        <button
          @click="activeTab = 'feedback'"
          class="flex-1 py-2.5 sm:py-3 px-4 rounded-lg font-medium text-sm sm:text-base transition-all"
          :style="
            activeTab === 'feedback'
              ? 'background-color: var(--theme-primary); color: white;'
              : 'color: var(--theme-text-secondary);'
          "
        >
          <MessageSquare class="w-4 h-4 inline mr-1.5" />
          意见反馈
        </button>
      </div>

      <!-- 举报表单 -->
      <div v-if="activeTab === 'report'" class="p-4 sm:p-6 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <h2 class="text-lg sm:text-xl font-semibold mb-5" style="color: var(--theme-text);">提交举报</h2>
        
        <div class="space-y-4 sm:space-y-5">
          <!-- 举报类型 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">举报类型</label>
            <select 
              v-model="reportForm.reportType"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            >
              <option v-for="type in reportTypes" :key="type.value" :value="type.value">
                {{ type.label }}
              </option>
            </select>
          </div>

          <!-- 目标链接 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">目标链接（可选）</label>
            <input 
              v-model="reportForm.targetUrl"
              type="text"
              placeholder="请输入相关链接"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            />
          </div>

          <!-- 详细描述 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">详细描述</label>
            <textarea 
              v-model="reportForm.description"
              rows="5"
              placeholder="请详细描述您发现的问题，帮助我们更好地处理"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            ></textarea>
          </div>

          <!-- 上传图片 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">上传图片（可选，最多 {{ MAX_IMAGES }} 张）</label>
            <div class="flex flex-wrap gap-3">
              <!-- 已上传图片预览 -->
              <div
                v-for="(img, idx) in reportForm.images"
                :key="idx"
                class="relative w-24 h-24 rounded-xl overflow-hidden group"
                style="border: 1px solid var(--theme-border);"
              >
                <img :src="img" :alt="`证据图${idx + 1}`" class="w-full h-full object-cover" />
                <button
                  type="button"
                  @click="removeImageAndClean(idx, img)"
                  class="absolute top-1 right-1 w-5 h-5 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                  style="background-color: rgba(0,0,0,0.6); color: white;"
                >
                  <X class="w-3 h-3" />
                </button>
              </div>
              <!-- 上传按钮 -->
              <label
                v-if="reportForm.images.length < MAX_IMAGES"
                class="w-24 h-24 rounded-xl border-2 border-dashed flex flex-col items-center justify-center cursor-pointer hover:border-primary transition-colors"
                style="border-color: var(--theme-border);"
                :class="{ 'opacity-60 pointer-events-none': isUploading }"
              >
                <Upload class="w-5 h-5 mb-1" style="color: var(--theme-text-secondary);" />
                <span class="text-xs" style="color: var(--theme-text-secondary);">{{ isUploading ? '上传中' : '添加图片' }}</span>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/jpg,image/gif,image/webp"
                  multiple
                  class="hidden"
                  @change="handleImageSelect"
                />
              </label>
            </div>
            <p class="text-xs mt-2" style="color: var(--theme-text-secondary);">支持 JPG/PNG/GIF/WebP，单张最大 5MB</p>
          </div>

          <!-- 联系方式 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">联系方式（可选）</label>
            <input 
              v-model="reportForm.contact"
              type="text"
              placeholder="请留下您的邮箱或手机号，便于我们后续沟通"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            />
          </div>

          <!-- 提交按钮 -->
          <button
            @click="handleSubmitReport"
            :disabled="isSubmitting"
            class="w-full py-3 rounded-xl font-medium transition-all hover:opacity-90 flex items-center justify-center gap-2"
            style="background-color: var(--theme-primary); color: white;"
          >
            {{ isSubmitting ? '提交中...' : '提交举报' }}
            <CheckCircle v-if="submitSuccess && activeTab === 'report'" class="w-4 h-4" />
          </button>
        </div>
      </div>

      <!-- 反馈表单 -->
      <div v-else class="p-4 sm:p-6 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
        <h2 class="text-lg sm:text-xl font-semibold mb-5" style="color: var(--theme-text);">提交反馈</h2>
        
        <div class="space-y-4 sm:space-y-5">
          <!-- 反馈类型 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">反馈类型</label>
            <select 
              v-model="feedbackForm.feedbackType"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            >
              <option v-for="type in feedbackTypes" :key="type.value" :value="type.value">
                {{ type.label }}
              </option>
            </select>
          </div>

          <!-- 主题 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">主题</label>
            <input 
              v-model="feedbackForm.subject"
              type="text"
              placeholder="请简要概括您的反馈"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            />
          </div>

          <!-- 详细描述 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">详细描述</label>
            <textarea 
              v-model="feedbackForm.description"
              rows="5"
              placeholder="请详细描述您的意见或建议"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            ></textarea>
          </div>

          <!-- 联系方式 -->
          <div>
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">联系方式（可选）</label>
            <input 
              v-model="feedbackForm.contact"
              type="text"
              placeholder="请留下您的邮箱或手机号，便于我们后续沟通"
              class="w-full px-4 py-2.5 rounded-xl border focus:outline-none focus:ring-2 focus:ring-primary"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            />
          </div>

          <!-- 提交按钮 -->
          <button
            @click="handleSubmitFeedback"
            :disabled="isSubmitting"
            class="w-full py-3 rounded-xl font-medium transition-all hover:opacity-90 flex items-center justify-center gap-2"
            style="background-color: var(--theme-primary); color: white;"
          >
            {{ isSubmitting ? '提交中...' : '提交反馈' }}
            <CheckCircle v-if="submitSuccess && activeTab === 'feedback'" class="w-4 h-4" />
          </button>
        </div>
      </div>

      <!-- 温馨提示 -->
      <div class="mt-6 p-4 sm:p-5 rounded-xl" style="background-color: var(--theme-accent);">
        <h3 class="font-medium text-sm sm:text-base mb-2" style="color: var(--theme-text);">温馨提示</h3>
        <!--
          清单 P2：原先写死「3 个工作日内处理」这一**服务承诺时限**，以及占位客服热线
          400-888-8888（无主号码，且用户协议页也出现过同一假号）。
          处理时限属对外承诺，应由运营确定口径后再写入；这里改为不计期限的表述，
          联系方式改用与页脚同源的邮箱常量，避免各页口径不一致。
        -->
        <ul class="text-xs sm:text-sm space-y-1" style="color: var(--theme-text-secondary);">
          <li>• 请如实举报或反馈，恶意举报将承担相应责任</li>
          <li>• 我们会在收到后尽快核实处理，处理结果可在「我的举报」中查看</li>
          <li>• 如需进一步沟通，可邮件联系：{{ CONTACT_EMAIL }}</li>
        </ul>
      </div>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
