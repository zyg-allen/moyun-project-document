/**
 * 记账模块 API（后端 com.moyun.ledger，路径 /portal/ledger/**）
 * 金额单位：分（转换见 utils/money.js）
 */
import { get, post, put, del, BASE_URL } from '@/utils/request';
import { useUserStore } from '@/stores/user';

// ---------------- 登录（复用门户账号体系） ----------------

/** 门户账号登录 */
export const login = (data) => post('/portal/login', data);

// ---------------- 总览 ----------------

/** 首页总览：净资产/总资产/总负债/涨跌/本月收支/预算/最近流水 */
export const getDashboard = () => get('/portal/ledger/dashboard');

// ---------------- 资产账户 ----------------

/** 资产账户列表 includeArchived=true 含归档 */
export const listAssets = (includeArchived) =>
  get('/portal/ledger/assets', { includeArchived: !!includeArchived });

/** 新增资产账户（initialBalance 单位分，自动生成 adjust 流水） */
export const createAsset = (data) => post('/portal/ledger/assets', data);

/** 修改资产账户（不含余额，余额校准走记账） */
export const updateAsset = (id, data) => put('/portal/ledger/assets/' + id, data);

/** 删除资产账户（归档，流水保留） */
export const deleteAsset = (id) => del('/portal/ledger/assets/' + id);

// ---------------- 负债账户 ----------------

export const listLiabilities = (includeArchived) =>
  get('/portal/ledger/liabilities', { includeArchived: !!includeArchived });

/** 新增负债账户（initialBalance 单位分，自动生成 borrow 流水） */
export const createLiability = (data) => post('/portal/ledger/liabilities', data);

export const updateLiability = (id, data) => put('/portal/ledger/liabilities/' + id, data);

export const deleteLiability = (id) => del('/portal/ledger/liabilities/' + id);

// ---------------- 流水 ----------------

/** 流水分页查询：{ type, accountId, liabilityId, categoryId, startDate, endDate, pageNum, pageSize } */
export const pageTransactions = (query) => get('/portal/ledger/transactions', query);

/** 新增记账（amount 单位分） */
export const createTransaction = (data) => post('/portal/ledger/transactions', data);

/** 修改记账（冲正→重放） */
export const updateTransaction = (id, data) => put('/portal/ledger/transactions/' + id, data);

/** 删除记账（冲正→归档） */
export const deleteTransaction = (id) => del('/portal/ledger/transactions/' + id);

// ---------------- 分类 ----------------

/** 可用分类（系统预设+自定义）type: income/expense */
export const listCategories = (type) => get('/portal/ledger/categories', type ? { type } : {});

export const createCategory = (data) => post('/portal/ledger/categories', data);

export const updateCategory = (id, data) => put('/portal/ledger/categories/' + id, data);

export const deleteCategory = (id) => del('/portal/ledger/categories/' + id);

// ---------------- 预算 ----------------

/** 指定月份预算列表（null=当年当月） */
export const listBudgets = (year, month) =>
  get('/portal/ledger/budgets', { year: year || undefined, month: month || undefined });

/** 设置预算（存在即更新） */
export const saveBudget = (data) => post('/portal/ledger/budgets', data);

// ---------------- 报表（Phase 3） ----------------

/** 年度报表总览：月度趋势/分类占比/净资产趋势/账户分布/在还负债/当月预算 */
export const getReportOverview = (year) => get('/portal/ledger/reports/overview', { year });

/**
 * 流水 CSV 导出（H5：fetch blob 下载；小程序：uni.downloadFile）
 * @returns {Promise<void>}
 */
export const exportTransactionsCsv = (startDate, endDate) => {
  const userStore = useUserStore();
  const params = [];
  if (startDate) params.push('startDate=' + startDate);
  if (endDate) params.push('endDate=' + endDate);
  const url = BASE_URL + '/portal/ledger/reports/export' + (params.length ? '?' + params.join('&') : '');
  const header = userStore.token ? { 'Authorization': 'Bearer ' + userStore.token } : {};
  return new Promise((resolve, reject) => {
    // #ifdef H5
    fetch(url, { headers: header })
      .then((res) => {
        if (res.status !== 200) throw new Error('导出失败(' + res.status + ')');
        return res.blob();
      })
      .then((blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'ledger-' + (startDate || 'all') + '~' + (endDate || 'now') + '.csv';
        a.click();
        URL.revokeObjectURL(a.href);
        resolve();
      })
      .catch((e) => {
        uni.showToast({ title: e.message || '导出失败', icon: 'none' });
        reject(e);
      });
    // #endif
    // #ifndef H5
    uni.downloadFile({
      url,
      header,
      success: (res) => {
        if (res.statusCode !== 200) {
          uni.showToast({ title: '导出失败(' + res.statusCode + ')', icon: 'none' });
          reject(new Error('导出失败'));
          return;
        }
        uni.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          success: () => resolve(),
          fail: () => {
            uni.showToast({ title: '文件已下载：' + res.tempFilePath, icon: 'none' });
            resolve();
          }
        });
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，导出失败', icon: 'none' });
        reject(err);
      }
    });
    // #endif
  });
};

// ---------------- 凭证截图（复用门户文件服务） ----------------

/**
 * 上传凭证截图（multipart/form-data）
 * @param {string} filePath uni.chooseImage 返回的本地临时路径
 * @returns {Promise<string>} 文件访问 URL（SysFile.fileUrl）
 */
export const uploadVoucher = (filePath) => {
  const userStore = useUserStore();
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/portal/file/upload',
      filePath,
      name: 'file',
      formData: { businessType: 'ledger_voucher' },
      header: userStore.token ? { 'Authorization': 'Bearer ' + userStore.token } : {},
      success: (res) => {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
          if (body.code !== undefined && body.code !== 200) {
            uni.showToast({ title: body.msg || '上传失败', icon: 'none' });
            reject(new Error(body.msg || '上传失败'));
            return;
          }
          const file = body.data || {};
          if (!file.fileUrl) {
            uni.showToast({ title: '上传失败：未获取到文件地址', icon: 'none' });
            reject(new Error('上传失败'));
            return;
          }
          resolve(file.fileUrl);
        } catch (e) { reject(e); }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，上传失败', icon: 'none' });
        reject(err);
      }
    });
  });
};


// ---------------- AI 财务分析 ----------------

/** 财务分析报告：画像/指标/收入来源/债务风险/建议/LLM 综述 */
export const getAiAnalysis = () => get('/portal/ledger/ai/analysis');

/** 用户画像（含身份标签字典选项） */
export const getAiProfile = () => get('/portal/ledger/ai/profile');

/** 更新画像（职位/公司/身份标签，与门户共用账号） */
export const updateAiProfile = (data) => post('/portal/ledger/ai/profile', data);