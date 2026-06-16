<template>
  <div class="app-container">
    <!-- 搜索表单 -->
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="文章标题">
        <el-input
          v-model="queryParams.title"
          placeholder="请输入文章标题"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="文章状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="全部" value="" />
          <el-option label="草稿" value="draft" />
          <el-option label="已发布" value="published" />
          <el-option label="已归档" value="archived" />
        </el-select>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable filterable>
          <el-option label="全部" value="" />
          <el-option
            v-for="cat in categoryOptions"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
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
    </div>

    <!-- 文章列表 -->
    <el-table
      v-loading="loading"
      :data="articleList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="封面" width="120">
        <template #default="{ row }">
          <el-image
            v-if="row.cover"
            :src="row.cover"
            fit="cover"
            class="article-cover"
            :preview-src-list="[row.cover]"
          />
          <span v-else class="no-cover">无封面</span>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
      <el-table-column label="作者" width="150">
        <template #default="{ row }">
          <span>{{ row.authorNickname || row.authorUsername || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="120">
        <template #default="{ row }">
          <el-tag>{{ row.categoryName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="浏览" width="80">
        <template #default="{ row }">
          <span>{{ row.views || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="点赞" width="80">
        <template #default="{ row }">
          <span>{{ row.likes || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="评论" width="80">
        <template #default="{ row }">
          <span>{{ row.comments || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="精选" width="80">
        <template #default="{ row }">
          <el-switch
            v-model="row.isFeatured"
            :active-value="true"
            :inactive-value="false"
            @change="handleFeaturedChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="置顶" width="80">
        <template #default="{ row }">
          <el-switch
            v-model="row.isTop"
            :active-value="true"
            :inactive-value="false"
            @change="handleTopChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180" prop="createTime" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleView(row)">
            查看
          </el-button>
          <el-button link type="primary" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDelete(row)">
            删除
          </el-button>
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

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue';
import { listArticle, delArticle, delArticleBatch, updateArticle, setFeatured, setTop } from '@/api/cms/article';
import { listCategory } from '@/api/cms/category';

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
  status: '',
  categoryId: ''
});

// 选中行
const ids = ref<number[]>([]);
const multiple = computed(() => ids.value.length === 0);

// 状态映射
const statusMap = {
  draft: { label: '草稿', type: 'info' },
  published: { label: '已发布', type: 'success' },
  archived: { label: '已归档', type: 'warning' }
};

function getStatusLabel(status: string) {
  return statusMap[status]?.label || status;
}

function getStatusType(status: string) {
  return statusMap[status]?.type || 'info';
}

// 加载分类选项
async function loadCategories() {
  try {
    const res = await listCategory({ pageNum: 1, pageSize: 100 });
    categoryOptions.value = res.rows || [];
  } catch (error) {
    console.error('加载分类失败:', error);
  }
}

// 加载文章列表
async function getList() {
  loading.value = true;
  try {
    const res = await listArticle(queryParams);
    articleList.value = res.rows || [];
    total.value = res.total || 0;
  } catch (error) {
    console.error('加载文章列表失败:', error);
  } finally {
    loading.value = false;
  }
}

// 搜索
function handleQuery() {
  queryParams.pageNum = 1;
  getList();
}

// 重置
function resetQuery() {
  queryParams.title = '';
  queryParams.status = '';
  queryParams.categoryId = '';
  queryParams.pageNum = 1;
  getList();
}

// 新增
function handleAdd() {
  router.push('/cms/article/edit');
}

// 查看
function handleView(row: any) {
  router.push({
    path: '/cms/article/edit',
    query: { id: row.id, mode: 'view' }
  });
}

// 编辑
function handleEdit(row: any) {
  router.push({
    path: '/cms/article/edit',
    query: { id: row.id }
  });
}

// 删除
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

// 精选切换
async function handleFeaturedChange(row: any) {
  try {
    await setFeatured({ id: row.id, isFeatured: row.isFeatured });
    ElMessage.success(row.isFeatured ? '设置精选成功' : '取消精选成功');
  } catch (error: any) {
    row.isFeatured = !row.isFeatured; // 回滚
    ElMessage.error(error.message || '操作失败');
  }
}

// 置顶切换
async function handleTopChange(row: any) {
  try {
    await setTop({ id: row.id, isTop: row.isTop });
    ElMessage.success(row.isTop ? '设置置顶成功' : '取消置顶成功');
  } catch (error: any) {
    row.isTop = !row.isTop; // 回滚
    ElMessage.error(error.message || '操作失败');
  }
}

// 多选
function handleSelectionChange(selection: any[]) {
  ids.value = selection.map((item: any) => item.id);
}

// 初始化
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
}

.article-cover {
  width: 80px;
  height: 60px;
  border-radius: 4px;
}

.no-cover {
  color: #909399;
  font-size: 12px;
}
</style>
