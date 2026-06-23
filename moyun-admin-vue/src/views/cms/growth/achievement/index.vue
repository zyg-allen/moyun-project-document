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
      <el-form-item label="名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入成就名称"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
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
      <el-table-column label="编码" align="center" prop="code" width="180" :show-overflow-tooltip="true" v-if="columns[1].visible" />
      <el-table-column label="名称" align="center" prop="name" width="160" :show-overflow-tooltip="true" v-if="columns[2].visible" />
      <el-table-column label="模块" align="center" prop="module" width="100" v-if="columns[3].visible">
        <template #default="scope">
          <el-tag :type="getModuleTagType(scope.row.module)">{{ getModuleText(scope.row.module) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="奖励成长值" align="center" prop="growthReward" width="110" v-if="columns[4].visible" />
      <el-table-column label="排序" align="center" prop="sort" width="80" v-if="columns[5].visible" />
      <el-table-column label="状态" align="center" prop="status" width="80" v-if="columns[6].visible">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'danger'">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" v-if="columns[7].visible">
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

    <!-- 添加或修改成就对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="成就编码" prop="code">
              <el-input v-model="form.code" placeholder="如 first_article" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成就名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入成就名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属模块" prop="module">
              <el-select v-model="form.module" placeholder="请选择模块" style="width: 100%">
                <el-option label="文章" value="article" />
                <el-option label="读书" value="reading" />
                <el-option label="面试" value="interview" />
                <el-option label="通用" value="all" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="奖励成长值" prop="growthReward">
              <el-input-number v-model="form.growthReward" controls-position="right" :min="0" :max="100000" style="width: 100%" />
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
        <el-form-item label="图标URL" prop="icon">
          <el-input v-model="form.icon" placeholder="请输入图标URL" />
        </el-form-item>
        <el-form-item label="成就描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入成就描述" />
        </el-form-item>
        <el-form-item label="达成条件JSON" prop="conditionJson">
          <el-input v-model="form.conditionJson" type="textarea" :rows="4" placeholder='请输入达成条件JSON，如 {"type":"count","target":1}' />
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

<script setup name="CmsGrowthAchievement">
import { listGrowthAchievement, getGrowthAchievement, addGrowthAchievement, updateGrowthAchievement, delGrowthAchievement } from "@/api/cms/growth";

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
  { key: 1, label: `编码`, visible: true },
  { key: 2, label: `名称`, visible: true },
  { key: 3, label: `模块`, visible: true },
  { key: 4, label: `奖励成长值`, visible: true },
  { key: 5, label: `排序`, visible: true },
  { key: 6, label: `状态`, visible: true },
  { key: 7, label: `创建时间`, visible: true }
]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    module: undefined,
    status: undefined,
    name: undefined
  },
  rules: {
    code: [{ required: true, message: "成就编码不能为空", trigger: "blur" }],
    name: [{ required: true, message: "成就名称不能为空", trigger: "blur" }],
    module: [{ required: true, message: "所属模块不能为空", trigger: "change" }]
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
  listGrowthAchievement(queryParams.value).then(response => {
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
    code: undefined,
    name: undefined,
    description: undefined,
    icon: undefined,
    module: undefined,
    conditionJson: undefined,
    growthReward: 0,
    sort: 0,
    status: "0"
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
  title.value = "新增成就";
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset();
  const id = row.id || ids.value[0];
  getGrowthAchievement(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改成就";
  });
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateGrowthAchievement(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addGrowthAchievement(form.value).then(response => {
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
  const achievementIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除成就编号为"' + achievementIds + '"的数据项？').then(function () {
    return delGrowthAchievement(achievementIds);
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
