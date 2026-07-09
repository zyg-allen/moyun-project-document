<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
import { ArrowLeft, Save, Send, Eye, Edit3, Loader2 } from 'lucide-vue-next';
import { marked } from 'marked';
import SiteFooter from '@/components/SiteFooter.vue';
import { generateSeo } from '@/utils/seo';
import { publishExperience, updateExperience, getExperienceDetail } from '@/api/interview';

const route = useRoute();
const router = useRouter();

const editId = computed(() => route.params.id as string | undefined);
const isEdit = computed(() => !!editId.value);

// 表单字段
const title = ref('');
const company = ref('');
const position = ref('');
const year = ref<number | ''>('');
const month = ref<number | ''>('');
const tags = ref('');
const summary = ref('');
const content = ref('');

const previewMode = ref(false);
const submitting = ref(false);
const loadingDetail = ref(false);
const pageError = ref<string | null>(null);

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
});

const renderedContent = computed(() => {
  if (!content.value) return '';
  try {
    return marked.parse(content.value) as string;
  } catch {
    return content.value;
  }
});

const summaryCount = computed(() => summary.value.length);
const contentCount = computed(() => content.value.replace(/\s/g, '').length);

// Toast
const toast = ref<{ message: string; type: 'success' | 'error' } | null>(null);
let toastTimer: number | null = null;
function showToast(message: string, type: 'success' | 'error' = 'success') {
  toast.value = { message, type };
  if (toastTimer) window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => { toast.value = null; }, 3000);
}

useHead(computed(() => generateSeo({
  title: isEdit.value ? '编辑面经' : '发布面经',
  description: '分享你的真实面试经验，帮助更多求职者备战面试、直通 Offer',
  keywords: ['发布面经', '面经', '面试经验', '面经投稿', '墨韵'],
  canonicalPath: isEdit.value
    ? `/interview/experience/edit/${editId.value}`
    : '/interview/experience/publish',
  robots: 'noindex,nofollow',
})));

const pageTitle = computed(() => isEdit.value ? '编辑面经' : '发布面经');

const months = Array.from({ length: 12 }, (_, i) => i + 1);

function goBack() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/interview/my/experiences');
  }
}

function validate(): string | null {
  if (!title.value.trim()) return '请填写标题';
  if (!company.value.trim()) return '请填写公司';
  if (content.value.replace(/\s/g, '').length < 50) return '正文内容不少于 50 字';
  if (summary.value.length > 500) return '摘要不能超过 500 字';
  return null;
}

function buildPayload() {
  const payload: any = {
    title: title.value.trim(),
    company: company.value.trim(),
    content: content.value,
  };
  if (position.value.trim()) payload.position = position.value.trim();
  if (year.value !== '') payload.year = Number(year.value);
  if (month.value !== '') payload.month = Number(month.value);
  if (summary.value.trim()) payload.summary = summary.value.trim();
  if (tags.value.trim()) payload.tags = tags.value.trim();
  return payload;
}

async function submit(status: 'draft' | 'pending') {
  const errMsg = validate();
  if (errMsg) {
    showToast(errMsg, 'error');
    return;
  }
  try {
    submitting.value = true;
    if (isEdit.value && editId.value) {
      await updateExperience(editId.value, buildPayload());
      showToast(status === 'draft' ? '草稿已保存' : '面经已更新', 'success');
    } else {
      await publishExperience({ ...buildPayload(), status });
      if (status === 'pending') {
        showToast('发布成功，发布后需审核', 'success');
      } else {
        showToast('草稿已保存', 'success');
      }
    }
    setTimeout(() => {
      router.push('/interview/my/experiences');
    }, 800);
  } catch (err: any) {
    showToast(err?.message || '提交失败，请稍后重试', 'error');
  } finally {
    submitting.value = false;
  }
}

async function loadDetail() {
  if (!editId.value) return;
  try {
    loadingDetail.value = true;
    pageError.value = null;
    const res = await getExperienceDetail(editId.value);
    if (res.code === 200 && res.data) {
      const d: any = res.data;
      title.value = d.title || '';
      company.value = d.company || '';
      position.value = d.position || '';
      year.value = d.year ?? '';
      month.value = d.month ?? '';
      summary.value = d.summary || '';
      content.value = d.content || '';
      // tags 可能是数组或字符串
      if (Array.isArray(d.tags)) {
        tags.value = d.tags.join(', ');
      } else if (typeof d.tags === 'string') {
        tags.value = d.tags;
      } else if (Array.isArray(d.tagList)) {
        tags.value = d.tagList.map((t: any) => t.name).filter(Boolean).join(', ');
      }
    } else {
      pageError.value = res.message || '加载面经失败';
    }
  } catch (err: any) {
    pageError.value = err?.message || '加载面经失败，请稍后重试';
  } finally {
    loadingDetail.value = false;
  }
}

