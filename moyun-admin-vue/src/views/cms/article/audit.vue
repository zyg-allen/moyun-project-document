<template>
  <div class="app-container audit-container">
    <!-- 顶部操作栏 -->
    <div class="audit-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="audit-title">文章审核</span>
    </div>

    <div class="audit-body">
      <!-- 左侧列表 -->
      <div class="audit-left">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="待审核" name="pending" />
          <el-tab-pane label="已办理" name="done" />
        </el-tabs>

        <!-- 标题搜索 -->
        <div class="search-box">
          <el-input
              v-model="queryParams.title"
              placeholder="搜索文章标题"
              clearable
              size="small"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <!-- 已办理 tab 子筛选 -->
        <div v-if="activeTab === 'done'" class="sub-filter">
          <el-select v-model="doneFilter" size="small" style="width: 100%" @change="handleDoneFilterChange">
            <el-option label="已发布" value="published" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </div>

        <div v-loading="listLoading" class="article-list">
          <div
              v-for="item in articleList"
              :key="item.id"
              class="article-item"
              :class="{ 'is-active': currentId === item.id }"
              @click="handleSelectArticle(item)"
          >
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">
              <span class="item-author">{{ item.authorNickname || item.authorUsername || '-' }}</span>
              <el-tag size="small" :type="getStatusType(item.status)">
                {{ getStatusLabel(item.status) }}
              </el-tag>
            </div>
            <div class="item-time">{{ item.createTime }}</div>
          </div>
          <el-empty v-if="!listLoading && articleList.length === 0" description="暂无数据" />
        </div>

        <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            :page-sizes="[10, 20, 30]"
            @pagination="getList"
        />
      </div>

      <!-- 右侧详情 -->
      <div class="audit-right">
        <el-card v-if="currentArticle" v-loading="detailLoading" shadow="never" class="detail-card">
          <template #header>
            <div class="detail-header">
              <span class="detail-title">{{ currentArticle.title }}</span>
              <el-tag :type="getStatusType(currentArticle.status)">
                {{ getStatusLabel(currentArticle.status) }}
              </el-tag>
            </div>
          </template>

          <div class="detail-meta">
            <div class="meta-item">
              <span class="meta-label">作者：</span>
              <span>{{ currentArticle.authorNickname || currentArticle.authorUsername || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">分类：</span>
              <el-tag size="small">{{ currentArticle.categoryName || '-' }}</el-tag>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建时间：</span>
              <span>{{ currentArticle.createTime }}</span>
            </div>
          </div>

          <div v-if="currentArticle.cover" class="detail-cover">
            <span class="meta-label">封面图：</span>
            <el-image
                :src="currentArticle.cover"
                fit="cover"
                class="cover-img"
                :preview-src-list="[currentArticle.cover]"
            />
          </div>

          <el-divider content-position="left">内容预览</el-divider>
          <div v-html="currentArticle.content" class="article-content-preview"></div>

          <!-- 已办理 tab：显示审核结果和审核意见 -->
          <template v-if="activeTab === 'done'">
            <el-divider content-position="left">审核结果</el-divider>
            <div class="audit-result">
              <el-tag :type="currentArticle.status === 'published' ? 'success' : 'danger'">
                {{ getStatusLabel(currentArticle.status) }}
              </el-tag>
              <div v-if="currentArticle.remark" class="audit-remark">{{ currentArticle.remark }}</div>
              <div v-else class="audit-remark audit-remark-empty">（无审核意见）</div>
            </div>
          </template>

          <!-- 审核操作区：仅待审核 tab 显示 -->
          <template v-if="activeTab === 'pending'">
            <el-divider content-position="left">审核意见</el-divider>
            <el-input
                v-model="auditRemark"
                type="textarea"
                :rows="4"
                placeholder="请输入审核意见（驳回时必填，通过时选填）"
            />
            <div class="audit-actions">
              <el-button type="danger" :loading="rejectLoading" @click="handleReject">驳回</el-button>
              <el-button type="primary" :loading="approveLoading" @click="handleApprove">通过并发布</el-button>
            </div>
          </template>
        </el-card>

        <el-empty v-else description="请从左侧选择文章查看详情" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Search } from '@element-plus/icons-vue';
import { listArticle, getArticle, auditArticle } from '@/api/cms/article';

const route = useRoute();
const router = useRouter();

const activeTab = ref<'pending' | 'done'>('pending');
const doneFilter = ref<'published' | 'rejected'>('published');
const listLoading = ref(false);
const detailLoading = ref(false);
const articleList = ref<any[]>([]);
const total = ref(0);
const currentId = ref<number | null>(null);
const currentArticle = ref<any>(null);

const auditRemark = ref('');
const approveLoading = ref(false);
const rejectLoading = ref(false);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: ''
});

