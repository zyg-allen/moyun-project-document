<script setup lang="ts">
/**
 * 简历维护页 - 预览弹窗
 * 对应 vue_resume_spec.md §七 Modal 简历预览弹窗
 * 完整简历排版展示 + 导出 PDF 入口
 */
import { computed } from 'vue';
import { X, Download, FileText, User, Target, GraduationCap, Briefcase, Code, Star } from 'lucide-vue-next';
import type { UserResumeVO } from '@/types/api';

const props = defineProps<{
  visible: boolean;
  form: UserResumeVO;
  completeness: number;
  exporting?: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'export-pdf'): void;
}>();

const jobIntention = computed(() => props.form.jobIntention || {});
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="re-pm-mask" @click.self="emit('close')">
      <div class="re-pm-box">
        <!-- 头部 -->
        <div class="re-pm-head">
          <h3>
            <FileText class="w-4 h-4" style="color: var(--theme-primary);" />
            简历预览
          </h3>
          <div class="re-pm-head-actions">
            <button
              type="button"
              class="re-pm-export-btn"
              :disabled="exporting"
              @click="emit('export-pdf')"
            >
              <Download class="w-3.5 h-3.5" />
              {{ exporting ? '导出中' : '导出 PDF' }}
            </button>
            <button type="button" class="re-pm-close" @click="emit('close')">
              <X class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- 简历文档 -->
        <div class="re-pm-body">
          <div class="re-pm-doc">
            <!-- 头部信息 -->
            <div class="re-pm-doc-header">
              <img v-if="form.avatar" :src="form.avatar" alt="头像" class="re-pm-avatar" />
              <div class="re-pm-header-text">
                <h2>{{ form.title || '未命名简历' }}</h2>
                <div v-if="form.name || form.phone || form.email" class="re-pm-contact">
                  <span v-if="form.name">{{ form.name }}</span>
                  <span v-if="form.gender">· {{ form.gender }}</span>
                  <span v-if="form.birthDate">· {{ form.birthDate }}</span>
                  <span v-if="form.phone">· {{ form.phone }}</span>
                  <span v-if="form.email">· {{ form.email }}</span>
                </div>
              </div>
            </div>

            <!-- 求职意向 -->
            <section v-if="jobIntention.position || jobIntention.city" class="re-pm-doc-section">
              <h4><Target class="w-3 h-3" />求职意向</h4>
              <div class="re-pm-doc-grid">
                <span v-if="jobIntention.position">期望职位：{{ jobIntention.position }}</span>
                <span v-if="jobIntention.city">期望城市：{{ jobIntention.city }}</span>
                <span v-if="jobIntention.salaryMin">薪资：{{ jobIntention.salaryMin }}K - {{ jobIntention.salaryMax }}K</span>
                <span v-if="jobIntention.jobType">性质：{{ jobIntention.jobType }}</span>
                <span v-if="jobIntention.availableTime">到岗：{{ jobIntention.availableTime }}</span>
              </div>
            </section>

            <!-- 教育经历 -->
            <section v-if="form.educations && form.educations.length" class="re-pm-doc-section">
              <h4><GraduationCap class="w-3 h-3" />教育经历</h4>
              <div v-for="(edu, i) in form.educations" :key="'pe-'+i" class="re-pm-doc-item">
                <div class="re-pm-doc-item-head">
                  {{ edu.school }}<span v-if="edu.major"> · {{ edu.major }}</span><span v-if="edu.degree"> · {{ edu.degree }}</span>
                </div>
                <div class="re-pm-doc-item-meta">{{ edu.startDate }} ~ {{ edu.endDate }}</div>
                <p v-if="edu.description" class="re-pm-doc-item-desc">{{ edu.description }}</p>
              </div>
            </section>

            <!-- 工作经历 -->
            <section v-if="form.works && form.works.length" class="re-pm-doc-section">
              <h4><Briefcase class="w-3 h-3" />工作经历</h4>
              <div v-for="(w, i) in form.works" :key="'pw-'+i" class="re-pm-doc-item">
                <div class="re-pm-doc-item-head">
                  {{ w.company }}<span v-if="w.position"> · {{ w.position }}</span>
                </div>
                <div class="re-pm-doc-item-meta">{{ w.startDate }} ~ {{ w.endDate }}</div>
                <p v-if="w.description" class="re-pm-doc-item-desc">{{ w.description }}</p>
              </div>
            </section>

            <!-- 项目经历 -->
            <section v-if="form.projects && form.projects.length" class="re-pm-doc-section">
              <h4><Code class="w-3 h-3" />项目经历</h4>
              <div v-for="(p, i) in form.projects" :key="'pp-'+i" class="re-pm-doc-item">
                <div class="re-pm-doc-item-head">
                  {{ p.name }}<span v-if="p.role"> · {{ p.role }}</span>
                </div>
                <div class="re-pm-doc-item-meta">{{ p.startDate }} ~ {{ p.endDate }}</div>
                <p v-if="p.description" class="re-pm-doc-item-desc">{{ p.description }}</p>
              </div>
            </section>

            <!-- 技能 -->
            <section v-if="form.skills && form.skills.length" class="re-pm-doc-section">
              <h4><Star class="w-3 h-3" />专业技能</h4>
              <div class="re-pm-doc-skills">
                <span v-for="(s, i) in form.skills" :key="'ps-'+i" class="re-pm-doc-skill-chip">
                  {{ s.name }}<span v-if="s.level" class="re-pm-doc-skill-level">· {{ s.level }}</span>
                </span>
              </div>
            </section>

            <!-- 自我介绍 -->
            <section v-if="form.selfIntro" class="re-pm-doc-section">
              <h4><User class="w-3 h-3" />自我介绍</h4>
              <p class="re-pm-doc-intro">{{ form.selfIntro }}</p>
            </section>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.re-pm-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: re-pm-fade 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 20px;
}
@keyframes re-pm-fade { from { opacity: 0; } to { opacity: 1; } }
.re-pm-box {
  background: #fff;
  border-radius: 20px;
  width: 100%;
  max-width: 680px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.08), 0 8px 10px -6px rgba(0,0,0,0.04);
  animation: re-pm-slide 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes re-pm-slide { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

.re-pm-head {
  padding: 18px 24px;
  border-bottom: 1px solid var(--theme-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  background: #fff;
  z-index: 1;
  border-radius: 20px 20px 0 0;
}
.re-pm-head h3 { font-size: 15px; font-weight: 700; display: flex; align-items: center; gap: 8px; color: var(--theme-text); }
.re-pm-head-actions { display: flex; align-items: center; gap: 8px; }
.re-pm-export-btn {
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 6px;
  background: var(--theme-primary);
  color: #fff;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  transition: all 0.15s;
}
.re-pm-export-btn:hover:not(:disabled) { background: #b91c1c; }
.re-pm-export-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.re-pm-close {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 8px;
  border: 1px solid var(--theme-border);
  background: #fff;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}
.re-pm-close:hover { border-color: #d1d5db; background: #f9fafb; color: var(--theme-text); }

.re-pm-body { padding: 24px; }

.re-pm-doc-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 2px solid var(--theme-primary);
  margin-bottom: 18px;
}
.re-pm-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid #e5e7eb;
}
/* 无头像时保持原居中布局 */
.re-pm-doc-header:not(:has(.re-pm-avatar)) { justify-content: center; text-align: center; }
.re-pm-header-text { min-width: 0; }
.re-pm-doc-header h2 { font-size: 22px; font-weight: 800; color: var(--theme-text); }
.re-pm-contact { font-size: 12px; color: #6b7280; margin-top: 6px; display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; }
.re-pm-doc-header:not(:has(.re-pm-avatar)) .re-pm-contact { justify-content: center; }
.re-pm-doc-header:has(.re-pm-avatar) .re-pm-contact { justify-content: flex-start; }
.re-pm-doc-header:has(.re-pm-avatar) h2 { text-align: left; }

.re-pm-doc-section { margin-bottom: 18px; }
.re-pm-doc-section h4 {
  font-size: 13px;
  font-weight: 700;
  color: var(--theme-primary);
  margin-bottom: 8px;
  padding-bottom: 4px;
  border-bottom: 1px solid #f3f4f6;
  display: flex;
  align-items: center;
  gap: 6px;
}
.re-pm-doc-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 2px 16px; font-size: 12px; color: #4b5563; }
.re-pm-doc-item { margin-bottom: 8px; }
.re-pm-doc-item:last-child { margin-bottom: 0; }
.re-pm-doc-item-head { font-size: 13px; font-weight: 600; color: var(--theme-text); }
.re-pm-doc-item-meta { font-size: 11px; color: #9ca3af; margin-top: 1px; }
.re-pm-doc-item-desc { font-size: 12px; color: #4b5563; line-height: 1.7; margin-top: 4px; white-space: pre-line; }
.re-pm-doc-skills { display: flex; flex-wrap: wrap; gap: 6px; }
.re-pm-doc-skill-chip {
  font-size: 12px;
  padding: 3px 10px;
  background: #f3f4f6;
  color: #374151;
  border-radius: 12px;
  border: 1px solid var(--theme-border);
}
.re-pm-doc-skill-level { color: #9ca3af; margin-left: 2px; }
.re-pm-doc-intro { font-size: 12px; color: #4b5563; line-height: 1.7; white-space: pre-line; }

@media (max-width: 640px) {
  .re-pm-doc-grid { grid-template-columns: 1fr; }
}
</style>
