<template>
  <div class="tab-container-wrapper">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" type="border-card">
      <el-tab-pane label="书单管理" name="bookList">
        <book-list-panel v-if="loaded.bookList" v-show="activeTab === 'bookList'" />
      </el-tab-pane>
      <el-tab-pane label="推荐位管理" name="bookRecommend">
        <book-recommend-panel v-if="loaded.bookRecommend" v-show="activeTab === 'bookRecommend'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="BookListTab">
import { ref, reactive } from 'vue'
import BookListPanel from '../bookList/index.vue'
import BookRecommendPanel from '../bookRecommend/index.vue'

const activeTab = ref('bookList')
const loaded = reactive({
  bookList: true,
  bookRecommend: false
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
