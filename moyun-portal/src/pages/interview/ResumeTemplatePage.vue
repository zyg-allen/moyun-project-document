<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useHead } from '@vueuse/head';
import {
  ChevronLeft, Search, Download, ThumbsUp, FileText,
  ChevronRight, Star, Tag
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import {
  getResumeTemplateList, downloadResumeTemplate, toggleResumeTemplateLike,
} from '@/api/interview';
import type { InterviewResumeTemplateVO } from '@/types/api';
import { useToast } from '@/composables/useToast';

const toast = useToast();
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

useHead(computed(() => generateSeo({
  title: '简历模板库',
  description: '精选优质简历模板，助力求职成功',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '面试指南', path: '/interview' },
  { label: '简历模板' },
]);

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
      toast.error(res.message || '加载失败');
    }
  } catch (err: any) {
    toast.error(err?.message || '加载简历模板失败，请稍后重试');
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
      toast.success('下载链接已打开');
    } else {
      toast.error(res.message || '获取下载链接失败');
    }
  } catch (err: any) {
    toast.error(err?.message || '下载失败');
  }
}

async function handleLike(t: InterviewResumeTemplateVO) {
  try {
    const res = await toggleResumeTemplateLike(t.id);
    if (res.code === 200 && res.data) {
      t.liked = res.data.liked;
      t.likeCount = res.data.likeCount;
      toast.success(res.data.liked ? '点赞成功' : '已取消点赞');
    } else {
      toast.error(res.message || '操作失败');
    }
  } catch (err: any) {
    toast.error(err?.message || '操作失败');
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
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <span class="w-12"></span>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 搜索工具栏 -->
        <div class="mb-6 max-w-xl mx-auto">
          <div class="flex items-center rounded-xl border px-3 py-1" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
            <Search class="w-5 h-5 flex-shrink-0" style="color: var(--theme-text-secondary);" />
            <input
              v-model="searchInput"
              @keyup.enter="doSearch"
              type="text"
              placeholder="搜索简历模板..."
              class="flex-1 px-3 py-2 focus:outline-none text-sm"
              style="color: var(--theme-text);"
            />
            <button @click="doSearch" class="px-5 py-1.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90" style="background-color: var(--theme-primary);">搜索</button>
          </div>
        </div>

        <!-- 分类 Tab -->
        <div class="rounded-xl shadow-sm p-4 mb-6" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
          <div class="flex items-center gap-2 overflow-x-auto">
            <button
              v-for="cat in categories"
              :key="cat.key"
              @click="activeCategory = cat.key; page = 1"
              class="flex-shrink-0 px-4 py-2 rounded-full text-sm font-medium transition whitespace-nowrap"
              :class="activeCategory === cat.key ? 'bg-[var(--theme-primary)] text-white' : 'bg-[var(--theme-bg)] text-[var(--theme-text-secondary)] hover:bg-[var(--theme-accent)]'"
            >
              <Tag v-if="cat.key === 'all'" class="w-3 h-3 inline mr-1" />
              {{ cat.label }}
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="text-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 mx-auto" style="border-color: var(--theme-primary);"></div>
          <p class="mt-4" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 模板网格 -->
        <template v-else>
          <div v-if="templates.length === 0" class="text-center py-16 rounded-xl shadow-sm" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
            <FileText class="w-12 h-12 mx-auto mb-3" style="color: var(--theme-text-secondary); opacity: 0.4;" />
            <p style="color: var(--theme-text-secondary);">暂无匹配的简历模板</p>
          </div>
          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
            <div
              v-for="t in templates"
              :key="t.id"
              class="rounded-xl shadow-sm hover:shadow-lg transition overflow-hidden flex flex-col"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <div class="h-48 relative" style="background-color: var(--theme-bg);">
                <LazyImage
                  v-if="t.cover"
                  :src="t.cover"
                  :alt="t.title"
                  class="w-full h-full object-cover"
                />
                <div v-else class="flex items-center justify-center h-full" style="background-color: var(--theme-accent);">
                  <FileText class="w-12 h-12 text-blue-400" />
                </div>
                <span
                  v-if="t.category"
                  class="absolute top-3 left-3 px-2 py-1 rounded-full text-xs font-medium shadow-sm"
                  style="background-color: var(--theme-bg); color: var(--theme-primary);"
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
                <h3 class="text-lg font-semibold mb-2 line-clamp-1" style="color: var(--theme-text);">{{ t.title }}</h3>
                <p class="text-sm mb-3 line-clamp-2 flex-1" style="color: var(--theme-text-secondary);">
                  {{ t.description || '优质简历模板，助力你的求职之路' }}
                </p>
                <div class="flex items-center text-sm mb-4" style="color: var(--theme-text-secondary);">
                  <span class="flex items-center mr-3"><ThumbsUp class="w-4 h-4 mr-1" />{{ t.likeCount }}</span>
                  <span class="flex items-center"><Download class="w-4 h-4 mr-1" />{{ t.downloadCount }}</span>
                  <span v-if="t.fileType" class="ml-auto text-xs uppercase" style="color: var(--theme-text-secondary);">{{ t.fileType }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <button
                    @click="handleLike(t)"
                    class="flex-1 py-2 text-sm rounded-lg border transition flex items-center justify-center"
                    :class="t.liked ? 'bg-[var(--theme-accent)] text-[var(--theme-primary)] border-[var(--theme-border)]' : 'border-[var(--theme-border)] text-[var(--theme-text-secondary)] hover:border-[var(--theme-primary)]'"
                  >
                    <ThumbsUp class="w-4 h-4 mr-1" />
                    {{ t.liked ? '已赞' : '点赞' }}
                  </button>
                  <button
                    @click="handleDownload(t)"
                    class="flex-1 py-2 text-white text-sm rounded-lg transition flex items-center justify-center hover:opacity-90"
                    style="background-color: var(--theme-primary);"
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
              class="px-3 py-2 rounded-lg text-sm disabled:opacity-50 hover:bg-[var(--theme-accent)]"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <ChevronLeft class="w-4 h-4" />
            </button>
            <button
              v-for="p in totalPages()"
              :key="p"
              @click="gotoPage(p)"
              class="min-w-[40px] px-3 py-2 rounded-lg text-sm transition"
              :class="page === p ? 'bg-blue-600 text-white' : 'bg-[var(--theme-surface)] border border-[var(--theme-border)] text-[var(--theme-text-secondary)] hover:bg-[var(--theme-accent)]'"
            >
              {{ p }}
            </button>
            <button
              @click="gotoPage(page + 1)"
              :disabled="page === totalPages()"
              class="px-3 py-2 rounded-lg text-sm disabled:opacity-50 hover:bg-[var(--theme-accent)]"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <ChevronRight class="w-4 h-4" />
            </button>
            <span class="ml-4 text-sm" style="color: var(--theme-text-secondary);">共 {{ total }} 个模板</span>
          </div>
        </template>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
