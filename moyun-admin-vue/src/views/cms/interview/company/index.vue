<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="公司名称">
        <el-input v-model="queryParams.name" placeholder="请输入公司名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="行业">
        <el-input v-model="queryParams.industry" placeholder="请输入行业" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="启用" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="filteredList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="名称" prop="name" min-width="180" />
      <el-table-column label="Logo" width="100">
        <template #default="{ row }">
          <el-image v-if="row.logo" :src="row.logo" fit="cover" style="width: 40px; height: 40px; border-radius: 4px;" :preview-src-list="[row.logo]" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="行业" prop="industry" width="140" />
      <el-table-column label="题目数" prop="questionCount" width="100" />
      <el-table-column label="排序" prop="sort" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">
            {{ row.status === 'active' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="请输入公司名称" /></el-form-item>
        <el-form-item label="Logo"><el-input v-model="form.logo" placeholder="Logo图片URL" /></el-form-item>
        <el-form-item label="行业"><el-input v-model="form.industry" placeholder="如 互联网 / 金融" /></el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterviewCompany, getInterviewCompany, addInterviewCompany,
  updateInterviewCompany, delInterviewCompany
} from '@/api/cms/interview';

const loading = ref(true);
const allList = ref([]);
const queryParams = reactive({ name: '', industry: '', status: '' });
const filteredList = computed(() => {
  return allList.value.filter(item => {
    const nameOk = !queryParams.name || (item.name && item.name.includes(queryParams.name));
    const indOk = !queryParams.industry || (item.industry && item.industry.includes(queryParams.industry));
    const statusOk = !queryParams.status || item.status === queryParams.status;
    return nameOk && indOk && statusOk;
  });
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑公司' : '新增公司');
const form = ref({
  id: null, name: '', logo: '', industry: '', description: '', sort: 0, status: 'active'
});

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewCompany();
    allList.value = res.data || [];
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function resetQuery() {
  queryParams.name = ''; queryParams.industry = ''; queryParams.status = '';
}

function handleAdd() {
  form.value = { id: null, name: '', logo: '', industry: '', description: '', sort: 0, status: 'active' };
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewCompany(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id, name: data.name || '', logo: data.logo || '',
      industry: data.industry || '', description: data.description || '',
      sort: data.sort || 0, status: data.status || 'active'
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function submitForm() {
  if (!form.value.name) { ElMessage.warning('请输入名称'); return; }
  try {
    if (form.value.id) {
      await updateInterviewCompany(form.value);
      ElMessage.success('修改成功');
    } else {
      await addInterviewCompany(form.value);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该公司标签？', '提示', { type: 'warning' });
    await delInterviewCompany(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* ignore */ }
}

onMounted(() => getList());
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form, .button-group { margin-bottom: 16px; }
</style>
