<template>
  <div class="dictionary-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-book-open"></i>
      </div>
      <div class="header-content">
        <h2>领域词典</h2>
        <span class="item-count">共 {{ filteredList.length }} 个词典</span>
      </div>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="搜索关键词/同义词..." prefix-icon="Search" clearable style="width: 200px" />
        <el-select v-model="filterGlobal" @change="loadList" placeholder="类型" clearable style="width: 120px">
          <el-option label="全局词典" value="true"></el-option>
          <el-option label="专业词典" value="false"></el-option>
        </el-select>
        <el-select v-model="filterCategory" @change="loadList" placeholder="分类" clearable style="width: 120px">
          <el-option label="硬件" value="硬件"></el-option>
          <el-option label="技术" value="技术"></el-option>
          <el-option label="AI" value="AI"></el-option>
          <el-option label="运维" value="运维"></el-option>
          <el-option label="安全" value="安全"></el-option>
        </el-select>
        <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
          <i class="fa-solid fa-plus"></i> 新建词典
        </el-button>
      </div>
    </div>

    <!-- 内容容器 -->
    <div class="content-container">

      <!-- 词典卡片列表 -->
      <div class="dictionary-grid" v-loading="loading">
      <TransitionGroup name="card-list">
        <div v-for="dict in paginatedList" :key="dict.id" class="dictionary-card">
          <!-- 卡片顶部装饰条 -->
          <div class="card-accent" :class="{ active: dict.enabled }"></div>
          
          <!-- 卡片主体内容 -->
          <div class="card-content">
            <!-- 头部：图标 + 标题 + 状态 -->
            <div class="card-top">
              <div class="card-icon" :class="{ active: dict.enabled }">
                <i class="fa-solid fa-book-open"></i>
              </div>
              <div class="card-title-area">
                <div class="card-title-row">
                  <span class="name">{{ dict.keyword }}</span>
                  <el-tag :type="dict.isGlobal ? 'success' : 'warning'" size="small" effect="plain" round>
                    {{ dict.isGlobal ? '全局' : '专业' }}
                  </el-tag>
                  <el-tag :type="dict.enabled ? 'success' : 'info'" size="small" effect="plain" round class="status-tag">
                    {{ dict.enabled ? '已启用' : '已禁用' }}
                  </el-tag>
                </div>
                <div class="desc">{{ dict.category }} · 优先级 {{ dict.priority }}</div>
              </div>
            </div>
            
            <!-- 相关词标签 -->
            <div class="related-terms">
              <el-tag 
                v-for="term in dict.relatedTerms.split(',')" 
                :key="term" 
                size="small" 
                effect="plain"
                class="term-tag"
              >
                {{ term }}
              </el-tag>
            </div>
            
            <!-- 描述 -->
            <div class="description" v-if="dict.description">
              <i class="fa-solid fa-circle-info"></i>
              {{ dict.description }}
            </div>

            <!-- 底部：元信息 + 操作按钮 -->
            <div class="card-bottom">
              <div class="card-meta">
                <span class="meta-item" v-if="dict.createTime">
                  <i class="fa-regular fa-clock"></i>
                  <span>{{ formatTime(dict.createTime) }}</span>
                </span>
              </div>
              <div class="card-actions" @click.stop>
                <el-tooltip content="编辑" placement="top" :show-after="200">
                  <button class="action-btn" @click="editDictionary(dict)">
                    <i class="fa-solid fa-pen-to-square"></i>
                  </button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top" :show-after="200">
                  <button class="action-btn danger" @click="deleteDictionary(dict.id)">
                    <i class="fa-solid fa-trash-can"></i>
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
        :total="filteredList.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editMode ? '编辑词典' : '新建词典'"
      width="600px"
    >
      <el-form :model="formData" label-width="100px">
        <el-form-item label="核心词" required>
          <el-input v-model="formData.keyword" placeholder="例如：服务器" />
        </el-form-item>
        
        <el-form-item label="相关词" required>
          <el-input
            v-model="formData.relatedTerms"
            type="textarea"
            :rows="3"
            placeholder="用逗号分隔，例如：cpu,gpu,内存,存储"
          />
        </el-form-item>
        
        <el-form-item label="分类">
          <el-select v-model="formData.category" placeholder="选择分类">
            <el-option label="硬件" value="硬件" />
            <el-option label="技术" value="技术" />
            <el-option label="AI" value="AI" />
            <el-option label="运维" value="运维" />
            <el-option label="安全" value="安全" />
            <el-option label="通用" value="general" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="词典类型">
          <el-radio-group v-model="formData.isGlobal">
            <el-radio :label="true">全局词典（默认对所有智能体生效）</el-radio>
            <el-radio :label="false">专业词典（需要智能体手动关联）</el-radio>
          </el-radio-group>
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            全局词典会自动应用到所有智能体，专业词典需要在智能体管理页面手动关联
          </div>
        </el-form-item>
        
        <el-form-item label="优先级">
          <el-input-number v-model="formData.priority" :min="0" :max="100" />
          <div style="font-size: 12px; color: #909399; margin-top: 5px;">
            数字越大优先级越高
          </div>
        </el-form-item>
        
        <el-form-item label="说明">
          <el-input v-model="formData.description" placeholder="词典用途说明" />
        </el-form-item>
        
        <el-form-item label="启用">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ editMode ? '更新' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const dictionaryList = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const submitting = ref(false)
