<template>
  <view class="page">
    <view class="tabs">
      <view class="tab" :class="{ active: type === 'expense' }" @tap="type = 'expense'; load()">支出分类</view>
      <view class="tab" :class="{ active: type === 'income' }" @tap="type = 'income'; load()">收入分类</view>
    </view>

    <view class="card">
      <view v-for="c in list" :key="c.id" class="cat-row">
        <view class="cat-dot" :style="{ background: c.color || '#999' }"></view>
        <text class="flex-1">{{ c.name }}<text v-if="c.isSystem === 1" class="sys-tag">系统</text></text>
        <text v-if="c.isSystem !== 1" class="cat-del" @tap="removeCat(c)">删除</text>
      </view>
      <view v-if="list.length === 0" class="empty">暂无分类</view>
    </view>

    <view class="add-bar">
      <input v-model="newName" :placeholder="type === 'expense' ? '新支出分类名' : '新收入分类名'" class="add-input" />
      <view class="add-btn" @tap="addCat">添加</view>
    </view>
  </view>
</template>

<script>
import { listCategories, createCategory, deleteCategory } from '@/api/ledger';

export default {
  data() {
    return {
      type: 'expense',
      list: [],
      newName: ''
    };
  },
  onShow() {
    this.load();
  },
  methods: {
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
.tabs { display: flex; background: #fff; margin-bottom: 24rpx; }
.tab {
  flex: 1; text-align: center; padding: 28rpx 0; font-size: 28rpx; color: #666; position: relative;
}
.tab.active { color: #6a4fd4; font-weight: 600; }
.tab.active::after {
  content: ''; position: absolute; left: 50%; transform: translateX(-50%);
  bottom: 8rpx; width: 48rpx; height: 6rpx; border-radius: 3rpx; background: #6a4fd4;
}
.card { background: #fff; border-radius: 20rpx; padding: 8rpx 32rpx; margin: 0 24rpx; }
.cat-row { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f5f5f7; font-size: 28rpx; }
.cat-row:last-child { border-bottom: none; }
.cat-dot { width: 24rpx; height: 24rpx; border-radius: 12rpx; margin-right: 20rpx; }
.sys-tag { font-size: 20rpx; color: #999; border: 1rpx solid #ddd; border-radius: 8rpx; padding: 2rpx 10rpx; margin-left: 16rpx; }
.cat-del { color: #e74c3c; font-size: 26rpx; padding: 8rpx 16rpx; }
.empty { text-align: center; color: #bbb; padding: 60rpx 0; font-size: 26rpx; }
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
  width: 160rpx; background: #6a4fd4; color: #fff; border-radius: 44rpx;
  text-align: center; line-height: 80rpx; font-size: 28rpx;
}
</style>
