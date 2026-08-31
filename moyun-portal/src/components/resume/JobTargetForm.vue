<template>
  <!--
    新建岗位弹窗（v10.18 阶段三独立组件抽离）
    设计文档：docs/简历编辑和优化模块重构设计-20260826.md 阶段三
    职责：纯表单弹窗 + v-model 双向绑定，调用方负责 saveJobTarget
  -->
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4" @click.self="$emit('update:visible', false)">
      <div class="bg-theme-surface rounded-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between px-5 py-4 border-b sticky top-0 bg-theme-surface">
          <h3 class="font-semibold flex items-center gap-2"><Plus class="w-4 h-4" style="color: var(--theme-primary);" /> 新建目标岗位</h3>
          <button class="text-theme-text-secondary hover:text-theme-text-secondary" @click="$emit('update:visible', false)"><X class="w-4 h-4" /></button>
        </div>
        <div class="p-5 space-y-4">
          <div>
            <label class="text-sm font-medium text-theme-text">目标岗位名称 <span class="text-theme-danger">*</span></label>
            <input :value="form.position" @input="updateField('position', ($event.target as HTMLInputElement).value)" placeholder="如：Java 开发工程师" class="mt-1 w-full border border-theme-border rounded-lg px-3 py-2 text-sm outline-none focus:border-theme-primary" />
          </div>
          <div class="grid grid-cols-3 gap-3">
            <div>
              <label class="text-sm font-medium text-theme-text">目标公司</label>
              <input :value="form.company" @input="updateField('company', ($event.target as HTMLInputElement).value)" placeholder="选填" class="mt-1 w-full border border-theme-border rounded-lg px-3 py-2 text-sm outline-none focus:border-theme-primary" />
            </div>
            <div>
              <label class="text-sm font-medium text-theme-text">城市</label>
              <input :value="form.city" @input="updateField('city', ($event.target as HTMLInputElement).value)" placeholder="选填" class="mt-1 w-full border border-theme-border rounded-lg px-3 py-2 text-sm outline-none focus:border-theme-primary" />
            </div>
            <div>
              <label class="text-sm font-medium text-theme-text">岗位类型</label>
              <select :value="form.jobType" @change="updateField('jobType', ($event.target as HTMLSelectElement).value)" class="mt-1 w-full border border-theme-border rounded-lg px-3 py-2 text-sm outline-none focus:border-theme-primary">
                <option v-for="t in jobTypeOptions" :key="t" :value="t">{{ t }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="text-sm font-medium text-theme-text">岗位描述（JD）<span class="text-theme-danger">*</span></label>
            <textarea :value="form.jdText" @input="updateField('jdText', ($event.target as HTMLTextAreaElement).value)" rows="7" placeholder="粘贴 BOSS直聘/拉勾等平台的岗位描述...&#10;例如：&#10;1. 5年以上Java开发经验，精通Spring Boot&#10;2. 熟悉微服务架构..." class="mt-1 w-full border border-theme-border rounded-lg px-3 py-2 text-sm outline-none focus:border-theme-primary resize-y" />
            <p class="text-xs text-theme-text-secondary mt-1">JD 越完整，匹配分析与优化建议越精准</p>
          </div>
        </div>
        <div class="flex justify-end gap-2 px-5 py-4 border-t">
          <button class="text-sm px-4 py-2 rounded-lg border border-theme-border text-theme-text-secondary" @click="$emit('update:visible', false)">取消</button>
          <button class="text-sm px-5 py-2 rounded-lg text-white font-medium disabled:opacity-50" style="background: var(--theme-primary);" :disabled="saving" @click="$emit('save')">
            {{ saving ? '保存中...' : '保存岗位' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { Plus, X } from 'lucide-vue-next';
import type { ResumeJobTarget } from '@/types/api';

/**
 * 新建岗位弹窗（v10.18 阶段三独立组件）
 * props:
 *   - visible: 弹窗显示
 *   - saving: 保存中
 *   - form: 岗位表单数据（v-model 双向绑定）
 *   - jobTypeOptions: 岗位类型选项数组（默认全职/兼职/实习）
 * emits:
 *   - update:visible: 关闭弹窗
 *   - update:form: 表单字段变更
 *   - save: 保存按钮点击（调用方负责 createJobTarget + toast）
 */
const props = defineProps<{
  visible: boolean;
  saving: boolean;
  form: ResumeJobTarget;
  jobTypeOptions?: string[];
}>();

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void;
  (e: 'update:form', v: ResumeJobTarget): void;
  (e: 'save'): void;
}>();

const DEFAULT_JOB_TYPES = ['全职', '兼职', '实习'];
const jobTypeOptions = props.jobTypeOptions ?? DEFAULT_JOB_TYPES;

/** 单字段更新，避免整个 form 替换破坏响应式 */
function updateField<K extends keyof ResumeJobTarget>(key: K, value: ResumeJobTarget[K]) {
  emit('update:form', { ...props.form, [key]: value });
}
</script>
