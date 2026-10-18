/**
 * 记账模块 API（后端 com.moyun.ledger，路径 /portal/ledger/**）
 * 金额单位：元（人民币 CNY），BigDecimal/DECIMAL(18,2) 全链路统一
 */
import { get, post, put, del, BASE_URL } from '@/utils/request';
import { useUserStore } from '@/stores/user';

// ---------------- 登录（复用门户账号体系） ----------------

/** 门户账号登录（captchaEnabled 开启时需携带 code + uuid） */
export const login = (data) => post('/portal/login', data);

/**
 * 当前登录用户的完整资料（后端每次现查数据库，非缓存）。
 * 返回 PortalUser 全字段，其中与本端相关的是 email 与 notifySchedule。
 * 注意：stores/user 只持久化 token，userInfo 每次冷启动都是 null，
 * 所以「设置」页读邮箱/开关必须现调本接口，不能依赖登录时那一份。
 */
export const getUserInfo = () => get('/portal/user/me');

/** 更新个人资料（部分更新；绑定邮箱即传 { email }，切换邮件提醒传 { notifySchedule }） */
export const updateProfile = (data) => put('/portal/user/profile', data);

/**
 * 图形验证码（复用后端 /captchaImage，响应顶层返回 captchaEnabled/uuid/img）
 * 注意：字段位于响应顶层而非 data 内（且 data 为 null），
 * 不能复用通用 get()（其 resolve body.data 会得到 null），须独立请求。
 * @returns {Promise<{captchaEnabled: boolean, uuid: string, img: string}>}
 */
export const getCaptchaImage = () => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + '/captchaImage',
      method: 'GET',
      success: (res) => {
        if (res.statusCode !== 200) {
          reject(new Error('HTTP ' + res.statusCode));
          return;
        }
        const body = res.data || {};
        resolve({
          captchaEnabled: body.captchaEnabled !== false,
          uuid: body.uuid || '',
          img: body.img || ''
        });
      },
      fail: (err) => reject(err)
    });
  });
};

// ---------------- 总览 ----------------

/** 首页总览：净资产/总资产/总负债/涨跌/本月收支/预算/最近流水 */
export const getDashboard = () => get('/portal/ledger/dashboard');

// ---------------- 资产账户 ----------------

/** 资产账户列表 includeArchived=true 含归档 */
export const listAssets = (includeArchived) =>
  get('/portal/ledger/assets', { includeArchived: !!includeArchived });

/** 新增资产账户（initialBalance 单位元，自动生成 adjust 流水） */
export const createAsset = (data) => post('/portal/ledger/assets', data);

/** 修改资产账户（不含余额，余额校准走记账） */
export const updateAsset = (id, data) => put('/portal/ledger/assets/' + id, data);

/** 删除资产账户（归档，流水保留） */
export const deleteAsset = (id) => del('/portal/ledger/assets/' + id);

// ---------------- 负债账户 ----------------

export const listLiabilities = (includeArchived) =>
  get('/portal/ledger/liabilities', { includeArchived: !!includeArchived });

/** 新增负债账户（initialBalance 单位元，自动生成 borrow 流水） */
export const createLiability = (data) => post('/portal/ledger/liabilities', data);

export const updateLiability = (id, data) => put('/portal/ledger/liabilities/' + id, data);

export const deleteLiability = (id) => del('/portal/ledger/liabilities/' + id);

// ---------------- 流水 ----------------

/** 流水分页查询：{ type, accountId, liabilityId, categoryId, startDate, endDate, pageNum, pageSize } */
export const pageTransactions = (query) => get('/portal/ledger/transactions', query);

/** 单笔流水详情（编辑页专用；按 id 直查，不受分页窗口限制） */
export const getTransaction = (id) => get('/portal/ledger/transactions/' + id);

/** 新增记账（amount 单位元） */
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

/**
 * 取消预算（categoryId 传 null 表示取消月度总预算）。
 * 后端金额校验只接受大于 0，无法用 0 表达"取消"，故需专用删除接口。
 */
export const deleteBudget = (year, month, categoryId) => {
  const q = '?year=' + year + '&month=' + month + (categoryId == null ? '' : '&categoryId=' + categoryId);
  return del('/portal/ledger/budgets' + q);
};
export const saveBudget = (data) => post('/portal/ledger/budgets', data);

