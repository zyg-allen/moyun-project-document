<template>
  <TabContainer :tabs="tabs" />
  <!-- 打赏管理 Tab 占位：原打赏功能已下线，如需恢复在 tabs 数组中新增一项即可 -->
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import OrderManage from '../order/index.vue'

const tabs = [
  { name: 'order', label: '付费订单', component: OrderManage }
]
</script>
