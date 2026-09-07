<template>
  <div class="app-container">
    <div class="button-group">
      <el-button type="primary" v-hasPermi="['cms:interview:config:create']" @click="handleAdd">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="configList">
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="配置名称" prop="configName" min-width="160" show-overflow-tooltip />
      <el-table-column label="人设" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="personaTagType(row.personaType)">{{ personaLabel(row.personaType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="自我介绍" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enableSelfIntro === 1 ? 'success' : 'info'">{{ row.enableSelfIntro === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDefault === 1" type="primary">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'active' ? 'success' : 'info'">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="160">
        <template #default="{ row }">{{ parseTime(row.updateTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" v-hasPermi="['cms:interview:config:update']" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" v-hasPermi="['cms:interview:config:remove']" @click="handleDelete(row)">删除</el-button>
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
      <el-form :model="form" label-width="150px">
        <el-form-item label="配置名称" required>
          <el-input v-model="form.configName" placeholder="请输入配置名称，如：标准技术面" />
        </el-form-item>
        <el-form-item label="面试官人设">
          <el-select v-model="form.personaType" placeholder="请选择面试官人设">
            <el-option label="专业" value="professional" />
            <el-option label="友善" value="friendly" />
            <el-option label="严厉" value="strict" />
          </el-select>
        </el-form-item>
        <el-form-item label="提示词模板">
          <el-input
            v-model="form.promptTemplate" type="textarea" :rows="5"
            :placeholder="'支持 {{position}}/{{resumeDigest}} 占位符，空则使用 Agent 人设'"
          />
          <div class="form-tip" v-pre>支持 {{position}}/{{resumeDigest}} 占位符，留空则使用 Agent 人设</div>
        </el-form-item>
        <el-form-item label="评分权重">
          <el-input
            v-model="form.scoringWeights" type="textarea" :rows="4"
            placeholder='{"selfIntro":{"structure":30,"awareness":25,"matching":25,"fluency":20},"llmRatio":70,"total":{"intro":20,"tech":80}}'
            style="font-family: monospace;"
          />
          <div class="form-tip">JSON 格式：自我介绍各维度权重、LLM 评分占比、环节总分权重</div>
        </el-form-item>
        <el-form-item label="出题权重">
          <el-input
            v-model="form.questionWeights" type="textarea" :rows="3"
            placeholder='{"job":40,"resume":20,"weak":20,"random":20}'
            style="font-family: monospace;"
          />
          <div class="form-tip">JSON 格式：job 岗位题 / resume 简历题 / weak 弱项题 / random 随机题的出题权重</div>
        </el-form-item>
        <el-form-item label="最大追问次数">
          <el-input-number v-model="form.maxFollowups" :min="0" :max="10" />
          <span class="form-tip" style="margin-left: 12px;">每题最大追问次数</span>
        </el-form-item>
        <el-form-item label="追问触发条件">
          <el-input
            v-model="form.followupTriggers" type="textarea" :rows="3"
            placeholder='["vague_answer","contradiction","depth_needed"]'
            style="font-family: monospace;"
          />
          <div class="form-tip">JSON 数组：模糊回答 / 前后矛盾 / 需要深挖</div>
        </el-form-item>
        <el-form-item label="启用自我介绍">
          <el-switch v-model="form.enableSelfIntro" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="自我介绍时长">
          <el-input-number v-model="form.selfIntroDuration" :min="30" :max="600" :step="10" />
          <span class="form-tip" style="margin-left: 12px;">自我介绍建议时长（秒）</span>
        </el-form-item>
        <el-form-item label="默认配置">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
          <span class="form-tip" style="margin-left: 12px;">默认配置不可删除</span>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listInterviewConfig, getInterviewConfig, addInterviewConfig, updateInterviewConfig, delInterviewConfig
} from '@/api/cms/interviewConfig';

const loading = ref(true);
const configList = ref([]);
const total = ref(0);

const queryParams = reactive({
  pageNum: 1, pageSize: 10
});

const dialogVisible = ref(false);
const dialogTitle = computed(() => form.value.id ? '编辑面试配置' : '新增面试配置');

function makeDefaultForm() {
  return {
    id: null,
    configName: '',
    personaType: 'professional',
    promptTemplate: '',
    scoringWeights: '',
    questionWeights: '',
    maxFollowups: 3,
    followupTriggers: '',
    enableSelfIntro: 1,
    selfIntroDuration: 120,
    isDefault: 0,
    status: 'active',
  };
}

const form = ref(makeDefaultForm());

function personaLabel(p) { return { professional: '专业', friendly: '友善', strict: '严厉' }[p] || p || '-'; }
function personaTagType(p) { return { professional: 'primary', friendly: 'success', strict: 'danger' }[p] || 'info'; }

async function getList() {
  loading.value = true;
  try {
    const res = await listInterviewConfig(queryParams);
    configList.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (e) { /* ignore */ } finally {
    loading.value = false;
  }
}

function handleAdd() {
  form.value = makeDefaultForm();
  dialogVisible.value = true;
}

async function handleEdit(row) {
  try {
    const res = await getInterviewConfig(row.id);
    const data = res.data || {};
    form.value = {
      id: data.id,
      configName: data.configName || '',
      personaType: data.personaType || 'professional',
      promptTemplate: data.promptTemplate || '',
      scoringWeights: data.scoringWeights || '',
      questionWeights: data.questionWeights || '',
      maxFollowups: data.maxFollowups != null ? data.maxFollowups : 3,
      followupTriggers: data.followupTriggers || '',
      enableSelfIntro: data.enableSelfIntro != null ? data.enableSelfIntro : 1,
      selfIntroDuration: data.selfIntroDuration != null ? data.selfIntroDuration : 120,
      isDefault: data.isDefault != null ? data.isDefault : 0,
      status: data.status || 'active',
    };
    dialogVisible.value = true;
  } catch (e) { /* ignore */ }
}

async function submitForm() {
  if (!form.value.configName) {
    ElMessage.warning('请输入配置名称');
    return;
  }
  try {
    if (form.value.id) {
      await updateInterviewConfig(form.value);
      ElMessage.success('修改成功');
    } else {
      await addInterviewConfig(form.value);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    getList();
  } catch (e) { /* 错误已由拦截器提示 */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除面试配置「' + row.configName + '」？', '提示', { type: 'warning' });
    await delInterviewConfig(row.id);
    ElMessage.success('删除成功');
    getList();
  } catch (e) {
    // 取消删除或后端拒绝（如默认配置不可删除），错误消息已由拦截器展示
  }
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.app-container { padding: 20px; }
.button-group { margin-bottom: 16px; }
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>