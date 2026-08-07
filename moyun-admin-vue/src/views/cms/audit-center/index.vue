<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="文章审核" name="article">
        <article-audit v-if="loaded.article" v-show="activeTab === 'article'" :embedded="true" />
      </el-tab-pane>
      <el-tab-pane label="专栏审核" name="column">
        <column-audit v-if="loaded.column" v-show="activeTab === 'column'" :embedded="true" />
      </el-tab-pane>
      <el-tab-pane label="话题审核" name="topic">
        <topic-audit v-if="loaded.topic" v-show="activeTab === 'topic'" :embedded="true" />
      </el-tab-pane>
      <el-tab-pane label="面经审核" name="interview-exp">
        <interview-exp-audit v-if="loaded['interview-exp']" v-show="activeTab === 'interview-exp'" :embedded="true" />
      </el-tab-pane>
      <el-tab-pane label="面试评论审核" name="interview-comment">
        <interview-comment-audit v-if="loaded['interview-comment']" v-show="activeTab === 'interview-comment'" :embedded="true" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="CmsAuditCenter">
import { ref, reactive } from 'vue';
import ArticleAudit from '../article/audit.vue';
import ColumnAudit from '../column/audit.vue';
import TopicAudit from '../topic/audit.vue';
import InterviewExpAudit from '../interview/experience/audit.vue';
import InterviewCommentAudit from '../interview/comment/audit.vue';

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
 * 后续新增审核类（如读书空间若接入审核），只需在此处新增 Tab 并
 * 引入对应 audit.vue 即可（遵循 embedded 属性 + AuditWorkbench 范式）。
 */
const activeTab = ref('article');
const loaded = reactive({
  article: true,
  column: false,
  topic: false,
  'interview-exp': false,
  'interview-comment': false
});

function handleTabChange(tabName) {
  // 懒加载：首次切换才挂载，保留各 Tab 独立状态
  if (!loaded[tabName]) {
    loaded[tabName] = true;
  }
}
</script>
