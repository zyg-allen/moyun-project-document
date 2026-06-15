<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="书单名称" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入书单名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-input v-model="queryParams.categoryId" placeholder="请输入分类ID" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="是否公开" prop="isPublic">
        <el-select v-model="queryParams.isPublic" placeholder="是否公开" clearable style="width: 120px">
          <el-option label="是" :value="true" />
          <el-option label="否" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="正常" value="active" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['portal:bookList:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['portal:bookList:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['portal:bookList:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="书单名称" align="center" prop="title" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="封面" align="center" prop="cover" width="90">
        <template #default="scope">
          <el-image v-if="scope.row.cover" :src="scope.row.cover" :preview-src-list="[scope.row.cover]" fit="cover" style="width: 60px; height: 80px" />
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center" prop="description" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="分类" align="center" prop="categoryId" width="80" />
      <el-table-column label="标签" align="center" prop="tags" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="书籍数" align="center" prop="bookCount" width="80" />
      <el-table-column label="浏览数" align="center" prop="viewCount" width="80" />
      <el-table-column label="点赞数" align="center" prop="likeCount" width="80" />
      <el-table-column label="访问级别" align="center" prop="accessLevel" width="100">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.accessLevel === 'vip' ? 'warning' : 'success'">
            {{ scope.row.accessLevel === 'vip' ? 'VIP' : '免费' }}
          </el-tag>
        </template>
      </el-table-column>
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
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'" size="small">
            {{ scope.row.status === 'active' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:bookList:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['portal:bookList:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="720px" append-to-body>
      <el-form ref="refEl" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="书单名称" prop="title">
              <el-input v-model="form.title" placeholder="请输入书单名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="封面URL" prop="cover">
              <el-input v-model="form.cover" placeholder="请输入封面URL" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类ID" prop="categoryId">
              <el-input-number v-model="form.categoryId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建者ID" prop="userId">
              <el-input-number v-model="form.userId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标签" prop="tags">
              <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="访问级别" prop="accessLevel">
              <el-select v-model="form.accessLevel" placeholder="请选择" style="width: 100%">
                <el-option label="免费公开" value="free" />
                <el-option label="会员专享" value="vip" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="active">正常</el-radio>
                <el-radio label="inactive">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否公开" prop="isPublic">
              <el-switch v-model="form.isPublic" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否精选" prop="isFeatured">
              <el-switch v-model="form.isFeatured" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="书籍数" prop="bookCount">
              <el-input-number v-model="form.bookCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="浏览数" prop="viewCount">
              <el-input-number v-model="form.viewCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="点赞数" prop="likeCount">
              <el-input-number v-model="form.likeCount" :min="0" style="width: 100%" />
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

<script setup name="BookList">
import { ref, reactive, getCurrentInstance, onMounted } from "vue";
import { listBookList, addBookList, updateBookList, delBookList, delBookListBatch, getBookList } from "@/api/portal/bookList";

const { proxy } = getCurrentInstance();

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: null,
  categoryId: null,
  isPublic: null,
  status: null
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
  title: null,
  description: null,
  cover: null,
  userId: 1,
  categoryId: null,
  isPublic: true,
  bookCount: 0,
  viewCount: 0,
  likeCount: 0,
  status: "active",
  isFeatured: false,
  accessLevel: "free",
  tags: null
});

const rules = {
  title: [{ required: true, message: "书单名称不能为空", trigger: "blur" }],
  userId: [{ required: true, message: "创建者ID不能为空", trigger: "blur" }]
};

const columns = [
  { key: 0, label: "编号", visible: true, prop: "id" },
  { key: 1, label: "书单名称", visible: true, prop: "title" },
  { key: 2, label: "封面", visible: true, prop: "cover" },
  { key: 3, label: "描述", visible: true, prop: "description" },
  { key: 4, label: "分类", visible: true, prop: "categoryId" },
  { key: 5, label: "标签", visible: true, prop: "tags" },
  { key: 6, label: "书籍数", visible: true, prop: "bookCount" },
  { key: 7, label: "浏览数", visible: true, prop: "viewCount" },
  { key: 8, label: "点赞数", visible: true, prop: "likeCount" },
  { key: 9, label: "访问级别", visible: true, prop: "accessLevel" },
  { key: 10, label: "是否公开", visible: true, prop: "isPublic" },
  { key: 11, label: "是否精选", visible: true, prop: "isFeatured" },
  { key: 12, label: "状态", visible: true, prop: "status" },
  { key: 13, label: "创建时间", visible: true, prop: "createTime" }
];

function getList() {
  loading.value = true;
  listBookList(queryParams).then((response) => {
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
    title: null,
    description: null,
    cover: null,
    userId: 1,
    categoryId: null,
    isPublic: true,
    bookCount: 0,
    viewCount: 0,
    likeCount: 0,
    status: "active",
    isFeatured: false,
    accessLevel: "free",
    tags: null
  };
  if (refEl.value) refEl.value.resetFields();
}

function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.title = null;
  queryParams.categoryId = null;
  queryParams.isPublic = null;
  queryParams.status = null;
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
  title.value = "新增书单";
  open.value = true;
}

function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0].id;
  getBookList(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    title.value = "修改书单";
    open.value = true;
  }).catch((e) => {
    proxy.$modal.msgError("查询详情失败: " + e.message);
  });
}

function submitForm() {
  refEl.value.validate((valid) => {
    if (valid) {
      const action = form.value.id ? updateBookList(form.value) : addBookList(form.value);
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
  proxy.$modal.confirm('是否确认删除书单编号为"' + ids + '"的数据项？').then(() => {
    const action = row.id ? delBookList(ids) : delBookListBatch(ids);
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
