<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="关键词">
        <el-input
          v-model="queryParams.keyword" placeholder="请输入题目标题" clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="练习模式">
        <el-select v-model="queryParams.practiceMode" placeholder="请选择练习模式" clearable>
          <el-option
            v-for="m in portal_practice_mode"
            :key="m.value"
            :label="m.label"
            :value="m.value"
          />
        </el-select>
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
          <el-option
          v-for="d in portal_question_difficulty"
          :key="d.value"
          :label="d.label"
          :value="d.value"
          />
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
      <el-button type="success" :icon="Upload" v-hasPermi="['cms:interview:import']" @click="handleImport">导入</el-button>
      <el-button type="warning" :icon="Download" v-hasPermi="['cms:interview:export']" @click="handleExport">导出</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="questionList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
      <el-table-column label="练习模式" width="100">
        <template #default="{ row }">
          <dict-tag :options="portal_practice_mode" :value="row.practiceMode" />
        </template>
      </el-table-column>
      <el-table-column label="难度" width="100">
        <template #default="{ row }">
          <dict-tag :options="portal_question_difficulty" :value="row.difficulty" />
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
      <el-table-column label="操作" width="240" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.practiceMode === 'coding'" link type="success" @click="handleTestCase(row)">用例</el-button>
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

    <!-- 新增/编辑弹窗：根据练习模式显示不同字段 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="1020px" :close-on-click-modal="false">
      <el-form :model="form" label-width="100px">
        <!-- 练习模式选择（顶部，决定后续表单） -->
        <el-form-item label="练习模式" required>
          <el-radio-group v-model="form.practiceMode" @change="onPracticeModeChange">
            <el-radio-button
              v-for="m in portal_practice_mode"
              :key="m.value"
              :label="m.value"
            >{{ m.label }}</el-radio-button>
          </el-radio-group>
          <div class="form-tip">
            <span v-if="form.practiceMode === 'reading'">展示阅读题：用户看题+参考答案，不做判分，阅读行为计入成长</span>
            <span v-else-if="form.practiceMode === 'choice'">选择题：用户选择选项，系统判分；支持单选与多选</span>
            <span v-else-if="form.practiceMode === 'coding'">编程题：用户在线写代码，沙箱运行全部测试用例判定</span>
          </div>
        </el-form-item>

        <!-- 基础字段（所有模式通用） -->
        <el-form-item label="标题" required><el-input v-model="form.title" placeholder="请输入标题" /></el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入题目描述" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="form.difficulty" placeholder="请选择难度">
            <el-option v-for="d in portal_question_difficulty" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" filterable clearable>
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
          <el-input v-model="form.hint" type="textarea" :rows="2" placeholder="请输入提示（选填）" />
        </el-form-item>

        <!-- ========== 展示阅读题（reading）字段 ========== -->
        <template v-if="form.practiceMode === 'reading'">
          <el-divider content-position="left">展示阅读题专属</el-divider>
          <el-form-item label="参考答案">
            <el-input v-model="form.solution" type="textarea" :rows="5" placeholder="请输入参考答案（用户看题后展示）" />
          </el-form-item>
          <el-form-item label="题目解析">
            <el-input v-model="form.analysis" type="textarea" :rows="4" placeholder="请输入题目解析（选填）" />
          </el-form-item>
        </template>

        <!-- ========== 选择题（choice）字段：动态选项配置 ========== -->
        <template v-else-if="form.practiceMode === 'choice'">
          <el-divider content-position="left">选择题选项配置</el-divider>
          <div v-for="(opt, idx) in form.optionList" :key="idx" class="option-row">
            <el-form-item :label="'选项 ' + opt.label" style="flex: 1; margin-bottom: 12px;">
              <div class="option-input-group">
                <el-input v-model="opt.text" placeholder="请输入选项内容" style="flex: 1;" />
                <el-checkbox
                  :model-value="form.correctAnswerArr.includes(opt.label)"
                  class="option-correct-radio"
                  @change="(val) => toggleCorrect(opt.label, val)"
                >正确项</el-checkbox>
                <el-button type="danger" :icon="Delete" circle size="small" @click="removeOption(idx)" :disabled="form.optionList.length <= 2" />
              </div>
            </el-form-item>
          </div>
          <el-form-item>
            <el-button type="primary" plain :icon="Plus" @click="addOption">添加选项</el-button>
            <span class="form-tip" style="margin-left: 12px;">
              至少 2 个选项；勾选 1 项为单选题，勾选多项为多选题（用户需全部选对方可判对）
            </span>
          </el-form-item>
          <el-form-item label="题目解析">
            <el-input v-model="form.analysis" type="textarea" :rows="4" placeholder="提交作答后展示的解析" />
          </el-form-item>
        </template>

        <!-- ========== 编程题（coding）字段 ========== -->
        <template v-else-if="form.practiceMode === 'coding'">
          <el-divider content-position="left">编程题专属</el-divider>
          <el-form-item label="参考代码">
            <el-input v-model="form.solution" type="textarea" :rows="6" placeholder="参考代码（题解展示，选填）" style="font-family: monospace;" />
          </el-form-item>
          <el-form-item label="题目解析">
            <el-input v-model="form.analysis" type="textarea" :rows="4" placeholder="做题后展示的解析" />
          </el-form-item>
          <el-form-item>
            <el-alert type="info" :closable="false" show-icon>
              <template #title>
                编程题的测试用例请保存题目后，点击列表「用例」按钮进行配置。
              </template>
            </el-alert>
          </el-form-item>
        </template>

        <!-- 知识点标签（所有模式通用） -->
        <el-form-item label="知识点">
          <el-input v-model="form.knowledgeTags" placeholder="多个知识点用英文逗号分隔，如：TCP,网络,三次握手" />
        </el-form-item>

        <!-- 通用尾部字段 -->
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

    <!-- Excel 导入弹窗 -->
    <ImportDialog
      v-model="importVisible"
      title="题库批量导入"
      business-key="interview_question"
      tip="请使用「下载导入模板」按钮获取最新模板格式，按字段说明填写后上传；导入失败的行可下载 Excel 修正后重导。"
      template-file-name="面试题库导入模板"
      fail-rows-file-name="面试题库导入失败行"
      :import-api="importInterviewQuestionData"
      :template-api="downloadInterviewQuestionTemplate"
      :fail-rows-api="exportInterviewQuestionFailRows"
      @success="handleImportSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, getCurrentInstance } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Upload, Download, Delete, Plus } from '@element-plus/icons-vue';
