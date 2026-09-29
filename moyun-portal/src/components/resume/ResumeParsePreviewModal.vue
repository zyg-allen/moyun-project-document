<template>
  <Teleport to="body">
    <Transition name="rpp-fade">
      <div v-if="visible" class="rpp-mask" @click.self="onCancel">
        <div class="rpp-panel" role="dialog" aria-modal="true">
          <!-- 头部 -->
          <header class="rpp-head">
            <div class="rpp-head-left">
              <h3 class="rpp-title">简历解析结果确认</h3>
              <p class="rpp-sub">
                <span class="rpp-file">📄 {{ data.fileName || '附件简历' }}</span>
                <span class="rpp-dot">·</span>
                <span>抽取 {{ data.textLength || 0 }} 字</span>
                <span class="rpp-dot">·</span>
                <span :class="['rpp-badge', data.aiPowered ? 'is-ai' : 'is-rule']">
                  {{ data.aiPowered ? 'AI 结构化' : '规则解析' }}
                </span>
              </p>
            </div>
            <button class="rpp-close" type="button" aria-label="关闭" @click="onCancel">✕</button>
          </header>

          <!-- 降级提示：未识别到章节 -->
          <div v-if="data.sectionDetectFailed" class="rpp-alert">
            ⚠️ 未识别到明确的章节标题（如「教育背景」「工作经历」），内容可能整块归入基本信息区。
            请在右侧原文中确认，必要时在下一步的编辑页手动整理。
          </div>
          <div v-else-if="data.sectionCount > 0" class="rpp-ok">
            ✅ 已识别 {{ data.sectionCount }} 个内容大类，请对照左侧原文校对
          </div>

          <!-- 主体：左原文 / 右解析结果（U1 左右对照） -->
          <div class="rpp-body">
            <!-- 左：原文 -->
            <section class="rpp-col rpp-col-raw">
              <div class="rpp-col-head">
                <span>原文（抽取自附件）</span>
                <button class="rpp-mini" type="button" @click="rawExpanded = !rawExpanded">
                  {{ rawExpanded ? '收起' : '展开全部' }}
                </button>
              </div>
              <pre :class="['rpp-raw', { 'is-expanded': rawExpanded }]">{{ data.rawText || '（无文本）' }}</pre>
            </section>

            <!-- 右：解析结果（可编辑） -->
            <section class="rpp-col rpp-col-form">
              <div class="rpp-col-head">
                <span>解析结果（可修改后保存）</span>
              </div>
              <div class="rpp-form">
                <!-- 基本信息 -->
                <div class="rpp-group">
                  <div class="rpp-group-title">基本信息</div>
                  <div class="rpp-grid">
                    <label class="rpp-field">
                      <span class="rpp-label">姓名</span>
                      <input v-model="edited.name" class="rpp-input" placeholder="未识别" />
                    </label>
                    <label class="rpp-field">
                      <span class="rpp-label">性别</span>
                      <select v-model="edited.gender" class="rpp-input">
                        <option value="">未识别</option>
                        <option value="男">男</option>
                        <option value="女">女</option>
                      </select>
                    </label>
                    <label class="rpp-field">
                      <span class="rpp-label">出生日期</span>
                      <input v-model="edited.birthDate" type="date" class="rpp-input" />
                    </label>
                    <label class="rpp-field">
                      <span class="rpp-label">手机</span>
                      <input v-model="edited.phone" class="rpp-input" placeholder="未识别" />
                    </label>
                    <label class="rpp-field rpp-span2">
                      <span class="rpp-label">邮箱</span>
                      <input v-model="edited.email" class="rpp-input" placeholder="未识别" />
                    </label>
                  </div>
                </div>

                <!-- 求职意向 -->
                <div class="rpp-group">
                  <div class="rpp-group-title">求职意向</div>
                  <div class="rpp-grid">
                    <label class="rpp-field">
                      <span class="rpp-label">目标岗位</span>
                      <input v-model="intention.position" class="rpp-input" placeholder="未识别" />
                    </label>
                    <label class="rpp-field">
                      <span class="rpp-label">期望城市</span>
                      <input v-model="intention.city" class="rpp-input" placeholder="未识别" />
                    </label>
                  </div>
                </div>

                <!-- 教育背景 -->
                <div class="rpp-group">
                  <div class="rpp-group-title">
                    教育背景
                    <span class="rpp-count">{{ (edited.educations || []).length }} 条</span>
                  </div>
                  <div v-if="!(edited.educations || []).length" class="rpp-empty">未识别到教育经历</div>
                  <div v-for="(e, i) in edited.educations" :key="'edu' + i" class="rpp-entry">
                    <div class="rpp-entry-head">
                      <span class="rpp-entry-idx">{{ i + 1 }}</span>
                      <input v-model="e.school" class="rpp-input rpp-flex" placeholder="学校" />
                      <button class="rpp-del" type="button" title="删除该条" @click="edited.educations.splice(i, 1)">✕</button>
                    </div>
                    <div class="rpp-grid">
                      <label class="rpp-field">
                        <span class="rpp-label">专业</span>
                        <input v-model="e.major" class="rpp-input" placeholder="未识别" />
                      </label>
                      <label class="rpp-field">
                        <span class="rpp-label">学历</span>
                        <input v-model="e.degree" class="rpp-input" placeholder="未识别" />
                      </label>
                      <label class="rpp-field">
                        <span class="rpp-label">开始</span>
                        <input v-model="e.startDate" class="rpp-input" placeholder="yyyy-MM" />
                      </label>
                      <label class="rpp-field">
                        <span class="rpp-label">结束</span>
                        <input v-model="e.endDate" class="rpp-input" placeholder="yyyy-MM / 至今" />
                      </label>
                    </div>
                    <textarea v-model="e.description" class="rpp-textarea" rows="2" placeholder="内容（原文保留）"></textarea>
                  </div>
                </div>

                <!-- 工作经历 -->
                <div class="rpp-group">
                  <div class="rpp-group-title">
                    工作经历
                    <span class="rpp-count">{{ (edited.works || []).length }} 条</span>
                  </div>
                  <div v-if="!(edited.works || []).length" class="rpp-empty">未识别到工作经历</div>
                  <div v-for="(w, i) in edited.works" :key="'work' + i" class="rpp-entry">
                    <div class="rpp-entry-head">
                      <span class="rpp-entry-idx">{{ i + 1 }}</span>
                      <input v-model="w.company" class="rpp-input rpp-flex" placeholder="公司" />
                      <button class="rpp-del" type="button" title="删除该条" @click="edited.works.splice(i, 1)">✕</button>
                    </div>
                    <div class="rpp-grid">
                      <label class="rpp-field">
                        <span class="rpp-label">职位</span>
                        <input v-model="w.position" class="rpp-input" placeholder="未识别" />
                      </label>
                      <label class="rpp-field"></label>
                      <label class="rpp-field">
                        <span class="rpp-label">开始</span>
                        <input v-model="w.startDate" class="rpp-input" placeholder="yyyy-MM" />
                      </label>
                      <label class="rpp-field">
                        <span class="rpp-label">结束</span>
                        <input v-model="w.endDate" class="rpp-input" placeholder="yyyy-MM / 至今" />
                      </label>
                    </div>
                    <textarea v-model="w.description" class="rpp-textarea" rows="3" placeholder="工作内容（原文保留）"></textarea>
                  </div>
                </div>

                <!-- 项目经历 -->
                <div class="rpp-group">
                  <div class="rpp-group-title">
                    项目经历
                    <span class="rpp-count">{{ (edited.projects || []).length }} 条</span>
                  </div>
                  <div v-if="!(edited.projects || []).length" class="rpp-empty">未识别到项目经历</div>
                  <div v-for="(p, i) in edited.projects" :key="'proj' + i" class="rpp-entry">
                    <div class="rpp-entry-head">
                      <span class="rpp-entry-idx">{{ i + 1 }}</span>
                      <input v-model="p.name" class="rpp-input rpp-flex" placeholder="项目名称" />
                      <button class="rpp-del" type="button" title="删除该条" @click="edited.projects.splice(i, 1)">✕</button>
                    </div>
                    <div class="rpp-grid">
                      <label class="rpp-field">
                        <span class="rpp-label">角色</span>
                        <input v-model="p.role" class="rpp-input" placeholder="未识别" />
                      </label>
                      <label class="rpp-field"></label>
                      <label class="rpp-field">
                        <span class="rpp-label">开始</span>
                        <input v-model="p.startDate" class="rpp-input" placeholder="yyyy-MM" />
                      </label>
                      <label class="rpp-field">
                        <span class="rpp-label">结束</span>
                        <input v-model="p.endDate" class="rpp-input" placeholder="yyyy-MM / 至今" />
                      </label>
                    </div>
                    <textarea v-model="p.description" class="rpp-textarea" rows="3" placeholder="项目内容（原文保留）"></textarea>
                  </div>
                </div>

                <!-- 技能 -->
                <div class="rpp-group">
                  <div class="rpp-group-title">
                    技能
                    <span class="rpp-count">{{ (edited.skills || []).length }} 项</span>
                  </div>
                  <div v-if="!(edited.skills || []).length" class="rpp-empty">未识别到技能</div>
                  <div class="rpp-skills">
                    <span v-for="(s, i) in edited.skills" :key="'sk' + i" class="rpp-skill">
                      {{ s.name }}
                      <button class="rpp-skill-del" type="button" @click="edited.skills.splice(i, 1)">✕</button>
                    </span>
                  </div>
                </div>

                <!-- 自我评价 -->
                <div class="rpp-group">
                  <div class="rpp-group-title">自我评价</div>
                  <textarea v-model="edited.selfIntro" class="rpp-textarea" rows="3" placeholder="未识别"></textarea>
                </div>
              </div>
            </section>
          </div>

          <!-- 底部 -->
          <footer class="rpp-foot">
            <p class="rpp-tip">
              💡 抽不准的字段已<b>留空</b>而非猜测（错值比空值危害大）；保存后可在编辑页继续完善
            </p>
            <div class="rpp-actions">
              <button class="rpp-btn rpp-btn-ghost" type="button" :disabled="saving" @click="onCancel">放弃</button>
              <button class="rpp-btn rpp-btn-primary" type="button" :disabled="saving" @click="onConfirm">
                {{ saving ? '保存中…' : '确认并保存为简历' }}
              </button>
            </div>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue';
