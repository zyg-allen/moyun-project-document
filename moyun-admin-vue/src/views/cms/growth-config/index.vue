<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import GrowthRule from '../growth/rule/index.vue'
import GrowthAchievement from '../growth/achievement/index.vue'

const tabs = [
  { name: 'rule', label: '成长规则', component: GrowthRule },
  { name: 'achievement', label: '成就管理', component: GrowthAchievement }
]
</script>
