<template>
  <div class="my-articles-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">我的文章</h1>
        <p class="page-subtitle">管理你创作的所有文章，包含草稿、待审核、已发布、已拒绝</p>
      </div>
      <button class="publish-btn" @click="goPublish">
        <Plus class="w-4 h-4" />
        发布新文章
      </button>
    </div>

    <!-- 状态筛选 Tab -->
    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        class="status-tab"
        :class="{ active: activeStatus === tab.value }"
        @click="switchStatus(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count !== null" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 文章列表 -->
    <div class="article-list" :class="{ 'is-loading': loading }">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>
      <div v-else-if="articles.length === 0" class="empty-state">
        <FileText class="empty-icon" />
        <p class="empty-text">暂无文章</p>
        <button class="empty-btn" @click="goPublish">去发布第一篇文章</button>
      </div>

      <template v-else>
      <div v-for="article in articles" :key="article.id" class="article-item">
        <!-- 封面缩略图 -->
        <div class="article-cover" v-if="article.cover">
          <img :src="article.cover" :alt="article.title" />
        </div>
        <div class="article-cover placeholder" v-else>
          <ImageIcon class="w-6 h-6" />
        </div>

        <!-- 文章信息 -->
        <div class="article-info">
          <div class="article-title-row">
            <h3 class="article-title" @click="viewArticle(article)">{{ article.title || '无标题草稿' }}</h3>
            <span class="status-badge" :class="`status-${article.status}`">
              {{ getStatusLabel(article.status) }}
            </span>
          </div>
          <p class="article-excerpt">{{ article.excerpt || '暂无摘要' }}</p>
          <!-- 拒绝原因 -->
          <div v-if="article.status === 'rejected' && article.remark" class="reject-reason">
            <AlertCircle class="w-3.5 h-3.5" />
            <span>拒绝原因：{{ article.remark }}</span>
          </div>
          <div class="article-meta">
            <span class="meta-item">
              <Clock class="w-3 h-3" />
              {{ formatTime(article.updateTime || article.createTime) }}
            </span>
            <span v-if="article.categoryName" class="meta-item">
              <Folder class="w-3 h-3" />
              {{ article.categoryName }}
            </span>
            <span v-if="article.status === 'published'" class="meta-item">
              <Eye class="w-3 h-3" />
              {{ article.views || 0 }}
            </span>
            <span v-if="article.status === 'published'" class="meta-item">
              <Heart class="w-3 h-3" />
              {{ article.likes || 0 }}
            </span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="article-actions">
          <button
            v-if="article.status === 'draft' || article.status === 'rejected'"
            class="action-btn primary"
            @click="editArticle(article)"
          >
            <Edit3 class="w-3.5 h-3.5" />
            继续编辑
          </button>
          <button
            v-if="article.status === 'draft' || article.status === 'rejected'"
            class="action-btn success"
            @click="resubmitArticle(article)"
          >
            <Send class="w-3.5 h-3.5" />
            重新提交
          </button>
          <button class="action-btn" @click="viewArticle(article)">
            <Eye class="w-3.5 h-3.5" />
            查看
          </button>
          <button class="action-btn danger" @click="deleteArticle(article)">
            <Trash2 class="w-3.5 h-3.5" />
            删除
          </button>
        </div>
      </div>
      </template>
    </div>

    <!-- 分页 -->
    <div v-if="total > pageSize" class="pagination">
      <button :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">上一页</button>
      <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
      <button :disabled="pageNum >= totalPages" @click="changePage(pageNum + 1)">下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import {
  Plus, FileText, ImageIcon, Clock, Folder, Eye, Heart,
  Edit3, Send, Trash2, AlertCircle
} from 'lucide-vue-next';
import { getMyArticles, deleteArticle as deleteArticleApi, updateArticle } from '@/api/article';
import { useToast } from '@/composables/useToast';
import { useConfirmModal } from '@/composables/useConfirmModal';

const router = useRouter();
const route = useRoute();
const toast = useToast();
const confirmModal = useConfirmModal();