const editMode = ref(false)
const searchKeyword = ref('')
const filterGlobal = ref('')
const filterCategory = ref('')

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)

const formData = ref({
  id: null,
  keyword: '',
  relatedTerms: '',
  category: 'general',
  isGlobal: true,
  description: '',
  enabled: true,
  priority: 0
})

// 加载词典列表
const loadList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/domain-dictionary/list')
    if (response.data.success) {
      dictionaryList.value = response.data.data || []
    }
  } catch (error) {
    console.error('加载词典列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 过滤列表
const filteredList = computed(() => {
  let list = dictionaryList.value
  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(item =>
      item.keyword?.toLowerCase().includes(keyword) ||
      item.relatedTerms?.toLowerCase().includes(keyword) ||
      item.description?.toLowerCase().includes(keyword)
    )
  }
  // 类型筛选
  if (filterGlobal.value) {
    const isGlobal = filterGlobal.value === 'true'
    list = list.filter(item => item.isGlobal === isGlobal)
  }
  // 分类筛选
  if (filterCategory.value) {
    list = list.filter(item => item.category === filterCategory.value)
  }
  return list
})

// 分页后的列表
const paginatedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredList.value.slice(start, end)
})

// 编辑词典
const editDictionary = (dictionary) => {
  editMode.value = true
  formData.value = { ...dictionary }
  showCreateDialog.value = true
}

// 删除词典
const deleteDictionary = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个词典吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await axios.delete(`/api/domain-dictionary/delete/${id}`)
    if (response.data.success) {
      ElMessage.success('删除成功')
      await loadList()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const submitForm = async () => {
  if (!formData.value.keyword || !formData.value.relatedTerms) {
    ElMessage.warning('请填写核心词和相关词')
    return
  }
  
  submitting.value = true
  try {
    const url = editMode.value ? '/api/domain-dictionary/update' : '/api/domain-dictionary/create'
    const method = editMode.value ? 'put' : 'post'
    
    const response = await axios[method](url, formData.value)
    
    if (response.data.success) {
      ElMessage.success(editMode.value ? '更新成功' : '创建成功')
      showCreateDialog.value = false
      resetForm()
      await loadList()
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: null,
    keyword: '',
    relatedTerms: '',
    category: 'general',
    isGlobal: true,
    description: '',
    enabled: true,
    priority: 0
  }
  editMode.value = false
}

// 重新加载到内存
const reload = async () => {
  try {
    const response = await axios.post('/api/domain-dictionary/reload')
    if (response.data.success) {
      ElMessage.success(response.data.data)
    }
  } catch (error) {
    console.error('重新加载失败:', error)
    ElMessage.error('重新加载失败')
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

onMounted(() => {
  loadList()
})
</script>

<style scoped src="@/styles/domain-dictionary-manage.css"></style>
