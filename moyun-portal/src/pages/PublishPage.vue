<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  ArrowLeft, Image as ImageIcon, Save, Eye, Send, X,
  List, ListOrdered, Clock, User, Calendar, FileText, Settings,
  Sparkles, Globe, Lock, Tag as TagIcon, BookOpen,
  ChevronDown, Check, Type, Plus, ChevronRight, Code
} from 'lucide-vue-next';
import { addArticle } from '@/data/mockData';
import { categories as mockCategories } from '@/data/categories';
import {
  getHotTags,
  searchTagList,
  createNewTag,
  getRecommendTags,
  mockGetHotTags,
  mockSearchTags,
  mockCreateTag,
  mockGetRecommendTags
} from '@/api/tag';
import { getCategoryList, getCategoryTree } from '@/api/category';
import { createArticle } from '@/api/article';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import type { Tag, Category } from '@/types/api';
import SiteFooter from '@/components/SiteFooter.vue';
import QuillEditor from '@/components/QuillEditor.vue';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { extractExcerpt } from '@/utils/excerpt';

const router = useRouter();
const userStore = useUserStore();
const { requireAuth } = useAuth();

// 用户信息
const currentUser = computed(() => userStore.user);

// 文件上传相关
const fileInputRef = ref<HTMLInputElement | null>(null);

// 基础信息
const title = ref('');
const content = ref('');
const tags = ref<string[]>([]);
const tagInput = ref('');
const excerpt = ref('');
const coverImage = ref('');

// 分类 - 二级联动
const categories = ref<Category[]>([]);
const selectedParentCategory = ref<string>('');
const selectedChildCategory = ref<string>('');
const loadingCategories = ref(false);

// 获取当前选中的分类ID
const selectedCategory = computed(() => {
  return selectedChildCategory.value || selectedParentCategory.value || '';
});

// 获取当前分类的子分类
const childCategories = computed(() => {
  const parent = categories.value.find(c => c.id === selectedParentCategory.value);
  return parent?.children || [];
});

// 元信息
const articleStatus = ref<'draft' | 'published' | 'review'>('draft');
const publishTime = ref('');
const authorName = ref('');

// SEO设置
const seoTitle = ref('');
const seoDescription = ref('');
const seoKeywords = ref('');

// 标签相关状态
const hotTags = ref<Tag[]>([]);
const tagSuggestions = ref<Tag[]>([]);
const tagSearchResults = ref<Tag[]>([]);
const isSearchingTags = ref(false);
const showTagSuggestions = ref(false);

// 评论设置
const allowComments = ref(true);
const commentModeration = ref(false);

// 权限设置
const visibility = ref<'public' | 'private' | 'password'>('public');
const articlePassword = ref('');

// 自定义URL
const customSlug = ref('');

// 编辑器设置
const editorMode = ref<'richtext' | 'markdown'>('richtext');
const showPreview = ref(false);
const isPublishing = ref(false);
const isSaving = ref(false);
const lastSaved = ref<string | null>(null);

// 高级选项显示状态
const showAdvanced = ref(false);
const showSeoSettings = ref(false);
const showCommentSettings = ref(false);
const showPermissionSettings = ref(false);

// 字数统计
const wordCount = computed(() => {
  return content.value.length;
});

const readingTime = computed(() => {
  const minutes = Math.ceil(wordCount.value / 400);
  return minutes;
});

