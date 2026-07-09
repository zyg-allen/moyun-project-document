<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Pen, BookOpen, Briefcase, Star, Calendar, ChevronRight } from 'lucide-vue-next';
import BackButton from '@/components/BackButton.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getTimeline, getMyGrowth } from '@/api/growth';
import type { GrowthTimelineItem, UserGrowthVO } from '@/types/api';
import { formatRelativeTime } from '@/utils/date';

const router = useRouter();
useHead(generateSeo({
  title: '成长时间线',
  description: '记录每一步成长足迹',
  keywords: ['成长时间线', '学习记录', '成长画像'],
  type: 'website'
}));

const loading = ref(false);
const loadingMore = ref(false);
const timeline = ref<GrowthTimelineItem[]>([]);
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const noMore = ref(false);
const activeModule = ref<string>('all');
const growthInfo = ref<UserGrowthVO | null>(null);

const moduleTabs = [
  { key: 'all', label: '全部', icon: Star },
  { key: 'reading', label: '读书', icon: BookOpen },
  { key: 'interview', label: '面试', icon: Briefcase },
  { key: 'article', label: '创作', icon: Pen },
];

const iconMap: Record<string, any> = {
  'pen': Pen,
  'book-open': BookOpen,
  'briefcase': Briefcase,
  'star': Star,
};

