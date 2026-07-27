<template>
  <div class="knowledge-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-book"></i>
      </div>
      <div class="header-content">
        <h2>知识库管理</h2>
        <span class="item-count">共 {{ filteredKnowledge.length }} 个知识库</span>
      </div>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="搜索文件名..." prefix-icon="Search" clearable style="width: 200px" />
        <el-select v-model="searchStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="待配置" value="pending" />
          <el-option label="已配置" value="configured" />
          <el-option label="处理中" value="processing" />
          <el-option label="已完成" value="completed" />
          <el-option label="失败" value="failed" />
        </el-select>
        <el-button @click="showStatsDialog = true" class="stats-btn">
          <i class="fa-solid fa-chart-bar"></i> 统计
        </el-button>
        <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
          <i class="fa-solid fa-plus"></i> 新增知识库
        </el-button>
      </div>
    </div>

    <!-- 内容容器 -->
    <div class="content-container">

      <!-- 知识库卡片列表 -->
      <div class="knowledge-cards" v-loading="loading">
      <TransitionGroup name="card-list">
        <div 
          v-for="kb in paginatedKnowledge" 
          :key="kb.id" 
          class="knowledge-card"
        >
          <!-- 卡片顶部装饰条 -->
          <div class="card-accent" :class="{ active: kb.processingStatus === 'completed' }"></div>
          
          <!-- 卡片主体内容 -->
          <div class="card-content">
            <!-- 头部：图标 + 标题 + 状态 -->
            <div class="card-top">
              <div class="card-icon" :class="{ active: kb.processingStatus === 'completed' }">
                <i :class="getFileIcon(kb.fileType)"></i>
              </div>
              <div class="card-title-area">
                <div class="card-title-row">
                  <span class="name" :title="kb.fileName">{{ kb.fileName }}</span>
                </div>
                <div class="desc">{{ kb.fileType.toUpperCase() }} · {{ kb.fileSize }}</div>
              </div>
            </div>
            
            <!-- 分片数量和解析方式 -->
            <div v-if="kb.segmentCount || kb.parseMethod" class="kb-meta-tags">
              <span v-if="kb.segmentCount" class="file-tag">
                <i class="fa-solid fa-layer-group"></i>
                {{ kb.segmentCount }} 段
              </span>
              <span v-if="kb.parseMethod" class="file-tag parse-method" :class="getParseMethodClass(kb.parseMethod)">
                <i :class="getParseMethodIcon(kb.parseMethod)"></i>
                {{ kb.parseMethod }}
              </span>
            </div>
            
            <!-- 描述 -->
            <div v-if="kb.description" class="kb-description">
              {{ kb.description }}
            </div>

            <!-- 状态区域 -->
            <div class="status-area">
              <!-- 处理中显示进度条 -->
              <div v-if="kb.processingStatus === 'processing'" class="progress-wrapper">
                <div class="progress-header">
                  <span class="progress-label">
                    <i class="fa-solid fa-spinner fa-spin"></i>
                    {{ getProgressText(kb) }}
                  </span>
                  <span class="progress-percentage">{{ Math.round(getProgress(kb)) }}%</span>
                </div>
                <el-progress
                  :percentage="Math.round(getProgress(kb))"
                  :stroke-width="6"
                  :show-text="false"
                  color="#0ea5e9"
                />
              </div>
              
              <!-- 非处理中显示状态标签 -->
              <el-tag
                v-else
                :type="getProcessingStatusType(kb)"
                effect="plain"
                size="small"
                round
              >
                <i v-if="kb.processingStatus === 'pending'" class="fa-solid fa-clock"></i>
                <i v-else-if="kb.processingStatus === 'configured'" class="fa-solid fa-gear"></i>
                <i v-else-if="kb.processingStatus === 'completed'" class="fa-solid fa-check"></i>
                <i v-else-if="kb.processingStatus === 'failed'" class="fa-solid fa-times"></i>
                {{ getProcessingStatusText(kb) }}
              </el-tag>
            </div>

            <!-- 底部：元信息 + 操作按钮 -->
            <div class="card-bottom">
              <div class="card-meta">
                <span class="meta-item">
                  <i class="fa-regular fa-clock"></i>
                  <span>{{ formatTime(kb.uploadTime) }}</span>
                </span>
              </div>
              <div class="card-actions" @click.stop>
                <!-- 待配置状态：显示配置按钮 -->
                <el-tooltip v-if="kb.processingStatus === 'pending'" content="配置" placement="top" :show-after="200">
                  <button class="action-btn primary" @click="openConfigForKnowledge(kb)">
                    <i class="fa-solid fa-gear"></i>
                  </button>
                </el-tooltip>
                
                <!-- 已配置未处理：显示开始处理按钮 -->
                <el-tooltip v-if="kb.processingStatus === 'configured'" content="开始处理" placement="top" :show-after="200">
                  <button class="action-btn primary" @click="startProcessing(kb.id)">
                    <i class="fa-solid fa-play"></i>
                  </button>
                </el-tooltip>
                
                <!-- 预览按钮 -->
                <el-tooltip v-if="isCompleted(kb)" content="预览" placement="top" :show-after="200">
                  <button class="action-btn" @click="previewFile(kb)">
                    <i class="fa-solid fa-eye"></i>
                  </button>
                </el-tooltip>
                
                <!-- 详情按钮 -->
                <el-tooltip v-if="isCompleted(kb)" content="详情" placement="top" :show-after="200">
                  <button class="action-btn" @click="viewDetail(kb)">
                    <i class="fa-solid fa-list"></i>
                  </button>
                </el-tooltip>
                
                <!-- 检索测试按钮 -->
                <el-tooltip v-if="isCompleted(kb)" content="检索测试" placement="top" :show-after="200">
                  <button class="action-btn" @click="openRetrievalTest(kb)">
                    <i class="fa-solid fa-magnifying-glass"></i>
                  </button>
                </el-tooltip>
                
                <!-- 编辑按钮 -->
                <el-tooltip content="编辑" placement="top" :show-after="200">
                  <button class="action-btn" @click="editKnowledge(kb)">
                    <i class="fa-solid fa-pen"></i>
                  </button>
                </el-tooltip>
                
                <!-- 重试按钮 -->
                <el-tooltip v-if="isFailed(kb)" content="重试" placement="top" :show-after="200">
                  <button class="action-btn" @click="reprocess(kb)">
                    <i class="fa-solid fa-rotate-right"></i>
                  </button>
                </el-tooltip>
                
                <!-- 删除按钮 -->
                <el-tooltip content="删除" placement="top" :show-after="200">
                  <button class="action-btn danger" @click="deleteKnowledge(kb.id)">
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
        :total="filteredKnowledge.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传文件"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-upload
        ref="uploadRef"
        class="upload-demo"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv"
      >
        <i class="fa-solid fa-cloud-arrow-up" style="font-size: 60px; color: #409eff;"></i>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、Word(doc/docx)、Excel(xls/xlsx)、PowerPoint(ppt/pptx)、TXT、MD、CSV 格式，单个文件不超过 50MB
          </div>
        </template>
      </el-upload>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelUpload">取消</el-button>
          <el-button
            type="primary"
            @click="submitUpload"
            :loading="uploading"
            :disabled="!selectedFile"
          >
            确认上传
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="showDetailDialog"
      title="知识库详情"
      width="80%"
      top="5vh"
    >
      <div v-if="currentDetail">
        <h3>📊 基本信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ currentDetail.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ currentDetail.fileType }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ currentDetail.fileSize }}</el-descriptions-item>
          <el-descriptions-item label="分段数量">{{ currentDetail.segmentCount || '0' }}</el-descriptions-item>
          <el-descriptions-item label="向量维度">{{ currentDetail.vectorDimension || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="getStatusType(currentDetail.status)">
              {{ currentDetail.statusText }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatTime(currentDetail.uploadTime) }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px;" v-if="currentDetail.status === 2">
          <h3>📋 文档分片信息（共 {{ segments.length }} 个分片）</h3>
          <el-table
            :data="segments"
            style="width: 100%"
            max-height="400"
            v-loading="segmentsLoading"
          >
            <el-table-column prop="segmentIndex" label="分片索引" width="100" />
            <el-table-column prop="content" label="分片内容" min-width="300">
              <template #default="scope">
                <el-tooltip :content="scope.row.content" placement="top">
                  <div class="content-preview">{{ scope.row.content }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="contentLength" label="内容长度" width="100" />
            <el-table-column prop="embeddingId" label="嵌入ID" width="200">
              <template #default="scope">
                <el-tooltip :content="scope.row.embeddingId" placement="top">
                  <div class="embedding-id">{{ scope.row.embeddingId }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="vectorDimension" label="向量维度" width="100" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button
                  size="small"
                  type="primary"
                  @click="showContentDetail(scope.row)"
                  link
                >
                  <i class="fa-solid fa-file-lines"></i> 查看原文
                </el-button>
                <el-button
                  size="small"
                  @click="showVectorData(scope.row)"
                  v-if="scope.row.vectorData"
                  link
                >
                  <i class="fa-solid fa-cube"></i> 向量
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 向量数据对话框 -->
    <el-dialog
      v-model="showVectorDialog"
      title="向量数据"
      width="600px"
    >
      <div v-if="currentVector">
        <p><strong>分片索引：</strong>{{ currentVector.segmentIndex }}</p>
        <p><strong>嵌入ID：</strong>{{ currentVector.embeddingId }}</p>
        <p><strong>向量维度：</strong>{{ currentVector.vectorDimension }}</p>
        <p><strong>向量数据（前100个值）：</strong></p>
        <el-input
          type="textarea"
          :rows="10"
          :value="formatVectorData(currentVector.vectorData)"
          readonly
        />
      </div>
    </el-dialog>

    <!-- 原文内容对话框 -->
    <el-dialog
      v-model="showContentDialog"
      title="📄 分片原文内容"
      width="700px"
      class="content-detail-dialog"
    >
      <div v-if="currentContent" class="content-detail-wrapper">
        <!-- 分片信息头部 -->
        <div class="content-header">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-label">分片序号</span>
              <span class="info-value">第 {{ currentContent.segmentIndex + 1 }} 段</span>
            </div>
            <div class="info-item">
              <span class="info-label">内容长度</span>
              <span class="info-value">{{ currentContent.contentLength }} 字符</span>
            </div>
            <div class="info-item" v-if="currentContent.pageNumber">
              <span class="info-label">页码位置</span>
              <span class="info-value">第 {{ currentContent.pageNumber }} 页</span>
            </div>
            <div class="info-item" v-if="currentContent.vectorDimension">
              <span class="info-label">向量维度</span>
              <span class="info-value">{{ currentContent.vectorDimension }}</span>
            </div>
          </div>
        </div>
        
        <!-- 原文内容区域 -->
        <div class="content-body">
          <div class="content-label">
            <i class="fa-solid fa-align-left"></i> 原文内容
          </div>
          <div class="content-text">{{ currentContent.content }}</div>
        </div>
        
        <!-- 操作按钮 -->
        <div class="content-footer">
          <el-button @click="copyContent(currentContent.content)" type="primary">
            <i class="fa-regular fa-copy"></i> 复制内容
          </el-button>
          <el-button v-if="currentContent.vectorData" @click="showVectorData(currentContent)">
            <i class="fa-solid fa-cube"></i> 查看向量
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog
      v-model="showPreviewDialog"
      title="文件预览"
      width="900px"
      :close-on-click-modal="false"
      @close="handlePreviewDialogClose"
    >
      <div v-loading="isLoadingPreview" class="pdf-preview-wrapper">
         <!-- 预览工具栏 -->
         <div class="preview-toolbar" v-if="previewFileUrl">
           <div class="toolbar-left">
             <span style="font-weight: 500;">{{ previewFileName }}</span>
           </div>
           <div class="toolbar-center">
             <el-button-group size="small">
               <el-button @click="prevPage" :disabled="pdfCurrentPage <= 1">
                 <i class="fa-solid fa-chevron-left"></i>
               </el-button>
               <el-button disabled>
                 {{ pdfCurrentPage }} / {{ pdfTotalPages }}
               </el-button>
               <el-button @click="nextPage" :disabled="pdfCurrentPage >= pdfTotalPages">
                 <i class="fa-solid fa-chevron-right"></i>
               </el-button>
             </el-button-group>
           </div>
           <div class="toolbar-right">
             <el-button size="small" @click="downloadFile">
               <i class="fa-solid fa-download"></i>
             </el-button>
           </div>
         </div>
         
         <!-- PDF 预览区域 -->
         <div class="pdf-viewer">
           <div class="pdf-container" v-if="previewFileUrl">
             <vue-pdf-embed
               :source="previewFileUrl"
               :page="pdfCurrentPage"
               @loaded="onPdfLoaded"
               @loading-failed="onPreviewError"
               class="pdf-canvas"
             />
           </div>
           <div v-else class="empty-state">
             <el-empty description="无法预览此文件，请下载后查看" />
           </div>
         </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="showPreviewDialog = false" size="large">
            关闭
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增知识库对话框（新的一体化流程） -->
    <CreateKnowledgeDialog
      v-model="showCreateDialog"
      @success="handleCreateSuccess"
    />

    <!-- 配置对话框（用于已上传但待配置的文件） -->
    <KnowledgeConfigDialog
      v-model="showConfigDialog"
      :file-info="pendingFileInfo || {}"
      :templates="configTemplates"
      @success="handleConfigSuccess"
    />

    <!-- 编辑知识库对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑知识库"
      width="500px"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="文件名">
          <el-input :value="editForm.fileName" disabled />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit" :loading="editSaving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 检索测试对话框 -->
    <el-dialog
      v-model="showRetrievalTestDialog"
      title="检索测试"
      width="800px"
    >
      <div class="retrieval-test-container">
        <!-- 测试表单 -->
        <el-form :model="retrievalTestForm" label-width="100px">
          <el-form-item label="查询文本">
            <el-input
              v-model="retrievalTestForm.query"
              type="textarea"
              :rows="3"
              placeholder="请输入要检索的查询文本"
            />
          </el-form-item>
          <el-form-item label="检索模式">
            <el-select v-model="retrievalTestForm.retrievalMode" style="width: 200px">
              <el-option label="向量检索" value="vector" />
              <el-option label="关键词检索" value="keyword" />
              <el-option label="混合检索" value="hybrid" />
            </el-select>
          </el-form-item>
          <el-form-item label="Top K">
            <el-input-number v-model="retrievalTestForm.topK" :min="1" :max="20" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="executeRetrievalTest" :loading="retrievalTesting">
              <i class="fa-solid fa-play"></i> 开始检索
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 检索结果 -->
        <div v-if="retrievalTestResults.length > 0" class="retrieval-results">
          <h3><i class="fa-solid fa-list-check"></i> 检索结果（共 {{ retrievalTestResults.length }} 条）</h3>
          <div 
            v-for="(result, index) in retrievalTestResults" 
            :key="index"
            class="result-item"
          >
            <div class="result-header">
              <span class="result-rank">#{{ result.segmentIndex }}</span>
              <el-tag type="success" size="small">
                相似度: {{ (result.score * 100).toFixed(2) }}%
              </el-tag>
            </div>
            <div class="result-content">
              {{ result.content }}
            </div>
            <div v-if="result.metadata" class="result-metadata">
              <i class="fa-solid fa-info-circle"></i> {{ result.metadata }}
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty v-else-if="!retrievalTesting" description="请输入查询文本并开始检索" />
      </div>
    </el-dialog>

    <!-- 统计对话框 -->
    <el-dialog
      v-model="showStatsDialog"
      title="知识库统计"
      width="900px"
    >
      <div v-loading="statsLoading">
        <!-- 总体统计 -->
        <div class="stats-overview">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #0ea5e9 0%, #0284c7 100%);">
              <i class="fa-solid fa-book"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.total || 0 }}</div>
              <div class="stat-label">知识库总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #10b981 0%, #059669 100%);">
              <i class="fa-solid fa-check-circle"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.completed || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);">
              <i class="fa-solid fa-search"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalUsage || 0 }}</div>
              <div class="stat-label">总检索次数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);">
              <i class="fa-solid fa-bullseye"></i>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.avgHitRate || 0 }}%</div>
              <div class="stat-label">平均命中率</div>
            </div>
          </div>
        </div>

        <!-- 热门知识库 -->
        <div class="stats-section">
          <h3><i class="fa-solid fa-fire"></i> 热门知识库 Top 10</h3>
          <el-table :data="stats.topKnowledge || []" style="width: 100%">
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column prop="category" label="分组" width="120" />
            <el-table-column prop="usageCount" label="使用次数" width="100" align="center" />
            <el-table-column prop="hitCount" label="命中次数" width="100" align="center" />
            <el-table-column label="命中率" width="100" align="center">
              <template #default="scope">
                {{ calculateHitRate(scope.row) }}%
              </template>
            </el-table-column>
            <el-table-column prop="lastUsedTime" label="最后使用" width="160">
              <template #default="scope">
                {{ formatTime(scope.row.lastUsedTime) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, onUnmounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import KnowledgeConfigDialog from './KnowledgeConfigDialog.vue'
import CreateKnowledgeDialog from './CreateKnowledgeDialog.vue'
import VuePdfEmbed from 'vue-pdf-embed'

const knowledgeList = ref([])
const loading = ref(false)

// 搜索相关
const searchKeyword = ref('')
const searchStatus = ref('')

// 分页相关
const currentPage = ref(1)
const pageSize = ref(12)

// 编辑相关
const showEditDialog = ref(false)
const editForm = ref({
  id: null,
  fileName: '',
  description: ''
})
const editSaving = ref(false)

// 统计相关
const showStatsDialog = ref(false)
const statsLoading = ref(false)
const stats = ref({
  total: 0,
  completed: 0,
  totalUsage: 0,
  avgHitRate: 0,
  topKnowledge: []
})

// 检索测试相关
const showRetrievalTestDialog = ref(false)
const retrievalTesting = ref(false)
const currentTestKnowledge = ref(null)
const retrievalTestForm = ref({
  query: '',
  retrievalMode: 'vector',
  topK: 5
})
const retrievalTestResults = ref([])

// 过滤后的知识库列表
const filteredKnowledge = computed(() => {
  let result = knowledgeList.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(kb => kb.fileName?.toLowerCase().includes(keyword))
  }
  if (searchStatus.value) {
    result = result.filter(kb => kb.processingStatus === searchStatus.value)
  }
  return result
})

const showCreateDialog = ref(false) // 新增知识库对话框
const showUploadDialog = ref(false) // 保留旧的上传对话框（暂时兼容）
const showDetailDialog = ref(false)
const uploading = ref(false)
const selectedFile = ref(null)
const uploadRef = ref()
const currentDetail = ref(null)
const segments = ref([])
const segmentsLoading = ref(false)
const processingKnowledgeIds = ref(new Set()) // 正在处理的知识库ID
const pollingTimers = ref(new Map()) // 轮询定时器
const isUnmounted = ref(false) // 组件是否已卸载
const showVectorDialog = ref(false)
const currentVector = ref(null)
const showContentDialog = ref(false)
const currentContent = ref(null)
const showPreviewDialog = ref(false)
const previewFileUrl = ref('')
const previewFileName = ref('')
const isLoadingPreview = ref(false)
const pdfCurrentPage = ref(1)
const pdfTotalPages = ref(0)
const currentPreviewKnowledge = ref(null)
const textContent = ref('') // 文本文件内容

// 加载知识库列表
const loadKnowledgeList = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/knowledge/list')
    if (response.data.success) {
      // 后端返回的是 ListResponse 格式：{ list: [], total: n }
      const listData = response.data.data?.list || []
      knowledgeList.value = listData
      
      // 检查是否有处理中的知识库，自动开始轮询
      knowledgeList.value.forEach(knowledge => {
        if (knowledge.processingStatus === 'processing' || knowledge.status === 1) {
          // 检查是否已经在轮询中
          if (!processingKnowledgeIds.value.has(knowledge.id)) {
            console.log(`检测到处理中的知识库ID=${knowledge.id}，开始监控`)
            processingKnowledgeIds.value.add(knowledge.id)
            startPolling(knowledge.id)
          }
        }
      })
    } else {
      ElMessage.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 文件选择处理
const handleFileChange = (file) => {
  selectedFile.value = file.raw
}

// 取消上传
const cancelUpload = () => {
  showUploadDialog.value = false
  selectedFile.value = null
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 配置对话框相关
const showConfigDialog = ref(false)
const pendingFileInfo = ref(null)
const configTemplates = ref([])

// 提交上传（新流程：只上传不处理）
const submitUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  const formData = new FormData()
  formData.append('file', selectedFile.value)

  try {
    const response = await axios.post('/api/knowledge/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    if (response.data.success) {
      ElMessage.success('文件上传成功！')
      
      // 关闭上传弹窗
      showUploadDialog.value = false
      selectedFile.value = null
      if (uploadRef.value) {
        uploadRef.value.clearFiles()
      }
      
      // 保存文件信息和推荐模板
      pendingFileInfo.value = {
        knowledgeId: response.data.knowledgeId,
        fileName: response.data.fileName,
        fileType: response.data.fileType,
        fileSize: response.data.fileSize
      }
      configTemplates.value = response.data.recommendedTemplates || []
      
      // 显示配置对话框
      showConfigDialog.value = true
      
      // 立即刷新列表（显示待配置状态）
      await loadKnowledgeList()
    } else {
      ElMessage.error(response.data.message || '上传失败')
    }
  } catch (error) {
    console.error('上传失败:', error)
    ElMessage.error('上传失败: ' + (error.response?.data?.message || error.message))
  } finally {
    uploading.value = false
  }
}

// 创建知识库成功回调（新流程）
const handleCreateSuccess = async (data) => {
  ElMessage.success('知识库创建成功，正在处理...')
  
  // 添加到处理中列表
  processingKnowledgeIds.value.add(data.knowledgeId)
  
  // 刷新列表
  await loadKnowledgeList()
  
  // 开始轮询处理进度
  startPolling(data.knowledgeId)
}

// 配置成功后的回调（旧流程，用于待配置文件）
const handleConfigSuccess = async (data) => {
  ElMessage.success('配置成功，开始处理...')
  
  // 添加到处理中列表
  processingKnowledgeIds.value.add(data.knowledgeId)
  
  // 刷新列表
  await loadKnowledgeList()
  
  // 开始轮询处理进度
  startPolling(data.knowledgeId)
}

// 开始轮询处理进度
const startPolling = (knowledgeId) => {
  // 如果已经有定时器，先清除
  if (pollingTimers.value.has(knowledgeId)) {
    clearInterval(pollingTimers.value.get(knowledgeId))
  }
  
  // 初始化进度
  processingProgress.value.set(knowledgeId, {
    progress: 0,
    message: '开始处理',
    startTime: Date.now()
  })
  
  // 创建新的定时器，每1秒查询一次
  const timer = setInterval(async () => {
    try {
      console.log(`[轮询] 查询知识库ID=${knowledgeId}的进度...`)
      
      // 先查询实时进度
      const progressResponse = await axios.get(`/api/knowledge/progress/${knowledgeId}`)
      if (progressResponse.data.success && progressResponse.data.data) {
        const progressData = progressResponse.data.data
        console.log(`[轮询] ID=${knowledgeId}, 进度=${progressData.progress}%, 消息=${progressData.message}`)
        
        // 更新进度信息
        processingProgress.value.set(knowledgeId, {
          progress: progressData.progress,
          message: progressData.message,
          currentStep: progressData.currentStep,
          startTime: processingProgress.value.get(knowledgeId)?.startTime || Date.now()
        })
      }
      
      // 再查询知识库状态
      const statusResponse = await axios.get(`/api/knowledge/${knowledgeId}`)
      if (statusResponse.data.success) {
        const knowledge = statusResponse.data.data
        console.log(`[轮询] ID=${knowledgeId}, status=${knowledge.status}, processingStatus=${knowledge.processingStatus}`)
        
        // 更新列表中的状态
        const index = knowledgeList.value.findIndex(k => k.id === knowledgeId)
        if (index !== -1) {
          knowledgeList.value[index] = knowledge
        }
        
        // 如果处理完成（成功或失败），停止轮询
        if (knowledge.status === 2 || knowledge.status === 3 || 
            knowledge.processingStatus === 'completed' || knowledge.processingStatus === 'failed') {
          console.log(`[轮询] ID=${knowledgeId}处理完成，停止轮询`)
          
          // 设置进度为100%（如果是成功）
          if (knowledge.status === 2 || knowledge.processingStatus === 'completed') {
            // 立即停止轮询，防止重复触发
            clearInterval(timer)
            pollingTimers.value.delete(knowledgeId)
            
            processingProgress.value.set(knowledgeId, {
              progress: 100,
              message: '处理完成',
              currentStep: '完成'
            })
            
            // 只有组件未卸载时才显示消息
            if (!isUnmounted.value) {
              ElMessage.success(`《${knowledge.fileName}》处理完成！`)
            }
            
            // 延迟清理进度显示，让用户看到100%
            setTimeout(() => {
              processingKnowledgeIds.value.delete(knowledgeId)
              processingProgress.value.delete(knowledgeId)
            }, 1500)
          } else {
            // 失败立即停止
            clearInterval(timer)
            pollingTimers.value.delete(knowledgeId)
            processingKnowledgeIds.value.delete(knowledgeId)
            processingProgress.value.delete(knowledgeId)
            
            // 只有组件未卸载时才显示消息
            if (!isUnmounted.value) {
              ElMessage.error(`《${knowledge.fileName}》处理失败: ${knowledge.errorMessage || '未知错误'}`)
            }
          }
        }
      }
    } catch (error) {
      console.error('[轮询] 查询处理进度失败:', error)
      // 出错继续轮询，不要立即停止
      // 只有在连续多次失败时才停止
    }
  }, 1000) // 每1秒查询一次，更快响应
  
  pollingTimers.value.set(knowledgeId, timer)
  console.log(`[轮询] 已启动ID=${knowledgeId}的轮询，定时器ID=${timer}`)
}

// 检查知识库是否可选择
const isKnowledgeSelectable = (knowledge) => {
  // 只有处理成功的才能选择
  return knowledge.status === 2
}

// 判断是否处理完成
const isCompleted = (row) => {
  return row.processingStatus === 'completed' || row.status === 2
}

// 分页数据（基于过滤后的列表）
const paginatedKnowledge = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredKnowledge.value.slice(start, end)
})

