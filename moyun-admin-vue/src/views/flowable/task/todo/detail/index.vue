<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <span class="el-icon-document">待办任务</span>
          <el-tag style="margin-left:10px">发起人:{{ startUser }}</el-tag>
          <el-tag>任务节点:{{ taskName }}</el-tag>
          <el-button style="float: right;" type="danger" @click="goBack">关闭</el-button>
        </div>
      </template>

      <el-tabs tab-position="top" v-model="activeName" @tab-click="handleClick">
        <!--表单信息-->
        <el-tab-pane label="表单信息" name="1">
          <el-col :span="16" :offset="4">
            <v-form-render ref="vFormRef" />
            <div style="margin-left:10%;margin-bottom: 20px;font-size: 14px;">
              <el-button type="primary" @click="handleComplete">审 批</el-button>
            </div>
          </el-col>
        </el-tab-pane>

        <!--流程流转记录-->
        <el-tab-pane label="流转记录" name="2">
          <!--flowRecordList-->
          <el-col :span="16" :offset="4">
            <div class="block">
              <el-timeline>
                <el-timeline-item v-for="(item, index) in flowRecordList" :key="index" :icon="setIcon(item.finishTime)"
                  :color="setColor(item.finishTime)">
                  <p style="font-weight: 700">{{ item.taskName }}</p>
                  <el-card :body-style="{ padding: '10px' }">
                    <el-descriptions class="margin-top" :column="1" border>
                      <el-descriptions-item v-if="item.assigneeName" label-class-name="my-label">
                        <template #label><i class="el-icon-user"></i>办理人</template>
                        {{ item.assigneeName }}
                        <el-tag type="info">{{ item.deptName }}</el-tag>
                      </el-descriptions-item>
                      <el-descriptions-item v-if="item.candidate" label-class-name="my-label">
                        <template #label><i class="el-icon-user"></i>候选办理</template>
                        {{ item.candidate }}
                      </el-descriptions-item>
                      <el-descriptions-item label-class-name="my-label">
                        <template #label><i class="el-icon-date"></i>接收时间</template>
                        {{ item.createTime }}
                      </el-descriptions-item>
                      <el-descriptions-item v-if="item.finishTime" label-class-name="my-label">
                        <template #label><i class="el-icon-date"></i>处理时间</template>
                        {{ item.finishTime }}
                      </el-descriptions-item>
                      <el-descriptions-item v-if="item.duration" label-class-name="my-label">
                        <template #label><i class="el-icon-time"></i>耗时</template>
                        {{ item.duration }}
                      </el-descriptions-item>
                      <el-descriptions-item v-if="item.comment" label-class-name="my-label">
                        <template #label><i class="el-icon-tickets"></i>处理意见</template>
                        {{ item.comment.comment }}
                      </el-descriptions-item>
                    </el-descriptions>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-col>
        </el-tab-pane>
        <!--流程图-->
        <el-tab-pane label="流程图" name="3">
          <bpmn-viewer :flowData="flowData" :procInsId="taskForm.procInsId" />
        </el-tab-pane>
      </el-tabs>
      <!--审批任务-->
      <el-dialog :title="completeTitle" v-model="completeOpen" width="60%" append-to-body>
        <el-form ref="taskFormRef" :model="taskForm">
          <el-form-item prop="targetKey">
            <flow-user v-if="checkSendUser" :checkType="checkType" @handleUserSelect="handleUserSelect"></flow-user>
            <flow-role v-if="checkSendRole" @handleRoleSelect="handleRoleSelect"></flow-role>
          </el-form-item>
          <el-form-item label="处理意见" label-width="80px" prop="comment"
            :rules="[{ required: true, message: '请输入处理意见', trigger: 'blur' }]">
            <el-input type="textarea" v-model="taskForm.comment" placeholder="请输入处理意见" />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="completeOpen = false">取 消</el-button>
            <el-button type="primary" @click="taskComplete">确 定</el-button>
          </div>
        </template>

      </el-dialog>
      <!--退回流程-->
      <el-dialog :title="returnTitle" v-model="returnOpen" width="40%" append-to-body>
        <el-form ref="taskFormRef" :model="taskForm" label-width="80px">
          <el-form-item label="退回节点" prop="targetKey">
            <el-radio-group v-model="taskForm.targetKey">
              <el-radio-button v-for="item in returnTaskList" :key="item.id" :label="item.id">{{ item.name }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="退回意见" prop="comment" :rules="[{ required: true, message: '请输入意见', trigger: 'blur' }]">
            <el-input style="width: 50%" type="textarea" v-model="taskForm.comment" placeholder="请输入意见" />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="returnOpen = false">取 消</el-button>
            <el-button type="primary" @click="taskReturn">确 定</el-button>
          </div>
        </template>

      </el-dialog>
      <!--驳回流程-->
      <el-dialog :title="rejectTitle" v-model="rejectOpen" width="40%" append-to-body>
        <el-form ref="taskFormRef" :model="taskForm" label-width="80px">
          <el-form-item label="驳回意见" prop="comment" :rules="[{ required: true, message: '请输入意见', trigger: 'blur' }]">
            <el-input style="width: 50%" type="textarea" v-model="taskForm.comment" placeholder="请输入意见" />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="rejectOpen = false">取 消</el-button>
            <el-button type="primary" @click="taskReject">确 定</el-button>
          </div>
        </template>

      </el-dialog>
    </el-card>
  </div>
</template>

<script setup name="TodoDetail">
import { ref, reactive, onMounted } from 'vue'
import {
  complete,
  rejectTask,
  returnList,
  returnTask,
  getNextFlowNode,
  delegate,
  flowTaskForm
} from '@/api/flowable/todo'
import { flowXmlAndNode } from '@/api/flowable/definition'
import { flowRecord } from "@/api/flowable/finished";
import BpmnViewer from '@/components/Process/viewer';
import FlowUser from '@/components/flow/User'
import FlowRole from '@/components/flow/Role'


const router = useRouter()
const { proxy } = getCurrentInstance()

// 响应式状态
const activeName = ref('1')
const loading = ref(true)
const flowRecordList = ref([])
const flowData = ref({})
const completeOpen = ref(false)
const returnOpen = ref(false)
const rejectOpen = ref(false)
const completeTitle = ref(null)
const returnTitle = ref(null)
const rejectTitle = ref(null)
const checkSendUser = ref(false)
const checkSendRole = ref(false)
const checkType = ref('single')
const multiInstanceVars = ref('')
const formJson = ref({})
const returnTaskList = ref([])
const taskFormRef = ref(null)
// 表单数据
const taskForm = reactive({
  deployId: '',
  taskId: '',
  procInsId: '',
  executionId: '',
  instanceId: '',
  procDefId: '',
  targetKey: '',
  comment: '',
  variables: {},
  defaultTaskShow: true,
  returnTaskShow: false,
  delegateTaskShow: false
})

// 查询参数
const startUser = ref('')
const taskName = ref('')

/** 初始化数据 */
function init() {
  const query = router.currentRoute.value.query
  if (query) {
    startUser.value = query.startUser
    taskName.value = query.taskName
    taskForm.deployId = query.deployId
    taskForm.taskId = query.taskId
    taskForm.procInsId = query.procInsId
    taskForm.executionId = query.executionId
    taskForm.instanceId = query.procInsId

    getFlowTaskForm(taskForm.taskId)
    getFlowRecordList(taskForm.procInsId, taskForm.deployId)
  }
}

/** 获取流程表单信息 */
function getFlowTaskForm(taskId) {
  if (taskId) {
    flowTaskForm({ taskId }).then(res => {
      formJson.value = res.data.formJson
      proxy.$refs.vFormRef.setFormJson(res.data.formJson)
      proxy.$nextTick(() => {
        proxy.$refs.vFormRef.setFormData(res.data)
      })
    })
  }
}

/** 获取流程流转记录 */
function getFlowRecordList(procInsId, deployId) {
  flowRecord({ procInsId, deployId }).then(res => {
    flowRecordList.value = res.data.flowList
  }).catch(() => {
    goBack()
  })
}

/** 切换到流程图标签页时获取流程XML数据 */
function handleClick(tab) {
  if (tab.paneName === '3') {
    flowXmlAndNode({ procInsId: taskForm.procInsId, deployId: taskForm.deployId }).then(res => {
      flowData.value = res.data
    })
  }
}

/** 设置图标 */
function setIcon(val) {
  return val ? 'el-icon-check' : 'el-icon-time'
}

/** 设置颜色 */
function setColor(val) {
  return val ? '#2bc418' : '#b3bdbb'
}

/** 用户选择回调 */
function handleUserSelect(selection) {
  if (selection) {
    if (Array.isArray(selection)) {
      const selectVal = selection.map(item => item.userId.toString())
      if (multiInstanceVars.value) {
        taskForm.variables[multiInstanceVars.value] = selectVal
      } else {
        taskForm.variables.approval = selectVal.join(',')
      }
    } else {
      taskForm.variables.approval = selection.userId?.toString()
    }
  }
}

/** 角色选择回调 */
function handleRoleSelect(selection) {
  if (selection) {
    const selectVal = Array.isArray(selection) ? selection.map(i => i.roleId.toString()) : [selection]
    taskForm.variables.approval = selectVal.join(',')
  }
}

/** 返回上一页 */
function goBack() {
  proxy.$tab.closeOpenPage({ path: '/task/todo', query: { t: Date.now() } })
}

/** 驳回按钮 */
function handleReject() {
  rejectOpen.value = true
  rejectTitle.value = '驳回流程'
}

/** 提交驳回 */
function taskReject() {
  rejectTask(taskForm).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    goBack()
  })
}

