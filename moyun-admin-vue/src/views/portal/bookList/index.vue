<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="书单名称" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入书单名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable filterable style="width: 180px" @change="handleQuery">
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
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
      <el-table-column label="分类" align="center" prop="categoryId" width="100">
        <template #default="scope">
          <span>{{ getCategoryName(scope.row.categoryId) }}</span>
        </template>
      </el-table-column>
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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['portal:bookList:edit']">修改</el-button>
          <el-button link type="success" icon="Reading" @click="handleManageBooks(scope.row)" v-hasPermi="['portal:bookList:query']">管理书籍</el-button>
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
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" clearable filterable style="width: 100%">
                <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="创建者ID" prop="userId">
              <el-input-number v-model="form.userId" :min="1" :disabled="true" style="width: 100%" />
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
                <el-option label="试读（前30%免费）" value="preview" />
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
              <el-input-number v-model="form.bookCount" :min="0" :disabled="true" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="浏览数" prop="viewCount">
              <el-input-number v-model="form.viewCount" :min="0" :disabled="true" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="点赞数" prop="likeCount">
              <el-input-number v-model="form.likeCount" :min="0" :disabled="true" style="width: 100%" />
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

    <!-- 管理书籍对话框 -->
    <el-dialog :title="booksDialogTitle" v-model="booksDialogOpen" width="1000px" append-to-body @open="onBooksDialogOpen">
      <div style="margin-bottom: 12px; color: #909399; font-size: 13px">
        当前书单共 <b style="color: #409eff">{{ currentBooks.length }}</b> 本书籍。左侧为可添加书籍（按条件筛选），右侧为书单内已有书籍。
      </div>
      <el-row :gutter="16">
        <!-- 左侧：可添加书籍 -->
        <el-col :span="14">
          <div style="font-weight: 600; margin-bottom: 8px">可添加书籍</div>
          <el-form :inline="true" :model="availableQuery" size="small" style="margin-bottom: 8px">
            <el-form-item label="书名" style="margin-bottom: 0">
              <el-input v-model="availableQuery.title" placeholder="书名" clearable style="width: 130px" @keyup.enter="loadAvailableBooks" />
            </el-form-item>
            <el-form-item label="作者" style="margin-bottom: 0">
              <el-input v-model="availableQuery.author" placeholder="作者" clearable style="width: 120px" @keyup.enter="loadAvailableBooks" />
            </el-form-item>
            <el-form-item label="分类" style="margin-bottom: 0">
              <el-select v-model="availableQuery.categoryId" placeholder="分类" clearable filterable style="width: 130px">
                <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item style="margin-bottom: 0">
              <el-button type="primary" icon="Search" @click="loadAvailableBooks">查询</el-button>
              <el-button icon="Refresh" @click="resetAvailableQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table
            v-loading="availableLoading"
            :data="availableBooks"
            height="380"
            size="small"
            @selection-change="handleAvailableSelectionChange"
          >
            <el-table-column type="selection" width="40" align="center" />
            <el-table-column label="书名" prop="title" min-width="140" :show-overflow-tooltip="true" />
            <el-table-column label="作者" prop="author" width="90" :show-overflow-tooltip="true" />
            <el-table-column label="分类" width="90" align="center">
              <template #default="scope">
                <span>{{ getCategoryName(scope.row.categoryId) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 8px; text-align: right">
            <el-button
              type="primary"
              icon="Plus"
              :disabled="availableSelected.length === 0"
              @click="handleAddBooks"
            >添加选中书籍</el-button>
          </div>
        </el-col>
        <!-- 右侧：书单内已有书籍 -->
        <el-col :span="10">
          <div style="font-weight: 600; margin-bottom: 8px">书单内书籍</div>
          <el-table
            v-loading="currentLoading"
            :data="currentBooks"
            height="380"
            size="small"
            @selection-change="handleCurrentSelectionChange"
          >
            <el-table-column type="selection" width="40" align="center" />
            <el-table-column label="书名" prop="title" min-width="100" :show-overflow-tooltip="true" />
            <el-table-column label="作者" prop="author" width="70" :show-overflow-tooltip="true" />
            <el-table-column label="排序" width="80" align="center">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.sort"
                  :min="0"
                  :max="9999"
                  size="small"
                  controls-position="right"
                  style="width: 60px"
                  @change="markSortChanged"
                />
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 8px; text-align: right">
            <el-button
              type="primary"
              icon="Sort"
              :disabled="!sortChanged"
              :loading="sortSaving"
              @click="handleSaveSort"
            >保存排序</el-button>
            <el-button
              type="danger"
              icon="Delete"
              :disabled="currentSelected.length === 0"
              @click="handleRemoveBooks"
            >移除选中书籍</el-button>
          </div>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup name="BookList">
import { ref, reactive, getCurrentInstance, onMounted } from "vue";
import useUserStore from "@/store/modules/user";
import { listBookList, addBookList, updateBookList, delBookList, delBookListBatch, getBookList } from "@/api/portal/bookList";
import { listCategories } from "@/api/portal/category";
import {
  listBookListItems,
  addBooksToBookList,
  removeBooksFromBookList,
  listAvailableBooks,
  updateBookListSort
} from "@/api/portal/bookListRelation";

const { proxy } = getCurrentInstance();
const userStore = useUserStore();

// 分类下拉数据
const categoryOptions = ref([]);
// 分类ID -> 名称映射，用于表格列显示
const categoryMap = ref({});

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

// ====== 管理书籍弹窗状态 =====
const booksDialogOpen = ref(false);
const booksDialogTitle = ref("");
const currentBookListId = ref(null);
// 当前书单内已有书籍
const currentBooks = ref([]);
const currentLoading = ref(false);
const currentSelected = ref([]);
// 可添加书籍（排除已在书单内）
const availableBooks = ref([]);
const availableLoading = ref(false);
const availableSelected = ref([]);
const availableQuery = reactive({ title: null, author: null, categoryId: null });
// 排序状态
const sortChanged = ref(false);
const sortSaving = ref(false);

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
    const rows = response.data.records || [];
    const totalCount = response.data.total || 0;
    list.value = rows;
    total.value = totalCount;
    loading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("查询失败: " + e.message);
    loading.value = false;
  });
}

