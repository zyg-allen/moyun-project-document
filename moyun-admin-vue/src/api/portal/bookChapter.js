import request from '@/utils/request'

/**
 * 上传文档并解析（不入库，仅返回解析结果供预览）
 * @param {FormData} data  含 file 与分章规则字段
 * @returns {Promise}
 */
export function parseDocument(data) {
  return request({
    url: '/portal/admin/document/parse',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

/**
 * 解析粘贴的文本内容（不入库，仅返回解析结果供预览）
 * @param {Object} data  { text, mode, regex, headingLevel, fixedWordCount, detectVolume, minChapterWords }
 * @returns {Promise}
 */
export function parseDocumentText(data) {
  return request({
    url: '/portal/admin/document/parse-text',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

/**
 * 批量导入章节（确认入库）
 * @param {Object} data  { bookId, chapters: [{ title, content, contentMarkdown, editorMode, isFree }], autoPublish }
 * @returns {Promise}
 */
export function batchImportChapters(data) {
  return request({
    url: '/portal/admin/book-chapters/batch-import',
    method: 'post',
    data: data,
    timeout: 120000
  })
}

/**
 * 批量导入章节（batchImportChapters 的别名，保持导入向导命名一致）
 * @param {Object} data  { bookId, chapters, autoPublish }
 * @returns {Promise}
 */
export function batchImportBookChapter(data) {
  return batchImportChapters(data)
}

/**
 * 查询章节列表（分页）
 * @param {Object} query  { bookId, pageNum, pageSize, ... }
 * @returns {Promise}
 */
export function listBookChapter(query) {
  return request({
    url: '/portal/admin/book-chapters/list',
    method: 'get',
    params: query
  })
}

/**
 * 获取章节详情
 * @param {number|string} id
 * @returns {Promise}
 */
export function getBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + id,
    method: 'get'
  })
}

/**
 * 新增章节
 * @param {Object} data  章节实体
 * @returns {Promise}
 */
export function addBookChapter(data) {
  return request({
    url: '/portal/admin/book-chapters',
    method: 'post',
    data: data
  })
}

/**
 * 修改章节
 * @param {Object} data  章节实体（含 id）
 * @returns {Promise}
 */
export function updateBookChapter(data) {
  return request({
    url: '/portal/admin/book-chapters',
    method: 'put',
    data: data
  })
}

/**
 * 删除章节
 * @param {number|string} id
 * @returns {Promise}
 */
export function delBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + id,
    method: 'delete'
  })
}

/**
 * 批量删除章节
 * @param {string|number[]} ids  逗号分隔的ID字符串或ID数组
 * @returns {Promise}
 */
export function delBookChapterBatch(ids) {
  const idsStr = Array.isArray(ids) ? ids.join(',') : ids
  return request({
    url: '/portal/admin/book-chapters/ids/' + idsStr,
    method: 'delete'
  })
}

/**
 * 发布章节
 * @param {number|string} id
 * @returns {Promise}
 */
export function publishBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + id + '/publish',
    method: 'put'
  })
}

/**
 * 撤回发布章节
 * @param {number|string} id
 * @returns {Promise}
 */
export function unpublishBookChapter(id) {
  return request({
    url: '/portal/admin/book-chapters/' + id + '/unpublish',
    method: 'put'
  })
}

/**
 * 重新统计书籍字数与章节数
 * @param {number|string} bookId
 * @returns {Promise}
 */
export function recountBookStats(bookId) {
  return request({
    url: '/portal/admin/book-chapters/recount/' + bookId,
    method: 'post'
  })
}
