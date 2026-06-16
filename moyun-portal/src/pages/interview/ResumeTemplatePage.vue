<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ChevronLeft, Search, Download, ThumbsUp, FileText, Sparkles,
  ChevronRight, Clock, Star, Tag
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import {
  getResumeTemplateList, downloadResumeTemplate, toggleResumeTemplateLike,
} from '@/api/interview';
import type { InterviewResumeTemplateVO } from '@/types/api';

const router = useRouter();
const loading = ref(false);
const templates = ref<InterviewResumeTemplateVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;
const keyword = ref('');
const searchInput = ref('');
const activeCategory = ref('all');

const categories = [
  { key: 'all', label: '全部' },
  { key: '技术岗', label: '技术岗' },
  { key: '产品岗', label: '产品岗' },
  { key: '应届生', label: '应届生' },
  { key: '社招', label: '社招' },
  { key: '实习', label: '实习' },
  { key: '简历', label: '简历' },
];

const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
let toastTimer: number | null = null;
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value = null; }, 3000);
}

useHead(computed(() => generateSeo({
  title: '简历模板库',
  description: '精选优质简历模板，助力求职成功',
})));

onMounted(() => {
  loadTemplates();
});

watch([page, activeCategory], () => {
  loadTemplates();
});

function doSearch() {
  keyword.value = searchInput.value.trim();
  page.value = 1;
  loadTemplates();
}

