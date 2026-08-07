<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="付费订单" name="order">
        <order-manage v-if="loaded.order" v-show="activeTab === 'order'" />
      </el-tab-pane>
      <!-- 打赏管理 Tab 占位：原打赏功能已下线，如需恢复在此添加 -->
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import OrderManage from '../order/index.vue'

const activeTab = ref('order')
const loaded = reactive({ order: true })

function handleTabChange() {}
</script>
