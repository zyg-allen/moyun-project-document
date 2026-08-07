<template>
  <div class="tab-container-wrapper">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" type="border-card">
      <el-tab-pane label="金句摘录" name="quote">
        <quote-panel v-if="loaded.quote" v-show="activeTab === 'quote'" />
      </el-tab-pane>
      <el-tab-pane label="书架管理" name="bookshelf">
        <bookshelf-panel v-if="loaded.bookshelf" v-show="activeTab === 'bookshelf'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="UserContentTab">
import { ref, reactive } from 'vue'
import QuotePanel from '../bookQuote/index.vue'
import BookshelfPanel from '../bookshelf/index.vue'

const activeTab = ref('quote')
const loaded = reactive({
  quote: true,
  bookshelf: false
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
