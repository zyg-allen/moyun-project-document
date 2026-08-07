<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="缓存监控" name="cache">
        <cache-monitor v-if="loaded.cache" v-show="activeTab === 'cache'" />
      </el-tab-pane>
      <el-tab-pane label="缓存列表" name="list">
        <cache-list v-if="loaded.list" v-show="activeTab === 'list'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import CacheMonitor from '../cache/index.vue'
import CacheList from '../cache/list.vue'

const activeTab = ref('cache')
const loaded = reactive({ cache: true, list: false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
