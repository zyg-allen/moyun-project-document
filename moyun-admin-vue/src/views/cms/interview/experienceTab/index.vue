<template>
  <TabContainer :tabs="tabs" variant="borderless" />
</template>

<script setup name="InterviewExperienceTab">
import TabContainer from '@/components/TabContainer'
import ExperiencePanel from '../experience/index.vue'
import CommentPanel from '../comment/index.vue'

const tabs = [
  { name: 'experience', label: '面经管理', component: ExperiencePanel },
  { name: 'comment', label: '评论管理', component: CommentPanel }
]
</script>
