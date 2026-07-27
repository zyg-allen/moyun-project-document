<template>
  <div class="node-library">
    <div class="library-header">
      <h3>节点库</h3>
      <el-input 
        v-model="searchText" 
        placeholder="搜索节点..." 
        prefix-icon="Search" 
        clearable 
        size="small"
      />
    </div>
    
    <div class="library-content">
      <div 
        v-for="category in filteredCategories" 
        :key="category.name" 
        class="node-category"
      >
        <div class="category-header" @click="toggleCategory(category.name)">
          <i :class="expandedCategories.includes(category.name) ? 'fa-solid fa-chevron-down' : 'fa-solid fa-chevron-right'"></i>
          <span>{{ category.label }}</span>
          <span class="node-count">{{ category.nodes.length }}</span>
        </div>
        
        <transition name="expand">
          <div v-show="expandedCategories.includes(category.name)" class="category-nodes">
            <div
              v-for="node in category.nodes"
              :key="node.type"
              class="node-item"
              draggable="true"
              @dragstart="onDragStart($event, node)"
              @click="$emit('add-node', node)"
            >
              <div class="node-icon" :style="{ background: node.color }">
                <i :class="node.icon"></i>
              </div>
              <div class="node-info">
                <div class="node-name">{{ node.label }}</div>
                <div class="node-desc">{{ node.description }}</div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  nodeTypes: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['add-node', 'drag-start'])

const searchText = ref('')
const expandedCategories = ref(['basic', 'ai', 'control', 'data'])

// 节点分类
const categories = computed(() => [
  {
    name: 'basic',
    label: '基础节点',
    nodes: props.nodeTypes.filter(n => ['start', 'end'].includes(n.type))
  },
  {
    name: 'ai',
    label: 'AI 节点',
    nodes: props.nodeTypes.filter(n => ['llm', 'agent', 'knowledge', 'question', 'classifier', 'extractor'].includes(n.type))
  },
  {
    name: 'control',
    label: '流程控制',
    nodes: props.nodeTypes.filter(n => ['condition', 'loop', 'iterator', 'while', 'parallel', 'merge', 'aggregator', 'delay', 'subflow'].includes(n.type))
  },
  {
    name: 'data',
    label: '数据处理',
    nodes: props.nodeTypes.filter(n => ['setvar', 'text', 'template', 'code', 'http', 'tool'].includes(n.type))
  }
])

// 过滤后的分类
const filteredCategories = computed(() => {
  if (!searchText.value) return categories.value
  
  const search = searchText.value.toLowerCase()
  return categories.value
    .map(cat => ({
      ...cat,
      nodes: cat.nodes.filter(n => 
        n.label.toLowerCase().includes(search) || 
        n.description?.toLowerCase().includes(search) ||
        n.type.toLowerCase().includes(search)
      )
    }))
    .filter(cat => cat.nodes.length > 0)
})

// 切换分类展开
const toggleCategory = (name) => {
  const index = expandedCategories.value.indexOf(name)
  if (index >= 0) {
    expandedCategories.value.splice(index, 1)
  } else {
    expandedCategories.value.push(name)
  }
}

// 拖拽开始
const onDragStart = (event, node) => {
  event.dataTransfer.setData('application/workflow-node', JSON.stringify(node))
  event.dataTransfer.effectAllowed = 'copy'
  emit('drag-start', node)
}
</script>

<style scoped>
.node-library {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.library-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.library-header h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.library-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.node-category {
  margin-bottom: 8px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}

.category-header:hover {
  background: #e4e7ed;
}

.category-header i {
  font-size: 10px;
  color: #909399;
  transition: transform 0.2s;
}

.category-header span {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

.node-count {
  flex: none !important;
  font-size: 11px;
  color: #909399;
  background: #e4e7ed;
  padding: 2px 8px;
  border-radius: 10px;
}

.category-nodes {
  padding: 8px 0 0 0;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin: 4px 0;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
}

.node-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
  transform: translateX(4px);
}

.node-item:active {
  cursor: grabbing;
}

.node-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  flex-shrink: 0;
}

.node-info {
  flex: 1;
  min-width: 0;
}

.node-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 2px;
}

.node-desc {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 展开动画 */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  max-height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