// 判断是否处理中
const isProcessing = (row) => {
  return row.processingStatus === 'processing' || row.status === 1
}

// 判断是否失败
const isFailed = (knowledge) => {
  return knowledge.status === 3 || 
         knowledge.processingStatus === 'failed' || 
         (knowledge.errorMessage && knowledge.processingStatus !== 'completed')
}

// 获取处理状态类型（用于Tag颜色）
const getProcessingStatusType = (row) => {
  const status = row.processingStatus || ''
  switch (status) {
    case 'pending':
      return 'warning'
    case 'configured':
      return 'info'
    case 'processing':
      return 'primary'
    case 'completed':
      return 'success'
    case 'failed':
      return 'danger'
    default:
      // 兼容旧状态
      return getStatusType(row.status)
  }
}

// 获取处理状态文本
const getProcessingStatusText = (row) => {
  const status = row.processingStatus || ''
  switch (status) {
    case 'pending':
      return '待配置'
    case 'configured':
      return '已配置'
    case 'processing':
      return '处理中'
    case 'completed':
      return '已完成'
    case 'failed':
      return '失败'
    default:
      // 兼容旧状态
      return row.statusText || '未知'
  }
}

// 计算处理进度
const getProgress = (row) => {
  if (row.processingStatus !== 'processing') return 0
  
  // 从实时进度数据中获取
  const progressData = processingProgress.value.get(row.id)
  if (progressData && progressData.progress >= 0) {
    return progressData.progress
  }
  
  // 默认显示10%（表示正在处理）
  return 10
}

