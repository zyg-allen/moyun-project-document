import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { InterviewResumeTemplateVO, UserResumeVO } from '@/types/api';

/**
 * 简历 Store（v10.18 简历模块重构补丁·阶段一）
 *
 * 职责：跨页面传递「模板套用」数据。
 * 流程：模板库页点击「使用此模板」→ 拉详情 → fillFromTemplate 解析 sampleData 存入 store
 *      → 跳转编辑页带 source=template 标识 → 编辑页 onMounted 消费 templateSource 填充 form → clearTemplateSource
 *
 * 设计原则：
 * - store 仅承担"跨页面"数据传递，编辑页内表单状态仍由页面 ref 管理（避免过度中心化）
 * - sampleData 字段为可选，未配置的模板回退到原 query 参数预填标题/期望岗位流程
 * - 消费后立即 clearTemplateSource，避免刷新页面残留旧模板数据
 */
export const useResumeStore = defineStore('resume', () => {
  /** 模板套用暂存数据（编辑页消费后清空） */
  const templateSource = ref<{
    sourceTemplateId: number | string;
    templateTitle: string;
    templateCategory?: string;
    /** 从 sampleData 解析出的结构化字段（与 UserResumeVO 字段语义对齐） */
    fields: Partial<UserResumeVO> | null;
  } | null>(null);

  /** 当前是否有待消费的模板套用数据 */
  function hasTemplateSource(): boolean {
    return templateSource.value !== null;
  }

  /**
   * 从模板详情解析 sampleData 并暂存
   * @param template 模板详情（含 sampleData JSON 字符串）
   * @returns true 表示已暂存可填充字段，false 表示 sampleData 为空（调用方应回退到 query 预填）
   */
  function fillFromTemplate(template: InterviewResumeTemplateVO): boolean {
    let fields: Partial<UserResumeVO> | null = null;
    if (template.sampleData) {
      try {
        fields = JSON.parse(template.sampleData) as Partial<UserResumeVO>;
      } catch (e) {
        console.warn('[resumeStore] sampleData JSON 解析失败，回退到 query 预填', e);
        fields = null;
      }
    }
    templateSource.value = {
      sourceTemplateId: template.id,
      templateTitle: template.title || '',
      templateCategory: template.category,
      fields,
    };
    return fields !== null;
  }

  /** 编辑页 onMounted 消费后清空暂存，避免刷新残留 */
  function clearTemplateSource() {
    templateSource.value = null;
  }

  return {
    templateSource,
    hasTemplateSource,
    fillFromTemplate,
    clearTemplateSource,
  };
});
