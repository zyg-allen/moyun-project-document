<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
      <el-form-item label="套餐名称" prop="packageName">
        <el-input
          v-model="queryParams.packageName"
          placeholder="请输入套餐名称"
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

    <el-table v-loading="loading" :data="packageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="套餐ID" align="center" prop="packageId" width="80" />
      <el-table-column label="套餐名称" align="center" prop="packageName" width="150" />
      <el-table-column label="时长(天)" align="center" prop="duration" width="100" />
      <el-table-column label="价格" align="center" prop="price" width="100">
        <template #default="scope">
          ¥{{ scope.row.price }}
        </template>
      </el-table-column>
      <el-table-column label="原价" align="center" prop="originalPrice" width="100">
        <template #default="scope">
          ¥{{ scope.row.originalPrice }}
        </template>
      </el-table-column>
      <el-table-column label="热门" align="center" prop="isPopular" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.isPopular" type="success">是</el-tag>
          <el-tag v-else type="info">否</el-tag>
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

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="packageForm" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="套餐名称" prop="packageName">
          <el-input v-model="form.packageName" placeholder="请输入套餐名称" />
        </el-form-item>
        <el-form-item label="时长(天)" prop="duration">
          <el-input-number v-model="form.duration" :min="1" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="原价" prop="originalPrice">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="套餐描述" prop="description">
          <el-input v-model="form.description" type="textarea" placeholder="请输入套餐描述" />
        </el-form-item>
        <el-form-item label="热门" prop="isPopular">
          <el-switch v-model="form.isPopular" />
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

<script setup name="Vip">
import { listVipPackage, getVipPackage, addVipPackage, updateVipPackage, deleteVipPackage } from '@/api/community/vip'

const loading = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const packageList = ref([])
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  packageName: null,
  status: null
})

const form = ref({})
const rules = ref({
  packageName: [{ required: true, message: '套餐名称不能为空', trigger: 'blur' }],
  duration: [{ required: true, message: '时长不能为空', trigger: 'blur' }],
  price: [{ required: true, message: '价格不能为空', trigger: 'blur' }]
})
const packageForm = ref()

function getList() {
  loading.value = true
  listVipPackage(queryParams.value).then(response => {
    packageList.value = response.data || []
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
    packageName: null,
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
  title.value = '添加VIP套餐'
}

function handleUpdate(row) {
  reset()
  const packageId = row.packageId || this.ids
  getVipPackage(packageId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改VIP套餐'
  })
}

function submitForm() {
  packageForm.value.validate(valid => {
    if (valid) {
      if (form.value.packageId) {
        updateVipPackage(form.value).then(response => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addVipPackage(form.value).then(response => {
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
    packageId: null,
    packageName: null,
    duration: 30,
    price: 0,
    originalPrice: 0,
    description: null,
    isPopular: false,
    sortOrder: 0,
    status: '0'
  }
}

getList()
</script>
