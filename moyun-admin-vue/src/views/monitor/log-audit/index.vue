<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import OperLog from '../operlog/index.vue'
import LoginInfor from '../logininfor/index.vue'

const tabs = [
  { name: 'operlog', label: '操作日志', component: OperLog },
  { name: 'logininfor', label: '登录日志', component: LoginInfor }
]
</script>
