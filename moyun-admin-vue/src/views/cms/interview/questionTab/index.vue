<template>
  <TabContainer :tabs="tabs" variant="borderless" />
</template>

<script setup name="InterviewQuestionTab">
import TabContainer from '@/components/TabContainer'
import QuestionPanel from '../question/index.vue'
import CategoryPanel from '../category/index.vue'
import CompanyPanel from '../company/index.vue'
import ResumePanel from '../resume/index.vue'

const tabs = [
  { name: 'question', label: '面试题库', component: QuestionPanel },
  { name: 'category', label: '分类管理', component: CategoryPanel },
  { name: 'company', label: '公司标签', component: CompanyPanel },
  { name: 'resume', label: '简历模板', component: ResumePanel }
]
</script>