// ---------------- 报表（Phase 3） ----------------

/** 年度报表总览：月度趋势/分类占比/净资产趋势/账户分布/在还负债/当月预算 */
export const getReportOverview = (year) => get('/portal/ledger/reports/overview', { year });

/**
 * 财务分析报告（v2 整改）：核心指标/资产负债表/收支报表/6 比率/规则引擎评分/画像
 * @param {string} period 期间 yyyy-MM（缺省=当月）
 */
export const getFinancialReport = (period) =>
  get('/portal/ledger/reports/financial', period ? { period } : {});

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
export const uploadFile = (filePath, businessType) => {
  const userStore = useUserStore();
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: BASE_URL + '/portal/file/upload',
      filePath,
      name: 'file',
      formData: { businessType: businessType || 'ledger_voucher' },
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

/** 上传凭证截图 */
export const uploadVoucher = (filePath) => uploadFile(filePath, 'ledger_voucher');
/** 上传分类/账户图标图片（返回 fileUrl，存进 icon 字段） */
export const uploadCategoryIcon = (filePath) => uploadFile(filePath, 'ledger_category_icon');


// ---------------- AI 财务分析 ----------------

/** 财务分析报告：画像/指标/收入来源/债务风险/建议/LLM 综述（LLM 综述经统一网关 finance_analysis 场景生成） */
export const getAiAnalysis = (params) => get('/portal/ledger/ai/analysis', params);
export const listAiReports = (params) => get('/portal/ledger/ai/reports', params);

/** 异步分析任务：提交（立即返回 taskId）+ 轮询状态（success 时带 report） */
export const submitAiAnalysisTask = (data) => post('/portal/ledger/ai/analysis/task', data);
export const getAiAnalysisTask = (taskId) => get('/portal/ledger/ai/analysis/task/' + taskId);

/** 历史版本：完整回看 + 删除 */
export const getAiReportDetail = (id) => get('/portal/ledger/ai/reports/' + id);
export const deleteAiReport = (id) => del('/portal/ledger/ai/reports/' + id);

/** 用户画像（含身份标签字典选项） */
export const getAiProfile = () => get('/portal/ledger/ai/profile');

/** 更新画像（职位/公司/身份标签，与门户共用账号） */
export const updateAiProfile = (data) => post('/portal/ledger/ai/profile', data);

// ---------------- 小程序功能入口配置（后台可视化运营） ----------------

/** 可见功能入口列表（后台配置；失败时前端回退内置默认清单） */
export const getAppFeatures = () => get('/portal/ledger/app-features');
// ---------------- 存钱计划 ----------------

/** 存钱计划列表 + 汇总（剩余需存/累计存入/目标金额） */
export const listSavingPlans = () => get('/portal/ledger/savings');

/** 存钱计划详情（计划 + 期次流水） */
export const getSavingPlan = (id) => get('/portal/ledger/savings/' + id);

/** 新建存钱计划（method: 52week/fixed/monthly/custom，后端按方式生成期次） */
export const createSavingPlan = (data) => post('/portal/ledger/savings', data);

/** 存入某一期（actualAmount 可空=按目标金额） */
export const depositSavingRecord = (planId, recordId, actualAmount) =>
  post('/portal/ledger/savings/' + planId + '/records/' + recordId + '/deposit' +
    (actualAmount != null ? ('?actualAmount=' + encodeURIComponent(actualAmount)) : ''), {});

/** 放弃某一期（记录失败原因） */
export const failSavingRecord = (planId, recordId, reason) =>
  post('/portal/ledger/savings/' + planId + '/records/' + recordId + '/fail', { reason });

/** 删除存钱计划 */
export const deleteSavingPlan = (id) => del('/portal/ledger/savings/' + id);

// ---------------- 备忘录（待办事项） ----------------

/** 待办列表（limit>0 仅取前 N 条，首页展示用） */
export const listMemos = (limit) =>
  get('/portal/ledger/memos', limit ? { limit } : {});

/** 新增待办（title/content/eventTime/remindEnabled/remindRule/importance 整体透传） */
export const createMemo = (data) => post('/portal/ledger/memos', data);

/** 更新待办（content/done 任意） */
export const updateMemo = (id, data) => put('/portal/ledger/memos/' + id, data);

/** 切换完成状态 */
export const toggleMemo = (id) => post('/portal/ledger/memos/' + id + '/toggle', {});

/** 删除待办 */
export const deleteMemo = (id) => del('/portal/ledger/memos/' + id);



// ---------------- 库存（仓库/商品/出入库/流水） ----------------
export const listWarehouses = () => get('/portal/ledger/inventory/warehouses');
export const createWarehouse = (data) => post('/portal/ledger/inventory/warehouses', data);
export const updateWarehouse = (id, data) => put('/portal/ledger/inventory/warehouses/' + id, data);
export const deleteWarehouse = (id) => del('/portal/ledger/inventory/warehouses/' + id);
export const listProducts = (warehouseId, params) => get('/portal/ledger/inventory/warehouses/' + warehouseId + '/products', params || {});
export const createProduct = (data) => post('/portal/ledger/inventory/products', data);
export const updateProduct = (id, data) => put('/portal/ledger/inventory/products/' + id, data);
export const deleteProduct = (id) => del('/portal/ledger/inventory/products/' + id);
export const stockIn = (data) => post('/portal/ledger/inventory/stock/in', data);
export const stockOut = (data) => post('/portal/ledger/inventory/stock/out', data);
export const listStockFlows = (warehouseId, params) => get('/portal/ledger/inventory/warehouses/' + warehouseId + '/flows', params || {});

// ---------------- 账户类型（字典树） ----------------
/** 账户类型：{ asset: [{value,label}...], liability: [{value,label}...] } */
export const listAccountTypes = () => get('/portal/ledger/accountTypes');

// ---------------- 注册 / 找回密码 ----------------

/** 用户注册（后端 RegisterBody：username/password/code/uuid） */
export const register = (data) => post('/register', data);

/** 发送短信验证码（scene: register / reset_password） */
export const sendSmsCode = (data) => post('/portal/sms/code/send', data);

/** 校验短信验证码 */
export const verifySmsCode = (data) => post('/portal/sms/code/verify', data);

/** 短信重置密码（phone/code/newPassword/confirmPassword） */
export const resetPassword = (data) => post('/portal/sms/reset-password', data);

/** 发送邮箱验证码（email + type: register / reset_password） */
export const sendEmailCode = (data) => post('/portal/email/code', data);

/** 邮箱重置密码（email/code/newPassword） */
export const resetPasswordByEmail = (data) => post('/portal/email/reset-password', data);


// ---------------- 意见反馈 ----------------

/** 提交意见反馈（feedbackType/subject/description/contact） */
export const submitFeedback = (data) => post('/portal/feedback/submit', data);

/** 我的反馈列表（page/pageSize） */
export const listMyFeedback = (params) => get('/portal/feedback/my-list', params);


// ---------------- 定时任务 ----------------

/** 定时任务列表（keyword/pageNum/pageSize） */
export const listScheduleTasks = (params) => get('/portal/ledger/schedules', params);

/** 定时任务详情 */
export const getScheduleTask = (id) => get('/portal/ledger/schedules/' + id);

/** 新建定时任务 */
export const createScheduleTask = (data) => post('/portal/ledger/schedules', data);

/** 修改定时任务 */
export const updateScheduleTask = (id, data) => put('/portal/ledger/schedules/' + id, data);

/** 删除定时任务 */
export const deleteScheduleTask = (id) => del('/portal/ledger/schedules/' + id);

/** 启用/停用定时任务 */
export const toggleScheduleTask = (id) => post('/portal/ledger/schedules/' + id + '/toggle');

/** 立即执行一次 */
export const runScheduleTask = (id) => post('/portal/ledger/schedules/' + id + '/run');

/** 任务执行日志（按任务ID） */
export const listScheduleLogs = (taskId) => get('/portal/ledger/schedules/logs', taskId ? { taskId } : {});

/** 重试失败日志 */
export const retryScheduleLog = (logId) => post('/portal/ledger/schedules/logs/' + logId + '/retry');