/** 获取可退回节点列表 */
function handleReturn() {
  returnOpen.value = true
  returnTitle.value = '退回流程'
  returnList(taskForm).then(res => {
    returnTaskList.value = res.data
  })
}

/** 提交退回 */
function taskReturn() {
  returnTask(taskForm).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    goBack()
  })
}

/** 审批任务打开弹窗 */
function handleComplete() {
  completeOpen.value = true
  completeTitle.value = '流程审批'
  submitForm()
}

/** 提交审批任务 */
function taskComplete() {
  if (!taskForm.variables && checkSendUser.value) {
    proxy.$modal.msgError("请选择流程接收人员!")
    return
  }
  if (!taskForm.variables && checkSendRole.value) {
    proxy.$modal.msgError("请选择流程接收角色组!")
    return
  }
  if (!taskForm.comment) {
    proxy.$modal.msgError("请输入审批意见!")
    return
  }

  complete(taskForm).then(res => {
    proxy.$modal.msgSuccess(res.msg)
    goBack()
  })
}

/** 提交前获取下一步节点信息 */
function submitForm() {
  const params = { taskId: taskForm.taskId }
  getNextFlowNode(params).then(res => {
    proxy.$refs.vFormRef.getFormData().then(formData => {
      Object.assign(taskForm.variables, formData)
      taskForm.variables.formJson = formJson.value
    })

    const data = res.data
    if (data) {
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
      }
    }
  })
}

// 页面加载钩子
onMounted(() => {
  init()
})
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
  clear: both
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
