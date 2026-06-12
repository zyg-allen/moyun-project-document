<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ChevronRight, Home } from 'lucide-vue-next';
import { computed } from 'vue';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

interface Props {
  items: BreadcrumbItem[];
}

const props = defineProps<Props>();
const router = useRouter();

function navigateTo(path?: string) {
  if (path) {
    router.push(path);
  }
}

// 生成面包屑结构化数据（供父组件注入 JSON-LD 使用）
const breadcrumbJsonLd = computed(() => {
  const items = [
    { name: '首页', url: '/' },
    ...props.items.map(item => ({
      name: item.label,
      url: item.path || ''
    }))
  ].filter(item => item.url);

  return {
    '@context': 'https://schema.org',
    '@type': 'BreadcrumbList',
    itemListElement: items.map((item, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: item.name,
      item: item.url.startsWith('http') ? item.url : `${window.location.origin}${item.url}`
    }))
  };
});

// 暴露给父组件使用
defineExpose({ breadcrumbJsonLd });
</script>

<template>
  <nav class="flex items-center space-x-1 text-xs sm:text-sm" aria-label="面包屑导航">
    <button 
      @click="navigateTo('/')"
      class="flex items-center gap-1 hover:opacity-80 transition-opacity"
      style="color: var(--theme-text-secondary);"
    >
      <Home class="w-3 h-3 sm:w-4 sm:h-4" />
      <span class="hidden sm:inline">首页</span>
    </button>
    
    <template v-for="(item, index) in items" :key="index">
      <ChevronRight class="w-3 h-3 sm:w-4 sm:h-4" style="color: var(--theme-text-secondary);" />
      <button 
        v-if="item.path && index < items.length - 1"
        @click="navigateTo(item.path)"
        class="hover:opacity-80 transition-opacity"
        style="color: var(--theme-text-secondary);"
      >
        {{ item.label }}
      </button>
      <span 
        v-else
        class="font-medium"
        style="color: var(--theme-text);"
      >
        {{ item.label }}
      </span>
    </template>
  </nav>
</template>
