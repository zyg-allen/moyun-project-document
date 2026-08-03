<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="敏感词" prop="word">
        <el-input
          v-model="queryParams.word"
          placeholder="请输入敏感词"
          clearable
          style="width: 240px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="分类" clearable style="width: 180px">
          <el-option label="政治" value="politics" />
          <el-option label="色情" value="porn" />
          <el-option label="广告" value="ad" />
          <el-option label="辱骂" value="insult" />
          <el-option label="其他" value="other" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 160px">
          <el-option label="启用" value="0" />
          <el-option label="禁用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:sensitiveWord:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:sensitiveWord:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="Refresh" @click="handleReload" v-hasPermi="['system:sensitiveWord:edit']">刷新词树</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="wordList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="敏感词" align="center" prop="word" min-width="200">
        <template #default="scope">
          <span :style="{ color: '#f56c6c', fontWeight: 600 }">{{ scope.row.word }}</span>
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" prop="category" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.category === 'politics'" type="danger" size="small">政治</el-tag>
          <el-tag v-else-if="scope.row.category === 'porn'" type="danger" size="small">色情</el-tag>
          <el-tag v-else-if="scope.row.category === 'ad'" type="warning" size="small">广告</el-tag>
          <el-tag v-else-if="scope.row.category === 'insult'" type="warning" size="small">辱骂</el-tag>
          <el-tag v-else-if="scope.row.category === 'other'" type="info" size="small">其他</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <dict-tag :options="sys_normal_disable" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:sensitiveWord:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:sensitiveWord:remove']">删除</el-button>
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

    <el-dialog :title="title" v-model="open" width="520px" append-to-body>
      <el-form ref="wordRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="form.word" placeholder="请输入敏感词" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" clearable style="width: 100%">
            <el-option label="政治" value="politics" />
            <el-option label="色情" value="porn" />
            <el-option label="广告" value="ad" />
            <el-option label="辱骂" value="insult" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="（选填）备注" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SensitiveWord">
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  listSensitiveWord,
  getSensitiveWord,
  addSensitiveWord,
  updateSensitiveWord,
  delSensitiveWord,
  reloadSensitiveWord
} from '@/api/system/sensitiveWord';

const sys_normal_disable = ref([
  { label: '启用', value: '0', type: 'success' },
  { label: '停用', value: '1', type: 'danger' }
]);

const loading = ref(false);
const showSearch = ref(true);
const ids = ref<Array<number>>([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const wordList = ref<any[]>([]);
const open = ref(false);
const submitLoading = ref(false);

const queryRef = ref();
const wordRef = ref();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  word: null as string | null,
  category: null as string | null,
  status: null as string | null
});

const title = computed(() => (form.value.id != null ? '修改敏感词' : '新增敏感词'));

const defaultForm = {
  id: null,
  word: '',
  category: null,
  status: '0',
  remark: ''
};
const form = ref<any>({ ...defaultForm });

const rules = {
  word: [{ required: true, message: '敏感词不能为空', trigger: 'blur' }]
};

/** 查询列表 */
async function getList() {
  loading.value = true;
  try {
    const params: any = {
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize
    };
    if (queryParams.word) params.word = queryParams.word;
    if (queryParams.category) params.category = queryParams.category;
    if (queryParams.status != null) params.status = queryParams.status;
    const res: any = await listSensitiveWord(params);
    wordList.value = res.rows || res.data?.rows || res.data?.records || [];
    total.value = res.total ?? res.data?.total ?? 0;
  } catch (error) {
    console.error('加载敏感词列表失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item: any) => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}
function resetQuery() {
  queryRef.value?.resetFields();
  queryParams.word = null;
  queryParams.category = null;
  queryParams.status = null;
  handleQuery();
}

/** 新增 */
function handleAdd() {
  form.value = { ...defaultForm };
  open.value = true;
}

/** 修改 */
async function handleUpdate(row: any) {
  form.value = { ...defaultForm };
  const id = row.id || ids.value[0];
  if (id != null) {
    const res: any = await getSensitiveWord(id);
    form.value = { ...defaultForm, ...(res.data || res) };
  }
  open.value = true;
}

/** 提交表单 */
async function submitForm() {
  if (!wordRef.value) return;
  const valid = await wordRef.value.validate().catch(() => false);
  if (!valid) return;
  submitLoading.value = true;
  try {
    if (form.value.id != null) {
      await updateSensitiveWord(form.value);
      ElMessage.success('修改成功');
    } else {
      await addSensitiveWord(form.value);
      ElMessage.success('新增成功');
    }
    open.value = false;
    getList();
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败');
  } finally {
    submitLoading.value = false;
  }
}

/** 删除（行按钮 / 批量） */
function handleDelete(row?: any) {
  const deleteIds = row?.id != null ? [row.id] : ids.value;
  if (deleteIds.length === 0) {
    ElMessage.warning('请选择要删除的数据');
    return;
  }
  const idsStr = deleteIds.join(',');
  ElMessageBox.confirm(
    '是否确认删除选中的 ' + deleteIds.length + ' 条敏感词？',
    '警告',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await delSensitiveWord(idsStr);
      ElMessage.success('删除成功');
      getList();
    } catch (error: any) {
      ElMessage.error(error?.message || '删除失败');
    }
  }).catch(() => {});
}

/** 刷新词树 */
async function handleReload() {
  try {
    await reloadSensitiveWord();
    ElMessage.success('词库已刷新');
  } catch (error: any) {
    ElMessage.error(error?.message || '刷新失败');
  }
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.text-muted {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
