<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="开始时间" prop="deployTime">
        <el-date-picker clearable v-model="queryParams.deployTime" type="date" value-format="yyyy-MM-dd"
          placeholder="选择时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="search" @click="handleQuery">搜索</el-button>
        <el-button icon="refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="plus" @click="handleLoadXml">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="delete" :disabled="multiple" @click="handleDelete"
          v-hasPermi="['system:deployment:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert title="流程设计说明" type="success">
      <template #title>
        <p>流程设计说明:</p>
        <div>1、XML文件中的流程定义id属性用作流程定义的key参数。</div>
        <div>2、XML文件中的流程定义name属性用作流程定义的name参数。如果未给定name属性，会使用id作为name。</div>
        <div>3、当每个唯一key的流程第一次部署时，指定版本为1。对其后所有使用相同key的流程定义，部署时版本会在该key当前已部署的最高版本号基础上加1。key参数用于区分流程定义。</div>
        <div>4、id参数设置为{processDefinitionKey}:{processDefinitionVersion}:{generated-id}，其中generated-id是一个唯一数字，用以保证在集群环境下，流程定义缓存中，流程id的唯一性。</div>
      </template>
    </el-alert>

    <el-table v-loading="loading" fit :data="definitionList" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="流程编号" align="center" prop="deploymentId" :show-overflow-tooltip="true" />
      <el-table-column label="流程标识" align="center" prop="flowKey" :show-overflow-tooltip="true" />
      <el-table-column label="流程分类" align="center" prop="category" />
      <el-table-column label="流程名称" align="center" width="120" :show-overflow-tooltip="true">
        <template #="scope">
          <el-button type="primary" link @click="handleReadImage(scope.row.deploymentId)">
            <span>{{ scope.row.name }}</span>
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="业务表单" align="center" :show-overflow-tooltip="true">
        <template #="scope">
          <el-button v-if="scope.row.formId" type="primary" link @click="handleForm(scope.row.formId)">
            <span>{{ scope.row.formName }}</span>
          </el-button>
          <label v-else>暂无表单</label>
        </template>
      </el-table-column>
      <el-table-column label="流程版本" align="center">
        <template #="scope">
          <el-tag>v{{ scope.row.version }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center">
        <template #="scope">
          <el-tag type="success" v-if="scope.row.suspensionState === 1">激活</el-tag>
          <el-tag type="warning" v-if="scope.row.suspensionState === 2">挂起</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="部署时间" align="center" prop="deploymentTime" width="180" />
      <el-table-column label="操作" width="250" fixed="right" class-name="small-padding fixed-width">
        <template #="scope">
          <el-button @click="handleLoadXml(scope.row)" icon="edit-outline" type="primary" link>设计</el-button>
          <el-button @click="handleAddForm(scope.row)" icon="edit-el-icon-s-promotion" type="primary" link
            v-if="scope.row.formId == null">配置主表单</el-button>
          <el-button @click="handleUpdateSuspensionState(scope.row)" icon="video-pause" type="primary" link
            v-if="scope.row.suspensionState === 1">挂起</el-button>
          <el-button @click="handleUpdateSuspensionState(scope.row)" icon="video-play" type="primary" link
            v-if="scope.row.suspensionState === 2">激活</el-button>
          <el-button @click="handleDelete(scope.row)" icon="delete" type="primary" link
            v-hasPermi="['system:deployment:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改流程定义对话框 -->
    <el-dialog :title="title" v-model="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="看看" prop="name">
          <el-input v-model="form.name" placeholder="请输入看看" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- bpmn20.xml导入对话框 -->
    <el-dialog :title="upload.title" v-model="upload.open" width="400px" append-to-body>
      <el-upload ref="upload" :limit="1" accept=".xml" :headers="upload.headers"
        :action="upload.url + '?name=' + upload.name + '&category=' + upload.category"
        :disabled="upload.isUploading" :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess" :auto-upload="false" drag>
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">
          将文件拖到此处，或
          <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            流程名称：<el-input v-model="upload.name" />
            流程分类：
            <div>
              <el-select v-model="upload.category" placeholder="请选择流程分类">
                <el-option v-for="dict in dict.sys_process_category" :key="dict.value" :label="dict.label"
                  :value="dict.value"></el-option>
              </el-select>
            </div>
          </div>
          <div class="el-upload__tip" style="color:red">提示：仅允许导入“bpmn20.xml”格式文件！</div>
        </template>
      </el-upload>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitFileForm">确 定</el-button>
          <el-button @click="upload.open = false">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 流程图 -->
    <el-dialog :title="readImage.title" v-model="readImage.open" width="70%" append-to-body>
      <bpmn-viewer :flowData="flowData" />
    </el-dialog>

    <!--表单配置详情-->
    <el-dialog :title="formTitle" v-model="formConfOpen" width="50%" append-to-body>
      <div class="test-form">
        <v-form-render :form-data="formData" ref="vFormRef" />
      </div>
    </el-dialog>

    <!--挂载表单-->
    <el-dialog :title="formDeployTitle" v-model="formDeployOpen" width="60%" append-to-body>
      <el-row :gutter="24">
        <el-col :span="10" :xs="24">
          <el-table ref="singleTable" :data="formList" border highlight-current-row
            @current-change="handleCurrentChange" style="width: 100%">
            <el-table-column label="表单编号" align="center" prop="formId" />
            <el-table-column label="表单名称" align="center" prop="formName" />
            <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
              <template #="scope">
                <el-button type="primary" link @click="submitFormDeploy(scope.row)">确定</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination small layout="prev, pager, next" v-show="formTotal > 0" :total="formTotal"
            v-model:page="formQueryParams.pageNum" v-model:limit="formQueryParams.pageSize"
            @pagination="ListFormDeploy" />
        </el-col>
        <el-col :span="14" :xs="24">
          <div class="test-form">
            <v-form-render :form-data="formData" ref="vFormCurrentRowRef" />
          </div>
        </el-col>
      </el-row>
    </el-dialog>

    <!-- 流程设计器 -->
    <el-dialog title="流程配置" v-model="dialogVisible" :close-on-press-escape="false" :fullscreen="true"
      :before-close="handleClose" destroy-on-close append-to-body>
      <Model :deployId="deployId" @close="() => { dialogVisible = false; getList(); }" />
    </el-dialog>
  </div>
</template>

<script setup>
import {
  listDefinition,
  updateState,
  delDeployment,
  addDeployment,
  updateDeployment,
  exportDeployment,
  definitionStart,
  flowXmlAndNode
} from '@/api/flowable/definition'
import { getToken } from '@/utils/auth'
import { getForm, addDeployForm, listForm } from '@/api/flowable/form'
import BpmnViewer from '@/components/Process/viewer/index.vue'
import Model from './model.vue'

import { ref, reactive, onMounted, watch, getCurrentInstance } from 'vue'

// 数据初始化
const { proxy } = getCurrentInstance()
const dict = ref(proxy.useDict('sys_process_category'))

// 查询参数
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  name: null,
  category: null,
  key: null,
  tenantId: null,
  deployTime: null,
  derivedFrom: null,
  derivedFromRoot: null,
  parentDeploymentId: null,
  engineVersion: null
})

