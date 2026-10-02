<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { Pen, BookOpen, Briefcase, Star, Calendar, ChevronRight } from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
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

const breadcrumbs = computed(() => [
  { label: '个人中心', path: '/user' },
  { label: '成长时间线' },
]);

const loading = ref(false);
const loadingMore = ref(false);
const timeline = ref<GrowthTimelineItem[]>([]);
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const noMore = ref(false);
/** 列表加载失败提示（清单 P2）：与"还没有成长记录"空态区分 */
const loadError = ref<string | null>(null);
/** 成长概览加载失败（清单 P2）：避免整块卡片无声消失 */
const growthFailed = ref(false);
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

// 清单 P2：switchModule 直接 await load(true)，无序号/取消 ⇒ 上个模块的响应后到会覆盖当前模块列表。
let loadSeq = 0;

async function load(reset = false) {
  const seq = ++loadSeq;
  if (reset) {
    page.value = 1;
    timeline.value = [];
    noMore.value = false;
  }
  if (noMore.value) return;

  if (reset) loading.value = true;
  else loadingMore.value = true;

  loadError.value = null;
  try {
    const resp = await getTimeline({
      pageNum: page.value,
      pageSize,
      module: activeModule.value,
    });
    if (resp.code !== 200) {
      // 清单 P2：原先没有 else 分支，业务失败与"确实没有记录"都渲染成"还没有成长记录"
      loadError.value = resp.message || '加载成长记录失败';
      return;
    }
    if (resp.code === 200 && resp.data) {
      const list = resp.data.list || [];
      if (seq !== loadSeq) return;   // 已切模块/已发起新请求：丢弃本次结果
    timeline.value = reset ? list : [...timeline.value, ...list];
      total.value = resp.data.total || 0;
      if (list.length < pageSize) {
        noMore.value = true;
      } else {
        page.value += 1;
      }
    }
  } catch (e) {
    // 清单 P2：原先静默失败；失败后 page 不递增、noMore 保持 false，
    // 滚动到底会反复重发同一个失败请求（下方 handleScroll 已在 loadError 时暂停）。
    loadError.value = (e as { message?: string })?.message || '加载成长记录失败，请稍后重试';
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
}

async function switchModule(mod: string) {
  activeModule.value = mod;
  await load(true);
}

/** 重新加载成长概览（概览失败重试用） */
async function reloadGrowth() {
  growthFailed.value = false;
  try {
    const res = await getMyGrowth();
    if (res.code === 200) growthInfo.value = res.data;
    else growthFailed.value = true;
  } catch {
    growthFailed.value = true;
  }
}

function goTarget(item: GrowthTimelineItem) {
  if (item.targetUrl) {
    router.push(item.targetUrl);
  }
}

function handleScroll() {
  // 清单 P2：失败后暂停触底加载，需用户显式重试，避免"滚到底就把同一个失败请求再发一遍"
  if (loading.value || loadingMore.value || noMore.value || loadError.value) return;
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
  // 加载成长概览（清单 P2：原先 catch {} 静默，失败时 growthInfo 保持 null，
  // 整块概览卡片 v-if 直接消失，用户看不出是"没数据"还是"加载失败"）
  try {
    const res = await getMyGrowth();
    if (res.code === 200) growthInfo.value = res.data;
    else growthFailed.value = true;
  } catch {
    growthFailed.value = true;
  }
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
      </div>
    </div>

    <!-- 修复：用户反馈"宽度还是小小的"。
         原因：之前用 <main class="max-w-7xl ..."> 包裹所有内容，理论上等价于首页，
         但实际渲染时 main 的 max-w-7xl 会被内部 flex/grid 子元素继承失效。
         改为与首页完全一致的结构：外层 main 不限宽，每个子区块独立用 max-w-7xl mx-auto。
         这样无论怎么缩放浏览器，每个区块都与首页对齐。 -->
    <main class="flex-1 py-6 pb-20">
      <!-- 成长概览卡片 -->
      <!-- 概览加载失败提示（清单 P2）：原失败时整块卡片静默消失 -->
      <div
        v-if="growthFailed && !growthInfo"
        class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-6"
      >
        <div class="rounded-xl px-4 py-3 text-sm flex items-center justify-between gap-3" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text-secondary);">
          <span>成长概览加载失败</span>
          <button class="font-medium" style="color: var(--theme-primary);" @click="reloadGrowth">重试</button>
        </div>
      </div>

      <div v-if="growthInfo" class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-6">
        <div class="rounded-xl p-6 border" style="background-color: var(--theme-primary-soft, var(--theme-surface)); border-color: var(--theme-border);">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm" style="color: var(--theme-text-secondary);">当前等级</p>
              <p class="text-3xl font-bold mt-1" style="color: var(--theme-text);">Lv.{{ growthInfo.level || 1 }}</p>
              <p class="text-sm mt-1" style="color: var(--theme-text-secondary);">{{ growthInfo.title || '初出茅庐' }}</p>
            </div>
            <div class="text-right">
              <p class="text-sm" style="color: var(--theme-text-secondary);">成长值</p>
              <p class="text-2xl font-bold mt-1" style="color: var(--theme-primary);">{{ growthInfo.growthValue || 0 }}</p>
              <p class="text-xs mt-1" style="color: var(--theme-text-tertiary);">本季 {{ growthInfo.seasonValue || 0 }}</p>
            </div>
          </div>
          <div class="mt-4 h-2 rounded-full overflow-hidden" style="background-color: var(--theme-border);">
            <div
              class="h-full rounded-full transition-all duration-500"
              style="background-color: var(--theme-primary);"
              :style="{ width: `${growthInfo.levelProgress ?? 0}%` }"
            />
          </div>
        </div>
      </div>

      <!-- 模块筛选 Tab -->
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-6">
        <div class="flex gap-2 overflow-x-auto pb-1">
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
      </div>

      <!-- 时间线列表 -->
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div v-if="loading && timeline.length === 0" class="text-center py-16" style="color: var(--theme-text-secondary);">
          加载中...
        </div>
        <!-- 失败态（清单 P2）：必须排在空态之前 -->
        <div v-else-if="loadError" class="text-center py-16 rounded-2xl" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ loadError }}</p>
          <button
            class="px-5 py-2 rounded-xl text-sm font-medium"
            style="background-color: var(--theme-primary); color: white;"
            @click="load(true)"
          >重试</button>
        </div>

        <div v-else-if="timeline.length === 0">
          <Empty description="还没有成长记录，去阅读题目、刷题或读书吧，学习行为都会记录在这里" />
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
      </div>
    </main>

    <SiteFooter />
  </div>
</template>
