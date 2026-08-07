<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="操作日志" name="operlog">
        <oper-log v-if="loaded.operlog" v-show="activeTab === 'operlog'" />
      </el-tab-pane>
      <el-tab-pane label="登录日志" name="logininfor">
        <login-infor v-if="loaded.logininfor" v-show="activeTab === 'logininfor'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import OperLog from '../operlog/index.vue'
import LoginInfor from '../logininfor/index.vue'

const activeTab = ref('operlog')
const loaded = reactive({ operlog: true, logininfor: false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
