<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="反馈管理" name="feedback">
        <feedback v-if="loaded.feedback" v-show="activeTab === 'feedback'" />
      </el-tab-pane>
      <el-tab-pane label="举报管理" name="report">
        <report v-if="loaded.report" v-show="activeTab === 'report'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import Feedback from '../feedback/index.vue'
import Report from '../report/index.vue'

const activeTab = ref('feedback')
const loaded = reactive({ feedback: true, report: false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