import type { ResumePreviewVO } from '@/types/api';

const props = defineProps<{
  visible: boolean;
  /** 解析预览结果（后端 ResumePreviewVO） */
  data: ResumePreviewVO;
  saving?: boolean;
}>();

const emit = defineEmits<{
  (e: 'cancel'): void;
  /** 确认保存：payload 为经用户校对后的字段 */
  (e: 'confirm', payload: ResumePreviewVO): void;
}>();

const rawExpanded = ref(false);

/** 可编辑副本：初始化与 data 同步 */
const edited = reactive({
  name: '',
  gender: '',
  birthDate: '',
  phone: '',
  email: '',
  educations: [] as Array<Record<string, any>>,
  works: [] as Array<Record<string, any>>,
  projects: [] as Array<Record<string, any>>,
  skills: [] as Array<Record<string, any>>,
  selfIntro: '',
});
const intention = reactive({ position: '', city: '' });

function syncFromData() {
  const d = props.data || ({} as ResumePreviewVO);
  edited.name = d.name || '';
  edited.gender = d.gender || '';
  edited.birthDate = d.birthDate || '';
  edited.phone = d.phone || '';
  edited.email = d.email || '';
  edited.educations = (d.educations || []).map((x) => ({ ...x }));
  edited.works = (d.works || []).map((x) => ({ ...x }));
  edited.projects = (d.projects || []).map((x) => ({ ...x }));
  edited.skills = (d.skills || []).map((x) => ({ ...x }));
  edited.selfIntro = d.selfIntro || '';
  intention.position = d.jobIntention?.position || '';
  intention.city = d.jobIntention?.city || '';
  rawExpanded.value = false;
}

