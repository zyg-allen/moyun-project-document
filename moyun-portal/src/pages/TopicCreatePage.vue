<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ImagePlus, Loader2, Send, AlertCircle, ShieldCheck,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { generateSeo } from '@/utils/seo';
import { createTopic } from '@/api/topic';
import { uploadPortalFile } from '@/api/file';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

const title = ref('');
const description = ref('');
const cover = ref('');
const submitting = ref(false);
const uploadingCover = ref(false);

useHead(computed(() => generateSeo({
  title: '发起话题',
  description: '发起一个新话题，邀请社区成员参与讨论',
  keywords: ['发起话题', '话题创建', '讨论', '墨韵'],
  canonicalPath: '/topic/create',
  robots: 'noindex,nofollow',
})));

// 面包屑
const breadcrumbs = computed(() => [
  { label: '话题广场', path: '/topics' },
  { label: '发布话题' },
]);

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

function handleRemoveCover() {
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
  if (submitting.value) return;
  submitting.value = true;
  try {
    const res = await createTopic({
      title: t,
      description: description.value.trim() || undefined,
      cover: cover.value || undefined,
    });
    if (res.code === 200 && res.data) {
      toast.success('话题已发起');
      router.replace(`/topic/${res.data.id}`);
    } else {
      toast.error(res.message || '发起失败');
    }
  } catch (err) {
    const e = err as { message?: string };
    toast.error(e?.message || '发起失败');
  } finally {
    submitting.value = false;
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/topics');
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
        <!-- 创作者认证提示 -->
        <div
          class="rounded-xl border p-4 mb-6 flex items-start gap-3"
          style="background-color: var(--theme-accent); border-color: var(--theme-border);"
        >
          <AlertCircle class="w-5 h-5 flex-shrink-0 mt-0.5" style="color: var(--theme-primary);" />
          <div class="flex-1 text-sm" style="color: var(--theme-text);">
            <p class="font-medium mb-1">话题发起说明</p>
            <p class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
              发起话题需为认证创作者。如未通过认证，请先
              <router-link
                to="/creator/certification"
                class="inline-flex items-center underline font-medium"
                style="color: var(--theme-primary);"
              >
                <ShieldCheck class="w-3 h-3 mr-0.5" />
                申请创作者认证
              </router-link>
              。
            </p>
          </div>
        </div>

        <!-- 表单 -->
        <div
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
              <Send v-else class="w-4 h-4 mr-1" />
              发起话题
            </button>
          </div>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>
