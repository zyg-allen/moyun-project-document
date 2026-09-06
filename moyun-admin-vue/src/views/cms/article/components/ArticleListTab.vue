<template>
  <div class="app-container article-list-tab">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="文章标题">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入文章标题"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户名称">
        <el-input
          v-model="queryParams.authorName"
          placeholder="作者昵称/用户名"
          clearable
          style="width: 180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文章状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 140px">
          <el-option label="全部" value="" />
          <el-option v-for="d in cms_article_status" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-tree-select
          v-model="queryParams.categoryId"
          :data="categoryOptions"
          :props="{ value: 'id', label: 'name', children: 'children' }"
          value-key="id"
          placeholder="请选择分类"
          clearable
          filterable
          check-strictly
          style="width: 180px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <el-icon><Refresh /></el-icon> 重置
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮 -->
    <div class="button-group">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增
      </el-button>
      <el-button type="danger" :disabled="multiple" @click="handleDelete">
        <el-icon><Delete /></el-icon> 删除
      </el-button>
      <el-button type="success" :disabled="multiple" @click="openBatchColumnDialog">
        <el-icon><Collection /></el-icon> 加入专栏
      </el-button>
    </div>

    <!-- 文章列表 -->
    <el-table
      v-loading="loading"
      :data="articleList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="封面" width="100">
        <template #default="{ row }">
          <!-- ImagePreview：自动拼 VITE_APP_BASE_API 前缀 + preview-teleported 挂载 body，避免预览层撑破表格布局 -->
          <image-preview v-if="row.cover" :src="row.cover" :width="60" :height="45" />
          <span v-else class="no-cover">无</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="作者" width="140">
        <template #default="{ row }">
          <div class="author-cell">
            <span>{{ row.authorNickname || row.authorUsername || '-' }}</span>
            <div v-if="row.authorUsername" class="author-sub">用户名: {{ row.authorUsername }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.categoryName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" min-width="160">
        <template #default="{ row }">
          <template v-if="row.tagNames">
            <el-tag
              v-for="(t, idx) in row.tagNames.split(',').filter(Boolean).slice(0, 3)"
              :key="idx"
              size="small"
              type="info"
              class="tag-chip"
            >{{ t }}</el-tag>
            <el-tooltip
              v-if="row.tagNames.split(',').filter(Boolean).length > 3"
              :content="row.tagNames"
              placement="top"
            >
              <el-tag size="small" type="info" class="tag-chip">+{{ row.tagNames.split(',').filter(Boolean).length - 3 }}</el-tag>
            </el-tooltip>
          </template>
          <span v-else class="no-cover">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <dict-tag :options="cms_article_status" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="浏览" width="70" prop="views" />
      <el-table-column label="点赞" width="70" prop="likes" />
      <el-table-column label="评论" width="70" prop="comments" />
      <el-table-column label="精选" width="70">
        <template #default="{ row }">
          <el-switch
            v-model="row.isFeatured"
            :active-value="true"
            :inactive-value="false"
            @change="handleFeaturedChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="70">
        <template #default="{ row }">
          <el-switch
            v-model="row.isTop"
            :active-value="true"
            :inactive-value="false"
            @change="handleTopChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="160" prop="createTime" />
      <!-- 操作栏：收窄、紧凑布局，仅在 pending 时显示审核按钮 -->
      <el-table-column label="操作" width="180" fixed="right" align="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 'pending'" link type="warning" @click="handleAuditPage(row)">审核</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 批量加入专栏弹窗 -->
    <el-dialog
      v-model="batchColumnDialog"
      title="批量加入专栏"
      width="820px"
      append-to-body
      :close-on-click-modal="false"
    >
      <el-row :gutter="16">
        <!-- 已选文章列表 -->
        <el-col :span="12">
          <div class="dialog-section-title">已选文章（{{ selectedRows.length }} 篇）</div>
          <el-table :data="selectedRows" height="380" border>
            <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
            <el-table-column label="作者" width="140">
              <template #default="{ row }">
                <span>{{ row.authorNickname || row.authorUsername || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
        <!-- 专栏选择 -->
        <el-col :span="12">
          <div class="dialog-section-title">选择专栏</div>
          <el-form :inline="true" class="column-filter-form">
            <el-form-item label="作者">
              <el-input
                v-model="columnQuery.authorName"
                placeholder="按作者昵称筛选"
                clearable
                style="width: 140px"
                @keyup.enter="loadColumnOptions(true)"
              />
            </el-form-item>
            <el-form-item label="专栏名">
              <el-input
                v-model="columnQuery.keyword"
                placeholder="专栏名/副标题"
                clearable
                style="width: 140px"
                @keyup.enter="loadColumnOptions(true)"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="loadColumnOptions(true)">搜索</el-button>
            </el-form-item>
          </el-form>
          <el-table
            :data="columnOptions"
            height="320"
            border
            v-loading="columnLoading"
            highlight-current-row
            @current-change="handleColumnCurrentChange"
          >
            <el-table-column label="" width="40" align="center">
              <template #default="{ row }">
                <el-radio v-model="selectedColumnId" :label="row.id">{{ '' }}</el-radio>
              </template>
            </el-table-column>
            <el-table-column label="专栏名" prop="title" min-width="160" show-overflow-tooltip />
            <el-table-column label="作者" width="120">
              <template #default="{ row }">
                <span>{{ row.authorName || row.authorUsername || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="文章数" width="70" prop="articleCount" />
          </el-table>
          <pagination
            v-show="columnTotal > 0"
            :total="columnTotal"
            v-model:page="columnQuery.pageNum"
            v-model:limit="columnQuery.pageSize"
            @pagination="loadColumnOptions(false)"
            :small="true"
          />
        </el-col>
      </el-row>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="batchColumnDialog = false">取 消</el-button>
          <el-button type="primary" :loading="batchSubmitLoading" @click="submitBatchBind">保存</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, getCurrentInstance } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Refresh, Plus, Delete, Collection } from '@element-plus/icons-vue';
import { listArticle, delArticle, delArticleBatch, setFeatured, setTop } from '@/api/cms/article';
import { listCategory } from '@/api/cms/category';
import { listColumn, bindColumnArticles } from '@/api/cms/column';

const { proxy } = getCurrentInstance() as any;
const { cms_article_status } = proxy.useDict('cms_article_status');

const router = useRouter();

// 列表数据
const loading = ref(true);
const articleList = ref<any[]>([]);
const total = ref(0);
const categoryOptions = ref<any[]>([]);

// 查询参数
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  authorName: '',   // 新增：用户名称搜索
  status: '',
  categoryId: '' as string | number | null
});

// 选中行
const selectedRows = ref<any[]>([]);
const ids = computed(() => selectedRows.value.map((item: any) => item.id));
const multiple = computed(() => selectedRows.value.length === 0);

// ========== 批量加入专栏弹窗 ==========
const batchColumnDialog = ref(false);
const columnLoading = ref(false);
const columnOptions = ref<any[]>([]);
const columnTotal = ref(0);
const selectedColumnId = ref<number | null>(null);
const batchSubmitLoading = ref(false);
const columnQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  authorName: '',
  status: 'published'  // 仅展示已审核通过的专栏供选择
});

