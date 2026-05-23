<template>
  <div class="app-container">
    <el-card class="box-card">
      <template #header>
        <div class="clearfix">
          <span class="el-icon-document">已办任务</span>
          <el-button style="float: right;" type="danger" @click="goBack">关闭</el-button>
        </div>
      </template>

      <el-tabs tab-position="top" v-model="activeName" @tab-click="handleClick">
        <!--表单信息-->
        <el-tab-pane label="表单信息" name="1">
          <el-col :span="16" :offset="4">
            <v-form-render ref="vFormRef" />
          </el-col>
        </el-tab-pane>
        <!--流程流转记录-->
        <el-tab-pane label="流转记录" name="2">
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
        <el-tab-pane label="流程图" name="3">
          <bpmn-viewer :flowData="flowData" :procInsId="taskForm.procInsId" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup name="FinishedDetail">
import { ref, reactive, onMounted } from 'vue'
import BpmnViewer from '@/components/Process/viewer'
import { flowRecord } from "@/api/flowable/finished";
import { getProcessVariables, flowXmlAndNode } from "@/api/flowable/definition";

const router = useRouter()
const { proxy } = getCurrentInstance()

// 响应式状态
const activeName = ref('1')
const loading = ref(true)
const flowRecordList = ref([])
const flowData = ref({})
const formJson = ref({})

// 表单数据
const taskForm = reactive({
  deployId: '',
  taskId: '',
  procInsId: ''
})

/** 初始化数据 */
function init() {
  const query = router.currentRoute.value.query
  if (query) {
    taskForm.deployId = query.deployId
    taskForm.taskId = query.taskId
    taskForm.procInsId = query.procInsId

    processVariables(taskForm.taskId)
    getFlowRecordList(taskForm.procInsId, taskForm.deployId)
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

/** 获取流程变量内容 */
function processVariables(taskId) {
  if (taskId) {
    getProcessVariables(taskId).then(res => {
      formJson.value = res.data.formJson
      proxy.$refs.vFormRef.setFormJson(res.data.formJson)
      proxy.$nextTick(() => {
        proxy.$refs.vFormRef.setFormData(res.data)
        proxy.$nextTick(() => {
          // 禁用表单
          proxy.$refs.vFormRef.disableForm()
        })
      })
    })
  }
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

/** 返回上一页 */
function goBack() {
  proxy.$tab.closeOpenPage({ path: '/task/finished', query: { t: Date.now() } })
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
