<template>
  <div class="http-config">
    <!-- 请求方法 -->
    <el-form-item label="请求方法">
      <el-select v-model="localConfig.method" @change="emitUpdate">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
        <el-option label="PATCH" value="PATCH" />
      </el-select>
    </el-form-item>

    <!-- URL -->
    <el-form-item label="请求URL">
      <el-input 
        v-model="localConfig.url" 
        placeholder="https://api.example.com/data"
        @blur="emitUpdate"
      >
        <template #prefix>
          <i class="fa-solid fa-link"></i>
        </template>
      </el-input>
      <div class="param-hint">支持使用 {{变量名}} 动态替换</div>
    </el-form-item>

    <!-- 请求头 -->
    <el-form-item label="请求头 (JSON)">
      <el-input
        v-model="localConfig.headers"
        type="textarea"
        :rows="3"
        placeholder='{"Content-Type": "application/json", "Authorization": "Bearer {{token}}"}'
        @blur="emitUpdate"
      />
    </el-form-item>

    <!-- 请求体 (POST/PUT/PATCH) -->
    <el-form-item v-if="['POST', 'PUT', 'PATCH'].includes(localConfig.method)" label="请求体 (JSON)">
      <el-input
        v-model="localConfig.body"
        type="textarea"
        :rows="5"
        placeholder='{"key": "{{value}}"}'
        @blur="emitUpdate"
      />
    </el-form-item>

    <!-- 超时设置 -->
    <el-form-item label="超时时间（秒）">
      <el-input-number 
        v-model="localConfig.timeout" 
        :min="1" 
        :max="300"
        @change="emitUpdate"
      />
    </el-form-item>

    <!-- 输出变量 -->
    <el-form-item label="输出变量名">
      <el-input 
        v-model="localConfig.outputVariable" 
        placeholder="http_response"
        @blur="emitUpdate"
      />
      <div class="param-hint">响应数据将保存到此变量</div>
    </el-form-item>

    <!-- 可用变量 -->
    <div class="available-vars">
      <div class="vars-title">
        <i class="fa-solid fa-code"></i> 可用变量
      </div>
      <div class="vars-list">
        <el-tag 
          v-for="v in availableVariables" 
          :key="v" 
          size="small"
          class="var-tag"
          @click="copyVariable(v)"
        >
          {{ v }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  config: Object,
  availableVariables: Array
})

const emit = defineEmits(['update:config'])

const localConfig = reactive({
  method: 'GET',
  url: '',
  headers: '{"Content-Type": "application/json"}',
  body: '',
  timeout: 30,
  outputVariable: 'http_response'
})

watch(() => props.config, (newConfig) => {
  if (newConfig) {
    Object.assign(localConfig, newConfig)
  }
}, { immediate: true, deep: true })

const emitUpdate = () => {
  emit('update:config', { ...localConfig })
}

const copyVariable = (varName) => {
  navigator.clipboard.writeText(`{{${varName}}}`)
  ElMessage.success(`已复制 {{${varName}}}`)
}
</script>

<style scoped>
.http-config {
  padding: 8px 0;
}

.param-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
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
</style>
