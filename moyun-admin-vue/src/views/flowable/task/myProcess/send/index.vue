<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <span class="el-icon-document">发起任务</span>
          <el-button style="float: right" type="danger" @click="goBack">关闭</el-button>
        </div>
      </template>

      <el-tabs tab-position="top" v-model="activeName" @tab-click="handleClick">
        <!-- 表单信息 -->
        <el-tab-pane label="表单信息" name="1">
          <el-col :span="16" :offset="4">
            <v-form-render :form-data="formRenderData" ref="vFormRef" />
            <div style="margin-left:15%;margin-bottom: 20px;font-size: 14px;">
              <el-button type="primary" @click="submitForm">提 交</el-button>
              <el-button type="primary" @click="resetForm">重 置</el-button>
            </div>
          </el-col>
        </el-tab-pane>

        <!-- 流程图 -->
        <el-tab-pane label="流程图" name="2">
          <bpmn-viewer :flowData="flowData" />
        </el-tab-pane>
      </el-tabs>

      <!-- 选择流程接收人 -->
      <el-dialog :title="taskTitle" v-model="taskOpen" width="65%" append-to-body>
        <flow-user v-if="checkSendUser" :checkType="checkType" @handleUserSelect="handleUserSelect" />
        <flow-role v-if="checkSendRole" @handleRoleSelect="handleRoleSelect" />
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="taskOpen = false">取 消</el-button>
            <el-button type="primary" @click="submitTask">提 交</el-button>
          </div>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup name="FlowStart">
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'

import { definitionStart, flowXmlAndNode } from "@/api/flowable/definition";
import BpmnViewer from '@/components/Process/viewer';
import { flowFormData } from "@/api/flowable/process";
import { getNextFlowNodeByStart } from "@/api/flowable/todo";
import FlowUser from '@/components/flow/User'
import FlowRole from '@/components/flow/Role'

const { proxy } = getCurrentInstance()

// 响应式数据
const activeName = ref('1') // 切换tab标签
const loading = ref(true)
const deployId = ref('')
const procDefId = ref('')
const formRenderData = ref({})
const variables = ref([])
const taskTitle = ref(null)
const taskOpen = ref(false)
const checkSendUser = ref(false) // 是否展示人员选择模块
const checkSendRole = ref(false) // 是否展示角色选择模块
const checkType = ref('')
const checkValues = ref(null) // 选中任务接收人员数据
const formData = ref({}) // 填写的表单数据
const multiInstanceVars = ref('') // 会签节点
const formJson = ref({}) // 表单json
const flowData = ref({})

/** 初始化获取参数 */
onMounted(() => {
  deployId.value = proxy.$route.query?.deployId
  procDefId.value = proxy.$route.query?.procDefId
  getFlowFormData(deployId.value)
})

/** 流程表单数据 */
function getFlowFormData(deployId) {
  const params = { deployId }
  flowFormData(params).then(res => {
    proxy.$nextTick(() => {
      proxy.$refs.vFormRef.setFormJson(res.data)
      formJson.value = res.data
    })
  }).catch(res => {
    goBack()
  })
}

/** 返回页面 */
function goBack() {
  const obj = { path: "/task/process", query: { t: Date.now() } }
  proxy.$tab.closeOpenPage(obj)
}

/** Tab切换时加载流程图 */
function handleClick(tab /* , event */) {
  if (tab.paneName === '2') {
    flowXmlAndNode({ deployId: deployId.value }).then(res => {
      flowData.value = res.data
    })
  }
}

/** 申请流程表单数据提交 */
function submitForm() {
  proxy.$refs.vFormRef.getFormData().then(formDataRes => {
    // 获取下一步节点
    getNextFlowNodeByStart({ deploymentId: deployId.value, variables: formDataRes }).then(res => {
      const data = res.data
      if (data) {
        formData.value = formDataRes
        if (data.dataType === 'dynamic') {
          if (data.type === 'assignee') {
            checkSendUser.value = true
            checkType.value = "single"
          } else if (data.type === 'candidateUsers') {
            checkSendUser.value = true
            checkType.value = "multiple"
          } else if (data.type === 'candidateGroups') {
            checkSendRole.value = true
          } else {
            multiInstanceVars.value = data.vars
            checkSendUser.value = true
            checkType.value = "multiple"
          }
          taskOpen.value = true
          taskTitle.value = "选择任务接收"
        } else {
          if (procDefId.value) {
            const param = {
              formJson: formJson.value
            }
            Object.assign(param, formDataRes)
            definitionStart(procDefId.value, param).then(res => {
              proxy.$modal.msgSuccess(res.msg)
              goBack()
            })
          }
        }
      }
    }).catch(error => {
      // proxy.$modal.msgError(error)
    })
  })
}

/** 重置表单 */
function resetForm() {
  proxy.$refs.vFormRef.resetForm()
}

/** 提交流程 */
function submitTask() {
  if (!checkValues.value && checkSendUser.value) {
    proxy.$modal.msgError("请选择任务接收!")
    return
  }
  if (!checkValues.value && checkSendRole.value) {
    proxy.$modal.msgError("请选择流程接收角色组!")
    return
  }

  if (formData.value) {
    const param = {
      formJson: formJson.value
    }
    Object.assign(param, formData.value)

    if (multiInstanceVars.value) {
      param[multiInstanceVars.value] = checkValues.value
    } else {
      param["approval"] = checkValues.value
    }

    definitionStart(procDefId.value, param).then(res => {
      proxy.$modal.msgSuccess(res.msg)
      goBack()
    })
  }
}

/** 用户信息选中数据 */
function handleUserSelect(selection) {
  if (selection) {
    if (Array.isArray(selection)) {
      const selectVal = selection.map(item => item.userId)
      if (multiInstanceVars.value) {
        checkValues.value = selectVal
      } else {
        checkValues.value = selectVal.join(',')
      }
    } else {
      checkValues.value = selection.userId
    }
  }
}

/** 角色信息选中数据 */
function handleRoleSelect(selection) {
  if (selection) {
    if (Array.isArray(selection)) {
      const selectVal = selection.map(item => item.roleId)
      checkValues.value = selectVal.join(',')
    } else {
      checkValues.value = selection
    }
  }
}
</script>

<style lang="scss" scoped>
.test-form {
  margin: 15px auto;
  width: 800px;
  padding: 15px;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both;
}

.box-card {
  width: 100%;
  margin-bottom: 20px;
}

.el-tag+.el-tag {
  margin-left: 10px;
}

.my-label {
  background: #E1F3D8;
}
</style>