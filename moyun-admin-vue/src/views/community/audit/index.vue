<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
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
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="pendingList">
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
      <el-table-column label="字数" align="center" prop="wordCount" width="80" />
      <el-table-column label="提交时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="250" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
          >查看</el-button>
          <el-button
            link
            type="success"
            icon="Check"
            @click="handleApprove(scope.row)"
          >通过</el-button>
          <el-button
            link
            type="danger"
            icon="Close"
            @click="handleReject(scope.row)"
          >驳回</el-button>
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

    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="500px" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确定驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Audit">
import { listPendingArticle, approveArticle, rejectArticle } from '@/api/community/article'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const showSearch = ref(true)
const pendingList = ref([])
const total = ref(0)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentArticleId = ref(null)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  articleType: null
})

function getList() {
  loading.value = true
  listPendingArticle(queryParams.value).then(response => {
    pendingList.value = response.data || []
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
    articleType: null
  }
  handleQuery()
}

function handleDetail(row) {
  // navigate to detail
}

function handleApprove(row) {
  approveArticle(row.articleId, '').then(() => {
    ElMessage.success('审核通过')
    getList()
  })
}

function handleReject(row) {
  currentArticleId.value = row.articleId
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

function confirmReject() {
  rejectArticle(currentArticleId.value, rejectReason.value).then(() => {
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    getList()
  })
}

getList()
</script>