import {
  listInterviewQuestion, getInterviewQuestion, addInterviewQuestion,
  updateInterviewQuestion, delInterviewQuestion,
  exportInterviewQuestion, downloadInterviewQuestionTemplate,
  importInterviewQuestionData, exportInterviewQuestionFailRows
} from '@/api/cms/interview';
import { listInterviewCategory } from '@/api/cms/interview';
import { bindTagsToEntity, getHotTags } from '@/api/cms/tag';
import TagSelect from '@/components/TagSelect.vue';
import ImportDialog from '@/components/ImportDialog/index.vue';

const { proxy } = getCurrentInstance();

const { portal_question_difficulty } = proxy.useDict("portal_question_difficulty");
const { portal_practice_mode } = proxy.useDict("portal_practice_mode");

const loading = ref(true);
const questionList = ref([]);
const total = ref(0);
const categoryOptions = ref([]);
const tagOptions = ref([]);
const ids = ref([]);
const multiple = computed(() => ids.value.length === 0);

const queryParams = reactive({
  pageNum: 1, pageSize: 10,
  keyword: '', categoryId: '', difficulty: '', status: '', practiceMode: ''
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑题目' : '新增题目');

// Excel 导入弹窗
const importVisible = ref(false);

// 选项列表（选择题动态选项）
function makeDefaultForm() {
  return {
    id: null,
    title: '',
    description: '',
    difficulty: 'easy',
    categoryId: null,
    tags: [],
    companies: '',
    hint: '',
    solution: '',
    sort: 0,
    status: 'published',
    // v10.6 题库重构：练习模式扩展字段
    practiceMode: 'reading',
    optionList: makeDefaultOptions(),  // 选择题选项数组（前端临时态）
    correctAnswer: '',                 // 正确答案（提交值：选项字母，多选逗号分隔）
    correctAnswerArr: [],              // 正确答案勾选集（前端临时态，提交时序列化）
    analysis: '',                      // 题目解析
    knowledgeTags: '',                  // 知识点标签
  };
}

// 默认 4 个选项 A/B/C/D
function makeDefaultOptions() {
  return [
    { label: 'A', text: '', is_correct: false },
    { label: 'B', text: '', is_correct: false },
    { label: 'C', text: '', is_correct: false },
    { label: 'D', text: '', is_correct: false },
  ];
}

const form = ref(makeDefaultForm());

function statusLabel(s) { return { draft: '草稿', published: '已发布', archived: '已归档' }[s] || s; }
function statusType(s) { return { draft: 'info', published: 'success', archived: 'warning' }[s] || 'info'; }
function tagList(tags) { return tags ? String(tags).split(',').map(s => s.trim()).filter(Boolean) : []; }
function tagsToStr(tags) { return (tags || []).join(','); }

// 选项 label 序列：A B C D E F G H
function nextOptionLabel() {
  const labels = ['A','B','C','D','E','F','G','H','I','J'];
  return labels[form.value.optionList.length] || String.fromCharCode(65 + form.value.optionList.length);
}

function addOption() {
  form.value.optionList.push({ label: nextOptionLabel(), text: '', is_correct: false });
}

/** 勾选/取消正确项：维护勾选集并同步提交值（多选逗号拼接） */
function toggleCorrect(label, checked) {
  const arr = form.value.correctAnswerArr;
  const idx = arr.indexOf(label);
  if (checked && idx === -1) arr.push(label);
  else if (!checked && idx >= 0) arr.splice(idx, 1);
  form.value.correctAnswer = arr.join(',');
}

function removeOption(idx) {
  if (form.value.optionList.length <= 2) {
    ElMessage.warning('至少保留 2 个选项');
    return;
  }
  // 先按位置记录勾选项（删除后 label 会整体前移，不能按旧 label 匹配）
  const checkedIdx = form.value.optionList
    .map((opt, i) => form.value.correctAnswerArr.includes(opt.label) ? i : -1)
    .filter(i => i !== -1)
    .filter(i => i !== idx);
  form.value.optionList.splice(idx, 1);
  // 重新排序 label
  form.value.optionList.forEach((opt, i) => {
    opt.label = String.fromCharCode(65 + i);
  });
  // 勾选位置前移一位后映射到新 label
  form.value.correctAnswerArr = checkedIdx
    .map(i => (i > idx ? i - 1 : i))
    .map(i => form.value.optionList[i]?.label)
    .filter(Boolean);
  form.value.correctAnswer = form.value.correctAnswerArr.join(',');
}

// 练习模式切换：进入 choice 初始化选项，离开 choice 清空选择题临时态
function onPracticeModeChange(newMode) {
  if (newMode === 'choice' && form.value.optionList.length === 0) {
    form.value.optionList = makeDefaultOptions();
  }
  if (newMode !== 'choice') {
    form.value.correctAnswerArr = [];
    form.value.correctAnswer = '';
  }
}

async function loadCategories() {
  try {
    const res = await listInterviewCategory();
    categoryOptions.value = res.data || [];
  } catch (e) { /* ignore */ }
}

async function loadTagOptions() {
  try {
    const res = await getHotTags('interview_question', 50);
    const rows = res.data || [];
    tagOptions.value = rows
      .map(item => item.name || item.tagName || item)
      .filter(Boolean);
  } catch (e) { /* ignore */ }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewQuestion(queryParams);
    questionList.value = res.data.records || [];
    total.value = res.data.total || 0;
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
  queryParams.practiceMode = '';
  queryParams.pageNum = 1;
  getList();
}

function handleAdd() {
  form.value = makeDefaultForm();
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewQuestion(row.id);
    const data = res.data || {};
    // 解析 options JSON 字符串为数组
    let optionList = makeDefaultOptions();
    if (data.options) {
      try {
        const parsed = typeof data.options === 'string' ? JSON.parse(data.options) : data.options;
        if (Array.isArray(parsed) && parsed.length > 0) {
          optionList = parsed.map((o, i) => ({
            label: o.label || String.fromCharCode(65 + i),
            text: o.text || '',
            is_correct: !!o.is_correct || o.isCorrect || false,
          }));
        }
      } catch { /* JSON 解析失败，用默认空选项 */ }
    }
    // 正确答案勾选集：correctAnswer（支持 "A" / "A,B" 多选写法）优先，
    // 兼容历史数据仅 options 标记 is_correct 而 correctAnswer 为空的情况
    const answerStr = (data.correctAnswer || '').toUpperCase();
    let correctAnswerArr = answerStr
      ? answerStr.split(/[^A-Z]+/).filter(Boolean)
      : optionList.filter(o => o.is_correct).map(o => o.label);
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
      status: data.status || 'published',
      // v10.6 扩展字段
      practiceMode: data.practiceMode || 'reading',
      optionList,
      correctAnswer: correctAnswerArr.join(','),
      correctAnswerArr,
      analysis: data.analysis || '',
      knowledgeTags: data.knowledgeTags || '',
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

const router = useRouter();
const route = useRoute();

function handleTestCase(row) {
  // 携带来源路径，测试用例页返回时动态回跳（菜单调整后无需改代码）
  router.push({ path: `/cms/interview/testCase/${row.id}`, query: { from: route.fullPath } });
}

async function submitForm() {
  if (!form.value.title) {
    ElMessage.warning('请输入标题');
    return;
  }
  // 选择题校验
  if (form.value.practiceMode === 'choice') {
    const validOpts = form.value.optionList.filter(o => o.text.trim());
    if (validOpts.length < 2) {
      ElMessage.warning('选择题至少需要 2 个有效选项');
      return;
    }
    if (!form.value.correctAnswerArr.length) {
      ElMessage.warning('请勾选正确项（1 项为单选题，多项为多选题）');
      return;
    }
    // 勾选正确项的选项内容不能为空
    const emptyCorrect = form.value.correctAnswerArr.find(
      (label) => !form.value.optionList.find(o => o.label === label && o.text.trim())
    );
    if (emptyCorrect) {
      ElMessage.warning(`选项 ${emptyCorrect} 已勾选为正确项，请补充其内容`);
      return;
    }
    // 同步勾选集到提交值（多选逗号拼接，与判分归一化规则一致）
    form.value.correctAnswer = form.value.correctAnswerArr.join(',');
  }
  // 阅读题提示：无参考答案时给出非阻断提醒
  if (form.value.practiceMode === 'reading' && !form.value.solution && !form.value.analysis) {
    ElMessage.warning('建议填写参考答案或题目解析，供用户阅读时查看');
  }
  // 编程题提示：无测试用例时给出非阻断提醒
  if (form.value.practiceMode === 'coding') {
    if (!form.value.solution) {
      ElMessage.warning('建议填写参考代码，供用户提交后查看题解');
    }
    if (!form.value.id) {
      ElMessage.info('保存后请及时配置测试用例，未配置用例的编程题无法判题');
    }
  }
  try {
    // 构造提交数据：optionList 序列化为 options JSON 字符串
    const submitData = {
      ...form.value,
      tags: tagsToStr(form.value.tags),
    };
    // 选择题：optionList → options JSON 字符串（is_correct 支持多选）
    if (form.value.practiceMode === 'choice') {
      submitData.options = JSON.stringify(
        form.value.optionList
          .filter(o => o.text.trim())
          .map(o => ({
            label: o.label,
            text: o.text.trim(),
            is_correct: form.value.correctAnswerArr.includes(o.label),
          }))
      );
    } else {
      submitData.options = null;
      submitData.correctAnswer = null;
    }
    // 移除前端临时态字段（optionList 已序列化为 options；correctAnswerArr 已序列化为 correctAnswer）
    delete submitData.optionList;
    delete submitData.correctAnswerArr;

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

// 打开导入弹窗
function handleImport() {
  importVisible.value = true;
}

// 导入成功回调：刷新列表
function handleImportSuccess(result) {
  // 仅当有成功记录时刷新
  if (result && result.successCount > 0) {
    getList();
  }
}

// 导出按钮：按当前筛选条件导出 Excel
function handleExport() {
  proxy.download(
    '/cms/interview/question/export',
    { ...queryParams },
    `面试题目_${formatNow()}.xlsx`
  );
}

function formatNow() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`;
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
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
.option-row {
  margin-bottom: 4px;
}
.option-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
}
.option-correct-radio {
  flex-shrink: 0;
}
</style>
