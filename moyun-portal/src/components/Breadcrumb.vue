<script setup lang="ts">
import { useRouter } from 'vue-router';
import { ChevronRight, Home } from 'lucide-vue-next';
import { computed } from 'vue';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

interface Props {
  // 业务路径，不应包含"首页"——首页由组件内部自动补齐
  items: BreadcrumbItem[];
}

const props = defineProps<Props>();
const router = useRouter();

function navigateTo(path?: string) {
  if (path) {
    router.push(path);
  }
}

// 完整面包屑：自动在最前面补"首页"项
const fullItems = computed<BreadcrumbItem[]>(() => [
  { label: '首页', path: '/' },
  ...props.items
]);

// 生成面包屑结构化数据（供父组件注入 JSON-LD 使用）
const breadcrumbJsonLd = computed(() => {
  const items = fullItems.value
    .filter(item => item.path)
    .map(item => ({
      name: item.label,
      url: item.path || ''
    }))
    .filter(item => item.url);

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
  <nav class="flex items-center space-x-1 text-xs sm:text-sm overflow-hidden" aria-label="面包屑导航">
    <template v-for="(item, index) in fullItems" :key="index">
      <button
        v-if="index === 0"
        @click="navigateTo(item.path)"
        class="flex items-center gap-1 hover:opacity-80 transition-opacity flex-shrink-0"
        style="color: var(--theme-text-secondary);"
      >
        <Home class="w-3 h-3 sm:w-4 sm:h-4 flex-shrink-0" />
        <span class="hidden sm:inline truncate">{{ item.label }}</span>
      </button>
      <button
        v-else-if="item.path && index < fullItems.length - 1"
        @click="navigateTo(item.path)"
        class="hover:opacity-80 transition-opacity truncate"
        style="color: var(--theme-text-secondary);"
      >
        {{ item.label }}
      </button>
      <span
        v-else
        class="font-medium truncate"
        style="color: var(--theme-text);"
      >
        {{ item.label }}
      </span>
      <ChevronRight
        v-if="index < fullItems.length - 1"
        class="w-3 h-3 sm:w-4 sm:h-4 flex-shrink-0"
        style="color: var(--theme-text-secondary);"
      />
    </template>
  </nav>
</template>