// 状态数据
const loading = ref(false);
const articles = ref<any[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

// 从 query 参数初始化状态筛选（'all' 或空 → 全部，对应 activeStatus 空字符串）
const validStatuses = ['all', 'draft', 'pending', 'published', 'rejected'];
const queryStatus = (route.query.status as string) || '';
const initialStatus = validStatuses.includes(queryStatus) && queryStatus !== 'all' ? queryStatus : '';
const activeStatus = ref(initialStatus); // 空 = 全部

// 状态 Tab 配置
const statusTabs = computed(() => [
  { value: '', label: '全部', count: null },
  { value: 'draft', label: '草稿', count: null },
  { value: 'pending', label: '待审核', count: null },
  { value: 'published', label: '已发布', count: null },
  { value: 'rejected', label: '已拒绝', count: null }
]);

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1);

// 状态标签映射
function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    draft: '草稿',
    pending: '待审核',
    published: '已发布',
    rejected: '已拒绝',
    archived: '已归档'
  };
  return map[status] || status;
}

// 时间格式化
function formatTime(time: string): string {
  if (!time) return '';
  const d = new Date(time);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

// 加载文章列表
async function loadArticles() {
  loading.value = true;
  try {
    const res = await getMyArticles({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: activeStatus.value || undefined,
    });
    if (res.code === 200) {
      const page = res.data;
      articles.value = page.records || [];
      total.value = page.total || 0;
    }
  } catch (error: any) {
    console.error('加载我的文章失败:', error);
    if (error?.message?.includes('登录') || error?.message?.includes('401')) {
      router.push('/login?redirect=/my/articles');
    }
  } finally {
    loading.value = false;
  }
}

// 切换状态 Tab
function switchStatus(status: string) {
  activeStatus.value = status;
  pageNum.value = 1;
  loadArticles();
}

// 翻页
function changePage(page: number) {
  pageNum.value = page;
  loadArticles();
}

// 跳转发布页
function goPublish() {
  router.push('/publish');
}

// 查看文章
function viewArticle(article: any) {
  if (article.status === 'published') {
    router.push(`/article/${article.id}`);
  } else {
    // 草稿/待审核/已拒绝：跳转编辑页查看
    router.push({ path: '/publish', query: { id: article.id, mode: 'view' } });
  }
}

// 继续编辑
function editArticle(article: any) {
  router.push({ path: '/publish', query: { id: article.id } });
}

// 重新提交审核（草稿/已拒绝 → pending）
async function resubmitArticle(article: any) {
  const ok = await confirmModal.confirm(`确认重新提交文章"${article.title}"进行审核？`, {
    title: '重新提交审核',
    confirmText: '确认提交',
    danger: false,
  });
  if (!ok) return;
  try {
    // 更新已有文章，将状态改为 pending（待审核）
    await updateArticle({
      id: article.id,
      title: article.title,
      content: article.content,
      excerpt: article.excerpt,
      cover: article.cover,
      categoryId: article.categoryId,
      tagNames: article.tagNames,
      status: 'pending'
    } as any);
    toast.success('已重新提交，等待审核');
    loadArticles();
  } catch (error: any) {
    toast.error(error?.message || '提交失败');
  }
}

// 删除文章
async function deleteArticle(article: any) {
  const ok = await confirmModal.confirm(`确认删除文章"${article.title || '无标题草稿'}"？删除后不可恢复。`, {
    title: '删除文章',
    confirmText: '确认删除',
    danger: true,
  });
  if (!ok) return;
  try {
    await deleteArticleApi(article.id);
    toast.success('删除成功');
    loadArticles();
  } catch (error: any) {
    toast.error(error?.message || '删除失败');
  }
}

onMounted(() => {
  loadArticles();
});
</script>

<style scoped>
.my-articles-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.page-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--theme-text);
  margin: 0 0 0.25rem;
}

