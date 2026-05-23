<script setup lang="ts">
import { ref } from 'vue';
import { useHead } from '@vueuse/head';
import { AlertTriangle, MessageSquare, CheckCircle } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';

useHead(
  generateSeo({
    title: '举报与反馈',
    description: '提交举报或反馈，帮助我们改进平台。',
    keywords: ['举报', '反馈', '意见'],
    type: 'website'
  })
);

const activeTab = ref('report'); // 'report' 或 'feedback'
const showToast = ref(false);

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
const submitSuccess = ref(false);

const reportTypes = [
  { value: 'spam', label: '垃圾内容' },
  { value: 'inappropriate', label: '不当内容' },
  { value: 'infringement', label: '侵权内容' },
  { value: 'fraud', label: '欺诈行为' },
  { value: 'other', label: '其他问题' }
];

const feedbackTypes = [
  { value: 'suggestion', label: '功能建议' },
  { value: 'bug', label: 'Bug反馈' },
  { value: 'experience', label: '体验问题' },
  { value: 'other', label: '其他' }
];

const handleSubmitReport = () => {
  if (!reportForm.value.description.trim()) {
    alert('请描述问题详情');
    return;
  }

  isSubmitting.value = true;
  setTimeout(() => {
    isSubmitting.value = false;
    submitSuccess.value = true;
    showToast.value = true;
    setTimeout(() => {
      submitSuccess.value = false;
      showToast.value = false;
      reportForm.value = {
        reportType: 'spam',
        targetUrl: '',
        description: '',
        contact: '',
        images: []
      };
    }, 2000);
  }, 1000);
};

const handleSubmitFeedback = () => {
  if (!feedbackForm.value.description.trim()) {
    alert('请填写反馈内容');
    return;
  }

  isSubmitting.value = true;
  setTimeout(() => {
    isSubmitting.value = false;
    submitSuccess.value = true;
    showToast.value = true;
    setTimeout(() => {
      submitSuccess.value = false;
      showToast.value = false;
      feedbackForm.value = {
        feedbackType: 'suggestion',
        subject: '',
        description: '',
        contact: ''
      };
    }, 2000);
  }, 1000);
};
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '举报与反馈' }]" />
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
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">上传图片（可选）</label>
            <div 
              class="border-2 border-dashed rounded-xl p-6 text-center cursor-pointer hover:border-primary"
              style="border-color: var(--theme-border);"
            >
              <div class="text-sm" style="color: var(--theme-text-secondary);">
                点击或拖拽图片到此处上传<br />
                支持 JPG、PNG 格式，最多 3 张
              </div>
            </div>
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
        <ul class="text-xs sm:text-sm space-y-1" style="color: var(--theme-text-secondary);">
          <li>• 请如实举报或反馈，恶意举报将承担相应责任</li>
          <li>• 我们会在收到后的 3 个工作日内处理您的举报</li>
          <li>• 如情况紧急，可联系客服热线：400-888-8888</li>
        </ul>
      </div>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
