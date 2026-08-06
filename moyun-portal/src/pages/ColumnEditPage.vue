<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  Save, Loader2, Upload, Image as ImageIcon, X, FileText,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import MyArticlePicker from '@/components/MyArticlePicker.vue';
import { generateSeo } from '@/utils/seo';
import { uploadImage } from '@/api/upload';
import { deletePortalFile } from '@/api/file';
import { getColumnDetail, saveColumn, addArticle, removeArticle } from '@/api/column';
import type { ColumnSaveBody } from '@/types/api';
import { useToast } from '@/composables/useToast';

const route = useRoute();
const router = useRouter();
const toast = useToast();

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

// v1.1.3 新增：专栏文章关联（已选 articleId 列表 + 加载时回填的原列表）
const selectedArticleIds = ref<Array<string | number>>([]);
const originalArticleIds = ref<Array<string | number>>([]);

const submitting = ref(false);
const loadingDetail = ref(false);
const uploading = ref(false);
const pageError = ref<string | null>(null);

const fileInput = ref<HTMLInputElement | null>(null);

useHead(computed(() => generateSeo({
  title: isEdit.value ? '编辑专栏' : '创建专栏',
  description: '创建或编辑你的专栏，按主题聚合文章，持续连载',
  keywords: ['创建专栏', '编辑专栏', '专栏', '墨韵'],
  canonicalPath: isEdit.value ? `/column/edit/${editId.value}` : '/column/create',
  robots: 'noindex,nofollow',
})));

const pageTitle = computed(() => isEdit.value ? '编辑专栏' : '创建专栏');

const breadcrumbs = computed(() => [
  { label: '专栏广场', path: '/columns' },
  { label: pageTitle.value },
]);

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
    selectedArticleIds.value = [];
    originalArticleIds.value = [];
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
      // v1.1.3 新增：回填专栏已关联的文章 ID 列表
      const ids = (c.articles || []).map(a => a.id);
      selectedArticleIds.value = ids;
      originalArticleIds.value = [...ids];
    } else {
      pageError.value = res.message || '加载专栏失败';
      toast.error(res.message || '加载专栏失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    pageError.value = e?.message || '加载专栏失败，请稍后重试';
    toast.error(pageError.value);
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
  // 替换场景（已有旧封面）：先记录旧封面，新上传成功后再删旧，失败则恢复旧封面，保证不丢失。
  // 与「清除」按钮区分：清除直接删；替换先传新再删旧。
  const oldCover = cover.value || null;
  uploading.value = true;
  try {
    const res = await uploadImage(file, { businessType: 'column_cover' });
    if (res.code === 200 && res.data) {
      cover.value = res.data.fileUrl;
      // 新封面上传成功后，删除旧封面（DB+存储），失败仅警告不影响新封面
      if (oldCover) {
        try {
          await deletePortalFile(oldCover);
        } catch (e) {
          console.warn('旧封面清理失败：', e);
        }
      }
      toast.success('封面上传成功');
    } else {
      // 上传失败：恢复旧封面（替换语义——不丢失原封面），用户可重试或改用「清除」
      cover.value = oldCover || '';
      toast.error(res.message || '上传失败，请重试');
    }
  } catch (err) {
    cover.value = oldCover || '';
    const er = err as { message?: string };
    toast.error(er?.message || '上传失败，请稍后重试');
  } finally {
    uploading.value = false;
    // 清空 input 以便重复选择同一文件
    if (fileInput.value) fileInput.value.value = '';
  }
}

// 删除封面：二次确认后调后端清理存储+记录
async function clearCover() {
  if (!cover.value) {
    cover.value = '';
    return;
  }
  const ok = window.confirm('删除后将永久清除该封面的存储与记录，且无法恢复，是否确认？');
  if (!ok) return;
  const oldCover = cover.value;
  cover.value = '';
  try {
    await deletePortalFile(oldCover);
  } catch (e) {
    toast.error('文件记录清理失败，请稍后在文件管理中处理');
    console.warn('封面清理失败：', e);
  }
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
    // 后端 ColumnVO.isFinished 为 Integer（0/1），此处把 boolean 转换为 0/1
    isFinished: isFinished.value ? 1 : 0,
  };
  if (categoryId.value.trim()) payload.categoryId = categoryId.value.trim();
  if (price.value !== '') payload.price = Number(price.value);
  if (isEdit.value && editId.value) payload.id = editId.value;
  return payload;
}

