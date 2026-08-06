<template>
  <div class="agent-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-icon">
        <i class="fa-solid fa-robot"></i>
      </div>
      <div class="header-content">
        <h2>智能体管理</h2>
        <span class="item-count">共 {{ filteredAgents.length }} 个智能体</span>
      </div>
      <div class="header-actions">
        <el-input v-model="searchKeyword" placeholder="搜索名称/描述..." prefix-icon="Search" clearable style="width: 200px" />
        <el-select v-model="searchStatus" placeholder="状态" clearable style="width: 120px">
          <el-option label="已启用" :value="true" />
          <el-option label="已禁用" :value="false" />
        </el-select>
        <el-dropdown trigger="click" @command="handleExportImport">
          <el-button>
            <i class="fa-solid fa-file-import"></i> 导入/导出 <i class="fa-solid fa-caret-down"></i>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="export">
                <i class="fa-solid fa-file-export"></i> 导出全部智能体
              </el-dropdown-item>
              <el-dropdown-item command="import">
                <i class="fa-solid fa-file-import"></i> 导入智能体
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="primary" @click="showCreateDialog = true" class="create-btn">
          <i class="fa-solid fa-plus"></i> 新建智能体
        </el-button>
      </div>
    </div>

    <!-- 内容容器 -->
    <div class="content-container">

      <!-- 智能体卡片列表 -->
      <div class="agent-cards" v-loading="loading">
      <TransitionGroup name="card-list">
        <div
          v-for="agent in paginatedAgents"
          :key="agent.id"
          class="agent-card"
          @click="startChat(agent.id)"
        >
          <!-- 卡片顶部装饰条 -->
          <div class="card-accent" :class="{ active: agent.enabled }"></div>

          <!-- 卡片主体内容 -->
          <div class="card-content">
            <!-- 头部：图标 + 标题 + 状态 -->
            <div class="card-top">
              <div class="card-icon" :class="{ active: agent.enabled }">
                <i class="fa-solid fa-robot"></i>
              </div>
              <div class="card-title-area">
                <div class="card-title-row">
                  <span class="name">{{ agent.name }}</span>
                  <el-tag :type="agent.enabled ? 'success' : 'info'" size="small" effect="plain" round class="status-tag">
                    {{ agent.enabled ? '已启用' : '已禁用' }}
                  </el-tag>
                </div>
                <div class="desc">{{ agent.description || '暂无描述' }}</div>
              </div>
            </div>

            <!-- 提示词预览 -->
            <el-tooltip 
              v-if="agent.systemPrompt" 
              :content="agent.systemPrompt" 
              placement="bottom" 
              :show-after="300"
              :hide-after="0"
              effect="dark"
              popper-class="prompt-tooltip"
              :disabled="agent.systemPrompt.trim().length <= 50"
            >
              <div class="prompt-preview">
                <i class="fa-solid fa-wand-magic-sparkles"></i>
                <span class="prompt-text">{{ agent.systemPrompt }}</span>
              </div>
            </el-tooltip>

            <!-- 开场白预览 -->
            <el-tooltip 
              v-if="agent.welcomeMessage" 
              :content="agent.welcomeMessage" 
              placement="bottom" 
              :show-after="300"
              effect="dark"
              popper-class="prompt-tooltip"
              :disabled="agent.welcomeMessage.trim().length <= 40"
            >
              <div class="welcome-preview">
                <i class="fa-solid fa-comment-dots"></i>
                <span>{{ agent.welcomeMessage }}</span>
              </div>
            </el-tooltip>
            <div v-else class="welcome-preview empty">
              <i class="fa-solid fa-comment-dots"></i>
              <span>暂无开场白</span>
            </div>

            <!-- 配置标签 -->
            <div class="config-tags">
              <span class="config-tag">
                <i class="fa-solid fa-temperature-half"></i>
                {{ agent.temperature || 0.7 }}
              </span>
              <span class="config-tag" v-if="agent.knowledgeLibraryIds || agent.knowledgeBaseIds">
                <i class="fa-solid fa-book"></i>
                {{ getKnowledgeCount(agent) }} 知识库
              </span>
              <span class="config-tag" v-if="agent.toolCount > 0">
                <i class="fa-solid fa-wrench"></i>
                {{ agent.toolCount }} 工具
              </span>
              <span class="config-tag success" v-if="agent.apiEnabled">
                <i class="fa-solid fa-plug"></i>
                API
              </span>
              <span class="config-tag" v-if="getSuggestedQuestionsCount(agent) > 0">
                <i class="fa-solid fa-lightbulb"></i>
                {{ getSuggestedQuestionsCount(agent) }} 预设问题
              </span>
              <span class="config-tag primary" v-if="agent.workflowId">
                <i class="fa-solid fa-diagram-project"></i>
                工作流
              </span>
              <span class="config-tag warning" v-if="agent.publishEnabled">
                <i class="fa-solid fa-rocket"></i>
                已发布
              </span>
            </div>

            <!-- 底部：元信息 + 操作按钮 -->
            <div class="card-bottom">
              <div class="card-meta">
                <span class="meta-item">
                  <i class="fa-solid fa-comments"></i>
                  <span>{{ agent.sessionCount || 0 }} 会话</span>
                </span>
                <span class="meta-item">
                  <i class="fa-solid fa-message"></i>
                  <span>{{ agent.messageCount || 0 }} 消息</span>
                </span>
                <span class="meta-item">
                  <i class="fa-regular fa-clock"></i>
                  <span>{{ formatTime(agent.updateTime) }}</span>
                </span>
              </div>
              <div class="card-actions" @click.stop>
                <el-tooltip content="对话" placement="top" :show-after="200">
                  <button class="action-btn primary" @click="startChat(agent.id)">
                    <i class="fa-solid fa-comment"></i>
                  </button>
                </el-tooltip>
                <el-tooltip content="统计" placement="top" :show-after="200">
                  <button class="action-btn" @click="showAgentStats(agent)">
                    <i class="fa-solid fa-chart-line"></i>
                  </button>
                </el-tooltip>
                <el-tooltip content="复制" placement="top" :show-after="200">
                  <button class="action-btn" @click="duplicateAgent(agent)">
                    <i class="fa-solid fa-copy"></i>
                  </button>
                </el-tooltip>
                <el-tooltip content="编辑" placement="top" :show-after="200">
                  <button class="action-btn" @click="editAgent(agent)">
                    <i class="fa-solid fa-pen-to-square"></i>
                  </button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top" :show-after="200">
                  <button class="action-btn danger" @click="deleteAgent(agent.id)">
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
        :page-sizes="[9, 18, 27, 36]"
        :total="filteredAgents.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editMode ? '✏️ 编辑智能体' : '✨ 新建智能体'"
      width="1100px"
      :close-on-click-modal="false"
      destroy-on-close
      class="agent-dialog"
    >
      <!-- 步骤条 -->
      <el-steps :active="currentStep" finish-status="success" simple style="margin-bottom: 20px;" class="agent-steps">
        <el-step title="基本信息" />
        <el-step title="对话设置" />
        <el-step title="知识库" />
        <el-step title="RAG" />
        <el-step title="工具" />
        <el-step title="工作流" />
        <el-step title="发布" />
      </el-steps>

      <el-form :model="formData" label-width="120px" style="min-height: 400px;">

        <!-- 步骤1：基本信息 -->
        <div v-show="currentStep === 0">
          <div class="step-header">
            <i class="fa-solid fa-info-circle"></i> 基本信息
            <span class="step-desc">设置智能体的名称、描述和系统提示词</span>
          </div>

          <el-form-item label="名称" required>
            <el-input v-model="formData.name" placeholder="请输入智能体名称" />
            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
              <i class="fa-solid fa-info-circle"></i>
              智能体的显示名称，会在对话中作为身份介绍
            </div>
          </el-form-item>
          <el-form-item label="描述">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="2"
              placeholder="请输入智能体描述，如：专业的技术文档助手"
            />
            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
              <i class="fa-solid fa-info-circle"></i>
              简要描述智能体的专业领域和能力，会添加到系统提示词中
            </div>
          </el-form-item>
          <el-form-item label="系统提示词" required>
            <div style="width: 100%;">
              <el-input
                v-model="formData.systemPrompt"
                type="textarea"
                :autosize="{ minRows: 5, maxRows: 10 }"
                placeholder="描述智能体的角色、专业领域和回答风格..."
              />
              <div style="margin-top: 8px; display: flex; align-items: center; gap: 10px;">
                <el-button
                  type="primary"
                  size="small"
                  :loading="generatingPrompt"
                  @click="generateSystemPrompt"
                >
                  ✨ AI生成专业提示词
                </el-button>
                <span style="color: #909399; font-size: 12px;">输入简单描述，AI帮你生成专业的系统提示词</span>
              </div>
              <div style="font-size: 12px; color: #606266; margin-top: 8px; line-height: 1.6; background: #f5f7fa; padding: 10px; border-radius: 4px;">
                <strong>💡 系统提示词说明：</strong><br/>
                • 定义智能体的角色、性格和回答风格<br/>
                • 会与名称、描述、工具说明、知识库规则自动组合<br/>
                • 优先级：工具调用 > 知识库检索 > 通用回答
              </div>
            </div>
          </el-form-item>
          <el-form-item label="启用状态">
            <el-switch v-model="formData.enabled" />
            <span style="margin-left: 10px; color: #909399; font-size: 12px;">
              {{ formData.enabled ? '智能体已启用，可在对话中使用' : '智能体已禁用，不会出现在对话列表' }}
            </span>
          </el-form-item>
        </div>

        <!-- 步骤2：对话设置 -->
        <div v-show="currentStep === 1">
          <div class="step-header">
            <i class="fa-solid fa-comments"></i> 对话设置
            <span class="step-desc">配置开场白、预设问题和对话行为</span>
          </div>

          <el-form-item label="开场白">
            <el-input
              v-model="formData.welcomeMessage"
              type="textarea"
              :rows="3"
              placeholder="你好！我是你的智能助手，有什么可以帮助你的吗？"
            />
            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
              <i class="fa-solid fa-info-circle"></i>
              用户进入对话时显示的欢迎语
            </div>
          </el-form-item>

          <el-form-item label="预设问题">
            <div style="width: 100%;">
              <div v-for="(q, index) in suggestedQuestionsList" :key="index" style="display: flex; gap: 8px; margin-bottom: 8px;">
                <el-input v-model="suggestedQuestionsList[index]" placeholder="输入建议问题" />
                <el-button type="danger" :icon="Delete" circle @click="removeSuggestedQuestion(index)" />
              </div>
              <el-button type="primary" size="small" @click="addSuggestedQuestion" :disabled="suggestedQuestionsList.length >= 5">
                <i class="fa-solid fa-plus"></i> 添加问题
              </el-button>
              <span style="margin-left: 10px; color: #909399; font-size: 12px;">
                最多5个预设问题，用户可点击快速提问
              </span>
            </div>
          </el-form-item>

          <el-divider content-position="left">
            <i class="fa-solid fa-sliders"></i> 对话行为
          </el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="使用模型">
                <el-select v-model="formData.modelConfigId" placeholder="默认模型" clearable filterable>
                  <el-option
                    v-for="config in modelConfigs"
                    :key="config.id"
                    :label="`${config.modelName} (${config.provider})`"
                    :value="config.id"
                  >
                    <span>{{ config.modelName }}</span>
                    <span style="float: right; color: #909399; margin-left: 8px;">{{ config.provider }}</span>
                    <i v-if="config.isDefault" class="fa-solid fa-star" style="color: #f5a623; margin-left: 5px;"></i>
                  </el-option>
                </el-select>
                <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                  <i class="fa-solid fa-info-circle"></i>
                  不选择则使用系统默认Chat模型
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="显示引用来源">
                <el-switch v-model="formData.showCitations" />
                <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                  回答时显示知识库引用片段
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="历史轮数">
                <el-input-number v-model="formData.maxHistoryTurns" :min="1" :max="50" style="width: 100%;" />
                <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                  保留的对话历史轮数
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="left">
            <i class="fa-solid fa-key"></i> API访问
          </el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="启用API">
                <el-switch v-model="formData.apiEnabled" />
                <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                  允许通过API调用此智能体
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12" v-if="formData.apiEnabled">
              <el-form-item label="API Key">
                <el-input v-model="formData.apiKey" placeholder="自动生成" readonly>
                  <template #append>
                    <el-button @click="generateApiKey">生成</el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 步骤3：知识库配置 -->
        <div v-show="currentStep === 2">
          <div class="step-header">
            <i class="fa-solid fa-database"></i> 知识库配置
            <span class="step-desc">配置智能体的知识来源和专业术语理解能力</span>
          </div>

          <el-form-item label="关联知识库">
            <el-select
              v-model="selectedKnowledgeLibraries"
              multiple
              placeholder="请选择知识库"
              style="width: 100%"
            >
              <el-option
                v-for="lib in knowledgeLibraryList"
                :key="lib.id"
                :label="lib.name"
                :value="lib.id"
              >
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>{{ lib.icon || '📚' }} {{ lib.name }}</span>
                  <span style="color: #909399; font-size: 12px;">{{ lib.documentCount || 0 }} 文档</span>
                </div>
              </el-option>
            </el-select>
            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
              <i class="fa-solid fa-info-circle"></i>
              选择智能体可以访问的知识库，支持多选
            </div>
          </el-form-item>

          <!-- 知识库权重配置 -->
          <el-form-item label="知识库权重" v-if="selectedKnowledgeLibraries.length > 0">
            <div style="width: 100%;">
              <div style="background: #fff7e6; padding: 12px; border-radius: 6px; margin-bottom: 12px; border-left: 3px solid #faad14;">
                <div style="font-size: 13px; color: #606266; line-height: 1.8;">
                  <div><i class="fa-solid fa-balance-scale" style="color: #faad14;"></i> <strong>知识库权重说明</strong></div>
                  <div style="margin-top: 6px;">
                    • 权重控制不同知识库内容在检索结果中的优先级<br/>
                    • 权重范围：0.0-1.0，默认1.0（标准权重），0.0表示禁用<br/>
                    • <strong style="color: #e6a23c;">建议：核心知识库设为1.0，辅助知识库设为0.6-0.8</strong><br/>
                    • <strong style="color: #f56c6c;">⚠️ 建议关联知识库数量 ≤ 3个</strong>，过多会影响检索准确率
                  </div>
                </div>
              </div>

              <!-- 权重预设模板 -->
              <div style="margin-bottom: 12px;">
                <div style="font-size: 13px; color: #606266; margin-bottom: 8px;">
                  <i class="fa-solid fa-magic"></i> 快速应用权重模板：
                </div>
                <el-button-group size="small">
                  <el-button @click="applyWeightTemplate('equal')">
                    <i class="fa-solid fa-equals"></i> 平等权重
                  </el-button>
                  <el-button @click="applyWeightTemplate('primary-secondary')">
                    <i class="fa-solid fa-layer-group"></i> 主次分明
                  </el-button>
                  <el-button @click="applyWeightTemplate('core-reference')">
                    <i class="fa-solid fa-star"></i> 核心+参考
                  </el-button>
                  <el-button @click="applyWeightTemplate('core-secondary-archive')" v-if="selectedKnowledgeLibraries.length >= 3">
                    <i class="fa-solid fa-sitemap"></i> 三级权重
                  </el-button>
                </el-button-group>
              </div>

              <div v-for="libId in selectedKnowledgeLibraries" :key="libId" 
                   style="display: flex; align-items: center; gap: 12px; margin-bottom: 12px; padding: 10px; background: #f5f7fa; border-radius: 6px;">
                <div style="flex: 1; display: flex; align-items: center; gap: 8px;">
                  <i class="fa-solid fa-book" style="color: #409eff;"></i>
                  <span style="font-weight: 500;">{{ getKnowledgeLibraryName(libId) }}</span>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <!-- 启用/禁用开关 -->
                  <el-switch
                    v-model="knowledgeBaseEnabled[libId]"
                    size="small"
                    active-text="启用"
                    inactive-text="禁用"
                    @change="toggleKnowledgeBase(libId)"
                    style="margin-right: 8px;"
                  />
                  <span style="font-size: 12px; color: #909399;">权重：</span>
                  <el-input-number
                    v-model="knowledgeBaseWeights[libId]"
                    :min="0.0"
                    :max="1.0"
                    :step="0.1"
                    :precision="1"
                    size="small"
                    style="width: 120px;"
                    :disabled="!knowledgeBaseEnabled[libId]"
                    @change="updateKnowledgeBaseWeights"
                  />
                  <el-tag 
                    :type="getWeightTagType(knowledgeBaseWeights[libId])" 
                    size="small"
                    style="min-width: 70px; text-align: center;"
                  >
                    {{ getWeightLabel(knowledgeBaseWeights[libId]) }}
                  </el-tag>
                </div>
              </div>

              <el-alert 
                v-if="selectedKnowledgeLibraries.length > 3" 
                type="warning" 
                :closable="false"
                style="margin-top: 12px;"
              >
                <template #title>
                  <div style="font-size: 12px;">
                    <i class="fa-solid fa-exclamation-triangle"></i>
                    当前已选择 <strong>{{ selectedKnowledgeLibraries.length }}</strong> 个知识库，建议减少到 <strong>3个以内</strong>，以提升检索准确率和响应速度
                  </div>
                </template>
              </el-alert>
            </div>
          </el-form-item>

          <el-form-item label="专业词典">
            <el-select
              v-model="selectedDictionaries"
              multiple
              placeholder="选择专业领域词典（可选）"
              style="width: 100%"
            >
              <el-option
                v-for="dict in availableDictionaries"
                :key="dict.id"
                :label="`${dict.keyword} (${dict.category})`"
                :value="dict.id"
              >
                <div style="display: flex; justify-content: space-between; align-items: center;">
                  <span>{{ dict.keyword }}</span>
                  <el-tag size="small" type="warning">{{ dict.category }}</el-tag>
                </div>
              </el-option>
            </el-select>
            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
              <i class="fa-solid fa-info-circle"></i>
              通过关键词扩展提高知识库检索召回率。例如：查询"服务器"时，自动扩展搜索"cpu、内存、存储"等相关词。
            </div>
          </el-form-item>

          <el-alert type="info" :closable="false" style="margin-top: 20px;">
            <template #title>
              <div style="font-size: 13px;">
                <i class="fa-solid fa-lightbulb"></i>
                <strong>提示：</strong>如果不需要知识库检索功能，可以跳过此步骤直接下一步
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 步骤4：RAG 检索配置 -->
        <div v-show="currentStep === 3">
          <div class="step-header">
            <i class="fa-solid fa-brain"></i> RAG 智能检索配置
            <span class="step-desc">控制知识库检索的方式和数量，已设置推荐默认值</span>
          </div>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item>
              <template #label>
                <span>相似度阈值</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 280px; line-height: 1.6;">
                      <strong style="color: #409eff;">📊 相似度阈值说明：</strong><br/>
                      控制检索结果与问题的匹配程度<br/><br/>
                      <strong>• 0.7-0.9</strong>：只返回高度匹配的内容，适合精准问答<br/>
                      <strong>• 0.5-0.6</strong>：平衡匹配，适合大多数场景（推荐）<br/>
                      <strong>• 0.3-0.4</strong>：广泛召回，可能包含相关度低的内容<br/><br/>
                      <em style="color: #909399;">💡 值越高越精准，但可能遗漏相关内容</em>
                    </div>
                  </template>
                  <i class="fa-solid fa-circle-question" style="color: #909399; margin-left: 4px; cursor: help;"></i>
                </el-tooltip>
              </template>
              <el-input-number
                v-model="formData.ragMinScore"
                :min="0.3"
                :max="1.0"
                :step="0.05"
                :precision="2"
                style="width: 100%"
                :controls-position="'right'"
              >
                <template #prefix>
                  <i class="fa-solid fa-percentage" style="color: #909399;"></i>
                </template>
              </el-input-number>
              <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                推荐值：0.5-0.6（通用场景）
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item>
              <template #label>
                <span>最终结果数</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 280px; line-height: 1.6;">
                      <strong style="color: #409eff;">📋 最终结果数说明：</strong><br/>
                      经过重排后，实际提供给AI的知识库内容条数<br/><br/>
                      <strong>• 3-5条</strong>：快速简洁回答，适合FAQ<br/>
                      <strong>• 8-10条</strong>：标准回答，适合大多数场景（推荐）<br/>
                      <strong>• 15-20条</strong>：详尽深度回答，适合复杂问题<br/><br/>
                      <em style="color: #909399;">💡 越多内容越全面，但AI响应会更慢</em>
                    </div>
                  </template>
                  <i class="fa-solid fa-circle-question" style="color: #909399; margin-left: 4px; cursor: help;"></i>
                </el-tooltip>
              </template>
              <el-input-number
                v-model="formData.ragMaxResults"
                :min="1"
                :max="20"
                :step="1"
                style="width: 100%"
                :controls-position="'right'"
              >
                <template #prefix>
                  <i class="fa-solid fa-list-check" style="color: #909399;"></i>
                </template>
              </el-input-number>
              <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                推荐值：8-10条（通用场景）
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item>
              <template #label>
                <span>召回倍数</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 360px; line-height: 1.8;">
                      <strong style="color: #409eff;">🎯 为什么需要召回倍数？</strong><br/>
                      <div style="margin: 8px 0; padding: 8px; background: #f5f7fa; border-radius: 4px;">
                        RAG检索分两阶段：<br/>
                        <strong>① 粗筛</strong>：向量检索快速召回较多候选（如20条）<br/>
                        <strong>② 精排</strong>：Rerank模型精细排序，返回最相关的（如10条）
                      </div>
                      <strong style="color: #67c23a;">💡 核心作用：</strong><br/>
                      先多召回一些候选，再通过精排筛选出最优结果<br/>
                      避免向量检索漏掉相关内容<br/><br/>
                      <strong>计算公式</strong>：召回数 = 最终结果数 × 倍数<br/>
                      <strong>示例</strong>：10条 × 2倍 = 先召回20条 → 精排后返回10条<br/><br/>
                      <strong>推荐设置：</strong><br/>
                      <strong>• 1.5x</strong>：简单问答、FAQ（速度优先）<br/>
                      <strong>• 2.0x</strong>：通用场景（推荐默认）<br/>
                      <strong>• 3.0x</strong>：复杂分析、研究报告（质量优先）
                    </div>
                  </template>
                  <i class="fa-solid fa-circle-question" style="color: #909399; margin-left: 4px; cursor: help;"></i>
                </el-tooltip>
              </template>
              <el-input-number
                v-model="formData.ragRecallMultiplier"
                :min="1.0"
                :max="5.0"
                :step="0.5"
                :precision="1"
                style="width: 100%"
                :controls-position="'right'"
              >
                <template #prefix>
                  <i class="fa-solid fa-magnifying-glass-chart" style="color: #909399;"></i>
                </template>
              </el-input-number>
              <div style="font-size: 12px; color: #909399; margin-top: 5px;">
                推荐值：2.0倍（通用场景）
              </div>
              <div style="font-size: 12px; color: #e6a23c; margin-top: 3px;">
                <i class="fa-solid fa-calculator"></i>
                实际召回：{{ Math.max((formData.ragMaxResults * formData.ragRecallMultiplier), 15).toFixed(0) }}条候选
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 混合检索配置 -->
        <el-divider content-position="left">
          <i class="fa-solid fa-layer-group"></i> 混合检索配置（高级）
        </el-divider>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item>
              <template #label>
                <span>启用混合检索</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 300px; line-height: 1.6;">
                      <strong>混合检索：</strong>同时使用向量检索（语义理解）和BM25检索（关键词匹配）<br/>
                      <strong>优势：</strong>提升召回率和准确度，特别适合技术文档和专业术语<br/>
                      <strong>建议：</strong>保持启用（默认）
                    </div>
                  </template>
                  <i class="fa-solid fa-circle-question" style="color: #909399; margin-left: 4px;"></i>
                </el-tooltip>
              </template>
              <el-switch
                v-model="formData.ragEnableHybridSearch"
                active-text="启用"
                inactive-text="禁用"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item>
              <template #label>
                <span>启用查询扩展</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 300px; line-height: 1.6;">
                      <strong>查询扩展：</strong>自动添加相关词汇扩展用户查询<br/>
                      <strong>示例：</strong>"服务器" → "服务器 CPU GPU 内存 鲲鹏 昇腾"<br/>
                      <strong>优势：</strong>提升召回率，找到更多相关内容<br/>
                      <strong>建议：</strong>保持启用（默认）
                    </div>
                  </template>
                  <i class="fa-solid fa-circle-question" style="color: #909399; margin-left: 4px;"></i>
                </el-tooltip>
              </template>
              <el-switch
                v-model="formData.ragEnableQueryExpansion"
                active-text="启用"
                inactive-text="禁用"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20" v-if="formData.ragEnableHybridSearch">
          <el-col :span="12">
            <el-form-item>
              <template #label>
                <span>BM25权重</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 300px; line-height: 1.6;">
                      <strong>BM25检索：</strong>基于关键词的精确匹配<br/>
                      <strong>适用：</strong>专业术语、品牌型号、精确查询<br/>
                      <strong>范围：</strong>0.0-1.0<br/>
                      <strong>默认：</strong>0.3（30%权重）<br/>
                      <strong>建议：</strong>技术文档可提高到0.4-0.5
                    </div>
                  </template>
                  <i class="fa-solid fa-circle-question" style="color: #909399; margin-left: 4px;"></i>
                </el-tooltip>
              </template>
              <el-input-number
                v-model="formData.ragBm25Weight"
                :min="0.0"
                :max="1.0"
                :step="0.1"
                :precision="1"
                style="width: 100%;"
              />
              <div style="font-size: 12px; color: #909399; margin-top: 3px;">
                <i class="fa-solid fa-key"></i>
                关键词匹配权重：{{ (formData.ragBm25Weight * 100).toFixed(0) }}%
              </div>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item>
              <template #label>
                <span>向量权重</span>
                <el-tooltip placement="top" effect="light">
                  <template #content>
                    <div style="max-width: 300px; line-height: 1.6;">
                      <strong>向量检索：</strong>基于语义的相似度匹配<br/>
                      <strong>适用：</strong>模糊查询、概念性问题、同义表达<br/>
                      <strong>范围：</strong>0.0-1.0<br/>
                      <strong>默认：</strong>0.7（70%权重）<br/>
                      <strong>建议：</strong>保持默认或略微调整
                    </div>
                  </template>
                  <i class="fa-solid fa-circle-question" style="color: #909399; margin-left: 4px;"></i>
                </el-tooltip>
              </template>
              <el-input-number
                v-model="formData.ragVectorWeight"
                :min="0.0"
                :max="1.0"
                :step="0.1"
                :precision="1"
                style="width: 100%;"
              />
              <div style="font-size: 12px; color: #909399; margin-top: 3px;">
                <i class="fa-solid fa-brain"></i>
                语义理解权重：{{ (formData.ragVectorWeight * 100).toFixed(0) }}%
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 当前配置预览 -->
        <el-card shadow="hover" style="margin-bottom: 15px; border-left: 3px solid #409eff;">
          <template #header>
            <div style="display: flex; align-items: center;">
              <i class="fa-solid fa-eye" style="color: #409eff; margin-right: 8px;"></i>
              <strong>当前配置预览</strong>
            </div>
          </template>
          <div style="font-size: 13px; line-height: 1.8; color: #606266;">
            <div style="display: flex; gap: 30px; flex-wrap: wrap;">
              <div>
                <i class="fa-solid fa-percentage" style="color: #909399;"></i>
                相似度阈值：<strong style="color: #409eff;">{{ formData.ragMinScore }}</strong>
              </div>
              <div>
                <i class="fa-solid fa-list-check" style="color: #909399;"></i>
                最终结果数：<strong style="color: #409eff;">{{ formData.ragMaxResults }}条</strong>
              </div>
              <div>
                <i class="fa-solid fa-magnifying-glass-chart" style="color: #909399;"></i>
                召回倍数：<strong style="color: #409eff;">{{ formData.ragRecallMultiplier }}x</strong>
              </div>
              <div>
                <i class="fa-solid fa-layer-group" style="color: #909399;"></i>
                混合检索：<strong :style="{ color: formData.ragEnableHybridSearch ? '#67c23a' : '#909399' }">
                  {{ formData.ragEnableHybridSearch ? '启用' : '禁用' }}
                </strong>
              </div>
              <div>
                <i class="fa-solid fa-expand" style="color: #909399;"></i>
                查询扩展：<strong :style="{ color: formData.ragEnableQueryExpansion ? '#67c23a' : '#909399' }">
                  {{ formData.ragEnableQueryExpansion ? '启用' : '禁用' }}
                </strong>
              </div>
              <div v-if="formData.ragEnableHybridSearch">
                <i class="fa-solid fa-balance-scale" style="color: #909399;"></i>
                权重比例：<strong style="color: #409eff;">
                  向量{{ (formData.ragVectorWeight * 100).toFixed(0) }}% : BM25{{ (formData.ragBm25Weight * 100).toFixed(0) }}%
                </strong>
                <span v-if="Math.abs((formData.ragVectorWeight + formData.ragBm25Weight) - 1.0) > 0.01"
                      style="color: #e6a23c; margin-left: 8px;">
                  <i class="fa-solid fa-exclamation-triangle"></i>
                  建议两者之和为100%
                </span>
              </div>
              <div style="width: 100%; padding-top: 5px; border-top: 1px dashed #dcdfe6;">
                <i class="fa-solid fa-arrow-right" style="color: #e6a23c;"></i>
                <strong style="color: #e6a23c;">
                  将从知识库召回 {{ Math.max((formData.ragMaxResults * formData.ragRecallMultiplier), 15).toFixed(0) }} 条候选，经重排后提供 {{ formData.ragMaxResults }} 条给AI
                </strong>
              </div>
            </div>
          </div>
        </el-card>

        <!-- RAG参数综合说明 -->
        <el-alert
          type="info"
          :closable="false"
          style="margin-top: 15px;"
        >
          <template #title>
            <div style="font-size: 13px; line-height: 1.8;">
              <i class="fa-solid fa-lightbulb" style="color: #409eff;"></i>
              <strong>参数配合使用建议：</strong><br/>
              <span style="color: #606266;">
                • <strong>精准问答</strong>（客服/FAQ）：相似度0.75 + 结果5条 + 倍数1.5x<br/>
                • <strong>通用助手</strong>（推荐默认）：相似度0.5 + 结果10条 + 倍数2.0x<br/>
                • <strong>深度研究</strong>（分析/报告）：相似度0.6 + 结果15条 + 倍数3.0x<br/>
                • <strong>技术文档</strong>（专业术语）：BM25权重0.4-0.5
              </span>
            </div>
          </template>
        </el-alert>
        </div>

        <!-- 步骤5：工具配置 -->
        <div v-show="currentStep === 4">
          <div class="step-header">
            <i class="fa-solid fa-wrench"></i> Function Calling 工具配置
            <span class="step-desc">让智能体具备执行任务的能力（可选）</span>
          </div>

          <div style="background: #fdf6ec; padding: 12px; border-radius: 6px; margin-bottom: 20px; border-left: 3px solid #e6a23c;">
            <div style="font-size: 13px; color: #606266; line-height: 1.8;">
              <div><i class="fa-solid fa-circle-info" style="color: #e6a23c;"></i> <strong>Function Calling 工具调用</strong></div>
              <div style="margin-top: 6px;">
                • 让智能体具备执行任务的能力，如查询天气、搜索网络、执行计算等<br/>
                • AI会自动判断何时需要调用工具，并将结果融入回答<br/>
                • <strong style="color: #e6a23c;">工具调用优先级高于知识库检索</strong>
              </div>
            </div>
          </div>

          <el-form-item label="启用工具">
            <el-checkbox-group v-model="selectedTools">
              <el-checkbox
                v-for="tool in availableTools"
                :key="tool.id"
                :label="tool.id"
                style="margin-bottom: 12px; width: 100%;"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <i :class="'fa-solid ' + tool.icon" style="color: #e6a23c; width: 16px;"></i>
                  <span style="font-weight: 500;">{{ tool.displayName }}</span>
                  <el-tag size="small" type="info">{{ getCategoryLabel(tool.category) }}</el-tag>
                  <span style="color: #909399; font-size: 12px;">{{ tool.description }}</span>
                </div>
              </el-checkbox>
            </el-checkbox-group>
            <div v-if="availableTools.length === 0" style="color: #909399; font-size: 13px;">
              <i class="fa-solid fa-info-circle"></i> 暂无可用工具，请先在工具管理中添加工具
            </div>
          </el-form-item>

        </div>

        <!-- 步骤6：工作流配置 -->
        <div v-show="currentStep === 5">
          <div class="step-header">
            <i class="fa-solid fa-diagram-project"></i> 工作流配置
            <span class="step-desc">关联工作流，让智能体可以执行复杂任务（可选）</span>
          </div>

          <div style="background: #ecf5ff; padding: 12px; border-radius: 6px; margin-bottom: 20px; border-left: 3px solid #409eff;">
            <div style="font-size: 13px; color: #606266; line-height: 1.8;">
              <div><i class="fa-solid fa-circle-info" style="color: #409eff;"></i> <strong>工作流集成说明</strong></div>
              <div style="margin-top: 6px;">
                • 工作流可以执行复杂的多步骤任务，如数据处理、翻译、摘要等<br/>
                • 根据触发模式不同，工作流可以<strong>替代</strong>或<strong>增强</strong>正常对话<br/>
                • 如果不需要工作流功能，可以跳过此步骤
              </div>
            </div>
          </div>

          <el-form-item label="关联工作流">
            <el-select v-model="formData.workflowId" placeholder="选择工作流（可选）" clearable style="width: 100%">
              <el-option
                v-for="wf in workflowList"
                :key="wf.id"
                :label="wf.name"
                :value="wf.id"
              >
                <div style="display: flex; align-items: center; gap: 8px;">
                  <i class="fa-solid fa-diagram-project" style="color: #6366f1;"></i>
                  <span>{{ wf.name }}</span>
                  <el-tag v-if="wf.enabled" size="small" type="success">已启用</el-tag>
                  <el-tag v-else size="small" type="info">草稿</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item label="触发模式" v-if="formData.workflowId">
            <el-radio-group v-model="formData.workflowTriggerMode">
              <el-radio value="manual">手动触发</el-radio>
              <el-radio value="auto">自动增强</el-radio>
              <el-radio value="keyword">关键词触发</el-radio>
            </el-radio-group>
            <div style="font-size: 12px; color: #606266; margin-top: 8px; line-height: 1.8; background: #f5f7fa; padding: 10px; border-radius: 4px;">
              <template v-if="formData.workflowTriggerMode === 'manual'">
                <div><strong>🎯 手动触发（替代模式）</strong></div>
                <div>• 用户发送 <code style="background:#e6e6e6;padding:2px 6px;border-radius:3px;">/run</code> 或 <code style="background:#e6e6e6;padding:2px 6px;border-radius:3px;">/workflow</code> 命令时执行工作流</div>
                <div>• 工作流结果<strong style="color:#e6a23c;">直接返回</strong>，不经过知识库和工具处理</div>
              </template>
              <template v-else-if="formData.workflowTriggerMode === 'auto'">
                <div><strong>🔄 自动增强（增强模式）</strong></div>
                <div>• 每次对话都会先执行工作流进行预处理</div>
                <div>• 工作流结果作为<strong style="color:#67c23a;">上下文</strong>，继续正常对话流程</div>
                <div>• 知识库检索、工具调用等功能<strong style="color:#67c23a;">正常生效</strong></div>
              </template>
              <template v-else>
                <div><strong>🔑 关键词触发（替代模式）</strong></div>
                <div>• 用户消息包含指定关键词时执行工作流</div>
                <div>• 工作流结果<strong style="color:#e6a23c;">直接返回</strong>，不经过知识库和工具处理</div>
              </template>
            </div>
          </el-form-item>

          <el-form-item label="触发关键词" v-if="formData.workflowId && formData.workflowTriggerMode === 'keyword'">
            <el-select
              v-model="workflowKeywordsList"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入关键词后按回车添加"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <!-- 步骤7：应用发布 -->
        <div v-show="currentStep === 6">
          <div class="step-header">
            <i class="fa-solid fa-rocket"></i> 应用发布
            <span class="step-desc">将智能体发布为独立应用，可嵌入到其他网站（可选）</span>
          </div>

          <div style="background: #f0f9eb; padding: 12px; border-radius: 6px; margin-bottom: 20px; border-left: 3px solid #67c23a;">
            <div style="font-size: 13px; color: #606266; line-height: 1.8;">
              <div><i class="fa-solid fa-circle-info" style="color: #67c23a;"></i> <strong>应用发布说明</strong></div>
              <div style="margin-top: 6px;">
                • 发布后生成独立访问链接，无需登录即可使用<br/>
                • 可通过iframe嵌入到其他网站或系统<br/>
                • 发布的应用会继承智能体的所有配置（知识库、工具、工作流等）
              </div>
            </div>
          </div>

          <el-form-item label="发布应用">
            <el-switch v-model="formData.publishEnabled" />
            <div style="font-size: 12px; color: #909399; margin-top: 5px;">
              {{ formData.publishEnabled ? '已启用发布，保存后生成访问链接' : '未启用发布' }}
            </div>
          </el-form-item>

          <template v-if="formData.publishEnabled && formData.publishToken">
            <el-form-item label="访问链接">
              <el-input :value="publishUrl" readonly>
                <template #append>
                  <el-button @click="copyPublishUrl">复制</el-button>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="嵌入代码">
              <el-input
                type="textarea"
                :rows="3"
                :value="iframeCode"
                readonly
              />
              <el-button size="small" @click="copyIframeCode" style="margin-top: 8px;">
                <i class="fa-solid fa-copy"></i> 复制代码
              </el-button>
            </el-form-item>
          </template>

          <el-alert v-if="!formData.publishEnabled" type="info" :closable="false">
            <template #title>
              <div style="font-size: 13px;">
                <i class="fa-solid fa-info-circle"></i>
                启用发布后，将生成独立访问链接和嵌入代码
              </div>
            </template>
          </el-alert>

          <!-- 配置摘要 -->
          <div class="config-summary">
            <div class="summary-title">
              <i class="fa-solid fa-clipboard-list"></i> 配置摘要
            </div>
            <div class="summary-grid">
              <div class="summary-item">
                <span class="label">名称</span>
                <span class="value">{{ formData.name || '未设置' }}</span>
              </div>
              <div class="summary-item">
                <span class="label">描述</span>
                <span class="value">{{ formData.description || '未设置' }}</span>
              </div>
              <div class="summary-item">
                <span class="label">系统提示词</span>
                <span class="value" :class="{ empty: !formData.systemPrompt }">
                  {{ formData.systemPrompt ? '已配置 (' + formData.systemPrompt.length + '字)' : '未设置' }}
                </span>
              </div>
              <div class="summary-item">
                <span class="label">开场白</span>
                <span class="value" :class="{ empty: !formData.welcomeMessage }">
                  {{ formData.welcomeMessage ? '已配置' : '未设置' }}
                </span>
              </div>
              <div class="summary-item">
                <span class="label">知识库</span>
                <span class="value" :class="{ empty: selectedKnowledgeLibraries.length === 0 }">
                  {{ selectedKnowledgeLibraries.length > 0 ? '已选择 ' + selectedKnowledgeLibraries.length + ' 个' : '未选择' }}
                </span>
              </div>
              <div class="summary-item">
                <span class="label">工具</span>
                <span class="value" :class="{ empty: selectedTools.length === 0 }">
                  {{ selectedTools.length > 0 ? '已选择 ' + selectedTools.length + ' 个' : '未选择' }}
                </span>
              </div>
              <div class="summary-item">
                <span class="label">工作流</span>
                <span class="value" :class="{ empty: !formData.workflowId }">
                  {{ formData.workflowId ? '已关联' : '未关联' }}
                </span>
              </div>
              <div class="summary-item">
                <span class="label">状态</span>
                <span class="value">
                  <el-tag :type="formData.enabled ? 'success' : 'info'" size="small">
                    {{ formData.enabled ? '启用' : '禁用' }}
                  </el-tag>
                </span>
              </div>
            </div>
          </div>

          <el-alert type="success" :closable="false" style="margin-top: 20px;">
            <template #title>
              <div style="font-size: 13px;">
                <i class="fa-solid fa-check-circle"></i>
                <strong>配置完成！</strong>点击下方"{{ editMode ? '更新' : '创建' }}"按钮保存智能体配置
              </div>
            </template>
          </el-alert>
        </div>

      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDialog = false" style="margin-right: auto;">取消</el-button>
          <el-button v-if="currentStep > 0" @click="prevStep">
            <i class="fa-solid fa-arrow-left"></i> 上一步
          </el-button>
          <el-button v-if="currentStep < 6" type="primary" @click="nextStep">
            下一步 <i class="fa-solid fa-arrow-right"></i>
          </el-button>
          <el-button v-if="currentStep === 6" type="success" @click="submitForm" :loading="submitting">
            <i class="fa-solid fa-check"></i> {{ editMode ? '更新' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 统计对话框 -->
    <el-dialog
      v-model="showStatsDialog"
      :title="'📊 ' + (currentStatsAgent?.name || '') + ' - 使用统计'"
      width="700px"
      destroy-on-close
    >
      <div v-loading="statsLoading" class="stats-container">
        <!-- 统计卡片 -->
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
              <i class="fa-solid fa-comments"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ agentStats.sessionCount || 0 }}</div>
              <div class="stat-label">会话数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
              <i class="fa-solid fa-message"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ agentStats.messageCount || 0 }}</div>
              <div class="stat-label">消息数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
              <i class="fa-solid fa-coins"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatTokens(agentStats.totalTokens) }}</div>
              <div class="stat-label">Token消耗</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);">
              <i class="fa-solid fa-clock"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ agentStats.avgResponseTime || 0 }}ms</div>
              <div class="stat-label">平均响应</div>
            </div>
          </div>
        </div>

        <!-- 最近会话 -->
        <div class="recent-sessions">
          <h4><i class="fa-solid fa-history"></i> 最近会话</h4>
          <el-table :data="agentSessions" style="width: 100%" max-height="300" empty-text="暂无会话记录">
            <el-table-column prop="session_id" label="会话ID" width="180">
              <template #default="{ row }">
                <span style="font-family: monospace; font-size: 12px;">{{ row.session_id?.substring(0, 16) }}...</span>
              </template>
            </el-table-column>
            <el-table-column prop="message_count" label="消息数" width="80" align="center" />
            <el-table-column prop="total_tokens" label="Token" width="100" align="center">
              <template #default="{ row }">
                {{ formatTokens(row.total_tokens) }}
              </template>
            </el-table-column>
            <el-table-column prop="last_time" label="最后活跃" width="160">
              <template #default="{ row }">
                {{ formatTime(row.last_time) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- API信息 -->
        <div v-if="currentStatsAgent?.apiEnabled" class="api-info">
          <h4><i class="fa-solid fa-key"></i> API访问</h4>
          <div class="api-key-display">
            <span class="label">API Key：</span>
            <span class="api-key">{{ showApiKey ? currentStatsAgent.apiKey : '••••••••••••••••••••••••' }}</span>
            <el-button size="small" @click="showApiKey = !showApiKey">
              <i :class="showApiKey ? 'fa-solid fa-eye-slash' : 'fa-solid fa-eye'"></i>
            </el-button>
            <el-button size="small" @click="copyApiKey">
              <i class="fa-solid fa-copy"></i>
            </el-button>
          </div>

          <el-divider />

          <div class="api-docs">
            <h5>📖 接口文档</h5>

            <div class="api-section">
              <div class="api-title">流式对话</div>
              <code class="api-url">POST /cms/ai/agent/{{ currentStatsAgent.id }}/chat</code>
            </div>

            <div class="api-section">
              <div class="api-title">同步对话</div>
              <code class="api-url">POST /cms/ai/agent/{{ currentStatsAgent.id }}/chat/sync</code>
            </div>

            <div class="api-section">
              <div class="api-title">请求示例</div>
              <pre class="code-block">curl -X POST "{{ apiBaseUrl }}/cms/ai/agent/{{ currentStatsAgent.id }}/chat/sync" \
  -H "Authorization: Bearer {{ currentStatsAgent.apiKey || 'YOUR_API_KEY' }}" \
  -H "Content-Type: application/json" \
  -d '{"message": "你好", "sessionId": "可选会话ID"}'</pre>
            </div>

            <div class="api-section">
              <div class="api-title">响应格式</div>
              <pre class="code-block">{
  "success": true,
  "data": {
    "sessionId": "会话ID",
    "message": "AI回复内容"
  }
}</pre>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showStatsDialog = false">关闭</el-button>
        <el-popconfirm
          title="确定要清空该智能体的所有对话历史吗？"
          confirm-button-text="确定"
          cancel-button-text="取消"
          @confirm="clearAgentHistory"
        >
          <template #reference>
            <el-button type="danger">
              <i class="fa-solid fa-trash"></i> 清空历史
            </el-button>
          </template>
        </el-popconfirm>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const router = useRouter()

const agentList = ref([])
const knowledgeBaseList = ref([])
const knowledgeLibraryList = ref([])  // 新版知识库列表
const availableDictionaries = ref([])
const availableTools = ref([])
const loading = ref(false)
const showCreateDialog = ref(false)
const submitting = ref(false)
const editMode = ref(false)
const selectedKnowledgeBases = ref([])
const selectedKnowledgeLibraries = ref([])  // 新版知识库选择
const selectedDictionaries = ref([])
const selectedTools = ref([])
const generatingPrompt = ref(false)
const currentStep = ref(0)  // 当前步骤（0-6）
const suggestedQuestionsList = ref([])  // 预设问题列表
const knowledgeBaseWeights = ref({})  // 知识库权重配置 { "知识库ID": 权重值 }
const knowledgeBaseEnabled = ref({})  // 知识库启用状态 { "知识库ID": true/false }

// 工作流相关
const workflowList = ref([])
const workflowKeywordsList = ref([])

// 发布相关
const publishUrl = computed(() => {
  if (!formData.value.publishToken) return ''
  return `${window.location.origin}/app/${formData.value.publishToken}`
})
const iframeCode = computed(() => {
  if (!formData.value.publishToken) return ''
  return `<iframe src="${publishUrl.value}" width="400" height="600" frameborder="0"></iframe>`
})

// 模型配置列表
const modelConfigs = ref([])

// 统计相关
const showStatsDialog = ref(false)
const statsLoading = ref(false)
const currentStatsAgent = ref(null)
const agentStats = ref({})
const agentSessions = ref([])
const showApiKey = ref(false)

// API基础URL
const apiBaseUrl = computed(() => window.location.origin)

// 搜索相关
const searchKeyword = ref('')
const searchStatus = ref(null)

// 分页相关
const currentPage = ref(1)
const pageSize = ref(9)

// 过滤后的列表
const filteredAgents = computed(() => {
  let result = agentList.value
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(agent =>
      agent.name?.toLowerCase().includes(keyword) ||
      agent.description?.toLowerCase().includes(keyword)
    )
  }
  if (searchStatus.value !== null && searchStatus.value !== '') {
    result = result.filter(agent => agent.enabled === searchStatus.value)
  }
  return result
})

const formData = ref({
  id: null,
  name: '',
  description: '',
  systemPrompt: '',
  knowledgeBaseIds: '',
  temperature: 0.7,
  maxTokens: 2000,
  ragMinScore: 0.5,
  ragMaxResults: 10,
  ragRecallMultiplier: 2.0,
  ragEnableHybridSearch: true,
  ragEnableQueryExpansion: true,
  ragBm25Weight: 0.3,
  ragVectorWeight: 0.7,
  modelConfigId: null,
  enabled: true,
  // 扩展字段
  welcomeMessage: '',
  suggestedQuestions: '',
  showCitations: true,
  maxHistoryTurns: 10,
  apiEnabled: false,
  apiKey: '',
  // 工作流
  workflowId: null,
  workflowTriggerMode: 'manual',
  workflowTriggerKeywords: '',
  // 发布
  publishEnabled: false,
  publishToken: '',
  publishSettings: '',
  // 知识库权重
  knowledgeBaseWeights: ''
})

// 获取知识库名称
const getKnowledgeLibraryName = (libId) => {
  const lib = knowledgeLibraryList.value.find(l => l.id === libId)
  return lib ? lib.name : `知识库 ${libId}`
}

// 获取权重标签文本
const getWeightLabel = (weight) => {
  if (weight === 0 || weight === 0.0) return '已禁用'
  if (weight >= 0.9) return '高优先级'
  if (weight >= 0.7) return '中优先级'
  return '低优先级'
}

// 获取权重标签类型
const getWeightTagType = (weight) => {
  if (weight === 0 || weight === 0.0) return 'info'
  if (weight >= 0.9) return 'danger'
  if (weight >= 0.7) return 'warning'
  return 'success'
}

// 应用权重模板
const applyWeightTemplate = (template) => {
  const libs = selectedKnowledgeLibraries.value
  
  if (libs.length === 0) {
    ElMessage.warning('请先选择知识库')
    return
  }
  
  switch(template) {
    case 'equal':
      // 平等权重：所有知识库1.0
      libs.forEach(libId => {
        knowledgeBaseWeights.value[libId] = 1.0
        knowledgeBaseEnabled.value[libId] = true
      })
      ElMessage.success('已应用平等权重模板（所有知识库权重1.0）')
      break
      
    case 'primary-secondary':
      // 主次分明：第一个1.0，其他0.7
      libs.forEach((libId, index) => {
        knowledgeBaseWeights.value[libId] = index === 0 ? 1.0 : 0.7
        knowledgeBaseEnabled.value[libId] = true
      })
      ElMessage.success('已应用主次分明模板（主要1.0，次要0.7）')
      break
      
    case 'core-reference':
      // 核心+参考：第一个1.0，其他0.5
      libs.forEach((libId, index) => {
        knowledgeBaseWeights.value[libId] = index === 0 ? 1.0 : 0.5
        knowledgeBaseEnabled.value[libId] = true
      })
      ElMessage.success('已应用核心+参考模板（核心1.0，参考0.5）')
      break
      
    case 'core-secondary-archive':
      // 核心+次要+归档：1.0, 0.7, 0.3
      const weights = [1.0, 0.7, 0.3]
      libs.forEach((libId, index) => {
        knowledgeBaseWeights.value[libId] = weights[Math.min(index, 2)]
        knowledgeBaseEnabled.value[libId] = true
      })
      ElMessage.success('已应用三级权重模板（核心1.0，次要0.7，归档0.3）')
      break
  }
}

// 切换知识库启用状态
const toggleKnowledgeBase = (libId) => {
  if (knowledgeBaseEnabled.value[libId]) {
    // 启用：恢复之前的权重或设为1.0
    if (!knowledgeBaseWeights.value[libId] || knowledgeBaseWeights.value[libId] === 0) {
      knowledgeBaseWeights.value[libId] = 1.0
    }
  } else {
    // 禁用：设置权重为0
    knowledgeBaseWeights.value[libId] = 0.0
  }
}

// 更新知识库权重配置
const updateKnowledgeBaseWeights = () => {
  // 当权重值改变时，自动触发（无需额外操作）
  console.log('知识库权重已更新:', knowledgeBaseWeights.value)
}

// 监听知识库选择变化，自动初始化权重
watch(selectedKnowledgeLibraries, (newLibs, oldLibs) => {
  // 为新添加的知识库设置默认权重 1.0
  newLibs.forEach(libId => {
    if (!knowledgeBaseWeights.value[libId]) {
      knowledgeBaseWeights.value[libId] = 1.0
      knowledgeBaseEnabled.value[libId] = true
    }
  })
  
  // 移除已取消选择的知识库权重
  Object.keys(knowledgeBaseWeights.value).forEach(libId => {
    if (!newLibs.includes(parseInt(libId))) {
      delete knowledgeBaseWeights.value[libId]
      delete knowledgeBaseEnabled.value[libId]
    }
  })
})

// 加载智能体列表
const loadAgentList = async () => {
  loading.value = true
  try {
    const response = await request({ url: '/cms/ai/agent/list', method: 'get' })
    // 后端返回的是 ListResponse 格式：{ list: [], total: n }
    agentList.value = response.data?.list || []
  } catch (error) {
    console.error('加载智能体列表失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载知识库列表（新版：知识库主表）
const loadKnowledgeLibraryList = async () => {
  try {
    const response = await request({ url: '/cms/ai/knowledge-library/all', method: 'get' })
    knowledgeLibraryList.value = response.data || []
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

// 加载知识库列表（旧版：兼容保留）
const loadKnowledgeBaseList = async () => {
  try {
    const response = await request({ url: '/cms/ai/knowledge-base/list', method: 'get' })
    const listData = response.data?.list || []
    knowledgeBaseList.value = listData.filter(kb => kb.status === 2)
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

// 加载工作流列表
const loadWorkflowList = async () => {
  try {
    const response = await request({ url: '/cms/ai/workflow/list', method: 'get' })
    workflowList.value = response.data || []
  } catch (error) {
    console.error('加载工作流列表失败:', error)
  }
}

// 加载模型配置列表（只加载对话模型）
const loadModelConfigs = async () => {
  try {
    const response = await request({ url: '/cms/ai/model-config/list', method: 'get' })
    // 只过滤出对话模型（chat类型）
    const allConfigs = response.data?.list || []
    modelConfigs.value = allConfigs.filter(config => config.modelType === 'chat')
  } catch (error) {
    console.error('加载模型配置列表失败:', error)
  }
}

// 复制发布链接
const copyPublishUrl = () => {
  navigator.clipboard.writeText(publishUrl.value)
  ElMessage.success('已复制访问链接')
}

// 复制嵌入代码
const copyIframeCode = () => {
  navigator.clipboard.writeText(iframeCode.value)
  ElMessage.success('已复制嵌入代码')
}

// 加载可用的专业词典
const loadAvailableDictionaries = async () => {
  try {
    const response = await request({ url: '/cms/ai/agent/available-dictionaries', method: 'get' })
    availableDictionaries.value = response.data || []
  } catch (error) {
    console.error('加载可用词典失败:', error)
  }
}

// 加载可用的工具列表
const loadAvailableTools = async () => {
  try {
    const response = await request({ url: '/cms/ai/tool/enabled', method: 'get' })
    availableTools.value = response.data || []
  } catch (error) {
    console.error('加载可用工具失败:', error)
  }
}

// 开始对话（在新标签页打开纯净聊天界面）
const startChat = (agentId) => {
  console.log('在新标签页打开对话页面，智能体ID:', agentId)
  // 使用router.resolve生成完整URL，自动包含base路径
  const { href } = router.resolve({
    path: '/chat',
    query: { agentId, hideMenu: 'true' }
  })
  const fullUrl = window.location.origin + href
  window.open(fullUrl, '_blank')
}

// 预设问题操作
const addSuggestedQuestion = () => {
  if (suggestedQuestionsList.value.length < 5) {
    suggestedQuestionsList.value.push('')
  }
}

const removeSuggestedQuestion = (index) => {
  suggestedQuestionsList.value.splice(index, 1)
}

// 生成API Key
const generateApiKey = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let key = 'sk-'
  for (let i = 0; i < 32; i++) {
    key += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  formData.value.apiKey = key
}

// 编辑智能体
const editAgent = async (agent) => {
  editMode.value = true
  formData.value = {
    ...agent,
    // 为新字段提供默认值（兼容旧数据）
    ragEnableHybridSearch: agent.ragEnableHybridSearch ?? true,
    ragEnableQueryExpansion: agent.ragEnableQueryExpansion ?? true,
    ragBm25Weight: agent.ragBm25Weight ?? 0.3,
    ragVectorWeight: agent.ragVectorWeight ?? 0.7,
    welcomeMessage: agent.welcomeMessage || '',
    showCitations: agent.showCitations ?? true,
    maxHistoryTurns: agent.maxHistoryTurns ?? 10,
    apiEnabled: agent.apiEnabled ?? false,
    apiKey: agent.apiKey || '',
    // 工作流
    workflowId: agent.workflowId || null,
    workflowTriggerMode: agent.workflowTriggerMode || 'manual',
    workflowTriggerKeywords: agent.workflowTriggerKeywords || '',
    // 发布
    publishEnabled: agent.publishEnabled ?? false,
    publishToken: agent.publishToken || '',
    publishSettings: agent.publishSettings || ''
  }

  // 解析预设问题
  if (agent.suggestedQuestions) {
    try {
      suggestedQuestionsList.value = JSON.parse(agent.suggestedQuestions)
    } catch {
      suggestedQuestionsList.value = []
    }
  } else {
    suggestedQuestionsList.value = []
  }

  // 解析工作流触发关键词
  if (agent.workflowTriggerKeywords) {
    try {
      workflowKeywordsList.value = JSON.parse(agent.workflowTriggerKeywords)
    } catch {
      workflowKeywordsList.value = []
    }
  } else {
    workflowKeywordsList.value = []
  }

  // 解析知识库ID（新版）
  console.log('📚 编辑智能体 - 原始知识库数据:', agent.knowledgeLibraryIds)
  if (agent.knowledgeLibraryIds) {
    try {
      // 如果已经是数组，直接使用；如果是字符串，解析JSON
      if (Array.isArray(agent.knowledgeLibraryIds)) {
        selectedKnowledgeLibraries.value = agent.knowledgeLibraryIds
      } else if (typeof agent.knowledgeLibraryIds === 'string') {
        selectedKnowledgeLibraries.value = JSON.parse(agent.knowledgeLibraryIds)
      } else {
        selectedKnowledgeLibraries.value = []
      }
      console.log('✅ 解析后的知识库IDs:', selectedKnowledgeLibraries.value)
    } catch (e) {
      console.error('❌ 解析知识库IDs失败:', e)
      selectedKnowledgeLibraries.value = []
    }
  } else {
    console.log('⚠️ 知识库IDs为空')
    selectedKnowledgeLibraries.value = []
  }
  
  // 解析知识库权重配置
  console.log('⚖️ 编辑智能体 - 原始权重数据:', agent.knowledgeBaseWeights)
  if (agent.knowledgeBaseWeights) {
    try {
      const weights = typeof agent.knowledgeBaseWeights === 'string' 
        ? JSON.parse(agent.knowledgeBaseWeights) 
        : agent.knowledgeBaseWeights
      
      knowledgeBaseWeights.value = weights
      console.log('✅ 解析后的权重配置:', knowledgeBaseWeights.value)
      
      // 初始化启用状态
      knowledgeBaseEnabled.value = {}
      for (const libId in weights) {
        knowledgeBaseEnabled.value[libId] = weights[libId] > 0
      }
    } catch (e) {
      console.error('❌ 解析权重配置失败:', e)
      knowledgeBaseWeights.value = {}
      knowledgeBaseEnabled.value = {}
    }
  } else {
    console.log('⚠️ 权重配置为空，使用默认值')
    // 如果没有权重配置，为所有知识库设置默认权重 1.0
    knowledgeBaseWeights.value = {}
    knowledgeBaseEnabled.value = {}
    selectedKnowledgeLibraries.value.forEach(libId => {
      knowledgeBaseWeights.value[libId] = 1.0
      knowledgeBaseEnabled.value[libId] = true
    })
  }
  
  // 解析知识库ID（旧版兼容）
  if (agent.knowledgeBaseIds) {
    selectedKnowledgeBases.value = agent.knowledgeBaseIds.split(',').map(id => parseInt(id.trim()))
  } else {
    selectedKnowledgeBases.value = []
  }

  // 加载智能体关联的词典
  try {
    const response = await request({ url: `/cms/ai/agent/${agent.id}/dictionaries`, method: 'get' })
    selectedDictionaries.value = response.data || []
  } catch (error) {
    console.error('加载智能体词典失败:', error)
    selectedDictionaries.value = []
  }

  // 加载智能体关联的工具
  try {
    const response = await request({ url: `/cms/ai/tool/agent/${agent.id}/ids`, method: 'get' })
    selectedTools.value = response.data || []
  } catch (error) {
    console.error('加载智能体工具失败:', error)
    selectedTools.value = []
  }

  currentStep.value = 0  // 编辑时从第一步开始
  showCreateDialog.value = true
}

// 删除智能体
const deleteAgent = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除这个智能体吗？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    const response = await request({ url: `/cms/ai/agent/${id}`, method: 'delete'})
    ElMessage.success('删除成功')
    await loadAgentList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 显示统计
const showAgentStats = async (agent) => {
  currentStatsAgent.value = agent
  showStatsDialog.value = true
  showApiKey.value = false
  statsLoading.value = true

  try {
    // 获取统计数据
    const [statsRes, sessionsRes] = await Promise.all([
      request({ url: `/cms/ai/agent/${agent.id}/stats`, method: 'get' }),
      request({ url: `/cms/ai/agent/${agent.id}/sessions?limit=10`, method: 'get' })
    ])

    agentStats.value = {
      sessionCount: statsRes.data?.session_count || 0,
      messageCount: statsRes.data?.message_count || 0,
      totalTokens: statsRes.data?.total_tokens || 0,
      avgResponseTime: Math.round(statsRes.data?.avg_response_time || 0)
    }

    agentSessions.value = sessionsRes.data || []
  } catch (error) {
    console.error('获取统计失败:', error)
    ElMessage.error('获取统计失败')
  } finally {
    statsLoading.value = false
  }
}

// 复制智能体
const duplicateAgent = async (agent) => {
  try {
    const newAgent = {
      ...agent,
      id: null,
      name: agent.name + ' (副本)',
      createTime: null,
      updateTime: null
    }

    const response = await request({ url: '/cms/ai/agent/create', method: 'post', data: newAgent})
    ElMessage.success('复制成功')
    await loadAgentList()
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败')
  }
}

// 清空智能体历史
const clearAgentHistory = async () => {
  if (!currentStatsAgent.value) return

  try {
    const response = await request({ url: `/cms/ai/agent/${currentStatsAgent.value.id}/history`, method: 'delete'})
    ElMessage.success('清空成功')
    agentStats.value = { sessionCount: 0, messageCount: 0, totalTokens: 0, avgResponseTime: 0 }
    agentSessions.value = []
    await loadAgentList()
  } catch (error) {
    console.error('清空失败:', error)
    ElMessage.error('清空失败')
  }
}

// 复制API Key
const copyApiKey = () => {
  if (currentStatsAgent.value?.apiKey) {
    navigator.clipboard.writeText(currentStatsAgent.value.apiKey)
    ElMessage.success('已复制到剪贴板')
  }
}

// 格式化Token数量
const formatTokens = (tokens) => {
  if (!tokens) return '0'
  if (tokens >= 1000000) return (tokens / 1000000).toFixed(1) + 'M'
  if (tokens >= 1000) return (tokens / 1000).toFixed(1) + 'K'
  return tokens.toString()
}

// 导入导出处理
const handleExportImport = (command) => {
  if (command === 'export') {
    exportAgents()
  } else if (command === 'import') {
    importAgents()
  }
}

// 导出智能体
const exportAgents = () => {
  if (agentList.value.length === 0) {
    ElMessage.warning('没有可导出的智能体')
    return
  }

  // 准备导出数据（去除ID和时间戳）
  const exportData = agentList.value.map(agent => ({
    name: agent.name,
    description: agent.description,
    systemPrompt: agent.systemPrompt,
    temperature: agent.temperature,
    maxTokens: agent.maxTokens,
    ragMinScore: agent.ragMinScore,
    ragMaxResults: agent.ragMaxResults,
    ragRecallMultiplier: agent.ragRecallMultiplier,
    ragEnableHybridSearch: agent.ragEnableHybridSearch,
    ragEnableQueryExpansion: agent.ragEnableQueryExpansion,
    ragBm25Weight: agent.ragBm25Weight,
    ragVectorWeight: agent.ragVectorWeight,
    enabled: agent.enabled,
    welcomeMessage: agent.welcomeMessage,
    suggestedQuestions: agent.suggestedQuestions,
    showCitations: agent.showCitations,
    maxHistoryTurns: agent.maxHistoryTurns
  }))

  const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `agents_export_${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)

  ElMessage.success(`已导出 ${exportData.length} 个智能体`)
}

// 导入智能体
const importAgents = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    try {
      const text = await file.text()
      const agents = JSON.parse(text)

      if (!Array.isArray(agents)) {
        ElMessage.error('文件格式错误')
        return
      }

      let successCount = 0
      for (const agent of agents) {
        try {
          const response = await request({ url: '/cms/ai/agent/create', method: 'post'})
          successCount++
        } catch (err) {
          console.error('导入智能体失败:', err)
        }
      }

      ElMessage.success(`成功导入 ${successCount} 个智能体`)
      await loadAgentList()
    } catch (err) {
      console.error('解析文件失败:', err)
      ElMessage.error('文件解析失败')
    }
  }
  input.click()
}

// 提交表单
const submitForm = async () => {
  if (!formData.value.name) {
    ElMessage.warning('请输入智能体名称')
    return
  }

  submitting.value = true

  // 将选中的知识库ID转换为JSON数组（新版）
  formData.value.knowledgeLibraryIds = selectedKnowledgeLibraries.value.length > 0 
    ? JSON.stringify(selectedKnowledgeLibraries.value) : ''
  
  // 将知识库权重转换为JSON（只保存已选择的知识库权重）
  const activeWeights = {}
  selectedKnowledgeLibraries.value.forEach(libId => {
    activeWeights[libId] = knowledgeBaseWeights.value[libId] || 1.0
  })
  formData.value.knowledgeBaseWeights = Object.keys(activeWeights).length > 0
    ? JSON.stringify(activeWeights) : ''
  
  // 兼容旧版：将选中的知识库ID转换为逗号分隔的字符串
  formData.value.knowledgeBaseIds = selectedKnowledgeBases.value.join(',')

  // 将预设问题列表转为JSON
  const validQuestions = suggestedQuestionsList.value.filter(q => q && q.trim())
  formData.value.suggestedQuestions = validQuestions.length > 0 ? JSON.stringify(validQuestions) : ''

  // 将工作流触发关键词转为JSON
  formData.value.workflowTriggerKeywords = workflowKeywordsList.value.length > 0
    ? JSON.stringify(workflowKeywordsList.value) : ''

  // 如果启用发布但没有Token，生成一个
  if (formData.value.publishEnabled && !formData.value.publishToken) {
    formData.value.publishToken = 'pub-' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
  }
  // 如果禁用发布，清空Token
  if (!formData.value.publishEnabled) {
    formData.value.publishToken = ''
  }

  try {
    const url = editMode.value ? '/cms/ai/agent/update' : '/cms/ai/agent/create'
    const method = editMode.value ? 'put' : 'post'
    const response = await request({ url, method, data: formData.value })

    // 保存成功后，更新词典关联
    const agentId = editMode.value ? formData.value.id : response.data.id
    if (agentId) {
      try {
        await request({ url: `/cms/ai/agent/${agentId}/dictionaries`, method: 'post', data: selectedDictionaries.value})
      } catch (dictError) {
        console.error('更新词典关联失败:', dictError)
      }

      // 更新工具关联
      try {
        await request({ url: `/cms/ai/tool/agent/${agentId}/bind`, method: 'post', data: selectedTools.value})
      } catch (toolError) {
        console.error('更新工具关联失败:', toolError)
      }
    }

    ElMessage.success(editMode.value ? '更新成功' : '创建成功')
    showCreateDialog.value = false
    resetForm()
    await loadAgentList()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// AI生成系统提示词
const generateSystemPrompt = async () => {
  // 检查是否有描述或提示词内容
  const inputText = formData.value.systemPrompt || formData.value.description

  if (!inputText || inputText.trim().length === 0) {
    ElMessage.warning('请先输入智能体描述或简单的系统提示词，AI将帮你润色成专业版本')
    return
  }

  generatingPrompt.value = true

  try {
    const response = await request({
      url: '/cms/ai/prompt/generate',
      method: 'post',
      data: { description: inputText }
    })

    // request 拦截器返回 AjaxResult {code, msg, data}，data 即 PromptGenerateResponse
    formData.value.systemPrompt = response.data.systemPrompt
    ElMessage.success('✨ 系统提示词生成成功！')
  } catch (error) {
    console.error('生成系统提示词失败:', error)
    // request 拦截器已统一错误提示，这里不重复 ElMessage
  } finally {
    generatingPrompt.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: null,
    name: '',
    description: '',
    systemPrompt: '',
    knowledgeBaseIds: '',
    knowledgeLibraryIds: '',
    temperature: 0.7,
    maxTokens: 2000,
    ragMinScore: 0.5,
    ragMaxResults: 10,
    ragRecallMultiplier: 2.0,
    ragEnableHybridSearch: true,
    ragEnableQueryExpansion: true,
    ragBm25Weight: 0.3,
    ragVectorWeight: 0.7,
    enabled: true,
    // 扩展字段
    welcomeMessage: '',
    suggestedQuestions: '',
    showCitations: true,
    maxHistoryTurns: 10,
    apiEnabled: false,
    apiKey: '',
    // 工作流
    workflowId: null,
    workflowTriggerMode: 'manual',
    workflowTriggerKeywords: '',
    // 发布
    publishEnabled: false,
    publishToken: '',
    publishSettings: '',
    // 知识库权重
    knowledgeBaseWeights: ''
  }
  selectedKnowledgeBases.value = []
  selectedKnowledgeLibraries.value = []
  selectedDictionaries.value = []
  selectedTools.value = []
  suggestedQuestionsList.value = []
  workflowKeywordsList.value = []
  knowledgeBaseWeights.value = {}  // 清空知识库权重
  currentStep.value = 0
  editMode.value = false
}

// 分页数据（基于过滤后的列表）
const paginatedAgents = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredAgents.value.slice(start, end)
})

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

// 获取提示词预览（截取前80个字符，约2行）
const getPromptPreview = (prompt) => {
  if (!prompt) return ''
  const cleanPrompt = prompt.trim()
  if (cleanPrompt.length <= 80) {
    return cleanPrompt
  }
  return cleanPrompt.substring(0, 80) + '...'
}

// 获取开场白预览
const getWelcomePreview = (welcome) => {
  if (!welcome) return ''
  const clean = welcome.trim()
  if (clean.length <= 40) return clean
  return clean.substring(0, 40) + '...'
}

// 获取知识库数量（支持新旧两种格式）
const getKnowledgeCount = (agent) => {
  // 优先使用新版格式
  if (agent.knowledgeLibraryIds) {
    try {
      const ids = JSON.parse(agent.knowledgeLibraryIds)
      return Array.isArray(ids) ? ids.length : 0
    } catch {
      return 0
    }
  }
  // 兼容旧版格式
  if (agent.knowledgeBaseIds) {
    return agent.knowledgeBaseIds.split(',').filter(id => id.trim()).length
  }
  return 0
}

// 获取预设问题数量
const getSuggestedQuestionsCount = (agent) => {
  if (!agent.suggestedQuestions) return 0
  try {
    const questions = JSON.parse(agent.suggestedQuestions)
    return Array.isArray(questions) ? questions.length : 0
  } catch {
    return 0
  }
}

// 工具分类标签转换
const getCategoryLabel = (category) => {
  const map = {
    'utility': '实用工具',
    'information': '信息查询',
    'action': '执行操作',
    'data': '数据查询',
    'general': '通用'
  }
  return map[category] || category
}

// 步骤导航
const nextStep = () => {
  // 步骤1验证：名称和提示词必填
  if (currentStep.value === 0) {
    if (!formData.value.name?.trim()) {
      ElMessage.warning('请输入智能体名称')
      return
    }
    if (!formData.value.systemPrompt?.trim()) {
      ElMessage.warning('请输入系统提示词')
      return
    }
  }
  if (currentStep.value < 6) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

// 监听对话框关闭
watch(showCreateDialog, (newVal) => {
  if (!newVal) {
    resetForm()
  }
})

onMounted(() => {
  loadAgentList()
  loadKnowledgeLibraryList()  // 新版知识库
  loadKnowledgeBaseList()     // 旧版兼容
  loadModelConfigs()
  loadAvailableDictionaries()
  loadAvailableTools()
  loadWorkflowList()
})
</script>

<style scoped src="@/views/ai/styles/agent-manage.css"></style>
<style src="@/views/ai/styles/agent-manage-global.css"></style>
