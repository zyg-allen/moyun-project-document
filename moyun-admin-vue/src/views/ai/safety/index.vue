<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>AI 内容安全检测</span>
          <span class="header-tip">LLM 级文本复核（语义/变体识别）——DFA 词库只能字面匹配，本工具经统一网关调用 AI 场景检测，限流与成本熔断自动生效</span>
        </div>
      </template>

      <el-form label-width="90px">
        <el-form-item label="待检测文本">
          <el-input
            v-model="text"
            type="textarea"
            :rows="8"
            maxlength="8000"
            show-word-limit
            placeholder="粘贴需要复核的文本：话题/观点/评论/文章片段等（最多 8000 字符）"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="detecting" @click="handleDetect">
            {{ detecting ? '检测中…' : '开始检测' }}
          </el-button>
          <el-button @click="handleClear">清空</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>
        <div class="card-header">
          <span>检测结果</span>
          <span v-if="meta" class="header-tip">
            requestId: {{ meta.requestId || '-' }} · 耗时 {{ meta.elapsedMs != null ? meta.elapsedMs + 'ms' : '-' }} · 模型 {{ meta.modelUsed || '-' }} · token {{ meta.tokenUsed != null ? meta.tokenUsed : '-' }}
          </span>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="是否包含敏感内容">
          <el-tag :type="result.hasSensitive ? 'danger' : 'success'">
            {{ result.hasSensitive ? '命中敏感内容' : '未检出敏感内容' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="风险等级">
          <el-tag :type="riskTagType(result.riskLevel)">{{ riskLabel(result.riskLevel) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="result.words && result.words.length" label="命中词/类别">
          <el-tag v-for="w in result.words" :key="w" type="warning" size="small" class="word-tag">{{ w }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="处理建议">{{ result.suggestion || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { detectText } from '@/api/ai/safety';

const text = ref('');
const detecting = ref(false);
const result = ref(null);
const meta = ref(null);

async function handleDetect() {
  if (!text.value || !text.value.trim()) {
    ElMessage.warning('请输入待检测文本');
    return;
  }
  detecting.value = true;
  result.value = null;
  meta.value = null;
  try {
    const res = await detectText(text.value);
    const data = res.data || {};
    result.value = data.data || {};
    meta.value = data;
  } catch (e) { /* request 已提示 */ } finally {
    detecting.value = false;
  }
}

function handleClear() {
  text.value = '';
  result.value = null;
  meta.value = null;
}

function riskLabel(level) {
  return { high: '高风险', medium: '中风险', low: '低风险' }[level] || (level || '-');
}

function riskTagType(level) {
  return { high: 'danger', medium: 'warning', low: 'success' }[level] || 'info';
}
</script>

<style scoped>
.app-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-tip { font-size: 12px; color: #909399; font-weight: normal; }
.result-card { margin-top: 16px; }
.word-tag { margin-right: 6px; }
</style>
