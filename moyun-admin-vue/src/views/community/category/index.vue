<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="default" :inline="true" v-show="showSearch">
      <el-form-item label="栏目名称" prop="categoryName">
        <el-input
          v-model="queryParams.categoryName"
          placeholder="请输入栏目名称"
          clearable
        />
      </el-form-item>
      <el-form-item label="类型" prop="categoryType">
        <el-select v-model="queryParams.categoryType" placeholder="请选择类型" clearable>
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
          icon="Plus"
          @click="handleAdd"
        >新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-loading="loading"
      :data="categoryList"
      row-key="categoryId"
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
    >
      <el-table-column prop="categoryName" label="栏目名称" width="200" />
      <el-table-column prop="categoryCode" label="栏目编码" width="120" />
      <el-table-column prop="categoryType" label="类型" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.categoryType === 'technical'" type="success">技术</el-tag>
          <el-tag v-else type="warning">文学</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag v-if="scope.row.status === '0'" type="success">正常</el-tag>
          <el-tag v-else type="danger">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isShow" label="首页显示" width="100">
        <template #default="scope">
          <el-switch v-model="scope.row.isShow" :active-value="1" :inactive-value="0" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="上级栏目" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="categoryTree"
            :props="{ value: 'categoryId', label: 'categoryName', children: 'children' }"
            placeholder="选择上级栏目"
            check-strictly
          />
        </el-form-item>
        <el-form-item label="栏目名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入栏目名称" />
        </el-form-item>
        <el-form-item label="栏目编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="请输入栏目编码" />
        </el-form-item>
        <el-form-item label="栏目类型" prop="categoryType">
          <el-radio-group v-model="form.categoryType">
            <el-radio label="technical">技术</el-radio>
            <el-radio label="literary">文学</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Category">
import { listCategory, getCategoryTree, saveCategory, updateCategory, deleteCategory } from '@/api/community/category'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(true)
const showSearch = ref(true)
const categoryList = ref([])
const categoryTree = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()

const queryParams = ref({
  categoryName: null,
  categoryType: null
})

const form = ref({
  categoryId: null,
  parentId: 0,
  categoryName: '',
  categoryCode: '',
  categoryType: 'technical',
  sort: 0,
  status: '0'
})

const rules = {
  categoryName: [{ required: true, message: '栏目名称不能为空', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '栏目编码不能为空', trigger: 'blur' }],
  categoryType: [{ required: true, message: '栏目类型不能为空', trigger: 'change' }]
}

function getList() {
  loading.value = true
  listCategory(queryParams.value).then(response => {
    categoryList.value = response.data || []
    loading.value = false
  })
}

function getTree() {
  getCategoryTree().then(response => {
    categoryTree.value = [{ categoryId: 0, categoryName: '顶级栏目', children: response.data || [] }]
  })
}

function handleQuery() {
  getList()
}

function resetQuery() {
  queryParams.value = {
    categoryName: null,
    categoryType: null
  }
  handleQuery()
}

function handleAdd() {
  dialogTitle.value = '添加栏目'
  form.value = {
    categoryId: null,
    parentId: 0,
    categoryName: '',
    categoryCode: '',
    categoryType: 'technical',
    sort: 0,
    status: '0'
  }
  dialogVisible.value = true
}

function handleUpdate(row) {
  dialogTitle.value = '修改栏目'
  form.value = { ...row }
  dialogVisible.value = true
}

function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.value.categoryId) {
        updateCategory(form.value).then(() => {
          ElMessage.success('修改成功')
          dialogVisible.value = false
          getList()
          getTree()
        })
      } else {
        saveCategory(form.value).then(() => {
          ElMessage.success('新增成功')
          dialogVisible.value = false
          getList()
          getTree()
        })
      }
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm('是否确认删除名称为"' + row.categoryName + '"的数据项?', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deleteCategory(row.categoryId).then(() => {
      ElMessage.success('删除成功')
      getList()
      getTree()
    })
  })
}

getList()
getTree()
</script>