// 获取进度文本
const getProgressText = (row) => {
  // 从实时进度数据中获取消息
  const progressData = processingProgress.value.get(row.id)
  if (progressData && progressData.message) {
    return progressData.message
  }
  
  // 如果有分片数量，显示分片信息
  if (row.segmentCount && row.segmentCount > 0) {
    return `已分片 ${row.segmentCount} 个`
  }
  
  // 默认消息
  return '正在处理...'
}

// 处理进度追踪
const processingProgress = ref(new Map())

// 获取知识库状态提示
const getKnowledgeStatusTip = (knowledge) => {
  switch (knowledge.status) {
    case 0:
      return '待处理'
    case 1:
      return '处理中...'
    case 2:
      return '可用'
    case 3:
      return '处理失败'
    default:
      return '未知状态'
  }
}

/**
 * 获取解析方式对应的图标类名
 * @param {string} parseMethod - 解析方式：POI, PDFBox, Text, LibreOffice
 * @returns {string} FontAwesome 图标类名
 */
const getParseMethodIcon = (parseMethod) => {
  if (!parseMethod) return 'fa-solid fa-file'
  switch (parseMethod.toLowerCase()) {
    case 'pdfbox':
      return 'fa-solid fa-file-pdf'              // PDF 图标
    case 'poi':
      return 'fa-solid fa-file-word'             // Word 图标
    case 'text':
      return 'fa-solid fa-file-lines'            // 文本文件图标
    case 'libreoffice':
      return 'fa-solid fa-file-export'           // 转换图标
    default:
      return 'fa-solid fa-file'
  }
}

