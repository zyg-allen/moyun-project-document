import request from '@/utils/request'
import { parseStrEmpty } from "@/utils/ruoyi";

// 查询章节列表（必须传 bookId）
export function listBookChapter(query) {
  return request({
    url: '/portal/admin/book-chapters/list',
    method: 'get',
    params: query
  })
}

// 查询章节详情
export function getBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + parseStrEmpty(id),
    method: 'get'
  })
}

// 新增章节
export function addBookChapter(data) {
  return request({
    url: '/portal/admin/book-chapters',
    method: 'post',
    data: data
  })
}

// 修改章节
export function updateBookChapter(data) {
  return request({
    url: '/portal/admin/book-chapters',
    method: 'put',
    data: data
  })
}

// 删除章节
export function delBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + id,
    method: 'delete'
  })
}

// 批量删除章节
export function delBookChapterBatch(ids) {
  return request({
    url: '/portal/admin/book-chapters/ids/' + ids,
    method: 'delete'
  })
}

// 发布章节
export function publishBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + id + '/publish',
    method: 'put'
  })
}

// 撤回发布
export function unpublishBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + id + '/unpublish',
    method: 'put'
  })
}

// 批量导入章节（确认入库）
// data: { bookId, chapters: [{...}], autoPublish }
export function batchImportBookChapter(data) {
  return request({
    url: '/portal/admin/book-chapters/batch-import',
    method: 'post',
    data: data
  })
}

// 重新统计书籍字数与章节数（数据修复）
export function recountBookStats(bookId) {
  return request({
    url: '/portal/admin/book-chapters/recount/' + bookId,
    method: 'post'
  })
}

// 上传文档并解析（不入库，仅返回解析结果供预览）
// formData: file + ChapterSplitRule 字段
export function parseDocument(formData) {
  return request({
    url: '/portal/admin/document/parse',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

// 解析文本内容（不依赖文件上传，用于直接粘贴文本）
// data: { text, mode, regex, headingLevel, fixedWordCount }
export function parseDocumentText(data) {
  return request({
    url: '/portal/admin/document/parse-text',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

// 上传文档转为 Markdown（用于面经/长文录入辅助）
export function parseToMarkdown(formData) {
  return request({
    url: '/portal/admin/document/to-markdown',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
