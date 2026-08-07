<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="广告位" name="ad">
        <ad-manage v-if="loaded.ad" v-show="activeTab === 'ad'" />
      </el-tab-pane>
      <el-tab-pane label="友情链接" name="friend-link">
        <friend-link v-if="loaded['friend-link']" v-show="activeTab === 'friend-link'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import AdManage from '../ad/index.vue'
import FriendLink from '../friend-link/index.vue'

const activeTab = ref('ad')
const loaded = reactive({ ad: true, 'friend-link': false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
