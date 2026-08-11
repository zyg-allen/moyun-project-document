<template>
  <TabContainer :tabs="tabs" variant="borderless" />
</template>

<script setup name="UserContentTab">
import TabContainer from '@/components/TabContainer'
import QuotePanel from '../bookQuote/index.vue'
import BookshelfPanel from '../bookshelf/index.vue'

const tabs = [
  { name: 'quote', label: '金句摘录', component: QuotePanel },
  { name: 'bookshelf', label: '书架管理', component: BookshelfPanel }
]
</script>