async function submit() {
  const errMsg = validate();
  if (errMsg) {
    toast.error(errMsg);
    return;
  }
  submitting.value = true;
  try {
    const res = await saveColumn(buildPayload());
    if (res.code === 200) {
      // v1.1.3 新增：保存专栏后增量同步文章关联（diff selectedArticleIds 与 originalArticleIds）
      // - 新建专栏：res.data 是新专栏 ID，所有 selectedArticleIds 都需要 addArticle
      // - 编辑专栏：editId.value 是已有专栏 ID，diff 出新增和移除
      const columnId = res.data ?? editId.value;
      if (columnId) {
        await syncArticleRelations(String(columnId));
      }
      toast.success(isEdit.value ? '专栏已更新' : '专栏创建成功');
      const newId = res.data;
      if (newId !== undefined && newId !== null && newId !== '') {
        router.push(`/column/${newId}`);
      } else if (isEdit.value && editId.value) {
        router.push(`/column/${editId.value}`);
      } else {
        router.push('/columns');
      }
    } else {
      toast.error(res.message || '保存失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '保存失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}

/**
 * v1.1.3 新增：增量同步专栏-文章关联
 * 比较 selectedArticleIds（当前勾选）与 originalArticleIds（加载时的原列表），
 * 调 addArticle 加入新增的，调 removeArticle 移除取消的。
 * 单条失败不影响整体（已加入/移出的不回滚，仅提示）。
 */
async function syncArticleRelations(columnId: string) {
  const originalSet = new Set(originalArticleIds.value.map(id => String(id)));
  const currentSet = new Set(selectedArticleIds.value.map(id => String(id)));

  const toAdd: Array<string | number> = [];
  selectedArticleIds.value.forEach(id => {
    if (!originalSet.has(String(id))) toAdd.push(id);
  });

  const toRemove: Array<string | number> = [];
  originalArticleIds.value.forEach(id => {
    if (!currentSet.has(String(id))) toRemove.push(id);
  });

  // 串行执行（避免并发对同一专栏的并发冲突）
  for (const aid of toAdd) {
    try {
      await addArticle(columnId, aid);
    } catch (e) {
      // 已加入过的会因唯一索引冲突报错，忽略
      console.warn(`加入文章 ${aid} 失败：`, e);
    }
  }
  for (const aid of toRemove) {
    try {
      await removeArticle(columnId, aid);
    } catch (e) {
      console.warn(`移出文章 ${aid} 失败：`, e);
    }
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
    <div class="border-b sticky top-0 z-30 backdrop-blur-sm py-3" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2">
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
                  {{ uploading ? '上传中...' : (cover ? '替换封面' : '上传封面') }}
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

          <!-- v1.1.3 新增：专栏文章选择（与详情页"管理文章"统一组件） -->
          <div>
            <label class="block text-sm font-medium mb-1.5 flex items-center" style="color: var(--theme-text);">
              <FileText class="w-4 h-4 mr-1" style="color: var(--theme-primary);" />
              专栏文章
              <span class="ml-1 text-xs" style="color: var(--theme-text-secondary);">从我的已发布文章中勾选，保存后自动同步关联</span>
            </label>
            <MyArticlePicker
              v-model="selectedArticleIds"
              :exclude-ids="[]"
              :multiple="true"
              placeholder="搜索文章标题加入专栏..."
            />
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
