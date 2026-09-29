<template>
  <div class="app-container">
    <!-- 说明：本页维护规则解析的词表。表为空时规则引擎使用内置默认词典兜底，不会导致解析失败 -->
    <el-alert
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    >
      <template #title>
        简历解析配置（规则解析词表）：<b>章节标题词典</b>决定「大类划分」，
        <b>技能词域</b>用于技能抽取（引擎还会自动聚合「岗位模板」的必备技能），
        <b>学历词/岗位词</b>用于对应字段匹配。
        <span style="color: var(--el-color-warning)">留空即使用内置默认词典。</span>
      </template>
    </el-alert>

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="配置类型" prop="configType">
        <el-select v-model="queryParams.configType" placeholder="全部类型" clearable style="width: 200px">
          <el-option
            v-for="t in typeOptions"
            :key="t.value"
            :label="t.label"
            :value="t.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="显示名称" prop="itemName">
        <el-input
          v-model="queryParams.itemName"
          placeholder="请输入显示名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 140px">
          <el-option label="启用" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          v-hasPermi="['cms:interview:resumeParseConfig:create']"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="configList">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="配置类型" width="120">
        <template #default="scope">
          <el-tag :type="typeTagType(scope.row.configType)" size="small">
            {{ typeLabel(scope.row.configType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配置键" prop="itemKey" width="140" show-overflow-tooltip>
        <template #default="scope">
          <span v-if="scope.row.configType === 'section'">
            <el-tag size="small" effect="plain">{{ scope.row.itemKey }}</el-tag>
          </span>
          <span v-else>{{ scope.row.itemKey }}</span>
        </template>
      </el-table-column>
      <el-table-column label="显示名称" prop="itemName" width="150" show-overflow-tooltip />
      <el-table-column label="关键词" prop="keywords" min-width="280" show-overflow-tooltip />
      <el-table-column label="排序" prop="sort" width="70" align="center" />
      <el-table-column label="状态" width="80" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'" size="small">
            {{ scope.row.status === 'active' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160" align="center">
        <template #default="scope">
          <span>{{ parseTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right" align="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            v-hasPermi="['cms:interview:resumeParseConfig:update']"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            v-hasPermi="['cms:interview:resumeParseConfig:remove']"
            @click="handleDelete(scope.row)"
          >删除</el-button>
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

    <!-- 新增/修改对话框 -->
    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配置类型" prop="configType">
          <el-select v-model="form.configType" placeholder="请选择配置类型" style="width: 100%" @change="onTypeChange">
            <el-option
              v-for="t in typeOptions"
              :key="t.value"
              :label="t.label"
              :value="t.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="配置键" prop="itemKey">
          <el-select
            v-if="form.configType === 'section'"
            v-model="form.itemKey"
            placeholder="请选择目标大类"
            style="width: 100%"
          >
            <el-option
              v-for="s in sectionOptions"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
          <el-input
            v-else
            v-model="form.itemKey"
            :placeholder="itemKeyPlaceholder"
          />
          <div class="form-tip">{{ itemKeyTip }}</div>
        </el-form-item>

        <el-form-item label="显示名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="后台列表展示用，如「教育背景」" />
        </el-form-item>

        <el-form-item label="关键词" prop="keywords">
          <el-input
            v-model="form.keywords"
            type="textarea"
            :rows="3"
            :placeholder="keywordsPlaceholder"
          />
          <div class="form-tip">{{ keywordsTip }}</div>
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ResumeParseConfig">
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listResumeParseConfig,
  getResumeParseConfig,
  addResumeParseConfig,
  updateResumeParseConfig,
  delResumeParseConfig,
} from '@/api/cms/resumeParseConfig';

const { proxy } = getCurrentInstance();

const loading = ref(true);
const showSearch = ref(true);
const configList = ref([]);
const total = ref(0);
const dialogVisible = ref(false);

const queryRef = ref();
const formRef = ref();

/** 配置类型（须与后端 IPortalResumeParseConfigService 常量一致） */
const typeOptions = [
  { value: 'section', label: '章节标题词典' },
  { value: 'skill', label: '技能词域' },
  { value: 'degree', label: '学历词' },
  { value: 'position', label: '岗位词' },
];

/** 章节目标大类（须与后端白名单 / ResumeRuleParser 的 Section 枚举一致） */
const sectionOptions = [
  { value: 'basic', label: 'basic 基本信息' },
  { value: 'intention', label: 'intention 求职意向' },
  { value: 'edu', label: 'edu 教育背景' },
  { value: 'work', label: 'work 工作经历' },
  { value: 'project', label: 'project 项目经历' },
  { value: 'skill', label: 'skill 专业技能' },
  { value: 'self', label: 'self 自我评价' },
  { value: 'other', label: 'other 其他区块' },
];

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  configType: '',
  itemName: '',
  status: '',
});

function makeDefaultForm() {
  return {
    id: null,
    configType: 'section',
    itemKey: '',
    itemName: '',
    keywords: '',
    sort: 0,
    status: 'active',
    remark: '',
  };
}

const form = ref(makeDefaultForm());

const dialogTitle = computed(() => (form.value.id ? '编辑解析配置' : '新增解析配置'));

const rules = {
  configType: [{ required: true, message: '配置类型不能为空', trigger: 'change' }],
  itemKey: [{ required: true, message: '配置键不能为空', trigger: 'blur' }],
  itemName: [{ required: true, message: '显示名称不能为空', trigger: 'blur' }],
};

const itemKeyPlaceholder = computed(() =>
  form.value.configType === 'section' ? '请选择目标大类' : '词条本身（留空则以关键词为准）'
);

const itemKeyTip = computed(() => {
  if (form.value.configType === 'section') {
    return '章节标题命中的目标大类，决定内容归入哪个模块';
  }
  return '非 section 类型时，此值本身也会作为一个词条参与匹配（可与关键词二选一）';
});

const keywordsPlaceholder = computed(() => {
  switch (form.value.configType) {
    case 'section':
      return '该章节的标题同义词，英文逗号分隔，如：教育背景,教育经历,学习经历';
    case 'skill':
      return '技能词条，英文逗号分隔，如：Java,Redis,Docker';
    case 'degree':
      return '学历词条，英文逗号分隔，如：博士,硕士,本科,大专';
    default:
      return '岗位词条，英文逗号分隔，如：Java开发,后端工程师';
  }
});

const keywordsTip = computed(() => {
  if (form.value.configType === 'section') {
    return '多个同义词用英文逗号分隔；标题独占一行或写成「标题：内容」都能识别';
  }
  return '规则引擎会把这些词与简历全文做匹配';
});

function typeLabel(v) {
  return typeOptions.find((t) => t.value === v)?.label || v;
}

function typeTagType(v) {
  switch (v) {
    case 'section':
      return 'primary';
    case 'skill':
      return 'success';
    case 'degree':
      return 'warning';
    default:
      return 'info';
  }
}

function onTypeChange() {
  form.value.itemKey = '';
}

async function getList() {
  loading.value = true;
  try {
    const res = await listResumeParseConfig(queryParams);
    // 兼容两种分页返回：AjaxResult.success(page) → res.data.records；旧式 TableDataInfo → res.rows
    configList.value = res.data?.records || res.rows || [];
    total.value = res.data?.total ?? res.total ?? 0;
  } catch (e) {
    configList.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  proxy.resetForm('queryRef');
  handleQuery();
}

function reset() {
  form.value = makeDefaultForm();
  proxy.resetForm('formRef');
}

function cancel() {
  dialogVisible.value = false;
  reset();
}

function handleAdd() {
  reset();
  dialogVisible.value = true;
}

async function handleUpdate(row) {
  reset();
  const res = await getResumeParseConfig(row.id);
  form.value = { ...makeDefaultForm(), ...(res.data || row) };
  dialogVisible.value = true;
}

async function submitForm() {
  await formRef.value.validate();
  const data = { ...form.value };
  if (data.id) {
    await updateResumeParseConfig(data);
    ElMessage.success('修改成功');
  } else {
    await addResumeParseConfig(data);
    ElMessage.success('新增成功');
  }
  dialogVisible.value = false;
  getList();
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确认删除「${row.itemName}」吗？`, '提示', { type: 'warning' });
  await delResumeParseConfig(row.id);
  ElMessage.success('删除成功');
  getList();
}

onMounted(getList);
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  margin-top: 2px;
}
</style>
