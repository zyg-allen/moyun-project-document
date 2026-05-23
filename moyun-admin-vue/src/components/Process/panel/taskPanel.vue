<template>
  <div>
    <el-form label-width="80px">
      <el-form-item label="异步">
        <el-switch v-model="bpmnFormData.async" active-text="是" inactive-text="否"
          @change="updateElementTask('async')" />
      </el-form-item>

      <el-form-item label="用户类型">
        <el-select v-model="bpmnFormData.userType" placeholder="选择人员" @change="updateUserType">
          <el-option v-for="item in userTypeOption" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <template v-if="bpmnFormData.userType">
        <!-- 指定人员 -->
        <el-form-item label="指定人员" v-if="bpmnFormData.userType === 'assignee'">
          <el-input-tag v-model="bpmnFormData.assignee" :value="bpmnFormData.assignee" />
          <el-button-group class="ml-4" style="margin-top: 4px">
            <el-tooltip effect="dark" content="指定人员" placement="bottom">
              <el-button type="primary" size="small" icon="user" @click="singleUserCheck" />
            </el-tooltip>
            <el-tooltip effect="dark" content="选择表达式" placement="bottom">
              <el-button type="warning" size="small" icon="postcard" @click="singleExpCheck" />
            </el-tooltip>
          </el-button-group>
        </el-form-item>

        <!-- 候选人员 -->
        <el-form-item label="候选人员" v-else-if="bpmnFormData.userType === 'candidateUsers'">
          <el-input-tag v-model="bpmnFormData.candidateUsers" :value="bpmnFormData.candidateUsers" />
          <el-button-group class="ml-4" style="margin-top: 4px">
            <el-tooltip effect="dark" content="候选人员" placement="bottom">
              <el-button type="primary" size="small" icon="user" @click="multipleUserCheck" />
            </el-tooltip>
            <el-tooltip effect="dark" content="选择表达式" placement="bottom">
              <el-button type="warning" size="small" icon="postcard" @click="singleExpCheck" />
            </el-tooltip>
          </el-button-group>
        </el-form-item>

        <!-- 候选角色 -->
        <el-form-item label="候选角色" v-else>
          <el-input-tag v-model="bpmnFormData.candidateGroups" :value="bpmnFormData.candidateGroups" />
          <el-button-group class="ml-4" style="margin-top: 4px">
            <el-tooltip effect="dark" content="候选角色" placement="bottom">
              <el-button type="primary" size="small" icon="user" @click="multipleRoleCheck" />
            </el-tooltip>
            <el-tooltip effect="dark" content="选择表达式" placement="bottom">
              <el-button type="warning" size="small" icon="postcard" @click="singleExpCheck" />
            </el-tooltip>
          </el-button-group>
        </el-form-item>
      </template>
      <el-form-item label="优先级">
        <el-input v-model="bpmnFormData.priority" @change="updateElementTask('priority')" />
      </el-form-item>

      <el-form-item label="到期时间">
        <el-input v-model="bpmnFormData.dueDate" @change="updateElementTask('dueDate')" />
      </el-form-item>
    </el-form>

    <!-- Dialogs -->

    <!-- 选择人员 -->
    <el-dialog title="选择人员" v-model="userVisible" width="60%" :close-on-press-escape="false" :show-close="false"
      destroy-on-close>
      <flow-user :checkType="checkType" :selectValues="assignee || candidateUsers" @handleUserSelect="userSelect" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="userVisible = false">取 消</el-button>
          <el-button type="primary" @click="checkUserComplete">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 选择角色 -->
    <el-dialog title="选择候选角色" v-model="roleVisible" width="60%" :close-on-press-escape="false" :show-close="false">
      <flow-role v-if="roleVisible" :selectValues="candidateGroups" @handleRoleSelect="roleSelect" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="roleVisible = false">取 消</el-button>
          <el-button type="primary" @click="checkRoleComplete">确 定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 选择表达式 -->
    <el-dialog title="选择表达式" v-model="expVisible" width="60%" :close-on-press-escape="false" :show-close="false">
      <flow-exp v-if="expVisible" :selectValues="exp" @handleSingleExpSelect="expSelect" />
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="expVisible = false">取 消</el-button>
          <el-button type="primary" @click="checkExpComplete">确 定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import FlowUser from '@/components/flow/User'
import FlowRole from '@/components/flow/Role'
import FlowExp from '@/components/flow/Expression'
import ElInputTag from '@/components/flow/ElInputTag'
import useModelerStore from '@/components/Process/common/global.js'
import { StrUtil } from '@/utils/StrUtil'

const modelerStore = useModelerStore()
// props
const props = defineProps({
  id: {
    type: String,
    required: true
  }
})

// 数据定义
const userVisible = ref(false)
const roleVisible = ref(false)
const expVisible = ref(false)
const checkType = ref('single')

const userTypeOption = [
  { label: '指定人员', value: 'assignee' },
  { label: '候选人员', value: 'candidateUsers' },
  { label: '候选角色', value: 'candidateGroups' }
]

const bpmnFormData = ref({
  userType: '',
  assignee: '',
  candidateUsers: '',
  candidateGroups: '',
  dueDate: '',
  priority: '',
  dataType: '',
  expId: ''
})

// const selectData = ref({
//   assignee: null,
//   candidateUsers: null,
//   candidateGroups: null,
//   exp: null
// })
const assignee = ref()
const candidateUsers = ref()
const candidateGroups = ref()
const exp = ref()

