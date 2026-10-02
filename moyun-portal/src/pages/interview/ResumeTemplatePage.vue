<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ChevronLeft, Search, Download, ThumbsUp, FileText,
  ChevronRight, Star, Tag, X, ChevronLeft as ChevronLeftIcon, Sparkles
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { getResumeTemplateList, getResumeTemplateDetail, downloadResumeTemplate } from '@/api/interview';
import type { InterviewResumeTemplateVO } from '@/types/api';
import { useToast } from '@/composables/useToast';
import { useResumeStore } from '@/stores/resume';

const router = useRouter();
import { useDictData } from '@/composables/useDictData';

const toast = useToast();
const resumeStore = useResumeStore();
const loading = ref(false);
const templates = ref<InterviewResumeTemplateVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 12;
const keyword = ref('');
const searchInput = ref('');
const activeCategory = ref('all');

// 图片预览状态（点击卡片图片时全屏预览多图）
const previewVisible = ref(false);
const previewImages = ref<string[]>([]);
const previewIndex = ref(0);

// 解析模板预览图 JSON 数组
function parsePreviewImages(json?: string | null): string[] {
  if (!json) return [];
  try {
    const arr = typeof json === 'string' ? JSON.parse(json) : json;
    return Array.isArray(arr) ? arr.filter(Boolean) : [];
  } catch { return []; }
}

// 获取模板的主图（预览图第一张 > 封面）
function getMainImage(t: InterviewResumeTemplateVO): string {
  const imgs = parsePreviewImages((t as any).previewImages);
  return imgs[0] || t.cover || '';
}

// 获取模板所有图片（预览图 + 封面兜底）
function getAllImages(t: InterviewResumeTemplateVO): string[] {
  const imgs = parsePreviewImages((t as any).previewImages);
  return imgs.length ? imgs : (t.cover ? [t.cover] : []);
}

// 打开图片预览
function openPreview(t: InterviewResumeTemplateVO) {
  const imgs = getAllImages(t);
  if (!imgs.length) return;
  previewImages.value = imgs;
  previewIndex.value = 0;
  resetPreviewScale();
  previewVisible.value = true;
}

function closePreview() { previewVisible.value = false; }

// 预览缩放：放大后可上下/左右滚动，1:1 可复位；顺序严格按 previewImages（= 后台预览图顺序）
const previewScale = ref(1);
const PREVIEW_SCALE_MIN = 0.5;
const PREVIEW_SCALE_MAX = 4;
function resetPreviewScale() { previewScale.value = 1; }
function zoomPreview(delta: number) {
  const next = Math.min(PREVIEW_SCALE_MAX, Math.max(PREVIEW_SCALE_MIN, previewScale.value + delta));
  previewScale.value = Number(next.toFixed(2));
}
function onPreviewWheel(e: WheelEvent) {
  zoomPreview(e.deltaY < 0 ? 0.15 : -0.15);
}
function previewPrev() {
  previewIndex.value = (previewIndex.value - 1 + previewImages.value.length) % previewImages.value.length;
  resetPreviewScale();
}
function previewNext() {
  previewIndex.value = (previewIndex.value + 1) % previewImages.value.length;
  resetPreviewScale();
}

// 放大后可拖拽平移（滚轮已被"缩放"占用，平移用拖动，符合看图习惯）
const previewDragging = ref(false);
let panStart = { x: 0, y: 0, left: 0, top: 0 };
function onPanStart(e: MouseEvent) {
  if (previewScale.value <= 1) return;
  const el = e.currentTarget as HTMLElement;
  previewDragging.value = true;
  panStart = { x: e.clientX, y: e.clientY, left: el.scrollLeft, top: el.scrollTop };
}
function onPanMove(e: MouseEvent) {
  if (!previewDragging.value) return;
  const el = e.currentTarget as HTMLElement;
  el.scrollLeft = panStart.left - (e.clientX - panStart.x);
  el.scrollTop = panStart.top - (e.clientY - panStart.y);
}
function onPanEnd() { previewDragging.value = false; }