onMounted(() => {
  if (isEdit.value) {
    loadDetail();
  }
});
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
        <span class="w-12"></span>
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

    <!-- 内容区 -->
    <div class="flex-1 py-8">
      <div class="max-w-3xl mx-auto px-4">
        <!-- 标题 -->
        <div class="mb-6 text-center">
          <h1 class="text-3xl font-bold tracking-tight mb-2" style="color: var(--theme-text);">
            {{ pageTitle }}
          </h1>
          <p class="text-sm" style="color: var(--theme-text-secondary);">
            分享真实面试经验，帮助更多求职者少走弯路
          </p>
        </div>

        <!-- 加载详情中 -->
        <div
          v-if="loadingDetail"
          class="rounded-xl border p-12 text-center"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <div
            class="animate-spin rounded-full h-10 w-10 border-2 mx-auto"
            style="border-color: var(--theme-border); border-top-color: var(--theme-primary);"
          ></div>
          <p class="mt-4 text-sm" style="color: var(--theme-text-secondary);">加载中...</p>
        </div>

        <!-- 加载失败 -->
        <div
          v-else-if="pageError"
          class="rounded-xl border p-8 text-center"
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

        <!-- 表单卡片 -->
        <div
          v-else
          class="rounded-xl border shadow-sm p-6 md:p-8"
          style="background-color: var(--theme-surface); border-color: var(--theme-border);"
        >
          <!-- 标题 -->
          <div class="mb-5">
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
              标题 <span style="color: #ef4444;">*</span>
            </label>
            <input
              v-model="title"
              type="text"
              placeholder="请输入面经标题，例如：字节跳动前端一面面经"
              maxlength="100"
              class="form-input w-full px-3 py-2.5 rounded-lg text-sm focus:outline-none transition"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            />
          </div>

          <!-- 公司 + 岗位 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-5">
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                公司 <span style="color: #ef4444;">*</span>
              </label>
              <input
                v-model="company"
                type="text"
                placeholder="例如：字节跳动"
                maxlength="50"
                class="form-input w-full px-3 py-2.5 rounded-lg text-sm focus:outline-none transition"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                岗位
              </label>
              <input
                v-model="position"
                type="text"
                placeholder="例如：前端开发工程师"
                maxlength="50"
                class="form-input w-full px-3 py-2.5 rounded-lg text-sm focus:outline-none transition"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              />
            </div>
          </div>

          <!-- 年份 + 月份 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-5">
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                年份
              </label>
              <input
                v-model="year"
                type="number"
                placeholder="例如：2025"
                min="1990"
                max="2099"
                class="form-input w-full px-3 py-2.5 rounded-lg text-sm focus:outline-none transition"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              />
            </div>
            <div>
              <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
                月份
              </label>
              <select
                v-model="month"
                class="form-input w-full px-3 py-2.5 rounded-lg text-sm focus:outline-none transition"
                style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
              >
                <option :value="''">选择月份</option>
                <option v-for="m in months" :key="m" :value="m">{{ m }} 月</option>
              </select>
            </div>
          </div>

          <!-- 标签 -->
          <div class="mb-5">
            <label class="block text-sm font-medium mb-2" style="color: var(--theme-text);">
              标签
            </label>
            <input
              v-model="tags"
              type="text"
              placeholder="多个标签用逗号分隔，例如：前端,React,一面"
              maxlength="200"
              class="form-input w-full px-3 py-2.5 rounded-lg text-sm focus:outline-none transition"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            />
          </div>

          <!-- 摘要 -->
          <div class="mb-5">
            <label class="block text-sm font-medium mb-2 flex items-center justify-between" style="color: var(--theme-text);">
              <span>摘要</span>
              <span class="text-xs" :style="{ color: summaryCount > 500 ? '#ef4444' : 'var(--theme-text-secondary)' }">
                {{ summaryCount }} / 500
              </span>
            </label>
            <textarea
              v-model="summary"
              rows="3"
              maxlength="500"
              placeholder="一句话概括这次面试的核心内容（选填）"
              class="form-input w-full px-3 py-2.5 rounded-lg text-sm focus:outline-none transition resize-y"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            ></textarea>
          </div>

          <!-- 正文内容 -->
          <div class="mb-6">
            <div class="flex items-center justify-between mb-2">
              <label class="text-sm font-medium" style="color: var(--theme-text);">
                正文内容 <span style="color: #ef4444;">*</span>
                <span class="text-xs ml-2" style="color: var(--theme-text-secondary);">
                  （支持 Markdown，不少于 50 字）
                </span>
              </label>
              <div class="flex items-center gap-1 rounded-lg p-0.5" style="background-color: var(--theme-bg); border: 1px solid var(--theme-border);">
                <button
                  type="button"
                  @click="previewMode = false"
                  class="px-3 py-1 rounded-md text-xs flex items-center transition"
                  :style="!previewMode
                    ? 'background-color: var(--theme-primary); color: #fff;'
                    : 'color: var(--theme-text-secondary);'"
                >
                  <Edit3 class="w-3 h-3 mr-1" />
                  编辑
                </button>
                <button
                  type="button"
                  @click="previewMode = true"
                  class="px-3 py-1 rounded-md text-xs flex items-center transition"
                  :style="previewMode
                    ? 'background-color: var(--theme-primary); color: #fff;'
                    : 'color: var(--theme-text-secondary);'"
                >
                  <Eye class="w-3 h-3 mr-1" />
                  预览
                </button>
              </div>
            </div>

            <!-- 编辑模式 -->
            <textarea
              v-if="!previewMode"
              v-model="content"
              rows="14"
              placeholder="请输入面经正文，支持 Markdown 语法..."
              class="form-input w-full px-3 py-2.5 rounded-lg text-sm font-mono focus:outline-none transition resize-y"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            ></textarea>

            <!-- 预览模式 -->
            <div
              v-else
              class="prose prose-lg max-w-none rounded-lg p-4 min-h-[200px] text-sm"
              style="background-color: var(--theme-bg); border: 1px solid var(--theme-border); color: var(--theme-text);"
            >
              <div v-if="content" v-html="renderedContent"></div>
              <p v-else style="color: var(--theme-text-secondary); opacity: 0.5;">暂无内容可预览</p>
            </div>

            <div class="mt-2 text-xs flex justify-between" style="color: var(--theme-text-secondary);">
              <span>提示：支持 **粗体**、*斜体*、`代码`、列表、引用等 Markdown 语法</span>
              <span>{{ contentCount }} 字</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="flex flex-col sm:flex-row gap-3 pt-4 border-t" style="border-color: var(--theme-border);">
            <button
              @click="submit('draft')"
              :disabled="submitting"
              class="flex-1 flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-medium transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
              style="background-color: var(--theme-bg); color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              <Loader2 v-if="submitting" class="w-4 h-4 mr-2 animate-spin" />
              <Save v-else class="w-4 h-4 mr-2" />
              存为草稿
            </button>
            <button
              @click="submit('pending')"
              :disabled="submitting"
              class="flex-1 flex items-center justify-center px-5 py-2.5 rounded-lg text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed"
              style="background-color: var(--theme-primary);"
            >
              <Loader2 v-if="submitting" class="w-4 h-4 mr-2 animate-spin" />
              <Send v-else class="w-4 h-4 mr-2" />
              发布
            </button>
          </div>
          <p class="mt-3 text-center text-xs" style="color: var(--theme-text-secondary);">
            发布后需审核，审核通过后将公开展示
          </p>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>

