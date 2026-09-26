<template>
  <view class="page" :style="themeVars">
    <!-- 头部 -->
    <NavBar title="个人信息维护" />

    <!-- 内容 -->
    <view class="content">
      <view class="tip-card">
        以下信息是 AI 财务分析的基础数据（结合年龄/行业/家庭负担给出个性化分析），仅用于本人报表分析，越完整分析越精准。
        <view class="completeness">
          <view class="comp-bar"><view class="comp-inner" :style="{ width: completeness + '%' }"></view></view>
          <text class="comp-text">资料完善度 {{ completeness }}%</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title">基础画像</view>
        <view class="field">
          <text class="field-label">身份标签</text>
          <picker :range="identityLabels" @change="onTagChange">
            <view class="field-picker">{{ editForm.identityTagLabel || '选择身份' }} ▾</view>
          </picker>
        </view>
        <view class="field">
          <text class="field-label">生日</text>
          <picker mode="date" :value="editForm.birthday" :end="today" @change="onBirthdayChange">
            <view class="field-picker">{{ editForm.birthday || '选择生日（推算年龄）' }} ▾</view>
          </picker>
        </view>
        <view class="field">
          <text class="field-label">行业</text>
          <input v-model="editForm.industry" placeholder="如：IT / 金融 / 教育" class="field-input" maxlength="100" />
        </view>
        <view class="field">
          <text class="field-label">职位</text>
          <input v-model="editForm.position" placeholder="如：Java工程师 / 产品经理" class="field-input" maxlength="100" />
        </view>
        <view class="field">
          <text class="field-label">公司</text>
          <input v-model="editForm.company" placeholder="选填" class="field-input" maxlength="200" />
        </view>
      </view>

      <view class="card">
        <view class="card-title">家庭与收入（分析重点）</view>
        <view class="field">
          <text class="field-label">婚姻状况</text>
          <picker :range="maritalLabels" @change="onMaritalChange">
            <view class="field-picker">{{ editForm.maritalStatusLabel || '选择婚姻状况' }} ▾</view>
          </picker>
        </view>
        <view class="field">
          <text class="field-label">是否有房贷</text>
          <switch :checked="editForm.hasMortgage === 1" @change="e => editForm.hasMortgage = e.detail.value ? 1 : 0" color="#26a69a" />
        </view>
        <view class="field">
          <text class="field-label">是否有副业收入</text>
          <switch :checked="editForm.hasSideIncome === 1" @change="e => editForm.hasSideIncome = e.detail.value ? 1 : 0" color="#26a69a" />
        </view>
        <view class="field col">
          <view class="field-label">收入类型（多选）</view>
          <view class="type-grid">
            <view class="type-item" :class="{ on: incomeTypeSet.has(t.value) }" v-for="t in incomeTypeOptions" :key="t.value" @tap="toggleIncomeType(t.value)">
              {{ t.label }}
            </view>
          </view>
        </view>
      </view>

      <view class="btn-primary" :class="{ disabled: saving }" @tap="save">{{ saving ? '保存中…' : '保存' }}</view>
    </view>

    <!-- 脚部 -->
    <view class="footer">画像信息与墨韵门户共用（同一账号体系）</view>
  </view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue';
import { getAiProfile, updateAiProfile } from '@/api/ledger';
import { useUserStore } from '@/stores/user';
import { useThemeStore } from '@/stores/theme';

