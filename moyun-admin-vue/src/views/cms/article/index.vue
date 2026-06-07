<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="文章标题" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入文章标题"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="顶级分类" prop="rootCategoryId">
        <el-select v-model="queryParams.rootCategoryId" placeholder="请选择顶级分类" clearable style="width: 200px">
          <el-option
            v-for="item in rootCategoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width: 200px">
          <el-option
            v-for="item in categoryOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="发布状态" clearable style="width: 200px">
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
          v-hasPermi="['cms:article:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['cms:article:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="文章编号" align="center" prop="id" width="80" />
      <el-table-column label="文章标题" align="center" prop="title" width="200" :show-overflow-tooltip="true" />
      <el-table-column label="封面" align="center" prop="cover" width="100">
        <template #default="scope">
          <el-image
            v-if="scope.row.cover"
            :src="scope.row.cover"
            :preview-src-list="[scope.row.cover]"
            fit="cover"
            style="width: 80px; height: 50px"
          />
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" prop="categoryName" width="120" />
      <el-table-column label="作者" align="center" prop="authorNickname" width="100" />
      <el-table-column label="浏览量" align="center" prop="views" width="80" />
      <el-table-column label="发布状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusText(scope.row.status) }}
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
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['cms:article:edit']"
          >修改</el-button>
          <el-button
            link
            type="success"
            icon="Check"
            @click="handleAudit(scope.row, 'published')"
            v-if="scope.row.status === 'draft'"
            v-hasPermi="['cms:article:audit']"
          >通过</el-button>
          <el-button
            link
            type="danger"
            icon="Close"
            @click="handleAudit(scope.row, 'archived')"
            v-if="scope.row.status === 'draft'"
            v-hasPermi="['cms:article:audit']"
          >拒绝</el-button>
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['cms:article:remove']"
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
  </div>
</template>

<script setup name="CmsArticle">
import { listArticle, delArticle, auditArticle } from "@/api/cms/article";
import { listCategory } from "@/api/cms/category";

const { proxy } = getCurrentInstance();
const router = useRouter();

// 表格数据
const articleList = ref([]);
const categoryOptions = ref([]);
const rootCategoryOptions = ref([]);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);

// 列显隐信息
const columns = ref([
  { key: 0, label: "文章编号", visible: true },
  { key: 1, label: "文章标题", visible: true },
  { key: 2, label: "封面", visible: true },
  { key: 3, label: "分类", visible: true },
  { key: 4, label: "作者", visible: true },
  { key: 5, label: "浏览量", visible: true },
  { key: 6, label: "发布状态", visible: true },
  { key: 7, label: "创建时间", visible: true }
]);

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: undefined,
  rootCategoryId: undefined,
  categoryId: undefined,
  status: undefined
});

// 获取状态类型
function getStatusType(status) {
  const typeMap = {
    draft: "warning",
    published: "success",
    archived: "info"
  };
  return typeMap[status] || "info";
}

// 获取状态文本
function getStatusText(status) {
  const textMap = {
    draft: "草稿",
    published: "已发布",
    archived: "已归档"
  };
  return textMap[status] || "未知";
}

// 查询文章列表
function getList() {
  loading.value = true;
  listArticle(queryParams).then(response => {
    articleList.value = response.rows || [];
    total.value = response.total || 0;
    loading.value = false;
  });
}

// 查询分类列表
function getCategoryList() {
  listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
    categoryOptions.value = response.rows || [];
    // 筛选出顶级分类（parentId 为空或 0）
    rootCategoryOptions.value = categoryOptions.value.filter(item => !item.parentId || item.parentId === 0);
  });
}

// 搜索按钮操作
function handleQuery() {
  queryParams.pageNum = 1;
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

// 新增按钮操作 - 跳转到编辑页面
function handleAdd() {
  router.push('/cms/article/edit');
}

// 修改按钮操作 - 跳转到编辑页面
function handleUpdate(row) {
  const id = row.id || ids.value[0];
  router.push(`/cms/article/edit/${id}`);
}

// 删除按钮操作
function handleDelete(row) {
  const articleIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除文章编号为"' + articleIds + '"的数据项？').then(function () {
    return delArticle(articleIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}

// 文章审核
function handleAudit(row, status) {
  const statusText = status === 'published' ? '通过' : '拒绝';
  proxy.$modal.confirm('确认要' + statusText + '该文章吗？').then(function () {
    return auditArticle(row.id, status);
  }).then(() => {
    proxy.$modal.msgSuccess("审核成功");
    getList();
  }).catch(() => {});
}

// 初始化查询
getCategoryList();
getList();
</script>
