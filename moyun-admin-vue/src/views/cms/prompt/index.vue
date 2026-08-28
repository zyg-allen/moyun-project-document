<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入 prompt 标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="请选择分类" clearable style="width: 160px">
          <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源" prop="source">
        <el-select v-model="queryParams.source" placeholder="请选择来源" clearable style="width: 140px">
          <el-option label="AI 生成" value="ai" />
          <el-option label="手动创建" value="manual" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cms:writing-prompt:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="MagicStick" :loading="aiGenerating" @click="handleAiGenerate" v-hasPermi="['cms:writing-prompt:add']">AI 生成今日</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Calendar" :loading="aiGeneratingRange" @click="handleAiGenerateRange" v-hasPermi="['cms:writing-prompt:add']">AI 批量生成</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['cms:writing-prompt:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['cms:writing-prompt:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="promptList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="日期" align="center" prop="promptDate" width="120" />
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="分类" align="center" prop="category" width="100" />
      <el-table-column label="特殊日期" align="center" prop="festivalName" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.festivalName" type="warning" effect="plain" size="small">{{ scope.row.festivalName }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="来源" align="center" prop="source" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.source === 'ai' ? 'success' : 'info'" effect="plain" size="small">
            {{ scope.row.source === 'ai' ? 'AI 生成' : '手动' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createdTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.createdTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="right" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="warning" icon="MagicStick" :loading="scope.row.aiRegenerating" @click="handleAiRegenerate(scope.row)" v-hasPermi="['cms:writing-prompt:edit']">AI重生成</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cms:writing-prompt:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cms:writing-prompt:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="700px" append-to-body>
      <el-form ref="promptRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="日期" prop="promptDate">
          <el-date-picker v-model="form.promptDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入 prompt 标题" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" allow-create clearable style="width: 100%">
            <el-option label="生活" value="生活" />
            <el-option label="职场" value="职场" />
            <el-option label="情感" value="情感" />
            <el-option label="虚构" value="虚构" />
            <el-option label="哲思" value="哲思" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请输入 prompt 描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- AI 批量生成对话框 -->
    <el-dialog title="AI 批量生成写作提示" v-model="rangeOpen" width="600px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="起始日期">
          <el-date-picker v-model="rangeForm.startDate" type="date" placeholder="选择起始日期" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生成天数">
          <el-input-number v-model="rangeForm.days" :min="1" :max="30" style="width: 100%" />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" show-icon
        title="从起始日期起连续 N 天，为缺失提示的日期生成（已有记录自动跳过）。每天结合节日/节气上下文，AI 失败时回退内置主题池。" />
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" :loading="aiGeneratingRange" @click="submitRange">开始生成</el-button>
          <el-button @click="rangeOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="WritingPrompt">
import { listPrompt, getPrompt, addPrompt, updatePrompt, delPrompt, aiGeneratePrompt, aiGeneratePromptRange, aiRegeneratePrompt } from "@/api/cms/prompt";

const { proxy } = getCurrentInstance();

const promptList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// AI 生成相关状态
const aiGenerating = ref(false);
const aiGeneratingRange = ref(false);
const rangeOpen = ref(false);
const rangeForm = ref({ startDate: undefined, days: 7 });

// 标准分类（与后端 CmsWritingPromptServiceImpl 五分类保持一致）
const categoryOptions = ["生活", "职场", "情感", "虚构", "哲思"];

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    category: undefined,
    source: undefined
  },
  rules: {
    promptDate: [{ required: true, message: "请选择日期", trigger: "change" }],
    title: [{ required: true, message: "标题不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function getList() {
  loading.value = true;
  listPrompt(queryParams.value).then(response => {
    promptList.value = response.data.records;
    total.value = response.data.total;
    loading.value = false;
  });
}

function cancel() {
  open.value = false;
  reset();
}

function reset() {
  form.value = {
    id: undefined,
    promptDate: undefined,
    title: undefined,
    description: undefined,
    category: undefined
  };
  proxy.resetForm("promptRef");
}

function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

function handleAdd() {
  reset();
  open.value = true;
  title.value = "添加写作 Prompt";
}

function handleUpdate(row) {
  reset();
  const id = row.id || ids.value;
  getPrompt(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改写作 Prompt";
  });
}

function submitForm() {
  proxy.$refs["promptRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updatePrompt(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addPrompt(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  const promptIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除编号为"' + promptIds + '"的数据项？').then(function() {
    return delPrompt(promptIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

// ===== AI 生成 =====

/** AI 生成今日提示（已存在则返回已有记录） */
function handleAiGenerate() {
  proxy.$modal.confirm('AI 将为今天生成写作提示（结合节日/节气，已存在则跳过），是否继续？').then(() => {
    aiGenerating.value = true;
    return aiGeneratePrompt();
  }).then(res => {
    aiGenerating.value = false;
    const data = res.data || {};
    if (data.title) {
      proxy.$modal.msgSuccess('生成成功：' + data.title + (data.festivalName ? '（' + data.festivalName + '）' : ''));
    } else {
      proxy.$modal.msgSuccess('生成成功');
    }
    getList();
  }).catch(() => {
    aiGenerating.value = false;
  });
}

/** 打开批量生成对话框 */
function handleAiGenerateRange() {
  rangeForm.value = { startDate: undefined, days: 7 };
  rangeOpen.value = true;
}

/** 提交批量生成 */
function submitRange() {
  if (!rangeForm.value.startDate) {
    proxy.$modal.msgWarning('请选择起始日期');
    return;
  }
  const days = Math.max(1, Math.min(parseInt(rangeForm.value.days) || 7, 30));
  aiGeneratingRange.value = true;
  aiGeneratePromptRange(proxy.parseTime(rangeForm.value.startDate, '{y}-{m}-{d}'), days).then(res => {
    proxy.$modal.msgSuccess('批量生成完成，新增 ' + (res.data || 0) + ' 条');
    rangeOpen.value = false;
    getList();
  }).finally(() => {
    aiGeneratingRange.value = false;
  });
}

/** AI 重新生成单条（覆盖标题/描述/分类/节日） */
function handleAiRegenerate(row) {
  proxy.$modal.confirm('AI 将重新生成该日期的写作提示内容（覆盖现有标题/描述），是否继续？').then(() => {
    row.aiRegenerating = true;
    return aiRegeneratePrompt(row.id);
  }).then(res => {
    row.aiRegenerating = false;
    proxy.$modal.msgSuccess('重新生成成功');
    getList();
  }).catch(() => {
    row.aiRegenerating = false;
  });
}

getList();
</script>
