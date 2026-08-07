<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="知识库管理" name="knowledge-base">
        <knowledge-base v-if="loaded['knowledge-base']" v-show="activeTab === 'knowledge-base'" :embedded="true" />
      </el-tab-pane>
      <el-tab-pane label="知识文库" name="knowledge-library">
        <knowledge-library v-if="loaded['knowledge-library']" v-show="activeTab === 'knowledge-library'" :embedded="true" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import KnowledgeBase from '../knowledge-base/index.vue'
import KnowledgeLibrary from '../knowledge-library/index.vue'

const activeTab = ref('knowledge-base')
const loaded = reactive({ 'knowledge-base': true, 'knowledge-library': false })

// Tab 切换时按需加载组件（避免首次进入即加载两个重组件）
function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
