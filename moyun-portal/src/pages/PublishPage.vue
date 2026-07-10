<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  ArrowLeft, Image as ImageIcon, Save, Eye, Send, X,
  List, Clock, User, FileText, Settings,
  Sparkles, Globe, Lock, Tag as TagIcon, BookOpen,
  ChevronDown, Check, Type, Plus, ChevronRight, Code,
  Lightbulb, ChevronRight as ChevronRightIcon,
  History, GitCompare, RotateCcw
} from 'lucide-vue-next';
import {
  getHotTags,
  searchTagList,
  createNewTag,
  getRecommendTags
} from '@/api/tag';
import { getCategoryTree } from '@/api/category';
import { publishArticle, saveDraft as saveDraftApi, getArticleDetail } from '@/api/article';
import { uploadPortalFile, deletePortalFile } from '@/api/file';
import { getTodayPrompt } from '@/api/prompt';
import type { WritingPromptVO } from '@/api/prompt';
import {
  getArticleVersions,
  getArticleVersionDetail,
  rollbackArticleVersion,
  diffArticleVersions
} from '@/api/articleVersion';
import type { ArticleVersionItem, ArticleVersionDetail, VersionDiffResult } from '@/api/articleVersion';
import { useUserStore } from '@/stores/user';
import { useAuth } from '@/composables/useAuth';
import { useToast } from '@/composables/useToast';
import { useConfirmModal } from '@/composables/useConfirmModal';
import type { Tag, Category } from '@/types/api';
import SiteFooter from '@/components/SiteFooter.vue';
import QuillEditor from '@/components/QuillEditor.vue';
import MarkdownEditor from '@/components/MarkdownEditor.vue';
import { extractExcerpt } from '@/utils/excerpt';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const { requireAuth } = useAuth();
const toast = useToast();
const confirmModal = useConfirmModal();

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
  return (parent as any)?.children || [];
});

// 元信息
const articleStatus = ref<'draft' | 'published' | 'pending'>('draft');
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

// 今日写作 prompt（页面顶部提示卡片）
const todayPrompt = ref<WritingPromptVO | null>(null);
const promptExpanded = ref(false);
async function loadTodayPrompt() {
  try {
    const res = await getTodayPrompt();
    if (res.code === 200 && res.data) {
      todayPrompt.value = res.data;
    }
  } catch (err) {
    // 静默失败，不影响发布页主流程
    console.warn('加载今日 prompt 失败:', err);
  }
}
function togglePrompt() {
  promptExpanded.value = !promptExpanded.value;
}
function applyPromptAsTitle() {
  if (!todayPrompt.value) return;
  if (!title.value.trim()) {
    title.value = todayPrompt.value.title;
  }
}

// 高级选项显示状态
const showAdvanced = ref(false);
const showSeoSettings = ref(false);
const showCommentSettings = ref(false);
const showPermissionSettings = ref(false);

// 草稿ID（保存后记录，后续保存为更新）
const draftId = ref<string | number | null>(null);

