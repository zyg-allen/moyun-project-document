<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
      <el-form-item label="文章状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="草稿" value="draft" />
          <el-option label="待审核" value="pending" />
          <el-option label="已发布" value="published" />
          <el-option label="已驳回" value="rejected" />
          <el-option label="已下架" value="offline" />
        </el-select>
      </el-form-item>
      <el-form-item label="文章类型" prop="articleType">
        <el-select v-model="queryParams.articleType" placeholder="请选择类型" clearable>
          <el-option label="技术" value="technical" />
          <el-option label="文学" value="literary" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="Refresh"
          @click="handleRefreshCache"
        >刷新缓存</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="articleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="文章ID" align="center" prop="articleId" width="80" />
      <el-table-column label="标题" align="center" prop="title" :show-overflow-tooltip="true" />
      <el-table-column label="作者" align="center" prop="authorNickname" width="100" />
      <el-table-column label="分类" align="center" prop="categoryName" width="100" />
      <el-table-column label="类型" align="center" prop="articleType" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.articleType === 'technical'" type="success">技术</el-tag>
          <el-tag v-else type="warning">文学</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'draft'" type="info">草稿</el-tag>
          <el-tag v-else-if="scope.row.status === 'pending'" type="warning">待审核</el-tag>
          <el-tag v-else-if="scope.row.status === 'published'" type="success">已发布</el-tag>
          <el-tag v-else-if="scope.row.status === 'rejected'" type="danger">已驳回</el-tag>
          <el-tag v-else type="info">已下架</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="阅读数" align="center" prop="viewCount" width="80" />
      <el-table-column label="点赞数" align="center" prop="likeCount" width="80" />
      <el-table-column label="评论数" align="center" prop="commentCount" width="80" />
      <el-table-column label="发布时间" align="center" prop="publishTime" width="160" />
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
          >详情</el-button>
          <el-button
            link
            type="danger"
            icon="Delete"
            @click="handleDelete(scope.row)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script setup name="Article">
import { listArticle, deleteArticle } from '@/api/community/article'

const loading = ref(true)
const showSearch = ref(true)
const articleList = ref([])
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  status: null,
  articleType: null
})

function getList() {
  loading.value = true
  listArticle(queryParams.value).then(response => {
    articleList.value = response.data || []
    total.value = response.total || 0
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.value = {
    pageNum: 1,
    pageSize: 10,
    status: null,
    articleType: null
  }
  handleQuery()
}

function handleSelectionChange(selection) {
  // handle selection
}

function handleDetail(row) {
  // navigate to detail
}

function handleDelete(row) {
  // handle delete
}

function handleRefreshCache() {
  // refresh cache
}

getList()
</script>
