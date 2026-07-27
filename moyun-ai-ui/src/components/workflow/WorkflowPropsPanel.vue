<template>
  <div class="props-panel" v-if="selectedNode">
    <div class="panel-header">
      <div class="header-left">
        <div class="node-icon" :style="{ background: getNodeColor(selectedNode.type) }">
          <i :class="getNodeIcon(selectedNode.type)"></i>
        </div>
        <div class="header-info">
          <input 
            v-model="selectedNode.data.label" 
            class="node-name-input"
            @blur="$emit('update-node', selectedNode)"
          />
          <span class="node-type">{{ getNodeLabel(selectedNode.type) }}</span>
        </div>
      </div>
      <el-button text @click="$emit('close')">
        <i class="fa-solid fa-xmark"></i>
      </el-button>
    </div>

    <div class="panel-content">
      <el-scrollbar>
        <!-- 节点描述 -->
        <div class="config-section">
          <div class="section-title">节点描述</div>
          <el-input
            v-model="selectedNode.data.description"
            type="textarea"
            :rows="2"
            placeholder="添加节点说明..."
            @blur="$emit('update-node', selectedNode)"
          />
        </div>

        <!-- 节点配置（动态加载） -->
        <div class="config-section">
          <div class="section-title">配置</div>
          
          <!-- 根据节点类型显示不同配置 -->
          <component 
            :is="getConfigComponent(selectedNode.type)"
            v-if="getConfigComponent(selectedNode.type)"
            :config="selectedNode.data.config"
            :available-variables="availableVariables"
            @update:config="handleConfigUpdate"
          />
          
          <!-- 默认配置（JSON编辑） -->
          <div v-else class="default-config">
            <el-input
              v-model="configJson"
              type="textarea"
              :rows="10"
              @blur="saveJsonConfig"
            />
          </div>
        </div>

        <!-- 验证错误 -->
        <div v-if="validationErrors.length > 0" class="validation-errors">
          <el-alert
            v-for="(error, idx) in validationErrors"
            :key="idx"
            :title="error"
            type="error"
            :closable="false"
            show-icon
            style="margin-bottom: 8px;"
          />
        </div>
      </el-scrollbar>
    </div>

    <div class="panel-footer">
      <el-button type="danger" text @click="$emit('delete-node', selectedNode.id)">
        <i class="fa-solid fa-trash"></i> 删除节点
      </el-button>
      <el-button type="primary" @click="$emit('update-node', selectedNode)">
        <i class="fa-solid fa-check"></i> 应用
      </el-button>
    </div>
  </div>
  
  <div v-else class="empty-panel">
    <i class="fa-solid fa-mouse-pointer"></i>
    <p>选择一个节点查看配置</p>
  </div>
</template>

<script setup>
import { ref, computed, watch, shallowRef, defineAsyncComponent } from 'vue'

// 异步加载节点配置组件
const LlmNodeConfig = defineAsyncComponent(() => import('./nodes/LlmNodeConfig.vue'))
const ConditionNodeConfig = defineAsyncComponent(() => import('./nodes/ConditionNodeConfig.vue'))
const LoopNodeConfig = defineAsyncComponent(() => import('./nodes/LoopNodeConfig.vue'))
const HttpNodeConfig = defineAsyncComponent(() => import('./nodes/HttpNodeConfig.vue'))
const CodeNodeConfig = defineAsyncComponent(() => import('./nodes/CodeNodeConfig.vue'))

const props = defineProps({
  selectedNode: Object,
  nodeTypes: Array,
  availableVariables: Array,
  validationErrors: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update-node', 'delete-node', 'close'])

// JSON配置（用于默认编辑器）
const configJson = ref('')

watch(() => props.selectedNode?.data?.config, (config) => {
  if (config) {
    configJson.value = JSON.stringify(config, null, 2)
  }
}, { immediate: true, deep: true })

// 保存JSON配置
const saveJsonConfig = () => {
  try {
    props.selectedNode.data.config = JSON.parse(configJson.value)
    emit('update-node', props.selectedNode)
  } catch (e) {
    console.error('JSON解析失败:', e)
  }
}

// 配置更新处理
const handleConfigUpdate = (newConfig) => {
  props.selectedNode.data.config = { ...props.selectedNode.data.config, ...newConfig }
  emit('update-node', props.selectedNode)
}

// 获取节点配置组件
const getConfigComponent = (type) => {
  const componentMap = {
    'llm': LlmNodeConfig,
    'condition': ConditionNodeConfig,
    'loop': LoopNodeConfig,
    'http': HttpNodeConfig,
    'code': CodeNodeConfig
  }
  return componentMap[type]
}

// 获取节点信息
const getNodeInfo = (type) => {
  return props.nodeTypes?.find(n => n.type === type) || {}
}

const getNodeColor = (type) => getNodeInfo(type).color || '#909399'
const getNodeIcon = (type) => getNodeInfo(type).icon || 'fa-solid fa-cube'
const getNodeLabel = (type) => getNodeInfo(type).label || type
</script>

<style scoped>
.props-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e4e7ed;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-name-input {
  border: none;
  outline: none;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  background: transparent;
  padding: 0;
  width: 180px;
}

.node-name-input:focus {
  border-bottom: 1px solid #409eff;
}

.node-type {
  font-size: 12px;
  color: #909399;
}

.panel-content {
  flex: 1;
  overflow: hidden;
  padding: 16px;
}

.config-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
}

.validation-errors {
  margin-top: 16px;
}

.panel-footer {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
}

.empty-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.empty-panel i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-panel p {
  font-size: 14px;
}

.default-config {
  font-family: monospace;
}
</style>