/**
 * 获取解析方式对应的 CSS 样式类名
 * @param {string} parseMethod - 解析方式
 * @returns {string} CSS 类名，用于设置标签颜色
 */
const getParseMethodClass = (parseMethod) => {
  if (!parseMethod) return ''
  switch (parseMethod.toLowerCase()) {
    case 'pdfbox':
      return 'parse-pdfbox'      // 红色 - PDF
    case 'poi':
      return 'parse-poi'         // 蓝色 - Office
    case 'text':
      return 'parse-text'        // 灰色 - 文本
    case 'libreoffice':
      return 'parse-libreoffice' // 绿色 - LibreOffice
    default:
      return ''
  }
}

// 为已上传的知识库打开配置对话框
const openConfigForKnowledge = async (knowledge) => {
  try {
    // 获取推荐模板
    const response = await axios.get(`/api/knowledge/templates/recommended?fileType=${knowledge.fileType}`)
    
    if (response.data.success) {
      pendingFileInfo.value = {
        knowledgeId: knowledge.id,
        fileName: knowledge.fileName,
        fileType: knowledge.fileType,
        fileSize: knowledge.fileSize
      }
      configTemplates.value = response.data.templates || []
      showConfigDialog.value = true
    } else {
      ElMessage.error('获取配置模板失败')
    }
  } catch (error) {
    console.error('获取配置模板失败:', error)
    ElMessage.error('获取配置模板失败: ' + (error.response?.data?.message || error.message))
  }
}

