<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="模块" prop="module">
        <el-select v-model="queryParams.module" placeholder="请选择模块" clearable style="width: 200px">
          <el-option label="文章" value="article" />
          <el-option label="读书" value="reading" />
          <el-option label="面试" value="interview" />
          <el-option label="通用" value="all" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['cms:growth:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['cms:growth:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cms:growth:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" align="center" prop="id" width="80" v-if="columns[0].visible" />
      <el-table-column label="模块" align="center" prop="module" width="100" v-if="columns[1].visible">
        <template #default="scope">
          <el-tag :type="getModuleTagType(scope.row.module)">{{ getModuleText(scope.row.module) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="行为编码" align="center" prop="action" width="180" :show-overflow-tooltip="true" v-if="columns[2].visible" />
      <el-table-column label="成长值" align="center" prop="growthDelta" width="100" v-if="columns[3].visible" />
      <el-table-column label="每日上限" align="center" prop="dailyLimit" width="100" v-if="columns[4].visible">
        <template #default="scope">
          <span>{{ scope.row.dailyLimit === 0 ? '不限' : scope.row.dailyLimit }}</span>
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" min-width="180" :show-overflow-tooltip="true" v-if="columns[5].visible" />
      <el-table-column label="状态" align="center" prop="status" width="80" v-if="columns[6].visible">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="80" v-if="columns[7].visible" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" v-if="columns[8].visible">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:growth:edit']"
          >修改</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['cms:growth:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改成长规则对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模块" prop="module">
              <el-select v-model="form.module" placeholder="请选择模块" style="width: 100%">
                <el-option label="文章" value="article" />
                <el-option label="读书" value="reading" />
                <el-option label="面试" value="interview" />
                <el-option label="通用" value="all" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行为编码" prop="action">
              <el-input v-model="form.action" placeholder="如 article_publish" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="成长值" prop="growthDelta">
              <el-input-number v-model="form.growthDelta" controls-position="right" :min="-1000" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="每日上限" prop="dailyLimit">
              <el-input-number v-model="form.dailyLimit" controls-position="right" :min="0" :max="1000" style="width: 100%" />
              <div style="color: #909399; font-size: 12px; margin-top: 4px;">0 表示不限</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" controls-position="right" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="0">启用</el-radio>
                <el-radio label="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
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

<script setup name="CmsGrowthRule">
import { listGrowthRule, getGrowthRule, addGrowthRule, updateGrowthRule, delGrowthRule } from "@/api/cms/growth";

const { proxy } = getCurrentInstance();

const dataList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const columns = ref([
  { key: 0, label: `ID`, visible: true },
  { key: 1, label: `模块`, visible: true },
  { key: 2, label: `行为编码`, visible: true },
  { key: 3, label: `成长值`, visible: true },
  { key: 4, label: `每日上限`, visible: true },
  { key: 5, label: `描述`, visible: true },
  { key: 6, label: `状态`, visible: true },
  { key: 7, label: `排序`, visible: true },
  { key: 8, label: `创建时间`, visible: true }
]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    module: undefined,
    status: undefined
  },
  rules: {
    module: [{ required: true, message: "模块不能为空", trigger: "change" }],
    action: [{ required: true, message: "行为编码不能为空", trigger: "blur" }],
    growthDelta: [{ required: true, message: "成长值不能为空", trigger: "blur" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

/** 模块文本 */
function getModuleText(module) {
  const map = { article: '文章', reading: '读书', interview: '面试', all: '通用' };
  return map[module] || module || '-';
}

/** 模块标签类型 */
function getModuleTagType(module) {
  const map = { article: '', reading: 'success', interview: 'warning', all: 'info' };
  return map[module] || '';
}

/** 查询列表 */
function getList() {
  loading.value = true;
  listGrowthRule(queryParams.value).then(response => {
    dataList.value = (response.data && response.data.records) || response.rows || [];
    total.value = (response.data && response.data.total) || response.total || 0;
    loading.value = false;
  });
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    module: undefined,
    action: undefined,
    growthDelta: 0,
    dailyLimit: 0,
    description: undefined,
    status: "0",
    sort: 0
  };
  proxy.resetForm("formRef");
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 表格多选 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

/** 新增按钮操作 */
function handleAdd() {
  reset();
  open.value = true;
  title.value = "新增成长规则";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const id = row.id || ids.value[0];
  getGrowthRule(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改成长规则";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateGrowthRule(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addGrowthRule(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

/** 删除按钮操作 */
function handleDelete(row) {
  const ruleIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除成长规则编号为"' + ruleIds + '"的数据项？').then(function () {
    return delGrowthRule(ruleIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
