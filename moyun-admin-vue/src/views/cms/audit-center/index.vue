<template>
  <TabContainer :tabs="tabs" />
</template>

<script setup name="CmsAuditCenter">
import TabContainer from '@/components/TabContainer'
import ArticleAudit from '../article/audit.vue'
import ColumnAudit from '../column/audit.vue'
import TopicAudit from '../topic/audit.vue'
import InterviewExpAudit from '../interview/experience/audit.vue'
import InterviewCommentAudit from '../interview/comment/audit.vue'

/**
 * 全局统一内容审核中心 Tab 容器
 *
 * 设计：统一入口承载系统内所有审核类页面，复用同一套 AuditWorkbench 组件
 * （审核工作台：待审/已办 Tab + 搜索 + 列表 + 详情 + 通过/驳回）。
 *
 * 当前接入审核体系的模块：
 *   1. 文章审核   (cms/article/audit.vue)
 *   2. 专栏审核   (cms/column/audit.vue)
 *   3. 话题审核   (cms/topic/audit.vue)
 *   4. 面经审核   (cms/interview/experience/audit.vue)  —— 新增 v7.13
 *   5. 面试评论审核 (cms/interview/comment/audit.vue)    —— 新增 v7.13
 *
 * 后续新增审核类（如读书空间若接入审核），只需在下方 tabs 数组新增一项即可
 * （遵循 embedded 属性 + AuditWorkbench 范式）。
 */
const tabs = [
  { name: 'article', label: '文章审核', component: ArticleAudit, props: { embedded: true } },
  { name: 'column', label: '专栏审核', component: ColumnAudit, props: { embedded: true } },
  { name: 'topic', label: '话题审核', component: TopicAudit, props: { embedded: true } },
  { name: 'interview-exp', label: '面经审核', component: InterviewExpAudit, props: { embedded: true } },
  { name: 'interview-comment', label: '面试评论审核', component: InterviewCommentAudit, props: { embedded: true } }
]
</script>
