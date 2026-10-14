/**
 * 提交锁 mixin —— 防止重复提交（双击「保存」出双份数据）
 *
 * ## 为什么需要
 *
 * uni-app 的 `@tap` 没有内置防抖。多个页面的保存按钮在弱网下双击会发出两次请求，
 * 而金额类接口大多**不是幂等**的（每次生成新的 clientUuid，后端按 uuid 去重拦不住），
 * 结果是：建两个资产账户（各带一笔初始流水）、两个存钱计划（各 52 期期次）、
 * 两条同名分类。
 *
 * 审计发现同库已有 8 处自己实现了 `saving` 标志，另有 9 处完全没有 —— 实现不一致。
 * 本 mixin 把这套逻辑收口到一处，各页只需包一层 `withSubmitLock`。
 *
 * ## 用法
 *
 * ```js
 * import submitLock from '@/mixins/submitLock';
 *
 * export default {
 *   mixins: [submitLock],
 *   methods: {
 *     async save() {
 *       await this.withSubmitLock(async () => {
 *         await createXxx(payload);
 *         uni.showToast({ title: '已保存', icon: 'success' });
 *       });
 *     }
 *   }
 * };
 * ```
 *
 * 模板里绑定 `:class="{ disabled: submitting }"` 给出视觉反馈，
 * 并可用 `@tap="!submitting && save()"` 双保险。
 *
 * ## 语义说明
 *
 * `withSubmitLock` **不吞异常**：回调抛错时先复位标志再重新抛出，
 * 交由各页既有的 `try/catch`（或请求拦截器统一 toast）处理，
 * 避免把「提交失败」静默成「没反应」。
 */
export default {
  data() {
    return {
      submitting: false
    };
  },
  methods: {
    /**
     * 在提交锁内执行异步任务；重复调用会被直接忽略。
     *
     * @param {Function} task 返回 Promise 的提交逻辑
     * @returns {Promise<*>} task 的结果；被锁拦截时返回 undefined
     */
    async withSubmitLock(task) {
      if (this.submitting) return undefined;
      this.submitting = true;
      try {
        return await task();
      } finally {
        this.submitting = false;
      }
    }
  }
};
