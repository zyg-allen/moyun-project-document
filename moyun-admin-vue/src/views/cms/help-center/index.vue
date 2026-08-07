<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="帮助分类" name="category">
        <help-category v-if="loaded.category" v-show="activeTab === 'category'" />
      </el-tab-pane>
      <el-tab-pane label="帮助文章" name="article">
        <help-article v-if="loaded.article" v-show="activeTab === 'article'" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import HelpCategory from '../help-category/index.vue'
import HelpArticle from '../help-article/index.vue'

const activeTab = ref('category')
const loaded = reactive({ category: true, article: false })

function handleTabChange(tabName) {
  loaded[tabName] = true
}
</script>
