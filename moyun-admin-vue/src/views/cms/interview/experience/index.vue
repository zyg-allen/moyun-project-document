<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="待审核" value="pending" />
          <el-option label="已发布" value="published" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item label="公司">
        <el-input v-model="queryParams.company" placeholder="请输入公司名" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="标题/内容" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="primary" @click="handleAdd">新增面经</el-button>
    </div>

    <el-table v-loading="loading" :data="experienceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="标题" prop="title" min-width="220" show-overflow-tooltip />
      <el-table-column label="公司" prop="company" width="140" />
      <el-table-column label="岗位" prop="position" width="140" />
      <el-table-column label="标签" width="200">
        <template #default="{ row }">
          <el-tag
            v-for="tag in tagList(row.tags)" :key="tag" size="small" style="margin: 2px;"
          >{{ tag }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="作者ID" prop="authorId" width="100" />
      <el-table-column label="浏览" prop="views" width="80" />
      <el-table-column label="点赞" prop="likes" width="80" />
      <el-table-column label="评论" prop="comments" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isTop" :active-value="true" :inactive-value="false" @change="handleTop(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">详情</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="detailVisible" title="面经详情" width="720px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="公司">{{ detail.company }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ detail.position }}</el-descriptions-item>
        <el-descriptions-item label="作者ID">{{ detail.authorId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="是否置顶">{{ detail.isTop ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="浏览/点赞/评论">{{ detail.views }} / {{ detail.likes }} / {{ detail.comments }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="2">
          <el-tag v-for="tag in tagList(detail.tags)" :key="tag" size="small" style="margin: 2px;">{{ tag }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">{{ detail.content }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        <el-form-item label="公司"><el-input v-model="form.company" placeholder="请输入公司" /></el-form-item>
        <el-form-item label="岗位"><el-input v-model="form.position" placeholder="请输入岗位" /></el-form-item>
        <el-form-item label="标签">
          <tag-select
            v-model="form.tags"
            :options="tagOptions"
            placeholder="请选择或输入标签，回车新增"
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入面经内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="待审核" value="pending" />
            <el-option label="已发布" value="published" />
            <el-option label="已拒绝" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" :active-value="true" :inactive-value="false" />
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
  listInterviewExperience, getInterviewExperience, addInterviewExperience,
  updateInterviewExperience, auditInterviewExperience,
  topInterviewExperience, delInterviewExperience
} from '@/api/cms/interview';
import { bindTagsToEntity, getHotTags } from '@/api/cms/tag';
import TagSelect from '@/components/TagSelect.vue';

const loading = ref(true);
const experienceList = ref([]);
const total = ref(0);
const tagOptions = ref([]);

const queryParams = reactive({
  pageNum: 1, pageSize: 10, status: '', company: '', keyword: ''
});

const detailVisible = ref(false);
const detail = ref({});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑面经' : '新增面经');
const form = ref({
  id: null, title: '', company: '', position: '', tags: [],
  content: '', status: 'pending', isTop: false
});

function statusLabel(s) {
  return { pending: '待审核', published: '已发布', rejected: '已拒绝' }[s] || s;
}
function statusType(s) {
  return { pending: 'warning', published: 'success', rejected: 'danger' }[s] || 'info';
}
function tagList(tags) { return tags ? String(tags).split(',').map(s => s.trim()).filter(Boolean) : []; }
function tagsToStr(tags) { return (tags || []).join(','); }

async function loadTagOptions() {
  try {
    const res = await getHotTags('interview_experience', 50);
    const rows = res.rows || res.data || [];
    tagOptions.value = rows
      .map(item => item.name || item.tagName || item)
      .filter(Boolean);
  } catch (e) { /* ignore */ }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewExperience(queryParams);
    experienceList.value = res.rows || [];
    total.value = res.total || 0;
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleQuery() { queryParams.pageNum = 1; getList(); }
function resetQuery() {
  queryParams.status = ''; queryParams.company = ''; queryParams.keyword = '';
  queryParams.pageNum = 1; getList();
}

async function handleView(row) {
  try {
    const res = await getInterviewExperience(row.id);
    detail.value = res.data || {};
    detailVisible.value = true;
  } catch (e) { /* ignore */ }
}

function handleAdd() {
  form.value = {
    id: null, title: '', company: '', position: '', tags: [],
    content: '', status: 'pending', isTop: false
  };
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewExperience(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id,
      title: data.title || '',
      company: data.company || '',
      position: data.position || '',
      tags: tagList(data.tags),
      content: data.content || '',
      status: data.status || 'pending',
      isTop: !!data.isTop
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
      await updateInterviewExperience(submitData);
      ElMessage.success('修改成功');
    } else {
      const res = await addInterviewExperience(submitData);
      ElMessage.success('新增成功');
      const newId = res?.id ?? res?.data?.id;
      if (newId) entityId = newId;
    }

    if (entityId) {
      try {
        await bindTagsToEntity({
          entityType: 'interview_experience',
          entityId,
          tagNames: Array.isArray(form.value.tags) ? form.value.tags : [],
          module: 'interview_experience'
        });
      } catch (e) { /* ignore */ }
    }

    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

async function handleAudit(row, status) {
  try {
    const remark = status === 'published' ? '审核通过' : '内容不符合要求';
    await ElMessageBox.confirm(
      `确认${status === 'published' ? '通过' : '拒绝'}该面经？`,
      '提示', { type: 'warning' }
    );
    await auditInterviewExperience({ id: row.id, status, remark });
    ElMessage.success('操作成功');
    getList();
  } catch (e) { /* ignore */ }
}

async function handleTop(row) {
  try {
    await topInterviewExperience({ id: row.id, isTop: row.isTop });
    ElMessage.success('操作成功');
  } catch (e) {
    row.isTop = !row.isTop;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该面经？', '提示', { type: 'warning' });
    await delInterviewExperience(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* ignore */ }
}

function handleSelectionChange() { /* noop */ }

onMounted(() => {
  loadTagOptions();
  getList();
});
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form, .button-group { margin-bottom: 16px; }
</style>
