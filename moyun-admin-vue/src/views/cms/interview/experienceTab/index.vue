<template>
  <div class="tab-container-wrapper">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" type="border-card">
      <el-tab-pane label="面经管理" name="experience">
        <experience-panel v-if="loaded.experience" v-show="activeTab === 'experience'" />
      </el-tab-pane>
      <el-tab-pane label="评论管理" name="comment">
        <comment-panel v-if="loaded.comment" v-show="activeTab === 'comment'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="InterviewExperienceTab">
import { ref, reactive } from 'vue'
import ExperiencePanel from '../experience/index.vue'
import CommentPanel from '../comment/index.vue'

const activeTab = ref('experience')
const loaded = reactive({
  experience: true,
  comment: false
})

function handleTabChange(tabName) {
  if (!loaded[tabName]) {
    loaded[tabName] = true
  }
}
</script>

<style scoped>
.tab-container-wrapper {
  /* 不使用 app-container，子页面自带 app-container padding */
}
:deep(.el-tabs--border-card) {
  box-shadow: none;
  border: none;
}
:deep(.el-tabs__content) {
  padding: 0;
}
</style>
