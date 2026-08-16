<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import OrderManage from '../order/index.vue'
import TipManage from '../tip/index.vue'

const tabs = [
  { name: 'order', label: '付费订单', component: OrderManage },
  { name: 'tip', label: '打赏流水', component: TipManage }
]
</script>
