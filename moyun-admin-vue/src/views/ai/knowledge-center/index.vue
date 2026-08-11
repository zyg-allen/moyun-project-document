<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup>
import TabContainer from '@/components/TabContainer'
import KnowledgeBase from '../knowledge-base/index.vue'
import KnowledgeLibrary from '../knowledge-library/index.vue'

const tabs = [
  { name: 'knowledge-base', label: '知识库管理', component: KnowledgeBase, props: { embedded: true } },
  { name: 'knowledge-library', label: '知识文库', component: KnowledgeLibrary, props: { embedded: true } }
]
</script>
