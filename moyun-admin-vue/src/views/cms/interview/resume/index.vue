<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="分类">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable filterable>
          <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="标题关键字" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="文件类型">
        <el-select v-model="queryParams.fileType" placeholder="请选择文件类型" clearable>
          <el-option label="PDF" value="pdf" />
          <el-option label="DOCX" value="docx" />
          <el-option label="DOC" value="doc" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="resumeList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="封面" width="120">
        <template #default="{ row }">
          <el-image v-if="row.cover" :src="row.cover" fit="cover" style="width: 80px; height: 60px; border-radius: 4px;" :preview-src-list="[row.cover]" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="标签" width="200">
        <template #default="{ row }">
          <el-tag
            v-for="tag in tagList(row.tags)" :key="tag" size="small" style="margin: 2px;"
          >{{ tag }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ row.categoryName || '-' }}</template>
      </el-table-column>
      <el-table-column label="文件类型" prop="fileType" width="100" />
      <el-table-column label="是否付费" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isPaid ? 'danger' : 'success'">{{ row.isPaid ? '付费' : '免费' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="下载数" prop="downloadCount" width="100" />
      <el-table-column label="点赞数" prop="likes" width="100" />
      <el-table-column label="排序" prop="sort" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'published' ? 'success' : 'info'">
            {{ row.status === 'published' ? '已发布' : '草稿' }}
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

    <pagination
      v-show="total > 0" :total="total"
      v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="封面">
          <el-input v-model="form.cover" placeholder="封面图片URL" />
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" filterable style="width: 100%;">
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
        <el-form-item label="文件类型">
          <el-select v-model="form.fileType" placeholder="请选择文件类型">
            <el-option label="PDF" value="pdf" />
            <el-option label="DOCX" value="docx" />
            <el-option label="DOC" value="doc" />
          </el-select>
        </el-form-item>
        <el-form-item label="下载URL">
          <el-input v-model="form.downloadUrl" placeholder="文件下载URL" />
        </el-form-item>
        <el-form-item label="是否付费">
          <el-switch v-model="form.isPaid" />
        </el-form-item>
        <el-form-item label="价格" v-if="form.isPaid">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="draft">草稿</el-radio>
            <el-radio value="published">已发布</el-radio>
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
  listInterviewResume, getInterviewResume, addInterviewResume,
  updateInterviewResume, delInterviewResume, listInterviewCategory
} from '@/api/cms/interview';
import { bindTagsToEntity, getHotTags } from '@/api/cms/tag';
import TagSelect from '@/components/TagSelect.vue';

const loading = ref(true);
const resumeList = ref([]);
const total = ref(0);
const categoryOptions = ref([]);
const tagOptions = ref([]);

const queryParams = reactive({
  pageNum: 1, pageSize: 10, categoryId: '', keyword: '', fileType: ''
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑简历模板' : '新增简历模板');
const form = ref({
  id: null, title: '', cover: '', categoryId: null, tags: [],
  fileType: 'pdf', downloadUrl: '', isPaid: false, price: 0,
  description: '', sort: 0, status: 'draft'
});

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
    const res = await getHotTags('interview_resume_template', 50);
    const rows = res.rows || res.data || [];
    tagOptions.value = rows
      .map(item => item.name || item.tagName || item)
      .filter(Boolean);
  } catch (e) { /* ignore */ }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewResume(queryParams);
    resumeList.value = res.rows || [];
    total.value = res.total || 0;
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleQuery() { queryParams.pageNum = 1; getList(); }
function resetQuery() {
  queryParams.categoryId = ''; queryParams.keyword = ''; queryParams.fileType = '';
  queryParams.pageNum = 1; getList();
}

function handleAdd() {
  form.value = {
    id: null, title: '', cover: '', categoryId: null, tags: [],
    fileType: 'pdf', downloadUrl: '', isPaid: false, price: 0,
    description: '', sort: 0, status: 'draft'
  };
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewResume(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id, title: data.title || '', cover: data.cover || '',
      categoryId: data.categoryId, tags: tagList(data.tags),
      fileType: data.fileType || 'pdf',
      downloadUrl: data.downloadUrl || '', isPaid: !!data.isPaid,
      price: data.price || 0, description: data.description || '',
      sort: data.sort || 0, status: data.status || 'draft'
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function submitForm() {
  if (!form.value.title) { ElMessage.warning('请输入标题'); return; }
  try {
    const submitData = { ...form.value, tags: tagsToStr(form.value.tags) };
    let entityId = form.value.id;

    if (form.value.id) {
      await updateInterviewResume(submitData);
      ElMessage.success('修改成功');
    } else {
      const res = await addInterviewResume(submitData);
      ElMessage.success('新增成功');
      const newId = res?.id ?? res?.data?.id;
      if (newId) entityId = newId;
    }

    if (entityId) {
      try {
        await bindTagsToEntity({
          entityType: 'interview_resume_template',
          entityId,
          tagNames: Array.isArray(form.value.tags) ? form.value.tags : [],
          module: 'interview_resume_template'
        });
      } catch (e) { /* ignore */ }
    }

    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该模板？', '提示', { type: 'warning' });
    await delInterviewResume(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* ignore */ }
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
