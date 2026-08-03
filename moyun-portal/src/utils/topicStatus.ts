/**
 * 话题审核状态映射工具
 *
 * 后端状态枚举（PortalTopic.status）：
 *   pending  待审核（新话题默认）
 *   active   活跃（审核通过）
 *   archived 归档（下架，可申诉恢复）
 *   deleted  删除（软删）
 *   rejected 审核驳回
 *
 * 用于「我的话题」列表展示状态标签、创建页提交后提示。
 */

export interface TopicStatusMeta {
  /** 展示文案 */
  label: string;
  /** 标签颜色类（Tailwind 任意值不便于动态拼接，统一返回内联色对） */
  color: {
    bg: string;
    text: string;
    border: string;
  };
  /** 是否允许编辑 */
  editable: boolean;
  /** 是否对外可见（active 才进 Feed/列表） */
  visible: boolean;
}

const STATUS_MAP: Record<string, TopicStatusMeta> = {
  pending: {
    label: '待审核',
    color: { bg: 'rgba(245, 158, 11, 0.1)', text: '#d97706', border: 'rgba(245, 158, 11, 0.3)' },
    editable: true,
    visible: false,
  },
  active: {
    label: '已发布',
    color: { bg: 'rgba(34, 197, 94, 0.1)', text: '#16a34a', border: 'rgba(34, 197, 94, 0.3)' },
    editable: true,
    visible: true,
  },
  archived: {
    label: '已归档',
    color: { bg: 'rgba(107, 114, 128, 0.1)', text: '#6b7280', border: 'rgba(107, 114, 128, 0.3)' },
    editable: false,
    visible: false,
  },
  deleted: {
    label: '已删除',
    color: { bg: 'rgba(239, 68, 68, 0.1)', text: '#dc2626', border: 'rgba(239, 68, 68, 0.3)' },
    editable: false,
    visible: false,
  },
  rejected: {
    label: '审核驳回',
    color: { bg: 'rgba(239, 68, 68, 0.1)', text: '#dc2626', border: 'rgba(239, 68, 68, 0.3)' },
    editable: true,
    visible: false,
  },
};

const DEFAULT_META: TopicStatusMeta = {
  label: '未知',
  color: { bg: 'rgba(107, 114, 128, 0.1)', text: '#6b7280', border: 'rgba(107, 114, 128, 0.3)' },
  editable: false,
  visible: false,
};

/**
 * 获取话题状态元信息
 */
export function getTopicStatusMeta(status?: string): TopicStatusMeta {
  if (!status) return DEFAULT_META;
  return STATUS_MAP[status] || DEFAULT_META;
}

/**
 * 是否为待审核/驳回状态（创建后未通过审核的）
 */
export function isPendingOrRejected(status?: string): boolean {
  return status === 'pending' || status === 'rejected';
}

export default { getTopicStatusMeta, isPendingOrRejected };