async function loadTemplates() {
  try {
    loading.value = true;
    const params: any = { page: page.value, pageSize };
    if (activeCategory.value !== 'all') params.category = activeCategory.value;
    if (keyword.value) params.keyword = keyword.value;
    const res = await getResumeTemplateList(params);
    if (res.code === 200 && res.data) {
      const data: any = res.data;
      templates.value = data.list || [];
      total.value = data.total || 0;
    } else {
      showToast(res.message || '加载失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '加载简历模板失败，请稍后重试', 'error');
  } finally {
    loading.value = false;
  }
}

async function handleDownload(t: InterviewResumeTemplateVO) {
  try {
    const res = await downloadResumeTemplate(t.id);
    if (res.code === 200 && res.data?.downloadUrl) {
      window.open(res.data.downloadUrl, '_blank');
      t.downloadCount = (t.downloadCount || 0) + 1;
      showToast('下载链接已打开', 'success');
    } else {
      showToast(res.message || '获取下载链接失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '下载失败', 'error');
  }
}

async function handleLike(t: InterviewResumeTemplateVO) {
  try {
    const res = await toggleResumeTemplateLike(t.id);
    if (res.code === 200 && res.data) {
      t.liked = res.data.liked;
      t.likeCount = res.data.likeCount;
      showToast(res.data.liked ? '点赞成功' : '已取消点赞', 'success');
    } else {
      showToast(res.message || '操作失败', 'error');
    }
  } catch (err: any) {
    showToast(err?.message || '操作失败', 'error');
  }
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
  <div class="min-h-screen flex flex-col bg-gray-50">
    <div class="bg-white border-b border-gray-200 sticky top-0 z-30">
      <div class="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
        <button @click="router.back()" class="flex items-center text-gray-600 hover:text-blue-600 transition text-sm">
          <ChevronLeft class="w-4 h-4 mr-1" /> 返回
        </button>
        <span class="text-sm text-gray-500">简历模板</span>
        <span class="w-12"></span>
      </div>
    </div>

    <div
      v-if="toast"
      class="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-lg shadow-lg text-sm"
      :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- 顶部 Hero -->
    <div class="bg-gradient-to-r from-indigo-600 to-blue-600 text-white py-10">
      <div class="max-w-6xl mx-auto px-4">
        <div class="flex items-center mb-3">
          <Sparkles class="w-6 h-6 mr-2" />
          <h1 class="text-3xl font-bold">精选简历模板库</h1>
        </div>
        <p class="text-blue-100 mb-6">选择合适的模板，快速打造高质量简历</p>
        <div class="relative max-w-xl bg-white rounded-lg p-2 flex">
          <Search class="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" />
          <input
            v-model="searchInput"
            @keyup.enter="doSearch"
            type="text"
            placeholder="搜索简历模板..."
            class="flex-1 pl-10 pr-3 py-2 text-gray-800 focus:outline-none text-sm"
          />
          <button @click="doSearch" class="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-md text-sm transition">搜索</button>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-6xl mx-auto px-4">
        <!-- 分类 Tab -->
        <div class="bg-white rounded-xl shadow-sm p-4 mb-6">
          <div class="flex items-center gap-2 overflow-x-auto">
            <button
              v-for="cat in categories"
              :key="cat.key"
              @click="activeCategory = cat.key; page = 1"
              class="flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition whitespace-nowrap"
              :class="activeCategory === cat.key ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'"
            >
              <Tag v-if="cat.key === 'all'" class="w-3 h-3 inline mr-1" />
              {{ cat.label }}
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p class="mt-4 text-gray-600">加载中...</p>
        </div>

        <!-- 模板网格 -->
        <template v-else>
          <div v-if="templates.length === 0" class="text-center py-16 bg-white rounded-xl shadow-sm">
            <FileText class="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p class="text-gray-500">暂无匹配的简历模板</p>
          </div>
          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            <div
              v-for="t in templates"
              :key="t.id"
              class="bg-white rounded-xl shadow-sm hover:shadow-lg transition overflow-hidden flex flex-col"
            >
              <div class="h-48 bg-gray-100 relative">
                <LazyImage
                  v-if="t.cover"
                  :src="t.cover"
                  :alt="t.title"
                  class="w-full h-full object-cover"
                />
                <div v-else class="flex items-center justify-center h-full bg-gradient-to-br from-blue-100 to-indigo-100">
                  <FileText class="w-12 h-12 text-blue-400" />
                </div>
                <span
                  v-if="t.category"
                  class="absolute top-3 left-3 px-2 py-1 bg-white/90 text-blue-600 rounded-full text-xs font-medium shadow-sm"
                >
                  {{ t.category }}
                </span>
                <span
                  v-if="t.isPremium"
                  class="absolute top-3 right-3 px-2 py-1 bg-yellow-400 text-yellow-900 rounded-full text-xs font-medium"
                >
                  <Star class="w-3 h-3 inline mr-1" /> 精选
                </span>
              </div>
              <div class="p-5 flex flex-col flex-1">
                <h3 class="text-lg font-semibold text-gray-900 mb-2 line-clamp-1">{{ t.title }}</h3>
                <p class="text-sm text-gray-600 mb-3 line-clamp-2 flex-1">
                  {{ t.description || '优质简历模板，助力你的求职之路' }}
                </p>
                <div class="flex items-center text-sm text-gray-500 mb-4">
                  <span class="flex items-center mr-3"><ThumbsUp class="w-4 h-4 mr-1" />{{ t.likeCount }}</span>
                  <span class="flex items-center"><Download class="w-4 h-4 mr-1" />{{ t.downloadCount }}</span>
                  <span v-if="t.fileType" class="ml-auto text-xs text-gray-400 uppercase">{{ t.fileType }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <button
                    @click="handleLike(t)"
                    class="flex-1 py-2 text-sm rounded-lg border transition flex items-center justify-center"
                    :class="t.liked ? 'bg-blue-50 text-blue-600 border-blue-200' : 'border-gray-200 text-gray-700 hover:border-blue-300'"
                  >
                    <ThumbsUp class="w-4 h-4 mr-1" />
                    {{ t.liked ? '已赞' : '点赞' }}
                  </button>
                  <button
                    @click="handleDownload(t)"
                    class="flex-1 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm rounded-lg transition flex items-center justify-center"
                  >
                    <Download class="w-4 h-4 mr-1" />
                    下载
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div v-if="totalPages() > 1" class="flex items-center justify-center gap-1 mt-8">
            <button
              @click="gotoPage(page - 1)"
              :disabled="page === 1"
              class="px-3 py-2 rounded-lg text-sm bg-white border border-gray-200 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronLeft class="w-4 h-4" />
            </button>
            <button
              v-for="p in totalPages()"
              :key="p"
              @click="gotoPage(p)"
              class="min-w-[40px] px-3 py-2 rounded-lg text-sm transition"
              :class="page === p ? 'bg-blue-600 text-white' : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-50'"
            >
              {{ p }}
            </button>
            <button
              @click="gotoPage(page + 1)"
              :disabled="page === totalPages()"
              class="px-3 py-2 rounded-lg text-sm bg-white border border-gray-200 disabled:opacity-50 hover:bg-gray-50"
            >
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-4 text-sm text-gray-500">共 {{ total }} 个模板</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
