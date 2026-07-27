<script setup lang="ts">
import {computed, onMounted, ref} from 'vue';
import LazyImage from './LazyImage.vue';
import {getAdList} from '@/api/ad';
import type {AdSlot} from '@/api/ad';
import {X} from 'lucide-vue-next';

interface Props {
  slotKey: string;
  limit?: number;
}

const props = withDefaults(defineProps<Props>(), {
  limit: 1,
});

const ads = ref<AdSlot[]>([]);
const loading = ref(true);
// 已关闭广告 id 集合（同会话内不复活，下次访问复活）
const closedAds = ref<Set<number>>(new Set());
const STORAGE_KEY = `ad-closed-${props.slotKey}`;

// 可见广告：过滤已关闭
const visibleAds = computed(() => ads.value.filter(ad => !closedAds.value.has(ad.id)));

onMounted(async () => {
  if (!props.slotKey) return;
  // 恢复本会话已关闭的广告 id
  try {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    if (saved) closedAds.value = new Set(JSON.parse(saved));
  } catch {
    // 解析失败忽略
  }
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

// 关闭单条广告：阻止冒泡（避免触发外层跳转），加入关闭集合并持久化
function handleClose(ad: AdSlot, e: Event) {
  e.stopPropagation();
  closedAds.value.add(ad.id);
  // 重新赋值触发响应式
  closedAds.value = new Set(closedAds.value);
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify([...closedAds.value]));
  } catch {
    // 持久化失败忽略
  }
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
    <div class="aspect-[4/1]" style="background-color: var(--theme-border);"></div>
    <div class="p-4 space-y-2">
      <div class="h-4 w-3/4 rounded" style="background-color: var(--theme-border);"></div>
      <div class="h-3 w-full rounded" style="background-color: var(--theme-border);"></div>
    </div>
  </div>

  <!-- 有可见广告：渲染列表 -->
  <template v-else-if="visibleAds.length > 0">
    <div
        v-for="ad in visibleAds"
        :key="ad.id"
        role="link"
        tabindex="0"
        class="group relative rounded-2xl overflow-hidden border shadow-sm hover:shadow-lg hover:-translate-y-1 transition-all duration-300 cursor-pointer"
        style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        :aria-label="'广告: ' + ad.title"
        @click="handleOpen(ad.link)"
        @keydown.enter="handleOpen(ad.link)"
    >
      <!-- 广告图：4:1 比例，符合广告位视觉，避免 16:9 过高 -->
      <div v-if="ad.image" class="relative overflow-hidden">
        <LazyImage
            :src="ad.image"
            :alt="ad.title"
            :aspect-ratio="4/1"
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

      <!-- 关闭按钮：右上角，点击仅关闭本条广告，不触发外层跳转 -->
      <button
          type="button"
          class="absolute top-2 right-2 w-7 h-7 rounded-full flex items-center justify-center transition hover:scale-110 focus:outline-none"
          style="background-color: rgba(0,0,0,0.5); color: #fff; backdrop-filter: blur(4px);"
          aria-label="关闭广告"
          title="关闭广告"
          @click="handleClose(ad, $event)"
          @keydown.enter.stop.prevent="handleClose(ad, $event)"
      >
        <X class="w-4 h-4" />
      </button>
    </div>
  </template>
  <!-- 所有广告已关闭或无广告：不渲染任何内容 -->
</template>
