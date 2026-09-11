import request from '@/utils/request';

// AI 内容安全检测（v11.57 P0-3 场景收口：sensitive_word 业务入口走统一网关）
export function detectText(text) {
  return request({
    url: '/cms/ai/safety/detect',
    method: 'post',
    data: { text }
  });
}
