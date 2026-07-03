<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Calendar, Users, FileText, Heart, Send } from 'lucide-vue-next';
import BackButton from '@/components/BackButton.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import Empty from '@/components/Empty.vue';
import Avatar from '@/components/Avatar.vue';
import { generateSeo } from '@/utils/seo';
import { getBookClubDetail, getBookClubRecords, joinBookClub, leaveBookClub, submitBookClubRecord, likeBookClubRecord, type BookClubActivity, type BookClubRecord } from '@/api/reading';
import { useUserStore } from '@/stores/user';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const toast = useToast();

const activityId = computed(() => Number(route.params.id));
const loading = ref(false);
const activity = ref<BookClubActivity | null>(null);
const records = ref<BookClubRecord[]>([]);
const recordsLoading = ref(false);
const joinLoading = ref(false);
const recordForm = ref({ content: '', recordType: 'reflection' });
const submitting = ref(false);
const likeLoadingSet = ref<Set<number>>(new Set());

useHead(generateSeo({ title: '共读活动详情', description: '查看共读活动详情并参与打卡', keywords: ['共读活动', '读后感'], type: 'website' }));

const statusMeta: Record<string, { label: string; color: string }> = {
  upcoming: { label: '即将开始', color: '#3b82f6' },
  ongoing: { label: '进行中', color: '#10b981' },
  ended: { label: '已结束', color: '#9ca3af' },
};

async function loadActivity() {
  loading.value = true;
  try {
    const resp = await getBookClubDetail(activityId.value);
    if (resp.code === 200 && resp.data) {
      activity.value = resp.data;
    } else {
      activity.value = null;
      toast.error(resp.message || '活动不存在');
    }
  } catch {
    activity.value = null;
  } finally {
    loading.value = false;
  }
}

async function loadRecords() {
  recordsLoading.value = true;
  try {
    const resp = await getBookClubRecords(activityId.value, { pageNum: 1, pageSize: 50 });
    if (resp.code === 200 && resp.data) {
      const data = resp.data as any;
      records.value = Array.isArray(data) ? data : data.records || [];
    } else {
      records.value = [];
    }
  } catch {
    records.value = [];
  } finally {
    recordsLoading.value = false;
  }
}

