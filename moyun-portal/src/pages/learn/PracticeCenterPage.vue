<script setup lang="ts">
import { computed } from 'vue';
import { useHead } from '@vueuse/head';
import { useRouter } from 'vue-router';
import { CheckSquare, Code2, BookOpen, ChevronRight } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { useDictData } from '@/composables/useDictData';

const router = useRouter();

useHead(computed(() => generateSeo({
  title: '刷题中心',
  description: '在线刷题中心 - 题库阅读、选择题练习、编程题练习三种模式，学习行为计入成长记录',
  keywords: ['刷题', '选择题', '编程题', '题库阅读', '在线练习', '旭林'],
  canonicalPath: '/learn/practice',
})));

const breadcrumbs = computed(() => [
  { label: '学习中心', path: '/learn' },
  { label: '刷题中心' },
]);

/**
 * 练习模式卡片（清单 P2）。
 *
 * <p>模式**名称**原为硬编码，而后端已有字典 `portal_practice_mode`
 * （reading=展示阅读 / choice=选择题 / coding=编程题）与匿名取字典接口；
 * 现改为**字典优先 + 本地兜底**（与题库页、后台题库管理的口径一致），后台改名即时生效。
 * 说明：`desc`/`icon`/`color`/`path` 属前端展示与路由，字典里没有对应字段，故保留在此（非"数据"而是"呈现"）。</p>
 */
const practiceDict = useDictData(['portal_practice_mode']);

/** 字典 code → 展示名（取不到时用兜底） */
function modeLabel(code: string, fallback: string): string {
  const items = practiceDict['portal_practice_mode'];
  const hit = items?.find(i => i.dictValue === code);
  return hit?.dictLabel || fallback;
}

const practiceModules = computed(() => [
  {
    name: modeLabel('choice', '选择题练习'),
    desc: '选项作答 · 服务端判分，即时给出答案与解析',
    icon: CheckSquare,
    color: 'linear-gradient(135deg, #DC2626, #B91C1C)',
    path: '/learn/practice/choice',
  },
  {
    name: modeLabel('coding', '编程题练习'),
    desc: '在线 IDE 编写代码，运行全部测试用例判定',
    icon: Code2,
    color: 'linear-gradient(135deg, #7C3AED, #5B21B6)',
    path: '/learn/practice/coding',
  },
  {
    name: modeLabel('reading', '题库阅读'),
    // 清单 P2：/learn/questions 列表页只渲染题目与分类，答案/解析在详情页 ⇒ 描述不应承诺"直接查看答案解析"
    desc: '按分类浏览题目，进入详情查看参考答案与解析',
    icon: BookOpen,
    color: 'linear-gradient(135deg, #0EA5E9, #0369A1)',
    path: '/learn/questions',
  },
]);

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
        <p class="text-sm" style="color: var(--theme-text-secondary);">阅读、选择、编程三种练习模式，登录后学习行为计入成长记录</p>
      </div>

      <!-- 练习模式卡片 -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-4xl mx-auto">
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
              <!--
                清单 P2：HOT/NEW 徽标原为写死在数组里的营销标（无任何后端字段支撑），
                已移除；如确需角标，应由后台可配置字段下发而不是前端写死。
              -->
              <div class="flex items-center gap-2 mb-1.5">
                <h3 class="text-base font-bold" style="color: var(--theme-text);">{{ m.name }}</h3>
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
