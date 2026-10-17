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
              <!-- 自定义分类：右上角删除 ×（仅图标，不占文字位） -->
              <text v-if="c.isSystem !== 1" class="cat-remove" @tap.stop="removeCat(c)">×</text>
              <view class="cat-icon" :style="{ background: softColor(c.color) }">
                <text>{{ iconOf(c.icon) }}</text>
              </view>
              <text class="cat-name">{{ c.name }}</text>
              <!-- 子分类：点开弹框维护（新增/修改/删除） -->
              <view class="sub-bar">
                <text class="sub-toggle" @tap.stop="openSubSheet(c)">
                  {{ subOf(c).length ? subOf(c).length + ' 个子类 ▸' : '子分类 ▸' }}
                </text>
              </view>
            </view>
          </view>
          <view v-if="g.items.length === 0" class="empty">暂无分类</view>
        </view>
      </view>
      <view v-if="grouped.length === 0" class="empty" style="padding:80rpx 0">暂无分类</view>
    </view>

    <!-- 添加自定义（图标选填） -->
    <view class="add-bar" :style="kbBottomStyle">
      <view class="add-row">
        <!-- 图标按钮：显示当前选中图标，点击展开选择器 -->
        <view class="add-icon-btn" :class="{ on: iconPickerOpen }" @tap="iconPickerOpen = !iconPickerOpen">
          <text class="add-icon-emoji">{{ iconOf(newIcon) }}</text>
        </view>
        <input
          v-model="newName"
          :placeholder="'新' + currentTabLabel + '分类名（图标选填）'"
          class="add-input"
          :cursor-spacing="24"
          adjust-position
        />
        <view class="add-btn" :class="{ disabled: submitting }" @tap="!submitting && addCat()">添加</view>
      </view>
      <!-- 图标选择面板（折叠，默认收起，不点不占位） -->
      <view v-if="iconPickerOpen" class="icon-panel">
        <view class="icon-panel-title">选择图标（选填，不选则用默认标签）</view>
        <scroll-view class="icon-scroll" scroll-y>
          <view class="icon-grid">
            <view
              v-for="(emoji, key) in iconOptions"
              :key="key"
              class="icon-cell"
              :class="{ on: newIcon === key }"
              @tap="pickIcon(key)"
            >
              <text>{{ emoji }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- ===== 子分类维护弹框 ===== -->
    <view v-if="subSheet.visible" class="sheet-mask" @tap="closeSubSheet">
      <view class="sheet" @tap.stop>
        <view class="sheet-head">
          <text class="sheet-title">「{{ subSheet.parent.name }}」的子分类</text>
          <text class="sheet-close" @tap="closeSubSheet">×</text>
        </view>

        <scroll-view class="sheet-body" scroll-y>
          <view v-if="!subSheet.list.length" class="sheet-empty">还没有子分类，在下方输入名称添加</view>
          <view v-for="s in subSheet.list" :key="s.id" class="sub-row">
            <text class="sub-row-name">{{ s.name }}</text>
            <text class="sub-row-act" @tap.stop="editSub(s)">改</text>
            <text class="sub-row-act del" @tap.stop="deleteSub(s)">删</text>
          </view>
        </scroll-view>

        <view class="sheet-add">
          <input v-model="subSheet.newName" placeholder="新子分类名称" class="sheet-input"
            :cursor-spacing="24" adjust-position @confirm="addSub" />
          <view class="sheet-add-btn" @tap="addSub">添加</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import submitLock from '@/mixins/submitLock';
import keyboardAware from '@/mixins/keyboardAware';
import { listCategories, createCategory, deleteCategory } from '@/api/ledger';
import { useThemeStore } from '@/stores/theme';
import { categoryIcon, softColor as toSoftColor, ICON_MAP } from '@/utils/categoryIcon';

const TYPE_TABS = [
  { key: 'expense',   label: '支出' },
  { key: 'income',    label: '收入' },
  { key: 'transfer',  label: '转账' },
  { key: 'repayment', label: '还款' },
  { key: 'borrow',    label: '借款' },
  { key: 'adjust',    label: '校准' }
];

export default {
  mixins: [keyboardAware, submitLock],
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    currentTabLabel() {
      const t = TYPE_TABS.find(x => x.key === this.type);
      return t ? t.label : '';
    },
    /** 按 group_name 语义分组（只列一级，二级挂在对应一级下） */
    grouped() {
      if (!this.list.length) return [];
      const map = new Map();
      const top = this.list.filter(c => !c.parentId || Number(c.parentId) === 0);
      for (const c of top) {
        const g = c.groupName || '其他';
        if (!map.has(g)) map.set(g, []);
        map.get(g).push(c);
      }
      // 保留 DB sort_order 隐含的分组顺序（取每组第一条的 sortOrder）
      return Array.from(map.entries())
        .map(([name, items]) => ({ name, items, order: items[0].sortOrder || 0 }))
        .sort((a, b) => a.order - b.order);
    },
    /** 可选图标（来自统一图标映射，保证选择后能正确渲染） */
    iconOptions() { return ICON_MAP; }
  },
  data() {
    return {
      typeTabs: TYPE_TABS,
      type: 'expense',
      list: [],
      newName: '',
      newIcon: '',        // 选填；空则后端用默认图标
      iconPickerOpen: false,
      // 子分类维护弹框：parent=当前一级分类，list=其子分类
      subSheet: { visible: false, parent: {}, list: [], newName: '' },
    };
  },
  onShow() {
    // uni-app H5 每个页面各自新建 uni-page-body：必须每次进入都重新注入主题，
    // 否则从别的页跳进来时，本页最外层仍是 page{} 的日间默认（暗色下发白）。
    useThemeStore().applyToPageBody();
    this.load();
  },

  methods: {
    iconOf(icon) { return categoryIcon(icon); },
    /** 某一级分类的子分类（模板调用 subOf(c)，必须是 methods 才能带参） */
    subOf(parent) {
      return this.list.filter(c => String(c.parentId) === String(parent.id));
    },
    /** 打开子分类维护弹框 */
    openSubSheet(c) {
      this.subSheet = { visible: true, parent: c, list: this.subOf(c), newName: '' };
    },
    closeSubSheet() {
      this.subSheet.visible = false;
    },
    /** 刷新弹框内的子分类列表（增删改后调用） */
    async refreshSubSheet() {
      await this.load();
      this.subSheet.list = this.subOf(this.subSheet.parent);
    },
    /** 弹框内新增子分类 */
    async addSub() {
      const name = (this.subSheet.newName || '').trim();
      if (!name) {
        uni.showToast({ title: '请输入子分类名称', icon: 'none' });
        return;
      }
      try {
        await createCategory({ name, type: this.type, parentId: this.subSheet.parent.id });
        this.subSheet.newName = '';
        await this.refreshSubSheet();
        uni.showToast({ title: '已添加', icon: 'success' });
      } catch (e) { /* 拦截器已提示 */ }
    },
    /** 弹框内修改子分类（弹输入框） */
    editSub(s) {
      uni.showModal({
        title: '修改子分类',
        editable: true,
        content: s.name,
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) {
            uni.showToast({ title: '名称不能为空', icon: 'none' });
            return;
          }
          try {
            await updateCategory(s.id, { name, type: this.type, parentId: this.subSheet.parent.id });
            await this.refreshSubSheet();
            uni.showToast({ title: '已修改', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 弹框内删除子分类（后端校验流水/子分类引用） */
    deleteSub(s) {
      uni.showModal({
        title: '删除确认',
        content: '删除「' + s.name + '」？已被流水引用的分类不能删除',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteCategory(s.id);
            await this.refreshSubSheet();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 选择/取消选择图标（再点一次取消，保持"选填"语义） */
    pickIcon(key) {
      this.newIcon = this.newIcon === key ? '' : key;
    },
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
      return this.withSubmitLock(async () => {
              if (!this.newName.trim()) {
                uni.showToast({ title: '请输入分类名', icon: 'none' });
                return;
              }
              try {
                const payload = { name: this.newName.trim(), type: this.type };
                // 图标选填：未选则不传，由后端保持默认
                if (this.newIcon) payload.icon = this.newIcon;
                await createCategory(payload);
                this.newName = '';
                this.newIcon = '';
                this.iconPickerOpen = false;
                this.load();
              } catch (e) { /* 拦截器已提示 */ }
      });
    },
    removeCat(c) {
      uni.showModal({
        title: '删除确认',
        content: `删除自定义分类「${c.name}」？已绑定流水的分类不能删除`,
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
.tabs { background: var(--card); white-space: nowrap; margin-bottom: 16rpx; }
.tabs-inner { display: inline-flex; padding: 0 12rpx; }
.tab {
  text-align: center; padding: 24rpx 28rpx; font-size: 28rpx; color: var(--text-sub); position: relative;
  white-space: nowrap;
}
.tab.active { color: var(--primary-text); font-weight: 600; }
.tab.active::after {
  content: ''; position: absolute; left: 50%; transform: translateX(-50%);
  bottom: 8rpx; width: 48rpx; height: 6rpx; border-radius: 3rpx; background: var(--primary-strong);
}

/* 分组 */
.content { padding: 0 24rpx; }
.group { margin-bottom: 24rpx; }
.group-title {
  font-size: 24rpx; font-weight: 600; color: var(--text-sub); padding: 12rpx 8rpx;
}
.group-card { background: var(--card); border-radius: 20rpx; padding: 20rpx 12rpx; }
.cat-grid { display: flex; flex-wrap: wrap; align-content: flex-start; }
.cat-cell {
  position: relative;   /* 供右上角删除 × 定位 */
  width: 25%; box-sizing: border-box;
  display: flex; flex-direction: column; align-items: center;
  padding: 12rpx 0;
}
.cat-icon {
  width: 72rpx; height: 72rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 34rpx;
}
.cat-name { font-size: 22rpx; color: var(--text-sub); margin-top: 8rpx; }
/* 二级分类 */
/* 子分类：常驻只占一行（数量 + 加号），点开才展示明细 */
.sub-bar {
  display: flex; align-items: center; justify-content: center;
  gap: 8rpx; margin-top: 6rpx; width: 100%;
}
.sub-toggle { font-size: 18rpx; color: var(--text-muted); }
.sub-arrow { font-size: 16rpx; color: var(--text-muted); }
.sub-tags { display: flex; flex-wrap: wrap; justify-content: center; gap: 6rpx; margin-top: 8rpx; }
.sub-tag {
  font-size: 18rpx; color: var(--text-muted); background: var(--surface);
  padding: 2rpx 10rpx; border-radius: 12rpx; max-width: 120rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.cat-remove {
  position: absolute; top: 4rpx; right: 12rpx;
  width: 36rpx; height: 36rpx; line-height: 32rpx; text-align: center;
  border-radius: 50%; font-size: 26rpx; color: #fff;
  background: rgba(0, 0, 0, 0.28);
}
.cat-remove:active { background: var(--expense); color: var(--on-expense); }

.add-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); background: var(--card);
  border-top: 1rpx solid var(--border);
  display: flex; flex-direction: column;
}
.add-row { display: flex; align-items: center; }
/* 图标按钮（选填入口）：默认显示"默认标签"图标 */
.add-icon-btn {
  width: 80rpx; height: 80rpx; flex-shrink: 0; margin-right: 16rpx;
  border-radius: 20rpx; background: var(--surface, #f5f6f8);
  display: flex; align-items: center; justify-content: center;
  border: 1rpx solid transparent;
}
.add-icon-btn.on { border-color: var(--primary-strong); background: var(--primary-soft, #eef2ff); }
.add-icon-emoji { font-size: 36rpx; }
.add-input {
  flex: 1; background: var(--surface); border-radius: 44rpx; height: 80rpx;
  padding: 0 32rpx; font-size: 28rpx; margin-right: 20rpx;
}
.add-btn {
  width: 160rpx; flex-shrink: 0; background: var(--primary-strong); color: var(--on-primary); border-radius: 44rpx;
  text-align: center; line-height: 80rpx; font-size: 28rpx;
}
/* 图标选择面板（折叠，默认收起） */
.icon-panel { margin-top: 20rpx; }
.icon-panel-title { font-size: 22rpx; color: var(--text-sub); margin-bottom: 12rpx; }
.icon-scroll { max-height: 320rpx; }
.icon-grid { display: flex; flex-wrap: wrap; }
.icon-cell {
  width: 12.5%; box-sizing: border-box; padding: 12rpx 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 36rpx; border-radius: 12rpx;
}
.icon-cell.on { background: var(--primary-soft, #eef2ff); }
.add-input {
  flex: 1; background: var(--surface); border-radius: 44rpx; height: 80rpx;
  padding: 0 32rpx; font-size: 28rpx; margin-right: 20rpx;
}
/* 提交锁生效时给出视觉反馈（与全局 .btn-primary.disabled 一致） */
.add-btn.disabled { opacity: 0.5; }
/* ===== 子分类维护弹框 ===== */
.sheet-mask {
  position: fixed; left: 0; right: 0; top: 0; bottom: 0;
  background: rgba(0,0,0,.45); z-index: 900;
  display: flex; align-items: flex-end;
}
.sheet {
  width: 100%; background: var(--card);
  border-radius: 28rpx 28rpx 0 0;
  padding: 28rpx 24rpx calc(24rpx + env(safe-area-inset-bottom));
  max-height: 72vh; display: flex; flex-direction: column;
}
.sheet-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.sheet-title { font-size: 30rpx; font-weight: 600; color: var(--text-main); }
.sheet-close { font-size: 40rpx; color: var(--text-muted); padding: 0 8rpx; line-height: 1; }
.sheet-body { flex: 1; max-height: 48vh; }
.sheet-empty { text-align: center; color: var(--text-muted); font-size: 24rpx; padding: 40rpx 0; }
.sub-row {
  display: flex; align-items: center; gap: 16rpx;
  padding: 20rpx 16rpx; border-bottom: 1rpx solid var(--border);
}
.sub-row-name { flex: 1; font-size: 28rpx; color: var(--text-main); }
.sub-row-act {
  font-size: 24rpx; color: var(--primary); padding: 4rpx 16rpx;
  border: 1rpx solid var(--primary); border-radius: 20rpx;
}
.sub-row-act.del { color: #E85D5D; border-color: #E85D5D; }
.sheet-add { display: flex; align-items: center; gap: 16rpx; margin-top: 16rpx; }
.sheet-input {
  flex: 1; height: 76rpx; background: var(--surface);
  border-radius: 40rpx; padding: 0 28rpx; font-size: 26rpx;
}
.sheet-add-btn {
  height: 76rpx; line-height: 76rpx; padding: 0 36rpx;
  background: var(--primary-strong); color: var(--on-primary);
  border-radius: 40rpx; font-size: 26rpx;
}
</style>
