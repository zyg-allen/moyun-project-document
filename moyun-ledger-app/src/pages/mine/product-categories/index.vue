<template>
  <view class="page" :style="themeVars">
    <NavBar title="商品类目" />

    <!-- 顶部：新增一级 -->
    <view class="add-bar">
      <view class="add-icon-btn" @tap="iconPickerOpen = !iconPickerOpen">
        <text class="add-icon">{{ newIcon || '🏷️' }}</text>
      </view>
      <input class="add-input" v-model="newName" placeholder="新商品分类名（图标选填）" />
      <view class="add-btn" :class="{ disabled: submitting }" @tap="addTop">添加</view>
    </view>
    <view v-if="iconPickerOpen" class="icon-panel">
      <view class="icon-grid">
        <view v-for="e in EMOJIS" :key="e" class="icon-cell" :class="{ on: newIcon === e }" @tap="newIcon = newIcon === e ? '' : e">{{ e }}</view>
      </view>
    </view>

    <view v-if="!categories.length" class="empty">还没有商品分类，上面输入名称添加一个</view>

    <!-- 一级列表 -->
    <view v-for="c in categories" :key="c.id" class="cat-card" @tap="openSheet(c)">
      <view class="cat-icon" :style="{ background: softColor(c.color) }">
        <image v-if="isImg(c.icon)" :src="c.icon" class="cat-img" mode="aspectFill" />
        <text v-else>{{ c.icon || '🏷️' }}</text>
      </view>
      <view class="flex-1 min-w-0">
        <view class="cat-name">
          {{ c.name }}
          <text v-if="c.system" class="cat-tag">预设</text>
        </view>
        <view class="cat-sub">{{ c.usedCount || 0 }} 个商品<text v-if="c.children && c.children.length"> · {{ c.children.length }} 个子类</text></view>
      </view>
      <text class="cat-more">{{ (c.children && c.children.length) ? '子类 ▸' : '维护 ▸' }}</text>
    </view>

    <!-- 维护弹层（一级 + 二级） -->
    <view v-if="sheet.visible" class="mask" @tap="closeSheet">
      <view class="sheet" @tap.stop>
        <view class="sheet-actions">
          <text v-if="!sheet.parent.system" class="sheet-act del" @tap.stop="deleteTop">删除</text>
          <text class="sheet-act edit" @tap.stop="editTop">编辑</text>
        </view>
        <view class="sheet-hero">
          <view class="sheet-hero-icon" :style="{ background: softColor(sheet.parent.color) }">
            <image v-if="isImg(sheet.parent.icon)" :src="sheet.parent.icon" class="cat-img" mode="aspectFill" />
            <text v-else>{{ sheet.parent.icon || '🏷️' }}</text>
          </view>
          <text class="sheet-hero-name">{{ sheet.parent.name }}</text>
        </view>

        <view class="sheet-sub-head">
          <text class="sheet-sub-title">二级分类</text>
          <text class="sheet-act edit" @tap.stop="subEditMode = !subEditMode">{{ subEditMode ? '完成' : '编辑' }}</text>
        </view>

        <scroll-view scroll-y class="sheet-sub-body">
          <view class="sub-grid">
            <view v-for="s in subList" :key="s.id" class="sub-cell" @tap="onSubTap(s)">
              <view v-if="subEditMode" class="sub-del" @tap.stop="deleteSub(s)">×</view>
              <view class="sub-icon" :style="{ background: softColor(s.color) }">
                <image v-if="isImg(s.icon)" :src="s.icon" class="sub-img" mode="aspectFill" />
                <text v-else>{{ s.icon || '🏷️' }}</text>
              </view>
              <text class="sub-name">{{ s.name }}</text>
            </view>
            <view class="sub-cell" @tap="addSub">
              <view class="sub-icon add">＋</view>
              <text class="sub-name">添加</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { useThemeStore } from '@/stores/theme';
import NavBar from '@/components/NavBar/NavBar.vue';
import { listProductCategories, createProductCategory, updateProductCategory, deleteProductCategory } from '@/api/ledger';

