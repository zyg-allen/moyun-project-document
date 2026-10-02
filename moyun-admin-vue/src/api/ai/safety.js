import request from '@/utils/request';

// AI 内容安全检测（sensitive_word 业务入口走统一网关）
export function detectText(text) {
  return request({
    url: '/cms/ai/safety/detect',
    method: 'post',
    data: { text }
  });
}
