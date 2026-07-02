import request from '@/utils/request'

// 查询书单内书籍列表（带书籍详情）
export function listBookListItems(bookListId) {
  return request({
    url: '/portal/admin/book-lists/' + bookListId + '/items',
    method: 'get'
  })
}

// 批量添加书籍到书单
export function addBooksToBookList(bookListId, bookIds, note) {
  return request({
    url: '/portal/admin/book-lists/' + bookListId + '/items',
    method: 'post',
    data: { bookIds: bookIds, note: note || null }
  })
}

// 批量从书单移除书籍
export function removeBooksFromBookList(bookListId, bookIds) {
  return request({
    url: '/portal/admin/book-lists/' + bookListId + '/items',
    method: 'delete',
    data: { bookIds: bookIds }
  })
}

// 从书单移除单本书籍
export function removeBookFromBookList(bookListId, bookId) {
  return request({
    url: '/portal/admin/book-lists/' + bookListId + '/items/' + bookId,
    method: 'delete'
  })
}

// 批量更新书单内书籍排序
// sortItems: [{ id: itemId, sort: 0 }, ...]
export function updateBookListSort(bookListId, sortItems) {
  return request({
    url: '/portal/admin/book-lists/' + bookListId + '/items/sort',
    method: 'put',
    data: sortItems
  })
}

// 查询可添加到书单的书籍（排除已在书单内的）
export function listAvailableBooks(bookListId, params) {
  return request({
    url: '/portal/admin/book-lists/' + bookListId + '/available-books',
    method: 'get',
    params: params
  })
}
