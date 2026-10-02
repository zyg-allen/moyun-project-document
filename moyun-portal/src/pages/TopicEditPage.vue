<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ImagePlus, Loader2, Save,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { generateSeo } from '@/utils/seo';
import { getTopicDetail, updateTopic } from '@/api/topic';
import { uploadPortalFile, deletePortalFile } from '@/api/file';
import { useToast } from '@/composables/useToast';
import { useUserStore } from '@/stores/user';

const route = useRoute();
const router = useRouter();
const toast = useToast();
const userStore = useUserStore();

const topicId = computed(() => route.params.id as string);

const loading = ref(false);
const loadError = ref<string | null>(null);

const title = ref('');
/** 与 portal_topic.description varchar(500) 对齐的长度上限 */
const TOPIC_DESCRIPTION_MAX = 500;
const description = ref('');
const cover = ref('');
const submitting = ref(false);
const uploadingCover = ref(false);

useHead(computed(() => generateSeo({
  title: '编辑话题',
  description: '编辑话题内容',
  keywords: ['编辑话题', '话题管理', '旭林'],
  canonicalPath: `/topic/edit/${topicId.value}`,
  robots: 'noindex,nofollow',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '话题广场', path: '/topics' },
  { label: '编辑话题' },
]);

onMounted(() => {
  loadTopic();
});

async function loadTopic() {
  loading.value = true;
  loadError.value = null;
  try {
    const res = await getTopicDetail(topicId.value);
    if (res.code === 200 && res.data) {
      // 权限校验：只有话题发起人才能编辑
      if (String(res.data.creatorId) !== String(userStore.user?.id)) {
        toast.error('您无权编辑此话题');
        router.replace(`/topic/${topicId.value}`);
        return;
      }
      title.value = res.data.title || '';
      description.value = res.data.description || '';
      cover.value = res.data.cover || '';
    } else {
      loadError.value = res.message || '话题不存在或已被删除';
    }
  } catch (err) {
    const e = err as { message?: string };
    loadError.value = e?.message || '加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

async function handleUploadCover(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  if (uploadingCover.value) return;
  uploadingCover.value = true;
  try {
    const res = await uploadPortalFile(file, 'topic_cover');
    if (res.code === 200 && res.data) {
      cover.value = res.data.fileUrl;
      toast.success('封面图上传成功');
    } else {
      toast.error(res.message || '上传失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '上传失败');
  } finally {
    uploadingCover.value = false;
    input.value = '';
  }
}

/**
 * 待删除的旧封面：点 × 时**只标记**，保存成功后才真删存储文件。
 *
 * <p>原实现点 × 立刻 `deletePortalFile`（连 sys_file 记录一起删），但"保存"未必发生——
 * 用户随后点取消/直接离开，话题记录里的 cover 仍是旧 URL，而文件已经不存在 ⇒ 封面永久 404。</p>
 */
const pendingCoverDeletion = ref<string | null>(null);

function handleRemoveCover() {
  const oldCover = cover.value;
  if (!oldCover) return;
  // 若之前标记过又换了新封面，旧的待删标记保留（仍应在保存后清理），此处只覆盖为最早的待删项
  pendingCoverDeletion.value = pendingCoverDeletion.value || oldCover;
  cover.value = '';
}

async function handleSubmit() {
  const t = title.value.trim();
  if (!t) {
    toast.warning('请填写话题标题');
    return;
  }
  if (t.length < 2) {
    toast.warning('标题至少 2 个字符');
    return;
  }
  // 与后端列宽（varchar(500)）同口径
  if (description.value.trim().length > TOPIC_DESCRIPTION_MAX) {
    toast.warning(`话题描述不能超过 ${TOPIC_DESCRIPTION_MAX} 个字符`);
    return;
  }
  if (submitting.value) return;
  submitting.value = true;
  try {
    const res = await updateTopic(topicId.value, {
      title: t,
      // 空串 = **显式清空**（后端以 null=不改 / ''=清空 区分）。
      // 原写法 `|| undefined` 会让"清空描述/封面"被 JSON.stringify 直接丢弃，
      // 用户以为删掉了，实际旧内容仍在。
      description: description.value.trim(),
      cover: cover.value,
    });
    if (res.code === 200) {
      // 保存成功后才清理被移除的旧封面（见 handleRemoveCover：点 × 只做标记）
      if (pendingCoverDeletion.value) {
        deletePortalFile(pendingCoverDeletion.value).catch((e) => {
          console.warn('删除旧封面文件失败（不影响保存结果）:', e);
        });
        pendingCoverDeletion.value = null;
      }
      // 被驳回/待审核的话题编辑后，后端会重置为待审核并**重新送审**（见 updateTopic），
      // 提示要如实说明，否则用户会以为已经公开发布。
      toast.success(res.data?.status === 'pending' ? '已提交审核，审核通过后公开展示' : '保存成功');
      router.replace(`/topic/${topicId.value}`);
    } else {
      toast.error(res.message || '保存失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '保存失败');
  } finally {
    submitting.value = false;
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push(`/topic/${topicId.value}`);
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 吸顶面包屑栏 -->
    <div
      class="border-b sticky top-0 z-30 backdrop-blur-sm py-3"
      style="background-color: var(--theme-surface); border-color: var(--theme-border);"
    >
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between gap-4">
        <Breadcrumb :items="breadcrumbs" />
        <div class="flex items-center gap-2"></div>
      </div>
    </div>

    <div class="flex-1 py-8">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <!-- 加载中 -->
        <div v-if="loading" class="flex flex-col items-center justify-center py-20">
          <div
            class="animate-spin rounded-full h-12 w-12 border-b-2"
            style="border-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 加载失败 -->
        <div
          v-else-if="loadError"
          class="rounded-xl border p-8 max-w-md mx-auto text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <p class="mb-4 text-sm" style="color: var(--theme-text);">{{ loadError }}</p>
          <button
            @click="loadTopic"
            class="px-4 py-2 text-white rounded-lg text-sm transition hover:opacity-90"
            style="background-color: var(--theme-primary);"
          >
            重试
          </button>
        </div>

        <!-- 表单 -->
        <div
          v-else
          class="rounded-2xl border p-6"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <!-- 标题 -->
          <div class="mb-5">
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
              话题标题 <span style="color: #ef4444;">*</span>
            </label>
            <input
              v-model="title"
              type="text"
              maxlength="100"
              placeholder="用一句话描述你想讨论的话题..."
              class="w-full rounded-lg border px-3 py-2.5 text-sm outline-none transition focus:border-[var(--theme-primary)]"
              style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
            />
            <p class="mt-1 text-xs text-right" style="color: var(--theme-text-secondary);">
              {{ title.length }} / 100
            </p>
          </div>

          <!-- 封面图 -->
          <div class="mb-5">
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
              封面图（可选）
            </label>
            <div v-if="cover" class="relative inline-block">
              <LazyImage
                :src="cover"
                alt="话题封面"
                class="rounded-lg object-cover w-64 h-36"
              />
              <button
                @click="handleRemoveCover"
                class="absolute -top-2 -right-2 w-6 h-6 rounded-full text-white text-xs transition hover:opacity-80"
                style="background-color: #ef4444;"
              >
                ×
              </button>
            </div>
            <label
              v-else
              class="flex flex-col items-center justify-center w-64 h-36 rounded-lg border-2 border-dashed cursor-pointer transition hover:opacity-80"
              style="border-color: var(--theme-border); background-color: var(--theme-bg);"
            >
              <Loader2 v-if="uploadingCover" class="w-6 h-6 animate-spin mb-2" style="color: var(--theme-primary);" />
              <ImagePlus v-else class="w-6 h-6 mb-2" style="color: var(--theme-text-secondary);" />
              <span class="text-xs" style="color: var(--theme-text-secondary);">
                {{ uploadingCover ? '上传中...' : '点击上传封面' }}
              </span>
              <input
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleUploadCover"
              />
            </label>
          </div>

          <!-- 话题描述 -->
          <div class="mb-5">
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
              话题描述（可选）
            </label>
            <MarkdownEditor
              v-model="description"
              :maxlength="TOPIC_DESCRIPTION_MAX"
              placeholder="补充话题背景、讨论方向、参与规则等..."
            />
          </div>

          <!-- 操作按钮 -->
          <div class="flex items-center justify-end gap-2 pt-3 border-t" style="border-color: var(--theme-border);">
            <button
              @click="goBack"
              class="px-4 py-2 text-sm rounded-lg transition hover:opacity-80"
              style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              :disabled="submitting || !title.trim()"
              class="inline-flex items-center px-4 py-2 text-sm text-white rounded-lg transition hover:opacity-90 disabled:opacity-50"
              style="background-color: var(--theme-primary);"
            >
              <Loader2 v-if="submitting" class="w-4 h-4 mr-1 animate-spin" />
              <Save v-else class="w-4 h-4 mr-1" />
              保存
            </button>
          </div>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
