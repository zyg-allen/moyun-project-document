<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ArrowLeft, Save, Loader2, Upload, Image as ImageIcon, BookOpen, X,
} from 'lucide-vue-next';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import { generateSeo } from '@/utils/seo';
import { uploadImage } from '@/api/upload';
import { getColumnDetail, saveColumn } from '@/api/column';
import type { ColumnSaveBody } from '@/types/api';

const route = useRoute();
const router = useRouter();

const editId = computed(() => route.params.id as string | undefined);
const isEdit = computed(() => !!editId.value);

// 表单字段
const title = ref('');
const subtitle = ref('');
const description = ref('');
const cover = ref('');
const categoryId = ref('');
const isFinished = ref(false);
const price = ref<number | ''>('');

const submitting = ref(false);
const loadingDetail = ref(false);
const uploading = ref(false);
const pageError = ref<string | null>(null);

// Toast
const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
let toastTimer: number | null = null;
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value = null; }, 3000);
}

const fileInput = ref<HTMLInputElement | null>(null);

useHead(computed(() => generateSeo({
  title: isEdit.value ? '编辑专栏' : '创建专栏',
  description: '创建或编辑你的专栏，按主题聚合文章，持续连载',
  keywords: ['创建专栏', '编辑专栏', '专栏', '墨韵'],
  canonicalPath: isEdit.value ? `/column/edit/${editId.value}` : '/column/create',
  robots: 'noindex,nofollow',
})));

const pageTitle = computed(() => isEdit.value ? '编辑专栏' : '创建专栏');

onMounted(async () => {
  if (isEdit.value && editId.value) {
    await loadDetail();
  }
});

// 路由参数变化时（同一组件复用，如从 /column/A/edit 跳转 /column/B/edit）重新加载
watch(editId, (newId, oldId) => {
  if (newId !== oldId) {
    // 重置表单状态，避免显示上一个专栏的数据
    title.value = '';
    subtitle.value = '';
    description.value = '';
    cover.value = '';
    categoryId.value = '';
    isFinished.value = false;
    price.value = '';
    pageError.value = null;
    if (newId) {
      loadDetail();
    }
  }
});

