<template>
  <div class="app-container">
    <!-- 场景注册表总览（v11.38：来自 AiSceneEnum，场景代码唯一权威来源） -->
    <el-card shadow="never" class="registry-card">
      <template #header>
        <div class="registry-header">
          <span>场景注册表（{{ registry.length }} 个场景）</span>
          <span class="registry-tip">场景代码由代码注册（AiSceneEnum），此处为只读总览；下方配置列表为各场景的 Agent/模型/工作流绑定</span>
        </div>
      </template>
      <el-table :data="registry" size="small">
        <el-table-column label="场景代码" prop="code" min-width="150" show-overflow-tooltip />
        <el-table-column label="场景名称" prop="name" min-width="120" />
        <el-table-column label="核心能力" prop="capability" min-width="200" show-overflow-tooltip />
        <el-table-column label="输入" prop="input" min-width="200" show-overflow-tooltip />
        <el-table-column label="输出" prop="output" min-width="180" show-overflow-tooltip />
        <el-table-column label="已建绑定" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="bindingCountOf(row.code) > 0 ? 'success' : 'info'" size="small">
              {{ bindingCountOf(row.code) }} 个
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
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
      <el-table-column label="分类" prop="sceneCategory" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.sceneCategory" size="small" type="info">{{ row.sceneCategory }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="Handler" prop="handlerBeanName" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.handlerBeanName" style="font-family: monospace; font-size: 12px;">{{ row.handlerBeanName }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="860px" :close-on-click-modal="false">
      <el-form :model="form" label-width="120px">
        <el-tabs v-model="formTab">
          <!-- ===== 基础配置 ===== -->
          <el-tab-pane label="基础配置" name="basic">
            <el-form-item label="场景代码" required>
              <el-select
                v-model="form.sceneCode"
                placeholder="请选择场景代码（来自 AiSceneEnum 代码注册表）"
                filterable
                allow-create
                :disabled="!!form.id"
                style="width: 100%;"
              >
                <el-option
                  v-for="s in registry"
                  :key="s.code"
                  :label="`${s.code}（${s.name}）`"
                  :value="s.code"
                />
              </el-select>
              <div class="form-tip">选项来自 AiSceneEnum 枚举（与 Handler Bean 注册保持一致，新增 Handler 需同步加枚举）</div>
            </el-form-item>
            <el-form-item label="场景名称" required>
              <el-input v-model="form.sceneName" placeholder="请输入场景名称" />
            </el-form-item>
            <el-form-item label="场景描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入场景描述" />
            </el-form-item>
            <el-form-item label="场景分类">
              <el-select v-model="form.sceneCategory" placeholder="请选择分类" clearable style="width: 100%;">
                <el-option label="对话 (chat)" value="chat" />
                <el-option label="分析 (analysis)" value="analysis" />
                <el-option label="生成 (generation)" value="generation" />
                <el-option label="分类 (classification)" value="classification" />
              </el-select>
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
            <el-form-item label="版本号">
              <el-input v-model="form.version" placeholder="同场景多版本灰度，默认 v1" />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="6">
                <el-form-item label="灰度权重" label-width="90px">
                  <el-input-number v-model="form.weight" :min="0" :max="100" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="优先级" label-width="70px">
                  <el-input-number v-model="form.priority" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="默认" label-width="50px">
                  <el-switch v-model="form.isDefault" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="启用" label-width="50px">
                  <el-switch v-model="form.enabled" />
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item label="开放API" label-width="80px">
                  <el-switch v-model="form.openApi" />
                </el-form-item>
              </el-col>
            </el-row>
            <div class="form-tip" style="margin: -8px 0 12px;">开放API=开启时该场景可经 /api/ai/execute 统一入口外部调用；关闭则仅限业务内部链路（防止绕过业务 Controller 的鉴权与编排）</div>
          </el-tab-pane>

          <!-- ===== 执行配置 ===== -->
          <el-tab-pane label="执行配置" name="exec">
            <el-form-item label="Handler Bean">
              <el-input v-model="form.handlerBeanName" placeholder="如 voiceInterviewHandler" />
            </el-form-item>
            <el-form-item label="执行方法">
              <el-input v-model="form.handlerMethod" placeholder="默认 execute" />
            </el-form-item>
            <el-form-item label="系统提示词模板">
              <el-input v-model="form.systemPromptTemplate" type="textarea" :rows="4" placeholder="支持占位符 {{variable}}" style="font-family: monospace;" />
            </el-form-item>
            <el-form-item label="用户提示词模板">
              <el-input v-model="form.userPromptTemplate" type="textarea" :rows="3" placeholder="用户提示词模板" style="font-family: monospace;" />
            </el-form-item>
            <el-form-item label="占位符说明">
              <el-input v-model="form.promptPlaceholders" placeholder='JSON，如 {"name":"用户名"}' style="font-family: monospace;" />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="输出模式" label-width="90px">
                  <el-select v-model="form.outputMode" style="width: 100%;">
                    <el-option label="同步" value="sync" />
                    <el-option label="流式" value="stream" />
                    <el-option label="双模式" value="both" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="解析器" label-width="70px">
                  <el-select v-model="form.outputParser" clearable style="width: 100%;">
                    <el-option label="JSON" value="json" />
                    <el-option label="Markdown" value="markdown" />
                    <el-option label="自定义" value="custom" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最大Token" label-width="80px">
                  <el-input-number v-model="form.maxTokens" :min="1" :max="32768" style="width: 100%;" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="温度" label-width="90px">
                  <el-input-number v-model="form.temperature" :precision="1" :min="0" :max="2" :step="0.1" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="超时(秒)" label-width="70px">
                  <el-input-number v-model="form.timeoutSeconds" :min="1" :max="300" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="重试次数" label-width="80px">
                  <el-input-number v-model="form.retryCount" :min="0" :max="10" style="width: 100%;" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="输出结构">
              <el-input v-model="form.outputSchema" type="textarea" :rows="3" placeholder="JSON Schema" style="font-family: monospace;" />
            </el-form-item>
            <el-form-item label="策略配置JSON">
              <el-input v-model="form.configJson" type="textarea" :rows="3" placeholder='如 {"dynamicMode":true}' style="font-family: monospace;" />
            </el-form-item>
          </el-tab-pane>

          <!-- ===== 限流与降级 ===== -->
          <el-tab-pane label="限流与降级" name="resilience">
            <el-row :gutter="16">
              <el-col :span="8">
                <el-form-item label="限流Key" label-width="90px">
                  <el-input v-model="form.rateLimitKey" placeholder="如 scene:voice" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="限流次数" label-width="70px">
                  <el-input-number v-model="form.rateLimitCount" :min="1" style="width: 100%;" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="时间窗口(秒)" label-width="100px">
                  <el-input-number v-model="form.rateLimitTime" :min="1" style="width: 100%;" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="日Token上限">
              <el-input-number
                v-model="form.dailyTokenLimit"
                :min="0" :step="10000" :step-strictly="false"
                placeholder="空或0表示不限"
                style="width: 260px;"
              />
              <div class="form-tip" style="width: 100%;">场景级成本熔断（全体用户共享）：当日累计消耗 Token 超过该上限后拒绝调用，次日自动恢复；空或 0 = 不限制</div>
            </el-form-item>
            <el-form-item label="备用模型">
              <el-select v-model="form.fallbackModelId" placeholder="AI不可用时备用模型（可清空）" filterable clearable style="width: 100%;">
                <el-option v-for="m in modelOptions" :key="m.id" :label="m.name || m.modelName" :value="m.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="兜底回复">
              <el-input v-model="form.fallbackResponse" type="textarea" :rows="3" placeholder="AI不可用时返回的兜底文案" />
            </el-form-item>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="启用缓存" label-width="90px">
                  <el-switch v-model="form.enableCache" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="缓存TTL(秒)" label-width="100px">
                  <el-input-number v-model="form.cacheTtl" :min="60" :step="60" style="width: 100%;" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
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
import { listScene, getScene, addScene, updateScene, delScene, testScene, sceneRegistry } from '@/api/ai/scene';
import { listAgent } from '@/api/ai/agent';
import { listModelConfig } from '@/api/ai/model';
import { listWorkflow } from '@/api/ai/workflow';

const loading = ref(true);
const sceneList = ref([]);
const agentOptions = ref([])
const registry = ref([]);
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
const formTab = ref('basic');
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
    sceneCategory: '',
    agentId: null,
    modelConfigId: null,
    workflowId: null,
    knowledgeLibraryIds: '',
    toolIds: '',
    configJson: '',
    // v11.41 执行层
    handlerBeanName: '',
    handlerMethod: 'execute',
    systemPromptTemplate: '',
    userPromptTemplate: '',
    promptPlaceholders: '',
    outputMode: 'sync',
    outputSchema: '',
    outputParser: '',
    maxTokens: 2048,
    temperature: 0.7,
    timeoutSeconds: 30,
    retryCount: 3,
    rateLimitKey: '',
    rateLimitCount: 100,
    rateLimitTime: 60,
    dailyTokenLimit: null,
    fallbackModelId: null,
    fallbackResponse: '',
    enableCache: false,
    cacheTtl: 3600,
    // 版本与灰度
    version: 'v1',
    weight: 100,
    priority: 0,
    isDefault: false,
    enabled: true,
    openApi: false
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

function bindingCountOf(code) {
  return sceneList.value.filter((c) => c.sceneCode === code).length;
}

const currentSceneMeta = computed(() => registry.value.find((s) => s.code === form.value.sceneCode) || null);

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
      sceneCategory: data.sceneCategory || '',
      agentId: data.agentId != null ? data.agentId : null,
      modelConfigId: data.modelConfigId != null ? data.modelConfigId : null,
      workflowId: data.workflowId != null ? data.workflowId : null,
      knowledgeLibraryIds: data.knowledgeLibraryIds || '',
      toolIds: data.toolIds || '',
      configJson: data.configJson || '',
      // v11.41 执行层
      handlerBeanName: data.handlerBeanName || '',
      handlerMethod: data.handlerMethod || 'execute',
      systemPromptTemplate: data.systemPromptTemplate || '',
      userPromptTemplate: data.userPromptTemplate || '',
      promptPlaceholders: data.promptPlaceholders || '',
      outputMode: data.outputMode || 'sync',
      outputSchema: data.outputSchema || '',
      outputParser: data.outputParser || '',
      maxTokens: data.maxTokens != null ? data.maxTokens : 2048,
      temperature: data.temperature != null ? data.temperature : 0.7,
      timeoutSeconds: data.timeoutSeconds != null ? data.timeoutSeconds : 30,
      retryCount: data.retryCount != null ? data.retryCount : 3,
      rateLimitKey: data.rateLimitKey || '',
      rateLimitCount: data.rateLimitCount != null ? data.rateLimitCount : 100,
      rateLimitTime: data.rateLimitTime != null ? data.rateLimitTime : 60,
      dailyTokenLimit: data.dailyTokenLimit != null ? data.dailyTokenLimit : null,
      fallbackModelId: data.fallbackModelId != null ? data.fallbackModelId : null,
      fallbackResponse: data.fallbackResponse || '',
      enableCache: !!data.enableCache,
      cacheTtl: data.cacheTtl != null ? data.cacheTtl : 3600,
      // 版本与灰度
      version: data.version || 'v1',
      weight: data.weight != null ? data.weight : 100,
      priority: data.priority != null ? data.priority : 0,
      isDefault: !!data.isDefault,
      enabled: !!data.enabled,
      openApi: !!data.openApi
    };
    formTab.value = 'basic';
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
  // 场景名称以注册表为准（v11.38）
  const meta = registry.value.find((s) => s.code === form.value.sceneCode);
  if (meta) form.value.sceneName = meta.name;
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

async function loadRegistry() {
  try {
    const res = await sceneRegistry();
    registry.value = (res.data || res.rows) || [];
  } catch (e) {
    // 注册表加载失败不阻塞页面，下拉降级为可手输
    registry.value = [];
  }
}

onMounted(() => {
  loadRegistry();
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
.registry-card { margin-bottom: 16px; }
.registry-header { display: flex; align-items: center; justify-content: space-between; }
.registry-tip { font-size: 12px; color: #909399; font-weight: 400; }
</style>