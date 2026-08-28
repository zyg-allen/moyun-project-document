<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="80%"
    class="audit-detail-dialog"
    top="6vh"
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

        <!-- 描述（bizDetail 中已有摘要/描述字段时隐藏，避免重复展示） -->
        <div v-if="task.description && !hasSummaryInBlocks" class="desc-block">
          <div class="desc-label">描述/摘要</div>
          <div class="desc-text">{{ task.description }}</div>
        </div>

        <!-- 业务详情：元数据短字段固定两列表格 -->
        <div v-if="metaFields.length > 0" class="biz-block">
          <div class="biz-label">基本信息</div>
          <el-descriptions :column="2" border size="small" class="meta-desc">
            <el-descriptions-item
              v-for="item in metaFields"
              :key="item.key"
              :label="item.label"
            >
              <el-image
                v-if="item.type === 'image'"
                :src="item.value"
                fit="cover"
                style="width: 80px; height: 80px; border-radius: 4px;"
                :preview-src-list="[item.value]"
                :preview-teleported="true"
              />
              <el-link v-else-if="item.type === 'link'" type="primary" :href="item.value" target="_blank" class="link-text">{{ item.value }}</el-link>
              <span v-else>{{ item.value || '-' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 大块内容块：封面 / 内容预览 / 摘要 / 图片证据，各自独占整行，顺序固定 -->
        <div v-for="block in contentBlocks" :key="block.key" class="content-block">
          <div class="content-label">{{ block.label }}</div>
          <!-- 富文本内容 -->
          <div
            v-if="block.type === 'html'"
            class="html-content content-panel"
            v-html="block.value"
            @click="handleContentClick"
          ></div>
          <!-- 多图证据 -->
          <div v-else-if="block.type === 'images'" class="image-list content-panel">
            <el-image
              v-for="(img, i) in block.value"
              :key="i"
              :src="img"
              fit="cover"
              class="image-item"
              :preview-src-list="block.value"
              :initial-index="i"
              :preview-teleported="true"
            />
          </div>
          <!-- 封面/证件照等单图 -->
          <div v-else-if="block.type === 'image'" class="content-panel">
            <el-image
              :src="block.value"
              fit="cover"
              class="cover-image"
              :preview-src-list="[block.value]"
              :preview-teleported="true"
            />
          </div>
          <!-- 摘要/描述等长文本 -->
          <div v-else class="desc-text content-panel">{{ block.value }}</div>
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

    <!-- 富文本内图片点击放大预览 -->
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewList"
      :initial-index="0"
      teleported
      @close="previewVisible = false"
    />
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
const previewVisible = ref(false)
const previewList = ref([])

/** 富文本内容内图片点击放大预览 */
function handleContentClick(e) {
  if (e.target?.tagName === 'IMG' && e.target.src) {
    // 收集该富文本内全部图片，支持左右切换
    const container = e.target.closest('.html-content')
    const imgs = container ? [...container.querySelectorAll('img')] : [e.target]
    previewList.value = imgs.map(i => i.src).filter(Boolean)
    previewVisible.value = true
  }
}

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

/** 元数据短字段：进入固定两列表格，位置规整 */
const metaFields = computed(() =>
  bizDetailList.value.filter(i => !isContentBlock(i))
)

/** 大块内容块：独占整行，按固定顺序排列（封面 → 内容 → 摘要 → 图片证据） */
const CONTENT_ORDER = ['cover', 'idCardFront', 'idCardBack', 'content', 'excerpt', 'description', 'images']
const contentBlocks = computed(() =>
  bizDetailList.value
    .filter(i => isContentBlock(i))
    .sort((a, b) => orderOf(a) - orderOf(b))
)
function isContentBlock(item) {
  // 富文本、多图、长文本(span=2) 独立成块；封面/证件照大图也独立展示
  return item.type === 'html' || item.type === 'images' || item.span === 2 ||
    (item.type === 'image' && /cover|front|back/i.test(item.key))
}
/** bizDetail 中是否已含摘要/描述字段（避免与外层描述块重复展示） */
const hasSummaryInBlocks = computed(() =>
  contentBlocks.value.some(i => i.key === 'excerpt' || i.key === 'description')
)
function orderOf(item) {
  const idx = CONTENT_ORDER.indexOf(item.key)
  return idx === -1 ? 99 : idx
}

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
  // 图片证据数组：多图缩略图展示（占整行）
  if (Array.isArray(value) && value.length > 0 && value.every(v => typeof v === 'string' && /^https?:\/\//.test(v))) {
    return { key, label, value, type: 'images', span: 2 }
  }
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
  return { key, label, value: displayValue, type, span }
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
/* 内容区：限制最大高度，超长滚动，图片/文本不撑破弹框 */
.audit-detail-body {
  min-height: 200px;
  max-height: calc(100vh - 240px);
  overflow-y: auto;
  padding-right: 6px;
}
.meta-block { margin-bottom: 16px; }
.title-block, .desc-block, .biz-block { margin-bottom: 16px; }
.title-label, .desc-label, .biz-label, .content-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 600;
}
/* 基本信息表：label 列固定宽度，两列对齐不漂移 */
.meta-desc :deep(.el-descriptions__label) {
  width: 110px;
  min-width: 110px;
}
/* 大块内容块：独占整行，块间留白 */
.content-block { margin-bottom: 16px; }
.content-panel {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px 14px;
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
/* 富文本内容预览：图片/视频限宽，不超出弹框；兼容常见富文本元素 */
.html-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  word-break: break-word;
  max-height: 420px;
  overflow-y: auto;
}
/* 图片：强制限宽（!important 覆盖编辑器内联 style 的固定宽），点击可放大 */
.html-content :deep(img) {
  max-width: 100% !important;
  width: auto !important;
  height: auto !important;
  border-radius: 4px;
  cursor: zoom-in;
  display: inline-block;
}
.html-content :deep(video) { max-width: 100%; border-radius: 4px; }
.html-content :deep(p) { margin: 0 0 8px; word-break: break-word; }
.html-content :deep(h1), .html-content :deep(h2), .html-content :deep(h3),
.html-content :deep(h4), .html-content :deep(h5), .html-content :deep(h6) {
  margin: 12px 0 8px;
  color: #303133;
  line-height: 1.4;
}
.html-content :deep(a) { color: #409eff; text-decoration: none; }
.html-content :deep(a:hover) { text-decoration: underline; }
.html-content :deep(ul), .html-content :deep(ol) { padding-left: 20px; margin: 0 0 8px; }
.html-content :deep(li) { margin: 2px 0; }
.html-content :deep(blockquote) {
  margin: 0 0 8px;
  padding: 8px 12px;
  border-left: 3px solid #dcdfe6;
  background: #f5f7fa;
  color: #606266;
}
.html-content :deep(pre) {
  background: #f5f7fa;
  padding: 10px 12px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
  margin: 0 0 8px;
}
.html-content :deep(code) {
  background: #f0f2f5;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 12px;
}
.html-content :deep(table) { border-collapse: collapse; width: 100%; margin: 0 0 8px; }
.html-content :deep(td), .html-content :deep(th) {
  border: 1px solid #ebeef5;
  padding: 6px 10px;
  font-size: 12px;
}
.html-content :deep(hr) { border: none; border-top: 1px solid #ebeef5; margin: 12px 0; }
/* 图片证据：多图缩略图墙 */
.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.image-item {
  width: 96px;
  height: 96px;
  border-radius: 4px;
  flex-shrink: 0;
}
/* 封面/证件照：中等尺寸展示，点击放大 */
.cover-image {
  width: 200px;
  height: 130px;
  border-radius: 4px;
}
.link-text { word-break: break-all; }
/* descriptions 单元格长内容强制换行 */
.biz-block :deep(.el-descriptions__body .el-descriptions__content) {
  word-break: break-all;
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

<!-- 非 scoped：dialog 为 append-to-body，需全局样式控制最大宽度 -->
<style>
.audit-detail-dialog { max-width: 1100px; }
@media (max-width: 768px) {
  .audit-detail-dialog { width: 94% !important; }
}
</style>
