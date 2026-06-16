<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="分类名称">
        <el-input v-model="queryParams.name" placeholder="请输入名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
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
      <el-table-column label="名称" prop="name" min-width="150" />
      <el-table-column label="Slug" prop="slug" width="140" />
      <el-table-column label="图标" width="100">
        <template #default="{ row }">
          <el-icon v-if="row.icon" :size="20"><component :is="row.icon" /></el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip />
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
        <el-form-item label="名称"><el-input v-model="form.name" placeholder="请输入名称" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="英文标识，如 algorithm" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="图标名称" /></el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
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
  listInterviewCategory, getInterviewCategory, addInterviewCategory,
  updateInterviewCategory, delInterviewCategory
} from '@/api/cms/interview';

const loading = ref(true);
const allList = ref([]);
const queryParams = reactive({ name: '', status: '' });

const filteredList = computed(() => {
  return allList.value.filter(item => {
    const matchName = !queryParams.name || (item.name && item.name.includes(queryParams.name));
    const matchStatus = !queryParams.status || item.status === queryParams.status;
    return matchName && matchStatus;
  });
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑分类' : '新增分类');
const form = ref({ id: null, name: '', slug: '', icon: '', description: '', sort: 0, status: 'active' });

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewCategory();
    allList.value = res.data || res.rows || [];
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function resetQuery() { queryParams.name = ''; queryParams.status = ''; }

function handleAdd() {
  form.value = { id: null, name: '', slug: '', icon: '', description: '', sort: 0, status: 'active' };
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewCategory(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id, name: data.name || '', slug: data.slug || '', icon: data.icon || '',
      description: data.description || '', sort: data.sort || 0, status: data.status || 'active'
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function submitForm() {
  if (!form.value.name) { ElMessage.warning('请输入名称'); return; }
  try {
    if (form.value.id) {
      await updateInterviewCategory(form.value);
      ElMessage.success('修改成功');
    } else {
      await addInterviewCategory(form.value);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该分类？', '提示', { type: 'warning' });
    await delInterviewCategory(row.id);
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
