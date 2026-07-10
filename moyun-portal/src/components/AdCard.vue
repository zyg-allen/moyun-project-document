<script setup lang="ts">
import {onMounted, ref} from 'vue';
import LazyImage from './LazyImage.vue';
import {getAdList} from '@/api/ad';
import type {AdSlot} from '@/api/ad';

interface Props {
  slotKey: string;
  limit?: number;
}

const props = withDefaults(defineProps<Props>(), {
  limit: 1,
});

const ads = ref<AdSlot[]>([]);
const loading = ref(true);

onMounted(async () => {
  if (!props.slotKey) return;
  loading.value = true;
  try {
    const res = await getAdList(props.slotKey);
    const list = (res.data || []).slice(0, props.limit);
    ads.value = list;
  } catch (e) {
    // 静默失败，不报错
    ads.value = [];
  } finally {
    loading.value = false;
  }
});

function handleOpen(link: string) {
  if (!link) return;
  window.open(link, '_blank', 'noopener');
}
</script>

<template>
  <!-- 加载中：简单骨架占位 -->
  <div
      v-if="loading"
      class="rounded-2xl overflow-hidden border shadow-sm animate-pulse"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
      aria-hidden="true"
  >
    <div class="aspect-[16/9]" style="background-color: var(--theme-border);"></div>
    <div class="p-4 space-y-2">
      <div class="h-4 w-3/4 rounded" style="background-color: var(--theme-border);"></div>
      <div class="h-3 w-full rounded" style="background-color: var(--theme-border);"></div>
    </div>
  </div>

  <!-- 无广告：不渲染 -->
  <template v-else>
    <div
        v-for="ad in ads"
        :key="ad.id"
        role="link"
        tabindex="0"
        class="group relative rounded-2xl overflow-hidden border shadow-sm hover:shadow-lg hover:-translate-y-1 transition-all duration-300 cursor-pointer"
        style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        :aria-label="'广告: ' + ad.title"
        @click="handleOpen(ad.link)"
        @keydown.enter="handleOpen(ad.link)"
    >
      <!-- 广告图 -->
      <div v-if="ad.image" class="relative aspect-[16/9] overflow-hidden">
        <LazyImage
            :src="ad.image"
            :alt="ad.title"
            :aspect-ratio="16/9"
        />
      </div>

      <!-- 文案 -->
      <div class="p-4">
        <h3
            class="text-base font-bold mb-1 line-clamp-2"
            style="color: var(--theme-text);"
        >
          {{ ad.title }}
        </h3>
        <p
            v-if="ad.content"
            class="text-sm line-clamp-2"
            style="color: var(--theme-text-secondary);"
        >
          {{ ad.content }}
        </p>
      </div>

      <!-- 广告标识 -->
      <span
          class="absolute bottom-2 right-2 px-1.5 py-0.5 text-[10px] rounded leading-none"
          style="background-color: rgba(0,0,0,0.35); color: #fff;"
      >
        广告
      </span>
    </div>
  </template>
</template>
