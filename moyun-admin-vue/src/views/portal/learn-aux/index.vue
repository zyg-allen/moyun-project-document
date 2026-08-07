<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="学习计划" name="study-plan">
        <study-plan v-if="loaded['study-plan']" v-show="activeTab === 'study-plan'" />
      </el-tab-pane>
      <el-tab-pane label="错题本" name="wrong-question">
        <wrong-question v-if="loaded['wrong-question']" v-show="activeTab === 'wrong-question'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import StudyPlan from '../studyPlan/index.vue'
import WrongQuestion from '../wrongQuestion/index.vue'

const activeTab = ref('study-plan')
const loaded = reactive({ 'study-plan': true, 'wrong-question': false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