// 表格数据
const loading = ref(true)
const total = ref(0)
const definitionList = ref([])

// 弹出层控制
const open = ref(false)
const formConfOpen = ref(false)
const formDeployOpen = ref(false)
const readImage = ref({ open: false, src: '' })
const dialogVisible = ref(false)
const formTitle = ref('')
const formDeployTitle = ref('')
const title = ref('')
const form = ref({})
const rules = ref({})
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const showSearch = ref(true)

// 文件上传配置
const upload = ref({
  open: false,
  title: '',
  isUploading: false,
  name: null,
  category: null,
  headers: { Authorization: 'Bearer ' + getToken() },
  url: import.meta.env.VITE_APP_BASE_API + '/flowable/definition/import'
})

// 分页与查询
const formQueryParams = ref({
  pageNum: 1,
  pageSize: 10
})
const formList = ref([])
const formTotal = ref(0)
const formData = ref({})
const formDeployParam = ref({
  formId: null,
  deployId: null
})
const deployId = ref('')
const flowData = ref({})

/**
 * 查询流程定义列表
 */
function getList() {
  loading.value = true
  listDefinition(queryParams.value).then(response => {
    definitionList.value = response.data.records
    total.value = response.data.total
    loading.value = false
  })
}

/**
 * 关闭弹窗回调
 */
function handleClose(done) {
  proxy.$confirm('确定要关闭吗？关闭未保存的修改都会丢失？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    done()
  }).catch(() => {})
}

