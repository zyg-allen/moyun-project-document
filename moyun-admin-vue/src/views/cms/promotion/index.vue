<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import AdManage from '../ad/index.vue'
import FriendLink from '../friend-link/index.vue'

const tabs = [
  { name: 'ad', label: '广告位', component: AdManage },
  { name: 'friend-link', label: '友情链接', component: FriendLink }
]
</script>
