<template>
  <TabContainer :tabs="tabs" variant="borderless" />
</template>

<script setup>
/**
 * 文章管理（合并入口）
 *
 * v8.1：将原「文章管理」和「专栏管理」合并为同一菜单下的 Tab。
 * 文章 Tab 为默认（"以文章为主"），专栏 Tab 复用原 cms/column/index.vue。
 * 原专栏管理菜单（/portal/cms/column）按项目铁律改为隐藏，路由仍保留，
 * 用户从文章管理页的 Tab 进入专栏管理。
 */
import TabContainer from '@/components/TabContainer'
import ArticleListTab from './components/ArticleListTab.vue'
import CmsColumn from '../column/index.vue'

const tabs = [
  { name: 'article', label: '文章管理', component: ArticleListTab },
  { name: 'column', label: '专栏管理', component: CmsColumn }
]
</script>
