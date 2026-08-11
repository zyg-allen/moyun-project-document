<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import Feedback from '../feedback/index.vue'
import Report from '../report/index.vue'

const tabs = [
  { name: 'feedback', label: '反馈管理', component: Feedback },
  { name: 'report', label: '举报管理', component: Report }
]
</script>
