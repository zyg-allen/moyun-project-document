<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Briefcase, Search, Star, ArrowLeft, BookOpen,
  ChevronLeft, ChevronRight, MessageSquare, Eye
} from 'lucide-vue-next';
import LazyImage from '@/components/LazyImage.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { getSafeAvatar } from '@/utils/avatar';
import { getExperienceList } from '@/api/interview';
import { useToast } from '@/composables/useToast';
import type { InterviewExperienceVO } from '@/types/api';

const route = useRoute();
const router = useRouter();
const toast = useToast();

const loading = ref(false);
const error = ref<string | null>(null);
const experiences = ref<InterviewExperienceVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;

const keyword = ref('');
const searchInput = ref('');

useHead(computed(() => generateSeo({
  title: '面试经验',
  description: '精选真实面试经验分享，涵盖大厂面经、求职心得、面试技巧，助你备战面试直通 Offer',
  keywords: ['面试经验', '面经', '大厂面试', '求职', '面试技巧'],
  canonicalPath: '/interview/experiences',
})));

onMounted(() => {
  // 从路由 query 中恢复搜索关键词
  const q = route.query.keyword as string;
  if (q) {
    keyword.value = q;
    searchInput.value = q;
  }
  loadExperiences();
});

watch(page, () => {
  loadExperiences();
});

function doSearch() {
  keyword.value = searchInput.value.trim();
  page.value = 1;
  loadExperiences();
}

