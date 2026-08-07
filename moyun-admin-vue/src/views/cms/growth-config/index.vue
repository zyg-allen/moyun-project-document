<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="成长规则" name="rule">
        <growth-rule v-if="loaded.rule" v-show="activeTab === 'rule'" />
      </el-tab-pane>
      <el-tab-pane label="成就管理" name="achievement">
        <growth-achievement v-if="loaded.achievement" v-show="activeTab === 'achievement'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import GrowthRule from '../growth/rule/index.vue'
import GrowthAchievement from '../growth/achievement/index.vue'

const activeTab = ref('rule')
const loaded = reactive({ rule: true, achievement: false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