// 编辑会话标识（一次编辑会话唯一，草稿/发布共用同一 token，后端按 token 幂等去重）
// 双重保障：1) 保存后回填 draftId 走更新；2) sessionToken 兜底，即使 draftId 丢失也只更新不新建
const generateSessionToken = () => {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
  } catch (e) { /* ignore */ }
  // 降级：时间戳 + 随机数
  return `sess-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
};
const sessionToken = ref<string>(generateSessionToken());

// ============ 版本历史 ============
// 抽屉显隐
const showVersionDrawer = ref(false);
// 版本列表
const versionList = ref<ArticleVersionItem[]>([]);
const loadingVersions = ref(false);
// 选中版本详情
const selectedVersion = ref<ArticleVersionDetail | null>(null);
const loadingVersionDetail = ref(false);
// 对比结果
const diffResult = ref<VersionDiffResult | null>(null);
const loadingDiff = ref(false);
// 对比选中的两个版本号（版本列表里勾选）
const diffV1 = ref<number | null>(null);
const diffV2 = ref<number | null>(null);
// 回滚中
const rollingBack = ref(false);

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

  // 加载分类（await 确保编辑模式回填分类时数据已就绪）
  await loadCategories();

  // 加载热门标签
  loadHotTags();

  // 加载今日写作 prompt（不阻塞主流程）
  loadTodayPrompt();

  // 编辑模式：如果 query.id 存在，加载已有文章
  const editId = route.query.id as string;
  if (editId) {
    await loadArticleForEdit(editId);
  }

  // 注：已移除草稿自动保存。为避免用户中途放弃时产生难以清理的脏数据，
  // 草稿仅在用户手动点击「保存草稿」或「发布」时才入库。
});

// 加载已有文章用于编辑
async function loadArticleForEdit(id: string) {
  try {
    const response = await getArticleDetail({ id });
    if (response.code === 200 && response.data) {
      const article = response.data as any;
      title.value = article.title || '';
      content.value = article.contentMarkdown || article.content || '';
      excerpt.value = article.excerpt || '';
      coverImage.value = article.cover || '';
      tags.value = article.tagNames || article.tags || [];
      draftId.value = article.id ? Number(article.id) : null;
      articleStatus.value = article.status || 'draft';
      // 回填会话标识：编辑已有文章时沿用其 sessionToken，保证后续保存/发布只更新不新建
      if (article.sessionToken) {
        sessionToken.value = article.sessionToken;
      }

      // 回填分类
      if (article.categoryId) {
        const categoryId = String(article.categoryId);
        // 尝试匹配一级分类
        const parentCat = categories.value.find(c => c.id === categoryId);
        if (parentCat) {
          selectedParentCategory.value = categoryId;
        } else {
          // 可能是二级分类，查找其父级
          for (const parent of categories.value) {
            const child = (parent.children || []).find((ch: any) => ch.id === categoryId);
            if (child) {
              selectedParentCategory.value = parent.id;
              selectedChildCategory.value = categoryId;
              break;
            }
          }
        }
      }

      // 回填编辑器模式
      if (article.editorMode) {
        editorMode.value = article.editorMode;
      }
    }
  } catch (error) {
    console.error('加载文章失败:', error);
    toast.error('加载文章失败，请重试');
  }
}

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
    const response = await searchTagList(keyword);
    if (response.code === 200 && response.data) {
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
  if (!title.value.trim() && !selectedParentCategory.value) {
    return;
  }

  try {
    const response = await getRecommendTags(title.value, selectedParentCategory.value);
    if (response.code === 200 && response.data) {
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
    const response = await createNewTag(tagName.trim());
    if (response.code === 200 && response.data) {
      addTag((response.data as any).name || tagName.trim());
    } else {
      addTag(tagName.trim());
    }
  } catch (error) {
    console.error('Failed to create tag:', error);
    // 直接添加标签名
    addTag(tagName.trim());
  }
}

// 标签推荐的防抖计时器（避免每输入一个字符就发请求）
let tagSuggestionTimer: ReturnType<typeof setTimeout> | null = null;

function scheduleTagSuggestions() {
  // 提高触发门槛：标题至少 5 字符，或已选择分类
  if (title.value.trim().length < 5 && !selectedParentCategory.value) {
    return;
  }
  if (tagSuggestionTimer) {
    clearTimeout(tagSuggestionTimer);
  }
  tagSuggestionTimer = setTimeout(() => {
    loadTagSuggestions();
    tagSuggestionTimer = null;
  }, 500);
}

// 监听标题和分类变化，更新标签建议（500ms 防抖）
watch([title, selectedParentCategory], () => {
  scheduleTagSuggestions();
});

onUnmounted(() => {
  if (tagSuggestionTimer) {
    clearTimeout(tagSuggestionTimer);
    tagSuggestionTimer = null;
  }
});

// 添加标签
function addTag(tag: string) {
  const trimmedTag = tag.trim();
  if (trimmedTag && !tags.value.includes(trimmedTag)) {
    if (tags.value.length >= 10) {
      toast.warning('最多只能添加10个标签');
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

// 保存草稿（真实入库，返回草稿详情含 id）
async function saveDraft(isAuto = false) {
  // 只读模式下不允许保存
  if (isReadOnly.value) return;
  // 草稿允许标题/内容为空（用户可能只想保存一个标题占位）
  if (!title.value.trim() && !content.value.trim()) {
    if (!isAuto) toast.warning('请至少输入标题或内容');
    return;
  }

  if (isAuto) {
    isSaving.value = true;
  }

  try {
    const response = await saveDraftApi({
      id: draftId.value != null ? String(draftId.value) : undefined,
      sessionToken: sessionToken.value,
      title: title.value,
      content: content.value,
      contentMarkdown: editorMode.value === 'markdown' ? content.value : undefined,
      editorMode: editorMode.value,
      excerpt: excerpt.value,
      cover: coverImage.value || '',
      categoryId: selectedCategory.value ? String(selectedCategory.value) : undefined,
      tagNames: tags.value,
    });

    if (response.code === 200 && response.data) {
      // 记录草稿 id，后续保存为更新
      draftId.value = (response.data as any).id;
      articleStatus.value = 'draft';
      lastSaved.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

      if (!isAuto) {
        toast.success('草稿已保存');
      }
    } else {
      // 后端 code=200 但 data 为 null，说明业务异常被包装成协议成功，不信任 response.message（默认"操作成功"会误导）
      if (!isAuto) toast.error('草稿保存失败，请稍后重试');
    }
  } catch (error: any) {
    console.error('Failed to save draft:', error);
    if (!isAuto) {
      const errorMessage = error?.message || '草稿保存失败，请稍后重试';
      if (errorMessage.includes('登录') || errorMessage.includes('认证') || errorMessage.includes('401')) {
        const ok = await confirmModal.confirm('您还没有登录，是否前往登录？', {
          title: '需要登录',
          confirmText: '前往登录',
        });
        if (ok) {
          router.push('/login');
        }
      } else {
        toast.error(errorMessage);
      }
    }
  } finally {
    isSaving.value = false;
  }
}

// 预览文章
function previewArticle() {
  showPreview.value = true;
}

// 关闭预览
function closePreview() {
  showPreview.value = false;
}

// 发布文章（发布后进入待审核状态）
async function handlePublish() {
  if (isReadOnly.value) {
    toast.warning(readOnlyReason.value || '当前不可发布');
    return;
  }
  if (!title.value.trim()) {
    toast.warning('请输入文章标题');
    return;
  }
  if (title.value.length < 4) {
    toast.warning('标题至少4个字符');
    return;
  }
  if (!content.value.trim()) {
    toast.warning('请输入文章内容');
    return;
  }
  if (content.value.length < 50) {
    toast.warning('内容至少50个字符');
    return;
  }
  if (!selectedCategory.value) {
    toast.warning('请选择文章分类');
    return;
  }

  isPublishing.value = true;

  try {
    const finalContent = editorMode.value === 'markdown'
        ? markdownPreview.value
        : content.value;

    const response = await publishArticle({
      id: draftId.value != null ? String(draftId.value) : undefined,
      sessionToken: sessionToken.value,
      title: title.value,
      content: finalContent,
      contentMarkdown: editorMode.value === 'markdown' ? content.value : undefined,
      excerpt: excerpt.value || content.value.substring(0, 200) + '...',
      cover: coverImage.value || '',
      categoryId: selectedCategory.value,
      tagNames: tags.value,
      status: 'published', // 前端标记意图为发布，后端会转为 pending 待审核
      // 同步编辑器模式，详情页据此渲染内容
      editorMode: editorMode.value,
      // 用户自定义 SEO 别名（为空时后端按标题自动生成，确保非空）
      slug: customSlug.value.trim() || undefined,
    } as any);

    if (response.code === 200) {
      articleStatus.value = 'pending';
      const msg = (response.data as any)?.message || '发布成功，等待审核';
      toast.success(msg);
      router.push('/my/articles?status=pending');
    } else {
      toast.error('发布失败：' + ((response as any).message || '未知错误'));
    }
  } catch (error: any) {
    console.error('Failed to publish article:', error);
    // 显示错误信息
    const errorMessage = error?.message || '发布失败，请稍后重试';

    // 如果是未登录错误，跳转到登录页
    if (errorMessage.includes('登录') || errorMessage.includes('认证') || errorMessage.includes('401')) {
      const ok = await confirmModal.confirm('您还没有登录，是否前往登录？', {
        title: '需要登录',
        confirmText: '前往登录',
      });
      if (ok) {
        router.push('/login');
      }
    } else {
      toast.error(errorMessage);
    }
  } finally {
    isPublishing.value = false;
  }
}

// 返回列表
// 返回上一页（无历史时回退到首页）
function goBackToPrev() {
  if (window.history.length > 1) {
    router.back();
  } else {
    router.push('/');
  }
}

async function goBack() {
  // 只读模式（查看 / 审核中）下内容来自已加载文章，直接离开不弹"未保存"提示
  if (isReadOnly.value) {
    goBackToPrev();
    return;
  }
  if (title.value.trim() || content.value.trim()) {
    const ok = await confirmModal.confirm('有未保存的内容，确定要离开吗？', {
      title: '离开页面',
      confirmText: '离开',
      danger: true,
    });
    if (ok) {
      goBackToPrev();
    }
  } else {
    goBackToPrev();
  }
}

// ============ 版本历史相关函数 ============

// 打开版本历史抽屉
async function openVersionDrawer() {
  if (!draftId.value) {
    toast.warning('请先保存草稿，生成版本记录');
    return;
  }
  showVersionDrawer.value = true;
  // 重置状态
  selectedVersion.value = null;
  diffResult.value = null;
  diffV1.value = null;
  diffV2.value = null;
  await loadVersions();
}

// 加载版本列表
async function loadVersions() {
  if (!draftId.value) return;
  loadingVersions.value = true;
  try {
    const response = await getArticleVersions(draftId.value);
    versionList.value = response.code === 200 ? (response.data || []) : [];
  } catch (error: any) {
    console.error('加载版本列表失败:', error);
    toast.error(error?.message || '加载版本列表失败');
    versionList.value = [];
  } finally {
    loadingVersions.value = false;
  }
}

// 查看版本详情
async function viewVersionDetail(version: ArticleVersionItem) {
  loadingVersionDetail.value = true;
  selectedVersion.value = null;
  diffResult.value = null;
  try {
    const response = await getArticleVersionDetail(version.id);
    if (response.code === 200 && response.data) {
      selectedVersion.value = response.data;
    } else {
      toast.error(response.message || '加载版本详情失败');
    }
  } catch (error: any) {
    console.error('加载版本详情失败:', error);
    toast.error(error?.message || '加载版本详情失败');
  } finally {
    loadingVersionDetail.value = false;
  }
}

// 回滚到指定版本
async function handleRollback(version: ArticleVersionItem) {
  if (!draftId.value) return;
  const ok = await confirmModal.confirm(
    `确定要回滚到「版本 ${version.versionNo}」吗？当前编辑器内容将被该版本覆盖，并生成一个新的版本快照。`,
    {
      title: '回滚确认',
      confirmText: '确认回滚',
      danger: true,
    }
  );
  if (!ok) return;

  rollingBack.value = true;
  try {
    const response = await rollbackArticleVersion(draftId.value, version.id);
    if (response.code === 200 && response.data) {
      const data = response.data;
      // 用回滚后的内容覆盖编辑器
      title.value = data.title || title.value;
      if (data.contentMarkdown) {
        content.value = data.contentMarkdown;
      } else if (data.content) {
        content.value = data.content;
      }
      if (data.excerpt !== undefined && data.excerpt !== null) {
        excerpt.value = data.excerpt;
      }
      toast.success(`已回滚到版本 ${version.versionNo}`);
      // 关闭详情，刷新版本列表
      selectedVersion.value = null;
      await loadVersions();
    } else {
      toast.warning(response.message || '回滚未生效，请稍后重试');
    }
  } catch (error: any) {
    console.error('回滚失败:', error);
    toast.error(error?.message || '回滚失败，请稍后重试');
  } finally {
    rollingBack.value = false;
  }
}

// 切换对比选中（最多选两个）
function toggleDiffPick(versionNo: number) {
  if (diffV1.value === versionNo) {
    diffV1.value = null;
  } else if (diffV2.value === versionNo) {
    diffV2.value = null;
  } else if (diffV1.value === null) {
    diffV1.value = versionNo;
  } else if (diffV2.value === null) {
    diffV2.value = versionNo;
  } else {
    // 都已选，替换第二个
    diffV2.value = versionNo;
  }
}

// 执行对比
async function handleDiff() {
  if (!draftId.value) {
    toast.warning('缺少文章 ID');
    return;
  }
  if (diffV1.value === null || diffV2.value === null) {
    toast.warning('请选择两个版本进行对比');
    return;
  }
  if (diffV1.value === diffV2.value) {
    toast.warning('请选择两个不同的版本');
    return;
  }

  loadingDiff.value = true;
  diffResult.value = null;
  selectedVersion.value = null;
  try {
    const response = await diffArticleVersions(draftId.value, diffV1.value, diffV2.value);
    if (response.code === 200 && response.data) {
      diffResult.value = response.data;
    } else {
      toast.error(response.message || '版本对比失败');
    }
  } catch (error: any) {
    console.error('版本对比失败:', error);
    toast.error(error?.message || '版本对比失败');
  } finally {
    loadingDiff.value = false;
  }
}

// 关闭抽屉
function closeVersionDrawer() {
  showVersionDrawer.value = false;
  selectedVersion.value = null;
  diffResult.value = null;
  diffV1.value = null;
  diffV2.value = null;
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
    toast.error('摘要提取失败，请重试');
  } finally {
    isExtractingExcerpt.value = false;
  }
}

// 文件上传相关状态
const isUploadingCover = ref(false);

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

// 删除封面：二次确认后调后端清理（仅 http URL 入库过需删；Base64 未入库直接清空）
async function removeCover() {
  if (!coverImage.value) return;
  const ok = window.confirm('删除后将永久清除该封面的存储与记录，且无法恢复，是否确认？');
  if (!ok) return;
  const oldCover = coverImage.value;
  coverImage.value = '';
  if (/^https?:\/\//.test(oldCover)) {
    try {
      await deletePortalFile(oldCover);
    } catch (e) {
      toast.error('文件记录清理失败，请稍后在文件管理中处理');
      console.warn('封面清理失败：', e);
    }
  }
}

async function handleFile(file: File) {
  // 检查文件类型（仅允许常见图片格式）
  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
  if (!allowedTypes.includes(file.type)) {
    toast.warning('仅支持 JPG / PNG / WebP / GIF 格式的图片！');
    return;
  }

  // 检查文件大小 (最大 500KB，方正封面无需大图)
  if (file.size > 500 * 1024) {
    toast.warning('文件大小不能超过 500KB！');
    return;
  }

  // 替换场景（封面已有 http URL）：先记录旧封面，新封面上传成功后再删旧，
  // 上传失败则恢复旧封面，保证不丢失。Base64/blob 预览未入库无需删。
  // 与「删除按钮」区分：删除按钮直接删；替换按钮先传新再删旧，兼容"不丢失"诉求。
  const oldCover = coverImage.value && /^https?:\/\//.test(coverImage.value)
    ? coverImage.value
    : null;

  // 本地预览（同步 API，避免 FileReader 异步回调与上传结果产生的时序竞争）
  const objectUrl = URL.createObjectURL(file);
  coverImage.value = objectUrl;

  // 上传到文件服务
  isUploadingCover.value = true;
  try {
    const response = await uploadPortalFile(file, 'article_cover');
    if (response.code === 200 && response.data) {
      // 上传成功，替换为服务器URL
      coverImage.value = (response.data as any).fileUrl || coverImage.value;
      // 新封面上传成功后，删除旧封面（DB+存储），失败仅警告不影响新封面
      if (oldCover) {
        try {
          await deletePortalFile(oldCover);
        } catch (e) {
          console.warn('旧封面清理失败：', e);
        }
      }
    } else {
      // 上传失败：恢复旧封面（替换语义——不丢失原封面），用户可重试或改用「删除」
      coverImage.value = oldCover || '';
      toast.error('封面上传失败，请重试');
    }
  } catch (error) {
    console.error('封面上传失败:', error);
    coverImage.value = oldCover || '';
    toast.error('封面上传失败，请重试');
  } finally {
    // 释放本地预览 blob URL（成功时已被 fileUrl 覆盖，失败时已恢复为 oldCover）
    URL.revokeObjectURL(objectUrl);
    isUploadingCover.value = false;
  }
}

// 计算标题字数
const titleLength = computed(() => title.value.length);

// 只读模式：查看模式（mode=view）或审核中（pending）时，禁止编辑与发布
const isReadOnly = computed(() => {
  return route.query.mode === 'view' || articleStatus.value === 'pending';
});
const readOnlyReason = computed(() => {
  if (route.query.mode === 'view') {
    return '当前为查看模式，内容不可编辑';
  }
  if (articleStatus.value === 'pending') {
    return '文章正在审核中，暂不可编辑或发布';
  }
  return '';
});
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
              <span class="text-lg font-semibold" style="color: var(--theme-text);">{{ isReadOnly ? '查看文章' : '写文章' }}</span>
              <!-- 状态标签 -->
              <span
                  class="px-2.5 py-1 rounded-full text-xs font-medium"
                  :style="{
                  backgroundColor: articleStatus === 'draft' ? 'var(--theme-accent)' :
                                   articleStatus === 'pending' ? '#fef3c7' : '#d1fae5',
                  color: articleStatus === 'draft' ? 'var(--theme-primary)' :
                         articleStatus === 'pending' ? '#92400e' : '#065f46'
                }"
              >
                {{ articleStatus === 'draft' ? '草稿' : articleStatus === 'pending' ? '审核中' : '已发布' }}
              </span>
            </div>
          </div>

          <!-- 右侧操作按钮 -->
          <div class="flex items-center gap-3">
            <!-- 保存提示（手动保存后显示最近保存时间） -->
            <div v-if="lastSaved" class="hidden sm:flex items-center gap-1.5 text-xs" style="color: var(--theme-text-secondary);">
              <Check class="w-3.5 h-3.5 text-green-500" />
              <span>已保存于 {{ lastSaved }}</span>
            </div>

            <!-- 保存草稿（只读模式下隐藏） -->
            <button
                v-if="!isReadOnly"
                @click="saveDraft(false)"
                :disabled="isSaving"
                class="px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2"
                style="color: var(--theme-text); border: 1px solid var(--theme-border);"
            >
              <Save class="w-4 h-4" />
              <span class="hidden sm:inline">保存草稿</span>
            </button>

            <!-- 版本历史（仅草稿模式显示） -->
            <button
                v-if="draftId && !isReadOnly"
                @click="openVersionDrawer"
                class="px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2"
                style="color: var(--theme-text-secondary); border: 1px solid var(--theme-border);"
                title="查看与回滚历史版本"
            >
              <History class="w-4 h-4" />
              <span class="hidden sm:inline">版本历史</span>
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

            <!-- 发布（只读模式下隐藏） -->
            <button
                v-if="!isReadOnly"
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
        <!-- 只读模式提示横幅 -->
        <div v-if="isReadOnly" class="mb-4 rounded-lg border px-4 py-3 flex items-center gap-2" style="background-color: #fef3c7; border-color: #fde68a;">
          <svg class="w-5 h-5 flex-shrink-0" style="color: #92400e;" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01M5 19h14a2 2 0 001.7-3l-7-12a2 2 0 00-3.4 0l-7 12A2 2 0 005 19z" />
          </svg>
          <span class="text-sm font-medium" style="color: #92400e;">{{ readOnlyReason }}</span>
        </div>
        <div class="grid grid-cols-1 lg:grid-cols-4 gap-4">
          <!-- 左侧主编辑区 -->
          <div class="lg:col-span-3 space-y-4">
            <!-- 今日写作 prompt 提示卡片（只读模式下隐藏） -->
            <div
              v-if="todayPrompt && !isReadOnly"
              class="rounded-lg overflow-hidden"
              style="background: linear-gradient(135deg, color-mix(in srgb, var(--theme-primary) 8%, var(--theme-surface)), var(--theme-surface)); border: 1px solid color-mix(in srgb, var(--theme-primary) 30%, var(--theme-border));"
            >
              <div
                class="px-4 py-3 flex items-center justify-between cursor-pointer"
                @click="togglePrompt"
              >
                <div class="flex items-center gap-2 min-w-0">
                  <Lightbulb class="w-4 h-4 flex-shrink-0" style="color: var(--theme-primary);" />
                  <span class="text-sm font-medium truncate" style="color: var(--theme-text);">
                    今日写作 Prompt：{{ todayPrompt.title }}
                  </span>
                  <span
                    v-if="todayPrompt.category"
                    class="px-1.5 py-0.5 rounded text-xs flex-shrink-0"
                    style="background-color: color-mix(in srgb, var(--theme-primary) 12%, transparent); color: var(--theme-primary);"
                  >
                    {{ todayPrompt.category }}
                  </span>
                </div>
                <ChevronRightIcon
                  class="w-4 h-4 flex-shrink-0 transition-transform"
                  :class="{ 'rotate-90': promptExpanded }"
                  style="color: var(--theme-text-secondary);"
                />
              </div>
              <div v-if="promptExpanded" class="px-4 pb-3 pt-1 border-t" style="border-color: color-mix(in srgb, var(--theme-primary) 20%, var(--theme-border));">
                <p v-if="todayPrompt.description" class="text-sm mt-2 mb-3 whitespace-pre-wrap" style="color: var(--theme-text);">
                  {{ todayPrompt.description }}
                </p>
                <div class="flex items-center gap-2 flex-wrap">
                  <button
                    @click="applyPromptAsTitle"
                    class="inline-flex items-center px-3 py-1.5 rounded-md text-xs font-medium text-white transition hover:opacity-90"
                    style="background-color: var(--theme-primary);"
                  >
                    <Sparkles class="w-3.5 h-3.5 mr-1" />
                    用它作为标题
                  </button>
                  <span v-if="todayPrompt.promptDate" class="text-xs" style="color: var(--theme-text-secondary);">
                    {{ todayPrompt.promptDate }}
                  </span>
                </div>
              </div>
            </div>
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
                <!-- 方正显示：使用 aspect-square 强制 1:1 比例，限制最大宽度 -->
                <div class="w-full max-w-xs aspect-square mx-auto">
                  <img :src="coverImage" alt="Cover" class="w-full h-full object-cover" />
                </div>
                <div v-if="isUploadingCover" class="absolute inset-0 bg-black/50 flex items-center justify-center">
                  <div class="flex flex-col items-center gap-2">
                    <div class="w-6 h-6 border-2 border-white/50 border-t-white rounded-full animate-spin"></div>
                    <span class="text-white text-sm">上传中...</span>
                  </div>
                </div>
                <!-- 封面操作：替换（先传新再删旧）+ 删除（直接删） -->
                <div v-else-if="!isReadOnly"
                    class="absolute top-2 right-2 flex gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <button type="button" @click="triggerFileUpload"
                      class="w-8 h-8 bg-black/50 text-white rounded-full flex items-center justify-center hover:bg-black/70"
                      title="替换封面（上传新封面成功后删除旧封面）"
                  >
                    <RotateCcw class="w-4 h-4" />
                  </button>
                  <button type="button" @click="removeCover"
                      class="w-8 h-8 bg-black/50 text-white rounded-full flex items-center justify-center hover:bg-red-600/80"
                      title="删除封面"
                  >
                    <X class="w-4 h-4" />
                  </button>
                </div>
              </div>
              <div v-else
                   class="w-full py-8 sm:py-10 flex flex-col items-center justify-center"
                   :class="{ 'cursor-pointer': !isReadOnly, 'opacity-60': isReadOnly }"
                   style="color: var(--theme-text-secondary);"
                   @click="!isReadOnly && triggerFileUpload()"
                   @dragover.prevent
                   @drop.prevent="!isReadOnly && handleDrop($event)"
              >
                <input
                    ref="fileInputRef"
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    class="hidden"
                    @change="handleFileSelect"
                />
                <ImageIcon class="w-8 h-8 sm:w-10 sm:h-10 mb-2" />
                <p class="font-medium mb-1">上传封面图片</p>
                <p class="text-sm">点击或拖拽上传，建议尺寸 600x600（方正显示）</p>
                <p class="text-xs mt-1" style="color: var(--theme-text-secondary);">支持 JPG / PNG / WebP / GIF，最大 500KB</p>
              </div>
            </div>

            <!-- 标题输入 -->
            <div>
              <input
                  v-model="title"
                  type="text"
                  placeholder="在这里输入文章标题..."
                  class="w-full text-xl sm:text-2xl lg:text-3xl font-bold bg-transparent border-none focus:outline-none focus:ring-0"
                  :class="{ 'cursor-default': isReadOnly }"
                  style="color: var(--theme-text);"
                  :readonly="isReadOnly"
                  maxlength="80"
              />
              <div class="flex items-center justify-end mt-1">
                <span class="text-xs" :style="{ color: titleLength > 70 ? '#ef4444' : 'var(--theme-text-secondary)' }">
                  {{ titleLength }}/80
                </span>
              </div>
            </div>

            <!-- 编辑器模式切换（只读模式下隐藏） -->
            <div v-if="!isReadOnly" class="flex items-center gap-3">
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
                <!-- 只读内容展示（查看模式 / 审核中） -->
                <div v-if="isReadOnly" class="rounded-lg border p-4 sm:p-6 min-h-[300px]" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
                  <div class="prose prose-lg max-w-none" style="color: var(--theme-text);">
                    <div v-if="editorMode === 'markdown'" v-html="markdownPreview"></div>
                    <div v-else v-html="content || '<p>暂无内容</p>'"></div>
                  </div>
                </div>

                <!-- 预览模式 -->
                <div v-else-if="showPreview" class="rounded-lg border p-4 sm:p-6" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
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
                      v-if="!isReadOnly"
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
                    :class="{ 'cursor-default': isReadOnly }"
                    rows="3"
                    maxlength="200"
                    :readonly="isReadOnly"
                    style="background-color: transparent; color: var(--theme-text-secondary);"
                ></textarea>
                <div class="flex justify-between items-center mt-1">
                  <span class="text-xs" style="color: var(--theme-text-secondary);">
                    {{ excerpt.length }}/200
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
                      :disabled="isReadOnly"
                      class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                      :class="{ 'cursor-not-allowed opacity-60': isReadOnly }"
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
                      :disabled="isReadOnly"
                      class="w-full px-3 py-2 text-sm rounded-lg border focus:outline-none focus:ring-2"
                      :class="{ 'cursor-not-allowed opacity-60': isReadOnly }"
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
                  #{{ tag }}
                  <button v-if="!isReadOnly" @click="removeTag(tag)" class="hover:text-red-500">
                    <X class="w-3 h-3" />
                  </button>
                </span>
              </div>

              <!-- 标签搜索输入框（只读模式下隐藏） -->
              <div v-if="!isReadOnly" class="relative mb-3">
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

              <!-- 标签搜索结果 / 建议标签（只读模式下隐藏） -->
              <div v-if="showTagSuggestions && !isReadOnly" class="mb-3 max-h-[250px] overflow-y-auto">
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
                      #{{ tag.name }}
                    </button>
                  </div>
                </div>

                <!-- 智能推荐 -->
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
                      #{{ tag.name }}
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
                      #{{ tag.name }}
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

              <!-- 收起建议按钮（只读模式下隐藏） -->
              <button
                  v-if="showTagSuggestions && !isReadOnly"
                  @click="showTagSuggestions = false"
                  class="text-xs w-full text-center py-1.5 hover:bg-gray-50 rounded-lg"
                  style="color: var(--theme-text-secondary);"
              >
                收起建议
              </button>
            </div>

            <!-- 高级选项区 - 移到这里（只读模式下隐藏） -->
            <div v-if="!isReadOnly" class="rounded-lg border overflow-hidden" style="background-color: var(--theme-surface); border-color: var(--theme-border);">
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

    <!-- 版本历史抽屉 -->
    <transition name="version-drawer">
      <div v-if="showVersionDrawer" class="fixed inset-0 z-50 flex" role="dialog" aria-modal="true" aria-label="版本历史">
        <!-- 遮罩 -->
        <div class="absolute inset-0 bg-black/40" @click="closeVersionDrawer"></div>
        <!-- 抽屉主体 -->
        <div class="relative ml-auto w-full max-w-2xl h-full shadow-xl flex flex-col" style="background-color: var(--theme-surface);">
          <!-- 头部 -->
          <div class="flex items-center justify-between px-5 py-4 border-b" style="border-color: var(--theme-border);">
            <div class="flex items-center gap-2">
              <History class="w-5 h-5" style="color: var(--theme-primary);" />
              <h3 class="text-lg font-semibold" style="color: var(--theme-text);">版本历史</h3>
              <span v-if="versionList.length > 0" class="text-xs px-2 py-0.5 rounded-full" style="background-color: var(--theme-accent); color: var(--theme-primary);">
                共 {{ versionList.length }} 个版本
              </span>
            </div>
            <button @click="closeVersionDrawer" class="p-1.5 rounded-lg hover:bg-gray-100" style="color: var(--theme-text-secondary);">
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- 操作提示条 -->
          <div class="px-5 py-2.5 border-b flex items-center justify-between flex-wrap gap-2" style="background-color: var(--theme-bg); border-color: var(--theme-border);">
            <div class="text-xs flex items-center gap-2" style="color: var(--theme-text-secondary);">
              <GitCompare class="w-3.5 h-3.5" />
              <span>勾选两个版本可进行对比</span>
              <span v-if="diffV1 !== null" class="px-1.5 py-0.5 rounded" style="background-color: var(--theme-accent); color: var(--theme-primary);">V{{ diffV1 }}</span>
              <span v-if="diffV2 !== null" class="px-1.5 py-0.5 rounded" style="background-color: var(--theme-accent); color: var(--theme-primary);">V{{ diffV2 }}</span>
            </div>
            <button
                @click="handleDiff"
                :disabled="diffV1 === null || diffV2 === null || loadingDiff"
                class="text-xs px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
                style="color: white; background-color: var(--theme-primary);"
            >
              <svg v-if="loadingDiff" class="animate-spin w-3 h-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <GitCompare v-else class="w-3 h-3" />
              {{ loadingDiff ? '对比中...' : '对比选中版本' }}
            </button>
          </div>

          <!-- 主体内容区：左侧版本列表 + 右侧详情/对比 -->
          <div class="flex-1 overflow-hidden flex">
            <!-- 版本列表 -->
            <div class="w-1/2 border-r overflow-y-auto" style="border-color: var(--theme-border);">
              <div v-if="loadingVersions" class="p-6 flex flex-col items-center gap-2" style="color: var(--theme-text-secondary);">
                <div class="w-5 h-5 border-2 border-t-transparent rounded-full animate-spin" style="border-color: var(--theme-primary);"></div>
                <span class="text-xs">加载版本中...</span>
              </div>

              <div v-else-if="versionList.length === 0" class="p-6 text-center" style="color: var(--theme-text-secondary);">
                <History class="w-8 h-8 mx-auto mb-2 opacity-40" />
                <p class="text-sm">暂无版本记录</p>
                <p class="text-xs mt-1">保存草稿后会自动生成版本快照</p>
              </div>

              <ul v-else class="divide-y" style="border-color: var(--theme-border);">
                <li
                    v-for="v in versionList"
                    :key="v.id"
                    class="px-4 py-3 cursor-pointer transition-colors hover:bg-gray-50"
                    :style="{
                      backgroundColor: selectedVersion?.id === v.id ? 'var(--theme-accent)' : 'transparent'
                    }"
                    @click="viewVersionDetail(v)"
                >
                  <div class="flex items-center justify-between mb-1">
                    <div class="flex items-center gap-2">
                      <input
                          type="checkbox"
                          :checked="diffV1 === v.versionNo || diffV2 === v.versionNo"
                          @click.stop="toggleDiffPick(v.versionNo)"
                          class="w-3.5 h-3.5"
                          :title="`选择 V${v.versionNo} 进行对比`"
                      />
                      <span class="text-sm font-semibold" style="color: var(--theme-primary);">版本 {{ v.versionNo }}</span>
                    </div>
                    <span class="text-xs" style="color: var(--theme-text-secondary);">{{ v.createdTime }}</span>
                  </div>
                  <p class="text-sm truncate" style="color: var(--theme-text);" :title="v.title">{{ v.title || '（无标题）' }}</p>
                  <p v-if="v.excerpt" class="text-xs mt-1 line-clamp-2" style="color: var(--theme-text-secondary);">{{ v.excerpt }}</p>
                </li>
              </ul>
            </div>

            <!-- 详情 / 对比区 -->
            <div class="w-1/2 overflow-y-auto">
              <!-- 对比结果优先展示 -->
              <div v-if="diffResult" class="p-4 space-y-4">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-semibold flex items-center gap-1.5" style="color: var(--theme-text);">
                    <GitCompare class="w-4 h-4" />
                    版本对比
                  </h4>
                  <button @click="diffResult = null" class="text-xs" style="color: var(--theme-text-secondary);">关闭对比</button>
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <!-- V1 -->
                  <div class="rounded-lg border p-3" style="border-color: var(--theme-border); background-color: var(--theme-bg);">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-xs font-semibold px-1.5 py-0.5 rounded" style="background-color: var(--theme-accent); color: var(--theme-primary);">
                        {{ diffResult.v1.found ? `V${diffResult.v1.versionNo}` : '版本不存在' }}
                      </span>
                      <span v-if="diffResult.v1.createdTime" class="text-xs" style="color: var(--theme-text-secondary);">{{ diffResult.v1.createdTime }}</span>
                    </div>
                    <p class="text-sm font-medium mb-2" style="color: var(--theme-text);">{{ diffResult.v1.title || '（无标题）' }}</p>
                    <pre v-if="diffResult.v1.contentMarkdown || diffResult.v1.content" class="text-xs whitespace-pre-wrap break-words max-h-64 overflow-y-auto p-2 rounded" style="color: var(--theme-text-secondary); background-color: var(--theme-surface);">{{ diffResult.v1.contentMarkdown || diffResult.v1.content }}</pre>
                    <p v-else class="text-xs italic" style="color: var(--theme-text-secondary);">（无内容）</p>
                  </div>
                  <!-- V2 -->
                  <div class="rounded-lg border p-3" style="border-color: var(--theme-border); background-color: var(--theme-bg);">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-xs font-semibold px-1.5 py-0.5 rounded" style="background-color: var(--theme-accent); color: var(--theme-primary);">
                        {{ diffResult.v2.found ? `V${diffResult.v2.versionNo}` : '版本不存在' }}
                      </span>
                      <span v-if="diffResult.v2.createdTime" class="text-xs" style="color: var(--theme-text-secondary);">{{ diffResult.v2.createdTime }}</span>
                    </div>
                    <p class="text-sm font-medium mb-2" style="color: var(--theme-text);">{{ diffResult.v2.title || '（无标题）' }}</p>
                    <pre v-if="diffResult.v2.contentMarkdown || diffResult.v2.content" class="text-xs whitespace-pre-wrap break-words max-h-64 overflow-y-auto p-2 rounded" style="color: var(--theme-text-secondary); background-color: var(--theme-surface);">{{ diffResult.v2.contentMarkdown || diffResult.v2.content }}</pre>
                    <p v-else class="text-xs italic" style="color: var(--theme-text-secondary);">（无内容）</p>
                  </div>
                </div>
                <p class="text-xs text-center" style="color: var(--theme-text-secondary);">
                  提示：此处仅并排展示两个版本的文本，不进行逐行差异标注。
                </p>
              </div>

              <!-- 版本详情 -->
              <div v-else-if="selectedVersion" class="p-4 space-y-3">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-semibold flex items-center gap-1.5" style="color: var(--theme-text);">
                    <FileText class="w-4 h-4" />
                    版本 {{ selectedVersion.versionNo }} 详情
                  </h4>
                  <button
                      @click="handleRollback(selectedVersion)"
                      :disabled="rollingBack"
                      class="text-xs px-3 py-1.5 rounded-lg transition-colors flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
                      style="color: white; background-color: #ef4444;"
                  >
                    <svg v-if="rollingBack" class="animate-spin w-3 h-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <RotateCcw v-else class="w-3 h-3" />
                    {{ rollingBack ? '回滚中...' : '回滚到此版本' }}
                  </button>
                </div>
                <div>
                  <label class="block text-xs mb-1" style="color: var(--theme-text-secondary);">标题</label>
                  <p class="text-sm font-medium" style="color: var(--theme-text);">{{ selectedVersion.title || '（无标题）' }}</p>
                </div>
                <div v-if="selectedVersion.excerpt">
                  <label class="block text-xs mb-1" style="color: var(--theme-text-secondary);">摘要</label>
                  <p class="text-sm" style="color: var(--theme-text-secondary);">{{ selectedVersion.excerpt }}</p>
                </div>
                <div>
                  <label class="block text-xs mb-1" style="color: var(--theme-text-secondary);">内容</label>
                  <pre class="text-xs whitespace-pre-wrap break-words max-h-96 overflow-y-auto p-3 rounded-lg border" style="color: var(--theme-text-secondary); background-color: var(--theme-bg); border-color: var(--theme-border);">{{ selectedVersion.contentMarkdown || selectedVersion.content || '（无内容）' }}</pre>
                </div>
                <div class="text-xs flex items-center gap-2" style="color: var(--theme-text-secondary);">
                  <Clock class="w-3 h-3" />
                  <span>创建于 {{ selectedVersion.createdTime }}</span>
                </div>
              </div>

              <!-- 加载中 -->
              <div v-else-if="loadingVersionDetail" class="p-6 flex flex-col items-center gap-2" style="color: var(--theme-text-secondary);">
                <div class="w-5 h-5 border-2 border-t-transparent rounded-full animate-spin" style="border-color: var(--theme-primary);"></div>
                <span class="text-xs">加载版本详情...</span>
              </div>

              <!-- 空状态 -->
              <div v-else class="p-6 text-center" style="color: var(--theme-text-secondary);">
                <FileText class="w-8 h-8 mx-auto mb-2 opacity-40" />
                <p class="text-sm">点击左侧版本查看详情</p>
                <p class="text-xs mt-1">可查看版本内容、回滚或勾选两个版本进行对比</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <!-- 底部 Footer -->
    <SiteFooter />
  </div>
</template>

<style scoped>
/* 版本历史抽屉过渡动画 */
.version-drawer-enter-active,
.version-drawer-leave-active {
  transition: opacity 0.2s ease;
}
.version-drawer-enter-active > div:last-child,
.version-drawer-leave-active > div:last-child {
  transition: transform 0.25s ease;
}
.version-drawer-enter-from,
.version-drawer-leave-to {
  opacity: 0;
}
</style>
