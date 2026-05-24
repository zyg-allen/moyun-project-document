<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="queryParams.username"
          placeholder="请输入用户名"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          placeholder="请输入昵称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>

    <el-table v-loading="loading" :data="userList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="用户ID" align="center" prop="userId" width="80" />
      <el-table-column label="用户名" align="center" prop="username" width="120" />
      <el-table-column label="昵称" align="center" prop="nickname" width="120" />
      <el-table-column label="头像" align="center" prop="avatar" width="80">
        <template #default="scope">
          <el-image :src="scope.row.avatar" style="width: 50px; height: 50px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="邮箱" align="center" prop="email" width="180" />
      <el-table-column label="会员" align="center" prop="isVip" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isVip" type="warning">VIP</el-tag>
          <el-tag v-else type="info">普通</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.status === '0'" type="success">正常</el-tag>
          <el-tag v-else type="danger">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="200">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="View"
            @click="handleDetail(scope.row)"
          >详情</el-button>
          <el-button
            link
            type="warning"
            @click="handleStatusChange(scope.row)"
          >{{ scope.row.status === '0' ? '停用' : '启用' }}</el-button>
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

    <el-dialog title="用户详情" v-model="detailOpen" width="700px">
      <el-descriptions v-if="currentUser" :column="2" border>
        <el-descriptions-item label="用户ID">{{ currentUser.userId }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ currentUser.nickname }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentUser.email }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentUser.phone }}</el-descriptions-item>
        <el-descriptions-item label="会员">{{ currentUser.isVip ? 'VIP' : '普通' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ currentUser.createTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentUser.status === '0'" type="success">正常</el-tag>
          <el-tag v-else type="danger">停用</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="个人简介" :span="2">{{ currentUser.bio }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">用户统计</el-divider>
      <el-row :gutter="20" v-if="userStats">
        <el-col :span="6">
          <el-statistic title="文章数" :value="userStats.articleCount" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="粉丝数" :value="userStats.followerCount" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="关注数" :value="userStats.followingCount" />
        </el-col>
        <el-col :span="6">
          <el-statistic title="获赞数" :value="userStats.likeCount" />
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup name="PortalUser">
import { listPortalUser, getPortalUser, updatePortalUserStatus, getUserStats } from '@/api/community/user'

const loading = ref(true)
const showSearch = ref(true)
const detailOpen = ref(false)
const userList = ref([])
const total = ref(0)
const currentUser = ref(null)
const userStats = ref(null)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  username: null,
  nickname: null,
  status: null
})

function getList() {
  loading.value = true
  listPortalUser(queryParams.value).then(response => {
    userList.value = response.data || []
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
    username: null,
    nickname: null,
    status: null
  }
  handleQuery()
}

function handleSelectionChange(selection) {
  // handle selection
}

function handleDetail(row) {
  currentUser.value = row
  detailOpen.value = true
  getUserStats(row.userId).then(response => {
    userStats.value = response.data
  })
}

function handleStatusChange(row) {
  const newStatus = row.status === '0' ? '1' : '0'
  updatePortalUserStatus(row.userId, newStatus).then(() => {
    ElMessage.success('操作成功')
    getList()
  })
}

getList()
</script>