watch(() => props.visible, (v) => { if (v) syncFromData(); });
watch(() => props.data, () => { if (props.visible) syncFromData(); }, { deep: false });

function onCancel() {
  emit('cancel');
}

function onConfirm() {
  emit('confirm', {
    ...props.data,
    name: edited.name,
    gender: edited.gender,
    birthDate: edited.birthDate,
    phone: edited.phone,
    email: edited.email,
    jobIntention: { position: intention.position, city: intention.city },
    educations: edited.educations,
    works: edited.works,
    projects: edited.projects,
    skills: edited.skills,
    selfIntro: edited.selfIntro,
  });
}

</script>

<style scoped>
.rpp-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}
.rpp-panel {
  width: min(1200px, 100%);
  height: min(88vh, 900px);
  display: flex;
  flex-direction: column;
  background: var(--theme-bg-elevated, #fff);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}
.rpp-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--gray-100, #f1f5f9);
}
.rpp-title { margin: 0; font-size: 1rem; font-weight: 600; }
.rpp-sub { margin: 0.25rem 0 0; font-size: 0.75rem; color: var(--gray-500, #64748b); display: flex; align-items: center; gap: 0.375rem; flex-wrap: wrap; }
.rpp-dot { color: var(--gray-300, #cbd5e1); }
.rpp-badge { padding: 0.0625rem 0.375rem; border-radius: 999px; font-size: 0.6875rem; }
.rpp-badge.is-rule { background: #eff6ff; color: #2563eb; }
.rpp-badge.is-ai { background: #f0fdf4; color: #16a34a; }
.rpp-close { border: none; background: transparent; font-size: 1rem; color: var(--gray-400, #94a3b8); cursor: pointer; padding: 0.25rem; }
.rpp-close:hover { color: var(--gray-700, #334155); }

.rpp-alert { margin: 0.75rem 1rem 0; padding: 0.5rem 0.75rem; border-radius: 8px; background: #fffbeb; color: #b45309; font-size: 0.75rem; line-height: 1.6; }
.rpp-ok { margin: 0.75rem 1rem 0; padding: 0.5rem 0.75rem; border-radius: 8px; background: #f0fdf4; color: #15803d; font-size: 0.75rem; }

.rpp-body { flex: 1; min-height: 0; display: grid; grid-template-columns: minmax(240px, 38%) 1fr; gap: 0; }
@media (max-width: 900px) { .rpp-body { grid-template-columns: 1fr; } }

.rpp-col { min-height: 0; display: flex; flex-direction: column; }
.rpp-col-raw { border-right: 1px solid var(--gray-100, #f1f5f9); background: var(--gray-50, #f8fafc); }
.rpp-col-head { display: flex; align-items: center; justify-content: space-between; padding: 0.5rem 0.75rem; font-size: 0.75rem; font-weight: 600; color: var(--gray-600, #475569); border-bottom: 1px solid var(--gray-100, #f1f5f9); }
.rpp-mini { border: none; background: transparent; color: var(--primary, #2563eb); font-size: 0.6875rem; cursor: pointer; }
.rpp-raw { flex: 1; margin: 0; padding: 0.75rem; overflow: auto; font-size: 0.75rem; line-height: 1.7; white-space: pre-wrap; word-break: break-word; color: var(--gray-700, #334155); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; max-height: 260px; }
.rpp-raw.is-expanded { max-height: none; }

.rpp-form { flex: 1; overflow: auto; padding: 0.75rem; }
.rpp-group { margin-bottom: 0.875rem; }
.rpp-group-title { display: flex; align-items: center; gap: 0.5rem; font-size: 0.75rem; font-weight: 600; color: var(--gray-700, #334155); margin-bottom: 0.375rem; }
.rpp-count { font-weight: 400; color: var(--gray-400, #94a3b8); font-size: 0.6875rem; }
.rpp-empty { font-size: 0.75rem; color: var(--gray-400, #94a3b8); padding: 0.25rem 0; }
.rpp-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.375rem 0.5rem; }
.rpp-span2 { grid-column: span 2; }
.rpp-field { display: flex; flex-direction: column; gap: 0.125rem; min-width: 0; }
.rpp-label { font-size: 0.6875rem; color: var(--gray-500, #64748b); }
.rpp-input { width: 100%; padding: 0.25rem 0.375rem; border: 1px solid var(--gray-200, #e2e8f0); border-radius: 6px; font-size: 0.75rem; background: var(--theme-bg-elevated, #fff); color: inherit; }
.rpp-input:focus { outline: none; border-color: var(--primary, #2563eb); }
.rpp-textarea { width: 100%; margin-top: 0.375rem; padding: 0.375rem; border: 1px solid var(--gray-200, #e2e8f0); border-radius: 6px; font-size: 0.75rem; line-height: 1.6; resize: vertical; background: var(--theme-bg-elevated, #fff); color: inherit; }
.rpp-entry { padding: 0.5rem; margin-bottom: 0.5rem; border: 1px solid var(--gray-100, #f1f5f9); border-radius: 8px; background: var(--gray-50, #f8fafc); }
.rpp-entry-head { display: flex; align-items: center; gap: 0.375rem; margin-bottom: 0.375rem; }
.rpp-entry-idx { flex: none; width: 1.125rem; height: 1.125rem; display: inline-flex; align-items: center; justify-content: center; border-radius: 999px; background: var(--primary, #2563eb); color: #fff; font-size: 0.625rem; }
.rpp-flex { flex: 1; }
.rpp-del { flex: none; border: none; background: transparent; color: var(--gray-400, #94a3b8); cursor: pointer; font-size: 0.75rem; }
.rpp-del:hover { color: #dc2626; }
.rpp-skills { display: flex; flex-wrap: wrap; gap: 0.375rem; }
.rpp-skill { display: inline-flex; align-items: center; gap: 0.25rem; padding: 0.125rem 0.375rem; border-radius: 999px; background: var(--primary-bg, #eff6ff); color: var(--primary, #2563eb); font-size: 0.6875rem; }
.rpp-skill-del { border: none; background: transparent; color: inherit; cursor: pointer; font-size: 0.625rem; opacity: 0.7; }
.rpp-skill-del:hover { opacity: 1; }

.rpp-foot { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.75rem 1rem; border-top: 1px solid var(--gray-100, #f1f5f9); }
.rpp-tip { margin: 0; font-size: 0.6875rem; color: var(--gray-500, #64748b); }
.rpp-actions { display: flex; gap: 0.5rem; flex: none; }
.rpp-btn { padding: 0.375rem 0.875rem; border-radius: 8px; font-size: 0.8125rem; cursor: pointer; border: 1px solid transparent; }
.rpp-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.rpp-btn-ghost { background: transparent; border-color: var(--gray-200, #e2e8f0); color: var(--gray-600, #475569); }
.rpp-btn-primary { background: var(--primary, #2563eb); color: #fff; }
.rpp-btn-primary:hover:not(:disabled) { filter: brightness(1.06); }

.rpp-fade-enter-active, .rpp-fade-leave-active { transition: opacity 0.18s ease; }
.rpp-fade-enter-from, .rpp-fade-leave-to { opacity: 0; }
</style>
