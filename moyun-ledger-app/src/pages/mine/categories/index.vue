<template>
  <view class="page" :style="themeVars">
    <NavBar title="分类管理" />

    <!-- 类型 Tab（横滑 6 类） -->
    <scroll-view scroll-x class="tabs" show-scrollbar="false">
      <view class="tabs-inner">
        <view v-for="t in typeTabs" :key="t.key" class="tab"
              :class="{ active: type === t.key }" @tap="switchType(t.key)">
          {{ t.label }}
        </view>
      </view>
    </scroll-view>

    <!-- 按语义分组展示 -->
    <view class="content">
      <view v-for="g in grouped" :key="g.name" class="group">
        <view class="group-title">{{ g.name }}</view>
        <view class="group-card">
          <view class="cat-grid">
            <view v-for="c in g.items" :key="c.id" class="cat-cell">
              <view class="cat-icon" :style="{ background: softColor(c.color) }">
                <text>{{ iconOf(c.icon) }}</text>
              </view>
              <text class="cat-name">{{ c.name }}</text>
              <text v-if="c.isSystem === 1" class="sys-tag">系统</text>
              <text v-else class="cat-del" @tap="removeCat(c)">删除</text>
            </view>
          </view>
          <view v-if="g.items.length === 0" class="empty">暂无分类</view>
        </view>
      </view>
      <view v-if="grouped.length === 0" class="empty" style="padding:80rpx 0">暂无分类</view>
    </view>

    <!-- 添加自定义 -->
    <view class="add-bar">
      <input v-model="newName" :placeholder="'新' + currentTabLabel + '分类名'" class="add-input" />
      <view class="add-btn" @tap="addCat">添加</view>
    </view>
  </view>
</template>

<script>
import { listCategories, createCategory, deleteCategory } from '@/api/ledger';
import { useThemeStore } from '@/stores/theme';
import { categoryIcon, softColor as toSoftColor } from '@/utils/categoryIcon';

const TYPE_TABS = [
  { key: 'expense',   label: '支出' },
  { key: 'income',    label: '收入' },
  { key: 'transfer',  label: '转账' },
  { key: 'repayment', label: '还款' },
  { key: 'borrow',    label: '借款' },
  { key: 'adjust',    label: '校准' }
];

export default {
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    currentTabLabel() {
      const t = TYPE_TABS.find(x => x.key === this.type);
      return t ? t.label : '';
    },
    /** 按 group_name 语义分组 */
    grouped() {
      if (!this.list.length) return [];
      const map = new Map();
      for (const c of this.list) {
        const g = c.groupName || '其他';
        if (!map.has(g)) map.set(g, []);
        map.get(g).push(c);
      }
      // 保留 DB sort_order 隐含的分组顺序（取每组第一条的 sortOrder）
      return Array.from(map.entries())
        .map(([name, items]) => ({ name, items, order: items[0].sortOrder || 0 }))
        .sort((a, b) => a.order - b.order);
    }
  },
  data() {
    return {
      typeTabs: TYPE_TABS,
      type: 'expense',
      list: [],
      newName: ''
    };
  },
  onShow() {
    this.load();
  },
  methods: {
    iconOf(icon) { return categoryIcon(icon); },
    softColor(color) { return toSoftColor(color); },
    switchType(key) {
      this.type = key;
      this.load();
    },
    async load() {
      try {
        const res = await listCategories(this.type);
        this.list = (res && res.records) || [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    async addCat() {
      if (!this.newName.trim()) {
        uni.showToast({ title: '请输入分类名', icon: 'none' });
        return;
      }
      try {
        await createCategory({ name: this.newName.trim(), type: this.type });
        this.newName = '';
        this.load();
      } catch (e) { /* 拦截器已提示 */ }
    },
    removeCat(c) {
      uni.showModal({
        title: '删除确认',
        content: `删除自定义分类「${c.name}」？已用该分类的流水不受影响`,
        success: async (r) => {
          if (r.confirm) {
            await deleteCategory(c.id);
            this.load();
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; }

/* 类型 Tab（横滑） */
.tabs { background: #fff; white-space: nowrap; margin-bottom: 16rpx; }
.tabs-inner { display: inline-flex; padding: 0 12rpx; }
.tab {
  text-align: center; padding: 24rpx 28rpx; font-size: 28rpx; color: #666; position: relative;
  white-space: nowrap;
}
.tab.active { color: var(--primary-strong); font-weight: 600; }
.tab.active::after {
  content: ''; position: absolute; left: 50%; transform: translateX(-50%);
  bottom: 8rpx; width: 48rpx; height: 6rpx; border-radius: 3rpx; background: var(--primary-strong);
}

/* 分组 */
.content { padding: 0 24rpx; }
.group { margin-bottom: 24rpx; }
.group-title {
  font-size: 24rpx; font-weight: 600; color: #999; padding: 12rpx 8rpx;
}
.group-card { background: #fff; border-radius: 20rpx; padding: 20rpx 12rpx; }
.cat-grid { display: flex; flex-wrap: wrap; align-content: flex-start; }
.cat-cell {
  width: 25%; box-sizing: border-box;
  display: flex; flex-direction: column; align-items: center;
  padding: 20rpx 0;
}
.cat-icon {
  width: 80rpx; height: 80rpx; border-radius: 20rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 38rpx;
}
.cat-name { font-size: 24rpx; color: #555; margin-top: 12rpx; }
.sys-tag { font-size: 18rpx; color: #999; border: 1rpx solid #ddd; border-radius: 8rpx; padding: 2rpx 10rpx; margin-top: 6rpx; }
.cat-del { color: #e74c3c; font-size: 22rpx; margin-top: 6rpx; padding: 4rpx 16rpx; }
.empty { text-align: center; color: #bbb; padding: 40rpx 0; font-size: 26rpx; }

.add-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  display: flex; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); background: #fff;
  border-top: 1rpx solid #f0f0f5;
}
.add-input {
  flex: 1; background: #f5f6f8; border-radius: 44rpx; height: 80rpx;
  padding: 0 32rpx; font-size: 28rpx; margin-right: 20rpx;
}
.add-btn {
  width: 160rpx; background: var(--primary-strong); color: #fff; border-radius: 44rpx;
  text-align: center; line-height: 80rpx; font-size: 28rpx;
}
</style>