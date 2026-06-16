<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="关键词">
        <el-input
          v-model="queryParams.keyword" placeholder="请输入题目标题" clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable filterable>
          <el-option
          v-for="cat in categoryOptions"
          :key="cat.id"
          :label="cat.name"
          :value="cat.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="难度">
        <el-select v-model="queryParams.difficulty" placeholder="请选择难度" clearable>
          <el-option label="简单" value="easy" />
          <el-option label="中等" value="medium" />
          <el-option label="困难" value="hard" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已归档" value="archived" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="primary" @click="handleAdd">新增</el-button>
      <el-button type="danger" :disabled="multiple" @click="handleDelete">删除</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="questionList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="难度" width="100">
        <template #default="{ row }">
          <el-tag :type="difficultyType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ row.categoryName || '-' }}</template>
      </el-table-column>
      <el-table-column label="标签" width="200">
        <template #default="{ row }">
          <el-tag
            v-for="tag in tagList(row.tags)" :key="tag" size="small" style="margin: 2px;"
          >{{ tag }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交数" prop="submissionCount" width="100" />
      <el-table-column label="通过率" width="100">
        <template #default="{ row }">{{ row.passRate != null ? row.passRate + '%' : '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="80" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入题目描述" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" placeholder="请选择难度">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" filterable>
            <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <tag-select
            v-model="form.tags"
            :options="tagOptions"
            placeholder="请选择或输入标签，回车新增"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="公司">
          <el-input v-model="form.companies" placeholder="多个公司用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="提示">
          <el-input v-model="form.hint" type="textarea" :rows="2" placeholder="请输入提示" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="form.solution" type="textarea" :rows="4" placeholder="请输入参考答案" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已归档" value="archived" />
          </el-select>
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
  listInterviewQuestion, getInterviewQuestion, addInterviewQuestion,
  updateInterviewQuestion, delInterviewQuestion
} from '@/api/cms/interview';
import { listInterviewCategory } from '@/api/cms/interview';
import { bindTagsToEntity, getHotTags } from '@/api/cms/tag';
import TagSelect from '@/components/TagSelect.vue';

const loading = ref(true);
const questionList = ref([]);
const total = ref(0);
const categoryOptions = ref([]);
const tagOptions = ref([]);
const ids = ref([]);
const multiple = computed(() => ids.value.length === 0);

const queryParams = reactive({
  pageNum: 1, pageSize: 10,
  keyword: '', categoryId: '', difficulty: '', status: ''
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑题目' : '新增题目');
const form = ref({
  id: null, title: '', description: '', difficulty: 'easy', categoryId: null,
  tags: [], companies: '', hint: '', solution: '', sort: 0, status: 'published'
});

function difficultyLabel(d) { return { easy: '简单', medium: '中等', hard: '困难' }[d] || d; }
function difficultyType(d) { return { easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info'; }
function statusLabel(s) { return { draft: '草稿', published: '已发布', archived: '已归档' }[s] || s; }
function statusType(s) { return { draft: 'info', published: 'success', archived: 'warning' }[s] || 'info'; }
function tagList(tags) { return tags ? String(tags).split(',').map(s => s.trim()).filter(Boolean) : []; }
function tagsToStr(tags) { return (tags || []).join(','); }

async function loadCategories() {
  try {
    const res = await listInterviewCategory();
    categoryOptions.value = res.data || res.rows || [];
  } catch (e) { /* ignore */ }
}

async function loadTagOptions() {
  try {
    const res = await getHotTags('interview_question', 50);
    const rows = res.rows || res.data || [];
    tagOptions.value = rows
      .map(item => item.name || item.tagName || item)
      .filter(Boolean);
  } catch (e) { /* ignore */ }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewQuestion(queryParams);
    questionList.value = res.rows || [];
    total.value = res.total || 0;
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleQuery() { queryParams.pageNum = 1; getList(); }
function resetQuery() {
  queryParams.keyword = '';
  queryParams.categoryId = '';
  queryParams.difficulty = '';
  queryParams.status = '';
  queryParams.pageNum = 1;
  getList();
}

function handleAdd() {
  form.value = {
    id: null, title: '', description: '', difficulty: 'easy', categoryId: null,
    tags: [], companies: '', hint: '', solution: '', sort: 0, status: 'published'
  };
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewQuestion(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id,
      title: data.title || '',
      description: data.description || '',
      difficulty: data.difficulty || 'easy',
      categoryId: data.categoryId,
      tags: tagList(data.tags),
      companies: data.companies || '',
      hint: data.hint || '',
      solution: data.solution || '',
      sort: data.sort || 0,
      status: data.status || 'published'
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function submitForm() {
  if (!form.value.title) {
    ElMessage.warning('请输入标题');
    return;
  }
  try {
    const submitData = { ...form.value, tags: tagsToStr(form.value.tags) };
    let entityId = form.value.id;

    if (form.value.id) {
      await updateInterviewQuestion(submitData);
      ElMessage.success('修改成功');
    } else {
      const res = await addInterviewQuestion(submitData);
      ElMessage.success('新增成功');
      const newId = res?.id ?? res?.data?.id ?? (typeof res === 'object' ? Object.values(res)[0] : null);
      if (newId) entityId = newId;
    }

    if (entityId) {
      try {
        await bindTagsToEntity({
          entityType: 'interview_question',
          entityId,
          tagNames: Array.isArray(form.value.tags) ? form.value.tags : [],
          module: 'interview_question'
        });
      } catch (e) { /* ignore */ }
    }

    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  const toDel = row.id ? [row.id] : ids.value;
  try {
    await ElMessageBox.confirm('确认删除选中的题目？', '提示', { type: 'warning' });
    await delInterviewQuestion(toDel);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* cancel */ }
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
}

onMounted(() => {
  loadCategories();
  loadTagOptions();
  getList();
});
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form, .button-group { margin-bottom: 16px; }
</style>
