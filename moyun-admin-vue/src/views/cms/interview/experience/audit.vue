<template>
  <AuditWorkbench
    :embedded="embedded"
    audit-title="面经审核"
    search-placeholder="搜索面经标题/公司/内容"
    search-field="keyword"
    :left-width="'400px'"
    empty-description="请从左侧选择面经查看详情"
    pass-button-text="通过并发布"
    pass-status="published"
    pass-success-message="审核通过，面经已发布"
    :status-map="statusMap"
    :done-filter-options="doneFilterOptions"
    done-filter-default="published"
    back-route="/interview/experience"
    cover-label="封面图："
    :list-api="listApi"
    :detail-api="detailApi"
    :audit-api="auditApi"
  >
    <!-- 列表项 -->
    <template #listItem="{ item }">
      <div class="exp-item-title">{{ item.title }}</div>
      <div class="exp-item-meta">
        <span class="exp-item-company">{{ item.company || '-' }}
          <template v-if="item.position"> · {{ item.position }}</template>
        </span>
        <el-tag size="small" :type="getStatusType(item.status)">
          {{ getStatusLabel(item.status) }}
        </el-tag>
      </div>
      <div class="exp-item-time">
        <el-tag v-if="item.isTop" size="small" type="danger" effect="plain" style="margin-right: 6px;">置顶</el-tag>
        {{ item.createTime }}
      </div>
    </template>

    <!-- 详情头额外 tag -->
    <template #detailHeaderExtra="{ detail }">
      <el-tag v-if="detail.isTop" size="small" type="danger" effect="plain" style="margin-right: 6px;">置顶</el-tag>
    </template>

    <!-- 详情元信息 -->
    <template #detailMeta="{ detail }">
      <div class="meta-item">
        <span class="meta-label">作者ID：</span>
        <span>{{ detail.authorId ?? detail.userId ?? '-' }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">公司：</span>
        <el-tag size="small">{{ detail.company }}</el-tag>
      </div>
      <div class="meta-item">
        <span class="meta-label">岗位：</span>
        <span>{{ detail.position || '-' }}</span>
      </div>
      <div class="meta-item" v-if="detail.year">
        <span class="meta-label">年份/月份：</span>
        <span>{{ detail.year }} 年 {{ detail.month ?? '-' }} 月</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">标签：</span>
        <el-tag
          v-for="tag in tagList(detail.tags)" :key="tag" size="small" style="margin: 2px;"
        >{{ tag }}</el-tag>
      </div>
      <div class="meta-item">
        <span class="meta-label">浏览/点赞/评论：</span>
        <span>{{ detail.views ?? detail.viewCount ?? 0 }} / {{ detail.likes ?? detail.likeCount ?? 0 }} / {{ detail.comments ?? detail.commentCount ?? 0 }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">创建时间：</span>
        <span>{{ detail.createTime }}</span>
      </div>
    </template>

    <!-- 详情正文：面经内容 + 摘要 -->
    <template #detailContent="{ detail }">
      <el-divider v-if="detail.summary" content-position="left">内容摘要</el-divider>
      <div v-if="detail.summary" class="exp-summary">{{ detail.summary }}</div>

      <el-divider content-position="left">面经正文</el-divider>
      <div v-html="detail.content" class="exp-content-preview"></div>
    </template>
  </AuditWorkbench>
</template>

<script setup lang="ts">
import AuditWorkbench from '@/components/AuditWorkbench/index.vue';
import {
  listInterviewExperience, getInterviewExperience, auditInterviewExperience
} from '@/api/cms/interview';

defineProps<{ embedded?: boolean }>();

const statusMap: Record<string, { label: string; type: any }> = {
  draft:     { label: '草稿',   type: 'info'    },
  pending:   { label: '待审核', type: 'warning' },
  published: { label: '已发布', type: 'success' },
  rejected:  { label: '已拒绝', type: 'danger'  },
  archived:  { label: '已归档', type: 'warning' }
};
const doneFilterOptions = [
  { label: '已发布', value: 'published' },
  { label: '已拒绝', value: 'rejected'  }
];
function getStatusLabel(status: string) { return statusMap[status]?.label || status; }
function getStatusType(status: string)  { return statusMap[status]?.type  || 'info'; }

function tagList(tags: any): string[] {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  if (typeof tags === 'string') {
    return tags.split(',').map((t: string) => t.trim()).filter(Boolean);
  }
  return [];
}

async function listApi(params: any)   { return listInterviewExperience(params); }
async function detailApi(id: any)     { return getInterviewExperience(id); }
// 后端 auditExperience(body) 使用 id/status/remark 字段名
async function auditApi(payload: { id: any; status: string; auditRemark: string }) {
  const data: any = { id: payload.id, status: payload.status };
  if (payload.auditRemark?.trim()) data.remark = payload.auditRemark;
  return auditInterviewExperience(data);
}
</script>

<style scoped>
.exp-item-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.exp-item-meta {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 4px;
}
.exp-item-company { font-size: 12px; color: #909399; }
.exp-item-time { font-size: 12px; color: #c0c4cc; }

.exp-summary {
  padding: 12px 16px;
  background: #fdf6ec;
  color: #b88230;
  border-left: 4px solid var(--el-color-warning);
  border-radius: 4px;
  margin-bottom: 16px;
  line-height: 1.7;
}

.exp-content-preview {
  pointer-events: none;
  line-height: 1.7;
  color: #303133;
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
.exp-content-preview :deep(p),
.exp-content-preview :deep(h1),
.exp-content-preview :deep(h2),
.exp-content-preview :deep(h3),
.exp-content-preview :deep(h4),
.exp-content-preview :deep(h5),
.exp-content-preview :deep(h6),
.exp-content-preview :deep(ul),
.exp-content-preview :deep(ol),
.exp-content-preview :deep(blockquote),
.exp-content-preview :deep(pre) { margin: 0 0 0.5em; }
.exp-content-preview :deep(p:last-child),
.exp-content-preview :deep(h1:last-child),
.exp-content-preview :deep(h2:last-child),
.exp-content-preview :deep(h3:last-child),
.exp-content-preview :deep(h4:last-child),
.exp-content-preview :deep(h5:last-child),
.exp-content-preview :deep(h6:last-child),
.exp-content-preview :deep(ul:last-child),
.exp-content-preview :deep(ol:last-child),
.exp-content-preview :deep(blockquote:last-child),
.exp-content-preview :deep(pre:last-child) { margin-bottom: 0; }
.exp-content-preview :deep(h1) { font-size: 1.8em; font-weight: 700; margin-top: 0.5em; }
.exp-content-preview :deep(h2) { font-size: 1.5em; font-weight: 700; margin-top: 0.5em; }
.exp-content-preview :deep(h3) { font-size: 1.3em; font-weight: 700; margin-top: 0.5em; }
.exp-content-preview :deep(h4) { font-size: 1.2em; font-weight: 700; margin-top: 0.5em; }
.exp-content-preview :deep(h5) { font-size: 1em; font-weight: 700; }
.exp-content-preview :deep(h6) { font-size: 0.9em; font-weight: 700; }
.exp-content-preview :deep(blockquote) {
  border-left: 4px solid var(--el-color-primary);
  padding: 4px 12px;
  margin: 0.5em 0;
  color: #606266;
  background: #f5f7fa;
}
.exp-content-preview :deep(pre) {
  background: #f5f7fa;
  padding: 8px 12px;
  border-radius: 4px;
  font-family: Menlo, Monaco, Consolas, "Courier New", monospace;
  font-size: 0.9em;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0.5em 0;
}
.exp-content-preview :deep(img) { max-width: 100%; height: auto; margin: 0.5em 0; }
.exp-content-preview :deep(a) { color: var(--el-color-primary); text-decoration: underline; }
.exp-content-preview :deep(ul) { list-style: disc; padding-left: 2em; margin: 0.5em 0; }
.exp-content-preview :deep(ol) { list-style: decimal; padding-left: 2em; margin: 0.5em 0; }
.exp-content-preview :deep(li) { margin: 0.2em 0; }
</style>
