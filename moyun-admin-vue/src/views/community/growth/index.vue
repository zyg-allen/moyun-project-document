<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="等级配置" name="level">
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

        <el-table v-loading="loading" :data="levelList">
          <el-table-column label="等级ID" align="center" prop="levelId" width="80" />
          <el-table-column label="等级名称" align="center" prop="levelName" width="150" />
          <el-table-column label="等级图标" align="center" prop="levelIcon" width="80">
            <template #default="scope">
              <el-image :src="scope.row.levelIcon" style="width: 50px; height: 50px" fit="cover" />
            </template>
          </el-table-column>
          <el-table-column label="所需积分" align="center" prop="requiredPoints" width="100" />
          <el-table-column label="特权说明" align="center" prop="privilege" :show-overflow-tooltip="true" />
          <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
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
          <el-form ref="levelForm" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="等级名称" prop="levelName">
              <el-input v-model="form.levelName" placeholder="请输入等级名称" />
            </el-form-item>
            <el-form-item label="等级图标" prop="levelIcon">
              <el-input v-model="form.levelIcon" placeholder="请输入等级图标地址" />
            </el-form-item>
            <el-form-item label="所需积分" prop="requiredPoints">
              <el-input-number v-model="form.requiredPoints" :min="0" />
            </el-form-item>
            <el-form-item label="特权说明" prop="privilege">
              <el-input v-model="form.privilege" type="textarea" placeholder="请输入特权说明" />
            </el-form-item>
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" />
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="dialog-footer">
              <el-button type="primary" @click="submitForm">确 定</el-button>
              <el-button @click="cancel">取 消</el-button>
            </div>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="徽章管理" name="badge">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="Plus"
              @click="handleAddBadge"
            >新增</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="badgeList">
          <el-table-column label="徽章ID" align="center" prop="badgeId" width="80" />
          <el-table-column label="徽章名称" align="center" prop="badgeName" width="150" />
          <el-table-column label="徽章图标" align="center" prop="badgeIcon" width="80">
            <template #default="scope">
              <el-image :src="scope.row.badgeIcon" style="width: 50px; height: 50px" fit="cover" />
            </template>
          </el-table-column>
          <el-table-column label="类型" align="center" prop="type" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.type === 'bronze' ? 'info' : scope.row.type === 'silver' ? '' : scope.row.type === 'gold' ? 'warning' : 'danger'">
                {{ scope.row.type === 'bronze' ? '铜' : scope.row.type === 'silver' ? '银' : scope.row.type === 'gold' ? '金' : '铂' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="获取条件" align="center" prop="condition" :show-overflow-tooltip="true" />
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
                @click="handleUpdateBadge(scope.row)"
              >修改</el-button>
              <el-button
                link
                type="danger"
                icon="Delete"
                @click="handleDeleteBadge(scope.row)"
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

        <el-dialog :title="title" v-model="badgeOpen" width="600px" append-to-body>
          <el-form ref="badgeForm" :model="badgeForm" :rules="badgeRules" label-width="100px">
            <el-form-item label="徽章名称" prop="badgeName">
              <el-input v-model="badgeForm.badgeName" placeholder="请输入徽章名称" />
            </el-form-item>
            <el-form-item label="徽章图标" prop="badgeIcon">
              <el-input v-model="badgeForm.badgeIcon" placeholder="请输入徽章图标地址" />
            </el-form-item>
            <el-form-item label="类型" prop="type">
              <el-select v-model="badgeForm.type" placeholder="请选择类型">
                <el-option label="铜" value="bronze" />
                <el-option label="银" value="silver" />
                <el-option label="金" value="gold" />
                <el-option label="铂" value="platinum" />
              </el-select>
            </el-form-item>
            <el-form-item label="获取条件" prop="condition">
              <el-input v-model="badgeForm.condition" type="textarea" placeholder="请输入获取条件" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="badgeForm.status">
                <el-radio value="0">启用</el-radio>
                <el-radio value="1">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="dialog-footer">
              <el-button type="primary" @click="submitBadgeForm">确 定</el-button>
              <el-button @click="cancelBadge">取 消</el-button>
            </div>
          </template>
        </el-dialog>
      </el-tab-pane>

      <el-tab-pane label="积分规则" name="points">
        <el-table v-loading="loading" :data="pointsList">
          <el-table-column label="规则ID" align="center" prop="ruleId" width="80" />
          <el-table-column label="动作" align="center" prop="action" width="150" />
          <el-table-column label="获得积分" align="center" prop="points" width="100">
            <template #default="scope">
              +{{ scope.row.points }}
            </template>
          </el-table-column>
          <el-table-column label="每日上限" align="center" prop="dailyLimit" width="100" />
          <el-form-item label="描述" prop="description" align="center">
            <template #default="scope">
              {{ scope.row.description }}
            </template>
          </el-form-item>
          <el-table-column label="操作" align="center" width="100">
            <template #default="scope">
              <el-button
                link
                type="primary"
                icon="Edit"
                @click="handleEditPoints(scope.row)"
              >编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog title="编辑积分规则" v-model="pointsOpen" width="500px" append-to-body>
          <el-form ref="pointsForm" :model="pointsForm" label-width="100px">
            <el-form-item label="动作">
              <el-input v-model="pointsForm.action" disabled />
            </el-form-item>
            <el-form-item label="获得积分">
              <el-input-number v-model="pointsForm.points" :min="0" />
            </el-form-item>
            <el-form-item label="每日上限">
              <el-input-number v-model="pointsForm.dailyLimit" :min="0" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="pointsForm.description" type="textarea" placeholder="请输入描述" />
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="dialog-footer">
              <el-button type="primary" @click="submitPointsForm">确 定</el-button>
              <el-button @click="cancelPoints">取 消</el-button>
            </div>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="Growth">
import { listLevelConfig, addLevelConfig, updateLevelConfig, deleteLevelConfig, listBadge, addBadge, updateBadge, deleteBadge, listPointsRule, updatePointsRule } from '@/api/community/growth'

const loading = ref(true)
const showSearch = ref(true)
const activeTab = ref('level')
const open = ref(false)
const badgeOpen = ref(false)
const pointsOpen = ref(false)
const title = ref('')
const levelList = ref([])
const badgeList = ref([])
const pointsList = ref([])
const total = ref(0)

const queryParams = ref({
  pageNum: 1,
  pageSize: 10
})

const form = ref({})
const badgeForm = ref({})
const pointsForm = ref({})
const rules = ref({
  levelName: [{ required: true, message: '等级名称不能为空', trigger: 'blur' }],
  requiredPoints: [{ required: true, message: '所需积分不能为空', trigger: 'blur' }]
})
const badgeRules = ref({
  badgeName: [{ required: true, message: '徽章名称不能为空', trigger: 'blur' }]
})
const levelForm = ref()
const badgeFormRef = ref()
const pointsFormRef = ref()

function getList() {
  loading.value = true
  let apiCall
  switch (activeTab.value) {
    case 'level':
      apiCall = listLevelConfig(queryParams.value)
      break
    case 'badge':
      apiCall = listBadge(queryParams.value)
      break
    case 'points':
      apiCall = listPointsRule(queryParams.value)
      break
  }
  apiCall.then(response => {
    switch (activeTab.value) {
      case 'level':
        levelList.value = response.data || []
        break
      case 'badge':
        badgeList.value = response.data || []
        break
      case 'points':
        pointsList.value = response.data || []
        break
    }
    total.value = response.total || 0
    loading.value = false
  })
}

function handleTabClick() {
  queryParams.value.pageNum = 1
  getList()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '添加等级配置'
}

function handleUpdate(row) {
  reset()
  form.value = { ...row }
  open.value = true
  title.value = '修改等级配置'
}

function submitForm() {
  levelForm.value.validate(valid => {
    if (valid) {
      if (form.value.levelId) {
        updateLevelConfig(form.value).then(response => {
          ElMessage.success('修改成功')
          open.value = false
          getList()
        })
      } else {
        addLevelConfig(form.value).then(response => {
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
    levelId: null,
    levelName: null,
    levelIcon: null,
    requiredPoints: 0,
    privilege: null,
    sortOrder: 0
  }
}

function handleAddBadge() {
  resetBadge()
  badgeOpen.value = true
  title.value = '添加徽章'
}

function handleUpdateBadge(row) {
  resetBadge()
  badgeForm.value = { ...row }
  badgeOpen.value = true
  title.value = '修改徽章'
}

function submitBadgeForm() {
  badgeFormRef.value.validate(valid => {
    if (valid) {
      if (badgeForm.value.badgeId) {
        updateBadge(badgeForm.value).then(response => {
          ElMessage.success('修改成功')
          badgeOpen.value = false
          getList()
        })
      } else {
        addBadge(badgeForm.value).then(response => {
          ElMessage.success('新增成功')
          badgeOpen.value = false
          getList()
        })
      }
    }
  })
}

function handleDeleteBadge(row) {
  // handle delete
}

function cancelBadge() {
  badgeOpen.value = false
  resetBadge()
}

function resetBadge() {
  badgeForm.value = {
    badgeId: null,
    badgeName: null,
    badgeIcon: null,
    type: 'bronze',
    condition: null,
    status: '0'
  }
}

function handleEditPoints(row) {
  pointsForm.value = { ...row }
  pointsOpen.value = true
}

function submitPointsForm() {
  updatePointsRule(pointsForm.value).then(response => {
    ElMessage.success('修改成功')
    pointsOpen.value = false
    getList()
  })
}

function cancelPoints() {
  pointsOpen.value = false
}

getList()
</script>
