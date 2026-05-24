<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
      <el-form-item label="文章ID" prop="articleId">
        <el-input
          v-model="queryParams.articleId"
          placeholder="请输入文章ID"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户昵称" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          placeholder="请输入用户昵称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>

    <el-table v-loading="loading" :data="commentList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="评论ID" align="center" prop="commentId" width="80" />
      <el-table-column label="文章标题" align="center" prop="articleTitle" :show-overflow-tooltip="true" />
      <el-table-column label="用户昵称" align="center" prop="nickname" width="100" />
      <el-table-column label="评论内容" align="center" prop="content" :show-overflow-tooltip="true" />
      <el-table-column label="点赞数" align="center" prop="likeCount" width="80" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="120">
        <template #default="scope">
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

<script setup name="Comment">
import { listComment, deleteComment } from '@/api/community/comment'

const loading = ref(true)
const showSearch = ref(true)
const commentList = ref([])
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  articleId: null,
  nickname: null
})

function getList() {
  loading.value = true
  listComment(queryParams.value).then(response => {
    commentList.value = response.data || []
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
    articleId: null,
    nickname: null
  }
  handleQuery()
}

function handleSelectionChange(selection) {
  // handle selection
}

function handleDelete(row) {
  // handle delete
}

getList()
</script>
