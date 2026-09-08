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
export const listAiReports = (params) => httpGet('/portal/ledger/ai/reports', params);

/** 用户画像（含身份标签字典选项） */
export const getAiProfile = () => get('/portal/ledger/ai/profile');

/** 更新画像（职位/公司/身份标签，与门户共用账号） */
export const updateAiProfile = (data) => post('/portal/ledger/ai/profile', data);
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

/** 新增待办 */
export const createMemo = (content) => post('/portal/ledger/memos', { content });

/** 更新待办（content/done 任意） */
export const updateMemo = (id, data) => put('/portal/ledger/memos/' + id, data);

/** 切换完成状态 */
export const toggleMemo = (id) => post('/portal/ledger/memos/' + id + '/toggle', {});

/** 删除待办 */
export const deleteMemo = (id) => del('/portal/ledger/memos/' + id);

// ---------------- 打赏（演示：模拟支付成功） ----------------

/** 累计打赏金额 */
export const getTipTotal = () => get('/portal/ledger/tips/total');

/** 发起打赏 { amount, payWay, target, reason } */
export const createTip = (data) => post('/portal/ledger/tips', data);

// ---------------- 定时记账 ----------------

/** 任务列表（含分类名/账户名 + 启用统计） */
export const listScheduleTasks = () => get('/portal/ledger/schedules');

/** 执行日志（taskId 可选=全部） */
export const listScheduleLogs = (taskId) =>
  get('/portal/ledger/schedules/logs', taskId ? { taskId } : {});

/** 新建定时记账任务 */
export const createScheduleTask = (data) => post('/portal/ledger/schedules', data);

/** 修改任务 */
export const updateScheduleTask = (id, data) => put('/portal/ledger/schedules/' + id, data);

/** 启用/停用任务 */
export const toggleScheduleTask = (id) => post('/portal/ledger/schedules/' + id + '/toggle', {});

/** 立即执行一次 */
export const runScheduleTask = (id) => post('/portal/ledger/schedules/' + id + '/run', {});

/** 重试失败日志 */
export const retryScheduleLog = (logId) => post('/portal/ledger/schedules/logs/' + logId + '/retry', {});

/** 删除任务 */
export const deleteScheduleTask = (id) => del('/portal/ledger/schedules/' + id);
// ---------------- 意见反馈（复用门户 portal_feedback） ----------------

/** 提交反馈 { feedbackType: suggestion/bug/experience/other, subject, description, contact } */
export const submitFeedback = (data) => post('/portal/feedback/submit', data);

/** 我的反馈历史（分页参数 page/pageSize 走 query；返回分页对象 records/total） */
export const listMyFeedback = (params) => get('/portal/feedback/my-list', params);
// ---------------- 注册（复用门户账号体系，v11.35 双模式） ----------------

/**
 * 发送邮箱验证码（注册场景）
 * { email, type: 'register' }
 */
export const sendEmailCode = (data) => post('/portal/email/code', data);

/**
 * 发送短信验证码（注册场景，匿名可发）
 * { phone, scene: 'register' }  mock 模式验证码写 dev 日志
 */
export const sendSmsCode = (data) => post('/portal/sms/code/send', data);

/**
 * 注册（成功即自动登录返回 token）
 * 手机模式 { username, password, phone, smsCode, code?, uuid? }
 * 邮箱模式 { username, password, email, emailCode, code?, uuid? }
 */
export const register = (data) => post('/portal/register', data);