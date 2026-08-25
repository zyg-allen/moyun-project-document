<template>
  <TabContainer :tabs="tabs" variant="borderless" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import HelpCategory from '../help-category/index.vue'
import HelpArticle from '../help-article/index.vue'

const tabs = [
  { name: 'category', label: '帮助分类', component: HelpCategory },
  { name: 'article', label: '帮助文章', component: HelpArticle }
]
</script>
