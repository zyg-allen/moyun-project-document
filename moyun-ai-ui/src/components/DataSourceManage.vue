<template>
  <div class="datasource-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-database"></i>
      </div>
      <div class="header-content">
        <h2>数据源管理</h2>
        <span class="item-count">共 {{ datasources.length }} 个数据源</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showAddDialog" class="create-btn">
          <i class="fa-solid fa-plus"></i> 添加数据源
        </el-button>
      </div>
    </div>

    <!-- 内容容器 -->
    <div class="content-container">
      <!-- 数据源卡片列表 -->
      <div class="datasource-cards" v-loading="loading">
        <TransitionGroup name="card-list">
          <div 
            v-for="ds in datasources" 
            :key="ds.id" 
            class="datasource-card"
          >
            <!-- 卡片顶部装饰条 -->
            <div class="card-accent" :class="{ active: ds.enabled }"></div>
            
            <!-- 卡片主体内容 -->
            <div class="card-content">
              <!-- 头部：图标 + 标题 + 状态 -->
              <div class="card-top">
                <div class="card-icon" :class="{ active: ds.enabled }">
                  <i class="fa-solid fa-database"></i>
                </div>
                <div class="card-title-area">
                  <div class="card-title-row">
                    <span class="name">{{ ds.name }}</span>
                    <el-tag :type="getTypeTagType(ds.type)" size="small" effect="plain" round>
                      {{ ds.type.toUpperCase() }}
                    </el-tag>
                    <el-tag 
                      :type="ds.enabled ? 'success' : 'info'" 
                      size="small" 
                      effect="plain" 
                      round 
                      class="status-tag">
                      {{ ds.enabled ? '已启用' : '已禁用' }}
                    </el-tag>
                  </div>
                  <div class="desc">{{ ds.host }}:{{ ds.port }} / {{ ds.databaseName }}</div>
                </div>
              </div>
              
              <!-- 数据源信息标签 -->
              <div class="datasource-tags">
                <span 
                  class="datasource-tag health-tag" 
                  :class="'health-' + ds.healthStatus"
                  @click="checkHealth(ds)" 
                  style="cursor: pointer;">
                  <i class="fa-solid fa-heart"></i>
                  {{ getHealthText(ds.healthStatus) }}
                </span>
                <span class="datasource-tag" v-if="ds.description">
                  <i class="fa-solid fa-info-circle"></i>
                  {{ ds.description }}
                </span>
              </div>

              <!-- 底部：元信息 + 操作按钮 -->
              <div class="card-bottom">
                <div class="card-meta">
                  <span class="meta-item">
                    <i class="fa-solid fa-user"></i>
                    <span>{{ ds.username }}</span>
                  </span>
                </div>
                <div class="card-actions" @click.stop>
                  <el-tooltip content="查看表" placement="top" :show-after="200">
                    <button class="action-btn primary" @click="viewTables(ds)">
                      <i class="fa-solid fa-table"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="同步" placement="top" :show-after="200">
                    <button class="action-btn" @click="syncMetadata(ds)">
                      <i class="fa-solid fa-sync"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top" :show-after="200">
                    <button class="action-btn" @click="editDataSource(ds)">
                      <i class="fa-solid fa-edit"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip :content="ds.enabled ? '禁用' : '启用'" placement="top" :show-after="200">
                    <button class="action-btn" @click="toggleEnabled(ds)">
                      <i :class="ds.enabled ? 'fa-solid fa-toggle-on' : 'fa-solid fa-toggle-off'"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top" :show-after="200">
                    <button class="action-btn danger" @click="deleteDataSource(ds)">
                      <i class="fa-solid fa-trash"></i>
                    </button>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </TransitionGroup>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 36, 48]"
        :total="datasources.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 添加/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle"
      width="600px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="数据源名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入数据源名称" />
        </el-form-item>

        <el-form-item label="数据源类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%" @change="handleTypeChange">
            <el-option label="MySQL" value="mysql" />
            <el-option label="Elasticsearch" value="elasticsearch" />
          </el-select>
        </el-form-item>

        <el-form-item label="主机地址" prop="host">
          <el-input v-model="formData.host" placeholder="localhost" />
        </el-form-item>

        <el-form-item label="端口" prop="port">
          <el-input-number v-model="formData.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>

        <!-- MySQL专属字段 -->
        <el-form-item v-if="formData.type === 'mysql'" label="数据库名" prop="databaseName">
          <el-input v-model="formData.databaseName" placeholder="请输入数据库名" />
        </el-form-item>

        <!-- Elasticsearch专属字段 -->
        <el-form-item v-if="formData.type === 'elasticsearch'" label="索引前缀">
          <el-input v-model="formData.databaseName" placeholder="索引前缀（可选，如：my-index-*）" />
          <div style="font-size: 12px; color: #909399; margin-top: 4px;">
            可选填，用于筛选特定前缀的索引
          </div>
        </el-form-item>

        <!-- 用户名密码：MySQL必填，ES可选 -->
        <el-form-item label="用户名" :prop="formData.type === 'mysql' ? 'username' : ''">
          <el-input v-model="formData.username" :placeholder="formData.type === 'elasticsearch' ? '可选，如启用了安全认证则填写' : '请输入用户名'" />
        </el-form-item>

        <el-form-item label="密码" :prop="formData.type === 'mysql' ? 'password' : ''">
          <el-input 
            v-model="formData.password" 
            type="password" 
            :placeholder="formData.type === 'elasticsearch' ? '可选，如启用了安全认证则填写' : '请输入密码'" 
            show-password />
        </el-form-item>

        <el-form-item label="描述">
          <el-input 
            v-model="formData.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入描述信息" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="testConnection" :loading="testing">测试连接</el-button>
        <el-button type="primary" @click="saveDataSource" :loading="saving">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 表列表对话框 -->
    <el-dialog v-model="tablesDialogVisible" title="数据表列表" width="1200px">
      <el-input 
        v-model="tableSearch" 
        placeholder="搜索表名或注释..." 
        style="margin-bottom: 15px"
        prefix-icon="Search"
        clearable />
      <el-table :data="filteredTables" height="500" v-loading="loadingTables" stripe>
        <el-table-column prop="tableName" label="表名" width="200">
          <template #default="scope">
            <div style="font-weight: 600; color: #303133;">
              <i class="fa-solid fa-table" style="color: #409eff; margin-right: 6px;"></i>
              {{ scope.row.tableName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="tableComment" :label="currentDatasource.type === 'elasticsearch' ? '类型' : '表注释'" min-width="200">
          <template #default="scope">
            <span style="color: #606266;">{{ scope.row.tableComment || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="rowCount" label="数据总数" width="120" align="right">
          <template #default="scope">
            <el-tag type="info" size="small">
              {{ formatNumber(scope.row.rowCount) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataSizeFormatted" label="存储大小" width="120" align="right">
          <template #default="scope">
            <el-tag type="warning" size="small">
              {{ scope.row.dataSizeFormatted }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <!-- 所有数据源都显示查看字段按钮 -->
            <el-button size="small" type="primary" @click="viewColumns(scope.row.tableName)">
              <i class="fa-solid fa-columns"></i> {{ currentDatasource.type === 'elasticsearch' ? '查看映射' : '查看字段' }}
            </el-button>
            <!-- 所有数据源都显示查询按钮 -->
            <el-button size="small" @click="queryTable(scope.row)">
              <i class="fa-solid fa-search"></i> 查询
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
    
    <!-- 字段详情对话框 -->
    <el-dialog v-model="columnsDialogVisible" :title="'表字段：' + currentTableName" width="1000px">
      <el-table :data="tableColumns" v-loading="loadingColumns" stripe max-height="500">
        <el-table-column prop="columnName" label="字段名" width="180">
          <template #default="scope">
            <div style="font-weight: 600; color: #409eff;">
              <i class="fa-solid fa-key" v-if="scope.row.columnKey === 'PRI'" style="color: #f56c6c; margin-right: 4px;"></i>
              <i class="fa-solid fa-circle" v-else style="color: #c0c4cc; font-size: 6px; margin-right: 6px;"></i>
              {{ scope.row.columnName }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="dataType" label="数据类型" width="150">
          <template #default="scope">
            <el-tag size="small" type="success">{{ scope.row.dataType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="columnComment" label="字段注释" min-width="200">
          <template #default="scope">
            <span style="color: #606266;">{{ scope.row.columnComment || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="nullable" label="可空" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.nullable === 'YES' ? 'info' : 'warning'" size="small">
              {{ scope.row.nullable === 'YES' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="columnDefault" label="默认值" width="120">
          <template #default="scope">
            <span style="color: #909399; font-size: 12px;">{{ scope.row.columnDefault || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="extra" label="额外" width="100">
          <template #default="scope">
            <el-tag v-if="scope.row.extra" size="small" type="info">{{ scope.row.extra }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()

const datasources = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const dialogVisible = ref(false)
const dialogTitle = ref('添加数据源')
const testing = ref(false)
const saving = ref(false)

const formRef = ref(null)
const formData = reactive({
  id: null,
  name: '',
  type: 'mysql',
  host: 'localhost',
  port: 3306,
  databaseName: '',
  username: '',
  password: '',
  description: '',
  enabled: true
})

const rules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 表列表相关
const tablesDialogVisible = ref(false)
const tables = ref([])
const tableSearch = ref('')
const loadingTables = ref(false)
const currentDatasource = ref(null)

// 字段详情相关
const columnsDialogVisible = ref(false)
const tableColumns = ref([])
const loadingColumns = ref(false)
const currentTableName = ref('')

const filteredTables = computed(() => {
  if (!tableSearch.value) return tables.value
  const keyword = tableSearch.value.toLowerCase()
  return tables.value.filter(t => 
    t.tableName?.toLowerCase().includes(keyword) ||
    t.tableComment?.toLowerCase().includes(keyword)
  )
})

onMounted(() => {
  loadDataSources()
})

const loadDataSources = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/data-analysis/datasources')
    const list = res.data.data || []
    
    // 确保每个数据源都有healthStatus字段，如果没有则设置为'unknown'
    datasources.value = list.map(ds => ({
      ...ds,
      healthStatus: ds.healthStatus || 'unknown'
    }))
    
    console.log('已加载数据源:', datasources.value.length, '个')
  } catch (error) {
    ElMessage.error('加载数据源失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  dialogTitle.value = '添加数据源'
  Object.assign(formData, {
    id: null,
    name: '',
    type: 'mysql',
    host: 'localhost',
    port: 3306,
    databaseName: '',
    username: '',
    password: '',
    description: '',
    enabled: true
  })
  dialogVisible.value = true
}

// 类型改变时调整默认端口
const handleTypeChange = (type) => {
  const defaultPorts = {
    'mysql': 3306,
    'elasticsearch': 9200
  }
  
  // 只在端口是默认值或为空时才自动更改
  if (!formData.port || formData.port === 3306 || formData.port === 9200) {
    formData.port = defaultPorts[type] || 3306
  }
  
  console.log('数据源类型已切换为:', type, '端口:', formData.port)
}

const editDataSource = (row) => {
  dialogTitle.value = '编辑数据源'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const testConnection = async () => {
  testing.value = true
  try {
    const res = await axios.post('/api/data-analysis/datasources/test', formData)
    if (res.data.success) {
      ElMessage.success('连接成功!')
    } else {
      ElMessage.error('连接失败')
    }
  } catch (error) {
    ElMessage.error('测试失败: ' + error.message)
  } finally {
    testing.value = false
  }
}

const saveDataSource = async () => {
  const valid = await formRef.value.validate()
  if (!valid) return

  saving.value = true
  try {
    if (formData.id) {
      await axios.put(`/api/data-analysis/datasources/${formData.id}`, formData)
      ElMessage.success('更新成功')
    } else {
      await axios.post('/api/data-analysis/datasources', formData)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadDataSources()
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const deleteDataSource = (row) => {
  ElMessageBox.confirm('确定要删除该数据源吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`/api/data-analysis/datasources/${row.id}`)
      ElMessage.success('删除成功')
      loadDataSources()
    } catch (error) {
      ElMessage.error('删除失败: ' + error.message)
    }
  })
}

const toggleEnabled = async (row) => {
  try {
    await axios.put(`/api/data-analysis/datasources/${row.id}`, row)
    ElMessage.success('状态更新成功')
  } catch (error) {
    ElMessage.error('更新失败: ' + error.message)
    row.enabled = !row.enabled
  }
}

const checkHealth = async (row) => {
  try {
    const res = await axios.get(`/api/data-analysis/datasources/${row.id}/health`)
    console.log('健康检查完整响应:', res)
    console.log('响应数据:', res.data)
    
    // 更新健康状态 - 后端返回格式：{success: true, message: "healthy"}
    if (res.data && res.data.success) {
      // 状态值在 message 字段，不在 data 字段
      const status = res.data.message || res.data.data
      console.log('健康状态值:', status)
      
      if (status === 'healthy' || status === 'unhealthy' || status === 'unknown') {
        row.healthStatus = status
        const statusText = getHealthText(status)
        ElMessage.success(`健康检查完成：${statusText}`)
        console.log('✅ 状态已更新为:', status)
      } else {
        console.warn('⚠️ 未知的健康状态值:', status)
        row.healthStatus = 'unknown'
        ElMessage.warning(`健康检查完成，状态值异常: ${status}`)
      }
    } else {
      console.error('❌ 响应格式错误:', res.data)
      row.healthStatus = 'unknown'
      ElMessage.warning('健康检查完成，但响应格式不正确')
    }
  } catch (error) {
    console.error('❌ 健康检查失败:', error)
    row.healthStatus = 'unhealthy'
    ElMessage.error('检查失败: ' + error.message)
  }
}

const syncMetadata = async (row) => {
  try {
    await axios.post(`/api/data-analysis/datasources/${row.id}/sync`)
    ElMessage.success('同步任务已启动,请稍候...')
  } catch (error) {
    ElMessage.error('同步失败: ' + error.message)
  }
}

const viewTables = async (row) => {
  currentDatasource.value = row
  loadingTables.value = true
  tablesDialogVisible.value = true
  try {
    const res = await axios.get(`/api/data-analysis/datasources/${row.id}/tables/info`)
    tables.value = res.data.data || []
  } catch (error) {
    ElMessage.error('加载表列表失败: ' + error.message)
  } finally {
    loadingTables.value = false
  }
}

const viewColumns = async (tableName) => {
  currentTableName.value = tableName
  loadingColumns.value = true
  columnsDialogVisible.value = true
  try {
    const res = await axios.get(`/api/data-analysis/datasources/${currentDatasource.value.id}/tables/${tableName}/schema`)
    tableColumns.value = res.data.data?.columns || []
  } catch (error) {
    ElMessage.error('加载字段信息失败: ' + error.message)
  } finally {
    loadingColumns.value = false
  }
}

const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  return num.toLocaleString()
}

const queryTable = (tableInfo) => {
  tablesDialogVisible.value = false
  // 生成智能提示文本
  const queryHint = tableInfo.tableComment 
    ? `查询${tableInfo.tableComment}的数据` 
    : `查询${tableInfo.tableName}的数据`
  
  // 跳转到智能查询页面，并传递数据源和表信息
  router.push({
    path: '/data-query',
    query: {
      datasourceId: currentDatasource.value.id,
      datasourceName: currentDatasource.value.name,
      tableName: tableInfo.tableName,
      tableComment: tableInfo.tableComment || '',
      queryHint: queryHint
    }
  })
  ElMessage.success(`已跳转到智能查询，可以直接使用"${tableInfo.tableComment || tableInfo.tableName}"来查询`)
}

const getTypeTagType = (type) => {
  const map = {
    'mysql': 'success',
    'elasticsearch': 'warning',
    'mongodb': 'info'
  }
  return map[type] || 'info'
}

const getHealthTagType = (status) => {
  const map = {
    'healthy': 'success',
    'unhealthy': 'danger',
    'unknown': 'info'
  }
  return map[status] || 'info'
}

const getHealthText = (status) => {
  const map = {
    'healthy': '健康',
    'unhealthy': '异常',
    'unknown': '未知'
  }
  return map[status] || '未知'
}
</script>

<style scoped src="@/styles/datasource-manage.css"></style>