async function load(reset = false) {
  if (reset) {
    page.value = 1;
    timeline.value = [];
    noMore.value = false;
  }
  if (noMore.value) return;

  if (reset) loading.value = true;
  else loadingMore.value = true;

  try {
    const resp = await getTimeline({
      pageNum: page.value,
      pageSize,
      module: activeModule.value,
    });
    if (resp.code === 200 && resp.data) {
      const list = resp.data.list || [];
      timeline.value = reset ? list : [...timeline.value, ...list];
      total.value = resp.data.total || 0;
      if (list.length < pageSize) {
        noMore.value = true;
      } else {
        page.value += 1;
      }
    }
  } catch {
    // 静默失败
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
}

async function switchModule(mod: string) {
  activeModule.value = mod;
  await load(true);
}

function goTarget(item: GrowthTimelineItem) {
  if (item.targetUrl) {
    router.push(item.targetUrl);
  }
}

function handleScroll() {
  if (loading.value || loadingMore.value || noMore.value) return;
  const scrollTop = window.scrollY;
  const clientHeight = window.innerHeight;
  const scrollHeight = document.documentElement.scrollHeight;
  if (scrollTop + clientHeight >= scrollHeight - 200) {
    load(false);
  }
}

onMounted(async () => {
  load(true);
  window.addEventListener('scroll', handleScroll);
  // 加载成长概览
  try {
    const res = await getMyGrowth();
    if (res.code === 200) growthInfo.value = res.data;
  } catch {}
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="min-h-screen" style="background-color: var(--theme-bg);">
    <!-- Header -->
    <header class="sticky top-0 z-10 backdrop-blur" style="background-color: var(--theme-surface); border-bottom: 1px solid var(--theme-border);">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between">
        <BackButton />
        <h1 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">成长时间线</h1>
        <span class="w-8" />
      </div>
    </header>

    <main class="max-w-4xl mx-auto px-4 sm:px-6 py-6 pb-20">
      <!-- 成长概览卡片 -->
      <div v-if="growthInfo" class="rounded-xl p-6 mb-6" style="background: linear-gradient(135deg, var(--theme-primary), var(--theme-primary-dark, var(--theme-primary))); color: white;">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm opacity-80">当前等级</p>
            <p class="text-3xl font-bold mt-1">Lv.{{ growthInfo.level || 1 }}</p>
            <p class="text-sm opacity-80 mt-1">{{ growthInfo.title || '初出茅庐' }}</p>
          </div>
          <div class="text-right">
            <p class="text-sm opacity-80">成长值</p>
            <p class="text-2xl font-bold mt-1">{{ growthInfo.growthValue || 0 }}</p>
            <p class="text-xs opacity-60 mt-1">本季 {{ growthInfo.seasonValue || 0 }}</p>
          </div>
        </div>
        <div class="mt-4 h-2 rounded-full bg-white/20 overflow-hidden">
          <div
            class="h-full bg-white rounded-full transition-all duration-500"
            :style="{ width: `${Math.min(100, ((growthInfo.growthValue || 0) % 100))}%` }"
          />
        </div>
      </div>

      <!-- 模块筛选 Tab -->
      <div class="flex gap-2 mb-6 overflow-x-auto pb-1">
        <button
          v-for="tab in moduleTabs"
          :key="tab.key"
          class="flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition"
          :style="activeModule === tab.key
            ? 'background-color: var(--theme-primary); color: white;'
            : 'background-color: var(--theme-surface); color: var(--theme-text-secondary); border: 1px solid var(--theme-border);'"
          @click="switchModule(tab.key)"
        >
          <component :is="tab.icon" class="w-4 h-4" />
          {{ tab.label }}
        </button>
      </div>

      <!-- 时间线列表 -->
      <div v-if="loading && timeline.length === 0" class="text-center py-16" style="color: var(--theme-text-secondary);">
        加载中...
      </div>
      <div v-else-if="timeline.length === 0">
        <Empty description="还没有成长记录，去读书或做题吧" />
      </div>
      <template v-else>
        <!-- 时间线 -->
        <div class="relative">
          <!-- 竖线 -->
          <div class="absolute left-5 top-0 bottom-0 w-0.5" style="background-color: var(--theme-border);" />

          <div class="space-y-4">
            <div
              v-for="item in timeline"
              :key="item.id"
              class="relative flex gap-4"
            >
              <!-- 图标节点 -->
              <div
                class="relative z-10 flex items-center justify-center w-10 h-10 rounded-full flex-shrink-0"
                style="background-color: var(--theme-primary); color: white;"
              >
                <component :is="iconMap[item.icon || 'star'] || Star" class="w-5 h-5" />
              </div>

              <!-- 内容卡片 -->
              <div
                class="flex-1 rounded-lg p-4 transition cursor-pointer hover:shadow-md"
                :style="{ 'background-color': 'var(--theme-surface)', 'border': '1px solid var(--theme-border)' }"
                @click="goTarget(item)"
              >
                <div class="flex items-start justify-between gap-2">
                  <div class="min-w-0 flex-1">
                    <div class="flex items-center gap-2 mb-1">
                      <span class="text-sm font-semibold" style="color: var(--theme-primary);">
                        {{ item.actionLabel }}
                      </span>
                      <span
                        v-if="item.growthDelta && item.growthDelta > 0"
                        class="text-xs px-1.5 py-0.5 rounded"
                        style="background-color: var(--theme-primary); color: white; opacity: 0.9;"
                      >
                        +{{ item.growthDelta }}
                      </span>
                    </div>
                    <p v-if="item.targetTitle" class="text-sm truncate" style="color: var(--theme-text);">
                      {{ item.targetTitle }}
                    </p>
                    <p v-if="item.description" class="text-xs mt-1" style="color: var(--theme-text-secondary);">
                      {{ item.description }}
                    </p>
                  </div>
                  <!-- 封面缩略图 -->
                  <div v-if="item.targetCover" class="w-12 h-16 rounded overflow-hidden flex-shrink-0">
                    <LazyImage
                      :src="item.targetCover"
                      :alt="item.targetTitle"
                      class="w-full h-full object-cover"
                    />
                  </div>
                </div>
                <!-- 时间 -->
                <div class="flex items-center gap-2 mt-2 text-xs" style="color: var(--theme-text-secondary);">
                  <Calendar class="w-3 h-3" />
                  <span>{{ formatRelativeTime(item.createTime) }}</span>
                  <span v-if="item.targetUrl" class="ml-auto flex items-center gap-0.5">
                    查看 <ChevronRight class="w-3 h-3" />
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="loadingMore" class="text-center py-8 text-sm" style="color: var(--theme-text-secondary);">
          加载更多...
        </div>
        <div v-else-if="noMore && timeline.length > 0" class="text-center py-8 text-xs" style="color: var(--theme-text-secondary);">
          —— 没有更多了 ——
        </div>
      </template>
    </main>

    <SiteFooter />
  </div>
</template>
