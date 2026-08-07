<template>
  <div class="knowledge-library-manage" :class="{ 'embedded-mode': embedded }">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-book-open"></i>
      </div>
      <div class="header-content">
        <h2>知识库管理</h2>
        <span class="item-count">共 {{ totalLibraries }} 个知识库</span>
      </div>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="搜索知识库..." prefix-icon="Search" clearable style="width: 240px" @input="handleSearch" />
        <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
          <i class="fa-solid fa-plus"></i> 新建知识库
        </el-button>
      </div>
    </div>

    <!-- 知识库卡片列表 -->
    <div class="content-container">
      <div class="library-cards" v-loading="loading">
        <TransitionGroup name="card-list">
          <div v-for="lib in libraries" :key="lib.id" class="library-card" @click="openLibraryDetail(lib)">
            <!-- 卡片顶部装饰条 -->
            <div class="card-accent" :class="{ active: lib.status === 'active' }"></div>
            
            <div class="card-content">
              <!-- 头部 -->
              <div class="card-top">
                <div class="card-icon">
                  <span class="lib-icon">{{ lib.icon || '📚' }}</span>
                </div>
                <div class="card-title-area">
                  <div class="card-title-row">
                    <div class="name">{{ lib.name }}</div>
                  </div>
                </div>
                <div @click.stop>
                  <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, lib)">
                    <el-button text class="more-btn">
                      <i class="fa-solid fa-ellipsis-vertical"></i>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="edit"><i class="fa-solid fa-pen"></i> 编辑</el-dropdown-item>
                        <el-dropdown-item command="upload"><i class="fa-solid fa-upload"></i> 上传文档</el-dropdown-item>
                        <el-dropdown-item command="delete" divided><i class="fa-solid fa-trash"></i> 删除</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
              
              <!-- 描述 -->
              <el-tooltip 
                v-if="lib.description" 
                :content="lib.description" 
                placement="bottom" 
                :show-after="300"
                effect="dark"
                popper-class="prompt-tooltip"
                :disabled="lib.description.length <= 50"
              >
                <div class="lib-description">
                  <span class="desc-text">{{ lib.description }}</span>
                </div>
              </el-tooltip>
              <div v-else class="lib-description">
                <span class="desc-text">暂无描述</span>
              </div>
              
              <!-- 统计标签 -->
              <div class="config-tags">
                <span class="config-tag">
                  <i class="fa-solid fa-file-lines"></i>
                  {{ lib.documentCount || 0 }} 文档
                </span>
                <span class="config-tag">
                  <i class="fa-solid fa-layer-group"></i>
                  {{ lib.totalSegments || 0 }} 分段
                </span>
                <span class="config-tag">
                  <i class="fa-solid fa-database"></i>
                  {{ lib.totalSizeFormatted || '0 B' }}
                </span>
                <span class="config-tag success" v-if="lib.status === 'active'">
                  <i class="fa-solid fa-check-circle"></i>
                  可用
                </span>
              </div>
              
              <!-- 底部信息 -->
              <div class="card-bottom">
                <div class="card-meta">
                  <span class="meta-item">
                    <i class="fa-regular fa-calendar"></i>
                    {{ formatTime(lib.createdAt) }}
                  </span>
                  <span class="meta-item" v-if="lib.usageCount">
                    <i class="fa-solid fa-chart-line"></i>
                    使用 {{ lib.usageCount }} 次
                  </span>
                </div>
                <div class="card-actions">
                  <el-tooltip content="上传文档" placement="top" :show-after="200">
                    <button class="action-btn" @click.stop="openUploadDialog(lib)">
                      <i class="fa-solid fa-upload"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="检索测试" placement="top" :show-after="200">
                    <button class="action-btn warning" @click.stop="openLibraryRetrievalTest(lib)">
                      <i class="fa-solid fa-magnifying-glass"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="查看详情" placement="top" :show-after="200">
                    <button class="action-btn primary" @click.stop="openLibraryDetail(lib)">
                      <i class="fa-solid fa-eye"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top" :show-after="200">
                    <button class="action-btn danger" @click.stop="deleteLibrary(lib)">
                      <i class="fa-solid fa-trash"></i>
                    </button>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </TransitionGroup>
        
        <!-- 空状态 -->
        <div v-if="!loading && libraries.length === 0" class="empty-state">
          <i class="fa-solid fa-book-open"></i>
          <p>暂无知识库</p>
          <el-button type="primary" @click="showCreateDialog = true">创建第一个知识库</el-button>
        </div>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 36, 48]"
          :total="totalLibraries"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadLibraries"
        />
      </div>
    </div>

    <!-- 创建知识库对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建知识库" width="540px" :close-on-click-modal="false" class="create-library-dialog">
      <el-form :model="createForm" label-width="0px">
        <div class="form-section">
          <div class="icon-name-row">
            <el-popover placement="bottom" :width="280" trigger="click">
              <template #reference>
                <div class="icon-selector">
                  <span class="big-icon">{{ createForm.icon || '📚' }}</span>
                </div>
              </template>
              <div class="icon-grid">
                <span v-for="icon in iconOptions" :key="icon" class="icon-option" :class="{ active: createForm.icon === icon }" @click="createForm.icon = icon">{{ icon }}</span>
              </div>
            </el-popover>
            <div class="name-input-wrapper">
              <el-input 
                v-model="createForm.name" 
                placeholder="输入知识库名称" 
                maxlength="50" 
                class="name-input"
                size="large"
              />
              <span class="char-count">{{ createForm.name.length }}/50</span>
            </div>
          </div>
        </div>

        <div class="form-section">
          <label class="section-label">描述</label>
          <el-input 
            v-model="createForm.description" 
            type="textarea" 
            :rows="3" 
            placeholder="简要描述知识库的用途和内容..."
            maxlength="200" 
            show-word-limit
          />
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="createLibrary" :loading="creating" size="large">
            <i class="fa-solid fa-check"></i> 创建知识库
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 编辑知识库对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑知识库" width="540px" :close-on-click-modal="false" class="create-library-dialog">
      <el-form :model="editForm" label-width="0px">
        <div class="form-section">
          <div class="icon-name-row">
            <el-popover placement="bottom" :width="280" trigger="click">
              <template #reference>
                <div class="icon-selector">
                  <span class="big-icon">{{ editForm.icon || '📚' }}</span>
                </div>
              </template>
              <div class="icon-grid">
                <span v-for="icon in iconOptions" :key="icon" class="icon-option" :class="{ active: editForm.icon === icon }" @click="editForm.icon = icon">{{ icon }}</span>
              </div>
            </el-popover>
            <div class="name-input-wrapper">
              <el-input 
                v-model="editForm.name" 
                placeholder="输入知识库名称" 
                maxlength="50" 
                class="name-input"
                size="large"
              />
              <span class="char-count">{{ editForm.name.length }}/50</span>
            </div>
          </div>
        </div>

        <div class="form-section">
          <label class="section-label">描述</label>
          <el-input 
            v-model="editForm.description" 
            type="textarea" 
            :rows="3" 
            placeholder="简要描述知识库的用途和内容..."
            maxlength="200" 
            show-word-limit
          />
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showEditDialog = false" size="large">取消</el-button>
          <el-button type="primary" @click="updateLibrary" :loading="updating" size="large">
            <i class="fa-solid fa-check"></i> 保存更改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 知识库详情对话框 -->
    <el-dialog v-model="showDetailDialog" :title="currentLibrary?.name" width="1100px" top="5vh" class="detail-dialog">
      <div class="detail-content" v-if="currentLibrary">
        <!-- 知识库信息 -->
        <div class="lib-info-header">
          <div class="lib-icon-large">{{ currentLibrary.icon || '📚' }}</div>
          <div class="lib-info">
            <h3>{{ currentLibrary.name }}</h3>
            <p>{{ currentLibrary.description || '暂无描述' }}</p>
            <div class="lib-meta">
              <span><i class="fa-solid fa-clock"></i> 创建于 {{ formatTime(currentLibrary.createdAt) }}</span>
              <span><i class="fa-solid fa-file"></i> {{ currentLibrary.documents?.length || 0 }} 个文档</span>
              <span><i class="fa-solid fa-chart-line"></i> 使用 {{ currentLibrary.usageCount || 0 }} 次</span>
            </div>
          </div>
          <div class="lib-actions">
            <el-button type="primary" @click="openUploadDialog(currentLibrary)">
              <i class="fa-solid fa-upload"></i> 上传文档
            </el-button>
          </div>
        </div>
        
        <!-- 文档列表 -->
        <div class="documents-section">
          <div class="section-header">
            <h4><i class="fa-solid fa-file-lines"></i> 文档列表 ({{ currentLibrary.documents?.length || 0 }})</h4>
          </div>
          <el-table :data="paginatedDocuments" style="width: 100%" v-loading="loadingDetail">
            <el-table-column prop="fileName" label="文件名" min-width="180">
              <template #default="{ row }">
                <div class="file-name-cell">
                  <i :class="getFileIcon(row.fileType)" class="file-icon"></i>
                  <span>{{ row.fileName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="fileSizeFormatted" label="大小" width="90" />
            <el-table-column prop="segmentCount" label="分段" width="70" align="center">
              <template #default="{ row }">
                {{ row.segmentCount || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="180" align="center">
              <template #default="{ row }">
                <!-- 处理中显示进度条 -->
                <div v-if="row.processingStatus === 'processing'" class="doc-progress-wrapper">
                  <div class="progress-info">
                    <span class="progress-text">{{ getDocProgressText(row) }}</span>
                    <span class="progress-percent">{{ getDocProgress(row) }}%</span>
                  </div>
                  <el-progress
                    :percentage="getDocProgress(row)"
                    :stroke-width="4"
                    :show-text="false"
                    color="#0ea5e9"
                  />
                </div>
                <el-tag v-else :type="getStatusType(row.processingStatus)" size="small">
                  {{ getStatusText(row.processingStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row }">
                <!-- 待配置：显示配置按钮 -->
                <el-button v-if="row.processingStatus === 'pending'" text type="primary" size="small" @click="openDocConfig(row)">
                  <i class="fa-solid fa-gear"></i> 配置
                </el-button>
                <!-- 已配置：显示开始处理按钮 -->
                <el-button v-if="row.processingStatus === 'configured'" text type="success" size="small" @click="startDocProcessing(row)">
                  <i class="fa-solid fa-play"></i> 处理
                </el-button>
                <!-- 已完成：显示预览、详情、检索测试 -->
                <template v-if="row.processingStatus === 'completed'">
                  <el-button text type="primary" size="small" @click="previewDocument(row)">
                    <i class="fa-solid fa-eye"></i>
                  </el-button>
                  <el-button text type="info" size="small" @click="viewDocDetail(row)">
                    <i class="fa-solid fa-list"></i>
                  </el-button>
                  <el-button text type="warning" size="small" @click="openRetrievalTest(row)">
                    <i class="fa-solid fa-magnifying-glass"></i>
                  </el-button>
                </template>
                <!-- 失败：显示重试按钮 -->
                <el-button v-if="row.processingStatus === 'failed'" text type="warning" size="small" @click="retryDocProcessing(row)">
                  <i class="fa-solid fa-rotate-right"></i> 重试
                </el-button>
                <!-- 删除按钮 -->
                <el-button text type="danger" size="small" @click="deleteDocument(row)">
                  <i class="fa-solid fa-trash"></i>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div v-if="currentLibrary.documents && currentLibrary.documents.length > docPageSize" class="doc-pagination">
            <el-pagination
              v-model:current-page="docCurrentPage"
              :page-size="docPageSize"
              :total="currentLibrary.documents.length"
              layout="total, prev, pager, next"
              small
              background
            />
          </div>
          
          <!-- 空状态 -->
          <div v-if="!loadingDetail && (!currentLibrary.documents || currentLibrary.documents.length === 0)" class="empty-docs">
            <i class="fa-solid fa-file-circle-plus"></i>
            <p>暂无文档，点击上方按钮上传</p>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 上传文档对话框（使用原有的创建知识库组件，支持先配置再上传） -->
    <CreateKnowledgeDialog
      v-model="showUploadDialog"
      :library-id="currentLibrary?.id"
      @success="handleUploadSuccess"
    />

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
              <el-button @click="pdfCurrentPage = Math.max(1, pdfCurrentPage - 1)" :disabled="pdfCurrentPage <= 1">
                <i class="fa-solid fa-chevron-left"></i>
              </el-button>
              <el-button disabled>{{ pdfCurrentPage }} / {{ pdfTotalPages }}</el-button>
              <el-button @click="pdfCurrentPage = Math.min(pdfTotalPages, pdfCurrentPage + 1)" :disabled="pdfCurrentPage >= pdfTotalPages">
                <i class="fa-solid fa-chevron-right"></i>
              </el-button>
            </el-button-group>
          </div>
          <div class="toolbar-right">
            <el-button size="small" @click="downloadFile">
              <i class="fa-solid fa-download"></i> 下载
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
          <div v-else class="preview-placeholder">
            <i class="fa-solid fa-file-pdf"></i>
            <p>正在加载预览...</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showPreviewDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 文档详情对话框（分段信息） -->
    <el-dialog v-model="showDocDetailDialog" title="文档详情" width="80%" top="5vh">
      <div v-if="currentDocDetail">
        <h3>📊 基本信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ currentDocDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="文件名">{{ currentDocDetail.fileName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ currentDocDetail.fileType }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ currentDocDetail.fileSize }}</el-descriptions-item>
          <el-descriptions-item label="分段数量">{{ currentDocDetail.segmentCount || '0' }}</el-descriptions-item>
          <el-descriptions-item label="向量维度">{{ currentDocDetail.vectorDimension || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="处理状态">
            <el-tag :type="getStatusType(currentDocDetail.processingStatus)">
              {{ getStatusText(currentDocDetail.processingStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatTime(currentDocDetail.uploadTime) }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px;" v-if="currentDocDetail.processingStatus === 'completed'">
          <h3>📋 文档分片信息（共 {{ docSegments.length }} 个分片）</h3>
          <el-table :data="docSegments" style="width: 100%" max-height="400" v-loading="segmentsLoading">
            <el-table-column prop="segmentIndex" label="序号" width="70" align="center" />
            <el-table-column prop="content" label="分片内容" min-width="350">
              <template #default="scope">
                <el-popover placement="top" :width="500" trigger="hover" :content="scope.row.content">
                  <template #reference>
                    <div class="content-preview-cell">{{ scope.row.content }}</div>
                  </template>
                </el-popover>
              </template>
            </el-table-column>
            <el-table-column prop="contentLength" label="字数" width="80" align="center" />
            <el-table-column prop="embeddingId" label="嵌入ID" width="150">
              <template #default="scope">
                <el-tooltip :content="scope.row.embeddingId" placement="top">
                  <div class="embedding-id">{{ scope.row.embeddingId }}</div>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="vectorDimension" label="维度" width="70" align="center" />
            <el-table-column label="操作" width="100" align="center">
              <template #default="scope">
                <el-button size="small" link type="primary" @click="showVectorData(scope.row)" v-if="scope.row.vectorData">
                  查看向量
                </el-button>
                <span v-else class="no-vector">-</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>

    <!-- 向量数据对话框 -->
    <el-dialog v-model="showVectorDialog" title="向量数据" width="600px">
      <div v-if="currentVector">
        <p><strong>分片索引：</strong>{{ currentVector.segmentIndex }}</p>
        <p><strong>嵌入ID：</strong>{{ currentVector.embeddingId }}</p>
        <p><strong>向量维度：</strong>{{ currentVector.vectorDimension }}</p>
        <p><strong>向量数据（前100个值）：</strong></p>
        <el-input type="textarea" :rows="10" :model-value="formatVectorData(currentVector.vectorData)" readonly />
      </div>
    </el-dialog>

    <!-- 文档检索测试对话框 -->
    <el-dialog v-model="showRetrievalTestDialog" title="检索测试" width="800px">
      <div class="retrieval-test-container">
        <el-form :model="retrievalTestForm" label-width="100px">
          <el-form-item label="查询文本">
            <el-input v-model="retrievalTestForm.query" type="textarea" :rows="3" placeholder="请输入要检索的查询文本" />
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

        <div v-if="retrievalTestResults.length > 0" class="retrieval-results">
          <h3><i class="fa-solid fa-list-check"></i> 检索结果（共 {{ retrievalTestResults.length }} 条）</h3>
          <div v-for="(result, index) in retrievalTestResults" :key="index" class="result-item">
            <div class="result-header">
              <span class="result-rank">#{{ result.segmentIndex }}</span>
              <el-tag type="success" size="small">相似度: {{ (result.score * 100).toFixed(2) }}%</el-tag>
            </div>
            <div class="result-content">{{ result.content }}</div>
          </div>
        </div>
        <el-empty v-else-if="!retrievalTesting" description="请输入查询文本并开始检索" />
      </div>
    </el-dialog>

    <!-- 知识库检索测试对话框 -->
    <el-dialog v-model="showLibraryRetrievalDialog" :title="'知识库检索 - ' + (currentRetrievalLibrary?.name || '')" width="900px">
      <div class="retrieval-test-container">
        <div class="library-retrieval-info">
          <el-tag type="info" size="small">
            <i class="fa-solid fa-file"></i> 包含 {{ currentRetrievalLibrary?.documentCount || 0 }} 个文档
          </el-tag>
        </div>
        <el-form :model="libraryRetrievalForm" label-width="100px">
          <el-form-item label="查询文本">
            <el-input v-model="libraryRetrievalForm.query" type="textarea" :rows="3" placeholder="请输入要检索的查询文本，将在知识库所有文档中搜索" />
          </el-form-item>
          <el-form-item label="检索模式">
            <el-select v-model="libraryRetrievalForm.retrievalMode" style="width: 200px">
              <el-option label="向量检索" value="vector" />
              <el-option label="关键词检索" value="keyword" />
              <el-option label="混合检索" value="hybrid" />
            </el-select>
          </el-form-item>
          <el-form-item label="Top K">
            <el-input-number v-model="libraryRetrievalForm.topK" :min="1" :max="20" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="executeLibraryRetrievalTest" :loading="libraryRetrievalTesting">
              <i class="fa-solid fa-play"></i> 开始检索
            </el-button>
          </el-form-item>
        </el-form>

        <div v-if="libraryRetrievalResults.length > 0" class="retrieval-results">
          <h3><i class="fa-solid fa-list-check"></i> 检索结果（共 {{ libraryRetrievalResults.length }} 条）</h3>
          <div v-for="(result, index) in libraryRetrievalResults" :key="index" class="result-item">
            <div class="result-header">
              <span class="result-rank">#{{ index + 1 }}</span>
              <el-tag type="success" size="small">相似度: {{ (result.score * 100).toFixed(2) }}%</el-tag>
              <el-tag v-if="result.fileName" type="info" size="small">{{ result.fileName }}</el-tag>
            </div>
            <div class="result-content">{{ result.content }}</div>
          </div>
        </div>
        <el-empty v-else-if="!libraryRetrievalTesting" description="请输入查询文本并开始检索" />
      </div>
    </el-dialog>

    <!-- 文档配置对话框 -->
    <KnowledgeConfigDialog
      v-model="showConfigDialog"
      :file-info="pendingFileInfo || {}"
      :templates="configTemplates"
      @success="handleConfigSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import VuePdfEmbed from 'vue-pdf-embed'
import KnowledgeConfigDialog from '../components/KnowledgeConfigDialog.vue'
import CreateKnowledgeDialog from '../components/CreateKnowledgeDialog.vue'

// embedded: 在 Tab 容器中嵌入时为 true，隐藏外层冗余边距
const props = defineProps({ embedded: { type: Boolean, default: false } })
const embedded = props.embedded

// 状态
const loading = ref(false)
const loadingDetail = ref(false)
const creating = ref(false)
const uploading = ref(false)
const libraries = ref([])
const totalLibraries = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const searchKeyword = ref('')

// 对话框
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const showDetailDialog = ref(false)
const showUploadDialog = ref(false)
const showPreviewDialog = ref(false)
const showDocDetailDialog = ref(false)
const showRetrievalTestDialog = ref(false)
const showConfigDialog = ref(false)
const currentLibrary = ref(null)

// 预览相关
const previewFileUrl = ref('')
const previewFileName = ref('')
const isLoadingPreview = ref(false)
const pdfCurrentPage = ref(1)
const pdfTotalPages = ref(0)
const currentPreviewDoc = ref(null)

// 文档详情相关
const currentDocDetail = ref(null)
const docSegments = ref([])
const segmentsLoading = ref(false)

// 文档列表分页
const docCurrentPage = ref(1)
const docPageSize = ref(10)

// 向量数据相关
const showVectorDialog = ref(false)
const currentVector = ref(null)

// 进度追踪
const docProcessingProgress = ref(new Map())
const pollingTimers = ref(new Map())
const isUnmounted = ref(false) // 组件是否已卸载

// 文档检索测试相关
const retrievalTestForm = ref({
  query: '',
  retrievalMode: 'hybrid',
  topK: 5
})
const retrievalTesting = ref(false)
const retrievalTestResults = ref([])
const currentRetrievalDoc = ref(null)

// 知识库检索测试相关
const showLibraryRetrievalDialog = ref(false)
const currentRetrievalLibrary = ref(null)
const libraryRetrievalForm = ref({
  query: '',
  retrievalMode: 'hybrid',
  topK: 10
})
const libraryRetrievalTesting = ref(false)
const libraryRetrievalResults = ref([])

// 配置相关
const pendingFileInfo = ref(null)
const configTemplates = ref([])

// 表单
const createForm = ref({
  name: '',
  description: '',
  icon: '📚'
})

const editForm = ref({
  id: null,
  name: '',
  description: '',
  icon: '📚'
})

const updating = ref(false)

// 图标选项
const iconOptions = ['📚', '📖', '📕', '📗', '📘', '📙', '📓', '📔', '📒', '📝', '📄', '📃', '🗂️', '📁', '💼', '🎓', '🔬', '⚙️', '💡', '🎯']

// 分页后的文档列表
const paginatedDocuments = computed(() => {
  if (!currentLibrary.value?.documents) return []
  const start = (docCurrentPage.value - 1) * docPageSize.value
  const end = start + docPageSize.value
  return currentLibrary.value.documents.slice(start, end)
})

// 加载知识库列表
const loadLibraries = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    
    const res = await request({ url: '/cms/ai/knowledge-library/list', method: 'get'})
    libraries.value = res.data.records || []
    totalLibraries.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载知识库失败')
  } finally {
    loading.value = false
  }
}

// 创建知识库搜索
const handleSearch = () => {
  currentPage.value = 1
  loadLibraries()
}

// 创建知识库
const createLibrary = async () => {
  if (!createForm.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  creating.value = true
  try {
    const res = await request({ url: '/cms/ai/knowledge-library', method: 'post', data: createForm.value})
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    resetCreateForm()
    loadLibraries()
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    creating.value = false
  }
}

// 重置表单
const resetCreateForm = () => {
  createForm.value = {
    name: '',
    description: '',
    icon: '📚'
  }
}

// 打开知识库详情
const openLibraryDetail = async (lib) => {
  currentLibrary.value = lib
  showDetailDialog.value = true
  loadingDetail.value = true
  docCurrentPage.value = 1
  
  try {
    const res = await request({ url: `/cms/ai/knowledge-library/${lib.id}`, method: 'get' })
    currentLibrary.value = res.data

    // 检查是否有处理中的文档，自动开始轮询
    if (currentLibrary.value.documents) {
      currentLibrary.value.documents.forEach(doc => {
        if (doc.processingStatus === 'processing' && !pollingTimers.value.has(doc.id)) {
          console.log(`检测到处理中的文档ID=${doc.id}，开始监控`)
          startDocPolling(doc.id)
        }
      })
    }
  } catch (e) {
    ElMessage.error('加载详情失败')
  } finally {
    loadingDetail.value = false
  }
}

// 处理命令
const handleCommand = (cmd, lib) => {
  if (cmd === 'edit') {
    openEditDialog(lib)
  } else if (cmd === 'upload') {
    openUploadDialog(lib)
  } else if (cmd === 'delete') {
    deleteLibrary(lib)
  }
}

// 打开编辑对话框
const openEditDialog = (lib) => {
  editForm.value = {
    id: lib.id,
    name: lib.name,
    description: lib.description || '',
    icon: lib.icon || '📚'
  }
  showEditDialog.value = true
}

// 更新知识库
const updateLibrary = async () => {
  if (!editForm.value.name.trim()) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  updating.value = true
  try {
    const res = await request({ url: `/cms/ai/knowledge-library/${editForm.value.id}`, method: 'put', data: editForm.value})
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadLibraries()
  } catch (e) {
    ElMessage.error('更新失败')
  } finally {
    updating.value = false
  }
}

// 打开上传对话框
const openUploadDialog = (lib) => {
  currentLibrary.value = lib
  showUploadDialog.value = true
}

// 上传成功回调
const handleUploadSuccess = () => {
  showUploadDialog.value = false
  // 刷新知识库详情和列表
  if (currentLibrary.value) {
    openLibraryDetail(currentLibrary.value)
  }
  loadLibraries()
}

// 删除知识库
const deleteLibrary = async (lib) => {
  try {
    await ElMessageBox.confirm(`确定删除知识库「${lib.name}」吗？该操作将同时删除所有文档，不可恢复！`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const res = await request({ url: `/cms/ai/knowledge-library/${lib.id}`, method: 'delete'})
    ElMessage.success('删除成功')
    loadLibraries()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}


// 删除文档
const deleteDocument = async (doc) => {
  try {
    await ElMessageBox.confirm(`确定删除文档「${doc.fileName}」吗？`, '删除确认', { type: 'warning' })
    const res = await request({ url: `/cms/ai/knowledge-library/${currentLibrary.value.id}/documents/${doc.id}`, method: 'delete'})
    ElMessage.success('删除成功')
    openLibraryDetail(currentLibrary.value)
    loadLibraries()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 预览文档
const previewDocument = async (doc) => {
  currentPreviewDoc.value = doc
  previewFileName.value = doc.fileName
  isLoadingPreview.value = true
  showPreviewDialog.value = true
  
  try {
    // 使用预览接口获取 PDF 文件
    const response = await request({ url: `/cms/ai/knowledge-base/${doc.id}/preview`, method: 'get', responseType: 'blob'})
    
    // 检查响应类型
    const contentType = response.headers['content-type']
    let blob
    if (contentType && contentType.includes('application/pdf')) {
      blob = new Blob([response.data], { type: 'application/pdf' })
    } else {
      blob = new Blob([response.data], { type: contentType || 'application/pdf' })
    }
    
    // 所有文件都转换为 PDF 预览
    previewFileUrl.value = URL.createObjectURL(blob)
    
    // 重置PDF状态
    pdfCurrentPage.value = 1
    pdfTotalPages.value = 0
    
    // 设置超时
    setTimeout(() => {
      if (isLoadingPreview.value) {
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
}

// 预览加载错误
const onPreviewError = (error) => {
  isLoadingPreview.value = false
  console.error('文件预览失败:', error)
  ElMessage.error('文件预览失败，请尝试下载后查看')
}

// 下载文件
const downloadFile = async () => {
  if (!currentPreviewDoc.value) return
  try {
    // 走 request 拦截器携带 Authorization，后端 download 接口需鉴权
    const blob = await request({
      url: `/cms/ai/knowledge-base/${currentPreviewDoc.value.id}/download`,
      method: 'get',
      responseType: 'blob'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = currentPreviewDoc.value.fileName || 'download'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('文件下载已开始')
  } catch (e) {
    console.error('下载失败:', e)
    ElMessage.error('下载失败: ' + (e.message || '未知错误'))
  }
}

// 清理预览文件的 Blob URL
const cleanupPreviewUrl = () => {
  if (previewFileUrl.value) {
    URL.revokeObjectURL(previewFileUrl.value)
    previewFileUrl.value = ''
  }
}

// 监听预览对话框关闭事件
const handlePreviewDialogClose = () => {
  cleanupPreviewUrl()
  isLoadingPreview.value = false
}

// 打开文档配置
const openDocConfig = async (doc) => {
  try {
    // 获取推荐模板
    const response = await request({ url: `/cms/ai/knowledge-base/templates/recommended?fileType=${doc.fileType}`, method: 'get' })
    pendingFileInfo.value = {
      knowledgeId: doc.id,
      fileName: doc.fileName,
      fileType: doc.fileType,
      fileSize: doc.fileSizeFormatted || doc.fileSize
    }
    configTemplates.value = response.data.templates || []
    showConfigDialog.value = true
  } catch (error) {
    ElMessage.error('获取配置模板失败: ' + (error.response?.data?.message || error.message))
  }
}

// 配置成功回调
const handleConfigSuccess = () => {
  showConfigDialog.value = false
  // 刷新知识库详情
  if (currentLibrary.value) {
    openLibraryDetail(currentLibrary.value)
  }
}

// 开始处理文档
const startDocProcessing = async (doc) => {
  try {
    const response = await request({ url: `/cms/ai/knowledge-base/start-processing/${doc.id}`, method: 'post'})
    ElMessage.success('开始处理...')
    // 开始轮询进度
    startDocPolling(doc.id)
    // 刷新详情
    openLibraryDetail(currentLibrary.value)
  } catch (error) {
    ElMessage.error('开始处理失败: ' + (error.response?.data?.message || error.message))
  }
}

// 开始轮询文档处理进度
const startDocPolling = (docId) => {
  // 如果已经有定时器，先清除
  if (pollingTimers.value.has(docId)) {
    clearInterval(pollingTimers.value.get(docId))
  }
  
  // 初始化进度
  docProcessingProgress.value.set(docId, {
    progress: 0,
    message: '开始处理'
  })
  
  // 创建新的定时器，每1秒查询一次
  const timer = setInterval(async () => {
    try {
      // 查询实时进度
      const progressResponse = await request({ url: `/cms/ai/knowledge/progress/${docId}`, method: 'get' })
      if (progressResponse.data) {
        const progressData = progressResponse.data

        // 更新进度信息
        docProcessingProgress.value.set(docId, {
          progress: progressData.progress,
          message: progressData.message,
          currentStep: progressData.currentStep
        })
      }
      
      // 查询文档状态
      const statusResponse = await request({ url: `/cms/ai/knowledge-base/${docId}`, method: 'get' })
      const doc = statusResponse.data

      // 如果处理完成，停止轮询
      if (doc.status === 2 || doc.status === 3 ||
          doc.processingStatus === 'completed' || doc.processingStatus === 'failed') {

        if (doc.status === 2 || doc.processingStatus === 'completed') {
          // 立即停止轮询，防止重复触发
          clearInterval(timer)
          pollingTimers.value.delete(docId)

          // 设置进度为100%
          docProcessingProgress.value.set(docId, {
            progress: 100,
            message: '处理完成'
          })

          // 只有组件未卸载时才显示消息
          if (!isUnmounted.value) {
            ElMessage.success(`《${doc.fileName}》处理完成！`)
          }

          // 延迟清理进度显示
          setTimeout(() => {
            docProcessingProgress.value.delete(docId)
            // 刷新详情（只有组件未卸载时）
            if (!isUnmounted.value && currentLibrary.value) {
              openLibraryDetail(currentLibrary.value)
            }
          }, 1500)
        } else {
          // 失败立即停止
          clearInterval(timer)
          pollingTimers.value.delete(docId)
          docProcessingProgress.value.delete(docId)

          // 只有组件未卸载时才显示消息和刷新
          if (!isUnmounted.value) {
            ElMessage.error(`《${doc.fileName}》处理失败`)
            // 刷新详情
            if (currentLibrary.value) {
              openLibraryDetail(currentLibrary.value)
            }
          }
        }
      }
    } catch (error) {
      console.error('[轮询] 查询文档进度失败:', error)
    }
  }, 1000)
  
  pollingTimers.value.set(docId, timer)
  console.log(`[轮询] 已启动文档ID=${docId}的轮询`)
}

// 获取文档处理进度
const getDocProgress = (doc) => {
  if (doc.processingStatus !== 'processing') return 0
  
  const progressData = docProcessingProgress.value.get(doc.id)
  if (progressData && progressData.progress >= 0) {
    return progressData.progress
  }
  
  return 10
}

// 获取文档进度文本
const getDocProgressText = (doc) => {
  const progressData = docProcessingProgress.value.get(doc.id)
  if (progressData && progressData.message) {
    return progressData.message
  }
  
  return '正在处理...'
}

// 查看文档详情（分段列表）
const viewDocDetail = async (doc) => {
  currentDocDetail.value = doc
  showDocDetailDialog.value = true
  segmentsLoading.value = true
  docSegments.value = []
  
  try {
    const response = await request({ url: `/cms/ai/knowledge-base/${doc.id}/segments`, method: 'get' })
    // API返回的是 data.list 而不是 data.data
    docSegments.value = response.data?.list || response.data || []
  } catch (error) {
    ElMessage.error('加载分段信息失败')
  } finally {
    segmentsLoading.value = false
  }
}

// 检索测试
const openRetrievalTest = (doc) => {
  currentRetrievalDoc.value = doc
  retrievalTestForm.value = {
    query: '',
    retrievalMode: 'hybrid',
    topK: 5
  }
  retrievalTestResults.value = []
  showRetrievalTestDialog.value = true
}

// 执行检索测试
const executeRetrievalTest = async () => {
  if (!retrievalTestForm.value.query.trim()) {
    ElMessage.warning('请输入查询文本')
    return
  }
  
  retrievalTesting.value = true
  retrievalTestResults.value = []
  
  try {
    const response = await request({ url: `/cms/ai/knowledge-base/${currentRetrievalDoc.value.id}/test-retrieval`, method: 'post'})

    retrievalTestResults.value = response.data || []
    if (retrievalTestResults.value.length === 0) {
      ElMessage.info('未检索到相关内容')
    }
  } catch (error) {
    ElMessage.error('检索失败: ' + (error.response?.data?.message || error.message))
  } finally {
    retrievalTesting.value = false
  }
}

// 打开知识库检索测试
const openLibraryRetrievalTest = async (lib) => {
  try {
    // 获取最新的知识库详情，确保文档数量是最新的
    const res = await request({ url: `/cms/ai/knowledge-library/${lib.id}`, method: 'get' })
    currentRetrievalLibrary.value = res.data
  } catch (e) {
    console.error('获取知识库详情失败', e)
    currentRetrievalLibrary.value = lib
  }
  
  libraryRetrievalForm.value = {
    query: '',
    retrievalMode: 'hybrid',
    topK: 10
  }
  libraryRetrievalResults.value = []
  showLibraryRetrievalDialog.value = true
}

// 执行知识库检索测试
const executeLibraryRetrievalTest = async () => {
  if (!libraryRetrievalForm.value.query.trim()) {
    ElMessage.warning('请输入查询文本')
    return
  }
  
  libraryRetrievalTesting.value = true
  libraryRetrievalResults.value = []
  
  try {
    const response = await request({ url: `/cms/ai/knowledge-library/${currentRetrievalLibrary.value.id}/test-retrieval`, method: 'post'})

    libraryRetrievalResults.value = response.data || []
    if (libraryRetrievalResults.value.length === 0) {
      ElMessage.info('未检索到相关内容')
    }
  } catch (error) {
    ElMessage.error('检索失败: ' + (error.response?.data?.message || error.message))
  } finally {
    libraryRetrievalTesting.value = false
  }
}

// 重试处理
const retryDocProcessing = async (doc) => {
  try {
    await ElMessageBox.confirm('是否重新处理该文档？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const response = await request({ url: `/cms/ai/knowledge-base/reprocess/${doc.id}`, method: 'post'})
    ElMessage.success('开始重新处理...')
    // 开始轮询进度
    startDocPolling(doc.id)
    openLibraryDetail(currentLibrary.value)
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

// 页面卸载时清理所有定时器
onUnmounted(() => {
  isUnmounted.value = true // 标记组件已卸载
  pollingTimers.value.forEach((timer) => {
    clearInterval(timer)
  })
  pollingTimers.value.clear()
  cleanupPreviewUrl()
})

// 显示向量数据
const showVectorData = (row) => {
  currentVector.value = row
  showVectorDialog.value = true
}

// 格式化向量数据
const formatVectorData = (vectorData) => {
  if (!vectorData) return '无数据'
  try {
    const vector = JSON.parse(vectorData)
    // 只显示前100个值
    const preview = vector.slice(0, 100)
    return JSON.stringify(preview, null, 2) + '\n\n...（共 ' + vector.length + ' 个值）'
  } catch (e) {
    return vectorData
  }
}

// 工具函数
const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const getFileIcon = (type) => {
  const icons = {
    pdf: 'fa-solid fa-file-pdf',
    doc: 'fa-solid fa-file-word',
    docx: 'fa-solid fa-file-word',
    xls: 'fa-solid fa-file-excel',
    xlsx: 'fa-solid fa-file-excel',
    ppt: 'fa-solid fa-file-powerpoint',
    pptx: 'fa-solid fa-file-powerpoint',
    txt: 'fa-solid fa-file-lines',
    md: 'fa-solid fa-file-code'
  }
  return icons[type?.toLowerCase()] || 'fa-solid fa-file'
}

const getStatusType = (status) => {
  const types = { pending: 'info', configured: 'warning', processing: '', completed: 'success', failed: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { pending: '待处理', configured: '已配置', processing: '处理中', completed: '已完成', failed: '失败' }
  return texts[status] || status
}

// 初始化
onMounted(() => {
  loadLibraries()
})
</script>

<style scoped>
/* 与智能体管理保持一致的样式 */
.knowledge-library-manage {
  min-height: 100vh;
  background: #f8fafc;
  padding: 40px 20px 100px;
  position: relative;
}

/* Tab 嵌入模式：去掉外层冗余 padding，由 Tab 容器统一控制边距 */
.knowledge-library-manage.embedded-mode {
  min-height: auto;
  padding: 16px 16px 40px;
  background: transparent;
}

/* 页面头部 - 居中布局 */
.page-header {
  max-width: 1400px;
  margin: 0 auto 40px;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 20px;
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
}

.header-content {
  flex: 1;
}

.header-content h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
  white-space: nowrap;
}

.item-count {
  color: #9ca3af;
  font-size: 12px;
  margin-top: 2px;
  display: block;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.create-btn {
  border-radius: 8px;
  font-weight: 500;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  border: none;
}

/* 内容容器 - 居中 */
.content-container {
  max-width: 1400px;
  margin: 0 auto;
  max-height: calc(100vh - 40px - 88px - 65px - 100px);
  overflow-y: auto;
  padding: 0 20px;
  scrollbar-width: none;
}

.content-container::-webkit-scrollbar {
  display: none;
}

/* 卡片网格 */
.library-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

/* 卡片样式 */
.library-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: relative;
  display: flex;
  flex-direction: column;
}

.library-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.15);
}

.card-accent {
  height: 4px;
  background: linear-gradient(90deg, #d1d5db, #d1d5db);
  transition: background 0.3s;
}

.card-accent.active {
  background: linear-gradient(90deg, #10b981, #059669);
}

.library-card:hover .card-accent {
  background: linear-gradient(90deg, #6366f1, #4f46e5);
}

.card-content {
  padding: 16px 20px;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-top {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-shrink: 0;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #6366f1 0%, #4f46e5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: white;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.25);
  transition: all 0.3s;
}

.library-card:hover .card-icon {
  transform: scale(1.05) rotate(5deg);
}

.lib-icon {
  font-size: 24px;
}

.card-title-area {
  flex: 1;
  min-width: 0;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
}

.desc {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-btn {
  color: #9ca3af;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  transition: all 0.2s;
}

.more-btn:hover {
  background: #f3f4f6;
  color: #6366f1;
}

/* 描述预览 */
.lib-description {
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 3px solid #6366f1;
  margin-bottom: 10px;
  flex-shrink: 0;
}

.lib-description .desc-text {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  width: 100%;
  word-break: break-word;
}

/* 统计标签 */
.lib-stats {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  max-height: 44px;
  overflow: hidden;
  flex-shrink: 0;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 4px;
  background: #f9fafb;
  font-size: 11px;
  color: #9ca3af;
  border: 1px solid #f3f4f6;
}

.stat-item i {
  font-size: 10px;
  color: #d1d5db;
}

.stat-item.primary {
  background: #eff6ff;
  color: #3b82f6;
  border-color: #dbeafe;
}

.stat-item.primary i {
  color: #3b82f6;
}

.stat-item .stat-label {
  display: none;
}

/* 配置标签 */
.config-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-bottom: 8px;
  max-height: 44px;
  overflow: hidden;
  flex-shrink: 0;
}

.config-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border-radius: 4px;
  background: #f9fafb;
  font-size: 11px;
  color: #9ca3af;
  border: 1px solid #f3f4f6;
}

.config-tag i {
  font-size: 10px;
  color: #d1d5db;
}

.config-tag.success {
  background: #ecfdf5;
  color: #10b981;
  border-color: #d1fae5;
}

.config-tag.success i {
  color: #10b981;
}

/* 卡片底部 */
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  margin-top: auto;
  border-top: 1px solid #f3f4f6;
  flex-shrink: 0;
}

.card-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  flex: 1;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
}

.meta-item i {
  font-size: 12px;
  color: #9ca3af;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.action-btn:hover {
  background: #e5e7eb;
  color: #374151;
  transform: scale(1.1);
}

.action-btn.primary {
  background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
  color: white;
}

.action-btn.primary:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.4);
}

.action-btn.danger:hover {
  background: #fee2e2;
  color: #dc2626;
}

.action-btn.warning {
  background: #fef3c7;
  color: #d97706;
}

.action-btn.warning:hover {
  background: #fde68a;
  color: #b45309;
  transform: scale(1.1);
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 16px;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  color: #d1d5db;
}

.empty-state p {
  margin-bottom: 16px;
  color: #9ca3af;
}

/* 分页 - 固定底部 */
.pagination-wrapper {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 20px;
  display: flex;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  z-index: 100;
}

/* 卡片列表动画 */
.card-list-enter-active,
.card-list-leave-active {
  transition: all 0.3s ease;
}

.card-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.card-list-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* 创建对话框 */
.create-library-dialog :deep(.el-dialog__body) {
  padding: 24px 32px;
}

.form-section {
  margin-bottom: 24px;
}

.icon-name-row {
  display: flex;
  gap: 16px;
  align-items: center;
}

.icon-selector {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid #e9d5ff;
  flex-shrink: 0;
}

.icon-selector:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.2);
}

.big-icon {
  font-size: 32px;
}

.name-input-wrapper {
  flex: 1;
  position: relative;
}

.name-input :deep(.el-input__wrapper) {
  font-size: 18px;
  font-weight: 500;
  border-radius: 12px;
  padding: 12px 16px;
}

.char-count {
  position: absolute;
  right: 12px;
  bottom: -20px;
  font-size: 12px;
  color: #9ca3af;
}

.section-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.icon-option {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.icon-option:hover {
  background: #f3f4f6;
}

.icon-option.active {
  background: #8b5cf6;
  color: white;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 详情对话框 */
.detail-dialog :deep(.el-dialog__body) {
  padding: 24px 28px;
  min-height: 500px;
  max-height: 80vh;
  overflow-y: auto;
}

.detail-content {
  min-height: 450px;
  display: flex;
  flex-direction: column;
}

.documents-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f3f4f6;
  min-height: 280px;
}

.lib-info-header {
  display: flex;
  gap: 24px;
  padding: 24px;
  background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
  border-radius: 16px;
  margin-bottom: 24px;
  border: 1px solid #e9d5ff;
}

.lib-icon-large {
  width: 80px;
  height: 80px;
  font-size: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.15);
  flex-shrink: 0;
}

.lib-info {
  flex: 1;
  min-width: 0;
}

.lib-info h3 {
  margin: 0 0 8px 0;
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
}

.lib-info p {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

.lib-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 13px;
  color: #6b7280;
}

.lib-meta span {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.lib-meta i {
  color: #8b5cf6;
}

.lib-actions {
  display: flex;
  align-items: flex-start;
  flex-shrink: 0;
}

.lib-actions .el-button {
  border-radius: 10px;
  padding: 12px 20px;
  font-weight: 500;
}

.section-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.section-header h4 {
  margin: 0;
  font-size: 15px;
  color: #374151;
}

.section-header h4 i {
  margin-right: 8px;
  color: #667eea;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-icon {
  color: #667eea;
}

.processing-status {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #0ea5e9;
  font-size: 13px;
}

.processing-status i {
  color: #0ea5e9;
}

/* 文档进度条样式 */
.doc-progress-wrapper {
  width: 100%;
  padding: 4px 0;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
}

.progress-text {
  color: #6b7280;
  font-size: 11px;
}

.progress-percent {
  color: #0ea5e9;
  font-weight: 600;
  font-size: 12px;
}

.doc-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
}

.empty-docs {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

.empty-docs i {
  font-size: 36px;
  margin-bottom: 12px;
}

/* 上传 */
.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  padding: 40px;
}

.upload-icon {
  font-size: 48px;
  color: #667eea;
  margin-bottom: 16px;
}

.upload-text {
  font-size: 14px;
  color: #606266;
}

.upload-text em {
  color: #667eea;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

/* 动画 */
.card-list-enter-active,
.card-list-leave-active {
  transition: all 0.3s ease;
}

.card-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.card-list-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* PDF预览 - 与原来保持一致 */
.pdf-preview-wrapper {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 80vh;
  background: #f5f5f5;
}

.preview-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.toolbar-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-center {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.pdf-viewer {
  flex: 1;
  overflow: auto;
  background: #525659;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
}

.pdf-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 30px 20px;
  box-sizing: border-box;
}

.pdf-canvas {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  width: 100% !important;
  height: auto !important;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  padding: 60px;
  width: 100%;
  height: 100%;
}

.preview-placeholder i {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 文档详情 */
.content-preview-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
  font-size: 13px;
  color: #4b5563;
  cursor: pointer;
}

.content-preview-cell:hover {
  color: #8b5cf6;
}

.embedding-id {
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: monospace;
  font-size: 11px;
  color: #9ca3af;
}

/* 检索测试 */
.retrieval-test-container {
  min-height: 300px;
}

.retrieval-results {
  margin-top: 24px;
}

.retrieval-results h3 {
  margin-bottom: 16px;
  font-size: 16px;
  color: #374151;
}

.retrieval-results h3 i {
  margin-right: 8px;
  color: #8b5cf6;
}

.result-item {
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 12px;
  border-left: 3px solid #8b5cf6;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.result-rank {
  font-weight: 600;
  color: #8b5cf6;
}

.result-content {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
}

.no-vector {
  color: #9ca3af;
  font-size: 12px;
}

/* 知识库检索信息 */
.library-retrieval-info {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
  border-radius: 8px;
  border: 1px solid #e9d5ff;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .header-actions {
    width: 100%;
  }
  .library-cards {
    grid-template-columns: 1fr;
  }
}
</style>