// 初始化表单
function resetTaskForm() {
  for (let key in bpmnFormData.value) {
    const value = window.bpmnElement?.businessObject[key] ?? ''
    bpmnFormData.value[key] = value
  }
  checkValuesEcho(bpmnFormData.value)
}

// 更新 bpmn 元素属性
function updateElementTask(key) {
  const taskAttr = {}
  taskAttr[key] = bpmnFormData.value[key] || ''
  window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, taskAttr)
}

// 更新自定义属性
function updateCustomElement(key, value) {
  const taskAttr = {}
  taskAttr[key] = value
  window.bpmnModeler.get('modeling').updateProperties(window.bpmnElement, taskAttr)
}

// 更新用户类型时删除旧数据
function updateUserType(val) {
  deleteFlowAttar()
  delete window.bpmnElement.businessObject.userType
  bpmnFormData.value[val] = null
  assignee.value = null
  candidateUsers.value = null
  candidateGroups.value = null
  exp.value = null

  updateCustomElement('userType', val)
}

// 数据回显处理
function checkValuesEcho(formData) {
  if (StrUtil.isNotBlank(formData.expId)) {
    getExpList(formData.expId, formData.userType)
  } else if (formData.userType === 'candidateGroups') {
    getRoleList(formData[formData.userType], formData.userType)
  } else {
    getUserList(formData[formData.userType], formData.userType)
  }
}

function getExpList(id, key) {
  if (StrUtil.isNotBlank(id)) {
    const item = modelerStore.expList.find(item => item.id.toString() === id)
    if (item) {
      if (key === key) { } // 这段代码防止奇怪报错！勿删
      bpmnFormData.value[key] = item.name
      exp.value = item.id
    }
  }
}

function getUserList(val, key) {
  if (StrUtil.isNotBlank(val)) {
    const users = modelerStore.userList.filter(i => val.split(',').includes(i.userId.toString()))
    const nickNames = users.map(u => u.nickName).join(',')
    bpmnFormData.value[key] = nickNames
    if (key === key) { } // 这段代码防止奇怪报错！勿删
    [key].value = key === 'assignee' ? users[0]?.userId : users.map(u => u.userId)
  }
}

function getRoleList(val, key) {
  if (StrUtil.isNotBlank(val)) {
    const roles = modelerStore.roleList.filter(i => val.split(',').includes(i.roleId.toString()))
    const roleNames = roles.map(r => r.roleName).join(',')
    bpmnFormData.value[key] = roleNames
    if (key === key) { } // 这段代码防止奇怪报错！勿删
    [key].value = key === 'assignee' ? roles[0]?.roleId : roles.map(r => r.roleId)
  }
}

// 弹窗操作
function singleUserCheck() {
  userVisible.value = true
  checkType.value = 'single'
}

function multipleUserCheck() {
  userVisible.value = true
  checkType.value = 'multiple'
}

function multipleRoleCheck() {
  roleVisible.value = true
}

function singleExpCheck() {
  expVisible.value = true
}

function expSelect(selection) {
  if (selection) {
    deleteFlowAttar()
    bpmnFormData.value[bpmnFormData.value.userType] = selection.name
    updateCustomElement('dataType', selection.dataType)
    updateCustomElement('expId', selection.id.toString())
    updateCustomElement(bpmnFormData.value.userType, selection.expression)
    handleSelectData('exp', selection.id)
  }
}

// 用户选中数据 TODO: 后面更改为 点击确认按钮再赋值人员信息
function userSelect(selection) {
  if (selection) {
    deleteFlowAttar()
    updateCustomElement('dataType', 'fixed')
    if (Array.isArray(selection)) {
      const userIds = selection.map(u => u.userId)
      const names = selection.map(u => u.nickName).join(',')
      bpmnFormData.value[bpmnFormData.value.userType] = names
      updateCustomElement(bpmnFormData.value.userType, userIds.join(','))
      handleSelectData(bpmnFormData.value.userType, userIds)
    } else {
      bpmnFormData.value[bpmnFormData.value.userType] = selection.nickName
      updateCustomElement(bpmnFormData.value.userType, selection.userId)
      handleSelectData(bpmnFormData.value.userType, selection.userId)
    }
  }
}

function roleSelect(selection, name) {
  if (selection && name) {
    deleteFlowAttar()
    bpmnFormData.value[bpmnFormData.value.userType] = name
    updateCustomElement('dataType', 'fixed')
    updateCustomElement(bpmnFormData.value.userType, selection)
    handleSelectData(bpmnFormData.value.userType, selection)
  }
}

function handleSelectData(key, value) {
  const selectData = {
    assignee: null,
    candidateUsers: null,
    candidateGroups: null,
    exp: null
  }
  for (const oldKey in selectData) {
    [oldKey].value = key === oldKey ? value : null
  }
}

function checkUserComplete() {
  userVisible.value = false
  checkType.value = ''
}

function checkRoleComplete() {
  roleVisible.value = false
}

function checkExpComplete() {
  expVisible.value = false
}

function deleteFlowAttar() {
  delete window.bpmnElement.businessObject.dataType
  delete window.bpmnElement.businessObject.expId
  delete window.bpmnElement.businessObject.assignee
  delete window.bpmnElement.businessObject.candidateUsers
  delete window.bpmnElement.businessObject.candidateGroups
}

// Watch props.id
watch(
  () => props.id,
  (newVal) => {
    if (StrUtil.isNotBlank(newVal)) {
      resetTaskForm()
    }
  },
  { immediate: true }
)
</script>