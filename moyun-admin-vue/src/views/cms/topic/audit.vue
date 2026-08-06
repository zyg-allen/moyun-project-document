<template>
  <div class="app-container audit-container">
    <div class="audit-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <span class="audit-title">话题审核</span>
    </div>

    <div class="audit-body">
      <div class="audit-left">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="待审核" name="pending" />
          <el-tab-pane label="已办理" name="done" />
        </el-tabs>

        <div class="search-box">
          <el-input
              v-model="queryParams.keyword"
              placeholder="搜索话题标题"
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

        <div v-if="activeTab === 'done'" class="sub-filter">
          <el-select v-model="doneFilter" size="small" style="width: 100%" @change="handleDoneFilterChange">
            <el-option label="已通过（活跃）" value="active" />
            <el-option label="已驳回" value="rejected" />
            <el-option label="已归档" value="archived" />
          </el-select>
        </div>

        <div v-loading="listLoading" class="topic-list">
          <div
              v-for="item in topicList"
              :key="item.id"
              class="topic-item"
              :class="{ 'is-active': currentId === item.id }"
              @click="handleSelectTopic(item)"
          >
            <div class="item-cover-wrap">
              <el-image v-if="item.cover" :src="item.cover" fit="cover" class="item-cover" />
              <div v-else class="item-cover item-cover-empty">无图</div>
            </div>
            <div class="item-main">
              <div class="item-title">{{ item.title }}</div>
              <div class="item-meta">
                <span class="item-creator">{{ item.creatorNickname || item.creatorUsername || '-' }}</span>
                <el-tag size="small" :type="getStatusType(item.status)">
                  {{ getStatusLabel(item.status) }}
                </el-tag>
              </div>
              <div class="item-desc">{{ item.description || '（无描述）' }}</div>
              <div class="item-time">{{ item.createdTime }}</div>
            </div>
          </div>
          <el-empty v-if="!listLoading && topicList.length === 0" description="暂无数据" />
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

      <div class="audit-right">
        <el-card v-if="currentTopic" v-loading="detailLoading" shadow="never" class="detail-card">
          <template #header>
            <div class="detail-header">
              <span class="detail-title">{{ currentTopic.title }}</span>
              <el-tag :type="getStatusType(currentTopic.status)">
                {{ getStatusLabel(currentTopic.status) }}
              </el-tag>
            </div>
          </template>

          <div class="detail-meta">
            <div class="meta-item">
              <span class="meta-label">发起人：</span>
              <span>{{ currentTopic.creatorNickname || currentTopic.creatorUsername || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">创建时间：</span>
              <span>{{ currentTopic.createdTime }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">浏览：</span><span>{{ currentTopic.viewCount || 0 }}</span>
              <span style="margin-left:12px;" class="meta-label">观点：</span><span>{{ currentTopic.postCount || 0 }}</span>
              <span style="margin-left:12px;" class="meta-label">点赞：</span><span>{{ currentTopic.likeCount || 0 }}</span>
            </div>
          </div>

          <div v-if="currentTopic.cover" class="detail-cover">
            <span class="meta-label">封面：</span>
            <el-image :src="currentTopic.cover" fit="cover" class="cover-img"
                      :preview-src-list="[currentTopic.cover]" />
          </div>

          <el-divider content-position="left">描述</el-divider>
          <div class="topic-description">{{ currentTopic.description || '（无描述）' }}</div>

          <template v-if="activeTab === 'done'">
            <el-divider content-position="left">审核结果</el-divider>
            <div class="audit-result">
              <el-tag :type="getStatusType(currentTopic.status)">
                {{ getStatusLabel(currentTopic.status) }}
              </el-tag>
              <div v-if="currentTopic.auditRemark || currentTopic.remark" class="audit-remark">
                {{ currentTopic.auditRemark || currentTopic.remark }}
              </div>
              <div v-else class="audit-remark audit-remark-empty">（无审核意见）</div>
              <div v-if="currentTopic.auditTime" class="audit-time">审核时间：{{ currentTopic.auditTime }}</div>
            </div>
          </template>

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

        <el-empty v-else description="请从左侧选择话题查看详情" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { ArrowLeft, Search } from '@element-plus/icons-vue';
import { listTopic, getTopic, auditTopic } from '@/api/cms/topic';

const route = useRoute();
const router = useRouter();

const activeTab = ref<'pending' | 'done'>('pending');
const doneFilter = ref<'active' | 'rejected' | 'archived'>('active');
const listLoading = ref(false);
const detailLoading = ref(false);
const topicList = ref<any[]>([]);
const total = ref(0);
const currentId = ref<number | null>(null);
const currentTopic = ref<any>(null);

const auditRemark = ref('');
const approveLoading = ref(false);
const rejectLoading = ref(false);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
});

const statusMap: Record<string, { label: string; type: any }> = {
  pending:  { label: '待审核', type: 'warning' },
  active:   { label: '活跃',   type: 'success' },
  archived: { label: '已归档', type: 'info' },
  deleted:  { label: '已删除', type: 'danger' },
  rejected: { label: '已驳回', type: 'danger' }
};

function getStatusLabel(status: string) {
  return statusMap[status]?.label || status;
}
function getStatusType(status: string) {
  return statusMap[status]?.type || 'info';
}

function getQueryStatus(): string | undefined {
  if (activeTab.value === 'pending') return 'pending';
  return doneFilter.value;
}

async function getList() {
  listLoading.value = true;
  try {
    const params: any = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize
    };
    const st = getQueryStatus();
    if (st) params.status = st;
    if (queryParams.keyword) params.keyword = queryParams.keyword;
    const res = await listTopic(params);
    // 兼容两种返回结构：records/rows
    topicList.value = res.data?.records || res.rows || [];
    total.value = res.data?.total ?? res.total ?? 0;

    if (topicList.value.length > 0) {
      const exists = topicList.value.find((item: any) => item.id === currentId.value);
      if (!exists) {
        await handleSelectTopic(topicList.value[0]);
      }
    } else if (currentTopic.value && activeTab.value === 'pending') {
      currentTopic.value = null;
      currentId.value = null;
    }
  } catch (error) {
    console.error('加载话题列表失败:', error);
  } finally {
    listLoading.value = false;
  }
}

