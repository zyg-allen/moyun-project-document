<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import ServerMonitor from '../server/index.vue'
import DruidMonitor from '../druid/index.vue'

const tabs = [
  { name: 'server', label: '服务器监控', component: ServerMonitor },
  { name: 'druid', label: '数据监控', component: DruidMonitor }
]
</script>