// 状态映射
const statusMap: Record<string, { label: string; type: any }> = {
  draft: { label: '草稿', type: 'info' },
  pending: { label: '待审核', type: 'warning' },
  published: { label: '已发布', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  archived: { label: '已归档', type: 'warning' }
};

function getStatusLabel(status: string) {
  return statusMap[status]?.label || status;
}

function getStatusType(status: string) {
  return statusMap[status]?.type || 'info';
}

// 当前 tab 对应的查询状态
function getQueryStatus(): string {
  if (activeTab.value === 'pending') {
    return 'pending';
  }
  return doneFilter.value;
}

// 加载列表
async function getList() {
  listLoading.value = true;
  try {
    const params: any = {
      status: getQueryStatus(),
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize
    };
    if (queryParams.title) {
      params.title = queryParams.title;
    }
    const res = await listArticle(params);
    articleList.value = res.data.records || [];
    total.value = res.data.total || 0;

    // 当前选中项不在列表中且列表非空，选中第一篇
    if (articleList.value.length > 0) {
      const exists = articleList.value.find((item: any) => item.id === currentId.value);
      if (!exists) {
        await handleSelectArticle(articleList.value[0]);
      }
    } else if (currentArticle.value && activeTab.value === 'pending') {
      currentArticle.value = null;
      currentId.value = null;
    }
  } catch (error) {
    console.error('加载列表失败:', error);
  } finally {
    listLoading.value = false;
  }
}

// 切换 tab
function handleTabChange() {
  queryParams.pageNum = 1;
  currentId.value = null;
  currentArticle.value = null;
  getList();
}

// 标题搜索
function handleSearch() {
  queryParams.pageNum = 1;
  currentId.value = null;
  currentArticle.value = null;
  getList();
}

// 切换已办理子筛选
function handleDoneFilterChange() {
  queryParams.pageNum = 1;
  currentId.value = null;
  currentArticle.value = null;
  getList();
}

// 选中文章
async function handleSelectArticle(item: any) {
  if (!item) return;
  currentId.value = item.id;
  router.replace({ query: { id: String(item.id) } });
  await loadArticleDetail(item.id);
}

// 加载文章详情
async function loadArticleDetail(id: any) {
  detailLoading.value = true;
  try {
    const res = await getArticle(id);
    currentArticle.value = res.data || null;
    auditRemark.value = '';
  } catch (error) {
    console.error('加载文章详情失败:', error);
    ElMessage.error('加载文章详情失败');
  } finally {
    detailLoading.value = false;
  }
}

// 驳回
async function handleReject() {
  if (!currentArticle.value) return;
  if (!auditRemark.value.trim()) {
    ElMessage.warning('请输入审核意见');
    return;
  }
  rejectLoading.value = true;
  try {
    await auditArticle({
      id: currentArticle.value.id,
      status: 'rejected',
      remark: auditRemark.value
    });
    ElMessage.success('已驳回');
    await refreshAfterAudit();
  } catch (error: any) {
    ElMessage.error(error.message || '审核操作失败');
  } finally {
    rejectLoading.value = false;
  }
}

// 通过
async function handleApprove() {
  if (!currentArticle.value) return;
  approveLoading.value = true;
  try {
    const payload: any = {
      id: currentArticle.value.id,
      status: 'published'
    };
    if (auditRemark.value.trim()) {
      payload.remark = auditRemark.value;
    }
    await auditArticle(payload);
    ElMessage.success('审核通过，文章已发布');
    await refreshAfterAudit();
  } catch (error: any) {
    ElMessage.error(error.message || '审核操作失败');
  } finally {
    approveLoading.value = false;
  }
}

// 审核后刷新：从待审核列表移除该项，并选中下一篇
async function refreshAfterAudit() {
  const idx = articleList.value.findIndex((item: any) => item.id === currentId.value);
  // 从列表中移除
  articleList.value = articleList.value.filter((item: any) => item.id !== currentId.value);
  total.value = Math.max(0, total.value - 1);

  // 选中下一篇（同索引位置，或最后一篇）
  let nextItem: any = null;
  if (articleList.value.length > 0) {
    nextItem = articleList.value[Math.min(idx, articleList.value.length - 1)];
  }

  if (nextItem) {
    currentId.value = nextItem.id;
    router.replace({ query: { id: String(nextItem.id) } });
    await loadArticleDetail(nextItem.id);
  } else if (total.value > 0 && queryParams.pageNum > 1) {
    // 当前页空了，回退一页
    queryParams.pageNum = Math.max(1, queryParams.pageNum - 1);
    await getList();
  } else {
    currentArticle.value = null;
    currentId.value = null;
    router.replace({ query: {} });
  }
}

// 返回列表页：优先回退历史，避免动态路由 /cms/article 未注册导致 404
function goBack() {
  if (window.history.state && window.history.state.back) {
    router.go(-1);
  } else {
    router.push('/cms/article');
  }
}

// 初始化
onMounted(async () => {
  const idFromQuery = route.query.id;
  await getList();
  // URL 带了 id 时加载该文章详情
  if (idFromQuery) {
    const exists = articleList.value.find((item: any) => String(item.id) === String(idFromQuery));
    if (exists) {
      currentId.value = exists.id;
      await loadArticleDetail(exists.id);
    } else {
      // 即使不在当前列表也直接加载详情
      currentId.value = Number(idFromQuery) || null;
      await loadArticleDetail(idFromQuery);
    }
  }
});
</script>

<style scoped>
.audit-container {
  padding: 20px;
}

.audit-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.audit-title {
  margin-left: 12px;
  font-size: 18px;
  font-weight: 600;
}

.audit-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.audit-left {
  width: 380px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 4px;
  padding: 12px;
}

.sub-filter {
  margin-bottom: 12px;
}

.search-box {
  margin-bottom: 12px;
}

.article-list {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}

.article-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.article-item:hover {
  background-color: var(--el-color-primary-light-9);
}

.article-item.is-active {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.item-author {
  font-size: 12px;
  color: #909399;
}

.item-time {
  font-size: 12px;
  color: #c0c4cc;
}

.audit-right {
  flex: 1;
  min-width: 0;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.detail-card {
  width: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  word-break: break-all;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-item {
  font-size: 13px;
}

.meta-label {
  color: #909399;
}

.detail-cover {
  margin-bottom: 12px;
}

.cover-img {
  width: 200px;
  height: 120px;
  border-radius: 4px;
}

.article-content-preview {
  pointer-events: none;
  line-height: 1.7;
  color: #303133;
  max-height: 500px;
  overflow-y: auto;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

.article-content-preview :deep(img) {
  max-width: 100%;
}

.audit-result {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.audit-remark {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  white-space: pre-wrap;
}

.audit-remark-empty {
  color: #c0c4cc;
}

.audit-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
