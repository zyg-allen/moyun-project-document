<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="表单名称" prop="formName">
        <el-input v-model="queryParams.formName" placeholder="请输入表单名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
        <el-button icon="refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="plus" @click="handleAdd">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="delete" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['flowable:form:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="formList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="表单主键" align="center" prop="formId" />
      <el-table-column label="表单名称" align="center" prop="formName" />
      <el-table-column label="备注" align="center" prop="remark" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #="scope">
          <el-button type="primary" link icon="view" @click="handleDetail(scope.row)">详情</el-button>
          <el-button type="primary" link icon="edit" @click="handleUpdate(scope.row)"
            v-hasPermi="['flowable:form:edit']">修改</el-button>
          <el-button type="primary" link icon="delete" @click="handleDelete(scope.row)"
            v-hasPermi="['flowable:form:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改流程表单对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="表单名称" prop="formName">
          <el-input v-model="form.formName" placeholder="请输入表单名称" />
        </el-form-item>
        <el-form-item label="表单内容">
          <editor v-model="form.formContent" :min-height="192" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 表单详情 -->
    <el-dialog :title="formTitle" v-model="formRenderOpen" width="60%" append-to-body>
      <v-form-render :form-data="formData" ref="vFormRef" />
    </el-dialog>

    <!-- 表单设计器 -->
    <el-dialog v-model="dialogVisible" :close-on-press-escape="false" :fullscreen="true" :before-close="handleClose"
      append-to-body>
      <v-form-designer ref="vfDesigner" :designer-config="designerConfig">
        <!-- 自定义按钮插槽演示 -->
        <template #customSaveButton>
          <el-button type="primary" link @click="saveFormJson"><i class="el-icon-s-promotion" />保存</el-button>
        </template>
      </v-form-designer>
    </el-dialog>

    <!-- 系统表单信息 -->
    <el-dialog :title="formTitle" v-model="formOpen" width="500px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="表单名称" prop="formName">
          <el-input v-model="form.formName" placeholder="请输入表单名称" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注" />
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

<script setup name="FlowForm">
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import Editor from '@/components/Editor'

import {
  listForm,
  getForm,
  delForm,
  addForm,
  updateForm
} from '@/api/flowable/form'

const { proxy } = getCurrentInstance()

// 响应式数据
const data = reactive({
  // 查询参数
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    formName: null,
    formContent: null
  },
  // 表单参数
  form: {
    formId: null,
    formName: null,
    formContent: null,
    remark: null
  },
  // 表单校验规则
  rules: {}
})

const {
  queryParams,
  form,
  rules
} = toRefs(data)

// 页面状态
const loading = ref(true)
const total = ref(0)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const formTitle = ref('')
const formRenderOpen = ref(false)
const formOpen = ref(false)
const dialogVisible = ref(false)
const designerConfig = {
  exportCodeButton: false
}
const formList = ref([])
const formData = ref({})

/** 查询流程表单列表 */
function getList() {
  loading.value = true
  listForm(queryParams.value).then(response => {
    formList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

// 表单重置
function reset() {
  form.value = {
    formId: null,
    formName: null,
    formContent: null,
    createTime: null,
    updateTime: null,
    createBy: null,
    updateBy: null,
    remark: null
  }
  proxy.resetForm("form")
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryForm")
  handleQuery()
}

// 多选框选中数据
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.formId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/** 表单配置信息 */
function handleDetail(row) {
  formRenderOpen.value = true
  formTitle.value = "表单详情"
  proxy.$nextTick(() => {
    // 回显数据
    proxy.$refs.vFormRef.setFormJson(JSON.parse(row.formContent))
    proxy.$nextTick(() => {
      // 表单禁用
      proxy.$refs.vFormRef.disableForm()
    })
  })
}

/** 新增按钮操作 */
function handleAdd() {
  proxy.$router.push({ path: '/flowable/task/flowForm/index' })
}

// 保存表单数据
function saveFormJson() {
  let formJson = proxy.$refs.vfDesigner.getFormJson()
  form.value.formContent = JSON.stringify(formJson)
  formOpen.value = true
}

// 取消按钮
function cancel() {
  formOpen.value = false
  reset()
}

// 关闭设计器前确认
function handleClose(done) {
  proxy.$confirm('确定要关闭吗？关闭未保存的修改都会丢失？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    done()
  }).catch(() => { })
}

/** 修改按钮操作 */
function handleUpdate(row) {
  proxy.$router.push({ path: '/flowable/task/flowForm/index', query: { formId: row.formId } })
}

/** 重置表单 */
function resetFormData() {
  proxy.$refs.vFormRef.resetForm()
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["formRef"].validate(valid => {
    if (valid) {
      if (form.value.formId != null) {
        updateForm(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          formOpen.value = false
          getList()
        })
      } else {
        addForm(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          formOpen.value = false
          getList()
        })
      }
      dialogVisible.value = false
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const formIds = row.formId || ids.value
  proxy.$confirm('是否确认删除表单编号为"' + formIds + '"的数据项?', "警告", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning"
  }).then(function () {
    return delForm(formIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => { })
}

// 初次加载
getList()
</script>

<style scoped>
.test-form {
  margin: 15px auto;
  width: 800px;
  padding: 15px;
}

::v-deep .dialogClass .el-dialog__header {
  padding: 0;
}

::v-deep .dialogClass .el-dialog__body {
  padding: 0;
}
</style>