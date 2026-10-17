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
                <image v-if="isImg(c.icon)" :src="c.icon" class="cat-img" mode="aspectFill" />
                <text v-else>{{ iconOf(c.icon) }}</text>
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
          <image v-if="isImg(newIcon)" :src="newIcon" class="add-icon-img" mode="aspectFill" />
          <text v-else class="add-icon-emoji">{{ iconOf(newIcon) }}</text>
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
        <view class="icon-panel-title">选择图标（选填，可选 emoji 或上传图片）</view>
        <view class="icon-upload-row">
          <view class="icon-upload-btn" @tap="uploadIcon">📷 上传图片图标</view>
        </view>
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
        <!-- 一级分类操作（系统预设不可删） -->
        <view class="sheet-actions">
          <text v-if="Number(subSheet.parent.isSystem) === 0" class="sheet-act del"
            @tap.stop="deleteParent">删除</text>
          <text class="sheet-act edit" @tap.stop="editParent">编辑</text>
        </view>

        <!-- 一级分类信息 -->
        <view class="sheet-hero">
          <view class="sheet-hero-icon" :style="{ background: softColor(subSheet.parent.color) }">
            <text>{{ iconOf(subSheet.parent.icon) }}</text>
          </view>
          <text class="sheet-hero-name">{{ subSheet.parent.name }}</text>
        </view>

        <!-- 二级分类 -->
        <view class="sheet-sub-head">
          <text class="sheet-sub-title">二级分类</text>
          <text class="sheet-act edit" @tap.stop="subEditMode = !subEditMode">
            {{ subEditMode ? '完成' : '编辑' }}
          </text>
        </view>

        <scroll-view scroll-y class="sheet-sub-body">
          <view class="sheet-sub-grid">
            <view v-for="s in subSheet.list" :key="s.id" class="sheet-sub-cell" @tap="onSubTap(s)">
              <view v-if="subEditMode" class="sheet-sub-del" @tap.stop="deleteSub(s)">×</view>
              <view class="sheet-sub-icon" :style="{ background: softColor(s.color) }">
                <image v-if="isImg(s.icon)" :src="s.icon" class="sub-img" mode="aspectFill" />
                <text v-else>{{ iconOf(s.icon) }}</text>
              </view>
              <text class="sheet-sub-name">{{ s.name }}</text>
            </view>
            <view class="sheet-sub-cell" @tap="addSubPrompt">
              <view class="sheet-sub-icon add">＋</view>
              <text class="sheet-sub-name">添加</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import submitLock from '@/mixins/submitLock';
