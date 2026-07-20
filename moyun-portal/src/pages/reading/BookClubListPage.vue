<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Calendar, Users, FileText } from 'lucide-vue-next';
import BackButton from '@/components/BackButton.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import { generateSeo } from '@/utils/seo';
import { getBookClubList, type BookClubActivity } from '@/api/reading';

const router = useRouter();
useHead(generateSeo({ title: '共读活动', description: '加入共读，与书友一起打卡', keywords: ['共读活动', '读书会'], type: 'website' }));

const loading = ref(false);
const activities = ref<BookClubActivity[]>([]);

const statusMeta: Record<string, { label: string; color: string }> = {
  upcoming: { label: '即将开始', color: '#3b82f6' },
  ongoing: { label: '进行中', color: '#10b981' },
  ended: { label: '已结束', color: '#9ca3af' },
};

async function load() {
  loading.value = true;
  try {
    const resp = await getBookClubList({ pageNum: 1, pageSize: 30 });
    if (resp.code === 200 && resp.data) {
      const data = resp.data as any;
      activities.value = Array.isArray(data) ? data : data.records || [];
    } else {
      activities.value = [];
    }
  } catch {
    activities.value = [];
  } finally {
    loading.value = false;
  }
}

function goDetail(id: number) {
  router.push(`/reading/club/${id}`);
}

onMounted(load);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <header class="sticky top-0 z-30 backdrop-blur-sm" style="background-color: var(--theme-surface); border-bottom: 1px solid var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        <BackButton />
        <h1 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">共读活动</h1>
        <span class="w-8" />
      </div>
    </header>

    <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-20">
      <div v-if="loading" class="text-center py-16" style="color: var(--theme-text-secondary);">加载中...</div>
      <div v-else-if="activities.length === 0">
        <Empty description="暂无共读活动" />
      </div>
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <article
          v-for="act in activities"
          :key="act.id"
          class="rounded-xl overflow-hidden shadow-sm hover:shadow-md transition cursor-pointer"
          style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
          @click="goDetail(act.id)"
        >
          <div class="aspect-[3/2] relative" style="background-color: var(--theme-bg);">
            <LazyImage v-if="act.cover" :src="act.cover" :alt="act.title" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center" style="color: var(--theme-text-secondary);">
              <FileText class="w-10 h-10" />
            </div>
            <span
              v-if="act.status"
              class="absolute top-2 left-2 px-2 py-0.5 text-xs rounded text-white"
              :style="{ backgroundColor: statusMeta[act.status]?.color || '#6b7280' }"
            >{{ statusMeta[act.status]?.label || act.status }}</span>
          </div>
          <div class="p-4">
            <h2 class="font-bold text-base mb-2 line-clamp-2" style="color: var(--theme-text);">{{ act.title }}</h2>
            <p v-if="act.description" class="text-sm line-clamp-2 mb-3" style="color: var(--theme-text-secondary);">{{ act.description }}</p>
            <div class="flex items-center gap-4 text-xs" style="color: var(--theme-text-secondary);">
              <span class="flex items-center gap-1">
                <Calendar class="w-3.5 h-3.5" />{{ act.startDate || '未设置' }}
              </span>
              <span class="flex items-center gap-1">
                <Users class="w-3.5 h-3.5" />{{ act.participantsCount || 0 }} 人
              </span>
              <span class="flex items-center gap-1">
                <FileText class="w-3.5 h-3.5" />{{ act.recordsCount || 0 }} 条打卡
              </span>
            </div>
          </div>
        </article>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>
