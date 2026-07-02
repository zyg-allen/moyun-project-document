import request from '@/utils/request'

/**
 * 上传文档并解析（不入库，仅返回解析结果供预览）
 * @param {FormData} formData  包含 file（文件）+ 分章规则字段（mode/regex/headingLevel/fixedWordCount/detectVolume/minChapterWords）
 * @returns {Promise} 解析结果 { title, author, totalWordCount, sourceFormat, chapters, success }
 */
export function parseDocument(formData) {
  return request({
    url: '/portal/admin/document/parse',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

/**
 * 解析粘贴的文本内容（不入库，仅返回解析结果供预览）
 * @param {Object} data  { text, mode, regex, headingLevel, fixedWordCount, detectVolume, minChapterWords }
 */
export function parseText(data) {
  return request({
    url: '/portal/admin/document/parse-text',
    method: 'post',
    data: data,
    timeout: 120000
  })
}
