<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { useRouter, RouterLink as Link } from 'vue-router';
import { 
  ArrowLeft, Image as ImageIcon, Upload, Save, Eye, Send, X, 
  Bold, Italic, List, ListOrdered, Quote, Code as CodeIcon,
  Link as LinkIcon, Clock, User, Calendar, FileText, Settings,
  Sparkles, Globe, Lock, MessageSquare, Tag as TagIcon, BookOpen, 
  ChevronDown, ChevronUp, Check, File, Video, Paperclip, 
  History, RotateCcw, Undo2, Redo2, Type, Plus, ChevronRight
} from 'lucide-vue-next';
import { getCurrentUser, addArticle } from '@/data/mockData';
import { categories, getParentCategoryNames, getSubCategories, type SubCategory } from '@/data/categories';
import { mockGetHotTags, mockSearchTags, mockCreateTag, mockGetRecommendTags } from '@/api/tag';
import type { Tag } from '@/types/api';
import SiteFooter from '@/components/SiteFooter.vue';
import QuillEditor from '@/components/QuillEditor.vue';
import { extractExcerpt } from '@/utils/excerpt';

const router = useRouter();

// 用户信息
const currentUser = ref<any>(null);

// 文件上传相关
const fileInputRef = ref<HTMLInputElement | null>(null);

// 基础信息
const title = ref('');
const content = ref('');
const tags = ref<string[]>([]);
const tagInput = ref('');
const excerpt = ref('');
const coverImage = ref('');

// 分类选择（二级联动）
const selectedParentCategory = ref('');
const selectedSubCategory = ref('');
const availableSubCategories = ref<SubCategory[]>([]);