/** 键盘：Esc 关闭 / ←→ 翻页 / +/- 缩放（无鼠标也能翻页） */
function handlePreviewKey(e: KeyboardEvent) {
  if (!previewVisible.value) return;
  if (e.key === 'Escape') closePreview();
  else if (e.key === 'ArrowLeft') previewPrev();
  else if (e.key === 'ArrowRight') previewNext();
  else if (e.key === '+' || e.key === '=') zoomPreview(0.15);
  else if (e.key === '-' || e.key === '_') zoomPreview(-0.15);
}
onMounted(() => window.addEventListener('keydown', handlePreviewKey));
onUnmounted(() => window.removeEventListener('keydown', handlePreviewKey));

// 分类 Tab（字典 portal_resume_category 驱动，本地默认兜底；"全部"始终在最前）
const dictMap = useDictData(['portal_resume_category']);

const DEFAULT_CATEGORIES = [
  { key: 'all', label: '全部' },
  { key: '技术岗', label: '技术岗' },
  { key: '产品岗', label: '产品岗' },
  { key: '应届生', label: '应届生' },
  { key: '社招', label: '社招' },
  { key: '实习', label: '实习' },
  { key: '简历', label: '简历' },
];

const categories = computed(() => {
  const items = dictMap['portal_resume_category'];
  if (items && items.length > 0) {
    return [
      { key: 'all', label: '全部' },
      ...items.map(i => ({ key: i.dictValue, label: i.dictLabel })),
    ];
  }
  return DEFAULT_CATEGORIES;
});

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
    // PageDomain 只接受 pageNum；用 page 会被 Spring 忽略、页码恒为 1
    const params: any = { pageNum: page.value, pageSize };
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
  // 付费检查预留位（当前全免费，后续接入钱包系统后启用）
  // if (t.isPremium) {
  //   toast.info('付费内容，敬请期待');
  //   return;
  // }
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

