<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="话题ID">
        <el-input
          v-model="queryParams.topicId"
          placeholder="请输入话题ID"
          clearable
          type="number"
        />
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
      :data="postList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="话题ID" prop="topicId" width="100" />
      <el-table-column label="话题标题" width="150">
        <template #default="{ row }">
          <span>{{ row.topicTitle || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="楼层" prop="floor" width="80" />
      <el-table-column label="内容" prop="content" min-width="300" show-overflow-tooltip />
      <el-table-column label="作者" width="120">
        <template #default="{ row }">
          <span>{{ row.userNickname || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="图片数" width="80">
        <template #default="{ row }">
          <span>{{ row.images ? (Array.isArray(row.images) ? row.images.length : 0) : 0 }}</span>
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
      <el-table-column label="是否删除" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isDeleted ? 'danger' : 'success'">
            {{ row.isDeleted ? '已删除' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180" prop="createdTime" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Refresh, Delete } from '@element-plus/icons-vue';
import { listPost, delPost } from '@/api/cms/topic';

const loading = ref(true);
const postList = ref<any[]>([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  topicId: ''
});

const ids = ref<number[]>([]);
const multiple = computed(() => ids.value.length === 0);

async function getList() {
  loading.value = true;
  try {
    const params: any = { ...queryParams };
    if (params.topicId) {
      params.topicId = Number(params.topicId);
    } else {
      delete params.topicId;
    }
    const res = await listPost(params);
    postList.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (error) {
    console.error('加载观点列表失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.topicId = '';
  queryParams.pageNum = 1;
  getList();
}

async function handleDelete(row: any) {
  const postIds = row.id ? [row.id] : ids.value;
  try {
    await ElMessageBox.confirm(
      `是否确认删除观点ID为"${postIds.join(',')}"的数据项？`,
      '警告',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    for (const id of postIds) {
      await delPost(id);
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
</style>