.page-subtitle {
  font-size: 0.875rem;
  color: var(--theme-text-secondary);
  margin: 0;
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.publish-btn:hover {
  opacity: 0.9;
}

/* 状态 Tab */
.status-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--theme-border);
  overflow-x: auto;
}

.status-tab {
  padding: 0.625rem 1rem;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--theme-text-secondary);
  font-size: 0.875rem;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
}

.status-tab:hover {
  color: var(--theme-text);
}

.status-tab.active {
  color: var(--theme-primary);
  border-bottom-color: var(--theme-primary);
  font-weight: 500;
}

.tab-count {
  background: var(--theme-accent);
  color: var(--theme-text-secondary);
  padding: 0 0.375rem;
  border-radius: 0.625rem;
  font-size: 0.75rem;
  min-width: 1.25rem;
  text-align: center;
}

/* 文章列表 */
.article-list {
  min-height: 200px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: var(--theme-text-secondary);
}

.loading-spinner {
  width: 2rem;
  height: 2rem;
  border: 2px solid var(--theme-border);
  border-top-color: var(--theme-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 0.75rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.article-item {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  border: 1px solid var(--theme-border);
  border-radius: 0.5rem;
  margin-bottom: 0.75rem;
  background: var(--theme-surface);
  transition: box-shadow 0.2s;
}

.article-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.article-cover {
  width: 100px;
  height: 100px;
  border-radius: 0.375rem;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--theme-accent);
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.article-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--theme-text-secondary);
}

.article-info {
  flex: 1;
  min-width: 0;
}

.article-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.375rem;
}

.article-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--theme-text);
  margin: 0;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.article-title:hover {
  color: var(--theme-primary);
}

.status-badge {
  padding: 0.125rem 0.5rem;
  border-radius: 0.625rem;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
}

.status-draft { background: #e5e7eb; color: #4b5563; }
.status-pending { background: #fef3c7; color: #92400e; }
.status-published { background: #d1fae5; color: #065f46; }
.status-rejected { background: #fee2e2; color: #991b1b; }
.status-archived { background: #f3f4f6; color: #6b7280; }

.article-excerpt {
  font-size: 0.8125rem;
  color: var(--theme-text-secondary);
  margin: 0 0 0.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.reject-reason {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: #991b1b;
  background: #fee2e2;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  margin-bottom: 0.5rem;
}

.article-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.75rem;
  color: var(--theme-text-secondary);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.article-actions {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  align-items: flex-end;
  justify-content: center;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.25rem 0.625rem;
  background: transparent;
  border: 1px solid var(--theme-border);
  border-radius: 0.25rem;
  color: var(--theme-text);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.action-btn:hover {
  background: var(--theme-hover);
}

.action-btn.primary {
  border-color: #3b82f6;
  color: #3b82f6;
}

.action-btn.success {
  border-color: #10b981;
  color: #10b981;
}

.action-btn.danger {
  border-color: #ef4444;
  color: #ef4444;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--theme-text-secondary);
}

.empty-icon {
  width: 3rem;
  height: 3rem;
  margin: 0 auto 0.75rem;
  opacity: 0.5;
}

.empty-text {
  font-size: 0.9375rem;
  margin: 0 0 1rem;
}

.empty-btn {
  padding: 0.5rem 1.25rem;
  background: var(--theme-primary);
  color: white;
  border: none;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  cursor: pointer;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
  margin-top: 2rem;
}

.pagination button {
  padding: 0.375rem 0.875rem;
  border: 1px solid var(--theme-border);
  background: var(--theme-surface);
  color: var(--theme-text);
  border-radius: 0.25rem;
  cursor: pointer;
  font-size: 0.875rem;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.875rem;
  color: var(--theme-text-secondary);
}

/* 移动端适配 */
@media (max-width: 640px) {
  .article-item {
    flex-direction: column;
  }
  .article-cover {
    width: 100%;
    height: 140px;
  }
  .article-actions {
    flex-direction: row;
    flex-wrap: wrap;
    align-items: flex-start;
  }
}
</style>