// 监听父分类变化，更新子分类选项
watch(selectedParentCategory, (newVal) => {
  availableSubCategories.value = getSubCategories(newVal);
  selectedSubCategory.value = '';
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

// 文学平台特有功能
const creativeNotes = ref('');
const series = ref('');
const sourceQuote = ref('');
const customSlug = ref('');

// 高级选项折叠状态
const showAdvanced = ref(false);
const showSeoSettings = ref(false);
const showCommentSettings = ref(false);
const showPermissionSettings = ref(false);

// 编辑器设置
const editorMode = ref<'richtext' | 'markdown'>('richtext');
const showPreview = ref(false);
const isPublishing = ref(false);
const isSaving = ref(false);
const lastSaved = ref<string | null>(null);

// 字数统计
const wordCount = computed(() => {
  return content.value.length;
});

const readingTime = computed(() => {
  const minutes = Math.ceil(wordCount.value / 400);
  return minutes;
});

// 字符数统计
const charCount = computed(() => {
  return content.value.replace(/\s/g, '').length;
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

// 系列/专栏选项
const seriesOptions = ref([
  '《岁月如歌》',
  '《心灵独白》',
  '《时光漫步》',
  '《人间烟火》'
]);

onMounted(() => {
  currentUser.value = getCurrentUser();
  if (!currentUser.value) {
    router.push('/login');
    return;
  }
  
  authorName.value = currentUser.value.username;
  publishTime.value = new Date().toISOString().slice(0, 16);
  
  // 初始化父分类为第一个选项
  if (categories.length > 0) {
    selectedParentCategory.value = categories[0].name;
  }
  
  // 加载热门标签
  loadHotTags();
});

// 加载热门标签
async function loadHotTags() {
  try {
    const response = await mockGetHotTags(30);
    if (response.code === 200) {
      hotTags.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load hot tags:', error);
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
    const response = await mockSearchTags(keyword);
    if (response.code === 200) {
      tagSearchResults.value = response.data;
    }
  } catch (error) {
    console.error('Failed to search tags:', error);
  } finally {
    isSearchingTags.value = false;
  }
}

// 获取标签建议
async function loadTagSuggestions() {
  if (!title.value.trim() && !selectedSubCategory.value) {
    return;
  }
  
  try {
    const response = await mockGetRecommendTags(title.value, selectedSubCategory.value);
    if (response.code === 200) {
      tagSuggestions.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load tag suggestions:', error);
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
    const response = await mockCreateTag(tagName.trim());
    if (response.code === 200) {
      addTag(response.data.name);
    }
  } catch (error) {
    console.error('Failed to create tag:', error);
  }
}

// 监听标题和分类变化，更新标签建议
watch([title, selectedSubCategory], () => {
  if (title.value.trim().length > 2 || selectedSubCategory.value) {
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
function handlePublish() {
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
  if (!selectedSubCategory.value) {
    alert('请选择文章分类');
    return;
  }

  isPublishing.value = true;

  setTimeout(() => {
    const user = currentUser.value;
    if (user) {
      const finalContent = editorMode.value === 'markdown' 
        ? markdownPreview.value
        : content.value;
        
      addArticle({
        title: title.value,
        content: finalContent,
        contentMarkdown: editorMode.value === 'markdown' ? content.value : undefined,
        editorMode: editorMode.value,
        excerpt: excerpt.value || content.value.substring(0, 200) + '...',
        cover: coverImage.value || '',
        author: user,
        category: selectedSubCategory.value,
        tags: tags.value
      });
    }
    articleStatus.value = 'published';
    router.push('/');
  }, 1000);
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
  if (target.files && target.files.length > 0) {
    handleFile(target.files[0]);
  }
}

function handleDrop(event: DragEvent) {
  if (event.dataTransfer && event.dataTransfer.files.length > 0) {
    handleFile(event.dataTransfer.files[0]);
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
                  <CodeIcon class="w-4 h-4" />
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
                <textarea 
                  v-else
                  v-model="content" 
                  placeholder="开始写作...

支持的格式：
# 一级标题
## 二级标题
**加粗**
*斜体*
- 列表项"
                  class="w-full h-full min-h-[200px] text-base sm:text-lg rounded-lg border p-3 sm:p-4 resize-vertical focus:outline-none focus:ring-2 font-mono"
                  style="background-color: var(--theme-surface); color: var(--theme-text); border-color: var(--theme-border);"
                ></textarea>
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

            <!-- 高级选项区 -->
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

          <!-- 右侧边栏 -->
          <div class="lg:col-span-1 space-y-4">
            <!-- 分类选择（二级联动） -->
            <div class="rounded-lg border p-3 sm:p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <h3 class="font-semibold mb-3 flex items-center gap-2" style="color: var(--theme-text);">
                <List class="w-4 h-4" />
                分类
              </h3>
              
              <!-- 一级分类选择 -->
              <div class="mb-2.5">
                <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">一级分类</label>
                <select 
                  v-model="selectedParentCategory"
                  class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                  style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                >
                  <option v-for="cat in categories" :key="cat.id" :value="cat.name">{{ cat.name }}</option>
                </select>
              </div>
              
              <!-- 二级分类选择 -->
              <div>
                <label class="block text-xs mb-1.5" style="color: var(--theme-text-secondary);">二级分类</label>
                <select 
                  v-model="selectedSubCategory"
                  class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                  style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                >
                  <option value="">请选择二级分类</option>
                  <option v-for="sub in availableSubCategories" :key="sub.id" :value="sub.name">{{ sub.name }}</option>
                </select>
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

            <!-- 发布设置 -->
            <div class="rounded-lg border p-3 sm:p-4" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
              <h3 class="font-semibold mb-3 flex items-center gap-2" style="color: var(--theme-text);">
                <Send class="w-4 h-4" />
                发布设置
              </h3>
              <div class="space-y-3">
                <div class="flex items-center justify-between">
                  <label class="text-sm" style="color: var(--theme-text-secondary);">允许评论</label>
                  <button
                    @click="allowComments = !allowComments"
                    class="relative inline-flex h-5 w-9 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-offset-2"
                    :style="{ backgroundColor: allowComments ? 'var(--theme-primary)' : 'var(--theme-border)' }"
                  >
                    <span
                      class="pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
                      :style="{ transform: allowComments ? 'translateX(1rem)' : 'translateX(0)' }"
                    ></span>
                  </button>
                </div>
                <div>
                  <label class="block text-sm mb-2" style="color: var(--theme-text-secondary);">可见性</label>
                  <select
                    v-model="visibility"
                    class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                    style="background-color: var(--theme-bg); border-color: var(--theme-border); color: var(--theme-text);"
                  >
                    <option value="public">公开</option>
                    <option value="private">私密</option>
                  </select>
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