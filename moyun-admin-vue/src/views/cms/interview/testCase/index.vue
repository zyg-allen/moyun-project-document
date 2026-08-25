<template>
  <div class="app-container">
    <!-- 题目信息头 -->
    <el-card class="question-header" shadow="never">
      <div class="header-inner">
        <div class="header-left">
          <el-button link @click="goBack">
            <el-icon><ArrowLeft /></el-icon>
            返回题目列表
          </el-button>
          <span class="question-title">
            题目 #{{ questionId }}
            <span v-if="question.title" class="title-text">{{ question.title }}</span>
          </span>
        </div>
        <div class="header-right">
          <el-tag v-if="question.questionType" :type="typeTagType(question.questionType)">
            {{ typeLabel(question.questionType) }}
          </el-tag>
          <el-tag :type="difficultyType(question.difficulty)">
            {{ difficultyLabel(question.difficulty) }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <!-- 用例列表 -->
    <div class="button-group">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增用例
      </el-button>
      <el-button @click="getList">刷新</el-button>
      <span class="case-summary">
        共 {{ caseList.length }} 条用例，
        样例 {{ sampleCount }} 条 / 隐藏 {{ caseList.length - sampleCount }} 条
      </span>
    </div>

    <el-table v-loading="loading" :data="caseList" empty-text="暂无测试用例，点击新增">
      <el-table-column label="序号" type="index" width="70" align="center" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isSample ? 'success' : 'info'" size="small">
            {{ row.isSample ? '样例' : '隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="输入" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <pre class="case-text">{{ row.input || '(无输入)' }}</pre>
        </template>
      </el-table-column>
      <el-table-column label="期望输出" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <pre class="case-text">{{ row.expectedOutput || '(无输出)' }}</pre>
        </template>
      </el-table-column>
      <el-table-column label="说明" prop="explanation" min-width="160" show-overflow-tooltip />
      <el-table-column label="排序" prop="orderNum" width="80" align="center" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 用例新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="是否样例">
          <el-switch v-model="form.isSample" active-text="样例（前端展示）" inactive-text="隐藏（仅判题）" />
          <div class="form-tip">样例用例会在前端详情页展示题意，隐藏用例仅用于判题，不公开 input/output</div>
        </el-form-item>
        <el-form-item label="标准输入" prop="input">
          <el-input
            v-model="form.input"
            type="textarea"
            :rows="4"
            placeholder="程序通过 stdin 接收的输入，多行直接换行输入"
            style="font-family: monospace;"
          />
        </el-form-item>
        <el-form-item label="期望输出" prop="expectedOutput">
          <el-input
            v-model="form.expectedOutput"
            type="textarea"
            :rows="4"
            placeholder="程序 stdout 应输出的内容，多行直接换行"
            style="font-family: monospace;"
          />
        </el-form-item>
        <el-form-item label="用例说明">
          <el-input v-model="form.explanation" placeholder="可选，样例用例可附简短说明" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNum" :min="0" />
          <div class="form-tip">数字越小越靠前，判题与展示均按此排序</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowLeft, Plus } from '@element-plus/icons-vue';
import {
  listTestCase, addTestCase, updateTestCase, delTestCase,
  getInterviewQuestion
} from '@/api/cms/interview';

const route = useRoute();
const router = useRouter();

const questionId = computed(() => route.params.questionId);

const loading = ref(false);
const submitting = ref(false);
const caseList = ref([]);
const question = ref({});

const dialogVisible = ref(false);
const dialogTitle = computed(() => (form.id ? '编辑测试用例' : '新增测试用例'));
const formRef = ref(null);
const form = ref({
  id: null,
  questionId: null,
  input: '',
  expectedOutput: '',
  isSample: false,
  orderNum: 0,
  explanation: ''
});

const rules = {
  expectedOutput: [{ required: true, message: '期望输出不能为空', trigger: 'blur' }]
};

const sampleCount = computed(() => caseList.value.filter(c => c.isSample).length);

function difficultyLabel(d) { return { easy: '简单', medium: '中等', hard: '困难' }[d] || d || ''; }
function difficultyType(d) { return { easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info'; }
function typeLabel(t) { return { algorithm: '算法', bagwen: '八股', system_design: '系统设计', project: '项目', hr: 'HR' }[t] || t; }
function typeTagType(t) { return { algorithm: 'primary', bagwen: 'warning', system_design: 'info', project: 'success', hr: 'danger' }[t] || 'info'; }

function goBack() {
  // 返回路径三级回退：来源路径（query.from）> activeMenu > 默认题库页
  // 菜单调整后来源路径自动跟随，无需改代码
  const from = route.query.from;
  const fallback = (route.meta && route.meta.activeMenu) || '/portal/interview/questionTab';
  router.push(typeof from === 'string' && from ? from : fallback);
}

async function loadQuestion() {
  try {
    const res = await getInterviewQuestion(questionId.value);
    question.value = res.data || {};
  } catch (e) { /* ignore */ }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listTestCase(questionId.value);
    caseList.value = res.data || [];
  } catch (e) {
    ElMessage.error('加载用例失败');
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  resetForm();
  form.value.questionId = questionId.value;
  form.value.orderNum = caseList.value.length;
  dialogVisible.value = true;
}

function handleEdit(row) {
  resetForm();
  Object.assign(form.value, {
    id: row.id,
    questionId: row.questionId || questionId.value,
    input: row.input || '',
    expectedOutput: row.expectedOutput || '',
    isSample: !!row.isSample,
    orderNum: row.orderNum || 0,
    explanation: row.explanation || ''
  });
  dialogVisible.value = true;
}

function resetForm() {
  form.value = {
    id: null,
    questionId: questionId.value,
    input: '',
    expectedOutput: '',
    isSample: false,
    orderNum: 0,
    explanation: ''
  };
  if (formRef.value) {
    formRef.value.clearValidate();
  }
}

async function submitForm() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch (e) {
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      questionId: form.value.questionId,
      input: form.value.input,
      expectedOutput: form.value.expectedOutput,
      isSample: form.value.isSample,
      orderNum: form.value.orderNum,
      explanation: form.value.explanation
    };
    if (form.value.id) {
      await updateTestCase(form.value.id, payload);
      ElMessage.success('修改成功');
    } else {
      await addTestCase(payload);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    getList();
  } catch (e) {
    ElMessage.error(e?.message || '操作失败');
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用例 #${row.id}？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    });
    await delTestCase(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败');
    }
  }
}

onMounted(() => {
  loadQuestion();
  getList();
});
</script>

<style lang="scss" scoped>
.question-header {
  margin-bottom: 12px;
  .header-inner {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }
  .question-title {
    font-size: 16px;
    font-weight: 600;
    .title-text {
      margin-left: 8px;
      font-weight: 400;
      color: #606266;
    }
  }
  .header-right {
    display: flex;
    gap: 8px;
  }
}
.button-group {
  margin: 12px 0;
  display: flex;
  align-items: center;
  gap: 12px;
  .case-summary {
    margin-left: 8px;
    color: #909399;
    font-size: 13px;
  }
}
.case-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12px;
  max-height: 80px;
  overflow: hidden;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
