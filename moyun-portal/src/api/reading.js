import { httpGet, httpPost, httpPut, httpDelete } from './client';
// =====================================================
// 读书空间 - 首页
// =====================================================
// 获取读书空间首页数据
export const getReadingHome = () => {
    return httpGet('/portal/reading/home');
};
// =====================================================
// 读书空间 - 书籍
// =====================================================
// 获取书籍详情
export const getBookDetail = (bookId) => {
    return httpGet(`/portal/reading/books/${bookId}`);
};
// =====================================================
// 读书空间 - 书单
// =====================================================
// 获取书单详情（包含书籍列表）
export const getBookListDetail = (listId) => {
    return httpGet(`/portal/reading/book-lists/${listId}`);
};
// 书单收藏（toggle，返回最新收藏状态）
export const toggleBookListBookmark = (listId) => {
    return httpPost(`/portal/reading/book-lists/${listId}/bookmark`);
};
// 查询书单收藏状态
export const checkBookListBookmark = (listId) => {
    return httpGet(`/portal/reading/book-lists/${listId}/bookmark`);
};
// =====================================================
// 读书空间 - 章节（v1.0 新增）
// =====================================================
// 获取书籍的章节目录（仅已发布，不含正文）
export const getBookChapterList = (bookId) => {
    return httpGet(`/portal/reading/books/${bookId}/chapters`);
};
// 获取章节详情（含正文，浏览量 +1）
export const getBookChapterDetail = (chapterId) => {
    return httpGet(`/portal/reading/chapters/${chapterId}`);
};
// 获取章节导航（上一章/下一章）
export const getBookChapterNav = (chapterId) => {
    return httpGet(`/portal/reading/chapters/${chapterId}/nav`);
};
// =====================================================
// 读书空间 - 阅读进度（v1.0 第二阶段新增）
// =====================================================
// 上报章节级阅读进度（前端节流 30s 上报，章节切换时强制上报）
export const reportReadingProgress = (data) => {
    return httpPost('/portal/reading/progress', data);
};
// 查询当前用户对某书的阅读进度（用于续读恢复）
export const getReadingProgress = (bookId) => {
    return httpGet(`/portal/reading/progress/${bookId}`);
};
// 查询最近阅读记录（首页/读书空间入口展示用）
export const getRecentReading = (limit = 10) => {
    return httpGet(`/portal/reading/progress/recent?limit=${limit}`);
};
// =====================================================
// 读书空间 - 书架（v1.0 第二阶段新增）
// =====================================================
// 加入书架（toggle，已收藏则取消）
export const toggleBookshelf = (bookId) => {
    return httpPost(`/portal/reading/bookshelf/${bookId}`);
};
// 移出书架
export const removeFromBookshelf = (bookId) => {
    return httpDelete(`/portal/reading/bookshelf/${bookId}`);
};
// 我的书架列表（分页）
export const getMyBookshelf = (params) => {
    return httpGet(`/portal/reading/bookshelf`, params);
};
// 检查是否已收藏
export const checkBookshelf = (bookId) => {
    return httpGet(`/portal/reading/bookshelf/check/${bookId}`);
};
// 更新书架项的最后阅读章节（阅读时同步）
export const updateBookshelfLastChapter = (bookId, lastChapterId, lastChapterNo) => {
    // httpPut 不支持 params 选项，直接拼接 query string
    const query = new URLSearchParams();
    if (lastChapterId !== null && lastChapterId !== undefined)
        query.append('lastChapterId', String(lastChapterId));
    if (lastChapterNo !== null && lastChapterNo !== undefined)
        query.append('lastChapterNo', String(lastChapterNo));
    const qs = query.toString();
    return httpPut(`/portal/reading/bookshelf/${bookId}/last-chapter${qs ? `?${qs}` : ''}`);
};
// =====================================================
// 读书空间 - 阅读偏好（v1.0 第二阶段新增）
// =====================================================
// 查询我的阅读偏好
export const getReadingPreference = () => {
    return httpGet('/portal/reading/preference');
};
// 保存（upsert）阅读偏好
export const saveReadingPreference = (data) => {
  return httpPut('/portal/reading/preference', data);
};

// =====================================================
// 读书空间 - 发现与运营（v1.0 第三阶段新增）
// =====================================================

// 发现页聚合数据（单次请求返回 Banner + 热门排行 + 限免 + 最近更新）
export const getDiscoverData = () => {
  return httpGet('/portal/reading/discover');
};

// 排行榜（type: hot/new/completed/word_count，limit: 返回条数）
export const getRanking = (type = 'hot', limit = 10) => {
  return httpGet(`/portal/reading/ranking?type=${type}&limit=${limit}`);
};

// 限免专区（返回当前有效的限免推荐书籍）
export const getLimitFree = () => {
  return httpGet('/portal/reading/limit-free');
};