const EMOJIS = ['🏷️','🍱','🥤','🧴','👕','📱','🧸','🔧','🍬','💊','📦','🎁','🌿','🧊','🍜','🪑'];

export default {
  components: { NavBar },
  data() {
    return {
      categories: [],
      newName: '',
      newIcon: '',
      iconPickerOpen: false,
      submitting: false,
      sheet: { visible: false, parent: {} },
      subEditMode: false,
      EMOJIS
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    subList() {
      const p = this.sheet.parent;
      if (!p || !p.id) return [];
      return p.children || [];
    }
  },
  onShow() {
    useThemeStore().restore();
    this.load();
  },
  methods: {
    isImg(v) { return !!v && (v.indexOf('http') === 0 || v.indexOf('/') === 0 || v.indexOf('data:') === 0); },
    softColor(c) { return c || 'rgba(95,167,122,.12)'; },
    async load() {
      try { this.categories = (await listProductCategories()) || []; } catch (e) { /* 忽略 */ }
      if (this.sheet.visible) {
        const cur = this.categories.find(c => String(c.id) === String(this.sheet.parent.id));
        if (cur) this.sheet.parent = cur;
      }
    },
    async addTop() {
      const name = (this.newName || '').trim();
      if (!name) { uni.showToast({ title: '请输入分类名', icon: 'none' }); return; }
      this.submitting = true;
      try {
        await createProductCategory({ name, parentId: 0, icon: this.newIcon || null });
        this.newName = '';
        this.newIcon = '';
        this.iconPickerOpen = false;
        await this.load();
        uni.showToast({ title: '已添加', icon: 'success' });
      } catch (e) { /* 拦截器已提示 */ }
      this.submitting = false;
    },
    openSheet(c) {
      this.sheet = { visible: true, parent: c };
      this.subEditMode = false;
    },
    closeSheet() { this.sheet.visible = false; this.subEditMode = false; },
    editTop() {
      const p = this.sheet.parent;
      uni.showModal({
        title: '编辑分类名', editable: true, content: p.name,
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) return;
          try {
            await updateProductCategory(p.id, { name });
            p.name = name;
            await this.load();
          } catch (e) { /* 忽略 */ }
        }
      });
    },
    deleteTop() {
      const p = this.sheet.parent;
      uni.showModal({
        title: '删除分类',
        content: '删除「' + p.name + '」？有子类目或商品引用时会被拒绝',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteProductCategory(p.id);
            this.closeSheet();
            await this.load();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 忽略 */ }
        }
      });
    },
    onSubTap(s) { if (this.subEditMode) this.editSub(s); },
    editSub(s) {
      uni.showModal({
        title: '修改二级分类', editable: true, content: s.name,
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) return;
          try {
            await updateProductCategory(s.id, { name });
            await this.load();
          } catch (e) { /* 忽略 */ }
        }
      });
    },
    addSub() {
      const p = this.sheet.parent;
      uni.showModal({
        title: '新增二级分类', editable: true, placeholderText: '如 碳酸饮料',
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) return;
          try {
            await createProductCategory({ name, parentId: p.id });
            await this.load();
            uni.showToast({ title: '已添加', icon: 'success' });
          } catch (e) { /* 忽略 */ }
        }
      });
    },
    deleteSub(s) {
      uni.showModal({
        title: '删除二级分类',
        content: '删除「' + s.name + '」？已被商品引用时会被拒绝',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteProductCategory(s.id);
            await this.load();
          } catch (e) { /* 忽略 */ }
        }
      });
    }
  }
};
</script>

