<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="关键词" prop="keyword">
        <el-input
          v-model="queryParams.keyword"
          placeholder="专栏名/副标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 160px">
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已归档" value="archived" />
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
          v-hasPermi="['portal:column:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['portal:column:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['portal:column:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="专栏名" align="center" prop="title" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="作者" align="center" prop="authorName" width="140" :show-overflow-tooltip="true">
        <template #default="scope">
          <span>{{ scope.row.authorName || '-' }}</span>
          <div style="font-size: 12px; color: #909399;">ID: {{ scope.row.userId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="文章数" align="center" prop="articleCount" width="80" />
      <el-table-column label="订阅数" align="center" prop="subscribeCount" width="80" />
      <el-table-column label="浏览数" align="center" prop="viewCount" width="80" />
      <el-table-column label="完结" align="center" prop="isFinished" width="70">
        <template #default="scope">
          <el-tag :type="scope.row.isFinished ? 'success' : 'info'" size="small">
            {{ scope.row.isFinished ? '已完结' : '连载中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="价格" align="center" prop="price" width="90">
        <template #default="scope">
          <span>{{ scope.row.price > 0 ? '¥' + scope.row.price : '免费' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusTagType(scope.row.status)" size="small">
            {{ getStatusText(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['portal:column:edit']"
          >修改</el-button>
          <el-button
            link
            type="warning"
            icon="View"
            @click="handleAuditPage(scope.row)"
            v-hasPermi="['cms:column:audit']"
          >审核</el-button>
          <el-button
            link
            type="info"
            icon="Switch"
            @click="handleStatus(scope.row)"
            v-hasPermi="['portal:column:edit']"
          >状态</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['portal:column:remove']"
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

    <!-- 添加或修改专栏对话框 -->
    <el-dialog :title="title" v-model="open" width="720px" append-to-body>
      <el-form ref="columnRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="专栏名" prop="title">
              <el-input v-model="form.title" placeholder="请输入专栏名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题" prop="subtitle">
              <el-input v-model="form.subtitle" placeholder="请输入副标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="作者ID" prop="userId">
              <el-input-number v-model="form.userId" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类ID" prop="categoryId">
              <el-input-number v-model="form.categoryId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="封面" prop="cover">
              <el-input v-model="form.cover" placeholder="封面URL（选填）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :step="1" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="是否完结" prop="isFinished">
              <el-radio-group v-model="form.isFinished">
                <el-radio :label="0">连载中</el-radio>
                <el-radio :label="1">已完结</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="draft">草稿</el-radio>
                <el-radio label="published">已发布</el-radio>
                <el-radio label="archived">已归档</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="专栏简介" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入专栏简介" />
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

    <!-- 状态变更对话框 -->
    <el-dialog title="专栏状态变更" v-model="statusOpen" width="420px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="专栏名">
          <span>{{ currentRow.title }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="getStatusTagType(currentRow.status)">{{ getStatusText(currentRow.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="statusForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="已归档" value="archived" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitStatus">确 定</el-button>
          <el-button @click="statusOpen = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="CmsColumn">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { listColumn, getColumn, addColumn, updateColumn, delColumn, changeColumnStatus } from "@/api/cms/column";

const { proxy } = getCurrentInstance();
const router = useRouter();

/** 跳转到专栏审核页（与文章审核入口一致） */
function handleAuditPage(row) {
  router.push({
    path: "/cms/column-audit",
    query: { id: row.id }
  });
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: null,
  status: null
});

const showSearch = ref(true);
const loading = ref(false);
const dataList = ref([]);
const total = ref(0);
const selectedRows = ref([]);
const single = ref(true);
const multiple = ref(true);

const open = ref(false);
const title = ref("");
const columnRef = ref();
const statusOpen = ref(false);
const currentRow = ref({});
const statusForm = reactive({ id: null, status: null });

const defaultForm = () => ({
  id: null,
  userId: null,
  title: null,
  subtitle: null,
  description: null,
  cover: null,
  categoryId: null,
  status: "draft",
  isFinished: 0,
  price: 0
});
const form = ref(defaultForm());

const rules = {
  title: [{ required: true, message: "专栏名不能为空", trigger: "blur" }],
  userId: [{ required: true, message: "作者ID不能为空", trigger: "blur" }],
  status: [{ required: true, message: "状态不能为空", trigger: "change" }]
};

function getStatusText(status) {
  const map = { draft: "草稿", published: "已发布", archived: "已归档" };
  return map[status] || status || "-";
}

function getStatusTagType(status) {
  const map = { draft: "info", published: "success", archived: "warning" };
  return map[status] || "info";
}

function getList() {
  loading.value = true;
  listColumn(queryParams).then((response) => {
    dataList.value = response.data.records || [];
    total.value = response.data.total || 0;
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
  form.value = defaultForm();
  if (columnRef.value) columnRef.value.resetFields();
}

function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.keyword = null;
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
  title.value = "新增专栏";
  open.value = true;
}

function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0].id;
  getColumn(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    title.value = "修改专栏";
    open.value = true;
  }).catch((e) => {
    proxy.$modal.msgError("查询详情失败: " + e.message);
  });
}

function submitForm() {
  columnRef.value.validate((valid) => {
    if (valid) {
      const action = form.value.id ? updateColumn(form.value) : addColumn(form.value);
      action.then((response) => {
        if (response.code === 200) {
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
  proxy.$modal.confirm('是否确认删除专栏编号为"' + ids + '"的数据项？').then(() => {
    delColumn(ids).then((response) => {
      if (response.code === 200) {
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

function handleStatus(row) {
  currentRow.value = row;
  statusForm.id = row.id;
  statusForm.status = row.status;
  statusOpen.value = true;
}

function submitStatus() {
  changeColumnStatus(statusForm.id, statusForm.status).then((response) => {
    if (response.code === 200) {
      proxy.$modal.msgSuccess("状态变更成功");
      statusOpen.value = false;
      getList();
    } else {
      proxy.$modal.msgError(response.msg || "状态变更失败");
    }
  }).catch((e) => {
    proxy.$modal.msgError("状态变更失败: " + e.message);
  });
}

onMounted(() => {
  getList();
});
</script>
