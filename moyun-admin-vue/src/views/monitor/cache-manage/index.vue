<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import CacheMonitor from '../cache/index.vue'
import CacheList from '../cache/list.vue'

const tabs = [
  { name: 'cache', label: '缓存监控', component: CacheMonitor },
  { name: 'list', label: '缓存列表', component: CacheList }
]
</script>
