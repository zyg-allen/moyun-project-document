<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
      <el-form-item label="链接名称" prop="linkName">
        <el-input
          v-model="queryParams.linkName"
          placeholder="请输入链接名称"
          clearable
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="0" />
          <el-option label="禁用" value="1" />
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
          icon="Plus"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="linkList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="链接ID" align="center" prop="linkId" width="80" />
      <el-table-column label="链接名称" align="center" prop="linkName" width="150" />
      <el-table-column label="链接地址" align="center" prop="linkUrl" :show-overflow-tooltip="true" />
      <el-table-column label="链接图标" align="center" prop="linkLogo" width="80">
        <template #default="scope">
          <el-image :src="scope.row.linkLogo" style="width: 50px; height: 50px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.status === '0'" type="success">启用</el-tag>
          <el-tag v-else type="danger">禁用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="180">
        <template #default="scope">
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="handleUpdate(scope.row)"
          >修改</el-button>
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

    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="linkForm" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="链接名称" prop="linkName">
          <el-input v-model="form.linkName" placeholder="请输入链接名称" />
        </el-form-item>
        <el-form-item label="链接地址" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="请输入链接地址" />
        </el-form-item>
        <el-form-item label="链接图标" prop="linkLogo">
          <el-input v-model="form.linkLogo" placeholder="请输入链接图标地址" />
        </el-form-item>
        <el-form-item label="链接描述" prop="linkDesc">
          <el-input v-model="form.linkDesc" type="textarea" placeholder="请输入链接描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
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

<script setup name="FriendLink">
import { listFriendLink, getFriendLink, addFriendLink, updateFriendLink, deleteFriendLink } from '@/api/community/friendlink'

const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const linkList = ref([])
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  linkName: null,
  status: null
})

const form = ref({})
const rules = ref({
  linkName: [{ required: true, message: '链接名称不能为空', trigger: 'blur' }],
  linkUrl: [{ required: true, message: '链接地址不能为空', trigger: 'blur' }]
})
const linkForm = ref()

function getList() {
  loading.value = true
  listFriendLink(queryParams.value).then(response => {
    linkList.value = response.data || []
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
    linkName: null,
    status: null
  }
  handleQuery()
}

function handleSelectionChange(selection) {
  // handle selection
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加友情链接'
}

function handleUpdate(row) {
  reset()
  const linkId = row.linkId || this.ids
  getFriendLink(linkId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改友情链接'
  })
}

function submitForm() {
  linkForm.value.validate(valid => {
    if (valid) {
      if (form.value.linkId) {
        updateFriendLink(form.value).then(response => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addFriendLink(form.value).then(response => {
          ElMessage.success('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

function handleDelete(row) {
  // handle delete
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.value = {
    linkId: null,
    linkName: null,
    linkUrl: null,
    linkLogo: null,
    linkDesc: null,
    sortOrder: 0,
    status: '0'
  }
}

getList()
</script>
