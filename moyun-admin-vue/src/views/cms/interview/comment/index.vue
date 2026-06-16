<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="published" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="内容关键字" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="面经ID">
        <el-input v-model="queryParams.experienceId" placeholder="面经ID" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="commentList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="面经ID" prop="experienceId" width="100" />
      <el-table-column label="用户ID" prop="userId" width="100" />
      <el-table-column label="内容" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">{{ row.content }}</template>
      </el-table-column>
      <el-table-column label="点赞" prop="likes" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时间" prop="createTime" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="handleAudit(row, 'published')" v-if="row.status !== 'published'">通过</el-button>
          <el-button link type="warning" @click="handleAudit(row, 'rejected')" v-if="row.status !== 'rejected'">拒绝</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterviewComment, auditInterviewComment, delInterviewComment
} from '@/api/cms/interview';

const loading = ref(true);
const commentList = ref([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1, pageSize: 10, status: '', keyword: '', experienceId: ''
});

function statusLabel(s) {
  return { pending: '待审核', published: '已通过', rejected: '已拒绝' }[s] || s;
}
function statusType(s) {
  return { pending: 'warning', published: 'success', rejected: 'danger' }[s] || 'info';
}

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewComment(queryParams);
    commentList.value = res.rows || [];
    total.value = res.total || 0;
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleQuery() { queryParams.pageNum = 1; getList(); }
function resetQuery() {
  queryParams.status = ''; queryParams.keyword = ''; queryParams.experienceId = '';
  queryParams.pageNum = 1; getList();
}

async function handleAudit(row, status) {
  try {
    const remark = status === 'published' ? '审核通过' : '内容不适当';
    await ElMessageBox.confirm(
      `确认${status === 'published' ? '通过' : '拒绝'}该评论？`,
      '提示', { type: 'warning' }
    );
    await auditInterviewComment({ id: row.id, status, remark });
    ElMessage.success('操作成功');
    getList();
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该评论？', '提示', { type: 'warning' });
    await delInterviewComment(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* ignore */ }
}

onMounted(() => getList());
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form { margin-bottom: 16px; }
</style>
