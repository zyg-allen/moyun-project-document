<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import GrowthRule from '../growth/rule/index.vue'
import GrowthAchievement from '../growth/achievement/index.vue'
import GrowthBadge from '../growth/badge/index.vue'
import GrowthLog from '../growth/log/index.vue'
import GrowthUser from '../growth/user/index.vue'

const tabs = [
  { name: 'rule', label: '成长规则', component: GrowthRule },
  { name: 'achievement', label: '成就管理', component: GrowthAchievement },
  { name: 'badge', label: '徽章管理', component: GrowthBadge },
  { name: 'log', label: '成长日志', component: GrowthLog },
  { name: 'user', label: '用户成长', component: GrowthUser }
]
</script>
