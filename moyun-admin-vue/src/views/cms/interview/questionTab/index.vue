<template>
  <div class="tab-container-wrapper">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" type="border-card">
      <el-tab-pane label="面试题库" name="question">
        <question-panel v-if="loaded.question" v-show="activeTab === 'question'" />
      </el-tab-pane>
      <el-tab-pane label="分类管理" name="category">
        <category-panel v-if="loaded.category" v-show="activeTab === 'category'" />
      </el-tab-pane>
      <el-tab-pane label="公司标签" name="company">
        <company-panel v-if="loaded.company" v-show="activeTab === 'company'" />
      </el-tab-pane>
      <el-tab-pane label="简历模板" name="resume">
        <resume-panel v-if="loaded.resume" v-show="activeTab === 'resume'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="InterviewQuestionTab">
import { ref, reactive } from 'vue'
import QuestionPanel from '../question/index.vue'
import CategoryPanel from '../category/index.vue'
import CompanyPanel from '../company/index.vue'
import ResumePanel from '../resume/index.vue'

const activeTab = ref('question')
const loaded = reactive({
  question: true,
  category: false,
  company: false,
  resume: false
})

function handleTabChange(tabName) {
  if (!loaded[tabName]) {
    loaded[tabName] = true
  }
}
</script>

<style scoped>
.tab-container-wrapper {
  /* 不使用 app-container，子页面自带 app-container padding */
}
:deep(.el-tabs--border-card) {
  box-shadow: none;
  border: none;
}
:deep(.el-tabs__content) {
  padding: 0;
}
</style>
