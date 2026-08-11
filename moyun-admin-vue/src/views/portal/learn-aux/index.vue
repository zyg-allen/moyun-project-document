<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import StudyPlan from '../studyPlan/index.vue'
import WrongQuestion from '../wrongQuestion/index.vue'

const tabs = [
  { name: 'study-plan', label: '学习计划', component: StudyPlan },
  { name: 'wrong-question', label: '错题本', component: WrongQuestion }
]
</script>
