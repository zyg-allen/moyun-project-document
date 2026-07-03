<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="活动标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入活动标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="活动状态" clearable style="width: 200px">
          <el-option label="草稿" value="draft" />
          <el-option label="征稿中" value="collecting" />
          <el-option label="投票中" value="voting" />
          <el-option label="已结束" value="ended" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['cms:contest:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['cms:contest:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['cms:contest:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="contestList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="80" />
      <el-table-column label="活动标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="主题" align="center" prop="theme" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.startTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="投稿截止" align="center" prop="endTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.endTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="投票截止" align="center" prop="voteEndTime" width="160">
        <template #default="scope">
          <span>{{ scope.row.voteEndTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdTime" width="180">
        <template #default="scope">
          <span>{{ scope.row.createdTime || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['cms:contest:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['cms:contest:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="640px" append-to-body>
      <el-form ref="contestRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>
        <el-form-item label="征文主题" prop="theme">
          <el-input v-model="form.theme" placeholder="请输入征文主题" />
        </el-form-item>
        <el-form-item label="封面" prop="cover">
          <el-input v-model="form.cover" placeholder="请输入封面URL" />
        </el-form-item>
        <el-form-item label="活动描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入活动描述" />
        </el-form-item>
        <el-form-item label="奖品说明" prop="prize">
          <el-input v-model="form.prize" type="textarea" :rows="2" placeholder="请输入奖品说明" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择开始时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="投稿截止" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择投稿截止时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="投票截止" prop="voteEndTime">
          <el-date-picker v-model="form.voteEndTime" type="datetime" placeholder="选择投票截止时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="draft">草稿</el-radio>
            <el-radio label="collecting">征稿中</el-radio>
            <el-radio label="voting">投票中</el-radio>
            <el-radio label="ended">已结束</el-radio>
          </el-radio-group>
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

<script setup name="Contest">
import { listContest, getContest, addContest, updateContest, delContest } from "@/api/cms/contest";

const { proxy } = getCurrentInstance();

const contestList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    title: undefined,
    status: undefined
  },
  rules: {
    title: [{ required: true, message: "活动标题不能为空", trigger: "blur" }],
    status: [{ required: true, message: "请选择状态", trigger: "change" }]
  }
});

const { queryParams, form, rules } = toRefs(data);

function statusLabel(status) {
  const map = { draft: '草稿', collecting: '征稿中', voting: '投票中', ended: '已结束' };
  return map[status] || status;
}

function statusTagType(status) {
  const map = { draft: 'info', collecting: 'success', voting: 'warning', ended: '' };
  return map[status] || '';
}

function getList() {
  loading.value = true;
  listContest(queryParams.value).then(response => {
    contestList.value = response.data.records;
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
    title: undefined,
    theme: undefined,
    cover: undefined,
    description: undefined,
    prize: undefined,
    startTime: undefined,
    endTime: undefined,
    voteEndTime: undefined,
    status: "draft"
  };
  proxy.resetForm("contestRef");
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
  title.value = "添加创作挑战";
}

function handleUpdate(row) {
  reset();
  const id = row.id || ids.value;
  getContest(id).then(response => {
    form.value = response.data;
    open.value = true;
    title.value = "修改创作挑战";
  });
}

function submitForm() {
  proxy.$refs["contestRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateContest(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功");
          open.value = false;
          getList();
        });
      } else {
        addContest(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功");
          open.value = false;
          getList();
        });
      }
    }
  });
}

function handleDelete(row) {
  const contestIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除编号为"' + contestIds + '"的数据项？').then(function() {
    return delContest(contestIds);
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

getList();
</script>
