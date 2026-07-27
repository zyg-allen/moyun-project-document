<template>
  <div class="code-config">
    <!-- 编程语言 -->
    <el-form-item label="编程语言">
      <el-select v-model="localConfig.language" @change="emitUpdate">
        <el-option label="JavaScript" value="javascript" />
      </el-select>
    </el-form-item>

    <!-- 代码编辑器 -->
    <el-form-item label="代码">
      <div class="code-editor-wrapper">
        <el-input
          v-model="localConfig.code"
          type="textarea"
          :rows="12"
          placeholder="// 编写 JavaScript 代码
// 可通过变量名直接访问上游变量
// 使用 return 语句返回结果

return input.toUpperCase();"
          @blur="emitUpdate"
          class="code-textarea"
        />
      </div>
    </el-form-item>

    <!-- 输出变量 -->
    <el-form-item label="输出变量名">
      <el-input 
        v-model="localConfig.outputVariable" 
        placeholder="code_result"
        @blur="emitUpdate"
      />
    </el-form-item>

    <!-- 可用变量 -->
    <div class="available-vars">
      <div class="vars-title">
        <i class="fa-solid fa-code"></i> 可在代码中使用的变量
      </div>
      <div class="vars-list">
        <el-tag 
          v-for="v in availableVariables" 
          :key="v" 
          size="small"
          class="var-tag"
          @click="insertVariable(v)"
        >
          {{ v }}
        </el-tag>
      </div>
    </div>

    <!-- 代码示例 -->
    <el-collapse class="examples-collapse">
      <el-collapse-item title="代码示例" name="examples">
        <div class="example-item">
          <div class="example-title">字符串处理</div>
          <pre>return input.toUpperCase();</pre>
        </div>
        <div class="example-item">
          <div class="example-title">JSON 解析</div>
          <pre>const data = JSON.parse(input);
return data.name;</pre>
        </div>
        <div class="example-item">
          <div class="example-title">数组处理</div>
          <pre>const items = input.split(',');
return items.map(s => s.trim());</pre>
        </div>
        <div class="example-item">
          <div class="example-title">条件判断</div>
          <pre>if (score > 80) {
  return '优秀';
} else if (score > 60) {
  return '及格';
}
return '不及格';</pre>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- 注意事项 -->
    <el-alert
      type="warning"
      :closable="false"
      style="margin-top: 16px;"
    >
      <template #title>
        <strong>注意事项：</strong>
        <br/>1. 代码在安全沙箱中执行，无法访问文件系统和网络
        <br/>2. 执行超时时间为 10 秒
        <br/>3. 必须使用 return 语句返回结果
      </template>
    </el-alert>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  config: Object,
  availableVariables: Array
})

const emit = defineEmits(['update:config'])

const localConfig = reactive({
  language: 'javascript',
  code: 'return input;',
  outputVariable: 'code_result'
})

watch(() => props.config, (newConfig) => {
  if (newConfig) {
    Object.assign(localConfig, newConfig)
  }
}, { immediate: true, deep: true })

const emitUpdate = () => {
  emit('update:config', { ...localConfig })
}

const insertVariable = (varName) => {
  localConfig.code += varName
  emitUpdate()
}
</script>

<style scoped>
.code-config {
  padding: 8px 0;
}

.code-editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.code-textarea :deep(textarea) {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
  line-height: 1.5;
  background: #1e1e1e;
  color: #d4d4d4;
}

.available-vars {
  margin-top: 16px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px dashed #e2e8f0;
}

.vars-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.vars-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.var-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.var-tag:hover {
  background: #409eff;
  color: white;
}

.examples-collapse {
  margin-top: 16px;
  border: none;
}

.examples-collapse :deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: #f8fafc;
  border-radius: 6px;
  padding: 0 12px;
  height: 36px;
}

.example-item {
  margin-bottom: 12px;
}

.example-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
}

.example-item pre {
  margin: 0;
  padding: 8px 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
  overflow-x: auto;
}
</style>