async function loadExperiences() {
  try {
    loading.value = true;
    error.value = null;
    const params: any = { pageNum: page.value, pageSize };
    if (keyword.value) params.keyword = keyword.value;
    const res = await getExperienceList(params);
    if (res.code === 200 && res.data) {
      const data: any = res.data;
      experiences.value = data.list || [];
      total.value = data.total || 0;
    } else {
      error.value = res.message || '加载面经失败';
      toast.error(res.message || '加载失败');
    }
  } catch (err: any) {
    error.value = err?.message || '加载面经失败，请稍后重试';
    toast.error(err?.message || '加载失败');
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push('/interview');
}

function goDetail(id: string | number) {
  router.push(`/interview/experience/${id}`);
}

function expName(exp: InterviewExperienceVO) {
  const u: any = (exp as any).user;
  if (u?.nickname) return u.nickname;
  if (exp.userNickname) return exp.userNickname;
  return '匿名用户';
}

function expAvatar(exp: InterviewExperienceVO) {
  const u: any = (exp as any).user;
  if (u?.avatar) return u.avatar;
  if (exp.userAvatar) return exp.userAvatar;
  return getSafeAvatar('', String(exp.id));
}

function formatNumber(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w';
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
  return String(n || 0);
}

function totalPages() {
  return Math.max(1, Math.ceil(total.value / pageSize));
}

function gotoPage(p: number) {
  if (p < 1 || p > totalPages()) return;
  page.value = p;
  window.scrollTo({ top: 0, behavior: 'smooth' });
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部返回栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3 flex items-center justify-between">
        <button
          @click="goBack"
          class="flex items-center text-sm transition hover:opacity-80"
          style="color: var(--theme-text-secondary);"
        >
          <ArrowLeft class="w-4 h-4 mr-1" />
          返回面试指南
        </button>
        <span class="text-sm" style="color: var(--theme-text-secondary);">面试经验</span>
        <span class="w-20"></span>
      </div>
    </div>

    <!-- Hero 区 -->
    <div class="py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 20% 30%, rgba(99, 102, 241, 0.4) 0%, transparent 50%), radial-gradient(circle at 80% 70%, rgba(168, 85, 247, 0.4) 0%, transparent 50%), linear-gradient(135deg, #4f46e5 0%, #7c3aed 100%);">
          <!-- 装饰图案：代码括号 + 目标靶 -->
          <div class="absolute inset-0 opacity-10 pointer-events-none" aria-hidden="true">
            <svg class="absolute top-6 left-8 w-32 h-32 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0L19.2 12l-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>
            <svg class="absolute bottom-4 right-10 w-40 h-40 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 4c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm0 2c2.21 0 4 1.79 4 4s-1.79 4-4 4-4-1.79-4-4 1.79-4 4-4zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/></svg>
          </div>
          <div class="relative px-6 py-8 sm:px-10 sm:py-10 text-center">
            <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-5">
              <Briefcase class="w-4 h-4 mr-2" /> 墨韵 · 面经分享
            </div>
            <h1 class="text-4xl md:text-5xl font-bold tracking-tight mb-4">面试经验</h1>
            <p class="text-base md:text-lg text-white/90 max-w-2xl mx-auto mb-8">
              汇集真实面试经验，从大厂面经到求职心得，助你少走弯路，直达 Offer
            </p>
            <!-- 搜索框 -->
            <div class="max-w-xl mx-auto rounded-xl p-2 flex items-center shadow-lg" style="background-color: var(--theme-bg);">
              <Search class="w-5 h-5 ml-2 flex-shrink-0" style="color: var(--theme-text-secondary);" />
              <input
                v-model="searchInput"
                @keyup.enter="doSearch"
                type="text"
                placeholder="搜索公司、职位、关键词..."
                class="flex-1 px-3 py-2 focus:outline-none text-sm"
                style="color: var(--theme-text);"
              />
              <button
                @click="doSearch"
                class="px-5 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90"
                style="background-color: var(--theme-primary);"
              >
                搜索
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20">
          <div
            class="animate-spin rounded-full h-12 w-12 border-b-2"
            style="border-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="error"
          class="rounded-xl border p-8 max-w-md mx-auto text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ error }}</p>
          <button
            @click="loadExperiences"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 空数据状态 -->
        <div
          v-else-if="experiences.length === 0"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <BookOpen class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.5;" />
          <p class="text-sm" style="color: var(--theme-text-secondary);">暂无面经</p>
        </div>

        <!-- 面经卡片列表 -->
        <template v-else>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5 mb-8">
            <div
              v-for="exp in experiences"
              :key="exp.id"
              @click="goDetail(exp.id)"
              class="rounded-xl overflow-hidden border shadow-sm hover:shadow-lg hover:-translate-y-1 transition cursor-pointer flex flex-col"
              style="background-color: var(--theme-surface); border-color: var(--theme-border);"
            >
              <!-- 封面图 -->
              <div v-if="exp.coverImage" class="h-40" style="background-color: var(--theme-bg);">
                <LazyImage
                  :src="exp.coverImage"
                  :alt="exp.title"
                  class="w-full h-full object-cover"
                />
              </div>
              <!-- 置顶标记 -->
              <div v-if="exp.isTop" class="relative">
                <span
                  class="absolute top-3 right-3 px-2 py-1 text-xs font-medium rounded-full text-white shadow-sm"
                  style="background-color: var(--theme-primary);"
                >
                  <Star class="w-3 h-3 inline mr-1" />置顶
                </span>
              </div>

              <div class="p-5 flex flex-col flex-1">
                <!-- 标签行：公司 / 职位 / 年份 -->
                <div class="flex items-center gap-2 mb-3 flex-wrap">
                  <span
                    v-if="exp.company"
                    class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                    style="background-color: var(--theme-bg); color: var(--theme-primary);"
                  >
                    <Briefcase class="w-3 h-3 mr-1" />
                    {{ exp.company }}
                  </span>
                  <span
                    v-if="exp.position"
                    class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                    style="background-color: var(--theme-bg); color: var(--theme-text-secondary);"
                  >
                    {{ exp.position }}
                  </span>
                  <span
                    v-if="exp.year"
                    class="text-xs"
                    style="color: var(--theme-text-secondary);"
                  >
                    {{ exp.year }}年{{ exp.month ? exp.month + '月' : '' }}
                  </span>
                </div>

                <!-- 标题 -->
                <h3
                  class="text-lg font-semibold mb-2 line-clamp-2"
                  style="color: var(--theme-text);"
                >
                  {{ exp.title }}
                </h3>

                <!-- 摘要 -->
                <p
                  v-if="exp.summary || exp.content"
                  class="text-sm mb-4 line-clamp-3 flex-1"
                  style="color: var(--theme-text-secondary);"
                >
                  {{ exp.summary || exp.content }}
                </p>

                <!-- 作者信息 + 统计 -->
                <div class="flex items-center justify-between pt-3 border-t" style="border-color: var(--theme-border);">
                  <div class="flex items-center min-w-0">
                    <div class="w-7 h-7 rounded-full overflow-hidden mr-2 flex-shrink-0" style="background-color: var(--theme-bg);">
                      <LazyImage
                        :src="expAvatar(exp)"
                        :alt="expName(exp)"
                        class="w-full h-full object-cover"
                      />
                    </div>
                    <span class="text-xs font-medium truncate" style="color: var(--theme-text);">{{ expName(exp) }}</span>
                  </div>
                  <div class="flex items-center gap-3 text-xs flex-shrink-0" style="color: var(--theme-text-secondary);">
                    <span class="flex items-center">
                      <Star class="w-3 h-3 mr-1" style="color: var(--theme-primary);" />{{ formatNumber(exp.likeCount) }}
                    </span>
                    <span class="flex items-center">
                      <Eye class="w-3 h-3 mr-1" />{{ formatNumber(exp.viewCount) }}
                    </span>
                    <span class="flex items-center">
                      <MessageSquare class="w-3 h-3 mr-1" />{{ formatNumber(exp.commentCount) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div
            v-if="totalPages() > 1"
            class="flex flex-wrap items-center justify-center gap-2 mt-8"
          >
            <button
              @click="gotoPage(page - 1)"
              :disabled="page === 1"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <ChevronLeft class="w-4 h-4" />
              上一页
            </button>
            <span class="px-4 py-2 text-sm" style="color: var(--theme-text-secondary);">
              第 {{ page }} / {{ totalPages() }} 页
            </span>
            <button
              @click="gotoPage(page + 1)"
              :disabled="page === totalPages()"
              class="px-3 py-2 rounded-lg text-sm transition disabled:opacity-40 disabled:cursor-not-allowed flex items-center"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              下一页
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-2 text-xs" style="color: var(--theme-text-secondary);">共 {{ total }} 篇</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