async function handleToggleJoin() {
  if (!activity.value) return;
  if (!userStore.isAuthenticated) {
    toast.warning('请先登录');
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  joinLoading.value = true;
  try {
    if (activity.value.isJoined) {
      await leaveBookClub(activityId.value);
      activity.value.isJoined = false;
      toast.success('已退出活动');
    } else {
      await joinBookClub(activityId.value);
      activity.value.isJoined = true;
      toast.success('已加入活动');
    }
  } catch {
    toast.error('操作失败');
  } finally {
    joinLoading.value = false;
  }
}

async function handleSubmitRecord() {
  if (!activity.value) return;
  if (!userStore.isAuthenticated) {
    toast.warning('请先登录');
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (!recordForm.value.content.trim()) {
    toast.warning('请输入打卡内容');
    return;
  }
  submitting.value = true;
  try {
    const resp = await submitBookClubRecord(activityId.value, {
      content: recordForm.value.content.trim(),
      recordType: recordForm.value.recordType,
    });
    if (resp.code === 200) {
      toast.success('打卡成功');
      recordForm.value.content = '';
      await loadRecords();
    } else {
      toast.error(resp.message || '打卡失败');
    }
  } catch {
    toast.error('打卡失败');
  } finally {
    submitting.value = false;
  }
}

async function handleLike(record: BookClubRecord) {
  if (!userStore.isAuthenticated) {
    toast.warning('请先登录');
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (likeLoadingSet.value.has(record.id)) return;
  likeLoadingSet.value.add(record.id);
  try {
    const resp = await likeBookClubRecord(record.id);
    if (resp.code === 200 && resp.data) {
      record.isLiked = resp.data.liked;
      record.likeCount = resp.data.likeCount ?? record.likeCount;
    }
  } catch {
    toast.error('点赞失败');
  } finally {
    likeLoadingSet.value.delete(record.id);
  }
}

onMounted(async () => {
  await loadActivity();
  await loadRecords();
});
</script>

<template>
  <div class="min-h-screen" style="background-color: var(--theme-bg);">
    <header class="sticky top-0 z-10 backdrop-blur" style="background-color: var(--theme-surface); border-bottom: 1px solid var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        <BackButton />
        <h1 class="text-base sm:text-lg font-bold" style="color: var(--theme-text);">共读活动</h1>
        <span class="w-8" />
      </div>
    </header>

    <main v-if="loading" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center" style="color: var(--theme-text-secondary);">加载中...</main>
    <main v-else-if="!activity" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
      <Empty description="活动不存在或已结束" />
    </main>
    <main v-else class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-20 grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- 活动信息 -->
      <section class="lg:col-span-1">
        <div class="rounded-xl overflow-hidden shadow-sm" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="aspect-video relative" style="background-color: var(--theme-bg);">
            <LazyImage v-if="activity.cover" :src="activity.cover" :alt="activity.title" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center" style="color: var(--theme-text-secondary);">
              <FileText class="w-12 h-12" />
            </div>
            <span
              v-if="activity.status"
              class="absolute top-2 left-2 px-2 py-0.5 text-xs rounded text-white"
              :style="{ backgroundColor: statusMeta[activity.status]?.color || '#6b7280' }"
            >{{ statusMeta[activity.status]?.label || activity.status }}</span>
          </div>
          <div class="p-5">
            <h1 class="text-xl font-bold mb-3" style="color: var(--theme-text);">{{ activity.title }}</h1>
            <div class="flex items-center gap-4 text-sm mb-4" style="color: var(--theme-text-secondary);">
              <span class="flex items-center gap-1"><Calendar class="w-4 h-4" />{{ activity.startDate }}~{{ activity.endDate }}</span>
            </div>
            <div class="flex items-center gap-4 text-sm mb-4" style="color: var(--theme-text-secondary);">
              <span class="flex items-center gap-1"><Users class="w-4 h-4" />{{ activity.participantsCount || 0 }} 人参与</span>
              <span class="flex items-center gap-1"><FileText class="w-4 h-4" />{{ activity.recordsCount || 0 }} 条打卡</span>
            </div>
            <p v-if="activity.description" class="text-sm leading-relaxed mb-4" style="color: var(--theme-text);">{{ activity.description }}</p>
            <button
              type="button"
              class="w-full py-2.5 rounded-lg font-medium text-sm transition"
              :style="activity.isJoined
                ? { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }
                : { backgroundColor: 'var(--theme-primary)', color: 'white' }"
              :disabled="joinLoading || activity.status === 'ended'"
              @click="handleToggleJoin"
            >
              {{ activity.status === 'ended' ? '活动已结束' : (activity.isJoined ? '退出活动' : '加入活动') }}
            </button>
          </div>
        </div>
      </section>

      <!-- 打卡区 -->
      <section class="lg:col-span-2 space-y-4">
        <div v-if="activity.isJoined && activity.status !== 'ended'" class="rounded-xl p-5 shadow-sm" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <h3 class="font-bold mb-3" style="color: var(--theme-text);">发布打卡</h3>
          <div class="flex gap-2 mb-3">
            <button
              type="button"
              class="px-3 py-1 text-xs rounded-full transition"
              :style="recordForm.recordType === 'reflection'
                ? { backgroundColor: 'var(--theme-primary)', color: 'white' }
                : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
              @click="recordForm.recordType = 'reflection'"
            >读后感</button>
            <button
              type="button"
              class="px-3 py-1 text-xs rounded-full transition"
              :style="recordForm.recordType === 'excerpt'
                ? { backgroundColor: 'var(--theme-primary)', color: 'white' }
                : { backgroundColor: 'var(--theme-bg)', color: 'var(--theme-text-secondary)', border: '1px solid var(--theme-border)' }"
              @click="recordForm.recordType = 'excerpt'"
            >摘抄</button>
          </div>
          <textarea
            v-model="recordForm.content"
            rows="4"
            class="w-full p-3 rounded-lg text-sm resize-none focus:outline-none"
            style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            placeholder="写下你的读后感或摘抄..."
          />
          <div class="flex justify-end mt-2">
            <button
              type="button"
              class="inline-flex items-center gap-1 px-4 py-1.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
              style="background-color: var(--theme-primary);"
              :disabled="submitting"
              @click="handleSubmitRecord"
            >
              <Send class="w-4 h-4" />
              {{ submitting ? '提交中...' : '提交打卡' }}
            </button>
          </div>
        </div>

        <div class="rounded-xl p-5 shadow-sm" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <h3 class="font-bold mb-4" style="color: var(--theme-text);">打卡记录（{{ records.length }}）</h3>
          <div v-if="recordsLoading" class="text-center py-8" style="color: var(--theme-text-secondary);">加载中...</div>
          <div v-else-if="records.length === 0">
            <Empty description="暂无打卡记录，快来抢沙发" />
          </div>
          <ul v-else class="space-y-4">
            <li v-for="record in records" :key="record.id" class="flex gap-3">
              <Avatar :src="record.authorAvatar" :name="record.authorName" size="md" />
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 text-sm mb-1">
                  <span class="font-medium" style="color: var(--theme-text);">{{ record.authorName || '匿名书友' }}</span>
                  <span v-if="record.recordType" class="px-1.5 py-0.5 text-xs rounded" style="background-color: var(--theme-bg); color: var(--theme-text-secondary);">
                    {{ record.recordType === 'excerpt' ? '摘抄' : '读后感' }}
                  </span>
                  <span class="text-xs" style="color: var(--theme-text-secondary);">{{ record.createTime }}</span>
                </div>
                <p class="text-sm leading-relaxed whitespace-pre-wrap break-words" style="color: var(--theme-text);">{{ record.content }}</p>
                <button
                  type="button"
                  class="mt-2 inline-flex items-center gap-1 text-xs transition hover:opacity-80 disabled:opacity-50"
                  :style="{ color: record.isLiked ? 'var(--theme-primary)' : 'var(--theme-text-secondary)' }"
                  :disabled="likeLoadingSet.has(record.id)"
                  @click="handleLike(record)"
                >
                  <Heart class="w-3.5 h-3.5" :fill="record.isLiked ? 'currentColor' : 'none'" />
                  {{ record.likeCount || 0 }}
                </button>
              </div>
            </li>
          </ul>
        </div>
      </section>
    </main>

    <SiteFooter />
  </div>
</template>
