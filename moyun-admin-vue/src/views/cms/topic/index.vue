<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="话题标题">
        <el-input
          v-model="queryParams.keyword"
          placeholder="请输入话题标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="全部" value="" />
          <el-option label="活跃" value="active" />
          <el-option label="归档" value="archived" />
          <el-option label="删除" value="deleted" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="danger" :disabled="multiple" @click="handleDelete">
        <el-icon><Delete /></el-icon> 删除
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="topicList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="封面" width="120">
        <template #default="{ row }">
          <el-image
            v-if="row.cover"
            :src="row.cover"
            fit="cover"
            class="topic-cover"
            :preview-src-list="[row.cover]"
          />
          <span v-else class="no-cover">无封面</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip />
      <el-table-column label="发起人" width="120">
        <template #default="{ row }">
          <span>{{ row.creatorNickname || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="80">
        <template #default="{ row }">
          <el-switch
            v-model="row.pinned"
            :active-value="1"
            :inactive-value="0"
            :disabled="!$auth.hasPermi('cms:topic:edit')"
            @change="handlePinnedChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="精选" width="80">
        <template #default="{ row }">
          <el-switch
            v-model="row.isFeatured"
            :active-value="1"
            :inactive-value="0"
            :disabled="!$auth.hasPermi('cms:topic:edit')"
            @change="handleFeaturedChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="浏览" width="80">
        <template #default="{ row }">
          <span>{{ row.viewCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="观点" width="80">
        <template #default="{ row }">
          <span>{{ row.postCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="点赞" width="80">
        <template #default="{ row }">
          <span>{{ row.likeCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评论" width="80">
        <template #default="{ row }">
          <span>{{ row.commentCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180" prop="createdTime" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)" v-hasPermi="['cms:topic:query']">查看</el-button>
          <el-button link type="warning" @click="handleAuditPage(row)" v-hasPermi="['cms:topic:audit']">审核</el-button>
          <el-button link type="info" @click="handleChangeStatus(row)" v-hasPermi="['cms:topic:edit']">状态</el-button>
          <el-button link type="danger" @click="handleDelete(row)" v-hasPermi="['cms:topic:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox, ElSelect, ElOption } from 'element-plus';
import { Search, Refresh, Delete } from '@element-plus/icons-vue';
import { listTopic, getTopic, updateTopicStatus, updateTopicPinned, featureTopic, delTopic } from '@/api/cms/topic';

const router = useRouter();
const loading = ref(true);
const topicList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: ''
});

const ids = ref<number[]>([]);
const multiple = computed(() => ids.value.length === 0);

const statusMap = {
  active: { label: '活跃', type: 'success' },
  archived: { label: '归档', type: 'warning' },
  deleted: { label: '删除', type: 'danger' }
};

function getStatusLabel(status: string) {
  return statusMap[status]?.label || status;
}

function getStatusType(status: string) {
  return statusMap[status]?.type || 'info';
}

async function getList() {
  loading.value = true;
  try {
    const res = await listTopic(queryParams);
    topicList.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (error) {
    console.error('加载话题列表失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.keyword = '';
  queryParams.status = '';
  queryParams.pageNum = 1;
  getList();
}

/** 跳转到话题审核页（与文章/专栏审核入口一致） */
function handleAuditPage(row: any) {
  router.push({
    path: '/cms/topic-audit',
    query: { id: row.id }
  });
}

function handleView(row: any) {
  ElMessageBox.alert(JSON.stringify(row, null, 2), '话题详情', {
    type: 'info',
    dangerouslyUseHTMLString: false,
    customClass: 'topic-detail-dialog'
  });
}

function handleChangeStatus(row: any) {
  const currentStatus = row.status || 'active';
  const options = [
    { label: '活跃', value: 'active' },
    { label: '归档', value: 'archived' },
    { label: '删除', value: 'deleted' }
  ].filter(o => o.value !== currentStatus);

  ElMessageBox.confirm(
    `<el-select v-model="newStatus" placeholder="请选择状态">
      ${options.map(o => `<el-option label="${o.label}" value="${o.value}" />`).join('')}
    </el-select>`,
    '修改状态',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
      dangerouslyUseHTMLString: true
    }
  ).then(async () => {
    try {
      const statusOptions = ['active', 'archived', 'deleted'].filter(s => s !== currentStatus);
      const newStatus = statusOptions[0];
      await updateTopicStatus(row.id, newStatus);
      row.status = newStatus;
      ElMessage.success('状态修改成功');
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败');
    }
  }).catch(() => {});
}

async function handlePinnedChange(row: any) {
  try {
    await updateTopicPinned(row.id, row.pinned);
    ElMessage.success(row.pinned ? '设置置顶成功' : '取消置顶成功');
  } catch (error: any) {
    row.pinned = row.pinned ? 0 : 1;
    ElMessage.error(error.message || '操作失败');
  }
}

async function handleFeaturedChange(row: any) {
  try {
    await featureTopic(row.id);
    row.isFeatured = 1;
    ElMessage.success('加精成功');
  } catch (error: any) {
    row.isFeatured = 0;
    ElMessage.error(error.message || '操作失败');
  }
}

async function handleDelete(row: any) {
  const topicIds = row.id ? [row.id] : ids.value;
  try {
    await ElMessageBox.confirm(
      `是否确认删除话题ID为"${topicIds.join(',')}"的数据项？`,
      '警告',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    for (const id of topicIds) {
      await delTopic(id);
    }
    ElMessage.success('删除成功');
    getList();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败');
    }
  }
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map(item => item.id);
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.topic-cover {
  width: 80px;
  height: 60px;
}
.no-cover {
  color: #999;
  font-size: 12px;
}
</style>