// Markdown预览
const markdownPreview = computed(() => {
  return content.value
      .replace(/^### (.*$)/gim, '<h3>$1</h3>')
      .replace(/^## (.*$)/gim, '<h2>$1</h2>')
      .replace(/^# (.*$)/gim, '<h1>$1</h1>')
      .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
      .replace(/\*(.*)\*/gim, '<em>$1</em>')
      .replace(/\n/g, '<br>');
});

onMounted(async () => {
  // 等待用户状态初始化
  if (!userStore.isUserInitialized) {
    await userStore.initializeUser();
  }

  // 检查是否登录
  if (!userStore.isAuthenticated) {
    requireAuth();
    return;
  }

  authorName.value = userStore.username;
  publishTime.value = new Date().toISOString().slice(0, 16);

  // 加载分类
  loadCategories();

  // 加载热门标签
  loadHotTags();
});

// 加载分类
async function loadCategories() {
  loadingCategories.value = true;
  try {
    const response = await getCategoryTree();
    if (response.code === 200 && response.data) {
      categories.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load categories:', error);
    // 降级到mock数据，转换为正确的Category类型
    categories.value = mockCategories.map(cat => ({
      id: cat.id,
      name: cat.name,
      slug: cat.id || cat.name.toLowerCase(),
      description: (cat as any).description,
      icon: (cat as any).icon,
      articleCount: (cat as any).articleCount,
      sort: 0,
      createdAt: new Date().toISOString(),
      children: []
    }));
  } finally {
    loadingCategories.value = false;
  }
}

// 加载热门标签
async function loadHotTags() {
  try {
    const response = await getHotTags(30);
    if (response.code === 200 && response.data) {
      hotTags.value = response.data;
    } else {
      throw new Error('API returned error');
    }
  } catch (error) {
    console.error('Failed to load hot tags, falling back to mock:', error);
    try {
      const mockResponse = await mockGetHotTags(30);
      if (mockResponse.code === 200) {
        hotTags.value = mockResponse.data;
      }
    } catch (mockError) {
      console.error('Failed to load mock hot tags:', mockError);
    }
  }
}

// 搜索标签
async function searchForTags(keyword: string) {
  if (!keyword.trim()) {
    tagSearchResults.value = [];
    isSearchingTags.value = false;
    return;
  }

  isSearchingTags.value = true;
  try {
    const response = await searchTagList(keyword);
    if (response.code === 200 && response.data) {
      tagSearchResults.value = response.data;
    } else {
      throw new Error('API returned error');
    }
  } catch (error) {
    console.error('Failed to search tags, falling back to mock:', error);
    try {
      const mockResponse = await mockSearchTags(keyword);
      if (mockResponse.code === 200) {
        tagSearchResults.value = mockResponse.data;
      }
    } catch (mockError) {
      console.error('Failed to search mock tags:', mockError);
    }
  } finally {
    isSearchingTags.value = false;
  }
}

// 获取标签建议
async function loadTagSuggestions() {
  if (!title.value.trim() && !selectedParentCategory.value) {
    return;
  }

  try {
    const response = await getRecommendTags(title.value, selectedParentCategory.value);
    if (response.code === 200 && response.data) {
      tagSuggestions.value = response.data;
    } else {
      throw new Error('API returned error');
    }
  } catch (error) {
    console.error('Failed to load tag suggestions, falling back to mock:', error);
    try {
      const mockResponse = await mockGetRecommendTags(title.value, selectedParentCategory.value);
      if (mockResponse.code === 200) {
        tagSuggestions.value = mockResponse.data;
      }
    } catch (mockError) {
      console.error('Failed to load mock tag suggestions:', mockError);
    }
  }
}

// 添加标签
function addTagFromSuggestion(tagName: string) {
  addTag(tagName);
}

// 创建并添加新标签
async function createAndAddTag(tagName: string) {
  if (!tagName.trim()) return;

  // 检查是否已存在
  if (tags.value.includes(tagName.trim())) {
    return;
  }

  try {
    const response = await createNewTag(tagName.trim());
    if (response.code === 200 && response.data) {
      addTag(response.data.name);
    } else {
      throw new Error('API returned error');
    }
  } catch (error) {
    console.error('Failed to create tag, falling back to mock:', error);
    try {
      const mockResponse = await mockCreateTag(tagName.trim());
      if (mockResponse.code === 200) {
        addTag(mockResponse.data.name);
      }
    } catch (mockError) {
      console.error('Failed to create mock tag:', mockError);
      // 直接添加标签名
      addTag(tagName.trim());
    }
  }
}

// 监听标题和分类变化，更新标签建议
watch([title, selectedParentCategory], () => {
  if (title.value.trim().length > 2 || selectedParentCategory.value) {
    loadTagSuggestions();
  }
});

// 添加标签
function addTag(tag: string) {
  const trimmedTag = tag.trim();
  if (trimmedTag && !tags.value.includes(trimmedTag)) {
    if (tags.value.length >= 10) {
      alert('最多只能添加10个标签');
      return;
    }
    tags.value.push(trimmedTag);
    tagInput.value = '';
    tagSearchResults.value = [];
  }
}

// 处理标签输入回车
function handleTagInputEnter() {
  if (tagInput.value.trim()) {
    // 优先检查搜索结果中是否有匹配的标签
    const matchedTag = tagSearchResults.value.find(t => t.name.toLowerCase() === tagInput.value.toLowerCase());
    if (matchedTag) {
      addTag(matchedTag.name);
    } else {
      // 如果搜索没有结果，创建新标签
      createAndAddTag(tagInput.value);
    }
  }
}

// 移除标签
function removeTag(tag: string) {
  tags.value = tags.value.filter(t => t !== tag);
}

// 保存草稿
async function saveDraft(isAuto = false) {
  if (isAuto) {
    isSaving.value = true;
  }

  // 模拟API调用
  setTimeout(() => {
    lastSaved.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    isSaving.value = false;

    if (!isAuto) {
      alert('草稿已保存');
    }
  }, 500);
}

// 预览文章
function previewArticle() {
  showPreview.value = true;
}

// 关闭预览
function closePreview() {
  showPreview.value = false;
}

// 发布文章
async function handlePublish() {
  if (!title.value.trim()) {
    alert('请输入文章标题');
    return;
  }
  if (title.value.length < 4) {
    alert('标题至少4个字符');
    return;
  }
  if (!content.value.trim()) {
    alert('请输入文章内容');
    return;
  }
  if (content.value.length < 50) {
    alert('内容至少50个字符');
    return;
  }
  if (!selectedCategory.value) {
    alert('请选择文章分类');
    return;
  }

  isPublishing.value = true;

  try {
    const finalContent = editorMode.value === 'markdown'
        ? markdownPreview.value
        : content.value;

    const response = await createArticle({
      title: title.value,
      content: finalContent,
      contentMarkdown: editorMode.value === 'markdown' ? content.value : undefined,
      excerpt: excerpt.value || content.value.substring(0, 200) + '...',
      cover: coverImage.value || '',
      categoryId: selectedCategory.value,
      tagNames: tags.value,
      status: 'published' // 已发布
    });

    if (response.code === 200) {
      articleStatus.value = 'published';
      alert('发布成功！');
      router.push('/');
    } else {
      alert('发布失败：' + (response.message || '未知错误'));
    }
  } catch (error: any) {
    console.error('Failed to publish article:', error);
    // 显示错误信息
    const errorMessage = error?.message || '发布失败，请稍后重试';

    // 如果是未登录错误，跳转到登录页
    if (errorMessage.includes('登录') || errorMessage.includes('认证') || errorMessage.includes('401')) {
      if (confirm('您还没有登录，是否前往登录？')) {
        router.push('/login');
      }
    } else {
      alert(errorMessage);
    }
  } finally {
    isPublishing.value = false;
  }
}

// 返回列表
function goBack() {
  if (title.value.trim() || content.value.trim()) {
    if (confirm('有未保存的内容，确定要离开吗？')) {
      router.push('/');
    }
  } else {
    router.push('/');
  }
}

// 摘要提取状态
const isExtractingExcerpt = ref(false);

// 从正文提取摘要
async function extractExcerptFromContent() {
  if (!content.value) return;

  isExtractingExcerpt.value = true;
  try {
    excerpt.value = await extractExcerpt(content.value, editorMode.value);
  } catch (error) {
    console.error('摘要提取失败:', error);
    alert('摘要提取失败，请重试');
  } finally {
    isExtractingExcerpt.value = false;
  }
}

// 文件上传相关函数
function triggerFileUpload() {
  fileInputRef.value?.click();
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) {
    handleFile(file);
  }
}

function handleDrop(event: DragEvent) {
  const file = event.dataTransfer?.files?.[0];
  if (file) {
    handleFile(file);
  }
}

function handleFile(file: File) {
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件！');
    return;
  }

  // 检查文件大小 (最大 5MB)
  if (file.size > 5 * 1024 * 1024) {
    alert('文件大小不能超过 5MB！');
    return;
  }

  // 创建预览 URL
  const reader = new FileReader();
  reader.onload = (e) => {
    if (e.target?.result) {
      coverImage.value = e.target.result as string;
    }
  };
  reader.readAsDataURL(file);
}

// 计算标题字数
const titleLength = computed(() => title.value.length);
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--theme-bg);">
    <!-- 顶部操作栏 -->
    <div class="sticky top-0 z-30 border-b" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex items-center justify-between h-16">
          <!-- 左侧 -->
          <div class="flex items-center gap-4">
            <button @click="goBack" class="p-2 rounded-lg transition-colors hover:bg-gray-100" style="color: var(--theme-text-secondary);">
              <ArrowLeft class="w-5 h-5" />
            </button>
            <div class="flex items-center gap-2">
              <span class="text-lg font-semibold" style="color: var(--theme-text);">写文章</span>
              <!-- 状态标签 -->
              <span
                  class="px-2.5 py-1 rounded-full text-xs font-medium"
                  :style="{
                  backgroundColor: articleStatus === 'draft' ? 'var(--theme-accent)' : 
                                   articleStatus === 'review' ? '#fef3c7' : '#d1fae5',
                  color: articleStatus === 'draft' ? 'var(--theme-primary)' : 
                         articleStatus === 'review' ? '#92400e' : '#065f46'
                }"
              >
                {{ articleStatus === 'draft' ? '草稿' : articleStatus === 'review' ? '审核中' : '已发布' }}
              </span>
            </div>
          </div>

          <!-- 右侧操作按钮 -->
          <div class="flex items-center gap-3">
            <!-- 自动保存提示 -->
            <div v-if="lastSaved" class="hidden sm:flex items-center gap-1.5 text-xs" style="color: var(--theme-text-secondary);">
              <Check class="w-3.5 h-3.5 text-green-500" />
              <span>已保存于 {{ lastSaved }}</span>
            </div>

            <!-- 保存草稿 -->
            <button
                @click="saveDraft(false)"
                :disabled="isSaving"
                class="px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2"
                style="color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              <Save class="w-4 h-4" />
              <span class="hidden sm:inline">保存草稿</span>
            </button>

            <!-- 预览 -->
            <button
                @click="previewArticle"
                class="px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2"
                :style="showPreview ? { backgroundColor: 'var(--theme-accent)', color: 'var(--theme-primary)' } : { color: 'var(--theme-text-secondary)' }"
            >
              <Eye class="w-4 h-4" />
              <span class="hidden sm:inline">预览</span>
            </button>

            <!-- 发布 -->
            <button
                @click="handlePublish"
                :disabled="isPublishing"
                class="px-5 py-2 rounded-lg font-medium transition-all flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                style="background-color: var(--theme-primary); color: white;"
            >
              <Send class="w-4 h-4" />
              {{ isPublishing ? '发布中...' : '发布' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="py-8 flex-1">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
          <!-- 左侧主编辑区 -->
          <div class="lg:col-span-3 space-y-4">
            <!-- 元信息区 -->
            <div class="rounded-lg p-3 flex flex-wrap items-center gap-3" style="background-color: var(--theme-surface); border: 1px solid var(--theme-border);">
              <!-- 作者 -->
              <div class="flex items-center gap-2">
                <User class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                <span class="text-sm" style="color: var(--theme-text-secondary);">作者：</span>
                <span class="text-sm font-medium" style="color: var(--theme-text);">{{ authorName }}</span>
              </div>

              <!-- 阅读时长 -->
              <div class="flex items-center gap-2">
                <Clock class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                <span class="text-sm" style="color: var(--theme-text-secondary);">预计阅读：</span>
                <span class="text-sm font-medium" style="color: var(--theme-primary);">{{ readingTime }} 分钟</span>
              </div>

              <!-- 字数统计 -->
              <div class="flex items-center gap-2">
                <FileText class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                <span class="text-sm" style="color: var(--theme-text-secondary);">字数：</span>
                <span class="text-sm font-medium" style="color: var(--theme-text);">{{ wordCount }} 字</span>
              </div>
            </div>

            <!-- 封面图上传 -->
            <div class="rounded-lg border-2 border-dashed overflow-hidden" style="border-color: var(--theme-border);">
              <div v-if="coverImage" class="relative group">
                <img :src="coverImage" alt="Cover" class="w-full h-40 sm:h-48 object-cover" />
                <button
                    @click="coverImage = ''"
                    class="absolute top-2 right-2 w-8 h-8 bg-black/50 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <X class="w-4 h-4" />
                </button>
              </div>
              <div v-else
                   class="w-full py-8 sm:py-10 flex flex-col items-center justify-center cursor-pointer"
                   style="color: var(--theme-text-secondary);"
                   @click="triggerFileUpload"
                   @dragover.prevent
                   @drop.prevent="handleDrop"
              >
                <input
                    ref="fileInputRef"
                    type="file"
                    accept="image/*"
                    class="hidden"
                    @change="handleFileSelect"
                />
                <ImageIcon class="w-8 h-8 sm:w-10 sm:h-10 mb-2" />
                <p class="font-medium mb-1">上传封面图片</p>
                <p class="text-sm">点击或拖拽上传，建议尺寸 1200x630</p>
              </div>
            </div>

            <!-- 标题输入 -->
            <div>
              <input
                  v-model="title"
                  type="text"
                  placeholder="在这里输入文章标题..."
                  class="w-full text-xl sm:text-2xl lg:text-3xl font-bold bg-transparent border-none focus:outline-none focus:ring-0"
                  style="color: var(--theme-text);"
                  maxlength="80"
              />
              <div class="flex items-center justify-end mt-1">
                <span class="text-xs" :style="{ color: titleLength > 70 ? '#ef4444' : 'var(--theme-text-secondary)' }">
                  {{ titleLength }}/80
                </span>
              </div>
            </div>

            <!-- 编辑器模式切换 -->
            <div class="flex items-center gap-3">
              <div class="flex rounded-lg p-1" style="background-color: var(--theme-surface);">
                <button
                    @click="editorMode = 'richtext'"
                    class="px-3 py-1.5 rounded-md text-sm font-medium transition-colors flex items-center gap-2"
                    :style="editorMode === 'richtext' ? { backgroundColor: 'var(--theme-primary)', color: 'white' } : { color: 'var(--theme-text-secondary)' }"
                >
                  <Type class="w-4 h-4" />
                  富文本
                </button>
                <button
                    @click="editorMode = 'markdown'"
                    class="px-3 py-1.5 rounded-md text-sm font-medium transition-colors flex items-center gap-2"
                    :style="editorMode === 'markdown' ? { backgroundColor: 'var(--theme-primary)', color: 'white' } : { color: 'var(--theme-text-secondary)' }"
                >
                  <Code class="w-4 h-4" />
                  Markdown
                </button>
              </div>
            </div>

            <!-- 内容编辑器和摘要区 -->
            <div class="space-y-3">
              <!-- 内容编辑器 -->
              <div>
                <!-- 预览模式 -->
                <div v-if="showPreview" class="rounded-lg border p-4 sm:p-6" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
                  <div class="flex items-center justify-between mb-4 pb-3 border-b" style="border-color: var(--theme-border);">
                    <h2 class="text-lg sm:text-xl font-bold" style="color: var(--theme-text);">{{ title || '文章标题' }}</h2>
                    <button @click="closePreview" class="p-1.5 rounded-lg hover:bg-gray-100" style="color: var(--theme-text-secondary);">
                      <X class="w-4 h-4" />
                    </button>
                  </div>
                  <div class="prose prose-lg max-w-none" style="color: var(--theme-text-secondary);">
                    <div v-if="editorMode === 'markdown'" v-html="markdownPreview"></div>
                    <div v-else v-html="content || '<p>在这里输入你的文章内容...</p>'"></div>
                  </div>
                </div>

                <!-- 富文本编辑模式 (Quill) -->
                <QuillEditor
                    v-else-if="editorMode === 'richtext'"
                    v-model="content"
                    placeholder="开始写作..."
                    theme="snow"
                />

                <!-- Markdown编辑模式 -->
                <MarkdownEditor
                    v-else
                    v-model="content"
                    placeholder="开始写作..."
                />
              </div>

              <!-- 摘要区 -->
              <div class="rounded-lg border p-3 sm:p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
                <div class="flex items-center justify-between mb-3">
                  <h3 class="font-semibold flex items-center gap-2" style="color: var(--theme-text);">
                    <BookOpen class="w-4 h-4" />
                    摘要
                  </h3>
                  <button
                      @click="extractExcerptFromContent"
                      :disabled="isExtractingExcerpt"
                      class="text-xs px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1"
                      style="color: var(--theme-primary); background-color: var(--theme-accent);"
                  >
                    <svg v-if="isExtractingExcerpt" class="animate-spin w-3 h-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <Sparkles v-else class="w-3 h-3" />
                    <span>{{ isExtractingExcerpt ? '提取中...' : '智能提取' }}</span>
                  </button>
                </div>

                <textarea
                    v-model="excerpt"
                    placeholder="文章摘要（选填，会在列表页显示，不填则自动截取内容前200字）"
                    class="w-full text-sm border-0 focus:outline-none resize-none"
                    rows="3"
                    maxlength="200"
                    style="background-color: transparent; color: var(--theme-text-secondary);"
                ></textarea>
                <div class="flex justify-between items-center mt-1">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">
                    {{ excerpt.length }}/200
                  </span>
                  <span v-if="excerpt.length === 0" class="text-xs text-blue-500">
                    💡 点击「智能提取」快速生成摘要
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧边栏 - 分类、标签和高级选项 -->
          <div class="lg:col-span-1 space-y-4">
            <!-- 分类选择 - 二级联动 -->
            <div class="rounded-lg border p-3 sm:p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <h3 class="font-semibold mb-3 flex items-center gap-2" style="color: var(--theme-text);">
                <List class="w-4 h-4" />
                分类
              </h3>

              <div class="space-y-3">
                <!-- 一级分类 -->
                <div>
                  <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">一级分类</label>
                  <select
                      v-model="selectedParentCategory"
                      @change="selectedChildCategory = ''"
                      class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                      style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                  >
                    <option value="">请选择一级分类</option>
                    <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
                  </select>
                </div>

                <!-- 二级分类 - 只有选择了一级分类后才显示 -->
                <div v-if="selectedParentCategory && childCategories.length > 0">
                  <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">二级分类</label>
                  <select
                      v-model="selectedChildCategory"
                      class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                      style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                  >
                    <option value="">请选择二级分类（可选）</option>
                    <option v-for="child in childCategories" :key="child.id" :value="child.id">{{ child.name }}</option>
                  </select>
                </div>

                <div v-if="loadingCategories" class="text-xs" style="color: var(--theme-text-secondary);">
                  加载中...
                </div>
              </div>
            </div>

            <!-- 标签 -->
            <div class="rounded-lg border p-3 sm:p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <h3 class="font-semibold mb-3 flex items-center gap-2" style="color: var(--theme-text);">
                <TagIcon class="w-4 h-4" />
                标签
                <span v-if="tags.length > 0" class="text-xs font-normal" style="color: var(--theme-text-secondary);">
                  ({{ tags.length }}/10)
                </span>
              </h3>

              <!-- 已选标签 -->
              <div v-if="tags.length > 0" class="flex flex-wrap gap-1.5 mb-3">
                <span
                    v-for="tag in tags"
                    :key="tag"
                    class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium"
                    style="background-color: var(--theme-accent); color: var(--theme-primary);"
                >
                  {{ tag }}
                  <button @click="removeTag(tag)" class="hover:text-red-500">
                    <X class="w-3 h-3" />
                  </button>
                </span>
              </div>

              <!-- 标签搜索输入框 -->
              <div class="relative mb-3">
                <input
                    v-model="tagInput"
                    @input="searchForTags(tagInput)"
                    @focus="showTagSuggestions = true"
                    @keydown.enter.prevent="handleTagInputEnter"
                    type="text"
                    placeholder="搜索或输入标签..."
                    class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2 pr-8"
                    style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                />
                <div v-if="isSearchingTags" class="absolute right-2 top-1/2 transform -translate-y-1/2">
                  <div class="w-4 h-4 border-2 border-t-transparent rounded-full animate-spin" style="border-color: var(--theme-primary);"></div>
                </div>
              </div>

              <!-- 标签搜索结果 / 建议标签 -->
              <div v-if="showTagSuggestions" class="mb-3 max-h-[250px] overflow-y-auto">
                <!-- 搜索结果 -->
                <div v-if="tagSearchResults.length > 0" class="mb-3">
                  <h4 class="text-xs font-medium mb-1.5" style="color: var(--theme-text-secondary);">搜索结果</h4>
                  <div class="flex flex-wrap gap-1.5">
                    <button
                        v-for="tag in tagSearchResults"
                        :key="tag.id"
                        @click="addTagFromSuggestion(tag.name)"
                        :disabled="tags.includes(tag.name)"
                        class="px-2.5 py-0.5 text-xs rounded-full transition-colors"
                        :style="tags.includes(tag.name)
                        ? 'background-color: var(--theme-accent); color: var(--theme-text-secondary); cursor: not-allowed;' 
                        : 'background-color: var(--theme-accent); color: var(--theme-primary); cursor: pointer;'"
                    >
                      {{ tag.name }}
                    </button>
                  </div>
                </div>

                <!-- 智能建议 -->
                <div v-if="tagSuggestions.length > 0" class="mb-3">
                  <h4 class="text-xs font-medium mb-1.5 flex items-center gap-1" style="color: var(--theme-text-secondary);">
                    <Sparkles class="w-3 h-3" />
                    智能推荐
                  </h4>
                  <div class="flex flex-wrap gap-1.5">
                    <button
                        v-for="tag in tagSuggestions"
                        :key="tag.id"
                        @click="addTagFromSuggestion(tag.name)"
                        :disabled="tags.includes(tag.name)"
                        class="px-2.5 py-0.5 text-xs rounded-full transition-colors"
                        :style="tags.includes(tag.name)
                        ? 'background-color: var(--theme-accent); color: var(--theme-text-secondary); cursor: not-allowed;' 
                        : 'background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; cursor: pointer;'"
                    >
                      {{ tag.name }}
                    </button>
                  </div>
                </div>

                <!-- 热门标签 -->
                <div v-if="hotTags.length > 0" class="mb-3">
                  <h4 class="text-xs font-medium mb-1.5" style="color: var(--theme-text-secondary);">热门标签</h4>
                  <div class="flex flex-wrap gap-1.5">
                    <button
                        v-for="tag in hotTags"
                        :key="tag.id"
                        @click="addTagFromSuggestion(tag.name)"
                        :disabled="tags.includes(tag.name)"
                        class="px-2.5 py-0.5 text-xs rounded-full transition-colors"
                        :style="tags.includes(tag.name)
                        ? 'background-color: var(--theme-accent); color: var(--theme-text-secondary); cursor: not-allowed;' 
                        : 'background-color: var(--theme-accent); color: var(--theme-text-secondary); cursor: pointer;'"
                    >
                      {{ tag.name }}
                    </button>
                  </div>
                </div>

                <!-- 自定义创建标签 -->
                <div v-if="tagInput.trim() && !tagSearchResults.some(t => t.name.toLowerCase() === tagInput.toLowerCase())" class="mt-2.5 pt-2.5 border-t" style="border-color: var(--theme-border);">
                  <button
                      @click="createAndAddTag(tagInput)"
                      class="flex items-center gap-1 text-xs text-primary-600 hover:text-primary-700"
                      style="color: var(--theme-primary);"
                  >
                    <Plus class="w-3 h-3" />
                    创建新标签: "{{ tagInput.trim() }}"
                  </button>
                </div>
              </div>

              <!-- 收起建议按钮 -->
              <button
                  v-if="showTagSuggestions"
                  @click="showTagSuggestions = false"
                  class="text-xs w-full text-center py-1.5 hover:bg-gray-50 rounded-lg"
                  style="color: var(--theme-text-secondary);"
              >
                收起建议
              </button>
            </div>

            <!-- 高级选项区 - 移到这里 -->
            <div class="rounded-lg border overflow-hidden" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <button
                  @click="showAdvanced = !showAdvanced"
                  class="w-full px-3 sm:px-4 py-3 flex items-center justify-between"
                  style="color: var(--theme-text);"
              >
                <span class="font-semibold flex items-center gap-2">
                  <Settings class="w-4 h-4" />
                  高级选项
                </span>
                <ChevronDown
                    class="w-5 h-5 transition-transform"
                    :class="{ 'rotate-180': showAdvanced }"
                    style="color: var(--theme-text-secondary);"
                />
              </button>

              <div v-if="showAdvanced" class="px-3 sm:px-4 pb-4 space-y-3">
                <!-- SEO设置 -->
                <div class="border-t pt-3" style="border-color: var(--theme-border);">
                  <button
                      @click="showSeoSettings = !showSeoSettings"
                      class="flex items-center justify-between w-full mb-2"
                  >
                    <span class="font-medium text-sm" style="color: var(--theme-text);">SEO 设置</span>
                    <ChevronRight class="w-4 h-4" :class="{ 'rotate-90': showSeoSettings }" style="color: var(--theme-text-secondary);" />
                  </button>
                  <div v-if="showSeoSettings" class="space-y-2 pl-2">
                    <div>
                      <label class="block text-xs mb-1" style="color: var(--theme-text-secondary);">SEO 标题</label>
                      <input
                          v-model="seoTitle"
                          type="text"
                          placeholder="不填则使用文章标题"
                          class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                          style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                      />
                    </div>
                    <div>
                      <label class="block text-xs mb-1" style="color: var(--theme-text-secondary);">SEO 描述</label>
                      <textarea
                          v-model="seoDescription"
                          placeholder="用于搜索引擎展示，建议150字以内"
                          class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none resize-none"
                          rows="2"
                          style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                      ></textarea>
                    </div>
                    <div>
                      <label class="block text-xs mb-1" style="color: var(--theme-text-secondary);">关键词</label>
                      <input
                          v-model="seoKeywords"
                          type="text"
                          placeholder="用逗号分隔，如：春天,回忆,人生"
                          class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                          style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                      />
                    </div>
                  </div>
                </div>

                <!-- 评论设置 -->
                <div class="border-t pt-3" style="border-color: var(--theme-border);">
                  <button
                      @click="showCommentSettings = !showCommentSettings"
                      class="flex items-center justify-between w-full mb-2"
                  >
                    <span class="font-medium text-sm" style="color: var(--theme-text);">评论设置</span>
                    <ChevronRight class="w-4 h-4" :class="{ 'rotate-90': showCommentSettings }" style="color: var(--theme-text-secondary);" />
                  </button>
                  <div v-if="showCommentSettings" class="space-y-2 pl-2">
                    <label class="flex items-center gap-2 cursor-pointer">
                      <input type="checkbox" v-model="allowComments" class="w-4 h-4 rounded" />
                      <span class="text-sm" style="color: var(--theme-text);">允许评论</span>
                    </label>
                    <label class="flex items-center gap-2 cursor-pointer">
                      <input type="checkbox" v-model="commentModeration" class="w-4 h-4 rounded" />
                      <span class="text-sm" style="color: var(--theme-text);">评论需要审核</span>
                    </label>
                  </div>
                </div>

                <!-- 权限设置 -->
                <div class="border-t pt-3" style="border-color: var(--theme-border);">
                  <button
                      @click="showPermissionSettings = !showPermissionSettings"
                      class="flex items-center justify-between w-full mb-2"
                  >
                    <span class="font-medium text-sm" style="color: var(--theme-text);">权限设置</span>
                    <ChevronRight class="w-4 h-4" :class="{ 'rotate-90': showPermissionSettings }" style="color: var(--theme-text-secondary);" />
                  </button>
                  <div v-if="showPermissionSettings" class="space-y-2 pl-2">
                    <div class="flex gap-2 flex-wrap">
                      <label class="flex items-center gap-2 cursor-pointer">
                        <input type="radio" v-model="visibility" value="public" class="w-4 h-4" />
                        <Globe class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                        <span class="text-sm" style="color: var(--theme-text);">公开</span>
                      </label>
                      <label class="flex items-center gap-2 cursor-pointer">
                        <input type="radio" v-model="visibility" value="private" class="w-4 h-4" />
                        <Lock class="w-4 h-4" style="color: var(--theme-text-secondary);" />
                        <span class="text-sm" style="color: var(--theme-text);">仅自己</span>
                      </label>
                      <label class="flex items-center gap-2 cursor-pointer">
                        <input type="radio" v-model="visibility" value="password" class="w-4 h-4" />
                        <span class="text-sm" style="color: var(--theme-text);">密码保护</span>
                      </label>
                    </div>
                    <div v-if="visibility === 'password'">
                      <input
                          v-model="articlePassword"
                          type="password"
                          placeholder="请输入访问密码"
                          class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                          style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                      />
                    </div>
                  </div>
                </div>

                <!-- 自定义URL -->
                <div class="border-t pt-3" style="border-color: var(--theme-border);">
                  <label class="block text-xs mb-1" style="color: var(--theme-text-secondary);">自定义 URL Slug</label>
                  <input
                      v-model="customSlug"
                      type="text"
                      placeholder="如：my-article-title（用于 SEO 友好链接）"
                      class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                      style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部 Footer -->
    <div class="mt-8">
      <SiteFooter />
    </div>
  </div>
</template>
