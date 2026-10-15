<template>
  <view class="page" :style="themeVars">
    <NavBar :title="editId ? '编辑计划' : '添加计划'">
      <view class="nav-ok" @tap="save">确定</view>
    </NavBar>

    <!-- 周期设置 -->
    <view class="form-card">
      <view class="form-row">
        <text class="form-label">循环周期</text>
        <picker class="fill-picker" :range="cycleLabels" :value="cycleIndex" @change="onCycleChange">
          <view class="form-value">{{ cycleLabels[cycleIndex] }} ›</view>
        </picker>
      </view>
      <view class="form-row" v-if="form.cycle === 'weekly'">
        <text class="form-label">每周几</text>
        <picker class="fill-picker" :range="weekLabels" :value="form.dayOfWeek - 1" @change="(e) => form.dayOfWeek = e.detail.value + 1">
          <view class="form-value">周{{ weekLabels[form.dayOfWeek - 1] }} ›</view>
        </picker>
      </view>
      <view class="form-row" v-if="form.cycle === 'monthly'">
        <text class="form-label">每月几号</text>
        <picker class="fill-picker" :range="dayList" :value="form.dayOfMonth - 1" @change="(e) => form.dayOfMonth = e.detail.value + 1">
          <view class="form-value">{{ form.dayOfMonth }}号 ›</view>
        </picker>
      </view>
      <view class="form-row" v-if="form.cycle === 'interval'">
        <text class="form-label">间隔天数</text>
        <input class="form-input" type="number" v-model="form.intervalDays" placeholder="如 3" />
      </view>
      <view class="form-row">
        <text class="form-label">执行时间</text>
        <picker mode="time" class="fill-picker" :value="form.execTime" @change="(e) => form.execTime = e.detail.value">
          <view class="form-value">{{ form.execTime }} ›</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="form-label">开始时间</text>
        <CalendarPicker class="fill-picker" :value="form.startDate" @change="onStartDateChange">
          <view class="form-value">{{ form.startDate }} ›</view>
        </CalendarPicker>
      </view>
      <view class="form-row">
        <text class="form-label">结束时间</text>
        <CalendarPicker class="fill-picker" :value="form.endDate" @change="(e) => form.endDate = e.detail.value">
          <view class="form-value">{{ form.endDate || '不填表示永不结束' }} ›</view>
        </CalendarPicker>
      </view>
    </view>

    <!-- 记账内容 -->
    <view class="form-card">
      <view class="form-row col">
        <text class="form-label">类型</text>
        <view class="type-group">
          <view class="type-tag" :class="{ active: form.type === 'expense' }" @tap="onTypeChange('expense')">支出</view>
          <view class="type-tag" :class="{ active: form.type === 'income' }" @tap="onTypeChange('income')">收入</view>
        </view>
      </view>
      <!-- 分类：与「记一笔」同一套宫格语言（图标 + 名称、选中高亮、浅色底随分类色）。
           交互也一致：**选中后自动收起**，点标题栏可重新展开；
           编辑反显时默认收起（见 loadForEdit）。
           这里刻意更简单：不做二级分类与自定义增删。 -->
      <view class="form-row col">
        <view class="cat-head">
          <text class="form-label">分类</text>
          <view class="cat-head-right" @tap="catCollapsed = !catCollapsed">
            <text class="cat-count">{{ selectedCategoryName || '请选择' }}</text>
            <text class="cat-toggle">{{ catCollapsed ? '展开 ▾' : '收起 ▴' }}</text>
          </view>
        </view>
        <view class="cat-grid" v-show="!catCollapsed">
          <view v-for="c in categories" :key="c.id" class="cat-cell" @tap="form.categoryId = c.id">
            <view class="cat-icon" :class="{ selected: form.categoryId === c.id }"
                  :style="form.categoryId === c.id ? '' : 'background:' + softColor(c.color)">
              <text>{{ iconOf(c.icon) }}</text>
            </view>
            <text class="cat-name" :class="{ selected: form.categoryId === c.id }">{{ c.name }}</text>
          </view>
          <view v-if="!categories.length" class="cat-empty">该类型下暂无可选分类</view>
        </view>
      </view>
      <view class="form-row">
        <text class="form-label">金额</text>
        <input class="form-input amount" type="digit" v-model="form.amount" placeholder="0" @input="onAmountInput" />
      </view>
      <view class="form-row">
        <text class="form-label">账户</text>
        <picker class="fill-picker" :range="accountNames" @change="onAccountChange">
          <view class="form-value" :class="{ placeholder: !form.accountId }">
            {{ form.accountId ? accountName : '请选择资产' }} ›
          </view>
        </picker>
      </view>
    </view>

    <!-- 备注 -->
    <view class="form-card">
      <view class="form-row col">
        <text class="form-label">备注</text>
        <textarea class="form-textarea" v-model="form.description" placeholder="点击输入备注..." maxlength="200" @input="onDescInput" />
      </view>
    </view>

    <!-- 邮件提醒：默认关。用途不是"每次执行都通知"（那会变成轰炸），
         而是当任务长期无人过问时提醒一次，避免"以为已取消其实还在跑"。 -->
    <view class="form-card">
      <view class="form-row">
        <view class="flex-1 min-w-0">
          <text class="form-label block">邮件提醒</text>
          <text class="switch-hint">执行失败时及时通知；长期未登录时提醒任务仍在执行</text>
        </view>
        <switch :checked="form.notifyEnabled === 1" @change="onNotifyChange" style="transform: scale(0.85)" />
      </view>
    </view>

    <view class="preview" v-if="nextExecText">下次记账时间：{{ nextExecText }}</view>

    <view class="submit-btn" :class="{ disabled: submitting }" @tap="!submitting && save()">确定</view>
  </view>