async function loadCategories() {
  try {
    const res = await listCategory({ pageNum: 1, pageSize: 100 });
    const listData = (res.data && Array.isArray(res.data)) ? res.data
                   : (res.rows && Array.isArray(res.rows)) ? res.rows
                   : [];
    categoryOptions.value = proxy.handleTree(listData, "id");
  } catch (error) {
    console.error('加载分类失败:', error);
  }
}

async function getList() {
  loading.value = true;
  try {
    const res = await listArticle(queryParams);
    articleList.value = res.data.records || [];
    total.value = res.data.total || 0;
  } catch (error) {
    console.error('加载文章列表失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

function resetQuery() {
  queryParams.title = '';
  queryParams.authorName = '';
  queryParams.status = '';
  queryParams.categoryId = '';
  queryParams.pageNum = 1;
  getList();
}

function handleAdd() {
  router.push('/cms/article/edit');
}

function handleView(row: any) {
  router.push({ path: '/cms/article/edit', query: { id: row.id, mode: 'view' } });
}

function handleEdit(row: any) {
  router.push({ path: '/cms/article/edit', query: { id: row.id } });
}

async function handleDelete(row: any) {
  const articleIds = row.id ? [row.id] : ids.value;
  try {
    await ElMessageBox.confirm(
      `是否确认删除文章ID为"${articleIds.join(',')}"的数据项？`,
      '警告',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    );
    if (articleIds.length === 1) {
      await delArticle(articleIds[0]);
    } else {
      await delArticleBatch(articleIds);
    }
    ElMessage.success('删除成功');
    getList();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败');
    }
  }
}

async function handleFeaturedChange(row: any) {
  try {
    await setFeatured({ id: row.id, isFeatured: row.isFeatured });
    ElMessage.success(row.isFeatured ? '设置精选成功' : '取消精选成功');
  } catch (error: any) {
    row.isFeatured = !row.isFeatured;
    ElMessage.error(error.message || '操作失败');
  }
}

async function handleTopChange(row: any) {
  try {
    await setTop({ id: row.id, isTop: row.isTop });
    ElMessage.success(row.isTop ? '设置置顶成功' : '取消置顶成功');
  } catch (error: any) {
    row.isTop = !row.isTop;
    ElMessage.error(error.message || '操作失败');
  }
}

function handleSelectionChange(selection: any[]) {
  selectedRows.value = selection;
}

function handleAuditPage(row: any) {
  router.push({
    path: '/portal/audit-center',
    query: { tab: 'article', bizId: row.id }
  });
}

// 打开批量加入专栏弹窗
async function openBatchColumnDialog() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择文章');
    return;
  }
  selectedColumnId.value = null;
  columnQuery.pageNum = 1;
  columnQuery.keyword = '';
  columnQuery.authorName = '';
  batchColumnDialog.value = true;
  await loadColumnOptions(false);
}

