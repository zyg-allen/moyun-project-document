<template>
  <div class="llm-config">
    <!-- 模型选择 -->
    <el-form-item label="模型">
      <el-select v-model="localConfig.modelId" placeholder="选择模型" @change="emitUpdate">
        <el-option
          v-for="model in models"
          :key="model.id"
          :label="model.name"
          :value="model.id"
        />
      </el-select>
    </el-form-item>

    <!-- 系统提示词 -->
    <el-form-item label="系统提示词">
      <el-input
        v-model="localConfig.systemPrompt"
        type="textarea"
        :rows="4"
        placeholder="设置AI的角色和行为规则..."
        @blur="emitUpdate"
      />
    </el-form-item>

    <!-- 用户提示词 -->
    <el-form-item label="用户提示词">
      <el-input
        v-model="localConfig.userPrompt"
        type="textarea"
        :rows="4"
        placeholder="使用 {{变量名}} 引用上游变量"
        @blur="emitUpdate"
      />
      <div class="variable-hints">
        <span class="hint-label">可用变量：</span>
        <el-tag 
          v-for="v in availableVariables" 
          :key="v" 
          size="small" 
          type="info"
          @click="insertVariable(v)"
          class="var-tag"
        >
          {{ v }}
        </el-tag>
      </div>
    </el-form-item>

    <!-- 输出变量 -->
    <el-form-item label="输出变量名">
      <el-input v-model="localConfig.outputVariable" placeholder="llm_output" @blur="emitUpdate" />
    </el-form-item>

    <!-- 高级配置 -->
    <el-collapse class="advanced-config">
      <el-collapse-item title="高级配置" name="advanced">
        <el-form-item label="Temperature">
          <el-slider 
            v-model="localConfig.temperature" 
            :min="0" 
            :max="2" 
            :step="0.1" 
            show-input
            :show-input-controls="false"
            @change="emitUpdate"
          />
          <div class="param-hint">控制输出随机性：0=确定性，1=平衡，2=创造性</div>
        </el-form-item>
        
        <el-form-item label="最大Token数">
          <el-input-number 
            v-model="localConfig.maxTokens" 
            :min="100" 
            :max="8000" 
            :step="100"
            @change="emitUpdate"
          />
          <div class="param-hint">限制AI回复的最大长度</div>
        </el-form-item>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import axios from 'axios'

const props = defineProps({
  config: Object,
  availableVariables: Array
})

const emit = defineEmits(['update:config'])

const models = ref([])

const localConfig = reactive({
  modelId: null,
  systemPrompt: '',
  userPrompt: '{{input}}',
  outputVariable: 'llm_output',
  temperature: 0.7,
  maxTokens: 2000
})

// 同步外部配置
watch(() => props.config, (newConfig) => {
  if (newConfig) {
    Object.assign(localConfig, newConfig)
  }
}, { immediate: true, deep: true })

// 加载模型列表
onMounted(async () => {
  try {
    const res = await axios.get('/api/model-config/list')
    if (res.data.success) {
      models.value = res.data.data.list || []
    }
  } catch (e) {
    console.error('加载模型列表失败:', e)
  }
})

// 发送更新
const emitUpdate = () => {
  emit('update:config', { ...localConfig })
}

// 插入变量
const insertVariable = (varName) => {
  localConfig.userPrompt += `{{${varName}}}`
  emitUpdate()
}
</script>

<style scoped>
.llm-config {
  padding: 8px 0;
}

.variable-hints {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.hint-label {
  font-size: 12px;
  color: #909399;
}

.var-tag {
  cursor: pointer;
}

.var-tag:hover {
  background: #409eff;
  color: white;
}

.advanced-config {
  margin-top: 12px;
  border: none;
}

.advanced-config :deep(.el-collapse-item__header) {
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
  background: #f8fafc;
  border-radius: 6px;
  padding: 0 12px;
  height: 36px;
}

.advanced-config :deep(.el-collapse-item__content) {
  padding: 12px 0;
}

.param-hint {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>