</template>

<script>
import CalendarPicker from '@/components/CalendarPicker/CalendarPicker.vue';
import submitLock from '@/mixins/submitLock';
import NavBar from '@/components/NavBar/NavBar.vue';
import { useThemeStore } from '@/stores/theme';
import { listCategories, listAssets, createScheduleTask, listScheduleTasks, updateScheduleTask } from '@/api/ledger';
import { applyAmountInput } from '@/utils/money';
import { categoryIcon, softColor as toSoftColor } from '@/utils/categoryIcon';

export default {
  mixins: [submitLock],
  components: { CalendarPicker,  NavBar },
  data() {
    const today = new Date();
    return {
      cycleLabels: ['每天', '每周', '每月', '每N天'],
      weekLabels: ['一', '二', '三', '四', '五', '六', '日'],
      dayList: Array.from({ length: 28 }, (_, i) => (i + 1) + '号'),
      cycleKeys: ['daily', 'weekly', 'monthly', 'interval'],
      form: {
        name: '',
        type: 'expense',
        amount: '',
        categoryId: null,
        accountId: null,
        description: '',
        cycle: 'daily',
        dayOfWeek: 1,
        dayOfMonth: 1,
        intervalDays: '',
        execTime: '08:00',
        startDate: `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`,
        endDate: '',
        // 邮件提醒开关：默认 0（关）。定时任务可能每天执行，
        // 默认发信会变成每日轰炸，必须用户逐任务显式开启。
        notifyEnabled: 0
      },
      categories: [],
      accounts: [],
      // 编辑模式：由列表页「编辑」经 url 传 id 进入
      editId: null,
      editLoaded: false,
      // 编辑回填时后端带回的账户名称，作为选项列表里的兜底显示
      fallbackAccountName: '',
      // 分类宫格是否收起：与记一笔一致 —— 选中即收起；编辑反显时也收起（见 loadForEdit）
      catCollapsed: false,
      // 备注是否被用户手动改过：改过之后就不再自动拼接覆盖（与记一笔同一套逻辑）
      descEdited: false
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    cycleIndex() { return this.cycleKeys.indexOf(this.form.cycle); },
    /* 账户列表里找不到时回退到后端回填的名称 —— 否则会出现"明明保存了，
       重进编辑却显示「请选择资产」"（列表未加载完、或账户已归档都会这样）。
       分类已改为宫格直接展示名称，不再需要同类兜底。 */
    accountNames() { return this.accounts.map(a => a.name); },
    /** 已选分类名（宫格标题栏展示；选项未加载完时为空 → 显示「请选择」） */
    selectedCategoryName() {
      const c = this.categories.find(x => x.id === this.form.categoryId);
      return c ? c.name : '';
    },
    /** 周期的人话描述（带明细，用于备注默认值） */
    cycleLabel() {
      switch (this.form.cycle) {
        case 'weekly': return '每周' + (this.weekLabels[this.form.dayOfWeek - 1] || '');
        case 'monthly': return '每月' + (this.form.dayOfMonth || 1) + '号';
        case 'interval': return '每' + (this.form.intervalDays || 'N') + '天';
        default: return '每天';
      }
    },
    accountName() {
      const a = this.accounts.find(x => x.id === this.form.accountId);
      return a ? a.name : this.fallbackAccountName;
    },
    nextExecText() {
      const d = this.firstExecDate();
      return d ? `${d} ${this.form.execTime}` : '';
    }
  },
  watch: {
    /**
     * 分类变化 → 自动收起宫格 + 刷新备注默认值。
     *
     * 为什么放 watch 而不是点击处理器里：
     * ① 模板只做纯赋值 `form.categoryId = c.id`，不引用方法，也就**不依赖
     *    `$options` 的运行时解析** —— HMR 半更新（新模板 + 旧脚本）时那种
     *    "xxx is not a function" 从根上不会出现；
     * ② 任何改动路径都能统一联动：点击选择、编辑回填、切类型清空。
     *
     * 只在**选中**时收起：切类型会先清空 categoryId，此时应保持展开便于重选，
     * 故 val 为空时不动 catCollapsed（onTypeChange 里已显式展开）。
     */
    'form.categoryId'(val) {
      if (val) this.catCollapsed = true;
      this.refreshRemark();
    }
  },
  onLoad(options) {
    // 列表页点「编辑」时带 id 进来；有 id 即进入编辑模式。
    // ⚠️ 必须保持**字符串**，不要 Number()：后端 ApplicationConfig 配了
    // `serializerByType(Long.class, ToStringSerializer.instance)`，
    // 所有 Long 都序列化成 JSON 字符串（防 JS 精度丢失），
    // 于是 tasks[].id 是 "1" 而 Number(options.id) 是 1 —— 用 === 比较永远不成立，
    // 表现为"后端明明有数据，前端却提示任务不存在或已删除"、表单完全不回填。
    if (options && options.id) this.editId = String(options.id);
  },
  onShow() {
    useThemeStore().restore();
    this.loadOptions();
    // 编辑模式首次进入回填一次；用 editLoaded 兜住 onShow 的重复触发，
    // 避免从其它页返回时把用户已修改的内容覆盖掉
    if (this.editId && !this.editLoaded) {
      this.editLoaded = true;
      this.loadForEdit();
    } else if (!this.editId && !this.form.description) {
      // 新增：预置一次默认备注，让用户一眼看到拼接规则（之后随分类/周期/开始日期联动）
      this.refreshRemark();
    }
  },
  methods: {
    /** 金额输入净化（见 utils/money.applyAmountInput） */
    onAmountInput(e) { applyAmountInput(e, this, 'form.amount'); },
    /** 分类图标 emoji（与分类管理/记一笔同一份映射） */
    iconOf(icon) { return categoryIcon(icon); },
    /** 分类颜色 → 浅色底（与记一笔一致，避免 emoji 撞色） */
    softColor(color) { return toSoftColor(color); },
    /**
     * 备注默认值：`开始日期 类型-分类（周期）`
     * 与记一笔同构（日期 + 类型 + 分类），括号里换成**周期** ——
     * 定时任务最需要一眼看出的就是"多久一次"。
     * 注：任务名取自备注前 50 字，所以这个默认值同时决定了任务名。
     */
    buildRemarkPrefix() {
      const dateStr = (this.form.startDate || '').replace(/-/g, '');
      const typeName = this.form.type === 'income' ? '收入' : '支出';
      const cat = this.categories.find(x => x.id === this.form.categoryId);
      const catPart = cat ? `-${cat.name}（${this.cycleLabel}）` : '';
      return `${dateStr} ${typeName}${catPart}`;
    },
    /** 未手动编辑过备注时，自动刷新默认值（类型/分类/周期/开始日期变化联动） */
    refreshRemark() {
      if (this.descEdited) return;
      this.form.description = this.buildRemarkPrefix();
    },
    /** 切换本任务的邮件提醒开关（仅改表单值，随「确定」一起提交） */
    onNotifyChange(e) { this.form.notifyEnabled = e.detail.value ? 1 : 0; },
    /** 用户一旦手动改过备注，就不再自动覆盖 */
    onDescInput() { this.descEdited = true; },
    onStartDateChange(e) {
      this.form.startDate = e.detail.value;
      this.refreshRemark();
    },
    async loadOptions() {
      // 捕获发起时的类型：await 期间类型可能被改（编辑回填 / 用户切类型），
      // 若不校验就会把"上一次类型"的分类列表写进来，导致分类对不上、
      // 已保存的分类显示成「请选择分类」。
      const type = this.form.type;
      try {
        const catData = await listCategories(type) || {};
        if (type === this.form.type) this.categories = catData.records || [];
      } catch (e) { }
      try {
        const accData = await listAssets() || {};
        this.accounts = accData.records || [];
      } catch (e) { }
    },
    /**
     * 编辑模式：拉取任务列表并回填表单。
     * 列表接口返回该用户全部未删除任务（无分页），按 id 查即可；
     * 这样无需新增单条查询接口，也不会像"从分页列表里找"那样漏数据。
     */
    async loadForEdit() {
      try {
        const data = await listScheduleTasks() || {};
        // 两侧统一 String()：后端 Long 序列化为字符串，而 URL 参数类型不定，
        // 直接 === 会因 "1" vs 1 失配（详见 onLoad 注释）
        const t = (data.tasks || []).find(x => String(x.id) === String(this.editId));
        if (!t) { uni.showToast({ title: '任务不存在或已删除', icon: 'none' }); return; }
        this.form.type = t.type || 'expense';
        this.form.amount = t.amount != null ? String(t.amount) : '';
        this.form.categoryId = t.categoryId || null;
        this.form.accountId = t.accountId || null;
        this.form.description = t.description || '';
        this.form.cycle = t.cycle || 'daily';
        this.form.dayOfWeek = t.dayOfWeek || 1;
        this.form.dayOfMonth = t.dayOfMonth || 1;
        this.form.intervalDays = t.intervalDays != null ? String(t.intervalDays) : '';
        this.form.execTime = t.execTime || '08:00';
        if (t.startDate) this.form.startDate = t.startDate;
        this.form.endDate = t.endDate || '';
        // 后端 listTasks 已回填 accountName，存下来做兜底显示
        this.fallbackAccountName = t.accountName || '';
        // notifyEnabled 是 tinyint，序列化为数字，故用 === 1
        this.form.notifyEnabled = t.notifyEnabled === 1 ? 1 : 0;
        // 编辑反显：分类宫格默认收起，让已保存的值一眼可见（点标题栏可展开重选）。
        // 同时标记备注已被用户"接管"，避免后续联动把存过的备注覆盖掉。
        this.catCollapsed = true;
        this.descEdited = true;
        // 类型可能和默认值不同，按其重新拉分类，否则分类下拉对不上（await 保证顺序）
        await this.loadOptions();
      } catch (e) { /* 拦截器已提示 */ }
    },
    onCycleChange(e) {
      this.form.cycle = this.cycleKeys[e.detail.value];
      this.refreshRemark();   // 周期变了 → 备注默认值里的"多久一次"跟着更新
    },
    onTypeChange(type) {
      if (this.form.type === type) return;
      this.form.type = type;
      this.form.categoryId = null;
      // 切类型后分类整体换批且已清空选择 → 展开宫格便于直接点选（与记一笔一致）
      this.catCollapsed = false;
      this.loadOptions();
      this.refreshRemark();
    },
    onAccountChange(e) {
      const a = this.accounts[e.detail.value];
      this.form.accountId = a ? a.id : null;
    },
    /** 与后端 firstExecDate 相同的规则：从开始日期起按周期推算第一个执行日 */
    firstExecDate() {
      const start = this.form.startDate ? new Date(this.form.startDate + 'T00:00:00') : new Date();
      const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
      const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
      let candidate = addDays(start, 1);
      switch (this.form.cycle) {
        case 'daily': return fmt(candidate);
        case 'weekly': {
          const jsDow = start.getDay() === 0 ? 7 : start.getDay();
          const diff = (this.form.dayOfWeek - jsDow + 7) % 7;
          return fmt(diff === 0 ? addDays(start, 7) : addDays(start, diff));
        }
        case 'monthly': {
          const base = new Date(start.getFullYear(), start.getMonth(), this.form.dayOfMonth);
          if (base >= start) base.setMonth(base.getMonth() + 1);
          return fmt(base);
        }
        case 'interval': {
          const n = parseInt(this.form.intervalDays);
          return fmt(addDays(start, n > 0 ? n : 1));
        }
      }
      return '';
    },
    async save() {
      return this.withSubmitLock(async () => {
              const amount = parseFloat(this.form.amount);
              if (!amount || amount <= 0) { uni.showToast({ title: '请输入金额', icon: 'none' }); return; }
              if (!this.form.categoryId) { uni.showToast({ title: '请选择分类', icon: 'none' }); return; }
              if (!this.form.accountId) { uni.showToast({ title: '请选择资产账户', icon: 'none' }); return; }
              if (this.form.cycle === 'interval' && !(parseInt(this.form.intervalDays) > 0)) {
                uni.showToast({ title: '请输入间隔天数', icon: 'none' }); return;
              }
              const payload = {
                name: this.form.description ? this.form.description.slice(0, 50)
                      : (this.form.type === 'expense' ? '定时支出' : '定时收入'),
                type: this.form.type,
                amount,
                categoryId: this.form.categoryId,
                accountId: this.form.accountId,
                description: this.form.description || null,
                cycle: this.form.cycle,
                dayOfWeek: this.form.cycle === 'weekly' ? this.form.dayOfWeek : null,
                dayOfMonth: this.form.cycle === 'monthly' ? this.form.dayOfMonth : null,
                intervalDays: this.form.cycle === 'interval' ? parseInt(this.form.intervalDays) : null,
                execTime: this.form.execTime,
                startDate: this.form.startDate,
                endDate: this.form.endDate || null,
                notifyEnabled: this.form.notifyEnabled ? 1 : 0
              };
              try {
                if (this.editId) {
                  await updateScheduleTask(this.editId, payload);
                  uni.showToast({ title: '已保存', icon: 'success' });
                } else {
                  await createScheduleTask(payload);
                  uni.showToast({ title: '创建成功', icon: 'success' });
                }
                setTimeout(() => uni.navigateBack(), 600);
              } catch (e) { /* 拦截器已提示 */ }
      });
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 160rpx; min-height: 100vh; background: var(--surface); }
.nav-ok { font-size: 26rpx; color: var(--primary-text); padding: 8rpx 20rpx; min-height: 88rpx; display: flex; align-items: center; }

.form-card { background: var(--card); margin: 24rpx 24rpx 0; border-radius: 20rpx; padding: 8rpx 32rpx; }
.form-row {
  display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid var(--border);
}
.form-row:last-child { border-bottom: none; }
.form-row.col { flex-direction: column; align-items: flex-start; gap: 20rpx; }
.form-label { width: 180rpx; font-size: 28rpx; color: var(--text-main); flex-shrink: 0; }
.form-row.col .form-label { width: auto; }
.form-input { flex: 1; text-align: right; font-size: 28rpx; }
.form-input.amount { font-size: 34rpx; font-weight: 700; color: var(--primary-text); }
.form-value { font-size: 28rpx; color: var(--text-main); }
.form-value.placeholder { color: var(--text-muted); }
/* 横向行（标签 + 值）的值统一右对齐，与 form-input 一致；
   .col 是纵向布局（类型组 / 备注 textarea），不适用 */
.form-row:not(.col) .form-value { flex: 1; text-align: right; }

/* ---- 分类宫格（与「记一笔」同一套视觉：4 列、图标 + 名称、选中放大 + 强调色底） ---- */
/* 折叠头：左侧"分类"标签，右侧已选名称 + 展开/收起 */
.cat-head { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.cat-head-right { display: flex; align-items: center; gap: 16rpx; }
.cat-count { font-size: 24rpx; color: var(--primary-text); }
.cat-toggle { font-size: 24rpx; color: var(--text-muted); }
.cat-grid { display: flex; flex-wrap: wrap; width: 100%; }
.cat-cell {
  width: 25%; display: flex; flex-direction: column; align-items: center;
  padding: 12rpx 0;
}
.cat-icon {
  width: 72rpx; height: 72rpx; border-radius: 18rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 34rpx; background: var(--surface);
}
.cat-icon.selected { background: var(--primary) !important; transform: scale(1.06); }
.cat-name {
  font-size: 22rpx; color: var(--text-sub); margin-top: 8rpx;
  max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.cat-name.selected { color: var(--primary-text); font-weight: 600; }
.cat-empty { width: 100%; font-size: 24rpx; color: var(--text-muted); padding: 16rpx 0; }

/* 邮件提醒开关行 */
.form-label.block { display: block; width: auto; }
.switch-hint { display: block; font-size: 22rpx; color: var(--text-muted); margin-top: 6rpx; line-height: 1.4; }

.type-group { display: flex; gap: 16rpx; }
.type-tag {
  font-size: 26rpx; padding: 10rpx 40rpx; border-radius: 32rpx;
  background: var(--surface); color: var(--text-sub);
}
.type-tag.active { background: var(--primary-strong); color: var(--on-primary); }

.form-textarea {
  width: 100%; min-height: 120rpx; font-size: 26rpx; color: var(--text-main);
  background: var(--surface); border-radius: 12rpx; padding: 20rpx; box-sizing: border-box;
}

.preview {
  margin: 24rpx 32rpx 0; font-size: 24rpx; color: var(--primary-text);
  background: var(--primary-soft); border-radius: 12rpx; padding: 16rpx 24rpx;
}

.submit-btn {
  position: fixed; bottom: 40rpx; left: 24rpx; right: 24rpx;
  height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: var(--on-primary);
  border-radius: 44rpx; font-size: 30rpx; font-weight: 600;
}
/* 提交锁生效时给出视觉反馈（与全局 .btn-primary.disabled 一致） */
.submit-btn.disabled { opacity: 0.5; }
</style>