import keyboardAware from '@/mixins/keyboardAware';
import { listCategories, createCategory, updateCategory, deleteCategory, uploadCategoryIcon } from '@/api/ledger';
import { useThemeStore } from '@/stores/theme';
import { categoryIcon, softColor as toSoftColor, isImageIcon, ICON_MAP } from '@/utils/categoryIcon';

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
      subEditMode: false,   // 二级分类编辑态（显示删除×，点击改名）
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
    /** icon 是否为图片地址 */
    isImg(icon) { return isImageIcon(icon); },
    /** 上传自定义图标图片（选填，成功后写入 newIcon） */
    uploadIcon() {
      uni.chooseImage({
        count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'],
        success: async (res) => {
          try {
            uni.showLoading({ title: '上传中' });
            this.newIcon = await uploadCategoryIcon(res.tempFilePaths[0]);
            uni.hideLoading();
            uni.showToast({ title: '图片已选择', icon: 'success' });
          } catch (e) { uni.hideLoading(); }
        }
      });
    },
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
    /** 弹框内新增子分类（弹输入框） */
    addSubPrompt() {
      uni.showModal({
        title: '新增子分类',
        editable: true,
        placeholderText: '子分类名称',
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) {
            uni.showToast({ title: '名称不能为空', icon: 'none' });
            return;
          }
          try {
            await createCategory({ name, type: this.type, parentId: this.subSheet.parent.id });
            await this.refreshSubSheet();
            uni.showToast({ title: '已添加', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 点击二级分类：编辑态=改名；普通态给提示 */
    onSubTap(s) {
      if (this.subEditMode) {
        this.editSub(s);
        return;
      }
      uni.showToast({ title: '点右上「编辑」可修改/删除', icon: 'none' });
    },
    /** 删除当前一级分类（系统预设不可删，由后端兜底） */
    deleteParent() {
      const p = this.subSheet.parent;
      uni.showModal({
        title: '删除确认',
        content: '删除「' + p.name + '」？已被流水或子分类引用的不能删除',
        success: async (r) => {
          if (!r.confirm) return;
          try {
            await deleteCategory(p.id);
            this.closeSubSheet();
            await this.load();
            uni.showToast({ title: '已删除', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
    },
    /** 重命名当前一级分类 */
    editParent() {
      const p = this.subSheet.parent;
      uni.showModal({
        title: '编辑分类名',
        editable: true,
        content: p.name,
        success: async (r) => {
          if (!r.confirm) return;
          const name = (r.content || '').trim();
          if (!name) {
            uni.showToast({ title: '名称不能为空', icon: 'none' });
            return;
          }
          try {
            await updateCategory(p.id, { name, type: this.type });
            p.name = name;
            await this.load();
            uni.showToast({ title: '已修改', icon: 'success' });
          } catch (e) { /* 拦截器已提示 */ }
        }
      });
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
.sheet-actions { display: flex; justify-content: flex-end; gap: 16rpx; margin-bottom: 8rpx; }
.sheet-act {
  font-size: 24rpx; padding: 6rpx 28rpx; border-radius: 24rpx;
  border: 1rpx solid var(--primary); color: var(--primary);
}
.sheet-act.del { border-color: #E85D5D; color: #E85D5D; }
/* 一级分类头部 */
.sheet-hero {
  display: flex; flex-direction: column; align-items: center;
  padding: 16rpx 0 24rpx;
}
.sheet-hero-icon {
  width: 112rpx; height: 112rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 52rpx;
}
.sheet-hero-name { font-size: 30rpx; font-weight: 600; color: var(--text-main); margin-top: 12rpx; }
/* 二级分类区 */
.sheet-sub-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8rpx 4rpx 12rpx;
}
.sheet-sub-title { font-size: 28rpx; font-weight: 600; color: var(--text-main); }
.sheet-sub-body { max-height: 46vh; }
.sheet-sub-grid { display: flex; flex-wrap: wrap; padding-bottom: 12rpx; }
.sheet-sub-cell {
  width: 20%; box-sizing: border-box;
  display: flex; flex-direction: column; align-items: center;
  padding: 12rpx 0; position: relative;
}
.sheet-sub-icon {
  width: 92rpx; height: 92rpx; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 42rpx; background: var(--surface);
}
.sheet-sub-icon.add { color: var(--text-muted); font-size: 40rpx; }
.sheet-sub-name {
  font-size: 22rpx; color: var(--text-sub); margin-top: 10rpx;
  max-width: 120rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.sheet-sub-del {
  position: absolute; top: 4rpx; right: 10rpx;
  width: 34rpx; height: 34rpx; line-height: 32rpx; text-align: center;
  border-radius: 50%; background: #E85D5D; color: #fff; font-size: 24rpx; z-index: 2;
}
/* ===== 图片图标 ===== */
.cat-img { width: 100%; height: 100%; border-radius: 50%; }
.sub-img { width: 100%; height: 100%; border-radius: 50%; }
.add-icon-img { width: 100%; height: 100%; border-radius: 50%; }
.icon-upload-row { padding: 0 0 12rpx; }
.icon-upload-btn {
  display: inline-block; font-size: 22rpx; color: var(--primary);
  border: 1rpx dashed var(--primary); border-radius: 24rpx; padding: 8rpx 24rpx;
}
</style>