// 手动开始处理（针对已配置但未处理的知识库）
const startProcessing = async (knowledgeId) => {
  try {
    const response = await axios.post(`/api/knowledge/start-processing/${knowledgeId}`)
    
    if (response.data.success) {
      ElMessage.success('开始处理...')
      
      // 添加到处理中列表
      processingKnowledgeIds.value.add(knowledgeId)
      
      // 刷新列表
      await loadKnowledgeList()
      
      // 开始轮询
      startPolling(knowledgeId)
    } else {
      ElMessage.error(response.data.message || '开始处理失败')
    }
  } catch (error) {
    console.error('开始处理失败:', error)
    ElMessage.error('开始处理失败: ' + (error.response?.data?.message || error.message))
  }
}

// 删除知识库
const deleteKnowledge = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这条知识库记录吗？删除后无法恢复。',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    const response = await axios.delete(`/api/knowledge/${id}`)
    if (response.data.success) {
      ElMessage.success('删除成功')
      await loadKnowledgeList()
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

// 重新处理
const reprocess = async (knowledge) => {
  try {
    // 如果有配置且失败，可以选择重新配置或直接重新处理
    if (knowledge.processingStatus === 'failed' && knowledge.configCompleted) {
      // 已配置但处理失败，询问用户
      await ElMessageBox.confirm(
        '是否使用原配置重新处理？点击"取消"可重新配置。',
        '重新处理',
        {
          confirmButtonText: '使用原配置',
          cancelButtonText: '重新配置',
          type: 'warning',
        }
      ).then(async () => {
        // 使用原配置重新处理
        await startProcessing(knowledge.id)
      }).catch(() => {
        // 重新配置
        openConfigForKnowledge(knowledge)
      })
    } else {
      // 旧流程或其他情况，直接调用reprocess接口
      const response = await axios.post(`/api/knowledge/reprocess/${knowledge.id}`)
      if (response.data.success) {
        ElMessage.success('已开始重新处理，正在处理向量化...')
        
        // 添加到处理中列表
        processingKnowledgeIds.value.add(knowledge.id)
        
        // 立即刷新列表
        await loadKnowledgeList()
        
        // 开始轮询处理进度
        startPolling(knowledge.id)
      } else {
        ElMessage.error(response.data.message || '重新处理失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新处理失败:', error)
      ElMessage.error('重新处理失败')
    }
  }
}

// 查看详情
const viewDetail = async (row) => {
  currentDetail.value = row
  showDetailDialog.value = true
  
  // 如果处理成功，加载分片信息
  if (row.status === 2) {
    await loadSegments(row.id)
  }
}

// 加载分片信息
const loadSegments = async (knowledgeBaseId) => {
  segmentsLoading.value = true
  try {
    const response = await axios.get(`/api/knowledge/${knowledgeBaseId}/segments`)
    if (response.data.success) {
      // 后端返回的是 ListResponse 格式：{ list: [], total: n }
      segments.value = response.data.data?.list || []
    } else {
      ElMessage.error(response.data.message || '加载分片信息失败')
    }
  } catch (error) {
    console.error('加载分片信息失败:', error)
    ElMessage.error('加载失败')
  } finally {
    segmentsLoading.value = false
  }
}

// 显示向量数据
const showVectorData = (segment) => {
  currentVector.value = segment
  showVectorDialog.value = true
}

// 显示原文内容
const showContentDetail = (segment) => {
  currentContent.value = segment
  showContentDialog.value = true
}

// 复制内容到剪贴板
const copyContent = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 格式化向量数据
const formatVectorData = (vectorData) => {
  if (!vectorData) return ''
  try {
    const vector = JSON.parse(vectorData)
    // 只显示前100个值
    const preview = vector.slice(0, 100)
    return JSON.stringify(preview, null, 2) + '\n\n...（共 ' + vector.length + ' 个值）'
  } catch (e) {
    return vectorData
  }
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
  }
  return typeMap[status] || 'info'
}

// 获取文件类型图标
const getFileIcon = (fileType) => {
  const type = fileType.toLowerCase()
  const iconMap = {
    'pdf': 'fa-solid fa-file-pdf',
    'doc': 'fa-solid fa-file-word',
    'docx': 'fa-solid fa-file-word',
    'xls': 'fa-solid fa-file-excel',
    'xlsx': 'fa-solid fa-file-excel',
    'ppt': 'fa-solid fa-file-powerpoint',
    'pptx': 'fa-solid fa-file-powerpoint',
    'txt': 'fa-solid fa-file-lines',
    'md': 'fa-brands fa-markdown',
    'csv': 'fa-solid fa-file-csv'
  }
  return iconMap[type] || 'fa-solid fa-file'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

// 编辑知识库
const editKnowledge = (kb) => {
  editForm.value = {
    id: kb.id,
    fileName: kb.fileName,
    description: kb.description || ''
  }
  showEditDialog.value = true
}

// 保存编辑
const saveEdit = async () => {
  // 验证
  if (!editForm.value.id) {
    ElMessage.warning('知识库ID不能为空')
    return
  }
  
  editSaving.value = true
  try {
    const response = await axios.put(`/api/knowledge/${editForm.value.id}/metadata`, {
      description: editForm.value.description || null
    })
    if (response.data.success) {
      ElMessage.success('保存成功')
      showEditDialog.value = false
      await loadKnowledgeList()
    } else {
      ElMessage.error(response.data.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    editSaving.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  statsLoading.value = true
  try {
    const response = await axios.get('/api/knowledge/stats')
    if (response.data.success) {
      stats.value = response.data.data
    }
  } catch (error) {
    console.error('加载统计失败:', error)
    ElMessage.error('加载统计失败')
  } finally {
    statsLoading.value = false
  }
}

// 监听统计对话框打开
watch(() => showStatsDialog.value, (newVal) => {
  if (newVal) {
    // 重置统计数据
    stats.value = {
      total: 0,
      completed: 0,
      totalUsage: 0,
      avgHitRate: 0,
      topKnowledge: []
    }
    loadStats()
  }
})

// 计算命中率
const calculateHitRate = (kb) => {
  if (!kb.usageCount || kb.usageCount === 0) return 0
  return Math.round((kb.hitCount / kb.usageCount) * 100)
}

// 打开检索测试
const openRetrievalTest = (kb) => {
  currentTestKnowledge.value = kb
  retrievalTestForm.value = {
    query: '',
    retrievalMode: 'vector',
    topK: 5
  }
  retrievalTestResults.value = []
  showRetrievalTestDialog.value = true
}

// 执行检索测试
const executeRetrievalTest = async () => {
  if (!retrievalTestForm.value.query) {
    ElMessage.warning('请输入查询文本')
    return
  }
  
  retrievalTesting.value = true
  try {
    const response = await axios.post(
      `/api/knowledge/${currentTestKnowledge.value.id}/test-retrieval`,
      retrievalTestForm.value
    )
    if (response.data.success) {
      retrievalTestResults.value = response.data.data
      if (retrievalTestResults.value.length === 0) {
        ElMessage.info('未找到相关结果')
      } else {
        ElMessage.success(`找到 ${retrievalTestResults.value.length} 条相关结果`)
      }
    } else {
      ElMessage.error(response.data.message || '检索失败')
    }
  } catch (error) {
    console.error('检索测试失败:', error)
    ElMessage.error('检索测试失败')
  } finally {
    retrievalTesting.value = false
  }
}

// 预览文件
const previewFile = async (knowledge) => {
  currentPreviewKnowledge.value = knowledge
  previewFileName.value = knowledge.fileName
  isLoadingPreview.value = true
  showPreviewDialog.value = true
  
  const fileType = knowledge.fileType.toLowerCase()
  // 移除loading提示，避免信息过多
  
  try {
    // 使用预览接口获取 PDF 文件
    const response = await axios.get(`/api/knowledge/${knowledge.id}/preview`, {
      responseType: 'blob'
    })
    
    console.log('文件下载响应:', {
      status: response.status,
      contentType: response.headers['content-type'],
      size: response.data.size
    })
    
    // 检查响应是否有效
    if (!response.data || response.data.size === 0) {
      throw new Error('文件内容为空')
    }
    
    // 创建 Blob URL
    const blob = new Blob([response.data], { 
      type: response.headers['content-type'] || 'application/octet-stream' 
    })
    
    // 所有文件都转换为 PDF 预览
    previewFileUrl.value = URL.createObjectURL(blob)
    
    // 重置PDF状态
    pdfCurrentPage.value = 1
    pdfTotalPages.value = 0
    
    console.log('PDF 文件加载成功，Blob URL:', previewFileUrl.value)
    
    // 设置超时，如果10秒后还没加载完成，自动关闭loading
    setTimeout(() => {
      if (isLoadingPreview.value) {
        console.warn('文件预览加载超时，自动关闭loading')
        isLoadingPreview.value = false
      }
    }, 10000)
    
  } catch (error) {
    console.error('文件加载失败:', error)
    ElMessage.error('文件加载失败: ' + (error.message || '未知错误'))
    isLoadingPreview.value = false
    showPreviewDialog.value = false
  }
}

// PDF加载完成
const onPdfLoaded = (pdf) => {
  isLoadingPreview.value = false
  pdfTotalPages.value = pdf.numPages
  console.log(`PDF加载完成，共 ${pdfTotalPages.value} 页`)
}

// PDF工具栏控制（使用自适应宽度）
const zoomIn = () => {
  // 自适应宽度模式下不需要缩放
}

const zoomOut = () => {
  // 自适应宽度模式下不需要缩放
}

const resetZoom = () => {
  // 自适应宽度模式下不需要缩放
}

const nextPage = () => {
  if (pdfCurrentPage.value < pdfTotalPages.value) {
    pdfCurrentPage.value++
  }
}

const prevPage = () => {
  if (pdfCurrentPage.value > 1) {
    pdfCurrentPage.value--
  }
}

// 预览加载错误
const onPreviewError = (error) => {
  isLoadingPreview.value = false
  console.error('文件预览失败:', error)
  ElMessage.error('文件预览失败，请尝试下载后查看')
}

// 下载文件
const downloadFile = () => {
  if (currentPreviewKnowledge.value) {
    window.open(`/api/knowledge/${currentPreviewKnowledge.value.id}/download`, '_blank')
    ElMessage.success('文件下载已开始')
  }
}

onMounted(() => {
  loadKnowledgeList()
})

// 清理预览文件的 Blob URL
const cleanupPreviewUrl = () => {
  if (previewFileUrl.value) {
    URL.revokeObjectURL(previewFileUrl.value)
    previewFileUrl.value = ''
  }
  textContent.value = ''
}

// 监听预览对话框关闭事件
const handlePreviewDialogClose = () => {
  cleanupPreviewUrl()
  isLoadingPreview.value = false
}

// 页面卸载时清理所有定时器和 Blob URL
onUnmounted(() => {
  isUnmounted.value = true // 标记组件已卸载
  pollingTimers.value.forEach((timer) => {
    clearInterval(timer)
  })
  pollingTimers.value.clear()
  cleanupPreviewUrl()
})
</script>

<style scoped src="@/styles/knowledge-manage.css"></style>
