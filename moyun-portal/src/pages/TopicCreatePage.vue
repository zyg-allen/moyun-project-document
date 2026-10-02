<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  ImagePlus, Loader2, Send, Clock,
} from 'lucide-vue-next';
import Breadcrumb from '@/components/Breadcrumb.vue';
import SiteFooter from '@/components/SiteFooter.vue';
import LazyImage from '@/components/LazyImage.vue';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { generateSeo } from '@/utils/seo';
import { createTopic } from '@/api/topic';
import { onBeforeRouteLeave } from 'vue-router';
import { uploadPortalFile, deletePortalFile } from '@/api/file';
import { useToast } from '@/composables/useToast';

const router = useRouter();
const toast = useToast();

const title = ref('');
/** 与 portal_topic.description varchar(500) 对齐的长度上限 */
const TOPIC_DESCRIPTION_MAX = 500;
const description = ref('');
const cover = ref('');
const submitting = ref(false);
const uploadingCover = ref(false);

useHead(computed(() => generateSeo({
  title: '发起话题',
  description: '发起一个新话题，邀请社区成员参与讨论',
  keywords: ['发起话题', '话题创建', '讨论', '旭林'],
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

async function handleRemoveCover() {
  const oldCover = cover.value;
  cover.value = '';
  // 清理已上传的封面文件，避免产生孤儿文件
  if (oldCover) {
    try {
      await deletePortalFile(oldCover);
    } catch (e) {
      console.warn('删除封面文件失败:', e);
    }
  }
}

/**
 * 已上传但未随话题提交的封面（清单 P2）。
 *
 * <p>封面是"先上传拿到 fileUrl、再随话题提交"的两步流程，而原先**只在用户手动点 × 时**
 * 才调用 deletePortalFile —— 用户上传后直接离开页面（不点取消、不点 ×）时**没有任何清理**，
 * 文件成为孤儿。这里记录"尚未随话题提交的封面"，在离开页面/组件卸载时回收。</p>
 */
const pendingUploadedCover = ref('');

/** 回收未提交的封面上传（提交成功或已手动移除时不处理） */
async function cleanupPendingCover() {
  const url = pendingUploadedCover.value;
  if (!url) return;
  pendingUploadedCover.value = '';
  try {
    await deletePortalFile(url);
  } catch (e) {
    console.warn('清理未提交封面失败:', e);
  }
}

onBeforeRouteLeave(async () => {
  await cleanupPendingCover();
  return true;
});

onUnmounted(() => {
  // 组件卸载（含直接关标签前的 SPA 卸载）兜底；beforeunload 场景无法异步删除，属已知限制
  void cleanupPendingCover();
});

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
  // 与后端列宽（varchar(500)）同口径；编辑器 maxlength 只是 UI 约束，提交口必须再拦一次
  if (description.value.trim().length > TOPIC_DESCRIPTION_MAX) {
    toast.warning(`话题描述不能超过 ${TOPIC_DESCRIPTION_MAX} 个字符`);
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
      // 封面已随话题入库 ⇒ 不再是"未提交上传"，不要回收（清单 P2）
      pendingUploadedCover.value = '';
      // 新话题默认 pending 待审核，审核通过后才会在话题广场曝光
      // 此处明确告知用户审核状态，避免误以为已发布
      toast.success('话题已提交，等待审核通过后将在话题广场展示');
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
        <!-- 话题发起说明 -->
        <div
          class="rounded-xl border p-4 mb-6 flex items-start gap-3"
          style="background-color: var(--theme-accent); border-color: var(--theme-border);"
        >
          <div class="flex-1 text-sm" style="color: var(--theme-text);">
            <p class="font-medium mb-1">话题发起说明</p>
            <p class="text-xs leading-relaxed" style="color: var(--theme-text-secondary);">
              登录后即可发起话题，内容提交后将进入审核流程。
            </p>
          </div>
        </div>

        <!-- 审核流程提示 -->
        <div
          class="rounded-xl border p-4 mb-6 flex items-start gap-3"
          style="background-color: rgba(245, 158, 11, 0.08); border-color: rgba(245, 158, 11, 0.3);"
        >
          <Clock class="w-5 h-5 flex-shrink-0 mt-0.5" style="color: #d97706;" />
          <div class="flex-1 text-sm" style="color: var(--theme-text);">
            <p class="font-medium mb-1" style="color: #d97706;">审核流程</p>
            <ul class="text-xs leading-relaxed space-y-1" style="color: var(--theme-text-secondary);">
              <li>· 提交后话题进入「待审核」状态，不会立即在话题广场展示</li>
              <!--
              清单 P2：原文案写"命中内容会转人工重点审核"，与后端实际行为**相反** ——
              Controller 在写库前用同一敏感词表直接 return error **拒绝创建**
              （Service 里"命中仍保持 pending、不阻断创建"的分支因前置拦截而永不命中）。
              这里按真实行为改写。
            -->
            <li>· 提交时会做敏感词校验，命中将直接拒绝创建，请修改后重试</li>
              <li>· 审核通过后话题自动发布到广场，并通过站内信通知你</li>
              <li>
              · 审核驳回会附驳回原因，可在「我的话题」查看并修改后重新提交
              <!-- 清单 P2：原文案让用户去「我的话题」，而全站当时没有任何入口，这里给出直达链接 -->
              <button
                type="button"
                class="ml-1 underline"
                style="color: var(--theme-primary);"
                @click="router.push('/topic/my/topics')"
              >前往我的话题</button>
            </li>
            </ul>
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
