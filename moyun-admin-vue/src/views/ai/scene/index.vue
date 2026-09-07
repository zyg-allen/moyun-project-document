<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="场景代码">
        <el-input
          v-model="queryParams.sceneCode" placeholder="如 voice_interview" clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="场景名称">
        <el-input
          v-model="queryParams.sceneName" placeholder="请输入场景名称关键词" clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="button-group">
      <el-button type="primary" v-hasPermi="['cms:ai:scene:create']" @click="handleAdd">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="pagedList">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="场景代码" prop="sceneCode" min-width="150" show-overflow-tooltip />
      <el-table-column label="场景名称" prop="sceneName" min-width="150" show-overflow-tooltip />
      <el-table-column label="版本" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.version || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="灰度权重" width="90" align="center">
        <template #default="{ row }">{{ row.weight != null ? row.weight + '%' : '-' }}</template>
      </el-table-column>
      <el-table-column label="优先级" prop="priority" width="80" align="center" />
      <el-table-column label="默认" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isDefault ? 'success' : 'info'">{{ row.isDefault ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggleEnabled(row)" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="170" align="center" />
      <el-table-column label="操作" width="200" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['cms:ai:scene:update']" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="success" @click="handleTest(row)">测试</el-button>
          <el-button link type="danger" v-hasPermi="['cms:ai:scene:remove']" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="filteredTotal > 0"
      :total="filteredTotal"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" :close-on-click-modal="false">
      <el-form :model="form" label-width="120px">
        <el-form-item label="场景代码" required>
          <el-input v-model="form.sceneCode" placeholder="如 voice_interview / resume_optimize / question_generate" />
        </el-form-item>
        <el-form-item label="场景名称" required>
          <el-input v-model="form.sceneName" placeholder="请输入场景名称" />
        </el-form-item>
        <el-form-item label="场景描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入场景描述" />
        </el-form-item>
        <el-form-item label="绑定智能体">
          <el-select v-model="form.agentId" placeholder="请选择智能体（可清空）" filterable clearable style="width: 100%;">
            <el-option v-for="a in agentOptions" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="直绑模型">
          <el-select v-model="form.modelConfigId" placeholder="请选择模型（可清空）" filterable clearable style="width: 100%;">
            <el-option v-for="m in modelOptions" :key="m.id" :label="m.name || m.modelName" :value="m.id" />
          </el-select>
          <div class="form-tip">解析顺序：绑定智能体 &gt; 直绑模型（智能体为空时生效）&gt; 空绑定走业务默认逻辑</div>
        </el-form-item>
        <el-form-item label="绑定工作流">
          <el-select v-model="form.workflowId" placeholder="请选择工作流（可清空）" filterable clearable style="width: 100%;">
            <el-option v-for="w in workflowOptions" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识库ID列表">
          <el-input v-model="form.knowledgeLibraryIds" placeholder="JSON 数组字符串，如 [1,2,3]" />
        </el-form-item>
        <el-form-item label="工具ID列表">
          <el-input v-model="form.toolIds" placeholder="JSON 数组字符串，如 [1,2]" />
        </el-form-item>
        <el-form-item label="策略配置JSON">
          <el-input v-model="form.configJson" type="textarea" :rows="3" placeholder='如 {"dynamicMode":true}' style="font-family: monospace;" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="form.version" placeholder="同场景多版本灰度，默认 v1" />
        </el-form-item>
        <el-form-item label="灰度权重">
          <el-input-number v-model="form.weight" :min="0" :max="100" />
          <span class="form-tip" style="margin-left: 12px;">0-100，同场景多版本按权重轮盘赌</span>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" />
        </el-form-item>
        <el-form-item label="默认版本">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 测试结果弹窗 -->
    <el-dialog v-model="testVisible" title="场景绑定解析结果" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="场景代码">{{ testResult.sceneCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ testResult.version || '-' }}</el-descriptions-item>
        <el-descriptions-item label="绑定类型">
          <el-tag :type="bindTypeTag(testResult.bindType)">{{ bindTypeLabel(testResult.bindType) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="智能体">{{ testResult.agentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ testResult.modelName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工作流ID">{{ testResult.workflowId != null ? testResult.workflowId : '-' }}</el-descriptions-item>
        <el-descriptions-item label="知识库数量">{{ testResult.knowledgeLibraryCount != null ? testResult.knowledgeLibraryCount : 0 }}</el-descriptions-item>
        <el-descriptions-item label="工具数量">{{ testResult.toolCount != null ? testResult.toolCount : 0 }}</el-descriptions-item>
        <el-descriptions-item label="策略配置" :span="2">{{ testResult.configJson || '-' }}</el-descriptions-item>
      </el-descriptions>
      <pre class="test-result-json">{{ testResultJson }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listScene, getScene, addScene, updateScene, delScene, testScene } from '@/api/ai/scene';
import { listAgent } from '@/api/ai/agent';
import { listModelConfig } from '@/api/ai/model';
import { listWorkflow } from '@/api/ai/workflow';

const loading = ref(true);
const sceneList = ref([]);
const agentOptions = ref([]);
const modelOptions = ref([]);
const workflowOptions = ref([]);

const queryParams = reactive({
  pageNum: 1, pageSize: 10,
  sceneCode: '',    // 后端精确筛选
  sceneName: ''     // 前端关键词过滤
});

const dialogVisible = ref(false);
const testVisible = ref(false);
const testResult = ref({});
const testResultJson = computed(() =>
  testResult.value && Object.keys(testResult.value).length > 0
    ? JSON.stringify(testResult.value, null, 2)
    : ''
);

function makeDefaultForm() {
  return {
    id: null,
    sceneCode: '',
    sceneName: '',
    description: '',
    agentId: null,
    modelConfigId: null,
    workflowId: null,
    knowledgeLibraryIds: '',
    toolIds: '',
    configJson: '',
    version: 'v1',
    weight: 100,
    priority: 0,
    isDefault: false,
    enabled: true
  };
}

const form = ref(makeDefaultForm());
const dialogTitle = computed(() => form.value.id ? '编辑场景配置' : '新增场景配置');

// 后端 /list 返回全量列表（ListResponse{list,total}），前端做场景名称过滤 + 分页切片
const filteredList = computed(() => {
  const kw = (queryParams.sceneName || '').trim().toLowerCase();
  if (!kw) return sceneList.value;
  return sceneList.value.filter(item =>
    (item.sceneName || '').toLowerCase().includes(kw)
  );
});
const filteredTotal = computed(() => filteredList.value.length);
const pagedList = computed(() => {
  const { pageNum, pageSize } = queryParams;
  return filteredList.value.slice((pageNum - 1) * pageSize, pageNum * pageSize);
});

async function loadOptions() {
  try {
    const [agentRes, modelRes, workflowRes] = await Promise.all([
      listAgent(),
      listModelConfig(),
      listWorkflow()
    ]);
    agentOptions.value = (agentRes.data && agentRes.data.list) || []; // ListResponse（data.list）
    modelOptions.value = (modelRes.data && modelRes.data.list) || []; // ListResponse
    workflowOptions.value = workflowRes.data || [];                  // 返回数组
  } catch (e) { /* ignore */ }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listScene({ sceneCode: queryParams.sceneCode || undefined });
    sceneList.value = (res.data && res.data.list) || [];
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.sceneCode = '';
  queryParams.sceneName = '';
  queryParams.pageNum = 1;
  getList();
}

function handleAdd() {
  form.value = makeDefaultForm();
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getScene(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id,
      sceneCode: data.sceneCode || '',
      sceneName: data.sceneName || '',
      description: data.description || '',
      agentId: data.agentId != null ? data.agentId : null,
      modelConfigId: data.modelConfigId != null ? data.modelConfigId : null,
      workflowId: data.workflowId != null ? data.workflowId : null,
      knowledgeLibraryIds: data.knowledgeLibraryIds || '',
      toolIds: data.toolIds || '',
      configJson: data.configJson || '',
      version: data.version || 'v1',
      weight: data.weight != null ? data.weight : 100,
      priority: data.priority != null ? data.priority : 0,
      isDefault: !!data.isDefault,
      enabled: !!data.enabled
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

/** 校验并归一化 JSON 数组字符串字段，非法时返回 null 并提示 */
function normalizeJsonArray(value, label) {
  const text = (value || '').trim();
  if (!text) return '';
  try {
    const parsed = JSON.parse(text);
    if (!Array.isArray(parsed)) throw new Error('not array');
    return JSON.stringify(parsed);
  } catch (e) {
    ElMessage.warning(label + ' 格式错误：须为 JSON 数组字符串，如 [1,2,3]');
    return null;
  }
}

/** 校验并归一化 JSON 对象字符串字段，非法时返回 null 并提示 */
function normalizeJsonObject(value) {
  const text = (value || '').trim();
  if (!text) return '';
  try {
    const parsed = JSON.parse(text);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) throw new Error('not object');
    return JSON.stringify(parsed);
  } catch (e) {
    ElMessage.warning('策略配置 JSON 格式错误：须为 JSON 对象，如 {"dynamicMode":true}');
    return null;
  }
}

async function submitForm() {
  if (!form.value.sceneCode || !form.value.sceneCode.trim()) {
    ElMessage.warning('请输入场景代码');
    return;
  }
  if (!form.value.sceneName || !form.value.sceneName.trim()) {
    ElMessage.warning('请输入场景名称');
    return;
  }
  if (!form.value.version || !form.value.version.trim()) {
    ElMessage.warning('请输入版本号');
    return;
  }
  const knowledgeLibraryIds = normalizeJsonArray(form.value.knowledgeLibraryIds, '知识库ID列表');
  if (knowledgeLibraryIds === null) return;
  const toolIds = normalizeJsonArray(form.value.toolIds, '工具ID列表');
  if (toolIds === null) return;
  const configJson = normalizeJsonObject(form.value.configJson);
  if (configJson === null) return;

  const submitData = {
    ...form.value,
    sceneCode: form.value.sceneCode.trim(),
    sceneName: form.value.sceneName.trim(),
    version: form.value.version.trim(),
    agentId: form.value.agentId || null,
    modelConfigId: form.value.modelConfigId || null,
    workflowId: form.value.workflowId || null,
    knowledgeLibraryIds: knowledgeLibraryIds || null,
    toolIds: toolIds || null,
    configJson: configJson || null
  };
  try {
    if (form.value.id) {
      await updateScene(submitData);
      ElMessage.success('修改成功');
    } else {
      await addScene(submitData);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    getList();
  } catch (e) { /* ignore */ }
}

/** 表格内启用开关：仅提交后端必填校验所需的最小字段，其余字段不动 */
async function handleToggleEnabled(row) {
  const prev = !row.enabled;
  try {
    await updateScene({
      id: row.id,
      sceneCode: row.sceneCode,
      sceneName: row.sceneName,
      version: row.version,
      enabled: row.enabled
    });
    ElMessage.success(row.enabled ? '已启用' : '已停用');
  } catch (e) {
    row.enabled = prev;
  }
}

async function handleTest(row) {
  try {
    const res = await testScene(row.id);
    testResult.value = res.data || {};
    testVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      '确认删除场景「' + row.sceneName + '」（' + row.sceneCode + ' / ' + row.version + '）？',
      '提示',
      { type: 'warning' }
    );
    await delScene(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) { /* cancel */ }
}

function bindTypeLabel(t) {
  return { agent: '智能体', model: '直绑模型', empty: '空绑定' }[t] || (t || '-');
}

function bindTypeTag(t) {
  return { agent: 'success', model: 'primary', empty: 'info' }[t] || 'info';
}

onMounted(() => {
  loadOptions();
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
.test-result-json {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #606266;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>