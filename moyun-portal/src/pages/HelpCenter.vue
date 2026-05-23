<script setup lang="ts">
import { ref, computed } from 'vue';
import { useHead } from '@vueuse/head';
import { Search, ChevronDown, ChevronRight, BookOpen, HelpCircle, MessageSquare, Shield } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';

const searchQuery = ref('');
const expandedFaq = ref<string | null>(null);

const faqs = [
  {
    id: 'faq-1',
    question: '如何发布文章？',
    answer: '点击页面右上角的"创作"按钮，进入文章编辑页面，填写标题、内容等信息后，点击"发布文章"按钮即可发布。'
  },
  {
    id: 'faq-2',
    question: '如何修改个人资料？',
    answer: '进入个人中心，点击"编辑个人资料"，在弹出的窗口中修改您的信息后保存即可。'
  },
  {
    id: 'faq-3',
    question: '如何关注其他用户？',
    answer: '在作者主页或文章详情页，点击作者头像或用户名进入作者主页，然后点击"关注"按钮即可。'
  },
  {
    id: 'faq-4',
    question: '文章审核需要多长时间？',
    answer: '文章提交后，通常会在1-3个工作日内完成审核。审核通过后文章即可公开发布。'
  },
  {
    id: 'faq-5',
    question: '如何举报违规内容？',
    answer: '在文章详情页或用户主页，点击"举报"按钮，填写举报原因后提交即可。'
  },
  {
    id: 'faq-6',
    question: '如何修改密码？',
    answer: '进入个人中心，点击"安全设置"，然后点击"修改密码"，按提示操作即可。'
  }
];

const categories = [
  { icon: BookOpen, title: '发布与编辑', description: '文章发布、编辑、删除等操作' },
  { icon: HelpCircle, title: '账号与安全', description: '登录、注册、密码、安全设置' },
  { icon: MessageSquare, title: '互动功能', description: '评论、点赞、关注等' },
  { icon: Shield, title: '社区规则', description: '使用规范、违规处理等' }
];

useHead(
  generateSeo({
    title: '帮助中心',
    description: '墨韵帮助中心，解答您在使用过程中遇到的各种问题。',
    keywords: ['帮助中心', 'FAQ', '常见问题'],
    type: 'website'
  })
);

function toggleFaq(id: string) {
  if (expandedFaq.value === id) {
    expandedFaq.value = null;
  } else {
    expandedFaq.value = id;
  }
}

const filteredFaqs = computed(() => {
  if (!searchQuery.value) return faqs;
  return faqs.filter(faq => 
    faq.question.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    faq.answer.toLowerCase().includes(searchQuery.value.toLowerCase())
  );
});
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="[{ label: '首页', path: '/' }, { label: '帮助中心' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1 max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-12">
      <!-- 页面标题 -->
      <div class="text-center mb-8 sm:mb-12">
        <h1 class="text-2xl sm:text-3xl lg:text-4xl font-bold mb-3 sm:mb-4" style="color: var(--theme-text);">帮助中心</h1>
        <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">找到您需要的答案，让使用更轻松</p>
      </div>

      <!-- 搜索框 -->
      <div class="mb-8 sm:mb-12">
        <div class="relative max-w-2xl mx-auto">
          <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 sm:w-6 sm:h-6" style="color: var(--theme-text-secondary);" />
          <input 
            v-model="searchQuery"
            type="text"
            placeholder="搜索您的问题..."
            class="w-full pl-12 sm:pl-14 pr-4 py-3 sm:py-4 rounded-2xl border focus:outline-none focus:ring-2 focus:ring-primary transition-all"
            style="background-color: var(--theme-surface); border-color: var(--theme-border); color: var(--theme-text);"
          />
        </div>
      </div>

      <!-- 分类卡片 -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 mb-10 sm:mb-14">
        <div 
          v-for="(category, index) in categories" 
          :key="index"
          class="p-4 sm:p-6 rounded-2xl border cursor-pointer hover:shadow-lg transition-all duration-300 hover:-translate-y-1"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <div class="w-10 h-10 sm:w-12 sm:h-12 rounded-xl flex items-center justify-center mb-3 sm:mb-4" style="background-color: var(--theme-accent);">
            <component :is="category.icon" class="w-5 h-5 sm:w-6 sm:h-6" style="color: var(--theme-primary);" />
          </div>
          <h3 class="font-semibold text-sm sm:text-base mb-1.5" style="color: var(--theme-text);">{{ category.title }}</h3>
          <p class="text-xs sm:text-sm" style="color: var(--theme-text-secondary);">{{ category.description }}</p>
        </div>
      </div>

      <!-- 常见问题 -->
      <div class="mb-8 sm:mb-12">
        <h2 class="text-xl sm:text-2xl font-bold mb-6 sm:mb-8" style="color: var(--theme-text);">常见问题</h2>
        <div class="space-y-3 sm:space-y-4">
          <div 
            v-for="faq in filteredFaqs" 
            :key="faq.id"
            class="p-4 sm:p-6 rounded-2xl border"
            style="background-color: var(--theme-surface); border-color: var(--theme-border);"
          >
            <button 
              @click="toggleFaq(faq.id)"
              class="w-full text-left flex items-center justify-between gap-4"
            >
              <span class="font-medium text-sm sm:text-base" style="color: var(--theme-text);">{{ faq.question }}</span>
              <ChevronDown 
                class="w-4 h-4 sm:w-5 sm:h-5 transition-transform duration-200 flex-shrink-0"
                :class="{ 'rotate-180': expandedFaq === faq.id }"
                style="color: var(--theme-text-secondary);"
              />
            </button>
            <div 
              v-if="expandedFaq === faq.id"
              class="mt-4 text-sm"
              style="color: var(--theme-text-secondary);"
            >
              {{ faq.answer }}
            </div>
          </div>
        </div>
        <div v-if="filteredFaqs.length === 0" class="text-center py-8 sm:py-12" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); border-radius: 1rem;">
          <Search class="w-10 h-10 sm:w-12 sm:h-12 mx-auto mb-4" style="color: var(--theme-text-secondary);" />
          <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">没有找到相关问题</p>
        </div>
      </div>

      <!-- 联系我们 -->
      <div class="p-6 sm:p-8 rounded-2xl text-center" style="background-color: var(--theme-accent);">
        <h3 class="text-lg sm:text-xl font-semibold mb-2" style="color: var(--theme-text);">没有找到答案？</h3>
        <p class="text-xs sm:text-sm mb-5" style="color: var(--theme-text-secondary);">我们很乐意为您提供帮助</p>
        <button 
          class="inline-flex items-center gap-2 px-5 sm:px-6 py-2.5 sm:py-3 rounded-xl font-medium transition-all hover:opacity-90"
          style="background-color: var(--theme-primary); color: white;"
        >
          <MessageSquare class="w-4 h-4 sm:w-5 sm:h-5" />
          联系客服
        </button>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
