<template>
  <AuditWorkbench
    :embedded="embedded"
    audit-title="文章审核"
    search-placeholder="搜索文章标题"
    search-field="title"
    :left-width="'380px'"
    empty-description="请从左侧选择文章查看详情"
    pass-button-text="通过并发布"
    pass-status="published"
    pass-success-message="审核通过，文章已发布"
    :status-map="statusMap"
    :done-filter-options="doneFilterOptions"
    done-filter-default="published"
    back-route="/cms/article"
    cover-label="封面图："
    :list-api="listApi"
    :detail-api="detailApi"
    :audit-api="auditApi"
  >
    <!-- 列表项 -->
    <template #listItem="{ item }">
      <div class="item-title">{{ item.title }}</div>
      <div class="item-meta">
        <span class="item-author">{{ item.authorNickname || item.authorUsername || '-' }}</span>
        <el-tag size="small" :type="getStatusType(item.status)">
          {{ getStatusLabel(item.status) }}
        </el-tag>
      </div>
      <div class="item-time">{{ item.createTime }}</div>
    </template>

    <!-- 详情元信息 -->
    <template #detailMeta="{ detail }">
      <div class="meta-item">
        <span class="meta-label">作者：</span>
        <span>{{ detail.authorNickname || detail.authorUsername || '-' }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">分类：</span>
        <el-tag size="small">{{ detail.categoryName || '-' }}</el-tag>
      </div>
      <div class="meta-item">
        <span class="meta-label">创建时间：</span>
        <span>{{ detail.createTime }}</span>
      </div>
    </template>

    <!-- 详情正文：文章内容预览 -->
    <template #detailContent="{ detail }">
      <el-divider content-position="left">内容预览</el-divider>
      <div v-html="detail.content" class="article-content-preview"></div>
    </template>
  </AuditWorkbench>
</template>

<script setup lang="ts">
import AuditWorkbench from '@/components/AuditWorkbench/index.vue';
import { listArticle, getArticle, auditArticle } from '@/api/cms/article';

// 嵌入模式：被「内容审核中心」Tab 容器引用时为 true
defineProps<{ embedded?: boolean }>();

const statusMap: Record<string, { label: string; type: any }> = {
  draft: { label: '草稿', type: 'info' },
  pending: { label: '待审核', type: 'warning' },
  published: { label: '已发布', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  archived: { label: '已归档', type: 'warning' }
};

const doneFilterOptions = [
  { label: '已发布', value: 'published' },
  { label: '已拒绝', value: 'rejected' }
];

function getStatusLabel(status: string) {
  return statusMap[status]?.label || status;
}
function getStatusType(status: string) {
  return statusMap[status]?.type || 'info';
}

// 适配函数：把通用查询参数透传给 listArticle
async function listApi(params: any) {
  return listArticle(params);
}
// 适配函数：取 res.data
async function detailApi(id: any) {
  return getArticle(id);
}
// 适配函数：统一 payload 转为 article 后端的 remark 字段
async function auditApi(payload: { id: any; status: string; auditRemark: string }) {
  const data: any = { id: payload.id, status: payload.status };
  if (payload.auditRemark?.trim()) {
    data.remark = payload.auditRemark;
  }
  return auditArticle(data);
}
</script>

<style scoped>
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
.item-author { font-size: 12px; color: #909399; }
.item-time { font-size: 12px; color: #c0c4cc; }

.article-content-preview {
  pointer-events: none;
  line-height: 1.7;
  color: #303133;
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 4px;
}
.article-content-preview :deep(p),
.article-content-preview :deep(h1),
.article-content-preview :deep(h2),
.article-content-preview :deep(h3),
.article-content-preview :deep(h4),
.article-content-preview :deep(h5),
.article-content-preview :deep(h6),
.article-content-preview :deep(ul),
.article-content-preview :deep(ol),
.article-content-preview :deep(blockquote),
.article-content-preview :deep(pre) { margin: 0 0 0.5em; }
.article-content-preview :deep(p:last-child),
.article-content-preview :deep(h1:last-child),
.article-content-preview :deep(h2:last-child),
.article-content-preview :deep(h3:last-child),
.article-content-preview :deep(h4:last-child),
.article-content-preview :deep(h5:last-child),
.article-content-preview :deep(h6:last-child),
.article-content-preview :deep(ul:last-child),
.article-content-preview :deep(ol:last-child),
.article-content-preview :deep(blockquote:last-child),
.article-content-preview :deep(pre:last-child) { margin-bottom: 0; }
.article-content-preview :deep(h1) { font-size: 1.8em; font-weight: 700; margin-top: 0.5em; }
.article-content-preview :deep(h2) { font-size: 1.5em; font-weight: 700; margin-top: 0.5em; }
.article-content-preview :deep(h3) { font-size: 1.3em; font-weight: 700; margin-top: 0.5em; }
.article-content-preview :deep(h4) { font-size: 1.2em; font-weight: 700; margin-top: 0.5em; }
.article-content-preview :deep(h5) { font-size: 1em; font-weight: 700; }
.article-content-preview :deep(h6) { font-size: 0.9em; font-weight: 700; }
.article-content-preview :deep(blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding: 4px 12px;
  margin: 0.5em 0;
  color: #606266;
  background: #f5f7fa;
}
.article-content-preview :deep(pre) {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 0.9em;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0.5em 0;
}
.article-content-preview :deep(img) { max-width: 100%; height: auto; margin: 0.5em 0; }
.article-content-preview :deep(a) { color: var(--el-color-primary); text-decoration: underline; }
.article-content-preview :deep(ul) { list-style: disc; padding-left: 2em; margin: 0.5em 0; }
.article-content-preview :deep(ol) { list-style: decimal; padding-left: 2em; margin: 0.5em 0; }
.article-content-preview :deep(li) { margin: 0.2em 0; }
</style>