// 加载分类下拉数据
function loadCategories() {
  listCategories({ status: "0" }).then((response) => {
    const list = response.data || [];
    categoryOptions.value = list;
    const map = {};
    list.forEach((c) => {
      map[c.id] = c.name;
    });
    categoryMap.value = map;
  }).catch(() => {
    categoryOptions.value = [];
    categoryMap.value = {};
  });
}

// 根据分类ID获取分类名称（用于表格展示）
function getCategoryName(categoryId) {
  if (categoryId == null) return "-";
  return categoryMap.value[categoryId] || ("#" + categoryId);
}

// ====== 管理书籍相关 =====
// 打开管理书籍弹窗
function handleManageBooks(row) {
  currentBookListId.value = row.id;
  booksDialogTitle.value = "管理书籍 - " + row.title;
  booksDialogOpen.value = true;
}

// 弹窗 open 时触发加载两侧数据
function onBooksDialogOpen() {
  // 重置筛选条件
  availableQuery.title = null;
  availableQuery.author = null;
  availableQuery.categoryId = null;
  availableSelected.value = [];
  currentSelected.value = [];
  sortChanged.value = false;
  sortSaving.value = false;
  loadCurrentBooks();
  loadAvailableBooks();
}

// 加载书单内已有书籍
function loadCurrentBooks() {
  if (!currentBookListId.value) return;
  currentLoading.value = true;
  listBookListItems(currentBookListId.value).then((response) => {
    currentBooks.value = response.data || [];
    currentLoading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("加载书单书籍失败: " + e.message);
    currentLoading.value = false;
  });
}

// 加载可添加书籍（按书名/作者/分类筛选，排除已在书单内）
function loadAvailableBooks() {
  if (!currentBookListId.value) return;
  availableLoading.value = true;
  listAvailableBooks(currentBookListId.value, availableQuery).then((response) => {
    availableBooks.value = response.data || [];
    availableLoading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("查询可添加书籍失败: " + e.message);
    availableLoading.value = false;
  });
}

// 重置可添加书籍筛选
function resetAvailableQuery() {
  availableQuery.title = null;
  availableQuery.author = null;
  availableQuery.categoryId = null;
  loadAvailableBooks();
}

// 可添加书籍选择变化
function handleAvailableSelectionChange(selection) {
  availableSelected.value = selection;
}

// 书单内书籍选择变化
function handleCurrentSelectionChange(selection) {
  currentSelected.value = selection;
}

// 添加选中书籍到书单
function handleAddBooks() {
  const bookIds = availableSelected.value.map((b) => b.id);
  if (bookIds.length === 0) return;
  proxy.$modal.confirm('是否确认将选中的 ' + bookIds.length + ' 本书籍加入书单？').then(() => {
    addBooksToBookList(currentBookListId.value, bookIds, null).then((response) => {
    if (response.code === 200) {
      proxy.$modal.msgSuccess("成功添加 " + bookIds.length + " 本书籍");
      sortChanged.value = false;
      loadCurrentBooks();
      loadAvailableBooks();
      getList();
    } else {
      proxy.$modal.msgError(response.msg || "添加失败");
    }
  }).catch((e) => {
    proxy.$modal.msgError("添加失败: " + e.message);
  });
  }).catch(() => {});
}

// 标记排序已修改
function markSortChanged() {
  sortChanged.value = true;
}

// 保存排序
function handleSaveSort() {
  if (!sortChanged.value || currentBooks.value.length === 0) return;
  // 构建排序数据（使用 itemId 作为 id，因为 SQL 中 i.id 映射为 itemId）
  const sortItems = currentBooks.value.map((item) => ({
    id: item.itemId,
    sort: item.sort || 0
  }));
  sortSaving.value = true;
  updateBookListSort(currentBookListId.value, sortItems).then((response) => {
    sortSaving.value = false;
    if (response.code === 200) {
      proxy.$modal.msgSuccess("排序保存成功");
      sortChanged.value = false;
      loadCurrentBooks();
      getList();
    } else {
      proxy.$modal.msgError(response.msg || "排序保存失败");
    }
  }).catch((e) => {
    sortSaving.value = false;
    proxy.$modal.msgError("排序保存失败: " + e.message);
  });
}

// 从书单移除选中书籍
function handleRemoveBooks() {
  const bookIds = currentSelected.value.map((b) => b.id);
  if (bookIds.length === 0) return;
  proxy.$modal.confirm('是否确认从书单中移除选中的 ' + bookIds.length + ' 本书籍？').then(() => {
    removeBooksFromBookList(currentBookListId.value, bookIds).then((response) => {
      if (response.code === 200) {
        proxy.$modal.msgSuccess("移除成功");
        loadCurrentBooks();
        loadAvailableBooks();
        getList();
      } else {
        proxy.$modal.msgError(response.msg || "移除失败");
      }
    }).catch((e) => {
      proxy.$modal.msgError("移除失败: " + e.message);
    });
  }).catch(() => {});
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
    userId: userStore.id || 1,
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
  loadCategories();
  getList();
});
</script>
