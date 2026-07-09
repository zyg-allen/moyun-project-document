<script setup lang="ts">
import { useHead } from '@vueuse/head';
import { Globe, Users, BookOpen, Heart, Award, Target } from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { generateSeo } from '@/utils/seo';

useHead(
  generateSeo({
    title: '关于我们',
    description: '了解墨韵的使命、愿景和团队。',
    keywords: ['关于我们', '墨韵', '团队'],
    type: 'website'
  })
);

const features = [
  { icon: Target, title: '我们的使命', description: '让优质内容触达更多人，为创作者提供展示才华的舞台' },
  { icon: Heart, title: '我们的愿景', description: '成为中文互联网最有影响力的内容社区之一' },
  { icon: BookOpen, title: '我们的理念', description: '尊重原创，鼓励分享，构建开放包容的内容生态' }
];

const values = [
  { icon: Award, title: '尊重原创', description: '尊重每一位创作者的劳动成果' },
  { icon: Users, title: '用户至上', description: '以用户需求为核心，持续优化产品' },
  { icon: Globe, title: '开放共享', description: '让知识和创意自由流动' }
];

const team = [
  { name: '张三', role: '产品负责人', avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop' },
  { name: '李四', role: '技术负责人', avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop' },
  { name: '王五', role: '运营负责人', avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop' }
];
</script>

<template>
  <div style="background-color: var(--theme-bg); min-height: 100vh;" class="flex flex-col">
    <!-- 面包屑 -->
    <div class="border-b py-3 sm:py-4" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Breadcrumb :items="[{ label: '关于我们' }]" />
      </div>
    </div>

    <!-- 主内容 -->
    <div class="flex-1">
      <!-- Hero 区域 -->
      <div class="py-6 sm:py-8">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="relative overflow-hidden rounded-2xl text-white text-center" style="background-image: radial-gradient(circle at 30% 30%, rgba(220, 38, 38, 0.3) 0%, transparent 50%), radial-gradient(circle at 70% 70%, rgba(234, 88, 12, 0.3) 0%, transparent 50%), linear-gradient(135deg, #b91c1c 0%, #c2410c 50%, #ea580c 100%);">
        <div class="absolute inset-0 opacity-10 pointer-events-none" aria-hidden="true">
          <svg class="absolute top-6 left-8 w-32 h-32 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>
          <svg class="absolute bottom-4 right-10 w-40 h-40 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
        </div>
        <div class="relative px-6 py-8 sm:px-10 sm:py-10">
          <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold mb-4 sm:mb-6" style="color: #ffffff;">关于墨韵</h1>
          <p class="text-sm sm:text-lg lg:text-xl" style="color: rgba(255,255,255,0.9); max-width: 2xl mx-auto;">
            墨韵是一个专注于原创内容分享的社区平台，我们相信每一篇文章都有独特的价值
          </p>
        </div>
      </div>
        </div>
      </div>

      <!-- 特色介绍 -->
      <div class="py-12 sm:py-16 lg:py-20">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl sm:text-3xl font-bold text-center mb-10 sm:mb-14" style="color: var(--theme-text);">我们的理念</h2>
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 sm:gap-8">
            <div 
              v-for="feature in features" 
              :key="feature.title"
              class="p-6 sm:p-8 rounded-2xl border text-center"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <div class="w-12 h-12 sm:w-16 sm:h-16 rounded-xl flex items-center justify-center mx-auto mb-4 sm:mb-6" style="background-color: var(--theme-accent);">
                <component :is="feature.icon" class="w-6 h-6 sm:w-8 sm:h-8" style="color: var(--theme-primary);" />
              </div>
              <h3 class="text-lg sm:text-xl font-semibold mb-3" style="color: var(--theme-text);">{{ feature.title }}</h3>
              <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">{{ feature.description }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 核心价值观 -->
      <div class="py-12 sm:py-16 lg:py-20" style="background-color: var(--theme-surface);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl sm:text-3xl font-bold text-center mb-10 sm:mb-14" style="color: var(--theme-text);">核心价值观</h2>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8">
            <div 
              v-for="value in values" 
              :key="value.title"
              class="text-center"
            >
              <div class="w-12 h-12 sm:w-16 sm:h-16 rounded-full flex items-center justify-center mx-auto mb-4 sm:mb-6" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                <component :is="value.icon" class="w-6 h-6 sm:w-8 sm:h-8 text-white" />
              </div>
              <h3 class="text-lg sm:text-xl font-semibold mb-2" style="color: var(--theme-text);">{{ value.title }}</h3>
              <p class="text-sm sm:text-base" style="color: var(--theme-text-secondary);">{{ value.description }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 团队介绍 -->
      <div class="py-12 sm:py-16 lg:py-20">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl sm:text-3xl font-bold text-center mb-10 sm:mb-14" style="color: var(--theme-text);">我们的团队</h2>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-6 sm:gap-8">
            <div 
              v-for="member in team" 
              :key="member.name"
              class="text-center"
            >
              <div class="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl overflow-hidden mx-auto mb-4 sm:mb-6">
                <img :src="member.avatar" :alt="member.name" class="w-full h-full object-cover" />
              </div>
              <h3 class="text-lg sm:text-xl font-semibold mb-1.5" style="color: var(--theme-text);">{{ member.name }}</h3>
              <p class="text-sm" style="color: var(--theme-text-secondary);">{{ member.role }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 发展历程 -->
      <div class="py-12 sm:py-16 lg:py-20" style="background-color: var(--theme-accent);">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 class="text-2xl sm:text-3xl font-bold text-center mb-10 sm:mb-14" style="color: var(--theme-text);">发展历程</h2>
          <div class="relative">
            <!-- 时间线 -->
            <div class="absolute left-4 sm:left-1/2 top-0 bottom-0 w-px sm:-translate-x-1/2" style="background-color: var(--theme-border);"></div>
            <div class="space-y-8 sm:space-y-12">
              <div class="relative flex items-start gap-4 sm:gap-8">
                <div class="flex sm:justify-end w-full sm:w-1/2 text-right pl-12 sm:pl-0 sm:pr-8">
                  <div>
                    <h3 class="font-semibold text-base sm:text-lg mb-1.5" style="color: var(--theme-text);">2023年1月</h3>
                    <p class="text-sm" style="color: var(--theme-text-secondary);">墨韵正式上线，开始内测</p>
                  </div>
                </div>
                <div class="absolute left-4 sm:left-1/2 w-3 h-3 rounded-full -translate-x-1/2" style="background-color: var(--theme-primary);"></div>
                <div class="hidden sm:block w-1/2"></div>
              </div>
              <div class="relative flex items-start gap-4 sm:gap-8">
                <div class="hidden sm:block w-1/2"></div>
                <div class="absolute left-4 sm:left-1/2 w-3 h-3 rounded-full -translate-x-1/2" style="background-color: var(--theme-primary);"></div>
                <div class="pl-12 sm:pl-8 w-full sm:w-1/2">
                  <h3 class="font-semibold text-base sm:text-lg mb-1.5" style="color: var(--theme-text);">2023年6月</h3>
                  <p class="text-sm" style="color: var(--theme-text-secondary);">用户数突破10万</p>
                </div>
              </div>
              <div class="relative flex items-start gap-4 sm:gap-8">
                <div class="flex sm:justify-end w-full sm:w-1/2 text-right pl-12 sm:pl-0 sm:pr-8">
                  <div>
                    <h3 class="font-semibold text-base sm:text-lg mb-1.5" style="color: var(--theme-text);">2024年1月</h3>
                    <p class="text-sm" style="color: var(--theme-text-secondary);">文章数超过100万篇</p>
                  </div>
                </div>
                <div class="absolute left-4 sm:left-1/2 w-3 h-3 rounded-full -translate-x-1/2" style="background-color: var(--theme-primary);"></div>
                <div class="hidden sm:block w-1/2"></div>
              </div>
              <div class="relative flex items-start gap-4 sm:gap-8">
                <div class="hidden sm:block w-1/2"></div>
                <div class="absolute left-4 sm:left-1/2 w-3 h-3 rounded-full -translate-x-1/2" style="background-color: var(--theme-primary);"></div>
                <div class="pl-12 sm:pl-8 w-full sm:w-1/2">
                  <h3 class="font-semibold text-base sm:text-lg mb-1.5" style="color: var(--theme-text);">未来</h3>
                  <p class="text-sm" style="color: var(--theme-text-secondary);">更多精彩，敬请期待</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Footer -->
    <SiteFooter />
  </div>
</template>