function handleTabChange() {
  queryParams.pageNum = 1;
  currentId.value = null;
  currentTopic.value = null;
  getList();
}
function handleSearch() {
  queryParams.pageNum = 1;
  currentId.value = null;
  currentTopic.value = null;
  getList();
}
function handleDoneFilterChange() {
  queryParams.pageNum = 1;
  currentId.value = null;
  currentTopic.value = null;
  getList();
}

async function handleSelectTopic(item: any) {
  if (!item) return;
  currentId.value = item.id;
  router.replace({ query: { id: String(item.id) } });
  await loadTopicDetail(item.id);
}

async function loadTopicDetail(id: any) {
  detailLoading.value = true;
  try {
    const res = await getTopic(id);
    currentTopic.value = res.data || null;
    auditRemark.value = '';
  } catch (error) {
    console.error('加载话题详情失败:', error);
    ElMessage.error('加载话题详情失败');
  } finally {
    detailLoading.value = false;
  }
}

async function handleReject() {
  if (!currentTopic.value) return;
  if (!auditRemark.value.trim()) {
    ElMessage.warning('请输入驳回原因');
    return;
  }
  rejectLoading.value = true;
  try {
    await auditTopic(currentTopic.value.id, {
      status: 'rejected',
      auditRemark: auditRemark.value
    });
    ElMessage.success('已驳回');
    await refreshAfterAudit();
  } catch (error: any) {
    ElMessage.error(error.message || '审核操作失败');
  } finally {
    rejectLoading.value = false;
  }
}

async function handleApprove() {
  if (!currentTopic.value) return;
  approveLoading.value = true;
  try {
    const payload: any = { status: 'active' };
    if (auditRemark.value.trim()) payload.auditRemark = auditRemark.value;
    await auditTopic(currentTopic.value.id, payload);
    ElMessage.success('审核通过，话题已发布');
    await refreshAfterAudit();
  } catch (error: any) {
    ElMessage.error(error.message || '审核操作失败');
  } finally {
    approveLoading.value = false;
  }
}

async function refreshAfterAudit() {
  const idx = topicList.value.findIndex((item: any) => item.id === currentId.value);
  topicList.value = topicList.value.filter((item: any) => item.id !== currentId.value);
  total.value = Math.max(0, total.value - 1);

  let nextItem: any = null;
  if (topicList.value.length > 0) {
    nextItem = topicList.value[Math.min(idx, topicList.value.length - 1)];
  }

  if (nextItem) {
    currentId.value = nextItem.id;
    router.replace({ query: { id: String(nextItem.id) } });
    await loadTopicDetail(nextItem.id);
  } else if (total.value > 0 && queryParams.pageNum > 1) {
    queryParams.pageNum = Math.max(1, queryParams.pageNum - 1);
    await getList();
  } else {
    currentTopic.value = null;
    currentId.value = null;
    router.replace({ query: {} });
  }
}

function goBack() {
  if (window.history.state && window.history.state.back) {
    router.go(-1);
  } else {
    router.push('/cms/topic');
  }
}

onMounted(async () => {
  const idFromQuery = route.query.id;
  await getList();
  if (idFromQuery) {
    const exists = topicList.value.find((item: any) => String(item.id) === String(idFromQuery));
    if (exists) {
      currentId.value = exists.id;
      await loadTopicDetail(exists.id);
    } else {
      currentId.value = Number(idFromQuery) || null;
      await loadTopicDetail(idFromQuery);
    }
  }
});
</script>

<style scoped>
.audit-container { padding: 20px; }
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
  width: 420px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 4px;
  padding: 12px;
}
.search-box { margin-bottom: 12px; }
.sub-filter { margin-bottom: 12px; }
.topic-list {
  max-height: calc(100vh - 320px);
  overflow-y: auto;
}
.topic-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.topic-item:hover { background-color: var(--el-color-primary-light-9); }
.topic-item.is-active {
  background-color: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}
.item-cover-wrap { flex-shrink: 0; }
.item-cover {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
}
.item-cover-empty { background: #f5f7fa; }
.item-main { flex: 1; min-width: 0; }
.item-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
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
.item-creator { font-size: 12px; color: #909399; }
.item-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.item-time { font-size: 12px; color: #c0c4cc; }

.audit-right {
  flex: 1;
  min-width: 0;
  max-height: calc(100vh - 140px);
  overflow-y: auto;
  padding-right: 4px;
}
.detail-card { width: 100%; }
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
.meta-item { font-size: 13px; }
.meta-label { color: #909399; }
.detail-cover { margin-bottom: 12px; }
.cover-img { width: 220px; height: 140px; border-radius: 4px; }
.topic-description {
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
  line-height: 1.7;
  white-space: pre-wrap;
  color: #303133;
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
.audit-remark-empty { color: #c0c4cc; }
.audit-time { font-size: 12px; color: #909399; }
.audit-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
