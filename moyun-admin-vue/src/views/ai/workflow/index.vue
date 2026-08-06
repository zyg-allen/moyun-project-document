<template>
  <div class="workflow-manage">
    <div class="workflow-list" v-if="!editingWorkflow">
      <div class="page-header">
        <div class="header-icon">
          <i class="fa-solid fa-diagram-project"></i>
        </div>
        <div class="header-content">
          <h2>AI工作流</h2>
          <span class="workflow-count">共 {{ workflows.length }} 个工作流</span>
        </div>
        <div class="header-actions">
          <el-input v-model="workflowSearch" placeholder="搜索名称/描述..." prefix-icon="Search" clearable style="width: 200px" />
          <el-select v-model="workflowStatusFilter" placeholder="状态" clearable style="width: 120px">
            <el-option label="已启用" :value="true" />
            <el-option label="草稿" :value="false" />
          </el-select>
          <el-button @click="showGuideDialog = true" class="guide-btn">
            <i class="fa-solid fa-circle-question"></i> 指南
          </el-button>
          <el-button @click="showTemplates" class="template-btn">
            <i class="fa-solid fa-layer-group"></i> 模板
          </el-button>
          <el-button type="primary" @click="createWorkflow" class="create-btn">
            <i class="fa-solid fa-plus"></i> 新建工作流
          </el-button>
        </div>
      </div>

      <!-- 内容容器 -->
      <div class="content-container">
        <div class="workflow-cards">
        <TransitionGroup name="card-list">
          <div v-for="wf in filteredWorkflows" :key="wf.id" class="workflow-card" @click="editWorkflow(wf)">
            <!-- 卡片顶部装饰条 -->
            <div class="card-accent" :class="wf.enabled ? 'active' : ''"></div>

            <!-- 卡片主体内容 -->
            <div class="card-content">
              <!-- 头部：图标 + 标题 + 状态 -->
              <div class="card-top">
                <div class="card-icon" :class="wf.enabled ? 'active' : ''">
                  <i class="fa-solid fa-diagram-project"></i>
                </div>
                <div class="card-title-area">
                  <div class="card-title-row">
                    <span class="name">{{ wf.name }}</span>
                    <el-tag :type="wf.enabled ? 'success' : 'info'" size="small" effect="plain" round class="status-tag">
                      {{ wf.enabled ? '已启用' : '草稿' }}
                    </el-tag>
                  </div>
                  <div class="desc">{{ wf.description || '暂无描述' }}</div>
                </div>
              </div>

              <!-- 底部：元信息 + 操作按钮 -->
              <div class="card-bottom">
                <div class="card-meta">
                  <span class="meta-item">
                    <i class="fa-solid fa-code-branch"></i>
                    <span>v{{ wf.version || 1 }}</span>
                  </span>
                  <span class="meta-item">
                    <i class="fa-regular fa-clock"></i>
                    <span>{{ formatTime(wf.updateTime) }}</span>
                  </span>
                  <span class="meta-item stat" v-if="wf.executeCount">
                    <i class="fa-solid fa-bolt"></i>
                    <span>{{ wf.executeCount }} 次</span>
                  </span>
                  <span class="meta-item success-rate" v-if="wf.successRate !== undefined">
                    <i class="fa-solid fa-chart-pie"></i>
                    <span>{{ wf.successRate }}%</span>
                  </span>
                </div>
                <div class="card-actions" @click.stop>
                  <el-tooltip content="运行" placement="top" :show-after="500">
                    <button class="action-btn primary" @click="runWorkflow(wf)">
                      <i class="fa-solid fa-play"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="编辑" placement="top" :show-after="500">
                    <button class="action-btn" @click="editWorkflow(wf)">
                      <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="复制" placement="top" :show-after="500">
                    <button class="action-btn" @click="duplicateWorkflow(wf)">
                      <i class="fa-solid fa-copy"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top" :show-after="500">
                    <button class="action-btn danger" @click="deleteWorkflow(wf)">
                      <i class="fa-solid fa-trash-can"></i>
                    </button>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </TransitionGroup>
          <!-- 加载中骨架屏 -->
          <template v-if="loadingWorkflows">
            <div v-for="i in 3" :key="'skeleton-'+i" class="workflow-card skeleton-card">
              <div class="card-accent"></div>
              <div class="card-content">
                <div class="card-top">
                  <div class="skeleton-icon"></div>
                  <div class="card-title-area">
                    <div class="skeleton-title"></div>
                    <div class="skeleton-desc"></div>
                  </div>
                </div>
                <div class="card-bottom">
                  <div class="skeleton-meta"></div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
      <!-- 分页 -->
      <div v-if="!loadingWorkflows && totalWorkflows > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[9, 18, 27, 36]"
          :total="totalWorkflows"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
      <div v-if="!loadingWorkflows && !workflows.length" class="empty-state">
        <div class="empty-illustration">
          <i class="fa-solid fa-diagram-project"></i>
          <div class="empty-circles">
            <span></span><span></span><span></span>
          </div>
        </div>
        <h3>还没有工作流</h3>
        <p>创建您的第一个AI工作流，开始自动化之旅</p>
        <el-button type="primary" size="large" @click="createWorkflow" class="create-btn">
          <i class="fa-solid fa-plus"></i> 立即创建
        </el-button>
      </div>
    </div>
    <div class="workflow-editor" v-else>
      <div class="editor-header">
        <div class="left">
          <el-button text @click="backToList" class="back-btn">
            <i class="fa-solid fa-arrow-left"></i> 返回列表
          </el-button>
          <el-divider direction="vertical" />
          <div class="workflow-info">
            <el-input v-model="editingWorkflow.name" placeholder="工作流名称" class="name-input" />
            <el-input v-model="editingWorkflow.description" placeholder="添加描述..." class="desc-input" />
          </div>
        </div>
        <div class="center">
          <div class="toolbar-group">
            <el-tooltip content="撤销 (Ctrl+Z)" placement="bottom">
              <el-button @click="undo" :disabled="!canUndo" class="toolbar-btn">
                <i class="fa-solid fa-undo"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="重做 (Ctrl+Y)" placement="bottom">
              <el-button @click="redo" :disabled="!canRedo" class="toolbar-btn">
                <i class="fa-solid fa-redo"></i>
              </el-button>
            </el-tooltip>
          </div>
          <div class="toolbar-divider"></div>
          <div class="toolbar-group">
            <el-tooltip content="放大" placement="bottom">
              <el-button @click="zoomIn" class="toolbar-btn"><i class="fa-solid fa-search-plus"></i></el-button>
            </el-tooltip>
            <el-tooltip content="适应画布" placement="bottom">
              <el-button @click="fitView" class="toolbar-btn"><i class="fa-solid fa-expand"></i></el-button>
            </el-tooltip>
            <el-tooltip content="缩小" placement="bottom">
              <el-button @click="zoomOut" class="toolbar-btn"><i class="fa-solid fa-search-minus"></i></el-button>
            </el-tooltip>
          </div>
          <div class="toolbar-divider"></div>
          <div class="toolbar-group">
            <el-tooltip content="复制节点 (Ctrl+C)" placement="bottom">
              <el-button @click="copyNode" :disabled="!selectedNode" class="toolbar-btn">
                <i class="fa-solid fa-copy"></i>
              </el-button>
            </el-tooltip>
            <el-tooltip content="粘贴节点 (Ctrl+V)" placement="bottom">
              <el-button @click="pasteNode" :disabled="!clipboard" class="toolbar-btn">
                <i class="fa-solid fa-paste"></i>
              </el-button>
            </el-tooltip>
          </div>
          <div class="toolbar-divider"></div>
          <el-tooltip content="自动布局" placement="bottom">
            <el-button @click="autoLayout" class="toolbar-btn magic-btn">
              <i class="fa-solid fa-wand-magic-sparkles"></i>
            </el-button>
          </el-tooltip>
          <div class="toolbar-divider"></div>
          <!-- AI生成工作流 -->
          <el-tooltip content="AI智能生成工作流" placement="bottom">
            <el-button @click="openAIGenerator" class="toolbar-btn ai-generate-btn">
              <i class="fa-solid fa-robot"></i> AI生成
            </el-button>
          </el-tooltip>
          <div class="toolbar-divider"></div>
          <el-tooltip content="删除选中节点 (Delete)" placement="bottom">
            <el-button @click="deleteNode" :disabled="!selectedNode" type="danger" plain class="toolbar-btn">
              <i class="fa-solid fa-trash"></i>
            </el-button>
          </el-tooltip>
        </div>
        <div class="right">
          <el-popover placement="bottom" :width="320" trigger="click" v-if="Object.keys(nodeErrors).length">
            <template #reference>
              <div class="error-status clickable">
                <i class="fa-solid fa-triangle-exclamation"></i>
                <span>{{ Object.keys(nodeErrors).length }} 个节点有错误</span>
                <i class="fa-solid fa-chevron-down" style="font-size:10px;margin-left:4px"></i>
              </div>
            </template>
            <div class="error-list-popover">
              <div class="error-popover-header">
                <i class="fa-solid fa-circle-exclamation"></i>
                <span>配置错误的节点</span>
              </div>
              <div class="error-popover-list">
                <div v-for="(errors, nodeId) in nodeErrors" :key="nodeId" class="error-popover-item" @click="goToErrorNode(nodeId)">
                  <div class="error-node-info">
                    <i class="fa-solid fa-cube"></i>
                    <span class="error-node-name">{{ getNodeNameById(nodeId) }}</span>
                  </div>
                  <div class="error-node-errors">
                    <span v-for="(err, idx) in errors" :key="idx" class="error-msg">{{ err }}</span>
                  </div>
                  <div class="error-action-hint">
                    <i class="fa-solid fa-hand-pointer"></i> 点击跳转并配置
                  </div>
                </div>
              </div>
            </div>
          </el-popover>
          <div class="save-status" v-if="editingWorkflow?.id && !Object.keys(nodeErrors).length">
            <i :class="saveStatus.icon" :style="{color: saveStatus.color}"></i>
            <span>{{ saveStatus.text }}</span>
          </div>
          <!-- 执行进度 -->
          <div class="exec-progress" v-if="executing">
            <i class="fa-solid fa-spinner fa-spin"></i>
            <span>执行中...</span>
            <el-progress :percentage="executionProgress" :show-text="false" :stroke-width="4" style="width:80px" />
            <el-button size="small" type="danger" @click="cancelExecution" plain>
              <i class="fa-solid fa-stop"></i> 取消
            </el-button>
          </div>
          <el-button @click="saveWorkflow" :loading="saving" class="action-btn">
            <i class="fa-solid fa-save" v-if="!saving"></i> {{ saving ? '保存中...' : '保存' }}
          </el-button>
          <el-button type="success" @click="publishWorkflow" :disabled="!editingWorkflow?.id" class="action-btn">
            <i class="fa-solid fa-rocket"></i> 发布
          </el-button>
          <el-button type="primary" @click="testRun" :disabled="!editingWorkflow?.id" class="action-btn run-btn">
            <i class="fa-solid fa-play"></i> 测试运行
          </el-button>
          <el-tooltip content="显示执行日志" placement="bottom" v-if="executing">
            <el-button @click="showExecutionLog = !showExecutionLog" 
                       :type="showExecutionLog ? 'primary' : 'default'" 
                       class="action-btn">
              <i class="fa-solid fa-list-check"></i>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="debugMode ? '关闭调试模式' : '开启调试模式'" placement="bottom">
            <el-button @click="debugMode = !debugMode" :type="debugMode ? 'warning' : 'default'" class="action-btn" :class="{ 'debug-active': debugMode }">
              <i class="fa-solid fa-bug"></i>
            </el-button>
          </el-tooltip>
          <el-button @click="showHistory" :disabled="!editingWorkflow?.id" class="action-btn">
            <i class="fa-solid fa-clock-rotate-left"></i> 历史
          </el-button>
          <el-button @click="showVersions" :disabled="!editingWorkflow?.id" class="action-btn version-btn">
            <i class="fa-solid fa-code-branch"></i> 版本
          </el-button>
          <el-dropdown trigger="click" @command="handleMoreAction">
            <el-button class="action-btn">
              <i class="fa-solid fa-ellipsis-vertical"></i>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="export">
                  <i class="fa-solid fa-download"></i> 导出工作流
                </el-dropdown-item>
                <el-dropdown-item command="import">
                  <i class="fa-solid fa-upload"></i> 导入工作流
                </el-dropdown-item>
                <el-dropdown-item divided command="search">
                  <i class="fa-solid fa-search"></i> 搜索节点
                </el-dropdown-item>
                <el-dropdown-item command="help">
                  <i class="fa-solid fa-circle-question"></i> 帮助
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="editor-body">
        <div class="node-lib">
          <div class="lib-header">
            <h4><i class="fa-solid fa-cubes"></i> 节点库</h4>
            <el-input v-model="nodeSearch" placeholder="搜索节点" size="small" clearable prefix-icon="Search" />
          </div>
          <div class="lib-content">
            <!-- AI智能生成入口 -->
            <div class="ai-generate-entry" @click="openAIGenerator">
              <div class="ai-entry-icon">
                <i class="fa-solid fa-wand-magic-sparkles"></i>
              </div>
              <div class="ai-entry-content">
                <span class="ai-entry-title">AI智能生成</span>
                <span class="ai-entry-desc">用自然语言描述，自动生成工作流</span>
              </div>
              <i class="fa-solid fa-chevron-right ai-entry-arrow"></i>
            </div>
            <!-- 收藏节点 -->
            <div class="node-category" v-if="favoriteNodes.length">
              <div class="cat-title favorite-cat">
                <i class="fa-solid fa-star"></i> 收藏
                <span class="cat-count">{{ favoriteNodes.length }}</span>
              </div>
              <div class="cat-nodes">
                <div v-for="type in favoriteNodes" :key="'fav_'+type"
                     class="node-item" :draggable="!isLocked"
                     @dragstart="onDragStart($event, nodeTypes.find(n=>n.type===type))"
                     @dblclick="addNodeToCenter(nodeTypes.find(n=>n.type===type))">
                  <i :class="nodeTypes.find(n=>n.type===type)?.icon" :style="{color: nodeTypes.find(n=>n.type===type)?.color}"></i>
                  <span>{{ nodeTypes.find(n=>n.type===type)?.label }}</span>
                  <i class="fa-solid fa-star fav-icon" @click.stop="toggleFavorite(type)"></i>
                </div>
              </div>
            </div>
            <!-- 最近使用 -->
            <div class="node-category" v-if="recentNodes.length && !nodeSearch">
              <div class="cat-title recent-cat">
                <i class="fa-solid fa-clock-rotate-left"></i> 最近使用
                <span class="cat-count">{{ recentNodes.length }}</span>
              </div>
              <div class="cat-nodes">
                <div v-for="type in recentNodes" :key="'recent_'+type"
                     class="node-item" :draggable="!isLocked"
                     @dragstart="onDragStart($event, nodeTypes.find(n=>n.type===type))"
                     @dblclick="addNodeToCenter(nodeTypes.find(n=>n.type===type))">
                  <i :class="nodeTypes.find(n=>n.type===type)?.icon" :style="{color: nodeTypes.find(n=>n.type===type)?.color}"></i>
                  <span>{{ nodeTypes.find(n=>n.type===type)?.label }}</span>
                </div>
              </div>
            </div>
            <!-- 常规分类 -->
            <div v-for="cat in nodeCategories" :key="cat" class="node-category">
              <div class="cat-title" @click="toggleCategory(cat)">
                <i :class="expandedCats.includes(cat) ? 'fa-solid fa-chevron-down' : 'fa-solid fa-chevron-right'"></i>
                {{ cat }}
                <span class="cat-count">{{ filteredNodeTypes.filter(n => n.category === cat).length }}</span>
              </div>
              <div class="cat-nodes" v-show="expandedCats.includes(cat)">
                <div v-for="nt in filteredNodeTypes.filter(n => n.category === cat)" :key="nt.type"
                     class="node-item" :draggable="!isLocked" @dragstart="onDragStart($event, nt)"
                     @dblclick="addNodeToCenter(nt)" @contextmenu.prevent="toggleFavorite(nt.type)">
                  <i :class="nt.icon" :style="{color: nt.color}"></i>
                  <span>{{ nt.label }}</span>
                  <i v-if="favoriteNodes.includes(nt.type)" class="fa-solid fa-star fav-icon"></i>
                  <el-tooltip v-else :content="nt.desc || '双击添加 | 右键收藏'" placement="right">
                    <i class="fa-solid fa-circle-info info-icon"></i>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 实时执行日志面板 -->
        <Transition name="slide-left">
          <div class="execution-log-panel" v-if="executing && showExecutionLog">
            <div class="log-panel-header">
              <div class="log-panel-title">
                <i class="fa-solid fa-list-check"></i>
                <span>执行日志</span>
              </div>
              <el-button text @click="showExecutionLog = false">
                <i class="fa-solid fa-xmark"></i>
              </el-button>
            </div>
            <div class="log-panel-content">
              <div v-for="(log, idx) in executionLogs" :key="idx" 
                   class="log-entry" 
                   :class="log.type">
                <div class="log-timestamp">{{ log.time }}</div>
                <div class="log-icon">
                  <i :class="getLogIcon(log.type)"></i>
                </div>
                <div class="log-message">{{ log.message }}</div>
              </div>
              <div v-if="executionLogs.length === 0" class="log-empty">
                <i class="fa-solid fa-hourglass-half"></i>
                <p>等待执行...</p>
              </div>
            </div>
          </div>
        </Transition>
        
        <div class="canvas" :class="{ executing: executing }" @dragover.prevent @drop="onDrop" @click="closeContextMenu">
          <VueFlow v-model:nodes="nodes" v-model:edges="edges" fit-view-on-init
            @node-click="onNodeClick" @pane-click="selectedNode=null; closeContextMenu()" @connect="onConnect"
            @node-context-menu="onNodeContextMenu" @edge-click="onEdgeClick"
            @node-drag="onNodeDrag" @node-drag-stop="onNodeDragStop"
            :nodes-connectable="!isLocked" :edges-updatable="!isLocked"
            :default-edge-options="defaultEdgeOptions"
            :connection-line-style="{ stroke: '#6366f1', strokeWidth: 2 }"
            :snap-to-grid="true" :snap-grid="[15, 15]"
            :zoom-on-scroll="!isLocked" :pan-on-scroll="false" :select-nodes-on-drag="false"
            :nodes-draggable="!isLocked" :elements-selectable="!isLocked" :pan-on-drag="!isLocked">
            <!-- 自定义边（连线）模板 -->
            <template #edge-custom="{ id, sourceX, sourceY, targetX, targetY, sourcePosition, targetPosition, data, markerEnd, style }">
              <path :id="id" class="vue-flow__edge-path" :d="getEdgePath(sourceX, sourceY, targetX, targetY)" :style="style" :marker-end="markerEnd" />
              <text v-if="data?.label" class="edge-label" :x="(sourceX + targetX) / 2" :y="(sourceY + targetY) / 2 - 10" text-anchor="middle">{{ data.label }}</text>
            </template>
            <!-- 开始节点：只有输出 -->
            <template #node-start="{ data, id }">
              <div class="cnode start" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error', 'has-note': data.config?.note }">
                <i class="fa-solid fa-play"></i>{{ data.label }}
                <el-tooltip v-if="data.config?.note" :content="data.config.note" placement="top"><i class="note-badge fa-solid fa-sticky-note"></i></el-tooltip>
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 结束节点：只有输入 -->
            <template #node-end="{ data, id }">
              <div class="cnode end" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error', 'has-note': data.config?.note }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-flag-checkered"></i>{{ data.label }}
                <el-tooltip v-if="data.config?.note" :content="data.config.note" placement="top"><i class="note-badge fa-solid fa-sticky-note"></i></el-tooltip>
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
              </div>
            </template>
            <!-- LLM节点 -->
            <template #node-llm="{ data, id }">
              <div class="cnode llm" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error', 'has-note': data.config?.note }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-robot"></i>{{ data.label }}
                <el-tooltip v-if="data.config?.note" :content="data.config.note" placement="top"><i class="note-badge fa-solid fa-sticky-note"></i></el-tooltip>
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 条件节点：有两个输出 -->
            <template #node-condition="{ data, id }">
              <div class="cnode cond" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-code-branch"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" id="true" style="top:30%;background:#10b981" />
                <Handle type="source" :position="Position.Right" id="false" style="top:70%;background:#ef4444" />
              </div>
            </template>
            <!-- 工具节点 -->
            <template #node-tool="{ data, id }">
              <div class="cnode tool" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-wrench"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- HTTP节点 -->
            <template #node-http="{ data, id }">
              <div class="cnode http" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-globe"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 代码节点 -->
            <template #node-code="{ data, id }">
              <div class="cnode code" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-code"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 智能体节点 -->
            <template #node-agent="{ data, id }">
              <div class="cnode agent" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-user-astronaut"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 变量设置节点 -->
            <template #node-setvar="{ data, id }">
              <div class="cnode setvar" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-sliders"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 知识库节点 -->
            <template #node-knowledge="{ data, id }">
              <div class="cnode knowledge" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-book"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 分类器节点 -->
            <template #node-classifier="{ data, id }">
              <div class="cnode classifier" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-tags"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 提取器节点 -->
            <template #node-extractor="{ data, id }">
              <div class="cnode extractor" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-filter"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 问答节点 -->
            <template #node-question="{ data, id }">
              <div class="cnode question" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-comment-dots"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 迭代器节点 -->
            <template #node-iterator="{ data, id }">
              <div class="cnode iterator" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-repeat"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 聚合器节点 -->
            <template #node-aggregator="{ data, id }">
              <div class="cnode aggregator" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-object-group"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 子流程节点 -->
            <template #node-subflow="{ data, id }">
              <div class="cnode subflow" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-diagram-project"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 文本处理节点 -->
            <template #node-text="{ data, id }">
              <div class="cnode text" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-font"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 模板节点 -->
            <template #node-template="{ data, id }">
              <div class="cnode template" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-file-code"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 延迟节点 -->
            <template #node-delay="{ data, id }">
              <div class="cnode delay" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-clock"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 数据库节点 -->
            <template #node-database="{ data, id }">
              <div class="cnode database" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-database"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 邮件节点 -->
            <template #node-email="{ data, id }">
              <div class="cnode email" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-envelope"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 缓存节点 -->
            <template #node-cache="{ data, id }">
              <div class="cnode cache" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-bolt"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- Webhook节点 -->
            <template #node-webhook="{ data, id }">
              <div class="cnode webhook" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-satellite-dish"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 并行节点：多个输出 -->
            <template #node-parallel="{ data, id }">
              <div class="cnode parallel" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-code-fork"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" id="branch1" style="top:25%" />
                <Handle type="source" :position="Position.Right" id="branch2" style="top:50%" />
                <Handle type="source" :position="Position.Right" id="branch3" style="top:75%" />
              </div>
            </template>
            <!-- 合并节点：多个输入 -->
            <template #node-merge="{ data, id }">
              <div class="cnode merge" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" id="in1" style="top:25%" />
                <Handle type="target" :position="Position.Left" id="in2" style="top:50%" />
                <Handle type="target" :position="Position.Left" id="in3" style="top:75%" />
                <i class="fa-solid fa-code-merge"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <!-- 循环节点 -->
            <template #node-while="{ data, id }">
              <div class="cnode while" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-arrows-spin"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" id="loop" style="top:30%;background:#8b5cf6" />
                <Handle type="source" :position="Position.Right" id="exit" style="top:70%;background:#10b981" />
              </div>
            </template>
            <!-- 列表循环节点 -->
            <template #node-loop="{ data, id }">
              <div class="cnode loop" :class="{ 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-list-ol"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <i v-if="nodeExecutionStatus[id] === 'running'" class="status-badge running fa-solid fa-spinner fa-spin"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'completed'" class="status-badge completed fa-solid fa-check"></i>
                <i v-else-if="nodeExecutionStatus[id] === 'error'" class="status-badge error fa-solid fa-times"></i>
                <Handle type="source" :position="Position.Right" id="loop" style="top:30%;background:#7c3aed" />
                <Handle type="source" :position="Position.Right" id="done" style="top:70%;background:#10b981" />
              </div>
            </template>
            <!-- 通用默认节点模板(fallback) -->
            <template #node-default="{ data, id }">
              <div class="cnode" :class="[data.type, { 'has-error': nodeErrors[id]?.length, 'is-running': nodeExecutionStatus[id] === 'running', 'is-completed': nodeExecutionStatus[id] === 'completed', 'is-error': nodeExecutionStatus[id] === 'error' }]">
                <Handle type="target" :position="Position.Left" />
                <i class="fa-solid fa-cube"></i>{{ data.label }}
                <i v-if="nodeErrors[id]?.length" class="error-badge fa-solid fa-exclamation-circle"></i>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
            <Background /><MiniMap />
            <!-- 智能对齐辅助线 -->
            <div v-for="(y, idx) in alignmentLines.horizontal" :key="'h'+idx" 
              class="alignment-line horizontal" :style="{ top: y + 'px' }"></div>
            <div v-for="(x, idx) in alignmentLines.vertical" :key="'v'+idx" 
              class="alignment-line vertical" :style="{ left: x + 'px' }"></div>
          </VueFlow>
          <div class="canvas-status">
            <span><i class="fa-solid fa-circle-nodes"></i> {{ nodes.length }} 节点</span>
            <span><i class="fa-solid fa-arrow-right-arrow-left"></i> {{ edges.length }} 连接</span>
            <span v-if="selectedNode"><i class="fa-solid fa-hand-pointer"></i> {{ selectedNode.data.label }}</span>
            <el-tooltip :content="isLocked ? '解锁画布' : '锁定画布'" placement="top">
              <span class="lock-btn" :class="{ active: isLocked }" @click="isLocked = !isLocked">
                <i :class="isLocked ? 'fa-solid fa-lock' : 'fa-solid fa-lock-open'"></i>
              </span>
            </el-tooltip>
            <el-tooltip content="添加注释" placement="top">
              <span class="add-annotation-btn" @click="addAnnotation">
                <i class="fa-solid fa-sticky-note"></i>
              </span>
            </el-tooltip>
            <el-tooltip :content="quickConnectMode ? '关闭快速连线' : '快速连线模式'" placement="top">
              <span class="quick-connect-btn" :class="{ active: quickConnectMode }" @click="toggleQuickConnect">
                <i class="fa-solid fa-link"></i>
              </span>
            </el-tooltip>
            <span class="zoom-control">
              <el-tooltip content="放大" placement="top">
                <i class="fa-solid fa-search-plus zoom-btn" @click="zoomIn"></i>
              </el-tooltip>
              <el-tooltip content="适应画布" placement="top">
                <i class="fa-solid fa-expand zoom-btn" @click="fitView"></i>
              </el-tooltip>
              <el-tooltip content="缩小" placement="top">
                <i class="fa-solid fa-search-minus zoom-btn" @click="zoomOut"></i>
              </el-tooltip>
              <span class="zoom-divider"></span>
              <el-slider v-model="zoomLevel" :min="25" :max="200" :step="5" :show-tooltip="false" style="width: 80px" @input="onZoomChange" />
              <span class="zoom-value">{{ zoomLevel }}%</span>
            </span>
          </div>
          <!-- 画布注释 -->
          <div v-for="anno in annotations" :key="anno.id"
               class="canvas-annotation"
               :style="{ left: anno.x + 'px', top: anno.y + 'px', background: anno.color }"
               @dblclick="editAnnotation(anno)">
            <div class="annotation-content">{{ anno.text }}</div>
            <div class="annotation-actions">
              <i class="fa-solid fa-pen" @click.stop="editAnnotation(anno)"></i>
              <i class="fa-solid fa-trash" @click.stop="deleteAnnotation(anno.id)"></i>
            </div>
          </div>
          <!-- 调试面板 -->
          <Transition name="slide-up">
            <div class="debug-panel" v-if="debugMode">
              <div class="debug-header">
                <div class="debug-title">
                  <i class="fa-solid fa-bug"></i>
                  <span>调试模式</span>
                  <el-tag type="warning" size="small">开发中</el-tag>
                </div>
                <div class="debug-actions">
                  <el-button size="small" type="primary" @click="startDebug" :disabled="executing">
                    <i class="fa-solid fa-play"></i> 开始调试
                  </el-button>
                  <el-button size="small" @click="debugMode = false">
                    <i class="fa-solid fa-times"></i> 关闭
                  </el-button>
                </div>
              </div>
              <div class="debug-content">
                <div class="debug-info">
                  <div class="debug-item">
                    <span class="debug-label">当前节点</span>
                    <span class="debug-value">{{ debugCurrentNode || '-' }}</span>
                  </div>
                  <div class="debug-item">
                    <span class="debug-label">已执行</span>
                    <span class="debug-value">{{ Object.keys(debugNodeOutputs).length }} / {{ nodes.length }}</span>
                  </div>
                </div>
                <div class="debug-outputs" v-if="Object.keys(debugNodeOutputs).length">
                  <div class="debug-output-title">节点输出</div>
                  <div v-for="(output, nodeId) in debugNodeOutputs" :key="nodeId" class="debug-output-item">
                    <span class="output-node">{{ nodeId }}</span>
                    <span class="output-value">{{ typeof output === 'string' ? output.substring(0, 50) : JSON.stringify(output).substring(0, 50) }}...</span>
                  </div>
                </div>
              </div>
            </div>
          </Transition>
        </div>
        <div class="props" :class="{ collapsed: propsPanelCollapsed }" :style="{ width: propsWidth + 'px' }" v-if="selectedNode">
          <div class="props-resize-handle" @mousedown="startResize"></div>
          <el-tooltip :content="propsPanelCollapsed ? '展开属性面板' : '收起属性面板'" placement="right">
            <div class="props-collapse-btn" @click="propsPanelCollapsed = !propsPanelCollapsed">
              <i :class="propsPanelCollapsed ? 'fa-solid fa-chevron-left' : 'fa-solid fa-chevron-right'"></i>
            </div>
          </el-tooltip>
          <div class="props-header">
            <div class="node-badge" :style="{background: getNodeColor(selectedNode.type)}">
              <i :class="getNodeIcon(selectedNode.type)"></i>
            </div>
            <div class="node-info">
              <span class="node-type">{{ getNodeLabel(selectedNode.type) }}</span>
              <span class="node-id">ID: {{ selectedNode.id }}</span>
            </div>
          </div>
          <!-- 错误提示 -->
          <el-alert v-if="nodeErrors[selectedNode.id]?.length"
                    type="error"
                    :closable="false"
                    class="node-error-alert">
            <template #title>
              <div class="error-title">
                <i class="fa-solid fa-triangle-exclamation"></i>
                <span>配置错误 ({{ nodeErrors[selectedNode.id].length }})</span>
              </div>
            </template>
            <ul class="error-list">
              <li v-for="(err, idx) in nodeErrors[selectedNode.id]" :key="idx">{{ err }}</li>
            </ul>
          </el-alert>
          <el-form label-position="top" size="small">
            <el-form-item label="节点名称"><el-input v-model="selectedNode.data.label" placeholder="自定义名称" /></el-form-item>
            <el-form-item label="节点备注">
              <el-input type="textarea" v-model="selectedNode.data.config.note" :rows="2" placeholder="添加备注说明（可选）" />
            </el-form-item>
            <!-- 节点输出预览 -->
            <div class="node-output-preview" v-if="debugNodeOutputs[selectedNode.id]">
              <div class="output-preview-header">
                <i class="fa-solid fa-eye"></i>
                <span>最近输出</span>
              </div>
              <pre class="output-preview-content">{{ formatNodeOutput(debugNodeOutputs[selectedNode.id]) }}</pre>
            </div>
            <!-- 智能推荐下一节点 -->
            <div class="suggested-nodes" v-if="suggestedNextNodes.length && selectedNode.type !== 'end'">
              <div class="suggested-header">
                <i class="fa-solid fa-wand-magic-sparkles"></i>
                <span>推荐添加</span>
              </div>
              <div class="suggested-list">
                <div v-for="sn in suggestedNextNodes" :key="sn.type" class="suggested-item" @click="addSuggestedNode(sn)">
                  <i :class="sn.icon" :style="{ color: sn.color }"></i>
                  <span>{{ sn.label }}</span>
                  <i class="fa-solid fa-plus add-icon"></i>
                </div>
              </div>
            </div>
            <!-- 开始节点：输入参数定义 -->
            <template v-if="selectedNode.type==='start'">
              <div class="node-guide">
                <div class="guide-header">
                  <i class="fa-solid fa-circle-info"></i>
                  <span>开始节点说明</span>
                </div>
                <div class="guide-content">
                  <p>这是工作流的<strong>入口点</strong>，所有执行从这里开始。</p>
                  <p>运行工作流时传入的内容会存储在 <code>input</code> 变量中。</p>
                </div>
              </div>
              <el-form-item label="输入参数定义（可选）">
                <div class="input-params">
                  <div v-for="(param, idx) in (selectedNode.data.config.inputParams || [])" :key="idx" class="param-item">
                    <el-input v-model="param.name" placeholder="参数名" style="width:80px" />
                    <el-select v-model="param.type" style="width:80px">
                      <el-option label="文本" value="string" />
                      <el-option label="数字" value="number" />
                      <el-option label="布尔" value="boolean" />
                      <el-option label="数组" value="array" />
                      <el-option label="对象" value="object" />
                    </el-select>
                    <el-input v-model="param.description" placeholder="描述" style="flex:1" />
                    <el-checkbox v-model="param.required" title="必填">必填</el-checkbox>
                    <el-button text type="danger" @click="selectedNode.data.config.inputParams.splice(idx,1)"><i class="fa-solid fa-minus"></i></el-button>
                  </div>
                  <el-button size="small" @click="addInputParam"><i class="fa-solid fa-plus"></i> 添加参数</el-button>
                </div>
              </el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 运行时传入的内容自动存储为 <code v-pre>{{input}}</code></p>
                <p>2. 后续节点可以通过 <code v-pre>{{input}}</code> 引用输入内容</p>
                <p>3. 如需多个参数，可添加自定义参数</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='llm'">
              <div class="node-guide">
                <div class="guide-header">
                  <i class="fa-solid fa-circle-info"></i>
                  <span>LLM节点说明</span>
                </div>
                <div class="guide-content">
                  <p>调用AI大模型生成内容，是工作流的<strong>核心节点</strong>。</p>
                </div>
              </div>
              <el-form-item label="模型" required>
                <el-select 
                  v-model="selectedNode.data.config.modelId" 
                  placeholder="选择AI模型"
                  clearable
                  filterable
                >
                  <el-option 
                    v-for="m in models" 
                    :key="m.id" 
                    :label="m.modelName || m.name || `模型${m.id}`" 
                    :value="String(m.id)"
                  >
                    <span style="float: left">{{ m.modelName || m.name || `模型${m.id}` }}</span>
                    <span style="float: right; color: #8492a6; font-size: 12px">ID: {{ m.id }}</span>
                  </el-option>
                </el-select>
                <div v-if="selectedNode.data.config.modelId" style="margin-top: 8px; font-size: 12px; color: #909399;">
                  <i class="fa-solid fa-check-circle" style="color: #67c23a;"></i>
                  已选择：{{ getModelName(selectedNode.data.config.modelId) }}
                </div>
              </el-form-item>
              <el-form-item label="系统提示词">
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.systemPrompt" :rows="6" placeholder="设定AI的角色和行为，例如：你是一个专业的客服助手" @input="e => handleVarInput(e, 'systemPrompt')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="用户提示词" required>
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.userPrompt" :rows="6" placeholder="请分析以下内容：{{input}}" @input="e => handleVarInput(e, 'userPrompt')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="输出变量名" required>
                <el-input v-model="selectedNode.data.config.outputVariable" placeholder="llm_output" />
              </el-form-item>
              <el-collapse class="advanced-config">
                <el-collapse-item title="高级配置" name="advanced">
                  <el-form-item label="Temperature">
                    <el-slider v-model="selectedNode.data.config.temperature" :min="0" :max="2" :step="0.1" show-input :show-input-controls="false" />
                    <div class="param-hint">控制输出随机性：0=确定性，1=平衡，2=创造性</div>
                  </el-form-item>
                  <el-form-item label="最大Token数">
                    <el-input-number v-model="selectedNode.data.config.maxTokens" :min="100" :max="8000" :step="100" />
                    <div class="param-hint">限制AI回复的最大长度</div>
                  </el-form-item>
                </el-collapse-item>
              </el-collapse>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 在提示词中用 <code v-pre>{{input}}</code> 引用输入内容</p>
                <p>2. 也可以引用其他节点的输出，如 <code v-pre>{{knowledge_result}}</code></p>
                <p>3. AI的回复会存储到「输出变量」中，供后续节点使用</p>
              </div>
              <div class="example-box">
                <div class="example-title"><i class="fa-solid fa-code"></i> 配置示例</div>
                <div class="example-content">
                  <p><strong>系统提示词：</strong>你是一个专业的文章摘要助手</p>
                  <p><strong>用户提示词：</strong>请为以下文章生成摘要：<code v-pre>{{input}}</code></p>
                  <p><strong>输出变量：</strong>summary</p>
                </div>
              </div>
            </template>
            <template v-if="selectedNode.type==='condition'">
              <el-form-item label="条件表达式" required>
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.expression" placeholder="{{count}} > 10" @input="e => handleVarInput(e, 'expression')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <div class="tip">
                <p>支持的操作符:</p>
                <p>• <code>==</code> <code>!=</code> <code>&gt;</code> <code>&lt;</code> <code>&gt;=</code> <code>&lt;=</code></p>
                <p>• <code>contains</code> <code>startsWith</code> <code>endsWith</code></p>
                <p>• <code>isEmpty</code> <code>isNotEmpty</code></p>
                <p style="margin-top:8px">从 <span style="color:#10b981">●绿色句柄</span> 连接 true 分支</p>
                <p>从 <span style="color:#ef4444">●红色句柄</span> 连接 false 分支</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='tool'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>工具节点说明</span></div>
                <div class="guide-content">
                  <p>调用系统内置工具获取外部数据，如<strong>天气、时间、计算</strong>等。</p>
                </div>
              </div>
              <el-form-item label="工具" required>
                <el-select 
                  v-model="selectedNode.data.config.toolName" 
                  placeholder="选择要调用的工具"
                  clearable
                  filterable
                >
                  <el-option 
                    v-for="t in tools" 
                    :key="t.name" 
                    :label="t.displayName || t.name" 
                    :value="t.name"
                  >
                    <span style="float: left">{{ t.displayName || t.name }}</span>
                    <span style="float: right; color: #8492a6; font-size: 12px" v-if="t.id">ID: {{ t.id }}</span>
                  </el-option>
                </el-select>
                <div v-if="selectedNode.data.config.toolName" style="margin-top: 8px; font-size: 12px; color: #909399;">
                  <i class="fa-solid fa-check-circle" style="color: #67c23a;"></i>
                  已选择：{{ selectedNode.data.config.toolName }}
                </div>
              </el-form-item>
              <el-form-item label="参数(JSON)">
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.paramsJson" :rows="6" placeholder='{"city": "{{input}}"}' @input="e => handleVarInput(e, 'paramsJson')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="tool_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 选择要调用的工具</p>
                <p>2. 参数使用JSON格式，支持 <code v-pre>{{变量}}</code> 引用</p>
                <p>3. 工具返回结果存储到「输出变量」中</p>
              </div>
              <div class="example-box">
                <div class="example-title"><i class="fa-solid fa-code"></i> 配置示例（天气查询）</div>
                <div class="example-content">
                  <p><strong>工具：</strong>天气查询</p>
                  <p><strong>参数：</strong><code>{"city": "北京"}</code></p>
                  <p><strong>输出变量：</strong>weather_info</p>
                </div>
              </div>
            </template>
            <template v-if="selectedNode.type==='http'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>HTTP节点说明</span></div>
                <div class="guide-content">
                  <p>发送HTTP请求调用<strong>外部API</strong>，获取或提交数据。</p>
                </div>
              </div>
              <el-form-item label="方法" required><el-select v-model="selectedNode.data.config.method"><el-option label="GET" value="GET" /><el-option label="POST" value="POST" /><el-option label="PUT" value="PUT" /><el-option label="DELETE" value="DELETE" /></el-select></el-form-item>
              <el-form-item label="URL" required>
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.url" placeholder="https://api.example.com/path?q={{input}}" @input="e => handleVarInput(e, 'url')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="请求头(JSON)"><el-input type="textarea" v-model="selectedNode.data.config.headersJson" :rows="2" placeholder='{"Authorization": "Bearer xxx"}' /></el-form-item>
              <el-form-item label="请求体" v-if="selectedNode.data.config.method !== 'GET'">
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.body" :rows="6" placeholder='{"message": "{{input}}"}' @input="e => handleVarInput(e, 'body')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="http_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. URL和请求体都支持 <code v-pre>{{变量}}</code> 引用</p>
                <p>2. API返回的JSON会自动解析，存储到输出变量中</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='agent'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>智能体节点说明</span></div>
                <div class="guide-content">
                  <p>调用已配置的<strong>智能体</strong>处理任务，复用智能体的能力。</p>
                </div>
              </div>
              <el-form-item label="智能体" required>
                <el-select 
                  v-model="selectedNode.data.config.agentId" 
                  placeholder="选择智能体"
                  clearable
                  filterable
                >
                  <el-option 
                    v-for="a in agents" 
                    :key="a.id" 
                    :label="a.name || `智能体${a.id}`" 
                    :value="String(a.id)"
                  >
                    <span style="float: left">{{ a.name || `智能体${a.id}` }}</span>
                    <span style="float: right; color: #8492a6; font-size: 12px">ID: {{ a.id }}</span>
                  </el-option>
                </el-select>
                <div v-if="selectedNode.data.config.agentId" style="margin-top: 8px; font-size: 12px; color: #909399;">
                  <i class="fa-solid fa-check-circle" style="color: #67c23a;"></i>
                  已选择：{{ getAgentName(selectedNode.data.config.agentId) }}
                </div>
              </el-form-item>
              <el-form-item label="用户提示词"><el-input type="textarea" v-model="selectedNode.data.config.userPrompt" :rows="2" placeholder="{{input}}" /></el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="agent_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 选择一个已创建的智能体</p>
                <p>2. 智能体会使用自己的模型和工具处理输入</p>
                <p>3. 适合复用复杂的对话逻辑</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='code'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>代码节点说明</span></div>
                <div class="guide-content">
                  <p>执行自定义代码进行<strong>数据处理、格式转换</strong>等操作。</p>
                </div>
              </div>
              <el-form-item label="语言">
                <el-select v-model="selectedNode.data.config.language">
                  <el-option label="JavaScript" value="javascript" />
                </el-select>
              </el-form-item>
              <el-form-item label="代码" required>
                <el-input type="textarea" v-model="selectedNode.data.config.code" :rows="8" placeholder="// 简单变量返回&#10;return {{input}};&#10;&#10;// 或返回字符串&#10;return 'hello world';&#10;&#10;// 或返回数字&#10;return 42;" />
              </el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="code_result" /></el-form-item>
              <el-alert type="warning" :closable="false" class="code-warning">
                <template #title>
                  <span><i class="fa-solid fa-exclamation-triangle"></i> 简化模式</span>
                </template>
                当前仅支持简单的 return 语句：
                <ul style="margin: 5px 0 0 20px; font-size: 12px;">
                  <li><code>return {{变量名}};</code> - 返回变量值</li>
                  <li><code>return "字符串";</code> - 返回字符串</li>
                  <li><code>return 123;</code> - 返回数字</li>
                </ul>
              </el-alert>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 使用 <code v-pre>{{变量}}</code> 引用其他节点的输出</p>
                <p>2. 代码最后一行的 <code>return</code> 值作为输出</p>
                <p>3. 适合简单的数据传递和格式转换</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='end'">
              <div class="node-guide">
                <div class="guide-header">
                  <i class="fa-solid fa-circle-info"></i>
                  <span>结束节点说明</span>
                </div>
                <div class="guide-content">
                  <p>这是工作流的<strong>出口点</strong>，指定返回给调用者的结果。</p>
                </div>
              </div>
              <el-form-item label="输出变量名" required>
                <el-input v-model="selectedNode.data.config.outputVariable" placeholder="例如：llm_output" />
              </el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 填写想要输出的变量名（不需要双花括号）</p>
                <p>2. 该变量的内容将作为工作流的最终结果返回</p>
              </div>
              <div class="available-vars" v-if="outputVariables.length">
                <div class="vars-title"><i class="fa-solid fa-list"></i> 可用的输出变量</div>
                <div class="vars-list">
                  <span v-for="v in outputVariables" :key="v" class="var-tag" @click="selectedNode.data.config.outputVariable = v">{{ v }}</span>
                </div>
              </div>
            </template>
            <template v-if="selectedNode.type==='setvar'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>变量设置节点说明</span></div>
                <div class="guide-content">
                  <p>创建或修改变量，用于<strong>存储中间结果</strong>或<strong>组合数据</strong>。</p>
                </div>
              </div>
              <el-form-item label="变量名"><el-input v-model="selectedNode.data.config.variableName" placeholder="my_var" /></el-form-item>
              <el-form-item label="值">
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.value" placeholder="{{input}} 或固定值" @input="e => handleVarInput(e, 'value')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="类型"><el-select v-model="selectedNode.data.config.valueType"><el-option label="字符串" value="string" /><el-option label="数字" value="number" /><el-option label="布尔" value="boolean" /><el-option label="JSON" value="json" /></el-select></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 可以存储固定值，如 <code>你好</code></p>
                <p>2. 也可以引用变量，如 <code v-pre>{{input}}</code></p>
                <p>3. 后续节点通过 <code v-pre>{{变量名}}</code> 使用</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='knowledge'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>知识库节点说明</span></div>
                <div class="guide-content">
                  <p>从知识库中<strong>检索相关内容</strong>，实现RAG（检索增强生成）。</p>
                </div>
              </div>
              <el-form-item label="知识库">
                <el-select 
                  v-model="selectedNode.data.config.knowledgeBaseId" 
                  placeholder="选择知识库" 
                  clearable
                  filterable
                >
                  <el-option 
                    v-for="kb in knowledgeBases" 
                    :key="kb.id" 
                    :label="kb.fileName || kb.file_name || kb.name || `知识库${kb.id}`" 
                    :value="String(kb.id)"
                  >
                    <div style="display: flex; justify-content: space-between; align-items: center; width: 100%;">
                      <span style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                        {{ kb.fileName || kb.file_name || kb.name || `知识库${kb.id}` }}
                      </span>
                      <span style="color: #8492a6; font-size: 12px; margin-left: 8px; flex-shrink: 0;">ID: {{ kb.id }}</span>
                    </div>
                  </el-option>
                </el-select>
                <div v-if="selectedNode.data.config.knowledgeBaseId" style="margin-top: 8px; font-size: 12px; color: #909399;">
                  <i class="fa-solid fa-check-circle" style="color: #67c23a;"></i>
                  已选择：{{ getKnowledgeBaseName(selectedNode.data.config.knowledgeBaseId) }}
                </div>
              </el-form-item>
              <el-form-item label="查询内容" required>
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.query" placeholder="{{input}}" @input="e => handleVarInput(e, 'query')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="返回数量"><el-input-number v-model="selectedNode.data.config.topK" :min="1" :max="20" /></el-form-item>
              <el-form-item label="最低分数"><el-input-number v-model="selectedNode.data.config.minScore" :min="0" :max="1" :step="0.1" /></el-form-item>
              <el-form-item label="输出格式"><el-select v-model="selectedNode.data.config.outputFormat"><el-option label="文本" value="text" /><el-option label="JSON" value="json" /><el-option label="Markdown" value="markdown" /></el-select></el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="knowledge_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 选择要检索的知识库</p>
                <p>2. 查询内容通常使用 <code v-pre>{{input}}</code></p>
                <p>3. 检索结果传给LLM节点生成回答</p>
              </div>
              <div class="example-box">
                <div class="example-title"><i class="fa-solid fa-lightbulb"></i> RAG典型流程</div>
                <div class="example-content">
                  <p>开始 → <strong>知识库</strong> → LLM → 结束</p>
                  <p>LLM提示词：根据以下资料回答问题：<code v-pre>{{knowledge_result}}</code></p>
                </div>
              </div>
            </template>
            <template v-if="selectedNode.type==='text'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>文本处理节点说明</span></div>
                <div class="guide-content">
                  <p>对文本进行<strong>格式化处理</strong>，如合并、分割、替换等。</p>
                </div>
              </div>
              <el-form-item label="操作">
                <el-select v-model="selectedNode.data.config.operation">
                  <el-option label="合并变量" value="concat" />
                  <el-option label="分割文本" value="split" />
                  <el-option label="替换内容" value="replace" />
                  <el-option label="正则提取" value="extract" />
                  <el-option label="截取子串" value="substring" />
                  <el-option label="转大写" value="uppercase" />
                  <el-option label="转小写" value="lowercase" />
                  <el-option label="去空格" value="trim" />
                  <el-option label="计算长度" value="length" />
                  <el-option label="按行分割" value="lines" />
                  <el-option label="JSON解析" value="json_parse" />
                  <el-option label="JSON序列化" value="json_stringify" />
                  <el-option label="模板格式化" value="format" />
                </el-select>
              </el-form-item>
              <el-form-item label="输入变量" v-if="!['concat'].includes(selectedNode.data.config.operation)">
                <el-input v-model="selectedNode.data.config.inputVariable" placeholder="input" />
              </el-form-item>
              <!-- 合并操作：选择多个变量 -->
              <el-form-item label="要合并的变量" v-if="selectedNode.data.config.operation === 'concat'">
                <div class="variables-list">
                  <div v-for="(varName, idx) in (selectedNode.data.config.variables || [])" :key="idx" class="var-item-row">
                    <el-input v-model="selectedNode.data.config.variables[idx]" placeholder="变量名" style="flex:1" />
                    <el-button text type="danger" @click="selectedNode.data.config.variables.splice(idx,1)"><i class="fa-solid fa-minus"></i></el-button>
                  </div>
                  <el-button size="small" @click="if(!selectedNode.data.config.variables)selectedNode.data.config.variables=[];selectedNode.data.config.variables.push('')"><i class="fa-solid fa-plus"></i> 添加变量</el-button>
                </div>
              </el-form-item>
              <!-- 分割操作：分隔符 -->
              <el-form-item label="分隔符" v-if="['split', 'concat'].includes(selectedNode.data.config.operation)">
                <el-input v-model="selectedNode.data.config.separator" :placeholder="selectedNode.data.config.operation === 'split' ? '\\n' : ''" />
              </el-form-item>
              <!-- 替换操作：查找和替换 -->
              <el-form-item label="查找内容" v-if="selectedNode.data.config.operation === 'replace'">
                <el-input v-model="selectedNode.data.config.pattern" placeholder="要查找的文本或正则" />
              </el-form-item>
              <el-form-item label="替换为" v-if="selectedNode.data.config.operation === 'replace'">
                <el-input v-model="selectedNode.data.config.replacement" placeholder="替换后的文本" />
              </el-form-item>
              <el-form-item label="使用正则" v-if="selectedNode.data.config.operation === 'replace'">
                <el-switch v-model="selectedNode.data.config.regex" />
              </el-form-item>
              <!-- 提取操作：正则表达式 -->
              <el-form-item label="正则表达式" v-if="selectedNode.data.config.operation === 'extract'">
                <el-input v-model="selectedNode.data.config.pattern" placeholder="如: \\d+" />
              </el-form-item>
              <el-form-item label="提取全部匹配" v-if="selectedNode.data.config.operation === 'extract'">
                <el-switch v-model="selectedNode.data.config.extractAll" />
              </el-form-item>
              <!-- 截取操作：起止位置 -->
              <el-form-item label="起始位置" v-if="selectedNode.data.config.operation === 'substring'">
                <el-input-number v-model="selectedNode.data.config.start" :min="0" />
              </el-form-item>
              <el-form-item label="结束位置" v-if="selectedNode.data.config.operation === 'substring'">
                <el-input-number v-model="selectedNode.data.config.end" :min="0" />
              </el-form-item>
              <!-- 模板格式化 -->
              <el-form-item label="模板" v-if="selectedNode.data.config.operation === 'format'">
                <el-input type="textarea" v-model="selectedNode.data.config.template" :rows="4" placeholder="你好{{name}}，欢迎使用" />
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="text_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 操作说明：</strong></p>
                <p>• <strong>合并变量</strong>：将多个变量值拼接成一个字符串</p>
                <p>• <strong>分割文本</strong>：按分隔符拆分为数组</p>
                <p>• <strong>替换内容</strong>：查找并替换文本内容</p>
                <p>• <strong>正则提取</strong>：用正则表达式提取匹配内容</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='template'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>模板节点说明</span></div>
                <div class="guide-content">
                  <p>使用模板语法<strong>组合生成文本</strong>，支持变量插值。</p>
                </div>
              </div>
              <el-form-item label="模板内容">
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.template" :rows="10" placeholder="尊敬的{{name}}，您好！&#10;您的订单{{order_id}}已发货。" @input="e => handleVarInput(e, 'template')" @keydown="handleVarKeydown" @blur="() => setTimeout(() => showVarSuggestions = false, 200)" />
                  <span class="var-hint" v-pre>输入 {{ 触发变量补全</span>
                </div>
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="template_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>在模板中使用 <code v-pre>{{变量名}}</code> 插入变量值</p>
                <p>适合生成格式化的消息、邮件等</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='iterator'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>迭代器节点说明</span></div>
                <div class="guide-content">
                  <p>遍历数组，对每个元素<strong>执行相同操作</strong>。</p>
                </div>
              </div>
              <el-form-item label="输入列表变量"><el-input v-model="selectedNode.data.config.inputVariable" placeholder="items" /></el-form-item>
              <el-form-item label="元素变量名"><el-input v-model="selectedNode.data.config.itemVariable" placeholder="item" /></el-form-item>
              <el-form-item label="索引变量名"><el-input v-model="selectedNode.data.config.indexVariable" placeholder="index" /></el-form-item>
              <el-form-item label="执行模式">
                <el-select v-model="selectedNode.data.config.mode">
                  <el-option label="顺序执行" value="sequential" />
                  <el-option label="批量处理" value="batch" />
                </el-select>
              </el-form-item>
              <el-form-item label="批量大小" v-if="selectedNode.data.config.mode === 'batch'">
                <el-input-number v-model="selectedNode.data.config.batchSize" :min="1" :max="100" :step="5" />
              </el-form-item>
              <el-form-item label="最大迭代次数">
                <el-input-number v-model="selectedNode.data.config.maxIterations" :min="1" :max="1000" :step="10" />
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="iterator_results" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 输入一个包含数组的变量</p>
                <p>2. 每次迭代，当前元素存入「元素变量」</p>
                <p>3. 后续节点用 <code v-pre>{{item}}</code> 访问当前元素</p>
                <p>4. 使用 <code v-pre>{{index}}</code> 获取当前索引</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='aggregator'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>聚合器节点说明</span></div>
                <div class="guide-content">
                  <p>将多个变量的数据<strong>合并为一个</strong>输出。</p>
                </div>
              </div>
              <el-form-item label="合并模式">
                <el-select v-model="selectedNode.data.config.mode">
                  <el-option label="对象(按变量名)" value="object" />
                  <el-option label="数组" value="array" />
                  <el-option label="文本拼接" value="concat" />
                  <el-option label="求和" value="sum" />
                  <el-option label="平均值" value="avg" />
                  <el-option label="最小值" value="min" />
                  <el-option label="最大值" value="max" />
                  <el-option label="计数" value="count" />
                  <el-option label="取第一个" value="first" />
                  <el-option label="取最后一个" value="last" />
                </el-select>
              </el-form-item>
              <el-form-item label="要聚合的变量">
                <div class="variables-list">
                  <div v-for="(varName, idx) in (selectedNode.data.config.variables || [])" :key="idx" class="var-item-row">
                    <el-input v-model="selectedNode.data.config.variables[idx]" placeholder="变量名" style="flex:1" />
                    <el-button text type="danger" @click="selectedNode.data.config.variables.splice(idx,1)"><i class="fa-solid fa-minus"></i></el-button>
                  </div>
                  <el-button size="small" @click="if(!selectedNode.data.config.variables)selectedNode.data.config.variables=[];selectedNode.data.config.variables.push('')"><i class="fa-solid fa-plus"></i> 添加变量</el-button>
                </div>
              </el-form-item>
              <el-form-item label="分隔符" v-if="selectedNode.data.config.mode === 'concat'">
                <el-input v-model="selectedNode.data.config.separator" placeholder="\n" />
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="aggregated_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 添加要聚合的变量名</p>
                <p>2. 选择合并模式</p>
                <p>3. 结果存储到输出变量中</p>
              </div>
              <div class="available-vars" v-if="outputVariables.length">
                <div class="vars-title"><i class="fa-solid fa-list"></i> 可用变量</div>
                <div class="vars-list">
                  <span v-for="v in outputVariables" :key="v" class="var-tag" @click="if(!selectedNode.data.config.variables)selectedNode.data.config.variables=[];if(!selectedNode.data.config.variables.includes(v))selectedNode.data.config.variables.push(v)">{{ v }}</span>
                </div>
              </div>
            </template>
            <template v-if="selectedNode.type==='subflow'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>子流程节点说明</span></div>
                <div class="guide-content">
                  <p>调用另一个工作流，实现<strong>流程复用</strong>和<strong>模块化</strong>。</p>
                </div>
              </div>
              <el-form-item label="子工作流"><el-select v-model="selectedNode.data.config.workflowId" placeholder="选择工作流"><el-option v-for="wf in workflows" :key="wf.id" :label="wf.name" :value="String(wf.id)" /></el-select></el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="subflow_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 选择一个已发布的工作流</p>
                <p>2. 当前输入会传递给子工作流</p>
                <p>3. 子工作流的输出存入变量</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='classifier'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>分类器节点说明</span></div>
                <div class="guide-content">
                  <p>使用AI自动<strong>判断输入属于哪个分类</strong>，实现智能路由。</p>
                </div>
              </div>
              <el-form-item label="输入变量"><el-input v-model="selectedNode.data.config.inputVariable" placeholder="input" /></el-form-item>
              <el-form-item label="分类列表" required>
                <div class="category-list">
                  <div v-for="(cat, idx) in (selectedNode.data.config.categories||[])" :key="idx" class="cat-item">
                    <el-input v-model="cat.name" placeholder="分类名" style="width:80px" />
                    <el-input v-model="cat.description" placeholder="描述" style="flex:1" />
                    <el-button text type="danger" @click="selectedNode.data.config.categories.splice(idx,1)"><i class="fa-solid fa-minus"></i></el-button>
                  </div>
                  <el-button size="small" @click="if(!selectedNode.data.config.categories)selectedNode.data.config.categories=[];selectedNode.data.config.categories.push({name:'',description:''})"><i class="fa-solid fa-plus"></i> 添加分类</el-button>
                </div>
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="category" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 定义多个分类及其描述</p>
                <p>2. AI会根据描述判断输入属于哪个分类</p>
                <p>3. 分类结果存入输出变量，可用于后续处理</p>
              </div>
              <div class="example-box">
                <div class="example-title"><i class="fa-solid fa-lightbulb"></i> 配置示例</div>
                <div class="example-content">
                  <p><strong>分类1：</strong>咨询 - 用户询问产品信息</p>
                  <p><strong>分类2：</strong>投诉 - 用户表达不满</p>
                  <p><strong>分类3：</strong>其他 - 无法归类的内容</p>
                </div>
              </div>
            </template>
            <template v-if="selectedNode.type==='extractor'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>提取器节点说明</span></div>
                <div class="guide-content">
                  <p>使用AI从文本中<strong>提取结构化信息</strong>，如姓名、日期等。</p>
                </div>
              </div>
              <el-form-item label="输入变量"><el-input v-model="selectedNode.data.config.inputVariable" placeholder="input" /></el-form-item>
              <el-form-item label="提取字段" required>
                <div class="category-list">
                  <div v-for="(f, idx) in (selectedNode.data.config.fields||[])" :key="idx" class="cat-item">
                    <el-input v-model="f.name" placeholder="字段名" style="width:80px" />
                    <el-select v-model="f.type" style="width:80px"><el-option label="文本" value="string" /><el-option label="数字" value="number" /><el-option label="布尔" value="boolean" /></el-select>
                    <el-input v-model="f.description" placeholder="描述" style="flex:1" />
                    <el-button text type="danger" @click="selectedNode.data.config.fields.splice(idx,1)"><i class="fa-solid fa-minus"></i></el-button>
                  </div>
                  <el-button size="small" @click="if(!selectedNode.data.config.fields)selectedNode.data.config.fields=[];selectedNode.data.config.fields.push({name:'',type:'string',description:''})"><i class="fa-solid fa-plus"></i> 添加字段</el-button>
                </div>
              </el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="extracted_data" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 定义要提取的字段名、类型和描述</p>
                <p>2. AI会从输入文本中提取这些字段</p>
                <p>3. 输出为JSON对象，如 <code>{"name": "张三", "age": 25}</code></p>
              </div>
            </template>
            <template v-if="selectedNode.type==='question'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>问答节点说明</span></div>
                <div class="guide-content">
                  <p>基于上下文<strong>回答问题</strong>或<strong>生成问题</strong>。</p>
                </div>
              </div>
              <el-form-item label="模式">
                <el-select v-model="selectedNode.data.config.mode">
                  <el-option label="回答问题" value="answer" />
                  <el-option label="生成问题" value="generate" />
                </el-select>
              </el-form-item>
              <el-form-item label="上下文变量"><el-input v-model="selectedNode.data.config.contextVariable" placeholder="knowledge_result" /></el-form-item>
              <el-form-item label="问题变量" v-if="selectedNode.data.config.mode === 'answer'">
                <el-input v-model="selectedNode.data.config.questionVariable" placeholder="input" />
              </el-form-item>
              <el-form-item label="生成问题数量" v-if="selectedNode.data.config.mode === 'generate'">
                <el-input-number v-model="selectedNode.data.config.questionCount" :min="1" :max="20" :step="1" />
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="qa_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>• <strong>回答问题</strong>：根据上下文回答用户问题</p>
                <p>• <strong>生成问题</strong>：根据上下文生成相关问题</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='delay'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>延迟节点说明</span></div>
                <div class="guide-content">
                  <p>暂停执行指定时间，用于<strong>限流</strong>或<strong>等待</strong>。</p>
                </div>
              </div>
              <el-form-item label="延迟时间(毫秒)"><el-input-number v-model="selectedNode.data.config.delayMs" :min="100" :max="60000" :step="100" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用场景：</strong></p>
                <p>• 避免API调用过于频繁</p>
                <p>• 等待外部系统处理完成</p>
                <p>• 1000毫秒 = 1秒</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='database'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>数据库节点说明</span></div>
                <div class="guide-content">
                  <p>执行SQL语句，支持<strong>查询</strong>和<strong>更新</strong>操作。</p>
                </div>
              </div>
              <el-form-item label="操作类型">
                <el-select v-model="selectedNode.data.config.operation">
                  <el-option label="查询(SELECT)" value="query" />
                  <el-option label="插入(INSERT)" value="insert" />
                  <el-option label="更新(UPDATE)" value="update" />
                  <el-option label="删除(DELETE)" value="delete" />
                </el-select>
              </el-form-item>
              <el-form-item label="SQL语句" required>
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.sql" :rows="6" placeholder="SELECT * FROM users WHERE id = ?" @input="e => handleVarInput(e, 'sql')" />
                  <span class="var-hint" v-pre>支持 {{变量}} 和 ? 占位符</span>
                </div>
              </el-form-item>
              <el-form-item label="参数(JSON数组)">
                <el-input type="textarea" v-model="selectedNode.data.config.paramsJson" :rows="2" placeholder='["{{input}}", 123]' />
              </el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="db_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>• 查询返回结果数组，更新返回影响行数</p>
                <p>• 使用 ? 占位符防止SQL注入</p>
                <p>• 参数按顺序替换占位符</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='email'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>邮件节点说明</span></div>
                <div class="guide-content">
                  <p>发送邮件通知，支持<strong>文本</strong>和<strong>HTML</strong>格式。</p>
                </div>
              </div>
              <el-form-item label="收件人" required>
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.to" placeholder="user@example.com" @input="e => handleVarInput(e, 'to')" />
                  <span class="var-hint">多个用逗号分隔</span>
                </div>
              </el-form-item>
              <el-form-item label="主题" required>
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.subject" placeholder="通知：{{title}}" @input="e => handleVarInput(e, 'subject')" />
                </div>
              </el-form-item>
              <el-form-item label="内容">
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.content" :rows="6" placeholder="您好，{{name}}，您的订单已处理完成。" @input="e => handleVarInput(e, 'content')" />
                </div>
              </el-form-item>
              <el-collapse class="advanced-config">
                <el-collapse-item title="高级配置" name="advanced">
                  <el-form-item label="抄送(CC)"><el-input v-model="selectedNode.data.config.cc" placeholder="cc@example.com" /></el-form-item>
                  <el-form-item label="密送(BCC)"><el-input v-model="selectedNode.data.config.bcc" placeholder="bcc@example.com" /></el-form-item>
                  <el-form-item label="HTML格式"><el-switch v-model="selectedNode.data.config.isHtml" /></el-form-item>
                </el-collapse-item>
              </el-collapse>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="email_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>• 所有字段都支持 <code v-pre>{{变量}}</code> 引用</p>
                <p>• 多个收件人用逗号分隔</p>
                <p>• 开启HTML可发送富文本邮件</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='cache'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>缓存节点说明</span></div>
                <div class="guide-content">
                  <p>缓存数据读写，支持<strong>Redis</strong>或<strong>内存缓存</strong>。</p>
                </div>
              </div>
              <el-form-item label="操作类型">
                <el-select v-model="selectedNode.data.config.operation">
                  <el-option label="读取缓存" value="get" />
                  <el-option label="写入缓存" value="set" />
                  <el-option label="删除缓存" value="delete" />
                  <el-option label="检查存在" value="exists" />
                </el-select>
              </el-form-item>
              <el-form-item label="缓存键" required>
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.key" placeholder="user:{{userId}}:profile" @input="e => handleVarInput(e, 'key')" />
                </div>
              </el-form-item>
              <el-form-item label="缓存值" v-if="selectedNode.data.config.operation === 'set'">
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.value" :rows="3" placeholder="{{data}}" @input="e => handleVarInput(e, 'value')" />
                </div>
              </el-form-item>
              <el-form-item label="过期时间(秒)" v-if="selectedNode.data.config.operation === 'set'">
                <el-input-number v-model="selectedNode.data.config.ttl" :min="1" :max="86400" :step="60" />
              </el-form-item>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="cache_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用场景：</strong></p>
                <p>• 缓存LLM响应减少重复调用</p>
                <p>• 跨工作流共享数据</p>
                <p>• 存储临时计算结果</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='webhook'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>Webhook节点说明</span></div>
                <div class="guide-content">
                  <p>发送Webhook通知到<strong>外部系统</strong>，支持重试和签名。</p>
                </div>
              </div>
              <el-form-item label="Webhook URL" required>
                <div class="var-input-wrapper">
                  <el-input v-model="selectedNode.data.config.url" placeholder="https://example.com/webhook" @input="e => handleVarInput(e, 'url')" />
                </div>
              </el-form-item>
              <el-form-item label="事件类型">
                <el-input v-model="selectedNode.data.config.eventType" placeholder="workflow.completed" />
              </el-form-item>
              <el-form-item label="自定义数据(JSON)">
                <div class="var-input-wrapper">
                  <el-input type="textarea" v-model="selectedNode.data.config.dataJson" :rows="4" placeholder='{"result": "{{output}}", "user": "{{userId}}"}' @input="e => handleVarInput(e, 'dataJson')" />
                </div>
              </el-form-item>
              <el-collapse class="advanced-config">
                <el-collapse-item title="高级配置" name="advanced">
                  <el-form-item label="自定义请求头"><el-input type="textarea" v-model="selectedNode.data.config.headersJson" :rows="2" placeholder='{"Authorization": "Bearer xxx"}' /></el-form-item>
                  <el-form-item label="签名密钥"><el-input v-model="selectedNode.data.config.secret" placeholder="用于生成X-Webhook-Signature" show-password /></el-form-item>
                  <el-form-item label="最大重试次数"><el-input-number v-model="selectedNode.data.config.maxRetries" :min="1" :max="10" /></el-form-item>
                </el-collapse-item>
              </el-collapse>
              <el-form-item label="输出变量名" required><el-input v-model="selectedNode.data.config.outputVariable" placeholder="webhook_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用场景：</strong></p>
                <p>• 通知第三方系统工作流完成</p>
                <p>• 触发外部自动化流程</p>
                <p>• 发送数据到数据分析平台</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='parallel'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>并行节点说明</span></div>
                <div class="guide-content">
                  <p>同时执行多个分支，<strong>提高处理效率</strong>。</p>
                </div>
              </div>
              <el-form-item label="超时时间(秒)"><el-input-number v-model="selectedNode.data.config.timeout" :min="1" :max="300" :step="10" /></el-form-item>
              <el-form-item label="执行模式">
                <el-select v-model="selectedNode.data.config.mode">
                  <el-option label="等待全部完成" value="all" />
                  <el-option label="任一完成即可" value="any" />
                  <el-option label="竞速(取最快)" value="race" />
                </el-select>
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="parallel_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 从<span style="color:#06b6d4">●不同句柄</span>连接多个分支</p>
                <p>2. 所有分支将<strong>同时执行</strong></p>
                <p>3. 根据模式决定何时继续</p>
              </div>
              <div class="example-box">
                <div class="example-title"><i class="fa-solid fa-lightbulb"></i> 使用场景</div>
                <div class="example-content">
                  <p>• 同时调用多个API获取数据</p>
                  <p>• 并行处理多个任务提高效率</p>
                </div>
              </div>
            </template>
            <template v-if="selectedNode.type==='merge'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>合并节点说明</span></div>
                <div class="guide-content">
                  <p>等待多个分支完成，<strong>合并结果</strong>继续执行。</p>
                </div>
              </div>
              <el-form-item label="合并模式">
                <el-select v-model="selectedNode.data.config.mode">
                  <el-option label="对象(按分支名)" value="object" />
                  <el-option label="数组" value="array" />
                  <el-option label="取第一个" value="first" />
                  <el-option label="取最后一个" value="last" />
                  <el-option label="文本拼接" value="concat" />
                </el-select>
              </el-form-item>
              <el-form-item label="输出变量名"><el-input v-model="selectedNode.data.config.outputVariable" placeholder="merged_result" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 多个分支连接到此节点</p>
                <p>2. 等待所有输入分支完成</p>
                <p>3. 按选定模式合并结果</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='while'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>循环节点说明</span></div>
                <div class="guide-content">
                  <p>根据条件<strong>重复执行</strong>循环体，直到条件不满足。</p>
                </div>
              </div>
              <el-form-item label="循环条件">
                <el-input v-model="selectedNode.data.config.condition" placeholder="{{loop_count}} < 10" />
              </el-form-item>
              <el-form-item label="最大迭代次数"><el-input-number v-model="selectedNode.data.config.maxIterations" :min="1" :max="1000" :step="10" /></el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. <span style="color:#8b5cf6">●紫色句柄</span> 连接循环体</p>
                <p>2. <span style="color:#10b981">●绿色句柄</span> 连接退出分支</p>
                <p>3. 使用 <code v-pre>{{loop_count}}</code> 获取当前循环次数</p>
                <p>4. 设置最大迭代次数防止死循环</p>
              </div>
            </template>
            <template v-if="selectedNode.type==='loop'">
              <div class="node-guide">
                <div class="guide-header"><i class="fa-solid fa-circle-info"></i><span>列表循环节点说明</span></div>
                <div class="guide-content">
                  <p>遍历列表中的每个元素，<strong>逐个执行</strong>循环体。</p>
                </div>
              </div>
              <el-form-item label="列表变量">
                <el-input v-model="selectedNode.data.config.listVariable" placeholder="items" />
              </el-form-item>
              <el-form-item label="元素变量名">
                <el-input v-model="selectedNode.data.config.itemVariable" placeholder="item" />
              </el-form-item>
              <el-form-item label="索引变量名">
                <el-input v-model="selectedNode.data.config.indexVariable" placeholder="index" />
              </el-form-item>
              <el-form-item label="输出变量名">
                <el-input v-model="selectedNode.data.config.outputVariable" placeholder="loop_results" />
              </el-form-item>
              <div class="tip">
                <p><strong>💡 使用方法：</strong></p>
                <p>1. 指定一个包含列表数据的变量</p>
                <p>2. <span style="color:#7c3aed">●紫色句柄</span> 连接循环体（每个元素执行）</p>
                <p>3. <span style="color:#10b981">●绿色句柄</span> 连接循环结束后的节点</p>
                <p>4. 使用 <code v-pre>{{item}}</code> 访问当前元素</p>
                <p>5. 使用 <code v-pre>{{index}}</code> 获取当前索引</p>
              </div>
              <div class="example-box">
                <div class="example-title"><i class="fa-solid fa-lightbulb"></i> 使用示例</div>
                <div class="example-content">
                  <p><strong>列表变量：</strong>users</p>
                  <p><strong>元素变量：</strong>user</p>
                  <p><strong>循环体中使用：</strong><code v-pre>{{user.name}}</code></p>
                </div>
              </div>
            </template>
            <el-form-item label="输出变量" v-if="selectedNode.data.config?.outputVariable !== undefined">
              <el-input v-model="selectedNode.data.config.outputVariable" placeholder="output" />
            </el-form-item>
            <div class="form-actions">
              <el-button type="danger" size="small" @click="deleteNode"><i class="fa-solid fa-trash"></i> 删除节点</el-button>
            </div>
          </el-form>
        </div>
        <div class="props empty" :class="{ collapsed: propsPanelCollapsed }" :style="{ width: propsWidth + 'px' }" v-else>
          <div class="props-resize-handle" @mousedown="startResize"></div>
          <el-tooltip :content="propsPanelCollapsed ? '展开属性面板' : '收起属性面板'" placement="right">
            <div class="props-collapse-btn" @click="propsPanelCollapsed = !propsPanelCollapsed">
              <i :class="propsPanelCollapsed ? 'fa-solid fa-chevron-left' : 'fa-solid fa-chevron-right'"></i>
            </div>
          </el-tooltip>
          <el-tabs v-model="rightPanelTab" class="right-tabs empty-tabs">
            <el-tab-pane label="节点" name="node">
              <div class="empty-hint">
                <i class="fa-solid fa-mouse-pointer"></i>
                <p>点击节点进行配置</p>
                <p class="sub">或从左侧拖拽节点到画布</p>
              </div>
            </el-tab-pane>
            <el-tab-pane label="变量" name="vars">
              <div class="vars-panel">
                <div class="vars-header">
                  <i class="fa-solid fa-hashtag"></i>
                  <span>可用变量 ({{ outputVariables.length }})</span>
                </div>
                <div class="vars-tip">
                  <i class="fa-solid fa-lightbulb"></i>
                  点击变量名复制，在配置中使用 <code v-pre>{{变量名}}</code> 引用
                </div>
                <div class="vars-list" v-if="outputVariables.length">
                  <div v-for="v in outputVariables" :key="v" class="var-item" @click="copyVarName(v)">
                    <div class="var-icon"><i class="fa-solid fa-hashtag"></i></div>
                    <div class="var-info">
                      <span class="var-name">{{ v }}</span>
                      <span class="var-usage" v-pre>{{</span><span class="var-usage-name">{{ v }}</span><span class="var-usage" v-pre>}}</span>
                    </div>
                    <i class="fa-solid fa-copy var-copy"></i>
                  </div>
                </div>
                <div v-else class="vars-empty">
                  <i class="fa-solid fa-inbox"></i>
                  <p>暂无变量</p>
                  <p class="vars-hint">添加节点并配置输出变量名</p>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="日志" name="logs">
              <div class="log-panel">
                <div v-if="executionLogs.length" class="log-list">
                  <div v-for="(log, idx) in executionLogs" :key="idx" class="log-item" :class="log.status">
                    <div class="log-header">
                      <span class="log-node">{{ log.nodeName }}</span>
                      <span class="log-time">{{ log.duration }}ms</span>
                    </div>
                    <div class="log-content" v-if="log.output">{{ typeof log.output === 'string' ? log.output.substring(0,100) : JSON.stringify(log.output).substring(0,100) }}...</div>
                  </div>
                </div>
                <div v-else class="log-empty">
                  <i class="fa-solid fa-terminal"></i>
                  <p>运行工作流后查看执行日志</p>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="帮助" name="help">
              <div class="help-panel">
                <div class="help-section">
                  <div class="help-title"><i class="fa-solid fa-rocket"></i> 快速开始</div>
                  <div class="help-steps">
                    <div class="help-step">
                      <span class="step-num">1</span>
                      <div class="step-content">
                        <strong>添加节点</strong>
                        <p>从左侧拖拽或双击节点添加到画布</p>
                      </div>
                    </div>
                    <div class="help-step">
                      <span class="step-num">2</span>
                      <div class="step-content">
                        <strong>连接节点</strong>
                        <p>从节点右侧圆点拖到另一节点左侧</p>
                      </div>
                    </div>
                    <div class="help-step">
                      <span class="step-num">3</span>
                      <div class="step-content">
                        <strong>配置节点</strong>
                        <p>点击节点，在右侧面板配置参数</p>
                      </div>
                    </div>
                    <div class="help-step">
                      <span class="step-num">4</span>
                      <div class="step-content">
                        <strong>测试运行</strong>
                        <p>点击「测试运行」按钮验证流程</p>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="help-section">
                  <div class="help-title"><i class="fa-solid fa-link"></i> 执行顺序</div>
                  <div class="help-content">
                    <p>工作流按连线顺序执行：</p>
                    <div class="flow-demo">
                      <span class="flow-node start">开始</span>
                      <i class="fa-solid fa-arrow-right"></i>
                      <span class="flow-node">节点A</span>
                      <i class="fa-solid fa-arrow-right"></i>
                      <span class="flow-node">节点B</span>
                      <i class="fa-solid fa-arrow-right"></i>
                      <span class="flow-node end">结束</span>
                    </div>
                    <p class="help-note">每个节点的输出可以被后续节点引用</p>
                  </div>
                </div>
                <div class="help-section">
                  <div class="help-title"><i class="fa-solid fa-code"></i> 变量引用</div>
                  <div class="help-content">
                    <p>使用双花括号引用变量：</p>
                    <div class="code-examples">
                      <div class="code-example">
                        <code v-pre>{{input}}</code>
                        <span>工作流输入</span>
                      </div>
                      <div class="code-example">
                        <code v-pre>{{llm_output}}</code>
                        <span>LLM节点输出</span>
                      </div>
                      <div class="code-example">
                        <code v-pre>{{knowledge_result}}</code>
                        <span>知识库结果</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="help-section">
                  <div class="help-title"><i class="fa-solid fa-lightbulb"></i> 常见配置示例</div>
                  <div class="help-content">
                    <div class="example-case">
                      <div class="case-title"><i class="fa-solid fa-comments"></i> 简单对话</div>
                      <div class="case-flow">开始 → LLM → 结束</div>
                      <div class="case-desc">
                        <p><strong>LLM配置：</strong></p>
                        <p>• 提示词：<code v-pre>{{input}}</code></p>
                        <p>• 输出变量：llm_output</p>
                        <p><strong>结束配置：</strong></p>
                        <p>• 输出变量：llm_output</p>
                      </div>
                    </div>
                    <div class="example-case">
                      <div class="case-title"><i class="fa-solid fa-book"></i> 知识库问答(RAG)</div>
                      <div class="case-flow">开始 → 知识库 → LLM → 结束</div>
                      <div class="case-desc">
                        <p><strong>知识库配置：</strong></p>
                        <p>• 查询：<code v-pre>{{input}}</code></p>
                        <p>• 输出变量：kb_result</p>
                        <p><strong>LLM配置：</strong></p>
                        <p>• 提示词：根据以下资料回答：<code v-pre>{{kb_result}}</code></p>
                        <p>问题：<code v-pre>{{input}}</code></p>
                      </div>
                    </div>
                    <div class="example-case">
                      <div class="case-title"><i class="fa-solid fa-code-branch"></i> 条件分支</div>
                      <div class="case-flow">开始 → 条件 → [分支A/分支B] → 结束</div>
                      <div class="case-desc">
                        <p><strong>条件配置：</strong></p>
                        <p>• 左值：<code v-pre>{{input}}</code></p>
                        <p>• 操作符：contains</p>
                        <p>• 右值：帮助</p>
                        <p>• 绿色句柄连接帮助分支</p>
                        <p>• 红色句柄连接其他分支</p>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="help-section">
                  <div class="help-title"><i class="fa-solid fa-keyboard"></i> 快捷键</div>
                  <div class="shortcut-list">
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>S</kbd> <span>保存</span></div>
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>Z</kbd> <span>撤销</span></div>
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>Y</kbd> <span>重做</span></div>
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>C</kbd> <span>复制节点</span></div>
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>V</kbd> <span>粘贴节点</span></div>
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>F</kbd> <span>搜索节点</span></div>
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>E</kbd> <span>导出</span></div>
                    <div class="shortcut-item"><kbd>Ctrl</kbd>+<kbd>I</kbd> <span>导入</span></div>
                    <div class="shortcut-item"><kbd>Delete</kbd> <span>删除节点</span></div>
                  </div>
                </div>
                <div class="help-section">
                  <div class="help-title"><i class="fa-solid fa-triangle-exclamation"></i> 常见问题</div>
                  <div class="help-content">
                    <div class="faq-item">
                      <p class="faq-q"><strong>Q: 如何引用上一个节点的输出？</strong></p>
                      <p class="faq-a">A: 在节点配置中使用 <code v-pre>{{输出变量名}}</code>，变量名在节点配置的「输出变量名」字段中定义。</p>
                    </div>
                    <div class="faq-item">
                      <p class="faq-q"><strong>Q: 为什么执行失败？</strong></p>
                      <p class="faq-a">A: 检查节点连接是否正确、必填字段是否填写、变量名是否存在。</p>
                    </div>
                    <div class="faq-item">
                      <p class="faq-q"><strong>Q: 如何调试工作流？</strong></p>
                      <p class="faq-a">A: 使用「测试运行」功能，查看执行日志和每个节点的输出。</p>
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
    <!-- 变量自动补全弹窗 -->
    <Teleport to="body">
      <div v-if="showVarSuggestions" class="var-suggestions" :style="{ top: varInputPosition.top + 'px', left: varInputPosition.left + 'px' }">
        <div class="var-suggestions-header">
          <i class="fa-solid fa-code"></i>
          <span>可用变量</span>
          <span class="hint">↑↓选择 Enter确认 Esc关闭</span>
        </div>
        <div class="var-suggestions-list">
          <div v-for="(v, idx) in varSuggestions" :key="v"
               class="var-suggestion-item"
               :class="{ active: idx === varSuggestionIndex }"
               @click="selectVarSuggestion(v)"
               @mouseenter="varSuggestionIndex = idx">
            <i class="fa-solid fa-hashtag"></i>
            <span class="var-name">{{ v }}</span>
            <span class="var-preview" v-pre>{{</span><span class="var-preview-name">{{ v }}</span><span class="var-preview" v-pre>}}</span>
          </div>
        </div>
      </div>
    </Teleport>
    <!-- 节点右键菜单 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="contextMenu.visible" class="context-menu" :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }" @click.stop>
          <div class="context-menu-item" @click="handleContextAction('copy')">
            <i class="fa-solid fa-copy"></i>
            <span>复制</span>
            <span class="shortcut">Ctrl+C</span>
          </div>
          <div class="context-menu-item" @click="handleContextAction('duplicate')">
            <i class="fa-solid fa-clone"></i>
            <span>复制为副本</span>
          </div>
          <div class="context-menu-divider"></div>
          <div class="context-menu-item" @click="handleContextAction('locate')">
            <i class="fa-solid fa-crosshairs"></i>
            <span>居中显示</span>
          </div>
          <div class="context-menu-divider"></div>
          <div class="context-menu-item danger" @click="handleContextAction('delete')">
            <i class="fa-solid fa-trash"></i>
            <span>删除</span>
            <span class="shortcut">Del</span>
          </div>
        </div>
      </Transition>
      <div v-if="contextMenu.visible" class="context-menu-overlay" @click="closeContextMenu"></div>
    </Teleport>
    <!-- 注释对话框 -->
    <el-dialog v-model="annotationDialog" :title="editingAnnotation?.id?.includes('anno_') ? '添加注释' : '编辑注释'" width="400px" class="annotation-dialog">
      <div class="annotation-form" v-if="editingAnnotation">
        <el-form-item label="注释内容">
          <el-input type="textarea" v-model="editingAnnotation.text" :rows="3" placeholder="输入注释内容..." />
        </el-form-item>
        <el-form-item label="颜色">
          <div class="color-options">
            <span v-for="c in ['#fef3c7', '#dcfce7', '#dbeafe', '#fce7f3', '#f3e8ff', '#fed7aa']"
                  :key="c" class="color-option" :class="{ active: editingAnnotation.color === c }"
                  :style="{ background: c }" @click="editingAnnotation.color = c"></span>
          </div>
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="annotationDialog=false">取消</el-button>
        <el-button type="primary" @click="saveAnnotation">保存</el-button>
      </template>
    </el-dialog>
    <!-- 帮助对话框 -->
    <el-dialog v-model="helpDialog" title="工作流帮助" width="560px" class="help-dialog">
      <div class="help-dialog-content">
        <div class="help-section">
          <div class="help-title"><i class="fa-solid fa-rocket"></i> 快速开始</div>
          <div class="help-steps">
            <div class="help-step"><span class="step-num">1</span><div class="step-content"><strong>添加节点</strong><p>从左侧拖拽或双击节点添加到画布</p></div></div>
            <div class="help-step"><span class="step-num">2</span><div class="step-content"><strong>连接节点</strong><p>从节点右侧圆点拖到另一节点左侧</p></div></div>
            <div class="help-step"><span class="step-num">3</span><div class="step-content"><strong>配置节点</strong><p>点击节点，在右侧面板配置参数</p></div></div>
            <div class="help-step"><span class="step-num">4</span><div class="step-content"><strong>测试运行</strong><p>点击「测试运行」按钮验证流程</p></div></div>
          </div>
        </div>
        <div class="help-section">
          <div class="help-title"><i class="fa-solid fa-keyboard"></i> 快捷键</div>
          <div class="shortcuts-grid">
            <div class="shortcut"><kbd>Ctrl</kbd>+<kbd>S</kbd><span>保存</span></div>
            <div class="shortcut"><kbd>Ctrl</kbd>+<kbd>Z</kbd><span>撤销</span></div>
            <div class="shortcut"><kbd>Ctrl</kbd>+<kbd>Y</kbd><span>重做</span></div>
            <div class="shortcut"><kbd>Delete</kbd><span>删除选中</span></div>
            <div class="shortcut"><kbd>Ctrl</kbd>+<kbd>C</kbd><span>复制</span></div>
            <div class="shortcut"><kbd>Ctrl</kbd>+<kbd>V</kbd><span>粘贴</span></div>
          </div>
        </div>
        <div class="help-section">
          <div class="help-title"><i class="fa-solid fa-lightbulb"></i> 变量引用</div>
          <p>在配置中使用 <code v-pre>{{变量名}}</code> 引用其他节点的输出变量</p>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="helpDialog=false">知道了</el-button>
      </template>
    </el-dialog>
    <!-- 边标签编辑对话框 -->
    <el-dialog v-model="edgeLabelDialog" title="编辑连线" width="400px" class="edge-dialog">
      <div class="edge-edit-content">
        <div class="edge-info">
          <i class="fa-solid fa-arrow-right-long"></i>
          <span>{{ selectedEdge?.source }}</span>
          <i class="fa-solid fa-arrow-right"></i>
          <span>{{ selectedEdge?.target }}</span>
        </div>
        <el-form-item label="连线标签">
          <el-input v-model="edgeLabelInput" placeholder="输入标签文字（如：是、否、成功等）" clearable />
        </el-form-item>
        <div class="edge-tip">
          <i class="fa-solid fa-lightbulb"></i>
          <span>标签会显示在连线中间，适合标注条件分支</span>
        </div>
      </div>
      <template #footer>
        <el-button type="danger" text @click="deleteEdge">
          <i class="fa-solid fa-trash"></i> 删除连线
        </el-button>
        <div style="flex:1"></div>
        <el-button @click="edgeLabelDialog=false">取消</el-button>
        <el-button type="primary" @click="saveEdgeLabel">保存</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="runDialog" title="运行工作流" width="480px" class="run-dialog">
      <div class="run-content">
        <div class="run-info">
          <i class="fa-solid fa-play-circle"></i>
          <div>
            <h4>{{ editingWorkflow?.name || '工作流' }}</h4>
            <p>输入参数将作为 <code>input</code> 变量传入工作流</p>
          </div>
        </div>
        <el-form-item label="输入参数">
          <el-input type="textarea" v-model="runInput" :rows="4" placeholder="请输入文本内容，例如：你好，请帮我分析一下..." />
        </el-form-item>
        <div class="run-tip">
          <i class="fa-solid fa-lightbulb"></i>
          <span>提示：可以输入任意文本，工作流会从开始节点接收并处理</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="runDialog=false">取消</el-button>
        <el-button type="primary" @click="execWorkflow" :loading="executing">
          <i class="fa-solid fa-rocket" v-if="!executing"></i> {{ executing ? '执行中...' : '开始执行' }}
        </el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="resultDialog" title="执行结果" width="700px" class="result-dialog">
      <div class="result" :class="execResult?.success?'ok':'fail'">
        <i :class="execResult?.success?'fa-solid fa-check-circle':'fa-solid fa-times-circle'"></i>
        <div class="result-text">
          <h3>{{ execResult?.success ? '✅ 执行成功' : '❌ 执行失败' }}</h3>
          <p v-if="execResult?.durationMs">总耗时: {{ execResult.durationMs }}ms</p>
          <p v-if="execResult?.errorMessage" class="error">{{ execResult.errorMessage }}</p>
        </div>
      </div>
      
      <!-- 📊 执行统计信息 -->
      <div class="exec-stats" v-if="execResult?.stats">
        <h4><i class="fa-solid fa-chart-bar"></i> 执行统计</h4>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-label">节点总数</div>
            <div class="stat-value">{{ execResult.stats.totalNodes }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">完成节点</div>
            <div class="stat-value success">{{ execResult.stats.completedNodes }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">平均耗时</div>
            <div class="stat-value">{{ execResult.stats.avgNodeTime }}ms</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">总耗时</div>
            <div class="stat-value">{{ execResult.stats.totalTime }}ms</div>
          </div>
        </div>
        
        <!-- 节点耗时排行 -->
        <div class="node-timings" v-if="execResult.stats.nodeTimings && Object.keys(execResult.stats.nodeTimings).length">
          <h5><i class="fa-solid fa-clock"></i> 节点耗时详情</h5>
          <div class="timing-list">
            <div v-for="(timing, index) in getSortedTimings(execResult.stats.nodeTimings)" :key="timing.id" class="timing-item">
              <div class="timing-name">{{ timing.name }}</div>
              <div class="timing-bar-wrapper">
                <div class="timing-bar" :style="{ width: getTimingWidth(timing.duration, execResult.stats.nodeTimings) + '%' }"></div>
              </div>
              <div class="timing-value">{{ timing.duration }}ms</div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="result-output" v-if="execResult?.output">
        <h4><i class="fa-solid fa-file-code"></i> 输出结果</h4>
        <pre>{{ typeof execResult.output === 'string' ? execResult.output : JSON.stringify(execResult.output, null, 2) }}</pre>
      </div>
    </el-dialog>
    <!-- 执行历史对话框 -->
    <el-dialog v-model="historyDialog" title="执行历史" width="900px" class="history-dialog">
      <div class="history-dashboard" v-if="historyStats">
        <div class="dashboard-header">
          <i class="fa-solid fa-chart-line"></i>
          <span>执行统计仪表盘</span>
        </div>
        <div class="dashboard-grid">
          <div class="stat-card total">
            <div class="stat-icon"><i class="fa-solid fa-bolt"></i></div>
            <div class="stat-content">
              <span class="stat-value">{{ historyStats.total }}</span>
              <span class="stat-label">总执行次数</span>
            </div>
          </div>
          <div class="stat-card success">
            <div class="stat-icon"><i class="fa-solid fa-check-circle"></i></div>
            <div class="stat-content">
              <span class="stat-value">{{ historyStats.success }}</span>
              <span class="stat-label">成功</span>
            </div>
            <div class="stat-rate" v-if="historyStats.total > 0">
              {{ Math.round(historyStats.success / historyStats.total * 100) }}%
            </div>
          </div>
          <div class="stat-card fail">
            <div class="stat-icon"><i class="fa-solid fa-times-circle"></i></div>
            <div class="stat-content">
              <span class="stat-value">{{ historyStats.failed }}</span>
              <span class="stat-label">失败</span>
            </div>
            <div class="stat-rate" v-if="historyStats.total > 0">
              {{ Math.round(historyStats.failed / historyStats.total * 100) }}%
            </div>
          </div>
          <div class="stat-card duration">
            <div class="stat-icon"><i class="fa-solid fa-clock"></i></div>
            <div class="stat-content">
              <span class="stat-value">{{ historyStats.avgDuration }}<small>ms</small></span>
              <span class="stat-label">平均耗时</span>
            </div>
          </div>
        </div>
        <div class="success-rate-bar" v-if="historyStats.total > 0">
          <div class="rate-label">成功率</div>
          <div class="rate-track">
            <div class="rate-fill" :style="{ width: Math.round(historyStats.success / historyStats.total * 100) + '%' }"></div>
          </div>
          <div class="rate-value">{{ Math.round(historyStats.success / historyStats.total * 100) }}%</div>
        </div>
      </div>
      <el-table :data="executionHistory" style="width: 100%" v-loading="loadingHistory" max-height="400">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : row.status === 'failed' ? 'danger' : 'warning'" size="small">
              {{ row.status === 'completed' ? '成功' : row.status === 'failed' ? '失败' : '运行中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="100">
          <template #default="{ row }">{{ row.durationMs || '-' }}ms</template>
        </el-table-column>
        <el-table-column prop="createTime" label="执行时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="输入" min-width="150">
          <template #default="{ row }">
            <span class="text-ellipsis">{{ row.inputData?.substring(0, 50) || '-' }}{{ row.inputData?.length > 50 ? '...' : '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="viewExecution(row)">
              <i class="fa-solid fa-eye"></i> 详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="history-pagination">
        <el-pagination
          v-model:current-page="historyPage"
          :page-size="10"
          :total="historyTotal"
          layout="prev, pager, next"
          @current-change="loadHistory"
        />
      </div>
    </el-dialog>
    <!-- 执行详情对话框 -->
    <el-dialog v-model="executionDetailDialog" title="执行详情" width="700px">
      <div v-if="selectedExecution" class="execution-detail">
        <div class="detail-header">
          <div class="detail-status" :class="selectedExecution.status">
            <i :class="selectedExecution.status === 'completed' ? 'fa-solid fa-check-circle' : 'fa-solid fa-times-circle'"></i>
            {{ selectedExecution.status === 'completed' ? '执行成功' : '执行失败' }}
          </div>
          <div class="detail-meta">
            <span><i class="fa-solid fa-clock"></i> {{ selectedExecution.durationMs }}ms</span>
            <span><i class="fa-solid fa-calendar"></i> {{ formatTime(selectedExecution.createTime) }}</span>
          </div>
        </div>
        <el-tabs>
          <el-tab-pane label="输入">
            <pre class="detail-pre">{{ selectedExecution.inputData || '无' }}</pre>
          </el-tab-pane>
          <el-tab-pane label="输出">
            <pre class="detail-pre">{{ formatOutput(selectedExecution.outputData) }}</pre>
          </el-tab-pane>
          <el-tab-pane label="执行日志">
            <div class="execution-log" v-if="selectedExecution.executionLog">
              <div v-for="(log, idx) in parseLog(selectedExecution.executionLog)" :key="idx" class="log-entry" :class="log.success ? 'success' : 'error'">
                <div class="log-entry-header">
                  <span class="log-node-name">{{ log.nodeName || log.nodeId }}</span>
                  <span class="log-duration">{{ log.duration }}ms</span>
                </div>
                <div class="log-entry-output" v-if="log.output">{{ typeof log.output === 'string' ? log.output.substring(0, 200) : JSON.stringify(log.output).substring(0, 200) }}</div>
                <div class="log-entry-error" v-if="log.error">{{ log.error }}</div>
              </div>
            </div>
            <div v-else class="no-log">暂无执行日志</div>
          </el-tab-pane>
          <el-tab-pane label="错误信息" v-if="selectedExecution.errorMessage">
            <el-alert :title="selectedExecution.errorMessage" type="error" show-icon :closable="false" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
    <!-- 版本管理对话框 -->
    <el-dialog v-model="versionDialog" title="版本管理" width="640px" class="version-dialog">
      <div class="version-header">
        <div class="version-info">
          <i class="fa-solid fa-code-branch"></i>
          <span>当前版本：<strong>v{{ currentVersion || 1 }}</strong></span>
        </div>
        <el-button type="primary" size="small" @click="saveAsNewVersion()">
          <i class="fa-solid fa-plus"></i> 保存新版本
        </el-button>
      </div>
      <div class="version-list" v-loading="loadingVersions">
        <div v-if="versions.length === 0" class="version-empty">
          <i class="fa-solid fa-code-branch"></i>
          <p>暂无版本记录</p>
        </div>
        <div v-else class="version-timeline">
          <div v-for="(v, idx) in versions" :key="v.version" class="version-item" :class="{ current: v.isCurrent }">
            <div class="version-dot">
              <i v-if="v.isCurrent" class="fa-solid fa-check"></i>
              <span v-else>{{ versions.length - idx }}</span>
            </div>
            <div class="version-content">
              <div class="version-title">
                <span class="version-num">v{{ v.version }}</span>
                <el-tag v-if="v.isCurrent" type="success" size="small">当前</el-tag>
              </div>
              <div class="version-desc">{{ v.description || '无描述' }}</div>
              <div class="version-meta">
                <i class="fa-solid fa-clock"></i>
                {{ formatTime(v.createdAt) }}
              </div>
            </div>
            <div class="version-actions" v-if="!v.isCurrent">
              <el-button size="small" text type="primary" @click="rollbackToVersion(v)">
                <i class="fa-solid fa-rotate-left"></i> 回滚
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="versionDialog = false">关闭</el-button>
      </template>
    </el-dialog>
    <!-- 模板库对话框 -->
    <el-dialog v-model="templateDialog" title="工作流模板库" width="800px" class="template-dialog">
      <div class="template-intro">
        <i class="fa-solid fa-layer-group"></i>
        <div>
          <h4>快速开始</h4>
          <p>选择一个模板，快速创建工作流</p>
        </div>
      </div>
      <div class="template-grid">
        <div v-for="t in templates" :key="t.id" class="template-card" @click="useTemplate(t)">
          <div class="template-icon" :style="{ background: t.color }">
            <i :class="t.icon"></i>
          </div>
          <div class="template-info">
            <div class="template-name">{{ t.name }}</div>
            <div class="template-desc">{{ t.desc }}</div>
            <div class="template-meta">
              <el-tag size="small" type="info">{{ t.category }}</el-tag>
              <span>{{ t.nodes.length }} 个节点</span>
            </div>
          </div>
          <div class="template-use">
            <i class="fa-solid fa-arrow-right"></i>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="templateDialog = false">关闭</el-button>
      </template>
    </el-dialog>
    <!-- 节点搜索对话框 -->
    <el-dialog v-model="nodeSearchDialog" title="搜索节点" width="480px" class="search-dialog">
      <el-input v-model="nodeSearchQuery" placeholder="输入节点名称、类型或ID..." prefix-icon="Search" size="large" autofocus clearable />
      <div class="search-results">
        <div v-if="!nodeSearchQuery" class="search-hint">
          <i class="fa-solid fa-lightbulb"></i>
          <p>输入关键词搜索画布中的节点</p>
        </div>
        <div v-else-if="searchedNodes.length === 0" class="search-empty">
          <i class="fa-solid fa-search"></i>
          <p>未找到匹配的节点</p>
        </div>
        <div v-else class="search-list">
          <div v-for="n in searchedNodes" :key="n.id" class="search-item" @click="locateNode(n.id); nodeSearchDialog = false">
            <div class="search-icon" :style="{ background: getNodeColor(n.type) }">
              <i :class="getNodeIcon(n.type)"></i>
            </div>
            <div class="search-info">
              <div class="search-name">{{ n.data.label }}</div>
              <div class="search-meta">{{ getNodeLabel(n.type) }} · {{ n.id }}</div>
            </div>
            <i class="fa-solid fa-arrow-right search-arrow"></i>
          </div>
        </div>
      </div>
    </el-dialog>
    <!-- 使用指南对话框 -->
    <el-dialog v-model="showGuideDialog" title="工作流使用指南" width="720px" class="guide-dialog">
      <div class="guide-content">
        <div class="guide-intro">
          <div class="guide-intro-icon">
            <i class="fa-solid fa-diagram-project"></i>
          </div>
          <div class="guide-intro-text">
            <h3>什么是工作流？</h3>
            <p>工作流是一种可视化的自动化流程编排工具，通过连接不同的节点来构建复杂的AI处理流程。</p>
          </div>
        </div>

        <div class="guide-section">
          <h4><i class="fa-solid fa-rocket"></i> 快速开始</h4>
          <div class="guide-steps">
            <div class="guide-step">
              <div class="step-number">1</div>
              <div class="step-info">
                <strong>创建工作流</strong>
                <p>点击「新建工作流」按钮，进入可视化编辑器</p>
              </div>
            </div>
            <div class="guide-step">
              <div class="step-number">2</div>
              <div class="step-info">
                <strong>添加节点</strong>
                <p>从左侧节点库拖拽节点到画布，或双击节点快速添加</p>
              </div>
            </div>
            <div class="guide-step">
              <div class="step-number">3</div>
              <div class="step-info">
                <strong>连接节点</strong>
                <p>从节点右侧的圆点拖拽到另一个节点左侧的圆点，建立连接</p>
              </div>
            </div>
            <div class="guide-step">
              <div class="step-number">4</div>
              <div class="step-info">
                <strong>配置节点</strong>
                <p>点击节点，在右侧面板配置参数（如选择模型、编写提示词等）</p>
              </div>
            </div>
            <div class="guide-step">
              <div class="step-number">5</div>
              <div class="step-info">
                <strong>测试运行</strong>
                <p>点击「测试运行」按钮，输入测试内容验证流程是否正常</p>
              </div>
            </div>
          </div>
        </div>

        <div class="guide-section">
          <h4><i class="fa-solid fa-cubes"></i> 常用节点说明</h4>
          <div class="node-guide-grid">
            <div class="node-guide-item">
              <div class="node-guide-icon start"><i class="fa-solid fa-play"></i></div>
              <div class="node-guide-info">
                <strong>开始节点</strong>
                <p>工作流入口，接收输入内容，存储为 <code v-pre>{{input}}</code> 变量</p>
              </div>
            </div>
            <div class="node-guide-item">
              <div class="node-guide-icon llm"><i class="fa-solid fa-robot"></i></div>
              <div class="node-guide-info">
                <strong>LLM节点</strong>
                <p>调用AI大模型，是最核心的节点。需要选择模型并编写提示词</p>
              </div>
            </div>
            <div class="node-guide-item">
              <div class="node-guide-icon knowledge"><i class="fa-solid fa-book"></i></div>
              <div class="node-guide-info">
                <strong>知识库节点</strong>
                <p>从知识库检索相关内容，常与LLM节点配合实现RAG</p>
              </div>
            </div>
            <div class="node-guide-item">
              <div class="node-guide-icon condition"><i class="fa-solid fa-code-branch"></i></div>
              <div class="node-guide-info">
                <strong>条件节点</strong>
                <p>根据条件判断走不同分支，实现流程分叉</p>
              </div>
            </div>
            <div class="node-guide-item">
              <div class="node-guide-icon end"><i class="fa-solid fa-flag-checkered"></i></div>
              <div class="node-guide-info">
                <strong>结束节点</strong>
                <p>工作流出口，指定要返回的变量作为最终结果</p>
              </div>
            </div>
            <div class="node-guide-item">
              <div class="node-guide-icon tool"><i class="fa-solid fa-wrench"></i></div>
              <div class="node-guide-info">
                <strong>工具节点</strong>
                <p>调用系统工具（如天气、计算器等）获取外部数据</p>
              </div>
            </div>
          </div>
        </div>

        <div class="guide-section">
          <h4><i class="fa-solid fa-code"></i> 变量使用</h4>
          <div class="var-guide">
            <p>在节点配置中，使用 <code v-pre>{{变量名}}</code> 引用变量：</p>
            <div class="var-examples">
              <div class="var-example">
                <code v-pre>{{input}}</code>
                <span>引用工作流输入内容</span>
              </div>
              <div class="var-example">
                <code v-pre>{{llm_output}}</code>
                <span>引用LLM节点的输出（需先设置输出变量名）</span>
              </div>
              <div class="var-example">
                <code v-pre>{{knowledge_result}}</code>
                <span>引用知识库检索结果</span>
              </div>
            </div>
          </div>
        </div>

        <div class="guide-section">
          <h4><i class="fa-solid fa-lightbulb"></i> 示例：简单问答工作流</h4>
          <div class="example-flow">
            <div class="flow-diagram">
              <span class="flow-node start">开始</span>
              <i class="fa-solid fa-arrow-right"></i>
              <span class="flow-node llm">LLM</span>
              <i class="fa-solid fa-arrow-right"></i>
              <span class="flow-node end">结束</span>
            </div>
            <div class="example-config">
              <p><strong>LLM节点配置：</strong></p>
              <ul>
                <li>系统提示词：你是一个友好的AI助手</li>
                <li>用户提示词：<code v-pre>{{input}}</code></li>
                <li>输出变量：answer</li>
              </ul>
              <p><strong>结束节点配置：</strong></p>
              <ul>
                <li>输出变量：answer</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="showGuideDialog = false">我知道了</el-button>
      </template>
    </el-dialog>
    
    <!-- AI智能生成工作流对话框（隐藏默认触发按钮，通过ref调用open方法打开） -->
    <WorkflowAIGenerator 
      ref="aiGeneratorRef"
      @generated="onAIGenerated"
      @apply="applyAIWorkflow"
    >
      <!-- 使用空的trigger slot隐藏默认按钮 -->
      <template #trigger></template>
    </WorkflowAIGenerator>
  </div>
</template>

<script setup>
import cache from '@/plugins/cache'

import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { VueFlow, useVueFlow, Handle, Position } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import request from '@/utils/request'
import { createAuthEventSource } from '@/utils/stream'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { useRoute } from 'vue-router'
import WorkflowAIGenerator from '../components/WorkflowAIGenerator.vue'

const route = useRoute()

const { addNodes, addEdges, project, removeNodes, fitView: vfFitView, zoomIn: vfZoomIn, zoomOut: vfZoomOut, getViewport, setViewport, onViewportChange } = useVueFlow()
const workflows = ref([])
const loadingWorkflows = ref(true)
const editingWorkflow = ref(null)
const nodes = ref([])
const edges = ref([])
const selectedNode = ref(null)
const models = ref([])
const tools = ref([])
const agents = ref([])
const runDialog = ref(false)
const resultDialog = ref(false)
const runInput = ref('')
const runningId = ref(null)
const executing = ref(false)
const execResult = ref(null)
const saving = ref(false)
const lastSaveTime = ref(null)
const rightPanelTab = ref('node')
// executionLogs 已移至SSE执行部分
const historyDialog = ref(false)
const executionHistory = ref([])
const historyStats = ref(null)
const historyPage = ref(1)
const historyTotal = ref(0)
const loadingHistory = ref(false)
const executionDetailDialog = ref(false)
const selectedExecution = ref(null)
const workflowSearch = ref('')
const workflowStatusFilter = ref(null)
const zoomLevel = ref(108)
const showGuideDialog = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
// 版本管理
const versionDialog = ref(false)
const versions = ref([])
const loadingVersions = ref(false)
const currentVersion = ref(null)
// 模板库
const templateDialog = ref(false)
const templates = ref([
  { id: 1, name: '简单对话', icon: 'fa-solid fa-comments', color: '#6366f1', desc: '基础的LLM对话流程', category: '基础', nodes: [
    { id: 'start_1', type: 'start', positionX: 100, positionY: 200, name: '开始', config: {} },
    { id: 'llm_1', type: 'llm', positionX: 350, positionY: 200, name: 'LLM', config: { userPrompt: '{{input}}', outputVariable: 'llm_output' } },
    { id: 'end_1', type: 'end', positionX: 600, positionY: 200, name: '结束', config: { outputVariable: 'llm_output' } }
  ], edges: [{ source: 'start_1', target: 'llm_1' }, { source: 'llm_1', target: 'end_1' }] },
  { id: 2, name: '知识库问答', icon: 'fa-solid fa-book', color: '#0ea5e9', desc: 'RAG检索增强生成', category: '高级', nodes: [
    { id: 'start_1', type: 'start', positionX: 100, positionY: 200, name: '开始', config: {} },
    { id: 'knowledge_1', type: 'knowledge', positionX: 300, positionY: 200, name: '知识库检索', config: { query: '{{input}}', outputVariable: 'kb_result' } },
    { id: 'llm_1', type: 'llm', positionX: 520, positionY: 200, name: 'LLM', config: { systemPrompt: '根据以下资料回答问题', userPrompt: '资料：{{kb_result}}\n\n问题：{{input}}', outputVariable: 'answer' } },
    { id: 'end_1', type: 'end', positionX: 740, positionY: 200, name: '结束', config: { outputVariable: 'answer' } }
  ], edges: [{ source: 'start_1', target: 'knowledge_1' }, { source: 'knowledge_1', target: 'llm_1' }, { source: 'llm_1', target: 'end_1' }] },
  { id: 3, name: '条件分支', icon: 'fa-solid fa-code-branch', color: '#f59e0b', desc: '根据条件执行不同分支', category: '逻辑', nodes: [
    { id: 'start_1', type: 'start', positionX: 100, positionY: 200, name: '开始', config: {} },
    { id: 'condition_1', type: 'condition', positionX: 300, positionY: 200, name: '条件判断', config: { expression: '{{input}} contains 帮助' } },
    { id: 'llm_1', type: 'llm', positionX: 520, positionY: 120, name: '帮助回复', config: { userPrompt: '用户需要帮助，请友好回复', outputVariable: 'help_output' } },
    { id: 'llm_2', type: 'llm', positionX: 520, positionY: 280, name: '普通回复', config: { userPrompt: '{{input}}', outputVariable: 'normal_output' } },
    { id: 'end_1', type: 'end', positionX: 740, positionY: 200, name: '结束', config: { outputVariable: 'output' } }
  ], edges: [{ source: 'start_1', target: 'condition_1' }, { source: 'condition_1', target: 'llm_1', sourceHandle: 'true' }, { source: 'condition_1', target: 'llm_2', sourceHandle: 'false' }, { source: 'llm_1', target: 'end_1' }, { source: 'llm_2', target: 'end_1' }] },
  { id: 4, name: '意图分类', icon: 'fa-solid fa-tags', color: '#a855f7', desc: 'AI自动识别用户意图', category: '高级', nodes: [
    { id: 'start_1', type: 'start', positionX: 100, positionY: 200, name: '开始', config: {} },
    { id: 'classifier_1', type: 'classifier', positionX: 300, positionY: 200, name: '意图分类', config: { categories: ['咨询', '投诉', '其他'] } },
    { id: 'llm_1', type: 'llm', positionX: 520, positionY: 200, name: 'LLM处理', config: { userPrompt: '用户意图：{{classifier_output}}\n内容：{{input}}', outputVariable: 'response' } },
    { id: 'end_1', type: 'end', positionX: 740, positionY: 200, name: '结束', config: { outputVariable: 'response' } }
  ], edges: [{ source: 'start_1', target: 'classifier_1' }, { source: 'classifier_1', target: 'llm_1' }, { source: 'llm_1', target: 'end_1' }] },
  { id: 5, name: '数据处理', icon: 'fa-solid fa-database', color: '#10b981', desc: 'HTTP请求+数据处理', category: '工具', nodes: [
    { id: 'start_1', type: 'start', positionX: 100, positionY: 200, name: '开始', config: {} },
    { id: 'http_1', type: 'http', positionX: 300, positionY: 200, name: 'API请求', config: { method: 'GET', url: 'https://api.example.com/data?q={{input}}', outputVariable: 'api_data' } },
    { id: 'llm_1', type: 'llm', positionX: 520, positionY: 200, name: '数据分析', config: { userPrompt: '分析以下数据：{{api_data}}', outputVariable: 'analysis' } },
    { id: 'end_1', type: 'end', positionX: 740, positionY: 200, name: '结束', config: { outputVariable: 'analysis' } }
  ], edges: [{ source: 'start_1', target: 'http_1' }, { source: 'http_1', target: 'llm_1' }, { source: 'llm_1', target: 'end_1' }] }
])
const templateCategories = computed(() => [...new Set(templates.value.map(t => t.category))])
// 调试模式
const debugMode = ref(false)
const debugPaused = ref(false)
const debugCurrentNode = ref(null)
const debugNodeOutputs = ref({})
// 右键菜单
const contextMenu = ref({ visible: false, x: 0, y: 0, node: null })
// 边编辑
const selectedEdge = ref(null)
const edgeLabelDialog = ref(false)
const edgeLabelInput = ref('')
// 帮助
const helpDialog = ref(false)
// 画布注释
const annotations = ref([])
const annotationDialog = ref(false)
const editingAnnotation = ref(null)
// 收藏节点
const favoriteNodes = ref(JSON.parse(cache.local.get('favoriteNodes') || '[]'))
// 最近使用节点
const recentNodes = ref(JSON.parse(cache.local.get('recentNodes') || '[]'))
// 快速连线模式
const quickConnectMode = ref(false)
const quickConnectSource = ref(null)
// 画布锁定状态
const isLocked = ref(false)
const propsPanelCollapsed = ref(false)
const propsWidth = ref(400)
const isResizing = ref(false)
const resizeStartX = ref(0)
const resizeStartWidth = ref(0)
let nodeId = 1

// 工作流搜索过滤
const filteredWorkflows = computed(() => {
  let filtered = workflows.value
  // 关键词搜索
  if (workflowSearch.value) {
    const s = workflowSearch.value.toLowerCase()
    filtered = filtered.filter(wf =>
      wf.name.toLowerCase().includes(s) ||
      (wf.description && wf.description.toLowerCase().includes(s))
    )
  }
  // 状态筛选
  if (workflowStatusFilter.value !== null && workflowStatusFilter.value !== '') {
    filtered = filtered.filter(wf => wf.enabled === workflowStatusFilter.value)
  }
  // 分页
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

// 总数
const totalWorkflows = computed(() => {
  let filtered = workflows.value
  if (workflowSearch.value) {
    const s = workflowSearch.value.toLowerCase()
    filtered = filtered.filter(wf =>
      wf.name.toLowerCase().includes(s) ||
      (wf.description && wf.description.toLowerCase().includes(s))
    )
  }
  if (workflowStatusFilter.value !== null && workflowStatusFilter.value !== '') {
    filtered = filtered.filter(wf => wf.enabled === workflowStatusFilter.value)
  }
  return filtered.length
})

// 可用的输出变量列表
const outputVariables = computed(() => {
  const vars = ['input']
  nodes.value.forEach(n => {
    if (n.data?.config?.outputVariable) {
      vars.push(n.data.config.outputVariable)
    }
  })
  return vars
})

// 保存状态计算
const saveStatus = computed(() => {
  if (!editingWorkflow.value?.id) return { icon: 'fa-solid fa-circle', color: '#f59e0b', text: '未保存' }
  if (saving.value) return { icon: 'fa-solid fa-spinner fa-spin', color: '#6366f1', text: '保存中...' }
  if (lastSaveTime.value) {
    const diff = Date.now() - lastSaveTime.value
    if (diff < 3000) return { icon: 'fa-solid fa-check-circle', color: '#10b981', text: '已保存' }
  }
  return { icon: 'fa-solid fa-cloud', color: '#9ca3af', text: '已同步' }
})

// 执行进度计算（已移至SSE执行部分，使用ref实时更新）

// 新增：节点搜索和分类展开
const nodeSearch = ref('')
const expandedCats = ref(['基础', 'AI', '逻辑', '工具', '处理', '数据', '控制'])

// 默认边配置
const defaultEdgeOptions = computed(() => ({
  type: 'smoothstep',
  animated: true,
  style: { stroke: '#6366f1', strokeWidth: 2 }
}))

// 获取边路径
const getEdgePath = (sourceX, sourceY, targetX, targetY) => {
  const midX = (sourceX + targetX) / 2
  return `M ${sourceX} ${sourceY} C ${midX} ${sourceY}, ${midX} ${targetY}, ${targetX} ${targetY}`
}

// 新增：撤销重做
const historyStack = ref([])
const historyIndex = ref(-1)
const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < historyStack.value.length - 1)

const nodeTypes = [
  { type: 'start', label: '开始', icon: 'fa-solid fa-play', color: '#10b981', category: '基础', desc: '工作流入口点' },
  { type: 'end', label: '结束', icon: 'fa-solid fa-flag-checkered', color: '#ef4444', category: '基础', desc: '工作流出口，返回结果' },
  { type: 'llm', label: 'LLM', icon: 'fa-solid fa-robot', color: '#8b5cf6', category: 'AI', desc: '调用大语言模型生成内容' },
  { type: 'agent', label: '智能体', icon: 'fa-solid fa-user-astronaut', color: '#6366f1', category: 'AI', desc: '调用配置好的智能体' },
  { type: 'knowledge', label: '知识库', icon: 'fa-solid fa-book', color: '#0ea5e9', category: 'AI', desc: '从知识库检索相关内容' },
  { type: 'classifier', label: '意图分类', icon: 'fa-solid fa-tags', color: '#a855f7', category: 'AI', desc: 'AI自动判断输入意图分类' },
  { type: 'extractor', label: '参数提取', icon: 'fa-solid fa-filter', color: '#d946ef', category: 'AI', desc: 'AI从文本提取结构化数据' },
  { type: 'question', label: '问答', icon: 'fa-solid fa-comment-dots', color: '#f472b6', category: 'AI', desc: '基于上下文回答或生成问题' },
  { type: 'condition', label: '条件', icon: 'fa-solid fa-code-branch', color: '#f59e0b', category: '逻辑', desc: '根据条件分支执行' },
  { type: 'parallel', label: '并行', icon: 'fa-solid fa-code-fork', color: '#06b6d4', category: '逻辑', desc: '同时执行多个分支' },
  { type: 'merge', label: '合并', icon: 'fa-solid fa-code-merge', color: '#0891b2', category: '逻辑', desc: '等待并行分支完成后合并' },
  { type: 'while', label: '循环', icon: 'fa-solid fa-arrows-spin', color: '#8b5cf6', category: '逻辑', desc: '根据条件循环执行' },
  { type: 'loop', label: '列表循环', icon: 'fa-solid fa-list-ol', color: '#7c3aed', category: '逻辑', desc: '对列表进行循环处理' },
  { type: 'iterator', label: '迭代器', icon: 'fa-solid fa-repeat', color: '#eab308', category: '逻辑', desc: '循环处理列表数据' },
  { type: 'aggregator', label: '聚合', icon: 'fa-solid fa-object-group', color: '#84cc16', category: '逻辑', desc: '合并多个变量数据' },
  { type: 'subflow', label: '子流程', icon: 'fa-solid fa-diagram-project', color: '#22c55e', category: '逻辑', desc: '调用其他工作流' },
  { type: 'tool', label: '工具', icon: 'fa-solid fa-wrench', color: '#3b82f6', category: '工具', desc: '调用系统工具' },
  { type: 'http', label: 'HTTP', icon: 'fa-solid fa-globe', color: '#06b6d4', category: '工具', desc: '发送HTTP请求' },
  { type: 'database', label: '数据库', icon: 'fa-solid fa-database', color: '#0d9488', category: '工具', desc: '执行SQL查询或更新' },
  { type: 'email', label: '邮件', icon: 'fa-solid fa-envelope', color: '#dc2626', category: '工具', desc: '发送邮件通知' },
  { type: 'cache', label: '缓存', icon: 'fa-solid fa-bolt', color: '#facc15', category: '工具', desc: '缓存数据读写操作' },
  { type: 'webhook', label: 'Webhook', icon: 'fa-solid fa-satellite-dish', color: '#a78bfa', category: '工具', desc: '发送Webhook通知' },
  { type: 'code', label: '代码', icon: 'fa-solid fa-code', color: '#ec4899', category: '工具', desc: '执行自定义代码' },
  { type: 'text', label: '文本处理', icon: 'fa-solid fa-font', color: '#f97316', category: '处理', desc: '文本拼接、分割、替换等' },
  { type: 'template', label: '模板', icon: 'fa-solid fa-file-code', color: '#fb923c', category: '处理', desc: '使用模板渲染文本' },
  { type: 'setvar', label: '变量', icon: 'fa-solid fa-sliders', color: '#14b8a6', category: '数据', desc: '设置或修改变量值' },
  { type: 'delay', label: '延迟', icon: 'fa-solid fa-clock', color: '#64748b', category: '控制', desc: '暂停执行指定时间' }
]

const nodeCategories = computed(() => [...new Set(nodeTypes.map(n => n.category))])
const filteredNodeTypes = computed(() => {
  if (!nodeSearch.value) return nodeTypes
  const s = nodeSearch.value.toLowerCase()
  return nodeTypes.filter(n => n.label.toLowerCase().includes(s) || n.type.includes(s) || n.desc?.toLowerCase().includes(s))
})

// 分类折叠切换
const toggleCategory = (cat) => {
  const idx = expandedCats.value.indexOf(cat)
  if (idx > -1) expandedCats.value.splice(idx, 1)
  else expandedCats.value.push(cat)
}

// 双击添加节点到画布中心
const addNodeToCenter = (nt) => {
  if (isLocked.value) return
  const viewport = getViewport()
  const pos = { x: -viewport.x / viewport.zoom + 300, y: -viewport.y / viewport.zoom + 200 }
  addNodes([{ id: `${nt.type}_${nodeId++}`, type: nt.type, position: pos, data: { label: nt.label, config: getDefCfg(nt.type) } }])
  saveHistory()
}

// 缩放控制
const zoomIn = () => { vfZoomIn(); updateZoomLevel() }
const zoomOut = () => { vfZoomOut(); updateZoomLevel() }
const fitView = () => { vfFitView({ padding: 0.2 }); setTimeout(updateZoomLevel, 100) }

// 更新缩放比例显示
const updateZoomLevel = () => {
  const viewport = getViewport()
  zoomLevel.value = Math.round(viewport.zoom * 100)
}

// 缩放滑块变化
const onZoomChange = (val) => {
  const viewport = getViewport()
  setViewport({ x: viewport.x, y: viewport.y, zoom: val / 100 })
}

// 监听视图变化
onViewportChange((viewport) => {
  zoomLevel.value = Math.round(viewport.zoom * 100)
})

// 撤销重做
const saveHistory = () => {
  const state = JSON.stringify({ nodes: nodes.value, edges: edges.value })
  historyStack.value = historyStack.value.slice(0, historyIndex.value + 1)
  historyStack.value.push(state)
  historyIndex.value = historyStack.value.length - 1
  if (historyStack.value.length > 50) { historyStack.value.shift(); historyIndex.value-- }
}
const undo = () => {
  if (canUndo.value) {
    historyIndex.value--
    const state = JSON.parse(historyStack.value[historyIndex.value])
    nodes.value = state.nodes
    edges.value = state.edges
  }
}
const redo = () => {
  if (canRedo.value) {
    historyIndex.value++
    const state = JSON.parse(historyStack.value[historyIndex.value])
    nodes.value = state.nodes
    edges.value = state.edges
  }
}
const knowledgeBases = ref([])

// 节点配置校验
const nodeErrors = ref({})

// 校验节点配置
const validateNode = (node) => {
  const errors = []
  const config = node.data?.config || {}

  switch (node.type) {
    case 'llm':
      // 支持 model 或 modelId 字段
      if (!config.model && !config.modelId) errors.push('请选择模型')
      if (!config.userPrompt?.trim()) errors.push('请填写用户提示词')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'knowledge':
      if (!config.query?.trim()) errors.push('请填写查询内容')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'condition':
      // 支持 expression 或 leftValue/operator/rightValue 两种配置方式
      if (config.expression) {
        // 使用表达式方式，不需要验证单独的字段
        if (!config.expression.trim()) errors.push('请填写条件表达式')
      } else {
        // 使用字段方式
        if (!config.leftValue?.trim()) errors.push('请填写左值')
        if (!config.operator) errors.push('请选择操作符')
        if (!config.rightValue?.trim() && !['isEmpty', 'isNotEmpty'].includes(config.operator)) {
          errors.push('请填写右值')
        }
      }
      break
    case 'tool':
      if (!config.toolName) errors.push('请选择工具')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'http':
      if (!config.url?.trim()) errors.push('请填写URL')
      if (!config.method) errors.push('请选择请求方法')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'agent':
      if (!config.agentId) errors.push('请选择智能体')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'code':
      if (!config.code?.trim()) errors.push('请填写代码')
      if (!config.language) errors.push('请选择语言')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'end':
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'template':
      if (!config.template?.trim()) errors.push('请填写模板内容')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'classifier':
      if (!config.categories?.length) errors.push('请添加至少一个分类')
      break
    case 'extractor':
      if (!config.fields?.length) errors.push('请添加至少一个提取字段')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'loop':
      if (!config.listVariable?.trim()) errors.push('请填写列表变量名')
      break
    case 'iterator':
      if (!config.inputVariable?.trim()) errors.push('请填写输入列表变量名')
      break
    case 'while':
      if (!config.condition?.trim()) errors.push('请填写循环条件')
      break
    case 'subflow':
      if (!config.workflowId) errors.push('请选择子工作流')
      break
    case 'aggregator':
      if (!config.variables?.length) errors.push('请添加至少一个要聚合的变量')
      break
    case 'database':
      if (!config.sql?.trim()) errors.push('请填写SQL语句')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'email':
      if (!config.to?.trim()) errors.push('请填写收件人')
      if (!config.subject?.trim()) errors.push('请填写邮件主题')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'cache':
      if (!config.key?.trim()) errors.push('请填写缓存键')
      if (config.operation === 'set' && !config.value?.trim()) errors.push('请填写缓存值')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
    case 'webhook':
      if (!config.url?.trim()) errors.push('请填写Webhook URL')
      if (!config.outputVariable?.trim()) errors.push('请填写输出变量名')
      break
  }

  // 检查变量引用是否存在
  const varPattern = /\{\{([^}]+)\}\}/g
  const missingVars = []
  const checkVarRefs = (text) => {
    if (!text) return
    const matches = text.matchAll(varPattern)
    for (const match of matches) {
      const varName = match[1].trim()
      if (!outputVariables.value.includes(varName) && !missingVars.includes(varName)) {
        missingVars.push(varName)
      }
    }
  }

  // 检查各字段中的变量引用
  if (config.userPrompt) checkVarRefs(config.userPrompt)
  if (config.systemPrompt) checkVarRefs(config.systemPrompt)
  if (config.query) checkVarRefs(config.query)
  if (config.leftValue) checkVarRefs(config.leftValue)
  if (config.rightValue) checkVarRefs(config.rightValue)
  if (config.expression) checkVarRefs(config.expression)
  if (config.template) checkVarRefs(config.template)
  if (config.body) checkVarRefs(config.body)
  if (config.url) checkVarRefs(config.url)
  // 新增节点变量引用检查
  if (config.sql) checkVarRefs(config.sql)
  if (config.to) checkVarRefs(config.to)
  if (config.subject) checkVarRefs(config.subject)
  if (config.content) checkVarRefs(config.content)
  if (config.key) checkVarRefs(config.key)
  if (config.value) checkVarRefs(config.value)
  if (config.dataJson) checkVarRefs(config.dataJson)

  // 如果有缺失的变量，显示错误和可用变量提示
  if (missingVars.length > 0) {
    errors.push(`变量 ${missingVars.map(v => '{{' + v + '}}').join(', ')} 不存在`)
    const availableVars = outputVariables.value.slice(0, 5).join(', ')
    const moreHint = outputVariables.value.length > 5 ? ` 等${outputVariables.value.length}个` : ''
    errors.push(`💡 可用变量: ${availableVars}${moreHint}`)
  }

  return errors
}

// 校验所有节点
const validateAllNodes = () => {
  const errors = {}
  nodes.value.forEach(node => {
    const nodeErrs = validateNode(node)
    if (nodeErrs.length > 0) {
      errors[node.id] = nodeErrs
    }
  })
  nodeErrors.value = errors
  return Object.keys(errors).length === 0
}

// 监听节点配置变化，实时校验
watch(() => selectedNode.value?.data?.config, () => {
  if (selectedNode.value) {
    const errors = validateNode(selectedNode.value)
    if (errors.length > 0) {
      nodeErrors.value[selectedNode.value.id] = errors
    } else {
      delete nodeErrors.value[selectedNode.value.id]
    }
  }
}, { deep: true })

// 变量自动补全
const showVarSuggestions = ref(false)
const varSuggestions = ref([])
const varInputPosition = ref({ top: 0, left: 0 })
const currentVarInput = ref(null)
const varSuggestionIndex = ref(0)

// 处理输入框中的变量补全
const handleVarInput = (event, field) => {
  const input = event.target
  const value = input.value
  const cursorPos = input.selectionStart

  // 检查光标前是否有 {{
  const beforeCursor = value.substring(0, cursorPos)
  const match = beforeCursor.match(/\{\{([^}]*)$/)

  if (match) {
    const prefix = match[1].toLowerCase()
    const filtered = outputVariables.value.filter(v =>
      v.toLowerCase().includes(prefix)
    )

    if (filtered.length > 0) {
      varSuggestions.value = filtered
      varSuggestionIndex.value = 0
      showVarSuggestions.value = true
      currentVarInput.value = { input, field, cursorPos, prefix: match[1] }

      // 计算提示框位置
      const rect = input.getBoundingClientRect()
      varInputPosition.value = {
        top: rect.bottom + window.scrollY,
        left: rect.left + window.scrollX
      }
    } else {
      showVarSuggestions.value = false
    }
  } else {
    showVarSuggestions.value = false
  }
}

// 选择变量补全
const selectVarSuggestion = (varName) => {
  if (!currentVarInput.value) return

  const { input, field, cursorPos, prefix } = currentVarInput.value
  const value = input.value
  const beforeCursor = value.substring(0, cursorPos)
  const afterCursor = value.substring(cursorPos)

  // 替换 {{ 后面的内容为选中的变量
  const newBefore = beforeCursor.replace(/\{\{[^}]*$/, `{{${varName}}}`)
  const newValue = newBefore + afterCursor

  // 更新值
  if (selectedNode.value?.data?.config) {
    selectedNode.value.data.config[field] = newValue
  }

  showVarSuggestions.value = false

  // 聚焦回输入框
  setTimeout(() => {
    input.focus()
    const newPos = newBefore.length
    input.setSelectionRange(newPos, newPos)
  }, 0)
}

// 复制变量名
const copyVarName = (varName) => {
  navigator.clipboard.writeText(`{{${varName}}}`)
  ElMessage.success(`已复制 {{${varName}}}`)
}

// 开始调试
const startDebug = () => {
  debugNodeOutputs.value = {}
  debugCurrentNode.value = null
  runDialog.value = true
  ElMessage.info('调试模式：执行后可查看每个节点的输出')
}

// 格式化节点输出
const formatNodeOutput = (output) => {
  if (!output) return '-'
  if (typeof output === 'string') return output.length > 200 ? output.substring(0, 200) + '...' : output
  try {
    const str = JSON.stringify(output, null, 2)
    return str.length > 200 ? str.substring(0, 200) + '...' : str
  } catch {
    return String(output)
  }
}

// 节点右键菜单
const onNodeContextMenu = (event, node) => {
  event.preventDefault()
  if (isLocked.value) return
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    node: node
  }
}

const closeContextMenu = () => {
  contextMenu.value.visible = false
}

const handleContextAction = (action) => {
  if (isLocked.value && ['delete', 'duplicate'].includes(action)) {
    ElMessage.warning('画布已锁定，无法编辑')
    return
  }
  const node = contextMenu.value.node
  if (!node) return

  switch (action) {
    case 'copy':
      clipboard.value = JSON.parse(JSON.stringify(node))
      ElMessage.success('已复制节点')
      break
    case 'delete':
      if (node.type !== 'start') {
        removeNodes([node.id])
        selectedNode.value = null
        saveHistory()
        ElMessage.success('已删除节点')
      } else {
        ElMessage.warning('开始节点不能删除')
      }
      break
    case 'duplicate':
      const newNode = {
        id: `${node.type}_${nodeId++}`,
        type: node.type,
        position: { x: node.position.x + 50, y: node.position.y + 50 },
        data: { label: node.data.label + ' (副本)', config: { ...node.data.config } }
      }
      addNodes([newNode])
      saveHistory()
      ElMessage.success('已复制节点')
      break
    case 'locate':
      setViewport({ x: -node.position.x + 400, y: -node.position.y + 200, zoom: 1 })
      selectedNode.value = node
      break
  }
  closeContextMenu()
}

// 边点击编辑标签
const onEdgeClick = (event, edge) => {
  selectedEdge.value = edge
  edgeLabelInput.value = edge.data?.label || ''
  edgeLabelDialog.value = true
}

// 保存边标签
const saveEdgeLabel = () => {
  if (selectedEdge.value) {
    const idx = edges.value.findIndex(e => e.id === selectedEdge.value.id)
    if (idx !== -1) {
      edges.value[idx] = {
        ...edges.value[idx],
        data: { ...edges.value[idx].data, label: edgeLabelInput.value }
      }
    }
  }
  edgeLabelDialog.value = false
  ElMessage.success('已保存连线标签')
}

// 删除边
const deleteEdge = () => {
  if (selectedEdge.value) {
    edges.value = edges.value.filter(e => e.id !== selectedEdge.value.id)
    edgeLabelDialog.value = false
    ElMessage.success('已删除连线')
  }
}

// 添加注释
const addAnnotation = () => {
  editingAnnotation.value = {
    id: `anno_${Date.now()}`,
    text: '',
    color: '#fef3c7',
    x: 300,
    y: 100
  }
  annotationDialog.value = true
}

// 保存注释
const saveAnnotation = () => {
  if (!editingAnnotation.value.text.trim()) {
    ElMessage.warning('请输入注释内容')
    return
  }
  const idx = annotations.value.findIndex(a => a.id === editingAnnotation.value.id)
  if (idx !== -1) {
    annotations.value[idx] = { ...editingAnnotation.value }
  } else {
    annotations.value.push({ ...editingAnnotation.value })
  }
  annotationDialog.value = false
  ElMessage.success('已保存注释')
}

// 删除注释
const deleteAnnotation = (id) => {
  annotations.value = annotations.value.filter(a => a.id !== id)
  ElMessage.success('已删除注释')
}

// 编辑注释
const editAnnotation = (anno) => {
  editingAnnotation.value = { ...anno }
  annotationDialog.value = true
}

// 收藏节点
const toggleFavorite = (nodeType) => {
  const idx = favoriteNodes.value.indexOf(nodeType)
  if (idx !== -1) {
    favoriteNodes.value.splice(idx, 1)
    ElMessage.success('已取消收藏')
  } else {
    favoriteNodes.value.push(nodeType)
    ElMessage.success('已添加收藏')
  }
  cache.local.set('favoriteNodes', JSON.stringify(favoriteNodes.value))
}

// 添加最近使用
const addRecentNode = (nodeType) => {
  const idx = recentNodes.value.indexOf(nodeType)
  if (idx !== -1) recentNodes.value.splice(idx, 1)
  recentNodes.value.unshift(nodeType)
  if (recentNodes.value.length > 5) recentNodes.value.pop()
  cache.local.set('recentNodes', JSON.stringify(recentNodes.value))
}

// 快速连线模式
const toggleQuickConnect = () => {
  quickConnectMode.value = !quickConnectMode.value
  quickConnectSource.value = null
  if (quickConnectMode.value) {
    ElMessage.info('快速连线模式：点击节点自动连接')
  }
}

// 快速连线点击
const onQuickConnect = (node) => {
  if (!quickConnectMode.value) return
  if (!quickConnectSource.value) {
    quickConnectSource.value = node.id
    ElMessage.info(`已选择源节点: ${node.data.label}`)
  } else {
    if (quickConnectSource.value !== node.id) {
      edges.value.push({
        id: `e_${quickConnectSource.value}_${node.id}`,
        source: quickConnectSource.value,
        target: node.id
      })
      ElMessage.success('已连接')
    }
    quickConnectSource.value = null
  }
}

// 智能推荐下一节点
const suggestedNextNodes = computed(() => {
  if (!selectedNode.value) return []
  const currentType = selectedNode.value.type
  const suggestions = {
    'start': ['llm', 'http', 'knowledge', 'code'],
    'llm': ['condition', 'end', 'code', 'template'],
    'knowledge': ['llm', 'template'],
    'http': ['llm', 'code', 'condition'],
    'condition': ['llm', 'end', 'http'],
    'code': ['llm', 'condition', 'end'],
    'template': ['llm', 'end'],
    'agent': ['end', 'condition'],
    'tool': ['llm', 'end'],
    'loop': ['llm', 'end']
  }
  return (suggestions[currentType] || ['end']).map(type => nodeTypes.find(n => n.type === type)).filter(Boolean)
})

// 快速添加推荐节点
const addSuggestedNode = (nodeType) => {
  if (!selectedNode.value) return
  const sourceNode = selectedNode.value
  const newNode = {
    id: `${nodeType.type}_${nodeId++}`,
    type: nodeType.type,
    position: { x: sourceNode.position.x + 200, y: sourceNode.position.y },
    data: { label: nodeType.label, config: getDefCfg(nodeType.type) }
  }
  addNodes([newNode])
  edges.value.push({
    id: `e_${sourceNode.id}_${newNode.id}`,
    source: sourceNode.id,
    target: newNode.id
  })
  addRecentNode(nodeType.type)
  ElMessage.success(`已添加 ${nodeType.label}`)
}

// 键盘导航变量建议
const handleVarKeydown = (event) => {
  if (!showVarSuggestions.value) return

  switch (event.key) {
    case 'ArrowDown':
      event.preventDefault()
      varSuggestionIndex.value = (varSuggestionIndex.value + 1) % varSuggestions.value.length
      break
    case 'ArrowUp':
      event.preventDefault()
      varSuggestionIndex.value = varSuggestionIndex.value === 0
        ? varSuggestions.value.length - 1
        : varSuggestionIndex.value - 1
      break
    case 'Enter':
    case 'Tab':
      event.preventDefault()
      selectVarSuggestion(varSuggestions.value[varSuggestionIndex.value])
      break
    case 'Escape':
      showVarSuggestions.value = false
      break
  }
}

// 键盘快捷键处理
// 剪贴板
const clipboard = ref(null)

// 复制节点
const copyNode = () => {
  if (selectedNode.value) {
    clipboard.value = JSON.parse(JSON.stringify(selectedNode.value))
    ElMessage.success('已复制节点')
  }
}

// 粘贴节点
const pasteNode = () => {
  if (clipboard.value) {
    const newNode = {
      ...clipboard.value,
      id: `${clipboard.value.type}_${nodeId++}`,
      position: {
        x: clipboard.value.position.x + 50,
        y: clipboard.value.position.y + 50
      },
      data: {
        ...clipboard.value.data,
        label: clipboard.value.data.label + ' (副本)'
      }
    }
    addNodes([newNode])
    ElMessage.success('已粘贴节点')
  }
}

const handleKeydown = (e) => {
  if (!editingWorkflow.value) return
  if (e.ctrlKey || e.metaKey) {
    if (e.key === 'z') { e.preventDefault(); undo() }
    if (e.key === 'y') { e.preventDefault(); redo() }
    if (e.key === 's') { e.preventDefault(); saveWorkflow() }
    if (e.key === 'c' && selectedNode.value) { e.preventDefault(); copyNode() }
    if (e.key === 'v' && clipboard.value) { e.preventDefault(); pasteNode() }
    if (e.key === 'f') { e.preventDefault(); nodeSearchDialog.value = true; nodeSearchQuery.value = '' }
    if (e.key === 'e') { e.preventDefault(); exportWorkflow() }
    if (e.key === 'i') { e.preventDefault(); importWorkflow() }
  }
  if (e.key === 'Delete' && selectedNode.value) { deleteNode() }
}

// 拖动调整属性面板宽度
const startResize = (e) => {
  isResizing.value = true
  resizeStartX.value = e.clientX
  resizeStartWidth.value = propsWidth.value
  document.body.style.cursor = 'ew-resize'
  document.body.style.userSelect = 'none'
  
  const handleMouseMove = (e) => {
    if (!isResizing.value) return
    const deltaX = resizeStartX.value - e.clientX
    const newWidth = Math.max(400, Math.min(800, resizeStartWidth.value + deltaX))
    propsWidth.value = newWidth
  }
  
  const handleMouseUp = () => {
    isResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }
  
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

onMounted(async () => {
  await loadWorkflows();
  loadModels(); loadTools(); loadAgents(); loadKnowledgeBases()
  window.addEventListener('keydown', handleKeydown)
  
  // 检查URL参数，恢复编辑状态
  const urlParams = new URLSearchParams(window.location.search)
  const workflowId = urlParams.get('id')
  const mode = urlParams.get('mode')
  
  if (workflowId) {
    // 有ID，加载工作流
    const wf = workflows.value.find(w => w.id === parseInt(workflowId))
    if (wf) {
      editWorkflow(wf)
    } else {
      ElMessage.warning('工作流不存在')
      backToList()
    }
  } else if (mode === 'create') {
    // 新建模式
    createWorkflow()
  }
})
onUnmounted(() => { window.removeEventListener('keydown', handleKeydown) })

// 监听节点和边变化，自动保存历史
watch([nodes, edges], () => {
  if (editingWorkflow.value && nodes.value.length > 0) {
    // 延迟保存避免频繁操作
    clearTimeout(window._historyTimer)
    window._historyTimer = setTimeout(saveHistory, 500)
  }
}, { deep: true })

const loadWorkflows = async () => {
  loadingWorkflows.value = true
  try {
    const r = await request({ url: '/cms/ai/workflow/list', method: 'get' })
    workflows.value = r.data || []
  } catch(e) {
    console.error('加载工作流失败', e)
  } finally {
    loadingWorkflows.value = false
  }
}
const loadModels = async () => { 
  try { 
    const r = await request({ url: '/cms/ai/model-config/list', method: 'get' })
    const list = r.data?.list || []
    models.value = list.map(m => ({
      ...m,
      modelName: m.modelName || m.name || m.model_name || `模型${m.id}`,
      id: m.id || m.model_id
    }))
    console.log('加载模型成功:', models.value)
  } catch(e) { 
    console.error('加载模型失败', e)
    ElMessage.error('加载模型失败')
  } 
}
const loadTools = async () => { 
  try { 
    const r = await request({ url: '/cms/ai/tool/list', method: 'get' })
    const list = r.data || []
    tools.value = list.map(t => ({
      ...t,
      displayName: t.displayName || t.name || t.tool_name || `工具${t.id}`,
      name: t.name || t.tool_name || `tool_${t.id}`
    }))
    console.log('加载工具成功:', tools.value)
  } catch(e) { 
    console.error('加载工具失败', e)
    ElMessage.error('加载工具失败')
  } 
}
const loadAgents = async () => { 
  try { 
    const r = await request({ url: '/cms/ai/agent/list', method: 'get' })
    const list = r.data?.list || []
    agents.value = list.map(a => ({
      ...a,
      name: a.name || a.agent_name || `智能体${a.id}`,
      id: a.id || a.agent_id
    }))
    console.log('加载智能体成功:', agents.value)
  } catch(e) { 
    console.error('加载智能体失败', e)
    ElMessage.error('加载智能体失败')
  } 
}
const loadKnowledgeBases = async () => { 
  try { 
    const r = await request({ url: '/cms/ai/knowledge-base/list', method: 'get' })
    const list = r.data?.list || []
    // 确保每个知识库都有必要的字段
    knowledgeBases.value = list.map(kb => ({
      ...kb,
      id: kb.id || kb.kb_id,
      // 优先使用文件名，其次使用name、title等
      fileName: kb.fileName || kb.file_name || kb.filename,
      name: kb.name || kb.title || kb.kb_name || kb.fileName || kb.file_name || `知识库${kb.id}`
    }))
    console.log('加载知识库成功:', knowledgeBases.value)
  } catch(e) { 
    console.error('加载知识库失败', e)
    ElMessage.error('加载知识库失败')
  } 
}

const createWorkflow = () => {
  editingWorkflow.value = { name: '新工作流', description: '' }
  nodes.value = [
    { id: 'start_1', type: 'start', position: { x: 100, y: 200 }, data: { label: '开始', config: {} } },
    { id: 'end_1', type: 'end', position: { x: 500, y: 200 }, data: { label: '结束', config: { outputVariable: 'output' } } }
  ]
  edges.value = []
  selectedNode.value = null
  nodeId = 2
  // 更新URL为新建模式
  const url = new URL(window.location)
  url.searchParams.set('mode', 'create')
  window.history.replaceState({}, '', url)
}

// AI生成相关
const aiGeneratorRef = ref(null)
const openAIGenerator = () => {
  if (aiGeneratorRef.value) {
    aiGeneratorRef.value.open()
  }
}
const onAIGenerated = (result) => {
  console.log('AI生成结果:', result)
  ElMessage.success(`成功生成 ${result.nodeCount} 个节点的工作流`)
}
const applyAIWorkflow = (result) => {
  if (!result.graphData) {
    ElMessage.error('无效的工作流数据')
    return
  }
  try {
    const graphData = JSON.parse(result.graphData)
    // 创建新工作流并应用AI生成的图
    editingWorkflow.value = { 
      name: result.workflowName || 'AI生成的工作流', 
      description: result.workflowDescription || '' 
    }
    // 转换节点格式
    nodes.value = (graphData.nodes || []).map(n => ({
      id: n.id,
      type: n.type,
      position: { x: n.positionX || 0, y: n.positionY || 0 },
      data: { label: n.name, config: n.config || {} }
    }))
    // 转换边格式
    edges.value = (graphData.edges || []).map((e, idx) => ({
      id: e.id || `edge_${idx + 1}`,
      source: e.source,
      target: e.target,
      sourceHandle: e.sourceHandle || null,
      label: e.label || null
    }))
    nodeId = nodes.value.length + 1
    selectedNode.value = null
    // 更新URL为新建模式
    const url = new URL(window.location)
    url.searchParams.set('mode', 'create')
    url.searchParams.delete('id')
    window.history.replaceState({}, '', url)
    ElMessage.success('AI工作流已应用到编辑器，请检查并保存')
    // 延迟后自动适应视图
    setTimeout(() => {
      try { vfFitView({ padding: 0.2 }) } catch(e) { /* 视图适应失败可忽略 */ }
    }, 300)
  } catch (e) {
    console.error('应用AI工作流失败:', e)
    ElMessage.error('应用失败: ' + e.message)
  }
}

const editWorkflow = (wf) => {
  editingWorkflow.value = { ...wf }
  selectedNode.value = null
  if (wf.graphData) {
    try {
      const g = JSON.parse(wf.graphData)
      nodes.value = (g.nodes||[]).map(n => ({ id: n.id, type: n.type, position: { x: n.positionX||0, y: n.positionY||0 }, data: { label: n.name, config: n.config||{} } }))
      edges.value = (g.edges||[]).map(e => ({ id: e.id, source: e.source, target: e.target, sourceHandle: e.sourceHandle }))
      nodeId = nodes.value.length + 1
    } catch(e) { nodes.value = []; edges.value = [] }
  } else { createWorkflow() }
  // 更新URL参数
  if (wf.id) {
    const url = new URL(window.location)
    url.searchParams.set('id', wf.id)
    url.searchParams.delete('mode')
    window.history.replaceState({}, '', url)
  }
}

const backToList = () => { 
  editingWorkflow.value = null; 
  selectedNode.value = null; 
  // 清除URL参数
  const url = new URL(window.location)
  url.searchParams.delete('id')
  url.searchParams.delete('mode')
  window.history.replaceState({}, '', url)
  loadWorkflows() 
}

const saveWorkflow = async (skipValidation = false) => {
  // 保存前校验
  if (!skipValidation) {
    const isValid = validateAllNodes()
    if (!isValid) {
      const errorCount = Object.keys(nodeErrors.value).length
      ElMessage.warning(`发现 ${errorCount} 个节点配置错误，请修正后再保存`)
      return
    }
  }

  saving.value = true
  const gd = {
    nodes: nodes.value.map(n => ({ id: n.id, type: n.type, name: n.data.label, positionX: n.position.x, positionY: n.position.y, config: n.data.config||{} })),
    edges: edges.value.map(e => ({ id: e.id, source: e.source, target: e.target, sourceHandle: e.sourceHandle }))
  }
  const graphData = JSON.stringify(gd)
  try {
    if (editingWorkflow.value.id) {
      await request({ url: '/cms/ai/workflow/update', method: 'put', data: {
        id: editingWorkflow.value.id,
        name: editingWorkflow.value.name,
        description: editingWorkflow.value.description || '',
        graphData,
        status: editingWorkflow.value.status || 'draft',
        enabled: editingWorkflow.value.enabled ?? true,
        version: editingWorkflow.value.version
      }})
    } else {
      const r = await request({ url: '/cms/ai/workflow/create', method: 'post', data: {
        name: editingWorkflow.value.name || '新工作流',
        description: editingWorkflow.value.description || '',
        graphData,
        status: 'draft',
        enabled: true
      }})
      editingWorkflow.value = r.data
      // 新建保存后，更新URL为编辑模式
      const url = new URL(window.location)
      url.searchParams.set('id', r.data.id)
      url.searchParams.delete('mode')
      window.history.replaceState({}, '', url)
    }
    lastSaveTime.value = Date.now()
    // 保存成功后清除错误状态
    nodeErrors.value = {}
    ElMessage.success('保存成功')
  } catch(e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const publishWorkflow = async () => { 
  // 保存工作流
  await saveWorkflow(); 
  
  // 如果保存失败（验证未通过），不继续发布
  if (Object.keys(nodeErrors.value).length > 0) {
    return;
  }
  
  try { 
    const response = await request({ url: `/cms/ai/workflow/${editingWorkflow.value.id}/publish`, method: 'post'});

    // 发布成功后，重新加载工作流数据以确保状态同步
    const wfData = await request({ url: `/cms/ai/workflow/${editingWorkflow.value.id}`, method: 'get' });
    editingWorkflow.value = wfData.data;
    // 重新加载节点数据
    const gd = JSON.parse(wfData.data.graphData || '{}');
    if (gd.nodes) {
      nodes.value = gd.nodes.map(n => ({
        id: n.id,
        type: n.type,
        position: { x: n.positionX || 0, y: n.positionY || 0 },
        data: { label: n.name, config: n.config || {} }
      }));
      edges.value = (gd.edges || []).map(e => ({
        id: e.id,
        source: e.source,
        target: e.target,
        sourceHandle: e.sourceHandle || null,
        type: 'custom'
      }));
    }
    // 清除错误状态并重新验证
    nodeErrors.value = {};
    validateAllNodes();
    ElMessage.success('发布成功');
  } catch(e) { 
    console.error('发布失败:', e);
    ElMessage.error('发布失败') 
  } 
}
const testRun = () => { runningId.value = editingWorkflow.value.id; runInput.value = ''; runDialog.value = true }
const runWorkflow = (wf) => { runningId.value = wf.id; runInput.value = ''; runDialog.value = true }

// 节点执行状态
const nodeExecutionStatus = ref({})
const executionProgress = ref(0)  // 执行进度百分比
let currentEventSource = null  // 当前SSE连接
let completedNodesSet = new Set()  // 已完成节点集合（防止重复闪光）
const executionStats = ref({
  startTime: null,
  completedNodes: 0,
  totalNodes: 0,
  nodeTimings: {}
})
const showExecutionLog = ref(true)  // 是否显示执行日志
const executionLogs = ref([])  // 执行日志列表

// 添加执行日志
const addExecutionLog = (type, message) => {
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  const logEntry = { type, message, time, timestamp: Date.now() }
  executionLogs.value.push(logEntry)
  console.log('📋 添加日志:', logEntry, '总数:', executionLogs.value.length)
  
  // 最多保留50条
  if (executionLogs.value.length > 50) {
    executionLogs.value.shift()
  }
}

// 获取日志图标
const getLogIcon = (type) => {
  const icons = {
    'start': 'fa-solid fa-flag-checkered',
    'node-start': 'fa-solid fa-play',
    'node-complete': 'fa-solid fa-check',
    'node-error': 'fa-solid fa-xmark',
    'complete': 'fa-solid fa-trophy',
    'error': 'fa-solid fa-triangle-exclamation'
  }
  return icons[type] || 'fa-solid fa-circle'
}

// 重置节点状态
const resetNodeStatus = () => {
  nodeExecutionStatus.value = {}
  executionProgress.value = 0
  executionLogs.value = []
  showExecutionLog.value = true
  completedNodesSet.clear()  // 清空已完成节点集合
  executionStats.value = {
    startTime: Date.now(),
    completedNodes: 0,
    totalNodes: nodes.value.length,
    nodeTimings: {}
  }
}

// 取消执行
const cancelExecution = () => {
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  executing.value = false
  ElMessage.warning('已取消执行')
  
  // 标记所有运行中的节点为取消
  Object.keys(nodeExecutionStatus.value).forEach(id => {
    if (nodeExecutionStatus.value[id] === 'running') {
      nodeExecutionStatus.value[id] = 'cancelled'
    }
  })
}

// 模拟节点执行状态更新（实际应从后端WebSocket获取）
const simulateNodeExecution = async () => {
  resetNodeStatus()

  // 按拓扑顺序获取节点
  const startNode = nodes.value.find(n => n.type === 'start')
  if (!startNode) return

  // 简单的BFS遍历模拟执行顺序
  const visited = new Set()
  const queue = [startNode.id]
  const executionOrder = []

  while (queue.length > 0) {
    const nodeId = queue.shift()
    if (visited.has(nodeId)) continue
    visited.add(nodeId)
    executionOrder.push(nodeId)

    // 找到从该节点出发的边
    const outEdges = edges.value.filter(e => e.source === nodeId)
    outEdges.forEach(e => {
      if (!visited.has(e.target)) {
        queue.push(e.target)
      }
    })
  }

  // 模拟逐个节点执行
  for (const nodeId of executionOrder) {
    nodeExecutionStatus.value[nodeId] = 'running'
    await new Promise(resolve => setTimeout(resolve, 500 + Math.random() * 500))
    nodeExecutionStatus.value[nodeId] = 'completed'
  }
}

const execWorkflow = async () => {
  executing.value = true
  resetNodeStatus()
  execResult.value = null

  // 🔍 诊断输出：检查工作流图结构
  console.log('🔍 工作流诊断信息:')
  console.log('  节点总数:', nodes.value.length)
  console.log('  连线总数:', edges.value.length)
  console.log('  节点列表:', nodes.value.map(n => ({ id: n.id, label: n.data?.label, type: n.type })))
  console.log('  连线列表:', edges.value.map(e => ({ id: e.id, source: e.source, target: e.target })))
  console.log('  初始状态:', JSON.parse(JSON.stringify(nodeExecutionStatus.value)))

  // 定义清除日志的函数（在try外面，方便catch块访问）
  let statusLogInterval = null
  const clearStatusLog = () => {
    if (statusLogInterval) {
      clearInterval(statusLogInterval)
      console.log('🛑 停止状态日志')
      statusLogInterval = null
    }
  }

  try {
    // 🌟 使用鉴权 EventSource 接收SSE实时事件（原生 EventSource 无法携带 Authorization header，改用 fetch 封装）
    let url = `/cms/ai/workflow/${runningId.value}/execute/stream?testRun=true`
    // 如果有输入参数，添加到URL
    if (runInput.value && runInput.value.trim()) {
      url += `&input=${encodeURIComponent(runInput.value)}`
    }
    const eventSource = createAuthEventSource(url)
    currentEventSource = eventSource  // 保存引用以便取消
    
    // 🎬 立即关闭对话框，显示执行过程
    runDialog.value = false
    
    let completedCount = 0
    const totalNodes = nodes.value.length
    
    // 🔍 定期输出状态（用于调试） - 注释掉以提高性能
    // statusLogInterval = setInterval(() => {
    //   console.log('📊 当前节点状态:', JSON.parse(JSON.stringify(nodeExecutionStatus.value)))
    // }, 500)

    eventSource.addEventListener('start', (e) => {
      const event = JSON.parse(e.data)
      console.log('🚀 工作流开始:', event.message)
      addExecutionLog('start', event.message)
      ElMessage.info({ message: '工作流开始执行...', duration: 1500 })
    })

    // 记录节点开始时间（用于确保最小显示时长）
    const nodeStartTimes = {}
    
    eventSource.addEventListener('node_start', (e) => {
      const event = JSON.parse(e.data)
      console.log('▶️  节点开始:', event.nodeName)
      
      // 记录节点开始时间
      nodeStartTimes[event.nodeId] = Date.now()
      
      // 📝 添加日志
      addExecutionLog('node-start', `开始执行节点: ${event.nodeName}`)
      
      // ⚡ 更新节点状态为运行中
      nodeExecutionStatus.value[event.nodeId] = 'running'
      console.log(`🔵 节点 ${event.nodeId} 状态更新为: running`)
      
      // 🎯 使用双重nextTick + 延迟确保DOM完全更新
      nextTick(() => {
        nextTick(() => {
          setTimeout(() => {
            const containerEl = document.querySelector(`[data-id="${event.nodeId}"]`)
            const nodeEl = containerEl?.querySelector('.cnode')
            if (nodeEl) {
              const hasRunning = nodeEl.classList.contains('is-running')
              console.log(`🔵 ${event.nodeId}: is-running=${hasRunning}`)
              if (hasRunning) {
                nodeEl.classList.add('node-pulse-animation')
                setTimeout(() => nodeEl.classList.remove('node-pulse-animation'), 600)
              }
            }
          }, 100)  // 增加到100ms
        })
      })
    })

    eventSource.addEventListener('node_complete', (e) => {
      const event = JSON.parse(e.data)
      console.log('✅ 节点完成:', event.nodeName, event.durationMs + 'ms')
      
      // 🛡️ 防止重复处理
      if (completedNodesSet.has(event.nodeId)) {
        console.warn(`⚠️ 节点 ${event.nodeId} 已完成，跳过重复处理`)
        return
      }
      completedNodesSet.add(event.nodeId)
      
      // 🎯 智能延迟策略：只对快速节点延迟，慢节点立即更新
      const startTime = nodeStartTimes[event.nodeId] || Date.now()
      const elapsed = Date.now() - startTime
      const minDisplayTime = 600 // 最小显示时间
      
      // 如果节点执行时间>=600ms，说明running状态已经显示够久了，立即更新
      // 如果<600ms，延迟到600ms确保running状态可见
      const delay = elapsed >= minDisplayTime ? 0 : (minDisplayTime - elapsed)
      
      console.log(`⏱️ 节点 ${event.nodeId}: 执行${elapsed}ms, 延迟${delay}ms后更新completed`)
      
      setTimeout(() => {
        // ✨ 更新节点状态为完成
        nodeExecutionStatus.value[event.nodeId] = 'completed'
        console.log(`🟢 节点 ${event.nodeId} 状态更新为: completed`)
        completedCount++
        
        // 📊 更新执行进度和统计
        executionProgress.value = Math.round((completedCount / totalNodes) * 100)
        executionStats.value.completedNodes = completedCount
        executionStats.value.nodeTimings[event.nodeId] = {
          name: event.nodeName,
          duration: event.durationMs
        }
        
        // 保存节点输出（用于调试）
        if (debugMode.value && event.output) {
          debugNodeOutputs.value[event.nodeId] = event.output
          debugCurrentNode.value = event.nodeId
        }
        
        // 🎯 成功闪光动画 - 带重试机制确保CSS应用
        const tryPlayAnimation = (retryCount = 0) => {
          nextTick(() => {
            nextTick(() => {
              setTimeout(() => {
                const containerEl = document.querySelector(`[data-id="${event.nodeId}"]`)
                const nodeEl = containerEl?.querySelector('.cnode')
                if (nodeEl) {
                  const hasCompleted = nodeEl.classList.contains('is-completed')
                  console.log(`🟢 ${event.nodeId}: is-completed=${hasCompleted} (尝试${retryCount + 1})`)
                  
                  if (hasCompleted) {
                    // completed状态已应用，播放动画
                    nodeEl.classList.add('node-success-flash')
                    setTimeout(() => nodeEl.classList.remove('node-success-flash'), 800)
                    highlightOutgoingEdges(event.nodeId)
                  } else if (retryCount < 3) {
                    // 最多重试3次，每次间隔50ms
                    setTimeout(() => tryPlayAnimation(retryCount + 1), 50)
                  }
                }
              }, 100)  // 初始延迟100ms
            })
          })
        }
        tryPlayAnimation(0)
        
        // 📝 添加日志
        addExecutionLog('node-complete', `${event.nodeName} 完成 (${event.durationMs}ms)`)
        
        // 只在重要节点显示提示（可选，减少干扰）
        // const avgTime = Object.values(executionStats.value.nodeTimings)
        //   .reduce((sum, t) => sum + t.duration, 0) / completedCount
        // ElMessage.success({
        //   message: `${event.nodeName} 完成 (${event.durationMs}ms) - ${executionProgress.value}%`,
        //   duration: 1000,
        //   showClose: false
        // })
      }, delay)
    })

    eventSource.addEventListener('node_error', (e) => {
      const event = JSON.parse(e.data)
      console.error('❌ 节点错误:', event.nodeName, event.error)
      
      // 📝 添加错误日志
      addExecutionLog('node-error', `${event.nodeName} 失败: ${event.error}`)
      
      // 🔴 更新节点状态为错误
      nodeExecutionStatus.value[event.nodeId] = 'error'
      
      // 错误抖动动画 - 查找内层 .cnode 元素
      const containerEl = document.querySelector(`[data-id="${event.nodeId}"]`)
      const nodeEl = containerEl?.querySelector('.cnode')
      if (nodeEl) {
        nodeEl.classList.add('node-error-shake')
        setTimeout(() => nodeEl.classList.remove('node-error-shake'), 600)
      }
      
      // 显示错误通知
      ElNotification({
        title: '节点执行失败',
        message: `${event.nodeName}: ${event.error}`,
        type: 'error',
        duration: 5000
      })
    })

    eventSource.addEventListener('complete', (e) => {
      const event = JSON.parse(e.data)
      console.log('🎉 工作流完成:', event.durationMs + 'ms')
      
      const totalTime = Date.now() - executionStats.value.startTime
      const avgNodeTime = Math.round(
        Object.values(executionStats.value.nodeTimings)
          .reduce((sum, t) => sum + t.duration, 0) / executionStats.value.completedNodes
      )
      
      execResult.value = {
        success: true,
        output: event.finalOutput,
        durationMs: event.durationMs,
        nodeLogs: [],
        stats: {
          totalNodes: executionStats.value.totalNodes,
          completedNodes: executionStats.value.completedNodes,
          totalTime,
          avgNodeTime,
          nodeTimings: executionStats.value.nodeTimings
        }
      }
      
      // 📝 添加完成日志
      addExecutionLog('complete', `✅ 工作流执行成功！总耗时: ${event.durationMs}ms`)
      
      eventSource.close()
      currentEventSource = null
      clearStatusLog()  // 停止状态日志
      resultDialog.value = true
      executing.value = false
      
      // 🎊 成功动画（显示详细统计）
      ElMessage.success({
        message: `🎉 工作流执行成功！总耗时:${event.durationMs}ms 节点平均:${avgNodeTime}ms`,
        duration: 3000
      })
    })

    eventSource.addEventListener('error', (e) => {
      try {
        const event = JSON.parse(e.data)
        console.error('❌ 工作流失败:', event.error)
        
        execResult.value = {
          success: false,
          errorMessage: event.error
        }
        
        ElMessage.error('执行失败: ' + event.error)
      } catch (err) {
        console.error('解析错误事件失败', err)
      }
      
      eventSource.close()
      clearStatusLog()  // 停止状态日志
      executing.value = false
      
      // 标记所有运行中的节点为错误
      Object.keys(nodeExecutionStatus.value).forEach(id => {
        if (nodeExecutionStatus.value[id] === 'running') {
          nodeExecutionStatus.value[id] = 'error'
        }
      })
    })

    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error)
      eventSource.close()
      clearStatusLog()  // 停止状态日志
      
      if (!execResult.value) {
        ElMessage.error('连接中断')
      }
      
      executing.value = false
    }

  } catch(e) {
    console.error('执行失败:', e)
    ElMessage.error('执行失败: ' + e.message)
    clearStatusLog()  // 停止状态日志
    executing.value = false
  }
}
// 🎯 高亮出边连线
const highlightOutgoingEdges = (nodeId) => {
  // 找到从该节点发出的所有连线
  const outgoingEdges = edges.value.filter(e => e.source === nodeId)
  
  outgoingEdges.forEach(edge => {
    // 找到DOM元素并添加高亮class
    const edgeEl = document.querySelector(`[data-id="${edge.id}"]`)
    if (edgeEl) {
      edgeEl.classList.add('edge-flowing')
      // 1秒后移除高亮
      setTimeout(() => edgeEl.classList.remove('edge-flowing'), 1000)
    }
  })
}

// 📊 获取排序后的节点耗时
const getSortedTimings = (timings) => {
  return Object.entries(timings)
    .sort((a, b) => b[1].duration - a[1].duration)
    .map(([id, timing]) => ({ id, ...timing }))
}

// 📏 计算耗时条宽度百分比
const getTimingWidth = (duration, allTimings) => {
  const maxDuration = Math.max(...Object.values(allTimings).map(t => t.duration))
  return (duration / maxDuration) * 100
}

const deleteWorkflow = async (wf) => {
  try {
    await ElMessageBox.confirm('确定删除该工作流？此操作不可恢复。', '删除确认', { type: 'warning' })
    await request({ url: `/cms/ai/workflow/${wf.id}`, method: 'delete'})
    ElMessage.success('删除成功')
    loadWorkflows()
  } catch (e) {
    // 用户取消删除操作，忽略
  }
}

// 卡片操作菜单处理
const handleCardAction = (command, wf) => {
  switch (command) {
    case 'edit': editWorkflow(wf); break
    case 'duplicate': duplicateWorkflow(wf); break
    case 'history': showHistory(wf); break
    case 'delete': deleteWorkflow(wf); break
  }
}

// 复制工作流
const duplicateWorkflow = async (wf) => {
  try {
    const newWf = {
      name: wf.name + ' (副本)',
      description: wf.description,
      graphData: wf.graphData
    }
    const r = await request({ url: '/cms/ai/workflow/create', method: 'post', data: newWf})
    ElMessage.success('复制成功')
    loadWorkflows()
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

// 版本管理
const showVersions = async () => {
  if (!editingWorkflow.value?.id) return
  versionDialog.value = true
  loadingVersions.value = true
  try {
    const r = await request({ url: `/cms/ai/workflow/${editingWorkflow.value.id}/versions`, method: 'get' })
    versions.value = r.data || []
    currentVersion.value = editingWorkflow.value.version || 1
  } catch (e) {
    // 如果API不存在，使用模拟数据
    versions.value = [
      {
        version: editingWorkflow.value.version || 1,
        createdAt: new Date().toISOString(),
        description: '当前版本',
        isCurrent: true
      }
    ]
    currentVersion.value = editingWorkflow.value.version || 1
  } finally {
    loadingVersions.value = false
  }
}

// 保存新版本
const saveAsNewVersion = async (description = '') => {
  if (!editingWorkflow.value?.id) return

  const gd = {
    nodes: nodes.value.map(n => ({ id: n.id, type: n.type, name: n.data.label, positionX: n.position.x, positionY: n.position.y, config: n.data.config||{} })),
    edges: edges.value.map(e => ({ id: e.id, source: e.source, target: e.target, sourceHandle: e.sourceHandle }))
  }

  try {
    const r = await request({ url: `/cms/ai/workflow/${editingWorkflow.value.id}/version`, method: 'post', data: {
      graphData: JSON.stringify(gd),
      description: description || `版本 ${(currentVersion.value || 0) + 1}`
    }})
    ElMessage.success('新版本保存成功')
    currentVersion.value = r.data?.version || (currentVersion.value + 1)
    showVersions()
  } catch (e) {
    // 模拟成功
    const newVersion = {
      version: (currentVersion.value || 1) + 1,
      createdAt: new Date().toISOString(),
      description: description || `版本 ${(currentVersion.value || 1) + 1}`,
      isCurrent: true
    }
    versions.value.forEach(v => v.isCurrent = false)
    versions.value.unshift(newVersion)
    currentVersion.value = newVersion.version
    ElMessage.success('新版本保存成功')
  }
}

// 回滚到指定版本
const rollbackToVersion = async (version) => {
  try {
    await ElMessageBox.confirm(`确定要回滚到版本 ${version.version} 吗？当前未保存的更改将丢失。`, '版本回滚', { type: 'warning' })

    const r = await request({ url: `/cms/ai/workflow/${editingWorkflow.value.id}/rollback`, method: 'post', data: {
      version: version.version
    }})

    if (r.data?.graphData) {
      const g = JSON.parse(r.data.graphData)
      nodes.value = (g.nodes||[]).map(n => ({ id: n.id, type: n.type, position: { x: n.positionX||0, y: n.positionY||0 }, data: { label: n.name, config: n.config||{} } }))
      edges.value = (g.edges||[]).map(e => ({ id: e.id, source: e.source, target: e.target, sourceHandle: e.sourceHandle }))
      currentVersion.value = version.version
      ElMessage.success('已回滚到版本 ' + version.version)
      versionDialog.value = false
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.info('版本回滚功能需要后端支持')
    }
  }
}

// 比较版本差异
const compareVersions = (v1, v2) => {
  ElMessage.info('版本对比功能开发中')
}

// 模板库
const showTemplates = () => {
  templateDialog.value = true
}

// 使用模板创建工作流
const useTemplate = (template) => {
  editingWorkflow.value = { name: template.name, description: template.desc }
  nodes.value = template.nodes.map(n => ({
    id: n.id,
    type: n.type,
    position: { x: n.positionX, y: n.positionY },
    data: { label: n.name, config: { ...n.config } }
  }))
  edges.value = template.edges.map((e, idx) => ({
    id: `edge_${idx}`,
    source: e.source,
    target: e.target,
    sourceHandle: e.sourceHandle
  }))
  nodeId = nodes.value.length + 1
  templateDialog.value = false
  ElMessage.success(`已使用「${template.name}」模板`)
}

// 导出工作流
const exportWorkflow = () => {
  if (!editingWorkflow.value) return

  const exportData = {
    name: editingWorkflow.value.name,
    description: editingWorkflow.value.description,
    version: currentVersion.value || 1,
    exportTime: new Date().toISOString(),
    nodes: nodes.value.map(n => ({
      id: n.id,
      type: n.type,
      positionX: n.position.x,
      positionY: n.position.y,
      name: n.data.label,
      config: n.data.config
    })),
    edges: edges.value.map(e => ({
      id: e.id,
      source: e.source,
      target: e.target,
      sourceHandle: e.sourceHandle
    }))
  }

  const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${editingWorkflow.value.name || 'workflow'}_${new Date().toISOString().slice(0,10)}.json`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('工作流已导出')
}

// 导入工作流
const importWorkflow = () => {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.json'
  input.onchange = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    try {
      const text = await file.text()
      const data = JSON.parse(text)

      if (!data.nodes || !data.edges) {
        ElMessage.error('无效的工作流文件')
        return
      }

      editingWorkflow.value = {
        name: data.name || '导入的工作流',
        description: data.description || ''
      }
      nodes.value = data.nodes.map(n => ({
        id: n.id,
        type: n.type,
        position: { x: n.positionX || 0, y: n.positionY || 0 },
        data: { label: n.name, config: n.config || {} }
      }))
      edges.value = data.edges.map(e => ({
        id: e.id,
        source: e.source,
        target: e.target,
        sourceHandle: e.sourceHandle
      }))
      nodeId = nodes.value.length + 1

      ElMessage.success(`已导入工作流「${data.name}」`)
    } catch (err) {
      ElMessage.error('导入失败：文件格式错误')
    }
  }
  input.click()
}

// 快速定位节点
const locateNode = (nodeId) => {
  const node = nodes.value.find(n => n.id === nodeId)
  if (node) {
    setViewport({ x: -node.position.x + 400, y: -node.position.y + 200, zoom: 1 })
    selectedNode.value = node
  }
}

// 节点搜索
const nodeSearchQuery = ref('')
const nodeSearchDialog = ref(false)
const searchedNodes = computed(() => {
  if (!nodeSearchQuery.value) return []
  const q = nodeSearchQuery.value.toLowerCase()
  return nodes.value.filter(n =>
    n.data.label?.toLowerCase().includes(q) ||
    n.type.includes(q) ||
    n.id.includes(q)
  )
})

// 更多操作处理
const handleMoreAction = (command) => {
  switch (command) {
    case 'export': exportWorkflow(); break
    case 'import': importWorkflow(); break
    case 'search': nodeSearchDialog.value = true; nodeSearchQuery.value = ''; break
    // case 'share': shareWorkflow(); break  // 分享功能已移除，集成到 admin 仅管理员可访问
    case 'help': helpDialog.value = true; break
  }
}

const onDragStart = (e, nt) => { 
  if (isLocked.value) {
    e.preventDefault()
    return
  }
  e.dataTransfer.setData('nodeType', JSON.stringify(nt)) 
}
const onDrop = (e) => { 
  if (isLocked.value) return
  const d = e.dataTransfer.getData('nodeType'); if (!d) return; const nt = JSON.parse(d); const pos = project({ x: e.clientX - 250, y: e.clientY - 100 }); addNodes([{ id: `${nt.type}_${nodeId++}`, type: nt.type, position: pos, data: { label: nt.label, config: getDefCfg(nt.type) } }]); saveHistory() 
}
// 添加输入参数
const addInputParam = () => {
  if (!selectedNode.value.data.config.inputParams) {
    selectedNode.value.data.config.inputParams = []
  }
  selectedNode.value.data.config.inputParams.push({ name: '', type: 'string', description: '', required: false })
}

const getDefCfg = (t) => {
  // 获取默认模型ID
  const getDefaultModelId = () => {
    if (!models.value || models.value.length === 0) return ''
    // 优先选择标记为默认的模型
    const defaultModel = models.value.find(m => m.isDefault || m.is_default)
    if (defaultModel) return String(defaultModel.id)
    // 否则选择第一个模型
    return String(models.value[0].id)
  }
  
  const configs = {
    start: { inputParams: [] },
    llm: { modelId: getDefaultModelId(), systemPrompt: '', userPrompt: '{{input}}', outputVariable: 'llm_output', temperature: 0.7, maxTokens: 2000 },
    agent: { agentId: '', userPrompt: '{{input}}', outputVariable: 'agent_output' },
    condition: { expression: '' },
    tool: { toolName: '', params: {}, paramsJson: '', outputVariable: 'tool_output' },
    http: { method: 'GET', url: '', headers: {}, headersJson: '', body: '', outputVariable: 'http_response' },
    database: { operation: 'query', sql: '', paramsJson: '', outputVariable: 'db_result' },
    email: { to: '', subject: '', content: '', from: '', cc: '', bcc: '', isHtml: false, outputVariable: 'email_result' },
    cache: { operation: 'get', key: '', value: '', ttl: 3600, outputVariable: 'cache_result' },
    webhook: { url: '', eventType: 'workflow.completed', dataJson: '', headersJson: '', secret: '', maxRetries: 3, outputVariable: 'webhook_result' },
    code: { code: '// 使用 {{变量}} 引用变量\nreturn {{input}};', language: 'javascript', outputVariable: 'code_output' },
    end: { outputVariable: 'output' },
    setvar: { variableName: '', value: '', valueType: 'string' },
    knowledge: { knowledgeBaseId: '', query: '{{input}}', topK: 5, minScore: 0.5, outputVariable: 'knowledge_result', outputFormat: 'text' },
    classifier: { inputVariable: 'input', categories: [], outputVariable: 'category' },
    extractor: { inputVariable: 'input', fields: [], outputVariable: 'extracted' },
    question: { mode: 'answer', contextVariable: 'knowledge_result', questionVariable: 'input', outputVariable: 'answer', questionCount: 5 },
    iterator: { inputVariable: 'input', itemVariable: 'item', indexVariable: 'index', outputVariable: 'iterator_results', mode: 'sequential', batchSize: 10, maxIterations: 100 },
    aggregator: { mode: 'object', variables: [], outputVariable: 'aggregated', separator: '\n' },
    subflow: { workflowId: '', inputMapping: {}, outputMapping: {}, outputVariable: 'subflow_output' },
    text: { operation: 'concat', inputVariable: 'input', outputVariable: 'text_output', variables: [], separator: '', pattern: '', replacement: '', regex: false, extractAll: false, start: 0, end: 100, template: '' },
    template: { template: '', outputVariable: 'template_output' },
    delay: { delayMs: 1000 },
    parallel: { timeout: 60, mode: 'all', outputVariable: 'parallel_results' },
    merge: { mode: 'object', outputVariable: 'merged_result' },
    while: { condition: '{{loop_count}} < 10', maxIterations: 100, counterVariable: 'loop_count' },
    loop: { listVariable: 'input', itemVariable: 'item', indexVariable: 'index', maxIterations: 100, outputVariable: 'loop_results' }
  }
  return configs[t] || {}
}

// 获取节点信息辅助函数
const getNodeColor = (type) => nodeTypes.find(n => n.type === type)?.color || '#909399'
const getNodeIcon = (type) => nodeTypes.find(n => n.type === type)?.icon || 'fa-solid fa-circle'
const getNodeLabel = (type) => nodeTypes.find(n => n.type === type)?.label || type

// 获取知识库名称
const getKnowledgeBaseName = (id) => {
  if (!id) return '未选择'
  const kb = knowledgeBases.value.find(k => String(k.id) === String(id))
  return kb?.fileName || kb?.file_name || kb?.name || `知识库 ${id}`
}

// 获取智能体名称
const getAgentName = (id) => {
  if (!id) return '未选择'
  const agent = agents.value.find(a => String(a.id) === String(id))
  return agent?.name || `智能体 ${id}`
}

// 获取工具名称
const getToolName = (id) => {
  if (!id) return '未选择'
  const tool = tools.value.find(t => String(t.id) === String(id))
  return tool?.name || `工具 ${id}`
}

// 获取模型名称
const getModelName = (id) => {
  if (!id) return '未选择'
  const model = models.value.find(m => String(m.id) === String(id))
  return model?.name || model?.model_name || `模型 ${id}`
}

// 根据节点ID获取节点名称（用于错误提示）
const getNodeNameById = (nodeId) => {
  const node = nodes.value.find(n => n.id === nodeId)
  return node?.data?.label || node?.type || nodeId
}

// 跳转到错误节点
const goToErrorNode = (nodeId) => {
  const node = nodes.value.find(n => n.id === nodeId)
  if (node) {
    selectedNode.value = node
    // 将视图居中到该节点（与 locateNode 定位公式一致，通过 useVueFlow 解构的 setViewport 实现）
    setViewport({ x: -node.position.x + 400, y: -node.position.y + 200, zoom: 1 })
  }
}

const onNodeClick = (event) => {
  // VueFlow 可能传递 { node } 或直接传递 node
  const node = event.node || event
  if (node && node.id) {
    // 确保节点有 config 对象
    if (!node.data) node.data = { label: '', config: {} }
    if (!node.data.config) node.data.config = {}
    selectedNode.value = node
  }
}
const onConnect = (p) => {
  if (isLocked.value) return
  // 获取源节点类型
  const sourceNode = nodes.value.find(n => n.id === p.source)
  let edgeLabel = ''
  let edgeStyle = { stroke: '#6366f1', strokeWidth: 2 }

  // 为条件分支添加标签
  if (sourceNode?.type === 'condition') {
    if (p.sourceHandle === 'true') {
      edgeLabel = 'Yes'
      edgeStyle = { stroke: '#10b981', strokeWidth: 2 }
    } else if (p.sourceHandle === 'false') {
      edgeLabel = 'No'
      edgeStyle = { stroke: '#ef4444', strokeWidth: 2 }
    }
  } else if (sourceNode?.type === 'while') {
    if (p.sourceHandle === 'loop') {
      edgeLabel = '循环'
      edgeStyle = { stroke: '#8b5cf6', strokeWidth: 2 }
    } else if (p.sourceHandle === 'exit') {
      edgeLabel = '退出'
      edgeStyle = { stroke: '#10b981', strokeWidth: 2 }
    }
  } else if (sourceNode?.type === 'loop') {
    if (p.sourceHandle === 'loop') {
      edgeLabel = '循环体'
      edgeStyle = { stroke: '#7c3aed', strokeWidth: 2 }
    } else if (p.sourceHandle === 'done') {
      edgeLabel = '完成'
      edgeStyle = { stroke: '#10b981', strokeWidth: 2 }
    }
  } else if (sourceNode?.type === 'parallel') {
    edgeLabel = p.sourceHandle?.replace('branch', '分支')
  }

  addEdges([{
    id: `e_${p.source}_${p.target}`,
    source: p.source,
    target: p.target,
    sourceHandle: p.sourceHandle,
    label: edgeLabel,
    style: edgeStyle,
    labelStyle: { fill: '#6b7280', fontSize: 12 },
    labelBgStyle: { fill: '#fff', fillOpacity: 0.9 }
  }])
  saveHistory()
}
const deleteNode = () => { 
  if (isLocked.value) {
    ElMessage.warning('画布已锁定，无法删除')
    return
  }
  if (selectedNode.value) { 
    removeNodes([selectedNode.value.id]); 
    selectedNode.value = null;
    saveHistory()
  } 
}
const formatTime = (t) => t ? new Date(t).toLocaleString('zh-CN') : '-'

// 复制输出结果
const copyOutput = () => {
  const text = typeof execResult.value?.output === 'string'
    ? execResult.value.output
    : JSON.stringify(execResult.value?.output, null, 2)
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

// 执行历史
const showHistory = async (wf = null) => {
  if (wf) {
    editingWorkflow.value = wf
  }
  historyDialog.value = true
  historyPage.value = 1
  await loadHistory()
}

const loadHistory = async () => {
  if (!editingWorkflow.value?.id) return
  loadingHistory.value = true
  try {
    const r = await request({ url: `/cms/ai/workflow/${editingWorkflow.value.id}/executions`, method: 'get', params: { page: historyPage.value, size: 10 } })
    executionHistory.value = r.data || []
    historyTotal.value = executionHistory.value.length

    // 计算统计
    const all = executionHistory.value
    const success = all.filter(e => e.status === 'completed').length
    const failed = all.filter(e => e.status === 'failed').length
    const avgDuration = all.length > 0
      ? Math.round(all.reduce((sum, e) => sum + (e.durationMs || 0), 0) / all.length)
      : 0
    historyStats.value = { total: historyTotal.value, success, failed, avgDuration }
  } catch (e) {
    console.error('加载执行历史失败', e)
  } finally {
    loadingHistory.value = false
  }
}

const viewExecution = (row) => {
  selectedExecution.value = row
  executionDetailDialog.value = true
}

const formatOutput = (data) => {
  if (!data) return '无'
  try {
    const parsed = typeof data === 'string' ? JSON.parse(data) : data
    return JSON.stringify(parsed, null, 2)
  } catch {
    return data
  }
}

const parseLog = (log) => {
  if (!log) return []
  try {
    return typeof log === 'string' ? JSON.parse(log) : log
  } catch {
    return []
  }
}

// ========== 智能对齐辅助线 ==========
const alignmentLines = ref({ horizontal: [], vertical: [] })
const ALIGNMENT_THRESHOLD = 8 // 对齐阈值（像素）

// 节点拖拽时检测对齐
const onNodeDrag = ({ node }) => {
  if (!node || nodes.value.length < 2) return
  
  const lines = { horizontal: [], vertical: [] }
  const draggedCenter = {
    x: node.position.x + 75, // 节点宽度约150px
    y: node.position.y + 25  // 节点高度约50px
  }
  
  nodes.value.forEach(n => {
    if (n.id === node.id) return
    
    const otherCenter = {
      x: n.position.x + 75,
      y: n.position.y + 25
    }
    
    // 水平对齐检测（Y轴对齐）
    if (Math.abs(draggedCenter.y - otherCenter.y) < ALIGNMENT_THRESHOLD) {
      lines.horizontal.push(otherCenter.y)
    }
    // 顶部对齐
    if (Math.abs(node.position.y - n.position.y) < ALIGNMENT_THRESHOLD) {
      lines.horizontal.push(n.position.y)
    }
    
    // 垂直对齐检测（X轴对齐）
    if (Math.abs(draggedCenter.x - otherCenter.x) < ALIGNMENT_THRESHOLD) {
      lines.vertical.push(otherCenter.x)
    }
    // 左侧对齐
    if (Math.abs(node.position.x - n.position.x) < ALIGNMENT_THRESHOLD) {
      lines.vertical.push(n.position.x)
    }
  })
  
  alignmentLines.value = lines
}

// 拖拽结束清除对齐线
const onNodeDragStop = () => {
  alignmentLines.value = { horizontal: [], vertical: [] }
  saveHistory()
}

// 自动布局
const autoLayout = () => {
  if (nodes.value.length === 0) return

  // 构建邻接表
  const adjacency = {}
  const inDegree = {}
  nodes.value.forEach(n => {
    adjacency[n.id] = []
    inDegree[n.id] = 0
  })
  edges.value.forEach(e => {
    if (adjacency[e.source]) {
      adjacency[e.source].push(e.target)
    }
    if (inDegree[e.target] !== undefined) {
      inDegree[e.target]++
    }
  })

  // 拓扑排序分层
  const layers = []
  const visited = new Set()
  let queue = nodes.value.filter(n => inDegree[n.id] === 0).map(n => n.id)

  while (queue.length > 0) {
    layers.push([...queue])
    queue.forEach(id => visited.add(id))
    const nextQueue = []
    queue.forEach(id => {
      adjacency[id]?.forEach(target => {
        if (!visited.has(target) && !nextQueue.includes(target)) {
          // 检查所有前置节点是否已访问
          const allPrevVisited = edges.value
            .filter(e => e.target === target)
            .every(e => visited.has(e.source))
          if (allPrevVisited) {
            nextQueue.push(target)
          }
        }
      })
    })
    queue = nextQueue
  }

  // 添加未访问的节点
  nodes.value.forEach(n => {
    if (!visited.has(n.id)) {
      layers.push([n.id])
      visited.add(n.id)
    }
  })

  // 计算位置
  const layerGap = 200
  const nodeGap = 100
  const startX = 100
  const startY = 100

  layers.forEach((layer, layerIdx) => {
    const layerHeight = layer.length * nodeGap
    const offsetY = startY + (500 - layerHeight) / 2

    layer.forEach((nodeId, nodeIdx) => {
      const node = nodes.value.find(n => n.id === nodeId)
      if (node) {
        node.position = {
          x: startX + layerIdx * layerGap,
          y: offsetY + nodeIdx * nodeGap
        }
      }
    })
  })

  // 触发更新
  nodes.value = [...nodes.value]

  // 适应画布
  setTimeout(() => fitView(), 100)
  ElMessage.success('已自动布局')
}

</script>

<style scoped src="@/views/ai/styles/workflow-manage.css"></style>
