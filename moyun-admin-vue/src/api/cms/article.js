import request from '@/utils/request';

// 查询文章列表
export function listArticle(query) {
  return request({
    url: '/cms/article/list',
    method: 'get',
    params: query
  });
}

// 查询文章详细
export function getArticle(id) {
  return request({
    url: '/cms/article/' + id,
    method: 'get'
  });
}

// 新增文章
export function addArticle(data) {
  return request({
    url: '/cms/article',
    method: 'post',
    data: data
  });
}

// 修改文章
export function updateArticle(data) {
  return request({
    url: '/cms/article',
    method: 'put',
    data: data
  });
}

// 删除文章
export function delArticle(id) {
  return request({
    url: '/cms/article/' + id,
    method: 'delete'
  });
}

// 批量删除文章
export function delArticleBatch(ids) {
  return request({
    url: '/cms/article/batch',
    method: 'delete',
    data: ids
  });
}

// 审核文章
export function auditArticle(data) {
  return request({
    url: '/cms/article/audit',
    method: 'put',
    data: data
  });
}

// 发布/下架文章
export function publishArticle(data) {
  return request({
    url: '/cms/article/publish',
    method: 'put',
    data: data
  });
}

// 设置精选
export function setFeatured(data) {
  return request({
    url: '/cms/article/featured',
    method: 'put',
    data: data
  });
}

// 设置置顶
export function setTop(data) {
  return request({
    url: '/cms/article/top',
    method: 'put',
    data: data
  });
}

// 设置轮播
export function setCarousel(data) {
  return request({
    url: '/cms/article/carousel',
    method: 'put',
    data: data
  });
}