export default {
  components: { NavBar },
  data() {
    return {
      saving: false,
      identityOptions: [],
      editForm: {
        identityTag: '', identityTagLabel: '',
        birthday: '', industry: '', position: '', company: '',
        maritalStatus: '', maritalStatusLabel: '',
        hasMortgage: null, hasSideIncome: null
      },
      maritalOptions: [
        { value: 'single', label: '单身' },
        { value: 'married', label: '已婚' },
        { value: 'other', label: '其他' }
      ],
      incomeTypeOptions: [
        { value: 'salary', label: '工资' },
        { value: 'bonus', label: '奖金' },
        { value: 'investment', label: '投资收益' },
        { value: 'rent', label: '租金' },
        { value: 'side', label: '副业' },
        { value: 'other', label: '其他' }
      ],
      incomeTypes: []
    };
  },
  computed: {
    themeVars() { return useThemeStore().themeVars; },
    identityLabels() { return this.identityOptions.map(o => o.label); },
    maritalLabels() { return this.maritalOptions.map(o => o.label); },
    incomeTypeSet() { return new Set(this.incomeTypes); },
    today() {
      const d = new Date();
      return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0');
    },
    completeness() {
      const checks = [
        this.editForm.identityTag, this.editForm.birthday, this.editForm.industry,
        this.editForm.position, this.editForm.maritalStatus,
        this.editForm.hasMortgage, this.editForm.hasSideIncome, this.incomeTypes.length
      ];
      const filled = checks.filter(v => v || v === 0 || (Array.isArray(v) && v.length)).length;
      return Math.round(filled / checks.length * 100);
    }
  },
  onShow() {
    useThemeStore().restore();
    if (useUserStore().isLoggedIn) this.load();
  },
  methods: {
    async load() {
      try {
        const pf = await getAiProfile() || {};
        this.identityOptions = pf.identityOptions || [];
        this.editForm = {
          identityTag: pf.identityTag || '',
          identityTagLabel: pf.identityTagLabel || '',
          birthday: pf.birthday || '',
          industry: pf.industry || '',
          position: pf.position || '',
          company: pf.company || '',
          maritalStatus: pf.maritalStatus || '',
          maritalStatusLabel: pf.maritalStatusLabel || '',
          hasMortgage: pf.hasMortgage == null ? null : Number(pf.hasMortgage),
          hasSideIncome: pf.hasSideIncome == null ? null : Number(pf.hasSideIncome)
        };
        this.incomeTypes = Array.isArray(pf.incomeTypes) ? pf.incomeTypes.filter(Boolean) : [];
      } catch (e) { /* 拦截器已提示 */ }
    },
    onTagChange(e) {
      const opt = this.identityOptions[Number(e.detail.value)];
      if (opt) {
        this.editForm.identityTag = opt.value;
        this.editForm.identityTagLabel = opt.label;
      }
    },
    onBirthdayChange(e) { this.editForm.birthday = e.detail.value; },
    onMaritalChange(e) {
      const opt = this.maritalOptions[Number(e.detail.value)];
      if (opt) {
        this.editForm.maritalStatus = opt.value;
        this.editForm.maritalStatusLabel = opt.label;
      }
    },
    toggleIncomeType(value) {
      const idx = this.incomeTypes.indexOf(value);
      if (idx >= 0) this.incomeTypes.splice(idx, 1);
      else this.incomeTypes.push(value);
    },
    async save() {
      if (this.saving) return;
      if (!useUserStore().isLoggedIn) { uni.showToast({ title: '请先登录', icon: 'none' }); return; }
      this.saving = true;
      try {
        await updateAiProfile({
          identityTag: this.editForm.identityTag || '',
          position: this.editForm.position || '',
          company: this.editForm.company || '',
          birthday: this.editForm.birthday || '',
          industry: this.editForm.industry || '',
          maritalStatus: this.editForm.maritalStatus || '',
          hasMortgage: this.editForm.hasMortgage == null ? '' : String(this.editForm.hasMortgage),
          hasSideIncome: this.editForm.hasSideIncome == null ? '' : String(this.editForm.hasSideIncome),
          incomeTypes: this.incomeTypes.join(',')
        });
        uni.showToast({ title: '已保存', icon: 'success' });
        setTimeout(() => uni.navigateBack(), 700);
      } catch (e) { /* 拦截器已提示 */ }
      this.saving = false;
    }
  }
};
</script>

<style scoped>
.page { padding-bottom: 60rpx; min-height: 100vh; background: #f5f6f8; display: flex; flex-direction: column; }

.content { flex: 1; }

.tip-card {
  margin: 24rpx 24rpx 0; padding: 20rpx 24rpx;
  background: #f0f7ff; border-left: 6rpx solid var(--primary-strong);
  border-radius: 12rpx; font-size: 24rpx; color: #55708c; line-height: 1.7;
}
.completeness { display: flex; align-items: center; gap: 16rpx; margin-top: 16rpx; }
.comp-bar { flex: 1; height: 14rpx; background: #e3ecf5; border-radius: 7rpx; overflow: hidden; }
.comp-inner { height: 100%; background: var(--primary-strong); border-radius: 7rpx; transition: width .3s; }
.comp-text { font-size: 22rpx; color: var(--primary-strong); flex-shrink: 0; }

.card { background: #fff; border-radius: 20rpx; padding: 24rpx 32rpx 8rpx; margin: 24rpx 24rpx 0; }
.card-title { font-size: 28rpx; font-weight: 600; color: #333; padding-bottom: 8rpx; }
.field { display: flex; align-items: center; padding: 26rpx 0; border-bottom: 1rpx solid #f5f5f7; }
.field.col { flex-direction: column; align-items: stretch; gap: 20rpx; }
.field:last-of-type { border-bottom: none; }
.field-label { width: 220rpx; font-size: 28rpx; color: #666; flex-shrink: 0; }
.field-input { flex: 1; font-size: 28rpx; text-align: right; }
.field-picker { flex: 1; font-size: 28rpx; text-align: right; color: #333; }

.type-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.type-item {
  font-size: 26rpx; color: #666; background: #f5f6f8; border-radius: 30rpx;
  padding: 12rpx 30rpx; border: 1rpx solid transparent;
}
.type-item.on { color: #fff; background: var(--primary-strong); }

.btn-primary {
  margin: 32rpx 24rpx 0; height: 88rpx; line-height: 88rpx; text-align: center;
  background: var(--primary-strong); color: #fff; border-radius: 44rpx;
  font-size: 30rpx; font-weight: 600;
}
.btn-primary.disabled { opacity: 0.6; }
.btn-primary:active { opacity: 0.85; }

.footer {
  padding: 32rpx 0 calc(24rpx + env(safe-area-inset-bottom));
  text-align: center; font-size: 22rpx; color: #c3c8cf;
}
</style>