async function loadDetail() {
  if (!editId.value) return;
  loadingDetail.value = true;
  try {
    const res = await getColumnDetail(editId.value);
    if (res.code === 200 && res.data) {
      const c = res.data;
      title.value = c.title || '';
      subtitle.value = c.subtitle || '';
      description.value = c.description || '';
      cover.value = c.cover || '';
      categoryId.value = c.categoryId != null ? String(c.categoryId) : '';
      isFinished.value = !!c.isFinished;
      price.value = c.price != null ? c.price : '';
    } else {
      pageError.value = res.message || '加载专栏失败';
      showToast(res.message || '加载专栏失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    pageError.value = e?.message || '加载专栏失败，请稍后重试';
    showToast(pageError.value, 'error');
  } finally {
    loadingDetail.value = false;
  }
}

function triggerUpload() {
  fileInput.value?.click();
}

async function handleUpload(e: Event) {
  const target = e.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  if (uploading.value) return;
  uploading.value = true;
  try {
    const res = await uploadImage(file);
    if (res.code === 200 && res.data) {
      cover.value = res.data.fileUrl;
      showToast('封面上传成功', 'success');
    } else {
      showToast(res.message || '上传失败', 'error');
    }
  } catch (err) {
    const er = err as { message?: string };
    showToast(er?.message || '上传失败，请稍后重试', 'error');
  } finally {
    uploading.value = false;
    // 清空 input 以便重复选择同一文件
    if (fileInput.value) fileInput.value.value = '';
  }
}

function clearCover() {
  cover.value = '';
}

function validate(): string | null {
  if (!title.value.trim()) return '请填写专栏标题';
  if (title.value.length > 100) return '标题不能超过 100 字';
  if (subtitle.value.length > 100) return '副标题不能超过 100 字';
  if (description.value.length > 1000) return '简介不能超过 1000 字';
  if (price.value !== '' && (isNaN(Number(price.value)) || Number(price.value) < 0)) {
    return '价格必须为非负数字';
  }
  return null;
}

function buildPayload(): ColumnSaveBody {
  const payload: ColumnSaveBody = {
    title: title.value.trim(),
    subtitle: subtitle.value.trim() || undefined,
    description: description.value.trim() || undefined,
    cover: cover.value.trim() || undefined,
    isFinished: isFinished.value,
  };
  if (categoryId.value.trim()) payload.categoryId = categoryId.value.trim();
  if (price.value !== '') payload.price = Number(price.value);
  if (isEdit.value && editId.value) payload.id = editId.value;
  return payload;
}

async function submit() {
  const errMsg = validate();
  if (errMsg) {
    showToast(errMsg, 'error');
    return;
  }
  submitting.value = true;
  try {
    const res = await saveColumn(buildPayload());
    if (res.code === 200) {
      showToast(isEdit.value ? '专栏已更新' : '专栏创建成功', 'success');
      const newId = res.data;
      if (newId !== undefined && newId !== null && newId !== '') {
        router.push(`/column/${newId}`);
      } else if (isEdit.value && editId.value) {
        router.push(`/column/${editId.value}`);
      } else {
        router.push('/columns');
      }
    } else {
      showToast(res.message || '保存失败', 'error');
    }
  } catch (err) {
    const e = err as { message?: string };
    showToast(e?.message || '保存失败，请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/columns');
  }
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
          返回
        </button>
        <span class="text-sm font-medium" style="color: var(--theme-text);">{{ pageTitle }}</span>
        <span class="w-20"></span>
      </div>
    </div>

    <!-- Toast -->
    <div
      v-if="toast"
      class="fixed top-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-lg shadow-lg text-sm"
      :class="toast.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'"
    >
      {{ toast.message }}
    </div>

    <!-- Hero 区 -->
    <div class="py-6 sm:py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="relative overflow-hidden rounded-2xl text-white" style="background-image: radial-gradient(circle at 20% 50%, rgba(190, 24, 93, 0.3) 0%, transparent 50%), radial-gradient(circle at 80% 30%, rgba(124, 58, 237, 0.3) 0%, transparent 50%), linear-gradient(135deg, #be185d 0%, #a21caf 50%, #7c3aed 100%);">
          <div class="absolute inset-0 opacity-10 pointer-events-none" aria-hidden="true">
            <svg class="absolute top-6 left-8 w-32 h-32 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/></svg>
            <svg class="absolute bottom-4 right-10 w-40 h-40 text-white" viewBox="0 0 24 24" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10H7v-2h10v2zm0-4H7V7h10v2z"/></svg>
          </div>
          <div class="relative px-6 py-8 sm:px-10 sm:py-10 text-center">
            <div class="inline-flex items-center bg-white/10 backdrop-blur-sm px-4 py-1.5 rounded-full text-sm mb-4">
              <BookOpen class="w-4 h-4 mr-2" /> 墨韵 · 专栏
            </div>
            <h1 class="text-3xl font-bold mb-2">{{ pageTitle }}</h1>
            <p class="text-sm opacity-90">按主题聚合文章，持续连载，构建知识体系</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载状态 -->
        <div v-if="loadingDetail" class="flex flex-col items-center justify-center py-20">
          <Loader2 class="w-10 h-10 animate-spin" style="color: var(--theme-primary);" />
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 错误状态 -->
        <div
          v-else-if="pageError"
          class="rounded-xl border p-8 max-w-md mx-auto text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ pageError }}</p>
          <button
            @click="loadDetail"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 表单 -->
        <form
          v-else
          @submit.prevent="submit"
          class="rounded-xl border shadow-sm p-6 space-y-5"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <!-- 标题 -->
          <div>
            <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">
              专栏标题 <span style="color: var(--theme-danger);">*</span>
            </label>
            <input
              v-model="title"
              type="text"
              maxlength="100"
              placeholder="给你的专栏起个名字"
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            />
          </div>

          <!-- 副标题 -->
          <div>
            <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">副标题</label>
            <input
              v-model="subtitle"
              type="text"
              maxlength="100"
              placeholder="一句话概括专栏定位（选填）"
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            />
          </div>

          <!-- 简介 -->
          <div>
            <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">简介</label>
            <textarea
              v-model="description"
              rows="4"
              maxlength="1000"
              placeholder="介绍专栏内容、面向读者、更新计划等"
              class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none resize-y"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            ></textarea>
          </div>

          <!-- 封面上传 -->
          <div>
            <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">封面图</label>
            <div class="flex items-start gap-4">
              <div class="w-28 h-36 rounded-lg overflow-hidden flex-shrink-0" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                <LazyImage
                  v-if="cover"
                  :src="cover"
                  alt="专栏封面"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center" style="color: var(--theme-text-secondary);">
                  <ImageIcon class="w-6 h-6 opacity-50" />
                </div>
              </div>
              <div class="flex flex-col gap-2">
                <button
                  type="button"
                  @click="triggerUpload"
                  :disabled="uploading"
                  class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm transition hover:opacity-90 disabled:opacity-50"
                  style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
                >
                  <Loader2 v-if="uploading" class="w-4 h-4 mr-1 animate-spin" />
                  <Upload v-else class="w-4 h-4 mr-1" />
                  {{ uploading ? '上传中...' : '上传封面' }}
                </button>
                <button
                  v-if="cover"
                  type="button"
                  @click="clearCover"
                  class="inline-flex items-center px-3 py-1.5 rounded-lg text-sm transition hover:opacity-90"
                  style="color: var(--theme-danger);"
                >
                  <X class="w-4 h-4 mr-1" />
                  清除
                </button>
                <input
                  ref="fileInput"
                  type="file"
                  accept="image/*"
                  class="hidden"
                  @change="handleUpload"
                />
              </div>
            </div>
          </div>

          <!-- 分类 + 价格 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">分类ID（选填）</label>
              <input
                v-model="categoryId"
                type="text"
                placeholder="可选，填写分类ID"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1.5" style="color: var(--theme-text);">价格</label>
              <input
                v-model="price"
                type="number"
                min="0"
                step="0.01"
                placeholder="0 表示免费"
                class="w-full px-3 py-2 rounded-lg text-sm focus:outline-none"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              />
            </div>
          </div>

          <!-- 完结状态 -->
          <div class="flex items-center gap-2">
            <input
              v-model="isFinished"
              type="checkbox"
              id="isFinished"
              class="w-4 h-4"
            />
            <label for="isFinished" class="text-sm" style="color: var(--theme-text);">
              标记为已完结
            </label>
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center justify-end gap-2 pt-2 border-t" style="border-color: var(--theme-border);">
            <button
              type="button"
              @click="goBack"
              class="px-4 py-2 rounded-lg text-sm transition hover:opacity-80"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              取消
            </button>
            <button
              type="submit"
              :disabled="submitting"
              class="inline-flex items-center px-5 py-2 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50"
              style="background-color: var(--theme-primary);"
            >
              <Loader2 v-if="submitting" class="w-4 h-4 mr-1 animate-spin" />
              <Save v-else class="w-4 h-4 mr-1" />
              {{ submitting ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
