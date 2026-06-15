<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="书名" prop="title">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入书名"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="作者" prop="author">
        <el-input
          v-model="queryParams.author"
          placeholder="请输入作者"
          clearable
          style="width: 200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-input
          v-model="queryParams.categoryId"
          placeholder="请输入分类ID"
          clearable
          style="width: 150px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 150px">
          <el-option label="正常" value="active" />
          <el-option label="停用" value="inactive" />
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

    <!-- 操作按钮 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Plus"
          @click="handleAdd"
          v-hasPermi="['portal:book:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="Edit"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['portal:book:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="Delete"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['portal:book:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="bookList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="编号" align="center" prop="id" width="70" />
      <el-table-column label="书名" align="center" prop="title" width="180" :show-overflow-tooltip="true" />
      <el-table-column label="作者" align="center" prop="author" width="100" :show-overflow-tooltip="true" />
      <el-table-column label="封面" align="center" prop="cover" width="90">
        <template #default="scope">
          <el-image
            v-if="scope.row.cover"
            :src="scope.row.cover"
            :preview-src-list="[scope.row.cover]"
            fit="cover"
            style="width: 60px; height: 80px"
          />
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" prop="categoryId" width="80" />
      <el-table-column label="标签" align="center" prop="tags" width="150" :show-overflow-tooltip="true" />
      <el-table-column label="评分" align="center" prop="rating" width="90">
        <template #default="scope">
          <el-tag size="small" v-if="scope.row.rating">{{ scope.row.rating }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="阅读数" align="center" prop="readingCount" width="90" />
      <el-table-column label="访问级别" align="center" prop="accessLevel" width="100">
        <template #default="scope">
          <el-tag :type="getAccessLevelType(scope.row.accessLevel)" size="small">
            {{ getAccessLevelText(scope.row.accessLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否精选" align="center" prop="isFeatured" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isFeatured" type="warning" size="small">精选</el-tag>
          <span v-else>否</span>
        </template>
      </el-table-column>
      <el-table-column label="是否推荐" align="center" prop="isRecommended" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isRecommended" type="success" size="small">推荐</el-tag>
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
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['portal:book:edit']"
          >修改</el-button>
          <el-button
            link
            type="primary"
            icon="Delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['portal:book:remove']"
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

    <!-- 添加或修改书籍对话框 -->
    <el-dialog :title="title" v-model="open" width="780px" append-to-body>
      <el-form ref="bookRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="书名" prop="title">
              <el-input v-model="form.title" placeholder="请输入书名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者" prop="author">
              <el-input v-model="form.author" placeholder="请输入作者" />
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
            <el-form-item label="简介(短)" prop="summary">
              <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入简介（短）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="描述(长)" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入详细描述" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="请输入ISBN" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版社" prop="publisher">
              <el-input v-model="form.publisher" placeholder="请输入出版社" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出版日期" prop="publishDate">
              <el-date-picker
                v-model="form.publishDate"
                type="date"
                placeholder="请选择出版日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="页数" prop="pageCount">
              <el-input-number v-model="form.pageCount" :min="0" style="width: 100%" />
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
            <el-form-item label="标签" prop="tags">
              <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="评分" prop="rating">
              <el-input-number v-model="form.rating" :min="0" :max="5" :step="0.1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阅读数" prop="readingCount">
              <el-input-number v-model="form.readingCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="访问级别" prop="accessLevel">
              <el-select v-model="form.accessLevel" placeholder="请选择访问级别" style="width: 100%">
                <el-option label="免费公开" value="free" />
                <el-option label="VIP专享" value="vip" />
                <el-option label="试读" value="preview" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试读比例" prop="previewRatio">
              <el-input-number v-model="form.previewRatio" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :step="0.01" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio label="active">正常</el-radio>
                <el-radio label="inactive">停用</el-radio>
              </el-radio-group>
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
            <el-form-item label="是否推荐" prop="isRecommended">
              <el-switch v-model="form.isRecommended" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="作者简介" prop="authorBio">
              <el-input v-model="form.authorBio" type="textarea" :rows="2" placeholder="请输入作者简介" />
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

<script setup name="Book">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from "vue";
import { listBook, addBook, updateBook, delBook, delBookBatch, getBook } from "@/api/portal/book";

const { proxy } = getCurrentInstance();

// 搜索参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: null,
  author: null,
  categoryId: null,
  status: null,
  isFeatured: null
});

// 表格相关
const showSearch = ref(true);
const loading = ref(false);
const bookList = ref([]);
const total = ref(0);
const selectedRows = ref([]);
const single = ref(true);
const multiple = ref(true);

// 弹窗相关
const open = ref(false);
const title = ref("");
const bookRef = ref();
const form = ref({
  id: null,
  title: null,
  author: null,
  cover: null,
  summary: null,
  description: null,
  isbn: null,
  publisher: null,
  publishDate: null,
  pageCount: null,
  categoryId: null,
  tags: null,
  rating: null,
  readingCount: 0,
  accessLevel: "free",
  previewRatio: 30,
  price: 0,
  status: "active",
  isFeatured: false,
  isRecommended: false,
  authorBio: null
});

// 校验规则
const rules = {
  title: [{ required: true, message: "书名不能为空", trigger: "blur" }],
  author: [{ required: true, message: "作者不能为空", trigger: "blur" }]
};

// 列配置
const columns = [
  { key: 0, label: "编号", visible: true, prop: "id" },
  { key: 1, label: "书名", visible: true, prop: "title" },
  { key: 2, label: "作者", visible: true, prop: "author" },
  { key: 3, label: "封面", visible: true, prop: "cover" },
  { key: 4, label: "分类", visible: true, prop: "categoryId" },
  { key: 5, label: "标签", visible: true, prop: "tags" },
  { key: 6, label: "评分", visible: true, prop: "rating" },
  { key: 7, label: "阅读数", visible: true, prop: "readingCount" },
  { key: 8, label: "访问级别", visible: true, prop: "accessLevel" },
  { key: 9, label: "是否精选", visible: true, prop: "isFeatured" },
  { key: 10, label: "是否推荐", visible: true, prop: "isRecommended" },
  { key: 11, label: "状态", visible: true, prop: "status" },
  { key: 12, label: "创建时间", visible: true, prop: "createTime" }
];

// 查询列表
function getList() {
  loading.value = true;
  listBook(queryParams).then((response) => {
    const rows = response.rows || response.data?.rows || (response.data ? (Array.isArray(response.data) ? response.data : response.data.records) : []);
    const totalCount = response.total || response.data?.total || 0;
    bookList.value = rows;
    total.value = totalCount;
    loading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("查询失败: " + e.message);
    loading.value = false;
  });
}

// 取消
function cancel() {
  open.value = false;
  resetForm();
}

// 重置表单
function resetForm() {
  form.value = {
    id: null,
    title: null,
    author: null,
    cover: null,
    summary: null,
    description: null,
    isbn: null,
    publisher: null,
    publishDate: null,
    pageCount: null,
    categoryId: null,
    tags: null,
    rating: null,
    readingCount: 0,
    accessLevel: "free",
    previewRatio: 30,
    price: 0,
    status: "active",
    isFeatured: false,
    isRecommended: false,
    authorBio: null
  };
  if (bookRef.value) bookRef.value.resetFields();
}

// 搜索重置
function resetQuery() {
  queryParams.pageNum = 1;
  queryParams.title = null;
  queryParams.author = null;
  queryParams.categoryId = null;
  queryParams.status = null;
  queryParams.isFeatured = null;
  handleQuery();
}

// 搜索
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

// 多选变化
function handleSelectionChange(selection) {
  selectedRows.value = selection;
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

// 新增
function handleAdd() {
  resetForm();
  title.value = "新增书籍";
  open.value = true;
}

// 修改
function handleUpdate(row) {
  resetForm();
  const id = row.id || selectedRows.value[0].id;
  getBook(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    title.value = "修改书籍";
    open.value = true;
  }).catch((e) => {
    proxy.$modal.msgError("查询详情失败: " + e.message);
  });
}

// 提交
function submitForm() {
  bookRef.value.validate((valid) => {
    if (valid) {
      const action = form.value.id ? updateBook(form.value) : addBook(form.value);
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

// 删除
function handleDelete(row) {
  const ids = row.id ? row.id : selectedRows.value.map((r) => r.id).join(",");
  proxy.$modal.confirm('是否确认删除书籍编号为"' + ids + '"的数据项？').then(() => {
    const action = row.id ? delBook(ids) : delBookBatch(ids);
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

// 访问级别显示映射
function getAccessLevelType(level) {
  if (level === "free") return "success";
  if (level === "vip") return "warning";
  if (level === "preview") return "info";
  return "";
}

function getAccessLevelText(level) {
  if (level === "free") return "免费";
  if (level === "vip") return "VIP";
  if (level === "preview") return "试读";
  return level || "-";
}

onMounted(() => {
  getList();
});
</script>
