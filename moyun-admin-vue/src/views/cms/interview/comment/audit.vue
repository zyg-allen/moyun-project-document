<template>
  <AuditWorkbench
    :embedded="embedded"
    audit-title="面试评论审核"
    search-placeholder="搜索评论内容"
    search-field="keyword"
    :left-width="'420px'"
    empty-description="请从左侧选择评论查看详情"
    pass-button-text="通过"
    pass-status="published"
    pass-success-message="审核通过，评论已发布"
    :status-map="statusMap"
    :done-filter-options="doneFilterOptions"
    done-filter-default="published"
    back-route="/interview/experience"
    :list-api="listApi"
    :detail-api="detailApi"
    :audit-api="auditApi"
  >
    <!-- 列表项：评论内容截断显示 -->
    <template #listItem="{ item }">
      <div class="cmt-item-content">{{ item.content }}</div>
      <div class="cmt-item-meta">
        <span class="cmt-item-user">用户ID: {{ item.userId }}</span>
        <el-tag size="small" :type="getStatusType(item.status)">
          {{ getStatusLabel(item.status) }}
        </el-tag>
      </div>
      <div class="cmt-item-time">面经ID: {{ item.experienceId }} · {{ item.createTime }}</div>
    </template>

    <!-- 详情元信息 -->
    <template #detailMeta="{ detail }">
      <div class="meta-item">
        <span class="meta-label">评论ID：</span>
        <span>{{ detail.id }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">所属面经ID：</span>
        <span>{{ detail.experienceId }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">用户ID：</span>
        <span>{{ detail.userId }}</span>
      </div>
      <div class="meta-item" v-if="detail.parentId">
        <span class="meta-label">回复评论ID：</span>
        <span>{{ detail.parentId }}</span>
      </div>
      <div class="meta-item" v-if="detail.replyToUserId">
        <span class="meta-label">回复用户ID：</span>
        <span>{{ detail.replyToUserId }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">点赞数：</span>
        <span>{{ detail.likes ?? detail.likeCount ?? 0 }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">创建时间：</span>
        <span>{{ detail.createTime }}</span>
      </div>
    </template>

    <!-- 详情正文：评论全文 -->
    <template #detailContent="{ detail }">
      <el-divider content-position="left">评论内容</el-divider>
      <div class="cmt-content">{{ detail.content }}</div>
    </template>
  </AuditWorkbench>
</template>

<script setup lang="ts">
import AuditWorkbench from '@/components/AuditWorkbench/index.vue';
import {
  listInterviewComment, getInterviewComment, auditInterviewComment
} from '@/api/cms/interview';

defineProps<{ embedded?: boolean }>();

const statusMap: Record<string, { label: string; type: any }> = {
  pending:   { label: '待审核', type: 'warning' },
  published: { label: '已通过', type: 'success' },
  rejected:  { label: '已拒绝', type: 'danger'  },
  deleted:   { label: '已删除', type: 'info'    }
};
const doneFilterOptions = [
  { label: '已通过', value: 'published' },
  { label: '已拒绝', value: 'rejected'  }
];
function getStatusLabel(status: string) { return statusMap[status]?.label || status; }
function getStatusType(status: string)  { return statusMap[status]?.type  || 'info'; }

async function listApi(params: any)   { return listInterviewComment(params); }
async function detailApi(id: any)     { return getInterviewComment(id); }
// 后端 auditComment(body) 使用 id/status/remark 字段名
async function auditApi(payload: { id: any; status: string; auditRemark: string }) {
  const data: any = { id: payload.id, status: payload.status };
  if (payload.auditRemark?.trim()) data.remark = payload.auditRemark;
  return auditInterviewComment(data);
}
</script>

<style scoped>
.cmt-item-content {
  font-size: 14px;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.5;
}
.cmt-item-meta {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px; color: #909399; margin-bottom: 4px;
}
.cmt-item-user { font-size: 12px; }
.cmt-item-time { font-size: 12px; color: #c0c4cc; }

.cmt-content {
  padding: 16px 20px;
  background: #fafafa;
  border-radius: 4px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
