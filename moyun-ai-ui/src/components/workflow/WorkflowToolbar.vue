<template>
  <div class="workflow-toolbar">
    <!-- 左侧：返回和标题 -->
    <div class="toolbar-left">
      <el-button text @click="$emit('back')">
        <i class="fa-solid fa-arrow-left"></i>
      </el-button>
      
      <div class="workflow-title" v-if="workflow">
        <span class="title-text">{{ workflow.name }}</span>
        <el-tag v-if="workflow.status === 1" type="success" size="small">已发布</el-tag>
        <el-tag v-else type="info" size="small">草稿</el-tag>
        <span v-if="hasChanges" class="unsaved-indicator">
          <i class="fa-solid fa-circle"></i> 未保存
        </span>
      </div>
    </div>

    <!-- 中间：视图控制 -->
    <div class="toolbar-center">
      <el-button-group>
        <el-tooltip content="放大">
          <el-button @click="$emit('zoom-in')" :icon="ZoomIn" />
        </el-tooltip>
        <el-tooltip content="缩小">
          <el-button @click="$emit('zoom-out')" :icon="ZoomOut" />
        </el-tooltip>
        <el-tooltip content="适应画布">
          <el-button @click="$emit('fit-view')" :icon="FullScreen" />
        </el-tooltip>
      </el-button-group>
      
      <el-divider direction="vertical" />
      
      <el-button-group>
        <el-tooltip content="撤销 (Ctrl+Z)">
          <el-button @click="$emit('undo')" :disabled="!canUndo">
            <i class="fa-solid fa-rotate-left"></i>
          </el-button>
        </el-tooltip>
        <el-tooltip content="重做 (Ctrl+Y)">
          <el-button @click="$emit('redo')" :disabled="!canRedo">
            <i class="fa-solid fa-rotate-right"></i>
          </el-button>
        </el-tooltip>
      </el-button-group>
    </div>

    <!-- 右侧：操作按钮 -->
    <div class="toolbar-right">
      <!-- 调试模式开关 -->
      <el-switch
        v-model="debugModeLocal"
        active-text="调试"
        inactive-text=""
        @change="$emit('update:debugMode', $event)"
        style="margin-right: 16px;"
      />
      
      <!-- 运行按钮 -->
      <el-button 
        type="success" 
        @click="$emit('run')"
        :loading="running"
        :disabled="running"
      >
        <i class="fa-solid fa-play"></i>
        {{ running ? '运行中...' : '运行' }}
      </el-button>
      
      <!-- 保存按钮 -->
      <el-button 
        type="primary" 
        @click="$emit('save')"
        :loading="saving"
      >
        <i class="fa-solid fa-floppy-disk"></i>
        保存
      </el-button>
      
      <!-- 更多操作 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <el-button>
          <i class="fa-solid fa-ellipsis-v"></i>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="publish">
              <i class="fa-solid fa-rocket"></i> 发布
            </el-dropdown-item>
            <el-dropdown-item command="history" divided>
              <i class="fa-solid fa-clock-rotate-left"></i> 执行历史
            </el-dropdown-item>
            <el-dropdown-item command="versions">
              <i class="fa-solid fa-code-branch"></i> 版本管理
            </el-dropdown-item>
            <el-dropdown-item command="export" divided>
              <i class="fa-solid fa-file-export"></i> 导出
            </el-dropdown-item>
            <el-dropdown-item command="import">
              <i class="fa-solid fa-file-import"></i> 导入
            </el-dropdown-item>
            <el-dropdown-item command="ai-generate" divided>
              <i class="fa-solid fa-wand-magic-sparkles"></i> AI 生成
            </el-dropdown-item>
            <el-dropdown-item command="delete" divided>
              <i class="fa-solid fa-trash" style="color: #f56c6c;"></i>
              <span style="color: #f56c6c;">删除工作流</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ZoomIn, ZoomOut, FullScreen } from '@element-plus/icons-vue'

const props = defineProps({
  workflow: Object,
  hasChanges: Boolean,
  running: Boolean,
  saving: Boolean,
  debugMode: Boolean,
  canUndo: Boolean,
  canRedo: Boolean
})

const emit = defineEmits([
  'back', 'save', 'run', 'publish',
  'zoom-in', 'zoom-out', 'fit-view',
  'undo', 'redo',
  'history', 'versions', 'export', 'import', 'delete', 'ai-generate',
  'update:debugMode'
])

const debugModeLocal = ref(props.debugMode)

watch(() => props.debugMode, (val) => {
  debugModeLocal.value = val
})

const handleCommand = (command) => {
  emit(command)
}
</script>

<style scoped>
.workflow-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  height: 56px;
  box-sizing: border-box;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workflow-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.unsaved-indicator {
  font-size: 12px;
  color: #e6a23c;
  display: flex;
  align-items: center;
  gap: 4px;
}

.unsaved-indicator i {
  font-size: 6px;
}

.toolbar-center {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
