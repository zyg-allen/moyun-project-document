<template>
  <div :class="wrapperClass">
    <el-tabs v-model="activeTab" :type="tabType" @tab-change="handleTabChange">
      <el-tab-pane
        v-for="tab in tabs"
        :key="tab.name"
        :label="tab.label"
        :name="tab.name"
      >
        <component
          :is="tab.component"
          v-if="loaded[tab.name]"
          v-show="activeTab === tab.name"
          v-bind="tab.props"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
/**
 * 通用 Tab 容器基类
 *
 * 用途：将分散在 cms/monitor/portal 等模块下的 Tab 容器页面（约 11 个）
 *      的重复结构（el-tabs + 懒加载 + activeTab 状态）抽取为一个组件，
 *      调用方只需传入 tabs 配置数组即可。
 *
 * 支持两种样式变体：
 *   - variant="default"   （默认）：app-container 包裹，无边框
 *   - variant="borderless"        ：tab-container-wrapper 包裹，type="border-card"，
 *                                  去 box-shadow/border/content padding（适用于子页面
 *                                  自带 app-container padding 的场景）
 *
 * 用法示例：
 *   <TabContainer :tabs="tabs" />
 *
 *   const tabs = [
 *     { name: 'rule', label: '成长规则', component: GrowthRule },
 *     { name: 'achievement', label: '成就管理', component: GrowthAchievement }
 *   ]
 *
 * 若子组件需接收 props（如审核中心的 :embedded="true"）：
 *   { name: 'article', label: '文章审核', component: ArticleAudit, props: { embedded: true } }
 */
import { ref, reactive, computed } from 'vue'

const props = defineProps({
  // Tab 配置数组：[{ name: string, label: string, component: Component, props?: object }]
  tabs: {
    type: Array,
    required: true
  },
  // 当前激活的 Tab 名称（支持 v-model，可选）
  modelValue: {
    type: String,
    default: ''
  },
  // 样式变体：'default' | 'borderless'
  variant: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['update:modelValue', 'tab-change'])

const isBorderless = computed(() => props.variant === 'borderless')
const wrapperClass = computed(() =>
  isBorderless.value ? 'tab-container-wrapper' : 'app-container'
)
const tabType = computed(() => (isBorderless.value ? 'border-card' : ''))

// 默认激活第一个 Tab；若传入 modelValue 则优先使用
const activeTab = ref(
  props.modelValue || (props.tabs.length ? props.tabs[0].name : '')
)

// 懒加载状态：默认第一个 Tab 已加载，其余 Tab 首次切换时挂载
const loaded = reactive(
  props.tabs.reduce((acc, tab, idx) => {
    acc[tab.name] = idx === 0
    return acc
  }, {})
)

function handleTabChange(tabName) {
  if (!loaded[tabName]) {
    loaded[tabName] = true
  }
  emit('update:modelValue', tabName)
  emit('tab-change', tabName)
}
</script>

<style scoped>
/* borderless 变体专用样式：去 box-shadow / border / content padding */
.tab-container-wrapper {
  /* 不使用 app-container，子页面自带 app-container padding */
}
.tab-container-wrapper :deep(.el-tabs--border-card) {
  box-shadow: none;
  border: none;
}
.tab-container-wrapper :deep(.el-tabs__content) {
  padding: 0;
}
</style>
