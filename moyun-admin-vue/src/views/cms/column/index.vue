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
      <el-form-item label="用户名称" prop="authorName">
        <el-input
          v-model="queryParams.authorName"
          placeholder="作者昵称/用户名"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 160px">
          <el-option v-for="d in cms_column_status" :key="d.value" :label="d.label" :value="d.value" />
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
          <dict-tag :options="cms_column_status" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createdTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createdTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="right" class-name="small-padding fixed-width" width="260" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['portal:column:edit']"
          >修改</el-button>
          <!-- 审核按钮：仅 pending 状态显示（与文章一致；审核通过后按钮隐藏） -->
          <el-button
            v-if="scope.row.status === 'pending'"
            link
            type="warning"
            icon="View"
            @click="handleAuditPage(scope.row)"
            v-hasPermi="['system:auditTask:list']"
          >审核</el-button>
          <!-- 维护文章：管理专栏下绑定的文章（新增绑定/移除/分页查询） -->
          <el-button
            link
            type="success"
            icon="Collection"
            @click="handleManageArticles(scope.row)"
            v-hasPermi="['portal:column:edit']"
          >维护文章</el-button>
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
    <el-dialog :title="title" v-model="open" width="900px" append-to-body>
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
            <!-- 创建者用户名：只读展示（来自列表/详情，无则回退到 authorUsername） -->
            <el-form-item label="创建者">
              <el-input
                :model-value="form.authorName || form.authorUsername || '-'"
                disabled
                placeholder="提交后由系统填充"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- 分类ID：按需求隐藏（专栏自身即为分类节点，无需在表单中暴露 categoryId） -->
        <!-- <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类ID" prop="categoryId">
              <el-input-number v-model="form.categoryId" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row> -->
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
                <el-radio v-for="d in cms_column_status" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
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
    <el-dialog title="专栏状态变更" v-model="statusOpen" width="520px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="专栏名">
          <span>{{ currentRow.title }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <dict-tag :options="cms_column_status" :value="currentRow.status" />
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="statusForm.status" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="d in cms_column_status" :key="d.value" :label="d.label" :value="d.value" />
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

    <!-- 维护文章对话框 -->
    <el-dialog
      v-model="manageArticlesOpen"
      :title="'维护文章 - ' + (manageRow.title || '')"
      width="980px"
      append-to-body
      :close-on-click-modal="false"
    >
      <!-- 专栏基本信息 -->
      <el-descriptions :column="3" border size="small" class="manage-desc">
        <el-descriptions-item label="专栏ID">{{ manageRow.id }}</el-descriptions-item>
        <el-descriptions-item label="专栏名">{{ manageRow.title }}</el-descriptions-item>
        <el-descriptions-item label="作者">
          <span>{{ manageRow.authorName || manageRow.authorUsername || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="cms_column_status" :value="manageRow.status" />
        </el-descriptions-item>
        <el-descriptions-item label="文章数">{{ manageRow.articleCount }}</el-descriptions-item>
        <el-descriptions-item label="订阅数">{{ manageRow.subscribeCount }}</el-descriptions-item>
      </el-descriptions>

      <!-- 已绑定文章列表 -->
      <div class="manage-toolbar">
        <div class="manage-toolbar-left">
          <el-input
            v-model="manageQuery.keyword"
            placeholder="按文章标题搜索"
            clearable
            style="width: 220px"
            @keyup.enter="loadColumnArticles(true)"
          />
          <el-button type="primary" size="small" @click="loadColumnArticles(true)">搜索</el-button>
        </div>
        <el-button type="success" size="small" @click="openAddArticleDialog">新增文章绑定</el-button>
      </div>
      <el-table
        v-loading="manageLoading"
        :data="manageArticles"
        border
        height="380"
      >
        <el-table-column label="ID" prop="id" width="70" />
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="作者" width="140">
          <template #default="{ row }">
            <span>{{ row.authorName || row.authorUsername || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="浏览" width="70" prop="viewCount" />
        <el-table-column label="点赞" width="70" prop="likeCount" />
        <el-table-column label="顺序" width="70" prop="sortOrder" />
        <el-table-column label="加入时间" width="160" prop="createdTime" />
        <el-table-column label="操作" width="100" align="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleRemoveArticle(row)">移出</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="manageTotal > 0"
        :total="manageTotal"
        v-model:page="manageQuery.pageNum"
        v-model:limit="manageQuery.pageSize"
        @pagination="loadColumnArticles(false)"
        :small="true"
      />

      <!-- 新增文章绑定（嵌套对话框：append-to-body 防止层级问题） -->
      <el-dialog
        v-model="addArticleOpen"
        title="选择文章加入专栏"
        width="900px"
        append-to-body
        :close-on-click-modal="false"
      >
        <el-form :inline="true" class="add-article-filter">
          <el-form-item label="文章标题">
            <el-input
              v-model="addArticleQuery.title"
              placeholder="文章标题"
              clearable
              style="width: 180px"
              @keyup.enter="loadArticlesForAdd(true)"
            />
          </el-form-item>
          <el-form-item label="作者">
            <el-input
              v-model="addArticleQuery.authorName"
              placeholder="作者昵称/用户名"
              clearable
              style="width: 160px"
              @keyup.enter="loadArticlesForAdd(true)"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" @click="loadArticlesForAdd(true)">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table
          v-loading="addArticleLoading"
          :data="addArticleList"
          border
          height="380"
          @selection-change="handleAddSelectionChange"
        >
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
          <el-table-column label="作者" width="140">
            <template #default="{ row }">
              <span>{{ row.authorNickname || row.authorUsername || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <dict-tag :options="cms_article_status" :value="row.status" />
            </template>
          </el-table-column>
        </el-table>
        <pagination
          v-show="addArticleTotal > 0"
          :total="addArticleTotal"
          v-model:page="addArticleQuery.pageNum"
          v-model:limit="addArticleQuery.pageSize"
          @pagination="loadArticlesForAdd(false)"
          :small="true"
        />
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="addArticleOpen = false">取 消</el-button>
            <el-button type="primary" :loading="addArticleSubmitting" @click="submitAddArticles">加入专栏</el-button>
          </div>
        </template>
      </el-dialog>
    </el-dialog>
  </div>
</template>

<script setup name="CmsColumn">
import { getCurrentInstance, ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { listColumn, getColumn, addColumn, updateColumn, delColumn, changeColumnStatus, listColumnArticles, bindColumnArticles, removeColumnArticle } from "@/api/cms/column";
import { listArticle } from "@/api/cms/article";

const { proxy } = getCurrentInstance();
const { cms_column_status, cms_article_status } = proxy.useDict("cms_column_status", "cms_article_status");
const router = useRouter();

/** 跳转到专栏审核页（与文章审核入口一致） */
function handleAuditPage(row) {
  router.push({
    path: "/portal/audit-center",
    query: { tab: "column", bizId: row.id }
  });
}

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: null,
  authorName: null,
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

// ========== 维护文章弹窗相关 ==========
const manageArticlesOpen = ref(false);
const manageRow = ref({});
const manageLoading = ref(false);
const manageArticles = ref([]);
const manageTotal = ref(0);
const manageQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '' });

// 新增文章绑定（嵌套弹窗）
const addArticleOpen = ref(false);
const addArticleLoading = ref(false);
const addArticleList = ref([]);
const addArticleTotal = ref(0);
const addArticleQuery = reactive({ pageNum: 1, pageSize: 10, title: '', authorName: '', status: 'published' });
const addArticleSelected = ref([]);
const addArticleSubmitting = ref(false);

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
  queryParams.authorName = null;
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
  // 记录列表中的作者信息（详情接口可能未返回 authorName/authorUsername）
  const rowAuthorName = row.authorName || row.authorUsername;
  getColumn(id).then((response) => {
    const data = response.data || response;
    Object.assign(form.value, data);
    // 详情未返回时回退到列表中的作者信息
    if (!form.value.authorName && !form.value.authorUsername && rowAuthorName) {
      form.value.authorName = rowAuthorName;
    }
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

// ========== 维护文章 ==========
function handleManageArticles(row) {
  manageRow.value = row;
  manageQuery.pageNum = 1;
  manageQuery.keyword = '';
  manageArticlesOpen.value = true;
  loadColumnArticles(false);
}

function loadColumnArticles(resetPage) {
  if (resetPage) manageQuery.pageNum = 1;
  if (!manageRow.value.id) return;
  manageLoading.value = true;
  listColumnArticles(manageRow.value.id, manageQuery).then((response) => {
    manageArticles.value = response.data.records || [];
    manageTotal.value = response.data.total || 0;
    manageLoading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("加载专栏文章失败: " + e.message);
    manageLoading.value = false;
  });
}

function handleRemoveArticle(row) {
  ElMessageBox.confirm(`确认将文章「${row.title}」移出该专栏？`, "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(() => {
    removeColumnArticle(manageRow.value.id, row.id).then((response) => {
      if (response.code === 200) {
        proxy.$modal.msgSuccess("已移出");
        // 同步专栏文章数
        manageRow.value.articleCount = Math.max(0, (manageRow.value.articleCount || 0) - 1);
        loadColumnArticles(false);
      } else {
        proxy.$modal.msgError(response.msg || "移出失败");
      }
    }).catch((e) => proxy.$modal.msgError("移出失败: " + e.message));
  }).catch(() => {});
}

// 嵌套：新增文章绑定
function openAddArticleDialog() {
  addArticleQuery.pageNum = 1;
  addArticleQuery.title = '';
  addArticleQuery.authorName = '';
  addArticleSelected.value = [];
  addArticleOpen.value = true;
  loadArticlesForAdd(false);
}

function loadArticlesForAdd(resetPage) {
  if (resetPage) addArticleQuery.pageNum = 1;
  addArticleLoading.value = true;
  listArticle(addArticleQuery).then((response) => {
    addArticleList.value = response.data.records || [];
    addArticleTotal.value = response.data.total || 0;
    addArticleLoading.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("加载文章列表失败: " + e.message);
    addArticleLoading.value = false;
  });
}

function handleAddSelectionChange(selection) {
  addArticleSelected.value = selection;
}

function submitAddArticles() {
  if (addArticleSelected.value.length === 0) {
    ElMessage.warning("请先选择文章");
    return;
  }
  const articleIds = addArticleSelected.value.map((r) => r.id);
  addArticleSubmitting.value = true;
  bindColumnArticles(manageRow.value.id, articleIds).then((response) => {
    if (response.code === 200) {
      proxy.$modal.msgSuccess("已绑定（自动跳过重复）");
      addArticleOpen.value = false;
      // 刷新已绑定列表 + 同步文章数
      loadColumnArticles(false);
      // 拉取最新专栏信息（articleCount 由后端同步更新）
      getColumn(manageRow.value.id).then((resp) => {
        const d = resp.data || resp;
        Object.assign(manageRow.value, d);
      }).catch(() => {});
    } else {
      proxy.$modal.msgError(response.msg || "绑定失败");
    }
    addArticleSubmitting.value = false;
  }).catch((e) => {
    proxy.$modal.msgError("绑定失败: " + e.message);
    addArticleSubmitting.value = false;
  });
}

onMounted(() => {
  getList();
});
</script>

<style scoped>
.manage-desc {
  margin-bottom: 16px;
}

.manage-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.manage-toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.add-article-filter {
  margin-bottom: 8px;
}
</style>
