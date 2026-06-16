<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="评论内容" prop="content">
        <el-input
          v-model="queryParams.content"
          placeholder="请输入评论内容"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 200px">
          <el-option label="正常" value="0" />
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
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cms:comment:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange" row-key="id" :tree-props="{children: 'children', hasChildren: 'hasChildren'}">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="评论编号" align="center" prop="id" width="80" />
      <el-table-column label="评论内容" align="center" prop="content" :show-overflow-tooltip="true" min-width="200" />
      <el-table-column label="用户昵称" align="center" prop="authorNickname" width="120" />
      <el-table-column label="文章标题" align="center" prop="articleTitle" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getAuditStatusType(scope.row.status)">
            {{ getAuditStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleView(scope.row)"
            v-hasPermi="['cms:comment:query']"
          >查看</el-button>
          <el-button
            link
            type="success"
            icon="Check"
            @click="handleAudit('0', scope.row)"
            v-hasPermi="['cms:comment:edit']"
          >启用</el-button>
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['cms:comment:remove']"
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

    <!-- 查看评论对话框 -->
    <el-dialog title="评论详情" v-model="viewOpen" width="600px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="评论编号">{{ viewForm.id }}</el-descriptions-item>
        <el-descriptions-item label="用户昵称">{{ viewForm.authorNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="文章标题">{{ viewForm.articleTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评论内容">
          <span>{{ viewForm.content }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getAuditStatusType(viewForm.status)">
            {{ getAuditStatusText(viewForm.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(viewForm.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewOpen = false">关 闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsComment">
import { listComment, getComment, delComment, auditComment } from "@/api/cms/comment";

const { proxy } = getCurrentInstance();

// 表格数据
const commentList = ref([]);
const open = ref(false);
const viewOpen = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const title = ref("");

// 列显隐信息
const columns = ref([
  { key: 0, label: `评论编号`, visible: true },
  { key: 1, label: `评论内容`, visible: true },
  { key: 2, label: `用户昵称`, visible: true },
  { key: 3, label: `文章标题`, visible: true },
  { key: 4, label: `审核状态`, visible: true },
  { key: 5, label: `创建时间`, visible: true }
]);

// 查询参数
const data = reactive({
  form: {},
  viewForm: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    content: undefined,
    status: undefined
  },
  rules: {}
});

const { queryParams, form, viewForm, rules } = toRefs(data);

// 获取状态类型
function getAuditStatusType(status) {
  const typeMap = {
    '0': 'success',
    '1': 'danger'
  };
  return typeMap[status] || 'info';
}

// 获取状态文本
function getAuditStatusText(status) {
  const textMap = {
    '0': '正常',
    '1': '停用'
  };
  return textMap[status] || '未知';
}

// 查询评论列表
function getList() {
  loading.value = true;
  listComment(queryParams.value).then(response => {
    commentList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}

// 搜索按钮操作
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

// 重置按钮操作
function resetQuery() {
  proxy.resetForm("queryRef");
  handleQuery();
}

// 表格多选
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  single.value = selection.length !== 1;
  multiple.value = !selection.length;
}

// 查看按钮操作
function handleView(row) {
  const id = row.id;
  getComment(id).then(response => {
    viewForm.value = response.data;
    viewOpen.value = true;
  });
}

// 删除按钮操作
function handleDelete(row) {
  const commentIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除评论编号为"' + commentIds + '"的数据项？').then(function () {
    return delComment(commentIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

// 启用评论
function handleAudit(command, row) {
  const statusText = command === "0" ? "启用" : "停用";
  proxy.$modal.confirm('确认要' + statusText + '该评论吗？').then(function () {
    return auditComment(row.id, command);
  }).then(() => {
    proxy.$modal.msgSuccess("操作成功");
    getList();
  }).catch(() => {});
}

// 初始化查询
getList();
</script>
