<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="780px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div v-loading="loading" class="audit-detail-body">
      <template v-if="task">
        <!-- 基础信息 -->
        <el-descriptions :column="2" border size="small" class="meta-block">
          <el-descriptions-item label="任务类型">
            <el-tag size="small" :type="taskTypeTagType(task.taskType)">{{ task.taskTypeLabel || task.taskType }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag size="small" :type="priorityTagType(task.priority)" effect="plain">{{ task.priorityLabel || task.priority }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusTagType(task.status)">{{ task.statusLabel || task.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交人">{{ task.submitterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ task.submitTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人" v-if="task.auditorName">{{ task.auditorName }}</el-descriptions-item>
          <el-descriptions-item label="处理时间" v-if="task.auditTime">{{ task.auditTime }}</el-descriptions-item>
          <el-descriptions-item label="业务ID" v-if="task.bizId">{{ task.bizId }}</el-descriptions-item>
        </el-descriptions>

        <!-- 标题 -->
        <div class="title-block">
          <div class="title-label">标题</div>
          <div class="title-text">{{ task.title }}</div>
        </div>

        <!-- 描述 -->
        <div v-if="task.description" class="desc-block">
          <div class="desc-label">描述/摘要</div>
          <div class="desc-text">{{ task.description }}</div>
        </div>

        <!-- 业务详情 bizDetail -->
        <div v-if="bizDetailList.length > 0" class="biz-block">
          <div class="biz-label">业务详情</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item
              v-for="item in bizDetailList"
              :key="item.label"
              :label="item.label"
              :span="item.span || 1"
            >
              <span v-if="item.type === 'html'" v-html="item.value"></span>
              <el-image
                v-else-if="item.type === 'image'"
                :src="item.value"
                fit="cover"
                style="width: 96px; height: 96px; border-radius: 4px;"
                :preview-src-list="[item.value]"
                :preview-teleported="true"
              />
              <el-link v-else-if="item.type === 'link'" type="primary" :href="item.value" target="_blank">{{ item.value }}</el-link>
              <span v-else>{{ item.value || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 已处理：审核结果 -->
        <template v-if="!isPending">
          <el-divider content-position="left">审核结果</el-divider>
          <div class="audit-result">
            <el-tag :type="statusTagType(task.status)">{{ task.statusLabel || task.status }}</el-tag>
            <div v-if="task.auditOpinion" class="audit-remark">{{ task.auditOpinion }}</div>
            <div v-else class="audit-remark audit-remark-empty">（无审核意见）</div>
          </div>
        </template>

        <!-- 待处理：审核意见输入 -->
        <template v-if="isPending && canHandle">
          <el-divider content-position="left">审核意见</el-divider>
          <el-input
            v-model="auditOpinion"
            type="textarea"
            :rows="4"
            placeholder="请输入审核意见（驳回时必填，通过时选填）"
          />
        </template>
      </template>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="task && task.routePath" @click="goBizPage">
          <el-icon><Link /></el-icon> 查看原业务
        </el-button>
        <el-button @click="visible = false">关闭</el-button>
        <template v-if="isPending && canHandle">
          <el-button type="danger" :loading="rejectLoading" @click="handleReject">驳回</el-button>
          <el-button type="primary" :loading="approveLoading" @click="handleApprove">同意</el-button>
        </template>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Link } from '@element-plus/icons-vue'
import { getAuditTask, handleAuditTask } from '@/api/system/auditTask'

const props = defineProps({
  // 控制弹窗显示
  modelValue: { type: Boolean, default: false },
  // 审核任务ID
  taskId: { type: [Number, String], default: null },
  // 是否允许审核操作（待办列表中打开=true，已办/只读=false）
  canHandle: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'success'])

const router = useRouter()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const loading = ref(false)
const task = ref(null)
const auditOpinion = ref('')
const approveLoading = ref(false)
const rejectLoading = ref(false)

const dialogTitle = computed(() => {
  if (!task.value) return '审核详情'
  return `审核详情 - ${task.value.taskTypeLabel || task.value.taskType}`
})

const isPending = computed(() => task.value?.status === 'pending')

/** bizDetail Map 转为列表展示（按字段名友好化） */
const bizDetailList = computed(() => {
  const detail = task.value?.bizDetail
  if (!detail || typeof detail !== 'object') return []
  const result = []
  for (const [key, value] of Object.entries(detail)) {
    if (value === null || value === undefined || value === '') continue
    const item = formatBizItem(key, value)
    result.push(item)
  }
  return result
})

/** 字段名友好映射 + 特殊渲染 */
function formatBizItem(key, value) {
  const labelMap = {
    authorName: '作者', authorNickname: '作者', categoryName: '分类',
    cover: '封面图', content: '内容预览', excerpt: '摘要',
    createTime: '创建时间', username: '用户名', realName: '真实姓名',
    idCard: '身份证号', idCardFront: '身份证正面', idCardBack: '身份证背面',
    certificationName: '认证名称', userId: '用户ID',
    reportType: '举报类型', targetType: '目标类型', targetUrl: '目标链接',
    images: '图片证据', contact: '联系方式', ip: 'IP地址',
    feedbackType: '反馈类型', subject: '主题', description: '描述',
    handleResult: '处理结果', handler: '处理人',
    columnId: '专栏ID', articleCount: '文章数',
    categoryName: '分类', tags: '标签'
  }
  let label = labelMap[key] || key
  let type = 'text'
  let span = 1
  // 图片字段
  if (/image|cover|front|back|idCard/i.test(key) && typeof value === 'string' && /^https?:\/\//.test(value)) {
    type = 'image'
  }
  // 链接字段
  if (key === 'targetUrl' || key === 'url') {
    type = 'link'
  }
  // HTML 内容
  if (key === 'content' && typeof value === 'string' && /<[a-z][\s\S]*>/i.test(value)) {
    type = 'html'
    span = 2
  }
  // 长文本
  if (typeof value === 'string' && value.length > 60) {
    span = 2
  }
  // 数组转字符串
  let displayValue = value
  if (Array.isArray(value)) {
    displayValue = value.join('、')
  } else if (typeof value === 'object') {
    displayValue = JSON.stringify(value)
  }
  return { label, value: displayValue, type, span }
}

/** 任务类型 → tag type */
function taskTypeTagType(type) {
  const map = {
    article: 'primary', column: 'success', topic: 'warning',
    interview_exp: 'danger', interview_comment: 'info',
    certification: '', feedback: 'warning', report: 'danger'
  }
  return map[type] || 'info'
}
function statusTagType(status) {
  return { pending: 'warning', approved: 'success', rejected: 'danger' }[status] || 'info'
}
function priorityTagType(p) {
  return { high: 'danger', medium: 'warning', low: 'info' }[p] || 'info'
}

/** 加载详情 */
async function loadDetail(id) {
  if (!id) return
  loading.value = true
  try {
    const res = await getAuditTask(id)
    task.value = res.data || null
    auditOpinion.value = ''
  } catch (e) {
    ElMessage.error('加载详情失败')
    task.value = null
  } finally {
    loading.value = false
  }
}

/** 同意 */
async function handleApprove() {
  if (!task.value) return
  approveLoading.value = true
  try {
    await handleAuditTask({
      taskId: task.value.id,
      action: 'approve',
      auditOpinion: auditOpinion.value || '',
      notifyUser: true
    })
    ElMessage.success('已同意')
    emit('success', { action: 'approve', task: task.value })
    visible.value = false
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    approveLoading.value = false
  }
}

/** 驳回 */
async function handleReject() {
  if (!task.value) return
  if (!auditOpinion.value.trim()) {
    ElMessage.warning('驳回时审核意见原因必填')
    return
  }
  rejectLoading.value = true
  try {
    await handleAuditTask({
      taskId: task.value.id,
      action: 'reject',
      auditOpinion: auditOpinion.value,
      notifyUser: true
    })
    ElMessage.success('已驳回')
    emit('success', { action: 'reject', task: task.value })
    visible.value = false
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    rejectLoading.value = false
  }
}

/** 跳转到原业务管理页 */
function goBizPage() {
  if (!task.value?.routePath) return
  visible.value = false
  router.push(task.value.routePath).catch(() => {
    ElMessage.warning('目标页面不可达：' + task.value.routePath)
  })
}

function handleClose() {
  task.value = null
  auditOpinion.value = ''
}

watch(() => props.modelValue, (v) => {
  if (v && props.taskId) {
    loadDetail(props.taskId)
  } else if (!v) {
    task.value = null
  }
})

watch(() => props.taskId, (v) => {
  if (v && visible.value) {
    loadDetail(v)
  }
})
</script>

<style scoped>
.audit-detail-body { min-height: 200px; }
.meta-block { margin-bottom: 16px; }
.title-block, .desc-block, .biz-block { margin-bottom: 16px; }
.title-label, .desc-label, .biz-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 600;
}
.title-text {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
  word-break: break-all;
}
.desc-text {
  font-size: 13px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
  background: #f5f7fa;
  padding: 10px 12px;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
}
.audit-result {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.audit-remark {
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  white-space: pre-wrap;
}
.audit-remark-empty { color: #c0c4cc; }
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
