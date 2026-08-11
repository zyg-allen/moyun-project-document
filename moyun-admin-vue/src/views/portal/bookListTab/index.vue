<template>
  <TabContainer :tabs="tabs" variant="borderless" />
</template>

<script setup name="BookListTab">
import TabContainer from '@/components/TabContainer'
import BookListPanel from '../bookList/index.vue'
import BookRecommendPanel from '../bookRecommend/index.vue'

const tabs = [
  { name: 'bookList', label: '书单管理', component: BookListPanel },
  { name: 'bookRecommend', label: '推荐位管理', component: BookRecommendPanel }
]
</script>
