<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="服务器监控" name="server">
        <server-monitor v-if="loaded.server" v-show="activeTab === 'server'" />
      </el-tab-pane>
      <el-tab-pane label="数据监控" name="druid">
        <druid-monitor v-if="loaded.druid" v-show="activeTab === 'druid'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import ServerMonitor from '../server/index.vue'
import DruidMonitor from '../druid/index.vue'

const activeTab = ref('server')
const loaded = reactive({ server: true, druid: false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
