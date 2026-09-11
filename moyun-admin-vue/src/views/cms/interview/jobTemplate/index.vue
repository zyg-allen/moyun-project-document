<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="名称">
        <el-input
          v-model="queryParams.name" placeholder="请输入模板名称" clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="类别">
        <el-select v-model="queryParams.category" placeholder="请选择类别" clearable filterable>
          <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="primary" v-hasPermi="['cms:interview:jobTemplate:create']" @click="handleAdd">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="jobTemplateList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="名称" prop="name" min-width="160" show-overflow-tooltip />
      <el-table-column label="类别" prop="category" width="110">
        <template #default="{ row }">{{ row.category || '-' }}</template>
      </el-table-column>
      <el-table-column label="岗位编码" prop="positionCode" width="120">
        <template #default="{ row }">{{ row.positionCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="难度" width="90">
        <template #default="{ row }">
          <el-tag :type="difficultyType(row.difficulty)">{{ difficultyLabel(row.difficulty) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="出题数量" prop="questionCount" width="100" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160">
        <template #default="{ row }">{{ parseTime(row.createTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['cms:interview:jobTemplate:update']" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="success" v-hasPermi="['cms:interview:jobTemplate:update']" @click="handleBindQuestions(row)">关联题目</el-button>
          <el-button link type="danger" v-hasPermi="['cms:interview:jobTemplate:remove']" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px" :close-on-click-modal="false">
      <el-form :model="form" label-width="110px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" placeholder="请输入模板名称，如：Java后端工程师" />
        </el-form-item>
        <el-form-item label="岗位类别">
          <el-select v-model="form.category" placeholder="请选择或输入岗位类别" filterable allow-create clearable>
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位编码">
          <el-input v-model="form.positionCode" placeholder="请输入岗位编码" />
        </el-form-item>
        <el-form-item label="模板描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入模板描述" />
        </el-form-item>
        <el-form-item label="岗位JD原文">
          <el-input v-model="form.jdText" type="textarea" :rows="5" placeholder="请粘贴岗位 JD 原文，可用于 LLM 提取关键词" />
          <div>
            <el-button style="margin-top: 6px;" type="primary" plain :loading="extracting" @click="handleExtractKeywords">LLM 提取关键词</el-button>
            <span class="form-tip" style="margin-left: 12px;">提取结果将填充到「岗位关键词」</span>
          </div>
        </el-form-item>
        <el-form-item label="岗位关键词">
          <el-input v-model="form.keywords" placeholder="多个关键词用英文逗号分隔，可通过 LLM 提取自动填充" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" placeholder="请选择难度">
            <el-option label="简单" value="easy" />
            <el-option label="中等" value="medium" />
            <el-option label="困难" value="hard" />
          </el-select>
        </el-form-item>
        <el-form-item label="出题数量">
          <el-input-number v-model="form.questionCount" :min="1" :max="100" />
          <span class="form-tip" style="margin-left: 12px;">默认出题数量</span>
        </el-form-item>
        <el-form-item label="出题权重">
          <el-input
            v-model="form.weights" type="textarea" :rows="3"
            placeholder='{"job":40,"resume":20,"weak":20,"random":20}'
            style="font-family: monospace;"
          />
          <div class="form-tip">JSON 格式：job 岗位题 / resume 简历题 / weak 弱项题 / random 随机题的出题权重</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="inactive" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 关联题目弹窗 -->
    <el-dialog v-model="bindVisible" :title="bindDialogTitle" width="860px" :close-on-click-modal="false">
      <el-table
        ref="questionTableRef"
        v-loading="bindLoading"
        :data="questionOptions"
        row-key="id"
        max-height="480"
        @selection-change="handleBindSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="标题" prop="title" min-width="240" show-overflow-tooltip />
        <el-table-column label="难度" width="90">
          <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="questionStatusType(row.status)">{{ questionStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <span class="form-tip" style="margin-right: 12px;">已选 {{ selectedQuestionIds.length }} 题（保存为全量覆盖）</span>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindSubmitting" @click="submitBind">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listJobTemplate, getJobTemplate, addJobTemplate, updateJobTemplate, delJobTemplate,
  extractJobTemplateKeywords, getBoundQuestionIds, bindJobTemplateQuestions
} from '@/api/cms/interviewJobTemplate';
import { listInterviewQuestion } from '@/api/cms/interview';

const loading = ref(true);
const jobTemplateList = ref([]);
const total = ref(0);

// 常用岗位类别（可输入自定义值）
const categoryOptions = ref(['技术', '产品', '运营', '设计', '市场', '职能']);

const queryParams = reactive({
  pageNum: 1, pageSize: 10,
  name: '', category: '', status: ''
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑岗位模板' : '新增岗位模板');

const extracting = ref(false);

function makeDefaultForm() {
  return {
    id: null,
    name: '',
    category: '',
    positionCode: '',
    description: '',
    jdText: '',
    keywords: '',
    difficulty: 'easy',
    questionCount: 5,
    weights: '',
    status: 'active',
  };
}

const form = ref(makeDefaultForm());

function difficultyLabel(d) { return { easy: '简单', medium: '中等', hard: '困难' }[d] || d || '-'; }
function difficultyType(d) { return { easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info'; }
function questionStatusLabel(s) { return { draft: '草稿', published: '已发布', archived: '已归档' }[s] || s; }
function questionStatusType(s) { return { draft: 'info', published: 'success', archived: 'warning' }[s] || 'info'; }

async function getList() {
  loading.value = true;
  try {
    const res = await listJobTemplate(queryParams);
    jobTemplateList.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleQuery() { queryParams.pageNum = 1; getList(); }
function resetQuery() {
  queryParams.name = '';
  queryParams.category = '';
  queryParams.status = '';
  queryParams.pageNum = 1;
  getList();
}

function handleAdd() {
  form.value = makeDefaultForm();
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getJobTemplate(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id,
      name: data.name || '',
      category: data.category || '',
      positionCode: data.positionCode || '',
      description: data.description || '',
      jdText: data.jdText || '',
      keywords: data.keywords || '',
      difficulty: data.difficulty || 'easy',
      questionCount: data.questionCount != null ? data.questionCount : 5,
      weights: data.weights || '',
      status: data.status || 'active',
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

/** LLM 提取 JD 关键词（失败自动回退规则分词），结果填充到 keywords */
async function handleExtractKeywords() {
  if (!form.value.jdText || !form.value.jdText.trim()) {
    ElMessage.warning('请先填写岗位 JD 原文');
    return;
  }
  extracting.value = true;
  try {
    const res = await extractJobTemplateKeywords(form.value.jdText);
    const keywords = res.data || [];
    if (keywords.length > 0) {
      form.value.keywords = keywords.join(',');
      ElMessage.success('关键词提取成功，已填充 ' + keywords.length + ' 个');
    } else {
      ElMessage.warning('未能提取到关键词');
    }
  } catch (e) { /* 错误已由拦截器提示 */ } finally {
    extracting.value = false;
  }
}

async function submitForm() {
  if (!form.value.name) {
    ElMessage.warning('请输入模板名称');
    return;
  }
  try {
    if (form.value.id) {
      await updateJobTemplate(form.value);
      ElMessage.success('修改成功');
    } else {
      await addJobTemplate(form.value);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    getList();
  } catch (e) { /* 错误已由拦截器提示 */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除岗位模板「' + row.name + '」？', '提示', { type: 'warning' });
    await delJobTemplate(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* cancel */ }
}

// ==================== 关联题目 ====================
const bindVisible = ref(false);
const bindLoading = ref(false);
const bindSubmitting = ref(false);
const questionOptions = ref([]);
const selectedQuestionIds = ref([]);
const currentTemplate = ref(null);
const questionTableRef = ref(null);

const bindDialogTitle = computed(() =>
  currentTemplate.value ? '关联题目 - ' + currentTemplate.value.name : '关联题目'
);

async function handleBindQuestions(row) {
  currentTemplate.value = row;
  bindVisible.value = true;
  bindLoading.value = true;
  selectedQuestionIds.value = [];
  try {
    const [qRes, bRes] = await Promise.all([
      listInterviewQuestion({ pageNum: 1, pageSize: 200 }),
      getBoundQuestionIds(row.id)
    ]);
    questionOptions.value = qRes.data.records || [];
    const boundIds = bRes.data || [];
    // 等表格渲染完成后回显已关联题目勾选
    nextTick(() => {
      questionTableRef.value?.clearSelection();
      questionOptions.value.forEach(q => {
        if (boundIds.includes(q.id)) {
          questionTableRef.value?.toggleRowSelection(q, true);
        }
      });
    });
  } catch (e) { /* ignore */ } finally {
    bindLoading.value = false;
  }
}

function handleBindSelectionChange(selection) {
  selectedQuestionIds.value = selection.map(item => item.id);
}

async function submitBind() {
  bindSubmitting.value = true;
  try {
    await bindJobTemplateQuestions(currentTemplate.value.id, selectedQuestionIds.value);
    ElMessage.success('关联题目保存成功');
    bindVisible.value = false;
    getList();
  } catch (e) { /* 错误已由拦截器提示 */ } finally {
    bindSubmitting.value = false;
  }
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.app-container { padding: 20px; }
.search-form, .button-group { margin-bottom: 16px; }
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>