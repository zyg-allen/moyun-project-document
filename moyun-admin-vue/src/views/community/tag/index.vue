<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
      <el-form-item label="标签名称" prop="tagName">
        <el-input
          v-model="queryParams.tagName"
          placeholder="请输入标签名称"
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

    <el-table v-loading="loading" :data="tagList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="标签ID" align="center" prop="tagId" width="80" />
      <el-table-column label="标签名称" align="center" prop="tagName" width="150" />
      <el-table-column label="标签别名" align="center" prop="tagSlug" width="150" />
      <el-table-column label="文章数" align="center" prop="articleCount" width="80" />
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
      <el-form ref="tagForm" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签别名" prop="tagSlug">
          <el-input v-model="form.tagSlug" placeholder="请输入标签别名" />
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

<script setup name="Tag">
import { listTag, getTag, addTag, updateTag, deleteTag } from '@/api/community/tag'

const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const tagList = ref([])
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  tagName: null,
  status: null
})

const form = ref({})
const rules = ref({
  tagName: [{ required: true, message: '标签名称不能为空', trigger: 'blur' }],
  tagSlug: [{ required: true, message: '标签别名不能为空', trigger: 'blur' }]
})
const tagForm = ref()

function getList() {
  loading.value = true
  listTag(queryParams.value).then(response => {
    tagList.value = response.data || []
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
    tagName: null,
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
  title.value = '添加标签'
}

function handleUpdate(row) {
  reset()
  const tagId = row.tagId || this.ids
  getTag(tagId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改标签'
  })
}

function submitForm() {
  tagForm.value.validate(valid => {
    if (valid) {
      if (form.value.tagId) {
        updateTag(form.value).then(response => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addTag(form.value).then(response => {
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
    tagId: null,
    tagName: null,
    tagSlug: null,
    sortOrder: 0,
    status: '0'
  }
}

getList()
</script>