<style scoped>
.form-input::placeholder {
  color: var(--theme-text-secondary);
  opacity: 0.5;
}
.form-input:focus {
  border-color: var(--theme-primary);
}

.prose :deep(h1),
.prose :deep(h2),
.prose :deep(h3) {
  color: var(--theme-text);
  font-weight: 600;
}
.prose :deep(p) {
  color: var(--theme-text);
  margin: 0.5rem 0;
}
.prose :deep(a) {
  color: var(--theme-primary);
}
.prose :deep(ul),
.prose :deep(ol) {
  color: var(--theme-text);
  margin: 0.5rem 0;
  padding-left: 1.5rem;
}
.prose :deep(ul) {
  list-style: disc;
}
.prose :deep(ol) {
  list-style: decimal;
}
.prose :deep(li) {
  margin: 0.25rem 0;
}
.prose :deep(strong) {
  color: var(--theme-text);
}
.prose :deep(blockquote) {
  border-left: 3px solid var(--theme-primary);
  padding-left: 1rem;
  margin: 0.75rem 0;
  color: var(--theme-text-secondary);
}
.prose :deep(code) {
  background-color: var(--theme-surface);
  padding: 0.125rem 0.375rem;
  border-radius: 0.25rem;
  font-size: 0.85em;
}
.prose :deep(pre) {
  background-color: var(--theme-surface);
  padding: 0.75rem;
  border-radius: 0.5rem;
  overflow-x: auto;
}
.prose :deep(pre code) {
  background: transparent;
  padding: 0;
}
</style>
