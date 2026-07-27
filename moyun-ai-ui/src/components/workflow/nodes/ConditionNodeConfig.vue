<template>
  <div class="condition-config">
    <!-- 条件表达式 -->
    <el-form-item label="条件表达式">
      <el-input
        v-model="localConfig.expression"
        type="textarea"
        :rows="3"
        placeholder="例如: {{score}} > 80"
        @blur="emitUpdate"
      />
      <div class="expression-help">
        <div class="help-title">支持的表达式：</div>
        <ul>
          <li><code>{{var}} == 'value'</code> 等于</li>
          <li><code>{{var}} != 'value'</code> 不等于</li>
          <li><code>{{var}} > 10</code> 大于</li>
          <li><code>{{var}} >= 10</code> 大于等于</li>
          <li><code>{{var}} < 10</code> 小于</li>
          <li><code>{{var}} contains 'text'</code> 包含</li>
          <li><code>{{var}} startsWith 'prefix'</code> 以...开头</li>
          <li><code>{{var}} isEmpty</code> 是否为空</li>
        </ul>
      </div>
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
          @click="insertVariable(v)"
        >
          {{ v }}
        </el-tag>
        <span v-if="!availableVariables?.length" class="no-vars">暂无可用变量</span>
      </div>
    </div>

    <!-- 分支说明 -->
    <el-alert
      type="info"
      :closable="false"
      style="margin-top: 16px;"
    >
      <template #title>
        条件节点有两个输出分支：
        <br/>• <strong>true</strong> - 条件成立时执行
        <br/>• <strong>false</strong> - 条件不成立时执行
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
  expression: ''
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
  localConfig.expression += `{{${varName}}}`
  emitUpdate()
}
</script>

<style scoped>
.condition-config {
  padding: 8px 0;
}

.expression-help {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 12px;
}

.help-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #606266;
}

.expression-help ul {
  margin: 0;
  padding-left: 16px;
  color: #909399;
}

.expression-help li {
  margin: 4px 0;
}

.expression-help code {
  background: #e4e7ed;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 11px;
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

.no-vars {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