<style scoped>
.page { min-height: 100vh; padding-bottom: 60rpx; }
.add-bar { display: flex; align-items: center; gap: 12rpx; padding: 20rpx 24rpx; background: var(--card); border-bottom: 1rpx solid var(--border); }
.add-icon-btn { width: 72rpx; height: 72rpx; display: flex; align-items: center; justify-content: center; border-radius: 12rpx; border: 1rpx solid var(--border); background: var(--bg); }
.add-icon { font-size: 34rpx; }
.add-input { flex: 1; font-size: 28rpx; color: var(--text-main); }
.add-btn { padding: 14rpx 30rpx; border-radius: 10rpx; background: var(--primary); color: #fff; font-size: 26rpx; }
.add-btn.disabled { opacity: .6; }
.icon-panel { padding: 16rpx 24rpx; background: var(--card); border-bottom: 1rpx solid var(--border); }
.icon-grid { display: flex; flex-wrap: wrap; gap: 12rpx; }
.icon-cell { width: 76rpx; height: 76rpx; display: flex; align-items: center; justify-content: center; font-size: 34rpx; border-radius: 10rpx; border: 1rpx solid var(--border); background: var(--bg); }
.icon-cell.on { border-color: var(--primary); background: rgba(95,167,122,.14); }

.cat-card { display: flex; align-items: center; gap: 16rpx; margin: 14rpx 24rpx 0; padding: 22rpx 20rpx; background: var(--card); border: 1rpx solid var(--border); border-radius: 16rpx; }
.cat-icon { width: 80rpx; height: 80rpx; display: flex; align-items: center; justify-content: center; font-size: 36rpx; border-radius: 14rpx; flex: 0 0 auto; overflow: hidden; }
.cat-img { width: 100%; height: 100%; }
.cat-name { font-size: 28rpx; color: var(--text-main); font-weight: 600; }
.cat-tag { font-size: 20rpx; color: var(--text-sub); border: 1rpx solid var(--border); border-radius: 6rpx; padding: 0 8rpx; margin-left: 8rpx; }
.cat-sub { font-size: 22rpx; color: var(--text-sub); margin-top: 4rpx; }
.cat-more { font-size: 23rpx; color: var(--primary); }
.empty { text-align: center; padding: 100rpx 0; font-size: 26rpx; color: var(--text-sub); }

.mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; }
.sheet { width: 100%; max-height: 76vh; background: var(--card); border-radius: 24rpx 24rpx 0 0; padding: 20rpx 24rpx 40rpx; }
.sheet-actions { display: flex; justify-content: flex-end; gap: 24rpx; padding: 8rpx 0; }
.sheet-act { font-size: 26rpx; color: var(--primary); }
.sheet-act.del { color: var(--expense); }
.sheet-hero { display: flex; flex-direction: column; align-items: center; padding: 6rpx 0 12rpx; }
.sheet-hero-icon { width: 76rpx; height: 76rpx; display: flex; align-items: center; justify-content: center; font-size: 34rpx; border-radius: 16rpx; overflow: hidden; }
.sheet-hero-name { font-size: 26rpx; font-weight: 700; color: var(--text-main); margin-top: 8rpx; }
.sheet-sub-head { display: flex; align-items: center; justify-content: space-between; padding: 12rpx 0; border-top: 1rpx solid var(--border); }
.sheet-sub-title { font-size: 26rpx; color: var(--text-sub); }
.sheet-sub-body { max-height: 46vh; }
.sub-grid { display: flex; flex-wrap: wrap; gap: 16rpx; padding: 8rpx 0; }
.sub-cell { width: 120rpx; display: flex; flex-direction: column; align-items: center; position: relative; }
.sub-icon { width: 88rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; font-size: 36rpx; border-radius: 16rpx; background: var(--bg); overflow: hidden; }
.sub-icon.add { border: 1rpx dashed var(--border); color: var(--text-sub); }
.sub-img { width: 100%; height: 100%; }
.sub-name { font-size: 22rpx; color: var(--text-main); margin-top: 8rpx; text-align: center; }
.sub-del { position: absolute; top: -8rpx; right: 12rpx; width: 34rpx; height: 34rpx; line-height: 30rpx; text-align: center; border-radius: 50%; background: var(--expense); color: var(--on-expense); font-size: 24rpx; }
.flex-1 { flex: 1; }
.min-w-0 { min-width: 0; }
/* ===== 排版统一 ===== */
.cat-name { font-size: var(--fs-subtitle); font-weight: 600; }
.cat-sub, .cat-tag, .sub-name { font-size: var(--fs-unit); color: var(--text-sub); }
.sheet-title, .sheet-hero-name, .sheet-sub-title { font-size: var(--fs-title); font-weight: 700; }
.cat-more, .sheet-act { font-size: var(--fs-sub); }
</style>
