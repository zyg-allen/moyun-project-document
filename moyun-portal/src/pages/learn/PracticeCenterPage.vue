<script setup lang="ts">
import { computed } from 'vue';
import { useHead } from '@vueuse/head';
import { useRouter } from 'vue-router';
import { CheckSquare, Code2, ChevronRight } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';

const router = useRouter();

useHead(computed(() => generateSeo({
  title: '刷题中心',
  description: '在线刷题中心 - 选择题练习、编程题练习，全面提升技术能力',
  keywords: ['刷题', '选择题', '编程题', '在线练习', '墨韵'],
  canonicalPath: '/learn/practice',
})));

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '刷题中心' },
]);

const practiceModules = [
  {
    name: '选择题练习',
    desc: '计算机基础、八股文选择题库，即时判定与解析',
    icon: CheckSquare,
    color: 'linear-gradient(135deg, #DC2626, #B91C1C)',
    path: '/learn/practice/choice',
    badge: 'HOT',
  },
  {
    name: '编程题练习',
    desc: '算法题、数据结构，在线 IDE 编写代码，测试用例判定',
    icon: Code2,
    color: 'linear-gradient(135deg, #7C3AED, #5B21B6)',
    path: '/learn/practice/coding',
    badge: 'NEW',
  },
];

function goto(path: string) {
  router.push(path);
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <main class="flex-1 w-full max-w-[1280px] mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <Breadcrumb :items="breadcrumbs" />

      <!-- 页头 -->
      <div class="mt-4 mb-8 text-center">
        <h1 class="text-2xl font-bold mb-2" style="color: var(--theme-text);">刷题中心</h1>
        <p class="text-sm" style="color: var(--theme-text-secondary);">选择练习模式，开始你的刷题之旅</p>
      </div>

      <!-- 练习模式卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 max-w-3xl mx-auto">
        <div
          v-for="m in practiceModules"
          :key="m.name"
          @click="goto(m.path)"
          class="p-6 rounded-2xl border cursor-pointer transition-all hover:shadow-lg hover:-translate-y-0.5 group"
          style="background-color: var(--theme-card-bg); border-color: var(--theme-border);"
        >
          <div class="flex items-start gap-4">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center text-white flex-shrink-0"
                 :style="{ background: m.color }">
              <component :is="m.icon" class="w-6 h-6" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1.5">
                <h3 class="text-base font-bold" style="color: var(--theme-text);">{{ m.name }}</h3>
                <span v-if="m.badge === 'NEW'" class="px-1.5 py-0.5 rounded-full text-[9px] font-bold text-white" style="background: linear-gradient(90deg,#ef4444,#f97316);">NEW</span>
                <span v-else-if="m.badge === 'HOT'" class="px-1.5 py-0.5 rounded-full text-[9px] font-bold text-white" style="background-color: #D97706;">HOT</span>
              </div>
              <p class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">{{ m.desc }}</p>
            </div>
            <ChevronRight class="w-5 h-5 flex-shrink-0 mt-3 group-hover:translate-x-1 transition-transform"
                          style="color: var(--theme-text-secondary);" />
          </div>
        </div>
      </div>
    </main>
    <SiteFooter />
  </div>
</template>