// 基于模板创建在线简历：拉取模板详情（含 sampleData 结构化示例数据）→ 暂存到 resumeStore
// → 跳转编辑页带 source=template 标识 → 编辑页 onMounted 消费 store 填充 form 后清空
// sampleData 为空时回退到 query 参数预填标题/期望岗位（模板为纯文件资源场景）
async function useTemplate(t: InterviewResumeTemplateVO) {
  const templateId = t.id;
  const templateTitle = t.title || '';
  const templateCategory = (t as any).category || '';
  try {
    // 拉详情拿 sampleData（列表接口可能未返回该字段，详情接口直传）
    const res = await getResumeTemplateDetail(templateId);
    const detail = res.code === 200 ? res.data : null;
    if (detail) {
      resumeStore.fillFromTemplate(detail);
    }
  } catch (e) {
    // 详情拉取失败不阻断流程，编辑页会回退到 query 预填
    console.warn('[useTemplate] 模板详情拉取失败，回退到 query 预填', e);
  }
  router.push({
    path: '/interview/resume/edit',
    query: {
      source: 'template',
      templateId: String(templateId || ''),
      templateTitle,
      templateCategory,
    },
  });
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
        <router-link
          to="/interview/resume/edit"
          class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-white transition hover:opacity-90 flex-shrink-0"
          style="background-color: var(--theme-primary);"
          title="维护我的简历（教育、工作、项目、技能等）"
        >
          <FileText class="w-3.5 h-3.5" />
          维护我的简历
        </router-link>
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
              class="rounded-xl shadow-sm hover:shadow-lg transition overflow-hidden flex flex-col group"
              style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);"
            >
              <!-- 大图区（优先预览图第一张 > 封面） -->
              <div
                class="h-44 relative cursor-pointer overflow-hidden"
                style="background-color: var(--theme-bg);"
                @click="openPreview(t)"
              >
                <LazyImage
                  v-if="getMainImage(t)"
                  :src="getMainImage(t)"
                  :alt="t.title"
                  class="w-full h-full object-contain transition-transform duration-300 group-hover:scale-105"
                />
                <div v-else class="flex items-center justify-center h-full" style="background-color: var(--theme-accent);">
                  <FileText class="w-14 h-14 text-theme-text-secondary" />
                </div>
                <!-- 分类角标 -->
                <span
                  v-if="t.category"
                  class="absolute top-2.5 left-2.5 px-2 py-0.5 rounded-full text-xs font-medium shadow-sm"
                  style="background-color: rgba(255,255,255,0.92); color: var(--theme-primary);"
                >
                  {{ t.category }}
                </span>
                <!-- 精选角标（付费预留，当前仅标记） -->
                <span
                  v-if="t.isPremium"
                  class="absolute top-2.5 left-2.5 px-2 py-0.5 bg-theme-warning-bg text-theme-warning rounded-full text-xs font-medium flex items-center"
                  :style="t.category ? 'left: auto; right: 2.5rem;' : 'left: auto; right: 0.625rem;'"
                >
                  <Star class="w-3 h-3 inline mr-0.5" />精选
                </span>
                <!-- 多图预览角标 -->
                <span
                  v-if="getAllImages(t).length > 1"
                  class="absolute bottom-2.5 left-2.5 px-2 py-0.5 rounded-full text-xs font-medium flex items-center"
                  style="background-color: rgba(0,0,0,0.6); color: white;"
                >
                  <FileText class="w-3 h-3 inline mr-1" />{{ getAllImages(t).length }} 张
                </span>
                <!-- 操作按钮组（hover 显示，小按钮叠在图片右上角） -->
                <div class="absolute top-2.5 right-2.5 flex gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                  <button
                    @click.stop="handleDownload(t)"
                    title="免费下载"
                    class="px-2.5 py-1 text-xs text-white rounded-md font-medium flex items-center gap-1 shadow-sm hover:opacity-90 transition"
                    style="background-color: var(--theme-primary);"
                  >
                    <Download class="w-3 h-3" />
                    下载
                  </button>
                  <button
                    @click.stop="useTemplate(t)"
                    title="基于此模板创建简历"
                    class="px-2.5 py-1 text-xs rounded-md font-medium flex items-center gap-1 shadow-sm backdrop-blur-sm transition hover:opacity-90"
                    style="background-color: rgba(255,255,255,0.92); color: var(--theme-primary); border: 1px solid var(--theme-primary);"
                  >
                    <Sparkles class="w-3 h-3" />
                    用此模板
                  </button>
                </div>
                <!-- 悬停提示（底部，不与按钮冲突） -->
                <div class="absolute inset-x-0 bottom-0 h-12 flex items-end justify-center pb-2 opacity-0 group-hover:opacity-100 transition" style="background: linear-gradient(to top, rgba(0,0,0,0.4), transparent);">
                  <span class="text-white text-xs font-medium flex items-center gap-1">
                    <Search class="w-3.5 h-3.5" />点击查看预览
                  </span>
                </div>
              </div>
              <!-- 信息区（突出标题与点赞收藏） -->
              <div class="p-3.5 flex flex-col flex-1">
                <h3 class="text-base font-semibold mb-1 line-clamp-1" style="color: var(--theme-text);">{{ t.title }}</h3>
                <p class="text-xs mb-2.5 line-clamp-1 flex-1" style="color: var(--theme-text-secondary);">
                  {{ t.description || '优质简历模板，助力你的求职之路' }}
                </p>
                <div class="flex items-center text-xs" style="color: var(--theme-text-secondary);">
                  <span class="flex items-center mr-3"><ThumbsUp class="w-3.5 h-3.5 mr-1" />{{ t.likeCount }}</span>
                  <span class="flex items-center"><Download class="w-3.5 h-3.5 mr-1" />{{ t.downloadCount }}</span>
                  <button
                    class="ml-auto mr-2 px-2 py-0.5 rounded text-[11px] font-medium flex items-center gap-1 transition hover:opacity-90"
                    style="background-color: var(--theme-primary); color: #fff;"
                    :title="`查看预览（共 ${getAllImages(t).length} 张，顺序同后台预览图）`"
                    @click.stop="openPreview(t)"
                  >
                    <Search class="w-3 h-3" />查看
                  </button>
                  <span v-if="t.fileType" class="text-[10px] uppercase px-1.5 py-0.5 rounded font-medium" style="background-color: var(--theme-accent); color: var(--theme-text-secondary);">{{ t.fileType }}</span>
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
              :class="page === p ? 'bg-theme-primary text-white' : 'bg-[var(--theme-surface)] border border-[var(--theme-border)] text-[var(--theme-text-secondary)] hover:bg-[var(--theme-accent)]'"
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

    <!-- 图片预览弹窗（全屏多图轮播） -->
    <div
      v-if="previewVisible"
      class="fixed inset-0 z-50 flex flex-col"
      style="background-color: rgba(0,0,0,0.92);"
      @click.self="closePreview"
    >
      <!-- 顶部工具条：缩放 / 复位 / 看原图（原图在新窗口打开，不受本页样式影响） -->
      <div class="absolute top-4 left-4 z-10 flex items-center gap-2 text-white text-sm">
        <button class="px-2 py-1 rounded bg-white/10 hover:bg-white/20 transition" title="缩小" @click.stop="zoomPreview(-0.15)">−</button>
        <span class="tabular-nums w-12 text-center">{{ Math.round(previewScale * 100) }}%</span>
        <button class="px-2 py-1 rounded bg-white/10 hover:bg-white/20 transition" title="放大" @click.stop="zoomPreview(0.15)">＋</button>
        <button class="px-2 py-1 rounded bg-white/10 hover:bg-white/20 transition" title="重置为 1:1" @click.stop="resetPreviewScale">1:1</button>
        <a
          class="px-2 py-1 rounded bg-white/10 hover:bg-white/20 transition"
          :href="previewImages[previewIndex]"
          target="_blank"
          rel="noopener"
          title="在新窗口打开原图"
          @click.stop
        >原图</a>
      </div>
      <button class="absolute top-4 right-4 z-10 p-2 rounded-full hover:bg-white/10 transition" title="关闭（Esc）" @click="closePreview">
        <X class="w-6 h-6 text-white" />
      </button>
      <button v-if="previewImages.length > 1" class="absolute left-4 top-1/2 -translate-y-1/2 z-10 p-2 rounded-full bg-white/10 hover:bg-white/20 transition" title="上一张（←）" @click.stop="previewPrev">
        <ChevronLeftIcon class="w-8 h-8 text-white" />
      </button>
      <button v-if="previewImages.length > 1" class="absolute right-4 top-1/2 -translate-y-1/2 z-10 p-2 rounded-full bg-white/10 hover:bg-white/20 transition" title="下一张（→）" @click.stop="previewNext">
        <ChevronRight class="w-8 h-8 text-white" />
      </button>
      <!-- 滚动容器：放大后仍可上下/左右滚动，图片不会被截断 -->
      <div
        class="flex-1 w-full overflow-auto flex items-center justify-center p-4"
        :style="{ cursor: previewScale > 1 ? (previewDragging ? 'grabbing' : 'grab') : 'default' }"
        @wheel.prevent="onPreviewWheel"
        @mousedown="onPanStart"
        @mousemove="onPanMove"
        @mouseup="onPanEnd"
        @mouseleave="onPanEnd"
      >
        <img
          :src="previewImages[previewIndex]"
          class="max-w-full max-h-full object-contain rounded-lg shadow-2xl transition-transform duration-150"
          :style="{ transform: `scale(${previewScale})` }"
          @click.stop
        />
      </div>
      <div
        v-if="previewImages.length > 1"
        class="absolute bottom-4 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full text-sm text-white"
        style="background-color: rgba(0,0,0,0.6);"
      >
        第 {{ previewIndex + 1 }} / {{ previewImages.length }} 张（顺序同后台预览图）· ← → 翻页 · 滚轮缩放 · 放大后拖动查看
      </div>
      <div v-else class="absolute bottom-4 left-1/2 -translate-x-1/2 text-xs text-white/70">
        滚轮缩放 · Esc 关闭
      </div>
    </div>
  </div>
</template>
