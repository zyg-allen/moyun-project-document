<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="金句内容" prop="content">
        <el-input v-model="queryParams.content" placeholder="请输入金句内容" clearable style="width: 250px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="书籍ID" prop="bookId">
        <el-input v-model="queryParams.bookId" placeholder="请输入书籍ID" clearable style="width: 120px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否公开" prop="isPublic">
        <el-select v-model="queryParams.isPublic" placeholder="是否公开" clearable style="width: 120px">
          <el-option label="是" :value="true" />
          <el-option label="否" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否精选" prop="isFeatured">
        <el-select v-model="queryParams.isFeatured" placeholder="是否精选" clearable style="width: 120px">
          <el-option label="是" :value="true" />
          <el-option label="否" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['portal:bookQuote:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['portal:bookQuote:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:bookQuote:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="书籍ID" align="center" prop="bookId" width="80" />
      <el-table-column label="金句内容" align="left" prop="content" :show-overflow-tooltip="true" min-width="300" />
      <el-table-column label="页码" align="center" prop="page" width="100" />
      <el-table-column label="章节" align="center" prop="chapter" width="120" />
      <el-table-column label="位置" align="center" prop="location" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="点赞数" align="center" prop="likeCount" width="80" />
      <el-table-column label="是否公开" align="center" prop="isPublic" width="90">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.isPublic ? 'success' : 'info'">
            {{ scope.row.isPublic ? '公开' : '私有' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否精选" align="center" prop="isFeatured" width="90">
        <template #default="scope">
          <el-tag v-if="scope.row.isFeatured" type="warning" size="small">精选</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:bookQuote:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:bookQuote:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form ref="refEl" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="书籍ID" prop="bookId">
              <el-input-number v-model="form.bookId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户ID" prop="userId">
              <el-input-number v-model="form.userId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="金句内容" prop="content">
              <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入金句内容" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="页码" prop="page">
              <el-input v-model="form.page" placeholder="请输入页码" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="章节" prop="chapter">
              <el-input v-model="form.chapter" placeholder="请输入章节" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="位置描述" prop="location">
              <el-input v-model="form.location" placeholder="位置描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="点赞数" prop="likeCount">
              <el-input-number v-model="form.likeCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否公开" prop="isPublic">
              <el-switch v-model="form.isPublic" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否精选" prop="isFeatured">
              <el-switch v-model="form.isFeatured" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="BookQuote">
import { ref, reactive, getCurrentInstance, onMounted } from "vue";
import { listBookQuote, addBookQuote, updateBookQuote, delBookQuote, delBookQuoteBatch, getBookQuote } from "@/api/portal/bookQuote";

const { proxy } = getCurrentInstance();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  content: null,
  bookId: null,
  isPublic: null,
  isFeatured: null
});

const showSearch = ref(true);
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const selectedRows = ref([]);
const single = ref(true);
const multiple = ref(true);

const open = ref(false);
const title = ref("");
const refEl = ref();
const form = ref({
  id: null,
  userId: 1,
  bookId: null,
  content: null,
  page: null,
  chapter: null,
  location: null,
  likeCount: 0,
  isPublic: true,
  isFeatured: false
});

const rules = {
  content: [{ required: true, message: "金句内容不能为空", trigger: "blur" }],
  bookId: [{ required: true, message: "书籍ID不能为空", trigger: "blur" }],
  userId: [{ required: true, message: "用户ID不能为空", trigger: "blur" }]
};

const columns = [
  { key: 0, label: "编号", visible: true, prop: "id" },
  { key: 1, label: "书籍ID", visible: true, prop: "bookId" },
  { key: 2, label: "金句内容", visible: true, prop: "content" },
  { key: 3, label: "页码", visible: true, prop: "page" },
  { key: 4, label: "章节", visible: true, prop: "chapter" },
  { key: 5, label: "位置", visible: true, prop: "location" },
  { key: 6, label: "点赞数", visible: true, prop: "likeCount" },
  { key: 7, label: "是否公开", visible: true, prop: "isPublic" },
  { key: 8, label: "是否精选", visible: true, prop: "isFeatured" },
  { key: 9, label: "创建时间", visible: true, prop: "createTime" }
];

function getList() {
  loading.value = true;
  listBookQuote(queryParams).then((response) => {
    const rows = response.rows || response.data?.rows || (response.data ? (Array.isArray(response.data) ? response.data : response.data.records) : []);
    const totalCount = response.total || response.data?.total || 0;
    list.value = rows;
    total.value = totalCount;
    loading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("查询失败: " + e.message);
    loading.value = false;
  });
}

function cancel() {
  open.value = false;
  resetForm();
}

function resetForm() {
  form.value = {
    id: null,
    userId: 1,
    bookId: null,
    content: null,
    page: null,
    chapter: null,
    location: null,
    likeCount: 0,
    isPublic: true,
    isFeatured: false
  };
  if (refEl.value) refEl.value.resetFields();
}

function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.content = null;
  queryParams.bookId = null;
  queryParams.isPublic = null;
  queryParams.isFeatured = null;
  handleQuery();
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function handleSelectionChange(selection) {
  selectedRows.value = selection;
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

function handleAdd() {
  resetForm();
  title.value = "新增金句";
  open.value = true;
}

function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0].id;
  getBookQuote(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    title.value = "修改金句";
    open.value = true;
  }).catch((e) => {
    proxy.$modal.msgError("查询详情失败: " + e.message);
  });
}

function submitForm() {
  refEl.value.validate((valid) => {
    if (valid) {
      const action = form.value.id ? updateBookQuote(form.value) : addBookQuote(form.value);
      action.then((response) => {
        const code = response.code;
        if (code === 200) {
          proxy.$modal.msgSuccess("操作成功");
          open.value = false;
          getList();
        } else {
          proxy.$modal.msgError(response.msg || "操作失败");
        }
      }).catch((e) => {
        proxy.$modal.msgError("操作失败: " + e.message);
      });
    }
  });
}

function handleDelete(row) {
  const ids = row.id ? row.id : selectedRows.value.map((r) => r.id).join(",");
  proxy.$modal.confirm('是否确认删除金句编号为"' + ids + '"的数据项？').then(() => {
    const action = row.id ? delBookQuote(ids) : delBookQuoteBatch(ids);
    action.then((response) => {
      const code = response.code;
      if (code === 200) {
        proxy.$modal.msgSuccess("删除成功");
        getList();
      } else {
        proxy.$modal.msgError(response.msg || "删除失败");
      }
    }).catch((e) => {
      proxy.$modal.msgError("删除失败: " + e.message);
    });
  }).catch(() => {});
}

onMounted(() => {
  getList();
});
</script>