/**
 * 取消按钮
 */
function cancel() {
  open.value = false
  reset()
}

/**
 * 表单重置
 */
function reset() {
  form.value = {
    id: null,
    name: null,
    category: null,
    key: null,
    tenantId: null,
    deployTime: null,
    derivedFrom: null,
    derivedFromRoot: null,
    parentDeploymentId: null,
    engineVersion: null
  }
  // this.resetForm("form")
}

/**
 * 搜索按钮操作
 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/**
 * 重置按钮操作
 */
function resetQuery() {
  // this.resetForm("queryForm")
  handleQuery()
}

/**
 * 多选框选中数据
 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.deploymentId)
  single.value = selection.length !== 1
  multiple.value = !selection.length
}

/**
 * 新增按钮操作
 */
function handleAdd() {
  reset()
  open.value = true
  title.value = '添加流程定义'
}

/**
 * 跳转到流程设计页面
 */
function handleLoadXml(row) {
  dialogVisible.value = true
  deployId.value = row.deploymentId
}

/**
 * 流程图查看
 */
function handleReadImage(deployId) {
  readImage.value.title = '流程图'
  readImage.value.open = true
  flowXmlAndNode({ deployId: deployId }).then(res => {
    flowData.value = res.data
  })
}

/**
 * 表单查看
 */
function handleForm(formId) {
  getForm(formId).then(res => {
    formTitle.value = '表单详情'
    formConfOpen.value = true
    proxy.$nextTick(() => {
      proxy.$refs.vFormRef.setFormJson(JSON.parse(res.data.formContent))
      proxy.$nextTick(() => {
        proxy.$refs.vFormRef.disableForm()
      })
    })
  })
}

/**
 * 挂载表单弹框
 */
function handleAddForm(row) {
  formDeployParam.value.deployId = row.deploymentId
  ListFormDeploy()
}

/**
 * 挂载表单列表
 */
function ListFormDeploy() {
  listForm(formQueryParams.value).then(res => {
    formList.value = res.rows
    formTotal.value = res.total
    formDeployOpen.value = true
    formDeployTitle.value = '挂载表单'
  })
}

/**
 * 挂载表单
 */
function submitFormDeploy(row) {
  formDeployParam.value.formId = row.formId
  addDeployForm(formDeployParam.value).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    formDeployOpen.value = false
    getList()
  })
}

/**
 * 当前行变化
 */
function handleCurrentChange(data) {
  if (data) {
    proxy.$nextTick(() => {
      proxy.$refs.vFormCurrentRowRef.setFormJson(JSON.parse(data.formContent))
      proxy.$nextTick(() => {
        proxy.$refs.vFormCurrentRowRef.disableForm()
      })
    })
  }
}

/**
 * 挂起/激活流程
 */
function handleUpdateSuspensionState(row) {
  let state = 1
  if (row.suspensionState === 1) {
    state = 2
  }
  const params = {
    deployId: row.deploymentId,
    state: state
  }
  updateState(params).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    getList()
  })
}

/**
 * 删除按钮操作
 */
function handleDelete(row) {
  const deploymentIds = row.deploymentId || ids.value.join(',')
  proxy.$confirm(`是否确认删除流程定义编号为"${deploymentIds}"的数据项?`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    return delDeployment(deploymentIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

/**
 * 提交按钮
 */
function submitForm() {
  proxy.$refs.form.validate(valid => {
    if (valid) {
      if (form.value.id != null) {
        updateDeployment(form.value).then(response => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        })
      } else {
        addDeployment(form.value).then(response => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        })
      }
    }
  })
}

/**
 * 导入bpmn.xml文件
 */
function handleImport() {
  upload.value.title = 'bpmn20.xml文件导入'
  upload.value.open = true
}

/**
 * 文件上传中处理
 */
function handleFileUploadProgress(event, file, fileList) {
  upload.value.isUploading = true
}

/**
 * 文件上传成功处理
 */
function handleFileSuccess(response, file, fileList) {
  upload.value.open = false
  upload.value.isUploading = false
  proxy.$refs.upload.clearFiles()
  proxy.$message(response.msg)
  getList()
}

/**
 * 提交上传文件
 */
function submitFileForm() {
  proxy.$refs.upload.submit()
}

// 初始化加载
getList()
</script>