// 加载专栏列表（按作者筛选/专栏名搜索）
async function loadColumnOptions(resetPage: boolean) {
  if (resetPage) columnQuery.pageNum = 1;
  columnLoading.value = true;
  try {
    const res = await listColumn(columnQuery);
    columnOptions.value = res.data.records || [];
    columnTotal.value = res.data.total || 0;
  } catch (e: any) {
    ElMessage.error('加载专栏列表失败: ' + (e.message || ''));
  } finally {
    columnLoading.value = false;
  }
}

function handleColumnCurrentChange(row: any) {
  if (row) selectedColumnId.value = row.id;
}

// 提交批量绑定
async function submitBatchBind() {
  if (!selectedColumnId.value) {
    ElMessage.warning('请选择一个专栏');
    return;
  }
  if (ids.value.length === 0) {
    ElMessage.warning('请先选择文章');
    return;
  }
  batchSubmitLoading.value = true;
  try {
    const res = await bindColumnArticles(selectedColumnId.value, ids.value);
    if (res.code === 200) {
      ElMessage.success(`成功加入专栏（已自动跳过重复绑定的文章）`);
      batchColumnDialog.value = false;
    } else {
      ElMessage.error(res.msg || '保存失败');
    }
  } catch (e: any) {
    ElMessage.error('保存失败: ' + (e.message || ''));
  } finally {
    batchSubmitLoading.value = false;
  }
}

onMounted(() => {
  loadCategories();
  getList();
});
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 16px;
}

.button-group {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.no-cover {
  color: #909399;
  font-size: 12px;
}

.author-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.author-sub {
  font-size: 12px;
  color: #909399;
}

.tag-chip {
  margin: 0 4px 4px 0;
}

.dialog-section-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  color: #303133;
}

.column-filter-form {
  margin-bottom: 8px;
}
</style>
